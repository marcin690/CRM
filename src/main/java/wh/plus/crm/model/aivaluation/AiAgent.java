package wh.plus.crm.model.aivaluation;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rejestr agentów AI dostępnych w module Narzędzia → Wyceny AI.
 * Klucz API i base-url NIE są tu trzymane — pochodzą z konfiguracji (env),
 * a agent wiązany jest z kluczem po polu {@code code}.
 * Dzięki temu dołożenie kolejnego agenta = jeden wpis + jeden klucz w env.
 */
@Entity
@Table(name = "ai_agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiAgent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kod techniczny, np. "meble-wspolne" — łączy agenta z kluczem API w konfiguracji. */
    @Column(nullable = false, unique = true)
    private String code;

    /** Nazwa wyświetlana w UI. */
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    private boolean active = true;
}
