package net.mamby.health.ui

import android.content.ClipboardManager
import android.content.ClipDescription
import androidx.compose.runtime.mutableStateOf
import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.util.UUID
import net.mamby.health.R
import net.mamby.health.core.model.VaultContact
import net.mamby.health.feature.contacts.ContactDetailScreen
import net.mamby.health.feature.contacts.ContactEditorScreen
import net.mamby.health.feature.contacts.ContactsScreen
import net.mamby.health.ui.theme.HealthVaultTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun contactFormCreatesVaultWideContactWithoutBlankValues() {
        var savedContact: VaultContact? = null
        var openedContactId: UUID? = null
        var canceled = false
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = null,
                    onCancel = { canceled = true },
                    onSaved = { openedContactId = it },
                    onSave = { contact, onResult ->
                        savedContact = contact
                        onResult(true)
                    },
                )
            }
        }

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.contact_name))
            .performTextInput("  Samira Haddad  ")
        composeRule
            .onNode(hasText("Samira Haddad", substring = true) and hasSetTextAction())
            .assertTextContains("Samira Haddad", substring = true)
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.common_save))
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertNotNull(savedContact)
            assertEquals("Samira Haddad", savedContact?.name)
            assertEquals(emptyList<String>(), savedContact?.phoneNumbers)
            assertEquals(emptyList<String>(), savedContact?.emailAddresses)
            assertEquals(emptyList<String>(), savedContact?.websites)
            assertEquals(emptyList<String>(), savedContact?.addresses)
            assertEquals(savedContact?.id, openedContactId)
            assertEquals(false, canceled)
        }
    }

    @Test
    fun addingValuesToNewContactFocusesEachNewField() {
        assertAddedValuesReceiveFocus(existing = null)
    }

    @Test
    fun addingValuesToExistingContactFocusesEachNewField() {
        assertAddedValuesReceiveFocus(
            existing = VaultContact(
                id = UUID.randomUUID(),
                name = "Samira",
                phoneNumbers = listOf("+33 6 12 34 56 78"),
                emailAddresses = listOf("samira@example.test"),
                websites = listOf("https://example.test"),
                addresses = listOf("Paris"),
                updatedAt = Instant.EPOCH,
            ),
        )
    }

    private fun assertAddedValuesReceiveFocus(existing: VaultContact?) {
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = existing,
                    onCancel = {},
                    onSaved = {},
                    onSave = { _, _ -> },
                )
            }
        }
        listOf(
            R.string.editor_section_basic,
            R.string.editor_section_contact_channels,
            R.string.editor_section_location_online,
        ).forEach { title ->
            composeRule.onNodeWithText(composeRule.activity.getString(title)).assertDoesNotExist()
        }
        listOf(
            R.string.contact_phone,
            R.string.contact_email_address,
            R.string.contact_website,
            R.string.contact_address,
        ).forEachIndexed { groupIndex, label ->
            repeat(2) { addedIndex ->
                composeRule
                    .onAllNodesWithText(composeRule.activity.getString(R.string.add_another))[groupIndex]
                    .performScrollTo()
                    .performClick()
                composeRule
                    .onAllNodes(hasText(composeRule.activity.getString(label)) and hasSetTextAction())[addedIndex + 1]
                    .assertIsFocused()
                    .performTextInput("new-$groupIndex-$addedIndex")
            }
        }
    }

    @Test
    fun dirtyCancelKeepsTheDraftUntilDiscardIsConfirmed() {
        var canceled = false
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = null,
                    onCancel = { canceled = true },
                    onSaved = {},
                    onSave = { _, _ -> },
                )
            }
        }

        composeRule
            .onNode(hasText(composeRule.activity.getString(R.string.contact_name)) and hasSetTextAction())
            .performTextInput("Samira")
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_cancel)).performClick()
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.unsaved_changes_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.unsaved_changes_keep_editing)).performClick()
        composeRule.onNode(hasText("Samira") and hasSetTextAction()).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(false, canceled) }

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.unsaved_changes_discard)).performClick()
        composeRule.runOnIdle { assertEquals(true, canceled) }
    }

    @Test
    fun cleanBackExitsWithoutDiscardConfirmation() {
        var canceled = false
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = null,
                    onCancel = { canceled = true },
                    onSaved = {},
                    onSave = { _, _ -> },
                )
            }
        }

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.runOnIdle { assertEquals(true, canceled) }
        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.unsaved_changes_title))
            .assertDoesNotExist()
    }

    @Test
    fun failedSaveRetainsTheDraftAndReEnablesSave() {
        var completion: ((Boolean) -> Unit)? = null
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = null,
                    onCancel = {},
                    onSaved = {},
                    onSave = { _, onResult -> completion = onResult },
                )
            }
        }

        composeRule
            .onNode(hasText(composeRule.activity.getString(R.string.contact_name)) and hasSetTextAction())
            .performTextInput("Samira")
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_save)).performClick()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_save)).assertDoesNotExist()

        composeRule.runOnIdle { requireNotNull(completion)(false) }

        composeRule.onNode(hasText("Samira") and hasSetTextAction()).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.common_save))
            .assertIsEnabled()
    }

    @Test
    fun invalidWebsiteShowsInlineValidationAndDisablesSave() {
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = null,
                    onCancel = {},
                    onSaved = {},
                    onSave = { _, _ -> },
                )
            }
        }

        composeRule
            .onNode(hasText(composeRule.activity.getString(R.string.contact_name)) and hasSetTextAction())
            .performTextInput("Samira")
        composeRule
            .onNode(hasText(composeRule.activity.getString(R.string.contact_website)) and hasSetTextAction())
            .performTextInput("ftp://example.test")

        composeRule
            .onNodeWithText(composeRule.activity.getString(R.string.invalid_contact_website))
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(composeRule.activity.getString(R.string.common_save))
            .assertIsNotEnabled()
    }

    @Test
    fun hardwareTabTraversalFollowsTheVisualFieldOrder() {
        composeRule.setContent {
            HealthVaultTheme {
                ContactEditorScreen(
                    existing = null,
                    onCancel = {},
                    onSaved = {},
                    onSave = { _, _ -> },
                )
            }
        }
        val nameField = composeRule
            .onNode(hasText(composeRule.activity.getString(R.string.contact_name)) and hasSetTextAction())
        val phoneField = composeRule
            .onNode(hasText(composeRule.activity.getString(R.string.contact_phone)) and hasSetTextAction())

        nameField.performClick().assertIsFocused()
        nameField.performKeyInput { pressKey(Key.Tab) }

        phoneField.assertIsFocused()
    }

    @Test
    fun everySavedContactValueIsAnIndependentAccessibleAction() {
        val invokedActions = mutableListOf<String>()
        var edits = 0
        var deletes = 0
        val sharedValues = mutableListOf<String>()
        val updates = mutableListOf<VaultContact>()
        val contact = VaultContact(
            id = UUID.fromString("982c7e3f-68ce-43e8-b480-69d46b755a31"),
            name = "Samira Haddad",
            phoneNumbers = listOf("+33 1 23 45 67 89", "+33 6 12 34 56 78"),
            emailAddresses = listOf("samira@example.com"),
            websites = listOf("https://example.com"),
            addresses = listOf("10 rue de la Paix\n75002 Paris"),
            notes = "Family doctor",
            updatedAt = Instant.EPOCH,
        )
        composeRule.setContent {
            HealthVaultTheme {
                ContactDetailScreen(
                    contact = contact,
                    onBack = null,
                    onEdit = { edits++ },
                    onDelete = { deletes++ },
                    onShare = { sharedValues += it },
                    onUpdate = { updated, complete ->
                        updates += updated
                        complete(true)
                    },
                    onDialPhone = { invokedActions += "phone:$it" },
                    onComposeEmail = { invokedActions += "email:$it" },
                    onOpenWebsite = { invokedActions += "website:$it" },
                    onSearchAddress = { invokedActions += "address:$it" },
                )
            }
        }

        val actions = listOf(
            composeRule.activity.getString(R.string.contact_phone_action, contact.phoneNumbers[0]) to
                "phone:${contact.phoneNumbers[0]}",
            composeRule.activity.getString(R.string.contact_phone_action, contact.phoneNumbers[1]) to
                "phone:${contact.phoneNumbers[1]}",
            composeRule.activity.getString(R.string.contact_email_action, contact.emailAddresses.single()) to
                "email:${contact.emailAddresses.single()}",
            composeRule.activity.getString(R.string.contact_website_action, contact.websites.single()) to
                "website:${contact.websites.single()}",
            composeRule.activity.getString(R.string.contact_address_action, contact.addresses.single()) to
                "address:${contact.addresses.single()}",
        )
        actions.forEach { (actionLabel, expectedAction) ->
            composeRule
                .onNodeWithText(expectedAction.substringAfter(':'))
                .performScrollTo()
                .assertIsDisplayed()
                .assertHasClickAction()
                .assert(SemanticsMatcher("Localized contact action label") {
                    it.config[SemanticsActions.OnClick].label == actionLabel
                })
                .performClick()
        }

        composeRule.runOnIdle {
            assertEquals(actions.map(Pair<String, String>::second), invokedActions)
        }

        // Each value, including read-only notes, has its own Kit context menu.
        (actions.map { it.second.substringAfter(':') } + requireNotNull(contact.notes)).forEach { value ->
            listOf(R.string.common_copy, R.string.common_share, R.string.common_delete).forEach { action ->
                composeRule.onNodeWithText(value)
                    .performScrollTo()
                    .performTouchInput { longClick(Offset(2f, center.y)) }
                    .assertIsSelected()
                listOf(R.string.common_copy, R.string.common_share, R.string.common_delete).forEach { label ->
                    composeRule.onNode(
                        hasText(composeRule.activity.getString(label)) and hasAnyAncestor(isPopup()),
                    ).assertIsDisplayed()
                }
                composeRule.onNode(
                    hasText(composeRule.activity.getString(action)) and hasAnyAncestor(isPopup()),
                ).performClick()
                if (action == R.string.common_copy) {
                    composeRule.runOnIdle {
                        val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
                        assertEquals(value, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
                        assertEquals(true, clipboard.primaryClipDescription?.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE))
                    }
                }
                if (action == R.string.common_delete) {
                    composeRule.onNodeWithText(composeRule.activity.getString(R.string.delete_contact_value_title))
                        .assertIsDisplayed()
                    composeRule.runOnIdle { assertEquals(emptyList<VaultContact>(), updates) }
                    composeRule.onNodeWithText(composeRule.activity.getString(R.string.common_cancel)).performClick()
                }
                composeRule.onNodeWithText(value).assertIsNotSelected()
                composeRule.onNodeWithText(composeRule.activity.getString(R.string.common_copy)).assertDoesNotExist()
            }
        }

        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.common_edit))
            .assertIsDisplayed().performClick()
        composeRule.runOnIdle {
            assertEquals(1, edits)
            assertEquals(0, deletes)
            assertEquals(actions.map { it.second.substringAfter(':') } + requireNotNull(contact.notes), sharedValues)
            assertEquals(emptyList<VaultContact>(), updates)
            assertEquals(actions.map(Pair<String, String>::second), invokedActions)
        }
    }

    @Test
    fun contactActionsPinShareAndConfirmDeletion() {
        val original = VaultContact(
            id = UUID.randomUUID(),
            name = "Samira",
            phoneNumbers = listOf("111", "222"),
            notes = "Family doctor",
            updatedAt = Instant.EPOCH,
        )
        val contact = mutableStateOf(original)
        val shared = mutableListOf<String>()
        var deletes = 0
        composeRule.setContent {
            HealthVaultTheme {
                ContactDetailScreen(
                    contact = contact.value,
                    onBack = null,
                    onEdit = {},
                    onDelete = { deletes++ },
                    onDialPhone = {},
                    onComposeEmail = {},
                    onOpenWebsite = {},
                    onSearchAddress = {},
                    onShare = { shared += it },
                    onUpdate = { updated, complete -> contact.value = updated; complete(true) },
                )
            }
        }
        clickTitleAction(R.string.common_pin)
        composeRule.runOnIdle { assertEquals(original.copy(isPinned = true), contact.value) }
        clickTitleAction(R.string.common_share)
        composeRule.runOnIdle {
            assertEquals(1, shared.size)
            org.junit.Assert.assertTrue(shared.single().contains("111\n222"))
            org.junit.Assert.assertTrue(shared.single().contains("Family doctor"))
        }
        clickTitleAction(R.string.common_unpin)
        composeRule.runOnIdle { assertEquals(original, contact.value) }

        composeRule.onNodeWithText("111").performScrollTo()
            .performTouchInput { longClick(Offset(2f, center.y)) }
        composeRule.onNode(hasText(composeRule.activity.getString(R.string.common_delete)) and hasAnyAncestor(isPopup()))
            .performClick()
        composeRule.runOnIdle { assertEquals(original, contact.value) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.common_delete)).performClick()
        composeRule.onNodeWithText("111").assertDoesNotExist()
        composeRule.onNodeWithText("222").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(original.copy(phoneNumbers = listOf("222")), contact.value) }

        composeRule.onNodeWithText("Family doctor").performScrollTo()
            .performTouchInput { longClick(Offset(2f, center.y)) }
        composeRule.onNode(hasText(composeRule.activity.getString(R.string.common_delete)) and hasAnyAncestor(isPopup()))
            .performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.common_delete)).performClick()
        composeRule.onNodeWithText("Family doctor").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(null, contact.value.notes) }

        clickTitleAction(R.string.common_delete)
        composeRule.runOnIdle { assertEquals(0, deletes) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.common_cancel)).performClick()
        composeRule.runOnIdle { assertEquals(0, deletes) }
        clickTitleAction(R.string.common_delete)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.common_delete)).performClick()
        composeRule.runOnIdle { assertEquals(1, deletes) }
    }

    @Test
    @OptIn(ExperimentalTestApi::class)
    fun pinnedContactsLeadParentListAndUnpinRestoresAlphabeticalOrder() {
        val alice = VaultContact(UUID.randomUUID(), "Alice", updatedAt = Instant.EPOCH)
        val zoe = VaultContact(UUID.randomUUID(), "Zoe", updatedAt = Instant.EPOCH, isPinned = true)
        val contacts = mutableStateOf(listOf(alice, zoe))
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 640.dp))) {
                HealthVaultTheme {
                    ContactsScreen(contacts = contacts.value, onAdd = {}, onSelected = {})
                }
            }
        }
        fun top(name: String) = composeRule.onNodeWithText(name, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot.top
        org.junit.Assert.assertTrue(top("Zoe") < top("Alice"))
        composeRule.runOnIdle { contacts.value = listOf(alice, zoe.copy(isPinned = false)) }
        org.junit.Assert.assertTrue(top("Alice") < top("Zoe"))
    }

    private fun clickTitleAction(labelId: Int) {
        val label = composeRule.activity.getString(labelId)
        val direct = composeRule.onAllNodes(androidx.compose.ui.test.hasContentDescription(label))
        if (direct.fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithContentDescription(label).performClick()
        } else {
            val more = composeRule.activity.getString(net.mamby.androidkit.compose.R.string.androidkit_compose_more)
            composeRule.onNodeWithContentDescription(more).performClick()
            composeRule.onNode(hasText(label) and hasAnyAncestor(isPopup())).performClick()
        }
    }
}
