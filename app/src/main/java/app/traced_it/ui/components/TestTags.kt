package app.traced_it.ui.components

import app.traced_it.data.database.EntryUnit

object TestTags {
    // Components
    const val CONFIRMATION_DIALOG_CONFIRM = "confirmation_dialog_confirm"
    const val CONFIRMATION_DIALOG_DISMISS = "confirmation_dialog_dismiss"
    const val DATE_TIME_PICKER_DATE = "date_time_picker_date"
    const val DATE_TIME_PICKER_HOUR = "date_time_picker_hour"
    const val DATE_TIME_PICKER_MINUTE = "date_time_picker_minute"
    const val DATE_TIME_PICKER_ITEM = "date_time_picker_item"

    // Detail
    const val ENTRY_DETAIL_CONTENT = "entry_detail_content"
    const val ENTRY_DETAIL_CONTENT_CLEAR = "entry_detail_content_clear"
    const val ENTRY_DETAIL_DISMISS = "entry_detail_dismiss"
    const val ENTRY_DETAIL_CREATED_AT_RESET = "entry_detail_created_at_reset"
    const val ENTRY_DETAIL_SAVE_INSERT = "entry_detail_save_insert"
    const val ENTRY_DETAIL_SAVE_UPDATE = "entry_detail_save_update"
    const val ENTRY_DETAIL_UNIT_MENU = "entry_detail_unit_menu"
    fun entryDetailAmount(amountUnit: EntryUnit) = "entry_detail_amount_${amountUnit.id}"
    fun entryDetailUnitChoice(amountUnit: EntryUnit) = "entry_detail_unit_choice_${amountUnit.id}"
    fun entryDetailUnitMenuItem(amountUnit: EntryUnit) = "entry_detail_unit_menu_item_${amountUnit.id}"

    // List
    const val ENTRY_LIST_COLUMN = "entry_list_column"
    const val ENTRY_LIST_FILTER_BACK = "entry_list_filter_back"
    const val ENTRY_LIST_FILTER_CLEAR = "entry_list_filter_clear"
    const val ENTRY_LIST_FILTER_EXPAND = "entry_list_filter_expand"
    const val ENTRY_LIST_FILTER_EXPORT = "entry_list_filter_export"
    const val ENTRY_LIST_FILTER_QUERY = "entry_list_filter_query"
    const val ENTRY_LIST_HEADING = "entry_list_heading"
    const val ENTRY_LIST_INSERT = "entry_list_insert"
    const val ENTRY_LIST_ITEM = "entry_list_item"
    const val ENTRY_LIST_ITEM_DELETE = "entry_list_item_delete"
    const val ENTRY_LIST_ITEM_REPEAT = "entry_list_item_repeat"
    const val ENTRY_LIST_ITEM_CONTENT = "entry_list_item_text"
    const val ENTRY_LIST_ITEM_TIME = "entry_list_item_time"
    const val ENTRY_LIST_ITEM_UPDATE = "entry_list_item_update"
    const val ENTRY_LIST_MENU = "entry_list_menu"
    const val ENTRY_LIST_MENU_DELETE_ALL = "entry_list_menu_delete_all"
    const val ENTRY_LIST_MENU_EXPORT_ALL = "entry_list_menu_export_all"
    const val ENTRY_LIST_MENU_IMPORT = "entry_list_menu_import"
    const val ENTRY_LIST_SELECTED_BACK = "entry_list_selected_back"
    const val ENTRY_LIST_SELECTED_DELETE = "entry_list_selected_delete"
    const val ENTRY_LIST_SELECTED_MENU = "entry_list_seleted_menu"
    const val ENTRY_LIST_SELECTED_MENU_COPY = "entry_list_selected_menu_copy"
    const val ENTRY_LIST_SELECTED_MENU_FILTER_SIMILAR = "entry_list_selected_menu_filter_similar"
    const val ENTRY_LIST_SELECTED_MENU_REPEAT = "entry_list_selected_menu_repeat"
    const val ENTRY_LIST_SELECTED_TITLE = "entry_list_selected_title"
    const val ENTRY_LIST_SELECTED_UPDATE = "entry_list_selected_update"
    const val ENTRY_LIST_TITLE = "entry_list_title"
    fun entryListFilterStatus(itemCount: Int, totalCount: Int) = "entry_list_filter_status_${itemCount}_${totalCount}"
}
