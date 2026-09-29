package net.ariatus.project.profiler;

import java.util.Map;
import java.util.Objects;

public record ModuleProfileSnapshot(
        String moduleId,
        Map<ProfilerCategory, ProfilerMetricSnapshot> metrics
) {

    public ModuleProfileSnapshot {
        Objects.requireNonNull(
                moduleId,
                "moduleId"
        );

        metrics = Map.copyOf(
                Objects.requireNonNull(
                        metrics,
                        "metrics"
                )
        );
    }

    public ProfilerMetricSnapshot metric(
            ProfilerCategory category
    ) {
        return metrics.getOrDefault(
                category,
                ProfilerMetricSnapshot.EMPTY
        );
    }

    public long totalExecutions() {
        return metrics.values()
                .stream()
                .mapToLong(
                        ProfilerMetricSnapshot::executions
                )
                .sum();
    }

    public long totalErrors() {
        return metrics.values()
                .stream()
                .mapToLong(
                        ProfilerMetricSnapshot::errors
                )
                .sum();
    }

    public double totalMillis() {
        return metrics.values()
                .stream()
                .mapToDouble(
                        ProfilerMetricSnapshot::totalMillis
                )
                .sum();
    }

    public long mainThreadExecutions() {
        return metrics.values()
                .stream()
                .mapToLong(
                        ProfilerMetricSnapshot::mainThreadExecutions
                )
                .sum();
    }

    public double mainThreadTotalMillis() {
        return metrics.values()
                .stream()
                .mapToDouble(
                        ProfilerMetricSnapshot::mainThreadTotalMillis
                )
                .sum();
    }

    public double mainThreadAverageMillis() {
        long executions =
                mainThreadExecutions();

        if (executions == 0) {
            return 0.0;
        }

        return mainThreadTotalMillis()
                / executions;
    }
}