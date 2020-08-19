package com.snowgears.shop.util;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class DisplayUtil {
    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};

    private static final EulerAngle itemAngle = new EulerAngle(Math.toRadians(-90.0), Math.toRadians(0.0), Math.toRadians(0.0));
    private static final EulerAngle toolAngle = new EulerAngle(Math.toRadians(-100.0), Math.toRadians(-90.0), Math.toRadians(0.0));
    private static final EulerAngle rodAngle = new EulerAngle(Math.toRadians(-80.0), Math.toRadians(-90.0), Math.toRadians(0.0));
    private static final EulerAngle bowAngle = new EulerAngle(Math.toRadians(-90.0), Math.toRadians(5.0), Math.toRadians(-10.0));
    private static final EulerAngle shieldAngle = new EulerAngle(Math.toRadians(90.0), Math.toRadians(0.0), Math.toRadians(0.0));

    public static ArmorStand createDisplay(ItemStack itemStack, Location blockLocation, BlockFace facing) {
        ItemType itemType = getItemType(itemStack);
        Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, itemType);
        ArmorStand stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
        stand.setSmall(true);
        stand.setGravity(false);
        stand.setVisible(false);
        stand.setBasePlate(false);
        EntityEquipment equipment = stand.getEquipment();
        if (equipment != null) {
            switch (itemType) {
                case HEAD:
                    equipment.setHelmet(itemStack);
                    break;
                case BODY:
                    equipment.setChestplate(itemStack);
                    break;
                case LEGS:
                    equipment.setLeggings(itemStack);
                    break;
                case FEET:
                    equipment.setBoots(itemStack);
                    break;
                case HAND:
                    equipment.setItemInMainHand(itemStack);
                    stand.setRightArmPose(getArmAngle(itemStack));
                    if (itemStack.getType() != Material.SHIELD) {
                        stand.setSmall(false);
                    }
                    break;
            }
        }
        return stand;
    }

    private static ItemType getItemType(ItemStack itemStack) {
        Material type = itemStack.getType();
        String sType = type.toString().toUpperCase();
        if (sType.contains("_HELMET") || type == Material.PLAYER_HEAD || type.isBlock()) {
            return ItemType.HEAD;
        }
        if (sType.contains("_CHESTPLATE") || type == Material.ELYTRA) {
            return ItemType.BODY;
        }
        if (sType.contains("_LEGGINGS")) {
            return ItemType.LEGS;
        }
        if (sType.contains("_BOOTS")) {
            return ItemType.FEET;
        }
        return ItemType.HAND;
    }

    private static Location getStandLocation(Location blockLocation, Material material, BlockFace facing, ItemType itemType) {
        Location standLocation = null;
        switch (itemType) {
            case HEAD:
                standLocation = blockLocation.clone().add(0.5, -0.7, 0.5);
                break;
            case BODY:
                standLocation = blockLocation.clone().add(0.5, -0.3, 0.5);
                break;
            case LEGS:
                standLocation = blockLocation.clone().add(0.5, -0.1, 0.5);
                break;
            case FEET:
                standLocation = blockLocation.clone().add(0.5, 0.05, 0.5);
                break;
            case HAND:
                standLocation = blockLocation;
                if (isTool(material)) {
                    double rodOffset = 0.1;
                    switch (facing) {
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.7, -1.3, 0.6);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(rodOffset, 0.0, 0.0);
                            }
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.425, -1.3, 0.7);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(0.0, 0.0, rodOffset);
                            }
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.3, -1.3, 0.42);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(-rodOffset, 0.0, 0.0);
                            }
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.6, -1.3, 0.3);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(0.0, 0.0, -rodOffset);
                            }
                            break;
                    }
                } else if (material == Material.BOW) {
                    switch (facing) {
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.99, -0.8, 0.84);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.15, -0.8, 0.99);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.0, -0.8, 0.15);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.85, -0.8, 0.0);
                            break;
                    }
                } else if (Tag.BANNERS.isTagged(material)) {
                    switch (facing) {
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.99, -1.4, 0.86);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.12, -1.4, 1.0);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.01, -1.4, 0.12);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.88, -1.4, 0.0);
                            break;
                    }
                } else {
                    switch (facing) {
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.125, -1.4, 0.95);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.005, -1.4, 0.11);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.88, -1.4, 0.005);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.99, -1.4, 0.88);
                            break;
                    }
                }
                if (material == Material.SHIELD) {
                    standLocation.add(0.0, 1.0, 0.0);
                    switch (facing) {
                        case NORTH:
                            standLocation.add(0.17, 0.0, -0.2);
                            break;
                        case EAST:
                            standLocation.add(0.2, 0.0, 0.17);
                            break;
                        case SOUTH:
                            standLocation.add(-0.17, 0.0, 0.2);
                            break;
                        case WEST:
                            standLocation.add(-0.2, 0.0, -0.17);
                            break;
                    }
                }
                break;
        }
        if (facing == null) {
            facing = BlockFace.NORTH;
        }
        standLocation.setYaw(blockfaceToYaw(facing));
        if (isFence(material) || material == Material.BOW || Tag.BANNERS.isTagged(material) || material == Material.SHIELD) {
            standLocation.setYaw(blockfaceToYaw(nextFace(facing)));
        }
        if (blockLocation.getBlock().getState() instanceof ShulkerBox || blockLocation.getBlock().getRelative(BlockFace.DOWN).getState() instanceof ShulkerBox) {
            standLocation.add(0.0, 0.1, 0.0);
        }
        return standLocation;
    }

    private static EulerAngle getArmAngle(ItemStack itemStack) {
        Material material = itemStack.getType();
        if (isTool(material) && material != Material.FISHING_ROD && material != Material.CARROT_ON_A_STICK) {
            return toolAngle;
        }
        if (material == Material.BOW) {
            return bowAngle;
        }
        if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
            return rodAngle;
        }
        if (material == Material.SHIELD) {
            return shieldAngle;
        }
        return itemAngle;
    }

    private static float blockfaceToYaw(BlockFace bf) {
        if (bf.equals(BlockFace.SOUTH)) {
            return 0.0f;
        }
        if (bf.equals(BlockFace.WEST)) {
            return 90.0f;
        }
        if (bf.equals(BlockFace.NORTH)) {
            return 180.0f;
        }
        if (bf.equals(BlockFace.EAST)) {
            return 270.0f;
        }
        return 0.0f;
    }

    private static boolean isTool(Material material) {
        String sMaterial = material.name();
        return sMaterial.contains("_AXE") || sMaterial.contains("_HOE") || sMaterial.contains("_PICKAXE") || sMaterial.contains("_SPADE") || sMaterial.contains("_SWORD") || material == Material.BONE || material == Material.STICK || material == Material.BLAZE_ROD || material == Material.CARROT_ON_A_STICK || material == Material.FISHING_ROD;
    }

    private static boolean isFence(Material material) {
        String sMaterial = material.toString().toUpperCase();
        return sMaterial.contains("FENCE") && material != Material.IRON_BARS && !sMaterial.contains("GATE");
    }

    private static BlockFace nextFace(BlockFace face) {
        for (int i = 0; i < FACES.length; ++i) {
            if (face == FACES[i]) {
                return FACES[i + 1];
            }
        }
        return FACES[0];
    }

    public enum ItemType {
        HEAD,
        BODY,
        LEGS,
        FEET,
        HAND
    }
}
