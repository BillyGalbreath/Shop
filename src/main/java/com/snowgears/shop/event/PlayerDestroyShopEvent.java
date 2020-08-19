package com.snowgears.shop.event;

import com.snowgears.shop.AbstractShop;
import org.bukkit.entity.Player;

public class PlayerDestroyShopEvent extends ShopEvent {
    public PlayerDestroyShopEvent(Player player, AbstractShop shop) {
        super(player, shop);
    }
}
