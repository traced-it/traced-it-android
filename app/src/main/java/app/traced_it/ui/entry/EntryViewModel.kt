package app.traced_it.ui.entry

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.SnackbarDuration
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import app.traced_it.R
import app.traced_it.data.EntryRepository
import app.traced_it.data.local.database.Entry
import app.traced_it.data.local.database.units
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class Message(
    val text: String,
    val type: Type = Type.SUCCESS,
    val actionLabel: String? = null,
    val withDismissAction: Boolean = true,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onActionPerform: () -> Unit = {},
    val onDismiss: () -> Unit = {},
) {
    enum class Type { SUCCESS, ERROR }
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
        private const val FILTER_EXPANDED = "filerExpanded"
        private const val FILTER_QUERY = "filterQuery"
    }

    @Suppress("SpellCheckingInspection")
    private val csvDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

    private val _highlightedEntryUid = MutableStateFlow<Int?>(null)
    val highlightedEntryUid: StateFlow<Int?> = _highlightedEntryUid

    val filterExpanded: StateFlow<Boolean> =
        savedStateHandle.getStateFlow(FILTER_EXPANDED, false)

    val filterQuery: StateFlow<String> =
        savedStateHandle.getStateFlow(FILTER_QUERY, "")

    val filterQuerySanitizedForFilename: String
        get() = filterQuery.value.replace("""[^\w -]""".toRegex(), "_")

    val allEntriesCount: StateFlow<Int> =
        entryRepository.count()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredEntries: StateFlow<PagingData<Entry>> =
        filterQuery.flatMapLatest { filterQuery ->
            Pager(
                PagingConfig(pageSize = 20, enablePlaceholders = true)
            ) {
                entryRepository.filter(filterQuery)
            }.flow
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(), PagingData.empty()
        )

    val latestEntry: StateFlow<Entry?> =
        entryRepository.getLatest()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    fun insertEntry(context: Context, entry: Entry) {
        viewModelScope.launch {
            val newRowId = entryRepository.insert(entry)
            val newUid = newRowId.toInt()
            setHighlightedEntryUid(newUid)
            setMessage(
                Message(
                    context.resources.getString(R.string.list_message_added)
                )
            )
        }
    }

    fun updateEntry(context: Context, entry: Entry) {
        viewModelScope.launch {
            entryRepository.update(entry)
            setHighlightedEntryUid(entry.uid)
            setMessage(
                Message(
                    context.resources.getString(R.string.list_message_updated)
                )
            )
        }
    }

    fun deleteEntry(context: Context, entry: Entry) {
        viewModelScope.launch {
            entryRepository.delete(entry.uid)
            setMessage(
                Message(
                    text = context.resources.getString(
                        R.string.list_message_deleted
                    ),
                    type = Message.Type.ERROR,
                    actionLabel = context.resources.getString(
                        R.string.list_message_deleted_action
                    ),
                    duration = SnackbarDuration.Long,
                    withDismissAction = false,
                    onActionPerform = { restoreEntry(context, entry) },
                    onDismiss = { cleanupDeleted() },
                )
            )
        }
    }

    private fun restoreEntry(context: Context, entry: Entry) {
        viewModelScope.launch {
            entryRepository.restore(entry.uid)
            setHighlightedEntryUid(entry.uid)
            setMessage(
                Message(
                    context.resources.getString(R.string.list_message_restored)
                )
            )
        }
    }

    fun deleteAllEntries(context: Context) {
        viewModelScope.launch {
            entryRepository.deleteAll()
            setMessage(
                Message(
                    text = context.resources.getString(
                        R.string.list_message_all_deleted
                    ),
                    type = Message.Type.ERROR,
                    actionLabel = context.resources.getString(
                        R.string.list_message_all_deleted_action
                    ),
                    duration = SnackbarDuration.Long,
                    withDismissAction = false,
                    onActionPerform = { restoreAllEntries(context) },
                    onDismiss = { cleanupDeleted() },
                )
            )
        }
    }

    private fun restoreAllEntries(context: Context) {
        viewModelScope.launch {
            entryRepository.restoreAll()
            setMessage(
                Message(
                    context.resources.getString(R.string.list_message_all_restored)
                )
            )
        }
    }

    private fun cleanupDeleted() {
        viewModelScope.launch {
            entryRepository.cleanupDeleted()
        }
    }

    fun copyEntryToClipboard(
        context: Context,
        clipboard: Clipboard,
        entry: Entry,
    ) {
        viewModelScope.launch {
            clipboard.setClipEntry(
                ClipEntry(
                    ClipData.newPlainText(
                        "note", entry.format(context)
                    )
                )
            )
            val systemHasClipboardEditor =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            if (!systemHasClipboardEditor) {
                setMessage(
                    Message(
                        context.resources.getString(R.string.list_item_copied_to_clipboard)
                    )
                )
            }
        }
    }

    private fun setMessage(message: Message) {
        _message.value = message
    }

    fun performMessageAction() {
        val action = _message.value?.onActionPerform
        _message.value = null
        action?.invoke()
    }

    fun dismissMessage() {
        val action = _message.value?.onDismiss
        _message.value = null
        action?.invoke()
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

    private sealed class ParseResult {
        data class Succeeded(val entry: Entry) : ParseResult()
        data class Failed(val message: String) : ParseResult()
    }

    private fun parseEntryCsvRecord(
        context: Context,
        record: CSVRecord,
    ): ParseResult {
        val amountUnitRaw = try {
            record.get(COLUMN_AMOUNT_UNIT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_missing,
                    COLUMN_AMOUNT_UNIT,
                )
            )
        }
        if (amountUnitRaw.isNullOrEmpty()) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_empty,
                    COLUMN_AMOUNT_UNIT,
                )
            )
        }
        val amountUnit = units.find {
            it.id == amountUnitRaw
        } ?: units.find {
            context.resources.getString(it.nameResId) == amountUnitRaw
        }
        if (amountUnit == null) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_choice_unknown,
                    COLUMN_AMOUNT_UNIT,
                    units.joinToString(", ") { "\"${it.id}\"" },
                    amountUnitRaw,
                )
            )
        }

        val amountRaw = try {
            record.get(COLUMN_AMOUNT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_missing,
                    COLUMN_AMOUNT,
                )
            )
        }
        val amount = amountUnit.deserialize(amountRaw)

        val content = try {
            record.get(COLUMN_CONTENT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_missing,
                    COLUMN_CONTENT,
                )
            )
        }
        if (content.isNullOrEmpty()) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_empty,
                    COLUMN_CONTENT,
                )
            )
        }

        val createdAtRaw = try {
            record.get(COLUMN_CREATED_AT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_missing,
                    COLUMN_CREATED_AT,
                )
            )
        }
        if (createdAtRaw.isNullOrEmpty()) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_empty,
                    COLUMN_CREATED_AT,
                )
            )
        }
        val createdAtDate = try {
            csvDateFormat.parse(createdAtRaw)
        } catch (_: ParseException) {
            null
        }
        if (createdAtDate == null) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_parsing_error,
                    COLUMN_CREATED_AT,
                    createdAtRaw,
                )
            )
        }
        val createdAt = createdAtDate.time

        return ParseResult.Succeeded(
            Entry(
                amount = amount,
                amountUnit = amountUnit,
                content = content,
                createdAt = createdAt,
            )
        )
    }

    fun launchImportEntries(importLauncher: ActivityResultLauncher<Intent>) {
        importLauncher.launch(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            },
        )
    }

    fun importEntries(context: Context, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data.also { uri ->
                if (uri != null) {
                    viewModelScope.launch {
                        context.contentResolver.openInputStream(uri)
                            ?.use { inputStream ->
                                importEntriesCsv(context, inputStream)
                            }
                    }
                }
            }
        }
    }

    suspend fun importEntriesCsv(
        context: Context,
        inputStream: InputStream,
    ) {
        var importedCount = 0
        var skippedCount = 0
        var failedMessage: String? = null

        val records = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .build()
            .parse(inputStream.reader())
        for (record in records) {
            val parseResult = parseEntryCsvRecord(context, record)
            when (parseResult) {
                is ParseResult.Succeeded -> {
                    val existingEntry = entryRepository.getByCreatedAt(
                        parseResult.entry.createdAt
                    )
                    if (existingEntry == null) {
                        entryRepository.insert(parseResult.entry)
                        importedCount++
                    } else {
                        skippedCount++
                    }
                }

                is ParseResult.Failed -> {
                    failedMessage = parseResult.message
                    break
                }
            }
        }
        if (importedCount == 0 && skippedCount == 0 && failedMessage == null) {
            failedMessage = context.resources.getString(
                R.string.list_import_finished_empty
            )
        }

        val messageText = listOfNotNull(
            importedCount.takeIf { it != 0 }?.let {
                context.resources.getQuantityString(
                    R.plurals.list_import_finished_imported, it, it
                )
            },
            skippedCount.takeIf { it != 0 }?.let {
                context.resources.getQuantityString(
                    R.plurals.list_import_finished_skipped, it, it
                )
            },
            failedMessage,
        ).joinToString(
            context.resources.getString(R.string.list_import_finished_delimiter)
        )
        setMessage(
            Message(
                messageText,
                type = if (failedMessage == null)
                    Message.Type.SUCCESS
                else
                    Message.Type.ERROR,
                duration = SnackbarDuration.Long,
            )
        )
    }

    fun launchExportAllEntries(
        context: Context,
        exportLauncher: ActivityResultLauncher<Intent>,
    ) {
        launchExportEntries(
            exportLauncher,
            context.resources.getString(
                R.string.list_export_all_filename,
                context.resources.getString(R.string.app_name),
                Build.MODEL,
            ),
        )
    }

    fun launchExportFilteredEntries(
        context: Context,
        exportLauncher: ActivityResultLauncher<Intent>,
    ) {
        launchExportEntries(
            exportLauncher,
            context.resources.getString(
                R.string.list_export_filtered_filename,
                context.resources.getString(R.string.app_name),
                Build.MODEL,
                filterQuerySanitizedForFilename,
            ),
        )
    }

    private fun launchExportEntries(
        exportLauncher: ActivityResultLauncher<Intent>,
        filename: String,
    ) {
        exportLauncher.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/csv"
                putExtra(Intent.EXTRA_TITLE, filename)
            },
        )
    }

    fun exportAllEntries(context: Context, result: ActivityResult) =
        exportEntries(
            context, result, entryRepository.filter()
        )

    fun exportFilteredEntries(context: Context, result: ActivityResult) =
        exportEntries(
            context, result, entryRepository.filter(filterQuery.value)
        )

    private fun exportEntries(
        context: Context,
        result: ActivityResult,
        pagingSource: PagingSource<Int, Entry>,
    ) {
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data.also { uri ->
                if (uri != null) {
                    viewModelScope.launch {
                        // For the writing to work with the suspended
                        // function `exportEntriesCsv`, it's important to
                        // open the stream and close the writer inside the
                        // coroutine.
                        context.contentResolver.openOutputStream(uri)
                            ?.use { outputStream ->
                                val writer = outputStream.writer()
                                exportEntriesCsv(context, writer, pagingSource)
                                writer.close()
                            }
                    }
                }
            }
        }
    }

    suspend fun exportEntriesCsv(
        context: Context,
        writer: Appendable,
        pagingSource: PagingSource<Int, Entry>,
    ) {
        val printer = CSVFormat.DEFAULT
            .builder()
            .setHeader(
                COLUMN_CREATED_AT,
                COLUMN_CONTENT,
                COLUMN_AMOUNT_FORMATTED,
                COLUMN_AMOUNT,
                COLUMN_AMOUNT_UNIT,
            )
            .build()
            .print(writer)
        forEachEntry(pagingSource) { entry ->
            printer.printRecord(
                csvDateFormat.format(entry.createdAt),
                entry.content,
                entry.amountUnit.format(context, entry.amount),
                entry.amountUnit.serialize(entry.amount),
                entry.amountUnit.id,
            )
        }
    }
}
