package app.traced_it.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.R
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Gray
import app.traced_it.ui.theme.Spacing

/**
 * Same as TextField but with a `contentPadding` parameter, which is passed to
 * the DecorationBox.
 *
 * Notice that the color definition is simpler than in TextField, because
 * TextField uses internal methods of TextFieldColors, which we cannot use in
 * this module.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextFieldWithCustomPadding(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = TextFieldDefaults.shape,
    contentPadding: PaddingValues = if (label == null) {
        TextFieldDefaults.contentPaddingWithoutLabel()
    } else {
        TextFieldDefaults.contentPaddingWithLabel()
    },
    textColor: Color = LocalTextStyle.current.color,
    containerColor: Color = Color.Unspecified,
    selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
    errorIndicatorColor: Color = Color.Unspecified,
    indicatorColor: Color = Color.Unspecified,
    trailingIconColor: Color = Color.Unspecified,
    placeholderColor: Color = LocalTextStyle.current.color,
) {
    val colors = TextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        errorContainerColor = containerColor,
        cursorColor = textColor,
        errorCursorColor = textColor,
        selectionColors = selectionColors,
        errorIndicatorColor = errorIndicatorColor,
        focusedIndicatorColor = indicatorColor,
        unfocusedIndicatorColor = indicatorColor,
        errorTrailingIconColor = trailingIconColor,
        focusedTrailingIconColor = trailingIconColor,
        unfocusedTrailingIconColor = trailingIconColor,
        errorPlaceholderColor = placeholderColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        errorSuffixColor = placeholderColor,
        focusedSuffixColor = placeholderColor,
        unfocusedSuffixColor = placeholderColor,
    )
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
        BasicTextField(
            value = value,
            modifier = modifier.defaultMinSize(
                minWidth = TextFieldDefaults.MinWidth,
                minHeight = TextFieldDefaults.MinHeight
            ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(textColor),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            decorationBox = @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = value,
                    visualTransformation = visualTransformation,
                    innerTextField = innerTextField,
                    placeholder = placeholder,
                    label = label,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    prefix = prefix,
                    suffix = suffix,
                    supportingText = supportingText,
                    shape = shape,
                    singleLine = singleLine,
                    enabled = enabled,
                    isError = isError,
                    interactionSource = interactionSource,
                    colors = colors,
                    contentPadding = contentPadding,
                    container = {
                        Box(
                            Modifier
                                .background(containerColor, shape)
                                .indicatorLine(
                                    enabled,
                                    isError,
                                    interactionSource,
                                    colors,
                                    focusedIndicatorLineThickness = Spacing.inputBorderWidth,
                                    unfocusedIndicatorLineThickness = Spacing.inputBorderWidth,
                                )
                        )
                    },
                )
            },
        )
    }
}

@Composable
fun TracedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    placeholder: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = Spacing.medium,
        vertical = Spacing.medium
    ),
    showTrailingIcon: Boolean = true,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    selectionColors: TextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary
    ),
    errorIndicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    placeholderColor: Color = Gray,
) {
    TextFieldWithCustomPadding(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        placeholder = placeholder,
        trailingIcon = {
            if (showTrailingIcon && value.isNotEmpty()) {
                IconButton(
                    { onValueChange("") },
                    Modifier.padding(end = Spacing.extraSmall)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.backspace_24px),
                        contentDescription = stringResource(
                            R.string.input_clear_content_description
                        ),
                    )
                }
            }
        },
        suffix = suffix,
        isError = isError,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        contentPadding = contentPadding,
        textColor = textColor,
        containerColor = containerColor,
        selectionColors = selectionColors,
        errorIndicatorColor = errorIndicatorColor,
        indicatorColor = indicatorColor,
        trailingIconColor = indicatorColor,
        placeholderColor = placeholderColor,
    )
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        TracedTextField(
            value = "Apples",
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderPreview() {
    AppTheme {
        TracedTextField(
            value = """
                Jetpack Compose is Android’s recommended modern toolkit for
                building native UI. It simplifies and accelerates UI development
                on Android. Quickly bring your app to life with less code,
                powerful tools, and intuitive Kotlin APIs.
            """.trimIndent(),
            onValueChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() {
    AppTheme {
        TracedTextField(
            value = "",
            placeholder = { Text("Placeholder") },
            onValueChange = {},
        )
    }
}
