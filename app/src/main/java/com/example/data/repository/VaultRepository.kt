package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.LruCache
import com.example.data.local.AlbumEntity
import com.example.data.local.CloudBackupSnapshotEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.local.VaultDao
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val dao: VaultDao,
    val cryptoManager: CryptoManager
) {

    private val photosDir = File(context.filesDir, "vault_photos").apply { mkdirs() }
    private val thumbsDir = File(context.filesDir, "vault_thumbs").apply { mkdirs() }
    private val backupsDir = File(context.filesDir, "vault_backups").apply { mkdirs() }

    // In-memory memory LRU cache for decrypted thumbnails (max 100 items)
    private val thumbMemoryCache = object : LruCache<String, Bitmap>(100) {}

    // === ALBUM OPERATIONS ===

    fun getRootAlbums(): Flow<List<AlbumEntity>> = dao.getRootAlbums()

    fun getSubAlbums(parentId: String): Flow<List<AlbumEntity>> = dao.getSubAlbums(parentId)

    fun getAllAlbums(): Flow<List<AlbumEntity>> = dao.getAllAlbums()

    suspend fun getAllAlbumsList(): List<AlbumEntity> = dao.getAllAlbumsList()

    fun getAlbumByIdFlow(id: String): Flow<AlbumEntity?> = dao.getAlbumByIdFlow(id)

    suspend fun getAlbumById(id: String): AlbumEntity? = dao.getAlbumById(id)

    suspend fun createAlbum(
        name: String,
        description: String = "",
        parentId: String? = null,
        colorHex: String = "#0284C7"
    ): AlbumEntity {
        val album = AlbumEntity(
            id = UUID.randomUUID().toString(),
            parentId = parentId,
            name = name.trim(),
            description = description.trim(),
            colorHex = colorHex,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertAlbum(album)
        return album
    }

    suspend fun updateAlbum(album: AlbumEntity) {
        dao.updateAlbum(album.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun setAlbumCover(albumId: String, photoId: String?) {
        dao.updateAlbumCover(albumId, photoId, System.currentTimeMillis())
    }

    suspend fun deleteAlbum(albumId: String) {
        // Move all photos in this album to trash
        val photos = dao.getPhotosByAlbumList(albumId)
        val photoIds = photos.map { it.id }
        if (photoIds.isNotEmpty()) {
            dao.moveMultipleToTrash(photoIds)
        }

        // Recursively handle sub-albums
        val subAlbums = dao.getSubAlbumsList(albumId)
        for (sub in subAlbums) {
            deleteAlbum(sub.id)
        }

        dao.deleteAlbumById(albumId)
    }

    /**
     * Builds breadcrumb path from Root down to current album.
     */
    suspend fun getBreadcrumbPath(currentAlbumId: String?): List<AlbumEntity> = withContext(Dispatchers.IO) {
        if (currentAlbumId == null) return@withContext emptyList()
        val path = mutableListOf<AlbumEntity>()
        var currId: String? = currentAlbumId
        val visited = mutableSetOf<String>()

        while (currId != null && !visited.contains(currId)) {
            visited.add(currId)
            val album = dao.getAlbumById(currId) ?: break
            path.add(0, album) // prepend
            currId = album.parentId
        }
        path
    }

    // === PHOTO OPERATIONS ===

    fun getPhotosByAlbum(albumId: String): Flow<List<EncryptedPhotoEntity>> = dao.getPhotosByAlbum(albumId)

    fun getAllActivePhotos(): Flow<List<EncryptedPhotoEntity>> = dao.getAllActivePhotos()

    fun getFavoritePhotos(): Flow<List<EncryptedPhotoEntity>> = dao.getFavoritePhotos()

    fun getTrashPhotos(): Flow<List<EncryptedPhotoEntity>> = dao.getTrashPhotos()

    fun getPhotoByIdFlow(id: String): Flow<EncryptedPhotoEntity?> = dao.getPhotoByIdFlow(id)

    suspend fun getPhotoById(id: String): EncryptedPhotoEntity? = dao.getPhotoById(id)

    fun getPhotoCountInAlbumFlow(albumId: String): Flow<Int> = dao.getPhotoCountInAlbumFlow(albumId)

    suspend fun getPhotoCountInAlbum(albumId: String): Int = dao.getPhotoCountInAlbum(albumId)

    suspend fun getTotalPhotosCount(): Int = dao.getTotalPhotosCount()

    suspend fun getTotalStorageBytes(): Long = dao.getTotalStorageBytes() ?: 0L

    /**
     * Imports a photo from a gallery URI, encrypts full image and thumbnail, and saves to database.
     */
    suspend fun importPhotoFromUri(
        uri: Uri,
        albumId: String,
        originalName: String = "photo_${System.currentTimeMillis()}.jpg"
    ): EncryptedPhotoEntity? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext null
            val rawBytes = inputStream.use { it.readBytes() }
            if (rawBytes.isEmpty()) return@withContext null

            // Normalize orientation so imported photos and DB metadata are always upright and true to aspect ratio
            val (normalizedBytes, width, height) = cryptoManager.normalizeImageBytes(rawBytes)
            val mimeType = "image/jpeg"

            val photoId = UUID.randomUUID().toString()
            val encFileName = "$photoId.enc"
            val encThumbFileName = "${photoId}_thumb.enc"

            val destPhotoFile = File(photosDir, encFileName)
            val destThumbFile = File(thumbsDir, encThumbFileName)

            // Encrypt full image with normalized orientation
            val encryptedBytes = cryptoManager.encryptBytes(normalizedBytes)
            FileOutputStream(destPhotoFile).use { it.write(encryptedBytes) }

            // Generate and encrypt proportional thumbnail preserving aspect ratio
            cryptoManager.generateAndEncryptThumbnail(normalizedBytes, destThumbFile, 400)

            val photoEntity = EncryptedPhotoEntity(
                id = photoId,
                albumId = albumId,
                encryptedFileName = encFileName,
                encryptedThumbFileName = encThumbFileName,
                originalFileName = originalName,
                mimeType = mimeType,
                fileSizeBytes = encryptedBytes.size.toLong(),
                width = width,
                height = height,
                importedAt = System.currentTimeMillis()
            )

            dao.insertPhoto(photoEntity)

            // If album has no cover set, assign this photo as cover automatically
            val album = dao.getAlbumById(albumId)
            if (album != null && album.coverPhotoId == null) {
                dao.updateAlbumCover(albumId, photoId)
            }

            photoEntity
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Decrypts and loads thumbnail Bitmap with in-memory caching.
     */
    suspend fun getThumbnailBitmap(photo: EncryptedPhotoEntity): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = photo.id
        thumbMemoryCache.get(cacheKey)?.let { return@withContext it }

        val thumbFile = File(thumbsDir, photo.encryptedThumbFileName)
        val bitmap = if (thumbFile.exists()) {
            cryptoManager.decryptFileToBitmap(thumbFile)
        } else {
            // Fallback to full file downsampled
            val fullFile = File(photosDir, photo.encryptedFileName)
            cryptoManager.decryptFileToBitmap(fullFile, sampleSize = 4)
        }

        if (bitmap != null) {
            thumbMemoryCache.put(cacheKey, bitmap)
        }
        bitmap
    }

    /**
     * Decrypts and loads full resolution Bitmap.
     */
    suspend fun getFullBitmap(photo: EncryptedPhotoEntity): Bitmap? = withContext(Dispatchers.IO) {
        val fullFile = File(photosDir, photo.encryptedFileName)
        cryptoManager.decryptFileToBitmap(fullFile, sampleSize = 1)
    }

    suspend fun toggleFavorite(photoId: String, isFavorite: Boolean) {
        dao.toggleFavorite(photoId, isFavorite)
    }

    suspend fun updatePhotoNote(photoId: String, note: String) {
        val photo = dao.getPhotoById(photoId) ?: return
        dao.updatePhoto(photo.copy(notes = note))
    }

    suspend fun movePhoto(photoId: String, targetAlbumId: String) {
        dao.movePhoto(photoId, targetAlbumId)
    }

    suspend fun movePhotos(photoIds: List<String>, targetAlbumId: String) {
        dao.movePhotos(photoIds, targetAlbumId)
    }

    suspend fun moveToTrash(photoId: String) {
        dao.moveToTrash(photoId)
    }

    suspend fun moveMultipleToTrash(photoIds: List<String>) {
        dao.moveMultipleToTrash(photoIds)
    }

    suspend fun restoreFromTrash(photoId: String) {
        dao.restoreFromTrash(photoId)
    }

    suspend fun restoreMultipleFromTrash(photoIds: List<String>) {
        dao.restoreMultipleFromTrash(photoIds)
    }

    suspend fun permanentlyDeletePhoto(photoId: String) = withContext(Dispatchers.IO) {
        val photo = dao.getPhotoById(photoId)
        if (photo != null) {
            File(photosDir, photo.encryptedFileName).delete()
            File(thumbsDir, photo.encryptedThumbFileName).delete()
            thumbMemoryCache.remove(photoId)
            dao.permanentlyDeletePhoto(photoId)
        }
    }

    suspend fun permanentlyDeleteMultiple(photoIds: List<String>) = withContext(Dispatchers.IO) {
        for (id in photoIds) {
            val photo = dao.getPhotoById(id)
            if (photo != null) {
                File(photosDir, photo.encryptedFileName).delete()
                File(thumbsDir, photo.encryptedThumbFileName).delete()
                thumbMemoryCache.remove(id)
            }
        }
        dao.permanentlyDeletePhotos(photoIds)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashed = dao.getTrashPhotosList()
        for (photo in trashed) {
            File(photosDir, photo.encryptedFileName).delete()
            File(thumbsDir, photo.encryptedThumbFileName).delete()
            thumbMemoryCache.remove(photo.id)
        }
        dao.emptyTrash()
    }

    /**
     * Decrypts and exports photo back to Android public Gallery.
     */
    suspend fun exportPhotoToGallery(photo: EncryptedPhotoEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val fullFile = File(photosDir, photo.encryptedFileName)
            val decryptedBytes = cryptoManager.decryptFileToBytes(fullFile) ?: return@withContext false

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Export_${photo.originalFileName}")
                put(MediaStore.MediaColumns.MIME_TYPE, photo.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoLock")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false

            resolver.openOutputStream(uri)?.use { out ->
                out.write(decryptedBytes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // === BACKUP ARCHIVE OPERATIONS ===

    fun getAllBackups(): Flow<List<CloudBackupSnapshotEntity>> = dao.getAllBackups()

    suspend fun deleteBackup(backup: CloudBackupSnapshotEntity) = withContext(Dispatchers.IO) {
        if (backup.localFilePath != null) {
            File(backup.localFilePath).delete()
        }
        dao.deleteBackup(backup)
    }

    fun getPhotosDir(): File = photosDir
    fun getThumbsDir(): File = thumbsDir
    fun getBackupsDir(): File = backupsDir
}
