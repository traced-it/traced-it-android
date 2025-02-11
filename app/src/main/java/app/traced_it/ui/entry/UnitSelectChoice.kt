package app.traced_it.ui.entry

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.traced_it.data.local.database.EntryUnit
import app.traced_it.data.local.database.clothingSizeUnit
import app.traced_it.ui.components.TracedSegmentedButton
import app.traced_it.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitSelectChoice(
    amountRaw: String,
    unit: EntryUnit,
    selectedUnit: EntryUnit,
    modifier: Modifier = Modifier,
    onAmountRawChange: (newAmountRaw: String) -> Unit = {},
    onDeselect: () -> Unit = {},
) {
    SingleChoiceSegmentedButtonRow(modifier) {
        unit.choices.forEachIndexed { index, choice ->
            val choiceName = stringResource(choice.nameResId)
            TracedSegmentedButton(
                index = index,
                count = unit.choices.size,
                selected = unit == selectedUnit && choiceName == amountRaw,
                onClick = {
                    if (unit == selectedUnit && choiceName == amountRaw) {
                        onDeselect()
                    } else {
                        onAmountRawChange(choiceName)
                    }
                },
            ) {
                Text(
                    stringResource(choice.nameResId),
                    modifier = Modifier.testTag("unitSelectChoiceText"),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    AppTheme {
        UnitSelectChoice("S", clothingSizeUnit, clothingSizeUnit)
    }
}
