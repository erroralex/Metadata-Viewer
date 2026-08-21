<p align="center">
  <img src="src/main/resources/icon.png" width="64" alt="Metadata Viewer Icon">
</p>

# AI Metadata Viewer & Extractor

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-Programmatic-4285F4?style=for-the-badge&logo=java&logoColor=white)
![CSS](https://img.shields.io/badge/Design_System-Latent-4FD8D0?style=for-the-badge&logo=css3&logoColor=white)
![Jackson](https://img.shields.io/badge/Engine-Jackson_JSON-2f2f2f?style=for-the-badge&logo=json&logoColor=white)

A high-performance JavaFX desktop application designed to inspect and extract generation metadata across the AI image generation ecosystem. It provides instant extraction, deep ComfyUI node-graph inspection, and raw metadata inspection for artists and developers.

> [!NOTE]
> Favorites Library, Metadata Scrubber, and Speed Sorter have moved to [Latent Library](https://github.com/erroralex/Latent-Library). Metadata Viewer is now focused purely as a lightweight, fast, single-screen extractor utility.

---

## 📸 Interface

<p align="center">
  <img src="src/main/resources/screenshots/viewer.png" width="800" alt="Metadata Viewer main interface">
  <br>
  <i>Single-screen extractor: drop an image and every generation field populates instantly.</i>
</p>

<p align="center">
  <img src="src/main/resources/screenshots/raw_metadata.png" width="800" alt="Raw Metadata inspector">
  <br>
  <i><b>Raw Metadata:</b> full unparsed output for non-standard or unrecognized sources, with one-click copy.</i>
</p>

---

## 🔐 Stateless, Portable & Private

Built as a lightweight, zero-setup utility for quick, on-demand inspection.

* **100% Offline / No Telemetry:** No network calls, no analytics, no cloud dependency. Your images and prompts never leave your machine.
* **Zero Setup:** No database, no config files, no background service. Drop an image and inspect it — nothing is written to disk.
* **Portable Build:** Ships as a single self-contained executable/app-image with a bundled Java 21 runtime. No system-wide Java installation required.
* **Lightweight:** Programmatic JavaFX (no FXML) keeps startup instant and the footprint small.

---

## ✨ Key Features

* **Universal Compatibility:** Intelligent parsing for **ComfyUI** (API & Workflow graphs), **SwarmUI**, **Automatic1111 / Forge**, **InvokeAI**, and **NovelAI**.
* **Advanced ComfyUI Engine:**
  * **Graph Traversal:** Resolves connected prompts, samplers, schedulers, models, and LoRAs across complex execution flows.
  * **Custom Node Support:** Recursively extracts parameters from advanced custom nodes (Power LoRA Loaders, KSampler variations, reroutes).
  * **Extended Field Discovery:** Surfaces Scheduler, Denoise, and Hires. fix as dedicated fields; Distilled CFG (flux-guidance) is folded into the CFG field. Model Hash and ControlNet are A1111/Forge-only today — still captured, but only visible via the Raw Metadata inspector for other sources.
* **Physical Fallback:** Reads physical image headers to guarantee valid image dimensions and file size even when metadata is missing or malformed.
* **Latent Design System:** Clean, modern dark UI styled against the Latent ecosystem's token specifications — shadows, gradients, and type scale included.
* **Interactive UI:**
  * **Drag & Drop:** Drop any PNG, JPG, JPEG, or WEBP directly onto the image preview for instant extraction — even to replace an image that's already loaded.
  * **Fullscreen Preview:** Click any image for a modal, high-res inspection view.
  * **Raw Inspector:** Debug non-standard outputs with a raw text/JSON inspection modal, with one-click copy.
  * **Copy Feedback:** Prompt and raw-metadata copy actions confirm with a toast instead of a silent clipboard write.
  * **Responsive Layout:** The metadata panel reflows underneath the image on narrower windows instead of clipping, and stat cards wrap to fit. Prompt boxes grow to fit their content, up to a cap, then scroll internally.
* **Lightweight & Cross-Platform:** Programmatic JavaFX (No FXML) with zero startup lag and cross-platform build support.

---

## 💻 System Requirements

* **OS:** Windows 10/11 (64-bit), Linux, or macOS (11+).
* **Memory:** Minimal. A few hundred MB of RAM is sufficient — no database or background indexing.
* **Storage:** ~100MB for the application (bundled Java 21 runtime included).
* **Network:** Not required. Metadata Viewer performs no network calls of any kind.

---

## 🛠️ Technical Architecture

* **Strategy Pattern:** Tool-specific parsing strategies (`ComfyUIStrategy`, `CommonStrategy`, `SwarmUIStrategy`, `InvokeAIStrategy`, `NovelAIStrategy`) with heuristic chunk scoring.
* **Stateless & Portable:** Zero background database or config locking — drop and inspect with zero setup overhead.
* **Technology Stack:** Java 21, JavaFX 21, Jackson, metadata-extractor (Drew Noakes), Ikonli (FontAwesome), SLF4J, JUnit 5.

---

## 🚀 Getting Started

[![Download Latest Release](https://img.shields.io/badge/Download-Latest_Release-2ea44f?style=for-the-badge&logo=github&logoColor=white)](https://github.com/erroralex/Metadata-Viewer/releases/latest)

1.  **Download** the appropriate file for your OS:
    *   **Windows:** `MetadataViewer-windows-X.X.X.exe` — single self-contained file, no installation required.
    *   **Linux:** `MetadataViewer-linux-X.X.X.zip`
    *   **macOS:** `MetadataViewer-macos-X.X.X.zip`
2.  **Extract** the `.zip` (macOS/Linux only — a real "Extract All", not just browsing into the archive) and **run** the app from inside the extracted folder. No installation is required.
3.  **Drop an image** onto the preview to extract its generation metadata instantly.

> **🍎 macOS Users:**
> Because this app is not yet signed with an Apple Developer Certificate, you may see an error saying the app is **"damaged and can't be opened."** This is a standard macOS security message for unsigned apps.
>
> **To fix this:**
> 1. Move the app to your **Applications** folder.
> 2. Open **Terminal**.
> 3. Run the following command to clear the quarantine attribute:
>    ```bash
>    sudo xattr -cr "/Applications/MetadataViewer.app"
>    ```
> 4. You can now open the app normally.

Building from source instead? See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## 📜 License

Distributed under the **MIT License with the Commons Clause**. Free to use, modify, and share. The Commons Clause restricts *selling* the Software or offering it as a paid hosted/consulting service. See [LICENSE.md](LICENSE.md) for the full text.

---

## 💖 Support the Project

If **AI Metadata Viewer** has streamlined your workflow, consider supporting its ongoing development.

[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-GitHub-ea4aaa?style=for-the-badge&logo=github-sponsors)](https://github.com/sponsors/erroralex)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/error_alex)

---

<p align="center">
  <b>Developed by</b><br>
  <img src="src/main/resources/alx_logo.png" width="120" alt="Alexander Nilsson Logo"><br>
  Copyright (c) 2025-2026 Alexander Nilsson
</p>
