package net.ariatus.project.storage;

import net.ariatus.project.api.storage.StorageChestView;

import java.util.UUID;

public class PlayerStorageChest implements StorageChestView {

    private final UUID uuid;
    private final int chestNumber;
    private final boolean unlocked;
    private final boolean vip;
    private final int rows;

    public PlayerStorageChest(UUID uuid, int chestNumber, boolean unlocked, boolean vip, int rows) {
        this.uuid = uuid;
        this.chestNumber = chestNumber;
        this.unlocked = unlocked;
        this.vip = vip;
        this.rows = rows;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public int chestNumber() {
        return chestNumber;
    }

    @Override
    public boolean unlocked() {
        return unlocked;
    }

    @Override
    public boolean vip() {
        return vip;
    }

    @Override
    public int rows() {
        return rows;
    }
}