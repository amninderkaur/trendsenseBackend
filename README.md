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
cd ca.sheridancolleg
```

---

### 2. Get Your API Keys

You need 3 API keys before running the app:

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

---

### 3. Set Environment Variables

Create a file called `.env` or set these as environment variables on your machine. **Never commit these to Git.**

The app reads the following variables:

```
MONGODB_URI=mongodb+srv://<username>:<password>@cluster.mongodb.net/fashionapp
GEMINI_API_KEY=your_gemini_api_key_here
OPENWEATHER_API_KEY=your_openweather_api_key_here
AI_SERVICE_URL=http://127.0.0.1:5000
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
export AI_SERVICE_URL="http://127.0.0.1:5000"
```

**Option B — Terminal (Windows):**
```cmd
set MONGODB_URI=mongodb+srv://...
set GEMINI_API_KEY=...
set OPENWEATHER_API_KEY=...
set AI_SERVICE_URL=http://127.0.0.1:5000
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

Open your browser or Postman and hit:
```
GET http://localhost:8080/api/profile
```
You should get a `401 Unauthorized` — that means the server is up and JWT auth is working.

---

### Notes

- Max file upload size is **10MB** (configured in `application.properties`)
- Uploaded wardrobe images are saved locally to `uploads/wardrobe/`
- The `AI_SERVICE_URL` points to the Python FastAPI clothing detection service — if you don't have it running locally, wardrobe uploads will fail but all other endpoints will still work
- `JWT_SECRET` has a default value in `application.properties` so you don't need to set it locally

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

Get the token from the login response and include it in every request header.

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
  "phoneNumber": "+16471234567",
  "deliveryMethod": "email"
}
```
> `phoneNumber` is optional. Required only if `deliveryMethod` is `"sms"`. Must be in E.164 format e.g. `+16471234567`.  
> `deliveryMethod` is `"email"` (default) or `"sms"`. This is saved to the user and used for all future OTP and review notifications.

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
> `deliveryMethod` is `"email"` (default) or `"sms"`. Only applies on first login.

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
  "requiresOtp": false
}
```

---

#### Verify OTP
```
POST /api/v1/auth/verify-otp
```
Verifies the OTP sent during first login. Returns a JWT token on success.

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
  "culturalPreferences": ["modest coverage", "no sleeveless", "full length bottoms"]
}
```

Valid values:
- `genderAesthetic`: `"masculine"` | `"feminine"` | `"androgynous"` | `"mixed"`
- `modestyLevel`: `"low"` | `"medium"` | `"high"` | `"full"`
- `culturalPreferences`: free array of strings

**Response:**
```json
{
  "message": "Preferences updated successfully",
  "genderAesthetic": "feminine",
  "modestyLevel": "high",
  "culturalPreferences": ["modest coverage", "no sleeveless", "full length bottoms"]
}
```

---

### Wardrobe

#### Get All Wardrobe Items
```
GET /api/v1/wardrobe
```
**Response:** Array of `WardrobeItem` documents

---

#### Get Single Wardrobe Item
```
GET /api/v1/wardrobe/{id}
```
**Response:** Single `WardrobeItem` document

---

#### Upload Clothing Item
```
POST /api/v1/wardrobe/upload
Content-Type: multipart/form-data
```
| Field | Type | Description |
|-------|------|-------------|
| `file` | File | JPEG, PNG, or WebP image of the clothing item |

**Response:**
```json
{
  "message": "Clothing item added successfully",
  "item": { "id": "...", "tag": "shirt", "cropUrls": ["..."] },
  "detections": [ { "class": "shirt", "confidence": 0.97 } ]
}
```

---

#### Mark Item as Worn
```
PATCH /api/v1/wardrobe/{itemId}/worn
```
No body needed. Increments the item's wear count by 1. Call this when a user accepts an outfit suggestion.

**Response:**
```json
{
  "itemId": "abc123",
  "name": "White shirt",
  "wearCount": 4
}
```

---

#### Get Wardrobe Usage Stats
```
GET /api/v1/wardrobe/usage
```
Returns the user's wardrobe sorted by least worn first, useful for surfacing forgotten items.

**Response:**
```json
{
  "leastWorn": [
    { "itemId": "xyz789", "name": "Red blazer", "wearCount": 0, "category": "outerwear" },
    { "itemId": "abc123", "name": "Floral dress", "wearCount": 1, "category": "dress" }
  ],
  "totalItems": 24,
  "unwornCount": 8
}
```

---

### Outfit Suggestion

#### Get AI Outfit Suggestion
```
POST /api/outfit/suggest
```
Fetches live weather for the city, selects the best outfit from the user's wardrobe using Gemini AI, and respects the user's saved style preferences.

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
    {
      "itemId": "abc123",
      "type": "shirt",
      "color": "white",
      "imageBase64": "..."
    }
  ],
  "reasoning": "This outfit works well for a work occasion in cool weather.",
  "weatherSummary": "12.0°C, light rain in Toronto"
}
```

---

### Outfit Analysis

#### Analyse an Outfit Photo
```
POST /api/outfit/analyze
Content-Type: multipart/form-data
```
Uploads a photo of an outfit, fetches live weather for the given city, and uses Gemini Vision to evaluate the outfit against the weather, the user's style preferences, modesty level, cultural preferences, and colour season. Fully stateless — nothing is saved to the database.

| Field | Type | Description |
|-------|------|-------------|
| `image` | File | JPEG, PNG, or WebP photo of the outfit |
| `city` | String | City name for live weather lookup e.g. `"Toronto"` |

**Response:**
```json
{
  "occasion": "casual brunch",
  "styleScore": 7,
  "weatherVerdict": "not suitable",
  "weatherReason": "It's 4°C and raining — this outfit will leave you cold",
  "whatWorksWell": [
    "The colour palette is cohesive",
    "The silhouette is flattering for a casual setting"
  ],
  "suggestions": [
    "Add a warm coat or trench over the top",
    "Swap the open-toe sandals for ankle boots",
    "A scarf would add warmth without disrupting the look"
  ],
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
| Field | Type | Description |
|-------|------|-------------|
| `file` | File | JPEG, PNG, or WebP photo of the person's face |

Gemini Vision reads the person's skin tone, eye colour, and hair colour directly from the photo. Determines their colour season and returns a recommended hex colour palette. Result is automatically saved to the user's profile.

**Response:**
```json
{
  "season": "Autumn",
  "description": "Warm olive skin with dark brown eyes detected. Earthy tones complement your features best.",
  "palette": {
    "tops": ["#C4622D", "#8B5E3C", "#D4A853"],
    "bottoms": ["#5C4033", "#7A6652", "#3B2F2F"],
    "outerwear": ["#8B4513", "#6B4226", "#A0522D"],
    "shoes": ["#5C3317", "#8B6914", "#704214"]
  }
}
```

---

### Trip Packing

#### Get AI Packing Suggestion
```
POST /api/packing/suggest
```
Fetches live weather for the destination and generates a personalised packing list using Gemini AI, taking into account the user's style preferences and colour season.

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
    "tops": ["Light knit sweater x3", "Long sleeve shirt x2", "Blouse x2"],
    "bottoms": ["Tailored trousers x2", "Dark jeans x1", "Midi skirt x1"],
    "outerwear": ["Trench coat x1", "Light rain jacket x1"],
    "shoes": ["Comfortable walking flats x1", "Ankle boots x1"],
    "accessories": ["Scarf x1", "Compact umbrella x1"],
    "extras": ["Adapter plug", "Comfortable bag for day trips"]
  },
  "tips": "Pack neutral colours that mix and match easily for a 7-day trip."
}
```

---

### Mood Board

#### Save a Mood Board
```
POST /api/moodboard
```
**Body:**
```json
{
  "mood": "minimal",
  "savedOutfits": [
    {
      "itemIds": ["itemId1", "itemId2"],
      "description": "Clean white shirt with beige trousers"
    }
  ]
}
```
**Response:**
```json
{
  "id": "moodboardId123",
  "mood": "minimal",
  "savedOutfits": [ { "itemIds": ["itemId1", "itemId2"], "description": "..." } ],
  "createdAt": "2025-05-13T10:00:00Z"
}
```

---

#### Get All Mood Boards
```
GET /api/moodboard
```
**Response:** Array of all saved mood boards for the authenticated user

---

#### Match Wardrobe to a Mood
```
POST /api/moodboard/match
```
Gemini picks 2–3 outfit combinations from the user's actual wardrobe items that match the selected mood, occasion, and weather. User style preferences are also applied.

**Body:**
```json
{
  "mood": "minimal",
  "occasion": "work",
  "weather": "cool"
}
```
**Response:**
```json
{
  "mood": "minimal",
  "outfitSuggestions": [
    {
      "description": "Clean monochrome look",
      "items": [
        { "itemId": "abc123", "name": "White oversized shirt", "category": "top" },
        { "itemId": "def456", "name": "Black straight leg trousers", "category": "bottom" }
      ]
    },
    {
      "description": "Neutral tones with clean lines",
      "items": [
        { "itemId": "ghi789", "name": "Beige knit", "category": "top" },
        { "itemId": "jkl012", "name": "Cream wide leg pants", "category": "bottom" }
      ]
    }
  ]
}
```

---

### Shopping Suggestions

#### Get AI Shopping Suggestions
```
POST /api/shopping/suggest
```
Uses Gemini AI with **Google Search grounding** to find real products available in Canada that fill gaps in the user's wardrobe. Personalises results using the user's colour season, style preferences, modesty level, and cultural preferences. Returns real product links sourced from live web search.

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
| `destination` | String | Country/region for product availability |
| `budget` | Number | Total budget for all suggestions |
| `currency` | String | Currency code e.g. `"CAD"`, `"USD"` |
| `location` | String | User's city for nearby store suggestions |
| `focusCategory` | String | Optional — target a specific gap e.g. `"shoes"`. If empty, gaps are auto-detected from wardrobe |
| `preferOnline` | Boolean | `true` = online stores only, `false` = include physical stores |

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
      "whyItFits": "Earthy camel tone matches your Autumn palette perfectly",
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

### AI Chat

#### Fashion Chatbot
```
POST /api/chat
```
Multi-turn Gemini-powered fashion assistant. The server is stateless — send the full conversation history with each request so context is maintained.

**First message:**
```json
{
  "message": "What should I wear to a casual brunch?",
  "history": []
}
```

**Follow-up message:**
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
{
  "reply": "White sneakers or loafers would work perfectly with that look..."
}
```

---

### Reviews

#### Submit a Review
```
POST /api/v1/reviews
```
Any authenticated user can submit a review. A confirmation email with the case number is sent to the user automatically.

**Body:**
```json
{
  "message": "Love the outfit suggestions!",
  "rating": 5
}
```
**Response:**
```json
{
  "id": "...",
  "caseNumber": 1,
  "userId": "abc123",
  "email": "jane@example.com",
  "message": "Love the outfit suggestions!",
  "rating": 5,
  "adminReply": null,
  "createdAt": "2025-05-20T10:00:00Z"
}
```

---

#### Get All Reviews *(Admin only)*
```
GET /api/v1/admin/reviews
```
Returns all reviews submitted by users. Requires `ADMIN` role.

**Response:** Array of review objects.

---

#### Reply to a Review *(Admin only)*
```
POST /api/v1/admin/reviews/{caseNumber}/reply
```
Admin replies to a review by case number. The reply is saved to the review and emailed to the user.

**Body:**
```json
{
  "reply": "Thank you for your feedback! We are looking into this."
}
```
**Response:** Updated review object with `adminReply` filled in.

---

## Error Reference

| Status | Meaning |
|--------|---------|
| `400` | Bad request — missing or invalid fields |
| `401` | Unauthorized — missing or invalid JWT token |
| `403` | Forbidden — valid token but accessing another user's resource |
| `404` | Resource not found |
| `405` | Wrong HTTP method (e.g. GET instead of POST) |
| `500` | Server error — check Azure logs |
