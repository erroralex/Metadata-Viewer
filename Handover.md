# Handover

## Overview
This document tracks recent changes, current context, and next steps for AI and human contributors working on `MetaDataViewer`.

## Recent Changes
- Completed the Latent rework on branch `latent-rework` (see `docs/latent-rework-2026-08-16.md` for the design, `docs/superpowers/plans/2026-08-16-latent-rework-implementation-plan.md` for the implementation plan):
  - Removed Favorites, Metadata Scrubber, and Speed Sorter — they now live in Latent Library (`C:\Users\error\IdeaProjects\Projects\Latent-Library`).
  - Removed the sidebar navigation shell (`RootLayout`, `SideNavigation`); the app is now a single-screen extractor hosted directly by `MetadataApp`.
  - Ported the metadata-parsing engine (`MetadataService`, `TextParamsParser`, and the 5 `MetadataStrategy` implementations) from Latent-Library's Spring Boot backend, stripped of Spring — the ComfyUI strategy in particular gained full node-graph traversal, custom-node support (minus the `UserDataManager`-backed user-configurable node names, which needs a settings store this stateless app doesn't have), and several new result fields (`Scheduler`, `Denoise`, `Hires. fix`, `Model Hash`, `Distilled CFG`, `ControlNet`).
  - Replaced `dark-theme.css` with `latent-theme.css`, built from the Latent Design System's token values (`C:\Users\error\IdeaProjects\Projects\Latent-Design-System`). Fixed JavaFX CSS custom variable resolution on `-fx-border-radius` and `-fx-background-radius` by using direct pixel lengths to prevent runtime `ClassCastException` warnings.
  - Rebuilt `ExtractorView` as an image + metadata-panel layout.
  - Added a settings/about modal (`SettingsDialog`, opened from a titlebar icon) carrying the `alx_logo` branding, sponsor links, and a pointer to Latent Library for the removed features.
  - Made the Maven build cross-platform via OS-family profiles selecting the `javafx-graphics` classifier, instead of a hardcoded `win` classifier.
  - Added JUnit 5 and unit tests for the ported strategy/service classes (11/11 tests pass).
  - Updated `README.md` with current Java 21 badges, single-screen feature overview, and references to Latent Library.

## Known issues / needs attention
- The `data/` directory (`data/favorites/`, `data/settings.json`) still exists on disk from before this rework and is still tracked in git, but nothing in the app reads or writes it anymore. Still needs a decision on whether to remove it from git (flagged previously, not yet resolved).

## Next Steps
- Merge `latent-rework` into `development` once reviewed.
- Consider whether `ComfyUIStrategy`'s dropped custom-node-name feature is worth reintroducing via a minimal local settings file, if users ask for it.
