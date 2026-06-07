package net.ariatus.project.commands;

import net.ariatus.project.api.world.WorldTeleportResult;
import net.ariatus.project.api.world.WorldView;
import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.world.WorldManager;
import net.ariatus.project.world.WorldStats;
import net.ariatus.project.world.WorldStatsService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WorldCommand implements AriatusCommandExecutor {

    private final WorldManager worldManager;
    private final WorldStatsService statsService = new WorldStatsService();

    public WorldCommand(WorldManager worldManager) {
        this.worldManager = worldManager;
    }

    @Override
    public String name() {
        return "ariatusworlds";
    }

    @Override
    public List<String> aliases() {
        return List.of("aworld", "aw", "world");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> list(sender);
            case "load" -> load(sender, args);
            case "unload" -> unload(sender, args);
            case "tp", "teleport" -> teleport(sender, args);
            case "setspawn" -> setSpawn(sender, args);
            case "spawn" -> spawn(sender, args);
            case "info" -> info(sender, args);
            case "stats" -> stats(sender, args);
            case "topentities" -> topEntities(sender, args);
            case "topblocks" -> topBlocks(sender, args);
            case "border" -> border(sender, args);
            case "delete", "remove" -> delete(sender, args);
            default -> {
                MessageService.send(sender, "&cSubcomando desconocido. Usa &e/aw help&c.");
                return true;
            }
        }

        return true;
    }

    private void list(CommandSender sender) {
        if (!hasAdmin(sender)) {
            return;
        }

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS WORLDS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");

        if (worldManager.worlds().isEmpty()) {
            MessageService.send(sender, "&7No hay mundos configurados.");
        } else {
            for (WorldView world : worldManager.worlds()) {
                String status = worldManager.loaded(world.id()) ? "&aCargado" : "&cDescargado";
                String enabled = world.enabled() ? "&aActivo" : "&cDesactivado";
                String autoLoad = world.autoLoad() ? "&aSí" : "&7No";

                MessageService.send(
                        sender,
                        "&e" + world.id()
                                + " &8| &7folder: &f" + world.folder()
                                + " &8| " + status
                                + " &8| &7enabled: " + enabled
                                + " &8| &7auto-load: " + autoLoad
                );
            }
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void load(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw load <mundo>");
            return;
        }

        String worldId = args[1];

        worldManager.load(worldId).thenAccept(result -> {
            switch (result) {
                case SUCCESS -> MessageService.send(sender, "&aMundo cargado correctamente: &e" + worldId);
                case ALREADY_LOADED -> MessageService.send(sender, "&eEl mundo ya estaba cargado: &f" + worldId);
                case NOT_CONFIGURED -> MessageService.send(sender, "&cEl mundo no está configurado en worlds.yml: &e" + worldId);
                case DISABLED -> MessageService.send(sender, "&cEl mundo está desactivado en worlds.yml: &e" + worldId);
                case FAILED -> MessageService.send(sender, "&cNo se pudo cargar el mundo: &e" + worldId);
            }
        });
    }

    private void unload(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw unload <mundo> [save:true|false]");
            return;
        }

        String worldId = args[1];
        boolean save = args.length < 3 || Boolean.parseBoolean(args[2]);

        worldManager.unload(worldId, save).thenAccept(success -> {
            if (success) {
                MessageService.send(sender, "&aMundo descargado correctamente: &e" + worldId);
            } else {
                MessageService.send(sender, "&cNo se pudo descargar el mundo. Puede tener jugadores dentro o no existir: &e" + worldId);
            }
        });
    }

    private void teleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        if (!sender.hasPermission("ariatusworlds.teleport") && !sender.hasPermission("ariatusworlds.admin")) {
            MessageService.send(sender, "&cNo tienes permisos.");
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw tp <mundo> [spawn]");
            return;
        }

        String worldId = args[1];
        String spawnName = args.length >= 3 ? args[2] : "default";

        worldManager.teleport(player, worldId, spawnName).thenAccept(result -> {
            switch (result) {
                case SUCCESS -> MessageService.send(player, "&aViajaste a &e" + worldId + "&a.");
                case WORLD_NOT_FOUND -> MessageService.send(player, "&cEse mundo no existe en worlds.yml: &e" + worldId);
                case WORLD_NOT_LOADED -> MessageService.send(player, "&cNo se pudo cargar el mundo: &e" + worldId);
                case SPAWN_NOT_SET -> MessageService.send(player, "&cEse mundo no tiene spawn configurado: &e" + worldId + "&7/" + spawnName);
                case PLAYER_OFFLINE -> MessageService.send(player, "&cEl jugador no está conectado.");
                case FAILED -> MessageService.send(player, "&cNo se pudo teletransportar al mundo.");
            }
        });
    }

    private void setSpawn(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw setspawn <mundo> [spawn]");
            return;
        }

        String worldId = args[1].toLowerCase();
        String spawnName = args.length >= 3 ? args[2].toLowerCase() : "default";

        if (!worldManager.exists(worldId)) {
            MessageService.send(sender, "&cEse mundo no existe en worlds.yml: &e" + worldId);
            return;
        }

        Location location = player.getLocation();
        worldManager.setSpawn(worldId, spawnName, location);

        MessageService.send(
                sender,
                "&aSpawn &e" + spawnName + " &aguardado para el mundo &e" + worldId + "&a."
        );
    }

    private void spawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageService.send(sender, "&cSolo jugadores pueden usar este comando.");
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw spawn <mundo> [spawn]");
            return;
        }

        String worldId = args[1];
        String spawnName = args.length >= 3 ? args[2] : "default";

        worldManager.teleport(player, worldId, spawnName).thenAccept(result -> {
            if (result == WorldTeleportResult.SUCCESS) {
                MessageService.send(player, "&aViajaste al spawn &e" + spawnName + " &ade &e" + worldId + "&a.");
                return;
            }

            MessageService.send(player, "&cNo se pudo viajar al spawn. Resultado: &e" + result.name());
        });
    }

    private void info(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw info <mundo>");
            return;
        }

        String worldId = args[1].toLowerCase();
        Optional<WorldView> optionalWorld = worldManager.world(worldId);

        if (optionalWorld.isEmpty()) {
            MessageService.send(sender, "&cEse mundo no existe en worlds.yml: &e" + worldId);
            return;
        }

        WorldView ariatusWorld = optionalWorld.get();
        World bukkitWorld = Bukkit.getWorld(ariatusWorld.folder());

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>WORLD INFO</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7ID: &e" + ariatusWorld.id());
        MessageService.send(sender, "&7Nombre: &f" + ariatusWorld.displayName());
        MessageService.send(sender, "&7Folder: &f" + ariatusWorld.folder());
        MessageService.send(sender, "&7Enabled: " + (ariatusWorld.enabled() ? "&aSí" : "&cNo"));
        MessageService.send(sender, "&7Auto-load: " + (ariatusWorld.autoLoad() ? "&aSí" : "&7No"));
        MessageService.send(sender, "&7Loaded: " + (worldManager.loaded(ariatusWorld.id()) ? "&aSí" : "&cNo"));

        if (bukkitWorld != null) {
            MessageService.send(sender, "&7Bukkit name: &f" + bukkitWorld.getName());
            MessageService.send(sender, "&7Environment: &f" + bukkitWorld.getEnvironment().name());
            MessageService.send(sender, "&7Difficulty: &f" + bukkitWorld.getDifficulty().name());
            MessageService.send(sender, "&7Players: &b" + bukkitWorld.getPlayers().size());
            MessageService.send(sender, "&7PVP: " + (bukkitWorld.getPVP() ? "&aSí" : "&cNo"));
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void delete(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw delete <mundo> confirm");
            return;
        }

        String worldId = args[1].toLowerCase();

        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            MessageService.send(sender, "&cEsta acción eliminará la carpeta del mundo.");
            MessageService.send(sender, "&cConfirma con: &e/aw delete " + worldId + " confirm");
            return;
        }

        worldManager.delete(worldId).thenAccept(success -> {
            if (success) {
                MessageService.send(sender, "&aMundo eliminado correctamente: &e" + worldId);
            } else {
                MessageService.send(sender, "&cNo se pudo eliminar el mundo. Puede tener jugadores dentro o no existir: &e" + worldId);
            }
        });
    }

    private void border(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso:");
            MessageService.send(sender, "&e/aw border info <mundo>");
            MessageService.send(sender, "&e/aw border set <mundo> <minX> <maxX> <minZ> <maxZ>");
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "info" -> borderInfo(sender, args);
            case "set" -> borderSet(sender, args);
            default -> {
                MessageService.send(sender, "&cUso:");
                MessageService.send(sender, "&e/aw border info <mundo>");
                MessageService.send(sender, "&e/aw border set <mundo> <minX> <maxX> <minZ> <maxZ>");
            }
        }
    }

    private void borderInfo(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/aw border info <mundo>");
            return;
        }

        String worldId = args[2].toLowerCase();

        worldManager.border(worldId).ifPresentOrElse(border -> {
            MessageService.send(sender, "");
            MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>WORLD BORDER</bold></gradient>");
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "&7Mundo: &e" + worldId);
            MessageService.send(sender, "&7Tipo: &f" + border.type());
            MessageService.send(sender, "&7Min X: &f" + border.minX());
            MessageService.send(sender, "&7Max X: &f" + border.maxX());
            MessageService.send(sender, "&7Min Z: &f" + border.minZ());
            MessageService.send(sender, "&7Max Z: &f" + border.maxZ());
            MessageService.send(sender, "&7Acción: &f" + border.action());
            MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
            MessageService.send(sender, "");
        }, () -> MessageService.send(sender, "&cEse mundo no tiene AriatusBorder configurado."));
    }

    private void borderSet(CommandSender sender, String[] args) {
        if (args.length < 7) {
            MessageService.send(sender, "&cUso: &e/aw border set <mundo> <minX> <maxX> <minZ> <maxZ>");
            return;
        }

        String worldId = args[2].toLowerCase();

        if (!worldManager.exists(worldId)) {
            MessageService.send(sender, "&cEse mundo no existe en worlds.yml: &e" + worldId);
            return;
        }

        try {
            double minX = Double.parseDouble(args[3]);
            double maxX = Double.parseDouble(args[4]);
            double minZ = Double.parseDouble(args[5]);
            double maxZ = Double.parseDouble(args[6]);

            if (minX >= maxX || minZ >= maxZ) {
                MessageService.send(sender, "&cCoordenadas inválidas. min debe ser menor que max.");
                return;
            }

            worldManager.setRectangleBorder(worldId, minX, maxX, minZ, maxZ);

            MessageService.send(sender, "&aAriatusBorder configurado para &e" + worldId + "&a.");
            MessageService.send(sender, "&7X: &f" + minX + " &8→ &f" + maxX);
            MessageService.send(sender, "&7Z: &f" + minZ + " &8→ &f" + maxZ);

        } catch (NumberFormatException exception) {
            MessageService.send(sender, "&cCoordenadas inválidas.");
        }
    }

    private void stats(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw stats <mundo>");
            return;
        }

        String worldId = args[1].toLowerCase();

        Optional<WorldView> optionalWorld = worldManager.world(worldId);

        if (optionalWorld.isEmpty()) {
            MessageService.send(sender, "&cEse mundo no existe en worlds.yml: &e" + worldId);
            return;
        }

        WorldView ariatusWorld = optionalWorld.get();
        World bukkitWorld = Bukkit.getWorld(ariatusWorld.folder());

        if (bukkitWorld == null) {
            MessageService.send(sender, "&cEse mundo no está cargado: &e" + worldId);
            return;
        }

        WorldStats stats = statsService.collect(bukkitWorld);

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>WORLD STATS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Mundo: &e" + stats.worldName());
        MessageService.send(sender, "&7Chunks cargados: &b" + format(stats.loadedChunks()));
        MessageService.send(sender, "&7Jugadores: &a" + format(stats.players()));
        MessageService.send(sender, "");
        MessageService.send(sender, "&7Entidades totales: &f" + format(stats.entities()));
        MessageService.send(sender, "&7Living entities: &f" + format(stats.livingEntities()));
        MessageService.send(sender, "&7Mobs: &c" + format(stats.mobs()));
        MessageService.send(sender, "&7Animales: &a" + format(stats.animals()));
        MessageService.send(sender, "&7Monstruos: &c" + format(stats.monsters()));
        MessageService.send(sender, "&7Items tirados: &e" + format(stats.droppedItems()));
        MessageService.send(sender, "&7Villagers: &6" + format(stats.villagers()));
        MessageService.send(sender, "&7ArmorStands: &d" + format(stats.armorStands()));
        MessageService.send(sender, "&7Tile entities: &b" + format(stats.tileEntities()));
        MessageService.send(sender, "");
        MessageService.send(sender, "&7WorldBorder: &f" + format((int) stats.borderSize()) + " bloques");
        MessageService.send(sender, "&7PVP: " + (stats.pvp() ? "&aSí" : "&cNo"));
        MessageService.send(sender, "&7Dificultad: &f" + stats.difficulty());
        MessageService.send(sender, "&7Tiempo: &f" + stats.fullTime());
        MessageService.send(sender, "&7Tormenta: " + (stats.storm() ? "&aSí" : "&7No"));
        MessageService.send(sender, "&7Truenos: " + (stats.thundering() ? "&aSí" : "&7No"));
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void topEntities(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw topentities <mundo> [limite]");
            return;
        }

        String worldId = args[1].toLowerCase();
        int limit = args.length >= 3 ? parseInt(args[2], 10) : 10;

        Optional<WorldView> optionalWorld = worldManager.world(worldId);

        if (optionalWorld.isEmpty()) {
            MessageService.send(sender, "&cEse mundo no existe en worlds.yml: &e" + worldId);
            return;
        }

        WorldView ariatusWorld = optionalWorld.get();
        World bukkitWorld = Bukkit.getWorld(ariatusWorld.folder());

        if (bukkitWorld == null) {
            MessageService.send(sender, "&cEse mundo no está cargado: &e" + worldId);
            return;
        }

        Map<String, Integer> top = statsService.topEntities(bukkitWorld, limit);

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>TOP ENTITIES</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Mundo: &e" + worldId);
        MessageService.send(sender, "&7Total tipos: &b" + top.size());
        MessageService.send(sender, "");

        if (top.isEmpty()) {
            MessageService.send(sender, "&7No hay entidades cargadas.");
        } else {
            int position = 1;

            for (Map.Entry<String, Integer> entry : top.entrySet()) {
                MessageService.send(
                        sender,
                        "&e#" + position
                                + " &f" + prettify(entry.getKey())
                                + " &8→ &b" + format(entry.getValue())
                );
                position++;
            }
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void topBlocks(CommandSender sender, String[] args) {
        if (!hasAdmin(sender)) {
            return;
        }

        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/aw topblocks <mundo> [limite]");
            return;
        }

        String worldId = args[1].toLowerCase();
        int limit = args.length >= 3 ? parseInt(args[2], 10) : 10;

        Optional<WorldView> optionalWorld = worldManager.world(worldId);

        if (optionalWorld.isEmpty()) {
            MessageService.send(sender, "&cEse mundo no existe en worlds.yml: &e" + worldId);
            return;
        }

        WorldView ariatusWorld = optionalWorld.get();
        World bukkitWorld = Bukkit.getWorld(ariatusWorld.folder());

        if (bukkitWorld == null) {
            MessageService.send(sender, "&cEse mundo no está cargado: &e" + worldId);
            return;
        }

        Map<String, Integer> top = statsService.topBlockEntities(bukkitWorld, limit);

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>TOP BLOCK ENTITIES</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Mundo: &e" + worldId);
        MessageService.send(sender, "&7Total tipos: &b" + top.size());
        MessageService.send(sender, "");

        if (top.isEmpty()) {
            MessageService.send(sender, "&7No hay block entities cargadas.");
        } else {
            int position = 1;

            for (Map.Entry<String, Integer> entry : top.entrySet()) {
                MessageService.send(
                        sender,
                        "&e#" + position
                                + " &f" + prettify(entry.getKey())
                                + " &8→ &b" + format(entry.getValue())
                );
                position++;
            }
        }

        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private int parseInt(String value, int def) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return def;
        }
    }

    private String prettify(String key) {
        String lower = key.toLowerCase().replace("_", " ");
        String[] parts = lower.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return builder.toString().trim();
    }

    private String format(int value) {
        return String.format("%,d", value);
    }

    private void sendHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS WORLDS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&e/aw list &7- Lista mundos configurados.");
        MessageService.send(sender, "&e/aw load <mundo> &7- Carga un mundo.");
        MessageService.send(sender, "&e/aw unload <mundo> [save] &7- Descarga un mundo.");
        MessageService.send(sender, "&e/aw tp <mundo> [spawn] &7- Viaja a un mundo.");
        MessageService.send(sender, "&e/aw spawn <mundo> [spawn] &7- Viaja al spawn de un mundo.");
        MessageService.send(sender, "&e/aw setspawn <mundo> [spawn] &7- Guarda un spawn.");
        MessageService.send(sender, "&e/aw topentities <mundo> [limite] &7- Muestra tipos de entidades cargadas.");
        MessageService.send(sender, "&e/aw topblocks <mundo> [limite] &7- Muestra block entities cargadas.");
        MessageService.send(sender, "&e/aw border set <mundo> <minX> <maxX> <minZ> <maxZ> &7- Configura borde rectangular.");
        MessageService.send(sender, "&e/aw border info <mundo> &7- Muestra el borde rectangular.");
        MessageService.send(sender, "&e/aw info <mundo> &7- Muestra información.");
        MessageService.send(sender, "&e/aw stats <mundo> &7- Muestra chunks, mobs y entidades.");
        MessageService.send(sender, "&e/aw delete <mundo> confirm &7- Elimina carpeta del mundo.");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private boolean hasAdmin(CommandSender sender) {
        if (sender.hasPermission("ariatusworlds.admin")) {
            return true;
        }

        MessageService.send(sender, "&cNo tienes permisos.");
        return false;
    }
}