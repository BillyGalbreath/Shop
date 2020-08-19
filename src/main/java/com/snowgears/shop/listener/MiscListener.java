package com.snowgears.shop.listener;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.SellShop;
import com.snowgears.shop.Shop;
import com.snowgears.shop.ShopType;
import com.snowgears.shop.display.DisplayType;
import com.snowgears.shop.event.PlayerCreateShopEvent;
import com.snowgears.shop.event.PlayerDestroyShopEvent;
import com.snowgears.shop.event.PlayerInitializeShopEvent;
import com.snowgears.shop.event.PlayerResizeShopEvent;
import com.snowgears.shop.util.EconomyUtils;
import com.snowgears.shop.util.InventoryUtils;
import com.snowgears.shop.util.ShopMessage;
import com.snowgears.shop.util.UtilMethods;
import com.snowgears.shop.util.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


public class MiscListener implements Listener {
    private final Shop plugin;

    public MiscListener(Shop instance) {
        this.plugin = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Block b = event.getBlockClicked();
        if (Tag.WALL_SIGNS.isTagged(b.getType())) {
            Directional sign = (Directional) b.getState().getBlockData();
            AbstractShop shop = plugin.getShopHandler().getShopByChest(b.getRelative(sign.getFacing().getOppositeFace()));
            if (shop != null) {
                event.setCancelled(true);
            }
        }
        Block blockToFill = event.getBlockClicked().getRelative(event.getBlockFace());
        AbstractShop shop = plugin.getShopHandler().getShopByChest(blockToFill.getRelative(BlockFace.DOWN));
        if (shop != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onShopCreation(SignChangeEvent event) {
        Block b = event.getBlock();
        Player player = event.getPlayer();
        if (!(b.getState() instanceof Sign)) {
            return;
        }
        BlockData data = b.getState().getBlockData();
        BlockFace facing;
        if (data instanceof WallSign) {
            facing = ((Directional) data).getFacing();
        } else {
            facing = ((Rotatable) data).getRotation();
        }
        Block chest = b.getRelative(facing.getOppositeFace());
        double priceCombo = 0.0;
        boolean isAdmin = false;
        if (plugin.getShopHandler().isChest(chest)) {
            Sign signBlock = (Sign) b.getState();
            String[] lines = event.getLines();
            if (lines[0].toLowerCase().contains(ShopMessage.getCreationWord("SHOP").toLowerCase())) {
                int numberOfShops = plugin.getShopHandler().getNumberOfShops(player);
                int buildPermissionNumber = plugin.getShopListener().getBuildLimit(player);
                if (plugin.usePerms() && !player.isOp() && !player.hasPermission("shop.operator") && numberOfShops >= buildPermissionNumber) {
                    event.setCancelled(true);
                    AbstractShop tempShop = new SellShop(null, player.getUniqueId(), 0.0, 0, false);
                    player.sendMessage(ShopMessage.getMessage("permission", "buildLimit", tempShop, player));
                    return;
                }
                if (plugin.getWorldBlacklist().contains(b.getLocation().getWorld().getName()) && !player.isOp() && (!plugin.usePerms() || !player.hasPermission("shop.operator"))) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "worldBlacklist", null, player));
                    event.setCancelled(true);
                    return;
                }
                boolean canCreateShopInRegion = true;
                try {
                    canCreateShopInRegion = WorldGuardHook.canCreateShop(player, b.getLocation());
                } catch (NoClassDefFoundError ignore) {
                }
                if (!canCreateShopInRegion) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "regionRestriction", null, player));
                    event.setCancelled(true);
                    return;
                }
                int amount;
                try {
                    String line2 = UtilMethods.cleanNumberText(lines[1]);
                    amount = Integer.parseInt(line2);
                    if (amount < 1) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                        return;
                    }
                } catch (NumberFormatException e2) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "line2", null, player));
                    return;
                }
                ShopType type = ShopType.SELL;
                if (plugin.usePerms() && !player.hasPermission("shop.create.sell")) {
                    type = ShopType.BUY;
                    if (!player.hasPermission("shop.create.buy")) {
                        type = ShopType.BARTER;
                    }
                }
                if (lines[3].toLowerCase().contains(ShopMessage.getCreationWord("SELL"))) {
                    type = ShopType.SELL;
                } else if (lines[3].toLowerCase().contains(ShopMessage.getCreationWord("BUY"))) {
                    type = ShopType.BUY;
                } else if (lines[3].toLowerCase().contains(ShopMessage.getCreationWord("BARTER"))) {
                    type = ShopType.BARTER;
                } else if (lines[3].toLowerCase().contains(ShopMessage.getCreationWord("GAMBLE"))) {
                    type = ShopType.GAMBLE;
                } else if (lines[3].toLowerCase().contains(ShopMessage.getCreationWord("COMBO"))) {
                    type = ShopType.COMBO;
                }
                double price;

                if (plugin.useVault()) {
                    try {
                        String line3 = UtilMethods.cleanNumberText(lines[2]);
                        String[] multiplePrices = line3.split(" ");
                        if (multiplePrices.length > 1) {
                            price = Integer.parseInt(multiplePrices[0]);
                            priceCombo = Integer.parseInt(multiplePrices[1]);
                        } else {
                            price = Integer.parseInt(line3);
                        }
                    } catch (NumberFormatException e2) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                } else {
                    try {
                        String line3 = UtilMethods.cleanNumberText(lines[2]);
                        String[] multiplePrices = line3.split(" ");
                        if (multiplePrices.length > 1) {
                            price = Integer.parseInt(multiplePrices[0]);
                            priceCombo = Integer.parseInt(multiplePrices[1]);
                        } else {
                            price = Integer.parseInt(line3);
                        }
                    } catch (NumberFormatException e2) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                        return;
                    }
                }

                if (price < 0.0 || (price == 0.0 && type == ShopType.BARTER)) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "line3", null, player));
                    return;
                }
                String playerMessage = null;
                AbstractShop tempShop2 = new SellShop(null, player.getUniqueId(), 0.0, 0, false);
                if (plugin.usePerms() && !player.hasPermission("shop.create." + type.toString().toLowerCase()) && !player.hasPermission("shop.create")) {
                    playerMessage = ShopMessage.getMessage("permission", "create", tempShop2, player);
                }
                if (type == ShopType.GAMBLE) {
                    isAdmin = true;
                }
                double cost = plugin.getCreationCost();
                if (cost > 0.0 && !EconomyUtils.hasSufficientFunds(player, player.getInventory(), cost)) {
                    playerMessage = ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", tempShop2, player);
                }
                if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    playerMessage = null;
                }
                AbstractShop existingShop = plugin.getShopHandler().getShopByChest(chest);
                if (existingShop != null && !existingShop.isAdmin() && !existingShop.getOwnerUUID().equals(player.getUniqueId())) {
                    playerMessage = ShopMessage.getMessage("interactionIssue", "createOtherPlayer", null, player);
                }
                if (playerMessage != null) {
                    player.sendMessage(playerMessage);
                    event.setCancelled(true);
                    return;
                }
                if (lines[3].toLowerCase().contains(ShopMessage.getCreationWord("ADMIN")) && (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator")))) {
                    isAdmin = true;
                }
                if (chest.getState().getBlockData() instanceof Directional && chest.getType() != Material.BARREL) {
                    Directional container = (Directional) chest.getState().getBlockData();
                    if (container.getFacing() != facing || !chest.getRelative(facing).getLocation().equals(signBlock.getLocation())) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "direction", null, player));
                        return;
                    }
                } else {
                    existingShop = plugin.getShopHandler().getShopByChest(chest);
                    if (existingShop != null) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "direction", null, player));
                        return;
                    }
                }
                chest.getRelative(facing).setType(UtilMethods.getWallEquivalentMaterial(signBlock.getType()));
                Sign newSign = (Sign) chest.getRelative(facing).getState();
                newSign.setType(UtilMethods.getWallEquivalentMaterial(signBlock.getType()));
                Directional newData = (Directional) newSign.getBlockData();
                newData.setFacing(facing);
                newSign.setBlockData(newData);
                newSign.update();
                signBlock.update();
                AbstractShop shop = AbstractShop.create(signBlock.getLocation(), player.getUniqueId(), price, priceCombo, amount, isAdmin, type);
                if (shop == null) {
                    return;
                }
                PlayerCreateShopEvent e = new PlayerCreateShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    return;
                }
                if (type == ShopType.GAMBLE) {
                    shop.setItemStack(plugin.getGambleDisplayItem());
                    shop.setAmount(1);
                    plugin.getShopHandler().addShop(shop);
                    shop.getDisplay().setType(DisplayType.LARGE_ITEM);
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                    plugin.getTransactionListener().sendEffects(true, player, shop);
                    plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    return;
                }
                plugin.getShopHandler().addShop(shop);
                shop.updateSign();
                player.sendMessage(ShopMessage.getMessage(type.toString(), "initialize", shop, player));
                if (type == ShopType.BUY && plugin.allowCreativeSelection()) {
                    player.sendMessage(ShopMessage.getMessage(type.toString(), "initializeAlt", shop, player));
                }
                new BukkitRunnable() {
                    public void run() {
                        if (!shop.isInitialized()) {
                            plugin.getShopHandler().removeShop(shop);
                            if (Tag.WALL_SIGNS.isTagged(b.getType())) {
                                Sign sign = (Sign) b.getState();
                                sign.setLine(0, ChatColor.RED + "SHOP CLOSED");
                                sign.setLine(1, ChatColor.GRAY + "CREATION TIMEOUT");
                                sign.setLine(2, "");
                                sign.setLine(3, "");
                                sign.update(true);
                                plugin.getCreativeSelectionListener().removePlayerData(player);
                            }
                        }
                    }
                }.runTaskLater(plugin, 1200L);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPreShopSignClick(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block clicked = event.getClickedBlock();
            if (clicked == null) {
                return;
            }
            if (Tag.WALL_SIGNS.isTagged(clicked.getType())) {
                AbstractShop shop = plugin.getShopHandler().getShop(clicked.getLocation());
                if (shop == null) {
                    return;
                }
                if (shop.isInitialized()) {
                    return;
                }
                if (!player.getUniqueId().equals(shop.getOwnerUUID()) && ((!plugin.usePerms() && !player.isOp()) || (plugin.usePerms() && !player.hasPermission("shop.operator")))) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "initialize", null, player));
                    plugin.getTransactionListener().sendEffects(false, player, shop);
                    event.setCancelled(true);
                    return;
                }
                if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
                    return;
                }
                if (Shop.getPlugin().getDisplayType() != DisplayType.NONE) {
                    Block aboveShop = shop.getChestLocation().getBlock().getRelative(BlockFace.UP);
                    if (!UtilMethods.materialIsNonIntrusive(aboveShop.getType())) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "displayRoom", null, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }
                double cost = plugin.getCreationCost();
                if (cost > 0.0 && !shop.isAdmin()) {
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if (!removed) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "createInsufficientFunds", shop, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                }
                ItemStack shopItem = player.getInventory().getItemInMainHand();
                if (shop.getItemStack() == null) {
                    PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                    Bukkit.getServer().getPluginManager().callEvent(e);
                    if (e.isCancelled()) {
                        return;
                    }
                    if (shop.getItemStack() == null) {
                        shop.setItemStack(shopItem);
                    }
                    if (shop.getType() == ShopType.BARTER) {
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "initializeInfo", shop, player));
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "initializeBarter", shop, player));
                        if (plugin.allowCreativeSelection()) {
                            player.sendMessage(ShopMessage.getMessage("BUY", "initializeAlt", shop, player));
                        }
                    } else {
                        shop.getDisplay().spawn();
                        player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                        plugin.getTransactionListener().sendEffects(true, player, shop);
                        plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                    }
                } else if (shop.getSecondaryItemStack() == null) {
                    if (InventoryUtils.itemstacksAreSimilar(shop.getItemStack(), shopItem)) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "sameItem", null, player));
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                        event.setCancelled(true);
                        return;
                    }
                    PlayerInitializeShopEvent e = new PlayerInitializeShopEvent(player, shop);
                    Bukkit.getServer().getPluginManager().callEvent(e);
                    if (e.isCancelled()) {
                        return;
                    }
                    if (shop.getSecondaryItemStack() == null) {
                        shop.setSecondaryItemStack(shopItem);
                    }
                    shop.getDisplay().spawn();
                    player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "create", shop, player));
                    plugin.getTransactionListener().sendEffects(true, player, shop);
                    plugin.getShopHandler().saveShops(shop.getOwnerUUID());
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void shopDestroy(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Block b = event.getBlock();
        Player player = event.getPlayer();
        if (Tag.WALL_SIGNS.isTagged(b.getType())) {
            AbstractShop shop = plugin.getShopHandler().getShop(b.getLocation());
            if (shop == null) {
                return;
            }
            if (!shop.isInitialized()) {
                event.setCancelled(true);
                return;
            }
            if (shop.getOwnerName().equals(player.getName())) {
                if (plugin.usePerms() && !player.hasPermission("shop.destroy") && !player.hasPermission("shop.operator")) {
                    event.setCancelled(true);
                    player.sendMessage(ShopMessage.getMessage("permission", "destroy", shop, player));
                    return;
                }
                double cost = plugin.getDestructionCost();
                if (cost > 0.0) {
                    boolean removed = EconomyUtils.removeFunds(player, player.getInventory(), cost);
                    if (!removed) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyInsufficientFunds", shop, player));
                        return;
                    }
                }
                PlayerDestroyShopEvent e = new PlayerDestroyShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "destroy", shop, player));
                shop.delete();
                plugin.getShopHandler().saveShops(shop.getOwnerUUID());
            } else if (player.isOp() || (plugin.usePerms() && (player.hasPermission("shop.operator") || player.hasPermission("shop.destroy.other")))) {
                PlayerDestroyShopEvent e2 = new PlayerDestroyShopEvent(player, shop);
                plugin.getServer().getPluginManager().callEvent(e2);
                if (e2.isCancelled()) {
                    event.setCancelled(true);
                    return;
                }
                player.sendMessage(ShopMessage.getMessage(shop.getType().toString(), "opDestroy", shop, player));
                shop.delete();
                plugin.getShopHandler().saveShops(shop.getOwnerUUID());
            } else {
                event.setCancelled(true);
            }
        } else if (plugin.getShopHandler().isChest(b)) {
            AbstractShop shop = plugin.getShopHandler().getShopByChest(b);
            if (shop == null) {
                return;
            }
            InventoryHolder ih = ((InventoryHolder) b.getState()).getInventory().getHolder();
            if (ih instanceof DoubleChest) {
                if (shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    if (shop.getChestLocation().equals(b.getLocation())) {
                        player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyChest", null, player));
                        event.setCancelled(true);
                        plugin.getTransactionListener().sendEffects(false, player, shop);
                    } else {
                        PlayerResizeShopEvent e3 = new PlayerResizeShopEvent(player, shop, b.getLocation(), false);
                        Bukkit.getPluginManager().callEvent(e3);
                        if (e3.isCancelled()) {
                            event.setCancelled(true);
                        }
                    }
                } else {
                    event.setCancelled(true);
                }
            } else {
                if (shop.getOwnerUUID().equals(player.getUniqueId()) || player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                    player.sendMessage(ShopMessage.getMessage("interactionIssue", "destroyChest", null, player));
                    plugin.getTransactionListener().sendEffects(false, player, shop);
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onShopExpansion(BlockPlaceEvent event) {
        Block b = event.getBlockPlaced();
        Player player = event.getPlayer();
        if (plugin.getShopHandler().isChest(b)) {
            AbstractShop shop = plugin.getShopHandler().getShopNearBlock(b);
            if (shop == null || b.getType() != shop.getChestLocation().getBlock().getType()) {
                return;
            }
            if (b.getType() == Material.ENDER_CHEST || b.getType() == Material.BARREL) {
                return;
            }
            Block shopChestBlock = shop.getChestLocation().getBlock();
            if (shopChestBlock.getRelative(shop.getFacing().getOppositeFace()).getLocation().equals(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
            BlockFace chestFacing = ((Directional) b.getState().getBlockData()).getFacing();
            if (chestFacing == shop.getFacing().getOppositeFace()) {
                event.setCancelled(true);
                return;
            }
            if (shop.getOwnerName().equals(player.getName())) {
                PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    event.setCancelled(true);
                }
            } else if (player.isOp() || (plugin.usePerms() && player.hasPermission("shop.operator"))) {
                PlayerResizeShopEvent e = new PlayerResizeShopEvent(player, shop, b.getLocation(), true);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) {
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }
}
