package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_documents")
public class BillingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String docNumber;

    @Column(nullable = false)
    private String type; // "INVOICE" or "RECEIPT"

    private Long clientId;
    private String clientName;
    private String clientLegalName;
    private String clientMobile;
    private String clientEmail;
    private String clientGstin;
    private String clientPan;

    @Column(columnDefinition = "TEXT")
    private String clientAddress;
    private String clientState;
    private String clientStateCode;

    private String docDate;
    private String docTime;
    private String dueDate;

    private Double totalAmount = 0.0;
    private Double paidAmount = 0.0;
    private Double remainingBalance = 0.0;
    private String paymentMode;
    private String receiptTowards;
    private Double declaredNetWorthPayable;
    private Double totalOutstanding = 0.0;
    private Double declaredAdjustmentDiff = 0.0;
    private String declaredAdjustmentNote;

    private Boolean isArchived = false;

    @Column(columnDefinition = "TEXT")
    private String paymentDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String itemsJson;

    @Column(columnDefinition = "TEXT")
    private String intermediateRowsJson;

    @Column(columnDefinition = "TEXT")
    private String customColumnsJson;

    @Column(columnDefinition = "TEXT")
    private String netWorthAuditHistoryJson;

    @Column(columnDefinition = "TEXT")
    private String firmJson;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        if (this.updatedAt == null) this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDocNumber() { return docNumber; }
    public void setDocNumber(String docNumber) { this.docNumber = docNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientLegalName() { return clientLegalName; }
    public void setClientLegalName(String clientLegalName) { this.clientLegalName = clientLegalName; }

    public String getClientMobile() { return clientMobile; }
    public void setClientMobile(String clientMobile) { this.clientMobile = clientMobile; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientGstin() { return clientGstin; }
    public void setClientGstin(String clientGstin) { this.clientGstin = clientGstin; }

    public String getClientPan() { return clientPan; }
    public void setClientPan(String clientPan) { this.clientPan = clientPan; }

    public String getClientAddress() { return clientAddress; }
    public void setClientAddress(String clientAddress) { this.clientAddress = clientAddress; }

    public String getClientState() { return clientState; }
    public void setClientState(String clientState) { this.clientState = clientState; }

    public String getClientStateCode() { return clientStateCode; }
    public void setClientStateCode(String clientStateCode) { this.clientStateCode = clientStateCode; }

    public String getDocDate() { return docDate; }
    public void setDocDate(String docDate) { this.docDate = docDate; }

    public String getDocTime() { return docTime; }
    public void setDocTime(String docTime) { this.docTime = docTime; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(Double paidAmount) { this.paidAmount = paidAmount; }

    public Double getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(Double remainingBalance) { this.remainingBalance = remainingBalance; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }

    public String getReceiptTowards() { return receiptTowards; }
    public void setReceiptTowards(String receiptTowards) { this.receiptTowards = receiptTowards; }

    public Double getDeclaredNetWorthPayable() { return declaredNetWorthPayable; }
    public void setDeclaredNetWorthPayable(Double declaredNetWorthPayable) { this.declaredNetWorthPayable = declaredNetWorthPayable; }

    public Double getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(Double totalOutstanding) { this.totalOutstanding = totalOutstanding; }

    public Double getDeclaredAdjustmentDiff() { return declaredAdjustmentDiff; }
    public void setDeclaredAdjustmentDiff(Double declaredAdjustmentDiff) { this.declaredAdjustmentDiff = declaredAdjustmentDiff; }

    public String getDeclaredAdjustmentNote() { return declaredAdjustmentNote; }
    public void setDeclaredAdjustmentNote(String declaredAdjustmentNote) { this.declaredAdjustmentNote = declaredAdjustmentNote; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public String getPaymentDetailsJson() { return paymentDetailsJson; }
    public void setPaymentDetailsJson(String paymentDetailsJson) { this.paymentDetailsJson = paymentDetailsJson; }

    public String getItemsJson() { return itemsJson; }
    public void setItemsJson(String itemsJson) { this.itemsJson = itemsJson; }

    public String getIntermediateRowsJson() { return intermediateRowsJson; }
    public void setIntermediateRowsJson(String intermediateRowsJson) { this.intermediateRowsJson = intermediateRowsJson; }

    public String getCustomColumnsJson() { return customColumnsJson; }
    public void setCustomColumnsJson(String customColumnsJson) { this.customColumnsJson = customColumnsJson; }

    public String getNetWorthAuditHistoryJson() { return netWorthAuditHistoryJson; }
    public void setNetWorthAuditHistoryJson(String netWorthAuditHistoryJson) { this.netWorthAuditHistoryJson = netWorthAuditHistoryJson; }

    public String getFirmJson() { return firmJson; }
    public void setFirmJson(String firmJson) { this.firmJson = firmJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
