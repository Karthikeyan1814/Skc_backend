package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "firm_settings")
public class FirmSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name = "Sri Kathiravan & Co.";
    private String subTitle = "Tax, Audit & Financial Advisory Consultants";

    @Column(columnDefinition = "TEXT")
    private String address = "No. 42, Commercial Complex, Gandhi Road";
    private String city = "Salem, Tamil Nadu - 636007";
    private String phone = "+91 88257 50992";
    private String email = "contact@srikathiravan.com";
    private String gstin = "33AAAAA0000A1Z5";
    private String pan = "AAAAA0000A";
    private String state = "Tamil Nadu";
    private String stateCode = "33";

    // Banking & Remittance Details
    private String bankName = "HDFC Bank Ltd";
    private String accountName = "Sri Kathiravan & Co.";
    private String accountNumber = "50200084729103";
    private String ifscCode = "HDFC0001234";
    private String branch = "Salem Main Branch";
    private String upiId = "8825750992@upi";
    private Boolean membershipEnabled = false;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public FirmSettings() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubTitle() { return subTitle; }
    public void setSubTitle(String subTitle) { this.subTitle = subTitle; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public Boolean getMembershipEnabled() { return membershipEnabled != null && membershipEnabled; }
    public void setMembershipEnabled(Boolean membershipEnabled) { this.membershipEnabled = membershipEnabled; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
