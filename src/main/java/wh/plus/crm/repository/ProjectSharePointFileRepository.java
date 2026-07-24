package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.sharepoint.ProjectSharePointFile;

import java.util.List;

public interface ProjectSharePointFileRepository extends JpaRepository<ProjectSharePointFile, Long> {
    List<ProjectSharePointFile> findAllByProject_IdOrderByCategoryAscNameAsc(Long projectId);
}
