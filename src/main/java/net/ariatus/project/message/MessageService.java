package net.ariatus.project.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.Map;

public final class MessageService {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private static final Map<Character, String> LEGACY_COLORS = Map.ofEntries(
            Map.entry('0', "<black>"),
            Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"),
            Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"),
            Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"),
            Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"),
            Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"),
            Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"),
            Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"),
            Map.entry('f', "<white>"),
            Map.entry('l', "<bold>"),
            Map.entry('o', "<italic>"),
            Map.entry('n', "<underlined>"),
            Map.entry('m', "<strikethrough>"),
            Map.entry('r', "<reset>")
    );

    private MessageService() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(parse(message));
    }

    public static Component parse(String message) {
        return MINI_MESSAGE.deserialize(convertLegacyToMiniMessage(message));
    }

    private static String convertLegacyToMiniMessage(String message) {
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char current = message.charAt(i);

            if (current == '&' && i + 1 < message.length()) {
                char next = Character.toLowerCase(message.charAt(i + 1));

                // RGB legacy: &#FFAA00
                if (next == '#' && i + 7 < message.length()) {
                    String hex = message.substring(i + 2, i + 8);

                    if (hex.matches("[A-Fa-f0-9]{6}")) {
                        result.append("<#").append(hex).append(">");
                        i += 7;
                        continue;
                    }
                }

                String miniTag = LEGACY_COLORS.get(next);

                if (miniTag != null) {
                    result.append(miniTag);
                    i++;
                    continue;
                }
            }

            result.append(current);
        }

        return result.toString();
    }
}