package com.snowgears.shop.gui;

import org.bukkit.Bukkit;

import java.util.UUID;

public class OptionsWindow extends ShopGuiWindow {
    public OptionsWindow(final UUID player) {
        super(player);
        page = Bukkit.createInventory(null, INV_SIZE, "Options");
        initInvContents();
    }

    @Override
    protected void initInvContents() {
    }
}
