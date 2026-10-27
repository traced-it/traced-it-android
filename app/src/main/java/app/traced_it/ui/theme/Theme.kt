package app.traced_it.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

    // Lower emphasis text and text field indicator
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
    onPrimaryContainer = Darkest,
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

@Composable
fun textFieldColors(
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    disabledTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    errorIndicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    placeholderColor: Color = Gray,
    selectionColor: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
) = TextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    disabledTextColor = disabledTextColor,
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = containerColor,
    errorContainerColor = containerColor,
    cursorColor = textColor,
    errorCursorColor = textColor,
    selectionColors = TextSelectionColors(
        handleColor = selectionColor,
        backgroundColor = selectionColor,
    ),
    errorIndicatorColor = errorIndicatorColor,
    focusedIndicatorColor = indicatorColor,
    unfocusedIndicatorColor = indicatorColor,
    disabledIndicatorColor = indicatorColor,
    focusedTrailingIconColor = indicatorColor,
    unfocusedTrailingIconColor = indicatorColor,
    disabledTrailingIconColor = indicatorColor,
    errorTrailingIconColor = indicatorColor,
    focusedPlaceholderColor = placeholderColor,
    unfocusedPlaceholderColor = placeholderColor,
    disabledPlaceholderColor = placeholderColor,
    errorPlaceholderColor = placeholderColor,
)

@Composable
fun topAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
)
