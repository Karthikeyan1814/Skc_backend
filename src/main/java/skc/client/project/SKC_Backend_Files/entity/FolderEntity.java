package skc.client.project.SKC_Backend_Files.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "folders")
public class FolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String folderName;

    private String description;

    private String folderColor = "#4f46e5";

    // Category under which this folder is organized (GST, ROC, TAX, etc.)
    private String category;

    // true: common/global to all clients; false: allocated to specific client
    @Column(nullable = false)
    private boolean isCommon = false;

    // Allocated client (if isCommon is false)
    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    private String createdByRole; // "ADMIN" or "STAFF"
    private Long createdById;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public FolderEntity() {}

    public FolderEntity(String folderName, String description, boolean isCommon, Client client, String createdByRole, Long createdById) {
        this.folderName = folderName;
        this.description = description;
        this.isCommon = isCommon;
        this.client = client;
        this.createdByRole = createdByRole;
        this.createdById = createdById;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFolderColor() { return folderColor; }
    public void setFolderColor(String folderColor) { this.folderColor = folderColor; }

    public boolean isCommon() { return isCommon; }
    public void setCommon(boolean common) { isCommon = common; }

    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }

    public String getCreatedByRole() { return createdByRole; }
    public void setCreatedByRole(String createdByRole) { this.createdByRole = createdByRole; }

    public Long getCreatedById() { return createdById; }
    public void setCreatedById(Long createdById) { this.createdById = createdById; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
