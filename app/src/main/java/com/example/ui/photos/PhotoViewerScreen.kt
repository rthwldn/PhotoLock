package com.example.ui.photos

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AlbumEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.repository.VaultRepository
import com.example.ui.VaultViewModel
import com.example.ui.dialogs.MovePhotosDialog
import com.example.ui.dialogs.PhotoDetailsDialog
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackground
import kotlinx.coroutines.launch

@Composable
fun PhotoViewerScreen(
    photos: List<EncryptedPhotoEntity>,
    initialIndex: Int,
    currentAlbum: AlbumEntity?,
    allAlbums: List<AlbumEntity>,
    viewModel: VaultViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialPage = initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = initialPage) {
        photos.size
    }

    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    // Map for instantaneous optimistic UI update when tapping favorite
    var optimisticFavorites by remember { mutableStateOf(mapOf<String, Boolean>()) }

    // Track active page zoom scale to allow/disable pager scrolling
    var isCurrentPageZoomed by remember { mutableStateOf(false) }

    val currentPhoto = photos.getOrNull(pagerState.currentPage)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("photo_viewer_screen"),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Fotografie nebyla nalezena", color = Color.White)
                }
                return@Surface
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                ZoomableDecryptedImage(
                    photo = photo,
                    repository = viewModel.repository,
                    onTap = { showControls = !showControls },
                    onZoomChanged = { zoomed ->
                        if (page == pagerState.currentPage) {
                            isCurrentPageZoomed = zoomed
                        }
                    }
                )
            }

            // Quick navigation side buttons for ease of browsing
            if (showControls && photos.size > 1) {
                // Previous button
                if (pagerState.currentPage > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                                contentDescription = "Předchozí",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Next button
                if (pagerState.currentPage < photos.size - 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    ) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color.Black.copy(alpha = 0.5f),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                                contentDescription = "Další",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            // Top Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zpět",
                            tint = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photos.size}",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = " AES-256 GCM",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color.White
                            )
                        }

                        if (currentPhoto != null) {
                            val isFav = optimisticFavorites[currentPhoto.id] ?: currentPhoto.isFavorite
                            IconButton(
                                onClick = {
                                    val newFav = !isFav
                                    optimisticFavorites = optimisticFavorites + (currentPhoto.id to newFav)
                                    viewModel.toggleFavorite(currentPhoto.id, newFav)
                                },
                                modifier = Modifier.testTag("viewer_favorite_button")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFav) RedDanger else Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Floating Action Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set as Album Cover
                    if (currentPhoto != null && currentAlbum != null) {
                        ViewerActionButton(
                            icon = Icons.Default.PhotoAlbum,
                            label = "Obal alba",
                            onClick = {
                                viewModel.setAlbumCover(currentAlbum.id, currentPhoto.id)
                                Toast.makeText(context, "Nastaveno jako náhledová fotka alba", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    // Move to Sub-album / Album
                    if (currentPhoto != null) {
                        ViewerActionButton(
                            icon = Icons.Default.DriveFileMove,
                            label = "Přesunout",
                            onClick = { showMoveDialog = true }
                        )
                    }

                    // Export to Gallery
                    if (currentPhoto != null) {
                        ViewerActionButton(
                            icon = Icons.Default.FileDownload,
                            label = "Exportovat",
                            onClick = {
                                viewModel.exportPhotoToGallery(currentPhoto) { success ->
                                    scope.launch {
                                        if (success) {
                                            Toast.makeText(context, "Fotka dešifrována a exportována do Galerie", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Export se nezdařil", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Delete to Trash
                    if (currentPhoto != null) {
                        ViewerActionButton(
                            icon = Icons.Default.Delete,
                            label = "Smazat",
                            tint = RedDanger,
                            onClick = {
                                viewModel.moveSingleToTrash(currentPhoto.id)
                                Toast.makeText(context, "Přesunuto do koše", Toast.LENGTH_SHORT).show()
                                if (photos.size <= 1) {
                                    onBack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog && currentPhoto != null) {
        PhotoDetailsDialog(
            photo = currentPhoto,
            onDismiss = { showInfoDialog = false },
            onSaveNote = { note -> viewModel.updatePhotoNote(currentPhoto.id, note) }
        )
    }

    if (showMoveDialog && currentPhoto != null) {
        MovePhotosDialog(
            allAlbums = allAlbums,
            currentAlbumId = currentPhoto.albumId,
            onDismiss = { showMoveDialog = false },
            onAlbumSelected = { targetId ->
                viewModel.moveSinglePhoto(currentPhoto.id, targetId)
                showMoveDialog = false
                Toast.makeText(context, "Fotka přesunuta", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ZoomableDecryptedImage(
    photo: EncryptedPhotoEntity,
    repository: VaultRepository,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    var bitmap by remember(photo.id) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(photo.id) { mutableStateOf(true) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(photo.id) {
        isLoading = true
        bitmap = repository.getFullBitmap(photo)
        isLoading = false
    }

    LaunchedEffect(scale) {
        onZoomChanged(scale > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(photo.id) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .then(
                if (scale > 1.05f) {
                    Modifier.pointerInput(photo.id, scale) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale > 1f) {
                                val maxOffsetX = (newScale - 1) * 500
                                val maxOffsetY = (newScale - 1) * 700
                                val newOffset = offset + pan
                                offset = Offset(
                                    newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                                    newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                                )
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = photo.originalFileName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
        } else if (isLoading) {
            CircularProgressIndicator(color = Color.White)
        } else {
            Text("Chyba při dešifrování", color = Color.White)
        }
    }
}

@Composable
fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp
        )
    }
}
