package net.ariatus.project.profiler;

import java.util.HashMap;
import java.util.Map;

public class ModuleProfiler {

    private final Map<String, ProfilerMetric> metrics = new HashMap<>();

    public void record(String moduleId, long nanos) {
        metrics.computeIfAbsent(moduleId.toLowerCase(), id -> new ProfilerMetric())
                .record(nanos);
    }

    public void error(String moduleId) {
        metrics.computeIfAbsent(moduleId.toLowerCase(), id -> new ProfilerMetric())
                .recordError();
    }

    public ProfilerMetric get(String moduleId) {
        return metrics.computeIfAbsent(moduleId.toLowerCase(), id -> new ProfilerMetric());
    }

    public Map<String, ProfilerMetric> all() {
        return metrics;
    }
}