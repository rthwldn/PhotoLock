package com.example.ui.albums

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AlbumEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.ui.VaultViewModel
import com.example.ui.dialogs.CreateAlbumDialog
import com.example.ui.dialogs.ImportProgressDialog
import com.example.ui.dialogs.MovePhotosDialog
import com.example.ui.dialogs.SelectCoverDialog
import com.example.ui.photos.PhotoGrid
import com.example.ui.theme.ActionButtonBg
import com.example.ui.theme.ActionButtonText
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import com.example.util.GalleryHelper
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumHierarchyView(
    viewModel: VaultViewModel,
    onPhotoClick: (photo: EncryptedPhotoEntity, index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAlbum by viewModel.currentAlbum.collectAsStateWithLifecycle()
    val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
    val rootAlbums by viewModel.rootAlbums.collectAsStateWithLifecycle()
    val subAlbums by viewModel.currentSubAlbums.collectAsStateWithLifecycle()
    val photos by viewModel.currentAlbumPhotos.collectAsStateWithLifecycle()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var createParentAlbum by remember { mutableStateOf<AlbumEntity?>(null) }
    var showSelectCoverDialog by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }
    var showAlbumMenu by remember { mutableStateOf(false) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Fotografie byly smazány z galerie telefonu", Toast.LENGTH_SHORT).show()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val targetAlbumId = currentAlbum?.id ?: rootAlbums.firstOrNull()?.id
            if (targetAlbumId != null) {
                viewModel.importPhotos(uris, targetAlbumId) { importedUris ->
                    if (viewModel.securityPrefs.deleteAfterImport && importedUris.isNotEmpty()) {
                        scope.launch {
                            GalleryHelper.requestDeleteFromGallery(
                                context = context,
                                uris = importedUris,
                                onRequiresConsent = { request ->
                                    deleteConsentLauncher.launch(request)
                                },
                                onSuccess = { count ->
                                    if (count > 0) {
                                        Toast.makeText(
                                            context,
                                            "Fotky byly uloženy a smazány z galerie ($count)",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Fotky byly bezpečně zašifrovány",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onError = { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val isRoot = currentAlbum == null
    val displayAlbums = if (isRoot) rootAlbums else subAlbums

    Scaffold(
        topBar = {
            if (selectedPhotoIds.isNotEmpty()) {
                // Multi-selection Contextual Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "Vybráno: ${selectedPhotoIds.size}",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Zrušit výběr", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllPhotos(photos) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Vybrat vše", tint = Color.White)
                        }
                        IconButton(onClick = { showMoveDialog = true }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Přesunout", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.moveSelectedToTrash() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Do koše", tint = RedDanger)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F12))
                )
            } else {
                // Standard Hierarchy Bar with Sophisticated Dark Branding
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isRoot) "Trezor" else (currentAlbum?.name ?: "Album"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = if (isRoot) "AES-256 ŠIFROVÁNÍ" else "${photos.size} fotek",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (!isRoot) {
                            IconButton(onClick = {
                                viewModel.navigateToAlbum(currentAlbum?.parentId)
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Zpět",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isRoot && currentAlbum != null) {
                            Box {
                                IconButton(onClick = { showAlbumMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Možnosti", tint = Color.White)
                                }

                                DropdownMenu(
                                    expanded = showAlbumMenu,
                                    onDismissRequest = { showAlbumMenu = false },
                                    modifier = Modifier
                                        .background(Color(0xFF141418))
                                        .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Přidat pod-album", color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = Color.White)
                                        },
                                        onClick = {
                                            showAlbumMenu = false
                                            createParentAlbum = currentAlbum
                                            showCreateAlbumDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Zvolit obal alba", color = Color.White) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White)
                                        },
                                        onClick = {
                                            showAlbumMenu = false
                                            showSelectCoverDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Smazat toto album", color = RedDanger) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = RedDanger)
                                        },
                                        onClick = {
                                            showAlbumMenu = false
                                            viewModel.deleteCurrentAlbum {}
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = VaultBackground)
                )
            }
        },
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // New Album / Sub-album button
                FloatingActionButton(
                    onClick = {
                        createParentAlbum = currentAlbum
                        showCreateAlbumDialog = true
                    },
                    containerColor = Color(0x24FFFFFF),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                        .testTag("create_album_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Nové album / pod-album",
                        tint = Color.White
                    )
                }

                // Import photos button
                FloatingActionButton(
                    onClick = {
                        if (isRoot && rootAlbums.isEmpty()) {
                            createParentAlbum = null
                            showCreateAlbumDialog = true
                        } else {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    containerColor = ActionButtonBg,
                    contentColor = ActionButtonText,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.testTag("import_photos_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Importovat fotky",
                        tint = ActionButtonText
                    )
                }
            }
        },
        containerColor = VaultBackground
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Breadcrumbs Navigation Bar (e.g. Trezor > Dovolená > Pláž)
            if (breadcrumbs.isNotEmpty() || !isRoot) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0C0C0E))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Root Item
                    Text(
                        text = "Trezor",
                        color = if (isRoot) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.navigateToAlbum(null) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    breadcrumbs.forEachIndexed { index, bAlbum ->
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier
                                .size(10.dp)
                                .padding(horizontal = 2.dp)
                        )

                        val isLast = index == breadcrumbs.size - 1
                        Text(
                            text = bAlbum.name,
                            color = if (isLast) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { viewModel.navigateToAlbum(bAlbum.id) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Main Content Area
            if (isRoot) {
                // Root view: Shows all top-level albums + quick actions
                if (rootAlbums.isEmpty()) {
                    // Empty State for Root
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(VaultSurfaceVariant)
                                    .border(1.dp, CyanPrimary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoAlbum,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Váš trezor je prázdný",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Vytvořte své první šifrované album a importujte soukromé fotografie z galerie.",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    createParentAlbum = null
                                    showCreateAlbumDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ActionButtonBg,
                                    contentColor = ActionButtonText
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = ActionButtonText, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Vytvořit první album", color = ActionButtonText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(rootAlbums, key = { it.id }) { album ->
                            AlbumCard(
                                album = album,
                                repository = viewModel.repository,
                                onClick = { viewModel.navigateToAlbum(album.id) },
                                onAddSubAlbum = {
                                    createParentAlbum = album
                                    showCreateAlbumDialog = true
                                },
                                onChangeCover = {
                                    viewModel.navigateToAlbum(album.id)
                                    showSelectCoverDialog = true
                                },
                                onDelete = { viewModel.deleteAlbumById(album.id) }
                            )
                        }
                    }
                }
            } else {
                // Inside an album: Shows Sub-albums section + Photos Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sub-albums Header and Horizontal Carousel / Grid
                    if (subAlbums.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pod-alba (${subAlbums.size})",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "+ Přidat",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable {
                                                createParentAlbum = currentAlbum
                                                showCreateAlbumDialog = true
                                            }
                                            .padding(4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(subAlbums, key = { it.id }) { sub ->
                                        Box(modifier = Modifier.width(150.dp)) {
                                            AlbumCard(
                                                album = sub,
                                                repository = viewModel.repository,
                                                onClick = { viewModel.navigateToAlbum(sub.id) },
                                                onAddSubAlbum = {
                                                    createParentAlbum = sub
                                                    showCreateAlbumDialog = true
                                                },
                                                onChangeCover = {
                                                    viewModel.navigateToAlbum(sub.id)
                                                    showSelectCoverDialog = true
                                                },
                                                onDelete = { viewModel.deleteAlbumById(sub.id) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Fotografie (${photos.size})",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Photos items
                    if (photos.isEmpty() && subAlbums.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = CyanPrimary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Zatím žádné fotky v tomto albu", color = TextSecondary, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ActionButtonBg,
                                            contentColor = ActionButtonText
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = ActionButtonText, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Importovat fotky z Galerie", color = ActionButtonText, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        items(photos, key = { it.id }) { photo ->
                            val isSelected = selectedPhotoIds.contains(photo.id)
                            val isSelectionMode = selectedPhotoIds.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        if (isSelected) 3.dp else 0.5.dp,
                                        if (isSelected) Color.White else Color(0x26FFFFFF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isSelectionMode) {
                                            viewModel.togglePhotoSelection(photo.id)
                                        } else {
                                            val index = photos.indexOf(photo)
                                            onPhotoClick(photo, index)
                                        }
                                    }
                            ) {
                                com.example.ui.common.EncryptedThumbnail(
                                    photo = photo,
                                    repository = viewModel.repository,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isSelectionMode) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color.Black.copy(alpha = 0.6f))
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAlbumDialog) {
        CreateAlbumDialog(
            parentAlbum = createParentAlbum,
            onDismiss = { showCreateAlbumDialog = false },
            onCreate = { name, desc, color ->
                viewModel.createAlbum(name, desc, createParentAlbum?.id, color)
                showCreateAlbumDialog = false
            }
        )
    }

    if (showSelectCoverDialog && currentAlbum != null) {
        SelectCoverDialog(
            album = currentAlbum!!,
            photos = photos,
            repository = viewModel.repository,
            onDismiss = { showSelectCoverDialog = false },
            onPhotoSelected = { photoId ->
                viewModel.setAlbumCover(currentAlbum!!.id, photoId)
                showSelectCoverDialog = false
            }
        )
    }

    if (showMoveDialog) {
        MovePhotosDialog(
            allAlbums = allAlbums,
            currentAlbumId = currentAlbum?.id,
            onDismiss = { showMoveDialog = false },
            onAlbumSelected = { targetId ->
                viewModel.moveSelectedPhotos(targetId)
                showMoveDialog = false
            }
        )
    }

    ImportProgressDialog(state = importProgress)
}
