package com.acenite.internal;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class AceniteConstants {
    private static final Logger LOGGER = Logger.getLogger(AceniteConstants.class.getName());

    public static final String ACENITE_URL = "https://ingest.acenite.com";
    public static final String ALLOW_ENDPOINT_OVERRIDE_ENV = "ACENITE_AGENT_ALLOW_ENDPOINT_OVERRIDE";
    public static final String INGEST_URL_ENV = "ACENITE_AGENT_INGEST_URL";
    public static final Set<String> ALLOWED_FRAMEWORKS = Set.of();
    public static final Set<String> ALLOWED_INSTRUMENTATIONS = Set.of();

    private AceniteConstants() {
    }

    public static String resolveAceniteUrl() {
        return resolveAceniteUrl(System.getenv());
    }

    static String resolveAceniteUrl(Map<String, String> environment) {
        String allowOverride = environment.get(ALLOW_ENDPOINT_OVERRIDE_ENV);
        if (allowOverride == null || !allowOverride.equalsIgnoreCase("true")) {
            return ACENITE_URL;
        }

        String candidate = environment.get(INGEST_URL_ENV);
        if (candidate == null || candidate.isEmpty() || !candidate.equals(candidate.trim())) {
            return ACENITE_URL;
        }

        try {
            URI parsed = URI.create(candidate);
            String scheme = parsed.getScheme();
            String hostname = parsed.getHost();
            if (scheme == null
                    || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || hostname == null
                    || parsed.getUserInfo() != null
                    || parsed.getQuery() != null
                    || parsed.getFragment() != null) {
                return ACENITE_URL;
            }

            String normalizedHost = hostname.toLowerCase(Locale.ROOT);
            if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]")) {
                normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
            }
            if (normalizedHost.equals("localhost") || isLoopbackAddress(normalizedHost)) {
                return candidate;
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid overrides always fall back to the production endpoint.
        }

        return ACENITE_URL;
    }

    public static void logLocalOverrideIfActive(boolean loggingEnabled) {
        if (!loggingEnabled) {
            return;
        }

        String resolvedUrl = resolveAceniteUrl();
        if (!resolvedUrl.equals(ACENITE_URL)) {
            LOGGER.info(() -> "Acenite development endpoint override active: telemetry is being sent to "
                    + resolvedUrl + " instead of production.");
        }
    }

    private static boolean isLoopbackAddress(String hostname) {
        if (hostname.equals("::1") || hostname.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }

        String[] octets = hostname.split("\\.", -1);
        if (octets.length != 4 || !octets[0].equals("127")) {
            return false;
        }

        for (String octet : octets) {
            try {
                int value = Integer.parseInt(octet);
                if (value < 0 || value > 255) {
                    return false;
                }
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return true;
    }
}

