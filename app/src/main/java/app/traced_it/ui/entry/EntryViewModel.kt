package app.traced_it.ui.entry

import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import app.traced_it.R
import app.traced_it.data.EntryRepository
import app.traced_it.data.local.database.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStreamReader
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
        private const val COLUMN_UID = "uid"
        private const val FILTER_EXPANDED = "filerExpanded"
        private const val FILTER_QUERY = "filterQuery"
    }

    @Suppress("SpellCheckingInspection")
    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _highlightedEntryUid = MutableStateFlow<UUID?>(null)
    val highlightedEntryUid: StateFlow<UUID?> = _highlightedEntryUid

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
            entryRepository.insert(entry)
            setHighlightedEntryUid(entry.uid)
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

    fun setHighlightedEntryUid(uid: UUID?) {
        _highlightedEntryUid.value = uid
    }

    fun filter(filterQuery: String) {
        savedStateHandle[FILTER_QUERY] = filterQuery
    }

    fun setFilterExpanded(filterExpanded: Boolean) {
        savedStateHandle[FILTER_EXPANDED] = filterExpanded
    }

    private suspend fun forEachEntry(
        pagingSource: PagingSource<Int, Entry>,
        pageSize: Int = 100,
        callback: (Entry) -> Unit,
    ) {
        var loadResult: PagingSource.LoadResult<Int, Entry>
        var i = 0
        var key: Int? = 0
        while (key != null) {
            loadResult = pagingSource.load(
                PagingSource.LoadParams.Refresh(key, pageSize, false)
            )
            if (loadResult !is PagingSource.LoadResult.Page<Int, Entry>) {
                break
            }
            for (entry in loadResult) {
                callback(entry)
                i++
            }
            key = loadResult.nextKey
        }
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

        val uidRaw = try {
            record.get(COLUMN_UID)
        } catch (_: IllegalArgumentException) {
            null
        }
        val uid = if (uidRaw != null) {
            try {
                UUID.fromString(uidRaw)
            } catch (_: IllegalArgumentException) {
                return ParseResult.Failed(
                    resources.getString(
                        R.string.list_import_failed_column_parsing_error, COLUMN_UID, uidRaw
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
            uid = uid ?: UUID.randomUUID(),
        )
        return ParseResult.Succeeded(entry)
    }

    suspend fun importEntriesCsv(
        resources: Resources,
        reader: InputStreamReader,
    ) = withContext(Dispatchers.IO) {
        val unitsById = units.associateById()
        val unitsByName = units.associateByName(resources)

        var importedCount = 0
        var skippedCount = 0
        var updatedCount = 0
        var failedMessage: String? = null

        setMessage(
            Message(
                resources.getString(R.string.list_import_started),
                Message.Type.SUCCESS,
                duration = Message.Duration.INDEFINITE,
            )
        )
        val records = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get()
            .parse(reader)
        val hasUidColumn = records.headerNames.contains(COLUMN_UID)
        for (record in records) {
            when (val parseResult = parseEntryCsvRecord(resources, record, unitsById, unitsByName)) {
                is ParseResult.Succeeded -> {
                    if (hasUidColumn) {
                        val existingEntry = entryRepository.getByUid(parseResult.entry.uid)
                        if (existingEntry == null) {
                            entryRepository.insert(parseResult.entry)
                            importedCount++
                        } else {
                            entryRepository.update(parseResult.entry)
                            updatedCount++
                        }
                    } else {
                        val existingEntry = entryRepository.getByCreatedAt(parseResult.entry.createdAt)
                        if (existingEntry == null) {
                            entryRepository.insert(parseResult.entry)
                            importedCount++
                        } else {
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
    }

    suspend fun exportAllEntriesCsv(resources: Resources, writer: Appendable) =
        exportEntriesCsv(resources, writer, entryRepository.filter())

    suspend fun exportFilteredEntriesCsv(resources: Resources, writer: Appendable) =
        exportEntriesCsv(resources, writer, entryRepository.filter(filterQuery.value))

    suspend fun exportEntriesCsv(resources: Resources, writer: Appendable, pagingSource: PagingSource<Int, Entry>) =
        withContext(Dispatchers.IO) {
            val printer = CSVFormat.DEFAULT
                .builder()
                .setHeader(
                    COLUMN_CREATED_AT,
                    COLUMN_CONTENT,
                    COLUMN_AMOUNT_FORMATTED,
                    COLUMN_AMOUNT,
                    COLUMN_AMOUNT_UNIT,
                    COLUMN_UID,
                )
                .get()
                .print(writer)
            forEachEntry(pagingSource) { entry ->
                printer.printRecord(
                    csvDateFormat.format(entry.createdAt),
                    entry.content,
                    entry.amountUnit.format(resources, entry.amount),
                    entry.amountUnit.serialize(entry.amount),
                    entry.amountUnit.id,
                    entry.uid.toString(),
                )
            }
        }
}
