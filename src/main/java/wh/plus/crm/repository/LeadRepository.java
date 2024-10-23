package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.lead.Lead;


public interface LeadRepository extends JpaRepository<Lead, Long> {

    Page<Lead> findAll(Pageable pageable);
}
