package wh.plus.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CrmApplicationTests {

    public static void main(String[] args) {
        SpringApplication.run(CrmApplication.class, args);
    }
}
