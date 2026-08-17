# Handover

## Overview
This document tracks recent changes, current context, and next steps for AI and human contributors working on `MetaDataViewer`.

## Recent Changes

### The Latent rework (design: `docs/latent-rework-2026-08-16.md`, plan: `docs/superpowers/plans/2026-08-16-latent-rework-implementation-plan.md`)
- Removed Favorites, Metadata Scrubber, and Speed Sorter — they now live in Latent Library (`C:\Users\error\IdeaProjects\Projects\Latent-Library`). Removed the sidebar navigation shell (`RootLayout`, `SideNavigation`); the app is now a single-screen extractor hosted directly by `MetadataApp`.
- Ported the metadata-parsing engine (`MetadataService`, `TextParamsParser`, and the 5 `MetadataStrategy` implementations) from Latent-Library's Spring Boot backend, stripped of Spring. `ComfyUIStrategy` gained full node-graph traversal and custom-node support, plus several new result fields (`Scheduler`, `Denoise`, `Hires. fix`, `Model Hash`, `Distilled CFG`, `ControlNet`) — though Model Hash/ControlNet are A1111/Forge-only in practice (see Known issues).
- New `latent-theme.css` built from the actual Latent Design System token values (`C:\Users\error\IdeaProjects\Projects\Latent-Design-System`): colors, radii, shadows/glows, and type scale, not just a color swap. `CustomTitleBar` rebuilt with a real `SVGPath` brand mark (gradient square + "M" glyph) and `IconButton`-styled window buttons.
- `ExtractorView` rebuilt as an image + metadata-panel layout: the drop-zone and image preview are one component (`.image-frame`/`.drop-zone` swap), copy actions confirm via a toast, and the Raw Metadata/Settings modals use `StageStyle.TRANSPARENT` (not `UNDECORATED`) to avoid a rounded-corner rendering artifact.
- Cross-platform Maven build via OS-family profiles selecting the `javafx-graphics` classifier (was hardcoded to `win`). JUnit 5 tests added for all 5 strategy classes + `MetadataService`.

### Repo hygiene & release pipeline
- Added `LICENSE.md` (MIT + Commons Clause) and `CONTRIBUTING.md`.
- Added `.github/workflows/build.yml`: on `windows-latest`/`ubuntu-latest`/`macos-latest` whenever a `v*` tag is pushed. Builds the shaded jar, then packages it via `jpackage --type app-image` (module list computed per-build via `jdeps --print-module-deps` instead of hardcoding the huge `java.se` umbrella module, `--jlink-options` strips debug/headers/man-pages). **Windows** gets wrapped into a genuine single-file `MetadataViewer-windows.exe` via a custom NSIS script (`packaging/windows-portable.nsi`) — silent, GUI-subsystem, no console, self-extracts to `%LOCALAPPDATA%\MetadataViewerPortable\` on first run, launches from cache after. **macOS/Linux** ship as zipped app-image folders.
  - *Why NSIS and not something simpler:* first tried [Warp](https://github.com/dgiagio/warp) — worked, but its Windows runner stub is console-subsystem, so it left an empty cmd window open for the app's whole lifetime. Swapped to NSIS (the same tool class Electron's own "portable" builds use, matching how the user's other apps — Latent Library/Tools/Model Organizer — already behave).
  - *NSIS took six tagged-build iterations to get working* — worth internalizing for next time: an invalid `IfFileExists ... 0` fall-through syntax; the runner image genuinely not shipping NSIS despite docs saying otherwise (`choco install` needed); then a `File: "...\*.*" -> no files found` compile error that survived *multiple* independently-logged-as-correct working-directory fixes (`Set-Location`, then `[Environment]::CurrentDirectory`) before being sidestepped entirely by passing absolute paths into the script via `makensis /DSRC_DIR=... /DOUT_FILE=...`. The actual root cause of the CWD symptom was never confirmed — don't assume "logged as correct" means CWD is actually fine if this breaks again.
  - Getting real CI logs required an authenticated API call (anonymous calls 403 even on this public repo) — reused the existing git-push credential's token via `git credential fill` piped straight into `curl`, never printed/logged.

### Post-rework polish
- Renamed all app-facing text from "Metadata Extractor" to "Metadata Viewer" to match the actual app/repo name.
- Replaced six stale pre-rework screenshots with two current captures (`viewer.png`, `raw_metadata.png`); added a "📸 Interface" section to `README.md` matching the other Latent apps' layout (centered icon, screenshots right after the intro).
- **Responsive layout:** narrow windows were clipping content past both window edges (`ScrollPane` hbar was `NEVER`, so overflow was simply unreachable). Fixed in `ExtractorView`: hbar is now `AS_NEEDED`; the stat-card rows switched from `HBox` to `FlowPane` so cards wrap; a `widthProperty` listener moves the metadata panel out of the side-by-side `HBox` into the outer `VBox` (full-width, stacked under the image) below a 720px breakpoint.
- **Auto-growing prompts:** Positive/Negative Prompt `TextArea`s size themselves to fit content on each image load (`autoSizeTextArea`), from 100px/60px up to a 320px cap — past that, `TextArea`'s own scrollbar takes over.

### Versioning
- Released as **v1.2.0**, not v1.1.0 — `pom.xml` was still `1.1.0-SNAPSHOT` and had never actually been bumped, and an unrelated older `metadata-v.1.1.0` tag already exists from January under a different tag-naming convention (`metadata-v.X.Y.Z` vs. this project's `vX.Y.Z`). Bumped `pom.xml` to real `1.2.0`, updated jar-filename references in `AGENTS.md`/`CONTRIBUTING.md`/`Package CMD.md`, renamed `docs/release-notes.v1.1.0.md` → `docs/release-notes.v1.2.0.md`. The `v1.1.0` tag this session had created (not the January one) has been deleted.
- Release notes (`docs/release-notes.v1.2.0.md`) have no em dashes or AI-attribution phrasing per user request — keep future edits to that file consistent with that style.

## Known issues / needs attention
- The `data/` directory (`data/favorites/`, `data/settings.json`) still exists on disk from before this rework and is still tracked in git, but nothing in the app reads or writes it anymore. Still needs a decision on whether to remove it from git.
- Model Hash and ControlNet extraction is A1111/Forge-only; other sources only show them via the Raw Metadata inspector, not as dedicated cards. `ComfyUIStrategy` could be taught to extract these from its node graph if that's ever wanted.
- The macOS build has no custom app icon yet (`icon.icns` doesn't exist; ships with jpackage's default).
- No automated UI test coverage — verified via clean builds/launches (`mcp__idea__build_project` + the `MetadataApp` run configuration, checked for exceptions/CSS warnings), not a visual regression suite.
- **Unverified by an actual human yet:** the NSIS-packed Windows exe (no console window claim, first-run-extract vs. cached-launch behavior), and the 720px responsive breakpoint / 320px prompt auto-grow cap "feeling right" in practice. The `v1.2.0` release itself has been built successfully by CI and its assets confirmed present, but nobody has run the actual downloaded files yet.

## Next Steps
- Download and run `MetadataViewer-windows.exe` from the `v1.2.0` release twice in a row (confirm no console window, cache behavior on second run) and drop in a real image with a long prompt to sanity-check the responsive/auto-grow behavior.
- Consider adding `src/main/resources/icon.icns` for a real macOS app icon.
- Consider whether `ComfyUIStrategy`'s dropped custom-node-name feature is worth reintroducing via a minimal local settings file, if users ask for it.
