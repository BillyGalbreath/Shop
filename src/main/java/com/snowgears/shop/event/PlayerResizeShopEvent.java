package com.snowgears.shop.event;

import com.snowgears.shop.AbstractShop;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerResizeShopEvent extends ShopEvent {
    private final Location location;
    private final boolean isExpansion;

    public PlayerResizeShopEvent(Player player, AbstractShop shop, Location location, boolean isExpansion) {
        super(player, shop);
        this.location = location;
        this.isExpansion = isExpansion;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isExpansion() {
        return isExpansion;
    }
}
