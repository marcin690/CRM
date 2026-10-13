package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.lead.LeadCandidate;
import wh.plus.crm.model.lead.LeadCandidateStatus;

import java.util.List;
import java.util.Optional;

public interface LeadCandidateRepository extends JpaRepository<LeadCandidate, Long> {

    Optional<LeadCandidate> findByDedupKey(String dedupKey);

    List<LeadCandidate> findByStatusOrderByPunktyDescIdDesc(LeadCandidateStatus status);

    long countByStatus(LeadCandidateStatus status);
}
