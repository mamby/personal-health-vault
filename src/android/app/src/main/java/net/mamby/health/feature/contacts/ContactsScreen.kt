package net.mamby.health.feature.contacts

import android.content.ClipData
import android.content.ClipDescription
import android.os.PersistableBundle
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import java.net.URI
import java.time.Instant
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import net.mamby.androidkit.compose.presentation.AndroidKitSectionCard
import net.mamby.androidkit.compose.presentation.AndroidKitCard
import net.mamby.androidkit.compose.presentation.AndroidKitSectionCardEntry
import net.mamby.androidkit.compose.action.AndroidKitActionFlyoutScope
import net.mamby.androidkit.compose.layout.AndroidKitPage
import net.mamby.androidkit.compose.theme.AndroidKitThemeTokens
import net.mamby.health.R
import net.mamby.health.core.model.VaultContact
import net.mamby.health.ui.components.AppEditorScaffold
import net.mamby.health.ui.components.detailTitleBarActions
import net.mamby.health.ui.components.EditorFieldPair
import net.mamby.health.ui.components.EmptyState
import net.mamby.health.ui.components.ConfirmDeleteDialog
import net.mamby.health.ui.components.ListCard
import net.mamby.health.ui.components.rememberEditorState
import net.mamby.health.ui.components.addTitleBarAction
import net.mamby.health.ui.components.titleBarAction
import net.mamby.health.ui.components.withPagePadding
import net.mamby.health.ui.theme.LocalContactActionColors
import net.mamby.health.ui.theme.UiTokens

@Composable
fun ContactsScreen(
    contacts: List<VaultContact>,
    onAdd: () -> Unit,
    onSelected: (UUID) -> Unit,
) {
    val sortedContacts = remember(contacts) {
        contacts.sortedWith(
            compareByDescending<VaultContact> { it.isPinned }
                .thenBy { it.name.lowercase(Locale.getDefault()) }
                .thenBy(VaultContact::id),
        )
    }

    AndroidKitPage(
        title = stringResource(R.string.contacts_title),
        actions = listOf(
            addTitleBarAction(
                label = stringResource(R.string.add_contact),
                onClick = onAdd,
            ),
        ),
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(UiTokens.CardMinWidth),
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(padding),
            contentPadding = padding.withPagePadding(),
            horizontalArrangement = Arrangement.spacedBy(UiTokens.ContentSpacing),
            verticalArrangement = Arrangement.spacedBy(UiTokens.ContentSpacing),
        ) {
            if (sortedContacts.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(R.string.no_contacts_title),
                        body = stringResource(R.string.no_contacts_body),
                    )
                }
            } else {
                // Temporary presentation comparison; the first half uses Kit cards.
                itemsIndexed(sortedContacts, key = { _, contact -> contact.id }) { index, contact ->
                    if (index < (sortedContacts.size + 1) / 2) {
                        AndroidKitCard(
                            title = contact.name,
                            supportingText = contact.firstContactValue(),
                            onClick = { onSelected(contact.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {}
                    } else {
                        ListCard(
                            title = contact.name,
                            onClick = { onSelected(contact.id) },
                        ) {
                            contact.firstContactValue()?.let { Text(it) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactDetailScreen(
    contact: VaultContact,
    onBack: (() -> Unit)?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDialPhone: (String) -> Unit,
    onComposeEmail: (String) -> Unit,
    onOpenWebsite: (String) -> Unit,
    onSearchAddress: (String) -> Unit,
    onShare: (String) -> Unit,
    onUpdate: (VaultContact, (Boolean) -> Unit) -> Unit,
) {
    val actionColors = LocalContactActionColors.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val clipboardLabel = stringResource(R.string.contact_title)
    var deleting by remember(contact.id) { mutableStateOf(false) }
    var pendingRemoval by remember(contact.id) { mutableStateOf<ContactFieldRemoval?>(null) }
    var updating by remember(contact.id) { mutableStateOf(false) }
    val copyValue: (String) -> Unit = { value ->
        scope.launch {
            val clipData = ClipData.newPlainText(clipboardLabel, value).apply {
                description.extras = PersistableBundle().apply {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                }
            }
            clipboard.setClipEntry(ClipEntry(clipData))
        }
    }
    val shareText = buildList {
        add(contact.name)
        listOf(
            stringResource(R.string.contact_phone_numbers) to contact.phoneNumbers,
            stringResource(R.string.contact_email_addresses) to contact.emailAddresses,
            stringResource(R.string.contact_websites) to contact.websites,
            stringResource(R.string.contact_addresses) to contact.addresses,
            stringResource(R.string.common_notes) to listOfNotNull(contact.notes),
        ).forEach { (label, values) ->
            values.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.let {
                add("$label:\n${it.joinToString("\n")}")
            }
        }
    }.joinToString("\n\n")

    AndroidKitPage(
        title = stringResource(R.string.contact_title),
        onBack = onBack,
        actions = detailTitleBarActions(onEdit = onEdit) + listOf(
            titleBarAction(
                label = stringResource(R.string.common_share),
                icon = R.drawable.ic_lucide_share_2,
                onClick = { onShare(shareText) },
            ),
            titleBarAction(
                label = stringResource(if (contact.isPinned) R.string.common_unpin else R.string.common_pin),
                icon = R.drawable.ic_lucide_pin,
                enabled = !updating,
                onClick = {
                    updating = true
                    onUpdate(contact.copy(isPinned = !contact.isPinned)) { updating = false }
                },
            ),
            titleBarAction(
                label = stringResource(R.string.common_delete),
                icon = R.drawable.ic_lucide_trash_2,
                destructive = true,
                enabled = !updating,
                onClick = { deleting = true },
            ),
        ),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .consumeWindowInsets(padding)
                .withPagePadding(),
            verticalArrangement = Arrangement.spacedBy(UiTokens.ContentSpacing),
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineSmall,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    AndroidKitThemeTokens.dimensions.settingsPageSectionSpacing,
                ),
            ) {
                ContactActionGroup(
                    label = stringResource(R.string.contact_phone_numbers),
                    values = contact.phoneNumbers,
                    icon = R.drawable.ic_lucide_phone,
                    iconTint = actionColors.phone,
                    actionLabel = { stringResource(R.string.contact_phone_action, it) },
                    onClick = onDialPhone,
                    onCopy = copyValue,
                    onShare = onShare,
                    onDelete = { pendingRemoval = ContactFieldRemoval(ContactField.Phone, it) },
                    enabled = !updating,
                )
                ContactActionGroup(
                    label = stringResource(R.string.contact_email_addresses),
                    values = contact.emailAddresses,
                    icon = R.drawable.ic_lucide_mail,
                    iconTint = actionColors.email,
                    actionLabel = { stringResource(R.string.contact_email_action, it) },
                    onClick = onComposeEmail,
                    onCopy = copyValue,
                    onShare = onShare,
                    onDelete = { pendingRemoval = ContactFieldRemoval(ContactField.Email, it) },
                    enabled = !updating,
                )
                ContactActionGroup(
                    label = stringResource(R.string.contact_websites),
                    values = contact.websites,
                    icon = R.drawable.ic_lucide_external_link,
                    iconTint = actionColors.website,
                    actionLabel = { stringResource(R.string.contact_website_action, it) },
                    onClick = onOpenWebsite,
                    onCopy = copyValue,
                    onShare = onShare,
                    onDelete = { pendingRemoval = ContactFieldRemoval(ContactField.Website, it) },
                    enabled = !updating,
                )
                ContactActionGroup(
                    label = stringResource(R.string.contact_addresses),
                    values = contact.addresses,
                    icon = R.drawable.ic_lucide_map_pin,
                    iconTint = actionColors.address,
                    actionLabel = { stringResource(R.string.contact_address_action, it) },
                    onClick = onSearchAddress,
                    onCopy = copyValue,
                    onShare = onShare,
                    onDelete = { pendingRemoval = ContactFieldRemoval(ContactField.Address, it) },
                    enabled = !updating,
                )
                contact.notes?.takeIf(String::isNotBlank)?.let { notes ->
                    AndroidKitSectionCard(
                        title = stringResource(R.string.common_notes),
                        entries = listOf(
                            AndroidKitSectionCardEntry.Multiline(
                                key = "notes",
                                text = notes,
                                contextMenu = contactEntryContextMenu(
                                    onCopy = { copyValue(notes) },
                                    onShare = { onShare(notes) },
                                    onDelete = { pendingRemoval = ContactFieldRemoval(ContactField.Notes, notes) },
                                    enabled = !updating,
                                ),
                            ),
                        ),
                    )
                }
            }
        }
    }

    if (deleting) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.delete_contact_title),
            message = stringResource(R.string.delete_vault_contact_message),
            onDismiss = { deleting = false },
            onConfirm = {
                deleting = false
                onDelete()
            },
        )
    }
    pendingRemoval?.let { removal ->
        ConfirmDeleteDialog(
            title = stringResource(
                when (removal.field) {
                    ContactField.Phone -> R.string.delete_contact_phone_title
                    ContactField.Email -> R.string.delete_contact_email_title
                    ContactField.Website -> R.string.delete_contact_website_title
                    ContactField.Address -> R.string.delete_contact_address_title
                    ContactField.Notes -> R.string.delete_contact_notes_title
                },
            ),
            message = removal.value.takeUnless { removal.field == ContactField.Notes },
            onDismiss = { pendingRemoval = null },
            onConfirm = {
                pendingRemoval = null
                updating = true
                val updated = when (removal.field) {
                    ContactField.Phone -> contact.copy(phoneNumbers = contact.phoneNumbers - removal.value)
                    ContactField.Email -> contact.copy(emailAddresses = contact.emailAddresses - removal.value)
                    ContactField.Website -> contact.copy(websites = contact.websites - removal.value)
                    ContactField.Address -> contact.copy(addresses = contact.addresses - removal.value)
                    ContactField.Notes -> contact.copy(notes = null)
                }
                onUpdate(updated) { updating = false }
            },
        )
    }
}

private enum class ContactField { Phone, Email, Website, Address, Notes }

private data class ContactFieldRemoval(val field: ContactField, val value: String)

@Composable
private fun ContactActionGroup(
    label: String,
    values: List<String>,
    @DrawableRes icon: Int,
    iconTint: Color,
    actionLabel: @Composable (String) -> String,
    onClick: (String) -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDelete: (String) -> Unit,
    enabled: Boolean,
) {
    val nonBlankValues = values.filter(String::isNotBlank)
    AndroidKitSectionCard(
        title = label,
        entries = nonBlankValues.mapIndexed { index, value ->
            val localizedActionLabel = actionLabel(value)
            AndroidKitSectionCardEntry.Action(
                key = "value:$value:${nonBlankValues.take(index).count { it == value }}",
                label = value,
                actionLabel = localizedActionLabel,
                onClick = { onClick(value) },
                trailingIcon = ImageVector.vectorResource(icon),
                trailingIconTint = iconTint,
                contextMenu = contactEntryContextMenu(
                    onCopy = { onCopy(value) },
                    onShare = { onShare(value) },
                    onDelete = { onDelete(value) },
                    enabled = enabled,
                ),
            )
        },
    )
}

@Composable
private fun contactEntryContextMenu(
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean,
): AndroidKitActionFlyoutScope.() -> Unit {
    val copyLabel = stringResource(R.string.common_copy)
    val shareLabel = stringResource(R.string.common_share)
    val deleteLabel = stringResource(R.string.common_delete)
    val copyIcon = ImageVector.vectorResource(R.drawable.ic_lucide_copy)
    val shareIcon = ImageVector.vectorResource(R.drawable.ic_lucide_share_2)
    val deleteIcon = ImageVector.vectorResource(R.drawable.ic_lucide_trash_2)
    return {
        item(label = copyLabel, icon = copyIcon, onClick = onCopy)
        item(label = shareLabel, icon = shareIcon, onClick = onShare)
        item(label = deleteLabel, icon = deleteIcon, enabled = enabled, destructive = true, onClick = onDelete)
    }
}

@Composable
fun ContactEditorScreen(
    existing: VaultContact?,
    onCancel: () -> Unit,
    onSave: (VaultContact, (Boolean) -> Unit) -> Unit,
    onSaved: (UUID) -> Unit,
) {
    val state = rememberEditorState {
        ContactDraft(
            id = existing?.id ?: UUID.randomUUID(),
            name = existing?.name.orEmpty(),
            phoneNumbers = existing?.phoneNumbers.toEditorValues(),
            emailAddresses = existing?.emailAddresses.toEditorValues(),
            websites = existing?.websites.toEditorValues(),
            addresses = existing?.addresses.toEditorValues(),
            notes = existing?.notes.orEmpty(),
            updatedAt = existing?.updatedAt ?: Instant.EPOCH,
        )
    }
    val draft = state.value
    val websitesValid = draft.websites.all { it.isBlank() || normalizeWebsite(it) != null }

    AppEditorScaffold(
        title = stringResource(if (existing == null) R.string.new_contact else R.string.edit_contact),
        isDirty = state.isDirty,
        saveEnabled = draft.name.isNotBlank() && websitesValid,
        isSaving = state.isSaving,
        onCancel = onCancel,
        onSave = {
            state.isSaving = true
            onSave(
                VaultContact(
                    id = draft.id,
                    name = draft.name.trim(),
                    phoneNumbers = draft.phoneNumbers.normalizedValues(),
                    emailAddresses = draft.emailAddresses.normalizedValues(),
                    websites = draft.websites.mapNotNull(::normalizeWebsite).deduplicatedValues(),
                    addresses = draft.addresses.normalizedValues(),
                    notes = draft.notes.trim().ifBlank { null },
                    updatedAt = draft.updatedAt,
                    isPinned = existing?.isPinned ?: false,
                ),
            ) { saved ->
                state.isSaving = false
                if (saved) onSaved(draft.id)
            }
        },
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { state.value = draft.copy(name = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.contact_name)) },
            singleLine = true,
        )
        EditorFieldPair(
            first = { modifier ->
                ContactValueEditor(
                    label = stringResource(R.string.contact_phone_numbers),
                    fieldLabel = stringResource(R.string.contact_phone),
                    values = draft.phoneNumbers,
                    onValuesChange = { state.value = draft.copy(phoneNumbers = it) },
                    keyboardType = KeyboardType.Phone,
                    modifier = modifier,
                )
            },
            second = { modifier ->
                ContactValueEditor(
                    label = stringResource(R.string.contact_email_addresses),
                    fieldLabel = stringResource(R.string.contact_email_address),
                    values = draft.emailAddresses,
                    onValuesChange = { state.value = draft.copy(emailAddresses = it) },
                    keyboardType = KeyboardType.Email,
                    modifier = modifier,
                )
            },
        )
        EditorFieldPair(
            first = { modifier ->
                ContactValueEditor(
                    label = stringResource(R.string.contact_websites),
                    fieldLabel = stringResource(R.string.contact_website),
                    values = draft.websites,
                    onValuesChange = { state.value = draft.copy(websites = it) },
                    keyboardType = KeyboardType.Uri,
                    modifier = modifier,
                    invalidValueMessage = stringResource(R.string.invalid_contact_website),
                    isValid = { it.isBlank() || normalizeWebsite(it) != null },
                )
            },
            second = { modifier ->
                ContactValueEditor(
                    label = stringResource(R.string.contact_addresses),
                    fieldLabel = stringResource(R.string.contact_address),
                    values = draft.addresses,
                    onValuesChange = { state.value = draft.copy(addresses = it) },
                    keyboardType = KeyboardType.Text,
                    modifier = modifier,
                    multiline = true,
                )
            },
        )
        OutlinedTextField(
            value = draft.notes,
            onValueChange = { state.value = draft.copy(notes = it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.common_notes)) },
            minLines = 3,
        )
    }
}

@Composable
private fun ContactValueEditor(
    label: String,
    fieldLabel: String,
    values: List<String>,
    onValuesChange: (List<String>) -> Unit,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    multiline: Boolean = false,
    invalidValueMessage: String? = null,
    isValid: (String) -> Boolean = { true },
) {
    var pendingFocusIndex by remember { mutableStateOf<Int?>(null) }
    // Material reserves half the minimized label line height above the outline.
    val labelClearance = with(LocalDensity.current) {
        (MaterialTheme.typography.bodySmall.lineHeight.toPx() / 2).roundToInt()
    }
    val contentPadding = OutlinedTextFieldDefaults.contentPaddingWithoutLabel()
    val layoutDirection = LocalLayoutDirection.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(UiTokens.CompactSpacing),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        values.forEachIndexed { index, value ->
            val valid = isValid(value)
            val focusRequester = remember { FocusRequester() }
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            LaunchedEffect(pendingFocusIndex) {
                if (pendingFocusIndex == index) {
                    focusRequester.requestFocus()
                    pendingFocusIndex = null
                }
            }
            val removeLabel = stringResource(
                R.string.remove_contact_value,
                value.ifBlank { fieldLabel },
            )
            Column(
                modifier = Modifier
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .onSizeChanged {
                        if (!valid && invalidValueMessage != null) {
                            scope.launch { bringIntoViewRequester.bringIntoView() }
                        }
                    },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(UiTokens.CompactSpacing)) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { next ->
                            onValuesChange(values.toMutableList().apply { this[index] = next })
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .alignBy { (it.measuredHeight + labelClearance) / 2 }
                            .semantics {
                                if (!valid && invalidValueMessage != null) error(invalidValueMessage)
                            },
                        label = { Text(fieldLabel) },
                        singleLine = !multiline,
                        minLines = if (multiline) 2 else 1,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        isError = !valid,
                    )
                    IconButton(
                        onClick = {
                            onValuesChange(values.toMutableList().apply { removeAt(index) })
                        },
                        modifier = Modifier
                            .alignBy { it.measuredHeight / 2 }
                            .semantics {
                                contentDescription = removeLabel
                                onClick(label = removeLabel, action = null)
                            },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_x),
                            contentDescription = null,
                        )
                    }
                }
                invalidValueMessage?.takeIf { !valid }?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(
                            start = contentPadding.calculateStartPadding(layoutDirection),
                            end = contentPadding.calculateEndPadding(layoutDirection),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        TextButton(
            onClick = {
                pendingFocusIndex = values.size
                onValuesChange(values + "")
            },
            modifier = Modifier.align(Alignment.Start),
            contentPadding = PaddingValues(
                top = ButtonDefaults.TextButtonContentPadding.calculateTopPadding(),
                bottom = ButtonDefaults.TextButtonContentPadding.calculateBottomPadding(),
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_plus),
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.add_another))
        }
    }
}

private data class ContactDraft(
    val id: UUID,
    val name: String,
    val phoneNumbers: List<String>,
    val emailAddresses: List<String>,
    val websites: List<String>,
    val addresses: List<String>,
    val notes: String,
    val updatedAt: Instant,
)

private fun VaultContact.firstContactValue(): String? = sequenceOf(
    phoneNumbers,
    emailAddresses,
    websites,
    addresses,
).flatten().firstOrNull(String::isNotBlank)

private fun List<String>?.toEditorValues(): List<String> =
    this?.takeIf(List<String>::isNotEmpty) ?: listOf("")

private fun List<String>.normalizedValues(): List<String> =
    map(String::trim).filter(String::isNotEmpty).deduplicatedValues()

private fun List<String>.deduplicatedValues(): List<String> {
    val seen = mutableSetOf<String>()
    return filter { seen.add(it.lowercase(Locale.ROOT)) }
}

private fun normalizeWebsite(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val candidate = if (EXPLICIT_URI_SCHEME.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
    val normalized = runCatching { URI(candidate) }.getOrNull() ?: return null
    return candidate.takeIf {
        normalized.isAbsolute && normalized.host != null &&
            (normalized.scheme.equals("http", ignoreCase = true) ||
                normalized.scheme.equals("https", ignoreCase = true))
    }
}

private val EXPLICIT_URI_SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]*://")
