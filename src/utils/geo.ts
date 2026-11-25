import { NavigationGuidanceState, RouteInstruction, RoutePoint } from '../types/convoy';

const EARTH_RADIUS_METERS = 6_371_000;

export interface LatLngPoint {
  lat: number;
  lng: number;
}

export function haversineMeters(a: LatLngPoint, b: LatLngPoint): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(b.lat - a.lat);
  const dLng = toRad(b.lng - a.lng);
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);

  const sinDLat = Math.sin(dLat / 2);
  const sinDLng = Math.sin(dLng / 2);
  const h =
    sinDLat * sinDLat +
    Math.cos(lat1) * Math.cos(lat2) * sinDLng * sinDLng;
  return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(h)));
}

export function computePolylineLengthMeters(points: LatLngPoint[]): number {
  if (points.length < 2) return 0;
  let total = 0;
  for (let i = 0; i < points.length - 1; i++) {
    total += haversineMeters(points[i], points[i + 1]);
  }
  return Math.round(total);
}

/**
 * Projects lat/lng onto a local tangent plane around a reference point to compute
 * exact perpendicular distance to the route polyline in real meters + normalized route progress (0..1).
 */
export function evaluatePositionAlongRoute(
  pos: LatLngPoint,
  waypoints: RoutePoint[],
  totalRouteMetersFallback = 12000
): {
  routeProgress: number;
  distanceTraveledMeters: number;
  remainingMeters: number;
  offRouteMeters: number;
} {
  if (waypoints.length < 2) {
    const dest = waypoints[0];
    const dist = dest
      ? haversineMeters(pos, { lat: dest.latitude, lng: dest.longitude })
      : 0;
    return {
      routeProgress: 0,
      distanceTraveledMeters: 0,
      remainingMeters: Math.round(dist),
      offRouteMeters: Math.round(dist),
    };
  }

  const refLatRad = (waypoints[0].latitude * Math.PI) / 180;
  const metersPerDegLat = 111_132.92;
  const metersPerDegLng = 111_412.84 * Math.cos(refLatRad);

  const toXY = (pt: LatLngPoint) => ({
    x: (pt.lng - waypoints[0].longitude) * metersPerDegLng,
    y: (pt.lat - waypoints[0].latitude) * metersPerDegLat,
  });

  const pXY = toXY(pos);
  let cumulativeMeters = 0;
  const segStarts: number[] = [];
  const segLengths: number[] = [];

  for (let i = 0; i < waypoints.length - 1; i++) {
    segStarts.push(cumulativeMeters);
    const len = haversineMeters(
      { lat: waypoints[i].latitude, lng: waypoints[i].longitude },
      { lat: waypoints[i + 1].latitude, lng: waypoints[i + 1].longitude }
    );
    segLengths.push(len);
    cumulativeMeters += len;
  }

  const totalMeters = Math.max(100, cumulativeMeters || totalRouteMetersFallback);

  let minPerpMeters = Number.MAX_VALUE;
  let bestTraveledMeters = 0;

  for (let i = 0; i < waypoints.length - 1; i++) {
    const a = toXY({ lat: waypoints[i].latitude, lng: waypoints[i].longitude });
    const b = toXY({
      lat: waypoints[i + 1].latitude,
      lng: waypoints[i + 1].longitude,
    });
    const abx = b.x - a.x;
    const aby = b.y - a.y;
    const abLenSq = abx * abx + aby * aby;
    const t =
      abLenSq > 0
        ? Math.max(
            0,
            Math.min(1, ((pXY.x - a.x) * abx + (pXY.y - a.y) * aby) / abLenSq)
          )
        : 0;
    const projX = a.x + t * abx;
    const projY = a.y + t * aby;
    const perpDist = Math.hypot(pXY.x - projX, pXY.y - projY);

    if (perpDist < minPerpMeters) {
      minPerpMeters = perpDist;
      bestTraveledMeters = segStarts[i] + t * segLengths[i];
    }
  }

  const routeProgress = Math.max(0, Math.min(1, bestTraveledMeters / totalMeters));
  const remainingMeters = Math.max(0, Math.round(totalMeters - bestTraveledMeters));

  return {
    routeProgress,
    distanceTraveledMeters: Math.round(bestTraveledMeters),
    remainingMeters,
    offRouteMeters: Math.round(minPerpMeters === Number.MAX_VALUE ? 0 : minPerpMeters),
  };
}

export function computeBearingDegrees(a: LatLngPoint, b: LatLngPoint): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const toDeg = (rad: number) => (rad * 180) / Math.PI;
  const lat1 = toRad(a.lat);
  const lat2 = toRad(b.lat);
  const dLng = toRad(b.lng - a.lng);

  const y = Math.sin(dLng) * Math.cos(lat2);
  const x =
    Math.cos(lat1) * Math.sin(lat2) -
    Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);
  const deg = (toDeg(Math.atan2(y, x)) + 360) % 360;
  return Math.round(deg);
}

/**
 * Encodes waypoints array into Google Encoded Polyline format so routes stay compact in Firestore.
 */
export function encodePolyline(points: LatLngPoint[]): string {
  let lastLat = 0;
  let lastLng = 0;
  let result = '';

  const encodeValue = (v: number) => {
    let val = v < 0 ? ~(v << 1) : v << 1;
    let out = '';
    while (val >= 0x20) {
      out += String.fromCharCode((0x20 | (val & 0x1f)) + 63);
      val >>= 5;
    }
    out += String.fromCharCode(val + 63);
    return out;
  };

  for (const pt of points) {
    const lat = Math.round(pt.lat * 1e5);
    const lng = Math.round(pt.lng * 1e5);
    result += encodeValue(lat - lastLat);
    result += encodeValue(lng - lastLng);
    lastLat = lat;
    lastLng = lng;
  }
  return result;
}

export function decodePolyline(encoded: string): LatLngPoint[] {
  if (!encoded) return [];
  const points: LatLngPoint[] = [];
  let index = 0;
  let lat = 0;
  let lng = 0;

  while (index < encoded.length) {
    let b = 0;
    let shift = 0;
    let result = 0;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20 && index < encoded.length);
    const dLat = result & 1 ? ~(result >> 1) : result >> 1;
    lat += dLat;

    shift = 0;
    result = 0;
    do {
      b = encoded.charCodeAt(index++) - 63;
      result |= (b & 0x1f) << shift;
      shift += 5;
    } while (b >= 0x20 && index < encoded.length);
    const dLng = result & 1 ? ~(result >> 1) : result >> 1;
    lng += dLng;

    points.push({ lat: lat / 1e5, lng: lng / 1e5 });
  }

  return points;
}

export function hasValidGeoCoord(lat: number, lng: number): boolean {
  return (
    Number.isFinite(lat) &&
    Number.isFinite(lng) &&
    (Math.abs(lat) > 0.001 || Math.abs(lng) > 0.001)
  );
}

/**
 * Keeps GPS track vertices spaced by at least minSpacingMeters and capped at maxPoints.
 */
export function downsampleTrackPoints(
  points: LatLngPoint[],
  minSpacingMeters = 15,
  maxPoints = 150
): LatLngPoint[] {
  if (points.length === 0) return [];
  const spaced: LatLngPoint[] = [points[0]];
  for (let i = 1; i < points.length - 1; i++) {
    const prev = spaced[spaced.length - 1];
    if (haversineMeters(prev, points[i]) >= minSpacingMeters) {
      spaced.push(points[i]);
    }
  }
  if (points.length > 1) {
    spaced.push(points[points.length - 1]);
  }
  if (spaced.length <= maxPoints) return spaced;

  const step = (spaced.length - 1) / (maxPoints - 1);
  const result: LatLngPoint[] = [];
  for (let i = 0; i < maxPoints; i++) {
    const idx = i === maxPoints - 1 ? spaced.length - 1 : Math.round(i * step);
    result.push(spaced[idx]);
  }
  return result;
}

/**
 * Downsamples a polyline to at most maxPoints vertices and computes aspect-preserving xFraction/yFraction.
 */
export function downsampleRoutePoints(
  points: LatLngPoint[],
  maxPoints = 60
): RoutePoint[] {
  if (points.length === 0) return [];
  const sampled: LatLngPoint[] =
    points.length <= maxPoints
      ? points
      : points.filter(
          (_, idx) =>
            idx === 0 ||
            idx === points.length - 1 ||
            idx % Math.ceil(points.length / maxPoints) === 0
        );

  let minLat = Infinity;
  let maxLat = -Infinity;
  let minLng = Infinity;
  let maxLng = -Infinity;
  for (const pt of sampled) {
    if (pt.lat < minLat) minLat = pt.lat;
    if (pt.lat > maxLat) maxLat = pt.lat;
    if (pt.lng < minLng) minLng = pt.lng;
    if (pt.lng > maxLng) maxLng = pt.lng;
  }
  const avgLatRad = (((minLat + maxLat) / 2) * Math.PI) / 180;
  const lngScale = Math.max(0.2, Math.cos(avgLatRad));
  const rawLatSpan = Math.max(0.002, maxLat - minLat);
  const rawLngSpan = Math.max(0.002, (maxLng - minLng) * lngScale);
  const maxSpan = Math.max(rawLatSpan, rawLngSpan);
  const centerLat = (minLat + maxLat) / 2;
  const centerLng = (minLng + maxLng) / 2;

  return sampled.map((pt) => ({
    latitude: pt.lat,
    longitude: pt.lng,
    xFraction: Math.max(
      0.08,
      Math.min(0.92, 0.5 + (((pt.lng - centerLng) * lngScale) / maxSpan) * 0.8)
    ),
    yFraction: Math.max(
      0.08,
      Math.min(0.92, 0.5 - ((pt.lat - centerLat) / maxSpan) * 0.8)
    ),
  }));
}

export interface ProjectedViewportPoint {
  x: number;
  y: number;
}

export interface ProjectedMapTile {
  key: string;
  z: number;
  x: number;
  y: number;
  left: number;
  top: number;
  size: number;
}

export interface TripMapViewportProjection {
  waypoints: ProjectedViewportPoint[];
  checkpoints: ProjectedViewportPoint[];
  tiles: ProjectedMapTile[];
  hasGeo: boolean;
  tileZoom: number;
}

function mercatorNormX(lng: number): number {
  return (lng + 180) / 360;
}

function mercatorNormY(lat: number): number {
  const clamped = Math.max(-85.05112878, Math.min(85.05112878, lat));
  const sinLat = Math.sin((clamped * Math.PI) / 180);
  return 0.5 - Math.log((1 + sinLat) / (1 - sinLat)) / (4 * Math.PI);
}

/**
 * Projects a trip's waypoints and checkpoints using exact Web Mercator coordinates
 * and generates the matching TomTom raster tile grid covering the viewport.
 */
export function projectTripMapViewport(
  waypoints: RoutePoint[],
  checkpoints: Array<{ latitude: number; longitude: number; xFraction: number; yFraction: number }>,
  width: number,
  height: number,
  paddingFraction = 0.14
): TripMapViewportProjection {
  const validGeoWaypoints = waypoints.filter((w) =>
    hasValidGeoCoord(w.latitude, w.longitude)
  );
  const validGeoCheckpoints = checkpoints.filter((c) =>
    hasValidGeoCoord(c.latitude, c.longitude)
  );

  const allGeo = [
    ...validGeoWaypoints.map((w) => ({ lat: w.latitude, lng: w.longitude })),
    ...validGeoCheckpoints.map((c) => ({ lat: c.latitude, lng: c.longitude })),
  ];

  const padX = width * paddingFraction;
  const padY = height * paddingFraction;
  const usableW = Math.max(20, width - padX * 2);
  const usableH = Math.max(20, height - padY * 2);

  if (allGeo.length === 0) {
    return {
      waypoints: waypoints.map((pt) => ({
        x: padX + pt.xFraction * usableW,
        y: padY + pt.yFraction * usableH,
      })),
      checkpoints: checkpoints.map((cp) => ({
        x: padX + cp.xFraction * usableW,
        y: padY + cp.yFraction * usableH,
      })),
      tiles: [],
      hasGeo: false,
      tileZoom: 12,
    };
  }

  let minX = Infinity;
  let maxX = -Infinity;
  let minY = Infinity;
  let maxY = -Infinity;
  for (const pt of allGeo) {
    const nx = mercatorNormX(pt.lng);
    const ny = mercatorNormY(pt.lat);
    if (nx < minX) minX = nx;
    if (nx > maxX) maxX = nx;
    if (ny < minY) minY = ny;
    if (ny > maxY) maxY = ny;
  }

  // Minimum span ~ 400m so single/short tracks get a sensible street zoom
  const minNormSpan = 0.000018;
  const spanX = Math.max(minNormSpan, maxX - minX);
  const spanY = Math.max(minNormSpan, maxY - minY);

  const worldNeededX = usableW / (spanX * 256);
  const worldNeededY = usableH / (spanY * 256);
  const scaleZoom = Math.max(
    4,
    Math.min(16.4, Math.log2(Math.min(worldNeededX, worldNeededY)))
  );
  const tileZoom = Math.max(4, Math.min(16, Math.floor(scaleZoom)));
  const tileCountPerAxis = 1 << tileZoom;

  const totalWorldPx = 256 * Math.pow(2, scaleZoom);
  const centerWorldX = ((minX + maxX) / 2) * totalWorldPx;
  const centerWorldY = ((minY + maxY) / 2) * totalWorldPx;

  const projectLatLng = (lat: number, lng: number): ProjectedViewportPoint => ({
    x: width / 2 + (mercatorNormX(lng) * totalWorldPx - centerWorldX),
    y: height / 2 + (mercatorNormY(lat) * totalWorldPx - centerWorldY),
  });

  const tileSizePx = 256 * Math.pow(2, scaleZoom - tileZoom);
  const worldLeftPx = centerWorldX - width / 2;
  const worldTopPx = centerWorldY - height / 2;
  const worldRightPx = centerWorldX + width / 2;
  const worldBottomPx = centerWorldY + height / 2;

  const txStart = Math.floor(worldLeftPx / tileSizePx);
  const txEnd = Math.floor(worldRightPx / tileSizePx);
  const tyStart = Math.max(0, Math.floor(worldTopPx / tileSizePx));
  const tyEnd = Math.min(tileCountPerAxis - 1, Math.floor(worldBottomPx / tileSizePx));

  const tiles: ProjectedMapTile[] = [];
  for (let ty = tyStart; ty <= tyEnd; ty++) {
    for (let tx = txStart; tx <= txEnd; tx++) {
      const wrappedX = ((tx % tileCountPerAxis) + tileCountPerAxis) % tileCountPerAxis;
      tiles.push({
        key: `${tileZoom}-${wrappedX}-${ty}-${tx}`,
        z: tileZoom,
        x: wrappedX,
        y: ty,
        left: width / 2 + (tx * tileSizePx - centerWorldX),
        top: height / 2 + (ty * tileSizePx - centerWorldY),
        size: tileSizePx,
      });
    }
  }

  return {
    waypoints: waypoints.map((pt) =>
      hasValidGeoCoord(pt.latitude, pt.longitude)
        ? projectLatLng(pt.latitude, pt.longitude)
        : { x: padX + pt.xFraction * usableW, y: padY + pt.yFraction * usableH }
    ),
    checkpoints: checkpoints.map((cp) =>
      hasValidGeoCoord(cp.latitude, cp.longitude)
        ? projectLatLng(cp.latitude, cp.longitude)
        : { x: padX + cp.xFraction * usableW, y: padY + cp.yFraction * usableH }
    ),
    tiles,
    hasGeo: true,
    tileZoom,
  };
}

/**
 * Projects a lat/lng point forward along a compass bearing by offsetMeters (used for GPS cockpit look-ahead).
 */
export function projectAheadPoint(
  lat: number,
  lng: number,
  headingDeg: number,
  offsetMeters: number
): LatLngPoint {
  if (!Number.isFinite(lat) || !Number.isFinite(lng) || offsetMeters <= 0) {
    return { lat, lng };
  }
  const rad = (headingDeg * Math.PI) / 180;
  const dLat = (offsetMeters * Math.cos(rad)) / 111_132.92;
  const cosLat = Math.max(0.2, Math.cos((lat * Math.PI) / 180));
  const dLng = (offsetMeters * Math.sin(rad)) / (111_412.84 * cosLat);
  return {
    lat: lat + dLat,
    lng: lng + dLng,
  };
}

function formatNavMetersLabel(meters: number): string {
  if (meters >= 1000) {
    return `${(meters / 1000).toFixed(1)} km`;
  }
  const rounded = Math.max(10, Math.round(meters / 10) * 10);
  return `${rounded} m`;
}

/**
 * Computes heading-up bearing + next turn instruction from TomTom routeInstructions or polyline geometry fallback.
 */
export function computeNavigationGuidance(params: {
  pos: LatLngPoint;
  speedKmh: number;
  gpsHeading?: number;
  waypoints: RoutePoint[];
  instructions?: RouteInstruction[];
  destinationName: string;
  totalRouteMeters: number;
  offRouteMeters?: number;
  offRouteToleranceMeters?: number;
}): NavigationGuidanceState {
  const {
    pos,
    speedKmh,
    gpsHeading,
    waypoints,
    instructions = [],
    destinationName,
    totalRouteMeters,
    offRouteMeters = 0,
    offRouteToleranceMeters = 75,
  } = params;

  const isOffRoute = offRouteMeters > offRouteToleranceMeters;

  if (waypoints.length < 2) {
    const dest = waypoints[0];
    const dist = dest ? haversineMeters(pos, { lat: dest.latitude, lng: dest.longitude }) : 0;
    const bearing = dest
      ? computeBearingDegrees(pos, { lat: dest.latitude, lng: dest.longitude })
      : Math.round(gpsHeading ?? 0);
    return {
      maneuver: dist <= 60 ? 'ARRIVE' : 'STRAIGHT',
      instructionText:
        dist <= 60 ? `Anda tiba di ${destinationName}` : `Menuju ${destinationName}`,
      streetName: destinationName,
      distanceToTurnMeters: Math.round(dist),
      distanceToTurnLabel: formatNavMetersLabel(dist),
      routeBearingDeg: gpsHeading && gpsHeading > 0 ? Math.round(gpsHeading) : bearing,
      speedKmh: Math.max(0, Math.round(speedKmh)),
      isOffRoute,
    };
  }

  // Find closest segment and cumulative traveled distance
  const segStarts: number[] = [];
  const segLengths: number[] = [];
  let cumulative = 0;
  for (let i = 0; i < waypoints.length - 1; i++) {
    segStarts.push(cumulative);
    const len = haversineMeters(
      { lat: waypoints[i].latitude, lng: waypoints[i].longitude },
      { lat: waypoints[i + 1].latitude, lng: waypoints[i + 1].longitude }
    );
    segLengths.push(len);
    cumulative += len;
  }

  const refLatRad = (waypoints[0].latitude * Math.PI) / 180;
  const metersPerDegLat = 111_132.92;
  const metersPerDegLng = 111_412.84 * Math.cos(refLatRad);
  const toXY = (pt: LatLngPoint) => ({
    x: (pt.lng - waypoints[0].longitude) * metersPerDegLng,
    y: (pt.lat - waypoints[0].latitude) * metersPerDegLat,
  });

  const pXY = toXY(pos);
  let bestSegIdx = 0;
  let minDist = Number.MAX_VALUE;
  let bestTraveled = 0;

  for (let i = 0; i < waypoints.length - 1; i++) {
    const a = toXY({ lat: waypoints[i].latitude, lng: waypoints[i].longitude });
    const b = toXY({
      lat: waypoints[i + 1].latitude,
      lng: waypoints[i + 1].longitude,
    });
    const abx = b.x - a.x;
    const aby = b.y - a.y;
    const abLenSq = abx * abx + aby * aby;
    const t =
      abLenSq > 0
        ? Math.max(0, Math.min(1, ((pXY.x - a.x) * abx + (pXY.y - a.y) * aby) / abLenSq))
        : 0;
    const projX = a.x + t * abx;
    const projY = a.y + t * aby;
    const perp = Math.hypot(pXY.x - projX, pXY.y - projY);
    if (perp < minDist) {
      minDist = perp;
      bestSegIdx = i;
      bestTraveled = segStarts[i] + t * segLengths[i];
    }
  }

  const segBearing = computeBearingDegrees(
    { lat: waypoints[bestSegIdx].latitude, lng: waypoints[bestSegIdx].longitude },
    {
      lat: waypoints[bestSegIdx + 1].latitude,
      lng: waypoints[bestSegIdx + 1].longitude,
    }
  );
  const effectiveBearing =
    typeof gpsHeading === 'number' && Number.isFinite(gpsHeading) && gpsHeading > 0 && speedKmh >= 5
      ? Math.round(gpsHeading)
      : segBearing;

  const effectiveTotal = Math.max(100, cumulative || totalRouteMeters);
  const remainingTotal = Math.max(0, effectiveTotal - bestTraveled);

  if (remainingTotal <= 60) {
    return {
      maneuver: 'ARRIVE',
      instructionText: `Tiba di Tujuan: ${destinationName}`,
      streetName: destinationName,
      distanceToTurnMeters: Math.round(remainingTotal),
      distanceToTurnLabel: `${Math.round(remainingTotal)} m`,
      routeBearingDeg: effectiveBearing,
      speedKmh: Math.max(0, Math.round(speedKmh)),
      isOffRoute,
    };
  }

  // Prefer TomTom guidance instructions when available
  if (instructions.length > 0) {
    const nextIns =
      instructions.find((ins) => ins.routeOffsetInMeters >= bestTraveled + 15) ||
      instructions[instructions.length - 1];
    if (nextIns) {
      const distToTurn = Math.max(
        10,
        Math.round(
          Math.max(
            nextIns.routeOffsetInMeters - bestTraveled,
            haversineMeters(pos, { lat: nextIns.latitude, lng: nextIns.longitude }) * 0.85
          )
        )
      );
      return {
        maneuver: nextIns.maneuver || 'STRAIGHT',
        instructionText: nextIns.message,
        streetName: nextIns.street || destinationName,
        distanceToTurnMeters: distToTurn,
        distanceToTurnLabel: formatNavMetersLabel(distToTurn),
        routeBearingDeg: effectiveBearing,
        speedKmh: Math.max(0, Math.round(speedKmh)),
        isOffRoute,
      };
    }
  }

  // Offline polyline bend-detection fallback
  for (let i = bestSegIdx; i < waypoints.length - 2; i++) {
    const b1 = computeBearingDegrees(
      { lat: waypoints[i].latitude, lng: waypoints[i].longitude },
      { lat: waypoints[i + 1].latitude, lng: waypoints[i + 1].longitude }
    );
    const b2 = computeBearingDegrees(
      { lat: waypoints[i + 1].latitude, lng: waypoints[i + 1].longitude },
      { lat: waypoints[i + 2].latitude, lng: waypoints[i + 2].longitude }
    );
    let delta = b2 - b1;
    while (delta > 180) delta -= 360;
    while (delta < -180) delta += 360;

    if (Math.abs(delta) >= 22) {
      const turnVertexDist = Math.max(15, segStarts[i + 1] - bestTraveled);
      let maneuver = 'STRAIGHT';
      let label = 'Lurus ikuti jalur rombongan';
      if (Math.abs(delta) >= 135) {
        maneuver = 'MAKE_UTURN';
        label = 'Putar balik ikuti rute';
      } else if (delta >= 55) {
        maneuver = 'TURN_RIGHT';
        label = 'Belok kanan ikuti rute rombongan';
      } else if (delta >= 22) {
        maneuver = 'BEAR_RIGHT';
        label = 'Ambil jalur kanan';
      } else if (delta <= -55) {
        maneuver = 'TURN_LEFT';
        label = 'Belok kiri ikuti rute rombongan';
      } else if (delta <= -22) {
        maneuver = 'BEAR_LEFT';
        label = 'Ambil jalur kiri';
      }
      return {
        maneuver,
        instructionText: label,
        streetName: `Menuju ${destinationName}`,
        distanceToTurnMeters: Math.round(turnVertexDist),
        distanceToTurnLabel: formatNavMetersLabel(turnVertexDist),
        routeBearingDeg: effectiveBearing,
        speedKmh: Math.max(0, Math.round(speedKmh)),
        isOffRoute,
      };
    }
  }

  return {
    maneuver: 'STRAIGHT',
    instructionText: `Terus ikuti jalur menuju ${destinationName}`,
    streetName: destinationName,
    distanceToTurnMeters: Math.round(remainingTotal),
    distanceToTurnLabel: formatNavMetersLabel(remainingTotal),
    routeBearingDeg: effectiveBearing,
    speedKmh: Math.max(0, Math.round(speedKmh)),
    isOffRoute,
  };
}

export function compactInstructionsForSync(
  instructions?: RouteInstruction[],
  maxCount = 50
): Array<{
  latitude: number;
  longitude: number;
  routeOffsetInMeters: number;
  maneuver: string;
  message: string;
  street: string;
}> {
  if (!Array.isArray(instructions) || instructions.length === 0) return [];
  const cap = Math.max(1, Math.min(60, maxCount));
  const list =
    instructions.length <= cap
      ? instructions
      : instructions.filter(
          (_, idx) =>
            idx === 0 ||
            idx === instructions.length - 1 ||
            idx % Math.ceil(instructions.length / cap) === 0
        );
  return list.slice(0, cap).map((ins) => ({
    latitude: Number.isFinite(ins.latitude) ? Number(ins.latitude.toFixed(6)) : 0,
    longitude: Number.isFinite(ins.longitude) ? Number(ins.longitude.toFixed(6)) : 0,
    routeOffsetInMeters: Math.max(0, Math.round(ins.routeOffsetInMeters || 0)),
    maneuver: String(ins.maneuver || 'STRAIGHT').slice(0, 64),
    message: String(ins.message || 'Ikuti jalur').slice(0, 240),
    street: String(ins.street || '').slice(0, 120),
  }));
}

export interface ClosestCoLeaderResult {
  coLeaderUid: string | null;
  distanceByMemberId: Map<string, number>;
}

export function computeClosestCoLeader(
  members: Array<{
    id: string;
    role: string;
    connectionStatus: string;
    conditionStatus: string;
    lastUpdatedMs?: number;
    convoyOrderRank?: number;
    mapPosition: {
      latitude: number;
      longitude: number;
      routeProgress: number;
    };
  }>,
  originalLeaderUid: string,
  nowMs = Date.now(),
  staleThresholdMs = 20_000
): ClosestCoLeaderResult {
  const distanceByMemberId = new Map<string, number>();
  const leader =
    members.find((m) => m.id === originalLeaderUid) ||
    members.find((m) => m.role === 'TOUR_LEADER');

  if (!leader) {
    return { coLeaderUid: null, distanceByMemberId };
  }

  const leaderHasGeo = hasValidGeoCoord(
    leader.mapPosition.latitude,
    leader.mapPosition.longitude
  );

  const candidates: Array<{ id: string; dist: number; rank: number; preferred: boolean }> = [];

  for (const m of members) {
    if (m.id === leader.id) {
      distanceByMemberId.set(m.id, 0);
      continue;
    }

    const memberHasGeo = hasValidGeoCoord(m.mapPosition.latitude, m.mapPosition.longitude);
    const dist =
      leaderHasGeo && memberHasGeo
        ? Math.round(
            haversineMeters(
              { lat: leader.mapPosition.latitude, lng: leader.mapPosition.longitude },
              { lat: m.mapPosition.latitude, lng: m.mapPosition.longitude }
            )
          )
        : Math.round(
            Math.abs((leader.mapPosition.routeProgress ?? 0) - (m.mapPosition.routeProgress ?? 0)) *
              12000
          );

    distanceByMemberId.set(m.id, dist);

    if (m.connectionStatus === 'OFFLINE') continue;
    const isFresh =
      !m.lastUpdatedMs || m.lastUpdatedMs <= 0 || nowMs - m.lastUpdatedMs <= staleThresholdMs;
    const isHealthy = m.conditionStatus !== 'MOGOK' && m.conditionStatus !== 'SOS';
    candidates.push({
      id: m.id,
      dist,
      rank: m.convoyOrderRank ?? 999,
      preferred: isFresh && isHealthy,
    });
  }

  if (candidates.length === 0) {
    return { coLeaderUid: null, distanceByMemberId };
  }

  const preferredPool = candidates.filter((c) => c.preferred);
  const pool = preferredPool.length > 0 ? preferredPool : candidates;
  pool.sort((a, b) => {
    if (a.dist !== b.dist) return a.dist - b.dist;
    if (a.rank !== b.rank) return a.rank - b.rank;
    return a.id.localeCompare(b.id);
  });

  return {
    coLeaderUid: pool[0]?.id ?? null,
    distanceByMemberId,
  };
}


