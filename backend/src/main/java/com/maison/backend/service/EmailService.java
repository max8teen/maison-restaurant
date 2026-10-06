package com.maison.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${restaurant.name:Maison Fine Dining}")
    private String restaurantName;

    @Value("${restaurant.address:Anand Nagar, Thane, Maharashtra, India}")
    private String restaurantAddress;

    @Value("${restaurant.phone:+91 7972666151}")
    private String restaurantPhone;

    @Value("${restaurant.email:maisonadmin007@gmail.com}")
    private String restaurantEmail;

    @Value("${restaurant.url:http://localhost:8080}")
    private String restaurantUrl;

    @Value("${restaurant.wa-enabled:true}")
    private boolean waEnabled;

    @Value("${restaurant.wa-business-number:917972666151}")
    private String waBusinessNumber;

    @Value("${spring.mail.username:maisonadmin007@gmail.com}")
    private String mailFrom;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    private boolean sendMail(String toEmail, String toName, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom, restaurantName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            System.err.println("Email send failed to " + toEmail + ": " + e.getMessage());
            return false;
        }
    }

    private String buildWrapper(String preheader, String contentHtml) {
        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="utf-8"/><meta name="viewport" content="width=device-width,initial-scale=1.0"/></head>
        <body style="margin:0;padding:0;background-color:#050507;color:#f2edd8;font-family:Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#050507;padding:40px 0;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0" style="background:#0f1015;border:1px solid #1e1f24;max-width:600px;width:100%%;">
                <tr><td style="padding:32px 40px;text-align:center;border-bottom:1px solid #1e1f24;">
                  <div style="font-family:Georgia,serif;font-size:22px;letter-spacing:4px;color:#c49a3c;text-transform:uppercase;">%s</div>
                </td></tr>
                <tr><td style="padding:40px;">
                  %s
                </td></tr>
                <tr><td style="padding:24px 40px;background:#0a0b0d;border-top:1px solid #1e1f24;text-align:center;font-size:12px;color:#4a4540;">
                  © %d %s. All rights reserved.
                </td></tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(restaurantName, contentHtml, LocalDate.now().getYear(), restaurantName);
    }

    public boolean sendWelcomeOTP(String toEmail, String toName, String otp) {
        String content = """
            <p style="font-size:13px;letter-spacing:3px;color:#7a6a35;text-transform:uppercase;margin:0 0 16px;">Welcome</p>
            <h1 style="font-size:26px;color:#f2edd8;font-family:Georgia,serif;font-weight:normal;margin:0 0 20px;">Hello, %s</h1>
            <p style="font-size:15px;color:#8a8070;line-height:1.8;margin:0 0 32px;">
                Thank you for joining %s. Use the code below to verify your account.
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="margin:0 0 32px;">
              <tr><td align="center">
                <div style="background:#0a0b0d;border:1px solid #2a2416;display:inline-block;padding:24px 48px;text-align:center;">
                  <div style="font-size:11px;letter-spacing:4px;color:#7a6a35;text-transform:uppercase;margin-bottom:12px;">Verification Code</div>
                  <div style="font-size:38px;letter-spacing:12px;color:#c49a3c;font-family:Courier,monospace;font-weight:bold;">%s</div>
                  <div style="font-size:11px;color:#3a3b3f;margin-top:12px;">Valid for 3 minutes</div>
                </div>
              </td></tr>
            </table>
            <p style="font-size:13px;color:#4a4540;">If you did not create an account, please ignore this email.</p>
        """.formatted(toName, restaurantName, otp);
        return sendMail(toEmail, toName, "Welcome to " + restaurantName + " — Verify Your Account", buildWrapper("Welcome", content));
    }

    public boolean sendPasswordResetOTP(String toEmail, String toName, String otp) {
        String content = """
            <p style="font-size:13px;letter-spacing:3px;color:#7a6a35;text-transform:uppercase;margin:0 0 16px;">Security</p>
            <h1 style="font-size:26px;color:#f2edd8;font-family:Georgia,serif;font-weight:normal;margin:0 0 20px;">Reset Your Password</h1>
            <p style="font-size:15px;color:#8a8070;line-height:1.8;margin:0 0 32px;">
                We received a request to reset your account password. Use the code below to proceed.
            </p>
            <table width="100%%" cellpadding="0" cellspacing="0" style="margin:0 0 32px;">
              <tr><td align="center">
                <div style="background:#0a0b0d;border:1px solid #2a1a1a;display:inline-block;padding:24px 48px;text-align:center;">
                  <div style="font-size:11px;letter-spacing:4px;color:#7a6a35;text-transform:uppercase;margin-bottom:12px;">Reset Code</div>
                  <div style="font-size:38px;letter-spacing:12px;color:#c49a3c;font-family:Courier,monospace;font-weight:bold;">%s</div>
                  <div style="font-size:11px;color:#3a3b3f;margin-top:12px;">Valid for 3 minutes</div>
                </div>
              </td></tr>
            </table>
            <p style="font-size:13px;color:#4a4540;">If you did not request this, your account is safe — ignore this email.</p>
        """.formatted(otp);
        return sendMail(toEmail, toName, restaurantName + " — Password Reset Code", buildWrapper("Security", content));
    }

    public boolean sendBookingConfirmation(String toEmail, String toName, String bookingRef, LocalDate date, String time, int partySize,
                                            String occasion, String tableNumber, String tableLocation) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        String occasionRow = (occasion != null && !occasion.equalsIgnoreCase("none")) ?
            """
            <tr>
              <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Occasion</td>
              <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%s</td>
            </tr>
            """.formatted(occasion) : "";

        String tableRow = (tableNumber != null && !tableNumber.isEmpty()) ?
            """
            <tr>
              <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Table</td>
              <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%s · %s</td>
            </tr>
            """.formatted(tableNumber, tableLocation) : "";

        String content = """
            <p style="font-size:13px;letter-spacing:3px;color:#7a6a35;text-transform:uppercase;margin:0 0 16px;">Reservation Confirmed</p>
            <h1 style="font-size:26px;color:#f2edd8;font-family:Georgia,serif;font-weight:normal;margin:0 0 12px;">Your table awaits, %s</h1>
            <p style="font-size:15px;color:#8a8070;line-height:1.8;margin:0 0 32px;">
                Your reservation at %s is confirmed. We look forward to welcoming you.
            </p>
            <div style="background:#0a0b0d;border:1px solid #1e1f24;padding:24px 28px;margin-bottom:32px;">
              <div style="font-size:10px;letter-spacing:4px;color:#7a6a35;text-transform:uppercase;margin-bottom:20px;">Booking Details</div>
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Reference</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c49a3c;font-family:Courier,monospace;text-align:right;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Date</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Time</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Guests</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%d Guests</td>
                </tr>
                %s
                %s
              </table>
            </div>
            <p style="font-size:13px;color:#4a4540;line-height:1.8;">
                📍 %s &nbsp;·&nbsp; 📞 %s
            </p>
        """.formatted(toName, restaurantName, bookingRef, formattedDate, time, partySize, occasionRow, tableRow, restaurantAddress, restaurantPhone);

        return sendMail(toEmail, toName, "Booking Confirmed — " + bookingRef + " · " + restaurantName, buildWrapper("Reservation Confirmed", content));
    }

    public boolean sendCancellationEmail(String toEmail, String toName, String bookingRef, LocalDate date, String time, int partySize) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        String content = """
            <p style="font-size:13px;letter-spacing:3px;color:#7a6a35;text-transform:uppercase;margin:0 0 16px;">Cancellation</p>
            <h1 style="font-size:26px;color:#f2edd8;font-family:Georgia,serif;font-weight:normal;margin:0 0 12px;">Booking Cancelled</h1>
            <p style="font-size:15px;color:#8a8070;line-height:1.8;margin:0 0 32px;">
                Your reservation at %s has been cancelled. We hope to welcome you another time.
            </p>
            <div style="background:#0a0b0d;border:1px solid #1e1f24;padding:24px 28px;margin-bottom:32px;">
              <table width="100%%" cellpadding="0" cellspacing="0">
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Reference</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c49a3c;font-family:Courier,monospace;text-align:right;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Date</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%s</td>
                </tr>
                <tr>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#6a6560;">Time</td>
                  <td style="padding:10px 0;border-bottom:1px solid #1e1f24;font-size:13px;color:#c4b898;text-align:right;">%s</td>
                </tr>
              </table>
            </div>
        """.formatted(restaurantName, bookingRef, formattedDate, time);

        return sendMail(toEmail, toName, "Booking Cancelled — " + bookingRef + " · " + restaurantName, buildWrapper("Cancellation", content));
    }

    public boolean sendStatusChangeEmail(String toEmail, String toName, String bookingRef, LocalDate date, String time, String status) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        String content = """
            <p style="font-size:13px;letter-spacing:3px;color:#7a6a35;text-transform:uppercase;margin:0 0 16px;">Booking Update</p>
            <h1 style="font-size:26px;color:#f2edd8;font-family:Georgia,serif;font-weight:normal;margin:0 0 12px;">Status: %s</h1>
            <p style="font-size:15px;color:#8a8070;line-height:1.8;margin:0 0 32px;">
                Your reservation at %s has been updated to status: %s.
            </p>
        """.formatted(status, restaurantName, status);

        return sendMail(toEmail, toName, restaurantName + " — Booking Status Update · " + bookingRef, buildWrapper("Status Update", content));
    }

    public boolean sendWaitlistNotification(String toEmail, String toName, LocalDate date, String time, int partySize, String bookingUrl) {
        String formattedDate = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        String content = """
            <p style="font-size:13px;letter-spacing:3px;color:#7a6a35;text-transform:uppercase;margin:0 0 16px;">Waitlist</p>
            <h1 style="font-size:26px;color:#f2edd8;font-family:Georgia,serif;font-weight:normal;margin:0 0 12px;">A table is now available!</h1>
            <p style="font-size:15px;color:#8a8070;line-height:1.8;margin:0 0 32px;">
                Good news, %s! A table has opened up for your requested date (%s) and time (%s) at %s.
            </p>
            <div style="text-align:center;">
              <a href="%s" style="display:inline-block;background:#c49a3c;color:#0a0b0d;font-size:13px;font-weight:bold;padding:14px 36px;text-decoration:none;letter-spacing:2px;text-transform:uppercase;">
                Book Now
              </a>
            </div>
        """.formatted(toName, formattedDate, time, restaurantName, bookingUrl);

        return sendMail(toEmail, toName, "Table Available for " + formattedDate + " · " + restaurantName, buildWrapper("Waitlist Notification", content));
    }
}
