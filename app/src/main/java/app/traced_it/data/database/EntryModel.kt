package app.traced_it.data.database

import android.content.res.Resources
import android.database.Cursor
import androidx.paging.PagingSource
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.TypeConverter
import androidx.room.Update
import androidx.room.Upsert
import app.traced_it.R
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID
import kotlin.time.Instant

@Entity
data class Entry(
    val amount: Double,
    val amountUnit: EntryUnit,
    val content: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val deleted: Boolean = false,
    @PrimaryKey(autoGenerate = true) val uid: Int = 0, // Use integer primary key instead of UUID, because FTS supports only integers
    @ColumnInfo(index = true, defaultValue = DEFAULT_UUID_VALUE) val uuid: UUID = UUID.randomUUID(),
) {
    val createdAtInstant by lazy { Instant.fromEpochMilliseconds(createdAt) }

    fun formatContentWithAmount(resources: Resources): String =
        buildString {
            append(content)
            if (amountUnit != NoneUnit) {
                append(" (")
                append(amountUnit.format(resources, amount))
                append(")")
            }
        }

    fun formatContentWithAmountAndCreatedAt(resources: Resources, locale: Locale, timeZone: TimeZone): String =
        resources.getString(
            R.string.entry_formatted_content_with_amount_and_created_at,
            formatContentWithAmount(resources),
            dateTimeFormatter.withLocale(locale)
                .format(createdAtInstant.toLocalDateTime(timeZone).toJavaLocalDateTime())
        )

    fun dateEquals(other: Entry, timeZone: TimeZone): Boolean =
        createdAtInstant.toLocalDateTime(timeZone).date == other.createdAtInstant.toLocalDateTime(timeZone).date

    fun dateEquals(other: LocalDate, timeZone: TimeZone): Boolean =
        createdAtInstant.toLocalDateTime(timeZone).date == other

    fun formatExactTime(resources: Resources, locale: Locale, timeZone: TimeZone): String =
        resources.getString(
            R.string.list_item_time_at,
            timeFormatter.withLocale(locale)
                .format(createdAtInstant.toLocalDateTime(timeZone).toJavaLocalDateTime()),
        )

    fun formatRelativeTime(resources: Resources, now: Instant, locale: Locale, timeZone: TimeZone): String =
        now.minus(createdAtInstant).toComponents { hours, minutes, seconds, _ ->
            if (hours !in 0..23 || minutes < 0 || seconds < 0) {
                // More than 24 hours ago, or in the future
                formatExactTime(resources, locale, timeZone)
            } else if (hours > 0) {
                // Between 1 hour and 24 hours ago
                resources.getString(R.string.list_item_time_ago_hours_and_minutes, hours, minutes)
            } else if (minutes > 0) {
                // Less than 1 hour ago
                resources.getString(R.string.list_item_time_ago_minutes, minutes)
            } else {
                // Less than 1 minute ago
                resources.getString(R.string.list_item_time_ago_seconds, seconds)
            }
        }

    fun formatDate(locale: Locale, timeZone: TimeZone): String =
        dateFormatter.withLocale(locale)
            .format(createdAtInstant.toLocalDateTime(timeZone).toJavaLocalDateTime())

    private companion object {
        private const val DEFAULT_UUID_VALUE =
            @Suppress("GrazieInspectionRunner", "SpellCheckingInspection") "(RANDOMBLOB(16))"
        private val dateTimeFormatter: DateTimeFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    }
}

@Entity(tableName = "entry_fts")
@Fts4(contentEntity = Entry::class)
data class EntryFTS(
    @ColumnInfo(name = "content") val content: String,
)

@Entity
data class EntryDraft(
    val amount: Double,
    val amountUnit: EntryUnit,
    val content: String,
    val createdAt: Long,
    @ColumnInfo(index = true) val entryUid: Int,
    @PrimaryKey val uid: Int,
)

class EntryTypeConverters {
    @TypeConverter
    fun unitIdToUnit(unitId: String): EntryUnit? = units.firstOrNull { unit -> unit.id == unitId }

    @TypeConverter
    fun unitToUnitId(unit: EntryUnit): String = unit.id
}

typealias IndexAndUid = Pair<Int, Int>

@Dao
abstract class EntryDao {
    @Query("SELECT COUNT(uid) FROM entry WHERE NOT deleted")
    abstract fun count(): Flow<Int>

    @Query("SELECT * FROM entry WHERE NOT deleted ORDER BY createdAt DESC")
    abstract fun getAll(): PagingSource<Int, Entry>

    @Query("SELECT * FROM entry WHERE NOT deleted ORDER BY createdAt DESC")
    abstract fun getAllAsCursor(): Cursor

    @Query("SELECT * FROM entry WHERE uid = :uid")
    abstract suspend fun getByUid(uid: Int): Entry?

    @Query("SELECT * FROM entry WHERE uuid = :uuid")
    abstract suspend fun getByUuid(uuid: UUID): Entry?

    @Query("SELECT * FROM entry WHERE createdAt = :createdAt")
    abstract suspend fun getByCreatedAt(createdAt: Long): Entry?

    @Query("SELECT COUNT(*) FROM entry WHERE NOT deleted AND createdAt > (SELECT createdAt FROM entry WHERE uid = :uid)")
    abstract suspend fun getIndexOf(uid: Int): Int

    @Query(
        """
        SELECT * FROM entry
        JOIN entry_fts ON entry_fts.rowid = entry.uid
        WHERE entry_fts MATCH :fullTextQueryExpression
        AND NOT deleted
        ORDER BY createdAt DESC
    """
    )
    abstract fun search(fullTextQueryExpression: String): PagingSource<Int, Entry>

    @Query(
        """
        SELECT * FROM entry
        JOIN entry_fts ON entry_fts.rowid = entry.uid
        WHERE entry_fts MATCH :fullTextQueryExpression
        AND NOT deleted
        ORDER BY createdAt DESC
    """
    )
    abstract fun searchAsCursor(fullTextQueryExpression: String): Cursor

    @Insert
    abstract suspend fun insert(entry: Entry): Long

    @Update
    abstract suspend fun update(vararg entries: Entry): Int

    @Query("UPDATE entry SET deleted = 1 WHERE uid = :uid")
    abstract suspend fun delete(uid: Int)

    @Query("UPDATE entry SET deleted = 0 WHERE uid = :uid")
    abstract suspend fun restore(uid: Int)

    @Transaction
    open suspend fun restoreAndGetIndex(uid: Int): Int {
        restore(uid)
        return getIndexOf(uid)
    }

    @Query("UPDATE entry SET deleted = 1")
    abstract suspend fun deleteAll()

    @Query("UPDATE entry SET deleted = 0 WHERE deleted = 1")
    abstract suspend fun restoreAll()

    @Query("DELETE FROM entry WHERE deleted = 1")
    abstract suspend fun cleanupDeleted()

    @Query("SELECT * FROM entrydraft LIMIT 1")
    abstract suspend fun getFirstDraft(): EntryDraft?

    @Query("SELECT * FROM entrydraft LIMIT 1")
    abstract fun draft(): Flow<EntryDraft?>

    @Transaction
    open suspend fun upsertEntryFromDraft(draft: EntryDraft): IndexAndUid? {
        deleteDraft(draft)
        val newEntryUid = if (draft.entryUid == -1) {
            val entry = Entry(
                amount = draft.amount,
                amountUnit = draft.amountUnit,
                content = draft.content,
                createdAt = draft.createdAt,
            )
            val rowId = insert(entry)
            rowId.toInt()
        } else {
            val entry = getByUid(draft.entryUid)?.copy(
                amount = draft.amount,
                amountUnit = draft.amountUnit,
                content = draft.content,
                createdAt = draft.createdAt,
            ) ?: return null
            update(entry)
            entry.uid
        }
        val index = getIndexOf(newEntryUid)
        return index to newEntryUid
    }

    @Insert
    abstract suspend fun insertDraft(draft: EntryDraft): Long

    @Upsert
    abstract suspend fun upsertDraft(draft: EntryDraft)

    @Delete
    abstract suspend fun deleteDraft(draft: EntryDraft)
}
