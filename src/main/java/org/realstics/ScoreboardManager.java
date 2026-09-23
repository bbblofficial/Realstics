package org.realstics;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Per-world scoreboard driven by each mode's scoreboard file.
 *   Platform  -> scoreboard.yml
 *   LowMid    -> sb-lowmid.yml
 *   OneWide   -> sb-onewide.yml
 *   BlockFight-> sb-blockfight.yml
 */
public class ScoreboardManager implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private final Map<UUID, Scoreboard> playerBoards = new HashMap<UUID, Scoreboard>();
    private final Set<UUID> hiddenPlayers = new HashSet<UUID>();

    private int animationFrame = 0;
    private int taskId = -1;

    private static final int MAX_LINES = 15;

    public ScoreboardManager(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        startUpdateTask();
    }

    public void reloadConfig() {
        this.animationFrame = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            createScoreboard(player);
        }
    }

    private void startUpdateTask() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
        }
        // Use default config interval (Platform)
        int interval = plugin.getConfig().getInt("scoreboard.update-interval", 10);
        if (interval < 1) interval = 10;

        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                animationFrame++;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (hiddenPlayers.contains(player.getUniqueId())) continue;
                    updateScoreboard(player);
                }
            }
        }, interval, interval);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    createScoreboard(player);
                }
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.playerBoards.remove(id);
        this.hiddenPlayers.remove(id);
    }

    // ============================================================
    //  CREATE / UPDATE
    // ============================================================
    public void createScoreboard(Player player) {
        if (this.hiddenPlayers.contains(player.getUniqueId())) return;

        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());
        FileConfiguration sb = gameModeManager.getScoreboard(mode);

        if (!sb.getBoolean("enabled", true)) return;

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("realstics", "dummy");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(colorize(getTitle(sb)));

        for (int i = 0; i < MAX_LINES; i++) {
            Team team = board.registerNewTeam("line_" + i);
            String entry = getUniqueEntry(i);
            team.addEntry(entry);
        }

        this.playerBoards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        updateScoreboard(player);
    }

    public void updateScoreboard(Player player) {
        if (this.hiddenPlayers.contains(player.getUniqueId())) return;

        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());
        FileConfiguration sb = gameModeManager.getScoreboard(mode);

        if (!sb.getBoolean("enabled", true)) {
            Scoreboard existing = this.playerBoards.remove(player.getUniqueId());
            if (existing != null) {
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            }
            return;
        }

        Scoreboard board = this.playerBoards.get(player.getUniqueId());
        if (board == null) {
            createScoreboard(player);
            return;
        }

        Objective objective = board.getObjective("realstics");
        if (objective == null) {
            objective = board.registerNewObjective("realstics", "dummy");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        objective.setDisplayName(colorize(getTitle(sb)));

        List<String> lines = sb.getStringList("lines");
        if (lines == null) lines = new ArrayList<String>();
        if (lines.size() > MAX_LINES) lines = lines.subList(0, MAX_LINES);

        List<String> processed = new ArrayList<String>();
        for (String raw : lines) {
            processed.add(applyPlaceholders(player, raw));
        }

        for (int i = 0; i < MAX_LINES; i++) {
            Team team = board.getTeam("line_" + i);
            if (team == null) {
                team = board.registerNewTeam("line_" + i);
                team.addEntry(getUniqueEntry(i));
            }

            if (i < processed.size()) {
                String line = processed.get(i);
                String[] parts = splitLine(line);
                team.setPrefix(parts[0]);
                team.setSuffix(parts[1]);
            } else {
                team.setPrefix("");
                team.setSuffix("");
            }
        }

        for (int i = 0; i < MAX_LINES; i++) {
            String entry = getUniqueEntry(i);
            if (!objective.getScore(entry).isScoreSet()) {
                objective.getScore(entry).setScore(MAX_LINES - i);
            }
        }
    }

    private String getTitle(FileConfiguration sb) {
        if (sb.getBoolean("title.animated", true)) {
            List<String> frames = sb.getStringList("title.frames");
            if (frames == null || frames.isEmpty()) return "&6&lRealstics";
            return frames.get(this.animationFrame % frames.size());
        }
        return sb.getString("title.static", "&6&lRealstics");
    }

    private String[] splitLine(String line) {
        if (line == null) return new String[]{"", ""};
        if (line.isEmpty()) return new String[]{"", ""};
        if (line.length() <= 16) return new String[]{line, ""};

        int splitAt = 16;
        if (line.charAt(splitAt - 1) == ChatColor.COLOR_CHAR) splitAt--;

        String prefix = line.substring(0, splitAt);
        String suffix = line.substring(splitAt);

        String lastColors = ChatColor.getLastColors(prefix);
        if (lastColors != null && !lastColors.isEmpty()) suffix = lastColors + suffix;

        if (suffix.length() > 16) {
            int end = 16;
            if (suffix.length() > 0 && suffix.charAt(end - 1) == ChatColor.COLOR_CHAR) end--;
            suffix = suffix.substring(0, end);
        }
        return new String[]{prefix, suffix};
    }

    private String getUniqueEntry(int index) {
        ChatColor[] colors = ChatColor.values();
        ChatColor c1 = colors[index % colors.length];
        ChatColor c2 = colors[(index / colors.length) % colors.length];
        return c1.toString() + c2.toString() + ChatColor.RESET;
    }

    private String applyPlaceholders(Player player, String line) {
        if (line == null) return "";
        int online = Bukkit.getOnlinePlayers().size();
        int maxOnline = Bukkit.getMaxPlayers();
        String world = player.getWorld().getName();
        int ping = getPing(player);
        int health = (int) Math.ceil(player.getHealth());
        int food = player.getFoodLevel();
        int y = player.getLocation().getBlockY();

        String mode = gameModeManager.getModeForWorld(player.getWorld()).getDisplayName();

        String out = line;
        out = out.replace("%player%", player.getName());
        out = out.replace("%online%", String.valueOf(online));
        out = out.replace("%max_online%", String.valueOf(maxOnline));
        out = out.replace("%world%", world);
        out = out.replace("%mode%", mode);
        out = out.replace("%ping%", String.valueOf(ping));
        out = out.replace("%health%", String.valueOf(health));
        out = out.replace("%food%", String.valueOf(food));
        out = out.replace("%y%", String.valueOf(y));
        return colorize(out);
    }

    private int getPing(Player player) {
        try {
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            return ((Integer) craftPlayer.getClass().getField("ping").get(craftPlayer)).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isHidden(Player player) {
        return this.hiddenPlayers.contains(player.getUniqueId());
    }

    public boolean toggleScoreboard(Player player) {
        UUID id = player.getUniqueId();
        if (this.hiddenPlayers.contains(id)) {
            this.hiddenPlayers.remove(id);
            createScoreboard(player);
            return true;
        } else {
            this.hiddenPlayers.add(id);
            this.playerBoards.remove(id);
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            return false;
        }
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void shutdown() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
        this.playerBoards.clear();
        this.hiddenPlayers.clear();
    }
}
