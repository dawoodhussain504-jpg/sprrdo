import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { createNotification } from '../services/notification';
import { performCaptainKycOcr } from '../services/ocr.service';
import {
  emitSosAlert,
  emitSosResolved,
  emitKycStatus,
  emitSurgeUpdate,
  emitBroadcast,
} from '../services/socket';

export async function getDashboardStats(_req: AuthenticatedRequest, res: Response) {
  try {
    const ridersCountRes = await db.query('SELECT COUNT(*) as count FROM users');
    const captainsCountRes = await db.query('SELECT COUNT(*) as count FROM captains');
    const onlineCaptainsRes = await db.query('SELECT COUNT(*) as count FROM captains WHERE is_online = 1');
    const activeRidesRes = await db.query("SELECT COUNT(*) as count FROM rides WHERE status IN ('requested', 'accepted', 'arrived', 'ongoing')");
    const pendingKycRes = await db.query("SELECT COUNT(*) as count FROM captains WHERE kyc_status IN ('pending', 'under_review')");
    const completedRidesRes = await db.query("SELECT COUNT(*) as count, SUM(fare) as revenue FROM rides WHERE status = 'completed'");

    return res.json({
      success: true,
      data: {
        total_riders: Number(ridersCountRes.rows[0]?.count || 0),
        total_captains: Number(captainsCountRes.rows[0]?.count || 0),
        online_captains: Number(onlineCaptainsRes.rows[0]?.count || 0),
        active_rides: Number(activeRidesRes.rows[0]?.count || 0),
        pending_kyc_count: Number(pendingKycRes.rows[0]?.count || 0),
        completed_rides: Number(completedRidesRes.rows[0]?.count || 0),
        total_revenue: Number(completedRidesRes.rows[0]?.revenue || 0),
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch dashboard stats', error: error.message });
  }
}

export async function getKycReviewQueue(_req: AuthenticatedRequest, res: Response) {
  try {
    const captainsRes = await db.query(
      `SELECT id, name, email, phone, vehicle_type, vehicle_number, kyc_status, admin_remarks, payment_qr_url, created_at
       FROM captains
       ORDER BY CASE WHEN kyc_status = 'under_review' THEN 1 WHEN kyc_status = 'pending' THEN 2 ELSE 3 END, created_at DESC`
    );

    const captainsWithDocs = [];
    for (const capt of captainsRes.rows) {
      const docsRes = await db.query('SELECT * FROM kyc_documents WHERE captain_id = $1 ORDER BY created_at DESC', [capt.id]);
      captainsWithDocs.push({
        ...capt,
        documents: docsRes.rows,
        has_all_docs: docsRes.rows.length >= 4,
      });
    }

    return res.json({
      success: true,
      data: captainsWithDocs,
      count: captainsWithDocs.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch KYC review queue', error: error.message });
  }
}

export async function reviewKyc(req: AuthenticatedRequest, res: Response) {
  try {
    const captainId = req.params.captainId;
    const { status, admin_remarks, document_remarks } = req.body; // status: 'approved' | 'rejected'

    if (!['approved', 'rejected'].includes(status)) {
      return res.status(400).json({ success: false, message: "Status must be 'approved' or 'rejected'" });
    }

    const captRes = await db.query('SELECT name FROM captains WHERE id = $1', [captainId]);
    if (captRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Captain not found' });
    }

    await db.query(
      `UPDATE captains SET kyc_status = $1, admin_remarks = $2, updated_at = CURRENT_TIMESTAMP WHERE id = $3`,
      [status, admin_remarks || null, captainId]
    );

    // Update individual documents
    const docStatus = status === 'approved' ? 'approved' : 'rejected';
    await db.query(`UPDATE kyc_documents SET status = $1, admin_remarks = $2, updated_at = CURRENT_TIMESTAMP WHERE captain_id = $3`, [
      docStatus,
      admin_remarks || null,
      captainId,
    ]);

    // Send notification to Captain
    const isApprove = status === 'approved';
    await createNotification({
      recipientId: captainId,
      recipientRole: 'captain',
      title: isApprove ? '🎉 KYC Verification Approved!' : '⚠️ KYC Verification Rejected',
      message: isApprove
        ? 'Congratulations! Your documents have been verified. You can now go ONLINE and start accepting ride requests.'
        : `Your KYC submission was rejected. Reason: ${admin_remarks || 'Please re-upload clear document copies.'}`,
      type: 'kyc_update',
      metadata: { status, admin_remarks },
    });

    // Broadcast real-time Socket.IO event to instantly update captain app & admin queues
    emitKycStatus(captainId, {
      status,
      admin_remarks: admin_remarks || null,
      updated_at: new Date().toISOString(),
    });

    return res.json({
      success: true,
      message: `Captain KYC status updated to ${status.toUpperCase()}`,
      data: { captainId, status, admin_remarks },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to review KYC', error: error.message });
  }
}

export async function getLiveMapData(_req: AuthenticatedRequest, res: Response) {
  try {
    // 1. All online captains
    const captainsRes = await db.query(
      `SELECT c.id, c.name, c.phone, c.vehicle_type, c.vehicle_number, c.rating, c.kyc_status, c.is_online,
              l.lat, l.lng, l.bearing, l.speed, l.updated_at as location_updated_at
       FROM captains c
       JOIN locations l ON c.id = l.captain_id
       WHERE c.is_online = 1 AND c.is_active = 1`
    );

    // 2. All active rides
    const activeRidesRes = await db.query(
      `SELECT r.*, u.name as rider_name, u.phone as rider_phone,
              c.name as captain_name, c.vehicle_number,
              l.lat as live_captain_lat, l.lng as live_captain_lng
       FROM rides r
       JOIN users u ON r.rider_id = u.id
       LEFT JOIN captains c ON r.captain_id = c.id
       LEFT JOIN locations l ON c.id = l.captain_id
       WHERE r.status IN ('requested', 'accepted', 'arrived', 'ongoing')`
    );

    return res.json({
      success: true,
      data: {
        online_captains: captainsRes.rows,
        active_rides: activeRidesRes.rows,
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch live map data', error: error.message });
  }
}

export async function getRidesMonitoring(req: AuthenticatedRequest, res: Response) {
  try {
    const status = req.query.status as string;
    const vehicleType = req.query.vehicle_type as string;

    let sql = `
      SELECT r.*, u.name as rider_name, u.phone as rider_phone,
             c.name as captain_name, c.phone as captain_phone, c.vehicle_number
      FROM rides r
      JOIN users u ON r.rider_id = u.id
      LEFT JOIN captains c ON r.captain_id = c.id
      WHERE 1=1
    `;
    const params: any[] = [];

    if (status && status !== 'all') {
      params.push(status);
      sql += ` AND r.status = $${params.length}`;
    }

    if (vehicleType && vehicleType !== 'all') {
      params.push(vehicleType);
      sql += ` AND r.vehicle_type = $${params.length}`;
    }

    sql += ' ORDER BY r.created_at DESC LIMIT 100';

    const result = await db.query(sql, params);
    return res.json({ success: true, data: result.rows });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch rides monitoring list', error: error.message });
  }
}

export async function getUsersManagement(req: AuthenticatedRequest, res: Response) {
  try {
    const role = (req.query.role as string) || 'all'; // 'all' | 'rider' | 'captain'

    let riders: any[] = [];
    let captains: any[] = [];

    if (role === 'all' || role === 'rider') {
      const ridersRes = await db.query('SELECT id, name, email, phone, is_active, created_at FROM users ORDER BY created_at DESC');
      riders = ridersRes.rows.map((r) => ({ ...r, role: 'rider' }));
    }

    if (role === 'all' || role === 'captain') {
      const captRes = await db.query(
        'SELECT id, name, email, phone, vehicle_type, vehicle_number, kyc_status, is_online, rating, total_rides, total_earnings, is_active, created_at FROM captains ORDER BY created_at DESC'
      );
      captains = captRes.rows.map((c) => ({ ...c, role: 'captain', is_online: Boolean(c.is_online) }));
    }

    return res.json({
      success: true,
      data: { riders, captains },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch users', error: error.message });
  }
}

export async function toggleUserStatus(req: AuthenticatedRequest, res: Response) {
  try {
    const { role, id } = req.params;
    const { is_active } = req.body;

    const table = role === 'captain' ? 'captains' : 'users';
    const activeVal = is_active ? 1 : 0;

    await db.query(`UPDATE ${table} SET is_active = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`, [activeVal, id]);

    return res.json({
      success: true,
      message: `${role} account ${is_active ? 'activated' : 'suspended'} successfully`,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to toggle status', error: error.message });
  }
}

export async function getAdminNotifications(req: AuthenticatedRequest, res: Response) {
  try {
    const notifs = await db.query(`SELECT * FROM notifications WHERE recipient_role = 'admin' ORDER BY created_at DESC LIMIT 50`);
    const unreadCountRes = await db.query(`SELECT COUNT(*) as count FROM notifications WHERE recipient_role = 'admin' AND is_read = 0`);
    return res.json({
      success: true,
      data: notifs.rows,
      unread_count: Number(unreadCountRes.rows[0]?.count || 0),
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch admin notifications', error: error.message });
  }
}

// -------------------------------------------------------------
// 1. AI DOCUMENT OCR & INSTANT KYC SCAN
// -------------------------------------------------------------
export async function aiScanKycDocuments(req: AuthenticatedRequest, res: Response) {
  try {
    const { captainId } = req.params;
    const ocrData = await performCaptainKycOcr(captainId);

    return res.json({
      success: true,
      data: ocrData,
      message: 'AI Document OCR Scan completed successfully',
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'AI KYC Scan failed', error: error.message });
  }
}

export async function instantApproveKyc(req: AuthenticatedRequest, res: Response) {
  try {
    const { captainId } = req.params;
    const { admin_remarks } = req.body;

    const remarks = admin_remarks || 'Auto-verified & approved by Speedo AI Document Engine';

    await db.query(
      `UPDATE captains SET kyc_status = 'approved', admin_remarks = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`,
      [remarks, captainId]
    );

    await db.query(
      `UPDATE kyc_documents SET status = 'approved', admin_remarks = $1, updated_at = CURRENT_TIMESTAMP WHERE captain_id = $2`,
      [remarks, captainId]
    );

    await createNotification({
      recipientId: captainId,
      recipientRole: 'captain',
      title: '🎉 Instant AI KYC Approved!',
      message: 'Congratulations! Your documents were verified with high confidence. You can now go ONLINE to accept Speedo rides!',
      type: 'kyc_update',
      metadata: { status: 'approved', remarks },
    });

    emitKycStatus(captainId, {
      status: 'approved',
      admin_remarks: remarks || 'Instant AI Approved',
      updated_at: new Date().toISOString(),
    });

    return res.json({
      success: true,
      message: 'Captain instantly approved via AI Verification!',
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Instant approval failed', error: error.message });
  }
}

// -------------------------------------------------------------
// 2. GEOFENCED CUSTOM FARE & SURGE ENGINE
// -------------------------------------------------------------
export async function getSurgeZones(_req: AuthenticatedRequest, res: Response) {
  try {
    const zonesRes = await db.query('SELECT * FROM geofence_surge_zones ORDER BY created_at DESC');
    return res.json({
      success: true,
      data: zonesRes.rows.map((z) => ({
        ...z,
        is_active: Boolean(z.is_active),
      })),
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch surge zones', error: error.message });
  }
}

export async function createSurgeZone(req: AuthenticatedRequest, res: Response) {
  try {
    const { name, zone_type, center_lat, center_lng, radius_km, surge_multiplier, base_fare_multiplier, per_km_multiplier } = req.body;

    if (!name || center_lat == null || center_lng == null) {
      return res.status(400).json({ success: false, message: 'Name, center latitude and longitude are required' });
    }

    const id = 'zone_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
    const zType = zone_type || 'custom';
    const radius = radius_km || 3.0;
    const surge = surge_multiplier || 1.3;
    const baseMul = base_fare_multiplier || 1.25;
    const perKmMul = per_km_multiplier || 1.25;

    await db.query(
      `INSERT INTO geofence_surge_zones (id, name, zone_type, center_lat, center_lng, radius_km, surge_multiplier, base_fare_multiplier, per_km_multiplier, is_active)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 1)`,
      [id, name, zType, center_lat, center_lng, radius, surge, baseMul, perKmMul]
    );

    const createdZone = { id, name, zone_type: zType, center_lat, center_lng, radius_km: radius, surge_multiplier: surge, is_active: true };
    emitSurgeUpdate({ type: 'created', zone: createdZone });

    return res.json({
      success: true,
      data: createdZone,
      message: 'Geofenced surge zone created successfully',
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to create surge zone', error: error.message });
  }
}

export async function updateSurgeZone(req: AuthenticatedRequest, res: Response) {
  try {
    const { id } = req.params;
    const { name, surge_multiplier, base_fare_multiplier, per_km_multiplier, radius_km, is_active } = req.body;

    const existing = await db.query('SELECT * FROM geofence_surge_zones WHERE id = $1', [id]);
    if (existing.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Surge zone not found' });
    }

    const zone = existing.rows[0];
    const newName = name !== undefined ? name : zone.name;
    const newSurge = surge_multiplier !== undefined ? surge_multiplier : zone.surge_multiplier;
    const newBase = base_fare_multiplier !== undefined ? base_fare_multiplier : zone.base_fare_multiplier;
    const newPerKm = per_km_multiplier !== undefined ? per_km_multiplier : zone.per_km_multiplier;
    const newRadius = radius_km !== undefined ? radius_km : zone.radius_km;
    const newActive = is_active !== undefined ? (is_active ? 1 : 0) : zone.is_active;

    await db.query(
      `UPDATE geofence_surge_zones
       SET name = $1, surge_multiplier = $2, base_fare_multiplier = $3, per_km_multiplier = $4, radius_km = $5, is_active = $6, updated_at = CURRENT_TIMESTAMP
       WHERE id = $7`,
      [newName, newSurge, newBase, newPerKm, newRadius, newActive, id]
    );

    emitSurgeUpdate({
      type: 'updated',
      zone: { id, name: newName, surge_multiplier: newSurge, radius_km: newRadius, is_active: Boolean(newActive) },
    });

    return res.json({
      success: true,
      message: 'Surge zone updated successfully',
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update surge zone', error: error.message });
  }
}

export async function deleteSurgeZone(req: AuthenticatedRequest, res: Response) {
  try {
    const { id } = req.params;
    await db.query('DELETE FROM geofence_surge_zones WHERE id = $1', [id]);
    emitSurgeUpdate({ type: 'deleted', zoneId: id });
    return res.json({ success: true, message: 'Surge zone removed' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to delete surge zone', error: error.message });
  }
}

// -------------------------------------------------------------
// 3. LIVE SOS EMERGENCY COMMAND CENTER
// -------------------------------------------------------------
export async function getSosAlerts(_req: AuthenticatedRequest, res: Response) {
  try {
    const alertsRes = await db.query('SELECT * FROM sos_alerts ORDER BY CASE WHEN status = \'active\' THEN 1 WHEN status = \'in_progress\' THEN 2 ELSE 3 END, created_at DESC');
    return res.json({
      success: true,
      data: alertsRes.rows,
      active_count: alertsRes.rows.filter((a) => a.status === 'active' || a.status === 'in_progress').length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch SOS alerts', error: error.message });
  }
}

export async function triggerSosEmergency(req: AuthenticatedRequest, res: Response) {
  try {
    const user = req.user;
    const userRole = user?.role || 'rider';
    const userId = user?.id || 'guest';
    const { ride_id, lat, lng, address } = req.body;

    const id = 'sos_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);

    let userName = user?.name || 'Speedo User';
    let userPhone = '9876543210';
    let captainId: string | null = null;
    let captainName: string | null = null;
    let captainPhone: string | null = null;
    let vehicleNum: string | null = null;

    if (user) {
      if (user.role === 'captain') {
        const cRes = await db.query('SELECT name, phone, vehicle_number FROM captains WHERE id = $1', [user.id]);
        if (cRes.rows.length > 0) {
          userName = cRes.rows[0].name || userName;
          userPhone = cRes.rows[0].phone || userPhone;
          captainId = user.id;
          captainName = userName;
          captainPhone = userPhone;
          vehicleNum = cRes.rows[0].vehicle_number;
        }
      } else {
        const uRes = await db.query('SELECT name, phone FROM users WHERE id = $1', [user.id]);
        if (uRes.rows.length > 0) {
          userName = uRes.rows[0].name || userName;
          userPhone = uRes.rows[0].phone || userPhone;
        }
      }
    }

    if (ride_id) {
      const rideRes = await db.query('SELECT r.*, c.name as c_name, c.phone as c_phone, c.vehicle_number FROM rides r LEFT JOIN captains c ON r.captain_id = c.id WHERE r.id = $1', [ride_id]);
      if (rideRes.rows.length > 0) {
        const r = rideRes.rows[0];
        captainId = r.captain_id;
        captainName = r.c_name;
        captainPhone = r.c_phone;
        vehicleNum = r.vehicle_number;
      }
    }

    await db.query(
      `INSERT INTO sos_alerts (id, ride_id, triggered_by, user_id, user_name, user_phone, captain_id, captain_name, captain_phone, vehicle_number, lat, lng, address, status)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, 'active')`,
      [id, ride_id || null, userRole, userId, userName, userPhone, captainId, captainName, captainPhone, vehicleNum, lat || 12.9716, lng || 77.5946, address || 'Live GPS Coordinates']
    );

    const sosAlertPayload = {
      id,
      ride_id: ride_id || null,
      triggered_by: userRole,
      user_id: userId,
      user_name: userName,
      user_phone: userPhone,
      captain_id: captainId,
      captain_name: captainName,
      captain_phone: captainPhone,
      vehicle_number: vehicleNum,
      lat: lat || 12.9716,
      lng: lng || 77.5946,
      address: address || 'Live GPS Coordinates',
      status: 'active',
      created_at: new Date().toISOString(),
    };

    // Notify admins in database & push
    await createNotification({
      recipientId: 'admin_all',
      recipientRole: 'admin',
      title: '🚨 HIGH PRIORITY: SOS Emergency Triggered!',
      message: `Emergency SOS triggered by ${userName} (${userRole.toUpperCase()}) at ${address || 'Live Map Location'}. Immediate response required!`,
      type: 'sos_alert',
      metadata: { sos_id: id, ride_id, lat, lng },
    });

    // Real-Time Socket Broadcast to Admin SOS Command Center
    emitSosAlert(sosAlertPayload);

    return res.json({
      success: true,
      data: { id, status: 'active', message: 'SOS signal broadcasted to Speedo Emergency Command Center!' },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to trigger SOS', error: error.message });
  }
}

export async function resolveSosAlert(req: AuthenticatedRequest, res: Response) {
  try {
    const alertId = req.params.id || req.body.id || req.body.sos_id || (req.query.id as string);
    const status = (req.body.status || req.body.outcome || 'resolved').toLowerCase().trim();
    const adminNotes = req.body.admin_notes || req.body.adminNotes || req.body.notes || req.body.remarks || 'Resolved by Administrator';

    if (!alertId) {
      return res.status(400).json({ success: false, message: 'Alert ID is required to resolve incident.' });
    }

    console.log(`🚨 [RESOLVE SOS INITIATED] ID: ${alertId}, Status: ${status}, Notes: ${adminNotes}`);

    const isResolved = status === 'resolved' || status === 'false_alarm' || status === 'closed';

    if (isResolved) {
      await db.query(
        `UPDATE sos_alerts 
         SET status = $1, 
             admin_notes = $2, 
             resolved_at = CURRENT_TIMESTAMP 
         WHERE id = $3 OR ride_id = $3`,
        [status, adminNotes, alertId]
      );
    } else {
      await db.query(
        `UPDATE sos_alerts 
         SET status = $1, 
             admin_notes = $2, 
             resolved_at = NULL 
         WHERE id = $3 OR ride_id = $3`,
        [status, adminNotes, alertId]
      );
    }

    const resolutionPayload = {
      id: alertId,
      status,
      admin_notes: adminNotes,
      resolved_at: new Date().toISOString(),
    };

    emitSosResolved(resolutionPayload);

    return res.json({
      success: true,
      message: `SOS alert updated to ${status.toUpperCase()}`,
      data: resolutionPayload,
    });
  } catch (error: any) {
    console.error('❌ Error resolving SOS alert:', error.message);
    return res.status(500).json({ success: false, message: 'Failed to resolve SOS alert', error: error.message });
  }
}

// -------------------------------------------------------------
// 4. TARGETED CITY-WIDE BROADCASTS
// -------------------------------------------------------------
export async function sendBroadcast(req: AuthenticatedRequest, res: Response) {
  try {
    const { title, message, target_audience, target_city, coupon_code, discount_percent, bonus_amount } = req.body;

    if (!title || !message) {
      return res.status(400).json({ success: false, message: 'Broadcast title and message are required' });
    }

    const id = 'bcast_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
    const audience = target_audience || 'all'; // 'all' | 'riders' | 'captains'
    const city = target_city || 'All Cities';

    let recipientsCount = 0;
    if (audience === 'all' || audience === 'riders') {
      const riders = await db.query('SELECT id FROM users WHERE is_active = 1');
      recipientsCount += riders.rows.length;
      for (const r of riders.rows) {
        await createNotification({
          recipientId: r.id,
          recipientRole: 'rider',
          title: `📢 ${title}`,
          message: `${message}${coupon_code ? ` (Use Code: ${coupon_code} for ${discount_percent || 20}% OFF!)` : ''}`,
          type: 'general',
          metadata: { broadcast_id: id, coupon_code, discount_percent },
        });
      }
    }

    if (audience === 'all' || audience === 'captains') {
      const capts = await db.query('SELECT id FROM captains WHERE is_active = 1');
      recipientsCount += capts.rows.length;
      for (const c of capts.rows) {
        await createNotification({
          recipientId: c.id,
          recipientRole: 'captain',
          title: `📢 ${title}`,
          message: `${message}${bonus_amount ? ` (Earn +₹${bonus_amount} extra incentive bonus!)` : ''}`,
          type: 'general',
          metadata: { broadcast_id: id, bonus_amount },
        });
      }
    }

    await db.query(
      `INSERT INTO broadcast_announcements (id, title, message, target_audience, target_city, coupon_code, discount_percent, bonus_amount, total_recipients)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9)`,
      [id, title, message, audience, city, coupon_code || null, discount_percent || 0.0, bonus_amount || 0.0, recipientsCount]
    );

    const bcastPayload = {
      id,
      title,
      message,
      target_audience: audience,
      target_city: city,
      coupon_code: coupon_code || null,
      discount_percent: discount_percent || 0.0,
      bonus_amount: bonus_amount || 0.0,
      total_recipients: recipientsCount,
      created_at: new Date().toISOString(),
    };
    emitBroadcast(bcastPayload);

    return res.json({
      success: true,
      data: { id, total_recipients: recipientsCount },
      message: `Broadcast delivered to ${recipientsCount} active users in ${city}!`,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Broadcast failed', error: error.message });
  }
}

export async function getBroadcasts(_req: AuthenticatedRequest, res: Response) {
  try {
    const listRes = await db.query('SELECT * FROM broadcast_announcements ORDER BY created_at DESC LIMIT 50');
    return res.json({
      success: true,
      data: listRes.rows,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch broadcasts', error: error.message });
  }
}
