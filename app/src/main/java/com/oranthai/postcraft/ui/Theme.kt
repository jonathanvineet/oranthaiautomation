package com.oranthai.postcraft.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val InstaGradient = Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFFE1306C), Color(0xFFFCAF45)))

private val Colors = darkColorScheme(
    primary = Color(0xFFF06292),
    onPrimary = Color(0xFF1A0A12),
    secondary = Color(0xFFB39DDB),
    background = Color(0xFF0F0D13),
    surface = Color(0xFF0F0D13),
    surfaceVariant = Color(0xFF221E2A),
    surfaceContainer = Color(0xFF1A1720),
    surfaceContainerHigh = Color(0xFF241F2B),
    secondaryContainer = Color(0xFF4A2340),
    onSecondaryContainer = Color(0xFFFFD8EC),
)

@Composable
fun PostCraftTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = Colors, content = content)
