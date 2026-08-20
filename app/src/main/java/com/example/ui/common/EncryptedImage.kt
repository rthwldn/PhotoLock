package com.example.ui.common

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.repository.VaultRepository
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.VaultSurfaceVariant

/**
 * Renders an encrypted photo by decrypting its thumbnail or full bitmap on-the-fly.
 */
@Composable
fun EncryptedThumbnail(
    photo: EncryptedPhotoEntity?,
    repository: VaultRepository,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    var bitmap by remember(photo?.id) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(photo?.id) { mutableStateOf(true) }

    LaunchedEffect(photo?.id) {
        if (photo == null) {
            bitmap = null
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        bitmap = repository.getThumbnailBitmap(photo)
        isLoading = false
    }

    Box(
        modifier = modifier.background(VaultSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = Pair(bitmap, isLoading), label = "image_fade") { (bmp, loading) ->
            when {
                bmp != null -> {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = contentDescription ?: photo?.originalFileName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale
                    )
                }
                loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CyanPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Photo,
                        contentDescription = "Placeholder",
                        tint = CyanPrimary.copy(alpha = 0.4f),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
