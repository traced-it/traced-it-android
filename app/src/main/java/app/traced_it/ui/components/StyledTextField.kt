package app.traced_it.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import app.traced_it.ui.theme.textFieldColors

/**
 * A copy of [TextField] with custom colors, content padding, and indicator width.
 *
 * It supports only those [TextField] parameters that we use.
 */
@Composable
fun StyledTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
    colors: TextFieldColors = textFieldColors(),
    contentPadding: PaddingValues = PaddingValues(Spacing.medium),
) {
    val interactionSource = remember { MutableInteractionSource() }
    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        val focused = interactionSource.collectIsFocusedAsState().value
        when {
            !enabled -> colors.disabledTextColor
            isError -> colors.errorTextColor
            focused -> colors.focusedTextColor
            else -> colors.unfocusedTextColor
        }
    }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    BasicTextField(
        state = state,
        modifier = modifier.defaultMinSize(
            minWidth = TextFieldDefaults.MinWidth,
            minHeight = TextFieldDefaults.MinHeight,
        ),
        textStyle = mergedTextStyle,
        cursorBrush = SolidColor(if (isError) colors.errorCursorColor else colors.cursorColor),
        keyboardOptions = keyboardOptions,
        lineLimits = lineLimits,
        interactionSource = interactionSource,
        decorator = TextFieldDefaults.decorator(
            state = state,
            enabled = enabled,
            lineLimits = lineLimits,
            outputTransformation = null,
            interactionSource = interactionSource,
            placeholder = placeholder,
            trailingIcon = trailingIcon,
            isError = isError,
            colors = colors,
            contentPadding = contentPadding,
            container = {
                TextFieldDefaults.Container(
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    focusedIndicatorLineThickness = Spacing.inputBorderWidth,
                    unfocusedIndicatorLineThickness = Spacing.inputBorderWidth,
                )
            },
        ),
    )
}

@Composable
fun StyledTextFieldClearButton(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    if (state.text.isNotEmpty()) {
        IconButton(
            {
                state.clearText()
                focusRequester?.requestFocus()
            },
            modifier.padding(end = Spacing.trailingIconPadding),
        ) {
            Icon(
                painter = painterResource(R.drawable.backspace_24px),
                contentDescription = stringResource(R.string.input_clear_content_description),
            )
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        val state = TextFieldState("Apples")
        StyledTextField(
            state = state,
            trailingIcon = { StyledTextFieldClearButton(state) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FilledPreview() {
    AppTheme {
        val state = TextFieldState(
            """
                Jetpack Compose is Android’s recommended modern toolkit for
                building native UI. It simplifies and accelerates UI development
                on Android. Quickly bring your app to life with less code,
                powerful tools, and intuitive Kotlin APIs.
            """.trimIndent()
        )
        StyledTextField(
            state = state,
            trailingIcon = { StyledTextFieldClearButton(state) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    AppTheme {
        val state = TextFieldState()
        StyledTextField(
            state = state,
            placeholder = { Text("Placeholder") },
            trailingIcon = { StyledTextFieldClearButton(state) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledPlaceholderPreview() {
    AppTheme {
        val state = TextFieldState()
        StyledTextField(
            state = state,
            placeholder = { Text("Placeholder") },
            trailingIcon = { StyledTextFieldClearButton(state) },
            enabled = false,
        )
    }
}
