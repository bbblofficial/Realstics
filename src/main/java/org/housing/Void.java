package org.housing;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Void implements Listener {

    private final JavaPlugin plugin;
    private final PlayerJoin playerJoin;
    private final GameModeManager gameModeManager;

    private final Set<UUID> teleporting = new HashSet<UUID>();

    public Void(JavaPlugin plugin, PlayerJoin playerJoin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.playerJoin = playerJoin;
        this.gameModeManager = gameModeManager;
    }

    public void reloadConfig() {
    }

    public double getKillHeight(World world) {
        GameMode mode = gameModeManager.getModeForWorld(world);
        FileConfiguration cfg = gameModeManager.getConfig(mode);
        return cfg.getDouble("void.kill-height", -13.0D);
    }

    public void setKillHeight(World world, double y) {
        GameMode mode = gameModeManager.getModeForWorld(world);
        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("void.kill-height", Double.valueOf(y));
        gameModeManager.saveModeConfig(mode);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (player.isDead()) return;
        if (player.getHealth() <= 0) return;
        if (this.teleporting.contains(player.getUniqueId())) return;

        Location to = event.getTo();
        if (to == null) return;

        double killHeight = getKillHeight(to.getWorld());
        if (to.getY() < killHeight) {
            this.teleporting.add(player.getUniqueId());
            handleVoidFall(player);
        }
    }

    private void handleVoidFall(final Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
        player.setFireTicks(0);
        player.setFallDistance(0.0F);

        for (org.bukkit.potion.PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        Location spawn = playerJoin.getSpawnLocation(player.getWorld());
        if (spawn != null) {
            player.teleport(spawn);
        } else {
            player.teleport(player.getWorld().getSpawnLocation());
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    playerJoin.giveKit(player);
                }
            }
        }, 2L);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                teleporting.remove(player.getUniqueId());
            }
        }, 20L);
    }
}
