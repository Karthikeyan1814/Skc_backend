package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_requests")
public class MembershipRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String email;
    private String mobile;
    private String businessName;
    private String businessType;
    private String location;
    private String gstnumber;
    private String pannumber;
    @Column(columnDefinition = "TEXT")
    private String message;

    // Plan selected by applicant: SILVER, GOLD, PLATINUM
    private String planType = "SILVER";

    // Status: PENDING, APPROVED, REJECTED
    private String status = "PENDING";

    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;

    // Admin notes when approving/rejecting
    @Column(columnDefinition = "TEXT")
    private String adminNote;

    // Whether QR was sent
    private boolean qrSent = false;

    // Whether membership access was granted (linked to a Client)
    private Long linkedClientId;

    @PrePersist
    public void prePersist() {
        this.requestedAt = LocalDateTime.now();
    }

    public MembershipRequest() {}

    // Getters and setters
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getGstnumber() { return gstnumber; }
    public void setGstnumber(String gstnumber) { this.gstnumber = gstnumber; }
    public String getPannumber() { return pannumber; }
    public void setPannumber(String pannumber) { this.pannumber = pannumber; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
    public boolean isQrSent() { return qrSent; }
    public void setQrSent(boolean qrSent) { this.qrSent = qrSent; }
    public Long getLinkedClientId() { return linkedClientId; }
    public void setLinkedClientId(Long linkedClientId) { this.linkedClientId = linkedClientId; }
}
