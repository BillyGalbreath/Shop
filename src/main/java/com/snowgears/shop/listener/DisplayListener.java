package com.snowgears.shop.listener;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.display.Display;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.util.InventoryUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.ShulkerBox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DisplayListener implements Listener {

    public Shop plugin = Shop.getPlugin();
    private ArrayList<ItemStack> allServerRecipeResults = new ArrayList<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        if (Display.isDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onArmorStandClick(PlayerInteractEntityEvent event) {
        if (Display.isDisplay(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemFrameBreak(HangingBreakEvent event){
        if (Display.isDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    public DisplayListener(Shop instance) {
        plugin = instance;

        new BukkitRunnable() {
            @Override
            public void run() {
                HashMap<ItemStack, Boolean> recipes = new HashMap();
                Iterator<Recipe> recipeIterator = plugin.getServer().recipeIterator();
                while(recipeIterator.hasNext()) {
                    recipes.put(recipeIterator.next().getResult(), true);
                }
                allServerRecipeResults.addAll(recipes.keySet());
                Collections.shuffle(allServerRecipeResults);
            }
        }.runTaskLater(this.plugin, 1); //load all recipes on server once all other plugins are loaded
    }

    public ItemStack getRandomItem(AbstractShop shop){
        try {
            if (shop == null || !plugin.getShopHandler().isChest(shop.getChestLocation().getBlock()))
                return new ItemStack(Material.AIR);
        } catch (NullPointerException e){
            return new ItemStack(Material.AIR);
        }

        if(InventoryUtils.isEmpty(shop.getInventory())) {
            int index = new Random().nextInt(allServerRecipeResults.size());
            //TODO maybe later on add random amount between 1-64 depending on item type
            //like you could get 46 stack of dirt but not 46 stack of swords
            return allServerRecipeResults.get(index);
        } else {
            return InventoryUtils.getRandomItem(shop.getInventory());
        }
    }

    @EventHandler
    public void onWaterFlow(BlockFromToEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getToBlock().getRelative(BlockFace.DOWN));
        if (shop != null)
            event.setCancelled(true);
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
        if (shop != null && shop.getDisplay().getType() != DisplayType.NONE)
            event.setCancelled(true);

        for(Block pushedBlock : event.getBlocks()){
            shop = plugin.getShopHandler().getShopByChest(pushedBlock.getRelative(event.getDirection()).getRelative(BlockFace.DOWN));
            if (shop != null && shop.getDisplay().getType() != DisplayType.NONE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombust(EntityCombustEvent event) {
        if (Display.isDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (Display.isDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        AbstractShop shop = plugin.getShopHandler().getShopByChest(event.getBlock().getRelative(BlockFace.DOWN));
        if (shop != null && shop.getDisplay().getType() != DisplayType.NONE)
            event.setCancelled(true);
    }

    @EventHandler (priority = EventPriority.HIGHEST)
    public void onShopInventoryClose(InventoryCloseEvent event) {
        try {
            if(event.getInventory().getHolder() instanceof Container){
                Container container = ((Container)event.getInventory().getHolder());
                AbstractShop shop = plugin.getShopHandler().getShopByChest(container.getBlock());

                if(shop == null)
                    return;

                //refresh display if it's a shulker box (this elevates armor stands)
                if (event.getInventory().getHolder() instanceof ShulkerBox) {
                    if(shop != null){
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if(shop != null)
                                    shop.getDisplay().spawn();
                            }
                        }.runTaskLater(this.plugin, 15);
                    }
                }

                //if the sign lines use a variable that requires a refresh (like stock that is dynamically updated), then refresh sign
                if(shop.getSignLinesRequireRefresh())
                    shop.updateSign();
            }
        } catch (NoClassDefFoundError e) {}
    }

    //prevent picking up display items
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent event) {
        if(event.isCancelled())
            return;

        if(Display.isDisplay(event.getItem())){
            event.setCancelled(true);
            AbstractShop shop = Display.getShop(event.getItem());
            if(shop != null)
                shop.getDisplay().spawn();
            else
                event.getItem().remove();
        }
    }

    //prevent fishing hooks from grabbing display items
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onItemHook(PlayerFishEvent event){
        //if(event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY){
            if(event.getCaught() != null){
                if(Display.isDisplay(event.getCaught())){
                    event.setCancelled(true);
                }
            }
        //}
    }

    //prevent hoppers from grabbing display items
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onInventoryMoveItem(InventoryPickupItemEvent event) {
        if(Display.isDisplay(event.getItem())){
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (Display.isDisplay(event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
