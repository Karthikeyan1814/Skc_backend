package skc.client.project.SKC_Backend_Files.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private String email;
	@JsonProperty(access = Access.WRITE_ONLY)
	private String password;
	private String name;
	private String mobile;
	private String role;
	private String shopname;
	private String location;
	
	@OneToMany(mappedBy = "client")
	private List<FileEntity> files;
	
	
	public List<FileEntity> getFiles() {
		return files;
	}


	public void setFiles(List<FileEntity> files) {
		this.files = files;
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


	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getMobile() {
		return mobile;
	}


	public void setMobile(String mobile) {
		this.mobile = mobile;
	}


	public String getRole() {
		return role;
	}


	public void setRole(String role) {
		this.role = role;
	}


	public String getShopname() {
		return shopname;
	}


	public void setShopname(String shopname) {
		this.shopname = shopname;
	}


	public String getLocation() {
		return location;
	}


	public void setLocation(String location) {
		this.location = location;
	}


	public Long getId() {
		return id;
	}



	public Client(String email, String password, String name, String mobile, String shopname,
			String location) {
		super();
		this.email = email;
		this.password = password;
		this.name = name;
		this.mobile = mobile;
		this.role = "CLIENT";
		this.shopname = shopname;
		this.location = location;
	}


	public Client() {
		
	}
}
