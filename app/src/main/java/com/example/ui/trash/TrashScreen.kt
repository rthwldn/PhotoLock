package com.example.ui.trash

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.VaultViewModel
import com.example.ui.common.EncryptedThumbnail
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

@Composable
fun TrashScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val trashPhotos by viewModel.trashPhotos.collectAsStateWithLifecycle()
    val selectedPhotoIds by viewModel.selectedPhotoIds.collectAsStateWithLifecycle()

    var showEmptyTrashConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("trash_screen"),
        color = VaultBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Koš",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${trashPhotos.size} smazaných položek",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                if (trashPhotos.isNotEmpty()) {
                    Row {
                        if (selectedPhotoIds.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.restoreSelectedFromTrash() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ActionButtonBg,
                                    contentColor = ActionButtonText
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = ActionButtonText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Obnovit (${selectedPhotoIds.size})", color = ActionButtonText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showEmptyTrashConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = RedDanger),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vysypat koš", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (trashPhotos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Koš je prázdný",
                            color = TextMuted,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(trashPhotos, key = { it.id }) { photo ->
                        val isSelected = selectedPhotoIds.contains(photo.id)

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
                                    viewModel.togglePhotoSelection(photo.id)
                                }
                        ) {
                            EncryptedThumbnail(
                                photo = photo,
                                repository = viewModel.repository,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Selection check badge
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
                                        imageVector = Icons.Default.Check,
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

    if (showEmptyTrashConfirm) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirm = false },
            title = {
                Text("Trvale smazat všechny položky z koše?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Tato akce trvale skartuje zašifrované soubory z vašeho zařízení. Tuto akci nelze vrátit zpět.", color = TextSecondary)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEmptyTrashConfirm = false
                        viewModel.emptyTrash()
                        Toast.makeText(context, "Koš byl vysypán a soubory skartovány", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger)
                ) {
                    Text("Trvale smazat", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirm = false }) {
                    Text("Zrušit", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141418),
            modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
        )
    }
}
