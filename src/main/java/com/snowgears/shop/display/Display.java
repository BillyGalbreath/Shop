package com.snowgears.shop.display;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.GambleShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.util.DisplayUtil;
import com.snowgears.shop.util.UtilMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class Display {

    private Location shopSignLocation;
    private DisplayType type;
    private ArrayList<Entity> entities;
    private DisplayType[] cycle = Shop.getPlugin().getDisplayCycle();

    public Display(Location shopSignLocation) {
        this.shopSignLocation = shopSignLocation;
        entities = new ArrayList<>();
    }

    public void spawn() {
        remove();
        //Random random = new Random();

        AbstractShop shop = this.getShop();

        if (shop.getItemStack() == null)
            return;

        //define the initial display item
        ItemStack item = shop.getItemStack().clone();
        item.setAmount(1);

        DisplayType displayType = this.type;
        if(displayType == null)
            displayType = Shop.getPlugin().getDisplayType();

        //two display entities on the chest
        if (shop.getType() == ShopType.BARTER) {
            if (shop.getSecondaryItemStack() == null)
                return;

            //define the barter display item
            ItemStack barterItem = shop.getSecondaryItemStack().clone();
            barterItem.setAmount(1);

            switch (displayType){
                case NONE:
                    //do nothing
                    break;
                case ITEM:
                    //Drop initial display item
                    Item i1 = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(false), item);
                    i1.setVelocity(new Vector(0, 0.1, 0));
                    i1.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    tagDisplayWithName(i1, item);
                    tagEntityAsDisplay(i1);

                    //Drop the barter display item
                    Item i2 = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(true), barterItem);
                    i2.setVelocity(new Vector(0, 0.1, 0));
                    i2.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    tagDisplayWithName(i2, barterItem);
                    tagEntityAsDisplay(i2);
                    break;
                case LARGE_ITEM:
                    //put first large display down
                    Location leftLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    leftLoc.add(getLargeItemBarterOffset(false));
                    ArmorStand stand = DisplayUtil.createDisplay(item, leftLoc, shop.getFacing());
                    tagDisplayWithName(stand, item);
                    tagEntityAsDisplay(stand);

                    //put second large display down
                    Location rightLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    rightLoc.add(getLargeItemBarterOffset(true));
                    ArmorStand stand2 = DisplayUtil.createDisplay(barterItem, rightLoc, shop.getFacing());
                    tagDisplayWithName(stand2, barterItem);
                    tagEntityAsDisplay(stand2);
                    break;
                case GLASS_CASE:
                    //put the extra large glass casing down
                    Location caseLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    caseLoc.add(0, -0.74, 0);
                    ArmorStand caseStand = DisplayUtil.createDisplay(new ItemStack(Material.GLASS), caseLoc, shop.getFacing());
                    caseStand.setSmall(false);
                    tagEntityAsDisplay(caseStand);

                    //Drop initial display item
                    Item item1 = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(false), item);
                    item1.setVelocity(new Vector(0, 0.1, 0));
                    item1.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    tagDisplayWithName(item1, item);
                    tagEntityAsDisplay(item1);

                    //Drop the barter display item
                    Item item2 = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(true), barterItem);
                    item2.setVelocity(new Vector(0, 0.1, 0));
                    item2.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    tagDisplayWithName(item2, barterItem);
                    tagEntityAsDisplay(item2);
                    break;
                    //this code stacks item frames on top of each other for barter type. i dont like it and will be disabling item_frame display type for barter shops for the moment
//                case ITEM_FRAME:
//                    ItemFrame frame = (ItemFrame) shop.getChestLocation().getWorld().spawn(shop.getChestLocation().getBlock().getLocation().clone().add(0, 2, 0),
//                            ItemFrame.class,
//                            entity -> {
//                                ItemFrame itemFrame = (ItemFrame) entity;
//                                itemFrame.setFacingDirection(shop.getFacing(), true);
//                                itemFrame.setFixed(true);
//                                itemFrame.setCustomName(this.generateDisplayName(random));
//                                itemFrame.setItem(item);
//                            });
//                    ItemFrame frame2 = (ItemFrame) shop.getChestLocation().getWorld().spawn(shop.getChestLocation().getBlock().getLocation().clone().add(0, 1, 0),
//                            ItemFrame.class,
//                            entity -> {
//                                ItemFrame itemFrame = (ItemFrame) entity;
//                                itemFrame.setFacingDirection(shop.getFacing(), true);
//                                itemFrame.setFixed(true);
//                                itemFrame.setCustomName(this.generateDisplayName(random));
//                                itemFrame.setItem(barterItem);
//                            });
//                    entities.add(frame);
//                    entities.add(frame2);
//                    break;
            }
        }
        //one display entity on the chest
        else {
            switch (displayType){
                case NONE:
                    //do nothing
                    break;
                case ITEM:
                    Item i = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(false), item);
                    i.setVelocity(new Vector(0, 0.1, 0));
                    i.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    tagDisplayWithName(i, item);
                    tagEntityAsDisplay(i);
                    break;
                case LARGE_ITEM:
                    ArmorStand stand = DisplayUtil.createDisplay(item, shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation(), shop.getFacing());
                    tagDisplayWithName(stand, item);
                    tagEntityAsDisplay(stand);
                    break;
                case GLASS_CASE:
                    //put the extra large glass casing down
                    Location caseLoc = shop.getChestLocation().getBlock().getRelative(BlockFace.UP).getLocation();
                    caseLoc.add(0, -0.74, 0);
                    ArmorStand caseStand = DisplayUtil.createDisplay(new ItemStack(Material.GLASS), caseLoc, shop.getFacing());
                    caseStand.setSmall(false);
                    tagEntityAsDisplay(caseStand);

                    //drop the display item in the glass case
                    Item caseDisplayItem = shop.getChestLocation().getWorld().dropItem(this.getItemDropLocation(false), item);
                    caseDisplayItem.setVelocity(new Vector(0, 0.1, 0));
                    caseDisplayItem.setPickupDelay(Integer.MAX_VALUE); //stop item from being picked up ever
                    tagDisplayWithName(caseDisplayItem, item);
                    tagEntityAsDisplay(caseDisplayItem);
                    break;
                case ITEM_FRAME:
                    ItemFrame frame = (ItemFrame) shop.getChestLocation().getWorld().spawn(shop.getChestLocation().getBlock().getLocation().clone().add(0, 1, 0),
                                ItemFrame.class,
                                entity -> {
                                    ItemFrame itemFrame = (ItemFrame) entity;
                                    itemFrame.setFacingDirection(shop.getFacing(), true);
                                    itemFrame.setFixed(true);
                                    itemFrame.setItem(shop.getItemStack());
                                });
                    //todo might only want to do this if the item is not already custom named in frame?
                    tagDisplayWithName(frame, item);
                    tagEntityAsDisplay(frame);
                    break;
            }
        }
        shop.updateSign();
    }

    private void tagDisplayWithName(Entity entity, ItemStack item){
        if(Shop.getPlugin().showDisplayNameTags()){
            if(this.getShop().getType() == ShopType.GAMBLE) {
                ItemStack gambleItem = ((GambleShop)this.getShop()).getGambleItem();
                entity.setCustomName(Shop.getPlugin().getItemNameUtil().getName(gambleItem));
                return;
            }

            entity.setCustomName(Shop.getPlugin().getItemNameUtil().getName(item));
            entity.setCustomNameVisible(true);
        }
    }

    private void tagEntityAsDisplay(Entity entity){
        PersistentDataContainer persistentData = entity.getPersistentDataContainer();
        persistentData.set(new NamespacedKey(Shop.getPlugin(), "display"), PersistentDataType.INTEGER, 1);
        entities.add(entity);
    }

    public DisplayType getType(){
        return type;
    }

    public AbstractShop getShop(){
        return Shop.getPlugin().getShopHandler().getShop(this.shopSignLocation);
    }

    public void setType(DisplayType type){
        DisplayType oldType = this.type;

        if(oldType == DisplayType.NONE){
            //make sure there is room above the shop for the display
            Block aboveShop = this.getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                return;
            }
        }

        if(type == DisplayType.ITEM_FRAME && this.getShop().getType() == ShopType.BARTER){
            //make sure there are two blocks free above the shop for the 2 itemframe displays
            Block twoAboveShop = this.getShop().getChestLocation().getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(twoAboveShop.getType())) {
                return;
            }
        }


        this.type = type;
        if(!(type == DisplayType.NONE || type == DisplayType.ITEM)) {
            try {
                if (EntityType.ARMOR_STAND == EntityType.ARROW) {
                    //check that armor stand exists (server not on MC 1.7)
                }
            } catch (NoSuchFieldError e) {
                this.type = oldType;
            }
        }

        this.spawn();
    }

    public void cycleType(){
        DisplayType displayType = this.type;
        if(displayType == null) {
            displayType = Shop.getPlugin().getDisplayType();
        }

        if(displayType == DisplayType.NONE){
            //make sure there is room above the shop for the display
            Block aboveShop = this.getShop().getChestLocation().getBlock().getRelative(BlockFace.UP);
            if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                return;
            }
        }

        int index = 0;
        for(int i=0; i<cycle.length; i++){
            if(cycle[i] == displayType)
                index = i + 1;
        }
        if(index >= cycle.length)
            index = 0;

        //don't allow barter shops to have ITEM_FRAME display types (for NOW)
        if(cycle[index] == DisplayType.ITEM_FRAME && getShop().getType() == ShopType.BARTER){

            index++;
            if(index >= cycle.length)
                index = 0;
        }

        this.setType(cycle[index]);

        Shop.getPlugin().getShopHandler().saveShops(getShop().getOwnerUUID());
    }

    public void remove() {
        AbstractShop shop = this.getShop();

        Iterator<Entity> displayIterator = entities.iterator();
        while(displayIterator.hasNext()) {
            Entity displayEntity = displayIterator.next();
            displayEntity.remove();
        }
        entities.clear();

        for (Entity entity : shop.getChestLocation().getChunk().getEntities()) {
            if(isDisplay(entity)){
                AbstractShop s =  getShop(entity);
                //remove any displays that are left over but still belong to the same shop
                if(s != null && s.getSignLocation().equals(shop.getSignLocation()))
                    entity.remove();
            }
        }
    }

    private Location getItemDropLocation(boolean isBarterItem) {
        AbstractShop shop = this.getShop();

        //calculate which x,z to drop items at depending on direction of the shop sign
        double dropY = 1.2;
        double dropX = 0.5;
        double dropZ = 0.5;
        if (shop.getType() == ShopType.BARTER) {
            WallSign shopSign = (WallSign) shop.getSignLocation().getBlock().getBlockData();
            switch (shopSign.getFacing()) {
                case NORTH:
                    if (isBarterItem)
                        dropX = 0.3;
                    else
                        dropX = 0.7;
                    break;
                case EAST:
                    if (isBarterItem)
                        dropZ = 0.3;
                    else
                        dropZ = 0.7;
                    break;
                case SOUTH:
                    if (isBarterItem)
                        dropX = 0.7;
                    else
                        dropX = 0.3;
                    break;
                case WEST:
                    if (isBarterItem)
                        dropZ = 0.7;
                    else
                        dropZ = 0.3;
                    break;
                default:
                    dropX = 0.5;
                    dropZ = 0.5;
                    break;
            }
        }
        return shop.getChestLocation().clone().add(dropX, dropY, dropZ);
    }

    private Vector getLargeItemBarterOffset(boolean isBarterItem){
        AbstractShop shop = this.getShop();

        Vector offset = new Vector(0,0,0);
        double space = 0.24;
        if (shop.getType() == ShopType.BARTER) {
            WallSign shopSign = (WallSign) shop.getSignLocation().getBlock().getBlockData();
            switch (shopSign.getFacing()) {
                case NORTH:
                    if (isBarterItem)
                        offset.setX(-space);
                    else
                        offset.setX(space);
                    break;
                case EAST:
                    if (isBarterItem)
                        offset.setZ(-space);
                    else
                        offset.setZ(space);
                    break;
                case SOUTH:
                    if (isBarterItem)
                        offset.setX(space);
                    else
                        offset.setX(-space);
                    break;
                case WEST:
                    if (isBarterItem)
                        offset.setZ(space);
                    else
                        offset.setZ(-space);
                    break;
            }
        }
        return offset;
    }

    public static boolean isDisplay(Entity entity){
        //keep this old legacy code around for a bit //TODO delete this later and only use persistentDataContainer code for checking
        try {
            if (entity.getType() == EntityType.DROPPED_ITEM) {
                ItemMeta itemMeta = ((Item) entity).getItemStack().getItemMeta();
                if (itemMeta != null && UtilMethods.containsLocation(itemMeta.getDisplayName())) {
                    return true;
                }
            } else if (entity.getType() == EntityType.ARMOR_STAND || entity.getType() == EntityType.ITEM_FRAME) {
                if (UtilMethods.containsLocation(entity.getCustomName())) {
                    return true;
                }
            }
        } catch (NoSuchFieldError error){
            //do nothing
        }

        PersistentDataContainer persistentData = entity.getPersistentDataContainer();
        if(persistentData != null) {
            try {
                int dataDisplay = persistentData.get(new NamespacedKey(Shop.getPlugin(), "display"), PersistentDataType.INTEGER);
                return (dataDisplay == 1);
            } catch (NullPointerException e){ return false; }
        }

        return false;
    }

    public static AbstractShop getShop(Entity display){
        if(display == null)
            return null;
        String name = null;
        if (display.getType() == EntityType.DROPPED_ITEM) {
            ItemMeta itemMeta = ((Item) display).getItemStack().getItemMeta();
            name = itemMeta.getDisplayName();
        }
        try {
            if (display.getType() == EntityType.ARMOR_STAND || display.getType() == EntityType.ITEM_FRAME) {
                name = display.getCustomName();
            }
        } catch (NoSuchFieldError error){
            return null;
        }

        if(!UtilMethods.containsLocation(name)) {
            return null;
        }

        String locString = name.substring(name.indexOf('{')+1, name.indexOf('}'));
        String[] parts = locString.split(",");
        Location location = new Location(display.getWorld(), Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        return Shop.getPlugin().getShopHandler().getShop(location);
    }

    private String generateDisplayName(Random random){
        Location loc = this.shopSignLocation;
        String name = "***{"+loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()+"}"; //+random.nextInt(1000);
        return name;
    }
}