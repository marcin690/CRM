package wh.plus.crm.model.client.clientMarketingTest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import wh.plus.crm.model.client.Client;
import jakarta.persistence.Entity;

@Entity
public class TestQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question;

    @Enumerated(EnumType.STRING)
    private MarketingTestCategory marketingTestCategory;
}
