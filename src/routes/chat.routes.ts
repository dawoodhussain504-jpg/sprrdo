import { Router } from 'express';
import { authenticate } from '../middleware/auth';
import { sendMessage, getRideMessages, markMessagesRead } from '../controllers/chat.controller';

const router = Router();

// Apply auth guard
router.use(authenticate);

router.post('/rides/:rideId/messages', sendMessage);
router.get('/rides/:rideId/messages', getRideMessages);
router.patch('/rides/:rideId/read', markMessagesRead);

export default router;
