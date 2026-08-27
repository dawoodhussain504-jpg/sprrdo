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
  baseFare: number;
  perKmRate: number;
  totalFare: number;
  distanceKm: number;
  estimatedTimeMin: number;
}

export function calculateFares(distanceKm: number): Record<VehicleType, FareEstimate> {
  const safeDistance = Math.max(distanceKm, 0.5);

  // Bike pricing: Base ₹20 (includes 1.5km), ₹8/km after, avg 30km/h in traffic
  const bikeFare = Math.round(20 + Math.max(0, safeDistance - 1.5) * 8);
  const bikeTime = Math.max(3, Math.round((safeDistance / 30) * 60));

  // Auto pricing: Base ₹30 (includes 1.5km), ₹13/km after, avg 25km/h in traffic
  const autoFare = Math.round(30 + Math.max(0, safeDistance - 1.5) * 13);
  const autoTime = Math.max(4, Math.round((safeDistance / 25) * 60));

  // Cab pricing: Base ₹50 (includes 2km), ₹18/km after, avg 22km/h in traffic
  const cabFare = Math.round(50 + Math.max(0, safeDistance - 2.0) * 18);
  const cabTime = Math.max(5, Math.round((safeDistance / 22) * 60));

  return {
    bike: {
      vehicleType: 'bike',
      baseFare: 20,
      perKmRate: 8,
      totalFare: bikeFare,
      distanceKm: safeDistance,
      estimatedTimeMin: bikeTime,
    },
    auto: {
      vehicleType: 'auto',
      baseFare: 30,
      perKmRate: 13,
      totalFare: autoFare,
      distanceKm: safeDistance,
      estimatedTimeMin: autoTime,
    },
    cab: {
      vehicleType: 'cab',
      baseFare: 50,
      perKmRate: 18,
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
