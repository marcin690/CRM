package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.project.ProjectStage;

import java.util.List;

public interface ProjectStageRepository extends JpaRepository<ProjectStage, Long> {
    List<ProjectStage> findAllByProject_IdOrderByStartDateAsc(Long projectId);

    List<ProjectStage> findAllByProject_IdOrderBySortOrderAscIdAsc(Long projectId);

    List<ProjectStage> findByProject_IdInOrderBySortOrderAscIdAsc(java.util.Collection<Long> projectIds);

    boolean existsByProject_Id(Long projectId);
}
