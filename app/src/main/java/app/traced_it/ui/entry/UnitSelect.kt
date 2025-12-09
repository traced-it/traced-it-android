package app.traced_it.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.R
import app.traced_it.data.local.database.*
import app.traced_it.ui.components.TracedTextField
import app.traced_it.ui.theme.AppTheme
import app.traced_it.ui.theme.Spacing

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UnitSelect(
    amountRaw: String,
    selectedUnit: EntryUnit,
    visibleUnit: EntryUnit,
    modifier: Modifier = Modifier,
    onAmountRawChange: (newAmountRaw: String) -> Unit = {},
    onUnitChange: (newUnit: EntryUnit) -> Unit = {},
    onVisibleUnitChange: (newVisibleUnit: EntryUnit) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.windowPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.detail_unit_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Box {
                TextButton(
                    onClick = { expanded = true },
                    modifier = Modifier.testTag("unitSelectButton"),
                    shape = MaterialTheme.shapes.extraSmall,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    contentPadding = PaddingValues(start = Spacing.medium),
                ) {
                    Text(
                        stringResource(visibleUnit.nameResId),
                        fontWeight = FontWeight.Normal,
                    )
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
                    visibleUnits.filterNot { it == visibleUnit }
                        .forEach { unit ->
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(unit.nameResId))
                                },
                                onClick = {
                                    expanded = false
                                    onVisibleUnitChange(unit)
                                },
                                modifier = Modifier.testTag("unitSelectDropdownMenuItem"),
                                contentPadding = PaddingValues(
                                    horizontal = Spacing.medium,
                                    vertical = Spacing.small,
                                ),
                            )
                        }
                }
            }
        }
        if (visibleUnit.choices.isNotEmpty()) {
            UnitSelectChoice(
                amountRaw = amountRaw,
                unit = visibleUnit,
                selectedUnit = selectedUnit,
                modifier = Modifier.fillMaxWidth(),
                onAmountRawChange = { newAmountRaw ->
                    onAmountRawChange(newAmountRaw)
                    onUnitChange(visibleUnit)
                },
                onDeselect = {
                    onAmountRawChange("")
                    onUnitChange(noneUnit)
                },
            )
        } else {
            TracedTextField(
                value = amountRaw,
                onValueChange = { newAmountRaw ->
                    onAmountRawChange(newAmountRaw)
                    if (newAmountRaw.isEmpty()) {
                        onUnitChange(noneUnit)
                    } else {
                        onUnitChange(visibleUnit)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(visibleUnit.placeholder) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnitSelect(
                "",
                noneUnit,
                clothingSizeUnit,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClothingSizePreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnitSelect(
                "S",
                clothingSizeUnit,
                clothingSizeUnit,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SmallNumbersChoicePreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnitSelect(
                "2x",
                smallNumbersChoiceUnit,
                smallNumbersChoiceUnit,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FractionPreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnitSelect(
                "⅓",
                fractionUnit,
                fractionUnit,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DoublePreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnitSelect(
                "",
                doubleUnit,
                doubleUnit,
            )
        }
    }
}

@Preview(showBackground = true, locale = "fr-rFR")
@Composable
private fun DoubleFrenchPreview() {
    AppTheme {
        Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
            UnitSelect(
                "",
                doubleUnit,
                doubleUnit,
            )
        }
    }
}
