package app.traced_it.data

import androidx.paging.PagingSource
import app.traced_it.data.local.database.Entry
import app.traced_it.data.local.database.EntryDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface EntryRepository {
    fun getAll(filterQuery: String = ""): PagingSource<Int, Entry>

    fun getLatestEntry(): Flow<Entry?>

    suspend fun findByCreatedAt(createdAt: Long): Entry?

    suspend fun insert(entry: Entry): Long

    suspend fun update(vararg entries: Entry)

    suspend fun delete(uid: Int)

    suspend fun restore(uid: Int)

    suspend fun deleteAll()

    suspend fun restoreAll()

    suspend fun cleanupDeleted()
}

class DefaultEntryRepository @Inject constructor(
    private val entryDao: EntryDao,
) : EntryRepository {
    override fun getAll(filterQuery: String): PagingSource<Int, Entry> =
        if (filterQuery.isNotEmpty()) {
            entryDao.findByContent(filterQuery)
        } else {
            entryDao.getAll()
        }

    override fun getLatestEntry(): Flow<Entry?> = entryDao.getLatestEntry()

    override suspend fun findByCreatedAt(createdAt: Long): Entry? =
        entryDao.findByCreatedAt(createdAt)

    override suspend fun insert(entry: Entry): Long = entryDao.insert(entry)

    override suspend fun update(vararg entries: Entry) =
        entryDao.update(*entries)

    override suspend fun delete(uid: Int) = entryDao.delete(uid)

    override suspend fun restore(uid: Int) = entryDao.restore(uid)

    override suspend fun deleteAll() = entryDao.deleteAll()

    override suspend fun restoreAll() = entryDao.restoreAll()

    override suspend fun cleanupDeleted() = entryDao.cleanupDeleted()
}
