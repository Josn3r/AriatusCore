package net.ariatus.project.storage;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class StorageChestHolder implements InventoryHolder {

    private final UUID uuid;
    private final int chestNumber;

    public StorageChestHolder(UUID uuid, int chestNumber) {
        this.uuid = uuid;
        this.chestNumber = chestNumber;
    }

    public UUID uuid() {
        return uuid;
    }

    public int chestNumber() {
        return chestNumber;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}