package wh.plus.crm.model.client.clientMarketingTest;

import lombok.Getter;

@Getter
public enum MarketingTestCategory {

    LOYALTY("Lojalność klienta wobec marki lub produktu"),
    ENGAGEMENT("Poziom zaangażowania klienta, np. częstotliwość interakcji z marką"),
    BRAND_AWARENESS("Świadomość marki – jak dobrze klienci rozpoznają markę lub produkt"),
    PRODUCT_INTEREST("Zainteresowanie klienta produktem, np. atrakcyjność produktów"),
    CUSTOMER_SATISFACTION("Ogólna satysfakcja klienta z produktów lub usług");

    private final String description;

    MarketingTestCategory(String description){
        this.description = description;
    }

}

