package net.ariatus.project.api.storage;

import java.util.UUID;

public interface StorageChestView {

    UUID uuid();

    int chestNumber();

    boolean unlocked();

    boolean vip();

    int rows();
}