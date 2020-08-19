package com.snowgears.shop.event;

import com.snowgears.shop.AbstractShop;
import org.bukkit.entity.Player;

public class PlayerCreateShopEvent extends ShopEvent {
    public PlayerCreateShopEvent(Player player, AbstractShop shop) {
        super(player, shop);
    }
}
