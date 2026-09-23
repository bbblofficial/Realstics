# Realstics Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

## Features

- 4 game modes in one JAR: **Platform, LowMid, OneWide, BlockFight**
- **Auto-loads worlds** — no need to edit bukkit.yml
- **`/realstics join <mode>`** — players teleport with one command
- PvP with no HP loss (knockback works)
- No fall damage, infinite food
- Per-mode kits, configs, scoreboards
- Global combo system + join/quit messages

## Commands

```
/realstics join <mode>                  - Join a game mode (teleport + kit)
/realstics worlds                       - List loaded worlds + modes
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
# Admin — in-game, first time only:
/realstics setworld onewide
/realstics onewide setspawn
/realstics onewide setvoid -13

# Player — join any mode:
/realstics join onewide
/realstics join lowmid
/realstics join blockfight
/realstics join platform
```

The plugin will auto-load the world if it exists in the server folder,
or create a new one named after the mode.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `realstics.join` | true | Join a mode with /realstics join |
| `realstics.setworld` | op | Assign a world to a mode |
| `realstics.setspawn` | op | Set spawn for a mode |
| `realstics.setvoid` | op | Set void Y |
| `realstics.reload` | op | Reload configs |
| `realstics.scoreboard` | true | Toggle scoreboard |
| `realstics.bypass` | op | Bypass all protection |
| `realstics.break` | false | Break blocks |
| `realstics.place` | false | Place blocks |
| `realstics.drop` | false | Drop items |

## Building

```bash
mvn clean package
```

Output: `target/Realstics.jar`

Or push to GitHub — Actions builds automatically.

## Credits

- **Muvixo** — Creator
