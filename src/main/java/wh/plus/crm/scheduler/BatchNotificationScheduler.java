package wh.plus.crm.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import wh.plus.crm.service.NotificationService;

@Component
public class BatchNotificationScheduler {

    @Autowired
    private NotificationService notificationService;

    /**
     * Metoda wywoływana codziennie o 8:00 rano.
     * Wyrażenie cron "0 0 8 * * ?" oznacza: 8:00:00 każdego dnia.
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void runBatchNotifications() {
        notificationService.sendBatchNotifications();
    }
}
