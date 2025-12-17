package app.traced_it.data

import android.annotation.SuppressLint
import androidx.paging.PagingSource
import androidx.room.util.convertByteToUUID
import app.traced_it.data.local.database.*
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject

interface EntryRepository {
    fun count(): Flow<Int>

    suspend fun getByUid(uid: UUID): Entry?

    suspend fun getByCreatedAt(createdAt: Long): Entry?

    fun getLatest(): Flow<Entry?>

    fun filter(unsafeQuery: String = ""): PagingSource<Int, Entry>

    fun filterAsSequence(unsafeQuery: String = ""): Sequence<Entry>

    suspend fun insert(entry: Entry): Long

    suspend fun update(vararg entries: Entry)

    suspend fun delete(uid: UUID)

    suspend fun restore(uid: UUID)

    suspend fun deleteAll()

    suspend fun restoreAll()

    suspend fun cleanupDeleted()
}

class DefaultEntryRepository @Inject constructor(
    private val entryDao: EntryDao,
) : EntryRepository {
    override fun count(): Flow<Int> = entryDao.count()

    override fun filter(unsafeQuery: String): PagingSource<Int, Entry> =
        if (unsafeQuery.isNotEmpty()) {
            entryDao.search(createFullTextQueryExpression(unsafeQuery))
        } else {
            entryDao.getAll()
        }

    @SuppressLint("RestrictedApi")
    override fun filterAsSequence(unsafeQuery: String): Sequence<Entry> = sequence {
        val cursor = if (unsafeQuery.isNotEmpty()) {
            entryDao.searchAsCursor(createFullTextQueryExpression(unsafeQuery))
        } else {
            entryDao.getAllAsCursor()
        }
        with(cursor) {
            while (moveToNext()) {
                yield(
                    Entry(
                        amount = cursor.getDouble(0),
                        amountUnit = convertUnitIdToUnit(cursor.getString(1)) ?: noneUnit,
                        content = cursor.getString(2),
                        createdAt = cursor.getLong(3),
                        deleted = cursor.getInt(4) == 1,
                        uid = convertByteToUUID(cursor.getBlob(5)),
                    )
                )
            }
        }
        cursor.close()
    }

    override suspend fun getByUid(uid: UUID): Entry? = entryDao.getByUid(uid)

    override suspend fun getByCreatedAt(createdAt: Long): Entry? = entryDao.getByCreatedAt(createdAt)

    override fun getLatest(): Flow<Entry?> = entryDao.getLatest()

    override suspend fun insert(entry: Entry): Long = entryDao.insert(entry)

    override suspend fun update(vararg entries: Entry) = entryDao.update(*entries)

    override suspend fun delete(uid: UUID) = entryDao.delete(uid)

    override suspend fun restore(uid: UUID) = entryDao.restore(uid)

    override suspend fun deleteAll() = entryDao.deleteAll()

    override suspend fun restoreAll() = entryDao.restoreAll()

    override suspend fun cleanupDeleted() = entryDao.cleanupDeleted()
}
