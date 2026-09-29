package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.offer.Offer;

import java.time.LocalDateTime;
import java.util.List;


public interface OfferRepository extends JpaRepository<Offer, Long>, JpaSpecificationExecutor<Offer> {

    Page<Offer> findAll(Pageable pageable);

    Page<Offer> findByClientId(Long clientId, Pageable pageable);

    /**
     * Oferty kandydatów do przypięcia do konkretnego projektu:
     * - tego samego klienta co projekt
     * - jeszcze nieprzypiętych do żadnego projektu LUB przypiętych do innego niż ten
     * Sortowanie: nieprzypięte najpierw (project IS NULL).
     */
    @Query("SELECT o FROM Offer o WHERE o.client.id = :clientId " +
            "AND (o.project IS NULL OR o.project.id <> :projectId) " +
            "ORDER BY CASE WHEN o.project IS NULL THEN 0 ELSE 1 END, o.creationDate DESC")
    List<Offer> findPinnableForProject(@Param("projectId") Long projectId, @Param("clientId") Long clientId);

    @Query("SELECT o.salesTeam.id, o.salesTeam.name, COUNT(o), " +
            "SUM(CASE WHEN o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.totalPrice), 0), " +
            "COALESCE(SUM(CASE WHEN o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until THEN o.totalPrice ELSE 0 END), 0), " +
            "COALESCE(AVG(o.totalPrice), 0) " +
            "FROM Offer o " +
            "WHERE o.creationDate >= :since AND o.creationDate <= :until " +
            "AND o.salesTeam IS NOT NULL " +
            "GROUP BY o.salesTeam.id, o.salesTeam.name")
    List<Object[]> getStatisticsByTeam(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    @Query("SELECT COUNT(o), " +
            "SUM(CASE WHEN o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.totalPrice), 0), " +
            "COALESCE(SUM(CASE WHEN o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until THEN o.totalPrice ELSE 0 END), 0), " +
            "COALESCE(AVG(o.totalPrice), 0) " +
            "FROM Offer o " +
            "WHERE o.creationDate >= :since AND o.creationDate <= :until")
    List<Object[]> getOverallStatistics(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until);

    // ==================== Analityka sprzedaży (cockpit) ====================
    // teamId == null → brak filtra (ADMIN). W przeciwnym razie tylko oferty danego zespołu (Offer.salesTeam).
    // LEFT JOIN o.salesTeam, aby przy braku filtra admin widział też oferty bez zespołu.

    /** [0]=liczba ofert, [1]=liczba zaakceptowanych(ACCEPTED lub SIGNED), [2]=liczba podpisanych(SIGNED w okresie), [3]=suma wartości podpisanych */
    @Query("SELECT COUNT(o), " +
            "SUM(CASE WHEN (o.offerStatus = 'ACCEPTED' OR o.offerStatus = 'SIGNED') THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(CASE WHEN o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until THEN o.totalPrice ELSE 0 END), 0) " +
            "FROM Offer o LEFT JOIN o.salesTeam st WHERE o.creationDate >= :since AND o.creationDate <= :until AND (:teamId IS NULL OR st.id = :teamId)")
    List<Object[]> analyticsOfferTotals(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** nazwa źródła leada, liczba podpisanych umów */
    @Query("SELECT o.lead.leadSource.name, COUNT(o) FROM Offer o LEFT JOIN o.salesTeam st " +
            "WHERE o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until AND o.lead IS NOT NULL " +
            "AND (:teamId IS NULL OR st.id = :teamId) " +
            "GROUP BY o.lead.leadSource.name")
    List<Object[]> analyticsSignedBySource(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** userId handlowca oferty, liczba podpisanych umów */
    @Query("SELECT o.user.id, COUNT(o) FROM Offer o LEFT JOIN o.salesTeam st " +
            "WHERE o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until AND o.user IS NOT NULL " +
            "AND (:teamId IS NULL OR st.id = :teamId) " +
            "GROUP BY o.user.id")
    List<Object[]> analyticsSignedByRep(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** rok, miesiac, liczba ofert (wg daty utworzenia) */
    @Query("SELECT YEAR(o.creationDate), MONTH(o.creationDate), COUNT(o) FROM Offer o LEFT JOIN o.salesTeam st " +
            "WHERE o.creationDate >= :since AND o.creationDate <= :until AND (:teamId IS NULL OR st.id = :teamId) " +
            "GROUP BY YEAR(o.creationDate), MONTH(o.creationDate)")
    List<Object[]> analyticsOffersByMonth(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** śr. liczba dni od utworzenia leada do utworzenia oferty (ile zajmuje przejście lead->oferta) */
    @Query("SELECT AVG(FUNCTION('DATEDIFF', o.creationDate, o.lead.creationDate)) FROM Offer o LEFT JOIN o.salesTeam st " +
            "WHERE o.lead IS NOT NULL AND o.creationDate >= :since AND o.creationDate <= :until AND (:teamId IS NULL OR st.id = :teamId)")
    Double analyticsAvgLeadToOfferDays(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** śr. liczba dni od utworzenia oferty do podpisania umowy (ile trwa ofertowanie) */
    @Query("SELECT AVG(FUNCTION('DATEDIFF', o.signedContractDate, o.creationDate)) FROM Offer o LEFT JOIN o.salesTeam st " +
            "WHERE o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until AND (:teamId IS NULL OR st.id = :teamId)")
    Double analyticsAvgOfferToSignDays(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** rok, miesiac, liczba podpisanych umów (wg daty podpisu) */
    @Query("SELECT YEAR(o.signedContractDate), MONTH(o.signedContractDate), COUNT(o) FROM Offer o LEFT JOIN o.salesTeam st " +
            "WHERE o.offerStatus = 'SIGNED' AND o.signedContractDate >= :since AND o.signedContractDate <= :until AND (:teamId IS NULL OR st.id = :teamId) " +
            "GROUP BY YEAR(o.signedContractDate), MONTH(o.signedContractDate)")
    List<Object[]> analyticsSignedByMonth(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

}
