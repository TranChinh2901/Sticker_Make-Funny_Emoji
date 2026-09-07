# Step 1 — Home

Source: https://www.figma.com/design/q8gFDhLrDsdJ7623gknget/Sticker-maker?node-id=335-1861

Implemented with Jetpack Compose: logo, search field, viral banner, category sections,
three-column sticker cards, favorite toggles, and the Home navigation state.

- Design reference: 390 × 844; horizontal padding 20 dp; card gap 12 dp;
  cards 140 dp high; primary #12AD82; background #F8FBFF.
- Fonts: bundled Baloo 2, Poppins and Inter. Licenses: ../fonts/.
- Asset provenance: asset-manifest.json. PNG images are downloaded from Figma;
  SVG originals are retained in assets/ and rasterized at 4× for Android resources.
- Logo viewport: 172.242 × 28 dp, with the source crop from Figma.
- Sticker viewport: 70 × 70 dp; image leaf 56.021 × 70 dp.
- Navigation: 24 dp outer boxes; folder leaf 21.5 dp; settings leaf 21 dp.
- Android draws its own status/navigation bars rather than using the iOS mock status bar.
- Phone layouts scale the 390-unit artboard uniformly with available width (0.8–1.3×),
  preserving the user's font scale. Android's bottom system inset sits outside the
  artboard; the app tab bar retains its designed 84.2-unit height.
- Header: logo at y=52, search at y=100, banner at y=168. Banner heading uses
  a white text stroke; Baloo 2 weight axes are explicitly set to 400/500/600/700.

Scope: local sample data matching the repeated NickNam sticker in the design.
Search filters category/name; favorites survive activity recreation but are not stored
in a database. Explore Now scrolls to the first category. Other destinations display
an informational snackbar and keep Home selected. No backend, ads, payment,
editor, collections, or additional screen implementation is included in this step.

Verification: `./gradlew assembleDebug lintDebug` and emulator visual/interaction checks.

Suggested commit: `feat: implement Figma Home screen with Jetpack Compose`
