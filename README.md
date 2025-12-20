# AI Metadata Viewer & Extractor

![Java](https://img.shields.io/badge/Java-8-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![JavaFX](https://img.shields.io/badge/JavaFX-Programmatic-4285F4?style=for-the-badge&logo=java&logoColor=white)
![CSS](https://img.shields.io/badge/CSS-Obsidian_Indigo-1572B6?style=for-the-badge&logo=css3&logoColor=white)
![Jackson](https://img.shields.io/badge/Persistence-Jackson_JSON-2f2f2f?style=for-the-badge&logo=json&logoColor=white)

A professional desktop application for extracting, managing, and storing generation parameters from AI-generated images. Designed specifically for high-performance workflows across all major AI interfaces.

This project demonstrates **layered architecture**, **recursive JSON parsing**, and **programmatic UI development** without the use of FXML.

---

## 📸 Interface

| Extractor Portal | Favorites Library |
|:---:|:---:|
| <img src="src/main/resources/screenshots/extractor_view.png" width="400" alt="Extractor View"> | <img src="src/main/resources/screenshots/favorites_view.png" width="400" alt="Favorites Library"> |
| *Drag & Drop Extraction with Preview* | *Persistent Card-Based Library* |

<details>
<summary><b>View More Features</b></summary>
<br>

| Raw JSON Viewer | Save to Favorites |
|:---:|:---:|
| <img src="src/main/resources/screenshots/raw_json.png" width="400" alt="Raw Metadata"> | <img src="src/main/resources/screenshots/save_dialog.png" width="400" alt="Save Dialog"> |
| *Deep Inspection for Complex Workflows* | *Custom Undecorated Themed Dialogs* |

</details>

---

## 📖 About

The **AI Metadata Viewer & Extractor** was developed to solve the "fragmentation problem" in the generative AI ecosystem. With various platforms like ComfyUI, A1111, and SwarmUI storing generation data in vastly different formats—ranging from structured JSON node graphs to raw text chunks—artists often lose track of the exact settings used for their best creations.

This tool bridges that gap by providing a unified, high-performance interface that "just works" regardless of the source platform.

### The Vision
* **Unified Intelligence:** Using a deep-recursive parsing engine, the app identifies models, samplers, and prompts buried within complex, nested JSON structures that standard parsers often miss.
* **Developer-Centric Design:** Built for those who live in their IDEs. The "Obsidian & Indigo" theme provides a low-strain, high-contrast environment, while the **Raw Metadata Inspector** allows for deep-dive debugging of new or custom node outputs.
* **Performance First:** By leveraging a programmatic, "Code-First" JavaFX approach (avoiding the overhead of FXML), the application remains lightweight, launching instantly and handling large image drops with zero lag.
* **Privacy & Persistence:** All data, including the **Favorites Library**, is **stored locally** in human-readable JSON files. **Your prompts and workflows never leave your machine**.

This project serves as a technical showcase of how modern Java desktop applications can be both aesthetically striking and technically robust.

---

## 🛠️ Technical Architecture

The application follows a strict **Model-View-Service** architecture to ensure business logic remains decoupled from the interface.

### Design Patterns & Logic
* **Singleton Pattern:** Provides thread-safe, global access to the `FavoriteRegistry` for persistent storage.
* **Layered Architecture:** Clear separation between UI components, the `MetadataService` logic, and the `FavoriteData` models.
* **Recursive Parsing:** The extraction engine uses a deep-recursive strategy to navigate complex node graphs and find hidden metadata keys.
* **Responsive Binding:** Extensive use of JavaFX property bindings to ensure layout stability and text wrapping during window resizing.

### Technology Stack
* **Core:** Java 8 (Liberica JDK Full recommended)
* **UI Framework:** JavaFX (No FXML, Programmatic Layouts)
* **Metadata Engine:** Metadata Extractor (Drew Noakes)
* **Persistence:** Jackson (JSON Data Binding)
* **Assets:** Ikonli (FontAwesome), Custom "Obsidian & Indigo" CSS

---

## ✨ Key Features

* **Wide Compatibility:** Full support for **ComfyUI**, **SwarmUI**, **A1111**, **Forge**, **Reforge**, **Forge Neo** and **SD-Matrix**.
* **Drag & Drop Workflow:** Instant extraction by dropping images directly into the portal.
* **Favorites System:** Save generation parameters with automatic thumbnail generation for later use.
* **Raw Inspector:** A dedicated raw JSON viewer for debugging complex or non-standard node structures.
* **Modern Theming:** A sleek developer-focused dark theme with custom draggable, undecorated dialogs.

---

## 🚀 Getting Started

To run the application locally:

1.  **Clone the repository**
   
2.  **Build with Maven:**
    ```bash
    mvn clean install
    ```
3.  **Run:**
    ```bash
    mvn javafx:run
    ```

---

## 📂 Project Structure

```
src/main/java/com/nilsson/metadataviewer 
├── model/ # JSON Persistence & Entity Data (FavoriteData) 
├── service/ # Metadata Extraction Logic & Recursive Parsers 
└── ui/ # Pure JavaFX Components (CustomTitleBar, SideNav) 
    └── views/ # Main content screens (Extractor, Favorites)
```

---

## 📜 License

This project is distributed under the **MIT License**.

---
