package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.domain.AppearanceMode

@Composable
fun WorkoutTrackerTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (appearanceMode) {
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
    }
    val palette = if (isDark) DarkWorkoutPalette else LightWorkoutPalette

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = palette.brand,
            onPrimary = palette.onBrand,
            primaryContainer = palette.brandSoft,
            onPrimaryContainer = palette.brandInk,
            secondary = palette.teal,
            background = palette.appBg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.subtle,
            onSurfaceVariant = palette.muted,
            outline = palette.borderStrong,
            outlineVariant = palette.border,
            error = palette.redInk,
            errorContainer = palette.redSoft
        )
    } else {
        lightColorScheme(
            primary = palette.brand,
            onPrimary = palette.onBrand,
            primaryContainer = palette.brandSoft,
            onPrimaryContainer = palette.brandInk,
            secondary = palette.teal,
            background = palette.appBg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.subtle,
            onSurfaceVariant = palette.muted,
            outline = palette.borderStrong,
            outlineVariant = palette.border,
            error = palette.redInk,
            errorContainer = palette.redSoft
        )
    }

    CompositionLocalProvider(LocalWorkoutPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WorkoutTypography,
            content = content
        )
    }
}
