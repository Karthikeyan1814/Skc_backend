package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.entity.Staff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skc/Admin/Staff")
public class StaffManagementController {

    @Autowired private StaffRepo staffRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EmailService emailService;

    // ─── GET: All staff ───────────────────────────────────────────────────────────
    @GetMapping
    public ResponseEntity<List<Staff>> getAllStaff() {
        return ResponseEntity.ok(staffRepo.findAll());
    }

    // ─── GET: Staff by ID ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable Long id) {
        return staffRepo.findById(id)
                .map(s -> (Object) s)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── POST: Create staff ───────────────────────────────────────────────────────
    @PostMapping("/Create")
    public ResponseEntity<Object> createStaff(@RequestBody Staff staff) {
        Map<String, Object> response = new HashMap<>();

        if (staffRepo.findByEmail(staff.getEmail()) != null) {
            response.put("status", "failed");
            response.put("message", "Email already exists");
            return ResponseEntity.badRequest().body(response);
        }

        String rawPassword = staff.getPassword();
        staff.setPassword(passwordEncoder.encode(staff.getPassword()));
        staff.setRole("STAFF");
        staff.setActive(true);
        Staff saved = staffRepo.save(staff);

        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            System.out.println(">>> [StaffManagementController] Dispatching welcome email to staff: " + saved.getEmail());
            emailService.sendAccountCreationWelcomeEmail(
                    saved.getEmail(),
                    saved.getName() != null ? saved.getName() : "Staff Member",
                    saved.getEmail(),
                    rawPassword,
                    "STAFF"
            );
        }

        response.put("status", "success");
        response.put("message", "Staff created successfully");
        response.put("id", saved.getId());
        return ResponseEntity.ok(response);
    }

    // ─── PUT: Update staff ────────────────────────────────────────────────────────
    @PutMapping("/Update/{id}")
    public ResponseEntity<Object> updateStaff(@PathVariable Long id, @RequestBody Staff updatedStaff) {
        Map<String, Object> response = new HashMap<>();

        Staff staff = staffRepo.findById(id).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            response.put("message", "Staff not found");
            return ResponseEntity.badRequest().body(response);
        }

        if (updatedStaff.getName() != null) staff.setName(updatedStaff.getName());
        if (updatedStaff.getEmail() != null) staff.setEmail(updatedStaff.getEmail());
        if (updatedStaff.getMobile() != null) staff.setMobile(updatedStaff.getMobile());
        if (updatedStaff.getDesignation() != null) staff.setDesignation(updatedStaff.getDesignation());

        staffRepo.save(staff);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // ─── POST: Reset staff password ───────────────────────────────────────────────
    @PostMapping("/ResetPassword/{id}")
    public ResponseEntity<Object> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        Staff staff = staffRepo.findById(id).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
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
        response.put("message", "Password reset successfully");
        return ResponseEntity.ok(response);
    }

    // ─── POST: Toggle staff active status ────────────────────────────────────────
    @PostMapping("/ToggleActive/{id}")
    public ResponseEntity<Object> toggleActive(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Staff staff = staffRepo.findById(id).orElse(null);
        if (staff == null) {
            response.put("status", "failed");
            return ResponseEntity.badRequest().body(response);
        }

        staff.setActive(!staff.isActive());
        staffRepo.save(staff);

        response.put("status", "success");
        response.put("active", staff.isActive());
        response.put("message", staff.isActive() ? "Staff activated" : "Staff deactivated");
        return ResponseEntity.ok(response);
    }

    // ─── DELETE: Remove staff ─────────────────────────────────────────────────────
    @DeleteMapping("/Delete/{id}")
    public ResponseEntity<Object> deleteStaff(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (!staffRepo.existsById(id)) {
            response.put("status", "failed");
            return ResponseEntity.badRequest().body(response);
        }
        staffRepo.deleteById(id);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }
}
