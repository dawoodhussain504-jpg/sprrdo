# Speedo Android Multi-Module Applications

Three native Android applications built with **Kotlin**, **Jetpack Compose**, and **Material 3**, interconnected through a centralized backend:
1. **Rider App (`:rider-app`)** — Ride search, osmdroid CARTO map tracking, fare estimation, OTP verification, trip history.
2. **Captain App (`:captain-app`)** — KYC multi-document upload, Foreground GPS location tracking, incoming ride request alerts, active navigation, payment QR display.
3. **Admin App (`:admin-app`)** — KYC review inspector, live fleet map, ride monitoring with status filters, user moderation.

---

## 🏗️ Multi-Module Architecture

```
android/
├── gradle/
│   └── libs.versions.toml       # Pinned dependencies catalog
├── core/                        # Shared module (:core)
│   ├── model/                   # Domain data models & API responses
│   ├── network/                 # Retrofit 2 + OkHttp + dynamic AuthInterceptor
│   ├── storage/                 # EncryptedSharedPreferences (TokenManager)
│   ├── database/                # Room offline cache (SpeedoDatabase, DAOs, Entities)
│   ├── theme/                   # Custom Material 3 theme (Speedo Orange & White)
│   ├── components/              # Buttons, TextFields, Cards, StatusBadges
│   ├── maps/                    # osmdroid CARTO map wrapper & vector marker generators
│   ├── utils/                   # LocationHelper, NotificationHelper, BadgeHelper
│   └── work/                    # WorkManager periodic notification sync
├── rider-app/                   # Rider Application APK (:rider-app)
├── captain-app/                 # Captain Application APK (:captain-app)
└── admin-app/                   # Admin Application APK (:admin-app)
```

---

## 🗺️ Map Integration Specifics (osmdroid + CARTO)

- **Map Engine:** `osmdroid` native Android map library (no Google Maps API key required).
- **Tile Sources:** Styled via CARTO tile templates (Voyager & Positron).
- **Custom Vector Markers:** Dynamically generated bitmap markers matching the Speedo orange & white design system (Pickup Pin, Destination Pin, Directional Captain Vehicle Marker).
- **Polyline Routing:** Dynamic route overlays rendered between pickup and drop points.

---

## ⚙️ Building & Running in Android Studio

### 1. Configure Backend API URL
Copy `local.properties.example` to `local.properties` (or edit inside app profile screen):
```properties
# Android Emulator connecting to host backend:
SPEEDO_API_BASE_URL="http://10.0.2.2:5000/api/"

# Physical device on local Wi-Fi:
# SPEEDO_API_BASE_URL="http://192.168.1.100:5000/api/"

# Production Railway deployment:
# SPEEDO_API_BASE_URL="https://speedo-backend-production.up.railway.app/api/"
```

### 2. Open Project in Android Studio
1. Open Android Studio $\rightarrow$ **Open Project** $\rightarrow$ select `d:/speedo/android`.
2. Let Gradle sync project modules (`:core`, `:rider-app`, `:captain-app`, `:admin-app`).

### 3. Run Specific App Modules
Select the run configuration dropdown in Android Studio:
- Select **`rider-app`** $\rightarrow$ Click Run (Installs `com.speedo.rider`)
- Select **`captain-app`** $\rightarrow$ Click Run (Installs `com.speedo.captain`)
- Select **`admin-app`** $\rightarrow$ Click Run (Installs `com.speedo.admin`)
