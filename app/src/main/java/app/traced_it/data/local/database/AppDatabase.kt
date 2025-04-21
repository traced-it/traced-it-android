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
    entities = [Entry::class, EntryFTS::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
    ]
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
