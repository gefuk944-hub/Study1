package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = StudioAccent,
    onPrimary = StudioTextPrimary,
    primaryContainer = StudioAccentContainer,
    onPrimaryContainer = StudioAccent,
    secondary = StudioNeutralActive,
    onSecondary = StudioVoid,
    secondaryContainer = StudioNeutralContainer,
    onSecondaryContainer = StudioTextPrimary,
    tertiary = StudioSuccess,
    onTertiary = StudioVoid,
    tertiaryContainer = StudioSuccessContainer,
    onTertiaryContainer = StudioSuccess,
    error = StudioRecordRed,
    onError = StudioTextPrimary,
    errorContainer = StudioRecordRedContainer,
    onErrorContainer = StudioRecordRed,
    background = StudioVoid,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    surfaceContainerLowest = StudioVoid,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioSurfaceHighlight,
    surfaceContainerHighest = StudioBorder,
    outline = StudioBorder,
    outlineVariant = StudioBorderSubtle
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AiVoiceStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
