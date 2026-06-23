package com.acenite.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class HeartbeatScheduler {
    private final ScheduledExecutorService executorService;

    private HeartbeatScheduler(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public static HeartbeatScheduler start(String apiKey, double intervalSeconds) {
        HeartbeatClient heartbeatClient = new HeartbeatClient();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "acenite-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        long intervalMillis = Math.max(1L, Math.round(intervalSeconds * 1000.0));
        executorService.scheduleWithFixedDelay(
                () -> heartbeatClient.sendHeartbeat(apiKey, intervalSeconds),
                0,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );

        return new HeartbeatScheduler(executorService);
    }

    public void stop() {
        executorService.shutdownNow();
    }
}
