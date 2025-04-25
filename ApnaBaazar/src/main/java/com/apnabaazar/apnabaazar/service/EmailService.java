package com.apnabaazar.apnabaazar.service;

import com.apnabaazar.apnabaazar.model.products.Product;
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

        String verificationLink = "http://localhost:8080/auth/verify/" + token;
        String emailContent = String.format(
                "<h3>Please verify your email</h3>" +
                        "<p>Click the button below to verify your email address:</p>" +
                        "<a href='%s' style='display: inline-block; padding: 10px 20px; font-size: 16px; color: #ffffff; background-color: #28a745; text-decoration: none; border-radius: 5px;'>Click to Activate</a>" +
                        "<p>Link expires in 3 hours.</p>" +
                        "<br><img src='https://media.makeameme.org/created/register-now-you-5bf869.jpg' width='300' height='300' alt='Company Logo'/>",
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
                        "<p>Your email has been successfully verified.</p>",
                "<br><img src='https://framerusercontent.com/images/Csc0qjXRWqlnkKQ34jtyO3bardw.jpeg' width='300' height='300' alt='Company Logo'/>"

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
    public void sendResetPasswordEmail(String to, String subject, String token) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String verificationLink = "http://localhost:8080/auth/forgot-password/" + token;
        String emailContent = String.format(
                "<h3>Please verify your email</h3>" +
                        "<p>Click the button below to reset your password:</p>" +
                        "<a href='%s' style='display: inline-block; padding: 10px 20px; font-size: 16px; color: #ffffff; background-color: #28a745; text-decoration: none; border-radius: 5px;'>Click to Activate</a>" +
                        "<p>Link valid for 15 minutes.</p>" +
                        "<br><img src='https://media.makeameme.org/created/you-forgot-again-5c09c4.jpg' width='300' height='300' alt='Company Logo'/>",
                verificationLink
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);

    }

    @Async
    public void sendAccountLockedEmail(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Account Locked</h3>" +
                        "<p>Your Account has been locked for entering multiple wrong credentials. Reset your password.</p>"+
                        "<br><img src='https://i.imgflip.com/3m564o.jpg' width='300' height='300' alt='Company Logo'/>"
                );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);

    }


    public void sendAccountDeactivationEmail(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Account Locked</h3>" +
                        "<p>Your Account has been Deactivated. Contact with admin.</p>"
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }

    @Async
    public void sendProductAddedMail(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Product Added</h3>" +
                        "<p>A new product has been added. Please review and active it.</p>"
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }

    @Async
    public void sendProductDeactivationEmail(String to, String subject, Product product) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Product Deactivated</h3>" +
                        "<p>Your product \"%s\" has been deactivated by admin.</p>" +
                        "<p>Product details:</p>" +
                        "<ul>" +
                        "<li>Product ID: %s</li>" +
                        "<li>Name: %s</li>" +
                        "<li>Brand: %s</li>" +
                        "</ul>" +
                        "<p>Please contact support if you have any questions.</p>",
                product.getName(), product.getId(), product.getName(), product.getBrand()
        );

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }

    @Async
    public void sendProductActivationEmail(String to, String subject, Product product) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Product Activated</h3>" +
                        "<p>Your product \"%s\" has been activated and is now available for customers.</p>" +
                        "<p>Product details:</p>" +
                        "<ul>" +
                        "<li>Product ID: %s</li>" +
                        "<li>Name: %s</li>" +
                        "<li>Brand: %s</li>" +
                        "</ul>" +
                        "<p>Thank you for choosing our platform!</p>",
                product.getName(), product.getId(), product.getName(), product.getBrand()
        );

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);
    }

    @Async
    public void sendAccountUnlockedEmail(String to, String subject) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String emailContent = String.format(
                "<h3>Account Unlocked</h3>" +
                        "<p>Your Account has been unlocked.</p>"
        );
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        emailSender.send(message);

    }
}