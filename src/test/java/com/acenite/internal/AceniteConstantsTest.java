package com.acenite.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AceniteConstantsTest {
    @Test
    void noVariablesUseProduction() {
        assertEquals(AceniteConstants.ACENITE_URL, AceniteConstants.resolveAceniteUrl(Map.of()));
    }

    @Test
    void urlAloneUsesProduction() {
        assertEquals(AceniteConstants.ACENITE_URL, AceniteConstants.resolveAceniteUrl(Map.of(
                AceniteConstants.INGEST_URL_ENV, "http://127.0.0.1:5001"
        )));
    }

    @Test
    void allowFlagAloneUsesProduction() {
        assertEquals(AceniteConstants.ACENITE_URL, AceniteConstants.resolveAceniteUrl(Map.of(
                AceniteConstants.ALLOW_ENDPOINT_OVERRIDE_ENV, "TrUe"
        )));
    }

    @Test
    void loopbackIpv4OverrideIsUsed() {
        assertEquals("http://127.0.0.1:5001", resolve("http://127.0.0.1:5001"));
    }

    @Test
    void localhostOverrideIsUsed() {
        assertEquals("http://localhost:5001", resolve("http://localhost:5001"));
    }

    @Test
    void loopbackIpv6OverrideIsUsed() {
        assertEquals("http://[::1]:5001", resolve("http://[::1]:5001"));
    }

    @Test
    void remoteHttpAndHttpsUrlsUseProduction() {
        assertEquals(AceniteConstants.ACENITE_URL, resolve("http://example.com:5001"));
        assertEquals(AceniteConstants.ACENITE_URL, resolve("https://example.com"));
    }

    private static String resolve(String url) {
        return AceniteConstants.resolveAceniteUrl(Map.of(
                AceniteConstants.ALLOW_ENDPOINT_OVERRIDE_ENV, "true",
                AceniteConstants.INGEST_URL_ENV, url
        ));
    }
}
