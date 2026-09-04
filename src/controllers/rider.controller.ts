import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { calculateDistanceKm, calculateFares, generateRideOtp, VehicleType, normalizeVehicleType } from '../services/distance';
import { createNotification } from '../services/notification';
import { emitRideEvent, emitToCaptains } from '../services/socket';

export async function getRiderProfile(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const userRes = await db.query('SELECT id, name, email, phone, avatar_url, created_at FROM users WHERE id = $1', [riderId]);
    if (userRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Rider not found' });
    }
    return res.json({ success: true, data: userRes.rows[0] });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to get profile', error: error.message });
  }
}

export async function getNearbyCaptains(req: AuthenticatedRequest, res: Response) {
  try {
    const lat = parseFloat(req.query.lat as string);
    const lng = parseFloat(req.query.lng as string);
    const radiusKm = parseFloat((req.query.radius as string) || '5.0');
    const vehicleTypeRaw = req.query.vehicle_type as string;
    const vehicleType = vehicleTypeRaw ? normalizeVehicleType(vehicleTypeRaw) : undefined;

    if (isNaN(lat) || isNaN(lng)) {
      return res.status(400).json({ success: false, message: 'Valid lat and lng query params are required' });
    }

    let sql = `
      SELECT c.id, c.name, c.phone, c.vehicle_type, c.vehicle_number, c.rating, c.avatar_url,
             l.lat, l.lng, l.bearing, l.speed, l.updated_at
      FROM captains c
      JOIN locations l ON c.id = l.captain_id
      WHERE c.is_online = 1 AND c.kyc_status = 'approved' AND c.is_active = 1
    `;
    const params: any[] = [];

    if (vehicleType) {
      params.push(vehicleType);
      sql += ` AND c.vehicle_type = $${params.length}`;
    }

    const result = await db.query(sql, params);

    // Calculate distance and filter within radius
    const nearbyCaptains = result.rows
      .map((capt) => {
        const distance = calculateDistanceKm(lat, lng, capt.lat, capt.lng);
        return {
          ...capt,
          distance_km: distance,
        };
      })
      .filter((capt) => capt.distance_km <= radiusKm)
      .sort((a, b) => a.distance_km - b.distance_km);

    return res.json({
      success: true,
      data: nearbyCaptains,
      count: nearbyCaptains.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch nearby captains', error: error.message });
  }
}

import { calculateRoadRoute } from '../services/routing';

export async function estimateFares(req: AuthenticatedRequest, res: Response) {
  try {
    const { pickup_lat, pickup_lng, drop_lat, drop_lng } = req.body;
    if (pickup_lat === undefined || pickup_lng === undefined || drop_lat === undefined || drop_lng === undefined) {
      return res.status(400).json({ success: false, message: 'pickup_lat, pickup_lng, drop_lat, drop_lng are required' });
    }

    const route = await calculateRoadRoute(pickup_lat, pickup_lng, drop_lat, drop_lng);
    const distanceKm = route.distanceKm;

    // Check if pickup or drop falls within any active geofenced surge zone
    let appliedSurgeMultiplier = 1.0;
    let surgeZoneName: string | null = null;

    try {
      const activeZones = await db.query('SELECT * FROM geofence_surge_zones WHERE is_active = 1');
      for (const zone of activeZones.rows) {
        const dPickup = calculateDistanceKm(pickup_lat, pickup_lng, zone.center_lat, zone.center_lng);
        const dDrop = calculateDistanceKm(drop_lat, drop_lng, zone.center_lat, zone.center_lng);
        if (dPickup <= (zone.radius_km || 3.0) || dDrop <= (zone.radius_km || 3.0)) {
          if (zone.surge_multiplier > appliedSurgeMultiplier) {
            appliedSurgeMultiplier = zone.surge_multiplier;
            surgeZoneName = zone.name;
          }
        }
      }
    } catch (e: any) {
      console.warn('Surge zone check fallback:', e.message);
    }

    const fares = calculateFares(distanceKm, appliedSurgeMultiplier);

    return res.json({
      success: true,
      data: {
        distance_km: distanceKm,
        duration_mins: route.durationMins,
        summary: route.summary,
        polyline: route.coordinates,
        estimates: fares,
        surge_applied: appliedSurgeMultiplier > 1.0,
        surge_multiplier: appliedSurgeMultiplier,
        surge_zone: surgeZoneName,
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to calculate fare estimate', error: error.message });
  }
}

export async function requestRide(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const { pickup_address, pickup_lat, pickup_lng, drop_address, drop_lat, drop_lng, vehicle_type } = req.body;

    if (!pickup_address || pickup_lat === undefined || pickup_lng === undefined || !drop_address || drop_lat === undefined || drop_lng === undefined || !vehicle_type) {
      return res.status(400).json({ success: false, message: 'All ride request fields are required' });
    }

    // Auto-cancel any previous unassigned requested rides for this rider to allow re-booking
    await db.query(
      `UPDATE rides SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP WHERE rider_id = $1 AND status = 'requested'`,
      [riderId]
    );

    // Only block if rider already has an assigned captain in progress
    const activeRideRes = await db.query(
      `SELECT id FROM rides WHERE rider_id = $1 AND status IN ('accepted', 'arrived', 'ongoing')`,
      [riderId]
    );
    if (activeRideRes.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'You already have an active ride in progress with a captain.',
        activeRideId: activeRideRes.rows[0].id,
      });
    }

    const normalizedVehicleType = normalizeVehicleType(vehicle_type);
    const route = await calculateRoadRoute(pickup_lat, pickup_lng, drop_lat, drop_lng);
    const distanceKm = route.distanceKm;
    const fares = calculateFares(distanceKm);
    const selectedEstimate = fares[normalizedVehicleType] || fares.bike;
    const otp = generateRideOtp();
    const rideId = 'ride_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);

    await db.query(
      `INSERT INTO rides (
        id, rider_id, pickup_address, pickup_lat, pickup_lng, drop_address, drop_lat, drop_lng,
        vehicle_type, fare, distance_km, status, otp, payment_status, created_at, updated_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, 'requested', $12, 'pending', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
      [
        rideId,
        riderId,
        pickup_address,
        pickup_lat,
        pickup_lng,
        drop_address,
        drop_lat,
        drop_lng,
        vehicle_type.toLowerCase(),
        selectedEstimate.totalFare,
        distanceKm,
        otp,
      ]
    );

    // Fetch the inserted ride with rider info
    const newRideRes = await db.query(
      `SELECT r.*, u.name as rider_name, u.phone as rider_phone
       FROM rides r
       JOIN users u ON r.rider_id = u.id
       WHERE r.id = $1`,
      [rideId]
    );
    const ride = newRideRes.rows[0];

    // Notify online nearby captains via WebSockets immediately (sub-second broadcast)
    emitToCaptains('ride:new_request', ride);

    // Notify all approved captains of this specific vehicle type (e.g. Auto -> Auto, Bike -> Bike, Cab -> Cab)
    const nearbyCaptains = await db.query(
      `SELECT c.id FROM captains c
       WHERE c.kyc_status = 'approved' AND LOWER(c.vehicle_type) = $1`,
      [vehicle_type.toLowerCase()]
    );

    for (const capt of nearbyCaptains.rows) {
      await createNotification({
        recipientId: capt.id,
        recipientRole: 'captain',
        title: `New ${vehicle_type.toUpperCase()} Ride Request!`,
        message: `New ${vehicle_type.toUpperCase()} ride: ${pickup_address} → ${drop_address} (₹${selectedEstimate.totalFare})`,
        type: 'ride_request',
        metadata: { rideId, fare: selectedEstimate.totalFare, pickup_address, drop_address, vehicleType: vehicle_type.toLowerCase() },
      });
    }

    return res.status(201).json({
      success: true,
      message: 'Ride requested successfully. Searching for nearby captains...',
      data: ride,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to request ride', error: error.message });
  }
}

export async function getActiveRide(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const rideRes = await db.query(
      `SELECT r.*, c.name as captain_name, c.phone as captain_phone, c.vehicle_number, c.vehicle_type as captain_vehicle_type,
              c.rating as captain_rating, c.avatar_url as captain_avatar_url, c.payment_qr_url as captain_qr_url,
              l.lat as live_captain_lat, l.lng as live_captain_lng, l.bearing as live_captain_bearing
       FROM rides r
       LEFT JOIN captains c ON r.captain_id = c.id
       LEFT JOIN locations l ON c.id = l.captain_id
       WHERE r.rider_id = $1 AND r.status IN ('requested', 'accepted', 'arrived', 'ongoing')
       ORDER BY r.created_at DESC LIMIT 1`,
      [riderId]
    );

    if (rideRes.rows.length === 0) {
      return res.json({ success: true, data: null, message: 'No active ride found' });
    }

    const ride = rideRes.rows[0];

    // Compute dynamic ETA if captain is assigned and active
    let dynamicEtaMin = null;
    let captainDistanceKm = null;

    if (ride.live_captain_lat && ride.live_captain_lng) {
      const targetLat = ride.status === 'ongoing' ? ride.drop_lat : ride.pickup_lat;
      const targetLng = ride.status === 'ongoing' ? ride.drop_lng : ride.pickup_lng;
      captainDistanceKm = calculateDistanceKm(ride.live_captain_lat, ride.live_captain_lng, targetLat, targetLng);
      // Assume avg 25 km/h
      dynamicEtaMin = Math.max(1, Math.round((captainDistanceKm / 25) * 60));
    }

    return res.json({
      success: true,
      data: {
        ...ride,
        captain_distance_km: captainDistanceKm,
        eta_minutes: dynamicEtaMin,
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch active ride', error: error.message });
  }
}

export async function cancelRide(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const rideId = req.params.id;
    const { reason } = req.body;

    const rideRes = await db.query('SELECT * FROM rides WHERE id = $1 AND rider_id = $2', [rideId, riderId]);
    if (rideRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Ride not found' });
    }

    const ride = rideRes.rows[0];
    if (['completed', 'cancelled'].includes(ride.status)) {
      return res.status(400).json({ success: false, message: `Ride is already ${ride.status}` });
    }

    if (ride.status === 'ongoing') {
      return res.status(400).json({ success: false, message: 'Cannot cancel an ongoing ride. Please contact captain/support.' });
    }

    await db.query(
      `UPDATE rides SET status = 'cancelled', cancelled_by = 'rider', cancellation_reason = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`,
      [reason || 'Cancelled by rider', rideId]
    );

    // Notify captain if one was accepted
    if (ride.captain_id) {
      await createNotification({
        recipientId: ride.captain_id,
        recipientRole: 'captain',
        title: 'Ride Cancelled',
        message: `Rider cancelled the ride (${ride.pickup_address} → ${ride.drop_address}).`,
        type: 'ride_cancelled',
        metadata: { rideId },
      });
    }

    // Broadcast real-time cancellation to room
    emitRideEvent(rideId, 'ride:status_update', { rideId, status: 'cancelled', cancelledBy: 'rider' });

    return res.json({ success: true, message: 'Ride cancelled successfully' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to cancel ride', error: error.message });
  }
}

export async function getRideHistory(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const rides = await db.query(
      `SELECT r.*, c.name as captain_name, c.phone as captain_phone, c.vehicle_number, c.rating as captain_rating
       FROM rides r
       LEFT JOIN captains c ON r.captain_id = c.id
       WHERE r.rider_id = $1
       ORDER BY r.created_at DESC`,
      [riderId]
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

export async function getRiderNotifications(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const currentVersion = req.query.currentVersion ? parseInt(req.query.currentVersion as string, 10) : null;
    const notifs = await db.query(
      `SELECT * FROM notifications WHERE (recipient_id = $1 OR recipient_id = 'all') AND (recipient_role = 'rider' OR recipient_role = 'all') AND is_read = 0 ORDER BY created_at DESC LIMIT 50`,
      [riderId]
    );
    let data = notifs.rows;
    if (currentVersion !== null && !isNaN(currentVersion)) {
      const verRes = await db.query(`SELECT latest_version_code FROM app_version_configs WHERE app_id = 'rider'`);
      const latestCode = verRes.rows[0]?.latest_version_code ? Number(verRes.rows[0].latest_version_code) : 1;
      const isUserUpdated = currentVersion >= latestCode;

      data = data.filter(n => {
        const isUpdate = n.type === 'app_update' || n.title?.toLowerCase().includes('update') || n.message?.toLowerCase().includes('update');
        if (!isUpdate) return true;
        // If user already updated to latest version, ALL update notifications disappear!
        if (isUserUpdated) return false;
        return !isOldUpdateNotif(n, currentVersion);
      });
    }
    return res.json({ success: true, data });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch notifications', error: error.message });
  }
}

export async function markNotificationRead(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const notifId = req.params.id;
    await db.query(`UPDATE notifications SET is_read = 1 WHERE id = $1 AND (recipient_id = $2 OR recipient_id = 'all')`, [notifId, riderId]);
    return res.json({ success: true, message: 'Notification marked as read' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update notification', error: error.message });
  }
}

export async function getRiderUnreadCount(req: AuthenticatedRequest, res: Response) {
  try {
    const riderId = req.user?.id;
    const currentVersion = req.query.currentVersion ? parseInt(req.query.currentVersion as string, 10) : null;
    const countRes = await db.query(`SELECT * FROM notifications WHERE (recipient_id = $1 OR recipient_id = 'all') AND (recipient_role = 'rider' OR recipient_role = 'all') AND is_read = 0`, [riderId]);
    let rows = countRes.rows;
    if (currentVersion !== null && !isNaN(currentVersion)) {
      const verRes = await db.query(`SELECT latest_version_code FROM app_version_configs WHERE app_id = 'rider'`);
      const latestCode = verRes.rows[0]?.latest_version_code ? Number(verRes.rows[0].latest_version_code) : 1;
      const isUserUpdated = currentVersion >= latestCode;

      rows = rows.filter(n => {
        const isUpdate = n.type === 'app_update' || n.title?.toLowerCase().includes('update') || n.message?.toLowerCase().includes('update');
        if (!isUpdate) return true;
        if (isUserUpdated) return false;
        return !isOldUpdateNotif(n, currentVersion);
      });
    }
    return res.json({ success: true, count: rows.length });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to get unread count', error: error.message });
  }
}
