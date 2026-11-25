import { RouteInstruction, RoutePreference, VehicleType } from '../types/convoy';
import { LatLngPoint } from '../utils/geo';

export const GMP_ATTRIBUTION_ID = 'gmp_mcp_codeassist_v1_aistudio';

export interface GoogleRouteResult {
  distanceMeters: number;
  durationMillis: number;
  path: LatLngPoint[];
  instructions: RouteInstruction[];
}

export interface GooglePlaceSuggestion {
  id: string;
  name: string;
  address: string;
  prediction: google.maps.places.PlacePrediction;
}

export interface GoogleResolvedPlace {
  name: string;
  address: string;
  lat: number;
  lng: number;
}

export function isGoogleQuotaError(error: unknown): boolean {
  if (!error) return false;
  const errObj = error as {
    status?: unknown;
    code?: unknown;
    message?: unknown;
  };
  if (errObj.status === 429 || errObj.code === 429) {
    return true;
  }
  const combined = `${String(errObj.status ?? '')} ${String(errObj.code ?? '')} ${String(
    errObj.message ?? error
  )}`.toUpperCase();
  return (
    combined.includes('RESOURCE_EXHAUSTED') ||
    combined.includes('OVER_QUERY_LIMIT') ||
    combined.includes('OVER_QUOTA') ||
    combined.includes('QUOTA_EXCEEDED') ||
    combined.includes('QUOTAEXCEEDED') ||
    combined.includes('429')
  );
}

export function notifyQuotaIfTriggered(error: unknown): boolean {
  if (isGoogleQuotaError(error)) {
    window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
    return true;
  }
  return false;
}

export async function computeGoogleRoute(
  routesLib: google.maps.RoutesLibrary | null,
  waypoints: Array<{ latitude: number; longitude: number }>,
  vehicleType: VehicleType = 'MOTOR',
  routePreference: RoutePreference = 'fastest',
  useStopovers = false
): Promise<GoogleRouteResult | null> {
  if (!routesLib || waypoints.length < 2) return null;

  // Keep intermediate waypoints concise (max 12 points when multi-stop destinations, otherwise 8)
  const cap = useStopovers ? 12 : 8;
  const maxIntermediates = cap - 2;
  const sampled =
    waypoints.length <= cap
      ? waypoints
      : [
          waypoints[0],
          ...waypoints
            .slice(1, -1)
            .filter(
              (_, idx) =>
                idx % Math.ceil((waypoints.length - 2) / maxIntermediates) === 0
            )
            .slice(0, maxIntermediates),
          waypoints[waypoints.length - 1],
        ];

  const origin = { lat: sampled[0].latitude, lng: sampled[0].longitude };
  const destination = {
    lat: sampled[sampled.length - 1].latitude,
    lng: sampled[sampled.length - 1].longitude,
  };
  const intermediates = sampled.slice(1, -1).map((pt) => ({
    location: { lat: pt.latitude, lng: pt.longitude },
    via: !useStopovers,
  }));

  const buildRequest = (
    useTwoWheeler: boolean,
    includeShorterRef: boolean
  ): google.maps.routes.ComputeRoutesRequest => {
    const req: google.maps.routes.ComputeRoutesRequest = {
      origin,
      destination,
      travelMode: useTwoWheeler ? 'TWO_WHEELER' : 'DRIVING',
      routingPreference:
        routePreference === 'shortest' ? 'TRAFFIC_UNAWARE' : 'TRAFFIC_AWARE',
      fields: ['path', 'distanceMeters', 'durationMillis', 'viewport', 'legs'],
      language: 'id',
      region: 'SA',
      internalUsageAttributionIds: [GMP_ATTRIBUTION_ID],
    };

    if (intermediates.length > 0) {
      req.intermediates = intermediates;
    }
    if (routePreference === 'no_toll') {
      req.routeModifiers = { avoidTolls: true };
    }
    if (includeShorterRef && routePreference === 'shortest' && intermediates.length === 0) {
      req.requestedReferenceRoutes = ['SHORTER_DISTANCE'];
    }
    return req;
  };

  let routesResponse: {
    routes?: google.maps.routes.Route[];
  } | null = null;
  try {
    routesResponse = await routesLib.Route.computeRoutes(
      buildRequest(vehicleType === 'MOTOR', true)
    );
  } catch (err) {
    if (notifyQuotaIfTriggered(err)) return null;
    try {
      routesResponse = await routesLib.Route.computeRoutes(
        buildRequest(false, false)
      );
    } catch (fallbackErr) {
      notifyQuotaIfTriggered(fallbackErr);
      return null;
    }
  }

  const route = routesResponse?.routes?.[0];
  if (!route) return null;

  const path: LatLngPoint[] = (route.path ?? [])
    .filter((pt) => Number.isFinite(pt.lat) && Number.isFinite(pt.lng))
    .map((pt) => ({ lat: pt.lat, lng: pt.lng }));

  if (path.length === 0) return null;

  const instructions: RouteInstruction[] = [];
  let offsetMeters = 0;
  for (const leg of route.legs ?? []) {
    for (const step of leg.steps ?? []) {
      const msg = (step.instructions || '').trim();
      const loc = step.startLocation;
      if (msg && loc && Number.isFinite(loc.lat) && Number.isFinite(loc.lng)) {
        instructions.push({
          latitude: loc.lat,
          longitude: loc.lng,
          routeOffsetInMeters: offsetMeters,
          maneuver: String(step.maneuver || 'STRAIGHT'),
          message: msg.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim(),
        });
      }
      offsetMeters += Math.max(0, Math.round(Number(step.distanceMeters ?? 0)));
      if (instructions.length >= 60) break;
    }
    if (instructions.length >= 60) break;
  }

  return {
    distanceMeters: Math.max(0, Number(route.distanceMeters ?? 0)),
    durationMillis: Math.max(0, Number(route.durationMillis ?? 0)),
    path,
    instructions,
  };
}

export async function searchGoogleAutocompleteSuggestions(
  placesLib: google.maps.PlacesLibrary | null,
  query: string,
  sessionToken?: google.maps.places.AutocompleteSessionToken
): Promise<GooglePlaceSuggestion[]> {
  const trimmed = query.trim();
  if (!placesLib || trimmed.length < 2) return [];

  try {
    const request: google.maps.places.AutocompleteRequest = {
      input: trimmed,
      language: 'id',
      region: 'SA',
      includedRegionCodes: ['sa', 'id'],
      internalUsageAttributionIds: [GMP_ATTRIBUTION_ID],
    };
    if (sessionToken) {
      request.sessionToken = sessionToken;
    }

    const { suggestions } =
      await placesLib.AutocompleteSuggestion.fetchAutocompleteSuggestions(request);

    return (suggestions ?? [])
      .map((s) => s.placePrediction)
      .filter((pred): pred is google.maps.places.PlacePrediction => Boolean(pred))
      .slice(0, 6)
      .map((pred, idx) => ({
        id: pred.placeId || `pred-${idx}`,
        name: pred.mainText?.text || pred.text.text || 'Lokasi Dipilih',
        address: pred.secondaryText?.text || pred.text.text || '',
        prediction: pred,
      }));
  } catch (err) {
    notifyQuotaIfTriggered(err);
    return [];
  }
}

export async function selectGooglePlacePrediction(
  suggestion: GooglePlaceSuggestion
): Promise<GoogleResolvedPlace | null> {
  try {
    const place = suggestion.prediction.toPlace();
    await place.fetchFields({
      fields: ['displayName', 'formattedAddress', 'location', 'viewport'],
    });
    const loc = place.location;
    if (!loc) return null;
    const lat = loc.lat();
    const lng = loc.lng();
    return {
      name: place.displayName || suggestion.name,
      address:
        place.formattedAddress ||
        suggestion.address ||
        `${lat.toFixed(4)}, ${lng.toFixed(4)}`,
      lat,
      lng,
    };
  } catch (err) {
    notifyQuotaIfTriggered(err);
    return null;
  }
}

export async function reverseGeocodeGoogle(
  lat: number,
  lng: number
): Promise<{ name: string; address: string }> {
  const fallbackName = `Titik Peta (${lat.toFixed(3)}, ${lng.toFixed(3)})`;
  const fallbackAddress = `Koordinat GPS ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
  try {
    const res = await fetch(
      `/api/maps/reverse?lat=${encodeURIComponent(lat)}&lng=${encodeURIComponent(lng)}`
    );
    if (res.status === 429) {
      window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
      return { name: fallbackName, address: fallbackAddress };
    }
    if (!res.ok) {
      return { name: fallbackName, address: fallbackAddress };
    }
    const data = (await res.json()) as {
      quotaExceeded?: boolean;
      name?: string;
      address?: string;
    };
    if (data.quotaExceeded) {
      window.dispatchEvent(new CustomEvent('gmp-quota-exceeded'));
    }
    return {
      name: data.name || fallbackName,
      address: data.address || fallbackAddress,
    };
  } catch {
    return { name: fallbackName, address: fallbackAddress };
  }
}
