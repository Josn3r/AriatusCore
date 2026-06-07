package net.ariatus.project.npc;

import net.ariatus.project.api.npc.NPCClickType;
import net.ariatus.project.api.npc.NPCEngineType;
import net.ariatus.project.api.npc.NPCView;
import net.ariatus.project.npc.attribute.NPCPoseType;
import net.ariatus.project.npc.equipment.NPCEquipmentSlot;
import net.ariatus.project.npc.skin.NPCSkinData;
import net.ariatus.project.npc.visibility.NPCVisibilityDistanceMode;
import net.ariatus.project.npc.visibility.NPCVisibilityMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AriatusNPC implements NPCView {

    private final String id;
    private NPCEngineType engineType;
    private EntityType entityType;
    private Location location;

    private boolean enabled;
    private String displayName;
    private boolean nameVisible;

    private boolean ai;
    private boolean silent;
    private boolean invulnerable;
    private boolean gravity;
    private boolean glowing;
    private boolean collidable;
    private boolean persistent;
    private double size;

    private String typeName;
    private NPCGlowingColor glowingColor = NPCGlowingColor.DISABLED;
    private boolean showInTab = false;
    private boolean turnToPlayer = false;
    private double turnToPlayerDistance = 8.0;
    private long interactionCooldownMillis = 350L;
    private final NPCSkinData skinData = new NPCSkinData();

    private final Map<NPCClickType, List<NPCAction>> actions = new EnumMap<>(NPCClickType.class);

    private boolean invisible = false;
    private boolean onFire = false;
    private boolean shaking = false;

    private NPCPoseType pose = NPCPoseType.STANDING;
    private NPCVisibilityMode visibilityMode = NPCVisibilityMode.ALL;
    private NPCVisibilityDistanceMode visibilityDistanceMode = NPCVisibilityDistanceMode.DEFAULT;

    private double visibilityDistance = 16.0;

    private UUID entityUuid;

    public AriatusNPC(
            String id,
            NPCEngineType engineType,
            EntityType entityType,
            Location location
    ) {
        this.id = id;
        this.engineType = engineType;
        this.entityType = entityType;
        this.location = location;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public NPCEngineType engineType() {
        return engineType;
    }

    @Override
    public EntityType entityType() {
        return entityType;
    }

    @Override
    public Location location() {
        return location;
    }

    public void location(Location location) {
        this.location = location;
    }

    @Override
    public boolean spawned() {
        return entityUuid != null;
    }

    @Override
    public UUID entityUuid() {
        return entityUuid;
    }

    public void entityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public Entity entity() {
        if (entityUuid == null || location == null || location.getWorld() == null) {
            return null;
        }

        return location.getWorld().getEntities()
                .stream()
                .filter(entity -> entity.getUniqueId().equals(entityUuid))
                .findFirst()
                .orElse(null);
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean nameVisible() {
        return nameVisible;
    }

    public void nameVisible(boolean nameVisible) {
        this.nameVisible = nameVisible;
    }

    public boolean ai() {
        return ai;
    }

    public void ai(boolean ai) {
        this.ai = ai;
    }

    public boolean silent() {
        return silent;
    }

    public void silent(boolean silent) {
        this.silent = silent;
    }

    public boolean invulnerable() {
        return invulnerable;
    }

    public void invulnerable(boolean invulnerable) {
        this.invulnerable = invulnerable;
    }

    public boolean gravity() {
        return gravity;
    }

    public void gravity(boolean gravity) {
        this.gravity = gravity;
    }

    public boolean glowing() {
        return glowing;
    }

    public void glowing(boolean glowing) {
        this.glowing = glowing;
    }

    public boolean collidable() {
        return collidable;
    }

    public void collidable(boolean collidable) {
        this.collidable = collidable;
    }

    public boolean persistent() {
        return persistent;
    }

    public void persistent(boolean persistent) {
        this.persistent = persistent;
    }

    public double size() {
        return size;
    }

    public void size(double size) {
        this.size = size;
    }

    public Map<NPCClickType, List<NPCAction>> actions() {
        return actions;
    }

    public List<NPCAction> actions(NPCClickType clickType) {
        return actions.getOrDefault(clickType, List.of());
    }

    public void actions(NPCClickType clickType, List<NPCAction> actions) {
        this.actions.put(clickType, actions);
    }

    public String typeName() {
        return typeName;
    }

    public void typeName(String typeName) {
        this.typeName = typeName;
    }

    public NPCGlowingColor glowingColor() {
        return glowingColor;
    }

    public void glowingColor(NPCGlowingColor glowingColor) {
        this.glowingColor = glowingColor;
    }

    public boolean showInTab() {
        return showInTab;
    }

    public void showInTab(boolean showInTab) {
        this.showInTab = showInTab;
    }

    public boolean turnToPlayer() {
        return turnToPlayer;
    }

    public void turnToPlayer(boolean turnToPlayer) {
        this.turnToPlayer = turnToPlayer;
    }

    public double turnToPlayerDistance() {
        return turnToPlayerDistance;
    }

    public void turnToPlayerDistance(double turnToPlayerDistance) {
        this.turnToPlayerDistance = turnToPlayerDistance;
    }

    public long interactionCooldownMillis() {
        return interactionCooldownMillis;
    }

    public void interactionCooldownMillis(long interactionCooldownMillis) {
        this.interactionCooldownMillis = interactionCooldownMillis;
    }

    public NPCSkinData skinData() {
        return skinData;
    }

    public void engineType(NPCEngineType engineType) {
        this.engineType = engineType;
    }

    public void entityType(EntityType entityType) {
        this.entityType = entityType;
    }

    private final Map<NPCEquipmentSlot, ItemStack> equipment =
            new EnumMap<>(NPCEquipmentSlot.class);

    public Map<NPCEquipmentSlot, ItemStack> equipment() {
        return equipment;
    }

    public boolean invisible() {
        return invisible;
    }

    public void invisible(boolean invisible) {
        this.invisible = invisible;
    }

    public boolean onFire() {
        return onFire;
    }

    public void onFire(boolean onFire) {
        this.onFire = onFire;
    }

    public boolean shaking() {
        return shaking;
    }

    public void shaking(boolean shaking) {
        this.shaking = shaking;
    }

    public NPCPoseType pose() {
        return pose;
    }

    public void pose(NPCPoseType pose) {
        this.pose = pose;
    }

    public NPCVisibilityMode visibilityMode() {
        return visibilityMode;
    }

    public void visibilityMode(NPCVisibilityMode visibilityMode) {
        this.visibilityMode = visibilityMode;
    }

    public NPCVisibilityDistanceMode visibilityDistanceMode() {
        return visibilityDistanceMode;
    }

    public void visibilityDistanceMode(NPCVisibilityDistanceMode visibilityDistanceMode) {
        this.visibilityDistanceMode = visibilityDistanceMode;
    }

    public double visibilityDistance() {
        return visibilityDistance;
    }

    public void visibilityDistance(double visibilityDistance) {
        this.visibilityDistance = visibilityDistance;
    }
}