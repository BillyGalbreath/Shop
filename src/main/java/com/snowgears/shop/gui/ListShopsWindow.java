package com.snowgears.shop.gui;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.handler.ShopGuiHandler.GuiIcon;
import com.snowgears.shop.util.ShopTypeComparator;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ListShopsWindow extends ShopGuiWindow {
    private final UUID playerToList;

    public ListShopsWindow(UUID player, UUID playerToList) {
        super(player);
        String name;
        if (handler.getAdminUUID().equals(playerToList)) {
            ItemStack is = gui.getIcon(GuiIcon.LIST_PLAYER_ADMIN, null, null);
            name = is.getItemMeta().getDisplayName();
        } else {
            name = Bukkit.getOfflinePlayer(playerToList).getName();
        }
        page = Bukkit.createInventory(null, INV_SIZE, name);
        this.playerToList = playerToList;
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        clearInvBody();
        makeMenuBarUpper();
        makeMenuBarLower();
        List<AbstractShop> shops = handler.getShops(playerToList);
        shops.sort(new ShopTypeComparator());
        int startIndex = pageIndex * 36;
        boolean added = true;
        for (int i = startIndex; i < shops.size(); ++i) {
            AbstractShop shop = shops.get(i);
            ItemStack icon = gui.getIcon(GuiIcon.LIST_SHOP, null, shop);
            if (!addIcon(icon)) {
                added = false;
                break;
            }
        }
        if (added) {
            page.setItem(53, null);
        } else {
            page.setItem(53, getNextPageIcon());
        }
    }

    @Override
    protected void makeMenuBarUpper() {
        super.makeMenuBarUpper();
    }

    @Override
    protected void makeMenuBarLower() {
        super.makeMenuBarLower();
    }
}
