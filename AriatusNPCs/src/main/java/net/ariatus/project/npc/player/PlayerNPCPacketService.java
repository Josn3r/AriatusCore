package net.ariatus.project.npc.player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import net.ariatus.project.AriatusNPCs;
import net.ariatus.project.npc.AriatusNPC;
import net.ariatus.project.npc.attribute.NPCPoseType;
import net.ariatus.project.npc.skin.SkinResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;

import java.util.List;

public class PlayerNPCPacketService {

    private final AriatusNPCs module;
    private final ProtocolManager protocolManager;
    private final SkinResolver skinResolver;

    public PlayerNPCPacketService(
            AriatusNPCs module,
            ProtocolManager protocolManager,
            SkinResolver skinResolver
    ) {
        this.module = module;
        this.protocolManager = protocolManager;
        this.skinResolver = skinResolver;
    }

    public void sendLook(Player viewer, FakePlayerNPCData data) {
        try {
            sendEntityLook(viewer, data);
            sendHeadRotation(viewer, data);
        } catch (Exception exception) {
            module.logger().warn(
                    module,
                    "No se pudo actualizar look del PLAYER NPC "
                            + data.npc().id()
                            + " para "
                            + viewer.getName()
                            + ": "
                            + exception.getMessage()
            );
        }
    }

    private void sendEntityLook(Player viewer, FakePlayerNPCData data) throws Exception {
        Location location = data.npc().location();

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_LOOK);

        packet.getIntegers().write(0, data.entityId());
        packet.getBytes().write(0, angle(location.getYaw()));
        packet.getBytes().write(1, angle(location.getPitch()));
        packet.getBooleans().write(0, true);

        protocolManager.sendServerPacket(viewer, packet);
    }

    public void spawn(Player viewer, FakePlayerNPCData data) {
        skinResolver.resolve(data.npc(), viewer.getUniqueId()).thenAccept(optionalSkin -> {
            module.tasks().run(module, () -> {
                try {
                    WrappedGameProfile profile = createProfile(data, optionalSkin.orElse(null));

                    sendPlayerInfoAdd(viewer, data, profile);
                    boolean sitting = data.npc().pose() == net.ariatus.project.npc.attribute.NPCPoseType.SITTING;
                    if (sitting && data.seat() != null) {
                        sendSeat(viewer, data, data.seat());
                    }

                    sendSpawnEntity(viewer, data);
                    sendHeadRotation(viewer, data);
                    sendTeam(viewer, data);
                    sendMetadata(viewer, data);
                    module.core().getServer().getScheduler().runTaskLater(module.core(), () ->
                            sendMetadata(viewer, data), 2L
                    );
                    sendScale(viewer, data);

                    if (sitting && data.seat() != null) {
                        module.core().getServer().getScheduler().runTaskLater(module.core(), () ->
                                sendPassengers(viewer, data.seat().entityId(), data.entityId()), 2L
                        );
                    }

                    if (!data.npc().showInTab()) {
                        module.tasks().runLater(module, () -> removeFromTab(viewer, data), 120L);
                    }

                } catch (Exception exception) {
                    module.logger().warn(module, "No se pudo enviar spawn PLAYER NPC a "
                            + viewer.getName() + ": " + exception.getMessage());
                }
            });
        });
    }

    public void destroy(Player viewer, FakePlayerNPCData data) {
        try {
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, List.of(data.entityId()));
            protocolManager.sendServerPacket(viewer, destroy);
            if (data.seat() != null) {
                destroySeat(viewer, data.seat());
            }
            removeFromTab(viewer, data);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo destruir PLAYER NPC para " + viewer.getName() + ": " + exception.getMessage());
        }
    }

    public void removeFromTab(Player viewer, FakePlayerNPCData data) {
        try {
            PacketContainer remove = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);
            remove.getUUIDLists().write(0, List.of(data.uuid()));

            protocolManager.sendServerPacket(viewer, remove);
        } catch (Exception exception) {
            // Algunos builds de ProtocolLib/versiones pueden cambiar wrapper. No rompemos el NPC.
            module.logger().warn(module, "No se pudo remover PLAYER NPC del tab: " + data.npc().id());
        }
    }

    private void sendPlayerInfoAdd(
            Player viewer,
            FakePlayerNPCData data,
            WrappedGameProfile profile
    ) throws Exception {
        AriatusNPC npc = data.npc();

        WrappedChatComponent displayName = WrappedChatComponent.fromJson(
                "{\"text\":\"" + safeJson(stripMiniMessage(npc.displayName())) + "\"}"
        );

        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

            packet.getPlayerInfoActions().write(0, java.util.EnumSet.of(
                    EnumWrappers.PlayerInfoAction.ADD_PLAYER,
                    EnumWrappers.PlayerInfoAction.UPDATE_LISTED,
                    EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME
            ));

            PlayerInfoData infoData = new PlayerInfoData(
                    profile,
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    displayName
            );

            packet.getPlayerInfoDataLists().write(1, List.of(infoData));
            protocolManager.sendServerPacket(viewer, packet);
            return;
        } catch (Exception ignored) {
            // Fallback para builds que todavía usen PLAYER_INFO.
        }

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

        packet.getPlayerInfoActions().write(0, java.util.EnumSet.of(
                EnumWrappers.PlayerInfoAction.ADD_PLAYER
        ));

        PlayerInfoData infoData = new PlayerInfoData(
                profile,
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                displayName
        );

        packet.getPlayerInfoDataLists().write(1, List.of(infoData));
        module.logger().info(module, "PLAYER_INFO sent for " + data.npc().id());
        protocolManager.sendServerPacket(viewer, packet);
    }

    private void sendSpawnEntity(Player viewer, FakePlayerNPCData data) throws Exception {
        Location location = data.npc().location();

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

        packet.getIntegers().write(0, data.entityId());
        packet.getUUIDs().write(0, data.uuid());

        boolean entityTypeWritten = false;

        try {
            packet.getEntityTypeModifier().write(0, org.bukkit.entity.EntityType.PLAYER);
            entityTypeWritten = true;
        } catch (Exception ignored) {
        }

        if (!entityTypeWritten) {
            try {
                packet.getEntityTypeModifier().writeSafely(0, org.bukkit.entity.EntityType.PLAYER);
                entityTypeWritten = true;
            } catch (Exception ignored) {
            }
        }

        if (!entityTypeWritten) {
            module.logger().warn(module, "No se pudo escribir EntityType.PLAYER en SPAWN_ENTITY para " + data.npc().id());
        }

        packet.getDoubles().write(0, location.getX());
        packet.getDoubles().write(1, location.getY());
        packet.getDoubles().write(2, location.getZ());

        packet.getBytes().write(0, angle(location.getPitch()));
        packet.getBytes().write(1, angle(location.getYaw()));
        packet.getBytes().write(2, angle(location.getYaw()));

        module.logger().info(module, "SPAWN_ENTITY structure: " + packet);
        protocolManager.sendServerPacket(viewer, packet);
    }

    private void sendHeadRotation(Player viewer, FakePlayerNPCData data) throws Exception {
        Location location = data.npc().location();

        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);

        packet.getIntegers().write(0, data.entityId());
        packet.getBytes().write(0, angle(location.getYaw()));

        protocolManager.sendServerPacket(viewer, packet);
    }

    private void sendMetadata(Player viewer, FakePlayerNPCData data) {
        try {
            byte flags = entityFlags(data);

            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

            packet.getIntegers().write(0, data.entityId());

            java.util.List<com.comphenix.protocol.wrappers.WrappedDataValue> values =
                    new java.util.ArrayList<>();

            var byteSerializer =
                    com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(Byte.class);

            values.add(new com.comphenix.protocol.wrappers.WrappedDataValue(
                    0,
                    byteSerializer,
                    Byte.valueOf(flags)
            ));

            EnumWrappers.EntityPose pose = entityPose(data.npc().pose());

            var poseSerializer =
                    com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass());

            values.add(new com.comphenix.protocol.wrappers.WrappedDataValue(
                    6,
                    poseSerializer,
                    pose.toNms()
            ));

            packet.getDataValueCollectionModifier().write(0, values);

            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo enviar metadata del PLAYER NPC "
                    + data.npc().id()
                    + " a "
                    + viewer.getName()
                    + ": "
                    + exception.getMessage());
        }
    }

    private byte entityFlags(FakePlayerNPCData data) {
        byte flags = 0x00;

        if (data.npc().onFire()) {
            flags = (byte) (flags | 0x01);
        }

        if (data.npc().pose() == net.ariatus.project.npc.attribute.NPCPoseType.CROUCHING) {
            flags = (byte) (flags | 0x02);
        }

        if (data.npc().pose() == net.ariatus.project.npc.attribute.NPCPoseType.SWIMMING) {
            flags = (byte) (flags | 0x10);
        }

        if (data.npc().invisible()) {
            flags = (byte) (flags | 0x20);
        }

        if (data.npc().glowing()) {
            flags = (byte) (flags | 0x40);
        }

        return flags;
    }

    private EnumWrappers.EntityPose entityPose(net.ariatus.project.npc.attribute.NPCPoseType pose) {
        if (pose == null) {
            return EnumWrappers.EntityPose.STANDING;
        }

        return switch (pose) {
            case STANDING -> EnumWrappers.EntityPose.STANDING;
            case CROUCHING -> EnumWrappers.EntityPose.CROUCHING;
            case SWIMMING -> EnumWrappers.EntityPose.SWIMMING;
            case SLEEPING -> EnumWrappers.EntityPose.SLEEPING;
            case SITTING -> EnumWrappers.EntityPose.SITTING;
        };
    }

    private void sendSeat(Player viewer, FakePlayerNPCData data, FakePlayerSeatData seat) {
        try {
            Location location = data.npc().location().clone();

            /*
             * Ajuste visual:
             * El ArmorStand como mount levanta al pasajero.
             * Probamos bajándolo para que el fake player parezca sentado.
             */
            location.subtract(0.0, 0.0, 0.0);

            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

            packet.getIntegers().write(0, seat.entityId());
            packet.getUUIDs().write(0, seat.uuid());

            packet.getEntityTypeModifier().write(0, org.bukkit.entity.EntityType.ARMOR_STAND);

            packet.getDoubles().write(0, location.getX());
            packet.getDoubles().write(1, location.getY());
            packet.getDoubles().write(2, location.getZ());

            packet.getBytes().write(0, angle(location.getPitch()));
            packet.getBytes().write(1, angle(location.getYaw()));
            packet.getBytes().write(2, angle(location.getYaw()));

            protocolManager.sendServerPacket(viewer, packet);

            sendSeatMetadata(viewer, seat);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo spawnear seat para PLAYER NPC "
                    + data.npc().id()
                    + ": "
                    + exception.getMessage());
        }
    }

    private void sendSeatMetadata(Player viewer, FakePlayerSeatData seat) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_METADATA);

            packet.getIntegers().write(0, seat.entityId());

            java.util.List<com.comphenix.protocol.wrappers.WrappedDataValue> values =
                    new java.util.ArrayList<>();

            var byteSerializer =
                    com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(Byte.class);

            var booleanSerializer =
                    com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry.get(Boolean.class);

            /*
             * Entity flags index 0:
             * 0x20 invisible.
             */
            values.add(new com.comphenix.protocol.wrappers.WrappedDataValue(
                    0,
                    byteSerializer,
                    Byte.valueOf((byte) 0x20)
            ));

            /*
             * ArmorStand metadata:
             * index 15 suele ser armor stand flags:
             * 0x01 small
             * 0x10 marker
             *
             * Si en tu build da error, removemos este valor.
             */
            values.add(new com.comphenix.protocol.wrappers.WrappedDataValue(
                    15,
                    byteSerializer,
                    Byte.valueOf((byte) 0x11)
            ));

            /*
             * No gravity en entidades modernas suele estar en metadata base.
             * Según versión puede cambiar. Si causa error, lo quitamos.
             */
            try {
                values.add(new com.comphenix.protocol.wrappers.WrappedDataValue(
                        5,
                        booleanSerializer,
                        Boolean.TRUE
                ));
            } catch (Exception ignored) {
            }

            packet.getDataValueCollectionModifier().write(0, values);

            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo enviar metadata del seat: " + exception.getMessage());
        }
    }

    private void sendPassengers(Player viewer, int vehicleEntityId, int passengerEntityId) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.MOUNT);

            packet.getIntegers().write(0, vehicleEntityId);
            packet.getIntegerArrays().write(0, new int[]{passengerEntityId});

            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo enviar passengers/mount: " + exception.getMessage());
        }
    }

    private void destroySeat(Player viewer, FakePlayerSeatData seat) {
        try {
            PacketContainer destroy = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            destroy.getIntLists().write(0, java.util.List.of(seat.entityId()));
            protocolManager.sendServerPacket(viewer, destroy);
        } catch (Exception ignored) {
        }
    }

    private void sendTeam(Player viewer, FakePlayerNPCData data) {
        AriatusNPC npc = data.npc();

        try {
            String teamName = "anpc_" + data.entityId();

            org.bukkit.scoreboard.Scoreboard scoreboard = viewer.getScoreboard();

            if (scoreboard == null) {
                scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            }

            org.bukkit.scoreboard.Team oldTeam = scoreboard.getTeam(teamName);

            if (oldTeam != null) {
                oldTeam.unregister();
            }

            org.bukkit.scoreboard.Team team = scoreboard.registerNewTeam(teamName);

            // Importante: el entry debe ser exactamente el profileName del fake player.
            team.addEntry(data.profileName());

            // Oculta el nametag nativo del fake player.
            // Esto también evita que se vea el belowname del objective.
            team.setOption(
                    org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY,
                    org.bukkit.scoreboard.Team.OptionStatus.NEVER
            );

            team.setOption(
                    org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
                    npc.collidable()
                            ? org.bukkit.scoreboard.Team.OptionStatus.ALWAYS
                            : org.bukkit.scoreboard.Team.OptionStatus.NEVER
            );

            if (npc.glowing()) {
                team.setColor(chatColor(npc.glowingColor()));
            }

            // Limpieza extra del belowname score.
            scoreboard.resetScores(data.profileName());
            scoreboard.resetScores(npc.id());

        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo crear team para PLAYER NPC " + data.npc().id()
                    + ": " + exception.getMessage());
        }
    }

    private void sendScale(Player viewer, FakePlayerNPCData data) {
        double scale = data.npc().size();

        if (scale <= 0 || scale == 1.0) {
            return;
        }

        String[] possibleKeys = {
                "generic.scale",
                "minecraft.generic.scale",
                "scale"
        };

        Exception lastException = null;

        for (String key : possibleKeys) {
            try {
                PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.UPDATE_ATTRIBUTES);

                packet.getIntegers().write(0, data.entityId());

                com.comphenix.protocol.wrappers.WrappedAttribute attribute =
                        com.comphenix.protocol.wrappers.WrappedAttribute.newBuilder()
                                .attributeKey(key)
                                .baseValue(scale)
                                .build();

                packet.getAttributeCollectionModifier().write(0, java.util.List.of(attribute));

                protocolManager.sendServerPacket(viewer, packet);

                module.logger().info(module, "Scale aplicado a PLAYER NPC "
                        + data.npc().id()
                        + " usando atributo "
                        + key
                        + " scale="
                        + scale);

                return;
            } catch (Exception exception) {
                lastException = exception;
            }
        }

        module.logger().warn(module, "No se pudo aplicar scale a PLAYER NPC "
                + data.npc().id()
                + ": "
                + (lastException == null ? "error desconocido" : lastException.getMessage()));
    }

    public void sendAnimation(
            Player viewer,
            FakePlayerNPCData data,
            net.ariatus.project.npc.animation.NPCAnimationType type
    ) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ANIMATION);

            packet.getIntegers().write(0, data.entityId());
            packet.getIntegers().write(1, animationId(type));

            protocolManager.sendServerPacket(viewer, packet);
        } catch (Exception exception) {
            module.logger().warn(module, "No se pudo enviar animación "
                    + type.name()
                    + " para NPC "
                    + data.npc().id()
                    + ": "
                    + exception.getMessage());
        }
    }

    private int animationId(net.ariatus.project.npc.animation.NPCAnimationType type) {
        return switch (type) {
            case SWING_MAIN_HAND -> 0;
            case HURT -> 1;
            case SWING_OFF_HAND -> 3;
            case CRITICAL -> 4;
            case MAGIC_CRITICAL -> 5;
            case DEATH -> 1;
        };
    }

    private byte angle(float value) {
        return (byte) ((int) (value * 256.0F / 360.0F));
    }

    private String safeJson(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String stripMiniMessage(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replaceAll("<[^>]+>", "")
                .replace("&0", "")
                .replace("&1", "")
                .replace("&2", "")
                .replace("&3", "")
                .replace("&4", "")
                .replace("&5", "")
                .replace("&6", "")
                .replace("&7", "")
                .replace("&8", "")
                .replace("&9", "")
                .replace("&a", "")
                .replace("&b", "")
                .replace("&c", "")
                .replace("&d", "")
                .replace("&e", "")
                .replace("&f", "")
                .replace("&l", "")
                .replace("&o", "")
                .replace("&n", "")
                .replace("&m", "")
                .replace("&r", "");
    }

    private WrappedGameProfile createProfile(
            FakePlayerNPCData data,
            net.ariatus.project.npc.skin.ResolvedSkin skin
    ) {
        WrappedGameProfile profile = new WrappedGameProfile(data.uuid(), data.profileName());

        try {
            profile.getProperties().removeAll("textures");
        } catch (Exception ignored) {
        }

        if (skin != null && skin.valid()) {
            module.logger().info(module, "Aplicando skin a NPC "
                    + data.npc().id()
                    + " profile=" + data.profileName()
                    + " uuid=" + data.uuid()
                    + " value=" + skin.value().length()
                    + " signature=" + skin.signature().length());

            profile.getProperties().put(
                    "textures",
                    new com.comphenix.protocol.wrappers.WrappedSignedProperty(
                            "textures",
                            skin.value(),
                            skin.signature()
                    )
            );
        } else {
            module.logger().warn(module, "NPC " + data.npc().id() + " se enviará sin skin.");
        }

        return profile;
    }

    private org.bukkit.ChatColor chatColor(net.ariatus.project.npc.NPCGlowingColor color) {
        if (color == null) {
            return org.bukkit.ChatColor.WHITE;
        }

        return switch (color) {
            case BLACK -> org.bukkit.ChatColor.BLACK;
            case DARK_BLUE -> org.bukkit.ChatColor.DARK_BLUE;
            case DARK_GREEN -> org.bukkit.ChatColor.DARK_GREEN;
            case DARK_AQUA -> org.bukkit.ChatColor.DARK_AQUA;
            case DARK_RED -> org.bukkit.ChatColor.DARK_RED;
            case DARK_PURPLE -> org.bukkit.ChatColor.DARK_PURPLE;
            case GOLD -> org.bukkit.ChatColor.GOLD;
            case GRAY -> org.bukkit.ChatColor.GRAY;
            case DARK_GRAY -> org.bukkit.ChatColor.DARK_GRAY;
            case BLUE -> org.bukkit.ChatColor.BLUE;
            case GREEN -> org.bukkit.ChatColor.GREEN;
            case AQUA -> org.bukkit.ChatColor.AQUA;
            case RED -> org.bukkit.ChatColor.RED;
            case LIGHT_PURPLE -> org.bukkit.ChatColor.LIGHT_PURPLE;
            case YELLOW -> org.bukkit.ChatColor.YELLOW;
            case DISABLED, WHITE -> org.bukkit.ChatColor.WHITE;
        };
    }

    private Pose bukkitPose(NPCPoseType pose) {
        return switch (pose) {
            case STANDING -> Pose.STANDING;
            case CROUCHING -> Pose.SNEAKING;
            case SWIMMING -> Pose.SWIMMING;
            case SLEEPING -> Pose.SLEEPING;
            case SITTING -> Pose.SITTING;
        };
    }
}