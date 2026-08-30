import { Pool } from 'pg';
import sqlite3 from 'sqlite3';
import path from 'path';
import fs from 'fs';
import dotenv from 'dotenv';

dotenv.config();

export interface QueryResult<T = any> {
  rows: T[];
  rowCount: number;
}

export interface DatabaseAdapter {
  query<T = any>(sql: string, params?: any[]): Promise<QueryResult<T>>;
  exec(sql: string): Promise<void>;
  close(): Promise<void>;
  isPostgres(): boolean;
}

class PostgresAdapter implements DatabaseAdapter {
  private pool: Pool;

  constructor(connectionString: string) {
    this.pool = new Pool({
      connectionString,
      ssl: process.env.NODE_ENV === 'production' ? { rejectUnauthorized: false } : undefined,
    });
  }

  async query<T = any>(sql: string, params: any[] = []): Promise<QueryResult<T>> {
    const res = await this.pool.query(sql, params);
    return {
      rows: res.rows,
      rowCount: res.rowCount ?? 0,
    };
  }

  async exec(sql: string): Promise<void> {
    await this.pool.query(sql);
  }

  async close(): Promise<void> {
    await this.pool.end();
  }

  isPostgres(): boolean {
    return true;
  }
}

class SqliteAdapter implements DatabaseAdapter {
  private db: sqlite3.Database;

  constructor(dbPath: string) {
    const dir = path.dirname(dbPath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    this.db = new sqlite3.Database(dbPath);
  }

  async query<T = any>(sql: string, params: any[] = []): Promise<QueryResult<T>> {
    return new Promise((resolve, reject) => {
      // Normalize PostgreSQL $1, $2, $3 parameter placeholders to SQLite indexed parameters ?1, ?2, ?3
      let normalizedSql = sql.replace(/\$(\d+)/g, '?$1');
      const normalizedParams = [...params];

      const trimmed = normalizedSql.trim().toUpperCase();
      const isSelect = trimmed.startsWith('SELECT') || trimmed.startsWith('PRAGMA');

      if (isSelect) {
        this.db.all(normalizedSql, normalizedParams, (err, rows) => {
          if (err) return reject(err);
          resolve({
            rows: (rows as T[]) || [],
            rowCount: rows ? rows.length : 0,
          });
        });
      } else {
        this.db.run(normalizedSql, normalizedParams, function (err) {
          if (err) return reject(err);
          resolve({
            rows: [],
            rowCount: this.changes,
          });
        });
      }
    });
  }

  async exec(sql: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db.exec(sql, (err) => {
        if (err) reject(err);
        else resolve();
      });
    });
  }

  async close(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.db.close((err) => {
        if (err) reject(err);
        else resolve();
      });
    });
  }

  isPostgres(): boolean {
    return false;
  }
}

let dbInstance: DatabaseAdapter;

export function getDb(): DatabaseAdapter {
  if (!dbInstance) {
    const dbUrl = process.env.DATABASE_URL;
    if (dbUrl && dbUrl.trim() !== '') {
      console.log(' Connecting to PostgreSQL database on Railway / Cloud...');
      dbInstance = new PostgresAdapter(dbUrl);
    } else {
      const defaultPath = path.resolve(__dirname, '../../speedo.db');
      console.log(` Using SQLite database at: ${defaultPath}`);
      dbInstance = new SqliteAdapter(defaultPath);
    }
  }
  return dbInstance;
}

export const db = {
  query: <T = any>(sql: string, params?: any[]) => getDb().query<T>(sql, params),
  exec: (sql: string) => getDb().exec(sql),
};
