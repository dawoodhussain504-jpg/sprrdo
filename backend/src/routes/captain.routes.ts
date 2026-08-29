import { Router } from 'express';
import { requireRole } from '../middleware/auth';
import { upload } from '../middleware/upload';
import { uploadKycDocument, getKycStatus } from '../controllers/kyc.controller';
import {
  getCaptainProfile,
  toggleOnlineStatus,
  updateLocation,
  getIncomingRideRequests,
  acceptRide,
  getCaptainActiveRide,
  updateRideStatus,
  getCaptainRideHistory,
  getCaptainNotifications,
  markCaptainNotificationRead,
  getCaptainUnreadCount,
} from '../controllers/captain.controller';
import { getRoute } from '../controllers/route.controller';

const router = Router();

// Apply captain role guard to all /api/captain routes
router.use(requireRole('captain'));

// Profile & Status
router.get('/profile', getCaptainProfile);
router.post('/status/toggle', toggleOnlineStatus);

// Live GPS location push (from ForegroundService)
router.post('/location/update', updateLocation);
router.post('/routes/calculate', getRoute);

// KYC Document Upload & Status
router.post('/kyc/upload', upload.single('document'), uploadKycDocument);
router.get('/kyc/status', getKycStatus);

// Ride Management
router.get('/rides/requests', getIncomingRideRequests);
router.post('/rides/:id/accept', acceptRide);
router.get('/rides/active', getCaptainActiveRide);
router.post('/rides/:id/status', updateRideStatus);
router.get('/rides/history', getCaptainRideHistory);

// Notifications & Badges
router.get('/notifications', getCaptainNotifications);
router.put('/notifications/:id/read', markCaptainNotificationRead);
router.get('/notifications/unread-count', getCaptainUnreadCount);

export default router;
