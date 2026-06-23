package com.acenite.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("acenite")
public class AceniteProperties {
    private boolean enabled = true;
    private String apiKey;
    private String serviceName = "unknown-service";
    private boolean applicationMonitoringEnabled = true;
    private boolean heartbeatEnabled = true;
    private double heartbeatIntervalSeconds = 60;
    private boolean hostMetricsEnabled = true;
    private double hostMetricsIntervalSeconds = 60;
    private boolean loggingEnabled = true;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public boolean isApplicationMonitoringEnabled() { return applicationMonitoringEnabled; }
    public void setApplicationMonitoringEnabled(boolean value) { this.applicationMonitoringEnabled = value; }
    public boolean isHeartbeatEnabled() { return heartbeatEnabled; }
    public void setHeartbeatEnabled(boolean value) { this.heartbeatEnabled = value; }
    public double getHeartbeatIntervalSeconds() { return heartbeatIntervalSeconds; }
    public void setHeartbeatIntervalSeconds(double value) { this.heartbeatIntervalSeconds = value; }
    public boolean isHostMetricsEnabled() { return hostMetricsEnabled; }
    public void setHostMetricsEnabled(boolean value) { this.hostMetricsEnabled = value; }
    public double getHostMetricsIntervalSeconds() { return hostMetricsIntervalSeconds; }
    public void setHostMetricsIntervalSeconds(double value) { this.hostMetricsIntervalSeconds = value; }
    public boolean isLoggingEnabled() { return loggingEnabled; }
    public void setLoggingEnabled(boolean value) { this.loggingEnabled = value; }
}
