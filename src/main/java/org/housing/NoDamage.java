package org.housing;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cosmetic PvP rules + PvP-zone enforcement.
 *
 * All modes:
 *   - HP never drops (damage set to 0)
 *   - Fall damage off
 *   - Infinite food
 *
 * Platform / OneWide:
 *   - PvP is only allowed when the attacker's X >= pvpzone.x
 *   - Outside the zone, hits are cancelled entirely
 *
 * LowMid / BlockFight:
 *   - PvP always allowed (no zone check)
 */
public class NoDamage implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    public NoDamage(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    // ============================================================
    //  Zone check — X axis
    // ============================================================
    private boolean isInPvpZone(Player attacker) {
        World world = attacker.getWorld();
        GameMode mode = gameModeManager.getModeForWorld(world);

        // LowMid & BlockFight — always allow PvP
        if (mode == GameMode.LOWMID || mode == GameMode.BLOCKFIGHT) {
            return true;
        }

        // Platform / OneWide — read zone from config
        FileConfiguration cfg = gameModeManager.getConfig(mode);
        boolean enabled = cfg.getBoolean("pvpzone.enabled", true);
        if (!enabled) return true;

        double zoneX = cfg.getDouble("pvpzone.x", 0.0);
        return attacker.getLocation().getX() >= zoneX;
    }

    // ============================================================
    //  Main damage event
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID) return;
        event.setDamage(0);
    }

    // ============================================================
    //  PvP damage — zone-aware
    // ============================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // Non-player attacker → zero damage
        if (!(event.getDamager() instanceof Player)) {
            if (!event.isCancelled()) {
                event.setDamage(0);
            }
            return;
        }

        Player attacker = (Player) event.getDamager();

        if (isInPvpZone(attacker)) {
            // Normal PvP behavior — keep knockback, zero damage
            if (!event.isCancelled()) {
                event.setDamage(0);
            }
        } else {
            // Outside PvP zone — cancel entirely
            event.setCancelled(true);
            event.setDamage(0);
            attacker.sendMessage(colorize("&bYou must enter the PvP zone first!"));
        }
    }

    // ============================================================
    //  Fall damage
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    // ============================================================
    //  Infinite food
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.getFoodLevel() < 20) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setExhaustion(0.0F);
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}