package org.realstics;

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

    private static final int LEATHER_COLOR = 16711680; // 0xFF0000
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
