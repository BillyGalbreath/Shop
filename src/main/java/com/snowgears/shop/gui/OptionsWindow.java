package com.snowgears.shop.gui;

import org.bukkit.Bukkit;

import java.util.UUID;

public class OptionsWindow extends ShopGuiWindow {
    public OptionsWindow(UUID player) {
        super(player);
        this.page = Bukkit.createInventory(null, INV_SIZE, "Options");
        initInvContents();
    }

    @Override
    protected void initInvContents() {
    }
}
