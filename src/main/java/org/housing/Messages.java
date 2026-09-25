package org.housing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Messages — central message manager.
 * Reads every message from messages.yml.
 */
public class Messages {

    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration cfg;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        this.file = new File(plugin.getDataFolder(), "messages.yml");

        if (!this.file.exists()) {
            try {
                plugin.saveResource("messages.yml", false);
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not save messages.yml: " + t.getMessage());
            }
        }

        this.cfg = YamlConfiguration.loadConfiguration(this.file);

        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));

            for (String key : defaults.getKeys(true)) {
                if (defaults.isConfigurationSection(key)) continue;
                if (!this.cfg.contains(key)) {
                    this.cfg.set(key, defaults.get(key));
                }
            }

            try { this.cfg.save(this.file); }
            catch (IOException e) {
                plugin.getLogger().warning("Could not save messages.yml: " + e.getMessage());
            }
        }
    }

    public void reload() {
        load();
    }

    public FileConfiguration getConfig() {
        return this.cfg;
    }

    // ============================================================
    //  Core API
    // ============================================================

    /** Get raw colored message (no placeholders). */
    public String get(String path) {
        String raw = this.cfg.getString(path, "");
        if (raw == null) return "";
        // Convert literal "\n" sequences into real newlines
        raw = raw.replace("\\n", "\n");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Get message with placeholders replaced.
     * Usage: msg("kit.restored-self", "mode", "LowMid")
     */
    public String msg(String path, String... placeholders) {
        String out = get(path);
        if (placeholders != null) {
            for (int i = 0; i + 1 < placeholders.length; i += 2) {
                String key = placeholders[i];
                String val = placeholders[i + 1];
                if (key == null) continue;
                out = out.replace("%" + key + "%", val == null ? "" : val);
            }
        }
        return out;
    }

    /** Convenience: send a message to a CommandSender. */
    public void send(CommandSender to, String path, String... placeholders) {
        if (to == null) return;
        to.sendMessage(msg(path, placeholders));
    }

    /** Convenience: send only if not empty. */
    public void sendIfNotEmpty(CommandSender to, String path, String... placeholders) {
        String m = msg(path, placeholders);
        if (m != null && !m.isEmpty()) to.sendMessage(m);
    }
}