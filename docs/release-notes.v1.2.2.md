# v1.2.2: Portable exe now actually updates

v1.2.1 shipped the right icon inside the release, but anyone who had already run an earlier version of
`MetadataViewer-windows.exe` never saw it: the portable wrapper's local cache didn't check versions, so it
kept launching whatever it had extracted the first time. This release fixes that.

## Fixes

### The portable exe now re-extracts on every new version
`MetadataViewer-windows.exe` self-extracts to a local cache folder on first run and launches from there on
later runs, to avoid re-extracting every launch. That cache folder used to be shared across every version
(`%LOCALAPPDATA%\MetadataViewerPortable\`), so once any version had extracted there, every later download,
including v1.2.1, silently launched the old cached copy instead, old icon, old "About" version, and all.
The cache folder is now per-version (`%LOCALAPPDATA%\MetadataViewerPortable\<version>\`), so a new release
always extracts and runs fresh.

### The "About" dialog no longer shows a stale hardcoded version
The version shown in Settings > About was a hardcoded string that had been stuck at "v1.1.0" since before
v1.2.0. It's now read from the build itself, so it will always match whatever was actually released.

## If you're upgrading from v1.2.1 or earlier
Download the new `MetadataViewer-windows.exe` and run it. If your desktop shortcut still shows the old
icon afterward, that's Windows' own icon cache holding onto a stale thumbnail for that filename; right-click
the shortcut and choose "Refresh" (or sign out and back in) to clear it.
