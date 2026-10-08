package net.mamby.health.feature.contacts

import java.time.Instant
import java.util.Locale
import java.util.UUID
import net.mamby.health.core.model.VaultContact
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactListOptionsTest {
    private val now = Instant.parse("2026-10-08T12:00:00Z")

    @Test
    fun nameSortingUsesLocaleCollationInBothDirections() {
        listOf(
            Locale.FRENCH to listOf("alice", "Béatrice", "Émile", "Zoé"),
            Locale.forLanguageTag("ar") to listOf("أحمد", "بدر", "زيد"),
        ).forEach { (locale, names) ->
            val contacts = names.reversed().mapIndexed { index, name -> contact(index.toLong(), name) }
            assertEquals(names, ContactListOptions().applyTo(contacts, locale).map(VaultContact::name))
            assertEquals(
                names.reversed(),
                ContactListOptions(sortDirection = ContactSortDirection.Descending)
                    .applyTo(contacts, locale).map(VaultContact::name),
            )
        }
    }

    @Test
    fun creationDateSortingKeepsUnknownDatesLastInBothDirections() {
        val oldest = contact(1, "Zoe", createdAt = now.minusSeconds(60))
        val newest = contact(2, "Alice", createdAt = now)
        val unknown = contact(3, "Beatrice", createdAt = null)
        val contacts = listOf(unknown, newest, oldest)
        assertEquals(
            listOf(oldest, newest, unknown),
            ContactListOptions(sortField = ContactSortField.DateAdded).applyTo(contacts, Locale.ENGLISH),
        )
        assertEquals(
            listOf(newest, oldest, unknown),
            ContactListOptions(
                sortField = ContactSortField.DateAdded,
                sortDirection = ContactSortDirection.Descending,
            ).applyTo(contacts, Locale.ENGLISH),
        )
    }

    @Test
    fun modificationDateSortingUsesNamesAndIdsToResolveTies() {
        val older = contact(1, "Zoe", updatedAt = now.minusSeconds(60))
        val sameNameFirst = contact(2, "Alice")
        val sameNameSecond = contact(3, "alice")
        val sameDateLaterName = contact(4, "Beatrice")
        val contacts = listOf(sameDateLaterName, sameNameSecond, older, sameNameFirst)
        assertEquals(
            listOf(older, sameNameFirst, sameNameSecond, sameDateLaterName),
            ContactListOptions(sortField = ContactSortField.LastModified).applyTo(contacts, Locale.ENGLISH),
        )
        assertEquals(
            listOf(sameNameFirst, sameNameSecond, sameDateLaterName, older),
            ContactListOptions(
                sortField = ContactSortField.LastModified,
                sortDirection = ContactSortDirection.Descending,
            ).applyTo(contacts, Locale.ENGLISH),
        )
        ContactSortDirection.entries.forEach { direction ->
            assertEquals(
                listOf(sameNameFirst, sameNameSecond),
                ContactListOptions(sortDirection = direction)
                    .applyTo(listOf(sameNameSecond, sameNameFirst), Locale.ENGLISH),
            )
        }
    }

    @Test
    fun filtersCombinePinStatusAndNonblankFieldsWithoutMutatingContacts() {
        val complete = contact(1, "Complete").copy(
            phoneNumbers = listOf("111"),
            emailAddresses = listOf("person@example.test"),
            addresses = listOf("Paris"),
        )
        val pinnedComplete = complete.copy(id = UUID(0, 2), name = "Pinned", isPinned = true)
        val missingPhone = complete.copy(id = UUID(0, 3), name = "No phone", phoneNumbers = listOf(" "))
        val missingEmail = complete.copy(id = UUID(0, 4), name = "No email", emailAddresses = emptyList())
        val missingAddress = complete.copy(id = UUID(0, 5), name = "No address", addresses = emptyList())
        val contacts = listOf(complete, pinnedComplete, missingPhone, missingEmail, missingAddress)
        val original = contacts.toList()
        val fields = ContactListOptions(hasPhone = true, hasEmail = true, hasAddress = true)
        assertEquals(listOf(complete, pinnedComplete), fields.applyTo(contacts, Locale.ENGLISH))
        assertEquals(
            listOf(pinnedComplete),
            fields.copy(pinFilter = ContactPinFilter.Pinned).applyTo(contacts, Locale.ENGLISH),
        )
        assertEquals(
            listOf(complete),
            fields.copy(pinFilter = ContactPinFilter.Unpinned).applyTo(contacts, Locale.ENGLISH),
        )
        assertEquals(original, contacts)
    }

    private fun contact(
        id: Long,
        name: String,
        createdAt: Instant? = now,
        updatedAt: Instant = now,
    ) = VaultContact(id = UUID(0, id), name = name, createdAt = createdAt, updatedAt = updatedAt)
}
