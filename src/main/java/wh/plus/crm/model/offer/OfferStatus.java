package wh.plus.crm.model.offer;


import lombok.Getter;

@Getter
public enum OfferStatus {

    SENT("Wysłana"),
    ACCEPTED("Zaakceptowana"),
    REJECTED("Odrzucona");

    private final String description;

    OfferStatus(String description){
        this.description = description;
    }



}
