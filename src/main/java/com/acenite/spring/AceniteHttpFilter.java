package com.acenite.spring;

import com.acenite.AceniteAgent;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.function.Supplier;

final class AceniteHttpFilter extends OncePerRequestFilter {
    private static final AttributeKey<String> METHOD = AttributeKey.stringKey("http.request.method");
    private static final AttributeKey<String> ROUTE = AttributeKey.stringKey("http.route");
    private static final AttributeKey<Long> STATUS = AttributeKey.longKey("http.response.status_code");
    private static final AttributeKey<Long> DURATION = AttributeKey.longKey("http.request.duration_ms");
    private final Supplier<Tracer> tracerSupplier;

    AceniteHttpFilter() {
        this(AceniteAgent::getTracer);
    }

    AceniteHttpFilter(Supplier<Tracer> tracerSupplier) {
        this.tracerSupplier = tracerSupplier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Span span = tracerSupplier.get()
                .spanBuilder(request.getMethod() + " " + request.getRequestURI())
                .setSpanKind(SpanKind.SERVER)
                .startSpan();
        long started = System.nanoTime();
        try (Scope ignored = span.makeCurrent()) {
            span.setAttribute(METHOD, request.getMethod());
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException error) {
            span.recordException(error);
            span.setStatus(StatusCode.ERROR);
            throw error;
        } finally {
            Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            span.setAttribute(ROUTE, route == null ? request.getRequestURI() : route.toString());
            span.setAttribute(STATUS, response.getStatus());
            span.setAttribute(DURATION, (System.nanoTime() - started) / 1_000_000);
            if (response.getStatus() >= 500) span.setStatus(StatusCode.ERROR);
            span.end();
        }
    }
}
