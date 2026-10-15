package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.Client;
import java.util.List;
import java.util.Optional;

public interface ClientRepo extends JpaRepository<Client, Long> {
    Client findByEmail(String email);
    Client findByMobile(String mobile);
    Optional<Client> findById(Long id);
    List<Client> findByActive(boolean active);
    List<Client> findByMembershipStatus(String membershipStatus);
    long countByActive(boolean active);
    long countByMembershipStatus(String membershipStatus);
}
