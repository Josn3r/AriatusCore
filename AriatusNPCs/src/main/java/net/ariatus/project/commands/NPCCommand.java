package net.ariatus.project.commands;

import net.ariatus.project.api.npc.NPCClickType;
import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.npc.AriatusNPC;
import net.ariatus.project.npc.NPCAction;
import net.ariatus.project.npc.NPCActionType;
import net.ariatus.project.npc.NPCManager;
import net.ariatus.project.npc.animation.NPCAnimationService;
import net.ariatus.project.npc.equipment.NPCEquipmentMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.OldEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NPCCommand implements AriatusCommandExecutor {

    private final NPCManager npcManager;
    private final NPCAnimationService animationService;
    private final NPCEquipmentMenu equipmentMenu;

    public NPCCommand(
            NPCManager npcManager,
            net.ariatus.project.npc.animation.NPCAnimationService animationService
    ) {
        this.npcManager = npcManager;
        this.animationService = animationService;
        this.equipmentMenu = new NPCEquipmentMenu();
    }

    @Override
    public String name() {
        return "ariatusnpc";
    }

    @Override
    public List<String> aliases() {
        return List.of("npc", "npcs", "anpc");
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (Exception exception) {
            return def;
        }
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariatusnpcs.admin")) {
            MessageService.send(sender, "&cNo tienes permisos.");
            return true;
        }

        if (args.length == 0) {
            MessageService.send(sender, "");
            MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS NPCS</bold></gradient>");
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "&7Usa &e/npc help <página> &7para ver todos los comandos.");
            MessageService.send(sender, "&7Ejemplo: &e/npc help 1");
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "");
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            int page = args.length >= 2 ? parseInt(args[1], 1) : 1;
            help(sender, page);
            return true;
        }

        try {
            switch (args[0].toLowerCase()) {
                case "create" -> create(sender, args);
                case "copy" -> copy(sender, args);
                case "remove", "delete" -> delete(sender, args);
                case "list" -> list(sender, args);
                case "info" -> info(sender, args);
                case "type" -> type(sender, args);
                case "rename" -> displayName(sender, args);
                case "skin" -> skin(sender, args);
                case "glowing" -> glowing(sender, args);
                case "showInTab" -> showInTab(sender, args);
                case "visibility" -> visibility(sender, args);
                case "visibility_distance" -> visibilityDistance(sender, args);
                case "collidable" -> collidable(sender, args);
                case "scale" -> scale(sender, args);
                case "equipment" -> equipment(sender, args);
                case "attribute" -> attribute(sender, args);
                case "animation" -> animation(sender, args);
                case "look" -> turnToPlayer(sender, args);
                case "look_distance" -> turnToPlayerDistance(sender, args);
                case "move_here" -> moveHere(sender, args);
                case "move_to" -> moveTo(sender, args);
                case "rotate" -> rotate(sender, args);
                case "center" -> center(sender, args);
                case "nearby" -> nearby(sender, args);
                case "teleport" -> teleport(sender, args);
                case "action" -> action(sender, args);
                case "interaction_cooldown" -> interactionCooldown(sender, args);
                default -> help(sender, 1);
            }
        } catch (Exception exception) {
            MessageService.send(sender, "&cUso inválido o error: &e" + exception.getMessage());
        }

        return true;
    }

    private void create(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden crear NPCs desde ubicación.");
            return;
        }

        NPCCommandContext ctx = new NPCCommandContext(args, 1);

        String id = ctx.requireArg(0).toLowerCase();
        String typeName = ctx.flagOne("type", "PLAYER");

        World world = ctx.hasFlag("world")
                ? Bukkit.getWorld(ctx.flagOne("world", player.getWorld().getName()))
                : player.getWorld();

        if (world == null) {
            MessageService.send(sender, "&cMundo inválido.");
            return;
        }

        Location location = player.getLocation();

        if (ctx.hasFlag("position")) {
            List<String> pos = ctx.flag("position");

            if (pos.size() < 3) {
                MessageService.send(sender, "&cUso: &e--position <x> <y> <z>");
                return;
            }

            location = new Location(
                    world,
                    Double.parseDouble(pos.get(0)),
                    Double.parseDouble(pos.get(1)),
                    Double.parseDouble(pos.get(2)),
                    player.getLocation().getYaw(),
                    player.getLocation().getPitch()
            );
        } else {
            location.setWorld(world);
        }

        boolean success;

        if (typeName.equalsIgnoreCase("PLAYER")) {
            success = npcManager.createPlayer(id, id, location);
        } else {
            EntityType type = EntityType.valueOf(typeName.toUpperCase());
            try {
                success = npcManager.createEntity(id, type, location);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        MessageService.send(sender, success
                ? "&aNPC &e" + id + " &acreado correctamente."
                : "&cNo se pudo crear el NPC. Puede que ya exista.");
    }

    private void copy(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc copy <npc> <new_name>");
            return;
        }

        MessageService.send(sender, npcManager.copy(args[1], args[2])
                ? "&aNPC copiado correctamente."
                : "&cNo se pudo copiar el NPC.");
    }

    private void delete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/npc delete <npc>");
            return;
        }

        try {
            MessageService.send(sender, npcManager.delete(args[1])
                    ? "&aNPC eliminado correctamente."
                    : "&cEse NPC no existe.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void visibility(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc visibility <npc> <all|manual|permission>");
            return;
        }

        try {
            var mode = net.ariatus.project.npc.visibility.NPCVisibilityMode.parse(args[2]);

            MessageService.send(sender, npcManager.visibility(args[1], mode)
                    ? "&aVisibilidad actualizada a &e" + mode.name() + "&a."
                    : "&cNo se pudo actualizar la visibilidad.");
        } catch (Exception exception) {
            MessageService.send(sender, "&cModo inválido. Usa: &eall&c, &emanual&c, &epermission&c.");
        }
    }

    private void visibilityDistance(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc visibility_distance <npc> <always|default|distance>");
            return;
        }

        String value = args[2];

        boolean success;

        if (value.equalsIgnoreCase("always")) {
            success = npcManager.visibilityDistanceAlways(args[1]);
        } else if (value.equalsIgnoreCase("default")) {
            success = npcManager.visibilityDistanceDefault(args[1]);
        } else {
            try {
                success = npcManager.visibilityDistance(args[1], Double.parseDouble(value));
            } catch (NumberFormatException exception) {
                MessageService.send(sender, "&cDistancia inválida.");
                return;
            }
        }

        MessageService.send(sender, success
                ? "&aDistancia de visibilidad actualizada."
                : "&cNo se pudo actualizar la distancia de visibilidad.");
    }

    private void list(CommandSender sender, String[] args) {
        NPCCommandContext ctx = new NPCCommandContext(args, 1);

        String type = ctx.flagOne("type", "");
        String sort = ctx.flagOne("sort", "id");

        List<AriatusNPC> npcs = npcManager.list(type, sort);

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS NPCS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        if (npcs.isEmpty()) {
            MessageService.send(sender, "&7No hay NPCs.");
        } else {
            for (AriatusNPC npc : npcs) {
                MessageService.send(sender,
                        "&e" + npc.id()
                                + " &8| &7Type: &f" + npc.typeName()
                                + " &8| &7World: &f" + npc.location().getWorld().getName()
                                + " &8| &7Spawned: " + (npc.spawned() ? "&aSí" : "&cNo"));
            }
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/npc info <npc>");
            return;
        }

        npcManager.internalNPC(args[1]).ifPresentOrElse(npc -> {
            MessageService.send(sender, "");
            MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>NPC INFO</bold></gradient>");
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "&7ID: &e" + npc.id());
            MessageService.send(sender, "&7Type: &f" + npc.typeName());
            MessageService.send(sender, "&7Engine: &f" + npc.engineType().name());
            MessageService.send(sender, "&7DisplayName: &f" + (npc.displayName() == null || npc.displayName().isBlank() ? "@none" : npc.displayName()));
            MessageService.send(sender, "&7Show in tab: &f" + npc.showInTab());
            MessageService.send(sender, "&7Collidable: &f" + npc.collidable());
            MessageService.send(sender, "&7Scale: &f" + npc.size());
            MessageService.send(sender, "&7Cooldown: &f" + npc.interactionCooldownMillis() + "ms");
            MessageService.send(sender, "&7Location: &f"
                    + npc.location().getWorld().getName() + " "
                    + format(npc.location().getX()) + " "
                    + format(npc.location().getY()) + " "
                    + format(npc.location().getZ()));
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "");
        }, () -> MessageService.send(sender, "&cEse NPC no existe."));
    }

    private void type(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc type <npc> <type>");
            return;
        }

        MessageService.send(sender, npcManager.type(args[1], args[2])
                ? "&aTipo actualizado."
                : "&cNo se pudo actualizar el tipo.");
    }

    private void displayName(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc displayname <npc> <@none|name>");
            return;
        }

        String name = join(args, 2);

        MessageService.send(sender, npcManager.displayName(args[1], name)
                ? "&aDisplayName actualizado."
                : "&cNo se pudo actualizar displayname.");
    }

    private void skin(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc skin <npc> <@none|@mirror|name|url|file> [--slim]");
            return;
        }

        NPCCommandContext ctx = new NPCCommandContext(args, 2);
        String source = ctx.requireArg(0);
        boolean slim = ctx.hasFlag("slim");

        MessageService.send(sender, npcManager.skin(args[1], source, slim)
                ? "&aSkin actualizada."
                : "&cNo se pudo actualizar la skin.");
    }

    private void glowing(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc glowing <npc> <disabled|color>");
            return;
        }

        String id = args[1];
        String rawColor = args[2];

        try {
            net.ariatus.project.npc.NPCGlowingColor color =
                    rawColor.equalsIgnoreCase("disabled")
                            ? net.ariatus.project.npc.NPCGlowingColor.DISABLED
                            : net.ariatus.project.npc.NPCGlowingColor.valueOf(rawColor.toUpperCase());

            MessageService.send(sender, npcManager.glowing(id, color)
                    ? "&aGlowing actualizado."
                    : "&cNo se pudo actualizar glowing.");
        } catch (Exception exception) {
            MessageService.send(sender, "&cColor inválido. Usa: disabled, white, gold, yellow, red, green, blue, aqua...");
        }
    }

    private void showInTab(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc show_in_tab <npc> <true|false>");
            return;
        }

        MessageService.send(sender, npcManager.showInTab(args[1], Boolean.parseBoolean(args[2]))
                ? "&aShow in tab actualizado."
                : "&cNo se pudo actualizar.");
    }

    private void collidable(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc collidable <npc> <true|false>");
            return;
        }

        MessageService.send(sender, npcManager.collidable(args[1], Boolean.parseBoolean(args[2]))
                ? "&aCollidable actualizado."
                : "&cNo se pudo actualizar.");
    }

    private void scale(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc scale <npc> <factor>");
            return;
        }

        MessageService.send(sender, npcManager.size(args[1], Double.parseDouble(args[2]))
                ? "&aScale actualizado."
                : "&cNo se pudo actualizar scale.");
    }

    private void equipment(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc equipment <npc> <set|clear|list>");
            return;
        }

        String npcId = args[1];
        String operation = args[2].toLowerCase();

        switch (operation) {
            case "set" -> equipmentSet(sender, npcId);
            case "clear" -> equipmentClear(sender, npcId);
            case "list" -> equipmentList(sender, npcId);
            default -> MessageService.send(sender, "&cUso: &e/npc equipment <npc> <set|clear|list>");
        }
    }

    private void attribute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc attribute <npc> <set|list>");
            return;
        }

        String npcId = args[1];
        String operation = args[2].toLowerCase();

        switch (operation) {
            case "list" -> attributeList(sender, npcId);
            case "set" -> attributeSet(sender, args);
            default -> MessageService.send(sender, "&cUso: &e/npc attribute <npc> <set|list>");
        }
    }

    private void animation(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc animation <npc> <animation>");
            animationList(sender);
            return;
        }

        String npcId = args[1];
        String rawAnimation = args[2];

        net.ariatus.project.npc.animation.NPCAnimationType animationType;

        try {
            animationType = net.ariatus.project.npc.animation.NPCAnimationType.valueOf(rawAnimation.toUpperCase());
        } catch (Exception exception) {
            MessageService.send(sender, "&cAnimación inválida.");
            animationList(sender);
            return;
        }

        boolean success = animationService.play(npcId, animationType);

        MessageService.send(sender, success
                ? "&aAnimación &e" + animationType.name() + " &aejecutada en &e" + npcId + "&a."
                : "&cNo se pudo ejecutar la animación.");
    }

    private void turnToPlayer(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc turn_to_player <npc> <true|false>");
            return;
        }

        MessageService.send(sender, npcManager.turnToPlayer(args[1], Boolean.parseBoolean(args[2]))
                ? "&aTurn to player actualizado."
                : "&cNo se pudo actualizar.");
    }

    private void turnToPlayerDistance(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc turn_to_player_distance <npc> <distance>");
            return;
        }

        MessageService.send(sender, npcManager.turnToPlayerDistance(args[1], Double.parseDouble(args[2]))
                ? "&aDistancia actualizada."
                : "&cNo se pudo actualizar.");
    }

    private void moveHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/npc move_here <npc>");
            return;
        }

        try {
            MessageService.send(sender, npcManager.moveHere(args[1], player.getLocation())
                    ? "&aNPC movido."
                    : "&cNo se pudo mover.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void moveTo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        if (args.length < 5) {
            MessageService.send(sender, "&cUso: &e/npc move_to <npc> <x> <y> <z> [world] [--look-in-my-direction]");
            return;
        }

        String id = args[1];
        double x = Double.parseDouble(args[2]);
        double y = Double.parseDouble(args[3]);
        double z = Double.parseDouble(args[4]);

        String worldName = args.length >= 6 && !args[5].startsWith("--")
                ? args[5]
                : player.getWorld().getName();

        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            MessageService.send(sender, "&cMundo inválido.");
            return;
        }

        boolean look = Arrays.asList(args).contains("--look-in-my-direction");
        Location location = new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch());

        MessageService.send(sender, npcManager.moveTo(id, location, look)
                ? "&aNPC movido."
                : "&cNo se pudo mover.");
    }

    private void rotate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageService.send(sender, "&cUso: &e/npc rotate <npc> <yaw> <pitch>");
            return;
        }

        MessageService.send(sender, npcManager.rotate(args[1], Float.parseFloat(args[2]), Float.parseFloat(args[3]))
                ? "&aNPC rotado."
                : "&cNo se pudo rotar.");
    }

    private void center(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/npc center <npc>");
            return;
        }

        MessageService.send(sender, npcManager.center(args[1])
                ? "&aNPC centrado."
                : "&cNo se pudo centrar.");
    }

    private void nearby(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        NPCCommandContext ctx = new NPCCommandContext(args, 1);

        double radius = Double.parseDouble(ctx.flagOne("radius", "10"));
        String type = ctx.flagOne("type", "");

        List<AriatusNPC> nearby = npcManager.nearby(player.getLocation(), radius, type);

        MessageService.send(sender, "&eNPCs cercanos &7(" + nearby.size() + "):");

        for (AriatusNPC npc : nearby) {
            MessageService.send(sender, "&7- &e" + npc.id() + " &8| &f" + format(player.getLocation().distance(npc.location())) + " bloques");
        }
    }

    private void teleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/npc teleport <npc>");
            return;
        }

        npcManager.internalNPC(args[1]).ifPresentOrElse(npc -> {
            player.teleport(npc.location());
            MessageService.send(sender, "&aTeletransportado a &e" + npc.id() + "&a.");
        }, () -> MessageService.send(sender, "&cEse NPC no existe."));
    }

    private void action(CommandSender sender, String[] args) {
        if (args.length < 4) {
            actionHelp(sender);
            return;
        }

        String npc = args[1];
        NPCClickType trigger = NPCClickType.valueOf(args[2].toUpperCase());
        String operation = args[3].toLowerCase();

        switch (operation) {
            case "add" -> {
                if (args.length < 5) {
                    actionHelp(sender);
                    return;
                }

                NPCAction action = NPCAction.parse(args[4], join(args, 5));
                MessageService.send(sender, npcManager.addAction(npc, trigger, action)
                        ? "&aAcción añadida."
                        : "&cNo se pudo añadir acción.");
            }

            case "add_before" -> {
                int index = Integer.parseInt(args[4]);
                NPCAction action = NPCAction.parse(args[5], join(args, 6));
                MessageService.send(sender, npcManager.addActionBefore(npc, trigger, index, action)
                        ? "&aAcción añadida."
                        : "&cNo se pudo añadir.");
            }

            case "add_after" -> {
                int index = Integer.parseInt(args[4]);
                NPCAction action = NPCAction.parse(args[5], join(args, 6));
                MessageService.send(sender, npcManager.addActionAfter(npc, trigger, index, action)
                        ? "&aAcción añadida."
                        : "&cNo se pudo añadir.");
            }

            case "set" -> {
                int index = Integer.parseInt(args[4]);
                NPCAction action = NPCAction.parse(args[5], join(args, 6));
                MessageService.send(sender, npcManager.setAction(npc, trigger, index, action)
                        ? "&aAcción actualizada."
                        : "&cNo se pudo actualizar.");
            }

            case "remove" -> {
                int index = Integer.parseInt(args[4]);
                MessageService.send(sender, npcManager.removeAction(npc, trigger, index)
                        ? "&aAcción eliminada."
                        : "&cNo se pudo eliminar.");
            }

            case "move_up" -> {
                int index = Integer.parseInt(args[4]);
                MessageService.send(sender, npcManager.moveActionUp(npc, trigger, index)
                        ? "&aAcción movida."
                        : "&cNo se pudo mover.");
            }

            case "move_down" -> {
                int index = Integer.parseInt(args[4]);
                MessageService.send(sender, npcManager.moveActionDown(npc, trigger, index)
                        ? "&aAcción movida."
                        : "&cNo se pudo mover.");
            }

            case "clear" -> {
                try {
                    MessageService.send(sender, npcManager.clearActions(npc, trigger)
                            ? "&aAcciones limpiadas."
                            : "&cNo se pudo limpiar.");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            case "list" -> actionList(sender, npc, trigger);

            default -> actionHelp(sender);
        }
    }

    private void actionList(CommandSender sender, String npcId, NPCClickType trigger) {
        npcManager.internalNPC(npcId).ifPresentOrElse(npc -> {
            List<NPCAction> actions = npc.actions(trigger);

            MessageService.send(sender, "&eAcciones de &f" + npc.id() + " &7/ &b" + trigger.name());

            if (actions.isEmpty()) {
                MessageService.send(sender, "&7No hay acciones.");
                return;
            }

            for (int i = 0; i < actions.size(); i++) {
                NPCAction action = actions.get(i);
                MessageService.send(sender, "&e#" + (i + 1) + " &f" + action.serialize());
            }
        }, () -> MessageService.send(sender, "&cEse NPC no existe."));
    }

    private void interactionCooldown(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/npc interaction_cooldown <npc> <disabled|cooldown>");
            return;
        }

        long millis = args[2].equalsIgnoreCase("disabled")
                ? 0L
                : Long.parseLong(args[2]);

        MessageService.send(sender, npcManager.setInteractionCooldown(args[1], millis)
                ? "&aCooldown actualizado."
                : "&cNo se pudo actualizar.");
    }

    private void notImplemented(CommandSender sender, String feature) {
        MessageService.send(sender, "&e" + feature + " &7todavía está preparado pero no implementado.");
    }

    private void equipmentSet(CommandSender sender, String npcId) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden abrir el menú de equipo.");
            return;
        }

        var optionalNPC = npcManager.internalNPC(npcId);

        if (optionalNPC.isEmpty()) {
            MessageService.send(sender, "&cEse NPC no existe.");
            return;
        }

        equipmentMenu.open(player, optionalNPC.get());
    }

    private void equipmentClear(CommandSender sender, String npcId) {
        var optionalNPC = npcManager.internalNPC(npcId);

        if (optionalNPC.isEmpty()) {
            MessageService.send(sender, "&cEse NPC no existe.");
            return;
        }

        var npc = optionalNPC.get();

        npc.equipment().clear();
        npcManager.saveAndRefresh(npc);

        MessageService.send(sender, "&aEquipo limpiado para &e" + npc.id() + "&a.");
    }

    private void equipmentList(CommandSender sender, String npcId) {
        var optionalNPC = npcManager.internalNPC(npcId);

        if (optionalNPC.isEmpty()) {
            MessageService.send(sender, "&cEse NPC no existe.");
            return;
        }

        var npc = optionalNPC.get();

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>NPC EQUIPMENT</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7NPC: &e" + npc.id());

        for (net.ariatus.project.npc.equipment.NPCEquipmentSlot slot
                : net.ariatus.project.npc.equipment.NPCEquipmentSlot.values()) {

            org.bukkit.inventory.ItemStack item = npc.equipment().get(slot);

            String itemName = item == null || item.getType().isAir()
                    ? "&7Vacío"
                    : "&f" + item.getType().name();

            MessageService.send(sender, "&e" + slot.name() + " &8→ " + itemName);
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void animationList(CommandSender sender) {
        MessageService.send(sender, "&7Animaciones disponibles:");
        MessageService.send(sender, "&eSWING_MAIN_HAND&7, &eSWING_OFF_HAND&7, &eHURT&7, &eDEATH&7, &eCRITICAL&7, &eMAGIC_CRITICAL");
    }

    private void attributeList(CommandSender sender, String npcId) {
        java.util.Map<String, String> attributes = npcManager.customAttributes(npcId);

        if (attributes.isEmpty()) {
            MessageService.send(sender, "&cEse NPC no existe.");
            return;
        }

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>NPC CUSTOM ATTRIBUTES</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7NPC: &e" + npcId);

        for (var entry : attributes.entrySet()) {
            MessageService.send(sender, "&e" + entry.getKey() + " &8→ &f" + entry.getValue());
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void attributeSet(CommandSender sender, String[] args) {
        if (args.length < 5) {
            MessageService.send(sender, "&cUso: &e/npc attribute <npc> set <attribute> <value>");
            return;
        }

        String npcId = args[1];
        String attribute = args[3];
        String value = args[4];

        boolean success = npcManager.setCustomAttribute(npcId, attribute, value);

        MessageService.send(sender, success
                ? "&aAtributo &e" + attribute.toUpperCase() + " &aactualizado a &f" + value + "&a."
                : "&cNo se pudo actualizar el atributo.");
    }

    private void actionHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>NPC ACTIONS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        MessageService.send(sender, "&6Uso:");
        MessageService.send(sender, "&e/npc action <npc> <trigger> add <actiontype> &7[value]");
        MessageService.send(sender, "&e/npc action <npc> <trigger> add_before <index> <actiontype> &7[value]");
        MessageService.send(sender, "&e/npc action <npc> <trigger> add_after <index> <actiontype> &7[value]");
        MessageService.send(sender, "&e/npc action <npc> <trigger> set <number> <actiontype> &7[value]");
        MessageService.send(sender, "&e/npc action <npc> <trigger> remove <number>");
        MessageService.send(sender, "&e/npc action <npc> <trigger> move_up <number>");
        MessageService.send(sender, "&e/npc action <npc> <trigger> move_down <number>");
        MessageService.send(sender, "&e/npc action <npc> <trigger> clear");
        MessageService.send(sender, "&e/npc action <npc> <trigger> list");
        MessageService.send(sender, "");

        MessageService.send(sender, "&6Triggers:");
        MessageService.send(sender, "&7RIGHT_CLICK");
        MessageService.send(sender, "&7LEFT_CLICK");
        MessageService.send(sender, "&7SHIFT_RIGHT_CLICK");
        MessageService.send(sender, "&7SHIFT_LEFT_CLICK");
        MessageService.send(sender, "&7MIDDLE_CLICK");
        MessageService.send(sender, "&7SHIFT_MIDDLE_CLICK");
        MessageService.send(sender, "");

        MessageService.send(sender, "&6ActionTypes:");
        MessageService.send(sender, "&7MESSAGE");
        MessageService.send(sender, "&7CONSOLE_COMMAND");
        MessageService.send(sender, "&7PLAYER_COMMAND");
        MessageService.send(sender, "&7PLAYER_COMMAND_AS_OP");
        MessageService.send(sender, "&7SEND_TO_SERVER");
        MessageService.send(sender, "&7PLAY_SOUND");
        MessageService.send(sender, "");

        MessageService.send(sender, "&6Ejemplos:");
        MessageService.send(sender, "&e/npc action banker RIGHT_CLICK add MESSAGE &fBienvenido, %player%.");
        MessageService.send(sender, "&e/npc action banker LEFT_CLICK add CONSOLE_COMMAND eco give %player% COINS 10");
        MessageService.send(sender, "&e/npc action lobby RIGHT_CLICK add SEND_TO_SERVER survival");
        MessageService.send(sender, "&e/npc action bard RIGHT_CLICK add PLAY_SOUND minecraft:block.note_block.pling,1.0,1.2");

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void help(CommandSender sender, int page) {
        java.util.List<String> lines = java.util.List.of(
                "&e/npc create <name> &7[--position <x> <y> <z>] [--world <world>] [--type <type>]",
                "&e/npc copy <npc> <new_name>",
                "&e/npc remove <npc>",
                "&e/npc delete <npc>",
                "&e/npc list &7[--type <type>] [--sort <sort>]",
                "&e/npc info <npc>",

                "&e/npc type <npc> <type>",
                "&e/npc displayname <npc> <@none|name>",
                "&e/npc skin <npc> <@none|@mirror|name|url|file> &7[--slim]",
                "&e/npc glowing <npc> &7<disabled|color>",
                "&e/npc show_in_tab <npc> &7<true|false>",
                "&e/npc collidable <npc> &7<true|false>",

                "&e/npc scale <npc> <factor>",
                "&e/npc attribute <npc> list",
                "&e/npc attribute <npc> set <INVISIBLE|ON_FIRE|SHAKING|POSE> <value>",
                "&e/npc visibility <npc> <all|manual|permission>",
                "&e/npc visibility_distance <npc> <always|default|distance>",
                "&e/npc equipment <npc> <set|clear|list>",

                "&e/npc animation <npc> <animation>",
                "&e/npc turn_to_player <npc> &7<true|false>",
                "&e/npc turn_to_player_distance <npc> &7<distance>",
                "&e/npc move_here <npc>",
                "&e/npc move_to <npc> <x> <y> <z> &7[world] [--look-in-my-direction]",
                "&e/npc rotate <npc> <yaw> <pitch>",

                "&e/npc center <npc>",
                "&e/npc nearby &7[--radius <radius>] [--type <type>] [--sort <sort>]",
                "&e/npc teleport <npc>",
                "&e/npc action <npc> <trigger> add <actiontype> &7[value]",
                "&e/npc action <npc> <trigger> set <number> <actiontype> &7[value]",
                "&e/npc action <npc> <trigger> list"
        );

        int perPage = 6;
        int maxPage = (int) Math.ceil(lines.size() / (double) perPage);

        page = Math.max(1, Math.min(page, maxPage));

        int from = (page - 1) * perPage;
        int to = Math.min(from + perPage, lines.size());

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS NPCS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Página &e" + page + "&7/&e" + maxPage);

        for (int i = from; i < to; i++) {
            MessageService.send(sender, lines.get(i));
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        if (page < maxPage) {
            MessageService.send(sender, "&7Siguiente página: &e/npc help " + (page + 1));
        }

        if (page > 1) {
            MessageService.send(sender, "&7Página anterior: &e/npc help " + (page - 1));
        }

        MessageService.send(sender, "");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariatusnpcs.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(args[0], List.of(
                    "create", "copy", "remove", "list", "info", "type",
                    "rename", "skin", "equipment", "glowing", "showInTab",
                    "collidable", "scale", "visibility", "visibility_distance", "attribute", "animation", "look",
                    "look_distance", "move_here", "move_to", "rotate",
                    "center", "nearby", "teleport", "action", "interaction_cooldown"
            ));
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2 && needsNpcId(sub)) {
            return filter(args[1], npcIds());
        }

        if (sub.equals("create")) {
            return tabCreate(args);
        }

        if (sub.equals("type") && args.length == 3) {
            return filter(args[2], entityTypesWithPlayer());
        }

        if (sub.equals("rename") && args.length == 3) {
            return filter(args[2], List.of("@none"));
        }

        if (sub.equals("skin") && args.length == 3) {
            return filter(args[2], List.of("@none", "@mirror"));
        }

        if (sub.equals("glowing") && args.length == 3) {
            return filter(args[2], List.of("disabled", "white", "yellow", "gold", "red", "green", "blue", "aqua", "gray"));
        }

        if ((sub.equals("showInTab") || sub.equals("collidable") || sub.equals("look")) && args.length == 3) {
            return filter(args[2], List.of("true", "false"));
        }

        if (sub.equals("action")) {
            return tabAction(args);
        }

        if (sub.equals("interaction_cooldown") && args.length == 3) {
            return filter(args[2], List.of("disabled", "350", "500", "1000"));
        }

        if (sub.equals("equipment")) {
            return tabEquipment(args);
        }

        if (sub.equals("attribute")) {
            return tabAttribute(args);
        }

        if (sub.equals("animation")) {
            return tabAnimation(args);
        }

        if (sub.equals("visibility")) {
            return tabVisibility(args);
        }

        if (sub.equals("visibility_distance")) {
            return tabVisibilityDistance(args);
        }

        return List.of();
    }

    private List<String> tabVisibility(String[] args) {
        if (args.length == 2) {
            return filter(args[1], npcIds());
        }

        if (args.length == 3) {
            return filter(args[2], List.of("all", "manual", "permission"));
        }

        return List.of();
    }

    private List<String> tabVisibilityDistance(String[] args) {
        if (args.length == 2) {
            return filter(args[1], npcIds());
        }

        if (args.length == 3) {
            return filter(args[2], List.of("always", "default", "8", "16", "24", "32", "48", "64"));
        }

        return List.of();
    }

    private List<String> tabEquipment(String[] args) {
        if (args.length == 2) {
            return filter(args[1], npcIds());
        }

        if (args.length == 3) {
            return filter(args[2], List.of("set", "clear", "list"));
        }

        return List.of();
    }

    private List<String> tabAnimation(String[] args) {
        if (args.length == 2) {
            return filter(args[1], npcIds());
        }

        if (args.length == 3) {
            return filter(
                    args[2],
                    java.util.Arrays.stream(net.ariatus.project.npc.animation.NPCAnimationType.values())
                            .map(Enum::name)
                            .toList()
            );
        }

        return List.of();
    }

    private List<String> tabAttribute(String[] args) {
        if (args.length == 2) {
            return filter(args[1], npcIds());
        }

        if (args.length == 3) {
            return filter(args[2], List.of("set", "list"));
        }

        if (args.length == 4 && args[2].equalsIgnoreCase("set")) {
            return filter(args[3], List.of("INVISIBLE", "ON_FIRE", "SHAKING", "POSE"));
        }

        if (args.length == 5 && args[2].equalsIgnoreCase("set")) {
            String attribute = args[3].toUpperCase();

            if (attribute.equals("POSE")) {
                return filter(args[4], List.of("standing", "crouching", "swimming", "sleeping", "sitting"));
            }

            return filter(args[4], List.of("true", "false"));
        }

        return List.of();
    }

    private List<String> attributeNames() {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Attribute attribute : org.bukkit.Registry.ATTRIBUTE) {
            NamespacedKey key = attribute.getKey();
            names.add(key.getKey().toUpperCase());
        }
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    private List<String> tabCreate(String[] args) {
        if (args.length == 2) {
            return List.of("<name>");
        }

        if (args.length >= 3) {
            String last = args[args.length - 1];

            if (last.startsWith("--")) {
                return filter(last, List.of("--position", "--world", "--type"));
            }

            String previous = args.length >= 2 ? args[args.length - 2] : "";

            if (previous.equalsIgnoreCase("--world")) {
                return filter(last, Bukkit.getWorlds().stream().map(World::getName).toList());
            }

            if (previous.equalsIgnoreCase("--type")) {
                return filter(last, entityTypesWithPlayer());
            }
        }

        return List.of("--position", "--world", "--type");
    }

    private List<String> tabAction(String[] args) {
        if (args.length == 2) {
            return filter(args[1], npcIds());
        }

        if (args.length == 3) {
            return filter(args[2], Arrays.stream(NPCClickType.values()).map(Enum::name).toList());
        }

        if (args.length == 4) {
            return filter(args[3], List.of("add", "add_before", "add_after", "set", "remove", "move_up", "move_down", "clear", "list"));
        }

        String operation = args[3].toLowerCase();

        if (List.of("add", "add_before", "add_after", "set").contains(operation)) {
            if ((operation.equals("add") && args.length == 5)
                    || (!operation.equals("add") && args.length == 6)) {
                return filter(args[args.length - 1], Arrays.stream(NPCActionType.values()).map(Enum::name).toList());
            }
        }

        return List.of();
    }

    private boolean needsNpcId(String sub) {
        return List.of(
                "copy", "remove", "info", "type", "rename", "skin",
                "equipment", "glowing", "showInTab", "collidable", "scale",
                "attribute", "animation", "visibility", "visibility_distance", "look", "look_distance",
                "move_here", "move_to", "rotate", "center", "teleport",
                "interaction_cooldown"
        ).contains(sub);
    }

    private List<String> npcIds() {
        return npcManager.internalNPCs().stream().map(AriatusNPC::id).toList();
    }

    private List<String> entityTypesWithPlayer() {
        List<String> result = new ArrayList<>();
        result.add("PLAYER");
        result.addAll(Arrays.stream(EntityType.values()).map(Enum::name).toList());
        return result;
    }

    private List<String> filter(String current, List<String> options) {
        String lowered = current.toLowerCase();

        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowered))
                .toList();
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();

        for (int i = start; i < args.length; i++) {
            if (i > start) {
                builder.append(" ");
            }

            builder.append(args[i]);
        }

        return builder.toString();
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}