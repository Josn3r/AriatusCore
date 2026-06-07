package net.ariatus.project.npc.equipment;

import net.ariatus.project.message.MessageService;
import net.ariatus.project.npc.AriatusNPC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class NPCEquipmentMenu {

    public static final int SIZE = 54;

    public static final int HELMET_SLOT = 10;
    public static final int CHESTPLATE_SLOT = 19;
    public static final int LEGGINGS_SLOT = 28;
    public static final int BOOTS_SLOT = 37;
    public static final int MAIN_HAND_SLOT = 24;
    public static final int OFF_HAND_SLOT = 33;

    public void open(Player player, AriatusNPC npc) {
        Inventory inventory = Bukkit.createInventory(
                new NPCEquipmentMenuHolder(npc.id()),
                SIZE,
                MessageService.parse("&6Equipo NPC: &e" + npc.id())
        );

        fill(inventory);

        setSlot(inventory, HELMET_SLOT, npc, NPCEquipmentSlot.HELMET, Material.LEATHER_HELMET, "&eCasco");
        setSlot(inventory, CHESTPLATE_SLOT, npc, NPCEquipmentSlot.CHESTPLATE, Material.LEATHER_CHESTPLATE, "&ePechera");
        setSlot(inventory, LEGGINGS_SLOT, npc, NPCEquipmentSlot.LEGGINGS, Material.LEATHER_LEGGINGS, "&ePantalones");
        setSlot(inventory, BOOTS_SLOT, npc, NPCEquipmentSlot.BOOTS, Material.LEATHER_BOOTS, "&eBotas");
        setSlot(inventory, MAIN_HAND_SLOT, npc, NPCEquipmentSlot.MAIN_HAND, Material.IRON_SWORD, "&eMano principal");
        setSlot(inventory, OFF_HAND_SLOT, npc, NPCEquipmentSlot.OFF_HAND, Material.SHIELD, "&eMano secundaria");

        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        ItemStack filler = item(Material.GRAY_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private void setSlot(
            Inventory inventory,
            int slot,
            AriatusNPC npc,
            NPCEquipmentSlot equipmentSlot,
            Material placeholder,
            String name
    ) {
        ItemStack item = npc.equipment().get(equipmentSlot);

        if (item == null || item.getType().isAir()) {
            inventory.setItem(slot, item(placeholder, name + " &7(vacío)"));
            return;
        }

        inventory.setItem(slot, item.clone());
    }

    private ItemStack item(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(MessageService.parse(name));
        item.setItemMeta(meta);

        return item;
    }
}