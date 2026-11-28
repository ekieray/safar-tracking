import { useCallback, useEffect, useRef, useState } from 'react';
import { APIProvider } from '@vis.gl/react-google-maps';
import { AnimatePresence, motion } from 'motion/react';
import { User } from 'firebase/auth';
import {
  BookOpenCheck,
  Check,
  Compass,
  Copy,
  History,
  MapPin,
  Moon,
  RotateCcw,
  Sparkles,
  Sun,
} from 'lucide-react';
import makkahBackdropImg from './assets/images/makkah_haram_backdrop_1795511632163.jpg';
import safarTrackingIcon from './assets/images/safar_tracking_icon_1795599322033.jpg';
import { MANASIK_GUIDE_MAP } from './data/manasikGuideData';
import { createStarterRoomUmrahActivities } from './data/umrahActivityTemplates';
import { KotlinSourceExplorer } from './components/KotlinSourceExplorer';
import { AboutAppView } from './components/screens/AboutAppView';
import { AdminBackendView } from './components/screens/AdminBackendView';
import { CustomizeProfileView } from './components/screens/CustomizeProfileView';
import { FamilyMonitorView } from './components/screens/FamilyMonitorView';
import { JoinTourView } from './components/screens/JoinTourView';
import { LiveTrackingMapView } from './components/screens/LiveTrackingMapView';
import { LoginView } from './components/screens/LoginView';
import { ManasikChatbotView } from './components/screens/ManasikChatbotView';
import { ManasikGuideView } from './components/screens/ManasikGuideView';
import { RoleSelectionView } from './components/screens/RoleSelectionView';
import { TourLeaderView } from './components/screens/TourLeaderView';
import { TripDetailView } from './components/screens/TripDetailView';
import { TripHistoryView } from './components/screens/TripHistoryView';
import { VehicleProfileView } from './components/screens/VehicleProfileView';
import {
  consumeGoogleRedirectResult,
  ensureCloudSignedInUser,
  isDeviceCloudGuestEmail,
  mapFirebaseAuthError,
  sendResetPasswordEmail,
  signInWithEmail,
  signInWithFacebook,
  signInWithGoogle,
  signInWithGoogleForMobileBridge,
  signOutFirebase,
  signUpWithEmail,
  subscribeToAuthState,
  testFirestoreConnection,
} from './firebase';
import { LiveGpsFix, useDeviceGps } from './hooks/useDeviceGps';
import {
  addRoomCheckpoint,
  addSyncedRoomActivity,
  assignSyncedSweeper,
  clearFamilyGrant,
  clearPriorityRoomBroadcast,
  createOrGetFamilyInviteLink,
  createSyncedRoom,
  DEFAULT_GLOBAL_APP_CONFIG,
  extractFamilyInviteCode,
  finishSyncedRoom,
  isAdminEmailAddress,
  isVerifiedAdminSession,
  joinSyncedRoom,
  loadActiveFamilyGrant,
  loadLocalSession,
  loadManasikProgressFromFirestore,
  loadManasikProgressLocal,
  loadProfileByEmail,
  loadUserPrivateProfile,
  loadUserTripsFromFirestore,
  normalizeProfileEmail,
  publishMemberTelemetry,
  publishUserPresence,
  recoverOriginalLeader,
  redeemFamilyInviteLink,
  regenerateFamilyInviteLink,
  removeRoomCheckpoint,
  removeSyncedMember,
  removeSyncedRoomActivity,
  resolveSyncedSos,
  revokeFamilyInviteLink,
  saveLocalSession,
  saveManasikProgressLocal,
  saveManasikProgressToFirestore,
  saveMemberRoomActivityProgress,
  saveProfileByEmail,
  saveTripRecordToFirestore,
  saveUserPrivateProfile,
  sendPriorityRoomBroadcast,
  sendRoomMessage,
  subscribeToFamilyMonitor,
  subscribeToGlobalAppConfig,
  subscribeToMyFamilyLink,
  subscribeToSyncedRoom,
  subscribeToTourLeaderEligibility,
  syncCoLeaderAssignment,
  triggerCoLeaderFailover,
  triggerSyncedSos,
  updateSyncedMemberCondition,
  updateSyncedRoomActivity,
  updateSyncedRoomRoute,
} from './services/convoySyncService';
import {
  enqueuePriorityBroadcast,
  enqueueRoomMessage,
  enqueueTelemetryUpdate,
  flushOfflineSyncQueue,
  getOfflineQueueCount,
} from './services/offlineSyncQueue';
import {
  AuthProviderId,
  ConnectionStatus,
  ConvoyMember,
  ConvoyRouteConfig,
  DestinationPreset,
  DestinationStop,
  FamilyGrantRecord,
  FamilyLinkRecord,
  FamilyMonitorSnapshot,
  GlobalAppConfig,
  ManasikCheckInRecord,
  ManasikGuideType,
  ManasikProgressState,
  MapPosition,
  MemberConditionStatus,
  MissingPilgrimAlertState,
  OffRouteAlertState,
  RestCheckpoint,
  RoomActivityItem,
  RoomActivityMemberProgress,
  RoomMessage,
  RouteInstruction,
  RoutePoint,
  RoutePreference,
  ScreenRoute,
  SosEmergencyState,
  StragglerAlertState,
  TourRoom,
  TripHistoryRecord,
  UserRole,
  VehicleProfile,
  VehicleType,
} from './types/convoy';
import {
  computeClosestCoLeader,
  computePolylineLengthMeters,
  downsampleRoutePoints,
  downsampleTrackPoints,
  evaluatePositionAlongRoute,
  haversineMeters,
  LatLngPoint,
} from './utils/geo';

const INITIAL_PROFILE: VehicleProfile = {
  fullName: '',
  email: '',
  authProvider: 'local',
  clubName: '',
  homeCity: '',
  bio: '',
  instagramHandle: '',
  vehicleType: 'MOBIL',
  vehicleDetail: '',
  licensePlate: '',
  passportNumber: '',
  emergencyContact: '',
  avatarPreset: 'HELMET_TOURING',
};

const DESTINATION_PRESETS: DestinationPreset[] = [
  {
    name: "Masjidil Haram (Ka'bah)",
    endPoint: { xFraction: 0.86, yFraction: 0.14, latitude: 21.4225, longitude: 39.8262 },
    totalMeters: 1200,
  },
  {
    name: 'Jabal Rahmah · Arafah',
    endPoint: { xFraction: 0.78, yFraction: 0.22, latitude: 21.3549, longitude: 39.9841 },
    totalMeters: 22500,
  },
  {
    name: 'Jabal Nur · Gua Hira',
    endPoint: { xFraction: 0.24, yFraction: 0.18, latitude: 21.4575, longitude: 39.8594 },
    totalMeters: 7200,
  },
  {
    name: 'Masjid Nabawi · Madinah',
    endPoint: { xFraction: 0.88, yFraction: 0.44, latitude: 24.4672, longitude: 39.6112 },
    totalMeters: 440000,
  },
];

const INITIAL_ROUTE_CONFIG: ConvoyRouteConfig = {
  originName: 'Hotel Area Ajyad Makkah',
  originAddress: 'Ajyad Street, Area Masjidil Haram · 21.4180, 39.8275',
  originLockedByLeader: false,
  destinationName: "Masjidil Haram (Ka'bah)",
  destinationAddress: 'Al Haram, Makkah 24231 · 21.4225, 39.8262',
  destinationStops: [
    {
      id: 'stop-default-1',
      name: "Masjidil Haram (Ka'bah)",
      address: 'Al Haram, Makkah 24231 · 21.4225, 39.8262',
      latitude: 21.4225,
      longitude: 39.8262,
    },
  ],
  routePreference: 'fastest',
  waypoints: [
    { xFraction: 0.14, yFraction: 0.86, latitude: 21.418, longitude: 39.8275 },
    { xFraction: 0.32, yFraction: 0.65, latitude: 21.4192, longitude: 39.8272 },
    { xFraction: 0.55, yFraction: 0.48, latitude: 21.4204, longitude: 39.8269 },
    { xFraction: 0.72, yFraction: 0.3, latitude: 21.4215, longitude: 39.8265 },
    { xFraction: 0.86, yFraction: 0.14, latitude: 21.4225, longitude: 39.8262 },
  ],
  routeInstructions: [],
  checkpoints: [],
  totalRouteMeters: 1200,
  offRouteToleranceMeters: 75,
  maxStragglerDistanceMeters: 1000,
  soundAlertEnabled: true,
};

interface SimTemplate {
  fullName: string;
  vehicleType: VehicleType;
  vehicleDetail: string;
  licensePlate: string;
  emergencyContact: string;
  connectionStatus: ConnectionStatus;
  conditionStatus: MemberConditionStatus;
  lastLocationText: string;
  baseProgress: number;
  baseSpeed: number;
  avatarPreset: VehicleProfile['avatarPreset'];
}

const DEFAULT_CONVOY_TEMPLATES: SimTemplate[] = [
  {
    fullName: 'Ust. H. Ahmad Fauzi',
    vehicleType: 'MOBIL',
    vehicleDetail: 'Bus Rombongan 01',
    licensePlate: 'JKT-01 / KMR 402',
    emergencyContact: '0812-3301-7712',
    connectionStatus: 'CONNECTED',
    conditionStatus: 'AMAN',
    lastLocationText: 'Gerbang King Abdul Aziz',
    baseProgress: 0.68,
    baseSpeed: 18,
    avatarPreset: 'RIDER_CAPTAIN',
  },
  {
    fullName: 'Hj. Siti Aminah',
    vehicleType: 'MOTOR',
    vehicleDetail: 'Jalan Kaki · Sektor A',
    licensePlate: 'JKT-01 / KMR 405',
    emergencyContact: '0813-9012-4411',
    connectionStatus: 'CONNECTED',
    conditionStatus: 'AMAN',
    lastLocationText: 'Pelataran Masjidil Haram',
    baseProgress: 0.6,
    baseSpeed: 5,
    avatarPreset: 'HELMET_TOURING',
  },
  {
    fullName: 'Bpk. H. Rahmat',
    vehicleType: 'MOTOR',
    vehicleDetail: 'Jalan Kaki · Sektor B',
    licensePlate: 'JKT-01 / KMR 410',
    emergencyContact: '0857-2201-8834',
    connectionStatus: 'WEAK_SIGNAL',
    conditionStatus: 'ISTIRAHAT',
    lastLocationText: 'Area Zamzam Tower',
    baseProgress: 0.54,
    baseSpeed: 4,
    avatarPreset: 'HELMET_SPORT',
  },
  {
    fullName: 'Ibu Nadia Kusuma',
    vehicleType: 'MOBIL',
    vehicleDetail: 'Bus Rombongan 02',
    licensePlate: 'JKT-02 / KMR 312',
    emergencyContact: '0811-3349-7720',
    connectionStatus: 'CONNECTED',
    conditionStatus: 'AMAN',
    lastLocationText: 'Terminal Syib Amir',
    baseProgress: 0.46,
    baseSpeed: 22,
    avatarPreset: 'PILOT_CLASSIC',
  },
];

function currentClock(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

function createInitialMessages(roomCode: string): RoomMessage[] {
  return [
    {
      id: 'init-sys',
      senderId: 'system',
      senderName: 'Sistem Rombongan',
      senderVehicle: '',
      type: 'SYSTEM',
      textContent: `Grup ${roomCode} aktif. Jaga kekompakan rombongan dan pantau posisi Pendamping.`,
      timestampLabel: currentClock(),
    },
    {
      id: 'init-1',
      senderId: 'leader',
      senderName: 'Ust. H. Ahmad Fauzi',
      senderVehicle: 'Bus Rombongan 01',
      type: 'TEXT',
      textContent: 'Rombongan siap berangkat. Ikuti jalur resmi yang sudah ditetapkan menuju Masjidil Haram.',
      timestampLabel: currentClock(),
      senderAvatarPreset: 'RIDER_CAPTAIN',
    },
  ];
}

function buildPolylineToDestination(start: RoutePoint, end: RoutePoint): RoutePoint[] {
  const mid1: RoutePoint = {
    xFraction: start.xFraction + (end.xFraction - start.xFraction) * 0.33,
    yFraction: start.yFraction + (end.yFraction - start.yFraction) * 0.33,
    latitude: start.latitude + (end.latitude - start.latitude) * 0.33,
    longitude: start.longitude + (end.longitude - start.longitude) * 0.33,
  };
  const mid2: RoutePoint = {
    xFraction: start.xFraction + (end.xFraction - start.xFraction) * 0.66,
    yFraction: start.yFraction + (end.yFraction - start.yFraction) * 0.66,
    latitude: start.latitude + (end.latitude - start.latitude) * 0.66,
    longitude: start.longitude + (end.longitude - start.longitude) * 0.66,
  };
  return [start, mid1, mid2, end];
}

function getEffectiveDestinationStops(cfg: ConvoyRouteConfig): DestinationStop[] {
  if (cfg.destinationStops && cfg.destinationStops.length > 0) {
    return cfg.destinationStops;
  }
  const lastWp = cfg.waypoints[cfg.waypoints.length - 1];
  return [
    {
      id: 'stop-fallback-1',
      name: cfg.destinationName || 'Titik Tujuan',
      address: cfg.destinationAddress || cfg.destinationName || 'Titik Tujuan',
      latitude: lastWp?.latitude ?? 21.4225,
      longitude: lastWp?.longitude ?? 39.8262,
    },
  ];
}

function buildMultiStopPolyline(
  start: RoutePoint,
  stops: Array<{ latitude: number; longitude: number }>
): RoutePoint[] {
  if (stops.length === 0) return [start];
  if (stops.length === 1) {
    return buildPolylineToDestination(start, {
      xFraction: 0.86,
      yFraction: 0.14,
      latitude: stops[0].latitude,
      longitude: stops[0].longitude,
    });
  }
  const totalStops = stops.length;
  return [
    start,
    ...stops.map((stop, idx) => {
      const frac = (idx + 1) / totalStops;
      return {
        xFraction: 0.14 + frac * 0.72,
        yFraction: 0.86 - frac * 0.72,
        latitude: stop.latitude,
        longitude: stop.longitude,
      };
    }),
  ];
}

function estimateMultiStopMeters(
  start: { latitude: number; longitude: number },
  stops: Array<{ latitude: number; longitude: number }>
): number {
  if (stops.length === 0) return 500;
  let meters = 0;
  let prev = { lat: start.latitude, lng: start.longitude };
  for (const stop of stops) {
    const cur = { lat: stop.latitude, lng: stop.longitude };
    meters += haversineMeters(prev, cur);
    prev = cur;
  }
  return Math.max(500, Math.round(meters * 1.25));
}

function interpolateAlongPolyline(
  progress: number,
  waypoints: RoutePoint[],
  lateralOffset: number,
  speedKmh: number
): MapPosition {
  const p = Math.max(0.05, Math.min(0.95, progress));
  if (waypoints.length < 2) {
    return {
      routeProgress: p,
      xFraction: 0.5,
      yFraction: 0.5,
      latitude: 21.4225,
      longitude: 39.8262,
      speedKmh,
    };
  }
  const segCount = waypoints.length - 1;
  const scaled = p * segCount;
  const idx = Math.max(0, Math.min(segCount - 1, Math.floor(scaled)));
  const localT = scaled - idx;
  const a = waypoints[idx];
  const b = waypoints[idx + 1];

  const dx = b.xFraction - a.xFraction;
  const dy = b.yFraction - a.yFraction;
  const len = Math.max(0.001, Math.hypot(dx, dy));
  const perpX = -dy / len;
  const perpY = dx / len;

  const dLat = b.latitude - a.latitude;
  const dLng = b.longitude - a.longitude;
  const geoLen = Math.max(0.0001, Math.hypot(dLat, dLng));
  const latShift = (-dLng / geoLen) * lateralOffset * 0.03;
  const lngShift = (dLat / geoLen) * lateralOffset * 0.03;

  return {
    routeProgress: p,
    xFraction: Math.max(0.06, Math.min(0.94, a.xFraction + dx * localT + perpX * lateralOffset)),
    yFraction: Math.max(0.06, Math.min(0.94, a.yFraction + dy * localT + perpY * lateralOffset)),
    latitude: a.latitude + dLat * localT + latShift,
    longitude: a.longitude + dLng * localT + lngShift,
    speedKmh,
  };
}

function evaluateRoomSafety(
  rawMembers: ConvoyMember[],
  routeConfig: ConvoyRouteConfig,
  failoverContext?: {
    originalLeaderUid?: string;
    actingLeaderUid?: string;
    leaderTroubleActive?: boolean;
  }
): {
  members: ConvoyMember[];
  offRouteAlert: OffRouteAlertState;
  stragglerAlert: StragglerAlertState;
  coLeaderUid: string;
} {
  const enriched = rawMembers.map((member) => {
    const hasValidGeo =
      Math.abs(member.mapPosition.latitude) > 0.001 ||
      Math.abs(member.mapPosition.longitude) > 0.001;
    if (hasValidGeo && routeConfig.waypoints.length >= 2) {
      const geoEval = evaluatePositionAlongRoute(
        {
          lat: member.mapPosition.latitude,
          lng: member.mapPosition.longitude,
        },
        routeConfig.waypoints,
        routeConfig.totalRouteMeters
      );
      return {
        ...member,
        routeDistanceMeters: geoEval.distanceTraveledMeters,
        offRouteMeters: geoEval.offRouteMeters,
        mapPosition: {
          ...member.mapPosition,
          routeProgress: geoEval.routeProgress,
        },
      };
    }

    const routeDistanceMeters = Math.round(
      member.mapPosition.routeProgress * routeConfig.totalRouteMeters
    );
    return { ...member, routeDistanceMeters, offRouteMeters: 0 };
  });

  const sorted = [...enriched].sort(
    (a, b) => b.routeDistanceMeters - a.routeDistanceMeters
  );
  const rankMap = new Map(sorted.map((m, idx) => [m.id, idx + 1]));

  const originalLeader =
    (failoverContext?.originalLeaderUid
      ? enriched.find((m) => m.id === failoverContext.originalLeaderUid)
      : undefined) || enriched.find((m) => m.role === 'TOUR_LEADER');
  const origLeaderId =
    failoverContext?.originalLeaderUid || originalLeader?.id || '';
  const closestResult = computeClosestCoLeader(
    enriched.map((m) => ({
      ...m,
      convoyOrderRank: rankMap.get(m.id) ?? 1,
    })),
    origLeaderId
  );
  const coLeaderUid = closestResult.coLeaderUid ?? '';
  const troubleActive = Boolean(failoverContext?.leaderTroubleActive);
  const effectiveActingUid =
    troubleActive && failoverContext?.actingLeaderUid
      ? failoverContext.actingLeaderUid
      : troubleActive
        ? coLeaderUid
        : '';

  const hasManualSweeper = enriched.some((m) => m.isSweeper && !m.isAutoSweeper);
  let autoSweeperId = '';
  if (!hasManualSweeper && sorted.length >= 2) {
    const nonLeaderRearmost = [...sorted]
      .reverse()
      .find(
        (m) =>
          m.id !== origLeaderId &&
          m.id !== effectiveActingUid &&
          m.role !== 'TOUR_LEADER'
      );
    autoSweeperId = (nonLeaderRearmost ?? sorted[sorted.length - 1])?.id ?? '';
  }

  const rankedMembers = enriched.map((m) => {
    const isManualSweeper = Boolean(m.isSweeper && !m.isAutoSweeper);
    const isAutoSweeper = Boolean(
      !hasManualSweeper && autoSweeperId && m.id === autoSweeperId
    );
    return {
      ...m,
      convoyOrderRank: rankMap.get(m.id) ?? 1,
      distanceToLeaderMeters: closestResult.distanceByMemberId.get(m.id) ?? 0,
      isCoLeader: Boolean(coLeaderUid && m.id === coLeaderUid),
      isActingLeader: Boolean(
        troubleActive && effectiveActingUid && m.id === effectiveActingUid
      ),
      isSweeper: isManualSweeper || isAutoSweeper,
      isAutoSweeper,
    };
  });

  const sortedRanked = [...rankedMembers].sort(
    (a, b) => b.routeDistanceMeters - a.routeDistanceMeters
  );

  const self = rankedMembers.find((m) => m.isCurrentUser);
  const selfDeviation = self?.offRouteMeters ?? 0;
  const offRouteAlert: OffRouteAlertState = {
    isOffRoute: selfDeviation > routeConfig.offRouteToleranceMeters,
    deviationMeters: selfDeviation,
    toleranceMeters: routeConfig.offRouteToleranceMeters,
  };

  let stragglerAlert: StragglerAlertState = {
    isTriggered: false,
    trailingMember: null,
    aheadMember: null,
    gapMeters: 0,
    maxAllowedMeters: routeConfig.maxStragglerDistanceMeters,
  };

  if (sortedRanked.length >= 2) {
    const designatedSweeper = sortedRanked.find(
      (m) => m.isSweeper && m.id !== sortedRanked[sortedRanked.length - 1].id
    );
    const aheadMember = designatedSweeper ?? sortedRanked[sortedRanked.length - 2];
    const trailingMember = sortedRanked[sortedRanked.length - 1];
    const gapMeters = Math.max(
      0,
      aheadMember.routeDistanceMeters - trailingMember.routeDistanceMeters
    );
    stragglerAlert = {
      isTriggered: gapMeters > routeConfig.maxStragglerDistanceMeters,
      trailingMember,
      aheadMember,
      gapMeters,
      maxAllowedMeters: routeConfig.maxStragglerDistanceMeters,
    };
  }

  return { members: rankedMembers, offRouteAlert, stragglerAlert, coLeaderUid };
}

function isRoomEffectiveLeader(room: TourRoom | null | undefined, uid?: string | null): boolean {
  if (!room || !uid) return false;
  if (room.leaderTroubleActive && room.actingLeaderUid) {
    return room.actingLeaderUid === uid;
  }
  return room.leaderUid === uid;
}

function landmarkForProgress(progress: number): string {
  if (progress < 0.3) return 'Hotel Area Ajyad';
  if (progress < 0.5) return 'Jalur Menuju Bab Al-Malik';
  if (progress < 0.7) return 'Pelataran Selatan Haram';
  return 'Gerbang Masjidil Haram';
}

function generateUniqueRoomId(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  let suffix = '';
  for (let i = 0; i < 4; i++) {
    suffix += chars[Math.floor(Math.random() * chars.length)];
  }
  return `UMR-${suffix}`;
}

function extractRoomId(raw: string): string {
  const trimmed = raw.trim();
  if (!trimmed) return '';
  const queryMatch = trimmed.match(/[?&]room=([A-Za-z0-9_-]{4,32})/i);
  if (queryMatch?.[1]) {
    return queryMatch[1].toUpperCase();
  }
  const joinPathMatch = trimmed.match(/\/join\/([A-Za-z0-9_-]{4,32})/i);
  if (joinPathMatch?.[1]) {
    return joinPathMatch[1].toUpperCase();
  }
  const withoutQuery = trimmed.split('?')[0].replace(/\/+$/, '');
  const segments = withoutQuery.split('/');
  const candidate = (segments[segments.length - 1] || trimmed).trim().toUpperCase();
  return /^[A-Za-z0-9_-]{4,32}$/.test(candidate) ? candidate : '';
}

const DEFAULT_TRIP_HISTORY: TripHistoryRecord[] = [
  {
    id: 'trip-seed-1',
    roomId: 'UMR-7391',
    tourTitle: 'Safar Subuh Masjidil Haram',
    dateLabel: '08 Nov 2026',
    timeRangeLabel: '04:15 - 06:30',
    durationLabel: '2j 15m',
    durationMinutes: 135,
    distanceKm: 4.8,
    avgSpeedKmh: 18,
    maxSpeedKmh: 36,
    userRole: 'TOUR_LEADER',
    destinationName: "Masjidil Haram (Ka'bah)",
    destinationAddress: 'Al Haram, Makkah 24231 · 21.4225, 39.8262',
    waypoints: downsampleRoutePoints(
      [
        { lat: 21.4152, lng: 39.8286 },
        { lat: 21.4161, lng: 39.8282 },
        { lat: 21.417, lng: 39.8278 },
        { lat: 21.418, lng: 39.8275 },
        { lat: 21.4189, lng: 39.8273 },
        { lat: 21.4198, lng: 39.827 },
        { lat: 21.4207, lng: 39.8267 },
        { lat: 21.4216, lng: 39.8264 },
        { lat: 21.4225, lng: 39.8262 },
      ],
      60
    ),
    visitedCheckpoints: [
      {
        id: 'cp-s1',
        name: 'Pos Kumpul Gerbang Ajyad',
        arrivedAtLabel: '04:40',
        xFraction: 0.36,
        yFraction: 0.62,
        latitude: 21.418,
        longitude: 39.8275,
      },
      {
        id: 'cp-s2',
        name: 'Pelataran Bab Al-Malik Abdul Aziz',
        arrivedAtLabel: '05:15',
        xFraction: 0.66,
        yFraction: 0.36,
        latitude: 21.4207,
        longitude: 39.8267,
      },
    ],
    participants: [
      {
        id: 'p1',
        fullName: 'Anda (Mutawwif)',
        vehicleType: 'MOBIL',
        vehicleDetail: 'Bus Rombongan 01',
        licensePlate: 'JKT-01 / KMR 402',
        role: 'TOUR_LEADER',
        avatarPreset: 'RIDER_CAPTAIN',
      },
      {
        id: 'p2',
        fullName: 'Hj. Siti Aminah',
        vehicleType: 'MOTOR',
        vehicleDetail: 'Jalan Kaki · Sektor A',
        licensePlate: 'JKT-01 / KMR 405',
        role: 'MEMBER',
        isSweeper: true,
        avatarPreset: 'HELMET_TOURING',
      },
      {
        id: 'p3',
        fullName: 'Bpk. H. Rahmat',
        vehicleType: 'MOTOR',
        vehicleDetail: 'Jalan Kaki · Sektor B',
        licensePlate: 'JKT-01 / KMR 410',
        role: 'MEMBER',
        avatarPreset: 'HELMET_SPORT',
      },
      {
        id: 'p4',
        fullName: 'Ibu Nadia Kusuma',
        vehicleType: 'MOBIL',
        vehicleDetail: 'Bus Rombongan 02',
        licensePlate: 'JKT-02 / KMR 312',
        role: 'MEMBER',
        avatarPreset: 'PILOT_CLASSIC',
      },
    ],
  },
  {
    id: 'trip-seed-2',
    roomId: 'UMR-4120',
    tourTitle: 'Ziarah Kota Makkah & Arafah',
    dateLabel: '01 Nov 2026',
    timeRangeLabel: '07:00 - 10:20',
    durationLabel: '3j 20m',
    durationMinutes: 200,
    distanceKm: 24.6,
    avgSpeedKmh: 44,
    maxSpeedKmh: 72,
    userRole: 'MEMBER',
    destinationName: 'Jabal Rahmah · Arafah',
    destinationAddress: 'Arafah, Makkah · 21.3549, 39.9841',
    waypoints: downsampleRoutePoints(
      [
        { lat: 21.418, lng: 39.8275 },
        { lat: 21.414, lng: 39.852 },
        { lat: 21.411, lng: 39.882 },
        { lat: 21.402, lng: 39.914 },
        { lat: 21.388, lng: 39.941 },
        { lat: 21.372, lng: 39.964 },
        { lat: 21.3549, lng: 39.9841 },
      ],
      60
    ),
    visitedCheckpoints: [
      {
        id: 'cp-s3',
        name: "Masjid Tan'im (Miqat)",
        arrivedAtLabel: '08:05',
        xFraction: 0.38,
        yFraction: 0.68,
        latitude: 21.411,
        longitude: 39.882,
      },
      {
        id: 'cp-s4',
        name: 'Pos Kumpul Mina & Muzdalifah',
        arrivedAtLabel: '09:10',
        xFraction: 0.52,
        yFraction: 0.44,
        latitude: 21.388,
        longitude: 39.941,
      },
    ],
    participants: [
      {
        id: 'p5',
        fullName: 'Ust. H. Ahmad Fauzi',
        vehicleType: 'MOBIL',
        vehicleDetail: 'Bus Rombongan 01',
        licensePlate: 'JKT-01 / KMR 402',
        role: 'TOUR_LEADER',
        avatarPreset: 'RALLY_PRO',
      },
      {
        id: 'p6',
        fullName: 'Anda',
        vehicleType: 'MOTOR',
        vehicleDetail: 'Jalan Kaki · Sektor A',
        licensePlate: 'JKT-01 / KMR 408',
        role: 'MEMBER',
        avatarPreset: 'HELMET_TOURING',
      },
      {
        id: 'p7',
        fullName: 'Hj. Siti Aminah',
        vehicleType: 'MOTOR',
        vehicleDetail: 'Jalan Kaki · Sektor B',
        licensePlate: 'JKT-01 / KMR 405',
        role: 'MEMBER',
        isSweeper: true,
        avatarPreset: 'HELMET_SPORT',
      },
    ],
  },
];

const ROUTE_STEPS: Array<{ route: ScreenRoute; label: string }> = [
  { route: 'login', label: '0. Masuk' },
  { route: 'role_selection', label: '1. Beranda' },
  { route: 'manasik_guide', label: 'Manasik' },
  { route: 'tour_leader', label: 'Mutawwif' },
  { route: 'join_tour', label: 'Gabung' },
  { route: 'live_map', label: 'Peta Live' },
  { route: 'ai_assistant', label: 'Tanya AI' },
  { route: 'trip_history', label: 'Riwayat' },
  { route: 'customize_profile', label: 'Profil' },
  { route: 'vehicle_profile', label: 'Identitas' },
];

export default function App() {
  const [darkMode, setDarkMode] = useState(false);
  const [mapsQuotaExceeded, setMapsQuotaExceeded] = useState(false);
  const [currentRoute, setCurrentRoute] = useState<ScreenRoute>('login');
  const [aboutReturnRoute, setAboutReturnRoute] =
    useState<ScreenRoute>('login');
  const [customizeReturnRoute, setCustomizeReturnRoute] =
    useState<ScreenRoute>('role_selection');
  const [assistantReturnRoute, setAssistantReturnRoute] =
    useState<ScreenRoute>('role_selection');
  const [assistantInitialPrompt, setAssistantInitialPrompt] = useState<
    string | null
  >(null);
  const [navDirection, setNavDirection] = useState<1 | -1>(1);
  const [selectedRole, setSelectedRole] = useState<UserRole>('TOUR_LEADER');
  const [vehicleProfile, setVehicleProfile] = useState<VehicleProfile>(INITIAL_PROFILE);

  // Auth & Cloud Room Sync state
  const [authUser, setAuthUser] = useState<User | null>(null);
  const [authProvider, setAuthProvider] = useState<AuthProviderId>('local');
  const [localUserName, setLocalUserName] = useState<string | null>(null);
  const [localUserEmail, setLocalUserEmail] = useState<string>('');
  const [authError, setAuthError] = useState<string | null>(null);
  const [authInfoMessage, setAuthInfoMessage] = useState<string | null>(null);
  const [isSigningIn, setIsSigningIn] = useState(false);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileSyncMessage, setProfileSyncMessage] = useState<string | null>(null);
  const [isCreatingRoom, setIsCreatingRoom] = useState(false);
  const [createRoomError, setCreateRoomError] = useState<string | null>(null);
  const [isJoiningRoom, setIsJoiningRoom] = useState(false);
  const [pendingInviteRoomId, setPendingInviteRoomId] = useState<string | null>(() => {
    if (typeof window === 'undefined') return null;
    const params = new URLSearchParams(window.location.search);
    if (params.get('mobile_auth') === 'google') return null;
    const qRoom = params.get('room');
    if (qRoom) {
      const code = extractRoomId(qRoom);
      if (code.length >= 4) return code;
    }
    const pathMatch = window.location.pathname.match(/\/join\/([A-Za-z0-9_-]{4,32})/i);
    return pathMatch?.[1] ? pathMatch[1].toUpperCase() : null;
  });
  const [isMobileGoogleBridge] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    const params = new URLSearchParams(window.location.search);
    return (
      params.get('mobile_auth') === 'google' ||
      window.location.pathname === '/mobile-auth' ||
      window.location.pathname === '/mobile-auth/' ||
      window.location.pathname === '/auth/google' ||
      window.location.pathname === '/auth/google/'
    );
  });
  const [mobileBridgeBusy, setMobileBridgeBusy] = useState(false);
  const [mobileBridgeError, setMobileBridgeError] = useState<string | null>(null);
  const [mobileBridgeDeepLink, setMobileBridgeDeepLink] = useState<string | null>(null);
  const [mobileBridgeEmail, setMobileBridgeEmail] = useState<string>('');
  const [mobileBridgeCopied, setMobileBridgeCopied] = useState(false);
  const [globalConfig, setGlobalConfig] = useState<GlobalAppConfig>(
    DEFAULT_GLOBAL_APP_CONFIG
  );
  const [isRegisteredTourLeader, setIsRegisteredTourLeader] = useState(false);

  const [activeRoom, setActiveRoom] = useState<TourRoom | null>(null);
  const [joinInput, setJoinInput] = useState(() => pendingInviteRoomId || '');
  const [joinError, setJoinError] = useState<string | null>(null);
  const [selectedMemberId, setSelectedMemberId] = useState<string | null>(null);
  const [isSimulationRunning, setIsSimulationRunning] = useState(true);
  const [routeConfig, setRouteConfig] = useState<ConvoyRouteConfig>(INITIAL_ROUTE_CONFIG);
  const [offRouteAlert, setOffRouteAlert] = useState<OffRouteAlertState>({
    isOffRoute: false,
    deviationMeters: 0,
    toleranceMeters: 75,
  });
  const [stragglerAlert, setStragglerAlert] = useState<StragglerAlertState>({
    isTriggered: false,
    trailingMember: null,
    aheadMember: null,
    gapMeters: 0,
    maxAllowedMeters: 1000,
  });
  const [sosState, setSosState] = useState<SosEmergencyState>({
    isActive: false,
    senderId: '',
    senderName: '',
    senderVehicle: '',
    latitude: 0,
    longitude: 0,
    locationText: '',
    triggeredAtLabel: '',
  });
  const [messages, setMessages] = useState<RoomMessage[]>([]);
  const [isRecordingVoice, setIsRecordingVoice] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [playingMessageId, setPlayingMessageId] = useState<string | null>(null);
  const [voiceStatusMessage, setVoiceStatusMessage] = useState<string | null>(null);

  const [isPickingDestinationOnMap, setIsPickingDestinationOnMap] = useState(false);
  const [isPickingCheckpointOnMap, setIsPickingCheckpointOnMap] = useState(false);
  const [pendingCheckpointName, setPendingCheckpointName] = useState('');
  const [isOffRouteSimActive, setIsOffRouteSimActive] = useState(false);
  const [isStragglerSimActive, setIsStragglerSimActive] = useState(false);
  const [isLeaderTroubleSimActive, setIsLeaderTroubleSimActive] = useState(false);
  const [isMissingPilgrimSimActive, setIsMissingPilgrimSimActive] = useState(false);
  const [pendingFamilyInviteCode, setPendingFamilyInviteCode] = useState<string>(() => {
    if (typeof window === 'undefined') return '';
    const params = new URLSearchParams(window.location.search);
    const qFamily = params.get('family');
    if (qFamily) {
      return extractFamilyInviteCode(qFamily) || '';
    }
    const pathMatch = window.location.pathname.match(/\/family\/(FAM-[A-Za-z0-9]+)/i);
    return pathMatch?.[1] ? extractFamilyInviteCode(pathMatch[1]) || '' : '';
  });
  const [familyGrant, setFamilyGrant] = useState<FamilyGrantRecord | null>(null);
  const [familyMonitorSnapshot, setFamilyMonitorSnapshot] =
    useState<FamilyMonitorSnapshot | null>(null);
  const [familyMonitorError, setFamilyMonitorError] = useState<string | null>(null);
  const [myFamilyLink, setMyFamilyLink] = useState<FamilyLinkRecord | null>(null);
  const [isGeneratingFamilyLink, setIsGeneratingFamilyLink] = useState(false);
  const [familyLinkError, setFamilyLinkError] = useState<string | null>(null);
  const [nowTickMs, setNowTickMs] = useState<number>(() => Date.now());
  const [simIndex, setSimIndex] = useState(0);
  const [tripHistory, setTripHistory] = useState<TripHistoryRecord[]>(DEFAULT_TRIP_HISTORY);
  const [selectedTripId, setSelectedTripId] = useState<string | null>(
    DEFAULT_TRIP_HISTORY[0]?.id ?? null
  );
  const [lastCompletedTrip, setLastCompletedTrip] = useState<TripHistoryRecord | null>(null);
  const [autoFinishNotice, setAutoFinishNotice] = useState<string | null>(null);
  const [manasikProgress, setManasikProgress] = useState<ManasikProgressState>(() =>
    loadManasikProgressLocal('default')
  );
  const [manasikInitialTab, setManasikInitialTab] = useState<'manasik' | 'doa' | 'activities'>('manasik');
  const [draftRoomActivities, setDraftRoomActivities] = useState<RoomActivityItem[]>(() =>
    createStarterRoomUmrahActivities('leader')
  );
  const [roomActivities, setRoomActivities] = useState<RoomActivityItem[]>([]);
  const [roomActivityProgress, setRoomActivityProgress] = useState<
    Record<string, RoomActivityMemberProgress>
  >({});
  const [syncWithPreview, setSyncWithPreview] = useState(true);
  const [mobileWorkspaceTab, setMobileWorkspaceTab] = useState<'preview' | 'code'>('preview');
  const [batterySaverMode, setBatterySaverMode] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false;
    try {
      return window.localStorage.getItem('safar_battery_saver_v1') === '1';
    } catch {
      return false;
    }
  });
  const [isOnline, setIsOnline] = useState<boolean>(() =>
    typeof navigator !== 'undefined' ? navigator.onLine : true
  );
  const [offlineQueueCount, setOfflineQueueCount] = useState<number>(() =>
    getOfflineQueueCount()
  );
  const [dismissedBroadcastId, setDismissedBroadcastId] = useState<string>('');

  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const audioChunksRef = useRef<BlobPart[]>([]);
  const recordedMimeTypeRef = useRef<string>('audio/webm');
  const voiceStartMsRef = useRef<number>(0);
  const audioPlayerRef = useRef<HTMLAudioElement | null>(null);
  const playbackTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const voiceErrorTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const lastTelemetrySentMsRef = useRef<number>(0);
  const lastTelemetryFixRef = useRef<LatLngPoint | null>(null);
  const lastSyncedRouteSigRef = useRef<string>('');
  const lastCoLeaderSyncedRef = useRef<string>('');
  const lastAlertedBroadcastIdRef = useRef<string>('');
  const lastAlertedMissingIdsRef = useRef<string>('');
  const pendingFamilyRedemptionRef = useRef<{ code: string; name?: string } | null>(null);
  const recordedTrackRef = useRef<LatLngPoint[]>([]);
  const batterySaverModeRef = useRef<boolean>(batterySaverMode);
  batterySaverModeRef.current = batterySaverMode;
  const routeConfigRef = useRef<ConvoyRouteConfig>(routeConfig);
  routeConfigRef.current = routeConfig;
  const activeRoomRef = useRef<TourRoom | null>(activeRoom);
  activeRoomRef.current = activeRoom;
  const authUserRef = useRef<User | null>(authUser);
  authUserRef.current = authUser;
  const handleFinishAndSaveTripRef = useRef<(opts?: { autoEndedByLeader?: boolean }) => void>(() => {});

  useEffect(() => {
    document.documentElement.classList.toggle('dark', darkMode);
  }, [darkMode]);

  // Listen for Google Maps Platform quota-exceeded events
  useEffect(() => {
    const handleQuota = () => setMapsQuotaExceeded(true);
    window.addEventListener('gmp-quota-exceeded', handleQuota);
    return () => window.removeEventListener('gmp-quota-exceeded', handleQuota);
  }, []);

  // Restore local/Instagram session on startup if no Firebase user active
  useEffect(() => {
    const savedSession = loadLocalSession();
    if (savedSession) {
      setLocalUserName(savedSession.fullName);
      setLocalUserEmail(savedSession.email);
      setAuthProvider(savedSession.authProvider);
      const emailProfile = loadProfileByEmail(savedSession.email);
      if (emailProfile) {
        setVehicleProfile((prev) => ({
          ...prev,
          ...emailProfile,
          fullName: emailProfile.fullName || savedSession.fullName,
          email: emailProfile.email || savedSession.email,
          instagramHandle:
            emailProfile.instagramHandle || savedSession.instagramHandle || '',
          authProvider: savedSession.authProvider,
        }));
      } else {
        setVehicleProfile((prev) => ({
          ...prev,
          fullName: prev.fullName || savedSession.fullName,
          email: savedSession.email,
          instagramHandle: savedSession.instagramHandle || '',
          authProvider: savedSession.authProvider,
        }));
      }
      setManasikProgress(
        loadManasikProgressLocal(savedSession.email || savedSession.fullName || 'default')
      );
      setCurrentRoute((prev) => (prev === 'login' ? 'role_selection' : prev));
    }
  }, []);

  // Listen for Instagram 3P OAuth popup callback messages
  useEffect(() => {
    const handleOAuthMessage = (event: MessageEvent) => {
      const origin = event.origin || '';
      if (
        !origin.endsWith('.run.app') &&
        !origin.includes('localhost') &&
        origin !== window.location.origin
      ) {
        return;
      }

      if (event.data?.type === 'OAUTH_AUTH_ERROR') {
        setIsSigningIn(false);
        setAuthError(
          typeof event.data.error === 'string'
            ? event.data.error
            : 'Autentikasi Instagram dibatalkan atau gagal.'
        );
        return;
      }

      if (
        event.data?.type === 'OAUTH_AUTH_SUCCESS' &&
        event.data?.provider === 'instagram'
      ) {
        const igProfile = event.data.profile as
          | {
              id?: string;
              username?: string;
              fullName?: string;
              avatarPhotoUrl?: string;
            }
          | undefined;
        const igUsername = (igProfile?.username || '').replace(/^@/, '');
        const igEmail = igUsername
          ? `${igUsername.toLowerCase()}@instagram.safartracking.id`
          : '';
        const resolvedName =
          igProfile?.fullName?.trim() ||
          (igUsername ? `@${igUsername}` : 'Jamaah Instagram');
        const restoredByEmail = loadProfileByEmail(igEmail);

        setIsSigningIn(false);
        setAuthError(null);
        setAuthInfoMessage(
          `Akun Instagram @${igUsername || 'jamaah'} berhasil terhubung!`
        );
        setAuthProvider('instagram');
        setLocalUserName(resolvedName);
        setLocalUserEmail(restoredByEmail?.email || igEmail);

        const nextProfile: VehicleProfile = {
          ...(restoredByEmail || INITIAL_PROFILE),
          fullName: restoredByEmail?.fullName || resolvedName,
          email: restoredByEmail?.email || igEmail,
          authProvider: 'instagram',
          instagramHandle: igUsername || restoredByEmail?.instagramHandle || '',
          avatarPhotoUrl:
            restoredByEmail?.avatarPhotoUrl ||
            (igProfile?.avatarPhotoUrl ? igProfile.avatarPhotoUrl : undefined),
        };
        setVehicleProfile(nextProfile);
        saveProfileByEmail(nextProfile.email, nextProfile);
        saveLocalSession({
          fullName: nextProfile.fullName,
          email: nextProfile.email || '',
          authProvider: 'instagram',
          instagramHandle: igUsername,
        });
        setNavDirection(1);
        setCurrentRoute('customize_profile');
      }
    };

    window.addEventListener('message', handleOAuthMessage);
    return () => window.removeEventListener('message', handleOAuthMessage);
  }, []);

  // Test Firestore connection + Subscribe to Firebase Auth
  useEffect(() => {
    testFirestoreConnection();
    const unsubAuth = subscribeToAuthState(async (user) => {
      setAuthUser(user);
      if (user) {
        if (isDeviceCloudGuestEmail(user.email)) {
          return;
        }
        setAuthError(null);
        const rawProviderId = user.providerData?.[0]?.providerId || '';
        const detectedProvider: AuthProviderId = rawProviderId.includes('facebook')
          ? 'facebook'
          : rawProviderId.includes('password')
            ? 'email'
            : 'google';
        setAuthProvider(detectedProvider);

        const normalizedUserEmail = normalizeProfileEmail(user.email);
        if (normalizedUserEmail) {
          setLocalUserEmail(normalizedUserEmail);
        }

        // Check if this user arrived via a pending Family Monitoring link OR has an active Family Grant
        const pendingFamily = pendingFamilyRedemptionRef.current;
        if (pendingFamily?.code) {
          pendingFamilyRedemptionRef.current = null;
          try {
            const grant = await redeemFamilyInviteLink(
              pendingFamily.code,
              user.uid,
              pendingFamily.name || user.displayName || 'Keluarga Jamaah'
            );
            setFamilyGrant(grant);
            setPendingFamilyInviteCode('');
            setNavDirection(1);
            setCurrentRoute('family_monitor');
            return;
          } catch (famErr) {
            setFamilyGrant(null);
            setAuthError(
              famErr instanceof Error
                ? famErr.message
                : 'Gagal mengaktifkan tautan pantauan keluarga.'
            );
            setCurrentRoute('login');
            return;
          }
        }

        const existingFamilyGrant = await loadActiveFamilyGrant(user.uid);
        if (existingFamilyGrant) {
          setFamilyGrant(existingFamilyGrant);
          setCurrentRoute('family_monitor');
          return;
        }

        setFamilyGrant(null);
        setCurrentRoute((prev) => (prev === 'login' ? 'role_selection' : prev));
        try {
          const savedCloud = await loadUserPrivateProfile(user.uid);
          const savedLocalByEmail = loadProfileByEmail(
            savedCloud?.email || normalizedUserEmail
          );

          const merged: VehicleProfile = {
            ...INITIAL_PROFILE,
            ...(savedLocalByEmail || {}),
            ...(savedCloud || {}),
            fullName:
              savedCloud?.fullName ||
              savedLocalByEmail?.fullName ||
              user.displayName ||
              'Jamaah Safar Tracking',
            email:
              normalizeProfileEmail(savedCloud?.email) ||
              normalizeProfileEmail(savedLocalByEmail?.email) ||
              normalizedUserEmail,
            authProvider:
              savedCloud?.authProvider && savedCloud.authProvider !== 'local'
                ? savedCloud.authProvider
                : detectedProvider,
          };

          setVehicleProfile(merged);
          if (merged.email) {
            saveProfileByEmail(merged.email, merged);
          }

          if (!savedCloud) {
            saveUserPrivateProfile(user.uid, merged).catch(() => {});
          }

          const cloudTrips = await loadUserTripsFromFirestore(user.uid);
          if (cloudTrips.length > 0) {
            setTripHistory((prev) => {
              const cloudIds = new Set(cloudTrips.map((t) => t.id));
              return [...cloudTrips, ...prev.filter((t) => !cloudIds.has(t.id))];
            });
          }

          const localForUser = loadManasikProgressLocal(
            user.uid || normalizedUserEmail || 'default'
          );
          const cloudManasik = await loadManasikProgressFromFirestore(user.uid);
          const mergedManasik: ManasikProgressState = {
            umrah: Array.from(
              new Set([...cloudManasik.umrah, ...localForUser.umrah])
            ),
            haji: Array.from(
              new Set([...cloudManasik.haji, ...localForUser.haji])
            ),
            checkIns: {
              ...(localForUser.checkIns || {}),
              ...(cloudManasik.checkIns || {}),
            },
          };
          setManasikProgress(mergedManasik);
          saveManasikProgressLocal(user.uid, mergedManasik);
        } catch (err) {
          console.warn('Gagal memuat profil cloud:', err);
        }
      } else {
        setFamilyGrant(null);
        setFamilyMonitorSnapshot(null);
        setMyFamilyLink(null);
      }
    });
    return () => unsubAuth();
  }, []);

  // Recover Firebase Google redirect auth result (browser popup fallback & Android bridge)
  useEffect(() => {
    let cancelled = false;
    consumeGoogleRedirectResult()
      .then((res) => {
        if (cancelled || !res) return;
        if (res.mode === 'mobile_bridge' && res.user) {
          if (res.idToken) {
            const deepLink = `safartracking://auth?id_token=${encodeURIComponent(res.idToken)}`;
            setMobileBridgeEmail(res.user.email || '');
            setMobileBridgeDeepLink(deepLink);
            setMobileBridgeError(null);
          } else {
            setMobileBridgeError(
              'Sesi Google tersimpan, silakan ketuk "Masuk dengan Akun Google" sekali lagi untuk mengirim token ke aplikasi Android.'
            );
          }
          return;
        }
        if (res.user) {
          setLocalUserName(null);
          saveLocalSession(null);
          setAuthError(null);
          setAuthInfoMessage(
            `Berhasil masuk dengan Google (${res.user.email || res.user.displayName || 'terverifikasi'}).`
          );
          setIsSigningIn(false);
          setNavDirection(1);
          setCurrentRoute((prev) => (prev === 'login' ? 'role_selection' : prev));
        }
      })
      .catch((err) => {
        if (cancelled) return;
        const msg = mapFirebaseAuthError(err, 'Google');
        if (isMobileGoogleBridge) {
          setMobileBridgeError(msg);
        } else {
          setAuthError(msg);
        }
        setIsSigningIn(false);
      });
    return () => {
      cancelled = true;
    };
  }, [isMobileGoogleBridge]);

  // Auto-open Join Tour page when user arrives from ?room=KNV-XXXX or /join/KNV-XXXX
  useEffect(() => {
    if (!pendingInviteRoomId || isMobileGoogleBridge || activeRoom) return;
    if (!authUser && !localUserName) return;
    setJoinInput(pendingInviteRoomId);
    setSelectedRole('MEMBER');
    setPendingInviteRoomId(null);
    setNavDirection(1);
    setCurrentRoute('join_tour');
    if (typeof window !== 'undefined') {
      const url = new URL(window.location.href);
      url.searchParams.delete('room');
      if (url.pathname.startsWith('/join/')) {
        url.pathname = '/';
      }
      window.history.replaceState({}, '', url.toString());
    }
  }, [pendingInviteRoomId, isMobileGoogleBridge, activeRoom, authUser, localUserName]);

  // Subscribe to Global App Config (/app_config/global)
  useEffect(() => {
    const unsubConfig = subscribeToGlobalAppConfig((cfg) => {
      setGlobalConfig(cfg);
      if (!activeRoomRef.current) {
        setRouteConfig((prev) => ({
          ...prev,
          offRouteToleranceMeters: cfg.defaultOffRouteToleranceMeters,
          maxStragglerDistanceMeters: cfg.defaultMaxStragglerDistanceMeters,
          soundAlertEnabled: cfg.defaultSoundAlertEnabled,
        }));
      }
    });
    return () => unsubConfig();
  }, []);

  // Subscribe to Registered Tour Leader Eligibility (/tour_leader_hashes/{sha256(email)})
  useEffect(() => {
    const cloudEmail =
      authUser && !isDeviceCloudGuestEmail(authUser.email)
        ? authUser.email
        : null;
    if (!cloudEmail) {
      setIsRegisteredTourLeader(false);
      return;
    }
    const unsub = subscribeToTourLeaderEligibility(cloudEmail, (eligible) => {
      setIsRegisteredTourLeader(eligible);
    });
    return () => unsub();
  }, [authUser]);

  useEffect(() => {
    if (!isRecordingVoice) return;
    const timer = setInterval(() => {
      setRecordingSeconds((prev) => prev + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, [isRecordingVoice]);

  const commitRoomMembers = useCallback(
    (
      rawMembers: ConvoyMember[],
      config: ConvoyRouteConfig = routeConfigRef.current,
      roomOverride?: TourRoom
    ) => {
      setActiveRoom((prev) => {
        const base = roomOverride ?? prev;
        if (!base) return base;
        const evaluated = evaluateRoomSafety(rawMembers, config, {
          originalLeaderUid: base.leaderUid,
          actingLeaderUid: base.actingLeaderUid,
          leaderTroubleActive: base.leaderTroubleActive,
        });
        setOffRouteAlert(evaluated.offRouteAlert);
        setStragglerAlert(evaluated.stragglerAlert);
        const coLeaderUid =
          base.leaderTroubleActive && base.coLeaderUid
            ? base.coLeaderUid
            : evaluated.coLeaderUid;
        return {
          ...base,
          coLeaderUid,
          members: evaluated.members,
        };
      });
    },
    []
  );

  // Real Device GPS tracking
  const handleGpsFix = useCallback(
    (fix: LiveGpsFix) => {
      const currentRoom = activeRoomRef.current;
      const currentCfg = routeConfigRef.current;
      const user = authUserRef.current;
      if (!currentRoom) return;

      // Record real GPS breadcrumb along the route actually traveled
      if (fix.accuracyMeters <= 80 || recordedTrackRef.current.length === 0) {
        const pt: LatLngPoint = { lat: fix.latitude, lng: fix.longitude };
        const track = recordedTrackRef.current;
        const lastPt = track[track.length - 1];
        if (!lastPt || haversineMeters(lastPt, pt) >= 15) {
          track.push(pt);
          if (track.length > 1800) {
            recordedTrackRef.current = downsampleTrackPoints(track, 20, 300);
          }
        }
      }

      const geoEval = evaluatePositionAlongRoute(
        { lat: fix.latitude, lng: fix.longitude },
        currentCfg.waypoints,
        currentCfg.totalRouteMeters
      );

      // Update local self marker immediately for responsive UI
      const updatedMembers = currentRoom.members.map((m) =>
        m.isCurrentUser
          ? {
              ...m,
              connectionStatus: 'CONNECTED' as ConnectionStatus,
              lastLocationText: `GPS (${fix.latitude.toFixed(4)}, ${fix.longitude.toFixed(4)})`,
              lastUpdatedLabel: 'GPS Live',
              mapPosition: {
                ...m.mapPosition,
                latitude: fix.latitude,
                longitude: fix.longitude,
                speedKmh: fix.speedKmh,
                heading: fix.heading,
                accuracyMeters: fix.accuracyMeters,
                routeProgress: geoEval.routeProgress,
              },
            }
          : m
      );
      commitRoomMembers(updatedMembers, currentCfg);

      // Publish throttled telemetry to Firestore when in a synced room (adaptive for battery saver + offline queue)
      if (currentRoom.isSyncedRoom && user) {
        const now = Date.now();
        const prevPt = lastTelemetryFixRef.current;
        const movedMeters = prevPt
          ? haversineMeters(prevPt, { lat: fix.latitude, lng: fix.longitude })
          : 999;
        const saverActive = batterySaverModeRef.current;
        const intervalThresholdMs = saverActive
          ? fix.speedKmh < 3
            ? 20_000
            : 10_000
          : 2_200;
        const distanceThresholdMeters = saverActive ? 25 : 15;

        if (
          now - lastTelemetrySentMsRef.current >= intervalThresholdMs ||
          movedMeters >= distanceThresholdMeters
        ) {
          lastTelemetrySentMsRef.current = now;
          lastTelemetryFixRef.current = { lat: fix.latitude, lng: fix.longitude };
          const telemetryPatch = {
            latitude: fix.latitude,
            longitude: fix.longitude,
            speedKmh: fix.speedKmh,
            heading: fix.heading,
            accuracyMeters: fix.accuracyMeters,
            routeProgress: geoEval.routeProgress,
            xFraction: Math.max(0.08, Math.min(0.92, geoEval.routeProgress)),
            yFraction: Math.max(0.08, Math.min(0.92, 1 - geoEval.routeProgress)),
            lastLocationText: `GPS Live (±${fix.accuracyMeters}m)`,
          };

          if (typeof navigator !== 'undefined' && navigator.onLine === false) {
            const count = enqueueTelemetryUpdate({
              roomId: currentRoom.roomId,
              uid: user.uid,
              patch: telemetryPatch,
            });
            setOfflineQueueCount(count);
          } else {
            publishMemberTelemetry(currentRoom.roomId, user.uid, telemetryPatch)
              .then(() => {
                if (getOfflineQueueCount() > 0) {
                  flushOfflineSyncQueue()
                    .then(({ remaining }) => setOfflineQueueCount(remaining))
                    .catch(() => {});
                }
              })
              .catch(() => {
                const count = enqueueTelemetryUpdate({
                  roomId: currentRoom.roomId,
                  uid: user.uid,
                  patch: telemetryPatch,
                });
                setOfflineQueueCount(count);
              });
          }
        }
      }
    },
    [commitRoomMembers]
  );

  const gps = useDeviceGps({
    enabled:
      Boolean(activeRoom) ||
      currentRoute === 'live_map' ||
      currentRoute === 'tour_leader' ||
      currentRoute === 'join_tour',
    batterySaver: batterySaverMode,
    onFix: handleGpsFix,
  });

  const handleToggleBatterySaverMode = useCallback(() => {
    setBatterySaverMode((prev) => {
      const next = !prev;
      try {
        window.localStorage.setItem('safar_battery_saver_v1', next ? '1' : '0');
      } catch {
        // Ignore storage error
      }
      return next;
    });
  }, []);

  const handleManualFlushOfflineQueue = useCallback(async () => {
    const res = await flushOfflineSyncQueue();
    setOfflineQueueCount(res.remaining);
  }, []);

  useEffect(() => {
    const onOnline = () => {
      setIsOnline(true);
      flushOfflineSyncQueue()
        .then(({ remaining }) => setOfflineQueueCount(remaining))
        .catch(() => {});
    };
    const onOffline = () => {
      setIsOnline(false);
    };
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);
    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, []);

  useEffect(() => {
    if (offlineQueueCount <= 0 || !isOnline) return;
    const timer = setInterval(() => {
      flushOfflineSyncQueue()
        .then(({ remaining }) => setOfflineQueueCount(remaining))
        .catch(() => {});
    }, 8000);
    return () => clearInterval(timer);
  }, [offlineQueueCount, isOnline]);

  // Publish live online/traveling presence heartbeat for Admin Backend monitoring
  useEffect(() => {
    if (!authUser) return;
    const sendHeartbeat = () => {
      const selfMember = activeRoom?.members.find((m) => m.isCurrentUser);
      const speed = gps.lastFix?.speedKmh ?? selfMember?.mapPosition.speedKmh ?? 0;
      publishUserPresence(authUser.uid, {
        fullName:
          vehicleProfile.fullName.trim() ||
          authUser.displayName ||
          'Jamaah Safar Tracking',
        email:
          (vehicleProfile.email ?? '').trim() ||
          authUser.email ||
          localUserEmail ||
          '',
        isTraveling: Boolean(activeRoom),
        activeRoomId: activeRoom?.roomId || '',
        speedKmh: speed,
      }).catch(() => {});
    };

    sendHeartbeat();
    const timer = setInterval(sendHeartbeat, 30_000);
    return () => clearInterval(timer);
  }, [
    authUser?.uid,
    authUser?.displayName,
    authUser?.email,
    activeRoom?.roomId,
    vehicleProfile.fullName,
    vehicleProfile.email,
    localUserEmail,
    gps.lastFix?.speedKmh,
  ]);

  // Real-time Firestore room subscription when activeRoom.isSyncedRoom === true
  useEffect(() => {
    if (!activeRoom?.isSyncedRoom || !activeRoom.roomId || !authUser) return;

    let latestMembers: ConvoyMember[] = [];
    let latestCheckpoints: RestCheckpoint[] = routeConfigRef.current.checkpoints;
    lastCoLeaderSyncedRef.current = '';

    const unsub = subscribeToSyncedRoom(activeRoom.roomId, {
      onRoomUpdate: (snapshot) => {
        if (!snapshot) return;
        if (snapshot.room.status === 'FINISHED') {
          if (activeRoomRef.current?.roomId === snapshot.room.roomId) {
            handleFinishAndSaveTripRef.current({ autoEndedByLeader: true });
          }
          return;
        }
        const mergedConfig: ConvoyRouteConfig = {
          ...snapshot.routeConfig,
          checkpoints: latestCheckpoints,
        };
        const stopsSig = (mergedConfig.destinationStops || [])
          .map((s) => `${s.latitude.toFixed(4)},${s.longitude.toFixed(4)}`)
          .join(';');
        const sig = `${mergedConfig.originName ?? ''}|${mergedConfig.destinationName}|${stopsSig}|${mergedConfig.routePreference ?? 'fastest'}|${Math.round(mergedConfig.totalRouteMeters / 50)}|${mergedConfig.waypoints.length}`;
        lastSyncedRouteSigRef.current = sig;
        setRouteConfig(mergedConfig);
        setSosState(snapshot.sosState);
        setActiveRoom((prev) => {
          const base: TourRoom = {
            roomId: snapshot.room.roomId,
            roomTitle: snapshot.room.roomTitle,
            inviteLink: snapshot.room.inviteLink,
            createdAtLabel: snapshot.room.createdAtLabel,
            isSyncedRoom: true,
            leaderUid: snapshot.room.leaderUid,
            coLeaderUid: snapshot.room.coLeaderUid,
            actingLeaderUid: snapshot.room.actingLeaderUid,
            leaderTroubleActive: snapshot.room.leaderTroubleActive,
            leaderTroubleReason: snapshot.room.leaderTroubleReason,
            priorityBroadcast: snapshot.room.priorityBroadcast,
            members: prev?.members ?? latestMembers,
          };
          const evaluated = evaluateRoomSafety(base.members, mergedConfig, {
            originalLeaderUid: base.leaderUid,
            actingLeaderUid: base.actingLeaderUid,
            leaderTroubleActive: base.leaderTroubleActive,
          });
          setOffRouteAlert(evaluated.offRouteAlert);
          setStragglerAlert(evaluated.stragglerAlert);
          return { ...base, members: evaluated.members };
        });
        if (snapshot.room.leaderUid === authUser.uid) {
          setSelectedRole('TOUR_LEADER');
        }
      },
      onMembersUpdate: (members) => {
        latestMembers = members;
        const cfg = routeConfigRef.current;
        const curRoom = activeRoomRef.current;
        const evaluated = evaluateRoomSafety(members, cfg, {
          originalLeaderUid: curRoom?.leaderUid,
          actingLeaderUid: curRoom?.actingLeaderUid,
          leaderTroubleActive: curRoom?.leaderTroubleActive,
        });
        setOffRouteAlert(evaluated.offRouteAlert);
        setStragglerAlert(evaluated.stragglerAlert);
        setActiveRoom((prev) =>
          prev
            ? {
                ...prev,
                coLeaderUid: prev.coLeaderUid || evaluated.coLeaderUid,
                members: evaluated.members,
              }
            : prev
        );
        if (
          curRoom &&
          isRoomEffectiveLeader(curRoom, authUser.uid) &&
          evaluated.coLeaderUid &&
          evaluated.coLeaderUid !== curRoom.coLeaderUid &&
          evaluated.coLeaderUid !== lastCoLeaderSyncedRef.current
        ) {
          lastCoLeaderSyncedRef.current = evaluated.coLeaderUid;
          syncCoLeaderAssignment(curRoom.roomId, evaluated.coLeaderUid).catch(() => {});
        }
        const self = members.find((m) => m.isCurrentUser);
        if (self) {
          setSelectedMemberId((cur) => cur ?? self.id);
        }
      },
      onCheckpointsUpdate: (checkpoints) => {
        latestCheckpoints = checkpoints;
        setRouteConfig((prev) => {
          const next = { ...prev, checkpoints };
          if (latestMembers.length > 0) {
            const curRoom = activeRoomRef.current;
            const evaluated = evaluateRoomSafety(latestMembers, next, {
              originalLeaderUid: curRoom?.leaderUid,
              actingLeaderUid: curRoom?.actingLeaderUid,
              leaderTroubleActive: curRoom?.leaderTroubleActive,
            });
            setOffRouteAlert(evaluated.offRouteAlert);
            setStragglerAlert(evaluated.stragglerAlert);
          }
          return next;
        });
      },
      onMessagesUpdate: (nextMsgs) => {
        setMessages(nextMsgs);
      },
      onActivitiesUpdate: (activities) => {
        setRoomActivities(activities);
      },
      onActivityProgressUpdate: (progressMap) => {
        setRoomActivityProgress(progressMap);
      },
    });

    return () => {
      unsub();
    };
  }, [activeRoom?.roomId, activeRoom?.isSyncedRoom, authUser?.uid]);

  // Automatic Leader Trouble & Co-Leader Failover Watchdog for Synced Rooms
  useEffect(() => {
    if (!activeRoom?.isSyncedRoom || !activeRoom.roomId || !authUser) return;
    const interval = setInterval(() => {
      const room = activeRoomRef.current;
      const user = authUserRef.current;
      if (!room || !user || !room.isSyncedRoom || !room.leaderUid) return;

      const origLeader = room.members.find((m) => m.id === room.leaderUid);
      const nowMs = Date.now();
      const staleMs =
        origLeader?.lastUpdatedMs && origLeader.lastUpdatedMs > 0
          ? nowMs - origLeader.lastUpdatedMs
          : 0;
      const isLeaderTroubled =
        Boolean(origLeader) &&
        (origLeader?.connectionStatus === 'OFFLINE' ||
          origLeader?.conditionStatus === 'MOGOK' ||
          origLeader?.conditionStatus === 'SOS' ||
          staleMs > 18_000);

      if (
        isLeaderTroubled &&
        !room.leaderTroubleActive &&
        room.coLeaderUid &&
        (room.coLeaderUid === user.uid || staleMs > 23_000)
      ) {
        const coLeader = room.members.find((m) => m.id === room.coLeaderUid);
        const reason =
          origLeader?.connectionStatus === 'OFFLINE'
            ? 'Sinyal Mutawwif terputus'
            : origLeader?.conditionStatus === 'MOGOK'
              ? 'HP/Kondisi Mutawwif mengalami kendala'
              : 'HP Mutawwif kehilangan sinyal atau baterai habis';
        triggerCoLeaderFailover(room.roomId, room.coLeaderUid, reason)
          .then(() => {
            if (room.coLeaderUid === user.uid) {
              sendRoomMessage(room.roomId, {
                id: `failover-${Date.now()}`,
                senderUid: user.uid,
                senderName: 'Sistem Wakil',
                senderVehicle: coLeader?.vehicleDetail || '',
                senderAvatarPreset: coLeader?.avatarPreset || 'RIDER_CAPTAIN',
                type: 'SYSTEM',
                textContent: `${reason}. Wakil Pembimbing ${coLeader?.fullName || 'Terdekat'} otomatis menjadi Plt. Pembimbing & mengambil alih navigasi rute.`,
              }).catch(() => {});
            }
          })
          .catch(() => {});
      }
    }, 4000);
    return () => clearInterval(interval);
  }, [activeRoom?.roomId, activeRoom?.isSyncedRoom, authUser?.uid]);

  // Offline Demo Simulator ONLY when NOT in a real Firestore synced room
  useEffect(() => {
    if (!isSimulationRunning || !activeRoom || activeRoom.isSyncedRoom) return;
    const interval = setInterval(() => {
      setActiveRoom((prev) => {
        if (!prev || prev.isSyncedRoom) return prev;
        const ordered = [...prev.members].sort(
          (a, b) => b.mapPosition.routeProgress - a.mapPosition.routeProgress
        );
        const tailId = ordered[ordered.length - 1]?.id;
        const useRealSelfGps = gps.status === 'active' && !isOffRouteSimActive;

        const moved = prev.members.map((member, idx) => {
          if (member.isCurrentUser && useRealSelfGps) {
            return member;
          }
          const isTroubledOrigLeader =
            Boolean(prev.leaderTroubleActive) &&
            (member.id === prev.leaderUid ||
              (!prev.leaderUid && member.role === 'TOUR_LEADER'));
          const isStopped =
            isTroubledOrigLeader ||
            member.conditionStatus === 'MOGOK' ||
            member.conditionStatus === 'SOS';
          const isTail = member.id === tailId && isStragglerSimActive;
          const step = isStopped ? 0 : isTail ? 0.002 : 0.01 + (idx % 2) * 0.002;
          const rawNext = member.mapPosition.routeProgress + step;
          const nextProgress = rawNext > 0.9 ? 0.16 : rawNext;
          const offShift =
            member.isCurrentUser && isOffRouteSimActive ? 0.12 : 0;
          const speed = isStopped ? 0 : isTail ? 4 : 14 + Math.floor(Math.random() * 10);

          return {
            ...member,
            connectionStatus: isTroubledOrigLeader
              ? ('OFFLINE' as ConnectionStatus)
              : member.connectionStatus,
            mapPosition: interpolateAlongPolyline(
              nextProgress,
              routeConfig.waypoints,
              offShift,
              speed
            ),
            lastLocationText: isTroubledOrigLeader
              ? 'Sinyal/Baterai Mutawwif Terputus'
              : landmarkForProgress(nextProgress),
            lastUpdatedLabel: isTroubledOrigLeader
              ? 'Terputus'
              : member.connectionStatus === 'WEAK_SIGNAL'
                ? '5 dtk lalu'
                : 'Baru saja',
          };
        });

        const evaluated = evaluateRoomSafety(moved, routeConfig, {
          originalLeaderUid: prev.leaderUid,
          actingLeaderUid: prev.actingLeaderUid,
          leaderTroubleActive: prev.leaderTroubleActive,
        });
        setOffRouteAlert(evaluated.offRouteAlert);
        setStragglerAlert(evaluated.stragglerAlert);
        return {
          ...prev,
          coLeaderUid: prev.leaderTroubleActive
            ? prev.coLeaderUid
            : evaluated.coLeaderUid,
          members: evaluated.members,
        };
      });
    }, 2000);
    return () => clearInterval(interval);
  }, [
    isSimulationRunning,
    activeRoom?.roomId,
    activeRoom?.isSyncedRoom,
    routeConfig,
    isOffRouteSimActive,
    isStragglerSimActive,
    gps.status,
  ]);

  const isFamilyAccount = Boolean(familyGrant && familyGrant.active);
  const isSessionAuthenticated = Boolean(authUser || localUserName);
  const hasAuthenticatedCloudEmailSession = Boolean(
    authUser && !isDeviceCloudGuestEmail(authUser.email)
  );
  const isAdminUser =
    !isFamilyAccount &&
    isAdminEmailAddress(authUser?.email || vehicleProfile.email || localUserEmail);
  const canCreateRoom =
    !isFamilyAccount &&
    (isVerifiedAdminSession(authUser) ||
      (hasAuthenticatedCloudEmailSession &&
        (isAdminEmailAddress(authUser?.email) || isRegisteredTourLeader)));
  const isRegisteredLeaderUnverified = Boolean(
    !isFamilyAccount &&
      hasAuthenticatedCloudEmailSession &&
      authUser &&
      !authUser.emailVerified &&
      isRegisteredTourLeader &&
      !isVerifiedAdminSession(authUser)
  );

  const visibleRouteSteps = isFamilyAccount
    ? [{ route: 'family_monitor' as ScreenRoute, label: 'Pantauan Keluarga' }]
    : canCreateRoom
      ? ROUTE_STEPS
      : ROUTE_STEPS.filter((step) => step.route !== 'tour_leader');
  const navSteps = isAdminUser
    ? [...visibleRouteSteps, { route: 'admin_backend' as ScreenRoute, label: '6. Admin Backend' }]
    : visibleRouteSteps;

  const navigateTo = (next: ScreenRoute, direction: 1 | -1 = 1) => {
    if (
      !isSessionAuthenticated &&
      next !== 'login' &&
      next !== 'about_app'
    ) {
      setAuthError('Silakan masuk terlebih dahulu sebelum menggunakan aplikasi.');
      setNavDirection(-1);
      setCurrentRoute('login');
      return;
    }
    if (
      isFamilyAccount &&
      next !== 'family_monitor' &&
      next !== 'login' &&
      next !== 'about_app'
    ) {
      setNavDirection(1);
      setCurrentRoute('family_monitor');
      return;
    }
    if (next === 'tour_leader' && !canCreateRoom) {
      setAuthError(
        'Menu Buat Rombongan Baru hanya tersedia untuk Ketua Rombongan yang emailnya terdaftar oleh Admin.'
      );
      setNavDirection(-1);
      setCurrentRoute('role_selection');
      return;
    }
    setNavDirection(direction);
    setCurrentRoute(next);
  };

  // Subscribe to current user's personal Family Invite Link when inside a Cloud Sync room
  useEffect(() => {
    if (!activeRoom?.isSyncedRoom || !activeRoom.roomId || !authUser?.uid || isFamilyAccount) {
      setMyFamilyLink(null);
      return;
    }
    const unsub = subscribeToMyFamilyLink(
      activeRoom.roomId,
      authUser.uid,
      (link) => setMyFamilyLink(link)
    );
    return () => unsub();
  }, [activeRoom?.roomId, activeRoom?.isSyncedRoom, authUser?.uid, isFamilyAccount]);

  // Subscribe to strict read-only single-jamaah Family Monitoring snapshot
  useEffect(() => {
    if (!familyGrant?.active || !authUser?.uid) {
      setFamilyMonitorSnapshot(null);
      setFamilyMonitorError(null);
      return;
    }
    const unsub = subscribeToFamilyMonitor(familyGrant, {
      onSnapshot: (snap) => {
        setFamilyMonitorSnapshot(snap);
        setFamilyMonitorError(null);
      },
      onError: (msg) => {
        setFamilyMonitorError(msg);
      },
    });
    return () => unsub();
  }, [
    familyGrant?.active,
    familyGrant?.inviteCode,
    familyGrant?.roomId,
    familyGrant?.targetUid,
    authUser?.uid,
  ]);

  // Periodic clock tick for Missing Pilgrim staleness detection (75s threshold)
  useEffect(() => {
    if (!activeRoom && !familyGrant) return;
    const id = setInterval(() => setNowTickMs(Date.now()), 5000);
    return () => clearInterval(id);
  }, [Boolean(activeRoom), Boolean(familyGrant)]);

  const MISSING_PILGRIM_THRESHOLD_MS = 75_000;
  const missingPilgrimAlert: MissingPilgrimAlertState = (() => {
    if (!activeRoom) {
      return {
        isTriggered: false,
        affectedMembers: [],
        thresholdSeconds: 75,
        staleSinceLabel: '',
      };
    }
    const affected = activeRoom.members.filter((m) => {
      if (m.connectionStatus === 'OFFLINE') return true;
      if (activeRoom.isSyncedRoom && m.lastUpdatedMs && m.lastUpdatedMs > 0) {
        return nowTickMs - m.lastUpdatedMs >= MISSING_PILGRIM_THRESHOLD_MS;
      }
      return false;
    });
    return {
      isTriggered: affected.length > 0,
      affectedMembers: affected,
      thresholdSeconds: 75,
      staleSinceLabel: affected[0]?.lastUpdatedLabel || 'Tidak ada sinyal > 75 dtk',
    };
  })();

  // Audio + Browser Notification for newly undetected / missing pilgrims
  useEffect(() => {
    if (!missingPilgrimAlert.isTriggered || missingPilgrimAlert.affectedMembers.length === 0) {
      lastAlertedMissingIdsRef.current = '';
      return;
    }
    const sig = missingPilgrimAlert.affectedMembers.map((m) => m.id).sort().join('|');
    if (lastAlertedMissingIdsRef.current === sig) return;
    lastAlertedMissingIdsRef.current = sig;

    if (routeConfigRef.current.soundAlertEnabled) {
      try {
        const AudioCtx =
          window.AudioContext ||
          (window as unknown as { webkitAudioContext: typeof AudioContext })
            .webkitAudioContext;
        if (AudioCtx) {
          const ctx = new AudioCtx();
          [0, 0.22].forEach((offset) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(660, ctx.currentTime + offset);
            gain.gain.setValueAtTime(0.08, ctx.currentTime + offset);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + offset + 0.18);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(ctx.currentTime + offset);
            osc.stop(ctx.currentTime + offset + 0.18);
          });
        }
      } catch {
        // Ignore AudioContext restriction
      }
    }

    if (
      typeof window !== 'undefined' &&
      'Notification' in window &&
      Notification.permission === 'granted'
    ) {
      try {
        const names = missingPilgrimAlert.affectedMembers.map((m) => m.fullName).join(', ');
        new Notification('Peringatan Jamaah Tidak Terdeteksi', {
          body: `${names} terputus atau belum mengirim sinyal lokasi.`,
          tag: `safar-missing-${sig}`,
        });
      } catch {
        // Ignore Notification errors
      }
    }
  }, [missingPilgrimAlert.isTriggered, missingPilgrimAlert.affectedMembers.map((m) => m.id).join('|')]);

  const handleGoogleSignIn = async () => {
    if (isSigningIn) return;
    pendingFamilyRedemptionRef.current = null;
    setIsSigningIn(true);
    setAuthError(null);
    setAuthInfoMessage(null);
    try {
      const { user, redirected } = await signInWithGoogle();
      if (redirected || !user) {
        setAuthInfoMessage('Mengarahkan ke halaman login Google...');
        return;
      }
      setLocalUserName(null);
      saveLocalSession(null);
      setNavDirection(1);
      setCurrentRoute('role_selection');
    } catch (err) {
      console.error('Login Google gagal:', err);
      setAuthError(mapFirebaseAuthError(err, 'Google'));
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleFamilyGoogleSignIn = async (inviteInput: string) => {
    if (isSigningIn) return;
    const code = extractFamilyInviteCode(inviteInput);
    if (!code) {
      setAuthError(
        'Kode atau link pantauan keluarga tidak valid (contoh: FAM-XXXXXX).'
      );
      return;
    }
    setIsSigningIn(true);
    setAuthError(null);
    setAuthInfoMessage(null);
    pendingFamilyRedemptionRef.current = { code };
    try {
      const { user, redirected } = await signInWithGoogle();
      if (redirected || !user) {
        setAuthInfoMessage('Mengarahkan ke login Google untuk pantauan keluarga...');
        return;
      }
      setLocalUserName(null);
      saveLocalSession(null);
      const grant = await redeemFamilyInviteLink(
        code,
        user.uid,
        user.displayName || 'Keluarga Jamaah'
      );
      pendingFamilyRedemptionRef.current = null;
      setFamilyGrant(grant);
      setPendingFamilyInviteCode('');
      setNavDirection(1);
      setCurrentRoute('family_monitor');
    } catch (err) {
      pendingFamilyRedemptionRef.current = null;
      setAuthError(
        err instanceof Error
          ? err.message
          : mapFirebaseAuthError(err, 'Google Keluarga')
      );
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleFamilyEmailAuth = async (params: {
    mode: 'signin' | 'signup';
    fullName: string;
    email: string;
    password: string;
    inviteInput: string;
  }) => {
    if (isSigningIn) return;
    const code = extractFamilyInviteCode(params.inviteInput);
    if (!code) {
      setAuthError(
        'Kode atau link pantauan keluarga tidak valid (contoh: FAM-XXXXXX).'
      );
      return;
    }
    setIsSigningIn(true);
    setAuthError(null);
    setAuthInfoMessage(null);
    const normEmail = normalizeProfileEmail(params.email);
    pendingFamilyRedemptionRef.current = {
      code,
      name: params.fullName.trim() || 'Keluarga Jamaah',
    };
    try {
      const user =
        params.mode === 'signup'
          ? await signUpWithEmail(params.fullName, normEmail, params.password)
          : await signInWithEmail(normEmail, params.password);
      setLocalUserName(null);
      saveLocalSession(null);
      const grant = await redeemFamilyInviteLink(
        code,
        user.uid,
        params.fullName.trim() || user.displayName || 'Keluarga Jamaah'
      );
      pendingFamilyRedemptionRef.current = null;
      setFamilyGrant(grant);
      setPendingFamilyInviteCode('');
      setNavDirection(1);
      setCurrentRoute('family_monitor');
    } catch (err) {
      pendingFamilyRedemptionRef.current = null;
      setAuthError(
        err instanceof Error
          ? err.message
          : mapFirebaseAuthError(err, 'Email Keluarga')
      );
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleFacebookSignIn = async () => {
    if (isSigningIn) return;
    setIsSigningIn(true);
    setAuthError(null);
    setAuthInfoMessage(null);
    try {
      await signInWithFacebook();
      setLocalUserName(null);
      saveLocalSession(null);
      setNavDirection(1);
      setCurrentRoute('role_selection');
    } catch (err) {
      console.error('Login Facebook gagal:', err);
      setAuthError(mapFirebaseAuthError(err, 'Facebook'));
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleInstagramSignIn = async () => {
    if (isSigningIn) return;
    setIsSigningIn(true);
    setAuthError(null);
    setAuthInfoMessage(null);
    try {
      const res = await fetch('/api/auth/instagram/url');
      if (!res.ok) {
        throw new Error('Gagal menghubungi server OAuth Instagram.');
      }
      const data = (await res.json()) as {
        configured?: boolean;
        url?: string;
        redirectUri?: string;
        message?: string;
      };

      if (!data.configured || !data.url) {
        setIsSigningIn(false);
        setAuthError(
          `Instagram OAuth belum aktif (${data.message || 'INSTAGRAM_CLIENT_ID belum diatur'}). Callback URI: ${data.redirectUri || `${window.location.origin}/auth/callback`}`
        );
        return;
      }

      const authWindow = window.open(
        data.url,
        'instagram_oauth_popup',
        'width=540,height=680'
      );
      if (!authWindow) {
        setIsSigningIn(false);
        setAuthError(
          'Popup Instagram diblokir browser. Izinkan popup untuk situs ini lalu coba lagi.'
        );
      }
    } catch (err) {
      setIsSigningIn(false);
      setAuthError(
        err instanceof Error
          ? err.message
          : 'Gagal membuka otorisasi Instagram.'
      );
    }
  };

  const handleEmailAuth = async (params: {
    mode: 'signin' | 'signup';
    fullName: string;
    email: string;
    password: string;
  }) => {
    if (isSigningIn) return;
    setIsSigningIn(true);
    setAuthError(null);
    setAuthInfoMessage(null);
    const normEmail = normalizeProfileEmail(params.email);

    try {
      const user =
        params.mode === 'signup'
          ? await signUpWithEmail(params.fullName, normEmail, params.password)
          : await signInWithEmail(normEmail, params.password);

      setLocalUserName(null);
      saveLocalSession(null);

      if (params.mode === 'signup') {
        const seedProfile: VehicleProfile = {
          ...INITIAL_PROFILE,
          fullName: params.fullName.trim() || user.displayName || 'Jamaah Safar Tracking',
          email: normEmail,
          authProvider: 'email',
        };
        setVehicleProfile(seedProfile);
        saveProfileByEmail(normEmail, seedProfile);
        await saveUserPrivateProfile(user.uid, seedProfile).catch(() => {});
        setCustomizeReturnRoute('role_selection');
        setNavDirection(1);
        setCurrentRoute('customize_profile');
      } else {
        setNavDirection(1);
        setCurrentRoute('role_selection');
      }
    } catch (err) {
      console.error('Email auth gagal:', err);
      setAuthError(mapFirebaseAuthError(err, 'Email'));
    } finally {
      setIsSigningIn(false);
    }
  };

  const handleForgotPassword = async (email: string) => {
    setAuthError(null);
    setAuthInfoMessage(null);
    try {
      await sendResetPasswordEmail(email);
      setAuthInfoMessage(
        `Tautan atur ulang kata sandi telah dikirim ke ${email.trim()}.`
      );
    } catch (err) {
      setAuthError(mapFirebaseAuthError(err, 'Reset Sandi'));
    }
  };

  const handleLocalLogin = (fullName: string, email: string) => {
    const cleanName = fullName.trim();
    const normEmail = normalizeProfileEmail(email);
    if (!cleanName) return;

    setAuthError(null);
    setAuthInfoMessage(null);
    setAuthProvider('local');
    setLocalUserName(cleanName);
    setLocalUserEmail(normEmail);

    const existingEmailProfile = normEmail ? loadProfileByEmail(normEmail) : null;
    const nextProfile: VehicleProfile = existingEmailProfile
      ? {
          ...existingEmailProfile,
          fullName: existingEmailProfile.fullName || cleanName,
          email: normEmail,
          authProvider: 'local',
        }
      : {
          ...vehicleProfile,
          fullName: cleanName,
          email: normEmail,
          authProvider: 'local',
        };

    setVehicleProfile(nextProfile);
    if (normEmail) {
      saveProfileByEmail(normEmail, nextProfile);
    }
    saveLocalSession({
      fullName: nextProfile.fullName,
      email: normEmail,
      authProvider: 'local',
    });
    setNavDirection(1);
    setCurrentRoute('role_selection');
  };

  const handleGoogleSignOut = async () => {
    try {
      if (authUser && familyGrant?.active) {
        await clearFamilyGrant(authUser.uid);
      }
      if (authUser) {
        await signOutFirebase();
      }
      pendingFamilyRedemptionRef.current = null;
      setFamilyGrant(null);
      setFamilyMonitorSnapshot(null);
      setMyFamilyLink(null);
      setLocalUserName(null);
      setLocalUserEmail('');
      setAuthProvider('local');
      saveLocalSession(null);
      setAuthError(null);
      setAuthInfoMessage(null);
      setProfileSyncMessage(null);
      setActiveRoom(null);
      setNavDirection(-1);
      setCurrentRoute('login');
    } catch (err) {
      console.error('Logout gagal:', err);
    }
  };

  const handleSaveCustomizedProfile = async (nextProfile: VehicleProfile) => {
    setIsSavingProfile(true);
    setProfileSyncMessage(null);
    const normEmail = normalizeProfileEmail(
      nextProfile.email || authUser?.email || localUserEmail
    );
    const enrichedProfile: VehicleProfile = {
      ...nextProfile,
      email: normEmail,
      authProvider: nextProfile.authProvider || authProvider,
    };

    setVehicleProfile(enrichedProfile);
    if (normEmail) {
      setLocalUserEmail(normEmail);
      saveProfileByEmail(normEmail, enrichedProfile);
    }
    if (!authUser && localUserName) {
      setLocalUserName(enrichedProfile.fullName);
      saveLocalSession({
        fullName: enrichedProfile.fullName,
        email: normEmail,
        authProvider,
        instagramHandle: enrichedProfile.instagramHandle,
      });
    }

    // Update current user card inside active room if already riding
    setActiveRoom((prev) =>
      prev
        ? {
            ...prev,
            members: prev.members.map((m) =>
              m.isCurrentUser
                ? {
                    ...m,
                    fullName: enrichedProfile.fullName || m.fullName,
                    vehicleType: enrichedProfile.vehicleType,
                    vehicleDetail:
                      enrichedProfile.vehicleDetail || m.vehicleDetail,
                    licensePlate: enrichedProfile.licensePlate || m.licensePlate,
                    emergencyContact:
                      enrichedProfile.emergencyContact || m.emergencyContact,
                    avatarPreset: enrichedProfile.avatarPreset,
                    avatarPhotoUrl: enrichedProfile.avatarPhotoUrl,
                  }
                : m
            ),
          }
        : prev
    );

    try {
      if (authUser) {
        await saveUserPrivateProfile(authUser.uid, enrichedProfile);
      }
      setProfileSyncMessage(
        normEmail
          ? `Tersimpan & tersinkronisasi dengan ${normEmail}`
          : 'Profil berhasil disimpan.'
      );
    } catch (err) {
      console.warn('Gagal sinkronisasi ke Firestore:', err);
      setProfileSyncMessage(
        'Profil tersimpan lokal di perangkat. Pastikan Firestore & koneksi aktif untuk sinkronisasi cloud.'
      );
    } finally {
      setIsSavingProfile(false);
    }
  };

  const manasikStorageScope =
    authUser && !isDeviceCloudGuestEmail(authUser.email)
      ? authUser.uid
      : localUserEmail || localUserName || 'default';
  const isManasikCloudSynced = Boolean(
    authUser && !isDeviceCloudGuestEmail(authUser.email)
  );

  const handleToggleManasikStep = (guideType: ManasikGuideType, stepId: string) => {
    setManasikProgress((prev) => {
      const existing = prev[guideType] || [];
      const isRemoving = existing.includes(stepId);
      const nextList = isRemoving
        ? existing.filter((id) => id !== stepId)
        : [...existing, stepId];
      const nextCheckIns = { ...(prev.checkIns || {}) };
      if (isRemoving) {
        delete nextCheckIns[stepId];
      }
      const nextProgress: ManasikProgressState = {
        ...prev,
        [guideType]: nextList,
        checkIns: nextCheckIns,
      };
      saveManasikProgressLocal(manasikStorageScope, nextProgress);
      if (authUser && !isDeviceCloudGuestEmail(authUser.email)) {
        saveManasikProgressToFirestore(
          authUser.uid,
          guideType,
          nextList,
          nextCheckIns
        ).catch((err) =>
          console.warn('Gagal sinkron checklist manasik:', err)
        );
      }
      return nextProgress;
    });
  };

  const handleCheckInManasikStep = (
    guideType: ManasikGuideType,
    checkIn: ManasikCheckInRecord
  ) => {
    setManasikProgress((prev) => {
      const existing = prev[guideType] || [];
      const nextList = existing.includes(checkIn.stepId)
        ? existing
        : [...existing, checkIn.stepId];
      const nextCheckIns = {
        ...(prev.checkIns || {}),
        [checkIn.stepId]: checkIn,
      };
      const nextProgress: ManasikProgressState = {
        ...prev,
        [guideType]: nextList,
        checkIns: nextCheckIns,
      };
      saveManasikProgressLocal(manasikStorageScope, nextProgress);
      if (authUser && !isDeviceCloudGuestEmail(authUser.email)) {
        saveManasikProgressToFirestore(
          authUser.uid,
          guideType,
          nextList,
          nextCheckIns
        ).catch((err) =>
          console.warn('Gagal sinkron check-in manasik:', err)
        );
      }
      return nextProgress;
    });
  };

  const handleResetManasikGuide = (guideType: ManasikGuideType) => {
    setManasikProgress((prev) => {
      const prefix = guideType === 'umrah' ? 'umrah_' : 'haji_';
      const nextCheckIns: Record<string, ManasikCheckInRecord> = {};
      for (const [k, v] of Object.entries(prev.checkIns || {})) {
        if (!k.startsWith(prefix)) {
          nextCheckIns[k] = v;
        }
      }
      const nextProgress: ManasikProgressState = {
        ...prev,
        [guideType]: [],
        checkIns: nextCheckIns,
      };
      saveManasikProgressLocal(manasikStorageScope, nextProgress);
      if (authUser && !isDeviceCloudGuestEmail(authUser.email)) {
        saveManasikProgressToFirestore(
          authUser.uid,
          guideType,
          [],
          nextCheckIns
        ).catch((err) =>
          console.warn('Gagal reset checklist manasik:', err)
        );
      }
      return nextProgress;
    });
  };

  const openManasikAssistant = (
    returnRoute: ScreenRoute,
    initialPrompt?: string
  ) => {
    setAssistantReturnRoute(returnRoute);
    setAssistantInitialPrompt(initialPrompt || null);
    navigateTo('ai_assistant', 1);
  };

  const ensureDemoRoom = (roleForFallback: UserRole) => {
    if (activeRoom) return;
    const roomId = 'UMR-8492';
    const initialLat = gps.lastFix?.latitude ?? routeConfig.waypoints[0]?.latitude ?? 21.418;
    const initialLng = gps.lastFix?.longitude ?? routeConfig.waypoints[0]?.longitude ?? 39.8275;
    const selfMember: ConvoyMember = {
      id: 'self-demo',
      fullName: vehicleProfile.fullName.trim() || 'Anda',
      vehicleType: vehicleProfile.vehicleType,
      vehicleDetail:
        vehicleProfile.vehicleDetail.trim() ||
        (vehicleProfile.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'),
      licensePlate: vehicleProfile.licensePlate.trim() || 'JKT-01 / KMR 101',
      emergencyContact: vehicleProfile.emergencyContact.trim() || '0812-3456-7890',
      role: roleForFallback,
      joinedAtLabel: 'Baru saja',
      isCurrentUser: true,
      isSweeper: false,
      avatarPreset: vehicleProfile.avatarPreset,
      avatarPhotoUrl: vehicleProfile.avatarPhotoUrl,
      conditionStatus: 'AMAN',
      connectionStatus: 'CONNECTED',
      lastLocationText: gps.lastFix ? 'GPS Live' : 'Area Sekitar Masjidil Haram',
      lastUpdatedLabel: 'Baru saja',
      routeDistanceMeters: 700,
      offRouteMeters: 0,
      convoyOrderRank: 2,
      mapPosition: gps.lastFix
        ? {
            routeProgress: 0.25,
            xFraction: 0.4,
            yFraction: 0.6,
            latitude: initialLat,
            longitude: initialLng,
            speedKmh: gps.lastFix.speedKmh,
            heading: gps.lastFix.heading,
            accuracyMeters: gps.lastFix.accuracyMeters,
          }
        : interpolateAlongPolyline(0.58, routeConfig.waypoints, 0, 12),
    };
    const simulatedPeers: ConvoyMember[] = DEFAULT_CONVOY_TEMPLATES.slice(0, 3).map(
      (tpl, idx) => ({
        id: `peer-demo-${idx}`,
        fullName: tpl.fullName,
        vehicleType: tpl.vehicleType,
        vehicleDetail: tpl.vehicleDetail,
        licensePlate: tpl.licensePlate,
        emergencyContact: tpl.emergencyContact,
        role: idx === 0 && roleForFallback === 'MEMBER' ? 'TOUR_LEADER' : 'MEMBER',
        joinedAtLabel: 'Aktif di grup',
        isCurrentUser: false,
        isSweeper: idx === 1,
        avatarPreset: tpl.avatarPreset,
        conditionStatus: tpl.conditionStatus,
        connectionStatus: tpl.connectionStatus,
        lastLocationText: tpl.lastLocationText,
        lastUpdatedLabel: tpl.connectionStatus === 'WEAK_SIGNAL' ? '4 dtk lalu' : 'Baru saja',
        routeDistanceMeters: Math.round(tpl.baseProgress * routeConfig.totalRouteMeters),
        offRouteMeters: 0,
        convoyOrderRank: idx + 1,
        mapPosition: interpolateAlongPolyline(
          tpl.baseProgress,
          routeConfig.waypoints,
          0,
          tpl.baseSpeed
        ),
      })
    );

    const baseRoom: TourRoom = {
      roomId,
      roomTitle: 'Rombongan Umroh Makkah (Demo)',
      inviteLink: `https://safartracking.id/join/${roomId}`,
      createdAtLabel: 'Aktif sekarang',
      isSyncedRoom: false,
      members: [selfMember, ...simulatedPeers],
    };
    setSelectedMemberId(selfMember.id);
    setMessages(createInitialMessages(roomId));
    setRoomActivities(createStarterRoomUmrahActivities('peer-demo-0'));
    setRoomActivityProgress({});
    commitRoomMembers(baseRoom.members, routeConfig, baseRoom);
  };

  const handleSelectRole = (role: UserRole) => {
    if (role === 'TOUR_LEADER' && !canCreateRoom) {
      setAuthError(
        'Menu Buat Rombongan Baru hanya tersedia untuk Ketua Rombongan yang emailnya terdaftar oleh Admin.'
      );
      return;
    }
    if (
      role === 'TOUR_LEADER' &&
      globalConfig.allowNewRooms === false &&
      !isVerifiedAdminSession(authUser)
    ) {
      setAuthError('Pembuatan grup rombongan baru sedang dijeda sementara oleh Admin Backend.');
      return;
    }
    setSelectedRole(role);
    navigateTo('vehicle_profile', 1);
  };

  const handleUpdateProfile = (patch: Partial<VehicleProfile>) => {
    setVehicleProfile((prev) => {
      const next = { ...prev, ...patch };
      const normEmail = normalizeProfileEmail(
        next.email || authUser?.email || localUserEmail
      );
      if (normEmail) {
        saveProfileByEmail(normEmail, { ...next, email: normEmail });
      }
      if (authUser) {
        saveUserPrivateProfile(authUser.uid, {
          ...next,
          email: normEmail,
          authProvider: next.authProvider || authProvider,
        }).catch((err) =>
          console.warn('Gagal sinkronisasi profil ke Firestore:', err)
        );
      }
      return next;
    });
  };

  const handleAddDraftActivity = (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => {
    setDraftRoomActivities((prev) => {
      if (prev.length >= 40) return prev;
      const nextOrder =
        prev.length > 0 ? Math.max(...prev.map((a) => a.orderIndex)) + 1 : 0;
      return [
        ...prev,
        {
          id: `act-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
          title: input.title.trim().slice(0, 100) || 'Kegiatan Umroh',
          timeLabel: input.timeLabel.trim().slice(0, 32),
          locationNote: input.locationNote.trim().slice(0, 120),
          orderIndex: nextOrder,
          createdByUid: authUser?.uid || 'leader',
        },
      ];
    });
  };

  const handleRemoveDraftActivity = (activityId: string) => {
    setDraftRoomActivities((prev) =>
      prev
        .filter((a) => a.id !== activityId)
        .map((item, idx) => ({ ...item, orderIndex: idx }))
    );
  };

  const handleMoveDraftActivity = (activityId: string, direction: 'up' | 'down') => {
    setDraftRoomActivities((prev) => {
      const sorted = [...prev].sort((a, b) => a.orderIndex - b.orderIndex);
      const idx = sorted.findIndex((a) => a.id === activityId);
      const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
      if (idx < 0 || targetIdx < 0 || targetIdx >= sorted.length) return prev;
      const [moved] = sorted.splice(idx, 1);
      sorted.splice(targetIdx, 0, moved);
      return sorted.map((item, i) => ({ ...item, orderIndex: i }));
    });
  };

  const handleApplyDraftStarterTemplates = (items: RoomActivityItem[]) => {
    setDraftRoomActivities(
      items.map((item, idx) => ({
        ...item,
        orderIndex: idx,
        createdByUid: authUser?.uid || 'leader',
      }))
    );
  };

  const handleAddRoomActivity = (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => {
    const room = activeRoomRef.current ?? activeRoom;
    if (!room || roomActivities.length >= 40) return;
    const creatorUid =
      (room.isSyncedRoom && authUser?.uid) || room.leaderUid || 'leader';
    const nextOrder =
      roomActivities.length > 0
        ? Math.max(...roomActivities.map((a) => a.orderIndex)) + 1
        : 0;
    const newItem: RoomActivityItem = {
      id: `act-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      title: input.title.trim().slice(0, 100) || 'Kegiatan Umroh',
      timeLabel: input.timeLabel.trim().slice(0, 32),
      locationNote: input.locationNote.trim().slice(0, 120),
      orderIndex: nextOrder,
      createdByUid: creatorUid,
    };
    setRoomActivities((prev) => [...prev, newItem]);
    if (room.isSyncedRoom && authUser) {
      addSyncedRoomActivity(room.roomId, authUser.uid, newItem).catch((err) =>
        console.warn('Gagal menambah kegiatan grup:', err)
      );
    }
  };

  const handleRemoveRoomActivity = (activityId: string) => {
    const room = activeRoomRef.current ?? activeRoom;
    setRoomActivities((prev) =>
      prev
        .filter((a) => a.id !== activityId)
        .map((item, idx) => ({ ...item, orderIndex: idx }))
    );
    if (room?.isSyncedRoom && authUser) {
      removeSyncedRoomActivity(room.roomId, activityId).catch((err) =>
        console.warn('Gagal menghapus kegiatan grup:', err)
      );
    }
  };

  const handleMoveRoomActivity = (activityId: string, direction: 'up' | 'down') => {
    const sorted = [...roomActivities].sort((a, b) => a.orderIndex - b.orderIndex);
    const idx = sorted.findIndex((a) => a.id === activityId);
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (idx < 0 || targetIdx < 0 || targetIdx >= sorted.length) return;
    const swapped = [...sorted];
    const [moved] = swapped.splice(idx, 1);
    swapped.splice(targetIdx, 0, moved);
    const reindexed = swapped.map((item, i) => ({ ...item, orderIndex: i }));
    setRoomActivities(reindexed);
    const room = activeRoomRef.current ?? activeRoom;
    if (room?.isSyncedRoom && authUser) {
      const a = reindexed[idx];
      const b = reindexed[targetIdx];
      updateSyncedRoomActivity(room.roomId, a.id, { orderIndex: a.orderIndex }).catch((err) =>
        console.warn('Gagal mengubah urutan kegiatan:', err)
      );
      updateSyncedRoomActivity(room.roomId, b.id, { orderIndex: b.orderIndex }).catch((err) =>
        console.warn('Gagal mengubah urutan kegiatan:', err)
      );
    }
  };

  const handleApplyRoomStarterTemplates = (items: RoomActivityItem[]) => {
    const room = activeRoomRef.current ?? activeRoom;
    if (!room) return;
    const creatorUid =
      (room.isSyncedRoom && authUser?.uid) || room.leaderUid || 'leader';
    const normalized = items.map((item, idx) => ({
      ...item,
      orderIndex: idx,
      createdByUid: creatorUid,
    }));
    setRoomActivities(normalized);
    if (room.isSyncedRoom && authUser) {
      normalized.forEach((act) => {
        addSyncedRoomActivity(room.roomId, authUser.uid, act).catch((err) =>
          console.warn('Gagal menambah template kegiatan grup:', err)
        );
      });
    }
  };

  const handleToggleRoomActivity = (activityId: string) => {
    const room = activeRoomRef.current ?? activeRoom;
    if (!room) return;
    const selfMember = room.members.find((m) => m.isCurrentUser);
    const memberUid =
      (room.isSyncedRoom && authUser?.uid) ||
      selfMember?.id ||
      authUser?.uid ||
      'local-user';
    const memberName =
      selfMember?.fullName ||
      vehicleProfile.fullName.trim() ||
      authUser?.displayName ||
      localUserName ||
      'Jamaah Umroh';

    const prevEntry = roomActivityProgress[memberUid];
    const prevIds = prevEntry?.completedActivityIds ?? [];
    const prevTimes = prevEntry?.completedTimes ?? {};
    const exists = prevIds.includes(activityId);
    const nextIds = exists
      ? prevIds.filter((id) => id !== activityId)
      : [...prevIds, activityId];
    const nextTimes = { ...prevTimes };
    if (exists) {
      delete nextTimes[activityId];
    } else {
      nextTimes[activityId] = currentClock();
    }

    setRoomActivityProgress((prev) => ({
      ...prev,
      [memberUid]: {
        uid: memberUid,
        memberName,
        completedActivityIds: nextIds,
        completedTimes: nextTimes,
      },
    }));

    if (room.isSyncedRoom && authUser) {
      saveMemberRoomActivityProgress(
        room.roomId,
        authUser.uid,
        memberName,
        nextIds,
        nextTimes
      ).catch((err) =>
        console.warn('Gagal menyinkronkan checklist kegiatan grup:', err)
      );
    }
  };

  const handleCreateRoom = async (
    customTitle = 'Rombongan Ziarah Makkah',
    routeOverride?: ConvoyRouteConfig
  ) => {
    if (!canCreateRoom) {
      setCreateRoomError(
        'Hanya Ketua Rombongan yang emailnya terdaftar oleh Admin yang dapat membuat rombongan baru.'
      );
      return;
    }
    if (
      globalConfig.allowNewRooms === false &&
      !isVerifiedAdminSession(authUser)
    ) {
      setJoinError('Pembuatan grup rombongan baru sedang dijeda oleh Admin Backend.');
      return;
    }
    const effectiveConfig = routeOverride ?? routeConfig;
    const roomId = generateUniqueRoomId();
    const title = customTitle.trim() || 'Rombongan Ziarah Makkah';
    const startLat =
      effectiveConfig.originLockedByLeader || effectiveConfig.waypoints.length >= 2
        ? (effectiveConfig.waypoints[0]?.latitude ?? gps.lastFix?.latitude ?? 21.418)
        : (gps.lastFix?.latitude ?? effectiveConfig.waypoints[0]?.latitude ?? 21.418);
    const startLng =
      effectiveConfig.originLockedByLeader || effectiveConfig.waypoints.length >= 2
        ? (effectiveConfig.waypoints[0]?.longitude ?? gps.lastFix?.longitude ?? 39.8275)
        : (gps.lastFix?.longitude ?? effectiveConfig.waypoints[0]?.longitude ?? 39.8275);
    const memberInitLat = gps.lastFix?.latitude ?? startLat;
    const memberInitLng = gps.lastFix?.longitude ?? startLng;

    // Cloud Synced Firestore Room (auto-provisions device cloud session if needed)
    setIsCreatingRoom(true);
    setCreateRoomError(null);
    try {
      const cloudUser = authUser ?? (await ensureCloudSignedInUser());
      setAuthUser(cloudUser);
      const stops = getEffectiveDestinationStops(effectiveConfig);
      const hasExplicitOrigin =
        effectiveConfig.originLockedByLeader || effectiveConfig.waypoints.length >= 2;
      const nextWaypoints: RoutePoint[] =
        !hasExplicitOrigin && gps.lastFix && stops.length > 0
          ? buildMultiStopPolyline(
              {
                xFraction: 0.14,
                yFraction: 0.86,
                latitude: gps.lastFix.latitude,
                longitude: gps.lastFix.longitude,
              },
              stops
            )
          : effectiveConfig.waypoints;

      const nextConfig: ConvoyRouteConfig = {
        ...effectiveConfig,
        destinationStops: stops,
        waypoints: nextWaypoints,
      };

      await saveUserPrivateProfile(cloudUser.uid, vehicleProfile);
      await createSyncedRoom({
        roomId,
        roomTitle: title,
        leaderUid: cloudUser.uid,
        profile: vehicleProfile,
        routeConfig: nextConfig,
        initialLat: memberInitLat,
        initialLng: memberInitLng,
        initialActivities: draftRoomActivities,
      });

      setRouteConfig(nextConfig);
      setRoomActivities(
        draftRoomActivities.map((act, idx) => ({
          ...act,
          orderIndex: idx,
          createdByUid: cloudUser.uid,
        }))
      );
      setRoomActivityProgress({});
      recordedTrackRef.current = gps.lastFix
        ? [{ lat: gps.lastFix.latitude, lng: gps.lastFix.longitude }]
        : [];
      setLastCompletedTrip(null);
      setIsOffRouteSimActive(false);
      setIsStragglerSimActive(false);
      setIsLeaderTroubleSimActive(false);
      const syncedRoom: TourRoom = {
        roomId,
        roomTitle: title,
        inviteLink: `https://safartracking.id/join/${roomId}`,
        createdAtLabel: 'Sinkronisasi Cloud Aktif',
        isSyncedRoom: true,
        leaderUid: cloudUser.uid,
        actingLeaderUid: cloudUser.uid,
        leaderTroubleActive: false,
        members: [],
      };
      setSelectedMemberId(cloudUser.uid);
      setActiveRoom(syncedRoom);
      navigateTo('live_map', 1);
    } catch (err) {
      console.error('Gagal membuat synced room:', err);
      const rawMsg = err instanceof Error ? err.message : String(err || '');
      const upperMsg = rawMsg.toUpperCase();
      const isAuthDisabled =
        upperMsg.includes('OPERATION_NOT_ALLOWED') ||
        upperMsg.includes('ADMIN_ONLY_OPERATION') ||
        upperMsg.includes('ADMIN-RESTRICTED-OPERATION') ||
        upperMsg.includes('PASSWORD_LOGIN_DISABLED');
      setCreateRoomError(
        isAuthDisabled
          ? 'Mode Tamu Cloud belum diaktifkan di Firebase Console. Silakan Masuk Google untuk Grup Cloud Live, atau klik "Buat Grup Lokal (Offline / Demo)" di bawah.'
          : err instanceof Error
            ? `Gagal membuat grup cloud: ${err.message}`
            : 'Gagal terhubung ke Firebase Firestore. Periksa koneksi internet dan coba lagi.'
      );
    } finally {
      setIsCreatingRoom(false);
    }
  };

  const handleCreateLocalDemoRoom = (
    customTitle = 'Rombongan Ziarah Makkah',
    routeOverride?: ConvoyRouteConfig
  ) => {
    if (!canCreateRoom) {
      setCreateRoomError(
        'Hanya Ketua Rombongan yang emailnya terdaftar oleh Admin yang dapat membuat rombongan baru.'
      );
      return;
    }
    const effectiveConfig = routeOverride ?? routeConfig;
    const roomId = generateUniqueRoomId();
    const title = customTitle.trim() || 'Rombongan Ziarah Makkah';
    const startLat =
      effectiveConfig.originLockedByLeader || effectiveConfig.waypoints.length >= 2
        ? (effectiveConfig.waypoints[0]?.latitude ?? gps.lastFix?.latitude ?? 21.418)
        : (gps.lastFix?.latitude ?? effectiveConfig.waypoints[0]?.latitude ?? 21.418);
    const startLng =
      effectiveConfig.originLockedByLeader || effectiveConfig.waypoints.length >= 2
        ? (effectiveConfig.waypoints[0]?.longitude ?? gps.lastFix?.longitude ?? 39.8275)
        : (gps.lastFix?.longitude ?? effectiveConfig.waypoints[0]?.longitude ?? 39.8275);
    const initialLat = gps.lastFix?.latitude ?? startLat;
    const initialLng = gps.lastFix?.longitude ?? startLng;

    const selfMember: ConvoyMember = {
      id: 'self-local-leader',
      fullName: vehicleProfile.fullName.trim() || localUserName || 'Mutawwif',
      vehicleType: vehicleProfile.vehicleType,
      vehicleDetail:
        vehicleProfile.vehicleDetail.trim() ||
        (vehicleProfile.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'),
      licensePlate: vehicleProfile.licensePlate.trim() || 'JKT-01 / KMR 101',
      emergencyContact: vehicleProfile.emergencyContact.trim() || '0812-3456-7890',
      role: 'TOUR_LEADER',
      joinedAtLabel: 'Baru saja',
      isCurrentUser: true,
      isSweeper: false,
      avatarPreset: vehicleProfile.avatarPreset,
      avatarPhotoUrl: vehicleProfile.avatarPhotoUrl,
      conditionStatus: 'AMAN',
      connectionStatus: 'CONNECTED',
      lastLocationText: gps.lastFix ? 'GPS Live (Lokal)' : 'Titik Kumpul Awal',
      lastUpdatedLabel: 'Baru saja',
      routeDistanceMeters: 0,
      offRouteMeters: 0,
      convoyOrderRank: 1,
      mapPosition: gps.lastFix
        ? {
            routeProgress: 0.05,
            xFraction: 0.14,
            yFraction: 0.86,
            latitude: initialLat,
            longitude: initialLng,
            speedKmh: gps.lastFix.speedKmh,
            heading: gps.lastFix.heading,
            accuracyMeters: gps.lastFix.accuracyMeters,
          }
        : interpolateAlongPolyline(0.08, effectiveConfig.waypoints, 0, 12),
    };

    const simulatedPeers: ConvoyMember[] = DEFAULT_CONVOY_TEMPLATES.slice(0, 3).map(
      (tpl, idx) => ({
        id: `peer-local-${idx}`,
        fullName: tpl.fullName,
        vehicleType: tpl.vehicleType,
        vehicleDetail: tpl.vehicleDetail,
        licensePlate: tpl.licensePlate,
        emergencyContact: tpl.emergencyContact,
        role: 'MEMBER',
        joinedAtLabel: 'Aktif di grup',
        isCurrentUser: false,
        isSweeper: idx === 1,
        avatarPreset: tpl.avatarPreset,
        conditionStatus: tpl.conditionStatus,
        connectionStatus: tpl.connectionStatus,
        lastLocationText: tpl.lastLocationText,
        lastUpdatedLabel:
          tpl.connectionStatus === 'WEAK_SIGNAL' ? '4 dtk lalu' : 'Baru saja',
        routeDistanceMeters: Math.round(
          tpl.baseProgress * effectiveConfig.totalRouteMeters
        ),
        offRouteMeters: 0,
        convoyOrderRank: idx + 2,
        mapPosition: interpolateAlongPolyline(
          tpl.baseProgress,
          effectiveConfig.waypoints,
          0,
          tpl.baseSpeed
        ),
      })
    );

    const localRoom: TourRoom = {
      roomId,
      roomTitle: title,
      inviteLink: `https://safartracking.id/join/${roomId}`,
      createdAtLabel: 'Mode Lokal / Offline',
      isSyncedRoom: false,
      leaderUid: selfMember.id,
      actingLeaderUid: selfMember.id,
      leaderTroubleActive: false,
      members: [selfMember, ...simulatedPeers],
    };

    setCreateRoomError(null);
    setSelectedRole('TOUR_LEADER');
    setSelectedMemberId(selfMember.id);
    recordedTrackRef.current = gps.lastFix
      ? [{ lat: gps.lastFix.latitude, lng: gps.lastFix.longitude }]
      : [];
    setLastCompletedTrip(null);
    setIsOffRouteSimActive(false);
    setIsStragglerSimActive(false);
    setIsLeaderTroubleSimActive(false);
    setMessages(createInitialMessages(roomId));
    setRoomActivities(
      draftRoomActivities.map((act, idx) => ({
        ...act,
        orderIndex: idx,
        createdByUid: selfMember.id,
      }))
    );
    setRoomActivityProgress({});
    commitRoomMembers(localRoom.members, effectiveConfig, localRoom);
    navigateTo('live_map', 1);
  };

  const handleSimulateJoin = () => {
    if (!activeRoom || activeRoom.isSyncedRoom) return;
    const tpl = DEFAULT_CONVOY_TEMPLATES[simIndex % DEFAULT_CONVOY_TEMPLATES.length];
    setSimIndex((prev) => prev + 1);
    const newMember: ConvoyMember = {
      id: `member-${Date.now()}-${simIndex}`,
      fullName: `${tpl.fullName} #${simIndex + 2}`,
      vehicleType: tpl.vehicleType,
      vehicleDetail: tpl.vehicleDetail,
      licensePlate: tpl.licensePlate,
      emergencyContact: tpl.emergencyContact,
      role: 'MEMBER',
      joinedAtLabel: 'Baru bergabung',
      isCurrentUser: false,
      isSweeper: false,
      avatarPreset: tpl.avatarPreset,
      conditionStatus: 'AMAN',
      connectionStatus: 'CONNECTED',
      lastLocationText: tpl.lastLocationText,
      lastUpdatedLabel: 'Baru saja',
      routeDistanceMeters: Math.round(0.5 * routeConfig.totalRouteMeters),
      offRouteMeters: 0,
      convoyOrderRank: activeRoom.members.length + 1,
      mapPosition: interpolateAlongPolyline(0.5, routeConfig.waypoints, 0, tpl.baseSpeed),
    };
    commitRoomMembers([...activeRoom.members, newMember]);
  };

  const handleRemoveMember = (memberId: string) => {
    if (!activeRoom) return;
    if (activeRoom.isSyncedRoom && authUser) {
      removeSyncedMember(activeRoom.roomId, memberId).catch((err) =>
        console.warn('Gagal menghapus jamaah:', err)
      );
      return;
    }
    commitRoomMembers(
      activeRoom.members.filter((m) => m.id !== memberId || m.isCurrentUser)
    );
  };

  const handleJoinSubmit = async () => {
    const code = extractRoomId(joinInput);
    if (code.length < 4) {
      setJoinError(
        'Masukkan Kode Grup yang valid (contoh: UMR-8492) atau tempel link undangan.'
      );
      return;
    }

    setIsJoiningRoom(true);
    setJoinError(null);
    try {
      const cloudUser = authUser ?? (await ensureCloudSignedInUser());
      setAuthUser(cloudUser);
      const initLat = gps.lastFix?.latitude ?? 21.418;
      const initLng = gps.lastFix?.longitude ?? 39.8275;
      await saveUserPrivateProfile(cloudUser.uid, vehicleProfile);
      const res = await joinSyncedRoom({
        roomId: code,
        uid: cloudUser.uid,
        profile: vehicleProfile,
        initialLat: initLat,
        initialLng: initLng,
      });
      if (!res.ok) {
        setJoinError(res.error || 'Gagal bergabung ke grup rombongan.');
        return;
      }
      setSelectedRole(res.role);
      setSelectedMemberId(cloudUser.uid);
      recordedTrackRef.current = gps.lastFix
        ? [{ lat: gps.lastFix.latitude, lng: gps.lastFix.longitude }]
        : [];
      setLastCompletedTrip(null);
      setRoomActivities([]);
      setRoomActivityProgress({});
      setActiveRoom({
        roomId: code,
        roomTitle: `Grup ${code}`,
        inviteLink: `https://safartracking.id/join/${code}`,
        createdAtLabel: 'Terhubung Cloud',
        isSyncedRoom: true,
        members: [],
      });
      navigateTo('live_map', 1);
    } catch (err) {
      setJoinError(
        !authUser
          ? mapFirebaseAuthError(err, 'Tamu Cloud')
          : err instanceof Error
            ? err.message
            : 'Gagal terhubung ke Firestore.'
      );
    } finally {
      setIsJoiningRoom(false);
    }
  };

  const joinOfflineDemoRoom = (rawCode: string) => {
    const code = extractRoomId(rawCode) || 'UMR-8492';
    // Offline Demo join fallback
    const hostTpl = DEFAULT_CONVOY_TEMPLATES[0];
    const peer1Tpl = DEFAULT_CONVOY_TEMPLATES[1];
    const peer2Tpl = DEFAULT_CONVOY_TEMPLATES[2];

    const hostLeader: ConvoyMember = {
      id: 'leader-host',
      fullName: hostTpl.fullName,
      vehicleType: hostTpl.vehicleType,
      vehicleDetail: hostTpl.vehicleDetail,
      licensePlate: hostTpl.licensePlate,
      emergencyContact: hostTpl.emergencyContact,
      role: 'TOUR_LEADER',
      joinedAtLabel: 'Host Grup',
      isCurrentUser: false,
      isSweeper: false,
      avatarPreset: hostTpl.avatarPreset,
      conditionStatus: 'AMAN',
      connectionStatus: 'CONNECTED',
      lastLocationText: 'Gerbang King Abdul Aziz',
      lastUpdatedLabel: 'Baru saja',
      routeDistanceMeters: 880,
      offRouteMeters: 0,
      convoyOrderRank: 1,
      mapPosition: interpolateAlongPolyline(0.74, routeConfig.waypoints, 0, 18),
    };

    const selfMember: ConvoyMember = {
      id: `self-${Date.now()}`,
      fullName: vehicleProfile.fullName.trim() || 'Anda',
      vehicleType: vehicleProfile.vehicleType,
      vehicleDetail:
        vehicleProfile.vehicleDetail.trim() ||
        (vehicleProfile.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'),
      licensePlate: vehicleProfile.licensePlate.trim() || 'JKT-02 / KMR 204',
      emergencyContact: vehicleProfile.emergencyContact.trim() || '0812-9999-8888',
      role: 'MEMBER',
      joinedAtLabel: 'Baru bergabung',
      isCurrentUser: true,
      isSweeper: false,
      avatarPreset: vehicleProfile.avatarPreset,
      avatarPhotoUrl: vehicleProfile.avatarPhotoUrl,
      conditionStatus: 'AMAN',
      connectionStatus: 'CONNECTED',
      lastLocationText: gps.lastFix ? 'GPS Live' : 'Area Masjidil Haram',
      lastUpdatedLabel: 'Baru saja',
      routeDistanceMeters: 690,
      offRouteMeters: 0,
      convoyOrderRank: 3,
      mapPosition: gps.lastFix
        ? {
            routeProgress: 0.4,
            xFraction: 0.45,
            yFraction: 0.55,
            latitude: gps.lastFix.latitude,
            longitude: gps.lastFix.longitude,
            speedKmh: gps.lastFix.speedKmh,
            heading: gps.lastFix.heading,
            accuracyMeters: gps.lastFix.accuracyMeters,
          }
        : interpolateAlongPolyline(0.58, routeConfig.waypoints, 0, 12),
    };

    const peer1: ConvoyMember = {
      id: 'peer-1',
      fullName: peer1Tpl.fullName,
      vehicleType: peer1Tpl.vehicleType,
      vehicleDetail: peer1Tpl.vehicleDetail,
      licensePlate: peer1Tpl.licensePlate,
      emergencyContact: peer1Tpl.emergencyContact,
      role: 'MEMBER',
      joinedAtLabel: 'Aktif',
      isCurrentUser: false,
      isSweeper: true,
      avatarPreset: peer1Tpl.avatarPreset,
      conditionStatus: peer1Tpl.conditionStatus,
      connectionStatus: peer1Tpl.connectionStatus,
      lastLocationText: peer1Tpl.lastLocationText,
      lastUpdatedLabel: '1 dtk lalu',
      routeDistanceMeters: 790,
      offRouteMeters: 0,
      convoyOrderRank: 2,
      mapPosition: interpolateAlongPolyline(0.66, routeConfig.waypoints, 0, 5),
    };

    const peer2: ConvoyMember = {
      id: 'peer-2',
      fullName: peer2Tpl.fullName,
      vehicleType: peer2Tpl.vehicleType,
      vehicleDetail: peer2Tpl.vehicleDetail,
      licensePlate: peer2Tpl.licensePlate,
      emergencyContact: peer2Tpl.emergencyContact,
      role: 'MEMBER',
      joinedAtLabel: 'Aktif',
      isCurrentUser: false,
      isSweeper: false,
      avatarPreset: peer2Tpl.avatarPreset,
      conditionStatus: peer2Tpl.conditionStatus,
      connectionStatus: peer2Tpl.connectionStatus,
      lastLocationText: peer2Tpl.lastLocationText,
      lastUpdatedLabel: '5 dtk lalu',
      routeDistanceMeters: 610,
      offRouteMeters: 0,
      convoyOrderRank: 4,
      mapPosition: interpolateAlongPolyline(0.51, routeConfig.waypoints, 0, 4),
    };

    setJoinError(null);
    setIsOffRouteSimActive(false);
    setIsStragglerSimActive(false);
    setIsLeaderTroubleSimActive(false);
    setSosState({
      isActive: false,
      senderId: '',
      senderName: '',
      senderVehicle: '',
      latitude: 0,
      longitude: 0,
      locationText: '',
      triggeredAtLabel: '',
    });
    setMessages(createInitialMessages(code));
    const room: TourRoom = {
      roomId: code,
      roomTitle: `Rombongan Bersama ${code} (Demo)`,
      inviteLink: `https://safartracking.id/join/${code}`,
      createdAtLabel: 'Terhubung Demo',
      isSyncedRoom: false,
      leaderUid: hostLeader.id,
      actingLeaderUid: hostLeader.id,
      leaderTroubleActive: false,
      members: [hostLeader, selfMember, peer1, peer2],
    };
    setSelectedMemberId(selfMember.id);
    recordedTrackRef.current = gps.lastFix
      ? [{ lat: gps.lastFix.latitude, lng: gps.lastFix.longitude }]
      : [];
    setLastCompletedTrip(null);
    setRoomActivities(createStarterRoomUmrahActivities(hostLeader.id));
    setRoomActivityProgress({});
    commitRoomMembers(room.members, routeConfig, room);
    navigateTo('live_map', 1);
  };

  // --- Stage 4 Handlers ---
  const handleTriggerSos = () => {
    if (!activeRoom) return;
    const self =
      activeRoom.members.find((m) => m.isCurrentUser) || activeRoom.members[0];
    if (!self) return;
    const now = currentClock();
    const lat = gps.lastFix?.latitude ?? self.mapPosition.latitude;
    const lng = gps.lastFix?.longitude ?? self.mapPosition.longitude;

    if (activeRoom.isSyncedRoom && authUser) {
      triggerSyncedSos(activeRoom.roomId, {
        uid: authUser.uid,
        fullName: self.fullName,
        vehicleDetail: self.vehicleDetail,
        latitude: lat,
        longitude: lng,
        locationText: self.lastLocationText,
        triggeredAtLabel: now,
      });
      sendRoomMessage(activeRoom.roomId, {
        id: `sos-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: self.fullName,
        senderVehicle: self.vehicleDetail,
        senderAvatarPreset: self.avatarPreset,
        type: 'SOS_ALERT',
        textContent: `SINYAL DARURAT SOS di ${self.lastLocationText} (${lat.toFixed(4)}, ${lng.toFixed(4)}). Butuh bantuan segera!`,
      });
      return;
    }

    const updated = activeRoom.members.map((m) =>
      m.id === self.id ? { ...m, conditionStatus: 'SOS' as MemberConditionStatus } : m
    );
    setSosState({
      isActive: true,
      senderId: self.id,
      senderName: self.fullName,
      senderVehicle: self.vehicleDetail,
      latitude: lat,
      longitude: lng,
      locationText: self.lastLocationText,
      triggeredAtLabel: now,
    });
    setSelectedMemberId(self.id);
    setMessages((prev) => [
      ...prev,
      {
        id: `sos-${Date.now()}`,
        senderId: self.id,
        senderName: self.fullName,
        senderVehicle: self.vehicleDetail,
        type: 'SOS_ALERT',
        textContent: `SINYAL DARURAT SOS di ${self.lastLocationText} (${lat.toFixed(4)}, ${lng.toFixed(4)}). Butuh bantuan segera!`,
        timestampLabel: now,
        isFromCurrentUser: true,
      },
    ]);
    commitRoomMembers(updated);
  };

  const handleResolveSos = () => {
    if (!activeRoom) return;
    const senderId = sosState.senderId;

    if (activeRoom.isSyncedRoom && authUser) {
      resolveSyncedSos(activeRoom.roomId, senderId);
      sendRoomMessage(activeRoom.roomId, {
        id: `sys-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: 'Sistem Rombongan',
        senderVehicle: '',
        senderAvatarPreset: 'RIDER_CAPTAIN',
        type: 'SYSTEM',
        textContent: 'Sinyal darurat SOS telah diselesaikan.',
      });
      return;
    }

    const updated = activeRoom.members.map((m) =>
      m.id === senderId && m.conditionStatus === 'SOS'
        ? { ...m, conditionStatus: 'AMAN' as MemberConditionStatus }
        : m
    );
    setSosState({
      isActive: false,
      senderId: '',
      senderName: '',
      senderVehicle: '',
      latitude: 0,
      longitude: 0,
      locationText: '',
      triggeredAtLabel: '',
    });
    setMessages((prev) => [
      ...prev,
      {
        id: `sys-${Date.now()}`,
        senderId: 'system',
        senderName: 'Sistem Rombongan',
        senderVehicle: '',
        type: 'SYSTEM',
        textContent: 'Sinyal darurat SOS telah diselesaikan.',
        timestampLabel: currentClock(),
      },
    ]);
    commitRoomMembers(updated);
  };

  // Audible triple-chime & browser notification for new priority broadcasts
  useEffect(() => {
    const broadcast = activeRoom?.priorityBroadcast;
    if (!broadcast?.isActive || !broadcast.id) return;
    if (lastAlertedBroadcastIdRef.current === broadcast.id) return;
    lastAlertedBroadcastIdRef.current = broadcast.id;
    setDismissedBroadcastId((prev) => (prev === broadcast.id ? '' : prev));

    if (routeConfigRef.current.soundAlertEnabled) {
      try {
        const AudioCtx =
          window.AudioContext ||
          (window as unknown as { webkitAudioContext: typeof AudioContext })
            .webkitAudioContext;
        if (AudioCtx) {
          const ctx = new AudioCtx();
          [0, 0.18, 0.36].forEach((offset, idx) => {
            const osc = ctx.createOscillator();
            const gain = ctx.createGain();
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(idx === 2 ? 987.77 : 783.99, ctx.currentTime + offset);
            gain.gain.setValueAtTime(0.11, ctx.currentTime + offset);
            gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + offset + 0.16);
            osc.connect(gain);
            gain.connect(ctx.destination);
            osc.start(ctx.currentTime + offset);
            osc.stop(ctx.currentTime + offset + 0.16);
          });
        }
      } catch {
        // Ignore AudioContext restrictions
      }
    }

    if (
      typeof window !== 'undefined' &&
      'Notification' in window &&
      Notification.permission === 'granted'
    ) {
      try {
        new Notification(`Pengumuman Rombongan: ${broadcast.title}`, {
          body: `${broadcast.message} — ${broadcast.senderName}`,
          tag: `safar-broadcast-${broadcast.id}`,
        });
      } catch {
        // Ignore Notification errors
      }
    }
  }, [activeRoom?.priorityBroadcast?.id, activeRoom?.priorityBroadcast?.isActive]);

  const handleSendTextMessage = (text: string) => {
    if (!activeRoom || !text.trim()) return;
    const self = activeRoom.members.find((m) => m.isCurrentUser);
    const trimmed = text.trim();
    const msgId = `txt-${Date.now()}`;
    if (activeRoom.isSyncedRoom && authUser) {
      const msgPayload = {
        id: msgId,
        senderUid: authUser.uid,
        senderName: self?.fullName ?? vehicleProfile.fullName ?? 'Jamaah',
        senderVehicle: self?.vehicleDetail ?? vehicleProfile.vehicleDetail ?? 'Peserta',
        senderAvatarPreset: self?.avatarPreset ?? vehicleProfile.avatarPreset,
        type: 'TEXT' as const,
        textContent: trimmed,
      };
      if (typeof navigator !== 'undefined' && navigator.onLine === false) {
        const count = enqueueRoomMessage({
          roomId: activeRoom.roomId,
          msg: msgPayload,
        });
        setOfflineQueueCount(count);
        setMessages((prev) => [
          ...prev,
          {
            id: msgId,
            senderId: authUser.uid,
            senderName: msgPayload.senderName,
            senderVehicle: msgPayload.senderVehicle,
            type: 'TEXT',
            textContent: `${trimmed} (menunggu sinyal)`,
            timestampLabel: currentClock(),
            isFromCurrentUser: true,
            senderAvatarPreset: msgPayload.senderAvatarPreset,
          },
        ]);
        return;
      }
      sendRoomMessage(activeRoom.roomId, msgPayload).catch(() => {
        const count = enqueueRoomMessage({
          roomId: activeRoom.roomId,
          msg: msgPayload,
        });
        setOfflineQueueCount(count);
      });
      return;
    }
    setMessages((prev) => [
      ...prev,
      {
        id: msgId,
        senderId: self?.id ?? 'self',
        senderName: self?.fullName ?? 'Anda',
        senderVehicle: self?.vehicleDetail ?? 'Peserta',
        type: 'TEXT',
        textContent: trimmed,
        timestampLabel: currentClock(),
        isFromCurrentUser: true,
        senderAvatarPreset: self?.avatarPreset ?? vehicleProfile.avatarPreset,
        senderAvatarPhotoUrl: self?.avatarPhotoUrl ?? vehicleProfile.avatarPhotoUrl,
      },
    ]);
  };

  const handleSendPriorityBroadcast = async (title: string, message: string) => {
    if (!activeRoom) return;
    const cleanTitle = title.trim();
    const cleanMessage = message.trim();
    if (!cleanTitle || !cleanMessage) return;

    const self = activeRoom.members.find((m) => m.isCurrentUser);
    const id = `brd-${Date.now()}`;
    const atLabel = currentClock();
    const senderName =
      self?.fullName ?? vehicleProfile.fullName ?? localUserName ?? 'Mutawwif';
    const senderVehicle =
      self?.vehicleDetail ?? vehicleProfile.vehicleDetail ?? 'Pembimbing Rombongan';
    const senderAvatarPreset = self?.avatarPreset ?? vehicleProfile.avatarPreset;

    // Optimistic local state update so UI reflects immediately even when offline
    setActiveRoom((prev) =>
      prev
        ? {
            ...prev,
            priorityBroadcast: {
              isActive: true,
              id,
              title: cleanTitle,
              message: cleanMessage,
              senderName,
              atLabel,
            },
          }
        : prev
    );

    if (activeRoom.isSyncedRoom && authUser) {
      const payload = {
        id,
        senderUid: authUser.uid,
        senderName,
        senderVehicle,
        senderAvatarPreset,
        title: cleanTitle,
        message: cleanMessage,
        atLabel,
      };
      if (typeof navigator !== 'undefined' && navigator.onLine === false) {
        const count = enqueuePriorityBroadcast({
          roomId: activeRoom.roomId,
          payload,
        });
        setOfflineQueueCount(count);
        return;
      }
      try {
        await sendPriorityRoomBroadcast(activeRoom.roomId, payload);
      } catch {
        const count = enqueuePriorityBroadcast({
          roomId: activeRoom.roomId,
          payload,
        });
        setOfflineQueueCount(count);
      }
      return;
    }

    setMessages((prev) => [
      ...prev,
      {
        id,
        senderId: self?.id ?? 'leader',
        senderName,
        senderVehicle,
        type: 'BROADCAST',
        textContent: `[PENGUMUMAN] ${cleanTitle}: ${cleanMessage}`,
        timestampLabel: atLabel,
        isFromCurrentUser: true,
        senderAvatarPreset,
        senderAvatarPhotoUrl: self?.avatarPhotoUrl ?? vehicleProfile.avatarPhotoUrl,
      },
    ]);
  };

  const handleClearPriorityBroadcast = async () => {
    if (!activeRoom) return;
    setActiveRoom((prev) =>
      prev
        ? {
            ...prev,
            priorityBroadcast: {
              isActive: false,
              id: prev.priorityBroadcast?.id ?? '',
              title: '',
              message: '',
              senderName: '',
              atLabel: '',
            },
          }
        : prev
    );
    if (activeRoom.isSyncedRoom && authUser) {
      clearPriorityRoomBroadcast(activeRoom.roomId).catch(() => {});
    }
  };

  const showVoiceStatus = (msg: string) => {
    setVoiceStatusMessage(msg);
    if (voiceErrorTimerRef.current) {
      clearTimeout(voiceErrorTimerRef.current);
    }
    voiceErrorTimerRef.current = setTimeout(() => {
      setVoiceStatusMessage((prev) => (prev === msg ? null : prev));
    }, 4500);
  };

  useEffect(() => {
    if (!isRecordingVoice) return;
    const id = setInterval(() => {
      const elapsed = Math.floor((Date.now() - voiceStartMsRef.current) / 1000);
      setRecordingSeconds(elapsed);
      if (elapsed >= 28) {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
          mediaRecorderRef.current.stop();
        }
      }
    }, 500);
    return () => clearInterval(id);
  }, [isRecordingVoice]);

  const blobToAudioDataUrl = (blob: Blob): Promise<string | undefined> =>
    new Promise((resolve) => {
      const reader = new FileReader();
      reader.onload = () => {
        const res = typeof reader.result === 'string' ? reader.result : '';
        resolve(res.startsWith('data:audio/') ? res : undefined);
      };
      reader.onerror = () => resolve(undefined);
      reader.readAsDataURL(blob);
    });

  const handleStartVoiceRecord = async () => {
    if (isRecordingVoice) return;
    setVoiceStatusMessage(null);
    setRecordingSeconds(0);
    audioChunksRef.current = [];

    if (
      typeof navigator === 'undefined' ||
      !navigator.mediaDevices?.getUserMedia ||
      typeof MediaRecorder === 'undefined'
    ) {
      showVoiceStatus('Browser/perangkat tidak mendukung rekaman audio mikrofon.');
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const preferredTypes = [
        'audio/mp4',
        'audio/webm;codecs=opus',
        'audio/webm',
        'audio/ogg;codecs=opus',
      ];
      const chosenMime =
        preferredTypes.find(
          (t) =>
            typeof MediaRecorder.isTypeSupported === 'function' &&
            MediaRecorder.isTypeSupported(t)
        ) ?? '';
      recordedMimeTypeRef.current = chosenMime || 'audio/webm';

      const recorderOptions: MediaRecorderOptions = {
        audioBitsPerSecond: 24000,
      };
      if (chosenMime) {
        recorderOptions.mimeType = chosenMime;
      }

      const recorder = new MediaRecorder(stream, recorderOptions);
      recordedMimeTypeRef.current =
        recorder.mimeType || recordedMimeTypeRef.current;

      recorder.ondataavailable = (ev) => {
        if (ev.data && ev.data.size > 0) {
          audioChunksRef.current.push(ev.data);
        }
      };
      recorder.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop());
        const elapsedSec = Math.max(
          1,
          Math.round((Date.now() - voiceStartMsRef.current) / 1000)
        );
        const rawMime =
          recordedMimeTypeRef.current.split(';')[0] || 'audio/webm';
        const blob = new Blob(audioChunksRef.current, { type: rawMime });
        audioChunksRef.current = [];
        mediaRecorderRef.current = null;

        if (blob.size < 400) {
          setIsRecordingVoice(false);
          setRecordingSeconds(0);
          showVoiceStatus(
            'Rekaman suara terlalu singkat atau kosong. Tahan tombol lebih lama.'
          );
          return;
        }

        const dataUrl = await blobToAudioDataUrl(blob);
        if (dataUrl && dataUrl.length > 345_000) {
          setIsRecordingVoice(false);
          setRecordingSeconds(0);
          showVoiceStatus(
            'Rekaman terlalu panjang (maks 28 detik). Ulangi dengan pesan lebih singkat.'
          );
          return;
        }

        const localObjectUrl = URL.createObjectURL(blob);
        finalizeVoiceMessage(localObjectUrl, dataUrl, elapsedSec);
      };

      voiceStartMsRef.current = Date.now();
      setIsRecordingVoice(true);
      mediaRecorderRef.current = recorder;
      recorder.start(250);
    } catch {
      mediaRecorderRef.current = null;
      setIsRecordingVoice(false);
      setRecordingSeconds(0);
      showVoiceStatus(
        'Izin mikrofon ditolak atau tidak tersedia. Aktifkan izin mikrofon di browser.'
      );
    }
  };

  const finalizeVoiceMessage = (
    localAudioUrl?: string,
    audioDataUrl?: string,
    durationSec?: number
  ) => {
    const self = activeRoom?.members.find((m) => m.isCurrentUser);
    const dur = Math.max(1, durationSec ?? recordingSeconds);
    setIsRecordingVoice(false);
    setRecordingSeconds(0);

    if (activeRoom?.isSyncedRoom && authUser) {
      if (!audioDataUrl) {
        showVoiceStatus('Audio rekaman kosong sehingga tidak dikirim.');
        return;
      }
      sendRoomMessage(activeRoom.roomId, {
        id: `voice-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: self?.fullName ?? vehicleProfile.fullName ?? 'Jamaah',
        senderVehicle:
          self?.vehicleDetail ?? vehicleProfile.vehicleDetail ?? 'Peserta',
        senderAvatarPreset: self?.avatarPreset ?? vehicleProfile.avatarPreset,
        type: 'VOICE',
        textContent: `Pesan Suara PTT (${dur} detik)`,
        voiceDurationSec: dur,
        voiceAudioDataUrl: audioDataUrl,
      });
      return;
    }

    const playableUrl = localAudioUrl ?? audioDataUrl;
    if (!playableUrl) {
      showVoiceStatus('Audio rekaman kosong sehingga tidak dikirim.');
      return;
    }

    setMessages((prev) => [
      ...prev,
      {
        id: `voice-${Date.now()}`,
        senderId: self?.id ?? 'self',
        senderName: self?.fullName ?? 'Anda',
        senderVehicle: self?.vehicleDetail ?? 'Peserta',
        type: 'VOICE',
        textContent: 'Pesan Suara Walkie-Talkie',
        timestampLabel: currentClock(),
        voiceDurationSec: dur,
        voiceAudioUrl: playableUrl,
        isFromCurrentUser: true,
        senderAvatarPreset: self?.avatarPreset ?? vehicleProfile.avatarPreset,
        senderAvatarPhotoUrl:
          self?.avatarPhotoUrl ?? vehicleProfile.avatarPhotoUrl,
      },
    ]);
  };

  const handleStopVoiceRecord = () => {
    if (!isRecordingVoice) return;
    if (
      mediaRecorderRef.current &&
      mediaRecorderRef.current.state !== 'inactive'
    ) {
      try {
        mediaRecorderRef.current.stop();
      } catch {
        setIsRecordingVoice(false);
        setRecordingSeconds(0);
      }
    } else {
      setIsRecordingVoice(false);
      setRecordingSeconds(0);
    }
  };

  const handleToggleVoicePlay = (msg: RoomMessage) => {
    if (playbackTimeoutRef.current) {
      clearTimeout(playbackTimeoutRef.current);
      playbackTimeoutRef.current = null;
    }
    if (audioPlayerRef.current) {
      audioPlayerRef.current.pause();
      audioPlayerRef.current = null;
    }

    if (playingMessageId === msg.id) {
      setPlayingMessageId(null);
      return;
    }

    if (!msg.voiceAudioUrl) {
      showVoiceStatus('File audio untuk pesan suara ini tidak tersedia.');
      setPlayingMessageId(null);
      return;
    }

    setPlayingMessageId(msg.id);
    const audio = new Audio(msg.voiceAudioUrl);
    audioPlayerRef.current = audio;
    audio.onended = () => setPlayingMessageId(null);
    audio.onerror = () => {
      setPlayingMessageId(null);
      showVoiceStatus('Gagal memutar rekaman suara. Format audio tidak didukung browser.');
    };
    audio.play().catch(() => {
      setPlayingMessageId(null);
      showVoiceStatus('Browser memblokir pemutaran audio otomatis. Ketuk tombol putar lagi.');
    });
  };

  const handleUpdateOwnCondition = (status: MemberConditionStatus) => {
    if (!activeRoom) return;
    const self = activeRoom.members.find((m) => m.isCurrentUser);
    if (!self) return;

    const labelMap: Record<MemberConditionStatus, string> = {
      AMAN: 'Aman / Sehat',
      ISTIRAHAT: 'Istirahat / Miqat',
      MOGOK: 'Perlu Bantuan',
      SOS: 'Darurat SOS',
    };

    if (activeRoom.isSyncedRoom && authUser) {
      updateSyncedMemberCondition(activeRoom.roomId, authUser.uid, status);
      sendRoomMessage(activeRoom.roomId, {
        id: `cond-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: self.fullName,
        senderVehicle: self.vehicleDetail,
        senderAvatarPreset: self.avatarPreset,
        type: 'SYSTEM',
        textContent: `${self.fullName} memperbarui kondisi: ${labelMap[status]}`,
      });
      return;
    }

    const updated = activeRoom.members.map((m) =>
      m.isCurrentUser ? { ...m, conditionStatus: status } : m
    );
    setMessages((prev) => [
      ...prev,
      {
        id: `cond-${Date.now()}`,
        senderId: self.id,
        senderName: self.fullName,
        senderVehicle: self.vehicleDetail,
        type: 'SYSTEM',
        textContent: `${self.fullName} memperbarui kondisi: ${labelMap[status]}`,
        timestampLabel: currentClock(),
      },
    ]);
    commitRoomMembers(updated);
  };

  const handleAssignSweeper = (memberId: string) => {
    if (!activeRoom) return;
    const target = activeRoom.members.find((m) => m.id === memberId);
    if (!target) return;

    if (activeRoom.isSyncedRoom && authUser) {
      assignSyncedSweeper(
        activeRoom.roomId,
        memberId,
        activeRoom.members.map((m) => m.id)
      );
      sendRoomMessage(activeRoom.roomId, {
        id: `swp-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: 'Mutawwif',
        senderVehicle: '',
        senderAvatarPreset: 'RIDER_CAPTAIN',
        type: 'SYSTEM',
        textContent: `${target.fullName} (${target.vehicleDetail}) ditetapkan sebagai Pendamping penutup rombongan.`,
      });
      return;
    }

    const updated = activeRoom.members.map((m) => ({
      ...m,
      isSweeper: m.id === memberId,
      isAutoSweeper: false,
    }));
    setMessages((prev) => [
      ...prev,
      {
        id: `swp-${Date.now()}`,
        senderId: 'system',
        senderName: 'Mutawwif',
        senderVehicle: '',
        type: 'SYSTEM',
        textContent: `${target.fullName} (${target.vehicleDetail}) ditetapkan sebagai Pendamping penutup rombongan.`,
        timestampLabel: currentClock(),
      },
    ]);
    commitRoomMembers(updated);
  };

  // --- Route & Checkpoint Handlers ---
  const syncMembersToRoute = (nextConfig: ConvoyRouteConfig) => {
    if (!activeRoom) return;
    if (activeRoom.isSyncedRoom) {
      commitRoomMembers(activeRoom.members, nextConfig);
      return;
    }
    const synced = activeRoom.members.map((member) => {
      if (member.isCurrentUser && gps.status === 'active') {
        return member;
      }
      return {
        ...member,
        mapPosition: interpolateAlongPolyline(
          member.mapPosition.routeProgress,
          nextConfig.waypoints,
          member.isCurrentUser && isOffRouteSimActive ? 0.12 : 0,
          member.mapPosition.speedKmh
        ),
      };
    });
    commitRoomMembers(synced, nextConfig);
  };

  const getCurrentStartPoint = (): RoutePoint =>
    routeConfig.waypoints[0] ??
    (gps.lastFix
      ? {
          xFraction: 0.14,
          yFraction: 0.86,
          latitude: gps.lastFix.latitude,
          longitude: gps.lastFix.longitude,
        }
      : {
          xFraction: 0.14,
          yFraction: 0.86,
          latitude: 21.418,
          longitude: 39.8275,
        });

  const applyDestinationStops = (
    baseConfig: ConvoyRouteConfig,
    nextStops: DestinationStop[],
    startOverride?: RoutePoint
  ) => {
    const safeStops = nextStops.slice(0, 10);
    const startPt = startOverride ?? baseConfig.waypoints[0] ?? getCurrentStartPoint();
    const lastStop = safeStops[safeStops.length - 1];
    const nextWaypoints = buildMultiStopPolyline(startPt, safeStops);
    const estMeters = estimateMultiStopMeters(startPt, safeStops);
    const nextConfig: ConvoyRouteConfig = {
      ...baseConfig,
      destinationName: lastStop?.name || baseConfig.destinationName,
      destinationAddress: lastStop?.address || baseConfig.destinationAddress,
      destinationStops: safeStops,
      waypoints: nextWaypoints,
      routeInstructions: [],
      totalRouteMeters: estMeters,
    };
    setRouteConfig(nextConfig);
    setIsPickingDestinationOnMap(false);
    syncMembersToRoute(nextConfig);
    if (
      activeRoom?.isSyncedRoom &&
      authUser &&
      isRoomEffectiveLeader(activeRoom, authUser.uid)
    ) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handlePickOriginLatLng = (
    lat: number,
    lng: number,
    placeName?: string,
    addressLabel?: string
  ) => {
    const startPt: RoutePoint = {
      xFraction: 0.14,
      yFraction: 0.86,
      latitude: lat,
      longitude: lng,
    };
    const stops = getEffectiveDestinationStops(routeConfig);
    const nextWaypoints = buildMultiStopPolyline(startPt, stops);
    const estMeters = estimateMultiStopMeters(startPt, stops);
    const resolvedName =
      placeName?.trim() || `Titik Kumpul (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
    const resolvedAddress =
      addressLabel?.trim() || `${resolvedName} · ${lat.toFixed(5)}, ${lng.toFixed(5)}`;

    const nextConfig: ConvoyRouteConfig = {
      ...routeConfig,
      originName: resolvedName,
      originAddress: resolvedAddress,
      originLockedByLeader: true,
      destinationStops: stops,
      waypoints: nextWaypoints,
      routeInstructions: [],
      totalRouteMeters: estMeters,
    };
    setRouteConfig(nextConfig);
    syncMembersToRoute(nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleUseGpsAsOrigin = () => {
    if (!gps.lastFix) {
      gps.retryGps();
      return;
    }
    const { latitude, longitude } = gps.lastFix;
    handlePickOriginLatLng(
      latitude,
      longitude,
      'Lokasi GPS Saya',
      `Posisi GPS Saat Ini · ${latitude.toFixed(5)}, ${longitude.toFixed(5)}`
    );
  };

  const handleSelectRoutePreference = (preference: RoutePreference) => {
    const nextConfig: ConvoyRouteConfig = {
      ...routeConfig,
      routePreference: preference,
      routeInstructions: [],
    };
    setRouteConfig(nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleSelectPreset = (preset: DestinationPreset) => {
    const coordStr = `${preset.endPoint.latitude.toFixed(4)}, ${preset.endPoint.longitude.toFixed(4)}`;
    const updatedStop: DestinationStop = {
      id: `stop-${Date.now()}`,
      name: preset.name,
      address: `${preset.name} · ${coordStr}`,
      latitude: preset.endPoint.latitude,
      longitude: preset.endPoint.longitude,
    };
    const existingStops = getEffectiveDestinationStops(routeConfig);
    const nextStops =
      existingStops.length > 1
        ? [...existingStops.slice(0, -1), { ...updatedStop, id: existingStops[existingStops.length - 1].id }]
        : [updatedStop];
    applyDestinationStops(routeConfig, nextStops);
  };

  const handlePickDestinationLatLng = (
    lat: number,
    lng: number,
    placeName?: string,
    addressLabel?: string
  ) => {
    const resolvedName =
      placeName?.trim() || `Titik Tujuan (${lat.toFixed(4)}, ${lng.toFixed(4)})`;
    const resolvedAddress =
      addressLabel?.trim() || `${resolvedName} · ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    const existingStops = getEffectiveDestinationStops(routeConfig);
    const newStop: DestinationStop = {
      id: existingStops[existingStops.length - 1]?.id || `stop-${Date.now()}`,
      name: resolvedName,
      address: resolvedAddress,
      latitude: lat,
      longitude: lng,
    };
    const nextStops =
      existingStops.length > 1
        ? [...existingStops.slice(0, -1), newStop]
        : [newStop];
    applyDestinationStops(routeConfig, nextStops);
  };

  const handleAddDestinationPoint = (
    lat: number,
    lng: number,
    placeName?: string,
    addressLabel?: string
  ) => {
    const existingStops = getEffectiveDestinationStops(routeConfig);
    if (existingStops.length >= 10) return;
    const resolvedName =
      placeName?.trim() || `Titik Tujuan #${existingStops.length + 1}`;
    const resolvedAddress =
      addressLabel?.trim() || `${resolvedName} · ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    const nextStop: DestinationStop = {
      id: `stop-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
      name: resolvedName,
      address: resolvedAddress,
      latitude: lat,
      longitude: lng,
    };
    applyDestinationStops(routeConfig, [...existingStops, nextStop]);
  };

  const handleRemoveDestinationPoint = (stopId: string) => {
    const existingStops = getEffectiveDestinationStops(routeConfig);
    if (existingStops.length <= 1) return;
    const filtered = existingStops.filter((s) => s.id !== stopId);
    if (filtered.length === 0) return;
    applyDestinationStops(routeConfig, filtered);
  };

  const handleMoveDestinationPoint = (
    stopId: string,
    direction: 'up' | 'down'
  ) => {
    const existingStops = [...getEffectiveDestinationStops(routeConfig)];
    const idx = existingStops.findIndex((s) => s.id === stopId);
    if (idx < 0) return;
    const targetIdx = direction === 'up' ? idx - 1 : idx + 1;
    if (targetIdx < 0 || targetIdx >= existingStops.length) return;
    const [moved] = existingStops.splice(idx, 1);
    existingStops.splice(targetIdx, 0, moved);
    applyDestinationStops(routeConfig, existingStops);
  };

  const handleRouteComputed = useCallback(
    (
      distanceMeters: number,
      durationMillis: number,
      path: LatLngPoint[],
      instructions?: RouteInstruction[]
    ) => {
      if (path.length < 2 || distanceMeters <= 0) return;
      const sampled = downsampleRoutePoints(path, 60);
      const currentCfg = routeConfigRef.current;
      const distDiff = Math.abs(currentCfg.totalRouteMeters - distanceMeters);
      const stopsSig = (currentCfg.destinationStops || [])
        .map((s) => `${s.latitude.toFixed(4)},${s.longitude.toFixed(4)}`)
        .join(';');
      const sig = `${currentCfg.originName ?? ''}|${currentCfg.destinationName}|${stopsSig}|${currentCfg.routePreference ?? 'fastest'}|${Math.round(distanceMeters / 50)}|${sampled.length}`;
      const hasNewInstructions =
        Boolean(instructions && instructions.length > 0) &&
        (!currentCfg.routeInstructions || currentCfg.routeInstructions.length === 0);
      if (sig === lastSyncedRouteSigRef.current && distDiff < 80 && !hasNewInstructions) {
        return;
      }

      lastSyncedRouteSigRef.current = sig;
      const nextConfig: ConvoyRouteConfig = {
        ...currentCfg,
        waypoints: sampled,
        totalRouteMeters: distanceMeters,
        estimatedDurationMillis:
          durationMillis > 0 ? durationMillis : currentCfg.estimatedDurationMillis,
        routeInstructions:
          instructions && instructions.length > 0
            ? instructions
            : currentCfg.routeInstructions,
      };
      setRouteConfig(nextConfig);

      const room = activeRoomRef.current;
      const user = authUserRef.current;
      if (room) {
        const evaluated = evaluateRoomSafety(room.members, nextConfig, {
          originalLeaderUid: room.leaderUid,
          actingLeaderUid: room.actingLeaderUid,
          leaderTroubleActive: room.leaderTroubleActive,
        });
        setOffRouteAlert(evaluated.offRouteAlert);
        setStragglerAlert(evaluated.stragglerAlert);
        setActiveRoom((prev) =>
          prev ? { ...prev, members: evaluated.members } : prev
        );
        if (room.isSyncedRoom && user && isRoomEffectiveLeader(room, user.uid)) {
          updateSyncedRoomRoute(room.roomId, nextConfig).catch(() => {});
        }
      }
    },
    []
  );

  // --- Rest Area Checkpoint Handlers ---
  const handleStartPickCheckpoint = (name: string) => {
    setIsPickingDestinationOnMap(false);
    setPendingCheckpointName(name.trim());
    setIsPickingCheckpointOnMap(true);
  };

  const handleCancelPickCheckpoint = () => {
    setIsPickingCheckpointOnMap(false);
    setPendingCheckpointName('');
  };

  const handleConfirmAddCheckpointLatLng = (
    name: string,
    lat: number,
    lng: number
  ) => {
    const finalName = name.trim() || 'Pos Kumpul / Miqat';
    const newCheckpoint: RestCheckpoint = {
      id: `cp-${Date.now()}`,
      name: finalName,
      xFraction: 0.5,
      yFraction: 0.5,
      latitude: lat,
      longitude: lng,
    };
    setRouteConfig((prev) => ({
      ...prev,
      checkpoints: [...prev.checkpoints, newCheckpoint],
    }));
    setIsPickingCheckpointOnMap(false);
    setPendingCheckpointName('');

    if (activeRoom?.isSyncedRoom && authUser) {
      addRoomCheckpoint(activeRoom.roomId, authUser.uid, newCheckpoint);
      sendRoomMessage(activeRoom.roomId, {
        id: `cp-msg-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: 'Mutawwif',
        senderVehicle: '',
        senderAvatarPreset: 'RIDER_CAPTAIN',
        type: 'SYSTEM',
        textContent: `Pos kumpul/istirahat baru dibagikan ke seluruh rombongan: ${finalName}.`,
      });
      return;
    }

    setMessages((prev) => [
      ...prev,
      {
        id: `cp-msg-${Date.now()}`,
        senderId: 'system',
        senderName: 'Mutawwif',
        senderVehicle: '',
        type: 'SYSTEM',
        textContent: `Pos kumpul/istirahat baru dibagikan ke seluruh rombongan: ${finalName}.`,
        timestampLabel: currentClock(),
      },
    ]);
  };

  const handleAddCheckpointFromStepper = (
    name: string,
    xFraction: number,
    yFraction: number
  ) => {
    const wps = routeConfig.waypoints;
    const start = wps[0] ?? { latitude: 21.418, longitude: 39.8275 };
    const end = wps[wps.length - 1] ?? { latitude: 21.4225, longitude: 39.8262 };
    const factor = Math.max(0.1, Math.min(0.9, xFraction));
    const lat = start.latitude + (end.latitude - start.latitude) * factor;
    const lng = start.longitude + (end.longitude - start.longitude) * factor;
    const cp: RestCheckpoint = {
      id: `cp-${Date.now()}`,
      name: name.trim() || 'Pos Kumpul / Miqat',
      xFraction: factor,
      yFraction,
      latitude: lat,
      longitude: lng,
    };
    setRouteConfig((prev) => ({
      ...prev,
      checkpoints: [...prev.checkpoints, cp],
    }));
    if (activeRoom?.isSyncedRoom && authUser) {
      addRoomCheckpoint(activeRoom.roomId, authUser.uid, cp);
    }
  };

  const handleRemoveCheckpoint = (checkpointId: string) => {
    setRouteConfig((prev) => ({
      ...prev,
      checkpoints: prev.checkpoints.filter((cp) => cp.id !== checkpointId),
    }));
    if (activeRoom?.isSyncedRoom && authUser) {
      removeRoomCheckpoint(activeRoom.roomId, checkpointId);
    }
  };

  const handleUpdateWaypoints = (nextWaypoints: RoutePoint[]) => {
    if (nextWaypoints.length < 2) return;
    const nextConfig: ConvoyRouteConfig = {
      ...routeConfig,
      waypoints: nextWaypoints,
    };
    setRouteConfig(nextConfig);
    syncMembersToRoute(nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleResetDefaultWaypoints = () => {
    const first = routeConfig.waypoints[0];
    const stops = getEffectiveDestinationStops(routeConfig);
    const keptEndpoints =
      first && stops.length >= 1
        ? buildMultiStopPolyline(first, stops)
        : INITIAL_ROUTE_CONFIG.waypoints;
    const nextConfig: ConvoyRouteConfig = {
      ...routeConfig,
      destinationStops: stops,
      waypoints: keptEndpoints,
      routeInstructions: [],
    };
    setRouteConfig(nextConfig);
    syncMembersToRoute(nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleUpdateMaxStraggler = (meters: number) => {
    const nextConfig = { ...routeConfig, maxStragglerDistanceMeters: meters };
    setRouteConfig(nextConfig);
    if (activeRoom) commitRoomMembers(activeRoom.members, nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleUpdateOffRouteTolerance = (meters: number) => {
    const nextConfig = { ...routeConfig, offRouteToleranceMeters: meters };
    setRouteConfig(nextConfig);
    if (activeRoom) commitRoomMembers(activeRoom.members, nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleToggleSoundAlert = () => {
    const nextConfig = {
      ...routeConfig,
      soundAlertEnabled: !routeConfig.soundAlertEnabled,
    };
    setRouteConfig(nextConfig);
    if (activeRoom?.isSyncedRoom && authUser && isRoomEffectiveLeader(activeRoom, authUser.uid)) {
      updateSyncedRoomRoute(activeRoom.roomId, nextConfig);
    }
  };

  const handleToggleOffRouteSim = () => {
    if (activeRoom?.isSyncedRoom) return;
    const next = !isOffRouteSimActive;
    setIsOffRouteSimActive(next);
    if (!activeRoom) return;
    const updated = activeRoom.members.map((member) =>
      member.isCurrentUser
        ? {
            ...member,
            mapPosition: interpolateAlongPolyline(
              member.mapPosition.routeProgress,
              routeConfig.waypoints,
              next ? 0.12 : 0,
              member.mapPosition.speedKmh
            ),
          }
        : member
    );
    commitRoomMembers(updated);
  };

  const handleToggleStragglerSim = () => {
    if (activeRoom?.isSyncedRoom) return;
    const next = !isStragglerSimActive;
    setIsStragglerSimActive(next);
    if (!activeRoom) return;
    const ordered = [...activeRoom.members].sort(
      (a, b) => b.mapPosition.routeProgress - a.mapPosition.routeProgress
    );
    const tailId = ordered[ordered.length - 1]?.id;
    if (!tailId) return;

    const updated = activeRoom.members.map((member) => {
      if (member.id !== tailId) return member;
      const targetProgress = next ? 0.18 : 0.56;
      return {
        ...member,
        mapPosition: interpolateAlongPolyline(
          targetProgress,
          routeConfig.waypoints,
          0,
          next ? 28 : 52
        ),
        lastLocationText: landmarkForProgress(targetProgress),
      };
    });
    commitRoomMembers(updated);
  };

  const handleReclaimLeader = () => {
    const room = activeRoomRef.current;
    if (!room) return;
    setIsLeaderTroubleSimActive(false);

    if (room.isSyncedRoom && authUser && room.leaderUid === authUser.uid) {
      recoverOriginalLeader(room.roomId, authUser.uid).catch(() => {});
      sendRoomMessage(room.roomId, {
        id: `recover-${Date.now()}`,
        senderUid: authUser.uid,
        senderName: 'Mutawwif',
        senderVehicle: '',
        senderAvatarPreset: 'RIDER_CAPTAIN',
        type: 'SYSTEM',
        textContent:
          'HP Mutawwif kembali normal. Wewenang rute & navigasi dikembalikan ke Mutawwif utama.',
      }).catch(() => {});
      return;
    }

    const origLeaderId =
      room.leaderUid ||
      room.members.find((m) => m.role === 'TOUR_LEADER')?.id ||
      '';
    const updatedMembers = room.members.map((m) =>
      m.id === origLeaderId
        ? {
            ...m,
            connectionStatus: 'CONNECTED' as ConnectionStatus,
            lastLocationText: 'GPS Mutawwif Pulih',
            lastUpdatedLabel: 'Baru saja',
          }
        : m
    );
    const updatedRoom: TourRoom = {
      ...room,
      leaderTroubleActive: false,
      leaderTroubleReason: '',
      actingLeaderUid: origLeaderId,
      members: updatedMembers,
    };
    setMessages((prev) => [
      ...prev,
      {
        id: `recover-${Date.now()}`,
        senderId: 'system',
        senderName: 'Sistem Rombongan',
        senderVehicle: '',
        type: 'SYSTEM',
        textContent:
          'HP Mutawwif kembali aktif. Kepemimpinan & ping rute kembali ke Mutawwif utama.',
        timestampLabel: currentClock(),
      },
    ]);
    commitRoomMembers(updatedMembers, routeConfigRef.current, updatedRoom);
  };

  const handleToggleLeaderTroubleSim = () => {
    const room = activeRoomRef.current;
    if (!room || room.isSyncedRoom) return;
    if (isLeaderTroubleSimActive || room.leaderTroubleActive) {
      handleReclaimLeader();
      return;
    }

    const origLeader =
      room.members.find((m) => m.id === room.leaderUid) ||
      room.members.find((m) => m.role === 'TOUR_LEADER');
    if (!origLeader) return;

    const closest = computeClosestCoLeader(room.members, origLeader.id);
    const promotedId = closest.coLeaderUid;
    if (!promotedId) return;
    const promotedMember = room.members.find((m) => m.id === promotedId);

    setIsLeaderTroubleSimActive(true);
    const updatedMembers = room.members.map((m) =>
      m.id === origLeader.id
        ? {
            ...m,
            connectionStatus: 'OFFLINE' as ConnectionStatus,
            lastLocationText: 'Sinyal/Baterai Mutawwif Terputus',
            lastUpdatedLabel: 'Terputus',
          }
        : m
    );
    const updatedRoom: TourRoom = {
      ...room,
      leaderUid: origLeader.id,
      coLeaderUid: promotedId,
      actingLeaderUid: promotedId,
      leaderTroubleActive: true,
      leaderTroubleReason: 'Sinyal/Baterai HP Mutawwif terputus',
      members: updatedMembers,
    };
    setMessages((prev) => [
      ...prev,
      {
        id: `failover-sim-${Date.now()}`,
        senderId: 'system',
        senderName: 'Sistem Wakil',
        senderVehicle: promotedMember?.vehicleDetail || '',
        type: 'SYSTEM',
        textContent: `HP Mutawwif mengalami kendala sinyal/baterai! Wakil Pembimbing terdekat (${promotedMember?.fullName || 'Jamaah'}, ${closest.distanceByMemberId.get(promotedId) ?? 0} m) otomatis menggantikan Pembimbing & mengambil alih ping rute.`,
        timestampLabel: currentClock(),
      },
    ]);
    commitRoomMembers(updatedMembers, routeConfigRef.current, updatedRoom);
  };

  const handleToggleMissingPilgrimSim = () => {
    const room = activeRoomRef.current;
    if (!room || room.isSyncedRoom) return;
    const targetMember =
      room.members.find((m) => !m.isCurrentUser) || room.members[0];
    if (!targetMember) return;

    const nextActive = !isMissingPilgrimSimActive;
    setIsMissingPilgrimSimActive(nextActive);
    const updatedMembers = room.members.map((m) =>
      m.id === targetMember.id
        ? {
            ...m,
            connectionStatus: nextActive
              ? ('OFFLINE' as ConnectionStatus)
              : ('CONNECTED' as ConnectionStatus),
            lastLocationText: nextActive
              ? 'Sinyal Hilang · Terakhir dekat Gate 79'
              : landmarkForProgress(m.mapPosition.routeProgress),
            lastUpdatedLabel: nextActive ? '2 mnt lalu' : 'Baru saja',
          }
        : m
    );
    commitRoomMembers(updatedMembers);
  };

  const handleGenerateFamilyLink = async () => {
    const room = activeRoomRef.current;
    if (!room || !room.isSyncedRoom || !authUser) return;
    const self = room.members.find((m) => m.isCurrentUser);
    setIsGeneratingFamilyLink(true);
    setFamilyLinkError(null);
    try {
      const created = await createOrGetFamilyInviteLink({
        roomId: room.roomId,
        roomTitle: room.roomTitle,
        targetUid: authUser.uid,
        targetName: self?.fullName || vehicleProfile.fullName || 'Jamaah',
        targetVehicleDetail: self?.vehicleDetail || vehicleProfile.vehicleDetail || '',
        targetAvatarPreset: self?.avatarPreset || vehicleProfile.avatarPreset,
        destinationName: routeConfig.destinationName,
      });
      setMyFamilyLink(created);
    } catch (err) {
      setFamilyLinkError(
        err instanceof Error ? err.message : 'Gagal membuat link pantauan keluarga.'
      );
    } finally {
      setIsGeneratingFamilyLink(false);
    }
  };

  const handleRegenerateFamilyLink = async () => {
    const room = activeRoomRef.current;
    if (!room || !room.isSyncedRoom || !authUser) return;
    const self = room.members.find((m) => m.isCurrentUser);
    setIsGeneratingFamilyLink(true);
    setFamilyLinkError(null);
    try {
      const recreated = await regenerateFamilyInviteLink({
        previousInviteCode: myFamilyLink?.inviteCode,
        roomId: room.roomId,
        roomTitle: room.roomTitle,
        targetUid: authUser.uid,
        targetName: self?.fullName || vehicleProfile.fullName || 'Jamaah',
        targetVehicleDetail: self?.vehicleDetail || vehicleProfile.vehicleDetail || '',
        targetAvatarPreset: self?.avatarPreset || vehicleProfile.avatarPreset,
        destinationName: routeConfig.destinationName,
      });
      setMyFamilyLink(recreated);
    } catch (err) {
      setFamilyLinkError(
        err instanceof Error ? err.message : 'Gagal membuat kode keluarga baru.'
      );
    } finally {
      setIsGeneratingFamilyLink(false);
    }
  };

  const handleRevokeFamilyLink = async () => {
    if (!myFamilyLink?.inviteCode) return;
    setIsGeneratingFamilyLink(true);
    setFamilyLinkError(null);
    try {
      await revokeFamilyInviteLink(myFamilyLink.inviteCode);
      setMyFamilyLink(null);
    } catch (err) {
      setFamilyLinkError(
        err instanceof Error ? err.message : 'Gagal memutus akses keluarga.'
      );
    } finally {
      setIsGeneratingFamilyLink(false);
    }
  };

  const handleFinishAndSaveTrip = (options?: { autoEndedByLeader?: boolean }) => {
    const autoEndedByLeader = Boolean(
      options &&
        typeof options === 'object' &&
        'autoEndedByLeader' in options &&
        options.autoEndedByLeader === true
    );
    const room = activeRoomRef.current ?? activeRoom;
    const cfg = routeConfigRef.current;
    const user = authUserRef.current;
    if (!room) {
      navigateTo('trip_history', 1);
      return;
    }

    const activeSpeeds = room.members
      .map((m) => m.mapPosition.speedKmh)
      .filter((spd) => spd > 0);
    const avgSpeed =
      activeSpeeds.length > 0
        ? Math.round(activeSpeeds.reduce((sum, s) => sum + s, 0) / activeSpeeds.length)
        : 52;
    const maxSpeed = (activeSpeeds.length > 0 ? Math.max(...activeSpeeds) : 64) + 12;

    const rawTrack = recordedTrackRef.current;
    const rawTrackMeters = computePolylineLengthMeters(rawTrack);
    const hasRealRecordedGpsTrack = rawTrack.length >= 2 && rawTrackMeters >= 50;

    const finalWaypoints: RoutePoint[] = hasRealRecordedGpsTrack
      ? downsampleRoutePoints(downsampleTrackPoints(rawTrack, 15, 120), 120)
      : cfg.waypoints;

    const realDistanceMeters = hasRealRecordedGpsTrack
      ? rawTrackMeters
      : cfg.totalRouteMeters;

    const distanceKm = Math.max(
      0.1,
      Number((realDistanceMeters / 1000).toFixed(1))
    );
    const estMinutes = Math.max(15, Math.round((distanceKm / Math.max(25, avgSpeed)) * 60));
    const hours = Math.floor(estMinutes / 60);
    const mins = estMinutes % 60;
    const durationLabel = hours > 0 ? `${hours}j ${mins}m` : `${mins} mnt`;

    const nowDate = new Date();
    const dateLabel = nowDate.toLocaleDateString('id-ID', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });

    const newRecord: TripHistoryRecord = {
      id: `trip-${Date.now()}`,
      roomId: room.roomId,
      tourTitle: room.roomTitle,
      dateLabel,
      timeRangeLabel: `Selesai ${currentClock()}`,
      durationLabel,
      durationMinutes: estMinutes,
      distanceKm,
      avgSpeedKmh: avgSpeed,
      maxSpeedKmh: maxSpeed,
      userRole: selectedRole,
      destinationName: cfg.destinationName,
      destinationAddress: cfg.destinationAddress,
      waypoints: finalWaypoints,
      visitedCheckpoints: cfg.checkpoints.map((cp, idx) => ({
        id: cp.id,
        name: cp.name,
        arrivedAtLabel: `Check #${idx + 1}`,
        xFraction: cp.xFraction,
        yFraction: cp.yFraction,
        latitude: cp.latitude,
        longitude: cp.longitude,
      })),
      participants: room.members.map((m) => ({
        id: m.id,
        fullName: m.fullName,
        vehicleType: m.vehicleType,
        vehicleDetail: m.vehicleDetail,
        licensePlate: m.licensePlate,
        role: m.role,
        isSweeper: m.isSweeper,
        avatarPreset: m.avatarPreset,
        avatarPhotoUrl: m.avatarPhotoUrl,
      })),
    };

    if (room.isSyncedRoom && user) {
      if (!autoEndedByLeader && isRoomEffectiveLeader(room, user.uid)) {
        finishSyncedRoom(room.roomId).catch(() => {});
      }
      saveTripRecordToFirestore(user.uid, newRecord).catch((err) =>
        console.warn('Gagal menyimpan riwayat ke Firestore:', err)
      );
    }

    recordedTrackRef.current = [];
    activeRoomRef.current = null;
    setTripHistory((prev) => [newRecord, ...prev]);
    setSelectedTripId(newRecord.id);
    setLastCompletedTrip(newRecord);
    setAutoFinishNotice(
      autoEndedByLeader
        ? 'Mutawwif telah mengakhiri safar. Perjalanan Anda otomatis selesai & tersimpan ke riwayat.'
        : null
    );
    setActiveRoom(null);
    setRoomActivities([]);
    setRoomActivityProgress({});
    setIsOffRouteSimActive(false);
    setIsStragglerSimActive(false);
    setIsLeaderTroubleSimActive(false);
    setSosState({
      isActive: false,
      senderId: '',
      senderName: '',
      senderVehicle: '',
      latitude: 0,
      longitude: 0,
      locationText: '',
      triggeredAtLabel: '',
    });
    navigateTo('trip_detail', 1);
  };
  handleFinishAndSaveTripRef.current = handleFinishAndSaveTrip;

  const handleRepeatTour = async (trip: TripHistoryRecord) => {
    if (!canCreateRoom) {
      setAuthError(
        'Hanya Ketua Rombongan yang emailnya terdaftar oleh Admin yang dapat membuat rombongan baru.'
      );
      return;
    }
    const restoredWaypoints =
      trip.waypoints.length >= 2 ? trip.waypoints : routeConfig.waypoints;
    const restoredCheckpoints: RestCheckpoint[] = trip.visitedCheckpoints.map(
      (cp, idx) => ({
        id: cp.id || `cp-repeat-${Date.now()}-${idx}`,
        name: cp.name,
        xFraction: cp.xFraction,
        yFraction: cp.yFraction,
        latitude: cp.latitude,
        longitude: cp.longitude,
      })
    );
    const endPoint = restoredWaypoints[restoredWaypoints.length - 1];
    const restoredConfig: ConvoyRouteConfig = {
      ...routeConfig,
      destinationName: trip.destinationName || routeConfig.destinationName,
      destinationAddress:
        trip.destinationAddress || routeConfig.destinationAddress,
      destinationStops: endPoint
        ? [
            {
              id: `stop-repeat-${Date.now()}`,
              name: trip.destinationName || routeConfig.destinationName,
              address: trip.destinationAddress || routeConfig.destinationAddress,
              latitude: endPoint.latitude,
              longitude: endPoint.longitude,
            },
          ]
        : routeConfig.destinationStops,
      waypoints: restoredWaypoints,
      checkpoints: restoredCheckpoints,
      totalRouteMeters: Math.max(1000, Math.round(trip.distanceKm * 1000)),
    };
    setSelectedRole('TOUR_LEADER');
    setRouteConfig(restoredConfig);
    setLastCompletedTrip(null);
    await handleCreateRoom(
      trip.tourTitle || 'Rombongan Ziarah Makkah',
      restoredConfig
    );
  };

  const handleResetDemo = () => {
    recordedTrackRef.current = [];
    setSelectedRole('TOUR_LEADER');
    setVehicleProfile(INITIAL_PROFILE);
    setActiveRoom(null);
    setDraftRoomActivities(createStarterRoomUmrahActivities('leader'));
    setRoomActivities([]);
    setRoomActivityProgress({});
    setManasikInitialTab('manasik');
    setLastCompletedTrip(null);
    setJoinInput('');
    setJoinError(null);
    setSelectedMemberId(null);
    setIsSimulationRunning(true);
    setRouteConfig(INITIAL_ROUTE_CONFIG);
    setMessages([]);
    setSosState({
      isActive: false,
      senderId: '',
      senderName: '',
      senderVehicle: '',
      latitude: 0,
      longitude: 0,
      locationText: '',
      triggeredAtLabel: '',
    });
    setIsPickingDestinationOnMap(false);
    setIsPickingCheckpointOnMap(false);
    setPendingCheckpointName('');
    setIsOffRouteSimActive(false);
    setIsStragglerSimActive(false);
    setTripHistory(DEFAULT_TRIP_HISTORY);
    setSelectedTripId(DEFAULT_TRIP_HISTORY[0]?.id ?? null);
    setAuthError(null);
    setNavDirection(-1);
    setCurrentRoute(authUser || localUserName ? 'role_selection' : 'login');
  };

  const handleMobileGoogleBridgeAuth = async () => {
    if (mobileBridgeBusy) return;
    setMobileBridgeBusy(true);
    setMobileBridgeError(null);
    setMobileBridgeDeepLink(null);
    setMobileBridgeCopied(false);
    try {
      const { user, idToken, redirected } = await signInWithGoogleForMobileBridge();
      if (redirected || !user) {
        return;
      }
      if (!idToken) {
        setMobileBridgeError(
          'Token verifikasi Google tidak ditemukan. Silakan ulangi masuk dengan Google.'
        );
        return;
      }
      const deepLink = `safartracking://auth?id_token=${encodeURIComponent(idToken)}`;
      setMobileBridgeEmail(user.email || '');
      setMobileBridgeDeepLink(deepLink);
    } catch (err) {
      setMobileBridgeError(mapFirebaseAuthError(err, 'Google'));
    } finally {
      setMobileBridgeBusy(false);
    }
  };

  const handleCopyMobileBridgeLink = async () => {
    if (!mobileBridgeDeepLink) return;
    try {
      await navigator.clipboard.writeText(mobileBridgeDeepLink);
      setMobileBridgeCopied(true);
      window.setTimeout(() => setMobileBridgeCopied(false), 3000);
    } catch {
      setMobileBridgeError(
        'Gagal menyalin otomatis. Silakan tekan tombol Buka Aplikasi Safar Tracking di atas.'
      );
    }
  };

  if (isMobileGoogleBridge) {
    return (
      <div className="min-h-screen bg-[#F4F8F5] dark:bg-[#06130E] text-[#0B1F16] dark:text-[#ECFDF5] flex items-center justify-center p-4">
        <div className="w-full max-w-md rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] shadow-xl p-6 space-y-5">
          <div className="space-y-1.5">
            <span className="inline-block px-2.5 py-1 rounded-md bg-[#ECFDF5] dark:bg-[#064E3B]/40 text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold">
              Safar Tracking Android Sync
            </span>
            <h1 className="text-xl font-bold tracking-tight">
              Sinkronisasi Akun Google ke APK
            </h1>
            <p className="text-sm text-[#4D6659] dark:text-[#9CC2AE]">
              Masuk dengan akun Google Anda (termasuk akun Admin{' '}
              <span className="font-semibold text-[#0B1F16] dark:text-white">
                ekie.ray@gmail.com
              </span>
              ) agar profil &amp; Admin Backend langsung aktif di aplikasi Android.
            </p>
          </div>

          {mobileBridgeError && (
            <div className="rounded-xl bg-[#FEF2F2] dark:bg-[#450A0A]/40 border border-[#FECACA] dark:border-[#991B1B] px-3.5 py-2.5 text-xs text-[#B91C1C] dark:text-[#FCA5A5] space-y-1">
              <p>{mobileBridgeError}</p>
              <p className="opacity-85">
                Tip: Jika popup diblokir browser HP, izinkan jendela pop-up lalu ketuk lagi tombol masuk di bawah.
              </p>
            </div>
          )}

          {mobileBridgeDeepLink ? (
            <div className="space-y-3">
              <div className="rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/40 border border-[#A7F3D0] dark:border-[#047857] px-3.5 py-3 text-xs text-[#065F46] dark:text-[#A7F3D0]">
                Login Google berhasil ({mobileBridgeEmail || 'terverifikasi'}). Jika aplikasi Android belum terbuka otomatis, ketuk tombol di bawah atau salin link sinkronisasi lalu tempel di APK.
              </div>
              <a
                href={mobileBridgeDeepLink}
                className="w-full min-h-[48px] px-4 py-3 rounded-xl bg-[#B45309] hover:bg-[#92400E] text-white font-semibold text-sm flex items-center justify-center transition-colors"
              >
                Buka Aplikasi Safar Tracking
              </a>
              <a
                href={`intent://auth${mobileBridgeDeepLink.slice('safartracking://auth'.length)}#Intent;scheme=safartracking;package=com.kangekie.safartracking;S.browser_fallback_url=${encodeURIComponent(typeof window !== 'undefined' ? window.location.href : '')};end`}
                className="w-full min-h-[42px] px-4 py-2 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/40 border border-[#A7F3D0] dark:border-[#065F46] text-[#047857] dark:text-[#6EE7B7] font-semibold text-xs flex items-center justify-center transition-colors"
              >
                Jika Tidak Terbuka, Coba Alternatif Chrome Intent
              </a>
              <button
                type="button"
                onClick={handleCopyMobileBridgeLink}
                className="w-full min-h-[44px] px-4 py-2.5 rounded-xl border border-[#CFE0D5] dark:border-[#1B3B2E] hover:bg-[#F4F8F5] dark:hover:bg-[#133025] text-[#0B1F16] dark:text-[#ECFDF5] font-semibold text-xs flex items-center justify-center gap-2 transition-colors cursor-pointer"
              >
                {mobileBridgeCopied ? (
                  <>
                    <Check className="w-4 h-4 text-[#047857]" />
                    Link Sinkronisasi Disalin! Tempel di APK
                  </>
                ) : (
                  <>
                    <Copy className="w-4 h-4" />
                    Salin Link / Token Sinkronisasi
                  </>
                )}
              </button>
            </div>
          ) : (
            <button
              type="button"
              disabled={mobileBridgeBusy}
              onClick={handleMobileGoogleBridgeAuth}
              className="w-full min-h-[48px] px-4 py-3 rounded-xl bg-[#047857] hover:bg-[#065F46] disabled:opacity-60 text-white font-semibold text-sm flex items-center justify-center gap-2 transition-colors cursor-pointer"
            >
              {mobileBridgeBusy ? 'Membuka Google...' : 'Masuk dengan Akun Google'}
            </button>
          )}
        </div>
      </div>
    );
  }

  return (
    <APIProvider
      apiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY || ''}
      language="id"
      region="SA"
      libraries={['routes', 'places', 'marker']}
    >
      <div className="relative min-h-screen bg-[#F4F8F5] dark:bg-[#06130E] text-[#0B1F16] dark:text-[#ECFDF5] flex flex-col transition-colors">
        {/* Google Maps Platform Quota Exceeded Banner */}
        {mapsQuotaExceeded && (
          <div className="sticky top-0 z-50 w-full bg-amber-500/95 px-4 py-2.5 text-center text-sm font-medium text-amber-950 shadow-sm backdrop-blur-sm">
            Map services have reached their temporary limit. Please check back later or visit the{' '}
            <a
              href="https://developers.google.com/maps/ai/ai-studio?utm_campaign=gmp_mcp_codeassist_v1_aistudio#quota_exceeded_errors"
              target="_blank"
              rel="noreferrer"
              className="underline font-semibold"
            >
              maps developer site
            </a>{' '}
            to continue.
          </div>
        )}

      {/* Persistent Makkah Illustration Backdrop (Web Desktop & Mobile) */}
      <div
        aria-hidden="true"
        className="pointer-events-none fixed inset-0 overflow-hidden select-none z-0"
      >
        <img
          src={makkahBackdropImg}
          alt=""
          className="w-full h-full object-cover object-center opacity-50 dark:opacity-25 scale-105"
        />
        <div className="absolute inset-0 bg-[#F4F8F5]/65 dark:bg-[#06130E]/82 backdrop-blur-[1px]" />
      </div>

      {/* Top Bar Contract: 3 zones */}
      <header className="sticky top-0 z-30 bg-white/95 dark:bg-[#0C1E17]/95 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-8">
          <a
            href="#top"
            onClick={(e) => {
              e.preventDefault();
              handleResetDemo();
            }}
            className="text-lg font-bold tracking-tight text-[#064E3B] dark:text-[#ECFDF5] whitespace-nowrap shrink-0 flex items-center gap-2.5"
          >
            <img
              src={safarTrackingIcon}
              alt="Safar Tracking"
              referrerPolicy="no-referrer"
              className="w-7 h-7 rounded-lg object-cover border border-[#047857]/30 shadow-xs shrink-0"
            />
            <span>Safar Tracking</span>
          </a>

          <nav
            aria-label="Navigasi Layar"
            className="hidden md:flex items-center gap-4 text-sm font-medium text-[#4D6659] dark:text-[#9CC2AE] overflow-x-auto no-scrollbar"
          >
            {navSteps.map((step) => {
              const active =
                currentRoute === step.route ||
                (step.route === 'trip_history' && currentRoute === 'trip_detail');
              return (
                <button
                  key={step.route}
                  type="button"
                  onClick={() => {
                    if (step.route === 'tour_leader') setSelectedRole('TOUR_LEADER');
                    if (step.route === 'join_tour') setSelectedRole('MEMBER');
                    if (step.route === 'live_map') {
                      ensureDemoRoom(canCreateRoom ? selectedRole : 'MEMBER');
                    }
                    navigateTo(step.route, 1);
                  }}
                  className={`py-1 transition-colors whitespace-nowrap shrink-0 cursor-pointer border-b-2 flex items-center gap-1.5 ${
                    active
                      ? 'text-[#047857] dark:text-[#34D399] border-[#047857] dark:border-[#34D399] font-semibold'
                      : 'border-transparent hover:text-[#0B1F16] dark:hover:text-white'
                  }`}
                >
                  {step.route === 'manasik_guide' && (
                    <BookOpenCheck className="w-3.5 h-3.5 shrink-0" />
                  )}
                  <span>{step.label}</span>
                </button>
              );
            })}
          </nav>

          <div className="flex items-center gap-2 shrink-0">
            <button
              type="button"
              onClick={() => navigateTo('manasik_guide', 1)}
              className={`min-h-[40px] px-3 py-2 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0 border ${
                currentRoute === 'manasik_guide'
                  ? 'bg-[#047857] text-white border-[#047857]'
                  : 'bg-[#ECFDF5] dark:bg-[#064E3B]/70 border-[#A7F3D0] dark:border-[#059669] text-[#047857] dark:text-[#6EE7B7] hover:bg-[#D1FAE5] dark:hover:bg-[#065F46]'
              }`}
            >
              <BookOpenCheck className="w-3.5 h-3.5 shrink-0" />
              <span>Menu Manasik</span>
            </button>
            <button
              type="button"
              onClick={handleResetDemo}
              className="min-h-[40px] px-3.5 py-2 rounded-lg bg-[#B45309] hover:bg-[#92400E] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap shrink-0"
            >
              <RotateCcw className="w-3.5 h-3.5 shrink-0" />
              <span>Reset Alur</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Workspace */}
      <main className="relative z-10 flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-6 lg:py-8">
        <div className="flex flex-wrap items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-balance">
              Safar Tracking — Pelacakan Jamaah Umroh &amp; Peta Tanah Suci Real-Time
            </h1>
            <p className="mt-1 text-sm text-[#4D6659] dark:text-[#9CC2AE]">
              Material Design 3 · Live GPS Jamaah · Cloud Sync · Google Maps Makkah/Madinah · PTT &amp; SOS
            </p>
          </div>

          <div className="flex items-center gap-2">
            <div className="flex lg:hidden items-center p-1 rounded-lg bg-[#DCEAE1] dark:bg-[#133025]">
              <button
                type="button"
                onClick={() => setMobileWorkspaceTab('preview')}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors cursor-pointer whitespace-nowrap ${
                  mobileWorkspaceTab === 'preview'
                    ? 'bg-white dark:bg-[#06130E] text-[#0B1F16] dark:text-white shadow-xs'
                    : 'text-[#4D6659] dark:text-[#9CC2AE]'
                }`}
              >
                Pratinjau App
              </button>
              <button
                type="button"
                onClick={() => setMobileWorkspaceTab('code')}
                className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors cursor-pointer whitespace-nowrap ${
                  mobileWorkspaceTab === 'code'
                    ? 'bg-white dark:bg-[#06130E] text-[#0B1F16] dark:text-white shadow-xs'
                    : 'text-[#4D6659] dark:text-[#9CC2AE]'
                }`}
              >
                Kode Kotlin
              </button>
            </div>

            <button
              type="button"
              onClick={() => setDarkMode((prev) => !prev)}
              className="min-h-[40px] px-3.5 py-2 rounded-lg bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs font-medium flex items-center gap-2 hover:bg-[#ECFDF5] dark:hover:bg-[#133025] transition-colors cursor-pointer whitespace-nowrap shrink-0"
            >
              {darkMode ? (
                <>
                  <Sun className="w-4 h-4 text-[#FCD34D]" />
                  <span>Tema Terang</span>
                </>
              ) : (
                <>
                  <Moon className="w-4 h-4 text-[#047857]" />
                  <span>Tema Gelap</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* 2-Column Workspace */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Left Column: Interactive Android Simulator */}
          <div
            className={`lg:col-span-5 xl:col-span-4 flex flex-col items-center ${
              mobileWorkspaceTab === 'code' ? 'hidden lg:flex' : 'flex'
            }`}
          >
            <div className="flex md:hidden flex-wrap justify-center gap-1.5 mb-4 w-full">
              {navSteps.map((step) => {
                const active =
                  currentRoute === step.route ||
                  (step.route === 'trip_history' && currentRoute === 'trip_detail');
                return (
                  <button
                    key={step.route}
                    type="button"
                    onClick={() => {
                      if (step.route === 'tour_leader') setSelectedRole('TOUR_LEADER');
                      if (step.route === 'join_tour') setSelectedRole('MEMBER');
                      if (step.route === 'live_map') {
                        ensureDemoRoom(canCreateRoom ? selectedRole : 'MEMBER');
                      }
                      navigateTo(step.route, 1);
                    }}
                    className={`px-2.5 py-1.5 rounded-lg text-xs font-medium cursor-pointer whitespace-nowrap ${
                      active
                        ? 'bg-[#B45309] text-white'
                        : 'bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] text-[#2F4A3C] dark:text-[#D1E7DA]'
                    }`}
                  >
                    {step.label}
                  </button>
                );
              })}
            </div>

            <div className="w-full max-w-[390px] rounded-[34px] border-[6px] border-[#0B3B28] dark:border-[#1B3B2E] bg-[#F4F8F5] dark:bg-[#06130E] shadow-xl overflow-hidden flex flex-col">
              {/* Android Status Bar */}
              <div className="h-7 px-5 bg-white dark:bg-[#0C1E17] flex items-center justify-between text-[11px] font-medium text-[#4D6659] dark:text-[#9CC2AE] select-none border-b border-[#E4EFE8] dark:border-[#163126]">
                <span className="tabular-nums">09:41</span>
                <div className="w-16 h-3.5 rounded-full bg-[#06130E] dark:bg-[#1B3B2E]" />
                <span className="tabular-nums">
                  {gps.status === 'active' ? 'GPS · 5G' : '5G · 98%'}
                </span>
              </div>

              {/* Compose NavHost Animated Viewport */}
              <div className="relative h-[680px] flex flex-col overflow-hidden">
                {/* Fixed Makkah Haramain App Background */}
                <div
                  aria-hidden="true"
                  className="pointer-events-none absolute inset-0 z-0 select-none"
                >
                  <img
                    src={makkahBackdropImg}
                    alt=""
                    className="w-full h-full object-cover object-center"
                  />
                  <div className="absolute inset-0 bg-[#F4F8F5]/45 dark:bg-[#06130E]/82 transition-colors" />
                </div>

                <div className="relative z-10 flex-1 min-h-0 overflow-y-auto overflow-x-hidden">
                  <AnimatePresence mode="wait" initial={false}>
                    <motion.div
                      key={currentRoute}
                      initial={{ opacity: 0, x: navDirection * 28 }}
                      animate={{ opacity: 1, x: 0 }}
                      exit={{ opacity: 0, x: navDirection * -28 }}
                      transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
                      className="min-h-full flex flex-col"
                    >
                    {currentRoute === 'login' && (
                      <LoginView
                        authUserEmail={authUser?.email ?? (localUserEmail || null)}
                        authUserName={
                          vehicleProfile.fullName || authUser?.displayName || null
                        }
                        authProvider={authProvider}
                        localUserName={localUserName}
                        isSigningIn={isSigningIn}
                        authError={authError}
                        authInfoMessage={authInfoMessage}
                        initialName={vehicleProfile.fullName}
                        initialEmail={vehicleProfile.email || localUserEmail}
                        globalConfig={globalConfig}
                        pendingFamilyInviteCode={pendingFamilyInviteCode}
                        hasActiveFamilySession={isFamilyAccount}
                        onGoogleSignIn={handleGoogleSignIn}
                        onFacebookSignIn={handleFacebookSignIn}
                        onInstagramSignIn={handleInstagramSignIn}
                        onEmailAuth={handleEmailAuth}
                        onFamilyGoogleSignIn={handleFamilyGoogleSignIn}
                        onFamilyEmailAuth={handleFamilyEmailAuth}
                        onForgotPassword={handleForgotPassword}
                        onLocalLogin={handleLocalLogin}
                        onContinueSession={() =>
                          navigateTo(isFamilyAccount ? 'family_monitor' : 'role_selection', 1)
                        }
                        onOpenCustomizeProfile={() => {
                          setCustomizeReturnRoute('login');
                          setProfileSyncMessage(null);
                          navigateTo('customize_profile', 1);
                        }}
                        onOpenAboutApp={() => {
                          setAboutReturnRoute('login');
                          navigateTo('about_app', 1);
                        }}
                      />
                    )}

                    {currentRoute === 'role_selection' && (
                      <RoleSelectionView
                        activeRoom={activeRoom}
                        lastCompletedTrip={lastCompletedTrip ?? tripHistory[0] ?? null}
                        destinationName={routeConfig.destinationName}
                        tripHistoryCount={tripHistory.length}
                        latestTripTitle={tripHistory[0]?.tourTitle}
                        authUserEmail={authUser?.email ?? (localUserEmail || null)}
                        authUserName={authUser?.displayName ?? null}
                        authProvider={authProvider}
                        userProfile={vehicleProfile}
                        localUserName={localUserName}
                        isSigningIn={isSigningIn}
                        globalConfig={globalConfig}
                        isAdminUser={isAdminUser}
                        canCreateRoom={canCreateRoom}
                        isRegisteredLeaderUnverified={isRegisteredLeaderUnverified}
                        onGoogleSignIn={handleGoogleSignIn}
                        onGoogleSignOut={handleGoogleSignOut}
                        onOpenCustomizeProfile={() => {
                          setCustomizeReturnRoute('role_selection');
                          setProfileSyncMessage(null);
                          navigateTo('customize_profile', 1);
                        }}
                        onOpenAdminBackend={() => navigateTo('admin_backend', 1)}
                        onSelectRole={handleSelectRole}
                        onResumeCurrentConvoy={() => {
                          if (activeRoom) {
                            navigateTo('live_map', 1);
                          }
                        }}
                        onRepeatTour={handleRepeatTour}
                        onOpenTripHistory={() => navigateTo('trip_history', 1)}
                        onOpenManasikGuide={() => {
                          setManasikInitialTab('manasik');
                          navigateTo('manasik_guide', 1);
                        }}
                        onOpenPrayerGuide={() => {
                          setManasikInitialTab('doa');
                          navigateTo('manasik_guide', 1);
                        }}
                        onOpenRoomActivities={() => {
                          setManasikInitialTab('activities');
                          navigateTo('manasik_guide', 1);
                        }}
                        onOpenAssistant={() =>
                          openManasikAssistant('role_selection')
                        }
                        manasikProgress={manasikProgress}
                        roomActivities={roomActivities}
                        roomActivityProgress={roomActivityProgress}
                        currentMemberUid={
                          (activeRoom?.isSyncedRoom && authUser?.uid) ||
                          activeRoom?.members.find((m) => m.isCurrentUser)?.id ||
                          authUser?.uid ||
                          ''
                        }
                        userGpsLocation={
                          gps.lastFix
                            ? {
                                lat: gps.lastFix.latitude,
                                lng: gps.lastFix.longitude,
                              }
                            : null
                        }
                        onOpenAboutApp={() => {
                          setAboutReturnRoute('role_selection');
                          navigateTo('about_app', 1);
                        }}
                      />
                    )}

                    {currentRoute === 'admin_backend' && (
                      <AdminBackendView
                        authUser={authUser}
                        globalConfig={globalConfig}
                        isSigningIn={isSigningIn}
                        onGoogleSignIn={handleGoogleSignIn}
                        onBack={() => navigateTo('role_selection', -1)}
                      />
                    )}

                    {currentRoute === 'customize_profile' && (
                      <CustomizeProfileView
                        profile={vehicleProfile}
                        authProvider={authProvider}
                        isCloudAuthenticated={Boolean(authUser)}
                        isSavingProfile={isSavingProfile}
                        syncStatusMessage={profileSyncMessage}
                        onSaveAndSyncProfile={handleSaveCustomizedProfile}
                        onBack={() => navigateTo(customizeReturnRoute, -1)}
                      />
                    )}

                    {currentRoute === 'vehicle_profile' && (
                      <VehicleProfileView
                        selectedRole={selectedRole}
                        profile={vehicleProfile}
                        onUpdateProfile={handleUpdateProfile}
                        onBack={() => navigateTo('role_selection', -1)}
                        onOpenAboutApp={() => {
                          setAboutReturnRoute('vehicle_profile');
                          navigateTo('about_app', 1);
                        }}
                        onContinue={() =>
                          navigateTo(
                            selectedRole === 'TOUR_LEADER'
                              ? 'tour_leader'
                              : 'join_tour',
                            1
                          )
                        }
                      />
                    )}

                    {currentRoute === 'tour_leader' && (
                      <TourLeaderView
                        activeRoom={activeRoom}
                        routeConfig={routeConfig}
                        destinationPresets={DESTINATION_PRESETS}
                        vehicleType={vehicleProfile.vehicleType}
                        isAuthenticated={Boolean(authUser)}
                        isCreatingRoom={isCreatingRoom}
                        createRoomError={createRoomError}
                        userGpsLocation={
                          gps.lastFix
                            ? {
                                lat: gps.lastFix.latitude,
                                lng: gps.lastFix.longitude,
                              }
                            : null
                        }
                        darkMode={darkMode}
                        onGoogleSignIn={handleGoogleSignIn}
                        onPickOriginLatLng={handlePickOriginLatLng}
                        onUseGpsAsOrigin={handleUseGpsAsOrigin}
                        onSelectPreset={handleSelectPreset}
                        onPickDestinationLatLng={handlePickDestinationLatLng}
                        onAddDestinationPoint={handleAddDestinationPoint}
                        onRemoveDestinationPoint={handleRemoveDestinationPoint}
                        onMoveDestinationPoint={handleMoveDestinationPoint}
                        onSelectRoutePreference={handleSelectRoutePreference}
                        onUpdateWaypoints={handleUpdateWaypoints}
                        onResetDefaultWaypoints={handleResetDefaultWaypoints}
                        onRouteComputed={handleRouteComputed}
                        onAddCheckpoint={handleAddCheckpointFromStepper}
                        onRemoveCheckpoint={handleRemoveCheckpoint}
                        onUpdateMaxStragglerDistance={handleUpdateMaxStraggler}
                        onToggleSoundAlert={handleToggleSoundAlert}
                        onCreateRoom={handleCreateRoom}
                        onCreateLocalRoom={handleCreateLocalDemoRoom}
                        onSimulateJoin={handleSimulateJoin}
                        onRemoveMember={handleRemoveMember}
                        onEnterMapPlaceholder={() =>
                          navigateTo('live_map', 1)
                        }
                        onBack={() => navigateTo('vehicle_profile', -1)}
                        draftActivities={draftRoomActivities}
                        activeRoomActivities={roomActivities}
                        activityProgressByMember={roomActivityProgress}
                        currentUid={
                          (activeRoom?.isSyncedRoom && authUser?.uid) ||
                          activeRoom?.members.find((m) => m.isCurrentUser)?.id ||
                          authUser?.uid ||
                          ''
                        }
                        onAddDraftActivity={handleAddDraftActivity}
                        onRemoveDraftActivity={handleRemoveDraftActivity}
                        onMoveDraftActivity={handleMoveDraftActivity}
                        onApplyDraftStarterTemplates={handleApplyDraftStarterTemplates}
                        onToggleRoomActivity={handleToggleRoomActivity}
                        onAddRoomActivity={handleAddRoomActivity}
                        onRemoveRoomActivity={handleRemoveRoomActivity}
                        onMoveRoomActivity={handleMoveRoomActivity}
                        onApplyRoomStarterTemplates={handleApplyRoomStarterTemplates}
                      />
                    )}

                    {currentRoute === 'join_tour' && (
                      <JoinTourView
                        joinInput={joinInput}
                        joinError={joinError}
                        extractedRoomId={extractRoomId(joinInput)}
                        vehicleProfile={vehicleProfile}
                        isAuthenticated={Boolean(authUser)}
                        isJoining={isJoiningRoom}
                        onGoogleSignIn={handleGoogleSignIn}
                        onInputChange={(val) => {
                          setJoinInput(val);
                          setJoinError(null);
                        }}
                        onJoinSubmit={handleJoinSubmit}
                        onJoinDemoOffline={() =>
                          joinOfflineDemoRoom(extractRoomId(joinInput) || 'UMR-8492')
                        }
                        onBack={() => navigateTo('vehicle_profile', -1)}
                      />
                    )}

                    {currentRoute === 'live_map' && (() => {
                      const selfMember = activeRoom?.members.find((m) => m.isCurrentUser);
                      const isEffectiveLeaderNow = activeRoom?.isSyncedRoom && authUser
                        ? isRoomEffectiveLeader(activeRoom, authUser.uid)
                        : activeRoom?.leaderTroubleActive
                          ? Boolean(
                              selfMember &&
                                activeRoom.actingLeaderUid &&
                                selfMember.id === activeRoom.actingLeaderUid
                            )
                          : selectedRole === 'TOUR_LEADER';
                      return (
                      <LiveTrackingMapView
                        activeRoom={activeRoom}
                        role={selectedRole}
                        selectedMemberId={selectedMemberId}
                        isSimulationRunning={isSimulationRunning}
                        routeConfig={routeConfig}
                        vehicleType={vehicleProfile.vehicleType}
                        stragglerAlert={stragglerAlert}
                        offRouteAlert={offRouteAlert}
                        sosState={sosState}
                        messages={messages}
                        isRecordingVoice={isRecordingVoice}
                        recordingSeconds={recordingSeconds}
                        playingMessageId={playingMessageId}
                        voiceStatusMessage={voiceStatusMessage}
                        isPickingDestinationOnMap={isPickingDestinationOnMap}
                        isPickingCheckpointOnMap={isPickingCheckpointOnMap}
                        pendingCheckpointName={pendingCheckpointName}
                        isOffRouteSimActive={isOffRouteSimActive}
                        isStragglerSimActive={isStragglerSimActive}
                        isLeaderTroubleSimActive={isLeaderTroubleSimActive}
                        canPingRouting={isEffectiveLeaderNow}
                        isEffectiveLeader={isEffectiveLeaderNow}
                        destinationPresets={DESTINATION_PRESETS}
                        darkMode={darkMode}
                        gpsStatus={gps.status}
                        gpsAccuracyMeters={gps.lastFix?.accuracyMeters}
                        gpsErrorMessage={gps.errorMessage}
                        onRetryGps={gps.retryGps}
                        onRouteComputed={handleRouteComputed}
                        onSelectMember={setSelectedMemberId}
                        onToggleSimulation={() =>
                          setIsSimulationRunning((prev) => !prev)
                        }
                        onSelectPreset={handleSelectPreset}
                        onPickDestinationLatLng={(
                          lat,
                          lng,
                          placeName,
                          addressLabel
                        ) =>
                          handlePickDestinationLatLng(
                            lat,
                            lng,
                            placeName,
                            addressLabel
                          )
                        }
                        onTogglePickMode={(enabled) => {
                          if (enabled) {
                            setIsPickingCheckpointOnMap(false);
                            setPendingCheckpointName('');
                          }
                          setIsPickingDestinationOnMap(enabled);
                        }}
                        onStartPickCheckpoint={handleStartPickCheckpoint}
                        onCancelPickCheckpoint={handleCancelPickCheckpoint}
                        onConfirmAddCheckpointLatLng={handleConfirmAddCheckpointLatLng}
                        onRemoveCheckpoint={handleRemoveCheckpoint}
                        onUpdateMaxStragglerDistance={handleUpdateMaxStraggler}
                        onUpdateOffRouteTolerance={handleUpdateOffRouteTolerance}
                        onToggleSoundAlert={handleToggleSoundAlert}
                        onToggleOffRouteSim={handleToggleOffRouteSim}
                        onToggleStragglerSim={handleToggleStragglerSim}
                        onToggleLeaderTroubleSim={handleToggleLeaderTroubleSim}
                        onReclaimLeader={handleReclaimLeader}
                        onTriggerSos={handleTriggerSos}
                        onResolveSos={handleResolveSos}
                        onSendTextMessage={handleSendTextMessage}
                        onStartVoiceRecord={handleStartVoiceRecord}
                        onStopVoiceRecord={handleStopVoiceRecord}
                        onToggleVoicePlay={handleToggleVoicePlay}
                        onUpdateOwnCondition={handleUpdateOwnCondition}
                        onAssignSweeper={handleAssignSweeper}
                        onSwitchPreviewRole={setSelectedRole}
                        onFinishAndSaveTrip={handleFinishAndSaveTrip}
                        onExitRoom={() => navigateTo('role_selection', -1)}
                        manasikProgress={manasikProgress}
                        isManasikCloudSynced={isManasikCloudSynced}
                        onToggleManasikStep={handleToggleManasikStep}
                        onCheckInManasikStep={handleCheckInManasikStep}
                        onResetManasikGuide={handleResetManasikGuide}
                        onOpenManasikAssistant={(prompt) =>
                          openManasikAssistant('live_map', prompt)
                        }
                        roomActivities={roomActivities}
                        roomActivityProgress={roomActivityProgress}
                        currentMemberUid={
                          (activeRoom?.isSyncedRoom && authUser?.uid) ||
                          selfMember?.id ||
                          authUser?.uid ||
                          ''
                        }
                        onToggleRoomActivity={handleToggleRoomActivity}
                        onAddRoomActivity={handleAddRoomActivity}
                        onRemoveRoomActivity={handleRemoveRoomActivity}
                        onMoveRoomActivity={handleMoveRoomActivity}
                        onApplyRoomStarterTemplates={handleApplyRoomStarterTemplates}
                        batterySaverMode={batterySaverMode}
                        onToggleBatterySaverMode={handleToggleBatterySaverMode}
                        isOnline={isOnline}
                        offlineQueueCount={offlineQueueCount}
                        onFlushOfflineQueue={handleManualFlushOfflineQueue}
                        dismissedBroadcastId={dismissedBroadcastId}
                        onDismissBroadcast={(id) => setDismissedBroadcastId(id)}
                        onSendPriorityBroadcast={handleSendPriorityBroadcast}
                        onClearPriorityBroadcast={handleClearPriorityBroadcast}
                        missingPilgrimAlert={missingPilgrimAlert}
                        isMissingPilgrimSimActive={isMissingPilgrimSimActive}
                        onToggleMissingPilgrimSim={handleToggleMissingPilgrimSim}
                        myFamilyLink={myFamilyLink}
                        isGeneratingFamilyLink={isGeneratingFamilyLink}
                        familyLinkError={familyLinkError}
                        onGenerateFamilyLink={handleGenerateFamilyLink}
                        onRegenerateFamilyLink={handleRegenerateFamilyLink}
                        onRevokeFamilyLink={handleRevokeFamilyLink}
                      />
                      );
                    })()}

                    {currentRoute === 'trip_history' && (
                      <TripHistoryView
                        trips={tripHistory}
                        onSelectTrip={(tripId) => {
                          setSelectedTripId(tripId);
                          navigateTo('trip_detail', 1);
                        }}
                        onBack={() => navigateTo('role_selection', -1)}
                      />
                    )}

                    {currentRoute === 'trip_detail' && (
                      <TripDetailView
                        trip={
                          tripHistory.find((t) => t.id === selectedTripId) ??
                          tripHistory[0] ??
                          null
                        }
                        currentUserName={
                          vehicleProfile.fullName.trim() ||
                          authUser?.displayName?.trim() ||
                          localUserName?.trim() ||
                          undefined
                        }
                        currentUserAvatarPreset={vehicleProfile.avatarPreset}
                        currentUserAvatarPhotoUrl={vehicleProfile.avatarPhotoUrl}
                        autoFinishNotice={autoFinishNotice}
                        onDismissAutoFinishNotice={() => setAutoFinishNotice(null)}
                        onBack={() => {
                          setAutoFinishNotice(null);
                          navigateTo('trip_history', -1);
                        }}
                      />
                    )}

                    {currentRoute === 'manasik_guide' && (() => {
                      const roomSelf = activeRoom?.members.find((m) => m.isCurrentUser);
                      const resolvedLocation = gps.lastFix
                        ? {
                            latitude: gps.lastFix.latitude,
                            longitude: gps.lastFix.longitude,
                            accuracyMeters: gps.lastFix.accuracyMeters,
                            sourceLabel: 'GPS Perangkat Asli',
                          }
                        : roomSelf
                          ? {
                              latitude: roomSelf.mapPosition.latitude,
                              longitude: roomSelf.mapPosition.longitude,
                              accuracyMeters: roomSelf.mapPosition.accuracyMeters,
                              sourceLabel: `Posisi Grup (${roomSelf.lastLocationText})`,
                            }
                          : null;
                      const resolvedUid =
                        (activeRoom?.isSyncedRoom && authUser?.uid) ||
                        roomSelf?.id ||
                        authUser?.uid ||
                        '';
                      const resolvedIsCreator = activeRoom?.isSyncedRoom && authUser
                        ? isRoomEffectiveLeader(activeRoom, authUser.uid)
                        : activeRoom?.leaderTroubleActive
                          ? Boolean(
                              roomSelf &&
                                activeRoom.actingLeaderUid &&
                                roomSelf.id === activeRoom.actingLeaderUid
                            )
                          : selectedRole === 'TOUR_LEADER';
                      return (
                        <ManasikGuideView
                          progress={manasikProgress}
                          isCloudSynced={isManasikCloudSynced}
                          initialTab={manasikInitialTab}
                          activeRoom={activeRoom}
                          roomActivities={roomActivities}
                          roomActivityProgress={roomActivityProgress}
                          currentMemberUid={resolvedUid}
                          isRoomCreator={resolvedIsCreator}
                          onToggleRoomActivity={handleToggleRoomActivity}
                          onAddRoomActivity={handleAddRoomActivity}
                          onRemoveRoomActivity={handleRemoveRoomActivity}
                          onMoveRoomActivity={handleMoveRoomActivity}
                          onApplyRoomStarterTemplates={handleApplyRoomStarterTemplates}
                          userLocation={resolvedLocation}
                          gpsStatus={gps.status}
                          onRetryGps={gps.retryGps}
                          onToggleStep={handleToggleManasikStep}
                          onCheckInStep={handleCheckInManasikStep}
                          onResetGuide={handleResetManasikGuide}
                          onOpenAssistant={(prompt) =>
                            openManasikAssistant('manasik_guide', prompt)
                          }
                          onBack={() => navigateTo('role_selection', -1)}
                        />
                      );
                    })()}

                    {currentRoute === 'ai_assistant' && (
                      <ManasikChatbotView
                        manasikProgress={manasikProgress}
                        currentLocationText={
                          activeRoom?.members.find((m) => m.isCurrentUser)
                            ?.lastLocationText || routeConfig.destinationName
                        }
                        initialPrompt={assistantInitialPrompt}
                        onConsumeInitialPrompt={() =>
                          setAssistantInitialPrompt(null)
                        }
                        onBack={() => navigateTo(assistantReturnRoute, -1)}
                      />
                    )}

                    {currentRoute === 'family_monitor' && familyGrant && (
                      <FamilyMonitorView
                        grant={familyGrant}
                        snapshot={familyMonitorSnapshot}
                        errorMessage={familyMonitorError}
                        darkMode={darkMode}
                        nowMs={nowTickMs}
                        onLogoutFamily={handleGoogleSignOut}
                      />
                    )}

                    {currentRoute === 'about_app' && (
                      <AboutAppView
                        onBack={() => navigateTo(aboutReturnRoute, -1)}
                      />
                    )}
                  </motion.div>
                </AnimatePresence>
                </div>

                {/* Persistent In-App Bottom Navigation Bar */}
                {isSessionAuthenticated &&
                  !isFamilyAccount &&
                  currentRoute !== 'family_monitor' &&
                  currentRoute !== 'login' &&
                  currentRoute !== 'live_map' && (
                    <nav
                      aria-label="Navigasi Utama Aplikasi"
                      className="relative z-20 shrink-0 grid grid-cols-5 bg-white/95 dark:bg-[#0C1E17]/95 backdrop-blur-md border-t border-[#D8E6DD] dark:border-[#1B3B2E]"
                    >
                      {(
                        [
                          {
                            id: 'role_selection',
                            label: 'Beranda',
                            active:
                              currentRoute === 'role_selection' ||
                              currentRoute === 'vehicle_profile' ||
                              currentRoute === 'tour_leader' ||
                              currentRoute === 'join_tour',
                            icon: <Compass className="w-4 h-4 shrink-0" />,
                            onClick: () => navigateTo('role_selection', -1),
                          },
                          {
                            id: 'live_map',
                            label: 'Peta Live',
                            active: false,
                            icon: <MapPin className="w-4 h-4 shrink-0" />,
                            onClick: () => {
                              ensureDemoRoom(selectedRole);
                              navigateTo('live_map', 1);
                            },
                          },
                          {
                            id: 'manasik_guide',
                            label: 'Manasik',
                            subLabel: `${manasikProgress.umrah.length}/${MANASIK_GUIDE_MAP.umrah.length}`,
                            active: currentRoute === 'manasik_guide',
                            icon: <BookOpenCheck className="w-4 h-4 shrink-0" />,
                            onClick: () => {
                              setManasikInitialTab('manasik');
                              navigateTo('manasik_guide', 1);
                            },
                          },
                          {
                            id: 'ai_assistant',
                            label: 'Tanya AI',
                            active: currentRoute === 'ai_assistant',
                            icon: <Sparkles className="w-4 h-4 shrink-0" />,
                            onClick: () =>
                              openManasikAssistant(
                                currentRoute === 'ai_assistant'
                                  ? assistantReturnRoute
                                  : currentRoute
                              ),
                          },
                          {
                            id: 'trip_history',
                            label: 'Riwayat',
                            active:
                              currentRoute === 'trip_history' ||
                              currentRoute === 'trip_detail',
                            icon: <History className="w-4 h-4 shrink-0" />,
                            onClick: () => navigateTo('trip_history', 1),
                          },
                        ] as Array<{
                          id: string;
                          label: string;
                          subLabel?: string;
                          active: boolean;
                          icon: React.ReactNode;
                          onClick: () => void;
                        }>
                      ).map((item) => (
                        <button
                          key={item.id}
                          type="button"
                          onClick={item.onClick}
                          aria-current={item.active ? 'page' : undefined}
                          className={`min-h-[54px] px-1 py-1.5 flex flex-col items-center justify-center gap-0.5 border-t-2 transition-colors cursor-pointer ${
                            item.active
                              ? 'border-[#047857] dark:border-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/35 text-[#047857] dark:text-[#34D399]'
                              : 'border-transparent text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#047857] dark:hover:text-[#A7F3D0] hover:bg-[#F4F8F5]/70 dark:hover:bg-[#091812]/60'
                          }`}
                        >
                          {item.icon}
                          <span className="text-[10px] font-semibold leading-tight truncate max-w-full">
                            {item.label}
                          </span>
                          {item.subLabel && (
                            <span className="text-[9px] font-mono tabular-nums leading-none opacity-80">
                              {item.subLabel}
                            </span>
                          )}
                        </button>
                      ))}
                    </nav>
                  )}
              </div>

              {/* Android Gesture Navigation Pill */}
              <div className="h-5 bg-white dark:bg-[#0C1E17] flex items-center justify-center border-t border-[#E4EFE8] dark:border-[#163126]">
                <div className="w-28 h-1 rounded-full bg-[#CFE0D5] dark:bg-[#1B3B2E]" />
              </div>
            </div>
          </div>

          {/* Right Column: Kotlin & Jetpack Compose Source Inspector */}
          <div
            className={`lg:col-span-7 xl:col-span-8 ${
              mobileWorkspaceTab === 'preview' ? 'hidden lg:block' : 'block'
            }`}
          >
            <KotlinSourceExplorer
              currentRoute={currentRoute}
              syncWithPreview={syncWithPreview}
              onToggleSync={() => setSyncWithPreview((prev) => !prev)}
            />
          </div>
        </div>
      </main>
    </div>
    </APIProvider>
  );
}
