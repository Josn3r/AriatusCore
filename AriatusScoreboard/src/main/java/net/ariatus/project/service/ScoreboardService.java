package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.board.PlayerBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardService {

    private final AriatusScoreboard module;

    private final TabListService tabListService;
    private final NametagService nametagService;
    private final BelowNameService belowNameService;

    private final PlaceholderService placeholders;
    private final AnimationService animationService;

    private final Map<UUID, PlayerBoard> boards = new HashMap<>();

    public ScoreboardService(AriatusScoreboard module, PlaceholderService placeholders, AnimationService animationService) {
        this.module = module;
        this.placeholders = placeholders;
        this.animationService = animationService;

        this.tabListService = new TabListService(module, placeholders);
        this.nametagService = new NametagService(module, placeholders);
        this.belowNameService = new BelowNameService(module, placeholders);
    }

    public void start() {
        module.tasks().runRepeating(module, () -> {
            animationService.tick();

            boards.values().forEach(PlayerBoard::tick);

            tabListService.tick();
            nametagService.tick();
            belowNameService.tick();
        }, 20L, 1L);
    }

    public void create(Player player) {
        remove(player);

        PlayerBoard board = new PlayerBoard(module, player, placeholders);
        boards.put(player.getUniqueId(), board);

        board.create();
        tabListService.update(player);
        nametagService.update(player);
        belowNameService.update(player);
    }

    public void remove(Player player) {
        PlayerBoard board = boards.remove(player.getUniqueId());

        if (board != null) {
            board.destroy();
        }

        tabListService.clear(player);
        nametagService.clear(player);
        belowNameService.clear(player);
    }

    public void shutdown() {
        boards.values().forEach(PlayerBoard::destroy);
        boards.clear();

        Bukkit.getOnlinePlayers().forEach(tabListService::clear);
        Bukkit.getOnlinePlayers().forEach(nametagService::clear);
        belowNameService.clearAll();
    }

    //

    public void reload() {
        module.reloadConfig("config.yml");
        module.reloadConfig("nametags.yml");
        module.reloadConfig("belowname.yml");
        module.reloadConfig("animations.yml");

        animationService.reload();

        boards.values().forEach(PlayerBoard::destroy);
        boards.clear();

        Bukkit.getOnlinePlayers().forEach(this::create);

        module.logger().info(module, "AriatusScoreboard recargado correctamente.");
    }

    public void toggle(Player player) {
        if (boards.containsKey(player.getUniqueId())) {
            disableFor(player);
        } else {
            enableFor(player);
        }
    }

    public void enableFor(Player player) {
        create(player);
    }

    public void disableFor(Player player) {
        PlayerBoard board = boards.remove(player.getUniqueId());

        if (board != null) {
            board.destroy();
        }
    }

    public boolean isEnabledFor(Player player) {
        return boards.containsKey(player.getUniqueId());
    }

    public int activeBoards() {
        return boards.size();
    }


}