package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.project.ConstructionLogComment;

public interface ConstructionLogCommentRepository extends JpaRepository<ConstructionLogComment, Long> {
}
