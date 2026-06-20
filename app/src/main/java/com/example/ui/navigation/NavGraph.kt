package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.example.ui.screens.*
import com.example.ui.viewmodel.NoteViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Observe deep-link intent trigger for note reminders
    val pendingNoteId by viewModel.pendingNoteIdToOpen.collectAsStateWithLifecycle()
    LaunchedEffect(pendingNoteId) {
        pendingNoteId?.let { noteId ->
            navController.navigate(Screen.AddEdit(noteId = noteId))
            viewModel.setPendingNoteId(null)
        }
    }

    // Determine bottom bar visibility dynamically. Visible ONLY on top-level screens.
    val showBottomBar = currentDestination != null && (
            currentDestination.hasRoute<Screen.Home>() ||
            currentDestination.hasRoute<Screen.Search>() ||
            currentDestination.hasRoute<Screen.Reminders>() ||
            currentDestination.hasRoute<Screen.Settings>()
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        NavigationTab(
                            route = Screen.Home,
                            selectedIcon = Icons.Filled.Home,
                            unselectedIcon = Icons.Outlined.Home,
                            label = "Home"
                        ),
                        NavigationTab(
                            route = Screen.Search,
                            selectedIcon = Icons.Filled.Search,
                            unselectedIcon = Icons.Outlined.Search,
                            label = "Search"
                        ),
                        NavigationTab(
                            route = Screen.Reminders,
                            selectedIcon = Icons.Filled.Notifications,
                            unselectedIcon = Icons.Outlined.Notifications,
                            label = "Alerts"
                        ),
                        NavigationTab(
                            route = Screen.Settings,
                            selectedIcon = Icons.Filled.Settings,
                            unselectedIcon = Icons.Outlined.Settings,
                            label = "Settings"
                        )
                    )

                    tabs.forEach { tab ->
                        val isSelected = currentDestination?.hasRoute(tab.route::class) ?: false
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (!isSelected) {
                                    navController.navigate(tab.route) {
                                        popUpTo(Screen.Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("nav_tab_${tab.label.lowercase()}")
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash, // Smooth starting point
            modifier = modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable<Screen.Splash> {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Splash) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.Home> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAddEdit = { noteId ->
                        navController.navigate(Screen.AddEdit(noteId = noteId))
                    },
                    onNavigateToArchive = {
                        navController.navigate(Screen.Archive)
                    },
                    onNavigateToLock = {
                        navController.navigate(Screen.Lock)
                    },
                    onNavigateToSearch = {
                        navController.navigate(Screen.Search)
                    }
                )
            }

            composable<Screen.AddEdit> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AddEdit>()
                AddEditScreen(
                    noteId = args.noteId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<Screen.Search> {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToNote = { noteId ->
                        navController.navigate(Screen.AddEdit(noteId = noteId))
                    },
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable<Screen.Reminders> {
                ReminderScreen(
                    viewModel = viewModel,
                    onNavigateToNote = { noteId ->
                        navController.navigate(Screen.AddEdit(noteId = noteId))
                    }
                )
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToLock = {
                        navController.navigate(Screen.Lock)
                    }
                )
            }

            composable<Screen.Lock> {
                LockScreen(
                    viewModel = viewModel,
                    onUnlockSuccess = {
                        navController.navigateUp()
                    }
                )
            }

            composable<Screen.Archive> {
                ArchiveScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
        }
    }
}

private data class NavigationTab(
    val route: Screen,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)
