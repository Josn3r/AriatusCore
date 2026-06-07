package net.ariatus.project.npc.equipment;

import net.ariatus.project.npc.NPCManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class NPCEquipmentListener implements Listener {

    private final NPCManager npcManager;

    public NPCEquipmentListener(NPCManager npcManager) {
        this.npcManager = npcManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NPCEquipmentMenuHolder holder)) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot >= event.getInventory().getSize()) {
            return;
        }

        NPCEquipmentSlot equipmentSlot = equipmentSlot(slot);

        if (equipmentSlot == null) {
            event.setCancelled(true);
            return;
        }

        var optionalNPC = npcManager.internalNPC(holder.npcId());

        if (optionalNPC.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        var npc = optionalNPC.get();

        var cursor = event.getCursor();

        if (cursor == null || cursor.getType() == Material.AIR) {
            npc.equipment().remove(equipmentSlot);
        } else {
            npc.equipment().put(equipmentSlot, cursor.clone());
        }

        npcManager.saveAndRefresh(npc);

        event.setCancelled(true);

        new NPCEquipmentMenu().open((org.bukkit.entity.Player) event.getWhoClicked(), npc);
    }

    private NPCEquipmentSlot equipmentSlot(int slot) {
        return switch (slot) {
            case NPCEquipmentMenu.HELMET_SLOT -> NPCEquipmentSlot.HELMET;
            case NPCEquipmentMenu.CHESTPLATE_SLOT -> NPCEquipmentSlot.CHESTPLATE;
            case NPCEquipmentMenu.LEGGINGS_SLOT -> NPCEquipmentSlot.LEGGINGS;
            case NPCEquipmentMenu.BOOTS_SLOT -> NPCEquipmentSlot.BOOTS;
            case NPCEquipmentMenu.MAIN_HAND_SLOT -> NPCEquipmentSlot.MAIN_HAND;
            case NPCEquipmentMenu.OFF_HAND_SLOT -> NPCEquipmentSlot.OFF_HAND;
            default -> null;
        };
    }
}