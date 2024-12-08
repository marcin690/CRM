package wh.plus.crm.model;

import lombok.Getter;

@Getter
public enum Tax {

    VAT_0(0, "5%"),
    VAT_5(0.5, "23%"),
    VAT_23(0.23, "23%");

    private final double rate;
    private final String displayName;

    Tax(double rate, String displayName) {
        this.rate = rate;
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

