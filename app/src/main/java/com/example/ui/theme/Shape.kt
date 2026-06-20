package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    // Extra-fine components like small badge chips
    extraSmall = RoundedCornerShape(4.dp),
    // Small elements like custom tags, filter chips or context details
    small = RoundedCornerShape(8.dp),
    // Medium containers like search bar or popups
    medium = RoundedCornerShape(12.dp),
    // Large prominent surfaces like main note cards
    large = RoundedCornerShape(16.dp),
    // Extra-large fullscreen-like panels (e.g., editorial editor sheet)
    extraLarge = RoundedCornerShape(24.dp)
)
