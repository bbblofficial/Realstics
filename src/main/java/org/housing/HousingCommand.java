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

    private Messages M() {
        if (plugin instanceof Housing) return ((Housing) plugin).getMessages();
        return null;
    }

    // ============================================================
    //  COMMAND HANDLER
    // ============================================================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            if (sender instanceof Player) {
                return joinDefaultWorld(sender);
            }
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("help"))          { sendHelp(sender); return true; }
        if (sub.equals("creator"))       { return handleCreator(sender); }
        if (sub.equals("worlds"))        { return handleWorlds(sender); }
        if (sub.equals("join"))          { return handleJoin(sender, args); }
        if (sub.equals("menu"))          { return handleMenu(sender); }
        if (sub.equals("lobby"))         { return handleLobby(sender); }
        if (sub.equals("setlobbyspawn")) { return handleSetLobbySpawn(sender); }
        if (sub.equals("buildmode"))     { return handleBuildMode(sender, args); }

        if (sub.equals("reload")) {
            if (!sender.hasPermission(getPerm("reload", "housing.reload"))) {
                sendNoPerm(sender); return true;
            }
            this.plugin.reloadConfig();
            this.gameModeManager.reloadAll();
            if (this.scoreboardManager != null) this.scoreboardManager.reloadConfig();
            if (this.voidSystem != null) this.voidSystem.reloadConfig();

            if (plugin instanceof Housing) {
                Housing h = (Housing) plugin;
                if (h.getMessages() != null) h.getMessages().reload();
                ModeMenu menu = h.getModeMenu();
                if (menu != null) menu.reloadConfig();
            }

            Messages m = M();
            if (m != null) m.send(sender, "general.reload-success");
            return true;
        }

        if (sub.equals("setworld")) {
            return handleSetWorld(sender, args);
        }

        GameMode mode = GameMode.fromId(sub);
        if (mode == null) {
            Messages m = M();
            if (m != null) m.send(sender, "general.unknown-subcommand");
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

        Messages m = M();
        if (m != null) m.send(sender, "general.unknown-action");
        return true;
    }

    // ============================================================
    //  PERMISSION HELPER
    // ============================================================
    private String getPerm(String action, String defaultPerm) {
        String path = "permissions." + action;
        String value = plugin.getConfig().getString(path);
        if (value == null || value.trim().isEmpty()) {
            return defaultPerm;
        }
        return value.trim();
    }

    private void sendNoPerm(CommandSender sender) {
        Messages m = M();
        if (m != null) m.send(sender, "general.no-permission");
        else sender.sendMessage(ChatColor.RED + "No permission.");
    }

    // ============================================================
    //  BUILD MODE
    // ============================================================

    private boolean handleBuildMode(CommandSender sender, String[] args) {
        Messages m = M();
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }
        if (!sender.hasPermission(getPerm("buildmode", "housing.buildmode"))) {
            sendNoPerm(sender); return true;
        }

        Player player = (Player) sender;

        boolean enable;
        if (args.length >= 2) {
            String mode = args[1].toLowerCase();
            if (mode.equals("on") || mode.equals("true") || mode.equals("1")) {
                enable = true;
            } else if (mode.equals("off") || mode.equals("false") || mode.equals("0")) {
                enable = false;
            } else {
                enable = !((Housing) plugin).isInBuildMode(player.getUniqueId());
            }
        } else {
            enable = !((Housing) plugin).isInBuildMode(player.getUniqueId());
        }

        ((Housing) plugin).setBuildMode(player.getUniqueId(), enable);

        if (m != null) {
            player.sendMessage(m.get("buildmode.header"));
            if (enable) {
                player.sendMessage(m.get("buildmode.enabled-title"));
                player.sendMessage(m.get("buildmode.enabled-desc"));
                player.sendMessage(m.get("buildmode.enabled-note"));
            } else {
                long seconds = plugin.getConfig()
                        .getLong("protection.placed-decay-seconds", 5L);
                player.sendMessage(m.get("buildmode.disabled-title"));
                player.sendMessage(m.get("buildmode.disabled-desc"));
                player.sendMessage(m.msg("buildmode.disabled-note",
                        "seconds", String.valueOf(seconds)));
            }
            player.sendMessage(m.get("buildmode.header"));
        }
        return true;
    }

    // ============================================================
    //  /housing (no args)
    // ============================================================

    private boolean joinDefaultWorld(CommandSender sender) {
        Messages m = M();
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }
        if (!sender.hasPermission(getPerm("join", "housing.join"))) {
            sendNoPerm(sender); return true;
        }

        final Player player = (Player) sender;
        final GameMode mode = GameMode.PLATFORM;

        String defaultWorldName = plugin.getConfig()
                .getString("protection.locked-world", "world");
        if (defaultWorldName == null || defaultWorldName.trim().isEmpty()) {
            defaultWorldName = "world";
        }

        if (player.getWorld().getName().equalsIgnoreCase(defaultWorldName)) {
            if (m != null) m.send(player, "mode.already-in",
                    "mode", mode.getDisplayName());
            return true;
        }

        World targetWorld = worldLoader.findLoaded(defaultWorldName);
        if (targetWorld == null) {
            targetWorld = worldLoader.ensureLoaded(defaultWorldName);
        }
        if (targetWorld == null && !defaultWorldName.equalsIgnoreCase("world")) {
            targetWorld = worldLoader.findLoaded("world");
            if (targetWorld == null) {
                targetWorld = worldLoader.ensureLoaded("world");
            }
        }
        if (targetWorld == null) {
            if (m != null) m.send(player, "mode.world-not-loaded",
                    "world", defaultWorldName);
            return true;
        }

        gameModeManager.setWorldMode(targetWorld.getName().toLowerCase(), GameMode.PLATFORM);
        final World finalWorld = targetWorld;

        Location spawn = playerJoin.getSpawnLocation(finalWorld);
        if (spawn == null) {
            spawn = finalWorld.getSpawnLocation();
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();

        player.teleport(spawn);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                playerJoin.giveKitForMode(player, mode);
                player.updateInventory();
            }
        }, 5L);

        if (m != null) m.send(player, "mode.joined",
                "mode", mode.getDisplayName(),
                "world", finalWorld.getName());
        return true;
    }

    // ============================================================
    //  /housing lobby
    // ============================================================

    private boolean handleLobby(CommandSender sender) {
        Messages m = M();
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }
        if (!sender.hasPermission(getPerm("join", "housing.join"))) {
            sendNoPerm(sender); return true;
        }

        Player player = (Player) sender;

        String defaultWorldName = plugin.getConfig()
                .getString("protection.locked-world", "world");
        if (defaultWorldName != null
                && player.getWorld().getName().equalsIgnoreCase(defaultWorldName)) {
            if (m != null) m.send(player, "mode.already-in-lobby");
            return true;
        }

        playerJoin.joinLobby(player);
        if (m != null) m.send(player, "mode.lobby-teleport");
        return true;
    }

    // ============================================================
    //  /housing setlobbyspawn
    // ============================================================

    private boolean handleSetLobbySpawn(CommandSender sender) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("setlobbyspawn", "housing.setlobbyspawn"))) {
            sendNoPerm(sender); return true;
        }
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        FileConfiguration config = gameModeManager.getConfig(GameMode.PLATFORM);
        config.set("spawn.world", loc.getWorld().getName());
        config.set("spawn.x", Double.valueOf(loc.getX()));
        config.set("spawn.y", Double.valueOf(loc.getY()));
        config.set("spawn.z", Double.valueOf(loc.getZ()));
        config.set("spawn.yaw", Float.valueOf(loc.getYaw()));
        config.set("spawn.pitch", Float.valueOf(loc.getPitch()));
        gameModeManager.saveModeConfig(GameMode.PLATFORM);

        if (m != null) m.send(player, "spawn.set-lobby",
                "world", loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()));
        return true;
    }

    // ============================================================
    //  Player commands
    // ============================================================

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

    private boolean handleMenu(CommandSender sender) {
        Messages m = M();
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }
        if (!sender.hasPermission(getPerm("menu", "housing.menu"))) {
            sendNoPerm(sender); return true;
        }

        Player player = (Player) sender;
        if (plugin instanceof Housing) {
            ModeMenu menu = ((Housing) plugin).getModeMenu();
            if (menu != null) {
                menu.openMenu(player);
            }
        }
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        Messages m = M();
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }

        if (!sender.hasPermission(getPerm("join", "housing.join"))) {
            sendNoPerm(sender); return true;
        }

        if (args.length < 2) {
            if (m != null) {
                m.send(sender, "usage.join");
                m.send(sender, "usage.join-modes");
            }
            return true;
        }

        GameMode mode = GameMode.fromId(args[1]);
        if (mode == null) {
            if (m != null) {
                m.send(sender, "general.unknown-mode", "mode", args[1]);
                m.send(sender, "usage.join-modes");
            }
            return true;
        }

        if (mode == GameMode.PLATFORM) {
            return joinDefaultWorld(sender);
        }

        final Player player = (Player) sender;

        GameMode currentMode = gameModeManager.getModeForWorld(player.getWorld());
        if (currentMode == mode) {
            if (m != null) m.send(player, "mode.already-in",
                    "mode", mode.getDisplayName());
            return true;
        }

        String worldName = gameModeManager.getWorldForMode(mode);

        if (worldName == null) {
            if (m != null) m.send(player, "mode.no-world",
                    "mode", mode.getDisplayName());
            World world = worldLoader.ensureLoaded(mode.getId());
            if (world == null) {
                if (m != null) m.send(player, "mode.could-not-create",
                        "mode", mode.getId());
                return true;
            }
            gameModeManager.setWorldMode(mode.getId(), mode);
            worldName = world.getName();
        }

        final World targetWorld = worldLoader.ensureLoaded(worldName);
        if (targetWorld == null) {
            if (m != null) m.send(player, "mode.world-not-available",
                    "world", worldName);
            return true;
        }

        final GameMode finalMode = mode;

        Location spawn = playerJoin.getSpawnLocation(targetWorld);
        if (spawn == null) {
            spawn = targetWorld.getSpawnLocation();
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.updateInventory();

        player.teleport(spawn);

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

        if (m != null) m.send(player, "mode.joined",
                "mode", finalMode.getDisplayName(),
                "world", targetWorld.getName());
        return true;
    }

    private boolean handleSetWorld(CommandSender sender, String[] args) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("setworld", "housing.setworld"))) {
            sendNoPerm(sender); return true;
        }

        String worldName;
        GameMode mode;

        if (args.length == 2) {
            mode = GameMode.fromId(args[1]);
            if (mode == null) {
                if (m != null) {
                    m.send(sender, "general.unknown-mode", "mode", args[1]);
                    m.send(sender, "usage.join-modes");
                }
                return true;
            }
            if (!(sender instanceof Player)) {
                if (m != null) m.send(sender, "usage.setworld-console");
                return true;
            }
            worldName = ((Player) sender).getWorld().getName();
        } else if (args.length >= 3) {
            worldName = args[1];
            mode = GameMode.fromId(args[2]);
            if (mode == null) {
                if (m != null) {
                    m.send(sender, "general.unknown-mode", "mode", args[2]);
                    m.send(sender, "usage.join-modes");
                }
                return true;
            }
        } else {
            if (m != null) {
                m.send(sender, "usage.setworld");
                m.send(sender, "usage.join-modes");
            }
            return true;
        }

        World world = worldLoader.findLoaded(worldName);
        if (world == null) {
            if (m != null) m.send(sender, "world.auto-loading", "world", worldName);
            world = worldLoader.ensureLoaded(worldName);
        }

        if (world == null) {
            if (m != null) m.send(sender, "world.could-not-load", "world", worldName);
            return true;
        }

        String finalWorldName = world.getName().toLowerCase();
        gameModeManager.setWorldMode(finalWorldName, mode);

        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.getWorld().equals(world)) {
                player.teleport(world.getSpawnLocation());
            }
        }

        if (m != null) m.send(sender, "world.set-success",
                "world", world.getName(),
                "mode", mode.getDisplayName());
        return true;
    }

    private boolean handleSetSpawn(CommandSender sender, GameMode mode) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("setspawn", "housing.setspawn"))) {
            sendNoPerm(sender); return true;
        }
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
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

        if (m != null) m.send(player, "spawn.set-mode",
                "mode", mode.getDisplayName(),
                "world", loc.getWorld().getName(),
                "x", String.valueOf(loc.getBlockX()),
                "y", String.valueOf(loc.getBlockY()),
                "z", String.valueOf(loc.getBlockZ()));
        return true;
    }

    private boolean handleSetVoid(CommandSender sender, GameMode mode, String[] args) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("setvoid", "housing.setvoid"))) {
            sendNoPerm(sender); return true;
        }

        double y;
        if (args.length >= 3) {
            try { y = Double.parseDouble(args[2]); }
            catch (NumberFormatException e) {
                if (m != null) m.send(sender, "general.invalid-number", "value", args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                if (m != null) m.send(sender, "usage.setvoid-console", "mode", mode.getId());
                return true;
            }
            y = ((Player) sender).getLocation().getY();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("void.kill-height", Double.valueOf(y));
        gameModeManager.saveModeConfig(mode);

        if (m != null) m.send(sender, "spawn.void-set",
                "mode", mode.getDisplayName(),
                "y", String.valueOf(y));
        return true;
    }

    private boolean handleSetZShowSword(CommandSender sender, GameMode mode, String[] args) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("setzshowsword", "housing.setzshowsword"))) {
            sendNoPerm(sender); return true;
        }

        if (mode != GameMode.ONEWIDE) {
            if (m != null) m.send(sender, "mode-error.setzshowsword-only-onewide");
            return true;
        }

        double z;
        if (args.length >= 3) {
            try { z = Double.parseDouble(args[2]); }
            catch (NumberFormatException e) {
                if (m != null) m.send(sender, "general.invalid-number", "value", args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                if (m != null) m.send(sender, "usage.zshowsword");
                return true;
            }
            z = ((Player) sender).getLocation().getZ();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("zshowsword", Double.valueOf(z));
        gameModeManager.saveModeConfig(mode);

        if (m != null) m.send(sender, "spawn.zshowsword-set", "z", String.valueOf(z));
        return true;
    }

    private boolean handleSetPvpZone(CommandSender sender, GameMode mode, String[] args) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("setpvpzone", "housing.setpvpzone"))) {
            sendNoPerm(sender); return true;
        }

        if (mode != GameMode.PLATFORM && mode != GameMode.ONEWIDE) {
            if (m != null) m.send(sender, "mode-error.setpvpzone-only-platform-onewide");
            return true;
        }

        double x;
        if (args.length >= 3) {
            try { x = Double.parseDouble(args[2]); }
            catch (NumberFormatException e) {
                if (m != null) m.send(sender, "general.invalid-number", "value", args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                if (m != null) m.send(sender, "usage.pvpzone", "mode", mode.getId());
                return true;
            }
            x = ((Player) sender).getLocation().getX();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("pvpzone.enabled", Boolean.valueOf(true));
        cfg.set("pvpzone.x", Double.valueOf(x));
        gameModeManager.saveModeConfig(mode);

        if (m != null) m.send(sender, "spawn.pvpzone-set",
                "mode", mode.getDisplayName(),
                "x", String.valueOf(x));
        return true;
    }

    private boolean handleKit(CommandSender sender, GameMode mode, String[] args) {
        Messages m = M();
        if (!sender.hasPermission(getPerm("kit", "housing.kit"))) {
            sendNoPerm(sender); return true;
        }

        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                if (m != null) m.send(sender, "general.player-not-found", "player", args[2]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                if (m != null) m.send(sender, "usage.kit-console", "mode", mode.getId());
                return true;
            }
            target = (Player) sender;
        }

        playerJoin.giveKitForMode(target, mode);

        if (m != null) {
            if (sender.equals(target)) {
                m.send(sender, "kit.restored-self", "mode", mode.getDisplayName());
            } else {
                m.send(sender, "kit.restored-other",
                        "mode", mode.getDisplayName(),
                        "player", target.getName());
                m.send(target, "kit.restored-target", "mode", mode.getDisplayName());
            }
        }
        return true;
    }

    private boolean handleScoreboard(CommandSender sender, GameMode mode, String[] args) {
        Messages m = M();
        if (!(sender instanceof Player)) {
            if (m != null) m.send(sender, "general.player-only");
            return true;
        }
        Player player = (Player) sender;

        if (args.length >= 3 && args[2].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(getPerm("reload", "housing.reload"))) {
                sendNoPerm(sender); return true;
            }
            gameModeManager.reloadMode(mode);
            if (this.scoreboardManager != null) this.scoreboardManager.reloadConfig();
            if (m != null) m.send(player, "scoreboard.reloaded", "mode", mode.getDisplayName());
            return true;
        }

        if (!sender.hasPermission(getPerm("scoreboard", "housing.scoreboard"))) {
            sendNoPerm(sender); return true;
        }

        boolean nowVisible = this.scoreboardManager.toggleScoreboard(player);
        if (m != null) {
            m.send(player, nowVisible ? "scoreboard.enabled" : "scoreboard.disabled");
        }
        return true;
    }

    private boolean handleCreator(CommandSender sender) {
        Messages m = M();
        if (m != null) {
            sender.sendMessage(m.get("creator.header"));
            sender.sendMessage(m.get("creator.title"));
            sender.sendMessage(m.get("creator.version"));
            sender.sendMessage(m.get("creator.modes"));
            sender.sendMessage(m.get("creator.header"));
        } else {
            sender.sendMessage(colorize("&bHousing - Created by Muvixo"));
        }
        return true;
    }

    // ============================================================
    //  HELP MENU — still hardcoded (uses perms), but messages from yml
    // ============================================================

    private void sendHelp(CommandSender sender) {

        String permJoin          = getPerm("join",          "housing.join");
        String permMenu          = getPerm("menu",          "housing.menu");
        String permScoreboard    = getPerm("scoreboard",    "housing.scoreboard");

        String permSetWorld      = getPerm("setworld",      "housing.setworld");
        String permSetSpawn      = getPerm("setspawn",      "housing.setspawn");
        String permSetLobbySpawn = getPerm("setlobbyspawn", "housing.setlobbyspawn");
        String permSetVoid       = getPerm("setvoid",       "housing.setvoid");
        String permSetZShowSword = getPerm("setzshowsword", "housing.setzshowsword");
        String permSetPvpZone    = getPerm("setpvpzone",    "housing.setpvpzone");
        String permReload        = getPerm("reload",        "housing.reload");
        String permKit           = getPerm("kit",           "housing.kit");
        String permBuildMode     = getPerm("buildmode",     "housing.buildmode");
        String permBypass        = getPerm("bypass",        "housing.bypass");

        boolean isAdmin =
                sender.hasPermission(permSetWorld)
             || sender.hasPermission(permSetSpawn)
             || sender.hasPermission(permSetLobbySpawn)
             || sender.hasPermission(permSetVoid)
             || sender.hasPermission(permSetZShowSword)
             || sender.hasPermission(permSetPvpZone)
             || sender.hasPermission(permReload)
             || sender.hasPermission(permKit)
             || sender.hasPermission(permBuildMode)
             || sender.hasPermission(permBypass);

        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b&lHousing &f- &bCommands"));
        sender.sendMessage(colorize("&b&m----------------------------------"));

        sender.sendMessage(colorize("&e&lGeneral Commands"));
        sender.sendMessage(colorize("  &b/housing help &8- &7Show this help"));
        sender.sendMessage(colorize("  &b/housing creator &8- &7Show plugin credits"));
        sender.sendMessage(colorize("  &b/housing worlds &8- &7List loaded worlds"));

        if (sender.hasPermission(permJoin)) {
            sender.sendMessage(colorize("  &b/housing &8- &7Join the default mode (Platform)"));
            sender.sendMessage(colorize("  &b/housing join <mode> &8- &7Join a game mode"));
        }
        if (sender.hasPermission(permMenu)) {
            sender.sendMessage(colorize("  &b/housing menu &8- &7Open the mode selection menu"));
        }
        if (sender.hasPermission(permJoin)) {
            sender.sendMessage(colorize("  &b/housing lobby &8- &7Teleport to the lobby"));
        }
        if (sender.hasPermission(permBuildMode)) {
            sender.sendMessage(colorize("  &b/housing buildmode &8- &7Toggle build mode"));
        }

        sender.sendMessage(colorize("  &7Modes: &fplatform, lowmid, onewide, blockfight"));

        if (isAdmin) {
            sender.sendMessage(colorize("&b&m----------------------------------"));
            sender.sendMessage(colorize("&c&lAdmin Commands"));

            if (sender.hasPermission(permReload)) {
                sender.sendMessage(colorize("  &b/housing reload &8- &7Reload all configs"));
            }
            if (sender.hasPermission(permSetWorld)) {
                sender.sendMessage(colorize("  &b/housing setworld [world] <mode> &8- &7Assign a world"));
            }
            if (sender.hasPermission(permSetLobbySpawn)) {
                sender.sendMessage(colorize("  &b/housing setlobbyspawn &8- &7Set the lobby spawn"));
            }

            boolean anyModeCmd =
                    sender.hasPermission(permSetSpawn)
                 || sender.hasPermission(permSetVoid)
                 || sender.hasPermission(permSetPvpZone)
                 || sender.hasPermission(permSetZShowSword)
                 || sender.hasPermission(permKit)
                 || sender.hasPermission(permScoreboard);

            if (anyModeCmd) {
                sender.sendMessage(colorize("  &7&m-- &r&bPer-Mode Commands &7&m--"));

                if (sender.hasPermission(permSetSpawn)) {
                    sender.sendMessage(colorize("  &b/housing <mode> setspawn &8- &7Set spawn"));
                }
                if (sender.hasPermission(permSetVoid)) {
                    sender.sendMessage(colorize("  &b/housing <mode> setvoid [y] &8- &7Set void Y"));
                }
                if (sender.hasPermission(permSetPvpZone)) {
                    sender.sendMessage(colorize("  &b/housing <mode> setpvpzone <x> &8- &7Set PvP zone"));
                }
                if (sender.hasPermission(permKit)) {
                    sender.sendMessage(colorize("  &b/housing <mode> kit [player] &8- &7Give kit"));
                }
                if (sender.hasPermission(permScoreboard)) {
                    sender.sendMessage(colorize("  &b/housing <mode> sb &8- &7Toggle scoreboard"));
                }
                if (sender.hasPermission(permSetZShowSword)) {
                    sender.sendMessage(colorize("  &b/housing onewide setzshowsword <z> &8- &7OneWide only"));
                }
            }

            if (sender.hasPermission(permBypass)) {
                sender.sendMessage(colorize("&7&oYou have full protection bypass."));
            }
        }

        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&7Use &b/housing <mode> &7for mode-specific help."));
        sender.sendMessage(colorize("&b&m----------------------------------"));
    }

    private void sendModeHelp(CommandSender sender, GameMode mode) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bHousing &f- &b" + mode.getDisplayName()));
        sender.sendMessage(colorize("&b&m----------------------------------"));

        if (sender.hasPermission(getPerm("join", "housing.join"))) {
            sender.sendMessage(colorize("&b/housing join " + mode.getId()));
        }
        if (sender.hasPermission(getPerm("setspawn", "housing.setspawn"))) {
            sender.sendMessage(colorize("&b/housing " + mode.getId() + " setspawn"));
        }
        if (sender.hasPermission(getPerm("setvoid", "housing.setvoid"))) {
            sender.sendMessage(colorize("&b/housing " + mode.getId() + " setvoid [y]"));
        }
        if ((mode == GameMode.PLATFORM || mode == GameMode.ONEWIDE)
                && sender.hasPermission(getPerm("setpvpzone", "housing.setpvpzone"))) {
            sender.sendMessage(colorize("&b/housing " + mode.getId() + " setpvpzone <x>"));
        }
        if (sender.hasPermission(getPerm("kit", "housing.kit"))) {
            sender.sendMessage(colorize("&b/housing " + mode.getId() + " kit [player]"));
        }
        if (sender.hasPermission(getPerm("scoreboard", "housing.scoreboard"))) {
            sender.sendMessage(colorize("&b/housing " + mode.getId() + " sb [reload]"));
        }
        if (mode == GameMode.ONEWIDE
                && sender.hasPermission(getPerm("setzshowsword", "housing.setzshowsword"))) {
            sender.sendMessage(colorize("&b/housing onewide setzshowsword <z>"));
        }

        sender.sendMessage(colorize("&b&m----------------------------------"));
    }

    // ============================================================
    //  Tab completion (unchanged)
    // ============================================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<String>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<String>();
            subs.add("creator");
            subs.add("help");
            subs.add("worlds");

            if (sender.hasPermission(getPerm("join", "housing.join"))) {
                subs.add("join");
                subs.add("lobby");
            }
            if (sender.hasPermission(getPerm("menu", "housing.menu"))) {
                subs.add("menu");
            }
            if (sender.hasPermission(getPerm("buildmode", "housing.buildmode"))) {
                subs.add("buildmode");
            }
            if (sender.hasPermission(getPerm("reload", "housing.reload"))) {
                subs.add("reload");
            }
            if (sender.hasPermission(getPerm("setworld", "housing.setworld"))) {
                subs.add("setworld");
            }
            if (sender.hasPermission(getPerm("setlobbyspawn", "housing.setlobbyspawn"))) {
                subs.add("setlobbyspawn");
            }

            boolean anyModePerm =
                    sender.hasPermission(getPerm("setspawn", "housing.setspawn"))
                 || sender.hasPermission(getPerm("setvoid", "housing.setvoid"))
                 || sender.hasPermission(getPerm("kit", "housing.kit"))
                 || sender.hasPermission(getPerm("scoreboard", "housing.scoreboard"))
                 || sender.hasPermission(getPerm("join", "housing.join"));

            if (anyModePerm) {
                subs.add("platform");
                subs.add("lowmid");
                subs.add("onewide");
                subs.add("blockfight");
            }

            String partial = args[0].toLowerCase();
            for (String s : subs) if (s.startsWith(partial)) out.add(s);
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("buildmode")) {
                if (sender.hasPermission(getPerm("buildmode", "housing.buildmode"))) {
                    out.add("on"); out.add("off"); out.add("toggle");
                }
                String partial = args[1].toLowerCase();
                List<String> filtered = new ArrayList<String>();
                for (String s : out) if (s.startsWith(partial)) filtered.add(s);
                return filtered;
            }

            if (sub.equals("setworld")) {
                if (sender.hasPermission(getPerm("setworld", "housing.setworld"))) {
                    out.add("platform"); out.add("lowmid");
                    out.add("onewide");  out.add("blockfight");
                    for (World w : Bukkit.getWorlds()) out.add(w.getName().toLowerCase());
                }
                return out;
            }

            if (sub.equals("join")) {
                if (sender.hasPermission(getPerm("join", "housing.join"))) {
                    out.add("platform"); out.add("lowmid");
                    out.add("onewide");  out.add("blockfight");
                }
                return out;
            }

            GameMode mode = GameMode.fromId(sub);
            if (mode != null) {
                List<String> actions = new ArrayList<String>();
                if (sender.hasPermission(getPerm("setspawn", "housing.setspawn"))) {
                    actions.add("setspawn");
                }
                if (sender.hasPermission(getPerm("setvoid", "housing.setvoid"))) {
                    actions.add("setvoid");
                }
                if (sender.hasPermission(getPerm("kit", "housing.kit"))) {
                    actions.add("kit");
                }
                if (sender.hasPermission(getPerm("scoreboard", "housing.scoreboard"))) {
                    actions.add("sb");
                }
                if (mode == GameMode.ONEWIDE
                        && sender.hasPermission(getPerm("setzshowsword", "housing.setzshowsword"))) {
                    actions.add("setzshowsword");
                }
                if ((mode == GameMode.PLATFORM || mode == GameMode.ONEWIDE)
                        && sender.hasPermission(getPerm("setpvpzone", "housing.setpvpzone"))) {
                    actions.add("setpvpzone");
                }

                String partial = args[1].toLowerCase();
                for (String s : actions) if (s.startsWith(partial)) out.add(s);
            }
            return out;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (sub.equals("setworld")
                    && sender.hasPermission(getPerm("setworld", "housing.setworld"))) {
                out.add("platform"); out.add("lowmid");
                out.add("onewide");  out.add("blockfight");
                return out;
            }
            if (action.equals("kit")
                    && sender.hasPermission(getPerm("kit", "housing.kit"))) {
                String partial = args[2].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers())
                    if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName());
                return out;
            }
            if (action.equals("sb")
                    && sender.hasPermission(getPerm("reload", "housing.reload"))) {
                out.add("reload"); return out;
            }
        }

        return out;
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}