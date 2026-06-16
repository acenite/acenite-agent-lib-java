package com.acenite;

import com.acenite.internal.AceniteConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.net.InetAddress;

public final class AceniteAgentConfig {
    private final String apiKey;
    private final String serviceName;
    private final boolean enableLogging;
    private final boolean enableHeartbeat;
    private final double heartbeatIntervalSeconds;
    private final boolean enableHostMetrics;
    private final double hostMetricsIntervalSeconds;
    private final String instanceId;
    private final String hostname;
    private final String framework;
    private final List<String> instrumentations;

    private AceniteAgentConfig(Builder builder) {
        this.apiKey = builder.apiKey;
        this.serviceName = builder.serviceName;
        this.enableLogging = builder.enableLogging;
        this.enableHeartbeat = builder.enableHeartbeat;
        this.heartbeatIntervalSeconds = builder.heartbeatIntervalSeconds;
        this.enableHostMetrics = builder.enableHostMetrics;
        this.hostMetricsIntervalSeconds = builder.hostMetricsIntervalSeconds;
        this.hostname = normalizeHostName(builder.hostname);
        this.instanceId = normalizeInstanceId(builder.instanceId, this.hostname);
        this.framework = builder.framework;
        this.instrumentations = Collections.unmodifiableList(new ArrayList<>(builder.instrumentations));
    }

    public static Builder builder() {
        return new Builder();
    }

    static AceniteAgentConfig validate(AceniteAgentConfig config) {
        Objects.requireNonNull(config, "config must not be null");

        if (config.apiKey == null || config.apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }

        if (config.serviceName == null || config.serviceName.isBlank()) {
            throw new IllegalArgumentException("serviceName must not be blank");
        }

        if (config.heartbeatIntervalSeconds <= 0) {
            throw new IllegalArgumentException("heartbeatIntervalSeconds must be greater than 0");
        }
        if (config.hostMetricsIntervalSeconds <= 0) {
            throw new IllegalArgumentException("hostMetricsIntervalSeconds must be greater than 0");
        }

        if (config.framework != null && !AceniteConstants.ALLOWED_FRAMEWORKS.contains(config.framework)) {
            throw new IllegalArgumentException("Unsupported framework: " + config.framework);
        }

        for (String instrumentation : config.instrumentations) {
            if (!AceniteConstants.ALLOWED_INSTRUMENTATIONS.contains(instrumentation)) {
                throw new IllegalArgumentException("Unsupported instrumentation: " + instrumentation);
            }
        }

        return config;
    }

    public String apiKey() {
        return apiKey;
    }

    public String serviceName() {
        return serviceName;
    }

    public boolean enableLogging() {
        return enableLogging;
    }

    public boolean enableHeartbeat() {
        return enableHeartbeat;
    }

    public double heartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public boolean enableHostMetrics() {
        return enableHostMetrics;
    }

    public double hostMetricsIntervalSeconds() {
        return hostMetricsIntervalSeconds;
    }

    public String instanceId() {
        return instanceId;
    }

    public String hostname() {
        return hostname;
    }

    public String framework() {
        return framework;
    }

    public List<String> instrumentations() {
        return instrumentations;
    }

    public static final class Builder {
        private String apiKey;
        private String serviceName = "unknown-service";
        private boolean enableLogging = true;
        private boolean enableHeartbeat = true;
        private double heartbeatIntervalSeconds = 60.0;
        private boolean enableHostMetrics = true;
        private double hostMetricsIntervalSeconds = 60.0;
        private String instanceId;
        private String hostname;
        private String framework;
        private final List<String> instrumentations = new ArrayList<>();

        private Builder() {
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder enableLogging(boolean enableLogging) {
            this.enableLogging = enableLogging;
            return this;
        }

        public Builder enableHeartbeat(boolean enableHeartbeat) {
            this.enableHeartbeat = enableHeartbeat;
            return this;
        }

        public Builder heartbeatIntervalSeconds(double heartbeatIntervalSeconds) {
            this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
            return this;
        }

        public Builder enableHostMetrics(boolean enableHostMetrics) {
            this.enableHostMetrics = enableHostMetrics;
            return this;
        }

        public Builder hostMetricsIntervalSeconds(double hostMetricsIntervalSeconds) {
            this.hostMetricsIntervalSeconds = hostMetricsIntervalSeconds;
            return this;
        }

        public Builder instanceId(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder framework(String framework) {
            this.framework = framework;
            return this;
        }

        public Builder instrumentation(String instrumentation) {
            this.instrumentations.add(instrumentation);
            return this;
        }

        public Builder instrumentations(List<String> instrumentations) {
            this.instrumentations.clear();
            if (instrumentations != null) {
                this.instrumentations.addAll(instrumentations);
            }
            return this;
        }

        public AceniteAgentConfig build() {
            return new AceniteAgentConfig(this);
        }
    }

    private static String normalizeHostName(String configuredHostname) {
        if (configuredHostname != null && !configuredHostname.isBlank()) {
            return configuredHostname.trim();
        }

        try {
            String localHostName = InetAddress.getLocalHost().getHostName();
            if (localHostName != null && !localHostName.isBlank()) {
                return localHostName.trim();
            }
        } catch (RuntimeException | java.net.UnknownHostException ignored) {
            // Fall through to a stable non-empty placeholder.
        }

        return "unknown-host";
    }

    private static String normalizeInstanceId(String configuredInstanceId, String hostname) {
        if (configuredInstanceId != null && !configuredInstanceId.isBlank()) {
            return configuredInstanceId.trim();
        }
        return hostname;
    }
}
