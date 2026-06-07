package net.ariatus.project.npc.player;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import net.ariatus.project.AriatusNPCs;
import org.bukkit.Bukkit;

public class ProtocolLibHook {

    private final AriatusNPCs module;
    private ProtocolManager protocolManager;
    private PlayerNPCPacketService packetService;

    public ProtocolLibHook(AriatusNPCs module) {
        this.module = module;
    }

    public boolean load() {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            module.logger().warn(module, "ProtocolLib no está instalado. NPCs tipo PLAYER desactivados.");
            return false;
        }

        try {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            this.packetService = new PlayerNPCPacketService(
                    module,
                    protocolManager,
                    new net.ariatus.project.npc.skin.SkinResolver(module)
            );

            module.logger().info(module, "ProtocolLib detectado correctamente.");
            return true;
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo inicializar ProtocolLib: " + exception.getMessage());
            return false;
        }
    }

    public ProtocolManager protocolManager() {
        return protocolManager;
    }

    public PlayerNPCPacketService packetService() {
        return packetService;
    }

    public boolean available() {
        return protocolManager != null && packetService != null;
    }
}