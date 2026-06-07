package net.ariatus.project;

import net.ariatus.project.api.npc.NPCService;
import net.ariatus.project.commands.NPCCommand;
import net.ariatus.project.module.ExternalAriatusModule;
import net.ariatus.project.module.ModuleStatus;
import net.ariatus.project.npc.*;
import net.ariatus.project.npc.animation.NPCAnimationService;
import net.ariatus.project.npc.equipment.NPCEquipmentListener;
import net.ariatus.project.npc.player.PlayerNPCInteractionListener;
import net.ariatus.project.npc.player.PlayerNPCViewerListener;
import net.ariatus.project.npc.player.ProtocolLibHook;

public class AriatusNPCs extends ExternalAriatusModule {

    private ModuleStatus status = ModuleStatus.DISABLED;

    private NPCConfigManager configManager;
    private NPCPropertyApplier propertyApplier;
    private NPCActionExecutor actionExecutor;
    private ProtocolLibHook protocolLibHook;
    private NPCManager npcManager;

    @Override
    public String id() {
        return "npcs";
    }

    @Override
    public String name() {
        return "AriatusNPCs";
    }

    @Override
    public void enable() {
        status = ModuleStatus.ENABLING;

        loadConfig("config.yml");
        loadConfig("npcs.yml");
        loadConfig("skins.yml");

        // NO CONFIG
        // TEST BUILD
        this.configManager = new NPCConfigManager(this);
        this.propertyApplier = new NPCPropertyApplier();
        this.actionExecutor = new NPCActionExecutor(this);
        this.protocolLibHook = new net.ariatus.project.npc.player.ProtocolLibHook(this);
        this.protocolLibHook.load();

        this.npcManager = new NPCManager(this, configManager, actionExecutor, propertyApplier, protocolLibHook);

        listeners().register(this, new NPCListener(npcManager));
        listeners().register(this, new PlayerNPCViewerListener(this, protocolLibHook, npcManager));
        listeners().register(this, new NPCEquipmentListener(npcManager));
        new PlayerNPCInteractionListener(this, protocolLibHook, npcManager).register();

        NPCAnimationService animationService = new NPCAnimationService(npcManager, protocolLibHook);
        commands().register(this, new NPCCommand(npcManager, animationService));
        services().register(NPCService.class, npcManager);
        this.core().getServer().getMessenger().registerOutgoingPluginChannel(this.core(), "BungeeCord");

        //tasks().runRepeating(this, () -> new NPCLookTask(npcManager, protocolLibHook), 20L, 5L);
        core().getServer().getScheduler().runTaskTimer(
                core(),
                new NPCLookTask(npcManager, protocolLibHook),
                20L,
                5L
        );

        npcManager.load();

        status = ModuleStatus.ENABLED;
        logger().info(this, "AriatusNPCs activado correctamente.");
    }

    @Override
    public void disable() {
        status = ModuleStatus.DISABLING;

        if (npcManager != null) {
            npcManager.despawnAll();
        }

        status = ModuleStatus.DISABLED;
        logger().info(this, "AriatusNPCs desactivado correctamente.");
    }

    @Override
    public ModuleStatus status() {
        return status;
    }
}