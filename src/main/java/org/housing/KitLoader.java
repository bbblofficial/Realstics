package org.housing;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * KitLoader — کیت را از فایل YAML می‌خواند و به بازیکن می‌دهد.
 * فرمت فایل: kit-<mode>.yml
 */
public class KitLoader {

    public static void applyKit(Player player, FileConfiguration kit) {
        if (player == null || !player.isOnline()) return;
        if (kit == null) return;

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // ---------- Armor ----------
        ConfigurationSection armor = kit.getConfigurationSection("armor");
        if (armor != null) {
            setArmor(player, armor.getConfigurationSection("helmet"),     3);
            setArmor(player, armor.getConfigurationSection("chestplate"), 2);
            setArmor(player, armor.getConfigurationSection("leggings"),   1);
            setArmor(player, armor.getConfigurationSection("boots"),      0);
        }

        // ---------- Items ----------
        ConfigurationSection items = kit.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection sec = items.getConfigurationSection(key);
                if (sec == null) continue;

                int slot = sec.getInt("slot", -1);
                if (slot < 0 || slot > 35) continue;

                ItemStack item = buildItem(sec);
                if (item != null) player.getInventory().setItem(slot, item);
            }
        }

        // ---------- Food ----------
        int foodLevel = kit.getInt("food.level", 20);
        double sat    = kit.getDouble("food.saturation", 20.0);
        player.setFoodLevel(foodLevel);
        player.setSaturation((float) sat);
        player.setExhaustion(0f);

        player.updateInventory();
    }

    // ============================================================
    //  Internals
    // ============================================================

    private static void setArmor(Player player, ConfigurationSection sec, int armorSlot) {
        if (sec == null) return;
        ItemStack item = buildItem(sec);
        if (item == null) return;
        switch (armorSlot) {
            case 3: player.getInventory().setHelmet(item);     break;
            case 2: player.getInventory().setChestplate(item); break;
            case 1: player.getInventory().setLeggings(item);   break;
            case 0: player.getInventory().setBoots(item);      break;
        }
    }

    @SuppressWarnings("deprecation")
    private static ItemStack buildItem(ConfigurationSection sec) {
        if (sec == null) return null;

        String matName = sec.getString("material");
        if (matName == null) return null;

        Material mat = Material.matchMaterial(matName.toUpperCase());
        if (mat == null) return null;

        int amount = sec.getInt("amount", 1);
        if (amount < 1) amount = 1;
        if (amount > 64) amount = 64;

        short data = (short) sec.getInt("data", 0);

        ItemStack item = new ItemStack(mat, amount, data);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // ---- Leather color ----
        if (sec.contains("color") && meta instanceof LeatherArmorMeta) {
            Color c = parseColor(sec.getString("color"));
            if (c != null) ((LeatherArmorMeta) meta).setColor(c);
        }

        // ---- Display name ----
        if (sec.contains("name")) {
            meta.setDisplayName(colorize(sec.getString("name")));
        }

        // ---- Lore ----
        List<String> lore = sec.getStringList("lore");
        if (lore != null && !lore.isEmpty()) {
            List<String> colored = new ArrayList<String>();
            for (String l : lore) colored.add(colorize(l));
            meta.setLore(colored);
        }

        // ---- Enchantments ----
        ConfigurationSection ench = sec.getConfigurationSection("enchantments");
        if (ench != null) {
            for (String eName : ench.getKeys(false)) {
                int level = ench.getInt(eName);
                Enchantment e = resolveEnchantment(eName);
                if (e != null && level > 0) {
                    meta.addEnchant(e, level, true);
                }
            }
        }

        // ---- Unbreakable ----
        if (sec.getBoolean("unbreakable", false)) {
            try { meta.spigot().setUnbreakable(true); }
            catch (Throwable ignored) {}
        }

        // ---- Hide flags ----
        List<String> flags = sec.getStringList("hide-flags");
        if (flags != null) {
            for (String f : flags) {
                try {
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(f.toUpperCase()));
                } catch (Throwable ignored) {}
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private static Enchantment resolveEnchantment(String name) {
        if (name == null) return null;
        String n = name.toUpperCase();

        Enchantment e = Enchantment.getByName(n);
        if (e != null) return e;

        switch (n) {
            case "SHARPNESS":
            case "DAMAGE_ALL":              return Enchantment.DAMAGE_ALL;
            case "PROTECTION":
            case "PROTECTION_ENVIRONMENTAL":return Enchantment.PROTECTION_ENVIRONMENTAL;
            case "UNBREAKING":
            case "DURABILITY":              return Enchantment.DURABILITY;
            case "KNOCKBACK":               return Enchantment.KNOCKBACK;
            case "FIRE_ASPECT":             return Enchantment.FIRE_ASPECT;
            case "LOOTING":
            case "LOOT_BONUS_MOBS":         return Enchantment.LOOT_BONUS_MOBS;
            case "FEATHER_FALLING":
            case "PROTECTION_FALL":         return Enchantment.PROTECTION_FALL;
            case "FIRE_PROTECTION":
            case "PROTECTION_FIRE":         return Enchantment.PROTECTION_FIRE;
            case "BLAST_PROTECTION":
            case "PROTECTION_EXPLOSIONS":   return Enchantment.PROTECTION_EXPLOSIONS;
            case "PROJECTILE_PROTECTION":
            case "PROTECTION_PROJECTILE":   return Enchantment.PROTECTION_PROJECTILE;
            case "THORNS":                  return Enchantment.THORNS;
            case "EFFICIENCY":
            case "DIG_SPEED":               return Enchantment.DIG_SPEED;
            case "POWER":
            case "ARROW_DAMAGE":            return Enchantment.ARROW_DAMAGE;
            case "PUNCH":
            case "ARROW_KNOCKBACK":         return Enchantment.ARROW_KNOCKBACK;
            case "FLAME":
            case "ARROW_FIRE":              return Enchantment.ARROW_FIRE;
            case "INFINITY":
            case "ARROW_INFINITE":          return Enchantment.ARROW_INFINITE;
        }
        return null;
    }

    private static Color parseColor(String str) {
        if (str == null) return null;
        try {
            if (str.startsWith("#")) {
                return Color.fromRGB(Integer.parseInt(str.substring(1), 16));
            }
            if (str.contains(",")) {
                String[] p = str.split(",");
                return Color.fromRGB(
                        Integer.parseInt(p[0].trim()),
                        Integer.parseInt(p[1].trim()),
                        Integer.parseInt(p[2].trim()));
            }
            return Color.fromRGB(Integer.parseInt(str));
        } catch (Throwable t) {
            return null;
        }
    }

    private static String colorize(String msg) {
        if (msg == null) return "";
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}