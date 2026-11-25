package app.traced_it.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.snapshots.Snapshot.Companion.withMutableSnapshot
import androidx.core.content.getSystemService
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.traced_it.R
import app.traced_it.data.EntryRepository
import app.traced_it.data.UserPreferenceRepository
import app.traced_it.data.database.ActiveEntryUnit
import app.traced_it.data.database.Entry
import app.traced_it.data.database.EntryDraft
import app.traced_it.data.database.EntryUnit
import app.traced_it.data.database.units
import app.traced_it.data.extensions.hasClipboardEditor
import app.traced_it.data.extensions.openInputStreamOrNull
import app.traced_it.data.extensions.openOutputStreamOrNull
import app.traced_it.data.preferences.DefaultUnitPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import java.io.InputStream
import java.io.OutputStream
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
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

sealed interface NavigationState {
    data object Loading : NavigationState

    /**
     * The optional [lastDraft] is displayed during the exit animation from detail to list, because at that moment
     * the draft might have already been copied into an entry and deleted.
     */
    data class List(val lastDraft: EntryDraft? = null) : NavigationState

    data class Detail(val initialDraft: EntryDraft) : NavigationState
}

sealed interface ScrollTarget {
    data object Top : ScrollTarget
    data class Item(val index: Int, val uid: Int) : ScrollTarget
}

sealed interface ImportExportState {
    data object ImportRequested : ImportExportState
    data class ExportAllRequested(val filename: String) : ImportExportState
    data class ExportFilteredRequested(val filename: String) : ImportExportState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entryRepository: EntryRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Filter

    private val _query = savedStateHandle.getMutableStateFlow<String?>("query", null)
    val query: StateFlow<String?> = _query.asStateFlow()

    private val _scrollTarget = MutableStateFlow<ScrollTarget?>(null)
    val scrollTarget: StateFlow<ScrollTarget?> = _scrollTarget.asStateFlow()

    fun filter(query: String?) {
        _query.value = query
        _scrollTarget.value = ScrollTarget.Top
    }

    fun finishScrollToTarget() {
        _scrollTarget.value = null
    }

    // List

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    val totalCount: StateFlow<Int> =
        entryRepository.count()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<PagingData<Entry>> =
        _query
            .flatMapLatest { query ->
                Pager(PagingConfig(pageSize = 20, enablePlaceholders = true, jumpThreshold = 3 * 20)) {
                    entryRepository.filter(query.orEmpty())
                }.flow
            }
            .cachedIn(viewModelScope)

    private val _highlightedUid = MutableStateFlow<Int?>(null)
    val highlightedUid: StateFlow<Int?> = _highlightedUid.asStateFlow()

    fun finishHighlight() {
        _highlightedUid.value = null
    }

    // Navigation

    /**
     * Automatically navigate to the detail screen when there is a draft, so the app forces the user to finish or
     * dismiss any existing draft before viewing the list.
     */
    fun initialize() {
        viewModelScope.launch {
            val draft = entryRepository.getFirstDraft()
            withMutableSnapshot {
                _navigationState.value =
                    if (draft == null) {
                        NavigationState.List()
                    } else {
                        NavigationState.Detail(draft)
                    }
            }
        }
    }

    fun navigateToInsert() {
        viewModelScope.launch {
            val draft = entryRepository.createDraftToInsert()
            navigateToDetail(draft)
        }
    }

    fun navigateToRepeat(uid: Int) {
        viewModelScope.launch {
            val entry = entryRepository.getByUid(uid)
            if (entry != null) {
                val draft = entryRepository.createDraftToRepeat(entry)
                navigateToDetail(draft)
            }
        }
    }

    fun navigateToUpdate(uid: Int) {
        viewModelScope.launch {
            val entry = entryRepository.getByUid(uid)
            if (entry != null) {
                val draft = entryRepository.createDraftToUpdate(entry)
                navigateToDetail(draft)
            }
        }
    }

    private fun navigateToDetail(draft: EntryDraft) {
        viewModelScope.launch {
            withMutableSnapshot {
                _navigationState.value = NavigationState.Detail(draft)
            }
            val rowId = entryRepository.insertDraft(draft)
            val newUid = rowId.toInt()
            withMutableSnapshot {
                _navigationState.value = NavigationState.Detail(draft.copy(uid = newUid))
            }
        }
    }

    // Methods

    fun dismissDraft(draft: EntryDraft) {
        _navigationState.value = NavigationState.List(draft)
        viewModelScope.launch {
            entryRepository.deleteDraft(draft)
        }
    }

    fun finishDraft(draft: EntryDraft) {
        _navigationState.value = NavigationState.List(draft)
        viewModelScope.launch {
            val (index, uid) = entryRepository.upsertEntryFromDraft(draft) ?: return@launch
            withMutableSnapshot {
                _scrollTarget.value = ScrollTarget.Item(index = index, uid = uid)
                _highlightedUid.value = uid
                _message.value = Message(
                    text = if (draft.entryUid == -1) {
                        context.resources.getString(R.string.list_message_added)
                    } else {
                        context.resources.getString(R.string.list_message_updated)
                    }
                )
            }
        }
    }

    fun upsertDraft(draft: EntryDraft) {
        viewModelScope.launch {
            entryRepository.upsertDraft(draft)
        }
    }

    fun delete(uid: Int) {
        viewModelScope.launch {
            entryRepository.delete(uid)
            withMutableSnapshot {
                _message.value = Message(
                    text = context.resources.getString(R.string.list_message_deleted),
                    type = Message.Type.ERROR,
                    actionLabel = context.resources.getString(R.string.list_message_deleted_action),
                    duration = Message.Duration.LONG,
                    withDismissAction = false,
                    onActionPerform = { restore(uid) },
                    onDismiss = { cleanupDeleted() },
                )
            }
        }
    }

    private fun restore(uid: Int) {
        viewModelScope.launch {
            val index = entryRepository.restoreAndGetIndex(uid)
            withMutableSnapshot {
                _scrollTarget.value = ScrollTarget.Item(index = index, uid = uid)
                _highlightedUid.value = uid
                _message.value = Message(context.resources.getString(R.string.list_message_restored))
            }
        }
    }

    fun deleteAll() {
        viewModelScope.launch {
            entryRepository.deleteAll()
            withMutableSnapshot {
                _message.value = Message(
                    text = context.resources.getString(R.string.list_message_all_deleted),
                    type = Message.Type.ERROR,
                    actionLabel = context.resources.getString(R.string.list_message_all_deleted_action),
                    duration = Message.Duration.LONG,
                    withDismissAction = false,
                    onActionPerform = { restoreAll() },
                    onDismiss = { cleanupDeleted() },
                )
            }
        }
    }

    private fun restoreAll() {
        viewModelScope.launch {
            entryRepository.restoreAll()
            withMutableSnapshot {
                _message.value = Message(context.resources.getString(R.string.list_message_all_restored))
            }
        }
    }

    private fun cleanupDeleted() {
        viewModelScope.launch {
            entryRepository.cleanupDeleted()
        }
    }

    fun copyToClipboard(uid: Int) {
        viewModelScope.launch {
            val entry = entryRepository.getByUid(uid)
            if (entry != null) {
                val text = entry.formatContentWithAmountAndCreatedAt(
                    context.resources,
                    Locale.getDefault(),
                    TimeZone.currentSystemDefault(),
                )
                val clipboardManager = context.getSystemService<ClipboardManager>()
                if (clipboardManager != null) {
                    @Suppress("UsePropertyAccessSyntax")
                    clipboardManager.setPrimaryClip(ClipData.newPlainText(null, text))
                    if (!hasClipboardEditor()) {
                        withMutableSnapshot {
                            _message.value = Message(
                                context.resources.getString(R.string.list_item_copied_to_clipboard)
                            )
                        }
                    }
                }
            }
        }
    }

    fun filterSimilar(uid: Int) {
        viewModelScope.launch {
            val entry = entryRepository.getByUid(uid)
            if (entry != null) {
                withMutableSnapshot {
                    filter(entry.content)
                }
            }
        }
    }

    // Message

    private val _message = MutableStateFlow<Message?>(null)
    val message: StateFlow<Message?> = _message

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

    // Preferences

    val defaultUnit: StateFlow<ActiveEntryUnit> =
        userPreferenceRepository.values
            .map { it.defaultUnit }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DefaultUnitPreference.default,
            )

    fun setDefaultUnit(defaultUnit: ActiveEntryUnit) {
        viewModelScope.launch {
            userPreferenceRepository.setDefaultUnit(defaultUnit)
        }
    }

    // Import and export

    private sealed interface ParseResult {
        data class Succeeded(val entry: Entry) : ParseResult
        data class Failed(val message: String) : ParseResult
    }

    private val _importExportState: MutableStateFlow<ImportExportState?> = MutableStateFlow(null)
    val importExportState: StateFlow<ImportExportState?> = _importExportState

    private val csvDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
    }
    private val oldCsvDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    }

    fun requestImport() {
        _importExportState.value = ImportExportState.ImportRequested
    }

    fun requestExportAll() {
        val filename = context.resources.getString(
            R.string.list_export_all_filename,
            context.resources.getString(R.string.app_name),
            Build.MODEL,
        )
        _importExportState.value = ImportExportState.ExportAllRequested(filename)
    }

    fun requestExportFiltered() {
        val query = _query.value ?: return
        val sanitizedQuery = query.replace("""[^\w -]""".toRegex(), "_")
        val filename = context.resources.getString(
            R.string.list_export_filtered_filename,
            context.resources.getString(R.string.app_name),
            Build.MODEL,
            sanitizedQuery,
        )
        _importExportState.value = ImportExportState.ExportFilteredRequested(filename)
    }

    fun receiveImportExportUri(uri: Uri) {
        try {
            when (_importExportState.value) {
                is ImportExportState.ImportRequested -> {
                    context.openInputStreamOrNull(uri)?.let { inputStream ->
                        importEntriesCsv(inputStream)
                    }
                }

                is ImportExportState.ExportAllRequested -> {
                    context.openOutputStreamOrNull(uri)?.let { outputStream ->
                        exportAllEntriesCsv(outputStream)
                    }
                }

                is ImportExportState.ExportFilteredRequested -> {
                    context.openOutputStreamOrNull(uri)?.let { outputStream ->
                        exportFilteredEntriesCsv(outputStream)
                    }
                }

                null -> {}
            }
        } finally {
            finishImportExport()
        }
    }

    fun finishImportExport() {
        _importExportState.value = null
    }

    private fun parseEntryCsvRecord(
        record: CSVRecord,
        unitsById: Map<String, EntryUnit>,
        unitsByName: Map<String, EntryUnit>,
    ): ParseResult {
        val amountUnitRaw = try {
            record.get(Columns.AMOUNT_UNIT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_missing, Columns.AMOUNT_UNIT),
            )
        }
        if (amountUnitRaw.isNullOrEmpty()) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_empty, Columns.AMOUNT_UNIT),
            )
        }
        val amountUnit = unitsById[amountUnitRaw] ?: unitsByName[amountUnitRaw]
        if (amountUnit == null) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_choice_unknown,
                    Columns.AMOUNT_UNIT,
                    units.joinToString(", ") { "\"${it.id}\"" },
                    amountUnitRaw,
                ),
            )
        }

        val amountRaw = try {
            record.get(Columns.AMOUNT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_missing, Columns.AMOUNT),
            )
        }
        val amount = amountUnit.deserialize(amountRaw)

        val content = try {
            record.get(Columns.CONTENT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_missing, Columns.CONTENT),
            )
        }
        if (content.isNullOrEmpty()) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_empty, Columns.CONTENT),
            )
        }

        val createdAtRaw = try {
            record.get(Columns.CREATED_AT)
        } catch (_: IllegalArgumentException) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_missing, Columns.CREATED_AT),
            )
        }
        if (createdAtRaw.isNullOrEmpty()) {
            return ParseResult.Failed(
                context.resources.getString(R.string.list_import_failed_column_empty, Columns.CREATED_AT),
            )
        }
        val createdAtDate = try {
            csvDateFormat.parse(createdAtRaw)
        } catch (_: ParseException) {
            null
        } ?: try {
            oldCsvDateFormat.parse(createdAtRaw)
        } catch (_: ParseException) {
            null
        }
        if (createdAtDate == null) {
            return ParseResult.Failed(
                context.resources.getString(
                    R.string.list_import_failed_column_parsing_error, Columns.CREATED_AT, createdAtRaw
                ),
            )
        }
        val createdAt = createdAtDate.time

        val uuidRaw = try {
            record.get(Columns.UUID)
        } catch (_: IllegalArgumentException) {
            null
        }
        val uuid = if (uuidRaw != null) {
            try {
                UUID.fromString(uuidRaw)
            } catch (_: IllegalArgumentException) {
                return ParseResult.Failed(
                    context.resources.getString(
                        R.string.list_import_failed_column_parsing_error, Columns.UUID, uuidRaw
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

    fun importEntriesCsv(inputStream: InputStream, dispatcher: CoroutineDispatcher = Dispatchers.IO): Job =
        viewModelScope.launch(dispatcher) {
            try {
                val unitsById = units.associateBy { it.id }
                val unitsByName = units.associateBy { it.getName(context.resources) }

                var importedCount = 0
                var skippedCount = 0
                var updatedCount = 0
                var failedMessage: String? = null

                _message.value = Message(
                    context.resources.getString(R.string.list_import_in_progress),
                    Message.Type.SUCCESS,
                    withDismissAction = false,
                    duration = Message.Duration.INDEFINITE,
                )

                inputStream.use { inputStream ->
                    inputStream.reader().use { reader ->
                        val records = CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .build()
                            .parse(reader)
                        val hasUuidColumn = records.headerNames.contains(Columns.UUID)
                        for (record in records) {
                            when (val parseResult = parseEntryCsvRecord(record, unitsById, unitsByName)) {
                                is ParseResult.Succeeded -> {
                                    // TODO Speed up import by doing batch import using SQL annotation `@Insert(onConflict = OnConflictStrategy.REPLACE)`
                                    if (hasUuidColumn) {
                                        val existingEntry = entryRepository.getByUuid(parseResult.entry.uuid)
                                        if (existingEntry == null) {
                                            entryRepository.insert(parseResult.entry)
                                            importedCount++
                                        } else {
                                            entryRepository.update(
                                                existingEntry.copy(
                                                    amount = parseResult.entry.amount,
                                                    amountUnit = parseResult.entry.amountUnit,
                                                    content = parseResult.entry.content,
                                                    createdAt = parseResult.entry.createdAt,
                                                    deleted = false,
                                                )
                                            )
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
                    failedMessage = context.resources.getString(R.string.list_import_finished_empty)
                }

                val messageText = listOfNotNull(
                    importedCount.takeIf { it != 0 }?.let {
                        context.resources.getQuantityString(R.plurals.list_import_finished_imported, it, it)
                    },
                    updatedCount.takeIf { it != 0 }?.let {
                        context.resources.getQuantityString(R.plurals.list_import_finished_updated, it, it)
                    },
                    skippedCount.takeIf { it != 0 }?.let {
                        context.resources.getQuantityString(R.plurals.list_import_finished_skipped, it, it)
                    },
                    failedMessage,
                ).joinToString(context.resources.getString(R.string.list_import_finished_delimiter))

                _message.value = Message(
                    messageText,
                    type = if (failedMessage == null) {
                        Message.Type.SUCCESS
                    } else {
                        Message.Type.ERROR
                    },
                    duration = Message.Duration.LONG,
                )
            } catch (e: Exception) {
                Log.e(null, "Unknown error when importing entries", e)
                _message.value = Message(
                    context.resources.getString(R.string.list_import_failed_unknown),
                    Message.Type.ERROR,
                    duration = Message.Duration.LONG,
                )
            }
        }

    fun exportAllEntriesCsv(outputStream: OutputStream) =
        exportEntriesCsv(outputStream, entryRepository.filterAsSequence())

    fun exportFilteredEntriesCsv(outputStream: OutputStream) =
        exportEntriesCsv(outputStream, entryRepository.filterAsSequence(_query.value.orEmpty()))

    fun exportEntriesCsv(
        outputStream: OutputStream,
        entries: Sequence<Entry>,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): Job =
        viewModelScope.launch(dispatcher) {
            try {
                var exportedCount = 0

                _message.value = Message(
                    context.resources.getString(R.string.list_export_in_progress),
                    Message.Type.SUCCESS,
                    withDismissAction = false,
                    duration = Message.Duration.INDEFINITE,
                )

                outputStream.use { outputStream ->
                    outputStream.writer().use { writer ->
                        val printer = CSVFormat.DEFAULT
                            .builder()
                            .setHeader(
                                Columns.CREATED_AT,
                                Columns.CONTENT,
                                Columns.AMOUNT_FORMATTED,
                                Columns.AMOUNT,
                                Columns.AMOUNT_UNIT,
                                Columns.UUID,
                            )
                            .build()
                            .print(writer)
                        for (entry in entries) {
                            with(entry) {
                                printer.printRecord(
                                    csvDateFormat.format(createdAt),
                                    content,
                                    amountUnit.format(context.resources, amount),
                                    amountUnit.serialize(amount),
                                    amountUnit.id,
                                    uuid.toString(),
                                )
                            }
                            exportedCount++
                        }
                    }
                }

                _message.value = Message(
                    context.resources.getQuantityString(
                        R.plurals.list_export_finished,
                        exportedCount,
                        exportedCount,
                    ),
                    Message.Type.SUCCESS,
                    duration = Message.Duration.LONG,
                )
            } catch (e: Exception) {
                Log.e(null, "Unknown error when exporting entries", e)
                _message.value = Message(
                    context.resources.getString(R.string.list_export_failed_unknown),
                    Message.Type.ERROR,
                    duration = Message.Duration.LONG,
                )
            }
        }

    private companion object {
        object Columns {
            const val AMOUNT_FORMATTED = "amountFormatted"
            const val AMOUNT_UNIT = "amountUnit"
            const val AMOUNT = "amount"
            const val CONTENT = "content"
            const val CREATED_AT = "createdAt"
            const val UUID = "uuid"
        }
    }
}
