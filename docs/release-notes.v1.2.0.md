# v1.2.0: The Latent Rework

MetaDataViewer is now a focused, single-screen metadata extractor with a rebuilt parsing engine and a UI
restyled to the Latent Design System. Favorites, the Metadata Scrubber, and Speed Sorter have moved to
[Latent Library](https://github.com/erroralex). This app now does one thing: drop an image, get its
generation metadata, fast.

## Highlights

### A single-screen extractor
- Removed the sidebar navigation shell (`RootLayout`/`SideNavigation`) along with Favorites, the Metadata
  Scrubber, and Speed Sorter. The app now hosts the extractor directly, with no navigation and no other
  screens.
- The image drop-zone and preview are now one component: drop an image to extract, drop another on top of
  it at any time to replace it, click it for a fullscreen view.
- A settings/about modal (gear icon in the titlebar) carries branding, sponsor links, and a pointer to
  Latent Library for the features that moved.

### A much stronger metadata engine
Ported the parsing engine from Latent Library's backend, stripped of its Spring dependency since this app
is stateless:
- **ComfyUI:** full node-graph traversal instead of shallow text scraping, including custom node support
  (Power LoRA Loaders, KSampler variations, reroutes).
- **New fields:** Scheduler, Denoise, and Hires. fix are now surfaced as dedicated cards; Distilled CFG
  (flux-guidance) is folded into the CFG field. Model Hash and ControlNet are still extracted for
  A1111/Forge sources and visible via the Raw Metadata inspector, but don't get dedicated cards since no
  other source (ComfyUI/SwarmUI/InvokeAI/NovelAI) ever populates them.
- Five parsing strategies covered by unit tests (ComfyUI, SwarmUI, A1111/Forge, InvokeAI, NovelAI).

### Restyled to the Latent Design System
- New `latent-theme.css`, built from the Latent Design System's actual token values: colors, radii,
  shadows/glows, and type scale, not just a color swap.
- Titlebar rebuilt with a real brand mark (gradient rounded square + glyph, matching Latent's `BrandMark`
  component) and window controls that follow Latent's `IconButton` styling.
- Copy actions (prompts, raw metadata) now confirm with a toast instead of a silent clipboard write.
- Fixed the default JavaFX focus glow showing through on non-interactive elements, a rounded-corner
  rendering artifact on modals, and a couple of stat-card clipping bugs (Steps/CFG/Seed) from hardcoded
  pixel widths.

### Cross-platform build & standalone releases
- `pom.xml` now resolves the `javafx-graphics` classifier per OS instead of being hardcoded to Windows.
- This release ships as a standalone, no-install-required build per OS, packaged via `jpackage` with a
  minimal bundled runtime, so no separate Java install is needed to run it.
- **Windows:** `MetadataViewer-windows.exe` is a genuinely single-file download; no separate extraction
  step is needed. It's packed with NSIS (the same tool class Electron's "portable" builds use), so it's a
  proper GUI app with no console window: it self-extracts to a local cache on first run and launches
  straight from that cache on later runs.
- **macOS / Linux:** `MetadataViewer-macos.zip` / `MetadataViewer-linux.zip`: extract with a real "Extract
  All" (not just browsing into the zip) before running, since the app needs its `app`/`runtime`
  subfolders alongside the executable.

### Responsive layout & auto-growing prompts
- Narrow windows no longer clip content off both edges: stat cards wrap onto new lines, and the metadata
  panel stacks full-width under the image column below a ~720px window width instead of being squeezed
  into an unreadable sliver.
- The Positive/Negative Prompt boxes now grow to fit the actual prompt on each image load, up to a cap,
  instead of staying a fixed short height regardless of length; past the cap, the box scrolls internally
  and the rest of the panel stays reachable via the page's own scrollbar.

### Repo hygiene
- Added `LICENSE.md` (MIT + Commons Clause) and `CONTRIBUTING.md`.
- Added a tag-triggered GitHub Actions release workflow.
- Renamed all app-facing text from "Metadata Extractor" to "Metadata Viewer" to match the app's actual
  name, and refreshed the README's screenshots to the current UI.

## Known limitations

- Model Hash and ControlNet extraction is A1111/Forge-only for now; other sources will show these fields
  only in the Raw Metadata inspector, not as dedicated cards.
- The macOS build doesn't have a custom app icon yet (ships with jpackage's default).
- No automated UI test coverage; this release was verified by manual builds and launches, not a visual
  regression suite.
