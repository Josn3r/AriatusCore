package net.ariatus.project.protection;

import org.bukkit.Material;

import java.util.Set;

public record ProtectionSettings(
        String worldName,
        boolean enabled,
        boolean blockBreaking,
        boolean blockPlacing,
        Set<Material> blockedInteractions,
        Set<Material> blockedInventoryOpen,
        Set<Material> allowedInteractions
) {

    public boolean interactionBlocked(Material material) {
        if (allowedInteractions.contains(material)) {
            return false;
        }

        return blockedInteractions.contains(material);
    }

    public boolean inventoryBlocked(Material material) {
        return blockedInventoryOpen.contains(material);
    }
}