package wh.plus.crm.specyfications;

import org.springframework.data.jpa.domain.Specification;
import wh.plus.crm.model.client.Client;

public class ClientSpecification {

    public static Specification<Client> hasClientType(String clientType) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("clientType"), clientType);
    }

    public static Specification<Client> hasClientFullName(String fullName){
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("clientFullName")),
                        "%" + fullName.toLowerCase() + "%"
                );
    }

    // Poprawiona metoda, aby wyszukiwać częściowo po polu clientBusinessName
    public static Specification<Client> hasClientBusinessName(String businessName) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("clientBusinessName")),
                        "%" + businessName.toLowerCase() + "%"
                );
    }

    // Metoda sprawdzająca czy imię i nazwisko LUB nazwa firmy zawiera dany fragment
    public static Specification<Client> hasNameOrBusinnesLike(String text) {
        return hasClientFullName(text)
                .or(hasClientBusinessName(text));
    }

    public static Specification<Client> hasClientEmail(String email) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("clientEmail")),
                        "%" + email.toLowerCase() + "%"
                );
    }

    public static Specification<Client> hasClientPhone(Long phone) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("clientPhone"), phone);
    }

    public static Specification<Client> hasVatNumber(Long vatNumber) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("vatNumber"), vatNumber);
    }

}
