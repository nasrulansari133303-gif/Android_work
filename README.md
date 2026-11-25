# Kaam-Mitra — Complete Service Booking Platform

**Kaam-Mitra** is a production-oriented local service marketplace connecting **Customers**, **Verified Service Partners (Workers)**, and **Platform Admins** over a real **Spring Boot + PostgreSQL** backend, **JWT authentication**, **WebSocket live tracking**, **Google Maps & Fused Location**, and **Firebase Cloud Messaging (FCM)**.

```text
Customer Creates Job ──▶ Spring Boot Haversine Matching ──▶ Nearby Verified Online Workers
         │                                                            │
         ▼                                                            ▼
   Customer Rates ◀── Customer Pays ◀── Worker Completes ◀── Worker Accepts & Navigates
                                              │
                                              ▼
                               Admin Verifies, Monitors & Resolves
```

---

## 1. Architecture & Stack

### Android App (`/app`)
- **Language**: Java 17 **ONLY** (No Kotlin, No Jetpack Compose)
- **UI**: XML Layouts + Material Design 3 + ViewBinding + Navigation Component
- **Architecture**: MVVM (`ViewModel` + `LiveData` + `Repository`)
- **Networking & Real-Time**: Retrofit 2 + OkHttp 4 + Gson + OkHttp WebSocket (`/ws`)
- **Maps & Location**: Google Maps SDK + Fused Location Provider + Geocoder
- **Notifications**: Firebase Cloud Messaging (`KaamMitraMessagingService`) + Notification Channels
- **Media & Play Policy**: Zero-permission Android Photo Picker (`PickVisualMedia`) + `FileProvider` camera capture (no broad storage permissions)

### Spring Boot Backend (`/backend`)
- **Language / Runtime**: Java 21 + Spring Boot 3.4
- **Security**: Spring Security + Stateless JWT (`ROLE_CUSTOMER`, `ROLE_WORKER`, `ROLE_ADMIN`) + BCrypt password hashing
- **Persistence**: Spring Data JPA / Hibernate + PostgreSQL (`kaam_mitra_db`)
- **Real-Time**: Native Spring WebSocket (`/ws?token=<JWT>`) + Firebase Admin SDK FCM

---

## 2. Prerequisites

1. **Java 21** (for Spring Boot backend) & **Maven 3.9+**
2. **PostgreSQL 15+**
3. **Android Studio** (Ladybug / Koala or newer) with Android SDK 36
4. **Google Maps API Key** (Maps SDK for Android)
5. **Firebase Project** (optional for push notifications; WebSockets work out of the box)

---

## 3. PostgreSQL Database Setup

Create the PostgreSQL database `kaam_mitra_db`:

```sql
CREATE DATABASE kaam_mitra_db;
```

Default development configuration in `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/kaam_mitra_db
spring.datasource.username=postgres
spring.datasource.password=123456
spring.jpa.hibernate.ddl-auto=update
server.port=8080
server.address=0.0.0.0
```

For production deployments, **never hardcode credentials** — supply environment variables:
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (at least 32+ characters random Base64/ASCII secret)
- `FIREBASE_SERVICE_ACCOUNT_PATH`

A complete normalized SQL reference schema is also included in `backend/src/main/resources/schema.sql`.

---

## 4. Running the Spring Boot Backend

```bash
cd backend
mvn spring-boot:run
```

On startup, `DataSeeder` seeds:
- **12 Popular Service Categories**: Electrician, Plumber, AC Repair, Carpenter, Painter, Cleaning, RO Repair, Mobile Repair, Computer Repair, Refrigerator Repair, Washing Machine Repair, Bike Repair.
- **Development Accounts** (dev profile only):
  - **Admin**: `admin@kaammitra.local` / `Admin@123`
  - **Customer**: `customer@kaammitra.local` / `Customer@123`
  - **Verified Worker**: `worker@kaammitra.local` / `Worker@123`

Run backend integration tests (uses H2 in PostgreSQL compatibility mode):

```bash
cd backend
mvn test
```

---

## 5. Android Configuration (Maps, Firebase, Server URL)

### A. Google Maps API Key
- In AI Studio, open the **Secrets** panel and set `MAPS_API_KEY` to your Google Maps Android API key.
- Locally in Android Studio, create a `.env` file in the project root (never commit `.env`):
  ```properties
  MAPS_API_KEY=your_google_maps_api_key
  ```
- Restrict your Google Maps key in Google Cloud Console to your app's package name (`com.aistudio.kaammitra.rtxkzm`) and SHA-1 fingerprint.

### B. Firebase Cloud Messaging (FCM)
1. In Firebase Console, register an Android app with package `com.aistudio.kaammitra.rtxkzm` and place `google-services.json` inside `app/`.
2. For backend push delivery, download a Firebase Admin service-account JSON and set `FIREBASE_SERVICE_ACCOUNT_PATH=/path/to/serviceAccount.json` (or set `app.firebase.enabled=true` in `application.properties`).
3. If Firebase is not yet configured, `KaamMitraApp` detects the absence safely and relies on the active WebSocket channel without crashing.

### C. Server URL (Emulator vs. Physical Device)
- **Android Emulator**: Uses `http://10.0.2.2:8080/` automatically (`localhost` of host PC).
- **Physical Phone (same Wi-Fi)**: Tap **Server URL Settings** on the Login screen and enter `http://<YOUR_PC_LAN_IP>:8080/` (e.g., `http://192.168.1.25:8080/`). Cleartext HTTP for local development LAN/emulator hosts is configured in `app/src/main/res/xml/network_security_config.xml`.

---

## 6. Key API & WebSocket Endpoints

| Domain | Endpoints |
|---|---|
| Auth | `POST /api/v1/auth/login`, `/register/customer`, `/register/worker` |
| Files | `POST /api/v1/files/upload`, `GET /uploads/{folder}/{file}` |
| Customer | `GET /api/v1/services`, `GET/POST /api/v1/customer/addresses`, `POST/GET /api/v1/customer/jobs`, `GET /api/v1/customer/jobs/{id}`, `POST .../cancel`, `.../pay`, `.../rate`, `GET /api/v1/customer/payments` |
| Worker | `GET/PUT /api/v1/worker/profile`, `PUT /api/v1/worker/services`, `PUT /api/v1/worker/status`, `POST /api/v1/worker/location`, `GET /api/v1/worker/jobs`, `POST .../accept`, `.../reject`, `.../on-the-way`, `.../arrived`, `.../start`, `.../complete`, `GET /api/v1/worker/earnings` |
| Admin | `GET /api/v1/admin/dashboard`, `GET /api/v1/admin/workers`, `PUT .../verify`, `.../reject`, `.../suspend`, `.../activate`, `GET /api/v1/admin/jobs`, `PUT .../reassign`, `POST .../cancel`, `GET/POST/PUT /api/v1/admin/services`, `GET /api/v1/admin/payments`, `POST .../refund`, `GET/PUT /api/v1/admin/complaints` |
| WebSocket | `ws://<host>:8080/ws?token=<JWT>` (`NEW_JOB_REQUEST`, `JOB_ACCEPTED`, `JOB_STATUS_UPDATED`, `JOB_CANCELLED`, `WORKER_LOCATION_UPDATE`) |

---

## 7. Troubleshooting

- **Network connection failed on Login**: Ensure Spring Boot is running on port `8080` and `server.address=0.0.0.0`. On a physical device, check Windows/Linux firewall allows TCP `8080` and update **Server URL Settings** to your PC's LAN IP.
- **Worker cannot go ONLINE**: New worker accounts start as `PENDING_VERIFICATION`. Sign in as Admin (`admin@kaammitra.local`), open the **Workers** tab, and tap **Verify**. Also verify GPS/Location permission is granted.
- **No nearby workers matched**: Worker must be **VERIFIED**, **ONLINE**, **not busy** on another job, have selected the matching service, and have updated GPS coordinates within 25 km (`app.matching.radius-km`).
- **Map tiles blank**: Verify `MAPS_API_KEY` is configured in AI Studio Secrets or root `.env` with **Maps SDK for Android** enabled in Google Cloud Console.
