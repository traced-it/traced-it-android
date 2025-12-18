package app.traced_it.ui.entry

import android.content.res.Resources
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.traced_it.R
import app.traced_it.data.EntryRepository
import app.traced_it.data.local.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.OutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class Message(
    val text: String,
    val type: Type = Type.SUCCESS,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = true,
    val duration: Duration = Duration.SHORT,
    val onActionPerform: () -> Unit = {},
    val onDismiss: () -> Unit = {},
) {
    enum class Type { SUCCESS, ERROR }
    enum class Duration { SHORT, LONG, INDEFINITE }
}

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val entryRepository: EntryRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private companion object {
        private const val COLUMN_AMOUNT_FORMATTED = "amountFormatted"
        private const val COLUMN_AMOUNT_UNIT = "amountUnit"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_CREATED_AT = "createdAt"
        private const val COLUMN_UUID = "uuid"
        private const val FILTER_EXPANDED = "filerExpanded"
        private const val FILTER_QUERY = "filterQuery"
    }

    @Suppress("SpellCheckingInspection")
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _highlightedEntryUid = MutableStateFlow<Int?>(null)
    val highlightedEntryUid: StateFlow<Int?> = _highlightedEntryUid

    val filterExpanded: StateFlow<Boolean> = savedStateHandle.getStateFlow(FILTER_EXPANDED, false)

    val filterQuery: StateFlow<String> = savedStateHandle.getStateFlow(FILTER_QUERY, "")

    val filterQuerySanitizedForFilename: String
        get() = filterQuery.value.replace("""[^\w -]""".toRegex(), "_")

    val allEntriesCount: StateFlow<Int> = entryRepository.count()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredEntries: StateFlow<PagingData<Entry>> =
        filterQuery.flatMapLatest { filterQuery ->
            Pager(PagingConfig(pageSize = 20, enablePlaceholders = true)) {
                entryRepository.filter(filterQuery)
            }.flow
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), PagingData.empty())

    val latestEntryUnit: StateFlow<EntryUnit?> = entryRepository.getLatest().map { it?.amountUnit }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun insertEntry(resources: Resources, entry: Entry) {
        viewModelScope.launch {
            val newRowId = entryRepository.insert(entry)
            val newUid = newRowId.toInt()
            setHighlightedEntryUid(newUid)
            setMessage(Message(resources.getString(R.string.list_message_added)))
        }
    }

    fun updateEntry(resources: Resources, entry: Entry) {
        viewModelScope.launch {
            entryRepository.update(entry)
            setHighlightedEntryUid(entry.uid)
            setMessage(Message(resources.getString(R.string.list_message_updated)))
        }
    }

    fun deleteEntry(resources: Resources, entry: Entry) {
        viewModelScope.launch {
            entryRepository.delete(entry.uid)
            setMessage(
                Message(
                    text = resources.getString(R.string.list_message_deleted),
                    type = Message.Type.ERROR,
                    actionLabel = resources.getString(R.string.list_message_deleted_action),
                    duration = Message.Duration.LONG,
                    withDismissAction = false,
                    onActionPerform = { restoreEntry(resources, entry) },
                    onDismiss = { cleanupDeleted() },
                )
            )
        }
    }

    private fun restoreEntry(resources: Resources, entry: Entry) {
        viewModelScope.launch {
            entryRepository.restore(entry.uid)
            setHighlightedEntryUid(entry.uid)
            setMessage(Message(resources.getString(R.string.list_message_restored)))
        }
    }

    fun deleteAllEntries(resources: Resources) {
        viewModelScope.launch {
            entryRepository.deleteAll()
            setMessage(
                Message(
                    text = resources.getString(R.string.list_message_all_deleted),
                    type = Message.Type.ERROR,
                    actionLabel = resources.getString(R.string.list_message_all_deleted_action),
                    duration = Message.Duration.LONG,
                    withDismissAction = false,
                    onActionPerform = { restoreAllEntries(resources) },
                    onDismiss = { cleanupDeleted() },
                )
            )
        }
    }

    private fun restoreAllEntries(resources: Resources) {
        viewModelScope.launch {
            entryRepository.restoreAll()
            setMessage(Message(resources.getString(R.string.list_message_all_restored)))
        }
    }

    private fun cleanupDeleted() {
        viewModelScope.launch {
            entryRepository.cleanupDeleted()
        }
    }

    private fun setMessage(message: Message) {
        _message.value = message
    }

    fun performMessageAction() {
        _message.value?.let { message ->
            _message.value = null
            message.onActionPerform()
        }
    }

    fun dismissMessage() {
        _message.value?.let { message ->
            _message.value = null
            message.onDismiss()
        }
    }

    fun setHighlightedEntryUid(uid: Int?) {
        _highlightedEntryUid.value = uid
    }

    fun filter(filterQuery: String) {
        savedStateHandle[FILTER_QUERY] = filterQuery
    }

    fun setFilterExpanded(filterExpanded: Boolean) {
        savedStateHandle[FILTER_EXPANDED] = filterExpanded
    }

    private sealed interface ParseResult {
        data class Succeeded(val entry: Entry) : ParseResult
        data class Failed(val message: String) : ParseResult
    }

    private fun parseEntryCsvRecord(
        resources: Resources,
        record: CSVRecord,
        unitsById: Map<String, EntryUnit>,
        unitsByName: Map<String, EntryUnit>,
    ): ParseResult {
        val amountUnitRaw = try {
            record.get(COLUMN_AMOUNT_UNIT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_missing, COLUMN_AMOUNT_UNIT),
            )
        }
        if (amountUnitRaw.isNullOrEmpty()) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_empty, COLUMN_AMOUNT_UNIT),
            )
        }
        val amountUnit = unitsById[amountUnitRaw] ?: unitsByName[amountUnitRaw]
        if (amountUnit == null) {
            return ParseResult.Failed(
                resources.getString(
                    R.string.list_import_failed_column_choice_unknown,
                    COLUMN_AMOUNT_UNIT,
                    units.joinToString(", ") { "\"${it.id}\"" },
                    amountUnitRaw,
                ),
            )
        }

        val amountRaw = try {
            record.get(COLUMN_AMOUNT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_missing, COLUMN_AMOUNT),
            )
        }
        val amount = amountUnit.deserialize(amountRaw)

        val content = try {
            record.get(COLUMN_CONTENT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_missing, COLUMN_CONTENT),
            )
        }
        if (content.isNullOrEmpty()) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_empty, COLUMN_CONTENT),
            )
        }

        val createdAtRaw = try {
            record.get(COLUMN_CREATED_AT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_missing, COLUMN_CREATED_AT),
            )
        }
        if (createdAtRaw.isNullOrEmpty()) {
            return ParseResult.Failed(
                resources.getString(R.string.list_import_failed_column_empty, COLUMN_CREATED_AT),
            )
        }
        val createdAtDate = try {
            csvDateFormat.parse(createdAtRaw)
        } catch (_: ParseException) {
            null
        }
        if (createdAtDate == null) {
            return ParseResult.Failed(
                resources.getString(
                    R.string.list_import_failed_column_parsing_error, COLUMN_CREATED_AT, createdAtRaw
                ),
            )
        }
        val createdAt = createdAtDate.time

        val uuidRaw = try {
            record.get(COLUMN_UUID)
        } catch (_: IllegalArgumentException) {
            null
        }
        val uuid = if (uuidRaw != null) {
            try {
                UUID.fromString(uuidRaw)
            } catch (_: IllegalArgumentException) {
                return ParseResult.Failed(
                    resources.getString(
                        R.string.list_import_failed_column_parsing_error, COLUMN_UUID, uuidRaw
                    ),
                )
            }
        } else {
            null
        }

        val entry = Entry(
            amount = amount,
            amountUnit = amountUnit,
            content = content,
            createdAt = createdAt,
            uuid = uuid ?: UUID.randomUUID(),
        )
        return ParseResult.Succeeded(entry)
    }

    fun importEntriesCsv(resources: Resources, inputStream: InputStream) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val unitsById = units.associateById()
                val unitsByName = units.associateByName(resources)

                var importedCount = 0
                var skippedCount = 0
                var updatedCount = 0
                var failedMessage: String? = null

                setMessage(
                    Message(
                        resources.getString(R.string.list_import_in_progress),
                        Message.Type.SUCCESS,
                        withDismissAction = false,
                        duration = Message.Duration.INDEFINITE,
                    )
                )

                inputStream.use { inputStream ->
                    inputStream.reader().use { reader ->
                        val records = CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .get()
                            .parse(reader)
                        val hasUuidColumn = records.headerNames.contains(COLUMN_UUID)
                        for (record in records) {
                            when (val parseResult = parseEntryCsvRecord(resources, record, unitsById, unitsByName)) {
                                is ParseResult.Succeeded -> {
                                    // TODO Speed up import by doing batch import using SQL annotation `@Insert(onConflict = OnConflictStrategy.REPLACE)`
                                    if (hasUuidColumn) {
                                        val existingEntry = entryRepository.getByUuid(parseResult.entry.uuid)
                                        if (existingEntry == null) {
                                            entryRepository.insert(parseResult.entry)
                                            importedCount++
                                        } else {
                                            entryRepository.update(existingEntry.copy(
                                                amount = parseResult.entry.amount,
                                                amountUnit = parseResult.entry.amountUnit,
                                                content = parseResult.entry.content,
                                                createdAt = parseResult.entry.createdAt,
                                                deleted = false,
                                            ))
                                            updatedCount++
                                        }
                                    } else {
                                        val existingEntry = entryRepository.getByCreatedAt(parseResult.entry.createdAt)
                                        if (existingEntry == null) {
                                            entryRepository.insert(parseResult.entry)
                                            importedCount++
                                        } else {
                                            entryRepository.update(existingEntry.copy(deleted = false))
                                            skippedCount++
                                        }
                                    }
                                }

                                is ParseResult.Failed -> {
                                    failedMessage = parseResult.message
                                    break
                                }
                            }
                        }
                    }
                }

                if (importedCount == 0 && skippedCount == 0 && updatedCount == 0 && failedMessage == null) {
                    failedMessage = resources.getString(R.string.list_import_finished_empty)
                }

                val messageText = listOfNotNull(
                    importedCount.takeIf { it != 0 }?.let {
                        resources.getQuantityString(R.plurals.list_import_finished_imported, it, it)
                    },
                    updatedCount.takeIf { it != 0 }?.let {
                        resources.getQuantityString(R.plurals.list_import_finished_updated, it, it)
                    },
                    skippedCount.takeIf { it != 0 }?.let {
                        resources.getQuantityString(R.plurals.list_import_finished_skipped, it, it)
                    },
                    failedMessage,
                ).joinToString(resources.getString(R.string.list_import_finished_delimiter))

                setMessage(
                    Message(
                        messageText,
                        type = if (failedMessage == null) {
                            Message.Type.SUCCESS
                        } else {
                            Message.Type.ERROR
                        },
                        duration = Message.Duration.LONG,
                    )
                )
            } catch (e: Exception) {
                Log.e(null, "Unknown error when importing entries", e)
                setMessage(
                    Message(
                        resources.getString(R.string.list_import_failed_unknown),
                        Message.Type.ERROR,
                        duration = Message.Duration.LONG,
                    )
                )
            }
        }

    fun exportAllEntriesCsv(resources: Resources, outputStream: OutputStream) =
        exportEntriesCsv(resources, outputStream, entryRepository.filterAsSequence())

    fun exportFilteredEntriesCsv(resources: Resources, outputStream: OutputStream) =
        exportEntriesCsv(resources, outputStream, entryRepository.filterAsSequence(filterQuery.value))

    fun exportEntriesCsv(resources: Resources, outputStream: OutputStream, entries: Sequence<Entry>) =
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var exportedCount = 0

                setMessage(
                    Message(
                        resources.getString(R.string.list_export_in_progress),
                        Message.Type.SUCCESS,
                        withDismissAction = false,
                        duration = Message.Duration.INDEFINITE,
                    )
                )

                outputStream.use { outputStream ->
                    outputStream.writer().use { writer ->
                        val printer = CSVFormat.DEFAULT
                            .builder()
                            .setHeader(
                                COLUMN_CREATED_AT,
                                COLUMN_CONTENT,
                                COLUMN_AMOUNT_FORMATTED,
                                COLUMN_AMOUNT,
                                COLUMN_AMOUNT_UNIT,
                                COLUMN_UUID,
                            )
                            .get()
                            .print(writer)
                        for (entry in entries) {
                            printer.printRecord(
                                csvDateFormat.format(entry.createdAt),
                                entry.content,
                                entry.amountUnit.format(resources, entry.amount),
                                entry.amountUnit.serialize(entry.amount),
                                entry.amountUnit.id,
                                entry.uuid.toString(),
                            )
                            exportedCount++
                        }
                    }
                }

                setMessage(
                    Message(
                        resources.getQuantityString(R.plurals.list_export_finished, exportedCount, exportedCount),
                        Message.Type.SUCCESS,
                        duration = Message.Duration.LONG,
                    )
                )
            } catch (e: Exception) {
                Log.e(null, "Unknown error when exporting entries", e)
                setMessage(
                    Message(
                        resources.getString(R.string.list_export_failed_unknown),
                        Message.Type.ERROR,
                        duration = Message.Duration.LONG,
                    )
                )
            }
        }
}
