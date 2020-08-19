package com.snowgears.shop.event;

import com.snowgears.shop.AbstractShop;
import org.bukkit.entity.Player;

public class PlayerExchangeShopEvent extends ShopEvent {
    public PlayerExchangeShopEvent(Player player, AbstractShop shop) {
        super(player, shop);
    }
}
