package net.ariatus.project.storage;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class InventorySerializer {

    private InventorySerializer() {
    }

    public static String serialize(Inventory inventory) {
        try {
            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            BukkitObjectOutputStream objectOutput = new BukkitObjectOutputStream(byteOutput);

            objectOutput.writeInt(inventory.getSize());

            for (int slot = 0; slot < inventory.getSize(); slot++) {
                objectOutput.writeObject(inventory.getItem(slot));
            }

            objectOutput.close();

            return Base64.getEncoder().encodeToString(byteOutput.toByteArray());
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo serializar el inventario.", exception);
        }
    }

    public static ItemStack[] deserialize(String content, int expectedSize) {
        ItemStack[] items = new ItemStack[expectedSize];

        if (content == null || content.isBlank()) {
            return items;
        }

        try {
            byte[] data = Base64.getDecoder().decode(content);
            ByteArrayInputStream byteInput = new ByteArrayInputStream(data);
            BukkitObjectInputStream objectInput = new BukkitObjectInputStream(byteInput);

            int savedSize = objectInput.readInt();
            int size = Math.min(savedSize, expectedSize);

            for (int slot = 0; slot < savedSize; slot++) {
                Object object = objectInput.readObject();

                if (slot < size && object instanceof ItemStack itemStack) {
                    items[slot] = itemStack;
                }
            }

            objectInput.close();

            return items;
        } catch (Exception exception) {
            return items;
        }
    }
}