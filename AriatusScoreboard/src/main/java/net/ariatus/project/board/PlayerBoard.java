package net.ariatus.project.board;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.message.MessageService;
import net.ariatus.project.service.PlaceholderService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

public class PlayerBoard {

    private static final String OBJECTIVE_NAME = "ariatus";
    private static final String CONFIG_FILE = "config.yml";

    private final AriatusScoreboard module;
    private final Player player;
    private final PlaceholderService placeholders;

    private Scoreboard scoreboard;
    private Objective sidebarObjective;

    private final List<BoardLine> lines = new ArrayList<>();
    private int ticks;

    public PlayerBoard(AriatusScoreboard module, Player player, PlaceholderService placeholders) {
        this.module = module;
        this.player = player;
        this.placeholders = placeholders;
    }

    public void create() {
        if (!module.configBoolean(CONFIG_FILE, "sidebar.enabled", true)) {
            return;
        }

        ScoreboardManager manager = Bukkit.getScoreboardManager();

        if (manager == null) {
            module.logger().warn(module, "No se pudo obtener ScoreboardManager.");
            return;
        }

        this.scoreboard = manager.getNewScoreboard();

        this.sidebarObjective = scoreboard.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                MessageService.parse(parse(getTitleText()))
        );

        sidebarObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
        sidebarObjective.numberFormat(NumberFormat.blank());
        loadLines();

        player.setScoreboard(scoreboard);

        updateTitle();
        updateAllLines();
    }

    public void tick() {
        ticks++;

        if (sidebarObjective == null || scoreboard == null || !player.isOnline()) {
            return;
        }

        int titleUpdateTicks = getTitleUpdateTicks();

        if (titleUpdateTicks > 0 && ticks % titleUpdateTicks == 0) {
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

        ScoreboardManager manager = Bukkit.getScoreboardManager();

        if (manager != null && player.isOnline()) {
            player.setScoreboard(manager.getNewScoreboard());
        }

        lines.clear();
        sidebarObjective = null;
        scoreboard = null;
    }

    public Scoreboard scoreboard() {
        return scoreboard;
    }

    private void loadLines() {
        lines.clear();

        List<?> rawLines = module.config(CONFIG_FILE).getList("sidebar.lines");

        if (rawLines == null || rawLines.isEmpty()) {
            module.logger().warn(module, "No hay líneas configuradas en config.yml -> sidebar.lines");
            return;
        }

        int score = rawLines.size();

        for (Object raw : rawLines) {
            if (!(raw instanceof java.util.Map)) {
                module.logger().warn(module, "Línea inválida en sidebar.lines: " + raw);
                continue;
            }

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) raw;

            String text = String.valueOf(map.getOrDefault("text", ""));
            int updateTicks = parseUpdateTicks(map.get("update-ticks"), 40);

            lines.add(new BoardLine(score, uniqueEntry(score), text, updateTicks));
            score--;
        }

        module.logger().info(module, "Líneas cargadas en sidebar: " + lines.size());
    }

    private void updateTitle() {
        if (sidebarObjective == null) {
            return;
        }

        sidebarObjective.displayName(MessageService.parse(parse(getTitleText())));
    }

    private void updateAllLines() {
        for (BoardLine line : lines) {
            sidebarObjective.getScore(line.entry()).setScore(line.score());
            updateLine(line);
        }
    }

    private void updateLine(BoardLine line) {
        if (scoreboard == null || sidebarObjective == null) {
            return;
        }

        String parsedText = parse(line.rawText());

        Team team = scoreboard.getTeam("line_" + line.score());

        if (team == null) {
            team = scoreboard.registerNewTeam("line_" + line.score());
            team.addEntry(line.entry());
        }

        team.prefix(MessageService.parse(parsedText));

        if (module.configBoolean(CONFIG_FILE, "sidebar.hide-numbers", true)) {
            team.suffix(MessageService.parse("<reset>"));
        }

        sidebarObjective.getScore(line.entry()).setScore(line.score());
    }

    private String getTitleText() {
        return module.configString(
                CONFIG_FILE,
                "sidebar.title.text",
                "<gradient:#8A2BE2:#00D4FF><bold>ARIATUS.NET</bold></gradient>"
        );
    }

    private int getTitleUpdateTicks() {
        ConfigurationSection titleSection = module.config(CONFIG_FILE)
                .getConfigurationSection("sidebar.title");

        if (titleSection == null) {
            return 40;
        }

        return titleSection.getInt("update-ticks", 40);
    }

    private int parseUpdateTicks(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String parse(String text) {
        return placeholders.apply(player, text);
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
            int updateTicks
    ) {
    }
}