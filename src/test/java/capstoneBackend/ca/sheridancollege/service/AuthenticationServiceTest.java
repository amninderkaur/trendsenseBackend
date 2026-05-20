package capstoneBackend.ca.sheridancollege.service;

import capstoneBackend.ca.sheridancollege.beans.*;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_ShouldReturnToken_WhenEmailNotTaken() {
        AuthenticationRequest request = new AuthenticationRequest("new@test.com", "pass123", null, "email");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
        when(userRepository.save(any())).thenReturn(new User());

        AuthenticationResponse response = authenticationService.register(request);

        assertEquals("Registration successful. Please log in.", response.getMessage());
    }

    @Test
    void register_ShouldThrow_WhenEmailAlreadyExists() {
        AuthenticationRequest request = new AuthenticationRequest("existing@test.com", "pass", null, "email");

        when(userRepository.findByEmail("existing@test.com"))
                .thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> authenticationService.register(request));
    }

    @Test
    void authenticate_ShouldReturnToken_WhenCredentialsValid() {
        AuthenticationRequest request = new AuthenticationRequest("user@test.com", "pass", null, "email");
        User user = User.builder().email("user@test.com").password("encoded").role(Role.USER).hasLoggedInBefore(true).build();

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("USER", response.getRole());
    }
}
