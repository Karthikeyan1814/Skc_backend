package skc.client.project.SKC_Backend_Files.Controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Security.JwtUtil;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.dto.LoginRequest;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;

@RestController
@RequestMapping("/skc")
public class AuthLoginController {
	
	@Autowired
	private JwtUtil jwtutil;

	@Autowired
	private AdminRepo adminRepo;
	
	@Autowired
	private ClientRepo clientRepo;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	
	@PostMapping("/SignIn/Client")
	public ResponseEntity<Object> clientLogin(@RequestBody LoginRequest loginreq){
		
		String Username = loginreq.getUsername();
		Map<String , Object> response=new HashMap<String , Object>();
		
		
		
		if(Username.endsWith(".com")) {
			
			Client clientDetail = clientRepo.findByEmail(loginreq.getUsername());
			if((clientDetail == null) || (!passwordEncoder.matches(loginreq.getPassword(), clientDetail.getPassword()))){
				 response.put("status","failed");
				 response.put("message", "Invalid Username or Password ");
				 return ResponseEntity.badRequest().body(response);
			}
			String UsernameAsMobile = clientDetail.getMobile();
			 String token= jwtutil.generateToken(UsernameAsMobile, clientDetail.getRole(), clientDetail.getId());
			 response.put("status", "success");
			 response.put("role", clientDetail.getRole());
			 response.put("token", token);
			 return ResponseEntity.ok(response);
			
		}else if((!Username.endsWith(".com"))&&(Username.length()==10)) {

			Client clientDetail = clientRepo.findByMobile(loginreq.getUsername());
			if((clientDetail == null) || (!passwordEncoder.matches(loginreq.getPassword(), clientDetail.getPassword()))){
				 response.put("status","failed");
				 response.put("message", "Invalid Username or Password ");
				 return ResponseEntity.badRequest().body(response);
			}
			String UsernameAsMobile = clientDetail.getMobile();
			 String token= jwtutil.generateToken(UsernameAsMobile, clientDetail.getRole(),clientDetail.getId());
			 response.put("status", "success");
			 response.put("role", clientDetail.getRole());
			 response.put("token", token);
			 return ResponseEntity.ok(response);
		}
		
	 response.put("status", "failed");
	 return ResponseEntity.badRequest().body(response);
		
	}
	
	@PostMapping("/SignIn/Admin")
	public ResponseEntity<Object> adminLogin(@RequestBody LoginRequest loginreq){
		
		String Username = loginreq.getUsername();
		Map<String , Object> response=new HashMap<String , Object>();
		
		
		
		if(Username.endsWith(".com")) {
			
			Admin adminDetail= adminRepo.findByEmail(loginreq.getUsername());
			if((adminDetail == null) || (!passwordEncoder.matches(loginreq.getPassword(), adminDetail.getPassword()))){
				 response.put("status","failed");
				 response.put("message", "Invalid Username or Password ");
				 return ResponseEntity.badRequest().body(response);
			}
			String UsernameAsMobile = adminDetail.getMobile();
			 String token= jwtutil.generateToken(UsernameAsMobile, adminDetail.getRole(), adminDetail.getId());
			 response.put("status", "success");
			 response.put("role", adminDetail.getRole());
			 response.put("token", token);
			 return ResponseEntity.ok(response);
			
		}else if((!Username.endsWith(".com"))&&(Username.length()==10)) {

			Admin adminDetail= adminRepo.findByMobile(loginreq.getUsername());
			if((adminDetail == null) || (!passwordEncoder.matches(loginreq.getPassword(), adminDetail.getPassword()))){
				 response.put("status","failed");
				 response.put("message", "Invalid Username or Password ");
				 return ResponseEntity.badRequest().body(response);
			}
			String UsernameAsMobile = adminDetail.getMobile();
			 String token= jwtutil.generateToken(UsernameAsMobile, adminDetail.getRole(),adminDetail.getId());
			 response.put("status", "success");
			 response.put("role", adminDetail.getRole());
			 response.put("token", token);
			 return ResponseEntity.ok(response);
		}
		
	 response.put("status", "failed");
	 response.put("message", "Invalid Username or Password ");
	 return ResponseEntity.badRequest().body(response);
		
	}
}
