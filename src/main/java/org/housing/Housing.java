package org.housing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    /**
     * Pending mode requests from HousingBackendBridge (Velocity).
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

        HousingCommand cmd = new HousingCommand(this,
                this.playerJoin, this.scoreboardManager, this.voidSystem,
                this.gameModeManager, this.worldLoader);
        getCommand("housing").setExecutor(cmd);
        getCommand("housing").setTabCompleter(cmd);

        getLogger().info("=================================================");
        getLogger().info("  Housing v1.0 - Enabled");
        getLogger().info("  Modes: Platform, LowMid, OneWide, BlockFight");
        getLogger().info("  Loaded worlds: " + worldLoader.listLoadedWorldNames());
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
    //  Pending mode API (used by HousingBackendBridge)
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

        setIfMissing(cfg, "pvpzone.enabled", Boolean.valueOf(true));
        setIfMissing(cfg, "pvpzone.x", Double.valueOf(0.0D));

        if (cfg.contains("pvpzone.z")) {
            if (!cfg.contains("pvpzone.x")) {
                double oldZ = cfg.getDouble("pvpzone.z", 0.0);
                cfg.set("pvpzone.x", Double.valueOf(oldZ));
            }
            cfg.set("pvpzone.z", null);
        }

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