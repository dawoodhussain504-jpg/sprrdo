import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { calculateDistanceKm } from '../services/distance';
import { createNotification } from '../services/notification';
import { emitRideEvent } from '../services/socket';

export async function getCaptainProfile(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const captRes = await db.query(
      `SELECT id, name, email, phone, vehicle_type, vehicle_number, kyc_status, admin_remarks,
              is_online, rating, total_rides, total_earnings, avatar_url, payment_qr_url, created_at
       FROM captains WHERE id = $1`,
      [captainId]
    );

    if (captRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Captain not found' });
    }

    const captain = captRes.rows[0];
    return res.json({
      success: true,
      data: {
        ...captain,
        is_online: Boolean(captain.is_online),
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch captain profile', error: error.message });
  }
}

export async function toggleOnlineStatus(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const { is_online } = req.body;

    const captRes = await db.query('SELECT kyc_status, is_active FROM captains WHERE id = $1', [captainId]);
    if (captRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Captain not found' });
    }

    const captain = captRes.rows[0];
    if (captain.is_active === 0) {
      return res.status(403).json({ success: false, message: 'Your account is suspended. Contact admin.' });
    }

    if (is_online && captain.kyc_status !== 'approved') {
      return res.status(403).json({
        success: false,
        message: `Cannot go online. Your KYC status is '${captain.kyc_status}'. Only approved captains can take rides.`,
        kyc_status: captain.kyc_status,
      });
    }

    const onlineVal = is_online ? 1 : 0;
    await db.query('UPDATE captains SET is_online = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2', [onlineVal, captainId]);
    await db.query('UPDATE locations SET is_online = $1, updated_at = CURRENT_TIMESTAMP WHERE captain_id = $2', [onlineVal, captainId]);

    return res.json({
      success: true,
      message: `Captain is now ${is_online ? 'ONLINE' : 'OFFLINE'}`,
      data: { is_online: Boolean(onlineVal) },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update online status', error: error.message });
  }
}

export async function updateLocation(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const { lat, lng, bearing = 0.0, speed = 0.0 } = req.body;

    if (lat === undefined || lng === undefined) {
      return res.status(400).json({ success: false, message: 'lat and lng are required' });
    }

    // Upsert location
    const existing = await db.query('SELECT captain_id FROM locations WHERE captain_id = $1', [captainId]);
    if (existing.rows.length > 0) {
      await db.query(
        `UPDATE locations SET lat = $1, lng = $2, bearing = $3, speed = $4, updated_at = CURRENT_TIMESTAMP WHERE captain_id = $5`,
        [lat, lng, bearing, speed, captainId]
      );
    } else {
      await db.query(
        `INSERT INTO locations (captain_id, lat, lng, bearing, speed, is_online, updated_at)
         VALUES ($1, $2, $3, $4, $5, 1, CURRENT_TIMESTAMP)`,
        [captainId, lat, lng, bearing, speed]
      );
    }

    // Update active ride's captain lat/lng for real-time rider tracking
    await db.query(
      `UPDATE rides SET captain_lat = $1, captain_lng = $2, captain_heading = $3, updated_at = CURRENT_TIMESTAMP
       WHERE captain_id = $4 AND status IN ('accepted', 'arrived', 'ongoing')`,
      [lat, lng, bearing, captainId]
    );

    return res.json({ success: true, message: 'Location updated successfully' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update location', error: error.message });
  }
}

export async function getIncomingRideRequests(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;

    // Check if captain already has an active ongoing ride
    const activeRes = await db.query(
      `SELECT id FROM rides WHERE captain_id = $1 AND status IN ('accepted', 'arrived', 'ongoing')`,
      [captainId]
    );
    if (activeRes.rows.length > 0) {
      return res.json({ success: true, data: [], message: 'Captain is currently on an active ride' });
    }

    // Fetch captain onboarded vehicle type (Auto, Bike, Cab)
    const captRes = await db.query('SELECT vehicle_type FROM captains WHERE id = $1', [captainId]);
    const vehicleType = captRes.rows[0]?.vehicle_type?.toLowerCase() || 'bike';

    // Fetch captain live location
    const locRes = await db.query('SELECT lat, lng FROM locations WHERE captain_id = $1', [captainId]);
    const captLat = locRes.rows[0]?.lat;
    const captLng = locRes.rows[0]?.lng;

    // Find requested rides matching this Captain's onboarded vehicle type
    const ridesRes = await db.query(
      `SELECT r.*, u.name as rider_name, u.phone as rider_phone
       FROM rides r
       JOIN users u ON r.rider_id = u.id
       WHERE r.status = 'requested' AND LOWER(r.vehicle_type) = $1
       ORDER BY r.created_at DESC LIMIT 10`,
      [vehicleType]
    );

    const formattedRequests = ridesRes.rows.map((ride) => {
      let pickupDistanceKm = 0;
      if (captLat !== undefined && captLng !== undefined) {
        pickupDistanceKm = calculateDistanceKm(captLat, captLng, ride.pickup_lat, ride.pickup_lng);
      }
      return {
        ...ride,
        distance_to_pickup_km: pickupDistanceKm,
        pickup_eta_min: Math.max(2, Math.round((pickupDistanceKm / 25) * 60)),
      };
    });

    return res.json({
      success: true,
      data: formattedRequests,
      count: formattedRequests.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch incoming requests', error: error.message });
  }
}

export async function acceptRide(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const rideId = req.params.id;

    // Verify captain has no active ride
    const activeRes = await db.query(
      `SELECT id FROM rides WHERE captain_id = $1 AND status IN ('accepted', 'arrived', 'ongoing')`,
      [captainId]
    );
    if (activeRes.rows.length > 0) {
      return res.status(400).json({ success: false, message: 'You already have an active ride in progress' });
    }

    // Atomic update of ride if still in 'requested' state
    const updateRes = await db.query(
      `UPDATE rides SET captain_id = $1, status = 'accepted', updated_at = CURRENT_TIMESTAMP
       WHERE id = $2 AND status = 'requested'`,
      [captainId, rideId]
    );

    if (updateRes.rowCount === 0) {
      return res.status(400).json({ success: false, message: 'Ride request was cancelled or accepted by another captain' });
    }

    const rideRes = await db.query(
      `SELECT r.*, u.name as rider_name, u.phone as rider_phone
       FROM rides r
       JOIN users u ON r.rider_id = u.id
       WHERE r.id = $1`,
      [rideId]
    );
    const ride = rideRes.rows[0];

    // Get captain details
    const captRes = await db.query('SELECT name, phone, vehicle_number, vehicle_type FROM captains WHERE id = $1', [captainId]);
    const capt = captRes.rows[0];

    // Notify rider that captain has accepted
    await createNotification({
      recipientId: ride.rider_id,
      recipientRole: 'rider',
      title: 'Captain on the way!',
      message: `${capt.name} (${capt.vehicle_number}) has accepted your ride request.`,
      type: 'ride_accepted',
      metadata: { rideId, captainName: capt.name, vehicleNumber: capt.vehicle_number },
    });

    // Real-time WebSocket broadcast to Rider and Admin
    emitRideEvent(rideId, 'ride:status_update', {
      rideId,
      status: 'accepted',
      captain: {
        id: capt.id,
        name: capt.name,
        phone: capt.phone,
        vehicleNumber: capt.vehicle_number,
        vehicleType: capt.vehicle_type,
      },
    });

    return res.json({
      success: true,
      message: 'Ride accepted successfully',
      data: ride,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to accept ride', error: error.message });
  }
}

export async function getCaptainActiveRide(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const rideRes = await db.query(
      `SELECT r.*, u.name as rider_name, u.phone as rider_phone, u.avatar_url as rider_avatar_url
       FROM rides r
       JOIN users u ON r.rider_id = u.id
       WHERE r.captain_id = $1 AND r.status IN ('accepted', 'arrived', 'ongoing')
       ORDER BY r.created_at DESC LIMIT 1`,
      [captainId]
    );

    if (rideRes.rows.length === 0) {
      return res.json({ success: true, data: null, message: 'No active ride' });
    }

    return res.json({ success: true, data: rideRes.rows[0] });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch active ride', error: error.message });
  }
}

export async function updateRideStatus(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const rideId = req.params.id;
    const { status, otp } = req.body; // 'arrived', 'ongoing', 'completed'

    const rideRes = await db.query('SELECT * FROM rides WHERE id = $1 AND captain_id = $2', [rideId, captainId]);
    if (rideRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Ride not found or not assigned to you' });
    }

    const ride = rideRes.rows[0];

    if (status === 'arrived') {
      if (ride.status !== 'accepted') {
        return res.status(400).json({ success: false, message: `Cannot mark arrived from status '${ride.status}'` });
      }

      await db.query(`UPDATE rides SET status = 'arrived', updated_at = CURRENT_TIMESTAMP WHERE id = $1`, [rideId]);

      await createNotification({
        recipientId: ride.rider_id,
        recipientRole: 'rider',
        title: 'Captain has arrived!',
        message: 'Your captain is waiting at the pickup location. Share your OTP to begin.',
        type: 'captain_arrived',
        metadata: { rideId },
      });

      emitRideEvent(rideId, 'ride:status_update', { rideId, status: 'arrived' });

      return res.json({ success: true, message: 'Status updated to Arrived', data: { status: 'arrived' } });
    }

    if (status === 'ongoing') {
      if (!['accepted', 'arrived'].includes(ride.status)) {
        return res.status(400).json({ success: false, message: `Cannot start ride from status '${ride.status}'` });
      }

      if (!otp || String(otp).trim() !== String(ride.otp).trim()) {
        return res.status(400).json({ success: false, message: 'Invalid OTP. Please ask the rider for the 4-digit ride OTP.' });
      }

      await db.query(`UPDATE rides SET status = 'ongoing', updated_at = CURRENT_TIMESTAMP WHERE id = $1`, [rideId]);

      await createNotification({
        recipientId: ride.rider_id,
        recipientRole: 'rider',
        title: 'Ride Started',
        message: 'Have a safe trip with Speedo!',
        type: 'ride_started',
        metadata: { rideId },
      });

      emitRideEvent(rideId, 'ride:status_update', { rideId, status: 'ongoing' });

      return res.json({ success: true, message: 'OTP verified. Ride started!', data: { status: 'ongoing' } });
    }

    if (status === 'completed') {
      if (ride.status !== 'ongoing') {
        return res.status(400).json({ success: false, message: `Cannot complete ride that is not ongoing (current: ${ride.status})` });
      }

      await db.query(`UPDATE rides SET status = 'completed', payment_status = 'completed', updated_at = CURRENT_TIMESTAMP WHERE id = $1`, [rideId]);

      // Update captain stats and earnings
      await db.query(
        `UPDATE captains SET total_rides = total_rides + 1, total_earnings = total_earnings + $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`,
        [ride.fare, captainId]
      );

      // Create payment record
      const paymentId = 'pay_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
      await db.query(
        `INSERT INTO payments (id, ride_id, rider_id, captain_id, amount, status, payment_method, created_at)
         VALUES ($1, $2, $3, $4, $5, 'completed', 'cash_or_qr', CURRENT_TIMESTAMP)`,
        [paymentId, rideId, ride.rider_id, captainId, ride.fare]
      );

      await createNotification({
        recipientId: ride.rider_id,
        recipientRole: 'rider',
        title: 'Ride Completed!',
        message: `Your ride is completed. Fare: ₹${ride.fare}. Please pay your captain via cash or scan their QR.`,
        type: 'ride_completed',
        metadata: { rideId, fare: ride.fare },
      });

      emitRideEvent(rideId, 'ride:status_update', { rideId, status: 'completed', fare: ride.fare });

      return res.json({
        success: true,
        message: 'Ride completed successfully',
        data: { status: 'completed', fare: ride.fare },
      });
    }

    return res.status(400).json({ success: false, message: `Invalid target status '${status}'` });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update ride status', error: error.message });
  }
}

export async function getCaptainRideHistory(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const rides = await db.query(
      `SELECT r.*, u.name as rider_name, u.phone as rider_phone
       FROM rides r
       JOIN users u ON r.rider_id = u.id
       WHERE r.captain_id = $1
       ORDER BY r.created_at DESC`,
      [captainId]
    );
    return res.json({ success: true, data: rides.rows });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch ride history', error: error.message });
  }
}

function isOldUpdateNotif(n: any, currentVersion: number | null): boolean {
  if (currentVersion === null || isNaN(currentVersion)) return false;
  const isUpdate = n.type === 'app_update' || n.title?.toLowerCase().includes('update') || n.message?.toLowerCase().includes('update');
  if (!isUpdate) return false;

  if (n.metadata_json) {
    try {
      const meta = typeof n.metadata_json === 'string' ? JSON.parse(n.metadata_json) : n.metadata_json;
      const targetCode = meta.latestVersionCode || meta.versionCode;
      if (targetCode && Number(targetCode) <= currentVersion) {
        return true;
      }
    } catch (e) {}
  }
  const match = ((n.title || '') + ' ' + (n.message || '')).match(/(?:build|code|#)\s*(\d+)/i);
  if (match) {
    const code = parseInt(match[1], 10);
    if (!isNaN(code) && code <= currentVersion) {
      return true;
    }
  }
  return false;
}

export async function getCaptainNotifications(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const currentVersion = req.query.currentVersion ? parseInt(req.query.currentVersion as string, 10) : null;
    const notifs = await db.query(
      `SELECT * FROM notifications WHERE (recipient_id = $1 OR recipient_id = 'all') AND (recipient_role = 'captain' OR recipient_role = 'all') AND is_read = 0 ORDER BY created_at DESC LIMIT 50`,
      [captainId]
    );
    let data = notifs.rows;
    if (currentVersion !== null && !isNaN(currentVersion)) {
      data = data.filter(n => !isOldUpdateNotif(n, currentVersion));
    }
    return res.json({ success: true, data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch notifications', error: error.message });
  }
}

export async function markCaptainNotificationRead(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const notifId = req.params.id;
    await db.query(`UPDATE notifications SET is_read = 1 WHERE id = $1 AND (recipient_id = $2 OR recipient_id = 'all')`, [notifId, captainId]);
    return res.json({ success: true, message: 'Notification marked as read' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update notification', error: error.message });
  }
}

export async function getCaptainUnreadCount(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.user?.id;
    const currentVersion = req.query.currentVersion ? parseInt(req.query.currentVersion as string, 10) : null;
    const countRes = await db.query(`SELECT * FROM notifications WHERE (recipient_id = $1 OR recipient_id = 'all') AND (recipient_role = 'captain' OR recipient_role = 'all') AND is_read = 0`, [captainId]);
    let rows = countRes.rows;
    if (currentVersion !== null && !isNaN(currentVersion)) {
      rows = rows.filter(n => !isOldUpdateNotif(n, currentVersion));
    }
    return res.json({ success: true, count: rows.length });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to get unread count', error: error.message });
  }
}
