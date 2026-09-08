import fs from 'fs';
import path from 'path';
import { getDb } from '../config/db';

export async function runMigrations() {
  console.log(' Running database migrations...');
  const db = getDb();
  let schemaPath = path.resolve(__dirname, 'schema.sql');
  if (!fs.existsSync(schemaPath)) {
    schemaPath = path.resolve(__dirname, '../../src/database/schema.sql');
  }
  const sql = fs.readFileSync(schemaPath, 'utf8');

  try {
    await db.exec(sql);

    // Ensure columns exist on both Postgres and SQLite
    if (db.isPostgres()) {
      await db.exec(`
        ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS file_data TEXT;
        ALTER TABLE kyc_documents ADD COLUMN IF NOT EXISTS mime_type VARCHAR(64);
        ALTER TABLE captains ADD COLUMN IF NOT EXISTS payment_qr_data TEXT;
        ALTER TABLE captains ADD COLUMN IF NOT EXISTS payment_qr_mime VARCHAR(64);
      `);
    } else {
      try {
        const kycCols = await db.query("PRAGMA table_info(kyc_documents)");
        const colNames = kycCols.rows.map((r: any) => r.name);
        if (!colNames.includes('file_data')) {
          await db.exec("ALTER TABLE kyc_documents ADD COLUMN file_data TEXT");
        }
        if (!colNames.includes('mime_type')) {
          await db.exec("ALTER TABLE kyc_documents ADD COLUMN mime_type VARCHAR(64)");
        }
        const captCols = await db.query("PRAGMA table_info(captains)");
        const captColNames = captCols.rows.map((r: any) => r.name);
        if (!captColNames.includes('payment_qr_data')) {
          await db.exec("ALTER TABLE captains ADD COLUMN payment_qr_data TEXT");
        }
        if (!captColNames.includes('payment_qr_mime')) {
          await db.exec("ALTER TABLE captains ADD COLUMN payment_qr_mime VARCHAR(64)");
        }
      } catch (sqliteErr: any) {
        console.warn(' SQLite alter table fallback notice:', sqliteErr.message);
      }
    }

    // Ensure device_tokens table exists for FCM push notifications
    await db.exec(`
      CREATE TABLE IF NOT EXISTS device_tokens (
        id VARCHAR(64) PRIMARY KEY,
        user_id VARCHAR(64) NOT NULL,
        role VARCHAR(32) NOT NULL,
        token TEXT NOT NULL,
        device_model VARCHAR(128),
        os_version VARCHAR(64),
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE(user_id, token)
      );
    `);

    console.log(' Database migrations completed successfully.');
  } catch (err: any) {
    console.error(' Migration execution error:', err.message);
    throw err;
  }
}

if (require.main === module) {
  runMigrations()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error(' Migration failed:', err);
      process.exit(1);
    });
}
