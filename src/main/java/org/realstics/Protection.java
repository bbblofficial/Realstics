package org.realstics;

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

/**
 * Protection rules:
 *   - Cannot break map blocks   (realstics.break)
 *   - Cannot place blocks       (realstics.place)
 *   - Cannot drop items         (realstics.drop)
 *   - realstics.bypass bypasses all
 *
 * Special rules:
 *   - BlockFight: wool can be placed AND broken; wool is infinite (always stays at 64)
 *   - All other modes: wool can be placed but NOT broken
 */
public class Protection implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    private static final String PERM_BYPASS = "realstics.bypass";
    private static final String PERM_BREAK  = "realstics.break";
    private static final String PERM_PLACE  = "realstics.place";
    private static final String PERM_DROP   = "realstics.drop";

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
    }

    private boolean isBlockFight(World world) {
        return gameModeManager.getModeForWorld(world) == GameMode.BLOCKFIGHT;
    }

    // ============================================================
    //  BREAK
    //  - Bypass / realstics.break     → allow
    //  - BlockFight + block is WOOL   → allow
    //  - Everything else              → cancel
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_BREAK)) return;

        // BlockFight exception: wool is breakable
        if (isBlockFight(block.getWorld())
                && block.getType() == Material.WOOL) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot break blocks here!"));
    }

    // ============================================================
    //  PLACE
    //  - Bypass / realstics.place → allow
    //  - WOOL (any mode)          → allow
    //  - Everything else          → cancel
    //
    //  BlockFight: refill wool to 64 after placing
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (hasBypass(player)) return;

        Material type = event.getBlock().getType();

        // Wool is always placeable
        if (type == Material.WOOL) {
            // BlockFight: keep wool at 64 forever
            if (isBlockFight(world)) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                refillWool(player);
                            }
                        }, 1L);
            }
            return;
        }

        if (player.hasPermission(PERM_PLACE)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot place blocks here!"));
    }

    // ============================================================
    //  DROP
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
    //  WOOL REFILL (BlockFight only)
    //  Finds wool in the player's inventory and tops it up to 64.
    //  If no wool exists, adds a fresh stack to slot 2.
    // ============================================================
    private void refillWool(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory inv = player.getInventory();

        // Look for an existing wool stack
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() == Material.WOOL) {
                if (item.getAmount() < 64) {
                    item.setAmount(64);
                    inv.setItem(i, item);
                    player.updateInventory();
                }
                return;
            }
        }

        // No wool found — give a fresh stack in the next free slot
        // (or slot 1 which is the traditional wool slot)
        inv.setItem(1, new ItemStack(Material.WOOL, 64));
        player.updateInventory();
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}