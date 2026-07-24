package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.order.AdditionalCost;

import java.util.List;

public interface AdditionalCostRepository extends JpaRepository<AdditionalCost, Long> {
    List<AdditionalCost> findAllByProject_IdOrderByCostDateDesc(Long projectId);

    /** Odpięcie kosztów od usuwanego etapu (stageId to luźne pole, bez twardego FK). */
    @Modifying
    @Query("update AdditionalCost c set c.stageId = null where c.stageId = :stageId")
    void clearStage(@Param("stageId") Long stageId);
}
