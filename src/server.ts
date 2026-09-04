import { getPopularDestinationsPublic } from './controllers/destination.controller';
import { getAppVersionConfig } from './controllers/version.controller';
import http from 'http';
import express from 'express';
import cors from 'cors';
import fs from 'fs';
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
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true });
}
app.use('/uploads', express.static(uploadDir));

const GITHUB_CDN_BASE = 'https://raw.githubusercontent.com/dawoodhussain504-jpg/sprrdo/main/downloads';

// Serve APK downloads directly for Over-the-Air App Updates
const downloadsDir = path.resolve(__dirname, '../downloads');
if (!fs.existsSync(downloadsDir)) {
  fs.mkdirSync(downloadsDir, { recursive: true });
}
app.use('/downloads', express.static(downloadsDir));

// Fallback for direct APK requests if not stored locally in container
app.get('/downloads/speedo-rider.apk', (_req, res) => {
  const file = path.join(downloadsDir, 'speedo-rider.apk');
  if (fs.existsSync(file)) {
    res.download(file, 'speedo-rider.apk');
  } else {
    res.redirect(`${GITHUB_CDN_BASE}/speedo-rider.apk`);
  }
});

app.get('/downloads/speedo-captain.apk', (_req, res) => {
  const file = path.join(downloadsDir, 'speedo-captain.apk');
  if (fs.existsSync(file)) {
    res.download(file, 'speedo-captain.apk');
  } else {
    res.redirect(`${GITHUB_CDN_BASE}/speedo-captain.apk`);
  }
});

app.get('/downloads/speedo-admin.apk', (_req, res) => {
  const file = path.join(downloadsDir, 'speedo-admin.apk');
  if (fs.existsSync(file)) {
    res.download(file, 'speedo-admin.apk');
  } else {
    res.redirect(`${GITHUB_CDN_BASE}/speedo-admin.apk`);
  }
});

// Direct download shortcuts
app.get('/download/rider', (_req, res) => {
  const file = path.join(downloadsDir, 'speedo-rider.apk');
  if (fs.existsSync(file)) {
    res.download(file, 'speedo-rider.apk');
  } else {
    res.redirect(`${GITHUB_CDN_BASE}/speedo-rider.apk`);
  }
});

app.get('/download/captain', (_req, res) => {
  const file = path.join(downloadsDir, 'speedo-captain.apk');
  if (fs.existsSync(file)) {
    res.download(file, 'speedo-captain.apk');
  } else {
    res.redirect(`${GITHUB_CDN_BASE}/speedo-captain.apk`);
  }
});

app.get('/download/admin', (_req, res) => {
  const file = path.join(downloadsDir, 'speedo-admin.apk');
  if (fs.existsSync(file)) {
    res.download(file, 'speedo-admin.apk');
  } else {
    res.redirect(`${GITHUB_CDN_BASE}/speedo-admin.apk`);
  }
});

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
app.get('/api/destinations', getPopularDestinationsPublic);
app.get('/api/destinations/popular', getPopularDestinationsPublic);
app.get('/api/app-version', getAppVersionConfig);
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

    // Ensure database app version configs align with built binaries (v3), breaking update loops
    try {
      const { getDb } = await import('./config/db');
      const db = getDb();
      await db.query(`
        CREATE TABLE IF NOT EXISTS schema_patches (
          patch_name VARCHAR(64) PRIMARY KEY,
          applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
      `);
      const patchCheck = await db.query(
        "SELECT * FROM schema_patches WHERE patch_name = 'patch_v3_loop_fix'"
      );
      if (patchCheck.rows.length === 0) {
        await db.query(`
          UPDATE app_version_configs 
          SET latest_version_code = 3, latest_version_name = '1.0.3', min_supported_version_code = 1, force_update = 0 
          WHERE app_id IN ('rider', 'captain', 'admin');

          UPDATE notifications 
          SET is_read = 1 
          WHERE type = 'app_update';

          INSERT INTO schema_patches (patch_name) VALUES ('patch_v3_loop_fix');
        `);
        console.log('✅ Applied patch_v3_loop_fix: App versions aligned to 3 (1.0.3) & old update notifications cleared.');
      }
    } catch (patchErr: any) {
      console.log('⚠️ Patch info:', patchErr.message);
    }

    // Automated App Version Synchronization & Broadcast
    try {
      const { syncAppVersions } = await import('./services/app-version-sync.service');
      await syncAppVersions(true);
    } catch (syncErr: any) {
      console.log('⚠️ App version sync info:', syncErr.message);
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
