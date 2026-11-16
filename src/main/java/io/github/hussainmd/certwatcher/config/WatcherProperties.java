package io.github.hussainmd.certwatcher.config;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings bound from the {@code cert-watcher.*} properties.
 *
 * @param endpoints        TLS endpoints to check, as {@code host} or {@code host:port} (port defaults to 443)
 * @param certificateFiles PEM certificate files to check, e.g. Kubernetes TLS Secrets mounted as volumes
 * @param checkInterval    delay between check runs
 * @param warningDays      certificates expiring within this many days are reported as WARNING
 * @param criticalDays     certificates expiring within this many days are reported as CRITICAL
 * @param webhookUrl       optional Slack/Teams-compatible webhook that receives alerts
 */
@ConfigurationProperties(prefix = "cert-watcher")
public record WatcherProperties(
        @DefaultValue List<String> endpoints,
        @DefaultValue List<String> certificateFiles,
        @DefaultValue("PT1H") Duration checkInterval,
        @DefaultValue("30") int warningDays,
        @DefaultValue("7") int criticalDays,
        URI webhookUrl) {

    public WatcherProperties {
        endpoints = List.copyOf(endpoints);
        certificateFiles = List.copyOf(certificateFiles);
        if (criticalDays > warningDays) {
            throw new IllegalArgumentException(
                    "cert-watcher.critical-days must not be greater than cert-watcher.warning-days");
        }
    }
}
