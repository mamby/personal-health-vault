package net.mamby.health.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.Collator
import java.time.Instant
import java.util.Locale
import net.mamby.health.core.model.VaultContact

enum class ContactSortField { Name, DateAdded, LastModified }

enum class ContactSortDirection { Ascending, Descending }

enum class ContactPinFilter { All, Pinned, Unpinned }

data class ContactListOptions(
    val sortField: ContactSortField = ContactSortField.Name,
    val sortDirection: ContactSortDirection = ContactSortDirection.Ascending,
    val pinFilter: ContactPinFilter = ContactPinFilter.All,
    val hasPhone: Boolean = false,
    val hasEmail: Boolean = false,
    val hasAddress: Boolean = false,
) {
    val hasFilters: Boolean
        get() = pinFilter != ContactPinFilter.All || hasPhone || hasEmail || hasAddress

    fun clearFilters(): ContactListOptions = copy(
        pinFilter = ContactPinFilter.All,
        hasPhone = false,
        hasEmail = false,
        hasAddress = false,
    )

    fun withSortField(field: ContactSortField): ContactListOptions =
        if (field == sortField) this else copy(
            sortField = field,
            sortDirection = if (field == ContactSortField.Name) {
                ContactSortDirection.Ascending
            } else {
                ContactSortDirection.Descending
            },
        )

    fun matches(contact: VaultContact): Boolean =
        when (pinFilter) {
            ContactPinFilter.All -> true
            ContactPinFilter.Pinned -> contact.isPinned
            ContactPinFilter.Unpinned -> !contact.isPinned
        } &&
            (!hasPhone || contact.phoneNumbers.any(String::isNotBlank)) &&
            (!hasEmail || contact.emailAddresses.any(String::isNotBlank)) &&
            (!hasAddress || contact.addresses.any(String::isNotBlank))

    fun applyTo(contacts: List<VaultContact>, locale: Locale): List<VaultContact> {
        val collator = Collator.getInstance(locale).apply {
            strength = Collator.SECONDARY
            decomposition = Collator.CANONICAL_DECOMPOSITION
        }
        val byName = compareBy<VaultContact, String>(collator, VaultContact::name)
        val byDate = if (sortDirection == ContactSortDirection.Ascending) {
            naturalOrder<Instant>()
        } else {
            reverseOrder<Instant>()
        }
        val comparator = when (sortField) {
            ContactSortField.Name ->
                if (sortDirection == ContactSortDirection.Ascending) byName else byName.reversed()
            ContactSortField.DateAdded ->
                compareBy<VaultContact, Instant?>(nullsLast(byDate), VaultContact::createdAt).then(byName)
            ContactSortField.LastModified ->
                compareBy<VaultContact, Instant>(byDate, VaultContact::updatedAt).then(byName)
        }.thenBy(VaultContact::id)
        // AndroidKit owns pin grouping and preserves this order inside each section.
        return contacts.filter(::matches).sortedWith(comparator)
    }
}

@Stable
class ContactListOptionsState {
    var options: ContactListOptions by mutableStateOf(ContactListOptions())
}

@Composable
fun rememberContactListOptionsState(): ContactListOptionsState = remember { ContactListOptionsState() }
