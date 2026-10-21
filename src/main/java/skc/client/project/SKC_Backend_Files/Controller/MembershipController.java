package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.FirmSettingsRepo;
import skc.client.project.SKC_Backend_Files.Repo.MembershipPlanRepo;
import skc.client.project.SKC_Backend_Files.Repo.MembershipRequestRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.FirmSettings;
import skc.client.project.SKC_Backend_Files.entity.MembershipPlan;
import skc.client.project.SKC_Backend_Files.entity.MembershipRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skc")
public class MembershipController {

    @Autowired private MembershipRequestRepo membershipRepo;
    @Autowired private ClientRepo clientRepo;
    @Autowired private MembershipPlanRepo planRepo;
    @Autowired private FirmSettingsRepo firmRepo;
    @Autowired private EmailService emailService;
    @Autowired private NotificationService notificationService;
    @Autowired private PasswordEncoder passwordEncoder;

    // ─── PUBLIC: Validate if email is registered ──────────────────────────────────
    @GetMapping("/Membership/CheckEmail")
    public ResponseEntity<Object> checkEmail(@RequestParam String email) {
        Client client = clientRepo.findByEmail(email);
        Map<String, Object> res = new HashMap<>();
        if (client != null) {
            res.put("registered", true);
            res.put("name", client.getName());
            res.put("membershipStatus", client.getMembershipStatus());
        } else {
            res.put("registered", false);
        }
        return ResponseEntity.ok(res);
    }

    // ─── PUBLIC: Test email sending directly ──────────────────────────────────────
    @GetMapping("/Membership/SendSampleEmail")
    public ResponseEntity<Object> sendSampleEmail(@RequestParam(defaultValue = "karthikeyan1472005@gmail.com") String to) {
        Map<String, Object> res = new HashMap<>();
        try {
            emailService.sendMembershipActivationEmail(
                to, "Kathiravan & Co. Member", "GOLD", 6, LocalDate.now().plusMonths(6).toString(), "This is a verified test sample email from Sri Kathiravan & Co Portal"
            );
            res.put("status", "dispatched");
            res.put("recipient", to);
            res.put("message", "Sample activation email triggered to " + to);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            res.put("status", "failed");
            res.put("error", e.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ─── PUBLIC: Submit membership request (must be a registered client) ──────────
    @PostMapping("/Membership/Request")
    public ResponseEntity<Object> submitRequest(@RequestBody MembershipRequest request) {
        Map<String, Object> response = new HashMap<>();

        // Validate registered client
        Client client = clientRepo.findByEmail(request.getEmail());
        if (client == null) {
            response.put("status", "not_registered");
            response.put("message", "This email is not registered in our portal. Please contact the office to create an account first.");
            return ResponseEntity.badRequest().body(response);
        }

        // Check for duplicate pending
        List<MembershipRequest> existing = membershipRepo.findByEmailAndStatus(request.getEmail(), "PENDING");
        if (!existing.isEmpty()) {
            response.put("status", "duplicate");
            response.put("message", "You already have a pending membership request. We will contact you soon.");
            return ResponseEntity.badRequest().body(response);
        }

        // Pre-fill name from registered client
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            request.setFullName(client.getName());
        }
        request.setLinkedClientId(client.getId());

        MembershipRequest saved = membershipRepo.save(request);

        // Notify all admins in portal
        notificationService.notifyAllAdmins(
            "MEMBERSHIP_REQUEST",
            "New Membership Request",
            request.getFullName() + " applied for " + (request.getPlanType() != null ? request.getPlanType() : "SILVER") + " membership",
            saved.getId()
        );

        // Send confirmation email to applicant
        emailService.sendMembershipRequestConfirmation(request.getEmail(), request.getFullName(), request.getPlanType());

        // Send alert email to admin
        emailService.sendAdminMembershipAlert(request.getFullName(), request.getEmail(), request.getMobile(), request.getPlanType());

        response.put("status", "success");
        response.put("message", "Your membership request has been submitted. We will review and contact you within 24 hours.");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Get all membership requests (filterable by status) ────────────────
    @GetMapping("/Admin/Membership/Requests")
    public ResponseEntity<List<MembershipRequest>> getAllRequests(
            @RequestParam(defaultValue = "PENDING") String status) {
        List<MembershipRequest> requests = membershipRepo.findByStatus(status);
        return ResponseEntity.ok(requests);
    }

    // ─── ADMIN: Get all requests (no filter) ─────────────────────────────────────
    @GetMapping("/Admin/Membership/Requests/All")
    public ResponseEntity<List<MembershipRequest>> getAllRequestsAll() {
        return ResponseEntity.ok(membershipRepo.findAll());
    }

    // ─── ADMIN: Get pending count for live badge ───────────────────────────────────
    @GetMapping("/Admin/Membership/PendingCount")
    public ResponseEntity<Object> getPendingCount() {
        long count = membershipRepo.countByStatus("PENDING");
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ─── ADMIN: Get single request ────────────────────────────────────────────────
    @GetMapping("/Admin/Membership/Request/{id}")
    public ResponseEntity<Object> getRequest(@PathVariable Long id) {
        return membershipRepo.findById(id)
                .map(r -> (Object) r)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── ADMIN: Reject membership request ────────────────────────────────────────
    @PostMapping("/Admin/Membership/Reject/{id}")
    public ResponseEntity<Object> rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        MembershipRequest req = membershipRepo.findById(id).orElse(null);
        if (req == null) {
            response.put("status", "failed");
            response.put("message", "Request not found");
            return ResponseEntity.badRequest().body(response);
        }
        String reason = body.getOrDefault("reason", "");
        req.setStatus("REJECTED");
        req.setProcessedAt(LocalDateTime.now());
        req.setAdminNote(reason);
        membershipRepo.save(req);
        emailService.sendMembershipRejectionEmail(req.getEmail(), req.getFullName(), reason);
        response.put("status", "success");
        response.put("message", "Membership request rejected. Email sent to " + req.getEmail());
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Activate membership plan for a client (core action) ───────────────
    @PostMapping("/Admin/Membership/Activate/{id}")
    public ResponseEntity<Object> activateMembership(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        MembershipRequest req = membershipRepo.findById(id).orElse(null);
        if (req == null) {
            response.put("status", "failed");
            response.put("message", "Request not found");
            return ResponseEntity.badRequest().body(response);
        }

        String planType = body.getOrDefault("planType", req.getPlanType() != null ? req.getPlanType() : "SILVER");
        String adminNote = body.getOrDefault("adminNote", "");

        // Resolve duration from plan settings
        int months = resolveDuration(planType);

        // Find or create client
        Client client = null;
        if (req.getLinkedClientId() != null) {
            client = clientRepo.findById(req.getLinkedClientId()).orElse(null);
        }
        if (client == null && req.getEmail() != null) {
            client = clientRepo.findByEmail(req.getEmail().trim());
            if (client == null) {
                client = clientRepo.findByEmail(req.getEmail().trim().toLowerCase());
            }
        }
        if (client == null && req.getMobile() != null) {
            client = clientRepo.findByMobile(req.getMobile().trim());
        }

        if (client == null) {
            String tempPassword = "kathiravan&co";
            client = new Client();
            client.setName(req.getFullName());
            client.setEmail(req.getEmail());
            client.setMobile(req.getMobile());
            client.setPassword(passwordEncoder.encode(tempPassword));
            client.setLegalname(req.getBusinessName());
            client.setLocation(req.getLocation());
            client.setGstnumber(req.getGstnumber());
            client.setPannumber(req.getPannumber());
            client.setRole("CLIENT");
            client.setTemppass(true);
            client.setActive(true);
        }

        // Activate membership
        client.setMembershipStatus("MEMBER");
        client.setMembershipPlan(planType);
        LocalDate baseDate = (client.getMembershipExpiry() != null && client.getMembershipExpiry().isAfter(LocalDate.now()))
                ? client.getMembershipExpiry()
                : LocalDate.now();
        client.setMembershipExpiry(baseDate.plusMonths(months));
        client.setMembershipActivatedAt(LocalDateTime.now());
        Client savedClient = clientRepo.save(client);

        // Update request
        req.setStatus("APPROVED");
        req.setProcessedAt(LocalDateTime.now());
        req.setAdminNote(adminNote);
        req.setQrSent(true);
        req.setPlanType(planType);
        req.setLinkedClientId(savedClient.getId());
        membershipRepo.save(req);

        // Send activation email to client safely
        try {
            emailService.sendMembershipActivationEmail(
                req.getEmail(), req.getFullName(), planType, months,
                client.getMembershipExpiry().toString(), adminNote
            );
        } catch (Exception ex) {
            System.err.println("Activation email send notice: " + ex.getMessage());
        }

        response.put("status", "success");
        response.put("message", "Membership activated! " + planType + " plan valid for " + months + " months.");
        response.put("clientId", savedClient.getId());
        response.put("expiry", client.getMembershipExpiry().toString());
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Grant access (legacy approve — links existing client) ─────────────
    @PostMapping("/Admin/Membership/Approve/{id}")
    public ResponseEntity<Object> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        if (body == null) body = new HashMap<>();
        return activateMembership(id, body);
    }

    // ─── CLIENT: Check own membership status ──────────────────────────────────────
    @GetMapping("/Client/Membership/Status")
    public ResponseEntity<Object> getMyMembershipStatus(@AuthenticationPrincipal AuthUser user) {
        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) return ResponseEntity.notFound().build();
        Map<String, Object> res = new HashMap<>();
        res.put("membershipStatus", client.getMembershipStatus());
        res.put("membershipPlan", client.getMembershipPlan());
        res.put("membershipExpiry", client.getMembershipExpiry());
        res.put("membershipActivatedAt", client.getMembershipActivatedAt());
        res.put("email", client.getEmail());
        res.put("mobile", client.getMobile());
        res.put("clientName", client.getName());
        res.put("clientId", client.getId());
        boolean active = "MEMBER".equals(client.getMembershipStatus())
                && client.getMembershipExpiry() != null
                && !client.getMembershipExpiry().isBefore(LocalDate.now());
        res.put("isActive", active);
        return ResponseEntity.ok(res);
    }

    // ─── CLIENT: Authenticated Upgrade or Extension Request ───────────────────────
    @PostMapping("/Client/Membership/UpgradeRequest")
    public ResponseEntity<Object> clientUpgradeRequest(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            response.put("message", "Client profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        String action = body.get("action") != null ? body.get("action").toString() : "UPGRADE";
        String targetPlan = body.get("targetPlan") != null ? body.get("targetPlan").toString() : "PLATINUM";
        String utr = body.get("utr") != null ? body.get("utr").toString() : "";
        String note = body.get("note") != null ? body.get("note").toString() : "";
        int durationMonths = body.get("durationMonths") != null 
                ? Integer.parseInt(body.get("durationMonths").toString()) 
                : resolveDuration(targetPlan);

        // Find existing PENDING request or create new
        List<MembershipRequest> existing = membershipRepo.findByEmailAndStatus(client.getEmail(), "PENDING");
        MembershipRequest req;
        if (!existing.isEmpty()) {
            req = existing.get(0);
        } else {
            req = new MembershipRequest();
        }

        req.setFullName(client.getName());
        req.setEmail(client.getEmail());
        req.setMobile(client.getMobile());
        req.setBusinessName(client.getLegalname() != null ? client.getLegalname() : client.getName());
        req.setPlanType(targetPlan);
        req.setStatus("PENDING");
        req.setLinkedClientId(client.getId());
        req.setMessage(String.format("[%s REQUEST] Current: %s. Target: %s (%d Months). UTR/Ref: %s. Note: %s",
                action, client.getMembershipPlan() != null ? client.getMembershipPlan() : "NONE", targetPlan, durationMonths, utr, note));
        MembershipRequest saved = membershipRepo.save(req);

        // Notify admins in portal
        notificationService.notifyAllAdmins(
            "MEMBERSHIP_REQUEST",
            "Membership " + ("EXTEND".equalsIgnoreCase(action) ? "Extension" : "Upgrade") + " Request",
            client.getName() + " requested " + action + " to " + targetPlan + " (" + durationMonths + " months)",
            saved.getId()
        );

        response.put("status", "success");
        response.put("message", "Your " + action.toLowerCase() + " request has been received. Our team will verify and activate your privileges.");
        return ResponseEntity.ok(response);
    }

    // ─── GLOBAL MEMBERSHIP MASTER TOGGLE (ADMIN & PUBLIC STATUS) ─────────────────
    @GetMapping("/Public/Membership/GlobalSettings")
    public ResponseEntity<Object> getGlobalMembershipSettings() {
        FirmSettings firm = firmRepo.findById(1L).orElseGet(() -> firmRepo.save(new FirmSettings()));
        Map<String, Object> res = new HashMap<>();
        res.put("membershipEnabled", Boolean.TRUE.equals(firm.getMembershipEnabled()));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/Admin/Membership/ToggleMaster")
    public ResponseEntity<Object> toggleMembershipMaster(@RequestBody(required = false) Map<String, Object> body) {
        FirmSettings firm = firmRepo.findById(1L).orElseGet(() -> firmRepo.save(new FirmSettings()));
        boolean newState;
        if (body != null && body.containsKey("enabled")) {
            newState = Boolean.TRUE.equals(body.get("enabled"));
        } else {
            newState = !Boolean.TRUE.equals(firm.getMembershipEnabled());
        }
        firm.setMembershipEnabled(newState);
        firmRepo.save(firm);

        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("membershipEnabled", newState);
        res.put("message", newState ? "Membership system has been ACTIVATED" : "Membership system has been DEACTIVATED (Initial Launch Mode)");
        return ResponseEntity.ok(res);
    }

    // ─── ADMIN: Get / Update membership plan settings ────────────────────────────
    @GetMapping("/Admin/Membership/Plans")
    public ResponseEntity<List<MembershipPlan>> getPlans() {
        // Ensure legacy SILVER is permanently removed
        planRepo.findByPlanTier("SILVER").ifPresent(planRepo::delete);
        List<MembershipPlan> plans = planRepo.findAll();
        if (plans.isEmpty()) {
            plans = seedDefaultPlans();
        }
        return ResponseEntity.ok(plans);
    }

    @PutMapping("/Admin/Membership/Plans")
    public ResponseEntity<Object> updatePlans(@RequestBody List<MembershipPlan> plans) {
        // Ensure legacy SILVER is filtered out
        List<MembershipPlan> validPlans = plans.stream()
                .filter(p -> !"SILVER".equalsIgnoreCase(p.getPlanTier()))
                .toList();
        planRepo.saveAll(validPlans);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Plans updated successfully"));
    }

    @PutMapping("/Admin/Membership/Plans/{tier}")
    public ResponseEntity<Object> updateSinglePlan(
            @PathVariable String tier,
            @RequestBody MembershipPlan plan) {
        if ("SILVER".equalsIgnoreCase(tier)) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Silver tier is deprecated and removed."));
        }
        MembershipPlan existing = planRepo.findByPlanTier(tier.toUpperCase()).orElse(new MembershipPlan());
        existing.setPlanTier(tier.toUpperCase());
        existing.setDisplayName(plan.getDisplayName());
        existing.setDurationMonths(plan.getDurationMonths());
        existing.setPrice(plan.getPrice());
        existing.setFeatures(plan.getFeatures());
        existing.setActive(plan.isActive());
        planRepo.save(existing);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    // ─── ADMIN: Get all active members list ──────────────────────────────────────
    @GetMapping("/Admin/Membership/ActiveMembers")
    public ResponseEntity<List<Client>> getActiveMembers() {
        List<Client> members = clientRepo.findByMembershipStatus("MEMBER");
        return ResponseEntity.ok(members);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────
    private int resolveDuration(String planType) {
        if (planType == null) return 6;
        return planRepo.findByPlanTier(planType.toUpperCase())
                .map(MembershipPlan::getDurationMonths)
                .orElse(switch (planType.toUpperCase()) {
                    case "PLATINUM" -> 12;
                    default -> 6;
                });
    }

    private List<MembershipPlan> seedDefaultPlans() {
        planRepo.findByPlanTier("SILVER").ifPresent(planRepo::delete);

        MembershipPlan gold = new MembershipPlan();
        gold.setPlanTier("GOLD"); gold.setDisplayName("Gold"); gold.setDurationMonths(6);
        gold.setPrice(699.0); gold.setFeatures("Free File & Invoice Hub,Free Appointment Booking,Priority Messages to Admin,Live Chat Support,Document Review");

        MembershipPlan platinum = new MembershipPlan();
        platinum.setPlanTier("PLATINUM"); platinum.setDisplayName("Platinum"); platinum.setDurationMonths(12);
        platinum.setPrice(999.0); platinum.setFeatures("All Features Unlocked,Dedicated CA Manager,1-on-1 Video Consultations,Expedited Filing,Priority SCN Defense");

        return planRepo.saveAll(List.of(gold, platinum));
    }
}
