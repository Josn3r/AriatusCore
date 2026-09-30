package net.ariatus.project.utils;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class PermissionUtils {

    private PermissionUtils() {
    }

    public static boolean has(
            CommandSender sender,
            String permission
    ) {
        if (sender == null) return false;

        if (
                permission == null
                        || permission.isBlank()
        ) {
            return true;
        }

        return sender.hasPermission(
                permission
        );
    }

    public static boolean hasOrOp(
            CommandSender sender,
            String permission
    ) {
        if (sender == null) return false;

        return sender.isOp()
                || has(
                sender,
                permission
        );
    }

    public static boolean hasAny(
            CommandSender sender,
            String... permissions
    ) {
        if (
                sender == null
                        || permissions == null
                        || permissions.length == 0
        ) {
            return false;
        }

        for (String permission : permissions) {
            if (has(sender, permission)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasAll(
            CommandSender sender,
            String... permissions
    ) {
        if (sender == null) return false;

        if (
                permissions == null
                        || permissions.length == 0
        ) {
            return true;
        }

        for (String permission : permissions) {
            if (!has(sender, permission)) {
                return false;
            }
        }

        return true;
    }

    public static List<String> missing(
            CommandSender sender,
            Collection<String> permissions
    ) {
        if (
                permissions == null
                        || permissions.isEmpty()
        ) {
            return List.of();
        }

        List<String> missing =
                new ArrayList<>();

        for (String permission : permissions) {
            if (!has(sender, permission)) {
                missing.add(
                        permission
                );
            }
        }

        return List.copyOf(
                missing
        );
    }
}