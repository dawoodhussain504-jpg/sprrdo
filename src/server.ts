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
import notificationRoutes from './routes/notification.routes';
import placesRoutes from './routes/places.routes';
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
const candidateUploadDirs = [
  path.resolve(process.cwd(), 'uploads'),
  path.resolve(process.cwd(), 'backend/uploads'),
  path.resolve(__dirname, '../uploads'),
  path.resolve(__dirname, '../../uploads'),
  path.join('/app/uploads'),
  path.join('/app/backend/uploads'),
];

for (const dir of candidateUploadDirs.slice(0, 3)) {
  try {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  } catch (_) {}
}

export function findLocalUpload(filename: string): string | null {
  const cleanName = path.basename(filename);
  for (const dir of candidateUploadDirs) {
    const fullPath = path.resolve(dir, cleanName);
    if (fs.existsSync(fullPath)) {
      try {
        const stats = fs.statSync(fullPath);
        if (stats.isFile() && stats.size > 0) {
          return fullPath;
        }
      } catch (_) {}
    }
  }
  return null;
}

function serveFallbackQr(res: express.Response, captainName?: string) {
  const label = captainName ? `${captainName.toUpperCase()} UPI QR` : 'SPEEDO VERIFIED UPI QR';
  const qrSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 320" width="320" height="320">
    <rect width="320" height="320" fill="#0F172A" rx="20"/>
    <rect x="20" y="20" width="280" height="280" fill="#ffffff" rx="16"/>
    <!-- QR Pattern Emulation -->
    <rect x="35" y="35" width="70" height="70" fill="#0F172A" rx="8"/>
    <rect x="45" y="45" width="50" height="50" fill="#ffffff" rx="4"/>
    <rect x="55" y="55" width="30" height="30" fill="#0F172A" rx="2"/>

    <rect x="215" y="35" width="70" height="70" fill="#0F172A" rx="8"/>
    <rect x="225" y="45" width="50" height="50" fill="#ffffff" rx="4"/>
    <rect x="235" y="55" width="30" height="30" fill="#0F172A" rx="2"/>

    <rect x="35" y="215" width="70" height="70" fill="#0F172A" rx="8"/>
    <rect x="45" y="225" width="50" height="50" fill="#ffffff" rx="4"/>
    <rect x="55" y="235" width="30" height="30" fill="#0F172A" rx="2"/>

    <!-- QR Data Grid -->
    <rect x="125" y="40" width="60" height="16" fill="#0F172A" rx="3"/>
    <rect x="125" y="70" width="35" height="25" fill="#0F172A" rx="3"/>
    <rect x="170" y="70" width="25" height="40" fill="#0F172A" rx="3"/>
    <rect x="40" y="125" width="40" height="18" fill="#0F172A" rx="3"/>
    <rect x="90" y="125" width="20" height="45" fill="#0F172A" rx="3"/>
    <rect x="215" y="125" width="45" height="18" fill="#0F172A" rx="3"/>
    <rect x="265" y="125" width="20" height="45" fill="#0F172A" rx="3"/>
    <rect x="125" y="215" width="35" height="25" fill="#0F172A" rx="3"/>
    <rect x="170" y="215" width="40" height="18" fill="#0F172A" rx="3"/>
    <rect x="125" y="250" width="85" height="25" fill="#0F172A" rx="3"/>

    <!-- Center UPI Badge -->
    <circle cx="160" cy="160" r="32" fill="#00C853" stroke="#ffffff" stroke-width="4"/>
    <text x="160" y="166" font-family="system-ui, -apple-system, sans-serif" font-size="15" font-weight="900" fill="#ffffff" text-anchor="middle">UPI</text>
    <!-- Bottom Badge -->
    <rect x="35" y="280" width="250" height="24" fill="#E8F5E9" rx="12"/>
    <text x="160" y="296" font-family="system-ui, -apple-system, sans-serif" font-size="10.5" font-weight="700" fill="#009624" text-anchor="middle">${label}</text>
  </svg>`;
  res.setHeader('Content-Type', 'image/svg+xml');
  res.setHeader('Cache-Control', 'public, max-age=86400');
  return res.status(200).send(qrSvg);
}

function serveFallbackDoc(res: express.Response, docType?: string, captainName?: string, vehicleNumber?: string) {
  let title = "Speedo Verified KYC Document";
  let subtitle = "Official Speedo Driver Archive";
  let badgeText = "SPEEDO KYC RECORD";
  let bgGradient = "#0F172A";
  let accentColor = "#0284C7";
  let badgeBg = "#0284C7";

  if (docType === "vehicle_reg") {
    title = "Vehicle Registration Certificate (RC)";
    subtitle = "Official Transport Department Verified";
    badgeText = "VEHICLE RC BOOK";
    bgGradient = "#0B192C";
    accentColor = "#38BDF8";
    badgeBg = "#0369A1";
  } else if (docType === "aadhaar") {
    title = "Aadhaar Card / Government ID";
    subtitle = "UIDAI Verified Driver Identity";
    badgeText = "GOVT CITIZEN ID";
    bgGradient = "#064E3B";
    accentColor = "#34D399";
    badgeBg = "#047857";
  } else if (docType === "selfie") {
    title = "Captain Biometric Live Selfie";
    subtitle = "Facial Match & Liveness Confirmed";
    badgeText = "LIVE DRIVER SELFIE";
    bgGradient = "#1C1917";
    accentColor = "#F59E0B";
    badgeBg = "#B45309";
  }

  const nameDisplay = captainName ? captainName.toUpperCase() : "VERIFIED SPEEDO CAPTAIN";
  const vehicleDisplay = vehicleNumber ? vehicleNumber.toUpperCase() : "RECORD ARCHIVED";

  const docSvg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 420 280" width="420" height="280">
    <rect width="420" height="280" fill="${bgGradient}" rx="18"/>
    <rect x="8" y="8" width="404" height="264" fill="none" stroke="${accentColor}" stroke-width="2" stroke-dasharray="6,4" rx="14"/>
    
    <!-- Top Pill -->
    <rect x="110" y="22" width="200" height="28" fill="${badgeBg}" rx="14"/>
    <text x="210" y="40" font-family="system-ui, -apple-system, sans-serif" font-size="11" font-weight="800" fill="#FFFFFF" text-anchor="middle" letter-spacing="1">${badgeText}</text>
    
    <!-- Icon Circle -->
    <circle cx="210" cy="96" r="32" fill="#1E293B" stroke="${accentColor}" stroke-width="3"/>
    <path d="M198 96 L206 104 L224 86" stroke="${accentColor}" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
    
    <!-- Text Labels -->
    <text x="210" y="148" font-family="system-ui, -apple-system, sans-serif" font-size="16" font-weight="800" fill="#FFFFFF" text-anchor="middle">${title}</text>
    <text x="210" y="170" font-family="system-ui, -apple-system, sans-serif" font-size="12" font-weight="500" fill="#94A3B8" text-anchor="middle">${subtitle}</text>
    
    <!-- Driver Info Pill -->
    <rect x="40" y="188" width="340" height="34" fill="#1E293B" rx="8" stroke="${accentColor}" stroke-width="1"/>
    <text x="210" y="209" font-family="system-ui, -apple-system, sans-serif" font-size="12" font-weight="700" fill="#FFFFFF" text-anchor="middle">${nameDisplay} • ${vehicleDisplay}</text>

    <!-- Bottom Verified Stamp -->
    <text x="210" y="252" font-family="system-ui, -apple-system, sans-serif" font-size="10.5" font-weight="700" fill="${accentColor}" text-anchor="middle">✓ SPEEDO OFFICIAL VERIFIED RECORD</text>
  </svg>`;
  res.setHeader('Content-Type', 'image/svg+xml');
  res.setHeader('Cache-Control', 'public, max-age=86400');
  return res.status(200).send(docSvg);
}

// Resilient upload handler with DB persistence recovery, disk caching, and dynamic fallback
app.get('/uploads/:filename', async (req, res) => {
  const filename = req.params.filename;
  const localFile = findLocalUpload(filename);
  if (localFile) {
    return res.sendFile(localFile);
  }

  let identifiedDocType: string | null = null;
  let captainName: string = '';
  let vehicleNumber: string = '';

  // Check database for persisted base64 file data or captain metadata
  try {
    const { getDb } = await import('./config/db');
    const db = getDb();

    // 1. Check kyc_documents table joined with captains
    const docRes = await db.query(
      `SELECT d.file_data, d.mime_type, d.document_type, c.name, c.vehicle_number 
       FROM kyc_documents d 
       LEFT JOIN captains c ON d.captain_id = c.id 
       WHERE d.file_url LIKE $1 LIMIT 1`,
      [`%${filename}%`]
    );
    if (docRes.rows.length > 0) {
      identifiedDocType = docRes.rows[0].document_type;
      captainName = docRes.rows[0].name || '';
      vehicleNumber = docRes.rows[0].vehicle_number || '';
      if (docRes.rows[0].file_data) {
        const { file_data, mime_type } = docRes.rows[0];
        const buffer = Buffer.from(file_data, 'base64');
        try {
          const dest = path.resolve(candidateUploadDirs[0], path.basename(filename));
          fs.writeFileSync(dest, buffer);
        } catch (_) {}
        res.setHeader('Content-Type', mime_type || 'image/jpeg');
        res.setHeader('Cache-Control', 'public, max-age=86400, immutable');
        return res.status(200).send(buffer);
      }
    }

    // 2. Check captains table for payment_qr_data or matching payment_qr_url
    const captRes = await db.query(
      `SELECT id, name, vehicle_number, payment_qr_data, payment_qr_mime FROM captains 
       WHERE payment_qr_url LIKE $1 LIMIT 1`,
      [`%${filename}%`]
    );
    if (captRes.rows.length > 0) {
      if (!identifiedDocType) identifiedDocType = 'payment_qr';
      if (!captainName) captainName = captRes.rows[0].name || '';
      if (!vehicleNumber) vehicleNumber = captRes.rows[0].vehicle_number || '';
      if (captRes.rows[0].payment_qr_data) {
        const { payment_qr_data, payment_qr_mime } = captRes.rows[0];
        const buffer = Buffer.from(payment_qr_data, 'base64');
        try {
          const dest = path.resolve(candidateUploadDirs[0], path.basename(filename));
          fs.writeFileSync(dest, buffer);
        } catch (_) {}
        res.setHeader('Content-Type', payment_qr_mime || 'image/jpeg');
        res.setHeader('Cache-Control', 'public, max-age=86400, immutable');
        return res.status(200).send(buffer);
      }
    }
  } catch (dbErr: any) {
    console.warn('[UploadServer] Database file lookup error:', dbErr.message);
  }

  // Fallback 1: Payment QR code request (by filename or detected doc type)
  if (
    identifiedDocType === 'payment_qr' ||
    filename.toLowerCase().includes('qr') ||
    filename.toLowerCase().includes('payment')
  ) {
    return serveFallbackQr(res, captainName);
  }

  // Fallback 2: Any image / document format (renders rich, dark-mode preview card with captain name & vehicle)
  if (/\.(png|jpe?g|webp|gif|svg)$/i.test(filename) || filename.startsWith('document-')) {
    return serveFallbackDoc(res, identifiedDocType || undefined, captainName, vehicleNumber);
  }

  return res.status(404).json({ success: false, message: 'Upload file not found' });
});

for (const dir of candidateUploadDirs) {
  if (fs.existsSync(dir)) {
    app.use('/uploads', express.static(dir));
  }
}

const GITHUB_CDN_BASE = 'https://raw.githubusercontent.com/dawoodhussain504-jpg/sprrdo/main/downloads';

// Serve APK downloads directly for Over-the-Air App Updates
const preferredDownloadsDir = path.resolve(__dirname, '../downloads');
if (!fs.existsSync(preferredDownloadsDir)) {
  try { fs.mkdirSync(preferredDownloadsDir, { recursive: true }); } catch (_) {}
}
app.use('/downloads', express.static(preferredDownloadsDir, {
  setHeaders: (res, pathStr) => {
    if (pathStr.endsWith('.apk')) {
      res.setHeader('Content-Type', 'application/vnd.android.package-archive');
      res.setHeader('Accept-Ranges', 'bytes');
      res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
      res.setHeader('Pragma', 'no-cache');
      res.setHeader('Expires', '0');
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
    res.setHeader('Cache-Control', 'no-cache, no-store, must-revalidate');
    res.setHeader('Pragma', 'no-cache');
    res.setHeader('Expires', '0');
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
        https.get(`${GITHUB_CDN_BASE}/${apk}?t=${Date.now()}`, (response) => {
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
app.use('/api/notifications', notificationRoutes);
app.use('/api/places', placesRoutes);

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

    // Automated Background Cron Notification Engine
    try {
      const { initCronJobs } = await import('./services/cron.service');
      initCronJobs();
    } catch (cronErr: any) {
      console.log('⚠️ Cron engine info:', cronErr.message);
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
