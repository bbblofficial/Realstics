package org.housing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ModeMenu implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;
    private final PlayerJoin playerJoin;

    private File menuFile;
    private FileConfiguration menuConfig;

    private static final String PERM_MENU = "housing.menu";

    public ModeMenu(JavaPlugin plugin, GameModeManager gameModeManager, PlayerJoin playerJoin) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
        this.playerJoin = playerJoin;
        loadConfig();
        startSafetyTask();
    }

    public void loadConfig() {
        this.menuFile = new File(plugin.getDataFolder(), "menu.yml");

        if (!this.menuFile.exists()) {
            try {
                plugin.saveResource("menu.yml", false);
            } catch (Throwable t) {
                plugin.getLogger().warning("Could not save menu.yml: " + t.getMessage());
            }
        }

        this.menuConfig = YamlConfiguration.loadConfiguration(this.menuFile);

        InputStream defStream = plugin.getResource("menu.yml");
        if (defStream != null) {
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.menuConfig.setDefaults(defaults);
            this.menuConfig.options().copyDefaults(true);

            for (String key : defaults.getKeys(true)) {
                if (!this.menuConfig.contains(key)
                        && !defaults.isConfigurationSection(key)) {
                    this.menuConfig.set(key, defaults.get(key));
                }
            }

            try {
                this.menuConfig.save(this.menuFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save menu.yml: " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public FileConfiguration getConfig() {
        return this.menuConfig;
    }

    private void startSafetyTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (!menuConfig.getBoolean("item.enabled", true)) return;

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.hasPermission(PERM_MENU)) continue;
                    if (player.isDead()) continue;

                    if (!hasMenuItem(player)) {
                        giveMenuItem(player);
                    }
                }
            }
        }, 60L, 60L);
    }

    private boolean hasMenuItem(Player player) {
        int slot = this.menuConfig.getInt("item.slot", 8);
        if (slot < 0 || slot > 8) slot = 8;

        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.getType() == Material.AIR) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return false;

        String expected = colorize(this.menuConfig.getString("item.name", "&b&lMode Selector"));
        return meta.getDisplayName().equals(expected);
    }

    public void giveMenuItem(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!this.menuConfig.getBoolean("item.enabled", true)) return;

        int slot = this.menuConfig.getInt("item.slot", 8);
        if (slot < 0 || slot > 8) slot = 8;

        Material mat = Material.matchMaterial(
                this.menuConfig.getString("item.material", "COMPASS"));
        if (mat == null) mat = Material.COMPASS;

        int data = this.menuConfig.getInt("item.data", 0);

        ItemStack item = new ItemStack(mat, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = this.menuConfig.getString("item.name", "&b&lMode Selector");
            meta.setDisplayName(colorize(name));

            List<String> lore = this.menuConfig.getStringList("item.lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<String>();
                for (String line : lore) {
                    colored.add(colorize(line.replace("%player%", player.getName())));
                }
                meta.setLore(colored);
            }

            if (this.menuConfig.getBoolean("item.glow", true)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
            }

            if (this.menuConfig.getBoolean("item.hide-attributes", true)) {
                try {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                } catch (Throwable ignored) {}
            }

            item.setItemMeta(meta);
        }

        player.getInventory().setItem(slot, item);
        player.updateInventory();
    }

    public void openMenu(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!player.hasPermission(PERM_MENU)) {
            if (plugin instanceof Housing) {
                Messages m = ((Housing) plugin).getMessages();
                if (m != null) m.send(player, "menu.no-permission");
            }
            return;
        }

        String title = colorize(this.menuConfig.getString("gui.title", "&8&lSelect a Game Mode"));
        int size = this.menuConfig.getInt("gui.size", 27);
        if (size < 9) size = 9;
        if (size > 54) size = 54;
        if (size % 9 != 0) size = 27;

        Inventory inv = Bukkit.createInventory(null, size, title);

        ItemStack filler = buildFiller();
        if (filler != null) {
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        if (this.menuConfig.isConfigurationSection("buttons")) {
            for (String modeId : this.menuConfig.getConfigurationSection("buttons").getKeys(false)) {
                if (GameMode.fromId(modeId) == null) continue;

                String path = "buttons." + modeId;
                int slot = this.menuConfig.getInt(path + ".slot", -1);
                if (slot < 0 || slot >= size) continue;

                ItemStack button = buildButton(player, path);
                if (button != null) inv.setItem(slot, button);
            }
        }

        if (this.menuConfig.getBoolean("close-button.enabled", true)) {
            int slot = this.menuConfig.getInt("close-button.slot", size - 5);
            if (slot >= 0 && slot < size) {
                ItemStack close = buildCloseButton();
                if (close != null) inv.setItem(slot, close);
            }
        }

        player.openInventory(inv);

        if (this.menuConfig.getBoolean("gui.sound-open", true)) {
            playSound(player, "gui.sound-open-name", "CLICK",
                    "gui.sound-open-volume", 1.0F,
                    "gui.sound-open-pitch", 1.5F);
        }
    }

    private ItemStack buildFiller() {
        String matName = this.menuConfig.getString("gui.filler-material", "STAINED_GLASS_PANE");
        if (matName == null || matName.equalsIgnoreCase("AIR")) return null;

        Material mat = Material.matchMaterial(matName);
        if (mat == null) return null;

        int data = this.menuConfig.getInt("gui.filler-data", 7);
        ItemStack item = new ItemStack(mat, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = this.menuConfig.getString("gui.filler-name", " ");
            meta.setDisplayName(colorize(name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildButton(Player viewer, String path) {
        String matName = this.menuConfig.getString(path + ".material", "STONE");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) return null;

        int data = this.menuConfig.getInt(path + ".data", 0);

        ItemStack item = new ItemStack(mat, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = this.menuConfig.getString(path + ".name", "&bMode");
            meta.setDisplayName(colorize(name.replace("%player%", viewer.getName())));

            List<String> lore = this.menuConfig.getStringList(path + ".lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<String>();
                for (String line : lore) {
                    colored.add(colorize(line.replace("%player%", viewer.getName())));
                }
                meta.setLore(colored);
            }

            if (this.menuConfig.getBoolean(path + ".glow", false)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
            }

            try {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            } catch (Throwable ignored) {}

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildCloseButton() {
        String matName = this.menuConfig.getString("close-button.material", "BARRIER");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.BARRIER;

        int data = this.menuConfig.getInt("close-button.data", 0);

        ItemStack item = new ItemStack(mat, 1, (short) data);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = this.menuConfig.getString("close-button.name", "&c&lClose");
            meta.setDisplayName(colorize(name));

            List<String> lore = this.menuConfig.getStringList("close-button.lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> colored = new ArrayList<String>();
                for (String line : lore) colored.add(colorize(line));
                meta.setLore(colored);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();
        if (item == null) return;

        int slot = this.menuConfig.getInt("item.slot", 8);
        if (slot < 0 || slot > 8) slot = 8;

        if (player.getInventory().getHeldItemSlot() != slot) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String expected = colorize(this.menuConfig.getString("item.name", "&b&lMode Selector"));
        if (!meta.getDisplayName().equals(expected)) return;

        event.setCancelled(true);
        openMenu(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getInventory() == null) return;

        String title = event.getInventory().getTitle();
        if (title == null) return;

        String expected = colorize(this.menuConfig.getString("gui.title", "&8&lSelect a Game Mode"));
        if (!title.equals(expected)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0) return;

        if (this.menuConfig.getBoolean("close-button.enabled", true)) {
            int closeSlot = this.menuConfig.getInt("close-button.slot", -1);
            if (slot == closeSlot) {
                player.closeInventory();
                return;
            }
        }

        if (this.menuConfig.isConfigurationSection("buttons")) {
            for (String modeId : this.menuConfig.getConfigurationSection("buttons").getKeys(false)) {
                int btnSlot = this.menuConfig.getInt("buttons." + modeId + ".slot", -1);
                if (slot != btnSlot) continue;
                if (GameMode.fromId(modeId) == null) continue;

                if (this.menuConfig.getBoolean("gui.sound-click", true)) {
                    playSound(player, "gui.sound-click-name", "LEVEL_UP",
                            "gui.sound-click-volume", 1.0F,
                            "gui.sound-click-pitch", 1.2F);
                }

                player.closeInventory();
                player.performCommand("housing join " + modeId);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemDrop().getItemStack();
        if (item == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String expected = colorize(this.menuConfig.getString("item.name", "&b&lMode Selector"));
        if (meta.getDisplayName().equals(expected)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!this.menuConfig.getBoolean("item.enabled", true)) return;

        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (!hasMenuItem(player)) {
                        giveMenuItem(player);
                    }
                }
            }
        }, 50L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!this.menuConfig.getBoolean("item.enabled", true)) return;

        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline() && !hasMenuItem(player)) {
                    giveMenuItem(player);
                }
            }
        }, 10L);
    }

    @SuppressWarnings("deprecation")
    private void playSound(Player player, String namePath, String defName,
                           String volPath, float defVol,
                           String pitchPath, float defPitch) {
        String soundName = this.menuConfig.getString(namePath, defName);
        float volume = (float) this.menuConfig.getDouble(volPath, defVol);
        float pitch = (float) this.menuConfig.getDouble(pitchPath, defPitch);

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Throwable ignored) {}
    }

    private String colorize(String message) {
        if (message == null) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}