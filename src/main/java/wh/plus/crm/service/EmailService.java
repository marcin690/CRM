package wh.plus.crm.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import wh.plus.crm.helper.EmailContentBuilder;

import java.util.Map;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private EmailContentBuilder emailContentBuilder;

    @Value("${spring.mail.from}")
    private String fromEmail;

    /**
     * Metoda wysyła e-mail na podany adres (to) z określonym tematem i treścią.
     *
     * @param to      adres odbiorcy
     * @param subject temat wiadomości; jeśli null, zostanie użyty domyślny temat "Powiadomienie z systemu CRM"
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

        } catch (MessagingException e) {
            e.printStackTrace();
            // Opcjonalnie: logowanie lub rzucenie custom exception
        }


    }
}
