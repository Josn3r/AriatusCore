package net.ariatus.project.profiler;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class ProfilerMetric {

    private final LongAdder executions = new LongAdder();
    private final LongAdder errors = new LongAdder();
    private final LongAdder totalNanos = new LongAdder();

    private final AtomicLong maxNanos = new AtomicLong();

    private final LongAdder mainThreadExecutions = new LongAdder();
    private final LongAdder mainThreadTotalNanos = new LongAdder();

    private final AtomicLong mainThreadMaxNanos = new AtomicLong();

    public void record(long nanos, boolean mainThread) {
        long elapsed = Math.max(0L, nanos);

        executions.increment();
        totalNanos.add(elapsed);

        maxNanos.accumulateAndGet(
                elapsed,
                Math::max
        );

        if (!mainThread) {
            return;
        }

        mainThreadExecutions.increment();
        mainThreadTotalNanos.add(elapsed);

        mainThreadMaxNanos.accumulateAndGet(
                elapsed,
                Math::max
        );
    }

    public void recordError() {
        errors.increment();
    }

    public ProfilerMetricSnapshot snapshot() {
        return new ProfilerMetricSnapshot(
                executions.sum(),
                errors.sum(),
                totalNanos.sum(),
                maxNanos.get(),
                mainThreadExecutions.sum(),
                mainThreadTotalNanos.sum(),
                mainThreadMaxNanos.get()
        );
    }
}