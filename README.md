<div align="center">

# PotatoToolV2

**A client-side Fabric mod for Hypixel SkyBlock.**

Scans the players around you for fairy armor, crystal armor, exotics, Seymour pieces, cosmetic skins
and other collectibles, then reports every hit to chat, the HUD, Discord, and a local cache.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=flat-square)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric%200.19.3%2B-DBD0B4?style=flat-square)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-25-E76F00?style=flat-square)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Discord](https://img.shields.io/badge/Discord-join%20the%20server-5865F2?style=flat-square&logo=discord&logoColor=white)](https://discord.gg/PvEYc4Kwy)

</div>

---

## Contents

[Requirements](#requirements) · [Installation](#installation) · [Features](#features) · [Commands](#commands) · [Settings](#settings) · [Building from source](#building-from-source) · [Data and privacy](#data-and-privacy) · [Support](#support) · [License](#license)

---

## Requirements

| | |
|---|---|
| **Minecraft** | 26.1.2 |
| **Mod loader** | [Fabric Loader](https://fabricmc.net/) 0.19.3 or newer |
| **Dependencies** | [Fabric API](https://modrinth.com/mod/fabric-api) · [Mod Menu](https://modrinth.com/mod/modmenu) *(optional)* |
| **Java** | 25 or newer |
| **Side** | Client only — never install it on a server |
| **Account** | A [Hypixel API key](https://developer.hypixel.net/) |

> [!IMPORTANT]
> PotatoToolV2 does nothing without a valid Hypixel API key. Hypixel issues one key per account, and
> generating a new key immediately invalidates the previous one.

## Installation

1. Install Fabric Loader 0.19.3+ for Minecraft 26.1.2, then drop [Fabric API](https://modrinth.com/mod/fabric-api) into your `mods` folder.
2. Add `potato-tool-v2-1.0.3-mc26.1.2.jar` to the same `mods` folder. No prebuilt jar is published yet, so [build it from source](#building-from-source).
3. Launch the game and generate a key at [developer.hypixel.net](https://developer.hypixel.net/).
4. Register the key with `/scannerkey add <key>`, or open `/potatotoolv2` and paste it under **API Keys**.
5. Run `/scan lobby` to confirm everything works.

## Features

**Scanning**
- Scan a whole lobby or a single player, filtered by SkyBlock level, item category, and skin value.
- Cosmetic skin pricing is resolved from the last three SkyCofl sales.
- Results are cached locally so repeat lobbies do not burn API calls.

**Display**
- Condensed chat reports with hoverable item lists and a click-to-party action.
- Configurable HUD overlay showing each hit and the exact container an item was found in.
- Inventory highlights for fairy, crystal, and OG fairy pieces.
- Dark glass interface by default, switchable from the title bar or **Appearance → Dark mode**.

**Automation**
- **Automatic** warp macros for `/hub` and `/warp dhub|spider|end|mines|park`, arranged on a board, five seconds apart, looping until cancelled with <kbd>Esc</kbd>.
- **Webhook** — sends a Discord message when a scan produces a hit, with per-category toggles.
- **Notifier** — watches chosen IGNs and alerts you in chat and on Discord when they come online.
- **Hits** — records everyone who actually had something worth noting.

## Commands

Aliases are listed in parentheses. Every command is client-side.

**Scanning**

| Command | Description |
|---|---|
| `/scan lobby` | Scan every player in the current lobby |
| `/scan list` | List cached players that had special items |
| `/scan search <item>` | Find which cached players had a given item |
| `/scanplayer <name>` | Fresh API lookup and profile viewer (`/lookup`) |

**API keys**

| Command | Description |
|---|---|
| `/scannerkey add <key>` | Register a Hypixel API key |
| `/scannerkey list` | Show configured keys and their usage |
| `/scannerkey remove <index>` | Remove a key by index |

**Filters**

| Command | Description |
|---|---|
| `/filter status` | Show all current filter settings |
| `/filter reset` | Restore filter defaults |
| `/filter level min\|max <0-500>` | Bound the SkyBlock level range to scan |
| `/filter toggle <type> <true\|false>` | Enable or disable an item category |
| `/filter blacklist add\|remove\|list\|clear` | Manage the blacklist |
| `/filter whitelist add\|remove\|list\|clear` | Manage the whitelist |
| `/filter whitelist enable <true\|false>` | Switch whitelist mode on or off |

Filter types are grouped as **categories** (`weapons`, `armor`, `accessories`, `tools`, `pets`, `consumables`, `cosmetics`), **special** (`dungeon`, `slayer`, `event`, `admin`, `valuable`, `legacy`, `ghost`), and **armor** (`crystal`, `fairy`, `bleached`, `soulbound`).

**Interface and config**

| Command | Description |
|---|---|
| `/potatotoolv2` | Open the settings menu (`/potatotool`, `/scannergui`, `/scannersettings`) |
| `/scanner help` | Print the full in-game command reference |
| `/ptshare export` | Copy a shareable config code to the clipboard |
| `/ptshare import <code>` | Import a config code from someone else |
| `/ptcake add\|remove\|list\|defaults` | Manage tracked cake years |

## Settings

The settings menu is organised into these pages:

API Keys · Lookup · Categories · Special · Skins · Advanced · HUD · Appearance · Seymour · Automatic · Webhook · Notifier · Hits

It is reachable with `/potatotoolv2` or through **Mods → PotatoToolV2 → Configure** when Mod Menu is installed.

## Building from source

Requires **JDK 25**. The Gradle wrapper is included, so no separate Gradle installation is needed.

```bash
git clone https://github.com/potatotoolv2/potatotoolv2.git
cd potatotoolv2
./gradlew build
```

On Windows, run `.\gradlew.bat build` instead.

The finished jar is written to `build/libs/potato-tool-v2-1.0.3-mc26.1.2.jar`.

## Data and privacy

Everything the mod stores stays on your machine, inside your Minecraft `config` folder. API keys and
webhook URLs are held locally and are never transmitted anywhere except to the service they belong to,
and never committed to this repository.

| File | Contents |
|---|---|
| `config/potato-tool-v2.json` | Settings, filters, and API keys |
| `config/potato-tool-v2-hits.json` | Cached players that had special items |

## Support

Setup help, bug reports, and feature requests are handled on Discord.

<div align="center">

[![Discord](https://img.shields.io/badge/discord.gg%2FPvEYc4Kwy-join%20the%20server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/PvEYc4Kwy)

</div>

Open a ticket in `#create-a-ticket` and include your Minecraft version, mod version, and the relevant
section of `logs/latest.log` if you are reporting a bug.

## License

Released under the [MIT License](LICENSE). Copyright © prpl.
