package com.snowgears.shop.util;


import com.snowgears.shop.AbstractShop;
import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.UUID;

public class PlayerData {
    private final UUID playerUUID;
    private final Location shopSignLocation;
    private final GameMode oldGameMode;

    public PlayerData(Player player, Location shopSignLocation) {
        this.playerUUID = player.getUniqueId();
        this.shopSignLocation = shopSignLocation;
        this.oldGameMode = player.getGameMode();
        saveToFile();
    }

    private PlayerData(UUID playerUUID, GameMode oldGameMode, Location shopSignLocation) {
        this.playerUUID = playerUUID;
        this.oldGameMode = oldGameMode;
        this.shopSignLocation = shopSignLocation;
    }

    private void saveToFile() {
        try {
            File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");
            File creativeDirectory = new File(fileDirectory, "LimitedCreative");
            if (!creativeDirectory.exists() && !creativeDirectory.mkdir()) {
                return;
            }
            File playerDataFile = new File(creativeDirectory, playerUUID.toString() + ".yml");
            if (!playerDataFile.exists()) {
                playerDataFile.createNewFile();
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
            config.set("player.UUID", playerUUID.toString());
            config.set("player.gamemode", oldGameMode.toString());
            config.set("player.shopSignLocation", locationToString(shopSignLocation));
            config.save(playerDataFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static PlayerData loadFromFile(Player player) {
        if (player == null) {
            return null;
        }
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");
        File creativeDirectory = new File(fileDirectory, "LimitedCreative");
        if (!creativeDirectory.exists() && !creativeDirectory.mkdir()) {
            return null;
        }
        File playerDataFile = new File(creativeDirectory, player.getUniqueId().toString() + ".yml");
        if (playerDataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
            UUID uuid = UUID.fromString(config.getString("player.UUID"));
            GameMode gamemode = GameMode.valueOf(config.getString("player.gamemode"));
            Location signLoc = locationFromString(config.getString("player.shopSignLocation"));
            return new PlayerData(uuid, gamemode, signLoc);
        }
        return null;
    }

    public void apply() {
        Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }
        player.setGameMode(oldGameMode);
        removeFile();
    }

    private void removeFile() {
        File fileDirectory = new File(Shop.getPlugin().getDataFolder(), "Data");
        File creativeDirectory = new File(fileDirectory, "LimitedCreative");
        File playerDataFile = new File(creativeDirectory, playerUUID.toString() + ".yml");
        if (playerDataFile.exists()) {
            playerDataFile.delete();
        }
    }

    private static String locationToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private static Location locationFromString(String locString) {
        String[] parts = locString.split(",");
        return new Location(Bukkit.getServer().getWorld(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Location getShopSignLocation() {
        return shopSignLocation;
    }

    public AbstractShop getShop() {
        return Shop.getPlugin().getShopHandler().getShop(shopSignLocation);
    }

    public GameMode getOldGameMode() {
        return oldGameMode;
    }
}
