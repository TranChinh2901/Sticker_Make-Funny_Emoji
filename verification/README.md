# Sticker Studio implementation — 2026-09-07

Figma MCP source: https://www.figma.com/design/q8gFDhLrDsdJ7623gknget/Sticker-maker?node-id=0-1

## Design analysis and implementation

| Figma node | Screen | Implemented behavior |
| --- | --- | --- |
| `335:3051` | Create | Background palettes/pattern, photo picker, text, emoji, outline, brush, layer selection/order/scale, drag position, undo/redo, private local draft, PNG render and cloud save |
| `352:1318` | Collection detail | Private thumbnail, real sticker count, add existing stickers, create new sticker, rename/delete collection, remove sticker from collection |
| `353:3755` | Sticker preview | Preview a saved user PNG, Android share chooser, add to collection; adapted to user-created content |
| `343:8502` | Settings | Premium navigation, welcome-language preference, feedback/copyright-report form, in-app rating feedback, share chooser, data-handling information |
| `347:14158` | My Studio | Existing screen extended with collections and saved-sticker tabs; complete design context unavailable due Figma Starter quota |

Existing fonts, colors, navigation, asset helpers and startup/collection work were reused. Android owns system bars. Figma SVGs were rasterized at 4×; artwork and source URLs are retained locally in `design/figma/studio-assets.json` (the design directory is intentionally ignored by this checkout).

The editor uses the same 512×512 renderer for preview and PNG, with normalized positions and preserved image aspect ratio. The checkerboard belongs to the editor UI and is excluded from transparent exports. The draft survives app restarts; undo history lasts for the current editing session. Imported files are private to the app.

The design's sample ads, AD locks and native-ad row are omitted because no ad SDK or reward verification is configured. Background options are usable without pretending to play an ad. Existing Premium/Unlock screens, catalogue fixtures and local favorites are unchanged functionally. No billing, rewarded-ad unlock, live public catalogue, full localization, WhatsApp sticker-pack provider or published store/legal URL is claimed. Share PNG uses Android's chooser; Share App currently shares text, because there is no configured published store URL. Rating submits private in-app feedback, not a Play Store review.

## Supabase

`sticker_collections`, `stickers`, `collection_stickers`, `user_settings`, `app_feedback`; private buckets `collection-thumbnails` and `sticker-images`. Anonymous Auth supplies a stable guest identity while app data is retained. RLS and composite owner foreign keys prevent cross-user collection links. Stable save IDs allow retry after a lost response. Deleting a collection removes links and keeps the user's stickers. Storage orphan cleanup is not automated.

The configured remote project returned **PGRST205** for `sticker_collections`, `stickers` and `user_settings` on 2026-09-07. Thus cloud CRUD is implemented but not verified against the deployed server. No service-role key was available or embedded in Android.

For the current fresh server, execute [`../supabase/setup.sql`](../supabase/setup.sql) once in Supabase SQL Editor, then enable Anonymous Sign-Ins. For a server with earlier migrations applied, apply only the pending numbered migrations instead of the combined setup file. The combined file is transactional and intentionally does not silently rewrite existing tables.

## Verification

- `./gradlew assembleDebug lintDebug`: passed.
- Six Android instrumentation tests on **emulator-5554, Pixel_7 / Android 17**: passed. Coverage: transparent PNG, exact color round-trip, imported-image aspect ratio/padding, normalized brush position, paint ordering, draft round-trip. Direct invocation uses `adb -s emulator-5554 shell am instrument -w com.stickermaker.funnyemoji.test/androidx.test.runner.AndroidJUnitRunner` after installing app and test APKs.
- SQL migrations executed on isolated PostgreSQL 16 with test-only Auth/Storage stubs. `tests/studio-rls.sql` passed owner isolation, foreign-key isolation, impersonation rejection, image path checks, private settings/feedback/storage, upload ownership and collection deletion semantics. This validates SQL/RLS, not deployed Supabase REST/Storage services.
- Emulator UI: Home → Create, emoji/text/brush edits, undo/redo, Save error and retry availability, draft persistence through force-stop/relaunch, Home → Settings, Home → My Studio error/retry.
- Screenshots: `editor-restored.png`, `settings.png`, `save-error.png`, `studio-error.png`.
- Physical device `a1146e3e` was detected; no physical-device test coverage is claimed.
- The first instrumentation run had two test-directory permission failures; corrected to an isolated writable directory under the target UID. The final six tests passed.

Remaining live checks after schema deployment: create/list/rename/delete collection; upload and reload PNG; add/remove collection membership; save/reload language; user-initiated feedback submission; sharing a downloaded private PNG. These depend on the server schema and anonymous sign-in being enabled.

## Continuation — 2026-09-08

Implemented shared catalogue identities and real name/category/tag filtering; a Favorites tab
in My Studio; a device cache with an outbox for private Supabase favorites and explicit
retry feedback. Offline unfavorites override old server snapshots, including toggles while
a request is in flight. Catalogue records remain the bundled Figma fixtures.

Create and saved-sticker Preview now export PNG via Android's document picker. Create can
export without a cloud connection. A sticker created from a collection is linked to that
collection before the cloud save flow reports completion. Preview can delete a sticker
and its collection links after confirmation. Collection/preview navigation survives
Activity recreation. My Studio's title has enough vertical space to avoid clipping.

New migration: `202609080001_favorites.sql`; also included in `setup.sql` for fresh projects.
Live REST probes on 2026-09-08 still returned `PGRST205` for collections, stickers and
settings. This environment has no Supabase administration connector. No deployment or
successful cloud CRUD is claimed. Figma MCP still reports the Starter-plan call limit;
the implementation reuses the design context and assets retrieved earlier.

Validation for this continuation:

- `testDebugUnitTest assembleDebug lintDebug`: passed (4 new tests, no lint errors).
- All 6 existing rendering instrumentation tests passed on emulator-5554.
- `setup.sql`, existing Studio RLS tests and new `tests/favorites-rls.sql` passed on
  isolated PostgreSQL 16 with test Auth/Storage stubs; no production database was changed.
- Emulator UI verified favorite propagation Home → Search → Studio, filtering `Animal`,
  and favorites surviving force-stop/relaunch.
- Create → Save → Save PNG to device → Android Downloads saved a 512 × 512 PNG;
  downloaded file bytes were identical to the prepared renderer output.
- Screenshots: `favorites-offline.png`, `search-category-results.png`, `export-success.png`.
  Export sample: `exported-sticker.png`. Images are intentionally ignored by this checkout.
- Physical device a1146e3e was detected but was not used for these tests.

After deployment, verify favorites sync/retry across sessions, automatic collection linking,
saved-sticker deletion and private-image export through the real Supabase API.

## My Studio fidelity pass — 2026-09-09

Source exports: `pdf/Sticker maker/Mixed Mode-1.svg` (frame `343:8610`, empty)
and `Mixed Mode-2.svg` (frame `347:14158`, populated). Figma MCP's design-context
request was retried and returned the Starter quota limit. These local Figma SVG
exports are the source for this pass; a current live Figma revision was not available.

My Studio now follows the original overview: four 83.5 × 87 statistic tiles at
x=16/y=100 with 8-unit gaps; the 350 × 72 New Collection banner at (20,207);
My Collections heading; a 350 × 190 empty panel or two-column 167 × 163.22
collection cards with 24-unit corners. The title, statistic artwork, plus/folder,
overflow and navigation glyphs are extracted from the SVG nodes. Browser rendering
preserves SVG masks and image patterns that Cairo rendered incorrectly.
Text and dynamic content use Compose; body fonts are bundled Be Vietnam Pro.

Recent/Favorites/Drafts/Collections counters are interactive and use app data.
The populated design's sample shows a zero Collections counter despite one card;
the app displays the actual collection count. Loading and failure use explicit
states instead of reporting that collections are empty.

`MyStudioVisualTest` renders the production layout at 390 × 844 with empty and
populated fixtures, captures both screenshots, and checks Create Collection,
New Collection, opening a card and the independent overflow action. The fixture
thumbnail is confined to the visual test. No database seeding is involved.
Android system bars are hidden for the comparison; the SVG's simulated iOS bar
is not rendered by the app.

Artifacts: `my-studio-empty.png`, `my-studio-populated.png`,
`my-studio-comparison.png` (Figma export on the left, Compose on the right).
The image files remain local under this checkout's existing ignore rules.

Final validation: `assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest`
passed (4 unit tests, no lint errors). Both fresh APKs were installed on
`emulator-5554`; the complete instrumentation suite passed (7 tests, including
the two-state visual/action check). Final screenshots were inspected side by
side with the browser-rendered SVGs. This pass does not establish physical-device
coverage or remote Supabase CRUD verification.

## New Collection and live Studio persistence — 2026-09-09

Figma MCP successfully returned design context for New Collection `353:3180`
in this pass. The local `New Collection.svg` export supplies the exact vector
image-picker and custom-color glyphs. The sheet now uses Be Vietnam Pro,
#F5FFFD surface, 32dp top corners, left-aligned 20sp title, 58dp name field,
56dp thumbnail, 40dp palette swatches, and 48dp Create/Cancel controls.
The default custom background is #FDF2F8; custom hex colors can be edited.
An unselected thumbnail uses a placeholder rather than persisting Figma's sample photo.
Create requires a nonblank name and waits for image decoding. Success refreshes
My Studio and closes the sheet; failure retains all choices for retry. Dismissal
is blocked during saving, including dragging the sheet away.

The remote tables now respond successfully (HTTP 200), superseding the older
PGRST205 observations above. `StudioCloudTest` passed against the configured
Supabase from emulator-5554: thumbnail and PNG upload/download byte comparison,
collection creation/list/rename, idempotent membership addition, count refresh,
removal, and collection deletion preserving the saved sticker. The test cleaned
its own records and uploaded objects. It is opt-in to prevent ordinary test runs
from writing to a configured server:

```sh
adb -s emulator-5554 shell am instrument -w \
  -e class com.stickermaker.funnyemoji.data.StudioCloudTest \
  -e liveSupabase true \
  com.stickermaker.funnyemoji.test/androidx.test.runner.AndroidJUnitRunner
```

`NewCollectionFlowTest` injects a failing then suspended save into the production
sheet; it checks name trimming, chosen color, disabled empty submit, input retained
on error, blocked Cancel during saving, and exactly one success/dismiss callback.
This fixture test does not access Supabase.

Screenshots: `new-collection-empty.png`, `new-collection-form.png`,
`new-collection-retry.png`, and `new-collection-comparison.png`. The comparison
shows the 390dp Figma artboard beside the emulator's approximately 411dp-wide
Android viewport scaled to the same image width; system bars and responsive
sheet position therefore differ. An empty name disables Create, and the thumbnail
placeholder remains until the user chooses an image. These are functional states,
not the sample thumbnail and enabled button depicted in the static design.

Final checks passed: `assembleDebug assembleDebugAndroidTest lintDebug testDebugUnitTest`
(4 unit tests, no lint errors), 8 ordinary instrumentation tests on emulator-5554,
and the separate opt-in Supabase integration test (1 test). The accessibility
harness refreshes cached nodes and waits for the expected enabled state before
interaction; the earlier failure was a stale disabled flag on an enabled Create
button. Debug logging was removed. Physical-device coverage was not performed.
