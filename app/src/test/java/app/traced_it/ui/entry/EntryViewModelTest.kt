package app.traced_it.ui.entry

import android.content.Context
import android.content.res.Resources
import androidx.compose.material3.SnackbarDuration
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
                getString(
                    R.string.list_import_failed_column_parsing_error,
                    "createdAt",
                    "INVALID_DATE"
                )
            } doReturn "Failed to parse \"createdAt\" value \"INVALID_DATE\""
            on { getString(R.string.list_import_finished_delimiter) } doReturn " "
            on {
                getQuantityString(
                    R.plurals.list_import_finished_imported,
                    5,
                    5,
                )
            } doReturn "Imported 5 notes."
            on {
                getQuantityString(
                    R.plurals.list_import_finished_skipped,
                    1,
                    1,
                )
            } doReturn "Skipped 1 note."
        }
    }

    @Test
    fun `importEntriesCsv inserts valid entries, skips entries with the same createdAt, and aborts after failing to parse a row`() =
        runTest {
            val entryRepository = FakeEntryRepository(emptyList())
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            val mockContext = mock<Context> {
                on { resources } doReturn mockResources
            }
            val csv = """
                createdAt,content,amountFormatted,amount,amountUnit
                2025-02-01T18:00:22.755+0100,"Red apples",,0.0,NONE
                2025-02-01T18:00:22.755+0100,"Red apples duplicate",,0.0,NONE
                2025-02-01T15:18:43.189+0100,"Yellow bananas",2x,2.0,SMALL_NUMBERS_CHOICE
                2025-02-01T15:16:56.985+0100,"Green kiwis",L,3.0,CLOTHING_SIZE
                2025-02-01T15:00:00.000+0100,"Purple grapes",3.14,3.14,DOUBLE
                2025-02-01T07:00:00.000+0100,"Pineapple",1/3,0.333,FRACTION
                INVALID_DATE,"Green kiwis invalid",,0.0,NONE
                2025-02-01T01:59:38.771+0100,"Green kiwis not processed",,0.0,NONE
            """.trimIndent()
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockContext, inputStream)

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
            for ((expectedEntry, fakeEntry) in expectedEntries zip resultEntries) {
                assertEquals(expectedEntry, fakeEntry)
            }
            assertEquals(
                Message(
                    "Imported ${expectedEntries.size} notes. Skipped 1 note. Failed to parse \"createdAt\" value \"INVALID_DATE\"",
                    type = Message.Type.ERROR,
                    duration = SnackbarDuration.Long,
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
            val mockContext = mock<Context> {
                on { resources } doReturn mockResources
            }
            val csv = """
                createdAt,content,amount,amountUnit
                2025-02-01T18:00:22.755+0100,"Red apples",0.0,no unit
                2025-02-01T15:18:43.189+0100,"Yellow bananas",2.0,portion
                2025-02-01T15:16:56.985+0100,"Green kiwis",3.0,clothing
                2025-02-01T15:00:00.000+0100,"Purple grapes",3.14,number
                2025-02-01T07:00:00.000+0100,"Pineapple",0.333,fraction
            """.trimIndent()
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockContext, inputStream)

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
            for ((expectedEntry, fakeEntry) in expectedEntries zip resultEntries) {
                assertEquals(expectedEntry, fakeEntry)
            }
            assertEquals(
                Message(
                    "Imported ${expectedEntries.size} notes.",
                    type = Message.Type.SUCCESS,
                    duration = SnackbarDuration.Long,
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
            val mockResources = mock<Resources> {
                on { getString(R.string.list_import_finished_empty) } doReturn "Empty CSV file"
                on { getString(R.string.list_import_finished_delimiter) } doReturn " "
            }
            val mockContext = mock<Context> {
                on { resources } doReturn mockResources
            }
            val csv = ""
            val inputStream = ByteArrayInputStream(csv.toByteArray())

            entryViewModel.importEntriesCsv(mockContext, inputStream)

            val resultEntries = entryRepository.fakeEntries.first()
            assertEquals(0, resultEntries.size)
            assertEquals(
                Message(
                    "Empty CSV file",
                    type = Message.Type.ERROR,
                    duration = SnackbarDuration.Long,
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
            )
            val entryViewModel = EntryViewModel(
                entryRepository,
                SavedStateHandle(),
            )
            val mockContext = mock<Context> {
                on { resources } doReturn mockResources
            }
            val outputStream = ByteArrayOutputStream()
            val writer = outputStream.writer()

            entryViewModel.exportEntriesCsv(
                mockContext,
                writer,
                entryRepository.filter(),
            )
            writer.close()

            assertEquals(
                listOf(
                    "createdAt,content,amountFormatted,amount,amountUnit",
                    "2025-02-01T14:00:22.755-0300,Red apples,,0.0,NONE",
                    "2025-02-01T11:18:43.189-0300,Yellow bananas,2x,2.0,SMALL_NUMBERS_CHOICE",
                    "2025-02-01T11:16:56.985-0300,Green kiwis,L,3.0,CLOTHING_SIZE",
                    "2025-02-01T11:00:00.000-0300,Purple grapes,3.14,3.14,DOUBLE",
                    "2025-02-01T03:00:00.000-0300,Pineapple,1/3,0.333,FRACTION",
                    ""
                ).joinToString("\r\n"),
                outputStream.toString(),
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
