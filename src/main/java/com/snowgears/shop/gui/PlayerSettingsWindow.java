package com.snowgears.shop.gui;

import com.snowgears.shop.handler.ShopGuiHandler.GuiIcon;
import com.snowgears.shop.handler.ShopGuiHandler.GuiTitle;
import com.snowgears.shop.util.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerSettingsWindow extends ShopGuiWindow {
    public PlayerSettingsWindow(final UUID player) {
        super(player);
        page = Bukkit.createInventory(null, this.INV_SIZE, gui.getTitle(GuiTitle.SETTINGS));
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        Player player = getPlayer();
        if (player != null) {
            page.setItem(10, gui.getSettingsOption(player, PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS) ?
                    gui.getIcon(GuiIcon.SETTINGS_NOTIFY_OWNER_ON, player, null) :
                    gui.getIcon(GuiIcon.SETTINGS_NOTIFY_OWNER_OFF, player, null)
            );
            page.setItem(11, gui.getSettingsOption(player, PlayerSettings.Option.SALE_USER_NOTIFICATIONS) ?
                    gui.getIcon(GuiIcon.SETTINGS_NOTIFY_USER_ON, player, null) :
                    gui.getIcon(GuiIcon.SETTINGS_NOTIFY_USER_OFF, player, null)
            );
            page.setItem(12, gui.getSettingsOption(player, PlayerSettings.Option.STOCK_NOTIFICATIONS) ?
                    gui.getIcon(GuiIcon.SETTINGS_NOTIFY_STOCK_ON, player, null) :
                    gui.getIcon(GuiIcon.SETTINGS_NOTIFY_STOCK_OFF, player, null)
            );
        }
    }
}
