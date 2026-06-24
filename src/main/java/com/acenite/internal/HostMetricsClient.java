package com.acenite.internal;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HostMetricsClient {
    private static final Logger LOGGER = Logger.getLogger(HostMetricsClient.class.getName());
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;
    private final SystemInfo systemInfo;
    private long[] previousCpuTicks;

    public HostMetricsClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build(), new SystemInfo());
    }

    HostMetricsClient(HttpClient httpClient, SystemInfo systemInfo) {
        this.httpClient = httpClient;
        this.systemInfo = systemInfo;
    }

    public void sendHostMetrics(
            String apiKey,
            String serviceName,
            String instanceId,
            String hostname,
            double intervalSeconds,
            String environment
    ) {
        sleepWithJitter(intervalSeconds);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AceniteConstants.resolveAceniteUrl() + "/metrics/host"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-Acenite-Environment", environment)
                    .POST(HttpRequest.BodyPublishers.ofString(payload(serviceName, instanceId, hostname)))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException | RuntimeException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(Level.WARNING, "Acenite host metrics failed", error);
        }
    }

    private String payload(String serviceName, String instanceId, String hostname) {
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        OperatingSystem operatingSystem = systemInfo.getOperatingSystem();
        CentralProcessor processor = hardware.getProcessor();
        GlobalMemory memory = hardware.getMemory();
        DiskUsage disk = diskUsage(operatingSystem.getFileSystem());
        NetworkUsage network = networkUsage(hardware.getNetworkIFs());
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = Math.max(0L, totalMemory - availableMemory);
        double[] loadAverages = processor.getSystemLoadAverage(1);
        double loadAverage1m = loadAverages.length > 0 && loadAverages[0] >= 0 ? loadAverages[0] : 0.0;

        return "{"
                + "\"service_name\":\"" + escape(serviceName) + "\","
                + "\"instance_id\":\"" + escape(instanceId) + "\","
                + "\"hostname\":\"" + escape(hostname) + "\","
                + "\"timestamp\":\"" + Instant.now().toString() + "\","
                + "\"metrics\":{"
                + "\"cpu_percent\":" + number(cpuPercent(processor)) + ","
                + "\"memory_used_percent\":" + number(percent(usedMemory, totalMemory)) + ","
                + "\"memory_used_bytes\":" + usedMemory + ","
                + "\"memory_total_bytes\":" + totalMemory + ","
                + "\"disk_used_percent\":" + number(percent(disk.usedBytes(), disk.totalBytes())) + ","
                + "\"disk_used_bytes\":" + disk.usedBytes() + ","
                + "\"disk_total_bytes\":" + disk.totalBytes() + ","
                // Cumulative counters. The Acenite backend calculates deltas/rates.
                + "\"network_rx_bytes\":" + network.rxBytes() + ","
                + "\"network_tx_bytes\":" + network.txBytes() + ","
                + "\"load_average_1m\":" + number(loadAverage1m) + ","
                + "\"host_uptime_seconds\":" + operatingSystem.getSystemUptime()
                + "}"
                + "}";
    }

    private double cpuPercent(CentralProcessor processor) {
        long[] currentTicks = processor.getSystemCpuLoadTicks();
        if (previousCpuTicks == null) {
            previousCpuTicks = currentTicks;
            return 0.0;
        }

        double load = processor.getSystemCpuLoadBetweenTicks(previousCpuTicks);
        previousCpuTicks = currentTicks;
        return Math.max(0.0, Math.min(100.0, load * 100.0));
    }

    private static DiskUsage diskUsage(FileSystem fileSystem) {
        long total = 0L;
        long usable = 0L;
        for (OSFileStore store : fileSystem.getFileStores()) {
            total += Math.max(0L, store.getTotalSpace());
            usable += Math.max(0L, store.getUsableSpace());
        }
        return new DiskUsage(Math.max(0L, total - usable), total);
    }

    private static NetworkUsage networkUsage(List<NetworkIF> networkIFs) {
        long rx = 0L;
        long tx = 0L;
        for (NetworkIF networkIF : networkIFs) {
            networkIF.updateAttributes();
            rx += Math.max(0L, networkIF.getBytesRecv());
            tx += Math.max(0L, networkIF.getBytesSent());
        }
        return new NetworkUsage(rx, tx);
    }

    private static double percent(long used, long total) {
        if (total <= 0) {
            return 0.0;
        }
        return ((double) used / (double) total) * 100.0;
    }

    private static String number(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void sleepWithJitter(double intervalSeconds) {
        long maxJitterMillis = Math.max(0L, Math.round(intervalSeconds * 100.0));
        if (maxJitterMillis <= 0L) {
            return;
        }

        long jitterMillis = ThreadLocalRandom.current().nextLong(maxJitterMillis + 1);
        try {
            Thread.sleep(jitterMillis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    private record DiskUsage(long usedBytes, long totalBytes) {
    }

    private record NetworkUsage(long rxBytes, long txBytes) {
    }
}
