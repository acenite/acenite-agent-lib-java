package com.acenite.internal;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class OpenTelemetryBootstrap {
    private static final Logger LOGGER = Logger.getLogger(OpenTelemetryBootstrap.class.getName());

    private final OpenTelemetrySdk openTelemetry;
    private final SdkTracerProvider tracerProvider;

    private OpenTelemetryBootstrap(OpenTelemetrySdk openTelemetry, SdkTracerProvider tracerProvider) {
        this.openTelemetry = openTelemetry;
        this.tracerProvider = tracerProvider;
    }

    public static OpenTelemetryBootstrap start(String apiKey, String serviceName) {
        OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(AceniteConstants.ACENITE_URL + "/monitor/")
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();

        Resource resource = Resource.getDefault().merge(Resource.create(Attributes.of(
                AttributeKey.stringKey("service.name"),
                serviceName
        )));

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
                .build();

        OpenTelemetrySdk openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build();

        return new OpenTelemetryBootstrap(openTelemetry, tracerProvider);
    }

    public Tracer getTracer() {
        return openTelemetry.getTracer("acenite-agent");
    }

    public static Tracer noopTracer() {
        return OpenTelemetrySdk.builder().build().getTracer("acenite-agent");
    }

    public void shutdown() {
        try {
            tracerProvider.shutdown();
        } catch (RuntimeException error) {
            LOGGER.log(Level.WARNING, "Acenite OpenTelemetry shutdown failed", error);
        }
    }
}
