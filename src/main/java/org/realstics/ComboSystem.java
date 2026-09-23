package org.realstics;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ComboSystem implements Listener {

    private static final String DEFAULT_MESSAGE =
            "&b&m-------------------------------\n"
          + "&bCOMBO &f%combo%x\n"
          + "&b%attacker% &fcomboed &b%victim% &7(&f%combo% &7combo)\n"
          + "&b&m-------------------------------";

    private final JavaPlugin plugin;

    private final Map<UUID, Integer> combos = new HashMap<UUID, Integer>();
    private final Map<UUID, Long> lastHitTime = new HashMap<UUID, Long>();

    private boolean enabled;
    private int comboStep;
    private long comboResetTime;
    private String broadcastMessage;
    private boolean soundEnabled;

    public ComboSystem(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfiguration();
        startResetTask();
    }

    public void loadConfiguration() {
        FileConfiguration config = this.plugin.getConfig();
        this.enabled        = config.getBoolean("combo.enabled", true);
        this.comboStep      = config.getInt("combo.step", 10);
        this.comboResetTime = config.getLong("combo.reset-time", 3000L);

        // Read message — fallback if missing / null / empty
        String msg = config.getString("combo.broadcast-message", null);
        if (msg == null || msg.trim().isEmpty()) {
            msg = DEFAULT_MESSAGE;
        }
        this.broadcastMessage = msg;

        this.soundEnabled = config.getBoolean("combo.sound-enabled", true);

        if (this.comboStep < 1) this.comboStep = 10;
        if (this.comboResetTime < 500L) this.comboResetTime = 3000L;

        // Convert literal "\n" from YAML into real newlines
        if (this.broadcastMessage != null) {
            this.broadcastMessage = this.broadcastMessage.replace("\\n", "\n");
        }
    }

    public void reloadConfig() {
        loadConfiguration();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!this.enabled) return;
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getDamager() instanceof Player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;

        Player attacker = (Player) event.getDamager();
        Player victim   = (Player) event.getEntity();

        UUID attackerId = attacker.getUniqueId();
        UUID victimId   = victim.getUniqueId();

        resetCombo(victimId);

        int combo = this.combos.containsKey(attackerId)
                ? this.combos.get(attackerId).intValue() + 1
                : 1;

        this.combos.put(attackerId, Integer.valueOf(combo));
        this.lastHitTime.put(attackerId, Long.valueOf(System.currentTimeMillis()));

        if (combo % this.comboStep == 0) {
            announceCombo(attacker, victim, combo);
        }
    }

    private void announceCombo(Player attacker, Player victim, int combo) {
        String raw = this.broadcastMessage
                .replace("%combo%", String.valueOf(combo))
                .replace("%attacker%", attacker.getName())
                .replace("%victim%", victim.getName());

        String colored = colorize(raw);
        String[] lines = colored.split("\\r?\\n");
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                p.sendMessage(line);
            }
        }

        if (this.soundEnabled) {
            playComboSound(combo);
        }
    }

    private void playComboSound(int combo) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (combo >= this.comboStep * 5) {
                p.playSound(p.getLocation(), Sound.FIREWORK_TWINKLE,  0.7F, 1.4F);
                p.playSound(p.getLocation(), Sound.ORB_PICKUP,        0.9F, 1.6F);
                p.playSound(p.getLocation(), Sound.LEVEL_UP,          1.0F, 1.5F);
            } else if (combo >= this.comboStep * 3) {
                p.playSound(p.getLocation(), Sound.LEVEL_UP,          0.9F, 1.5F);
                p.playSound(p.getLocation(), Sound.ORB_PICKUP,        0.8F, 1.4F);
            } else if (combo >= this.comboStep * 2) {
                p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT,    0.7F, 1.2F);
                p.playSound(p.getLocation(), Sound.ORB_PICKUP,        0.8F, 0.9F);
            } else {
                p.playSound(p.getLocation(), Sound.ORB_PICKUP,        0.7F, 1.2F);
            }
        }
    }

    private void resetCombo(UUID playerId) {
        this.combos.remove(playerId);
        this.lastHitTime.remove(playerId);
    }

    private void startResetTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<UUID, Long>> it = lastHitTime.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, Long> entry = it.next();
                    if (now - entry.getValue().longValue() > comboResetTime) {
                        combos.remove(entry.getKey());
                        it.remove();
                    }
                }
            }
        }.runTaskTimer(this.plugin, 20L, 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.combos.remove(id);
        this.lastHitTime.remove(id);
    }

    public int getCombo(Player player) {
        if (!this.combos.containsKey(player.getUniqueId())) return 0;
        return this.combos.get(player.getUniqueId()).intValue();
    }

    public void clearCombo(Player player) {
        resetCombo(player.getUniqueId());
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
