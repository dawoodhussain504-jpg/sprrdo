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

CREATE TABLE IF NOT EXISTS support_tickets (
  id VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  user_role VARCHAR(32) NOT NULL, -- 'rider', 'captain'
  user_name VARCHAR(255),
  user_phone VARCHAR(32),
  ride_id VARCHAR(64),
  subject VARCHAR(255) NOT NULL,
  category VARCHAR(64) DEFAULT 'general', -- 'payment_fare', 'ride_issue', 'safety', 'account_kyc', 'app_feedback', 'general'
  status VARCHAR(32) DEFAULT 'open', -- 'open', 'in_progress', 'resolved', 'closed'
  priority VARCHAR(32) DEFAULT 'normal', -- 'normal', 'high', 'urgent'
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS support_messages (
  id VARCHAR(64) PRIMARY KEY,
  ticket_id VARCHAR(64) NOT NULL,
  sender_id VARCHAR(64) NOT NULL,
  sender_role VARCHAR(32) NOT NULL, -- 'rider', 'captain', 'admin', 'speedo_support'
  sender_name VARCHAR(255) NOT NULL,
  message_text TEXT NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS geofence_surge_zones (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  zone_type VARCHAR(64) DEFAULT 'custom', -- 'airport', 'tech_park', 'railway_station', 'city_center', 'custom'
  center_lat REAL NOT NULL,
  center_lng REAL NOT NULL,
  radius_km REAL DEFAULT 3.0,
  polygon_coords_json TEXT,
  base_fare_multiplier REAL DEFAULT 1.25,
  per_km_multiplier REAL DEFAULT 1.25,
  surge_multiplier REAL DEFAULT 1.3,
  is_active INTEGER DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sos_alerts (
  id VARCHAR(64) PRIMARY KEY,
  ride_id VARCHAR(64),
  triggered_by VARCHAR(32) NOT NULL, -- 'rider', 'captain'
  user_id VARCHAR(64) NOT NULL,
  user_name VARCHAR(255) NOT NULL,
  user_phone VARCHAR(32) NOT NULL,
  captain_id VARCHAR(64),
  captain_name VARCHAR(255),
  captain_phone VARCHAR(32),
  vehicle_number VARCHAR(64),
  lat REAL NOT NULL,
  lng REAL NOT NULL,
  address TEXT,
  status VARCHAR(32) DEFAULT 'active', -- 'active', 'in_progress', 'resolved', 'false_alarm'
  admin_notes TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS broadcast_announcements (
  id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  target_audience VARCHAR(64) DEFAULT 'all', -- 'all', 'riders', 'captains', 'city_zone'
  target_city VARCHAR(128) DEFAULT 'All Cities',
  coupon_code VARCHAR(64),
  discount_percent REAL DEFAULT 0.0,
  bonus_amount REAL DEFAULT 0.0,
  sent_by VARCHAR(64) DEFAULT 'admin',
  total_recipients INTEGER DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

