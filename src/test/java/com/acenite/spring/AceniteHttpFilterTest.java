package com.acenite.spring;

import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AceniteHttpFilterTest {
    @Test
    void emitsServerSpanWithIngestionCompatibleHttpAttributes() throws Exception {
        List<SpanData> exported = new ArrayList<>();
        SpanExporter exporter = new SpanExporter() {
            @Override public CompletableResultCode export(Collection<SpanData> spans) {
                exported.addAll(spans);
                return CompletableResultCode.ofSuccess();
            }
            @Override public CompletableResultCode flush() { return CompletableResultCode.ofSuccess(); }
            @Override public CompletableResultCode shutdown() { return CompletableResultCode.ofSuccess(); }
        };
        SdkTracerProvider provider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        AceniteHttpFilter filter = new AceniteHttpFilter(() -> provider.get("test"));

        HttpServletRequest request = proxy(HttpServletRequest.class, (method) -> switch (method) {
            case "getMethod" -> "GET";
            case "getRequestURI" -> "/orders/42";
            case "getAttribute" -> "/orders/{id}";
            default -> null;
        });
        HttpServletResponse response = proxy(HttpServletResponse.class,
                (method) -> method.equals("getStatus") ? 200 : null);

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(Context.current()).isNotNull();
        });
        provider.forceFlush().join(5, TimeUnit.SECONDS);

        assertThat(exported).hasSize(1);
        SpanData span = exported.get(0);
        assertThat(span.getKind().name()).isEqualTo("SERVER");
        assertThat(span.getAttributes().asMap()).containsEntry(
                io.opentelemetry.api.common.AttributeKey.stringKey("http.request.method"), "GET");
        assertThat(span.getAttributes().asMap()).containsEntry(
                io.opentelemetry.api.common.AttributeKey.stringKey("http.route"), "/orders/{id}");
        provider.close();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, Value value) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (_proxy, method, _args) -> {
                    Object result = value.forMethod(method.getName());
                    if (result != null || !method.getReturnType().isPrimitive()) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                });
    }

    private interface Value { Object forMethod(String method); }
}
