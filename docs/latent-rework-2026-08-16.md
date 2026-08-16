# MetaDataViewer Latent Rework — Design

Date: 2026-08-16
Branch: `latent-rework` (off `development`)

## Scope

MetaDataViewer becomes a single-purpose tool: drop an image, see its AI-generation
metadata, in a Latent-styled window.

- Favorites, Scrubber, and Speed Sorter are removed entirely — they now live in
  Latent Library (`C:\Users\error\IdeaProjects\Projects\Latent-Library`).
- No local settings/data persistence remains. The app is fully stateless.
- The sidebar/multi-view navigation shell is removed — there's only one screen now.
- Visual design is fully reimplemented against the Latent Design System
  (`C:\Users\error\IdeaProjects\Projects\Latent-Design-System`) tokens and component
  patterns, not a light reskin.
- The metadata-parsing engine is replaced with the more advanced strategy
  implementations ported from Latent Library's Spring Boot backend.
- The Maven build is made cross-platform (currently locked to the `win`
  `javafx-graphics` classifier).

## Not doing

- Not touching the Latent-Library or Latent-Design-System repos.
- Not adding TestFX or any UI-level test automation.
- Not keeping any settings/data persistence (no last-used folder, no window
  state, nothing) — the removal of Favorites/Speed Sorter's settings needs
  means there's nothing left to persist.
- Not matching any specific `ui_kits/` mockup pixel-for-pixel — none of the
  three existing Latent Design System UI kits (`latent-tools`,
  `latent-library`, `latent-model-organizer`) correspond to this app's shape,
  so the single-screen extractor layout is a new composition built from the
  design system's primitives and tokens, not a copy of an existing mockup.
- Not preserving the old multi-view shell "for future growth" — out of scope;
  can be revisited later if new features are actually added.

## Package layout after rework

```
com.nilsson.metadataviewer
├── Launcher.java, MetadataApp.java          (kept, simplified — no nav shell to wire up)
├── service/
│   ├── MetadataService.java                 (ported from Latent-Library, adapted)
│   ├── TextParamsParser.java                (ported wholesale — replaces the old dual dispatch/duplication)
│   └── strategy/
│       ├── MetadataStrategy.java            (interface unchanged — identical contract)
│       ├── CommonStrategy.java
│       ├── ComfyUIStrategy.java
│       ├── InvokeAIStrategy.java
│       ├── NovelAIStrategy.java
│       └── SwarmUIStrategy.java             (all ported, Spring annotations stripped)
└── ui/
    ├── CustomTitleBar.java, ResizeHelper.java  (kept — undecorated window chrome, restyled)
    └── views/
        └── ExtractorView.java                (rebuilt: image + side panel layout)
```

**Deleted entirely:**
- `model/FavoriteData.java`
- `model/FavoriteRegistry.java`
- `ui/RootLayout.java`
- `ui/SideNavigation.java`
- `ui/views/FavoritesView.java`
- `ui/views/ScrubView.java`
- `ui/views/SpeedSorterView.java`
- `service/parser/TextParamsParser.java` (old implementation — replaced by the ported one at `service/TextParamsParser.java`)
- the old `service/strategy/*` classes (replaced in place by the ported versions)

The `data/` runtime directory (`data/favorites/`, `data/settings.json`) is no
longer created or read by anything; existing on-disk data under `data/` is left
alone (not deleted by the app) but the app no longer touches it.

## Metadata engine port

Source: `Latent-Library\backend\src\main\java\com\nilsson\backend\strategy\*`
and `Latent-Library\backend\src\main\java\com\nilsson\backend\service\{MetadataService,TextParamsParser}.java`.

Latent-Library's `TextParamsParser` is the single orchestrator (JSON-format
sniffing + ComfyUI node-graph walking + text-block dispatch). It already
resolves the duplication that exists in MetaDataViewer's current split between
`MetadataService.findKeysRecursively` and the old `service/parser/TextParamsParser`.

Port plan:
- Port `TextParamsParser.java`, `MetadataService.java`, and the 5 strategy
  classes (`CommonStrategy`, `ComfyUIStrategy`, `InvokeAIStrategy`,
  `NovelAIStrategy`, `SwarmUIStrategy`) into MetaDataViewer's
  `com.nilsson.metadataviewer` package tree, adjusting package declarations
  and imports only — logic stays as-is.
- Strip `@Service`, `@Lazy`, and all `org.springframework.*` imports; classes
  become plain `new X()` instances instead of Spring-managed beans.
- `ComfyUIStrategy` and `TextParamsParser` take a `UserDataManager` dependency
  in Latent-Library, used for a "custom node type" feature backed by its
  DB-persisted settings (`userDataManager.getCustomPromptNodes()` /
  `.getCustomLoraNodes()`, `ComfyUIStrategy.java:557-558`). Since
  MetaDataViewer is stateless, always construct with `null` and guard those
  two call sites to fall back to an empty list when `userDataManager == null`.
  This drops only the "user-configured custom node names" niche feature —
  the core graph-traversal logic is untouched.
- Add `slf4j-api` + `slf4j-simple` as new Maven dependencies rather than
  stripping every `log.debug/info/warn/error` call across ~1600 lines of
  ported code.
- `MetadataStrategy` interface is byte-for-byte identical between the two
  codebases (`extract(String, JsonNode, JsonNode, Map<String,String>)` +
  default `parse(String)`) — confirmed, no adaptation needed there.

**New/changed result fields** the ported strategies populate beyond what
`ExtractorView` currently reads (`Model`/`Steps`/`Sampler`/`CFG`/`Seed`/
`Width`/`Height`/`Loras`/`Prompt`/`Negative`): `Scheduler`, `Denoise`,
`Hires. fix`, `Model Hash`, `Distilled CFG`, `ControlNet`. The rebuilt
`ExtractorView` metadata panel should surface these as additional stat
pills/rows, not just the original set.

## Design system port

Source: `Latent-Design-System\tokens\{colors,typography,spacing,effects}.css`
and `Latent-Design-System\components\{forms,feedback,navigation,surfaces,chrome}\*.jsx`.

The Latent Design System is copy-in/reimplement-per-framework by design (per
its own `IMPLEMENTATION_GUIDE.md`) — there is no library dependency to add.

Replace `dark-theme.css` with a new stylesheet built from the token values,
translated to JavaFX CSS. JavaFX doesn't support native CSS custom properties
the same way the web does, so tokens become `-fx-*`-prefixed looked-up colors
defined on a `.root` block (the same pattern the current `dark-theme.css`
already uses with `-app-*` variables) — e.g.:

```css
.root {
    -app-bg-canvas: #0A0A0D;      /* --gray-950 */
    -app-surface-1: #14151B;      /* --gray-850 */
    -app-accent-primary: #4FD8D0; /* --cyan-500 */
    -app-accent-secondary: #9B7EF5; /* --violet-500 */
    -app-text-primary: #F2F3F7;
    -app-radius-lg: 12px;
    /* ...full token set from colors.css/spacing.css/effects.css */
}
```

Reimplement, as JavaFX style classes (not literal ports of the `.jsx` files —
hand-translated per the design system's own guidance), the visual language of:
- **Button** — variant classes (`primary`/`cta`/`secondary`/`ghost`/`danger`)
  matching `Button.jsx`'s padding/radius/color rules per variant.
- **Card** — surface-1 background, subtle border, `radius-lg`, `shadow-card`,
  optional kicker + title header — used for the metadata panel container.
- **Titlebar** — 52px height, translucent surface + blur, border-bottom —
  applied to the existing `CustomTitleBar`.
- Supporting primitives as needed for the metadata panel: text input/field
  styling, badge/status-pill styling for e.g. detected-software indicator.

Typography: Inter for UI text, JetBrains Mono for the raw-JSON/prompt text
areas, using the token type scale (`--text-h1` 28px down to `--text-caption`
11px) and weights (400–800).

## Layout

`ExtractorView` becomes the entire window content — no sidebar navigation.

- Large image preview on one side (the drop zone doubles as the preview once
  an image is loaded), fullscreen modal preview carried over from the current
  implementation.
- A `Card`-styled metadata panel on the other side: prompt/negative text
  areas, a params grid of stat pills (Model/Steps/Sampler/Scheduler/CFG/Seed/
  Size/Denoise/Hires. fix/ControlNet/Model Hash), and a Loras list.
- Raw-JSON viewer dialog carried over, restyled with the new tokens
  (JetBrains Mono for the JSON body).
- No "Save Favorite" button or any Favorites-related affordance.
- A small settings/about icon button in `CustomTitleBar` (Ikonli, ghost-button
  style) opens a `Dialog`-styled settings modal (new `ui/views/SettingsDialog.java`
  or similar) containing: the `alx_logo.png` dev mark, app name/version, GitHub
  Sponsors and Ko-fi links (`https://github.com/sponsors/erroralex`,
  `https://ko-fi.com/error_alex` — same as `README.md`'s "Support the Project"
  section), and a note/link pointing users to Latent Library for
  Favorites/Scrubber/Speed Sorter now that those features live there. Links
  open via `java.awt.Desktop.browse`. This replaces `SideNavigation`'s old
  lower-logo-area as the alx_logo's new home, now that the sidebar is gone.

## Cross-platform build fix

`pom.xml` currently pins a single `javafx-graphics` dependency with
`<classifier>win</classifier>`, making the build Windows-only. Replace with
OS-family Maven profiles (using `os-maven-plugin` to detect
`${os.detected.classifier}`) selecting the correct classifier (`win`/`mac`/
`linux`) per build machine, so `mvn package` produces a working build on
whichever OS it's run on. `javafx-swing` and other JavaFX modules are already
classifier-free and unaffected.

## Testing

No test infrastructure currently exists in MetaDataViewer (`src/test/` is
absent). Add JUnit 5 and write unit tests for the ported strategy classes —
they're pure functions (JSON/text in, `Map<String,String>` out), well suited
to isolated testing, and `ComfyUIStrategy` in particular is intricate enough
(~990 lines of graph traversal) to be worth regression coverage. UI classes
(`ExtractorView`, `CustomTitleBar`, etc.) stay untested — no TestFX, out of
scope for this rework.

## Error handling

Unchanged from current behavior: extraction runs on a background `Task`
(as `ExtractorView` already does), unparseable/missing metadata results in an
empty/placeholder state in the metadata panel rather than a crash. The ported
`TextParamsParser`/`MetadataService` throw `ApplicationException` on malformed
JSON that looked parseable but wasn't — `ApplicationException` is a
Latent-Library-specific exception class; MetaDataViewer will need its own
lightweight equivalent (or catch and convert to a runtime exception locally)
since it won't be porting Latent-Library's `exception` package wholesale.

## Open items for the implementation plan

- Exact JavaFX CSS token file structure (one file vs. split like the source
  token files) — implementation detail, not a design decision.
- Whether `MetadataService`'s image-dimension/file-size helpers duplicate
  anything already in MetaDataViewer's `MetadataService.loadFxImage` — to be
  resolved during porting by diffing the two `MetadataService` classes line
  by line.
