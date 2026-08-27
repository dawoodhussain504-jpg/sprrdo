import { Router } from 'express';
import { requireRole } from '../middleware/auth';
import {
  getDashboardStats,
  getKycReviewQueue,
  reviewKyc,
  getLiveMapData,
  getRidesMonitoring,
  getUsersManagement,
  toggleUserStatus,
  getAdminNotifications,
} from '../controllers/admin.controller';

const router = Router();

// Apply admin role guard to all /api/admin routes
router.use(requireRole('admin'));

// Analytics & Dashboard
router.get('/dashboard', getDashboardStats);

// KYC Queue & Review
router.get('/kyc/queue', getKycReviewQueue);
router.post('/kyc/:captainId/review', reviewKyc);

// Live Fleet Map
router.get('/map/live', getLiveMapData);

// Ride Monitoring
router.get('/rides', getRidesMonitoring);

// User & Captain Moderation
router.get('/users', getUsersManagement);
router.post('/users/:role/:id/toggle-status', toggleUserStatus);

// Notifications & Badges
router.get('/notifications', getAdminNotifications);

export default router;
