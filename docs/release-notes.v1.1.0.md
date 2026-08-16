# v1.1.0 — The Latent Rework

MetaDataViewer is now a focused, single-screen metadata extractor with a rebuilt parsing engine and a UI
restyled to the Latent Design System. Favorites, the Metadata Scrubber, and Speed Sorter have moved to
[Latent Library](https://github.com/erroralex) — this app now does one thing: drop an image, get its
generation metadata, fast.

## Highlights

### A single-screen extractor
- Removed the sidebar navigation shell (`RootLayout`/`SideNavigation`) along with Favorites, the Metadata
  Scrubber, and Speed Sorter. The app now hosts the extractor directly — no navigation, no other screens.
- The image drop-zone and preview are now one component: drop an image to extract, drop another on top of
  it at any time to replace it, click it for a fullscreen view.
- A settings/about modal (gear icon in the titlebar) carries branding, sponsor links, and a pointer to
  Latent Library for the features that moved.

### A much stronger metadata engine
Ported the parsing engine from Latent Library's backend (stripped of its Spring dependency, since this app
is stateless):
- **ComfyUI:** full node-graph traversal instead of shallow text scraping, including custom node support
  (Power LoRA Loaders, KSampler variations, reroutes).
- **New fields:** Scheduler, Denoise, and Hires. fix are now surfaced as dedicated cards; Distilled CFG
  (flux-guidance) is folded into the CFG field. Model Hash and ControlNet are still extracted for
  A1111/Forge sources and visible via the Raw Metadata inspector, but don't get dedicated cards since no
  other source (ComfyUI/SwarmUI/InvokeAI/NovelAI) ever populates them.
- Five parsing strategies covered by unit tests (ComfyUI, SwarmUI, A1111/Forge, InvokeAI, NovelAI).

### Restyled to the Latent Design System
- New `latent-theme.css`, built from the Latent Design System's actual token values — colors, radii,
  shadows/glows, and type scale, not just a color swap.
- Titlebar rebuilt with a real brand mark (gradient rounded square + glyph, matching Latent's `BrandMark`
  component) and window controls that follow Latent's `IconButton` styling.
- Copy actions (prompts, raw metadata) now confirm with a toast instead of a silent clipboard write.
- Fixed the default JavaFX focus glow showing through on non-interactive elements, a rounded-corner
  rendering artifact on modals, and a couple of stat-card clipping bugs (Steps/CFG/Seed) from hardcoded
  pixel widths.

### Cross-platform build & standalone releases
- `pom.xml` now resolves the `javafx-graphics` classifier per OS instead of being hardcoded to Windows.
- This release ships as a standalone, no-install-required build per OS (Windows/Linux/macOS), packaged via
  `jpackage` with a minimal bundled runtime — no separate Java install needed to run it.

### Repo hygiene
- Added `LICENSE.md` (MIT + Commons Clause) and `CONTRIBUTING.md`.
- Added a tag-triggered GitHub Actions release workflow.

## Known limitations

- Model Hash and ControlNet extraction is A1111/Forge-only for now; other sources will show these fields
  only in the Raw Metadata inspector, not as dedicated cards.
- The macOS build doesn't have a custom app icon yet (ships with jpackage's default).
- No automated UI test coverage — this release was verified by manual builds and launches, not a visual
  regression suite.
