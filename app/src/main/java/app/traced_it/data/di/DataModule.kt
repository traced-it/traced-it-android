package app.traced_it.data.di

import android.annotation.SuppressLint
import androidx.paging.PagingSource
import androidx.paging.testing.asPagingSourceFactory
import app.traced_it.data.DefaultEntryRepository
import app.traced_it.data.EntryRepository
import app.traced_it.data.local.database.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused")
interface DataModule {

    @Singleton
    @Binds
    fun bindsEntryRepository(
        entryRepository: DefaultEntryRepository,
    ): EntryRepository
}

class FakeEntryRepository @Inject constructor() : EntryRepository {
    var fakeEntries: MutableList<Entry> = defaultFakeEntries
        .filter { !it.deleted }
        .sortedBy { it.createdAt }
        .reversed()
        .toMutableList()

    @SuppressLint("VisibleForTests")
    override fun getAll(): PagingSource<Int, Entry> {
        val pagingSourceFactory = this.fakeEntries
            .filter { !it.deleted }
            .sortedBy { it.createdAt }
            .reversed()
            .asPagingSourceFactory()
        return pagingSourceFactory()
    }

    override suspend fun findByCreatedAt(createdAt: Long): Entry? =
        this.fakeEntries.find { it.createdAt == createdAt }

    override suspend fun insert(entry: Entry): Long {
        fakeEntries.add(entry)
        return fakeEntries.size.toLong()
    }

    override suspend fun update(vararg entries: Entry) {
        throw NotImplementedError()
    }

    override suspend fun delete(uid: Int) {
        throw NotImplementedError()
    }

    override suspend fun restore(uid: Int) {
        throw NotImplementedError()
    }

    override suspend fun deleteAll() {
        throw NotImplementedError()
    }

    override suspend fun restoreAll() {
        throw NotImplementedError()
    }

    override suspend fun cleanupDeleted() {
        throw NotImplementedError()
    }
}

// Use java.util.Calendar for compatibility with API 25
val defaultFakeEntries = listOf(
    Entry(
        uid = 1,
        amount = 4.0,
        amountUnit = clothingSizeUnit,
        content = "Coffee",
        createdAt = Calendar.getInstance().run {
            add(Calendar.SECOND, -30)
            time.time
        },
    ),
    Entry(
        uid = 2,
        amount = 0.0,
        amountUnit = noneUnit,
        content = "Watered plants",
        createdAt = Calendar.getInstance().run {
            add(Calendar.MINUTE, -9)
            time.time
        },
    ),
    Entry(
        uid = 3,
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
        uid = 4,
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
        uid = 5,
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
        uid = 6,
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
        uid = 7,
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
    Entry(
        uid = 8,
        amount = 0.0,
        amountUnit = clothingSizeUnit,
        content = """
            Jetpack Compose is Android’s recommended modern toolkit for building
            native UI. It simplifies and accelerates UI development on Android.
            Quickly bring your app to life with less code, powerful tools, and
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
        uid = 9,
        amount = 0.0,
        amountUnit = noneUnit,
        content = "Future",
        createdAt = Calendar.getInstance().run {
            add(Calendar.DAY_OF_MONTH, 1)
            time.time
        },
    ),
)
