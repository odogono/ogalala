# Repository Guidelines

## Project Structure & Module Organization
- `server/` — Core Java server framework (packages under `com.ogalala.server`), includes a `Makefile` and a `server` run script.
- `mua/` — MUA engine (events, world model, scripting) under `com.ogalala.*`.
- `nile/` — Persistence and world data (ODI/PSE integration) and app entry points.
- `tools/` — Developer utilities (ODL compiler, docs, dependency tools) with its own `Makefile`.
- `apps/` — Example and product apps (e.g., `apps/nileServer/`) with Make-based builds and scripts.
- Historical `CVS/` folders and `.DS_Store` files are artifacts; do not modify.

## Build, Test, and Development Commands
- Build server JAR: `cd server && make all` (outputs `Server.jar`).
- Build tools JAR: `cd tools && make all` (outputs `tools.jar`).
- Build Nile app + scripts: `cd apps/nileServer && make` (produces `Server.jar` and compiled scripts under `scripts/compiled`).
- Clean (where available): `make clean`.
- Run server: `cd server && ./server` or `cd apps/nileServer && ./run`.
- Note: Requires JDK tools (`javac`, `jar`) and `/misc/ODI/pse.jar` on classpath (see Makefiles).

## Getting Started
- Install JDK (8 or 11 recommended). Ensure `javac` and `jar` are on `PATH`.
- Provide PSE jar path: `export PSE_JAR=/path/to/pse.jar`.
- Override Makefile classpath when building: `make JCLASSPATH=$PSE_JAR all` (e.g., in `server/` or `apps/nileServer/`).
- Run without `jre` wrapper: `java -cp Server.jar:$PSE_JAR com.ogalala.server.Main`.
- Optional wrapper (if scripts call `jre`): create `~/bin/jre` with:
  ```bash
  #!/usr/bin/env bash
  exec java "$@"
  ```
  then `chmod +x ~/bin/jre` and add `~/bin` to `PATH`.

## Coding Style & Naming Conventions
- Java 1.1/1.2-era code; keep changes minimal and do not reformat wholesale.
- Indentation: preserve existing style (tabs are common); 100% consistent within touched files.
- Packages lower-case (e.g., `com.ogalala.util`); classes `UpperCamelCase`; methods/fields `lowerCamelCase`.
- Filenames match public classes; avoid moving files or packages.

## Testing Guidelines
- No formal unit test harness; rely on building example apps and manual smoke tests.
- Quick check: build `apps/nileServer` and run `./run`; verify login and basic commands load.
- Place any ad‑hoc test Java under existing `.../test/` dirs; do not commit binaries or generated `scripts/compiled`.

## Commit & Pull Request Guidelines
- Commits: imperative mood, concise scope first line (e.g., `server: fix connection timeout handling`).
- Include what/why; reference issues like `#123` when applicable.
- PRs: clear description, reproduction/verification steps, affected dirs (e.g., `server/`, `mua/`), and screenshots/logs if behavior changes.

## Security & Configuration Tips
- Do not commit JARs, databases (`*.db`), or generated compiled scripts.
- The database layer expects `/misc/ODI/pse.jar`; document local overrides but do not hardcode alternative paths in source.
- Maintain Make compatibility; avoid introducing new build systems without discussion.
