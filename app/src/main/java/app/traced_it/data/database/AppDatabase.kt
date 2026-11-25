package app.traced_it.data.database

import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Entry::class, EntryDraft::class, EntryFTS::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3, spec = AppDatabase.AutoMigration2To3::class),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(EntryTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    class AutoMigration2To3 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            super.onPostMigrate(db)
            db.execSQL("INSERT INTO entry_fts(entry_fts) VALUES ('rebuild')")
        }
    }
}
