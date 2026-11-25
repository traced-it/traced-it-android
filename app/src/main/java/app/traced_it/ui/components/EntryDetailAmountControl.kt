package app.traced_it.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.traced_it.R
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.DoubleUnit
import app.traced_it.data.database.EntryUnit
import app.traced_it.data.database.FractionUnit
import app.traced_it.data.database.NoneUnit
import app.traced_it.data.database.SmallNumbersChoiceUnit
import app.traced_it.data.database.toActiveUnit
import app.traced_it.data.database.units
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlin.math.PI

@Composable
fun EntryDetailAmountControl(
    defaultUnit: StateFlow<ActiveEntryUnit>,
    initialAmount: Double,
    initialAmountUnit: EntryUnit,
    onSetAmountAndUnit: (amount: Double, amountUnit: EntryUnit) -> Unit,
    onSetDefaultUnit: (defaultUnit: ActiveEntryUnit) -> Unit,
    enabled: Boolean = true,
) {
    val resources = LocalResources.current

    // Default unit

    val defaultUnit by defaultUnit.collectAsStateWithLifecycle()
    val activeUnits = remember(defaultUnit) { units.map { it.toActiveUnit(defaultUnit) }.toSet() }

    // Amount

    var amountFocusRequested by remember { mutableStateOf(false) }
    val amountFocusRequester = remember { FocusRequester() }
    val amountRawFieldState = retain(initialAmount, initialAmountUnit) {
        val text = initialAmountUnit.format(resources, initialAmount)
        TextFieldState(
            initialText = text,
            initialSelection = TextRange(text.length), // Put cursor at the end of the text
        )
    }

    // Selected unit

    /**
     * Unit that is selected in the dropdown menu. It can differ from the unit that is currently saved in the database.
     *
     * For example when deselecting a choice, we save [NoneUnit] in the database but keep [selectedUnit] unchanged.
     */
    var selectedUnit by retain(initialAmountUnit) {
        mutableStateOf(initialAmountUnit.toActiveUnit(defaultUnit))
    }

    /**
     * Selected choice of the selected unit.
     *
     * A choice can be selected only when its unit is the one currently saved in database, so that for example XS is not
     * selected when the unit in the database is none and the amount is 0.0.
     */
    var selectedChoiceValue by retain(initialAmountUnit, initialAmount) {
        mutableStateOf(
            selectedUnit
                .takeIf { selectedUnit -> selectedUnit == initialAmountUnit }
                ?.choices
                ?.firstOrNull { choice -> choice.value == initialAmount }
                ?.value
        )
    }

    // Set amount and unit when the text field value changes
    LaunchedEffect(amountRawFieldState) {
        snapshotFlow { amountRawFieldState.text.toString() }
            .drop(1) // Skip initial value
            .collectLatest { amountRaw ->
                if (amountRaw.isBlank()) {
                    onSetAmountAndUnit(NoneUnit.default, NoneUnit)
                } else {
                    onSetAmountAndUnit(selectedUnit.parse(resources, amountRaw), selectedUnit)
                }
            }
    }

    EntryDetailControl(
        label = stringResource(R.string.detail_unit_label),
        labelMenu = {
            var expanded by rememberSaveable { mutableStateOf(false) }

            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.testTag(TestTags.ENTRY_DETAIL_UNIT_MENU),
                    enabled = enabled,
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    contentPadding = PaddingValues(start = Spacing.medium),
                ) {
                    Text(selectedUnit.getName(resources), fontWeight = FontWeight.Normal)
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.detail_unit_dropdown_content_description),
                        modifier = Modifier.padding(Spacing.small),
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.semantics { testTagsAsResourceId = true },
                ) {
                    activeUnits.forEach { activeUnit ->
                        DropdownMenuItem(
                            text = { Text(activeUnit.getName(resources)) },
                            onClick = {
                                amountFocusRequested = true
                                expanded = false
                                selectedUnit = activeUnit
                                selectedChoiceValue = null

                                // Clear the amount field, so that for example "XL" doesn't appear as the amount after
                                // switching from the clothing size unit to the double unit.
                                amountRawFieldState.clearText()

                                // Set amount and unit to none, so that for example "S" is not selected after switching
                                // from the double unit with amount 1.0 to the clothing size unit. We reset amount and
                                // unit explicitly instead of relying on an observation of the amount field, because an
                                // observation of the amount field doesn't get triggered if the amount was already
                                // empty.
                                onSetAmountAndUnit(NoneUnit.default, NoneUnit)

                                onSetDefaultUnit(activeUnit)
                            },
                            modifier = Modifier.testTag(TestTags.entryDetailUnitMenuItem(activeUnit)),
                            enabled = enabled,
                            contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
                        )
                    }
                }
            }
        },
    ) {
        selectedUnit.choices?.takeIf { it.isNotEmpty() }?.let { choices ->
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                choices.forEachIndexed { index, choice ->
                    StyledSegmentedButton(
                        index = index,
                        count = choices.size,
                        selected = choice.value == selectedChoiceValue,
                        onClick = {
                            if (choice.value != selectedChoiceValue) {
                                // Select
                                selectedChoiceValue = choice.value
                                onSetAmountAndUnit(choice.value, selectedUnit)
                            } else {
                                // Deselect
                                selectedChoiceValue = null
                                onSetAmountAndUnit(NoneUnit.default, NoneUnit)
                            }
                        },
                        modifier = Modifier.testTag(TestTags.entryDetailUnitChoice(selectedUnit)),
                        enabled = enabled,
                    ) {
                        choice.Label()
                    }
                }
            }
        } ?: run {
            LaunchedEffect(amountFocusRequested) {
                if (amountFocusRequested) {
                    amountFocusRequester.requestFocus()
                    amountFocusRequested = false
                }
            }

            StyledTextField(
                state = amountRawFieldState,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
                    .testTag(TestTags.entryDetailAmount(selectedUnit)),
                enabled = enabled,
                placeholder = { Text(selectedUnit.placeholder) },
                trailingIcon = {
                    StyledTextFieldClearButton(
                        amountRawFieldState,
                        focusRequester = amountFocusRequester,
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = 0.0,
                initialAmountUnit = NoneUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledClothingSizePreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = 0.0,
                initialAmountUnit = NoneUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
                enabled = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SmallNumbersChoicePreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = 2.0,
                initialAmountUnit = SmallNumbersChoiceUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FractionPreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = 0.333,
                initialAmountUnit = FractionUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoublePreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = PI,
                initialAmountUnit = DoubleUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DisabledDoublePreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = PI,
                initialAmountUnit = DoubleUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
                enabled = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyDoublePreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = 0.0,
                initialAmountUnit = DoubleUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
            )
        }
    }
}

@Preview(showBackground = true, locale = "fr-rFR")
@Composable
private fun FrenchDoublePreview() {
    AppTheme {
        Surface {
            EntryDetailAmountControl(
                defaultUnit = MutableStateFlow(ClothingSizeUnit),
                initialAmount = 0.0,
                initialAmountUnit = DoubleUnit,
                onSetAmountAndUnit = { _, _ -> },
                onSetDefaultUnit = {},
            )
        }
    }
}
