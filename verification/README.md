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
