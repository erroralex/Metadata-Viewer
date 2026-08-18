# Contributing to MetaDataViewer

We welcome contributions! Please follow these guidelines.

## Development Setup

1. **Prerequisites:**
    * Java 21+ (JDK)
    * Maven (no wrapper — install it yourself: https://maven.apache.org/install.html)

2. **Build & Run:**
    * `mvn clean package`
    * `java -jar target/MetadataViewer-1.2.4.jar`
    * Run tests with `mvn test`

3. **Standalone native build (optional):** the release workflow (`.github/workflows/build.yml`) packages the jar into a
   self-contained executable via `jpackage` — no local Java required to run it. To reproduce that locally, see
   `Package CMD.md` for the current manual `jpackage` invocation.

See [AGENTS.md](AGENTS.md) for the full engineering rulebook (testing contracts, git conventions, module boundaries, etc.)
that applies to all contributions, human or AI-assisted.

## Code Style

* **Java:** Standard Java conventions, 4-space indentation.
* **UI:** JavaFX is built programmatically (no FXML) — follow the patterns already established in
  `src/main/java/com/nilsson/metadataviewer/ui/`.

## Pull Requests

* Create a feature branch.
* Submit a PR with a clear description of changes.
