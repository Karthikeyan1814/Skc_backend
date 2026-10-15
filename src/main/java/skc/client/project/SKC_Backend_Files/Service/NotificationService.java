package skc.client.project.SKC_Backend_Files.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skc.client.project.SKC_Backend_Files.Repo.NotificationRepo;
import skc.client.project.SKC_Backend_Files.entity.Notification;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepo notificationRepo;

    /**
     * Creates a notification for a specific user.
     */
    public Notification createNotification(String recipientRole, Long recipientId,
                                            String type, String title, String message, Long referenceId) {
        Notification n = new Notification(recipientRole, recipientId, type, title, message, referenceId);
        return notificationRepo.save(n);
    }

    /**
     * Broadcast to all ADMIN users (recipientId = null means all of that role).
     */
    public Notification notifyAllAdmins(String type, String title, String message, Long referenceId) {
        return createNotification("ADMIN", null, type, title, message, referenceId);
    }

    /**
     * Broadcast to all STAFF users.
     */
    public Notification notifyAllStaff(String type, String title, String message, Long referenceId) {
        return createNotification("STAFF", null, type, title, message, referenceId);
    }

    /**
     * Notify a specific client.
     */
    public Notification notifyClient(Long clientId, String type, String title, String message, Long referenceId) {
        return createNotification("CLIENT", clientId, type, title, message, referenceId);
    }

    /**
     * Get all notifications for a specific user.
     */
    public List<Notification> getNotificationsForUser(String role, Long userId) {
        return notificationRepo.findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(role, userId);
    }

    /**
     * Get all role-wide notifications (no specific user).
     */
    public List<Notification> getNotificationsForRole(String role) {
        return notificationRepo.findByRecipientRoleOrderByCreatedAtDesc(role);
    }

    /**
     * Mark a notification as read.
     */
    public void markAsRead(Long notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepo.save(n);
        });
    }

    /**
     * Mark all notifications for a user/role as read.
     */
    public void markAllAsRead(String role, Long userId) {
        List<Notification> unread;
        if (userId != null) {
            unread = notificationRepo.findByRecipientIdAndReadFalse(userId);
        } else {
            unread = notificationRepo.findByRecipientRoleAndReadFalse(role);
        }
        unread.forEach(n -> n.setRead(true));
        notificationRepo.saveAll(unread);
    }

    /**
     * Get unread count for a user.
     */
    public long getUnreadCount(String role, Long userId) {
        if (userId != null) {
            return notificationRepo.countByRecipientRoleAndRecipientIdAndReadFalse(role, userId);
        }
        return notificationRepo.countByRecipientRoleAndReadFalse(role);
    }

    /**
     * Delete a single notification by ID.
     */
    public boolean deleteNotification(Long notificationId) {
        if (notificationId != null && notificationRepo.existsById(notificationId)) {
            notificationRepo.deleteById(notificationId);
            return true;
        }
        return false;
    }

    /**
     * Delete all notifications for a specific user.
     */
    public void deleteAllForUser(String role, Long userId) {
        List<Notification> list;
        if (userId != null) {
            list = notificationRepo.findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(role, userId);
        } else {
            list = notificationRepo.findByRecipientRoleOrderByCreatedAtDesc(role);
        }
        if (list != null && !list.isEmpty()) {
            notificationRepo.deleteAll(list);
        }
    }
}
