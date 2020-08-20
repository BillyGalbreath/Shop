package com.snowgears.shop.display;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.util.DisplayUtil;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Display {
    private final DisplayType[] cycle = {DisplayType.NONE, DisplayType.ITEM, DisplayType.GLASS_CASE, DisplayType.LARGE_ITEM};
    private final List<Entity> entities = new ArrayList<>();
    private final Location shopSignLocation;
    private DisplayType type;

    public Display(Location shopSignLocation) {
        this.shopSignLocation = shopSignLocation;
    }

    public void spawn() {
        remove();
        Random random = new Random();

        AbstractShop shop = getShop();

        if (shop.getItemStack() == null) {
            return;
        }

        //define the initial display item
        ItemStack item = shop.getItemStack().clone();
        item.setAmount(1);
        ItemMeta sellMeta = item.getItemMeta();
        sellMeta.setDisplayName(generateDisplayName(random));
        item.setItemMeta(sellMeta);

        DisplayType displayType = this.type;
        if (displayType == null) {
            displayType = Shop.getPlugin().getDisplayType();
        }

        //two display entities on the chest
        if (shop.getType() == ShopType.BARTER) {
            if (shop.getSecondaryItemStack() == null) {
                return;
            }

            //define the barter display item
            ItemStack barterItem = shop.getSecondaryItemStack().clone();
            barterItem.setAmount(1);
            ItemMeta buyMeta = barterItem.getItemMeta();
            buyMeta.setDisplayName(generateDisplayName(random)); // stop item stacking and aid in searching
            barterItem.setItemMeta(buyMeta);

            switch (displayType) {
                case NONE:
                    //do nothing
                    break;
                case ITEM:
                    //Drop initial display item
                    Item i1 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    i1.setVelocity(new Vector(0, 0.1, 0));
                    i1.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(i1);

                    //Drop the barter display item
                    Item i2 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(true), barterItem);
                    i2.setVelocity(new Vector(0, 0.1, 0));
                    i2.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(i2);
                    break;
                case LARGE_ITEM:
                    //put first large display down
                    Location leftLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    leftLoc.add(getLargeItemBarterOffset(false));
                    ArmorStand stand = DisplayUtil.createDisplay(item, leftLoc, shop.getFacing());
                    stand.setCustomName(generateDisplayName(random));
                    stand.setCustomNameVisible(false);
                    entities.add(stand);

                    //put first large display down
                    Location rightLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    rightLoc.add(getLargeItemBarterOffset(true));
                    ArmorStand stand2 = DisplayUtil.createDisplay(barterItem, rightLoc, shop.getFacing());
                    stand2.setCustomName(generateDisplayName(random));
                    stand2.setCustomNameVisible(false);
                    entities.add(stand2);
                    break;
                case GLASS_CASE:
                    //put the extra large glass casing down
                    Location caseLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    caseLoc.add(0, -0.74, 0);
                    ArmorStand caseStand = DisplayUtil.createDisplay(new ItemStack(Material.GLASS), caseLoc, shop.getFacing());
                    caseStand.setSmall(false);
                    caseStand.setCustomName(generateDisplayName(random));
                    caseStand.setCustomNameVisible(false);
                    entities.add(caseStand);

                    //Drop initial display item
                    Item item1 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    item1.setVelocity(new Vector(0, 0.1, 0));
                    item1.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(item1);

                    //Drop the barter display item
                    Item item2 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(true), barterItem);
                    item2.setVelocity(new Vector(0, 0.1, 0));
                    item2.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(item2);
                    break;
            }
        }
        //one display entity on the chest
        else {
            switch (displayType) {
                case NONE:
                    //do nothing
                    break;
                case ITEM:
                    Item i = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    i.setVelocity(new Vector(0, 0.1, 0));
                    i.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(i);
                    break;
                case LARGE_ITEM:
                    ArmorStand stand = DisplayUtil.createDisplay(item, shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation(), shop.getFacing());
                    stand.setCustomName(generateDisplayName(random));
                    stand.setCustomNameVisible(false);
                    entities.add(stand);
                    break;
                case GLASS_CASE:
                    //put the extra large glass casing down
                    Location caseLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    caseLoc.add(0, -0.74, 0);
                    ArmorStand caseStand = DisplayUtil.createDisplay(new ItemStack(Material.GLASS), caseLoc, shop.getFacing());
                    caseStand.setSmall(false);
                    caseStand.setCustomName(generateDisplayName(random));
                    caseStand.setCustomNameVisible(false);
                    entities.add(caseStand);

                    //drop the display item in the glass case
                    Item caseDisplayItem = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    caseDisplayItem.setVelocity(new Vector(0, 0.1, 0));
                    caseDisplayItem.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    entities.add(caseDisplayItem);
                    break;
            }
        }
        shop.updateSign();
    }

    public DisplayType getType() {
        return this.type;
    }

    public AbstractShop getShop() {
        return Shop.getPlugin().getShopHandler().getShop(shopSignLocation);
    }

    public void setType(DisplayType type) {
        DisplayType oldType = this.type;
        if (oldType == DisplayType.NONE) {
            //make sure there is room above the shop for the display
            Block aboveShop = getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                return;
            }
        }
        this.type = type;
        spawn();
    }

    public void cycleType() {
        DisplayType displayType = this.type;
        if (displayType == null) {
            displayType = Shop.getPlugin().getDisplayType();
        }
        if (displayType == DisplayType.NONE) {
            //make sure there is room above the shop for the display
            Block aboveShop = getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                return;
            }
        }
        int index = 0;
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == displayType) {
                index = i + 1;
            }
        }
        if (index >= cycle.length) {
            index = 0;
        }
        setType(cycle[index]);
        Shop.getPlugin().getShopHandler().saveShops(getShop().getOwnerUUID());
    }

    public void remove() {
        AbstractShop shop = getShop();

        for (Entity displayEntity : entities) {
            displayEntity.remove();
        }
        entities.clear();

        for (Entity entity : shop.getChestLocation().getChunk().getEntities()) {
            if (isDisplay(entity)) {
                AbstractShop s = getShop(entity);
                //remove any displays that are left over but still belong to the same shop
                if (s != null && s.getSignLocation().equals(shop.getSignLocation())) {
                    entity.remove();
                }
            }
        }
    }

    private Location getItemDropLocation(boolean isBarterItem) {
        AbstractShop shop = getShop();
        //calculate which x,z to drop items at depending on direction of the shop sign
        double dropY = 1.2;
        double dropX = 0.5;
        double dropZ = 0.5;
        if (shop.getType() == ShopType.BARTER) {
            Directional shopSign = (Directional) shop.getSignLocation().getBlock().getState().getBlockData();
            switch (shopSign.getFacing()) {
                case NORTH:
                    dropX = isBarterItem ? 0.3 : 0.7;
                    break;
                case EAST:
                    dropZ = isBarterItem ? 0.3 : 0.7;
                    break;
                case SOUTH:
                    dropX = isBarterItem ? 0.7 : 0.3;
                    break;
                case WEST:
                    dropZ = isBarterItem ? 0.7 : 0.3;
                    break;
                default:
                    dropX = 0.5;
                    dropZ = 0.5;
                    break;
            }
        }
        return shop.getChestLocation().clone().add(dropX, dropY, dropZ);
    }

    private Vector getLargeItemBarterOffset(boolean isBarterItem) {
        AbstractShop shop = getShop();
        Vector offset = new Vector(0, 0, 0);
        double space = 0.24;
        if (shop.getType() == ShopType.BARTER) {
            Directional shopSign = (Directional) shop.getSignLocation().getBlock().getState().getBlockData();
            switch (shopSign.getFacing()) {
                case NORTH:
                case EAST:
                    offset.setZ(isBarterItem ? -space : space);
                    break;
                case SOUTH:
                case WEST:
                    offset.setZ(isBarterItem ? space : -space);
                    break;
            }
        }
        return offset;
    }

    public static boolean isDisplay(Entity entity) {
        if (entity.getType() == EntityType.DROPPED_ITEM) {
            ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
            return itemMeta != null && UtilMethods.containsLocation(itemMeta.getDisplayName());
        } else if (entity.getType() == EntityType.ARMOR_STAND) {
            return UtilMethods.containsLocation(entity.getCustomName());
        }
        return false;
    }

    public static AbstractShop getShop(Entity display) {
        if (display == null) {
            return null;
        }
        String name = null;
        if (display.getType() == EntityType.DROPPED_ITEM) {
            ItemMeta itemMeta = ((Item) display).getItemStack().getItemMeta();
            name = itemMeta.getDisplayName();
        }
        if (display.getType() == EntityType.ARMOR_STAND) {
            name = display.getCustomName();
        }

        if (!UtilMethods.containsLocation(name)) {
            return null;
        }

        String locString = name.substring(name.indexOf('{') + 1, name.indexOf('}'));
        String[] parts = locString.split(",");
        Location location = new Location(display.getWorld(), Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        return Shop.getPlugin().getShopHandler().getShop(location);
    }

    private String generateDisplayName(Random random) {
        return "***{" + shopSignLocation.getBlockX() + "," + shopSignLocation.getBlockY() + "," + shopSignLocation.getBlockZ() + "}"; //+random.nextInt(1000);
    }
}
