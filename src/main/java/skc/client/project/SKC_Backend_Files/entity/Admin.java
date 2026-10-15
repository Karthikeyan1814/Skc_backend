package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Admin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String name;
	private String email;
	private String password;
	private String mobile;
	private String altmobile;
	private String role;
	private Boolean active = true;

	public Boolean isActive() {
		return active == null || active;
	}
	public void setActive(Boolean active) {
		this.active = active;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	public String getAltmobile() {
		return altmobile;
	}
	public void setAltmobile(String altmobile) {
		this.altmobile = altmobile;
	}
	public void setAltmobileno(String altmobile) {
		this.altmobile = altmobile;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public Long getId() {
		return id;
	}
	public Admin(String name, String email, String password, String mobile, String altmobile) {
		super();
		this.name = name;
		this.email = email;
		this.password = password;
		this.mobile = mobile;
		this.altmobile = altmobile;
		this.role = "ADMIN";
	}
	
	public Admin() {
		
	}
	
}
