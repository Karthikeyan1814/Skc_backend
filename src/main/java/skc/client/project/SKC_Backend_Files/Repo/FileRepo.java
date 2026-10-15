package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import skc.client.project.SKC_Backend_Files.entity.FileEntity;
import java.util.List;

public interface FileRepo extends JpaRepository<FileEntity, Long> {

    List<FileEntity> findByClientId(Long clientId);

    List<FileEntity> findByClientIdAndCategory(Long clientId, String category);

    List<FileEntity> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<FileEntity> findByUserEmailOrUserMobile(String email, String mobile);

    List<FileEntity> findByDirection(String direction);

    List<FileEntity> findByClientIdAndDirection(Long clientId, String direction);

    List<FileEntity> findByExtensionRequestedTrue();

    List<FileEntity> findAllByOrderByCreatedAtDesc();

    List<FileEntity> findByCategoryOrderByCreatedAtDesc(String category);
}
