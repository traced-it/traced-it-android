package app.traced_it.data

import androidx.paging.PagingSource
import app.traced_it.data.local.database.Entry
import app.traced_it.data.local.database.EntryDao
import javax.inject.Inject

interface EntryRepository {
    fun getAll(): PagingSource<Int, Entry>

    suspend fun findByCreatedAt(createdAt: Long): Entry?

    suspend fun insert(entry: Entry): Long

    suspend fun update(vararg entries: Entry)

    suspend fun delete(uid: Int)

    suspend fun undelete(uid: Int)

    suspend fun deleteAll()

    suspend fun undeleteAll()

    suspend fun cleanupDeleted()
}

class DefaultEntryRepository @Inject constructor(
    private val entryDao: EntryDao,
) : EntryRepository {
    override fun getAll(): PagingSource<Int, Entry> = entryDao.getAll()

    override suspend fun findByCreatedAt(createdAt: Long): Entry? =
        entryDao.findByCreatedAt(createdAt)

    override suspend fun insert(entry: Entry): Long = entryDao.insert(entry)

    override suspend fun update(vararg entries: Entry) =
        entryDao.update(*entries)

    override suspend fun delete(uid: Int) = entryDao.delete(uid)

    override suspend fun undelete(uid: Int) = entryDao.undelete(uid)

    override suspend fun deleteAll() = entryDao.deleteAll()

    override suspend fun undeleteAll() = entryDao.undeleteAll()

    override suspend fun cleanupDeleted() = entryDao.cleanupDeleted()
}
