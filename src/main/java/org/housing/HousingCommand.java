package org.housing;

import java.util.ArrayList;
import java.util.List;
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

public class HousingCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PlayerJoin playerJoin;
    private final ScoreboardManager scoreboardManager;
    private final Void voidSystem;
    private final GameModeManager gameModeManager;
    private final WorldLoader worldLoader;

    public HousingCommand(JavaPlugin plugin,
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

        // /housing  (no args)  →  join the DEFAULT mode (platform)
        if (args.length == 0) {
            if (sender instanceof Player) {
                return handleJoin(sender, new String[]{"join", "platform"});
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help"))    { sendHelp(sender); return true; }
        if (sub.equals("creator")) { return handleCreator(sender); }
        if (sub.equals("worlds"))  { return handleWorlds(sender); }
        if (sub.equals("join"))    { return handleJoin(sender, args); }

        if (sub.equals("reload")) {
            if (!sender.hasPermission("housing.reload")) { sendNoPerm(sender); return true; }
            this.plugin.reloadConfig();
            this.gameModeManager.reloadAll();
            if (this.scoreboardManager != null) this.scoreboardManager.reloadConfig();
            if (this.voidSystem != null) this.voidSystem.reloadConfig();
            sender.sendMessage(colorize("&bHousing configuration reloaded."));
            return true;
        }

        if (sub.equals("setworld")) {
            return handleSetWorld(sender, args);
        }

        GameMode mode = GameMode.fromId(sub);
        if (mode == null) {
            sender.sendMessage(colorize("&cUnknown subcommand. Use /housing help"));
            return true;
        }

        if (args.length < 2) { sendModeHelp(sender, mode); return true; }

        String action = args[1].toLowerCase();

        if (action.equals("setspawn"))      return handleSetSpawn(sender, mode);
        if (action.equals("setvoid"))       return handleSetVoid(sender, mode, args);
        if (action.equals("setzshowsword")) return handleSetZShowSword(sender, mode, args);
        if (action.equals("setpvpzone"))    return handleSetPvpZone(sender, mode, args);
        if (action.equals("kit"))           return handleKit(sender, mode, args);
        if (action.equals("sb") || action.equals("scoreboard"))
                                            return handleScoreboard(sender, mode, args);

        sender.sendMessage(colorize("&cUnknown action. Use /housing help"));
        return true;
    }

    private boolean handleWorlds(CommandSender sender) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bHousing &f- &bLoaded Worlds"));
        sender.sendMessage(colorize("&b&m----------------------------------"));

        for (World w : Bukkit.getWorlds()) {
            GameMode mode = gameModeManager.getModeForWorld(w);
            sender.sendMessage(colorize("&b" + w.getName() + " &f» &b" + mode.getDisplayName()));
        }

        sender.sendMessage(colorize("&b&m----------------------------------"));
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use /housing join."));
            return true;
        }

        if (!sender.hasPermission("housing.join")) { sendNoPerm(sender); return true; }

        if (args.length < 2) {
            sender.sendMessage(colorize("&cUsage: /housing join <mode>"));
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

        String worldName = gameModeManager.getWorldForMode(mode);

        if (worldName == null) {
            player.sendMessage(colorize("&bMode &f" + mode.getDisplayName()
                    + "&b has no world yet. Auto-creating..."));
            World world = worldLoader.ensureLoaded(mode.getId());
            if (world == null) {
                player.sendMessage(colorize("&cCould not create world for &e" + mode.getId()));
                return true;
            }
            gameModeManager.setWorldMode(mode.getId(), mode);
            worldName = world.getName();
        }

        final World targetWorld = worldLoader.ensureLoaded(worldName);
        if (targetWorld == null) {
            player.sendMessage(colorize("&cWorld not available: &e" + worldName));
            return true;
        }

        final GameMode finalMode = mode;

        // Same world → just refresh kit for that mode
        if (player.getWorld().equals(targetWorld)) {
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            playerJoin.giveKitForMode(player, finalMode);
            player.updateInventory();
            player.sendMessage(colorize("&bKit refreshed for &f"
                    + finalMode.getDisplayName() + "&b."));
            return true;
        }

        Location spawn = playerJoin.getSpawnLocation(targetWorld);
        if (spawn == null) {
            spawn = targetWorld.getSpawnLocation();
        }

        // 1) Clear inventory IMMEDIATELY (before teleport)
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();

        // 2) Teleport to target world
        player.teleport(spawn);

        // 3) Give the TARGET mode's kit after teleport is complete
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                playerJoin.giveKitForMode(player, finalMode);
                player.updateInventory();
            }
        }, 5L);

        player.sendMessage(colorize("&bJoined &f" + finalMode.getDisplayName()
                + " &b(world: &f" + targetWorld.getName() + "&b)"));
        return true;
    }

    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("housing.setworld")) { sendNoPerm(sender); return true; }

        String worldName;
        GameMode mode;

        if (args.length == 2) {
            mode = GameMode.fromId(args[1]);
            if (mode == null) {
                sender.sendMessage(colorize("&cUnknown mode: &e" + args[1]));
                sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cFrom console use: /housing setworld <world> <mode>"));
                return true;
            }
            worldName = ((Player) sender).getWorld().getName();
        }
        else if (args.length >= 3) {
            worldName = args[1];
            mode = GameMode.fromId(args[2]);
            if (mode == null) {
                sender.sendMessage(colorize("&cUnknown mode: &e" + args[2]));
                sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
                return true;
            }
        }
        else {
            sender.sendMessage(colorize("&cUsage: /housing setworld [world] <mode>"));
            sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
            return true;
        }

        World world = worldLoader.findLoaded(worldName);
        if (world == null) {
            sender.sendMessage(colorize("&bWorld '&f" + worldName
                    + "&b' is not loaded. Auto-loading..."));
            world = worldLoader.ensureLoaded(worldName);
        }

        if (world == null) {
            sender.sendMessage(colorize("&cCould not load or create world: &e" + worldName));
            sender.sendMessage(colorize("&7Loaded worlds: &f" + worldLoader.listLoadedWorldNames()));
            return true;
        }

        String finalWorldName = world.getName().toLowerCase();
        gameModeManager.setWorldMode(finalWorldName, mode);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.getWorld().equals(world)) {
                player.teleport(world.getSpawnLocation());
            }
            player.sendMessage(colorize("&bWorld &f" + world.getName()
                    + " &bis now game mode &f" + mode.getDisplayName() + "&b."));
            player.sendMessage(colorize("&7Next: &f/housing " + mode.getId() + " setspawn"));
        } else {
            sender.sendMessage(colorize("&bWorld &f" + world.getName()
                    + " &bis now game mode &f" + mode.getDisplayName() + "&b."));
        }

        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, GameMode mode) {
        if (!sender.hasPermission("housing.setspawn")) { sendNoPerm(sender); return true; }

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

        player.sendMessage(colorize("&b[" + mode.getDisplayName() + "] &fSpawn set to &b"
                + loc.getWorld().getName() + " "
                + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "&b."));
        return true;
    }

    private boolean handleSetVoid(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("housing.setvoid")) { sendNoPerm(sender); return true; }

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
                sender.sendMessage(colorize("&cUsage from console: /housing "
                        + mode.getId() + " setvoid <y>"));
                return true;
            }
            y = ((Player) sender).getLocation().getY();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("void.kill-height", Double.valueOf(y));
        gameModeManager.saveModeConfig(mode);

        sender.sendMessage(colorize("&b[" + mode.getDisplayName() + "] &fVoid kill height set to &b"
                + y + "&b."));
        return true;
    }

    private boolean handleSetZShowSword(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("housing.setzshowsword")) { sendNoPerm(sender); return true; }

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
                sender.sendMessage(colorize("&cUsage: /housing onewide setzshowsword <z>"));
                return true;
            }
            z = ((Player) sender).getLocation().getZ();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("zshowsword", Double.valueOf(z));
        gameModeManager.saveModeConfig(mode);

        sender.sendMessage(colorize("&b[OneWide] &fSword will appear once Z passes &b" + z + "&b."));
        return true;
    }

    // ============================================================
    //  /housing <mode> setpvpzone [x]
    //  PvP is allowed only when the player's X >= zone.
    //  Supported: Platform, OneWide
    // ============================================================
    private boolean handleSetPvpZone(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("housing.setpvpzone")) { sendNoPerm(sender); return true; }

        if (mode != GameMode.PLATFORM && mode != GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&csetpvpzone is only for Platform and OneWide modes."));
            return true;
        }

        double x;
        if (args.length >= 3) {
            try {
                x = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(colorize("&cInvalid number: &e" + args[2]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage: /housing " + mode.getId() + " setpvpzone <x>"));
                return true;
            }
            x = ((Player) sender).getLocation().getX();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("pvpzone.enabled", Boolean.valueOf(true));
        cfg.set("pvpzone.x", Double.valueOf(x));
        gameModeManager.saveModeConfig(mode);

        sender.sendMessage(colorize("&b[" + mode.getDisplayName()
                + "] &fPvP zone set — PvP enabled from X &b" + x + "&f."));
        return true;
    }

    private boolean handleKit(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("housing.kit")) { sendNoPerm(sender); return true; }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(colorize("&cPlayer not found: &e" + args[2]));
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                sender.sendMessage(colorize("&cUsage from console: /housing "
                        + mode.getId() + " kit <player>"));
                return true;
            }
            target = (Player) sender;
        }

        playerJoin.giveKitForMode(target, mode);

        if (sender.equals(target)) {
            sender.sendMessage(colorize("&bYour &f" + mode.getDisplayName() + " &bkit has been restored."));
        } else {
            sender.sendMessage(colorize("&bGave &f" + mode.getDisplayName() + " &bkit to &f"
                    + target.getName() + "&b."));
            target.sendMessage(colorize("&bYour &f" + mode.getDisplayName()
                    + " &bkit has been restored."));
        }
        return true;
    }

    private boolean handleScoreboard(CommandSender sender, GameMode mode, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(colorize("&cOnly players can use the scoreboard command."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 3 && args[2].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("housing.reload")) { sendNoPerm(sender); return true; }
            gameModeManager.reloadMode(mode);
            if (this.scoreboardManager != null) this.scoreboardManager.reloadConfig();
            player.sendMessage(colorize("&b[" + mode.getDisplayName()
                    + "] &fScoreboard configuration reloaded."));
            return true;
        }

        boolean nowVisible = this.scoreboardManager.toggleScoreboard(player);
        if (nowVisible) player.sendMessage(colorize("&bScoreboard &fENABLED&b."));
        else            player.sendMessage(colorize("&cScoreboard &fDISABLED&c."));
        return true;
    }

    private boolean handleCreator(CommandSender sender) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bHousing &f- &bCreated by &fMuvixo"));
        sender.sendMessage(colorize("&bVersion: &f1.0"));
        sender.sendMessage(colorize("&bModes: &fPlatform, LowMid, OneWide, BlockFight"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bHousing &f- &bCommands"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b/housing &f- Join the default mode (Platform)"));
        sender.sendMessage(colorize("&b/housing join <mode> &f- Join a game mode"));
        sender.sendMessage(colorize("&b/housing worlds &f- List loaded worlds"));
        sender.sendMessage(colorize("&b/housing creator &f- Show plugin credits"));
        sender.sendMessage(colorize("&b/housing reload &f- Reload all configs"));
        sender.sendMessage(colorize("&b/housing setworld [world] <mode> &f- Assign a world"));
        sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b/housing <mode> setspawn &f- Set spawn"));
        sender.sendMessage(colorize("&b/housing <mode> setvoid [y] &f- Set void Y"));
        sender.sendMessage(colorize("&b/housing <mode> setpvpzone <x> &f- Set PvP zone"));
        sender.sendMessage(colorize("&b/housing <mode> kit [player] &f- Give kit"));
        sender.sendMessage(colorize("&b/housing <mode> sb &f- Toggle scoreboard"));
        sender.sendMessage(colorize("&b/housing onewide setzshowsword <z> &f- OneWide only"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
    }

    private void sendModeHelp(CommandSender sender, GameMode mode) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bHousing &f- &b" + mode.getDisplayName()));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b/housing join " + mode.getId()));
        sender.sendMessage(colorize("&b/housing " + mode.getId() + " setspawn"));
        sender.sendMessage(colorize("&b/housing " + mode.getId() + " setvoid [y]"));
        if (mode == GameMode.PLATFORM || mode == GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&b/housing " + mode.getId() + " setpvpzone <x>"));
        }
        sender.sendMessage(colorize("&b/housing " + mode.getId() + " kit [player]"));
        sender.sendMessage(colorize("&b/housing " + mode.getId() + " sb [reload]"));
        if (mode == GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&b/housing onewide setzshowsword <z>"));
        }
        sender.sendMessage(colorize("&b&m----------------------------------"));
    }

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
                if (mode == GameMode.PLATFORM || mode == GameMode.ONEWIDE) actions.add("setpvpzone");

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