import { Router } from 'express';
import { riderRegister, riderLogin, captainRegister, captainLogin, adminLogin } from '../controllers/auth.controller';

const router = Router();

router.post('/rider/register', riderRegister);
router.post('/rider/login', riderLogin);

router.post('/captain/register', captainRegister);
router.post('/captain/login', captainLogin);

router.post('/admin/login', adminLogin);

export default router;
