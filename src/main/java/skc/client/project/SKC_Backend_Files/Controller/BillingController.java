package skc.client.project.SKC_Backend_Files.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.BillingDocumentRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.FirmSettingsRepo;
import skc.client.project.SKC_Backend_Files.Service.EmailService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.BillingDocument;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.FirmSettings;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/skc")
public class BillingController {

    @Autowired
    private BillingDocumentRepo billingRepo;

    @Autowired
    private ClientRepo clientRepo;

    @Autowired
    private FirmSettingsRepo firmRepo;

    @Autowired
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public FirmSettings getOrCreateFirmSettings() {
        return firmRepo.findById(1L).map(firm -> {
            boolean updated = false;
            if ("SKC ASSOCIATES".equalsIgnoreCase(firm.getName())) {
                firm.setName("Sri Kathiravan & Co.");
                updated = true;
            }
            if ("SKC ASSOCIATES".equalsIgnoreCase(firm.getAccountName())) {
                firm.setAccountName("Sri Kathiravan & Co.");
                updated = true;
            }
            if ("contact@skcassociates.in".equalsIgnoreCase(firm.getEmail())) {
                firm.setEmail("contact@srikathiravan.com");
                updated = true;
            }
            return updated ? firmRepo.save(firm) : firm;
        }).orElseGet(() -> {
            FirmSettings def = new FirmSettings();
            return firmRepo.save(def);
        });
    }

    // ─── PUBLIC: Get Active Firm Profile & Banking Details for All Dashboards ──
    @GetMapping("/Public/FirmDetails")
    public ResponseEntity<FirmSettings> getFirmDetails() {
        return ResponseEntity.ok(getOrCreateFirmSettings());
    }

    // ─── ADMIN: Update Firm Profile & Banking Account Settings ───────────────
    @PostMapping("/Admin/Firm/Details")
    public ResponseEntity<Object> updateFirmDetails(@RequestBody Map<String, Object> payload) {
        FirmSettings firm = getOrCreateFirmSettings();
        if (payload.containsKey("name") && payload.get("name") != null) firm.setName(String.valueOf(payload.get("name")));
        if (payload.containsKey("subTitle") && payload.get("subTitle") != null) firm.setSubTitle(String.valueOf(payload.get("subTitle")));
        if (payload.containsKey("address") && payload.get("address") != null) firm.setAddress(String.valueOf(payload.get("address")));
        if (payload.containsKey("city") && payload.get("city") != null) firm.setCity(String.valueOf(payload.get("city")));
        if (payload.containsKey("phone") && payload.get("phone") != null) firm.setPhone(String.valueOf(payload.get("phone")));
        if (payload.containsKey("email") && payload.get("email") != null) firm.setEmail(String.valueOf(payload.get("email")));
        if (payload.containsKey("gstin") && payload.get("gstin") != null) firm.setGstin(String.valueOf(payload.get("gstin")));
        if (payload.containsKey("pan") && payload.get("pan") != null) firm.setPan(String.valueOf(payload.get("pan")));
        if (payload.containsKey("state") && payload.get("state") != null) firm.setState(String.valueOf(payload.get("state")));
        if (payload.containsKey("stateCode") && payload.get("stateCode") != null) firm.setStateCode(String.valueOf(payload.get("stateCode")));
        if (payload.containsKey("bankName") && payload.get("bankName") != null) firm.setBankName(String.valueOf(payload.get("bankName")));
        if (payload.containsKey("accountName") && payload.get("accountName") != null) firm.setAccountName(String.valueOf(payload.get("accountName")));
        if (payload.containsKey("accountNumber") && payload.get("accountNumber") != null) firm.setAccountNumber(String.valueOf(payload.get("accountNumber")));
        if (payload.containsKey("ifscCode") && payload.get("ifscCode") != null) firm.setIfscCode(String.valueOf(payload.get("ifscCode")));
        if (payload.containsKey("branch") && payload.get("branch") != null) firm.setBranch(String.valueOf(payload.get("branch")));
        if (payload.containsKey("upiId") && payload.get("upiId") != null) firm.setUpiId(String.valueOf(payload.get("upiId")));

        FirmSettings saved = firmRepo.save(firm);
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("message", "Official Firm & Banking details saved successfully");
        res.put("firm", saved);
        return ResponseEntity.ok(res);
    }

    // ─── ADMIN: Get All Filed Billing Documents ──────────────────────────────
    @GetMapping("/Admin/Billing/Archive")
    public ResponseEntity<List<BillingDocument>> getAdminArchive() {
        return ResponseEntity.ok(billingRepo.findAllByOrderByCreatedAtDesc());
    }

    // ─── ADMIN: File / Save an Invoice or Payment Receipt ────────────────────
    @PostMapping("/Admin/Billing/Save")
    public ResponseEntity<Object> saveDocument(@RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String docNumber = (String) payload.get("docNumber");
            if (docNumber == null || docNumber.trim().isEmpty()) {
                response.put("status", "failed");
                response.put("message", "Document number is required");
                return ResponseEntity.badRequest().body(response);
            }

            BillingDocument doc = billingRepo.findByDocNumber(docNumber).orElse(new BillingDocument());
            doc.setDocNumber(docNumber);
            doc.setType(String.valueOf(payload.getOrDefault("type", "INVOICE")));

            // Client particulars
            String clientName = (String) payload.get("clientName");
            String clientMobile = (String) payload.get("clientMobile");
            doc.setClientName(clientName);
            doc.setClientLegalName((String) payload.get("clientLegalName"));
            doc.setClientMobile(clientMobile);
            doc.setClientEmail((String) payload.get("clientEmail"));
            doc.setClientGstin((String) payload.get("clientGstin"));
            doc.setClientPan((String) payload.get("clientPan"));
            doc.setClientAddress((String) payload.get("clientAddress"));
            doc.setClientState((String) payload.get("clientState"));
            doc.setClientStateCode((String) payload.get("clientStateCode"));

            // Match Client in database to associate clientId and auto-fill email if missing
            if (clientMobile != null && !clientMobile.isEmpty()) {
                String cleanMobile = clientMobile.replaceAll("\\D", "");
                Client matchedClient = clientRepo.findByMobile(clientMobile);
                if (matchedClient == null && !cleanMobile.isEmpty()) {
                    matchedClient = clientRepo.findByMobile(cleanMobile);
                }
                if (matchedClient != null) {
                    doc.setClientId(matchedClient.getId());
                    if ((doc.getClientEmail() == null || doc.getClientEmail().trim().isEmpty()) && matchedClient.getEmail() != null) {
                        doc.setClientEmail(matchedClient.getEmail());
                    }
                }
            }

            // Dates & Times
            doc.setDocDate((String) payload.get("date"));
            doc.setDocTime((String) payload.get("time"));
            doc.setDueDate((String) payload.get("dueDate"));

            // Amounts
            doc.setTotalAmount(parseDouble(payload.get("totalAmount")));
            doc.setPaidAmount(parseDouble(payload.get("paidAmount")));
            doc.setRemainingBalance(parseDouble(payload.get("remainingBalance")));
            doc.setPaymentMode((String) payload.get("paymentMode"));
            doc.setReceiptTowards((String) payload.get("receiptTowards"));
            if (payload.containsKey("declaredNetWorthPayable") && payload.get("declaredNetWorthPayable") != null) {
                doc.setDeclaredNetWorthPayable(parseDouble(payload.get("declaredNetWorthPayable")));
            }
            if (payload.containsKey("totalOutstanding") && payload.get("totalOutstanding") != null) {
                doc.setTotalOutstanding(parseDouble(payload.get("totalOutstanding")));
            }
            if (payload.containsKey("declaredAdjustmentDiff") && payload.get("declaredAdjustmentDiff") != null) {
                doc.setDeclaredAdjustmentDiff(parseDouble(payload.get("declaredAdjustmentDiff")));
            }
            if (payload.containsKey("declaredAdjustmentNote") && payload.get("declaredAdjustmentNote") != null) {
                doc.setDeclaredAdjustmentNote(String.valueOf(payload.get("declaredAdjustmentNote")));
            }

            // JSON Blocks
            if (payload.containsKey("paymentDetails")) {
                doc.setPaymentDetailsJson(objectMapper.writeValueAsString(payload.get("paymentDetails")));
            }
            if (payload.containsKey("items")) {
                doc.setItemsJson(objectMapper.writeValueAsString(payload.get("items")));
            }
            if (payload.containsKey("intermediateRows")) {
                doc.setIntermediateRowsJson(objectMapper.writeValueAsString(payload.get("intermediateRows")));
            }
            if (payload.containsKey("customColumns")) {
                doc.setCustomColumnsJson(objectMapper.writeValueAsString(payload.get("customColumns")));
            }
            if (payload.containsKey("netWorthAuditHistory")) {
                doc.setNetWorthAuditHistoryJson(objectMapper.writeValueAsString(payload.get("netWorthAuditHistory")));
            }
            if (payload.containsKey("firm")) {
                doc.setFirmJson(objectMapper.writeValueAsString(payload.get("firm")));
            }

            BillingDocument saved = billingRepo.save(doc);

            // Asynchronously send official invoice document & message to client's email
            if (saved.getClientEmail() != null && saved.getClientEmail().contains("@")) {
                CompletableFuture.runAsync(() -> {
                    try {
                        FirmSettings currentFirm = getOrCreateFirmSettings();
                        emailService.sendInvoiceNotificationEmail(saved, currentFirm);
                    } catch (Exception ex) {
                        System.err.println("Async invoice email trigger warning: " + ex.getMessage());
                    }
                });
            }

            response.put("status", "success");
            response.put("message", "Document " + saved.getDocNumber() + " saved and emailed to client successfully");
            response.put("document", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "failed");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ─── ADMIN: Delete Document ───────────────────────────────────────────────
    @PostMapping("/Admin/Billing/Delete/{id}")
    public ResponseEntity<Object> deleteDocument(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        if (!billingRepo.existsById(id)) {
            response.put("status", "failed");
            response.put("message", "Document not found");
            return ResponseEntity.badRequest().body(response);
        }
        billingRepo.deleteById(id);
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Toggle Archive Status ─────────────────────────────────────────
    @PostMapping("/Admin/Billing/ToggleArchive/{id}")
    public ResponseEntity<Object> toggleArchive(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        BillingDocument doc = billingRepo.findById(id).orElse(null);
        if (doc == null) {
            response.put("status", "failed");
            response.put("message", "Document not found");
            return ResponseEntity.badRequest().body(response);
        }
        doc.setIsArchived(!Boolean.TRUE.equals(doc.getIsArchived()));
        billingRepo.save(doc);
        response.put("status", "success");
        response.put("isArchived", doc.getIsArchived());
        return ResponseEntity.ok(response);
    }

    // ─── CLIENT: Get Invoices & Receipts for Current Logged-in Client ─────────
    @GetMapping("/Client/Billing/Documents")
    public ResponseEntity<Object> getClientDocuments(@AuthenticationPrincipal AuthUser user) {
        Map<String, Object> errResponse = new HashMap<>();
        if (user == null || user.getId() == null) {
            errResponse.put("status", "failed");
            errResponse.put("message", "Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errResponse);
        }

        // STRICT ACCESS CONTROL: Staff is completely forbidden
        if ("STAFF".equalsIgnoreCase(user.getRole())) {
            errResponse.put("status", "forbidden");
            errResponse.put("message", "Staff members do not have permission to access client billing documents.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errResponse);
        }

        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String mobile = client.getMobile() != null ? client.getMobile().trim() : "";
        String name = client.getName() != null ? client.getName().trim() : "";
        Long clientId = client.getId();

        List<BillingDocument> docs = billingRepo.findForClient(clientId, mobile, name);
        return ResponseEntity.ok(docs);
    }

    // ─── CLIENT: Real-time Financial Billing Summary ──────────────────────────
    @GetMapping("/Client/Billing/Summary")
    public ResponseEntity<Object> getClientSummary(@AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null || user.getId() == null) {
            response.put("totalBilled", 0.0);
            response.put("totalPaid", 0.0);
            response.put("totalPendingDue", 0.0);
            response.put("documentCount", 0);
            return ResponseEntity.ok(response);
        }

        if ("STAFF".equalsIgnoreCase(user.getRole())) {
            response.put("status", "forbidden");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        Client client = clientRepo.findById(user.getId()).orElse(null);
        if (client == null) {
            response.put("totalBilled", 0.0);
            response.put("totalPaid", 0.0);
            response.put("totalPendingDue", 0.0);
            response.put("documentCount", 0);
            return ResponseEntity.ok(response);
        }

        List<BillingDocument> docs = billingRepo.findForClient(client.getId(), client.getMobile(), client.getName());

        double totalBilled = docs.stream()
                .filter(d -> "INVOICE".equalsIgnoreCase(d.getType()))
                .mapToDouble(d -> d.getTotalAmount() != null ? d.getTotalAmount() : 0.0)
                .sum();

        double totalPaid = docs.stream()
                .mapToDouble(d -> d.getPaidAmount() != null ? d.getPaidAmount() : 0.0)
                .sum();

        double totalPendingDue = docs.stream()
                .filter(d -> "INVOICE".equalsIgnoreCase(d.getType()))
                .mapToDouble(d -> d.getRemainingBalance() != null ? Math.max(0.0, d.getRemainingBalance()) : 0.0)
                .sum();

        response.put("totalBilled", totalBilled);
        response.put("totalPaid", totalPaid);
        response.put("totalPendingDue", totalPendingDue);
        response.put("documentCount", docs.size());
        return ResponseEntity.ok(response);
    }

    private Double parseDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(val).replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
