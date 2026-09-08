import { Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import {
  registerDeviceToken,
  removeDeviceToken,
  sendPushNotificationToTokens,
  sendPushNotificationToUser,
  sendPushNotificationToRole,
  getFcmEngineStatus,
} from '../services/fcm.service';

/**
 * Register or update device FCM token
 * POST /api/notifications/fcm-token
 */
export async function saveDeviceToken(req: AuthenticatedRequest, res: Response) {
  try {
    const { token, deviceModel, osVersion } = req.body;
    const userId = req.user?.id || req.body.userId;
    const role = req.user?.role || req.body.role || 'rider';

    if (!token || !userId) {
      return res.status(400).json({
        success: false,
        message: 'Both token and userId are required to register device token',
      });
    }

    const saved = await registerDeviceToken({
      userId,
      role,
      token,
      deviceModel,
      osVersion,
    });

    if (saved) {
      return res.status(200).json({
        success: true,
        message: 'Device FCM token registered successfully',
        userId,
        role,
      });
    } else {
      return res.status(500).json({
        success: false,
        message: 'Failed to save device token in database',
      });
    }
  } catch (err: any) {
    console.error('❌ Error in saveDeviceToken:', err);
    return res.status(500).json({
      success: false,
      message: 'Internal server error while saving device token',
      error: err.message,
    });
  }
}

/**
 * Remove device FCM token on logout
 * DELETE /api/notifications/fcm-token
 */
export async function deleteDeviceToken(req: AuthenticatedRequest, res: Response) {
  try {
    const { token } = req.body;
    if (!token) {
      return res.status(400).json({
        success: false,
        message: 'Device token is required to remove registration',
      });
    }

    await removeDeviceToken(token);
    return res.status(200).json({
      success: true,
      message: 'Device token removed successfully',
    });
  } catch (err: any) {
    console.error('❌ Error in deleteDeviceToken:', err);
    return res.status(500).json({
      success: false,
      message: 'Internal server error while removing device token',
      error: err.message,
    });
  }
}

/**
 * Get FCM Engine status and registered devices count
 * GET /api/notifications/status
 */
export async function getNotificationStatus(_req: AuthenticatedRequest, res: Response) {
  try {
    const status = await getFcmEngineStatus();
    return res.status(200).json({
      success: true,
      data: status,
    });
  } catch (err: any) {
    return res.status(500).json({
      success: false,
      message: err.message,
    });
  }
}

/**
 * Trigger a test push notification (Admin/Debug tool)
 * POST /api/notifications/test-push
 */
export async function sendTestPushNotification(req: AuthenticatedRequest, res: Response) {
  try {
    const { token, userId, role, title, body, channelId, data } = req.body;

    const notifTitle = title || '🔔 Speedo Test Push Alert';
    const notifBody = body || 'This is a test notification verifying background system-tray delivery.';

    let result;
    if (token) {
      result = await sendPushNotificationToTokens([token], {
        title: notifTitle,
        body: notifBody,
        channelId: channelId || 'speedo_ride_alerts',
        data,
      });
    } else if (userId && role) {
      result = await sendPushNotificationToUser(userId, role, {
        title: notifTitle,
        body: notifBody,
        channelId: channelId || 'speedo_ride_alerts',
        data,
      });
    } else if (role) {
      result = await sendPushNotificationToRole(role, {
        title: notifTitle,
        body: notifBody,
        channelId: channelId || 'speedo_ride_alerts',
        data,
      });
    } else {
      return res.status(400).json({
        success: false,
        message: 'Must provide either token, userId + role, or target role',
      });
    }

    return res.status(200).json({
      success: result.success,
      result,
    });
  } catch (err: any) {
    console.error('❌ Error in sendTestPushNotification:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to dispatch test push notification',
      error: err.message,
    });
  }
}
