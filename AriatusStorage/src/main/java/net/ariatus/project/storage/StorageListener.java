package net.ariatus.project.storage;

import net.ariatus.project.AriatusStorage;
import net.ariatus.project.message.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class StorageListener implements Listener {

    private final AriatusStorage module;
    private final StorageManager storageManager;

    public StorageListener(AriatusStorage module, StorageManager storageManager) {
        this.module = module;
        this.storageManager = storageManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageMenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot < 9 || slot > 17) {
            return;
        }

        int chestNumber = slot - 8;

        if (!storageManager.validChest(chestNumber)) {
            return;
        }

        storageManager.unlocked(player.getUniqueId(), chestNumber).thenAccept(unlocked -> {
            module.tasks().runAsync(module, () -> {
                if (unlocked || storageManager.canAccessVipChest(player, chestNumber)) {
                    storageManager.openChest(player, chestNumber);
                    return;
                }
                storageManager.purchaseChest(player, chestNumber);
            });
        });
    }

    @EventHandler
    public void onChestClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof StorageChestHolder holder)) {
            return;
        }

        storageManager.saveChest(holder.uuid(), holder.chestNumber(), event.getInventory());

        if (event.getPlayer() instanceof Player player) {
            MessageService.send(player, "&aBaúl guardado.");
        }
    }
}