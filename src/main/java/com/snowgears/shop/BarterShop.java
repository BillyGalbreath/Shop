package com.snowgears.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BarterShop extends AbstractShop {
    public BarterShop(Location signLoc, UUID player, double pri, int amt, Boolean admin) {
        super(signLoc, player, pri, amt, admin);

        this.type = ShopType.BARTER;
        this.signLines = ShopMessage.getSignLines(this, type);
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {

        TransactionError issue = null;

        ItemStack is = getItemStack();
        ItemStack is2 = getSecondaryItemStack();

        //check if shop has enough items
        if (!isAdmin()) {
            if (isCheck) {
                int shopItems = InventoryUtils.getAmount(getInventory(), is);
                if (shopItems < is.getAmount()) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
            } else {
                //remove items from shop
                InventoryUtils.removeItem(getInventory(), is, getOwner());
            }
        }

        if (issue == null) {
            if (isCheck) {
                //check if player has enough barter items
                int playerItems = InventoryUtils.getAmount(player.getInventory(), is2);
                if (playerItems < is2.getAmount()) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                //remove barter items from player
                InventoryUtils.removeItem(player.getInventory(), is2, player);
            }
        }

        if (issue == null) {
            //check if shop has enough room to accept barter items
            if (!isAdmin()) {
                if (isCheck) {
                    boolean hasRoom = InventoryUtils.hasRoom(getInventory(), is2, getOwner());
                    if (!hasRoom) {
                        issue = TransactionError.INVENTORY_FULL_SHOP;
                    }
                } else {
                    //add barter items to shop
                    InventoryUtils.addItem(getInventory(), is2, getOwner());
                }
            }
        }

        if (issue == null) {
            if (isCheck) {
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), is, player);
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
                }
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), is, player);
            }
        }

        player.updateInventory();

        if (issue != null) {
            return issue;
        }

        //if there are no issues with the test/check transaction
        if (isCheck) {

            PlayerExchangeShopEvent e = new PlayerExchangeShopEvent(player, this);
            Bukkit.getPluginManager().callEvent(e);

            if (e.isCancelled()) {
                return TransactionError.CANCELLED;
            }

            //run the transaction again without the check clause
            return executeTransaction(orders, player, false, transactionType);
        }
        return TransactionError.NONE;
    }

    @Override
    public boolean isInitialized() {
        return (item != null && secondaryItem != null);
    }

}
