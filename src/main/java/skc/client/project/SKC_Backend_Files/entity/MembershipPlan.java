package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "membership_plans")
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // SILVER, GOLD, PLATINUM
    @Column(unique = true)
    private String planTier;

    private String displayName;
    private Integer durationMonths;
    private Double price;
    private String features; // comma-separated features
    private boolean active = true;

    public MembershipPlan() {}

    // Getters & Setters
    public Long getId() { return id; }
    public String getPlanTier() { return planTier; }
    public void setPlanTier(String planTier) { this.planTier = planTier; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
