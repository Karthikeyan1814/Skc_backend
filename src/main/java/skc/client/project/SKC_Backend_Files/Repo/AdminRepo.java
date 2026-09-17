package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import skc.client.project.SKC_Backend_Files.entity.Admin;

public interface AdminRepo extends JpaRepository<Admin, Long>{

	 Admin findByEmail(String email);
	 Admin findByMobile(String mobile);
}
