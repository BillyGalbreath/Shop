package com.snowgears.shop.event;

import com.snowgears.shop.AbstractShop;
import org.bukkit.entity.Player;

public class PlayerInitializeShopEvent extends ShopEvent {
    public PlayerInitializeShopEvent(Player player, AbstractShop shop) {
        super(player, shop);
    }
}
