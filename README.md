# Acenite Agent (Java)

Java services can enable lightweight host resource metrics alongside tracing and heartbeat monitoring:

```java
AceniteAgent.start(
    AceniteAgentConfig.builder()
        .apiKey("your-api-key")
        .serviceName("orders-service")
        .enableHostMetrics(true)
        .hostMetricsIntervalSeconds(60)
        .instanceId("server-01")
        .build()
);
```

Host metrics are sent to `/server/metrics/host` separately from heartbeat requests.
`network_rx_bytes` and `network_tx_bytes` are cumulative host counters; the Acenite backend calculates deltas and chart rates.
