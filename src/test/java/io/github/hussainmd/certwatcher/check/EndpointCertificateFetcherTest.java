package io.github.hussainmd.certwatcher.check;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.hussainmd.certwatcher.check.EndpointCertificateFetcher.HostPort;
import org.junit.jupiter.api.Test;

class EndpointCertificateFetcherTest {

    @Test
    void defaultsToPort443() {
        assertThat(HostPort.parse("example.com")).isEqualTo(new HostPort("example.com", 443));
    }

    @Test
    void parsesExplicitPort() {
        assertThat(HostPort.parse(" example.com:8443 ")).isEqualTo(new HostPort("example.com", 8443));
    }

    @Test
    void detectsIpAddressesSoSniIsSkipped() {
        assertThat(HostPort.parse("10.0.0.5:443").isIpAddress()).isTrue();
        assertThat(HostPort.parse("example.com").isIpAddress()).isFalse();
    }
}
