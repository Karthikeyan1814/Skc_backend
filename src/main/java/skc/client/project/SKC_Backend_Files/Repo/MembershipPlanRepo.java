package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.MembershipPlan;
import java.util.Optional;

public interface MembershipPlanRepo extends JpaRepository<MembershipPlan, Long> {
    Optional<MembershipPlan> findByPlanTier(String planTier);
}
