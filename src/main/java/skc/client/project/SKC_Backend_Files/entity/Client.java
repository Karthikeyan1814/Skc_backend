package skc.client.project.SKC_Backend_Files.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;

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
	private String legalname;
	private String gstnumber;
	private String pannumber;
	private Boolean temppass = false;
	private String location;

	// Membership: NONE, MEMBER
	private String membershipStatus = "NONE";

	// Membership plan tier: SILVER, GOLD, PLATINUM
	private String membershipPlan;

	// Membership expiry date
	private LocalDate membershipExpiry;

	// Membership activation timestamp
	private LocalDateTime membershipActivatedAt;

	private Boolean active = true;

	private Boolean reconsultationRequested = false;

	@jakarta.persistence.Column(columnDefinition = "TEXT")
	private String reconsultationNotes;

	private LocalDateTime reconsultationRequestedAt;

	private Integer formversion;

	@jakarta.persistence.Column(columnDefinition = "TEXT")
	private String customFields;

	private LocalDateTime createdAt;

	@PrePersist
	public void prePersist() {
		if (this.createdAt == null) this.createdAt = LocalDateTime.now();
	}
	
	@JsonIgnore
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


	


	public String getLegalname() {
		return legalname;
	}


	public void setLegalname(String legalname) {
		this.legalname = legalname;
	}


	public String getGstnumber() {
		return gstnumber;
	}


	public void setGstnumber(String gstnumber) {
		this.gstnumber = gstnumber;
	}


	public String getPannumber() {
		return pannumber;
	}


	public void setPannumber(String pannumber) {
		this.pannumber = pannumber;
	}


	public Boolean isTemppass() {
		return temppass != null && temppass;
	}


	public void setTemppass(Boolean temppass) {
		this.temppass = temppass;
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

	

	public String getMembershipStatus() { return membershipStatus; }
	public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }
	public String getMembershipPlan() { return membershipPlan; }
	public void setMembershipPlan(String membershipPlan) { this.membershipPlan = membershipPlan; }
	public LocalDate getMembershipExpiry() { return membershipExpiry; }
	public void setMembershipExpiry(LocalDate membershipExpiry) { this.membershipExpiry = membershipExpiry; }
	public LocalDateTime getMembershipActivatedAt() { return membershipActivatedAt; }
	public void setMembershipActivatedAt(LocalDateTime membershipActivatedAt) { this.membershipActivatedAt = membershipActivatedAt; }
	public Boolean isActive() { return active != null && active; }
	public void setActive(Boolean active) { this.active = active; }
	public Boolean isReconsultationRequested() { return reconsultationRequested != null && reconsultationRequested; }
	public void setReconsultationRequested(Boolean reconsultationRequested) { this.reconsultationRequested = reconsultationRequested; }
	public String getReconsultationNotes() { return reconsultationNotes; }
	public void setReconsultationNotes(String reconsultationNotes) { this.reconsultationNotes = reconsultationNotes; }
	public LocalDateTime getReconsultationRequestedAt() { return reconsultationRequestedAt; }
	public void setReconsultationRequestedAt(LocalDateTime reconsultationRequestedAt) { this.reconsultationRequestedAt = reconsultationRequestedAt; }
	public Integer getFormversion() { return formversion; }
	public void setFormversion(Integer formversion) { this.formversion = formversion; }
	public String getCustomFields() { return customFields; }
	public void setCustomFields(String customFields) { this.customFields = customFields; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	public Client(String email, String password, String name, String mobile, String role, String legalname,
			String gstnumber, String pannumber, boolean temppass, String location, List<FileEntity> files) {
		super();
		this.email = email;
		this.password = password;
		this.name = name;
		this.mobile = mobile;
		this.role = role;
		this.legalname = legalname;
		this.gstnumber = gstnumber;
		this.pannumber = pannumber;
		this.temppass = temppass;
		this.location = location;
		this.files = files;
		this.active = true;
		this.membershipStatus = "NONE";
	}

	public Client() {}
}
