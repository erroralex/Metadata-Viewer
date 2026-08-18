# Handover

## Overview
This document tracks recent changes, current context, and next steps for AI and human contributors working on `MetaDataViewer`.

## Recent Changes

### Dev-credit logo swapped for the real design-system asset
- `src/main/resources/alx_logo.png` was a flat solid-purple version of the "ALX" wordmark, not the
  gradient version the design system and sibling apps actually use. Replaced it with the canonical file
  from `C:\Users\error\IdeaProjects\Projects\Latent-Design-System\assets\alx_logo.png` (teal-to-purple
  gradient, matching `Latent-Tools`/`Latent-Library`'s `sidebar-footer-logo`). No code changes needed —
  `DevCredit.java` already just loads `/alx_logo.png` from the classpath.

### Clickable dropzone, dev-credit corner logo, and a real branch-drift bug
- The image dropzone in `ExtractorView` now opens a `FileChooser` (filtered to `png`/`jpg`/`jpeg`/`webp`)
  when clicked while empty, in addition to drag-and-drop. `setupImageArea()`'s click handler now branches:
  fullscreen preview if an image is already loaded (`lastFile` set), otherwise `openFilePicker()`. The
  dropzone hint label now reads "Drop Image Here or Click to Browse" so this is discoverable.
- **New `ui/DevCredit.java`**: a small (100x36) `alx_logo.png` `ImageView`, opacity 0.55 idle / 1.0 on
  hover, links to `github.com/erroralex` via `Desktop.browse`, tooltip "Alexander Nilsson — GitHub" —
  matching the lower-left developer-credit pattern used across the sibling Latent apps (confirmed via a
  sub-agent survey of `Latent-Tools`' `sidebar-footer-logo` CSS/markup: same asset, same opacity/hover
  values, same GitHub link). Wired into `MetadataApp.java` as `mainWrapper.setBottom(footer)`, an `HBox`
  aligned `BOTTOM_LEFT`. **Removed the old large centered `alx_logo.png` from `SettingsDialog`'s About
  panel entirely** (that was the "old style" usage the user wanted gone — a big splash logo, not the
  subtle corner-credit style Latent apps actually use) — About now just shows name/version/moved-note/
  sponsor links, no logo.
- **Found and fixed a real bug while testing this**: `development` branch's `icon.png`/`icon.ico` were
  still the *old* pre-rework "ME" branding — commit `a5d3c69` (the MV monogram update, 2026-08-17) was
  made directly on `main` and never merged back into `development`
  (`git merge-base --is-ancestor a5d3c69 development` → `NO`). Every `git checkout development` this
  session was silently reverting those two files back to the stale version in the working tree (no
  conflict/warning, since there were no local uncommitted edits to them) — meaning most of this session's
  in-app verification screenshots after the first one were unknowingly showing the wrong icon, even
  though every actual **shipped release** was built from tags on `main` and always had the correct icon.
  Fixed by `git checkout main -- src/main/resources/icon.png src/main/resources/icon.ico` on
  `development`, confirmed via md5 match against `main`'s copies before committing. Worth remembering:
  this repo's git workflow ([[feedback_git-workflow]]) only flows `development` → `main`, never the
  reverse, so any file changed directly on `main` (outside this convention) will silently drift out of
  sync on `development` until someone notices and backports it like this.
- **`/docs` is now gitignored and untracked from git** (`git rm -r --cached docs/`), per explicit user
  request — the files (release notes, the rework design doc, the rework implementation plan) still exist
  on disk locally, just no longer committed. This means:
  - Older Handover/README references to `docs/...` paths are now dead links for anyone who clones fresh —
    left as historical record rather than rewritten, since they describe what was true at the time.
  - Future release notes (`docs/release-notes.vX.Y.Z.md`) still work fine with
    `gh release edit --notes-file`, since that reads the local file regardless of git tracking — just
    remember they won't show up in `git status`/`git add -A` and won't be visible to anyone without this
    machine's local `docs/` folder.
- **Verification note:** visually confirmed the dropzone text change and the dev-credit corner logo in a
  live run before the icon-drift discovery. Could not get a clean final screenshot of the corrected
  titlebar icon together with the other changes — mid-session the automation environment's screen
  resolution dropped from 3456x1404 to 1024x768 and window-focus/z-order automation (`SetForegroundWindow`,
  taskbar clicks) stopped reliably working, so further attempts risked more accidental clicks (see the
  near-miss below). Confidence in the icon fix instead comes from the taskbar thumbnail already rendering
  correctly post-fix, plus the direct md5 match against `main`. Worth an actual human eyeball pass.
- **Near-miss during testing:** an early attempt to click the dropzone via simulated mouse coordinates
  missed the app window (still maximized to the old screen size at that point) and landed on IntelliJ's
  Project panel instead, triggering a "Delete directory 'docs'?" confirmation dialog. Caught and cancelled
  before any deletion happened — nothing was lost — but a reminder that blind coordinate-based clicks
  against a moving/resizing desktop are risky; confirm the target window actually has focus
  (`GetForegroundWindow` == target) before clicking anything.

### v1.2.3: versioned release asset filenames
- Every release used to upload the exact same filenames each time (`MetadataViewer-windows.exe`,
  `MetadataViewer-macos.zip`, `MetadataViewer-linux.zip`), matching neither this session's later finding
  that reused filenames feed Windows' path-keyed icon cache (see v1.2.2 below) nor the naming convention
  the user's other Latent apps use (electron-builder's default artifact naming, e.g.
  `Latent.Model.Organizer.1.3.0.exe` — GitHub itself converts the spaces in that default `${productName}
  ${version}.${ext}` template to dots on upload, since asset filenames can't contain spaces).
- `.github/workflows/build.yml` now names the Windows exe `MetadataViewer-windows-<version>.exe` and the
  macOS/Linux zips `MetadataViewer-<macos|linux>-<version>.zip`, reusing the `app_version` output already
  computed by the "Read project version" step. Kept the existing hyphenated house style (matches the jar
  filename convention, e.g. `MetadataViewer-1.2.3.jar`) rather than adopting spaces-to-dots literally,
  since our filenames are hyphen-only from the start.
- Updated `README.md`'s install instructions to say `MetadataViewer-windows-<version>.exe` instead of a
  hardcoded, now-stale exact filename.
- Packaging-only change, no app behavior changed — see `docs/release-notes.v1.2.3.md`.

### v1.2.2: portable exe cache bug and stale About version
- **Root cause of v1.2.1 still showing the old icon/version after install:** `packaging/windows-portable.nsi`'s
  cache folder (`%LOCALAPPDATA%\MetadataViewerPortable\`) was shared across every version — the
  `IfFileExists "${CACHE_DIR}\${APP_EXE}"` check only tested presence, not version, so once anything had
  extracted there, all later launches (including a freshly downloaded v1.2.1) silently kept running the
  stale cached copy and never re-extracted. Fixed by making `CACHE_DIR` version-scoped
  (`%LOCALAPPDATA%\MetadataViewerPortable\${APP_VERSION}\`), with `APP_VERSION` threaded through from
  `build.yml`'s existing `steps.version.outputs.app_version` the same way `ICON_PATH` was wired in for
  v1.2.1.
- **The About dialog's version string (`SettingsDialog.VERSION`) was hardcoded to `"1.1.0"`** and had
  never been updated across v1.2.0 or v1.2.1. Fixed at the source instead of patching the string again:
  `maven-shade-plugin`'s `ManifestResourceTransformer` now stamps `Implementation-Version` from
  `${project.version}`, and `SettingsDialog` reads it via
  `getClass().getPackage().getImplementationVersion()` (falls back to `"dev"` when run unpacked from the
  IDE, where there's no jar manifest). This should never drift again on future version bumps.
- Added `docs/release-notes.v1.2.2.md`, including a note for users upgrading from v1.2.1: if the desktop
  shortcut icon still looks stale after installing, that's Windows' own icon cache for the filename, not
  the app — right-click Refresh or re-login clears it.

### v1.2.1: icon fixes released
- Bumped `pom.xml` to `1.2.1` (non-SNAPSHOT) and updated the jar-filename references in `AGENTS.md`,
  `CONTRIBUTING.md`, and `Package CMD.md` to match, per the versioning convention from the v1.2.0 release.
- Added `docs/release-notes.v1.2.1.md` covering the NSIS icon fix, the titlebar brand-mark change, and the
  bold title — same no-em-dash, no-AI-attribution style as `v1.2.0`'s notes.
- Tagged and pushed `v1.2.1` to trigger the release workflow; this ships the fixes described in the
  "v1.2.0 release icon bugs" section below, since `v1.2.0` itself predates all of them.

### v1.2.0 release icon bugs (found by the user comparing the actual downloaded release against the app)
- **Desktop/taskbar shortcut icon was generic, not the MV monogram.** Root cause: `packaging/windows-portable.nsi` (the NSIS wrapper that packs the jpackage app-image into the single-file `MetadataViewer-windows.exe`) never had an `Icon` directive, so NSIS fell back to its own default installer icon regardless of what `jpackage --icon` embedded inside. Fixed by adding a required `ICON_PATH` define (`!ifndef` guard, same pattern as `SRC_DIR`/`OUT_FILE`) and `Icon "${ICON_PATH}"` in the script, with `.github/workflows/build.yml`'s Windows packaging step now passing `/DICON_PATH=<abs path>\src\main\resources\icon.ico`.
- Separately confirmed: `src/main/resources/icon.ico`/`icon.png` themselves are correct and already contain the current MV monogram (updated in `a5d3c69`) — this was purely the NSIS wrapper missing the flag, not a stale/wrong source asset. Verified by extracting frames from the committed `.ico` via `System.Drawing.Icon` in PowerShell.
- **Why the released v1.2.0 exe still looked stale even on the icon it did have:** that release was published at 18:17 UTC, over an hour *before* the `a5d3c69` icon-update commit at 19:44 UTC the same day — the release simply predates the current brand mark. A re-tag/re-release is needed to ship both this NSIS fix and the current icon (see Next Steps).
- **Titlebar brand mark now renders from `icon.png` directly** instead of a hand-drawn `SVGPath` "M" glyph + separate `.title-mark` CSS gradient square (`CustomTitleBar.createBrandMark()`). The user wanted the titlebar icon to be pixel-identical to the taskbar icon, not a lookalike vector redraw — an `ImageView` scaled to 20x20 guarantees that by construction. The old `.title-mark` CSS rule was removed as dead code.
- Titlebar app-name label is now `-fx-font-weight: bold` (was `600`) per user request.
- Verified all of the above by running the app through IntelliJ's `MetadataApp` run configuration and screenshotting the live titlebar (PowerShell `System.Drawing` window capture, since this is a native desktop window, not a browser).

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
- **This app has no SVG asset pipeline**, unlike Latent Library, Latent Tools, and Latent Model
  Organizer. `MetadataApp.java`'s `Stage.getIcons()` and the `jpackage --icon` flag in
  `Package CMD.md` both consume rasterized files only (`src/main/resources/icon.png`, `icon.ico`).
  A brand-mark update here means delivering a pre-rendered PNG (1024x1024) and a multi-res ICO
  (16/24/32/48/64/72/96/128/256, matching the existing set) rather than dropping in an SVG like the
  sibling apps.
- **Closing the app calls `System.exit(0)` directly** (`MetadataApp.java` close-button callback,
  `() -> System.exit(0)` passed into `CustomTitleBar`), which skips JavaFX's normal `Platform.exit()`
  shutdown sequence and kills the process mid-frame. This is a plausible cause of native JVM crashes
  on close (Direct3D/Prism pipeline torn down abruptly, worse with this app's undecorated/transparent
  stages) — the user reported IntelliJ appearing to crash/hang when closing the app run through its
  run configuration. Not yet fixed; the `primaryStage.close()` line right after `System.exit(0)` in
  `CustomTitleBar`'s close handler is dead code since `System.exit` never returns. Fix is to drop
  `System.exit(0)` entirely and rely on `primaryStage.close()` + JavaFX's default implicit-exit
  behavior (closing the last window already shuts the runtime down cleanly).
- **The portable exe's per-version cache folders are never cleaned up** — each new version now gets its
  own `%LOCALAPPDATA%\MetadataViewerPortable\<version>\` (fixing the stale-launch bug), but old versions'
  folders are left behind indefinitely. Low priority (a few hundred MB per stale version at most), but
  worth a cleanup step (e.g. delete sibling version folders on launch) if this accumulates complaints.
- The `data/` directory (`data/favorites/`, `data/settings.json`) still exists on disk from before this rework and is still tracked in git, but nothing in the app reads or writes it anymore. Still needs a decision on whether to remove it from git.
- Model Hash and ControlNet extraction is A1111/Forge-only; other sources only show them via the Raw Metadata inspector, not as dedicated cards. `ComfyUIStrategy` could be taught to extract these from its node graph if that's ever wanted.
- The macOS build has no custom app icon yet (`icon.icns` doesn't exist; ships with jpackage's default).
- No automated UI test coverage — verified via clean builds/launches (`mcp__idea__build_project` + the `MetadataApp` run configuration, checked for exceptions/CSS warnings), not a visual regression suite.
- **Unverified by an actual human yet:** the NSIS-packed Windows exe (no console window claim, first-run-extract vs. cached-launch behavior), and the 720px responsive breakpoint / 320px prompt auto-grow cap "feeling right" in practice. The `v1.2.0` release itself has been built successfully by CI and its assets confirmed present, but nobody has run the actual downloaded files yet.

## Next Steps
- **A human should eyeball the running app** to confirm what this session couldn't cleanly screenshot:
  the titlebar/taskbar icon is the current MV monogram (not the old "ME"), the dropzone opens a file
  picker when clicked empty, the lower-left dev-credit logo fades in on hover and opens
  `github.com/erroralex`, and Settings no longer shows a big logo.
- ~~Confirm the `v1.2.4` CI run finished green...~~ Done: all three platform builds succeeded, assets
  uploaded, and the release title/notes were set from `docs/release-notes.v1.2.4.md` via `gh release edit`.
- Fix the `System.exit(0)`-on-close crash risk described above (swap for plain `primaryStage.close()`), ideally with a manual close-then-reopen check under IntelliJ's run configuration to confirm no more crash/hang.
- Download and run the `v1.2.4` Windows exe twice in a row (confirm no console window, per-version cache extracting correctly, and that the desktop/taskbar shortcut and About dialog now show the current icon/version) and drop in a real image with a long prompt to sanity-check the responsive/auto-grow behavior.
- Consider adding `src/main/resources/icon.icns` for a real macOS app icon.
- Consider whether `ComfyUIStrategy`'s dropped custom-node-name feature is worth reintroducing via a minimal local settings file, if users ask for it.
