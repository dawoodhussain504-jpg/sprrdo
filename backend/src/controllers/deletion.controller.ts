import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import {
  emitAccountDeletionRequested,
  emitAccountDeleted,
  emitAccountDeletionCancelled
} from '../services/socket';

export async function requestAccountDeletion(req: AuthenticatedRequest, res: Response) {
  try {
    const user = req.user;
    if (!user) {
      return res.status(401).json({ success: false, message: 'Authentication required' });
    }

    const { reason } = req.body;

    // Check if user already has an active pending request
    const existing = await db.query(
      `SELECT * FROM account_deletion_requests
       WHERE user_id = $1 AND status = 'pending'`,
      [user.id]
    );

    if (existing.rows.length > 0) {
      return res.status(400).json({
        success: false,
        message: 'A deletion request is already pending review for this account',
        data: existing.rows[0]
      });
    }

    // Fetch user details depending on role
    let name = 'User';
    let phone = '';
    let email = user.email || '';

    if (user.role === 'captain') {
      const captainRes = await db.query('SELECT name, phone, email FROM captains WHERE id = $1', [user.id]);
      if (captainRes.rows.length > 0) {
        name = captainRes.rows[0].name;
        phone = captainRes.rows[0].phone;
        email = captainRes.rows[0].email;
      }
    } else {
      const userRes = await db.query('SELECT name, phone, email FROM users WHERE id = $1', [user.id]);
      if (userRes.rows.length > 0) {
        name = userRes.rows[0].name;
        phone = userRes.rows[0].phone;
        email = userRes.rows[0].email;
      }
    }

    const id = 'del_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
    const now = new Date();
    // 24 hours grace period for admin review & verification
    const scheduledAt = new Date(now.getTime() + 24 * 60 * 60 * 1000);

    const insertRes = await db.query(
      `INSERT INTO account_deletion_requests (
        id, user_id, user_role, name, phone, email, reason, status, requested_at, scheduled_deletion_at
      ) VALUES ($1, $2, $3, $4, $5, $6, $7, 'pending', $8, $9)
      RETURNING *`,
      [id, user.id, user.role, name, phone, email, reason || 'User requested account deletion', now.toISOString(), scheduledAt.toISOString()]
    );

    const record = insertRes.rows[0];

    // Real-time WebSocket emission to Speedo Admin
    emitAccountDeletionRequested(record);

    return res.status(201).json({
      success: true,
      message: 'Account deletion request submitted. Deletion review period takes 24 hours and requires Speedo Admin approval.',
      data: record
    });
  } catch (error: any) {
    console.error('Error requesting account deletion:', error);
    return res.status(500).json({ success: false, message: 'Failed to submit account deletion request', error: error.message });
  }
}

export async function getAccountDeletionStatus(req: AuthenticatedRequest, res: Response) {
  try {
    const user = req.user;
    if (!user) {
      return res.status(401).json({ success: false, message: 'Authentication required' });
    }

    const result = await db.query(
      `SELECT * FROM account_deletion_requests
       WHERE user_id = $1
       ORDER BY created_at DESC
       LIMIT 1`,
      [user.id]
    );

    if (result.rows.length === 0) {
      return res.json({
        success: true,
        hasPendingRequest: false,
        data: null
      });
    }

    const latest = result.rows[0];
    const isPending = latest.status === 'pending';

    return res.json({
      success: true,
      hasPendingRequest: isPending,
      data: latest
    });
  } catch (error: any) {
    console.error('Error fetching deletion status:', error);
    return res.status(500).json({ success: false, message: 'Failed to fetch deletion status', error: error.message });
  }
}

export async function cancelAccountDeletion(req: AuthenticatedRequest, res: Response) {
  try {
    const user = req.user;
    if (!user) {
      return res.status(401).json({ success: false, message: 'Authentication required' });
    }

    const pending = await db.query(
      `SELECT * FROM account_deletion_requests
       WHERE user_id = $1 AND status = 'pending'`,
      [user.id]
    );

    if (pending.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'No active pending deletion request found' });
    }

    const reqId = pending.rows[0].id;
    await db.query(
      `UPDATE account_deletion_requests
       SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP
       WHERE id = $1`,
      [reqId]
    );

    emitAccountDeletionCancelled({ id: reqId, user_id: user.id, status: 'cancelled' });

    return res.json({
      success: true,
      message: 'Account deletion request has been cancelled successfully'
    });
  } catch (error: any) {
    console.error('Error cancelling account deletion:', error);
    return res.status(500).json({ success: false, message: 'Failed to cancel deletion request', error: error.message });
  }
}

// --- ADMIN CONTROLLERS ---

export async function getAdminDeletionRequests(req: AuthenticatedRequest, res: Response) {
  try {
    const { status } = req.query;

    let query = `SELECT * FROM account_deletion_requests`;
    const params: any[] = [];

    if (status && status !== 'all') {
      query += ` WHERE status = $1`;
      params.push(status);
    }

    query += ` ORDER BY created_at DESC`;

    const result = await db.query(query, params);

    return res.json({
      success: true,
      data: result.rows
    });
  } catch (error: any) {
    console.error('Error fetching admin deletion requests:', error);
    return res.status(500).json({ success: false, message: 'Failed to fetch deletion requests', error: error.message });
  }
}

export async function approveAccountDeletion(req: AuthenticatedRequest, res: Response) {
  try {
    const { id } = req.params;
    const { admin_notes } = req.body;
    const adminUser = req.user;

    const requestRes = await db.query('SELECT * FROM account_deletion_requests WHERE id = $1', [id]);
    if (requestRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Deletion request not found' });
    }

    const deletionReq = requestRes.rows[0];
    const targetUserId = deletionReq.user_id;
    const targetRole = deletionReq.user_role;

    console.log(`🗑️ [ADMIN APPROVING DELETION] Purging ${targetRole} id: ${targetUserId}`);

    // PERMANENT DELETION FROM REALTIME BACKEND DATABASE
    if (targetRole === 'captain') {
      // 1. Delete captain locations
      await db.query('DELETE FROM locations WHERE captain_id = $1', [targetUserId]);
      // 2. Delete KYC documents
      await db.query('DELETE FROM kyc_documents WHERE captain_id = $1', [targetUserId]);
      // 3. Clear or cascade notifications
      await db.query('DELETE FROM notifications WHERE recipient_id = $1', [targetUserId]);
      // 4. Anonymize/detach rides so history financial reports remain consistent
      await db.query('UPDATE rides SET captain_id = NULL WHERE captain_id = $1', [targetUserId]);
      // 5. Permanently delete captain from database
      await db.query('DELETE FROM captains WHERE id = $1', [targetUserId]);
    } else {
      // Rider
      // 1. Clear notifications
      await db.query('DELETE FROM notifications WHERE recipient_id = $1', [targetUserId]);
      // 2. Anonymize rides
      await db.query('UPDATE rides SET rider_id = $1 WHERE rider_id = $2', ['deleted_user', targetUserId]);
      // 3. Permanently delete rider from database
      await db.query('DELETE FROM users WHERE id = $1', [targetUserId]);
    }

    // Update request status to 'approved'
    await db.query(
      `UPDATE account_deletion_requests
       SET status = 'approved', reviewed_at = CURRENT_TIMESTAMP, reviewed_by = $1, admin_notes = $2, updated_at = CURRENT_TIMESTAMP
       WHERE id = $3`,
      [adminUser?.id || 'admin', admin_notes || 'Approved and permanently deleted by Admin', id]
    );

    // Notify user's socket room and force sign out
    emitAccountDeleted(targetUserId, {
      requestId: id,
      message: 'Your account has been deleted by Speedo Admin as per your request. You have been logged out.'
    });

    return res.json({
      success: true,
      message: `Account for ${deletionReq.name} (${targetRole}) has been permanently deleted from the database.`
    });
  } catch (error: any) {
    console.error('Error approving account deletion:', error);
    return res.status(500).json({ success: false, message: 'Failed to approve account deletion', error: error.message });
  }
}

export async function rejectAccountDeletion(req: AuthenticatedRequest, res: Response) {
  try {
    const { id } = req.params;
    const { admin_notes } = req.body;
    const adminUser = req.user;

    const requestRes = await db.query('SELECT * FROM account_deletion_requests WHERE id = $1', [id]);
    if (requestRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Deletion request not found' });
    }

    await db.query(
      `UPDATE account_deletion_requests
       SET status = 'rejected', reviewed_at = CURRENT_TIMESTAMP, reviewed_by = $1, admin_notes = $2, updated_at = CURRENT_TIMESTAMP
       WHERE id = $3`,
      [adminUser?.id || 'admin', admin_notes || 'Deletion request rejected by Admin', id]
    );

    return res.json({
      success: true,
      message: 'Account deletion request has been rejected. The account remains active.'
    });
  } catch (error: any) {
    console.error('Error rejecting account deletion:', error);
    return res.status(500).json({ success: false, message: 'Failed to reject account deletion', error: error.message });
  }
}
