# Realstics Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

**Created by Muvixo**

## Game Modes

| Mode | Status | Description |
|------|--------|-------------|
| **Platform** | Default / enabled | Leather + Iron armor (Prot III), Wooden Sword (Sharp I) |
| **LowMid** | Must be set up | Wooden Sword only (Sharp I) |
| **OneWide** | Must be set up | Iron Sword, hidden in spawn zone |
| **BlockFight** | Must be set up | Diamond Sword (Sharp IV), 64 Light Blue Wool (infinite), Shears |

## Features

- **Auto-loads worlds** — no need to edit bukkit.yml
- **`/realstics join <mode>`** — players teleport with one command
- PvP with no HP loss (knockback works)
- No fall damage, infinite food
- **BlockFight**: only light-blue wool breakable, no drop, infinite
- **Aqua + White theme** in all configs by default
- Auto-fills empty messages on plugin update
- Per-mode kits, configs, scoreboards

## Commands

```
/realstics join <mode>                  - Join a game mode
/realstics worlds                       - List loaded worlds
/realstics setworld [world] <mode>      - Assign world
/realstics <mode> setspawn
/realstics <mode> setvoid [y]
/realstics <mode> kit [player]
/realstics <mode> sb [reload]
/realstics onewide setzshowsword <z>
/realstics reload
/realstics creator
/realstics help
```

Aliases: `/rs`, `/rl`

## Quick Setup

```
# Admin — first time
/realstics setworld onewide
/realstics onewide setspawn

# Player — join any mode
/realstics join onewide
/realstics join lowmid
/realstics join blockfight
/realstics join platform
```

## Building

```bash
mvn clean package
```

Output: `target/Realstics.jar`

Or push to GitHub — Actions builds automatically.

## Credits

- **Muvixo** — Creator
