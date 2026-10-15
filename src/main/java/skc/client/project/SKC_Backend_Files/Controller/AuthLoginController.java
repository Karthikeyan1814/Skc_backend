package skc.client.project.SKC_Backend_Files.Controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Security.JwtUtil;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.dto.LoginRequest;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.Staff;

@RestController
@RequestMapping("/skc")
public class AuthLoginController {

    @Autowired private JwtUtil jwtutil;
    @Autowired private AdminRepo adminRepo;
    @Autowired private ClientRepo clientRepo;
    @Autowired private StaffRepo staffRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    // In-memory OTP store (email -> [otp, expiryMillis])
    private static final Map<String, OtpRecord> OTP_STORE = new ConcurrentHashMap<>();

    private record OtpRecord(String otp, long expiresAt) {}

    // ─── CLIENT LOGIN ────────────────────────────────────────────────────────────
    @PostMapping("/SignIn/Client")
    public ResponseEntity<Object> clientLogin(@RequestBody LoginRequest req) {
        Map<String, Object> response = new HashMap<>();
        String username = req.getUsername();
        Client client = null;

        if (username.contains("@")) {
            client = clientRepo.findByEmail(username);
        } else if (username.length() == 10) {
            client = clientRepo.findByMobile(username);
        }

        if (client == null || !passwordEncoder.matches(req.getPassword(), client.getPassword())) {
            response.put("status", "failed");
            response.put("message", "Invalid credentials");
            return ResponseEntity.badRequest().body(response);
        }

        boolean clientActive = client.isActive();
        String token = jwtutil.generateToken(client.getMobile(), client.getRole(), client.getId());
        response.put("status", "success");
        response.put("role", client.getRole());
        response.put("token", token);
        response.put("name", client.getName());
        response.put("id", client.getId());
        response.put("isTempPass", client.isTemppass());
        response.put("active", clientActive);
        response.put("isDeactivated", !clientActive);
        response.put("reconsultationRequested", Boolean.TRUE.equals(client.isReconsultationRequested()));
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN LOGIN ─────────────────────────────────────────────────────────────
    @PostMapping("/SignIn/Admin")
    public ResponseEntity<Object> adminLogin(@RequestBody LoginRequest req) {
        Map<String, Object> response = new HashMap<>();
        String username = req.getUsername();
        Admin admin = null;

        if (username.contains("@")) {
            admin = adminRepo.findByEmail(username);
        } else if (username.length() == 10) {
            admin = adminRepo.findByMobile(username);
        }

        if (admin == null || !passwordEncoder.matches(req.getPassword(), admin.getPassword())) {
            response.put("status", "failed");
            response.put("message", "Invalid credentials");
            return ResponseEntity.badRequest().body(response);
        }

        if (!admin.isActive()) {
            response.put("status", "failed");
            response.put("message", "This administrator account has been deactivated. Please contact the primary administrator.");
            return ResponseEntity.badRequest().body(response);
        }

        String token = jwtutil.generateToken(admin.getMobile(), admin.getRole(), admin.getId());
        response.put("status", "success");
        response.put("role", admin.getRole());
        response.put("token", token);
        response.put("name", admin.getName());
        response.put("id", admin.getId());
        return ResponseEntity.ok(response);
    }

    // ─── STAFF LOGIN ─────────────────────────────────────────────────────────────
    @PostMapping("/SignIn/Staff")
    public ResponseEntity<Object> staffLogin(@RequestBody LoginRequest req) {
        Map<String, Object> response = new HashMap<>();
        String username = req.getUsername();
        Staff staff = null;

        if (username.contains("@")) {
            staff = staffRepo.findByEmail(username);
        } else if (username.length() == 10) {
            staff = staffRepo.findByMobile(username);
        }

        if (staff == null || !passwordEncoder.matches(req.getPassword(), staff.getPassword())) {
            response.put("status", "failed");
            response.put("message", "Invalid credentials");
            return ResponseEntity.badRequest().body(response);
        }

        if (!staff.isActive()) {
            response.put("status", "failed");
            response.put("message", "Account is deactivated. Please contact admin.");
            return ResponseEntity.badRequest().body(response);
        }

        String token = jwtutil.generateToken(staff.getMobile(), staff.getRole(), staff.getId());
        response.put("status", "success");
        response.put("role", staff.getRole());
        response.put("token", token);
        response.put("name", staff.getName());
        response.put("id", staff.getId());
        return ResponseEntity.ok(response);
    }

    // ─── FORGOT PASSWORD: SEND OTP ───────────────────────────────────────────────
    @PostMapping("/SignIn/ForgotPassword/SendOtp")
    public ResponseEntity<Object> sendForgotPasswordOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Please provide a valid email address"));
        }
        email = email.trim().toLowerCase();

        // Check if user exists in client, admin, or staff
        boolean exists = clientRepo.findByEmail(email) != null ||
                         adminRepo.findByEmail(email) != null ||
                         staffRepo.findByEmail(email) != null;

        if (!exists) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "No account registered with this email address"));
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        OTP_STORE.put(email, new OtpRecord(otp, System.currentTimeMillis() + 10 * 60 * 1000));

        // Dispatch email
        emailService.sendOtpEmail(email, otp);

        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("message", "A 6-digit verification code has been sent to " + email);
        return ResponseEntity.ok(res);
    }

    // ─── FORGOT PASSWORD: VERIFY OTP & RESET ────────────────────────────────────
    @PostMapping("/SignIn/ForgotPassword/VerifyAndReset")
    public ResponseEntity<Object> verifyOtpAndResetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");
        String newPassword = body.get("newPassword");

        if (email == null || otp == null || newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Please provide email, valid OTP, and a password with at least 4 characters"));
        }
        email = email.trim().toLowerCase();

        OtpRecord record = OTP_STORE.get(email);
        if (record == null || !record.otp().equals(otp.trim())) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Invalid verification code. Please check your email or resend code."));
        }

        if (System.currentTimeMillis() > record.expiresAt()) {
            OTP_STORE.remove(email);
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Verification code has expired. Please request a new code."));
        }

        String encoded = passwordEncoder.encode(newPassword);
        Client client = clientRepo.findByEmail(email);
        if (client != null) {
            client.setPassword(encoded);
            client.setTemppass(false);
            clientRepo.save(client);
        } else {
            Admin admin = adminRepo.findByEmail(email);
            if (admin != null) {
                admin.setPassword(encoded);
                adminRepo.save(admin);
            } else {
                Staff staff = staffRepo.findByEmail(email);
                if (staff != null) {
                    staff.setPassword(encoded);
                    staffRepo.save(staff);
                } else {
                    return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Account not found"));
                }
            }
        }

        OTP_STORE.remove(email);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Password reset successfully! You can now log in with your new password."));
    }

    // ─── CLIENT: CHANGE PASSWORD (TEMP PASSWORD RESOLUTION) ───────────────────────
    @PostMapping("/Client/ChangePassword")
    public ResponseEntity<Object> changeClientPassword(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal AuthUser user) {

        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Password must be at least 4 characters"));
        }

        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Client account not found"));
        }

        client.setPassword(passwordEncoder.encode(newPassword));
        client.setTemppass(false);
        clientRepo.save(client);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Password changed successfully!"));
    }
}
