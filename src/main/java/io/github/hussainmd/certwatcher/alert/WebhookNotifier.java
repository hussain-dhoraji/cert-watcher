package io.github.hussainmd.certwatcher.alert;

import java.net.URI;
import java.util.Map;

import io.github.hussainmd.certwatcher.check.CertificateStatus;
import io.github.hussainmd.certwatcher.check.Severity;
import io.github.hussainmd.certwatcher.config.WatcherProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Posts alerts as {@code {"text": "..."}}, the payload accepted by Slack and Teams incoming webhooks.
 * Without a configured webhook, alerts are only logged.
 */
@Component
public class WebhookNotifier implements AlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);

    private final RestClient restClient;
    private final URI webhookUrl;

    public WebhookNotifier(RestClient.Builder restClientBuilder, WatcherProperties properties) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = properties.webhookUrl();
    }

    @Override
    public void send(CertificateStatus status) {
        String message = format(status);
        if (webhookUrl == null || webhookUrl.toString().isBlank()) {
            log.warn("No webhook configured; alert: {}", message);
            return;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("Webhook delivery failed for {}: {}", status.target(), e.getMessage());
        }
    }

    static String format(CertificateStatus status) {
        if (status.severity() == Severity.ERROR) {
            return "[ERROR] %s: check failed (%s)".formatted(status.target(), status.error());
        }
        return "[%s] %s: certificate %s expires %s (%d days)".formatted(
                status.severity(), status.target(), status.subject(), status.notAfter(), status.daysUntilExpiry());
    }
}
