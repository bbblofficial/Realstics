package org.realstics;

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

/**
 * Protection rules:
 *   - Cannot break map blocks   (realstics.break)
 *   - Cannot place blocks       (realstics.place)
 *   - Cannot drop items         (realstics.drop)
 *   - realstics.bypass bypasses all
 *
 * Special rules (BlockFight):
 *   - Only PLAYER-PLACED LIGHT BLUE wool (data=3) is breakable
 *   - All other wool colors are protected
 *   - Light blue wool is infinite (always stays at 64)
 *   - Broken light blue wool drops NOTHING (cleared next tick)
 */
public class Protection implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    /** Locations of light-blue wool blocks placed by players. */
    private final Set<String> playerPlacedWool = new HashSet<String>();

    /** Minecraft 1.8 wool color data value for LIGHT BLUE. */
    private static final byte LIGHT_BLUE_DATA = 3;

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
    //  BREAK
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (hasBypass(player)) return;
        if (player.hasPermission(PERM_BREAK)) return;

        // BlockFight: only player-placed LIGHT BLUE wool is breakable
        if (isBlockFight(block.getWorld()) && isLightBlueWool(block)) {

            String key = locKey(block);

            if (playerPlacedWool.contains(key)) {
                // Allow break
                playerPlacedWool.remove(key);

                // Clear drops next tick (1.8.8 doesn't support setDropItems)
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                block.setType(Material.AIR);
                            }
                        }, 0L);
                return;
            }

            // Map light blue wool → protect
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou can only break light blue wool placed by players!"));
            return;
        }

        // Any other block → protect
        event.setCancelled(true);
        player.sendMessage(colorize("&cYou can only break light blue wool!"));
    }

    // ============================================================
    //  PLACE
    // ============================================================
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (hasBypass(player)) return;

        Material type = event.getBlock().getType();

        if (type == Material.WOOL) {
            if (isBlockFight(world) && isLightBlueWool(event.getBlock())) {
                playerPlacedWool.add(locKey(event.getBlock()));

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
    //  WOOL REFILL
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

        inv.setItem(1, new ItemStack(Material.WOOL, 64, LIGHT_BLUE_DATA));
        player.updateInventory();
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}