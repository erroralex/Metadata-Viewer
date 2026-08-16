# Latent Rework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rework MetaDataViewer into a single-screen JavaFX metadata extractor, styled with the Latent Design System, running the improved metadata-parsing engine ported from Latent-Library, with Favorites/Scrubber/Speed Sorter removed (they now live in Latent Library).

**Architecture:** Port the plain-Java parsing engine (`MetadataService`, `TextParamsParser`, 5 strategy classes) from `Latent-Library\backend\src\main\java\com\nilsson\backend\{service,strategy}` into `com.nilsson.metadataviewer`, stripping Spring. Delete the Favorites/Scrubber/SpeedSorter view classes and the `RootLayout`/`SideNavigation` shell. Replace `dark-theme.css` with a new stylesheet built from the Latent Design System's token values, reusing the app's existing `-app-*` CSS variable names so restyling doesn't require touching every inline style. Rebuild `ExtractorView` as an image+side-panel layout hosted directly by `MetadataApp`. Add a settings/about modal (opened from the titlebar) carrying the `alx_logo` branding and links, replacing its old home in the deleted sidebar.

**Tech Stack:** Java 21, JavaFX 21.0.1 (programmatic UI, no FXML), Jackson (databind + jsr310), metadata-extractor 2.18.0, Ikonli FontAwesome 12.3.1, Maven (maven-shade-plugin uber-jar), new: slf4j-api + slf4j-simple, JUnit 5 (junit-jupiter).

**Spec:** `docs/latent-rework-2026-08-16.md`

## Global Constraints

- No Spring dependency anywhere — all ported classes become plain instantiated Java, not managed beans.
- No new local settings/data persistence — the app is fully stateless (per spec's "Persistence" decision).
- `pom.xml`'s `javafx-graphics` dependency must resolve a platform-appropriate classifier (`win`/`mac`/`linux`) instead of being hardcoded to `win`.
- Ported `MetadataStrategy` implementations keep the exact same `extract(String, JsonNode, JsonNode, Map<String,String>)` + default `parse(String)` contract — the interface file (`service/strategy/MetadataStrategy.java`) is already identical between the two codebases and needs no changes.
- `ComfyUIStrategy`'s `UserDataManager`-backed custom-node-type feature is dropped (always empty list) — MetaDataViewer has no settings store to back it with.
- Every ported file must have its package declaration changed from `com.nilsson.backend.*` to `com.nilsson.metadataviewer.*` and every `com.nilsson.backend.*` import either removed or replaced — grep each ported file for `com.nilsson.backend` after copying and resolve every hit before moving on.
- Branch: `latent-rework`, created off `development`.

---

### Task 1: Branch, `pom.xml` cross-platform build, and new dependencies

**Files:**
- Modify: `pom.xml`

**Interfaces:**
- Produces: `slf4j-api`/`slf4j-simple` on the compile classpath (used by every ported service/strategy class in Tasks 3–9); `junit-jupiter` on the test classpath (used by every `Test` file in Tasks 3–9); a working `mvn package` on any OS.

- [ ] **Step 1: Create the branch**

```bash
git checkout development
git pull
git checkout -b latent-rework
```

- [ ] **Step 2: Replace the hardcoded `win` classifier with OS-family profiles**

In `pom.xml`, remove this dependency:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
    <version>${javafx.version}</version>
    <classifier>win</classifier>
</dependency>
```

Replace it with a classifier-free reference driven by a property:

```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-graphics</artifactId>
    <version>${javafx.version}</version>
    <classifier>${javafx.platform}</classifier>
</dependency>
```

Add a default property (so a build works even if no profile activates) and the three OS-activated profiles, right after `</properties>` / before `<dependencies>`... actually add the default inside `<properties>`:

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <javafx.version>21.0.1</javafx.version>
    <javafx.platform>win</javafx.platform>
</properties>
```

And add this `<profiles>` block as a sibling of `<properties>`/`<dependencies>`/`<build>` (top-level under `<project>`):

```xml
<profiles>
    <profile>
        <id>windows-profile</id>
        <activation>
            <os><family>windows</family></os>
        </activation>
        <properties>
            <javafx.platform>win</javafx.platform>
        </properties>
    </profile>
    <profile>
        <id>mac-profile</id>
        <activation>
            <os><family>mac</family></os>
        </activation>
        <properties>
            <javafx.platform>mac</javafx.platform>
        </properties>
    </profile>
    <profile>
        <id>linux-profile</id>
        <activation>
            <os><family>unix</family><name>linux</name></os>
        </activation>
        <properties>
            <javafx.platform>linux</javafx.platform>
        </properties>
    </profile>
</profiles>
```

- [ ] **Step 3: Add slf4j and JUnit 5 dependencies**

Add to `<dependencies>`:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.13</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.13</version>
</dependency>
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

Add the surefire plugin to `<build><plugins>` (needed to actually run JUnit 5 tests via `mvn test`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
</plugin>
```

- [ ] **Step 4: Verify the build resolves dependencies and compiles**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`, no errors about missing `javafx-graphics` classifier or unresolvable slf4j/junit artifacts.

- [ ] **Step 5: Commit**

```bash
git add pom.xml
git commit -m "Make javafx-graphics classifier platform-aware, add slf4j and JUnit 5"
```

---

### Task 2: Local `MetadataExtractionException`

**Files:**
- Create: `src/main/java/com/nilsson/metadataviewer/service/MetadataExtractionException.java`

**Interfaces:**
- Produces: `MetadataExtractionException(String message)` and `MetadataExtractionException(String message, Throwable cause)`, both unchecked (`extends RuntimeException`). Used by Task 9 (`MetadataService`) and Task 8 (`TextParamsParser`) in place of Latent-Library's `ApplicationException`/`ResourceNotFoundException`, which live in a `com.nilsson.backend.exception` package this project doesn't have.

- [ ] **Step 1: Write the class**

```java
package com.nilsson.metadataviewer.service;

public class MetadataExtractionException extends RuntimeException {
    public MetadataExtractionException(String message) {
        super(message);
    }

    public MetadataExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/MetadataExtractionException.java
git commit -m "Add local MetadataExtractionException for ported metadata engine"
```

---

### Task 3: Port `CommonStrategy`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/service/strategy/CommonStrategy.java` (overwritten with the ported version)
- Test: `src/test/java/com/nilsson/metadataviewer/service/strategy/CommonStrategyTest.java`

**Interfaces:**
- Consumes: `MetadataStrategy` interface (unchanged, already present at `service/strategy/MetadataStrategy.java`).
- Produces: `new CommonStrategy()` — a no-arg-constructible `MetadataStrategy` implementation with `.parse(String rawText)` returning a `Map<String,String>` with keys including `Prompt`, `Negative`, `Steps`, `Sampler`, `Scheduler`, `CFG`, `Distilled CFG`, `Seed`, `Width`, `Height`, `Model`, `Model Hash`, `Denoise`, `Hires. fix`, `Loras`, `ControlNet`. Used by Task 9 (`MetadataService`)'s strategy list.

- [ ] **Step 1: Copy the source file**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\strategy\CommonStrategy.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\CommonStrategy.java"
```

- [ ] **Step 2: Fix the package declaration and Spring annotation**

In the copied file, change:
```java
package com.nilsson.backend.strategy;
```
to:
```java
package com.nilsson.metadataviewer.service.strategy;
```

Remove the line `import org.springframework.stereotype.Service;` and the `@Service` annotation directly above `public class CommonStrategy implements MetadataStrategy {`.

- [ ] **Step 3: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\CommonStrategy.java"`
Expected: no output. If there is output, resolve each reference (it will be an import of another ported class — update it to the `com.nilsson.metadataviewer.service.strategy` package instead).

- [ ] **Step 4: Write the failing test**

```java
package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CommonStrategyTest {

    private final CommonStrategy strategy = new CommonStrategy();

    @Test
    void parse_extractsA1111Params() {
        String text = "a photo of a cat\nNegative prompt: blurry, low quality\n"
                + "Steps: 24, Sampler: DPM++ 2M Karras, CFG scale: 6.5, Seed: 987654321, "
                + "Size: 768x1152, Model: sd_xl_base_1.0, Model hash: 31e35c80fc, "
                + "Denoising strength: 0.4, Hires upscale: 2";

        Map<String, String> result = strategy.parse(text);

        assertEquals("24", result.get("Steps"));
        assertEquals("DPM++ 2M Karras", result.get("Sampler"));
        assertEquals("6.5", result.get("CFG"));
        assertEquals("987654321", result.get("Seed"));
        assertEquals("sd_xl_base_1.0", result.get("Model"));
        assertTrue(result.get("Prompt").contains("a photo of a cat"));
        assertTrue(result.get("Negative").contains("blurry"));
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=CommonStrategyTest test`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. (If it fails, read the actual field names `CommonStrategy.parse` produces via a quick debug print and adjust the assertions — the port must not change parsing behavior, only the test's expectations if they were wrong.)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/strategy/CommonStrategy.java src/test/java/com/nilsson/metadataviewer/service/strategy/CommonStrategyTest.java
git commit -m "Port CommonStrategy from Latent-Library"
```

---

### Task 4: Port `SwarmUIStrategy`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/service/strategy/SwarmUIStrategy.java`
- Test: `src/test/java/com/nilsson/metadataviewer/service/strategy/SwarmUIStrategyTest.java`

**Interfaces:**
- Consumes: `MetadataStrategy` interface.
- Produces: `new SwarmUIStrategy()` — `.parse(String rawJson)` returning a `Map<String,String>` for SwarmUI's `sui_image_params` JSON shape (keys: `Model`, `Sampler`, `Prompt`, `Negative`, `CFG`, `Steps`, `Seed`). Used by Task 9's strategy list.

- [ ] **Step 1: Copy the source file**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\strategy\SwarmUIStrategy.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\SwarmUIStrategy.java"
```

- [ ] **Step 2: Fix package declaration and strip Spring**

Change `package com.nilsson.backend.strategy;` to `package com.nilsson.metadataviewer.service.strategy;`. Remove `import org.springframework.stereotype.Service;` and the `@Service` annotation.

- [ ] **Step 3: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\SwarmUIStrategy.java"`
Expected: no output.

- [ ] **Step 4: Write the failing test**

```java
package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwarmUIStrategyTest {

    private final SwarmUIStrategy strategy = new SwarmUIStrategy();

    @Test
    void parse_extractsSwarmUiParams() {
        String json = """
                {"sui_image_params": {
                    "model": "OfficialStableDiffusion/sd_xl_base_1.0",
                    "sampler": "euler",
                    "prompt": "a lighthouse at dusk",
                    "negativeprompt": "blurry",
                    "cfgscale": 7.0,
                    "steps": 20,
                    "seed": 42
                }}
                """;

        Map<String, String> result = strategy.parse(json);

        assertEquals("euler", result.get("Sampler"));
        assertEquals("a lighthouse at dusk", result.get("Prompt"));
        assertEquals("20", result.get("Steps"));
        assertEquals("42", result.get("Seed"));
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=SwarmUIStrategyTest test`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. Adjust assertions to match actual field/key names only if the port's real output differs from this guess — verify by temporarily printing `result` if needed, then finalize the assertions to what the ported code actually returns.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/strategy/SwarmUIStrategy.java src/test/java/com/nilsson/metadataviewer/service/strategy/SwarmUIStrategyTest.java
git commit -m "Port SwarmUIStrategy from Latent-Library"
```

---

### Task 5: Port `InvokeAIStrategy`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/service/strategy/InvokeAIStrategy.java`
- Test: `src/test/java/com/nilsson/metadataviewer/service/strategy/InvokeAIStrategyTest.java`

**Interfaces:**
- Consumes: `MetadataStrategy` interface.
- Produces: `new InvokeAIStrategy()` — `.parse(String rawJson)` for InvokeAI's metadata JSON shape (keys include `Model`, `Prompt`, `Negative`, `CFG`, `Sampler`, `Scheduler`). Used by Task 9's strategy list.

- [ ] **Step 1: Copy the source file**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\strategy\InvokeAIStrategy.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\InvokeAIStrategy.java"
```

- [ ] **Step 2: Fix package declaration and strip Spring**

Change the package to `com.nilsson.metadataviewer.service.strategy`. Remove the Spring `import` and `@Service` annotation.

- [ ] **Step 3: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\InvokeAIStrategy.java"`
Expected: no output.

- [ ] **Step 4: Write the failing test**

```java
package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvokeAIStrategyTest {

    private final InvokeAIStrategy strategy = new InvokeAIStrategy();

    @Test
    void parse_extractsInvokeAiParams() {
        String json = """
                {
                    "app_version": "4.2.0",
                    "invokeai_metadata": {},
                    "model_name": "sd_xl_base_1.0",
                    "positive_prompt": "a forest clearing",
                    "negative_prompt": "blurry",
                    "cfg_scale": 7.5,
                    "sampler_name": "euler_a",
                    "scheduler": "karras"
                }
                """;

        Map<String, String> result = strategy.parse(json);

        assertEquals("a forest clearing", result.get("Prompt"));
        assertEquals("euler_a", result.get("Sampler"));
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=InvokeAIStrategyTest test`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. If field-name assumptions in the test don't match `InvokeAIStrategy`'s real JSON key expectations, read the ported file's `parse`/`extract` method to find the exact keys it looks for and correct the test's input JSON and assertions to match — don't change the strategy's logic to fit the test.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/strategy/InvokeAIStrategy.java src/test/java/com/nilsson/metadataviewer/service/strategy/InvokeAIStrategyTest.java
git commit -m "Port InvokeAIStrategy from Latent-Library"
```

---

### Task 6: Port `NovelAIStrategy`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/service/strategy/NovelAIStrategy.java`
- Test: `src/test/java/com/nilsson/metadataviewer/service/strategy/NovelAIStrategyTest.java`

**Interfaces:**
- Consumes: `MetadataStrategy` interface.
- Produces: `new NovelAIStrategy()` — `.parse(String rawJson)` for NovelAI's JSON shape (`prompt`, `uc` for negative, `scale` for CFG, `steps`, `seed`, `sampler`). Used by Task 9's strategy list.

- [ ] **Step 1: Copy the source file**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\strategy\NovelAIStrategy.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\NovelAIStrategy.java"
```

- [ ] **Step 2: Fix package declaration and strip Spring**

Change the package to `com.nilsson.metadataviewer.service.strategy`. Remove the Spring `import` and `@Service` annotation.

- [ ] **Step 3: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\NovelAIStrategy.java"`
Expected: no output.

- [ ] **Step 4: Write the failing test**

```java
package com.nilsson.metadataviewer.service.strategy;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NovelAIStrategyTest {

    private final NovelAIStrategy strategy = new NovelAIStrategy();

    @Test
    void parse_extractsNovelAiParams() {
        String json = """
                {
                    "Software": "NovelAI",
                    "prompt": "a shrine in the rain",
                    "uc": "blurry, watermark",
                    "scale": 5.0,
                    "steps": 28,
                    "seed": 555,
                    "sampler": "k_euler_ancestral"
                }
                """;

        Map<String, String> result = strategy.parse(json);

        assertEquals("a shrine in the rain", result.get("Prompt"));
        assertEquals("28", result.get("Steps"));
        assertEquals("555", result.get("Seed"));
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `mvn -q -Dtest=NovelAIStrategyTest test`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. Correct field names in the test against the ported file's actual behavior if they diverge, same rule as prior tasks.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/strategy/NovelAIStrategy.java src/test/java/com/nilsson/metadataviewer/service/strategy/NovelAIStrategyTest.java
git commit -m "Port NovelAIStrategy from Latent-Library"
```

---

### Task 7: Port `ComfyUIStrategy`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/service/strategy/ComfyUIStrategy.java`
- Test: `src/test/java/com/nilsson/metadataviewer/service/strategy/ComfyUIStrategyTest.java`

**Interfaces:**
- Consumes: `MetadataStrategy` interface.
- Produces: `new ComfyUIStrategy()` (single no-arg constructor — the `UserDataManager`-taking constructor from the source is deleted) — implements `extract(String, JsonNode, JsonNode, Map<String,String>)` doing full ComfyUI node-graph traversal. Used by Task 9's strategy list and Task 8's `TextParamsParser`.

- [ ] **Step 1: Copy the source file**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\strategy\ComfyUIStrategy.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\ComfyUIStrategy.java"
```

- [ ] **Step 2: Fix package declaration**

Change `package com.nilsson.backend.strategy;` to `package com.nilsson.metadataviewer.service.strategy;`.

- [ ] **Step 3: Strip Spring and the `UserDataManager` dependency**

Remove these imports:
```java
import com.nilsson.backend.exception.ApplicationException;
import com.nilsson.backend.service.UserDataManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
```
(If `ApplicationException` is actually used somewhere in the 990-line body — check with `grep -n "ApplicationException"` on the copied file first — replace each usage with `com.nilsson.metadataviewer.service.MetadataExtractionException` and add `import com.nilsson.metadataviewer.service.MetadataExtractionException;` instead of removing the import outright.)

Remove the `@Service` annotation and the `userDataManager` field + both constructors:
```java
private final UserDataManager userDataManager;

public ComfyUIStrategy(@Lazy UserDataManager userDataManager) {
    this.userDataManager = userDataManager;
}

public ComfyUIStrategy() {
    this.userDataManager = null;
}
```
Replace with nothing (delete the field and both constructors) — the class now relies on the implicit default no-arg constructor.

Find the two custom-node-list call sites (originally around line 557-558):
```java
List<String> customPromptNodes = userDataManager.getCustomPromptNodes();
List<String> customLoraNodes = userDataManager.getCustomLoraNodes();
```
Replace with:
```java
List<String> customPromptNodes = Collections.emptyList();
List<String> customLoraNodes = Collections.emptyList();
```
(`java.util.Collections` is already covered by the file's existing `import java.util.*;`.)

- [ ] **Step 4: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend\|userDataManager\|UserDataManager" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\strategy\ComfyUIStrategy.java"`
Expected: no output.

- [ ] **Step 5: Verify it compiles**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Write the failing test**

```java
package com.nilsson.metadataviewer.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComfyUIStrategyTest {

    private final ComfyUIStrategy strategy = new ComfyUIStrategy();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void extract_resolvesParamsFromApiSchemaKSampler() throws Exception {
        // Minimal ComfyUI API-schema graph: CLIPTextEncode -> KSampler
        String json = """
                {
                  "1": {
                    "class_type": "CLIPTextEncode",
                    "inputs": { "text": "a neon city at night" }
                  },
                  "2": {
                    "class_type": "KSampler",
                    "inputs": {
                      "seed": 123456789,
                      "steps": 30,
                      "cfg": 7.5,
                      "sampler_name": "dpmpp_2m",
                      "scheduler": "karras",
                      "positive": ["1", 0]
                    }
                  }
                }
                """;

        JsonNode root = mapper.readTree(json);
        Map<String, String> results = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = fields.next();
            JsonNode node = nodeEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> nodeFields = node.fields();
            while (nodeFields.hasNext()) {
                Map.Entry<String, JsonNode> field = nodeFields.next();
                strategy.extract(field.getKey(), field.getValue(), node, results);
            }
        }

        assertEquals("30", results.get("Steps"));
        assertEquals("7.5", results.get("CFG"));
        assertEquals("dpmpp_2m", results.get("Sampler"));
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `mvn -q -Dtest=ComfyUIStrategyTest test`
Expected: `BUILD SUCCESS`, 1 test run, 0 failures. `ComfyUIStrategy` is intricate — if the assertions don't match on the first run, do not weaken them to pass; instead read the relevant extraction method in the ported file (search for `"steps"`, `"cfg"`, `"sampler_name"` in the switch/if chains) to understand what shape of input it actually expects, and adjust the test's input JSON to a shape the strategy is designed to handle, keeping the assertions as real correctness checks.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/strategy/ComfyUIStrategy.java src/test/java/com/nilsson/metadataviewer/service/strategy/ComfyUIStrategyTest.java
git commit -m "Port ComfyUIStrategy from Latent-Library, drop UserDataManager dependency"
```

---

### Task 8: Port `TextParamsParser`, delete the old one

**Files:**
- Create: `src/main/java/com/nilsson/metadataviewer/service/TextParamsParser.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/service/parser/TextParamsParser.java` (and the now-empty `service/parser/` directory)
- Test: `src/test/java/com/nilsson/metadataviewer/service/TextParamsParserTest.java`

**Interfaces:**
- Consumes: `ComfyUIStrategy`, `SwarmUIStrategy`, `CommonStrategy`, `InvokeAIStrategy`, `NovelAIStrategy` (all from Tasks 3–7), `MetadataExtractionException` (Task 2).
- Produces: `new TextParamsParser()` with `.parse(String text)` returning `Map<String,String>`. Used by Task 9's `MetadataService`.

- [ ] **Step 1: Delete the old parser**

```powershell
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\parser\TextParamsParser.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\parser" -Force
```

- [ ] **Step 2: Copy the new source file**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\service\TextParamsParser.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\TextParamsParser.java"
```

- [ ] **Step 3: Fix package, imports, and Spring/UserDataManager**

Change `package com.nilsson.backend.service;` to `package com.nilsson.metadataviewer.service;`.

Change `import com.nilsson.backend.strategy.*;` to `import com.nilsson.metadataviewer.service.strategy.*;`.

Change `import com.nilsson.backend.exception.ApplicationException;` to `import com.nilsson.metadataviewer.service.MetadataExtractionException;`, and change the one throw site:
```java
throw new ApplicationException("Failed to parse image generation metadata from JSON structure.", e);
```
to:
```java
throw new MetadataExtractionException("Failed to parse image generation metadata from JSON structure.", e);
```

Remove `import org.springframework.context.annotation.Lazy;` and `import org.springframework.stereotype.Service;`, and the `@Service` annotation.

Change the constructor from:
```java
public TextParamsParser(@Lazy UserDataManager userDataManager) {
    this.userDataManager = userDataManager;
}
```
to a no-arg constructor that passes `null` through to the `ComfyUIStrategy` it constructs internally — but since Task 7 gave `ComfyUIStrategy` only a no-arg constructor, also change the `private final UserDataManager userDataManager;` field (delete it) and the internal `new ComfyUIStrategy(userDataManager)` call to `new ComfyUIStrategy()`. Delete the whole constructor (the class now uses the implicit default no-arg constructor).

- [ ] **Step 4: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend\|userDataManager\|UserDataManager\|@Lazy" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\TextParamsParser.java"`
Expected: no output.

- [ ] **Step 5: Verify it compiles**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Write the failing test**

```java
package com.nilsson.metadataviewer.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TextParamsParserTest {

    private final TextParamsParser parser = new TextParamsParser();

    @Test
    void parse_detectsAndParsesA1111Text() {
        String text = "a beautiful sunset\nSteps: 20, Sampler: Euler a, CFG scale: 7, Seed: 123";

        Map<String, String> result = parser.parse(text);

        assertFalse(result.isEmpty());
        assertEquals("20", result.get("Steps"));
        assertEquals("Euler a", result.get("Sampler"));
    }

    @Test
    void parse_returnsEmptyMapForUnrecognizedText() {
        Map<String, String> result = parser.parse("just some random text");
        assertTrue(result.isEmpty());
    }

    @Test
    void parse_returnsEmptyMapForNullOrBlank() {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("   ").isEmpty());
    }
}
```

- [ ] **Step 7: Run the test to verify it passes**

Run: `mvn -q -Dtest=TextParamsParserTest test`
Expected: `BUILD SUCCESS`, 3 tests run, 0 failures.

- [ ] **Step 8: Commit**

```bash
git add -A src/main/java/com/nilsson/metadataviewer/service/TextParamsParser.java src/test/java/com/nilsson/metadataviewer/service/TextParamsParserTest.java
git add -A src/main/java/com/nilsson/metadataviewer/service/parser
git commit -m "Port TextParamsParser from Latent-Library, remove old duplicate parser"
```

---

### Task 9: Port `MetadataService`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/service/MetadataService.java` (overwritten with the ported version, `loadFxImage` preserved)
- Test: `src/test/java/com/nilsson/metadataviewer/service/MetadataServiceTest.java`

**Interfaces:**
- Consumes: `CommonStrategy`, `SwarmUIStrategy`, `ComfyUIStrategy`, `InvokeAIStrategy`, `NovelAIStrategy` (Tasks 3–7), `TextParamsParser` (Task 8), `MetadataExtractionException` (Task 2).
- Produces: `new MetadataService()` with `.getExtractedData(File file)` returning `Map<String,String>` (keys: `Prompt`, `Negative`, `Model`, `Software`, `Sampler`, `Scheduler`, `Steps`, `CFG`, `Distilled CFG`, `Seed`, `Resolution`, `FileSize`, `Loras`, `Model Hash`, `Denoise`, `Hires. fix`, `ControlNet`, `Raw`), `.getRawMetadata(File file)` returning `String`, and the existing `static Image loadFxImage(File file)` helper (unchanged, carried over from the pre-port file). Used by Task 14 (`ExtractorView`).

- [ ] **Step 1: Read the current file to preserve `loadFxImage`**

Read `C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\MetadataService.java` and copy its `loadFxImage` static method aside (it's the last method in the file, ~10 lines, using `javax.imageio.ImageIO`, `java.awt.image.BufferedImage`, `javafx.embed.swing.SwingFXUtils`, `javafx.scene.image.Image`) — you'll paste it back in at Step 4.

- [ ] **Step 2: Copy the new source file over the old one**

```powershell
Copy-Item "C:\Users\error\IdeaProjects\Projects\Latent-Library\backend\src\main\java\com\nilsson\backend\service\MetadataService.java" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\MetadataService.java" -Force
```

- [ ] **Step 3: Fix package, imports, Spring, exceptions, and constructor**

Change `package com.nilsson.backend.service;` to `package com.nilsson.metadataviewer.service;`.

Change `import com.nilsson.backend.strategy.ComfyUIStrategy;` and `import com.nilsson.backend.strategy.MetadataStrategy;` to `import com.nilsson.metadataviewer.service.strategy.ComfyUIStrategy;` and `import com.nilsson.metadataviewer.service.strategy.MetadataStrategy;`.

Remove:
```java
import com.nilsson.backend.exception.ApplicationException;
import com.nilsson.backend.exception.ResourceNotFoundException;
import com.nilsson.backend.exception.ValidationException;
import org.springframework.stereotype.Service;
```
Add:
```java
import com.nilsson.metadataviewer.service.strategy.CommonStrategy;
import com.nilsson.metadataviewer.service.strategy.InvokeAIStrategy;
import com.nilsson.metadataviewer.service.strategy.NovelAIStrategy;
import com.nilsson.metadataviewer.service.strategy.SwarmUIStrategy;
```

Remove the `@Service` annotation.

Change the constructor from:
```java
public MetadataService(List<MetadataStrategy> jsonStrategies, TextParamsParser textParamsParser) {
    this.jsonStrategies = jsonStrategies;
    this.textParamsParser = textParamsParser;
}
```
to a no-arg constructor that builds both dependencies itself:
```java
public MetadataService() {
    this.jsonStrategies = List.of(
            new SwarmUIStrategy(),
            new ComfyUIStrategy(),
            new InvokeAIStrategy(),
            new NovelAIStrategy(),
            new CommonStrategy()
    );
    this.textParamsParser = new TextParamsParser();
}
```

Change the one throw site in `getExtractedData`:
```java
throw new ResourceNotFoundException("Image file", file != null ? file.getAbsolutePath() : "null");
```
to:
```java
throw new MetadataExtractionException("Image file not found: " + (file != null ? file.getAbsolutePath() : "null"));
```

- [ ] **Step 4: Re-append `loadFxImage`**

Paste the `loadFxImage` static method you saved in Step 1 back in as the last method in the class, immediately before the file's closing `}`. Add any imports it needs that aren't already present (`javax.imageio.ImageIO` and `java.io.File` are already imported by the ported file; add `java.awt.image.BufferedImage`, `javafx.embed.swing.SwingFXUtils`, and `javafx.scene.image.Image` if missing).

- [ ] **Step 5: Check for leftover `com.nilsson.backend` references**

Run: `grep -n "com.nilsson.backend" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\service\MetadataService.java"`
Expected: no output.

- [ ] **Step 6: Verify it compiles**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Write the failing test**

```java
package com.nilsson.metadataviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MetadataServiceTest {

    private final MetadataService service = new MetadataService();

    @Test
    void getExtractedData_throwsForMissingFile() {
        File missing = new File("does-not-exist.png");
        assertThrows(MetadataExtractionException.class, () -> service.getExtractedData(missing));
    }

    @Test
    void processRawMetadata_handlesNullRawData() {
        Map<String, String> result = service.processRawMetadata(null);
        assertEquals("No metadata found in this image.", result.get("Prompt"));
    }

    @Test
    void processRawMetadata_detectsA1111TextBlock(@TempDir Path tempDir) {
        String raw = "a cat on a windowsill\nSteps: 15, Sampler: Euler a, CFG scale: 6, Seed: 1";
        Map<String, String> result = service.processRawMetadata(raw);

        assertEquals("A1111 / Forge", result.get("Software"));
        assertEquals("15", result.get("Steps"));
    }
}
```

- [ ] **Step 8: Run the test to verify it passes**

Run: `mvn -q -Dtest=MetadataServiceTest test`
Expected: `BUILD SUCCESS`, 3 tests run, 0 failures.

- [ ] **Step 9: Run the full test suite**

Run: `mvn -q test`
Expected: `BUILD SUCCESS`, all tests from Tasks 3–9 pass together.

- [ ] **Step 10: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/service/MetadataService.java src/test/java/com/nilsson/metadataviewer/service/MetadataServiceTest.java
git commit -m "Port MetadataService from Latent-Library, preserve loadFxImage helper"
```

---

### Task 10: Delete Favorites, Scrubber, Speed Sorter, and the navigation shell

**Files:**
- Delete: `src/main/java/com/nilsson/metadataviewer/model/FavoriteData.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/model/FavoriteRegistry.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/ui/RootLayout.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/ui/SideNavigation.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/ui/views/FavoritesView.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/ui/views/ScrubView.java`
- Delete: `src/main/java/com/nilsson/metadataviewer/ui/views/SpeedSorterView.java`

**Interfaces:**
- Produces: nothing — this task only removes code. It leaves `MetadataApp.java` and `ExtractorView.java` referencing now-deleted classes (`RootLayout`, `FavoriteData`, `FavoriteRegistry`); those are fixed in Tasks 14–15. `mvn compile` is expected to **fail** at the end of this task — that failure is checked and accepted here, then fixed in Tasks 14–15.

- [ ] **Step 1: Delete the files**

```powershell
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\model\FavoriteData.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\model\FavoriteRegistry.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\ui\RootLayout.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\ui\SideNavigation.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\ui\views\FavoritesView.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\ui\views\ScrubView.java"
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\ui\views\SpeedSorterView.java"
```

If the `model/` directory is now empty, remove it too:

```powershell
if ((Get-ChildItem "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\model" -Force | Measure-Object).Count -eq 0) {
    Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\java\com\nilsson\metadataviewer\model"
}
```

- [ ] **Step 2: Confirm the expected compile failure**

Run: `mvn -q compile`
Expected: `BUILD FAILURE`, with errors pointing at `MetadataApp.java` (can't find `RootLayout`) and `ExtractorView.java` (can't find `FavoriteData`/`FavoriteRegistry`). This is expected — do not fix it in this task.

- [ ] **Step 3: Commit**

```bash
git add -A src/main/java/com/nilsson/metadataviewer/model src/main/java/com/nilsson/metadataviewer/ui/RootLayout.java src/main/java/com/nilsson/metadataviewer/ui/SideNavigation.java src/main/java/com/nilsson/metadataviewer/ui/views/FavoritesView.java src/main/java/com/nilsson/metadataviewer/ui/views/ScrubView.java src/main/java/com/nilsson/metadataviewer/ui/views/SpeedSorterView.java
git commit -m "Delete Favorites, Scrubber, Speed Sorter, and the nav shell (moved to Latent Library)"
```

---

### Task 11: Latent Design System token stylesheet

**Files:**
- Create: `src/main/resources/latent-theme.css`
- Delete: `src/main/resources/dark-theme.css`

**Interfaces:**
- Produces: a stylesheet defining a `.root` block with the same `-app-*` custom property names the codebase already uses (`-app-bg-main`, `-app-bg-secondary`, `-app-bg-card`, `-app-accent`, `-app-text-primary`, `-app-text-muted`, `-app-border-subtle`, `-app-warning-red`), now sourced from Latent Design System token values, plus the existing style classes (`.content-view`, `.content-title`, `.app-title`, `.custom-title-bar`, `.title-label`, `.window-button`, `.window-close`, `.button`, `.text-area`, `.ikonli-font-icon`, `.drop-zone`, `.drop-zone-active`, `.income-stats-box`, `.income-stat-value`, `.selectable-stat-field`, `.dialog-pane`, `.custom-dialog`) restyled to match. This is consumed by Task 15 (`MetadataApp` loads it instead of `dark-theme.css`) and requires no changes to `CustomTitleBar`/`ExtractorView`'s existing style-class usage or `-app-*` inline style references — only the values change.

- [ ] **Step 1: Write the new stylesheet**

Token source values are from `Latent-Design-System\tokens\{colors,typography,spacing,effects}.css`.

```css
/*
 * METADATA VIEWER - APPLICATION THEME
 * Built from the Latent Design System tokens.
 * Source: C:\Users\error\IdeaProjects\Projects\Latent-Design-System\tokens\*.css
 */

.root {
    /* --- Color Palette (Latent tokens) --- */
    -app-bg-main:       #0A0A0D; /* --color-bg-canvas / --gray-950 */
    -app-bg-secondary:  #0E0F13; /* --color-bg-base */
    -app-bg-card:       #14151B; /* --color-surface-1 / --gray-850 */
    -app-bg-card-raised: #23252F; /* --color-surface-2 / --gray-700 */
    -app-accent:        #4FD8D0; /* --color-accent-primary / --cyan-500 */
    -app-accent-hover:  #67E0D8; /* --cyan-400 */
    -app-accent-secondary: #9B7EF5; /* --color-accent-secondary / --violet-500 */
    -app-text-primary:  #F2F3F7; /* --color-text-primary */
    -app-text-muted:    #9294A3; /* --color-text-secondary / --gray-300 */
    -app-text-tertiary: #6F7180; /* --color-text-tertiary / --gray-400 */
    -app-border-subtle: rgba(255,255,255,0.10); /* --color-border-default */
    -app-warning-red:   #F2665B; /* --color-danger */

    /* --- Radii (Latent tokens) --- */
    -app-radius-sm: 6px;
    -app-radius-md: 8px;
    -app-radius-lg: 12px;

    /* --- JavaFX Overrides --- */
    -fx-background-color: -app-bg-main;
    -fx-base:             -app-bg-main;
    -fx-accent:           -app-accent;
    -fx-default-button:   -app-accent;

    /* --- Global Typography (Latent tokens: --font-sans, --text-body) --- */
    -fx-font-family: "Inter", "Segoe UI", Arial, sans-serif;
    -fx-font-size:   14px;
}

/* 2. LAYOUT CONTAINERS */

.content-view {
    -fx-background-color: -app-bg-card;
    -fx-background:       -app-bg-card;
}

.content-title {
    -fx-text-fill:   -app-accent;
    -fx-font-weight: bold;
    -fx-font-size:   1.5em;
    -fx-padding:     0 0 10 0;
}

.app-title {
    -fx-text-fill:   -app-text-primary;
    -fx-font-weight: bold;
    -fx-font-size:   1.1em;
}

/* 3. WINDOW CHROME (Latent --titlebar-height: 52px) */

.custom-title-bar {
    -fx-background-color: -app-bg-secondary;
    -fx-alignment:        center-left;
    -fx-padding:          0 2 0 0;
    -fx-border-color:     -app-border-subtle;
    -fx-border-width:     0 0 1 0;
}

.title-label {
    -fx-text-fill:   -app-text-primary;
    -fx-font-weight: 600;
    -fx-font-size:   13px;
    -fx-padding:     0 0 0 15;
}

.window-button {
    -fx-background-color: transparent;
    -fx-text-fill:        -app-text-muted;
    -fx-border-color:     transparent;
    -fx-padding:          10 15;
    -fx-background-radius: 0;
    -fx-background-insets: 0;
    -fx-focus-traversable: false;
}

.window-button:hover {
    -fx-background-color: rgba(255, 255, 255, 0.1);
    -fx-text-fill:        -app-text-primary;
}

.button.window-close:hover {
    -fx-background-color: -app-warning-red;
    -fx-text-fill:        white;
}

/* 5. GENERIC CONTROLS (Latent Button.jsx "secondary" variant look) */

.button {
    -fx-background-color: -app-bg-card-raised;
    -fx-text-fill:        -app-text-primary;
    -fx-border-color:     -app-border-subtle;
    -fx-border-radius:    -app-radius-md;
    -fx-background-radius: -app-radius-md;
}

.button:hover {
    -fx-background-color: -app-accent;
    -fx-text-fill:        #06101A; /* --color-text-on-accent */
}

.text-area {
    -fx-control-inner-background: -app-bg-secondary;
    -fx-text-fill:                -app-text-primary;
    -fx-border-color:             -app-border-subtle;
}

.ikonli-font-icon {
    -fx-fill:       -app-accent;
    -fx-icon-color: -app-accent;
}

.button:hover .ikonli-font-icon {
    -fx-fill:       #06101A;
    -fx-icon-color: #06101A;
}

.drop-zone {
    -fx-background-color: rgba(79, 216, 208, 0.05);
    -fx-border-color:     rgba(79, 216, 208, 0.4);
    -fx-border-width:     2;
    -fx-border-style:     dashed;
    -fx-border-radius:    -app-radius-lg;
}

.drop-zone:hover, .drop-zone-active {
    -fx-border-color:     -app-accent;
    -fx-background-color: rgba(79, 216, 208, 0.12);
}

/* 6. DATA DISPLAYS (Latent Card.jsx look: surface-1, subtle border, radius-lg) */

.income-stats-box {
    -fx-background-color: -app-bg-card;
    -fx-background-radius: -app-radius-lg;
    -fx-border-color:     -app-border-subtle;
    -fx-border-radius:    -app-radius-lg;
    -fx-border-width:     1;
    -fx-padding:          15;
}

.income-stat-value {
    -fx-text-fill:   -app-accent;
    -fx-font-size:   1.4em;
    -fx-font-weight: bold;
}

.selectable-stat-field {
    -fx-background-color: transparent;
    -fx-background-insets: 0;
    -fx-padding: 0;
    -fx-text-fill: -app-accent;
    -fx-font-size: 1.4em;
    -fx-font-weight: bold;
}

/* 7. DIALOGS & MODALS */

.dialog-pane {
    -fx-background-color: -app-bg-main;
    -fx-background-radius: -app-radius-lg;
    -fx-background-insets: 0;
    -fx-border-color:  -app-accent;
    -fx-border-width:  1.5;
    -fx-border-radius: -app-radius-lg;
}

.dialog-pane > .container {
    -fx-background-color:  -app-bg-main;
}

.dialog-pane:header .header-panel {
    -fx-background-color:  -app-bg-secondary;
    -fx-padding:           20;
}

.dialog-pane:header .header-panel .label {
    -fx-text-fill:   -app-accent;
    -fx-font-weight: bold;
}

.dialog-pane .text-field {
    -fx-background-color: -app-bg-secondary;
    -fx-text-fill:        -app-text-primary;
    -fx-border-color:     -app-border-subtle;
}

.custom-dialog {
    -fx-background-color:  -app-bg-secondary;
    -fx-border-color:      -app-accent;
    -fx-border-width:      1.5;
    -fx-border-radius:     -app-radius-lg;
    -fx-background-insets: 0;
}

.custom-dialog .text-area {
    -fx-control-inner-background: -app-bg-secondary;
    -fx-text-fill:                -app-text-primary;
    -fx-border-color:             -app-border-subtle;
    -fx-font-family:              'JetBrains Mono', 'Consolas', 'Monospace';
}
```

- [ ] **Step 2: Delete the old stylesheet**

```powershell
Remove-Item "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src\main\resources\dark-theme.css"
```

- [ ] **Step 3: Verify resource is present**

Run: `mvn -q compile` then check the file was copied to the classpath:
```powershell
Test-Path "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\target\classes\latent-theme.css"
```
Expected: `True`.

- [ ] **Step 4: Commit**

```bash
git add -A src/main/resources/latent-theme.css src/main/resources/dark-theme.css
git commit -m "Replace dark-theme.css with a Latent Design System token stylesheet"
```

---

### Task 12: Restyle `CustomTitleBar`, add the settings button

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/ui/CustomTitleBar.java`

**Interfaces:**
- Consumes: nothing new.
- Produces: `CustomTitleBar(Stage primaryStage, Runnable onExitCleanup, Runnable onSettings)` — constructor gains a third parameter. `onSettings.run()` is invoked when the new settings icon button is clicked. Used by Task 15 (`MetadataApp`), which supplies the callback that opens the `SettingsDialog` from Task 13.

- [ ] **Step 1: Change the constructor signature and height**

In `CustomTitleBar.java`, change:
```java
public CustomTitleBar(Stage primaryStage, Runnable onExitCleanup) {
    this.getStyleClass().add("custom-title-bar");
    this.setAlignment(Pos.CENTER_LEFT);
    this.setPrefHeight(40);
```
to:
```java
public CustomTitleBar(Stage primaryStage, Runnable onExitCleanup, Runnable onSettings) {
    this.getStyleClass().add("custom-title-bar");
    this.setAlignment(Pos.CENTER_LEFT);
    this.setPrefHeight(52); // Latent Design System --titlebar-height
```

- [ ] **Step 2: Update the title label text**

Change:
```java
Label titleLabel = new Label("Metadata Extractor by ALX v.1.1.0");
```
to:
```java
Label titleLabel = new Label("Metadata Extractor");
```
(the version number and "by ALX" branding move to the new settings/about modal in Task 13, so it isn't duplicated).

- [ ] **Step 3: Add the settings button**

Change:
```java
Button minimizeBtn = new Button();
```
to add a settings button right before it:
```java
Button settingsBtn = new Button();
settingsBtn.setGraphic(new FontIcon(FontAwesome.COG));
settingsBtn.getStyleClass().add("window-button");
settingsBtn.setOnAction(e -> {
    if (onSettings != null) onSettings.run();
});

Button minimizeBtn = new Button();
```

Change:
```java
this.getChildren().addAll(titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);
```
to:
```java
this.getChildren().addAll(titleLabel, spacer, settingsBtn, minimizeBtn, maximizeBtn, closeBtn);
```

- [ ] **Step 4: Verify it compiles**

Run: `mvn -q compile`
Expected: `BUILD FAILURE` still — `MetadataApp.java` calls the old two-argument `CustomTitleBar` constructor and still references the deleted `RootLayout`. This is expected; fixed in Task 15.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/ui/CustomTitleBar.java
git commit -m "Restyle titlebar to 52px, add settings button"
```

---

### Task 13: `SettingsDialog`

**Files:**
- Create: `src/main/java/com/nilsson/metadataviewer/ui/views/SettingsDialog.java`

**Interfaces:**
- Consumes: `alx_logo.png` (already at `src/main/resources/alx_logo.png` — kept from the pre-rework resources).
- Produces: `SettingsDialog.show(Window owner)` — a static method that builds and shows a modal `Dialog<Void>` styled with the `.custom-dialog` class. Used by Task 15 (`MetadataApp`'s settings callback).

- [ ] **Step 1: Write the class**

```java
package com.nilsson.metadataviewer.ui.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.awt.Desktop;
import java.net.URI;

public class SettingsDialog {

    private static final String VERSION = "1.1.0";
    private static final String SPONSOR_GITHUB_URL = "https://github.com/sponsors/erroralex";
    private static final String SPONSOR_KOFI_URL = "https://ko-fi.com/error_alex";

    private SettingsDialog() {
    }

    public static void show(Window owner) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("custom-dialog");
        if (owner != null && owner.getScene() != null) {
            pane.getStylesheets().addAll(owner.getScene().getStylesheets());
        }
        pane.getButtonTypes().add(ButtonType.CLOSE);

        VBox content = new VBox(15);
        content.setPadding(new Insets(25));
        content.setAlignment(Pos.CENTER);
        content.setPrefWidth(320);

        ImageView logoView = new ImageView();
        if (SettingsDialog.class.getResource("/alx_logo.png") != null) {
            logoView.setImage(new Image(SettingsDialog.class.getResource("/alx_logo.png").toExternalForm()));
        }
        logoView.setFitWidth(120);
        logoView.setPreserveRatio(true);
        logoView.setSmooth(true);

        Label appName = new Label("Metadata Extractor");
        appName.getStyleClass().add("app-title");

        Label version = new Label("v" + VERSION);
        version.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 0.85em;");

        Label movedNote = new Label(
                "Favorites, the Metadata Scrubber, and Speed Sorter have moved to Latent Library.");
        movedNote.setWrapText(true);
        movedNote.setStyle("-fx-text-fill: -app-text-muted; -fx-font-size: 0.85em; -fx-text-alignment: center;");
        movedNote.setAlignment(Pos.CENTER);

        HBox links = new HBox(10);
        links.setAlignment(Pos.CENTER);

        Button sponsorGithubBtn = new Button("GitHub Sponsors");
        sponsorGithubBtn.getStyleClass().add("button");
        sponsorGithubBtn.setOnAction(e -> openLink(SPONSOR_GITHUB_URL));

        Button sponsorKofiBtn = new Button("Ko-fi");
        sponsorKofiBtn.getStyleClass().add("button");
        sponsorKofiBtn.setOnAction(e -> openLink(SPONSOR_KOFI_URL));

        links.getChildren().addAll(sponsorGithubBtn, sponsorKofiBtn);

        content.getChildren().addAll(logoView, appName, version, movedNote, links);
        pane.setContent(content);

        dialog.showAndWait();
    }

    private static void openLink(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `mvn -q compile`
Expected: still `BUILD FAILURE` from the pre-existing `MetadataApp`/`ExtractorView` breakage (Task 10) — but no new errors attributable to `SettingsDialog.java` itself. Confirm by checking the error output only mentions `MetadataApp.java` and `ExtractorView.java`.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/ui/views/SettingsDialog.java
git commit -m "Add settings/about dialog with alx_logo and sponsor links"
```

---

### Task 14: Rebuild `ExtractorView`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/ui/views/ExtractorView.java` (rewritten)

**Interfaces:**
- Consumes: `MetadataService` (Task 9) — `new MetadataService()`, `.getExtractedData(File)` returning `Map<String,String>` with the field set documented in Task 9, `MetadataService.loadFxImage(File)`.
- Produces: `public ExtractorView()` (no-arg, unchanged from before), `public void process(File f)` (unchanged signature — called by Task 15's `MetadataApp` for file-association drops). Removes: `populateFromFavorite(FavoriteData)` (no longer needed — no more Favorites), and the "Save Favorite" button.

- [ ] **Step 1: Remove the Favorites dependency and the Save Favorite button**

Remove these imports:
```java
import com.nilsson.metadataviewer.model.FavoriteData;
import com.nilsson.metadataviewer.model.FavoriteRegistry;
```

Delete the `populateFromFavorite(FavoriteData fav)` method entirely (lines 134-168 in the pre-rework file).

Delete the `createSaveButton()` method entirely (lines 235-281 in the pre-rework file) and its call site in the constructor:
```java
previewWrapper.getChildren().addAll(fullScreenHint, previewContainer, createSaveButton(), createRawButton());
```
becomes:
```java
previewWrapper.getChildren().addAll(fullScreenHint, previewContainer, createRawButton());
```

- [ ] **Step 2: Add fields for the new stat cards**

Change the field block:
```java
private final TextField modelVal = createSelectableField("-");
private final TextField softwareVal = createSelectableField("-");
private final TextField samplerVal = createSelectableField("-");
private final TextField stepsVal = createSelectableField("-");
private final TextField cfgVal = createSelectableField("-");
private final TextField seedVal = createSelectableField("-");
private final TextField sizeVal = createSelectableField("-");
private final TextArea lorasVal = new TextArea("-");
```
to:
```java
private final TextField modelVal = createSelectableField("-");
private final TextField softwareVal = createSelectableField("-");
private final TextField samplerVal = createSelectableField("-");
private final TextField schedulerVal = createSelectableField("-");
private final TextField stepsVal = createSelectableField("-");
private final TextField cfgVal = createSelectableField("-");
private final TextField seedVal = createSelectableField("-");
private final TextField sizeVal = createSelectableField("-");
private final TextField denoiseVal = createSelectableField("-");
private final TextField hiresFixVal = createSelectableField("-");
private final TextField modelHashVal = createSelectableField("-");
private final TextArea lorasVal = new TextArea("-");
private final TextArea controlNetVal = new TextArea("-");
```

- [ ] **Step 3: Restructure the layout into an image + side panel**

Replace the constructor's layout-building section (from `HBox dropSection = new HBox(20);` through the `container.getChildren().addAll(...)` line) with:

```java
HBox mainSplit = new HBox(20);
mainSplit.setAlignment(Pos.TOP_CENTER);

// --- Left: image column (drop zone doubles as preview) ---
VBox imageColumn = new VBox(10);
imageColumn.setAlignment(Pos.TOP_CENTER);
imageColumn.setPrefWidth(360);
imageColumn.setMinWidth(280);

VBox dropZone = createDropZone();

setupPreviewContainer();
previewContainer.setPrefSize(320, 320);
previewImageView.setFitWidth(300);
previewImageView.setFitHeight(300);

Label fullScreenHint = new Label("Click to Fullscreen");
fullScreenHint.setStyle("-fx-font-size: 10px; -fx-text-fill: -app-text-muted;");

imageColumn.getChildren().addAll(dropZone, previewContainer, fullScreenHint, createRawButton());

// --- Right: metadata panel ---
VBox metadataPanel = new VBox(15);
HBox.setHgrow(metadataPanel, Priority.ALWAYS);

VBox promptsWrapper = new VBox(15);
promptsWrapper.getChildren().addAll(
        createPromptSection("Positive Prompt", promptText, 100),
        createPromptSection("Negative Prompt", negativePromptText, 60)
);

VBox statsWrapper = new VBox(12);

VBox modelCard = createStatCard("Model", modelVal, FontAwesome.CUBE);
VBox softwareCard = createStatCard("Software", softwareVal, FontAwesome.TERMINAL);
HBox.setHgrow(modelCard, Priority.ALWAYS);
softwareCard.setPrefWidth(220);
HBox.setHgrow(softwareCard, Priority.NEVER);
HBox row1 = new HBox(12, modelCard, softwareCard);

VBox stepsCard = createStatCard("Steps", stepsVal, FontAwesome.TASKS);
VBox cfgCard = createStatCard("CFG", cfgVal, FontAwesome.ADJUST);
VBox seedCard = createStatCard("Seed", seedVal, FontAwesome.KEY);
VBox samplerCard = createStatCard("Sampler", samplerVal, FontAwesome.SLIDERS);
VBox schedulerCard = createStatCard("Scheduler", schedulerVal, FontAwesome.CLOCK_O);
stepsCard.setPrefWidth(80);
cfgCard.setPrefWidth(80);
seedCard.setPrefWidth(180);
HBox.setHgrow(seedCard, Priority.NEVER);
HBox.setHgrow(samplerCard, Priority.ALWAYS);
HBox.setHgrow(schedulerCard, Priority.ALWAYS);
HBox row2 = new HBox(12, stepsCard, cfgCard, seedCard, samplerCard, schedulerCard);

VBox sizeCard = createStatCard("Size", sizeVal, FontAwesome.IMAGE);
VBox denoiseCard = createStatCard("Denoise", denoiseVal, FontAwesome.TINT);
VBox hiresCard = createStatCard("Hires. fix", hiresFixVal, FontAwesome.EXPAND);
VBox modelHashCard = createStatCard("Model Hash", modelHashVal, FontAwesome.HASHTAG);
HBox row3 = new HBox(12, sizeCard, denoiseCard, hiresCard, modelHashCard);
HBox.setHgrow(sizeCard, Priority.ALWAYS);
HBox.setHgrow(denoiseCard, Priority.ALWAYS);
HBox.setHgrow(hiresCard, Priority.ALWAYS);
HBox.setHgrow(modelHashCard, Priority.ALWAYS);

lorasVal.setEditable(false);
lorasVal.setWrapText(true);
lorasVal.setPrefHeight(70);
lorasVal.getStyleClass().add("text-area");
lorasVal.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

controlNetVal.setEditable(false);
controlNetVal.setWrapText(true);
controlNetVal.setPrefHeight(50);
controlNetVal.getStyleClass().add("text-area");
controlNetVal.setStyle("-fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0;");

statsWrapper.getChildren().addAll(
        row1, row2, row3,
        createStatCard("Loras Used", lorasVal, FontAwesome.PUZZLE_PIECE),
        createStatCard("ControlNet", controlNetVal, FontAwesome.SITEMAP)
);

metadataPanel.getChildren().addAll(promptsWrapper, statsWrapper);

mainSplit.getChildren().addAll(imageColumn, metadataPanel);

Label title = new Label("AI Image Metadata Extractor");
title.getStyleClass().add("content-title");

container.getChildren().addAll(title, mainSplit);
this.setContent(container);
```

(This replaces the old top-to-bottom `dropSection` / `promptsWrapper` / `statsWrapper` stacking with a single `mainSplit` `HBox` — image column on the left, prompts+stats panel on the right.)

- [ ] **Step 4: Fix the `Size` field to read the ported `Resolution`/`FileSize` keys**

The ported `MetadataService` (Task 9) emits `Resolution` (e.g. `"1024x1024"`) instead of separate `Width`/`Height`, and adds `FileSize`. In the `process(File f)` method, change:
```java
String sizeText = lastData.get("Size");
if (sizeText == null && lastData.containsKey("Width") && lastData.containsKey("Height")) {
    sizeText = lastData.get("Width") + "x" + lastData.get("Height");
}
sizeVal.setText(sizeText != null ? sizeText : "N/A");
```
to:
```java
sizeVal.setText(lastData.getOrDefault("Resolution", "N/A"));
```

- [ ] **Step 5: Populate the new fields in `process(File f)`**

In the `extractionTask.setOnSucceeded` handler, after the existing `lorasVal.setText(...)` line, add:
```java
schedulerVal.setText(lastData.getOrDefault("Scheduler", "N/A"));
denoiseVal.setText(lastData.getOrDefault("Denoise", "N/A"));
hiresFixVal.setText(lastData.getOrDefault("Hires. fix", "Disabled"));
modelHashVal.setText(lastData.getOrDefault("Model Hash", "N/A"));
controlNetVal.setText(lastData.getOrDefault("ControlNet", "None"));
```

- [ ] **Step 6: Verify it compiles**

Run: `mvn -q compile`
Expected: `BUILD FAILURE` still, but now only from `MetadataApp.java` (which still constructs the old `RootLayout` and the old 2-arg `CustomTitleBar`). Confirm no errors reference `ExtractorView.java`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/ui/views/ExtractorView.java
git commit -m "Rebuild ExtractorView as an image + side panel layout, remove Favorites"
```

---

### Task 15: Rewire `MetadataApp`

**Files:**
- Modify: `src/main/java/com/nilsson/metadataviewer/MetadataApp.java`

**Interfaces:**
- Consumes: `CustomTitleBar(Stage, Runnable, Runnable)` (Task 12), `ExtractorView` (Task 14) — `process(File)`, `SettingsDialog.show(Window)` (Task 13).
- Produces: a fully wired, compiling application entry point. This is the last task that touches production code — after this task `mvn compile` must succeed.

- [ ] **Step 1: Remove the `RootLayout` import and usage**

Change:
```java
import com.nilsson.metadataviewer.ui.CustomTitleBar;
import com.nilsson.metadataviewer.ui.ResizeHelper;
import com.nilsson.metadataviewer.ui.RootLayout;
```
to:
```java
import com.nilsson.metadataviewer.ui.CustomTitleBar;
import com.nilsson.metadataviewer.ui.ResizeHelper;
import com.nilsson.metadataviewer.ui.views.ExtractorView;
import com.nilsson.metadataviewer.ui.views.SettingsDialog;
```

- [ ] **Step 2: Host `ExtractorView` directly instead of `RootLayout`**

Change:
```java
CustomTitleBar titleBar = new CustomTitleBar(primaryStage, () -> System.exit(0));
RootLayout rootLayout = new RootLayout(primaryStage, titleBar);

BorderPane mainWrapper = new BorderPane();
mainWrapper.setTop(titleBar);
mainWrapper.setCenter(rootLayout);
```
to:
```java
ExtractorView extractorView = new ExtractorView();
CustomTitleBar titleBar = new CustomTitleBar(
        primaryStage,
        () -> System.exit(0),
        () -> SettingsDialog.show(primaryStage)
);

BorderPane mainWrapper = new BorderPane();
mainWrapper.setTop(titleBar);
mainWrapper.setCenter(extractorView);
```

- [ ] **Step 3: Load the new stylesheet**

Change:
```java
String cssPath = getClass().getResource("/dark-theme.css") != null
        ? getClass().getResource("/dark-theme.css").toExternalForm()
        : "";
```
to:
```java
String cssPath = getClass().getResource("/latent-theme.css") != null
        ? getClass().getResource("/latent-theme.css").toExternalForm()
        : "";
```

- [ ] **Step 4: Fix the file-association open-with handling**

Change:
```java
List<String> args = getParameters().getRaw();
if (!args.isEmpty()) {
    File file = new File(args.get(0));
    String lowerName = file.getName().toLowerCase();

    // Case-insensitive check + jpeg support
    if (file.exists() && (lowerName.endsWith(".png") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") ||
            lowerName.endsWith(".webp"))) {

        // Run on UI thread to be safe
        Platform.runLater(() -> rootLayout.openInitialFile(file));
    }
}
```
to:
```java
List<String> args = getParameters().getRaw();
if (!args.isEmpty()) {
    File file = new File(args.get(0));
    String lowerName = file.getName().toLowerCase();

    // Case-insensitive check + jpeg support
    if (file.exists() && (lowerName.endsWith(".png") ||
            lowerName.endsWith(".jpg") ||
            lowerName.endsWith(".jpeg") ||
            lowerName.endsWith(".webp"))) {

        // Run on UI thread to be safe
        Platform.runLater(() -> extractorView.process(file));
    }
}
```

- [ ] **Step 5: Verify the full project compiles**

Run: `mvn -q compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Run the full test suite**

Run: `mvn -q test`
Expected: `BUILD SUCCESS`, all tests from Tasks 3–9 pass.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/nilsson/metadataviewer/MetadataApp.java
git commit -m "Rewire MetadataApp to host ExtractorView directly, remove nav shell"
```

---

### Task 16: Final integration verification, docs, and cleanup

**Files:**
- Modify: `Handover.md`
- Modify: `README.md`

**Interfaces:**
- Produces: nothing new — this task verifies the whole rework end-to-end and updates docs to match reality.

- [ ] **Step 1: Full clean build**

Run: `mvn -q clean package`
Expected: `BUILD SUCCESS`, producing `target/MetadataViewer-1.1.0-SNAPSHOT.jar`.

- [ ] **Step 2: Manual smoke test (documented, not automated — no TestFX in scope)**

Run: `java -jar target/MetadataViewer-1.1.0-SNAPSHOT.jar`

Verify by hand:
1. Window opens, undecorated, titled "Metadata Extractor" — no sidebar, just the image+panel layout.
2. Click the gear/settings icon in the titlebar → the `alx_logo`, version, sponsor links, and "moved to Latent Library" note appear in a modal; sponsor links open a browser; Close dismisses it.
3. Drag a ComfyUI-generated PNG onto the drop zone → prompt, negative, model, software, sampler, scheduler, steps, CFG, seed, size, denoise, hires. fix, model hash, loras, and controlnet fields populate (whichever the test image actually has metadata for — absent fields should show their `N/A`/`None`/`Disabled` placeholders, not throw).
4. Click the image preview → fullscreen modal opens; Escape or click closes it.
5. Click "Raw Metadata" → the raw JSON/text dialog opens showing the unparsed metadata blob.
6. No Favorites/Scrubber/Speed Sorter UI is reachable anywhere.

If any step fails, stop and fix the underlying task before proceeding — do not patch around it in this task.

- [ ] **Step 3: Grep for dead references**

Run: `grep -rn "FavoriteRegistry\|FavoriteData\|RootLayout\|SideNavigation\|dark-theme" "C:\Users\error\IdeaProjects\Projects\MetaDataViewer\src"`
Expected: no output (only `docs/` and `Handover.md` may still mention them historically, which is fine).

- [ ] **Step 4: Update `Handover.md`**

Replace its content with a fresh handover reflecting the rework:

```markdown
# Handover

## Overview
This document tracks recent changes, current context, and next steps for AI and human contributors working on `MetaDataViewer`.

## Recent Changes
- Completed the Latent rework on branch `latent-rework` (see `docs/latent-rework-2026-08-16.md` for the design, `docs/superpowers/plans/2026-08-16-latent-rework.md` for the implementation plan):
  - Removed Favorites, Metadata Scrubber, and Speed Sorter — they now live in Latent Library (`C:\Users\error\IdeaProjects\Projects\Latent-Library`).
  - Removed the sidebar navigation shell (`RootLayout`, `SideNavigation`); the app is now a single-screen extractor hosted directly by `MetadataApp`.
  - Ported the metadata-parsing engine (`MetadataService`, `TextParamsParser`, and the 5 `MetadataStrategy` implementations) from Latent-Library's Spring Boot backend, stripped of Spring — the ComfyUI strategy in particular gained full node-graph traversal, custom-node support (minus the `UserDataManager`-backed user-configurable node names, which needs a settings store this stateless app doesn't have), and several new result fields (`Scheduler`, `Denoise`, `Hires. fix`, `Model Hash`, `Distilled CFG`, `ControlNet`).
  - Replaced `dark-theme.css` with `latent-theme.css`, built from the Latent Design System's token values (`C:\Users\error\IdeaProjects\Projects\Latent-Design-System`).
  - Rebuilt `ExtractorView` as an image + metadata-panel layout.
  - Added a settings/about modal (`SettingsDialog`, opened from a titlebar icon) carrying the `alx_logo` branding, sponsor links, and a pointer to Latent Library for the removed features.
  - Made the Maven build cross-platform via OS-family profiles selecting the `javafx-graphics` classifier, instead of a hardcoded `win` classifier.
  - Added JUnit 5 and unit tests for the ported strategy/service classes (no UI test automation — out of scope).

## Known issues / needs attention
- The `data/` directory (`data/favorites/`, `data/settings.json`) still exists on disk from before this rework and is still tracked in git, but nothing in the app reads or writes it anymore. Still needs a decision on whether to remove it from git (flagged previously, not yet resolved).
- `README.md` badges/screenshots describing Favorites/Scrubber/Speed Sorter and the "Java 8" badge are stale relative to the current Java 21 / single-screen app — update alongside this rework if not already done.

## Next Steps
- Merge `latent-rework` into `development` once reviewed.
- Consider whether `ComfyUIStrategy`'s dropped custom-node-name feature is worth reintroducing via a minimal local settings file, if users ask for it.
```

- [ ] **Step 5: Update `README.md`**

Update the feature list, screenshots section, and badges to describe the single-screen extractor instead of the four-view app — remove Favorites/Scrubber/Speed Sorter mentions and the stale `Java-8` badge (replace with a `Java-21` badge in the same shields.io style), and add a line noting those features now live in Latent Library. Read the current `README.md` fully first and make the edits directly — this is a documentation pass, not a design decision, so use your judgment on exact wording while preserving the existing badge/section style.

- [ ] **Step 6: Commit**

```bash
git add Handover.md README.md
git commit -m "Update Handover.md and README.md for the Latent rework"
```

---

## Self-Review Notes

- **Spec coverage:** every section of `docs/latent-rework-2026-08-16.md` maps to a task — scope/removal (Tasks 10, 16), metadata engine port (Tasks 2–9), design system port (Task 11), layout (Task 14), cross-platform build (Task 1), testing (Tasks 3–9), settings modal (Tasks 12–13, 15).
- **Type/name consistency verified across tasks:** `MetadataService()` no-arg constructor (Task 9) matches `new MetadataService()` used in `ExtractorView` (Task 14, unchanged from the pre-rework file). `ComfyUIStrategy()` no-arg-only (Task 7) matches its use in `TextParamsParser` (Task 8) and `MetadataService`'s strategy list (Task 9). `CustomTitleBar`'s new 3-arg constructor (Task 12) matches its call site in `MetadataApp` (Task 15). `SettingsDialog.show(Window)` (Task 13) matches its call site in `MetadataApp` (Task 15).
- **Known intentional temporary breakage:** Tasks 10, 12, 13, 14 each leave `mvn compile` failing at points other than the task's own new code — called out explicitly in each task's verification step so an executor doesn't mistake it for a bug in their own work. Task 15 is where the build is expected to go green again.
