# Wynn Market Search

**Wynncraft mod with enhanced market search**

![WynnMarketSearch GUI](https://i.imgur.com/1muxM4V.png)

## Description

This modification is designed specifically for the **Wynncraft** server and significantly simplifies the process of searching for goods on the in-game market. Thanks to the improved interface and expanded search functions, you will be able to find items faster and make profitable purchases.

## Features

- 🎯 **Auto-open** — Search GUI opens automatically when market message appears
- 🔍 **Smart search** — Instant item search by name with partial match support
- 🎨 **Color coding** — Items displayed with colors according to their rarity
- 📦 **Item icons** — All items have their icons with custom Wynncraft textures
- ⚡ **Async loading** — Data loads from API without game lag
- 📱 **Adaptive interface** — GUI looks the same at any gui scale value

## Rarity Colors

| Rarity | Color |
|--------|-------|
| Normal | Gray |
| Unique | Yellow |
| Rare | Light Purple |
| Legendary | Aqua |
| Fabled | Red |
| Mythic | Dark Purple |
| Set | Green |

## Installation

1. Install **Fabric Loader** for Minecraft 1.21.11
2. Download the mod and place the `.jar` file in the `mods` folder
3. Install dependencies:
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Cloth Config](https://modrinth.com/mod/cloth-config)
   - [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for settings)

## Settings

Settings available via **Mod Menu** → **WynnMarketSearch**:

| Option | Description | Default |
|--------|-------------|---------|
| Market Search | Enable search when message appears | ✅ |
| Auto Focus | Auto-focus search field when GUI opens | ✅ |

## Usage

1. Open the market on Wynncraft server
2. Click on an item to sell
3. When the message `Type the item name or type 'cancel' to cancel:` appears, the mod will automatically open the search GUI
4. Start typing the item name
5. Select the desired item from the list by clicking or press Enter to send the first result
6. To cancel, type `cancel` or close the GUI with ESC

## Requirements

- **Minecraft**: 1.21.11
- **Fabric Loader**: ≥ 0.18.4
- **Java**: 21+

## Dependencies

- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config)
- [Mod Menu](https://modrinth.com/mod/modmenu) (optional)

## Building

```bash
# Clone repository
git clone https://github.com/a0g/WynnMarketSearch.git
cd WynnMarketSearch

# Build
./gradlew build

# Run client for testing
./gradlew runClient
```

The compiled `.jar` file will be in `build/libs/`.

## License

[CC0 1.0 Universal](LICENSE) — Public Domain

## Links

- [Source Code](https://github.com/a0g/WynnMarketSearch)
- [Wynncraft API](https://api.wynncraft.com/v3/item/database?fullResult)
- [Report a Bug](https://github.com/a0g/WynnMarketSearch/issues)
