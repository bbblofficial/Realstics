# Housing Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

## Game Modes

| Mode | PvP Zone | Description |
|------|----------|-------------|
| **Platform** | ✅ Yes | Leather + Iron armor, Wooden Sword |
| **LowMid** | ❌ Always | Wooden Sword |
| **OneWide** | ✅ Yes | Iron Sword |
| **BlockFight** | ❌ Always | Diamond Sword, Light Blue Wool (infinite) |

## Commands

```
/housing join <mode>
/housing worlds
/housing setworld [world] <mode>
/housing <mode> setspawn
/housing <mode> setvoid [y]
/housing <mode> setpvpzone [z]
/housing <mode> kit [player]
/housing <mode> sb [reload]
/housing onewide setzshowsword <z>
/housing reload
/housing creator
/housing help
```

Aliases: `/hs`, `/hg`

## PvP Zone

Platform & OneWide support a Z-based PvP zone:

```
/housing platform setpvpzone -50
/housing onewide setpvpzone 100
```

## Building

```bash
mvn clean package
```

Output: `target/Housing.jar`

## Credits

- **Muvixo** — Creator
