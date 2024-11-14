package wh.plus.crm.model.offer;

import lombok.Getter;

@Getter
public enum ClientType {

    NEW_CLIENT("Klient nowy"),
    CURRENT_CLIENT("Klient obecnie obsługiwany"),
    RETURNING_CLIENT("Klient powracający");

    private final String description;

    ClientType(String description) {
        this.description = description;
    }


}
