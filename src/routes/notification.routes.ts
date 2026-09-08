import { Router } from 'express';
import { authenticate } from '../middleware/auth';
import {
  saveDeviceToken,
  deleteDeviceToken,
  getNotificationStatus,
  sendTestPushNotification,
} from '../controllers/notification.controller';

const router = Router();

// Device token registration & management
router.post(
  '/fcm-token',
  (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      return authenticate(req as any, res, next);
    }
    next();
  },
  saveDeviceToken
);

router.delete(
  '/fcm-token',
  (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      return authenticate(req as any, res, next);
    }
    next();
  },
  deleteDeviceToken
);

// Engine status & diagnostics
router.get('/status', getNotificationStatus);

// Test push notification dispatch (for testing and verification)
router.post('/test-push', sendTestPushNotification);

export default router;
