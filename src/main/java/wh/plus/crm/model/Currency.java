package wh.plus.crm.model;

import lombok.*;


@AllArgsConstructor
@Getter
@ToString
public enum Currency {
    PLN("Złotówka"," zł"),
    EUR("Euro", "€");

    private final String name;
    private final String symbol;



}
