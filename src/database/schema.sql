-- Speedo Centralized Database Schema

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  avatar_url TEXT,
  is_active INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS captains (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  vehicle_type VARCHAR(32) NOT NULL, -- 'bike', 'auto', 'cab'
  vehicle_number VARCHAR(64) NOT NULL,
  kyc_status VARCHAR(32) DEFAULT 'pending', -- 'pending', 'under_review', 'approved', 'rejected'
  admin_remarks TEXT,
  is_online INTEGER DEFAULT 0,
  rating REAL DEFAULT 5.0,
  total_rides INTEGER DEFAULT 0,
  total_earnings REAL DEFAULT 0.0,
  avatar_url TEXT,
  payment_qr_url TEXT,
  is_active INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS admins (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(32) DEFAULT 'admin',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kyc_documents (
  id VARCHAR(64) PRIMARY KEY,
  captain_id VARCHAR(64) NOT NULL,
  document_type VARCHAR(64) NOT NULL, -- 'vehicle_reg', 'aadhaar', 'selfie', 'payment_qr'
  file_url TEXT NOT NULL,
  status VARCHAR(32) DEFAULT 'pending', -- 'pending', 'approved', 'rejected'
  admin_remarks TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (captain_id) REFERENCES captains(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS rides (
  id VARCHAR(64) PRIMARY KEY,
  rider_id VARCHAR(64) NOT NULL,
  captain_id VARCHAR(64),
  pickup_address TEXT NOT NULL,
  pickup_lat REAL NOT NULL,
  pickup_lng REAL NOT NULL,
  drop_address TEXT NOT NULL,
  drop_lat REAL NOT NULL,
  drop_lng REAL NOT NULL,
  vehicle_type VARCHAR(32) NOT NULL, -- 'bike', 'auto', 'cab'
  fare REAL NOT NULL,
  distance_km REAL NOT NULL,
  status VARCHAR(32) DEFAULT 'requested', -- 'requested', 'accepted', 'arrived', 'ongoing', 'completed', 'cancelled'
  otp VARCHAR(8) NOT NULL,
  captain_lat REAL,
  captain_lng REAL,
  captain_heading REAL DEFAULT 0,
  payment_status VARCHAR(32) DEFAULT 'pending', -- 'pending', 'paid'
  payment_method VARCHAR(32) DEFAULT 'cash_or_qr',
  cancelled_by VARCHAR(32),
  cancellation_reason TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (rider_id) REFERENCES users(id),
  FOREIGN KEY (captain_id) REFERENCES captains(id)
);

CREATE TABLE IF NOT EXISTS locations (
  captain_id VARCHAR(64) PRIMARY KEY,
  lat REAL NOT NULL,
  lng REAL NOT NULL,
  bearing REAL DEFAULT 0.0,
  speed REAL DEFAULT 0.0,
  is_online INTEGER DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (captain_id) REFERENCES captains(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
  id VARCHAR(64) PRIMARY KEY,
  recipient_id VARCHAR(64) NOT NULL,
  recipient_role VARCHAR(32) NOT NULL, -- 'rider', 'captain', 'admin'
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  type VARCHAR(64) NOT NULL, -- 'ride_request', 'ride_accepted', 'captain_arrived', 'ride_completed', 'kyc_update', 'kyc_submitted', 'general'
  is_read INTEGER DEFAULT 0,
  metadata_json TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments (
  id VARCHAR(64) PRIMARY KEY,
  ride_id VARCHAR(64) NOT NULL,
  rider_id VARCHAR(64) NOT NULL,
  captain_id VARCHAR(64) NOT NULL,
  amount REAL NOT NULL,
  status VARCHAR(32) DEFAULT 'completed',
  payment_method VARCHAR(32) DEFAULT 'qr_code',
  qr_image_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ride_id) REFERENCES rides(id),
  FOREIGN KEY (rider_id) REFERENCES users(id),
  FOREIGN KEY (captain_id) REFERENCES captains(id)
);

CREATE TABLE IF NOT EXISTS messages (
  id VARCHAR(64) PRIMARY KEY,
  ride_id VARCHAR(64) NOT NULL,
  sender_id VARCHAR(64) NOT NULL,
  sender_role VARCHAR(32) NOT NULL, -- 'rider', 'captain'
  recipient_id VARCHAR(64) NOT NULL,
  recipient_role VARCHAR(32) NOT NULL, -- 'rider', 'captain'
  message_text TEXT NOT NULL,
  message_type VARCHAR(32) DEFAULT 'text', -- 'text', 'quick_chip', 'voice_note'
  audio_url TEXT,
  is_read INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE
);
