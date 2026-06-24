package com.acenite.internal;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HeartbeatClient {
    private static final Logger LOGGER = Logger.getLogger(HeartbeatClient.class.getName());
    private static final String BOOT_ID = UUID.randomUUID().toString();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;

    public HeartbeatClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build());
    }

    HeartbeatClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void sendHeartbeat(String apiKey, double intervalSeconds, String environment) {
        sleepWithJitter(intervalSeconds);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(AceniteConstants.resolveAceniteUrl() + "/heartbeat/"))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("X-Acenite-Environment", environment)
                    .POST(HttpRequest.BodyPublishers.ofString(payload()))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException | InterruptedException | RuntimeException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.log(Level.WARNING, "Acenite heartbeat failed", error);
        }
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

    private static String payload() {
        return "{"
                + "\"status\":\"up\","
                + "\"boot_id\":\"" + BOOT_ID + "\","
                + "\"instance_id\":\"default\""
                + "}";
    }
}
