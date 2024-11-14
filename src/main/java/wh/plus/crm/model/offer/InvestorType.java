package wh.plus.crm.model.offer;

import lombok.Getter;

@Getter
public enum InvestorType {

    LARGE_PRIVATE_INVESTOR("Pryw. inwestor duży"),
    MULTI_PROPERTY_PRIVATE_INVESTOR("Pryw. inwestor wieloobiektowy"),
    CONSTRUCTION_COMPANY_HOTEL("Firma budowlana dla hotelu"),
    CONDO_HOTELS("Condohotele"),
    CONSTRUCTION_COMPANY_PRIVATE("Firma budowlana dla prywatnych"),
    FOREIGN_TOTAL("Zagranica w sumie"),
    SMALL_PRIVATE_INVESTOR("Pryw. inwestor mały"),
    PRIVATE_APARTMENTS("Apart. prywatne");

    private final String description;

    InvestorType(String description) {
        this.description = description;
    }
}
