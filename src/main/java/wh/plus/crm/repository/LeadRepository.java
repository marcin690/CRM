package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.lead.Lead;

import java.time.LocalDateTime;


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

}
