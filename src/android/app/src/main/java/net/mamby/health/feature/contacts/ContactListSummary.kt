package net.mamby.health.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import java.text.NumberFormat
import net.mamby.androidkit.compose.presentation.AndroidKitListSummary
import net.mamby.androidkit.compose.presentation.AndroidKitListSort
import net.mamby.androidkit.compose.presentation.androidKitListSortSummary
import net.mamby.health.core.model.VaultContact
import net.mamby.health.R

@Composable
internal fun contactListSummary(
    options: ContactListOptions,
    listSort: AndroidKitListSort<VaultContact>,
    visibleCount: Int,
    totalCount: Int,
): AndroidKitListSummary {
    val sort = androidKitListSortSummary(listSort)
    val filters = buildList {
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
    val text = (listOf(sort.text) + filters + count).joinToString(separator)
    val description = (listOf(sort.contentDescription) + filters + count).joinToString(separator)
    return AndroidKitListSummary(
        text = text,
        contentDescription = description,
    )
}
