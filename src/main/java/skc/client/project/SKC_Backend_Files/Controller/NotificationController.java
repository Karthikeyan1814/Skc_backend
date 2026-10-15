package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skc")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    // ─── GET: Notifications for the authenticated user ────────────────────────────
    @GetMapping("/Client/Notifications")
    public ResponseEntity<List<Notification>> getClientNotifications(@AuthenticationPrincipal AuthUser user) {
        List<Notification> notifications = notificationService.getNotificationsForUser("CLIENT", user.getId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/Admin/Notifications")
    public ResponseEntity<List<Notification>> getAdminNotifications() {
        List<Notification> notifications = notificationService.getNotificationsForRole("ADMIN");
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/Staff/Notifications")
    public ResponseEntity<List<Notification>> getStaffNotifications(@AuthenticationPrincipal AuthUser user) {
        List<Notification> notifications = notificationService.getNotificationsForRole("STAFF");
        return ResponseEntity.ok(notifications);
    }

    // ─── GET: Unread count ────────────────────────────────────────────────────────
    @GetMapping("/Client/Notifications/UnreadCount")
    public ResponseEntity<Map<String, Long>> getClientUnread(@AuthenticationPrincipal AuthUser user) {
        long count = notificationService.getUnreadCount("CLIENT", user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/Admin/Notifications/UnreadCount")
    public ResponseEntity<Map<String, Long>> getAdminUnread() {
        long count = notificationService.getUnreadCount("ADMIN", null);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @GetMapping("/Staff/Notifications/UnreadCount")
    public ResponseEntity<Map<String, Long>> getStaffUnread(@AuthenticationPrincipal AuthUser user) {
        long count = notificationService.getUnreadCount("STAFF", user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ─── POST: Mark notification as read ──────────────────────────────────────────
    @PostMapping("/Client/Notifications/{id}/Read")
    public ResponseEntity<Map<String, String>> markReadClient(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/Admin/Notifications/{id}/Read")
    public ResponseEntity<Map<String, String>> markReadAdmin(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/Staff/Notifications/{id}/Read")
    public ResponseEntity<Map<String, String>> markReadStaff(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // ─── POST: Mark all as read ───────────────────────────────────────────────────
    @PostMapping("/Client/Notifications/ReadAll")
    public ResponseEntity<Map<String, String>> markAllReadClient(@AuthenticationPrincipal AuthUser user) {
        notificationService.markAllAsRead("CLIENT", user.getId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/Admin/Notifications/ReadAll")
    public ResponseEntity<Map<String, String>> markAllReadAdmin() {
        notificationService.markAllAsRead("ADMIN", null);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @PostMapping("/Staff/Notifications/ReadAll")
    public ResponseEntity<Map<String, String>> markAllReadStaff(@AuthenticationPrincipal AuthUser user) {
        notificationService.markAllAsRead("STAFF", user.getId());
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // ─── DELETE: Individual Notification ──────────────────────────────────────────
    @DeleteMapping({"/Client/Notifications/{id}", "/Client/Notification/{id}"})
    @PostMapping({"/Client/Notifications/{id}/Delete", "/Client/Notification/{id}/Delete"})
    public ResponseEntity<Map<String, Object>> deleteClientNotification(@PathVariable Long id) {
        boolean deleted = notificationService.deleteNotification(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Notification deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping({"/Admin/Notifications/{id}", "/Admin/Notification/{id}"})
    @PostMapping({"/Admin/Notifications/{id}/Delete", "/Admin/Notification/{id}/Delete"})
    public ResponseEntity<Map<String, Object>> deleteAdminNotification(@PathVariable Long id) {
        boolean deleted = notificationService.deleteNotification(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Notification deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping({"/Staff/Notifications/{id}", "/Staff/Notification/{id}"})
    @PostMapping({"/Staff/Notifications/{id}/Delete", "/Staff/Notification/{id}/Delete"})
    public ResponseEntity<Map<String, Object>> deleteStaffNotification(@PathVariable Long id) {
        boolean deleted = notificationService.deleteNotification(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Notification deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    // ─── DELETE: Clear all notifications ──────────────────────────────────────────
    @DeleteMapping("/Client/Notifications/ClearAll")
    @PostMapping("/Client/Notifications/ClearAll")
    public ResponseEntity<Map<String, Object>> clearAllClientNotifications(@AuthenticationPrincipal AuthUser user) {
        if (user != null && user.getId() != null) {
            notificationService.deleteAllForUser("CLIENT", user.getId());
        }
        return ResponseEntity.ok(Map.of("status", "success", "message", "All notifications cleared"));
    }

    @DeleteMapping("/Admin/Notifications/ClearAll")
    @PostMapping("/Admin/Notifications/ClearAll")
    public ResponseEntity<Map<String, Object>> clearAllAdminNotifications() {
        notificationService.deleteAllForUser("ADMIN", null);
        return ResponseEntity.ok(Map.of("status", "success", "message", "All admin alerts cleared"));
    }

    @DeleteMapping("/Staff/Notifications/ClearAll")
    @PostMapping("/Staff/Notifications/ClearAll")
    public ResponseEntity<Map<String, Object>> clearAllStaffNotifications(@AuthenticationPrincipal AuthUser user) {
        notificationService.deleteAllForUser("STAFF", user != null ? user.getId() : null);
        return ResponseEntity.ok(Map.of("status", "success", "message", "All staff alerts cleared"));
    }
}
