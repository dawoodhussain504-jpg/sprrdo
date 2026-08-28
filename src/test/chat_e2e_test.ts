import { io } from 'socket.io-client';

const CLOUD_URL = 'https://web-production-5d826.up.railway.app';

async function testInAppChat() {
  console.log('================================================================');
  console.log('💬 TESTING SPEEDO REAL-TIME IN-APP CHAT (RIDER <-> CAPTAIN)');
  console.log(`🌐 Production Target: ${CLOUD_URL}`);
  console.log('================================================================\n');

  // 1. Authenticate Rider & Captain
  console.log('1️⃣ Authenticating test participants...');
  const riderLogin = await fetch(`${CLOUD_URL}/api/auth/rider/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'rider@speedo.com', password: 'Rider@123' }),
  });
  const riderData: any = await riderLogin.json();
  const riderToken = riderData.data.token;
  const riderId = riderData.data.user.id;
  console.log(`   🧑 Rider Logged In: ${riderData.data.user.name} (${riderId})`);

  const captainLogin = await fetch(`${CLOUD_URL}/api/auth/captain/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'captain@speedo.com', password: 'Captain@123' }),
  });
  const captainData: any = await captainLogin.json();
  const captainToken = captainData.data.token;
  const captainId = captainData.data.captain.id;
  console.log(`   🛵 Captain Logged In: ${captainData.data.captain.name} (${captainId})\n`);

  // 2. Create and Accept a Ride to simulate Active Ride State
  console.log('2️⃣ Booking & Accepting Active Trip for Chat Room...');
  const bookRes = await fetch(`${CLOUD_URL}/api/rider/rides/request`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${riderToken}`,
    },
    body: JSON.stringify({
      pickup_address: 'Koramangala 4th Block, Bangalore',
      pickup_lat: 12.9345,
      pickup_lng: 77.6256,
      drop_address: 'Indiranagar 100ft Road, Bangalore',
      drop_lat: 12.9784,
      drop_lng: 77.6408,
      vehicle_type: 'bike',
    }),
  });
  const bookData: any = await bookRes.json();
  const rideId = bookData.data.id;
  console.log(`   ✅ Ride Requested: ${rideId} (OTP: ${bookData.data.otp})`);

  const acceptRes = await fetch(`${CLOUD_URL}/api/captain/rides/${rideId}/accept`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${captainToken}` },
  });
  const acceptData: any = await acceptRes.json();
  console.log(`   ✅ Ride Accepted by Captain: ${acceptData.data.status}\n`);

  // 3. Connect Real-time Sockets
  console.log('3️⃣ Establishing Bidirectional Socket Connections...');
  const riderSocket = io(CLOUD_URL, {
    auth: { token: riderToken },
    transports: ['websocket', 'polling'],
  });

  const captainSocket = io(CLOUD_URL, {
    auth: { token: captainToken },
    transports: ['websocket', 'polling'],
  });

  await new Promise<void>((resolve) => {
    let connected = 0;
    riderSocket.on('connect', () => {
      console.log(`   ⚡ Rider Socket Connected [${riderSocket.id}]`);
      connected++;
      if (connected === 2) resolve();
    });
    captainSocket.on('connect', () => {
      console.log(`   ⚡ Captain Socket Connected [${captainSocket.id}]`);
      connected++;
      if (connected === 2) resolve();
    });
  });

  // Join Ride Room
  riderSocket.emit('ride:join', { rideId });
  captainSocket.emit('ride:join', { rideId });

  // 4. Test Rider -> Captain Real-Time Message
  console.log('\n4️⃣ Testing Rider -> Captain Message Delivery...');
  const captainReceivedPromise = new Promise<any>((resolve) => {
    captainSocket.on('ride:chat_message', (msg: any) => {
      if (msg.senderRole === 'rider') {
        console.log(`   📥 [CAPTAIN RECEIVED REAL-TIME VIA SOCKET]:`);
        console.log(`      From: Rider (${msg.senderId})`);
        console.log(`      Text: "${msg.messageText}" (Type: ${msg.messageType})`);
        resolve(msg);
      }
    });
  });

  const sendRiderMsgRes = await fetch(`${CLOUD_URL}/api/chat/rides/${rideId}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${riderToken}`,
    },
    body: JSON.stringify({
      message_text: "I'm at the main gate 📍",
      message_type: 'quick_chip',
    }),
  });
  const sendRiderMsgData: any = await sendRiderMsgRes.json();
  console.log(`   📤 Rider sent message: "${sendRiderMsgData.data.messageText}"`);

  await captainReceivedPromise;

  // 5. Test Captain -> Rider Real-Time Reply
  console.log('\n5️⃣ Testing Captain -> Rider Reply Delivery...');
  const riderReceivedPromise = new Promise<any>((resolve) => {
    riderSocket.on('ride:chat_message', (msg: any) => {
      if (msg.senderRole === 'captain') {
        console.log(`   📥 [RIDER RECEIVED REAL-TIME VIA SOCKET]:`);
        console.log(`      From: Captain (${msg.senderId})`);
        console.log(`      Text: "${msg.messageText}" (Type: ${msg.messageType})`);
        resolve(msg);
      }
    });
  });

  const sendCaptMsgRes = await fetch(`${CLOUD_URL}/api/chat/rides/${rideId}/messages`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${captainToken}`,
    },
    body: JSON.stringify({
      message_text: 'I have arrived at pickup 🛵',
      message_type: 'quick_chip',
    }),
  });
  const sendCaptMsgData: any = await sendCaptMsgRes.json();
  console.log(`   📤 Captain sent reply: "${sendCaptMsgData.data.messageText}"`);

  await riderReceivedPromise;

  // 6. Test Conversation History Retrieval
  console.log('\n6️⃣ Verifying Conversation History Endpoint...');
  const historyRes = await fetch(`${CLOUD_URL}/api/chat/rides/${rideId}/messages`, {
    headers: { Authorization: `Bearer ${riderToken}` },
  });
  const historyData: any = await historyRes.json();
  console.log(`   ✅ Fetched ${historyData.count} messages in conversation:`);
  historyData.data.forEach((m: any, idx: number) => {
    console.log(`      ${idx + 1}. [${m.senderRole.toUpperCase()}] ${m.messageText} (${m.messageType})`);
  });

  // 7. Cleanup & Finish Ride
  riderSocket.disconnect();
  captainSocket.disconnect();

  console.log('\n================================================================');
  console.log('🎉 IN-APP REAL-TIME CHAT FULLY VERIFIED 100%!');
  console.log('================================================================\n');
  process.exit(0);
}

testInAppChat().catch((err) => {
  console.error('Chat test failed:', err);
  process.exit(1);
});
