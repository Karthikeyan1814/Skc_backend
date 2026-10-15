package skc.client.project.SKC_Backend_Files.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.AppointmentRepo;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Appointment;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class AppointmentReminderScheduler {

    @Autowired
    private AppointmentRepo appointmentRepo;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminRepo adminRepo;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    /**
     * Executes every 60 seconds.
     * Identifies confirmed appointments starting within the next 35 minutes that haven't received the 30-min reminder email.
     */
    @Scheduled(fixedRate = 60000)
    public void checkAndSendUpcomingMeetingReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(35);

        List<Appointment> upcoming = appointmentRepo.findUpcomingAppointmentsNeedingReminder(now.minusMinutes(5), windowEnd);
        if (upcoming.isEmpty()) {
            return;
        }

        System.out.println(">>> [AppointmentReminderScheduler] Found " + upcoming.size() + " appointment(s) needing 30-min reminder.");

        for (Appointment appt : upcoming) {
            try {
                String formattedTime = appt.getRequestedDateTime() != null
                        ? appt.getRequestedDateTime().format(FORMATTER)
                        : "Scheduled Timeline";

                String clientName = appt.getClientName() != null ? appt.getClientName() : "Valued Client";
                String clientEmail = appt.getClientEmail();
                String apptType = appt.getAppointmentType() != null ? appt.getAppointmentType() : "Consultation";
                String meetingLink = appt.getMeetingLink();

                // 1. Send Email Reminder to Client
                if (clientEmail != null && !clientEmail.isBlank()) {
                    emailService.sendAppointment30MinReminderEmail(
                            clientEmail,
                            clientName,
                            true,
                            clientName,
                            apptType,
                            formattedTime,
                            meetingLink,
                            appt.getAdminNote()
                    );
                }

                // 2. Send in-portal notification to Client
                if (appt.getClientId() != null) {
                    String clientMsg = "Your " + apptType + " session starts in 30 minutes (" + formattedTime + "). Meeting Link: "
                            + (meetingLink != null ? meetingLink : "Direct Desk");
                    notificationService.notifyClient(
                            appt.getClientId(),
                            "APPOINTMENT_REMINDER",
                            "⏰ Meeting in 30 Minutes",
                            clientMsg,
                            appt.getId()
                    );
                }

                // 3. Send Email Alert to Admins
                List<Admin> admins = adminRepo.findAll();
                for (Admin admin : admins) {
                    if (admin.getEmail() != null && !admin.getEmail().isBlank()) {
                        emailService.sendAppointment30MinReminderEmail(
                                admin.getEmail(),
                                admin.getName(),
                                false,
                                clientName,
                                apptType,
                                formattedTime,
                                meetingLink,
                                appt.getAdminNote()
                        );
                    }
                }

                // 4. Mark reminder as sent and persist
                appt.setReminderSent(true);
                appointmentRepo.save(appt);
                System.out.println(">>> [AppointmentReminderScheduler] Dispatched 30-min reminder for appointment ID: " + appt.getId());

            } catch (Exception e) {
                System.err.println("Error processing 30-min reminder for appt #" + appt.getId() + ": " + e.getMessage());
            }
        }
    }
}
