package com.acenite.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class HostMetricsScheduler {
    private final ScheduledExecutorService executorService;

    private HostMetricsScheduler(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public static HostMetricsScheduler start(
            String apiKey,
            String serviceName,
            String instanceId,
            String hostname,
            double intervalSeconds
    ) {
        HostMetricsClient hostMetricsClient = new HostMetricsClient();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "acenite-host-metrics");
            thread.setDaemon(true);
            return thread;
        });

        long intervalMillis = Math.max(1L, Math.round(intervalSeconds * 1000.0));
        executorService.scheduleWithFixedDelay(
                () -> hostMetricsClient.sendHostMetrics(
                        apiKey,
                        serviceName,
                        instanceId,
                        hostname,
                        intervalSeconds
                ),
                intervalMillis,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );

        return new HostMetricsScheduler(executorService);
    }

    public void stop() {
        executorService.shutdownNow();
    }
}
