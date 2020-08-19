package com.snowgears.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BuyShop extends AbstractShop {
    public BuyShop(Location signLocation, UUID owner, double price, int amount, Boolean isAdmin) {
        super(signLocation, owner, price, amount, isAdmin);
        this.type = ShopType.BUY;
        this.signLines = ShopMessage.getSignLines(this, type);
    }

    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        TransactionError issue = null;
        ItemStack is = getItemStack();
        if (isCheck) {
            int playerItems = InventoryUtils.getAmount(player.getInventory(), is);
            if (playerItems < is.getAmount()) {
                issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
            }
        } else {
            InventoryUtils.removeItem(player.getInventory(), is, player);
        }
        if (issue == null && !isAdmin()) {
            if (isCheck) {
                boolean hasFunds = EconomyUtils.hasSufficientFunds(getOwner(), getInventory(), getPrice());
                if (!hasFunds) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
            } else {
                EconomyUtils.removeFunds(getOwner(), getInventory(), getPrice());
            }
        }
        if (issue == null) {
            if (isCheck) {
                boolean hasRoom = EconomyUtils.canAcceptFunds(player, player.getInventory(), getPrice());
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
                }
            } else {
                EconomyUtils.addFunds(player, player.getInventory(), getPrice());
            }
        }
        if (issue == null && !isAdmin()) {
            if (isCheck) {
                boolean shopHasRoom = InventoryUtils.hasRoom(getInventory(), is, getOwner());
                if (!shopHasRoom) {
                    issue = TransactionError.INVENTORY_FULL_SHOP;
                }
            } else {
                InventoryUtils.addItem(getInventory(), is, getOwner());
            }
        }
        player.updateInventory();
        if (issue != null) {
            return issue;
        }
        if (!isCheck) {
            return TransactionError.NONE;
        }
        if (!new PlayerExchangeShopEvent(player, this).callEvent()) {
            return TransactionError.CANCELLED;
        }
        return executeTransaction(orders, player, false, transactionType);
    }

    @Override
    public int getStock() {
        double funds = EconomyUtils.getFunds(getOwner(), getInventory());
        if (getPrice() == 0.0) {
            return Integer.MAX_VALUE;
        }
        return (int) (funds / getPrice());
    }
}
