package net.ariatus.project.integration;

import net.ariatus.project.AriatusCore;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PlaceholderService {

    private final AriatusCore core;

    private volatile Accessor accessor;

    public PlaceholderService(
            AriatusCore core
    ) {
        this.core =
                Objects.requireNonNull(
                        core,
                        "core"
                );
    }

    public boolean available() {
        Plugin plugin =
                Bukkit.getPluginManager()
                        .getPlugin(
                                "PlaceholderAPI"
                        );

        return plugin != null
                && plugin.isEnabled();
    }

    public String parse(
            OfflinePlayer player,
            String text
    ) {
        if (
                text == null
                        || text.isEmpty()
        ) {
            return Objects.requireNonNullElse(
                    text,
                    ""
            );
        }

        Plugin plugin =
                Bukkit.getPluginManager()
                        .getPlugin(
                                "PlaceholderAPI"
                        );

        if (
                plugin == null
                        || !plugin.isEnabled()
        ) {
            return text;
        }

        try {
            Accessor current =
                    accessor;

            if (
                    current == null
                            || current.plugin()
                            != plugin
            ) {
                current =
                        createAccessor(
                                plugin
                        );

                accessor =
                        current;
            }

            Object result =
                    current.setPlaceholders()
                            .invoke(
                                    null,
                                    player,
                                    text
                            );

            return result
                    instanceof String parsed
                    ? parsed
                    : text;

        } catch (
                ReflectiveOperationException
                        | LinkageError exception
        ) {
            accessor =
                    null;

            core.loggerService()
                    .warn(
                            "PlaceholderAPI: no se pudieron procesar placeholders: "
                                    + rootMessage(
                                    exception
                            )
                    );

            return text;
        }
    }

    public List<String> parse(
            OfflinePlayer player,
            List<String> lines
    ) {
        if (
                lines == null
                        || lines.isEmpty()
        ) {
            return List.of();
        }

        List<String> parsed =
                new ArrayList<>(
                        lines.size()
                );

        for (String line : lines) {
            parsed.add(
                    parse(
                            player,
                            Objects.requireNonNullElse(
                                    line,
                                    ""
                            )
                    )
            );
        }

        return List.copyOf(
                parsed
        );
    }

    public void invalidate() {
        accessor = null;
    }

    private Accessor createAccessor(
            Plugin plugin
    ) throws
            ClassNotFoundException,
            NoSuchMethodException {

        ClassLoader loader =
                plugin.getClass()
                        .getClassLoader();

        Class<?> placeholderApiClass =
                Class.forName(
                        "me.clip.placeholderapi.PlaceholderAPI",
                        true,
                        loader
                );

        Method setPlaceholders =
                placeholderApiClass.getMethod(
                        "setPlaceholders",
                        OfflinePlayer.class,
                        String.class
                );

        return new Accessor(
                plugin,
                setPlaceholders
        );
    }

    private static String rootMessage(
            Throwable throwable
    ) {
        Throwable current =
                throwable;

        if (
                current
                        instanceof InvocationTargetException invocationTargetException
                        && invocationTargetException
                        .getCause()
                        != null
        ) {
            current =
                    invocationTargetException
                            .getCause();
        }

        while (
                current.getCause()
                        != null
        ) {
            current =
                    current.getCause();
        }

        String message =
                current.getMessage();

        return message == null
                ? current.getClass()
                .getSimpleName()
                : message;
    }

    private record Accessor(
            Plugin plugin,
            Method setPlaceholders
    ) {
    }
}