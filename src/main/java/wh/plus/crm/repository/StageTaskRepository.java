package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.project.StageTask;

import java.util.List;

public interface StageTaskRepository extends JpaRepository<StageTask, Long> {
    List<StageTask> findAllByStage_IdOrderBySortOrderAscIdAsc(Long stageId);
}
