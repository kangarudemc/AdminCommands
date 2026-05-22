# AdminCommands

**The lightweight, conflict-aware essentials mod for NeoForge servers.**

Teleportation, moderation, nicknames with formatting, warps, homes, kits, inventory tools — all server-side, zero client install. Plays nice with other mods.

---

## ✨ Why AdminCommands?

- 🛡️ **Server-side only** — players join without installing anything
- 🎨 **Formatted nicknames, prefixes & suffixes** — two-stage color → style autotab dropdown, rendered in chat and tab list
- 🤝 **Conflict-aware** — if another mod registers `/heal`, AdminCommands steps aside. Use `/ac:heal` to reach ours anytime
- ⚡ **TPS-optimized** — pre-built packets, throttled tick handlers, cached lookups. Designed for high-player servers
- 🔐 **LuckPerms-ready** — every command has a `admincommands.command.*` permission node. Works with vanilla op out of the box
- 🪶 **Lightweight** — no economy, no web dashboard, no bloat. Just the commands you actually need

---

## 📦 Command Overview

| Category | Commands |
|---|---|
| **Movement** | `/back`, `/fly`, `/top`, `/thru`, `/spawn`, `/tphere`, `/dimension` |
| **Self** | `/heal`, `/feed`, `/god`, `/repair`, `/speed`, `/c` `/s` `/sp` `/ad` |
| **Moderation** | `/vanish`, `/freeze`, `/mute`, `/unmute`, `/sudo`, `/smite`, `/broadcast` |
| **Cosmetics** | `/nickname`, `/prefix`, `/suffix`, `/itemname`, `/itemlore` |
| **Inventory** | `/seeinv`, `/ec`, `/kit`, `/repair all` |
| **Teleports** | `/warp`, `/sethome`, `/home`, `/delhome`, `/homes` |
| **Info** | `/whois`, `/admincommands` (help) |
| **Owner** | `/reop` (config-gated, hidden from non-owners) |

### 🎨 Formatting System
`/nickname`, `/prefix`, `/suffix`, `/itemname`, `/itemlore`, and `/broadcast` all support an interactive formatting system:
1. **Tab once** → color dropdown (red, gold, aqua, dark_purple…)
2. **Tab again** → style dropdown (bold, italic, strikethrough…)
3. **Type your text** — it renders with your chosen formatting!

### 🤝 Namespace Aliases
Every command is also available as `/ac:<command>` and `/admincommands:<command>`. If another mod takes `/heal`, just use `/ac:heal` — guaranteed to run AdminCommands' version.

---

## ⚙️ Setup

1. Install **NeoForge** for Minecraft **26.1.2** on the server
2. Drop `admincommands-*.jar` in `mods/`
3. Start once, then edit `serverconfig/admincommands-server.toml` for `/reop` owners
4. Restart — done!

## 🔐 LuckPerms Example

```
/lp group moderator permission set admincommands.command.freeze true
/lp group moderator permission set admincommands.command.vanish true
/lp user Alex permission set admincommands.command.whois true
```

All nodes follow the pattern `admincommands.command.<name>`. Operators (op 2+) have access by default.

---

## 🔗 Links

- **Source:** [github.com/kangarudemc/AdminCommands](https://github.com/kangarudemc/AdminCommands)
- **Issues:** [GitHub Issues](https://github.com/kangarudemc/AdminCommands/issues)
- **License:** MIT
