package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Tracks encrypted cloud backup snapshots created by the user.
 */
@Entity(tableName = "cloud_backups")
data class CloudBackupSnapshotEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val backupName: String,
    val totalPhotos: Int,
    val totalAlbums: Int,
    val totalSizeBytes: Long,
    val checksumSha256: String,
    val cloudStatus: String = "SYNCED", // SYNCED, PENDING, LOCAL_ONLY
    val remoteLocation: String = "Cloud Vault://v1/snapshots/",
    val localFilePath: String? = null
)
