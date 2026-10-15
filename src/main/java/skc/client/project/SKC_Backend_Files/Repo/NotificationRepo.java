package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.Notification;
import java.util.List;

public interface NotificationRepo extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(String role, Long recipientId);
    List<Notification> findByRecipientRoleOrderByCreatedAtDesc(String role);
    List<Notification> findByRecipientRoleAndReadFalse(String role);
    List<Notification> findByRecipientIdAndReadFalse(Long recipientId);
    long countByRecipientRoleAndRecipientIdAndReadFalse(String role, Long recipientId);
    long countByRecipientRoleAndReadFalse(String role);
}
