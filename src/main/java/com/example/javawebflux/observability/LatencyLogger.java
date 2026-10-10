package com.example.javawebflux.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.concurrent.atomic.AtomicReference;

public final class LatencyLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatencyLogger.class);

    private LatencyLogger() {
    }

    public static <T> Mono<T> measure(String operation, Mono<T> publisher) {
        return Mono.defer(() -> {
            long startedAt = System.nanoTime();
            return publisher.doFinally(signal -> log(operation, startedAt, signal));
        });
    }

    public static Mono<ResponseEntity<?>> measureHttpEndpoint(
            String endpoint,
            Mono<ResponseEntity<?>> publisher) {
        return Mono.defer(() -> {
            long startedAt = System.nanoTime();
            AtomicReference<String> status = new AtomicReference<>("UNKNOWN");
            return publisher
                    .doOnNext(response -> status.set(String.valueOf(response.getStatusCode().value())))
                    .doFinally(signal -> {
                        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
                        LOGGER.info("event=http_latency endpoint={} status={} outcome={} latencyMs={}",
                                endpoint, status.get(), signal.name(), latencyMs);
                    });
        });
    }

    public static void log(String operation, long startedAt, SignalType signal) {
        long latencyMs = (System.nanoTime() - startedAt) / 1_000_000;
        LOGGER.info("event=latency operation={} outcome={} latencyMs={}",
                operation, signal.name(), latencyMs);
    }
}