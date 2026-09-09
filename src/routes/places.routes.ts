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

/**
 * Autocomplete Search
 * GET /api/places/autocomplete?input=Airport&lat=12.9716&lng=77.5946
 */
router.get('/autocomplete', async (req: Request, res: Response) => {
  const input = String(req.query.input || '').trim();
  const lat = req.query.lat ? parseFloat(String(req.query.lat)) : undefined;
  const lng = req.query.lng ? parseFloat(String(req.query.lng)) : undefined;

  if (!input) {
    return res.json({
      success: true,
      data: PRESET_PLACES.slice(0, 8),
    });
  }

  const apiKey = process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyAl4c7ort-C7mVrM-4eZKvqucgWkt03X1E';

  // 1. Try Google Places API (New)
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

      const gResponse = await fetch('https://places.googleapis.com/v1/places:autocomplete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Goog-Api-Key': apiKey,
        },
        body: JSON.stringify(payload),
      });

      if (gResponse.ok) {
        const gData: any = await gResponse.json();
        const suggestions = (gData.suggestions || []).map((s: any) => {
          const pred = s.placePrediction;
          return {
            placeId: pred?.placeId,
            title: pred?.structuredFormat?.mainText?.text || pred?.text?.text || 'Location',
            subtitle: pred?.structuredFormat?.secondaryText?.text || '',
            fullAddress: pred?.text?.text || '',
          };
        });

        if (suggestions.length > 0) {
          return res.json({ success: true, source: 'google', data: suggestions });
        }
      }
    } catch (err: any) {
      console.warn('[PlacesRoutes] Google Places API autocomplete error:', err.message);
    }
  }

  // 2. Fallback: Preset match + Nominatim/Photon
  const matched = PRESET_PLACES.filter(
    (p) =>
      p.title.toLowerCase().includes(input.toLowerCase()) ||
      p.subtitle.toLowerCase().includes(input.toLowerCase()) ||
      p.fullAddress.toLowerCase().includes(input.toLowerCase())
  );

  return res.json({
    success: true,
    source: 'fallback',
    data: matched.length > 0 ? matched : PRESET_PLACES.slice(0, 5),
  });
});

/**
 * Place Details (Resolve Coordinates)
 * GET /api/places/details?placeId=ChIJ...
 */
router.get('/details', async (req: Request, res: Response) => {
  const placeId = String(req.query.placeId || '').trim();
  if (!placeId) {
    return res.status(400).json({ success: false, message: 'placeId is required' });
  }

  const apiKey = process.env.GOOGLE_MAPS_API_KEY || 'AIzaSyAl4c7ort-C7mVrM-4eZKvqucgWkt03X1E';

  if (apiKey) {
    try {
      const gResponse = await fetch(`https://places.googleapis.com/v1/places/${encodeURIComponent(placeId)}`, {
        headers: {
          'X-Goog-Api-Key': apiKey,
          'X-Goog-FieldMask': 'id,displayName,formattedAddress,location',
        },
      });

      if (gResponse.ok) {
        const data: any = await gResponse.json();
        return res.json({
          success: true,
          data: {
            placeId: data.id,
            title: data.displayName?.text,
            fullAddress: data.formattedAddress,
            lat: data.location?.latitude,
            lng: data.location?.longitude,
          },
        });
      }
    } catch (err: any) {
      console.warn('[PlacesRoutes] Google Place Details error:', err.message);
    }
  }

  return res.status(404).json({ success: false, message: 'Place coordinates could not be resolved' });
});

export default router;
