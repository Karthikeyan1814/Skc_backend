package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.MembershipPlanRepo;
import skc.client.project.SKC_Backend_Files.Repo.MembershipRequestRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Client;
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
                to, "Karthikeyan", "GOLD", 6, LocalDate.now().plusMonths(6).toString(), "This is a verified test sample email from Sri Kathiravan & Co Portal"
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
        Client client = clientRepo.findByEmail(req.getEmail());
        if (client == null) {
            String tempPassword = req.getMobile().length() >= 4
                    ? req.getMobile().substring(req.getMobile().length() - 4) + "2024"
                    : "skc2024";
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
        client.setMembershipExpiry(LocalDate.now().plusMonths(months));
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

        // Send activation email to client
        emailService.sendMembershipActivationEmail(
            req.getEmail(), req.getFullName(), planType, months,
            LocalDate.now().plusMonths(months).toString(), adminNote
        );

        response.put("status", "success");
        response.put("message", "Membership activated! " + planType + " plan valid for " + months + " months.");
        response.put("clientId", savedClient.getId());
        response.put("expiry", LocalDate.now().plusMonths(months).toString());
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
        boolean active = "MEMBER".equals(client.getMembershipStatus())
                && client.getMembershipExpiry() != null
                && !client.getMembershipExpiry().isBefore(LocalDate.now());
        res.put("isActive", active);
        return ResponseEntity.ok(res);
    }

    // ─── ADMIN: Get / Update membership plan settings ────────────────────────────
    @GetMapping("/Admin/Membership/Plans")
    public ResponseEntity<List<MembershipPlan>> getPlans() {
        List<MembershipPlan> plans = planRepo.findAll();
        if (plans.isEmpty()) {
            // seed defaults
            plans = seedDefaultPlans();
        }
        return ResponseEntity.ok(plans);
    }

    @PutMapping("/Admin/Membership/Plans")
    public ResponseEntity<Object> updatePlans(@RequestBody List<MembershipPlan> plans) {
        planRepo.saveAll(plans);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Plans updated successfully"));
    }

    @PutMapping("/Admin/Membership/Plans/{tier}")
    public ResponseEntity<Object> updateSinglePlan(
            @PathVariable String tier,
            @RequestBody MembershipPlan plan) {
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
        if (planType == null) return 1;
        return switch (planType.toUpperCase()) {
            case "GOLD" -> 6;
            case "PLATINUM" -> 12;
            default -> 1; // SILVER
        };
    }

    private List<MembershipPlan> seedDefaultPlans() {
        MembershipPlan silver = new MembershipPlan();
        silver.setPlanTier("SILVER"); silver.setDisplayName("Silver"); silver.setDurationMonths(1);
        silver.setPrice(299.0); silver.setFeatures("24/7 Support,GST Guidance,Priority Response");

        MembershipPlan gold = new MembershipPlan();
        gold.setPlanTier("GOLD"); gold.setDisplayName("Gold"); gold.setDurationMonths(6);
        gold.setPrice(699.0); gold.setFeatures("All Silver,Document Support,Live Chat,File Access");

        MembershipPlan platinum = new MembershipPlan();
        platinum.setPlanTier("PLATINUM"); platinum.setDisplayName("Platinum"); platinum.setDurationMonths(12);
        platinum.setPrice(999.0); platinum.setFeatures("All Gold,1-on-1 Meetings,Unlimited File Downloads,Compliance Reminders");

        return planRepo.saveAll(List.of(silver, gold, platinum));
    }
}
