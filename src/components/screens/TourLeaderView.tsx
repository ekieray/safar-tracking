import { type ReactNode, useEffect, useMemo, useRef, useState } from 'react';
import {
  AdvancedMarker,
  ColorScheme,
  Map as GoogleMap,
  MapMouseEvent,
  useMap,
  useMapsLibrary,
} from '@vis.gl/react-google-maps';
import {
  ArrowLeft,
  ArrowRight,
  Ban,
  Bus,
  Check,
  ChevronDown,
  ChevronUp,
  Cloud,
  Copy,
  Flag,
  Footprints,
  Gauge,
  Loader2,
  LocateFixed,
  Map as MapIcon,
  MapPin,
  Plus,
  RotateCcw,
  Route,
  Search,
  Share2,
  Trash2,
  UserPlus,
  X,
  Zap,
} from 'lucide-react';
import {
  ConvoyRouteConfig,
  DestinationPreset,
  DestinationStop,
  RoomActivityItem,
  RoomActivityMemberProgress,
  RouteInstruction,
  RoutePoint,
  RoutePreference,
  TourRoom,
  VehicleType,
} from '../../types/convoy';
import { ConvoyAvatar } from '../ConvoyAvatar';
import { RoomActivityChecklistPanel } from '../RoomActivityChecklistPanel';
import { GoogleRoutePickerModal } from '../map/GoogleRoutePickerModal';
import {
  computeGoogleRoute,
  GMP_ATTRIBUTION_ID,
  GooglePlaceSuggestion,
  searchGoogleAutocompleteSuggestions,
  selectGooglePlacePrediction,
} from '../../services/googleMapsService';
import { LatLngPoint } from '../../utils/geo';

interface OriginPreset {
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}

const ORIGIN_PRESETS: OriginPreset[] = [
  {
    name: 'Hotel Area Ajyad Makkah',
    address: 'Ajyad Street, Area Masjidil Haram, Makkah',
    latitude: 21.418,
    longitude: 39.8275,
  },
  {
    name: 'Terminal Syib Amir Makkah',
    address: 'Syib Amir Bus Station, Makkah',
    latitude: 21.4258,
    longitude: 39.8298,
  },
  {
    name: 'Stasiun Haramain Makkah',
    address: 'Al Rasaifah, Makkah Al Mukarramah',
    latitude: 21.4165,
    longitude: 39.7938,
  },
  {
    name: 'Masjid Bir Ali (Miqat)',
    address: 'Dzulhulaifah, Madinah Al Munawwarah',
    latitude: 24.4139,
    longitude: 39.5425,
  },
];

const EXTRA_STOP_PRESETS: Array<{
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}> = [
  {
    name: "Masjid Tan'im (Miqat Aisyah)",
    address: "At-Tan'im, Makkah Al Mukarramah",
    latitude: 21.4673,
    longitude: 39.8007,
  },
  {
    name: 'Masjid Quba · Madinah',
    address: 'Al Khatim, Madinah Al Munawwarah',
    latitude: 24.4392,
    longitude: 39.6173,
  },
  {
    name: 'Jabal Uhud · Madinah',
    address: 'Area Syuhada Uhud, Madinah',
    latitude: 24.5023,
    longitude: 39.6122,
  },
];

interface TourLeaderViewProps {
  activeRoom: TourRoom | null;
  routeConfig: ConvoyRouteConfig;
  destinationPresets: DestinationPreset[];
  vehicleType?: VehicleType;
  isAuthenticated?: boolean;
  isCreatingRoom?: boolean;
  createRoomError?: string | null;
  userGpsLocation?: LatLngPoint | null;
  darkMode?: boolean;
  onGoogleSignIn?: () => void;
  onPickOriginLatLng: (
    lat: number,
    lng: number,
    placeName: string,
    addressLabel: string
  ) => void;
  onUseGpsAsOrigin: () => void;
  onSelectPreset: (preset: DestinationPreset) => void;
  onPickDestinationLatLng: (
    lat: number,
    lng: number,
    placeName: string,
    addressLabel: string
  ) => void;
  onAddDestinationPoint: (
    lat: number,
    lng: number,
    placeName: string,
    addressLabel: string
  ) => void;
  onRemoveDestinationPoint: (stopId: string) => void;
  onMoveDestinationPoint: (stopId: string, direction: 'up' | 'down') => void;
  onSelectRoutePreference: (preference: RoutePreference) => void;
  onUpdateWaypoints: (waypoints: RoutePoint[]) => void;
  onResetDefaultWaypoints: () => void;
  onRouteComputed?: (
    distanceMeters: number,
    durationMillis: number,
    path: LatLngPoint[],
    instructions?: RouteInstruction[]
  ) => void;
  onAddCheckpoint: (name: string, xFraction: number, yFraction: number) => void;
  onRemoveCheckpoint: (checkpointId: string) => void;
  onUpdateMaxStragglerDistance: (meters: number) => void;
  onToggleSoundAlert: () => void;
  onCreateRoom: (customTitle?: string) => void;
  onCreateLocalRoom?: (customTitle?: string) => void;
  onSimulateJoin: () => void;
  onRemoveMember: (memberId: string) => void;
  onEnterMapPlaceholder: () => void;
  onBack: () => void;
  draftActivities?: RoomActivityItem[];
  activeRoomActivities?: RoomActivityItem[];
  activityProgressByMember?: Record<string, RoomActivityMemberProgress>;
  currentUid?: string;
  onAddDraftActivity?: (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => void;
  onRemoveDraftActivity?: (activityId: string) => void;
  onMoveDraftActivity?: (activityId: string, direction: 'up' | 'down') => void;
  onApplyDraftStarterTemplates?: (items: RoomActivityItem[]) => void;
  onToggleRoomActivity?: (activityId: string) => void;
  onAddRoomActivity?: (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => void;
  onRemoveRoomActivity?: (activityId: string) => void;
  onMoveRoomActivity?: (activityId: string, direction: 'up' | 'down') => void;
  onApplyRoomStarterTemplates?: (items: RoomActivityItem[]) => void;
}

const STEP_LABELS = [
  { num: 1, label: 'Kumpul' },
  { num: 2, label: 'Tujuan' },
  { num: 3, label: 'Rute' },
  { num: 4, label: 'Batas' },
];

function StepRouteController({
  waypoints,
  destinationStops,
  manualViaPoints,
  vehicleType,
  routePreference,
  onPreviewRouteComputed,
}: {
  waypoints: RoutePoint[];
  destinationStops: DestinationStop[];
  manualViaPoints: RoutePoint[];
  vehicleType: VehicleType;
  routePreference: RoutePreference;
  onPreviewRouteComputed?: (
    distanceMeters: number,
    durationMillis: number,
    path: LatLngPoint[],
    instructions?: RouteInstruction[]
  ) => void;
}) {
  const map = useMap();
  const mapsLib = useMapsLibrary('maps');
  const routesLib = useMapsLibrary('routes');
  const lastBoundsKeyRef = useRef<string>('');
  const onPreviewRouteComputedRef = useRef(onPreviewRouteComputed);
  onPreviewRouteComputedRef.current = onPreviewRouteComputed;
  const [linePath, setLinePath] = useState<LatLngPoint[]>([]);

  const startPt = waypoints[0];
  const stopCoords = destinationStops.map((s) => ({
    latitude: s.latitude,
    longitude: s.longitude,
  }));
  const hasMultiStops = destinationStops.length >= 2;
  const routingPoints: Array<{ latitude: number; longitude: number }> =
    startPt && stopCoords.length >= 1
      ? hasMultiStops
        ? manualViaPoints.length > 0
          ? [
              startPt,
              ...stopCoords.slice(0, -1),
              ...manualViaPoints,
              stopCoords[stopCoords.length - 1],
            ]
          : [startPt, ...stopCoords]
        : manualViaPoints.length > 0
          ? [startPt, ...manualViaPoints, stopCoords[stopCoords.length - 1]]
          : waypoints
      : waypoints;

  const previewKey = `${routePreference}|${vehicleType}|${hasMultiStops ? 'multi' : 'single'}|${routingPoints
    .map((pt) => `${pt.latitude.toFixed(4)},${pt.longitude.toFixed(4)}`)
    .join('|')}`;

  useEffect(() => {
    setLinePath(
      waypoints.map((pt) => ({ lat: pt.latitude, lng: pt.longitude }))
    );

    const start = routingPoints[0];
    const end = routingPoints[routingPoints.length - 1];
    const boundsKey =
      start && end
        ? `${start.latitude.toFixed(4)},${start.longitude.toFixed(4)}|${stopCoords.map((s) => `${s.latitude.toFixed(4)},${s.longitude.toFixed(4)}`).join(';')}`
        : '';
    if (map && boundsKey && boundsKey !== lastBoundsKeyRef.current) {
      lastBoundsKeyRef.current = boundsKey;
      const bounds = new google.maps.LatLngBounds();
      routingPoints.forEach((w) =>
        bounds.extend({ lat: w.latitude, lng: w.longitude })
      );
      map.fitBounds(bounds, 36);
    }

    if (!routesLib || routingPoints.length < 2) return;
    let cancelled = false;
    computeGoogleRoute(
      routesLib,
      routingPoints,
      vehicleType,
      routePreference,
      hasMultiStops
    ).then((result) => {
      if (cancelled || !result || result.path.length < 2) return;
      setLinePath(result.path);
      if (onPreviewRouteComputedRef.current) {
        onPreviewRouteComputedRef.current(
          result.distanceMeters,
          result.durationMillis,
          result.path,
          result.instructions
        );
      }
    });

    return () => {
      cancelled = true;
    };
  }, [map, routesLib, previewKey]);

  useEffect(() => {
    if (!map || !mapsLib || linePath.length < 2) return;
    const casing = new mapsLib.Polyline({
      path: linePath,
      strokeColor: '#FFFFFF',
      strokeOpacity: 0.9,
      strokeWeight: 8,
      zIndex: 10,
      clickable: false,
      map,
    });
    const inner = new mapsLib.Polyline({
      path: linePath,
      strokeColor: '#047857',
      strokeOpacity: 0.95,
      strokeWeight: 5,
      zIndex: 11,
      clickable: false,
      map,
    });
    return () => {
      casing.setMap(null);
      inner.setMap(null);
    };
  }, [map, mapsLib, linePath]);

  return null;
}

function GoogleStepRouteMap({
  waypoints,
  destinationStops,
  manualViaPoints,
  vehicleType = 'MOTOR',
  routePreference = 'fastest',
  darkMode = false,
  onMapClick,
  onPreviewRouteComputed,
}: {
  waypoints: RoutePoint[];
  destinationStops: DestinationStop[];
  manualViaPoints: RoutePoint[];
  vehicleType?: VehicleType;
  routePreference?: RoutePreference;
  darkMode?: boolean;
  onMapClick: (lat: number, lng: number) => void;
  onPreviewRouteComputed?: (
    distanceMeters: number,
    durationMillis: number,
    path: LatLngPoint[],
    instructions?: RouteInstruction[]
  ) => void;
}) {
  const defaultCenter = useMemo(() => {
    const first = waypoints[0];
    return first
      ? { lat: first.latitude, lng: first.longitude }
      : { lat: 21.4225, lng: 39.8262 };
  }, []);

  const handleClick = (ev: MapMouseEvent) => {
    const ll = ev.detail.latLng;
    if (!ll) return;
    onMapClick(ll.lat, ll.lng);
  };

  const startPt = waypoints[0];

  return (
    <div className="w-full h-full cursor-crosshair">
      <GoogleMap
        mapId="DEMO_MAP_ID"
        colorScheme={darkMode ? ColorScheme.DARK : ColorScheme.LIGHT}
        gestureHandling="greedy"
        disableDefaultUI={true}
        defaultCenter={defaultCenter}
        defaultZoom={13}
        internalUsageAttributionIds={[GMP_ATTRIBUTION_ID]}
        onClick={handleClick}
        className="w-full h-full"
      >
        <StepRouteController
          waypoints={waypoints}
          destinationStops={destinationStops}
          manualViaPoints={manualViaPoints}
          vehicleType={vehicleType}
          routePreference={routePreference}
          onPreviewRouteComputed={onPreviewRouteComputed}
        />

        {startPt && (
          <AdvancedMarker
            position={{ lat: startPt.latitude, lng: startPt.longitude }}
            zIndex={200}
            clickable={false}
          >
            <span className="inline-block px-2 py-0.5 rounded-full bg-[#B45309] text-white text-[10px] font-bold border-2 border-white shadow-md">
              Start
            </span>
          </AdvancedMarker>
        )}

        {manualViaPoints.map((pt, idx) => (
          <AdvancedMarker
            key={`via-${idx}-${pt.latitude}-${pt.longitude}`}
            position={{ lat: pt.latitude, lng: pt.longitude }}
            zIndex={120}
            clickable={false}
          >
            <span className="w-[18px] h-[18px] rounded-full bg-[#0F766E] text-white text-[9px] font-bold border-2 border-white shadow-md flex items-center justify-center">
              L{idx + 1}
            </span>
          </AdvancedMarker>
        ))}

        {destinationStops.map((stop, idx) => {
          const isFinal = idx === destinationStops.length - 1;
          return (
            <AdvancedMarker
              key={stop.id}
              position={{ lat: stop.latitude, lng: stop.longitude }}
              zIndex={isFinal ? 210 : 180}
              clickable={false}
            >
              {isFinal ? (
                <span className="inline-block px-2 py-0.5 rounded-full bg-[#047857] text-white text-[10px] font-bold border-2 border-white shadow-md">
                  {destinationStops.length > 1
                    ? `${idx + 1}. Tujuan`
                    : 'Tujuan'}
                </span>
              ) : (
                <span className="w-6 h-6 rounded-full bg-[#065F46] text-white text-[10px] font-bold border-2 border-white shadow-md flex items-center justify-center">
                  {idx + 1}
                </span>
              )}
            </AdvancedMarker>
          );
        })}
      </GoogleMap>
    </div>
  );
}

export function TourLeaderView({
  activeRoom,
  routeConfig,
  destinationPresets,
  vehicleType = 'MOTOR',
  isAuthenticated,
  isCreatingRoom,
  createRoomError,
  userGpsLocation,
  darkMode,
  onGoogleSignIn,
  onPickOriginLatLng,
  onUseGpsAsOrigin,
  onSelectPreset,
  onPickDestinationLatLng,
  onAddDestinationPoint,
  onRemoveDestinationPoint,
  onMoveDestinationPoint,
  onSelectRoutePreference,
  onUpdateWaypoints,
  onResetDefaultWaypoints,
  onRouteComputed,
  onUpdateMaxStragglerDistance,
  onToggleSoundAlert,
  onCreateRoom,
  onCreateLocalRoom,
  onSimulateJoin,
  onRemoveMember,
  onEnterMapPlaceholder,
  onBack,
  draftActivities = [],
  activeRoomActivities = [],
  activityProgressByMember = {},
  currentUid = '',
  onAddDraftActivity,
  onRemoveDraftActivity,
  onMoveDraftActivity,
  onApplyDraftStarterTemplates,
  onToggleRoomActivity,
  onAddRoomActivity,
  onRemoveRoomActivity,
  onMoveRoomActivity,
  onApplyRoomStarterTemplates,
}: TourLeaderViewProps) {
  const [currentStep, setCurrentStep] = useState(0);
  const [showSetupStepper, setShowSetupStepper] = useState(!activeRoom);
  const [pickerMode, setPickerMode] = useState<
    'origin' | 'destination' | 'add_point' | null
  >(null);
  const [tourNameDraft, setTourNameDraft] = useState(
    activeRoom?.roomTitle || 'Rombongan Ziarah Makkah'
  );
  const [copiedType, setCopiedType] = useState<'id' | 'link' | 'web' | null>(null);
  const [previewStats, setPreviewStats] = useState<{
    distanceMeters: number;
    durationMillis: number;
  } | null>(null);

  const [showAddPointMenu, setShowAddPointMenu] = useState(false);
  const [addPointTab, setAddPointTab] = useState<
    'menu' | 'search' | 'presets' | 'custom'
  >('menu');
  const [addPointSearchQuery, setAddPointSearchQuery] = useState('');
  const [addPointSearchResults, setAddPointSearchResults] = useState<
    GooglePlaceSuggestion[]
  >([]);
  const [isSearchingAddPoint, setIsSearchingAddPoint] = useState(false);
  const [customPointName, setCustomPointName] = useState('');
  const [customPointLat, setCustomPointLat] = useState('21.4225');
  const [customPointLng, setCustomPointLng] = useState('39.8262');
  const [customPointError, setCustomPointError] = useState<string | null>(null);
  const [manualViaPoints, setManualViaPoints] = useState<RoutePoint[]>([]);

  const placesLib = useMapsLibrary('places');
  const originTokenRef = useRef<google.maps.places.AutocompleteSessionToken | undefined>(
    undefined
  );
  const destTokenRef = useRef<google.maps.places.AutocompleteSessionToken | undefined>(
    undefined
  );
  const addPointTokenRef = useRef<google.maps.places.AutocompleteSessionToken | undefined>(
    undefined
  );

  const [originSearchQuery, setOriginSearchQuery] = useState('');
  const [originSearchResults, setOriginSearchResults] = useState<
    GooglePlaceSuggestion[]
  >([]);
  const [isSearchingOrigin, setIsSearchingOrigin] = useState(false);

  const [destSearchQuery, setDestSearchQuery] = useState('');
  const [destSearchResults, setDestSearchResults] = useState<
    GooglePlaceSuggestion[]
  >([]);
  const [isSearchingDest, setIsSearchingDest] = useState(false);

  const effectiveStops: DestinationStop[] = useMemo(() => {
    if (routeConfig.destinationStops && routeConfig.destinationStops.length > 0) {
      return routeConfig.destinationStops;
    }
    const lastWp = routeConfig.waypoints[routeConfig.waypoints.length - 1];
    return [
      {
        id: 'stop-fallback-1',
        name: routeConfig.destinationName || 'Titik Tujuan',
        address: routeConfig.destinationAddress || routeConfig.destinationName,
        latitude: lastWp?.latitude ?? 21.4225,
        longitude: lastWp?.longitude ?? 39.8262,
      },
    ];
  }, [
    routeConfig.destinationStops,
    routeConfig.destinationName,
    routeConfig.destinationAddress,
    routeConfig.waypoints,
  ]);

  useEffect(() => {
    const trimmed = originSearchQuery.trim();
    if (!placesLib || trimmed.length < 2) {
      setOriginSearchResults([]);
      setIsSearchingOrigin(false);
      return;
    }
    let cancelled = false;
    setIsSearchingOrigin(true);
    if (!originTokenRef.current) {
      originTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
    const timer = window.setTimeout(async () => {
      try {
        const res = await searchGoogleAutocompleteSuggestions(
          placesLib,
          trimmed,
          originTokenRef.current
        );
        if (!cancelled) setOriginSearchResults(res);
      } catch {
        if (!cancelled) setOriginSearchResults([]);
      } finally {
        if (!cancelled) setIsSearchingOrigin(false);
      }
    }, 320);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [placesLib, originSearchQuery]);

  useEffect(() => {
    const trimmed = destSearchQuery.trim();
    if (!placesLib || trimmed.length < 2) {
      setDestSearchResults([]);
      setIsSearchingDest(false);
      return;
    }
    let cancelled = false;
    setIsSearchingDest(true);
    if (!destTokenRef.current) {
      destTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
    const timer = window.setTimeout(async () => {
      try {
        const res = await searchGoogleAutocompleteSuggestions(
          placesLib,
          trimmed,
          destTokenRef.current
        );
        if (!cancelled) setDestSearchResults(res);
      } catch {
        if (!cancelled) setDestSearchResults([]);
      } finally {
        if (!cancelled) setIsSearchingDest(false);
      }
    }, 320);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [placesLib, destSearchQuery]);

  useEffect(() => {
    const trimmed = addPointSearchQuery.trim();
    if (!placesLib || trimmed.length < 2) {
      setAddPointSearchResults([]);
      setIsSearchingAddPoint(false);
      return;
    }
    let cancelled = false;
    setIsSearchingAddPoint(true);
    if (!addPointTokenRef.current) {
      addPointTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
    const timer = window.setTimeout(async () => {
      try {
        const res = await searchGoogleAutocompleteSuggestions(
          placesLib,
          trimmed,
          addPointTokenRef.current
        );
        if (!cancelled) setAddPointSearchResults(res);
      } catch {
        if (!cancelled) setAddPointSearchResults([]);
      } finally {
        if (!cancelled) setIsSearchingAddPoint(false);
      }
    }, 320);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [placesLib, addPointSearchQuery]);

  const handleCopy = async (text: string, type: 'id' | 'link' | 'web') => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // Clipboard fallback ignored
    }
    setCopiedType(type);
    setTimeout(() => setCopiedType(null), 1800);
  };

  const getWebRoomShareLink = (roomId: string) => {
    if (typeof window === 'undefined') {
      return `https://safartracking.id/join/${encodeURIComponent(roomId)}`;
    }
    return `${window.location.origin}/?room=${encodeURIComponent(roomId)}`;
  };

  const handleNativeShareRoom = async (room: TourRoom) => {
    const webLink = getWebRoomShareLink(room.roomId);
    const shareText = `Gabung rombongan umroh "${room.roomTitle}" di Safar Tracking!\nKode Grup: ${room.roomId}\nLink Browser: ${webLink}\nLink APK: ${room.inviteLink}`;
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        await navigator.share({
          title: `Undangan Rombongan ${room.roomId}`,
          text: shareText,
          url: webLink,
        });
        return;
      } catch {
        // User cancelled or unsupported fallback to copy
      }
    }
    await handleCopy(webLink, 'web');
  };

  const handleStepMapClick = (lat: number, lng: number) => {
    const newPt: RoutePoint = {
      xFraction: 0.5,
      yFraction: 0.5,
      latitude: lat,
      longitude: lng,
    };
    setManualViaPoints((prev) => [...prev.slice(-4), newPt]);
    const pts = routeConfig.waypoints;
    const updated =
      pts.length >= 2
        ? [...pts.slice(0, pts.length - 1), newPt, pts[pts.length - 1]]
        : [...pts, newPt];
    onUpdateWaypoints(updated);
  };

  const handleRemoveLastViaPoint = () => {
    if (manualViaPoints.length === 0) return;
    const removed = manualViaPoints[manualViaPoints.length - 1];
    setManualViaPoints((prev) => prev.slice(0, -1));
    const idx = routeConfig.waypoints.findIndex(
      (w) =>
        Math.abs(w.latitude - removed.latitude) < 1e-6 &&
        Math.abs(w.longitude - removed.longitude) < 1e-6
    );
    if (idx > 0 && idx < routeConfig.waypoints.length - 1) {
      const nextWps = [...routeConfig.waypoints];
      nextWps.splice(idx, 1);
      onUpdateWaypoints(nextWps);
    }
  };

  const handleOpenAddPointMenu = () => {
    const anchorStop = effectiveStops[effectiveStops.length - 1];
    if (anchorStop) {
      setCustomPointLat(anchorStop.latitude.toFixed(4));
      setCustomPointLng(anchorStop.longitude.toFixed(4));
    }
    setCustomPointName('');
    setCustomPointError(null);
    setShowAddPointMenu(true);
    setAddPointTab('menu');
  };

  const handleCloseAddPointMenu = () => {
    setShowAddPointMenu(false);
    setAddPointTab('menu');
    setAddPointSearchQuery('');
    setAddPointSearchResults([]);
    setCustomPointName('');
    setCustomPointError(null);
  };

  const handleSubmitCustomPoint = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmedName = customPointName.trim();
    const lat = Number.parseFloat(customPointLat);
    const lng = Number.parseFloat(customPointLng);
    if (!trimmedName) {
      setCustomPointError('Nama waypoint destinasi wajib diisi.');
      return;
    }
    if (!Number.isFinite(lat) || lat < -90 || lat > 90) {
      setCustomPointError('Latitude harus di antara -90 sampai 90.');
      return;
    }
    if (!Number.isFinite(lng) || lng < -180 || lng > 180) {
      setCustomPointError('Longitude harus di antara -180 sampai 180.');
      return;
    }
    onAddDestinationPoint(
      lat,
      lng,
      trimmedName,
      `${trimmedName} · ${lat.toFixed(5)}, ${lng.toFixed(5)}`
    );
    handleCloseAddPointMenu();
  };

  const renderAddPointMenuBox = () => (
    <div className="rounded-[14px] bg-white dark:bg-[#0C1E17] border-2 border-[#047857] dark:border-[#34D399] p-3 space-y-2.5">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5 min-w-0">
          <Plus className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
          <p className="text-xs font-bold text-[#047857] dark:text-[#6EE7B7] truncate">
            Menu Add Point Destinasi
          </p>
        </div>
        <button
          type="button"
          onClick={handleCloseAddPointMenu}
          aria-label="Tutup Menu Add Point"
          className="min-w-[32px] min-h-[32px] rounded-lg flex items-center justify-center text-[#4D6659] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] cursor-pointer"
        >
          <X className="w-4 h-4" />
        </button>
      </div>

      {addPointTab === 'menu' && (
        <div className="space-y-1.5">
          <button
            type="button"
            onClick={() => setAddPointTab('search')}
            className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] flex items-center gap-2.5 text-left transition-colors cursor-pointer"
          >
            <Search className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold">Cari Lokasi (Google Maps)</p>
              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                Ketik nama masjid, miqat, hotel, atau ziarah
              </p>
            </div>
          </button>

          <button
            type="button"
            onClick={() => {
              handleCloseAddPointMenu();
              setPickerMode('add_point');
            }}
            className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] flex items-center gap-2.5 text-left transition-colors cursor-pointer"
          >
            <MapPin className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold">Pin Titik di Peta (Google Maps)</p>
              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                Geser atau ketuk langsung posisi di peta
              </p>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setAddPointTab('presets')}
            className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] flex items-center gap-2.5 text-left transition-colors cursor-pointer"
          >
            <Flag className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold">Lokasi Populer Tanah Suci</p>
              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                Tambah Arafah, Gua Hira, Tan&apos;im, atau Quba
              </p>
            </div>
          </button>

          <button
            type="button"
            onClick={() => setAddPointTab('custom')}
            className="w-full min-h-[44px] px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] flex items-center gap-2.5 text-left transition-colors cursor-pointer"
          >
            <LocateFixed className="w-4 h-4 text-[#0F766E] dark:text-[#5EEAD4] shrink-0" />
            <div className="min-w-0 flex-1">
              <p className="text-xs font-bold">Input Nama &amp; Koordinat Manual</p>
              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                Masukkan nama titik dan koordinat GPS langsung
              </p>
            </div>
          </button>
        </div>
      )}

      {addPointTab === 'search' && (
        <div className="space-y-2">
          <button
            type="button"
            onClick={() => setAddPointTab('menu')}
            className="text-[11px] font-semibold text-[#047857] dark:text-[#34D399] flex items-center gap-1 cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Kembali ke Menu Add Point</span>
          </button>
          <div className="relative">
            <Search className="w-4 h-4 text-[#4D6659] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              type="text"
              value={addPointSearchQuery}
              onChange={(e) => setAddPointSearchQuery(e.target.value)}
              placeholder="Cari lokasi tambahan (mis. Jabal Rahmah, Quba)..."
              autoFocus
              className="w-full min-h-[42px] pl-9 pr-9 rounded-[12px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-xs outline-none"
            />
            {isSearchingAddPoint && (
              <Loader2 className="w-4 h-4 text-[#047857] animate-spin absolute right-3 top-1/2 -translate-y-1/2" />
            )}
          </div>
          {addPointSearchResults.length > 0 && (
            <div className="max-h-44 overflow-y-auto rounded-xl border border-[#D8E6DD] dark:border-[#1B3B2E] bg-[#F4F8F5] dark:bg-[#091812] divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E]">
              {addPointSearchResults.map((res) => (
                <button
                  key={res.id}
                  type="button"
                  onClick={async () => {
                    setIsSearchingAddPoint(true);
                    const resolved = await selectGooglePlacePrediction(res);
                    if (placesLib) {
                      addPointTokenRef.current =
                        new placesLib.AutocompleteSessionToken();
                    }
                    setIsSearchingAddPoint(false);
                    if (!resolved) return;
                    onAddDestinationPoint(
                      resolved.lat,
                      resolved.lng,
                      resolved.name,
                      resolved.address
                    );
                    handleCloseAddPointMenu();
                  }}
                  className="w-full px-3 py-2 text-left hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
                >
                  <p className="text-xs font-semibold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                    {res.name}
                  </p>
                  <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                    {res.address}
                  </p>
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {addPointTab === 'presets' && (
        <div className="space-y-2">
          <button
            type="button"
            onClick={() => setAddPointTab('menu')}
            className="text-[11px] font-semibold text-[#047857] dark:text-[#34D399] flex items-center gap-1 cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Kembali ke Menu Add Point</span>
          </button>
          <div className="grid grid-cols-1 gap-1.5 max-h-48 overflow-y-auto">
            {[
              ...destinationPresets.map((p) => ({
                name: p.name,
                address: `${p.name} · ${p.endPoint.latitude.toFixed(4)}, ${p.endPoint.longitude.toFixed(4)}`,
                latitude: p.endPoint.latitude,
                longitude: p.endPoint.longitude,
              })),
              ...EXTRA_STOP_PRESETS,
            ].map((item) => (
              <button
                key={`add-preset-${item.name}`}
                type="button"
                onClick={() => {
                  onAddDestinationPoint(
                    item.latitude,
                    item.longitude,
                    item.name,
                    item.address
                  );
                  handleCloseAddPointMenu();
                }}
                className="w-full min-h-[40px] px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] text-left flex items-center justify-between gap-2 transition-colors cursor-pointer"
              >
                <span className="text-xs font-semibold truncate">{item.name}</span>
                <Plus className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
              </button>
            ))}
          </div>
        </div>
      )}

      {addPointTab === 'custom' && (
        <form onSubmit={handleSubmitCustomPoint} className="space-y-2">
          <button
            type="button"
            onClick={() => setAddPointTab('menu')}
            className="text-[11px] font-semibold text-[#047857] dark:text-[#34D399] flex items-center gap-1 cursor-pointer"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Kembali ke Menu Add Point</span>
          </button>
          <input
            type="text"
            value={customPointName}
            onChange={(e) => {
              setCustomPointName(e.target.value);
              setCustomPointError(null);
            }}
            placeholder="Nama waypoint (mis. Maktab 45 / Gerbang King Fahd)"
            autoFocus
            className="w-full min-h-[40px] px-3 rounded-[10px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-xs outline-none"
          />
          <div className="grid grid-cols-2 gap-2">
            <div>
              <label className="block text-[10px] font-semibold text-[#4D6659] dark:text-[#9CC2AE] mb-0.5">
                Latitude
              </label>
              <input
                type="number"
                step="any"
                value={customPointLat}
                onChange={(e) => {
                  setCustomPointLat(e.target.value);
                  setCustomPointError(null);
                }}
                className="w-full min-h-[38px] px-2.5 rounded-[10px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] font-mono text-xs outline-none"
              />
            </div>
            <div>
              <label className="block text-[10px] font-semibold text-[#4D6659] dark:text-[#9CC2AE] mb-0.5">
                Longitude
              </label>
              <input
                type="number"
                step="any"
                value={customPointLng}
                onChange={(e) => {
                  setCustomPointLng(e.target.value);
                  setCustomPointError(null);
                }}
                className="w-full min-h-[38px] px-2.5 rounded-[10px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] font-mono text-xs outline-none"
              />
            </div>
          </div>
          {customPointError && (
            <p className="text-[11px] font-medium text-[#DC2626] dark:text-[#FCA5A5]">
              {customPointError}
            </p>
          )}
          <button
            type="submit"
            className="w-full min-h-[40px] rounded-[10px] bg-[#047857] hover:bg-[#065F46] text-white text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Tambah Waypoint</span>
          </button>
        </form>
      )}
    </div>
  );

  const kmValue = Math.max(
    0.5,
    Math.min(5.0, routeConfig.maxStragglerDistanceMeters / 1000)
  );

  return (
    <div className="flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Top App Bar */}
      <div className="sticky top-0 z-10 flex items-center justify-between gap-2 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="flex items-center gap-1.5 min-w-0">
          <button
            type="button"
            onClick={onBack}
            aria-label="Kembali"
            className="min-w-[42px] min-h-[42px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <h2 className="text-sm font-bold truncate">
              {showSetupStepper ? 'Buat Rombongan Baru' : 'Ruang Mutawwif'}
            </h2>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              {showSetupStepper
                ? `Langkah ${currentStep + 1} dari 4 · ${STEP_LABELS[currentStep]?.label ?? ''}`
                : 'Kode Grup & Daftar Jamaah'}
            </p>
          </div>
        </div>

        {activeRoom && (
          <button
            type="button"
            onClick={() => setShowSetupStepper((prev) => !prev)}
            className="px-2.5 py-1.5 rounded-lg text-xs font-semibold text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 cursor-pointer whitespace-nowrap shrink-0"
          >
            {showSetupStepper ? 'Lihat Grup' : 'Edit Rute'}
          </button>
        )}
      </div>

      {/* Body */}
      <div className="flex-1 px-4 py-4 space-y-4">
        {showSetupStepper ? (
          <>
            {/* Stepper Header (4 Steps) */}
            <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-1.5 grid grid-cols-4 gap-1">
              {STEP_LABELS.map((step, idx) => {
                const isActive = idx === currentStep;
                const isDone = idx < currentStep;
                return (
                  <button
                    key={step.num}
                    type="button"
                    onClick={() => setCurrentStep(idx)}
                    className={`py-2 px-0.5 rounded-[12px] flex flex-col items-center transition-colors cursor-pointer ${
                      isActive
                        ? 'bg-[#047857] text-white'
                        : isDone
                          ? 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#A7F3D0]'
                          : 'bg-[#F4F8F5] dark:bg-[#091812] text-[#4D6659] dark:text-[#9CC2AE]'
                    }`}
                  >
                    <span className="text-[10px] font-bold">
                      {isDone ? '✓' : step.num}
                    </span>
                    <span className="text-[10px] font-semibold truncate max-w-full">
                      {step.label}
                    </span>
                  </button>
                );
              })}
            </div>

            {/* Step Content Card */}
            <div className="rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-4">
              {currentStep === 0 && (
                <>
                  {!isAuthenticated && onGoogleSignIn && (
                    <div className="rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/45 border border-[#A7F3D0] dark:border-[#059669] p-2.5 flex items-center justify-between gap-2">
                      <p className="text-[11px] text-[#065F46] dark:text-[#A7F3D0] flex items-center gap-1.5 min-w-0">
                        <Cloud className="w-3.5 h-3.5 shrink-0" />
                        <span className="truncate">
                          Masuk Google agar Grup bisa diikuti jamaah secara live
                        </span>
                      </p>
                      <button
                        type="button"
                        onClick={onGoogleSignIn}
                        className="px-2.5 py-1 rounded-lg bg-[#047857] text-white text-[10px] font-semibold cursor-pointer shrink-0"
                      >
                        Masuk
                      </button>
                    </div>
                  )}

                  <div className="flex items-center gap-2">
                    <LocateFixed className="w-5 h-5 text-[#B45309]" />
                    <h3 className="text-base font-bold">
                      1. Menentukan Titik Kumpul Awal
                    </h3>
                  </div>

                  <div>
                    <label
                      htmlFor="tourNameInput"
                      className="block text-xs font-semibold text-[#1E3A2F] dark:text-[#D1FAE5] mb-1.5"
                    >
                      Nama Rombongan / Agenda Ziarah
                    </label>
                    <input
                      id="tourNameInput"
                      type="text"
                      value={tourNameDraft}
                      onChange={(e) => setTourNameDraft(e.target.value)}
                      placeholder="Contoh: Rombongan Umroh Kloter 01 Makkah"
                      className="w-full min-h-[44px] px-3.5 rounded-[12px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-sm outline-none"
                    />
                  </div>

                  <button
                    type="button"
                    disabled={!userGpsLocation}
                    onClick={onUseGpsAsOrigin}
                    className="w-full min-h-[44px] px-3.5 rounded-[12px] bg-[#FFFBEB] dark:bg-[#451A03]/50 border border-[#FDE68A] dark:border-[#B45309] disabled:opacity-50 text-[#B45309] dark:text-[#FCD34D] text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer disabled:cursor-not-allowed"
                  >
                    <LocateFixed className="w-4 h-4 shrink-0" />
                    <span>
                      {userGpsLocation
                        ? 'Gunakan Lokasi GPS Saya Sekarang'
                        : 'Sinyal GPS Belum Aktif — Pilih di Bawah / Peta'}
                    </span>
                  </button>

                  {/* Search Hotel / Miqat Start */}
                  <div className="space-y-1.5">
                    <label
                      htmlFor="originAreaSearchInput"
                      className="block text-xs font-semibold text-[#1E3A2F] dark:text-[#D1FAE5]"
                    >
                      Cari Hotel / Maktab / Titik Kumpul Awal
                    </label>
                    <div className="relative">
                      <Search className="w-4 h-4 text-[#4D6659] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <input
                        id="originAreaSearchInput"
                        type="text"
                        value={originSearchQuery}
                        onChange={(e) => setOriginSearchQuery(e.target.value)}
                        placeholder="Cari hotel Ajyad, terminal, masjid, atau miqat..."
                        className="w-full min-h-[42px] pl-9 pr-9 rounded-[12px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#B45309] text-xs outline-none"
                      />
                      {isSearchingOrigin ? (
                        <Loader2 className="w-4 h-4 text-[#B45309] animate-spin absolute right-3 top-1/2 -translate-y-1/2" />
                      ) : originSearchQuery ? (
                        <button
                          type="button"
                          onClick={() => {
                            setOriginSearchQuery('');
                            setOriginSearchResults([]);
                          }}
                          aria-label="Hapus pencarian start"
                          className="w-7 h-7 rounded-full flex items-center justify-center text-[#4D6659] hover:bg-[#D8E6DD] dark:hover:bg-[#132E24] absolute right-1.5 top-1/2 -translate-y-1/2 cursor-pointer"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      ) : null}
                    </div>
                    {originSearchResults.length > 0 && (
                      <div className="max-h-44 overflow-y-auto rounded-xl border border-[#D8E6DD] dark:border-[#1B3B2E] bg-white dark:bg-[#091812] divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E] shadow-sm">
                        {originSearchResults.map((res) => (
                          <button
                            key={res.id}
                            type="button"
                            onClick={async () => {
                              setIsSearchingOrigin(true);
                              const resolved = await selectGooglePlacePrediction(res);
                              if (placesLib) {
                                originTokenRef.current = new placesLib.AutocompleteSessionToken();
                              }
                              setIsSearchingOrigin(false);
                              if (!resolved) return;
                              onPickOriginLatLng(
                                resolved.lat,
                                resolved.lng,
                                resolved.name,
                                resolved.address
                              );
                              setOriginSearchQuery('');
                              setOriginSearchResults([]);
                            }}
                            className="w-full px-3 py-2 text-left hover:bg-[#FFFBEB] dark:hover:bg-[#451A03]/40 transition-colors cursor-pointer"
                          >
                            <p className="text-xs font-semibold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                              {res.name}
                            </p>
                            <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                              {res.address}
                            </p>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="space-y-2">
                    <p className="text-xs font-semibold text-[#1E3A2F] dark:text-[#D1FAE5]">
                      Atau Pilih Titik Kumpul Makkah / Miqat
                    </p>
                    <div className="grid grid-cols-2 gap-2">
                      {ORIGIN_PRESETS.map((hub) => {
                        const active = routeConfig.originName === hub.name;
                        return (
                          <button
                            key={hub.name}
                            type="button"
                            onClick={() =>
                              onPickOriginLatLng(
                                hub.latitude,
                                hub.longitude,
                                hub.name,
                                hub.address
                              )
                            }
                            className={`px-3 py-2.5 rounded-xl text-xs font-semibold border text-left truncate transition-colors cursor-pointer ${
                              active
                                ? 'bg-[#FFFBEB] dark:bg-[#451A03] border-[#B45309] text-[#B45309] dark:text-[#FCD34D]'
                                : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                            }`}
                          >
                            {hub.name}
                          </button>
                        );
                      })}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => setPickerMode('origin')}
                    className="w-full min-h-[46px] px-3.5 rounded-[14px] border-2 border-[#B45309] bg-[#FFFBEB]/70 dark:bg-[#451A03]/30 hover:bg-[#FFFBEB] dark:hover:bg-[#451A03]/50 text-[#B45309] dark:text-[#FCD34D] text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
                  >
                    <MapPin className="w-4 h-4 shrink-0" />
                    <span>Pilih Titik Kumpul di Peta (Google Maps)</span>
                  </button>

                  {(() => {
                    const startPt = routeConfig.waypoints[0];
                    return (
                      <div className="rounded-[14px] bg-[#FFFBEB] dark:bg-[#451A03]/35 border border-[#FDE68A] dark:border-[#B45309] p-3 flex items-start gap-2.5">
                        <div className="w-8 h-8 rounded-full bg-[#B45309] text-white flex items-center justify-center shrink-0 mt-0.5 text-xs font-bold">
                          S
                        </div>
                        <div className="min-w-0 flex-1 space-y-0.5">
                          <p className="text-[10px] font-semibold uppercase tracking-wide text-[#B45309] dark:text-[#FCD34D]">
                            Titik Kumpul Awal Aktif
                          </p>
                          <p className="text-xs font-bold text-[#0B1F16] dark:text-[#ECFDF5] truncate">
                            {routeConfig.originName || 'Hotel Area Ajyad Makkah'}
                          </p>
                          <p className="text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5] line-clamp-2">
                            {routeConfig.originAddress ||
                              (startPt
                                ? `${startPt.latitude.toFixed(4)}, ${startPt.longitude.toFixed(4)}`
                                : 'Menunggu titik kumpul')}
                          </p>
                        </div>
                      </div>
                    );
                  })()}
                </>
              )}

              {currentStep === 1 && (
                <>
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <Flag className="w-5 h-5 text-[#047857] dark:text-[#34D399] shrink-0" />
                      <h3 className="text-base font-bold truncate">
                        2. Tujuan Ibadah / Ziarah
                      </h3>
                    </div>
                    <span className="text-[11px] font-semibold text-[#047857] dark:text-[#6EE7B7] shrink-0 tabular-nums">
                      {effectiveStops.length} Point
                    </span>
                  </div>

                  {/* Ordered Multi-Point Destinations List */}
                  <div className="rounded-[16px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-[11px] font-bold uppercase tracking-wide text-[#047857] dark:text-[#6EE7B7]">
                        Urutan Rute Multi-Point
                      </p>
                      <span className="text-[10px] font-mono text-[#4D6659] dark:text-[#9CC2AE]">
                        Maks. 10 titik
                      </span>
                    </div>

                    {/* Start Hub Line */}
                    <div className="rounded-xl bg-[#FFFBEB] dark:bg-[#451A03]/35 border border-[#FDE68A] dark:border-[#B45309] px-2.5 py-2 flex items-center gap-2.5">
                      <span className="w-6 h-6 rounded-full bg-[#B45309] text-white text-[10px] font-bold flex items-center justify-center shrink-0">
                        S
                      </span>
                      <div className="min-w-0 flex-1">
                        <p className="text-[10px] font-semibold text-[#B45309] dark:text-[#FCD34D]">
                          Titik Kumpul Awal
                        </p>
                        <p className="text-xs font-bold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                          {routeConfig.originName || 'Hotel Area Ajyad Makkah'}
                        </p>
                      </div>
                    </div>

                    {/* Ordered Stops */}
                    <div className="space-y-1.5">
                      {effectiveStops.map((stop, idx) => {
                        const isLast = idx === effectiveStops.length - 1;
                        return (
                          <div
                            key={stop.id}
                            className={`rounded-xl border px-2.5 py-2 flex items-center gap-2 ${
                              isLast
                                ? 'bg-[#ECFDF5] dark:bg-[#064E3B]/45 border-[#047857]/40 dark:border-[#059669]'
                                : 'bg-white dark:bg-[#0C1E17] border-[#D8E6DD] dark:border-[#1B3B2E]'
                            }`}
                          >
                            <span
                              className={`w-6 h-6 rounded-full text-white text-[11px] font-bold flex items-center justify-center shrink-0 ${
                                isLast ? 'bg-[#047857]' : 'bg-[#065F46]'
                              }`}
                            >
                              {idx + 1}
                            </span>

                            <div className="min-w-0 flex-1">
                              <p className="text-xs font-bold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                                {stop.name}
                              </p>
                              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                                {isLast
                                  ? 'Tujuan Akhir'
                                  : `Singgah Point ${idx + 1}`}{' '}
                                · {stop.latitude.toFixed(4)},{' '}
                                {stop.longitude.toFixed(4)}
                              </p>
                            </div>

                            <div className="flex items-center gap-0.5 shrink-0">
                              <button
                                type="button"
                                disabled={idx === 0}
                                onClick={() =>
                                  onMoveDestinationPoint(stop.id, 'up')
                                }
                                aria-label={`Naikkan urutan ${stop.name}`}
                                className="min-w-[34px] min-h-[36px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/50 disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                              >
                                <ChevronUp className="w-4 h-4" />
                              </button>
                              <button
                                type="button"
                                disabled={isLast}
                                onClick={() =>
                                  onMoveDestinationPoint(stop.id, 'down')
                                }
                                aria-label={`Turunkan urutan ${stop.name}`}
                                className="min-w-[34px] min-h-[36px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/50 disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                              >
                                <ChevronDown className="w-4 h-4" />
                              </button>
                              {effectiveStops.length > 1 && (
                                <button
                                  type="button"
                                  onClick={() =>
                                    onRemoveDestinationPoint(stop.id)
                                  }
                                  aria-label={`Hapus point ${stop.name}`}
                                  className="min-w-[34px] min-h-[36px] rounded-lg flex items-center justify-center text-[#4D6659] hover:text-[#DC2626] hover:bg-[#FEE2E2] dark:hover:bg-[#450A0A]/40 cursor-pointer"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>

                    {/* Add Point Menu Trigger */}
                    {!showAddPointMenu ? (
                      <button
                        type="button"
                        disabled={effectiveStops.length >= 10}
                        onClick={handleOpenAddPointMenu}
                        className="w-full min-h-[44px] px-3.5 rounded-[12px] bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white text-xs font-bold flex items-center justify-center gap-2 transition-colors cursor-pointer disabled:cursor-not-allowed"
                      >
                        <Plus className="w-4 h-4 shrink-0" />
                        <span>
                          {effectiveStops.length >= 10
                            ? 'Batas Maksimal 10 Point Tercapai'
                            : 'Add Point'}
                        </span>
                      </button>
                    ) : (
                      renderAddPointMenuBox()
                    )}
                  </div>

                  {/* Quick Change Final Destination */}
                  <div className="space-y-1.5">
                    <label
                      htmlFor="destAreaSearchInput"
                      className="block text-xs font-semibold text-[#1E3A2F] dark:text-[#D1FAE5]"
                    >
                      Ganti Tujuan Akhir Cepat
                    </label>
                    <div className="relative">
                      <Search className="w-4 h-4 text-[#4D6659] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
                      <input
                        id="destAreaSearchInput"
                        type="text"
                        value={destSearchQuery}
                        onChange={(e) => setDestSearchQuery(e.target.value)}
                        placeholder="Cari Masjidil Haram, Arafah, Mina, Masjid Nabawi..."
                        className="w-full min-h-[42px] pl-9 pr-9 rounded-[12px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-xs outline-none"
                      />
                      {isSearchingDest ? (
                        <Loader2 className="w-4 h-4 text-[#047857] animate-spin absolute right-3 top-1/2 -translate-y-1/2" />
                      ) : destSearchQuery ? (
                        <button
                          type="button"
                          onClick={() => {
                            setDestSearchQuery('');
                            setDestSearchResults([]);
                          }}
                          aria-label="Hapus pencarian tujuan"
                          className="w-7 h-7 rounded-full flex items-center justify-center text-[#4D6659] hover:bg-[#D8E6DD] dark:hover:bg-[#132E24] absolute right-1.5 top-1/2 -translate-y-1/2 cursor-pointer"
                        >
                          <X className="w-3.5 h-3.5" />
                        </button>
                      ) : null}
                    </div>
                    {destSearchResults.length > 0 && (
                      <div className="max-h-44 overflow-y-auto rounded-xl border border-[#D8E6DD] dark:border-[#1B3B2E] bg-white dark:bg-[#091812] divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E] shadow-sm">
                        {destSearchResults.map((res) => (
                          <button
                            key={res.id}
                            type="button"
                            onClick={async () => {
                              setIsSearchingDest(true);
                              const resolved = await selectGooglePlacePrediction(res);
                              if (placesLib) {
                                destTokenRef.current = new placesLib.AutocompleteSessionToken();
                              }
                              setIsSearchingDest(false);
                              if (!resolved) return;
                              onPickDestinationLatLng(
                                resolved.lat,
                                resolved.lng,
                                resolved.name,
                                resolved.address
                              );
                              setDestSearchQuery('');
                              setDestSearchResults([]);
                            }}
                            className="w-full px-3 py-2 text-left hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
                          >
                            <p className="text-xs font-semibold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                              {res.name}
                            </p>
                            <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                              {res.address}
                            </p>
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="grid grid-cols-2 gap-2">
                    {destinationPresets.map((preset) => {
                      const active = routeConfig.destinationName === preset.name;
                      return (
                        <button
                          key={preset.name}
                          type="button"
                          onClick={() => onSelectPreset(preset)}
                          className={`px-3 py-2.5 rounded-xl text-xs font-semibold border text-left truncate transition-colors cursor-pointer ${
                            active
                              ? 'bg-[#ECFDF5] dark:bg-[#064E3B] border-[#047857] text-[#047857] dark:text-[#A7F3D0]'
                              : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                          }`}
                        >
                          {preset.name}
                        </button>
                      );
                    })}
                  </div>

                  <button
                    type="button"
                    onClick={() => setPickerMode('destination')}
                    className="w-full min-h-[44px] px-3.5 rounded-[14px] border border-[#047857] dark:border-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/25 hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/45 text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
                  >
                    <MapPin className="w-4 h-4 shrink-0" />
                    <span>Pilih Tujuan Akhir di Peta</span>
                  </button>

                  <div className="rounded-[14px] bg-[#ECFDF5] dark:bg-[#064E3B]/35 border border-[#A7F3D0] dark:border-[#059669] p-3 space-y-1">
                    <div className="flex items-center justify-between gap-2 text-[11px] font-semibold text-[#047857] dark:text-[#6EE7B7]">
                      <span className="truncate">
                        {routeConfig.originName || 'Titik Kumpul'}
                      </span>
                      <ArrowRight className="w-3.5 h-3.5 shrink-0" />
                      <span className="truncate text-[#B45309] dark:text-[#FCD34D]">
                        {routeConfig.destinationName}
                      </span>
                    </div>
                    <p className="text-[10px] font-mono text-[#047857] dark:text-[#6EE7B7]">
                      {effectiveStops.length} titik tujuan · Estimasi{' '}
                      {Math.round(routeConfig.totalRouteMeters / 1000)} km
                    </p>
                  </div>
                </>
              )}

              {currentStep === 2 && (
                <>
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <Route className="w-5 h-5 text-[#047857] dark:text-[#34D399] shrink-0" />
                      <h3 className="text-base font-bold truncate">
                        3. Pilih &amp; Gambar Jalur Rombongan
                      </h3>
                    </div>
                    <button
                      type="button"
                      onClick={() => {
                        setManualViaPoints([]);
                        onResetDefaultWaypoints();
                      }}
                      className="px-2.5 py-1 rounded-lg text-xs font-semibold text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 flex items-center gap-1 cursor-pointer shrink-0"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>Reset</span>
                    </button>
                  </div>

                  {/* Route Preference Selector */}
                  <div className="grid grid-cols-3 gap-1.5">
                    {(
                      [
                        {
                          id: 'fastest',
                          label: 'Tercepat',
                          icon: <Zap className="w-3.5 h-3.5 shrink-0" />,
                        },
                        {
                          id: 'shortest',
                          label: 'Terpendek',
                          icon: <Route className="w-3.5 h-3.5 shrink-0" />,
                        },
                        {
                          id: 'no_toll',
                          label: 'Bebas Tol',
                          icon: <Ban className="w-3.5 h-3.5 shrink-0" />,
                        },
                      ] as Array<{
                        id: RoutePreference;
                        label: string;
                        icon: ReactNode;
                      }>
                    ).map((pref) => {
                      const active =
                        (routeConfig.routePreference ?? 'fastest') === pref.id;
                      return (
                        <button
                          key={pref.id}
                          type="button"
                          onClick={() => onSelectRoutePreference(pref.id)}
                          className={`min-h-[38px] px-2 py-1.5 rounded-xl text-[11px] font-semibold border flex items-center justify-center gap-1 transition-colors cursor-pointer ${
                            active
                              ? 'bg-[#047857] text-white border-[#047857]'
                              : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E] text-[#1E3A2F] dark:text-[#D1FAE5]'
                          }`}
                        >
                          {pref.icon}
                          <span className="truncate">{pref.label}</span>
                        </button>
                      );
                    })}
                  </div>

                  {/* Dynamic Waypoint Destinasi Manager + Add Point inside Route Config */}
                  <div className="rounded-[16px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <p className="text-[11px] font-bold uppercase tracking-wide text-[#047857] dark:text-[#6EE7B7] truncate">
                        Waypoint Destinasi ({effectiveStops.length}/10)
                      </p>
                      <button
                        type="button"
                        disabled={effectiveStops.length >= 10}
                        onClick={() =>
                          showAddPointMenu
                            ? handleCloseAddPointMenu()
                            : handleOpenAddPointMenu()
                        }
                        className="min-h-[34px] px-2.5 py-1 rounded-lg bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white text-[11px] font-bold flex items-center gap-1 transition-colors cursor-pointer disabled:cursor-not-allowed shrink-0"
                      >
                        <Plus className="w-3.5 h-3.5 shrink-0" />
                        <span>Add Point</span>
                      </button>
                    </div>

                    <div className="space-y-1.5 max-h-40 overflow-y-auto pr-0.5">
                      {effectiveStops.map((stop, idx) => {
                        const isLast = idx === effectiveStops.length - 1;
                        return (
                          <div
                            key={`route-stop-${stop.id}`}
                            className={`rounded-xl border px-2.5 py-1.5 flex items-center gap-2 ${
                              isLast
                                ? 'bg-[#ECFDF5] dark:bg-[#064E3B]/45 border-[#047857]/40 dark:border-[#059669]'
                                : 'bg-white dark:bg-[#0C1E17] border-[#D8E6DD] dark:border-[#1B3B2E]'
                            }`}
                          >
                            <span
                              className={`w-5 h-5 rounded-full text-white text-[10px] font-bold flex items-center justify-center shrink-0 ${
                                isLast ? 'bg-[#047857]' : 'bg-[#065F46]'
                              }`}
                            >
                              {idx + 1}
                            </span>
                            <div className="min-w-0 flex-1">
                              <p className="text-xs font-semibold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                                {stop.name}
                              </p>
                              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate font-mono">
                                {isLast ? 'Tujuan Akhir' : `Singgah ${idx + 1}`} ·{' '}
                                {stop.latitude.toFixed(4)}, {stop.longitude.toFixed(4)}
                              </p>
                            </div>
                            <div className="flex items-center gap-0.5 shrink-0">
                              <button
                                type="button"
                                disabled={idx === 0}
                                onClick={() => onMoveDestinationPoint(stop.id, 'up')}
                                aria-label={`Naikkan waypoint ${stop.name}`}
                                className="w-7 h-7 rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/50 disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                              >
                                <ChevronUp className="w-3.5 h-3.5" />
                              </button>
                              <button
                                type="button"
                                disabled={isLast}
                                onClick={() => onMoveDestinationPoint(stop.id, 'down')}
                                aria-label={`Turunkan waypoint ${stop.name}`}
                                className="w-7 h-7 rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/50 disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                              >
                                <ChevronDown className="w-3.5 h-3.5" />
                              </button>
                              {effectiveStops.length > 1 && (
                                <button
                                  type="button"
                                  onClick={() => onRemoveDestinationPoint(stop.id)}
                                  aria-label={`Hapus waypoint ${stop.name}`}
                                  className="w-7 h-7 rounded-lg flex items-center justify-center text-[#4D6659] hover:text-[#DC2626] hover:bg-[#FEE2E2] dark:hover:bg-[#450A0A]/40 cursor-pointer"
                                >
                                  <Trash2 className="w-3.5 h-3.5" />
                                </button>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>

                    {showAddPointMenu && renderAddPointMenuBox()}
                  </div>

                  <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5]">
                    Ketuk peta untuk menambah lintasan (via) atau tekan{' '}
                    <span className="font-semibold text-[#047857] dark:text-[#34D399]">
                      Add Point
                    </span>{' '}
                    untuk menambah waypoint destinasi baru.
                  </p>

                  <div className="relative w-full h-[220px] rounded-[16px] bg-[#EBF3EE] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] overflow-hidden">
                    <GoogleStepRouteMap
                      waypoints={routeConfig.waypoints}
                      destinationStops={effectiveStops}
                      manualViaPoints={manualViaPoints}
                      vehicleType={vehicleType}
                      routePreference={routeConfig.routePreference ?? 'fastest'}
                      darkMode={darkMode}
                      onMapClick={handleStepMapClick}
                      onPreviewRouteComputed={(
                        distMeters,
                        durMillis,
                        path,
                        instructions
                      ) => {
                        setPreviewStats({
                          distanceMeters: distMeters,
                          durationMillis: durMillis,
                        });
                        if (onRouteComputed) {
                          onRouteComputed(
                            distMeters,
                            durMillis,
                            path,
                            instructions
                          );
                        }
                      }}
                    />
                  </div>

                  <div className="flex items-center justify-between gap-2 text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                    <span className="font-mono tabular-nums truncate">
                      {(
                        (previewStats?.distanceMeters ??
                          routeConfig.totalRouteMeters) / 1000
                      ).toFixed(1)}{' '}
                      km
                      {previewStats && previewStats.durationMillis > 0
                        ? ` · Est. ${Math.max(1, Math.round(previewStats.durationMillis / 60000))} mnt`
                        : ''}{' '}
                      · {effectiveStops.length} tujuan
                    </span>
                    {manualViaPoints.length > 0 && (
                      <button
                        type="button"
                        onClick={handleRemoveLastViaPoint}
                        className="px-2 py-1 rounded-lg bg-[#FEE2E2] dark:bg-[#450A0A] text-[#DC2626] dark:text-[#FCA5A5] font-semibold cursor-pointer shrink-0"
                      >
                        Hapus Lintasan ({manualViaPoints.length})
                      </button>
                    )}
                  </div>
                </>
              )}

              {currentStep === 3 && (
                <>
                  <div className="flex items-center gap-2">
                    <Gauge className="w-5 h-5 text-[#047857] dark:text-[#34D399]" />
                    <h3 className="text-base font-bold">
                      4. Batas Jarak &amp; Konfirmasi
                    </h3>
                  </div>

                  <div className="rounded-[16px] bg-[#ECFDF5] dark:bg-[#064E3B]/50 p-4 space-y-1">
                    <p className="text-xs font-semibold text-[#047857] dark:text-[#6EE7B7]">
                      Jarak Maksimal dari Pendamping Belakang
                    </p>
                    <p className="text-2xl font-bold font-mono tabular-nums text-[#064E3B] dark:text-white">
                      {kmValue.toFixed(1)} km
                    </p>
                    <p className="text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5]">
                      Peringatan otomatis muncul bila jamaah paling belakang terpisah melewati batas jarak ini.
                    </p>
                  </div>

                  <div className="space-y-1.5">
                    <input
                      type="range"
                      aria-label="Slider Batas Jarak Jamaah Terpisah"
                      min="0.5"
                      max="5.0"
                      step="0.5"
                      value={kmValue}
                      onChange={(e) =>
                        onUpdateMaxStragglerDistance(
                          Math.round(parseFloat(e.target.value) * 1000)
                        )
                      }
                      className="w-full accent-[#B45309] cursor-pointer h-2 rounded-lg"
                    />
                    <div className="flex justify-between text-[11px] font-mono text-[#4D6659] dark:text-[#9CC2AE]">
                      <span>0.5 km</span>
                      <span>2.5 km</span>
                      <span>5.0 km</span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between gap-3 pt-1">
                    <div>
                      <p className="text-xs font-semibold">
                        Peringatan Suara Otomatis
                      </p>
                      <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                        Bunyikan nada peringatan saat jamaah terpisah atau keluar jalur.
                      </p>
                    </div>
                    <button
                      type="button"
                      role="switch"
                      aria-checked={routeConfig.soundAlertEnabled}
                      onClick={onToggleSoundAlert}
                      className={`w-12 h-7 rounded-full p-1 transition-colors cursor-pointer shrink-0 ${
                        routeConfig.soundAlertEnabled
                          ? 'bg-[#047857]'
                          : 'bg-[#C6DACD] dark:bg-[#1E3F31]'
                      }`}
                    >
                      <span
                        className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                          routeConfig.soundAlertEnabled
                            ? 'translate-x-5'
                            : 'translate-x-0'
                        }`}
                      />
                    </button>
                  </div>

                  <div className="rounded-[14px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 space-y-2">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 text-xs font-bold">
                        <LocateFixed className="w-3.5 h-3.5 text-[#B45309] shrink-0" />
                        <span className="truncate">
                          {routeConfig.originName || 'Titik Kumpul'}
                        </span>
                      </div>
                      {effectiveStops.map((stop, idx) => (
                        <div
                          key={`confirm-${stop.id}`}
                          className="flex items-center gap-2 text-xs font-semibold pl-1"
                        >
                          <span className="w-4 h-4 rounded-full bg-[#047857] text-white text-[9px] font-bold flex items-center justify-center shrink-0">
                            {idx + 1}
                          </span>
                          <span className="truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                            {stop.name}
                          </span>
                        </div>
                      ))}
                    </div>
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                      Rute:{' '}
                      {routeConfig.routePreference === 'shortest'
                        ? 'Terpendek'
                        : routeConfig.routePreference === 'no_toll'
                          ? 'Bebas Tol'
                          : 'Tercepat'}{' '}
                      · {effectiveStops.length} titik tujuan ·{' '}
                      {(routeConfig.totalRouteMeters / 1000).toFixed(1)} km
                    </p>
                  </div>

                  {onAddDraftActivity && (
                    <div className="pt-1">
                      <RoomActivityChecklistPanel
                        mode="draft"
                        activities={draftActivities}
                        currentUid={currentUid}
                        isRoomCreator={true}
                        onAddActivity={onAddDraftActivity}
                        onRemoveActivity={onRemoveDraftActivity}
                        onMoveActivity={onMoveDraftActivity}
                        onApplyStarterTemplates={onApplyDraftStarterTemplates}
                      />
                    </div>
                  )}
                </>
              )}

              <div className="h-px bg-[#D8E6DD] dark:bg-[#1B3B2E]" />

              {createRoomError && (
                <div className="rounded-xl bg-[#FEF2F2] dark:bg-[#450A0A]/40 border border-[#FECACA] dark:border-[#991B1B] px-3 py-2.5 text-xs text-[#B91C1C] dark:text-[#FCA5A5] space-y-2">
                  <p>{createRoomError}</p>
                  {onGoogleSignIn && !isAuthenticated && (
                    <button
                      type="button"
                      onClick={onGoogleSignIn}
                      className="w-full min-h-[38px] rounded-lg bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <Cloud className="w-3.5 h-3.5" />
                      <span>Masuk dengan Google (Grup Cloud Live)</span>
                    </button>
                  )}
                </div>
              )}

              {/* Stepper Footer Actions */}
              <div className="space-y-2">
                <div className="flex items-center gap-2.5">
                  {currentStep > 0 && (
                    <button
                      type="button"
                      onClick={() => setCurrentStep((s) => s - 1)}
                      className="flex-1 min-h-[46px] rounded-[14px] border border-[#C6DACD] dark:border-[#1E3F31] text-xs font-semibold cursor-pointer"
                    >
                      Sebelumnya
                    </button>
                  )}
                  {currentStep < 3 ? (
                    <button
                      type="button"
                      onClick={() => setCurrentStep((s) => s + 1)}
                      className="flex-1 min-h-[46px] rounded-[14px] bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <span>Berikutnya</span>
                      <ArrowRight className="w-4 h-4" />
                    </button>
                  ) : (
                    <button
                      type="button"
                      disabled={isCreatingRoom}
                      onClick={() =>
                        onCreateRoom(
                          tourNameDraft.trim() || 'Rombongan Ziarah Makkah'
                        )
                      }
                      className="flex-1 min-h-[46px] rounded-[14px] bg-[#B45309] hover:bg-[#92400E] disabled:opacity-60 text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
                    >
                      <Check className="w-4 h-4" />
                      <span>
                        {isCreatingRoom
                          ? 'Membuat Grup...'
                          : 'Buat Grup Cloud & Mulai'}
                      </span>
                    </button>
                  )}
                </div>

                {currentStep === 3 && onCreateLocalRoom && !isAuthenticated && (
                  <button
                    type="button"
                    onClick={() =>
                      onCreateLocalRoom(
                        tourNameDraft.trim() || 'Rombongan Ziarah Makkah'
                      )
                    }
                    className="w-full min-h-[42px] rounded-[12px] border border-[#C6DACD] dark:border-[#1E3F31] bg-[#F4F8F5] dark:bg-[#091812] hover:bg-[#E6EFE9] dark:hover:bg-[#132E24] text-xs font-semibold text-[#1E3A2F] dark:text-[#D1FAE5] cursor-pointer transition-colors"
                  >
                    Atau Buat Grup Lokal (Offline / Demo)
                  </button>
                )}
              </div>
            </div>
          </>
        ) : (
          activeRoom && (
            <>
              {/* Active Group Info Card + Bagikan Link & Kode */}
              <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 space-y-4">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                      {activeRoom.roomTitle}
                    </p>
                    <p className="text-2xl font-bold font-mono tracking-tight text-[#047857] dark:text-[#34D399]">
                      {activeRoom.roomId}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleCopy(activeRoom.roomId, 'id')}
                    className="min-h-[40px] px-3.5 py-2 rounded-xl border border-[#C6DACD] dark:border-[#1E3F31] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
                  >
                    {copiedType === 'id' ? (
                      <Check className="w-3.5 h-3.5 text-[#047857]" />
                    ) : (
                      <Copy className="w-3.5 h-3.5" />
                    )}
                    <span>{copiedType === 'id' ? 'Disalin' : 'Salin Kode'}</span>
                  </button>
                </div>

                <div className="h-px bg-[#D8E6DD] dark:bg-[#1B3B2E]" />

                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] px-3.5 py-2">
                    <div className="min-w-0 flex-1">
                      <p className="text-[10px] font-semibold text-[#4D6659] dark:text-[#9CC2AE]">
                        Link Undangan Browser (Web)
                      </p>
                      <p className="text-xs font-mono truncate">
                        {getWebRoomShareLink(activeRoom.roomId)}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() =>
                        handleCopy(getWebRoomShareLink(activeRoom.roomId), 'web')
                      }
                      aria-label="Salin Link Web"
                      className="min-w-[36px] min-h-[36px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] cursor-pointer shrink-0"
                    >
                      {copiedType === 'web' ? (
                        <Check className="w-4 h-4 text-[#047857]" />
                      ) : (
                        <Copy className="w-4 h-4" />
                      )}
                    </button>
                  </div>

                  <div className="flex items-center justify-between gap-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] px-3.5 py-2">
                    <div className="min-w-0 flex-1">
                      <p className="text-[10px] font-semibold text-[#4D6659] dark:text-[#9CC2AE]">
                        Link Undangan APK (Deep Link)
                      </p>
                      <p className="text-xs font-mono truncate">
                        {activeRoom.inviteLink}
                      </p>
                    </div>
                    <button
                      type="button"
                      onClick={() => handleCopy(activeRoom.inviteLink, 'link')}
                      aria-label="Salin Link APK"
                      className="min-w-[36px] min-h-[36px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] cursor-pointer shrink-0"
                    >
                      {copiedType === 'link' ? (
                        <Check className="w-4 h-4 text-[#047857]" />
                      ) : (
                        <Copy className="w-4 h-4" />
                      )}
                    </button>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleNativeShareRoom(activeRoom)}
                    className="w-full min-h-[42px] px-3.5 py-2 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/50 border border-[#A7F3D0] dark:border-[#059669] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
                  >
                    <Share2 className="w-4 h-4 shrink-0" />
                    <span>Bagikan Link &amp; Kode Grup</span>
                  </button>
                </div>

                <div className="grid grid-cols-2 gap-2.5">
                  <button
                    type="button"
                    onClick={() =>
                      onCreateRoom(
                        tourNameDraft.trim() || 'Rombongan Ziarah Makkah'
                      )
                    }
                    className="min-h-[44px] px-3 py-2 rounded-xl border border-[#C6DACD] dark:border-[#1E3F31] text-xs font-semibold cursor-pointer whitespace-nowrap"
                  >
                    Buat Kode Baru
                  </button>
                  {!activeRoom.isSyncedRoom && (
                    <button
                      type="button"
                      onClick={onSimulateJoin}
                      className="min-h-[44px] px-3 py-2 rounded-xl bg-[#047857] text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer whitespace-nowrap"
                    >
                      <UserPlus className="w-3.5 h-3.5 shrink-0" />
                      <span>Simulasi Jamaah</span>
                    </button>
                  )}
                  {activeRoom.isSyncedRoom && (
                    <div className="min-h-[44px] px-3 py-2 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399] text-xs font-semibold flex items-center justify-center gap-1.5">
                      <Cloud className="w-3.5 h-3.5 shrink-0" />
                      <span>Sync Cloud Aktif</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Active Room Route Waypoints Quick Manager */}
              <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-2.5">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 min-w-0">
                    <Route className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
                    <h3 className="text-xs font-bold truncate">
                      Rute &amp; Waypoint Destinasi ({effectiveStops.length}/10)
                    </h3>
                  </div>
                  <button
                    type="button"
                    disabled={effectiveStops.length >= 10}
                    onClick={() =>
                      showAddPointMenu
                        ? handleCloseAddPointMenu()
                        : handleOpenAddPointMenu()
                    }
                    className="min-h-[34px] px-2.5 py-1 rounded-lg bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white text-[11px] font-bold flex items-center gap-1 transition-colors cursor-pointer disabled:cursor-not-allowed shrink-0"
                  >
                    <Plus className="w-3.5 h-3.5 shrink-0" />
                    <span>Add Point</span>
                  </button>
                </div>

                <div className="space-y-1.5">
                  {effectiveStops.map((stop, idx) => {
                    const isLast = idx === effectiveStops.length - 1;
                    return (
                      <div
                        key={`active-stop-${stop.id}`}
                        className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-2.5 py-1.5 flex items-center gap-2"
                      >
                        <span
                          className={`w-5 h-5 rounded-full text-white text-[10px] font-bold flex items-center justify-center shrink-0 ${
                            isLast ? 'bg-[#047857]' : 'bg-[#065F46]'
                          }`}
                        >
                          {idx + 1}
                        </span>
                        <div className="min-w-0 flex-1">
                          <p className="text-xs font-semibold truncate">
                            {stop.name}
                          </p>
                        </div>
                        <div className="flex items-center gap-0.5 shrink-0">
                          <button
                            type="button"
                            disabled={idx === 0}
                            onClick={() => onMoveDestinationPoint(stop.id, 'up')}
                            aria-label={`Naikkan ${stop.name}`}
                            className="w-7 h-7 rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                          >
                            <ChevronUp className="w-3.5 h-3.5" />
                          </button>
                          <button
                            type="button"
                            disabled={isLast}
                            onClick={() => onMoveDestinationPoint(stop.id, 'down')}
                            aria-label={`Turunkan ${stop.name}`}
                            className="w-7 h-7 rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                          >
                            <ChevronDown className="w-3.5 h-3.5" />
                          </button>
                          {effectiveStops.length > 1 && (
                            <button
                              type="button"
                              onClick={() => onRemoveDestinationPoint(stop.id)}
                              aria-label={`Hapus ${stop.name}`}
                              className="w-7 h-7 rounded-lg flex items-center justify-center text-[#4D6659] hover:text-[#DC2626] cursor-pointer"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>

                {showAddPointMenu && renderAddPointMenuBox()}
              </div>

              {/* Member List */}
              <div className="space-y-2.5">
                <h3 className="text-sm font-semibold">
                  Daftar Jamaah Rombongan ({activeRoom.members.length})
                </h3>
                {activeRoom.members.map((member) => {
                  const isLeader = member.role === 'TOUR_LEADER';
                  return (
                    <div
                      key={member.id}
                      className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 flex items-center gap-3"
                    >
                      <div className="relative shrink-0">
                        <ConvoyAvatar
                          preset={member.avatarPreset}
                          photoUrl={member.avatarPhotoUrl}
                          name={member.fullName}
                          sizeClass="w-10 h-10"
                          ringClass={
                            isLeader
                              ? 'ring-2 ring-[#047857]'
                              : 'ring-1 ring-[#C6DACD] dark:ring-[#1E3F31]'
                          }
                        />
                        <span
                          className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-full border-2 border-white dark:border-[#0C1E17] flex items-center justify-center ${
                            isLeader
                              ? 'bg-[#047857] text-white'
                              : 'bg-[#F4F8F5] dark:bg-[#091812] text-[#2D4A3D] dark:text-[#D1FAE5]'
                          }`}
                        >
                          {member.vehicleType === 'MOBIL' ? (
                            <Bus className="w-2.5 h-2.5" />
                          ) : (
                            <Footprints className="w-2.5 h-2.5" />
                          )}
                        </span>
                      </div>

                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-semibold truncate">
                          {member.fullName}
                        </p>
                        <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] truncate">
                          {isLeader ? 'Mutawwif' : 'Jamaah'} ·{' '}
                          {member.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'} ·{' '}
                          <span className="font-mono">{member.licensePlate}</span>
                        </p>
                      </div>

                      {!isLeader && (
                        <button
                          type="button"
                          onClick={() => onRemoveMember(member.id)}
                          aria-label="Keluarkan jamaah"
                          className="min-w-[40px] min-h-[40px] rounded-lg flex items-center justify-center text-[#4D6659] hover:text-[#DC2626] cursor-pointer shrink-0"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>

              {/* Active Room Umrah Activity Checklist Manager */}
              <div className="space-y-2.5 pt-1">
                <RoomActivityChecklistPanel
                  mode="room"
                  activities={activeRoomActivities}
                  progressByMember={activityProgressByMember}
                  members={activeRoom.members}
                  currentUid={currentUid}
                  isRoomCreator={true}
                  roomId={activeRoom.roomId}
                  roomTitle={activeRoom.roomTitle}
                  isCloudSynced={activeRoom.isSyncedRoom}
                  onToggleActivity={onToggleRoomActivity}
                  onAddActivity={onAddRoomActivity}
                  onRemoveActivity={onRemoveRoomActivity}
                  onMoveActivity={onMoveRoomActivity}
                  onApplyStarterTemplates={onApplyRoomStarterTemplates}
                />
              </div>
            </>
          )
        )}
      </div>

      {/* Bottom CTA when viewing created group */}
      {!showSetupStepper && activeRoom && (
        <div className="sticky bottom-0 mt-auto px-4 pt-3.5 pb-[calc(2rem+env(safe-area-inset-bottom))] mb-2 bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E]">
          <button
            type="button"
            onClick={onEnterMapPlaceholder}
            className="w-full min-h-[48px] px-4 py-3 rounded-[14px] bg-[#047857] hover:bg-[#065F46] active:scale-[0.98] text-white text-sm font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
          >
            <MapIcon className="w-4 h-4 shrink-0" />
            <span className="whitespace-nowrap">Buka Peta Jamaah Live</span>
          </button>
        </div>
      )}

      {pickerMode && (
        <GoogleRoutePickerModal
          mode={pickerMode}
          initialPoint={
            pickerMode === 'origin'
              ? routeConfig.waypoints[0] || {
                  xFraction: 0.14,
                  yFraction: 0.86,
                  latitude: 21.418,
                  longitude: 39.8275,
                }
              : routeConfig.waypoints[routeConfig.waypoints.length - 1] || {
                  xFraction: 0.86,
                  yFraction: 0.14,
                  latitude: 21.4225,
                  longitude: 39.8262,
                }
          }
          initialName={
            pickerMode === 'origin'
              ? routeConfig.originName || 'Titik Kumpul Rombongan'
              : routeConfig.destinationName
          }
          userGpsLocation={userGpsLocation}
          darkMode={darkMode}
          onDismiss={() => setPickerMode(null)}
          onConfirm={(lat, lng, placeName, addressLabel) => {
            if (pickerMode === 'origin') {
              onPickOriginLatLng(lat, lng, placeName, addressLabel);
            } else if (pickerMode === 'add_point') {
              onAddDestinationPoint(lat, lng, placeName, addressLabel);
            } else {
              onPickDestinationLatLng(lat, lng, placeName, addressLabel);
            }
            setPickerMode(null);
          }}
        />
      )}
    </div>
  );
}
