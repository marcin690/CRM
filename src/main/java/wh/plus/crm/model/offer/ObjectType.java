package wh.plus.crm.model.offer;

import lombok.Getter;

@Getter
public enum ObjectType {

    LARGE_HOTELS("Hotele Duże"),
    CHAIN_HOTELS("Hotele Sieciowe"),
    CONDO_HOTELS("Condohotele"),
    APARTMENTS("Apartamenty"),
    INDEPENDENT_HOTELS("Hotele niezależne"),
    STORE("Sklep");

    private final String description;

    ObjectType(String description){
        this.description = description;
    }

}
