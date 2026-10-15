package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skc.client.project.SKC_Backend_Files.entity.HomePageContent;
import java.util.Optional;

@Repository
public interface HomePageContentRepo extends JpaRepository<HomePageContent, Long> {
    Optional<HomePageContent> findByPageKey(String pageKey);
}
