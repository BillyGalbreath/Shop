package com.snowgears.shop.util;


import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class DisplayUtil {
    //main groups of angles
    public static EulerAngle itemAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(0), Math.toRadians(0));
    public static EulerAngle toolAngle = new EulerAngle(Math.toRadians(-100), Math.toRadians(-90), Math.toRadians(0));

    //very specific case angles
    public static EulerAngle rodAngle = new EulerAngle(Math.toRadians(-80), Math.toRadians(-90), Math.toRadians(0));
    public static EulerAngle bowAngle = new EulerAngle(Math.toRadians(-90), Math.toRadians(5), Math.toRadians(-10));
    public static EulerAngle shieldAngle = new EulerAngle(Math.toRadians(90), Math.toRadians(0), Math.toRadians(0));

    //this spawns an armorstand at a location, with the item on it
    public static ArmorStand createDisplay(ItemStack itemStack, Location blockLocation, BlockFace facing) {
        EquipmentSlot itemType = getItemType(itemStack);

        Location standLocation = getStandLocation(blockLocation, itemStack.getType(), facing, itemType);
        ArmorStand stand = (ArmorStand) blockLocation.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);

        stand.setSmall(true);
        stand.setGravity(false); // use to be false
        stand.setVisible(false);
        stand.setBasePlate(false);

        EntityEquipment equipment = stand.getEquipment();
        if (equipment == null) {
            return stand;
        }

        switch (itemType) {
            case HEAD:
                equipment.setHelmet(itemStack);
                break;
            case CHEST:
                equipment.setChestplate(itemStack);
                break;
            case LEGS:
                equipment.setLeggings(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case FEET:
                equipment.setBoots(itemStack);
                //TODO set legs pose to be slightly spread
                break;
            case HAND:
                equipment.setItemInMainHand(itemStack);
                stand.setRightArmPose(getArmAngle(itemStack));
                if (itemStack.getType() != Material.SHIELD) {
                    stand.setSmall(false);
                }
        }

        return stand;
    }

    public static EquipmentSlot getItemType(ItemStack itemStack) {

        Material type = itemStack.getType();
        String sType = type.toString().toUpperCase();

        if (sType.contains("_HELMET") || type == Material.PLAYER_HEAD || type.isBlock()) {
            return EquipmentSlot.HEAD;
        } else if (sType.contains("_CHESTPLATE") || type == Material.ELYTRA) {
            return EquipmentSlot.CHEST;
        } else if (sType.contains("_LEGGINGS")) {
            return EquipmentSlot.LEGS;
        } else if (sType.contains("_BOOTS")) {
            return EquipmentSlot.FEET;
        }
        return EquipmentSlot.HAND;
    }

    public static Location getStandLocation(Location blockLocation, Material material, BlockFace facing, EquipmentSlot itemType) {

        Location standLocation = blockLocation;
        switch (itemType) {
            case HEAD:
                standLocation = blockLocation.clone().add(0.5, -.7, 0.5);
                break;
            case CHEST:
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
                                standLocation = standLocation.add(rodOffset, 0, 0);
                            }
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.425, -1.3, 0.7);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(0, 0, rodOffset);
                            }
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.3, -1.3, 0.42);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(-rodOffset, 0, 0);
                            }
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.6, -1.3, 0.3);
                            if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
                                standLocation = standLocation.add(0, 0, -rodOffset);
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
                            standLocation = blockLocation.clone().add(0, -0.8, 0.15);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.85, -0.8, 0);
                            break;
                    }
                } else if (material.name().endsWith("BANNER")) {
                    switch (facing) {
                        case NORTH:
                            standLocation = blockLocation.clone().add(0.99, -1.4, 0.86);
                            break;
                        case EAST:
                            standLocation = blockLocation.clone().add(0.12, -1.4, 1);
                            break;
                        case SOUTH:
                            standLocation = blockLocation.clone().add(0.01, -1.4, 0.12);
                            break;
                        case WEST:
                            standLocation = blockLocation.clone().add(0.88, -1.4, 0);
                            break;
                    }
                }
                //the material is a simple, default item
                else {
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
                    standLocation.add(0, 1, 0);
                    switch (facing) {
                        case NORTH:
                            standLocation.add(0.17, 0, -0.2);
                            break;
                        case EAST:
                            standLocation.add(0.2, 0, 0.17);
                            break;
                        case SOUTH:
                            standLocation.add(-0.17, 0, 0.2);
                            break;
                        case WEST:
                            standLocation.add(-0.2, 0, -0.17);
                            break;
                    }
                }
                break;
        }

        if (facing == null) {
            facing = BlockFace.NORTH;
        }

        //make the stand face the correct direction when it spawns
        standLocation.setYaw(blockfaceToYaw(facing));
        //fences and bows and shields are always 90 degrees off
        if (isFence(material) || material == Material.BOW || material.name().endsWith("BANNER") || material == Material.SHIELD) {
            standLocation.setYaw(blockfaceToYaw(nextFace(facing)));
        }

        if (blockLocation.getBlock().getState() instanceof ShulkerBox || blockLocation.getBlock().getRelative(BlockFace.DOWN).getState() instanceof ShulkerBox) {
            standLocation.add(0, 0.1, 0);
        }

        //material is an item
        return standLocation;
    }

    public static EulerAngle getArmAngle(ItemStack itemStack) {

        Material material = itemStack.getType();

        if (isTool(material) && !(material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK)) {
            return toolAngle;
        } else if (material == Material.BOW) {
            return bowAngle;
        } else if (material == Material.FISHING_ROD || material == Material.CARROT_ON_A_STICK) {
            return rodAngle;
        } else if (material == Material.SHIELD) {
            return shieldAngle;
        }
        return itemAngle;
    }

    /**
     * Converts a BlockFace direction to a yaw (float) value
     * Return:
     * - float: the yaw value of the BlockFace direction provided
     */
    public static float blockfaceToYaw(BlockFace bf) {
        if (bf.equals(BlockFace.SOUTH)) {
            return 0F;
        } else if (bf.equals(BlockFace.WEST)) {
            return 90F;
        } else if (bf.equals(BlockFace.NORTH)) {
            return 180F;
        } else if (bf.equals(BlockFace.EAST)) {
            return 270F;
        }
        return 0F;
    }

    public static boolean isTool(Material material) {
        String sMaterial = material.name();
        return (sMaterial.contains("_AXE") || sMaterial.contains("_HOE") || sMaterial.contains("_PICKAXE")
                || sMaterial.contains("_SPADE") || sMaterial.contains("_SWORD")
                || material == Material.BONE || material == Material.STICK || material == Material.BLAZE_ROD
                || material == Material.CARROT_ON_A_STICK || material == Material.FISHING_ROD);
    }

    public static boolean isFence(Material material) {
        String sMaterial = material.name();
        return (sMaterial.contains("FENCE") && (material != Material.IRON_BARS) && !sMaterial.contains("GATE"));
    }

    private static final BlockFace[] FACES = {BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};

    public static BlockFace nextFace(BlockFace face) {
        for (int i = 0; i < FACES.length; ++i) {
            if (face == FACES[i]) {
                return FACES[i + 1];
            }
        }
        return FACES[0];
    }
}
