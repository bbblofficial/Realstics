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
 * All modes EXCEPT LowMid:
 *   - HP never drops (damage set to 0)
 *   - Fall damage off
 *
 * LowMid:
 *   - PvP damage is APPLIED (real damage)
 *   - Fall damage is APPLIED (real damage)
 *   - Other damage causes (fire, poison, drowning, etc.) are cancelled
 *
 * Platform / OneWide:
 *   - PvP is only allowed when the attacker's X >= pvpzone.x
 *   - Outside the zone, hits are cancelled entirely
 *
 * BlockFight:
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

    private GameMode modeOf(World world) {
        return gameModeManager.getModeForWorld(world);
    }

    private boolean isLowMid(World world) {
        return modeOf(world) == GameMode.LOWMID;
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

        Player victim = (Player) event.getEntity();
        World world = victim.getWorld();
        EntityDamageEvent.DamageCause cause = event.getCause();

        // Void damage is always ignored (handled by Void system)
        if (cause == EntityDamageEvent.DamageCause.VOID) return;

        if (isLowMid(world)) {
            // LowMid: allow FALL and ENTITY_ATTACK; cancel everything else
            if (cause == EntityDamageEvent.DamageCause.FALL) {
                return;
            }
            if (cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                return;
            }
            if (cause == EntityDamageEvent.DamageCause.PROJECTILE) {
                return;
            }

            event.setCancelled(true);
            event.setDamage(0);
            return;
        }

        // Other modes: cancel all damage
        event.setDamage(0);
    }

    // ============================================================
    //  PvP damage — zone-aware
    // ============================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        World world = victim.getWorld();

        // Non-player attacker
        if (!(event.getDamager() instanceof Player)) {
            if (isLowMid(world)) {
                event.setCancelled(true);
                event.setDamage(0);
            } else {
                if (!event.isCancelled()) {
                    event.setDamage(0);
                }
            }
            return;
        }

        Player attacker = (Player) event.getDamager();

        // ---- LowMid: apply real damage ----
        if (isLowMid(world)) {
            return;
        }

        // ---- Other modes: cosmetic PvP with zone check ----
        if (isInPvpZone(attacker)) {
            if (!event.isCancelled()) {
                event.setDamage(0);
            }
        } else {
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
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        Player player = (Player) event.getEntity();

        if (isLowMid(player.getWorld())) {
            return;
        }

        event.setCancelled(true);
        event.setDamage(0);
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