package org.housing;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class Protection implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    /**
     * Player-placed blocks: Location key -> place time.
     * هر بلاکی که پلیر میذاره اینجا track میشه و
     * بعد از PLACED_DECAY_TICKS حذف میشه.
     */
    private final Map<String, Long> placedBlocks = new HashMap<String, Long>();

    /** Auto-remove player-placed blocks after 5 seconds (100 ticks). */
    private static final long PLACED_DECAY_TICKS = 100L;

    private static final String PERM_BYPASS = "housing.bypass";
    private static final String PERM_BREAK  = "housing.break";
    private static final String PERM_PLACE  = "housing.place";

    /**
     * The main world name that must NEVER be modified.
     * Loaded from config.yml: protection.locked-world
     */
    private final String lockedWorld;

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
        this.lockedWorld = plugin.getConfig().getString("protection.locked-world", "world");
        // NOTE: registration is done in Housing.java, so we don't register here
        // to avoid double registration.
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
    }

    private boolean isLockedWorld(World world) {
        if (world == null) return false;
        return world.getName().equalsIgnoreCase(this.lockedWorld);
    }

    private String locKey(Location loc) {
        return loc.getWorld().getName()
                + ":" + loc.getBlockX()
                + ":" + loc.getBlockY()
                + ":" + loc.getBlockZ();
    }

    // ============================================================
    //  Block place — track + auto-remove after 5s
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        // ---- Locked world: never allow any placement ----
        if (isLockedWorld(world)) {
            if (hasBypass(player)) return;
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou cannot build in this world!"));
            return;
        }

        if (hasBypass(player)) return;

        // ---- Track ALL placed blocks for auto-removal after 5s ----
        final Block placedBlock = event.getBlockPlaced();
        final Location loc = placedBlock.getLocation().clone();
        final Material type = placedBlock.getType();
        final byte data = placedBlock.getData();
        final String key = locKey(loc);

        placedBlocks.put(key, Long.valueOf(System.currentTimeMillis()));

        // Schedule removal after 5 seconds
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                Block b = loc.getBlock();
                if (b.getType() == type) {
                    // Remove WITHOUT drops
                    b.setType(Material.AIR);
                }
                placedBlocks.remove(key);
            }
        }, PLACED_DECAY_TICKS);
    }

    // ============================================================
    //  Block break — only player-placed blocks, NO DROPS
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final World world = block.getWorld();

        // ---- Locked world: never allow any break ----
        if (isLockedWorld(world)) {
            if (hasBypass(player)) return;
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou cannot break blocks in this world!"));
            return;
        }

        if (hasBypass(player)) return;

        // ---- Check if it's a player-placed block ----
        Location loc = block.getLocation().clone();
        String key = locKey(loc);

        if (this.placedBlocks.containsKey(key)) {
            // Cancel default break (prevents drops)
            event.setCancelled(true);
            // Remove block manually WITHOUT drops
            block.setType(Material.AIR);
            this.placedBlocks.remove(key);
            return;
        }

        // ---- If player has break permission, allow (but no drops for natural blocks) ----
        if (player.hasPermission(PERM_BREAK)) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            return;
        }

        // ---- Everything else: cancel ----
        event.setCancelled(true);
        player.sendMessage(colorize("&cYou can only break blocks placed by players!"));
    }

    // ============================================================
    //  Drop item — cancel for everyone + restore kit
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        event.setCancelled(true);

        player.sendMessage(colorize("&cYou cannot drop items! Your kit has been reset."));

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.updateInventory();

                JavaPlugin pl = Protection.this.plugin;
                if (pl instanceof Housing) {
                    Housing housing = (Housing) pl;
                    PlayerJoin pj = housing.getPlayerJoin();
                    if (pj != null) {
                        pj.giveKit(player);
                    }
                }
            }
        }, 1L);
    }

    // ============================================================
    //  Utility
    // ============================================================

    public boolean isPlayerPlaced(Block block) {
        return this.placedBlocks.containsKey(locKey(block.getLocation()));
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}