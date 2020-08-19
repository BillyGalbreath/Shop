package com.snowgears.shop.gui;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryType;

import java.util.UUID;

public class SearchWindow extends ShopGuiWindow {
    public SearchWindow(final UUID player) {
        super(player);
        page = Bukkit.createInventory(null, InventoryType.ANVIL, "Search");
        initInvContents();
    }

    @Override
    protected void initInvContents() {
    }

    @Override
    protected void makeMenuBarUpper() {
    }

    @Override
    protected void makeMenuBarLower() {
    }
}
