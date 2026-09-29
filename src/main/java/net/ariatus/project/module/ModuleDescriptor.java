package net.ariatus.project.module;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public record ModuleDescriptor(
        String id,
        String name,
        String main,
        String version,
        List<String> dependencies,
        List<String> softDependencies,
        File file
) {

    public ModuleDescriptor {
        id = normalizeId(id);
        name = requireText(name, "name");
        main = requireText(main, "main");
        version = version == null || version.isBlank() ? "unknown" : version.trim();
        dependencies = normalizeDependencies(dependencies, id);
        softDependencies = normalizeDependencies(softDependencies, id);
        file = Objects.requireNonNull(file, "file");
    }

    private static String normalizeId(String value) {
        String id = requireText(value, "id").toLowerCase(Locale.ROOT);

        if (!id.matches("^[a-z0-9][a-z0-9_-]*$")) {
            throw new IllegalArgumentException("ID inválido: " + value + ". Solo se permite a-z, 0-9, - y _.");
        }

        return id;
    }

    private static List<String> normalizeDependencies(List<String> dependencies, String ownId) {
        if (dependencies == null || dependencies.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();

        for (String dependency : dependencies) {
            if (dependency == null || dependency.isBlank()) {
                continue;
            }

            String id = normalizeId(dependency);

            if (id.equals(ownId)) {
                throw new IllegalArgumentException("El módulo " + ownId + " no puede depender de sí mismo.");
            }

            normalized.add(id);
        }

        return List.copyOf(normalized);
    }

    private static String requireText(String value, String field) {
        String text = Objects.requireNonNull(value, field).trim();

        if (text.isEmpty()) {
            throw new IllegalArgumentException(field + " no puede estar vacío.");
        }

        return text;
    }
}