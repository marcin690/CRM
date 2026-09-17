package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import wh.plus.crm.dto.aichat.UserUsageRow;
import wh.plus.crm.model.aichat.AiUsage;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageRepository extends JpaRepository<AiUsage, Long> {

    boolean existsByDifyMessageId(String difyMessageId);

    /** Weryfikacja, że wiadomość należy do danej rozmowy (izolacja feedbacku). */
    boolean existsByConversationIdAndDifyMessageId(Long conversationId, String difyMessageId);

    /** Agregat zużycia per użytkownik od podanej daty — raport admina (30/90/365 dni). */
    @Query("""
            select u.userId as userId,
                   max(u.userEmail) as userEmail,
                   count(u.id) as messages,
                   count(distinct u.conversationId) as conversations,
                   coalesce(sum(u.promptTokens), 0) as promptTokens,
                   coalesce(sum(u.completionTokens), 0) as completionTokens,
                   coalesce(sum(u.costUsd), 0) as costUsd
            from AiUsage u
            where u.createdAt >= :since
            group by u.userId
            order by count(u.id) desc
            """)
    List<UserUsageRow> aggregateSince(@Param("since") LocalDateTime since);
}
