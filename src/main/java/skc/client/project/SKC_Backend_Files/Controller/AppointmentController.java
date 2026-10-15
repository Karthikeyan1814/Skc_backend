package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.AppointmentRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Appointment;
import skc.client.project.SKC_Backend_Files.entity.Client;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/skc")
public class AppointmentController {

    @Autowired private AppointmentRepo appointmentRepo;
    @Autowired private ClientRepo clientRepo;
    @Autowired private AdminRepo adminRepo;
    @Autowired private StaffRepo staffRepo;
    @Autowired private NotificationService notificationService;
    @Autowired private EmailService emailService;

    // ─── CLIENT: Book an appointment ─────────────────────────────────────────────
    @PostMapping({"/Client/Appointment/Book", "/Client/Appointments/Book"})
    public ResponseEntity<Object> bookAppointment(
            @RequestBody Appointment appointment,
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
            response.put("message", "Client profile not found for user ID: " + user.getId());
            return ResponseEntity.badRequest().body(response);
        }

        appointment.setClientId(user.getId());
        appointment.setClientName(client.getName());
        appointment.setClientEmail(client.getEmail());
        appointment.setClientMobile(client.getMobile());
        if (appointment.getStatus() == null || appointment.getStatus().isEmpty()) {
            appointment.setStatus("PENDING");
        }

        Appointment saved = appointmentRepo.save(appointment);

        // 1. Instant in-portal notification
        try {
            String apptType = saved.getAppointmentType() != null ? saved.getAppointmentType() : "Consultation";
            String msg = client.getName() + " has booked a " + apptType + " appointment";
            notificationService.notifyAllAdmins("APPOINTMENT_REQUEST", "New Appointment Request", msg, saved.getId());
            notificationService.notifyAllStaff("APPOINTMENT_REQUEST", "New Appointment Request", msg, saved.getId());
        } catch (Exception e) {
            System.err.println("In-portal notification error: " + e.getMessage());
        }

        // 2. Dispatch real emails asynchronously in background thread so HTTP response returns instantly
        CompletableFuture.runAsync(() -> {
            try {
                String apptType = saved.getAppointmentType() != null ? saved.getAppointmentType() : "Consultation";
                String reqTime = saved.getRequestedDateTime() != null
                        ? saved.getRequestedDateTime().toString().replace("T", " ")
                        : "Flexible timeline";

                // Collect distinct recipient emails to prevent duplicates
                Set<String> sentEmails = new HashSet<>();

                // Email alert to Admins (distinct valid emails only)
                for (var admin : adminRepo.findAll()) {
                    String email = admin.getEmail();
                    if (isValidRealEmail(email) && sentEmails.add(email.trim().toLowerCase())) {
                        emailService.sendNewAppointmentAlertToStaffAndAdmin(
                                email.trim(),
                                admin.getName(),
                                client.getName(),
                                client.getEmail(),
                                client.getMobile(),
                                apptType,
                                reqTime,
                                saved.getClientNote()
                        );
                    }
                }

                // Email alert to Staff (distinct valid emails only)
                for (var staff : staffRepo.findAll()) {
                    String email = staff.getEmail();
                    if (isValidRealEmail(email) && sentEmails.add(email.trim().toLowerCase())) {
                        emailService.sendNewAppointmentAlertToStaffAndAdmin(
                                email.trim(),
                                staff.getName(),
                                client.getName(),
                                client.getEmail(),
                                client.getMobile(),
                                apptType,
                                reqTime,
                                saved.getClientNote()
                        );
                    }
                }
            } catch (Exception e) {
                System.err.println("Background appointment notification email error: " + e.getMessage());
            }
        });

        response.put("status", "success");
        response.put("message", "Appointment booked successfully");
        response.put("appointmentId", saved.getId());
        return ResponseEntity.ok(response);
    }

    // ─── CLIENT: Get my appointments ─────────────────────────────────────────────
    @GetMapping({"/Client/Appointments", "/Client/Appointment/List"})
    public ResponseEntity<List<Appointment>> getClientAppointments(@AuthenticationPrincipal AuthUser user) {
        if (user == null || user.getId() == null) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(appointmentRepo.findByClientId(user.getId()));
    }

    // ─── CLIENT: Cancel appointment ──────────────────────────────────────────────
    @PostMapping({"/Client/Appointment/{id}/Cancel", "/Client/Appointments/{id}/Cancel"})
    public ResponseEntity<Object> cancelAppointment(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();

        if (user == null || user.getId() == null) {
            response.put("status", "failed");
            response.put("message", "Unauthorized");
            return ResponseEntity.status(401).body(response);
        }

        Appointment appt = appointmentRepo.findById(id).orElse(null);
        if (appt == null || !appt.getClientId().equals(user.getId())) {
            response.put("status", "failed");
            response.put("message", "Appointment not found");
            return ResponseEntity.badRequest().body(response);
        }

        appt.setStatus("CANCELLED");
        appointmentRepo.save(appt);
        dispatchAppointmentCancelledEmail(appt);

        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN/STAFF: Get all appointments ───────────────────────────────────────
    @GetMapping({"/Admin/Appointments", "/Admin/Appointment/List"})
    public ResponseEntity<List<Appointment>> getAllAppointments(
            @RequestParam(required = false) String status) {
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            return ResponseEntity.ok(appointmentRepo.findByStatus(status.toUpperCase()));
        }
        return ResponseEntity.ok(appointmentRepo.findAll());
    }

    @GetMapping({"/Staff/Appointments", "/Staff/Appointment/List"})
    public ResponseEntity<List<Appointment>> getAllAppointmentsStaff(
            @RequestParam(required = false) String status) {
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            return ResponseEntity.ok(appointmentRepo.findByStatus(status.toUpperCase()));
        }
        return ResponseEntity.ok(appointmentRepo.findAll());
    }

    // ─── ADMIN/STAFF: Confirm appointment + set meeting link ─────────────────────
    @PostMapping({
            "/Admin/Appointment/{id}/Confirm",
            "/Admin/Appointments/{id}/Confirm",
            "/Admin/Appointments/Schedule/{id}",
            "/Admin/Appointment/Schedule/{id}"
    })
    public ResponseEntity<Object> confirmAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();

        Appointment appt = appointmentRepo.findById(id).orElse(null);
        if (appt == null) {
            response.put("status", "failed");
            response.put("message", "Appointment not found");
            return ResponseEntity.badRequest().body(response);
        }

        appt.setStatus("CONFIRMED");
        if (body != null) {
            if (body.containsKey("adminNote") && body.get("adminNote") != null) {
                appt.setAdminNote(body.get("adminNote"));
            }
            if (body.containsKey("meetingLink") && body.get("meetingLink") != null) {
                appt.setMeetingLink(body.get("meetingLink"));
            }
        }
        appointmentRepo.save(appt);

        // Notify client via in-portal notification & official approval email (async)
        try {
            String apptType = appt.getAppointmentType() != null ? appt.getAppointmentType() : "Consultation";
            String msg = "Your " + apptType + " appointment has been confirmed";
            if (appt.getMeetingLink() != null && !appt.getMeetingLink().isEmpty()) {
                msg += ". Meeting link: " + appt.getMeetingLink();
            }
            if (appt.getClientId() != null) {
                notificationService.notifyClient(appt.getClientId(), "APPOINTMENT_CONFIRMED", "Appointment Confirmed", msg, appt.getId());
            }

            // Real email to client dispatched asynchronously
            if (appt.getClientEmail() != null && !appt.getClientEmail().isBlank()) {
                final Appointment confirmedAppt = appt;
                final String finalApptType = apptType;
                CompletableFuture.runAsync(() -> {
                    try {
                        String timeStr = confirmedAppt.getRequestedDateTime() != null
                                ? confirmedAppt.getRequestedDateTime().toString().replace("T", " ")
                                : "Confirmed Schedule";
                        emailService.sendAppointmentConfirmedEmailToClient(
                                confirmedAppt.getClientEmail(),
                                confirmedAppt.getClientName() != null ? confirmedAppt.getClientName() : "Valued Client",
                                finalApptType,
                                timeStr,
                                confirmedAppt.getMeetingLink(),
                                confirmedAppt.getAdminNote()
                        );
                    } catch (Exception e) {
                        System.err.println("Async appointment confirmation email error: " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to notify client: " + e.getMessage());
        }

        response.put("status", "success");
        response.put("message", "Appointment confirmed successfully");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN/STAFF: Update status (CONFIRMED, COMPLETED, CANCELLED) ─────────────
    @PostMapping({"/Admin/Appointment/Status/{id}", "/Admin/Appointments/Status/{id}"})
    public ResponseEntity<Object> updateStatus(
            @PathVariable Long id,
            @RequestParam(required = false) String status,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();
        Appointment appt = appointmentRepo.findById(id).orElse(null);
        if (appt == null) {
            response.put("status", "failed");
            response.put("message", "Appointment not found");
            return ResponseEntity.badRequest().body(response);
        }

        String newStatus = status;
        if (newStatus == null && body != null && body.containsKey("status")) {
            newStatus = body.get("status");
        }
        if (newStatus == null || newStatus.isEmpty()) {
            newStatus = "CONFIRMED";
        }
        appt.setStatus(newStatus.toUpperCase());
        if (body != null) {
            if (body.containsKey("adminNote") && body.get("adminNote") != null) {
                appt.setAdminNote(body.get("adminNote"));
            }
            if (body.containsKey("meetingLink") && body.get("meetingLink") != null) {
                appt.setMeetingLink(body.get("meetingLink"));
            }
        }
        appointmentRepo.save(appt);

        try {
            if ("COMPLETED".equalsIgnoreCase(newStatus)) {
                if (appt.getClientId() != null) {
                    notificationService.notifyClient(appt.getClientId(), "APPOINTMENT_COMPLETED", "Consultation Completed",
                            "Your consultation session has been marked as completed. Thank you!", appt.getId());
                }
            } else if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                if (appt.getClientId() != null) {
                    notificationService.notifyClient(appt.getClientId(), "APPOINTMENT_CANCELLED", "Appointment Cancelled",
                            "Your appointment has been cancelled.", appt.getId());
                }
                dispatchAppointmentCancelledEmail(appt);
            }
        } catch (Exception ignored) {}

        response.put("status", "success");
        response.put("message", "Appointment status updated to " + appt.getStatus());
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Cancel/reject appointment ─────────────────────────────────────────
    @PostMapping({"/Admin/Appointment/{id}/Cancel", "/Admin/Appointments/{id}/Cancel"})
    public ResponseEntity<Object> adminCancelAppointment(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {

        Map<String, Object> response = new HashMap<>();

        Appointment appt = appointmentRepo.findById(id).orElse(null);
        if (appt == null) {
            response.put("status", "failed");
            response.put("message", "Appointment not found");
            return ResponseEntity.badRequest().body(response);
        }

        appt.setStatus("CANCELLED");
        if (body != null && body.containsKey("adminNote") && body.get("adminNote") != null) {
            appt.setAdminNote(body.get("adminNote"));
        }
        appointmentRepo.save(appt);

        try {
            String reason = appt.getAdminNote() != null && !appt.getAdminNote().isEmpty()
                    ? " Reason: " + appt.getAdminNote()
                    : "";
            if (appt.getClientId() != null) {
                notificationService.notifyClient(appt.getClientId(), "APPOINTMENT_CANCELLED", "Appointment Cancelled",
                        "Your appointment has been cancelled." + reason, appt.getId());
            }
            // Real cancellation email to client
            dispatchAppointmentCancelledEmail(appt);
        } catch (Exception e) {
            System.err.println("Failed to notify client: " + e.getMessage());
        }

        response.put("status", "success");
        response.put("message", "Appointment cancelled");
        return ResponseEntity.ok(response);
    }

    // ─── PUBLIC: Unregistered Visitor Book Appointment ───────────────────────────
    @PostMapping({"/Public/Appointment/Book", "/Public/Appointments/Book"})
    public ResponseEntity<Object> publicBookAppointment(@RequestBody Appointment appointment) {
        Map<String, Object> response = new HashMap<>();

        if (appointment.getClientName() == null || appointment.getClientName().trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Full name is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (appointment.getClientEmail() == null || appointment.getClientEmail().trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Email address is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (appointment.getClientMobile() == null || appointment.getClientMobile().trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "Mobile number is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (appointment.getRequestedDateTime() == null) {
            response.put("status", "failed");
            response.put("message", "Preferred date and time is required");
            return ResponseEntity.badRequest().body(response);
        }

        appointment.setClientId(null);
        if (appointment.getAppointmentType() == null || appointment.getAppointmentType().trim().isEmpty()) {
            appointment.setAppointmentType("ONLINE");
        }
        appointment.setStatus("PENDING");

        Appointment saved = appointmentRepo.save(appointment);

        // 1. In-portal alerts to admins & staff
        try {
            String apptType = saved.getAppointmentType();
            String msg = "Website Visitor " + saved.getClientName() + " (" + saved.getClientMobile() + ") booked a " + apptType + " appointment";
            notificationService.notifyAllAdmins("GUEST_APPOINTMENT_REQUEST", "New Website Guest Appointment", msg, saved.getId());
            notificationService.notifyAllStaff("GUEST_APPOINTMENT_REQUEST", "New Website Guest Appointment", msg, saved.getId());
        } catch (Exception e) {
            System.err.println("In-portal guest notification error: " + e.getMessage());
        }

        // 2. Dispatch emails asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                String apptType = saved.getAppointmentType();
                String reqTime = saved.getRequestedDateTime() != null
                        ? saved.getRequestedDateTime().toString().replace("T", " ")
                        : "Requested Slot";

                Set<String> sentEmails = new HashSet<>();

                for (var admin : adminRepo.findAll()) {
                    String email = admin.getEmail();
                    if (isValidRealEmail(email) && sentEmails.add(email.trim().toLowerCase())) {
                        emailService.sendNewAppointmentAlertToStaffAndAdmin(
                                email.trim(),
                                admin.getName(),
                                saved.getClientName() + " (Website Guest)",
                                saved.getClientEmail(),
                                saved.getClientMobile(),
                                apptType,
                                reqTime,
                                saved.getClientNote()
                        );
                    }
                }

                for (var staff : staffRepo.findAll()) {
                    String email = staff.getEmail();
                    if (isValidRealEmail(email) && sentEmails.add(email.trim().toLowerCase())) {
                        emailService.sendNewAppointmentAlertToStaffAndAdmin(
                                email.trim(),
                                staff.getName(),
                                saved.getClientName() + " (Website Guest)",
                                saved.getClientEmail(),
                                saved.getClientMobile(),
                                apptType,
                                reqTime,
                                saved.getClientNote()
                        );
                    }
                }

                if (isValidRealEmail(saved.getClientEmail())) {
                    emailService.sendGuestAppointmentAcknowledgementEmail(
                            saved.getClientEmail().trim(),
                            saved.getClientName(),
                            apptType,
                            reqTime
                    );
                }
            } catch (Exception e) {
                System.err.println("Background guest appointment email error: " + e.getMessage());
            }
        });

        response.put("status", "success");
        response.put("message", "Consultation request submitted successfully. Our office will contact you soon.");
        response.put("appointmentId", saved.getId());
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN/STAFF: Get appointment by ID ──────────────────────────────────────
    @GetMapping({"/Admin/Appointment/{id}", "/Admin/Appointments/{id}"})
    public ResponseEntity<Object> getById(@PathVariable Long id) {
        return appointmentRepo.findById(id)
                .map(a -> (Object) a)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── CLIENT: Delete appointment ──────────────────────────────────────────────
    @DeleteMapping({"/Client/Appointment/{id}", "/Client/Appointments/{id}"})
    @PostMapping({"/Client/Appointment/{id}/Delete", "/Client/Appointments/{id}/Delete"})
    public ResponseEntity<Object> deleteClientAppointment(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        return appointmentRepo.findById(id).map(appt -> {
            if (user != null && user.getId() != null && appt.getClientId() != null
                    && !appt.getClientId().equals(user.getId())) {
                Map<String, Object> err = new HashMap<>();
                err.put("status", "error");
                err.put("message", "Unauthorized to delete this consultation appointment.");
                return ResponseEntity.status(403).body((Object) err);
            }
            appointmentRepo.delete(appt);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Appointment deleted successfully");
            return ResponseEntity.ok((Object) res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── ADMIN: Delete appointment ───────────────────────────────────────────────
    @DeleteMapping({"/Admin/Appointment/{id}", "/Admin/Appointments/{id}"})
    @PostMapping({"/Admin/Appointment/{id}/Delete", "/Admin/Appointments/{id}/Delete"})
    public ResponseEntity<Object> deleteAdminAppointment(@PathVariable Long id) {
        return appointmentRepo.findById(id).map(appt -> {
            appointmentRepo.delete(appt);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Appointment deleted permanently");
            return ResponseEntity.ok((Object) res);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── STAFF: Delete appointment ───────────────────────────────────────────────
    @DeleteMapping({"/Staff/Appointment/{id}", "/Staff/Appointments/{id}"})
    @PostMapping({"/Staff/Appointment/{id}/Delete", "/Staff/Appointments/{id}/Delete"})
    public ResponseEntity<Object> deleteStaffAppointment(@PathVariable Long id) {
        return appointmentRepo.findById(id).map(appt -> {
            appointmentRepo.delete(appt);
            Map<String, Object> res = new HashMap<>();
            res.put("status", "success");
            res.put("message", "Appointment deleted successfully");
            return ResponseEntity.ok((Object) res);
        }).orElse(ResponseEntity.notFound().build());
    }

    private void dispatchAppointmentCancelledEmail(Appointment appt) {
        if (appt == null || appt.getClientEmail() == null || appt.getClientEmail().isBlank()) {
            return;
        }
        String email = appt.getClientEmail().trim();
        if (!isValidRealEmail(email)) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                String timeStr = appt.getRequestedDateTime() != null
                        ? appt.getRequestedDateTime().toString().replace("T", " ")
                        : "Requested Slot";
                String apptType = appt.getAppointmentType() != null ? appt.getAppointmentType() : "Consultation";
                emailService.sendAppointmentCancelledEmailToClient(
                        email,
                        appt.getClientName() != null ? appt.getClientName() : "Valued Client",
                        apptType,
                        timeStr,
                        appt.getAdminNote()
                );
            } catch (Exception e) {
                System.err.println("Async appointment cancellation email error: " + e.getMessage());
            }
        });
    }

    private boolean isValidRealEmail(String email) {
        if (email == null || email.isBlank()) return false;
        String e = email.trim().toLowerCase();
        // Ignore fake dummy test domains that bounce
        if (e.endsWith("@srikathiravan.com") || e.contains("testadmin") || e.contains("example.com") || !e.contains("@") || !e.contains(".")) {
            return false;
        }
        return true;
    }
}
