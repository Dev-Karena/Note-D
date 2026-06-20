package com.example.ui.screens

import android.app.DatePickerDialog
import com.example.data.local.entity.RepeatType
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    noteId: Int,
    viewModel: NoteViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Base colors matching user system theme
    val defaultNoteColor = if (isSystemInDarkTheme()) NoteDarkLavender else NoteLavender

    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(defaultNoteColor) }
    var categoryText by remember { mutableStateOf("Personal") }
    var isPinned by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var repeatType by remember { mutableStateOf(RepeatType.NONE) }
    var isReminderEnabled by remember { mutableStateOf(false) }
    var isReminderConfigExpanded by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Load original Note if in Edit mode
    LaunchedEffect(noteId) {
        viewModel.loadNote(noteId)
    }

    // Load initial states when note metadata is parsed successfully
    LaunchedEffect(uiState.currentNote) {
        val current = uiState.currentNote
        if (current != null && noteId != -1) {
            titleText = current.title
            contentText = current.content
            selectedColor = Color(current.color)
            categoryText = current.category
            isPinned = current.isPinned
            reminderTime = current.reminderTime
            repeatType = current.repeatType
            isReminderEnabled = current.isReminderEnabled
            if (current.isReminderEnabled && current.reminderTime != null) {
                isReminderConfigExpanded = true
            }
        }
    }

    // Fluid spring animated background color to immerse the user in the note's custom mood color
    val animatedBackgroundColor by animateColorAsState(
        targetValue = selectedColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "animatedBackgroundColor"
    )

    // Deletion Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Note?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this note? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val current = uiState.currentNote
                        if (current != null) {
                            viewModel.deleteNote(
                                context = context,
                                note = current,
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                },
                                onSuccess = {
                                    showDeleteConfirm = false
                                    onNavigateBack()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId == -1) "New Note" else "Edit Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Show delete action button if note is already stored in Room
                    if (noteId != -1) {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("delete_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Note",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Done / Save check icon
                    IconButton(
                        onClick = {
                            viewModel.saveNote(
                                context = context,
                                id = noteId,
                                title = titleText,
                                content = contentText,
                                category = categoryText,
                                color = selectedColor.toArgb(),
                                reminderTime = reminderTime,
                                isPinned = isPinned,
                                repeatType = repeatType,
                                isReminderEnabled = isReminderEnabled,
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                },
                                onSuccess = onNavigateBack
                            )
                        },
                        modifier = Modifier.testTag("save_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = animatedBackgroundColor.copy(alpha = 0.15f)
                )
            )
        },
        containerColor = animatedBackgroundColor.copy(alpha = 0.10f)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // Automatically resize and push selectors when keyboard loads
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Elegant, minimalist note title input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    placeholder = {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                        )
                    },
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                // Extremely subtle visual horizontal divider
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Flexible content editor
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    placeholder = {
                        Text(
                            text = "Start writing your raw ideas...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        errorBorderColor = Color.Transparent
                    )
                )

                // Spacious dynamic keyboard and bottom spacing at the very end of scrollable area,
                // so the user can easily scroll their typed text completely above the floating toolbar.
                Spacer(modifier = Modifier.height(260.dp))
            }

            // High-fidelity, modern elevated floating toolbar per premium productivity apps (Notion, Obsidian)
            FloatingToolbar(
                categoryText = categoryText,
                onCategoryChanged = { categoryText = it },
                categories = uiState.categories.map { it.name }.ifEmpty { listOf("Personal", "Work", "Study", "Ideas") },
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it },
                isPinned = isPinned,
                onPinnedChanged = { isPinned = it },
                isReminderEnabled = isReminderEnabled,
                onReminderEnabledChanged = { enabled ->
                    isReminderEnabled = enabled
                    if (!enabled) {
                        reminderTime = null
                        repeatType = RepeatType.NONE
                    }
                },
                reminderTime = reminderTime,
                onReminderTimeChanged = { reminderTime = it },
                repeatType = repeatType,
                onRepeatTypeChanged = { repeatType = it },
                isReminderConfigExpanded = isReminderConfigExpanded,
                onExpandedChanged = { isReminderConfigExpanded = it },
                permissionLauncher = permissionLauncher,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}

/**
 * Modern elevated floating toolbar composable for rapid access to note attributes.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FloatingToolbar(
    categoryText: String,
    onCategoryChanged: (String) -> Unit,
    categories: List<String>,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    isPinned: Boolean,
    onPinnedChanged: (Boolean) -> Unit,
    isReminderEnabled: Boolean,
    onReminderEnabledChanged: (Boolean) -> Unit,
    reminderTime: Long?,
    onReminderTimeChanged: (Long?) -> Unit,
    repeatType: RepeatType,
    onRepeatTypeChanged: (RepeatType) -> Unit,
    isReminderConfigExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .testTag("floating_toolbar"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Category Selector
            CategorySelectorChipRow(
                categories = categories,
                selectedCategory = categoryText,
                onSelectedChange = onCategoryChanged
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color Circle Palette
                NoteColorPalette(
                    selectedColor = selectedColor,
                    onColorSelected = onColorSelected
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pin Button
                    IconButton(
                        onClick = { onPinnedChanged(!isPinned) },
                        modifier = Modifier
                            .background(
                                color = if (isPinned) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("pin_note_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note Toggle",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    // Reminder Trigger
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.POST_NOTIFICATIONS
                                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                if (!hasPermission) {
                                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    return@IconButton
                                }
                            }

                            onExpandedChanged(!isReminderConfigExpanded)
                            if (!isReminderConfigExpanded && !isReminderEnabled) {
                                onReminderEnabledChanged(true)
                                if (reminderTime == null) {
                                    onReminderTimeChanged(System.currentTimeMillis() + 3600_000)
                                }
                            }
                        },
                        modifier = Modifier
                            .background(
                                color = if (isReminderEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("reminder_time_picker")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Reminder Picker",
                            tint = if (isReminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isReminderConfigExpanded || isReminderEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    ReminderConfigPanel(
                        isEnabled = isReminderEnabled,
                        onEnabledChanged = onReminderEnabledChanged,
                        reminderTime = reminderTime,
                        onReminderTimeChanged = onReminderTimeChanged,
                        repeatType = repeatType,
                        onRepeatTypeChanged = onRepeatTypeChanged,
                        isExpanded = isReminderConfigExpanded,
                        onExpandedChanged = onExpandedChanged
                    )
                }
            }
        }
    }
}

/**
 * Reusable Category selecting Row component
 */
@Composable
fun CategorySelectorChipRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectedChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Category:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        categories.forEach { cat ->
            val isSelected = selectedCategory == cat
            FilterChip(
                selected = isSelected,
                onClick = { onSelectedChange(cat) },
                label = { Text(cat, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("edit_category_chip_${cat.lowercase()}")
            )
        }
    }
}

/**
 * Modern circular palette picker circles
 */
@Composable
fun NoteColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val colors = remember(isDark) {
        if (isDark) {
            listOf(NoteDarkLavender, NoteDarkIceBlue, NoteDarkSageGreen, NoteDarkSoftCream, NoteDarkCoralPink)
        } else {
            listOf(NoteLavender, NoteIceBlue, NoteSageGreen, NoteSoftCream, NoteCoralPink)
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEach { noteColor ->
            val isCurrent = selectedColor == noteColor
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(noteColor)
                    .border(
                        width = if (isCurrent) 2.5.dp else 1.dp,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(noteColor) }
                    .padding(2.dp)
            ) {
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    )
                }
            }
        }
    }
}

private fun getDateTimeFormatted(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderConfigPanel(
    isEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    reminderTime: Long?,
    onReminderTimeChanged: (Long?) -> Unit,
    repeatType: RepeatType,
    onRepeatTypeChanged: (RepeatType) -> Unit,
    isExpanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("reminder_config_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row with Notification icon, Enable Switch, and Expand/Collapse arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notification Bell",
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = "Note Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isExpanded && isEnabled && reminderTime != null) {
                            Text(
                                text = "Enabled · Click to configure",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (!isEnabled) {
                            Text(
                                text = "Disabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Switch
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                                    if (!hasPermission) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                onEnabledChanged(true)
                                if (reminderTime == null) {
                                    onReminderTimeChanged(System.currentTimeMillis() + 3600_000)
                                }
                                onExpandedChanged(true)
                            } else {
                                onEnabledChanged(false)
                                onReminderTimeChanged(null)
                                onExpandedChanged(false)
                            }
                        },
                        modifier = Modifier.testTag("reminder_enable_switch")
                    )

                    // Expand/Collapse text button
                    TextButton(
                        onClick = { onExpandedChanged(!isExpanded) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isExpanded) "Collapse" else "Configure")
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded && isEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(16.dp))

                    val activeTime = reminderTime ?: System.currentTimeMillis()
                    calendar.timeInMillis = activeTime

                    // Date & Time pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Button
                        OutlinedButton(
                            onClick = {
                                val datePicker = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        calendar.set(Calendar.YEAR, year)
                                        calendar.set(Calendar.MONTH, month)
                                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        onReminderTimeChanged(calendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                datePicker.show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reminder_date_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(activeTime)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Time Button
                        OutlinedButton(
                            onClick = {
                                val timePicker = TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        calendar.set(Calendar.MINUTE, minute)
                                        calendar.set(Calendar.SECOND, 0)
                                        calendar.set(Calendar.MILLISECOND, 0)
                                        onReminderTimeChanged(calendar.timeInMillis)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    false
                                )
                                timePicker.show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reminder_time_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(activeTime)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Repeat interval selector
                    Text(
                        text = "Repeat Interval",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    var dropdownExpanded by remember { mutableStateOf(false) }
                    val repeatOptions = listOf(
                        RepeatType.NONE to "One time",
                        RepeatType.DAILY to "Daily",
                        RepeatType.WEEKLY to "Weekly",
                        RepeatType.MONTHLY to "Monthly"
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { dropdownExpanded = !dropdownExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("repeat_dropdown_trigger"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val currentLabel = repeatOptions.find { it.first == repeatType }?.second ?: "One time"
                                Text(
                                    text = currentLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "▼",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            repeatOptions.forEach { (option, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        onRepeatTypeChanged(option)
                                        dropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("repeat_option_${option.name.lowercase()}"),
                                    colors = MenuDefaults.itemColors(
                                        textColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Always visible dynamic Reminder Summary Preview when details are enabled
            if (isEnabled && reminderTime != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_summary_preview")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔔",
                            fontSize = 16.sp
                        )
                        Column {
                            Text(
                                text = "Preview Summary",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = getReminderSummaryText(reminderTime, repeatType, isEnabled),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

fun getReminderSummaryText(timestamp: Long?, repeatType: RepeatType, isEnabled: Boolean): String {
    if (!isEnabled || timestamp == null) {
        return "No reminder scheduled"
    }
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(timestamp))
    val timeStr = timeFormat.format(Date(timestamp))
    
    return when (repeatType) {
        RepeatType.NONE -> "One-time alert on $dateStr at $timeStr"
        RepeatType.DAILY -> "Repeats daily at $timeStr"
        RepeatType.WEEKLY -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
            "Repeats weekly on ${dayOfWeek}s at $timeStr"
        }
        RepeatType.MONTHLY -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            "Repeats monthly on day $dayOfMonth at $timeStr"
        }
    }
}
