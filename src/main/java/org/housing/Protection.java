package org.housing;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Protection implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private final Set<String> playerPlacedWool = new HashSet<String>();

    private static final byte LIGHT_BLUE_DATA = 3;

    /** Auto-remove player-placed wool after this many ticks (5 seconds = 100 ticks) */
    private static final long WOOL_AUTO_REMOVE_TICKS = 100L;

    private static final String PERM_BYPASS = "housing.bypass";
    private static final String PERM_BREAK  = "housing.break";
    private static final String PERM_PLACE  = "housing.place";
    private static final String PERM_DROP   = "housing.drop";

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
    }

    // ============================================================
    //  Mode checks
    // ============================================================

    /**
     * BlockFight: full wool mechanics (refill + auto-remove after 5s).
     */
    private boolean isBlockFight(World world) {
        return gameModeManager.getModeForWorld(world) == GameMode.BLOCKFIGHT;
    }

    /**
     * LowMid: also auto-removes wool after 5s, but does NOT refill it.
     * This prevents players from bringing infinite wool from other sources.
     */
    private boolean isLowMid(World world) {
        return gameModeManager.getModeForWorld(world) == GameMode.LOWMID;
    }

    /**
     * True if the world uses wool auto-removal (BlockFight OR LowMid).
     */
    private boolean usesWoolAutoRemove(World world) {
        GameMode mode = gameModeManager.getModeForWorld(world);
        return mode == GameMode.BLOCKFIGHT || mode == GameMode.LOWMID;
    }

    // ============================================================
    //  Helpers
    // ============================================================

    private String locKey(Block block) {
        return block.getWorld().getName()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();
    }

    @SuppressWarnings("deprecation")
    private boolean isLightBlueWool(Block block) {
        if (block.getType() != Material.WOOL) return false;
        return block.getData() == LIGHT_BLUE_DATA;
    }

    @SuppressWarnings("deprecation")
    private boolean isLightBlueWoolItem(ItemStack item) {
        if (item == null) return false;
        if (item.getType() != Material.WOOL) return false;
        return item.getData().getData() == LIGHT_BLUE_DATA;
    }

    // ============================================================
    //  Block break
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_BREAK)) return;

        // ---- BlockFight: only player-placed light-blue wool ----
        if (isBlockFight(block.getWorld()) && isLightBlueWool(block)) {
            String key = locKey(block);
            if (playerPlacedWool.contains(key)) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                playerPlacedWool.remove(key);
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                refillWool(player);
                            }
                        }, 1L);
                return;
            }
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou can only break light blue wool placed by players!"));
            return;
        }

        // ---- LowMid: only player-placed light-blue wool, NO refill ----
        if (isLowMid(block.getWorld()) && isLightBlueWool(block)) {
            String key = locKey(block);
            if (playerPlacedWool.contains(key)) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                playerPlacedWool.remove(key);
                return;
            }
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou can only break light blue wool placed by players!"));
            return;
        }

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou can only break light blue wool!"));
    }

    // ============================================================
    //  Block place
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (hasBypass(player)) return;

        Material type = event.getBlock().getType();
        if (type == Material.WOOL) {

            // ---- BlockFight: register + refill + auto-remove ----
            if (isBlockFight(world) && isLightBlueWool(event.getBlock())) {
                final Block placedBlock = event.getBlock();
                final String key = locKey(placedBlock);
                playerPlacedWool.add(key);

                // Refill player's wool immediately
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                refillWool(player);
                            }
                        }, 1L);

                // Schedule auto-remove after 5s
                scheduleWoolRemoval(placedBlock, key);
                return;
            }

            // ---- LowMid: register + auto-remove (NO refill) ----
            if (isLowMid(world) && isLightBlueWool(event.getBlock())) {
                final Block placedBlock = event.getBlock();
                final String key = locKey(placedBlock);
                playerPlacedWool.add(key);

                // Schedule auto-remove after 5s
                scheduleWoolRemoval(placedBlock, key);
                return;
            }

            // Other worlds: allow wool placement normally (or block if no perm)
            return;
        }

        if (player.hasPermission(PERM_PLACE)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot place blocks here!"));
    }

    // ============================================================
    //  Auto-remove scheduling
    // ============================================================

    /**
     * Schedules a light-blue wool block to be removed after
     * WOOL_AUTO_REMOVE_TICKS (5 seconds). Uses captured coordinates
     * so the block reference remains valid even if the chunk unloads.
     */
    private void scheduleWoolRemoval(final Block placedBlock, final String key) {
        final World blockWorld = placedBlock.getWorld();
        final int bx = placedBlock.getX();
        final int by = placedBlock.getY();
        final int bz = placedBlock.getZ();

        Bukkit.getScheduler().scheduleSyncDelayedTask(
                this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        Block b = blockWorld.getBlockAt(bx, by, bz);
                        if (b.getType() == Material.WOOL
                                && b.getData() == LIGHT_BLUE_DATA) {
                            b.setType(Material.AIR);
                            playerPlacedWool.remove(key);
                        }
                    }
                }, WOOL_AUTO_REMOVE_TICKS);
    }

    // ============================================================
    //  Item drop
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_DROP)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot drop items here!"));
    }

    // ============================================================
    //  Refill wool (BlockFight only)
    // ============================================================

    private void refillWool(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isLightBlueWoolItem(item)) {
                if (item.getAmount() < 64) {
                    item.setAmount(64);
                    inv.setItem(i, item);
                    player.updateInventory();
                }
                return;
            }
        }

        ItemStack wool = new ItemStack(Material.WOOL, 64);
        wool.setDurability(LIGHT_BLUE_DATA);
        inv.setItem(1, wool);
        player.updateInventory();
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}