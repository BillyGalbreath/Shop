package com.snowgears.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BuyShop extends AbstractShop {
    public BuyShop(Location signLoc, UUID player, double pri, int amt, Boolean admin) {
        super(signLoc, player, pri, amt, admin);

        this.type = ShopType.BUY;
        this.signLines = ShopMessage.getSignLines(this, type);
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {

        TransactionError issue = null;
        ItemStack is = getItemStack();

        //check if player has enough items
        if (isCheck) {
            int playerItems = InventoryUtils.getAmount(player.getInventory(), is);
            if (playerItems < is.getAmount()) {
                issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
            }
        } else {
            //remove items from player
            InventoryUtils.removeItem(player.getInventory(), is, player);
        }

        if (issue == null) {
            //check if shop has enough currency
            if (!isAdmin()) {
                if (isCheck) {
                    boolean hasFunds = EconomyUtils.hasSufficientFunds(getOwner(), getInventory(), getPrice());
                    if (!hasFunds) {
                        issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
                    }
                } else {
                    EconomyUtils.removeFunds(getOwner(), getInventory(), getPrice());
                }
            }
        }

        if (issue == null) {
            if (isCheck) {
                //check if player has enough room to accept currency
                boolean hasRoom = EconomyUtils.canAcceptFunds(player, player.getInventory(), getPrice());
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
                }
            } else {
                //add currency to player
                EconomyUtils.addFunds(player, player.getInventory(), getPrice());
            }
        }

        if (issue == null) {
            //check if shop has enough room to accept items
            if (!isAdmin()) {
                if (isCheck) {
                    boolean shopHasRoom = InventoryUtils.hasRoom(getInventory(), is, getOwner());
                    if (!shopHasRoom) {
                        issue = TransactionError.INVENTORY_FULL_SHOP;
                    }
                } else {
                    //add items to shop's inventory
                    InventoryUtils.addItem(getInventory(), is, getOwner());
                }
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
    public int getStock() {
        double funds = EconomyUtils.getFunds(getOwner(), getInventory());
        if (getPrice() == 0) {
            return Integer.MAX_VALUE;
        }
        return (int) (funds / getPrice());
    }
}
