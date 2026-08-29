import { Router } from 'express';
import { authenticate, requireRole } from '../middleware/auth';
import {
  createSupportTicket,
  getUserTickets,
  getAdminTickets,
  getTicketMessages,
  sendTicketMessage,
  updateTicketStatus,
} from '../controllers/support.controller';

const router = Router();

// User routes (Rider & Captain)
router.post('/tickets', authenticate, createSupportTicket);
router.get('/tickets', authenticate, getUserTickets);
router.get('/tickets/:ticketId/messages', authenticate, getTicketMessages);
router.post('/tickets/:ticketId/messages', authenticate, sendTicketMessage);

// Admin routes
router.get('/admin/tickets', requireRole('admin'), getAdminTickets);
router.patch('/admin/tickets/:ticketId/status', requireRole('admin'), updateTicketStatus);

export default router;
