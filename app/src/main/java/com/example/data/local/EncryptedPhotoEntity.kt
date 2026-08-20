package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Encrypted Photo Entity.
 * The physical payload on disk is encrypted with AES-256 GCM.
 */
@Entity(
    tableName = "encrypted_photos",
    indices = [
        Index(value = ["albumId"]),
        Index(value = ["isTrash"]),
        Index(value = ["isFavorite"])
    ]
)
data class EncryptedPhotoEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val albumId: String, // The album/sub-album it belongs to
    val encryptedFileName: String, // Relative filename in filesDir/vault_photos/
    val encryptedThumbFileName: String, // Relative filename in filesDir/vault_thumbs/
    val originalFileName: String,
    val mimeType: String = "image/jpeg",
    val fileSizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val importedAt: Long = System.currentTimeMillis(),
    val takenAt: Long? = null,
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val trashTimestamp: Long? = null,
    val notes: String = "",
    val tags: String = ""
)
