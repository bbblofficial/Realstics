package org.housing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Housing extends JavaPlugin {

    private PlayerJoin playerJoin;
    private ScoreboardManager scoreboardManager;
    private Void voidSystem;
    private ComboSystem comboSystem;
    private GameModeManager gameModeManager;
    private WorldLoader worldLoader;
    private ModeMenu modeMenu;

    /**
     * Pending mode requests from external sources.
     * Key = player UUID, Value = mode ID (e.g. "onewide").
     */
    private final Map<UUID, String> pendingModes = new HashMap<UUID, String>();

    @Override
    public void onEnable() {

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        createConfigIfMissing();
        saveDefaultConfig();
        reloadConfig();

        // Save default menu.yml if missing
        if (!new File(getDataFolder(), "menu.yml").exists()) {
            saveResource("menu.yml", false);
        }

        // Save default kit files if missing
        String[] kitFiles = {
                "kit-platform.yml",
                "kit-lowmid.yml",
                "kit-onewide.yml",
                "kit-blockfight.yml"
        };
        for (String kit : kitFiles) {
            if (!new File(getDataFolder(), kit).exists()) {
                try { saveResource(kit, false); }
                catch (Throwable t) {
                    getLogger().warning("Could not save " + kit + ": " + t.getMessage());
                }
            }
        }

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

        getServer().getPluginManager().registerEvents(
                new KitRestore(this, this.playerJoin), this);

        getServer().getPluginManager().registerEvents(new Welcome(this), this);

        // ★ Custom death messages
        getServer().getPluginManager().registerEvents(
                new DeathListener(this, this.gameModeManager), this);

        this.voidSystem = new Void(this, this.playerJoin, this.gameModeManager);
        getServer().getPluginManager().registerEvents(this.voidSystem, this);

        this.comboSystem = new ComboSystem(this);
        getServer().getPluginManager().registerEvents(this.comboSystem, this);

        this.scoreboardManager = new ScoreboardManager(this, this.gameModeManager);

        // ---- Mode Menu (item + GUI) ----
        this.modeMenu = new ModeMenu(this, this.gameModeManager, this.playerJoin);
        getServer().getPluginManager().registerEvents(this.modeMenu, this);

        HousingCommand cmd = new HousingCommand(this,
                this.playerJoin, this.scoreboardManager, this.voidSystem,
                this.gameModeManager, this.worldLoader);
        getCommand("housing").setExecutor(cmd);
        getCommand("housing").setTabCompleter(cmd);

        getLogger().info("=================================================");
        getLogger().info("  Housing v1.0 - Enabled");
        getLogger().info("  Modes: Platform, LowMid, OneWide, BlockFight");
        getLogger().info("  Loaded worlds: " + worldLoader.listLoadedWorldNames());
        getLogger().info("  Menu: " + (modeMenu.getConfig().getBoolean("item.enabled", true) ? "ENABLED" : "DISABLED"));
        getLogger().info("=================================================");
    }

    @Override
    public void onDisable() {
        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }
        getLogger().info("Housing disabled.");
    }

    // ============================================================
    //  Pending mode API
    // ============================================================

    public void setPendingMode(UUID uuid, String mode) {
        if (uuid == null || mode == null) return;
        this.pendingModes.put(uuid, mode.toLowerCase());
        getLogger().info("[Housing] Pending mode set: " + mode + " for " + uuid);
    }

    public String consumePendingMode(UUID uuid) {
        if (uuid == null) return null;
        String mode = this.pendingModes.remove(uuid);
        if (mode != null) {
            getLogger().info("[Housing] Pending mode consumed: " + mode + " for " + uuid);
        }
        return mode;
    }

    public boolean hasPendingMode(UUID uuid) {
        return uuid != null && this.pendingModes.containsKey(uuid);
    }

    // ============================================================
    //  Config
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

        // ---- Spawn ----
        setIfMissing(cfg, "spawn.world", "world");
        setIfMissing(cfg, "spawn.x", Double.valueOf(0.5D));
        setIfMissing(cfg, "spawn.y", Double.valueOf(100.0D));
        setIfMissing(cfg, "spawn.z", Double.valueOf(0.5D));
        setIfMissing(cfg, "spawn.yaw", Float.valueOf(0.0F));
        setIfMissing(cfg, "spawn.pitch", Float.valueOf(0.0F));

        // ---- Void ----
        setIfMissing(cfg, "void.kill-height", Double.valueOf(-13.0D));

        // ---- Messages ----
        setIfMissingOrEmpty(cfg, "join-message",
                "&b%player% &fjoined the game &7(&b%online%&7/&b%max_online%&7)");
        setIfMissingOrEmpty(cfg, "quit-message",
                "&b%player% &fleft the game &7(&b%online%&7/&b%max_online%&7)");

        // ---- Combo ----
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

        // ---- Scoreboard ----
        setIfMissing(cfg, "scoreboard.update-interval", Integer.valueOf(10));

        // ---- Death Messages ----
        setIfMissing(cfg, "death.enabled", Boolean.valueOf(true));
        setIfMissingOrEmpty(cfg, "death.default", "&c%victim% &7died");
        setIfMissingOrEmpty(cfg, "death.killed-by-player",
                "&c%victim% &7was slain by &c%killer%");
        setIfMissingOrEmpty(cfg, "death.killed-by-mob",
                "&c%victim% &7was killed by &c%killer%");
        setIfMissing(cfg, "death.respawn-in-same-world", Boolean.valueOf(true));
        setIfMissing(cfg, "death.real-damage-modes", Arrays.asList("lowmid"));

        // ---- PvP zone ----
        setIfMissing(cfg, "pvpzone.enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "pvpzone.x", Double.valueOf(0.0D));

        if (cfg.contains("pvpzone.z")) {
            if (!cfg.contains("pvpzone.x")) {
                double oldZ = cfg.getDouble("pvpzone.z", 0.0);
                cfg.set("pvpzone.x", Double.valueOf(oldZ));
            }
            cfg.set("pvpzone.z", null);
        }

        // ---- World mapping ----
        setIfMissing(cfg, "worlds.world", "platform");

        // ---- Protection ----
        setIfMissing(cfg, "protection.locked-world", "world");
        setIfMissing(cfg, "protection.placed-decay-seconds", Integer.valueOf(5));
        setIfMissing(cfg, "protection.natural-restore-seconds", Integer.valueOf(9));

        // ---- Block Freeze ----
        setIfMissing(cfg, "protection.block-freeze.enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "protection.block-freeze.force-stack-size", Integer.valueOf(-1));
        setIfMissing(cfg, "protection.block-freeze.apply-to-everyone", Boolean.valueOf(true));
        setIfMissing(cfg, "protection.block-freeze.bypass-permissions",
                Arrays.asList("housing.bypass.freeze"));

        // ---- Menu ----
        setIfMissing(cfg, "menu.enabled", Boolean.valueOf(true));

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

    // ============================================================
    //  Getters
    // ============================================================

    public PlayerJoin getPlayerJoin()               { return playerJoin; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public Void getVoidSystem()                     { return voidSystem; }
    public ComboSystem getComboSystem()             { return comboSystem; }
    public GameModeManager getGameModeManager()     { return gameModeManager; }
    public WorldLoader getWorldLoader()             { return worldLoader; }
    public ModeMenu getModeMenu()                   { return modeMenu; }
}