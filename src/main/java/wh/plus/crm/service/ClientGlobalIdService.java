package wh.plus.crm.service;

import org.springframework.stereotype.Service;
import wh.plus.crm.helper.GenerateTemporaryClientId;

@Service
public class ClientGlobalIdService {

    public String generateClientGlobalId() {
        return new GenerateTemporaryClientId().generateTemporaryClientId();
    }

}
