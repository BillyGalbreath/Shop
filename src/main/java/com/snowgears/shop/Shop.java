package com.snowgears.shop;

import com.snowgears.shop.display.DisplayListener;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.gui.ShopGUIListener;
import com.snowgears.shop.handler.CommandHandler;
import com.snowgears.shop.handler.EnderChestHandler;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopHandler;
import com.snowgears.shop.listener.ArmorStandListener;
import com.snowgears.shop.listener.CraftbookListener;
import com.snowgears.shop.listener.CreativeSelectionListener;
import com.snowgears.shop.listener.MiscListener;
import com.snowgears.shop.listener.ShopListener;
import com.snowgears.shop.listener.TransactionListener;
import com.snowgears.shop.util.ItemNameUtil;
import com.snowgears.shop.util.Metrics;
import com.snowgears.shop.util.PriceUtil;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

public class Shop extends JavaPlugin {
    private static Logger log;
    private static Shop plugin;
    private ShopListener shopListener;
    private DisplayListener displayListener;
    private TransactionListener transactionListener;
    private MiscListener miscListener;
    private CreativeSelectionListener creativeSelectionListener;
    private CraftbookListener craftBookListener;
    private ArmorStandListener armorStandListener;
    private ShopGUIListener guiListener;
    private ShopHandler shopHandler;
    private CommandHandler commandHandler;
    private ShopGuiHandler guiHandler;
    private EnderChestHandler enderChestHandler;
    private ShopMessage shopMessage;
    private ItemNameUtil itemNameUtil;
    private PriceUtil priceUtil;
    private boolean usePerms;
    private boolean enableMetrics;
    private boolean enableGUI;
    private boolean useVault;
    private boolean hookWorldGuard;
    private String commandAlias;
    private DisplayType displayType;
    private boolean checkItemDurability;
    private boolean allowCreativeSelection;
    private boolean playSounds;
    private boolean playEffects;
    private ItemStack gambleDisplayItem;
    private ItemStack itemCurrency;
    private String itemCurrencyName;
    private String vaultCurrencySymbol;
    private String currencyFormat;
    private Economy econ;
    private boolean useEnderchests;
    private double creationCost;
    private double destructionCost;
    private double taxPercent;
    private ArrayList<String> worldBlackList;
    private YamlConfiguration config;

    public Shop() {
        plugin = this;
        log = getLogger();

        this.itemCurrency = null;
        this.itemCurrencyName = "";
        this.vaultCurrencySymbol = "";
        this.currencyFormat = "";
        this.econ = null;
    }

    public static Shop getPlugin() {
        return plugin;
    }

    public void onEnable() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("config.yml"), configFile);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        File chatConfigFile = new File(getDataFolder(), "chatConfig.yml");
        if (!chatConfigFile.exists()) {
            chatConfigFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("chatConfig.yml"), chatConfigFile);
        }
        File signConfigFile = new File(getDataFolder(), "signConfig.yml");
        if (!signConfigFile.exists()) {
            signConfigFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("signConfig.yml"), signConfigFile);
        }
        shopListener = new ShopListener(this);
        transactionListener = new TransactionListener(this);
        miscListener = new MiscListener(this);
        creativeSelectionListener = new CreativeSelectionListener(this);
        displayListener = new DisplayListener(this);
        armorStandListener = new ArmorStandListener(this);
        guiListener = new ShopGUIListener(this);
        if (getServer().getPluginManager().isPluginEnabled("CraftBook")) {
            craftBookListener = new CraftbookListener();
            getServer().getPluginManager().registerEvents(craftBookListener, this);
        }
        try {
            displayType = DisplayType.valueOf(config.getString("displayType"));
        } catch (Exception e) {
            displayType = DisplayType.ITEM;
        }
        shopMessage = new ShopMessage(this);
        itemNameUtil = new ItemNameUtil();
        priceUtil = new PriceUtil();
        File fileDirectory = new File(getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            boolean success = fileDirectory.mkdirs();
            if (!success) {
                getServer().getConsoleSender().sendMessage("[Shop]" + ChatColor.RED + " Data folder could not be created.");
            }
        }
        usePerms = config.getBoolean("usePermissions");
        enableMetrics = config.getBoolean("enableMetrics");
        enableGUI = config.getBoolean("enableGUI");
        hookWorldGuard = config.getBoolean("hookWorldGuard");
        commandAlias = config.getString("commandAlias");
        checkItemDurability = config.getBoolean("checkItemDurability");
        allowCreativeSelection = config.getBoolean("allowCreativeSelection");
        playSounds = config.getBoolean("playSounds");
        playEffects = config.getBoolean("playEffects");
        useVault = config.getBoolean("useVault");
        if (enableMetrics) {
            try {
                Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (IOException ignore) {
            }
        }
        File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
        if (itemCurrencyFile.exists()) {
            YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
            itemCurrency = currencyConfig.getItemStack("item");
            if (itemCurrency != null) {
                itemCurrency.setAmount(1);
            }
        } else {
            try {
                itemCurrency = new ItemStack(Material.EMERALD);
                itemCurrencyFile.createNewFile();
                YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
                currencyConfig.set("item", itemCurrency);
                currencyConfig.save(itemCurrencyFile);
            } catch (Exception ignore) {
            }
        }
        File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
        if (!gambleDisplayFile.exists()) {
            gambleDisplayFile.getParentFile().mkdirs();
            UtilMethods.copy(getResource("GAMBLE_DISPLAY.yml"), gambleDisplayFile);
        }
        YamlConfiguration gambleItemConfig = YamlConfiguration.loadConfiguration(gambleDisplayFile);
        gambleDisplayItem = gambleItemConfig.getItemStack("GAMBLE_DISPLAY");
        itemCurrencyName = config.getString("itemCurrencyName");
        vaultCurrencySymbol = config.getString("vaultCurrencyName");
        currencyFormat = config.getString("currencyFormat");
        useEnderchests = config.getBoolean("enableEnderChests");
        creationCost = config.getDouble("creationCost");
        destructionCost = config.getDouble("destructionCost");
        (worldBlackList = new ArrayList<>()).addAll(config.getConfigurationSection("worldBlacklist").getKeys(true));
        if (useVault) {
            if (!setupEconomy()) {
                log.severe("[Shop] PLUGIN DISABLED DUE TO NO VAULT DEPENDENCY FOUND ON SERVER!");
                log.info("[Shop] If you do not wish to use Vault with Shop, make sure to set 'useVault' in the config file to false.");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            log.info("[Shop] Vault dependency found. Using the Vault economy (" + vaultCurrencySymbol + ") for currency on the server.");
        } else if (itemCurrency == null) {
            log.severe("[Shop] PLUGIN DISABLED DUE TO INVALID VALUE IN CONFIGURATION SECTION: \"itemCurrencyID\"");
            getServer().getPluginManager().disablePlugin(this);
        } else {
            log.info("[Shop] Shops will use " + itemCurrency.getType().name().replace("_", " ").toLowerCase() + " as the currency on the server.");
        }
        commandHandler = new CommandHandler(this, "shop.use", commandAlias, "Base command for the Shop plugin", "/shop", Collections.singletonList(commandAlias));
        shopHandler = new ShopHandler(this);
        guiHandler = new ShopGuiHandler(this);
        enderChestHandler = new EnderChestHandler(this);
        getServer().getPluginManager().registerEvents(displayListener, this);
        getServer().getPluginManager().registerEvents(shopListener, this);
        getServer().getPluginManager().registerEvents(transactionListener, this);
        getServer().getPluginManager().registerEvents(miscListener, this);
        getServer().getPluginManager().registerEvents(creativeSelectionListener, this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(armorStandListener, this);
    }

    public void onDisable() {
    }

    public void reload() {
        HandlerList.unregisterAll(displayListener);
        HandlerList.unregisterAll(shopListener);
        HandlerList.unregisterAll(transactionListener);
        HandlerList.unregisterAll(miscListener);
        HandlerList.unregisterAll(creativeSelectionListener);
        HandlerList.unregisterAll(guiListener);
        if (craftBookListener != null) {
            HandlerList.unregisterAll(craftBookListener);
        }
        onEnable();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public ShopListener getShopListener() {
        return shopListener;
    }

    public DisplayListener getDisplayListener() {
        return displayListener;
    }

    public CreativeSelectionListener getCreativeSelectionListener() {
        return creativeSelectionListener;
    }

    public TransactionListener getTransactionListener() {
        return transactionListener;
    }

    public ShopHandler getShopHandler() {
        return shopHandler;
    }

    public ShopGuiHandler getGuiHandler() {
        return guiHandler;
    }

    public EnderChestHandler getEnderChestHandler() {
        return enderChestHandler;
    }

    public boolean usePerms() {
        return usePerms;
    }

    public boolean useVault() {
        return useVault;
    }

    public boolean hookWorldGuard() {
        return hookWorldGuard;
    }

    public DisplayType getDisplayType() {
        return displayType;
    }

    public boolean checkItemDurability() {
        return checkItemDurability;
    }

    public boolean allowCreativeSelection() {
        return allowCreativeSelection;
    }

    public boolean playSounds() {
        return playSounds;
    }

    public boolean playEffects() {
        return playEffects;
    }

    public boolean useGUI() {
        return enableGUI;
    }

    public ItemStack getGambleDisplayItem() {
        return gambleDisplayItem;
    }

    public ItemStack getItemCurrency() {
        return itemCurrency;
    }

    public void setItemCurrency(ItemStack itemCurrency) {
        this.itemCurrency = itemCurrency;
        try {
            File fileDirectory = new File(getDataFolder(), "Data");
            File itemCurrencyFile = new File(fileDirectory, "itemCurrency.yml");
            YamlConfiguration currencyConfig = YamlConfiguration.loadConfiguration(itemCurrencyFile);
            currencyConfig.set("item", getItemCurrency());
            currencyConfig.save(itemCurrencyFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setGambleDisplayItem(ItemStack gambleDisplayItem) {
        this.gambleDisplayItem = gambleDisplayItem;
        try {
            File fileDirectory = new File(getDataFolder(), "Data");
            File gambleDisplayFile = new File(fileDirectory, "gambleDisplayItem.yml");
            if (!gambleDisplayFile.exists()) {
                gambleDisplayFile.getParentFile().mkdirs();
                gambleDisplayFile.createNewFile();
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(gambleDisplayFile);
            config.set("GAMBLE_DISPLAY", gambleDisplayItem);
            config.save(gambleDisplayFile);
            reload();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getItemCurrencyName() {
        return itemCurrencyName;
    }

    public String getVaultCurrencySymbol() {
        return vaultCurrencySymbol;
    }

    public String getCommandAlias() {
        return commandAlias;
    }

    public String getPriceString(double price, boolean pricePer) {
        if (price == 0.0) {
            return ShopMessage.getFreePriceWord();
        }
        String format = currencyFormat;
        if (format.contains("[name]")) {
            if (useVault()) {
                format = format.replace("[name]", vaultCurrencySymbol);
            } else {
                format = format.replace("[name]", itemCurrencyName);
            }
        }
        if (!format.contains("[price]")) {
            return format;
        }
        if (useVault()) {
            return format.replace("[price]", new DecimalFormat("0.00").format(price));
        }
        if (pricePer) {
            return format.replace("[price]", new DecimalFormat("#.##").format(price));
        }
        return format.replace("[price]", "" + (int) price);
    }

    public String getPriceComboString(double price, double priceSell, boolean pricePer) {
        if (price == 0.0) {
            return ShopMessage.getFreePriceWord();
        }
        String format = currencyFormat;
        if (format.contains("[name]")) {
            if (useVault()) {
                format = format.replace("[name]", vaultCurrencySymbol);
            } else {
                format = format.replace("[name]", itemCurrencyName);
            }
        }
        if (!format.contains("[price]")) {
            return format;
        }
        if (useVault()) {
            return format.replace("[price]", new DecimalFormat("0.00").format(price) + "/" + new DecimalFormat("0.00").format(priceSell));
        }
        if (pricePer) {
            return format.replace("[price]", new DecimalFormat("#.##").format(price) + "/" + new DecimalFormat("0.00").format(priceSell));
        }
        return format.replace("[price]", "" + (int) price + "/" + (int) priceSell);
    }

    public double getTaxPercent() {
        return taxPercent;
    }

    public Economy getEconomy() {
        return econ;
    }

    public boolean useEnderChests() {
        return useEnderchests;
    }

    public double getCreationCost() {
        return creationCost;
    }

    public double getDestructionCost() {
        return destructionCost;
    }

    public ItemNameUtil getItemNameUtil() {
        return itemNameUtil;
    }

    public ArrayList<String> getWorldBlacklist() {
        return worldBlackList;
    }
}
