package wh.plus.crm.model;

import lombok.Getter;

@Getter
public enum RejectionReason {
    PRICE_TOO_HIGH("Zbyt wysoka cena"),
    LONG_BIDDING_PROCESS("Zbyt długie ofertowanie"),
    POOR_REPUTATION("Zła opinia na rynku"),
    RIGGED_TENDER("Ustawiony przetarg"),
    LACK_OF_QUALIFICATIONS("Brak kwalifikacji"),
    UNREALISTIC_DEADLINE("Nierealny termin"),
    OTHER("Inne"),
    POOR_QUALITY_SAMPLE_ROOM("Źle wykonany pokój wzorcowy"),
    LOW_QUALITY_PREVIOUS_PROJECT("Słaba jakość u tego samego klienta na poprzedniej realizacji"),
    NON_COMPLIANCE_SPECIFICATIONS("Niespełnienie wymagań technicznych/projektowych"),
    UNFAVORABLE_PAYMENT_TERMS("Niekorzystne warunki płatności"),
    LOGISTICAL_DIFFICULTIES("Trudności logistyczne"),
    CAPACITY_LIMITS("Przekroczone moce produkcyjne"),
    NON_STANDARD_MATERIALS("Niestandardowe materiały lub wykończenia"),
    COMPLEX_PROCUREMENT("Zbyt skomplikowany proces zakupowy"),
    LACK_OF_LOGISTICAL_REQUIREMENTS("Brak minimalnych wymagań logistycznych"),
    LACK_OF_HUMAN_RESOURCES("Brak zasobów ludzkich"),
    UNCLEAR_CLIENT_REQUIREMENTS("Niejasne wymagania klienta"),
    LACK_OF_RELEVANT_EXPERIENCE("Brak doświadczenia w podobnych projektach"),
    CLIENT_REQUIREMENT_CHANGES("Zmienność wymagań klienta"),
    FINANCIAL_RELIABILITY_ISSUES("Problemy z wiarygodnością finansową klienta"),
    LACK_OF_SUPPLIER_TRUST("Brak zaufania do dostawców"),
    UNSUITABLE_CONTRACT_TERMS("Nieodpowiedni projekt umowy"),
    OVERLY_COMPLEX_ORDER("Zbyt duża różnorodność zamówienia"),
    MISSING_CERTIFICATIONS("Problemy z certyfikacjami"),
    NON_COMPLIANCE_ENVIRONMENTAL_POLICY("Niezgodność z polityką środowiskową klienta"),
    LEGAL_RISKS("Ryzyko prawne"),
    NO_CONTACT("Brak kontaktu");


    private final String description;

    RejectionReason(String description) {
        this.description = description;
    }



}
