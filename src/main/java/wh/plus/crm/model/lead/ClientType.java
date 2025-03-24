package wh.plus.crm.model.lead;


import lombok.Getter;

@Getter
public enum ClientType {

    DESIGN_OFFICE("Biuro projektowe"),
    PRIVATE_CLIENT("Klient prywatny"),
    HOTEL_CHAIN("Hotel sieciowy"),
    CONSTRUCTION_COMPANY("Firma budowlana");

    private final String description;
     ClientType(String description) {
         this.description = description;
     }

}
