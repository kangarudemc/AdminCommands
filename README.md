# AdminCommands
Standalone NeoForge server mod: staff admin commands (teleport, moderation, warps, homes, kits) with PermissionAPI + LuckPerms support.

**Repository:** [github.com/kangarudemc/AdminCommands](https://github.com/kangarudemc/AdminCommands)

**Mod id `admincommands`** · Minecraft **26.1.2** · NeoForge **26.1.2.48-beta** · Java **25**

A standalone NeoForge server mod that adds a full suite of staff/admin slash commands: teleportation, moderation, inventory tools, warps, homes, kits, and more. It does **not** depend on Ironhold or any other content mod — drop the jar on any compatible NeoForge server.

Permissions use NeoForge’s **PermissionAPI** (`admincommands.command.*`). By default, gamemaster-level operators (op level 2+) pass. Install **LuckPerms** to grant individual commands to non-op players.

---

## Requirements

- Minecraft **26.1.2**
- NeoForge **26.1.2.48-beta** (or compatible)
- Java **25** (for building; the mod targets the toolchain in `gradle.properties`)

Optional: **LuckPerms** for per-player / per-group command grants.

---

## Installing

1. Install NeoForge for Minecraft 26.1.2 on the server (and on clients if you play with the mod in the pack).
2. Put `admincommands-<version>.jar` in the server `mods/` folder.
3. Start once to generate config, then edit `serverconfig/admincommands-server.toml` if you need `/reop` owners (see [Configuration](#configuration)).
4. Restart the server.

**Compatibility:** AdminCommands is conflict-aware — if another mod registers the same command (e.g. `/heal`), AdminCommands steps aside gracefully. You can always reach this mod's version via `/ac:heal` or `/admincommands:heal`.

---

## Configuration

Server-side only — never synced to clients.

| Where | Path |
|---|---|
| Dedicated server | `<world>/serverconfig/admincommands-server.toml` |
| Singleplayer | `saves/<World Name>/serverconfig/admincommands-server.toml` |

Edit while stopped, or use `/reload` if your server supports reloading server configs.

| Key | Default | Purpose |
|---|---|---|
| `reopOwners` | `[]` | Mojang **account** usernames (not nicknames) allowed to run `/reop` and auto-receive op on login. Case-insensitive. Not a LuckPerms node. |

Example:

```toml
reopOwners = ["YourUsername", "CoOwnerName"]
```

All other commands use permission nodes only. `/whois` nickname lookup is automatic (tab-list / chat display names); there are no extra config keys for it.

---

## Permissions

### Default (no LuckPerms)

Each command checks `admincommands.command.<name>`. The built-in resolver allows **gamemaster** operator level (vanilla op level 2+). Console and command blocks use the same gamemaster check.

### LuckPerms

Grant the exact node string:

```bash
/lp user Steve permission set admincommands.command.fly true
/lp group moderator permission set admincommands.command.freeze true
/lp group moderator permission set admincommands.command.whois true
```

**Nodes** (full id = `admincommands.command.` + key):

| Key | Commands |
|---|---|
| `back` | `/back` |
| `fly` | `/fly` |
| `top` | `/top` |
| `thru` | `/thru` |
| `spawn` | `/spawn [player]` |
| `tphere` | `/tphere <player>` |
| `dimension` | `/dimension <overworld\|nether\|end>` |
| `heal` | `/heal` |
| `feed` | `/feed` |
| `god` | `/god` |
| `vanish` | `/vanish` |
| `freeze` | `/freeze <player>` |
| `mute` | `/mute <player> [seconds]` |
| `unmute` | `/unmute <player>` |
| `sudo` | `/sudo <player> <command…>` |
| `speed` | `/speed <walk\|fly> <multiplier>` |
| `broadcast` | `/broadcast <message>` |
| `nickname` | `/nickname [clear\\|name]` · `/nickname set <target> <name>` |
| `prefix` | `/prefix [clear\\|text]` · `/prefix set <target> <text>` |
| `suffix` | `/suffix [clear\\|text]` · `/suffix set <target> <text>` |
| `itemname` | `/itemname [clear\\|name]` |
| `itemlore` | `/itemlore [clear\\|add <text>]` |
| `smite` | `/smite <player> [message]` |
| `repair` | `/repair` · `/repair all` |
| `kit` | `/kit create\|list\|delete\|give …` |
| `warp` | `/warp <name>` · `/warp set\|del\|list` |
| `sethome` | `/sethome [name]` |
| `home` | `/home [name]` |
| `delhome` | `/delhome <name>` |
| `homes` | `/homes` |
| `ec` | `/ec [player]` |
| `seeinv` | `/seeinv <player>` |
| `whois` | `/whois <name>` |
| `gamemode` | `/c` `/s` `/sp` `/ad` |

`/reop` is **not** node-gated — only `reopOwners` in config.

LuckPerms will not list nodes in its web editor until they have been registered at least once (start the server with this mod loaded).

---

## Commands

### Movement & teleport

| Command | Description |
|---|---|
| `/back` | Return to last death or pre-`/teleport` location (toggles with current spot). |
| `/fly` | Toggle survival flight. |
| `/top` | Teleport to the surface above you. |
| `/thru` | Teleport to your crosshair hit. |
| `/spawn [player]` | Send yourself or a target to world spawn. |
| `/tphere <player>` | Pull a player to you. |
| `/dimension <dim>` | `overworld`, `nether`, or `end`. |

### Self restore

| Command | Description |
|---|---|
| `/heal [player]` | Full health and hunger. |
| `/feed [player]` | Restore hunger. |
| `/god` | Toggle damage immunity. |
| `/repair` | Repair held item. |
| `/repair all` | Repair all damageable items in inventory. |

### Gamemode shortcuts

| Command | Mode |
|---|---|
| `/c` | Creative |
| `/s` | Survival |
| `/sp` | Spectator |
| `/ad` | Adventure |

All four share the `gamemode` permission node.

### Namespace Aliases & Conflict Resolution
Every command in this mod is registered dynamically with built-in conflict resolution!
- If another mod registers the same command (e.g., `/heal`), this mod politely steps aside to avoid breaking the other mod.
- To use this mod's version regardless of conflicts, simply prefix any command with `/ac:` or `/admincommands:` (e.g., `/ac:heal` or `/admincommands:heal`).

### Commands

| Command | Description |
|---|---|
| `/vanish` | Hide from other players (tab list, entity, etc.). |
| `/freeze <player>` | Lock player in place (toggle). |
| `/mute <player> [seconds]` | Block chat; omit seconds for permanent until `/unmute`. |
| `/unmute <player>` | Restore chat. |
| `/sudo <player> <command…>` | Run a command as that player. |
| `/speed <walk\|fly> <0–10>` | Movement speed multiplier. |
| `/broadcast <message>` | Server-wide message; you can specify style/color words before the message to format it. |
| `/nickname clear` · `/nickname <name>` | Set your nickname (color → style autotab). |
| `/nickname set <target> <name>` · `/nickname clear <target>` | Set/clear another player's nickname. |
| `/prefix clear` · `/prefix <text>` | Set your chat/tab prefix (color → style autotab). |
| `/prefix set <target> <text>` · `/prefix clear <target>` | Set/clear another player's prefix. |
| `/suffix clear` · `/suffix <text>` | Set your chat/tab suffix (color → style autotab). |
| `/suffix set <target> <text>` · `/suffix clear <target>` | Set/clear another player's suffix. |
| `/smite <player> [message]` | Lightning + optional broadcast. |

### Inventory

| Command | Description |
|---|---|
| `/seeinv <player>` | Live six-row view of main inventory, armor, offhand. |
| `/ec [player]` | Open ender chest (self or target). |
| `/itemname clear` · `/itemname <name>` | Rename the held item (supports color autotab). |
| `/itemlore clear` · `/itemlore add <text>` | Manage lore of the held item (supports color autotab). |
| `/kit create <name>` | Save your inventory as a kit. |
| `/kit <name> [player]` | Give kit to self or target. |
| `/kit list` · `/kit delete <name>` | List or remove kits. |

### Warps & homes

| Command | Description |
|---|---|
| `/warp set <name>` | Set a server warp at your position. |
| `/warp <name>` | Teleport to warp. |
| `/warp del <name>` · `/warp list` | Manage warps. |
| `/sethome [name]` | Personal home (default name `home`). |
| `/home [name]` | Teleport to home. |
| `/delhome <name>` · `/homes` | Delete or list your homes. |

Homes are per-player; warps are server-wide. Data is stored in the world save.

### `/whois <name>`

Permission: `admincommands.command.whois`.

Resolves an **online** player by **Mojang username** or **tab-list / chat nickname** (e.g. LuckPerms + NeoForge `TabListNameFormat`). Tab completion offers both when they differ.

**Output**

- Same name: `Username — gamemode [OP]`
- Nickname: `Shown as Nick — account Username — gamemode [OP]`
- Second line: dimension, position, HP, hunger, vanilla XP level

Generic server info only — not RPG or mod-specific profile data.

### `/reop`

Only for usernames in `reopOwners`. Re-grants operator status to yourself; also applied automatically on login. Hidden from players not on the list.



## Resources

- **Source & issues:** [kangarudemc/AdminCommands](https://github.com/kangarudemc/AdminCommands) · [Issues](https://github.com/kangarudemc/AdminCommands/issues)
- NeoForged docs: <https://docs.neoforged.net/>
- NeoForged permission nodes: <https://docs.neoforged.net/docs/server/permissions>
