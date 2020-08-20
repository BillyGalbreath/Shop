package com.snowgears.shop.handler;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.ComboShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class ShopHandler {
    public final Shop plugin;

    private final Map<UUID, List<Location>> playerShops = new HashMap<>();
    private final Map<Location, AbstractShop> allShops = new HashMap<>();
    private final List<Material> shopMaterials = new ArrayList<>();
    private final UUID adminUUID;

    private final List<UUID> playersSavingShops = new ArrayList<>();

    public ShopHandler(Shop instance) {
        this.plugin = instance;

        shopMaterials.add(Material.CHEST);
        shopMaterials.add(Material.TRAPPED_CHEST);
        shopMaterials.add(Material.BARREL);
        if (plugin.useEnderChests()) {
            shopMaterials.add(Material.ENDER_CHEST);
        }

        adminUUID = UUID.randomUUID();

        new BukkitRunnable() {
            @Override
            public void run() {
                loadShops();
            }
        }.runTaskLater(plugin, 10);
    }

    public AbstractShop getShop(Location loc) {
        return allShops.get(loc);
    }

    private static final BlockFace[] DIRECTIONS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    public AbstractShop getShopByChest(Block shopChest) {

        try {
            if (shopChest.getState() instanceof ShulkerBox) {
                AbstractShop shop;
                for (BlockFace direction : DIRECTIONS) {
                    shop = getShop(shopChest.getRelative(direction).getLocation());
                    if (shop != null) {
                        //make sure the shop sign you found is actually attached to the correct shop
                        if (shop.getChestLocation().equals(shopChest.getLocation())) {
                            return shop;
                        }
                    }
                }
                return null;
            }
        } catch (NoClassDefFoundError ignore) {
        }

        if (isChest(shopChest)) {
            BlockFace chestFacing = UtilMethods.getDirectionOfChest(shopChest);

            List<Block> chestBlocks = new ArrayList<>();
            chestBlocks.add(shopChest);

            InventoryHolder ih = null;
            if (shopChest.getState() instanceof Chest) {
                Chest chest = (Chest) shopChest.getState();
                ih = chest.getInventory().getHolder();

                if (ih instanceof DoubleChest) {
                    DoubleChest dc = (DoubleChest) ih;
                    Chest leftChest = (Chest) dc.getLeftSide();
                    Chest rightChest = (Chest) dc.getRightSide();
                    if (leftChest != null) {
                        if (chest.getLocation().equals(leftChest.getLocation())) {
                            if (rightChest != null) {
                                chestBlocks.add(rightChest.getBlock());
                            }
                        } else {
                            chestBlocks.add(leftChest.getBlock());
                        }
                    }
                }
            }

            for (Block chestBlock : chestBlocks) {
                Block signBlock = chestBlock.getRelative(chestFacing);
                if (signBlock.getBlockData() instanceof WallSign) {
                    WallSign sign = (WallSign) signBlock.getBlockData();
                    if (chestFacing == sign.getFacing()) {
                        AbstractShop shop = getShop(signBlock.getLocation());
                        if (shop != null) {
                            return shop;
                        }
                    }
                } else if (!(ih instanceof DoubleChest)) {
                    AbstractShop shop = getShop(signBlock.getLocation());
                    //delete the shop if it doesn't have a sign
                    if (shop != null) {
                        shop.delete();
                    }
                }
            }
        }
        return null;
    }

    public AbstractShop getShopNearBlock(Block block) {
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            if (isChest(block.getRelative(face))) {
                Block shopChest = block.getRelative(face);
                for (BlockFace newFace : faces) {
                    if (shopChest.getRelative(newFace).getBlockData() instanceof WallSign) {
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

        //this is to remove a bug that caused one shop to be saved to multiple files at one point
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

    //This method should only be used by AbstractShop object to delete
    public boolean removeShop(AbstractShop shop) {
        allShops.remove(shop.getSignLocation());
        if (playerShops.containsKey(shop.getOwnerUUID())) {
            List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
            if (shopLocations.contains(shop.getSignLocation())) {
                shopLocations.remove(shop.getSignLocation());
                playerShops.put(shop.getOwnerUUID(), shopLocations);
            }
        }

        return false;
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
        List<OfflinePlayer> owners = new ArrayList<>();
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

    private List<AbstractShop> orderedShopList() {
        List<AbstractShop> list = new ArrayList<>(allShops.values());
        list.sort((o1, o2) -> {
            if (o1 == null || o2 == null) {
                return 0;
            }
            //could have something to do with switching between online and offline mode
            return o1.getOwnerName().toLowerCase().compareTo(o2.getOwnerName().toLowerCase());
        });
        return list;
    }

    public void refreshShopDisplays() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (Display.isDisplay(entity)) {
                    entity.remove();
                }
                //make to sure to clear items from old version of plugin too
                else if (entity.getType() == EntityType.DROPPED_ITEM) {
                    ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                    if (UtilMethods.stringStartsWithUUID(itemMeta.getDisplayName())) {
                        entity.remove();
                    }
                }
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

        plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            playersSavingShops.add(player);
            saveShopsDriver(player);
        }, 20L);
    }

    private void saveShopsDriver(UUID player) {
        try {

            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            //UtilMethods.deleteDirectory(fileDirectory);
            if (!fileDirectory.exists()) {
                fileDirectory.mkdir();
            }

            String owner;
            File currentFile;
            if (player.equals(adminUUID)) {
                owner = "admin";
                currentFile = new File(fileDirectory + "/admin.yml");
            } else {
                owner = player.toString();
                //currentFile = new File(fileDirectory + "/" + owner + " (" + player.toString() + ").yml");
                currentFile = new File(fileDirectory + "/" + player.toString() + ".yml");
            }
            //owner = currentFile.getName().substring(0, currentFile.getName().length()-4); //remove .yml

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

                //this is to remove a bug that caused one shop to be saved to multiple files at one point
                if (!shop.getOwnerUUID().equals(player)) {
                    continue;
                }

                //don't save shops that are not initialized with items
                if (shop.isInitialized()) {
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
                    type = type + shop.getType().toString();
                    config.set("shops." + owner + "." + shopNumber + ".type", type);
                    if (shop.getDisplay().getType() != null) {
                        config.set("shops." + owner + "." + shopNumber + ".displayType", shop.getDisplay().getType().toString());
                    } else { //not sure why I have to do this but if I don't it will be set to LARGE_ITEM for some reason (I cannot find right now)
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
                    shopNumber++;
                }
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

    public void convertLegacyShopSaves() {
        //save to new format
        saveAllShops();

        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            return;
        }
        File[] listFiles = fileDirectory.listFiles();
        if (listFiles == null) {
            return;
        }
        // load all the yml files from the data directory
        for (File file : listFiles) {
            if (file.isFile()) {
                if (file.getName().endsWith(".yml")
                        && !file.getName().contains("enderchests")
                        && !file.getName().contains("itemCurrency")
                        && !file.getName().contains("gambleDisplay")) {
                    YamlConfiguration.loadConfiguration(file);
                }
            }
        }
    }

    public void loadShops() {
        boolean convertLegacySaves = false;
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists()) {
            return;
        }
        File[] listFiles = fileDirectory.listFiles();
        if (listFiles != null)
            // load all the yml files from the data directory
            for (File file : listFiles) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".yml")
                            && !file.getName().contains("enderchests")
                            && !file.getName().contains("itemCurrency")
                            && !file.getName().contains("gambleDisplay")) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        boolean isLegacyConfig = false;
                        UUID playerUUID;
                        int dotIndex = file.getName().lastIndexOf('.');
                        String fileNameNoExt = file.getName().substring(0, dotIndex); //remove .yml
                        try {
                            //all files are saved as UUID.yml except for admin shops which are admin.yml
                            if (!fileNameNoExt.equals("admin")) {
                                playerUUID = UUID.fromString(fileNameNoExt);
                                //file names are in UUID format. Load from new save files -> ownerUUID.yml
                            } else {
                                playerUUID = adminUUID;
                            }
                        } catch (IllegalArgumentException iae) {
                            //file names are not in UUID format. Load from legacy save files -> ownerName + " (" + ownerUUID + ").yml
                            isLegacyConfig = true;
                            convertLegacySaves = true;
                            playerUUID = uidFromString(fileNameNoExt);
                        }
                        loadShopsFromConfig(config, isLegacyConfig);
                        if (isLegacyConfig) {
                            //save new file
                            saveShops(playerUUID);
                            //delete old file
                            file.delete();
                        }
                    }
                }
            }
        if (convertLegacySaves) {
            convertLegacyShopSaves();
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                refreshShopDisplays();
            }
        }.runTaskLater(plugin, 20);
    }


    private void loadShopsFromConfig(YamlConfiguration config, boolean isLegacy) {
        if (config.getConfigurationSection("shops") == null) {
            return;
        }
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                if (signLoc != null) {
                    try {
                        Block b = signLoc.getBlock();
                        if (b.getBlockData() instanceof WallSign) {

                            UUID owner;
                            if (shopOwner.equals("admin")) {
                                owner = getAdminUUID();
                            } else if (isLegacy) {
                                owner = uidFromString(shopOwner);
                            } else {
                                owner = UUID.fromString(shopOwner);
                            }

                            String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                            double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                            double priceSell = 0;
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

                            if (isChest(shop.getChestLocation().getBlock())) {

                                shop.setItemStack(itemStack);
                                if (shop.getType() == ShopType.BARTER) {
                                    ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
                                    shop.setSecondaryItemStack(barterItemStack);
                                }

                                addShop(shop);

                                //final is necessary for use inside the BukkitRunnable class
                                final AbstractShop finalShop = shop;

                                final String displayType = config.getString("shops." + shopOwner + "." + shopNumber + ".displayType");
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (displayType != null) {
                                            finalShop.getDisplay().setType(DisplayType.valueOf(displayType));
                                        }
                                    }
                                }.runTaskLater(plugin, 2);
                            }
                        }
                    } catch (NullPointerException ignore) {
                    }
                }
            }
        }
    }

    public UUID getAdminUUID() {
        return adminUUID;
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
        } else if (typeString.contains("buy")) {
            return ShopType.BUY;
        } else if (typeString.contains("barter")) {
            return ShopType.BARTER;
        } else if (typeString.contains("combo")) {
            return ShopType.COMBO;
        } else {
            return ShopType.GAMBLE;
        }
    }

    public boolean isChest(Block b) {
        if (b.getState() instanceof ShulkerBox) {
            return true;
        }
        return shopMaterials.contains(b.getType());
    }
}
