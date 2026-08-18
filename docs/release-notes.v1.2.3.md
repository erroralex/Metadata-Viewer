# v1.2.3: Versioned release filenames

A packaging-only release: the downloadable assets are now named after their version, matching the
convention used across the other Latent apps.

## Changes

### Release assets now include the version number
Previously every release uploaded the same filenames every time (`MetadataViewer-windows.exe`,
`MetadataViewer-macos.zip`, `MetadataViewer-linux.zip`). That made it easy to end up with a stale file
sitting in a folder with the same name as a newer one, and on Windows it fed directly into the icon-cache
staleness issue from v1.2.2: Explorer caches icons by file path, so a new version landing on the exact
same filename in the exact same folder could still show a leftover cached icon from an older build.

Assets are now named `MetadataViewer-windows-<version>.exe`, `MetadataViewer-macos-<version>.zip`, and
`MetadataViewer-linux-<version>.zip`. Every release gets its own filename, so this can't happen again and
it's obvious at a glance which version a downloaded file actually is.

No functional changes to the app itself in this release.
