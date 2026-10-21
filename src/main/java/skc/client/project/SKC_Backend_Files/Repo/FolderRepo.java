package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skc.client.project.SKC_Backend_Files.entity.FolderEntity;

import java.util.List;

public interface FolderRepo extends JpaRepository<FolderEntity, Long> {

    List<FolderEntity> findAllByOrderByCreatedAtDesc();

    List<FolderEntity> findByCategoryOrderByCreatedAtDesc(String category);

    List<FolderEntity> findByIsCommonTrueOrderByCreatedAtDesc();

    List<FolderEntity> findByClientIdOrderByCreatedAtDesc(Long clientId);

    @Query("SELECT f FROM FolderEntity f WHERE f.isCommon = true OR (f.client IS NOT NULL AND f.client.id = :clientId) ORDER BY f.isCommon DESC, f.folderName ASC")
    List<FolderEntity> findAvailableForClient(@Param("clientId") Long clientId);
}
