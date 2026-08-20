package com.example.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant

@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageBytes by viewModel.storageUsageBytes.collectAsStateWithLifecycle()
    val allPhotos by viewModel.allActivePhotos.collectAsStateWithLifecycle()

    var biometricEnabled by remember { mutableStateOf(viewModel.securityPrefs.biometricEnabled) }
    var selectedAutoLock by remember { mutableIntStateOf(viewModel.securityPrefs.autoLockSeconds) }
    var deleteAfterImport by remember { mutableStateOf(viewModel.securityPrefs.deleteAfterImport) }

    val autoLockOptions = listOf(
        Pair(0, "Ihned"),
        Pair(30, "30 s"),
        Pair(60, "1 min"),
        Pair(300, "5 min"),
        Pair(-1, "Nikdy")
    )

    val storageMb = "%.2f MB".format(storageBytes / (1024.0 * 1024.0))

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        color = VaultBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Nastavení a bezpečnost",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Security Options Section
            item {
                Text("Autentizace", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121216))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Biometric Toggle
                        if (viewModel.biometricAvailable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("Biometrické odemykání", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                        Text("Otisk prstu nebo obličej", color = TextSecondary, fontSize = 12.sp)
                                    }
                                }
                                Switch(
                                    checked = biometricEnabled,
                                    onCheckedChange = {
                                        biometricEnabled = it
                                        viewModel.setBiometricEnabled(it)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color.White,
                                        uncheckedThumbColor = Color(0xFFAAAAAA),
                                        uncheckedTrackColor = Color(0xFF2A2A30)
                                    )
                                )
                            }
                        }

                        // Auto-delete from Gallery after import toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = RedDanger, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Smazat z galerie po importu", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                    Text("Odstraní originály z telefonu po zašifrování", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                            Switch(
                                checked = deleteAfterImport,
                                onCheckedChange = {
                                    deleteAfterImport = it
                                    viewModel.securityPrefs.deleteAfterImport = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = Color.White,
                                    uncheckedThumbColor = Color(0xFFAAAAAA),
                                    uncheckedTrackColor = Color(0xFF2A2A30)
                                )
                            )
                        }

                        // Auto-lock picker
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Automatické uzamčení", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                autoLockOptions.forEach { (sec, label) ->
                                    val isSelected = selectedAutoLock == sec
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White else Color(0x1AFFFFFF))
                                            .border(1.dp, if (isSelected) Color.White else Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedAutoLock = sec
                                                viewModel.setAutoLockSeconds(sec)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when (sec) {
                                                0 -> "Ihned"
                                                30 -> "30 s"
                                                60 -> "1 min"
                                                300 -> "5 min"
                                                -1 -> "Nikdy"
                                                else -> "${sec}s"
                                            },
                                            color = if (isSelected) Color.Black else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Storage Breakdown Card
            item {
                Text("Úložiště a šifrování", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x2EFFFFFF), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121216))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Aktivních fotografií:", color = TextSecondary, fontSize = 13.sp)
                            Text("${allPhotos.size} fotek", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Zašifrované úložiště:", color = TextSecondary, fontSize = 13.sp)
                            Text(storageMb, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Šifrovací algoritmus:", color = TextSecondary, fontSize = 13.sp)
                            Text("AES-256-GCM", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Správce klíčů:", color = TextSecondary, fontSize = 13.sp)
                            Text("AndroidKeyStore (HW)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Lock Vault Now button
            item {
                Button(
                    onClick = onLockNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("lock_vault_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedDanger,
                        contentColor = Color(0xFF601410)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF601410), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uzamknout Foto Trezor nyní", color = Color(0xFF601410), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
