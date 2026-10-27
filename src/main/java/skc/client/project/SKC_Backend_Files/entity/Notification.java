package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who receives the notification: ADMIN, STAFF, CLIENT
    private String recipientRole;

    // Specific recipient id (null = broadcast to all of that role)
    private Long recipientId;

    // Notification type: FILE_UPLOAD, FILE_EXPIRED, MEMBERSHIP_REQUEST,
    // APPOINTMENT_REQUEST, EXPIRY_EXTENSION_REQUEST, SYSTEM, ANNOUNCEMENT
    private String type;

    private String title;
    private String message;

    // Reference entity id (e.g. fileId, appointmentId)
    private Long referenceId;

    private boolean read = false;
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Notification() {}

    public Notification(String recipientRole, Long recipientId, String type, String title, String message, Long referenceId) {
        this.recipientRole = recipientRole;
        this.recipientId = recipientId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.referenceId = referenceId;
        this.createdAt = Instant.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }
    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
