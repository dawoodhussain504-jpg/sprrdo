import { Router } from 'express';
import { authenticate } from '../middleware/auth';
import { triggerSosEmergency } from '../controllers/admin.controller';

const router = Router();

// Allow any authenticated user (Rider, Captain, or Admin) to trigger SOS
router.post('/trigger', authenticate, triggerSosEmergency);

export default router;
