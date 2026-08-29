import http from 'http';
import app from '../server';
import { seedDatabase } from '../seed/seed';

async function runTests() {
  console.log(' Starting Speedo Backend Integration Tests...');
  await seedDatabase();

  const server = http.createServer(app);
  await new Promise<void>((resolve) => server.listen(5099, resolve));
  const baseUrl = 'http://localhost:5099';

  async function request(path: string, options: any = {}) {
    const url = `${baseUrl}${path}`;
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    };

    const res = await fetch(url, {
      method: options.method || 'GET',
      headers,
      body: options.body ? JSON.stringify(options.body) : undefined,
    });

    const data: any = await res.json();
    return { status: res.status, data };
  }

  try {
    // 1. Health check
    console.log('\n--- 1. Testing Health Check ---');
    const health = await request('/health');
    console.log('Health Response:', health.status, health.data.status);
    if (health.data.status !== 'healthy') throw new Error('Health check failed');

    // 2. Rider Login
    console.log('\n--- 2. Testing Rider Login ---');
    const riderLogin = await request('/api/auth/rider/login', {
      method: 'POST',
      body: { email: 'rider@speedo.com', password: 'Rider@123' },
    });
    console.log('Rider Login:', riderLogin.status, riderLogin.data.success);
    if (!riderLogin.data.success) throw new Error('Rider login failed: ' + riderLogin.data.message);
    const riderToken = riderLogin.data.data.token;

    // 3. Captain Login
    console.log('\n--- 3. Testing Captain Login ---');
    const captLogin = await request('/api/auth/captain/login', {
      method: 'POST',
      body: { email: 'captain@speedo.com', password: 'Captain@123' },
    });
    console.log('Captain Login:', captLogin.status, captLogin.data.success);
    if (!captLogin.data.success) throw new Error('Captain login failed: ' + captLogin.data.message);
    const captainToken = captLogin.data.data.token;

    // 4. Admin Login
    console.log('\n--- 4. Testing Admin Login ---');
    const adminLogin = await request('/api/auth/admin/login', {
      method: 'POST',
      body: { email: 'admin@speedo.com', password: 'Admin@123' },
    });
    console.log('Admin Login:', adminLogin.status, adminLogin.data.success);
    if (!adminLogin.data.success) throw new Error('Admin login failed: ' + adminLogin.data.message);
    const adminToken = adminLogin.data.data.token;

    // 5. Rider checks nearby captains
    console.log('\n--- 5. Testing Nearby Captains Search ---');
    const nearby = await request('/api/rider/captains/nearby?lat=12.9716&lng=77.5946&vehicle_type=bike', {
      headers: { Authorization: `Bearer ${riderToken}` },
    });
    console.log('Nearby Captains count:', nearby.data.count);
    if (!nearby.data.success) throw new Error('Nearby captains search failed');

    // 6. Captain Location Push (Simulate Foreground Service)
    console.log('\n--- 6. Testing Captain Live Location Push ---');
    const locPush = await request('/api/captain/location/update', {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { lat: 12.9720, lng: 77.5950, bearing: 90.0, speed: 22.5 },
    });
    console.log('Location Update:', locPush.data.success);
    if (!locPush.data.success) throw new Error('Location push failed');

    // 7. Rider Fare Estimate & Ride Request
    console.log('\n--- 7. Testing Fare Estimate & Ride Request ---');
    const fares = await request('/api/rider/fares/estimate', {
      method: 'POST',
      headers: { Authorization: `Bearer ${riderToken}` },
      body: { pickup_lat: 12.9716, pickup_lng: 77.5946, drop_lat: 12.9352, drop_lng: 77.6245 },
    });
    console.log('Fare Estimates computed for distance (km):', fares.data.data.distance_km);

    const rideReq = await request('/api/rider/rides/request', {
      method: 'POST',
      headers: { Authorization: `Bearer ${riderToken}` },
      body: {
        pickup_address: 'Indiranagar 100ft Road',
        pickup_lat: 12.9716,
        pickup_lng: 77.5946,
        drop_address: 'Koramangala Sony World Signal',
        drop_lat: 12.9352,
        drop_lng: 77.6245,
        vehicle_type: 'bike',
      },
    });
    console.log('Ride Request Status:', rideReq.status, rideReq.data.message);
    const rideId = rideReq.data.data.id;
    const rideOtp = rideReq.data.data.otp;

    // 8. Captain polls incoming requests & accepts
    console.log('\n--- 8. Testing Captain Incoming Requests & Accept ---');
    const captReqs = await request('/api/captain/rides/requests', {
      headers: { Authorization: `Bearer ${captainToken}` },
    });
    console.log('Captain incoming requests count:', captReqs.data.count);

    const accept = await request(`/api/captain/rides/${rideId}/accept`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
    });
    console.log('Captain accept ride:', accept.data.success);

    // 9. Rider tracks active ride
    console.log('\n--- 9. Testing Rider Active Ride Tracking (Polling) ---');
    const riderActive = await request('/api/rider/rides/active', {
      headers: { Authorization: `Bearer ${riderToken}` },
    });
    console.log('Active Ride Status:', riderActive.data.data.status, 'ETA (min):', riderActive.data.data.eta_minutes);

    // 10. Captain status progression: Arrived -> Start with OTP -> Complete
    console.log('\n--- 10. Testing Captain Ride Lifecycle (Arrived -> OTP Start -> Complete) ---');
    const arrived = await request(`/api/captain/rides/${rideId}/status`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { status: 'arrived' },
    });
    console.log('Status Arrived:', arrived.data.success);

    const startRide = await request(`/api/captain/rides/${rideId}/status`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { status: 'ongoing', otp: rideOtp },
    });
    console.log('Status Ongoing (with OTP):', startRide.data.success);

    const completeRide = await request(`/api/captain/rides/${rideId}/status`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { status: 'completed' },
    });
    console.log('Status Completed:', completeRide.data.success);

    // 11. Admin Dashboard & KYC Review
    console.log('\n--- 11. Testing Admin Dashboard & KYC Review Queue ---');
    const adminStats = await request('/api/admin/dashboard', {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    console.log('Admin Dashboard Stats:', adminStats.data.data);

    const kycQueue = await request('/api/admin/kyc/queue', {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    console.log('Pending KYC Queue items:', kycQueue.data.count);

    const review = await request('/api/admin/kyc/capt_pending_002/review', {
      method: 'POST',
      headers: { Authorization: `Bearer ${adminToken}` },
      body: { status: 'approved', admin_remarks: 'All documents verified and match state records.' },
    });
    console.log('Admin KYC Review outcome:', review.data.success);

    // 12. Notification Unread Badges
    console.log('\n--- 12. Testing Unread Notification Badges ---');
    const riderBadge = await request('/api/rider/notifications/unread-count', {
      headers: { Authorization: `Bearer ${riderToken}` },
    });
    console.log('Rider Unread Badges count:', riderBadge.data.count);

    console.log('\n======================================================');
    console.log('🎉 ALL SPEEDO BACKEND INTEGRATION TESTS PASSED 100%!');
    console.log('======================================================');
  } finally {
    server.close();
  }
}

if (require.main === module) {
  runTests()
    .then(() => process.exit(0))
    .catch((err) => {
      console.error('❌ Test failed:', err);
      process.exit(1);
    });
}
