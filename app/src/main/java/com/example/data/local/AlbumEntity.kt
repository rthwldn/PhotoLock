package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Album Entity supporting deep recursive sub-albums hierarchy.
 */
@Entity(
    tableName = "albums",
    indices = [Index(value = ["parentId"])]
)
data class AlbumEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null, // null for root-level albums, parent album id for sub-albums
    val name: String,
    val description: String = "",
    val coverPhotoId: String? = null, // Chosen thumbnail photo for this album
    val colorHex: String = "#0284C7", // Cyan/Sky blue default accent
    val iconName: String = "folder",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
