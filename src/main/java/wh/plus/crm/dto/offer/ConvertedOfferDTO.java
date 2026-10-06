package wh.plus.crm.dto.offer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Pojedyncza oferta wchodząca w skład konwersji zespołu (status SIGNED w okresie). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertedOfferDTO {

    private Long id;
    private String name;
    private String clientName;
    private String salespersonName;
    private BigDecimal totalPrice;
    private LocalDateTime signedContractDate;
}
