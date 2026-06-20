package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.security.SecurePreferencesManager
import com.example.ui.viewmodel.NoteViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    viewModel: NoteViewModel,
    onUnlockSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val securePrefs = remember { SecurePreferencesManager(context) }
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    // Screen security state machine
    var pinSetupStep by remember { mutableStateOf(0) } // 0 = not setup/ordinary lock, 1 = setup choose, 2 = setup confirm
    var firstEnteredPin by remember { mutableStateOf("") }
    val isPinAlreadySet = remember { securePrefs.isPinSet() }

    var currentInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isErrorActive by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Determine initial flow state
    LaunchedEffect(isPinAlreadySet) {
        if (!isPinAlreadySet) {
            pinSetupStep = 1 // Start setup choose mode
        } else {
            pinSetupStep = 0 // Unlock mode
            // Automatically trigger biometric prompt if configured & enabled
            if (isBiometricEnabled && BiometricHelper.isBiometricAvailable(context)) {
                val activity = findFragmentActivity(context)
                if (activity != null) {
                    var attempts = 0
                    // Wait up to 3 seconds for activity to reach RESUMED lifecycle state
                    while (activity.lifecycle.currentState < androidx.lifecycle.Lifecycle.State.RESUMED && attempts < 30) {
                        delay(100)
                        attempts++
                    }
                    if (activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                        BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            onSuccess = {
                                onUnlockSuccess()
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    }
                }
            }
        }
    }

    // Dynamic scale and color animations for error feedback
    val errorShakeOffset by animateDpAsState(
        targetValue = if (isErrorActive) 14.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "shake_animation",
        finishedListener = {
            if (isErrorActive) {
                isErrorActive = false
            }
        }
    )

    fun handlePinComplete(pin: String) {
        if (pinSetupStep == 1) {
            // Choice phase complete
            firstEnteredPin = pin
            currentInput = ""
            pinSetupStep = 2 // Move to verify confirm stage
        } else if (pinSetupStep == 2) {
            // Confirm phase complete
            if (pin == firstEnteredPin) {
                securePrefs.setPin(pin)
                Toast.makeText(context, "Secure Master PIN configured successfully!", Toast.LENGTH_SHORT).show()
                onUnlockSuccess()
            } else {
                errorMessage = "PINs do not match. Please restart setup."
                isErrorActive = true
                currentInput = ""
                pinSetupStep = 1
                firstEnteredPin = ""
            }
        } else {
            // Unlock mode
            val savedPin = securePrefs.getPin()
            if (pin == savedPin) {
                onUnlockSuccess()
            } else {
                errorMessage = "Invalid security passcode. Try again."
                isErrorActive = true
                currentInput = ""
            }
        }
    }

    fun onKeyPress(digit: String) {
        if (currentInput.length < 4) {
            currentInput += digit
            if (currentInput.length == 4) {
                coroutineScope.launch {
                    delay(150)
                    handlePinComplete(currentInput)
                }
            }
        }
    }

    fun onBackspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
        }
    }

    fun triggerBiometrics() {
        val activity = findFragmentActivity(context)
        if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
            BiometricHelper.showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    onUnlockSuccess()
                },
                onError = { err ->
                    errorMessage = err
                }
            )
        } else {
            errorMessage = "Biometrics sensor not enrolled/not available on this device."
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .offset(x = errorShakeOffset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper Header Banner Group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (errorMessage != null) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Secured Vault",
                        modifier = Modifier.size(36.dp),
                        tint = if (errorMessage != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = when (pinSetupStep) {
                        1 -> "Choose Security PIN"
                        2 -> "Confirm Security PIN"
                        else -> "NoteD Vault Locked"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (pinSetupStep) {
                        1 -> "Enter a four-digit numeric passcode passage."
                        2 -> "Re-type your numeric sequence to verify."
                        else -> "Your notes are sealed by high-grade local encryption."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Central Code Completion Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < currentInput.length
                    val dotColor = if (isFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }
            }

            // Lower Custom Modern Numeric Keypad (4x3 array grid)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val rowKeyData = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("biometrics", "0", "backspace")
                )

                rowKeyData.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowKeys.forEach { key ->
                            when (key) {
                                "biometrics" -> {
                                    if (isBiometricEnabled && isPinAlreadySet) {
                                        IconButton(
                                            onClick = { triggerBiometrics() },
                                            modifier = Modifier
                                                .size(72.dp)
                                                .testTag("key_biometrics")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Unlock with Fingerprint",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(72.dp))
                                    }
                                }
                                "backspace" -> {
                                    IconButton(
                                        onClick = { onBackspace() },
                                        enabled = currentInput.isNotEmpty(),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .testTag("key_backspace")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace",
                                            tint = if (currentInput.isNotEmpty()) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        onClick = { onKeyPress(key) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .testTag("key_$key")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 26.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
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
}

private fun findFragmentActivity(context: android.content.Context): FragmentActivity? {
    var cur = context
    var depth = 0
    while (cur is android.content.ContextWrapper && depth < 20) {
        if (cur is FragmentActivity) {
            return cur
        }
        val next = cur.baseContext
        if (next == cur) break
        cur = next
        depth++
    }
    return null
}
