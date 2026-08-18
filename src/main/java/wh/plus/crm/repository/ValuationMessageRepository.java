package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.aivaluation.ValuationMessage;

public interface ValuationMessageRepository extends JpaRepository<ValuationMessage, Long> {
}
