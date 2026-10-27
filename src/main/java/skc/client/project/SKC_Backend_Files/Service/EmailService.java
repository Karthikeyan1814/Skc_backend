package skc.client.project.SKC_Backend_Files.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import skc.client.project.SKC_Backend_Files.entity.BillingDocument;
import skc.client.project.SKC_Backend_Files.entity.FirmSettings;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@srikathiravan.com}")
    private String fromEmail;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    /**
     * Sends a general HTML email. Supports both standard JavaMail SMTP and Brevo HTTPS REST API (Port 443).
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) throws MessagingException {
        System.out.println(">>> [EmailService] Sending email to: " + to + " | Subject: " + subject);

        // 1. If Brevo HTTP API Key is configured, prioritize HTTPS API over port 443 (Render free tier friendly!)
        if (brevoApiKey != null && !brevoApiKey.isBlank()) {
            boolean sent = sendViaBrevoApi(to, subject, htmlBody);
            if (sent) return;
        }

        // 2. Standard SMTP delivery
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            System.out.println(">>> [EmailService] SUCCESS: Email delivered to: " + to);
        } catch (Exception ex) {
            System.err.println(">>> [EmailService] SMTP delivery failed to " + to + ": " + ex.getMessage());
            // If Brevo is configured, fallback to Brevo API
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                boolean sent = sendViaBrevoApi(to, subject, htmlBody);
                if (sent) return;
            }
            if (ex instanceof MessagingException) {
                throw (MessagingException) ex;
            }
            throw new MessagingException("Failed to deliver email: " + ex.getMessage(), ex);
        }
    }

    /**
     * Dispatches transactional email via Brevo HTTPS REST API (Port 443).
     * Bypasses hosting provider firewall restrictions on SMTP ports (25, 465, 587).
     */
    public boolean sendViaBrevoApi(String to, String subject, String htmlBody) {
        try {
            System.out.println(">>> [EmailService] Dispatching via Brevo HTTPS REST API (Port 443)...");
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .build();

            String jsonPayload = String.format(
                    "{\"sender\":{\"name\":\"Sri Kathiravan & Co.\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"htmlContent\":\"%s\"}",
                    escapeJson(fromEmail),
                    escapeJson(to),
                    escapeJson(subject),
                    escapeJson(htmlBody)
            );

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("api-key", brevoApiKey.trim())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload, java.nio.charset.StandardCharsets.UTF_8))
                    .timeout(java.time.Duration.ofSeconds(15))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println(">>> [EmailService] SUCCESS via Brevo HTTPS API: Delivered to " + to + " (HTTP " + response.statusCode() + ")");
                return true;
            } else {
                System.err.println(">>> [EmailService] Brevo HTTPS API failed with status " + response.statusCode() + ": " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println(">>> [EmailService] Brevo HTTPS API exception: " + e.getMessage());
            return false;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < ' ') {
                        String t = "000" + Integer.toHexString(ch);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(ch);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Sends membership approval email with QR code attachment.
     * The QR image should be placed at: src/main/resources/static/payment-qr.png
     */
    public void sendMembershipApprovalEmail(String to, String clientName, String adminNote) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("🎉 Membership Application Approved – Sri Kathiravan & Co");

            String html = buildApprovalEmailHtml(clientName, adminNote);
            helper.setText(html, true);

            // Attach QR code if it exists
            ClassPathResource qrResource = new ClassPathResource("static/payment-qr.png");
            if (qrResource.exists()) {
                helper.addAttachment("payment-qr.png", qrResource);
                // Also inline it
                helper.addInline("paymentQR", qrResource);
            }

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send membership approval email: " + e.getMessage());
        }
    }

    /**
     * Sends membership rejection email.
     */
    public void sendMembershipRejectionEmail(String to, String clientName, String reason) {
        try {
            String html = buildRejectionEmailHtml(clientName, reason);
            sendHtmlEmail(to, "Update on your Membership Application – Sri Kathiravan & Co", html);
        } catch (Exception e) {
            System.err.println("Failed to send rejection email: " + e.getMessage());
        }
    }

    /**
     * Sends file expiry notification to client.
     */
    public void sendFileExpiryEmail(String to, String clientName, String fileName, String expiryDate) {
        try {
            String html = buildFileExpiryEmailHtml(clientName, fileName, expiryDate);
            sendHtmlEmail(to, "File Expiry Notice – " + fileName, html);
        } catch (Exception e) {
            System.err.println("Failed to send file expiry email: " + e.getMessage());
        }
    }

    /**
     * Sends document to a receiver with optional custom note and attached file data.
     */
    public void sendDocumentEmailWithAttachment(String to, String subject, String messageText, String fileName, byte[] fileData, String contentType) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject != null && !subject.isBlank() ? subject : "Document from Sri Kathiravan & Co – " + fileName);

            String customMsgBlock = (messageText != null && !messageText.isBlank()) ?
                    "<div style=\"background:#f1f5f9;padding:12px 16px;border-radius:8px;margin:16px 0;font-size:13px;color:#334155;\"><strong>Message:</strong> " + messageText + "</div>" : "";

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                      <div style="max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                        <div style="background:linear-gradient(135deg,#1e1b4b,#312e81);padding:26px;text-align:center;">
                          <h1 style="color:#f59e0b;margin:0;font-size:22px;">Sri Kathiravan & Co</h1>
                          <p style="color:#cbd5e1;margin:6px 0 0;font-size:13px;">Document Distribution Service</p>
                        </div>
                        <div style="padding:28px;">
                          <h2 style="color:#1e293b;margin-top:0;font-size:18px;">Official Document Shared with You</h2>
                          <p style="color:#475569;line-height:1.6;font-size:14px;">
                            Please find the attached document: <strong style="color:#1e1b4b;">%s</strong>
                          </p>
                          %s
                          <div style="background:#f8fafc;border:1px solid #e2e8f0;border-left:4px solid #4f46e5;padding:14px;border-radius:6px;margin:20px 0;">
                            <p style="margin:0;color:#334155;font-size:13px;">
                              <strong>Attachment:</strong> %s<br>
                              <span style="color:#64748b;font-size:12px;">The file has been attached directly to this email for your reference and auditing records.</span>
                            </p>
                          </div>
                          <hr style="border:none;border-top:1px solid #e2e8f0;margin:24px 0;">
                          <p style="color:#94a3b8;font-size:12px;text-align:center;margin:0;">
                            Sri Kathiravan & Co | Chartered Accountants<br>
                            This email was generated automatically by our official document management portal.
                          </p>
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(fileName, customMsgBlock, fileName);

            helper.setText(html, true);

            if (fileData != null && fileData.length > 0) {
                org.springframework.core.io.ByteArrayResource resource = new org.springframework.core.io.ByteArrayResource(fileData);
                helper.addAttachment(fileName, resource, contentType != null ? contentType : "application/octet-stream");
            }

            mailSender.send(message);
            System.out.println(">>> [EmailService] Document email delivered to " + to);
        } catch (Exception e) {
            System.err.println("Failed to send document email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }

    // ─── HTML BUILDERS ────────────────────────────────────────────────────────────

    private String buildApprovalEmailHtml(String name, String note) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                    <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:30px;text-align:center;">
                      <h1 style="color:#f59e0b;margin:0;font-size:24px;">Sri Kathiravan & Co</h1>
                      <p style="color:#9ca3af;margin:8px 0 0;">Membership Approved ✅</p>
                    </div>
                    <div style="padding:30px;">
                      <h2 style="color:#1a1a2e;">Dear %s,</h2>
                      <p style="color:#4b5563;line-height:1.6;">We are pleased to inform you that your membership application has been <strong style="color:#22c55e;">approved</strong>.</p>
                      %s
                      <p style="color:#4b5563;line-height:1.6;">Please find the payment QR code attached. Complete the payment to activate your membership portal access.</p>
                      <div style="background:#fef3c7;border-left:4px solid #f59e0b;padding:15px;border-radius:4px;margin:20px 0;">
                        <p style="margin:0;color:#92400e;"><strong>Next Steps:</strong> Complete the payment via the attached QR code. Our team will activate your account within 24 hours of payment confirmation.</p>
                      </div>
                      <hr style="border:1px solid #e5e7eb;margin:20px 0;">
                      <p style="color:#9ca3af;font-size:12px;text-align:center;">Sri Kathiravan & Co | Chartered Accountants<br>If you have any questions, contact us at our office.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, note != null && !note.isEmpty() ?
                "<p style=\"color:#4b5563;\"><strong>Note from Admin:</strong> " + note + "</p>" : "");
    }

    private String buildRejectionEmailHtml(String name, String reason) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                    <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:30px;text-align:center;">
                      <h1 style="color:#f59e0b;margin:0;font-size:24px;">Sri Kathiravan & Co</h1>
                      <p style="color:#9ca3af;margin:8px 0 0;">Membership Application Update</p>
                    </div>
                    <div style="padding:30px;">
                      <h2 style="color:#1a1a2e;">Dear %s,</h2>
                      <p style="color:#4b5563;line-height:1.6;">Thank you for your interest in our membership program. After reviewing your application, we are unable to approve it at this time.</p>
                      %s
                      <p style="color:#4b5563;line-height:1.6;">You are welcome to reapply after addressing the above points or contact us for further assistance.</p>
                      <hr style="border:1px solid #e5e7eb;margin:20px 0;">
                      <p style="color:#9ca3af;font-size:12px;text-align:center;">Sri Kathiravan & Co | Chartered Accountants</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, reason != null && !reason.isEmpty() ?
                "<div style=\"background:#fee2e2;border-left:4px solid #ef4444;padding:15px;border-radius:4px;margin:20px 0;\"><p style=\"margin:0;color:#991b1b;\"><strong>Reason:</strong> " + reason + "</p></div>" : "");
    }

    private String buildFileExpiryEmailHtml(String name, String fileName, String expiryDate) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:30px;text-align:center;">
                      <h1 style="color:#f59e0b;margin:0;font-size:24px;">Sri Kathiravan & Co</h1>
                      <p style="color:#9ca3af;margin:8px 0 0;">File Expiry Notice</p>
                    </div>
                    <div style="padding:30px;">
                      <h2 style="color:#1a1a2e;">Dear %s,</h2>
                      <p style="color:#4b5563;">The following file in your portal will expire on <strong>%s</strong>:</p>
                      <div style="background:#f3f4f6;padding:15px;border-radius:8px;margin:15px 0;">
                        <p style="margin:0;color:#1f2937;font-weight:bold;">📄 %s</p>
                      </div>
                      <p style="color:#4b5563;">Please download it before the expiry date. If you need an extension, you can request one from your portal.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(name, expiryDate, fileName);
    }

    /**
     * Sends OTP for password reset.
     */
    public void sendOtpEmail(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Password Reset Verification Code – Sri Kathiravan & Co");
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:500px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 15px rgba(0,0,0,0.08);">
                    <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:24px;text-align:center;">
                      <h2 style="color:#f59e0b;margin:0;font-size:22px;">Sri Kathiravan & Co</h2>
                      <p style="color:#9ca3af;margin:6px 0 0;font-size:13px;">Account Security & Verification</p>
                    </div>
                    <div style="padding:28px;text-align:center;">
                      <h3 style="color:#1a1a2e;margin-top:0;">Password Reset Request</h3>
                      <p style="color:#4b5563;font-size:14px;line-height:1.5;">Use the following One-Time Password (OTP) to reset your password. This code will expire in 10 minutes.</p>
                      <div style="margin:24px auto;padding:12px 24px;background:#fef3c7;border:1px dashed #d97706;border-radius:8px;display:inline-block;">
                        <span style="font-size:32px;letter-spacing:6px;font-weight:bold;color:#92400e;">%s</span>
                      </div>
                      <p style="color:#9ca3af;font-size:12px;margin-bottom:0;">If you didn't request this code, you can safely ignore this email.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otp);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Note: SMTP send failed (" + e.getMessage() + "). OTP generated for dev: " + otp);
        }
    }

    /**
     * Sends confirmation to applicant when they submit membership request.
     */
    public void sendMembershipRequestConfirmation(String to, String clientName, String planType) {
        try {
            String plan = planType != null ? planType : "SILVER";
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#0f172a; padding:20px;">
                  <div style="max-width:600px;margin:0 auto;background:#1e293b;border-radius:16px;overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:32px;text-align:center;border-bottom:1px solid #334155;">
                      <h1 style="color:#f59e0b;margin:0;font-size:26px;letter-spacing:1px;">Sri Kathiravan &amp; Co</h1>
                      <p style="color:#94a3b8;margin:8px 0 0;">Membership Application Received ✅</p>
                    </div>
                    <div style="padding:32px;">
                      <h2 style="color:#f1f5f9;margin:0 0 16px;">Dear %s,</h2>
                      <p style="color:#94a3b8;line-height:1.7;">We have received your <strong style="color:#f59e0b;">%s Membership</strong> application. Our team will review your request and contact you within 24 hours.</p>
                      <div style="background:#0f172a;border:1px solid #334155;border-radius:10px;padding:20px;margin:24px 0;">
                        <p style="color:#94a3b8;margin:0 0 8px;"><strong style="color:#f1f5f9;">Plan Selected:</strong> %s</p>
                        <p style="color:#94a3b8;margin:0;"><strong style="color:#f1f5f9;">Status:</strong> Under Review</p>
                      </div>
                      <p style="color:#64748b;font-size:13px;">Questions? Contact us at our office. We're here to help.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(clientName, plan, plan);
            sendHtmlEmail(to, "Membership Application Received – Sri Kathiravan & Co", html);
        } catch (Exception e) {
            System.err.println("Failed to send membership confirmation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends alert email to admin when a new membership request is submitted.
     */
    public void sendAdminMembershipAlert(String clientName, String email, String mobile, String planType) {
        try {
            String adminEmail = fromEmail;
            String plan = planType != null ? planType : "SILVER";
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#f4f4f4; padding:20px;">
                  <div style="max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);">
                    <div style="background:linear-gradient(135deg,#7c3aed,#4f46e5);padding:28px;text-align:center;">
                      <h2 style="color:#fff;margin:0;">🔔 New Membership Request</h2>
                    </div>
                    <div style="padding:28px;">
                      <table style="width:100%%;border-collapse:collapse;font-size:14px;">
                        <tr><td style="padding:8px;color:#6b7280;"><strong>Name</strong></td><td style="padding:8px;color:#111827;">%s</td></tr>
                        <tr style="background:#f9fafb;"><td style="padding:8px;color:#6b7280;"><strong>Email</strong></td><td style="padding:8px;color:#111827;">%s</td></tr>
                        <tr><td style="padding:8px;color:#6b7280;"><strong>Mobile</strong></td><td style="padding:8px;color:#111827;">%s</td></tr>
                        <tr style="background:#f9fafb;"><td style="padding:8px;color:#6b7280;"><strong>Plan</strong></td><td style="padding:8px;"><span style="background:#ddd6fe;color:#4f46e5;padding:3px 10px;border-radius:12px;font-weight:600;">%s</span></td></tr>
                      </table>
                      <p style="margin:20px 0 0;color:#6b7280;font-size:13px;">Login to the admin portal to review and activate this membership.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(clientName, email, mobile != null ? mobile : "N/A", plan);
            sendHtmlEmail(adminEmail, "[Sri Kathiravan & Co.] New Membership Request – " + clientName, html);
        } catch (Exception e) {
            System.err.println("Failed to send admin alert: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends OTP to Admin's email whenever a staff member attempts to log in.
     */
    public void sendStaffLoginAdminOtp(String adminEmail, String staffName, String staffEmail, String staffMobile, String otp) {
        try {
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f1f5f9; padding:20px; color:#0f172a; margin:0;">
                  <div style="max-width:540px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #cbd5e1; box-shadow:0 8px 24px rgba(15,23,42,0.08);">
                    <div style="background:linear-gradient(135deg, #1e3a8a, #2563eb); padding:24px 28px; text-align:left; color:#ffffff;">
                      <div style="display:inline-block; background:rgba(255,255,255,0.2); padding:4px 12px; border-radius:999px; font-size:11px; font-weight:800; letter-spacing:0.5px; margin-bottom:8px;">
                        SECURITY AUTHORIZATION DESK
                      </div>
                      <h2 style="margin:0; font-size:20px; font-weight:800;">🔐 Staff Login Authorization Required</h2>
                    </div>

                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 16px 0;">
                        Hello <strong>Administrator</strong>,
                      </p>
                      <p style="font-size:13.5px; color:#475569; line-height:1.6; margin:0 0 18px 0;">
                        Staff member <strong>%s</strong> (Email: %s | Mobile: %s) has verified their credentials and is requesting entry to the Sri Kathiravan & Co. Staff Portal. To authorize this login, provide the following one-time code to the staff member:
                      </p>

                      <div style="background:#f8fafc; border:2px dashed #3b82f6; border-radius:12px; padding:18px; text-align:center; margin:18px 0;">
                        <span style="font-size:11.5px; font-weight:700; color:#64748b; letter-spacing:1px; text-transform:uppercase;">Admin Staff Authorization Code</span>
                        <div style="font-size:34px; font-weight:900; letter-spacing:8px; color:#1e3a8a; margin:8px 0;">
                          %s
                        </div>
                        <span style="font-size:11.5px; color:#dc2626; font-weight:700;">Valid for 10 minutes &bull; Single-use only</span>
                      </div>

                      <div style="font-size:12px; color:#64748b; background:#fef2f2; border:1px solid #fecaca; border-radius:8px; padding:12px; margin-top:16px; line-height:1.5;">
                        ⚠️ <strong>Security Caution:</strong> Only share this OTP with staff member <strong>%s</strong> if you explicitly approve their current access to client files and documents.
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(staffName, staffEmail != null ? staffEmail : "N/A", staffMobile != null ? staffMobile : "N/A", otp, staffName);

            sendHtmlEmail(adminEmail, "🔐 [Action Required] Staff Login Authorization OTP: " + otp + " – " + staffName, html);
            System.out.println(">>> [EmailService] Staff login OTP dispatched to Admin (" + adminEmail + ") for staff: " + staffName);
        } catch (Exception e) {
            System.err.println("Failed to send staff login admin OTP: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends membership plan activation email to client with plan details and expiry.
     */
    public void sendMembershipActivationEmail(String to, String clientName, String planType, int months, String expiryDate, String adminNote) {
        try {
            String planColor = switch (planType != null ? planType.toUpperCase() : "SILVER") {
                case "GOLD" -> "#f59e0b";
                case "PLATINUM" -> "#7c3aed";
                default -> "#94a3b8";
            };
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#0f172a; padding:20px;">
                  <div style="max-width:600px;margin:0 auto;background:#1e293b;border-radius:16px;overflow:hidden;">
                    <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:36px;text-align:center;">
                      <div style="font-size:48px;">🏆</div>
                      <h1 style="color:%s;margin:12px 0 0;font-size:28px;">%s Membership</h1>
                      <p style="color:#94a3b8;margin:8px 0 0;">Activated Successfully!</p>
                    </div>
                    <div style="padding:32px;">
                      <h2 style="color:#f1f5f9;">Congratulations, %s!</h2>
                      <p style="color:#94a3b8;line-height:1.7;">Your <strong style="color:%s;">%s Membership</strong> is now active and valid for <strong style="color:#f1f5f9;">%d month(s)</strong>.</p>
                      <div style="background:#0f172a;border:1px solid #334155;border-radius:12px;padding:20px;margin:24px 0;">
                        <p style="color:#94a3b8;margin:0 0 10px;"><span style="color:#64748b;">Plan:</span> <strong style="color:%s;">%s</strong></p>
                        <p style="color:#94a3b8;margin:0 0 10px;"><span style="color:#64748b;">Valid Until:</span> <strong style="color:#f1f5f9;">%s</strong></p>
                        <p style="color:#94a3b8;margin:0;"><span style="color:#64748b;">Benefits:</span> 24/7 Support · Priority Response · Expert Guidance</p>
                      </div>
                      %s
                      <p style="color:#64748b;font-size:13px;">Log into your portal to explore your exclusive member benefits.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                    planColor, planType, clientName,
                    planColor, planType, months,
                    planColor, planType, expiryDate,
                    adminNote != null && !adminNote.isEmpty() ?
                        "<div style=\"background:#1e3a5f;border-left:4px solid #3b82f6;padding:14px;border-radius:4px;margin:16px 0;\"><p style=\"margin:0;color:#93c5fd;\"><strong>Note from Admin:</strong> " + adminNote + "</p></div>" : ""
                );
            sendHtmlEmail(to, "🎉 Your " + planType + " Membership is Active – Sri Kathiravan & Co", html);
        } catch (Exception e) {
            System.err.println("Failed to send activation email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends OTP to current administrator authorizing deactivation of another administrator account.
     */
    public void sendAdminDeactivationOtp(String to, String targetAdminName, String otp) {
        try {
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#0f172a; padding:20px;">
                  <div style="max-width:540px;margin:0 auto;background:#1e293b;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.5);border:1px solid #334155;">
                    <div style="background:linear-gradient(135deg,#dc2626,#991b1b);padding:28px;text-align:center;">
                      <h2 style="color:#fff;margin:0;font-size:22px;">🛡️ Admin Security Authorization</h2>
                      <p style="color:#fecaca;margin:6px 0 0;font-size:13px;">Administrator Account Deactivation Request</p>
                    </div>
                    <div style="padding:28px;text-align:center;">
                      <p style="color:#e2e8f0;font-size:14px;line-height:1.6;">
                        You have initiated a request to <strong>deactivate</strong> the administrator account for <strong>%s</strong>.
                      </p>
                      <p style="color:#94a3b8;font-size:13px;">
                        Enter the following One-Time Password (OTP) to authorize this high-privilege action. This code expires in 10 minutes.
                      </p>
                      <div style="margin:20px auto;padding:12px 28px;background:#450a0a;border:2px dashed #ef4444;border-radius:10px;display:inline-block;">
                        <span style="font-size:32px;letter-spacing:6px;font-weight:bold;color:#fca5a5;">%s</span>
                      </div>
                      <p style="color:#64748b;font-size:12px;margin:16px 0 0;">
                        ⚠️ If you did not initiate this request, change your administrative password immediately.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(targetAdminName, otp);
            sendHtmlEmail(to, "🔒 Security Code: Authorize Deactivation of Admin " + targetAdminName, html);
        } catch (Exception e) {
            System.err.println("Failed to send admin deactivation OTP: " + e.getMessage() + ". Dev OTP: " + otp);
        }
    }

    /**
     * Sends OTP to current administrator authorizing creation of a new administrator role credential.
     */
    public void sendAdminCreationOtp(String to, String newAdminName, String otp) {
        try {
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; background:#0f172a; padding:20px;">
                  <div style="max-width:540px;margin:0 auto;background:#1e293b;border-radius:16px;overflow:hidden;box-shadow:0 10px 30px rgba(0,0,0,0.5);border:1px solid #334155;">
                    <div style="background:linear-gradient(135deg,#7c3aed,#4f46e5);padding:28px;text-align:center;">
                      <h2 style="color:#fff;margin:0;font-size:22px;">👑 Administrator Role Creation</h2>
                      <p style="color:#ddd6fe;margin:6px 0 0;font-size:13px;">Security Authorization Required</p>
                    </div>
                    <div style="padding:28px;text-align:center;">
                      <p style="color:#e2e8f0;font-size:14px;line-height:1.6;">
                        You are creating a new <strong>Administrator Account</strong> for <strong>%s</strong> with full executive portal privileges.
                      </p>
                      <p style="color:#94a3b8;font-size:13px;">
                        Use this verification code to confirm your administrative identity. Code is valid for 10 minutes.
                      </p>
                      <div style="margin:20px auto;padding:12px 28px;background:#2e1065;border:2px dashed #8b5cf6;border-radius:10px;display:inline-block;">
                        <span style="font-size:32px;letter-spacing:6px;font-weight:bold;color:#c4b5fd;">%s</span>
                      </div>
                      <p style="color:#64748b;font-size:12px;margin:16px 0 0;">
                        Do not share this code with anyone. It grants root administrative rights.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(newAdminName, otp);
            sendHtmlEmail(to, "👑 Security Code: Authorize New Admin Account for " + newAdminName, html);
        } catch (Exception e) {
            System.err.println("Failed to send admin creation OTP: " + e.getMessage());
        }
    }

    /**
     * Dispatches alert email to Admins & Staff when a client books an appointment.
     */
    public void sendNewAppointmentAlertToStaffAndAdmin(
            String to,
            String recipientName,
            String clientName,
            String clientEmail,
            String clientMobile,
            String apptType,
            String requestedTime,
            String note
    ) {
        try {
            String safeNote = (note != null && !note.isBlank()) ? note : "None provided";
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 4px 18px rgba(0,0,0,0.06);">
                    <div style="background:linear-gradient(135deg, #1e1b4b, #312e81); padding:24px 28px; text-align:center;">
                      <h2 style="color:#ffffff; margin:0; font-size:20px; font-weight:800;">📅 New Client Appointment Booked</h2>
                      <p style="color:#c7d2fe; margin:6px 0 0; font-size:13px;">Sri Kathiravan & Co. Advisory Desk</p>
                    </div>
                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 16px 0;">
                        Hello <strong>%s</strong>, a new consultation slot has been booked by a client.
                      </p>
                      <div style="background:#f1f5f9; border-radius:10px; padding:16px 18px; margin-bottom:18px;">
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Client Name:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Email:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Mobile:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Consultation Format:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Requested Time:</strong> %s</div>
                        <div style="font-size:13px;"><strong>Client Topic / Query:</strong> %s</div>
                      </div>
                      <p style="font-size:12.5px; color:#64748b; margin:0 0 16px 0;">
                        Please log in to the portal to review the slot, confirm availability, and attach the meeting link.
                      </p>
                      <div style="text-align:center;">
                        <a href="http://localhost:5173/Admin/Appointments" style="display:inline-block; padding:10px 22px; background:#4f46e5; color:#ffffff; text-decoration:none; border-radius:8px; font-size:13px; font-weight:700;">
                          Review Appointment in Portal
                        </a>
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(recipientName, clientName, clientEmail, clientMobile, apptType, requestedTime, safeNote);

            sendHtmlEmail(to, "📅 New Appointment Booking: " + clientName + " (" + apptType + ")", html);
        } catch (Exception e) {
            System.err.println("Failed to send appointment alert to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Sends approval email to client when Admin confirms appointment slot.
     */
    public void sendAppointmentConfirmedEmailToClient(
            String to,
            String clientName,
            String apptType,
            String scheduledTime,
            String meetingLink,
            String adminNote
    ) {
        try {
            String safeLink = (meetingLink != null && !meetingLink.isBlank()) ? meetingLink : "Meeting link will be shared 30 minutes before the session starts";
            String safeNote = (adminNote != null && !adminNote.isBlank()) ? adminNote : "Please keep necessary financial documents ready.";
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 4px 18px rgba(0,0,0,0.06);">
                    <div style="background:linear-gradient(135deg, #064e3b, #047857); padding:24px 28px; text-align:center;">
                      <h2 style="color:#ffffff; margin:0; font-size:20px; font-weight:800;">✅ Consultation Confirmed</h2>
                      <p style="color:#a7f3d0; margin:6px 0 0; font-size:13px;">Sri Kathiravan & Co. Chartered Accountants</p>
                    </div>
                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 16px 0;">
                        Dear <strong>%s</strong>, your consultation appointment has been reviewed and officially confirmed by our executive team.
                      </p>
                      <div style="background:#ecfdf5; border:1px solid #a7f3d0; border-radius:10px; padding:16px 18px; margin-bottom:18px;">
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Session Format:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Confirmed Timeline:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Meeting Link / Room:</strong> <span style="color:#047857; font-weight:600;">%s</span></div>
                        <div style="font-size:13px;"><strong>Office Instructions:</strong> %s</div>
                      </div>
                      <div style="background:#fffbeb; border:1px solid #fde68a; border-radius:8px; padding:10px 14px; margin-bottom:18px; font-size:12.5px; color:#92400e;">
                        ⏰ <strong>Automated Reminder:</strong> You will receive an automated email notification <strong>30 minutes before</strong> your meeting with the direct access link to ensure a seamless session.
                      </div>
                      <p style="font-size:12px; color:#64748b; margin:0;">
                        If you have questions prior to your meeting, you can contact our office directly via your client portal.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(clientName, apptType, scheduledTime, safeLink, safeNote);

            sendHtmlEmail(to, "✅ Consultation Confirmed: Sri Kathiravan & Co. (" + scheduledTime + ")", html);
        } catch (Exception e) {
            System.err.println("Failed to send appointment confirmation to client " + to + ": " + e.getMessage());
        }
    }

    /**
     * Automated reminder dispatched 30 minutes before meeting time.
     */
    public void sendAppointment30MinReminderEmail(
            String to,
            String recipientName,
            boolean isClient,
            String clientName,
            String apptType,
            String scheduledTime,
            String meetingLink,
            String note
    ) {
        try {
            String linkHtml = (meetingLink != null && meetingLink.startsWith("http"))
                    ? "<a href=\"" + meetingLink + "\" style=\"display:inline-block; padding:10px 24px; background:#2563eb; color:#ffffff; text-decoration:none; border-radius:8px; font-weight:700; font-size:13px;\">Join Meeting Now</a>"
                    : "<span style=\"font-size:13px; font-weight:700; color:#1e293b;\">" + (meetingLink != null ? meetingLink : "In-Person Office Desk") + "</span>";

            String headline = isClient ? "Your consultation session starts in 30 minutes!" : "Upcoming client meeting with " + clientName + " in 30 minutes";

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1.5px solid #3b82f6; box-shadow:0 6px 24px rgba(59,130,246,0.12);">
                    <div style="background:linear-gradient(135deg, #1d4ed8, #2563eb); padding:24px 28px; text-align:center;">
                      <h2 style="color:#ffffff; margin:0; font-size:20px; font-weight:800;">⏰ Meeting in 30 Minutes</h2>
                      <p style="color:#dbeafe; margin:6px 0 0; font-size:13px;">Sri Kathiravan & Co. Advisory Reminder</p>
                    </div>
                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 16px 0;">
                        Hello <strong>%s</strong>, this is an automated reminder that:
                      </p>
                      <div style="background:#eff6ff; border:1px solid #bfdbfe; border-radius:10px; padding:16px 18px; margin-bottom:18px;">
                        <div style="font-size:15px; font-weight:800; color:#1e3a8a; margin-bottom:8px;">%s</div>
                        <div style="margin-bottom:6px; font-size:13px;"><strong>Scheduled Time:</strong> %s</div>
                        <div style="margin-bottom:6px; font-size:13px;"><strong>Session Type:</strong> %s</div>
                        <div style="font-size:13px;"><strong>Client:</strong> %s</div>
                      </div>
                      <div style="text-align:center; margin:20px 0;">
                        %s
                      </div>
                      <p style="font-size:12px; color:#64748b; text-align:center; margin:0;">
                        Please ensure your microphone and video camera are ready prior to joining.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(recipientName, headline, scheduledTime, apptType, clientName, linkHtml);

            sendHtmlEmail(to, "⏰ Starting in 30 Mins: Consultation with Sri Kathiravan & Co.", html);
        } catch (Exception e) {
            System.err.println("Failed to send 30-min reminder email to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Broadcast notification email to clients, staff, or admins.
     */
    public void sendBroadcastNotificationEmail(String to, String recipientName, String title, String message) {
        try {
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 4px 18px rgba(0,0,0,0.06);">
                    <div style="background:linear-gradient(135deg, #0f172a, #1e293b); padding:24px 28px; text-align:center;">
                      <h2 style="color:#ffffff; margin:0; font-size:20px; font-weight:800;">📢 Official Announcement</h2>
                      <p style="color:#94a3b8; margin:6px 0 0; font-size:13px;">Sri Kathiravan & Co. Management</p>
                    </div>
                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 14px 0;">Hello <strong>%s</strong>,</p>
                      <h3 style="font-size:16px; font-weight:800; color:#0f172a; margin:0 0 10px 0;">%s</h3>
                      <div style="background:#f8fafc; border-left:4px solid #3b82f6; padding:14px 16px; border-radius:4px; margin-bottom:18px; font-size:13.5px; line-height:1.6; color:#334155;">
                        %s
                      </div>
                      <p style="font-size:12px; color:#64748b; margin:0;">
                        This notification was broadcast from the Sri Kathiravan & Co. Enterprise Portal.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(recipientName, title, message);

            sendHtmlEmail(to, "📢 Announcement: " + title, html);
        } catch (Exception e) {
            System.err.println("Failed to send broadcast email to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Sends Congratulations and account credentials email after creating a Client or Staff (or Admin).
     * Includes username, password, assigned role, and a security suggestion to change password after initial login.
     */
    public void sendAccountCreationWelcomeEmail(
            String toEmail,
            String fullName,
            String username,
            String initialPassword,
            String role
    ) {
        try {
            if (toEmail == null || toEmail.isBlank()) return;

            String roleDisplay = role != null ? role.toUpperCase() : "CLIENT";
            String roleBadgeColor = roleDisplay.contains("ADMIN") ? "#7c3aed" : (roleDisplay.contains("STAFF") ? "#0284c7" : "#059669");

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:16px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 8px 30px rgba(0,0,0,0.07);">
                    
                    <!-- Header -->
                    <div style="background:linear-gradient(135deg, #0f172a 0%%, #1e293b 60%%, #312e81 100%%); padding:28px; text-align:center;">
                      <h1 style="color:#ffffff; margin:0; font-size:22px; font-weight:800; letter-spacing:-0.3px;">🎉 Welcome to Sri Kathiravan &amp; Co.</h1>
                      <p style="color:#cbd5e1; margin:8px 0 0; font-size:13px;">Official Account Provisioning &amp; Access Details</p>
                    </div>

                    <!-- Body -->
                    <div style="padding:28px 32px;">
                      <p style="font-size:15px; color:#1e293b; margin:0 0 16px 0;">
                        Congratulations <strong>%s</strong>!
                      </p>
                      <p style="font-size:13.5px; color:#475569; line-height:1.6; margin:0 0 20px 0;">
                        Your official account has been successfully created and configured on the <strong>Sri Kathiravan &amp; Co. Enterprise Portal</strong>. You may now sign in using the credentials below:
                      </p>

                      <!-- Credentials Box -->
                      <div style="background:#f8fafc; border:1.5px solid #e2e8f0; border-radius:12px; padding:20px; margin-bottom:20px;">
                        
                        <div style="margin-bottom:12px; display:flex; justify-content:space-between; align-items:center;">
                          <span style="font-size:12px; color:#64748b; font-weight:700; text-transform:uppercase;">Assigned Role</span>
                          <span style="display:inline-block; padding:4px 12px; background:%s; color:#ffffff; font-size:11px; font-weight:800; border-radius:20px; letter-spacing:0.5px;">%s</span>
                        </div>

                        <div style="margin-bottom:12px;">
                          <span style="font-size:12px; color:#64748b; font-weight:700; text-transform:uppercase; display:block; margin-bottom:4px;">Username / Login ID</span>
                          <span style="font-size:14px; font-weight:700; color:#0f172a; font-family:Consolas, monospace; background:#ffffff; border:1px solid #cbd5e1; padding:6px 12px; border-radius:6px; display:inline-block;">%s</span>
                        </div>

                        <div>
                          <span style="font-size:12px; color:#64748b; font-weight:700; text-transform:uppercase; display:block; margin-bottom:4px;">Initial Password</span>
                          <span style="font-size:15px; font-weight:800; color:#b45309; font-family:Consolas, monospace; background:#fef3c7; border:1px dashed #d97706; padding:6px 14px; border-radius:6px; display:inline-block;">%s</span>
                        </div>
                      </div>

                      <!-- Security Suggestion Alert -->
                      <div style="background:#fffbeb; border-left:4px solid #f59e0b; padding:14px 16px; border-radius:6px; margin-bottom:24px;">
                        <h4 style="margin:0 0 6px 0; color:#92400e; font-size:13px; font-weight:700;">🔒 Recommended Security Action: Change Your Password</h4>
                        <p style="margin:0; font-size:12.5px; color:#b45309; line-height:1.5;">
                          For your account safety and confidentiality, we strongly suggest updating this initial password immediately upon your first login. You can do this at any time from your Account Profile / Security Settings tab.
                        </p>
                      </div>

                      <div style="text-align:center; margin-bottom:20px;">
                        <a href="http://localhost:5173/SignIn" style="display:inline-block; padding:11px 32px; background:linear-gradient(135deg, #4f46e5, #4338ca); color:#ffffff; text-decoration:none; font-weight:700; font-size:13px; border-radius:8px; box-shadow:0 3px 10px rgba(79,70,229,0.3);">
                          Sign In to Your Account &rarr;
                        </a>
                      </div>

                      <hr style="border:none; border-top:1px solid #f1f5f9; margin:20px 0;" />

                      <p style="font-size:12px; color:#94a3b8; text-align:center; margin:0; line-height:1.5;">
                        Sri Kathiravan &amp; Co. Chartered Accountants &bull; Confidential &bull; Do not forward this email
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(fullName, roleBadgeColor, roleDisplay, username, initialPassword);

            sendHtmlEmail(toEmail, "🎉 Welcome to Sri Kathiravan & Co. – Your Account Credentials & Access Details", html);
        } catch (Exception e) {
            System.err.println("Failed to dispatch welcome email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends cancellation notification email to client when appointment is cancelled.
     */
    public void sendAppointmentCancelledEmailToClient(
            String to,
            String clientName,
            String apptType,
            String scheduledTime,
            String cancelReason
    ) {
        try {
            String safeReason = (cancelReason != null && !cancelReason.isBlank())
                    ? cancelReason
                    : "The requested consultation slot was cancelled or is unavailable at this time.";
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 4px 18px rgba(0,0,0,0.06);">
                    <div style="background:linear-gradient(135deg, #991b1b, #dc2626); padding:24px 28px; text-align:center;">
                      <h2 style="color:#ffffff; margin:0; font-size:20px; font-weight:800;">🚫 Consultation Appointment Cancelled</h2>
                      <p style="color:#fecaca; margin:6px 0 0; font-size:13px;">Sri Kathiravan &amp; Co. Advisory Desk</p>
                    </div>
                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 16px 0;">
                        Dear <strong>%s</strong>, this is an official update regarding your consultation appointment with Sri Kathiravan &amp; Co.
                      </p>
                      <div style="background:#fef2f2; border:1px solid #fecaca; border-radius:10px; padding:16px 18px; margin-bottom:18px;">
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Session Type:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Scheduled Time:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px; color:#991b1b;"><strong>Status:</strong> CANCELLED</div>
                        <div style="font-size:13px; color:#374151;"><strong>Reason / Notes:</strong> %s</div>
                      </div>
                      <div style="background:#f1f5f9; border-radius:8px; padding:14px 16px; margin-bottom:18px; font-size:12.5px; color:#475569; line-height:1.5;">
                        💡 <strong>Need to Reschedule?</strong><br />
                        You are welcome to submit a new consultation request at your convenience through our online portal or contact our office desk directly.
                      </div>
                      <div style="text-align:center; margin-bottom:14px;">
                        <a href="http://localhost:5173" style="display:inline-block; padding:10px 24px; background:#0f172a; color:#ffffff; text-decoration:none; border-radius:8px; font-weight:700; font-size:13px;">
                          Visit Sri Kathiravan &amp; Co.
                        </a>
                      </div>
                      <p style="font-size:12px; color:#94a3b8; text-align:center; margin:0;">
                        Helpline: +91 8807836326 &bull; Email: info@srikathiravan.com
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(clientName, apptType, scheduledTime, safeReason);

            sendHtmlEmail(to, "🚫 Appointment Cancelled Notice: Sri Kathiravan & Co.", html);
        } catch (Exception e) {
            System.err.println("Failed to send appointment cancellation email to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Sends acknowledgement email to unregistered website visitor booking an appointment.
     */
    public void sendGuestAppointmentAcknowledgementEmail(
            String to,
            String guestName,
            String apptType,
            String requestedTime
    ) {
        try {
            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f8fafc; padding:24px; color:#0f172a;">
                  <div style="max-width:580px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #e2e8f0; box-shadow:0 4px 18px rgba(0,0,0,0.06);">
                    <div style="background:linear-gradient(135deg, #1e1b4b, #4338ca); padding:24px 28px; text-align:center;">
                      <h2 style="color:#ffffff; margin:0; font-size:20px; font-weight:800;">📅 Consultation Request Received</h2>
                      <p style="color:#c7d2fe; margin:6px 0 0; font-size:13px;">Sri Kathiravan &amp; Co. Tax &amp; Advisory Desk</p>
                    </div>
                    <div style="padding:26px 28px;">
                      <p style="font-size:14px; color:#334155; margin:0 0 16px 0;">
                        Dear <strong>%s</strong>, thank you for reaching out to Sri Kathiravan &amp; Co. We have successfully received your consultation request.
                      </p>
                      <div style="background:#eef2ff; border:1px solid #c7d2fe; border-radius:10px; padding:16px 18px; margin-bottom:18px;">
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Requested Mode:</strong> %s</div>
                        <div style="margin-bottom:8px; font-size:13px;"><strong>Requested Date &amp; Time:</strong> %s</div>
                        <div style="font-size:13px; color:#4338ca; font-weight:700;"><strong>Status:</strong> Pending Advisory Desk Review</div>
                      </div>
                      <p style="font-size:13px; color:#475569; line-height:1.5; margin-bottom:18px;">
                        Our advisory team will review your requested slot and will send you an official confirmation with the meeting room link / office desk details shortly.
                      </p>
                      <div style="background:#f8fafc; border-radius:8px; padding:12px 16px; font-size:12px; color:#64748b; text-align:center;">
                        Direct Assistance: +91 8807836326 &bull; Email: info@srikathiravan.com
                      </div>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(guestName, apptType, requestedTime);

            sendHtmlEmail(to, "📅 Consultation Request Received: Sri Kathiravan & Co.", html);
        } catch (Exception e) {
            System.err.println("Failed to send guest appointment acknowledgement to " + to + ": " + e.getMessage());
        }
    }

    /**
     * Sends official invoice or payment receipt notification to client's email upon filing.
     */
    public void sendInvoiceNotificationEmail(BillingDocument doc, FirmSettings firm) {
        if (doc == null || doc.getClientEmail() == null || !doc.getClientEmail().contains("@")) {
            return;
        }

        try {
            String to = doc.getClientEmail().trim();
            boolean isReceipt = "RECEIPT".equalsIgnoreCase(doc.getType());
            String firmName = (firm != null && firm.getName() != null) ? firm.getName() : "Sri Kathiravan & Co.";
            String docTitle = isReceipt ? "Official Payment Receipt" : "Official Tax Invoice";
            String docNumber = doc.getDocNumber() != null ? doc.getDocNumber() : "DOC";
            String subject = String.format("📄 %s (%s) – %s", docTitle, docNumber, firmName);

            String clientName = doc.getClientName() != null ? doc.getClientName() : "Valued Client";
            String clientLegalName = doc.getClientLegalName() != null ? doc.getClientLegalName() : clientName;
            String date = doc.getDocDate() != null ? doc.getDocDate() : "";
            String dueDate = doc.getDueDate() != null ? doc.getDueDate() : "Immediate";
            double totalAmount = doc.getTotalAmount() != null ? doc.getTotalAmount() : 0.0;
            double paidAmount = doc.getPaidAmount() != null ? doc.getPaidAmount() : 0.0;
            double remainingDue = doc.getRemainingBalance() != null ? Math.max(0.0, doc.getRemainingBalance()) : 0.0;

            String bankName = (firm != null && firm.getBankName() != null) ? firm.getBankName() : "HDFC Bank Ltd";
            String accountName = (firm != null && firm.getAccountName() != null) ? firm.getAccountName() : "Sri Kathiravan & Co.";
            String accountNo = (firm != null && firm.getAccountNumber() != null) ? firm.getAccountNumber() : "50200084729103";
            String ifsc = (firm != null && firm.getIfscCode() != null) ? firm.getIfscCode() : "HDFC0001234";
            String branch = (firm != null && firm.getBranch() != null) ? firm.getBranch() : "Salem Main Branch";
            String upiId = (firm != null && firm.getUpiId() != null) ? firm.getUpiId() : "8825750992@upi";
            String firmPhone = (firm != null && firm.getPhone() != null) ? firm.getPhone() : "+91 88257 50992";
            String firmEmail = (firm != null && firm.getEmail() != null) ? firm.getEmail() : "contact@srikathiravan.com";

            String headerBg = isReceipt ? "linear-gradient(135deg, #065f46, #059669)" : "linear-gradient(135deg, #1e3a8a, #2563eb)";
            String badgeBg = isReceipt ? "#059669" : "#2563eb";

            String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: 'Segoe UI', Arial, sans-serif; background:#f1f5f9; padding:20px; color:#0f172a; margin:0;">
                  <div style="max-width:620px; margin:0 auto; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #cbd5e1; box-shadow:0 8px 24px rgba(15,23,42,0.08);">
                    <!-- Header -->
                    <div style="background:%s; padding:26px 30px; text-align:left; color:#ffffff;">
                      <div style="display:inline-block; background:rgba(255,255,255,0.2); padding:4px 12px; border-radius:999px; font-size:11px; font-weight:800; letter-spacing:0.5px; margin-bottom:8px;">
                        %s
                      </div>
                      <h2 style="margin:0; font-size:22px; font-weight:800; letter-spacing:-0.5px;">%s</h2>
                      <p style="margin:4px 0 0; font-size:12px; opacity:0.9;">Document Ref: <strong>%s</strong> &bull; Date: %s</p>
                    </div>

                    <div style="padding:28px 30px;">
                      <p style="font-size:14.5px; color:#334155; margin:0 0 16px 0;">
                        Dear <strong>%s</strong> (%s),
                      </p>
                      <p style="font-size:13.5px; color:#475569; line-height:1.6; margin:0 0 20px 0;">
                        We have successfully processed and filed your <strong>%s</strong> in our official records. You can view, track, and download the full signed document anytime in your Client Portal.
                      </p>

                      <!-- Financial Summary Card -->
                      <div style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:12px; padding:18px 20px; margin-bottom:24px;">
                        <h4 style="margin:0 0 12px 0; font-size:13px; text-transform:uppercase; letter-spacing:0.5px; color:#64748b;">Financial Summary</h4>
                        <table style="width:100%%; font-size:13.5px; border-collapse:collapse;">
                          <tr>
                            <td style="padding:6px 0; color:#475569;">Total Billed Amount:</td>
                            <td style="padding:6px 0; text-align:right; font-weight:700; color:#0f172a;">₹ %,.2f</td>
                          </tr>
                          <tr>
                            <td style="padding:6px 0; color:#475569;">Amount Paid / Cleared:</td>
                            <td style="padding:6px 0; text-align:right; font-weight:700; color:#16a34a;">₹ %,.2f</td>
                          </tr>
                          <tr style="border-top:1px dashed #cbd5e1;">
                            <td style="padding:10px 0 4px; font-size:15px; font-weight:800; color:#0f172a;">Remaining Balance Due:</td>
                            <td style="padding:10px 0 4px; text-align:right; font-size:16px; font-weight:900; color:%s;">₹ %,.2f</td>
                          </tr>
                          <tr>
                            <td style="padding:2px 0; font-size:11.5px; color:#64748b;">Due Date:</td>
                            <td style="padding:2px 0; text-align:right; font-size:12px; font-weight:600; color:#64748b;">%s</td>
                          </tr>
                        </table>
                      </div>

                      <!-- Remittance Details -->
                      <div style="background:#ecfdf5; border:1px solid #a7f3d0; border-radius:12px; padding:18px 20px; margin-bottom:24px;">
                        <h4 style="margin:0 0 10px 0; font-size:13px; color:#065f46; font-weight:800;">🏦 Official Authorized Remittance Channels</h4>
                        <table style="width:100%%; font-size:12.5px; color:#065f46; line-height:1.7;">
                          <tr>
                            <td style="width:40%%; font-weight:600;">Beneficiary Bank:</td>
                            <td><strong>%s</strong> (%s)</td>
                          </tr>
                          <tr>
                            <td style="font-weight:600;">Account Name:</td>
                            <td><strong>%s</strong></td>
                          </tr>
                          <tr>
                            <td style="font-weight:600;">Account Number:</td>
                            <td><strong style="color:#1e3a8a; font-size:14px; letter-spacing:0.5px;">%s</strong></td>
                          </tr>
                          <tr>
                            <td style="font-weight:600;">IFSC Code:</td>
                            <td><strong>%s</strong></td>
                          </tr>
                          <tr>
                            <td style="font-weight:600;">Official UPI ID:</td>
                            <td><strong style="color:#047857; font-size:13.5px;">%s</strong></td>
                          </tr>
                        </table>
                      </div>

                      <!-- Action Button -->
                      <div style="text-align:center; margin:28px 0 16px;">
                        <p style="font-size:12.5px; color:#64748b; margin-bottom:12px;">Login to your client dashboard to view your complete billing ledger and print tax invoices:</p>
                        <a href="http://localhost:5173/Client/Billing" style="background:%s; color:#ffffff; padding:12px 28px; border-radius:8px; text-decoration:none; font-size:14px; font-weight:700; display:inline-block; box-shadow:0 4px 14px rgba(37,99,235,0.3);">
                          View in Client Portal &rarr;
                        </a>
                      </div>
                    </div>

                    <!-- Footer -->
                    <div style="background:#f8fafc; border-top:1px solid #e2e8f0; padding:16px 30px; font-size:11.5px; color:#64748b; text-align:center;">
                      <p style="margin:2px 0;"><strong>%s</strong> &bull; Phone: %s &bull; Email: %s</p>
                      <p style="margin:2px 0;">This is an automated legal financial statement. Computer generated document.</p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                    headerBg,
                    docTitle.toUpperCase(),
                    firmName,
                    docNumber,
                    date,
                    clientName,
                    clientLegalName,
                    docTitle,
                    totalAmount,
                    paidAmount,
                    remainingDue <= 0 ? "#16a34a" : "#dc2626",
                    remainingDue,
                    dueDate,
                    bankName,
                    branch,
                    accountName,
                    accountNo,
                    ifsc,
                    upiId,
                    badgeBg,
                    firmName,
                    firmPhone,
                    firmEmail
                );

            sendHtmlEmail(to, subject, html);
            System.out.println(">>> [EmailService] Invoice/Receipt email delivered to " + to + " for " + docNumber);
        } catch (Exception e) {
            System.err.println("Failed to send invoice email to " + doc.getClientEmail() + ": " + e.getMessage());
        }
    }
}


