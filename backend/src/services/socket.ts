import { Server as HttpServer } from 'http';
import { Server, Socket } from 'socket.io';
import jwt from 'jsonwebtoken';
import { db } from '../config/db';

const JWT_SECRET = process.env.JWT_SECRET || 'speedo_super_secret_jwt_key_rapido_2025_prod_safe';

let io: Server | null = null;

export interface SocketUser {
  id: string;
  email: string;
  role: 'rider' | 'captain' | 'admin';
}

export function initSocketServer(httpServer: HttpServer): Server {
  io = new Server(httpServer, {
    cors: {
      origin: '*',
      methods: ['GET', 'POST'],
    },
    pingInterval: 10000,
    pingTimeout: 5000,
  });

  // JWT Authentication Middleware for Sockets
  io.use((socket: Socket, next) => {
    const token =
      socket.handshake.auth?.token ||
      socket.handshake.headers?.authorization?.replace('Bearer ', '');

    if (!token) {
      // Allow unauthenticated connection for public tracker if needed, or attach guest
      (socket as any).user = null;
      return next();
    }

    try {
      const decoded = jwt.verify(token, JWT_SECRET) as SocketUser;
      (socket as any).user = decoded;
      next();
    } catch (err: any) {
      console.warn(`[Socket Auth Warning] Invalid token: ${err.message}`);
      // Don't kill socket, just assign guest
      (socket as any).user = null;
      next();
    }
  });

  io.on('connection', (socket: Socket) => {
    const user: SocketUser | null = (socket as any).user;
    const userId = user?.id || socket.id;
    const userRole = user?.role || 'guest';

    console.log(`⚡ [Socket Connected] ${socket.id} (User: ${userId}, Role: ${userRole})`);

    // Join user's personal room & role room
    if (user) {
      socket.join(`user_${user.id}`);
      socket.join(`role_${user.role}`);
    }

    // 1. Join Active Ride Room
    socket.on('ride:join', (data: { rideId: string }) => {
      if (data?.rideId) {
        socket.join(`ride_${data.rideId}`);
        console.log(`📌 Socket ${socket.id} joined room ride_${data.rideId}`);
      }
    });

    socket.on('ride:leave', (data: { rideId: string }) => {
      if (data?.rideId) {
        socket.leave(`ride_${data.rideId}`);
        console.log(`👋 Socket ${socket.id} left room ride_${data.rideId}`);
      }
    });

    // 2. High-Frequency Live GPS Location Streaming (Captain -> Server -> Ride Room & Admin)
    socket.on('captain:location_update', async (data: {
      lat: number;
      lng: number;
      bearing?: number;
      speed?: number;
      isOnline?: boolean;
      activeRideId?: string;
    }) => {
      if (!user || user.role !== 'captain') return;

      const { lat, lng, bearing = 0, speed = 0, isOnline = true, activeRideId } = data;

      // Broadcast immediately with sub-second latency to active ride room (Rider)
      if (activeRideId) {
        io?.to(`ride_${activeRideId}`).emit('ride:location_broadcast', {
          captainId: user.id,
          lat,
          lng,
          bearing,
          speed,
          timestamp: Date.now(),
        });
      }

      // Broadcast to live fleet map (Admin)
      io?.to('role_admin').emit('admin:captain_location', {
        captainId: user.id,
        lat,
        lng,
        bearing,
        speed,
        isOnline,
        timestamp: Date.now(),
      });

      // Asynchronously update DB location cache (non-blocking)
      try {
        await db.query(
          `INSERT INTO locations (captain_id, lat, lng, bearing, speed, is_online, updated_at)
           VALUES ($1, $2, $3, $4, $5, $6, CURRENT_TIMESTAMP)
           ON CONFLICT(captain_id) DO UPDATE SET
             lat = EXCLUDED.lat,
             lng = EXCLUDED.lng,
             bearing = EXCLUDED.bearing,
             speed = EXCLUDED.speed,
             is_online = EXCLUDED.is_online,
             updated_at = CURRENT_TIMESTAMP`,
          [user.id, lat, lng, bearing, speed, isOnline ? 1 : 0]
        );
      } catch (err: any) {
        // Suppress DB conflict logs during rapid socket streaming
      }
    });

    // 3. Emergency SOS Trigger via WebSocket
    socket.on('sos:trigger', async (data: { ride_id?: string; lat: number; lng: number; address?: string }) => {
      try {
        const sosId = 'sos_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
        const { ride_id, lat = 12.9716, lng = 77.5946, address = 'Live GPS Coordinates' } = data || {};

        let userName = user ? `${user.role.toUpperCase()} User` : 'Emergency Caller';
        let userPhone = '9876543210';
        let userRole = user?.role || 'rider';
        let captainId: string | null = null;
        let captainName: string | null = null;
        let captainPhone: string | null = null;
        let vehicleNum: string | null = null;

        if (user) {
          if (user.role === 'captain') {
            const cRes = await db.query('SELECT name, phone, vehicle_number FROM captains WHERE id = $1', [user.id]);
            if (cRes.rows.length > 0) {
              userName = cRes.rows[0].name;
              userPhone = cRes.rows[0].phone;
              captainId = user.id;
              captainName = userName;
              captainPhone = userPhone;
              vehicleNum = cRes.rows[0].vehicle_number;
            }
          } else {
            const uRes = await db.query('SELECT name, phone FROM users WHERE id = $1', [user.id]);
            if (uRes.rows.length > 0) {
              userName = uRes.rows[0].name;
              userPhone = uRes.rows[0].phone;
            }
          }
        }

        if (ride_id) {
          const rRes = await db.query('SELECT r.*, c.name as c_name, c.phone as c_phone, c.vehicle_number FROM rides r LEFT JOIN captains c ON r.captain_id = c.id WHERE r.id = $1', [ride_id]);
          if (rRes.rows.length > 0) {
            const r = rRes.rows[0];
            captainId = r.captain_id;
            captainName = r.c_name;
            captainPhone = r.c_phone;
            vehicleNum = r.vehicle_number;
          }
        }

        await db.query(
          `INSERT INTO sos_alerts (id, ride_id, triggered_by, user_id, user_name, user_phone, captain_id, captain_name, captain_phone, vehicle_number, lat, lng, address, status)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, 'active')`,
          [sosId, ride_id || null, userRole, user?.id || 'guest', userName, userPhone, captainId, captainName, captainPhone, vehicleNum, lat, lng, address]
        );

        const alertPayload = {
          id: sosId,
          ride_id: ride_id || null,
          triggered_by: userRole,
          user_id: user?.id || 'guest',
          user_name: userName,
          user_phone: userPhone,
          captain_id: captainId,
          captain_name: captainName,
          captain_phone: captainPhone,
          vehicle_number: vehicleNum,
          lat,
          lng,
          address,
          status: 'active',
          created_at: new Date().toISOString(),
        };

        emitSosAlert(alertPayload);
      } catch (err: any) {
        console.error('❌ Error handling sos:trigger socket event:', err.message);
      }
    });

    socket.on('disconnect', () => {
      console.log(`🔌 [Socket Disconnected] ${socket.id} (User: ${userId})`);
    });
  });

  return io;
}

export function getIO(): Server {
  if (!io) {
    throw new Error('Socket.io server has not been initialized yet!');
  }
  return io;
}

/**
 * Emit real-time events to specific ride or role
 */
export function emitRideEvent(rideId: string, eventName: string, payload: any) {
  if (io) {
    io.to(`ride_${rideId}`).emit(eventName, payload);
    // Also notify admins
    io.to('role_admin').emit(eventName, { rideId, ...payload });
  }
}

export function emitToCaptains(eventName: string, payload: any) {
  if (io) {
    io.to('role_captain').emit(eventName, payload);
    io.emit(eventName, payload); // Global fallback broadcast to ensure instant UI popups
  }
}

export function emitToUser(userId: string, eventName: string, payload: any) {
  if (io) {
    io.to(`user_${userId}`).emit(eventName, payload);
    io.emit(eventName, { targetUserId: userId, ...payload }); // fallback
  }
}

export function emitSosAlert(payload: any) {
  if (io) {
    console.log(`🚨 [SOCKET SOS EMIT] Broadcasting emergency alert ${payload.id} to Admin Command Center`);
    io.to('role_admin').emit('admin:sos_alert', payload);
    io.emit('admin:sos_alert', payload);
  }
}

export function emitSosResolved(payload: any) {
  if (io) {
    console.log(`✅ [SOCKET SOS RESOLVED] Broadcasting resolution for alert ${payload.id}`);
    io.to('role_admin').emit('admin:sos_resolved', payload);
    io.emit('admin:sos_resolved', payload);
  }
}

export function emitKycStatus(captainId: string, payload: any) {
  if (io) {
    console.log(`📑 [SOCKET KYC STATUS] Emitting status to captain ${captainId}:`, payload);
    io.to(`user_${captainId}`).emit('captain:kyc_status', payload);
    io.emit('captain:kyc_status', { captainId, ...payload });
    io.to('role_admin').emit('admin:kyc_queue_updated', { captainId, ...payload });
  }
}

export function emitSurgeUpdate(payload: any) {
  if (io) {
    console.log(`⚡ [SOCKET SURGE UPDATE] Emitting surge zones change:`, payload);
    io.to('role_rider').emit('surge:zones_updated', payload);
    io.to('role_admin').emit('surge:zones_updated', payload);
    io.emit('surge:zones_updated', payload);
  }
}

export function emitBroadcast(payload: any) {
  if (io) {
    console.log(`📢 [SOCKET BROADCAST] Emitting city broadcast ${payload.id}:`, payload.title);
    if (payload.target_audience === 'riders' || payload.target_audience === 'all') {
      io.to('role_rider').emit('broadcast:announcement', payload);
    }
    if (payload.target_audience === 'captains' || payload.target_audience === 'all') {
      io.to('role_captain').emit('broadcast:announcement', payload);
    }
    io.to('role_admin').emit('broadcast:announcement', payload);
    io.emit('broadcast:announcement', payload);
  }
}
