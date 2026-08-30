import http from 'http';
import express from 'express';
import cors from 'cors';
import path from 'path';
import dotenv from 'dotenv';
import authRoutes from './routes/auth.routes';
import riderRoutes from './routes/rider.routes';
import captainRoutes from './routes/captain.routes';
import adminRoutes from './routes/admin.routes';
import chatRoutes from './routes/chat.routes';
import supportRoutes from './routes/support.routes';
import sosRoutes from './routes/sos.routes';
import { runMigrations } from './database/migrate';
import { initSocketServer } from './services/socket';

dotenv.config();

const app = express();
const httpServer = http.createServer(app);
const PORT = Number(process.env.PORT) || 5000;

// Initialize WebSocket Engine
initSocketServer(httpServer);

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Serve uploaded KYC documents, selfies, and payment QR codes statically
const uploadDir = path.resolve(__dirname, '../uploads');
app.use('/uploads', express.static(uploadDir));

// Request logging middleware
app.use((req, _res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.originalUrl}`);
  next();
});

// Health check endpoint (Root, /health, /api/health)
app.get(['/', '/health', '/api/health'], (_req, res) => {
  res.json({
    status: 'healthy',
    platform: 'Speedo Centralized Ride-Hailing Backend',
    websocket: 'Socket.io Enabled (Sub-second GPS Streaming & In-App Chat)',
    timestamp: new Date().toISOString(),
    version: '1.2.0',
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/rider', riderRoutes);
app.use('/api/captain', captainRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/chat', chatRoutes);
app.use('/api/support', supportRoutes);
app.use('/api/sos', sosRoutes);

// 404 handler
app.use((_req, res) => {
  res.status(404).json({ success: false, message: 'Endpoint not found' });
});

// Global error handler
app.use((err: any, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  console.error(' Server Error:', err);
  res.status(500).json({
    success: false,
    message: err.message || 'Internal server error',
  });
});

// Start Server after running schema migrations and seeds
async function startServer() {
  try {
    await runMigrations();
    try {
      const { seedDatabase } = await import('./seed/seed');
      await seedDatabase();
    } catch (seedErr: any) {
      console.log('🌱 Seed info:', seedErr.message);
    }

    httpServer.listen(PORT, '0.0.0.0', () => {
      console.log(`====================================================`);
      console.log(` SPEEDO REAL-TIME BACKEND & SOCKETS RUNNING ON ${PORT}`);
      console.log(` Health Check: http://0.0.0.0:${PORT}/health`);
      console.log(` API Base:     http://0.0.0.0:${PORT}/api`);
      console.log(` WebSockets:   ws://0.0.0.0:${PORT}/socket.io/`);
      console.log(` Static Files: http://0.0.0.0:${PORT}/uploads`);
      console.log(`====================================================`);
    });
  } catch (err) {
    console.error(' Failed to start server:', err);
    process.exit(1);
  }
}

if (require.main === module) {
  startServer();
}

export default app;
