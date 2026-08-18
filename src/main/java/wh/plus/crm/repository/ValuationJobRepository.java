package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.aivaluation.ValuationJob;
import wh.plus.crm.model.aivaluation.ValuationStatus;

import java.util.Collection;
import java.util.List;

public interface ValuationJobRepository extends JpaRepository<ValuationJob, Long> {
    List<ValuationJob> findAllByOrderByIdDesc();
    long countByStatusIn(Collection<ValuationStatus> statuses);
}
