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

public class ComboShop extends AbstractShop {
    private final double priceBuy;
    private final double priceSell;

    public ComboShop(Location signLoc, UUID player, double pri, double priSell, int amt, Boolean admin) {
        super(signLoc, player, pri, amt, admin);

        this.type = ShopType.COMBO;
        this.signLines = ShopMessage.getSignLines(this, type);
        this.priceBuy = pri;
        this.priceSell = priSell;
    }

    //TODO incorporate # of orders at a time into this transaction
    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        TransactionError issue;
        if (transactionType == ShopType.SELL) {
            issue = executeSellTransaction(orders, player, isCheck);
        } else {
            issue = executeBuyTransaction(orders, player, isCheck);
        }
        return issue;
    }

    private TransactionError executeSellTransaction(int orders, Player player, boolean isCheck) {
        TransactionError issue = null;

        ItemStack is = getItemStack();

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
                //check if player has enough currency
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), getPriceSell());
                if (!hasFunds) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                //remove currency from player
                EconomyUtils.removeFunds(player, player.getInventory(), getPriceSell());
            }
        }

        if (issue == null) {
            //check if shop has enough room to accept currency
            if (!isAdmin()) {
                if (isCheck) {
                    boolean hasRoom = EconomyUtils.canAcceptFunds(getOwner(), getInventory(), getPriceSell());
                    if (!hasRoom) {
                        issue = TransactionError.INVENTORY_FULL_SHOP;
                    }
                } else {
                    //add currency to shop
                    EconomyUtils.addFunds(getOwner(), getInventory(), getPriceSell());
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
            return executeSellTransaction(orders, player, false);
        }
        return TransactionError.NONE;
    }

    private TransactionError executeBuyTransaction(int orders, Player player, boolean isCheck) {
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
            return executeBuyTransaction(orders, player, false);
        }
        return TransactionError.NONE;
    }

    public String getPriceSellString() {
        return Shop.getPlugin().getPriceString(priceSell, false);
    }

    public String getPriceSellPerItemString() {
        double pricePer = getPriceSell() / getAmount();
        return Shop.getPlugin().getPriceString(pricePer, true);
    }

    public String getPriceComboString() {
        return Shop.getPlugin().getPriceComboString(price, priceSell, false);
    }

    @Override
    public void printSalesInfo(Player player) {
        player.sendMessage("");

        String message = ShopMessage.getUnformattedMessage(ShopType.BUY.toString(), "descriptionItem");
        formatAndSendFancyMessage(message, player);
        player.sendMessage("");


        if (priceBuy != 0) {
            message = ShopMessage.getMessage(ShopType.BUY.toString(), "descriptionPrice", this, player);
            player.sendMessage(message);

            message = ShopMessage.getMessage(ShopType.BUY.toString(), "descriptionPricePerItem", this, player);
            player.sendMessage(message);
            player.sendMessage("");
        }

        if (priceSell != 0) {
            message = ShopMessage.getUnformattedMessage(ShopType.SELL.toString(), "descriptionItem");
            formatAndSendFancyMessage(message, player);
            player.sendMessage("");

            message = ShopMessage.getUnformattedMessage(ShopType.SELL.toString(), "descriptionPrice");
            message = message.replaceAll("price]", "priceSell]");
            message = ShopMessage.formatMessage(message, this, player, false);
            player.sendMessage(message);


            message = ShopMessage.getUnformattedMessage(ShopType.SELL.toString(), "descriptionPricePerItem");
            message = message.replaceAll("price per item]", "price sell per item]");
            message = ShopMessage.formatMessage(message, this, player, false);
            player.sendMessage(message);
        }

        if (isAdmin()) {
            message = ShopMessage.getMessage("description", "stockAdmin", this, player);
        } else {
            message = ShopMessage.getMessage("description", "stock", this, player);
        }
        player.sendMessage(message);
    }

    public double getPriceSell() {
        return priceSell;
    }
}
