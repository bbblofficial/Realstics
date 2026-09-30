package org.housing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * CombatManager — LowMid combat-tag + HP restore.
 *
 * Behavior:
 *   - Whenever two players hit each other in a real-damage mode,
 *     both are tagged as "in combat".
 *   - When combat ends for a player (opponent died, logged out,
 *     or timer expired), that player is fully healed.
 *   - On a kill, the killer is fully healed immediately.
 */
public class CombatManager implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    /** player UUID → last opponent UUID */
    private final Map<UUID, UUID> lastOpponent = new HashMap<UUID, UUID>();

    /** player UUID → last hit timestamp (ms) */
    private final Map<UUID, Long> lastHitTime = new HashMap<UUID, Long>();

    /** players currently in combat */
    private final Set<UUID> inCombat = new HashSet<UUID>();

    /** combat tag duration (ms) */
    private long combatDurationMs = 15000L;

    public CombatManager(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
        loadConfig();
        startExpireTask();
    }

    public void loadConfig() {
        // Read from config (with fallback)
        long seconds = plugin.getConfig().getLong("combat.duration-seconds", 15L);
        if (seconds < 1L) seconds = 15L;
        this.combatDurationMs = seconds * 1000L;
    }

    public void reloadConfig() {
        loadConfig();
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private boolean isRealDamageMode(Player player) {
        if (player == null) return false;
        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());

        java.util.List<String> realModes =
                plugin.getConfig().getStringList("death.real-damage-modes");
        if (realModes == null) return false;
        for (String m : realModes) {
            if (m != null && m.equalsIgnoreCase(mode.getId())) return true;
        }
        return false;
    }

    private void heal(Player player) {
        if (player == null || !player.isOnline()) return;
        try {
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setExhaustion(0.0F);
            player.setFireTicks(0);
        } catch (Throwable ignored) {}
    }

    private void enterCombat(Player a, Player b) {
        long now = System.currentTimeMillis();

        this.inCombat.add(a.getUniqueId());
        this.inCombat.add(b.getUniqueId());

        this.lastOpponent.put(a.getUniqueId(), b.getUniqueId());
        this.lastOpponent.put(b.getUniqueId(), a.getUniqueId());

        this.lastHitTime.put(a.getUniqueId(), Long.valueOf(now));
        this.lastHitTime.put(b.getUniqueId(), Long.valueOf(now));
    }

    /**
     * End combat for a player and heal them.
     * Called when: opponent died / logged out / timer expired.
     */
    private void endCombat(Player player, boolean heal) {
        if (player == null) return;
        UUID id = player.getUniqueId();

        boolean was = this.inCombat.remove(id);
        this.lastOpponent.remove(id);
        this.lastHitTime.remove(id);

        if (was && heal) {
            heal(player);
        }
    }

    private void endCombat(UUID id, boolean heal) {
        Player p = Bukkit.getPlayer(id);
        if (p != null && p.isOnline()) {
            endCombat(p, heal);
        } else {
            this.inCombat.remove(id);
            this.lastOpponent.remove(id);
            this.lastHitTime.remove(id);
        }
    }

    // ============================================================
    //  Damage → tag combat
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        // Only real-damage modes (LowMid)
        if (!isRealDamageMode(victim)) return;
        if (!isRealDamageMode(attacker)) return;

        // Skip if damage is 0 (cosmetic)
        if (event.getFinalDamage() <= 0.0) return;

        enterCombat(victim, attacker);
    }

    // ============================================================
    //  Kill → heal the killer immediately
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();

        if (!isRealDamageMode(victim)) return;

        // 1) Heal the killer immediately
        Player killer = victim.getKiller();
        if (killer != null && killer.isOnline() && !killer.equals(victim)) {
            heal(killer);

            // Remove killer from combat too (fight is over)
            endCombat(killer, false);
        }

        // 2) Anyone who was fighting the victim → end combat + heal
        UUID victimId = victim.getUniqueId();
        UUID opponentId = this.lastOpponent.get(victimId);
        if (opponentId != null) {
            endCombat(opponentId, true);
        }

        // 3) Clean up victim
        endCombat(victim, false);
    }

    // ============================================================
    //  Quit → heal the opponent
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        // Was this player fighting someone?
        UUID opponentId = this.lastOpponent.get(id);
        if (opponentId != null) {
            // Opponent gets healed (combat ended because other logged out)
            endCombat(opponentId, true);
        }

        // Clean up the quitting player
        this.inCombat.remove(id);
        this.lastOpponent.remove(id);
        this.lastHitTime.remove(id);
    }

    // ============================================================
    //  Timer → expire old combat tags
    // ============================================================

    private void startExpireTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();

                // Snapshot to avoid ConcurrentModificationException
                Set<UUID> snapshot = new HashSet<UUID>(inCombat);

                for (UUID id : snapshot) {
                    Long last = lastHitTime.get(id);
                    if (last == null) {
                        inCombat.remove(id);
                        lastOpponent.remove(id);
                        continue;
                    }

                    if (now - last.longValue() >= combatDurationMs) {
                        // Combat expired → heal
                        endCombat(id, true);
                    }
                }
            }
        }.runTaskTimer(this.plugin, 20L, 20L);
    }

    // ============================================================
    //  Public API
    // ============================================================

    public boolean isInCombat(Player player) {
        return player != null && this.inCombat.contains(player.getUniqueId());
    }

    public long getRemainingCombatMs(Player player) {
        if (player == null) return 0L;
        Long last = this.lastHitTime.get(player.getUniqueId());
        if (last == null) return 0L;
        long elapsed = System.currentTimeMillis() - last.longValue();
        long remaining = combatDurationMs - elapsed;
        return remaining > 0 ? remaining : 0L;
    }
}