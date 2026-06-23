package com.acenite;

import com.acenite.internal.AceniteConstants;
import com.acenite.internal.HeartbeatScheduler;
import com.acenite.internal.HostMetricsScheduler;
import com.acenite.internal.OpenTelemetryBootstrap;
import io.opentelemetry.api.trace.Tracer;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AceniteAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final Object LOCK = new Object();

    private static HeartbeatScheduler heartbeatScheduler;
    private static HostMetricsScheduler hostMetricsScheduler;
    private static OpenTelemetryBootstrap openTelemetryBootstrap;
    private static boolean shutdownHookRegistered;

    private AceniteAgent() {
    }

    public static void start(AceniteAgentConfig config) {
        AceniteAgentConfig validatedConfig = AceniteAgentConfig.validate(config);

        synchronized (LOCK) {
            if (STARTED.get()) {
                return;
            }

            AceniteConstants.logLocalOverrideIfActive(validatedConfig.enableLogging());

            OpenTelemetryBootstrap candidateOpenTelemetry = null;
            HeartbeatScheduler candidateHeartbeatScheduler = null;
            HostMetricsScheduler candidateHostMetricsScheduler = null;

            try {
                if (validatedConfig.enableApplicationMonitoring()) {
                    candidateOpenTelemetry = OpenTelemetryBootstrap.start(
                            validatedConfig.apiKey(),
                            validatedConfig.serviceName()
                    );
                }

                if (validatedConfig.enableHeartbeat()) {
                    candidateHeartbeatScheduler = HeartbeatScheduler.start(
                            validatedConfig.apiKey(),
                            validatedConfig.heartbeatIntervalSeconds()
                    );
                }

                if (validatedConfig.enableHostMetrics()) {
                    candidateHostMetricsScheduler = HostMetricsScheduler.start(
                            validatedConfig.apiKey(),
                            validatedConfig.serviceName(),
                            validatedConfig.instanceId(),
                            validatedConfig.hostname(),
                            validatedConfig.hostMetricsIntervalSeconds()
                    );
                }

                openTelemetryBootstrap = candidateOpenTelemetry;
                heartbeatScheduler = candidateHeartbeatScheduler;
                hostMetricsScheduler = candidateHostMetricsScheduler;
                STARTED.set(true);
                registerShutdownHook();
            } catch (RuntimeException error) {
                if (candidateHostMetricsScheduler != null) {
                    candidateHostMetricsScheduler.stop();
                }
                if (candidateHeartbeatScheduler != null) {
                    candidateHeartbeatScheduler.stop();
                }
                if (candidateOpenTelemetry != null) {
                    candidateOpenTelemetry.shutdown();
                }
                throw error;
            }
        }
    }

    public static Tracer getTracer() {
        synchronized (LOCK) {
            if (openTelemetryBootstrap != null) {
                return openTelemetryBootstrap.getTracer();
            }
        }

        return OpenTelemetryBootstrap.noopTracer();
    }

    public static void stop() {
        synchronized (LOCK) {
            if (heartbeatScheduler != null) {
                heartbeatScheduler.stop();
                heartbeatScheduler = null;
            }

            if (hostMetricsScheduler != null) {
                hostMetricsScheduler.stop();
                hostMetricsScheduler = null;
            }

            if (openTelemetryBootstrap != null) {
                openTelemetryBootstrap.shutdown();
                openTelemetryBootstrap = null;
            }

            STARTED.set(false);
        }
    }

    static boolean isStarted() {
        return STARTED.get();
    }

    private static void registerShutdownHook() {
        if (shutdownHookRegistered) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(AceniteAgent::stop, "acenite-agent-shutdown"));
        shutdownHookRegistered = true;
    }
}
