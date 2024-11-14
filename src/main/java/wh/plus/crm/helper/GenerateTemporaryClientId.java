package wh.plus.crm.helper;
import java.security.SecureRandom;

public class GenerateTemporaryClientId {

    private static final SecureRandom random = new SecureRandom();
    public String generateTemporaryClientId(){

        StringBuilder clientId = new StringBuilder();
        for(int i = 0; i < 6; i++ ){
            clientId.append(random.nextInt(10));
        }
        return clientId.toString();
    }
}
