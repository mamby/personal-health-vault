# Icon conversion contracts

Run `python test/icons/verify_icons.py` from the repository root. CI runs the
same offline check. Kit also runs the checker regressions with
`python -m unittest discover -s test/icons -p test_verify_icons.py`.

`sources.json` records each converted vector or Compose icon, the original SVG,
its immutable upstream commit URL and its SHA-256. Files in `upstream` are source
artwork, not generated screenshots. The checker compares each element separately;
relative path commands, element order, geometry, viewport, dimensions, fill,
stroke width, caps and joins must agree. Formatting and numeric spelling do not
affect path comparisons. Unsupported SVG constructs fail for explicit review.
The current supported source contract is the 24-unit, two-unit round-stroke
Lucide artwork. Extend reference verification deliberately when converting a
different source format; never approximate it or disable the check.

For a new conversion, download the original SVG from an immutable commit,
record its URL and hash, and register its target in the manifest. Put
`Converted from SVG:` and the revision in the vector comment; existing Lucide
comments and `ic_lucide_` names are also detected. An unregistered converted
vector fails CI. Future changes to the referenced SVG or target fail the check
unless deliberately reviewed against a new source revision.

Kit's manifest protects the common Compose renderer and explicitly records its
intentional Theme/Language artwork and Settings-chevron crop. Changing an
exception or renderer requires reviewing its geometry and rendered appearance
at the original scale before updating its fingerprint. Never update hashes just
to make a failing check pass.

Compare upstream and converted rendering before accepting conversions. This
check verifies structure; it does not claim screenshot or device visual parity.
Keep rendered comparison images outside the repository under
`%USERPROFILE%\.codex\attachments\android-kit`.

Hosts check their own conversions. Consuming Kit icons does not require copying
or reconverting them. Custom host artwork is outside this conversion contract;
Local Events' existing custom icons are unchanged and require an explicit user
request before any artwork migration. The checker has identical standalone
copies in Kit and both hosts so public host CI needs no private checkout.

Licenses for the pinned Lucide sources are in `UPSTREAM_NOTICES.md` where those
sources are present.
