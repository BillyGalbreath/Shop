package com.snowgears.shop.util;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UtilMethods {
    private static final List<Material> nonIntrusiveMaterials = new ArrayList<>();

    public static float faceToYaw(BlockFace face) {
        switch (face) {
            case NORTH:
                return 180.0f;
            case NORTH_EAST:
                return 225.0f;
            case EAST:
                return 270.0f;
            case SOUTH_EAST:
                return 315.0f;
            case SOUTH:
                return 0.0f;
            case SOUTH_WEST:
                return 45.0f;
            case WEST:
                return 90.0f;
            case NORTH_WEST:
                return 135.0f;
        }
        return 180.0f;
    }

    public static String getCleanLocation(Location loc, boolean includeWorld) {
        String text = "";
        if (includeWorld) {
            text = loc.getWorld().getName() + " - ";
        }
        return text + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }

    public static Location getLocation(String cleanLocation) {
        World world;
        if (cleanLocation.contains(" - ")) {
            int dashIndex = cleanLocation.indexOf(" - ");
            world = Bukkit.getWorld(cleanLocation.substring(0, dashIndex));
            cleanLocation = cleanLocation.substring(dashIndex + 1);
        } else {
            world = Bukkit.getWorld("world");
        }
        cleanLocation = cleanLocation.replaceAll("[^\\d-]", " ");
        String[] sp = cleanLocation.split("\\s+");
        try {
            return new Location(world, Integer.valueOf(sp[1]), Integer.valueOf(sp[2]), Integer.valueOf(sp[3]));
        } catch (Exception e) {
            return null;
        }
    }

    public static int calculateSideFromClickedSign(Player player, Block signBlock) {
        Directional s = (Directional) signBlock.getState().getBlockData();
        Location chest = signBlock.getRelative(s.getFacing().getOppositeFace()).getLocation().add(0.5, 0.5, 0.5);
        Location head = player.getLocation().add(0.0, player.getEyeHeight(), 0.0);
        Vector direction = head.subtract(chest).toVector().normalize();
        Vector look = player.getLocation().getDirection().normalize();
        Vector cp = direction.crossProduct(look);
        double d = 0.0;
        switch (s.getFacing().getOppositeFace()) {
            case NORTH:
                d = cp.getZ();
                break;
            case SOUTH:
                d = cp.getZ() * -1.0;
                break;
            case EAST:
                d = cp.getX() * -1.0;
                break;
            case WEST:
                d = cp.getX();
                break;
        }
        return Double.compare(d, 0.0);
    }

    public static int getDurabilityPercent(ItemStack item) {
        if (item.getType().getMaxDurability() > 0) {
            double dur = (item.getType().getMaxDurability() - InventoryUtils.getDurability(item)) / (double) item.getType().getMaxDurability();
            return (int) (dur * 100.0);
        }
        return 100;
    }

    public static boolean stringStartsWithUUID(String name) {
        if (name != null && name.length() > 35) {
            try {
                UUID.fromString(name.substring(0, 36));
                return true;
            } catch (Exception ignore) {
            }
        }
        return false;
    }

    public static boolean containsLocation(String s) {
        return s != null && s.startsWith("***{") && s.indexOf(',') != s.lastIndexOf(',') && s.indexOf('}') != -1;
    }

    public static boolean materialIsNonIntrusive(Material material) {
        if (nonIntrusiveMaterials.isEmpty()) {
            initializeNonIntrusiveMaterials();
        }
        return nonIntrusiveMaterials.contains(material);
    }

    private static void initializeNonIntrusiveMaterials() {
        for (Material m : Material.values()) {
            if (!m.isSolid()) {
                nonIntrusiveMaterials.add(m);
            }
        }
        nonIntrusiveMaterials.add(Material.ACACIA_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.BIRCH_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.DARK_OAK_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.JUNGLE_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.OAK_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.SPRUCE_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.CRIMSON_WALL_SIGN);
        nonIntrusiveMaterials.add(Material.WARPED_WALL_SIGN);
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

    public static String cleanNumberText(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ++i) {
            if (Character.isDigit(text.charAt(i))) {
                sb.append(text.charAt(i));
            } else if (text.charAt(i) == '.') {
                sb.append(text.charAt(i));
            } else if (text.charAt(i) == ' ') {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    public static ChatColor getChatColor(String message) {
        if (message.startsWith("&") && message.length() > 1) {
            ChatColor cc = ChatColor.getByChar(message.substring(1, 2));
            if (cc != ChatColor.RESET) {
                return cc;
            }
        }
        return null;
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

    public static boolean isWallSign(Material mat) {
        switch (mat) {
            case ACACIA_WALL_SIGN:
            case BIRCH_WALL_SIGN:
            case DARK_OAK_WALL_SIGN:
            case JUNGLE_WALL_SIGN:
            case OAK_WALL_SIGN:
            case SPRUCE_WALL_SIGN:
            case CRIMSON_WALL_SIGN:
            case WARPED_WALL_SIGN:
                return true;
            default:
                return false;
        }
    }

    public static Material getWallEquivalentMaterial(Material post) {
        switch (post) {
            case ACACIA_SIGN:
            case ACACIA_WALL_SIGN:
                return Material.ACACIA_WALL_SIGN;
            case BIRCH_SIGN:
            case BIRCH_WALL_SIGN:
                return Material.BIRCH_WALL_SIGN;
            case DARK_OAK_SIGN:
            case DARK_OAK_WALL_SIGN:
                return Material.DARK_OAK_WALL_SIGN;
            case JUNGLE_SIGN:
            case JUNGLE_WALL_SIGN:
                return Material.JUNGLE_WALL_SIGN;
            case SPRUCE_SIGN:
            case SPRUCE_WALL_SIGN:
                return Material.SPRUCE_WALL_SIGN;
            case CRIMSON_SIGN:
            case CRIMSON_WALL_SIGN:
                return Material.CRIMSON_WALL_SIGN;
            case WARPED_SIGN:
            case WARPED_WALL_SIGN:
                return Material.WARPED_WALL_SIGN;
            default:
                return Material.OAK_WALL_SIGN;
        }
    }
}
