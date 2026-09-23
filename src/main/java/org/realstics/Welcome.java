package org.realstics;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Welcome implements Listener {

    private final JavaPlugin plugin;

    public Welcome(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        event.setJoinMessage(null);

        FileConfiguration config = this.plugin.getConfig();
        String joinMessage = config.getString("join-message",
                "&b%player% &fjoined the game &7(&b%online%&7/&b%max_online%&7)");

        String rendered = colorize(joinMessage)
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_online%", String.valueOf(Bukkit.getMaxPlayers()));

        Bukkit.broadcastMessage(rendered);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null);

        FileConfiguration config = this.plugin.getConfig();
        String quitMessage = config.getString("quit-message",
                "&b%player% &fleft the game &7(&b%online%&7/&b%max_online%&7)");

        int onlineAfter = Math.max(0, Bukkit.getOnlinePlayers().size() - 1);

        String rendered = colorize(quitMessage)
                .replace("%player%", player.getName())
                .replace("%online%", String.valueOf(onlineAfter))
                .replace("%max_online%", String.valueOf(Bukkit.getMaxPlayers()));

        Bukkit.broadcastMessage(rendered);
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
