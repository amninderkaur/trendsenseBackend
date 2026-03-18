package capstoneBackend.ca.sheridancollege.service;

import capstoneBackend.ca.sheridancollege.beans.Role;
import capstoneBackend.ca.sheridancollege.beans.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private User makeUser() {
        return User.builder()
                .email("test@test.com")
                .password("password")
                .role(Role.USER)
                .build();
    }

    @Test
    void generateToken_ShouldReturnNonNullToken() {
        String token = jwtService.generateToken(makeUser());
        assertNotNull(token);
    }

    @Test
    void extractUsername_ShouldReturnCorrectEmail() {
        User user = makeUser();
        String token = jwtService.generateToken(user);
        assertEquals("test@test.com", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_ShouldReturnTrueForValidToken() {
        User user = makeUser();
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void isTokenValid_ShouldReturnFalseForWrongUser() {
        User user1 = makeUser();
        User user2 = User.builder().email("other@test.com").password("pass").role(Role.USER).build();
        String token = jwtService.generateToken(user1);
        assertFalse(jwtService.isTokenValid(token, user2));
    }
}
