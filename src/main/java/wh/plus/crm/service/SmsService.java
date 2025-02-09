package wh.plus.crm.service;

import org.springframework.stereotype.Service;

@Service
public class SmsService {

    public void sendSms(Long phoneNumber, String message) {
        System.out.println("Wysłano");
    }

}
