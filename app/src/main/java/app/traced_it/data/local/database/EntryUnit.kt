package app.traced_it.data.local.database

import android.content.Context
import app.traced_it.R
import java.text.NumberFormat
import java.text.ParseException

data class EntryUnitChoice(val value: Double, val nameResId: Int)

data class EntryUnit(
    val id: String,
    val nameResId: Int,
    val unit: String = "",
    val choices: List<EntryUnitChoice> = listOf(),
    val placeholderResId: Int? = null,
) {
    private val numberFormat = NumberFormat.getNumberInstance()

    fun format(context: Context, value: Double): String {
        if (choices.isNotEmpty()) {
            val choice = choices.find { it.value == value }
            if (choice != null) {
                return context.resources.getString(choice.nameResId)
            }
        }
        return numberFormat.format(value)
    }

    fun parse(context: Context, value: String): Double {
        if (choices.isNotEmpty()) {
            val choice = choices.find {
                context.resources.getString(it.nameResId) == value
            }
            if (choice != null) {
                return choice.value
            }
        }
        return try {
            numberFormat.parse(value)?.toDouble()
        } catch (_: ParseException) {
            null
        } ?: 0.0
    }

    fun serialize(value: Double): String = value.toString()

    fun deserialize(value: String): Double = value.toDoubleOrNull() ?: 0.0
}

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
        EntryUnitChoice(4.0, R.string.entry_unit_clothing_choice_xl)
    )
)
val smallNumbersChoiceUnit = EntryUnit(
    id = "SMALL_NUMBERS_CHOICE",
    nameResId = R.string.entry_unit_portion_name,
    choices = listOf(
        EntryUnitChoice(1.0, R.string.entry_unit_portion_choice_1),
        EntryUnitChoice(2.0, R.string.entry_unit_portion_choice_2),
        EntryUnitChoice(3.0, R.string.entry_unit_portion_choice_3),
        EntryUnitChoice(4.0, R.string.entry_unit_portion_choice_4),
        EntryUnitChoice(5.0, R.string.entry_unit_portion_choice_5)
    )
)
val doubleUnit = EntryUnit(
    id = "DOUBLE",
    nameResId = R.string.entry_unit_double_name,
    placeholderResId = R.string.entry_unit_double_placeholder,
)
val units: List<EntryUnit> = listOf(
    noneUnit,
    clothingSizeUnit,
    smallNumbersChoiceUnit,
    doubleUnit,
)
val defaultVisibleUnit = clothingSizeUnit
val visibleUnits: List<EntryUnit> = listOf(
    defaultVisibleUnit,
    smallNumbersChoiceUnit,
    doubleUnit
)
