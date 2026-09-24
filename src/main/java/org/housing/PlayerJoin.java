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

                String pendingMode = null;
                if (plugin instanceof Housing) {
                    pendingMode = ((Housing) plugin)
                            .consumePendingMode(player.getUniqueId());
                }

                if (pendingMode != null) {
                    player.performCommand("housing join " + pendingMode);
                    return;
                }

                // Default: kit for current world + spawn + menu
                giveKit(player);
                teleportToSpawn(player);

                if (plugin instanceof Housing) {
                    ModeMenu menu = ((Housing) plugin).getModeMenu();
                    if (menu != null) menu.giveMenuItem(player);
                }
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
                giveKit(player);

                if (plugin instanceof Housing) {
                    ModeMenu menu = ((Housing) plugin).getModeMenu();
                    if (menu != null) menu.giveMenuItem(player);
                }
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
            return;
        }

        // Fallback (در صورتی که فایل کیت خراب باشد)
        plugin.getLogger().warning("[Housing] Kit file missing for mode: " + mode.getId());
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