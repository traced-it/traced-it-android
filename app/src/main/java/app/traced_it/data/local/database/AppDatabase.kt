package app.traced_it.data.local.database

import android.annotation.SuppressLint
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun unitIdToUnit(unitId: String): EntryUnit? = convertUnitIdToUnit(unitId)

    @TypeConverter
    fun unitToUnitId(unit: EntryUnit): String = convertUnitToUnitId(unit)
}

@Database(
    entities = [Entry::class, EntryFTS::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = AppDatabase.AutoMigration2To3::class),
        AutoMigration(from = 3, to = 4, spec = AppDatabase.AutoMigration3To4::class),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    class AutoMigration2To3 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            db.execSQL("INSERT INTO entry_fts(entry_fts) VALUES ('rebuild')")
        }
    }

    class AutoMigration3To4 : AutoMigrationSpec {
        @SuppressLint("RestrictedApi")
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            @Suppress("SpellCheckingInspection")
            db.execSQL("UPDATE Entry SET uid = RANDOMBLOB(16)")
            db.execSQL("DROP TABLE IF EXISTS entry_fts")
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS entry_fts USING FTS4(content TEXT NOT NULL, content=`Entry`)")
            db.execSQL("INSERT INTO entry_fts(entry_fts) VALUES ('rebuild')")
        }
    }
}
