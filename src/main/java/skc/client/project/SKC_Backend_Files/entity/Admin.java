package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Admin {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	private String name;
	private String email;
	private String password;
	private String mobileno;
	private String altmobileno;
	private String role;
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
	public String getMobileno() {
		return mobileno;
	}
	public void setMobileno(String mobileno) {
		this.mobileno = mobileno;
	}
	public String getAltmobileno() {
		return altmobileno;
	}
	public void setAltmobileno(String altmobileno) {
		this.altmobileno = altmobileno;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	public Integer getId() {
		return id;
	}
	public Admin(String name, String email, String password, String mobileno, String altmobileno, String role) {
		super();
		this.name = name;
		this.email = email;
		this.password = password;
		this.mobileno = mobileno;
		this.altmobileno = altmobileno;
		this.role = role;
	}
	
	public Admin() {
		
	}
	
}
