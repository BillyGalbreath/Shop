package com.snowgears.shop.gui;

import com.snowgears.shop.handler.ShopGuiHandler.GuiIcon;
import com.snowgears.shop.handler.ShopGuiHandler.GuiTitle;
import org.bukkit.Bukkit;

import java.util.UUID;

public class CommandsWindow extends ShopGuiWindow {
    public CommandsWindow(UUID player) {
        super(player);
        this.page = Bukkit.createInventory(null, this.INV_SIZE, gui.getTitle(GuiTitle.COMMANDS));
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        page.setItem(10, gui.getIcon(GuiIcon.COMMANDS_CURRENCY, null, null));
        page.setItem(11, gui.getIcon(GuiIcon.COMMANDS_SET_CURRENCY, null, null));
        page.setItem(12, gui.getIcon(GuiIcon.COMMANDS_SET_GAMBLE, null, null));
        page.setItem(13, gui.getIcon(GuiIcon.COMMANDS_REFRESH_DISPLAYS, null, null));
        page.setItem(14, gui.getIcon(GuiIcon.COMMANDS_RELOAD, null, null));
    }
}
