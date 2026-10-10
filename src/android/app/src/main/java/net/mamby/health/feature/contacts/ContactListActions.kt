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
        Triple(ContactSortField.Name, R.string.contact_name, R.drawable.ic_lucide_case_sensitive),
        Triple(ContactSortField.DateAdded, R.string.contacts_sort_date_added, R.drawable.ic_lucide_calendar_plus),
        Triple(ContactSortField.LastModified, R.string.contacts_sort_last_modified, R.drawable.ic_lucide_calendar_clock),
    ).map { (field, label, icon) ->
        choiceAction(stringResource(label), icon, options.sortField == field, enabled) {
            onChange(options.withSortField(field))
        }
    }
    val directions = listOf(
        Triple(ContactSortDirection.Ascending, R.string.contacts_sort_ascending, R.drawable.ic_lucide_arrow_up_narrow_wide),
        Triple(ContactSortDirection.Descending, R.string.contacts_sort_descending, R.drawable.ic_lucide_arrow_down_wide_narrow),
    ).map { (direction, label, icon) ->
        choiceAction(stringResource(label), icon, options.sortDirection == direction, enabled) {
            onChange(options.copy(sortDirection = direction))
        }
    }
    val sortLabel = stringResource(R.string.contacts_sort_title)
    val sortIcon = ImageVector.vectorResource(R.drawable.ic_lucide_arrow_down_up)
    return buildList {
        add(AndroidKitPageActionSeparator)
        add(AndroidKitSubmenuAction(label = sortLabel, icon = sortIcon, enabled = enabled) {
            fields.forEach { action ->
                item(label = action.label, icon = action.icon, selected = action.selected, enabled = action.enabled, onClick = action.onClick)
            }
            separator()
            directions.forEach { action ->
                item(label = action.label, icon = action.icon, selected = action.selected, enabled = action.enabled, onClick = action.onClick)
            }
        })
        add(AndroidKitPageActionSeparator)
        add(choiceAction(stringResource(R.string.contacts_filter_all), R.drawable.ic_lucide_users, !options.hasFilters, enabled) {
            onChange(options.clearFilters())
        })
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
            options.hasPhone, options.hasEmail, options.hasAddress,
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
): AndroidKitPageAction = AndroidKitPageAction(
    label = label,
    icon = ImageVector.vectorResource(icon),
    selected = selected,
    enabled = enabled,
    onClick = onClick,
)
