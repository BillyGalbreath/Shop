package com.snowgears.shop;

import com.snowgears.shop.event.PlayerExchangeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ComboShop extends AbstractShop {
    private final double priceBuy;
    private final double priceSell;

    public ComboShop(Location signLocation, UUID owner, double price, double priceSell, int amount, Boolean isAdmin) {
        super(signLocation, owner, price, amount, isAdmin);
        this.type = ShopType.COMBO;
        this.signLines = ShopMessage.getSignLines(this, type);
        this.priceBuy = price;
        this.priceSell = priceSell;
    }

    @Override
    public TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType) {
        TransactionError issue;
        if (transactionType == ShopType.SELL) {
            issue = executeSellTransaction(player, isCheck);
        } else {
            issue = executeBuyTransaction(player, isCheck);
        }
        return issue;
    }

    private TransactionError executeSellTransaction(Player player, boolean isCheck) {
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
                boolean hasFunds = EconomyUtils.hasSufficientFunds(player, player.getInventory(), getPriceSell());
                if (!hasFunds) {
                    issue = TransactionError.INSUFFICIENT_FUNDS_PLAYER;
                }
            } else {
                EconomyUtils.removeFunds(player, player.getInventory(), getPriceSell());
            }
        }
        if (issue == null && !isAdmin()) {
            if (isCheck) {
                boolean hasRoom = EconomyUtils.canAcceptFunds(getOwner(), getInventory(), getPriceSell());
                if (!hasRoom) {
                    issue = TransactionError.INVENTORY_FULL_SHOP;
                }
            } else {
                EconomyUtils.addFunds(getOwner(), getInventory(), getPriceSell());
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
        return executeSellTransaction(player, false);
    }

    private TransactionError executeBuyTransaction(Player player, boolean isCheck) {
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
        return executeBuyTransaction(player, false);
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
        if (priceBuy != 0.0) {
            message = ShopMessage.getMessage(ShopType.BUY.toString(), "descriptionPrice", this, player);
            player.sendMessage(message);
            message = ShopMessage.getMessage(ShopType.BUY.toString(), "descriptionPricePerItem", this, player);
            player.sendMessage(message);
            player.sendMessage("");
        }
        if (priceSell != 0.0) {
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
