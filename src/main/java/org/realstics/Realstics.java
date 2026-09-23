package org.realstics;

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
        getServer().getPluginManager().registerEvents(new NoDamage(this), this);
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

        // ---- spawn ----
        setIfMissing(cfg, "spawn.world", "world");
        setIfMissing(cfg, "spawn.x", Double.valueOf(0.5D));
        setIfMissing(cfg, "spawn.y", Double.valueOf(100.0D));
        setIfMissing(cfg, "spawn.z", Double.valueOf(0.5D));
        setIfMissing(cfg, "spawn.yaw", Float.valueOf(0.0F));
        setIfMissing(cfg, "spawn.pitch", Float.valueOf(0.0F));

        // ---- void ----
        setIfMissing(cfg, "void.kill-height", Double.valueOf(-13.0D));

        // ---- messages (Aqua + White) ----
        setIfMissingOrEmpty(cfg, "join-message",
                "&b%player% &fjoined the game &7(&b%online%&7/&b%max_online%&7)");
        setIfMissingOrEmpty(cfg, "quit-message",
                "&b%player% &fleft the game &7(&b%online%&7/&b%max_online%&7)");

        // ---- combo ----
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

        // ---- scoreboard ----
        setIfMissing(cfg, "scoreboard.update-interval", Integer.valueOf(10));

        // ---- worlds ----
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

    /**
     * Set value only if the key is missing OR the value is empty.
     * Useful for auto-fixing empty messages after upgrades.
     */
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
