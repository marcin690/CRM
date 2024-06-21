package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.lead.Lead;

public interface LeadRepository extends JpaRepository<Lead, Long> {
}
