# PotatoToolV2
Client-side Fabric mod for Hypixel SkyBlock. Scans players around you for fairy / crystal / exotic / Seymour / skins and other collectibles, then shows hits in chat, HUD, Discord, and a local cache.
**MC 26.1.2** · Fabric · Java 25 · Discord: [discord.gg/potatotool](https://discord.gg/potatotool)
## Install
1. Minecraft **26.1.2** with [Fabric Loader](https://fabricmc.net/) + [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop `potato-tool-v2-1.0.3-mc26.1.2.jar` into your `mods` folder
3. Get a key from [developer.hypixel.net](https://developer.hypixel.net/)
4. In-game: `/potatotoolv2` → **API Keys** → paste it (or `/scannerkey add <key>`)
## Features
- Lobby / player scans with filters (level, categories, skins via SkyCofl last-3 sales)
- Overlay highlights (fairy / crystal / OG fairy borders only — dyes are not exotic)
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
```bash
./gradlew.bat build
```
Jar lands in `build/libs/potato-tool-v2-1.0.3-mc26.1.2.jar`.
