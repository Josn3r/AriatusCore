package net.ariatus.project.task;

public class TaskMetrics {

    private long executions;
    private long totalNanos;
    private long maxNanos;
    private long errors;

    public void record(long nanos) {
        executions++;
        totalNanos += nanos;

        if (nanos > maxNanos) {
            maxNanos = nanos;
        }
    }

    public void recordError() {
        errors++;
    }

    public long executions() {
        return executions;
    }

    public double averageMillis() {
        if (executions == 0) {
            return 0.0;
        }

        return (totalNanos / 1_000_000.0) / executions;
    }

    public double maxMillis() {
        return maxNanos / 1_000_000.0;
    }

    public long errors() {
        return errors;
    }
}