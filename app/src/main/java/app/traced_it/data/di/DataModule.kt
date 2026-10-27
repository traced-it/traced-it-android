package app.traced_it.data.di

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.traced_it.data.EntryRepository
import app.traced_it.data.database.ClothingSizeUnit
import app.traced_it.data.database.DoubleUnit
import app.traced_it.data.database.Entry
import app.traced_it.data.database.EntryDraft
import app.traced_it.data.database.FractionUnit
import app.traced_it.data.database.IndexAndUid
import app.traced_it.data.database.NoneUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Clock

class ListFlowPagingSource<T : Any>(
    private val listFlow: Flow<List<T>>,
) : PagingSource<Int, T>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        try {
            val pageKey = params.key ?: 0
            val pageSize = params.loadSize

            val items = listFlow.first()

            val startIndex = pageKey * pageSize
            val endIndex = minOf(startIndex + pageSize, items.size)

            val nextKey = if (endIndex < items.size) pageKey + 1 else null
            val prevKey = if (pageKey > 0) pageKey - 1 else null

            val pageItems = if (startIndex < items.size) {
                items.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            return LoadResult.Page(
                data = pageItems,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            return LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, T>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

class FakeEntryRepository @Inject constructor(
    initialEntries: List<Entry> = defaultFakeEntries,
    initialEntryDrafts: List<EntryDraft> = emptyList(),
) : EntryRepository {
    override val clock: Clock = Clock.System

    private val _entries: MutableStateFlow<List<Entry>> = MutableStateFlow(initialEntries)
    val entries: Flow<List<Entry>> =
        _entries.map {
            it
                .filter { entry -> !entry.deleted }
                .sortedBy { entry -> entry.createdAt }
                .reversed()
        }

    private var lastPagingSource: PagingSource<Int, Entry>? = null

    private fun updateEntries(entries: List<Entry>) {
        _entries.value = entries
        lastPagingSource?.invalidate()
    }

    override fun count() = _entries.map { it.size }

    override fun filter(unsafeQuery: String) =
        ListFlowPagingSource(
            if (unsafeQuery.isEmpty()) {
                entries
            } else {
                entries.map {
                    it.filter { entry -> unsafeQuery in entry.content }
                }
            }
        ).also {
            lastPagingSource = it
        }

    override fun filterAsSequence(unsafeQuery: String) =
        _entries.value
            .filter { entry -> !entry.deleted && unsafeQuery.isEmpty() || unsafeQuery in entry.content }
            .sortedBy { entry -> entry.createdAt }
            .reversed()
            .asSequence()

    override suspend fun getByUid(uid: Int) =
        _entries.value.firstOrNull { it.uid == uid }

    override suspend fun getByUuid(uuid: UUID) =
        _entries.value.firstOrNull { it.uuid == uuid }

    override suspend fun getByCreatedAt(createdAt: Long) =
        _entries.value.firstOrNull { it.createdAt == createdAt }

    private suspend fun getIndexOf(uid: Int): Int {
        val entry = getByUid(uid) ?: return 0
        return _entries.value
            .filter { !it.deleted && it.createdAt > entry.createdAt }
            .size
    }

    override suspend fun insert(entry: Entry): Long {
        updateEntries(_entries.value + listOf(entry))
        return _entries.value.size.toLong()
    }

    override suspend fun update(vararg entries: Entry): Int {
        var updatedCount = 0
        for (entry in entries) {
            updateEntries(
                _entries.value.map {
                    if (it.uid == entry.uid) {
                        updatedCount++
                        entry
                    } else {
                        it
                    }
                }
            )
        }
        return updatedCount
    }

    override suspend fun delete(uid: Int) {
        updateEntries(
            _entries.value.map {
                if (it.uid == uid) {
                    it.copy(deleted = true)
                } else {
                    it
                }
            }
        )
    }

    override suspend fun restoreAndGetIndex(uid: Int): Int {
        updateEntries(
            _entries.value.map {
                if (it.uid == uid) {
                    it.copy(deleted = false)
                } else {
                    it
                }
            }
        )
        return getIndexOf(uid)
    }

    override suspend fun deleteAll() {
        updateEntries(_entries.value.map { it.copy(deleted = true) })
    }

    override suspend fun restoreAll() {
        updateEntries(_entries.value.map { it.copy(deleted = false) })
    }

    override suspend fun cleanupDeleted() {
        updateEntries(_entries.value.filterNot { it.deleted })
    }

    private val _drafts: MutableStateFlow<List<EntryDraft>> = MutableStateFlow(initialEntryDrafts)

    override suspend fun getFirstDraft() =
        _drafts.value.firstOrNull()

    override fun draft() =
        _drafts.map { it.firstOrNull() }

    override suspend fun upsertEntryFromDraft(draft: EntryDraft): IndexAndUid {
        val newEntryUid = if (draft.entryUid == -1) {
            val rowId = insert(
                with(draft) {
                    Entry(
                        amount = amount,
                        amountUnit = amountUnit,
                        content = content,
                        createdAt = createdAt,
                    )
                }
            )
            rowId.toInt()
        } else {
            updateEntries(
                _entries.value.map {
                    if (it.uid == draft.entryUid) {
                        with(draft) {
                            it.copy(
                                amount = amount,
                                amountUnit = amountUnit,
                                content = content,
                                createdAt = createdAt,
                            )
                        }
                    } else {
                        it
                    }
                }
            )
            draft.entryUid
        }
        deleteDraft(draft)
        val index = getIndexOf(newEntryUid)
        return index to newEntryUid
    }

    override suspend fun insertDraft(draft: EntryDraft): Long {
        _drafts.value += draft
        return _drafts.value.size.toLong()
    }

    override suspend fun upsertDraft(draft: EntryDraft) {
        _drafts.value = if (draft.uid == 0) {
            _drafts.value + draft
        } else {
            _drafts.value.map {
                if (it.uid == draft.uid) {
                    draft
                } else {
                    it
                }
            }
        }
    }

    override suspend fun deleteDraft(draft: EntryDraft) {
        _drafts.value = _drafts.value.filterNot { it == draft }
    }
}

// Use java.util.Calendar for compatibility with API 25

val demoEntries = listOf(
    Entry(
        amount = 4.0,
        amountUnit = ClothingSizeUnit,
        content = "Dewberry",
        createdAt = Calendar.getInstance().run {
            add(Calendar.SECOND, -30)
            time.time
        },
        uid = 1,
    ),
    Entry(
        amount = 0.0,
        amountUnit = NoneUnit,
        content = "Watered plants",
        createdAt = Calendar.getInstance().run {
            add(Calendar.MINUTE, -9)
            time.time
        },
        uid = 2,
    ),
    Entry(
        amount = 25.0,
        amountUnit = NoneUnit,
        content = "Woke up",
        createdAt = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
        uid = 3,
    ),
    Entry(
        amount = 0.5,
        amountUnit = FractionUnit,
        content = "Sleeping pill",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
        uid = 4,
    ),
    Entry(
        amount = 35.0,
        amountUnit = DoubleUnit,
        content = "Pull-ups",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
        uid = 5,
    ),
    Entry(
        amount = 50.0,
        amountUnit = DoubleUnit,
        content = "Squats",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 27)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
        uid = 6,
    ),
    Entry(
        amount = 0.0,
        amountUnit = NoneUnit,
        content = "Started using traced it",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
        uid = 7,
    ),
)

val defaultFakeEntries = listOf(
    *demoEntries.toTypedArray(),
    Entry(
        amount = 0.0,
        amountUnit = ClothingSizeUnit,
        content = """
            Jetpack Compose is Android’s recommended modern toolkit for building native UI. It simplifies and
            accelerates UI development on Android. Quickly bring your app to life with less code, powerful tools, and
            intuitive Kotlin APIs.
        """.trimIndent(),
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -50)
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 3)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
        uid = 8,
    ),
    Entry(
        amount = 0.0,
        amountUnit = NoneUnit,
        content = "Future",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, 1)
            time.time
        },
        uid = 9,
    ),
)
