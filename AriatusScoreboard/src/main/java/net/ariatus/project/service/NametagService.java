package net.ariatus.project.service;

import net.ariatus.project.AriatusScoreboard;
import net.ariatus.project.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Comparator;
import java.util.Optional;

public class NametagService {

    private static final String CONFIG_FILE = "nametags.yml";
    private static final String TEAM_PREFIX = "at_";

    private final AriatusScoreboard module;
    private final PlaceholderService placeholders;

    private int ticks;

    public NametagService(AriatusScoreboard module, PlaceholderService placeholders) {
        this.module = module;
        this.placeholders = placeholders;
    }

    public void tick() {
        ticks++;

        int updateTicks = module.configInt(CONFIG_FILE, "settings.update-ticks", 20);

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
                updateNametagFor(viewer, target);
            }

            updateTabName(viewer);
        }
    }

    public void update(Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateNametagFor(viewer, player);
            updateNametagFor(player, viewer);
        }

        updateTabName(player);
    }

    public void clear(Player player) {
        player.playerListName(null);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard scoreboard = viewer.getScoreboard();

            if (scoreboard == null) {
                continue;
            }

            unregisterTeamsFor(scoreboard, player);
        }
    }

    private void updateTabName(Player player) {
        TagData tagData = resolveTagData(player);

        String format = module.configString(
                CONFIG_FILE,
                "settings.tabname-format",
                "%tabprefix%%player_name%%tabsuffix%"
        );

        String text = format
                .replace("%tabprefix%", tagData.tabPrefix())
                .replace("%tabsuffix%", tagData.tabSuffix());

        text = placeholders.apply(player, text);

        player.playerListName(MessageService.parse(text));
    }

    private void updateNametagFor(Player viewer, Player target) {
        Scoreboard scoreboard = viewer.getScoreboard();

        if (scoreboard == null) {
            return;
        }

        TagData tagData = resolveTagData(target);

        String teamName = teamName(target, tagData);

        unregisterDifferentTeamsFor(scoreboard, target, teamName);

        Team team = scoreboard.getTeam(teamName);

        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }

        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }

        String prefixFormat = module.configString(
                CONFIG_FILE,
                "settings.nametag-format.prefix",
                "%tagprefix%"
        );

        String suffixFormat = module.configString(
                CONFIG_FILE,
                "settings.nametag-format.suffix",
                "%tagsuffix%"
        );

        String prefix = prefixFormat.replace("%tagprefix%", tagData.tagPrefix());
        String suffix = suffixFormat.replace("%tagsuffix%", tagData.tagSuffix());

        prefix = placeholders.apply(target, prefix);
        suffix = placeholders.apply(target, suffix);

        team.prefix(MessageService.parse(prefix));
        team.suffix(MessageService.parse(suffix));
    }

    private TagData resolveTagData(Player player) {
        GroupData group = resolveGroup(player);
        SuffixData suffix = resolveSuffix(player);

        return new TagData(
                group.name(),
                group.priority(),
                group.tabPrefix(),
                group.tagPrefix(),
                suffix.name(),
                suffix.tabSuffix(),
                suffix.tagSuffix()
        );
    }

    private GroupData resolveGroup(Player player) {
        ConfigurationSection groupsSection = module.config(CONFIG_FILE).getConfigurationSection("groups");
        String defaultGroupName = module.configString(CONFIG_FILE, "settings.default-group", "_DEFAULT_");

        if (groupsSection == null) {
            return new GroupData(defaultGroupName, 0, "", "");
        }

        Optional<GroupData> bestGroup = groupsSection.getKeys(false).stream()
                .filter(groupName -> !groupName.equalsIgnoreCase(defaultGroupName))
                .map(groupName -> readGroup(groupsSection, groupName))
                .filter(group -> hasGroupPermission(player, group))
                .max(Comparator.comparingInt(GroupData::priority));

        if (bestGroup.isPresent()) {
            return bestGroup.get();
        }

        return readGroup(groupsSection, defaultGroupName);
    }

    private GroupData readGroup(ConfigurationSection groupsSection, String groupName) {
        int priority = groupsSection.getInt(groupName + ".priority", 0);
        String tabPrefix = groupsSection.getString(groupName + ".tabprefix", "");
        String tagPrefix = groupsSection.getString(groupName + ".tagprefix", "");

        return new GroupData(groupName, priority, tabPrefix, tagPrefix);
    }

    private boolean hasGroupPermission(Player player, GroupData group) {
        ConfigurationSection groupsSection = module.config(CONFIG_FILE).getConfigurationSection("groups");

        if (groupsSection == null) {
            return false;
        }

        String permission = groupsSection.getString(group.name() + ".permission", "");

        return !permission.isBlank() && player.hasPermission(permission);
    }

    private SuffixData resolveSuffix(Player player) {
        ConfigurationSection suffixSection = module.config(CONFIG_FILE).getConfigurationSection("suffixs");

        String defaultSuffix = module.configString(CONFIG_FILE, "settings.default-suffix", "NONE");

        if (suffixSection == null) {
            return new SuffixData(defaultSuffix, "", "");
        }

        for (String suffixName : suffixSection.getKeys(false)) {
            if (suffixName.equalsIgnoreCase(defaultSuffix)) {
                continue;
            }

            String permission = suffixSection.getString(suffixName + ".permission", "");

            if (!permission.isBlank() && player.hasPermission(permission)) {
                String tabSuffix = suffixSection.getString(suffixName + ".tabsuffix", "");
                String tagSuffix = suffixSection.getString(suffixName + ".tagsuffix", "");
                return new SuffixData(suffixName, tabSuffix, tagSuffix);
            }
        }

        String tabSuffix = suffixSection.getString(defaultSuffix + ".tabsuffix", "");
        String tagSuffix = suffixSection.getString(defaultSuffix + ".tagsuffix", "");

        return new SuffixData(defaultSuffix, tabSuffix, tagSuffix);
    }

    private String teamName(Player player, TagData tagData) {
        String uuidPart = player.getUniqueId().toString().replace("-", "").substring(0, 8);

        if (!isTabSortingEnabled()) {
            return TEAM_PREFIX + "9999_" + uuidPart;
        }

        int maxPriority = module.configInt(CONFIG_FILE, "settings.tab-sorting.max-priority", 9999);
        int sortWeight = Math.max(0, maxPriority - tagData.groupPriority());

        return TEAM_PREFIX + String.format("%04d", sortWeight) + "_" + uuidPart;
    }

    private boolean isTabSortingEnabled() {
        return module.configBoolean(CONFIG_FILE, "settings.tab-sorting.enabled", true);
    }

    private void unregisterDifferentTeamsFor(Scoreboard scoreboard, Player player, String expectedTeamName) {
        for (Team team : scoreboard.getTeams()) {
            if (!team.getName().startsWith(TEAM_PREFIX)) {
                continue;
            }

            if (!team.hasEntry(player.getName())) {
                continue;
            }

            if (team.getName().equals(expectedTeamName)) {
                continue;
            }

            team.removeEntry(player.getName());

            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private void unregisterTeamsFor(Scoreboard scoreboard, Player player) {
        for (Team team : scoreboard.getTeams()) {
            if (!team.getName().startsWith(TEAM_PREFIX)) {
                continue;
            }

            if (!team.hasEntry(player.getName())) {
                continue;
            }

            team.removeEntry(player.getName());

            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    private record GroupData(
            String name,
            int priority,
            String tabPrefix,
            String tagPrefix
    ) {
    }

    private record SuffixData(
            String name,
            String tabSuffix,
            String tagSuffix
    ) {
    }

    private record TagData(
            String groupName,
            int groupPriority,
            String tabPrefix,
            String tagPrefix,
            String suffixName,
            String tabSuffix,
            String tagSuffix
    ) {
    }
}