package io.github.hussainmd.certwatcher.check;

import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.github.hussainmd.certwatcher.alert.AlertNotifier;
import io.github.hussainmd.certwatcher.config.WatcherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Checks every configured target, keeps the latest results, and alerts when a target's severity changes.
 */
@Service
public class CertificateCheckService {

    private static final Logger log = LoggerFactory.getLogger(CertificateCheckService.class);

    private final WatcherProperties properties;
    private final EndpointCertificateFetcher endpointFetcher;
    private final PemFileCertificateReader fileReader;
    private final AlertNotifier notifier;
    private final CertificateMetrics metrics;
    private final Clock clock;
    private final Map<String, Severity> lastSeverity = new ConcurrentHashMap<>();
    private volatile List<CertificateStatus> latest = List.of();

    public CertificateCheckService(
            WatcherProperties properties,
            EndpointCertificateFetcher endpointFetcher,
            PemFileCertificateReader fileReader,
            AlertNotifier notifier,
            CertificateMetrics metrics,
            Clock clock) {
        this.properties = properties;
        this.endpointFetcher = endpointFetcher;
        this.fileReader = fileReader;
        this.notifier = notifier;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(initialDelayString = "PT5S", fixedDelayString = "${cert-watcher.check-interval:PT1H}")
    void scheduledCheck() {
        checkAll();
    }

    public synchronized List<CertificateStatus> checkAll() {
        List<CertificateStatus> results = new ArrayList<>();
        for (String endpoint : properties.endpoints()) {
            results.add(check(endpoint, () -> endpointFetcher.fetch(endpoint)));
        }
        for (String file : properties.certificateFiles()) {
            results.add(check(file, () -> fileReader.read(Path.of(file))));
        }
        results.forEach(metrics::record);
        results.forEach(this::alertOnChange);
        latest = List.copyOf(results);
        return latest;
    }

    public List<CertificateStatus> latest() {
        return latest;
    }

    private CertificateStatus check(String target, CertificateLoader loader) {
        try {
            return evaluate(target, loader.load());
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            log.warn("Certificate check failed for {}: {}", target, e.toString());
            return CertificateStatus.failed(target, Objects.requireNonNullElse(e.getMessage(), e.getClass().getSimpleName()));
        }
    }

    private CertificateStatus evaluate(String target, X509Certificate certificate) {
        Instant now = clock.instant();
        Instant notAfter = certificate.getNotAfter().toInstant();
        long days = Duration.between(now, notAfter).toDays();
        return new CertificateStatus(
                target,
                certificate.getSubjectX500Principal().getName(),
                certificate.getIssuerX500Principal().getName(),
                notAfter,
                days,
                severityFor(now, notAfter, days),
                null);
    }

    private Severity severityFor(Instant now, Instant notAfter, long days) {
        if (notAfter.isBefore(now)) {
            return Severity.EXPIRED;
        }
        if (days <= properties.criticalDays()) {
            return Severity.CRITICAL;
        }
        if (days <= properties.warningDays()) {
            return Severity.WARNING;
        }
        return Severity.OK;
    }

    private void alertOnChange(CertificateStatus status) {
        Severity previous = lastSeverity.put(status.target(), status.severity());
        if (status.severity() != Severity.OK && status.severity() != previous) {
            notifier.send(status);
        }
    }

    @FunctionalInterface
    private interface CertificateLoader {
        X509Certificate load() throws IOException, GeneralSecurityException;
    }
}
