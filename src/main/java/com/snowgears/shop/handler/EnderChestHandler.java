package com.snowgears.shop.handler;

import com.snowgears.shop.Shop;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EnderChestHandler {
    private final Shop plugin;
    private final HashMap<UUID, Inventory> enderChestInventories;
    private final ArrayList<UUID> playersSavingInventories;

    public EnderChestHandler(Shop plugin) {
        this.enderChestInventories = new HashMap<>();
        this.playersSavingInventories = new ArrayList<>();
        this.plugin = plugin;
        loadEnderChests();
    }

    public Inventory getInventory(OfflinePlayer player) {
        if (enderChestInventories.get(player.getUniqueId()) != null) {
            return enderChestInventories.get(player.getUniqueId());
        }
        if (player.getPlayer() != null) {
            return player.getPlayer().getEnderChest();
        }
        return null;
    }

    public void saveInventory(OfflinePlayer player, Inventory inv) {
        if (player.getUniqueId().equals(plugin.getShopHandler().getAdminUUID())) {
            return;
        }
        enderChestInventories.put(player.getUniqueId(), inv);
        if (playersSavingInventories.contains(player.getUniqueId())) {
            return;
        }
        new BukkitRunnable() {
            public void run() {
                playersSavingInventories.add(player.getUniqueId());
                saveInventoryDriver(player);
            }
        }.runTaskLaterAsynchronously(plugin, 20L);
    }

    private void saveInventoryDriver(OfflinePlayer player) {
        try {
            File fileDirectory = new File(plugin.getDataFolder(), "Data");
            if (!fileDirectory.exists() && !fileDirectory.mkdir()) {
                return;
            }
            File enderDirectory = new File(fileDirectory, "EnderChests");
            if (!enderDirectory.exists() && !enderDirectory.mkdir()) {
                return;
            }
            String owner = player.getName();
            File currentFile = new File(enderDirectory, owner + " (" + player.getUniqueId().toString() + ").yml");
            if (!currentFile.exists() && !currentFile.createNewFile()) {
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(currentFile);
            config.set("enderchest", getInventory(player).getContents());
            config.save(currentFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        playersSavingInventories.remove(player.getUniqueId());
    }

    private void loadEnderChests() {
        File fileDirectory = new File(plugin.getDataFolder(), "Data");
        if (!fileDirectory.exists() && !fileDirectory.mkdir()) {
            return;
        }
        File enderDirectory = new File(fileDirectory, "EnderChests");
        if (!enderDirectory.exists() && !enderDirectory.mkdir()) {
            return;
        }
        File[] files = enderDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".yml")) {
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                    loadEnderChestFromConfig(config, file.getName());
                }
            }
        }
    }

    private void loadEnderChestFromConfig(YamlConfiguration config, String fileName) {
        if (config.get("enderchest") == null) {
            return;
        }
        UUID owner = uidFromString(fileName);
        ItemStack[] contents = ((List<ItemStack>) config.get("enderchest")).toArray(new ItemStack[0]);
        Inventory inv = Bukkit.createInventory(null, InventoryType.ENDER_CHEST);
        inv.setContents(contents);
        enderChestInventories.put(owner, inv);
    }

    private UUID uidFromString(String ownerString) {
        int index = ownerString.indexOf("(");
        int lastIndex = ownerString.indexOf(")");
        String uidString = ownerString.substring(index + 1, lastIndex);
        return UUID.fromString(uidString);
    }
}
