package net.ariatus.project.message;

import net.ariatus.project.AriatusCore;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public final class MessagesManager {

    private final AriatusCore core;

    private File file;
    private YamlConfiguration config;

    public MessagesManager(
            AriatusCore core
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );
    }

    public void load() {
        file =
                new File(
                        core.getDataFolder(),
                        "messages.yml"
                );

        if (!file.exists()) {
            core.saveResource(
                    "messages.yml",
                    false
            );
        }

        config =
                YamlConfiguration.loadConfiguration(
                        file
                );
    }

    public void reload() {
        load();
    }

    public String get(
            String path
    ) {
        requireLoaded();

        return config.getString(
                path,
                path
        );
    }

    public String prefixed(
            String path
    ) {
        return get(
                "prefix"
        ) + get(
                path
        );
    }

    public Component component(
            String path
    ) {
        return MessageService.parse(
                get(
                        path
                )
        );
    }

    public Component component(
            String path,
            Map<String, ?> placeholders
    ) {
        return MessageService.parsePercent(
                get(
                        path
                ),
                placeholders
        );
    }

    public Component prefixedComponent(
            String path
    ) {
        return MessageService.parse(
                prefixed(
                        path
                )
        );
    }

    public Component prefixedComponent(
            String path,
            Map<String, ?> placeholders
    ) {
        return MessageService.parsePercent(
                prefixed(
                        path
                ),
                placeholders
        );
    }

    public void send(
            CommandSender sender,
            String path
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                component(
                        path
                )
        );
    }

    public void send(
            CommandSender sender,
            String path,
            Map<String, ?> placeholders
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                component(
                        path,
                        placeholders
                )
        );
    }

    public void sendPrefixed(
            CommandSender sender,
            String path
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                prefixedComponent(
                        path
                )
        );
    }

    public void sendPrefixed(
            CommandSender sender,
            String path,
            Map<String, ?> placeholders
    ) {
        Objects.requireNonNull(
                sender,
                "sender"
        );

        sender.sendMessage(
                prefixedComponent(
                        path,
                        placeholders
                )
        );
    }

    public String replace(
            String message,
            String key,
            String value
    ) {
        if (message == null) return "";

        return message.replace(
                "%"
                        + key
                        + "%",
                Objects.requireNonNullElse(
                        value,
                        ""
                )
        );
    }

    private void requireLoaded() {
        if (config == null) {
            throw new IllegalStateException(
                    "MessagesManager todavía no fue cargado."
            );
        }
    }
}