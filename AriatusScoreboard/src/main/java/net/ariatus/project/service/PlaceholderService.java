package net.ariatus.project.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderService {

    private static final Pattern ANIMATION_PATTERN =
            Pattern.compile("%animation_([a-zA-Z0-9_-]+)%");

    private final AnimationService animationService;

    public PlaceholderService(AnimationService animationService) {
        this.animationService = animationService;
    }

    public String apply(Player player, String text) {
        if (text == null) {
            return "";
        }

        String result = text;

        result = applyAnimations(result);

        result = result
                .replace("%player_name%", player.getName())
                .replace("%player_uuid%", player.getUniqueId().toString())
                .replace("%world%", player.getWorld().getName())
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%health%", String.valueOf((int) Math.ceil(player.getHealth())))
                .replace("%max_health%", String.valueOf((int) Math.ceil(player.getMaxHealth())))
                .replace("%level%", String.valueOf(player.getLevel()))
                .replace("%food%", String.valueOf(player.getFoodLevel()))
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%time%", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .replace("%date%", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("%perm-prefix%", "");

        /*
         * Importante:
         * aplicamos animaciones una segunda vez por si un frame trae placeholders.
         * Ejemplo: %animation_time% -> "&7Time &b%time%"
         */
        result = applyAnimations(result);

        return result;
    }

    public String joinLines(Player player, java.util.List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        return lines.stream()
                .map(line -> apply(player, line))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    private String applyAnimations(String text) {
        Matcher matcher = ANIMATION_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String animationId = matcher.group(1);
            String frame = animationService.frame(animationId);

            matcher.appendReplacement(buffer, Matcher.quoteReplacement(frame));
        }

        matcher.appendTail(buffer);

        return buffer.toString();
    }
}