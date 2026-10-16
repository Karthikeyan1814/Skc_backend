package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.Staff;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/skc")
public class AccessManagementController {

    @Autowired private AdminRepo adminRepo;
    @Autowired private StaffRepo staffRepo;
    @Autowired private ClientRepo clientRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;
    @Autowired private NotificationService notificationService;

    // In-memory OTP storage for admin deactivation: key = initiatingAdminId + "_" + targetAdminId
    private static final Map<String, OtpRecord> ADMIN_DEACTIVATE_OTP = new ConcurrentHashMap<>();

    // In-memory OTP storage for admin creation: key = initiatingAdminId + "_" + targetEmail
    private static final Map<String, AdminCreationDraft> ADMIN_CREATION_OTP = new ConcurrentHashMap<>();

    private record OtpRecord(String otp, long expiresAt) {}

    private record AdminCreationDraft(
            String name,
            String email,
            String mobile,
            String password,
            String altmobile,
            String otp,
            long expiresAt
    ) {}

    // =========================================================================
    // 1. STAFF ACCESS MANAGEMENT (Direct toggle, no OTP)
    // =========================================================================

    @GetMapping("/Admin/Access/Staff")
    public ResponseEntity<List<Staff>> getAllStaff() {
        return ResponseEntity.ok(staffRepo.findAll());
    }

    @PostMapping("/Admin/Access/Staff/ToggleActive/{id}")
    public ResponseEntity<Object> toggleStaffActive(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Staff staff = staffRepo.findById(id).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            response.put("message", "Staff member not found");
            return ResponseEntity.badRequest().body(response);
        }

        staff.setActive(!staff.isActive());
        staffRepo.save(staff);

        response.put("status", "success");
        response.put("active", staff.isActive());
        response.put("message", staff.isActive() ? "Staff member activated" : "Staff member deactivated");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/Admin/Access/Staff/ResetPassword/{id}")
    public ResponseEntity<Object> resetStaffPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        Staff staff = staffRepo.findById(id).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            response.put("message", "Staff member not found");
            return ResponseEntity.badRequest().body(response);
        }

        String newPassword = body.get("password");
        if (newPassword == null || newPassword.length() < 4) {
            response.put("status", "failed");
            response.put("message", "Password must be at least 4 characters");
            return ResponseEntity.badRequest().body(response);
        }

        staff.setPassword(passwordEncoder.encode(newPassword));
        staffRepo.save(staff);

        response.put("status", "success");
        response.put("message", "Staff password reset successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/Admin/Access/Staff/Delete/{id}")
    public ResponseEntity<Object> deleteStaff(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (!staffRepo.existsById(id)) {
            response.put("status", "failed");
            response.put("message", "Staff not found");
            return ResponseEntity.badRequest().body(response);
        }
        staffRepo.deleteById(id);
        response.put("status", "success");
        response.put("message", "Staff deleted successfully");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 2. CLIENT ACCESS MANAGEMENT (Direct toggle, no OTP, Re-consultation alert)
    // =========================================================================

    @GetMapping("/Admin/Access/Clients")
    public ResponseEntity<List<Map<String, Object>>> getAllClientsAccess() {
        List<Client> clients = clientRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Client c : clients) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("email", c.getEmail());
            map.put("mobile", c.getMobile());
            map.put("legalname", c.getLegalname());
            map.put("gstnumber", c.getGstnumber());
            map.put("location", c.getLocation());
            map.put("membershipStatus", c.getMembershipStatus());
            map.put("membershipPlan", c.getMembershipPlan());
            map.put("membershipExpiry", c.getMembershipExpiry());
            map.put("active", c.isActive());
            map.put("reconsultationRequested", Boolean.TRUE.equals(c.isReconsultationRequested()));
            map.put("reconsultationNotes", c.getReconsultationNotes());
            map.put("reconsultationRequestedAt", c.getReconsultationRequestedAt());
            map.put("createdAt", c.getCreatedAt());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/Admin/Access/Clients/ToggleActive/{id}")
    public ResponseEntity<Object> toggleClientActive(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Client client = clientRepo.findById(id).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            response.put("message", "Client not found");
            return ResponseEntity.badRequest().body(response);
        }

        boolean willBeActive = !client.isActive();
        client.setActive(willBeActive);

        // If reactivating client, clear any pending reconsultation request
        if (willBeActive) {
            client.setReconsultationRequested(false);
            client.setReconsultationNotes(null);
            client.setReconsultationRequestedAt(null);
        }

        clientRepo.save(client);

        response.put("status", "success");
        response.put("active", client.isActive());
        response.put("reconsultationRequested", client.isReconsultationRequested());
        response.put("message", client.isActive() ? "Client account reactivated successfully" : "Client account deactivated successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/Admin/Access/Clients/ClearReconsultation/{id}")
    public ResponseEntity<Object> clearClientReconsultation(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Client client = clientRepo.findById(id).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            response.put("message", "Client not found");
            return ResponseEntity.badRequest().body(response);
        }

        client.setReconsultationRequested(false);
        clientRepo.save(client);

        response.put("status", "success");
        response.put("message", "Reconsultation request badge cleared");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 3. ADMIN ACCESS MANAGEMENT (Admin deactivation requires OTP to own email)
    // =========================================================================

    @GetMapping("/Admin/Access/Admins")
    public ResponseEntity<List<Map<String, Object>>> getAllAdmins() {
        List<Admin> admins = adminRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Admin a : admins) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("name", a.getName());
            map.put("email", a.getEmail());
            map.put("mobile", a.getMobile());
            map.put("altmobile", a.getAltmobile());
            map.put("role", a.getRole());
            map.put("active", a.isActive());
            result.add(map);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Send OTP to current admin's email to authorize deactivating another admin.
     */
    @PostMapping("/Admin/Access/Admins/SendDeactivateOtp/{targetAdminId}")
    public ResponseEntity<Object> sendAdminDeactivateOtp(
            @PathVariable Long targetAdminId,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        if (user.getId().equals(targetAdminId)) {
            response.put("status", "failed");
            response.put("message", "You cannot deactivate your own administrator account.");
            return ResponseEntity.badRequest().body(response);
        }

        Admin targetAdmin = adminRepo.findById(targetAdminId).orElse(null);
        if (targetAdmin == null) {
            response.put("status", "failed");
            response.put("message", "Target administrator not found.");
            return ResponseEntity.badRequest().body(response);
        }

        Admin currentAdmin = adminRepo.findById(user.getId()).orElse(null);
        String currentEmail = currentAdmin != null ? currentAdmin.getEmail() : null;
        if (currentEmail == null || currentEmail.isBlank()) {
            response.put("status", "failed");
            response.put("message", "Your administrator account has no email address configured to receive verification codes.");
            return ResponseEntity.badRequest().body(response);
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        String key = user.getId() + "_" + targetAdminId;
        ADMIN_DEACTIVATE_OTP.put(key, new OtpRecord(otp, System.currentTimeMillis() + 10 * 60 * 1000));

        // Send OTP email to current admin
        emailService.sendAdminDeactivationOtp(currentEmail, targetAdmin.getName(), otp);

        response.put("status", "success");
        response.put("message", "A 6-digit security code has been sent to your email (" + currentEmail + ") to authorize this deactivation.");
        response.put("targetAdminName", targetAdmin.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Verify OTP and complete deactivation of target admin.
     */
    @PostMapping("/Admin/Access/Admins/VerifyAndDeactivate")
    public ResponseEntity<Object> verifyAndDeactivateAdmin(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Long targetAdminId = null;
        if (body.get("targetAdminId") != null) {
            targetAdminId = Long.valueOf(body.get("targetAdminId").toString());
        }
        String otp = (String) body.get("otp");

        if (targetAdminId == null || otp == null || otp.isBlank()) {
            response.put("status", "failed");
            response.put("message", "Target Admin ID and verification OTP code are required.");
            return ResponseEntity.badRequest().body(response);
        }

        String key = user.getId() + "_" + targetAdminId;
        OtpRecord record = ADMIN_DEACTIVATE_OTP.get(key);

        if (record == null || !record.otp().equals(otp.trim())) {
            response.put("status", "failed");
            response.put("message", "Invalid verification code. Please check your email or request a new code.");
            return ResponseEntity.badRequest().body(response);
        }

        if (System.currentTimeMillis() > record.expiresAt()) {
            ADMIN_DEACTIVATE_OTP.remove(key);
            response.put("status", "failed");
            response.put("message", "Verification code has expired. Please request a new code.");
            return ResponseEntity.badRequest().body(response);
        }

        Admin targetAdmin = adminRepo.findById(targetAdminId).orElse(null);
        if (targetAdmin == null) {
            response.put("status", "failed");
            response.put("message", "Target administrator not found.");
            return ResponseEntity.badRequest().body(response);
        }

        targetAdmin.setActive(false);
        adminRepo.save(targetAdmin);
        ADMIN_DEACTIVATE_OTP.remove(key);

        response.put("status", "success");
        response.put("message", "Administrator '" + targetAdmin.getName() + "' has been deactivated successfully.");
        return ResponseEntity.ok(response);
    }

    /**
     * Re-activate a deactivated admin (direct action).
     */
    @PostMapping("/Admin/Access/Admins/Activate/{id}")
    public ResponseEntity<Object> activateAdmin(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Admin admin = adminRepo.findById(id).orElse(null);
        if (admin == null) {
            response.put("status", "failed");
            response.put("message", "Administrator not found");
            return ResponseEntity.badRequest().body(response);
        }

        admin.setActive(true);
        adminRepo.save(admin);

        response.put("status", "success");
        response.put("message", "Administrator '" + admin.getName() + "' reactivated successfully.");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 4. ROLE CREATION: STAFF & ADMIN CREDENTIALS
    // =========================================================================

    /**
     * Create Staff Role Credential directly without OTP.
     */
    @PostMapping("/Admin/Access/CreateStaff")
    public ResponseEntity<Object> createStaffRole(@RequestBody Staff staff) {
        Map<String, Object> response = new HashMap<>();

        if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            response.put("status", "failed");
            response.put("message", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }

        String email = staff.getEmail().trim().toLowerCase();
        if (staffRepo.findByEmail(email) != null) {
            response.put("status", "failed");
            response.put("message", "A staff account with this email already exists");
            return ResponseEntity.badRequest().body(response);
        }

        if (staff.getPassword() == null || staff.getPassword().length() < 4) {
            response.put("status", "failed");
            response.put("message", "Password must be at least 4 characters");
            return ResponseEntity.badRequest().body(response);
        }

        String rawPassword = staff.getPassword();
        staff.setEmail(email);
        staff.setPassword(passwordEncoder.encode(staff.getPassword()));
        staff.setRole("STAFF");
        staff.setActive(true);
        Staff saved = staffRepo.save(staff);

        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            System.out.println(">>> [AccessManagementController] Dispatching welcome email to staff: " + saved.getEmail());
            emailService.sendAccountCreationWelcomeEmail(
                    saved.getEmail(),
                    saved.getName() != null ? saved.getName() : "Staff Member",
                    saved.getEmail(),
                    rawPassword,
                    "STAFF"
            );
        }

        response.put("status", "success");
        response.put("message", "Staff credential created successfully for " + staff.getName());
        response.put("id", saved.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Step 1: Send OTP to current admin's email to authorize creating a new Admin.
     */
    @PostMapping("/Admin/Access/SendAdminCreationOtp")
    public ResponseEntity<Object> sendAdminCreationOtp(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        String name = body.get("name");
        String email = body.get("email");
        String mobile = body.get("mobile");
        String password = body.get("password");
        String altmobile = body.get("altmobile");

        if (name == null || name.isBlank() || email == null || email.isBlank() ||
                mobile == null || mobile.isBlank() || password == null || password.length() < 4) {
            response.put("status", "failed");
            response.put("message", "All fields (name, email, 10-digit mobile, password >= 4 chars) are required.");
            return ResponseEntity.badRequest().body(response);
        }

        email = email.trim().toLowerCase();
        if (adminRepo.findByEmail(email) != null) {
            response.put("status", "failed");
            response.put("message", "An administrator with email " + email + " already exists.");
            return ResponseEntity.badRequest().body(response);
        }

        if (adminRepo.findByMobile(mobile) != null) {
            response.put("status", "failed");
            response.put("message", "An administrator with mobile number " + mobile + " already exists.");
            return ResponseEntity.badRequest().body(response);
        }

        Admin currentAdmin = adminRepo.findById(user.getId()).orElse(null);
        String currentEmail = currentAdmin != null ? currentAdmin.getEmail() : null;
        if (currentEmail == null || currentEmail.isBlank()) {
            response.put("status", "failed");
            response.put("message", "Your current admin profile does not have a registered email to receive the authorization OTP.");
            return ResponseEntity.badRequest().body(response);
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        String key = user.getId() + "_" + email;

        ADMIN_CREATION_OTP.put(key, new AdminCreationDraft(
                name.trim(), email, mobile.trim(), password, altmobile != null ? altmobile.trim() : "",
                otp, System.currentTimeMillis() + 10 * 60 * 1000
        ));

        // Send OTP email to current admin
        emailService.sendAdminCreationOtp(currentEmail, name.trim(), otp);

        response.put("status", "success");
        response.put("message", "A 6-digit security authorization code has been sent to your email (" + currentEmail + ").");
        response.put("candidateEmail", email);
        return ResponseEntity.ok(response);
    }

    /**
     * Step 2: Verify OTP and create new Admin role credential.
     */
    @PostMapping("/Admin/Access/CreateAdmin")
    public ResponseEntity<Object> verifyAndCreateAdmin(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null || email.isBlank() || otp.isBlank()) {
            response.put("status", "failed");
            response.put("message", "Email and verification code are required.");
            return ResponseEntity.badRequest().body(response);
        }

        email = email.trim().toLowerCase();
        String key = user.getId() + "_" + email;
        AdminCreationDraft draft = ADMIN_CREATION_OTP.get(key);

        if (draft == null || !draft.otp().equals(otp.trim())) {
            response.put("status", "failed");
            response.put("message", "Invalid verification code. Please check your email or resend code.");
            return ResponseEntity.badRequest().body(response);
        }

        if (System.currentTimeMillis() > draft.expiresAt()) {
            ADMIN_CREATION_OTP.remove(key);
            response.put("status", "failed");
            response.put("message", "Verification code has expired. Please initiate creation again.");
            return ResponseEntity.badRequest().body(response);
        }

        // Create new Admin
        Admin newAdmin = new Admin();
        newAdmin.setName(draft.name());
        newAdmin.setEmail(draft.email());
        newAdmin.setMobile(draft.mobile());
        newAdmin.setAltmobileno(draft.altmobile());
        newAdmin.setPassword(passwordEncoder.encode(draft.password()));
        newAdmin.setRole("ADMIN");
        newAdmin.setActive(true);

        Admin saved = adminRepo.save(newAdmin);
        ADMIN_CREATION_OTP.remove(key);

        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            emailService.sendAccountCreationWelcomeEmail(
                    saved.getEmail(),
                    saved.getName(),
                    saved.getEmail(),
                    draft.password(),
                    "ADMIN"
            );
        }

        response.put("status", "success");
        response.put("message", "Administrator account for '" + saved.getName() + "' created successfully!");
        response.put("adminId", saved.getId());
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 5. CLIENT DEACTIVATION & RE-CONSULTATION (Client portal)
    // =========================================================================

    /**
     * Client calls this to request re-consultation / reactivation when their account is deactivated.
     */
    @PostMapping("/Client/RequestReconsultation")
    public ResponseEntity<Object> requestReconsultation(
            @RequestBody(required = false) Map<String, String> body,
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
            response.put("message", "Client account not found");
            return ResponseEntity.badRequest().body(response);
        }

        String notes = body != null ? body.get("notes") : null;
        if (notes == null || notes.isBlank()) {
            notes = "Client requested account reactivation and re-consultation.";
        }

        client.setReconsultationRequested(true);
        client.setReconsultationNotes(notes.trim());
        client.setReconsultationRequestedAt(LocalDateTime.now());
        clientRepo.save(client);

        // Notify admins
        try {
            notificationService.notifyAllAdmins(
                    "CLIENT_RECONSULTATION",
                    "⚠️ Client Re-consultation Request",
                    client.getName() + " (" + client.getEmail() + ") has submitted a request for account re-consultation and reactivation.",
                    client.getId()
            );
        } catch (Exception e) {
            System.err.println("Could not create admin notification: " + e.getMessage());
        }

        response.put("status", "success");
        response.put("message", "Your re-consultation request has been submitted to the management team. An administrator will review your account details.");
        return ResponseEntity.ok(response);
    }

    /**
     * Check client account status (active, reconsultation status, membership).
     */
    @GetMapping("/Client/Status")
    public ResponseEntity<Object> getClientStatus(@AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            response.put("message", "Client account not found");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("id", client.getId());
        response.put("name", client.getName());
        response.put("email", client.getEmail());
        response.put("mobile", client.getMobile());
        response.put("legalname", client.getLegalname());
        response.put("active", client.isActive());
        response.put("isDeactivated", !client.isActive());
        response.put("reconsultationRequested", Boolean.TRUE.equals(client.isReconsultationRequested()));
        response.put("reconsultationNotes", client.getReconsultationNotes());
        response.put("reconsultationRequestedAt", client.getReconsultationRequestedAt());
        response.put("membershipStatus", client.getMembershipStatus());
        response.put("membershipPlan", client.getMembershipPlan());
        response.put("membershipExpiry", client.getMembershipExpiry());

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 6. ADMIN PROFILE & SECURITY MANAGEMENT
    // =========================================================================

    /**
     * Get current Admin profile details.
     */
    @GetMapping("/Admin/Profile")
    public ResponseEntity<Object> getAdminProfile(@AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Admin admin = adminRepo.findById(user.getId()).orElse(null);
        if (admin == null) {
            response.put("status", "failed");
            response.put("message", "Admin profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("status", "success");
        response.put("id", admin.getId());
        response.put("name", admin.getName());
        response.put("email", admin.getEmail());
        response.put("mobile", admin.getMobile());
        response.put("altmobile", admin.getAltmobile());
        response.put("role", admin.getRole() != null ? admin.getRole() : "ADMIN");
        response.put("active", admin.isActive());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current Admin profile details.
     */
    @PutMapping("/Admin/Profile")
    public ResponseEntity<Object> updateAdminProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Admin admin = adminRepo.findById(user.getId()).orElse(null);
        if (admin == null) {
            response.put("status", "failed");
            response.put("message", "Admin profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        String name = body.get("name");
        String email = body.get("email");
        String mobile = body.get("mobile");
        String altmobile = body.get("altmobile");

        if (name == null || name.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Full name cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }
        if (email == null || email.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Email address cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }
        if (mobile == null || mobile.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Mobile number cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        // Validate uniqueness if email changed
        String cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail.equalsIgnoreCase(admin.getEmail())) {
            if (adminRepo.findByEmail(cleanEmail) != null || clientRepo.findByEmail(cleanEmail) != null || staffRepo.findByEmail(cleanEmail) != null) {
                response.put("status", "failed");
                response.put("message", "The email " + cleanEmail + " is already in use by another account.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        // Validate uniqueness if mobile changed
        String cleanMobile = mobile.trim();
        if (!cleanMobile.equalsIgnoreCase(admin.getMobile())) {
            if (adminRepo.findByMobile(cleanMobile) != null || clientRepo.findByMobile(cleanMobile) != null || staffRepo.findByMobile(cleanMobile) != null) {
                response.put("status", "failed");
                response.put("message", "The mobile number " + cleanMobile + " is already in use by another account.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        admin.setName(name.trim());
        admin.setEmail(cleanEmail);
        admin.setMobile(cleanMobile);
        if (altmobile != null) {
            admin.setAltmobile(altmobile.trim());
        }

        adminRepo.save(admin);

        response.put("status", "success");
        response.put("message", "Profile updated successfully!");
        response.put("id", admin.getId());
        response.put("name", admin.getName());
        response.put("email", admin.getEmail());
        response.put("mobile", admin.getMobile());
        response.put("altmobile", admin.getAltmobile());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current Admin password.
     */
    @PutMapping("/Admin/Profile/Password")
    public ResponseEntity<Object> updateAdminPassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Admin admin = adminRepo.findById(user.getId()).orElse(null);
        if (admin == null) {
            response.put("status", "failed");
            response.put("message", "Admin profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Please enter your current password");
            return ResponseEntity.badRequest().body(response);
        }
        if (newPassword == null || newPassword.length() < 6) {
            response.put("status", "failed");
            response.put("message", "New password must be at least 6 characters long");
            return ResponseEntity.badRequest().body(response);
        }

        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            response.put("status", "failed");
            response.put("message", "Current password does not match. Please verify and try again.");
            return ResponseEntity.badRequest().body(response);
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        adminRepo.save(admin);

        response.put("status", "success");
        response.put("message", "Password updated successfully. Please use your new password next time you log in.");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 6B. STAFF PROFILE & CREDENTIAL MANAGEMENT
    // =========================================================================

    /**
     * Get current authenticated Staff profile.
     */
    @GetMapping("/Staff/Profile")
    public ResponseEntity<Object> getStaffProfile(@AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Staff staff = staffRepo.findById(user.getId()).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            response.put("message", "Staff profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        response.put("status", "success");
        response.put("id", staff.getId());
        response.put("name", staff.getName());
        response.put("email", staff.getEmail());
        response.put("mobile", staff.getMobile());
        response.put("designation", staff.getDesignation() != null ? staff.getDesignation() : "Audit Officer & Client Specialist");
        response.put("role", staff.getRole());
        response.put("active", staff.isActive());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current authenticated Staff profile.
     */
    @PutMapping("/Staff/Profile")
    public ResponseEntity<Object> updateStaffProfile(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Staff staff = staffRepo.findById(user.getId()).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            response.put("message", "Staff profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        String name = body.get("name");
        String email = body.get("email");
        String mobile = body.get("mobile");
        String designation = body.get("designation");

        if (name == null || name.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Full name cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }
        if (email == null || email.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Email address cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }
        if (mobile == null || mobile.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Mobile number cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        String cleanEmail = email.trim().toLowerCase();
        if (!cleanEmail.equalsIgnoreCase(staff.getEmail())) {
            if (adminRepo.findByEmail(cleanEmail) != null || clientRepo.findByEmail(cleanEmail) != null || staffRepo.findByEmail(cleanEmail) != null) {
                response.put("status", "failed");
                response.put("message", "The email " + cleanEmail + " is already in use by another account.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        String cleanMobile = mobile.trim();
        if (!cleanMobile.equalsIgnoreCase(staff.getMobile())) {
            if (adminRepo.findByMobile(cleanMobile) != null || clientRepo.findByMobile(cleanMobile) != null || staffRepo.findByMobile(cleanMobile) != null) {
                response.put("status", "failed");
                response.put("message", "The mobile number " + cleanMobile + " is already in use by another account.");
                return ResponseEntity.badRequest().body(response);
            }
        }

        staff.setName(name.trim());
        staff.setEmail(cleanEmail);
        staff.setMobile(cleanMobile);
        if (designation != null && !designation.trim().isEmpty()) {
            staff.setDesignation(designation.trim());
        }

        staffRepo.save(staff);

        response.put("status", "success");
        response.put("message", "Staff profile updated successfully!");
        response.put("id", staff.getId());
        response.put("name", staff.getName());
        response.put("email", staff.getEmail());
        response.put("mobile", staff.getMobile());
        response.put("designation", staff.getDesignation());
        return ResponseEntity.ok(response);
    }

    /**
     * Update current Staff password.
     */
    @PutMapping("/Staff/Profile/Password")
    public ResponseEntity<Object> updateStaffPassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        Staff staff = staffRepo.findById(user.getId()).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            response.put("message", "Staff profile not found");
            return ResponseEntity.badRequest().body(response);
        }

        String currentPassword = body.get("currentPassword");
        String newPassword = body.get("newPassword");

        if (currentPassword == null || currentPassword.isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Please enter your current password");
            return ResponseEntity.badRequest().body(response);
        }
        if (newPassword == null || newPassword.length() < 6) {
            response.put("status", "failed");
            response.put("message", "New password must be at least 6 characters long");
            return ResponseEntity.badRequest().body(response);
        }

        if (!passwordEncoder.matches(currentPassword, staff.getPassword())) {
            response.put("status", "failed");
            response.put("message", "Current password does not match. Please verify and try again.");
            return ResponseEntity.badRequest().body(response);
        }

        staff.setPassword(passwordEncoder.encode(newPassword));
        staffRepo.save(staff);

        response.put("status", "success");
        response.put("message", "Password updated successfully!");
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // 7. ADMIN BROADCAST NOTIFICATION TO CLIENTS, STAFF & ADMINS
    // =========================================================================

    /**
     * Dispatches announcement notification and real email to clients, staff, or admins.
     */
    @PostMapping("/Admin/BroadcastNotification")
    public ResponseEntity<Object> broadcastNotification(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized session");
            return ResponseEntity.status(401).body(response);
        }

        String target = body.get("target") != null ? body.get("target").toString().toLowerCase() : "all";
        String title = body.get("title") != null ? body.get("title").toString().trim() : "Office Announcement";
        String message = body.get("message") != null ? body.get("message").toString().trim() : "";
        boolean sendEmail = body.get("sendEmail") == null || Boolean.parseBoolean(body.get("sendEmail").toString());

        if (message.isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Notification message content cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        int count = 0;

        // Dispatches based on target
        if (target.equals("clients") || target.equals("all")) {
            List<Client> clients = clientRepo.findAll();
            for (Client c : clients) {
                if (Boolean.TRUE.equals(c.isActive())) {
                    notificationService.notifyClient(c.getId(), "ANNOUNCEMENT", title, message, null);
                    if (sendEmail && c.getEmail() != null && !c.getEmail().isBlank()) {
                        emailService.sendBroadcastNotificationEmail(c.getEmail(), c.getName(), title, message);
                    }
                    count++;
                }
            }
        }

        if (target.equals("staff") || target.equals("all")) {
            List<Staff> staffList = staffRepo.findAll();
            for (Staff s : staffList) {
                if (Boolean.TRUE.equals(s.isActive())) {
                    notificationService.createNotification("STAFF", s.getId(), "ANNOUNCEMENT", title, message, null);
                    if (sendEmail && s.getEmail() != null && !s.getEmail().isBlank()) {
                        emailService.sendBroadcastNotificationEmail(s.getEmail(), s.getName(), title, message);
                    }
                    count++;
                }
            }
        }

        if (target.equals("admins") || target.equals("all")) {
            List<Admin> admins = adminRepo.findAll();
            for (Admin a : admins) {
                if (Boolean.TRUE.equals(a.isActive())) {
                    notificationService.createNotification("ADMIN", a.getId(), "ANNOUNCEMENT", title, message, null);
                    if (sendEmail && a.getEmail() != null && !a.getEmail().isBlank()) {
                        emailService.sendBroadcastNotificationEmail(a.getEmail(), a.getName(), title, message);
                    }
                    count++;
                }
            }
        }

        response.put("status", "success");
        response.put("message", "Official broadcast dispatched to " + count + " recipient(s).");
        response.put("dispatchedCount", count);
        return ResponseEntity.ok(response);
    }
}
