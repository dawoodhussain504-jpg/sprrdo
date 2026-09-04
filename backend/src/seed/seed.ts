import bcrypt from 'bcryptjs';
import { getDb } from '../config/db';
import { runMigrations } from '../database/migrate';

export async function seedDatabase() {
  console.log('🌱 Starting database seeding with sample test accounts...');
  await runMigrations();

  const db = getDb();
  const passwordHash = await bcrypt.hash('Speedo@123', 10);
  const adminPasswordHash = await bcrypt.hash('Admin@123', 10);
  const captainPasswordHash = await bcrypt.hash('Captain@123', 10);
  const riderPasswordHash = await bcrypt.hash('Rider@123', 10);

  // Clear existing seed data to prevent duplicates
  await db.query('DELETE FROM users WHERE id = $1', ['rider_sample_001']);
  await db.query('DELETE FROM captains WHERE id IN ($1, $2, $3)', ['capt_approved_001', 'capt_pending_002', 'capt_approved_003']);
  await db.query('DELETE FROM admins WHERE id = $1', ['admin_001']);
  await db.query('DELETE FROM locations WHERE captain_id IN ($1, $2)', ['capt_approved_001', 'capt_approved_003']);

  // 1. Seed Admin
  await db.query(
    `INSERT INTO admins (id, name, email, password_hash, role)
     VALUES ($1, $2, $3, $4, 'admin')`,
    ['admin_001', 'Speedo Super Admin', 'admin@speedo.com', adminPasswordHash]
  );

  // 2. Seed Approved Captain 1 - Speedo Moto (Online in Bangalore Indiranagar area)
  const capt1Id = 'capt_approved_001';
  await db.query(
    `INSERT INTO captains (id, name, email, password_hash, phone, vehicle_type, vehicle_number, kyc_status, is_online, rating, total_rides, total_earnings, payment_qr_url)
     VALUES ($1, $2, $3, $4, $5, 'bike', 'KA-01-EQ-9876', 'approved', 1, 4.8, 142, 8520.0, 'http://localhost:5000/uploads/sample_qr.png')`,
    [capt1Id, 'Rajesh Kumar (Speedo Moto)', 'captain@speedo.com', captainPasswordHash, '+919876543210']
  );

  // Set Captain 1 Live GPS location
  await db.query(
    `INSERT INTO locations (captain_id, lat, lng, bearing, speed, is_online)
     VALUES ($1, 12.9716, 77.5946, 45.0, 18.5, 1)`,
    [capt1Id]
  );

  // 3. Seed Approved Captain 2 - Speedo 4 (Online in Bangalore Koramangala area)
  const capt3Id = 'capt_approved_003';
  await db.query(
    `INSERT INTO captains (id, name, email, password_hash, phone, vehicle_type, vehicle_number, kyc_status, is_online, rating, total_rides, total_earnings, payment_qr_url)
     VALUES ($1, $2, $3, $4, $5, 'cab', 'KA-05-CA-1234', 'approved', 1, 4.9, 88, 12400.0, 'http://localhost:5000/uploads/sample_qr.png')`,
    [capt3Id, 'Vikram Singh (Speedo 4)', 'cab_captain@speedo.com', captainPasswordHash, '+919877665544']
  );

  // Set Captain 3 Live GPS location
  await db.query(
    `INSERT INTO locations (captain_id, lat, lng, bearing, speed, is_online)
     VALUES ($1, 12.9352, 77.6245, 120.0, 22.0, 1)`,
    [capt3Id]
  );

  // 4. Seed Pending KYC Captain - Speedo Toto (Waiting for Admin review)
  const capt2Id = 'capt_pending_002';
  await db.query(
    `INSERT INTO captains (id, name, email, password_hash, phone, vehicle_type, vehicle_number, kyc_status, is_online, rating, total_rides, total_earnings)
     VALUES ($1, $2, $3, $4, $5, 'auto', 'KA-03-MB-4321', 'under_review', 0, 5.0, 0, 0.0)`,
    [capt2Id, 'Anil Sharma (Speedo Toto)', 'pending_captain@speedo.com', captainPasswordHash, '+919811223344']
  );

  // Seed 4 KYC documents for Pending Captain
  const docTypes = [
    { type: 'vehicle_reg', name: 'RC Card Copy' },
    { type: 'aadhaar', name: 'Aadhaar Card' },
    { type: 'selfie', name: 'Live Driver Selfie' },
    { type: 'payment_qr', name: 'UPI Payment QR' },
  ];

  await db.query('DELETE FROM kyc_documents WHERE captain_id = $1', [capt2Id]);

  for (let i = 0; i < docTypes.length; i++) {
    const docId = `kyc_sample_00${i + 1}`;
    await db.query(
      `INSERT INTO kyc_documents (id, captain_id, document_type, file_url, status)
       VALUES ($1, $2, $3, $4, 'pending')`,
      [docId, capt2Id, docTypes[i].type, `https://picsum.photos/seed/kyc_${docTypes[i].type}/600/400`]
    );
  }

  // 4. Seed Rider
  const riderId = 'rider_sample_001';
  await db.query(
    `INSERT INTO users (id, name, email, password_hash, phone)
     VALUES ($1, $2, $3, $4, $5)`,
    [riderId, 'Sneha Patel (Rider)', 'rider@speedo.com', riderPasswordHash, '+919988776655']
  );

  // 5. Seed sample completed ride for history
  const ridePastId = 'ride_completed_001';
  await db.query('DELETE FROM rides WHERE id = $1', [ridePastId]);
  await db.query(
    `INSERT INTO rides (
      id, rider_id, captain_id, pickup_address, pickup_lat, pickup_lng, drop_address, drop_lat, drop_lng,
      vehicle_type, fare, distance_km, status, otp, payment_status, created_at
    ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 'completed', '4589', 'paid', CURRENT_TIMESTAMP)`,
    [
      ridePastId,
      riderId,
      capt1Id,
      'Indiranagar 100ft Road, Bangalore',
      12.9716,
      77.5946,
      'Koramangala 5th Block, Bangalore',
      12.9352,
      77.6245,
      'bike',
      68.0,
      4.2,
    ]
  );

  // 6. Seed notifications
  await db.query('DELETE FROM notifications WHERE id IN ($1, $2, $3)', ['notif_rider_welcome', 'notif_capt_welcome', 'notif_admin_welcome']);

  await db.query(
    `INSERT INTO notifications (id, recipient_id, recipient_role, title, message, type, is_read)
     VALUES ($1, $2, 'rider', 'Welcome to Speedo! 🚀', 'Book your first ride on Speedo and experience fast, affordable commutes.', 'general', 0)`,
    ['notif_rider_welcome', riderId]
  );

  await db.query(
    `INSERT INTO notifications (id, recipient_id, recipient_role, title, message, type, is_read)
     VALUES ($1, $2, 'captain', 'KYC Verified 🎉', 'Your KYC documents have been approved. You are ready to go online.', 'kyc_update', 0)`,
    ['notif_capt_welcome', capt1Id]
  );

  await db.query(
    `INSERT INTO notifications (id, recipient_id, recipient_role, title, message, type, is_read)
     VALUES ($1, 'admin_001', 'admin', 'Pending KYC Verification 📋', 'Anil Sharma has submitted documents for verification.', 'kyc_submitted', 0)`,
    ['notif_admin_welcome']
  );

  // 7. Seed Sample Surge Zones
  await db.query('DELETE FROM geofence_surge_zones WHERE id IN ($1, $2)', ['zone_airport_blr', 'zone_tech_park']);
  await db.query(
    `INSERT INTO geofence_surge_zones (id, name, zone_type, center_lat, center_lng, radius_km, surge_multiplier, base_fare_multiplier, per_km_multiplier, is_active)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 1)`,
    ['zone_airport_blr', "Kempegowda Int'l Airport Surge Zone", 'airport', 13.1986, 77.7066, 5.0, 1.8, 1.5, 1.5]
  );
  await db.query(
    `INSERT INTO geofence_surge_zones (id, name, zone_type, center_lat, center_lng, radius_km, surge_multiplier, base_fare_multiplier, per_km_multiplier, is_active)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 1)`,
    ['zone_tech_park', 'Manyata Tech Park Evening Peak', 'tech_park', 13.0483, 77.6212, 3.5, 1.4, 1.25, 1.25]
  );

  // 8. Seed Sample SOS Emergency Alert
  await db.query('DELETE FROM sos_alerts WHERE id = $1', ['sos_sample_001']);
  await db.query(
    `INSERT INTO sos_alerts (id, ride_id, triggered_by, user_id, user_name, user_phone, captain_id, captain_name, captain_phone, vehicle_number, lat, lng, address, status, admin_notes)
     VALUES ($1, $2, 'rider', $3, 'Sneha Patel', '+919988776655', $4, 'Rajesh Kumar', '+919876543210', 'KA-01-EQ-9876', 12.9716, 77.5946, 'MG Road Metro Station, Bangalore', 'active', 'Live monitoring active. Police line 112 on standby.')`,
    ['sos_sample_001', 'ride_completed_001', riderId, capt1Id]
  );

  // 9. Seed Sample Broadcast Announcement
  await db.query('DELETE FROM broadcast_announcements WHERE id = $1', ['bcast_sample_001']);
  await db.query(
    `INSERT INTO broadcast_announcements (id, title, message, target_audience, target_city, coupon_code, discount_percent, bonus_amount, total_recipients)
     VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
    ['bcast_sample_001', 'Weekend Rush Discount! 🌧️', 'Heavy rain & rush hours! Get flat 30% OFF on all Speedo Moto and Speedo Toto rides today.', 'all', 'Bangalore', 'RAIN30', 30.0, 50.0, 1420]
  );

  // 10. Seed Default App Version Configurations
  const versionRows = await db.query('SELECT COUNT(*) as count FROM app_version_configs');
  const count = parseInt(versionRows.rows[0]?.count || '0', 10);
  if (count === 0) {
    await db.query(
      `INSERT INTO app_version_configs 
       (app_id, app_name, latest_version_code, latest_version_name, min_supported_version_code, force_update, title, message, release_notes, update_url, is_active)
       VALUES 
       ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 1),
       ($11, $12, $13, $14, $15, $16, $17, $18, $19, $20, 1),
       ($21, $22, $23, $24, $25, $26, $27, $28, $29, $30, 1)`,
      [
        'rider', 'Speedo Rider App', 1, '1.0.0', 1, 0, 'New Speedo Rider Update Available 🚀', 'Upgrade to enjoy live landmark suggestions, editable pickup search, and seamless 24h account deletion.', '• Editable pickup search\n• Popular destination thumbnails\n• Profile deletion request flow', 'https://play.google.com/store/apps/details?id=com.speedo.rider',
        'captain', 'Speedo Captain App', 1, '1.0.0', 1, 0, 'New Speedo Captain Update Available 🚀', 'Upgrade to enjoy live demand hotspots, moving vector markers on map, and captain profile deletion.', '• Live demand hotspots\n• Moving vehicle vector map markers\n• Captain profile deletion support', 'https://play.google.com/store/apps/details?id=com.speedo.captain',
        'admin', 'Speedo Admin Control', 1, '1.0.0', 1, 0, 'New Speedo Admin Update Available 🚀', 'Upgrade for complete fleet monitoring, app version OTA controls, and account deletion review.', '• App version OTA controls\n• Account deletion approvals\n• Popular destination management', 'https://play.google.com/store/apps/details?id=com.speedo.admin'
      ]
    );
  }

  console.log('✅ Database seeded successfully with the following test credentials:');
  console.log('-----------------------------------------------------------');
  console.log('👤 ADMIN:    admin@speedo.com           / Admin@123');
  console.log('🛵 CAPTAIN:  captain@speedo.com         / Captain@123 (Approved KYC, Online)');
  console.log('🛵 CAPTAIN:  pending_captain@speedo.com / Captain@123 (Under Review KYC)');
  console.log('🧑 RIDER:    rider@speedo.com           / Rider@123');
  console.log('-----------------------------------------------------------');
}

if (require.main === module) {
  seedDatabase()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('Seeding failed:', err);
      process.exit(1);
    });
}
