; Wraps the jpackage app-image folder (dist\MetadataViewer\) into a single
; portable .exe: a proper Win32 GUI-subsystem binary (no console window,
; unlike Warp's runner stub), silent, no installer UI, no admin elevation.
;
; First launch extracts to a per-user local cache folder; later launches
; reuse that cache and skip straight to running the app, same as a typical
; Electron "portable" build.

!define APP_NAME "MetadataViewer"
!define APP_EXE "MetadataViewer.exe"
!define CACHE_DIR "$LOCALAPPDATA\MetadataViewerPortable"

Name "${APP_NAME}"
OutFile "release\MetadataViewer-windows.exe"
SilentInstall silent
RequestExecutionLevel user
Unicode true

Section
  IfFileExists "${CACHE_DIR}\${APP_EXE}" already_extracted
    SetOutPath "${CACHE_DIR}"
    File /r "dist\MetadataViewer\*.*"
  already_extracted:
  Exec '"${CACHE_DIR}\${APP_EXE}"'
SectionEnd
