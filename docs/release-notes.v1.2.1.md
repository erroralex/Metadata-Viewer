# v1.2.1: Icon fixes

A small patch release fixing the app icon on Windows, following user reports right after v1.2.0 shipped.

## Fixes

### Windows release exe now shows the correct icon
The single-file `MetadataViewer-windows.exe` (packed with NSIS) was showing a generic default icon on
the desktop and taskbar instead of the MV monogram, because the NSIS wrapper script never had an `Icon`
directive set. It now embeds the app icon directly, so the desktop shortcut, taskbar, and window all show
the same MV mark.

### Titlebar icon now matches the taskbar icon exactly
The in-app titlebar previously drew its own small "M" glyph by hand instead of reusing the app's actual
icon. It now renders the same `icon.png` used everywhere else, scaled down, so there's no risk of the two
ever drifting apart again.

### Titlebar title is bolder
The "Metadata Viewer" title text in the custom titlebar is now bold, matching the weight used elsewhere
in the Latent Design System.

## Why v1.2.0's icon looked stale even though the source assets were already fixed
The MV monogram (`icon.ico`/`icon.png`) was actually updated on 2026-08-17, but the v1.2.0 release had
already been tagged and built about an hour earlier that same day, so it shipped with the previous icon
regardless of the NSIS bug above. This release picks up both fixes at once.
