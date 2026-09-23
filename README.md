# Realstics Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

## Features

- 4 game modes in one JAR: **Platform, LowMid, OneWide, BlockFight**
- **Auto-loads worlds** — no need to edit bukkit.yml
- PvP with no HP loss (knockback works)
- No fall damage, infinite food
- Per-mode kits, configs, scoreboards
- Global combo system + join/quit messages

## Commands

```
/realstics worlds                       - List loaded worlds + assigned modes
/realstics setworld [world] <mode>      - Assign world (auto-loads if needed)
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
# In-game, standing in a world:
/realstics setworld onewide
/realstics onewide setspawn
/realstics onewide setvoid -13

# Or with explicit world:
/realstics setworld world_nether lowmid
```

The plugin will auto-load the world if it exists in the server folder.

## Building

```bash
mvn clean package
```

Or push to GitHub — Actions builds automatically.

## Credits

- **Muvixo** — Creator
