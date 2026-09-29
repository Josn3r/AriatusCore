package net.ariatus.project.profiler;

public record ProfilerMetricSnapshot(
        long executions,
        long errors,
        long totalNanos,
        long maxNanos,
        long mainThreadExecutions,
        long mainThreadTotalNanos,
        long mainThreadMaxNanos
) {

    public static final ProfilerMetricSnapshot EMPTY =
            new ProfilerMetricSnapshot(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0
            );

    public double totalMillis() {
        return totalNanos / 1_000_000.0;
    }

    public double averageMillis() {
        if (executions == 0) {
            return 0.0;
        }

        return totalMillis() / executions;
    }

    public double maxMillis() {
        return maxNanos / 1_000_000.0;
    }

    public double mainThreadTotalMillis() {
        return mainThreadTotalNanos / 1_000_000.0;
    }

    public double mainThreadAverageMillis() {
        if (mainThreadExecutions == 0) {
            return 0.0;
        }

        return mainThreadTotalMillis() / mainThreadExecutions;
    }

    public double mainThreadMaxMillis() {
        return mainThreadMaxNanos / 1_000_000.0;
    }

    public double errorRatePercent() {
        if (executions == 0) {
            return 0.0;
        }

        return (errors * 100.0) / executions;
    }
}