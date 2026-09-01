package wh.plus.crm.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import wh.plus.crm.helper.EmailContentBuilder;

import java.util.Map;

/**
 * Wysyłka e-mail (HTML, przez szablony Thymeleaf).
 *
 * Bean powstaje TYLKO gdy skonfigurowany jest serwer SMTP (spring.mail.host).
 * Bez tego aplikacja wstaje normalnie, a kod korzystający z e-maila wstrzykuje
 * ten serwis jako opcjonalny ({@code @Autowired(required = false)}) i pomija wysyłkę.
 */
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailContentBuilder emailContentBuilder;

    @Value("${spring.mail.from:powiadomienia@whplus.com.pl}")
    private String fromEmail;

    /**
     * Wysyła wiadomość HTML zbudowaną z szablonu Thymeleaf.
     *
     * @param to           adres odbiorcy
     * @param subject      temat; null => "Powiadomienie z systemu CRM"
     * @param templateName nazwa szablonu (plik templates/{templateName}.html)
     * @param variables    zmienne przekazywane do szablonu
     */
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        String htmlContent = emailContentBuilder.build(templateName, variables);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject != null ? subject : "Powiadomienie z systemu CRM");
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("E-mail wysłany do {} (szablon '{}')", to, templateName);
        } catch (MessagingException | RuntimeException e) {
            // Nie propagujemy — wysyłka powiadomienia nie może wywrócić operacji biznesowej.
            log.error("Nie udało się wysłać e-maila do {} (szablon '{}'): {}", to, templateName, e.getMessage());
        }
    }
}
