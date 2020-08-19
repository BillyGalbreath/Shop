
package com.snowgears.shop.listener;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.TransactionError;
import com.snowgears.shop.util.PlayerSettings;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.snowgears.shop.util.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;


public class TransactionListener implements Listener {
    private final Shop plugin;
    private final HashMap<Location, UUID> shopMessageCooldown = new HashMap<>();

    public TransactionListener(Shop instance) {
        this.plugin = instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onShopSignClick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }
            if (Tag.WALL_SIGNS.isTagged(clicked.getType())) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null || !shop.isInitialized()) {
                    return;
                }
                boolean canUseShopInRegion = true;
                try {
                    canUseShopInRegion = WorldGuardHook.canUseShop(player, shop.getSignLocation());
                } catch (NoClassDefFoundError ignore) {
                }
                if (!canUseShopInRegion) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player));
                    event.setCancelled(true);
                    return;
                }
                if (!plugin.getShopHandler().isChest(shop.getChestLocation().getBlock())) {
                    shop.delete();
                    return;
                }
                if (!shop.getOwnerName().equals(player.getName())) {
                    if (plugin.usePerms() && !player.hasPermission("shop.use." + shop.getType().toString().toLowerCase()) && !player.hasPermission("shop.use") && !player.hasPermission("shop.operator")) {
                        player.sendMessage(ShopMessage.getMessage("permission", "use", shop, player));
                        return;
                    }
                    if (shop.getType() == ShopType.COMBO) {
                        int clickedSide = UtilMethods.calculateSideFromClickedSign(player, clicked);
                        if (clickedSide >= 0) {
                            executeTransaction(player, shop, ShopType.BUY);
                        } else {
                            executeTransaction(player, shop, ShopType.SELL);
                        }
                    } else {
                        executeTransaction(player, shop, shop.getType());
                    }
                } else {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "useOwnShop", shop, player));
                    sendEffects(false, player, shop);
                }
                event.setCancelled(true);
            }
        }
    }

    private void executeTransaction(Player player, AbstractShop shop, ShopType actionType) {
        TransactionError issue = shop.executeTransaction(1, player, true, actionType);
        if (issue != TransactionError.NONE) {
            switch (issue) {
                case INSUFFICIENT_FUNDS_SHOP:
                    if (!shop.isAdmin()) {
                        Player owner = shop.getOwner().getPlayer();
                        if (owner != null && notifyOwner(shop) && plugin.getGuiHandler().getSettingsOption(owner, PlayerSettings.Option.STOCK_NOTIFICATIONS)) {
                            owner.sendMessage(ShopMessage.getMessage(actionType.toString(), "ownerNoStock", shop, owner));
                        }
                    }
                    player.sendMessage(ShopMessage.getMessage(actionType.toString(), "shopNoStock", shop, player));
                    break;
                case INSUFFICIENT_FUNDS_PLAYER:
                    player.sendMessage(ShopMessage.getMessage(actionType.toString(), "playerNoStock", shop, player));
                    break;
                case INVENTORY_FULL_SHOP:
                    if (!shop.isAdmin()) {
                        Player owner = shop.getOwner().getPlayer();
                        if (owner != null && notifyOwner(shop) && plugin.getGuiHandler().getSettingsOption(owner, PlayerSettings.Option.STOCK_NOTIFICATIONS)) {
                            owner.sendMessage(ShopMessage.getMessage(actionType.toString(), "ownerNoSpace", shop, owner));
                        }
                    }
                    player.sendMessage(ShopMessage.getMessage(actionType.toString(), "shopNoSpace", shop, player));
                    break;
                case INVENTORY_FULL_PLAYER:
                    player.sendMessage(ShopMessage.getMessage(actionType.toString(), "playerNoSpace", shop, player));
                    break;
            }
            sendEffects(false, player, shop);
            return;
        }
        sendExchangeMessages(shop, player, actionType);
        sendEffects(true, player, shop);
    }

    private void sendExchangeMessages(AbstractShop shop, Player player, ShopType shopType) {
        String message;
        if (shop.getType() == ShopType.COMBO && shopType == ShopType.SELL) {
            message = ShopMessage.getUnformattedMessage(shopType.toString(), "user");
            message = message.replaceAll("price]", "priceSell]");
            message = ShopMessage.formatMessage(message, shop, player, false);
        } else {
            message = ShopMessage.getMessage(shopType.toString(), "user", shop, player);
        }
        if (plugin.getGuiHandler().getSettingsOption(player, PlayerSettings.Option.SALE_USER_NOTIFICATIONS)) {
            player.sendMessage(message);
        }
        Player owner = Bukkit.getPlayer(shop.getOwnerName());
        if (owner != null && !shop.isAdmin()) {
            if (shop.getType() == ShopType.COMBO && shopType == ShopType.SELL) {
                message = ShopMessage.getUnformattedMessage(shopType.toString(), "owner");
                message = message.replaceAll("price]", "priceSell]");
                message = ShopMessage.formatMessage(message, shop, player, false);
            } else {
                message = ShopMessage.getMessage(shopType.toString(), "owner", shop, player);
            }
            if (plugin.getGuiHandler().getSettingsOption(owner, PlayerSettings.Option.SALE_OWNER_NOTIFICATIONS)) {
                owner.sendMessage(message);
            }
        }
    }

    void sendEffects(boolean success, Player player, AbstractShop shop) {
        if (success) {
            if (plugin.playSounds()) {
                player.playSound(shop.getSignLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            }
            if (plugin.playEffects()) {
                player.getWorld().playEffect(shop.getChestLocation(), Effect.STEP_SOUND, 9914);
            }
        } else {
            if (plugin.playSounds()) {
                player.playSound(shop.getSignLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
            }
            if (plugin.playEffects()) {
                player.getWorld().playEffect(shop.getChestLocation(), Effect.STEP_SOUND, 19496);
            }
        }
    }

    private boolean notifyOwner(AbstractShop shop) {
        if (shop.isAdmin()) {
            return false;
        }
        if (shopMessageCooldown.containsKey(shop.getSignLocation())) {
            return false;
        }
        shopMessageCooldown.put(shop.getSignLocation(), shop.getOwnerUUID());
        new BukkitRunnable() {
            public void run() {
                shopMessageCooldown.remove(shop.getSignLocation());
            }
        }.runTaskLater(plugin, 2400L);
        return true;
    }
}
