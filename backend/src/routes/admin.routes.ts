import { Router } from 'express';
import { requireRole } from '../middleware/auth';
import {
  getDashboardStats,
  getKycReviewQueue,
  reviewKyc,
  aiScanKycDocuments,
  instantApproveKyc,
  getSurgeZones,
  createSurgeZone,
  updateSurgeZone,
  deleteSurgeZone,
  getSosAlerts,
  triggerSosEmergency,
  resolveSosAlert,
  sendBroadcast,
  getBroadcasts,
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

// 1. KYC Queue, AI Document OCR & Review
router.get('/kyc/queue', getKycReviewQueue);
router.post('/kyc/:captainId/review', reviewKyc);
router.post('/kyc/:captainId/ai-scan', aiScanKycDocuments);
router.post('/kyc/:captainId/instant-approve', instantApproveKyc);

// 2. Geofenced Custom Fare & Surge Engine
router.get('/surge-zones', getSurgeZones);
router.post('/surge-zones', createSurgeZone);
router.put('/surge-zones/:id', updateSurgeZone);
router.delete('/surge-zones/:id', deleteSurgeZone);

// 3. Live SOS Emergency Command Center
router.get('/sos-alerts', getSosAlerts);
router.post('/sos-alerts/trigger', triggerSosEmergency);
router.post('/sos-alerts/:id/resolve', resolveSosAlert);
router.put('/sos-alerts/:id/resolve', resolveSosAlert);
router.post('/sos-alerts/:id/close', resolveSosAlert);

// 4. Targeted City-Wide Broadcasts
router.post('/broadcast', sendBroadcast);
router.get('/broadcasts', getBroadcasts);

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

