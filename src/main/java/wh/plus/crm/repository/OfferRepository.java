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

    @Query("SELECT o.salesTeam.id, o.salesTeam.name, COUNT(o), " +
            "SUM(CASE WHEN o.contractSigned = true AND o.signedContractDate >= :since THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.totalPrice), 0), " +
            "COALESCE(SUM(CASE WHEN o.contractSigned = true AND o.signedContractDate >= :since THEN o.totalPrice ELSE 0 END), 0), " +
            "COALESCE(AVG(o.totalPrice), 0) " +
            "FROM Offer o " +
            "WHERE o.creationDate >= :since " +
            "AND o.salesTeam IS NOT NULL " +
            "GROUP BY o.salesTeam.id, o.salesTeam.name")
    List<Object[]> getStatisticsByTeam(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o), " +
            "SUM(CASE WHEN o.contractSigned = true AND o.signedContractDate >= :since THEN 1 ELSE 0 END), " +
            "COALESCE(SUM(o.totalPrice), 0), " +
            "COALESCE(SUM(CASE WHEN o.contractSigned = true AND o.signedContractDate >= :since THEN o.totalPrice ELSE 0 END), 0), " +
            "COALESCE(AVG(o.totalPrice), 0) " +
            "FROM Offer o " +
            "WHERE o.creationDate >= :since")
    List<Object[]> getOverallStatistics(@Param("since") LocalDateTime since);

}
