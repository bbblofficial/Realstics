package org.housing;

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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // ---- Pending mode from external source? ----
                String pendingMode = null;
                if (plugin instanceof Housing) {
                    pendingMode = ((Housing) plugin)
                            .consumePendingMode(player.getUniqueId());
                }

                if (pendingMode != null) {
                    player.performCommand("housing join " + pendingMode);
                    return;
                }

                // ★★★ ALWAYS go to LOBBY (Platform / default world) ★★★
                joinLobby(player);
            }
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // ★ After death, respawn at the lobby too
                joinLobby(player);
            }
        }, 5L);
    }

    // ============================================================
    //  ★ JOIN LOBBY — always teleport to the default world (Platform)
    // ============================================================

    /**
     * Sends the player to the LOBBY world (default world = Platform).
     * This is used on join and respawn, so players never land
     * in their previous mode's world.
     */
    public void joinLobby(final Player player) {
        if (player == null || !player.isOnline()) return;

        // ---- Resolve lobby world ----
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

        // ---- Get Platform spawn from its config (which is the lobby spawn) ----
        FileConfiguration platformCfg = gameModeManager.getConfig(GameMode.PLATFORM);
        Location spawn = readSpawn(platformCfg);

        // fallback: use world's default spawn
        if (spawn == null || !spawn.getWorld().equals(targetWorld)) {
            spawn = targetWorld.getSpawnLocation();
        }

        final Location finalSpawn = spawn;

        // ---- Already in lobby world? Just refresh kit + menu ----
        if (player.getWorld().equals(targetWorld)) {
            player.teleport(finalSpawn);
            giveKitForMode(player, GameMode.PLATFORM);
            player.updateInventory();
            return;
        }

        // ---- Clear inventory BEFORE teleport ----
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();

        // ---- Teleport ----
        player.teleport(finalSpawn);

        // ---- Give kit + menu AFTER teleport ----
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                giveKitForMode(player, GameMode.PLATFORM); // gives kit + menu
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