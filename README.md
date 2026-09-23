# Realstics Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

**Created by Muvixo**

## Game Modes

| Mode | Status | Description |
|------|--------|-------------|
| **Platform** | Default / enabled | Leather + Iron armor (Prot III), Wooden Sword (Sharp I) |
| **LowMid** | Must be set up | Wooden Sword only (Sharp I, 5.25 dmg) |
| **OneWide** | Must be set up | Iron Sword (6 dmg), hidden in spawn zone |
| **BlockFight** | Must be set up | Diamond Sword (Sharp IV, 12+ dmg), 64 Wool, Shears |

All modes share:
- PvP enabled, **no HP loss** (knockback + hits still work)
- No fall damage
- Infinite food
- Block / place / drop protection (permission-based)
- Auto kit restore
- Per-mode scoreboard
- Per-mode void system
- Global combo system
- Global join / quit messages

## Commands

```
/realstics help
/realstics creator
/realstics reload
/realstics setworld [world] <mode>          # assign a world to a mode

/realstics <mode> setspawn
/realstics <mode> setvoid [y]
/realstics <mode> kit [player]
/realstics <mode> sb [reload]
/realstics onewide setzshowsword <z>
```

Aliases: `/rs`, `/rl`

**Modes:** `platform`, `lowmid`, `onewide`, `blockfight`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `realstics.setworld` | op | Assign a world to a mode |
| `realstics.setspawn` | op | Set spawn for a mode |
| `realstics.setvoid` | op | Set void Y |
| `realstics.setzshowsword` | op | OneWide Z threshold |
| `realstics.kit` | op | Give a kit |
| `realstics.reload` | op | Reload configs |
| `realstics.scoreboard` | true | Toggle scoreboard |
| `realstics.bypass` | op | Bypass all protection |
| `realstics.break` | false | Break blocks |
| `realstics.place` | false | Place blocks |
| `realstics.drop` | false | Drop items |

## Setup

1. Drop the JAR into `plugins/`
2. Restart the server
3. All config files are auto-created with safe merging
4. Default world (`world`) uses **Platform** automatically
5. For other modes:
   ```
   # Standing in the target world:
   /realstics setworld lowmid
   /realstics lowmid setspawn
   /realstics lowmid setvoid -13

   # Or with explicit world name:
   /realstics setworld world_nether onewide
   /realstics onewide setspawn
   /realstics onewide setzshowsword 100
   ```

## Building

```bash
mvn clean package
```

Output: `target/Realstics.jar`

Or push to GitHub — the included workflow builds automatically.

## Credits

- **Muvixo** — Creator
