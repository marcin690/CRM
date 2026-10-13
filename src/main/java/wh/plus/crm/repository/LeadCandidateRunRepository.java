package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.lead.LeadCandidateRun;

import java.util.Optional;

public interface LeadCandidateRunRepository extends JpaRepository<LeadCandidateRun, Long> {

    Optional<LeadCandidateRun> findFirstByOrderByIdDesc();
}
