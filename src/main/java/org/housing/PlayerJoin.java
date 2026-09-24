package org.housing;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerJoin implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    public PlayerJoin(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    // ============================================================
    //  JOIN
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                String pendingMode = null;
                if (plugin instanceof Housing) {
                    pendingMode = ((Housing) plugin)
                            .consumePendingMode(player.getUniqueId());
                }

                if (pendingMode != null) {
                    player.performCommand("housing join " + pendingMode);
                    return;
                }

                joinLobby(player);
            }
        }, 40L);
    }

    // ============================================================
    //  RESPAWN — Single source of truth for respawn location
    //  Uses event.setRespawnLocation() so there's NO race.
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();

        // At this point (before respawn), the player is STILL in the
        // world they died in. Use that to determine the death mode.
        final World deathWorld = player.getWorld();
        final GameMode deathMode = gameModeManager.getModeForWorld(deathWorld);

        FileConfiguration cfg = plugin.getConfig();
        boolean sameWorld = cfg.getBoolean("death.respawn-in-same-world", true);

        if (sameWorld) {
            // Check if this mode is a real-damage mode
            List<String> realModes = cfg.getStringList("death.real-damage-modes");
            boolean isRealDamageMode = false;
            if (realModes != null) {
                for (String m : realModes) {
                    if (m != null && m.equalsIgnoreCase(deathMode.getId())) {
                        isRealDamageMode = true;
                        break;
                    }
                }
            }

            if (isRealDamageMode) {
                // ---- Respawn in the SAME world ----
                Location spawn = getSpawnLocation(deathWorld);

                // Fallback: if the configured spawn is in a different world,
                // use the current world's default spawn
                if (spawn == null || !spawn.getWorld().equals(deathWorld)) {
                    spawn = deathWorld.getSpawnLocation();
                }

                // ★ Set the respawn location directly — no delayed teleport
                event.setRespawnLocation(spawn);

                final GameMode kitMode = deathMode;

                // Give the mode's kit after respawn
                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        giveKitForMode(player, kitMode);
                    }
                }, 5L);
                return;
            }
        }

        // ---- Default: respawn at lobby (Platform) ----
        String lobbyName = cfg.getString("protection.locked-world", "world");
        if (lobbyName == null || lobbyName.trim().isEmpty()) {
            lobbyName = "world";
        }

        World lobbyWorld = Bukkit.getWorld(lobbyName);
        if (lobbyWorld == null && plugin instanceof Housing) {
            WorldLoader loader = ((Housing) plugin).getWorldLoader();
            if (loader != null) {
                lobbyWorld = loader.ensureLoaded(lobbyName);
            }
        }

        if (lobbyWorld != null) {
            FileConfiguration platformCfg = gameModeManager.getConfig(GameMode.PLATFORM);
            Location spawn = readSpawn(platformCfg);
            if (spawn == null || !spawn.getWorld().equals(lobbyWorld)) {
                spawn = lobbyWorld.getSpawnLocation();
            }
            event.setRespawnLocation(spawn);

            Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) return;
                    giveKitForMode(player, GameMode.PLATFORM);
                }
            }, 5L);
        }
    }

    // ============================================================
    //  JOIN LOBBY — always teleport to the default world (Platform)
    // ============================================================

    public void joinLobby(final Player player) {
        if (player == null || !player.isOnline()) return;

        String lobbyName = plugin.getConfig()
                .getString("protection.locked-world", "world");
        if (lobbyName == null || lobbyName.trim().isEmpty()) {
            lobbyName = "world";
        }

        World lobbyWorld = Bukkit.getWorld(lobbyName);
        if (lobbyWorld == null && plugin instanceof Housing) {
            WorldLoader loader = ((Housing) plugin).getWorldLoader();
            if (loader != null) {
                lobbyWorld = loader.ensureLoaded(lobbyName);
            }
        }
        if (lobbyWorld == null) {
            plugin.getLogger().warning("[Housing] Lobby world '"
                    + lobbyName + "' not found!");
            return;
        }

        final World targetWorld = lobbyWorld;

        FileConfiguration platformCfg = gameModeManager.getConfig(GameMode.PLATFORM);
        Location spawn = readSpawn(platformCfg);
        if (spawn == null || !spawn.getWorld().equals(targetWorld)) {
            spawn = targetWorld.getSpawnLocation();
        }

        final Location finalSpawn = spawn;

        if (player.getWorld().equals(targetWorld)) {
            player.teleport(finalSpawn);
            giveKitForMode(player, GameMode.PLATFORM);
            player.updateInventory();
            return;
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();

        player.teleport(finalSpawn);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                giveKitForMode(player, GameMode.PLATFORM);
                player.updateInventory();
            }
        }, 5L);
    }

    // ============================================================
    //  Kit
    // ============================================================

    public void giveKit(Player player) {
        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());
        giveKitForMode(player, mode);
    }

    public void giveKitForMode(Player player, GameMode mode) {
        if (player == null || !player.isOnline()) return;
        if (mode == null) mode = GameMode.PLATFORM;

        FileConfiguration kit = gameModeManager.getKit(mode);
        if (kit != null) {
            KitLoader.applyKit(player, kit);
        } else {
            plugin.getLogger().warning("[Housing] Kit file missing for mode: " + mode.getId());
        }

        giveMenuItem(player);
    }

    public void giveMenuItem(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!(plugin instanceof Housing)) return;
        ModeMenu menu = ((Housing) plugin).getModeMenu();
        if (menu != null) menu.giveMenuItem(player);
    }

    // ============================================================
    //  Spawn
    // ============================================================

    public void teleportToSpawn(Player player) {
        Location spawn = getSpawnLocation(player.getWorld());
        if (spawn == null) return;
        player.teleport(spawn);
    }

    public Location getSpawnLocation(World world) {
        GameMode mode = gameModeManager.getModeForWorld(world);
        FileConfiguration config = gameModeManager.getConfig(mode);
        return readSpawn(config);
    }

    public Location readSpawn(FileConfiguration config) {
        if (config == null) return null;
        if (!config.contains("spawn.world")) return null;

        String worldName = config.getString("spawn.world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }
}