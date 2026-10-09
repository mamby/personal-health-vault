package net.mamby.health.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.text.NumberFormat
import net.mamby.androidkit.compose.presentation.AndroidKitListSummary
import net.mamby.health.R

@Composable
internal fun contactListSummary(
    options: ContactListOptions,
    visibleCount: Int,
    totalCount: Int,
): AndroidKitListSummary {
    val field = stringResource(when (options.sortField) {
        ContactSortField.Name -> R.string.contact_name
        ContactSortField.DateAdded -> R.string.contacts_sort_date_added
        ContactSortField.LastModified -> R.string.contacts_sort_last_modified
    })
    val ascending = options.sortDirection == ContactSortDirection.Ascending
    val sort = stringResource(
        if (ascending) R.string.contacts_list_sort_ascending else R.string.contacts_list_sort_descending,
        field,
    )
    val spokenSort = stringResource(
        R.string.contacts_list_sort_description,
        field,
        stringResource(if (ascending) R.string.contacts_sort_ascending else R.string.contacts_sort_descending),
    )
    val filters = buildList {
        when (options.pinFilter) {
            ContactPinFilter.All -> Unit
            ContactPinFilter.Pinned -> add(stringResource(R.string.contacts_filter_pinned))
            ContactPinFilter.Unpinned -> add(stringResource(R.string.contacts_filter_unpinned))
        }
        if (options.hasPhone) add(stringResource(R.string.contacts_filter_has_phone))
        if (options.hasEmail) add(stringResource(R.string.contacts_filter_has_email))
        if (options.hasAddress) add(stringResource(R.string.contacts_filter_has_address))
        if (isEmpty()) add(stringResource(R.string.contacts_filter_all))
    }
    val numbers = NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0])
    val count = stringResource(
        R.string.contacts_list_result_count,
        numbers.format(visibleCount),
        numbers.format(totalCount),
    )
    val separator = stringResource(R.string.contacts_list_summary_separator)
    val text = (listOf(sort) + filters + count).joinToString(separator)
    val description = (listOf(spokenSort) + filters + count).joinToString(separator)
    return AndroidKitListSummary(
        text = text,
        contentDescription = description,
    )
}
