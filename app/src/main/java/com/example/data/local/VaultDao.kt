package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    // === ALBUM QUERIES ===

    @Query("SELECT * FROM albums WHERE parentId IS NULL ORDER BY name ASC")
    fun getRootAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubAlbums(parentId: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE parentId = :parentId ORDER BY name ASC")
    suspend fun getSubAlbumsList(parentId: String): List<AlbumEntity>

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY name ASC")
    suspend fun getAllAlbumsList(): List<AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :id LIMIT 1")
    fun getAlbumByIdFlow(id: String): Flow<AlbumEntity?>

    @Query("SELECT * FROM albums WHERE id = :id LIMIT 1")
    suspend fun getAlbumById(id: String): AlbumEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: AlbumEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbums(albums: List<AlbumEntity>)

    @Update
    suspend fun updateAlbum(album: AlbumEntity)

    @Query("UPDATE albums SET coverPhotoId = :photoId, updatedAt = :timestamp WHERE id = :albumId")
    suspend fun updateAlbumCover(albumId: String, photoId: String?, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM albums WHERE id = :id")
    suspend fun deleteAlbumById(id: String)

    // === PHOTO QUERIES ===

    @Query("SELECT * FROM encrypted_photos WHERE albumId = :albumId AND isTrash = 0 ORDER BY importedAt DESC")
    fun getPhotosByAlbum(albumId: String): Flow<List<EncryptedPhotoEntity>>

    @Query("SELECT * FROM encrypted_photos WHERE albumId = :albumId AND isTrash = 0 ORDER BY importedAt DESC")
    suspend fun getPhotosByAlbumList(albumId: String): List<EncryptedPhotoEntity>

    @Query("SELECT * FROM encrypted_photos WHERE isTrash = 0 ORDER BY importedAt DESC")
    fun getAllActivePhotos(): Flow<List<EncryptedPhotoEntity>>

    @Query("SELECT * FROM encrypted_photos WHERE isTrash = 0 ORDER BY importedAt DESC")
    suspend fun getAllActivePhotosList(): List<EncryptedPhotoEntity>

    @Query("SELECT * FROM encrypted_photos WHERE isTrash = 0 AND isFavorite = 1 ORDER BY importedAt DESC")
    fun getFavoritePhotos(): Flow<List<EncryptedPhotoEntity>>

    @Query("SELECT * FROM encrypted_photos WHERE isTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashPhotos(): Flow<List<EncryptedPhotoEntity>>

    @Query("SELECT * FROM encrypted_photos WHERE isTrash = 1")
    suspend fun getTrashPhotosList(): List<EncryptedPhotoEntity>

    @Query("SELECT * FROM encrypted_photos WHERE id = :id LIMIT 1")
    fun getPhotoByIdFlow(id: String): Flow<EncryptedPhotoEntity?>

    @Query("SELECT * FROM encrypted_photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: String): EncryptedPhotoEntity?

    @Query("SELECT COUNT(*) FROM encrypted_photos WHERE albumId = :albumId AND isTrash = 0")
    fun getPhotoCountInAlbumFlow(albumId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM encrypted_photos WHERE albumId = :albumId AND isTrash = 0")
    suspend fun getPhotoCountInAlbum(albumId: String): Int

    @Query("SELECT COUNT(*) FROM encrypted_photos WHERE isTrash = 0")
    suspend fun getTotalPhotosCount(): Int

    @Query("SELECT SUM(fileSizeBytes) FROM encrypted_photos WHERE isTrash = 0")
    suspend fun getTotalStorageBytes(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: EncryptedPhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<EncryptedPhotoEntity>)

    @Update
    suspend fun updatePhoto(photo: EncryptedPhotoEntity)

    @Query("UPDATE encrypted_photos SET albumId = :newAlbumId WHERE id = :photoId")
    suspend fun movePhoto(photoId: String, newAlbumId: String)

    @Query("UPDATE encrypted_photos SET albumId = :newAlbumId WHERE id IN (:photoIds)")
    suspend fun movePhotos(photoIds: List<String>, newAlbumId: String)

    @Query("UPDATE encrypted_photos SET isFavorite = :isFavorite WHERE id = :photoId")
    suspend fun toggleFavorite(photoId: String, isFavorite: Boolean)

    @Query("UPDATE encrypted_photos SET isTrash = 1, trashTimestamp = :timestamp WHERE id = :photoId")
    suspend fun moveToTrash(photoId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE encrypted_photos SET isTrash = 1, trashTimestamp = :timestamp WHERE id IN (:photoIds)")
    suspend fun moveMultipleToTrash(photoIds: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE encrypted_photos SET isTrash = 0, trashTimestamp = NULL WHERE id = :photoId")
    suspend fun restoreFromTrash(photoId: String)

    @Query("UPDATE encrypted_photos SET isTrash = 0, trashTimestamp = NULL WHERE id IN (:photoIds)")
    suspend fun restoreMultipleFromTrash(photoIds: List<String>)

    @Query("DELETE FROM encrypted_photos WHERE id = :photoId")
    suspend fun permanentlyDeletePhoto(photoId: String)

    @Query("DELETE FROM encrypted_photos WHERE id IN (:photoIds)")
    suspend fun permanentlyDeletePhotos(photoIds: List<String>)

    @Query("DELETE FROM encrypted_photos WHERE isTrash = 1")
    suspend fun emptyTrash()

    // === BACKUP QUERIES ===

    @Query("SELECT * FROM cloud_backups ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<CloudBackupSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: CloudBackupSnapshotEntity)

    @Delete
    suspend fun deleteBackup(backup: CloudBackupSnapshotEntity)

    @Query("DELETE FROM cloud_backups WHERE id = :id")
    suspend fun deleteBackupById(id: String)
}
