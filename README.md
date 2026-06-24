# Acenite Agent (Java)

The `com.acenite:acenite-agent-lib-java:0.2.0` artifact includes Spring Boot auto-configuration for Spring MVC. Configure a normal Boot application with:

```properties
acenite.api-key=${ACENITE_API_KEY}
acenite.service-name=orders-service
acenite.application-monitoring-enabled=true
acenite.heartbeat-enabled=true
acenite.heartbeat-interval-seconds=60
acenite.host-metrics-enabled=true
acenite.host-metrics-interval-seconds=60
```

The integration follows the Spring lifecycle and records request method, route, status, duration, failures, and exceptions. The core builder API remains available:

```java
AceniteAgent.start(
    AceniteAgentConfig.builder()
        .apiKey("your-api-key")
        .serviceName("orders-service")
        .framework("spring-boot")
        .enableApplicationMonitoring(true)
        .enableHostMetrics(true)
        .hostMetricsIntervalSeconds(60)
        .instanceId("server-01")
        .build()
);
```

Host metrics are sent to `/metrics/host` separately from heartbeat requests.
`network_rx_bytes` and `network_tx_bytes` are cumulative host counters; the Acenite backend calculates deltas and chart rates.

Set `ACENITE_ENVIRONMENT=production` or `ACENITE_ENVIRONMENT=development`.
Development starts application instrumentation only and does not send heartbeats
or host metrics. If unset, the agent warns once and defaults to production. See
https://acenite.com/docs/environments.
