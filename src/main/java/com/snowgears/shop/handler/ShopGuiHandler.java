package com.snowgears.shop.handler;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.gui.CommandsWindow;
import com.snowgears.shop.gui.HomeWindow;
import com.snowgears.shop.gui.ListPlayersWindow;
import com.snowgears.shop.gui.PlayerSettingsWindow;
import com.snowgears.shop.gui.ShopGuiWindow;
import com.snowgears.shop.util.PlayerSettings;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ShopGuiHandler {

    public enum GuiIcon {
        MENUBAR_BACK, MENUBAR_SEARCH, MENUBAR_LAST_PAGE, MENUBAR_NEXT_PAGE,
        HOME_LIST_OWN_SHOPS, HOME_LIST_PLAYERS, HOME_SETTINGS, HOME_COMMANDS,
        LIST_SHOP, LIST_PLAYER, LIST_PLAYER_ADMIN,
        SETTINGS_NOTIFY_OWNER_ON, SETTINGS_NOTIFY_OWNER_OFF, SETTINGS_NOTIFY_USER_ON, SETTINGS_NOTIFY_USER_OFF, SETTINGS_NOTIFY_STOCK_ON, SETTINGS_NOTIFY_STOCK_OFF,
        COMMANDS_CURRENCY, COMMANDS_SET_CURRENCY, COMMANDS_SET_GAMBLE, COMMANDS_REFRESH_DISPLAYS, COMMANDS_RELOAD
    }

    public enum GuiTitle {
        HOME, LIST_PLAYERS, SETTINGS, COMMANDS
    }

    private final Shop plugin;
    private final HashMap<UUID, ShopGuiWindow> playerGuiWindows = new HashMap<>();
    private final HashMap<UUID, PlayerSettings> playerSettings = new HashMap<>();
    private final HashMap<GuiIcon, ItemStack> guiIcons = new HashMap<>();
    private final HashMap<GuiTitle, String> guiWindowTitles = new HashMap<>();

    public ShopGuiHandler(Shop instance) {
        this.plugin = instance;
        loadIconsAndTitles();
    }

    public ShopGuiWindow getWindow(Player player) {
        if (playerGuiWindows.get(player.getUniqueId()) != null) {
            return playerGuiWindows.get(player.getUniqueId());
        }
        HomeWindow window = new HomeWindow(player.getUniqueId());
        playerGuiWindows.put(player.getUniqueId(), window);
        return window;
    }

    public void setWindow(Player player, ShopGuiWindow window) {
        playerGuiWindows.put(player.getUniqueId(), window);
        window.open();
    }

    public boolean getSettingsOption(Player player, PlayerSettings.Option option) {
        if (playerSettings.get(player.getUniqueId()) != null) {
            PlayerSettings settings = playerSettings.get(player.getUniqueId());
            return settings.getOption(option);
        }
        PlayerSettings settings = PlayerSettings.loadFromFile(player);
        if (settings == null) {
            settings = new PlayerSettings(player);
        }
        playerSettings.put(player.getUniqueId(), settings);
        return settings.getOption(option);
    }

    public void toggleSettingsOption(Player player, PlayerSettings.Option option) {
        PlayerSettings settings;
        if (playerSettings.get(player.getUniqueId()) != null) {
            settings = playerSettings.get(player.getUniqueId());
        } else {
            settings = PlayerSettings.loadFromFile(player);
            if (settings == null) {
                settings = new PlayerSettings(player);
            }
        }
        settings.setOption(option, !getSettingsOption(player, option));
        playerSettings.put(player.getUniqueId(), settings);
    }

    public ItemStack getIcon(GuiIcon iconEnum, OfflinePlayer player, AbstractShop shop) {
        if (iconEnum == GuiIcon.LIST_SHOP) {
            ItemStack icon = shop.getItemStack().clone();
            icon.setAmount(1);
            List<String> lore = new ArrayList<>();
            lore.add("Type: " + shop.getType().toString().toUpperCase());
            if (shop.getType() == ShopType.BARTER) {
                lore.add("Price: " + (int) shop.getPrice() + " " + Shop.getPlugin().getItemNameUtil().getName(shop.getSecondaryItemStack()));
            } else if (shop.getType() == ShopType.BUY) {
                lore.add("Pays: " + shop.getPriceString());
            } else {
                lore.add("Price: " + shop.getPriceString());
            }
            if (!shop.isAdmin()) {
                lore.add("Stock: " + shop.getStock());
            }
            lore.add("Location: " + UtilMethods.getCleanLocation(shop.getSignLocation(), true));
            String name = plugin.getItemNameUtil().getName(shop.getItemStack()) + " (x" + shop.getAmount() + ")";
            ItemMeta iconMeta = icon.getItemMeta();
            iconMeta.setDisplayName(name);
            iconMeta.setLore(lore);
            icon.setItemMeta(iconMeta);
            return icon;
        }
        if (iconEnum == GuiIcon.LIST_PLAYER) {
            ItemStack icon = new ItemStack(Material.PLAYER_HEAD);
            if (player == null) {
                return icon;
            }
            List<String> lore = new ArrayList<>();
            lore.add("Shops: " + Shop.getPlugin().getShopHandler().getShops(player.getUniqueId()).size());
            SkullMeta meta = (SkullMeta) icon.getItemMeta();
            meta.setOwningPlayer(player);
            meta.setDisplayName(player.getName());
            meta.setLore(lore);
            icon.setItemMeta(meta);
            return icon;
        } else {
            if (iconEnum == GuiIcon.LIST_PLAYER_ADMIN) {
                ItemStack icon = guiIcons.get(iconEnum).clone();
                ItemMeta iconMeta2 = icon.getItemMeta();
                List<String> lore2 = iconMeta2.getLore();
                if (lore2 == null) {
                    lore2 = new ArrayList<>();
                }
                lore2.add("Shops: " + Shop.getPlugin().getShopHandler().getShops(plugin.getShopHandler().getAdminUUID()).size());
                iconMeta2.setLore(lore2);
                icon.setItemMeta(iconMeta2);
                return icon;
            }
            if (guiIcons.containsKey(iconEnum)) {
                return guiIcons.get(iconEnum);
            }
            return null;
        }
    }

    public String getTitle(GuiTitle title) {
        return this.guiWindowTitles.get(title);
    }

    public String getTitle(ShopGuiWindow window) {
        if (window instanceof HomeWindow) {
            return this.guiWindowTitles.get(GuiTitle.HOME);
        }
        if (window instanceof ListPlayersWindow) {
            return this.guiWindowTitles.get(GuiTitle.LIST_PLAYERS);
        }
        if (window instanceof PlayerSettingsWindow) {
            return this.guiWindowTitles.get(GuiTitle.SETTINGS);
        }
        if (window instanceof CommandsWindow) {
            return this.guiWindowTitles.get(GuiTitle.COMMANDS);
        }
        return "Window";
    }

    private void loadIconsAndTitles() {
        File configFile = new File(this.plugin.getDataFolder(), "guiConfig.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            UtilMethods.copy(this.plugin.getResource("guiConfig.yml"), configFile);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        for (GuiTitle titleEnum : GuiTitle.values()) {
            String titleString = config.getString("titles." + titleEnum.toString().toLowerCase());
            this.guiWindowTitles.put(titleEnum, titleString);
        }
        for (GuiIcon iconEnum : GuiIcon.values()) {
            String iconString = iconEnum.toString().toLowerCase();
            String parentKey = iconString.substring(0, iconString.indexOf(95));
            String childKey = iconString.substring(iconString.indexOf(95) + 1);
            String type = config.getString("icons." + parentKey + "." + childKey + ".type");
            String name = config.getString("icons." + parentKey + "." + childKey + ".name");
            if (name != null) {
                name = ChatColor.translateAlternateColorCodes('&', name);
            }
            List<String> loreLines = config.getStringList("icons." + parentKey + "." + childKey + ".lore");
            List<String> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            ItemStack icon = null;
            if (type != null) {
                icon = new ItemStack(Material.valueOf(type.toUpperCase()));
            } else if (childKey.equals("set_gamble")) {
                icon = this.plugin.getGambleDisplayItem();
            } else if (parentKey.equals("list") && childKey.equals("player")) {
                icon = new ItemStack(Material.PLAYER_HEAD);
            }
            if (icon != null) {
                ItemMeta iconMeta = icon.getItemMeta();
                if (name != null) {
                    iconMeta.setDisplayName(name);
                }
                if (!lore.isEmpty()) {
                    iconMeta.setLore(lore);
                }
                icon.setItemMeta(iconMeta);
                this.guiIcons.put(iconEnum, icon);
            }
        }
    }
}
