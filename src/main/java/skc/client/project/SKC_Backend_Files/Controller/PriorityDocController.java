package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.PriorityDocRepo;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.PriorityDocRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skc")
public class PriorityDocController {

    @Autowired
    private PriorityDocRepo priorityDocRepo;

    @Autowired
    private ClientRepo clientRepo;

    @Autowired
    private NotificationService notificationService;

    // ─── CLIENT: Submit Priority Document Request ─────────────────────────────────
    @PostMapping("/Client/PriorityDocs/Request")
    public ResponseEntity<Object> submitPriorityDocRequest(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "User session is invalid. Please sign in again.");
            return ResponseEntity.status(401).body(response);
        }

        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            response.put("message", "Client profile not found.");
            return ResponseEntity.badRequest().body(response);
        }

        String docType = body.getOrDefault("docType", "CUSTOM_DOC");
        String message = body.getOrDefault("message", "");
        String urgency = body.getOrDefault("urgency", "VIP_PRIORITY");

        if (message.isBlank()) {
            response.put("status", "failed");
            response.put("message", "Query message cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        PriorityDocRequest req = new PriorityDocRequest();
        req.setClientId(client.getId());
        req.setClientName(client.getName());
        req.setClientEmail(client.getEmail());
        req.setClientMobile(client.getMobile());
        req.setMembershipPlan(client.getMembershipPlan() != null ? client.getMembershipPlan() : "SILVER");
        req.setDocType(docType);
        req.setMessage(message);
        req.setUrgency(urgency);
        req.setStatus("PENDING");

        PriorityDocRequest saved = priorityDocRepo.save(req);

        // Notify Admins & Staff in portal in real time
        try {
            String tierLabel = saved.getMembershipPlan() != null ? saved.getMembershipPlan() : "VIP";
            String title = "⚡ VIP Priority Document Request (" + tierLabel + ")";
            String note = client.getName() + " requested urgent " + formatDocTypeName(docType);
            notificationService.notifyAllAdmins("PRIORITY_DOC_REQUEST", title, note, saved.getId());
            notificationService.notifyAllStaff("PRIORITY_DOC_REQUEST", title, note, saved.getId());
        } catch (Exception e) {
            System.err.println("Priority doc notification error: " + e.getMessage());
        }

        response.put("status", "success");
        response.put("message", "Priority document request submitted successfully!");
        response.put("requestId", saved.getId());
        return ResponseEntity.ok(response);
    }

    // ─── CLIENT: View My Submitted Priority Document Inquiries ────────────────────
    @GetMapping("/Client/PriorityDocs/MyRequests")
    public ResponseEntity<List<PriorityDocRequest>> getMyPriorityDocRequests(
            @AuthenticationPrincipal AuthUser user) {
        if (user == null || user.getId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(priorityDocRepo.findByClientIdOrderByCreatedAtDesc(user.getId()));
    }

    // ─── ADMIN/STAFF: List All Priority Document Requests ─────────────────────────
    @GetMapping({"/Admin/PriorityDocs/All", "/Staff/PriorityDocs/All"})
    public ResponseEntity<List<PriorityDocRequest>> getAllPriorityDocRequests(
            @RequestParam(required = false) String status) {
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            return ResponseEntity.ok(priorityDocRepo.findByStatusOrderByCreatedAtDesc(status.toUpperCase()));
        }
        return ResponseEntity.ok(priorityDocRepo.findAllByOrderByCreatedAtDesc());
    }

    // ─── ADMIN/STAFF: Reply and Acknowledge Request ───────────────────────────────
    @PostMapping({"/Admin/PriorityDocs/{id}/Reply", "/Staff/PriorityDocs/{id}/Reply"})
    public ResponseEntity<Object> replyPriorityDocRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();

        PriorityDocRequest req = priorityDocRepo.findById(id).orElse(null);
        if (req == null) {
            response.put("status", "failed");
            response.put("message", "Priority document request not found.");
            return ResponseEntity.badRequest().body(response);
        }

        String reply = body.getOrDefault("reply", "").trim();
        String status = body.getOrDefault("status", "ACKNOWLEDGED").toUpperCase();
        String replierName = body.getOrDefault("repliedBy", (user != null && user.getUsername() != null) ? user.getUsername() : "SKC Audit Desk");

        if (reply.isBlank()) {
            response.put("status", "failed");
            response.put("message", "Acknowledgment reply cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        req.setAdminReply(reply);
        req.setStatus(status);
        req.setRepliedBy(replierName);
        req.setRepliedAt(LocalDateTime.now());
        priorityDocRepo.save(req);

        // Notify client
        try {
            if (req.getClientId() != null) {
                notificationService.notifyClient(
                        req.getClientId(),
                        "PRIORITY_DOC_ACKNOWLEDGED",
                        "⚡ Priority Request Update: " + formatDocTypeName(req.getDocType()),
                        replierName + " replied: " + reply,
                        req.getId()
                );
            }
        } catch (Exception e) {
            System.err.println("Priority doc client notification error: " + e.getMessage());
        }

        response.put("status", "success");
        response.put("message", "Acknowledgment dispatched to client successfully.");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Delete Request ───────────────────────────────────────────────────
    @DeleteMapping("/Admin/PriorityDocs/{id}")
    public ResponseEntity<Object> deletePriorityDocRequest(@PathVariable Long id) {
        priorityDocRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Request record removed."));
    }

    private String formatDocTypeName(String dt) {
        if (dt == null) return "Document";
        return switch (dt.toUpperCase()) {
            case "GST_RECONCILIATION" -> "GST Reconciliation Report";
            case "ITR_ACKNOWLEDGEMENT" -> "Income Tax ITR-V Acknowledgment";
            case "NETWORTH_CERTIFICATE" -> "CA Certified Net Worth Statement";
            case "BALANCE_SHEET_COPY" -> "Audited Balance Sheet & P&L";
            default -> "Custom Document Request";
        };
    }
}
