package app.traced_it.data.local.database

import android.content.Context
import android.text.format.DateUtils
import androidx.paging.PagingSource
import androidx.room.*
import app.traced_it.R
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration.Companion.milliseconds

@Entity
data class Entry(
    val amount: Double = 0.0,
    val amountUnit: EntryUnit = noneUnit,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") var deleted: Boolean = false,
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
) {
    fun formatContentWithAmount(context: Context): String =
        buildString {
            append(content)
            if (amountUnit != noneUnit) {
                append(" (")
                append(amountUnit.format(context, amount))
                append(")")
            }
        }

    fun format(context: Context): String =
        context.resources.getString(
            R.string.entry_formatted_content_with_amount_and_created_at,
            formatContentWithAmount(context),
            DateUtils.formatDateTime(
                context,
                createdAt,
                DateUtils.FORMAT_SHOW_TIME or
                    DateUtils.FORMAT_SHOW_YEAR or
                    DateUtils.FORMAT_SHOW_DATE,
            )
        )

    fun isSameDay(context: Context, otherEntry: Entry?): Boolean =
        otherEntry !== null && DateUtils.formatDateTime(
            context,
            createdAt,
            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
        ) == DateUtils.formatDateTime(
            context,
            otherEntry.createdAt,
            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_DATE
        )

    fun formatTime(context: Context, now: Long): String =
        (now - createdAt).milliseconds.toComponents { hours, minutes, seconds, _ ->
            if (hours >= 24 || hours < 0 || minutes < 0 || seconds < 0) {
                formatExactTime(context)
            } else if (hours == 0L) {
                if (minutes == 0) {
                    context.resources.getString(
                        R.string.list_item_time_ago_seconds,
                        seconds,
                    )
                } else {
                    context.resources.getString(
                        R.string.list_item_time_ago_minutes,
                        minutes,
                    )
                }
            } else {
                context.resources.getString(
                    R.string.list_item_time_ago_hours_and_minutes,
                    hours,
                    minutes,
                )
            }
        }

    fun formatExactTime(context: Context): String =
        context.resources.getString(
            R.string.list_item_time_at,
            DateUtils.formatDateTime(
                context,
                createdAt,
                DateUtils.FORMAT_SHOW_TIME,
            ),
        )

    fun getHeader(context: Context, prevEntry: Entry?): String? =
        if (prevEntry == null && !DateUtils.isToday(createdAt) ||
            prevEntry != null && !isSameDay(context, prevEntry)
        ) {
            DateUtils.formatDateTime(
                context,
                createdAt,
                DateUtils.FORMAT_SHOW_DATE
            )
        } else {
            null
        }
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry WHERE NOT deleted ORDER BY createdAt DESC")
    fun getAll(): PagingSource<Int, Entry>

    @Query("SELECT * FROM entry WHERE NOT deleted AND amountUnit != 'NONE' ORDER BY createdAt DESC LIMIT 1")
    fun getLatestEntry(): Flow<Entry?>

    @Query(
        "SELECT * FROM entry WHERE content LIKE '%' || :filterQuery || '%' ORDER BY createdAt DESC"
    )
    fun findByContent(filterQuery: String): PagingSource<Int, Entry>

    @Query("SELECT * FROM entry WHERE createdAt = :createdAt AND NOT deleted")
    suspend fun findByCreatedAt(createdAt: Long): Entry?

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(vararg entries: Entry)

    @Query("UPDATE entry SET deleted = 1 WHERE uid = :uid")
    suspend fun delete(uid: Int)

    @Query("UPDATE entry SET deleted = 0 WHERE uid = :uid")
    suspend fun restore(uid: Int)

    @Query("UPDATE entry SET deleted = 1")
    suspend fun deleteAll()

    @Query("UPDATE entry SET deleted = 0 WHERE deleted = 1")
    suspend fun restoreAll()

    @Query("DELETE FROM entry WHERE deleted = 1")
    suspend fun cleanupDeleted()
}
