package wh.plus.crm.service;

import com.github.javafaker.Faker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.user.User;
import wh.plus.crm.model.offer.*;
import wh.plus.crm.repository.OfferRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class OfferFactoryService {

    private final OfferRepository offerRepository;
    private final Faker faker = new Faker(new Locale("pl"));

    public void generateOffers(int numberOfOffers) {
        List<Offer> offers = new ArrayList<>();

        for (int i = 0; i < numberOfOffers; i++) {
            Offer offer = new Offer();
            offer.setName(faker.company().name());
            offer.setDescription(faker.lorem().sentence());
            offer.setTotalPrice(BigDecimal.valueOf(faker.number().randomDouble(2, 1000, 100000)));
            offer.setRejectionOrApprovalDate(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30)));
            offer.setSignedContractDate(LocalDateTime.now().minusDays(faker.number().numberBetween(1, 30)));
            offer.setClientType(ClientType.values()[faker.number().numberBetween(0, ClientType.values().length)]);
            offer.setInvestorType(InvestorType.values()[faker.number().numberBetween(0, InvestorType.values().length)]);
            offer.setOfferStatus(OfferStatus.values()[faker.number().numberBetween(0, OfferStatus.values().length)]);
            offer.setObjectType(ObjectType.values()[faker.number().numberBetween(0, ObjectType.values().length)]);
            offer.setSalesOpportunityLevel(SalesOpportunityLevel.values()[faker.number().numberBetween(0, SalesOpportunityLevel.values().length)]);
            offer.setArchived(faker.bool().bool());
            offer.setContractSigned(faker.bool().bool());

            // Set random user ID between 1 and 5
            User user = new User();
            user.setId((long) faker.number().numberBetween(1, 6));
            offer.setUser(user);

            offers.add(offer);
        }

        offerRepository.saveAll(offers);
    }
}