package app.traced_it.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val darkScheme = darkColorScheme(
    // Bright purple button
    primary = Dark,
    onPrimary = Lightest,

    // Disabled purple button and dialog
    secondary = Darker,
    onSecondary = Light,

    // Green button
    tertiary = Green,
    onTertiary = Darkest,

    // Error button
    error = Orange,
    onError = Lightest,

    // Default background
    surface = Darkest,
    onSurface = Lightest,

    // Lower emphasis text and text field bottom border
    onSurfaceVariant = Light,

    // App background
    background = Darkest,
    onBackground = Lightest,

    // Odd list item
    surfaceContainerHigh = Darker.copy(alpha = 0.2f),

    // Text input background
    surfaceContainerHighest = Darker,

    // Title text
    onPrimaryContainer = White,

    // Inactive segmented button
    secondaryContainer = Darkest,
    onSecondaryContainer = Lighter,

    // Active segmented button and highlighted list item
    tertiaryContainer = Subtle,
    onTertiaryContainer = Darkest,

    // Text field outline
    outline = Lighter,

    // Divider
    outlineVariant = Darker,

    // Success message
    inverseSurface = Light,
    inverseOnSurface = Darkest
)

val lightScheme = darkScheme.copy(
    background = darkScheme.inverseSurface,
    onBackground = darkScheme.inverseOnSurface,
    onPrimaryContainer = darkScheme.inverseOnSurface,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (!darkTheme) lightScheme else darkScheme,
        typography = AppTypography,
        content = content,
    )
}
