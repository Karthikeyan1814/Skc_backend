package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.FirmSettings;

public interface FirmSettingsRepo extends JpaRepository<FirmSettings, Long> {
}
