# Handover

## Overview
This document tracks recent changes, current context, and next steps for AI and human contributors working on `MetaDataViewer`.

## Recent Changes
- Set up the AI assistant rulebook via the `drop-in-brain` starter kit: `AGENTS.md` (base rules + filled-in Project header), `CLAUDE.md` shim (`@AGENTS.md`), `.agents/skills/ai-setup-doctor/`, `.claude/skills` junction to `.agents/skills`, and `.claude/settings.json` (permission allowlist adjusted for plain `mvn`, since this project has no Maven wrapper).
- No stack-specific addons were appended — this is a plain JavaFX desktop app (Java 21, Maven, no Spring Boot, no web frontend, no database), so none of the kit's addons (Spring Boot, .NET, Vue/Angular/React, Thymeleaf, databases) applied. Only the technology-agnostic base rulebook is in effect.
- Added `.claude/skills/` and `drop-in-brain-main/` to `.gitignore` — the former duplicates `.agents/skills/` content, the latter is the kit's own source folder (downloaded alongside this repo to run the setup), not part of the project.
- Brainstormed and wrote a design spec (`docs/latent-rework-2026-08-16.md`) and implementation plan (`docs/superpowers/plans/2026-08-16-latent-rework-implementation-plan.md`, 16 tasks) for a full rework: adopt the Latent Design System's look, port the improved metadata-parsing engine from Latent-Library, remove Favorites/Scrubber/Speed Sorter (now in Latent Library), and add a settings/about modal carrying the `alx_logo` branding. Work happens on branch `latent-rework` (not yet created/started — plan execution is the next step).

## Known issues / needs attention
- `.gitignore` still has a stale entry `src/main/resources/data/json/favorites.json`, but the app's actual data now lives at `data/favorites/favorites.json` and `data/settings.json` (tracked in git, per current `git status`). Worth deciding whether user library data (favorites, thumbnails) should be tracked in the repo at all, or gitignored as local/portable state — left untouched since it affects existing tracked data and uncommitted deletions already present in the working tree. The Latent rework plan makes this moot for `FavoriteRegistry`'s own writes (that class is deleted), but the existing tracked `data/` files and this stale `.gitignore` line still need a decision.
- No automated tests exist yet on `development` (`src/test/` is absent) — the Latent rework plan adds JUnit 5 and strategy/service unit tests as part of the port (Tasks 1, 3–9), but that's scoped to the `latent-rework` branch, not yet merged.

## Next Steps
- Execute `docs/superpowers/plans/2026-08-16-latent-rework-implementation-plan.md` (16 tasks, starting with creating the `latent-rework` branch).
- Decide on the `data/` directory's git tracking policy and update `.gitignore` accordingly.
- Run "check my AI setup" (the `ai-setup-doctor` skill) after cloning to a new machine, or if instructions/skills seem not to load.
