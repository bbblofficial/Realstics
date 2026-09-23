
#!/usr/bin/env python3
"""
create.py — Realstics Plugin Generator

Creates the complete Realstics plugin with 4 game modes:
  • Platform  (default, enabled)
  • LowMid
  • OneWide
  • BlockFight

Structure:
  Realstics/
  ├── pom.xml
  ├── plugin.yml
  ├── README.md
  ├── create.py                 (this file)
  └── src/main/
      ├── java/org/realstics/
      │   ├── Realstics.java
      │   ├── GameMode.java
      │   ├── GameModeManager.java
      │   ├── RealsticsCommand.java
      │   ├── PlayerJoin.java
      │   ├── NoDamage.java
      │   ├── Protection.java
      │   ├── KitRestore.java
      │   ├── Welcome.java
      │   ├── Void.java
      │   ├── ComboSystem.java
      │   └── ScoreboardManager.java
      └── resources/
          ├── config.yml            (Platform)
          ├── scoreboard.yml        (Platform SB)
          ├── lowmid.yml
          ├── sb-lowmid.yml
          ├── onewide.yml
          ├── sb-onewide.yml
          ├── blockfight.yml
          └── sb-blockfight.yml

Run:  python3 create.py
"""

import os

BASE = os.path.dirname(os.path.abspath(__file__))

# ==============================================================
#  DIRECTORIES
# ==============================================================
DIRS = [
    "src/main/java/org/realstics",
    "src/main/resources",
]

# ==============================================================
#  POM.XML
# ==============================================================
POM_XML = '''<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
                             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>org.realstics</groupId>
    <artifactId>Realstics</artifactId>
    <version>1.0</version>
    <packaging>jar</packaging>

    <name>Realstics</name>
    <description>Multi-gamemode cosmetic PvP plugin (Platform, LowMid, OneWide, BlockFight)</description>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
    </properties>

    <repositories>
        <repository>
            <id>spigot-repo</id>
            <url>https://hub.spigotmc.org/nexus/content/repositories/snapshots/</url>
        </repository>
        <repository>
            <id>codemc-repo</id>
            <url>https://repo.codemc.io/repository/maven-public/</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>org.spigotmc</groupId>
            <artifactId>spigot-api</artifactId>
            <version>1.8.8-R0.1-SNAPSHOT</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <groupId>net.md-5</groupId>
                    <artifactId>bungeecord-chat</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

        <dependency>
            <groupId>net.md-5</groupId>
            <artifactId>bungeecord-chat</artifactId>
            <version>1.16-R0.4</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>Realstics</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
'''

# ==============================================================
#  PLUGIN.YML
# ==============================================================
PLUGIN_YML = '''name: Realstics
version: 1.0
main: org.realstics.Realstics
author: Muvixo
description: Multi-gamemode cosmetic PvP plugin (Platform, LowMid, OneWide, BlockFight)
commands:
  realstics:
    description: Main Realstics command
    usage: /realstics <mode> <subcommand>
    aliases:
      - rs
      - rl
permissions:
  realstics.platform:
    description: Access to Platform mode
    default: op
  realstics.lowmid:
    description: Access to LowMid mode
    default: op
  realstics.onewide:
    description: Access to OneWide mode
    default: op
  realstics.blockfight:
    description: Access to BlockFight mode
    default: op
  realstics.setworld:
    description: Set a world's game mode
    default: op
  realstics.setspawn:
    description: Set spawn for the current world's mode
    default: op
  realstics.setvoid:
    description: Set void Y level
    default: op
  realstics.setzshowsword:
    description: Set OneWide Z threshold for showing sword
    default: op
  realstics.setcreator:
    description: Show plugin credits
    default: true
  realstics.reload:
    description: Reload the config
    default: op
  realstics.kit:
    description: Give the cosmetic kit
    default: op
  realstics.scoreboard:
    description: Toggle the scoreboard
    default: true

  # Protection
  realstics.bypass:
    description: Bypass ALL protection
    default: op
  realstics.break:
    description: Allow breaking blocks
    default: false
  realstics.place:
    description: Allow placing blocks
    default: false
  realstics.drop:
    description: Allow dropping items
    default: false
'''

# ==============================================================
#  JAVA FILES
# ==============================================================
JAVA = {}

JAVA["GameMode.java"] = r'''package org.realstics;

/**
 * All supported game modes.
 *
 * PLATFORM is the default and enabled automatically.
 * LOWMID, ONEWIDE, BLOCKFIGHT must be assigned to a world first
 * with  /realstics setworld <worldname> <mode>
 */
public enum GameMode {

    PLATFORM("platform",   "Platform",   "config.yml",      "scoreboard.yml"),
    LOWMID  ("lowmid",     "LowMid",     "lowmid.yml",      "sb-lowmid.yml"),
    ONEWIDE ("onewide",    "OneWide",    "onewide.yml",     "sb-onewide.yml"),
    BLOCKFIGHT("blockfight","BlockFight","blockfight.yml",  "sb-blockfight.yml");

    private final String id;
    private final String displayName;
    private final String configFile;
    private final String scoreboardFile;

    GameMode(String id, String displayName, String configFile, String scoreboardFile) {
        this.id = id;
        this.displayName = displayName;
        this.configFile = configFile;
        this.scoreboardFile = scoreboardFile;
    }

    public String getId()             { return id; }
    public String getDisplayName()    { return displayName; }
    public String getConfigFile()     { return configFile; }
    public String getScoreboardFile() { return scoreboardFile; }

    public static GameMode fromId(String id) {
        if (id == null) return null;
        for (GameMode m : values()) {
            if (m.id.equalsIgnoreCase(id)) return m;
        }
        return null;
    }
}
'''

JAVA["GameModeManager.java"] = r'''package org.realstics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages per-world game modes and per-mode config files.
 *
 * World -> GameMode mapping stored in config.yml under:
 *   worlds:
 *     <worldname>: <modeid>
 *
 * Unmapped world  =>  PLATFORM (enabled by default).
 * LowMid / OneWide / BlockFight must be set up explicitly.
 */
public class GameModeManager {

    private final JavaPlugin plugin;

    /** worldName (lowercase) -> GameMode */
    private final Map<String, GameMode> worldModes = new HashMap<String, GameMode>();

    /** GameMode -> its own FileConfiguration */
    private final Map<GameMode, FileConfiguration> modeConfigs =
            new HashMap<GameMode, FileConfiguration>();

    /** GameMode -> its scoreboard FileConfiguration */
    private final Map<GameMode, FileConfiguration> modeScoreboards =
            new HashMap<GameMode, FileConfiguration>();

    public GameModeManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // ============================================================
    //  INIT
    // ============================================================
    public void init() {
        for (GameMode mode : GameMode.values()) {
            ensureModeConfig(mode);
            ensureModeScoreboard(mode);
        }
        loadWorldMappings();
    }

    // ============================================================
    //  WORLD MAPPINGS
    // ============================================================
    public void loadWorldMappings() {
        worldModes.clear();
        FileConfiguration cfg = plugin.getConfig();
        if (cfg.isConfigurationSection("worlds")) {
            for (String world : cfg.getConfigurationSection("worlds").getKeys(false)) {
                String modeId = cfg.getString("worlds." + world);
                GameMode mode = GameMode.fromId(modeId);
                if (mode != null) {
                    worldModes.put(world.toLowerCase(), mode);
                }
            }
        }
    }

    public GameMode getModeForWorld(World world) {
        if (world == null) return GameMode.PLATFORM;
        GameMode mode = worldModes.get(world.getName().toLowerCase());
        return mode != null ? mode : GameMode.PLATFORM;
    }

    public GameMode getModeForWorld(String worldName) {
        if (worldName == null) return GameMode.PLATFORM;
        World w = Bukkit.getWorld(worldName);
        return getModeForWorld(w);
    }

    public void setWorldMode(String worldName, GameMode mode) {
        worldModes.put(worldName.toLowerCase(), mode);
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("worlds." + worldName.toLowerCase(), mode.getId());
        plugin.saveConfig();
    }

    public boolean isWorldConfigured(String worldName) {
        return worldModes.containsKey(worldName.toLowerCase());
    }

    public Map<String, GameMode> getWorldModes() {
        return worldModes;
    }

    // ============================================================
    //  MODE CONFIG FILES (auto-merge)
    // ============================================================
    private void ensureModeConfig(GameMode mode) {
        File file = new File(plugin.getDataFolder(), mode.getConfigFile());
        boolean isNew = !file.exists();

        if (isNew) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create " + mode.getConfigFile()
                        + ": " + e.getMessage());
                return;
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource(mode.getConfigFile());
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            cfg.setDefaults(defaults);
        }

        if (cfg.getDefaults() != null) {
            for (String key : cfg.getDefaults().getKeys(true)) {
                if (!cfg.contains(key)) {
                    cfg.set(key, cfg.getDefaults().get(key));
                }
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + mode.getConfigFile()
                    + ": " + e.getMessage());
        }

        modeConfigs.put(mode, cfg);
    }

    private void ensureModeScoreboard(GameMode mode) {
        File file = new File(plugin.getDataFolder(), mode.getScoreboardFile());
        boolean isNew = !file.exists();

        if (isNew) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Could not create " + mode.getScoreboardFile()
                        + ": " + e.getMessage());
                return;
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        InputStream defStream = plugin.getResource(mode.getScoreboardFile());
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            cfg.setDefaults(defaults);
        }

        if (cfg.getDefaults() != null) {
            for (String key : cfg.getDefaults().getKeys(true)) {
                if (!cfg.contains(key)) {
                    cfg.set(key, cfg.getDefaults().get(key));
                }
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + mode.getScoreboardFile()
                    + ": " + e.getMessage());
        }

        modeScoreboards.put(mode, cfg);
    }

    public FileConfiguration getConfig(GameMode mode) {
        FileConfiguration cfg = modeConfigs.get(mode);
        if (cfg == null) {
            ensureModeConfig(mode);
            cfg = modeConfigs.get(mode);
        }
        return cfg;
    }

    public FileConfiguration getScoreboard(GameMode mode) {
        FileConfiguration cfg = modeScoreboards.get(mode);
        if (cfg == null) {
            ensureModeScoreboard(mode);
            cfg = modeScoreboards.get(mode);
        }
        return cfg;
    }

    public void reloadMode(GameMode mode) {
        ensureModeConfig(mode);
        ensureModeScoreboard(mode);
    }

    public void reloadAll() {
        for (GameMode mode : GameMode.values()) {
            reloadMode(mode);
        }
        loadWorldMappings();
    }

    public void saveModeConfig(GameMode mode) {
        File file = new File(plugin.getDataFolder(), mode.getConfigFile());
        FileConfiguration cfg = modeConfigs.get(mode);
        if (cfg == null || file == null) return;
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save " + mode.getConfigFile()
                    + ": " + e.getMessage());
        }
    }
}
'''

JAVA["Realstics.java"] = r'''package org.realstics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Realstics extends JavaPlugin {

    private PlayerJoin playerJoin;
    private ScoreboardManager scoreboardManager;
    private Void voidSystem;
    private ComboSystem comboSystem;
    private GameModeManager gameModeManager;

    @Override
    public void onEnable() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // ---- default config.yml (Platform, auto-merge) ----
        createConfigIfMissing();
        saveDefaultConfig();
        reloadConfig();

        // ---- game mode manager (creates lowmid.yml etc.) ----
        this.gameModeManager = new GameModeManager(this);
        this.gameModeManager.init();

        // ---- listeners ----
        this.playerJoin = new PlayerJoin(this, this.gameModeManager);
        getServer().getPluginManager().registerEvents(this.playerJoin, this);
        getServer().getPluginManager().registerEvents(new NoDamage(this), this);
        getServer().getPluginManager().registerEvents(new Protection(this), this);
        getServer().getPluginManager().registerEvents(new KitRestore(this, this.playerJoin), this);
        getServer().getPluginManager().registerEvents(new Welcome(this), this);

        this.voidSystem = new Void(this, this.playerJoin, this.gameModeManager);
        getServer().getPluginManager().registerEvents(this.voidSystem, this);

        this.comboSystem = new ComboSystem(this);
        getServer().getPluginManager().registerEvents(this.comboSystem, this);

        this.scoreboardManager = new ScoreboardManager(this, this.gameModeManager);

        // ---- command ----
        RealsticsCommand cmd = new RealsticsCommand(this,
                this.playerJoin, this.scoreboardManager, this.voidSystem,
                this.gameModeManager);
        getCommand("realstics").setExecutor(cmd);
        getCommand("realstics").setTabCompleter(cmd);

        getLogger().info("=================================================");
        getLogger().info("  Realstics v1.0 - Enabled");
        getLogger().info("  Modes: Platform, LowMid, OneWide, BlockFight");
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }
        getLogger().info("Realstics disabled.");
    }

    // ============================================================
    //  DEFAULT config.yml AUTO-MERGE
    // ============================================================
    private void createConfigIfMissing() {
        File configFile = new File(getDataFolder(), "config.yml");
        boolean isNew = !configFile.exists();

        if (isNew) {
            try {
                configFile.createNewFile();
            } catch (IOException e) {
                getLogger().warning("Could not create config.yml: " + e.getMessage());
                return;
            }
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(configFile);

        InputStream defStream = this.getResource("config.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            cfg.setDefaults(defaults);
        }

        setIfMissing(cfg, "spawn.world", "world");
        setIfMissing(cfg, "spawn.x", Double.valueOf(0.5D));
        setIfMissing(cfg, "spawn.y", Double.valueOf(100.0D));
        setIfMissing(cfg, "spawn.z", Double.valueOf(0.5D));
        setIfMissing(cfg, "spawn.yaw", Float.valueOf(0.0F));
        setIfMissing(cfg, "spawn.pitch", Float.valueOf(0.0F));

        setIfMissing(cfg, "void.kill-height", Double.valueOf(-13.0D));

        setIfMissing(cfg, "join-message",
                "&b%player% &7joined the game &8(&b%online%&7/&b%max_online%&8)");
        setIfMissing(cfg, "quit-message",
                "&b%player% &7left the game &8(&b%online%&7/&b%max_online%&8)");

        setIfMissing(cfg, "combo.enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "combo.step", Integer.valueOf(10));
        setIfMissing(cfg, "combo.reset-time", Long.valueOf(3000L));
        setIfMissing(cfg, "combo.sound-enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "combo.broadcast-message",
                "&8&m-------------------------------\\n&6&lCOMBO &e&l%combo%x\\n&e%attacker% &7got a combo on &c%victim% &7(&6%combo% &7combo)\\n&8&m-------------------------------");

        // ---- default world mapping ----
        setIfMissing(cfg, "worlds.world", "platform");

        try {
            cfg.save(configFile);
            if (isNew) {
                getLogger().info("Created default config.yml");
            } else {
                getLogger().info("Config.yml merged (existing values preserved).");
            }
        } catch (IOException e) {
            getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
    }

    private void setIfMissing(FileConfiguration cfg, String path, Object value) {
        if (!cfg.contains(path)) {
            cfg.set(path, value);
        }
    }

    public PlayerJoin getPlayerJoin()                         { return playerJoin; }
    public ScoreboardManager getScoreboardManager()           { return scoreboardManager; }
    public Void getVoidSystem()                               { return voidSystem; }
    public ComboSystem getComboSystem()                       { return comboSystem; }
    public GameModeManager getGameModeManager()               { return gameModeManager; }
}
'''

JAVA["PlayerJoin.java"] = r'''package org.realstics;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Gives the correct cosmetic kit per game mode.
 *
 *  PLATFORM  : Leather Helmet + Chestplate (Prot III, red, Unbreakable)
 *              Iron Leggings + Boots (Prot III, Unbreakable)
 *              Wooden Sword (Sharpness I, Unbreakable)
 *
 *  LOWMID    : Wooden Sword (Sharpness I, 5.25 dmg, Unbreakable)
 *
 *  ONEWIDE   : Iron Sword (6 dmg, Unbreakable) - hidden while in spawn zone
 *
 *  BLOCKFIGHT: Diamond Sword (Sharpness IV, 12+ dmg, Unbreakable) slot 1
 *              64 Wool (never decreases)                         slot 2
 *              Shears (Unbreakable)                              slot 3
 */
public class PlayerJoin implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private static final int LEATHER_COLOR = 16711680; // 0xFF0000

    public PlayerJoin(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                giveKit(player);
                teleportToSpawn(player);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(final PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                giveKit(player);
            }
        }, 5L);
    }

    // ============================================================
    //  KIT DISPATCH
    // ============================================================
    public void giveKit(Player player) {
        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());
        switch (mode) {
            case LOWMID:    giveLowMidKit(player);     break;
            case ONEWIDE:   giveOneWideKit(player);    break;
            case BLOCKFIGHT:giveBlockFightKit(player); break;
            case PLATFORM:
            default:        givePlatformKit(player);   break;
        }
    }

    // ============================================================
    //  PLATFORM
    // ============================================================
    private void givePlatformKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.getInventory().setHelmet(dyedLeather(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(dyedLeather(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(protectionIron(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(protectionIron(Material.IRON_BOOTS));

        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
        player.getInventory().setItem(0, unbreakable(sword));

        refillFood(player);
        player.updateInventory();
    }

    // ============================================================
    //  LOWMID
    // ============================================================
    private void giveLowMidKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
        player.getInventory().setItem(0, unbreakable(sword));

        refillFood(player);
        player.updateInventory();
    }

    // ============================================================
    //  ONEWIDE
    //  Sword is given only when player leaves the spawn zone.
    //  Spawn zone = Z threshold set via /realstics setzshowsword
    // ============================================================
    private void giveOneWideKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        player.getInventory().setItem(0, unbreakable(sword));

        refillFood(player);
        player.updateInventory();
    }

    // ============================================================
    //  BLOCKFIGHT
    // ============================================================
    private void giveBlockFightKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Slot 1 — Diamond Sword (Sharpness IV, 12+ dmg)
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 4);
        player.getInventory().setItem(0, unbreakable(sword));

        // Slot 2 — 64 Wool (never decreases)
        player.getInventory().setItem(1, new ItemStack(Material.WOOL, 64));

        // Slot 3 — Shears (Unbreakable)
        player.getInventory().setItem(2, unbreakable(new ItemStack(Material.SHEARS)));

        refillFood(player);
        player.updateInventory();
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private void refillFood(Player player) {
        player.setFoodLevel(20);
        player.setSaturation(20.0F);
        player.setExhaustion(0.0F);
    }

    private ItemStack dyedLeather(Material mat) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(Color.fromRGB(LEATHER_COLOR));
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack protectionIron(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 3, true);
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack unbreakable(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.spigot().setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    // ============================================================
    //  SPAWN
    // ============================================================
    public void teleportToSpawn(Player player) {
        Location spawn = getSpawnLocation(player.getWorld());
        if (spawn == null) return;
        player.teleport(spawn);
    }

    public Location getSpawnLocation(World world) {
        GameMode mode = gameModeManager.getModeForWorld(world);
        FileConfiguration config = gameModeManager.getConfig(mode);
        return readSpawn(config);
    }

    public Location readSpawn(FileConfiguration config) {
        if (config == null) return null;
        if (!config.contains("spawn.world")) return null;

        String worldName = config.getString("spawn.world");
        if (worldName == null) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = config.getDouble("spawn.x");
        double y = config.getDouble("spawn.y");
        double z = config.getDouble("spawn.z");
        float yaw = (float) config.getDouble("spawn.yaw");
        float pitch = (float) config.getDouble("spawn.pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }
}
'''

JAVA["NoDamage.java"] = r'''package org.realstics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cosmetic PvP rules (applies to ALL modes):
 *   - PvP fully functional (hits + knockback)
 *   - HP never drops
 *   - Fall damage off
 *   - Infinite food
 */
public class NoDamage implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;

    public NoDamage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID) return;
        event.setDamage(0);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!event.isCancelled()) {
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFoodChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.getFoodLevel() < 20) {
            event.setCancelled(true);
            player.setFoodLevel(20);
            player.setSaturation(20.0F);
            player.setExhaustion(0.0F);
        }
    }
}
'''

JAVA["Protection.java"] = r'''package org.realstics;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Protection:
 *   - Cannot break map blocks   (realstics.break)
 *   - Cannot place blocks       (realstics.place)
 *   - Cannot drop items         (realstics.drop)
 *   - realstics.bypass bypasses all
 *
 * BlockFight exception: wool placement is allowed on the map.
 */
public class Protection implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;

    private static final String PERM_BYPASS = "realstics.bypass";
    private static final String PERM_BREAK  = "realstics.break";
    private static final String PERM_PLACE  = "realstics.place";
    private static final String PERM_DROP   = "realstics.drop";

    public Protection(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_BREAK)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot break blocks here!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;

        // BlockFight — wool placement allowed
        Material type = event.getBlock().getType();
        if (type == Material.WOOL) return;

        if (player.hasPermission(PERM_PLACE)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot place blocks here!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_DROP)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot drop items here!"));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
'''

JAVA["KitRestore.java"] = r'''package org.realstics;

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

/**
 * Restores the cosmetic kit when the inventory is empty.
 */
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
            player.sendMessage(colorize("&aYour cosmetic kit has been restored."));
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
'''

JAVA["Welcome.java"] = r'''package org.realstics;

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

/**
 * Custom join/quit messages per world's game mode.
 */
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

        // Use default config.yml message (Platform) — global join/quit
        FileConfiguration config = this.plugin.getConfig();
        String joinMessage = config.getString("join-message",
                "&b%player% &7joined the game &8(&b%online%&7/&b%max_online%&8)");

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
                "&b%player% &7left the game &8(&b%online%&7/&b%max_online%&8)");

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
'''

JAVA["Void.java"] = r'''package org.realstics;

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

/**
 * Per-mode void system.
 * Reads kill-height from the current world's mode config file.
 * Falls below => teleport to spawn, heal, kit restore.
 */
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
        // Nothing to cache — read live from mode config
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
'''

JAVA["ComboSystem.java"] = r'''package org.realstics;

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

/**
 * Combo System — globally shared, config from default config.yml.
 */
public class ComboSystem implements Listener {

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
        this.broadcastMessage = config.getString("combo.broadcast-message",
                "&8&m-------------------------------\n"
              + "&6&l⚔ COMBO &e&l%combo%x\n"
              + "&e%attacker% &7got a combo on &c%victim% &7(&6%combo% &7combo)\n"
              + "&8&m-------------------------------");
        this.soundEnabled   = config.getBoolean("combo.sound-enabled", true);

        if (this.comboStep < 1) this.comboStep = 10;
        if (this.comboResetTime < 500L) this.comboResetTime = 3000L;

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
'''

JAVA["ScoreboardManager.java"] = r'''package org.realstics;

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
'''

JAVA["RealsticsCommand.java"] = r'''package org.realstics;

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

/**
 * /realstics <mode> <subcommand>
 *
 *   /realstics setworld <world> <mode>     - assign a world to a mode
 *   /realstics <mode> setspawn             - set spawn for that mode
 *   /realstics <mode> setvoid [y]          - set void Y for that mode
 *   /realstics <mode> setzshowsword <z>    - OneWide only
 *   /realstics <mode> kit [player]         - give kit
 *   /realstics <mode> sb                   - toggle scoreboard
 *   /realstics <mode> sb reload            - reload mode scoreboard
 *   /realstics reload                      - reload everything
 *   /realstics creator                     - credits
 *   /realstics help
 */
public class RealsticsCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PlayerJoin playerJoin;
    private final ScoreboardManager scoreboardManager;
    private final Void voidSystem;
    private final GameModeManager gameModeManager;

    public RealsticsCommand(JavaPlugin plugin,
                            PlayerJoin playerJoin,
                            ScoreboardManager scoreboardManager,
                            Void voidSystem,
                            GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.playerJoin = playerJoin;
        this.scoreboardManager = scoreboardManager;
        this.voidSystem = voidSystem;
        this.gameModeManager = gameModeManager;
    }

    // ============================================================
    //  onCommand
    // ============================================================
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ---------- top-level ----------
        if (sub.equals("help")) {
            sendHelp(sender);
            return true;
        }

        if (sub.equals("creator")) {
            return handleCreator(sender);
        }

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

        // ---------- mode-scoped ----------
        GameMode mode = GameMode.fromId(sub);
        if (mode == null) {
            sender.sendMessage(colorize("&cUnknown subcommand. Use /realstics help"));
            return true;
        }

        if (args.length < 2) {
            sendModeHelp(sender, mode);
            return true;
        }

        String action = args[1].toLowerCase();

        if (action.equals("setspawn"))       return handleSetSpawn(sender, mode);
        if (action.equals("setvoid"))        return handleSetVoid(sender, mode, args);
        if (action.equals("setzshowsword"))  return handleSetZShowSword(sender, mode, args);
        if (action.equals("kit"))            return handleKit(sender, mode, args);
        if (action.equals("sb") || action.equals("scoreboard"))
                                             return handleScoreboard(sender, mode, args);

        sender.sendMessage(colorize("&cUnknown action. Use /realstics help"));
        return true;
    }

    // ============================================================
    //  /realstics setworld <world> <mode>
    // ============================================================
    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("realstics.setworld")) { sendNoPerm(sender); return true; }

        if (args.length < 3) {
            sender.sendMessage(colorize("&cUsage: /realstics setworld <world> <mode>"));
            sender.sendMessage(colorize("&7Modes: platform, lowmid, onewide, blockfight"));
            return true;
        }

        String worldName = args[1].toLowerCase();
        GameMode mode = GameMode.fromId(args[2]);
        if (mode == null) {
            sender.sendMessage(colorize("&cUnknown mode: &e" + args[2]));
            sender.sendMessage(colorize("&7Modes: platform, lowmid, onewide, blockfight"));
            return true;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(colorize("&cWorld not found: &e" + worldName));
            return true;
        }

        // If sender is a player, teleport them to that world
        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.teleport(world.getSpawnLocation());
            player.sendMessage(colorize("&aTeleported to &e" + world.getName()
                    + "&a. Now run &e/realstics " + mode.getId() + " setspawn"));
        }

        gameModeManager.setWorldMode(worldName, mode);
        sender.sendMessage(colorize("&aWorld &e" + worldName + " &ais now game mode &e"
                + mode.getDisplayName() + "&a."));
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
        if (nowVisible) {
            player.sendMessage(colorize("&aScoreboard &lENABLED&a."));
        } else {
            player.sendMessage(colorize("&cScoreboard &lDISABLED&c."));
        }
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
        sender.sendMessage(colorize("&e/realstics creator &7- Show plugin credits"));
        sender.sendMessage(colorize("&e/realstics reload &7- Reload all configs"));
        sender.sendMessage(colorize("&e/realstics setworld <world> <mode> &7- Assign a world"));
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
            subs.add("creator");
            subs.add("help");
            subs.add("reload");
            subs.add("setworld");
            subs.add("platform");
            subs.add("lowmid");
            subs.add("onewide");
            subs.add("blockfight");

            String partial = args[0].toLowerCase();
            for (String s : subs) {
                if (s.startsWith(partial)) out.add(s);
            }
            return out;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();

            if (sub.equals("setworld")) {
                for (World w : Bukkit.getWorlds()) out.add(w.getName().toLowerCase());
                return out;
            }

            GameMode mode = GameMode.fromId(sub);
            if (mode != null) {
                List<String> actions = new ArrayList<String>();
                actions.add("setspawn");
                actions.add("setvoid");
                actions.add("kit");
                actions.add("sb");
                if (mode == GameMode.ONEWIDE) actions.add("setzshowsword");

                String partial = args[1].toLowerCase();
                for (String s : actions) {
                    if (s.startsWith(partial)) out.add(s);
                }
            }
            return out;
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String action = args[1].toLowerCase();

            if (sub.equals("setworld")) {
                out.add("platform");
                out.add("lowmid");
                out.add("onewide");
                out.add("blockfight");
                return out;
            }

            if (action.equals("kit")) {
                String partial = args[2].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) out.add(p.getName());
                }
                return out;
            }

            if (action.equals("sb")) {
                out.add("reload");
                return out;
            }
        }

        return out;
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private void sendNoPerm(CommandSender sender) {
        sender.sendMessage(colorize("&cYou do not have permission to do this."));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
'''

# ==============================================================
#  RESOURCES
# ==============================================================
RESOURCES = {}

RESOURCES["config.yml"] = r'''# ==========================================================
#  Realstics Configuration — Platform (default mode)
#  Version: 1.0
# ==========================================================
#  AUTO-MERGED on plugin update. Existing values are preserved.
# ==========================================================

# ----------------------------------------------------------
#  Spawn (used by Platform mode)
#  Set with /realstics platform setspawn
# ----------------------------------------------------------
spawn:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

# ----------------------------------------------------------
#  Void (Platform)
#  Set with /realstics platform setvoid [y]
# ----------------------------------------------------------
void:
  kill-height: -13.0

# ----------------------------------------------------------
#  Join / Quit messages (global — all modes)
# ----------------------------------------------------------
join-message: '&b%player% &7joined the game &8(&b%online%&7/&b%max_online%&8)'
quit-message: '&b%player% &7left the game &8(&b%online%&7/&b%max_online%&8)'

# ----------------------------------------------------------
#  Combo System (global — all modes)
# ----------------------------------------------------------
combo:
  enabled: true
  step: 10
  reset-time: 3000
  sound-enabled: true
  broadcast-message: "&8&m-------------------------------\n&6&lCOMBO &e&l%combo%x\n&e%attacker% &7got a combo on &c%victim% &7(&6%combo% &7combo)\n&8&m-------------------------------"

# ----------------------------------------------------------
#  Scoreboard update interval (global)
# ----------------------------------------------------------
scoreboard:
  update-interval: 10

# ----------------------------------------------------------
#  World -> Game Mode mapping
#  Default: world = platform
#  Set others with: /realstics setworld <world> <mode>
#  Valid modes: platform, lowmid, onewide, blockfight
# ----------------------------------------------------------
worlds:
  world: platform
'''

RESOURCES["scoreboard.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — Platform mode
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&6Platform'
  frames:
    - '&6P&flatform'
    - '&6Pl&fatForm'
    - '&6Pla&ftform'
    - '&6Plat&fform'
    - '&6Platf&form'
    - '&6Platfo&frm'
    - '&6Platfor&fm'
    - '&6Platform'

lines:
  - '&8&m----------------'
  - '&6Player &8» &f%player%'
  - '&6World  &8» &f%world%'
  - '&6Ping   &8» &f%ping%ms'
  - '&6Health &8» &f%health%'
  - '&6Food   &8» &f%food%'
  - '&6Y      &8» &f%y%'
  - '&8&m----------------'
  - '&6Online &8» &f%online%&7/&f%max_online%'
  - '&8&m----------------'
  - '&6Play.MineStorm.iR'
  - '&8&m----------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

RESOURCES["lowmid.yml"] = r'''# ==========================================================
#  Realstics — LowMid mode
# ==========================================================
#  Kit: Wooden Sword (Sharpness I, 5.25 dmg, Unbreakable)
#  No damage, no fall damage, infinite food.
# ==========================================================

spawn:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

void:
  kill-height: -13.0
'''

RESOURCES["sb-lowmid.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — LowMid mode
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&6LowMid'
  frames:
    - '&6L&fowMid'
    - '&6Lo&fwMid'
    - '&6Low&fMid'
    - '&6LowM&fid'
    - '&6LowMi&fd'
    - '&6LowMid'

lines:
  - '&8&m----------------'
  - '&6Mode   &8» &f%mode%'
  - '&6Player &8» &f%player%'
  - '&6World  &8» &f%world%'
  - '&6Ping   &8» &f%ping%ms'
  - '&6Y      &8» &f%y%'
  - '&8&m----------------'
  - '&6Online &8» &f%online%&7/&f%max_online%'
  - '&8&m----------------'
  - '&6Play.MineStorm.iR'
  - '&8&m----------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

RESOURCES["onewide.yml"] = r'''# ==========================================================
#  Realstics — OneWide mode
# ==========================================================
#  Kit: Iron Sword (6 dmg, Unbreakable)
#  Sword is HIDDEN while player is inside the spawn zone.
#  Sword appears once the player passes the Z threshold.
#
#  Set Z threshold with:  /realstics onewide setzshowsword <z>
# ==========================================================

spawn:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

void:
  kill-height: -13.0

# Z coordinate from which the sword becomes visible
zshowsword: 0.0
'''

RESOURCES["sb-onewide.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — OneWide mode
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&6OneWide'
  frames:
    - '&6O&fneWide'
    - '&6On&feWide'
    - '&6One&fWide'
    - '&6OneW&fide'
    - '&6OneWi&fde'
    - '&6OneWid&fe'
    - '&6OneWide'

lines:
  - '&8&m----------------'
  - '&6Mode   &8» &f%mode%'
  - '&6Player &8» &f%player%'
  - '&6World  &8» &f%world%'
  - '&6Ping   &8» &f%ping%ms'
  - '&6Y      &8» &f%y%'
  - '&8&m----------------'
  - '&6Online &8» &f%online%&7/&f%max_online%'
  - '&8&m----------------'
  - '&6Play.MineStorm.iR'
  - '&8&m----------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

RESOURCES["blockfight.yml"] = r'''# ==========================================================
#  Realstics — BlockFight mode
# ==========================================================
#  Kit:
#    Slot 1 — Diamond Sword (Sharpness IV, 12+ dmg, Unbreakable)
#    Slot 2 — 64 Wool (never decreases)
#    Slot 3 — Shears (Unbreakable)
#
#  Wool may be placed on the map.
# ==========================================================

spawn:
  world: world
  x: 0.5
  y: 100.0
  z: 0.5
  yaw: 0.0
  pitch: 0.0

void:
  kill-height: -13.0

# Y level used by sety (same as void for symmetry)
sety: -13.0
'''

RESOURCES["sb-blockfight.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — BlockFight mode
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&6BlockFight'
  frames:
    - '&6B&flockFight'
    - '&6Bl&fockFight'
    - '&6Blo&fckFight'
    - '&6Bloc&fkFight'
    - '&6Block&fFight'
    - '&6BlockF&fight'
    - '&6BlockFi&fght'
    - '&6BlockFig&fht'
    - '&6BlockFigh&ft'
    - '&6BlockFight'

lines:
  - '&8&m----------------'
  - '&6Mode   &8» &f%mode%'
  - '&6Player &8» &f%player%'
  - '&6World  &8» &f%world%'
  - '&6Ping   &8» &f%ping%ms'
  - '&6Y      &8» &f%y%'
  - '&8&m----------------'
  - '&6Online &8» &f%online%&7/&f%max_online%'
  - '&8&m----------------'
  - '&6Play.MineStorm.iR'
  - '&8&m----------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

# ==============================================================
#  README
# ==============================================================
README = '''# Realstics Plugin v1.0

Multi-gamemode cosmetic PvP plugin for **Minecraft 1.8.8** — CarbonSpigot compatible.

**Created by Muvixo**

## Game Modes

| Mode | Status | Description |
|------|--------|-------------|
| **Platform** | Default / enabled | Leather + Iron armor (Prot III), Wooden Sword (Sharp I) |
| **LowMid** | Must be set up | Wooden Sword only (Sharp I, 5.25 dmg) |
| **OneWide** | Must be set up | Iron Sword (6 dmg), hidden in spawn zone |
| **BlockFight** | Must be set up | Diamond Sword (Sharp IV, 12+ dmg), 64 Wool, Shears |

All modes share:
- PvP enabled, **no HP loss** (knockback + hits still work)
- No fall damage
- Infinite food
- Block / place / drop protection (permission-based)
- Auto kit restore
- Per-mode scoreboard
- Per-mode void system
- Global combo system
- Global join / quit messages

## Commands

```
/realstics help
/realstics creator
/realstics reload
/realstics setworld <world> <mode>          # assign a world to a mode

/realstics <mode> setspawn
/realstics <mode> setvoid [y]
/realstics <mode> kit [player]
/realstics <mode> sb
/realstics <mode> sb reload
/realstics onewide setzshowsword <z>
```

Aliases: `/rs`, `/rl`

**Modes:** `platform`, `lowmid`, `onewide`, `blockfight`

## Permissions

| Permission | Default | Description |
|---|---|---|
| `realstics.setworld` | op | Assign a world to a mode |
| `realstics.setspawn` | op | Set spawn for a mode |
| `realstics.setvoid` | op | Set void Y |
| `realstics.setzshowsword` | op | OneWide Z threshold |
| `realstics.kit` | op | Give a kit |
| `realstics.reload` | op | Reload configs |
| `realstics.scoreboard` | true | Toggle scoreboard |
| `realstics.bypass` | op | Bypass all protection |
| `realstics.break` | false | Break blocks |
| `realstics.place` | false | Place blocks |
| `realstics.drop` | false | Drop items |

## Setup

1. Drop the JAR into `plugins/`
2. Restart the server
3. All config files are auto-created with safe merging
4. Default world (`world`) uses **Platform** automatically
5. For other modes:
   ```
   /realstics setworld <world> lowmid
   /realstics lowmid setspawn
   /realstics lowmid setvoid -13
   ```
   Same for `onewide` and `blockfight`.

## Building

```bash
mvn clean package
```

Output: `target/Realstics.jar`

## Credits

- **Muvixo** — Creator
'''

# ==============================================================
#  WRITE EVERYTHING
# ==============================================================
def write_file(rel_path, content):
    full = os.path.join(BASE, rel_path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(content)
    print("  + " + rel_path)

def main():
    print("Realstics plugin generator")
    print("=" * 50)

    print("\n[1/4] Creating directories...")
    for d in DIRS:
        full = os.path.join(BASE, d)
        os.makedirs(full, exist_ok=True)
        print("  + " + d + "/")

    print("\n[2/4] Writing pom.xml, plugin.yml, README.md...")
    write_file("pom.xml", POM_XML)
    write_file("plugin.yml", PLUGIN_YML)
    write_file("README.md", README)

    print("\n[3/4] Writing Java sources...")
    for name, content in JAVA.items():
        write_file(os.path.join("src/main/java/org/realstics", name), content)

    print("\n[4/4] Writing resources...")
    for name, content in RESOURCES.items():
        write_file(os.path.join("src/main/resources", name), content)

    print("\n" + "=" * 50)
    print("Done! Realstics plugin generated.")
    print("Build with:  mvn clean package")
    print("Output:      target/Realstics.jar")

if __name__ == "__main__":
    main()
