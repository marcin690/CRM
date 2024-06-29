package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.lead.LeadStatus;

import java.util.Optional;

public interface LeadStatusRepository extends JpaRepository<LeadStatus, Long> {

    Optional<LeadStatus> findByStatusName(String statusName);
}
