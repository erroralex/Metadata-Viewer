; Wraps the jpackage app-image folder (dist\MetadataViewer\) into a single
; portable .exe: a proper Win32 GUI-subsystem binary (no console window,
; unlike Warp's runner stub), silent, no installer UI, no admin elevation.
;
; First launch extracts to a per-user local cache folder; later launches
; reuse that cache and skip straight to running the app, same as a typical
; Electron "portable" build.

; SRC_DIR and OUT_FILE are passed in via "makensis /DSRC_DIR=<abs path>
; /DOUT_FILE=<abs path>" so neither the File glob nor the installer's own
; output path depend on makensis's working directory — relative paths here
; proved unreliable across repeated CI runs for reasons that didn't trace
; back to an actual missing/wrong CWD.
!ifndef SRC_DIR
  !error "SRC_DIR must be defined, e.g. /DSRC_DIR=C:\path\to\dist\MetadataViewer"
!endif
!ifndef OUT_FILE
  !error "OUT_FILE must be defined, e.g. /DOUT_FILE=C:\path\to\release\MetadataViewer-windows.exe"
!endif

!define APP_NAME "MetadataViewer"
!define APP_EXE "MetadataViewer.exe"
!define CACHE_DIR "$LOCALAPPDATA\MetadataViewerPortable"

Name "${APP_NAME}"
OutFile "${OUT_FILE}"
SilentInstall silent
RequestExecutionLevel user
Unicode true

Section
  IfFileExists "${CACHE_DIR}\${APP_EXE}" already_extracted
    SetOutPath "${CACHE_DIR}"
    File /r "${SRC_DIR}\*.*"
  already_extracted:
  Exec '"${CACHE_DIR}\${APP_EXE}"'
SectionEnd
