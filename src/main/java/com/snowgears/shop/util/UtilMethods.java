package com.snowgears.shop.util;


import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Sign;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class UtilMethods {

    private static ArrayList<Material> nonIntrusiveMaterials = new ArrayList<Material>();

    public static boolean isNumber(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isDouble(String s) {
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static BlockFace yawToFace(float yaw) {
        final BlockFace[] axis = {BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST};
        return axis[Math.round(yaw / 90f) & 0x3];
    }

    public static float faceToYaw(BlockFace bf) {
        switch(bf){
            case NORTH:
                return 180;
            case NORTH_EAST:
                return 225;
            case EAST:
                return 270;
            case SOUTH_EAST:
                return 315;
            case SOUTH:
                return 0;
            case SOUTH_WEST:
                return 45;
            case WEST:
                return 90;
            case NORTH_WEST:
                return 135;
        }
        return 180;
    }

    public static String capitalize(String line) {
        String[] spaces = line.split("\\s+");
        String capped = "";
        for (String s : spaces) {
            if (s.length() > 1)
                capped = capped + Character.toUpperCase(s.charAt(0)) + s.substring(1) + " ";
            else {
                capped = capped + s.toUpperCase() + " ";
            }
        }
        return capped.substring(0, capped.length()-1);
    }

    public static String getCleanLocation(Location loc, boolean includeWorld){
        String text = "";
        if(includeWorld)
            text = loc.getWorld().getName() + " - ";
        text = text + "("+ loc.getBlockX() + ", "+loc.getBlockY() + ", "+loc.getBlockZ() + ")";
        return text;
    }

    public static Location getLocation(String cleanLocation){
        World world = null;

        if(cleanLocation.contains(" - ")) {
            int dashIndex = cleanLocation.indexOf(" - ");
            world = Bukkit.getWorld(cleanLocation.substring(0, dashIndex));
            cleanLocation = cleanLocation.substring(dashIndex+1, cleanLocation.length());
        }
        else {
            world = Bukkit.getWorld("world");
        }
        cleanLocation = cleanLocation.replaceAll("[^\\d-]", " ");

        String[] sp = cleanLocation.split("\\s+");

        try {
            return new Location(world, Integer.valueOf(sp[1]), Integer.valueOf(sp[2]), Integer.valueOf(sp[3]));
        } catch (Exception e){
            return null;
        }
    }

    // Returns whether or not a player clicked the left or right side of a wall sign
    // 1 - LEFT SIDE
    // -1 - RIGHT SIDE
    // 0 - EXACT CENTER
    public static int calculateSideFromClickedSign(Player player, Block signBlock){
        Sign s = (Sign)signBlock.getState().getData();
        Location chest = signBlock.getRelative(s.getAttachedFace()).getLocation().add(0.5,0.5,0.5);
        Location head = player.getLocation().add(0, player.getEyeHeight(), 0);

        Vector direction = head.subtract(chest).toVector().normalize();
        Vector look = player.getLocation().getDirection().normalize();

        Vector cp = direction.crossProduct(look);

        //System.out.println("CROSS: "+cp);

        double d = 0;
        switch(s.getAttachedFace()){
            case NORTH:
                d = cp.getZ();
                break;
            case SOUTH:
                d = cp.getZ() * -1;
                break;
            case EAST:
                d = cp.getX() * -1;
                break;
            case WEST:
                d = cp.getX();
                break;
            default:
                break;
        }

        if(d > 0)
            return 1;
        else if(d < 0)
            return -1;
        else
            return 0;
    }

    public static String convertDurationToString(int duration) {
        duration = duration / 20;
        if (duration < 10)
            return "0:0" + duration;
        else if (duration < 60)
            return "0:" + duration;
        double mins = duration / 60;
        double secs = (mins - (int) mins);
        secs = (double) Math.round(secs * 100000) / 100000; //round to 2 decimal places
        if (secs == 0)
            return (int) mins + ":00";
        else if (secs < 10)
            return (int) mins + ":0" + (int) secs;
        else
            return (int) mins + ":" + (int) secs;
    }

    public static int getDurabilityPercent(ItemStack item) {
        if (item.getType().getMaxDurability() > 0) {
            double dur = ((double)(item.getType().getMaxDurability() - item.getDurability()) / (double)item.getType().getMaxDurability());
            return (int)(dur * 100);
        }
        return 100;
    }

    public static ItemStack getItemStack(String typeIdAndData){
        int index = typeIdAndData.indexOf(':');
        if(index == -1){
            int id = Integer.parseInt(typeIdAndData);
            return new ItemStack(id);
        }
        int id = Integer.parseInt(typeIdAndData.substring(0, index));
        int data = Integer.parseInt(typeIdAndData.substring(index+1));
        return new ItemStack(id, 1, (short)data);
    }

    public static String getItemName(ItemStack is){
        ItemMeta itemMeta = is.getItemMeta();

        if (itemMeta.getDisplayName() != null)
            return itemMeta.getDisplayName();
        else
            return capitalize(is.getType().name().replace("_", " ").toLowerCase());
    }

    public static boolean stringStartsWithUUID(String name){
        if (name != null && name.length() > 35){
            try {
                if (UUID.fromString(name.substring(0, 36)) != null)
                    return true;
            } catch (Exception ex) {
                return false;
            }
        }
        return false;
    }

    public static boolean containsLocation(String s){
        if(s == null)
            return false;
        if(s.startsWith("***{")){
            if((s.indexOf(',') != s.lastIndexOf(',')) && s.indexOf('}') != -1)
                return true;
        }
        return false;
    }

    public static boolean basicLocationMatch(Location loc1, Location loc2){
        return (loc1.getBlockX() == loc2.getBlockX() && loc1.getBlockY() == loc2.getBlockY() && loc1.getBlockZ() == loc2.getBlockZ());
    }

    public static boolean materialIsNonIntrusive(Material material){
        if(nonIntrusiveMaterials.isEmpty()){
            initializeNonIntrusiveMaterials();
        }

        return (nonIntrusiveMaterials.contains(material));
    }

    private static void initializeNonIntrusiveMaterials(){
        for(Material m : Material.values()){
            if(!m.isSolid())
                nonIntrusiveMaterials.add(m);
        }
        nonIntrusiveMaterials.add(Material.ACACIA_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.BIRCH_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.DARK_OAK_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.JUNGLE_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.OAK_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.SPRUCE_WALL_SIGN);
        nonIntrusiveMaterials.remove(Material.WATER);
        nonIntrusiveMaterials.remove(Material.LAVA);
        nonIntrusiveMaterials.remove(Material.FIRE);
        nonIntrusiveMaterials.remove(Material.END_PORTAL);
        nonIntrusiveMaterials.remove(Material.NETHER_PORTAL);
        nonIntrusiveMaterials.remove(Material.SKELETON_SKULL);
        nonIntrusiveMaterials.remove(Material.SKELETON_WALL_SKULL);
        nonIntrusiveMaterials.remove(Material.WITHER_SKELETON_SKULL);
        nonIntrusiveMaterials.remove(Material.WITHER_SKELETON_WALL_SKULL);
        nonIntrusiveMaterials.remove(Material.CREEPER_HEAD);
        nonIntrusiveMaterials.remove(Material.CREEPER_WALL_HEAD);
        nonIntrusiveMaterials.remove(Material.DRAGON_HEAD);
        nonIntrusiveMaterials.remove(Material.DRAGON_WALL_HEAD);
        nonIntrusiveMaterials.remove(Material.ZOMBIE_HEAD);
        nonIntrusiveMaterials.remove(Material.ZOMBIE_WALL_HEAD);
        nonIntrusiveMaterials.remove(Material.PLAYER_HEAD);
        nonIntrusiveMaterials.remove(Material.PLAYER_WALL_HEAD);
    }

    public static BlockFace getDirectionOfChest(Block block){
        byte rawDirectionData = block.getState().getData().getData();

        switch (rawDirectionData){
            case 2:
                return BlockFace.NORTH;
            case 5:
                return BlockFace.EAST;
            case 3:
                return BlockFace.SOUTH;
            case 4:
                return BlockFace.WEST;
            default:
                return BlockFace.NORTH;
        }
    }

    public static String cleanNumberText(String text){
        String cleaned = "";
        for(int i=0; i<text.length(); i++) {
            if(Character.isDigit(text.charAt(i)))
                cleaned += text.charAt(i);
            else if(text.charAt(i) == '.')
                cleaned += text.charAt(i);
            else if(text.charAt(i) == ' ')
                cleaned += text.charAt(i);
        }
        return cleaned;
    }

    public static ChatColor getChatColorByCode(String colorCode) {
        switch (colorCode) {
            case "&b":
                return ChatColor.AQUA;
            case "&0":
                return ChatColor.BLACK;
            case "&9":
                return ChatColor.BLUE;
            case "&l":
                return ChatColor.BOLD;
            case "&3":
                return ChatColor.DARK_AQUA;
            case "&1":
                return ChatColor.DARK_BLUE;
            case "&8":
                return ChatColor.DARK_GRAY;
            case "&2":
                return ChatColor.DARK_GREEN;
            case "&5":
                return ChatColor.DARK_PURPLE;
            case "&4":
                return ChatColor.DARK_RED;
            case "&6":
                return ChatColor.GOLD;
            case "&7":
                return ChatColor.GRAY;
            case "&a":
                return ChatColor.GREEN;
            case "&o":
                return ChatColor.ITALIC;
            case "&d":
                return ChatColor.LIGHT_PURPLE;
            case "&k":
                return ChatColor.MAGIC;
            case "&c":
                return ChatColor.RED;
            case "&r":
                return ChatColor.RESET;
            case "&m":
                return ChatColor.STRIKETHROUGH;
            case "&n":
                return ChatColor.UNDERLINE;
            case "&f" :
                return ChatColor.WHITE;
            case "&e":
                return ChatColor.YELLOW;
            default:
                return ChatColor.RESET;
        }
    }

    public static ChatColor getChatColor(String message) {
        if(message.startsWith("&") && message.length() > 1){
            ChatColor cc = getChatColorByCode(message.substring(0,2));
            if(cc != ChatColor.RESET)
                return cc;
        }
        return null;
    }

    public static boolean deleteDirectory(File directory) {
        if(directory.exists()){
            File[] files = directory.listFiles();
            if(null!=files){
                for(int i=0; i<files.length; i++) {
                    if(files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    }
                    else {
                        files[i].delete();
                    }
                }
            }
        }
        return(directory.delete());
    }

    public static void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
