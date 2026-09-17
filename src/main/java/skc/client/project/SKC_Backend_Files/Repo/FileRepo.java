package skc.client.project.SKC_Backend_Files.Repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import skc.client.project.SKC_Backend_Files.entity.FileEntity;

public interface FileRepo  extends JpaRepository<FileEntity, Long>{
//
//	@Override
//	Optional<FileEntity> findById(Long id);
}
