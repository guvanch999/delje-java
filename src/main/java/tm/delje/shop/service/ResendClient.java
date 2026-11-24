package tm.delje.shop.service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tm.delje.shop.config.AppProperties;

/**
 * Sends e-mails through the Resend HTTP API (https://resend.com/docs/api-reference/emails/send-email).
 */
@Component
public class ResendClient {

    private final AppProperties.Mail mail;
    private final RestClient restClient;

    public ResendClient(AppProperties properties, RestClient.Builder builder) {
        this.mail = properties.getMail();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return notBlank(mail.getResendApiKey()) && notBlank(mail.getAdminEmail()) && notBlank(mail.getFrom());
    }

    public void sendToAdmin(String subject, String html, String text, String replyTo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", mail.getFrom());
        body.put("to", List.of(mail.getAdminEmail()));
        body.put("subject", subject);
        body.put("html", html);
        body.put("text", text);
        if (notBlank(replyTo)) {
            body.put("reply_to", replyTo);
        }

        restClient.post()
                .uri(mail.getResendApiUrl())
                .header("Authorization", "Bearer " + mail.getResendApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
