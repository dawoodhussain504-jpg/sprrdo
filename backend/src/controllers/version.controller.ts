import { Request, Response } from 'express';
import { getDb } from '../config/db';
import { emitAppVersionUpdated } from '../services/socket';

export interface AppVersionConfigResponse {
  appId: string;
  appName: string;
  latestVersionCode: number;
  latestVersionName: string;
  minSupportedVersionCode: number;
  forceUpdate: boolean;
  title: string;
  message: string;
  releaseNotes: string | null;
  updateUrl: string;
  isActive: boolean;
  isUpdateAvailable: boolean;
  isForceUpdate: boolean;
}

function formatRow(row: any, currentCode?: number): AppVersionConfigResponse {
  const latestCode = parseInt(row.latest_version_code, 10) || 1;
  const minCode = parseInt(row.min_supported_version_code, 10) || 1;
  const forceUpdateFlag = row.force_update === 1 || row.force_update === true;

  let isUpdateAvailable = false;
  let isForceUpdate = false;

  if (currentCode !== undefined && !isNaN(currentCode)) {
    isUpdateAvailable = currentCode < latestCode;
    isForceUpdate = forceUpdateFlag || currentCode < minCode;
  } else {
    isForceUpdate = forceUpdateFlag;
  }

  return {
    appId: row.app_id,
    appName: row.app_name,
    latestVersionCode: latestCode,
    latestVersionName: row.latest_version_name || '1.0.0',
    minSupportedVersionCode: minCode,
    forceUpdate: forceUpdateFlag,
    title: row.title || 'Update Available 🚀',
    message: row.message || 'A new update is available. Please update to continue enjoying Speedo.',
    releaseNotes: row.release_notes || null,
    updateUrl: row.update_url || '',
    isActive: row.is_active === 1 || row.is_active === true,
    isUpdateAvailable,
    isForceUpdate,
  };
}

/**
 * Public Endpoint: GET /api/app-version?app=rider&currentVersion=1
 */
export async function getAppVersionConfig(req: Request, res: Response) {
  try {
    const db = getDb();
    const appId = String(req.query.app || 'rider').toLowerCase();
    const currentCodeStr = req.query.currentVersion as string | undefined;
    const currentCode = currentCodeStr ? parseInt(currentCodeStr, 10) : undefined;

    const result = await db.query(
      'SELECT * FROM app_version_configs WHERE app_id = $1 LIMIT 1',
      [appId]
    );

    if (result.rows.length === 0) {
      const defaultName = `Speedo ${appId.charAt(0).toUpperCase() + appId.slice(1)}`;
      await db.query(
        `INSERT INTO app_version_configs
         (app_id, app_name, latest_version_code, latest_version_name, min_supported_version_code, force_update, title, message, release_notes, update_url, is_active, updated_at)
         VALUES ($1, $2, 1, '1.0.0', 1, 0, $3, $4, $5, $6, 1, CURRENT_TIMESTAMP)`,
        [
          appId,
          defaultName,
          `New ${defaultName} Update Available 🚀`,
          'A new version of Speedo is available with new features and improvements.',
          '• Performance enhancements\n• Bug fixes and new destinations',
          `https://play.google.com/store/apps/details?id=com.speedo.${appId}`
        ]
      );
      const newResult = await db.query('SELECT * FROM app_version_configs WHERE app_id = $1 LIMIT 1', [appId]);
      const config = formatRow(newResult.rows[0], currentCode);
      return res.status(200).json({
        success: true,
        data: config,
      });
    }

    const config = formatRow(result.rows[0], currentCode);

    return res.status(200).json({
      success: true,
      data: config,
    });
  } catch (err: any) {
    console.error('❌ Error in getAppVersionConfig:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to fetch app version configuration',
      error: err.message,
    });
  }
}

/**
 * Admin Endpoint: GET /api/admin/app-versions
 */
export async function getAllAppVersionsAdmin(_req: Request, res: Response) {
  try {
    const db = getDb();
    const result = await db.query(
      'SELECT * FROM app_version_configs ORDER BY app_id ASC'
    );

    const configs = result.rows.map((row: any) => formatRow(row));

    return res.status(200).json({
      success: true,
      data: configs,
    });
  } catch (err: any) {
    console.error('❌ Error in getAllAppVersionsAdmin:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to fetch all app version configurations',
      error: err.message,
    });
  }
}

/**
 * Admin Endpoint: PUT /api/admin/app-versions/:app
 */
export async function updateAppVersionConfig(req: Request, res: Response) {
  try {
    const db = getDb();
    const appId = String(req.params.app || '').toLowerCase();
    const {
      appName,
      latestVersionCode,
      latestVersionName,
      minSupportedVersionCode,
      forceUpdate,
      title,
      message,
      releaseNotes,
      updateUrl,
      isActive,
    } = req.body;

    // Check if row exists
    const check = await db.query('SELECT * FROM app_version_configs WHERE app_id = $1', [appId]);

    if (check.rows.length === 0) {
      // Insert new if not present
      await db.query(
        `INSERT INTO app_version_configs
         (app_id, app_name, latest_version_code, latest_version_name, min_supported_version_code, force_update, title, message, release_notes, update_url, is_active, updated_at)
         VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, CURRENT_TIMESTAMP)`,
        [
          appId,
          appName || `Speedo ${appId}`,
          latestVersionCode || 1,
          latestVersionName || '1.0.0',
          minSupportedVersionCode || 1,
          forceUpdate ? 1 : 0,
          title || 'Update Available',
          message || 'Please update your app to continue.',
          releaseNotes || null,
          updateUrl || 'https://play.google.com/store/apps/details?id=com.speedo',
          isActive !== false ? 1 : 0,
        ]
      );
    } else {
      // Update existing
      await db.query(
        `UPDATE app_version_configs
         SET app_name = COALESCE($2, app_name),
             latest_version_code = COALESCE($3, latest_version_code),
             latest_version_name = COALESCE($4, latest_version_name),
             min_supported_version_code = COALESCE($5, min_supported_version_code),
             force_update = COALESCE($6, force_update),
             title = COALESCE($7, title),
             message = COALESCE($8, message),
             release_notes = COALESCE($9, release_notes),
             update_url = COALESCE($10, update_url),
             is_active = COALESCE($11, is_active),
             updated_at = CURRENT_TIMESTAMP
         WHERE app_id = $1`,
        [
          appId,
          appName || null,
          latestVersionCode !== undefined ? latestVersionCode : null,
          latestVersionName || null,
          minSupportedVersionCode !== undefined ? minSupportedVersionCode : null,
          forceUpdate !== undefined ? (forceUpdate ? 1 : 0) : null,
          title || null,
          message || null,
          releaseNotes !== undefined ? releaseNotes : null,
          updateUrl || null,
          isActive !== undefined ? (isActive ? 1 : 0) : null,
        ]
      );
    }

    const updatedResult = await db.query('SELECT * FROM app_version_configs WHERE app_id = $1', [appId]);
    const updatedConfig = formatRow(updatedResult.rows[0]);

    // Broadcast update to all connected clients in real time!
    emitAppVersionUpdated(updatedConfig);

    return res.status(200).json({
      success: true,
      message: `App version configuration for ${appId} updated and broadcasted successfully.`,
      data: updatedConfig,
    });
  } catch (err: any) {
    console.error('❌ Error in updateAppVersionConfig:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to update app version configuration',
      error: err.message,
    });
  }
}
