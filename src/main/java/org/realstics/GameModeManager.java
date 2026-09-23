package org.realstics;

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
