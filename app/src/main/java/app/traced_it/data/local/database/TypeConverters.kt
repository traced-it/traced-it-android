package app.traced_it.data.local.database

fun convertUnitIdToUnit(unitId: String): EntryUnit? = units.find { unit -> unit.id == unitId }

fun convertUnitToUnitId(unit: EntryUnit): String = unit.id
