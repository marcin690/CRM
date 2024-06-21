package wh.plus.crm.model.contactInfo;

import lombok.Getter;

@Getter
public enum ContactInfoType {
    NEW_CLIENT("Klient nowy"),
    RETURNING_CLIENT("Klient powracający"),
    CURRENT_CLIENT("Klient obecnie obsługiwany");

    private final String description;

    ContactInfoType(String description) {
        this.description = description;
    }
}
