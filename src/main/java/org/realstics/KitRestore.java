package org.realstics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class KitRestore implements Listener {

    private final JavaPlugin plugin;
    private final PlayerJoin playerJoin;

    private long lastRestoreTime = 0L;
    private static final long RESTORE_COOLDOWN = 3000L;

    public KitRestore(JavaPlugin plugin, PlayerJoin playerJoin) {
        this.plugin = plugin;
        this.playerJoin = playerJoin;
        startSafetyTask();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage().toLowerCase();
        if (raw.startsWith("/")) raw = raw.substring(1);

        String[] parts = raw.split(" ");
        String cmd = parts[0];

        if (cmd.contains(":")) {
            cmd = cmd.substring(cmd.indexOf(':') + 1);
        }

        if (!cmd.equals("clear")) return;

        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                restore(player, true);
            }
        }, 20L);
    }

    private void startSafetyTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.CREATIVE) continue;
                    if (!isEmpty(player)) continue;
                    restore(player, false);
                }
            }
        }.runTaskTimer(this.plugin, 40L, 20L);
    }

    private void restore(Player player, boolean notifyPlayer) {
        if (player == null || !player.isOnline()) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        long now = System.currentTimeMillis();
        if (now - lastRestoreTime < RESTORE_COOLDOWN) return;
        lastRestoreTime = now;

        playerJoin.giveKit(player);

        if (notifyPlayer) {
            player.sendMessage(colorize("&bYour kit has been restored."));
        }
    }

    private boolean isEmpty(Player player) {
        if (player.getInventory().getHelmet() != null) return false;
        if (player.getInventory().getChestplate() != null) return false;
        if (player.getInventory().getLeggings() != null) return false;
        if (player.getInventory().getBoots() != null) return false;

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) return false;
        }
        return true;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
