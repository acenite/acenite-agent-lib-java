package com.acenite.spring;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(Filter.class)
@ConditionalOnProperty(prefix = "acenite", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AceniteProperties.class)
public class AceniteAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    AceniteLifecycle aceniteLifecycle(AceniteProperties properties) {
        return new AceniteLifecycle(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
            prefix = "acenite",
            name = "application-monitoring-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    AceniteHttpFilter aceniteHttpFilter() {
        return new AceniteHttpFilter();
    }
}
