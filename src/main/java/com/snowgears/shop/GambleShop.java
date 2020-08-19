package com.snowgears.shop;

import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GambleShop extends AbstractShop {
    private ItemStack gambleItem;

    public GambleShop(Location signLocation, UUID owner, double price, int amount, Boolean isAdmin) {
        super(signLocation, owner, price, amount, isAdmin);
        this.type = ShopType.GAMBLE;
        this.signLines = ShopMessage.getSignLines(this, type);
        this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
    }

    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        TransactionError issue = null;
        if (!isAdmin()) {
            if (isCheck) {
                int shopItems = InventoryUtils.getAmount(getInventory(), gambleItem);
                if (shopItems < gambleItem.getAmount()) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
            } else {
                InventoryUtils.removeItem(getInventory(), gambleItem, getOwner());
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
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), gambleItem, player);
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
                }
            } else {
                InventoryUtils.addItem(player.getInventory(), gambleItem, player);
            }
        }
        player.updateInventory();
        if (issue != null) {
            return issue;
        }
        if (!isCheck) {
            shuffleGambleItem();
            return TransactionError.NONE;
        }
        if (!new PlayerExchangeShopEvent(player, this).callEvent()) {
            return TransactionError.CANCELLED;
        }
        return executeTransaction(orders, player, false, transactionType);
    }

    public void shuffleGambleItem() {
        setItemStack(gambleItem);
        DisplayType initialDisplayType = getDisplay().getType();
        getDisplay().setType(DisplayType.ITEM);
        gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
        new BukkitRunnable() {
            @Override
            public void run() {
                setItemStack(Shop.getPlugin().getGambleDisplayItem());
                if (initialDisplayType == null) {
                    display.setType(Shop.getPlugin().getDisplayType());
                } else {
                    display.setType(initialDisplayType);
                }
            }
        }.runTaskLater(Shop.getPlugin(), 20L);
    }
}
