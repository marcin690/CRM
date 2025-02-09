package wh.plus.crm.service;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.notification.CycleType;
import wh.plus.crm.model.notification.Notification;
import wh.plus.crm.model.notification.NotificationTemplate;
import wh.plus.crm.model.notification.NotificationType;
import wh.plus.crm.model.offer.Offer;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.NotificationRepository;
import wh.plus.crm.repository.NotificationTemplateRepository;
import wh.plus.crm.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository notificationTemplateRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;


    public void createImmediateNotification(
            List<Long> recipientIds,
            NotificationType notificationType,
            boolean sendEmail,
            boolean sendSms,
            boolean cyclic,
            CycleType cycleType,
            String content,
            EntityType relatedEntityType,
            Long relatedEntityId
    ) {
        LocalDateTime now = LocalDateTime.now();

        List<User> recipients = resolveRecipients(recipientIds);

        for (User user :  recipients) {
            Notification notification = Notification.builder()
                    .user(user)
                    .content(content)
                    .sendEmail(sendEmail)
                    .sendSms(sendSms)
                    .notificationType(notificationType)
                    .cycleType(cyclic ? cycleType : null)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .creationDate(now)
                    .open(false)
                    .build();

            notificationRepository.save(notification);

            if(notification.getNotificationType() == NotificationType.IMMEDIATE) {
                if(sendEmail && user.getEmail() != null) {

                    Map<String, Object> variables = new HashMap<>();
                    variables.put("content", content);
                    emailService.sendEmail( user.getEmail(),
                            "Powiadomienie z systemu CRM",
                            "immediateNotification",  // nazwa szablonu (plik immediateNotification.html w resources/templates)
                            variables);


                }

                if(sendSms && user.getPhone() != null) {
                    smsService.sendSms(user.getPhone(), content);
                }
            }



        }

    }

    public void sendBatchNotifications() {
        LocalDateTime now = LocalDateTime.now();
        // Pobieramy wszystkie aktywne szablony powiadomień typu BATCH, które są cykliczne
        List<NotificationTemplate> templates = templateRepository.findByNotificationTypeAndCyclicAndCycleType(NotificationType.BATCH, true, CycleType.YEARLY);


        for (NotificationTemplate template : templates) {
            // Jeśli nie ustawiono nextNotificationTime, a startDate już minęła, domyślnie przypisujemy nextNotificationTime = startDate
            if (template.getNextNotificationTime() == null && template.getStartDate() != null && !template.getStartDate().isAfter(now)) {
                template.setNextNotificationTime(template.getStartDate());
            }
            // Jeżeli nextNotificationTime jeszcze nie nadeszła, pomijamy ten szablon
            if (template.getNextNotificationTime() != null && template.getNextNotificationTime().isAfter(now)) {
                continue;
            }

            // Parsujemy listę odbiorców
            List<Long> recipientIds = parseRecipientIds(template.getRecipientIds());

            for (Long userId : recipientIds) {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isEmpty()) {
                    continue;
                }
                User user = userOpt.get();

                // Tworzymy powiadomienie na podstawie szablonu
                Notification notification = Notification.builder()
                        .user(user)
                        .content(template.getTemplateContent())
                        .notificationType(NotificationType.BATCH)
                        .creationDate(now)
                        .open(false)
                        .sendEmail(template.getSendEmail())
                        .sendSms(template.getSendSms())
                        .build();
                notificationRepository.save(notification);

                // Wysyłamy e-mail
                if (Boolean.TRUE.equals(template.getSendEmail()) && user.getEmail() != null) {
                    Map<String, Object> variables = new HashMap<>();
                    List<Map<String, String>> items = new ArrayList<>();
                    Map<String, String> item = new HashMap<>();
                    item.put("title", template.getTemplateContent());
                    item.put("url", "#");
                    items.add(item);
                    variables.put("items", items);
                    variables.put("generationTime", now.toString());

                    emailService.sendEmail(
                            user.getEmail(),
                            "Powiadomienie zbiorcze z systemu CRM",
                            "notificationTemplate", // nazwa szablonu (odnosi się do pliku notificationTemplate.html)
                            variables
                    );
                }
                // Wysyłamy SMS
                if (Boolean.TRUE.equals(template.getSendSms()) && user.getPhone() != null) {
                    smsService.sendSms(user.getPhone(), template.getTemplateContent());
                }
            }

            // Aktualizacja nextNotificationTime po wysłaniu powiadomień
            if (template.getCycleType() == CycleType.YEARLY) {
                // Ustawiamy nextNotificationTime na kolejny rok, z godziną 8:00
                LocalDateTime currentNext = template.getNextNotificationTime();
                if (currentNext == null) {
                    currentNext = template.getStartDate();
                }
                LocalDateTime nextOccurrence = LocalDateTime.of(
                        currentNext.getYear() + 1,
                        currentNext.getMonth(),
                        currentNext.getDayOfMonth(),
                        8, 0, 0, 0
                );
                template.setNextNotificationTime(nextOccurrence);
                templateRepository.save(template);
            }
            // Możesz dodać analogiczną logikę dla innych cycleType (np. DAILY, MONTHLY) wg swoich potrzeb.
        }
    }

    private List<Long> parseRecipientIds(String recipientIds) {
        if (recipientIds == null || recipientIds.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(recipientIds.split(","))
                .map(String::trim)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }



    public void scheduleNotification(
            String name,
            String templateContent,
            CycleType cycleType,
            List<Long> recipientIds,
            boolean sendEmail,
            boolean sendSms,
            LocalDateTime notificationDate
    ) {
        // Pobierz listę użytkowników na podstawie recipientIds (lub aktualnego użytkownika)
        List<User> recipients = resolveRecipients(recipientIds);

        // Konwersja listy użytkowników na format CSV z ich ID
        String recipientIdsCsv = recipients.stream()
                .map(user -> String.valueOf(user.getId()))
                .collect(Collectors.joining(","));

        // Tworzymy nowy NotificationTemplate
        NotificationTemplate template = NotificationTemplate.builder()
                .name(name)
                .templateContent(templateContent)
                .notificationType(NotificationType.BATCH)
                .cyclic(true)
                .cycleType(cycleType)
                .recipientIds(recipientIdsCsv)
                .sendEmail(sendEmail)
                .sendSms(sendSms)
                .startDate(notificationDate)
                .build();

        notificationTemplateRepository.save(template);
    }



    public List<User> resolveRecipients(List<Long> recipientIds) {
        List<User> recipients = new ArrayList<>();

        if(recipientIds == null || recipientIds.isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth != null && auth.getPrincipal() instanceof User) {
                recipients.add((User) auth.getPrincipal());
            }
        } else {
            for (Long id : recipientIds) {
                Optional<User> userOpt = userRepository.findById(id);
                userOpt.ifPresent(recipients::add);
            }
        }

        return recipients;
    }

    public void markAsRead(List<Long> notificationIds) {
        List<Notification> notifications = notificationRepository.findAllById(notificationIds);
        notifications.forEach(notification -> notification.setOpen(true));
        notificationRepository.saveAll(notifications);
    }


}
