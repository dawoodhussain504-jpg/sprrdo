import { Request, Response } from 'express';
import { AuthenticatedRequest } from '../middleware/auth';
import { db } from '../config/db';
import { emitPopularDestinationsUpdate } from '../services/socket';

// Haversine distance calculator in kilometers
function calculateDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371; // Earth radius in km
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

// Auto-migrate table columns and seed Sheikhpura / Regional places if absent
let migrationChecked = false;
async function ensureDestinationSchema() {
  if (migrationChecked) return;
  try {
    await db.query(`
      ALTER TABLE popular_destinations ADD COLUMN IF NOT EXISTS city VARCHAR(100);
      ALTER TABLE popular_destinations ADD COLUMN IF NOT EXISTS district VARCHAR(100);
      ALTER TABLE popular_destinations ADD COLUMN IF NOT EXISTS state VARCHAR(100);
    `);

    // Backfill legacy entries if city is NULL
    await db.query(`
      UPDATE popular_destinations
      SET city = 'Bangalore', district = 'Bengaluru Urban', state = 'Karnataka'
      WHERE city IS NULL AND (address ILIKE '%Bangalore%' OR address ILIKE '%Bengaluru%' OR title ILIKE '%Bangalore%' OR title ILIKE '%Bengaluru%' OR title ILIKE '%Airport%')
    `);

    // Backfill existing Sheikhpura places
    await db.query(`
      UPDATE popular_destinations
      SET city = 'Sheikhpura', district = 'Sheikhpura', state = 'Bihar'
      WHERE city IS NULL AND (
        address ILIKE '%Sheikhpura%' OR address ILIKE '%Shekhopur%' OR address ILIKE '%Girhinda%' OR
        title ILIKE '%Sheikhpura%' OR title ILIKE '%Girhinda%' OR address ILIKE '%Ahiyapur%' OR
        address ILIKE '%Bypass Road%' OR address ILIKE '%Khandpar%' OR address ILIKE '%Dallu Chowk%' OR
        address ILIKE '%Bazidpur%' OR address ILIKE '%Station Road%'
      )
    `);

    // Check if Sheikhpura destinations exist
    const checkSheikhpura = await db.query(
      "SELECT id FROM popular_destinations WHERE city ILIKE '%Sheikhpura%' OR district ILIKE '%Sheikhpura%' LIMIT 1"
    );

    if (checkSheikhpura.rows.length === 0) {
      console.log('📍 Seeding default popular destinations for Sheikhpura and Bihar...');
      const samplePlaces = [
        {
          id: 'dest_sheikhpura_junction',
          title: 'Sheikhpura Junction Railway Station',
          subtitle: 'Central Railway & Transit Hub',
          category: 'TRANSIT',
          badge: '🚆 Central Railway',
          image_url: 'https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=600&auto=format&fit=crop&q=80',
          lat: 25.1378,
          lng: 85.8569,
          address: 'Sheikhpura Railway Station, Station Road, Sheikhpura, Bihar 811105',
          city: 'Sheikhpura',
          district: 'Sheikhpura',
          state: 'Bihar',
          sort_order: 1
        },
        {
          id: 'dest_giriyak_hills',
          title: 'Giriyak Hills & Buddhist Heritage',
          subtitle: 'Ancient Stupa & Scenic Overlook',
          category: 'HERITAGE',
          badge: '🏔️ Historic & Scenic',
          image_url: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=80',
          lat: 25.0440,
          lng: 85.5290,
          address: 'Giriyak Hill Stupa, Bihar State Highway, near Sheikhpura Border, Bihar',
          city: 'Sheikhpura',
          district: 'Sheikhpura',
          state: 'Bihar',
          sort_order: 2
        },
        {
          id: 'dest_tripolia_gate',
          title: 'Tripolia Gate & Purani Bazaar',
          subtitle: 'Main City Center & Market Complex',
          category: 'SHOPPING',
          badge: '🛍️ City Market',
          image_url: 'https://images.unsplash.com/photo-1567449303078-57ad995bd301?w=600&auto=format&fit=crop&q=80',
          lat: 25.1320,
          lng: 85.8480,
          address: 'Purani Bazaar, Tripolia Gate, Sheikhpura, Bihar 811105',
          city: 'Sheikhpura',
          district: 'Sheikhpura',
          state: 'Bihar',
          sort_order: 3
        },
        {
          id: 'dest_arghauti_pokhar',
          title: 'Arghauti Pokhar Waterfront',
          subtitle: 'Scenic Lake & Sunset Promenade',
          category: 'PARK',
          badge: '🌳 Scenic Lake Spot',
          image_url: 'https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=600&auto=format&fit=crop&q=80',
          lat: 25.1350,
          lng: 85.8520,
          address: 'Arghauti Pokhar, Ward No 7, Sheikhpura, Bihar',
          city: 'Sheikhpura',
          district: 'Sheikhpura',
          state: 'Bihar',
          sort_order: 4
        },
        {
          id: 'dest_vaidyanath_temple',
          title: 'Vaidyanath Temple & Town Hall',
          subtitle: 'Heritage Temple & Cultural Center',
          category: 'HERITAGE',
          badge: '🕉️ Spiritual Spot',
          image_url: 'https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80',
          lat: 25.1410,
          lng: 85.8610,
          address: 'Vaidyanath Temple Road, Sheikhpura, Bihar',
          city: 'Sheikhpura',
          district: 'Sheikhpura',
          state: 'Bihar',
          sort_order: 5
        },
        {
          id: 'dest_patna_golghar',
          title: 'Golghar & Gandhi Maidan',
          subtitle: 'Historical Landmark & Public Grounds',
          category: 'HERITAGE',
          badge: '🏛️ Historic Landmark',
          image_url: 'https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80',
          lat: 25.6178,
          lng: 85.1414,
          address: 'Ashok Rajpath, near Gandhi Maidan, Patna, Bihar 800001',
          city: 'Patna',
          district: 'Patna',
          state: 'Bihar',
          sort_order: 10
        },
        {
          id: 'dest_patna_ganga_path',
          title: 'Marine Drive (Ganga Pathway)',
          subtitle: 'Riverfront Promenade & Cafes',
          category: 'DINING',
          badge: '🌊 Riverfront & Cafes',
          image_url: 'https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=600&auto=format&fit=crop&q=80',
          lat: 25.6260,
          lng: 85.1520,
          address: 'Loknayak Ganga Path, Digha Ghat to Patna Ghat, Patna, Bihar',
          city: 'Patna',
          district: 'Patna',
          state: 'Bihar',
          sort_order: 11
        }
      ];

      for (const p of samplePlaces) {
        await db.query(
          `INSERT INTO popular_destinations (id, title, subtitle, category, badge, image_url, lat, lng, address, city, district, state, is_active, sort_order)
           VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 1, $13)
           ON CONFLICT (id) DO NOTHING`,
          [p.id, p.title, p.subtitle, p.category, p.badge, p.image_url, p.lat, p.lng, p.address, p.city, p.district, p.state, p.sort_order]
        );
      }
    }

    migrationChecked = true;
  } catch (err: any) {
    console.warn('Destination schema ensure error:', err.message);
  }
}

export async function getPopularDestinationsPublic(req: Request, res: Response) {
  try {
    await ensureDestinationSchema();

    const latParam = req.query.lat ? parseFloat(req.query.lat as string) : null;
    const lngParam = req.query.lng ? parseFloat(req.query.lng as string) : null;
    const cityParam = req.query.city ? (req.query.city as string).trim() : null;
    const districtParam = req.query.district ? (req.query.district as string).trim() : null;
    const stateParam = req.query.state ? (req.query.state as string).trim() : null;

    const query = `
      SELECT id, title, subtitle, category, badge, image_url, lat, lng, address as full_address,
             city, district, state, is_active, sort_order, created_at
      FROM popular_destinations
      WHERE is_active = 1
      ORDER BY sort_order ASC, created_at DESC
    `;
    const result = await db.query(query);
    const allDestinations = result.rows.map(row => ({
      ...row,
      is_active: Boolean(row.is_active)
    }));

    // Calculate distances if coordinates provided
    const withDistance = allDestinations.map(dest => {
      let distanceKm: number | null = null;
      if (latParam != null && lngParam != null && !isNaN(latParam) && !isNaN(lngParam)) {
        distanceKm = calculateDistanceKm(latParam, lngParam, Number(dest.lat), Number(dest.lng));
      }
      return {
        ...dest,
        distance_km: distanceKm
      };
    });

    // Determine ranking and header
    const cleanCity = cityParam?.toLowerCase() || '';
    const cleanDistrict = districtParam?.toLowerCase() || '';
    const cleanState = stateParam?.toLowerCase() || '';

    // Check matches
    const cityMatches = withDistance.filter(d =>
      (cleanCity && (
        (d.city && d.city.toLowerCase().includes(cleanCity)) ||
        (d.full_address && d.full_address.toLowerCase().includes(cleanCity)) ||
        (d.title && d.title.toLowerCase().includes(cleanCity))
      )) ||
      (cleanDistrict && (
        (d.district && d.district.toLowerCase().includes(cleanDistrict)) ||
        (d.full_address && d.full_address.toLowerCase().includes(cleanDistrict)) ||
        (d.title && d.title.toLowerCase().includes(cleanDistrict))
      )) ||
      (d.distance_km != null && d.distance_km <= 35)
    );

    const stateMatches = withDistance.filter(d =>
      (cleanState && d.state && d.state.toLowerCase().includes(cleanState)) ||
      (d.distance_km != null && d.distance_km <= 150)
    );

    let locationHeader = 'Popular Destinations';
    let rankedList: typeof withDistance;

    if (cityMatches.length > 0) {
      const displayCity = cityParam || districtParam || cityMatches[0].city || 'Your Area';
      locationHeader = `Top Places in ${displayCity}`;
      // Put city matches first (sorted by distance or sort_order), then state matches, then remainder
      const rest = withDistance.filter(d => !cityMatches.some(cm => cm.id === d.id));
      rankedList = [...cityMatches, ...rest];
    } else if (stateMatches.length > 0) {
      const displayState = stateParam || stateMatches[0].state || 'Your Region';
      locationHeader = `Top Places in ${displayState}`;
      const rest = withDistance.filter(d => !stateMatches.some(sm => sm.id === d.id));
      rankedList = [...stateMatches, ...rest];
    } else if (latParam != null && lngParam != null) {
      // Find closest destination
      const sortedByDist = [...withDistance].sort((a, b) => (a.distance_km || 9999) - (b.distance_km || 9999));
      if (sortedByDist.length > 0 && (sortedByDist[0].distance_km || 9999) < 60) {
        locationHeader = `Top Places near You (${sortedByDist[0].distance_km} km)`;
      } else {
        locationHeader = 'Popular Destinations';
      }
      rankedList = sortedByDist;
    } else {
      locationHeader = 'Popular Destinations';
      rankedList = withDistance;
    }

    return res.json({
      success: true,
      locationHeader,
      data: rankedList
    });
  } catch (error: any) {
    console.error('Error fetching public popular destinations:', error);
    return res.status(500).json({ success: false, message: 'Failed to fetch popular destinations', error: error.message });
  }
}

export async function getPopularDestinationsAdmin(_req: AuthenticatedRequest, res: Response) {
  try {
    await ensureDestinationSchema();
    const query = `
      SELECT id, title, subtitle, category, badge, image_url, lat, lng, address as full_address,
             city, district, state, is_active, sort_order, created_at, updated_at
      FROM popular_destinations
      ORDER BY sort_order ASC, created_at DESC
    `;
    const result = await db.query(query);
    return res.json({
      success: true,
      data: result.rows.map(row => ({
        ...row,
        is_active: Boolean(row.is_active)
      }))
    });
  } catch (error: any) {
    console.error('Error fetching admin popular destinations:', error);
    return res.status(500).json({ success: false, message: 'Failed to fetch destinations for admin', error: error.message });
  }
}

export async function createPopularDestination(req: AuthenticatedRequest, res: Response) {
  try {
    await ensureDestinationSchema();
    const { title, subtitle, category, badge, image_url, lat, lng, address, full_address, city, district, state, sort_order } = req.body;

    if (!title || !category || lat == null || lng == null) {
      return res.status(400).json({ success: false, message: 'Title, category, latitude and longitude are required' });
    }

    const id = 'dest_' + Date.now() + '_' + Math.random().toString(36).substring(2, 6);
    const addr = full_address || address || title;
    const sub = subtitle || category;
    const b = badge || 'Popular';
    const img = image_url || 'https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80';
    const order = Number(sort_order) || 0;
    const c = city?.trim() || null;
    const dist = district?.trim() || c || null;
    const st = state?.trim() || null;

    await db.query(
      `INSERT INTO popular_destinations (id, title, subtitle, category, badge, image_url, lat, lng, address, city, district, state, is_active, sort_order)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, 1, $13)`,
      [id, title, sub, category, b, img, Number(lat), Number(lng), addr, c, dist, st, order]
    );

    const created = {
      id,
      title,
      subtitle: sub,
      category,
      badge: b,
      image_url: img,
      lat: Number(lat),
      lng: Number(lng),
      full_address: addr,
      city: c,
      district: dist,
      state: st,
      is_active: true,
      sort_order: order
    };

    // Broadcast real-time WebSocket update to Rider & Captain apps
    emitPopularDestinationsUpdate({ action: 'create', destination: created });

    return res.status(201).json({
      success: true,
      data: created,
      message: 'Popular destination created successfully'
    });
  } catch (error: any) {
    console.error('Error creating popular destination:', error);
    return res.status(500).json({ success: false, message: 'Failed to create destination', error: error.message });
  }
}

export async function updatePopularDestination(req: AuthenticatedRequest, res: Response) {
  try {
    await ensureDestinationSchema();
    const { id } = req.params;
    const { title, subtitle, category, badge, image_url, lat, lng, address, full_address, city, district, state, is_active, sort_order } = req.body;

    const existing = await db.query('SELECT * FROM popular_destinations WHERE id = $1', [id]);
    if (existing.rows.length === 0) {
      return res.status(404).json({ success: false, message: 'Destination not found' });
    }

    const current = existing.rows[0];
    const newTitle = title !== undefined ? title : current.title;
    const newSubtitle = subtitle !== undefined ? subtitle : current.subtitle;
    const newCategory = category !== undefined ? category : current.category;
    const newBadge = badge !== undefined ? badge : current.badge;
    const newImg = image_url !== undefined ? image_url : current.image_url;
    const newLat = lat !== undefined ? Number(lat) : Number(current.lat);
    const newLng = lng !== undefined ? Number(lng) : Number(current.lng);
    const newAddress = (full_address !== undefined ? full_address : address) !== undefined ? (full_address || address) : current.address;
    const newCity = city !== undefined ? (city ? city.trim() : null) : current.city;
    const newDistrict = district !== undefined ? (district ? district.trim() : null) : current.district;
    const newState = state !== undefined ? (state ? state.trim() : null) : current.state;
    const newActive = is_active !== undefined ? (is_active ? 1 : 0) : current.is_active;
    const newOrder = sort_order !== undefined ? Number(sort_order) : current.sort_order;

    await db.query(
      `UPDATE popular_destinations
       SET title = $1, subtitle = $2, category = $3, badge = $4, image_url = $5, lat = $6, lng = $7,
           address = $8, city = $9, district = $10, state = $11, is_active = $12, sort_order = $13, updated_at = CURRENT_TIMESTAMP
       WHERE id = $14`,
      [newTitle, newSubtitle, newCategory, newBadge, newImg, newLat, newLng, newAddress, newCity, newDistrict, newState, newActive, newOrder, id]
    );

    const updated = {
      id,
      title: newTitle,
      subtitle: newSubtitle,
      category: newCategory,
      badge: newBadge,
      image_url: newImg,
      lat: newLat,
      lng: newLng,
      full_address: newAddress,
      city: newCity,
      district: newDistrict,
      state: newState,
      is_active: Boolean(newActive),
      sort_order: newOrder
    };

    // Broadcast real-time WebSocket update to Rider & Captain apps
    emitPopularDestinationsUpdate({ action: 'update', destination: updated });

    return res.json({
      success: true,
      data: updated,
      message: 'Popular destination updated successfully'
    });
  } catch (error: any) {
    console.error('Error updating popular destination:', error);
    return res.status(500).json({ success: false, message: 'Failed to update destination', error: error.message });
  }
}

export async function deletePopularDestination(req: AuthenticatedRequest, res: Response) {
  try {
    await ensureDestinationSchema();
    const { id } = req.params;
    await db.query('DELETE FROM popular_destinations WHERE id = $1', [id]);

    // Broadcast real-time WebSocket update to Rider & Captain apps
    emitPopularDestinationsUpdate({ action: 'delete', id });

    return res.json({
      success: true,
      message: 'Popular destination deleted successfully'
    });
  } catch (error: any) {
    console.error('Error deleting popular destination:', error);
    return res.status(500).json({ success: false, message: 'Failed to delete destination', error: error.message });
  }
}
