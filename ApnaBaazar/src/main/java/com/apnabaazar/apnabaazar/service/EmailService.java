package com.apnabaazar.apnabaazar.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    @Async
    public void sendVerificationEmail(String to, String subject, String token) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String verificationLink = "http://localhost:8080/auth/customer/verify/" + token;
        String emailContent = String.format(
                "<h3>Please verify your email</h3>" +
                        "<p>Click the button below to verify your email address:</p>" +
                        "<a href='%s' style='display: inline-block; padding: 10px 20px; font-size: 16px; color: #ffffff; background-color: #28a745; text-decoration: none; border-radius: 5px;'>Click to Activate</a>" +
                        "<p>Link expires in 3 hours.</p>",
                verificationLink
        );

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);

    }
    @Async
    public void sendVerificationSuccessEmail(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Email Verification Successful</h3>" +
                        "<p>Your email has been successfully verified.</p>"
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }

    @Async
    public void sendSuccessEmailToSeller(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Account Created</h3>" +
                        "<p>Your Seller Account has been created. Waiting for approval.</p>"
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }


    @Async
    public void sellerRegistrationEmail(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Seller Registration Successful</h3>" +
                        "<p>Your registration as a seller has been successfully completed.</p>"
        );

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }
}