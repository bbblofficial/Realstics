package org.housing;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

    private Messages M() {
        if (plugin instanceof Housing) return ((Housing) plugin).getMessages();
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        event.setJoinMessage(null);

        Messages m = M();
        String rendered;
        if (m != null) {
            rendered = m.msg("join-message",
                    "player", player.getName(),
                    "online", String.valueOf(Bukkit.getOnlinePlayers().size()),
                    "max_online", String.valueOf(Bukkit.getMaxPlayers()));
        } else {
            rendered = "&b" + player.getName() + " &fjoined the game";
        }

        Bukkit.broadcastMessage(rendered);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        event.setQuitMessage(null);

        int onlineAfter = Math.max(0, Bukkit.getOnlinePlayers().size() - 1);

        Messages m = M();
        String rendered;
        if (m != null) {
            rendered = m.msg("quit-message",
                    "player", player.getName(),
                    "online", String.valueOf(onlineAfter),
                    "max_online", String.valueOf(Bukkit.getMaxPlayers()));
        } else {
            rendered = "&b" + player.getName() + " &fleft the game";
        }

        Bukkit.broadcastMessage(rendered);
    }
}