package org.housing;

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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerJoin implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private static final int LEATHER_COLOR = 16711680;   // Red
    private static final short LIGHT_BLUE_WOOL_DATA = 3;

    private static final String META_APPLIED = "housing_mode_applied";

    public PlayerJoin(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    // ============================================================
    //  Join — multiple checks for pending mode
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (player.hasMetadata(META_APPLIED)) {
            player.removeMetadata(META_APPLIED, this.plugin);
        }

        // Check at 40 ticks (2 seconds)
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                tryApplyPendingMode(player, "initial");
            }
        }, 40L);

        // Fallback at 100 ticks (5 seconds)
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                tryApplyPendingMode(player, "fallback");
            }
        }, 100L);

        // Final fallback at 160 ticks (8 seconds)
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                tryApplyFinalDefault(player);
            }
        }, 160L);
    }

    private void tryApplyPendingMode(Player player, String source) {
        if (player.hasMetadata(META_APPLIED)) return;

        String pendingMode = null;
        if (plugin instanceof Housing) {
            pendingMode = ((Housing) plugin).consumePendingMode(player.getUniqueId());
        }

        if (pendingMode == null) {
            plugin.getLogger().info("[PlayerJoin] No pending mode for "
                    + player.getName() + " at " + source);
            return;
        }

        player.setMetadata(META_APPLIED, new FixedMetadataValue(plugin, true));

        plugin.getLogger().info("[PlayerJoin] Applying pending mode '"
                + pendingMode + "' for " + player.getName() + " (via " + source + ")");

        player.performCommand("housing join " + pendingMode);
    }

    private void tryApplyFinalDefault(Player player) {
        if (player.hasMetadata(META_APPLIED)) return;

        String pendingMode = null;
        if (plugin instanceof Housing) {
            pendingMode = ((Housing) plugin).consumePendingMode(player.getUniqueId());
        }

        if (pendingMode != null) {
            player.setMetadata(META_APPLIED, new FixedMetadataValue(plugin, true));
            plugin.getLogger().info("[PlayerJoin] Late pending mode '"
                    + pendingMode + "' for " + player.getName());
            player.performCommand("housing join " + pendingMode);
            return;
        }

        plugin.getLogger().info("[PlayerJoin] Applying default (platform) for "
                + player.getName());
        giveKit(player);
        teleportToSpawn(player);
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
    //  Kit dispatch
    // ============================================================

    public void giveKit(Player player) {
        GameMode mode = gameModeManager.getModeForWorld(player.getWorld());
        giveKitForMode(player, mode);
    }

    public void giveKitForMode(Player player, GameMode mode) {
        if (player == null || !player.isOnline()) return;
        if (mode == null) mode = GameMode.PLATFORM;

        switch (mode) {
            case LOWMID:     giveLowMidKit(player);     break;
            case ONEWIDE:    giveOneWideKit(player);    break;
            case BLOCKFIGHT: giveBlockFightKit(player); break;
            case PLATFORM:
            default:         givePlatformKit(player);   break;
        }
    }

    // ============================================================
    //  PLATFORM KIT
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
    //  LOWMID KIT
    // ============================================================

    private void giveLowMidKit(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        player.getInventory().setHelmet(dyedLeather(Material.LEATHER_HELMET));
        player.getInventory().setChestplate(dyedLeather(Material.LEATHER_CHESTPLATE));
        player.getInventory().setLeggings(protectionIron(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(protectionIron(Material.IRON_BOOTS));

        ItemStack sword = new ItemStack(Material.WOOD_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
        player.getInventory().setItem(0, unbreakable(sword));

        ItemStack wool = new ItemStack(Material.WOOL, 64);
        wool.setDurability(LIGHT_BLUE_WOOL_DATA);
        player.getInventory().setItem(1, wool);

        player.getInventory().setItem(2, unbreakable(new ItemStack(Material.SHEARS)));

        refillFood(player);
        player.updateInventory();
    }

    // ============================================================
    //  ONEWIDE KIT
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
    //  BLOCKFIGHT KIT
    // ============================================================

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

    // ============================================================
    //  Helpers
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
    //  Spawn location
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