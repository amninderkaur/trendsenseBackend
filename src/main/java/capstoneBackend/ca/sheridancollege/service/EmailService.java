package capstoneBackend.ca.sheridancollege.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Your Trend Sense verification code");
        message.setText("Your code is: " + otp + ". Expires in 10 minutes.");
        mailSender.send(message);
    }

    public void sendReviewConfirmation(String toEmail, long caseNumber, String reviewMessage, int rating) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Trend Sense — Review Received (Case #" + caseNumber + ")");
        message.setText(
            "Hi,\n\n" +
            "Thank you for your feedback! We have received your review.\n\n" +
            "Case Number: #" + caseNumber + "\n" +
            "Rating: " + rating + "/5\n" +
            "Message: " + reviewMessage + "\n\n" +
            "Our team will get back to you if needed.\n\n" +
            "— Trend Sense Team"
        );
        mailSender.send(message);
    }

    public void sendAccountDeletionEmail(String toEmail, String displayName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Trend Sense — Your account has been deleted");
        message.setText(
            "Hi " + (displayName != null ? displayName : "there") + ",\n\n" +
            "We're sorry to see you go. Your Trend Sense account and all associated data " +
            "have been permanently deleted as requested.\n\n" +
            "If this was a mistake or you change your mind, you're always welcome to create " +
            "a new account at any time.\n\n" +
            "Thank you for being part of Trend Sense.\n\n" +
            "— Trend Sense Team"
        );
        mailSender.send(message);
    }

    public void sendAdminReply(String toEmail, long caseNumber, String reply) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Trend Sense — Response to Your Review (Case #" + caseNumber + ")");
        message.setText(
            "Hi,\n\n" +
            "Our team has responded to your review (Case #" + caseNumber + "):\n\n" +
            reply + "\n\n" +
            "— Trend Sense Team"
        );
        mailSender.send(message);
    }
}
