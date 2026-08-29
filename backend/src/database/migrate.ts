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
