import fs from 'fs';
import path from 'path';
import { getDb } from '../config/db';
import { emitAppVersionUpdated } from './socket';

interface AppVersionDef {
  versionCode: number;
  versionName: string;
  title: string;
  message: string;
  forceUpdate?: boolean;
  updateUrl?: string;
}

const DEFAULT_VERSIONS: Record<string, AppVersionDef> = {
  rider: {
    versionCode: 3,
    versionName: '1.0.3',
    title: 'Speedo Rider Update Available 🚀',
    message: 'A new version of Speedo Rider is ready with the latest improvements. Update now or choose later.',
    forceUpdate: false,
  },
  captain: {
    versionCode: 3,
    versionName: '1.0.3',
    title: 'Speedo Captain Update Available 🚀',
    message: 'A new version of Speedo Captain is ready with the latest improvements. Update now or choose later.',
    forceUpdate: false,
  },
  admin: {
    versionCode: 3,
    versionName: '1.0.3',
    title: 'Speedo Admin Update Available 🚀',
    message: 'A new version of Speedo Admin is ready with the latest improvements. Update now or choose later.',
    forceUpdate: false,
  },
};

let lastSyncTimestamp = 0;
const SYNC_DEBOUNCE_MS = 15000; // 15 seconds debounce

/**
 * Reads version definitions from app-versions.json or android build.gradle.kts
 */
export function getDefinedAppVersions(): Record<string, AppVersionDef> {
  const versions: Record<string, AppVersionDef> = { ...DEFAULT_VERSIONS };

  // 1. Try reading app-versions.json from possible locations
  const jsonCandidates = [
    path.resolve(__dirname, '../../app-versions.json'),
    path.resolve(__dirname, '../../../app-versions.json'),
    path.resolve(__dirname, '../app-versions.json'),
    path.resolve(process.cwd(), 'app-versions.json'),
  ];

  for (const candidate of jsonCandidates) {
    if (fs.existsSync(candidate)) {
      try {
        const raw = fs.readFileSync(candidate, 'utf8');
        const parsed = JSON.parse(raw);
        for (const key of ['rider', 'captain', 'admin']) {
          if (parsed[key] && typeof parsed[key].versionCode === 'number') {
            versions[key] = {
              ...versions[key],
              ...parsed[key],
            };
          }
        }
        break;
      } catch (e: any) {
        console.warn(`[AppVersionSync] Error reading ${candidate}:`, e.message);
      }
    }
  }

  // 2. Check build.gradle.kts files if present on system
  const appGradleMap: Record<string, string[]> = {
    rider: [
      path.resolve(process.cwd(), 'android/rider-app/build.gradle.kts'),
      path.resolve(__dirname, '../../../android/rider-app/build.gradle.kts'),
    ],
    captain: [
      path.resolve(process.cwd(), 'android/captain-app/build.gradle.kts'),
      path.resolve(__dirname, '../../../android/captain-app/build.gradle.kts'),
    ],
    admin: [
      path.resolve(process.cwd(), 'android/admin-app/build.gradle.kts'),
      path.resolve(__dirname, '../../../android/admin-app/build.gradle.kts'),
    ],
  };

  for (const [appId, paths] of Object.entries(appGradleMap)) {
    for (const p of paths) {
      if (fs.existsSync(p)) {
        try {
          const content = fs.readFileSync(p, 'utf8');
          const codeMatch = content.match(/versionCode\s*=\s*(\d+)/);
          const nameMatch = content.match(/versionName\s*=\s*"([^"]+)"/);
          if (codeMatch && codeMatch[1]) {
            const parsedCode = parseInt(codeMatch[1], 10);
            if (parsedCode > versions[appId].versionCode) {
              versions[appId].versionCode = parsedCode;
              if (nameMatch && nameMatch[1]) {
                versions[appId].versionName = nameMatch[1];
              }
            }
          }
        } catch (_) {}
      }
    }
  }

  return versions;
}

/**
 * Synchronizes database app_version_configs with defined app versions.
 * If a newer version is detected, it automatically broadcasts update notifications to existing users.
 */
export async function syncAppVersions(force = false): Promise<void> {
  const now = Date.now();
  if (!force && now - lastSyncTimestamp < SYNC_DEBOUNCE_MS) {
    return;
  }
  lastSyncTimestamp = now;

  const db = getDb();
  const definedVersions = getDefinedAppVersions();

  for (const [appId, def] of Object.entries(definedVersions)) {
    try {
      const res = await db.query(
        'SELECT latest_version_code, latest_version_name, update_url FROM app_version_configs WHERE app_id = $1 LIMIT 1',
        [appId]
      );

      const dbCode = res.rows.length > 0 ? Number(res.rows[0].latest_version_code || 1) : 0;
      const targetCode = def.versionCode;
      const targetUrl = def.updateUrl || `https://web-production-5d826.up.railway.app/downloads/speedo-${appId}.apk`;
      const targetRole = appId === 'rider' ? 'rider' : (appId === 'captain' ? 'captain' : 'all');

      if (res.rows.length === 0) {
        // Insert initial row
        await db.query(
          `INSERT INTO app_version_configs 
           (app_id, app_name, latest_version_code, latest_version_name, min_supported_version_code, force_update, title, message, release_notes, update_url, is_active, updated_at)
           VALUES ($1, $2, $3, $4, 1, $5, $6, $7, null, $8, 1, CURRENT_TIMESTAMP)`,
          [
            appId,
            `Speedo ${appId.charAt(0).toUpperCase() + appId.slice(1)}`,
            targetCode,
            def.versionName,
            def.forceUpdate ? 1 : 0,
            def.title,
            def.message,
            targetUrl,
          ]
        );
        console.log(`[AppVersionSync] Seeded initial version config for ${appId} (Build #${targetCode})`);
      } else if (targetCode > dbCode) {
        // New version detected! Automatically update and notify existing users
        console.log(`📢 [AppVersionSync] New build detected for ${appId}: #${targetCode} (v${def.versionName}) > current DB #${dbCode}`);

        await db.query(
          `UPDATE app_version_configs 
           SET latest_version_code = $2,
               latest_version_name = $3,
               title = $4,
               message = $5,
               force_update = $6,
               update_url = $7,
               updated_at = CURRENT_TIMESTAMP
           WHERE app_id = $1`,
          [
            appId,
            targetCode,
            def.versionName,
            def.title,
            def.message,
            def.forceUpdate ? 1 : 0,
            targetUrl,
          ]
        );

        // Mark obsolete update notifications as read
        await db.query(
          `UPDATE notifications SET is_read = 1 WHERE (recipient_role = $1 OR recipient_role = 'all') AND type = 'app_update'`,
          [targetRole]
        );

        // Insert new direct update notification for all existing users
        const notifId = 'notif_update_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
        await db.query(
          `INSERT INTO notifications (id, recipient_id, recipient_role, title, message, type, is_read, metadata_json, created_at)
           VALUES ($1, 'all', $2, $3, $4, 'app_update', 0, $5, CURRENT_TIMESTAMP)`,
          [
            notifId,
            targetRole,
            def.title,
            def.message,
            JSON.stringify({
              appId,
              latestVersionCode: targetCode,
              latestVersionName: def.versionName,
              updateUrl: targetUrl,
              forceUpdate: !!def.forceUpdate,
            }),
          ]
        );

        // Emit real-time WebSocket broadcast to connected users
        emitAppVersionUpdated({
          appId,
          appName: `Speedo ${appId.charAt(0).toUpperCase() + appId.slice(1)}`,
          latestVersionCode: targetCode,
          latestVersionName: def.versionName,
          minSupportedVersionCode: 1,
          forceUpdate: !!def.forceUpdate,
          title: def.title,
          message: def.message,
          releaseNotes: null,
          updateUrl: targetUrl,
          isActive: true,
          isUpdateAvailable: true,
          isForceUpdate: !!def.forceUpdate,
        });

        console.log(`🚀 [AppVersionSync] Directly notified existing ${appId} users of new update (Build #${targetCode}).`);
      } else if (targetCode < dbCode) {
        // Drift correction: Align DB with actual built version in app-versions.json
        console.log(`🔧 [AppVersionSync] Correcting DB version drift for ${appId}: DB #${dbCode} -> Defined #${targetCode}`);
        await db.query(
          `UPDATE app_version_configs 
           SET latest_version_code = $2,
               latest_version_name = $3,
               title = $4,
               message = $5,
               force_update = $6,
               update_url = $7,
               updated_at = CURRENT_TIMESTAMP
           WHERE app_id = $1`,
          [
            appId,
            targetCode,
            def.versionName,
            def.title,
            def.message,
            def.forceUpdate ? 1 : 0,
            targetUrl,
          ]
        );
      }
    } catch (err: any) {
      console.error(`[AppVersionSync] Error syncing ${appId}:`, err.message);
    }
  }
}
