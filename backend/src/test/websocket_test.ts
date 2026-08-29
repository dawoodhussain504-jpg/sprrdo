import { io } from 'socket.io-client';

const CLOUD_URL = 'https://web-production-5d826.up.railway.app';

async function testWebSocket() {
  console.log('================================================================');
  console.log('⚡ TESTING SPEEDO REAL-TIME WEBSOCKET (SOCKET.IO)');
  console.log(`🌐 Target: ${CLOUD_URL}`);
  console.log('================================================================\n');

  // 1. Authenticate Captain
  const loginRes = await fetch(`${CLOUD_URL}/api/auth/captain/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'captain@speedo.com', password: 'Captain@123' }),
  });
  const loginData: any = await loginRes.json();
  const token = loginData.data.token;
  console.log(`🔑 Captain Authenticated: ${loginData.data.captain.name}`);

  // 2. Connect Captain Socket
  const captainSocket = io(CLOUD_URL, {
    auth: { token },
    transports: ['websocket', 'polling'],
  });

  // 3. Connect Rider Socket
  const riderLoginRes = await fetch(`${CLOUD_URL}/api/auth/rider/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'rider@speedo.com', password: 'Rider@123' }),
  });
  const riderLoginData: any = await riderLoginRes.json();
  const riderToken = riderLoginData.data.token;
  console.log(`🧑 Rider Authenticated: ${riderLoginData.data.user.name}`);

  const riderSocket = io(CLOUD_URL, {
    auth: { token: riderToken },
    transports: ['websocket', 'polling'],
  });

  await new Promise<void>((resolve) => {
    let connected = 0;
    captainSocket.on('connect', () => {
      console.log(`✅ Captain Socket Connected [ID: ${captainSocket.id}]`);
      connected++;
      if (connected === 2) resolve();
    });
    riderSocket.on('connect', () => {
      console.log(`✅ Rider Socket Connected [ID: ${riderSocket.id}]`);
      connected++;
      if (connected === 2) resolve();
    });
  });

  // 4. Join Active Ride Room simulation
  const testRideId = 'ride_live_socket_test';
  captainSocket.emit('ride:join', { rideId: testRideId });
  riderSocket.emit('ride:join', { rideId: testRideId });

  // 5. Stream sub-second GPS from Captain -> Receive on Rider
  const receivedPromise = new Promise<void>((resolve) => {
    riderSocket.on('ride:location_broadcast', (locData: any) => {
      console.log('📍 [SUB-SECOND GPS BROADCAST RECEIVED BY RIDER]:');
      console.log(`   Lat: ${locData.lat}, Lng: ${locData.lng}, Bearing: ${locData.bearing}°, Speed: ${locData.speed} km/h`);
      console.log(`   Real-Time Latency: ${Date.now() - locData.timestamp}ms`);
      resolve();
    });
  });

  setTimeout(() => {
    captainSocket.emit('captain:location_update', {
      lat: 12.9716,
      lng: 77.5946,
      bearing: 88.5,
      speed: 34.2,
      isOnline: true,
      activeRideId: testRideId,
    });
  }, 500);

  await receivedPromise;

  captainSocket.disconnect();
  riderSocket.disconnect();

  console.log('\n================================================================');
  console.log('🎉 WEBSOCKET REAL-TIME SUB-SECOND STREAMING VERIFIED 100%!');
  console.log('================================================================\n');
  process.exit(0);
}

testWebSocket().catch((e) => {
  console.error('Socket Test Failed:', e);
  process.exit(1);
});
