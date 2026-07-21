package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.project.ConstructionLogEntry;

import java.util.List;

public interface ConstructionLogEntryRepository extends JpaRepository<ConstructionLogEntry, Long> {
    List<ConstructionLogEntry> findAllByProject_IdOrderByEntryDateDesc(Long projectId);
}
