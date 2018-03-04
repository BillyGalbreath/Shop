package com.snowgears.shop.handler;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.ComboShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.*;
import org.bukkit.block.*;
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
import org.bukkit.material.Sign;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
import java.util.*;
import java.util.Comparator;


public class ShopHandler {

    public Shop plugin = Shop.getPlugin();

    private HashMap<UUID, List<Location>> playerShops = new HashMap<>();
    private HashMap<Location, AbstractShop> allShops = new HashMap<>();
    private ArrayList<Material> shopMaterials = new ArrayList<Material>();
    private UUID adminUUID;

    private ArrayList<UUID> playersSavingShops = new ArrayList<>();

    public ShopHandler(Shop instance) {
        plugin = instance;

        shopMaterials.add(Material.CHEST);
        shopMaterials.add(Material.TRAPPED_CHEST);
        if(plugin.useEnderChests())
            shopMaterials.add(Material.ENDER_CHEST);

        adminUUID = UUID.randomUUID();

        new BukkitRunnable() {
            @Override
            public void run() {
                loadShops();
            }
        }.runTaskLater(this.plugin, 10);
    }

    public AbstractShop getShop(Location loc) {
        return allShops.get(loc);
    }

    public AbstractShop getShopByChest(Block shopChest) {

        try {
            if(shopChest.getState() instanceof ShulkerBox){
                BlockFace[] directions = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
                AbstractShop shop = null;
                for(BlockFace direction : directions){
                    shop = this.getShop(shopChest.getRelative(direction).getLocation());
                    if(shop != null) {
                        //make sure the shop sign you found is actually attached to the correct shop
                        if(shop.getChestLocation().equals(shopChest.getLocation()))
                            return shop;
                    }
                }
                return null;
            }
        } catch (NoClassDefFoundError e) {}

        if (this.isChest(shopChest)) {
            BlockFace chestFacing = UtilMethods.getDirectionOfChest(shopChest);

            ArrayList<Block> chestBlocks = new ArrayList<>();
            chestBlocks.add(shopChest);

            InventoryHolder ih = null;
            if(shopChest.getState() instanceof Chest) {
                Chest chest = (Chest) shopChest.getState();
                ih = chest.getInventory().getHolder();

                if (ih instanceof DoubleChest) {
                    DoubleChest dc = (DoubleChest) ih;
                    Chest leftChest = (Chest) dc.getLeftSide();
                    Chest rightChest = (Chest) dc.getRightSide();
                    if (chest.getLocation().equals(leftChest.getLocation()))
                        chestBlocks.add(rightChest.getBlock());
                    else
                        chestBlocks.add(leftChest.getBlock());
                }
            }

            for (Block chestBlock : chestBlocks) {
                Block signBlock = chestBlock.getRelative(chestFacing);
                if (signBlock.getType() == Material.WALL_SIGN) {
                    Sign sign = (Sign) signBlock.getState().getData();
                    if (chestFacing == sign.getFacing()) {
                        AbstractShop shop = this.getShop(signBlock.getLocation());
                        if (shop != null)
                            return shop;
                    }
                } else if(!(ih instanceof DoubleChest)){
                    AbstractShop shop = this.getShop(signBlock.getLocation());
                    //delete the shop if it doesn't have a sign
                    if (shop != null)
                        shop.delete();
                }
            }
        }
        return null;
    }

    public AbstractShop getShopNearBlock(Block block){
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for(BlockFace face : faces){
            if(this.isChest(block.getRelative(face))){
                Block shopChest = block.getRelative(face);
                for(BlockFace newFace : faces){
                    if(shopChest.getRelative(newFace).getType() == Material.WALL_SIGN){
                        AbstractShop shop = getShop(shopChest.getRelative(newFace).getLocation());
                        if(shop != null)
                            return shop;
                    }
                }
            }
        }
        return null;
    }

    public void addShop(AbstractShop shop) {

        //this is to remove a bug that caused one shop to be saved to multiple files at one point
        AbstractShop s = getShop(shop.getSignLocation());
        if(s != null) {
            return;
        }
        allShops.put(shop.getSignLocation(), shop);

        List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
        if(!shopLocations.contains(shop.getSignLocation())) {
            shopLocations.add(shop.getSignLocation());
            playerShops.put(shop.getOwnerUUID(), shopLocations);
        }
    }

    //This method should only be used by AbstractShop object to delete
    public boolean removeShop(AbstractShop shop) {
        if (allShops.containsKey(shop.getSignLocation())) {
            allShops.remove(shop.getSignLocation());
        }
        if(playerShops.containsKey(shop.getOwnerUUID())){
            List<Location> shopLocations = getShopLocations(shop.getOwnerUUID());
            if(shopLocations.contains(shop.getSignLocation())) {
                shopLocations.remove(shop.getSignLocation());
                playerShops.put(shop.getOwnerUUID(), shopLocations);
            }
        }

        return false;
    }

    public List<AbstractShop> getShops(UUID player){
        List<AbstractShop> shops = new ArrayList<>();
        for(Location shopSign : getShopLocations(player)){
            AbstractShop shop = getShop(shopSign);
            if(shop != null)
                shops.add(shop);
        }
        return shops;
    }

    public List<OfflinePlayer> getShopOwners(){
        ArrayList<OfflinePlayer> owners = new ArrayList<>();
        for(UUID player : playerShops.keySet()) {
            owners.add(Bukkit.getOfflinePlayer(player));
        }
        return owners;
    }

    private List<Location> getShopLocations(UUID player){
        List<Location> shopLocations;
        if(playerShops.containsKey(player)) {
            shopLocations = playerShops.get(player);
        }
        else
            shopLocations = new ArrayList<>();
        return shopLocations;
    }

    public int getNumberOfShops() {
        return allShops.size();
    }

    public int getNumberOfShops(Player player) {
        return getShopLocations(player.getUniqueId()).size();
    }

    private ArrayList<AbstractShop> orderedShopList() {
        ArrayList<AbstractShop> list = new ArrayList<AbstractShop>(allShops.values());
        Collections.sort(list, new Comparator<AbstractShop>() {
            @Override
            public int compare(AbstractShop o1, AbstractShop o2) {
                if(o1 == null || o2 == null)
                    return 0;
                //could have something to do with switching between online and offline mode
                return o1.getOwnerName().toLowerCase().compareTo(o2.getOwnerName().toLowerCase());
            }
        });
        return list;
    }

    public void refreshShopDisplays() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if(Display.isDisplay(entity)){
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

    public void saveShops(final UUID player){
        if(playersSavingShops.contains(player))
            return;

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        scheduler.scheduleAsyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                playersSavingShops.add(player);
                saveShopsDriver(player);
            }
        }, 20L);
    }

    private void saveShopsDriver(UUID player){
        try {

            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            //UtilMethods.deleteDirectory(fileDirectory);
            if (!fileDirectory.exists())
                fileDirectory.mkdir();

            String owner = null;
            File currentFile = null;
            if(player.equals(adminUUID)) {
                currentFile = new File(fileDirectory + "/admin.yml");
            }
            else {
                owner = Bukkit.getOfflinePlayer(player).getName();
                currentFile = new File(fileDirectory + "/" + owner + " (" + player.toString() + ").yml");
            }
            owner = currentFile.getName().substring(0, currentFile.getName().length()-4); //remove .yml

            if (!currentFile.exists()) // file doesn't exist
                currentFile.createNewFile();
            else{
                currentFile.delete();
                currentFile.createNewFile();
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);

            List<AbstractShop> shopList = getShops(player);
            if (shopList.isEmpty()) {
                currentFile.delete();
                if(playersSavingShops.contains(player)){
                    playersSavingShops.remove(player);
                }
                return;
            }

            int shopNumber = 1;
            for (AbstractShop shop : shopList) {

                //this is to remove a bug that caused one shop to be saved to multiple files at one point
                if(!shop.getOwnerUUID().equals(player))
                    continue;

                //don't save shops that are not initialized with items
                if (shop.isInitialized()) {
                    config.set("shops." + owner + "." + shopNumber + ".location", locationToString(shop.getSignLocation()));
                    config.set("shops." + owner + "." + shopNumber + ".price", shop.getPrice());
                    if(shop.getType() == ShopType.COMBO){
                        config.set("shops." + owner + "." + shopNumber + ".priceSell", ((ComboShop)shop).getPriceSell());
                    }
                    config.set("shops." + owner + "." + shopNumber + ".amount", shop.getAmount());
                    String type = "";
                    if (shop.isAdmin())
                        type = "admin ";
                    type = type + shop.getType().toString();
                    config.set("shops." + owner + "." + shopNumber + ".type", type);
                    if(shop.getDisplay().getType() != null) {
                        config.set("shops." + owner + "." + shopNumber + ".displayType", shop.getDisplay().getType().toString());
                    }
                    else{ //not sure why I have to do this but if I don't it will be set to LARGE_ITEM for some reason (I cannot find right now)
                        config.set("shops." + owner + "." + shopNumber + ".displayType", null);
                    }

                    ItemStack itemStack = shop.getItemStack();
                    itemStack.setAmount(1);
                    if(shop.getType() == ShopType.GAMBLE)
                        itemStack = new ItemStack(Material.AIR);
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
        } catch (Exception e){
            e.printStackTrace();
        }

        if(playersSavingShops.contains(player)){
            playersSavingShops.remove(player);
        }
    }

    public void saveAllShops() {
        HashMap<UUID, Boolean> allPlayersWithShops = new HashMap<>();
        for(AbstractShop shop : allShops.values()){
            allPlayersWithShops.put(shop.getOwnerUUID(), true);
        }

        for(UUID player : allPlayersWithShops.keySet()){
            saveShops(player);
        }
    }

    public void loadShops() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists())
            return;
        File shopFile = new File(fileDirectory + "/shops.yml");
        if (shopFile.exists()){
            YamlConfiguration config = YamlConfiguration.loadConfiguration(shopFile);
            backwardsCompatibleLoadShopsFromConfig(config);
        }
        else{
            // load all the yml files from the data directory
            for (File file : fileDirectory.listFiles()) {
                if (file.isFile()) {
                    if(file.getName().endsWith(".yml")
                            && !file.getName().contains("enderchests")
                            && !file.getName().contains("itemCurrency")
                            && !file.getName().contains("gambleDisplay")) {
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                        loadShopsFromConfig(config);
                    }
                }
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshShopDisplays();
            }
        }.runTaskLater(this.plugin, 20);
    }


    private void loadShopsFromConfig(YamlConfiguration config) {
        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                if(signLoc != null) {
                    try {
                        Block b = signLoc.getBlock();
                        if (b.getType() == Material.WALL_SIGN) {
                            org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData();
                            //Location loc = b.getRelative(sign.getAttachedFace()).getLocation();

                            UUID owner;
                            if (shopOwner.equals("admin"))
                                owner = this.getAdminUUID();
                            else
                                owner = uidFromString(shopOwner);

                            String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                            double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                            double priceSell = 0;
                            if (config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell") != null) {
                                priceSell = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".priceSell"));
                            }
                            int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));

                            boolean isAdmin = false;
                            if (type.contains("admin"))
                                isAdmin = true;
                            ShopType shopType = typeFromString(type);

                            ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                            if (shopType == ShopType.GAMBLE) {
                                itemStack = plugin.getGambleDisplayItem();
                            }

                            AbstractShop shop = AbstractShop.create(signLoc, owner, price, priceSell, amount, isAdmin, shopType);

                            if (this.isChest(shop.getChestLocation().getBlock())) {

                                shop.setItemStack(itemStack);
                                if (shop.getType() == ShopType.BARTER) {
                                    ItemStack barterItemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".itemBarter");
                                    shop.setSecondaryItemStack(barterItemStack);
                                }

                                this.addShop(shop);

                                //final is necessary for use inside the BukkitRunnable class
                                final AbstractShop finalShop = shop;

                                final String displayType = config.getString("shops." + shopOwner + "." + shopNumber + ".displayType");
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {

                                        if (finalShop != null && displayType != null) {
                                            finalShop.getDisplay().setType(DisplayType.valueOf(displayType));
                                        }
                                    }
                                }.runTaskLater(this.plugin, 2);
                            }
                        }
                    } catch (NullPointerException e) {}
                }
            }
        }
    }

    //==============================================================================//
    //            OLD WAY OF LOADING SHOPS FROM ONE CONFIG FOR TRANSFERRING         //
    //==============================================================================//


    private void backwardsCompatibleLoadShopsFromConfig(YamlConfiguration config){
        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        boolean loadByLegacyConfig = false;
        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                ItemStack itemStack = config.getItemStack("shops." + shopOwner + "." + shopNumber + ".item");
                if (itemStack == null)
                    loadByLegacyConfig = true;
                break;
            }
            break;
        }

        if(loadByLegacyConfig) {
            loadShopsFromLegacyConfig(config); //load as old
            saveAllShops(); //save as new
        }
        else {
            //load old config normally
            loadShopsFromConfig(config);
            saveAllShops(); //save as new
        }
    }

    public UUID getAdminUUID(){
        return adminUUID;
    }


    //==============================================================================//
    //            LEGACY WAY OF LOADING SHOPS FROM CONFIG FOR TRANSFERRING          //
    //==============================================================================//

    private void loadShopsFromLegacyConfig(YamlConfiguration config) {

        if (config.getConfigurationSection("shops") == null)
            return;
        Set<String> allShopOwners = config.getConfigurationSection("shops").getKeys(false);

        for (String shopOwner : allShopOwners) {
            Set<String> allShopNumbers = config.getConfigurationSection("shops." + shopOwner).getKeys(false);
            for (String shopNumber : allShopNumbers) {
                Location signLoc = locationFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".location"));
                Block b = signLoc.getBlock();
                if (b.getType() == Material.WALL_SIGN) {
                    org.bukkit.material.Sign sign = (org.bukkit.material.Sign) b.getState().getData();
                    Location loc = b.getRelative(sign.getAttachedFace()).getLocation();
                    UUID owner = uidFromString(shopOwner);
                    double price = Double.parseDouble(config.getString("shops." + shopOwner + "." + shopNumber + ".price"));
                    int amount = Integer.parseInt(config.getString("shops." + shopOwner + "." + shopNumber + ".amount"));
                    String type = config.getString("shops." + shopOwner + "." + shopNumber + ".type");
                    boolean isAdmin = false;
                    if (type.contains("admin"))
                        isAdmin = true;
                    ShopType shopType = typeFromString(type);

                    MaterialData itemData = dataFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.data"));
                    ItemStack itemStack = new ItemStack(itemData.getItemType());
                    itemStack.setData(itemData);
                    short itemDurability = (short) (config.getInt("shops." + shopOwner + "." + shopNumber + ".item.durability"));
                    itemStack.setDurability(itemDurability);
                    ItemMeta itemMeta = itemStack.getItemMeta();
                    if(itemMeta instanceof LeatherArmorMeta){
                        if(config.getString("shops." + shopOwner + "." + shopNumber + ".item.color") != null)
                            ((LeatherArmorMeta)itemMeta).setColor(Color.fromRGB(config.getInt("shops." + shopOwner + "." + shopNumber + ".item.color")));
                    }
                    String itemName = config.getString("shops." + shopOwner + "." + shopNumber + ".item.name");
                    if (!itemName.isEmpty())
                        itemMeta.setDisplayName(config.getString("shops." + shopOwner + "." + shopNumber + ".item.name"));
                    List<String> itemLore = loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.lore"));
                    if (itemLore.size() > 1)
                        itemMeta.setLore(loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.lore")));
                    itemStack.setItemMeta(itemMeta);
                    itemStack.addUnsafeEnchantments(enchantmentsFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".item.enchantments")));

                    ItemStack barterItemStack = null;
                    if (shopType == ShopType.BARTER) {
                        MaterialData barterItemData = dataFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.data"));
                        barterItemStack = new ItemStack(barterItemData.getItemType());
                        barterItemStack.setData(barterItemData);
                        short barterItemDurability = (short) (config.getInt("shops." + shopOwner + "." + shopNumber + ".itemBarter.durability"));
                        barterItemStack.setDurability(barterItemDurability);
                        ItemMeta barterItemMeta = barterItemStack.getItemMeta();
                        if(itemMeta instanceof LeatherArmorMeta){
                            if(config.getString("shops." + shopOwner + "." + shopNumber + ".item.color") != null)
                                ((LeatherArmorMeta)itemMeta).setColor(Color.fromRGB(config.getInt("shops." + shopOwner + "." + shopNumber + ".item.color")));
                        }
                        String barterItemName = config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.name");
                        if (!barterItemName.isEmpty())
                            barterItemMeta.setDisplayName(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.name"));
                        List<String> barterItemLore = loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.lore"));
                        if (barterItemLore.size() > 1)
                            barterItemMeta.setLore(loreFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.lore")));
                        barterItemStack.setItemMeta(barterItemMeta);
                        barterItemStack.addUnsafeEnchantments(enchantmentsFromString(config.getString("shops." + shopOwner + "." + shopNumber + ".itemBarter.enchantments")));
                    }

                    AbstractShop shop = AbstractShop.create(signLoc, owner, price, 0, amount, isAdmin, shopType);
                    shop.setItemStack(itemStack);

                    if(shop.isAdmin()){
                        shop.setOwner(plugin.getShopHandler().getAdminUUID());
                    }

                    shop.updateSign();
                    this.addShop(shop);
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
        if (typeString.contains("sell"))
            return ShopType.SELL;
        else if (typeString.contains("buy"))
            return ShopType.BUY;
        else if(typeString.contains("barter"))
            return ShopType.BARTER;
        else if(typeString.contains("combo"))
            return ShopType.COMBO;
        else
            return ShopType.GAMBLE;
    }

    public boolean isChest(Block b){
        try{
            if(b.getState() instanceof ShulkerBox){
                return true;
            }
        } catch (NoClassDefFoundError e) {}
        return shopMaterials.contains(b.getType());
    }

    private List<String> loreFromString(String loreString) {
        loreString = loreString.substring(1, loreString.length() - 1); //get rid of []
        String[] loreParts = loreString.split(", ");
        return Arrays.asList(loreParts);
    }

    private HashMap<Enchantment, Integer> enchantmentsFromString(String enchantments) {
        HashMap<Enchantment, Integer> enchants = new HashMap<Enchantment, Integer>();
        enchantments = enchantments.substring(1, enchantments.length() - 1); //get rid of {}
        if (enchantments.isEmpty())
            return enchants;
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