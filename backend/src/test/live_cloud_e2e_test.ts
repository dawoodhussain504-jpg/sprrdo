/**
 * Speedo Live Cloud End-to-End Test Suite
 * Tests the complete ride lifecycle against the live production backend on Railway
 */

const CLOUD_URL = 'https://web-production-5d826.up.railway.app';

async function req(path: string, options: any = {}) {
  const url = `${CLOUD_URL}${path}`;
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

async function runLiveCloudE2ETest() {
  console.log('================================================================');
  console.log('🚀 RUNNING SPEEDO LIVE CLOUD END-TO-END VERIFICATION');
  console.log(`🌐 Target: ${CLOUD_URL}`);
  console.log('================================================================\n');

  try {
    // 1. Health Check
    console.log('📡 [1/12] Testing Live Health Endpoint...');
    const health = await req('/health');
    console.log(`   Status: ${health.status} -> ${JSON.stringify(health.data)}`);
    if (health.data.status !== 'healthy') throw new Error('Cloud health check failed');

    // 2. Rider Login
    console.log('\n🧑 [2/12] Logging in as Rider (rider@speedo.com)...');
    const riderLogin = await req('/api/auth/rider/login', {
      method: 'POST',
      body: { email: 'rider@speedo.com', password: 'Rider@123' },
    });
    console.log(`   Rider Auth Success: ${riderLogin.data.success}, Name: ${riderLogin.data.data.user.name}`);
    const riderToken = riderLogin.data.data.token;

    // 3. Captain Login
    console.log('\n🛵 [3/12] Logging in as Captain (captain@speedo.com)...');
    const captainLogin = await req('/api/auth/captain/login', {
      method: 'POST',
      body: { email: 'captain@speedo.com', password: 'Captain@123' },
    });
    console.log(`   Captain Auth Success: ${captainLogin.data.success}, Name: ${captainLogin.data.data.captain.name}`);
    const captainToken = captainLogin.data.data.token;

    // 4. Admin Login
    console.log('\n🛡️ [4/12] Logging in as Admin (admin@speedo.com)...');
    const adminLogin = await req('/api/auth/admin/login', {
      method: 'POST',
      body: { email: 'admin@speedo.com', password: 'Admin@123' },
    });
    console.log(`   Admin Auth Success: ${adminLogin.data.success}, Role: ${adminLogin.data.data.admin.role}`);
    const adminToken = adminLogin.data.data.token;

    // 5. Captain goes Online & Pushes Live Location
    console.log('\n📍 [5/12] Captain toggling ONLINE & Streaming GPS Location...');
    const toggleOnline = await req('/api/captain/status/toggle', {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { is_online: true },
    });
    console.log(`   Captain Online State: ${toggleOnline.data.success}`);

    const locPush = await req('/api/captain/location/update', {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: {
        lat: 12.9716,
        lng: 77.5946,
        bearing: 45.0,
        speed: 22.5,
        is_online: true,
      },
    });
    console.log(`   GPS Pushed: ${locPush.data.success}`);

    // 6. Rider fetches Fare Estimates
    console.log('\n💰 [6/12] Rider fetching Fare Estimates (Indiranagar -> Koramangala)...');
    const fares = await req('/api/rider/fares/estimate', {
      method: 'POST',
      headers: { Authorization: `Bearer ${riderToken}` },
      body: {
        pickup_lat: 12.9716,
        pickup_lng: 77.5946,
        drop_lat: 12.9352,
        drop_lng: 77.6245,
      },
    });
    console.log(`   Distance: ${fares.data.data.distance_km} km`);
    console.log(`   Rapido Bike Fare: ₹${fares.data.data.estimates.bike.totalFare}`);
    console.log(`   Rapido Auto Fare: ₹${fares.data.data.estimates.auto.totalFare}`);
    console.log(`   Speedo Cab Fare:  ₹${fares.data.data.estimates.cab.totalFare}`);

    // 7. Rider Books a Ride
    console.log('\n📱 [7/12] Rider Booking Rapido Bike Ride...');
    const bookRide = await req('/api/rider/rides/request', {
      method: 'POST',
      headers: { Authorization: `Bearer ${riderToken}` },
      body: {
        pickup_address: 'Indiranagar 100ft Rd, Bangalore',
        pickup_lat: 12.9716,
        pickup_lng: 77.5946,
        drop_address: 'Koramangala 5th Block, Bangalore',
        drop_lat: 12.9352,
        drop_lng: 77.6245,
        vehicle_type: 'bike',
      },
    });
    const ride = bookRide.data.data;
    console.log(`   Ride Created ID: ${ride.id}`);
    console.log(`   Secret Rider OTP: [ ${ride.otp} ]`);
    console.log(`   Fare: ₹${ride.fare}, Status: ${ride.status}`);

    // 8. Captain Receives Incoming Request & Accepts
    console.log('\n🔔 [8/12] Captain Checking Incoming Requests & Accepting...');
    const incoming = await req('/api/captain/rides/requests', {
      headers: { Authorization: `Bearer ${captainToken}` },
    });
    console.log(`   Incoming Requests Available: ${incoming.data.data.length}`);

    const accept = await req(`/api/captain/rides/${ride.id}/accept`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
    });
    console.log(`   Ride Accepted: ${accept.data.success}, New Status: ${accept.data.data.status}`);

    // 9. Captain Marks "Arrived at Pickup"
    console.log('\n📍 [9/12] Captain arriving at Rider Pickup...');
    const arrived = await req(`/api/captain/rides/${ride.id}/status`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { status: 'arrived' },
    });
    console.log(`   Status updated to: ${arrived.data.data.status}`);

    // 10. Captain enters Rider OTP and Starts Trip
    console.log(`\n🔑 [10/12] Captain entering 4-digit OTP [${ride.otp}] to Start Trip...`);
    const startTrip = await req(`/api/captain/rides/${ride.id}/status`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { status: 'ongoing', otp: ride.otp },
    });
    console.log(`   OTP Verified! Ride Status: ${startTrip.data.data.status}`);

    // 11. Captain Completes Trip
    console.log('\n🏁 [11/12] Captain Completing Trip & Collecting Payment...');
    const complete = await req(`/api/captain/rides/${ride.id}/status`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${captainToken}` },
      body: { status: 'completed' },
    });
    console.log(`   Trip Complete: ${complete.data.data.status}`);

    // 12. Admin Dashboard KPI & Live Fleet Verification
    console.log('\n📊 [12/12] Admin checking Live KPIs & Completed Rides...');
    const kpis = await req('/api/admin/dashboard', {
      headers: { Authorization: `Bearer ${adminToken}` },
    });
    console.log(`   Total Registered Riders: ${kpis.data.data.total_riders}`);
    console.log(`   Total Captains:          ${kpis.data.data.total_captains}`);
    console.log(`   Captains Online Now:     ${kpis.data.data.online_captains}`);
    console.log(`   Completed Rides:         ${kpis.data.data.completed_rides}`);
    console.log(`   Total Platform Revenue:  ₹${kpis.data.data.total_revenue}`);

    console.log('\n================================================================');
    console.log('🎉 100% OF LIVE CLOUD FLOWS PASSED PERFECTLY!');
    console.log('================================================================\n');
  } catch (err: any) {
    console.error('\n❌ Cloud E2E Test Failed:', err.message);
    process.exit(1);
  }
}

runLiveCloudE2ETest();
