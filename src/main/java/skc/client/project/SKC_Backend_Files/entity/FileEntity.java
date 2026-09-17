package skc.client.project.SKC_Backend_Files.entity;

import java.sql.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="files")
public class FileEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE)
	private Long id;
	
	private String fileName;
	
	private String contentType;
	
	private String userName;
	
	private String userEmail;
	
	private String userMobile;
	
	private String status;
	
	private Long size;
	
	private Date createdAt;
	
	private String category;
	
	
	private String objectKey;
	
	@ManyToOne
	@JoinColumn(name="client_id")
	private Client client;
	

	
	public String getCategory() {
		return category;
	}


	public void setCategory(String category) {
		this.category = category;
	}


	public Client getClient() {
		return client;
	}


	public void setClient(Client client) {
		this.client = client;
	}


	public String getObjectKey() {
		return objectKey;
	}


	public void setObjectKey(String objectKey) {
		this.objectKey = objectKey;
	}


	public String getFileName() {
		return fileName;
	}


	public void setFileName(String fileName) {
		this.fileName = fileName;
	}


	public String getContentType() {
		return contentType;
	}


	public void setContentType(String contentType) {
		this.contentType = contentType;
	}


	public String getUserName() {
		return userName;
	}


	public void setUserName(String userName) {
		this.userName = userName;
	}


	public Long getSize() {
		return size;
	}


	public void setSize(Long size) {
		this.size = size;
	}




	public Long getId() {
		return id;
	}
	
	public FileEntity() {
		
	}


	public Date getCreatedAt() {
		return createdAt;
	}


	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}


	public String getUserEmail() {
		return userEmail;
	}


	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}


	public String getUserMobile() {
		return userMobile;
	}


	public void setUserMobile(String userMobile) {
		this.userMobile = userMobile;
	}


	public String getStatus() {
		return status;
	}


	public void setStatus(String status) {
		this.status = status;
	}


	public FileEntity(String fileName, String contentType, String userName, String userEmail, String userMobile,
			String status, Long size, Date createdAt, String category, String objectKey, Client client) {
		super();
		this.fileName = fileName;
		this.contentType = contentType;
		this.userName = userName;
		this.userEmail = userEmail;
		this.userMobile = userMobile;
		this.status = status;
		this.size = size;
		this.createdAt = createdAt;
		this.category = category;
		this.objectKey = objectKey;
		this.client = client;
	}



	









	
	
	

}

