package capstoneBackend.ca.sheridancollege.beans;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    private String id;

    /** Matches the User document id */
    private String userId;

    // ── 1. Basic Profile ──────────────────────────────────────────────────────
    private String displayName;
    private String ageGroup;        // "Under 18" | "18–24" | "25–34" | "35–44" | "45+"
    private String gender;          // "Women" | "Men" | "Non-binary" | "Prefer not to say" | custom

    // ── 2. Style Preferences ─────────────────────────────────────────────────
    private List<String> styles;            // up to 3: "Casual", "Streetwear", etc.
    private List<String> favoriteColors;    // "Black", "White", "Pastels", etc.
    private List<String> colorsToAvoid;

    // ── 3. Clothing Preferences ───────────────────────────────────────────────
    private List<String> shoppingFor;       // "Tops", "Bottoms", "Shoes", etc.
    private List<String> preferredFit;      // "Oversized", "Regular", "Slim/Fitted", etc.
    private List<String> preferredFabrics;  // "Cotton", "Linen", "Denim", etc.

    // ── 4. Size & Fit ─────────────────────────────────────────────────────────
    private String topSize;
    private String bottomSize;
    private String shoeSize;
    private List<String> fitConcerns;       // "Petite", "Tall", "Curvy", etc.

    // ── 5. Lifestyle ──────────────────────────────────────────────────────────
    private List<String> dressFor;          // "College/School", "Office", "Gym", etc.
    private String climate;                 // "Cold" | "Moderate" | "Hot" | "Mixed seasons"

    // ── 6. Shopping Preferences ───────────────────────────────────────────────
    private String budgetPerItem;           // "Under $25" | "$25–$50" | etc.
    private String shoppingFrequency;       // "Weekly" | "Monthly" | "Seasonally" | "Only when needed"
    private List<String> shoppingPriorities; // ordered list: "Price", "Quality", "Brand", etc.

    // ── 7. Brand Preferences ──────────────────────────────────────────────────
    private List<String> favoriteBrands;
    private List<String> brandsToAvoid;

    // ── 8. Personalization Features ───────────────────────────────────────────
    private List<String> recommendationBases; // "Current weather", "AI outfit generation", etc.
    private boolean styleNotifications;
}
