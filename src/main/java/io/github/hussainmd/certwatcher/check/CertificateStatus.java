package io.github.hussainmd.certwatcher.check;

import java.time.Instant;

/**
 * Result of checking one target. Certificate fields are null when {@code severity} is ERROR.
 */
public record CertificateStatus(
        String target,
        String subject,
        String issuer,
        Instant notAfter,
        Long daysUntilExpiry,
        Severity severity,
        String error) {

    static CertificateStatus failed(String target, String error) {
        return new CertificateStatus(target, null, null, null, null, Severity.ERROR, error);
    }
}
