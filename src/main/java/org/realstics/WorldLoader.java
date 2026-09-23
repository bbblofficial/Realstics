package org.realstics;

import java.io.File;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Auto-loads a world if the folder exists but Bukkit hasn't loaded it yet,
 * or creates a new one if the folder doesn't exist.
 *
 * Solves: "World not found: lowmid" when folder exists but server didn't load it.
 */
public class WorldLoader {

    private final JavaPlugin plugin;

    public WorldLoader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Ensure a world with the given name is loaded. Returns it, or null on failure.
     * - If already loaded → return it
     * - If folder + level.dat exist → load it
     * - Otherwise → create a new world
     */
    public World ensureLoaded(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) return null;

        // 1. Already loaded?
        World existing = findLoaded(worldName);
        if (existing != null) return existing;

        // 2. Does the folder exist with a valid level.dat?
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

    /**
     * Case-insensitive lookup of a loaded world.
     */
    public World findLoaded(String worldName) {
        if (worldName == null) return null;
        String target = worldName.trim().toLowerCase();
        for (World w : Bukkit.getWorlds()) {
            if (w.getName().toLowerCase().equals(target)) return w;
        }
        return null;
    }

    /**
     * Comma-separated list of loaded world names.
     */
    public String listLoadedWorldNames() {
        StringBuilder sb = new StringBuilder();
        for (World w : Bukkit.getWorlds()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(w.getName());
        }
        return sb.length() == 0 ? "(none)" : sb.toString();
    }
}
