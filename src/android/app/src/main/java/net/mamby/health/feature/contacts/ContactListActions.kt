package net.mamby.health.feature.contacts

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import java.text.NumberFormat
import net.mamby.androidkit.compose.action.AndroidKitSubmenuAction
import net.mamby.androidkit.compose.layout.AndroidKitPageAction
import net.mamby.androidkit.compose.layout.AndroidKitPageActionItem
import net.mamby.androidkit.compose.layout.AndroidKitPageActionSeparator
import net.mamby.health.R
import net.mamby.health.ui.components.titleBarAction

@Composable
internal fun contactListActions(
    options: ContactListOptions,
    enabled: Boolean,
    onChange: (ContactListOptions) -> Unit,
): List<AndroidKitPageActionItem> {
    val fields = listOf(
        Triple(ContactSortField.Name, R.string.contact_name, R.drawable.ic_lucide_list),
        Triple(ContactSortField.DateAdded, R.string.contacts_sort_date_added, R.drawable.ic_lucide_calendar_days),
        Triple(ContactSortField.LastModified, R.string.contacts_sort_last_modified, R.drawable.ic_lucide_calendar_days),
    ).map { (field, label, icon) ->
        choiceAction(stringResource(label), icon, options.sortField == field, enabled) {
            onChange(options.withSortField(field))
        }
    }
    val directions = listOf(
        ContactSortDirection.Ascending to R.string.contacts_sort_ascending,
        ContactSortDirection.Descending to R.string.contacts_sort_descending,
    ).map { (direction, label) ->
        choiceAction(stringResource(label), R.drawable.ic_lucide_list, options.sortDirection == direction, enabled) {
            onChange(options.copy(sortDirection = direction))
        }
    }
    val sortLabel = stringResource(R.string.contacts_sort_title)
    val sortIcon = ImageVector.vectorResource(R.drawable.ic_lucide_list)
    return buildList {
        add(AndroidKitPageActionSeparator)
        add(AndroidKitSubmenuAction(label = sortLabel, icon = sortIcon, enabled = enabled) {
            fields.forEach { action ->
                item(label = action.label, icon = action.icon, enabled = action.enabled, onClick = action.onClick)
            }
            separator()
            directions.forEach { action ->
                item(label = action.label, icon = action.icon, enabled = action.enabled, onClick = action.onClick)
            }
        })
        add(AndroidKitPageActionSeparator)
        listOf(
            Triple(ContactPinFilter.All, R.string.contacts_filter_all, R.drawable.ic_lucide_list),
            Triple(ContactPinFilter.Pinned, R.string.contacts_filter_pinned, R.drawable.ic_lucide_pin),
            Triple(ContactPinFilter.Unpinned, R.string.contacts_filter_unpinned, R.drawable.ic_lucide_pin_off),
        ).forEach { (filter, label, icon) ->
            add(choiceAction(stringResource(label), icon, options.pinFilter == filter, enabled) {
                onChange(options.copy(pinFilter = filter))
            })
        }
        add(AndroidKitPageActionSeparator)
        add(choiceAction(stringResource(R.string.contacts_filter_has_phone), R.drawable.ic_lucide_phone, options.hasPhone, enabled) {
            onChange(options.copy(hasPhone = !options.hasPhone))
        })
        add(choiceAction(stringResource(R.string.contacts_filter_has_email), R.drawable.ic_lucide_mail, options.hasEmail, enabled) {
            onChange(options.copy(hasEmail = !options.hasEmail))
        })
        add(choiceAction(stringResource(R.string.contacts_filter_has_address), R.drawable.ic_lucide_map_pin, options.hasAddress, enabled) {
            onChange(options.copy(hasAddress = !options.hasAddress))
        })
        val count = listOf(
            options.pinFilter != ContactPinFilter.All, options.hasPhone, options.hasEmail, options.hasAddress,
        ).count { it }
        add(titleBarAction(
            label = if (options.hasFilters) {
                stringResource(
                    R.string.contacts_clear_filters_count,
                    NumberFormat.getIntegerInstance(LocalConfiguration.current.locales[0]).format(count),
                )
            } else stringResource(R.string.contacts_clear_filters),
            icon = R.drawable.ic_lucide_x,
            enabled = enabled && options.hasFilters,
            onClick = { onChange(options.clearFilters()) },
        ))
    }
}

@Composable
private fun choiceAction(
    label: String,
    @DrawableRes icon: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
): AndroidKitPageAction = titleBarAction(
    label = if (selected) stringResource(R.string.contacts_selected_option, label) else label,
    icon = if (selected) R.drawable.ic_lucide_check else icon,
    enabled = enabled,
    onClick = onClick,
)
