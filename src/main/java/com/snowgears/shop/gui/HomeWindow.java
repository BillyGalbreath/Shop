package com.snowgears.shop.gui;

import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HomeWindow extends ShopGuiWindow {
    public HomeWindow(UUID player) {
        super(player);
        this.title = Shop.getPlugin().getGuiHandler().getTitle(ShopGuiHandler.GuiTitle.HOME);
        this.page = Bukkit.createInventory(null, INV_SIZE, title);
        initInvContents();
    }

    @Override
    protected void initInvContents() {

        ItemStack listShopsIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_OWN_SHOPS, null, null);
        page.setItem(21, listShopsIcon);


        ItemStack listPlayersIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_LIST_PLAYERS, null, null);
        page.setItem(22, listPlayersIcon);


        ItemStack settingsIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_SETTINGS, null, null);
        page.setItem(23, settingsIcon);


        //list the commands if they have operator permission
        Player p = this.getPlayer();
        if (p != null) {
            if ((Shop.getPlugin().usePerms() && p.hasPermission("shop.operator")) || p.isOp()) {

                ItemStack commandsIcon = Shop.getPlugin().getGuiHandler().getIcon(ShopGuiHandler.GuiIcon.HOME_COMMANDS, null, null);
                page.setItem(43, commandsIcon);
            }
        }
    }
}
