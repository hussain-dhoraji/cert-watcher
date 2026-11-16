package io.github.hussainmd.certwatcher.alert;

import io.github.hussainmd.certwatcher.check.CertificateStatus;

@FunctionalInterface
public interface AlertNotifier {

    void send(CertificateStatus status);
}
