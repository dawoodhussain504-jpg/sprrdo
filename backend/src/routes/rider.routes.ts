import { Router } from 'express';
import { requireRole } from '../middleware/auth';
import {
  getRiderProfile,
  getNearbyCaptains,
  estimateFares,
  requestRide,
  getActiveRide,
  cancelRide,
  getRideHistory,
  getRiderNotifications,
  markNotificationRead,
  getRiderUnreadCount,
} from '../controllers/rider.controller';
import { getRoute } from '../controllers/route.controller';

const router = Router();

// Apply rider role guard to all /api/rider routes
router.use(requireRole('rider'));

router.get('/profile', getRiderProfile);
router.get('/captains/nearby', getNearbyCaptains);
router.post('/fares/estimate', estimateFares);
router.post('/routes/calculate', getRoute);
router.post('/rides/request', requestRide);
router.get('/rides/active', getActiveRide);
router.post('/rides/:id/cancel', cancelRide);
router.get('/rides/history', getRideHistory);
router.get('/notifications', getRiderNotifications);
router.put('/notifications/:id/read', markNotificationRead);
router.get('/notifications/unread-count', getRiderUnreadCount);

export default router;
