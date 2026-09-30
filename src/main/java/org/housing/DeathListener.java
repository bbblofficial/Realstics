package org.housing;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

// ★ FIX: this file previously contained a full copy of Welcome.java
// (public class Welcome, 1-arg constructor) instead of a DeathListener
// class. That's why the build failed:
//   - "class Welcome is public, should be declared in a file named
//     Welcome.java" (class name didn't match the file name)
//   - "cannot access org.housing.DeathListener" in Housing.java,
//     because no such class actually existed
//
// Housing.java already calls: new DeathListener(this, this.gameModeManager)
// so this class is rebuilt with that exact constructor signature.
// It broadcasts a Housing-styled death message (falls back to nothing
// extra — vanilla message stays — if messages.yml has no matching key),
// similar in style to Welcome.java's join/quit messages.

public class DeathListener implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    public DeathListener(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    private Messages M() {
        if (plugin instanceof Housing) return ((Housing) plugin).getMessages();
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        GameMode mode = gameModeManager.getModeForWorld(victim.getWorld());
        String modeId = (mode != null) ? mode.getId() : "unknown";

        Messages m = M();
        if (m == null) return; // no Messages configured -> keep vanilla death message

        String rendered;
        if (killer != null && killer.isOnline() && !killer.equals(victim)) {
            rendered = m.msg("death-message-pvp",
                    "victim", victim.getName(),
                    "killer", killer.getName(),
                    "mode", modeId);
        } else {
            rendered = m.msg("death-message",
                    "victim", victim.getName(),
                    "mode", modeId);
        }

        if (rendered != null && !rendered.trim().isEmpty()) {
            event.setDeathMessage(null); // suppress vanilla message, we broadcast ours
            Bukkit.broadcastMessage(rendered);
        }
    }
}