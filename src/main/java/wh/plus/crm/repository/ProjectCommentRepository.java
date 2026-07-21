package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.project.ProjectComment;

import java.util.List;

public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    List<ProjectComment> findAllByProject_IdOrderByCreationDateDesc(Long projectId);
}
