import { initializeApp, cert, getApps, App } from 'firebase-admin/app';
import { getMessaging, MulticastMessage, SendResponse } from 'firebase-admin/messaging';
import path from 'path';
import fs from 'fs';
import { getDb } from '../config/db';

export interface FcmPayload {
  title: string;
  body: string;
  channelId?: string;
  type?: string;
  data?: Record<string, string>;
  sound?: string;
}

export interface FcmSendResult {
  success: boolean;
  successCount: number;
  failureCount: number;
  message?: string;
}

let fcmApp: App | null = null;
let fcmInitError: string | null = null;

/**
 * Initializes the Firebase Admin SDK.
 * Supports:
 * 1. FIREBASE_SERVICE_ACCOUNT (raw JSON string in environment)
 * 2. FIREBASE_SERVICE_ACCOUNT_PATH (file path)
 * 3. Individual env vars: FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY
 * 4. Local file: backend/firebase-service-account.json or speedo-firebase.json
 */
function initFirebaseAdmin(): boolean {
  if (fcmApp) return true;
  if (getApps().length > 0) {
    fcmApp = getApps()[0];
    return true;
  }

  try {
    // 1. Raw JSON string in environment variable
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      try {
        const credentials = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
        fcmApp = initializeApp({
          credential: cert(credentials),
        });
        console.log(' [FCM] Firebase Admin SDK initialized via FIREBASE_SERVICE_ACCOUNT env variable.');
        return true;
      } catch (err: any) {
        console.warn('⚠️ [FCM] Failed to parse FIREBASE_SERVICE_ACCOUNT JSON:', err.message);
      }
    }

    // 2. Individual environment variables (Railway friendly)
    if (
      process.env.FIREBASE_PROJECT_ID &&
      process.env.FIREBASE_CLIENT_EMAIL &&
      process.env.FIREBASE_PRIVATE_KEY
    ) {
      try {
        const privateKey = process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n');
        fcmApp = initializeApp({
          credential: cert({
            projectId: process.env.FIREBASE_PROJECT_ID,
            clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
            privateKey,
          }),
        });
        console.log(` [FCM] Firebase Admin SDK initialized for project: ${process.env.FIREBASE_PROJECT_ID}`);
        return true;
      } catch (err: any) {
        console.warn('⚠️ [FCM] Failed to initialize via individual env vars:', err.message);
      }
    }

    // 3. Service account JSON file candidates
    const fileCandidates = [
      process.env.FIREBASE_SERVICE_ACCOUNT_PATH,
      path.resolve(__dirname, '../../firebase-service-account.json'),
      path.resolve(__dirname, '../../../firebase-service-account.json'),
      path.resolve(process.cwd(), 'firebase-service-account.json'),
      path.resolve(process.cwd(), 'speedo-firebase.json'),
    ].filter(Boolean) as string[];

    for (const filePath of fileCandidates) {
      if (fs.existsSync(filePath)) {
        try {
          const raw = fs.readFileSync(filePath, 'utf8');
          const credentials = JSON.parse(raw);
          fcmApp = initializeApp({
            credential: cert(credentials),
          });
          console.log(` [FCM] Firebase Admin SDK initialized from file: ${filePath}`);
          return true;
        } catch (err: any) {
          console.warn(`⚠️ [FCM] Error reading service account from ${filePath}:`, err.message);
        }
      }
    }

    // 4. Fallback: No credentials found
    fcmInitError = 'No Firebase service account credentials configured. Push notifications will operate in fallback mode.';
    console.log('ℹ️ [FCM] Running in fallback mode (device tokens recorded; socket & in-app alerts active).');
    return false;
  } catch (err: any) {
    fcmInitError = err.message;
    console.warn('⚠️ [FCM] Initialization error:', err.message);
    return false;
  }
}

// Attempt initialization immediately on module load
initFirebaseAdmin();

/**
 * Register or update a user's device FCM push token in the database
 */
export async function registerDeviceToken(params: {
  userId: string;
  role: string;
  token: string;
  deviceModel?: string;
  osVersion?: string;
}): Promise<boolean> {
  if (!params.token || !params.userId) return false;

  try {
    const db = getDb();
    const id = 'tok_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);

    if (db.isPostgres()) {
      await db.query(
        `INSERT INTO device_tokens (id, user_id, role, token, device_model, os_version, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, CURRENT_TIMESTAMP)
         ON CONFLICT (user_id, token) 
         DO UPDATE SET role = EXCLUDED.role,
                       device_model = COALESCE(EXCLUDED.device_model, device_tokens.device_model),
                       os_version = COALESCE(EXCLUDED.os_version, device_tokens.os_version),
                       updated_at = CURRENT_TIMESTAMP`,
        [id, params.userId, params.role, params.token, params.deviceModel || null, params.osVersion || null]
      );
    } else {
      // SQLite fallback with UPSERT
      await db.query(
        `INSERT INTO device_tokens (id, user_id, role, token, device_model, os_version, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, CURRENT_TIMESTAMP)
         ON CONFLICT(user_id, token) DO UPDATE SET
           role = excluded.role,
           device_model = COALESCE(excluded.device_model, device_tokens.device_model),
           os_version = COALESCE(excluded.os_version, device_tokens.os_version),
           updated_at = CURRENT_TIMESTAMP`,
        [id, params.userId, params.role, params.token, params.deviceModel || null, params.osVersion || null]
      );
    }

    return true;
  } catch (err: any) {
    console.error('❌ [FCM] Error registering device token:', err.message);
    return false;
  }
}

/**
 * Remove a device token on logout
 */
export async function removeDeviceToken(token: string): Promise<boolean> {
  if (!token) return false;
  try {
    const db = getDb();
    await db.query(`DELETE FROM device_tokens WHERE token = $1`, [token]);
    return true;
  } catch (err: any) {
    console.error('❌ [FCM] Error removing device token:', err.message);
    return false;
  }
}

/**
 * Remove stale or invalid tokens returned by FCM
 */
async function cleanupStaleTokens(tokens: string[]) {
  if (!tokens || tokens.length === 0) return;
  try {
    const db = getDb();
    for (const t of tokens) {
      await db.query(`DELETE FROM device_tokens WHERE token = $1`, [t]);
    }
    console.log(`🧹 [FCM] Cleaned up ${tokens.length} invalid/stale device tokens from database.`);
  } catch (err: any) {
    console.warn('⚠️ [FCM] Error during stale token cleanup:', err.message);
  }
}

/**
 * Send high-priority system-level push notification to a list of tokens
 */
export async function sendPushNotificationToTokens(
  tokens: string[],
  payload: FcmPayload
): Promise<FcmSendResult> {
  const uniqueTokens = Array.from(new Set(tokens.filter(Boolean)));
  if (uniqueTokens.length === 0) {
    return { success: true, successCount: 0, failureCount: 0, message: 'No target tokens provided' };
  }

  // Check if live FCM SDK is available
  if (!fcmApp && !initFirebaseAdmin()) {
    console.log(`📡 [FCM Simulated Push] "${payload.title}" -> ${uniqueTokens.length} device(s)`);
    return {
      success: true,
      successCount: uniqueTokens.length,
      failureCount: 0,
      message: 'Simulated push (credentials not configured)',
    };
  }

  try {
    const channelId = payload.channelId || 'speedo_ride_alerts';
    const dataMap: Record<string, string> = {
      title: payload.title,
      body: payload.body,
      type: payload.type || 'general',
      channelId,
      timestamp: Date.now().toString(),
      ...(payload.data || {}),
    };

    let totalSuccess = 0;
    let totalFailure = 0;
    const staleTokens: string[] = [];

    // Batch tokens in chunks of 500 (FCM multicast limit)
    const BATCH_SIZE = 500;
    const messaging = getMessaging(fcmApp!);

    for (let i = 0; i < uniqueTokens.length; i += BATCH_SIZE) {
      const batch = uniqueTokens.slice(i, i + BATCH_SIZE);

      const multicastMessage: MulticastMessage = {
        tokens: batch,
        notification: {
          title: payload.title,
          body: payload.body,
        },
        android: {
          priority: 'high',
          notification: {
            channelId,
            sound: payload.sound || 'default',
            defaultVibrateTimings: true,
            priority: 'high',
            visibility: 'public',
            clickAction: 'OPEN_SPEEDO_ACTIVITY',
          },
        },
        data: dataMap,
      };

      const response = await messaging.sendEachForMulticast(multicastMessage);
      totalSuccess += response.successCount;
      totalFailure += response.failureCount;

      response.responses.forEach((resp: SendResponse, idx: number) => {
        if (!resp.success && resp.error) {
          const code = resp.error.code;
          if (
            code === 'messaging/invalid-registration-token' ||
            code === 'messaging/registration-token-not-registered'
          ) {
            staleTokens.push(batch[idx]);
          }
        }
      });
    }

    if (staleTokens.length > 0) {
      await cleanupStaleTokens(staleTokens);
    }

    console.log(`🚀 [FCM] Dispatched push: "${payload.title}" | Success: ${totalSuccess}, Failed: ${totalFailure}`);
    return {
      success: totalSuccess > 0 || totalFailure === 0,
      successCount: totalSuccess,
      failureCount: totalFailure,
    };
  } catch (err: any) {
    console.error('❌ [FCM] Multicast send error:', err.message);
    return {
      success: false,
      successCount: 0,
      failureCount: uniqueTokens.length,
      message: err.message,
    };
  }
}

/**
 * Send push notification to a specific user (by userId and role)
 */
export async function sendPushNotificationToUser(
  userId: string,
  role: string,
  payload: FcmPayload
): Promise<FcmSendResult> {
  try {
    const db = getDb();
    const res = await db.query<{ token: string }>(
      `SELECT token FROM device_tokens WHERE user_id = $1 AND role = $2`,
      [userId, role]
    );

    const tokens = res.rows.map((r) => r.token);
    return await sendPushNotificationToTokens(tokens, payload);
  } catch (err: any) {
    console.error(`❌ [FCM] Error querying tokens for user ${userId}:`, err.message);
    return { success: false, successCount: 0, failureCount: 0, message: err.message };
  }
}

/**
 * Send push notification to all users of a specific role ('rider', 'captain', 'admin', or 'all')
 */
export async function sendPushNotificationToRole(
  role: string,
  payload: FcmPayload
): Promise<FcmSendResult> {
  try {
    const db = getDb();
    let query = `SELECT token FROM device_tokens`;
    const params: any[] = [];

    if (role && role !== 'all') {
      query += ` WHERE role = $1`;
      params.push(role);
    }

    const res = await db.query<{ token: string }>(query, params);
    const tokens = res.rows.map((r) => r.token);
    return await sendPushNotificationToTokens(tokens, payload);
  } catch (err: any) {
    console.error(`❌ [FCM] Error querying tokens for role ${role}:`, err.message);
    return { success: false, successCount: 0, failureCount: 0, message: err.message };
  }
}

/**
 * Check FCM status and registered token counts
 */
export async function getFcmEngineStatus() {
  let tokenCount = 0;
  let breakdown: Record<string, number> = { rider: 0, captain: 0, admin: 0 };

  try {
    const db = getDb();
    const countRes = await db.query<{ count: string | number }>(`SELECT COUNT(*) as count FROM device_tokens`);
    tokenCount = Number(countRes.rows[0]?.count || 0);

    const breakdownRes = await db.query<{ role: string; count: string | number }>(
      `SELECT role, COUNT(*) as count FROM device_tokens GROUP BY role`
    );
    for (const row of breakdownRes.rows) {
      breakdown[row.role] = Number(row.count || 0);
    }
  } catch (_) {}

  return {
    isConfigured: fcmApp !== null,
    initError: fcmInitError,
    totalRegisteredDevices: tokenCount,
    deviceBreakdown: breakdown,
  };
}
