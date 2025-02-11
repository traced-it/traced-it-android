package app.traced_it.data.local.database

import androidx.room.*

class Converters {
    @TypeConverter
    fun unitIdToUnit(unitId: String): EntryUnit? =
        units.find { unit -> unit.id == unitId }

    @TypeConverter
    fun unitToUnitId(unit: EntryUnit): String = unit.id
}

@Database(
    entities = [Entry::class],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
