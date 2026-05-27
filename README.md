# FashionApp — Backend API

**Stack:** Spring Boot · MongoDB · JWT Authentication · Google Gemini AI · OpenWeather API  
**Package:** `capstoneBackend.ca.sheridancollege`

---

## Local Setup

### Prerequisites

Make sure you have the following installed:

- [Java 21](https://adoptium.net/)
- [Maven](https://maven.apache.org/install.html)
- [MongoDB](https://www.mongodb.com/try/download/community) (local) or a [MongoDB Atlas](https://www.mongodb.com/atlas) connection string
- A code editor (IntelliJ IDEA recommended)

---

### 1. Clone the Repository

```bash
git clone (https://github.com/amninderkaur/FASHIONAPP.git)
git checkout backend
cd ca.sheridancollege
```

---

### 2. Get Your API Keys

You need the following before running the app:

| Key | Where to get it |
|-----|----------------|
| `MONGODB_URI` | [MongoDB Atlas](https://www.mongodb.com/atlas) → Create cluster → Connect → Copy connection string |
| `GEMINI_API_KEY` | [Google AI Studio](https://aistudio.google.com) → Get API Key |
| `OPENWEATHER_API_KEY` | [OpenWeatherMap](https://openweathermap.org/api) → Sign up → API Keys tab |
| `MAIL_USERNAME` | Gmail address used to send emails |
| `MAIL_PASSWORD` | Gmail App Password (not your login password) — [Generate one here](https://myaccount.google.com/apppasswords) |
| `TWILIO_ACCOUNT_SID` | [Twilio Console](https://console.twilio.com) → Account Info |
| `TWILIO_AUTH_TOKEN` | [Twilio Console](https://console.twilio.com) → Account Info |
| `TWILIO_PHONE_NUMBER` | [Twilio Console](https://console.twilio.com) → Phone Numbers — must be in E.164 format |
| `JWT_SECRET` | Any Base64-encoded secret string — a default is provided for local dev |

---

### 3. Set Environment Variables

Create a file called `.env` or set these as environment variables on your machine. **Never commit these to Git.**

```
MONGODB_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/fashionapp
GEMINI_API_KEY=your_gemini_api_key_here
OPENWEATHER_API_KEY=your_openweather_api_key_here
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_PHONE_NUMBER=+1XXXXXXXXXX
```

**Option A — IntelliJ (recommended):**
1. Run → Edit Configurations → Your Spring Boot config
2. Click **Modify options → Environment variables**
3. Paste all variables

**Option B — Terminal (Mac/Linux):**
```bash
export MONGODB_URI="mongodb+srv://..."
export GEMINI_API_KEY="..."
export OPENWEATHER_API_KEY="..."
```

**Option B — Terminal (Windows):**
```cmd
set MONGODB_URI=mongodb+srv://...
set GEMINI_API_KEY=...
set OPENWEATHER_API_KEY=...
```

---

### 4. Install Dependencies

```bash
mvn clean install
```

---

### 5. Run the App

```bash
mvn spring-boot:run
```

The server starts on **http://localhost:8080**

---

### 6. Verify It's Running

```
GET http://localhost:8080/api/profile
```
You should get a `401 Unauthorized` — that means the server is up and JWT auth is working.

---

### Notes

- Max file upload size is **10MB** (configured in `application.properties`)
- Wardrobe images are stored as base64 in MongoDB — no local file system required
- Clothing detection and image generation use **Gemini Vision + Imagen** via the `GEMINI_API_KEY`
- `JWT_SECRET` has a default value in `application.properties` so you don't need to set it locally
- JWT tokens expire after **24 hours** (configurable via `jwt.expiration.ms` in `application.properties`)
- OTP codes expire after **10 minutes** — use `/resend-otp` to get a fresh one (configurable via `otp.expiry.minutes`)

---

## Base URLs

| Environment | URL |
|-------------|-----|
| Local | `http://localhost:8080` |
| Production (Azure) | `https://fashionapp-backend-gtatg0hjbwh4c2dk.canadacentral-01.azurewebsites.net` |

---

## Authentication

All endpoints except **Register** and **Login** require a JWT Bearer token:

```
Authorization: Bearer <token>
```

Get the token from the login or verify-otp response and include it in every request header.

---

## Endpoints

### Auth

#### Register
```
POST /api/v1/auth/register
```
**Body:**
```json
{
  "email": "jane@example.com",
  "password": "password123",
  "name": "Jane",
  "phoneNumber": "+16471234567",
  "deliveryMethod": "email"
}
```
> `name` is the user's display name — collected at sign-up.  
> `phoneNumber` is optional. Required only if `deliveryMethod` is `"sms"`. Must be in E.164 format.  
> `deliveryMethod` is `"email"` (default) or `"sms"`.

**Response:**
```json
{ "message": "Registration successful. Please log in." }
```

---

#### Login
```
POST /api/v1/auth/authenticate
```
**Body:**
```json
{
  "email": "jane@example.com",
  "password": "password123",
  "deliveryMethod": "email"
}
```

**Response — first login (OTP required):**
```json
{
  "message": "OTP sent",
  "requiresOtp": true,
  "deliveryMethod": "email"
}
```

**Response — returning user:**
```json
{
  "token": "eyJhbGci...",
  "userId": "abc123",
  "role": "USER",
  "name": "Jane",
  "profilePicture": "<base64-encoded image or null>",
  "profilePictureType": "image/jpeg",
  "requiresOtp": false
}
```

---

#### Verify OTP
```
POST /api/v1/auth/verify-otp
```
**Body:**
```json
{
  "email": "jane@example.com",
  "otp": "123456"
}
```
**Response:**
```json
{
  "token": "eyJhbGci...",
  "userId": "abc123"
}
```

---

#### Resend OTP
```
POST /api/v1/auth/resend-otp
```
Generates and sends a fresh OTP. The previous OTP is immediately invalidated. Use this when the 10-minute window has expired.

**Body:**
```json
{
  "email": "jane@example.com"
}
```
**Response:**
```json
{ "message": "A new OTP has been sent" }
```

---

#### Forgot Password
```
POST /api/v1/auth/forgot-password
```
Sends an OTP to the user's email to begin the password reset flow. The response is identical whether or not the email exists (to avoid revealing registered accounts).

**Body:**
```json
{
  "email": "jane@example.com"
}
```
**Response:**
```json
{ "message": "If that email is registered, an OTP has been sent" }
```

---

#### Reset Password
```
POST /api/v1/auth/reset-password
```
Verifies the OTP from the forgot-password step, then sets the new password.

**Body:**
```json
{
  "email": "jane@example.com",
  "otp": "123456",
  "newPassword": "newSecurePassword"
}
```
> `newPassword` must be at least 6 characters.

**Response:**
```json
{ "message": "Password reset successful. Please log in." }
```

---

### Profile

#### Save / Update Onboarding Profile
```
POST /api/profile
```
**Body:**
```json
{
  "displayName": "Jane",
  "ageGroup": "18–24",
  "gender": "Women",
  "styles": ["Casual", "Streetwear"],
  "favoriteColors": ["Black", "White"],
  "colorsToAvoid": ["Neon Yellow"],
  "shoppingFor": ["Tops", "Bottoms", "Shoes"],
  "preferredFit": ["Oversized", "Regular"],
  "preferredFabrics": ["Cotton", "Denim"],
  "topSize": "S",
  "bottomSize": "28",
  "shoeSize": "7",
  "fitConcerns": ["Petite"],
  "dressFor": ["College/School", "Casual outings"],
  "climate": "Mixed seasons",
  "budgetPerItem": "$25–$50",
  "shoppingFrequency": "Monthly",
  "shoppingPriorities": ["Price", "Quality"],
  "favoriteBrands": ["Zara", "H&M"],
  "brandsToAvoid": [],
  "recommendationBases": ["Current weather", "AI outfit generation"],
  "styleNotifications": true
}
```
**Response:** Saved `UserProfile` document with `id` and `userId`

---

#### Get Profile
```
GET /api/profile
```
**Response:** Saved `UserProfile` document

---

#### Update Style Preferences
```
PATCH /api/profile/preferences
```
**Body:**
```json
{
  "genderAesthetic": "feminine",
  "modestyLevel": "high",
  "culturalPreferences": ["modest coverage", "no sleeveless"]
}
```
Valid values:
- `genderAesthetic`: `"masculine"` | `"feminine"` | `"androgynous"` | `"mixed"`
- `modestyLevel`: `"low"` | `"medium"` | `"high"` | `"full"`

**Response:**
```json
{
  "message": "Preferences updated successfully",
  "genderAesthetic": "feminine",
  "modestyLevel": "high",
  "culturalPreferences": ["modest coverage", "no sleeveless"]
}
```

---

### Wardrobe

#### Get All Wardrobe Items
```
GET /api/wardrobe
```
**Response:** Array of `ClothingItem` documents, each with `id`, `userId`, `tags` (type, color, style, occasion), `generatedImageBase64`, and `createdAt`.

---

#### Upload Clothing Item
```
POST /api/wardrobe/add
Content-Type: multipart/form-data
```
Sends the photo to Gemini Vision which detects all clothing items in the image, then Imagen generates a clean product photo for each one. All detected items are saved to the wardrobe in one request.

| Field | Type | Description |
|-------|------|-------------|
| `file` | File | JPEG, PNG, or WebP image — can contain multiple clothing items |

**Response:**
```json
{
  "message": "Wardrobe updated successfully",
  "itemsAdded": 2,
  "items": [
    {
      "id": "abc123",
      "tags": { "type": "jacket", "color": "black", "style": "casual", "occasion": ["casual", "work"] },
      "generatedImageBase64": "<base64 PNG>",
      "createdAt": "2025-01-01T00:00:00.000Z"
    }
  ]
}
```

If no clothing is detected:
```json
{ "message": "No clothing items detected in the photo", "items": [] }
```

---

#### Delete a Clothing Item
```
DELETE /api/wardrobe/{id}
```
Permanently removes the item from the user's wardrobe. Returns `404` if not found or belongs to another user.

**Response:** `204 No Content`

---

### Outfit Suggestion

#### Get AI Outfit Suggestion
```
POST /api/outfit/suggest
```
Fetches live weather for the city, selects the best outfit from the user's wardrobe using Gemini AI, and respects saved style preferences.

**Body:**
```json
{
  "occasion": "work",
  "city": "Toronto"
}
```
**Response:**
```json
{
  "selectedItems": [
    { "itemId": "abc123", "type": "shirt", "color": "white", "imageBase64": "..." }
  ],
  "reasoning": "This outfit works well for a work occasion in cool weather.",
  "weatherSummary": "12.0°C, light rain in Toronto"
}
```

---

#### Save Outfit to History
```
POST /api/outfit/history
```
Only saves outfits the user explicitly likes. Nothing is auto-saved. Send the suggestion response body plus `occasion` and `city`.

**Body:**
```json
{
  "occasion": "work",
  "city": "Toronto",
  "weatherSummary": "12.0°C, light rain in Toronto",
  "reasoning": "Great layered look for the weather.",
  "selectedItems": [
    { "itemId": "abc123", "type": "shirt", "color": "white", "imageBase64": "..." }
  ]
}
```
**Response:** Saved `OutfitHistory` document with `id` and `savedAt`

---

#### Get Saved Outfit History
```
GET /api/outfit/history
```
Returns all outfits the user has saved, newest first.

**Response:** Array of `OutfitHistory` documents

---

#### Delete a Saved Outfit
```
DELETE /api/outfit/history/{id}
```
**Response:** `204 No Content`

---

### Outfit Analysis

#### Analyse an Outfit Photo
```
POST /api/outfit/analyze
Content-Type: multipart/form-data
```
Uploads a photo and uses Gemini Vision to evaluate the outfit against weather, style preferences, modesty, and colour season.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `image` | File | Yes | JPEG, PNG, or WebP photo of the outfit |
| `occasion` | String | Yes | What the user is dressing for e.g. `"job interview"`, `"casual brunch"`, `"wedding"` |
| `city` | String | No | City name for live weather e.g. `"Toronto"` — if omitted, weather evaluation is skipped |

**Response:**
```json
{
  "occasion": "casual brunch",
  "styleScore": 7,
  "weatherVerdict": "not suitable",
  "weatherReason": "It's 4°C and raining — this outfit will leave you cold",
  "whatWorksWell": ["The colour palette is cohesive"],
  "suggestions": ["Add a warm coat", "Swap sandals for ankle boots"],
  "overallVerdict": "Great casual look, but needs layers for today's weather.",
  "currentWeather": "Light rain, 4°C, Toronto"
}
```

---

### Colour Analysis

#### Analyse Colour Season from Photo
```
POST /api/colour/analyze
Content-Type: multipart/form-data
```
Combines Gemini Vision analysis of the face photo with the user's self-reported details to determine their 12-season colour type. Result is automatically saved to the user's profile.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `file` | File | Yes | JPEG, PNG, or WebP photo of the person's face |
| `naturalHair` | String | No | Natural hair colour e.g. `"dark brown"` |
| `currentHair` | String | No | Current/dyed hair colour if different |
| `eyeColor` | String | No | Eye colour e.g. `"hazel"` |
| `jewelry` | String | No | Preferred jewelry e.g. `"gold"` |
| `veins` | String | No | Wrist vein colour e.g. `"blue-green"` |
| `sunReaction` | String | No | How skin reacts to sun e.g. `"tans easily"` |

**Response:**
```json
{
  "season": "Soft Autumn",
  "undertone": "Warm",
  "contrast": "Low",
  "bestJewelry": "Gold",
  "summary": "You have warm, muted colouring with low contrast between your skin, hair, and eyes — classic Soft Autumn.",
  "recommendedColors": ["#C4622D", "#8B5E3C", "#D4A853", "#7A6652", "#A0522D", "#5C4033", "#6B4226", "#B8975A", "#9B7653"]
}
```

---

#### Get Saved Colour Analysis
```
GET /api/profile
```
Returns the full user profile including `colourSeason` and `colourPalette` fields. No separate endpoint needed — read these fields from the profile response.

---

#### Clear Colour Analysis
```
DELETE /api/profile/colour
```
Removes the saved colour season and palette from the user's profile. All other profile data is kept.

**Response:**
```json
{ "message": "Colour analysis cleared" }
```

---

### Trip Packing

#### Get AI Packing Suggestion
```
POST /api/packing/suggest
```
**Body:**
```json
{
  "destination": "Paris, France",
  "tripLengthDays": 7,
  "activities": ["sightseeing", "fine dining", "museum visits"]
}
```
**Response:**
```json
{
  "destination": "Paris, France",
  "weatherSummary": "14.0°C, overcast clouds in Paris",
  "packingList": {
    "tops": ["Light knit sweater x3", "Long sleeve shirt x2"],
    "bottoms": ["Tailored trousers x2", "Dark jeans x1"],
    "outerwear": ["Trench coat x1"],
    "shoes": ["Comfortable walking flats x1", "Ankle boots x1"],
    "accessories": ["Scarf x1", "Compact umbrella x1"],
    "extras": ["Adapter plug"]
  },
  "tips": "Pack neutral colours that mix and match easily."
}
```

---

### Shopping Suggestions

#### Get AI Shopping Suggestions
```
POST /api/shopping/suggest
```
Uses Gemini AI with Google Search grounding to find real products in Canada. Automatically uses all profile preferences — styles, colours, fit, fabrics, brands, modesty, budget per item, and more.

**Body:**
```json
{
  "destination": "Canada",
  "budget": 200.00,
  "currency": "CAD",
  "location": "Toronto, Ontario",
  "focusCategory": "outerwear",
  "preferOnline": false
}
```

| Field | Type | Description |
|-------|------|-------------|
| `budget` | Number | Total budget for all suggestions |
| `currency` | String | e.g. `"CAD"`, `"USD"` |
| `location` | String | City for nearby store suggestions |
| `focusCategory` | String | Optional — target a specific category. If empty, gaps are auto-detected from wardrobe |
| `preferOnline` | Boolean | `true` = online only, `false` = include physical stores |

**Response:**
```json
{
  "season": "Autumn",
  "gapsIdentified": ["outerwear", "shoes"],
  "totalEstimate": "$185.00 CAD",
  "withinBudget": true,
  "suggestions": [
    {
      "item": "Camel wool trench coat",
      "category": "outerwear",
      "whyItFits": "Earthy camel tone matches your Autumn palette",
      "estimatedPrice": "$120 CAD",
      "storeName": "Zara Canada",
      "storeType": "Online + In-store",
      "link": "https://www.zara.com/ca/...",
      "nearbyLocation": "Eaton Centre, Toronto"
    }
  ]
}
```

---

#### Save a Shopping Item
```
POST /api/shopping/saved
```
Save a suggestion the user likes (triggered by the Save button on each result).

**Body:** Any suggestion object from the shopping response:
```json
{
  "item": "Camel wool trench coat",
  "category": "outerwear",
  "whyItFits": "Earthy camel tone matches your Autumn palette",
  "estimatedPrice": "$120 CAD",
  "storeName": "Zara Canada",
  "storeType": "Online + In-store",
  "link": "https://www.zara.com/ca/...",
  "nearbyLocation": "Eaton Centre, Toronto"
}
```
**Response:** Saved `SavedShoppingItem` document with `id` and `savedAt`

---

#### Get All Saved Shopping Items
```
GET /api/shopping/saved
```
Returns all saved items, newest first.

**Response:** Array of `SavedShoppingItem` documents

---

#### Delete a Saved Shopping Item
```
DELETE /api/shopping/saved/{id}
```
**Response:** `204 No Content`

---

### AI Chat

#### Fashion Chatbot
```
POST /api/chat
```
Multi-turn Gemini-powered fashion assistant. Send the full conversation history with each request.

**Body:**
```json
{
  "message": "What should I wear to a casual brunch?",
  "history": []
}
```

**Follow-up:**
```json
{
  "message": "What shoes go with that?",
  "history": [
    { "role": "user", "text": "What should I wear to a casual brunch?" },
    { "role": "model", "text": "I'd suggest a linen blouse with straight-leg jeans..." }
  ]
}
```

**Response:**
```json
{ "reply": "White sneakers or loafers would work perfectly..." }
```

---

### Reviews

#### Submit a Review
```
POST /api/v1/reviews
```
**Body:**
```json
{
  "message": "Love the outfit suggestions!",
  "rating": 5
}
```
A confirmation email with the case number is sent to the user automatically.

---

#### Get All Reviews *(Admin only)*
```
GET /api/v1/admin/reviews
```

---

#### Reply to a Review *(Admin only)*
```
POST /api/v1/admin/reviews/{caseNumber}/reply
```
**Body:**
```json
{ "reply": "Thank you for your feedback!" }
```

---

### Admin *(Admin only — requires `ROLE_ADMIN`)*

#### Get All Users
```
GET /api/v1/admin/users
```
**Response:** Array of `{ id, email, name, role, phoneNumber }`

---

#### Get User Count
```
GET /api/v1/admin/users/count
```
**Response:**
```json
{ "totalUsers": 25, "users": 23, "admins": 2 }
```

---

#### Get App Stats
```
GET /api/v1/admin/stats
```
**Response:**
```json
{
  "totalUsers": 25,
  "totalClothingItems": 142,
  "totalOutfitsGenerated": 87,
  "totalReviews": 12,
  "totalSavedItems": 56
}
```

---

#### Edit a User
```
PATCH /api/v1/admin/users/{id}
```
**Body (any subset):**
```json
{ "name": "Jane", "email": "jane@example.com", "phoneNumber": "+16471234567", "role": "ADMIN" }
```
Valid role values: `USER`, `ADMIN`

---

#### Delete a User
```
DELETE /api/v1/admin/users/{id}
```
Deletes the user and all their data (clothing, outfit history, saved shopping, profile, OTP tokens).

**Response:** `204 No Content`

---

#### Send Email to a User
```
POST /api/v1/admin/users/{id}/email
```
**Body:**
```json
{ "subject": "Important update", "content": "Hi Jane, just wanted to let you know..." }
```
**Response:**
```json
{ "message": "Email sent to jane@example.com" }
```

---

### Account

#### Get Current User
```
GET /api/v1/user/me
```
**Response:**
```json
{
  "userId": "abc123",
  "email": "jane@example.com",
  "name": "Jane",
  "profilePicture": "<base64-encoded image or empty string>",
  "profilePictureType": "image/jpeg"
}
```

---

#### Update Profile
```
PATCH /api/v1/user/me
```
Updates name, phone number, and/or OTP delivery method. Send only the fields you want to change.

**Body (any subset):**
```json
{
  "name": "Jane Smith",
  "phoneNumber": "+16471234567",
  "deliveryMethod": "sms"
}
```
> `deliveryMethod` accepts `"email"` or `"sms"`.

**Response:**
```json
{
  "name": "Jane Smith",
  "phoneNumber": "+16471234567",
  "deliveryMethod": "sms"
}
```

---

#### Change Password
```
POST /api/v1/user/me/change-password
```
Verifies the current password, sets the new one, sends a security alert email, and re-triggers OTP verification on next login.

**Body:**
```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newSecurePassword"
}
```
> `newPassword` must be at least 6 characters.

**Response:**
```json
{ "message": "Password changed. An OTP has been sent to verify your identity." }
```

---

#### Upload Profile Picture *(optional)*
```
POST /api/v1/user/me/profile-picture
Content-Type: multipart/form-data
```
| Field | Type | Description |
|-------|------|-------------|
| `file` | File | JPEG, PNG, or WebP — stored as binary in MongoDB |

**Response:**
```json
{
  "message": "Profile picture updated",
  "profilePictureType": "image/jpeg"
}
```

---

#### Delete Account
```
DELETE /api/v1/user/me
```
Permanently deletes the user's account and **all associated data** — wardrobe, outfit history, saved shopping items, profile, OTP tokens. A farewell email is sent to the user's registered address automatically.

**Response:** `204 No Content`

---

## Error Reference

| Status | Meaning |
|--------|---------|
| `400` | Bad request — missing or invalid fields |
| `401` | Unauthorized — missing, invalid, or expired JWT |
| `403` | Forbidden — valid token but accessing another user's resource |
| `404` | Resource not found |
| `405` | Wrong HTTP method |
| `500` | Server error — check Azure logs |
