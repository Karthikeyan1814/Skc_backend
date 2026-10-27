package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.*;
import skc.client.project.SKC_Backend_Files.entity.Client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/skc/Admin")
public class AdminDashboardController {

    @Autowired private ClientRepo clientRepo;
    @Autowired private StaffRepo staffRepo;
    @Autowired private MembershipRequestRepo membershipRequestRepo;
    @Autowired private AppointmentRepo appointmentRepo;
    @Autowired private FileRepo fileRepo;

    // ─── Dashboard stats overview ─────────────────────────────────────────────────
    @GetMapping("/Dashboard/Stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalClients", clientRepo.count());
        stats.put("activeClients", clientRepo.countByActive(true));
        stats.put("memberClients", clientRepo.countByMembershipStatus("MEMBER"));
        stats.put("totalStaff", staffRepo.count());
        stats.put("activeStaff", staffRepo.findAll().stream().filter(s -> s.isActive()).count());
        stats.put("pendingMemberships", membershipRequestRepo.findByStatus("PENDING").size());
        stats.put("pendingAppointments", appointmentRepo.findByStatus("PENDING").size());
        stats.put("totalFiles", fileRepo.count());

        return ResponseEntity.ok(stats);
    }

    // ─── Get all clients ──────────────────────────────────────────────────────────
    @GetMapping("/Clients")
    public ResponseEntity<List<Client>> getAllClients(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String membershipStatus) {

        List<Client> clients;
        if (active != null) {
            clients = clientRepo.findByActive(active);
        } else if (membershipStatus != null) {
            clients = clientRepo.findByMembershipStatus(membershipStatus);
        } else {
            clients = clientRepo.findAll();
        }
        return ResponseEntity.ok(clients);
    }

    // ─── Get client by ID ─────────────────────────────────────────────────────────
    @GetMapping("/Client/{id}")
    public ResponseEntity<Object> getClientById(@PathVariable Long id) {
        return clientRepo.findById(id)
                .map(c -> (Object) c)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Toggle client active ─────────────────────────────────────────────────────
    @PostMapping("/Client/ToggleActive/{id}")
    public ResponseEntity<Object> toggleClientActive(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Client client = clientRepo.findById(id).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            return ResponseEntity.badRequest().body(response);
        }
        client.setActive(!client.isActive());
        clientRepo.save(client);
        response.put("status", "success");
        response.put("active", client.isActive());
        return ResponseEntity.ok(response);
    }

    // ─── Update client membership status ─────────────────────────────────────────
    @PostMapping("/Client/UpdateMembership/{id}")
    public ResponseEntity<Object> updateMembership(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        Client client = clientRepo.findById(id).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            return ResponseEntity.badRequest().body(response);
        }
        client.setMembershipStatus(body.getOrDefault("membershipStatus", "NONE"));
        clientRepo.save(client);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // ─── Delete client (Admin only - Staff forbidden) ───────────────────────────
    @DeleteMapping({"/Client/Delete/{id}", "/skc/Client/Delete/{id}"})
    public ResponseEntity<Object> deleteClient(@PathVariable Long id) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "forbidden");
            err.put("message", "Staff members are not permitted to delete client accounts. Contact an administrator.");
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN).body(err);
        }

        Map<String, Object> response = new HashMap<>();
        if (!clientRepo.existsById(id)) {
            response.put("status", "failed");
            response.put("message", "Client record not found");
            return ResponseEntity.badRequest().body(response);
        }
        clientRepo.deleteById(id);
        response.put("status", "success");
        response.put("message", "Client deleted successfully");
        return ResponseEntity.ok(response);
    }

    // ─── Update client profile and custom fields ──────────────────────────────────
    @PutMapping("/Client/Update/{id}")
    public ResponseEntity<Object> updateClient(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();
        Client client = clientRepo.findById(id).orElse(null);
        if (client == null) {
            response.put("status", "failed");
            response.put("message", "Client not found");
            return ResponseEntity.badRequest().body(response);
        }

        if (body.containsKey("name") && body.get("name") != null) client.setName(body.get("name").toString().trim());
        if (body.containsKey("legalname") && body.get("legalname") != null) client.setLegalname(body.get("legalname").toString().trim());
        if (body.containsKey("mobile") && body.get("mobile") != null) client.setMobile(body.get("mobile").toString().trim());
        if (body.containsKey("email") && body.get("email") != null) client.setEmail(body.get("email").toString().trim().toLowerCase());
        if (body.containsKey("gstnumber")) client.setGstnumber(body.get("gstnumber") != null ? body.get("gstnumber").toString().trim().toUpperCase() : null);
        if (body.containsKey("pannumber")) client.setPannumber(body.get("pannumber") != null ? body.get("pannumber").toString().trim().toUpperCase() : null);
        if (body.containsKey("location")) client.setLocation(body.get("location") != null ? body.get("location").toString().trim() : null);
        if (body.containsKey("membershipStatus") && body.get("membershipStatus") != null) client.setMembershipStatus(body.get("membershipStatus").toString());

        if (body.containsKey("formversion") && body.get("formversion") != null && !body.get("formversion").toString().isBlank()) {
            try {
                client.setFormversion(Integer.parseInt(body.get("formversion").toString().trim()));
            } catch (Exception ignored) {}
        }

        if (body.containsKey("customFields") && body.get("customFields") != null) {
            Object cf = body.get("customFields");
            if (cf instanceof String) {
                client.setCustomFields((String) cf);
            } else if (cf instanceof Map) {
                StringBuilder sb = new StringBuilder("{");
                Map<?, ?> map = (Map<?, ?>) cf;
                int i = 0;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (i++ > 0) sb.append(",");
                    sb.append("\"").append(entry.getKey()).append("\":");
                    Object val = entry.getValue();
                    if (val == null) {
                        sb.append("null");
                    } else if (val instanceof Number || val instanceof Boolean) {
                        sb.append(val);
                    } else {
                        sb.append("\"").append(val.toString().replace("\"", "\\\"")).append("\"");
                    }
                }
                sb.append("}");
                client.setCustomFields(sb.toString());
            } else {
                client.setCustomFields(cf.toString());
            }
        }

        Client updated = clientRepo.save(client);
        response.put("status", "success");
        response.put("message", "Client updated successfully!");
        response.put("client", updated);
        return ResponseEntity.ok(response);
    }
}
