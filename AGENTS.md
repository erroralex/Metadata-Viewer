# Agent Instructions

<!-- ===== PROJECT (fill in, delete unused lines) ===== -->
## Project

- **Name:** MetaDataViewer (AI Metadata Viewer & Extractor)
- **Purpose:** JavaFX desktop app that extracts, views, and scrubs AI image-generation metadata (ComfyUI, SwarmUI, A1111, Forge, InvokeAI, NovelAI, SD-Matrix) for artists and developers.
- **Stack:** Java 21 / JavaFX 21 (programmatic UI, no FXML) / Jackson (JSON persistence) / metadata-extractor / Ikonli-FontAwesome / Maven (no wrapper)
- **Build:** `mvn clean package`
- **Test:** no automated tests yet
- **Run locally:** `java -jar target/MetadataViewer-1.2.1.jar` (built by `mvn package`, shaded jar, main class `com.nilsson.metadataviewer.Launcher`)

## Workflow

- Before non-trivial changes: state your plan in 2–5 bullet points, then implement.
  If requirements are ambiguous, ask — don't guess.
- Work in small, verifiable steps. One logical change at a time.
- Write or update the test for a behavior change **before or with** the code,
  never "later".
- Before claiming anything works: run the test/build command and show the result.
  "Should work" is not done — verified is done.
- When a task touches unfamiliar code, read the surrounding files first and follow
  the patterns already there.
- Check `.agents/skills/` for an applicable skill before starting specialized work
  (framework setup, UI design, reviews); use it if its description matches the task.

## Engineering rules

- **YAGNI:** build what the task needs, nothing speculative. No extra config options,
  abstraction layers, or "flexibility" that wasn't asked for.
- **DRY, but not premature:** extract shared code on the third occurrence, not the
  second. Duplication is cheaper than the wrong abstraction.
- **Single responsibility:** one reason to change per class/module/function. If a file
  needs "and" to describe what it does, split it.
- **Depend on interfaces at boundaries** (service ↔ persistence, domain ↔ external
  APIs); don't interface-ify everything else.
- Keep functions short and files focused. A file approaching ~300 lines is a signal
  to split.
- Prefer boring, idiomatic solutions over clever ones. Optimize only with a
  measurement in hand.
- Fail fast: validate inputs at system boundaries, throw early with specific messages,
  never swallow exceptions silently.

## Testing

- Every bugfix gets a regression test that fails before the fix and passes after.
- Test behavior through public interfaces, not implementation details.
- Never delete, skip, or weaken a test to make a change pass. If a test seems wrong,
  say so and ask.
- Tests must be deterministic: no sleeps for synchronization, no order dependence,
  no shared mutable state between tests.

## Git

- Small commits, one logical change each. Imperative subject line ≤ 72 chars;
  body explains *why* when it isn't obvious.
- Before committing: create or update `Handover.md` at the project root with the
  latest changes, context, and clear next steps. Keep it relevant, clean, and free of stale logs.
- **No AI attribution anywhere in git:** no `Co-Authored-By` trailers naming an AI,
  no "Generated with ..." lines in commit messages, PR descriptions, or code
  comments. Commits carry the human author's identity only.
- Never commit secrets, credentials, or generated artifacts.
- Never force-push or rewrite history on shared branches.

## Security

- No secrets in code or config files — use environment variables or a secret manager.
- All user input is untrusted: validate at the boundary, use parameterized
  queries/bound parameters, escape output in templates.
- Don't add dependencies for trivial tasks; when adding one, prefer well-maintained,
  widely-used libraries.

<!-- ===== ADDONS: append per-tech sections from starter-kit/addons/ below ===== -->
