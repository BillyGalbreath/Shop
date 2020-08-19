package com.snowgears.shop.gui;


import com.snowgears.shop.Shop;
import com.snowgears.shop.handler.ShopGuiHandler;
import com.snowgears.shop.handler.ShopHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public abstract class ShopGuiWindow {
    protected final int INV_SIZE = 54;
    protected Inventory page;
    protected ShopGuiWindow prevWindow;
    protected UUID player;
    private int currentSlot;
    protected int pageIndex;
    protected final ShopGuiHandler gui;
    protected final ShopHandler handler;

    public ShopGuiWindow(UUID player) {
        this.player = player;
        this.page = null;
        this.prevWindow = null;
        this.currentSlot = 9;
        this.gui = Shop.getPlugin().getGuiHandler();
        this.handler = Shop.getPlugin().getShopHandler();
    }

    public void scrollPageNext() {
        ItemStack nextPageIcon = page.getItem(53);
        if (nextPageIcon != null && nextPageIcon.getType() == Material.RED_STAINED_GLASS_PANE) {
            page.setItem(45, getPrevPageIcon());
            ++pageIndex;
            initInvContents();
        }
    }

    public void scrollPagePrev() {
        ItemStack nextPageIcon = page.getItem(45);
        if (nextPageIcon != null && nextPageIcon.getType() == Material.RED_STAINED_GLASS_PANE) {
            page.setItem(53, getNextPageIcon());
            --pageIndex;
            if (pageIndex == 0) {
                page.setItem(45, null);
            }
            initInvContents();
        }
    }

    public void setPrevWindow(ShopGuiWindow prev) {
        prevWindow = prev;
        page.setItem(0, getBackIcon());
    }

    public boolean hasPrevWindow() {
        return prevWindow != null;
    }

    protected boolean addIcon(ItemStack icon) {
        if (currentSlot == 45) {
            return false;
        }
        page.setItem(currentSlot, icon);
        ++currentSlot;
        return true;
    }

    public void open() {
        Player player = getPlayer();
        if (player != null) {
            player.openInventory(page);
        }
    }

    public boolean close() {
        Player player = getPlayer();
        if (player != null) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    protected void initInvContents() {
        currentSlot = 9;
    }

    protected void clearInvBody() {
        for (int i = 9; i < INV_SIZE - 9; ++i) {
            page.setItem(i, null);
        }
    }

    protected void makeMenuBarUpper() {
    }

    protected void makeMenuBarLower() {
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(player);
    }

    public Inventory getInventory() {
        return page;
    }

    protected ItemStack getPrevPageIcon() {
        ItemStack icon = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        icon.setDisplayName("Previous Page");
        return icon;
    }

    protected ItemStack getNextPageIcon() {
        ItemStack icon = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        icon.setDisplayName("Next Page");
        return icon;
    }

    protected ItemStack getBackIcon() {
        ItemStack icon = new ItemStack(Material.BARRIER);
        icon.setDisplayName("Back");
        return icon;
    }
}
