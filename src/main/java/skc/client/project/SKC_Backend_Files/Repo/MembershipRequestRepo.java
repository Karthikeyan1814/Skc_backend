package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.MembershipRequest;
import java.util.List;

public interface MembershipRequestRepo extends JpaRepository<MembershipRequest, Long> {
    List<MembershipRequest> findByStatus(String status);
    List<MembershipRequest> findByEmailAndStatus(String email, String status);
    long countByStatus(String status);
}
