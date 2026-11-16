package io.github.hussainmd.certwatcher.check;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.stereotype.Component;

/**
 * Opens a TLS handshake to an endpoint and returns the leaf certificate it presents.
 */
@Component
public class EndpointCertificateFetcher {

    private static final int DEFAULT_PORT = 443;
    private static final int TIMEOUT_MILLIS = 10_000;

    public X509Certificate fetch(String endpoint) throws IOException, GeneralSecurityException {
        HostPort target = HostPort.parse(endpoint);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[] {new InspectOnlyTrustManager()}, null);

        try (SSLSocket socket = (SSLSocket) context.getSocketFactory().createSocket()) {
            if (!target.isIpAddress()) {
                SSLParameters parameters = socket.getSSLParameters();
                parameters.setServerNames(List.of(new SNIHostName(target.host())));
                socket.setSSLParameters(parameters);
            }
            socket.connect(new InetSocketAddress(target.host(), target.port()), TIMEOUT_MILLIS);
            socket.setSoTimeout(TIMEOUT_MILLIS);
            socket.startHandshake();
            Certificate[] chain = socket.getSession().getPeerCertificates();
            return (X509Certificate) chain[0];
        }
    }

    record HostPort(String host, int port) {

        static HostPort parse(String endpoint) {
            String value = endpoint.trim();
            int colon = value.lastIndexOf(':');
            if (colon < 0) {
                return new HostPort(value, DEFAULT_PORT);
            }
            return new HostPort(value.substring(0, colon), Integer.parseInt(value.substring(colon + 1)));
        }

        boolean isIpAddress() {
            return host.matches("[0-9.]+");
        }
    }

    /**
     * Accepts any chain so expired or self-signed certificates can still be reported.
     * Only the handshake runs over these connections; no application data is ever sent.
     */
    private static final class InspectOnlyTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
