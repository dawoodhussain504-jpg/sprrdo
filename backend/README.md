# Speedo Centralized Backend REST API

Centralized REST API backend for the **Speedo Ride-Hailing Platform**, serving all 3 native Android applications (**Rider App**, **Captain App**, **Admin App**) with a unified database hosted on Railway / PostgreSQL / SQLite.

---

## 🚀 Key Capabilities

- **Role-Scoped JWT Authentication:** Strict role guards (`/api/rider/*`, `/api/captain/*`, `/api/admin/*`).
- **Dynamic Database Adapter:** Native support for PostgreSQL on Railway with seamless local SQLite fallback (`speedo.db`).
- **Multipart Document Uploads:** Endpoints for Vehicle RC, Aadhaar ID, Camera Live Selfie, and UPI Payment QR images.
- **Real-Time Polling Engine:** High-performance polling architecture for Captain GPS tracking (5s push, 3–5s rider tracking), incoming ride alerts, and notification badge counts.
- **Automated OTP & Fare Verification:** 4-digit ride OTP verification and dynamic distance/time/pricing engine for Bike, Auto, and Cab.

---

## 🛠️ Tech Stack

- **Runtime:** Node.js (TypeScript / Express)
- **Database:** PostgreSQL (Cloud / Railway) or SQLite (`speedo.db` for zero-setup local development)
- **Authentication:** JWT (`jsonwebtoken`) + Password Hashing (`bcryptjs`)
- **File Uploads:** Multer with static asset serving (`/uploads/*`) and Cloudinary/S3 compatibility
- **Tests:** Integrated TypeScript end-to-end API test suite

---

## 📦 Setup & Running Locally

### 1. Install Dependencies
```bash
cd backend
npm install
```

### 2. Configure Environment (.env)
Copy `.env.example` to `.env`:
```env
PORT=5000
NODE_ENV=development
JWT_SECRET=speedo_super_secret_jwt_key_rapido_2025_prod_safe
BASE_URL=http://localhost:5000
# Leave DATABASE_URL empty for zero-setup SQLite, or set your Railway Postgres connection string:
# DATABASE_URL=postgresql://postgres:password@host:port/railway
```

### 3. Run Migrations & Seed Sample Test Accounts
```bash
npm run db:migrate
npm run db:seed
```

### 4. Start Development Server
```bash
npm run dev
# or build & start:
npm run build && npm start
```

### 5. Run Automated Integration Tests
```bash
npm test
```

---

## 🔑 Pre-Seeded Test Credentials

| Role | Email | Password | Details |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@speedo.com` | `Admin@123` | Platform Super Admin (KYC review, live fleet map) |
| **Captain (Approved)** | `captain@speedo.com` | `Captain@123` | Approved KYC, Online in Bangalore |
| **Captain (Pending)** | `pending_captain@speedo.com` | `Captain@123` | Submitted 4 KYC docs, awaiting review |
| **Rider** | `rider@speedo.com` | `Rider@123` | Customer with ride history |

---

## 🚂 Railway Cloud Deployment

1. Create a new project on [Railway](https://railway.app).
2. Provision a **PostgreSQL** database on Railway.
3. Link this repository's `/backend` directory.
4. Set environment variables on Railway:
   - `DATABASE_URL`: `${{Postgres.DATABASE_URL}}`
   - `JWT_SECRET`: `your_secure_random_key`
   - `NODE_ENV`: `production`
5. Railway will automatically run `Procfile` / `railway.json` (`npm run db:migrate && npm run db:seed && npm start`).
