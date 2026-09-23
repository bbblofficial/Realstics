# Realstics Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

## Features

- 4 game modes in one JAR: **Platform, LowMid, OneWide, BlockFight**
- **Auto-loads worlds** — no need to edit bukkit.yml
- **`/realstics join <mode>`** — players teleport with one command
- PvP with no HP loss (knockback works)
- No fall damage, infinite food
- **BlockFight**: wool can be placed AND broken; other blocks protected
- All other modes: only wool placeable, nothing breakable
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

## Credits

- **Muvixo** — Creator
