package com.snowgears.shop.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ItemNameUtil {

    private Map<String, String> names = new HashMap<String, String>();

    public ItemNameUtil() {

        //no longer reading from items.tsv file as all item ids are deprecated. May revisit later with material names but removing for now
//        try {
//            File itemNameFile = new File(Shop.getPlugin().getDataFolder(), "items.tsv");
//            BufferedReader reader = new BufferedReader(new FileReader(itemNameFile));
//
//            String row;
//                while ((row = reader.readLine()) != null) {
//                    row = row.trim();
//                    if (row.isEmpty())
//                        continue;
//                    String[] cols = row.split("\t");
//                    String name = cols[2];
//                    String id = cols[0];
//                    String metadata = cols[1];
//                    //String idAndMetadata = metadata.equals("0") ? id : (id + ":" + metadata);
//                    String idAndMetadata = id+":"+metadata;
//                    names.put(idAndMetadata, name);
//                }
//            } catch (IOException e) {
//                System.out.println("[Shop] ERROR! Unable to initialize item name buffer reader. Using default spigot item names.");
//                return;
//            }
    }

    @SuppressWarnings("deprecation")
    public String getName(ItemStack item){
        if(item == null)
            return "";

        if(item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null && !item.getItemMeta().getDisplayName().isEmpty())
            return item.getItemMeta().getDisplayName();
//
//        String format = ""+item.getTypeId()+":"+item.getData().getData();
//        String name = names.get(format);
//        if(name != null)
//            return name;
//        return getBackupName(item.getType());

        return getBackupName(item.getType());
    }

    @SuppressWarnings("deprecation")
    public String getName(Material material){
//        String format = ""+material.getId()+":0";
//        String name = names.get(format);
//        if(name != null)
//            return name;
        return getBackupName(material);
    }

    private String getBackupName(Material material){
        return UtilMethods.capitalize(material.name().replace("_", " ").toLowerCase());
    }
}
