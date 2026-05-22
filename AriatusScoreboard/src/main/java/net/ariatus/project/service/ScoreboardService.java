package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardService {

    private final AriatusScoreboard module;
    private final Map<UUID, PlayerBoard> boards = new HashMap<>();

    public ScoreboardService(AriatusScoreboard module) {
        this.module = module;
    }

    public void start() {
        module.tasks().runRepeating(module, () -> {
            boards.values().forEach(PlayerBoard::tick);
        }, 20L, 1L);
    }

    public void create(Player player) {
        if (!module.configBoolean("messages.yml", "scoreboard.enabled", true)) {
            return;
        }

        remove(player);

        PlayerBoard board = new PlayerBoard(module, player);
        boards.put(player.getUniqueId(), board);
        board.create();
    }

    public void remove(Player player) {
        PlayerBoard board = boards.remove(player.getUniqueId());

        if (board != null) {
            board.destroy();
        }
    }

    public void shutdown() {
        boards.values().forEach(PlayerBoard::destroy);
        boards.clear();
    }
}