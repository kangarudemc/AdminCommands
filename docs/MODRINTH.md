# Publishing AdminCommands on Modrinth

This guide covers what Modrinth requires, what this repo already includes, and how to run a successful launch.

## What Modrinth requires

| Item | Requirement | This repo |
|---|---|---|
| **Project type** | `mod` | NeoForge mod jar |
| **Version file** | `.jar` per release | `build/libs/admincommands-<version>.jar` |
| **Name & slug** | Unique, 3–64 chars | Suggested slug: `admincommands` |
| **Summary** | 3–256 chars (search + cards) | See `gradle.properties` → `modrinth_summary` |
| **Description** | Markdown body (up to 64k) | `modrinth.body.md` (sync via Minotaur) |
| **License** | SPDX id (`MIT`, `ARR`, …) | `MIT` + `LICENSE` |
| **Categories** | 1–3 primary (mod-type) | `utility`, `management`, `game-mechanics` (pick on dashboard) |
| **Loaders** | Per version | `neoforge` |
| **Game versions** | Per version | `26.1.2` (match `minecraft_version`) |
| **Icon** | Square PNG/JPEG, ≤256 KiB | `assets/modrinth/icon.png` (512×512) |
| **Side support** | Client / server | **Server required, client unsupported** (see below) |

### Server-only (important)

AdminCommands is a **server mod**. On the Modrinth project settings set:

- **Server support:** Required (or equivalent)
- **Client support:** Unsupported

Players do not need the jar unless you intentionally require it in a pack.

## What helps success on Modrinth

1. **Clear summary** — state “NeoForge server admin commands” and LuckPerms in one line.
2. **Strong description** — features table, setup steps, permission examples (`modrinth.body.md`).
3. **Good icon** — readable at 64×64 in search results (`assets/modrinth/icon.png`).
4. **Accurate metadata** — correct Minecraft + NeoForge versions; don’t claim unsupported loaders.
5. **Changelog every release** — `CHANGELOG.md`; Minotaur uploads it with each version.
6. **Open license** — MIT improves trust and third-party pack inclusion (already set).
7. **Source link** — Git repo URL on the project page (required for approval in many cases).
8. **Issues link** — GitHub Issues or Discord for support.
9. **Gallery (optional)** — 1–3 screenshots: tab completion, `/whois` nickname output, LuckPerms node list.
10. **No duplicate commands** — document conflicts if users also run Essentials-style mods.
11. **Tags** — `neoforge`, `server-side`, `multiplayer`, `admin`, `permissions` as additional categories where available.
12. **Semantic versions** — bump `mod_version` in `gradle.properties` for each upload.

## One-time: create the Modrinth project

1. Log in at [modrinth.com](https://modrinth.com) → **Create a project**.
2. **Project type:** Mod.
3. **Name:** AdminCommands (or your display name).
4. **Slug:** `admincommands` (or available variant).
5. **Summary:** copy from `modrinth_summary` in `gradle.properties`.
6. **Description:** paste `modrinth.body.md`, or run `./gradlew modrinthSyncBody` after setup.
7. **License:** MIT.
8. **Categories:** `utility`, `management` (and one more if useful).
9. **Client / server:** server required, client unsupported.
10. **Upload icon:** `assets/modrinth/icon.png`.
11. **Links:** source repo, issues, optional Discord.
12. Copy the **project ID** (32-char) or note the **slug**.

Add to `gradle.properties` (or `~/.gradle/gradle.properties`, never commit tokens):

```properties
modrinth_project_id=YOUR_PROJECT_ID_OR_SLUG
```

## Token (for uploads)

1. [Account settings → Pat tokens](https://modrinth.com/settings/account)
2. Scopes: `CREATE_VERSION`, `PROJECT_WRITE`
3. Export locally:

```bash
export MODRINTH_TOKEN="mrp_..."
```

Never commit the token.

## Build & upload a version

```bash
./gradlew build
./gradlew modrinthSyncBody    # optional: push modrinth.body.md to project page
./gradlew modrinth            # uploads jar + changelog
```

`modrinth` only runs when `modrinth_project_id` is set in `gradle.properties` (not the placeholder).

## Manual upload (no Gradle)

1. `./gradlew build`
2. Modrinth → project → **Versions** → **Upload**
3. File: `build/libs/admincommands-<version>.jar`
4. Loaders: NeoForge; game version `26.1.2`
5. Changelog: section from `CHANGELOG.md`
6. Channel: Release (or Beta for previews)

## Checklist before first public release

- [ ] `mod_version` bumped
- [ ] `CHANGELOG.md` updated
- [ ] `./gradlew build` succeeds
- [ ] No duplicate command mods documented in description
- [ ] Icon uploaded (≤256 KiB, square)
- [ ] Server required / client unsupported set on project
- [ ] Source + issues URLs filled in
- [ ] First version tested on a NeoForge 26.1.2 dedicated server
- [ ] LuckPerms example tested (optional)

## Approval

New projects may enter **Under review** until staff approve. Complete metadata, valid license, working jar, and linked source improve approval time.

## References

- [Creating projects (Modrinth)](https://modrinth.com/settings/projects)
- [Minotaur Gradle plugin](https://github.com/modrinth/minotaur)
- [NeoForge downloads](https://neoforge.net/)
