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

    /** Auto-remove player-placed light-blue wool after 5 seconds (100 ticks). */
    private static final long WOOL_AUTO_REMOVE_TICKS = 100L;

    private static final String PERM_BYPASS = "housing.bypass";
    private static final String PERM_BREAK  = "housing.break";
    private static final String PERM_PLACE  = "housing.place";

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;
    }

    private boolean hasBypass(Player player) {
        return player.hasPermission(PERM_BYPASS);
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

    private boolean isBlockFight(World world) {
        return gameModeManager.getModeForWorld(world) == GameMode.BLOCKFIGHT;
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

        // Any light-blue wool that was placed by a player can be broken.
        if (isLightBlueWool(block)) {
            String key = locKey(block);
            if (playerPlacedWool.contains(key)) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                playerPlacedWool.remove(key);

                if (isBlockFight(block.getWorld())) {
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
            event.setCancelled(true);
            player.sendMessage(colorize("&cYou can only break light blue wool placed by players!"));
            return;
        }

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou can only break light blue wool!"));
    }

    // ============================================================
    //  Block place — auto-remove for EVERYONE (op, perm, normal)
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (hasBypass(player)) return;

        Material type = event.getBlock().getType();
        if (type == Material.WOOL) {

            // ---- ALL players: light-blue wool is auto-removed after 5s ----
            if (isLightBlueWool(event.getBlock())) {
                final Block placedBlock = event.getBlock();
                final String key = locKey(placedBlock);

                // Register the block
                playerPlacedWool.add(key);

                // In BlockFight, refill the player's wool stack
                if (isBlockFight(world)) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(
                            this.plugin, new Runnable() {
                                @Override
                                public void run() {
                                    refillWool(player);
                                }
                            }, 1L);
                }

                // Schedule auto-remove after 5 seconds
                scheduleWoolRemoval(placedBlock, key);
                return;
            }

            // Non-light-blue wool: block unless player has PERM_PLACE
            if (player.hasPermission(PERM_PLACE)) return;

            event.setCancelled(true);
            player.sendMessage(colorize("&cYou cannot place this type of wool here!"));
            return;
        }

        if (player.hasPermission(PERM_PLACE)) return;

        event.setCancelled(true);
        player.sendMessage(colorize("&cYou cannot place blocks here!"));
    }

    // ============================================================
    //  Auto-remove scheduling
    // ============================================================

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
    //  AntiDrop — cancel + reset kit for EVERYONE (op, perm, normal)
    // ============================================================

    /**
     * When a player tries to drop an item:
     *   1) Cancel the drop.
     *   2) Clear their inventory + armor.
     *   3) Give them a fresh kit for their current mode.
     *
     * This applies to ALL players — even ops and players with
     * housing.drop permission (that permission no longer exists).
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        // ---- Cancel the drop ----
        event.setCancelled(true);

        // ---- Notify ----
        player.sendMessage(colorize("&cYou cannot drop items! Your kit has been reset."));

        // ---- Reset the kit after a short delay ----
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;

                        // Clear inventory
                        player.getInventory().clear();
                        player.getInventory().setArmorContents(null);
                        player.updateInventory();

                        // Give fresh kit for current mode
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