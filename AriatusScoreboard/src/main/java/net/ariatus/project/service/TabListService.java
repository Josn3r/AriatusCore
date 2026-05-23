package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class TabListService {

    private static final String CONFIG_FILE = "config.yml";

    private final AriatusScoreboard module;
    private final PlaceholderService placeholders;

    private int ticks;

    public TabListService(AriatusScoreboard module, PlaceholderService placeholders) {
        this.module = module;
        this.placeholders = placeholders;
    }

    public void tick() {
        if (!module.configBoolean(CONFIG_FILE, "tablist.enabled", true)) {
            return;
        }

        ticks++;

        int updateTicks = module.configInt(CONFIG_FILE, "tablist.update-ticks", 40);

        if (updateTicks <= 0) {
            return;
        }

        if (ticks % updateTicks != 0) {
            return;
        }

        updateAll();
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {
        if (!module.configBoolean(CONFIG_FILE, "tablist.enabled", true)) {
            return;
        }

        String header = buildText(player, module.config(CONFIG_FILE).getStringList("tablist.header"));
        String footer = buildText(player, module.config(CONFIG_FILE).getStringList("tablist.footer"));

        player.sendPlayerListHeaderAndFooter(
                MessageService.parse(header),
                MessageService.parse(footer)
        );
    }

    public void clear(Player player) {
        player.sendPlayerListHeaderAndFooter(
                MessageService.parse(""),
                MessageService.parse("")
        );
    }

    private String buildText(Player player, List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        return lines.stream()
                .map(line -> placeholders.apply(player, line))
                .collect(java.util.stream.Collectors.joining("\n"));
    }
}