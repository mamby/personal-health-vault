package net.mamby.health.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Instant
import java.util.Locale
import net.mamby.health.core.model.VaultContact
import net.mamby.androidkit.compose.presentation.AndroidKitListSort
import net.mamby.androidkit.compose.presentation.AndroidKitListSortDirection
import net.mamby.androidkit.compose.presentation.AndroidKitListSortField
import net.mamby.androidkit.compose.presentation.AndroidKitListSortOrder

enum class ContactSortField { Name, DateAdded, LastModified }

typealias ContactSortDirection = AndroidKitListSortDirection

data class ContactListOptions(
    val sortField: ContactSortField = ContactSortField.Name,
    val sortDirection: ContactSortDirection = ContactSortDirection.Ascending,
    val hasPhone: Boolean = false,
    val hasEmail: Boolean = false,
    val hasAddress: Boolean = false,
) {
    val hasFilters: Boolean
        get() = hasPhone || hasEmail || hasAddress

    fun clearFilters(): ContactListOptions = copy(
        hasPhone = false,
        hasEmail = false,
        hasAddress = false,
    )

    fun matches(contact: VaultContact): Boolean =
        (!hasPhone || contact.phoneNumbers.any(String::isNotBlank)) &&
            (!hasEmail || contact.emailAddresses.any(String::isNotBlank)) &&
            (!hasAddress || contact.addresses.any(String::isNotBlank))

    fun applyTo(contacts: List<VaultContact>, locale: Locale): List<VaultContact> {
        return sort(ContactSortField.Name.name, ContactSortField.DateAdded.name, ContactSortField.LastModified.name)
            .sortedItems(contacts.filter(::matches), { it.id.toString() }, locale)
    }

    fun sort(name: String, added: String, modified: String): AndroidKitListSort<VaultContact> {
        val byName = AndroidKitListSortField.Text<VaultContact>(ContactSortField.Name.name, name, value = VaultContact::name)
        return AndroidKitListSort(
            fields = listOf(
                byName,
                AndroidKitListSortField.DateTime<VaultContact, Instant>(ContactSortField.DateAdded.name, added, value = VaultContact::createdAt),
                AndroidKitListSortField.DateTime<VaultContact, Instant>(ContactSortField.LastModified.name, modified, value = VaultContact::updatedAt),
            ),
            order = AndroidKitListSortOrder(sortField.name, sortDirection),
            tieBreakers = listOf(byName),
        )
    }
}

@Stable
class ContactListOptionsState {
    var options: ContactListOptions by mutableStateOf(ContactListOptions())
}

@Composable
fun rememberContactListOptionsState(): ContactListOptionsState = remember { ContactListOptionsState() }
