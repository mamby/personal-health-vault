package net.mamby.health.feature.contacts

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import net.mamby.androidkit.compose.action.AndroidKitActionGroupLabel
import net.mamby.androidkit.compose.presentation.AndroidKitListSort
import net.mamby.androidkit.compose.presentation.androidKitListSortAction
import net.mamby.health.core.model.VaultContact
import net.mamby.androidkit.compose.layout.AndroidKitPageAction
import net.mamby.androidkit.compose.layout.AndroidKitPageActionItem
import net.mamby.androidkit.compose.layout.AndroidKitPageActionSeparator
import net.mamby.health.R

@Composable
internal fun contactListActions(
    options: ContactListOptions,
    sort: AndroidKitListSort<VaultContact>,
    enabled: Boolean,
    onChange: (ContactListOptions) -> Unit,
): List<AndroidKitPageActionItem> {
    val sortAction = androidKitListSortAction(sort, enabled) { order ->
        onChange(options.copy(sortField = ContactSortField.valueOf(order.fieldId), sortDirection = order.direction))
    }
    return buildList {
        add(AndroidKitPageActionSeparator)
        add(sortAction)
        add(AndroidKitPageActionSeparator)
        add(choiceAction(stringResource(R.string.contacts_filter_show_all), R.drawable.ic_lucide_users, !options.hasFilters, enabled) {
            onChange(options.clearFilters())
        })
        add(AndroidKitPageActionSeparator)
        add(AndroidKitActionGroupLabel(stringResource(R.string.contacts_filter_show_with)))
        add(choiceAction(stringResource(R.string.contacts_filter_phone), R.drawable.ic_lucide_phone, options.hasPhone, enabled) {
            onChange(options.copy(hasPhone = !options.hasPhone))
        })
        add(choiceAction(stringResource(R.string.contacts_filter_email), R.drawable.ic_lucide_mail, options.hasEmail, enabled) {
            onChange(options.copy(hasEmail = !options.hasEmail))
        })
        add(choiceAction(stringResource(R.string.contacts_filter_address), R.drawable.ic_lucide_map_pin, options.hasAddress, enabled) {
            onChange(options.copy(hasAddress = !options.hasAddress))
        })
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
