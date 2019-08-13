package com.snowgears.shop.util;

import com.snowgears.shop.Shop;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class EconomyUtils {
    //check to see if the player has enough funds to take out [amount]
    //return false if they do not
    public static boolean hasSufficientFunds(OfflinePlayer player, Inventory inventory, double amount) {
        if (Shop.getPlugin().useVault()) {
            double balance = Shop.getPlugin().getEconomy().getBalance(player);
            return (balance >= amount);
        } else {
            ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
            currency.setAmount(1);
            int stock = InventoryUtils.getAmount(inventory, currency);
            return (stock >= amount);
        }
    }

    //check to see if the player has enough space to accept the funds to deposit [amount]
    //return false if they do not
    public static boolean canAcceptFunds(OfflinePlayer player, Inventory inventory, double amount) {
        if (Shop.getPlugin().useVault()) {
            return true;
        } else {
            ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
            currency.setAmount((int) amount);

            return InventoryUtils.hasRoom(inventory, currency, player);
        }
    }

    //gets the current funds of the player
    public static double getFunds(OfflinePlayer player, Inventory inventory) {
        if (Shop.getPlugin().useVault()) {
            return Shop.getPlugin().getEconomy().getBalance(player);
        } else {
            ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
            currency.setAmount(1);
            return InventoryUtils.getAmount(inventory, currency);
        }
    }

    //removes [amount] of funds from the player
    //return false if the player did not have sufficient funds or if something went wrong
    public static boolean removeFunds(OfflinePlayer player, Inventory inventory, double amount) {
        if (Shop.getPlugin().useVault()) {
            EconomyResponse response = Shop.getPlugin().getEconomy().withdrawPlayer(player, amount);
            return response.transactionSuccess();
        } else {
            ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
            currency.setAmount((int) amount);
            int unremoved = InventoryUtils.removeItem(inventory, currency, player);
            if (unremoved > 0) {
                currency.setAmount(((int) amount) - unremoved);
                InventoryUtils.addItem(inventory, currency, player);
                return false;
            }
        }
        return true;
    }

    //adds [amount] of funds to the player
    //return false if the player did not have enough room for items or if something went wrong
    public static void addFunds(OfflinePlayer player, Inventory inventory, double amount) {
        if (Shop.getPlugin().useVault()) {
            EconomyResponse response = Shop.getPlugin().getEconomy().depositPlayer(player, amount);
            response.transactionSuccess();
        } else {
            ItemStack currency = Shop.getPlugin().getItemCurrency().clone();
            currency.setAmount((int) amount);
            int unadded = InventoryUtils.addItem(inventory, currency, player);
            if (unadded > 0) {
                currency.setAmount(((int) amount) - unadded);
                InventoryUtils.removeItem(inventory, currency, player);
            }
        }
    }
}
