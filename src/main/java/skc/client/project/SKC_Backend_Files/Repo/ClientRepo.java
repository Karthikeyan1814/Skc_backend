package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import skc.client.project.SKC_Backend_Files.entity.Client;

public interface ClientRepo extends JpaRepository<Client, Integer> {

	Client findByEmail(String email);
}
