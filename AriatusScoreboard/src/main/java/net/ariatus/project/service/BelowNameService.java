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
            cleanupNpcEntries(viewer);

            for (Player target : Bukkit.getOnlinePlayers()) {
                updateBelowNameFor(viewer, target);
            }

            cleanupNpcEntries(viewer);
        }
    }

    public void update(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            cleanupNpcEntries(viewer);

            updateBelowNameFor(viewer, player);
            updateBelowNameFor(player, viewer);

            cleanupNpcEntries(viewer);
            cleanupNpcEntries(player);
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
            cleanupNpcEntries(viewer);
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

            cleanupNpcEntries(viewer);
        }
    }

    private void updateBelowNameFor(Player viewer, Player target) {
        Scoreboard scoreboard = viewer.getScoreboard();

        if (scoreboard == null) {
            return;
        }

        if (isNPCEntry(target.getName())) {
            scoreboard.resetScores(target.getName());
            cleanupNpcEntries(viewer);
            return;
        }

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

        objective.displayName(MessageService.parse(parsedText));
        objective.numberFormat(NumberFormat.blank());
        objective.getScore(target.getName()).setScore(0);
    }

    private void cleanupNpcEntries(Player viewer) {
        Scoreboard scoreboard = viewer.getScoreboard();

        if (scoreboard == null) {
            return;
        }

        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);

        if (objective == null) {
            return;
        }

        for (String entry : scoreboard.getEntries()) {
            if (isNPCEntry(entry)) {
                scoreboard.resetScores(entry);
            }
        }
    }

    private boolean isNPCEntry(String entry) {
        if (entry == null || entry.isBlank()) {
            return false;
        }

        try {
            net.ariatus.project.api.npc.NPCService npcService =
                    module.services().require(net.ariatus.project.api.npc.NPCService.class);

            return npcService.isNPCScoreboardEntry(entry);
        } catch (Exception ignored) {
            return false;
        }
    }
}