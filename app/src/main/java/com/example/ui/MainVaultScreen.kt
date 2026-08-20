package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.EncryptedPhotoEntity
import com.example.ui.albums.AlbumHierarchyView
import com.example.ui.backup.CloudBackupScreen
import com.example.ui.photos.PhotoGrid
import com.example.ui.photos.PhotoViewerScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultSurface
import com.example.ui.trash.TrashScreen

@Composable
fun MainVaultScreen(
    viewModel: VaultViewModel,
    onLockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allActivePhotos.collectAsStateWithLifecycle()
    val favoritePhotos by viewModel.favoritePhotos.collectAsStateWithLifecycle()
    val currentAlbumPhotos by viewModel.currentAlbumPhotos.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val currentAlbum by viewModel.currentAlbum.collectAsStateWithLifecycle()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsStateWithLifecycle()

    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Fullscreen Photo Viewer state: track the current browsing context dynamically
    var viewerSource by remember { mutableStateOf<String?>(null) } // "ALBUMS", "ALL_PHOTOS", "FAVORITES"
    var viewingPhotoId by remember { mutableStateOf<String?>(null) }
    var initialPhotoIndex by remember { mutableIntStateOf(0) }

    val activeViewerPhotos = when (viewerSource) {
        "ALBUMS" -> currentAlbumPhotos
        "ALL_PHOTOS" -> allPhotos
        "FAVORITES" -> favoritePhotos
        else -> null
    }

    // System Back Gesture handling
    BackHandler {
        if (viewerSource != null) {
            viewerSource = null
            viewingPhotoId = null
        } else if (selectedPhotoIds.isNotEmpty()) {
            viewModel.clearSelection()
        } else if (currentTab == VaultTab.ALBUMS && currentAlbum != null) {
            viewModel.navigateToAlbum(currentAlbum?.parentId)
        } else if (currentTab != VaultTab.ALBUMS) {
            viewModel.selectTab(VaultTab.ALBUMS)
        } else {
            // On Root Albums screen: move task to background instead of killing process
            val activity = context as? Activity
            activity?.moveTaskToBack(true)
        }
    }

    if (viewerSource != null && activeViewerPhotos != null && activeViewerPhotos.isNotEmpty()) {
        val targetIndex = remember(viewerSource, viewingPhotoId) {
            val idx = activeViewerPhotos.indexOfFirst { it.id == viewingPhotoId }
            if (idx >= 0) idx else initialPhotoIndex.coerceIn(0, (activeViewerPhotos.size - 1).coerceAtLeast(0))
        }
        PhotoViewerScreen(
            photos = activeViewerPhotos,
            initialIndex = targetIndex,
            currentAlbum = currentAlbum,
            allAlbums = allAlbums,
            viewModel = viewModel,
            onBack = {
                viewerSource = null
                viewingPhotoId = null
            }
        )
        return
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x66000000),
                                Color(0xCC000000),
                                Color(0xFF000000)
                            )
                        )
                    )
                    .padding(top = 10.dp)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .testTag("vault_bottom_navigation")
                ) {
                    NavigationBarItem(
                        selected = currentTab == VaultTab.ALBUMS,
                        onClick = { viewModel.selectTab(VaultTab.ALBUMS) },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Alba") },
                        label = { Text("Alba", fontSize = 10.5.sp) },
                        colors = navBarColors(),
                        modifier = Modifier.testTag("nav_albums")
                    )
                    NavigationBarItem(
                        selected = currentTab == VaultTab.ALL_PHOTOS,
                        onClick = { viewModel.selectTab(VaultTab.ALL_PHOTOS) },
                        icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Fotky") },
                        label = { Text("Fotky", fontSize = 10.5.sp) },
                        colors = navBarColors(),
                        modifier = Modifier.testTag("nav_all_photos")
                    )
                    NavigationBarItem(
                        selected = currentTab == VaultTab.FAVORITES,
                        onClick = { viewModel.selectTab(VaultTab.FAVORITES) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Oblíbené") },
                        label = { Text("Oblíbené", fontSize = 10.5.sp) },
                        colors = navBarColors(),
                        modifier = Modifier.testTag("nav_favorites")
                    )
                    NavigationBarItem(
                        selected = currentTab == VaultTab.BACKUP,
                        onClick = { viewModel.selectTab(VaultTab.BACKUP) },
                        icon = { Icon(Icons.Default.FolderZip, contentDescription = "Záloha") },
                        label = { Text("Záloha", fontSize = 10.5.sp) },
                        colors = navBarColors(),
                        modifier = Modifier.testTag("nav_backup")
                    )
                    NavigationBarItem(
                        selected = currentTab == VaultTab.TRASH,
                        onClick = { viewModel.selectTab(VaultTab.TRASH) },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Koš") },
                        label = { Text("Koš", fontSize = 10.5.sp) },
                        colors = navBarColors(),
                        modifier = Modifier.testTag("nav_trash")
                    )
                    NavigationBarItem(
                        selected = currentTab == VaultTab.SETTINGS,
                        onClick = { viewModel.selectTab(VaultTab.SETTINGS) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Nastavení") },
                        label = { Text("Nastavení", fontSize = 10.5.sp) },
                        colors = navBarColors(),
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        },
        containerColor = VaultBackground
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { tab ->
                when (tab) {
                    VaultTab.ALBUMS -> {
                        AlbumHierarchyView(
                            viewModel = viewModel,
                            onPhotoClick = { photo, index ->
                                viewerSource = "ALBUMS"
                                viewingPhotoId = photo.id
                                initialPhotoIndex = index
                            }
                        )
                    }
                    VaultTab.ALL_PHOTOS -> {
                        PhotoGrid(
                            photos = allPhotos,
                            repository = viewModel.repository,
                            selectedIds = viewModel.selectedPhotoIds.collectAsStateWithLifecycle().value,
                            onPhotoClick = { photo, index ->
                                viewerSource = "ALL_PHOTOS"
                                viewingPhotoId = photo.id
                                initialPhotoIndex = index
                            },
                            onPhotoLongClick = { photo ->
                                viewModel.togglePhotoSelection(photo.id)
                            },
                            emptyMessage = "Zatím žádné zašifrované fotky. Přejděte do Alb a importujte své první fotografie."
                        )
                    }
                    VaultTab.FAVORITES -> {
                        PhotoGrid(
                            photos = favoritePhotos,
                            repository = viewModel.repository,
                            selectedIds = viewModel.selectedPhotoIds.collectAsStateWithLifecycle().value,
                            onPhotoClick = { photo, index ->
                                viewerSource = "FAVORITES"
                                viewingPhotoId = photo.id
                                initialPhotoIndex = index
                            },
                            onPhotoLongClick = { photo ->
                                viewModel.togglePhotoSelection(photo.id)
                            },
                            emptyMessage = "Zatím nemáte žádné oblíbené fotografie. Označte fotky srdíčkem při prohlížení."
                        )
                    }
                    VaultTab.BACKUP -> {
                        CloudBackupScreen(viewModel = viewModel)
                    }
                    VaultTab.TRASH -> {
                        TrashScreen(viewModel = viewModel)
                    }
                    VaultTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onLockNow = {
                                viewModel.lockVault()
                                onLockVault()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun navBarColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.White,
    selectedTextColor = Color.White,
    unselectedIconColor = Color(0x66FFFFFF),
    unselectedTextColor = Color(0x66FFFFFF),
    indicatorColor = Color.Transparent
)
