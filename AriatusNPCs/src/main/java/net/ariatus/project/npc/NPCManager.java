package net.ariatus.project.npc;

import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.api.npc.NPCClickType;
import net.ariatus.project.api.npc.NPCEngineType;
import net.ariatus.project.api.npc.NPCService;
import net.ariatus.project.api.npc.NPCView;
import net.ariatus.project.npc.engine.EntityNPCSpawnEngine;
import net.ariatus.project.npc.engine.NPCSpawnEngine;
import net.ariatus.project.npc.engine.PlayerNPCSpawnEngine;
import net.ariatus.project.npc.player.FakePlayerNPCData;
import net.ariatus.project.npc.player.FakePlayerRegistry;
import net.ariatus.project.npc.player.FakePlayerSeatData;
import net.ariatus.project.npc.player.ProtocolLibHook;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class NPCManager implements NPCService {

    private final AriatusNPCs module;
    private final NPCConfigManager configManager;
    private final NPCActionExecutor actionExecutor;
    private final Map<String, AriatusNPC> npcs = new LinkedHashMap<>();
    private final NPCSpawnEngine entityEngine;
    private final NPCSpawnEngine playerEngine;
    private final FakePlayerRegistry fakePlayerRegistry;
    private final NPCLabelManager labelManager;

    public NPCManager(
            AriatusNPCs module,
            NPCConfigManager configManager,
            NPCActionExecutor actionExecutor,
            NPCPropertyApplier propertyApplier,
            ProtocolLibHook protocolLibHook
    ) {
        this.module = module;
        this.configManager = configManager;
        this.actionExecutor = actionExecutor;
        this.fakePlayerRegistry = new FakePlayerRegistry();
        this.entityEngine = new EntityNPCSpawnEngine(module, propertyApplier);
        this.playerEngine = new PlayerNPCSpawnEngine(module, protocolLibHook, fakePlayerRegistry);
        this.labelManager = new NPCLabelManager();
    }

    public NPCLabelManager labelManager() {
        return labelManager;
    }

    public void load() {
        despawnAll();
        npcs.clear();
        npcs.putAll(configManager.loadNPCs());

        module.logger().info(module, "NPCs cargados: " + npcs.size());

        if (module.configBoolean("config.yml", "npcs.auto-spawn", true)) {
            spawnEnabled();
        }
    }

    public void spawnEnabled() {
        for (AriatusNPC npc : npcs.values()) {
            if (npc.enabled()) {
                spawn(npc.id());
            }
        }
    }

    public void despawnAll() {
        for (AriatusNPC npc : npcs.values()) {
            despawn(npc.id());
        }
        labelManager.despawnAll();
    }

    @Override
    public Collection<NPCView> npcs() {
        return java.util.Collections.unmodifiableCollection(npcs.values());
    }

    public Collection<AriatusNPC> internalNPCs() {
        return java.util.Collections.unmodifiableCollection(npcs.values());
    }

    @Override
    public Optional<NPCView> npc(String id) {
        return Optional.ofNullable(npcs.get(id.toLowerCase()));
    }

    public Optional<AriatusNPC> internalNPC(String id) {
        return Optional.ofNullable(npcs.get(id.toLowerCase()));
    }

    @Override
    public boolean spawn(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null || !npc.enabled()) {
            return false;
        }

        boolean spawned = engine(npc).spawn(npc);

        if (spawned && npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            labelManager.spawnOrUpdate(npc);
        }

        return spawned;
    }

    @Override
    public boolean despawn(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        labelManager.despawn(id);
        return engine(npc).despawn(npc);
    }

    @Override
    public boolean createEntity(String id, EntityType type, Location location) throws IOException {
        id = id.toLowerCase();

        if (npcs.containsKey(id)) {
            return false;
        }

        AriatusNPC npc = new AriatusNPC(
                id,
                NPCEngineType.ENTITY,
                type,
                location
        );

        npc.enabled(true);
        npc.displayName("&e" + id);
        npc.typeName(type.name());
        npc.nameVisible(true);
        npc.ai(false);
        npc.silent(true);
        npc.invulnerable(true);
        npc.gravity(true);
        npc.glowing(false);
        npc.collidable(false);
        npc.persistent(true);
        npc.size(1.0);

        npcs.put(npc.id(), npc);
        configManager.saveNPC(npc);

        return spawn(npc.id());
    }

    @Override
    public boolean createPlayer(String id, String skinName, Location location) {
        if (npcs.containsKey(id.toLowerCase())) {
            return false;
        }

        AriatusNPC npc = new AriatusNPC(
                id.toLowerCase(),
                NPCEngineType.PLAYER,
                EntityType.PLAYER,
                location
        );

        npc.enabled(true);
        npc.displayName("&e" + id);
        npc.typeName("PLAYER");
        npc.nameVisible(true);
        npc.ai(false);
        npc.silent(true);
        npc.invulnerable(true);
        npc.gravity(false);
        npc.glowing(false);
        npc.collidable(false);
        npc.persistent(true);
        npc.size(1.0);

        npcs.put(npc.id(), npc);

        return spawn(npc.id());
    }

    @Override
    public boolean delete(String id) throws IOException {
        id = id.toLowerCase();

        AriatusNPC npc = npcs.remove(id);

        if (npc == null) {
            return false;
        }

        engine(npc).despawn(npc);
        configManager.deleteNPC(id);
        return true;
    }

    @Override
    public boolean executeAction(String id, NPCClickType clickType, Player player) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        for (NPCAction action : npc.actions(clickType)) {
            actionExecutor.execute(player, npc, action);
        }

        return true;
    }

    public Optional<AriatusNPC> byEntity(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }

        return npcs.values()
                .stream()
                .filter(npc -> npc.entityUuid() != null)
                .filter(npc -> npc.entityUuid().equals(entity.getUniqueId()))
                .findFirst();
    }

    private NPCSpawnEngine engine(AriatusNPC npc) {
        return npc.engineType() == NPCEngineType.PLAYER ? playerEngine : entityEngine;
    }

    public boolean moveHere(String id, Location location) throws IOException {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        boolean wasSpawned = npc.spawned();

        if (wasSpawned) {
            despawn(id);
        }

        npc.location(location);
        configManager.saveNPC(npc);

        if (wasSpawned) {
            spawn(id);
        }

        return true;
    }

    public boolean rename(String id, String name) {
        return displayName(id, name);
    }

    public boolean nameVisible(String id, boolean visible) throws IOException {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.nameVisible(visible);
        configManager.saveNPC(npc);

        var entity = npc.entity();

        if (entity != null) {
            if (!visible) {
                entity.customName(null);
                entity.setCustomNameVisible(false);
            } else {
                entity.customName(net.ariatus.project.message.MessageService.parse(npc.displayName()));
                entity.setCustomNameVisible(true);
            }
        }

        return true;
    }

    public boolean glowing(String id, boolean glowing) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.glowing(glowing);
        npc.glowingColor(glowing ? NPCGlowingColor.WHITE : NPCGlowingColor.DISABLED);

        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            refresh(id);
            return true;
        }

        var entity = npc.entity();

        if (entity != null) {
            entity.setGlowing(glowing);
        }

        return true;
    }

    public boolean glowing(String id, NPCGlowingColor color) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.glowingColor(color);
        npc.glowing(color != NPCGlowingColor.DISABLED);

        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        refresh(id);

        return true;
    }

    public boolean ai(String id, boolean ai) throws IOException {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.ai(ai);
        configManager.saveNPC(npc);

        var entity = npc.entity();

        if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
            livingEntity.setAI(ai);
        }

        return true;
    }

    public boolean size(String id, double size) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null || size <= 0) {
            return false;
        }

        npc.size(size);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            refresh(id);
            return true;
        }

        var entity = npc.entity();

        if (entity instanceof org.bukkit.entity.LivingEntity livingEntity) {
            try {
                var scale = livingEntity.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                if (scale != null) {
                    scale.setBaseValue(size);
                }
            } catch (Exception ignored) {
            }
        }

        return true;
    }

    public boolean clearActions(String id, NPCClickType clickType) throws IOException {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.actions(clickType, java.util.List.of());
        configManager.saveNPC(npc);

        return true;
    }

    public java.util.Optional<FakePlayerNPCData> fakePlayerByEntityId(int entityId) {
        return fakePlayerRegistry.byEntityId(entityId);
    }

    public java.util.Collection<FakePlayerNPCData> fakePlayers() {
        return fakePlayerRegistry.all();
    }

    @Override
    public boolean isNPCScoreboardEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }

        String lowered = entry.toLowerCase();

        if (npcs.containsKey(lowered)) {
            return true;
        }

        for (AriatusNPC npc : npcs.values()) {
            if (npc.id().equalsIgnoreCase(entry)) {
                return true;
            }

            if (npc.displayName() != null && npc.displayName().equalsIgnoreCase(entry)) {
                return true;
            }
        }

        for (var fake : fakePlayers()) {
            if (fake.profileName().equalsIgnoreCase(entry)) {
                return true;
            }
        }

        return false;
    }

    public void refresh(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return;
        }

        boolean wasSpawned = npc.spawned();
        boolean playerNpc = npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER;

        if (wasSpawned) {
            despawn(id);
        }

        if (playerNpc) {
            fakePlayerRegistry.removeByNpcId(id);
            npc.entityUuid(null);
        }

        if (wasSpawned) {
            spawn(id);
        }
    }

    public boolean setInteractionCooldown(String id, long cooldownMillis) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.interactionCooldownMillis(cooldownMillis);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean addAction(String id, NPCClickType trigger, NPCAction action) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        actions.add(action);
        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean addActionBefore(String id, NPCClickType trigger, int index, NPCAction action) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        int position = Math.max(0, Math.min(index - 1, actions.size()));
        actions.add(position, action);

        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean addActionAfter(String id, NPCClickType trigger, int index, NPCAction action) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        int position = Math.max(0, Math.min(index, actions.size()));
        actions.add(position, action);

        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean setAction(String id, NPCClickType trigger, int index, NPCAction action) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        int position = index - 1;

        if (position < 0 || position >= actions.size()) {
            return false;
        }

        actions.set(position, action);
        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean removeAction(String id, NPCClickType trigger, int index) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        int position = index - 1;

        if (position < 0 || position >= actions.size()) {
            return false;
        }

        actions.remove(position);
        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean moveActionUp(String id, NPCClickType trigger, int index) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        int position = index - 1;

        if (position <= 0 || position >= actions.size()) {
            return false;
        }

        java.util.Collections.swap(actions, position, position - 1);
        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean moveActionDown(String id, NPCClickType trigger, int index) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        var actions = new java.util.ArrayList<>(npc.actions(trigger));
        int position = index - 1;

        if (position < 0 || position >= actions.size() - 1) {
            return false;
        }

        java.util.Collections.swap(actions, position, position + 1);
        npc.actions(trigger, actions);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean copy(String sourceId, String newId) {
        AriatusNPC source = npcs.get(sourceId.toLowerCase());

        if (source == null || npcs.containsKey(newId.toLowerCase())) {
            return false;
        }

        AriatusNPC copy = new AriatusNPC(
                newId.toLowerCase(),
                source.engineType(),
                source.entityType(),
                source.location().clone()
        );

        copy.enabled(source.enabled());
        copy.typeName(source.typeName());
        copy.displayName(source.displayName());
        copy.nameVisible(source.nameVisible());
        copy.ai(source.ai());
        copy.silent(source.silent());
        copy.invulnerable(source.invulnerable());
        copy.gravity(source.gravity());
        copy.glowing(source.glowing());
        copy.collidable(source.collidable());
        copy.persistent(source.persistent());
        copy.size(source.size());
        copy.showInTab(source.showInTab());
        copy.turnToPlayer(source.turnToPlayer());
        copy.turnToPlayerDistance(source.turnToPlayerDistance());
        copy.interactionCooldownMillis(source.interactionCooldownMillis());

        copy.glowingColor(source.glowingColor());

        copy.skinData().mode(source.skinData().mode());
        copy.skinData().source(source.skinData().source());
        copy.skinData().slim(source.skinData().slim());
        copy.skinData().value(source.skinData().value());
        copy.skinData().signature(source.skinData().signature());

        source.actions().forEach((trigger, actions) ->
                copy.actions(trigger, new java.util.ArrayList<>(actions))
        );

        npcs.put(copy.id(), copy);
        try {
            configManager.saveNPC(copy);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return spawn(copy.id());
    }

    public boolean type(String id, String typeName) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        boolean wasSpawned = npc.spawned();

        if (wasSpawned) {
            despawn(id);
        }

        if (typeName.equalsIgnoreCase("PLAYER")) {
            npc.engineType(net.ariatus.project.api.npc.NPCEngineType.PLAYER);
            npc.typeName("PLAYER");
        } else {
            EntityType type = EntityType.valueOf(typeName.toUpperCase());

            npc.engineType(net.ariatus.project.api.npc.NPCEngineType.ENTITY);
            npc.entityType(type);
            npc.typeName(type.name());
        }

        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (wasSpawned) {
            spawn(id);
        }

        return true;
    }

    public boolean displayName(String id, String displayName) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        if (displayName.equalsIgnoreCase("@none")) {
            npc.displayName("");
            npc.nameVisible(false);
        } else {
            npc.displayName(displayName);
            npc.nameVisible(true);
        }

        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            labelManager.spawnOrUpdate(npc);
            refresh(id);
            return true;
        }

        var entity = npc.entity();

        if (entity != null) {
            if (!npc.nameVisible() || npc.displayName() == null || npc.displayName().isBlank()) {
                entity.customName(null);
                entity.setCustomNameVisible(false);
            } else {
                entity.customName(net.ariatus.project.message.MessageService.parse(npc.displayName()));
                entity.setCustomNameVisible(true);
            }
        }

        return true;
    }

    private void refreshOrApplyName(AriatusNPC npc) {
        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            refresh(npc.id());
            return;
        }

        var entity = npc.entity();

        if (entity == null) {
            return;
        }

        if (!npc.nameVisible() || npc.displayName() == null || npc.displayName().isBlank()) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
            return;
        }

        entity.customName(net.ariatus.project.message.MessageService.parse(npc.displayName()));
        entity.setCustomNameVisible(true);
    }

    public boolean showInTab(String id, boolean showInTab) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.showInTab(showInTab);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            refresh(id);
        }

        return true;
    }

    public boolean collidable(String id, boolean collidable) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.collidable(collidable);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (npc.engineType() == net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            refresh(id);
            return true;
        }

        refresh(id);
        return true;
    }

    public boolean turnToPlayer(String id, boolean value) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.turnToPlayer(value);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean turnToPlayerDistance(String id, double distance) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null || distance < 0) {
            return false;
        }

        npc.turnToPlayerDistance(distance);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public boolean moveTo(String id, Location location, boolean lookInMyDirection) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        if (!lookInMyDirection) {
            location.setYaw(npc.location().getYaw());
            location.setPitch(npc.location().getPitch());
        }

        boolean wasSpawned = npc.spawned();

        if (wasSpawned) {
            despawn(id);
        }

        npc.location(location);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (wasSpawned) {
            spawn(id);
        }

        return true;
    }

    public boolean rotate(String id, float yaw, float pitch) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        Location location = npc.location().clone();
        location.setYaw(yaw);
        location.setPitch(pitch);

        npc.location(location);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(id);

        return true;
    }

    public boolean center(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        Location location = npc.location().clone();
        location.setX(location.getBlockX() + 0.5);
        location.setZ(location.getBlockZ() + 0.5);

        npc.location(location);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(id);

        return true;
    }

    public boolean skin(String id, String source, boolean slim) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        if (npc.engineType() != net.ariatus.project.api.npc.NPCEngineType.PLAYER) {
            return false;
        }

        npc.skinData().slim(slim);

        // Forzamos resolución nueva.
        npc.skinData().value("");
        npc.skinData().signature("");

        if (source.equalsIgnoreCase("@none")) {
            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.NONE);
            npc.skinData().source("");
        } else if (source.equalsIgnoreCase("@mirror")) {
            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.MIRROR);
            npc.skinData().source("@mirror");
        } else if (source.startsWith("http://") || source.startsWith("https://")) {
            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.URL);
            npc.skinData().source(source);
        } else if (source.endsWith(".png") || source.endsWith(".skin") || source.endsWith(".json")) {
            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.FILE);
            npc.skinData().source(source);
        } else {
            npc.skinData().mode(net.ariatus.project.npc.skin.NPCSkinMode.NAME);
            npc.skinData().source(source);
        }

        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Importante: refresh con nuevo UUID.
        refresh(id);

        return true;
    }

    public boolean visibility(String id, net.ariatus.project.npc.visibility.NPCVisibilityMode mode) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.visibilityMode(mode);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(id);

        return true;
    }

    public boolean visibilityDistanceDefault(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.visibilityDistanceMode(net.ariatus.project.npc.visibility.NPCVisibilityDistanceMode.DEFAULT);
        npc.visibilityDistance(16.0);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(id);

        return true;
    }

    public boolean visibilityDistanceAlways(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        npc.visibilityDistanceMode(net.ariatus.project.npc.visibility.NPCVisibilityDistanceMode.ALWAYS);
        npc.visibilityDistance(-1.0);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(id);

        return true;
    }

    public boolean visibilityDistance(String id, double distance) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null || distance < 0) {
            return false;
        }

        npc.visibilityDistanceMode(net.ariatus.project.npc.visibility.NPCVisibilityDistanceMode.CUSTOM);
        npc.visibilityDistance(distance);
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(id);

        return true;
    }

    public void saveAndRefresh(AriatusNPC npc) {
        try {
            configManager.saveNPC(npc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        refresh(npc.id());
    }

    public java.util.List<AriatusNPC> nearby(Location location, double radius, String typeFilter) {
        double radiusSquared = radius * radius;

        return npcs.values()
                .stream()
                .filter(npc -> npc.location() != null && npc.location().getWorld() != null)
                .filter(npc -> npc.location().getWorld().equals(location.getWorld()))
                .filter(npc -> typeFilter == null || typeFilter.isBlank()
                        || npc.typeName().equalsIgnoreCase(typeFilter)
                        || npc.entityType().name().equalsIgnoreCase(typeFilter))
                .filter(npc -> npc.location().distanceSquared(location) <= radiusSquared)
                .sorted(java.util.Comparator.comparingDouble(npc -> npc.location().distanceSquared(location)))
                .toList();
    }

    public java.util.List<AriatusNPC> list(String typeFilter, String sort) {
        java.util.stream.Stream<AriatusNPC> stream = npcs.values().stream();

        if (typeFilter != null && !typeFilter.isBlank()) {
            stream = stream.filter(npc ->
                    npc.typeName().equalsIgnoreCase(typeFilter)
                            || npc.entityType().name().equalsIgnoreCase(typeFilter)
            );
        }

        java.util.List<AriatusNPC> result = new java.util.ArrayList<>(stream.toList());

        if (sort != null) {
            switch (sort.toLowerCase()) {
                case "name", "id" -> result.sort(java.util.Comparator.comparing(AriatusNPC::id));
                case "type" -> result.sort(java.util.Comparator.comparing(AriatusNPC::typeName));
                case "world" -> result.sort(java.util.Comparator.comparing(npc -> npc.location().getWorld().getName()));
            }
        }

        return result;
    }


    public java.util.Optional<net.ariatus.project.npc.player.FakePlayerNPCData> fakePlayerByNpcId(String npcId) {
        return fakePlayerRegistry.byNpcId(npcId);
    }

    public boolean setCustomAttribute(String id, String attribute, String value) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return false;
        }

        String normalized = attribute.toUpperCase();

        try {
            switch (normalized) {
                case "INVISIBLE" -> npc.invisible(Boolean.parseBoolean(value));
                case "ON_FIRE" -> npc.onFire(Boolean.parseBoolean(value));
                case "SHAKING" -> npc.shaking(Boolean.parseBoolean(value));
                case "POSE" -> npc.pose(net.ariatus.project.npc.attribute.NPCPoseType.parse(value));
                default -> {
                    return false;
                }
            }

            configManager.saveNPC(npc);
            refresh(id);
            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    public java.util.Map<String, String> customAttributes(String id) {
        AriatusNPC npc = npcs.get(id.toLowerCase());

        if (npc == null) {
            return java.util.Map.of();
        }

        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();

        result.put("INVISIBLE", String.valueOf(npc.invisible()));
        result.put("ON_FIRE", String.valueOf(npc.onFire()));
        result.put("SHAKING", String.valueOf(npc.shaking()));
        result.put("POSE", npc.pose().name());

        return result;
    }

    public FakePlayerSeatData fakeSeat(String npcId) {
        return fakePlayerRegistry.seat(npcId);
    }

    public Optional<FakePlayerSeatData> fakeSeatIfPresent(String npcId) {
        return fakePlayerRegistry.seatIfPresent(npcId);
    }

}