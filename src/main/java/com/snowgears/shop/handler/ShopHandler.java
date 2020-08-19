package com.snowgears.shop.handler;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.ComboShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.Directional;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class ShopHandler {
    private final BlockFace[] DIRECTIONS = new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public final Shop plugin;
    private final HashMap<UUID, List<Location>> playerShops = new HashMap<>();
    private final HashMap<Location, AbstractShop> allShops = new HashMap<>();
    private final List<Material> shopMaterials = new ArrayList<>();
    private final List<UUID> playersSavingShops = new ArrayList<>();
    private final UUID adminUUID;

    public ShopHandler(Shop instance) {
        plugin = instance;
        shopMaterials.add(Material.CHEST);
        shopMaterials.add(Material.TRAPPED_CHEST);
        shopMaterials.add(Material.BARREL);
        if (plugin.useEnderChests()) {
            shopMaterials.add(Material.ENDER_CHEST);
        }
        adminUUID = UUID.randomUUID();
        new BukkitRunnable() {
            public void run() {
                loadShops();
            }
        }.runTaskLater(plugin, 10L);
    }

    public AbstractShop getShop(Location loc) {
        return allShops.get(loc);
    }

    public AbstractShop getShopByChest(Block shopChest) {
        if (shopChest.getState() instanceof ShulkerBox) {
            for (BlockFace direction : DIRECTIONS) {
                AbstractShop shop = getShop(shopChest.getRelative(direction).getLocation());
                if (shop != null && shop.getChestLocation().equals(shopChest.getLocation())) {
                    return shop;
                }
            }
            return null;
        }
        if (isChest(shopChest)) {
            BlockFace chestFacing = ((Directional) shopChest.getState().getBlockData()).getFacing();
            ArrayList<Block> chestBlocks = new ArrayList<>();
            chestBlocks.add(shopChest);
            InventoryHolder holder = null;
            if (shopChest.getState() instanceof Chest) {
                Chest chest = (Chest) shopChest.getState();
                holder = chest.getInventory().getHolder();
                if (holder instanceof DoubleChest) {
                    DoubleChest doubleChest = (DoubleChest) holder;
                    Chest chest2 = (Chest) doubleChest.getLeftSide();
                    Chest chest3 = (Chest) doubleChest.getRightSide();
                    if (chest2 != null && chest.getLocation().equals(chest2.getLocation())) {
                        if (chest3 != null) {
                            chestBlocks.add(chest3.getBlock());
                        }
                    } else if (chest2 != null) {
                        chestBlocks.add(chest2.getBlock());
                    }
                }
            }
            for (Block chestBlock : chestBlocks) {
                Block signBlock = chestBlock.getRelative(chestFacing);
                if (Tag.WALL_SIGNS.isTagged(signBlock.getType())) {
                    Directional sign = (Directional) signBlock.getState().getBlockData();
                    if (chestFacing != sign.getFacing()) {
                        continue;
                    }
                    AbstractShop shop2 = getShop(signBlock.getLocation());
                    if (shop2 != null) {
                        return shop2;
                    }
                } else {
                    if (holder instanceof DoubleChest) {
                        continue;
                    }
                    AbstractShop shop3 = getShop(signBlock.getLocation());
                    if (shop3 == null) {
                        continue;
                    }
                    shop3.delete();
                }
            }
        }
        return null;
    }

    public AbstractShop getShopNearBlock(Block block) {
        BlockFace[] array;
        BlockFace[] faces = array = new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : array) {
            if (isChest(block.getRelative(face))) {
                Block shopChest = block.getRelative(face);
                for (BlockFace newFace : faces) {
                    if (Tag.WALL_SIGNS.isTagged(shopChest.getRelative(newFace).getType())) {
                        AbstractShop shop = getShop(shopChest.getRelative(newFace).getLocation());
                        if (shop != null) {
                            return shop;
                        }
                    }
                }
            }
        }
        return null;
    }

    public void addShop(AbstractShop shop) {
        AbstractShop s = getShop(shop.getSignLocation());
        if (s != null) {
            return;
        }
        allShops.put(shop.getSignLocation(), shop);
        List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
        if (!shopLocations.contains(shop.getSignLocation())) {
            shopLocations.add(shop.getSignLocation());
            playerShops.put(shop.getOwnerUUID(), shopLocations);
        }
    }

    public void removeShop(AbstractShop shop) {
        allShops.remove(shop.getSignLocation());
        if (playerShops.containsKey(shop.getOwnerUUID())) {
            List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
            if (shopLocations.contains(shop.getSignLocation())) {
                shopLocations.remove(shop.getSignLocation());
                playerShops.put(shop.getOwnerUUID(), shopLocations);
            }
        }
    }

    public List<AbstractShop> getShops(UUID player) {
        List<AbstractShop> shops = new ArrayList<>();
        for (Location shopSign : getShopLocations(player)) {
            AbstractShop shop = getShop(shopSign);
            if (shop != null) {
                shops.add(shop);
            }
        }
        return shops;
    }

    public List<OfflinePlayer> getShopOwners() {
        ArrayList<OfflinePlayer> owners = new ArrayList<>();
        for (UUID player : playerShops.keySet()) {
            owners.add(Bukkit.getOfflinePlayer(player));
        }
        return owners;
    }

    private List<Location> getShopLocations(UUID player) {
        List<Location> shopLocations;
        if (playerShops.containsKey(player)) {
            shopLocations = playerShops.get(player);
        } else {
            shopLocations = new ArrayList<>();
        }
        return shopLocations;
    }

    public int getNumberOfShops() {
        return allShops.size();
    }

    public int getNumberOfShops(Player player) {
        return getShopLocations(player.getUniqueId()).size();
    }

    public void refreshShopDisplays() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!Display.isDisplay(entity)) {
                    if (entity.getType() != EntityType.DROPPED_ITEM) {
                        continue;
                    }
                    ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                    if (!UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                        continue;
                    }
                }
                entity.remove();
            }
        }
        for (AbstractShop shop : allShops.values()) {
            shop.getDisplay().spawn();
        }
    }

    public void saveShops(UUID player) {
        if (playersSavingShops.contains(player)) {
            return;
        }
        new BukkitRunnable() {
            public void run() {
                playersSavingShops.add(player);
                saveShopsDriver(player);
            }
        }.runTaskLaterAsynchronously(plugin, 20L);
    }

    private void saveShopsDriver(UUID player) {
        try {
            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            if (!fileDirectory.exists() && !fileDirectory.mkdir()) {
                return;
            }
            File currentFile;
            if (player.equals(adminUUID)) {
                currentFile = new File(fileDirectory, "admin.yml");
            } else {
                String owner = Bukkit.getOfflinePlayer(player).getName();
                currentFile = new File(fileDirectory, owner + " (" + player.toString() + ").yml");
            }
            String owner = currentFile.getName().substring(0, currentFile.getName().length() - 4);
            if (currentFile.exists()) {
                currentFile.delete();
            }
            currentFile.createNewFile();
            YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);
            List<AbstractShop> shopList = getShops(player);
            if (shopList.isEmpty()) {
                currentFile.delete();
                playersSavingShops.remove(player);
                return;
            }
            int shopNumber = 1;
            for (AbstractShop shop : shopList) {
                if (!shop.getOwnerUUID().equals(player)) {
                    continue;
                }
                if (!shop.isInitialized()) {
                    continue;
                }
                config.set("shops." + owner + "." + shopNumber + ".location", locationToString(shop.getSignLocation()));
                config.set("shops." + owner + "." + shopNumber + ".price", shop.getPrice());
                if (shop.getType() == ShopType.COMBO) {
                    config.set("shops." + owner + "." + shopNumber + ".priceSell", ((ComboShop) shop).getPriceSell());
                }
                config.set("shops." + owner + "." + shopNumber + ".amount", shop.getAmount());
                String type = "";
                if (shop.isAdmin()) {
                    type = "admin ";
                }
                type += shop.getType().toString();
                config.set("shops." + owner + "." + shopNumber + ".type", type);
                if (shop.getDisplay().getType() != null) {
                    config.set("shops." + owner + "." + shopNumber + ".displayType", shop.getDisplay().getType().toString());
                } else {
                    config.set("shops." + owner + "." + shopNumber + ".displayType", null);
                }
                ItemStack itemStack = shop.getItemStack();
                itemStack.setAmount(1);
                if (shop.getType() == ShopType.GAMBLE) {
                    itemStack = new ItemStack(Material.AIR);
                }
                config.set("shops." + owner + "." + shopNumber + ".item", itemStack);
                if (shop.getType() == ShopType.BARTER) {
                    ItemStack barterItemStack = shop.getSecondaryItemStack();
                    barterItemStack.setAmount(1);
                    config.set("shops." + owner + "." + shopNumber + ".itemBarter", barterItemStack);
                }
                ++shopNumber;
            }
            config.save(currentFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        playersSavingShops.remove(player);
    }

    public void saveAllShops() {
        HashMap<UUID, Boolean> allPlayersWithShops = new HashMap<>();
        for (AbstractShop shop : allShops.values()) {
            allPlayersWithShops.put(shop.getOwnerUUID(), true);
        }
        for (UUID player : allPlayersWithShops.keySet()) {
            saveShops(player);
        }
    }

    public void loadShops() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            return;
        }
        File shopFile = new File(fileDirectory, "shops.yml");
        if (shopFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
            backwardsCompatibleLoadShopsFromConfig(config);
        } else {
            File[] files = fileDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".yml") && !file.getName().contains("enderchests") && !file.getName().contains("itemCurrency") && !file.getName().contains("gambleDisplay")) {
                        YamlConfiguration config2 = YamlConfiguration.loadConfiguration(file);
                        loadShopsFromConfig(config2);
                    }
                }
            }
        }
        new BukkitRunnable() {
            public void run() {
                refreshShopDisplays();
            }
        }.runTaskLater(plugin, 20L);
    }

    private void loadShopsFromConfig(YamlConfiguration config) {
        if (config.getConfigurationSection("shops") == null) {
            return;
        }
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);
        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                try {
                    Block b = signLoc.getBlock();
                    if (!Tag.WALL_SIGNS.isTagged(b.getType())) {
                        continue;
                    }
                    UUID owner;
                    if (shopOwner.equals("admin")) {
                        owner = getAdminUUID();
                    } else {
                        owner = uidFromString(shopOwner);
                    }
                    String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                    double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                    double priceSell = 0.0;
                    if (config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell") != null) {
                        priceSell = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell"));
                    }
                    int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));
                    boolean isAdmin = false;
                    if (type.contains("admin")) {
                        isAdmin = true;
                    }
                    ShopType shopType = typeFromString(type);
                    ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                    if (shopType == ShopType.GAMBLE) {
                        itemStack = plugin.getGambleDisplayItem();
                    }
                    AbstractShop shop = AbstractShop.create(signLoc, owner, price, priceSell, amount, isAdmin, shopType);
                    if (!isChest(shop.getChestLocation().getBlock())) {
                        continue;
                    }
                    shop.setItemStack(itemStack);
                    if (shop.getType() == ShopType.BARTER) {
                        ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
                        shop.setSecondaryItemStack(barterItemStack);
                    }
                    addShop(shop);
                    AbstractShop finalShop = shop;
                    String displayType = config.getString("shops." + shopOwner + "." + shopNumber + ".displayType");
                    new BukkitRunnable() {
                        public void run() {
                            if (finalShop != null && displayType != null) {
                                finalShop.getDisplay().setType(DisplayType.valueOf(displayType));
                            }
                        }
                    }.runTaskLater(plugin, 2L);
                } catch (NullPointerException ignore) {
                }
            }
        }
    }

    private void backwardsCompatibleLoadShopsFromConfig(YamlConfiguration config) {
        if (config.getConfigurationSection("shops") == null) {
            return;
        }
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);
        boolean loadByLegacyConfig = false;
        Iterator<String> iterator = allShopOwners.iterator();
        if (iterator.hasNext()) {
            String shopOwner = iterator.next();
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            Iterator<String> iterator2 = allShopNumbers.iterator();
            if (iterator2.hasNext()) {
                String shopNumber = iterator2.next();
                ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                if (itemStack == null) {
                    loadByLegacyConfig = true;
                }
            }
        }
        if (loadByLegacyConfig) {
            loadShopsFromLegacyConfig(config);
        } else {
            loadShopsFromConfig(config);
        }
        saveAllShops();
    }

    public UUID getAdminUUID() {
        return adminUUID;
    }

    private void loadShopsFromLegacyConfig(YamlConfiguration config) {
        if (config.getConfigurationSection("shops") == null) {
            return;
        }
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);
        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                Block b = signLoc.getBlock();
                if (Tag.WALL_SIGNS.isTagged(b.getType())) {
                    UUID owner = uidFromString(shopOwner);
                    double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                    int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));
                    String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                    boolean isAdmin = false;
                    if (type.contains("admin")) {
                        isAdmin = true;
                    }
                    ShopType shopType = typeFromString(type);
                    MaterialData itemData = dataFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.data"));
                    ItemStack itemStack = new ItemStack(itemData.getItemType());
                    itemStack.setData(itemData);
                    short itemDurability = (short) config.getInt("shops." + shopOwner + "." + shopNumber + ".item.durability");
                    InventoryUtils.setDurability(itemStack, itemDurability);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if (itemMeta instanceof LeatherArmorMeta && config.getString("shops." + shopOwner + "." + shopNumber + ".item.color") != null) {
                        ((LeatherArmorMeta) itemMeta).setColor(Color.fromRGB(config.getInt("shops." + shopOwner + "." + shopNumber + ".item.color")));
                    }
                    String itemName = config.getString("shops." + shopOwner + "." + shopNumber + ".item.name");
                    if (!itemName.isEmpty()) {
                        itemMeta.setDisplayName(config.getString("shops." + shopOwner + "." + shopNumber + ".item.name"));
                    }
                    List<String> itemLore = loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.lore"));
                    if (itemLore.size() > 1) {
                        itemMeta.setLore(loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.lore")));
                    }
                    itemStack.setItemMeta(itemMeta);
                    itemStack.addUnsafeEnchantments(enchantmentsFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.enchantments")));
                    if (shopType == ShopType.BARTER) {
                        MaterialData barterItemData = dataFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.data"));
                        ItemStack barterItemStack = new ItemStack(barterItemData.getItemType());
                        barterItemStack.setData(barterItemData);
                        short barterItemDurability = (short) config.getInt("shops." + shopOwner + "." + shopNumber + ".itemBarter.durability");
                        InventoryUtils.setDurability(barterItemStack, barterItemDurability);
                        ItemMeta barterItemMeta = barterItemStack.getItemMeta();
                        if (itemMeta instanceof LeatherArmorMeta && config.getString("shops." + shopOwner + "." + shopNumber + ".item.color") != null) {
                            ((LeatherArmorMeta) itemMeta).setColor(Color.fromRGB(config.getInt("shops." + shopOwner + "." + shopNumber + ".item.color")));
                        }
                        String barterItemName = config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.name");
                        if (!barterItemName.isEmpty()) {
                            barterItemMeta.setDisplayName(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.name"));
                        }
                        List<String> barterItemLore = loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.lore"));
                        if (barterItemLore.size() > 1) {
                            barterItemMeta.setLore(loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.lore")));
                        }
                        barterItemStack.setItemMeta(barterItemMeta);
                        barterItemStack.addUnsafeEnchantments(enchantmentsFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.enchantments")));
                    }
                    AbstractShop shop = AbstractShop.create(signLoc, owner, price, 0.0, amount, isAdmin, shopType);
                    shop.setItemStack(itemStack);
                    if (shop.isAdmin()) {
                        shop.setOwner(plugin.getShopHandler().getAdminUUID());
                    }
                    shop.updateSign();
                    addShop(shop);
                }
            }
        }
    }

    private String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(plugin.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    private UUID uidFromString(String ownerString) {
        int index = ownerString.indexOf("(");
        String uidString = ownerString.substring(index + 1, ownerString.length() - 1);
        return UUID.fromString(uidString);
    }

    private ShopType typeFromString(String typeString) {
        if (typeString.contains("sell")) {
            return ShopType.SELL;
        }
        if (typeString.contains("buy")) {
            return ShopType.BUY;
        }
        if (typeString.contains("barter")) {
            return ShopType.BARTER;
        }
        if (typeString.contains("combo")) {
            return ShopType.COMBO;
        }
        return ShopType.GAMBLE;
    }

    public boolean isChest(Block b) {
        return b.getState() instanceof ShulkerBox || shopMaterials.contains(b.getType());
    }

    private List<String> loreFromString(String loreString) {
        loreString = loreString.substring(1, loreString.length() - 1);
        String[] loreParts = loreString.split(", ");
        return Arrays.asList(loreParts);
    }

    private HashMap<Enchantment, Integer> enchantmentsFromString(String enchantments) {
        HashMap<Enchantment, Integer> enchants = new HashMap<>();
        enchantments = enchantments.substring(1, enchantments.length() - 1);
        if (enchantments.isEmpty()) {
            return enchants;
        }
        String[] enchantParts = enchantments.split(", ");
        for (String whole : enchantParts) {
            String[] pair = whole.split("=");
            enchants.put(Enchantment.getByName(pair[0]), Integer.parseInt(pair[1]));
        }
        return enchants;
    }

    private MaterialData dataFromString(String dataString) {
        int index = dataString.indexOf("(");
        String materialString = dataString.substring(0, index);
        Material m = Material.getMaterial(materialString);
        int data = Integer.parseInt(dataString.substring(index + 1, dataString.length() - 1));
        return new MaterialData(m, (byte) data);
    }
}
