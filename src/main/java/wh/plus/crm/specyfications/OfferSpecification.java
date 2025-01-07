package wh.plus.crm.specyfications;

import org.springframework.data.jpa.domain.Specification;
import wh.plus.crm.model.offer.*;

import java.time.LocalDateTime;

public class OfferSpecification {

    public static Specification<Offer> hasCreatedBy(String createdBy) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("createdBy"), createdBy);
    }

    public static Specification<Offer> hasName(String name) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("name"), "%" + name + "%");
    }

    public static Specification<Offer> hasClientType(ClientType clientType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("clientType"), clientType);
    }

    public static Specification<Offer> hasInvestorType(InvestorType investorType) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("investorType"), investorType);
    }

    public static  Specification<Offer> hasOfferStatus(OfferStatus offerStatus) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("offerStatus"), offerStatus);
    }

    public static Specification<Offer> hasObjectType(ObjectType objectType){
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("objectType"), objectType);
    }

    public static Specification<Offer> hasDescription(String description){
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(root.get("description"), "%" + description + "%");
    }

    public static Specification<Offer> hasSalesTeam(Long salesTeamId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("salesTeam").get("id"), salesTeamId);
    }

    public static Specification<Offer> hasUser(Long userId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Offer> hasClient(Long clientId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("client").get("id"), clientId);
    }

    public static Specification<Offer> hasLead(Long leadId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("lead").get("id"), leadId);
    }


    public static Specification<Offer> hasProject(Long projectId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Offer> hasSalesOpportunityLevel(SalesOpportunityLevel salesOpportunityLevel) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("salesOpportunityLevel"), salesOpportunityLevel);
    }

    public static Specification<Offer> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("creationDate"), startDate, endDate);

    }





}
