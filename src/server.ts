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
const preferredDownloadsDir = path.resolve(__dirname, '../downloads');
if (!fs.existsSync(preferredDownloadsDir)) {
  try { fs.mkdirSync(preferredDownloadsDir, { recursive: true }); } catch (_) {}
}
app.use('/downloads', express.static(preferredDownloadsDir, {
  maxAge: '1d',
  setHeaders: (res, pathStr) => {
    if (pathStr.endsWith('.apk')) {
      res.setHeader('Content-Type', 'application/vnd.android.package-archive');
      res.setHeader('Accept-Ranges', 'bytes');
      res.setHeader('Cache-Control', 'public, max-age=86400, immutable');
    }
  }
}));

/**
 * Searches all candidate paths where APK files might reside inside container
 */
function findLocalApk(filename: string): string | null {
  const candidates = [
    path.resolve(__dirname, '../downloads', filename),
    path.resolve(__dirname, '../../downloads', filename),
    path.resolve(__dirname, '../../../downloads', filename),
    path.resolve(process.cwd(), 'downloads', filename),
    path.resolve(process.cwd(), 'backend/downloads', filename),
    path.join('/app/downloads', filename),
    path.join('/app/backend/downloads', filename),
  ];
  for (const c of candidates) {
    if (fs.existsSync(c)) {
      try {
        const stats = fs.statSync(c);
        if (stats.size > 5 * 1024 * 1024) { // Valid APK > 5MB
          return c;
        }
      } catch (_) {}
    }
  }
  return null;
}

/**
 * High-speed APK streaming handler with full HTTP Range (206) and zero-copy sendfile
 */
function handleApkDownload(filename: string, _req: express.Request, res: express.Response) {
  const localFile = findLocalApk(filename);
  if (localFile) {
    res.setHeader('Content-Type', 'application/vnd.android.package-archive');
    res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
    res.setHeader('Accept-Ranges', 'bytes');
    res.setHeader('Cache-Control', 'public, max-age=86400, immutable');
    return res.sendFile(localFile);
  }

  // Fallback: If not found on local disk, redirect to GitHub
  console.warn(`[ApkServer] Local APK not found for ${filename}, falling back to remote CDN`);
  return res.redirect(`${GITHUB_CDN_BASE}/${filename}`);
}

// Direct & legacy routes with HEAD and GET support
const apkRoutes = [
  { route: '/downloads/speedo-rider.apk', file: 'speedo-rider.apk' },
  { route: '/downloads/speedo-captain.apk', file: 'speedo-captain.apk' },
  { route: '/downloads/speedo-admin.apk', file: 'speedo-admin.apk' },
  { route: '/download/rider', file: 'speedo-rider.apk' },
  { route: '/download/captain', file: 'speedo-captain.apk' },
  { route: '/download/admin', file: 'speedo-admin.apk' },
];

for (const { route, file } of apkRoutes) {
  app.get(route, (req, res) => handleApkDownload(file, req, res));
  app.head(route, (req, res) => handleApkDownload(file, req, res));
}

// Background pre-warmer: Ensures APKs are cached locally on Railway SSD disk
export async function prewarmApkStorage() {
  const apks = ['speedo-rider.apk', 'speedo-captain.apk', 'speedo-admin.apk'];
  for (const apk of apks) {
    const existing = findLocalApk(apk);
    if (!existing) {
      const dest = path.join(preferredDownloadsDir, apk);
      console.log(`[ApkPrewarm] Downloading ${apk} from GitHub into local cache: ${dest}...`);
      try {
        const https = await import('https');
        const fileStream = fs.createWriteStream(dest);
        https.get(`${GITHUB_CDN_BASE}/${apk}`, (response) => {
          if (response.statusCode === 200) {
            response.pipe(fileStream);
            fileStream.on('finish', () => {
              fileStream.close();
              try {
                console.log(`🚀 [ApkPrewarm] Successfully cached ${apk} locally (${fs.statSync(dest).size} bytes)`);
              } catch (_) {}
            });
          } else {
            fileStream.close();
            try { fs.unlinkSync(dest); } catch (_) {}
          }
        }).on('error', (err) => {
          fileStream.close();
          try { fs.unlinkSync(dest); } catch (_) {}
          console.warn(`[ApkPrewarm] Error caching ${apk}:`, err.message);
        });
      } catch (e: any) {
        console.warn(`[ApkPrewarm] Exception caching ${apk}:`, e.message);
      }
    } else {
      console.log(`✅ [ApkPrewarm] ${apk} is present locally: ${existing}`);
    }
  }
}

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
app.all(['/api/app-version/sync-now', '/api/app-version/publish-update'], async (_req, res) => {
  try {
    const { forceSyncAppVersions } = await import('./services/app-version-sync.service');
    const result = await forceSyncAppVersions();
    res.status(200).json(result);
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});
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

    // Automated App Version Synchronization & Background Watcher
    try {
      const { syncAppVersions, startAppVersionWatcher } = await import('./services/app-version-sync.service');
      await syncAppVersions(true);
      startAppVersionWatcher(30000);
      prewarmApkStorage().catch((e) => console.log('⚠️ Prewarm error:', e.message));
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
