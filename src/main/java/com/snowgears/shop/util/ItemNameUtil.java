package com.snowgears.shop.util;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemNameUtil {
    public String getName(ItemStack item) {
        if (item == null) {
            return "";
        }
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            if (meta.hasLocalizedName()) {
                return meta.getLocalizedName();
            }
        }
        try {
            // try Paper's API
            return Bukkit.getItemFactory().getI18NDisplayName(item);
        } catch (Exception e) {
            return getBackupName(item.getType());
        }
    }

    public String getName(Material material) {
        try {
            // try Paper's API
            return Bukkit.getItemFactory().getI18NDisplayName(new ItemStack(material));
        } catch (Exception e) {
            return getBackupName(material);
        }
    }

    private String getBackupName(Material material) {
        return WordUtils.capitalizeFully(material.name().replace("_", " ").toLowerCase());
    }
}
