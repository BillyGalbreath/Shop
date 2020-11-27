package com.snowgears.shop.listener;

import com.sk89q.craftbook.mechanics.ranged.RangedCollectEvent;
import com.snowgears.shop.display.Display;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class CraftBookListener implements Listener {
    @EventHandler
    public void onRangedCollector(RangedCollectEvent event) {
        if (Display.isDisplay(event.getItem())) {
            event.setCancelled(true);
        }
    }
}
