package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AlbumEntity
import com.example.data.local.CloudBackupSnapshotEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.local.VaultDao
import com.example.security.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Handles Encrypted Vault Backup creation, local export, archive sharing (e.g. Google Drive, USB, local files),
 * and importing/restoring from backup archives.
 */
class CloudBackupManager(
    private val context: Context,
    private val dao: VaultDao,
    private val repository: VaultRepository,
    private val cryptoManager: CryptoManager
) {

    sealed class BackupProgress {
        object Idle : BackupProgress()
        data class Running(val phase: String, val progressFraction: Float) : BackupProgress()
        data class Success(val snapshot: CloudBackupSnapshotEntity, val file: File) : BackupProgress()
        data class Error(val message: String) : BackupProgress()
    }

    sealed class RestoreProgress {
        object Idle : RestoreProgress()
        data class Running(val phase: String, val progressFraction: Float) : RestoreProgress()
        data class Success(val restoredAlbums: Int, val restoredPhotos: Int) : RestoreProgress()
        data class Error(val message: String) : RestoreProgress()
    }

    /**
     * Creates an encrypted backup archive (.vaultbackup) containing all albums and photos,
     * computes SHA-256 fingerprint, and saves the snapshot locally.
     */
    suspend fun createEncryptedCloudBackup(
        onProgress: (BackupProgress) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            onProgress(BackupProgress.Running("Příprava metadat...", 0.1f))

            val allAlbums = dao.getAllAlbumsList()
            val allPhotos = dao.getAllActivePhotosList()

            // Build metadata JSON
            val rootJson = JSONObject()
            rootJson.put("version", 1)
            rootJson.put("createdAt", System.currentTimeMillis())
            rootJson.put("totalPhotos", allPhotos.size)
            rootJson.put("totalAlbums", allAlbums.size)

            val albumsArray = JSONArray()
            for (album in allAlbums) {
                val obj = JSONObject().apply {
                    put("id", album.id)
                    put("parentId", album.parentId)
                    put("name", album.name)
                    put("description", album.description)
                    put("coverPhotoId", album.coverPhotoId)
                    put("colorHex", album.colorHex)
                    put("iconName", album.iconName)
                    put("createdAt", album.createdAt)
                    put("updatedAt", album.updatedAt)
                    put("isFavorite", album.isFavorite)
                }
                albumsArray.put(obj)
            }
            rootJson.put("albums", albumsArray)

            val photosArray = JSONArray()
            for (photo in allPhotos) {
                val obj = JSONObject().apply {
                    put("id", photo.id)
                    put("albumId", photo.albumId)
                    put("encryptedFileName", photo.encryptedFileName)
                    put("encryptedThumbFileName", photo.encryptedThumbFileName)
                    put("originalFileName", photo.originalFileName)
                    put("mimeType", photo.mimeType)
                    put("fileSizeBytes", photo.fileSizeBytes)
                    put("width", photo.width)
                    put("height", photo.height)
                    put("importedAt", photo.importedAt)
                    put("takenAt", photo.takenAt ?: 0L)
                    put("isFavorite", photo.isFavorite)
                    put("notes", photo.notes)
                    put("tags", photo.tags)
                }
                photosArray.put(obj)
            }
            rootJson.put("photos", photosArray)

            onProgress(BackupProgress.Running("Komprese a balení dat...", 0.3f))

            // Create ZIP package in temp file
            val tempZip = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(tempZip)).use { zos ->
                // Write encrypted metadata
                val metaBytes = rootJson.toString(2).toByteArray(Charsets.UTF_8)
                val encryptedMeta = cryptoManager.encryptBytes(metaBytes)
                zos.putNextEntry(ZipEntry("metadata.vault.enc"))
                zos.write(encryptedMeta)
                zos.closeEntry()

                // Add encrypted photos & thumbnails
                val photosDir = repository.getPhotosDir()
                val thumbsDir = repository.getThumbsDir()

                val total = allPhotos.size.coerceAtLeast(1)
                for ((index, photo) in allPhotos.withIndex()) {
                    val pFile = File(photosDir, photo.encryptedFileName)
                    if (pFile.exists()) {
                        zos.putNextEntry(ZipEntry("photos/${photo.encryptedFileName}"))
                        FileInputStream(pFile).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                    val tFile = File(thumbsDir, photo.encryptedThumbFileName)
                    if (tFile.exists()) {
                        zos.putNextEntry(ZipEntry("thumbs/${photo.encryptedThumbFileName}"))
                        FileInputStream(tFile).use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                    val frac = 0.3f + (0.4f * (index + 1) / total)
                    onProgress(BackupProgress.Running("Balení fotografií (${index + 1}/${allPhotos.size})...", frac))
                }
            }

            onProgress(BackupProgress.Running("Šifrování záložního archivu (AES-256)...", 0.8f))

            val zipBytes = FileInputStream(tempZip).use { it.readBytes() }
            tempZip.delete()

            // Double AES-256 seal for complete container
            val finalEncryptedArchive = cryptoManager.encryptBytes(zipBytes)
            val sha256 = cryptoManager.sha256(finalEncryptedArchive)

            val timeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            val fileTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFileName = "FotoTrezor_Zaloha_$fileTime.vaultbackup"
            val backupDest = File(repository.getBackupsDir(), backupFileName)
            FileOutputStream(backupDest).use { it.write(finalEncryptedArchive) }

            val snapshot = CloudBackupSnapshotEntity(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                backupName = "Záloha $timeFormat",
                totalPhotos = allPhotos.size,
                totalAlbums = allAlbums.size,
                totalSizeBytes = finalEncryptedArchive.size.toLong(),
                checksumSha256 = sha256.take(16).uppercase(),
                cloudStatus = "LOCAL_ARCHIVE",
                remoteLocation = "Exportovatelný archiv // SHA256-${sha256.take(8)}",
                localFilePath = backupDest.absolutePath
            )

            dao.insertBackup(snapshot)
            onProgress(BackupProgress.Success(snapshot, backupDest))
            backupDest
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(BackupProgress.Error("Chyba při tvorbě zálohy: ${e.localizedMessage}"))
            null
        }
    }

    /**
     * Restores albums and photos from an encrypted backup Uri (selected via SAF/Drive/Local Storage).
     */
    suspend fun restoreEncryptedBackupFromUri(
        uri: Uri,
        onProgress: (RestoreProgress) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(RestoreProgress.Running("Načítání záložního souboru...", 0.1f))

            val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.vaultbackup")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                onProgress(RestoreProgress.Error("Nepodařilo se otevřít vybraný soubor zálohy."))
                return@withContext false
            }

            val result = restoreEncryptedBackup(tempFile, onProgress)
            tempFile.delete()
            result
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(RestoreProgress.Error("Chyba při importu zálohy: ${e.localizedMessage}"))
            false
        }
    }

    /**
     * Restores albums and photos from an encrypted backup file.
     */
    suspend fun restoreEncryptedBackup(
        backupFile: File,
        onProgress: (RestoreProgress) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            onProgress(RestoreProgress.Running("Dešifrování kontejneru zálohy (AES-256)...", 0.2f))

            if (!backupFile.exists()) {
                onProgress(RestoreProgress.Error("Soubor zálohy nebyl nalezen"))
                return@withContext false
            }

            val encryptedContainer = FileInputStream(backupFile).use { it.readBytes() }
            val decryptedZipBytes = cryptoManager.decryptBytes(encryptedContainer)

            onProgress(RestoreProgress.Running("Rozbalování a ověřování integrity...", 0.4f))

            val tempZipFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.zip")
            FileOutputStream(tempZipFile).use { it.write(decryptedZipBytes) }

            var metaJson: JSONObject? = null
            val photosDir = repository.getPhotosDir()
            val thumbsDir = repository.getThumbsDir()

            ZipInputStream(FileInputStream(tempZipFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "metadata.vault.enc" -> {
                            val encMeta = zis.readBytes()
                            val plainMeta = cryptoManager.decryptBytes(encMeta)
                            metaJson = JSONObject(String(plainMeta, Charsets.UTF_8))
                        }
                        entry.name.startsWith("photos/") -> {
                            val fileName = entry.name.removePrefix("photos/")
                            val target = File(photosDir, fileName)
                            FileOutputStream(target).use { zis.copyTo(it) }
                        }
                        entry.name.startsWith("thumbs/") -> {
                            val fileName = entry.name.removePrefix("thumbs/")
                            val target = File(thumbsDir, fileName)
                            FileOutputStream(target).use { zis.copyTo(it) }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            tempZipFile.delete()

            if (metaJson == null) {
                onProgress(RestoreProgress.Error("Neplatná záloha: chybí metadata"))
                return@withContext false
            }

            onProgress(RestoreProgress.Running("Obnovování struktury alb...", 0.7f))

            val albumsArray = metaJson!!.optJSONArray("albums") ?: JSONArray()
            val restoredAlbums = mutableListOf<AlbumEntity>()
            for (i in 0 until albumsArray.length()) {
                val obj = albumsArray.getJSONObject(i)
                restoredAlbums.add(
                    AlbumEntity(
                        id = obj.getString("id"),
                        parentId = if (obj.isNull("parentId")) null else obj.optString("parentId", null),
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        coverPhotoId = if (obj.isNull("coverPhotoId")) null else obj.optString("coverPhotoId", null),
                        colorHex = obj.optString("colorHex", "#D0BCFF"),
                        iconName = obj.optString("iconName", "folder"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                )
            }
            dao.insertAlbums(restoredAlbums)

            onProgress(RestoreProgress.Running("Obnovování fotografií...", 0.85f))

            val photosArray = metaJson!!.optJSONArray("photos") ?: JSONArray()
            val restoredPhotos = mutableListOf<EncryptedPhotoEntity>()
            for (i in 0 until photosArray.length()) {
                val obj = photosArray.getJSONObject(i)
                restoredPhotos.add(
                    EncryptedPhotoEntity(
                        id = obj.getString("id"),
                        albumId = obj.getString("albumId"),
                        encryptedFileName = obj.getString("encryptedFileName"),
                        encryptedThumbFileName = obj.getString("encryptedThumbFileName"),
                        originalFileName = obj.getString("originalFileName"),
                        mimeType = obj.optString("mimeType", "image/jpeg"),
                        fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                        width = obj.optInt("width", 0),
                        height = obj.optInt("height", 0),
                        importedAt = obj.optLong("importedAt", System.currentTimeMillis()),
                        takenAt = if (obj.isNull("takenAt")) null else obj.optLong("takenAt"),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isTrash = false,
                        notes = obj.optString("notes", ""),
                        tags = obj.optString("tags", "")
                    )
                )
            }
            dao.insertPhotos(restoredPhotos)

            onProgress(RestoreProgress.Success(restoredAlbums.size, restoredPhotos.size))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            onProgress(RestoreProgress.Error("Chyba při obnově: ${e.localizedMessage}"))
            false
        }
    }

    /**
     * Shares backup file using Android system share sheet (Google Drive, Files, Email, Messaging, etc.)
     */
    fun shareBackupFile(file: File) {
        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Záloha Foto Trezoru")
                putExtra(Intent.EXTRA_TEXT, "Zašifrovaná záloha Foto Trezoru (AES-256).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Uložit nebo exportovat zálohu na...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
