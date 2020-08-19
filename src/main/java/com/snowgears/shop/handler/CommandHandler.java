package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import com.snowgears.shop.gui.ShopGuiWindow;
import com.snowgears.shop.util.ItemNameUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.util.List;

public class CommandHandler extends BukkitCommand {
    private final Shop plugin;

    public CommandHandler(Shop instance, String permission, String name, String description, String usageMessage, List<String> aliases) {
        super(name, description, usageMessage, aliases);
        this.plugin = instance;
        setPermission(permission);
        register();
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        ShopHandler handler = plugin.getShopHandler();
        ItemNameUtil nameUtil = plugin.getItemNameUtil();
        if (args.length == 0) {
            if (player != null) {
                if (plugin.useGUI()) {
                    ShopGuiWindow window = plugin.getGuiHandler().getWindow(player);
                    window.open();
                } else {
                    player.sendMessage(ChatColor.AQUA + "/" + getName() + " list" + ChatColor.GRAY + " - list your shops on the server");
                    player.sendMessage(ChatColor.AQUA + "/" + getName() + " currency" + ChatColor.GRAY + " - info about the currency shops use");
                    if (player.hasPermission("shop.operator") || player.isOp()) {
                        player.sendMessage(ChatColor.RED + "/" + getName() + " setcurrency" + ChatColor.GRAY + " - set the currency item to item in hand");
                        player.sendMessage(ChatColor.RED + "/" + getName() + " setgamble" + ChatColor.GRAY + " - set the gamble item display to item in hand");
                        player.sendMessage(ChatColor.RED + "/" + getName() + " item refresh" + ChatColor.GRAY + " - refresh all display items on shops");
                        player.sendMessage(ChatColor.RED + "/" + getName() + " reload" + ChatColor.GRAY + " - reload Shop plugin");
                    }
                }
            } else {
                sender.sendMessage("/" + getName() + " list - list all shops on server");
                sender.sendMessage("/" + getName() + " currency - information about currency being used on server");
                sender.sendMessage("/" + getName() + " item refresh - refresh display items on all shops");
                sender.sendMessage("/" + getName() + " reload - reload Shop plugin");
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("list")) {
                if (player != null) {
                    sender.sendMessage("There are " + ChatColor.GOLD + handler.getNumberOfShops() + ChatColor.WHITE + " shops registered on the server.");
                    if (plugin.usePerms()) {
                        sender.sendMessage(ChatColor.GRAY + "You have built " + handler.getNumberOfShops(player) + " out of your " + plugin.getShopListener().getBuildLimit(player) + " allotted shops.");
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "You own " + handler.getNumberOfShops(player) + " of these shops.");
                    }
                } else {
                    sender.sendMessage("[Shop] There are " + handler.getNumberOfShops() + " shops registered on the server.");
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (player != null) {
                    if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                        player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                        return true;
                    }
                }
                plugin.reload();
                sender.sendMessage("[Shop] Reloaded plugin.");
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online != null) {
                        online.closeInventory();
                    }
                }
            } else if (args[0].equalsIgnoreCase("currency")) {
                if (player != null) {
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        if (plugin.useVault()) {
                            player.sendMessage(ChatColor.GRAY + "The server is using virtual currency through Vault.");
                        } else {
                            player.sendMessage(ChatColor.GRAY + "The server is using " + nameUtil.getName(plugin.getItemCurrency()) + " as currency.");
                            player.sendMessage(ChatColor.GRAY + "To change this run the command '/shop setcurrency' with the item you want in your hand.");
                        }
                        return true;
                    }
                } else {
                    sender.sendMessage("The server is using " + nameUtil.getName(plugin.getItemCurrency()) + " as currency.");
                }
            } else if (args[0].equalsIgnoreCase("setcurrency")) {
                if (player != null) {
                    if ((plugin.usePerms() && player.hasPermission("shop.operator")) || player.isOp()) {
                        if (plugin.useVault()) {
                            player.sendMessage(ChatColor.RED + "The server is using virtual currency through Vault and so no item could be set.");
                            return true;
                        }
                        ItemStack handItem = player.getInventory().getItemInMainHand();
                        if (handItem.getType() == Material.AIR) {
                            player.sendMessage(ChatColor.RED + "You must be holding a valid item to set the shop currency.");
                            return true;
                        }
                        handItem.setAmount(1);
                        plugin.setItemCurrency(handItem);
                        player.sendMessage(ChatColor.GRAY + "The server is now using " + nameUtil.getName(plugin.getItemCurrency()) + " as currency.");
                        return true;
                    }
                } else {
                    sender.sendMessage("The server is using " + nameUtil.getName(plugin.getItemCurrency()) + " as currency.");
                }
            } else if (args[0].equalsIgnoreCase("setgamble") && player != null) {
                if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                    player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                    return true;
                }
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    player.sendMessage(ChatColor.RED + "You must have an item in your hand to use that command.");
                    return true;
                }
                plugin.setGambleDisplayItem(player.getInventory().getItemInMainHand());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("refresh")) {
            if (player != null) {
                if ((plugin.usePerms() && !player.hasPermission("shop.operator")) || (!plugin.usePerms() && !player.isOp())) {
                    player.sendMessage(ChatColor.RED + "You are not authorized to use that command.");
                    return true;
                }
            }
            handler.refreshShopDisplays();
            sender.sendMessage("[Shop] The display items on all of the shops have been refreshed.");
        }
        return true;
    }

    private void register() {
        try {
            Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            commandMap.register(getName(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
