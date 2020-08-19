package com.snowgears.shop.gui;


import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopGuiHandler.GuiIcon;
import com.snowgears.shop.util.PlayerSettings;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

public class ShopGUIListener implements Listener {
    private final Shop plugin;
    private final ShopGuiHandler gui;

    public ShopGUIListener(Shop instance) {
        this.plugin = instance;
        this.gui = plugin.getGuiHandler();
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ShopGuiWindow window = gui.getWindow(player);
        if (!event.getInventory().equals(window.getInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() == 0 && window.hasPrevWindow()) {
            gui.setWindow(player, window.prevWindow);
            return;
        }

        if (event.getRawSlot() == 45) {
            window.scrollPagePrev();
            return;
        }

        if (event.getRawSlot() == 53) {
            window.scrollPageNext();
            return;
        }

        if (window instanceof HomeWindow) {
            ItemStack listShopsIcon = getIcon(GuiIcon.HOME_LIST_OWN_SHOPS);
            ItemStack listPlayersIcon = getIcon(GuiIcon.HOME_LIST_PLAYERS);
            ItemStack settingsIcon = getIcon(GuiIcon.HOME_SETTINGS);
            ItemStack commandsIcon = getIcon(GuiIcon.HOME_COMMANDS);
            if (clicked.getType() == listShopsIcon.getType()) {
                ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId(), player.getUniqueId());
                shopsWindow.setPrevWindow(window);
                gui.setWindow(player, shopsWindow);
            } else if (clicked.getType() == listPlayersIcon.getType()) {
                ListPlayersWindow playersWindow = new ListPlayersWindow(player.getUniqueId());
                playersWindow.setPrevWindow(window);
                gui.setWindow(player, playersWindow);
            } else if (clicked.getType() == settingsIcon.getType()) {
                PlayerSettingsWindow settingsWindow = new PlayerSettingsWindow(player.getUniqueId());
                settingsWindow.setPrevWindow(window);
                gui.setWindow(player, settingsWindow);
            } else if (clicked.getType() == commandsIcon.getType()) {
                CommandsWindow commandsWindow = new CommandsWindow(player.getUniqueId());
                commandsWindow.setPrevWindow(window);
                gui.setWindow(player, commandsWindow);
            }
            return;
        }

        if (window instanceof ListPlayersWindow) {
            ItemStack playerIcon = getIcon(GuiIcon.LIST_PLAYER);
            ItemStack adminPlayerIcon = getIcon(GuiIcon.LIST_PLAYER_ADMIN);
            if (clicked.getType() == playerIcon.getType() || clicked.getType() == adminPlayerIcon.getType()) {
                String name = clicked.getItemMeta().getDisplayName();
                UUID uuid;
                if (name.equals(adminPlayerIcon.getItemMeta().getDisplayName())) {
                    uuid = Shop.getPlugin().getShopHandler().getAdminUUID();
                } else {
                    uuid = Bukkit.getOfflinePlayer(name).getUniqueId();
                }
                if (uuid == null) {
                    return;
                }
                ListShopsWindow shopsWindow = new ListShopsWindow(player.getUniqueId(), uuid);
                shopsWindow.setPrevWindow(window);
                gui.setWindow(player, shopsWindow);
            }
            return;
        }

        if (window instanceof ListShopsWindow) {
            if (!clicked.hasItemMeta()) {
                return;
            }
            ItemMeta meta = clicked.getItemMeta();
            if (!meta.hasLore()) {
                return;
            }
            List<String> lore = meta.getLore();
            if (lore == null) {
                return;
            }
            for (String line : lore) {
                if (!line.startsWith("Location: ")) {
                    continue;
                }
                line = line.substring(10);
                Location loc = UtilMethods.getLocation(line);
                AbstractShop shop = plugin.getShopHandler().getShop(loc);
                if (shop == null) {
                    continue;
                }
                if (player.isOp() || (Shop.getPlugin().usePerms() && (player.hasPermission("shop.operator") || player.hasPermission("shop.gui.teleport")))) {
                    shop.teleportPlayer(player);
                }
                return;
            }
            return;
        }

        if (window instanceof PlayerSettingsWindow) {
            ItemStack ownerIconOn = getIcon(GuiIcon.SETTINGS_NOTIFY_OWNER_ON);
            ItemStack ownerIconOff = getIcon(GuiIcon.SETTINGS_NOTIFY_OWNER_OFF);
            ItemStack userIconOn = getIcon(GuiIcon.SETTINGS_NOTIFY_USER_ON);
            ItemStack userIconOff = getIcon(GuiIcon.SETTINGS_NOTIFY_USER_OFF);
            ItemStack stockIconOn = getIcon(GuiIcon.SETTINGS_NOTIFY_STOCK_ON);
            ItemStack stockIconOff = getIcon(GuiIcon.SETTINGS_NOTIFY_STOCK_OFF);
            PlayerSettings.Option option;
            if (clicked.isSimilar(ownerIconOn)) {
                option = PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS;
                event.getInventory().setItem(event.getRawSlot(), ownerIconOff);
            } else if (clicked.isSimilar(ownerIconOff)) {
                option = PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS;
                event.getInventory().setItem(event.getRawSlot(), ownerIconOn);
            } else if (clicked.isSimilar(userIconOn)) {
                option = PlayerSettings.Option.SALE_USER_NOTIFICATIONS;
                event.getInventory().setItem(event.getRawSlot(), userIconOff);
            } else if (clicked.isSimilar(userIconOff)) {
                option = PlayerSettings.Option.SALE_USER_NOTIFICATIONS;
                event.getInventory().setItem(event.getRawSlot(), userIconOn);
            } else if (clicked.isSimilar(stockIconOn)) {
                option = PlayerSettings.Option.STOCK_NOTIFICATIONS;
                event.getInventory().setItem(event.getRawSlot(), stockIconOff);
            } else if (clicked.isSimilar(stockIconOff)) {
                option = PlayerSettings.Option.STOCK_NOTIFICATIONS;
                event.getInventory().setItem(event.getRawSlot(), stockIconOn);
            } else {
                option = PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS;
            }
            Shop.getPlugin().getGuiHandler().toggleSettingsOption(player, option);
            player.updateInventory();
            return;
        }

        if (window instanceof CommandsWindow) {
            String command = Shop.getPlugin().getCommandAlias() + " ";
            ItemStack currencyIcon = getIcon(GuiIcon.COMMANDS_CURRENCY);
            ItemStack setCurrencyIcon = getIcon(GuiIcon.COMMANDS_SET_CURRENCY);
            ItemStack setGambleIcon = getIcon(GuiIcon.COMMANDS_SET_GAMBLE);
            ItemStack refreshIcon = getIcon(GuiIcon.COMMANDS_REFRESH_DISPLAYS);
            ItemStack reloadIcon = getIcon(GuiIcon.COMMANDS_RELOAD);
            if (clicked.isSimilar(currencyIcon)) {
                command += "currency";
            } else if (clicked.isSimilar(setCurrencyIcon)) {
                command += "setcurrency";
            } else if (clicked.isSimilar(setGambleIcon)) {
                command += "setgamble";
            } else if (clicked.isSimilar(refreshIcon)) {
                command += "item refresh";
            } else if (clicked.isSimilar(reloadIcon)) {
                command += "reload";
            }
            player.closeInventory();
            Bukkit.getServer().dispatchCommand(player, command);
            return;
        }

        if (window instanceof SearchWindow && window.hasPrevWindow()) {
            gui.setWindow(player, window.prevWindow);
        }
    }

    private ItemStack getIcon(GuiIcon icon) {
        return gui.getIcon(icon, null, null);
    }
}
