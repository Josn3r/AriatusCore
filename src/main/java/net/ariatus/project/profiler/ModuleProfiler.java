package net.ariatus.project.profiler;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ModuleProfiler {

    private final ConcurrentMap<String, ConcurrentMap<ProfilerCategory, ProfilerMetric>> metrics =
            new ConcurrentHashMap<>();

    public void record(
            String moduleId,
            ProfilerCategory category,
            long nanos,
            boolean mainThread
    ) {
        metric(
                moduleId,
                category
        ).record(
                nanos,
                mainThread
        );
    }

    public void error(
            String moduleId,
            ProfilerCategory category
    ) {
        metric(
                moduleId,
                category
        ).recordError();
    }

    public ModuleProfileSnapshot snapshot(
            String moduleId
    ) {
        String id =
                normalize(
                        moduleId
                );

        ConcurrentMap<ProfilerCategory, ProfilerMetric> moduleMetrics =
                metrics.get(id);

        if (moduleMetrics == null) {
            return new ModuleProfileSnapshot(
                    id,
                    Map.of()
            );
        }

        EnumMap<ProfilerCategory, ProfilerMetricSnapshot> snapshot =
                new EnumMap<>(
                        ProfilerCategory.class
                );

        moduleMetrics.forEach(
                (category, metric) ->
                        snapshot.put(
                                category,
                                metric.snapshot()
                        )
        );

        return new ModuleProfileSnapshot(
                id,
                snapshot
        );
    }

    public Map<String, ModuleProfileSnapshot> allSnapshots() {
        Map<String, ModuleProfileSnapshot> snapshots =
                new TreeMap<>();

        metrics.keySet()
                .forEach(moduleId ->
                        snapshots.put(
                                moduleId,
                                snapshot(moduleId)
                        )
                );

        return Map.copyOf(
                snapshots
        );
    }

    public Set<String> moduleIds() {
        return Set.copyOf(
                metrics.keySet()
        );
    }

    public boolean hasData(
            String moduleId
    ) {
        return metrics.containsKey(
                normalize(moduleId)
        );
    }

    public void reset(
            String moduleId
    ) {
        metrics.remove(
                normalize(moduleId)
        );
    }

    public void resetAll() {
        metrics.clear();
    }

    private ProfilerMetric metric(
            String moduleId,
            ProfilerCategory category
    ) {
        String id =
                normalize(
                        moduleId
                );

        return metrics.computeIfAbsent(
                        id,
                        ignored ->
                                new ConcurrentHashMap<>()
                )
                .computeIfAbsent(
                        category,
                        ignored ->
                                new ProfilerMetric()
                );
    }

    private String normalize(
            String moduleId
    ) {
        String id =
                moduleId == null
                        ? ""
                        : moduleId
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        if (id.isEmpty()) {
            throw new IllegalArgumentException(
                    "moduleId no puede estar vacío."
            );
        }

        return id;
    }
}