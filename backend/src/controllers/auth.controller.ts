import { Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import { db } from '../config/db';
import { generateToken } from '../config/jwt';
import { normalizeVehicleType } from '../services/distance';

export async function riderRegister(req: Request, res: Response) {
  try {
    const { name, email, password, phone } = req.body;
    if (!name || !email || !password || !phone) {
      return res.status(400).json({ success: false, message: 'All fields are required' });
    }

    const existing = await db.query('SELECT id FROM users WHERE email = $1', [email.toLowerCase()]);
    if (existing.rows.length > 0) {
      return res.status(400).json({ success: false, message: 'Email is already registered' });
    }

    const id = 'rider_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const passwordHash = await bcrypt.hash(password, 10);

    await db.query(
      `INSERT INTO users (id, name, email, password_hash, phone, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
      [id, name, email.toLowerCase(), passwordHash, phone]
    );

    const token = generateToken({ id, email: email.toLowerCase(), role: 'rider', name });

    return res.status(201).json({
      success: true,
      message: 'Rider registered successfully',
      data: {
        token,
        user: { id, name, email: email.toLowerCase(), phone, role: 'rider' },
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Registration failed', error: error.message });
  }
}

export async function riderLogin(req: Request, res: Response) {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password are required' });
    }

    const userRes = await db.query('SELECT * FROM users WHERE email = $1', [email.toLowerCase()]);
    if (userRes.rows.length === 0) {
      return res.status(401).json({ success: false, message: 'Invalid email or password' });
    }

    const user = userRes.rows[0];
    if (user.is_active === 0) {
      return res.status(403).json({ success: false, message: 'Your rider account is suspended. Please contact support.' });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid email or password' });
    }

    const token = generateToken({ id: user.id, email: user.email, role: 'rider', name: user.name });

    return res.json({
      success: true,
      message: 'Login successful',
      data: {
        token,
        user: { id: user.id, name: user.name, email: user.email, phone: user.phone, role: 'rider', avatar_url: user.avatar_url },
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Login failed', error: error.message });
  }
}

export async function captainRegister(req: Request, res: Response) {
  try {
    const { name, email, password, phone, vehicle_type, vehicle_number } = req.body;
    if (!name || !email || !password || !phone || !vehicle_type || !vehicle_number) {
      return res.status(400).json({ success: false, message: 'All fields are required (name, email, password, phone, vehicle_type, vehicle_number)' });
    }

    const existing = await db.query('SELECT id FROM captains WHERE email = $1', [email.toLowerCase()]);
    if (existing.rows.length > 0) {
      return res.status(400).json({ success: false, message: 'Email is already registered' });
    }

    const id = 'capt_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
    const passwordHash = await bcrypt.hash(password, 10);

    const normalizedVehicleType = normalizeVehicleType(vehicle_type);

    await db.query(
      `INSERT INTO captains (id, name, email, password_hash, phone, vehicle_type, vehicle_number, kyc_status, is_online, rating, is_active, created_at, updated_at)
       VALUES ($1, $2, $3, $4, $5, $6, $7, 'pending', 0, 5.0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)`,
      [id, name, email.toLowerCase(), passwordHash, phone, normalizedVehicleType, vehicle_number.toUpperCase()]
    );

    const token = generateToken({ id, email: email.toLowerCase(), role: 'captain', name });

    return res.status(201).json({
      success: true,
      message: 'Captain registered successfully. Please submit KYC documents to begin taking rides.',
      data: {
        token,
        captain: {
          id,
          name,
          email: email.toLowerCase(),
          phone,
          vehicle_type: normalizedVehicleType,
          vehicle_number: vehicle_number.toUpperCase(),
          kyc_status: 'pending',
          is_online: false,
          role: 'captain',
        },
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Registration failed', error: error.message });
  }
}

export async function captainLogin(req: Request, res: Response) {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password are required' });
    }

    const captRes = await db.query('SELECT * FROM captains WHERE email = $1', [email.toLowerCase()]);
    if (captRes.rows.length === 0) {
      return res.status(401).json({ success: false, message: 'Invalid email or password' });
    }

    const captain = captRes.rows[0];
    if (captain.is_active === 0) {
      return res.status(403).json({ success: false, message: 'Your captain account is suspended. Please contact admin.' });
    }

    const isMatch = await bcrypt.compare(password, captain.password_hash);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid email or password' });
    }

    const token = generateToken({ id: captain.id, email: captain.email, role: 'captain', name: captain.name });

    return res.json({
      success: true,
      message: 'Login successful',
      data: {
        token,
        captain: {
          id: captain.id,
          name: captain.name,
          email: captain.email,
          phone: captain.phone,
          vehicle_type: captain.vehicle_type,
          vehicle_number: captain.vehicle_number,
          kyc_status: captain.kyc_status,
          admin_remarks: captain.admin_remarks,
          is_online: Boolean(captain.is_online),
          rating: captain.rating,
          total_rides: captain.total_rides,
          total_earnings: captain.total_earnings,
          payment_qr_url: captain.payment_qr_url,
          role: 'captain',
        },
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Login failed', error: error.message });
  }
}

export async function adminLogin(req: Request, res: Response) {
  try {
    const { email, password } = req.body;
    if (!email || !password) {
      return res.status(400).json({ success: false, message: 'Email and password are required' });
    }

    const adminRes = await db.query('SELECT * FROM admins WHERE email = $1', [email.toLowerCase()]);
    if (adminRes.rows.length === 0) {
      return res.status(401).json({ success: false, message: 'Invalid admin credentials' });
    }

    const admin = adminRes.rows[0];
    const isMatch = await bcrypt.compare(password, admin.password_hash);
    if (!isMatch) {
      return res.status(401).json({ success: false, message: 'Invalid admin credentials' });
    }

    const token = generateToken({ id: admin.id, email: admin.email, role: 'admin', name: admin.name });

    return res.json({
      success: true,
      message: 'Admin login successful',
      data: {
        token,
        admin: { id: admin.id, name: admin.name, email: admin.email, role: 'admin' },
      },
    });
  } catch (error: any) {
    return res.status(500).json({ success: false, message: 'Admin login failed', error: error.message });
  }
}
