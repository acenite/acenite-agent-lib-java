package com.acenite.spring;

import com.acenite.AceniteAgent;
import com.acenite.AceniteAgentConfig;
import org.springframework.context.SmartLifecycle;

final class AceniteLifecycle implements SmartLifecycle {
    private final AceniteProperties properties;
    private volatile boolean running;

    AceniteLifecycle(AceniteProperties properties) {
        this.properties = properties;
    }

    @Override
    public void start() {
        if (running || !properties.isEnabled()) return;
        AceniteAgent.start(AceniteAgentConfig.builder()
                .apiKey(properties.getApiKey())
                .serviceName(properties.getServiceName())
                .framework("spring-boot")
                .enableApplicationMonitoring(properties.isApplicationMonitoringEnabled())
                .enableHeartbeat(properties.isHeartbeatEnabled())
                .heartbeatIntervalSeconds(properties.getHeartbeatIntervalSeconds())
                .enableHostMetrics(properties.isHostMetricsEnabled())
                .hostMetricsIntervalSeconds(properties.getHostMetricsIntervalSeconds())
                .enableLogging(properties.isLoggingEnabled())
                .build());
        running = true;
    }

    @Override
    public void stop() {
        if (!running) return;
        AceniteAgent.stop();
        running = false;
    }

    @Override
    public boolean isRunning() { return running; }

    @Override
    public boolean isAutoStartup() { return true; }

    @Override
    public int getPhase() { return Integer.MIN_VALUE; }
}
