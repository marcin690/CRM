package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.lead.Lead;

import java.time.LocalDateTime;
import java.util.List;


public interface LeadRepository extends JpaRepository<Lead, Long> {

    Page<Lead> findAll(Pageable pageable);

    @Query("""
       SELECT l 
       FROM Lead l 
         LEFT JOIN l.user u
       WHERE (:fromDate IS NULL OR l.creationDate >= :fromDate)
         AND (:toDate IS NULL OR l.creationDate < :toDate)
         AND (:employee IS NULL OR u.fullname = :employee)
         AND (:status IS NULL OR l.leadStatus.id = :status)
         AND (
             (:search IS NULL OR :search = '') 
             OR lower(l.description) like lower(concat('%', :search, '%')) 
             OR lower(l.name) like lower(concat('%', :search, '%')) 
             OR lower(l.clientFullName) like lower(concat('%', :search, '%')) 
             OR lower(l.clientBusinessName) like lower(concat('%', :search, '%')) 
             OR lower(l.clientEmail) like lower(concat('%', :search, '%')) 
             OR cast(l.vatNumber as string) = :search 
             OR cast(l.clientPhone as string) = :search
         )
       """)
    Page<Lead> findLeadsByCriteria(
            Pageable pageable,
            @Param("fromDate") LocalDateTime from,
            @Param("toDate") LocalDateTime to,
            @Param("employee") String employee,
            @Param("status") Long status,
            @Param("search") String search
    );

    // ==================== Analityka sprzedaży (cockpit) ====================
    // teamId == null → brak filtra (ADMIN, widzi wszystko). W przeciwnym razie tylko dany zespół
    // (przez Lead.user.team). LEFT JOIN, aby przy braku filtra admin widział też leady bez usera/zespołu.

    /** [0]=liczba leadów, [1]=suma leadValue */
    @Query("SELECT COUNT(l), COALESCE(SUM(l.leadValue),0) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND (:teamId IS NULL OR t.id = :teamId)")
    List<Object[]> analyticsLeadTotals(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** źródło, liczba leadów, suma leadValue (tylko leady z przypisanym źródłem) */
    @Query("SELECT l.leadSource.name, COUNT(l), COALESCE(SUM(l.leadValue),0) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY l.leadSource.name")
    List<Object[]> analyticsLeadsBySource(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** userId, fullname, liczba leadów, suma leadValue */
    @Query("SELECT u.id, u.fullname, COUNT(l), COALESCE(SUM(l.leadValue),0) FROM Lead l JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY u.id, u.fullname")
    List<Object[]> analyticsLeadsByRep(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** userId, nazwa źródła, liczba leadów (do wyznaczenia dominującego źródła per handlowiec) */
    @Query("SELECT u.id, l.leadSource.name, COUNT(l) FROM Lead l JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY u.id, l.leadSource.name")
    List<Object[]> analyticsRepSourceCounts(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** userId, rejectionReason, liczba (do wyznaczenia dominującego powodu straty per handlowiec) */
    @Query("SELECT u.id, l.rejectionReason, COUNT(l) FROM Lead l JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND l.rejectionReason IS NOT NULL AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY u.id, l.rejectionReason")
    List<Object[]> analyticsRepReasonCounts(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** returningClient (true/false/null), liczba */
    @Query("SELECT l.returningClient, COUNT(l) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY l.returningClient")
    List<Object[]> analyticsClientsNewVsReturning(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** clientType (branża), liczba, średnia leadValue */
    @Query("SELECT l.clientType, COUNT(l), COALESCE(AVG(l.leadValue),0) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND l.clientType IS NOT NULL AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY l.clientType")
    List<Object[]> analyticsIndustry(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** rok, miesiac, liczba leadów */
    @Query("SELECT YEAR(l.creationDate), MONTH(l.creationDate), COUNT(l) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY YEAR(l.creationDate), MONTH(l.creationDate)")
    List<Object[]> analyticsLeadsByMonth(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** rejectionReason, liczba (globalnie — do wyboru top powodów) */
    @Query("SELECT l.rejectionReason, COUNT(l) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND l.rejectionReason IS NOT NULL AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY l.rejectionReason")
    List<Object[]> analyticsReasonTotals(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** clientType, rejectionReason, liczba */
    @Query("SELECT l.clientType, l.rejectionReason, COUNT(l) FROM Lead l LEFT JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND l.rejectionReason IS NOT NULL AND l.clientType IS NOT NULL AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY l.clientType, l.rejectionReason")
    List<Object[]> analyticsReasonByIndustry(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

    /** userId, fullname, rejectionReason, liczba */
    @Query("SELECT u.id, u.fullname, l.rejectionReason, COUNT(l) FROM Lead l JOIN l.user u LEFT JOIN u.team t " +
           "WHERE l.creationDate >= :since AND l.creationDate <= :until AND l.rejectionReason IS NOT NULL AND (:teamId IS NULL OR t.id = :teamId) " +
           "GROUP BY u.id, u.fullname, l.rejectionReason")
    List<Object[]> analyticsReasonByRep(@Param("since") LocalDateTime since, @Param("until") LocalDateTime until, @Param("teamId") Long teamId);

}
