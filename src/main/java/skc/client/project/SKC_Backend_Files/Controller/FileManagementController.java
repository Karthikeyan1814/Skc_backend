package skc.client.project.SKC_Backend_Files.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.FileRepo;
import skc.client.project.SKC_Backend_Files.Repo.FolderRepo;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Service.NotificationService;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.FileEntity;
import skc.client.project.SKC_Backend_Files.entity.FolderEntity;
import skc.client.project.SKC_Backend_Files.entity.Staff;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import skc.client.project.SKC_Backend_Files.Service.EmailService;

@RestController
@RequestMapping("/skc")
public class FileManagementController {

    @Autowired private S3Client s3Client;
    @Autowired private S3Presigner s3Presigner;
    @Autowired private FileRepo fileRepo;
    @Autowired private FolderRepo folderRepo;
    @Autowired private ClientRepo clientRepo;
    @Autowired private AdminRepo adminRepo;
    @Autowired private StaffRepo staffRepo;
    @Autowired private NotificationService notificationService;
    @Autowired private EmailService emailService;

    @Value("${aws.bucketName}")
    private String bucketName;

    private String resolveUploaderDisplayName(AuthUser user) {
        if (user == null) return "System";
        try {
            if ("ADMIN".equalsIgnoreCase(user.getRole())) {
                Admin admin = adminRepo.findById(user.getId()).orElse(null);
                if (admin != null && admin.getName() != null && !admin.getName().isBlank()) {
                    return admin.getName() + " (Admin)";
                }
                return "Admin";
            } else if ("STAFF".equalsIgnoreCase(user.getRole())) {
                Staff staff = staffRepo.findById(user.getId()).orElse(null);
                if (staff != null && staff.getName() != null && !staff.getName().isBlank()) {
                    return staff.getName() + " (Staff)";
                }
                return "Staff";
            } else if ("CLIENT".equalsIgnoreCase(user.getRole())) {
                Client client = clientRepo.findById(user.getId()).orElse(null);
                if (client != null && client.getName() != null && !client.getName().isBlank()) {
                    return client.getName() + " (Client)";
                }
                return "Client";
            }
        } catch (Exception ignored) {}
        return user.getUsername() != null ? user.getUsername() : user.getRole();
    }

    // ─── ADMIN/STAFF: Search client by email or mobile ────────────────────────────
    @GetMapping("/Admin/FileManagement/SearchClient")
    public ResponseEntity<Object> searchClient(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile) {

        Client client = null;
        if (email != null && !email.isEmpty()) {
            client = clientRepo.findByEmail(email);
        } else if (mobile != null && !mobile.isEmpty()) {
            client = clientRepo.findByMobile(mobile);
        }

        if (client == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", client.getId());
        result.put("name", client.getName());
        result.put("email", client.getEmail());
        result.put("mobile", client.getMobile());
        result.put("legalname", client.getLegalname());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/Staff/FileManagement/SearchClient")
    public ResponseEntity<Object> searchClientStaff(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile) {
        return searchClient(email, mobile);
    }

    // ─── ADMIN/STAFF: Get files for a client (by category) ───────────────────────
    @GetMapping("/Admin/FileManagement/Files/{clientId}")
    public ResponseEntity<List<FileEntity>> getClientFiles(
            @PathVariable Long clientId,
            @RequestParam(required = false) String category) {

        List<FileEntity> files;
        if (category != null && !category.isEmpty()) {
            files = fileRepo.findByClientIdAndCategory(clientId, category);
        } else {
            files = fileRepo.findByClientIdOrderByCreatedAtDesc(clientId);
        }
        return ResponseEntity.ok(files);
    }

    @GetMapping("/Staff/FileManagement/Files/{clientId}")
    public ResponseEntity<List<FileEntity>> getClientFilesStaff(
            @PathVariable Long clientId,
            @RequestParam(required = false) String category) {
        return getClientFiles(clientId, category);
    }

    // ─── ADMIN/STAFF: Get all files across all clients (filtered by category and search) ───
    @GetMapping("/Admin/FileManagement/AllFiles")
    public ResponseEntity<List<FileEntity>> getAllFiles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AuthUser user) {
        return doGetAllFiles(category, search, user);
    }

    @GetMapping("/Staff/FileManagement/AllFiles")
    public ResponseEntity<List<FileEntity>> getAllFilesStaff(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AuthUser user) {
        return doGetAllFiles(category, search, user);
    }

    private ResponseEntity<List<FileEntity>> doGetAllFiles(String category, String search, AuthUser user) {
        List<FileEntity> files;
        if (category != null && !category.isEmpty() && !"ALL".equalsIgnoreCase(category)) {
            files = fileRepo.findByCategoryOrderByCreatedAtDesc(category);
        } else {
            files = fileRepo.findAllByOrderByCreatedAtDesc();
        }

        // If user is STAFF, filter to files they have access to
        if (user != null && "STAFF".equalsIgnoreCase(user.getRole())) {
            files = files.stream().filter(f -> {
                if ("PERSONAL".equalsIgnoreCase(f.getDirection())) {
                    return user.getId() != null && user.getId().equals(f.getUploadedById());
                }
                return true;
            }).toList();
        }

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            files = files.stream().filter(f -> {
                boolean matchName = f.getFileName() != null && f.getFileName().toLowerCase().contains(q);
                boolean matchClient = false;
                if (f.getClient() != null) {
                    Client c = f.getClient();
                    matchClient = (c.getName() != null && c.getName().toLowerCase().contains(q)) ||
                                  (c.getMobile() != null && c.getMobile().contains(q)) ||
                                  (c.getEmail() != null && c.getEmail().toLowerCase().contains(q)) ||
                                  (c.getGstnumber() != null && c.getGstnumber().toLowerCase().contains(q)) ||
                                  (c.getLegalname() != null && c.getLegalname().toLowerCase().contains(q));
                } else {
                    String dir = f.getDirection() != null ? f.getDirection().toLowerCase() : "";
                    String uName = f.getUserName() != null ? f.getUserName().toLowerCase() : "";
                    String uRole = f.getUploadedByRole() != null ? f.getUploadedByRole().toLowerCase() : "";
                    matchClient = dir.contains(q) || uName.contains(q) || uRole.contains(q) ||
                                  "personal".contains(q) || "staff".contains(q) || "internal".contains(q);
                }
                return matchName || matchClient;
            }).toList();
        }

        return ResponseEntity.ok(files);
    }

    // ─── ADMIN/STAFF: Get all available clients for selection in Upload modal ─────
    @GetMapping("/Admin/FileManagement/Clients")
    public ResponseEntity<List<Client>> getAllClients() {
        return ResponseEntity.ok(clientRepo.findAll());
    }

    @GetMapping("/Staff/FileManagement/Clients")
    public ResponseEntity<List<Client>> getAllClientsStaff() {
        return ResponseEntity.ok(clientRepo.findAll());
    }

    // ─── ADMIN/STAFF: Generate presigned upload URL ────────────────────────────────
    @PostMapping("/Admin/FileManagement/GenerateUploadUrl")
    public ResponseEntity<Object> generateUploadUrl(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doGenerateUploadUrl(body, user);
    }

    @PostMapping("/Staff/FileManagement/GenerateUploadUrl")
    public ResponseEntity<Object> generateUploadUrlStaff(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doGenerateUploadUrl(body, user);
    }

    private ResponseEntity<Object> doGenerateUploadUrl(Map<String, Object> body, AuthUser user) {
        Map<String, Object> response = new HashMap<>();

        String filename = (String) body.get("filename");
        String contentType = (String) body.getOrDefault("contentType", "application/octet-stream");
        String category = (String) body.getOrDefault("category", "IMPORTANT");
        String expirationDateStr = (String) body.get("expirationDate");
        String targetAudience = (String) body.getOrDefault("targetAudience", "CLIENT");

        Long clientId = null;
        if (body.get("clientId") != null && !body.get("clientId").toString().isEmpty()) {
            try {
                clientId = Long.valueOf(body.get("clientId").toString());
            } catch (Exception ignored) {}
        }

        Client client = null;
        if (clientId != null && clientId > 0) {
            client = clientRepo.findById(clientId).orElse(null);
        }

        boolean isCommonSpace = "COMMON".equalsIgnoreCase(targetAudience) || "STAFF".equalsIgnoreCase(targetAudience) || "SHARED_WITH_STAFF".equalsIgnoreCase(targetAudience);
        String objectKey;
        String direction;
        if (client != null && !isCommonSpace && !"PERSONAL".equalsIgnoreCase(targetAudience)) {
            objectKey = user.getRole() + "/" + clientId + "/" + UUID.randomUUID() + "_" + filename;
            direction = "SENT_TO_CLIENT";
        } else if (isCommonSpace) {
            objectKey = user.getRole() + "/staff_shared/" + UUID.randomUUID() + "_" + filename;
            direction = "SHARED_WITH_STAFF";
        } else {
            objectKey = user.getRole() + "/personal/" + UUID.randomUUID() + "_" + filename;
            direction = "PERSONAL";
        }

        Long folderId = null;
        if (body.get("folderId") != null && !body.get("folderId").toString().isEmpty()) {
            try {
                folderId = Long.valueOf(body.get("folderId").toString());
            } catch (Exception ignored) {}
        }
        String folderName = (String) body.get("folderName");

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presigned.url().toString();

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(filename);
        fileEntity.setContentType(contentType);
        fileEntity.setCategory(category != null ? category.toUpperCase() : "IMPORTANT");
        fileEntity.setObjectKey(objectKey);
        fileEntity.setDirection(direction);
        fileEntity.setUploadedByRole(user.getRole());
        fileEntity.setUploadedById(user.getId());
        fileEntity.setUserName(user.getUsername() != null ? user.getUsername() : user.getRole());
        fileEntity.setStatus("PENDING");
        fileEntity.setClient(client);

        if (folderId != null) {
            FolderEntity f = folderRepo.findById(folderId).orElse(null);
            if (f != null) {
                fileEntity.setFolderId(f.getId());
                fileEntity.setFolderName(f.getFolderName());
            } else if (folderName != null && !folderName.trim().isEmpty()) {
                fileEntity.setFolderName(folderName.trim());
            } else {
                fileEntity.setFolderName("General");
            }
        } else if (folderName != null && !folderName.trim().isEmpty()) {
            fileEntity.setFolderName(folderName.trim());
        } else {
            fileEntity.setFolderName("General");
        }

        if (expirationDateStr != null && !expirationDateStr.isEmpty()) {
            fileEntity.setExpirationDate(LocalDate.parse(expirationDateStr));
        }

        FileEntity saved = fileRepo.save(fileEntity);

        response.put("status", "success");
        response.put("uploadUrl", uploadUrl);
        response.put("objectKey", objectKey);
        response.put("fileId", saved.getId());
        response.put("folderId", saved.getFolderId());
        response.put("folderName", saved.getFolderName());
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN/STAFF: Direct Multipart Upload to AWS S3 & Postgres ───────────────
    @PostMapping(value = "/Admin/FileManagement/Upload", consumes = {"multipart/form-data"})
    public ResponseEntity<Object> uploadFileDirectAdmin(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "category", defaultValue = "IMPORTANT") String category,
            @RequestParam(value = "expirationDate", required = false) String expirationDate,
            @RequestParam(value = "targetAudience", required = false, defaultValue = "CLIENT") String targetAudience,
            @RequestParam(value = "customFileName", required = false) String customFileName,
            @RequestParam(value = "fileName", required = false) String fileNameParam,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "folderName", required = false) String folderNameParam,
            @AuthenticationPrincipal AuthUser user) {
        String finalName = (customFileName != null && !customFileName.trim().isEmpty()) ? customFileName : fileNameParam;
        return doDirectUpload(file, clientId, category, expirationDate, targetAudience, finalName, folderId, folderNameParam, user);
    }

    @PostMapping(value = "/Staff/FileManagement/Upload", consumes = {"multipart/form-data"})
    public ResponseEntity<Object> uploadFileDirectStaff(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clientId", required = false) Long clientId,
            @RequestParam(value = "category", defaultValue = "IMPORTANT") String category,
            @RequestParam(value = "expirationDate", required = false) String expirationDate,
            @RequestParam(value = "targetAudience", required = false, defaultValue = "CLIENT") String targetAudience,
            @RequestParam(value = "customFileName", required = false) String customFileName,
            @RequestParam(value = "fileName", required = false) String fileNameParam,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "folderName", required = false) String folderNameParam,
            @AuthenticationPrincipal AuthUser user) {
        String finalName = (customFileName != null && !customFileName.trim().isEmpty()) ? customFileName : fileNameParam;
        return doDirectUpload(file, clientId, category, expirationDate, targetAudience, finalName, folderId, folderNameParam, user);
    }

    private ResponseEntity<Object> doDirectUpload(
            MultipartFile file,
            Long clientId,
            String category,
            String expirationDateStr,
            String targetAudience,
            String customFileName,
            Long folderId,
            String folderNameParam,
            AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                response.put("status", "failed");
                response.put("message", "File cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            boolean isImportant = "IMPORTANT".equalsIgnoreCase(category);
            Client client = null;

            if (clientId != null && clientId > 0) {
                client = clientRepo.findById(clientId).orElse(null);
            }

            boolean isCommonSpace = "COMMON".equalsIgnoreCase(targetAudience) || "STAFF".equalsIgnoreCase(targetAudience) || "SHARED_WITH_STAFF".equalsIgnoreCase(targetAudience);

            // If not important and not common office space, client is strictly required
            if (!isImportant && !isCommonSpace && client == null) {
                response.put("status", "failed");
                response.put("message", "Please select a client or choose Common Office Space for this document");
                return ResponseEntity.badRequest().body(response);
            }

            String originalName = (customFileName != null && !customFileName.trim().isEmpty())
                    ? customFileName.trim()
                    : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");

            // Preserve file extension if omitted by user
            if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".") && !originalName.contains(".")) {
                String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                originalName = originalName + ext;
            }

            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            String objectKey;
            String direction;

            if (client != null && !isCommonSpace && !"PERSONAL".equalsIgnoreCase(targetAudience)) {
                objectKey = user.getRole() + "/" + client.getId() + "/" + UUID.randomUUID() + "_" + originalName;
                direction = "SENT_TO_CLIENT";
            } else if (isCommonSpace) {
                objectKey = user.getRole() + "/staff_shared/" + UUID.randomUUID() + "_" + originalName;
                direction = "SHARED_WITH_STAFF";
            } else {
                // Personal use
                objectKey = user.getRole() + "/personal/" + UUID.randomUUID() + "_" + originalName;
                direction = "PERSONAL";
            }

            // Stream directly to AWS S3 using AWS SDK
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

            // Save active entity in Postgres
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(originalName);
            fileEntity.setContentType(contentType);
            fileEntity.setCategory(category != null ? category.toUpperCase() : "IMPORTANT");
            fileEntity.setObjectKey(objectKey);
            fileEntity.setSize(file.getSize());
            fileEntity.setDirection(direction);
            fileEntity.setUploadedByRole(user.getRole());
            fileEntity.setUploadedById(user.getId());
            fileEntity.setUserName(resolveUploaderDisplayName(user));
            fileEntity.setStatus("ACTIVE");
            fileEntity.setClient(client);

            if (folderId != null) {
                FolderEntity folder = folderRepo.findById(folderId).orElse(null);
                if (folder != null) {
                    fileEntity.setFolderId(folder.getId());
                    fileEntity.setFolderName(folder.getFolderName());
                } else if (folderNameParam != null && !folderNameParam.trim().isEmpty()) {
                    fileEntity.setFolderName(folderNameParam.trim());
                } else {
                    fileEntity.setFolderName("General");
                }
            } else if (folderNameParam != null && !folderNameParam.trim().isEmpty()) {
                fileEntity.setFolderName(folderNameParam.trim());
            } else {
                fileEntity.setFolderName("General");
            }

            if (expirationDateStr != null && !expirationDateStr.trim().isEmpty()) {
                fileEntity.setExpirationDate(LocalDate.parse(expirationDateStr.trim()));
            }

            FileEntity saved = fileRepo.save(fileEntity);

            // Notify client if assigned to client
            if (client != null) {
                notificationService.notifyClient(
                        client.getId(),
                        "FILE_UPLOAD",
                        "New File Available",
                        "A new file \"" + originalName + "\" has been sent to your portal.",
                        saved.getId()
                );
            } else if ("SHARED_WITH_STAFF".equals(direction)) {
                notificationService.notifyAllStaff(
                        "IMPORTANT_FILE",
                        "New Important File Shared",
                        "Admin shared an important file: \"" + originalName + "\"",
                        saved.getId()
                );
            }

            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("id", saved.getId());
            fileMap.put("fileName", saved.getFileName());
            fileMap.put("category", saved.getCategory());
            fileMap.put("size", saved.getSize());
            fileMap.put("createdAt", saved.getCreatedAt());
            fileMap.put("userName", saved.getUserName());
            fileMap.put("uploadedByRole", saved.getUploadedByRole());
            fileMap.put("direction", saved.getDirection());
            fileMap.put("status", saved.getStatus());
            fileMap.put("folderId", saved.getFolderId());
            fileMap.put("folderName", saved.getFolderName());
            fileMap.put("canRename", true);
            fileMap.put("canDelete", true);

            response.put("status", "success");
            response.put("message", "File uploaded successfully to AWS S3");
            response.put("fileId", saved.getId());
            response.put("file", fileMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "failed");
            response.put("message", "Failed to upload file to AWS S3: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // ─── ADMIN/STAFF: Confirm file uploaded (mark ACTIVE) ─────────────────────────
    @PostMapping("/Admin/FileManagement/ConfirmUpload/{fileId}")
    public ResponseEntity<Object> confirmUpload(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, Long> body) {
        return doConfirmUpload(fileId, body);
    }

    @PostMapping("/Staff/FileManagement/ConfirmUpload/{fileId}")
    public ResponseEntity<Object> confirmUploadStaff(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, Long> body) {
        return doConfirmUpload(fileId, body);
    }

    private ResponseEntity<Object> doConfirmUpload(Long fileId, Map<String, Long> body) {
        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            response.put("message", "File not found");
            return ResponseEntity.badRequest().body(response);
        }

        Long size = body != null ? body.get("size") : null;
        if (size != null) file.setSize(size);
        file.setStatus("ACTIVE");
        fileRepo.save(file);

        // Notify client if present
        if (file.getClient() != null) {
            notificationService.notifyClient(
                    file.getClient().getId(),
                    "FILE_UPLOAD",
                    "New File Available",
                    "A new file \"" + file.getFileName() + "\" has been sent to your portal.",
                    file.getId()
            );
        } else if ("SHARED_WITH_STAFF".equals(file.getDirection())) {
            notificationService.notifyAllStaff(
                    "IMPORTANT_FILE",
                    "New Important File Shared",
                    "Admin shared an important file: \"" + file.getFileName() + "\"",
                    file.getId()
            );
        }

        response.put("status", "success");
        response.put("message", "File confirmed and client notified");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN/STAFF: Update file expiration ──────────────────────────────────────
    @PostMapping("/Admin/FileManagement/UpdateExpiry/{fileId}")
    public ResponseEntity<Object> updateExpiry(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            return ResponseEntity.badRequest().body(response);
        }
        String dateStr = body.get("expirationDate");
        if (dateStr == null || dateStr.isEmpty()) {
            file.setExpirationDate(null);
        } else {
            file.setExpirationDate(LocalDate.parse(dateStr));
        }
        file.setExtensionRequested(false);
        fileRepo.save(file);

        // Notify client if present
        if (file.getClient() != null) {
            notificationService.notifyClient(file.getClient().getId(), "FILE_EXPIRY_UPDATED",
                    "File Expiry Updated",
                    "The expiry date for \"" + file.getFileName() + "\" has been updated.",
                    file.getId());
        }

        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/Staff/FileManagement/UpdateExpiry/{fileId}")
    public ResponseEntity<Object> updateExpiryStaff(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> body) {
        return updateExpiry(fileId, body);
    }

    // ─── STAFF: Download file (BLOCKED per client data protection policy) ─────────
    @GetMapping("/Staff/Files/{fileId}/Download")
    public ResponseEntity<Object> staffDownloadFile(@PathVariable Long fileId) {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "failed");
        res.put("message", "Staff members are restricted from downloading client files. Please use the View feature to inspect the document.");
        return ResponseEntity.status(403).body(res);
    }

    // ─── VIEW FILE INLINE (PDF / Images / Docs / Public Shared Links) ───────────
    @GetMapping({"/Staff/Files/{fileId}/View", "/Admin/Files/{fileId}/View", "/Client/Files/{fileId}/View", "/Public/Files/{fileId}/View", "/Public/Files/{fileId}/Download"})
    public ResponseEntity<Object> viewFileInline(@PathVariable Long fileId) {
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) return ResponseEntity.notFound().build();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName).key(file.getObjectKey()).build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
        String contentType = file.getContentType() != null && !file.getContentType().isBlank()
                ? file.getContentType() : "application/pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(objectBytes.asByteArray());
    }

    // ─── CLIENT: Get my files ─────────────────────────────────────────────────────
    @GetMapping("/Client/Files")
    public ResponseEntity<List<FileEntity>> getMyFiles(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) String category) {

        List<FileEntity> files;
        if (category != null && !category.isEmpty()) {
            files = fileRepo.findByClientIdAndCategory(user.getId(), category);
        } else {
            files = fileRepo.findByClientIdOrderByCreatedAtDesc(user.getId());
        }
        return ResponseEntity.ok(files);
    }

    // ─── CLIENT: Download a file (if not expired) ─────────────────────────────────
    @GetMapping("/Client/Files/{fileId}/Download")
    public ResponseEntity<Object> downloadFile(@PathVariable Long fileId, @AuthenticationPrincipal AuthUser user) {
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        Map<String, Object> errorResponse = new HashMap<>();

        if (file == null) {
            errorResponse.put("status", "failed");
            errorResponse.put("message", "File not found");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        // Security: client can only access their own files
        if (!"ACTIVE".equals(file.getStatus()) || file.getClient() == null ||
                !file.getClient().getId().equals(user.getId())) {
            errorResponse.put("status", "failed");
            errorResponse.put("message", "Access denied");
            return ResponseEntity.status(403).body(errorResponse);
        }

        // Check expiry (Active members get expiration-free downloads)
        Client client = file.getClient();
        boolean isMember = client != null
                && "MEMBER".equalsIgnoreCase(client.getMembershipStatus())
                && client.getMembershipExpiry() != null
                && !client.getMembershipExpiry().isBefore(LocalDate.now());

        if (!isMember && file.isExpired()) {
            errorResponse.put("status", "expired");
            errorResponse.put("message", "This file has expired and can no longer be downloaded. Upgrade to Membership for expiration-free access.");
            return ResponseEntity.status(410).body(errorResponse);
        }

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(file.getObjectKey())
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream"))
                .body(objectBytes.asByteArray());
    }

    // ─── ADMIN: Download file (no expiry check) ────────────────────────────────────
    @GetMapping("/Admin/Files/{fileId}/Download")
    public ResponseEntity<Object> adminDownloadFile(@PathVariable Long fileId) {
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) return ResponseEntity.notFound().build();

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName).key(file.getObjectKey()).build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(
                        file.getContentType() != null ? file.getContentType() : "application/octet-stream"))
                .body(objectBytes.asByteArray());
    }

    // ─── CLIENT: Upload file to own portal ────────────────────────────────────────
    @PostMapping("/Client/Files/GenerateUploadUrl")
    public ResponseEntity<Object> clientGenerateUploadUrl(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();
        String filename = (String) body.get("filename");
        String contentType = (String) body.getOrDefault("contentType", "application/octet-stream");
        String category = (String) body.getOrDefault("category", "UPLOADED_BY_CLIENT");

        String objectKey = "CLIENT/" + user.getId() + "/" + UUID.randomUUID() + "_" + filename;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName).key(objectKey).contentType(contentType).build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)).putObjectRequest(putRequest).build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);

        // Pre-create entity
        Client client = clientRepo.findById(user.getId()).orElseThrow();
        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(filename);
        fileEntity.setContentType(contentType);
        fileEntity.setCategory(category);
        fileEntity.setObjectKey(objectKey);
        fileEntity.setDirection("UPLOADED_BY_CLIENT");
        fileEntity.setUploadedByRole("CLIENT");
        fileEntity.setUploadedById(user.getId());
        fileEntity.setUserName(client.getName() != null ? client.getName() + " (Client)" : "Client");
        fileEntity.setStatus("PENDING");
        fileEntity.setClient(client);
        FileEntity saved = fileRepo.save(fileEntity);

        response.put("status", "success");
        response.put("uploadUrl", presigned.url().toString());
        response.put("objectKey", objectKey);
        response.put("fileId", saved.getId());
        return ResponseEntity.ok(response);
    }

    // ─── CLIENT: Confirm upload to portal ─────────────────────────────────────────
    @PostMapping("/Client/Files/ConfirmUpload/{fileId}")
    public ResponseEntity<Object> clientConfirmUpload(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            return ResponseEntity.badRequest().body(response);
        }

        if (body != null && body.get("size") != null) {
            file.setSize(Long.valueOf(body.get("size").toString()));
        }
        file.setStatus("ACTIVE");
        fileRepo.save(file);

        // Notify admin and staff
        notificationService.notifyAllAdmins("FILE_UPLOAD", "Client File Upload",
                "Client uploaded a new file: \"" + file.getFileName() + "\"", file.getId());
        notificationService.notifyAllStaff("FILE_UPLOAD", "Client File Upload",
                "Client uploaded a new file: \"" + file.getFileName() + "\"", file.getId());

        response.put("status", "success");
        response.put("message", "File uploaded successfully");
        return ResponseEntity.ok(response);
    }

    // ─── CLIENT: Direct Multipart Upload to own portal ────────────────────────────
    @PostMapping(value = "/Client/Files/Upload", consumes = {"multipart/form-data"})
    public ResponseEntity<Object> clientDirectUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "UPLOADED_BY_CLIENT") String category,
            @RequestParam(value = "customFileName", required = false) String customFileName,
            @RequestParam(value = "fileName", required = false) String fileNameParam,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestParam(value = "folderName", required = false) String folderNameParam,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                response.put("status", "failed");
                response.put("message", "File cannot be empty");
                return ResponseEntity.badRequest().body(response);
            }

            Client client = clientRepo.findById(user.getId()).orElse(null);
            if (client == null) {
                response.put("status", "failed");
                response.put("message", "Client account not found");
                return ResponseEntity.badRequest().body(response);
            }

            String finalCustomName = (customFileName != null && !customFileName.trim().isEmpty()) ? customFileName : fileNameParam;
            String originalName = (finalCustomName != null && !finalCustomName.trim().isEmpty())
                    ? finalCustomName.trim()
                    : (file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");

            // Preserve file extension if omitted by user
            if (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".") && !originalName.contains(".")) {
                String ext = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
                originalName = originalName + ext;
            }

            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String objectKey = "CLIENT/" + client.getId() + "/" + UUID.randomUUID() + "_" + originalName;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putRequest, software.amazon.awssdk.core.sync.RequestBody.fromBytes(file.getBytes()));

            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(originalName);
            fileEntity.setContentType(contentType);
            fileEntity.setCategory(category != null ? category : "UPLOADED_BY_CLIENT");
            fileEntity.setObjectKey(objectKey);
            fileEntity.setSize(file.getSize());
            fileEntity.setDirection("UPLOADED_BY_CLIENT");
            fileEntity.setUploadedByRole("CLIENT");
            fileEntity.setUploadedById(user.getId());
            fileEntity.setUserName(client.getName() != null ? client.getName() + " (Client)" : "Client");
            fileEntity.setStatus("ACTIVE");
            fileEntity.setClient(client);

            // Folder validation & assignment for client
            if (folderId != null) {
                FolderEntity folder = folderRepo.findById(folderId).orElse(null);
                if (folder == null) {
                    response.put("status", "failed");
                    response.put("message", "Selected folder does not exist");
                    return ResponseEntity.badRequest().body(response);
                }
                boolean hasAccess = folder.isCommon() || (folder.getClient() != null && folder.getClient().getId().equals(client.getId()));
                if (!hasAccess) {
                    response.put("status", "failed");
                    response.put("message", "You can only upload files to your allocated folder or office common folders");
                    return ResponseEntity.status(403).body(response);
                }
                fileEntity.setFolderId(folder.getId());
                fileEntity.setFolderName(folder.getFolderName());
            } else if (folderNameParam != null && !folderNameParam.trim().isEmpty()) {
                fileEntity.setFolderName(folderNameParam.trim());
            } else {
                fileEntity.setFolderName("General");
            }

            FileEntity saved = fileRepo.save(fileEntity);

            // Notify staff & admin
            notificationService.notifyAllAdmins("FILE_UPLOAD", "Client File Upload",
                    "Client " + client.getName() + " uploaded a new file: \"" + originalName + "\"", saved.getId());
            notificationService.notifyAllStaff("FILE_UPLOAD", "Client File Upload",
                    "Client " + client.getName() + " uploaded a new file: \"" + originalName + "\"", saved.getId());

            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("id", saved.getId());
            fileMap.put("fileName", saved.getFileName());
            fileMap.put("category", saved.getCategory());
            fileMap.put("size", saved.getSize());
            fileMap.put("createdAt", saved.getCreatedAt());
            fileMap.put("userName", saved.getUserName());
            fileMap.put("uploadedByRole", saved.getUploadedByRole());
            fileMap.put("direction", saved.getDirection());
            fileMap.put("status", saved.getStatus());
            fileMap.put("folderId", saved.getFolderId());
            fileMap.put("folderName", saved.getFolderName());
            fileMap.put("canRename", true);
            fileMap.put("canDelete", true);

            response.put("status", "success");
            response.put("message", "File uploaded successfully to AWS S3");
            response.put("fileId", saved.getId());
            response.put("file", fileMap);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("status", "failed");
            response.put("message", "Failed to upload file to AWS S3: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }


    // ─── CLIENT: Request extension of file expiry ──────────────────────────────────
    @PostMapping("/Client/Files/{fileId}/RequestExtension")
    public ResponseEntity<Object> requestExtension(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthUser user) {

        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null || !file.getClient().getId().equals(user.getId())) {
            response.put("status", "failed");
            response.put("message", "File not found");
            return ResponseEntity.badRequest().body(response);
        }

        file.setExtensionRequested(true);
        fileRepo.save(file);

        // Notify admin
        notificationService.notifyAllAdmins("EXPIRY_EXTENSION_REQUEST",
                "File Extension Requested",
                "Client " + file.getClient().getName() + " has requested an extension for file: \"" + file.getFileName() + "\"",
                file.getId());

        response.put("status", "success");
        response.put("message", "Extension request submitted. Admin will review and contact you.");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Get all extension requests ────────────────────────────────────────
    @GetMapping("/Admin/FileManagement/ExtensionRequests")
    public ResponseEntity<List<FileEntity>> getExtensionRequests() {
        return ResponseEntity.ok(fileRepo.findByExtensionRequestedTrue());
    }

    // ─── ADMIN: Delete file ────────────────────────────────────────────────────────
    @DeleteMapping({"/Admin/FileManagement/Delete/{fileId}", "/Admin/FileManagement/Files/{fileId}"})
    public ResponseEntity<Object> deleteFileAdmin(@PathVariable Long fileId, @AuthenticationPrincipal AuthUser user) {
        return doDeleteFile(fileId, user, false);
    }

    // ─── STAFF: Delete file (BLOCKED - staff must submit deletion request with reason) ────
    @DeleteMapping({"/Staff/FileManagement/Delete/{fileId}", "/Staff/FileManagement/Files/{fileId}"})
    public ResponseEntity<Object> deleteFileStaff(@PathVariable Long fileId, @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "failed");
        response.put("message", "Staff members do not have direct file deletion privileges. Please submit a Deletion Request with reason for Administrator approval.");
        return ResponseEntity.status(403).body(response);
    }

    // ─── STAFF: Request File Deletion with Reason ─────────────────────────────────
    @PostMapping("/Staff/FileManagement/RequestDelete/{fileId}")
    public ResponseEntity<Object> staffRequestDelete(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            response.put("message", "File record not found");
            return ResponseEntity.status(404).body(response);
        }

        String reason = (body != null && body.containsKey("reason") && body.get("reason") != null)
                ? (String) body.get("reason") : "No reason specified";
        String staffName = resolveUploaderDisplayName(user);
        String requestedByInfo = staffName + " (ID: " + (user != null ? user.getId() : "N/A") + ")";

        file.setDeleteRequested(true);
        file.setDeleteReason(reason);
        file.setDeleteRequestedBy(requestedByInfo);
        file.setDeleteRequestedById(user != null ? user.getId() : null);
        file.setDeleteRequestedAt(LocalDateTime.now());
        fileRepo.save(file);

        // Notify Admin of deletion request
        try {
            notificationService.notifyAllAdmins(
                    "FILE_DELETE_REQUEST",
                    "⚠️ Deletion Requested by " + staffName,
                    "Staff " + staffName + " requested permanent deletion of document '" + file.getFileName() + "'. Reason: " + reason,
                    file.getId()
            );
        } catch (Exception ignored) {}

        response.put("status", "success");
        response.put("message", "Deletion request submitted to Administrator for review.");
        return ResponseEntity.ok(response);
    }

    // ─── STAFF: Batch Request Deletion ────────────────────────────────────────────
    @PostMapping("/Staff/FileManagement/RequestDeleteBatch")
    public ResponseEntity<Object> staffRequestDeleteBatch(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        List<?> rawIds = (List<?>) body.get("fileIds");
        String reason = body.containsKey("reason") && body.get("reason") != null
                ? (String) body.get("reason") : "Bulk deletion request from staff";

        if (rawIds == null || rawIds.isEmpty()) {
            response.put("status", "failed");
            response.put("message", "No files specified for deletion request");
            return ResponseEntity.badRequest().body(response);
        }

        String staffName = resolveUploaderDisplayName(user);
        String requestedByInfo = staffName + " (ID: " + (user != null ? user.getId() : "N/A") + ")";

        int count = 0;
        for (Object item : rawIds) {
            try {
                Long fileId = Long.valueOf(item.toString());
                FileEntity file = fileRepo.findById(fileId).orElse(null);
                if (file != null) {
                    file.setDeleteRequested(true);
                    file.setDeleteReason(reason);
                    file.setDeleteRequestedBy(requestedByInfo);
                    file.setDeleteRequestedById(user != null ? user.getId() : null);
                    file.setDeleteRequestedAt(LocalDateTime.now());
                    fileRepo.save(file);
                    count++;
                }
            } catch (Exception ignored) {}
        }

        try {
            notificationService.notifyAllAdmins(
                    "FILE_DELETE_REQUEST",
                    "⚠️ Batch Deletion Requested (" + count + " files)",
                    "Staff " + staffName + " requested deletion of " + count + " file(s). Reason: " + reason,
                    null
            );
        } catch (Exception ignored) {}

        response.put("status", "success");
        response.put("message", count + " deletion request(s) submitted to Administrator.");
        response.put("count", count);
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Get Pending Deletion Requests ──────────────────────────────────────
    @GetMapping("/Admin/FileManagement/DeleteRequests")
    public ResponseEntity<List<FileEntity>> getPendingDeleteRequests() {
        return ResponseEntity.ok(fileRepo.findByDeleteRequestedTrueOrderByDeleteRequestedAtDesc());
    }

    // ─── ADMIN: Approve Deletion Request (Permanently delete) ──────────────────────
    @PostMapping("/Admin/FileManagement/ApproveDelete/{fileId}")
    public ResponseEntity<Object> approveDeleteRequest(
            @PathVariable Long fileId,
            @AuthenticationPrincipal AuthUser user) {
        return doDeleteFile(fileId, user, false);
    }

    // ─── ADMIN: Reject Deletion Request ───────────────────────────────────────────
    @PostMapping("/Admin/FileManagement/RejectDelete/{fileId}")
    public ResponseEntity<Object> rejectDeleteRequest(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            response.put("message", "File record not found");
            return ResponseEntity.status(404).body(response);
        }

        String note = (body != null && body.containsKey("note")) ? (String) body.get("note") : "Deletion request rejected by Administrator.";
        Long staffId = file.getDeleteRequestedById();

        file.setDeleteRequested(false);
        file.setDeleteReason(null);
        fileRepo.save(file);

        if (staffId != null) {
            try {
                notificationService.createNotification(
                        "STAFF",
                        staffId,
                        "FILE_DELETE_REJECTED",
                        "File Deletion Request Declined",
                        "Your request to delete document '" + file.getFileName() + "' was reviewed and declined by Administrator. Note: " + note,
                        file.getId()
                );
            } catch (Exception ignored) {}
        }

        response.put("status", "success");
        response.put("message", "Deletion request rejected.");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Automated Email Sharing ───────────────────────────────────────────
    @PostMapping("/Admin/FileManagement/ShareEmail")
    public ResponseEntity<Object> shareFileViaEmail(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> res = new HashMap<>();
        if (body == null || !body.containsKey("fileId") || !body.containsKey("receiverEmail")) {
            res.put("status", "failed");
            res.put("message", "File ID and Receiver Email are required.");
            return ResponseEntity.badRequest().body(res);
        }

        Long fileId = Long.valueOf(body.get("fileId").toString());
        String receiverEmail = (String) body.get("receiverEmail");
        String subject = (String) body.get("subject");
        String customMessage = (String) body.get("message");

        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            res.put("status", "failed");
            res.put("message", "File not found.");
            return ResponseEntity.status(404).body(res);
        }

        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName).key(file.getObjectKey()).build();
            ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(getRequest);
            byte[] data = objectBytes.asByteArray();

            emailService.sendDocumentEmailWithAttachment(
                    receiverEmail.trim(),
                    subject,
                    customMessage,
                    file.getFileName(),
                    data,
                    file.getContentType()
            );

            res.put("status", "success");
            res.put("message", "Document successfully emailed to " + receiverEmail);
            return ResponseEntity.ok(res);
        } catch (Exception ex) {
            res.put("status", "failed");
            res.put("message", "Failed to send email: " + ex.getMessage());
            return ResponseEntity.status(500).body(res);
        }
    }

    // ─── CLIENT: Delete file ───────────────────────────────────────────────────────
    @DeleteMapping({"/Client/Files/{fileId}", "/Client/Files/{fileId}/Delete"})
    public ResponseEntity<Object> deleteFileClient(@PathVariable Long fileId, @AuthenticationPrincipal AuthUser user) {
        return doDeleteFile(fileId, user, true);
    }

    private ResponseEntity<Object> doDeleteFile(Long fileId, AuthUser user, boolean clientOnly) {
        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            response.put("message", "File record not found");
            return ResponseEntity.status(404).body(response);
        }

        if (clientOnly) {
            if (user == null || file.getClient() == null || !file.getClient().getId().equals(user.getId())) {
                response.put("status", "failed");
                response.put("message", "Unauthorized to delete this file");
                return ResponseEntity.status(403).body(response);
            }
        }

        // Delete from AWS S3
        if (file.getObjectKey() != null && !file.getObjectKey().isEmpty()) {
            try {
                DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(file.getObjectKey())
                        .build();
                s3Client.deleteObject(delReq);
            } catch (Exception ex) {
                System.err.println("Warning deleting object from AWS S3: " + ex.getMessage());
            }
        }

        fileRepo.deleteById(fileId);
        response.put("status", "success");
        response.put("message", "File deleted successfully from AWS S3 and database.");
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN & STAFF: Delete multiple files in batch ───────────────────────────
    @PostMapping({"/Admin/FileManagement/DeleteBatch", "/Staff/FileManagement/DeleteBatch"})
    public ResponseEntity<Object> deleteFilesBatch(
            @RequestBody Map<String, List<Long>> request,
            @AuthenticationPrincipal AuthUser user) {
        List<Long> fileIds = request.get("fileIds");
        if (fileIds == null || fileIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "No files specified for deletion"));
        }
        int deletedCount = 0;
        for (Long fileId : fileIds) {
            try {
                FileEntity file = fileRepo.findById(fileId).orElse(null);
                if (file != null) {
                    if (file.getObjectKey() != null && !file.getObjectKey().isEmpty()) {
                        try {
                            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(file.getObjectKey())
                                    .build();
                            s3Client.deleteObject(delReq);
                        } catch (Exception ex) {
                            System.err.println("Warning deleting object from AWS S3: " + ex.getMessage());
                        }
                    }
                    fileRepo.deleteById(fileId);
                    deletedCount++;
                }
            } catch (Exception e) {
                System.err.println("Error deleting file " + fileId + ": " + e.getMessage());
            }
        }
        Map<String, Object> res = new HashMap<>();
        res.put("status", "success");
        res.put("message", deletedCount + " document(s) permanently deleted from AWS S3 and database.");
        res.put("deletedCount", deletedCount);
        return ResponseEntity.ok(res);
    }

    // ─── ADMIN: Rename file ────────────────────────────────────────────────────────
    @PutMapping("/Admin/FileManagement/Rename/{fileId}")
    public ResponseEntity<Object> renameFileAdmin(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doRenameFile(fileId, body, user, false);
    }

    // ─── STAFF: Rename file ────────────────────────────────────────────────────────
    @PutMapping("/Staff/FileManagement/Rename/{fileId}")
    public ResponseEntity<Object> renameFileStaff(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doRenameFile(fileId, body, user, false);
    }

    // ─── CLIENT: Rename file ───────────────────────────────────────────────────────
    @PutMapping("/Client/Files/{fileId}/Rename")
    public ResponseEntity<Object> renameFileClient(
            @PathVariable Long fileId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doRenameFile(fileId, body, user, true);
    }

    private ResponseEntity<Object> doRenameFile(Long fileId, Map<String, Object> body, AuthUser user, boolean clientOnly) {
        Map<String, Object> response = new HashMap<>();
        FileEntity file = fileRepo.findById(fileId).orElse(null);
        if (file == null) {
            response.put("status", "failed");
            response.put("message", "File record not found");
            return ResponseEntity.status(404).body(response);
        }

        if (clientOnly) {
            if (user == null || file.getClient() == null || !file.getClient().getId().equals(user.getId())) {
                response.put("status", "failed");
                response.put("message", "Unauthorized to rename this file");
                return ResponseEntity.status(403).body(response);
            }
        }

        String newName = body != null && body.containsKey("newFileName") ? (String) body.get("newFileName") : null;
        if (newName == null && body != null && body.containsKey("fileName")) {
            newName = (String) body.get("fileName");
        }

        if (newName == null || newName.trim().isEmpty()) {
            response.put("status", "failed");
            response.put("message", "File name cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        // Preserve file extension if omitted by user
        String oldName = file.getFileName();
        if (oldName != null && oldName.contains(".") && !newName.contains(".")) {
            String ext = oldName.substring(oldName.lastIndexOf("."));
            newName = newName.trim() + ext;
        }

        file.setFileName(newName.trim());
        fileRepo.save(file);

        response.put("status", "success");
        response.put("message", "File renamed successfully");
        response.put("fileName", file.getFileName());
        return ResponseEntity.ok(response);
    }

    // ─── ADMIN: Purge ALL files from AWS S3 & Database (Fresh Reset) ──────────────
    @DeleteMapping("/Admin/FileManagement/PurgeAll")
    public ResponseEntity<Object> purgeAllFiles() {
        Map<String, Object> res = new HashMap<>();
        try {
            int s3Deleted = 0;
            try {
                ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucketName).build();
                ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);
                if (!listRes.contents().isEmpty()) {
                    List<ObjectIdentifier> toDelete = listRes.contents().stream()
                            .map(o -> ObjectIdentifier.builder().key(o.key()).build())
                            .toList();
                    DeleteObjectsRequest delReq = DeleteObjectsRequest.builder()
                            .bucket(bucketName)
                            .delete(Delete.builder().objects(toDelete).build())
                            .build();
                    s3Client.deleteObjects(delReq);
                    s3Deleted = toDelete.size();
                }
            } catch (Exception s3Ex) {
                System.err.println("S3 Purge Warning: " + s3Ex.getMessage());
            }

            long dbDeleted = fileRepo.count();
            fileRepo.deleteAll();
            res.put("status", "success");
            res.put("message", "All files deleted from AWS S3 and database successfully.");
            res.put("s3Deleted", s3Deleted);
            res.put("dbDeleted", dbDeleted);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("status", "failed");
            res.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(res);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════════
    // FOLDER MANAGEMENT API (Admin, Staff, Client)
    // ══════════════════════════════════════════════════════════════════════════════

    // ─── ADMIN: Get all folders with counts (optional category filter) ───────────
    @GetMapping("/Admin/Folders")
    public ResponseEntity<List<Map<String, Object>>> getAdminFolders(
            @RequestParam(value = "category", required = false) String category) {
        List<FolderEntity> folders = folderRepo.findAllByOrderByCreatedAtDesc();
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            folders = folders.stream()
                    .filter(f -> category.equalsIgnoreCase(f.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(formatFoldersWithCounts(folders));
    }

    // ─── STAFF: Get all folders with counts (optional category filter) ───────────
    @GetMapping("/Staff/Folders")
    public ResponseEntity<List<Map<String, Object>>> getStaffFolders(
            @RequestParam(value = "category", required = false) String category) {
        List<FolderEntity> folders = folderRepo.findAllByOrderByCreatedAtDesc();
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            folders = folders.stream()
                    .filter(f -> category.equalsIgnoreCase(f.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(formatFoldersWithCounts(folders));
    }

    // ─── CLIENT: Get folders available to authenticated client ───────────────────
    @GetMapping("/Client/Folders")
    public ResponseEntity<List<Map<String, Object>>> getClientFolders(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(value = "category", required = false) String category) {
        if (user == null) return ResponseEntity.status(401).build();
        List<FolderEntity> folders = folderRepo.findAvailableForClient(user.getId());
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            folders = folders.stream()
                    .filter(f -> category.equalsIgnoreCase(f.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }
        return ResponseEntity.ok(formatFoldersWithCounts(folders));
    }

    // ─── ADMIN/STAFF: Get folders available for a specific client ─────────────────
    @GetMapping({"/Admin/Folders/ForClient/{clientId}", "/Staff/Folders/ForClient/{clientId}"})
    public ResponseEntity<List<Map<String, Object>>> getFoldersForSpecificClient(@PathVariable Long clientId) {
        List<FolderEntity> folders = folderRepo.findAvailableForClient(clientId);
        return ResponseEntity.ok(formatFoldersWithCounts(folders));
    }

    // ─── ADMIN: Create Folder ─────────────────────────────────────────────────────
    @PostMapping("/Admin/Folders")
    public ResponseEntity<Object> createFolderAdmin(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doCreateFolder(body, user);
    }

    // ─── STAFF: Create Folder ─────────────────────────────────────────────────────
    @PostMapping("/Staff/Folders")
    public ResponseEntity<Object> createFolderStaff(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        return doCreateFolder(body, user);
    }

    private ResponseEntity<Object> doCreateFolder(Map<String, Object> body, AuthUser user) {
        Map<String, Object> res = new HashMap<>();
        String folderName = body != null ? (String) body.get("folderName") : null;
        if (folderName == null || folderName.trim().isEmpty()) {
            res.put("status", "failed");
            res.put("message", "Folder name is required");
            return ResponseEntity.badRequest().body(res);
        }

        String description = body.containsKey("description") ? (String) body.get("description") : "";
        String color = body.containsKey("folderColor") && body.get("folderColor") != null
                ? (String) body.get("folderColor") : "#4f46e5";

        String category = body.containsKey("category") && body.get("category") != null
                ? body.get("category").toString().trim() : "OTHER";
        if (category.equalsIgnoreCase("ALL") || category.isBlank()) {
            category = "OTHER";
        }

        boolean isCommon = !body.containsKey("isCommon") || Boolean.parseBoolean(body.get("isCommon").toString());

        Client client = null;
        if (!isCommon && body.get("clientId") != null && !body.get("clientId").toString().isEmpty()) {
            try {
                Long clientId = Long.valueOf(body.get("clientId").toString());
                client = clientRepo.findById(clientId).orElse(null);
            } catch (Exception ignored) {}
        }

        FolderEntity folder = new FolderEntity();
        folder.setFolderName(folderName.trim());
        folder.setDescription(description != null ? description.trim() : "");
        folder.setFolderColor(color);
        folder.setCategory(category);
        folder.setCommon(isCommon);
        folder.setClient(client);
        folder.setCreatedByRole(user.getRole());
        folder.setCreatedById(user.getId());

        FolderEntity saved = folderRepo.save(folder);

        Map<String, Object> folderMap = buildFolderDto(saved, 0L);
        res.put("status", "success");
        res.put("message", "Folder created successfully");
        res.put("folder", folderMap);
        return ResponseEntity.ok(res);
    }

    // ─── ADMIN/STAFF: Rename / Edit Folder ─────────────────────────────────────────
    @PutMapping({"/Admin/Folders/{folderId}", "/Staff/Folders/{folderId}"})
    public ResponseEntity<Object> renameFolder(
            @PathVariable Long folderId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> res = new HashMap<>();
        FolderEntity folder = folderRepo.findById(folderId).orElse(null);
        if (folder == null) {
            res.put("status", "failed");
            res.put("message", "Folder not found");
            return ResponseEntity.status(404).body(res);
        }

        String newName = body != null ? (String) body.get("folderName") : null;
        if (newName == null || newName.trim().isEmpty()) {
            res.put("status", "failed");
            res.put("message", "Folder name cannot be empty");
            return ResponseEntity.badRequest().body(res);
        }

        folder.setFolderName(newName.trim());
        if (body.containsKey("description")) {
            folder.setDescription((String) body.get("description"));
        }
        if (body.containsKey("folderColor") && body.get("folderColor") != null) {
            folder.setFolderColor((String) body.get("folderColor"));
        }
        if (body.containsKey("category") && body.get("category") != null) {
            String cat = body.get("category").toString().trim();
            if (!cat.equalsIgnoreCase("ALL")) {
                folder.setCategory(cat);
            }
        }
        if (body.containsKey("isCommon")) {
            boolean isCommon = Boolean.parseBoolean(body.get("isCommon").toString());
            folder.setCommon(isCommon);
            if (isCommon) {
                folder.setClient(null);
            } else if (body.get("clientId") != null) {
                try {
                    Long cid = Long.valueOf(body.get("clientId").toString());
                    folder.setClient(clientRepo.findById(cid).orElse(null));
                } catch (Exception ignored) {}
            }
        }

        FolderEntity saved = folderRepo.save(folder);

        // Update all existing files in this folder with the new folder name
        List<FileEntity> filesInFolder = fileRepo.findByFolderId(folderId);
        for (FileEntity f : filesInFolder) {
            f.setFolderName(saved.getFolderName());
        }
        fileRepo.saveAll(filesInFolder);

        long count = fileRepo.countByFolderId(saved.getId());
        res.put("status", "success");
        res.put("message", "Folder renamed successfully");
        res.put("folder", buildFolderDto(saved, count));
        return ResponseEntity.ok(res);
    }

    // ─── ADMIN: Delete Folder ─────────────────────────────────────────────────────
    @DeleteMapping("/Admin/Folders/{folderId}")
    public ResponseEntity<Object> deleteFolder(
            @PathVariable Long folderId,
            @AuthenticationPrincipal AuthUser user) {
        Map<String, Object> res = new HashMap<>();
        FolderEntity folder = folderRepo.findById(folderId).orElse(null);
        if (folder == null) {
            res.put("status", "failed");
            res.put("message", "Folder not found");
            return ResponseEntity.status(404).body(res);
        }

        // Unlink files in this folder to General
        List<FileEntity> filesInFolder = fileRepo.findByFolderId(folderId);
        for (FileEntity f : filesInFolder) {
            f.setFolderId(null);
            f.setFolderName("General");
        }
        fileRepo.saveAll(filesInFolder);

        folderRepo.delete(folder);

        res.put("status", "success");
        res.put("message", "Folder deleted successfully. Attached files were unlinked.");
        return ResponseEntity.ok(res);
    }

    private List<Map<String, Object>> formatFoldersWithCounts(List<FolderEntity> folders) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (FolderEntity f : folders) {
            long count = fileRepo.countByFolderId(f.getId());
            result.add(buildFolderDto(f, count));
        }
        return result;
    }

    private Map<String, Object> buildFolderDto(FolderEntity f, long count) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", f.getId());
        map.put("folderName", f.getFolderName());
        map.put("description", f.getDescription());
        map.put("folderColor", f.getFolderColor());
        map.put("category", f.getCategory());
        map.put("isCommon", f.isCommon());
        map.put("fileCount", count);
        map.put("createdAt", f.getCreatedAt());
        map.put("updatedAt", f.getUpdatedAt());
        map.put("createdByRole", f.getCreatedByRole());
        if (f.getClient() != null) {
            Map<String, Object> clientMap = new HashMap<>();
            clientMap.put("id", f.getClient().getId());
            clientMap.put("name", f.getClient().getName());
            clientMap.put("email", f.getClient().getEmail());
            clientMap.put("mobile", f.getClient().getMobile());
            clientMap.put("legalname", f.getClient().getLegalname());
            map.put("client", clientMap);
            map.put("allocatedTo", f.getClient().getName());
        } else {
            map.put("client", null);
            map.put("allocatedTo", f.isCommon() ? "All Clients (Common)" : "General");
        }
        return map;
    }
}
