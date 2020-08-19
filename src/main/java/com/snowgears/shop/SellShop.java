package com.snowgears.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class SellShop extends AbstractShop {
    public SellShop(Location signLocation, UUID owner, double price, int amount, Boolean isAdmin) {
        super(signLocation, owner, price, amount, isAdmin);
        this.type = ShopType.SELL;
        this.signLines = ShopMessage.getSignLines(this, type);
    }

    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        TransactionError issue = null;
        ItemStack is = getItemStack();
        if (!isAdmin()) {
            if (isCheck) {
                int shopItems = InventoryUtils.getAmount(getInventory(), is);
                if (shopItems < is.getAmount()) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
            } else {
                InventoryUtils.removeItem(getInventory(), is, getOwner());
            }
        }
        if (issue == null) {
            if (isCheck) {
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), getPrice());
                if (!hasFunds) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                EconomyUtils.removeFunds(player, player.getInventory(), getPrice());
            }
        }
        if (issue == null && !isAdmin()) {
            if (isCheck) {
                boolean hasRoom = EconomyUtils.canAcceptFunds(getOwner(), getInventory(), getPrice());
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_SHOP;
                }
            } else {
                EconomyUtils.addFunds(getOwner(), getInventory(), getPrice());
            }
        }
        if (issue == null) {
            if (isCheck) {
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), is, player);
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
                }
            } else {
                InventoryUtils.addItem(player.getInventory(), is, player);
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
}
