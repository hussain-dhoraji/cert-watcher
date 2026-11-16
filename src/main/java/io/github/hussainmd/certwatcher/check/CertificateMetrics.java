package io.github.hussainmd.certwatcher.check;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code certwatcher_days_until_expiry} per target and counts failed checks.
 */
@Component
public class CertificateMetrics {

    private final MeterRegistry registry;
    private final Map<String, AtomicLong> daysUntilExpiry = new ConcurrentHashMap<>();

    public CertificateMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void record(CertificateStatus status) {
        if (status.daysUntilExpiry() == null) {
            registry.counter("certwatcher.check.failures", "target", status.target()).increment();
            return;
        }
        daysUntilExpiry.computeIfAbsent(status.target(), this::registerGauge).set(status.daysUntilExpiry());
    }

    private AtomicLong registerGauge(String target) {
        AtomicLong value = new AtomicLong();
        Gauge.builder("certwatcher.days.until.expiry", value, AtomicLong::get)
                .description("Days until the certificate expires; negative once expired")
                .tag("target", target)
                .register(registry);
        return value;
    }
}
