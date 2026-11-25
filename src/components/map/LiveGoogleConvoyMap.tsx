import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AdvancedMarker,
  ColorScheme,
  Map as GoogleMap,
  MapMouseEvent,
  useMap,
  useMapsLibrary,
} from '@vis.gl/react-google-maps';
import {
  Activity,
  ArrowUp,
  ArrowUpLeft,
  ArrowUpRight,
  Compass,
  CornerUpLeft,
  CornerUpRight,
  Flag,
  Layers,
  LocateFixed,
  Minus,
  Navigation,
  Plus,
  RotateCcw,
} from 'lucide-react';
import {
  ConvoyMember,
  ConvoyRouteConfig,
  DestinationEtaInfo,
  DestinationStop,
  RouteInstruction,
  RoutePoint,
  RoutePreference,
  SosEmergencyState,
  StragglerAlertState,
  VehicleType,
} from '../../types/convoy';
import {
  computeNavigationGuidance,
  LatLngPoint,
  projectAheadPoint,
} from '../../utils/geo';
import {
  computeGoogleRoute,
  GMP_ATTRIBUTION_ID,
} from '../../services/googleMapsService';

interface LiveGoogleConvoyMapProps {
  members: ConvoyMember[];
  focusedMember: ConvoyMember | null;
  routeConfig: ConvoyRouteConfig;
  vehicleType?: VehicleType;
  stragglerAlert: StragglerAlertState;
  sosState: SosEmergencyState;
  etaInfo: DestinationEtaInfo;
  darkMode?: boolean;
  isPickingDestination: boolean;
  isPickingCheckpoint: boolean;
  isNavigationMode?: boolean;
  cleanNavMode?: boolean;
  canPingRouting?: boolean;
  onToggleNavigationMode?: () => void;
  onSelectMember: (memberId: string) => void;
  onPickCoordinateOnMap: (lat: number, lng: number) => void;
  onRouteComputed?: (
    distanceMeters: number,
    durationMillis: number,
    path: LatLngPoint[],
    instructions?: RouteInstruction[]
  ) => void;
}

function getInitials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return 'RD';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
}

function compassDirectionLabel(deg: number): string {
  const dirs = ['U', 'TL', 'T', 'TG', 'S', 'BD', 'B', 'BL'];
  const idx = Math.round((((deg % 360) + 360) % 360) / 45) % 8;
  return dirs[idx];
}

function ManeuverIcon({ maneuver }: { maneuver: string }) {
  const upper = maneuver.toUpperCase();
  if (upper.includes('ARRIVE') || upper.includes('GOAL')) {
    return <Flag className="w-6 h-6" />;
  }
  if (upper.includes('UTURN')) {
    return <RotateCcw className="w-6 h-6" />;
  }
  if (
    upper.includes('BEAR_LEFT') ||
    upper.includes('KEEP_LEFT') ||
    upper.includes('SLIGHT_LEFT')
  ) {
    return <ArrowUpLeft className="w-6 h-6" />;
  }
  if (
    upper.includes('BEAR_RIGHT') ||
    upper.includes('KEEP_RIGHT') ||
    upper.includes('SLIGHT_RIGHT')
  ) {
    return <ArrowUpRight className="w-6 h-6" />;
  }
  if (upper.includes('LEFT')) {
    return <CornerUpLeft className="w-6 h-6" />;
  }
  if (upper.includes('RIGHT')) {
    return <CornerUpRight className="w-6 h-6" />;
  }
  return <ArrowUp className="w-6 h-6" />;
}

interface ConvoyMapControllerProps {
  waypoints: RoutePoint[];
  destinationStops?: DestinationStop[];
  vehicleType: VehicleType;
  routePref: RoutePreference;
  routeKey: string;
  canPingRouting: boolean;
  showTraffic: boolean;
  activeNavMode: boolean;
  navSubject: ConvoyMember | null;
  focusedMember: ConvoyMember | null;
  navHeadingDeg: number;
  isAutoFollowing: boolean;
  followTrigger: number;
  fitAllTrigger: number;
  zoomTrigger: { delta: number; seq: number };
  onRouteComputed?: (
    distanceMeters: number,
    durationMillis: number,
    path: LatLngPoint[],
    instructions?: RouteInstruction[]
  ) => void;
}

function ConvoyMapController({
  waypoints,
  destinationStops,
  vehicleType,
  routePref,
  routeKey,
  canPingRouting,
  showTraffic,
  activeNavMode,
  navSubject,
  focusedMember,
  navHeadingDeg,
  isAutoFollowing,
  followTrigger,
  fitAllTrigger,
  zoomTrigger,
  onRouteComputed,
}: ConvoyMapControllerProps) {
  const map = useMap();
  const mapsLib = useMapsLibrary('maps');
  const routesLib = useMapsLibrary('routes');

  const onRouteComputedRef = useRef(onRouteComputed);
  onRouteComputedRef.current = onRouteComputed;
  const lastFittedWaypointsKeyRef = useRef<string>('');
  const [routePath, setRoutePath] = useState<LatLngPoint[]>([]);

  // Live Google Maps TrafficLayer
  useEffect(() => {
    if (!map || !mapsLib) return;
    if (!showTraffic) return;
    const trafficLayer = new mapsLib.TrafficLayer();
    trafficLayer.setMap(map);
    return () => {
      trafficLayer.setMap(null);
    };
  }, [map, mapsLib, showTraffic]);

  // Compute Routes API geometry when leader pings or route changes
  useEffect(() => {
    const fallbackPts = waypoints.map((w) => ({
      lat: w.latitude,
      lng: w.longitude,
    }));
    setRoutePath(fallbackPts);

    if (!routesLib || waypoints.length < 2 || !canPingRouting) return;

    const hasMultiStops = Boolean(destinationStops && destinationStops.length >= 2);
    const routingInput = hasMultiStops
      ? [
          { latitude: waypoints[0].latitude, longitude: waypoints[0].longitude },
          ...(destinationStops ?? []).map((s) => ({
            latitude: s.latitude,
            longitude: s.longitude,
          })),
        ]
      : waypoints;

    let cancelled = false;
    computeGoogleRoute(
      routesLib,
      routingInput,
      vehicleType,
      routePref,
      hasMultiStops
    ).then(
      (result) => {
        if (cancelled || !result || result.path.length < 2) return;
        setRoutePath(result.path);
        if (onRouteComputedRef.current) {
          onRouteComputedRef.current(
            result.distanceMeters,
            result.durationMillis,
            result.path,
            result.instructions
          );
        }
      }
    );

    return () => {
      cancelled = true;
    };
  }, [routesLib, routeKey, canPingRouting]);

  // Render crisp dual-stroke route polyline
  useEffect(() => {
    if (!map || !mapsLib || routePath.length < 2) return;

    const casing = new mapsLib.Polyline({
      path: routePath,
      strokeColor: '#FFFFFF',
      strokeOpacity: 0.92,
      strokeWeight: 9,
      zIndex: 10,
      clickable: false,
      map,
    });
    const inner = new mapsLib.Polyline({
      path: routePath,
      strokeColor: activeNavMode ? '#059669' : '#047857',
      strokeOpacity: 0.96,
      strokeWeight: 6,
      zIndex: 11,
      clickable: false,
      map,
    });

    return () => {
      casing.setMap(null);
      inner.setMap(null);
    };
  }, [map, mapsLib, routePath, activeNavMode]);

  // Camera follow focused member (with cockpit look-ahead + vector tilt/heading in Nav Mode)
  useEffect(() => {
    if (!map || !navSubject) return;
    if (!isAutoFollowing && followTrigger === 0) return;

    const rawLat = navSubject.mapPosition.latitude;
    const rawLng = navSubject.mapPosition.longitude;
    const target = activeNavMode
      ? projectAheadPoint(rawLat, rawLng, navHeadingDeg, 85)
      : { lat: rawLat, lng: rawLng };

    const currentZoom = map.getZoom() ?? 12;

    if (activeNavMode) {
      map.panTo(target);
      if (currentZoom < 16 || followTrigger > 0) {
        map.setZoom(17);
      }
      try {
        map.setHeading(navHeadingDeg);
        map.setTilt(45);
      } catch {
        // Ignore if vector tilt/heading unavailable at zoom level
      }
    } else {
      try {
        map.setHeading(0);
        map.setTilt(0);
      } catch {
        // Ignore
      }
      if (followTrigger > 0 && currentZoom < 14) {
        map.setZoom(15);
      }
      map.panTo(target);
    }
  }, [
    map,
    navSubject?.id,
    navSubject?.mapPosition.latitude,
    navSubject?.mapPosition.longitude,
    activeNavMode,
    navHeadingDeg,
    isAutoFollowing,
    followTrigger,
  ]);

  // Fit entire route & focused member when exiting Nav Mode or pressing Fit All
  useEffect(() => {
    if (!map || activeNavMode) return;

    try {
      map.setHeading(0);
      map.setTilt(0);
    } catch {
      // Ignore
    }

    const wpSig = waypoints
      .map((w) => `${w.latitude.toFixed(4)},${w.longitude.toFixed(4)}`)
      .join('|');
    const shouldAutoFitOnSync =
      waypoints.length >= 2 && wpSig !== lastFittedWaypointsKeyRef.current;
    if (fitAllTrigger === 0 && !shouldAutoFitOnSync) return;
    if (waypoints.length >= 2) {
      lastFittedWaypointsKeyRef.current = wpSig;
    }

    const bounds = new google.maps.LatLngBounds();
    let count = 0;
    waypoints.forEach((w) => {
      bounds.extend({ lat: w.latitude, lng: w.longitude });
      count++;
    });
    if (focusedMember) {
      bounds.extend({
        lat: focusedMember.mapPosition.latitude,
        lng: focusedMember.mapPosition.longitude,
      });
      count++;
    }
    if (count > 0) {
      map.fitBounds(bounds, 46);
    }
  }, [map, fitAllTrigger, activeNavMode, routeKey]);

  // Zoom button controls
  useEffect(() => {
    if (!map || zoomTrigger.seq === 0) return;
    const current = map.getZoom() ?? 12;
    const next = Math.max(4, Math.min(20, current + zoomTrigger.delta));
    map.setZoom(next);
  }, [map, zoomTrigger.seq, zoomTrigger.delta]);

  return null;
}

export function LiveGoogleConvoyMap({
  members,
  focusedMember,
  routeConfig,
  vehicleType = 'MOTOR',
  stragglerAlert,
  sosState,
  etaInfo,
  darkMode = false,
  isPickingDestination,
  isPickingCheckpoint,
  isNavigationMode = false,
  cleanNavMode = false,
  canPingRouting = true,
  onToggleNavigationMode,
  onSelectMember,
  onPickCoordinateOnMap,
  onRouteComputed,
}: LiveGoogleConvoyMapProps) {
  const [mapType, setMapType] = useState<'roadmap' | 'hybrid'>('roadmap');
  const [showTraffic, setShowTraffic] = useState(true);
  const [isAutoFollowing, setIsAutoFollowing] = useState(true);
  const [followTrigger, setFollowTrigger] = useState(0);
  const [fitAllTrigger, setFitAllTrigger] = useState(1);
  const [zoomTrigger, setZoomTrigger] = useState({ delta: 0, seq: 0 });

  const waypoints = routeConfig.waypoints;
  const destPoint = waypoints[waypoints.length - 1];
  const activeNavMode =
    Boolean(isNavigationMode) && !isPickingDestination && !isPickingCheckpoint;

  const navSubject =
    focusedMember ||
    members.find((m) => m.isCurrentUser) ||
    members[0] ||
    null;

  const navGuidance = useMemo(() => {
    const pos = navSubject
      ? {
          lat: navSubject.mapPosition.latitude,
          lng: navSubject.mapPosition.longitude,
        }
      : waypoints[0]
        ? { lat: waypoints[0].latitude, lng: waypoints[0].longitude }
        : { lat: 21.4225, lng: 39.8262 };
    return computeNavigationGuidance({
      pos,
      speedKmh: navSubject?.mapPosition.speedKmh ?? 0,
      gpsHeading: navSubject?.mapPosition.heading,
      waypoints,
      instructions: routeConfig.routeInstructions,
      destinationName: routeConfig.destinationName,
      totalRouteMeters: routeConfig.totalRouteMeters,
      offRouteMeters: navSubject?.offRouteMeters ?? 0,
      offRouteToleranceMeters: routeConfig.offRouteToleranceMeters,
    });
  }, [
    navSubject?.id,
    navSubject?.mapPosition.latitude,
    navSubject?.mapPosition.longitude,
    navSubject?.mapPosition.speedKmh,
    navSubject?.mapPosition.heading,
    navSubject?.offRouteMeters,
    waypoints,
    routeConfig.routeInstructions,
    routeConfig.destinationName,
    routeConfig.totalRouteMeters,
    routeConfig.offRouteToleranceMeters,
  ]);

  const navHeadingDeg = navGuidance.routeBearingDeg;

  const defaultCenter = useMemo(
    () =>
      focusedMember
        ? {
            lat: focusedMember.mapPosition.latitude,
            lng: focusedMember.mapPosition.longitude,
          }
        : destPoint
          ? { lat: destPoint.latitude, lng: destPoint.longitude }
          : { lat: 21.4225, lng: 39.8262 },
    []
  );

  // Trigger camera adjustment when toggling GPS Navigation Mode
  useEffect(() => {
    if (activeNavMode) {
      setIsAutoFollowing(true);
      setFollowTrigger((n) => n + 1);
    } else {
      setFitAllTrigger((n) => n + 1);
    }
  }, [activeNavMode]);

  const destinationStops = routeConfig.destinationStops ?? [];
  const intermediateStops =
    destinationStops.length > 1 ? destinationStops.slice(0, -1) : [];
  const stopsSig = destinationStops
    .map((s) => `${s.latitude.toFixed(4)},${s.longitude.toFixed(4)}`)
    .join('>');

  const routePref: RoutePreference = routeConfig.routePreference ?? 'fastest';
  const routeKey = `${routePref}|${vehicleType}|${stopsSig}|${waypoints
    .map((w) => `${w.latitude.toFixed(4)},${w.longitude.toFixed(4)}`)
    .join('|')}`;

  const handleMapClick = (ev: MapMouseEvent) => {
    if (!isPickingDestination && !isPickingCheckpoint) return;
    const latLng = ev.detail.latLng;
    if (!latLng) return;
    onPickCoordinateOnMap(latLng.lat, latLng.lng);
  };

  return (
    <div
      className={`relative w-full h-full overflow-hidden ${
        isPickingDestination || isPickingCheckpoint ? 'cursor-crosshair' : ''
      } ${activeNavMode ? 'bg-[#0B1117]' : ''}`}
    >
      <GoogleMap
        mapId="DEMO_MAP_ID"
        colorScheme={darkMode ? ColorScheme.DARK : ColorScheme.LIGHT}
        mapTypeId={mapType}
        gestureHandling="greedy"
        disableDefaultUI={true}
        defaultCenter={defaultCenter}
        defaultZoom={12}
        internalUsageAttributionIds={[GMP_ATTRIBUTION_ID]}
        onClick={handleMapClick}
        onDragstart={() => setIsAutoFollowing(false)}
        className="w-full h-full"
      >
        <ConvoyMapController
          waypoints={waypoints}
          destinationStops={destinationStops}
          vehicleType={vehicleType}
          routePref={routePref}
          routeKey={routeKey}
          canPingRouting={canPingRouting}
          showTraffic={showTraffic}
          activeNavMode={activeNavMode}
          navSubject={navSubject}
          focusedMember={focusedMember}
          navHeadingDeg={navHeadingDeg}
          isAutoFollowing={isAutoFollowing}
          followTrigger={followTrigger}
          fitAllTrigger={fitAllTrigger}
          zoomTrigger={zoomTrigger}
          onRouteComputed={onRouteComputed}
        />

        {/* Intermediate Multi-Point Destination Markers */}
        {intermediateStops.map((stop, idx) => (
          <AdvancedMarker
            key={stop.id || `stop-${idx}`}
            position={{ lat: stop.latitude, lng: stop.longitude }}
            zIndex={185}
            clickable={false}
          >
            <div className="flex flex-col items-center pointer-events-none">
              <div className="px-2 py-0.5 rounded-lg bg-[#0B1F16]/90 text-white shadow-md text-[10px] font-semibold max-w-[155px] truncate border border-[#FBBF24]/50">
                Poin {idx + 1}: {stop.name}
              </div>
              <span className="mt-[3px] w-6 h-6 rounded-full bg-[#B45309] border-2 border-white shadow-md flex items-center justify-center text-white text-[10px] font-bold">
                {idx + 1}
              </span>
            </div>
          </AdvancedMarker>
        ))}

        {/* Final Destination Flag Marker */}
        {destPoint && (
          <AdvancedMarker
            position={{ lat: destPoint.latitude, lng: destPoint.longitude }}
            zIndex={200}
            clickable={false}
          >
            <div className="flex flex-col items-center pointer-events-none">
              <div className="px-2.5 py-1 rounded-[10px] bg-[#061E16] text-white shadow-lg text-center max-w-[195px] border border-[#34D399]/35">
                <div className="text-[10px] font-semibold truncate">
                  {destinationStops.length > 1
                    ? `Tujuan ${destinationStops.length}: ${routeConfig.destinationName}`
                    : `Tujuan: ${routeConfig.destinationName}`}
                </div>
                <div className="text-[9px] font-mono text-[#A7F3D0] whitespace-nowrap">
                  ETA {etaInfo.arrivalClockLabel} · {etaInfo.durationLabel} ·{' '}
                  {etaInfo.distanceLabel}
                </div>
              </div>
              <span className="mt-[3px] w-7 h-7 rounded-full bg-[#047857] border-2 border-white shadow-md flex items-center justify-center text-white text-[13px]">
                🕌
              </span>
            </div>
          </AdvancedMarker>
        )}

        {/* Convoy Member Markers */}
        {members.map((member) => {
          const isSelected = member.id === focusedMember?.id;
          const isNavHero = activeNavMode && member.id === navSubject?.id;
          const isSosBeacon =
            member.conditionStatus === 'SOS' ||
            (sosState.isActive && sosState.senderId === member.id);
          const isOffRoute =
            member.offRouteMeters > routeConfig.offRouteToleranceMeters;
          const isStraggler =
            stragglerAlert.isTriggered &&
            stragglerAlert.trailingMember?.id === member.id;
          const vehicleLabel =
            member.vehicleDetail.trim() ||
            (member.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki');
          const prefix = isSosBeacon
            ? 'SOS · '
            : member.isActingLeader
              ? 'Plt.Pembimbing · '
              : member.isCoLeader && member.isSweeper
                ? member.isAutoSweeper
                  ? 'Wakil/Pendamping · '
                  : 'Wakil/Pendamping · '
                : member.isCoLeader
                  ? 'Wakil · '
                  : member.isSweeper
                    ? member.isAutoSweeper
                      ? 'Pendamping (Auto) · '
                      : 'Pendamping · '
                    : '';
          const labelText = `${prefix}${member.fullName} · ${vehicleLabel}${
            member.isCurrentUser ? ' (Anda)' : ''
          }`;

          const pillBg = isSosBeacon
            ? '#991B1B'
            : isOffRoute
              ? '#DC2626'
              : isStraggler || member.isSweeper
                ? '#B45309'
                : member.isActingLeader
                  ? '#0F766E'
                  : member.isCoLeader
                    ? '#065F46'
                    : isSelected
                      ? '#047857'
                      : darkMode
                        ? '#0C1E17'
                        : '#FFFFFF';
          const pillText =
            isSosBeacon ||
            isOffRoute ||
            isStraggler ||
            member.isSweeper ||
            member.isActingLeader ||
            member.isCoLeader ||
            isSelected
              ? '#FFFFFF'
              : darkMode
                ? '#ECFDF5'
                : '#0B1F16';
          const pillBorder =
            isSosBeacon || isOffRoute
              ? '#7F1D1D'
              : isStraggler || member.isSweeper
                ? '#92400E'
                : member.isActingLeader
                  ? '#0D9488'
                  : member.isCoLeader
                    ? '#047857'
                    : isSelected
                      ? '#065F46'
                      : darkMode
                        ? '#1B3B2E'
                        : '#D8E6DD';

          const pinBg =
            isSosBeacon || isOffRoute
              ? '#DC2626'
              : member.isActingLeader
                ? '#0F766E'
                : member.isCoLeader
                  ? '#065F46'
                  : member.isCurrentUser
                    ? '#B45309'
                    : member.role === 'TOUR_LEADER'
                      ? '#047857'
                      : '#334155';

          const vehicleEmoji = isSosBeacon
            ? '⚠️'
            : member.vehicleType === 'MOBIL'
              ? '🚌'
              : '🚶';

          const memberWorldHeading =
            typeof member.mapPosition.heading === 'number' &&
            member.mapPosition.heading > 0 &&
            member.mapPosition.speedKmh >= 5
              ? member.mapPosition.heading
              : navHeadingDeg;

          return (
            <AdvancedMarker
              key={member.id}
              position={{
                lat: member.mapPosition.latitude,
                lng: member.mapPosition.longitude,
              }}
              zIndex={
                isSosBeacon ? 600 : isNavHero ? 550 : isSelected ? 500 : 300
              }
              clickable={true}
              onClick={() => {
                onSelectMember(member.id);
                setIsAutoFollowing(true);
                setFollowTrigger((n) => n + 1);
              }}
            >
              {isNavHero ? (
                <div className="relative w-[54px] h-[54px] flex items-center justify-center cursor-pointer">
                  <span
                    className="absolute inset-0 rounded-full"
                    style={{
                      background: 'rgba(16,185,129,0.22)',
                      border: '1.5px solid rgba(52,211,153,0.65)',
                    }}
                  />
                  <div
                    className="w-9 h-9 rounded-full bg-[#061E16] border-[2.5px] border-white shadow-lg flex items-center justify-center"
                    style={{ transform: `rotate(${memberWorldHeading}deg)` }}
                  >
                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                      <path
                        d="M12 2L20 20L12 16L4 20L12 2Z"
                        fill={isOffRoute ? '#EF4444' : '#34D399'}
                        stroke="#FFFFFF"
                        strokeWidth="1.5"
                        strokeLinejoin="round"
                      />
                    </svg>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center cursor-pointer">
                  <span
                    className="px-2 py-0.5 rounded-lg text-[10px] font-semibold whitespace-nowrap max-w-[175px] truncate shadow-md"
                    style={{
                      backgroundColor: pillBg,
                      color: pillText,
                      border: `1px solid ${pillBorder}`,
                    }}
                  >
                    {labelText}
                  </span>
                  <div
                    className="mt-[3px] -mb-[6px] z-[2] w-[26px] h-[26px] rounded-full bg-[#1E293B] text-white text-[9px] font-bold flex items-center justify-center shadow"
                    style={{
                      border: `2px solid ${
                        isSosBeacon || isOffRoute ? '#DC2626' : '#FFFFFF'
                      }`,
                    }}
                  >
                    {getInitials(member.fullName)}
                  </div>
                  <div className="relative flex items-center justify-center">
                    {isSosBeacon && (
                      <span
                        className="absolute w-10 h-10 rounded-full"
                        style={{ background: 'rgba(220,38,38,0.38)' }}
                      />
                    )}
                    <span
                      className="relative w-[30px] h-[30px] rounded-full border-2 border-white shadow-md flex items-center justify-center text-[13px]"
                      style={{ backgroundColor: pinBg }}
                    >
                      {vehicleEmoji}
                    </span>
                  </div>
                </div>
              )}
            </AdvancedMarker>
          );
        })}
      </GoogleMap>

      {/* Subtle Horizon Depth Gradient in GPS Navigation Mode */}
      {activeNavMode && (
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-14 z-10 bg-gradient-to-b from-black/35 to-transparent"
        />
      )}

      {/* Top Turn-by-Turn Navigation Instruction HUD */}
      {activeNavMode && (
        <div
          role="status"
          aria-label="Instruksi Navigasi GPS"
          className={`absolute top-2.5 left-2.5 right-12 z-20 rounded-2xl px-3 py-2 shadow-xl border flex items-center gap-2.5 backdrop-blur-md ${
            navGuidance.isOffRoute
              ? 'bg-[#DC2626]/95 border-[#FCA5A5]/60 text-white'
              : 'bg-[#065F46]/95 border-[#34D399]/40 text-white'
          }`}
        >
          <div className="w-9 h-9 rounded-xl bg-white/15 flex items-center justify-center shrink-0">
            <ManeuverIcon maneuver={navGuidance.maneuver} />
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-baseline gap-2">
              <span className="text-sm font-extrabold font-mono tabular-nums tracking-tight">
                {navGuidance.distanceToTurnLabel}
              </span>
              <span className="text-[11px] font-medium text-white/80 truncate">
                {navGuidance.isOffRoute ? 'Keluar Jalur' : navGuidance.streetName}
              </span>
            </div>
            <p className="text-xs font-semibold leading-tight truncate">
              {navGuidance.isOffRoute
                ? 'Kembali ke jalur hijau rombongan terdekat'
                : navGuidance.instructionText}
            </p>
          </div>
        </div>
      )}

      {/* Bottom-Left: Digital Speedometer & Compass in Nav Mode */}
      {activeNavMode && (
        <div className="absolute bottom-2.5 left-2.5 z-20 pointer-events-none rounded-2xl bg-[#061E16]/90 backdrop-blur-md border border-[#34D399]/25 text-white px-3 py-2 shadow-lg">
          <div className="flex items-baseline gap-1">
            <span className="text-2xl font-extrabold font-mono tabular-nums leading-none">
              {navGuidance.speedKmh}
            </span>
            <span className="text-[10px] font-semibold text-white/75">
              km/j
            </span>
            {cleanNavMode && (
              <span className="ml-1.5 pl-1.5 border-l border-white/20 text-[10px] font-mono tabular-nums text-[#A7F3D0] font-semibold">
                {etaInfo.arrivalClockLabel} · {etaInfo.distanceLabel}
              </span>
            )}
          </div>
          <div className="mt-1 flex items-center gap-1 text-[10px] font-mono text-[#FDE68A]">
            <Navigation
              className="w-2.5 h-2.5"
              style={{ transform: `rotate(${navHeadingDeg}deg)` }}
            />
            <span>
              {navHeadingDeg}° {compassDirectionLabel(navHeadingDeg)}
            </span>
            <span className="text-white/45">· Google Maps</span>
          </div>
        </div>
      )}

      {/* Right Floating Map Controls (compact vertical rail clear of bottom SOS/Mic row) */}
      <div className="absolute top-2.5 right-2.5 z-20 flex flex-col gap-1">
        <button
          type="button"
          onClick={() =>
            setMapType((m) => (m === 'roadmap' ? 'hybrid' : 'roadmap'))
          }
          aria-label="Ubah Tampilan Satelit / Peta"
          title={mapType === 'hybrid' ? 'Mode Peta Jalan' : 'Mode Satelit'}
          className={`w-8 h-8 rounded-xl border shadow-sm flex items-center justify-center cursor-pointer transition-colors ${
            mapType === 'hybrid'
              ? 'bg-[#047857] text-white border-[#065F46]'
              : 'bg-white/95 dark:bg-[#0C1E17]/95 border-[#D8E6DD] dark:border-[#1B3B2E] text-[#0B1F16] dark:text-[#ECFDF5]'
          }`}
        >
          <Layers className="w-4 h-4" />
        </button>
        {!cleanNavMode && (
          <button
            type="button"
            onClick={() => setShowTraffic((t) => !t)}
            aria-label="Toggle Lalu Lintas Langsung"
            title={showTraffic ? 'Sembunyikan Traffic' : 'Tampilkan Traffic Live'}
            className={`w-8 h-8 rounded-xl border shadow-sm flex items-center justify-center cursor-pointer transition-colors ${
              showTraffic
                ? 'bg-[#0F766E] text-white border-[#115E59]'
                : 'bg-white/95 dark:bg-[#0C1E17]/95 border-[#D8E6DD] dark:border-[#1B3B2E] text-[#0B1F16] dark:text-[#ECFDF5]'
            }`}
          >
            <Activity className="w-4 h-4" />
          </button>
        )}
        <button
          type="button"
          onClick={() => {
            setIsAutoFollowing(true);
            setFollowTrigger((n) => n + 1);
          }}
          aria-label="Fokus ke Jamaah & Ikuti Live"
          title={
            isAutoFollowing
              ? 'Mengikuti Live (Ketuk untuk pusatkan)'
              : 'Ikuti Jamaah Live'
          }
          className={`w-8 h-8 rounded-xl border shadow-sm flex items-center justify-center cursor-pointer transition-colors ${
            isAutoFollowing
              ? 'bg-[#047857] text-white border-[#065F46]'
              : 'bg-white/95 dark:bg-[#0C1E17]/95 border-[#D8E6DD] dark:border-[#1B3B2E] text-[#047857] dark:text-[#34D399]'
          }`}
        >
          <LocateFixed className="w-4 h-4" />
        </button>
        {!cleanNavMode && (
          <>
            <button
              type="button"
              onClick={() => {
                if (isNavigationMode && onToggleNavigationMode) {
                  onToggleNavigationMode();
                }
                setIsAutoFollowing(false);
                setFitAllTrigger((n) => n + 1);
              }}
              aria-label="Lihat Seluruh Rute"
              title="Lihat Seluruh Rute"
              className="w-8 h-8 rounded-xl bg-white/95 dark:bg-[#0C1E17]/95 border border-[#D8E6DD] dark:border-[#1B3B2E] shadow-sm flex items-center justify-center text-[#B45309] dark:text-[#FBBF24] cursor-pointer"
            >
              <Compass className="w-4 h-4" />
            </button>
            <div className="w-8 rounded-xl bg-white/95 dark:bg-[#0C1E17]/95 border border-[#D8E6DD] dark:border-[#1B3B2E] shadow-sm overflow-hidden flex flex-col divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E]">
              <button
                type="button"
                onClick={() =>
                  setZoomTrigger((prev) => ({ delta: 1, seq: prev.seq + 1 }))
                }
                aria-label="Perbesar Peta"
                className="w-8 h-7 flex items-center justify-center cursor-pointer hover:bg-[#ECFDF5] dark:hover:bg-[#133025]"
              >
                <Plus className="w-3.5 h-3.5" />
              </button>
              <button
                type="button"
                onClick={() =>
                  setZoomTrigger((prev) => ({ delta: -1, seq: prev.seq + 1 }))
                }
                aria-label="Perkecil Peta"
                className="w-8 h-7 flex items-center justify-center cursor-pointer hover:bg-[#ECFDF5] dark:hover:bg-[#133025]"
              >
                <Minus className="w-3.5 h-3.5" />
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
