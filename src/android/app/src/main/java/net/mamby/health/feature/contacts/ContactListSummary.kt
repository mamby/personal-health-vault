package net.mamby.health.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.mamby.androidkit.compose.presentation.AndroidKitListSummary
import net.mamby.androidkit.compose.presentation.AndroidKitListSummaryFilter
import net.mamby.health.R

@Composable
internal fun contactListSummary(
    options: ContactListOptions,
    matchingCount: Int,
    totalCount: Int,
): AndroidKitListSummary = AndroidKitListSummary(
    matchingCount = matchingCount,
    totalCount = totalCount,
    itemCountResource = R.plurals.contacts_list_count,
    activeFilters = buildList {
        if (options.hasPhone) add(AndroidKitListSummaryFilter(
            stringResource(R.string.contacts_list_has_phone), stringResource(R.string.contacts_filter_has_phone),
        ))
        if (options.hasEmail) add(AndroidKitListSummaryFilter(
            stringResource(R.string.contacts_list_has_email), stringResource(R.string.contacts_filter_has_email),
        ))
        if (options.hasAddress) add(AndroidKitListSummaryFilter(
            stringResource(R.string.contacts_list_has_address), stringResource(R.string.contacts_filter_has_address),
        ))
    },
)
