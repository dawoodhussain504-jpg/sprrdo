import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { getIO, emitToUser } from '../services/socket';
import { createNotification, notifyAdmins } from '../services/notification';

/**
 * 1. Create a new support ticket / complaint / query
 */
export async function createSupportTicket(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const userRole = (req.user?.role || 'rider') as 'rider' | 'captain';
    const { subject, category = 'general', message_text, ride_id } = req.body;

    if (!userId) {
      return res.status(401).json({ success: false, message: 'Authentication required. Please log in again.' });
    }

    if (!subject || !subject.trim() || !message_text || !message_text.trim()) {
      return res.status(400).json({ success: false, message: 'Subject and message are required' });
    }

    let userName = req.user?.name || (userRole === 'rider' ? 'Rider' : 'Captain');
    let userPhone = '';

    try {
      if (userRole === 'rider') {
        const uRes = await db.query('SELECT name, phone FROM users WHERE id = $1', [userId]);
        if (uRes.rows.length > 0) {
          if (uRes.rows[0].name) userName = uRes.rows[0].name;
          if (uRes.rows[0].phone) userPhone = uRes.rows[0].phone;
        }
      } else if (userRole === 'captain') {
        const cRes = await db.query('SELECT name, phone FROM captains WHERE id = $1', [userId]);
        if (cRes.rows.length > 0) {
          if (cRes.rows[0].name) userName = cRes.rows[0].name;
          if (cRes.rows[0].phone) userPhone = cRes.rows[0].phone;
        }
      }
    } catch (dbErr) {
      // Non-critical fallback
    }

    const ticketId = 'ticket_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const msgId = 'smsg_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const priority = category === 'safety' ? 'urgent' : category === 'payment_fare' ? 'high' : 'normal';

    await db.query(
      `INSERT INTO support_tickets (id, user_id, user_role, user_name, user_phone, ride_id, subject, category, status, priority, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, 'open', $9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
      [ticketId, userId, userRole, userName, userPhone, ride_id || null, subject.trim(), category, priority]
    );

    await db.query(
      `INSERT INTO support_messages (id, ticket_id, sender_id, sender_role, sender_name, message_text, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, CURRENT_TIMESTAMP)`,
      [msgId, ticketId, userId, userRole, userName, message_text.trim()]
    );

    const autoReplyId = 'smsg_' + (Date.now() + 10) + '_' + Math.random().toString(36).substring(2, 7);
    const autoReplyText = category === 'payment_fare'
      ? `Hello ${userName}, thank you for reaching out regarding your payment/fare. Our Speedo Support team is actively reviewing your request and will verify the transaction details shortly.`
      : category === 'safety'
      ? `Priority Alert: Hello ${userName}, your safety is our top priority. Speedo Safety & Trust team has been escalated to review this incident immediately.`
      : `Hello ${userName}, thanks for contacting Speedo Support! We have registered your ticket #${ticketId.slice(-6)}. A support specialist will assist you shortly.`;

    await db.query(
      `INSERT INTO support_messages (id, ticket_id, sender_id, sender_role, sender_name, message_text, created_at)
       VALUES ($1, $2, 'speedo_support_bot', 'speedo_support', 'Speedo Support Desk', $3, CURRENT_TIMESTAMP)`,
      [autoReplyId, ticketId, autoReplyText]
    );

    try {
      await notifyAdmins(
        `New ${priority.toUpperCase()} Support Query from ${userName} (${userRole.toUpperCase()})`,
        `[${category.toUpperCase()}] ${subject}: ${message_text}`,
        'support_query',
        { ticketId, userId, userRole, category }
      );
    } catch (notifErr) {}

    try {
      const io = getIO();
      io.to('role_admin').emit('support:new_ticket', {
        ticketId,
        userId,
        userRole,
        userName,
        userPhone,
        subject: subject.trim(),
        category,
        priority,
        status: 'open',
        createdAt: new Date().toISOString(),
      });
    } catch (e) {}

    return res.status(201).json({
      success: true,
      message: 'Support ticket created successfully',
      data: {
        ticketId,
        subject: subject.trim(),
        category,
        status: 'open',
        priority,
        autoReply: autoReplyText,
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to create support ticket', error: error.message });
  }
}

/**
 * 2. Get user's own tickets (Rider or Captain)
 */
export async function getUserTickets(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const ticketsRes = await db.query(
      `SELECT id, user_id as "userId", user_role as "userRole", user_name as "userName",
              user_phone as "userPhone", ride_id as "rideId", subject, category,
              status, priority, created_at as "createdAt", updated_at as "updatedAt"
       FROM support_tickets
       WHERE user_id = $1
       ORDER BY updated_at DESC`,
      [userId]
    );

    return res.json({
      success: true,
      data: ticketsRes.rows,
      count: ticketsRes.rows.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch tickets', error: error.message });
  }
}

/**
 * 3. Get all support tickets (Admin only)
 */
export async function getAdminTickets(req: AuthenticatedRequest, res: Response) {
  try {
    const { status, category, user_role } = req.query;

    let query = `SELECT id, user_id as "userId", user_role as "userRole", user_name as "userName",
                        user_phone as "userPhone", ride_id as "rideId", subject, category,
                        status, priority, created_at as "createdAt", updated_at as "updatedAt"
                 FROM support_tickets WHERE 1=1`;
    const params: any[] = [];

    if (status) {
      params.push(status);
      query += ` AND status = $${params.length}`;
    }

    if (category) {
      params.push(category);
      query += ` AND category = $${params.length}`;
    }

    if (user_role) {
      params.push(user_role);
      query += ` AND user_role = $${params.length}`;
    }

    query += ` ORDER BY CASE WHEN priority = 'urgent' THEN 1 WHEN priority = 'high' THEN 2 ELSE 3 END, updated_at DESC`;

    const ticketsRes = await db.query(query, params);

    return res.json({
      success: true,
      data: ticketsRes.rows,
      count: ticketsRes.rows.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch admin tickets', error: error.message });
  }
}

/**
 * 4. Get messages for a ticket
 */
export async function getTicketMessages(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const userRole = req.user?.role;
    const ticketId = req.params.ticketId;

    const ticketRes = await db.query('SELECT user_id FROM support_tickets WHERE id = $1', [ticketId]);
    if (ticketRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Support ticket not found' });
    }

    if (userRole !== 'admin' && ticketRes.rows[0].user_id !== userId) {
      return res.status(403).json({ success: false, message: 'Unauthorized to access this ticket' });
    }

    const messagesRes = await db.query(
      `SELECT id, ticket_id as "ticketId", sender_id as "senderId", sender_role as "senderRole",
              sender_name as "senderName", message_text as "messageText", created_at as "createdAt"
       FROM support_messages
       WHERE ticket_id = $1
       ORDER BY created_at ASC`,
      [ticketId]
    );

    return res.json({
      success: true,
      data: messagesRes.rows,
      count: messagesRes.rows.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch ticket messages', error: error.message });
  }
}

/**
 * 5. Send message on an existing ticket
 */
export async function sendTicketMessage(req: AuthenticatedRequest, res: Response) {
  try {
    const senderId = req.user?.id;
    const senderRole = req.user?.role;
    const ticketId = req.params.ticketId;
    const { message_text } = req.body;

    if (!message_text || !message_text.trim()) {
      return res.status(400).json({ success: false, message: 'Message text is required' });
    }

    const ticketRes = await db.query('SELECT * FROM support_tickets WHERE id = $1', [ticketId]);
    if (ticketRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Ticket not found' });
    }

    const ticket = ticketRes.rows[0];
    if (senderRole !== 'admin' && ticket.user_id !== senderId) {
      return res.status(403).json({ success: false, message: 'Unauthorized to send message on this ticket' });
    }

    let senderName = 'Support Specialist';
    if (senderRole === 'admin') {
      const aRes = await db.query('SELECT name FROM admins WHERE id = $1', [senderId]);
      if (aRes.rows.length > 0) senderName = aRes.rows[0].name;
    } else if (senderRole === 'rider') {
      const uRes = await db.query('SELECT name FROM users WHERE id = $1', [senderId]);
      if (uRes.rows.length > 0) senderName = uRes.rows[0].name;
    } else if (senderRole === 'captain') {
      const cRes = await db.query('SELECT name FROM captains WHERE id = $1', [senderId]);
      if (cRes.rows.length > 0) senderName = cRes.rows[0].name;
    }

    const msgId = 'smsg_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);

    await db.query(
      `INSERT INTO support_messages (id, ticket_id, sender_id, sender_role, sender_name, message_text, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, CURRENT_TIMESTAMP)`,
      [msgId, ticketId, senderId, senderRole, senderName, message_text.trim()]
    );

    if (senderRole === 'admin') {
      await db.query(
        `UPDATE support_tickets SET status = 'in_progress', updated_at = CURRENT_TIMESTAMP WHERE id = $1`,
        [ticketId]
      );

      await createNotification({
        recipientId: ticket.user_id,
        recipientRole: ticket.user_role,
        title: 'Speedo Support Response',
        message: `Admin ${senderName} replied to your query: "${message_text.trim().substring(0, 80)}"`,
        type: 'support_reply',
        metadata: { ticketId },
      });
    } else {
      await db.query(
        `UPDATE support_tickets SET updated_at = CURRENT_TIMESTAMP WHERE id = $1`,
        [ticketId]
      );
    }

    const payload = {
      id: msgId,
      ticketId,
      senderId,
      senderRole,
      senderName,
      messageText: message_text.trim(),
      createdAt: new Date().toISOString(),
    };

    try {
      const io = getIO();
      io.to(`ticket_${ticketId}`).emit('support:ticket_message', payload);
      if (senderRole === 'admin') {
        emitToUser(ticket.user_id, 'support:ticket_message', payload);
      } else {
        io.to('role_admin').emit('support:ticket_message', payload);
      }
    } catch (e) {}

    return res.status(201).json({
      success: true,
      message: 'Message sent successfully',
      data: payload,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to send message', error: error.message });
  }
}

/**
 * 6. Update Ticket Status (Resolve / Close / Reopen)
 */
export async function updateTicketStatus(req: AuthenticatedRequest, res: Response) {
  try {
    const ticketId = req.params.ticketId;
    const { status } = req.body;

    if (!['open', 'in_progress', 'resolved', 'closed'].includes(status)) {
      return res.status(400).json({ success: false, message: 'Invalid status' });
    }

    const ticketRes = await db.query('SELECT * FROM support_tickets WHERE id = $1', [ticketId]);
    if (ticketRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Ticket not found' });
    }

    const ticket = ticketRes.rows[0];

    await db.query(
      `UPDATE support_tickets SET status = $1, updated_at = CURRENT_TIMESTAMP WHERE id = $2`,
      [status, ticketId]
    );

    if (status === 'resolved') {
      await createNotification({
        recipientId: ticket.user_id,
        recipientRole: ticket.user_role,
        title: 'Support Query Resolved',
        message: `Your support ticket #${ticketId.slice(-6)} has been marked as resolved by Speedo Support.`,
        type: 'support_resolved',
        metadata: { ticketId },
      });
    }

    return res.json({
      success: true,
      message: `Ticket status updated to ${status}`,
      data: { ticketId, status },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to update ticket status', error: error.message });
  }
}
