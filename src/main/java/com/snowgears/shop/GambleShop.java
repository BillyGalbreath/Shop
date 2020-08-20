package com.snowgears.shop;

import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GambleShop extends AbstractShop {
    private ItemStack gambleItem;

    public GambleShop(Location signLoc, UUID player, double pri, int amt, Boolean admin) {
        super(signLoc, player, pri, amt, admin);

        this.type = ShopType.GAMBLE;
        this.signLines = ShopMessage.getSignLines(this, type);
        this.gambleItem = Shop.getPlugin().getDisplayListener().getRandomItem(this);
        this.setAmount(gambleItem.getAmount());
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {

        TransactionError issue = null;

        //check if shop has enough items
        if (!isAdmin()) {
            if (isCheck) {
                int shopItems = InventoryUtils.getAmount(getInventory(), gambleItem);
                if (shopItems < gambleItem.getAmount()) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_SHOP;
                }
            } else {
                //remove items from shop
                InventoryUtils.removeItem(getInventory(), gambleItem, getOwner());
            }
        }

        if (issue == null) {
            if (isCheck) {
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), getPrice());
                if (!hasFunds) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), getPrice());
            }
        }

        if (issue == null) {
            //check if shop has enough room to accept currency
            if (!isAdmin()) {
                if (isCheck) {
                    boolean hasRoom = EconomyUtils.canAcceptFunds(getOwner(), getInventory(), getPrice());
                    if (!hasRoom) {
                        issue = TransactionError.INVENTORY_FULL_SHOP;
                    }
                } else {
                    //add currency to shop
                    EconomyUtils.addFunds(getOwner(), getInventory(), getPrice());
                }
            }
        }

        if (issue == null) {
            if (isCheck) {
                //check if player has enough room to accept items
                boolean hasRoom = InventoryUtils.hasRoom(player.getInventory(), gambleItem, player);
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_PLAYER;
                }
            } else {
                //add items to player's inventory
                InventoryUtils.addItem(player.getInventory(), gambleItem, player);
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

        shuffleGambleItem();

        return TransactionError.NONE;
    }

    public void shuffleGambleItem() {
        setItemStack(gambleItem);
        setAmount(gambleItem.getAmount());
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
        }.runTaskLater(Shop.getPlugin(), 20);
    }
}
