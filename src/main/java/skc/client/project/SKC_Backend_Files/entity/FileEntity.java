package skc.client.project.SKC_Backend_Files.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String fileName;
    private String contentType;

    // Uploader info
    private String userName;
    private String userEmail;
    private String userMobile;

    // File status: ACTIVE, EXPIRED
    private String status = "ACTIVE";

    private Long size;

    private LocalDateTime createdAt;

    // Category: GST, ROC, TAX, FINANCIAL, PAYROLL, CERTIFICATE, COMPLIANCE, IMPORTANT
    private String category;

    // S3 object key
    private String objectKey;

    // Direction: SENT_TO_CLIENT (admin/staff → client), UPLOADED_BY_CLIENT (client → admin/staff)
    private String direction;

    // Who uploaded: ADMIN, STAFF, CLIENT
    private String uploadedByRole;
    private Long uploadedById;

    // Expiration date (null = no expiry)
    private LocalDate expirationDate;

    // Extension request flag
    private boolean extensionRequested = false;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public FileEntity() {}

    // Getters and Setters
    public Long getId() { return id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserMobile() { return userMobile; }
    public void setUserMobile(String userMobile) { this.userMobile = userMobile; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getUploadedByRole() { return uploadedByRole; }
    public void setUploadedByRole(String uploadedByRole) { this.uploadedByRole = uploadedByRole; }

    public Long getUploadedById() { return uploadedById; }
    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public boolean isExtensionRequested() { return extensionRequested; }
    public void setExtensionRequested(boolean extensionRequested) { this.extensionRequested = extensionRequested; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    /**
     * Returns true if the file is expired (expiration date set and in the past).
     */
    public boolean isExpired() {
        if (expirationDate == null) return false;
        return LocalDate.now().isAfter(expirationDate);
    }
}
