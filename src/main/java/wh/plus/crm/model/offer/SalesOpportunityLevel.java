package wh.plus.crm.model.offer;

import lombok.Getter;

@Getter
public enum SalesOpportunityLevel {

    LOW("Mały poziom szansy sprzedaży"),
    MODERATE("Umiarkowany poziom szansy sprzedaży"),
    HIGH("Duży poziom szansy sprzedaży");

    private final String description;

    // Konstruktor przypisujący opis do każdej wartości
    SalesOpportunityLevel(String description) {
        this.description = description;
    }

}
