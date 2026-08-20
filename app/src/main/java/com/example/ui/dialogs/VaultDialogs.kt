package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AlbumEntity
import com.example.data.local.EncryptedPhotoEntity
import com.example.data.repository.VaultRepository
import com.example.ui.ImportProgressState
import com.example.ui.common.EncryptedThumbnail
import com.example.ui.theme.ActionButtonBg
import com.example.ui.theme.ActionButtonText
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CreateAlbumDialog(
    parentAlbum: AlbumEntity?,
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0284C7") }
    var error by remember { mutableStateOf<String?>(null) }

    val colors = listOf("#0284C7", "#10B981", "#8B5CF6", "#F59E0B", "#EF4444", "#EC4899")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (parentAlbum != null) "Nové pod-album" else "Nové album",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                if (parentAlbum != null) {
                    Text(
                        text = "V nadřazeném albu: ${parentAlbum.name}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("Název alba") },
                    placeholder = { Text("např. Dovolená 2025") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("album_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Popis (volitelné)") },
                    placeholder = { Text("Krátký popis alba...") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Color.White
                    )
                )

                Text("Barva štítku:", color = TextSecondary, fontSize = 13.sp)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { hex ->
                        val isSelected = hex == selectedColor
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) Color.White else Color.Transparent,
                                    CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(error!!, color = RedDanger, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        error = "Zadejte prosím název alba"
                    } else {
                        onCreate(name.trim(), description.trim(), selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionButtonBg,
                    contentColor = ActionButtonText
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_create_album_button")
            ) {
                Text("Vytvořit", color = ActionButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF141418),
        modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
    )
}

@Composable
fun MovePhotosDialog(
    allAlbums: List<AlbumEntity>,
    currentAlbumId: String?,
    onDismiss: () -> Unit,
    onAlbumSelected: (targetAlbumId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Přesunout do alba", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 350.dp)) {
                Text("Vyberte cílové album nebo pod-album:", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                if (allAlbums.isEmpty()) {
                    Text("Žádná jiná alba nejsou k dispozici.", color = TextMuted, fontSize = 14.sp)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allAlbums) { album ->
                            val isCurrent = album.id == currentAlbumId
                            val isSub = album.parentId != null

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) VaultSurfaceVariant.copy(alpha = 0.5f) else VaultSurfaceVariant)
                                    .clickable(enabled = !isCurrent) {
                                        onAlbumSelected(album.id)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSub) {
                                    Icon(
                                        imageVector = Icons.Default.SubdirectoryArrowRight,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = Color(android.graphics.Color.parseColor(album.colorHex)),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        color = if (isCurrent) TextMuted else TextPrimary,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    if (isCurrent) {
                                        Text("Současné album", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF141418),
        modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
    )
}

@Composable
fun SelectCoverDialog(
    album: AlbumEntity,
    photos: List<EncryptedPhotoEntity>,
    repository: VaultRepository,
    onDismiss: () -> Unit,
    onPhotoSelected: (photoId: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Zvolit náhledovou fotku alba", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 400.dp)) {
                Text("Klepněte na fotografii, kterou chcete nastavit jako obal alba \"${album.name}\":", color = TextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))

                if (photos.isEmpty()) {
                    Text("V tomto albu zatím nejsou žádné fotky. Nejprve nějaké importujte.", color = TextMuted, fontSize = 13.sp)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(photos) { photo ->
                            val isCover = photo.id == album.coverPhotoId
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        if (isCover) 3.dp else 1.dp,
                                        if (isCover) Color.White else Color(0x2EFFFFFF),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onPhotoSelected(photo.id) }
                            ) {
                                EncryptedThumbnail(
                                    photo = photo,
                                    repository = repository,
                                    modifier = Modifier.fillMaxWidth().height(90.dp)
                                )

                                if (isCover) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Cover",
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF141418),
        modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
    )
}

@Composable
fun ImportProgressDialog(
    state: ImportProgressState
) {
    if (!state.isImporting) return

    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF141418),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Šifrování a import",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = state.message,
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                val progress = if (state.totalItems > 0) {
                    state.currentItem.toFloat() / state.totalItems.toFloat()
                } else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color(0x24FFFFFF)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${state.currentItem} z ${state.totalItems} položek",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PhotoDetailsDialog(
    photo: EncryptedPhotoEntity,
    onDismiss: () -> Unit,
    onSaveNote: (note: String) -> Unit
) {
    var noteText by remember { mutableStateOf(photo.notes) }
    val dateFormat = SimpleDateFormat("dd. MMMM yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(photo.importedAt))

    val sizeMb = "%.2f MB".format(photo.fileSizeBytes / (1024.0 * 1024.0))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Informace o fotografii", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailRow(label = "Původní název:", value = photo.originalFileName)
                DetailRow(label = "Rozlišení:", value = "${photo.width} × ${photo.height} px")
                DetailRow(label = "Velikost šifrovaná:", value = sizeMb)
                DetailRow(label = "Datum importu:", value = dateString)
                DetailRow(label = "Šifrování:", value = "AES-256-GCM (Hardware Keystore)")
                DetailRow(label = "Status zabezpečení:", value = "Privátní uzamčeno")

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Poznámka k fotografii") },
                    placeholder = { Text("Zadejte tajnou poznámku...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveNote(noteText)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionButtonBg,
                    contentColor = ActionButtonText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Uložit", color = ActionButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zavřít", color = TextSecondary)
            }
        },
        containerColor = Color(0xFF141418),
        modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, color = TextSecondary, fontSize = 12.sp)
        Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
