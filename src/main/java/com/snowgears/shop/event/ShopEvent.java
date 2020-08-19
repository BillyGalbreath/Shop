package com.snowgears.shop.event;

import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.ShopType;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class ShopEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final AbstractShop shop;
    private boolean cancelled;

    public ShopEvent(Player player, AbstractShop shop) {
        super(player);
        this.shop = shop;
    }

    public AbstractShop getShop() {
        return shop;
    }

    public ShopType getType() {
        return shop.getType();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean set) {
        cancelled = set;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
