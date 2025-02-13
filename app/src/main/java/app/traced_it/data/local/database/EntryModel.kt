package app.traced_it.data.local.database

import android.content.Context
import android.text.format.DateUtils
import androidx.paging.PagingSource
import androidx.room.*
import app.traced_it.R
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
        if (amountUnit == noneUnit) {
            content
        } else {
            context.resources.getString(
                R.string.entry_formatted_content_with_amount,
                content,
                amountUnit.format(context, amount),
                amountUnit.unit,
            )
        }

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

    fun formatTime(
        context: Context,
        now: Long,
    ): String =
        (now - createdAt).milliseconds.toComponents { hours, minutes, seconds, _ ->
            if (hours >= 24) {
                context.resources.getString(
                    R.string.list_item_time_at,
                    DateUtils.formatDateTime(
                        context,
                        createdAt,
                        DateUtils.FORMAT_SHOW_TIME,
                    ),
                )
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

    fun getHeader(
        context: Context,
        prevEntry: Entry?
    ): String? =
        if (prevEntry == null && !DateUtils.isToday(createdAt) ||
                prevEntry != null && !isSameDay(context, prevEntry)) {
            DateUtils.formatDateTime(context, createdAt, DateUtils.FORMAT_SHOW_DATE)
        } else {
            null
        }
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entry WHERE NOT deleted ORDER BY createdAt DESC")
    fun getAll(): PagingSource<Int, Entry>

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
