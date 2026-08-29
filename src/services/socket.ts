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
  }
}
