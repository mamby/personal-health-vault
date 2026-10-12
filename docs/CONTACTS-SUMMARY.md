# Contacts summary

AndroidKit owns and seals the summary composition and renderer. Contacts passes
typed collection counts, its contact quantity resource and ordered compact/full
active-filter labels. It never assembles summary text or accessibility sentences.
The list supplies its actual sort state to the same Kit renderer; empty content
uses `AndroidKitListSummaryText(summary, sort = sort)`.

The scrolling summary uses matching count, one compact filter label with an
additional-filter count, and the current sort: `6 / 7 contacts · Has phone +1 · Name ↑`.
When matching and overall counts are equal, it uses `7 contacts`. Singular and
empty counts use Android quantity resources. No active filters means no filter
segment. Active filters remain visible even if every contact matches them.

Both counts describe the complete in-memory contact collection, independent of
which rows the lazy list renders. The summary does not describe loading progress.
Filter labels use a stable phone, email, address order; `+N` counts only the other
active filters. Screen readers receive the full matching/overall wording, every
active filter, and the Kit's spoken sort description.

The shared Kit renderer limits summaries to one line with an ellipsis at narrow
widths or larger font scales. The full accessibility description remains intact.
Kit uses Android's `BidiFormatter` to isolate numeric fractions and `+N` in Arabic.
Numbers follow the selected locale's numbering system. The Arabic-digit fixture
uses `ar-u-nu-arab` explicitly; it does not assume a device's default Arabic digits.

## Translation review, 2026-10-12

English is the reference. New summary resources were reviewed in English, French,
and Modern Standard Arabic: `contacts_list_count`, `contacts_list_has_phone`,
`contacts_list_has_email`, and `contacts_list_has_address`.
Existing full filter labels are retained. Spoken result counts, numeric fractions,
additional-filter notation and separators now come only from Kit. Removed their
host-owned resource copies. Kit selects the domain quantity form using the overall
count, preserving `1 / 7 contacts` as plural.
French keeps the existing e-mail vocabulary; Arabic keeps جهات اتصال for contacts.
The compact translations are editorial choices, without native-speaker approval.

Sources checked for the implementation were Android's
[quantity resources](https://developer.android.com/guide/topics/resources/string-resource#Plurals),
[Compose resources](https://developer.android.com/develop/ui/compose/resources),
[bidirectional formatter](https://developer.android.com/reference/android/text/BidiFormatter),
and [text overflow](https://developer.android.com/develop/ui/compose/text/configure-layout).
These establish API behavior, rather than linguistic approval of the new phrases.
