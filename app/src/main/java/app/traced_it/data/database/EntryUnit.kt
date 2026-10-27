package app.traced_it.data.database

import android.content.res.Resources
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.traced_it.R
import app.traced_it.data.extensions.toDoubleOrNull
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.text.NumberFormat

private val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
private val placeholderNumberFormat: NumberFormat = NumberFormat.getNumberInstance().apply { minimumFractionDigits = 1 }

interface EntryUnitChoice {
    val value: Double

    fun getName(resources: Resources): String

    @Composable
    fun Label() {
        Text(getName(LocalResources.current))
    }
}

sealed interface EntryUnit {
    val id: String

    @Suppress("SameReturnValue")
    val choices: ImmutableList<EntryUnitChoice>? get() = null

    @Suppress("SameReturnValue")
    val default: Double get() = 0.0

    val placeholder: String get() = placeholderNumberFormat.format(default)

    fun getName(resources: Resources): String

    fun format(resources: Resources, value: Double): String =
        choices?.firstOrNull { it.value == value }?.getName(resources) ?: numberFormat.format(value)

    fun parse(resources: Resources, value: String): Double =
        choices?.firstOrNull { it.getName(resources) == value }?.value ?: numberFormat.toDoubleOrNull(value) ?: default

    fun serialize(value: Double): String = value.toString()

    fun deserialize(value: String): Double = value.toDoubleOrNull() ?: default

    companion object {
        @Suppress("unused")
        val Saver = run {
            val idKey = "id"
            mapSaver(
                save = { mapOf(idKey to it.id) },
                restore = {
                    val id = it[idKey] as? String ?: return@mapSaver null
                    units.firstOrNull { unit -> unit.id == id }
                },
            )
        }
    }
}

sealed interface ActiveEntryUnit : EntryUnit

sealed interface LegacyEntryUnit : EntryUnit {
    val replacedWith: ActiveEntryUnit
}

data object ClothingSizeUnit : ActiveEntryUnit {
    override val id = "CLOTHING_SIZE"

    override val choices = persistentListOf(
        object : EntryUnitChoice {
            override val value = 0.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_clothing_choice_xs)
        },
        object : EntryUnitChoice {
            override val value = 1.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_clothing_choice_s)
        },
        object : EntryUnitChoice {
            override val value = 2.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_clothing_choice_m)
        },
        object : EntryUnitChoice {
            override val value = 3.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_clothing_choice_l)
        },
        object : EntryUnitChoice {
            override val value = 4.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_clothing_choice_xl)
        },
    )

    override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_clothing_name)
}

data object FractionUnit : ActiveEntryUnit {
    override val id = "FRACTION"

    override val choices = persistentListOf(
        object : EntryUnitChoice {
            override val value = 0.25

            override fun getName(resources: Resources) =
                resources.getString(R.string.entry_unit_fraction_choice_one_quarter)

            @Composable
            override fun Label() {
                Text(
                    stringResource(R.string.entry_unit_fraction_choice_one_quarter),
                    fontSize = fractionFontSize,
                    fontWeight = fontWeight,
                )
            }
        },
        object : EntryUnitChoice {
            override val value = 0.333

            override fun getName(resources: Resources) =
                resources.getString(R.string.entry_unit_fraction_choice_one_third)

            @Composable
            override fun Label() {
                Text(
                    stringResource(R.string.entry_unit_fraction_choice_one_third),
                    fontSize = fractionFontSize,
                    fontWeight = fontWeight,
                )
            }
        },
        object : EntryUnitChoice {
            override val value = 0.5

            override fun getName(resources: Resources) =
                resources.getString(R.string.entry_unit_fraction_choice_one_half)

            @Composable
            override fun Label() {
                Text(
                    stringResource(R.string.entry_unit_fraction_choice_one_half),
                    fontSize = fractionFontSize,
                    fontWeight = fontWeight,
                )
            }
        },
        object : EntryUnitChoice {
            override val value = 0.75

            override fun getName(resources: Resources) =
                resources.getString(R.string.entry_unit_fraction_choice_three_quarters)

            @Composable
            override fun Label() {
                Text(
                    stringResource(R.string.entry_unit_fraction_choice_three_quarters),
                    fontSize = fractionFontSize,
                    fontWeight = fontWeight,
                )
            }
        },
        object : EntryUnitChoice {
            override val value = 1.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_fraction_choice_whole)

            @Composable
            override fun Label() {
                Text(
                    stringResource(R.string.entry_unit_fraction_choice_whole),
                    fontSize = wholeFontSize,
                    fontWeight = fontWeight,
                )
            }
        },
    )

    override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_fraction_name)

    private val fractionFontSize = 24.sp
    private val wholeFontSize = 18.sp
    private val fontWeight = FontWeight.Light
}

data object DoubleUnit : ActiveEntryUnit {
    override val id = "DOUBLE"

    override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_double_name)
}

data object SmallNumbersChoiceUnit : LegacyEntryUnit {
    override val id = "SMALL_NUMBERS_CHOICE"

    override val choices = persistentListOf(
        object : EntryUnitChoice {
            override val value = 1.0
            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_portion_choice_1)
        },
        object : EntryUnitChoice {
            override val value = 2.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_portion_choice_2)
        },
        object : EntryUnitChoice {
            override val value = 3.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_portion_choice_3)
        },
        object : EntryUnitChoice {
            override val value = 4.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_portion_choice_4)
        },
        object : EntryUnitChoice {
            override val value = 5.0

            override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_portion_choice_5)
        },
    )

    override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_portion_name)

    override val replacedWith = DoubleUnit
}

data object NoneUnit : EntryUnit {
    override val id = "NONE"

    override fun getName(resources: Resources) = resources.getString(R.string.entry_unit_none_name)

    override fun format(resources: Resources, value: Double) = ""

    override fun parse(resources: Resources, value: String) = default
}

fun EntryUnit.toActiveUnit(defaultUnit: ActiveEntryUnit): ActiveEntryUnit =
    when (this) {
        is NoneUnit -> defaultUnit
        is ActiveEntryUnit -> this
        is LegacyEntryUnit -> this.replacedWith
    }

/**
 * All existing units (active and legacy) in the order that should be displayed in the UI.
 *
 * Legacy units will be replaced with active ones in the UI.
 */
val units = persistentListOf(
    ClothingSizeUnit,
    FractionUnit,
    DoubleUnit,
    SmallNumbersChoiceUnit,
    NoneUnit,
)
