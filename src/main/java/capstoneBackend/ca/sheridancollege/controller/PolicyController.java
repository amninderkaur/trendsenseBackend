package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trendsense")
public class PolicyController {

    @GetMapping(value = "/privacy-policy", produces = "text/html")
    public String privacyPolicy() {
        return """
                <!DOCTYPE html>
                <html>
                <head><title>TrendSense Privacy Policy</title></head>
                <body>
                <h1>TrendSense Privacy Policy</h1>
                <p><strong>Last updated: May 25, 2026</strong></p>

                <h2>1. About TrendSense</h2>
                <p>TrendSense is a fashion styling app developed by students at Sheridan College. This app is a capstone project and is not intended for commercial use.</p>

                <h2>2. Information We Collect</h2>
                <ul>
                <li>Name and email address upon registration</li>
                <li>Outfit preferences and saved moodboard pins</li>
                <li>App usage data</li>
                </ul>

                <h2>3. How We Use Your Information</h2>
                <ul>
                <li>To create and manage your account</li>
                <li>To provide personalized outfit recommendations</li>
                <li>To save your Pinterest moodboard pins</li>
                </ul>

                <h2>4. Pinterest Integration</h2>
                <p>TrendSense uses the Pinterest API to allow users to search and save fashion pins. We do not store your Pinterest credentials.</p>

                <h2>5. Data Storage</h2>
                <p>Your data is stored securely on MongoDB Atlas cloud servers located in Canada.</p>

                <h2>6. Data Sharing</h2>
                <p>We do not sell or share your personal data with any third parties.</p>

                <h2>7. Contact Us</h2>
                <p>For any questions about this privacy policy, contact us at: heytrendsense@gmail.com</p>

                <h2>8. Changes to This Policy</h2>
                <p>This policy may be updated at any time during the development of the project.</p>
                </body>
                </html>
                """;
    }
}
