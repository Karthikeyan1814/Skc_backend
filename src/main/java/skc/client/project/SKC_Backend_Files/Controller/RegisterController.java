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

import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.Staff;

@RestController
@RequestMapping("/skc")
public class RegisterController {
	
	private final PasswordEncoder passwordencoder;
	
	@Autowired
	private ClientRepo clientRepo;
	
	@Autowired
	private AdminRepo adminRepo;

	@Autowired
	private StaffRepo staffRepo;

	@Autowired
	private EmailService emailService;
	
	public RegisterController(PasswordEncoder encode) {
		passwordencoder=encode;
	}
	
	@PostMapping("/register/Client")
	public ResponseEntity<Object> registerClient(@RequestBody Map<String, Object> body){
		Map<String, Object> response = new HashMap<>();

		String name = body.get("name") != null ? body.get("name").toString().trim() : "";
		String mobile = body.get("mobile") != null ? body.get("mobile").toString().trim() : "";
		String email = body.get("email") != null ? body.get("email").toString().trim().toLowerCase() : "";
		String legalname = body.get("legalname") != null ? body.get("legalname").toString().trim() : "";
		String gstnumber = body.get("gstnumber") != null ? body.get("gstnumber").toString().trim().toUpperCase() : "";
		String pannumber = body.get("pannumber") != null ? body.get("pannumber").toString().trim().toUpperCase() : "";
		String location = body.get("location") != null ? body.get("location").toString().trim() : "";
		String password = body.get("password") != null ? body.get("password").toString().trim() : "";

		if (name.isEmpty()) {
			response.put("status", "failed");
			response.put("message", "Client name is required");
			return ResponseEntity.badRequest().body(response);
		}

		if (mobile.length() < 10) {
			response.put("status", "failed");
			response.put("message", "A valid 10-digit mobile number is required");
			return ResponseEntity.badRequest().body(response);
		}

		if (!email.isEmpty() && clientRepo.findByEmail(email) != null) {
			response.put("status", "failed");
			response.put("message", "A client with this email address already exists.");
			return ResponseEntity.badRequest().body(response);
		}

		if (clientRepo.findByMobile(mobile) != null) {
			response.put("status", "failed");
			response.put("message", "A client with this mobile number already exists.");
			return ResponseEntity.badRequest().body(response);
		}

		if (password.isEmpty()) {
			password = mobile.substring(mobile.length() - 4) + "2026";
		}

		Client client = new Client();
		client.setName(name);
		client.setMobile(mobile);
		client.setEmail(email.isEmpty() ? null : email);
		client.setLegalname(legalname.isEmpty() ? name : legalname);
		client.setGstnumber(gstnumber.isEmpty() ? null : gstnumber);
		client.setPannumber(pannumber.isEmpty() ? null : pannumber);
		client.setLocation(location.isEmpty() ? null : location);
		client.setPassword(passwordencoder.encode(password));
		client.setRole("CLIENT");
		client.setTemppass(true);
		client.setActive(true);
		client.setMembershipStatus("NONE");

		if (body.get("formversion") != null && !body.get("formversion").toString().isBlank()) {
			try {
				client.setFormversion(Integer.parseInt(body.get("formversion").toString().trim()));
			} catch (Exception ignored) {}
		}

		if (body.get("customFields") != null) {
			Object cf = body.get("customFields");
			if (cf instanceof String) {
				client.setCustomFields((String) cf);
			} else if (cf instanceof Map) {
				StringBuilder sb = new StringBuilder("{");
				Map<?, ?> map = (Map<?, ?>) cf;
				int i = 0;
				for (Map.Entry<?, ?> entry : map.entrySet()) {
					if (i++ > 0) sb.append(",");
					sb.append("\"").append(entry.getKey()).append("\":");
					Object val = entry.getValue();
					if (val == null) {
						sb.append("null");
					} else if (val instanceof Number || val instanceof Boolean) {
						sb.append(val);
					} else {
						sb.append("\"").append(val.toString().replace("\"", "\\\"")).append("\"");
					}
				}
				sb.append("}");
				client.setCustomFields(sb.toString());
			} else {
				client.setCustomFields(cf.toString());
			}
		}

		Client saved = clientRepo.save(client);

		if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
			emailService.sendAccountCreationWelcomeEmail(
					saved.getEmail(),
					saved.getName(),
					saved.getMobile(),
					password,
					"CLIENT"
			);
		}

		response.put("status", "success");
		response.put("message", "Client registered successfully!");
		response.put("client", saved);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/register/Admin")
	public ResponseEntity<Object> registerAdmin(@RequestBody Admin admin){
		String rawPassword = admin.getPassword();
		admin.setPassword(passwordencoder.encode(admin.getPassword()));
		if (admin.getRole() == null || admin.getRole().isEmpty()) {
			admin.setRole("ADMIN");
		}
		Admin saved = adminRepo.save(admin);

		if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
			emailService.sendAccountCreationWelcomeEmail(
					saved.getEmail(),
					saved.getName(),
					saved.getEmail(),
					rawPassword,
					"ADMIN"
			);
		}

		Map<String,Object> response=new HashMap<String,Object>();
		response.put("status","Success");
		response.put("Message","Registation Successfull !!");
		return ResponseEntity.ok(response);
	}

	@PostMapping("/register/Staff")
	public ResponseEntity<Object> registerStaff(@RequestBody Staff staff){
		String rawPassword = staff.getPassword();
		staff.setPassword(passwordencoder.encode(staff.getPassword()));
		if (staff.getRole() == null || staff.getRole().isEmpty()) {
			staff.setRole("STAFF");
		}
		staff.setActive(true);
		Staff saved = staffRepo.save(staff);

		if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
			emailService.sendAccountCreationWelcomeEmail(
					saved.getEmail(),
					saved.getName(),
					saved.getEmail(),
					rawPassword,
					"STAFF"
			);
		}

		Map<String,Object> response=new HashMap<String,Object>();
		response.put("status","Success");
		response.put("Message","Staff Registration Successful !!");
		return ResponseEntity.ok(response);
	}
}
