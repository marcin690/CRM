package wh.plus.crm.model;

import lombok.Getter;

@Getter
public enum Tax {

    VAT_0("0%"),
    VAT_5("5%"),
    VAT_23("23%");

    private final String description;

    Tax(String description){
        this.description = description;
    }

}

