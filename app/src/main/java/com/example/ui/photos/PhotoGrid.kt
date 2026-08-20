package com.example.ui.photos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.repository.VaultRepository
import com.example.ui.common.EncryptedThumbnail
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.VaultCardBorder

@Composable
fun PhotoGrid(
    photos: List<EncryptedPhotoEntity>,
    repository: VaultRepository,
    selectedIds: Set<String>,
    onPhotoClick: (photo: EncryptedPhotoEntity, index: Int) -> Unit,
    onPhotoLongClick: (photo: EncryptedPhotoEntity) -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = "V tomto albu zatím nejsou žádné fotky."
) {
    if (photos.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                color = TextMuted,
                fontSize = 14.sp
            )
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_grid")
    ) {
        items(photos, key = { it.id }) { photo ->
            val isSelected = selectedIds.contains(photo.id)
            val isSelectionMode = selectedIds.isNotEmpty()

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        if (isSelected) 3.dp else 0.5.dp,
                        if (isSelected) Color.White else Color(0x26FFFFFF),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        if (isSelectionMode) {
                            onPhotoLongClick(photo)
                        } else {
                            val index = photos.indexOf(photo)
                            onPhotoClick(photo, index)
                        }
                    }
                    .testTag("photo_item_${photo.id}")
            ) {
                EncryptedThumbnail(
                    photo = photo,
                    repository = repository,
                    modifier = Modifier.fillMaxSize()
                )

                // Selection checkmark or empty circle in selection mode
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Favorite badge
                if (photo.isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xB3000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = RedDanger,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Small AES encrypted indicator icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xB3000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}
