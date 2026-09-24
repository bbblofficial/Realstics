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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class Protection implements Listener {

    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    /** Keys of player-placed light-blue wool blocks (world:x:y:z) */
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

    private boolean isBlockFight(World world) {
        return gameModeManager.getModeForWorld(world) == GameMode.BLOCKFIGHT;
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

        if (isLightBlueWool(block)) {
            String key = locKey(block);
            if (playerPlacedWool.contains(key)) {
                event.setCancelled(true);
                block.setType(Material.AIR);
                playerPlacedWool.remove(key);

                // In BlockFight, refill wool
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
    //  Block place — auto-remove for EVERYONE
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (hasBypass(player)) return;

        Material type = event.getBlock().getType();
        if (type == Material.WOOL) {

            // ---- Any mode: light-blue wool is auto-removed after 5s ----
            if (isLightBlueWool(event.getBlock())) {
                final Block placedBlock = event.getBlock();
                final String key = locKey(placedBlock);

                // Register the block
                playerPlacedWool.add(key);

                // Refill the player's wool stack (infinite wool)
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        this.plugin, new Runnable() {
                            @Override
                            public void run() {
                                refillWool(player);
                            }
                        }, 1L);

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
    //  Auto-remove scheduling — uses BUKKIT scheduler + safety net
    // ============================================================

    private void scheduleWoolRemoval(final Block placedBlock, final String key) {
        final World blockWorld = placedBlock.getWorld();
        final int bx = placedBlock.getX();
        final int by = placedBlock.getY();
        final int bz = placedBlock.getZ();

        // Primary: remove after exactly 5 seconds (100 ticks)
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        Block b = blockWorld.getBlockAt(bx, by, bz);
                        if (b.getType() == Material.WOOL
                                && b.getData() == LIGHT_BLUE_DATA) {
                            b.setType(Material.AIR);
                        }
                        playerPlacedWool.remove(key);
                    }
                }, WOOL_AUTO_REMOVE_TICKS);

        // Safety net: check again at 6 seconds (120 ticks) in case
        // the block was placed again or something interfered
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        Block b = blockWorld.getBlockAt(bx, by, bz);
                        if (b.getType() == Material.WOOL
                                && b.getData() == LIGHT_BLUE_DATA) {
                            b.setType(Material.AIR);
                        }
                        playerPlacedWool.remove(key);
                    }
                }, WOOL_AUTO_REMOVE_TICKS + 20L);
    }

    // ============================================================
    //  AntiDrop — cancel + reset kit for EVERYONE
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        // Cancel the drop
        event.setCancelled(true);

        player.sendMessage(colorize("&cYou cannot drop items! Your kit has been reset."));

        // Reset the kit after 1 tick
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                this.plugin, new Runnable() {
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
    //  Refill wool — infinite wool for the player
    // ============================================================

    /**
     * Refills the player's light-blue wool stack back to 64.
     * If the player doesn't have any wool, put 64 in slot 1.
     */
    private void refillWool(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerInventory inv = player.getInventory();

        // Look for existing light-blue wool
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

        // No wool found → give 64 in slot 1
        ItemStack wool = new ItemStack(Material.WOOL, 64);
        wool.setDurability(LIGHT_BLUE_DATA);
        inv.setItem(1, wool);
        player.updateInventory();
    }

    // ============================================================
    //  Prevent wool consumption in hand (infinite wool)
    // ============================================================

    /**
     * Fires every time the player's held item changes or they
     * interact. If they're holding light-blue wool and it's
     * not full, refill it.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    refillWoolIfNeeded(player);
                }
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    refillWoolIfNeeded(player);
                }
            }
        }, 1L);
    }

    /**
     * Called every tick-ish to make sure the wool stack never decreases.
     * Only refills light-blue wool.
     */
    private void refillWoolIfNeeded(Player player) {
        if (!player.isOnline()) return;

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (isLightBlueWoolItem(item)) {
                if (item.getAmount() < 64) {
                    item.setAmount(64);
                    inv.setItem(i, item);
                    player.updateInventory();
                }
            }
        }
    }

    private String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}