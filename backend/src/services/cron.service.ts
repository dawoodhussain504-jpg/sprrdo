import cron from 'node-cron';
import { db } from '../config/db';
import { createNotification } from './notification';
import { emitToUser, emitToCaptains, getIO } from './socket';

export interface CronJobMeta {
  key: string;
  name: string;
  schedule: string;
  timezone: string;
  description: string;
  lastRunAt: string | null;
  lastStatus: 'idle' | 'running' | 'success' | 'failed';
  lastMessage: string | null;
  totalRuns: number;
  totalNotificationsSent: number;
}

const CRON_TIMEZONE = 'Asia/Kolkata';

// In-memory status registry for admin dashboard and monitoring
const jobRegistry: Map<string, CronJobMeta> = new Map([
  [
    'morning_rush',
    {
      key: 'morning_rush',
      name: 'Morning Peak Hours Rush',
      schedule: '0 8 * * *',
      timezone: CRON_TIMEZONE,
      description: 'Notifies offline captains of surging morning demand & alerts riders for quick office/school commute (Daily at 8:00 AM IST)',
      lastRunAt: null,
      lastStatus: 'idle',
      lastMessage: null,
      totalRuns: 0,
      totalNotificationsSent: 0,
    },
  ],
  [
    'evening_rush',
    {
      key: 'evening_rush',
      name: 'Evening Peak Hours Rush',
      schedule: '30 17 * * *',
      timezone: CRON_TIMEZONE,
      description: 'Alerts captains to peak evening commute demand & reminds riders to beat the rush with Speedo (Daily at 5:30 PM IST)',
      lastRunAt: null,
      lastStatus: 'idle',
      lastMessage: null,
      totalRuns: 0,
      totalNotificationsSent: 0,
    },
  ],
  [
    'captain_incentive',
    {
      key: 'captain_incentive',
      name: 'Captain Daily Incentive Milestone',
      schedule: '0 21 * * *',
      timezone: CRON_TIMEZONE,
      description: 'Checks daily rides completed and nudges captains 1-2 rides away from earning their ₹200 bonus (Daily at 9:00 PM IST)',
      lastRunAt: null,
      lastStatus: 'idle',
      lastMessage: null,
      totalRuns: 0,
      totalNotificationsSent: 0,
    },
  ],
  [
    'inactive_riders',
    {
      key: 'inactive_riders',
      name: 'Inactive Rider Retention & Win-Back',
      schedule: '0 11 * * 1',
      timezone: CRON_TIMEZONE,
      description: 'Nudges riders who have not booked a ride in the past 7 days with a 20% discount coupon (Mondays at 11:00 AM IST)',
      lastRunAt: null,
      lastStatus: 'idle',
      lastMessage: null,
      totalRuns: 0,
      totalNotificationsSent: 0,
    },
  ],
  [
    'kyc_nudge',
    {
      key: 'kyc_nudge',
      name: 'Captain KYC Verification Nudge',
      schedule: '0 12 * * *',
      timezone: CRON_TIMEZONE,
      description: 'Reminds captains with incomplete or rejected KYC to complete documentation and start earning (Daily at 12:00 PM IST)',
      lastRunAt: null,
      lastStatus: 'idle',
      lastMessage: null,
      totalRuns: 0,
      totalNotificationsSent: 0,
    },
  ],
  [
    'admin_digest',
    {
      key: 'admin_digest',
      name: 'Admin Operations Morning Digest',
      schedule: '0 9 * * *',
      timezone: CRON_TIMEZONE,
      description: 'Sends real-time daily operational health metrics to admins (Daily at 9:00 AM IST)',
      lastRunAt: null,
      lastStatus: 'idle',
      lastMessage: null,
      totalRuns: 0,
      totalNotificationsSent: 0,
    },
  ],
]);

/**
 * Dispatches an automated notification, saves to database, and triggers real-time WebSocket emit
 */
async function dispatchCronNotification(params: {
  recipientId: string;
  recipientRole: 'rider' | 'captain' | 'admin';
  title: string;
  message: string;
  type: string;
  metadata?: Record<string, any>;
}): Promise<string> {
  const notifId = await createNotification(params);

  try {
    const io = getIO();
    if (io) {
      if (params.recipientRole === 'rider' || params.recipientRole === 'captain') {
        emitToUser(params.recipientId, 'notification:received', {
          id: notifId,
          title: params.title,
          message: params.message,
          type: params.type,
          metadata: params.metadata,
        });
      } else if (params.recipientRole === 'admin') {
        io.to('role_admin').emit('notification:received', {
          id: notifId,
          title: params.title,
          message: params.message,
          type: params.type,
          metadata: params.metadata,
        });
      }
    }
  } catch (_) {
    // Socket emit is opportunistic
  }

  return notifId;
}

// ---------------------------------------------------------------------------
// 1. MORNING RUSH NOTIFICATIONS (8:00 AM IST)
// ---------------------------------------------------------------------------
async function runMorningRushJob(): Promise<number> {
  let sentCount = 0;

  // 1. Captains: Alert all approved captains
  const captains = await db.query<{ id: string; name: string }>(
    `SELECT id, name FROM captains WHERE is_active = 1 AND kyc_status = 'approved'`
  );
  for (const c of captains.rows) {
    await dispatchCronNotification({
      recipientId: c.id,
      recipientRole: 'captain',
      title: '🌅 Morning Peak Hours Started!',
      message: 'High passenger demand across city hubs. Go online now to get back-to-back rides & peak earnings!',
      type: 'peak_hours',
      metadata: { peak: 'morning', time: '8:00 AM IST' },
    });
    sentCount++;
  }

  // 2. Riders: Morning commute nudge
  const riders = await db.query<{ id: string; name: string }>(
    `SELECT id, name FROM users WHERE is_active = 1 LIMIT 500`
  );
  for (const r of riders.rows) {
    await dispatchCronNotification({
      recipientId: r.id,
      recipientRole: 'rider',
      title: '⚡ Morning Commute Ready!',
      message: 'Heading to work or college? Speedo Captains are active in your area. Book your ride in seconds!',
      type: 'commute_nudge',
      metadata: { peak: 'morning' },
    });
    sentCount++;
  }

  try {
    const io = getIO();
    if (io) {
      io.to('role_captain').emit('broadcast:announcement', {
        title: '🌅 Morning Peak Hours Started!',
        message: 'High passenger demand across city hubs. Go online now!',
        target_audience: 'captains',
      });
      io.to('role_rider').emit('broadcast:announcement', {
        title: '⚡ Morning Commute Ready!',
        message: 'Captains are active in your area. Book your ride in seconds!',
        target_audience: 'riders',
      });
    }
  } catch (_) {}

  return sentCount;
}

// ---------------------------------------------------------------------------
// 2. EVENING RUSH NOTIFICATIONS (5:30 PM IST)
// ---------------------------------------------------------------------------
async function runEveningRushJob(): Promise<number> {
  let sentCount = 0;

  // 1. Captains
  const captains = await db.query<{ id: string; name: string }>(
    `SELECT id, name FROM captains WHERE is_active = 1 AND kyc_status = 'approved'`
  );
  for (const c of captains.rows) {
    await dispatchCronNotification({
      recipientId: c.id,
      recipientRole: 'captain',
      title: '🌆 Evening Peak Hours Active!',
      message: 'Commute demand is peaking near business parks & metro hubs. Jump online now to maximize earnings!',
      type: 'peak_hours',
      metadata: { peak: 'evening', time: '5:30 PM IST' },
    });
    sentCount++;
  }

  // 2. Riders
  const riders = await db.query<{ id: string; name: string }>(
    `SELECT id, name FROM users WHERE is_active = 1 LIMIT 500`
  );
  for (const r of riders.rows) {
    await dispatchCronNotification({
      recipientId: r.id,
      recipientRole: 'rider',
      title: '🚗 Returning Home?',
      message: 'Avoid traffic delays—book a Speedo Bike or Auto for quick pickup and transparent fares.',
      type: 'commute_nudge',
      metadata: { peak: 'evening' },
    });
    sentCount++;
  }

  return sentCount;
}

// ---------------------------------------------------------------------------
// 3. CAPTAIN DAILY INCENTIVE MILESTONE (9:00 PM IST)
// ---------------------------------------------------------------------------
async function runCaptainIncentiveJob(): Promise<number> {
  let sentCount = 0;

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayIso = today.toISOString();

  // Find captains who have completed rides today
  const res = await db.query<{ id: string; name: string; ride_count: string | number }>(
    `SELECT c.id, c.name, COUNT(r.id) as ride_count
     FROM captains c
     JOIN rides r ON r.captain_id = c.id
     WHERE r.status = 'completed' AND r.created_at >= $1
     GROUP BY c.id, c.name`,
    [todayIso]
  );

  const TARGET_RIDES = 5;
  const BONUS_AMOUNT = 200;

  for (const row of res.rows) {
    const count = Number(row.ride_count || 0);
    if (count >= 2 && count < TARGET_RIDES) {
      const remaining = TARGET_RIDES - count;
      await dispatchCronNotification({
        recipientId: row.id,
        recipientRole: 'captain',
        title: '🔥 Daily Bonus Within Reach!',
        message: `You've completed ${count} ride${count > 1 ? 's' : ''} today! Complete ${remaining} more before midnight to claim your ₹${BONUS_AMOUNT} daily bonus.`,
        type: 'incentive_milestone',
        metadata: { completed_today: count, target: TARGET_RIDES, bonus_amount: BONUS_AMOUNT },
      });
      sentCount++;
    } else if (count >= TARGET_RIDES) {
      await dispatchCronNotification({
        recipientId: row.id,
        recipientRole: 'captain',
        title: '🏆 Daily Target Achieved!',
        message: `Awesome work, Captain ${row.name}! You unlocked today's ₹${BONUS_AMOUNT} target bonus with ${count} completed rides.`,
        type: 'incentive_achieved',
        metadata: { completed_today: count, bonus_amount: BONUS_AMOUNT },
      });
      sentCount++;
    }
  }

  return sentCount;
}

// ---------------------------------------------------------------------------
// 4. INACTIVE RIDER RETENTION (Every Monday 11:00 AM IST)
// ---------------------------------------------------------------------------
async function runInactiveRidersJob(): Promise<number> {
  let sentCount = 0;

  const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();

  const riders = await db.query<{ id: string; name: string }>(
    `SELECT u.id, u.name 
     FROM users u
     WHERE u.is_active = 1 
       AND u.id NOT IN (
         SELECT DISTINCT rider_id FROM rides WHERE created_at >= $1
       )
     LIMIT 300`,
    [sevenDaysAgo]
  );

  for (const r of riders.rows) {
    await dispatchCronNotification({
      recipientId: r.id,
      recipientRole: 'rider',
      title: '🎁 We Miss You! 20% OFF Your Next Ride',
      message: 'Enjoy 20% OFF your next trip with promo code SPEEDO20. Book your ride today and travel comfortably!',
      type: 'retention_promo',
      metadata: { coupon: 'SPEEDO20', discount: '20%' },
    });
    sentCount++;
  }

  return sentCount;
}

// ---------------------------------------------------------------------------
// 5. CAPTAIN KYC NUDGE (Daily 12:00 PM IST)
// ---------------------------------------------------------------------------
async function runKycNudgeJob(): Promise<number> {
  let sentCount = 0;

  const captains = await db.query<{ id: string; name: string; kyc_status: string }>(
    `SELECT id, name, kyc_status 
     FROM captains 
     WHERE is_active = 1 AND (kyc_status = 'pending' OR kyc_status = 'rejected')
     LIMIT 200`
  );

  for (const c of captains.rows) {
    const isRejected = c.kyc_status === 'rejected';
    await dispatchCronNotification({
      recipientId: c.id,
      recipientRole: 'captain',
      title: isRejected ? '⚠️ Update Your KYC Documents' : '📑 Complete Your Speedo KYC Verification',
      message: isRejected
        ? 'Some KYC documents were rejected. Please re-upload clear photos to get verified and start taking rides.'
        : 'Upload your vehicle RC, driving license, and selfie to get verified and start earning up to ₹1,500/day.',
      type: 'kyc_nudge',
      metadata: { kyc_status: c.kyc_status },
    });
    sentCount++;
  }

  return sentCount;
}

// ---------------------------------------------------------------------------
// 6. ADMIN OPERATIONS DIGEST (Daily 9:00 AM IST)
// ---------------------------------------------------------------------------
async function runAdminDigestJob(): Promise<number> {
  let sentCount = 0;

  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const todayIso = today.toISOString();

  // 1. Pending KYC count
  const kycRes = await db.query<{ count: string | number }>(
    `SELECT COUNT(*) as count FROM captains WHERE kyc_status = 'pending'`
  );
  const pendingKyc = Number(kycRes.rows[0]?.count || 0);

  // 2. Active SOS count
  let activeSos = 0;
  try {
    const sosRes = await db.query<{ count: string | number }>(
      `SELECT COUNT(*) as count FROM sos_alerts WHERE status = 'active'`
    );
    activeSos = Number(sosRes.rows[0]?.count || 0);
  } catch (_) {}

  // 3. Online Captains
  let onlineCaptains = 0;
  try {
    const onlineRes = await db.query<{ count: string | number }>(
      `SELECT COUNT(*) as count FROM locations WHERE is_online = 1`
    );
    onlineCaptains = Number(onlineRes.rows[0]?.count || 0);
  } catch (_) {}

  // 4. Completed rides today
  const ridesRes = await db.query<{ count: string | number }>(
    `SELECT COUNT(*) as count FROM rides WHERE status = 'completed' AND created_at >= $1`,
    [todayIso]
  );
  const todayRides = Number(ridesRes.rows[0]?.count || 0);

  const digestMessage = `Daily Operations: ${onlineCaptains} captains online | ${pendingKyc} pending KYC reviews | ${activeSos} active SOS alerts | ${todayRides} rides today.`;

  const admins = await db.query<{ id: string }>(`SELECT id FROM admins`);
  for (const a of admins.rows) {
    await dispatchCronNotification({
      recipientId: a.id,
      recipientRole: 'admin',
      title: '📊 Speedo Daily Operations Digest',
      message: digestMessage,
      type: 'admin_digest',
      metadata: { onlineCaptains, pendingKyc, activeSos, todayRides },
    });
    sentCount++;
  }

  return sentCount;
}

// ---------------------------------------------------------------------------
// EXECUTOR WITH ERROR BOUNDARIES & TELEMETRY
// ---------------------------------------------------------------------------
async function executeRoutine(jobKey: string): Promise<number> {
  const meta = jobRegistry.get(jobKey);
  if (!meta) {
    throw new Error(`Job key "${jobKey}" not found in cron registry`);
  }

  meta.lastStatus = 'running';
  console.log(`⏱️ [CRON] Starting routine: ${meta.name} (${jobKey})...`);

  try {
    let sent = 0;
    switch (jobKey) {
      case 'morning_rush':
        sent = await runMorningRushJob();
        break;
      case 'evening_rush':
        sent = await runEveningRushJob();
        break;
      case 'captain_incentive':
        sent = await runCaptainIncentiveJob();
        break;
      case 'inactive_riders':
        sent = await runInactiveRidersJob();
        break;
      case 'kyc_nudge':
        sent = await runKycNudgeJob();
        break;
      case 'admin_digest':
        sent = await runAdminDigestJob();
        break;
      default:
        throw new Error(`Unknown routine handler for ${jobKey}`);
    }

    meta.lastRunAt = new Date().toISOString();
    meta.lastStatus = 'success';
    meta.lastMessage = `Dispatched ${sent} automated notification(s) successfully.`;
    meta.totalRuns += 1;
    meta.totalNotificationsSent += sent;

    console.log(`✅ [CRON] Completed ${meta.name}: ${meta.lastMessage}`);
    return sent;
  } catch (err: any) {
    meta.lastRunAt = new Date().toISOString();
    meta.lastStatus = 'failed';
    meta.lastMessage = `Error: ${err.message}`;
    console.error(`❌ [CRON ERROR] Routine ${jobKey} failed:`, err.message);
    throw err;
  }
}

/**
 * Initializes all automated cron jobs
 */
export function initCronJobs() {
  console.log(`====================================================`);
  console.log(`🚀 INITIALIZING AUTOMATED CRON ENGINE (TZ: ${CRON_TIMEZONE})`);
  console.log(`====================================================`);

  for (const [key, meta] of jobRegistry.entries()) {
    try {
      cron.schedule(
        meta.schedule,
        async () => {
          try {
            await executeRoutine(key);
          } catch (e: any) {
            console.error(`[CRON SCHEDULE ERROR] Failed to run ${key}:`, e.message);
          }
        },
        {
          timezone: CRON_TIMEZONE,
        }
      );
      console.log(`   ✓ Scheduled [${key}] "${meta.name}" at "${meta.schedule}"`);
    } catch (schedErr: any) {
      console.error(`   ✗ Failed to schedule [${key}]:`, schedErr.message);
    }
  }
}

/**
 * Retrieves the live status of all registered cron jobs
 */
export function getCronJobStatus() {
  const istFormatter = new Intl.DateTimeFormat('en-IN', {
    timeZone: CRON_TIMEZONE,
    dateStyle: 'full',
    timeStyle: 'medium',
  });

  return {
    timezone: CRON_TIMEZONE,
    serverTimeUtc: new Date().toISOString(),
    currentIstTime: istFormatter.format(new Date()),
    totalJobs: jobRegistry.size,
    jobs: Array.from(jobRegistry.values()),
  };
}

/**
 * Manually triggers a cron job on demand (e.g. from Admin API or testing script)
 */
export async function triggerJobManually(jobKey: string): Promise<{ success: boolean; message: string; notificationsSent: number }> {
  if (jobKey === 'all') {
    let totalSent = 0;
    for (const key of jobRegistry.keys()) {
      try {
        const sent = await executeRoutine(key);
        totalSent += sent;
      } catch (_) {}
    }
    return {
      success: true,
      message: `Triggered all routines. Total notifications dispatched: ${totalSent}`,
      notificationsSent: totalSent,
    };
  }

  if (!jobRegistry.has(jobKey)) {
    return {
      success: false,
      message: `Invalid job key: "${jobKey}". Valid keys: ${Array.from(jobRegistry.keys()).join(', ')}, all`,
      notificationsSent: 0,
    };
  }

  const sent = await executeRoutine(jobKey);
  return {
    success: true,
    message: `Job "${jobKey}" executed successfully. Dispatched ${sent} notifications.`,
    notificationsSent: sent,
  };
}
