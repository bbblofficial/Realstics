package org.housing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
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
import org.bukkit.plugin.java.JavaPlugin;

public class Protection implements Listener {

    private final JavaPlugin plugin;
    private final GameModeManager gameModeManager;

    /** world:x:y:z -> place time */
    private final Map<String, Long> placedBlocks = new HashMap<String, Long>();

    private final String lockedWorld;
    private final long placedDecaySeconds;

    private final boolean freezeEnabled;
    private final int freezeForceSize;
    private final boolean freezeApplyToEveryone;
    private final List<String> freezeBypassPermissions;

    private static final String PERM_BYPASS = "housing.bypass";
    private static final String PERM_BREAK  = "housing.break";

    public Protection(JavaPlugin plugin, GameModeManager gameModeManager) {
        this.plugin = plugin;
        this.gameModeManager = gameModeManager;

        this.lockedWorld = plugin.getConfig()
                .getString("protection.locked-world", "world");
        this.placedDecaySeconds = plugin.getConfig()
                .getLong("protection.placed-decay-seconds", 5L);

        this.freezeEnabled = plugin.getConfig()
                .getBoolean("protection.block-freeze.enabled", true);
        this.freezeForceSize = plugin.getConfig()
                .getInt("protection.block-freeze.force-stack-size", -1);
        this.freezeApplyToEveryone = plugin.getConfig()
                .getBoolean("protection.block-freeze.apply-to-everyone", true);

        List<String> perms = plugin.getConfig()
                .getStringList("protection.block-freeze.bypass-permissions");
        this.freezeBypassPermissions = (perms != null) ? perms : new ArrayList<String>();
    }

    private Messages M() {
        if (plugin instanceof Housing) return ((Housing) plugin).getMessages();
        return null;
    }

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

    private boolean shouldFreeze(Player player) {
        if (!this.freezeEnabled) return false;
        if (this.freezeApplyToEveryone) return true;

        for (String perm : this.freezeBypassPermissions) {
            if (perm != null && !perm.isEmpty() && player.hasPermission(perm)) {
                return false;
            }
        }
        return true;
    }

    private boolean isInBuildMode(Player player) {
        if (player == null) return false;
        if (!(plugin instanceof Housing)) return false;
        return ((Housing) plugin).isInBuildMode(player.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    private boolean isBlueWool(Block block) {
        if (block == null) return false;
        if (block.getType() != Material.WOOL) return false;
        return block.getData() == 3;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final World world = event.getBlock().getWorld();

        if (isLockedWorld(world) && !hasBypass(player)) {
            event.setCancelled(true);
            Messages m = M();
            if (m != null) m.send(player, "protection.cannot-build");
            return;
        }

        if (shouldFreeze(player)) {
            final int slot = player.getInventory().getHeldItemSlot();
            final ItemStack hand = player.getItemInHand();

            if (hand != null && hand.getType() != Material.AIR) {
                final ItemStack restore;
                if (this.freezeForceSize > 0) {
                    restore = hand.clone();
                    int forced = Math.min(this.freezeForceSize, restore.getMaxStackSize());
                    restore.setAmount(forced);
                } else {
                    restore = hand.clone();
                    int targetAmount = hand.getAmount() + 1;
                    if (targetAmount > restore.getMaxStackSize()) {
                        targetAmount = restore.getMaxStackSize();
                    }
                    restore.setAmount(targetAmount);
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                    @Override
                    public void run() {
                        if (!player.isOnline()) return;
                        ItemStack current = player.getInventory().getItem(slot);

                        if (current == null
                                || current.getType() == Material.AIR
                                || current.getType() == restore.getType()) {
                            player.getInventory().setItem(slot, restore);
                            player.updateInventory();
                        }
                    }
                }, 1L);
            }
        }

        final Block placedBlock = event.getBlockPlaced();
        final Location loc = placedBlock.getLocation().clone();
        final Material type = placedBlock.getType();
        final String key = locKey(loc);

        this.placedBlocks.put(key, Long.valueOf(System.currentTimeMillis()));

        boolean buildMode = isInBuildMode(player);
        boolean blueWool = isBlueWool(placedBlock);

        if (buildMode && !blueWool) {
            return;
        }

        long delayTicks = this.placedDecaySeconds * 20L;
        if (delayTicks < 1L) delayTicks = 100L;

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                Block b = loc.getBlock();
                if (b.getType() == type) {
                    b.setType(Material.AIR);
                }
                placedBlocks.remove(key);
            }
        }, delayTicks);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final World world = block.getWorld();

        if (isLockedWorld(world) && !hasBypass(player)) {
            event.setCancelled(true);
            Messages m = M();
            if (m != null) m.send(player, "protection.cannot-break");
            return;
        }

        Location loc = block.getLocation().clone();
        String key = locKey(loc);

        if (this.placedBlocks.containsKey(key)) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            this.placedBlocks.remove(key);
            return;
        }

        if (hasBypass(player)) return;

        if (player.hasPermission(PERM_BREAK)) {
            event.setCancelled(true);
            block.setType(Material.AIR);
            return;
        }

        event.setCancelled(true);
        Messages m = M();
        if (m != null) m.send(player, "protection.cannot-break-natural");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrop(PlayerDropItemEvent event) {
        final Player player = event.getPlayer();

        event.setCancelled(true);
        Messages m = M();
        if (m != null) m.send(player, "protection.cannot-drop");

        Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                player.getInventory().clear();
                player.getInventory().setArmorContents(null);
                player.updateInventory();

                JavaPlugin pl = Protection.this.plugin;
                if (pl instanceof Housing) {
                    PlayerJoin pj = ((Housing) pl).getPlayerJoin();
                    if (pj != null) pj.giveKit(player);
                }
            }
        }, 1L);
    }

    public boolean isPlayerPlaced(Block block) {
        return this.placedBlocks.containsKey(locKey(block.getLocation()));
    }
}