package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import capstoneBackend.ca.sheridancollege.beans.AuthenticationRequest;
import capstoneBackend.ca.sheridancollege.beans.AuthenticationResponse;
import capstoneBackend.ca.sheridancollege.beans.VerifyOtpRequest;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import capstoneBackend.ca.sheridancollege.service.AuthenticationService;
import capstoneBackend.ca.sheridancollege.service.OtpService;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final OtpService otpService;
    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;

    @PostMapping(value = "/register", consumes = "application/json")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping(value = "/authenticate", consumes = "application/json")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping(value = "/verify-otp", consumes = "application/json")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {
        boolean valid = otpService.validateOtp(request.getEmail(), request.getOtp());
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthenticationResponse.builder().message("Invalid or expired OTP").build());
        }
        return ResponseEntity.ok(authenticationService.generateTokenForVerifiedUser(request.getEmail()));
    }

    /**
     * POST /api/v1/auth/resend-otp
     * Generates and sends a fresh OTP. If a valid (non-expired) OTP already
     * exists it is replaced, so the old code is immediately invalidated.
     * Body: { "email": "user@example.com" }
     */
    @PostMapping(value = "/resend-otp", consumes = "application/json")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        // Make sure the user exists
        if (userRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No account found for this email"));
        }

        // Determine delivery method from user record
        String deliveryMethod = userRepository.findByEmail(email)
                .map(u -> u.getDeliveryMethod())
                .orElse("email");

        // Delete old OTP (expired or not) and send a brand-new one
        otpService.generateAndSendOtp(email, deliveryMethod);

        return ResponseEntity.ok(Map.of("message", "A new OTP has been sent"));
    }
}
