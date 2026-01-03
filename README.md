# AI Metadata Viewer & Extractor

![Java](https://img.shields.io/badge/Java-8-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-Programmatic-4285F4?style=for-the-badge&logo=java&logoColor=white)
![CSS](https://img.shields.io/badge/CSS-Obsidian_Indigo-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![Jackson](https://img.shields.io/badge/Persistence-Jackson_JSON-2f2f2f?style=for-the-badge&logo=json&logoColor=white)

A high-performance JavaFX desktop application designed to unify generation metadata across the fragmented AI image generation ecosystem. It provides instant extraction, **rapid-fire culling**, **privacy scrubbing**, portable local persistence, and deep-node inspection for professional artists and developers.

---

## 📸 Interface

### Core Workflow
| Extractor Portal | Speed Sorter ⚡ |
|:---:|:---:|
| <img src="src/main/resources/screenshots/extractor_view.png" width="400" alt="Extractor View"> | <img src="src/main/resources/screenshots/speedsorter_view.png" width="400" alt="Speed Sorter"> |
| *Drag & Drop Extraction & Fullscreen Preview* | *Rapid Organization with Keyboard Shortcuts* |

### Library & Privacy
| Favorites Library | Metadata Scrubber |
|:---:|:---:|
| <img src="src/main/resources/screenshots/favorites_view.png" width="400" alt="Favorites Library"> | <img src="src/main/resources/screenshots/scrub_view.png" width="400" alt="Scrubber View"> |
| *Portable Card-Based Persistence* | *Strip EXIF/PNG chunks for privacy* |

<details>
<summary><b>View Advanced Features</b></summary>
<br>

| Raw JSON Viewer | Save to Favorites |
|:---:|:---:|
| <img src="src/main/resources/screenshots/raw_json.png" width="400" alt="Raw Metadata"> | <img src="src/main/resources/screenshots/save_dialog.png" width="400" alt="Save Dialog"> |
| *Deep Inspection for Complex Graphs* | *Themed Undecorated Dialogs* |

</details>

---

## ✨ Key Features

* **Speed Sorting (New):** A high-velocity workflow for organizing large datasets.
    * **Hotkeys 1-5:** Instantly move images to pre-assigned folders.
    * **Spacebar Navigation:** Rapidly skip through sets.
    * **Deep Analysis:** One-click fullscreen mode for checking fine details before sorting.
* **Fully Portable:** The application now stores all favorites and thumbnails in a local `/data` directory. Move the folder to a USB drive or another PC, and your library travels with you.
* **Universal Compatibility:** Intelligent parsing for **ComfyUI** (API & Workflow), **SwarmUI**, **A1111**, **Forge**, **InvokeAI**, **NovelAI**, and **SD-Matrix**.
* **Metadata Scrubbing:** A dedicated view to strip all hidden metadata (Prompts, Workflow, EXIF) and export clean images for safe sharing.
* **Smart Parsing Engine:**
    * **Content-Aware Detection:** Distinguishes between API execution blocks and visual workflow graphs to prevent "N/A" errors.
    * **Deep Recursion:** Identifies custom nodes (e.g., *Power LoRA Loader*, *Qwen*), resolution inputs, and nested JSON structures.
    * **Physical Fallback:** Reads physical file headers to guarantee valid image dimensions even when metadata is missing or malformed.
* **Interactive UI:**
    * **Fullscreen Preview:** Click any thumbnail (Extractor, Sorter, or Scrubber) for a modal, high-res inspection view.
    * **Raw Inspector:** Debug non-standard outputs with a syntax-highlighted JSON viewer.
* **Lightweight Performance:** Programmatic JavaFX (No FXML) ensures near-instant launch times and zero-lag image processing.

---

## 🛠️ Technical Architecture

The application implements a **Model-View-Service** (MVS) architecture to decouple business logic from the interface.

* **Singleton Pattern:** Thread-safe global access to image registries and persistent views.
* **Portable Persistence:** Custom JSON serialization logic that maintains relative paths for a self-contained environment.
* **Heuristic Strategy Pattern:** Adaptive parsing strategies that score metadata chunks to select the most relevant generation data.
* **Reactive Binding:** JavaFX properties ensure real-time UI updates and responsive text wrapping.
* **Technology Stack:** Java 8 (Liberica JDK Full), Jackson (JSON Serialization), Metadata Extractor (Drew Noakes), Ikonli (FontAwesome).

---

## 🚀 Getting Started

[![Download Portable Zip](https://img.shields.io/badge/Download-Portable_Zip-2ea44f?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/erroralex/metadata-viewer/releases/latest)

---

## 📜 License

Distributed under the **MIT License**. Free for personal and commercial use.

---

## 💖 Support the Project

If the **AI Metadata Viewer** has streamlined your workflow, consider supporting its ongoing development. Your contributions help maintain compatibility with new AI platforms and node structures.

[![GitHub Sponsors](https://img.shields.io/badge/Sponsor-GitHub-ea4aaa?style=for-the-badge&logo=github-sponsors)](https://github.com/sponsors/erroralex)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/error_alex)

---

<p align="center">
  <b>Developed by</b><br>
  <img src="src/main/resources/alx_logo.png" width="120" alt="Alexander Nilsson Logo"><br>
  Copyright (c) 2025 Alexander Nilsson
</p>
