package wh.plus.crm.service.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import wh.plus.crm.dto.notification.NotificationTriggerDTO;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.notification.NotificationTrigger;
import wh.plus.crm.model.notification.NotificationTriggerType;
import wh.plus.crm.model.notification.NotificationType;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.NotificationTriggerRepository;
import wh.plus.crm.repository.UserRepository;
import wh.plus.crm.service.EmailService;
import wh.plus.crm.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationTriggerServiceTest {

    @Mock private NotificationTriggerRepository triggerRepository;
    @Mock private NotificationService notificationService;
    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks private NotificationTriggerService service;

    private TriggerContext sampleCtx() {
        return new TriggerContext("in-app", "temat", "newLeadNotification",
                Map.of("k", "v"), EntityType.LEAD, 42L);
    }

    private NotificationTrigger trigger(boolean enabled, boolean email, boolean inApp, Set<Long> recipients) {
        return NotificationTrigger.builder()
                .triggerType(NotificationTriggerType.NEW_LEAD)
                .enabled(enabled).channelEmail(email).channelInApp(inApp)
                .recipientUserIds(recipients).build();
    }

    @Test
    void disabledTrigger_doesNothing() {
        // emailService jest wstrzykiwany polowo (@Autowired required=false)
        ReflectionTestUtils.setField(service, "emailService", emailService);
        when(triggerRepository.findByTriggerType(NotificationTriggerType.NEW_LEAD))
                .thenReturn(Optional.of(trigger(false, true, true, Set.of(1L))));

        service.fire(NotificationTriggerType.NEW_LEAD, sampleCtx());

        verifyNoInteractions(notificationService);
        verifyNoInteractions(emailService);
    }

    @Test
    void enabledTrigger_sendsInAppAndEmail() {
        ReflectionTestUtils.setField(service, "emailService", emailService);
        when(triggerRepository.findByTriggerType(NotificationTriggerType.NEW_LEAD))
                .thenReturn(Optional.of(trigger(true, true, true, Set.of(1L, 2L))));
        User u1 = new User(); u1.setId(1L); u1.setEmail("a@wh.pl");
        User u2 = new User(); u2.setId(2L); u2.setEmail("b@wh.pl");
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(u1, u2));

        service.fire(NotificationTriggerType.NEW_LEAD, sampleCtx());

        // in-app: dokładnie raz, IMMEDIATE, dla LEAD/42, bez e-mail/sms w NotificationService
        verify(notificationService).createImmediateNotification(
                argThat(ids -> ids.containsAll(List.of(1L, 2L))),
                eq(NotificationType.IMMEDIATE),
                eq(false), eq(false), eq(false), isNull(),
                eq("in-app"), eq(EntityType.LEAD), eq(42L));
        // e-mail: do każdego odbiorcy z adresem
        verify(emailService).sendEmail(eq("a@wh.pl"), eq("temat"), eq("newLeadNotification"), anyMap());
        verify(emailService).sendEmail(eq("b@wh.pl"), eq("temat"), eq("newLeadNotification"), anyMap());
    }

    @Test
    void emailChannel_withoutSmtp_skipsEmailButKeepsInApp() {
        // emailService == null (brak SMTP)
        when(triggerRepository.findByTriggerType(NotificationTriggerType.NEW_LEAD))
                .thenReturn(Optional.of(trigger(true, true, true, Set.of(1L))));

        service.fire(NotificationTriggerType.NEW_LEAD, sampleCtx());

        verify(notificationService).createImmediateNotification(
                anyList(), eq(NotificationType.IMMEDIATE), eq(false), eq(false),
                eq(false), isNull(), anyString(), eq(EntityType.LEAD), eq(42L));
        // brak beana e-mail => userRepository nie jest odpytywane o odbiorców maili
        verify(userRepository, never()).findAllById(anyIterable());
    }

    @Test
    void update_persistsSettings() {
        when(triggerRepository.findByTriggerType(NotificationTriggerType.NEW_LEAD))
                .thenReturn(Optional.of(trigger(false, false, false, Set.of())));
        when(triggerRepository.save(any(NotificationTrigger.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NotificationTriggerDTO dto = new NotificationTriggerDTO();
        dto.setEnabled(true);
        dto.setChannelEmail(true);
        dto.setChannelInApp(false);
        dto.setRecipientUserIds(Set.of(5L, 7L));

        NotificationTriggerDTO saved = service.update(NotificationTriggerType.NEW_LEAD, dto);

        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.isChannelEmail()).isTrue();
        assertThat(saved.isChannelInApp()).isFalse();
        assertThat(saved.getRecipientUserIds()).containsExactlyInAnyOrder(5L, 7L);
    }
}
