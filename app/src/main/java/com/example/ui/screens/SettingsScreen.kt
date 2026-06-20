package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.security.SecurePreferencesManager
import com.example.ui.viewmodel.NoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onNavigateToLock: () -> Unit = {}
) {
    val context = LocalContext.current
    val securePrefs = remember { SecurePreferencesManager(context) }
    var isPinSet by remember { mutableStateOf(securePrefs.isPinSet()) }

    val darkModeEnabled by viewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
    val gridViewEnabled by viewModel.isGridViewEnabled.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.isNotificationsEnabled.collectAsStateWithLifecycle()
    val biometricEnabled by viewModel.isBiometricEnabled.collectAsStateWithLifecycle()

    val globalRemindersEnabled by viewModel.isGlobalRemindersEnabled.collectAsStateWithLifecycle()
    val snoozeMinutes by viewModel.snoozeDurationMinutes.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.isSoundEnabled.collectAsStateWithLifecycle()
    val vibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val historyList by viewModel.reminderHistory.collectAsStateWithLifecycle()

    var showSnoozeDialog by remember { mutableStateOf(false) }

    // Query state on enter/active
    LaunchedEffect(Unit) {
        isPinSet = securePrefs.isPinSet()
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Preferences & Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )

            SettingsItemWithToggle(
                icon = Icons.Outlined.Palette,
                title = "Dark Mode Theme",
                subtitle = "Force application-wide dark color scheme",
                checked = darkModeEnabled,
                onCheckedChange = { viewModel.setDarkModeEnabled(it) },
                testTag = "settings_dark_mode_toggle"
            )

            SettingsItemWithToggle(
                icon = Icons.Default.GridView,
                title = "Grid Layout Preference",
                subtitle = "Arrange note cards in a multi-column staggered grid",
                checked = gridViewEnabled,
                onCheckedChange = { viewModel.setGridViewEnabled(it) },
                testTag = "settings_grid_toggle"
            )

            SettingsItemWithToggle(
                icon = Icons.Outlined.Notifications,
                title = "Notification Reminders",
                subtitle = "Receive device alerts and alarm logs",
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                testTag = "settings_notifications_toggle"
            )

            SettingsItemWithToggle(
                icon = Icons.Outlined.Fingerprint,
                title = "Biometric Lock",
                subtitle = "Require biometric authentications to open directories",
                checked = biometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                testTag = "settings_biometric_toggle"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Text(
                text = "Security Locks & PIN",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            if (isPinSet) {
                SettingsActionItem(
                    icon = Icons.Outlined.Lock,
                    title = "Clear Passcode PIN",
                    subtitle = "Verify and remove established 4-digit master passcode",
                    onClick = {
                        securePrefs.clearPin()
                        isPinSet = false
                        Toast.makeText(context, "Passcode PIN removed successfully.", Toast.LENGTH_SHORT).show()
                    },
                    testTag = "settings_clear_pin"
                )
            } else {
                SettingsActionItem(
                    icon = Icons.Outlined.Lock,
                    title = "Setup Passcode PIN",
                    subtitle = "Configure a secure 4-digit entry combination",
                    onClick = {
                        onNavigateToLock()
                    },
                    testTag = "settings_setup_pin"
                )
            }

            SettingsActionItem(
                icon = Icons.Outlined.Lock,
                title = "Test Screen Lock Overlay",
                subtitle = "Instantly trigger and preview the secure Lock Screen",
                onClick = {
                    viewModel.setLockState(true) // Set locked state dynamically
                    onNavigateToLock()
                },
                testTag = "settings_lock_test"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Text(
                text = "Secure Notification Settings",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SettingsItemWithToggle(
                icon = Icons.Outlined.Notifications,
                title = "Global Reminders State",
                subtitle = "Toggle all note alerts on/off concurrently",
                checked = globalRemindersEnabled,
                onCheckedChange = { viewModel.setGlobalRemindersEnabled(it) },
                testTag = "settings_global_reminders_toggle"
            )

            SettingsItemWithToggle(
                icon = Icons.Outlined.VolumeUp,
                title = "Notification Sound Trigger",
                subtitle = "Enable or disable sound alert on notification arrival",
                checked = soundEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) },
                testTag = "settings_sound_toggle"
            )

            SettingsItemWithToggle(
                icon = Icons.Outlined.Vibration,
                title = "Notification Vibration Wave",
                subtitle = "Enable or disable physical haptic vibration pulse",
                checked = vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) },
                testTag = "settings_vibration_toggle"
            )

            SettingsActionItem(
                icon = Icons.Outlined.Schedule,
                title = "Snooze Interval Period",
                subtitle = "Active snooze cycle: $snoozeMinutes minutes",
                onClick = { showSnoozeDialog = true },
                testTag = "settings_snooze_interval"
            )

            if (showSnoozeDialog) {
                AlertDialog(
                    onDismissRequest = { showSnoozeDialog = false },
                    title = { Text("Set Snooze Duration") },
                    text = {
                        Column {
                            listOf(5, 10, 15, 30).forEach { mins ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setSnoozeDurationMinutes(mins)
                                            showSnoozeDialog = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (snoozeMinutes == mins),
                                        onClick = {
                                            viewModel.setSnoozeDurationMinutes(mins)
                                            showSnoozeDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("$mins minutes", style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSnoozeDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminder History Logs",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (historyList.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearReminderHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("settings_clear_history_btn")
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Clear Logs", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs")
                    }
                }
            }

            if (historyList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No reminder events logged yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    historyList.take(6).forEach { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (statusIcon, statusColor, statusLabel) = when (entry.status) {
                                    "TRIGGERED" -> Triple(Icons.Outlined.NotificationsActive, MaterialTheme.colorScheme.primary, "Notified")
                                    "SNOOZED" -> Triple(Icons.Outlined.Snooze, MaterialTheme.colorScheme.secondary, "Snoozed")
                                    else -> Triple(Icons.Outlined.CheckCircle, MaterialTheme.colorScheme.tertiary, "Dismissed")
                                }

                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = statusLabel,
                                    tint = statusColor,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.noteTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val formattedTime = remember(entry.timestamp) {
                                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(entry.timestamp))
                                    }
                                    Text(
                                        text = formattedTime,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }

                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = statusColor.copy(alpha = 0.12f),
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Text(
                text = "About NoteD",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            SettingsInfoItem(
                icon = Icons.Outlined.Info,
                title = "App Core Version",
                subtitle = "SQLite Room DB via Kotlin Coroutines & Flow (Local First)",
                value = "v1.3.0-SECURE"
            )

            SettingsInfoItem(
                icon = Icons.Default.Person,
                title = "Developer & Lab",
                subtitle = "Designed by NoteD Team & DeepMind AI for local-first productivity",
                value = "AI Session"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsItemWithToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.secondary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = "Navigate", tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun SettingsInfoItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
