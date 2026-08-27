import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { createNotification } from '../services/notification';

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
