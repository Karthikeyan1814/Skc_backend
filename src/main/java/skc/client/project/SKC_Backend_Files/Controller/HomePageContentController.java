package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.HomePageContentRepo;
import skc.client.project.SKC_Backend_Files.entity.HomePageContent;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/skc")
public class HomePageContentController {

    @Autowired
    private HomePageContentRepo contentRepo;

    // Helper to get or initialize default content
    private HomePageContent getOrCreateDefault() {
        return contentRepo.findByPageKey("HOME").orElseGet(() -> {
            HomePageContent defaultContent = new HomePageContent();
            defaultContent.setPageKey("HOME");
            return contentRepo.save(defaultContent);
        });
    }

    // ─── PUBLIC: Get Home Page dynamic content ──────────────────────────────────
    @GetMapping({"/Public/HomePageContent", "/Public/HomeContent"})
    public ResponseEntity<HomePageContent> getPublicHomeContent() {
        return ResponseEntity.ok(getOrCreateDefault());
    }

    // ─── ADMIN: Get Home Page dynamic content for settings master form ──────────
    @GetMapping("/Admin/HomePageContent")
    public ResponseEntity<HomePageContent> getAdminHomeContent() {
        return ResponseEntity.ok(getOrCreateDefault());
    }

    // ─── ADMIN: Update Home Page dynamic content ────────────────────────────────
    @PutMapping("/Admin/HomePageContent")
    @PostMapping("/Admin/HomePageContent")
    public ResponseEntity<Object> updateHomeContent(@RequestBody HomePageContent updated) {
        Map<String, Object> response = new HashMap<>();

        HomePageContent current = getOrCreateDefault();

        // Update Hero Left Box
        if (updated.getHeroLeftTitle1() != null) current.setHeroLeftTitle1(updated.getHeroLeftTitle1());
        if (updated.getHeroLeftTitle2() != null) current.setHeroLeftTitle2(updated.getHeroLeftTitle2());
        if (updated.getHeroLeftSubtitle() != null) current.setHeroLeftSubtitle(updated.getHeroLeftSubtitle());
        if (updated.getHeroLeftDescription() != null) current.setHeroLeftDescription(updated.getHeroLeftDescription());
        if (updated.getHeroLeftTagline() != null) current.setHeroLeftTagline(updated.getHeroLeftTagline());
        if (updated.getHeroLeftQuote() != null) current.setHeroLeftQuote(updated.getHeroLeftQuote());

        // Update Hero Scores
        if (updated.getHeroExpYears() != null) current.setHeroExpYears(updated.getHeroExpYears());
        if (updated.getHeroExpLabel() != null) current.setHeroExpLabel(updated.getHeroExpLabel());
        if (updated.getHeroClientsCount() != null) current.setHeroClientsCount(updated.getHeroClientsCount());
        if (updated.getHeroClientsLabel() != null) current.setHeroClientsLabel(updated.getHeroClientsLabel());
        if (updated.getHeroVerificationPct() != null) current.setHeroVerificationPct(updated.getHeroVerificationPct());
        if (updated.getHeroVerificationLabel() != null) current.setHeroVerificationLabel(updated.getHeroVerificationLabel());
        if (updated.getHeroSupportHours() != null) current.setHeroSupportHours(updated.getHeroSupportHours());
        if (updated.getHeroSupportDays() != null) current.setHeroSupportDays(updated.getHeroSupportDays());
        if (updated.getHeroSupportLabel() != null) current.setHeroSupportLabel(updated.getHeroSupportLabel());

        // Update Hero Right Box
        if (updated.getHeroRightTitle() != null) current.setHeroRightTitle(updated.getHeroRightTitle());
        if (updated.getHeroRightSubtitle() != null) current.setHeroRightSubtitle(updated.getHeroRightSubtitle());
        if (updated.getHeroRightDescription() != null) current.setHeroRightDescription(updated.getHeroRightDescription());
        if (updated.getHeroRightTagline() != null) current.setHeroRightTagline(updated.getHeroRightTagline());
        if (updated.getHeroCircleBadgeText() != null) current.setHeroCircleBadgeText(updated.getHeroCircleBadgeText());

        // Update Running Services & Core Boxes
        if (updated.getRunningServices() != null) current.setRunningServices(updated.getRunningServices());
        if (updated.getCoreNoticeHead() != null) current.setCoreNoticeHead(updated.getCoreNoticeHead());
        if (updated.getCoreNoticeTitle() != null) current.setCoreNoticeTitle(updated.getCoreNoticeTitle());
        if (updated.getCoreNoticeTamil() != null) current.setCoreNoticeTamil(updated.getCoreNoticeTamil());
        if (updated.getCoreNoticeDesc() != null) current.setCoreNoticeDesc(updated.getCoreNoticeDesc());
        if (updated.getCoreAppealHead() != null) current.setCoreAppealHead(updated.getCoreAppealHead());
        if (updated.getCoreAppealTitle() != null) current.setCoreAppealTitle(updated.getCoreAppealTitle());
        if (updated.getCoreAppealTamil() != null) current.setCoreAppealTamil(updated.getCoreAppealTamil());
        if (updated.getCoreAppealDesc() != null) current.setCoreAppealDesc(updated.getCoreAppealDesc());

        // Update Contacts & Office coordinates
        if (updated.getContactPhone() != null) current.setContactPhone(updated.getContactPhone());
        if (updated.getContactEmail() != null) current.setContactEmail(updated.getContactEmail());
        if (updated.getContactWhatsApp() != null) current.setContactWhatsApp(updated.getContactWhatsApp());
        if (updated.getOfficeAdvisorName() != null) current.setOfficeAdvisorName(updated.getOfficeAdvisorName());
        if (updated.getOfficeAdvisorTitle() != null) current.setOfficeAdvisorTitle(updated.getOfficeAdvisorTitle());
        if (updated.getOfficeAddressLine1() != null) current.setOfficeAddressLine1(updated.getOfficeAddressLine1());
        if (updated.getOfficeAddressLine2() != null) current.setOfficeAddressLine2(updated.getOfficeAddressLine2());
        if (updated.getOfficeAddressCity() != null) current.setOfficeAddressCity(updated.getOfficeAddressCity());

        HomePageContent saved = contentRepo.save(current);

        response.put("status", "success");
        response.put("message", "Home page content updated successfully");
        response.put("content", saved);
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Reset to factory defaults ───────────────────────────────────────
    @PostMapping("/Admin/HomePageContent/Reset")
    public ResponseEntity<Object> resetHomeContent() {
        Map<String, Object> response = new HashMap<>();

        contentRepo.findByPageKey("HOME").ifPresent(contentRepo::delete);
        HomePageContent fresh = getOrCreateDefault();

        response.put("status", "success");
        response.put("message", "Home page content has been reset to default values");
        response.put("content", fresh);
        return ResponseEntity.ok(response);
    }
}
