package wh.plus.crm.model.client.clientMarketingTest;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import wh.plus.crm.model.client.Client;

@Entity
@Getter
@Setter
public class MarketingTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question, answer;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

}
