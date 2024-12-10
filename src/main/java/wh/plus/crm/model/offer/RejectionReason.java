package wh.plus.crm.model.offer;

import lombok.Getter;

@Getter
public enum RejectionReason {
    PRICE_TOO_HIGH("Za wysoka cena"),
    LONG_BIDDING_PROCESS("Zbyt długie ofertowanie"),
    POOR_REPUTATION("Zła opinia na rynku"),
    RIGGED_TENDER("Ustawiony przetarg"),
    LACK_OF_QUALIFICATIONS("Brak kwalifikacji"),
    UNREALISTIC_DEADLINE("Nierealny termin"),
    OTHER("Inne"),
    POOR_QUALITY_SAMPLE_ROOM("Źle wykonany pokój wzorcowy"),
    LOW_QUALITY_PREVIOUS_PROJECT("Słaba jakość u tego samego klienta na poprzedniej realizacji");


    private final String description;

    RejectionReason(String description) {
        this.description = description;
    }



}
