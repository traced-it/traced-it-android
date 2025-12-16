package app.traced_it.data

import androidx.paging.PagingSource
import app.traced_it.data.local.database.Entry
import app.traced_it.data.local.database.EntryDao
import app.traced_it.data.local.database.createFullTextQueryExpression
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject

interface EntryRepository {
    fun count(): Flow<Int>

    suspend fun getByUid(uid: UUID): Entry?

    suspend fun getByCreatedAt(createdAt: Long): Entry?

    fun getLatest(): Flow<Entry?>

    fun filter(unsafeQuery: String = ""): PagingSource<Int, Entry>

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
