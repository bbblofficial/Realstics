
#!/usr/bin/env python3
"""
create.py — Realstics Plugin Generator (FULLY COMPLETE)

FEATURES:
  1. plugin.yml inside src/main/resources/ → Maven packs into JAR
  2. WorldLoader auto-loads/creates missing worlds
  3. /realstics join <mode>       → teleport + kit + scoreboard
  4. /realstics worlds            → list loaded worlds
  5. /realstics setworld [world] <mode> → auto-loads world
  6. BlockFight: ONLY light-blue wool (data=3) breakable
  7. BlockFight: wool is infinite (always 64)
  8. BlockFight: broken wool drops NOTHING
  9. Auto-fill EMPTY combo message on load (1.8.8 compatible)
 10. Aqua + White theme by default in ALL configs
 11. Platform & OneWide: PvP zone via /realstics <mode> setpvpzone <z>

Run:   python3 create.py
Build: mvn clean package
"""

import os

BASE = os.path.dirname(os.path.abspath(__file__))

DIRS = [
    "src/main/java/org/realstics",
    "src/main/resources",
    ".github/workflows",
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
    usage: /realstics <join|setworld|help|...>
    aliases:
      - rs
      - rl
permissions:
  realstics.join:
    description: Join a game mode world
    default: true
  realstics.setworld:
    description: Assign a world to a mode (auto-loads if missing)
    default: op
  realstics.setspawn:
    description: Set spawn for a mode
    default: op
  realstics.setvoid:
    description: Set void Y level
    default: op
  realstics.setzshowsword:
    description: Set OneWide Z threshold for showing sword
    default: op
  realstics.setpvpzone:
    description: Set the PvP zone for a mode (Platform/OneWide)
    default: op
  realstics.reload:
    description: Reload the config
    default: op
  realstics.kit:
    description: Give the cosmetic kit
    default: op
  realstics.scoreboard:
    description: Toggle the scoreboard
    default: true

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
#  GITHUB ACTIONS
# ==============================================================
GITHUB_WORKFLOW = '''name: Build Realstics

on:
  push:
    branches: [ main, master ]
    tags: [ 'v*' ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  build:
    name: Build with Maven
    runs-on: ubuntu-latest

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 8
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '8'
          cache: 'maven'

      - name: Build with Maven
        run: mvn -B clean package --file pom.xml

      - name: Upload JAR as artifact
        uses: actions/upload-artifact@v4
        with:
          name: Realstics-JAR
          path: target/Realstics.jar
          if-no-files-found: error

  release:
    name: Publish Release
    needs: build
    runs-on: ubuntu-latest
    if: startsWith(github.ref, 'refs/tags/v')

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Download JAR artifact
        uses: actions/download-artifact@v4
        with:
          name: Realstics-JAR
          path: ./release

      - name: Create GitHub Release
        uses: softprops/action-gh-release@v2
        with:
          files: ./release/Realstics.jar
          name: Realstics ${{ github.ref_name }}
          generate_release_notes: true
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
'''

# ==============================================================
#  JAVA FILES
# ==============================================================
JAVA = {}

JAVA["GameMode.java"] = r'''package org.realstics;

public enum GameMode {

    PLATFORM  ("platform",   "Platform",   "config.yml",      "scoreboard.yml"),
    LOWMID    ("lowmid",     "LowMid",     "lowmid.yml",      "sb-lowmid.yml"),
    ONEWIDE   ("onewide",    "OneWide",    "onewide.yml",     "sb-onewide.yml"),
    BLOCKFIGHT("blockfight", "BlockFight", "blockfight.yml",  "sb-blockfight.yml");

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

JAVA["WorldLoader.java"] = r'''package org.realstics;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

public class WorldLoader {

    private final JavaPlugin plugin;

    public WorldLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public World ensureLoaded(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) return null;

        World existing = findLoaded(worldName);
        if (existing != null) return existing;

        File serverRoot = plugin.getServer().getWorldContainer();
        File worldFolder = new File(serverRoot, worldName);
        boolean folderExists = worldFolder.exists()
                && new File(worldFolder, "level.dat").exists();

        try {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            creator.generateStructures(true);

            World world = creator.createWorld();

            if (world != null) {
                if (folderExists) {
                    plugin.getLogger().info("[WorldLoader] Loaded existing world: " + worldName);
                } else {
                    plugin.getLogger().info("[WorldLoader] Created new world: " + worldName);
                }
                return world;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[WorldLoader] Failed to load world '"
                    + worldName + "': " + t.getMessage());
        }

        return null;
    }

    public World findLoaded(String worldName) {
        if (worldName == null) return null;
        String target = worldName.trim().toLowerCase();
        for (World w : Bukkit.getWorlds()) {
            if (w.getName().toLowerCase().equals(target)) return w;
        }
        return null;
    }

    public String listLoadedWorldNames() {
        StringBuilder sb = new StringBuilder();
        for (World w : Bukkit.getWorlds()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(w.getName());
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
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
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class GameModeManager {

    private final JavaPlugin plugin;
    private final WorldLoader worldLoader;

    private final Map<String, GameMode> worldModes = new HashMap<String, GameMode>();
    private final Map<GameMode, FileConfiguration> modeConfigs =
            new HashMap<GameMode, FileConfiguration>();
    private final Map<GameMode, FileConfiguration> modeScoreboards =
            new HashMap<GameMode, FileConfiguration>();

    public GameModeManager(JavaPlugin plugin, WorldLoader worldLoader) {
        this.plugin = plugin;
        this.worldLoader = worldLoader;
    }

    public void init() {
        for (GameMode mode : GameMode.values()) {
            ensureModeConfig(mode);
            ensureModeScoreboard(mode);
        }
        loadWorldMappings();
    }

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
        World w = worldLoader.findLoaded(worldName);
        return getModeForWorld(w);
    }

    public void setWorldMode(String worldName, GameMode mode) {
        worldModes.put(worldName.toLowerCase(), mode);
        FileConfiguration cfg = plugin.getConfig();
        cfg.set("worlds." + worldName.toLowerCase(), mode.getId());
        plugin.saveConfig();
    }

    public String getWorldForMode(GameMode mode) {
        if (mode == null) return null;
        for (Map.Entry<String, GameMode> entry : worldModes.entrySet()) {
            if (entry.getValue() == mode) return entry.getKey();
        }
        return null;
    }

    public boolean isWorldConfigured(String worldName) {
        return worldModes.containsKey(worldName.toLowerCase());
    }

    public Map<String, GameMode> getWorldModes() {
        return worldModes;
    }

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
    private WorldLoader worldLoader;

    @Override
    public void onEnable() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        createConfigIfMissing();
        saveDefaultConfig();
        reloadConfig();

        this.worldLoader = new WorldLoader(this);

        this.gameModeManager = new GameModeManager(this, this.worldLoader);
        this.gameModeManager.init();

        for (String worldName : this.gameModeManager.getWorldModes().keySet()) {
            if (this.worldLoader.findLoaded(worldName) == null) {
                this.worldLoader.ensureLoaded(worldName);
            }
        }

        this.playerJoin = new PlayerJoin(this, this.gameModeManager);
        getServer().getPluginManager().registerEvents(this.playerJoin, this);
        getServer().getPluginManager().registerEvents(
                new NoDamage(this, this.gameModeManager), this);
        getServer().getPluginManager().registerEvents(
                new Protection(this, this.gameModeManager), this);
        getServer().getPluginManager().registerEvents(new KitRestore(this, this.playerJoin), this);
        getServer().getPluginManager().registerEvents(new Welcome(this), this);

        this.voidSystem = new Void(this, this.playerJoin, this.gameModeManager);
        getServer().getPluginManager().registerEvents(this.voidSystem, this);

        this.comboSystem = new ComboSystem(this);
        getServer().getPluginManager().registerEvents(this.comboSystem, this);

        this.scoreboardManager = new ScoreboardManager(this, this.gameModeManager);

        RealsticsCommand cmd = new RealsticsCommand(this,
                this.playerJoin, this.scoreboardManager, this.voidSystem,
                this.gameModeManager, this.worldLoader);
        getCommand("realstics").setExecutor(cmd);
        getCommand("realstics").setTabCompleter(cmd);

        getLogger().info("=================================================");
        getLogger().info("  Realstics v1.0 - Enabled");
        getLogger().info("  Modes: Platform, LowMid, OneWide, BlockFight");
        getLogger().info("  Loaded worlds: " + worldLoader.listLoadedWorldNames());
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }
        getLogger().info("Realstics disabled.");
    }

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

        setIfMissingOrEmpty(cfg, "join-message",
                "&b%player% &fjoined the game &7(&b%online%&7/&b%max_online%&7)");
        setIfMissingOrEmpty(cfg, "quit-message",
                "&b%player% &fleft the game &7(&b%online%&7/&b%max_online%&7)");

        setIfMissing(cfg, "combo.enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "combo.step", Integer.valueOf(10));
        setIfMissing(cfg, "combo.reset-time", Long.valueOf(3000L));
        setIfMissing(cfg, "combo.sound-enabled", Boolean.valueOf(true));

        String defaultComboMsg =
                "&b&m-------------------------------\n"
              + "&bCOMBO &f%combo%x\n"
              + "&b%attacker% &fcomboed &b%victim% &7(&f%combo% &7combo)\n"
              + "&b&m-------------------------------";
        setIfMissingOrEmpty(cfg, "combo.broadcast-message", defaultComboMsg);

        setIfMissing(cfg, "scoreboard.update-interval", Integer.valueOf(10));

        // ---- pvp zone (Platform only, global default) ----
        setIfMissing(cfg, "pvpzone.enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "pvpzone.z", Double.valueOf(0.0D));

        setIfMissing(cfg, "worlds.world", "platform");

        try {
            cfg.save(configFile);
        } catch (IOException e) {
            getLogger().warning("Could not save config.yml: " + e.getMessage());
        }
    }

    private void setIfMissing(FileConfiguration cfg, String path, Object value) {
        if (!cfg.contains(path)) {
            cfg.set(path, value);
        }
    }

    private void setIfMissingOrEmpty(FileConfiguration cfg, String path, Object defaultValue) {
        if (!cfg.contains(path)) {
            cfg.set(path, defaultValue);
            return;
        }
        Object existing = cfg.get(path);
        if (existing == null) {
            cfg.set(path, defaultValue);
            return;
        }
        if (existing instanceof String) {
            String s = (String) existing;
            if (s.trim().isEmpty()) {
                cfg.set(path, defaultValue);
            }
        }
    }

    public PlayerJoin getPlayerJoin()               { return playerJoin; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public Void getVoidSystem()                     { return voidSystem; }
    public ComboSystem getComboSystem()             { return comboSystem; }
    public GameModeManager getGameModeManager()     { return gameModeManager; }
    public WorldLoader getWorldLoader()             { return worldLoader; }
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

public class PlayerJoin implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private static final int LEATHER_COLOR = 16711680;
    private static final short LIGHT_BLUE_WOOL_DATA = 3;

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

    public void giveKit(Player player) {
        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());
        switch (mode) {
            case LOWMID:     giveLowMidKit(player);     break;
            case ONEWIDE:    giveOneWideKit(player);    break;
            case BLOCKFIGHT: giveBlockFightKit(player); break;
            case PLATFORM:
            default:         givePlatformKit(player);   break;
        }
    }

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

    private void giveLowMidKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
        player.getInventory().setItem(0, unbreakable(sword));

        refillFood(player);
        player.updateInventory();
    }

    private void giveOneWideKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        player.getInventory().setItem(0, unbreakable(sword));

        refillFood(player);
        player.updateInventory();
    }

    private void giveBlockFightKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 4);
        player.getInventory().setItem(0, unbreakable(sword));

        ItemStack wool = new ItemStack(Material.WOOL, 64);
        wool.setDurability(LIGHT_BLUE_WOOL_DATA);
        player.getInventory().setItem(1, wool);

        player.getInventory().setItem(2, unbreakable(new ItemStack(Material.SHEARS)));

        refillFood(player);
        player.updateInventory();
    }

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

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Cosmetic PvP rules + PvP-zone enforcement.
 *
 * All modes:
 *   - HP never drops (damage set to 0)
 *   - Fall damage off
 *   - Infinite food
 *
 * Platform / OneWide:
 *   - PvP is only allowed when the attacker's Z >= pvpzone.z
 *   - Outside the zone, hits are cancelled entirely
 *
 * LowMid / BlockFight:
 *   - PvP always allowed (no zone check)
 */
public class NoDamage implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    public NoDamage(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    // ============================================================
    //  Zone check
    // ============================================================
    private boolean isInPvpZone(Player attacker) {
        World world = attacker.getWorld();
        GameMode mode = gameModeManager.getModeForWorld(world);

        // LowMid & BlockFight — always allow PvP
        if (mode == GameMode.LOWMID || mode == GameMode.BLOCKFIGHT) {
            return true;
        }

        // Platform / OneWide — read zone from config
        FileConfiguration cfg = gameModeManager.getConfig(mode);
        boolean enabled = cfg.getBoolean("pvpzone.enabled", true);
        if (!enabled) return true;

        double zoneZ = cfg.getDouble("pvpzone.z", 0.0);
        return attacker.getLocation().getZ() >= zoneZ;
    }

    // ============================================================
    //  Main damage event
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnyDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID) return;
        event.setDamage(0);
    }

    // ============================================================
    //  PvP damage — zone-aware
    // ============================================================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        // Non-player attacker → zero damage, no zone check
        if (!(event.getDamager() instanceof Player)) {
            if (!event.isCancelled()) {
                event.setDamage(0);
            }
            return;
        }

        Player attacker = (Player) event.getDamager();

        if (isInPvpZone(attacker)) {
            // Normal PvP behavior — keep knockback, zero damage
            if (!event.isCancelled()) {
                event.setDamage(0);
            }
        } else {
            // Outside PvP zone — cancel entirely
            event.setCancelled(true);
            event.setDamage(0);
            attacker.sendMessage(colorize("&bYou must enter the PvP zone first!"));
        }
    }

    // ============================================================
    //  Fall damage
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            event.setDamage(0);
        }
    }

    // ============================================================
    //  Infinite food
    // ============================================================
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

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
'''

JAVA["Protection.java"] = r'''package org.realstics;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Protection implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private final Set<String> playerPlacedWool = new HashSet<String>();

    private static final byte LIGHT_BLUE_DATA = 3;

    private static final String PERM_BYPASS = "realstics.bypass";
    private static final String PERM_BREAK  = "realstics.break";
    private static final String PERM_PLACE  = "realstics.place";
    private static final String PERM_DROP   = "realstics.drop";

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
    }

    private boolean isBlockFight(World world) {
        return gameModeManager.getModeForWorld(world) == GameMode.BLOCKFIGHT;
    }

    private String locKey(Block block) {
        return block.getWorld().getName()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();
    }

    @SuppressWarnings("deprecation")
    private boolean isLightBlueWool(Block block) {
        if (block.getType() != Material.WOOL) return false;
        return block.getData() == LIGHT_BLUE_DATA;
    }

    @SuppressWarnings("deprecation")
    private boolean isLightBlueWoolItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.WOOL) return false;
        return item.getData().getData() == LIGHT_BLUE_DATA;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_BREAK)) return;

        if (isBlockFight(block.getWorld()) && isLightBlueWool(block)) {
            String key = locKey(block);
            if (playerPlacedWool.contains(key)) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                playerPlacedWool.remove(key);
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                refillWool(player);
                            }
                        }, 1L);
                return;
            }
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou can only break light blue wool placed by players!"));
            return;
        }

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou can only break light blue wool!"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (hasBypass(player)) return;

        Material type = event.getBlock().getType();
        if (type == Material.WOOL) {
            if (isBlockFight(world) && isLightBlueWool(event.getBlock())) {
                playerPlacedWool.add(locKey(event.getBlock()));
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                refillWool(player);
                            }
                        }, 1L);
            }
            return;
        }

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

    private void refillWool(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isLightBlueWoolItem(item)) {
                if (item.getAmount() < 64) {
                    item.setAmount(64);
                    inv.setItem(i, item);
                    player.updateInventory();
                }
                return;
            }
        }

        ItemStack wool = new ItemStack(Material.WOOL, 64);
        wool.setDurability(LIGHT_BLUE_DATA);
        inv.setItem(1, wool);
        player.updateInventory();
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

public class ComboSystem implements Listener {

    private static final String DEFAULT_MESSAGE =
            "&b&m-------------------------------\n"
          + "&bCOMBO &f%combo%x\n"
          + "&b%attacker% &fcomboed &b%victim% &7(&f%combo% &7combo)\n"
          + "&b&m-------------------------------";

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

        String msg = config.getString("combo.broadcast-message", null);
        if (msg == null || msg.trim().isEmpty()) {
            msg = DEFAULT_MESSAGE;
        }
        this.broadcastMessage = msg;

        this.soundEnabled = config.getBoolean("combo.sound-enabled", true);

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
            if (frames == null || frames.isEmpty()) return "&bRealstics";
            return frames.get(this.animationFrame % frames.size());
        }
        return sb.getString("title.static", "&bRealstics");
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
            sender.sendMessage(colorize("&bRealstics configuration reloaded."));
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
        if (action.equals("setpvpzone"))    return handleSetPvpZone(sender, mode, args);
        if (action.equals("kit"))           return handleKit(sender, mode, args);
        if (action.equals("sb") || action.equals("scoreboard"))
                                            return handleScoreboard(sender, mode, args);

        sender.sendMessage(colorize("&cUnknown action. Use /realstics help"));
        return true;
    }

    private boolean handleWorlds(CommandSender sender) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bRealstics &f- &bLoaded Worlds"));
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

        World targetWorld = worldLoader.ensureLoaded(worldName);
        if (targetWorld == null) {
            player.sendMessage(colorize("&cWorld not available: &e" + worldName));
            return true;
        }

        if (player.getWorld().equals(targetWorld)) {
            player.sendMessage(colorize("&7You are already in &b"
                    + mode.getDisplayName() + "&7."));
            return true;
        }

        Location spawn = playerJoin.getSpawnLocation(targetWorld);
        if (spawn == null) {
            spawn = targetWorld.getSpawnLocation();
        }

        final World finalWorld = targetWorld;
        player.teleport(spawn);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    playerJoin.giveKit(player);
                }
            }
        }, 3L);

        player.sendMessage(colorize("&bJoined &f" + mode.getDisplayName()
                + " &b(world: &f" + finalWorld.getName() + "&b)"));
        return true;
    }

    private boolean handleSetWorld(CommandSender sender, String[] args) {
        if (!sender.hasPermission("realstics.setworld")) { sendNoPerm(sender); return true; }

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
                sender.sendMessage(colorize("&cFrom console use: /realstics setworld <world> <mode>"));
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
            sender.sendMessage(colorize("&cUsage: /realstics setworld [world] <mode>"));
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
            player.sendMessage(colorize("&7Next: &f/realstics " + mode.getId() + " setspawn"));
        } else {
            sender.sendMessage(colorize("&bWorld &f" + world.getName()
                    + " &bis now game mode &f" + mode.getDisplayName() + "&b."));
        }

        return true;
    }

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

        player.sendMessage(colorize("&b[" + mode.getDisplayName() + "] &fSpawn set to &b"
                + loc.getWorld().getName() + " "
                + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "&b."));
        return true;
    }

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

        sender.sendMessage(colorize("&b[" + mode.getDisplayName() + "] &fVoid kill height set to &b"
                + y + "&b."));
        return true;
    }

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

        sender.sendMessage(colorize("&b[OneWide] &fSword will appear once Z passes &b" + z + "&b."));
        return true;
    }

    // ============================================================
    //  /realstics <mode> setpvpzone [z]
    //  PvP is allowed only when the player's Z >= zone.
    //  Supported: Platform, OneWide
    // ============================================================
    private boolean handleSetPvpZone(CommandSender sender, GameMode mode, String[] args) {
        if (!sender.hasPermission("realstics.setpvpzone")) { sendNoPerm(sender); return true; }

        if (mode != GameMode.PLATFORM && mode != GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&csetpvpzone is only for Platform and OneWide modes."));
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
                sender.sendMessage(colorize("&cUsage: /realstics " + mode.getId() + " setpvpzone <z>"));
                return true;
            }
            z = ((Player) sender).getLocation().getZ();
        }

        FileConfiguration cfg = gameModeManager.getConfig(mode);
        cfg.set("pvpzone.enabled", Boolean.valueOf(true));
        cfg.set("pvpzone.z", Double.valueOf(z));
        gameModeManager.saveModeConfig(mode);

        sender.sendMessage(colorize("&b[" + mode.getDisplayName()
                + "] &fPvP zone set — PvP enabled from Z &b" + z + "&f."));
        return true;
    }

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
            if (!sender.hasPermission("realstics.reload")) { sendNoPerm(sender); return true; }
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
        sender.sendMessage(colorize("&bRealstics &f- &bCreated by &fMuvixo"));
        sender.sendMessage(colorize("&bVersion: &f1.0"));
        sender.sendMessage(colorize("&bModes: &fPlatform, LowMid, OneWide, BlockFight"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bRealstics &f- &bCommands"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b/realstics join <mode> &f- Join a game mode"));
        sender.sendMessage(colorize("&b/realstics worlds &f- List loaded worlds"));
        sender.sendMessage(colorize("&b/realstics creator &f- Show plugin credits"));
        sender.sendMessage(colorize("&b/realstics reload &f- Reload all configs"));
        sender.sendMessage(colorize("&b/realstics setworld [world] <mode> &f- Assign a world"));
        sender.sendMessage(colorize("&7Modes: &fplatform, lowmid, onewide, blockfight"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b/realstics <mode> setspawn &f- Set spawn"));
        sender.sendMessage(colorize("&b/realstics <mode> setvoid [y] &f- Set void Y"));
        sender.sendMessage(colorize("&b/realstics <mode> setpvpzone <z> &f- Set PvP zone"));
        sender.sendMessage(colorize("&b/realstics <mode> kit [player] &f- Give kit"));
        sender.sendMessage(colorize("&b/realstics <mode> sb &f- Toggle scoreboard"));
        sender.sendMessage(colorize("&b/realstics onewide setzshowsword <z> &f- OneWide only"));
        sender.sendMessage(colorize("&b&m----------------------------------"));
    }

    private void sendModeHelp(CommandSender sender, GameMode mode) {
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&bRealstics &f- &b" + mode.getDisplayName()));
        sender.sendMessage(colorize("&b&m----------------------------------"));
        sender.sendMessage(colorize("&b/realstics join " + mode.getId()));
        sender.sendMessage(colorize("&b/realstics " + mode.getId() + " setspawn"));
        sender.sendMessage(colorize("&b/realstics " + mode.getId() + " setvoid [y]"));
        if (mode == GameMode.PLATFORM || mode == GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&b/realstics " + mode.getId() + " setpvpzone <z>"));
        }
        sender.sendMessage(colorize("&b/realstics " + mode.getId() + " kit [player]"));
        sender.sendMessage(colorize("&b/realstics " + mode.getId() + " sb [reload]"));
        if (mode == GameMode.ONEWIDE) {
            sender.sendMessage(colorize("&b/realstics onewide setzshowsword <z>"));
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
'''

# ==============================================================
#  RESOURCES
# ==============================================================
RESOURCES = {}

RESOURCES["config.yml"] = r'''# ==========================================================
#  Realstics Configuration — Platform (default mode)
#  Version: 1.0
#  Theme: Aqua + White
# ==========================================================
#  AUTO-MERGED on plugin update. Existing values are preserved.
# ==========================================================

# ----------------------------------------------------------
#  Spawn (Platform mode)
#  Set with: /realstics platform setspawn
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
#  Set with: /realstics platform setvoid [y]
# ----------------------------------------------------------
void:
  kill-height: -13.0

# ----------------------------------------------------------
#  Combo System (global — all modes)
#  Placeholders: %combo%, %attacker%, %victim%
#  Use "\n" inside double quotes for a new line.
# ----------------------------------------------------------
combo:
  enabled: true
  step: 10
  reset-time: 3000
  sound-enabled: true
  broadcast-message: "&b&m-------------------------------\n&bCOMBO &f%combo%x\n&b%attacker% &fcomboed &b%victim% &7(&f%combo% &7combo)\n&b&m-------------------------------"

# ----------------------------------------------------------
#  Scoreboard (global)
# ----------------------------------------------------------
scoreboard:
  update-interval: 10

# ----------------------------------------------------------
#  Join / Quit messages (global — all modes)
#  Placeholders: %player%, %online%, %max_online%
# ----------------------------------------------------------
join-message: '&b%player% &fjoined the game &7(&b%online%&7/&b%max_online%&7)'
quit-message: '&b%player% &fleft the game &7(&b%online%&7/&b%max_online%&7)'

# ----------------------------------------------------------
#  PvP Zone (Platform only)
#  PvP is enabled only when the attacker's Z >= pvpzone.z
#  Set with: /realstics platform setpvpzone [z]
# ----------------------------------------------------------
pvpzone:
  enabled: true
  z: 0.0

# ----------------------------------------------------------
#  World -> Game Mode mapping
#  Valid modes: platform, lowmid, onewide, blockfight
#  Assign with: /realstics setworld [world] <mode>
# ----------------------------------------------------------
worlds:
  world: platform
'''

RESOURCES["scoreboard.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — Platform mode
#  Theme: Aqua + White (no bold)
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&bPlatform'
  frames:
    - '&bP&flatform'
    - '&bPl&fatForm'
    - '&bPla&ftform'
    - '&bPlat&fform'
    - '&bPlatf&form'
    - '&bPlatfo&frm'
    - '&bPlatfor&fm'
    - '&bPlatform'
    - '&fPlatform'
    - '&bPlatform'

lines:
  - '&b&m---------------------'
  - '&bPlatform &fPVP'
  - '&b&m---------------------'
  - '&b▪ &fPlayer &b» &f%player%'
  - '&b▪ &fWorld  &b» &f%world%'
  - '&b▪ &fPing   &b» &f%ping%ms'
  - '&b▪ &fY      &b» &f%y%'
  - '&b&m---------------------'
  - '&b▪ &fOnline &b» &f%online%&b/&f%max_online%'
  - '&b&m---------------------'
  - '&fPlay.&bMineStorm&f.iR'
  - '&b&m---------------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

RESOURCES["lowmid.yml"] = r'''# ==========================================================
#  Realstics — LowMid mode
#  Theme: Aqua + White
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
#  Theme: Aqua + White (no bold)
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&bLowMid'
  frames:
    - '&bL&fowmid'
    - '&bLo&f&lwmid'
    - '&bLow&fmid'
    - '&bLowM&fid'
    - '&bLowMi&fd'
    - '&bLowMid'
    - '&fLowMid'
    - '&bLowMid'

lines:
  - '&b&m---------------------'
  - '&bLowMid &fPVP'
  - '&b&m---------------------'
  - '&b▪ &fPlayer &b» &f%player%'
  - '&b▪ &fWorld  &b» &f%world%'
  - '&b▪ &fPing   &b» &f%ping%ms'
  - '&b▪ &fY      &b» &f%y%'
  - '&b&m---------------------'
  - '&b▪ &fOnline &b» &f%online%&b/&f%max_online%'
  - '&b&m---------------------'
  - '&fPlay.&bMineStorm&f.iR'
  - '&b&m---------------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

RESOURCES["onewide.yml"] = r'''# ==========================================================
#  Realstics — OneWide mode
#  Theme: Aqua + White
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

# Z threshold from which the sword becomes visible
zshowsword: 0.0

# PvP Zone (OneWide only)
# PvP is enabled only when the attacker's Z >= pvpzone.z
# Set with: /realstics onewide setpvpzone [z]
pvpzone:
  enabled: true
  z: 0.0
'''

RESOURCES["sb-onewide.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — OneWide mode
#  Theme: Aqua + White (no bold)
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&bOneWide'
  frames:
    - '&bO&fneWide'
    - '&bOn&feWide'
    - '&bOne&fWide'
    - '&bOneW&fide'
    - '&bOneWi&fde'
    - '&bOneWid&fe'
    - '&bOneWide'
    - '&fOneWide'
    - '&bOneWide'

lines:
  - '&b&m---------------------'
  - '&bOneWide &fPVP'
  - '&b&m---------------------'
  - '&b▪ &fPlayer &b» &f%player%'
  - '&b▪ &fWorld  &b» &f%world%'
  - '&b▪ &fPing   &b» &f%ping%ms'
  - '&b▪ &fY      &b» &f%y%'
  - '&b&m---------------------'
  - '&b▪ &fOnline &b» &f%online%&b/&f%max_online%'
  - '&b&m---------------------'
  - '&fPlay.&bMineStorm&f.iR'
  - '&b&m---------------------'

allow-toggle: true
toggle-permission: 'realstics.scoreboard'
'''

RESOURCES["blockfight.yml"] = r'''# ==========================================================
#  Realstics — BlockFight mode
#  Theme: Aqua + White
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

sety: -13.0
'''

RESOURCES["sb-blockfight.yml"] = r'''# ==========================================================
#  Realstics Scoreboard — BlockFight mode
#  Theme: Aqua + White (no bold)
# ==========================================================
enabled: true
update-interval: 10
join-delay: 5

title:
  animated: true
  static: '&bBlockFight'
  frames:
    - '&bB&flockFight'
    - '&bBl&fockFight'
    - '&bBlo&fckFight'
    - '&bBloc&fkFight'
    - '&bBlock&fFight'
    - '&bBlockF&fight'
    - '&bBlockFi&fght'
    - '&bBlockFig&fht'
    - '&bBlockFigh&ft'
    - '&bBlockFight'
    - '&fBlockFight'
    - '&bBlockFight'

lines:
  - '&b&m---------------------'
  - '&bBlockFight &fPVP'
  - '&b&m---------------------'
  - '&b▪ &fPlayer &b» &f%player%'
  - '&b▪ &fWorld  &b» &f%world%'
  - '&b▪ &fPing   &b» &f%ping%ms'
  - '&b▪ &fY      &b» &f%y%'
  - '&b&m---------------------'
  - '&b▪ &fOnline &b» &f%online%&b/&f%max_online%'
  - '&b&m---------------------'
  - '&fPlay.&bMineStorm&f.iR'
  - '&b&m---------------------'

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

| Mode | Status | PvP Zone | Description |
|------|--------|----------|-------------|
| **Platform** | Default | ✅ Yes | Leather + Iron armor, Wooden Sword (Sharp I) |
| **LowMid** | Must be set up | ❌ Always on | Wooden Sword only (Sharp I) |
| **OneWide** | Must be set up | ✅ Yes | Iron Sword, hidden in spawn zone |
| **BlockFight** | Must be set up | ❌ Always on | Diamond Sword (Sharp IV), Light Blue Wool (infinite), Shears |

## Features

- **Auto-loads worlds** — no need to edit bukkit.yml
- **`/realstics join <mode>`** — players teleport with one command
- PvP with no HP loss (knockback works)
- No fall damage, infinite food
- **BlockFight**: only light-blue wool breakable, no drop, infinite
- **PvP Zone** in Platform & OneWide — PvP only beyond Z threshold
- **Aqua + White theme** in all configs by default
- Auto-fills empty messages on plugin update

## Commands

```
/realstics join <mode>                  - Join a game mode
/realstics worlds                       - List loaded worlds
/realstics setworld [world] <mode>      - Assign world
/realstics <mode> setspawn
/realstics <mode> setvoid [y]
/realstics <mode> setpvpzone [z]        - Platform/OneWide only
/realstics <mode> kit [player]
/realstics <mode> sb [reload]
/realstics onewide setzshowsword <z>
/realstics reload
/realstics creator
/realstics help
```

## PvP Zone

In **Platform** and **OneWide**, PvP is only enabled once the player's Z >= the configured threshold:

```
/realstics platform setpvpzone -50
/realstics onewide setpvpzone 100
```

Before the threshold, hits are cancelled with the message: *"You must enter the PvP zone first!"*

Disable with:
```yaml
pvpzone:
  enabled: false
```

## Building

```bash
mvn clean package
```

Or push to GitHub — Actions builds automatically.

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
    print("Realstics plugin generator (FULLY COMPLETE + PvP Zone)")
    print("=" * 60)

    print("\n[1/5] Creating directories...")
    for d in DIRS:
        full = os.path.join(BASE, d)
        os.makedirs(full, exist_ok=True)
        print("  + " + d + "/")

    print("\n[2/5] Writing pom.xml, README.md...")
    write_file("pom.xml", POM_XML)
    write_file("README.md", README)

    print("\n[3/5] Writing GitHub Actions workflow...")
    write_file(".github/workflows/build.yml", GITHUB_WORKFLOW)

    print("\n[4/5] Writing Java sources...")
    for name, content in JAVA.items():
        write_file(os.path.join("src/main/java/org/realstics", name), content)

    print("\n[5/5] Writing resources...")
    write_file(os.path.join("src/main/resources", "plugin.yml"), PLUGIN_YML)
    for name, content in RESOURCES.items():
        write_file(os.path.join("src/main/resources", name), content)

    print("\n" + "=" * 60)
    print("Done! Realstics plugin generated.")
    print("")
    print("Theme: Aqua + White")
    print("PvP Zone: Platform & OneWide")
    print("BlockFight: light-blue wool only, infinite, no drop")
    print("")
    print("Build: mvn clean package")

if __name__ == "__main__":
    main()
