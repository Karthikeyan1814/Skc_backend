package skc.client.project.SKC_Backend_Files.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_page_content")
public class HomePageContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "page_key", unique = true)
    private String pageKey = "HOME";

    // ─── HERO LEFT BOX ────────────────────────────────────────────────────────
    private String heroLeftTitle1 = "உங்கள் வணிகம்...";
    private String heroLeftTitle2 = "எங்கள் வரி நிபுணத்துவம்.";
    private String heroLeftSubtitle = "GST Advisory • Notice Reply • Appeal • Litigation Support";

    @Column(columnDefinition = "TEXT")
    private String heroLeftDescription = "From GST registrations to complex litigation matters, we assist businesses with practical tax solutions, statutory compliance, and professional representation before tax authorities.";

    private String heroLeftTagline = "Reliable Advice. Timely Action. Lasting Trust.";
    private String heroLeftQuote = "\"சரியான ஆலோசனை இன்று... பாதுகாப்பான வணிகம் நாளை.\"";

    // Hero Left Score counters
    private String heroExpYears = "10";
    private String heroExpLabel = "Years of Professional Experience";
    private String heroClientsCount = "500";
    private String heroClientsLabel = "Businesses & Professionals Served";
    private String heroVerificationPct = "100";
    private String heroVerificationLabel = "Legally Verified Documentation";
    private String heroSupportHours = "24";
    private String heroSupportDays = "7";
    private String heroSupportLabel = "Professional Response";

    // ─── HERO RIGHT BOX ───────────────────────────────────────────────────────
    private String heroRightTitle = "Trusted GST, Income Tax & Business Advisory";
    private String heroRightSubtitle = "Delivering Confidence Through Compliance.";

    @Column(columnDefinition = "TEXT")
    private String heroRightDescription = "We provide comprehensive GST, Income Tax, Audit, and Business Advisory services tailored to businesses, professionals, and growing enterprises. Every opinion, representation, and submission is backed by statutory provisions, judicial precedents, and practical industry expertise.";

    private String heroRightTagline = "Integrity • Accuracy • Professional Excellence";
    private String heroCircleBadgeText = "GST EXPERT";

    // ─── RUNNING TICKER & CORE SERVICES ───────────────────────────────────────
    @Column(columnDefinition = "TEXT")
    private String runningServices = "GST Registration, APL-01 Appeal Filing, Section 73 / 74 Defence, ASMT-11 Drafting, E-Way Bill Disputes, Income Tax Filing, GST Notice Reply";

    private String coreNoticeHead = "MOST REQUIRED";
    private String coreNoticeTitle = "GST Notice Reply";
    private String coreNoticeTamil = "ஜிஎஸ்டி நோட்டீஸ் பதில்";
    @Column(columnDefinition = "TEXT")
    private String coreNoticeDesc = "Professional reply drafting for every type of GST notice. Each reply built on bare act extracts, official circulars, and verified High Court or Supreme Court judgments. No copy-paste templates. No casual citations.";

    private String coreAppealHead = "CRITICAL SERVICE";
    private String coreAppealTitle = "GST Appeal Filing";
    private String coreAppealTamil = "ஜிஎஸ்டி மேல்முறையீடு";
    @Column(columnDefinition = "TEXT")
    private String coreAppealDesc = "Complete APL-01 appeal drafting and filing before the Appellate Authority. Pre-deposit calculation under Section 107(6), grounds of appeal in paragraph form, supported by actual judgment citations";

    // ─── CONTACT & OFFICE COORDINATES ─────────────────────────────────────────
    private String contactPhone = "+91 8807836326";
    private String contactEmail = "info@skctaxconsultants.com";
    private String contactWhatsApp = "+91 8807836326";
    private String officeAdvisorName = "Vel Murugan";
    private String officeAdvisorTitle = "Tax | Audit | Advisory";
    private String officeAddressLine1 = "KKP Complex , Salem Main Road ,";
    private String officeAddressLine2 = "Old Post Office , 2nd Floor ,";
    private String officeAddressCity = "Jalakandapuram - 636501 , Salem , TamilNadu .";

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public HomePageContent() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPageKey() { return pageKey; }
    public void setPageKey(String pageKey) { this.pageKey = pageKey; }

    public String getHeroLeftTitle1() { return heroLeftTitle1; }
    public void setHeroLeftTitle1(String heroLeftTitle1) { this.heroLeftTitle1 = heroLeftTitle1; }

    public String getHeroLeftTitle2() { return heroLeftTitle2; }
    public void setHeroLeftTitle2(String heroLeftTitle2) { this.heroLeftTitle2 = heroLeftTitle2; }

    public String getHeroLeftSubtitle() { return heroLeftSubtitle; }
    public void setHeroLeftSubtitle(String heroLeftSubtitle) { this.heroLeftSubtitle = heroLeftSubtitle; }

    public String getHeroLeftDescription() { return heroLeftDescription; }
    public void setHeroLeftDescription(String heroLeftDescription) { this.heroLeftDescription = heroLeftDescription; }

    public String getHeroLeftTagline() { return heroLeftTagline; }
    public void setHeroLeftTagline(String heroLeftTagline) { this.heroLeftTagline = heroLeftTagline; }

    public String getHeroLeftQuote() { return heroLeftQuote; }
    public void setHeroLeftQuote(String heroLeftQuote) { this.heroLeftQuote = heroLeftQuote; }

    public String getHeroExpYears() { return heroExpYears; }
    public void setHeroExpYears(String heroExpYears) { this.heroExpYears = heroExpYears; }

    public String getHeroExpLabel() { return heroExpLabel; }
    public void setHeroExpLabel(String heroExpLabel) { this.heroExpLabel = heroExpLabel; }

    public String getHeroClientsCount() { return heroClientsCount; }
    public void setHeroClientsCount(String heroClientsCount) { this.heroClientsCount = heroClientsCount; }

    public String getHeroClientsLabel() { return heroClientsLabel; }
    public void setHeroClientsLabel(String heroClientsLabel) { this.heroClientsLabel = heroClientsLabel; }

    public String getHeroVerificationPct() { return heroVerificationPct; }
    public void setHeroVerificationPct(String heroVerificationPct) { this.heroVerificationPct = heroVerificationPct; }

    public String getHeroVerificationLabel() { return heroVerificationLabel; }
    public void setHeroVerificationLabel(String heroVerificationLabel) { this.heroVerificationLabel = heroVerificationLabel; }

    public String getHeroSupportHours() { return heroSupportHours; }
    public void setHeroSupportHours(String heroSupportHours) { this.heroSupportHours = heroSupportHours; }

    public String getHeroSupportDays() { return heroSupportDays; }
    public void setHeroSupportDays(String heroSupportDays) { this.heroSupportDays = heroSupportDays; }

    public String getHeroSupportLabel() { return heroSupportLabel; }
    public void setHeroSupportLabel(String heroSupportLabel) { this.heroSupportLabel = heroSupportLabel; }

    public String getHeroRightTitle() { return heroRightTitle; }
    public void setHeroRightTitle(String heroRightTitle) { this.heroRightTitle = heroRightTitle; }

    public String getHeroRightSubtitle() { return heroRightSubtitle; }
    public void setHeroRightSubtitle(String heroRightSubtitle) { this.heroRightSubtitle = heroRightSubtitle; }

    public String getHeroRightDescription() { return heroRightDescription; }
    public void setHeroRightDescription(String heroRightDescription) { this.heroRightDescription = heroRightDescription; }

    public String getHeroRightTagline() { return heroRightTagline; }
    public void setHeroRightTagline(String heroRightTagline) { this.heroRightTagline = heroRightTagline; }

    public String getHeroCircleBadgeText() { return heroCircleBadgeText; }
    public void setHeroCircleBadgeText(String heroCircleBadgeText) { this.heroCircleBadgeText = heroCircleBadgeText; }

    public String getRunningServices() { return runningServices; }
    public void setRunningServices(String runningServices) { this.runningServices = runningServices; }

    public String getCoreNoticeHead() { return coreNoticeHead; }
    public void setCoreNoticeHead(String coreNoticeHead) { this.coreNoticeHead = coreNoticeHead; }

    public String getCoreNoticeTitle() { return coreNoticeTitle; }
    public void setCoreNoticeTitle(String coreNoticeTitle) { this.coreNoticeTitle = coreNoticeTitle; }

    public String getCoreNoticeTamil() { return coreNoticeTamil; }
    public void setCoreNoticeTamil(String coreNoticeTamil) { this.coreNoticeTamil = coreNoticeTamil; }

    public String getCoreNoticeDesc() { return coreNoticeDesc; }
    public void setCoreNoticeDesc(String coreNoticeDesc) { this.coreNoticeDesc = coreNoticeDesc; }

    public String getCoreAppealHead() { return coreAppealHead; }
    public void setCoreAppealHead(String coreAppealHead) { this.coreAppealHead = coreAppealHead; }

    public String getCoreAppealTitle() { return coreAppealTitle; }
    public void setCoreAppealTitle(String coreAppealTitle) { this.coreAppealTitle = coreAppealTitle; }

    public String getCoreAppealTamil() { return coreAppealTamil; }
    public void setCoreAppealTamil(String coreAppealTamil) { this.coreAppealTamil = coreAppealTamil; }

    public String getCoreAppealDesc() { return coreAppealDesc; }
    public void setCoreAppealDesc(String coreAppealDesc) { this.coreAppealDesc = coreAppealDesc; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getContactWhatsApp() { return contactWhatsApp; }
    public void setContactWhatsApp(String contactWhatsApp) { this.contactWhatsApp = contactWhatsApp; }

    public String getOfficeAdvisorName() { return officeAdvisorName; }
    public void setOfficeAdvisorName(String officeAdvisorName) { this.officeAdvisorName = officeAdvisorName; }

    public String getOfficeAdvisorTitle() { return officeAdvisorTitle; }
    public void setOfficeAdvisorTitle(String officeAdvisorTitle) { this.officeAdvisorTitle = officeAdvisorTitle; }

    public String getOfficeAddressLine1() { return officeAddressLine1; }
    public void setOfficeAddressLine1(String officeAddressLine1) { this.officeAddressLine1 = officeAddressLine1; }

    public String getOfficeAddressLine2() { return officeAddressLine2; }
    public void setOfficeAddressLine2(String officeAddressLine2) { this.officeAddressLine2 = officeAddressLine2; }

    public String getOfficeAddressCity() { return officeAddressCity; }
    public void setOfficeAddressCity(String officeAddressCity) { this.officeAddressCity = officeAddressCity; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
