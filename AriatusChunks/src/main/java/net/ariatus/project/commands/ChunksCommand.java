package net.ariatus.project.commands;

import net.ariatus.project.api.chunk.ChunkPreloadTaskView;
import net.ariatus.project.chunk.ChunkPreloadManager;
import net.ariatus.project.command.AriatusCommandExecutor;
import net.ariatus.project.message.MessageService;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Optional;

public class ChunksCommand implements AriatusCommandExecutor {

    private final ChunkPreloadManager preloadManager;

    public ChunksCommand(ChunkPreloadManager preloadManager) {
        this.preloadManager = preloadManager;
    }

    @Override
    public String name() {
        return "ariatuschunks";
    }

    @Override
    public List<String> aliases() {
        return List.of("achunks", "chunks", "ac");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ariatuschunks.admin")) {
            MessageService.send(sender, "&cNo tienes permisos.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> start(sender, args);
            case "pause" -> pause(sender, args);
            case "resume" -> resume(sender, args);
            case "cancel" -> cancel(sender, args);
            case "status" -> status(sender, args);
            default -> {
                MessageService.send(sender, "&cSubcomando desconocido. Usa &e/ac help&c.");
                return true;
            }
        }

        return true;
    }

    private void start(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageService.send(sender, "&cUso: &e/ac start <mundo> <radio> [centerX] [centerZ]");
            return;
        }

        String worldId = args[1].toLowerCase();
        int radius = parseInt(args[2], -1);

        if (radius <= 0) {
            MessageService.send(sender, "&cRadio inválido.");
            return;
        }

        int centerX = args.length >= 4 ? parseInt(args[3], 0) : 0;
        int centerZ = args.length >= 5 ? parseInt(args[4], 0) : 0;

        boolean success = preloadManager.start(worldId, centerX, centerZ, radius);

        if (!success) {
            MessageService.send(sender, "&cNo se pudo iniciar la pregeneración. Revisa si el mundo existe o si ya hay una tarea activa.");
            return;
        }

        MessageService.send(sender, "&aPregeneración iniciada para &e" + worldId + "&a.");
        MessageService.send(sender, "&7Centro: &f" + centerX + "&7, &f" + centerZ + " &8| &7Radio: &f" + radius);
    }

    private void pause(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/ac pause <mundo>");
            return;
        }

        String worldId = args[1].toLowerCase();

        if (preloadManager.pause(worldId)) {
            MessageService.send(sender, "&ePregeneración pausada para &f" + worldId + "&e.");
        } else {
            MessageService.send(sender, "&cNo se pudo pausar. Puede que no esté en ejecución.");
        }
    }

    private void resume(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/ac resume <mundo>");
            return;
        }

        String worldId = args[1].toLowerCase();

        if (preloadManager.resume(worldId)) {
            MessageService.send(sender, "&aPregeneración reanudada para &e" + worldId + "&a.");
        } else {
            MessageService.send(sender, "&cNo se pudo reanudar. Puede que no esté pausada o ya exista otra tarea activa.");
        }
    }

    private void cancel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/ac cancel <mundo>");
            return;
        }

        String worldId = args[1].toLowerCase();

        if (preloadManager.cancel(worldId)) {
            MessageService.send(sender, "&cPregeneración cancelada para &e" + worldId + "&c.");
        } else {
            MessageService.send(sender, "&cNo se pudo cancelar. La tarea no existe.");
        }
    }

    private void status(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageService.send(sender, "&cUso: &e/ac status <mundo>");
            return;
        }

        String worldId = args[1].toLowerCase();

        Optional<ChunkPreloadTaskView> optionalTask = preloadManager.task(worldId);

        if (optionalTask.isEmpty()) {
            MessageService.send(sender, "&cNo hay tarea de pregeneración para &e" + worldId + "&c.");
            return;
        }

        ChunkPreloadTaskView task = optionalTask.get();

        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS CHUNKS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&7Mundo: &e" + task.worldId());
        MessageService.send(sender, "&7Estado: &f" + task.state().name());
        MessageService.send(sender, "&7Radio: &f" + task.radius());
        MessageService.send(sender, "&7Centro: &f" + task.centerX() + "&7, &f" + task.centerZ());
        MessageService.send(sender, "&7Chunk actual: &f" + task.currentChunkX() + "&7, &f" + task.currentChunkZ());
        MessageService.send(sender, "&7Procesados: &b" + format(task.processedChunks()) + "&7/&b" + format(task.totalChunks()));
        MessageService.send(sender, "&7Progreso: &a" + String.format("%.2f", task.progress()) + "%");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "");
    }

    private void sendHelp(CommandSender sender) {
        MessageService.send(sender, "");
        MessageService.send(sender, "<gradient:#F97316:#FACC15><bold>ARIATUS CHUNKS</bold></gradient>");
        MessageService.send(sender, "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>");
        MessageService.send(sender, "&e/ac start <mundo> <radio> [centerX] [centerZ] &7- Inicia pregeneración.");
        MessageService.send(sender, "&e/ac pause <mundo> &7- Pausa pregeneración.");
        MessageService.send(sender, "&e/ac resume <mundo> &7- Reanuda pregeneración.");
        MessageService.send(sender, "&e/ac cancel <mundo> &7- Cancela pregeneración.");
        MessageService.send(sender, "&e/ac status <mundo> &7- Muestra progreso.");
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

    private String format(long value) {
        return String.format("%,d", value);
    }
}