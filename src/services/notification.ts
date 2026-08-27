import { db } from '../config/db';
import { UserRole } from '../config/jwt';

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
