package io.github.hussainmd.certwatcher.check;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.hussainmd.certwatcher.config.WatcherProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CertificateCheckServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @TempDir
    Path tempDir;

    private final List<CertificateStatus> sentAlerts = new ArrayList<>();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    @Test
    void classifiesCertificatesByDaysUntilExpiry() throws Exception {
        String healthy = pem("healthy", Duration.ofDays(90));
        String warning = pem("warning", Duration.ofDays(20));
        String critical = pem("critical", Duration.ofDays(3));
        String expired = pem("expired", Duration.ofDays(-1));

        Map<String, Severity> severities = serviceFor(List.of(healthy, warning, critical, expired)).checkAll().stream()
                .collect(toMap(CertificateStatus::target, CertificateStatus::severity));

        assertThat(severities)
                .containsEntry(healthy, Severity.OK)
                .containsEntry(warning, Severity.WARNING)
                .containsEntry(critical, Severity.CRITICAL)
                .containsEntry(expired, Severity.EXPIRED);
    }

    @Test
    void reportsUnreadableFilesAsErrors() {
        String missing = tempDir.resolve("missing.pem").toString();

        CertificateStatus status = serviceFor(List.of(missing)).checkAll().get(0);

        assertThat(status.severity()).isEqualTo(Severity.ERROR);
        assertThat(status.error()).isNotBlank();
        assertThat(registry.get("certwatcher.check.failures").tag("target", missing).counter().count())
                .isEqualTo(1.0);
    }

    @Test
    void alertsOnlyWhenSeverityChanges() throws Exception {
        CertificateCheckService service = serviceFor(List.of(pem("warning", Duration.ofDays(20))));

        service.checkAll();
        service.checkAll();

        assertThat(sentAlerts).hasSize(1);
        assertThat(sentAlerts.get(0).severity()).isEqualTo(Severity.WARNING);
    }

    @Test
    void doesNotAlertForHealthyCertificates() throws Exception {
        serviceFor(List.of(pem("healthy", Duration.ofDays(90)))).checkAll();

        assertThat(sentAlerts).isEmpty();
    }

    @Test
    void publishesDaysUntilExpiryGauge() throws Exception {
        String healthy = pem("healthy", Duration.ofDays(90));

        serviceFor(List.of(healthy)).checkAll();

        assertThat(registry.get("certwatcher.days.until.expiry").tag("target", healthy).gauge().value())
                .isEqualTo(90.0);
    }

    private CertificateCheckService serviceFor(List<String> certificateFiles) {
        WatcherProperties properties = new WatcherProperties(
                List.of(), certificateFiles, Duration.ofHours(1), 30, 7, null);
        return new CertificateCheckService(
                properties,
                new EndpointCertificateFetcher(),
                new PemFileCertificateReader(),
                sentAlerts::add,
                new CertificateMetrics(registry),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private String pem(String name, Duration validFor) throws Exception {
        X509Certificate certificate = TestCertificates.selfSigned(name, NOW.minus(Duration.ofDays(365)), NOW.plus(validFor));
        return TestCertificates.writePem(tempDir, name + ".pem", certificate).toString();
    }
}
