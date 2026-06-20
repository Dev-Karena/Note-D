package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    object Splash : Screen

    @Serializable
    object Home : Screen

    @Serializable
    data class AddEdit(val noteId: Int) : Screen

    @Serializable
    object Search : Screen

    @Serializable
    object Reminders : Screen

    @Serializable
    object Settings : Screen

    @Serializable
    object Lock : Screen

    @Serializable
    object Archive : Screen
}
