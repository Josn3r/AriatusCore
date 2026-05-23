package net.ariatus.project.service;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class BelowNameService {

    private static final String CONFIG_FILE = "belowname.yml";
    private static final String OBJECTIVE_NAME = "ariatus_below";

    private final AriatusScoreboard module;
    private final PlaceholderService placeholders;

    private int ticks;

    public BelowNameService(AriatusScoreboard module, PlaceholderService placeholders) {
        this.module = module;
        this.placeholders = placeholders;
    }

    public void tick() {
        if (!module.configBoolean(CONFIG_FILE, "belowname.enabled", true)) {
            return;
        }

        ticks++;

        int updateTicks = module.configInt(CONFIG_FILE, "belowname.update-ticks", 20);

        if (updateTicks <= 0) {
            return;
        }

        if (ticks % updateTicks != 0) {
            return;
        }

        updateAll();
    }

    public void updateAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                updateBelowNameFor(viewer, target);
            }
        }
    }

    public void update(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateBelowNameFor(viewer, player);
            updateBelowNameFor(player, viewer);
        }
    }

    public void clear(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();

            if (scoreboard == null) {
                continue;
            }

            Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

            if (objective == null) {
                continue;
            }

            scoreboard.resetScores(player.getName());
        }
    }

    public void clearAll() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();

            if (scoreboard == null) {
                continue;
            }

            Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

            if (objective != null) {
                objective.unregister();
            }
        }
    }

    private void updateBelowNameFor(Player viewer, Player target) {
        Scoreboard scoreboard = viewer.getScoreboard();

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            objective = scoreboard.registerNewObjective(
                    OBJECTIVE_NAME,
                    "dummy",
                    MessageService.parse("")
            );

            objective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        String text = module.configString(
                CONFIG_FILE,
                "belowname.text",
                "%health% &c❤"
        );

        String parsedText = placeholders.apply(target, text);

        /*
         * API moderna:
         * Si tu API 26.1.2 permite texto directo/fancy value en belowname,
         * este es el punto donde se debe aplicar el texto final.
         *
         * En APIs antiguas, BELOW_NAME sigue usando score numérico + displayName.
         * Aquí dejamos valor numérico fallback para mantener compatibilidad
         * mientras conectamos el método moderno exacto de la API.
         */

        objective.displayName(MessageService.parse(parsedText));
        objective.numberFormat(NumberFormat.blank());
        objective.getScore(target.getName()).setScore(0);
    }
}