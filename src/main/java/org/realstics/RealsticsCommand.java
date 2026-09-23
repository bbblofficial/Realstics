package org.realstics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * /realstics <subcommand>
 *
 *   /realstics help
 *   /realstics creator
 *   /realstics worlds
 *   /realstics reload
 *   /realstics join <mode>                     - teleport player to mode world
 *   /realstics setworld [world] <mode>         - assign (auto-loads world)
 *
 *   /realstics <mode> setspawn
 *   /realstics <mode> setvoid [y]
 *   /realstics onewide setzshowsword <z>
 *   /realstics <mode> kit [player]
 *   /realstics <mode> sb [reload]
 */
public class RealsticsCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PlayerJoin playerJoin;
    private final ScoreboardManager scoreboardManager;
    private final Void voidSystem;
    private final GameModeManager gameModeManager;
    private final WorldLoader worldLoader;

    public RealsticsCommand(JavaPlugin plugin,
                            PlayerJoin playerJoin,
                            ScoreboardManager scoreboardManager,
                            Void voidSystem,
                            GameModeManager gameModeManager,
                            WorldLoader worldLoader) {
        this.plugin = plugin;
        this.playerJoin = playerJoin;
        this.scoreboardManager = scoreboardManager;
        this.voidSystem = voidSystem;
        this.gameModeManager = gameModeManager;
        this.worldLoader = worldLoader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) { sendHelp(sender); return true; }

        String sub = args[0].toLowerCase();

        if (sub.equals("help"))    { sendHelp(sender); return true; }
        if (sub.equals("creator")) { return handleCreator(sender); }
        if (sub.equals("worlds"))  { return handleWorlds(sender); }
        if (sub.equals("join"))    { return handleJoin(sender, args); }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("realstics.reload")) { sendNoPerm(sender); return true; }
            this.plugin.reloadConfig();
            this.gameModeManager.reloadAll();
            if (this.scoreboardManager != null) this.scoreboardManager.reloadConfig();
            if (this.voidSystem != null) this.voidSystem.reloadConfig();
            sender.sendMessage(colorize("&aRealstics configuration reloaded."));
            return true;
        }

        if (sub.equals("setworld")) {
            return handleSetWorld(sender, args);
        }

        GameMode mode = GameMode.fromId(sub);
        if (mode == null) {
            sender.sendMessage(colorize("&cUnknown subcommand. Use /realstics help"));
            return true;
        }

        if (args.length < 2) { sendModeHelp(sender, mode); return true; }

        String action = args[1].toLowerCase();

        if (action.equals("setspawn"))      return handleSetSpawn(sender, mode);
        if (action.equals("setvoid"))       return handleSetVoid(sender, mode, args);
        if (action.equals("setzshowsword")) return handleSetZShowSword(sender, mode, args);
        if (action.equals("kit"))           return handleKit(sender, mode, args);
        if (action.equals("sb") || action.equals("scoreboard"))
                                            return handleScoreboard(sender, mode, args);

        sender.sendMessage(colorize("&cUnknown action. Use /realstics help"));
        return true;
    }

    // ============================================================
    //  /realstics worlds
    // ============================================================
    private boolean handleWorlds(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lRealstics &7- &fLoaded Worlds"));
        sender.sendMessage(colorize("&8&m----------------------------------"));

        for (World w : Bukkit.getWorlds()) {
            GameMode mode = gameModeManager.getModeForWorld(w);
            sender.sendMessage(colorize("&e" + w.getName() + " &7→ &f" + mode.getDisplayName()));
        }

        sender.sendMessage(colorize("&8&m----------------------------------"));
        return true;
    }

    // ============================================================
    //  /realstics join <mode>
    //  Teleports the player to the world assigned to that mode.
    //  If no world is assigned yet, auto-creates one named after the mode.
    //  The kit + scoreboard are given automatically by PlayerJoin & ScoreboardManager.
    // ============================================================
    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use /realstics join."));
            return true;
        }

        if (!sender.hasPermission("realstics.join")) { sendNoPerm(sender); return true; }

        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /realstics join <mode>"));
            sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
            return true;
        }

        GameMode mode = GameMode.fromId(args[1]);
        if (mode == null) {
            sender.sendMessage(colorize("&cUnknown mode: &e" + args[1]));
            sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
            return true;
        }

        final Player player = (Player) sender;

        // ---- Find which world is assigned to this mode ----
        String worldName = gameModeManager.getWorldForMode(mode);

        // ---- If not assigned, auto-create/load a world named after the mode ----
        if (worldName == null) {
            player.sendMessage(colorize("&7Mode &e" + mode.getDisplayName()
                    + "&7 has no world yet. Auto-creating..."));
            World world = worldLoader.ensureLoaded(mode.getId());
            if (world == null) {
                player.sendMessage(colorize("&cCould not create world for &e" + mode.getId()));
                return true;
            }
            gameModeManager.setWorldMode(mode.getId(), mode);
            worldName = world.getName();
        }

        // ---- Load world (or fail) ----
        World targetWorld = worldLoader.ensureLoaded(worldName);
        if (targetWorld == null) {
            player.sendMessage(colorize("&cWorld not available: &e" + worldName));
            return true;
        }

        // ---- Already in that world? ----
        if (player.getWorld().equals(targetWorld)) {
            player.sendMessage(colorize("&7You are already in &e"
                    + mode.getDisplayName() + "&7."));
            return true;
        }

        // ---- Teleport ----
        Location spawn = playerJoin.getSpawnLocation(targetWorld);
        if (spawn == null) {
            spawn = targetWorld.getSpawnLocation();
        }

        final World finalWorld = targetWorld;
        player.teleport(spawn);

        // Give kit after teleport (PlayerJoin also handles this on join,
        // but we do it here too in case the player was already online).
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    playerJoin.giveKit(player);
                }
            }
        }, 3L);

        player.sendMessage(colorize("&aJoined &e" + mode.getDisplayName()
                + " &7(world: &f" + finalWorld.getName() + "&7)"));
        return true;
    }

    // ============================================================
    //  /realstics setworld [world] <mode>
    // ============================================================
    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("realstics.setworld")) { sendNoPerm(sender); return true; }

        String worldName;
        GameMode mode;

        // Case 1: /realstics setworld <mode>   → player's current world
        if (args.length == 2) {
            mode = GameMode.fromId(args[1]);
            if (mode == null) {
                sender.sendMessage(colorize("&cUnknown mode: &e" + args[1]));
                sender.sendMessage(colorize("&7Modes: platform, lowmid, onewide, blockfight"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cFrom console use: /realstics setworld <world> <mode>"));
                return true;
            }
            worldName = ((Player) sender).getWorld().getName();
        }
        // Case 2: /realstics setworld <world> <mode>  → explicit
        else if (args.length >= 3) {
            worldName = args[1];
            mode = GameMode.fromId(args[2]);
            if (mode == null) {
                sender.sendMessage(colorize("&cUnknown mode: &e" + args[2]));
                sender.sendMessage(colorize("&7Modes: platform, lowmid, onewide, blockfight"));
                return true;
            }
        }
        // Case 3: wrong args
        else {
            sender.sendMessage(colorize("&cUsage: /realstics setworld [world] <mode>"));
            sender.sendMessage(colorize("&7Modes: platform, lowmid, onewide, blockfight"));
            return true;
        }

        // Auto-load the world if not loaded
        World world = worldLoader.findLoaded(worldName);
        if (world == null) {
            sender.sendMessage(colorize("&7World '&e" + worldName
                    + "&7' is not loaded. Auto-loading..."));
            world = worldLoader.ensureLoaded(worldName);
        }

        if (world == null) {
            sender.sendMessage(colorize("&cCould not load or create world: &e" + worldName));
            sender.sendMessage(colorize("&7Loaded worlds: &e" + worldLoader.listLoadedWorldNames()));
            return true;
        }

        String finalWorldName = world.getName().toLowerCase();
        gameModeManager.setWorldMode(finalWorldName, mode);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.getWorld().equals(world)) {
                player.teleport(world.getSpawnLocation());
            }
            player.sendMessage(colorize("&aWorld &e" + world.getName()
                    + " &ais now game mode &e" + mode.getDisplayName() + "&a."));
            player.sendMessage(colorize("&7Next: &e/realstics " + mode.getId() + " setspawn"));
        } else {
            sender.sendMessage(colorize("&aWorld &e" + world.getName()
                    + " &ais now game mode &e" + mode.getDisplayName() + "&a."));
        }

        return true;
    }

    // ============================================================
    //  /realstics <mode> setspawn
    // ============================================================
    private boolean handleSetSpawn(CommandSender sender, GameMode mode) {
        if (!sender.hasPermission("realstics.setspawn")) { sendNoPerm(sender); return true; }

        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use setspawn."));
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        FileConfiguration config = gameModeManager.getConfig(mode);
        config.set("spawn.world", loc.getWorld().getName());
        config.set("spawn.x", Double.valueOf(loc.getX()));
        config.set("spawn.y", Double.valueOf(loc.getY()));
        config.set("spawn.z", Double.valueOf(loc.getZ()));
        config.set("spawn.yaw", Float.valueOf(loc.getYaw()));
        config.set("spawn.pitch", Float.valueOf(loc.getPitch()));
        gameModeManager.saveModeConfig(mode);

        player.sendMessage(colorize("&a[" + mode.getDisplayName() + "] Spawn set to &e"
                + loc.getWorld().getName() + " "
                + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "&a."));
        return true;
    }

    // ============================================================
    //  /realstics <mode> setvoid [y]
    // ============================================================
    private boolean handleSetVoid(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("realstics.setvoid")) { sendNoPerm(sender); return true; }

        double y;

        if (args.length >= 3) {
            try {
                y = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(colorize("&cInvalid number: &e" + args[2]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage from console: /realstics "
                        + mode.getId() + " setvoid <y>"));
                return true;
            }
            y = ((Player) sender).getLocation().getY();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("void.kill-height", Double.valueOf(y));
        gameModeManager.saveModeConfig(mode);

        sender.sendMessage(colorize("&a[" + mode.getDisplayName() + "] Void kill height set to &e"
                + y + "&a."));
        return true;
    }

    // ============================================================
    //  /realstics onewide setzshowsword <z>
    // ============================================================
    private boolean handleSetZShowSword(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("realstics.setzshowsword")) { sendNoPerm(sender); return true; }

        if (mode != GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&csetzshowsword is only for OneWide mode."));
            return true;
        }

        double z;
        if (args.length >= 3) {
            try {
                z = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(colorize("&cInvalid number: &e" + args[2]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage: /realstics onewide setzshowsword <z>"));
                return true;
            }
            z = ((Player) sender).getLocation().getZ();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("zshowsword", Double.valueOf(z));
        gameModeManager.saveModeConfig(mode);

        sender.sendMessage(colorize("&a[OneWide] Sword will appear once Z passes &e" + z + "&a."));
        return true;
    }

    // ============================================================
    //  /realstics <mode> kit [player]
    // ============================================================
    private boolean handleKit(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("realstics.kit")) { sendNoPerm(sender); return true; }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(colorize("&cPlayer not found: &e" + args[2]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage from console: /realstics "
                        + mode.getId() + " kit <player>"));
                return true;
            }
            target = (Player) sender;
        }

        playerJoin.giveKit(target);

        if (sender.equals(target)) {
            sender.sendMessage(colorize("&aYour " + mode.getDisplayName() + " kit has been restored."));
        } else {
            sender.sendMessage(colorize("&aGave " + mode.getDisplayName() + " kit to &e"
                    + target.getName() + "&a."));
            target.sendMessage(colorize("&aYour " + mode.getDisplayName()
                    + " kit has been restored."));
        }
        return true;
    }

    // ============================================================
    //  /realstics <mode> sb [reload]
    // ============================================================
    private boolean handleScoreboard(CommandSender sender, GameMode mode, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use the scoreboard command."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 3 && args[2].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("realstics.reload")) { sendNoPerm(sender); return true; }
            gameModeManager.reloadMode(mode);
            if (this.scoreboardManager != null) this.scoreboardManager.reloadConfig();
            player.sendMessage(colorize("&a[" + mode.getDisplayName()
                    + "] Scoreboard configuration reloaded."));
            return true;
        }

        boolean nowVisible = this.scoreboardManager.toggleScoreboard(player);
        if (nowVisible) player.sendMessage(colorize("&aScoreboard &lENABLED&a."));
        else            player.sendMessage(colorize("&cScoreboard &lDISABLED&c."));
        return true;
    }

    // ============================================================
    //  /realstics creator
    // ============================================================
    private boolean handleCreator(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lRealstics &7- &fCreated by &bMuvixo"));
        sender.sendMessage(colorize("&7Version: &f1.0"));
        sender.sendMessage(colorize("&7Modes: &fPlatform, LowMid, OneWide, BlockFight"));
        sender.sendMessage(colorize("&8&m----------------------------------"));
        return true;
    }

    // ============================================================
    //  HELP
    // ============================================================
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lRealstics &7- &fCommands"));
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&e/realstics join <mode> &7- Join a game mode"));
        sender.sendMessage(colorize("&e/realstics worlds &7- List loaded worlds"));
        sender.sendMessage(colorize("&e/realstics creator &7- Show plugin credits"));
        sender.sendMessage(colorize("&e/realstics reload &7- Reload all configs"));
        sender.sendMessage(colorize("&e/realstics setworld [world] <mode> &7- Assign a world"));
        sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&e/realstics <mode> setspawn &7- Set spawn"));
        sender.sendMessage(colorize("&e/realstics <mode> setvoid [y] &7- Set void Y"));
        sender.sendMessage(colorize("&e/realstics <mode> kit [player] &7- Give kit"));
        sender.sendMessage(colorize("&e/realstics <mode> sb &7- Toggle scoreboard"));
        sender.sendMessage(colorize("&e/realstics onewide setzshowsword <z> &7- OneWide only"));
        sender.sendMessage(colorize("&8&m----------------------------------"));
    }

    private void sendModeHelp(CommandSender sender, GameMode mode) {
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&6&lRealstics &7- &f" + mode.getDisplayName()));
        sender.sendMessage(colorize("&8&m----------------------------------"));
        sender.sendMessage(colorize("&e/realstics join " + mode.getId()));
        sender.sendMessage(colorize("&e/realstics " + mode.getId() + " setspawn"));
        sender.sendMessage(colorize("&e/realstics " + mode.getId() + " setvoid [y]"));
        sender.sendMessage(colorize("&e/realstics " + mode.getId() + " kit [player]"));
        sender.sendMessage(colorize("&e/realstics " + mode.getId() + " sb [reload]"));
        if (mode == GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&e/realstics onewide setzshowsword <z>"));
        }
        sender.sendMessage(colorize("&8&m----------------------------------"));
    }

    // ============================================================
    //  TAB COMPLETE
    // ============================================================
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<String>();
            subs.add("creator"); subs.add("help"); subs.add("worlds");
            subs.add("reload");  subs.add("setworld"); subs.add("join");
            subs.add("platform"); subs.add("lowmid"); subs.add("onewide"); subs.add("blockfight");

            String partial = args[0].toLowerCase();
            for (String s : subs) if (s.startsWith(partial)) out.add(s);
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("setworld")) {
                out.add("platform"); out.add("lowmid"); out.add("onewide"); out.add("blockfight");
                for (World w : Bukkit.getWorlds()) out.add(w.getName().toLowerCase());
                return out;
            }

            if (sub.equals("join")) {
                out.add("platform"); out.add("lowmid"); out.add("onewide"); out.add("blockfight");
                return out;
            }

            GameMode mode = GameMode.fromId(sub);
            if (mode != null) {
                List<String> actions = new ArrayList<String>();
                actions.add("setspawn"); actions.add("setvoid");
                actions.add("kit");      actions.add("sb");
                if (mode == GameMode.ONEWIDE) actions.add("setzshowsword");

                String partial = args[1].toLowerCase();
                for (String s : actions) if (s.startsWith(partial)) out.add(s);
            }
            return out;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (sub.equals("setworld")) {
                out.add("platform"); out.add("lowmid"); out.add("onewide"); out.add("blockfight");
                return out;
            }
            if (action.equals("kit")) {
                String partial = args[2].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName());
                return out;
            }
            if (action.equals("sb")) { out.add("reload"); return out; }
        }

        return out;
    }

    private void sendNoPerm(CommandSender sender) {
        sender.sendMessage(colorize("&cYou do not have permission to do this."));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
