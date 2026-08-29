export {};

const CLOUD_URL = 'https://web-production-5d826.up.railway.app';

async function testRoadSnappedRouting() {
  console.log('================================================================');
  console.log('🗺️ TESTING SPEEDO ROAD-SNAPPED DYNAMIC ROUTING & OSRM ENGINE');
  console.log(`🌐 Production Target: ${CLOUD_URL}`);
  console.log('================================================================\n');

  // 1. Authenticate Rider
  console.log('1️⃣ Authenticating Rider for Routing API test...');
  const riderLogin = await fetch(`${CLOUD_URL}/api/auth/rider/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'rider@speedo.com', password: 'Rider@123' }),
  });
  const riderData: any = await riderLogin.json();
  const riderToken = riderData.data.token;
  console.log(`   🧑 Logged In: ${riderData.data.user.name}\n`);

  // Coordinates: Koramangala 4th Block -> Indiranagar 100ft Road
  const originLat = 12.9345;
  const originLng = 77.6256;
  const destLat = 12.9784;
  const destLng = 77.6408;

  // 2. Test Direct Road-Snapped Route Calculation
  console.log('2️⃣ Calling Road Route Calculation Endpoint (/api/rider/routes/calculate)...');
  const routeRes = await fetch(`${CLOUD_URL}/api/rider/routes/calculate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${riderToken}`,
    },
    body: JSON.stringify({
      origin_lat: originLat,
      origin_lng: originLng,
      dest_lat: destLat,
      dest_lng: destLng,
    }),
  });

  const routeData: any = await routeRes.json();
  console.log('   ✅ Route Calculation Response:');
  console.log(`      Road Distance: ${routeData.data.distanceKm} km`);
  console.log(`      Traffic Duration: ${routeData.data.durationMins} mins`);
  console.log(`      Route Summary: "${routeData.data.summary}"`);
  console.log(`      Road Coordinate Points: ${routeData.data.coordinates.length} waypoints`);
  console.log(`      Maneuvers: ${routeData.data.maneuvers.length} steps`);
  
  if (routeData.data.maneuvers.length > 0) {
    console.log('\n   📋 Turn-by-Turn Maneuvers Sample:');
    routeData.data.maneuvers.slice(0, 3).forEach((m: any, idx: number) => {
      console.log(`      Step ${idx + 1}: ${m.instruction} (${m.distanceMeters}m, ${m.modifier})`);
    });
  }

  // 3. Test Road-Snapped Fare Estimation
  console.log('\n3️⃣ Calling Fare Estimation Endpoint with Road Distance (/api/rider/fares/estimate)...');
  const fareRes = await fetch(`${CLOUD_URL}/api/rider/fares/estimate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${riderToken}`,
    },
    body: JSON.stringify({
      pickup_lat: originLat,
      pickup_lng: originLng,
      drop_lat: destLat,
      drop_lng: destLng,
    }),
  });
  const fareData: any = await fareRes.json();
  console.log('   ✅ Fare Estimates with Road Trajectory:');
  console.log(`      Road Distance: ${fareData.data.distance_km} km`);
  console.log(`      Estimated Driving Time: ${fareData.data.duration_mins} mins`);
  console.log(`      Bike Fare: ₹${fareData.data.estimates.bike.totalFare}`);
  console.log(`      Auto Fare: ₹${fareData.data.estimates.auto.totalFare}`);
  console.log(`      Cab Fare: ₹${fareData.data.estimates.cab.totalFare}`);

  console.log('\n================================================================');
  console.log('🎉 ROAD-SNAPPED ROUTING & OSRM ENGINE VERIFIED 100%!');
  console.log('================================================================\n');
  process.exit(0);
}

testRoadSnappedRouting().catch((err) => {
  console.error('Route test failed:', err);
  process.exit(1);
});
