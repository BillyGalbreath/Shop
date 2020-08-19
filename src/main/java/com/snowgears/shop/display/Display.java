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

public class Display {
    private final Location shopSignLocation;
    private DisplayType type;
    private final ArrayList<Entity> entities;
    private final DisplayType[] cycle = new DisplayType[]{DisplayType.NONE, DisplayType.ITEM, DisplayType.GLASS_CASE, DisplayType.LARGE_ITEM};

    public Display(Location shopSignLocation) {
        this.shopSignLocation = shopSignLocation;
        this.entities = new ArrayList<>();
    }

    public void spawn() {
        remove();
        AbstractShop shop = getShop();
        if (shop.getItemStack() == null) {
            return;
        }
        ItemStack item = shop.getItemStack().clone();
        item.setAmount(1);
        ItemMeta sellMeta = item.getItemMeta();
        sellMeta.setDisplayName(generateDisplayName());
        item.setItemMeta(sellMeta);
        DisplayType displayType = type;
        if (displayType == null) {
            displayType = Shop.getPlugin().getDisplayType();
        }
        if (shop.getType() == ShopType.BARTER) {
            if (shop.getSecondaryItemStack() == null) {
                return;
            }
            ItemStack barterItem = shop.getSecondaryItemStack().clone();
            barterItem.setAmount(1);
            ItemMeta buyMeta = barterItem.getItemMeta();
            buyMeta.setDisplayName(generateDisplayName());
            barterItem.setItemMeta(buyMeta);
            switch (displayType) {
                case ITEM:
                    Item i1 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    i1.setVelocity(new Vector(0.0, 0.1, 0.0));
                    i1.setPickupDelay(Integer.MAX_VALUE);
                    entities.add(i1);
                    Item i2 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(true), barterItem);
                    i2.setVelocity(new Vector(0.0, 0.1, 0.0));
                    i2.setPickupDelay(Integer.MAX_VALUE);
                    entities.add(i2);
                    break;
                case LARGE_ITEM:
                    Location leftLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    leftLoc.add(getLargeItemBarterOffset(false));
                    ArmorStand stand = DisplayUtil.createDisplay(item, leftLoc, shop.getFacing());
                    stand.setCustomName(generateDisplayName());
                    stand.setCustomNameVisible(false);
                    entities.add(stand);
                    Location rightLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    rightLoc.add(getLargeItemBarterOffset(true));
                    ArmorStand stand2 = DisplayUtil.createDisplay(barterItem, rightLoc, shop.getFacing());
                    stand2.setCustomName(generateDisplayName());
                    stand2.setCustomNameVisible(false);
                    entities.add(stand2);
                    break;
                case GLASS_CASE:
                    Location caseLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    caseLoc.add(0.0, -0.74, 0.0);
                    ArmorStand caseStand = DisplayUtil.createDisplay(new ItemStack(Material.GLASS), caseLoc, shop.getFacing());
                    caseStand.setSmall(false);
                    caseStand.setCustomName(generateDisplayName());
                    caseStand.setCustomNameVisible(false);
                    entities.add(caseStand);
                    Item item2 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    item2.setVelocity(new Vector(0.0, 0.1, 0.0));
                    item2.setPickupDelay(Integer.MAX_VALUE);
                    entities.add(item2);
                    Item item3 = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(true), barterItem);
                    item3.setVelocity(new Vector(0.0, 0.1, 0.0));
                    item3.setPickupDelay(Integer.MAX_VALUE);
                    entities.add(item3);
                    break;
            }
        } else {
            switch (displayType) {
                case ITEM:
                    Item j = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    j.setVelocity(new Vector(0.0, 0.1, 0.0));
                    j.setPickupDelay(Integer.MAX_VALUE);
                    entities.add(j);
                    break;
                case LARGE_ITEM:
                    ArmorStand stand3 = DisplayUtil.createDisplay(item, shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation(), shop.getFacing());
                    stand3.setCustomName(generateDisplayName());
                    stand3.setCustomNameVisible(false);
                    entities.add(stand3);
                    break;
                case GLASS_CASE:
                    Location caseLoc2 = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    caseLoc2.add(0.0, -0.74, 0.0);
                    ArmorStand caseStand2 = DisplayUtil.createDisplay(new ItemStack(Material.GLASS), caseLoc2, shop.getFacing());
                    caseStand2.setSmall(false);
                    caseStand2.setCustomName(generateDisplayName());
                    caseStand2.setCustomNameVisible(false);
                    entities.add(caseStand2);
                    Item caseDisplayItem = shop.getChestLocation().getWorld().dropItem(getItemDropLocation(false), item);
                    caseDisplayItem.setVelocity(new Vector(0.0, 0.1, 0.0));
                    caseDisplayItem.setPickupDelay(Integer.MAX_VALUE);
                    entities.add(caseDisplayItem);
                    break;
            }
        }
        shop.updateSign();
    }

    public DisplayType getType() {
        return type;
    }

    public AbstractShop getShop() {
        return Shop.getPlugin().getShopHandler().getShop(shopSignLocation);
    }

    public void setType(DisplayType type) {
        DisplayType oldType = this.type;
        if (oldType == DisplayType.NONE) {
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
            Block aboveShop = getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                return;
            }
        }
        int index = 0;
        for (int i = 0; i < cycle.length; ++i) {
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
        for (Entity entity : entities) {
            entity.remove();
        }
        entities.clear();
        for (Entity entity2 : shop.getChestLocation().getChunk().getEntities()) {
            if (isDisplay(entity2)) {
                AbstractShop s = getShop(entity2);
                if (s != null && s.getSignLocation().equals(shop.getSignLocation())) {
                    entity2.remove();
                }
            }
        }
    }

    private Location getItemDropLocation(boolean isBarterItem) {
        AbstractShop shop = getShop();
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
        try {
            if (entity.getType() == EntityType.DROPPED_ITEM) {
                ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                if (itemMeta != null && UtilMethods.containsLocation(itemMeta.getDisplayName())) {
                    return true;
                }
            } else if (entity.getType() == EntityType.ARMOR_STAND && UtilMethods.containsLocation(entity.getCustomName())) {
                return true;
            }
        } catch (NoSuchFieldError ignore) {
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
        try {
            if (display.getType() == EntityType.ARMOR_STAND) {
                name = display.getCustomName();
            }
        } catch (NoSuchFieldError error) {
            return null;
        }
        if (!UtilMethods.containsLocation(name)) {
            return null;
        }
        String locString = name.substring(name.indexOf(123) + 1, name.indexOf(125));
        String[] parts = locString.split(",");
        Location location = new Location(display.getWorld(), Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        return Shop.getPlugin().getShopHandler().getShop(location);
    }

    private String generateDisplayName() {
        return "***{" + shopSignLocation.getBlockX() + "," + shopSignLocation.getBlockY() + "," + shopSignLocation.getBlockZ() + "}";
    }
}
