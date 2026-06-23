package com.acenite.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AceniteAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AceniteAutoConfiguration.class)
            .withPropertyValues(
                    "acenite.api-key=test-key",
                    "acenite.application-monitoring-enabled=false",
                    "acenite.heartbeat-enabled=false",
                    "acenite.host-metrics-enabled=false"
            );

    @Test
    void bindsPropertiesAndStartsLifecycleWithoutDisabledSubsystems() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AceniteProperties.class);
            assertThat(context).hasSingleBean(AceniteLifecycle.class);
            assertThat(context).doesNotHaveBean(AceniteHttpFilter.class);
            assertThat(context.getBean(AceniteProperties.class).getApiKey()).isEqualTo("test-key");
        });
    }

    @Test
    void entireIntegrationCanBeDisabled() {
        runner.withPropertyValues("acenite.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(AceniteLifecycle.class));
    }
}
