package wh.plus.crm.dto.invoice;

import lombok.Data;

@Data
public class FakturowniaAccountDTO {
    private Long id;
    private String label;
    private String apiSubdomain;

    /**
     * Token jest WRITE-ONLY: serwer akceptuje go w request body (POST/PATCH),
     * ale nigdy nie zwraca z powrotem do klienta. Frontend wie tylko czy
     * jakikolwiek token istnieje przez flagę {@code hasToken}.
     */
    private String apiToken;

    private boolean hasToken;

    private boolean enabled;
}
