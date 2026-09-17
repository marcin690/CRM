package wh.plus.crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import wh.plus.crm.model.aichat.AiChatApp;
import wh.plus.crm.model.aichat.AiConversation;

import java.util.List;
import java.util.Optional;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    /** Pobranie rozmowy z weryfikacją właściciela — podstawa izolacji. */
    Optional<AiConversation> findByIdAndOwnerUserIdAndDeletedAtIsNull(Long id, Long ownerUserId);

    List<AiConversation> findByOwnerUserIdAndAppAndDeletedAtIsNullOrderByUpdatedAtDesc(Long ownerUserId, AiChatApp app);

    List<AiConversation> findByOwnerUserIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long ownerUserId);

    /** Do czyszczenia przy dezaktywacji konta (usunięcie rozmów w Dify). */
    List<AiConversation> findByOwnerUserIdAndDeletedAtIsNull(Long ownerUserId);
}
