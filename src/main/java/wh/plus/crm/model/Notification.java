package wh.plus.crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import wh.plus.crm.model.comment.EntityType;

import java.time.LocalDateTime;

@Entity
@Table
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private LocalDateTime timestamp;
    private boolean isOpened;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Long entityId;

    @Enumerated(EnumType.STRING)
    private EntityType entityType;
    private String entityUrl;






}
