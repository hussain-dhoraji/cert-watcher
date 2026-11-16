package io.github.hussainmd.certwatcher.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import io.github.hussainmd.certwatcher.check.CertificateStatus;
import io.github.hussainmd.certwatcher.check.Severity;
import org.junit.jupiter.api.Test;

class WebhookNotifierTest {

    @Test
    void formatsExpiryAlerts() {
        CertificateStatus status = new CertificateStatus(
                "example.com:443", "CN=example.com", "CN=Test CA", Instant.parse("2026-01-21T00:00:00Z"), 20L,
                Severity.WARNING, null);

        assertThat(WebhookNotifier.format(status))
                .isEqualTo("[WARNING] example.com:443: certificate CN=example.com expires 2026-01-21T00:00:00Z (20 days)");
    }

    @Test
    void formatsFailedChecks() {
        CertificateStatus status = new CertificateStatus(
                "bad.example:443", null, null, null, null, Severity.ERROR, "Connection refused");

        assertThat(WebhookNotifier.format(status)).isEqualTo("[ERROR] bad.example:443: check failed (Connection refused)");
    }
}
