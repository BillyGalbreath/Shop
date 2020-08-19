package com.snowgears.shop.listener;

import com.sk89q.craftbook.mechanics.ranged.RangedCollectEvent;
import com.snowgears.shop.display.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CraftbookListener implements Listener {
    @EventHandler
    public void onRangedCollect(final RangedCollectEvent event) {
        if (Display.isDisplay(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
