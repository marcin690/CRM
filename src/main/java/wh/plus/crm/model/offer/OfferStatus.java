package wh.plus.crm.model.offer;


import lombok.Getter;

@Getter
public enum OfferStatus {

    DRAFT("Szkic"),
    SENT("Wysłana"),
    ACCEPTED("Zaakceptowana"),
    REJECTED("Odrzucona"),
    SIGNED("Podpisana umowa");

    private final String description;

    OfferStatus(String description){
        this.description = description;
    }



}
