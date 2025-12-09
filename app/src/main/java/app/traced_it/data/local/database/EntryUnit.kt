package app.traced_it.data.local.database

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import app.traced_it.R
import java.text.NumberFormat
import java.text.ParseException

data class EntryUnitChoice(
    val value: Double,
    @param:StringRes val nameResId: Int,
    @param:StringRes val htmlResId: Int? = null,
) {
    fun format(resources: Resources): String = resources.getString(nameResId)

    fun formatHtml(resources: Resources): AnnotatedString? =
        htmlResId?.let { AnnotatedString.fromHtml(resources.getString(it)) }
}

data class EntryUnit(
    val id: String,
    @param:StringRes val nameResId: Int,
    val defaultValue: Double = 0.0,
    val choices: List<EntryUnitChoice> = listOf(),
) {
    private val numberFormat = NumberFormat.getNumberInstance()
    private val placeholderNumberFormat = NumberFormat.getNumberInstance()
        .apply { minimumFractionDigits = 1 }

    val placeholder: String get() = placeholderNumberFormat.format(defaultValue)

    fun format(resources: Resources, value: Double): String =
        choices.find { it.value == value }?.format(resources)
            ?: numberFormat.format(value)

    fun formatHtml(resources: Resources, value: Double): AnnotatedString? =
        choices.find { it.value == value }?.formatHtml(resources)

    fun parse(resources: Resources, value: String): Double =
        choices.find { resources.getString(it.nameResId) == value }
            ?.value
            ?: try {
                numberFormat.parse(value)?.toDouble()
            } catch (_: ParseException) {
                null
            } ?: defaultValue

    fun serialize(value: Double): String = value.toString()

    fun deserialize(value: String): Double = value.toDoubleOrNull() ?: defaultValue
}

fun List<EntryUnit>.associateById(): Map<String, EntryUnit> =
    this.associateBy { it.id }

fun List<EntryUnit>.associateByName(resources: Resources): Map<String, EntryUnit> =
    this.associateBy { resources.getString(it.nameResId) }

val noneUnit = EntryUnit(
    id = "NONE",
    nameResId = R.string.entry_unit_none_name,
    choices = listOf(
        EntryUnitChoice(0.0, R.string.entry_unit_none_choice_empty),
    ),
)
val clothingSizeUnit = EntryUnit(
    id = "CLOTHING_SIZE",
    nameResId = R.string.entry_unit_clothing_name,
    choices = listOf(
        EntryUnitChoice(0.0, R.string.entry_unit_clothing_choice_xs),
        EntryUnitChoice(1.0, R.string.entry_unit_clothing_choice_s),
        EntryUnitChoice(2.0, R.string.entry_unit_clothing_choice_m),
        EntryUnitChoice(3.0, R.string.entry_unit_clothing_choice_l),
        EntryUnitChoice(4.0, R.string.entry_unit_clothing_choice_xl),
    ),
)
val fractionUnit = EntryUnit(
    id = "FRACTION",
    nameResId = R.string.entry_unit_fraction_name,
    choices = listOf(
        EntryUnitChoice(
            0.25,
            R.string.entry_unit_fraction_choice_one_quarter,
            R.string.entry_unit_fraction_choice_one_quarter_html,
        ),
        EntryUnitChoice(
            0.333,
            R.string.entry_unit_fraction_choice_one_third,
            R.string.entry_unit_fraction_choice_one_third_html,
        ),
        EntryUnitChoice(
            0.5,
            R.string.entry_unit_fraction_choice_one_half,
            R.string.entry_unit_fraction_choice_one_half_html,
        ),
        EntryUnitChoice(
            0.75,
            R.string.entry_unit_fraction_choice_three_quarters,
            R.string.entry_unit_fraction_choice_three_quarters_html,
        ),
        EntryUnitChoice(
            1.0,
            R.string.entry_unit_fraction_choice_whole,
            R.string.entry_unit_fraction_choice_whole_html,
        ),
    ),
)
val smallNumbersChoiceUnit = EntryUnit(
    id = "SMALL_NUMBERS_CHOICE",
    nameResId = R.string.entry_unit_portion_name,
    choices = listOf(
        EntryUnitChoice(1.0, R.string.entry_unit_portion_choice_1),
        EntryUnitChoice(2.0, R.string.entry_unit_portion_choice_2),
        EntryUnitChoice(3.0, R.string.entry_unit_portion_choice_3),
        EntryUnitChoice(4.0, R.string.entry_unit_portion_choice_4),
        EntryUnitChoice(5.0, R.string.entry_unit_portion_choice_5),
    ),
)
val doubleUnit = EntryUnit(
    id = "DOUBLE",
    nameResId = R.string.entry_unit_double_name,
)
val units: List<EntryUnit> = listOf(
    noneUnit,
    clothingSizeUnit,
    fractionUnit,
    smallNumbersChoiceUnit,
    doubleUnit,
)
val defaultVisibleUnit = clothingSizeUnit
val visibleUnits: List<EntryUnit> = listOf(
    defaultVisibleUnit,
    fractionUnit,
    doubleUnit,
)
