package app.traced_it.data

import android.annotation.SuppressLint
import androidx.paging.PagingSource
import androidx.room.util.convertByteToUUID
import app.traced_it.data.database.Entry
import app.traced_it.data.database.EntryDao
import app.traced_it.data.database.EntryDraft
import app.traced_it.data.database.IndexAndUid
import app.traced_it.data.database.NoneUnit
import app.traced_it.data.database.toFullTextQueryExpression
import app.traced_it.data.database.units
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

interface EntryRepository {
    val clock: Clock

    fun count(): Flow<Int>

    suspend fun getByUid(uid: Int): Entry?

    suspend fun getByUuid(uuid: UUID): Entry?

    suspend fun getByCreatedAt(createdAt: Long): Entry?

    fun filter(unsafeQuery: String = ""): PagingSource<Int, Entry>

    fun filterAsSequence(unsafeQuery: String = ""): Sequence<Entry>

    suspend fun insert(entry: Entry): Long

    suspend fun update(vararg entries: Entry): Int

    suspend fun delete(uid: Int)

    suspend fun restoreAndGetIndex(uid: Int): Int

    suspend fun deleteAll()

    suspend fun restoreAll()

    suspend fun cleanupDeleted()

    suspend fun getFirstDraft(): EntryDraft?

    fun draft(): Flow<EntryDraft?>

    suspend fun upsertEntryFromDraft(draft: EntryDraft): IndexAndUid?

    suspend fun insertDraft(draft: EntryDraft): Long

    suspend fun upsertDraft(draft: EntryDraft)

    suspend fun deleteDraft(draft: EntryDraft)

    suspend fun createDraftToInsert(): EntryDraft =
        EntryDraft(
            amount = 0.0,
            amountUnit = NoneUnit,
            content = "",
            createdAt = clock.now().toEpochMilliseconds(),
            entryUid = -1,
            uid = 0,
        )

    suspend fun createDraftToRepeat(entry: Entry): EntryDraft =
        with(entry) {
            EntryDraft(
                amount = amount,
                amountUnit = amountUnit,
                content = content,
                createdAt = clock.now().toEpochMilliseconds(),
                entryUid = -1,
                uid = 0,
            )
        }

    suspend fun createDraftToUpdate(entry: Entry): EntryDraft =
        with(entry) {
            EntryDraft(
                amount = amount,
                amountUnit = amountUnit,
                content = content,
                createdAt = createdAt,
                entryUid = uid,
                uid = 0,
            )
        }
}

class DefaultEntryRepository @Inject constructor(
    override val clock: Clock,
    private val entryDao: EntryDao,
) : EntryRepository {
    override fun count(): Flow<Int> = entryDao.count()

    override fun filter(unsafeQuery: String): PagingSource<Int, Entry> =
        if (unsafeQuery.isNotEmpty()) {
            entryDao.search(unsafeQuery.toFullTextQueryExpression())
        } else {
            entryDao.getAll()
        }

    @SuppressLint("RestrictedApi")
    override fun filterAsSequence(unsafeQuery: String): Sequence<Entry> = sequence {
        val cursor = if (unsafeQuery.isNotEmpty()) {
            entryDao.searchAsCursor(unsafeQuery.toFullTextQueryExpression())
        } else {
            entryDao.getAllAsCursor()
        }
        with(cursor) {
            while (moveToNext()) {
                yield(
                    Entry(
                        amount = cursor.getDouble(getColumnIndexOrThrow("amount")),
                        amountUnit = cursor.getString(getColumnIndexOrThrow("amountUnit")).let { unitId ->
                            units.firstOrNull { unit -> unit.id == unitId } ?: NoneUnit
                        },
                        content = cursor.getString(getColumnIndexOrThrow("content")),
                        createdAt = cursor.getLong(getColumnIndexOrThrow("createdAt")),
                        deleted = cursor.getInt(getColumnIndexOrThrow("deleted")) == 1,
                        uuid = convertByteToUUID(cursor.getBlob(getColumnIndexOrThrow("uuid"))),
                    )
                )
            }
        }
        cursor.close()
    }

    override suspend fun getByUid(uid: Int): Entry? = entryDao.getByUid(uid)

    override suspend fun getByUuid(uuid: UUID): Entry? = entryDao.getByUuid(uuid)

    override suspend fun getByCreatedAt(createdAt: Long): Entry? = entryDao.getByCreatedAt(createdAt)

    override suspend fun insert(entry: Entry): Long = entryDao.insert(entry)

    override suspend fun update(vararg entries: Entry) = entryDao.update(*entries)

    override suspend fun delete(uid: Int) = entryDao.delete(uid)

    override suspend fun restoreAndGetIndex(uid: Int) = entryDao.restoreAndGetIndex(uid)

    override suspend fun deleteAll() = entryDao.deleteAll()

    override suspend fun restoreAll() = entryDao.restoreAll()

    override suspend fun cleanupDeleted() = entryDao.cleanupDeleted()

    override suspend fun getFirstDraft() = entryDao.getFirstDraft()

    override fun draft() = entryDao.draft()

    override suspend fun upsertEntryFromDraft(draft: EntryDraft): IndexAndUid? = entryDao.upsertEntryFromDraft(draft)

    override suspend fun insertDraft(draft: EntryDraft) = entryDao.insertDraft(draft)

    override suspend fun upsertDraft(draft: EntryDraft) = entryDao.upsertDraft(draft)

    override suspend fun deleteDraft(draft: EntryDraft) = entryDao.deleteDraft(draft)
}
