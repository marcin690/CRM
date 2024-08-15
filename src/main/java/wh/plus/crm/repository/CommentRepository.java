package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.comment.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
