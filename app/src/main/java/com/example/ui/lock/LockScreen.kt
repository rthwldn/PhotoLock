package com.example.ui.lock

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.BiometricHelper
import com.example.ui.PinSetupStep
import com.example.ui.VaultViewModel
import com.example.ui.theme.ActionButtonBg
import com.example.ui.theme.ActionButtonText
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryDark
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.KeypadButtonBg
import com.example.ui.theme.RedDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBackground
import com.example.ui.theme.VaultCardBorder
import com.example.ui.theme.VaultSurface
import com.example.ui.theme.VaultSurfaceVariant

@Composable
fun LockScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val step by viewModel.pinSetupStep.collectAsStateWithLifecycle()
    val lockError by viewModel.lockError.collectAsStateWithLifecycle()
    val lockoutSec by viewModel.lockoutSec.collectAsStateWithLifecycle()

    var pinInput by remember { mutableStateOf("") }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    // Auto-prompt biometrics on initial unlock if enabled
    LaunchedEffect(step) {
        pinInput = ""
        if (step == PinSetupStep.UNLOCK && viewModel.biometricEnabled && viewModel.biometricAvailable) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    title = "Odemknout PhotoLock",
                    subtitle = "Ověřte svou identitu otiskem nebo obličejem",
                    negativeButtonText = "Použít PIN",
                    onSuccess = { viewModel.unlockWithBiometricSuccess() },
                    onError = { /* Keep PIN fallback ready */ },
                    onCancel = { /* User cancelled to PIN */ }
                )
            }
        }
    }

    if (step == PinSetupStep.SECURITY_QUESTION) {
        SecurityQuestionSetup(
            onFinish = { question, answer, enableBiometrics ->
                viewModel.finishSecuritySetup(question, answer, enableBiometrics)
            }
        )
        return
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("lock_screen"),
        color = VaultBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (step == PinSetupStep.UNLOCK) Icons.Default.Lock else Icons.Default.Shield,
                        contentDescription = "Vault Lock",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (step) {
                        PinSetupStep.ENTER_NEW_PIN -> "Vytvořte bezpečnostní PIN"
                        PinSetupStep.CONFIRM_PIN -> "Potvrďte nový PIN"
                        PinSetupStep.UNLOCK -> "PhotoLock"
                        else -> "Zabezpečený Trezor"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (step) {
                        PinSetupStep.ENTER_NEW_PIN -> "Zadejte 4-6 místný kód pro ochranu vašich fotek"
                        PinSetupStep.CONFIRM_PIN -> "Zadejte stejný PIN ještě jednou pro ověření"
                        PinSetupStep.UNLOCK -> "Zadejte PIN kód nebo použijte biometrii"
                        else -> ""
                    },
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN Dots Display
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDots = if (pinInput.length > 4) 6 else 4
                    for (i in 0 until maxDots) {
                        val isFilled = i < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) Color.White else Color(0x1AFFFFFF))
                                .border(
                                    1.dp,
                                    if (isFilled) Color.White else Color(0x33FFFFFF),
                                    CircleShape
                                )
                        )
                    }
                }

                // Error / Lockout message
                AnimatedVisibility(visible = lockError != null) {
                    Text(
                        text = lockError ?: "",
                        color = RedDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }

            // Keypad Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            when (key) {
                                "BIO" -> {
                                    if (step == PinSetupStep.UNLOCK && viewModel.biometricAvailable && viewModel.biometricEnabled) {
                                        KeypadIconButton(
                                            icon = Icons.Default.Fingerprint,
                                            onClick = {
                                                val activity = context as? FragmentActivity
                                                if (activity != null) {
                                                    BiometricHelper.showBiometricPrompt(
                                                        activity = activity,
                                                        onSuccess = { viewModel.unlockWithBiometricSuccess() },
                                                        onError = {},
                                                        onCancel = {}
                                                    )
                                                }
                                            }
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(72.dp))
                                    }
                                }
                                "DEL" -> {
                                    KeypadIconButton(
                                        icon = Icons.Default.Backspace,
                                        onClick = {
                                            if (pinInput.isNotEmpty()) {
                                                pinInput = pinInput.dropLast(1)
                                            }
                                        }
                                    )
                                }
                                else -> {
                                    KeypadNumberButton(
                                        number = key,
                                        onClick = {
                                            if (pinInput.length < 6) {
                                                val newInput = pinInput + key
                                                pinInput = newInput
                                                if (newInput.length >= 4) {
                                                    // Allow submit button or auto submit on 4 or 6 digits
                                                    if (newInput.length == 4 && step != PinSetupStep.UNLOCK) {
                                                        // Let user continue typing or submit
                                                    } else if (newInput.length == 6 || (step == PinSetupStep.UNLOCK && newInput.length == 4)) {
                                                        viewModel.submitPin(newInput)
                                                        pinInput = ""
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Submit button for custom length or setup step
                if (pinInput.length in 4..6) {
                    Button(
                        onClick = {
                            viewModel.submitPin(pinInput)
                            pinInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ActionButtonBg,
                            contentColor = ActionButtonText
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(48.dp)
                            .testTag("submit_pin_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (step == PinSetupStep.UNLOCK) "Odemknout" else "Pokračovat",
                            color = ActionButtonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }

                // Forgot PIN option
                if (step == PinSetupStep.UNLOCK && viewModel.securityPrefs.securityQuestion.isNotBlank()) {
                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier.testTag("forgot_pin_button")
                    ) {
                        Text(
                            text = "Zapomněli jste PIN?",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    if (showRecoveryDialog) {
        RecoveryDialog(
            question = viewModel.securityPrefs.securityQuestion,
            onDismiss = { showRecoveryDialog = false },
            onVerify = { answer ->
                val ok = viewModel.securityPrefs.verifyRecoveryAnswer(answer, viewModel.cryptoManager)
                if (ok) {
                    showRecoveryDialog = false
                    viewModel.unlockWithBiometricSuccess()
                }
                ok
            }
        )
    }
}

@Composable
fun KeypadNumberButton(
    number: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0x14FFFFFF))
            .border(1.dp, Color(0x24FFFFFF), CircleShape)
            .clickable(onClick = onClick)
            .testTag("keypad_$number"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color(0x10FFFFFF))
            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
            .clickable(onClick = onClick)
            .testTag("keypad_action"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun SecurityQuestionSetup(
    onFinish: (question: String, answer: String, enableBiometrics: Boolean) -> Unit
) {
    var selectedQuestion by remember { mutableStateOf("Jaké bylo jméno vašeho prvního mazlíčka?") }
    var answerText by remember { mutableStateOf("") }
    var enableBiometrics by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val predefinedQuestions = listOf(
        "Jaké bylo jméno vašeho prvního mazlíčka?",
        "V jakém městě jste se narodili?",
        "Jaký byl váš oblíbený předmět ve škole?",
        "Jaká je vaše oblíbená kniha nebo film?"
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("security_setup_screen"),
        color = VaultBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = EmeraldAccent,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Bezpečnostní obnova",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Zvolte kontrolní otázku pro případ, že zapomenete svůj PIN.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Question selector buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                predefinedQuestions.forEach { q ->
                    val isSelected = q == selectedQuestion
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) VaultSurfaceVariant else VaultSurface)
                            .border(
                                1.dp,
                                if (isSelected) CyanPrimary else VaultCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedQuestion = q }
                            .padding(12.dp)
                    ) {
                        Text(
                            text = q,
                            color = if (isSelected) CyanPrimary else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = answerText,
                onValueChange = {
                    answerText = it
                    error = null
                },
                label = { Text("Vaše odpověď na otázku") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_answer_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = VaultCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { enableBiometrics = !enableBiometrics }
            ) {
                Checkbox(
                    checked = enableBiometrics,
                    onCheckedChange = { enableBiometrics = it },
                    colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Povolit odemykání otiskem prstu / obličejem",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }

            if (error != null) {
                Text(
                    text = error!!,
                    color = RedDanger,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (answerText.trim().length < 2) {
                        error = "Zadejte prosím odpověď na otázku"
                    } else {
                        onFinish(selectedQuestion, answerText.trim(), enableBiometrics)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("finish_security_setup_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionButtonBg,
                    contentColor = ActionButtonText
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Dokončit nastavení a otevřít Trezor", color = ActionButtonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RecoveryDialog(
    question: String,
    onDismiss: () -> Unit,
    onVerify: (answer: String) -> Boolean
) {
    var answer by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Obnova přístupu", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Kontrolní otázka:", color = TextSecondary, fontSize = 13.sp)
                Text(question, color = CyanPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        error = null
                    },
                    placeholder = { Text("Zadejte odpověď...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = VaultCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                if (error != null) {
                    Text(error!!, color = RedDanger, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ok = onVerify(answer)
                    if (!ok) {
                        error = "Nesprávná odpověď na otázku"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ActionButtonBg,
                    contentColor = ActionButtonText
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Ověřit", color = ActionButtonText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zrušit", color = TextSecondary)
            }
        },
        containerColor = VaultSurface
    )
}
