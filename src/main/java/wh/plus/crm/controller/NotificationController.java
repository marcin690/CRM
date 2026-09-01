package wh.plus.crm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import wh.plus.crm.dto.notification.NotificationDTO;
import wh.plus.crm.model.EntityType;
import wh.plus.crm.model.notification.Notification;
import wh.plus.crm.model.user.User;
import wh.plus.crm.repository.NotificationRepository;
import wh.plus.crm.repository.UserRepository;

import java.util.List;
import java.util.Map;

/** REST dla dzwonka powiadomień zalogowanego użytkownika. */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repository;
    private final UserRepository userRepository;

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByUsername(auth.getName()).map(User::getId).orElse(null);
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> list() {
        Long uid = currentUserId();
        if (uid == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(repository.findTop30ByUser_IdOrderByIdDesc(uid).stream().map(this::toDto).toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        Long uid = currentUserId();
        long count = uid == null ? 0 : repository.countByUser_IdAndOpenFalse(uid);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Long id) {
        repository.findById(id).ifPresent(n -> { n.setOpen(true); repository.save(n); });
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll() {
        Long uid = currentUserId();
        if (uid != null) repository.markAllRead(uid);
        return ResponseEntity.noContent().build();
    }

    private NotificationDTO toDto(Notification n) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(n.getId());
        dto.setContent(n.getContent());
        dto.setOpen(n.isOpen());
        dto.setCreationDate(n.getCreationDate());
        if (n.getRelatedEntityId() != null && n.getRelatedEntityType() != null) {
            switch (n.getRelatedEntityType()) {
                case PROJECT -> dto.setLink("/projects/" + n.getRelatedEntityId());
                case LEAD -> dto.setLink("/leads/" + n.getRelatedEntityId());
                default -> { /* pozostałe typy bez linku */ }
            }
        }
        return dto;
    }
}
