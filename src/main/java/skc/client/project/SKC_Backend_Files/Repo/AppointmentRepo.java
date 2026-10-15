package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skc.client.project.SKC_Backend_Files.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepo extends JpaRepository<Appointment, Long> {
    List<Appointment> findByClientId(Long clientId);
    List<Appointment> findByStatus(String status);
    List<Appointment> findByClientIdAndStatus(Long clientId, String status);

    @Query("SELECT a FROM Appointment a WHERE (a.status = 'CONFIRMED' OR a.status = 'SCHEDULED') AND (a.reminderSent IS NULL OR a.reminderSent = false) AND a.requestedDateTime BETWEEN :start AND :end")
    List<Appointment> findUpcomingAppointmentsNeedingReminder(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
