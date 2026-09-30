package net.ariatus.project.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class MessageService {

    private static final MiniMessage MINI_MESSAGE =
            MiniMessage.miniMessage();

    private static final Map<Character, String> LEGACY_COLORS =
            Map.ofEntries(
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

    public static void send(
            CommandSender sender,
            String message
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                parse(
                        message
                )
        );
    }

    public static void send(
            CommandSender sender,
            String message,
            Map<String, ?> placeholders
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                parse(
                        message,
                        placeholders
                )
        );
    }

    public static void send(
            CommandSender sender,
            Component component
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                Objects.requireNonNullElse(
                        component,
                        Component.empty()
                )
        );
    }

    public static Component parse(
            String message
    ) {
        return MINI_MESSAGE.deserialize(
                legacyToMiniMessage(
                        message
                )
        );
    }

    public static Component parse(
            String message,
            Map<String, ?> placeholders
    ) {
        if (
                placeholders == null
                        || placeholders.isEmpty()
        ) {
            return parse(
                    message
            );
        }

        TagResolver.Builder resolver =
                TagResolver.builder();

        for (
                Map.Entry<String, ?> entry :
                placeholders.entrySet()
        ) {
            String key =
                    normalizeTagName(
                            entry.getKey()
                    );

            if (key.isBlank()) {
                continue;
            }

            resolver.resolver(
                    Placeholder.unparsed(
                            key,
                            String.valueOf(
                                    entry.getValue()
                            )
                    )
            );
        }

        return MINI_MESSAGE.deserialize(
                legacyToMiniMessage(
                        message
                ),
                resolver.build()
        );
    }

    public static Component parseComponents(
            String message,
            Map<String, Component> placeholders
    ) {
        if (
                placeholders == null
                        || placeholders.isEmpty()
        ) {
            return parse(
                    message
            );
        }

        TagResolver.Builder resolver =
                TagResolver.builder();

        for (
                Map.Entry<String, Component> entry :
                placeholders.entrySet()
        ) {
            String key =
                    normalizeTagName(
                            entry.getKey()
                    );

            if (key.isBlank()) {
                continue;
            }

            resolver.resolver(
                    Placeholder.component(
                            key,
                            Objects.requireNonNullElse(
                                    entry.getValue(),
                                    Component.empty()
                            )
                    )
            );
        }

        return MINI_MESSAGE.deserialize(
                legacyToMiniMessage(
                        message
                ),
                resolver.build()
        );
    }

    public static Component parsePercent(
            String message,
            Map<String, ?> placeholders
    ) {
        String template =
                Objects.requireNonNullElse(
                        message,
                        ""
                );

        if (
                placeholders == null
                        || placeholders.isEmpty()
        ) {
            return parse(
                    template
            );
        }

        TagResolver.Builder resolver =
                TagResolver.builder();

        int index = 0;

        for (
                Map.Entry<String, ?> entry :
                placeholders.entrySet()
        ) {
            String originalKey =
                    Objects.requireNonNullElse(
                            entry.getKey(),
                            ""
                    );

            if (originalKey.isBlank()) {
                continue;
            }

            String tag =
                    "arg_"
                            + index++;

            template =
                    template.replace(
                            "%"
                                    + originalKey
                                    + "%",
                            "<"
                                    + tag
                                    + ">"
                    );

            resolver.resolver(
                    Placeholder.unparsed(
                            tag,
                            String.valueOf(
                                    entry.getValue()
                            )
                    )
            );
        }

        return MINI_MESSAGE.deserialize(
                legacyToMiniMessage(
                        template
                ),
                resolver.build()
        );
    }

    public static void blank(
            CommandSender sender
    ) {
        send(
                sender,
                Component.empty()
        );
    }

    public static void header(
            CommandSender sender,
            String title
    ) {
        blank(
                sender
        );

        send(
                sender,
                "<gradient:#FACC15:#F97316><bold><title></bold></gradient>",
                Map.of(
                        "title",
                        Objects.requireNonNullElse(
                                title,
                                ""
                        )
                )
        );

        send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );
    }

    public static void footer(
            CommandSender sender
    ) {
        send(
                sender,
                "<dark_gray>━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━</dark_gray>"
        );

        blank(
                sender
        );
    }

    public static String legacyToMiniMessage(
            String message
    ) {
        String source =
                Objects.requireNonNullElse(
                        message,
                        ""
                );

        StringBuilder result =
                new StringBuilder(
                        source.length()
                );

        for (
                int i = 0;
                i < source.length();
                i++
        ) {
            char current =
                    source.charAt(
                            i
                    );

            if (
                    current == '&'
                            && i + 1
                            < source.length()
            ) {
                char next =
                        Character.toLowerCase(
                                source.charAt(
                                        i + 1
                                )
                        );

                if (
                        next == '#'
                                && i + 7
                                < source.length()
                ) {
                    String hex =
                            source.substring(
                                    i + 2,
                                    i + 8
                            );

                    if (
                            hex.matches(
                                    "[A-Fa-f0-9]{6}"
                            )
                    ) {
                        result.append(
                                "<#"
                        );

                        result.append(
                                hex
                        );

                        result.append(
                                ">"
                        );

                        i += 7;

                        continue;
                    }
                }

                String miniTag =
                        LEGACY_COLORS.get(
                                next
                        );

                if (miniTag != null) {
                    result.append(
                            miniTag
                    );

                    i++;

                    continue;
                }
            }

            result.append(
                    current
            );
        }

        return result.toString();
    }

    private static String normalizeTagName(
            String key
    ) {
        String source =
                Objects.requireNonNullElse(
                                key,
                                ""
                        )
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (source.isBlank()) {
            return "";
        }

        StringBuilder result =
                new StringBuilder(
                        source.length()
                );

        for (
                char character :
                source.toCharArray()
        ) {
            if (
                    Character.isLetterOrDigit(
                            character
                    )
                            || character == '_'
                            || character == '-'
            ) {
                result.append(
                        character
                );

            } else {
                result.append(
                        '_'
                );
            }
        }

        return result.toString();
    }
}