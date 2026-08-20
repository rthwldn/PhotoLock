package com.example.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlbumEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.repository.VaultRepository
import com.example.ui.common.EncryptedThumbnail
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant

@Composable
fun AlbumCard(
    album: AlbumEntity,
    repository: VaultRepository,
    onClick: () -> Unit,
    onAddSubAlbum: () -> Unit,
    onChangeCover: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var coverPhoto by remember(album.coverPhotoId) { mutableStateOf<EncryptedPhotoEntity?>(null) }
    var photoCount by remember(album.id) { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(album.coverPhotoId, album.id) {
        if (album.coverPhotoId != null) {
            coverPhoto = repository.getPhotoById(album.coverPhotoId)
        }
        photoCount = repository.getPhotoCountInAlbum(album.id)
    }

    val accentColor = remember(album.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(album.colorHex))
        } catch (e: Exception) {
            CyanPrimary
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0x29FFFFFF), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("album_card_${album.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F0F12)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image Box with gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(136.dp)
                    .background(Color(0xFF141418))
            ) {
                if (coverPhoto != null) {
                    EncryptedThumbnail(
                        photo = coverPhoto,
                        repository = repository,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x1AFFFFFF),
                                        Color(0x08FFFFFF)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Dark gradient overlay at bottom for crisp text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                startY = 60f
                            )
                        )
                )

                // Top Accent Tag & Overflow Menu
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Accent pill
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF141418))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Vytvořit pod-album", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color.White)
                                },
                                onClick = {
                                    showMenu = false
                                    onAddSubAlbum()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Změnit náhledovou fotku", color = Color.White) },
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                                },
                                onClick = {
                                    showMenu = false
                                    onChangeCover()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Smazat album", color = Color(0xFFEF4444)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444))
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }

                // Photo count badge at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x99000000))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$photoCount položek",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Info Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = album.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (album.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = album.description,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
