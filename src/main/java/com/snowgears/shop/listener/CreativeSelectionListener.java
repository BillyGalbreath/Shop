package com.snowgears.shop.listener;


import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopObject;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.util.PlayerData;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.HashMap;
import java.util.UUID;

public class CreativeSelectionListener implements Listener {

    private Shop plugin = Shop.getPlugin();
    private HashMap<UUID, PlayerData> playerDataMap = new HashMap<>();

    public CreativeSelectionListener(Shop instance) {
        plugin = instance;
    }

    //this method calls PlayerCreateShopEvent
    @EventHandler
    public void onPreShopSignClick(PlayerInteractEvent event) {
        final Player player = event.getPlayer();

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            final Block clicked = event.getClickedBlock();

            if (clicked.getType() == Material.WALL_SIGN) {
                ShopObject shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                } else if (shop.isInitialized()) {
                    return;
                }
                if (!player.getUniqueId().equals(shop.getOwnerUUID())) {
                    if((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator"))) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "initialize", shop, player));
                        plugin.getExchangeListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }
                if (shop.getType() == ShopType.BARTER && shop.getItemStack() == null) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "noItem", shop, player));
                    event.setCancelled(true);
                    return;
                }

                if (player.getItemInHand().getType() == Material.AIR) {
                    if (shop.getType() == ShopType.SELL) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "noItem", shop, player));
                    } else {
                        if ((shop.getType() == ShopType.BARTER && shop.getItemStack() != null && shop.getBarterItemStack() == null)
                                || shop.getType() == ShopType.BUY) {
                            this.addPlayerData(player, clicked.getLocation());
                        }
                    }
                }
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            if (event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                    || event.getFrom().getBlockX() != event.getTo().getBlockX()
                    || event.getFrom().getBlockY() != event.getTo().getBlockY()) {
                player.teleport(event.getFrom());
                for(String message : ShopMessage.getCreativeSelectionLines(true)){
                    player.sendMessage(message);
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event){
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            if (event.getFrom().distanceSquared(event.getTo()) > 4) {
                event.setCancelled(true);
                for(String message : ShopMessage.getCreativeSelectionLines(true)){
                    player.sendMessage(message);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (playerDataMap.get(player.getUniqueId()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void inventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player))
            return;
        Player player = (Player) event.getPlayer();
        removePlayerData(player);
    }

    @EventHandler
    public void onShopIntialize(PlayerInitializeShopEvent event){
        removePlayerData(event.getPlayer());
    }

    @EventHandler
    public void onCreativeClick(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        PlayerData playerData = PlayerData.loadFromFile(player);
        if (playerData != null) {
            //player dropped item outside the inventory
            if (event.getSlot() == -999 && event.getCursor() != null) {
                ShopObject shop = playerData.getShop();
                if (shop != null) {
                    if (shop.getType() == ShopType.BUY) {

                        PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                        Bukkit.getServer().getPluginManager().callEvent(e);

                        if(e.isCancelled())
                            return;

                        shop.setItemStack(event.getCursor());
                        shop.getDisplay().spawn();
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                        plugin.getExchangeListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());

                    } else if (shop.getType() == ShopType.BARTER) {

                        PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                        Bukkit.getServer().getPluginManager().callEvent(e);

                        if(e.isCancelled())
                            return;

                        shop.setBarterItemStack(event.getCursor());
                        shop.getDisplay().spawn();
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                        plugin.getExchangeListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    }
                    removePlayerData(player);
                }
            }
            event.setCancelled(true);
        }
    }

    public void addPlayerData(Player player, Location shopSignLocation) {
        System.out.println("Add Player Data called.");
        if(playerDataMap.containsKey(player.getUniqueId()))
            return;
        System.out.println("Creating new player data.");
        PlayerData data = new PlayerData(player, shopSignLocation);
        playerDataMap.put(player.getUniqueId(), data);

        for(String message : ShopMessage.getCreativeSelectionLines(false)){
            player.sendMessage(message);
        }
        player.setGameMode(GameMode.CREATIVE);
    }

    public void removePlayerData(Player player){
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if(data != null) {
            playerDataMap.remove(player.getUniqueId());
            data.apply();
        }
    }

    //make sure that if player somehow quit without getting their old data back, return it to them when they login next
    @EventHandler
    public void onLogin(PlayerLoginEvent event){
        final Player player = event.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(Shop.getPlugin(), new Runnable() {
            @Override
            public void run() {
                PlayerData data = PlayerData.loadFromFile(player);
                if(data != null){
                    playerDataMap.remove(player.getUniqueId());
                    data.apply();
                }
            }
        }, 10);
    }
}
