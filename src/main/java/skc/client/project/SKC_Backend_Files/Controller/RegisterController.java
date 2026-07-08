package skc.client.project.SKC_Backend_Files.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.entity.Client;

@RestController
@RequestMapping("/skc")
public class RegisterController {
	
	
	private final PasswordEncoder passwordencoder;
	
	@Autowired
	private ClientRepo clientRepo;
	
	public RegisterController(PasswordEncoder encode) {
		passwordencoder=encode;
	}
	
	@PostMapping("/register")
	public ResponseEntity<Object> registerClient(@RequestBody Client client){
		client.setPassword(passwordencoder.encode(client.getPassword()));
		client.setRole("ROLE");
		clientRepo.save(client);
		Map<String,Object> response=new HashMap<String, Object>();
		response.put("status", "success");
		response.put("Message", "Registation Successfully !!!");
		return ResponseEntity.ok(response);
	}
}
