package com.snowgears.shop.gui;

import com.snowgears.shop.handler.ShopGuiHandler.GuiIcon;
import com.snowgears.shop.handler.ShopGuiHandler.GuiTitle;
import com.snowgears.shop.util.OfflinePlayerNameComparator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class ListPlayersWindow extends ShopGuiWindow {
    public ListPlayersWindow(UUID player) {
        super(player);
        page = Bukkit.createInventory(null, INV_SIZE, gui.getTitle(GuiTitle.LIST_PLAYERS));
        initInvContents();
    }

    @Override
    protected void initInvContents() {
        super.initInvContents();
        clearInvBody();
        makeMenuBarUpper();
        makeMenuBarLower();
        List<OfflinePlayer> owners = handler.getShopOwners();
        owners.sort(new OfflinePlayerNameComparator());
        int startIndex = pageIndex * 36;
        boolean added = true;
        for (int i = startIndex; i < owners.size(); ++i) {
            ItemStack icon = createIcon(owners.get(i));
            if (!addIcon(icon)) {
                added = false;
                break;
            }
        }
        if (added) {
            page.setItem(53, null);
        } else {
            page.setItem(53, getNextPageIcon());
        }
    }

    private ItemStack createIcon(OfflinePlayer owner) {
        if (handler.getAdminUUID().equals(owner.getUniqueId())) {
            return gui.getIcon(GuiIcon.LIST_PLAYER_ADMIN, owner, null);
        }
        return gui.getIcon(GuiIcon.LIST_PLAYER, owner, null);
    }

    @Override
    protected void makeMenuBarUpper() {
        super.makeMenuBarUpper();
    }

    @Override
    protected void makeMenuBarLower() {
        super.makeMenuBarLower();
    }
}
