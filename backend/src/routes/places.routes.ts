import { Router, Request, Response } from 'express';

const router = Router();

// Offline & instant high-traffic presets fallback
const PRESET_PLACES = [
  { title: "Indiranagar 100ft Road", subtitle: "Indiranagar, Bangalore", fullAddress: "Indiranagar 100ft Road, Bangalore, Karnataka", lat: 12.9716, lng: 77.5946 },
  { title: "Koramangala 5th Block", subtitle: "Koramangala, Bangalore", fullAddress: "Koramangala 5th Block, Bangalore, Karnataka", lat: 12.9352, lng: 77.6245 },
  { title: "HSR Layout Sector 1", subtitle: "HSR Layout, Bangalore", fullAddress: "HSR Layout Sector 1, Bangalore, Karnataka", lat: 12.9121, lng: 77.6446 },
  { title: "MG Road Metro Station", subtitle: "MG Road, Bangalore", fullAddress: "MG Road Metro Station, Shivaji Nagar, Bangalore", lat: 12.9756, lng: 77.6066 },
  { title: "Whitefield ITPL Main Gate", subtitle: "ITPL, Whitefield, Bangalore", fullAddress: "ITPL Main Gate, Whitefield, Bangalore, Karnataka", lat: 12.9850, lng: 77.7289 },
  { title: "Electronic City Phase 1", subtitle: "Electronic City, Bangalore", fullAddress: "Electronic City Phase 1, Bangalore, Karnataka", lat: 12.8399, lng: 77.6770 },
  { title: "Kempegowda International Airport (BLR)", subtitle: "Devanahalli, Bangalore", fullAddress: "Kempegowda International Airport, Devanahalli, Bangalore", lat: 13.1986, lng: 77.7066 },
  { title: "KSR Bengaluru City Railway Station", subtitle: "Majestic, Bangalore", fullAddress: "KSR Bengaluru Railway Station, Majestic, Bangalore", lat: 12.9781, lng: 77.5696 },
  { title: "Manyata Tech Park", subtitle: "Nagavara, Hebbal Outer Ring Road", fullAddress: "Manyata Tech Park, Nagavara, Bangalore", lat: 13.0475, lng: 77.6197 },
  { title: "Ecospace Business Park", subtitle: "Bellandur Outer Ring Road", fullAddress: "RMZ Ecospace, Bellandur, Bangalore", lat: 12.9260, lng: 77.6841 },
];

interface PlaceResult {
  placeId: string;
  title: string;
  subtitle: string;
  fullAddress: string;
  lat: number;
  lng: number;
}

// In-memory cache for resolved coordinates
const placeCache = new Map<string, PlaceResult>();

/**
 * Photon Komoot OpenStreetMap Geocoder (Fast, typo-tolerant, global + location-biased)
 */
async function searchPhoton(query: string, lat?: number, lng?: number): Promise<PlaceResult[]> {
  try {
    let url = `https://photon.komoot.io/api/?q=${encodeURIComponent(query)}&limit=10`;
    if (lat !== undefined && lng !== undefined && !isNaN(lat) && !isNaN(lng)) {
      url += `&lat=${lat}&lon=${lng}`;
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);

    const res = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent': 'SpeedoRideHailing/2.0 (speedo-app)',
        'Accept': 'application/json',
      },
    });
    clearTimeout(timeout);

    if (!res.ok) return [];
    const json: any = await res.json();
    const features: any[] = json.features || [];

    return features.map((f: any) => {
      const p = f.properties || {};
      const coords = f.geometry?.coordinates || [];
      const fLng = Number(coords[0] || 0.0);
      const fLat = Number(coords[1] || 0.0);

      const name = p.name || p.street || query;
      const subtitleParts = [p.street, p.district, p.city, p.state, p.country].filter(Boolean);
      // Remove duplicate of name from subtitle
      const filteredSubtitle = subtitleParts.filter((part: string) => part.toLowerCase() !== name.toLowerCase());
      const subtitle = filteredSubtitle.slice(0, 3).join(', ') || p.country || '';
      const fullAddress = [name, subtitle].filter(Boolean).join(', ');

      const item: PlaceResult = {
        placeId: `photon_${p.osm_id || Math.random().toString(36).substring(2, 9)}`,
        title: name,
        subtitle: subtitle,
        fullAddress: fullAddress,
        lat: fLat,
        lng: fLng,
      };

      if (item.placeId && item.lat && item.lng) {
        placeCache.set(item.placeId, item);
      }

      return item;
    }).filter((item: PlaceResult) => item.lat !== 0.0 && item.lng !== 0.0);
  } catch (err: any) {
    console.warn('[PlacesRoutes] Photon autocomplete warning:', err.message);
    return [];
  }
}

/**
 * OpenStreetMap Nominatim Search (Comprehensive fallback geocoder)
 */
async function searchNominatim(query: string, lat?: number, lng?: number): Promise<PlaceResult[]> {
  try {
    let url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(query)}&format=json&addressdetails=1&limit=8`;
    if (lat !== undefined && lng !== undefined && !isNaN(lat) && !isNaN(lng)) {
      const minLon = lng - 0.7;
      const maxLon = lng + 0.7;
      const minLat = lat - 0.7;
      const maxLat = lat + 0.7;
      url += `&viewbox=${minLon},${maxLat},${maxLon},${minLat}&bounded=0`;
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 4000);

    const res = await fetch(url, {
      signal: controller.signal,
      headers: {
        'User-Agent': 'SpeedoRideHailing/2.0 (contact@speedo.com)',
        'Accept': 'application/json',
      },
    });
    clearTimeout(timeout);

    if (!res.ok) return [];
    const items: any = await res.json();
    if (!Array.isArray(items)) return [];

    return items.map((item: any) => {
      const name = item.namedetails?.name || item.name || item.display_name?.split(',')[0] || query;
      const full = item.display_name || name;
      const parts = full.split(',').map((s: string) => s.trim());
      const subtitle = parts.slice(1, 4).join(', ');

      const placeItem: PlaceResult = {
        placeId: `nom_${item.osm_id || item.place_id}`,
        title: name,
        subtitle: subtitle,
        fullAddress: full,
        lat: parseFloat(item.lat),
        lng: parseFloat(item.lon),
      };

      if (placeItem.placeId && placeItem.lat && placeItem.lng) {
        placeCache.set(placeItem.placeId, placeItem);
      }

      return placeItem;
    }).filter((item: PlaceResult) => !isNaN(item.lat) && !isNaN(item.lng));
  } catch (err: any) {
    console.warn('[PlacesRoutes] Nominatim search warning:', err.message);
    return [];
  }
}

/**
 * Autocomplete Search
 * GET /api/places/autocomplete?input=Airport&lat=31.5204&lng=74.3587
 */
router.get('/autocomplete', async (req: Request, res: Response) => {
  const input = String(req.query.input || '').trim();
  const lat = req.query.lat ? parseFloat(String(req.query.lat)) : undefined;
  const lng = req.query.lng ? parseFloat(String(req.query.lng)) : undefined;

  // When input is empty, return popular presets for the city or default hub
  if (!input) {
    return res.json({
      success: true,
      source: 'preset',
      data: PRESET_PLACES.slice(0, 8),
    });
  }

  const apiKey = process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyAl4c7ort-C7mVrM-4eZKvqucgWkt03X1E';

  // 1. Try Google Places API (New) if active & enabled on Google Cloud
  if (apiKey) {
    try {
      const payload: any = { input };
      if (lat !== undefined && lng !== undefined && !isNaN(lat) && !isNaN(lng)) {
        payload.locationBias = {
          circle: {
            center: { latitude: lat, longitude: lng },
            radius: 50000.0,
          },
        };
      }

      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 3500);

      const gResponse = await fetch('https://places.googleapis.com/v1/places:autocomplete', {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'Content-Type': 'application/json',
          'X-Goog-Api-Key': apiKey,
        },
        body: JSON.stringify(payload),
      });
      clearTimeout(timeout);

      if (gResponse.ok) {
        const gData: any = await gResponse.json();
        const suggestions = (gData.suggestions || []).map((s: any) => {
          const pred = s.placePrediction;
          return {
            placeId: pred?.placeId,
            title: pred?.structuredFormat?.mainText?.text || pred?.text?.text || input,
            subtitle: pred?.structuredFormat?.secondaryText?.text || '',
            fullAddress: pred?.text?.text || '',
            lat: lat || 0.0,
            lng: lng || 0.0,
          };
        });

        if (suggestions.length > 0) {
          return res.json({ success: true, source: 'google', data: suggestions });
        }
      }
    } catch (err: any) {
      console.warn('[PlacesRoutes] Google Places API autocomplete skipped:', err.message);
    }
  }

  // 2. Primary Fast Real-World Geocoder: Photon (OpenStreetMap global search with GPS location bias)
  const photonResults = await searchPhoton(input, lat, lng);
  if (photonResults.length > 0) {
    return res.json({
      success: true,
      source: 'photon',
      data: photonResults,
    });
  }

  // 3. Secondary Real-World Geocoder: Nominatim (OpenStreetMap full directory)
  const nominatimResults = await searchNominatim(input, lat, lng);
  if (nominatimResults.length > 0) {
    return res.json({
      success: true,
      source: 'nominatim',
      data: nominatimResults,
    });
  }

  // 4. Offline Fallback: Only return presets if they actually match the search term
  const matched = PRESET_PLACES.filter(
    (p) =>
      p.title.toLowerCase().includes(input.toLowerCase()) ||
      p.subtitle.toLowerCase().includes(input.toLowerCase()) ||
      p.fullAddress.toLowerCase().includes(input.toLowerCase())
  );

  return res.json({
    success: true,
    source: 'preset_match',
    data: matched,
  });
});

/**
 * Place Details (Resolve Coordinates)
 * GET /api/places/details?placeId=...
 */
router.get('/details', async (req: Request, res: Response) => {
  const placeId = String(req.query.placeId || '').trim();
  if (!placeId) {
    return res.status(400).json({ success: false, message: 'placeId is required' });
  }

  // Check cache first
  if (placeCache.has(placeId)) {
    return res.json({ success: true, data: placeCache.get(placeId) });
  }

  // If placeId contains encoded coordinates (lat_lng)
  const coordMatch = placeId.match(/^(-?\d+\.\d+)[_,](-?\d+\.\d+)$/);
  if (coordMatch) {
    const lat = parseFloat(coordMatch[1]);
    const lng = parseFloat(coordMatch[2]);
    return res.json({
      success: true,
      data: {
        placeId,
        title: "Selected Location",
        fullAddress: `${lat.toFixed(5)}, ${lng.toFixed(5)}`,
        lat,
        lng,
      }
    });
  }

  const apiKey = process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyAl4c7ort-C7mVrM-4eZKvqucgWkt03X1E';

  if (apiKey && !placeId.startsWith('photon_') && !placeId.startsWith('nom_')) {
    try {
      const gResponse = await fetch(`https://places.googleapis.com/v1/places/${encodeURIComponent(placeId)}`, {
        headers: {
          'X-Goog-Api-Key': apiKey,
          'X-Goog-FieldMask': 'id,displayName,formattedAddress,location',
        },
      });

      if (gResponse.ok) {
        const data: any = await gResponse.json();
        const details: PlaceResult = {
          placeId: data.id,
          title: data.displayName?.text || 'Location',
          subtitle: data.formattedAddress || '',
          fullAddress: data.formattedAddress || '',
          lat: data.location?.latitude || 0.0,
          lng: data.location?.longitude || 0.0,
        };
        placeCache.set(placeId, details);
        return res.json({
          success: true,
          data: details,
        });
      }
    } catch (err: any) {
      console.warn('[PlacesRoutes] Google Place Details error:', err.message);
    }
  }

  // Fallback lookup in PRESET_PLACES
  const preset = PRESET_PLACES.find((p) => p.title.toLowerCase() === placeId.toLowerCase());
  if (preset) {
    return res.json({ success: true, data: preset });
  }

  return res.status(404).json({ success: false, message: 'Place coordinates could not be resolved' });
});

export default router;
