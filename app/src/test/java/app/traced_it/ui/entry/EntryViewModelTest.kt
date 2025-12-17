package app.traced_it.ui.entry

import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import app.traced_it.R
import app.traced_it.data.di.FakeEntryRepository
import app.traced_it.data.local.database.*
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class EntryViewModelTest {

    private lateinit var mockResources: Resources

    @Before
    fun before() {
        // Set time zone, so that CSV export produces the same result when
        // tested and doesn't depend on the time zone of the machine that is
        // running the tests.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"))

        mockResources = mock<Resources> {
            on { getString(R.string.entry_unit_clothing_name) } doReturn "clothing"
            on { getString(R.string.entry_unit_fraction_name) } doReturn "fraction"
            on { getString(R.string.entry_unit_fraction_choice_one_quarter) } doReturn "1/4"
            on { getString(R.string.entry_unit_fraction_choice_one_third) } doReturn "1/3"
            on { getString(R.string.entry_unit_fraction_choice_one_half) } doReturn "1/2"
            on { getString(R.string.entry_unit_fraction_choice_three_quarters) } doReturn "3/4"
            on { getString(R.string.entry_unit_fraction_choice_whole) } doReturn "1"
            on { getString(R.string.entry_unit_clothing_choice_xs) } doReturn "XS"
            on { getString(R.string.entry_unit_clothing_choice_s) } doReturn "S"
            on { getString(R.string.entry_unit_clothing_choice_m) } doReturn "M"
            on { getString(R.string.entry_unit_clothing_choice_l) } doReturn "L"
            on { getString(R.string.entry_unit_clothing_choice_xl) } doReturn "XL"
            on { getString(R.string.entry_unit_portion_name) } doReturn "portion"
            on { getString(R.string.entry_unit_portion_choice_1) } doReturn "1x"
            on { getString(R.string.entry_unit_portion_choice_2) } doReturn "2x"
            on { getString(R.string.entry_unit_portion_choice_3) } doReturn "3x"
            on { getString(R.string.entry_unit_portion_choice_4) } doReturn "4x"
            on { getString(R.string.entry_unit_portion_choice_5) } doReturn "5x"
            on { getString(R.string.entry_unit_double_name) } doReturn "number"
            on { getString(R.string.entry_unit_none_name) } doReturn "no unit"
            on { getString(R.string.entry_unit_none_choice_empty) } doReturn ""
            on {
                getQuantityString(R.plurals.list_export_finished, 5, 5)
            } doReturn "Exported 5 notes"
            on { getString(R.string.list_export_in_progress) } doReturn "Exporting notes…"
            on {
                getString(
                    R.string.list_import_failed_column_parsing_error,
                    "createdAt",
                    "INVALID_DATE"
                )
            } doReturn "Failed to parse \"createdAt\" value \"INVALID_DATE\""
            on { getString(R.string.list_import_finished_delimiter) } doReturn " "
            on {
                getQuantityString(R.plurals.list_import_finished_imported, 5, 5)
            } doReturn "Imported 5 notes."
            on {
                getQuantityString(R.plurals.list_import_finished_imported, 3, 3)
            } doReturn "Imported 3 notes."
            on {
                getQuantityString(R.plurals.list_import_finished_skipped, 1, 1)
            } doReturn "Skipped 1 note."
            on {
                getQuantityString(R.plurals.list_import_finished_updated, 1, 1)
            } doReturn "Updated 1 note."
            on {
                getQuantityString(R.plurals.list_import_finished_updated, 2, 2)
            } doReturn "Updated 2 notes."
            on { getString(R.string.list_import_finished_empty) } doReturn "Empty CSV file"
            on { getString(R.string.list_import_finished_delimiter) } doReturn " "
            on { getString(R.string.list_import_in_progress) } doReturn "Importing notes…"
        }
    }

    @Test
    fun `importEntriesCsv inserts valid entries, updates entries with the same uuid, and aborts after failing to parse a row`() =
        runTest {
            val entryRepository = FakeEntryRepository(emptyList())
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )

            @Suppress("SpellCheckingInspection")
            val csv = """
                createdAt,content,amountFormatted,amount,amountUnit,uuid
                2025-02-01T18:00:22.755+0100,"Red apples",,0.0,NONE,8be47977-3577-4534-993c-c14f2fccc8ef
                2025-02-01T18:00:21.000+0100,"Red apples duplicate",,0.0,NONE,8be47977-3577-4534-993c-c14f2fccc8ef
                2025-02-01T15:18:43.189+0100,"Yellow bananas",2x,2.0,SMALL_NUMBERS_CHOICE,85f2ff1f-1424-40ac-b45e-e8381d84005b
                2025-02-01T15:16:56.985+0100,"Green kiwis",L,3.0,CLOTHING_SIZE,98fb296e-29f3-4e6e-b7d2-646976cd2e0f
                2025-02-01T15:00:00.000+0100,"Purple grapes",3.14,3.14,DOUBLE,eee93824-8533-455e-8622-0dc2a24ef584
                2025-02-01T07:00:00.000+0100,"Pineapple",1/3,0.333,FRACTION,7fa18ae8-191d-46d1-bd86-748e9014ef33
                INVALID_DATE,"Green kiwis invalid",,0.0,NONE,d05d1809-8574-457f-a273-fd2509f1d034
                2025-02-01T01:59:38.771+0100,"Green kiwis not processed",,0.0,NONE,5a38ff2d-39f5-43b6-a3e4-f84814693f35
            """.trimIndent()
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockResources, inputStream).join()

            val expectedEntries = listOf(
                Entry(
                    amount = 0.0,
                    amountUnit = noneUnit,
                    content = "Red apples duplicate",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        18,
                        0,
                        21,
                        0,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uuid = UUID.fromString("8be47977-3577-4534-993c-c14f2fccc8ef"),
                ),
                Entry(
                    amount = 2.0,
                    amountUnit = smallNumbersChoiceUnit,
                    content = "Yellow bananas",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        18,
                        43,
                        189_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uuid = UUID.fromString("85f2ff1f-1424-40ac-b45e-e8381d84005b"),
                ),
                Entry(
                    amount = 3.0,
                    amountUnit = clothingSizeUnit,
                    content = "Green kiwis",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        16,
                        56,
                        985_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uuid = UUID.fromString("98fb296e-29f3-4e6e-b7d2-646976cd2e0f"),
                ),
                Entry(
                    amount = 3.14,
                    amountUnit = doubleUnit,
                    content = "Purple grapes",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        0,
                        0,
                        0,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uuid = UUID.fromString("eee93824-8533-455e-8622-0dc2a24ef584"),
                ),
                Entry(
                    amount = 0.333,
                    amountUnit = fractionUnit,
                    content = "Pineapple",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        7,
                        0,
                        0,
                        0,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uuid = UUID.fromString("7fa18ae8-191d-46d1-bd86-748e9014ef33"),
                ),
            )
            val resultEntries = entryRepository.fakeEntries.first()
            assertEquals(expectedEntries.size, resultEntries.size)
            for ((expectedEntry, fakeEntry) in expectedEntries zip resultEntries) {
                assertEquals(expectedEntry, fakeEntry)
            }
            assertEquals(
                Message(
                    "Imported ${expectedEntries.size} notes. Updated 1 note. Failed to parse \"createdAt\" value \"INVALID_DATE\"",
                    type = Message.Type.ERROR,
                    duration = Message.Duration.LONG,
                ),
                entryViewModel.message.first(),
            )
        }

    @Test
    fun `importEntriesCsv updates and undeletes existing entries`() =
        runTest {
            val entryRepository = FakeEntryRepository(
                listOf(
                    Entry(
                        amount = 0.0,
                        amountUnit = noneUnit,
                        content = "Red apples",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            18,
                            0,
                            22,
                            755_000_000,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 1,
                        uuid = UUID.fromString("8be47977-3577-4534-993c-c14f2fccc8ef"),
                        deleted = false,
                    ),
                    Entry(
                        amount = 3.0,
                        amountUnit = clothingSizeUnit,
                        content = "Green kiwis",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            15,
                            16,
                            56,
                            985_000_000,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 2,
                        uuid = UUID.fromString("98fb296e-29f3-4e6e-b7d2-646976cd2e0f"),
                        deleted = true,
                    ),
                )
            )
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )

            @Suppress("SpellCheckingInspection")
            val csv = """
                createdAt,content,amountFormatted,amount,amountUnit,uuid
                2025-02-01T18:00:55.755+0100,"Red apples updated",,5.9,DOUBLE,8be47977-3577-4534-993c-c14f2fccc8ef
                2025-02-01T15:16:33.985+0100,"Green kiwis updated",XL,4.0,CLOTHING_SIZE,98fb296e-29f3-4e6e-b7d2-646976cd2e0f
            """.trimIndent()
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockResources, inputStream).join()

            val expectedEntries = listOf(
                Entry(
                    amount = 5.9,
                    amountUnit = doubleUnit,
                    content = "Red apples updated",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        18,
                        0,
                        55,
                        755_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uid = 1,
                    uuid = UUID.fromString("8be47977-3577-4534-993c-c14f2fccc8ef"),
                    deleted = false,
                ),
                Entry(
                    amount = 4.0,
                    amountUnit = clothingSizeUnit,
                    content = "Green kiwis updated",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        16,
                        33,
                        985_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                    uid = 2,
                    uuid = UUID.fromString("98fb296e-29f3-4e6e-b7d2-646976cd2e0f"),
                    deleted = false,
                ),
            )
            val resultEntries = entryRepository.fakeEntries.first()
            assertEquals(expectedEntries.size, resultEntries.size)
            for ((expectedEntry, fakeEntry) in expectedEntries zip resultEntries) {
                assertEquals(expectedEntry, fakeEntry)
            }
            assertEquals(
                Message(
                    "Updated 2 notes.",
                    type = Message.Type.SUCCESS,
                    duration = Message.Duration.LONG,
                ),
                entryViewModel.message.first(),
            )
        }

    @Test
    fun `importEntriesCsv skips entries with the same createdAt, when there is no uuid column`() =
        runTest {
            val entryRepository = FakeEntryRepository(emptyList())
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            val csv = """
                createdAt,content,amountFormatted,amount,amountUnit
                2025-02-01T18:00:22.755+0100,"Red apples",,0.0,NONE
                2025-02-01T18:00:22.755+0100,"Red apples duplicate",,0.0,NONE
                2025-02-01T15:18:43.189+0100,"Yellow bananas",2x,2.0,SMALL_NUMBERS_CHOICE
                2025-02-01T15:16:56.985+0100,"Green kiwis",L,3.0,CLOTHING_SIZE
            """.trimIndent()
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockResources, inputStream).join()

            val expectedEntries = listOf(
                Entry(
                    amount = 0.0,
                    amountUnit = noneUnit,
                    content = "Red apples",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        18,
                        0,
                        22,
                        755_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
                Entry(
                    amount = 2.0,
                    amountUnit = smallNumbersChoiceUnit,
                    content = "Yellow bananas",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        18,
                        43,
                        189_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
                Entry(
                    amount = 3.0,
                    amountUnit = clothingSizeUnit,
                    content = "Green kiwis",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        16,
                        56,
                        985_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
            )
            val resultEntries = entryRepository.fakeEntries.first()
            assertEquals(expectedEntries.size, resultEntries.size)
            val testUuid = UUID.randomUUID()
            for ((expectedEntry, fakeEntry) in expectedEntries zip resultEntries) {
                assertEquals(expectedEntry.copy(uuid = testUuid), fakeEntry.copy(uuid = testUuid))
            }
            assertEquals(
                Message(
                    "Imported ${expectedEntries.size} notes. Skipped 1 note.",
                    type = Message.Type.SUCCESS,
                    duration = Message.Duration.LONG,
                ),
                entryViewModel.message.first(),
            )
        }

    @Test
    fun `importEntriesCsv inserts entries from a CSV that has human-readable data in the amount and amountUnit columns`() =
        runTest {
            val entryRepository = FakeEntryRepository(emptyList())
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            val csv = """
                createdAt,content,amount,amountUnit
                2025-02-01T18:00:22.755+0100,"Red apples",0.0,no unit
                2025-02-01T15:18:43.189+0100,"Yellow bananas",2.0,portion
                2025-02-01T15:16:56.985+0100,"Green kiwis",3.0,clothing
                2025-02-01T15:00:00.000+0100,"Purple grapes",3.14,number
                2025-02-01T07:00:00.000+0100,"Pineapple",0.333,fraction
            """.trimIndent()
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockResources, inputStream).join()

            val expectedEntries = listOf(
                Entry(
                    amount = 0.0,
                    amountUnit = noneUnit,
                    content = "Red apples",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        18,
                        0,
                        22,
                        755_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
                Entry(
                    amount = 2.0,
                    amountUnit = smallNumbersChoiceUnit,
                    content = "Yellow bananas",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        18,
                        43,
                        189_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
                Entry(
                    amount = 3.0,
                    amountUnit = clothingSizeUnit,
                    content = "Green kiwis",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        16,
                        56,
                        985_000_000,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
                Entry(
                    amount = 3.14,
                    amountUnit = doubleUnit,
                    content = "Purple grapes",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        15,
                        0,
                        0,
                        0,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
                Entry(
                    amount = 0.333,
                    amountUnit = fractionUnit,
                    content = "Pineapple",
                    createdAt = OffsetDateTime.of(
                        2025,
                        2,
                        1,
                        7,
                        0,
                        0,
                        0,
                        ZoneOffset.of("+01:00")
                    ).toInstant().toEpochMilli(),
                ),
            )
            val resultEntries = entryRepository.fakeEntries.first()
            assertEquals(expectedEntries.size, resultEntries.size)
            val testUuid = UUID.randomUUID()
            for ((expectedEntry, fakeEntry) in expectedEntries zip resultEntries) {
                assertEquals(expectedEntry.copy(uuid = testUuid), fakeEntry.copy(uuid = testUuid))
            }
            assertEquals(
                Message(
                    "Imported ${expectedEntries.size} notes.",
                    type = Message.Type.SUCCESS,
                    duration = Message.Duration.LONG,
                ),
                entryViewModel.message.first(),
            )
        }

    @Test
    fun `importEntriesCsv doesn't import any entries and returns an error message when the CSV file is empty`() =
        runTest {
            val entryRepository = FakeEntryRepository(emptyList())
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            val csv = ""
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockResources, inputStream).join()

            val resultEntries = entryRepository.fakeEntries.first()
            assertEquals(0, resultEntries.size)
            assertEquals(
                Message(
                    "Empty CSV file",
                    type = Message.Type.ERROR,
                    duration = Message.Duration.LONG,
                ),
                entryViewModel.message.first(),
            )
        }

    @Test
    fun `exportEntriesCsv writes all entries`() =
        runTest {
            val entryRepository = FakeEntryRepository(
                listOf(
                    Entry(
                        amount = 0.0,
                        amountUnit = noneUnit,
                        content = "Red apples",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            18,
                            0,
                            22,
                            755_000_000,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 1,
                        uuid = UUID.fromString("8be47977-3577-4534-993c-c14f2fccc8ef"),
                    ),
                    Entry(
                        amount = 2.0,
                        amountUnit = smallNumbersChoiceUnit,
                        content = "Yellow bananas",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            15,
                            18,
                            43,
                            189_000_000,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 2,
                        uuid = UUID.fromString("85f2ff1f-1424-40ac-b45e-e8381d84005b"),
                    ),
                    Entry(
                        amount = 3.0,
                        amountUnit = clothingSizeUnit,
                        content = "Green kiwis",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            15,
                            16,
                            56,
                            985_000_000,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 3,
                        uuid = UUID.fromString("98fb296e-29f3-4e6e-b7d2-646976cd2e0f"),
                    ),
                    Entry(
                        amount = 3.14,
                        amountUnit = doubleUnit,
                        content = "Purple grapes",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            15,
                            0,
                            0,
                            0,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 4,
                        uuid = UUID.fromString("eee93824-8533-455e-8622-0dc2a24ef584"),
                    ),
                    Entry(
                        amount = 0.333,
                        amountUnit = fractionUnit,
                        content = "Pineapple",
                        createdAt = OffsetDateTime.of(
                            2025,
                            2,
                            1,
                            7,
                            0,
                            0,
                            0,
                            ZoneOffset.of("+01:00")
                        ).toInstant().toEpochMilli(),
                        uid = 5,
                        uuid = UUID.fromString("7fa18ae8-191d-46d1-bd86-748e9014ef33"),
                    ),
                )
            )
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            val outputStream = ByteArrayOutputStream()

            entryViewModel.exportEntriesCsv(mockResources, outputStream, entryRepository.filterAsSequence()).join()

            assertEquals(
                @Suppress("SpellCheckingInspection")
                listOf(
                    "createdAt,content,amountFormatted,amount,amountUnit,uuid",
                    "2025-02-01T14:00:22.755-0300,Red apples,,0.0,NONE,8be47977-3577-4534-993c-c14f2fccc8ef",
                    "2025-02-01T11:18:43.189-0300,Yellow bananas,2x,2.0,SMALL_NUMBERS_CHOICE,85f2ff1f-1424-40ac-b45e-e8381d84005b",
                    "2025-02-01T11:16:56.985-0300,Green kiwis,L,3.0,CLOTHING_SIZE,98fb296e-29f3-4e6e-b7d2-646976cd2e0f",
                    "2025-02-01T11:00:00.000-0300,Purple grapes,3.14,3.14,DOUBLE,eee93824-8533-455e-8622-0dc2a24ef584",
                    "2025-02-01T03:00:00.000-0300,Pineapple,1/3,0.333,FRACTION,7fa18ae8-191d-46d1-bd86-748e9014ef33",
                    ""
                ).joinToString("\r\n"),
                outputStream.toString(),
            )
            assertEquals(
                Message(
                    "Exported 5 notes",
                    type = Message.Type.SUCCESS,
                    duration = Message.Duration.LONG,
                ),
                entryViewModel.message.first(),
            )
        }

    @Test
    fun `filterQueueSanitizedForFilename contains filter query with non-word characters replaced by underscores`() =
        runTest {
            val entryRepository = FakeEntryRepository(emptyList())
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            entryViewModel.filter("unicode 0žš中 emoji \uD83D\uDE42 slash / backslash \\ dash - underscore _ dot . colon :")
            assertEquals(
                "unicode 0___ emoji _ slash _ backslash _ dash - underscore _ dot _ colon _",
                entryViewModel.filterQuerySanitizedForFilename,
            )
        }
}
