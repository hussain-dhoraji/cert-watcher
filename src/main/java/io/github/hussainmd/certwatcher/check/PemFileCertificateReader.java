package io.github.hussainmd.certwatcher.check;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.springframework.stereotype.Component;

/**
 * Reads the first (leaf) certificate from a PEM file.
 */
@Component
public class PemFileCertificateReader {

    public X509Certificate read(Path path) throws IOException, CertificateException {
        try (InputStream in = Files.newInputStream(path)) {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
        }
    }
}
