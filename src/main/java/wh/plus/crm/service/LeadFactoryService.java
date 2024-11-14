package wh.plus.crm.service;

import com.github.javafaker.Faker;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wh.plus.crm.model.lead.Lead;
import wh.plus.crm.repository.LeadRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LeadFactoryService {

    private final LeadRepository leadRepository;
    private final Faker faker = new Faker(new Locale("pl"));

    @Transactional
    public void generateLeads(int numberOfLeads){
         List<Lead> leads = new ArrayList<>();

         for (int i = 0; i < numberOfLeads; i++){
             Lead lead = new Lead();
             lead.setName(faker.company().name());
             lead.setLeadValue(faker.number().numberBetween(100L, 100000L));
             lead.setRoomsQuantity(faker.number().numberBetween(1L, 10L));
             lead.setExecutionDate(LocalDateTime.now().plusDays(faker.number().numberBetween(1,30)));
             lead.setDescription(faker.lorem().sentence());
             lead.setClientBusinessName(faker.company().name());
             lead.setClientFullName(faker.name().fullName());
             lead.setClientAdress(faker.address().streetAddress());
             lead.setClientCity(faker.address().city());
             lead.setClientState(faker.address().state());
             lead.setClientZip(faker.address().zipCode());
             lead.setClientCountry(faker.address().country());
             lead.setClientEmail(faker.internet().emailAddress());
             lead.setClientPhone(faker.number().numberBetween(1000000000L, 9999999999L));
             lead.setVatNumber(faker.number().randomNumber(10, true));
             lead.setCreationDate(LocalDateTime.now().minusDays(faker.number().numberBetween(1,90)));

             leads.add(lead);
         }

         leadRepository.saveAll(leads);


    }



}
