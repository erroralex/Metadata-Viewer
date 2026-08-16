# Handover

## Overview
This document tracks recent changes, current context, and next steps for AI and human contributors working on `MetaDataViewer`.

## Recent Changes
- Set up the AI assistant rulebook via the `drop-in-brain` starter kit: `AGENTS.md` (base rules + filled-in Project header), `CLAUDE.md` shim (`@AGENTS.md`), `.agents/skills/ai-setup-doctor/`, `.claude/skills` junction to `.agents/skills`, and `.claude/settings.json` (permission allowlist adjusted for plain `mvn`, since this project has no Maven wrapper).
- No stack-specific addons were appended — this is a plain JavaFX desktop app (Java 21, Maven, no Spring Boot, no web frontend, no database), so none of the kit's addons (Spring Boot, .NET, Vue/Angular/React, Thymeleaf, databases) applied. Only the technology-agnostic base rulebook is in effect.
- Added `.claude/skills/` to `.gitignore` (the junction duplicates `.agents/skills/` content and shouldn't be committed twice).

## Known issues / needs attention
- `.gitignore` still has a stale entry `src/main/resources/data/json/favorites.json`, but the app's actual data now lives at `data/favorites/favorites.json` and `data/settings.json` (tracked in git, per current `git status`). Worth deciding whether user library data (favorites, thumbnails) should be tracked in the repo at all, or gitignored as local/portable state — left untouched since it affects existing tracked data and uncommitted deletions already present in the working tree.
- No automated tests exist yet (`src/test/` is absent) — the AGENTS.md Project header notes this; the base rulebook's testing rules (regression test per bugfix) will need a test setup (e.g. JUnit) before they can be followed.

## Next Steps
- Decide on the `data/` directory's git tracking policy and update `.gitignore` accordingly.
- Run "check my AI setup" (the `ai-setup-doctor` skill) after cloning to a new machine, or if instructions/skills seem not to load.
- Consider adding a test framework so the base rulebook's TDD/regression-test rules are actually followable.
