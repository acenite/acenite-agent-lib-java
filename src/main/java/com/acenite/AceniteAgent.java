package com.acenite;

import com.acenite.internal.HeartbeatScheduler;
import com.acenite.internal.OpenTelemetryBootstrap;
import io.opentelemetry.api.trace.Tracer;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AceniteAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final Object LOCK = new Object();

    private static HeartbeatScheduler heartbeatScheduler;
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

            OpenTelemetryBootstrap candidateOpenTelemetry = null;
            HeartbeatScheduler candidateHeartbeatScheduler = null;

            try {
                if (validatedConfig.enableLogging()) {
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

                openTelemetryBootstrap = candidateOpenTelemetry;
                heartbeatScheduler = candidateHeartbeatScheduler;
                STARTED.set(true);
                registerShutdownHook();
            } catch (RuntimeException error) {
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
