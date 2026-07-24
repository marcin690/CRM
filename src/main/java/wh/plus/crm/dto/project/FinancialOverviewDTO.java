package wh.plus.crm.dto.project;

import lombok.Data;

import java.math.BigDecimal;

/** Wiersz raportu finansowego projektów (odpowiednik arkusza K2026). */
@Data
public class FinancialOverviewDTO {
    private Long projectId;
    private String name;
    private String teamName;
    private String status;
    private Long contractValue;          // wartość kontraktu (netto)
    private BigDecimal declaredMargin;   // marża deklarowana %
    private BigDecimal plannedProfit;    // kwota zysku planowana = wartość × marża%
    private BigDecimal revenueNet;       // FW — przychód z Fakturowni
    private BigDecimal costNet;          // koszty z Fakturowni
    private BigDecimal profitNet;        // ZYSK realny = przychód − koszt
    private BigDecimal completionPercent;// %kont — realizacja vs wartość
    private BigDecimal toInvoice;        // DW — do wystawienia = wartość − przychód
}
