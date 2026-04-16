package com.example.ragApp.service;



import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class MailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendOtpEmail(String toEmail, String otp) throws Exception {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(toEmail);
        helper.setSubject("Verify your Email");

        String html = "<h2>Email Verification</h2>" +
                "<p>Your OTP is: <b>" + otp + "</b></p>" +
                "<p>Valid for 5 minutes</p>";

        helper.setText(html, true);

        mailSender.send(message);
    }

    public void sendWelcomeEmail(String toEmail) throws Exception {
        String subject = "Welcome to DigitalMSME: Important points for your profile.";
        String body = "Thanks, your email is verified.\n\n"
                + "Welcome to DigitalMSME. Your one-stop solution to make your business digital.\n\n"
                + "As the next step, kindly note:\n\n"
                + commonUsagePoints()
                + "\nLogin: www.digitalmsme.com\n"
                + "Your email is your login ID\n\n"
                + emailFooter();
        sendPlainText(toEmail, subject, body);
    }

    public void sendSubscriptionActivatedEmail(String toEmail,
                                               String planName,
                                               BigDecimal amount,
                                               LocalDateTime startDate,
                                               LocalDateTime endDate) throws Exception {
        String subject = "Payment successful - Subscription activated";
        String body = "Your payment has been successfully processed.\n"
                + "Plan: " + nullSafe(planName) + "\n"
                + "Amount: Rs." + (amount == null ? "0" : amount.toPlainString()) + "\n"
                + "Validity: " + formatDate(startDate) + " to " + formatDate(endDate) + "\n"
                + "You now have full access as per your plan.\n\n"
                + "A few points to note:\n\n"
                + commonUsagePoints()
                + "\n"
                + emailFooter();
        sendPlainText(toEmail, subject, body);
    }

    public void sendSubscriptionExpiryReminderEmail(String toEmail, LocalDateTime expiryDate) throws Exception {
        String subject = "DigitalMSME subscription is expiring soon";
        String body = "Your DigitalMSME subscription will expire on " + formatDate(expiryDate) + ".\n"
                + "To continue uninterrupted access to your personalised business advisory:\n"
                + "Please renew your plan.\n"
                + "Renew now at www.digitalmsme.com\n\n"
                + emailFooter();
        sendPlainText(toEmail, subject, body);
    }

    public void sendSubscriptionExpiredEmail(String toEmail) throws Exception {
        String subject = "Your DigitalMSME subscription has expired";
        String body = "Your DigitalMSME subscription has expired.\n"
                + "You may have extremely limited access to features.\n"
                + "To continue using:\n"
                + "Advisory tools\n"
                + "Financial insights\n"
                + "Business recommendations and more\n"
                + "Renew your plan here:\n"
                + "www.digitalmsme.com\n\n"
                + emailFooter();
        sendPlainText(toEmail, subject, body);
    }

    private void sendPlainText(String toEmail, String subject, String body) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(body, false);
        mailSender.send(message);
    }

    private String commonUsagePoints() {
        return "1) Complete your business profile. The more accurate your profile, the better personalised response you will get.\n"
                + "2) Choose relevant advisory options on your dashboard. You can ask follow-up questions in any language.\n"
                + "3) To get best personalised response, do not enter others' business details or ask on others' behalf.\n"
                + "4) Ask friends, family, or colleagues to create their own account. Do not share your login credentials.\n"
                + "5) For non-business-related questions, use the general chat option.\n";
    }

    private String emailFooter() {
        return "Thanks\n"
                + "Team DigitalMSME\n"
                + "www.digitalmsme.com\n"
                + "MSMEs, Let's Go Digital\n"
                + "digitalmsme.com is an initiative of CEDISI Partners LLP";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "NA" : value.toLocalDate().format(DATE_FORMATTER);
    }

    private String nullSafe(String value) {
        return Objects.toString(value, "NA");
    }
}