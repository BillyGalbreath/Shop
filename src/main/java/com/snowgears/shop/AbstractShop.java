package com.snowgears.shop;

import com.snowgears.shop.display.Display;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ReflectionUtil;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class AbstractShop {
    protected Location signLocation;
    protected Location chestLocation;
    protected UUID owner;
    protected ItemStack item;
    protected ItemStack secondaryItem;
    protected Display display;
    protected double price;
    protected int amount;
    protected boolean isAdmin;
    protected ShopType type;
    protected String[] signLines;

    public AbstractShop(Location signLoc, UUID player, double pri, int amt, Boolean admin) {
        signLocation = signLoc;
        owner = player;
        price = pri;
        amount = amt;
        isAdmin = admin;
        item = null;

        display = new Display(signLocation);

        if (isAdmin) {
            owner = Shop.getPlugin().getShopHandler().getAdminUUID();
        }

        if (signLocation != null) {
            WallSign sign = (WallSign) signLocation.getBlock().getBlockData();
            chestLocation = signLocation.getBlock().getRelative(sign.getFacing().getOppositeFace()).getLocation();
        }
    }

    public static AbstractShop create(Location signLoc, UUID player, double pri, double priCombo, int amt, Boolean admin, ShopType shopType) {

        switch (shopType) {
            case SELL:
                return new SellShop(signLoc, player, pri, amt, admin);
            case BUY:
                return new BuyShop(signLoc, player, pri, amt, admin);
            case BARTER:
                return new BarterShop(signLoc, player, pri, amt, admin);
            case GAMBLE:
                return new GambleShop(signLoc, player, pri, amt, admin);
            case COMBO:
                return new ComboShop(signLoc, player, pri, priCombo, amt, admin);
        }
        return new BuyShop(signLoc, player, pri, amt, admin);
    }

    //abstract methods that must be implemented in each shop subclass

    public abstract TransactionError executeTransaction(int orders, Player player, boolean isCheck, ShopType transactionType);

    public int getStock() {
        return InventoryUtils.getAmount(getInventory(), getItemStack()) / getAmount();
    }

    public boolean isInitialized() {
        return (item != null);
    }

    //getter methods

    public Location getSignLocation() {
        return signLocation;
    }

    public Location getChestLocation() {
        return chestLocation;
    }

    public Inventory getInventory() {
        Block chestBlock = chestLocation.getBlock();
        if (chestBlock.getType() == Material.ENDER_CHEST) {
            OfflinePlayer ownerPlayer = getOwner();
            if (ownerPlayer != null) {
                return Shop.getPlugin().getEnderChestHandler().getInventory(ownerPlayer);
            }
        } else if (chestBlock.getState() instanceof InventoryHolder) {
            return ((InventoryHolder) (chestBlock.getState())).getInventory();
        }
        return null;
    }

    public UUID getOwnerUUID() {
        return owner;
    }

    public String getOwnerName() {
        if (isAdmin()) {
            return "admin";
        }
        if (getOwner() != null) {
            return Bukkit.getOfflinePlayer(owner).getName();
        }
        return ChatColor.RED + "CLOSED";
    }

    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(owner);
    }

    public ItemStack getItemStack() {
        if (item != null) {
            ItemStack is = item.clone();
            is.setAmount(getAmount());
            return is;
        }
        return null;
    }

    public ItemStack getSecondaryItemStack() {
        if (secondaryItem != null) {
            ItemStack is = secondaryItem.clone();
            is.setAmount((int) getPrice());
            return is;
        }
        return null;
    }

    public Display getDisplay() {
        return display;
    }

    public double getPrice() {
        return price;
    }

    public String getPriceString() {
        return Shop.getPlugin().getPriceString(price, false);
    }

    public String getPricePerItemString() {
        double pricePer = getPrice() / getAmount();
        return Shop.getPlugin().getPriceString(pricePer, true);
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public ShopType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public BlockFace getFacing() {
        if (signLocation.getBlock().getBlockData() instanceof WallSign) {
            BlockData signData = signLocation.getBlock().getBlockData();
            if (signData instanceof Directional) {
                return ((Directional) signData).getFacing();
            }
        }
        return null;
    }

    //setter methods

    public void setItemStack(ItemStack is) {
        item = is.clone();
        if (!Shop.getPlugin().checkItemDurability()) {
            if (item.getType().getMaxDurability() > 0) {
                item.setDurability((short) 0); //set item to full durability
            }
        }
        //display.spawn();
    }

    public void setSecondaryItemStack(ItemStack is) {
        secondaryItem = is.clone();
        if (!Shop.getPlugin().checkItemDurability()) {
            if (secondaryItem.getType().getMaxDurability() > 0) {
                secondaryItem.setDurability((short) 0); //set item to full durability
            }
        }
        //display.spawn();
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getItemDurabilityPercent() {
        ItemStack item = getItemStack().clone();
        return UtilMethods.getDurabilityPercent(item);
    }

    public int getSecondaryItemDurabilityPercent() {
        ItemStack item = getSecondaryItemStack().clone();
        return UtilMethods.getDurabilityPercent(item);
    }

    //common base methods to all shops

    public void updateSign() {

        signLines = ShopMessage.getSignLines(this, type);

        Shop.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(Shop.getPlugin(), () -> {
            Sign signBlock = (Sign) signLocation.getBlock().getState();

            String[] lines = signLines.clone();

            if (!isInitialized()) {
                signBlock.setLine(0, ChatColor.RED + ChatColor.stripColor(lines[0]));
                signBlock.setLine(1, ChatColor.RED + ChatColor.stripColor(lines[1]));
                signBlock.setLine(2, ChatColor.RED + ChatColor.stripColor(lines[2]));
                signBlock.setLine(3, ChatColor.RED + ChatColor.stripColor(lines[3]));
            } else {
                signBlock.setLine(0, lines[0]);
                signBlock.setLine(1, lines[1]);
                signBlock.setLine(2, lines[2]);
                signBlock.setLine(3, lines[3]);
            }

            signBlock.update(true);
        }, 2L);
    }

    public void delete() {
        display.remove();

        Block b = getSignLocation().getBlock();
        if (b.getBlockData() instanceof WallSign) {
            Sign signBlock = (Sign) b.getState();
            signBlock.setLine(0, "");
            signBlock.setLine(1, "");
            signBlock.setLine(2, "");
            signBlock.setLine(3, "");
            signBlock.update(true);
        }

        //finally remove the shop from the shop handler
        Shop.getPlugin().getShopHandler().removeShop(this);
    }

    public void teleportPlayer(Player player) {
        if (player == null) {
            return;
        }

        BlockFace face = getFacing();
        Location loc = getSignLocation().getBlock().getRelative(face).getLocation().add(0.5, 0, 0.5);
        loc.setYaw(UtilMethods.faceToYaw(face.getOppositeFace()));
        loc.setPitch(25.0f);

        player.teleport(loc);
    }

    //TODO you may have to override this in other shop types like COMBO or GAMBLE
    public void printSalesInfo(Player player) {
        player.sendMessage("");

        String message = ShopMessage.getUnformattedMessage(getType().toString(), "descriptionItem");
        formatAndSendFancyMessage(message, player);

        if (getType() == ShopType.BARTER) {
            message = ShopMessage.getUnformattedMessage(getType().toString(), "descriptionBarterItem");
            formatAndSendFancyMessage(message, player);
        }
        player.sendMessage("");


        if (price != 0) {
            message = ShopMessage.getMessage(getType().toString(), "descriptionPrice", this, player);
            player.sendMessage(message);

            message = ShopMessage.getMessage(getType().toString(), "descriptionPricePerItem", this, player);
            player.sendMessage(message);
            player.sendMessage("");
        }

        if (isAdmin()) {
            message = ShopMessage.getMessage("description", "stockAdmin", this, player);
        } else {
            message = ShopMessage.getMessage("description", "stock", this, player);
        }
        player.sendMessage(message);
    }

    protected void formatAndSendFancyMessage(String message, Player player) {
        if (message == null) {
            return;
        }

        String[] parts = message.split("(?=&[0-9A-FK-ORa-fk-or])");
        TextComponent fancyMessage = new TextComponent("");

        for (String part : parts) {
            ComponentBuilder builder = new ComponentBuilder("");
            org.bukkit.ChatColor cc = UtilMethods.getChatColor(part);
            if (cc != null) {
                part = part.substring(2);
            }
            boolean barterItem = false;
            if (part.contains("[barter item]")) {
                barterItem = true;
            }
            part = ShopMessage.formatMessage(part, this, player, false);
            part = ChatColor.stripColor(part);
            builder.append(part);
            if (cc != null) {
                builder.color(ChatColor.of(cc.name()));
            }

            if (part.startsWith("[")) {
                String itemJson;
                if (barterItem) {
                    itemJson = ReflectionUtil.convertItemStackToJson(secondaryItem);
                } else {
                    itemJson = ReflectionUtil.convertItemStackToJson(item);
                }
                // Prepare a BaseComponent array with the itemJson as a text component
                BaseComponent[] hoverEventComponents = new BaseComponent[]{new TextComponent(itemJson)}; // The only element of the hover events basecomponents is the item json
                HoverEvent event = new HoverEvent(HoverEvent.Action.SHOW_ITEM, new Text(hoverEventComponents));

                builder.event(event);
            }

            for (BaseComponent b : builder.create()) {
                fancyMessage.addExtra(b);
            }
        }

        player.spigot().sendMessage(fancyMessage);
    }
}
