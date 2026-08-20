package com.example.ui.backup

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CloudBackupSnapshotEntity
import com.example.data.repository.CloudBackupManager
import com.example.ui.VaultViewModel
import com.example.ui.theme.ActionButtonBg
import com.example.ui.theme.ActionButtonText
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.LilacPrimary
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CloudBackupScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backups by viewModel.cloudBackups.collectAsStateWithLifecycle()
    val backupProgress by viewModel.backupProgress.collectAsStateWithLifecycle()
    val restoreProgress by viewModel.restoreProgress.collectAsStateWithLifecycle()

    var selectedSnapshotToRestore by remember { mutableStateOf<CloudBackupSnapshotEntity?>(null) }
    var showConfirmRestoreDialog by remember { mutableStateOf(false) }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var showConfirmUriRestoreDialog by remember { mutableStateOf(false) }

    // Launcher for selecting a backup file from Google Drive, Downloads, or Local Files
    val importFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImportUri = uri
            showConfirmUriRestoreDialog = true
        }
    }

    val lastBackupDate = remember(viewModel.securityPrefs.lastBackupTimestamp) {
        if (viewModel.securityPrefs.lastBackupTimestamp > 0) {
            SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(viewModel.securityPrefs.lastBackupTimestamp))
        } else {
            "Zatím neproběhla"
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("backup_screen"),
        color = VaultBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x1FFFFFFF))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Záloha a export trezoru",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Zašifrovaný offline archiv • AES-256",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Zero Knowledge Security Badge Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121216))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "100% soukromí bez cizího cloudu",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Záloha vytvoří jediný bezpečný zašifrovaný archiv (.vaultbackup). Můžete si sami vybrat, kam jej uložíte – na svůj Google Drive, USB disk, do úložiště nebo poslat e-mailem.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Quick Backup & Restore Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121216))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Poslední vytvořená záloha:", color = TextSecondary, fontSize = 13.sp)
                                Text(lastBackupDate, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Archiv připraven", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        // Create & Export Backup Button
                        Button(
                            onClick = {
                                viewModel.createEncryptedCloudBackup { createdFile ->
                                    viewModel.shareBackup(createdFile)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("create_backup_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ActionButtonBg,
                                contentColor = ActionButtonText
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, tint = ActionButtonText, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vytvořit a exportovat zálohu", color = ActionButtonText, fontWeight = FontWeight.Bold)
                        }

                        // Import Backup Button
                        OutlinedButton(
                            onClick = {
                                importFilePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_backup_button"),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importovat zálohu (z Drive / souborů)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Snapshots History Section
            item {
                Text(
                    text = "Lokální archívy záloh v zařízení (${backups.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (backups.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Zatím nemáte vytvořenou žádnou zálohu.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(backups, key = { it.id }) { snapshot ->
                    BackupSnapshotCard(
                        snapshot = snapshot,
                        onShare = {
                            if (snapshot.localFilePath != null) {
                                val file = File(snapshot.localFilePath)
                                if (file.exists()) {
                                    viewModel.shareBackup(file)
                                } else {
                                    Toast.makeText(context, "Soubor zálohy nenalezen", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onRestore = {
                            selectedSnapshotToRestore = snapshot
                            showConfirmRestoreDialog = true
                        },
                        onDelete = {
                            viewModel.deleteBackup(snapshot)
                            Toast.makeText(context, "Záloha smazána", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Backup Creation Progress Modal
    if (backupProgress is CloudBackupManager.BackupProgress.Running) {
        val state = backupProgress as CloudBackupManager.BackupProgress.Running
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
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tvorba zašifrované zálohy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(state.phase, color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.progressFraction },
                        color = Color.White,
                        trackColor = Color(0x26FFFFFF),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    } else if (backupProgress is CloudBackupManager.BackupProgress.Success) {
        LaunchedEffect(backupProgress) {
            Toast.makeText(context, "Záloha byla úspěšně vygenerována!", Toast.LENGTH_LONG).show()
            viewModel.resetBackupProgress()
        }
    }

    // Snapshot Restore Confirmation Dialog
    if (showConfirmRestoreDialog && selectedSnapshotToRestore != null) {
        val snap = selectedSnapshotToRestore!!
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(snap.timestamp))

        AlertDialog(
            onDismissRequest = { showConfirmRestoreDialog = false },
            title = {
                Text("Obnovit data ze zálohy?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tato akce dešifruje a obnoví obsah ze zálohy:", color = TextSecondary, fontSize = 13.sp)
                    Text("• Datum zálohy: $dateStr", color = TextPrimary, fontSize = 13.sp)
                    Text("• Fotografií: ${snap.totalPhotos}", color = TextPrimary, fontSize = 13.sp)
                    Text("• Alb: ${snap.totalAlbums}", color = TextPrimary, fontSize = 13.sp)
                    Text("• Kontrolní součet: ${snap.checksumSha256}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmRestoreDialog = false
                        if (snap.localFilePath != null) {
                            val file = File(snap.localFilePath)
                            viewModel.restoreBackupFromFile(file)
                        } else {
                            Toast.makeText(context, "Soubor zálohy nedostupný", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionButtonBg,
                        contentColor = ActionButtonText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Obnovit nyní", color = ActionButtonText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmRestoreDialog = false }) {
                    Text("Zrušit", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141418),
            modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
        )
    }

    // External URI Restore Confirmation Dialog
    if (showConfirmUriRestoreDialog && selectedImportUri != null) {
        val uri = selectedImportUri!!

        AlertDialog(
            onDismissRequest = { showConfirmUriRestoreDialog = false },
            title = {
                Text("Importovat vybranou zálohu?", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vybrali jste soubor zálohy pro import do Foto Trezoru.", color = TextPrimary, fontSize = 14.sp)
                    Text("Aplikace dešifruje obsah archivu a obnoví všechna alba a fotografie.", color = TextSecondary, fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmUriRestoreDialog = false
                        viewModel.restoreBackupFromUri(uri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ActionButtonBg,
                        contentColor = ActionButtonText
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Spustit import", color = ActionButtonText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmUriRestoreDialog = false }) {
                    Text("Zrušit", color = TextSecondary)
                }
            },
            containerColor = Color(0xFF141418),
            modifier = Modifier.border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(28.dp))
        )
    }

    // Restore Progress Modal
    if (restoreProgress is CloudBackupManager.RestoreProgress.Running) {
        val state = restoreProgress as CloudBackupManager.RestoreProgress.Running
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
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Obnova ze zálohy", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(state.phase, color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    } else if (restoreProgress is CloudBackupManager.RestoreProgress.Success) {
        val res = restoreProgress as CloudBackupManager.RestoreProgress.Success
        LaunchedEffect(restoreProgress) {
            Toast.makeText(context, "Úspěšně obnoveno ${res.restoredAlbums} alb a ${res.restoredPhotos} fotek!", Toast.LENGTH_LONG).show()
            viewModel.resetRestoreProgress()
        }
    }
}

@Composable
fun BackupSnapshotCard(
    snapshot: CloudBackupSnapshotEntity,
    onShare: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(snapshot.timestamp))
    val sizeMb = "%.2f MB".format(snapshot.totalSizeBytes / (1024.0 * 1024.0))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121216))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = snapshot.backupName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x22FFFFFF))
                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "AES-256",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Vytvořeno: $dateStr", color = TextSecondary, fontSize = 12.sp)
                Text("Velikost: $sizeMb", color = TextSecondary, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${snapshot.totalPhotos} fotek • ${snapshot.totalAlbums} alb", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("SHA: ${snapshot.checksumSha256.take(12)}...", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Smazat", tint = RedDanger, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Sdílet / Exportovat na Drive", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedButton(
                    onClick = onRestore,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FFFFFF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Obnovit", fontSize = 13.sp)
                }
            }
        }
    }
}
