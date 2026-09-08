import { db } from '../config/db';
import { UserRole } from '../config/jwt';
import { sendPushNotificationToUser } from './fcm.service';

export interface CreateNotificationParams {
  recipientId: string;
  recipientRole: UserRole;
  title: string;
  message: string;
  type: string;
  metadata?: Record<string, any>;
}

export async function createNotification(params: CreateNotificationParams) {
  const id = 'notif_' + Date.now() + '_' + Math.random().toString(36).substring(2, 8);
  const metadataJson = params.metadata ? JSON.stringify(params.metadata) : null;

  await db.query(
    `INSERT INTO notifications (id, recipient_id, recipient_role, title, message, type, is_read, metadata_json, created_at)
     VALUES ($1, $2, $3, $4, $5, $6, 0, $7, CURRENT_TIMESTAMP)`,
    [id, params.recipientId, params.recipientRole, params.title, params.message, params.type, metadataJson]
  );

  // Dispatch background FCM push notification to target device(s)
  try {
    let channelId = 'speedo_general';
    if (params.type && params.type.startsWith('ride_')) {
      channelId = 'speedo_ride_alerts';
    } else if (params.type && params.type.startsWith('kyc_')) {
      channelId = 'speedo_kyc_updates';
    } else if (params.type === 'app_update') {
      channelId = 'speedo_app_updates';
    }

    const fcmData: Record<string, string> = {
      id,
      type: params.type,
      channelId,
    };
    if (params.metadata) {
      for (const [k, v] of Object.entries(params.metadata)) {
        fcmData[k] = typeof v === 'string' ? v : JSON.stringify(v);
      }
    }

    sendPushNotificationToUser(params.recipientId, params.recipientRole, {
      title: params.title,
      body: params.message,
      channelId,
      type: params.type,
      data: fcmData,
    }).catch((err) => console.warn('[NotificationPush] Push send error:', err.message));
  } catch (pushErr: any) {
    console.warn('[NotificationPush] Push dispatch caught error:', pushErr.message);
  }

  return id;
}

export async function getUnreadCount(recipientId: string, recipientRole: UserRole): Promise<number> {
  const res = await db.query<{ count: string | number }>(
    `SELECT COUNT(*) as count FROM notifications WHERE recipient_id = $1 AND recipient_role = $2 AND is_read = 0`,
    [recipientId, recipientRole]
  );
  return Number(res.rows[0]?.count || 0);
}

export async function notifyAdmins(title: string, message: string, type: string, metadata?: Record<string, any>) {
  const admins = await db.query<{ id: string }>(`SELECT id FROM admins`);
  for (const admin of admins.rows) {
    await createNotification({
      recipientId: admin.id,
      recipientRole: 'admin',
      title,
      message,
      type,
      metadata,
    });
  }
}
