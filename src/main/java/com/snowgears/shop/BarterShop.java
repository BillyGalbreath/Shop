package com.snowgears.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class BarterShop extends AbstractShop {
    public BarterShop(Location signLocation, UUID owner, double price, int amount, Boolean isAdmin) {
        super(signLocation, owner, price, amount, isAdmin);
        this.type = ShopType.BARTER;
        this.signLines = ShopMessage.getSignLines(this, type);
    }

    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        TransactionError issue = null;
        ItemStack is = getItemStack();
        ItemStack is2 = getSecondaryItemStack();
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
                int playerItems = InventoryUtils.getAmount(player.getInventory(), is2);
                if (playerItems < is2.getAmount()) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                InventoryUtils.removeItem(player.getInventory(), is2, player);
            }
        }
        if (issue == null && !isAdmin()) {
            if (isCheck) {
                boolean hasRoom = InventoryUtils.hasRoom(getInventory(), is2, getOwner());
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_SHOP;
                }
            } else {
                InventoryUtils.addItem(getInventory(), is2, getOwner());
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

    @Override
    public boolean isInitialized() {
        return item != null && secondaryItem != null;
    }
}
