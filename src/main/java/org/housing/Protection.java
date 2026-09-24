package org.housing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Protection implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    /**
     * Player-placed blocks: Location -> place time.
     * Every block placed by a player is tracked here and
     * auto-removed after PLACED_DECAY_TICKS.
     */
    private final Map<Location, Long> placedBlocks = new HashMap<Location, Long>();

    /** Auto-remove player-placed blocks after 5 seconds (100 ticks). */
    private static final long PLACED_DECAY_TICKS = 100L;

    private static final String PERM_BYPASS = "housing.bypass";
    private static final String PERM_BREAK  = "housing.break";
    private static final String PERM_PLACE  = "housing.place";

    /**
     * The main world name that must NEVER be modified.
     * Loaded from config.yml: protection.locked-world
     * Default: "world"
     */
    private final String lockedWorld;

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
        this.lockedWorld = plugin.getConfig().getString("protection.locked-world", "world");
        plugin.getServer().getPluginManager().registerEvents(this, (Plugin) plugin);
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
    }

    /**
     * True if the world is the main locked world
     * (where no block modification is allowed).
     */
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
        if (player.hasPermission(PERM_PLACE)) {
            // Still track for auto-removal
            trackAndSchedule(event.getBlock());
            return;
        }

        // ---- Any block placed by anyone (op or not) is auto-removed ----
        final Block placedBlock = event.getBlockPlaced();
        final Location loc = placedBlock.getLocation().clone();
        final Material type = placedBlock.getType();
        final byte data = placedBlock.getData();

        // Track it
        placedBlocks.put(loc, Long.valueOf(System.currentTimeMillis()));

        // Schedule removal after 5 seconds
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                Block b = loc.getBlock();
                if (b.getType() != Material.AIR && b.getType() == type) {
                    b.setType(Material.AIR);
                }
                placedBlocks.remove(loc);
            }
        }, PLACED_DECAY_TICKS);
    }

    private void trackAndSchedule(final Block block) {
        final Location loc = block.getLocation().clone();
        final Material type = block.getType();

        placedBlocks.put(loc, Long.valueOf(System.currentTimeMillis()));

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                Block b = loc.getBlock();
                if (b.getType() != Material.AIR && b.getType() == type) {
                    b.setType(Material.AIR);
                }
                placedBlocks.remove(loc);
            }
        }, PLACED_DECAY_TICKS);
    }

    // ============================================================
    //  Block break — only player-placed blocks in non-locked worlds
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
        if (player.hasPermission(PERM_BREAK)) return;

        // Only allow breaking player-placed blocks
        Location loc = block.getLocation().clone();
        if (this.placedBlocks.containsKey(loc)) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            this.placedBlocks.remove(loc);
            return;
        }

        // Everything else: cancel
        event.setCancelled(true);
        player.sendMessage(colorize("&cYou can only break blocks placed by players!"));
    }

    // ============================================================
    //  Drop item — cancel for everyone
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        // Cancel the drop
        event.setCancelled(true);

        player.sendMessage(colorize("&cYou cannot drop items! Your kit has been reset."));

        // Reset the kit after 1 tick
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
        return this.placedBlocks.containsKey(block.getLocation().clone());
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}