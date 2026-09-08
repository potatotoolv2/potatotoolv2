# PotatoToolV2

[![Discord](https://img.shields.io/badge/Discord-join%20the%20server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/PvEYc4Kwy)

Client-side Fabric mod for Hypixel SkyBlock. Scans players around you for fairy / crystal / exotic / Seymour / skins and other collectibles, then shows hits in chat, HUD, Discord, and a local cache.

**MC 26.1.2** · Fabric · Java 25 · Discord: [discord.gg/PvEYc4Kwy](https://discord.gg/PvEYc4Kwy)

## Download

No prebuilt jar is published here yet, so build it from source with the [Build](#build) steps below.

## Install

1. Minecraft **26.1.2** with [Fabric Loader](https://fabricmc.net/) 0.19.3+ and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop `potato-tool-v2-1.0.3-mc26.1.2.jar` into your `mods` folder
3. Get a key from [developer.hypixel.net](https://developer.hypixel.net/)
4. In-game: `/potatotoolv2` → **API Keys** → paste it (or `/scannerkey add <key>`)

The mod is client-side only. You do not need it on a server, and it does nothing without a Hypixel API key.

## Features

- Lobby / player scans with filters (level, categories, skins via SkyCofl last-3 sales)
- Overlay highlights (fairy / crystal / OG fairy borders only — dyes are not exotic)
- Dark glass GUI by default (click **Dark** / **Light** in the title bar, or Appearance → Dark mode)
- **Automatic** warp macros (`/hub`, `/warp dhub|spider|end|mines|park`) — drag onto a board, 5s apart, loop, ESC cancels
- **Webhook** — Discord ping when a scan hits, with category toggles
- **Hits** — saved names of people who actually had stuff (`config/potato-tool-v2-hits.json`)
- **Notifier** — watch IGNs and get chat + Discord when they come online

## Commands

| Command | What it does |
|---|---|
| `/potatotoolv2` | Open settings (`/potatotool`, `/scannergui` also work) |
| `/scan lobby` | Scan everyone in the lobby |
| `/scanplayer <name>` | Lookup one player (`/lookup` alias) |
| `/scan list` | Cached hits |
| `/scan search <item>` | Who had that item |
| `/scannerkey add/list/remove` | API keys |
| `/scanner help` | Full command list |

## Settings pages

API Keys · Lookup · Categories · Special · Skins · Advanced · HUD · Appearance · Seymour · Automatic · Webhook · Notifier · Hits

## Build

Needs **Java 25**. The Gradle wrapper is included, so nothing else to install.

```bash
git clone https://github.com/potatotoolv2/potatotoolv2.git
cd potatotoolv2
./gradlew build
```

On Windows use `.\gradlew.bat build` instead.

The jar lands in `build/libs/potato-tool-v2-1.0.3-mc26.1.2.jar`.

## Your data

Everything the mod saves stays on your machine, in your Minecraft `config` folder. API keys and webhook URLs you enter are stored locally and are never committed to this repo.

## Support

Setup help, bug reports, and feature requests go through Discord: **[discord.gg/PvEYc4Kwy](https://discord.gg/PvEYc4Kwy)** — open a ticket in `#create-a-ticket`.

## License

[MIT](LICENSE)
