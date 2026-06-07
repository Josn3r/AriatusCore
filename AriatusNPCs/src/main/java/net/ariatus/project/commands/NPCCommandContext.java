package net.ariatus.project.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class NPCCommandContext {

    private final List<String> arguments = new ArrayList<>();
    private final Map<String, List<String>> flags = new HashMap<>();

    NPCCommandContext(String[] args, int start) {
        for (int i = start; i < args.length; i++) {
            String current = args[i];

            if (current.startsWith("--")) {
                String flag = current.substring(2).toLowerCase();
                List<String> values = new ArrayList<>();

                int j = i + 1;

                while (j < args.length && !args[j].startsWith("--")) {
                    values.add(args[j]);
                    j++;
                }

                flags.put(flag, values);
                i = j - 1;
                continue;
            }

            arguments.add(current);
        }
    }

    String arg(int index) {
        if (index < 0 || index >= arguments.size()) {
            return null;
        }

        return arguments.get(index);
    }

    String requireArg(int index) {
        String value = arg(index);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Falta argumento.");
        }

        return value;
    }

    boolean hasFlag(String flag) {
        return flags.containsKey(flag.toLowerCase());
    }

    List<String> flag(String flag) {
        return flags.getOrDefault(flag.toLowerCase(), List.of());
    }

    String flagOne(String flag, String def) {
        List<String> values = flag(flag);

        if (values.isEmpty()) {
            return def;
        }

        return values.getFirst();
    }

    String joinArgs(int start) {
        StringBuilder builder = new StringBuilder();

        for (int i = start; i < arguments.size(); i++) {
            if (i > start) {
                builder.append(" ");
            }

            builder.append(arguments.get(i));
        }

        return builder.toString();
    }

    List<String> args() {
        return arguments;
    }
}