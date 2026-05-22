# Changelog

All notable changes to AdminCommands will be documented in this file.

## [1.0.0] — 2025-05-22

### Added
- **30+ staff commands** — teleportation, moderation, inventory tools, warps, homes, kits
- **Permission system** — NeoForge PermissionAPI nodes (`admincommands.command.*`) with LuckPerms support
- **Conflict resolution** — gracefully bows out if another mod registers the same command name; always reachable via `/ac:<command>` or `/admincommands:<command>`
- **Formatting system** — `/nickname`, `/prefix`, `/suffix`, `/itemname`, `/itemlore`, and `/broadcast` support interactive color → style autotab completion
- **Nickname, prefix & suffix** — persistent, formatted, rendered in chat and tab list. Supports targeting other players
- **Item customization** — `/itemname` and `/itemlore` for renaming and adding formatted lore to held items
- **Vanish** — hides from tab list and player rendering, with persistent "You're Vanished!" action bar reminder
- **Freeze** — locks players in place with attribute-based movement prevention
- **Speed** — walk and fly speed multipliers using server-side attribute modifiers (no rubberbanding)
- **Whois** — resolves players by Mojang username or visible nickname, supports names with spaces
- **Kits** — save, give, list, and delete inventory kits
- **Warps & homes** — persistent, per-player homes and server-wide warps
- **Self-restore** — `/heal` and `/feed` with optional target player
- **Gamemode shortcuts** — `/c`, `/s`, `/sp`, `/ad`
- **Reop** — config-gated owner recovery, hidden from non-owners, auto-applies on login
- **TPS optimizations** — pre-built packets, throttled tick handlers, cached data lookups, early-exit fast paths
- **`/admincommands` help** — ties into vanilla `/help` system
