package com.acenite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AceniteAgentPhase2Test {
    @AfterEach
    void stop() {
        AceniteAgent.stop();
    }

    @Test
    void validatesFrameworkAndIntervals() {
        assertThrows(IllegalArgumentException.class, () -> AceniteAgent.start(base().framework("struts").build()));
        assertThrows(IllegalArgumentException.class, () -> AceniteAgent.start(base().heartbeatIntervalSeconds(14).build()));
        assertThrows(IllegalArgumentException.class, () -> AceniteAgent.start(base().hostMetricsIntervalSeconds(301).build()));
    }

    @Test
    void canonicalCapabilitiesCanAllBeDisabledAndLifecycleIsIdempotent() {
        AceniteAgentConfig config = base().build();
        AceniteAgent.start(config);
        AceniteAgent.start(config);
        assertTrue(AceniteAgent.isStarted());
        assertNotNull(AceniteAgent.getTracer());
        AceniteAgent.stop();
        AceniteAgent.stop();
        assertFalse(AceniteAgent.isStarted());
    }

    @Test
    void enableLoggingRemainsApplicationCompatibilityAlias() {
        AceniteAgentConfig config = AceniteAgentConfig.builder()
                .apiKey("key")
                .enableLogging(false)
                .enableHeartbeat(false)
                .enableHostMetrics(false)
                .build();
        assertFalse(AceniteAgentConfig.validate(config).enableApplicationMonitoring());
    }

    private AceniteAgentConfig.Builder base() {
        return AceniteAgentConfig.builder()
                .apiKey("key")
                .framework("spring-boot")
                .enableApplicationMonitoring(false)
                .enableHeartbeat(false)
                .enableHostMetrics(false);
    }
}
