<div align="center">

# AdminCommands

**The ultimate server-side utility and essentials replacement for NeoForge.**

[![Wiki](https://img.shields.io/badge/Wiki-Documentation-blue?style=flat&logo=wikipedia)](https://github.com/yourname/admincommands/wiki)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

</div>

---

## ⚡ What is AdminCommands?
AdminCommands is a highly optimized, fully-featured, server-side-only administration mod for NeoForge. It replaces bloated, laggy essential plugins with a single, tightly-integrated solution built directly into the game's command dispatcher.

Whether you're running a small private server or a massive SMP network, AdminCommands gives you the tools to moderate, customize, and control your server with zero client-side installation required.

## 🚀 Key Features

### ⏱️ Built for TPS Performance
Unlike many server-side mods, AdminCommands is engineered from the ground up to have **zero impact on your server's TPS (Ticks Per Second)**. 
- **In-Memory Caching:** Heavy operations like Jail rubber-banding and Display Name formatting are cached in `ConcurrentHashMaps`.
- **Zero Disk I/O on Tick:** Does not perform disk lookups during critical game loops.
- **Fast-Path Events:** Cancels unauthorized interactions instantaneously.

### 🛡️ Complete Jail System (`/jail`)
A robust, inescapable jail system designed for serious moderation.
- Create up to 10 persistent jails.
- Automatically strips and stores the jailed player's inventory.
- Disables commands, block breaking/placing, item pickups, dropping, and PVP/PVE damage for the jailed player.
- Instant rubber-banding if they attempt to escape or glitch out.
- `/release` safely restores their original location and fully returns their inventory.

### 💬 Deep Chat & Vanity Customization
Give your players the identity they deserve without needing a separate chat plugin.
- Set custom Nicknames, Prefixes, and Suffixes.
- Full support for standard Minecraft color formatting codes (`&a`, `&l`, `&o`, etc.).
- Custom formats are instantly reflected in both the **Chat** and the **Tab List**.

### 🌍 Teleportation & Waypoints
- **Warps:** Create global server warps for community hubs or arenas.
- **Homes:** Allow players to set personal home points to easily return to their base.
- **TPA System:** Players can request to teleport to each other (`/tpa`, `/tpaccept`, `/tpdeny`).
- **`/back`:** Instantly return to your previous location after teleporting or dying.

### 👻 Powerful Moderation Tools
- **`/vanish`:** Become completely invisible to regular players (hidden from the world and Tab List).
- **`/freeze`:** Stop a player dead in their tracks, preventing all movement.
- **`/mute`:** Silence disruptive players temporarily or permanently.

## 📋 Full Command List

> **Tip:** You can prefix any command with `/ac:` (e.g., `/ac:fly`) to bypass conflicts with other mods!

| Category | Commands |
| :--- | :--- |
| **Moderation** | `/jail`, `/setjail`, `/release`, `/freeze`, `/mute`, `/vanish`, `/kick`, `/ban` |
| **Teleportation** | `/warp`, `/setwarp`, `/home`, `/sethome`, `/tpa`, `/tpaccept`, `/tpdeny`, `/back`, `/top`, `/thru`, `/spawn`, `/tphere` |
| **Vanity** | `/nick`, `/prefix`, `/suffix`, `/itemname`, `/itemlore` |
| **Utility** | `/fly`, `/god`, `/heal`, `/feed`, `/repair`, `/speed`, `/whois`, `/dimension`, `/broadcast` |

*(Type `/admincommands` in-game for an interactive clickable tutorial menu!)*

## ⚙️ Installation & Compatibility

- **Server-Side Only:** This mod only needs to be installed on the server! Players connecting do not need to download the mod, making it perfect for vanilla clients.
- **NeoForge Compatible:** Built natively for NeoForge, ensuring deep integration with modern Minecraft servers.
- **Permissions:** Integrates seamlessly with standard NeoForge permission nodes.

## 🐛 Bug Reports & Support
If you encounter any issues or want to suggest a new feature, please open an issue on our [GitHub Tracker](#).
