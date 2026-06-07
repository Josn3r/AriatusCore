package net.ariatus.project.npc.equipment;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class NPCEquipmentMenuHolder implements InventoryHolder {

    private final String npcId;

    public NPCEquipmentMenuHolder(String npcId) {
        this.npcId = npcId;
    }

    public String npcId() {
        return npcId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}