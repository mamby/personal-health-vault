package net.mamby.health.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.Instant
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID
import net.mamby.androidkit.compose.action.AndroidKitListSelection
import net.mamby.androidkit.compose.presentation.rememberAndroidKitListState
import net.mamby.health.R
import net.mamby.health.core.model.VaultContact
import net.mamby.health.feature.contacts.ContactsScreen
import net.mamby.health.feature.contacts.rememberContactListOptionsState
import net.mamby.health.navigation.TopLevelDestination
import net.mamby.health.ui.components.AppNavigationSuite
import net.mamby.health.ui.theme.HealthVaultTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactListOptionsInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()
    private val now = Instant.parse("2026-10-08T12:00:00Z")
    private var localizedContext: Context? = null
    private val uiContext: Context get() = localizedContext ?: composeRule.activity

    @Before
    fun keepTestActivityAwake() {
        composeRule.activityRule.scenario.onActivity {
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    @Test
    fun filterSummaryScrollsAwayWithContactsAndReturnsAtTheStart() {
        val contacts = (1L..30L).map { contact(it, "Contact ${it.toString().padStart(2, '0')}") }
        composeRule.setContent { ContactList(contacts) }
        val summary = assertInlineSummary(shown = 30, total = 30)
        val list = composeRule.onNode(hasScrollToIndexAction())
        list.performScrollToIndex(14)
        summary.assertIsNotDisplayed()
        composeRule.onNodeWithText("Contact 14").assertIsDisplayed()
        list.performScrollToIndex(0)
        summary.assertIsDisplayed()
        composeRule.onNodeWithText("Contact 01").assertIsDisplayed()
    }

    @Test
    fun titleBarMenuSortsAllThreeFieldsBothWaysAndKeepsPinnedFirst() {
        val alice = contact(1, "Alice", added = 30, modified = 20)
        val beatrice = contact(2, "Beatrice", added = 10, modified = 30)
        val zoe = contact(3, "Zoe", added = 20, modified = 10)
        val pinned = contact(4, "Priority contact", added = 40, modified = 40).copy(isPinned = true)
        composeRule.setContent { ContactList(listOf(zoe, pinned, beatrice, alice)) }
        val cases = listOf(
            R.string.contact_name to listOf("Alice", "Beatrice", "Zoe"),
            R.string.contacts_sort_date_added to listOf("Beatrice", "Zoe", "Alice"),
            R.string.contacts_sort_last_modified to listOf("Zoe", "Alice", "Beatrice"),
        )
        cases.forEach { (field, ascending) ->
            chooseMenuOption(field)
            chooseMenuOption(R.string.contacts_sort_ascending)
            assertOrder(listOf("Priority contact") + ascending)
            assertInlineSummary(field, R.string.contacts_sort_ascending, shown = 4, total = 4)
            chooseMenuOption(R.string.contacts_sort_descending)
            assertOrder(listOf("Priority contact") + ascending.reversed())
            assertInlineSummary(field, R.string.contacts_sort_descending, shown = 4, total = 4)
        }
        openSortMenu()
        menuOption(R.string.contacts_sort_last_modified).assertIsDisplayed().assertIsSelected()
        menuOption(R.string.contacts_sort_descending).assertIsDisplayed().assertIsSelected()
        menuOption(R.string.contact_name).assertIsNotSelected()
        menuOption(R.string.contacts_sort_ascending).assertIsNotSelected()
    }

    @Test
    fun titleBarFiltersCombineShowEmptyResultsAndResetWithoutChangingSort() {
        val alice = contact(1, "Alice").copy(isPinned = true, phoneNumbers = listOf("111"))
        val bob = contact(2, "Bob").copy(
            phoneNumbers = listOf("222"), emailAddresses = listOf("bob@example.test"), addresses = listOf("Paris"),
        )
        val charlie = contact(3, "Charlie").copy(emailAddresses = listOf("charlie@example.test"), addresses = listOf("Lyon"))
        composeRule.setContent { ContactList(listOf(alice, bob, charlie)) }
        assertInlineSummary(shown = 3, total = 3)
        chooseMenuOption(R.string.contacts_sort_descending)
        chooseMenuOption(R.string.contacts_filter_has_phone)
        chooseMenuOption(R.string.contacts_filter_has_email)
        chooseMenuOption(R.string.contacts_filter_has_address)
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
        composeRule.onNodeWithText("Alice").assertDoesNotExist()
        composeRule.onNodeWithText("Charlie").assertDoesNotExist()
        openMenu()
        menuOption(R.string.contacts_filter_has_email).performScrollTo().assertIsSelected()
        menuOption(R.string.contacts_filter_has_phone).performScrollTo().assertIsSelected().performClick()
        chooseMenuOption(R.string.contacts_filter_has_phone)
        val fieldFilters = listOf(
            R.string.contacts_filter_has_phone,
            R.string.contacts_filter_has_email,
            R.string.contacts_filter_has_address,
        )
        assertInlineSummary(
            direction = R.string.contacts_sort_descending, filters = fieldFilters, shown = 1, total = 3,
        )
        chooseMenuOption(R.string.contacts_filter_pinned)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.contacts_no_matches_title)).assertIsDisplayed()
        assertInlineSummary(
            direction = R.string.contacts_sort_descending,
            filters = listOf(R.string.contacts_filter_pinned) + fieldFilters,
            shown = 0, total = 3,
        )
        chooseMenuOption(R.string.contacts_filter_unpinned)
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
        chooseMenuOption(R.string.contacts_filter_has_phone)
        assertOrder(listOf("Charlie", "Bob"))
        clearFilters(count = 3)
        assertOrder(listOf("Alice", "Charlie", "Bob"))
        assertInlineSummary(direction = R.string.contacts_sort_descending, shown = 3, total = 3)
        openMenu()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.contacts_filter_has_email))
            .performScrollTo().assertIsDisplayed()
        menuOption(R.string.contacts_filter_has_email).assertIsNotSelected()
    }

    @Test
    fun selectAllSharesAndDeletesOnlyFilteredContactsWithHoistedSelection() {
        val alice = contact(1, "Alice").copy(phoneNumbers = listOf("111"))
        val bob = contact(2, "Bob").copy(phoneNumbers = listOf("222"))
        val charlie = contact(3, "Charlie")
        val shared = mutableListOf<String>()
        val deleted = mutableListOf<Set<UUID>>()
        composeRule.setContent {
            ContactList(listOf(alice, bob, charlie), onShare = { shared += it }, onDelete = { deleted += it })
        }
        chooseMenuOption(R.string.contacts_filter_has_phone)
        fun selectAll() {
            clickTitleAction(R.string.common_select)
            composeRule.onNodeWithContentDescription(composeRule.activity.getString(
                net.mamby.androidkit.compose.R.string.androidkit_compose_select_all,
            )).performClick()
            composeRule.onNodeWithText("Alice").assertIsSelected()
            composeRule.onNodeWithText("Bob").assertIsSelected()
        }
        selectAll()
        assertInlineSummary(filters = listOf(R.string.contacts_filter_has_phone), shown = 2, total = 3)
        clickTitleAction(R.string.common_share)
        composeRule.runOnIdle {
            assertTrue(shared.single().contains("Alice"))
            assertTrue(shared.single().contains("Bob"))
            assertFalse(shared.single().contains("Charlie"))
        }
        selectAll()
        clickTitleAction(R.string.common_delete)
        composeRule.onNode(
            hasText(composeRule.activity.getString(R.string.common_delete)) and hasAnyAncestor(isDialog()),
        ).performClick()
        composeRule.runOnIdle { assertEquals(listOf(setOf(alice.id, bob.id)), deleted) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun titleBarMenusRemainUsableInRtlAtLargeFontScale() {
        val alice = contact(1, "Alice").copy(phoneNumbers = listOf("111"))
        val bob = contact(2, "Bob")
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(380.dp, 800.dp))) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        ContactList(listOf(alice, bob))
                    }
                }
            }
        }
        chooseMenuOption(R.string.contacts_sort_descending)
        chooseMenuOption(R.string.contacts_filter_has_phone)
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertDoesNotExist()
        val summary = assertInlineSummary(
            direction = R.string.contacts_sort_descending,
            filters = listOf(R.string.contacts_filter_has_phone), shown = 1, total = 2,
        ).fetchSemanticsNode().boundsInRoot
        val row = composeRule.onNodeWithText("Alice", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(summary.bottom <= row.top)
        clearFilters(count = 1)
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("Bob").assertIsDisplayed()
        assertOrder(listOf("Bob", "Alice"))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun inlineSummaryUsesLocalizedLabelsAndCountsInFrenchAndArabic() {
        val contacts = (1L..12L).map { id ->
            contact(id, "Contact $id").copy(phoneNumbers = if (id <= 3) listOf("111") else emptyList())
        }
        val contextState = mutableStateOf<Context>(composeRule.activity)
        composeRule.setContent {
            val context = contextState.value
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalLayoutDirection provides if (context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                    LayoutDirection.Rtl
                } else {
                    LayoutDirection.Ltr
                },
            ) {
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(380.dp, 800.dp))) {
                    DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                        ContactList(contacts)
                    }
                }
            }
        }
        for (locale in listOf(Locale.FRENCH, Locale.forLanguageTag("ar"))) {
            val configuration = Configuration(composeRule.activity.resources.configuration).apply {
                setLocales(LocaleList(locale))
            }
            val context = composeRule.activity.createConfigurationContext(configuration)
            composeRule.runOnIdle {
                localizedContext = context
                contextState.value = context
            }
            chooseMenuOption(R.string.contacts_sort_date_added)
            chooseMenuOption(R.string.contacts_filter_has_phone)
            val summary = assertInlineSummary(
                field = R.string.contacts_sort_date_added, direction = R.string.contacts_sort_descending,
                filters = listOf(R.string.contacts_filter_has_phone), shown = 3, total = 12,
            ).fetchSemanticsNode().boundsInRoot
            val row = composeRule.onNodeWithText("Contact 1", useUnmergedTree = true)
                .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertTrue(summary.bottom <= row.top)
            clearFilters(count = 1)
            assertInlineSummary(
                field = R.string.contacts_sort_date_added, direction = R.string.contacts_sort_descending,
                shown = 12, total = 12,
            )
        }
    }

    @Composable
    private fun ContactList(
        contacts: List<VaultContact>,
        onShare: (String) -> Unit = {},
        onDelete: (Set<UUID>) -> Unit = {},
    ) {
        HealthVaultTheme {
            val optionsState = rememberContactListOptionsState()
            val listState = rememberAndroidKitListState(
                contacts.filter(optionsState.options::matches).map { it.id.toString() }.toSet(),
            )
            AppNavigationSuite(
                selectedDestination = TopLevelDestination.Contacts,
                onDestinationSelected = {},
                selection = AndroidKitListSelection(listState.selection, onActionError = { error("Unexpected failure") }) {},
            ) {
                ContactsScreen(
                    contacts = contacts,
                    onAdd = {}, onSelected = {}, onEdit = {}, onShare = onShare,
                    onSetPinned = { _, _, complete -> complete(true) },
                    onDelete = { ids, complete -> onDelete(ids); complete(true) },
                    onActionError = { error("Unexpected failure") },
                    optionsState = optionsState,
                    listState = listState,
                )
            }
        }
    }

    private fun chooseMenuOption(labelId: Int) {
        if (labelId in listOf(
            R.string.contact_name, R.string.contacts_sort_date_added, R.string.contacts_sort_last_modified,
            R.string.contacts_sort_ascending, R.string.contacts_sort_descending,
        )) {
            openSortMenu()
        } else {
            openMenu()
        }
        menuOption(labelId).performScrollTo().performClick()
    }

    private fun menuOption(labelId: Int): SemanticsNodeInteraction = composeRule.onNode(
        hasText(uiContext.getString(labelId)) and hasAnyAncestor(isPopup()),
    )

    private fun openSortMenu() {
        openMenu()
        composeRule.onNode(
            hasText(uiContext.getString(R.string.contacts_sort_title)) and hasAnyAncestor(isPopup()),
        ).performScrollTo().performClick()
    }

    private fun clearFilters(count: Int) {
        val numbers = NumberFormat.getIntegerInstance(uiContext.resources.configuration.locales[0])
        chooseMenuText(uiContext.getString(R.string.contacts_clear_filters_count, numbers.format(count)))
    }

    private fun chooseMenuText(label: String) {
        openMenu()
        composeRule.onNode(hasText(label) and hasAnyAncestor(isPopup())).performScrollTo().performClick()
    }

    private fun openMenu() {
        composeRule.onNode(
            hasContentDescription(uiContext.getString(
                net.mamby.androidkit.compose.R.string.androidkit_compose_more,
            )) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button),
        ).performClick()
    }

    private fun clickTitleAction(labelId: Int) {
        val label = composeRule.activity.getString(labelId)
        if (composeRule.onAllNodes(hasContentDescription(label)).fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithContentDescription(label).performClick()
        } else {
            chooseMenuText(label)
        }
    }

    private fun assertOrder(names: List<String>) {
        val positions = names.map {
            composeRule.onNodeWithText(it, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        }
        assertEquals(names.size, positions.distinct().size)
        assertEquals(positions.sorted(), positions)
    }

    private fun assertInlineSummary(
        field: Int = R.string.contact_name,
        direction: Int = R.string.contacts_sort_ascending,
        filters: List<Int> = listOf(R.string.contacts_filter_all),
        shown: Int,
        total: Int,
    ): SemanticsNodeInteraction {
        val numbers = NumberFormat.getIntegerInstance(uiContext.resources.configuration.locales[0])
        val description = (listOf(uiContext.getString(
            R.string.contacts_list_sort_description, uiContext.getString(field), uiContext.getString(direction),
        )) + filters.map(uiContext::getString) + uiContext.getString(
            R.string.contacts_list_result_count, numbers.format(shown), numbers.format(total),
        )).joinToString(uiContext.getString(R.string.contacts_list_summary_separator))
        return composeRule.onNodeWithContentDescription(description).assertIsDisplayed().assertHasNoClickAction()
    }

    private fun contact(id: Long, name: String, added: Long = 0, modified: Long = 0) = VaultContact(
        id = UUID(0, id), name = name, createdAt = now.plusSeconds(added), updatedAt = now.plusSeconds(modified),
    )
}
