package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.Note
import com.example.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    onNavigateToAddEdit: (Int) -> Unit = {},
    onNavigateToArchive: () -> Unit = {},
    onNavigateToLock: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isGridView by viewModel.isGridViewEnabled.collectAsStateWithLifecycle()
    var showCategoryManager by remember { mutableStateOf(false) }

    val pinnedNotes = remember(uiState.activeNotes) { uiState.activeNotes.filter { it.isPinned } }
    val otherNotes = remember(uiState.activeNotes) { uiState.activeNotes.filter { !it.isPinned } }

    val lazyGridState = rememberLazyGridState()
    val showExtendedFab by remember {
        derivedStateOf {
            lazyGridState.firstVisibleItemIndex == 0
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NoteD",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onNavigateToArchive,
                                modifier = Modifier.testTag("archive_nav_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = onNavigateToLock,
                                modifier = Modifier.testTag("lock_nav_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Lock Screen",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                expanded = showExtendedFab,
                onClick = { onNavigateToAddEdit(-1) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Note"
                    )
                },
                text = {
                    Text(
                        text = "New Note",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
                    .testTag("add_note_fab")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Reusable polished SearchBar Composable
            SearchBar(
                onClick = onNavigateToSearch,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Section holding Categories & Layout Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Horizontal scrolling category chips row
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val categories = listOf("All") + uiState.categories.map { it.name }
                    categories.forEach { categoryName ->
                        val isSelected = uiState.selectedCategory == categoryName
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(categoryName) },
                            label = { 
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surface,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("category_chip_${categoryName.lowercase()}")
                        )
                    }

                    // Manage Categories Chip at the end of the scrollable row
                    AssistChip(
                        onClick = { showCategoryManager = true },
                        label = { Text("Manage") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Manage Categories",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.testTag("manage_categories_chip")
                    )
                }

                // Grid / List View dynamic toggle button
                IconButton(
                    onClick = { viewModel.setGridViewEnabled(!isGridView) },
                    modifier = Modifier
                        .testTag("layout_toggle_button")
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle Grid/List view",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Non-blocking database/network error indicator banner
            AnimatedVisibility(
                visible = uiState.error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (uiState.error != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .testTag("error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Database Alert: ${uiState.error}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Dynamic Adaptive Grid/List
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.isLoading) {
                    LazyVerticalGrid(
                        columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(6) { index ->
                            SkeletonNoteCard(
                                isGridView = isGridView,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }
                } else if (uiState.activeNotes.isEmpty()) {
                    EmptyState(
                        selectedCategory = uiState.selectedCategory,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("empty_state_view")
                    )
                } else {
                    LazyVerticalGrid(
                        state = lazyGridState,
                        columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // PINNED NOTES HEADER
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pinned Notes",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // PINNED ITEMS
                            itemsIndexed(pinnedNotes, key = { _, note -> "pinned_${note.id}" }) { index, note ->
                                NoteCard(
                                    note = note,
                                    index = index,
                                    isGridView = isGridView,
                                    onNoteClick = onNavigateToAddEdit,
                                    onTogglePin = { viewModel.togglePin(note) },
                                    onToggleArchive = { viewModel.toggleArchive(note) },
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }

                        // ALL / OTHER NOTES HEADER
                        if (otherNotes.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Text(
                                    text = if (pinnedNotes.isNotEmpty()) "All Notes" else "Notes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            // OTHER ITEMS
                            itemsIndexed(otherNotes, key = { _, note -> "other_${note.id}" }) { index, note ->
                                NoteCard(
                                    note = note,
                                    index = index + pinnedNotes.size,
                                    isGridView = isGridView,
                                    onNoteClick = onNavigateToAddEdit,
                                    onTogglePin = { viewModel.togglePin(note) },
                                    onToggleArchive = { viewModel.toggleArchive(note) },
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = uiState.categories,
            onDismissRequest = { showCategoryManager = false },
            onCreateCategory = { name -> viewModel.createCategory(name) },
            onEditCategory = { oldName, newName -> viewModel.editCategory(oldName, newName) },
            onDeleteCategory = { category -> viewModel.deleteCategory(category) }
        )
    }
}

/**
 * Reusable Notion-style modern search input card
 */
@Composable
fun SearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("search_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Search notes, tags, or contents...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Premium Note Card which handles elegant borders, dynamic color palettes, and adapts
 * beautiful rendering scales based on being configured inside a List or modern Grid layout.
 * Supports staggered load animations, springy press layouts, and modern swipe of notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteCard(
    note: Note,
    index: Int = 0,
    isGridView: Boolean = false,
    onNoteClick: (Int) -> Unit,
    onTogglePin: () -> Unit,
    onToggleArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    var isRendered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isRendered = true
    }

    // Dynamic entering animations
    val enterScale by animateFloatAsState(
        targetValue = if (isRendered) 1.0f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "enter_scale"
    )

    val enterAlpha by animateFloatAsState(
        targetValue = if (isRendered) 1.0f else 0.0f,
        animationSpec = tween(
            durationMillis = 300,
            delayMillis = (index % 8) * 45,
            easing = FastOutSlowInEasing
        ),
        label = "enter_alpha"
    )

    val cardContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNoteClick(note.id) }
                .testTag("note_card_${note.id}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(note.color).copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, Color(note.color).copy(alpha = 0.25f))
        ) {
            // Aesthetic colored accent ribbon at the top of the note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(note.color))
            )
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = if (isGridView) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onToggleArchive,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archive Note",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content.ifBlank { "No content" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    maxLines = if (isGridView) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                    )
                    if (note.category.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = note.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = enterScale
                scaleY = enterScale
                alpha = enterAlpha
            }
    ) {
        if (isGridView) {
            cardContent()
        } else {
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                        onToggleArchive()
                        true
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f)
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        else -> Color.Transparent
                    }
                    val alignment = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                        else -> Alignment.CenterStart
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Swipe Action",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                content = {
                    cardContent()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )

    return this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = androidx.compose.ui.geometry.Offset.Zero,
            end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
        )
    )
}

@Composable
fun SkeletonNoteCard(
    isGridView: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isGridView) 140.dp else 115.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(75.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(45.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

/**
 * Premium custom canvas empty state illustration and tip helper.
 * Designed to feel warm and organic, eliminating generic placeholders.
 */
@Composable
fun EmptyState(
    selectedCategory: String,
    modifier: Modifier = Modifier
) {
    val strokeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val highlightColor = MaterialTheme.colorScheme.primary

    // Animated breathing loop for glowing back circles
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radius"
    )

    Box(
        modifier = modifier.padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant Canvas-drawing representing dynamic creative focus
            Box(
                modifier = Modifier.size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val centerOffset = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    val baseRadius = centerX * 0.8f
                    val pulseRadius = (baseRadius * 1.2f * pulseScale).coerceAtLeast(0.01f)
                    // Glowing background pulsing circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(highlightColor.copy(alpha = 0.08f), Color.Transparent),
                            center = centerOffset,
                            radius = pulseRadius
                        ),
                        radius = pulseRadius,
                        center = centerOffset
                    )
                    // Intersecting minimal geometric paths
                    drawCircle(
                        color = strokeColor,
                        radius = (baseRadius * 0.75f).coerceAtLeast(0.01f),
                        center = centerOffset,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = highlightColor.copy(alpha = 0.15f),
                        radius = (baseRadius * 0.45f).coerceAtLeast(0.01f),
                        center = centerOffset
                    )
                }
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search empty",
                    modifier = Modifier.size(36.dp),
                    tint = highlightColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Empty thoughts in $selectedCategory",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ideas grow when you write them down.\nTap the Plus button to capture a new note securely.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagerDialog(
    categories: List<com.example.data.local.entity.Category>,
    onDismissRequest: () -> Unit,
    onCreateCategory: (String) -> Unit,
    onEditCategory: (String, String) -> Unit,
    onDeleteCategory: (com.example.data.local.entity.Category) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    // Dialog for renaming an existing category
    var editingCategoryName by remember { mutableStateOf<String?>(null) }
    var renamedCategoryName by remember { mutableStateOf("") }
    var renameErrorText by remember { mutableStateOf<String?>(null) }

    // Dialog for deleting a category
    var deletingCategory by remember { mutableStateOf<com.example.data.local.entity.Category?>(null) }

    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.testTag("category_manager_dialog")
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage Categories",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Create Category Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { 
                            newCategoryName = it
                            errorText = null
                        },
                        label = { Text("New Category") },
                        placeholder = { Text("e.g. Work, Ideas") },
                        singleLine = true,
                        isError = errorText != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_category_input")
                    )

                    Button(
                        onClick = {
                            val name = newCategoryName.trim()
                            if (name.isBlank()) {
                                errorText = "Name cannot be empty"
                            } else if (categories.any { it.name.equals(name, ignoreCase = true) }) {
                                errorText = "Category already exists"
                            } else {
                                onCreateCategory(name)
                                newCategoryName = ""
                                errorText = null
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("create_category_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                }

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Existing Categories",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Categories List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    if (categories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No custom categories. Add one above!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingCategoryName = category.name
                                                renamedCategoryName = category.name
                                                renameErrorText = null
                                            },
                                            modifier = Modifier.testTag("edit_category_btn_${category.name.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename Category",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                deletingCategory = category
                                            },
                                            modifier = Modifier.testTag("delete_category_btn_${category.name.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Category",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
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

    // Sub-dialog: Rename Category
    if (editingCategoryName != null) {
        val originalName = editingCategoryName!!
        BasicAlertDialog(
            onDismissRequest = { editingCategoryName = null },
            modifier = Modifier.testTag("rename_category_dialog")
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Rename Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = renamedCategoryName,
                        onValueChange = { 
                            renamedCategoryName = it
                            renameErrorText = null
                        },
                        label = { Text("Category Name") },
                        singleLine = true,
                        isError = renameErrorText != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rename_category_input")
                    )

                    if (renameErrorText != null) {
                        Text(
                            text = renameErrorText ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { editingCategoryName = null },
                            modifier = Modifier.testTag("cancel_rename_btn")
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val newName = renamedCategoryName.trim()
                                if (newName.isBlank()) {
                                    renameErrorText = "Name cannot be empty"
                                } else if (categories.any { it.name.equals(newName, ignoreCase = true) && !it.name.equals(originalName, ignoreCase = true) }) {
                                    renameErrorText = "Category already exists"
                                } else {
                                    onEditCategory(originalName, newName)
                                    editingCategoryName = null
                                }
                            },
                            modifier = Modifier.testTag("save_rename_btn")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog: Confirm Delete
    if (deletingCategory != null) {
        val categoryToDelete = deletingCategory!!
        AlertDialog(
            onDismissRequest = { deletingCategory = null },
            title = { Text("Delete Category") },
            text = { Text("Are you sure you want to delete the category \"${categoryToDelete.name}\"? All notes assigned to this category will be updated to \"Uncategorized\".") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(categoryToDelete)
                        deletingCategory = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_category_btn")
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deletingCategory = null },
                    modifier = Modifier.testTag("cancel_delete_category_btn")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_category_confirm_dialog")
        )
    }
}
