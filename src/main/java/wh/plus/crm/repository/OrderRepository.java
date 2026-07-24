package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.model.order.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByProject_IdOrderByIdDesc(Long projectId);

    List<Order> findAllByOrderByIdDesc();

    /** Odpięcie zamówień od usuwanego etapu (stageId to luźne pole, bez twardego FK). */
    @Modifying
    @Query("update Order o set o.stageId = null where o.stageId = :stageId")
    void clearStage(@Param("stageId") Long stageId);
}
