package net.mamby.health.feature.contacts

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import java.text.NumberFormat
import net.mamby.androidkit.compose.presentation.AndroidKitListSort
import net.mamby.androidkit.compose.presentation.androidKitListSortAction
import net.mamby.health.core.model.VaultContact
import net.mamby.androidkit.compose.layout.AndroidKitPageAction
import net.mamby.androidkit.compose.layout.AndroidKitPageActionItem
import net.mamby.androidkit.compose.layout.AndroidKitPageActionSeparator
import net.mamby.health.R
import net.mamby.health.ui.components.titleBarAction

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
