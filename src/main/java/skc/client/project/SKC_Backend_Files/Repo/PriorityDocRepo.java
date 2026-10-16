package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skc.client.project.SKC_Backend_Files.entity.PriorityDocRequest;
import java.util.List;

@Repository
public interface PriorityDocRepo extends JpaRepository<PriorityDocRequest, Long> {
    List<PriorityDocRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);
    List<PriorityDocRequest> findAllByOrderByCreatedAtDesc();
    List<PriorityDocRequest> findByStatusOrderByCreatedAtDesc(String status);
    long countByStatus(String status);
}
