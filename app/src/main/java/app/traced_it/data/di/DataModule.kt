package app.traced_it.data.di

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.traced_it.BuildConfig
import app.traced_it.data.DefaultEntryRepository
import app.traced_it.data.EntryRepository
import app.traced_it.data.local.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Singleton
    @Provides
    @Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
    fun provideEntryRepository(entryDao: EntryDao): EntryRepository =
        if (BuildConfig.BUILD_TYPE == "demo") {
            FakeEntryRepository(demoEntries)
        } else {
            DefaultEntryRepository(entryDao)
        }
}

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

@OptIn(ExperimentalCoroutinesApi::class)
class FakeEntryRepository(
    initialFakeEntries: List<Entry> = defaultFakeEntries,
) : EntryRepository {
    private val _fakeEntries: MutableStateFlow<List<Entry>> =
        MutableStateFlow(initialFakeEntries)
    val fakeEntries: Flow<List<Entry>> = _fakeEntries.mapLatest {
        it
            .filter { entry -> !entry.deleted }
            .sortedBy { entry -> entry.createdAt }
            .reversed()
    }
    private var lastPagingSource: PagingSource<Int, Entry>? = null

    private fun updateEntries(entries: List<Entry>) {
        _fakeEntries.value = entries
        lastPagingSource?.invalidate()
    }

    override fun count(): Flow<Int> = _fakeEntries.mapLatest { it.size }

    override fun filter(unsafeQuery: String): PagingSource<Int, Entry> =
        ListFlowPagingSource(
            if (unsafeQuery.isEmpty()) {
                fakeEntries
            } else {
                fakeEntries.mapLatest {
                    it.filter { entry -> unsafeQuery in entry.content }
                }
            }
        ).also {
            lastPagingSource = it
        }

    override suspend fun getByUid(uid: UUID): Entry? =
        _fakeEntries.value.find { it.uid == uid }

    override suspend fun getByCreatedAt(createdAt: Long): Entry? =
        _fakeEntries.value.find { it.createdAt == createdAt }

    override fun getLatest(): Flow<Entry?> =
        _fakeEntries.mapLatest { entries ->
            entries
                .filter { !it.deleted && it.amountUnit != noneUnit }
                .maxByOrNull { it.createdAt }
        }

    override suspend fun insert(entry: Entry): Long =
        (_fakeEntries.value + listOf(entry)).also {
            updateEntries(it)
        }.size.toLong()

    override suspend fun update(vararg entries: Entry) {
        for (entry in entries) {
            updateEntries(_fakeEntries.value.map {
                if (it.uid == entry.uid) {
                    entry
                } else {
                    it
                }
            })
        }
    }

    override suspend fun delete(uid: UUID) {
        updateEntries(_fakeEntries.value.map {
            if (it.uid == uid) {
                it.copy(deleted = true)
            } else {
                it
            }
        })
    }

    override suspend fun restore(uid: UUID) {
        updateEntries(_fakeEntries.value.map {
            if (it.uid == uid) {
                it.copy(deleted = false)
            } else {
                it
            }
        })
    }

    override suspend fun deleteAll() {
        updateEntries(_fakeEntries.value.map { it.copy(deleted = true) })
    }

    override suspend fun restoreAll() {
        updateEntries(_fakeEntries.value.map { it.copy(deleted = false) })
    }

    override suspend fun cleanupDeleted() {
        updateEntries(_fakeEntries.value.filterNot { it.deleted })
    }
}

// Use java.util.Calendar for compatibility with API 25

val demoEntries = listOf(
    Entry(
        amount = 4.0,
        amountUnit = clothingSizeUnit,
        content = "Coffee",
        createdAt = Calendar.getInstance().run {
            add(Calendar.SECOND, -30)
            time.time
        },
    ),
    Entry(
        amount = 0.0,
        amountUnit = noneUnit,
        content = "Watered plants",
        createdAt = Calendar.getInstance().run {
            add(Calendar.MINUTE, -9)
            time.time
        },
    ),
    Entry(
        amount = 25.0,
        amountUnit = noneUnit,
        content = "Woke up",
        createdAt = Calendar.getInstance().run {
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
    ),
    Entry(
        amount = 0.5,
        amountUnit = fractionUnit,
        content = "Sleeping pill",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 10)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
    ),
    Entry(
        amount = 35.0,
        amountUnit = doubleUnit,
        content = "Pull-ups",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
    ),
    Entry(
        amount = 50.0,
        amountUnit = doubleUnit,
        content = "Squats",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 27)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
    ),
    Entry(
        amount = 0.0,
        amountUnit = noneUnit,
        content = "Started using traced it",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            time.time
        },
    ),
)

val defaultFakeEntries = listOf(
    *demoEntries.toTypedArray(),
    Entry(
        amount = 0.0,
        amountUnit = clothingSizeUnit,
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
    ),
    Entry(
        amount = 0.0,
        amountUnit = noneUnit,
        content = "Future",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, 1)
            time.time
        },
    ),
)
