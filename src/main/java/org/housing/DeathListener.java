package org.housing;

import org.bukkit.ChatColor;
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
    //  CUSTOM DEATH MESSAGE ONLY
    //  Respawn location is handled entirely by PlayerJoin.onRespawn.
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        FileConfiguration cfg = plugin.getConfig();

        if (!cfg.getBoolean("death.enabled", true)) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            String format = cfg.getString("death.killed-by-player",
                    "&c%victim% &7was slain by &c%killer%");
            event.setDeathMessage(colorize(format
                    .replace("%victim%", victim.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%world%", victim.getWorld().getName())));
        } else if (victim.getLastDamageCause() != null
                && victim.getLastDamageCause().getEntity() != null) {
            String format = cfg.getString("death.killed-by-mob",
                    "&c%victim% &7was killed by &c%killer%");
            String entityName = victim.getLastDamageCause().getEntity().getType().name();
            event.setDeathMessage(colorize(format
                    .replace("%victim%", victim.getName())
                    .replace("%killer%", entityName)
                    .replace("%world%", victim.getWorld().getName())));
        } else {
            String format = cfg.getString("death.default",
                    "&c%victim% &7died");
            event.setDeathMessage(colorize(format
                    .replace("%victim%", victim.getName())
                    .replace("%world%", victim.getWorld().getName())));
        }
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}