package skc.client.project.SKC_Backend_Files;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkcBackendFilesApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkcBackendFilesApplication.class, args);
	}

}
