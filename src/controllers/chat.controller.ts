import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { emitRideEvent, emitToUser } from '../services/socket';
import { createNotification } from '../services/notification';

export async function sendMessage(req: AuthenticatedRequest, res: Response) {
  try {
    const senderId = req.user?.id;
    const senderRole = req.user?.role as 'rider' | 'captain';
    const rideId = req.params.rideId;
    const { message_text, message_type = 'text', audio_url } = req.body;

    if (!message_text && !audio_url) {
      return res.status(400).json({ success: false, message: 'Message text or audio URL is required' });
    }

    // Verify ride exists and user is participant
    const rideRes = await db.query('SELECT * FROM rides WHERE id = $1', [rideId]);
    if (rideRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Ride not found' });
    }

    const ride = rideRes.rows[0];
    const isParticipant =
      (senderRole === 'rider' && ride.rider_id === senderId) ||
      (senderRole === 'captain' && ride.captain_id === senderId);

    if (!isParticipant) {
      return res.status(403).json({ success: false, message: 'You are not a participant in this ride' });
    }

    const recipientId = senderRole === 'rider' ? ride.captain_id : ride.rider_id;
    const recipientRole = senderRole === 'rider' ? 'captain' : 'rider';

    if (!recipientId) {
      return res.status(400).json({ success: false, message: 'Recipient is not assigned to this ride yet' });
    }

    const messageId = 'msg_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);

    await db.query(
      `INSERT INTO messages (id, ride_id, sender_id, sender_role, recipient_id, recipient_role, message_text, message_type, audio_url, is_read, created_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 0, CURRENT_TIMESTAMP)`,
      [
        messageId,
        rideId,
        senderId,
        senderRole,
        recipientId,
        recipientRole,
        message_text || '',
        message_type,
        audio_url || null,
      ]
    );

    const messagePayload = {
      id: messageId,
      rideId,
      senderId,
      senderRole,
      recipientId,
      recipientRole,
      messageText: message_text || '',
      messageType: message_type,
      audioUrl: audio_url || null,
      isRead: false,
      createdAt: new Date().toISOString(),
    };

    // 1. Instant Real-Time WebSocket Delivery to Ride Room
    emitRideEvent(rideId, 'ride:chat_message', messagePayload);
    emitRideEvent(rideId, 'chat:message', messagePayload);

    // 2. Also emit directly to recipient's and sender's private user channels
    emitToUser(recipientId, 'user:new_chat_message', messagePayload);
    emitToUser(recipientId, 'ride:chat_message', messagePayload);
    if (senderId) {
      emitToUser(senderId, 'chat:message', messagePayload);
    }

    // 3. Create persistent Notification
    await createNotification({
      recipientId,
      recipientRole,
      title: `Message from ${senderRole === 'rider' ? 'Passenger' : 'Captain'}`,
      message: message_text || 'Sent an audio message 🎙️',
      type: 'chat_message',
      metadata: { rideId, messageId },
    });

    return res.status(201).json({
      success: true,
      message: 'Message sent successfully',
      data: messagePayload,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to send message', error: error.message });
  }
}

export async function getRideMessages(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const userRole = req.user?.role;
    const rideId = req.params.rideId;

    // Verify ride participant
    const rideRes = await db.query('SELECT rider_id, captain_id FROM rides WHERE id = $1', [rideId]);
    if (rideRes.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Ride not found' });
    }

    const ride = rideRes.rows[0];
    const isParticipant =
      userRole === 'admin' ||
      (userRole === 'rider' && ride.rider_id === userId) ||
      (userRole === 'captain' && ride.captain_id === userId);

    if (!isParticipant) {
      return res.status(403).json({ success: false, message: 'Unauthorized to view this conversation' });
    }

    const messagesRes = await db.query(
      `SELECT id, ride_id as "rideId", sender_id as "senderId", sender_role as "senderRole",
              recipient_id as "recipientId", recipient_role as "recipientRole",
              message_text as "messageText", message_type as "messageType",
              audio_url as "audioUrl", is_read as "isRead", created_at as "createdAt"
       FROM messages
       WHERE ride_id = $1
       ORDER BY created_at ASC`,
      [rideId]
    );

    return res.json({
      success: true,
      data: messagesRes.rows,
      count: messagesRes.rows.length,
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to fetch messages', error: error.message });
  }
}

export async function markMessagesRead(req: AuthenticatedRequest, res: Response) {
  try {
    const userId = req.user?.id;
    const rideId = req.params.rideId;

    await db.query(
      `UPDATE messages SET is_read = 1 WHERE ride_id = $1 AND recipient_id = $2`,
      [rideId, userId]
    );

    return res.json({ success: true, message: 'Messages marked as read' });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Failed to mark messages as read', error: error.message });
  }
}
