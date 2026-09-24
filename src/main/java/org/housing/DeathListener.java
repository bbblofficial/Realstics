package org.housing;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class DeathListener implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    public DeathListener(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    // ============================================================
    //  CUSTOM DEATH MESSAGE
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        FileConfiguration cfg = plugin.getConfig();

        // If custom death messages are disabled, do nothing
        if (!cfg.getBoolean("death.enabled", true)) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            // ---- Killed by another player ----
            String format = cfg.getString("death.killed-by-player",
                    "&c%victim% &7was slain by &c%killer%");
            event.setDeathMessage(colorize(format
                    .replace("%victim%", victim.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%world%", victim.getWorld().getName())));
        } else if (victim.getLastDamageCause() != null
                && victim.getLastDamageCause().getEntity() != null) {
            // ---- Killed by a mob / entity ----
            String format = cfg.getString("death.killed-by-mob",
                    "&c%victim% &7was killed by &c%killer%");
            String entityName = victim.getLastDamageCause().getEntity().getType().name();
            event.setDeathMessage(colorize(format
                    .replace("%victim%", victim.getName())
                    .replace("%killer%", entityName)
                    .replace("%world%", victim.getWorld().getName())));
        } else {
            // ---- Default death (fall, void, etc.) ----
            String format = cfg.getString("death.default",
                    "&c%victim% &7died");
            event.setDeathMessage(colorize(format
                    .replace("%victim%", victim.getName())
                    .replace("%world%", victim.getWorld().getName())));
        }

        // ---- Respawn logic ----
        handleRespawn(victim);
    }

    // ============================================================
    //  RESPAWN LOGIC
    // ============================================================

    private void handleRespawn(final Player victim) {
        FileConfiguration cfg = plugin.getConfig();

        // If respawn-in-same-world is disabled, default behavior (lobby) applies
        if (!cfg.getBoolean("death.respawn-in-same-world", true)) return;

        final GameMode mode = gameModeManager.getModeForWorld(victim.getWorld());

        // Check if this mode is in the real-damage-modes list
        List<String> realModes = cfg.getStringList("death.real-damage-modes");
        boolean isRealDamageMode = false;

        if (realModes != null) {
            for (String m : realModes) {
                if (m != null && m.equalsIgnoreCase(mode.getId())) {
                    isRealDamageMode = true;
                    break;
                }
            }
        }

        // If NOT a real damage mode → let PlayerJoin send them to lobby
        if (!isRealDamageMode) return;

        final World deathWorld = victim.getWorld();

        // Schedule 5 ticks later to override default respawn
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!victim.isOnline()) return;

                if (plugin instanceof Housing) {
                    PlayerJoin pj = ((Housing) plugin).getPlayerJoin();
                    if (pj != null) {
                        // ★ Same fallback logic as Void.java:
                        // Use mode spawn IF it belongs to the current world.
                        // Otherwise use the world's default spawn.
                        Location spawn = pj.getSpawnLocation(deathWorld);
                        if (spawn == null || !spawn.getWorld().equals(deathWorld)) {
                            spawn = deathWorld.getSpawnLocation();
                        }
                        victim.teleport(spawn);
                        pj.giveKitForMode(victim, mode);
                    }
                }
            }
        }, 5L);
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}