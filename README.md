# AI Metadata Viewer & Extractor

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-Programmatic-4285F4?style=for-the-badge&logo=java&logoColor=white)
![CSS](https://img.shields.io/badge/Design_System-Latent-4FD8D0?style=for-the-badge&logo=css3&logoColor=white)
![Jackson](https://img.shields.io/badge/Engine-Jackson_JSON-2f2f2f?style=for-the-badge&logo=json&logoColor=white)

A high-performance JavaFX desktop application designed to inspect and extract generation metadata across the AI image generation ecosystem. It provides instant extraction, deep ComfyUI node-graph inspection, and raw metadata inspection for artists and developers.

> [!NOTE]
> Favorites Library, Metadata Scrubber, and Speed Sorter have moved to [Latent Library](https://github.com/erroralex). MetaDataViewer is now focused purely as a lightweight, fast, single-screen extractor utility.

---

## ✨ Key Features

* **Universal Compatibility:** Intelligent parsing for **ComfyUI** (API & Workflow graphs), **SwarmUI**, **Automatic1111 / Forge**, **InvokeAI**, **NovelAI**, and **SD-Matrix**.
* **Advanced ComfyUI Engine:**
  * **Graph Traversal:** Resolves connected prompts, samplers, schedulers, models, and LoRAs across complex execution flows.
  * **Custom Node Support:** Recursively extracts parameters from advanced custom nodes (Power LoRA Loaders, KSampler variations, reroutes).
  * **Extended Field Discovery:** Extracts Scheduler, Denoise, Hires. fix, Model Hash, Distilled CFG, and ControlNet parameters.
* **Physical Fallback:** Reads physical image headers to guarantee valid image dimensions and file size even when metadata is missing or malformed.
* **Latent Design System:** Clean, modern dark UI styling matching the Latent ecosystem token specifications.
* **Interactive UI:**
  * **Drag & Drop:** Immediate extraction upon dropping any PNG, JPG, JPEG, or WEBP image.
  * **Fullscreen Preview:** Click any image for a modal, high-res inspection view.
  * **Raw Inspector:** Debug non-standard outputs with a raw text/JSON inspection modal.
* **Lightweight & Cross-Platform:** Programmatic JavaFX (No FXML) with zero startup lag and cross-platform build support.

---

## 🛠️ Technical Architecture

* **Strategy Pattern:** Tool-specific parsing strategies (`ComfyUIStrategy`, `CommonStrategy`, `SwarmUIStrategy`, `InvokeAIStrategy`, `NovelAIStrategy`) with heuristic chunk scoring.
* **Stateless & Portable:** Zero background database or config locking — drop and inspect with zero setup overhead.
* **Technology Stack:** Java 21, JavaFX 21, Jackson, metadata-extractor (Drew Noakes), Ikonli (FontAwesome), SLF4J, JUnit 5.

---

## 🚀 Getting Started

### Prerequisites
* Java 21+

### Build & Run
```bash
mvn clean package
java -jar target/MetadataViewer-1.1.0-SNAPSHOT.jar
```

---

## 📜 License

Distributed under the **MIT License**. Free for personal and commercial use.

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
