package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.Staff;
import java.util.Optional;

public interface StaffRepo extends JpaRepository<Staff, Long> {
    Staff findByEmail(String email);
    Staff findByMobile(String mobile);
    Optional<Staff> findById(Long id);
}
