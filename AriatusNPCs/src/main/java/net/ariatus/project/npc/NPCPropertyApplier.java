package net.ariatus.project.npc;

import net.ariatus.project.message.MessageService;
import net.ariatus.project.npc.attribute.NPCPoseType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;

public class NPCPropertyApplier {

    public void apply(AriatusNPC npc, Entity entity) {
        applyName(npc, entity);

        entity.setInvulnerable(npc.invulnerable());
        entity.setSilent(npc.silent());
        entity.setGravity(npc.gravity());
        entity.setGlowing(npc.glowing());
        applyGlowTeam(entity, npc);
        entity.setPersistent(npc.persistent());

        applyCollision(entity, npc.collidable());

        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setAI(npc.ai());
            applyScale(livingEntity, npc.size());
        }

        if (entity instanceof Ageable ageable) {
            ageable.setAdult();
        }

        if (entity instanceof Villager villager) {
            villager.setAI(npc.ai());
            villager.setInvulnerable(npc.invulnerable());
            villager.setProfession(Villager.Profession.LIBRARIAN);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setVillagerLevel(5);
        }
        applyCustomAttributes(entity, npc);
    }

    private void applyScale(LivingEntity entity, double size) {
        if (size <= 0) {
            return;
        }

        try {
            AttributeInstance scale = entity.getAttribute(Attribute.SCALE);

            if (scale != null) {
                scale.setBaseValue(size);
            }
        } catch (Exception ignored) {
            // Algunas versiones/entidades pueden no soportar SCALE.
        }
    }

    private void applyCollision(Entity entity, boolean collidable) {
        entity.getServer().getScoreboardManager();
        var scoreboard = entity.getServer().getScoreboardManager().getMainScoreboard();
        String teamName = collidable ? "ariatus_npc_col" : "ariatus_npc_nocol";
        var team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);

            if (!collidable) {
                team.setOption(
                        org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
                        org.bukkit.scoreboard.Team.OptionStatus.NEVER
                );
            } else {
                team.setOption(
                        org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
                        org.bukkit.scoreboard.Team.OptionStatus.ALWAYS
                );
            }
        }

        team.addEntry(entity.getUniqueId().toString());
    }

    private void applyName(AriatusNPC npc, Entity entity) {
        if (!npc.nameVisible()) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
            return;
        }

        if (npc.displayName() == null || npc.displayName().isBlank()) {
            entity.customName(null);
            entity.setCustomNameVisible(false);
            return;
        }

        entity.customName(MessageService.parse(npc.displayName()));
        entity.setCustomNameVisible(true);
    }

    private void applyCustomAttributes(Entity entity, AriatusNPC npc) {
        entity.setInvisible(npc.invisible());

        if (npc.onFire()) {
            entity.setFireTicks(Integer.MAX_VALUE);
        } else {
            entity.setFireTicks(0);
        }

        try {
            entity.setVisualFire(npc.onFire());
        } catch (Exception ignored) {
        }

        try {
            entity.setFreezeTicks(npc.shaking() ? entity.getMaxFreezeTicks() : 0);
        } catch (Exception ignored) {
        }

        applyPose(entity, npc.pose());
    }

    private void applyPose(Entity entity, NPCPoseType pose) {
        try {
            Pose bukkitPose = switch (pose) {
                case STANDING -> Pose.STANDING;
                case CROUCHING -> Pose.SNEAKING;
                case SWIMMING -> Pose.SWIMMING;
                case SLEEPING -> Pose.SLEEPING;
                case SITTING -> Pose.SITTING;
            };

            entity.setPose(bukkitPose, true);
        } catch (Exception ignored) {
        }
    }

    private void applyGlowTeam(Entity entity, AriatusNPC npc) {
        try {
            if (entity.getServer().getScoreboardManager() == null) {
                return;
            }

            var scoreboard = entity.getServer().getScoreboardManager().getMainScoreboard();

            String teamName = "anpcg_" + npc.id();

            if (teamName.length() > 16) {
                teamName = teamName.substring(0, 16);
            }

            var oldTeam = scoreboard.getTeam(teamName);

            if (oldTeam != null) {
                oldTeam.unregister();
            }

            var team = scoreboard.registerNewTeam(teamName);

            team.addEntry(entity.getUniqueId().toString());

            team.setOption(
                    org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
                    npc.collidable()
                            ? org.bukkit.scoreboard.Team.OptionStatus.ALWAYS
                            : org.bukkit.scoreboard.Team.OptionStatus.NEVER
            );

            if (npc.glowing()) {
                team.setColor(chatColor(npc.glowingColor()));
            }
        } catch (Exception ignored) {
        }
    }

    private org.bukkit.ChatColor chatColor(NPCGlowingColor color) {
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
}