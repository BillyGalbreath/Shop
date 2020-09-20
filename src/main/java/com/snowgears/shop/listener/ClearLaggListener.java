package com.snowgears.shop.listener;

import com.snowgears.shop.Shop;
import org.bukkit.event.Listener;

//import me.minebuilders.clearlag.events.EntityRemoveEvent;

public class ClearLaggListener implements Listener {

    public Shop plugin = Shop.getPlugin();

    public ClearLaggListener(Shop instance) {
        plugin = instance;
    }

//    @EventHandler
//    public void onClearLagg(EntityRemoveEvent event) {
//        Iterator<Entity> iterator = event.getEntityList().iterator();
//        while (iterator.hasNext()) {
//            Entity e = iterator.next();
//            if (Display.isDisplay(e)) {
//                iterator.remove();
//            }
//        }
//    }
}