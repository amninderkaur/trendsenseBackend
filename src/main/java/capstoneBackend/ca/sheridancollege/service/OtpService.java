package capstoneBackend.ca.sheridancollege.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import capstoneBackend.ca.sheridancollege.beans.OtpToken;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.OtpTokenRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final UserRepository userRepository;

    public void generateAndSendOtp(String email, String deliveryMethod) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));

        otpTokenRepository.deleteByEmail(email);

        OtpToken token = OtpToken.builder()
                .email(email)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        otpTokenRepository.save(token);

        Optional<User> userOpt = userRepository.findByEmail(email);
        String phoneNumber = userOpt.map(User::getPhoneNumber).orElse(null);

        if ("sms".equalsIgnoreCase(deliveryMethod) && phoneNumber != null && !phoneNumber.isBlank()) {
            smsService.sendOtpSms(phoneNumber, otp);
        } else {
            emailService.sendOtpEmail(email, otp);
        }
    }

    public boolean validateOtp(String email, String otp) {
        Optional<OtpToken> tokenOpt = otpTokenRepository.findByEmailAndUsedFalse(email);
        if (tokenOpt.isEmpty()) {
            return false;
        }
        OtpToken token = tokenOpt.get();
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        if (!token.getOtp().equals(otp)) {
            return false;
        }
        token.setUsed(true);
        otpTokenRepository.save(token);
        return true;
    }
}
