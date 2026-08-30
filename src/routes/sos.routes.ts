import { Router } from 'express';
import { authenticate } from '../middleware/auth';
import { triggerSosEmergency, resolveSosAlert } from '../controllers/admin.controller';

const router = Router();

// Allow any authenticated user (Rider, Captain, or Admin) to trigger SOS
router.post('/trigger', authenticate, triggerSosEmergency);

// Universal resolve endpoints for SOS alerts
router.post('/:id/resolve', authenticate, resolveSosAlert);
router.put('/:id/resolve', authenticate, resolveSosAlert);

export default router;
