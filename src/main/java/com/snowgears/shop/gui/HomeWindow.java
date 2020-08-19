package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler.GuiIcon;
import com.snowgears.shop.handler.ShopGuiHandler.GuiTitle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class HomeWindow extends ShopGuiWindow {
    public HomeWindow(UUID player) {
        super(player);
        page = Bukkit.createInventory(null, INV_SIZE, gui.getTitle(GuiTitle.HOME));
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        page.setItem(21, gui.getIcon(GuiIcon.HOME_LIST_OWN_SHOPS, null, null));
        page.setItem(22, gui.getIcon(GuiIcon.HOME_LIST_PLAYERS, null, null));
        page.setItem(23, gui.getIcon(GuiIcon.HOME_SETTINGS, null, null));
        Player player = getPlayer();
        if (player != null && ((Shop.getPlugin().usePerms() && player.hasPermission("shop.operator")) || player.isOp())) {
            page.setItem(43, gui.getIcon(GuiIcon.HOME_COMMANDS, null, null));
        }
    }
}
