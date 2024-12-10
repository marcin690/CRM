package wh.plus.crm.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.comment.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByClientGlobalId(String clientGlobalId, Pageable pageable);

    Long countByLeadId(Long entityId);

    Long countByProjectId(Long entityId);

    Long countByClientId(Long entityId);

    Long countByOfferId(Long entityId);
}
