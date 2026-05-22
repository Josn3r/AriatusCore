package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerBoard {

    private final AriatusScoreboard module;
    private final Player player;

    private Scoreboard scoreboard;
    private Objective objective;

    private final List<BoardLine> lines = new ArrayList<>();
    private int ticks;

    public PlayerBoard(AriatusScoreboard module, Player player) {
        this.module = module;
        this.player = player;
    }

    public void create() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();

        scoreboard = manager.getNewScoreboard();
        objective = scoreboard.registerNewObjective(
                "ariatus",
                Criteria.DUMMY,
                MessageService.parse(getTitleText())
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        loadLines();

        player.setScoreboard(scoreboard);
        updateTitle();
        updateAllLines();
    }

    public void tick() {
        ticks++;

        if (objective == null || scoreboard == null || !player.isOnline()) {
            return;
        }

        ConfigurationSection titleSection = module.config("messages.yml")
                .getConfigurationSection("scoreboard.title");

        int titleUpdate = titleSection != null
                ? titleSection.getInt("update-ticks", 40)
                : 40;

        if (titleUpdate > 0 && ticks % titleUpdate == 0) {
            updateTitle();
        }

        for (BoardLine line : lines) {
            if (line.updateTicks() > 0 && ticks % line.updateTicks() == 0) {
                updateLine(line);
            }
        }
    }

    public void destroy() {
        if (scoreboard != null) {
            scoreboard.clearSlot(DisplaySlot.SIDEBAR);
        }

        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }

    private void loadLines() {
        lines.clear();

        List<?> rawLines = module.config("messages.yml").getList("scoreboard.lines");

        if (rawLines == null || rawLines.isEmpty()) {
            module.logger().warn(module, "No hay líneas configuradas en messages.yml -> scoreboard.lines");
            return;
        }

        int score = rawLines.size();

        for (Object raw : rawLines) {
            if (!(raw instanceof java.util.Map)) {
                module.logger().warn(module, "Línea inválida en scoreboard.lines: " + raw);
                continue;
            }

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) raw;

            String text = String.valueOf(map.getOrDefault("text", ""));
            int updateTicks = 40;

            Object updateTicksObject = map.get("update-ticks");

            if (updateTicksObject != null) {
                try {
                    updateTicks = Integer.parseInt(String.valueOf(updateTicksObject));
                } catch (NumberFormatException ignored) {
                    updateTicks = 40;
                }
            }

            lines.add(new BoardLine(score, uniqueEntry(score), text, updateTicks, null));
            score--;
        }

        module.logger().info(module, "Líneas cargadas en scoreboard: " + lines.size());
    }

    private void updateTitle() {
        objective.displayName(MessageService.parse(applyPlaceholders(getTitleText())));
    }

    private String getTitleText() {
        return module.configString(
                "messages.yml",
                "scoreboard.title.text",
                "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS.NET</bold></gradient>"
        );
    }

    private void updateAllLines() {
        for (BoardLine line : lines) {
            objective.getScore(line.entry()).setScore(line.score());
            updateLine(line);
        }
    }

    private void updateLine(BoardLine line) {
        String parsedText = applyPlaceholders(line.rawText());

        Team team = scoreboard.getTeam("line_" + line.score());

        if (team == null) {
            team = scoreboard.registerNewTeam("line_" + line.score());
            team.addEntry(line.entry());
        }

        team.prefix(MessageService.parse(parsedText));

        if (module.configBoolean("messages.yml", "scoreboard.hide-numbers", true)) {
            team.suffix(MessageService.parse("<reset>"));
        }

        objective.getScore(line.entry()).setScore(line.score());
    }

    private String applyPlaceholders(String text) {
        return text
                .replace("%player_name%", player.getName())
                .replace("%world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
    }

    private String uniqueEntry(int index) {
        ChatColor[] colors = ChatColor.values();

        if (index < colors.length) {
            return colors[index].toString();
        }

        return ChatColor.WHITE + "" + ChatColor.values()[index % colors.length];
    }

    private record BoardLine(
            int score,
            String entry,
            String rawText,
            int updateTicks,
            String lastValue
    ) {
    }
}