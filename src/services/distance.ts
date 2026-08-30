// Haversine formula to compute great-circle distance between two coordinates in km
export function calculateDistanceKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number
): number {
  const R = 6371; // Earth's radius in km
  const dLat = deg2rad(lat2 - lat1);
  const dLon = deg2rad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const d = R * c;
  return Math.round(d * 100) / 100;
}

function deg2rad(deg: number): number {
  return deg * (Math.PI / 180);
}

export type VehicleType = 'bike' | 'auto' | 'cab';

export interface FareEstimate {
  vehicleType: VehicleType;
  displayName: string;
  description: string;
  baseFare: number;
  perKmRate: number;
  totalFare: number;
  distanceKm: number;
  estimatedTimeMin: number;
}

export function normalizeVehicleType(type: string): VehicleType {
  const t = (type || '').toLowerCase().trim().replace(/[\s_-]+/g, '');
  if (t.includes('moto') || t.includes('bike')) return 'bike';
  if (t.includes('toto') || t.includes('auto')) return 'auto';
  if (t.includes('4') || t.includes('cab') || t.includes('car')) return 'cab';
  return 'bike';
}

export function calculateFares(distanceKm: number, surgeMultiplier: number = 1.0): Record<VehicleType, FareEstimate> {
  const safeDistance = Math.max(distanceKm, 0.5);
  const surge = Math.max(1.0, surgeMultiplier);

  // Speedo Moto pricing: Base ₹20 (includes 1.5km), ₹8/km after, avg 30km/h in traffic
  const bikeFare = Math.round((20 + Math.max(0, safeDistance - 1.5) * 8) * surge);
  const bikeTime = Math.max(3, Math.round((safeDistance / 30) * 60));

  // Speedo Toto pricing: Base ₹30 (includes 1.5km), ₹13/km after, avg 25km/h in traffic
  const autoFare = Math.round((30 + Math.max(0, safeDistance - 1.5) * 13) * surge);
  const autoTime = Math.max(4, Math.round((safeDistance / 25) * 60));

  // Speedo 4 pricing: Base ₹50 (includes 2km), ₹18/km after, avg 22km/h in traffic
  const cabFare = Math.round((50 + Math.max(0, safeDistance - 2.0) * 18) * surge);
  const cabTime = Math.max(5, Math.round((safeDistance / 22) * 60));

  return {
    bike: {
      vehicleType: 'bike',
      displayName: 'Speedo Moto',
      description: 'Fastest for solo travel & traffic',
      baseFare: Math.round(20 * surge),
      perKmRate: Math.round(8 * surge),
      totalFare: bikeFare,
      distanceKm: safeDistance,
      estimatedTimeMin: bikeTime,
    },
    auto: {
      vehicleType: 'auto',
      displayName: 'Speedo Toto',
      description: 'Affordable & comfortable 3-seater',
      baseFare: Math.round(30 * surge),
      perKmRate: Math.round(13 * surge),
      totalFare: autoFare,
      distanceKm: safeDistance,
      estimatedTimeMin: autoTime,
    },
    cab: {
      vehicleType: 'cab',
      displayName: 'Speedo 4',
      description: 'AC 4-wheeler with premium comfort',
      baseFare: Math.round(50 * surge),
      perKmRate: Math.round(18 * surge),
      totalFare: cabFare,
      distanceKm: safeDistance,
      estimatedTimeMin: cabTime,
    },
  };
}

// Generates a 4-digit OTP for ride verification
export function generateRideOtp(): string {
  return Math.floor(1000 + Math.random() * 9000).toString();
}
