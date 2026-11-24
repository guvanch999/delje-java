package tm.delje.shop.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.HtmlUtils;
import tm.delje.shop.dto.ContactForm;

/**
 * Forwards messages from the contact form to the shop owner (ADMIN_EMAIL).
 */
@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private static final int MAX_MESSAGES_PER_WINDOW = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final ResendClient resendClient;
    private final Map<String, Deque<Instant>> recentByIp = new ConcurrentHashMap<>();

    public ContactService(ResendClient resendClient) {
        this.resendClient = resendClient;
    }

    /**
     * Simple protection against flooding the owner's inbox from one address.
     */
    public boolean isRateLimited(String ip) {
        Instant now = Instant.now();
        Instant border = now.minus(WINDOW);
        Deque<Instant> times = recentByIp.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst().isBefore(border)) {
                times.pollFirst();
            }
            if (times.size() >= MAX_MESSAGES_PER_WINDOW) {
                return true;
            }
            times.addLast(now);
        }
        if (recentByIp.size() > 10_000) {
            recentByIp.clear();
        }
        return false;
    }

    /**
     * @return true when the message was handed over to the mail service
     */
    public boolean send(ContactForm form, String lang) {
        if (!resendClient.isConfigured()) {
            log.error("contact form: RESEND_API_KEY / ADMIN_EMAIL / MAIL_FROM are not configured, message from {} not sent",
                    form.getEmail());
            return false;
        }

        String subject = "Сообщение с сайта DELJE от " + oneLine(form.getName());
        String html = "<p><strong>Имя:</strong> " + HtmlUtils.htmlEscape(form.getName()) + "</p>"
                + "<p><strong>Email:</strong> " + HtmlUtils.htmlEscape(form.getEmail()) + "</p>"
                + "<p><strong>Язык сайта:</strong> " + HtmlUtils.htmlEscape(lang) + "</p>"
                + "<p><strong>Сообщение:</strong></p>"
                + "<p>" + HtmlUtils.htmlEscape(form.getMessage()).replace("\n", "<br>") + "</p>";
        String text = "Имя: " + form.getName() + "\n"
                + "Email: " + form.getEmail() + "\n"
                + "Язык сайта: " + lang + "\n\n"
                + form.getMessage();

        try {
            resendClient.sendToAdmin(subject, html, text, form.getEmail());
            log.info("contact form message sent, from={}", form.getEmail());
            return true;
        } catch (RestClientException e) {
            log.error("contact form: sending through Resend failed", e);
            return false;
        }
    }

    private static String oneLine(String s) {
        return s.replaceAll("[\\r\\n]+", " ");
    }
}
