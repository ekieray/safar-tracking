import { useEffect, useRef, useState } from 'react';
import { useMapsLibrary } from '@vis.gl/react-google-maps';
import {
  AlertTriangle,
  ArrowLeft,
  BatteryCharging,
  BellRing,
  BookOpenCheck,
  Bus,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  ClipboardCheck,
  Clock,
  Copy,
  Eye,
  EyeOff,
  Flag,
  Footprints,
  Gauge,
  HeartPulse,
  Info,
  Landmark,
  Link2,
  Loader2,
  LocateFixed,
  MapPin,
  Megaphone,
  MessageCircle,
  Mic,
  Navigation,
  Pause,
  Play,
  RefreshCw,
  Route,
  Search,
  Send,
  Settings,
  Share2,
  Shield,
  Square,
  Volume2,
  Wifi,
  WifiOff,
  X,
} from 'lucide-react';
import {
  Area,
  AreaChart,
  ResponsiveContainer,
  Tooltip,
  YAxis,
} from 'recharts';
import {
  ConvoyMember,
  ConvoyRouteConfig,
  DestinationEtaInfo,
  DestinationPreset,
  ManasikCheckInRecord,
  ManasikGuideType,
  ManasikProgressState,
  MemberConditionStatus,
  OffRouteAlertState,
  RoomActivityItem,
  RoomActivityMemberProgress,
  RoomMessage,
  RouteInstruction,
  SosEmergencyState,
  StragglerAlertState,
  TourRoom,
  UserRole,
  VehicleType,
} from '../../types/convoy';
import { ConvoyAvatar } from '../ConvoyAvatar';
import { ManasikChecklistPanel } from '../ManasikChecklistPanel';
import { RoomActivityChecklistPanel } from '../RoomActivityChecklistPanel';
import { LiveGoogleConvoyMap } from '../map/LiveGoogleConvoyMap';
import { GpsPermissionStatus } from '../../hooks/useDeviceGps';
import {
  GooglePlaceSuggestion,
  searchGoogleAutocompleteSuggestions,
  selectGooglePlacePrediction,
} from '../../services/googleMapsService';
import { LatLngPoint } from '../../utils/geo';

interface LiveTrackingMapViewProps {
  activeRoom: TourRoom | null;
  role: UserRole;
  selectedMemberId: string | null;
  isSimulationRunning: boolean;
  routeConfig: ConvoyRouteConfig;
  vehicleType?: VehicleType;
  stragglerAlert: StragglerAlertState;
  offRouteAlert: OffRouteAlertState;
  sosState: SosEmergencyState;
  messages: RoomMessage[];
  isRecordingVoice: boolean;
  recordingSeconds: number;
  playingMessageId: string | null;
  voiceStatusMessage?: string | null;
  isPickingDestinationOnMap: boolean;
  isPickingCheckpointOnMap: boolean;
  pendingCheckpointName: string;
  isOffRouteSimActive: boolean;
  isStragglerSimActive: boolean;
  isLeaderTroubleSimActive: boolean;
  canPingRouting: boolean;
  isEffectiveLeader: boolean;
  destinationPresets: DestinationPreset[];
  darkMode?: boolean;
  gpsStatus: GpsPermissionStatus;
  gpsAccuracyMeters?: number;
  gpsErrorMessage?: string | null;
  onRetryGps: () => void;
  onRouteComputed?: (
    distanceMeters: number,
    durationMillis: number,
    path: LatLngPoint[],
    instructions?: RouteInstruction[]
  ) => void;
  onSelectMember: (memberId: string) => void;
  onToggleSimulation: () => void;
  onSelectPreset: (preset: DestinationPreset) => void;
  onPickDestinationLatLng: (
    lat: number,
    lng: number,
    placeName?: string,
    addressLabel?: string
  ) => void;
  onTogglePickMode: (enabled: boolean) => void;
  onStartPickCheckpoint: (name: string) => void;
  onCancelPickCheckpoint: () => void;
  onConfirmAddCheckpointLatLng: (
    name: string,
    lat: number,
    lng: number
  ) => void;
  onRemoveCheckpoint: (checkpointId: string) => void;
  onUpdateMaxStragglerDistance: (meters: number) => void;
  onUpdateOffRouteTolerance: (meters: number) => void;
  onToggleSoundAlert: () => void;
  onToggleOffRouteSim: () => void;
  onToggleStragglerSim: () => void;
  onToggleLeaderTroubleSim: () => void;
  onReclaimLeader: () => void;
  onTriggerSos: () => void;
  onResolveSos: () => void;
  onSendTextMessage: (text: string) => void;
  onStartVoiceRecord: () => void;
  onStopVoiceRecord: () => void;
  onToggleVoicePlay: (msg: RoomMessage) => void;
  onUpdateOwnCondition: (status: MemberConditionStatus) => void;
  onAssignSweeper: (memberId: string) => void;
  onSwitchPreviewRole: (role: UserRole) => void;
  onFinishAndSaveTrip: () => void;
  onExitRoom: () => void;
  manasikProgress?: ManasikProgressState;
  isManasikCloudSynced?: boolean;
  onToggleManasikStep?: (guideType: ManasikGuideType, stepId: string) => void;
  onCheckInManasikStep?: (
    guideType: ManasikGuideType,
    checkIn: ManasikCheckInRecord
  ) => void;
  onResetManasikGuide?: (guideType: ManasikGuideType) => void;
  onOpenManasikAssistant?: (initialPrompt?: string) => void;
  roomActivities?: RoomActivityItem[];
  roomActivityProgress?: Record<string, RoomActivityMemberProgress>;
  currentMemberUid?: string;
  onToggleRoomActivity?: (activityId: string) => void;
  onAddRoomActivity?: (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => void;
  onRemoveRoomActivity?: (activityId: string) => void;
  onMoveRoomActivity?: (activityId: string, direction: 'up' | 'down') => void;
  onApplyRoomStarterTemplates?: (items: RoomActivityItem[]) => void;
  batterySaverMode?: boolean;
  onToggleBatterySaverMode?: () => void;
  isOnline?: boolean;
  offlineQueueCount?: number;
  onFlushOfflineQueue?: () => void;
  dismissedBroadcastId?: string;
  onDismissBroadcast?: (id: string) => void;
  onSendPriorityBroadcast?: (title: string, message: string) => void;
  onClearPriorityBroadcast?: () => void;
}

function getMarkerTitle(member: ConvoyMember): string {
  const detail = member.vehicleDetail.trim()
    ? member.vehicleDetail.trim()
    : member.vehicleType === 'MOBIL'
      ? 'Bus Rombongan'
      : 'Jalan Kaki';
  return `${member.fullName} - ${detail}`;
}

function formatDistanceLabel(meters: number): string {
  if (meters >= 1000) {
    const km = meters / 1000;
    return km % 1 === 0 ? `${km.toFixed(0)} km` : `${km.toFixed(1)} km`;
  }
  return `${meters} m`;
}

function formatClockHHmm(date: Date): string {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function calculateDestinationEta(
  member: ConvoyMember | null,
  routeConfig: ConvoyRouteConfig,
  nowMs = Date.now()
): DestinationEtaInfo {
  const progress = Math.max(0, Math.min(1, member?.mapPosition.routeProgress ?? 0));
  const traveledMeters = Math.round(progress * routeConfig.totalRouteMeters);
  const remainingMeters = Math.max(0, routeConfig.totalRouteMeters - traveledMeters);
  const speedKmh = Math.max(0, member?.mapPosition.speedKmh ?? 0);
  const distanceLabel =
    remainingMeters >= 1000
      ? `${(remainingMeters / 1000).toFixed(1)} km`
      : `${remainingMeters} m`;

  if (remainingMeters <= 50) {
    return {
      remainingMeters: 0,
      speedKmh,
      etaMinutes: 0,
      isStopped: false,
      isArrived: true,
      durationLabel: 'Tiba',
      arrivalClockLabel: formatClockHHmm(new Date(nowMs)),
      distanceLabel: '0 m',
    };
  }

  const formatDurationText = (mins: number) => {
    if (mins >= 60) {
      const hours = Math.floor(mins / 60);
      const remMins = mins % 60;
      return remMins > 0 ? `${hours}j ${remMins}m` : `${hours} jam`;
    }
    return `${mins} mnt`;
  };

  if (speedKmh <= 0) {
    const remainingFraction = Math.max(0.02, 1 - progress);
    const routeApiMinutes =
      routeConfig.estimatedDurationMillis && routeConfig.estimatedDurationMillis > 0
        ? Math.round((routeConfig.estimatedDurationMillis * remainingFraction) / 60_000)
        : 0;
    const estMinutes = Math.max(
      1,
      routeApiMinutes > 0
        ? routeApiMinutes
        : Math.round((remainingMeters / 1000 / 45) * 60)
    );
    const estArrivalClock = `~${formatClockHHmm(new Date(nowMs + estMinutes * 60_000))}`;

    return {
      remainingMeters,
      speedKmh: 0,
      etaMinutes: estMinutes,
      isStopped: true,
      isArrived: false,
      durationLabel: `Est. ${formatDurationText(estMinutes)} · Diam`,
      arrivalClockLabel: estArrivalClock,
      distanceLabel,
    };
  }

  const etaMinutes = Math.max(
    1,
    Math.round((remainingMeters / 1000 / speedKmh) * 60)
  );
  const durationLabel = formatDurationText(etaMinutes);

  const arrivalClockLabel = formatClockHHmm(
    new Date(nowMs + etaMinutes * 60_000)
  );

  return {
    remainingMeters,
    speedKmh,
    etaMinutes,
    isStopped: false,
    isArrived: false,
    durationLabel,
    arrivalClockLabel,
    distanceLabel,
  };
}

function conditionLabel(status: MemberConditionStatus): string {
  switch (status) {
    case 'AMAN':
      return 'Aman / Sehat';
    case 'ISTIRAHAT':
      return 'Istirahat / Miqat';
    case 'MOGOK':
      return 'Perlu Bantuan';
    case 'SOS':
      return 'Darurat SOS';
  }
}

function playAlertTone(isUrgent = false) {
  try {
    const AudioCtx =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext })
        .webkitAudioContext;
    if (!AudioCtx) return;
    const ctx = new AudioCtx();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.type = isUrgent ? 'triangle' : 'sine';
    osc.frequency.setValueAtTime(isUrgent ? 880 : 740, ctx.currentTime);
    osc.frequency.setValueAtTime(isUrgent ? 660 : 580, ctx.currentTime + 0.14);
    gain.gain.setValueAtTime(0.09, ctx.currentTime);
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.34);
    osc.connect(gain);
    gain.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + 0.34);
  } catch {
    // Ignore AudioContext restriction
  }
}

const WAVE_BARS = [10, 20, 14, 28, 18, 32, 22, 16, 26, 14, 20, 12];

interface SpeedHistoryPoint {
  bucketMinute: number;
  timeLabel: string;
  avgSpeed: number;
}

function buildInitialSpeedHistory(baseSpeed: number, nowMs = Date.now()): SpeedHistoryPoint[] {
  const safeBase = Math.max(0, Math.round(baseSpeed));
  const offsets = [-4, -2, 1, -1, 3, 2, -2, 1, 2, 0];
  return offsets.map((delta, idx) => {
    const minutesAgo = 9 - idx;
    const tMs = nowMs - minutesAgo * 60_000;
    const d = new Date(tMs);
    const val = safeBase === 0 ? 0 : Math.max(12, Math.min(130, safeBase + delta));
    return {
      bucketMinute: Math.floor(tMs / 60_000),
      timeLabel: minutesAgo === 0 ? 'Skgr' : formatClockHHmm(d),
      avgSpeed: val,
    };
  });
}

export function LiveTrackingMapView({
  activeRoom,
  role,
  selectedMemberId,
  isSimulationRunning,
  routeConfig,
  vehicleType,
  stragglerAlert,
  offRouteAlert,
  sosState,
  messages,
  isRecordingVoice,
  recordingSeconds,
  playingMessageId,
  voiceStatusMessage,
  isPickingDestinationOnMap,
  isPickingCheckpointOnMap,
  pendingCheckpointName,
  isOffRouteSimActive,
  isStragglerSimActive,
  isLeaderTroubleSimActive,
  canPingRouting,
  isEffectiveLeader,
  destinationPresets,
  darkMode,
  gpsStatus,
  gpsAccuracyMeters,
  gpsErrorMessage,
  onRetryGps,
  onRouteComputed,
  onSelectMember,
  onToggleSimulation,
  onSelectPreset,
  onPickDestinationLatLng,
  onTogglePickMode,
  onStartPickCheckpoint,
  onCancelPickCheckpoint,
  onConfirmAddCheckpointLatLng,
  onRemoveCheckpoint,
  onUpdateMaxStragglerDistance,
  onUpdateOffRouteTolerance,
  onToggleSoundAlert,
  onToggleOffRouteSim,
  onToggleStragglerSim,
  onToggleLeaderTroubleSim,
  onReclaimLeader,
  onTriggerSos,
  onResolveSos,
  onSendTextMessage,
  onStartVoiceRecord,
  onStopVoiceRecord,
  onToggleVoicePlay,
  onUpdateOwnCondition,
  onAssignSweeper,
  onSwitchPreviewRole,
  onFinishAndSaveTrip,
  onExitRoom,
  manasikProgress,
  isManasikCloudSynced = false,
  onToggleManasikStep,
  onCheckInManasikStep,
  onResetManasikGuide,
  onOpenManasikAssistant,
  roomActivities = [],
  roomActivityProgress = {},
  currentMemberUid = '',
  onToggleRoomActivity,
  onAddRoomActivity,
  onRemoveRoomActivity,
  onMoveRoomActivity,
  onApplyRoomStarterTemplates,
  batterySaverMode = false,
  onToggleBatterySaverMode,
  isOnline = true,
  offlineQueueCount = 0,
  onFlushOfflineQueue,
  dismissedBroadcastId = '',
  onDismissBroadcast,
  onSendPriorityBroadcast,
  onClearPriorityBroadcast,
}: LiveTrackingMapViewProps) {
  const [isNavigationMode, setIsNavigationMode] = useState(true);
  const [isNavInfoHidden, setIsNavInfoHidden] = useState(true);
  const [showSideInfoDrawer, setShowSideInfoDrawer] = useState(false);
  const [sideInfoTouchTick, setSideInfoTouchTick] = useState(0);
  const [sideInfoSecondsLeft, setSideInfoSecondsLeft] = useState(7);
  const [participantsExpanded, setParticipantsExpanded] = useState(false);
  const [showChatSheet, setShowChatSheet] = useState(false);
  const [showManasikSheet, setShowManasikSheet] = useState(false);
  const [showRoomActivitiesSheet, setShowRoomActivitiesSheet] = useState(false);
  const [showLeaderSettings, setShowLeaderSettings] = useState(false);
  const [showShareRoomModal, setShowShareRoomModal] = useState(false);
  const [shareCopiedType, setShareCopiedType] = useState<
    'id' | 'web' | 'link' | null
  >(null);
  const placesLib = useMapsLibrary('places');
  const destSessionTokenRef = useRef<
    google.maps.places.AutocompleteSessionToken | undefined
  >(undefined);
  const [destSearchQuery, setDestSearchQuery] = useState('');
  const [destSearchResults, setDestSearchResults] = useState<
    GooglePlaceSuggestion[]
  >([]);
  const [isSearchingDest, setIsSearchingDest] = useState(false);
  const [showSosConfirm, setShowSosConfirm] = useState(false);
  const [showFinishTripConfirm, setShowFinishTripConfirm] = useState(false);
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  const [broadcastTitle, setBroadcastTitle] = useState('');
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [chatText, setChatText] = useState('');
  const [dismissedSosKey, setDismissedSosKey] = useState('');
  const [sosFlashSecondsLeft, setSosFlashSecondsLeft] = useState(10);

  const currentSosKey = sosState.isActive
    ? `${sosState.senderId}|${sosState.triggeredAtLabel}`
    : '';
  const showSosFlashOverlay =
    Boolean(currentSosKey) && currentSosKey !== dismissedSosKey;

  useEffect(() => {
    if (!currentSosKey) {
      setDismissedSosKey('');
      setSosFlashSecondsLeft(10);
      return;
    }
    if (currentSosKey === dismissedSosKey) return;
    setSosFlashSecondsLeft(10);
    const intervalId = window.setInterval(() => {
      setSosFlashSecondsLeft((prev) => {
        if (prev <= 1) {
          window.clearInterval(intervalId);
          setDismissedSosKey(currentSosKey);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => window.clearInterval(intervalId);
  }, [currentSosKey, dismissedSosKey]);

  useEffect(() => {
    if (!showLeaderSettings || !placesLib) return;
    const trimmed = destSearchQuery.trim();
    if (trimmed.length < 2) {
      setDestSearchResults([]);
      setIsSearchingDest(false);
      return;
    }
    let cancelled = false;
    setIsSearchingDest(true);
    if (!destSessionTokenRef.current) {
      destSessionTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
    const timer = window.setTimeout(async () => {
      try {
        const res = await searchGoogleAutocompleteSuggestions(
          placesLib,
          trimmed,
          destSessionTokenRef.current
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
  }, [placesLib, destSearchQuery, showLeaderSettings]);

  const getWebRoomShareLink = (roomId: string) => {
    if (typeof window === 'undefined') {
      return `https://safartracking.id/join/${encodeURIComponent(roomId)}`;
    }
    return `${window.location.origin}/?room=${encodeURIComponent(roomId)}`;
  };

  const handleCopyShare = async (
    text: string,
    type: 'id' | 'web' | 'link'
  ) => {
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      // Ignore clipboard failure
    }
    setShareCopiedType(type);
    window.setTimeout(() => setShareCopiedType(null), 1800);
  };

  const handleNativeShareRoom = async () => {
    if (!activeRoom) return;
    const webLink = getWebRoomShareLink(activeRoom.roomId);
    const shareText = `Gabung rombongan umroh "${activeRoom.roomTitle}" di Safar Tracking!\nKode Grup: ${activeRoom.roomId}\nLink Browser: ${webLink}\nLink APK: ${activeRoom.inviteLink}`;
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        await navigator.share({
          title: `Undangan Rombongan ${activeRoom.roomId}`,
          text: shareText,
          url: webLink,
        });
        return;
      } catch {
        // Fallback to copy
      }
    }
    await handleCopyShare(webLink, 'web');
  };

  const chatScrollRef = useRef<HTMLDivElement | null>(null);
  const prevAlertKeyRef = useRef('');

  // Auto-transition to Clean Turn-by-Turn GPS Navigation Mode whenever a convoy starts/opens
  useEffect(() => {
    if (activeRoom?.roomId) {
      setIsNavigationMode(true);
      setIsNavInfoHidden(true);
      setShowSideInfoDrawer(false);
    }
  }, [activeRoom?.roomId]);

  // Auto-hide Side Info Drawer after 7 seconds of inactivity
  useEffect(() => {
    if (!showSideInfoDrawer) return;
    setSideInfoSecondsLeft(7);
    const intervalId = window.setInterval(() => {
      setSideInfoSecondsLeft((prev) => {
        if (prev <= 1) {
          window.clearInterval(intervalId);
          setShowSideInfoDrawer(false);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => window.clearInterval(intervalId);
  }, [showSideInfoDrawer, sideInfoTouchTick]);

  const touchSideDrawer = () => setSideInfoTouchTick((t) => t + 1);
  const openSideDrawer = () => {
    setShowSideInfoDrawer(true);
    setSideInfoTouchTick((t) => t + 1);
  };

  const members = [...(activeRoom?.members ?? [])].sort(
    (a, b) => a.convoyOrderRank - b.convoyOrderRank
  );
  const focusedMember =
    members.find((m) => m.id === selectedMemberId) || members[0] || null;
  const currentUser = members.find((m) => m.isCurrentUser) || members[0] || null;
  const trackedMember = focusedMember || currentUser;
  const actingLeaderMember =
    members.find((m) => m.isActingLeader) ||
    members.find((m) => m.id === activeRoom?.actingLeaderUid) ||
    null;
  const coLeaderMember =
    members.find((m) => m.isCoLeader) ||
    members.find((m) => m.id === activeRoom?.coLeaderUid) ||
    null;
  const sweeperMember = members.find((m) => m.isSweeper) || null;
  const effectiveActivityUid = currentMemberUid || currentUser?.id || '';
  const myCompletedActivitySet = new Set(
    effectiveActivityUid
      ? (roomActivityProgress[effectiveActivityUid]?.completedActivityIds ?? [])
      : []
  );
  const roomActivitiesDoneCount = roomActivities.filter((a) =>
    myCompletedActivitySet.has(a.id)
  ).length;
  const etaInfo = calculateDestinationEta(trackedMember, routeConfig);

  const activeSpeedValues = members
    .filter((m) => m.conditionStatus !== 'MOGOK' && m.conditionStatus !== 'SOS')
    .map((m) => Math.max(0, m.mapPosition.speedKmh));
  const convoyAvgSpeedKmh =
    activeSpeedValues.length > 0
      ? Math.round(
          activeSpeedValues.reduce((sum, v) => sum + v, 0) /
            activeSpeedValues.length
        )
      : 0;

  const [speedHistory, setSpeedHistory] = useState<SpeedHistoryPoint[]>(() =>
    buildInitialSpeedHistory(convoyAvgSpeedKmh)
  );

  useEffect(() => {
    const nowMs = Date.now();
    const currentBucket = Math.floor(nowMs / 60_000);
    setSpeedHistory((prev) => {
      const base =
        prev.length > 0 ? [...prev] : buildInitialSpeedHistory(convoyAvgSpeedKmh, nowMs);
      const last = base[base.length - 1];
      if (last && last.bucketMinute === currentBucket) {
        base[base.length - 1] = {
          ...last,
          timeLabel: 'Skgr',
          avgSpeed: convoyAvgSpeedKmh,
        };
      } else {
        base.push({
          bucketMinute: currentBucket,
          timeLabel: 'Skgr',
          avgSpeed: convoyAvgSpeedKmh,
        });
      }
      const cutoff = currentBucket - 9;
      return base
        .filter((pt) => pt.bucketMinute >= cutoff)
        .slice(-10)
        .map((pt, idx, arr) => ({
          ...pt,
          timeLabel:
            idx === arr.length - 1
              ? 'Skgr'
              : formatClockHHmm(new Date(pt.bucketMinute * 60_000)),
        }));
    });
  }, [convoyAvgSpeedKmh, activeRoom?.roomId]);

  const avgTenMinSpeed =
    speedHistory.length > 0
      ? Math.round(
          speedHistory.reduce((sum, pt) => sum + pt.avgSpeed, 0) /
            speedHistory.length
        )
      : convoyAvgSpeedKmh;
  const firstTenMinSpeed = speedHistory[0]?.avgSpeed ?? avgTenMinSpeed;
  const speedDeltaTenMin = convoyAvgSpeedKmh - firstTenMinSpeed;

  useEffect(() => {
    const key = `${offRouteAlert.isOffRoute}-${stragglerAlert.isTriggered}-${currentSosKey}`;
    if (
      routeConfig.soundAlertEnabled &&
      (offRouteAlert.isOffRoute || stragglerAlert.isTriggered || sosState.isActive) &&
      key !== prevAlertKeyRef.current
    ) {
      playAlertTone(sosState.isActive);
    }
    prevAlertKeyRef.current = key;
  }, [
    offRouteAlert.isOffRoute,
    stragglerAlert.isTriggered,
    sosState.isActive,
    currentSosKey,
    routeConfig.soundAlertEnabled,
  ]);

  useEffect(() => {
    if (showChatSheet && chatScrollRef.current) {
      chatScrollRef.current.scrollTop = chatScrollRef.current.scrollHeight;
    }
  }, [messages.length, showChatSheet]);

  const handlePickCoordinateOnMap = (lat: number, lng: number) => {
    if (isPickingDestinationOnMap) {
      onPickDestinationLatLng(lat, lng);
    }
  };

  const handleSendChat = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatText.trim()) return;
    onSendTextMessage(chatText);
    setChatText('');
  };

  const sliderKm = Math.max(
    0.5,
    Math.min(5.0, routeConfig.maxStragglerDistanceMeters / 1000)
  );
  const activeNavMode = isNavigationMode && !isPickingDestinationOnMap;
  const isCleanNavHud = activeNavMode && isNavInfoHidden;

  return (
    <div
      id="live_map"
      className="relative flex flex-col h-[680px] bg-[#F4F8F5] dark:bg-[#091812] text-[#0B1F16] dark:text-[#ECFDF5] overflow-hidden"
    >
      {/* Top App Bar */}
      <div className="shrink-0 flex items-center justify-between gap-1.5 px-2.5 py-2 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="flex items-center gap-1.5 min-w-0 flex-1">
          <button
            type="button"
            onClick={onExitRoom}
            aria-label="Keluar Grup"
            className="w-9 h-9 rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="min-w-0 flex-1">
            <h2 className="text-[13px] font-bold font-mono truncate">
              {activeRoom?.roomId || 'UMR-8492'} · Live
            </h2>
            <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              {routeConfig.destinationName}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1 shrink-0">
          <button
            type="button"
            onClick={() =>
              setIsNavigationMode((prev) => {
                const next = !prev;
                if (next) setIsNavInfoHidden(true);
                return next;
              })
            }
            className={`min-h-[32px] px-2 py-1 rounded-lg text-[11px] font-semibold transition-colors cursor-pointer whitespace-nowrap flex items-center gap-1 ${
              isNavigationMode
                ? 'bg-[#B45309] text-white'
                : 'bg-[#FFFBEB] dark:bg-[#451A03] text-[#92400E] dark:text-[#FCD34D] border border-[#FDE68A] dark:border-[#92400E]'
            }`}
          >
            <Navigation className="w-3 h-3 shrink-0" />
            <span>{isNavigationMode ? 'Pantau' : 'Navigasi'}</span>
          </button>

          {activeNavMode && (
            <button
              type="button"
              onClick={() => setIsNavInfoHidden((v) => !v)}
              aria-label={
                isNavInfoHidden
                  ? 'Tampilkan Info Lengkap di Layar'
                  : 'Sembunyikan Info (Layar Navigasi Bersih)'
              }
              title={
                isNavInfoHidden
                  ? 'Tampilkan Info Lengkap di Layar'
                  : 'Layar Navigasi Bersih'
              }
              className={`w-8 h-8 rounded-lg text-[11px] font-semibold transition-colors cursor-pointer flex items-center justify-center border shrink-0 ${
                isNavInfoHidden
                  ? 'bg-[#0B1F16] dark:bg-[#133025] text-white border-[#1B3B2E]'
                  : 'bg-[#ECFDF5] dark:bg-[#064E3B]/50 text-[#047857] dark:text-[#A7F3D0] border-[#A7F3D0] dark:border-[#065F46]'
              }`}
            >
              {isNavInfoHidden ? (
                <EyeOff className="w-3.5 h-3.5 shrink-0" />
              ) : (
                <Eye className="w-3.5 h-3.5 shrink-0" />
              )}
            </button>
          )}

          <button
            type="button"
            onClick={() => setShowFinishTripConfirm(true)}
            className="min-h-[32px] px-2 py-1 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B] text-[11px] font-semibold text-[#047857] dark:text-[#6EE7B7] transition-colors cursor-pointer whitespace-nowrap"
          >
            Selesai
          </button>

          {isEffectiveLeader && (
            <button
              type="button"
              onClick={() => setShowLeaderSettings(true)}
              aria-label="Pengaturan Rute & Batas Terpisah"
              className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 text-[#047857] dark:text-[#34D399] transition-colors cursor-pointer shrink-0"
            >
              <Settings className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>

      {/* Collapsible Top Participant Strip & Overlay Drawer (Hidden during Clean Navigation Mode) */}
      {!isCleanNavHud && (
        <div className="relative shrink-0 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E] z-[25]">
        <button
          type="button"
          onClick={() => setParticipantsExpanded((prev) => !prev)}
          className="w-full px-3.5 py-2 flex items-center justify-between text-xs font-semibold text-[#047857] dark:text-[#34D399] cursor-pointer"
        >
          <span className="truncate">
            Jamaah ({members.length})
            {actingLeaderMember && activeRoom?.leaderTroubleActive
              ? ` · Plt.Pembimbing: ${actingLeaderMember.fullName}`
              : coLeaderMember
                ? ` · Wakil: ${coLeaderMember.fullName}`
                : ''}
            {sweeperMember
              ? ` · Pendamping: ${sweeperMember.fullName}${sweeperMember.isAutoSweeper ? ' (Auto)' : ''}`
              : ''}
          </span>
          {participantsExpanded ? (
            <ChevronUp className="w-4 h-4 shrink-0" />
          ) : (
            <ChevronDown className="w-4 h-4 shrink-0" />
          )}
        </button>

        {!participantsExpanded ? (
          <div className="px-3.5 pb-2.5 flex items-center gap-2 overflow-x-auto no-scrollbar">
            {members.map((member) => {
              const selected = member.id === focusedMember?.id;
              return (
                <button
                  key={member.id}
                  type="button"
                  onClick={() => onSelectMember(member.id)}
                  className={`shrink-0 px-2.5 py-1.5 rounded-[14px] border flex items-center gap-2 text-left transition-colors cursor-pointer ${
                    selected
                      ? 'bg-[#ECFDF5] dark:bg-[#064E3B]/60 border-[#047857]'
                      : member.isActingLeader
                        ? 'bg-[#ECFDF5] dark:bg-[#065F46]/40 border-[#059669]'
                        : member.isSweeper
                          ? 'bg-[#FFFBEB] dark:bg-[#451A03]/40 border-[#D97706]'
                          : member.isCoLeader
                            ? 'bg-[#F0FDF4] dark:bg-[#14532D]/40 border-[#15803D]'
                            : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                  }`}
                >
                  <ConvoyAvatar
                    preset={member.avatarPreset}
                    photoUrl={member.avatarPhotoUrl}
                    name={member.fullName}
                    sizeClass="w-7 h-7"
                    ringClass={
                      member.isActingLeader
                        ? 'ring-2 ring-[#059669]'
                        : member.isSweeper
                          ? 'ring-2 ring-[#D97706]'
                          : member.isCoLeader
                            ? 'ring-2 ring-[#15803D]'
                            : member.isCurrentUser
                              ? 'ring-2 ring-[#B45309]'
                              : 'ring-2 ring-white dark:ring-[#133025]'
                    }
                  />
                  <div className="min-w-0">
                    <div className="flex items-center gap-1">
                      <p className="text-[11px] font-semibold truncate max-w-[78px]">
                        {member.fullName}
                      </p>
                      {member.isActingLeader && (
                        <span className="px-1 py-0.2 rounded bg-[#059669] text-white text-[8px] font-bold">
                          Plt
                        </span>
                      )}
                      {!member.isActingLeader && member.isCoLeader && (
                        <span className="px-1 py-0.2 rounded bg-[#065F46] text-white text-[8px] font-bold">
                          Wakil
                        </span>
                      )}
                      {member.isSweeper && (
                        <span className="px-1 py-0.2 rounded bg-[#B45309] text-white text-[8px] font-bold">
                          {member.isAutoSweeper ? 'Pdp·Auto' : 'Pdp'}
                        </span>
                      )}
                    </div>
                    <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] flex items-center gap-1">
                      {member.vehicleType === 'MOBIL' ? (
                        <Bus className="w-3 h-3 shrink-0" />
                      ) : (
                        <Footprints className="w-3 h-3 shrink-0" />
                      )}
                      <span>
                        {member.isSweeper
                          ? member.isAutoSweeper
                            ? 'Pendamping (Auto)'
                            : 'Pendamping'
                          : member.isCoLeader && typeof member.distanceToLeaderMeters === 'number'
                            ? `${formatDistanceLabel(member.distanceToLeaderMeters)}`
                            : member.vehicleType === 'MOBIL'
                              ? 'Bus Rombongan'
                              : 'Jalan Kaki'}
                      </span>
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        ) : (
          <div className="absolute left-0 right-0 top-full z-30 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E] shadow-xl px-3.5 pb-3 pt-1.5 space-y-2.5">
            {/* Self Condition Quick Buttons */}
            <div className="grid grid-cols-3 gap-1.5">
              {(
                [
                  { status: 'AMAN', label: 'Sehat' },
                  { status: 'ISTIRAHAT', label: 'Istirahat' },
                  { status: 'MOGOK', label: 'Perlu Bantuan' },
                ] as Array<{ status: MemberConditionStatus; label: string }>
              ).map((item) => {
                const active = currentUser?.conditionStatus === item.status;
                return (
                  <button
                    key={item.status}
                    type="button"
                    onClick={() => onUpdateOwnCondition(item.status)}
                    className={`py-1.5 rounded-xl text-[11px] font-semibold border transition-colors cursor-pointer ${
                      active
                        ? item.status === 'MOGOK'
                          ? 'bg-[#FEF2F2] dark:bg-[#450A0A] border-[#DC2626] text-[#DC2626] dark:text-[#F87171]'
                          : item.status === 'ISTIRAHAT'
                            ? 'bg-[#FFFBEB] dark:bg-[#451A03] border-[#D97706] text-[#B45309] dark:text-[#FBBF24]'
                            : 'bg-[#ECFDF5] dark:bg-[#064E3B] border-[#047857] text-[#047857] dark:text-[#6EE7B7]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E] text-[#2F4A3C] dark:text-[#D1E7DA]'
                    }`}
                  >
                    {item.label}
                  </button>
                );
              })}
            </div>

            <div className="max-h-[168px] overflow-y-auto space-y-1.5">
              {members.map((member) => {
                const selected = member.id === focusedMember?.id;
                const condColor =
                  member.conditionStatus === 'AMAN'
                    ? 'text-[#047857] dark:text-[#34D399]'
                    : member.conditionStatus === 'ISTIRAHAT'
                      ? 'text-[#B45309] dark:text-[#FBBF24]'
                      : 'text-[#DC2626] dark:text-[#F87171]';

                return (
                  <div
                    key={member.id}
                    onClick={() => {
                      onSelectMember(member.id);
                      setParticipantsExpanded(false);
                    }}
                    className={`rounded-xl border px-3 py-2 flex items-center gap-2.5 cursor-pointer ${
                      selected
                        ? 'bg-[#ECFDF5] dark:bg-[#064E3B]/40 border-[#047857]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                    }`}
                  >
                    <ConvoyAvatar
                      preset={member.avatarPreset}
                      photoUrl={member.avatarPhotoUrl}
                      name={member.fullName}
                      sizeClass="w-9 h-9"
                      ringClass={
                        member.isCurrentUser
                          ? 'ring-2 ring-[#B45309]'
                          : 'ring-2 ring-[#047857]'
                      }
                    />

                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-semibold truncate">
                        {getMarkerTitle(member)}
                        {member.isCurrentUser ? ' (Anda)' : ''}
                      </p>
                      <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                        {member.isActingLeader
                          ? 'Plt. Pembimbing (Failover)'
                          : member.isCoLeader && member.isSweeper
                            ? `Wakil & ${member.isAutoSweeper ? 'Pendamping (Auto)' : 'Pendamping'}`
                            : member.isCoLeader
                              ? `Wakil (${formatDistanceLabel(member.distanceToLeaderMeters ?? 0)} dari Mutawwif)`
                              : member.isSweeper
                                ? member.isAutoSweeper
                                  ? 'Pendamping (Auto · Paling Belakang)'
                                  : 'Pendamping'
                                : member.role === 'TOUR_LEADER'
                                  ? activeRoom?.leaderTroubleActive
                                    ? 'Mutawwif (Kendala HP)'
                                    : 'Mutawwif'
                                  : 'Jamaah'}{' '}
                        · {member.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'} ·{' '}
                        {member.lastLocationText}
                      </p>
                      {isEffectiveLeader && member.role !== 'TOUR_LEADER' && !member.isActingLeader && (
                        <button
                          type="button"
                          onClick={(e) => {
                            e.stopPropagation();
                            onAssignSweeper(member.id);
                          }}
                          className="mt-0.5 inline-flex items-center gap-1 text-[10px] font-semibold text-[#047857] dark:text-[#34D399] hover:underline cursor-pointer"
                        >
                          <Shield className="w-3 h-3" />
                          <span>
                            {member.isSweeper && !member.isAutoSweeper
                              ? 'Pendamping Aktif'
                              : member.isAutoSweeper
                                ? 'Tetapkan Pendamping'
                                : 'Jadikan Pendamping'}
                          </span>
                        </button>
                      )}
                    </div>

                    <div className="text-right shrink-0">
                      <div
                        className={`flex items-center justify-end gap-1 text-[11px] font-semibold ${condColor}`}
                      >
                        {member.conditionStatus === 'AMAN' && (
                          <CheckCircle2 className="w-3 h-3" />
                        )}
                        {member.conditionStatus === 'ISTIRAHAT' && (
                          <Landmark className="w-3 h-3" />
                        )}
                        {member.conditionStatus === 'MOGOK' && (
                          <HeartPulse className="w-3 h-3" />
                        )}
                        {member.conditionStatus === 'SOS' && (
                          <AlertTriangle className="w-3 h-3" />
                        )}
                        <span>{conditionLabel(member.conditionStatus)}</span>
                      </div>
                      {(() => {
                        const memberEta = calculateDestinationEta(member, routeConfig);
                        return (
                          <>
                            <p className="text-[10px] font-mono text-[#4D6659] dark:text-[#9CC2AE] tabular-nums">
                              {member.mapPosition.speedKmh} km/j · ETA{' '}
                              {memberEta.durationLabel}
                            </p>
                            <p className="text-[10px] font-mono text-[#047857] dark:text-[#34D399] tabular-nums">
                              Sisa {memberEta.distanceLabel}
                            </p>
                          </>
                        );
                      })()}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}
        </div>
      )}

      {/* Off-Route Banner */}
      {offRouteAlert.isOffRoute && (
        <div
          role="alert"
          className="shrink-0 bg-[#DC2626] text-white px-3.5 py-2 flex items-center gap-2"
        >
          <AlertTriangle className="w-4 h-4 shrink-0" />
          <p className="text-[11px] font-semibold truncate">
            KELUAR JALUR · Menyimpang {offRouteAlert.deviationMeters} m dari rute resmi (batas {offRouteAlert.toleranceMeters} m)
          </p>
        </div>
      )}

      {/* Straggler Alert Banner */}
      {stragglerAlert.isTriggered &&
        stragglerAlert.trailingMember &&
        stragglerAlert.aheadMember && (
          <div
            role="alert"
            className="shrink-0 bg-[#B45309] text-white px-3.5 py-2 flex items-center gap-2"
          >
            <BellRing className="w-4 h-4 shrink-0" />
            <p className="text-[11px] font-semibold truncate flex-1">
              TERPISAH {formatDistanceLabel(stragglerAlert.gapMeters)} ·{' '}
              {stragglerAlert.trailingMember.fullName} di belakang{' '}
              {stragglerAlert.aheadMember.fullName}
            </p>
            {routeConfig.soundAlertEnabled && (
              <Volume2 className="w-4 h-4 shrink-0" />
            )}
          </div>
        )}

      {/* Leader Trouble & Co-Leader Failover Banner */}
      {activeRoom?.leaderTroubleActive && (
        <div
          role="alert"
          className="shrink-0 bg-[#065F46] text-white px-3.5 py-2 flex items-center gap-2.5"
        >
          <Shield className="w-4 h-4 shrink-0 text-[#A7F3D0]" />
          <p className="text-[11px] font-semibold truncate flex-1">
            {activeRoom.leaderTroubleReason || 'Kendala HP Mutawwif (sinyal/baterai)'} · Wakil{' '}
            {actingLeaderMember?.fullName || 'Pengganti'} mengambil alih rute &amp; navigasi
          </p>
          {(role === 'TOUR_LEADER' ||
            (currentUser && currentUser.id === activeRoom.leaderUid)) && (
            <button
              type="button"
              onClick={onReclaimLeader}
              className="px-2 py-1 rounded-lg bg-white/20 hover:bg-white/30 text-[10px] font-bold cursor-pointer shrink-0"
            >
              Pulihkan Mutawwif
            </button>
          )}
        </div>
      )}

      {/* Priority Broadcast Banner (Visible to all jamaah until dismissed or cleared) */}
      {activeRoom?.priorityBroadcast?.isActive &&
        activeRoom.priorityBroadcast.id !== dismissedBroadcastId && (
          <div
            role="alert"
            className="shrink-0 bg-gradient-to-r from-[#B45309] to-[#D97706] text-white px-3.5 py-2.5 flex items-start gap-2.5 shadow-md"
          >
            <div className="w-7 h-7 rounded-xl bg-white/20 flex items-center justify-center shrink-0 mt-0.5">
              <Megaphone className="w-4 h-4 text-white" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-1.5 flex-wrap">
                <span className="text-[10px] font-extrabold uppercase tracking-wider bg-black/20 px-1.5 py-0.5 rounded-md">
                  Pengumuman Prioritas
                </span>
                <span className="text-[10px] text-white/90 font-mono">
                  {activeRoom.priorityBroadcast.senderName} ·{' '}
                  {activeRoom.priorityBroadcast.atLabel || 'Baru saja'}
                </span>
              </div>
              <p className="text-xs font-bold leading-snug mt-0.5">
                {activeRoom.priorityBroadcast.title}
              </p>
              <p className="text-[11px] text-white/95 leading-snug">
                {activeRoom.priorityBroadcast.message}
              </p>
            </div>
            <div className="flex flex-col gap-1 shrink-0">
              <button
                type="button"
                onClick={() =>
                  onDismissBroadcast?.(activeRoom.priorityBroadcast?.id || '')
                }
                className="px-2 py-1 rounded-lg bg-white text-[#92400E] hover:bg-[#FEF3C7] text-[10px] font-bold cursor-pointer whitespace-nowrap"
              >
                Sudah Dibaca
              </button>
              {isEffectiveLeader && onClearPriorityBroadcast && (
                <button
                  type="button"
                  onClick={onClearPriorityBroadcast}
                  className="px-2 py-1 rounded-lg bg-black/25 hover:bg-black/35 text-white text-[10px] font-semibold cursor-pointer whitespace-nowrap"
                >
                  Tutup di Room
                </button>
              )}
            </div>
          </div>
        )}

      {/* Full Live Google Maps Viewport */}
      <div className="relative flex-1 min-h-0 overflow-hidden bg-[#EBF3EE] dark:bg-[#091812]">
        <div className="absolute inset-0">
          <LiveGoogleConvoyMap
            members={members}
            focusedMember={focusedMember}
            routeConfig={routeConfig}
            vehicleType={vehicleType || currentUser?.vehicleType || 'MOTOR'}
            stragglerAlert={stragglerAlert}
            sosState={sosState}
            etaInfo={etaInfo}
            darkMode={darkMode}
            isPickingDestination={isPickingDestinationOnMap}
            isPickingCheckpoint={false}
            isNavigationMode={isNavigationMode}
            cleanNavMode={isCleanNavHud}
            canPingRouting={canPingRouting}
            onToggleNavigationMode={() =>
              setIsNavigationMode((prev) => {
                const next = !prev;
                if (next) setIsNavInfoHidden(true);
                return next;
              })
            }
            onSelectMember={onSelectMember}
            onPickCoordinateOnMap={handlePickCoordinateOnMap}
            onRouteComputed={onRouteComputed}
          />
        </div>

        {/* Single Compact GPS Status Chip (clear of Turn-by-Turn HUD and Speedometer) */}
        {(!isCleanNavHud || gpsStatus === 'denied' || !isOnline || offlineQueueCount > 0) && (
          <div
            className={`absolute left-2.5 z-20 flex flex-col gap-1 max-w-[74%] ${
              activeNavMode ? 'top-[76px]' : 'top-2.5'
            }`}
          >
            <div className="flex flex-wrap items-center gap-1">
              {(!isCleanNavHud || gpsStatus === 'denied') && (
                <button
                  type="button"
                  onClick={onRetryGps}
                  className={`w-fit px-2.5 py-1 rounded-xl text-[10px] font-semibold border shadow-sm flex items-center gap-1.5 cursor-pointer ${
                    gpsStatus === 'active'
                      ? 'bg-[#ECFDF5]/95 dark:bg-[#064E3B]/95 text-[#047857] dark:text-[#6EE7B7] border-[#A7F3D0] dark:border-[#065F46]'
                      : gpsStatus === 'denied' || gpsStatus === 'unavailable'
                        ? 'bg-[#FEF2F2]/95 dark:bg-[#450A0A]/95 text-[#DC2626] dark:text-[#FCA5A5] border-[#FCA5A5] dark:border-[#991B1B]'
                        : 'bg-white/95 dark:bg-[#0C1E17]/95 text-[#047857] dark:text-[#34D399] border-[#D8E6DD] dark:border-[#1B3B2E]'
                  }`}
                >
                  <LocateFixed className="w-3 h-3 shrink-0" />
                  <span>
                    {gpsStatus === 'active'
                      ? `GPS ±${gpsAccuracyMeters ?? 15}m`
                      : gpsStatus === 'requesting'
                        ? 'Mencari GPS...'
                        : gpsStatus === 'denied'
                          ? 'Izin GPS Ditolak (Ketuk)'
                          : gpsStatus === 'unavailable'
                            ? 'GPS Tidak Tersedia'
                            : 'GPS Siap'}
                  </span>
                  {batterySaverMode && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span className="inline-flex items-center gap-0.5 text-[#B45309] dark:text-[#FBBF24]">
                        <BatteryCharging className="w-3 h-3" />
                        Hemat
                      </span>
                    </>
                  )}
                  {activeRoom?.isSyncedRoom && !batterySaverMode && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span>Sync</span>
                    </>
                  )}
                </button>
              )}

              {(!isOnline || offlineQueueCount > 0) && (
                <button
                  type="button"
                  onClick={() => onFlushOfflineQueue?.()}
                  className="w-fit px-2.5 py-1 rounded-xl text-[10px] font-semibold border shadow-sm flex items-center gap-1 cursor-pointer bg-[#FFFBEB]/95 dark:bg-[#451A03]/95 text-[#B45309] dark:text-[#FCD34D] border-[#FCD34D] dark:border-[#92400E]"
                >
                  {isOnline ? (
                    <RefreshCw className="w-3 h-3 shrink-0" />
                  ) : (
                    <WifiOff className="w-3 h-3 shrink-0" />
                  )}
                  <span>
                    {!isOnline
                      ? offlineQueueCount > 0
                        ? `Offline · Antrean ${offlineQueueCount}`
                        : 'Mode Offline'
                      : `Antrean ${offlineQueueCount} (Sinkronkan)`}
                  </span>
                </button>
              )}
            </div>
            {gpsStatus === 'denied' && gpsErrorMessage && !isCleanNavHud && (
              <div className="px-2.5 py-1 rounded-xl bg-[#FEF2F2]/95 dark:bg-[#450A0A]/95 border border-[#FCA5A5] dark:border-[#991B1B] text-[10px] leading-snug text-[#991B1B] dark:text-[#FECACA] shadow-sm">
                {gpsErrorMessage}
              </div>
            )}
          </div>
        )}

        {/* Collapsible Auto-Hiding Side Info Drawer (7s inactivity auto-collapse) */}
        {showSideInfoDrawer && (
          <div
            onClick={() => setShowSideInfoDrawer(false)}
            aria-hidden="true"
            className="absolute inset-0 z-30 bg-black/25 backdrop-blur-[1px]"
          />
        )}
        <aside
          aria-label="Panel Info & Data Perjalanan"
          onPointerDown={touchSideDrawer}
          onScroll={touchSideDrawer}
          className={`absolute inset-y-0 left-0 z-30 w-[294px] max-w-[85%] bg-white/98 dark:bg-[#0C1E17]/98 backdrop-blur-md border-r border-[#D8E6DD] dark:border-[#1B3B2E] shadow-2xl flex flex-col transition-transform duration-200 ease-out ${
            showSideInfoDrawer
              ? 'translate-x-0'
              : '-translate-x-full pointer-events-none'
          }`}
        >
          {/* Drawer Header with Auto-Hide Countdown */}
          <div className="shrink-0 px-3.5 py-2.5 border-b border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center justify-between gap-2">
            <div className="min-w-0">
              <div className="flex items-center gap-1.5">
                <Info className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
                <h3 className="text-xs font-bold truncate">
                  Info & Data Perjalanan
                </h3>
              </div>
              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] font-mono tabular-nums mt-0.5">
                {activeRoom?.roomId || 'UMR-8492'} · Auto-hide {sideInfoSecondsLeft}d
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowSideInfoDrawer(false)}
              aria-label="Tutup Panel Info"
              className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#133025] cursor-pointer shrink-0"
            >
              <X className="w-4 h-4" />
            </button>
          </div>

          <div
            onScroll={touchSideDrawer}
            className="flex-1 overflow-y-auto px-3.5 py-3 space-y-3.5 text-xs"
          >
            {/* Clean Navigation HUD Toggle */}
            <div className="flex items-center justify-between gap-2 pb-2.5 border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
              <div className="min-w-0">
                <p className="text-[11px] font-semibold">
                  Layar Navigasi Bersih
                </p>
                <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Sembunyikan strip & grafik saat navigasi
                </p>
              </div>
              <button
                type="button"
                role="switch"
                aria-checked={isNavInfoHidden}
                onClick={() => {
                  touchSideDrawer();
                  setIsNavInfoHidden((v) => !v);
                }}
                className={`w-10 h-6 rounded-full p-0.5 transition-colors cursor-pointer shrink-0 ${
                  isNavInfoHidden
                    ? 'bg-[#047857]'
                    : 'bg-[#CFE0D5] dark:bg-[#1B3B2E]'
                }`}
              >
                <span
                  className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                    isNavInfoHidden ? 'translate-x-4' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>

            {/* Battery Saver Mode & Offline Crowd-Signal Queue */}
            <div className="space-y-2.5 pb-2.5 border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
              <div className="flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    <BatteryCharging className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FBBF24] shrink-0" />
                    <p className="text-[11px] font-semibold">Mode Hemat Baterai</p>
                  </div>
                  <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                    Interval GPS adaptif 10–20 dtk saat berhenti
                  </p>
                </div>
                <button
                  type="button"
                  role="switch"
                  aria-checked={batterySaverMode}
                  onClick={() => {
                    touchSideDrawer();
                    onToggleBatterySaverMode?.();
                  }}
                  className={`w-10 h-6 rounded-full p-0.5 transition-colors cursor-pointer shrink-0 ${
                    batterySaverMode
                      ? 'bg-[#B45309]'
                      : 'bg-[#CFE0D5] dark:bg-[#1B3B2E]'
                  }`}
                >
                  <span
                    className={`block w-5 h-5 rounded-full bg-white transition-transform ${
                      batterySaverMode ? 'translate-x-4' : 'translate-x-0'
                    }`}
                  />
                </button>
              </div>

              <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-2.5 py-2 flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5">
                    {isOnline ? (
                      <Wifi className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
                    ) : (
                      <WifiOff className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FBBF24] shrink-0" />
                    )}
                    <p className="text-[11px] font-semibold truncate">
                      Antrean Sinyal Padat
                    </p>
                  </div>
                  <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                    {isOnline ? 'Online' : 'Offline'} · {offlineQueueCount} data menunggu
                  </p>
                </div>
                <button
                  type="button"
                  disabled={offlineQueueCount === 0}
                  onClick={() => {
                    touchSideDrawer();
                    onFlushOfflineQueue?.();
                  }}
                  className="px-2.5 py-1.5 rounded-lg bg-[#047857] disabled:bg-[#CFE0D5] dark:disabled:bg-[#1B3B2E] text-white disabled:text-[#4D6659] text-[10px] font-semibold cursor-pointer shrink-0"
                >
                  Sinkronkan
                </button>
              </div>

              {isEffectiveLeader && onSendPriorityBroadcast && (
                <button
                  type="button"
                  onClick={() => {
                    setShowSideInfoDrawer(false);
                    setShowBroadcastModal(true);
                  }}
                  className="w-full min-h-[38px] px-3 rounded-xl bg-[#B45309] hover:bg-[#92400E] text-white text-[11px] font-bold flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Megaphone className="w-3.5 h-3.5 shrink-0" />
                  <span>Broadcast Pengumuman Prioritas</span>
                </button>
              )}
            </div>

            {/* Trip Telemetry & 10-Min Avg Speed Chart */}
            <div className="space-y-2 pb-2.5 border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-[10px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                    Tujuan & ETA
                  </p>
                  <p className="text-xs font-bold truncate mt-0.5">
                    {routeConfig.destinationName}
                  </p>
                  <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                    {!trackedMember || trackedMember.id === currentUser?.id
                      ? 'Fokus: Posisi Anda'
                      : `Fokus: ${trackedMember.fullName}`}
                    {currentUser &&
                      trackedMember &&
                      trackedMember.id !== currentUser.id && (
                        <button
                          type="button"
                          onClick={() => {
                            touchSideDrawer();
                            onSelectMember(currentUser.id);
                          }}
                          className="ml-1 font-semibold text-[#047857] dark:text-[#34D399] hover:underline cursor-pointer"
                        >
                          · Kembali
                        </button>
                      )}
                  </p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-xs font-bold font-mono tabular-nums text-[#047857] dark:text-[#34D399]">
                    ETA {etaInfo.arrivalClockLabel}
                  </p>
                  <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                    {etaInfo.durationLabel}
                  </p>
                </div>
              </div>

              <div className="grid grid-cols-3 gap-2 py-1.5 text-center rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E]">
                <div>
                  <p className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE]">
                    Sisa Jarak
                  </p>
                  <p className="text-[11px] font-bold font-mono tabular-nums">
                    {etaInfo.distanceLabel}
                  </p>
                </div>
                <div className="border-x border-[#D8E6DD] dark:border-[#1B3B2E]">
                  <p className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE]">
                    Kecepatan
                  </p>
                  <p className="text-[11px] font-bold font-mono tabular-nums">
                    {etaInfo.speedKmh} km/j
                  </p>
                </div>
                <div>
                  <p className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE]">
                    Rata² 10m
                  </p>
                  <p className="text-[11px] font-bold font-mono tabular-nums text-[#047857] dark:text-[#34D399]">
                    {avgTenMinSpeed} km/j
                  </p>
                </div>
              </div>

              <div className="h-[52px] w-full rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-2 py-1">
                <ResponsiveContainer width="100%" height="100%">
                  <AreaChart
                    data={speedHistory}
                    margin={{ top: 4, right: 2, left: 2, bottom: 1 }}
                  >
                    <defs>
                      <linearGradient id="sideDrawerSpeedGrad" x1="0" y1="0" x2="0" y2="1">
                        <stop offset="5%" stopColor="#047857" stopOpacity={0.35} />
                        <stop offset="95%" stopColor="#047857" stopOpacity={0.02} />
                      </linearGradient>
                    </defs>
                    <YAxis domain={['dataMin - 8', 'dataMax + 8']} hide />
                    <Area
                      type="monotone"
                      dataKey="avgSpeed"
                      stroke="#047857"
                      strokeWidth={2}
                      fill="url(#sideDrawerSpeedGrad)"
                      isAnimationActive={false}
                      dot={false}
                    />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            </div>

            {/* GPS, Sync & Google Maps API Status */}
            <div className="space-y-1.5 pb-2.5 border-b border-[#D8E6DD] dark:border-[#1B3B2E] text-[11px]">
              <p className="text-[10px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                Sistem & Rute API
              </p>
              <div className="flex items-center justify-between gap-2">
                <span className="text-[#4D6659] dark:text-[#9CC2AE]">Sinyal GPS</span>
                <button
                  type="button"
                  onClick={() => {
                    touchSideDrawer();
                    onRetryGps();
                  }}
                  className={`font-semibold cursor-pointer hover:underline ${
                    gpsStatus === 'active'
                      ? 'text-[#047857] dark:text-[#34D399]'
                      : gpsStatus === 'denied'
                        ? 'text-[#DC2626] dark:text-[#FCA5A5]'
                        : 'text-[#047857] dark:text-[#34D399]'
                  }`}
                >
                  {gpsStatus === 'active'
                    ? `Aktif ±${gpsAccuracyMeters ?? 15}m`
                    : gpsStatus === 'denied'
                      ? 'Ditolak (Segarkan)'
                      : 'Segarkan GPS'}
                </button>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-[#4D6659] dark:text-[#9CC2AE]">Sinkronisasi</span>
                <span className="font-semibold">
                  {activeRoom?.isSyncedRoom ? 'Grup Sync Aktif' : 'Simulasi Lokal'}
                </span>
              </div>
              <div className="flex items-center justify-between gap-2">
                <span className="text-[#4D6659] dark:text-[#9CC2AE]">Ping Rute</span>
                <span className="font-semibold text-[#047857] dark:text-[#34D399]">
                  {canPingRouting
                    ? activeRoom?.leaderTroubleActive
                      ? 'Plt. Pembimbing'
                      : 'HP Mutawwif'
                    : 'Salinan Mutawwif'}
                </span>
              </div>
              {(actingLeaderMember && activeRoom?.leaderTroubleActive) || coLeaderMember ? (
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[#4D6659] dark:text-[#9CC2AE]">
                    {actingLeaderMember && activeRoom?.leaderTroubleActive
                      ? 'Plt. Pembimbing'
                      : 'Wakil Pembimbing'}
                  </span>
                  <span className="font-semibold truncate max-w-[150px]">
                    {actingLeaderMember && activeRoom?.leaderTroubleActive
                      ? actingLeaderMember.fullName
                      : coLeaderMember?.fullName}
                  </span>
                </div>
              ) : null}

              {sweeperMember && (
                <div className="flex items-center justify-between gap-2">
                  <span className="text-[#4D6659] dark:text-[#9CC2AE]">Pendamping</span>
                  <span className="font-semibold text-[#B45309] dark:text-[#FBBF24] truncate max-w-[150px]">
                    {sweeperMember.fullName}
                    {sweeperMember.isAutoSweeper ? ' (Auto)' : ''}
                  </span>
                </div>
              )}

              {activeRoom && (
                <button
                  type="button"
                  onClick={() => {
                    setShowSideInfoDrawer(false);
                    setShowRoomActivitiesSheet(true);
                  }}
                  className="w-full mt-1.5 min-h-[34px] px-2.5 rounded-xl bg-[#FFFBEB] dark:bg-[#451A03]/50 border border-[#FDE68A] dark:border-[#92400E] text-[#B45309] dark:text-[#FCD34D] text-[11px] font-semibold flex items-center justify-center gap-1.5 cursor-pointer tabular-nums"
                >
                  <ClipboardCheck className="w-3.5 h-3.5 shrink-0" />
                  <span>
                    Checklist Kegiatan Grup ({roomActivitiesDoneCount}/{roomActivities.length})
                  </span>
                </button>
              )}

              {activeRoom && (
                <button
                  type="button"
                  onClick={() => {
                    setShowSideInfoDrawer(false);
                    setShowShareRoomModal(true);
                  }}
                  className="w-full mt-1.5 min-h-[34px] px-2.5 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/40 border border-[#A7F3D0] dark:border-[#065F46] text-[#047857] dark:text-[#6EE7B7] text-[11px] font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Share2 className="w-3.5 h-3.5 shrink-0" />
                  <span>Bagikan Link &amp; Kode ({activeRoom.roomId})</span>
                </button>
              )}

              {!activeRoom?.isSyncedRoom && (
                <button
                  type="button"
                  onClick={() => {
                    touchSideDrawer();
                    onSwitchPreviewRole(
                      role === 'TOUR_LEADER' ? 'MEMBER' : 'TOUR_LEADER'
                    );
                  }}
                  className="w-full mt-1.5 min-h-[34px] px-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-[#0B1F16] dark:text-[#ECFDF5] text-[11px] font-semibold flex items-center justify-between gap-2 cursor-pointer"
                >
                  <span className="text-[#4D6659] dark:text-[#9CC2AE]">Pratinjau Peran</span>
                  <span className="text-[#047857] dark:text-[#34D399]">
                    {role === 'TOUR_LEADER' ? 'Mutawwif (Ganti)' : 'Jamaah (Ganti)'}
                  </span>
                </button>
              )}
            </div>

            {/* Convoy Participants & Quick Self Condition */}
            <div className="space-y-2 pb-2.5 border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
              <div className="flex items-center justify-between">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                  Jamaah Rombongan ({members.length})
                </p>
              </div>

              <div className="grid grid-cols-3 gap-1">
                {(
                  [
                    { status: 'AMAN', label: 'Sehat' },
                    { status: 'ISTIRAHAT', label: 'Istirahat' },
                    { status: 'MOGOK', label: 'Bantuan' },
                  ] as Array<{ status: MemberConditionStatus; label: string }>
                ).map((item) => {
                  const active = currentUser?.conditionStatus === item.status;
                  return (
                    <button
                      key={item.status}
                      type="button"
                      onClick={() => {
                        touchSideDrawer();
                        onUpdateOwnCondition(item.status);
                      }}
                      className={`py-1.5 rounded-lg text-[10px] font-semibold border transition-colors cursor-pointer ${
                        active
                          ? item.status === 'MOGOK'
                            ? 'bg-[#FEF2F2] dark:bg-[#450A0A] border-[#DC2626] text-[#DC2626] dark:text-[#F87171]'
                            : item.status === 'ISTIRAHAT'
                              ? 'bg-[#FFFBEB] dark:bg-[#451A03] border-[#D97706] text-[#B45309] dark:text-[#FBBF24]'
                              : 'bg-[#ECFDF5] dark:bg-[#064E3B] border-[#047857] text-[#047857] dark:text-[#6EE7B7]'
                          : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E] text-[#2F4A3C] dark:text-[#D1E7DA]'
                      }`}
                    >
                      {item.label}
                    </button>
                  );
                })}
              </div>

              <div className="space-y-1.5">
                {members.map((member) => {
                  const selected = member.id === focusedMember?.id;
                  const roleLabel = member.isActingLeader
                    ? 'Plt. Pembimbing'
                    : member.isCoLeader && member.isSweeper
                      ? member.isAutoSweeper
                        ? 'Wakil & Pendamping (Auto)'
                        : 'Wakil & Pendamping'
                      : member.isCoLeader
                        ? 'Wakil'
                        : member.isSweeper
                          ? member.isAutoSweeper
                            ? 'Pendamping (Auto · Belakang)'
                            : 'Pendamping'
                          : member.role === 'TOUR_LEADER'
                            ? 'Mutawwif'
                            : 'Jamaah';
                  return (
                    <div
                      key={member.id}
                      onClick={() => {
                        touchSideDrawer();
                        onSelectMember(member.id);
                      }}
                      className={`rounded-xl border px-2.5 py-2 flex items-center gap-2 cursor-pointer transition-colors ${
                        selected
                          ? 'bg-[#ECFDF5] dark:bg-[#064E3B]/40 border-[#047857]'
                          : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                      }`}
                    >
                      <ConvoyAvatar
                        preset={member.avatarPreset}
                        photoUrl={member.avatarPhotoUrl}
                        name={member.fullName}
                        sizeClass="w-7 h-7"
                        ringClass={
                          member.isCurrentUser
                            ? 'ring-2 ring-[#B45309]'
                            : 'ring-1 ring-[#047857]'
                        }
                      />
                      <div className="min-w-0 flex-1">
                        <p className="text-[11px] font-semibold truncate">
                          {member.fullName}
                          {member.isCurrentUser ? ' (Anda)' : ''}
                        </p>
                        <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                          {roleLabel}
                          {typeof member.distanceToLeaderMeters === 'number'
                            ? ` · ${formatDistanceLabel(member.distanceToLeaderMeters)}`
                            : ''}
                          {` · ${conditionLabel(member.conditionStatus)}`}
                        </p>
                        {isEffectiveLeader &&
                          member.role !== 'TOUR_LEADER' &&
                          !member.isActingLeader && (
                            <button
                              type="button"
                              onClick={(e) => {
                                e.stopPropagation();
                                touchSideDrawer();
                                onAssignSweeper(member.id);
                              }}
                              className="text-[9px] font-semibold text-[#047857] dark:text-[#34D399] hover:underline cursor-pointer"
                            >
                              {member.isSweeper && !member.isAutoSweeper
                                ? 'Pendamping Aktif'
                                : member.isAutoSweeper
                                  ? 'Tetapkan Pendamping'
                                  : 'Jadikan Pendamping'}
                            </button>
                          )}
                      </div>
                      <span className="text-[10px] font-mono tabular-nums text-[#4D6659] dark:text-[#9CC2AE] shrink-0">
                        {member.mapPosition.speedKmh} km/j
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>

            {/* Offline Simulation Controls (Hidden from main map when Clean HUD is active) */}
            {!activeRoom?.isSyncedRoom && (
              <div className="space-y-1.5">
                <p className="text-[10px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                  Simulasi & Uji Rombongan
                </p>
                <div className="grid grid-cols-2 gap-1.5">
                  <button
                    type="button"
                    onClick={() => {
                      touchSideDrawer();
                      onToggleSimulation();
                    }}
                    className="py-1.5 px-2 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B]/40 border border-[#A7F3D0] dark:border-[#065F46] text-[10px] font-semibold text-[#047857] dark:text-[#6EE7B7] flex items-center justify-center gap-1 cursor-pointer"
                  >
                    {isSimulationRunning ? (
                      <>
                        <Pause className="w-3 h-3" />
                        <span>Jeda Sim</span>
                      </>
                    ) : (
                      <>
                        <Play className="w-3 h-3" />
                        <span>Lanjut Sim</span>
                      </>
                    )}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      touchSideDrawer();
                      onToggleOffRouteSim();
                    }}
                    className={`py-1.5 px-2 rounded-lg border text-[10px] font-semibold cursor-pointer ${
                      isOffRouteSimActive
                        ? 'bg-[#DC2626] text-white border-[#991B1B]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                    }`}
                  >
                    {isOffRouteSimActive ? 'Keluar Jalur ✓' : 'Uji Keluar Jalur'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      touchSideDrawer();
                      onToggleStragglerSim();
                    }}
                    className={`py-1.5 px-2 rounded-lg border text-[10px] font-semibold cursor-pointer ${
                      isStragglerSimActive
                        ? 'bg-[#B45309] text-white border-[#92400E]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                    }`}
                  >
                    {isStragglerSimActive ? 'Terpisah ✓' : 'Uji Terpisah'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      touchSideDrawer();
                      onToggleLeaderTroubleSim();
                    }}
                    className={`py-1.5 px-2 rounded-lg border text-[10px] font-semibold cursor-pointer ${
                      isLeaderTroubleSimActive
                        ? 'bg-[#065F46] text-white border-[#064E3B]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                    }`}
                  >
                    {isLeaderTroubleSimActive ? 'Kendala ✓' : 'Uji Kendala Mtw'}
                  </button>
                </div>
              </div>
            )}
          </div>
        </aside>

        {/* Custom Destination Placement Hint */}
        {isPickingDestinationOnMap && (
          <div className="absolute bottom-24 left-3 right-3 z-30 rounded-xl bg-[#0B1F16] text-white px-3.5 py-2.5 flex items-center justify-between gap-2 shadow-lg">
            <span className="text-xs">
              Ketuk di peta untuk menetapkan Titik Tujuan baru
            </span>
            <button
              type="button"
              onClick={() => onTogglePickMode(false)}
              className="text-xs font-semibold text-[#FCD34D] cursor-pointer shrink-0"
            >
              Batal
            </button>
          </div>
        )}

        {/* Bottom-Right Floating Emergency & Voice PTT Row (compact 1-row, clear of speedometer & top-right rail) */}
        <div className="absolute bottom-2.5 right-2.5 z-20 flex flex-col items-end gap-1.5">
          {!showChatSheet && voiceStatusMessage && (
            <div className="max-w-[220px] px-2.5 py-1.5 rounded-xl bg-[#FEF2F2] dark:bg-[#450A0A] border border-[#FECACA] dark:border-[#991B1B] text-[#B91C1C] dark:text-[#FCA5A5] text-[10px] font-medium shadow-md">
              {voiceStatusMessage}
            </div>
          )}
          <div className="flex items-center gap-1.5">
            <button
              type="button"
              onClick={() => {
                if (isRecordingVoice) {
                  onStopVoiceRecord();
                  setShowChatSheet(true);
                } else {
                  onStartVoiceRecord();
                }
              }}
              aria-label="Rekam Voice Message"
              className={`min-h-[42px] min-w-[42px] px-2.5 rounded-[14px] text-white shadow-lg flex items-center justify-center gap-1.5 text-xs font-semibold transition-all cursor-pointer whitespace-nowrap ${
                isRecordingVoice
                  ? 'bg-[#DC2626]'
                  : 'bg-[#B45309] hover:bg-[#92400E]'
              }`}
            >
              {isRecordingVoice ? (
                <>
                  <Square className="w-3.5 h-3.5 fill-current" />
                  <span className="font-mono tabular-nums">
                    0:{recordingSeconds.toString().padStart(2, '0')}
                  </span>
                </>
              ) : (
                <Mic className="w-4 h-4" />
              )}
            </button>

            <button
              type="button"
              onClick={() => setShowSosConfirm(true)}
              className="min-h-[42px] px-3 rounded-[14px] bg-[#DC2626] hover:bg-[#B91C1C] active:scale-[0.97] text-white shadow-xl flex items-center gap-1.5 text-xs font-bold tracking-wide transition-all cursor-pointer whitespace-nowrap"
            >
              <AlertTriangle className="w-4 h-4 shrink-0" />
              <span>SOS</span>
            </button>
          </div>
        </div>
      </div>

      {/* Docked Destination ETA & 10-Min Avg Speed Telemetry Bar (Hidden in Clean Nav HUD) */}
      {!isCleanNavHud && (
        <div
          aria-label="Estimasi Waktu Tiba (ETA)"
          className="shrink-0 bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E] px-3.5 py-2 space-y-1.5 z-20"
        >
          <div className="flex items-center justify-between gap-2">
            <div className="flex items-center gap-2.5 min-w-0 flex-1">
              <div
                className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                  etaInfo.isArrived
                    ? 'bg-[#DCFCE7] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7]'
                    : 'bg-[#ECFDF5] dark:bg-[#064E3B]/60 text-[#047857] dark:text-[#34D399]'
                }`}
              >
                <Clock className="w-4 h-4" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-xs font-bold truncate">
                  {routeConfig.destinationName}
                  {routeConfig.destinationStops &&
                  routeConfig.destinationStops.length > 1 ? (
                    <span className="font-normal text-[10px] text-[#047857] dark:text-[#34D399]">
                      {' '}
                      · +{routeConfig.destinationStops.length - 1} singgah
                    </span>
                  ) : null}
                </p>
                <div className="flex items-center gap-1.5 text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                  <span>
                    {!trackedMember || trackedMember.id === currentUser?.id
                      ? 'Posisi Anda'
                      : trackedMember.fullName}
                  </span>
                  {currentUser && trackedMember && trackedMember.id !== currentUser.id && (
                    <button
                      type="button"
                      onClick={() => onSelectMember(currentUser.id)}
                      className="font-semibold text-[#047857] dark:text-[#34D399] hover:underline cursor-pointer shrink-0"
                    >
                      · Fokus Anda
                    </button>
                  )}
                </div>
              </div>
            </div>

            <div className="flex items-center gap-3 shrink-0">
              <div className="text-right">
                <p
                  className={`text-xs font-bold font-mono tabular-nums ${
                    etaInfo.isArrived
                      ? 'text-[#047857] dark:text-[#6EE7B7]'
                      : etaInfo.isStopped
                        ? 'text-[#B45309] dark:text-[#FBBF24]'
                        : 'text-[#047857] dark:text-[#34D399]'
                  }`}
                >
                  ETA {etaInfo.arrivalClockLabel}
                </p>
                <p
                  className={`text-[10px] font-semibold ${
                    etaInfo.isStopped
                      ? 'text-[#B45309] dark:text-[#FBBF24]'
                      : 'text-[#4D6659] dark:text-[#9CC2AE]'
                  }`}
                >
                  {etaInfo.durationLabel}
                </p>
              </div>

              <div className="text-right border-l border-[#D8E6DD] dark:border-[#1B3B2E] pl-3">
                <p className="text-xs font-bold font-mono tabular-nums">
                  {etaInfo.distanceLabel}
                </p>
                <p className="text-[10px] font-mono tabular-nums text-[#4D6659] dark:text-[#9CC2AE] flex items-center justify-end gap-1">
                  <Gauge className="w-3 h-3 shrink-0" />
                  <span>{etaInfo.speedKmh} km/j</span>
                </p>
              </div>
            </div>
          </div>

          {/* Mini-Graph: Average Convoy Speed (Last 10 Minutes) via Recharts */}
          <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-2.5 py-1.5 flex items-center gap-2.5">
            <div className="shrink-0 min-w-[108px]">
              <p className="text-[9px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                Rata² Rombongan · 10 Mnt
              </p>
              <div className="flex items-baseline gap-1.5">
                <span className="text-xs font-bold font-mono tabular-nums text-[#047857] dark:text-[#34D399]">
                  {avgTenMinSpeed} km/j
                </span>
                <span
                  className={`text-[9px] font-mono font-semibold ${
                    speedDeltaTenMin >= 0
                      ? 'text-[#047857] dark:text-[#6EE7B7]'
                      : 'text-[#B45309] dark:text-[#FBBF24]'
                  }`}
                >
                  {speedDeltaTenMin >= 0 ? `+${speedDeltaTenMin}` : speedDeltaTenMin}
                </span>
              </div>
            </div>

            <div className="flex-1 h-[36px] min-w-[120px]">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart
                  data={speedHistory}
                  margin={{ top: 3, right: 2, left: 2, bottom: 1 }}
                >
                  <defs>
                    <linearGradient id="convoyAvgSpeedGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#047857" stopOpacity={0.35} />
                      <stop offset="95%" stopColor="#047857" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <YAxis domain={['dataMin - 8', 'dataMax + 8']} hide />
                  <Tooltip
                    formatter={(value: unknown) => [`${value} km/j`, 'Rata-rata']}
                    labelFormatter={(label: unknown, payload: ReadonlyArray<{ payload?: SpeedHistoryPoint }>) =>
                      payload?.[0]?.payload?.timeLabel ?? String(label)
                    }
                    contentStyle={{
                      fontSize: '10px',
                      padding: '4px 8px',
                      borderRadius: '8px',
                      border: '1px solid #A7F3D0',
                      backgroundColor: darkMode ? '#0C1E17' : '#FFFFFF',
                      color: darkMode ? '#ECFDF5' : '#0B1F16',
                    }}
                  />
                  <Area
                    type="monotone"
                    dataKey="avgSpeed"
                    stroke="#047857"
                    strokeWidth={2}
                    fill="url(#convoyAvgSpeedGrad)"
                    isAnimationActive={false}
                    dot={false}
                    activeDot={{ r: 3, fill: '#D97706', strokeWidth: 0 }}
                  />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      )}

      {/* Clean Dedicated Bottom Navigation Bar for Live Map (zero overlap with Speedometer / Turn-by-Turn HUD) */}
      <nav
        aria-label="Menu Navigasi Grup Live"
        className="mt-auto shrink-0 grid grid-cols-5 bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E] pb-[env(safe-area-inset-bottom)] z-20"
      >
        <button
          type="button"
          onClick={openSideDrawer}
          className={`min-h-[52px] px-1 py-1.5 flex flex-col items-center justify-center gap-0.5 transition-colors cursor-pointer ${
            showSideInfoDrawer
              ? 'text-[#047857] dark:text-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/35'
              : 'text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#047857] dark:hover:text-[#A7F3D0]'
          }`}
        >
          <div className="relative">
            <Info className="w-4 h-4 shrink-0" />
            {(sosState.isActive ||
              offRouteAlert.isOffRoute ||
              stragglerAlert.isTriggered ||
              activeRoom?.leaderTroubleActive) && (
              <span className="absolute -top-0.5 -right-1 w-2 h-2 rounded-full bg-[#D97706]" />
            )}
          </div>
          <span className="text-[10px] font-semibold leading-tight truncate max-w-full">
            Info
          </span>
        </button>

        <button
          type="button"
          onClick={() => setShowRoomActivitiesSheet(true)}
          className={`min-h-[52px] px-1 py-1.5 flex flex-col items-center justify-center gap-0.5 transition-colors cursor-pointer ${
            showRoomActivitiesSheet
              ? 'text-[#047857] dark:text-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/35'
              : 'text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#047857] dark:hover:text-[#A7F3D0]'
          }`}
        >
          <ClipboardCheck className="w-4 h-4 shrink-0" />
          <span className="text-[10px] font-semibold leading-tight truncate max-w-full tabular-nums">
            {roomActivities.length > 0
              ? `Kegiatan ${roomActivitiesDoneCount}/${roomActivities.length}`
              : 'Kegiatan'}
          </span>
        </button>

        <button
          type="button"
          disabled={!manasikProgress || !onToggleManasikStep || !onResetManasikGuide}
          onClick={() => setShowManasikSheet(true)}
          className={`min-h-[52px] px-1 py-1.5 flex flex-col items-center justify-center gap-0.5 transition-colors cursor-pointer disabled:opacity-40 ${
            showManasikSheet
              ? 'text-[#047857] dark:text-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/35'
              : 'text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#047857] dark:hover:text-[#A7F3D0]'
          }`}
        >
          <BookOpenCheck className="w-4 h-4 shrink-0" />
          <span className="text-[10px] font-semibold leading-tight truncate max-w-full">
            Manasik
          </span>
        </button>

        <button
          type="button"
          onClick={() => setShowChatSheet(true)}
          className={`min-h-[52px] px-1 py-1.5 flex flex-col items-center justify-center gap-0.5 transition-colors cursor-pointer ${
            showChatSheet
              ? 'text-[#047857] dark:text-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/35'
              : 'text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#047857] dark:hover:text-[#A7F3D0]'
          }`}
        >
          <MessageCircle className="w-4 h-4 shrink-0" />
          <span className="text-[10px] font-semibold leading-tight truncate max-w-full tabular-nums">
            Chat ({messages.length})
          </span>
        </button>

        <button
          type="button"
          disabled={!activeRoom}
          onClick={() => setShowShareRoomModal(true)}
          className={`min-h-[52px] px-1 py-1.5 flex flex-col items-center justify-center gap-0.5 transition-colors cursor-pointer disabled:opacity-40 ${
            showShareRoomModal
              ? 'text-[#047857] dark:text-[#34D399] bg-[#ECFDF5]/60 dark:bg-[#064E3B]/35'
              : 'text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#047857] dark:hover:text-[#A7F3D0]'
          }`}
        >
          <Share2 className="w-4 h-4 shrink-0" />
          <span className="text-[10px] font-semibold leading-tight truncate max-w-full">
            Bagikan
          </span>
        </button>
      </nav>

      {/* Modern Messenger Chat & Interactive Waveform PTT Sheet */}
      {showChatSheet && (
        <div className="absolute inset-0 z-40 bg-black/45 backdrop-blur-[2px] flex items-end">
          <div className="w-full h-[80%] rounded-t-[24px] bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E] flex flex-col shadow-2xl overflow-hidden">
            {/* Sheet Header */}
            <div className="shrink-0 px-4 py-3 border-b border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center justify-between gap-2">
              <div className="min-w-0">
                <h3 className="text-sm font-bold truncate">Komunikasi Rombongan</h3>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                  {activeRoom?.roomId || 'UMR-8492'} · {members.length} Jamaah Terhubung
                </p>
              </div>
              <div className="flex items-center gap-1.5 shrink-0">
                {isEffectiveLeader && onSendPriorityBroadcast && (
                  <button
                    type="button"
                    onClick={() => {
                      setShowChatSheet(false);
                      setShowBroadcastModal(true);
                    }}
                    className="px-2.5 py-1.5 rounded-xl bg-[#B45309] hover:bg-[#92400E] text-white text-[11px] font-bold flex items-center gap-1 cursor-pointer"
                  >
                    <Megaphone className="w-3.5 h-3.5 shrink-0" />
                    <span>Broadcast</span>
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setShowChatSheet(false)}
                  aria-label="Tutup Chat"
                  className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#133025] cursor-pointer"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Messenger Bubbles */}
            <div
              ref={chatScrollRef}
              className="flex-1 overflow-y-auto px-3.5 py-3 space-y-2.5 bg-[#F4F8F5] dark:bg-[#091812]"
            >
              {messages.map((msg) => {
                if (msg.type === 'SYSTEM') {
                  return (
                    <p
                      key={msg.id}
                      className="text-center text-[11px] text-[#4D6659] dark:text-[#9CC2AE] py-0.5"
                    >
                      {msg.timestampLabel} · {msg.textContent}
                    </p>
                  );
                }

                const matchedMember = members.find((m) => m.id === msg.senderId);
                const msgPreset =
                  matchedMember?.avatarPreset ??
                  msg.senderAvatarPreset ??
                  'HELMET_TOURING';
                const msgPhoto =
                  matchedMember?.avatarPhotoUrl ?? msg.senderAvatarPhotoUrl;

                if (msg.type === 'BROADCAST') {
                  return (
                    <div
                      key={msg.id}
                      className="rounded-2xl bg-[#FFFBEB] dark:bg-[#451A03]/60 border border-[#D97706] p-3 flex items-start gap-2.5"
                    >
                      <div className="w-8 h-8 rounded-xl bg-[#D97706] text-white flex items-center justify-center shrink-0">
                        <Megaphone className="w-4 h-4" />
                      </div>
                      <div className="min-w-0 flex-1 space-y-0.5">
                        <p className="text-[11px] font-bold text-[#92400E] dark:text-[#FCD34D]">
                          PENGUMUMAN PRIORITAS · {msg.senderName} · {msg.timestampLabel}
                        </p>
                        <p className="text-xs font-medium text-[#78350F] dark:text-[#FEF3C7]">
                          {msg.textContent}
                        </p>
                      </div>
                    </div>
                  );
                }

                if (msg.type === 'SOS_ALERT') {
                  return (
                    <div
                      key={msg.id}
                      className="rounded-2xl bg-[#FEF2F2] dark:bg-[#450A0A]/50 border border-[#DC2626] p-3 flex items-start gap-2.5"
                    >
                      <ConvoyAvatar
                        preset={msgPreset}
                        photoUrl={msgPhoto}
                        name={msg.senderName}
                        sizeClass="w-8 h-8"
                        ringClass="ring-2 ring-[#DC2626]"
                      />
                      <div className="min-w-0 flex-1 space-y-1">
                        <p className="text-[11px] font-bold text-[#991B1B] dark:text-[#FCA5A5]">
                          DARURAT SOS · {msg.senderName}
                          {msg.senderVehicle ? ` (${msg.senderVehicle})` : ''} ·{' '}
                          {msg.timestampLabel}
                        </p>
                        <p className="text-xs text-[#7F1D1D] dark:text-[#FECACA]">
                          {msg.textContent}
                        </p>
                        {sosState.isActive && (
                          <button
                            type="button"
                            onClick={onResolveSos}
                            className="mt-1 inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-[#DC2626] hover:bg-[#B91C1C] text-white text-[10px] font-semibold cursor-pointer"
                          >
                            <Check className="w-3 h-3" />
                            <span>Tandai SOS Selesai</span>
                          </button>
                        )}
                      </div>
                    </div>
                  );
                }

                const isMine = Boolean(msg.isFromCurrentUser);
                const playing = playingMessageId === msg.id;

                return (
                  <div
                    key={msg.id}
                    className={`flex items-end gap-2 ${
                      isMine ? 'justify-end' : 'justify-start'
                    }`}
                  >
                    {!isMine && (
                      <ConvoyAvatar
                        preset={msgPreset}
                        photoUrl={msgPhoto}
                        name={msg.senderName}
                        sizeClass="w-7 h-7"
                        ringClass="ring-1 ring-[#047857]"
                      />
                    )}

                    <div
                      className={`max-w-[74%] px-3.5 py-2.5 shadow-xs ${
                        isMine
                          ? 'bg-[#047857] text-white rounded-[18px] rounded-br-[4px]'
                          : 'bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] rounded-[18px] rounded-bl-[4px]'
                      }`}
                    >
                      {!isMine && (
                        <p className="text-[11px] font-semibold text-[#047857] dark:text-[#34D399] mb-0.5">
                          {msg.senderName} · {msg.senderVehicle}
                        </p>
                      )}

                      {msg.type === 'VOICE' ? (
                        <div className="flex items-center gap-2.5">
                          <button
                            type="button"
                            onClick={() => onToggleVoicePlay(msg)}
                            aria-label="Putar Voice Note"
                            className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 cursor-pointer ${
                              isMine
                                ? 'bg-white/20 text-white'
                                : 'bg-[#047857] text-white'
                            }`}
                          >
                            {playing ? (
                              <Pause className="w-4 h-4" />
                            ) : (
                              <Play className="w-4 h-4 ml-0.5" />
                            )}
                          </button>
                          <div>
                            <div className="flex items-center gap-0.5">
                              {[5, 9, 6, 12, 7, 10, 6, 8].map((h, i) => (
                                <span
                                  key={i}
                                  style={{ height: `${h}px` }}
                                  className={`w-0.5 rounded-full ${
                                    isMine
                                      ? 'bg-white/85'
                                      : playing
                                        ? 'bg-[#047857]'
                                        : 'bg-[#7A9486]'
                                  }`}
                                />
                              ))}
                            </div>
                            <p
                              className={`text-[10px] font-mono tabular-nums mt-0.5 ${
                                isMine
                                  ? 'text-white/80'
                                  : 'text-[#4D6659] dark:text-[#9CC2AE]'
                              }`}
                            >
                              0:{String(msg.voiceDurationSec || 2).padStart(2, '0')} ·{' '}
                              {msg.timestampLabel}
                            </p>
                          </div>
                        </div>
                      ) : (
                        <>
                          <p className="text-xs leading-relaxed">
                            {msg.textContent}
                          </p>
                          <p
                            className={`text-[10px] mt-1 ${
                              isMine
                                ? 'text-white/75 text-right'
                                : 'text-[#4D6659] dark:text-[#9CC2AE]'
                            }`}
                          >
                            {msg.timestampLabel}
                          </p>
                        </>
                      )}
                    </div>

                    {isMine && (
                      <ConvoyAvatar
                        preset={msgPreset}
                        photoUrl={msgPhoto}
                        name={msg.senderName}
                        sizeClass="w-7 h-7"
                        ringClass="ring-1 ring-[#B45309]"
                      />
                    )}
                  </div>
                );
              })}
            </div>

            {/* Large Interactive Waveform PTT Button + Text Composer */}
            <div className="shrink-0 p-3 border-t border-[#D8E6DD] dark:border-[#1B3B2E] bg-white dark:bg-[#0C1E17] space-y-2.5">
              {voiceStatusMessage && (
                <div className="rounded-xl bg-[#FEF2F2] dark:bg-[#450A0A]/80 border border-[#FECACA] dark:border-[#991B1B] px-3 py-2 text-[11px] font-medium text-[#B91C1C] dark:text-[#FCA5A5] flex items-center gap-2">
                  <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                  <span>{voiceStatusMessage}</span>
                </div>
              )}
              <button
                type="button"
                onClick={() => {
                  if (isRecordingVoice) onStopVoiceRecord();
                  else onStartVoiceRecord();
                }}
                className={`w-full min-h-[60px] px-4 rounded-[18px] text-white shadow-md flex items-center justify-between gap-3 transition-colors cursor-pointer ${
                  isRecordingVoice
                    ? 'bg-[#DC2626]'
                    : 'bg-[#B45309] hover:bg-[#92400E]'
                }`}
              >
                <div className="flex items-center gap-2.5 text-left min-w-0">
                  {isRecordingVoice ? (
                    <Square className="w-5 h-5 fill-current shrink-0" />
                  ) : (
                    <Mic className="w-5 h-5 shrink-0" />
                  )}
                  <div className="min-w-0">
                    <p className="text-xs font-bold truncate">
                      {isRecordingVoice
                        ? 'Sedang Merekam · Ketuk Kirim'
                        : 'Tekan untuk Bicara (Walkie-Talkie)'}
                    </p>
                    <p className="text-[11px] text-white/85 truncate font-mono tabular-nums">
                      {isRecordingVoice
                        ? `0:${recordingSeconds.toString().padStart(2, '0')} · Audio rombongan aktif`
                        : 'Pesan suara instan ke semua jamaah'}
                    </p>
                  </div>
                </div>

                {/* Interactive Waveform Audio Bars */}
                <div className="flex items-center gap-1 shrink-0" aria-hidden="true">
                  {WAVE_BARS.map((h, i) => {
                    const barHeight = isRecordingVoice
                      ? Math.min(30, 8 + ((h + recordingSeconds * 7 + i * 5) % 22))
                      : Math.round(h * 0.55) + 4;
                    return (
                      <span
                        key={i}
                        style={{ height: `${barHeight}px` }}
                        className="w-1 rounded-full bg-white/90 transition-all duration-150"
                      />
                    );
                  })}
                </div>
              </button>

              <form onSubmit={handleSendChat} className="flex items-center gap-2">
                <input
                  type="text"
                  value={chatText}
                  onChange={(e) => setChatText(e.target.value)}
                  placeholder="Tulis pesan ke rombongan..."
                  className="flex-1 min-w-0 min-h-[42px] px-3.5 rounded-[14px] bg-[#F4F8F5] dark:bg-[#091812] border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857]"
                />
                <button
                  type="submit"
                  aria-label="Kirim Pesan"
                  className="w-11 h-11 rounded-[14px] bg-[#047857] hover:bg-[#065F46] text-white flex items-center justify-center shrink-0 cursor-pointer"
                >
                  <Send className="w-4 h-4" />
                </button>
              </form>
            </div>
          </div>
        </div>
      )}

      {/* Finish & Save Trip Confirmation Modal */}
      {showFinishTripConfirm && (
        <div className="absolute inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-5">
          <div className="w-full max-w-[320px] rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 space-y-4 shadow-2xl">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-full bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] flex items-center justify-center shrink-0">
                <Flag className="w-5 h-5" />
              </div>
              <div>
                <h3 className="text-sm font-bold">Akhiri & Simpan Safar?</h3>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Rekam jejak akan disimpan untuk ekspor GPX.
                </p>
              </div>
            </div>

            <p className="text-xs text-[#2F4A3C] dark:text-[#D1E7DA] leading-relaxed">
              Rute menuju <span className="font-semibold">{routeConfig.destinationName}</span>, pos kumpul/miqat, dan daftar jamaah akan ditambahkan ke Riwayat Safar.
            </p>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowFinishTripConfirm(false)}
                className="flex-1 min-h-[42px] rounded-xl border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs font-semibold cursor-pointer"
              >
                Lanjut Safar
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowFinishTripConfirm(false);
                  onFinishAndSaveTrip();
                }}
                className="flex-1 min-h-[42px] rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold cursor-pointer"
              >
                Simpan Riwayat
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SOS Confirmation Dialog */}
      {showSosConfirm && (
        <div className="absolute inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-5">
          <div className="w-full max-w-[320px] rounded-[24px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 space-y-4 shadow-xl">
            <div className="w-12 h-12 rounded-2xl bg-[#FEF2F2] dark:bg-[#450A0A] flex items-center justify-center mx-auto text-[#DC2626]">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div className="text-center space-y-1.5">
              <h3 className="text-base font-bold">Kirim Sinyal Darurat SOS?</h3>
              <p className="text-xs text-[#2F4A3C] dark:text-[#D1E7DA] leading-relaxed">
                Koordinat Anda (
                <span className="font-mono">
                  {(currentUser?.mapPosition.latitude ?? 21.4225).toFixed(4)},{' '}
                  {(currentUser?.mapPosition.longitude ?? 39.8262).toFixed(4)}
                </span>
                ) akan dikirim ke seluruh jamaah rombongan beserta penanda SOS di peta.
              </p>
            </div>
            <div className="grid grid-cols-2 gap-2.5">
              <button
                type="button"
                onClick={() => setShowSosConfirm(false)}
                className="min-h-[44px] rounded-xl border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs font-semibold cursor-pointer"
              >
                Batal
              </button>
              <button
                type="button"
                onClick={() => {
                  setShowSosConfirm(false);
                  onTriggerSos();
                }}
                className="min-h-[44px] rounded-xl bg-[#DC2626] hover:bg-[#B91C1C] text-white text-xs font-semibold cursor-pointer"
              >
                Kirim SOS
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Share Room Link & Code Modal */}
      {showShareRoomModal && activeRoom && (
        <div className="absolute inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-5">
          <div className="w-full max-w-[336px] rounded-[24px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 space-y-4 shadow-2xl">
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-10 h-10 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/50 text-[#047857] dark:text-[#6EE7B7] flex items-center justify-center shrink-0">
                  <Share2 className="w-5 h-5" />
                </div>
                <div className="min-w-0">
                  <h3 className="text-sm font-bold truncate">
                    Bagikan Grup Rombongan
                  </h3>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                    {activeRoom.roomTitle}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowShareRoomModal(false)}
                aria-label="Tutup Bagikan Grup"
                className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#133025] cursor-pointer shrink-0"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="rounded-2xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 flex items-center justify-between gap-2">
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                  Kode Grup
                </p>
                <p className="text-xl font-extrabold font-mono tracking-widest text-[#047857] dark:text-[#34D399]">
                  {activeRoom.roomId}
                </p>
              </div>
              <button
                type="button"
                onClick={() => handleCopyShare(activeRoom.roomId, 'id')}
                className="px-3 py-2 rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
              >
                {shareCopiedType === 'id' ? (
                  <Check className="w-3.5 h-3.5" />
                ) : (
                  <Copy className="w-3.5 h-3.5" />
                )}
                <span>{shareCopiedType === 'id' ? 'Disalin' : 'Salin'}</span>
              </button>
            </div>

            <div className="space-y-2">
              <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-2.5 flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-[10px] font-semibold text-[#4D6659] dark:text-[#9CC2AE]">
                    Link Browser (Web)
                  </p>
                  <p className="text-[11px] font-mono truncate">
                    {getWebRoomShareLink(activeRoom.roomId)}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    handleCopyShare(
                      getWebRoomShareLink(activeRoom.roomId),
                      'web'
                    )
                  }
                  className="px-2.5 py-1.5 rounded-lg border border-[#CFE0D5] dark:border-[#1B3B2E] text-[11px] font-semibold flex items-center gap-1 cursor-pointer shrink-0"
                >
                  {shareCopiedType === 'web' ? (
                    <Check className="w-3.5 h-3.5 text-[#047857]" />
                  ) : (
                    <Link2 className="w-3.5 h-3.5" />
                  )}
                  <span>{shareCopiedType === 'web' ? 'OK' : 'Salin'}</span>
                </button>
              </div>

              <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-2.5 flex items-center justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-[10px] font-semibold text-[#4D6659] dark:text-[#9CC2AE]">
                    Deep Link Android APK
                  </p>
                  <p className="text-[11px] font-mono truncate">
                    {activeRoom.inviteLink}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() =>
                    handleCopyShare(activeRoom.inviteLink, 'link')
                  }
                  className="px-2.5 py-1.5 rounded-lg border border-[#CFE0D5] dark:border-[#1B3B2E] text-[11px] font-semibold flex items-center gap-1 cursor-pointer shrink-0"
                >
                  {shareCopiedType === 'link' ? (
                    <Check className="w-3.5 h-3.5 text-[#047857]" />
                  ) : (
                    <Copy className="w-3.5 h-3.5" />
                  )}
                  <span>{shareCopiedType === 'link' ? 'OK' : 'Salin'}</span>
                </button>
              </div>
            </div>

            <button
              type="button"
              onClick={handleNativeShareRoom}
              className="w-full min-h-[44px] rounded-xl bg-[#B45309] hover:bg-[#92400E] text-white text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
            >
              <Share2 className="w-4 h-4" />
              <span>Bagikan Undangan Rombongan</span>
            </button>
          </div>
        </div>
      )}

      {/* Leader Route, Checkpoints & Slider Settings Sheet */}
      {showLeaderSettings && isEffectiveLeader && (
        <div className="absolute inset-0 z-40 bg-black/50 backdrop-blur-[2px] flex items-end">
          <div className="w-full rounded-t-[24px] bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E] p-5 space-y-4 max-h-[86%] overflow-y-auto">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Route className="w-5 h-5 text-[#047857] dark:text-[#34D399]" />
                <h3 className="text-base font-bold">
                  Pengaturan Rute & Peringatan Mutawwif
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowLeaderSettings(false)}
                aria-label="Tutup Pengaturan"
                className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#133025] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-2.5">
              <p className="text-xs font-semibold text-[#2F4A3C] dark:text-[#D1E7DA]">
                Titik Tujuan Akhir &amp; Garis Rute Resmi
              </p>

              {/* Inline Google Places (New) Search Area */}
              <div className="space-y-1.5">
                <div className="relative flex items-center">
                  <Search className="w-3.5 h-3.5 text-[#4D6659] absolute left-3 pointer-events-none" />
                  <input
                    type="text"
                    value={destSearchQuery}
                    onChange={(e) => setDestSearchQuery(e.target.value)}
                    placeholder="Cari Masjidil Haram, hotel, pintu gerbang, miqat..."
                    className="w-full min-h-[40px] pl-8 pr-8 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857]"
                  />
                  {isSearchingDest ? (
                    <Loader2 className="w-3.5 h-3.5 text-[#047857] animate-spin absolute right-3" />
                  ) : destSearchQuery ? (
                    <button
                      type="button"
                      onClick={() => {
                        setDestSearchQuery('');
                        setDestSearchResults([]);
                      }}
                      className="w-6 h-6 rounded-full flex items-center justify-center text-[#4D6659] hover:bg-[#D8E6DD] dark:hover:bg-[#133025] absolute right-2 cursor-pointer"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  ) : null}
                </div>

                {destSearchResults.length > 0 && (
                  <div className="rounded-xl border border-[#CFE0D5] dark:border-[#1B3B2E] bg-[#F4F8F5] dark:bg-[#091812] divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E] max-h-[168px] overflow-y-auto">
                    {destSearchResults.map((item) => (
                      <button
                        key={item.id}
                        type="button"
                        onClick={async () => {
                          setIsSearchingDest(true);
                          const resolved = await selectGooglePlacePrediction(item);
                          if (placesLib) {
                            destSessionTokenRef.current = new placesLib.AutocompleteSessionToken();
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
                          setShowLeaderSettings(false);
                        }}
                        className="w-full px-3 py-2 text-left hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
                      >
                        <p className="text-xs font-semibold truncate">
                          {item.name}
                        </p>
                        <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                          {item.address}
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
                      className={`px-3 py-2 rounded-xl text-xs font-medium border text-left truncate transition-colors cursor-pointer ${
                        active
                          ? 'bg-[#ECFDF5] dark:bg-[#064E3B] border-[#047857] text-[#047857] dark:text-[#ECFDF5]'
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
                onClick={() => {
                  onTogglePickMode(true);
                  setShowLeaderSettings(false);
                }}
                className="w-full min-h-[42px] px-3 py-2 rounded-xl border border-[#CFE0D5] dark:border-[#1B3B2E] hover:bg-[#ECFDF5] dark:hover:bg-[#133025] text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
              >
                <MapPin className="w-4 h-4 text-[#047857]" />
                <span>Ketuk Peta untuk Tujuan Kustom</span>
              </button>
            </div>

            <div className="h-px bg-[#D8E6DD] dark:bg-[#1B3B2E]" />

            {/* Interactive Slider for Max Straggler Distance */}
            <div className="space-y-1.5">
              <p className="text-xs font-semibold text-[#2F4A3C] dark:text-[#D1E7DA]">
                Batas Jarak Maksimal Terpisah:{' '}
                <span className="text-[#047857] dark:text-[#34D399] font-mono tabular-nums">
                  {sliderKm.toFixed(1)} km
                </span>
              </p>
              <input
                type="range"
                aria-label="Batas Jarak Maksimal Terpisah"
                min="0.5"
                max="5.0"
                step="0.5"
                value={sliderKm}
                onChange={(e) =>
                  onUpdateMaxStragglerDistance(
                    Math.round(parseFloat(e.target.value) * 1000)
                  )
                }
                className="w-full accent-[#B45309] cursor-pointer h-2 rounded-lg"
              />
              <div className="flex justify-between text-[10px] font-mono text-[#4D6659]">
                <span>0.5 km</span>
                <span>2.5 km</span>
                <span>5.0 km</span>
              </div>
            </div>

            <div className="space-y-2">
              <p className="text-xs font-semibold text-[#2F4A3C] dark:text-[#D1E7DA]">
                Toleransi Keluar Jalur:{' '}
                <span className="text-[#047857] dark:text-[#34D399] font-mono">
                  {routeConfig.offRouteToleranceMeters} m
                </span>
              </p>
              <div className="grid grid-cols-3 gap-2">
                {[30, 50, 100].map((meters) => {
                  const active = routeConfig.offRouteToleranceMeters === meters;
                  return (
                    <button
                      key={meters}
                      type="button"
                      onClick={() => onUpdateOffRouteTolerance(meters)}
                      className={`min-h-[38px] rounded-xl text-xs font-semibold border transition-colors cursor-pointer ${
                        active
                          ? 'bg-[#047857] text-white border-[#047857]'
                          : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                      }`}
                    >
                      {meters} m
                    </button>
                  );
                })}
              </div>
            </div>

            <div className="flex items-center justify-between gap-3 pt-1">
              <div>
                <p className="text-xs font-semibold">
                  Peringatan Suara (Audio Alert)
                </p>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Bunyikan nada peringatan saat SOS, keluar jalur, atau jamaah terpisah.
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
                    : 'bg-[#CFE0D5] dark:bg-[#1B3B2E]'
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

            <button
              type="button"
              onClick={() => setShowLeaderSettings(false)}
              className="w-full min-h-[46px] rounded-[14px] bg-[#B45309] hover:bg-[#92400E] text-white text-sm font-semibold cursor-pointer"
            >
              Terapkan ke Grup
            </button>
          </div>
        </div>
      )}

      {/* Interactive Manasik Checklist Bottom Sheet */}
      {showManasikSheet &&
        manasikProgress &&
        onToggleManasikStep &&
        onCheckInManasikStep &&
        onResetManasikGuide && (
          <div
            role="dialog"
            aria-modal="true"
            aria-label="Checklist Manasik Umroh dan Haji"
            className="absolute inset-0 z-30 bg-black/45 backdrop-blur-[1px] flex items-end justify-center"
            onClick={() => setShowManasikSheet(false)}
          >
            <div
              className="w-full max-h-[84%] rounded-t-[24px] bg-[#F4F8F5] dark:bg-[#06130E] border-t border-[#D8E6DD] dark:border-[#1B3B2E] shadow-2xl flex flex-col overflow-hidden"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="shrink-0 px-4 py-3 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center justify-between gap-2">
                <div className="flex items-center gap-2 min-w-0">
                  <BookOpenCheck className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
                  <div className="min-w-0">
                    <h3 className="text-sm font-bold truncate">
                      Checklist &amp; Check-in Manasik
                    </h3>
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                      Check-in GPS aktif otomatis saat di area landmark ibadah
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setShowManasikSheet(false)}
                  aria-label="Tutup Checklist Manasik"
                  className="min-w-[36px] min-h-[36px] rounded-full flex items-center justify-center text-[#4D6659] hover:bg-[#ECFDF5] dark:hover:bg-[#132E24] cursor-pointer shrink-0"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <div className="flex-1 overflow-y-auto p-3.5">
                <ManasikChecklistPanel
                  progress={manasikProgress}
                  isCloudSynced={isManasikCloudSynced}
                  userLocation={
                    currentUser
                      ? {
                          latitude: currentUser.mapPosition.latitude,
                          longitude: currentUser.mapPosition.longitude,
                          accuracyMeters: currentUser.mapPosition.accuracyMeters,
                          sourceLabel:
                            gpsStatus === 'active'
                              ? 'GPS Live Rombongan'
                              : `Posisi Peta (${currentUser.lastLocationText})`,
                        }
                      : null
                  }
                  gpsStatus={gpsStatus}
                  onRetryGps={onRetryGps}
                  onToggleStep={onToggleManasikStep}
                  onCheckInStep={onCheckInManasikStep}
                  onResetGuide={onResetManasikGuide}
                  onOpenAssistant={
                    onOpenManasikAssistant
                      ? (prompt) => {
                          setShowManasikSheet(false);
                          onOpenManasikAssistant(prompt);
                        }
                      : undefined
                  }
                  compact
                />
              </div>
            </div>
          </div>
        )}

      {/* Interactive Group Umrah Activity Checklist Bottom Sheet */}
      {showRoomActivitiesSheet && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Checklist Kegiatan Umroh Rombongan"
          className="absolute inset-0 z-30 bg-black/45 backdrop-blur-[1px] flex items-end justify-center"
          onClick={() => setShowRoomActivitiesSheet(false)}
        >
          <div
            className="w-full max-h-[84%] rounded-t-[24px] bg-[#F4F8F5] dark:bg-[#06130E] border-t border-[#D8E6DD] dark:border-[#1B3B2E] shadow-2xl flex flex-col overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="shrink-0 px-4 py-3 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <ClipboardCheck className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
                <div className="min-w-0">
                  <h3 className="text-sm font-bold truncate">
                    Checklist Kegiatan Umroh
                  </h3>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                    {isEffectiveLeader
                      ? 'Buat/atur agenda dan centang progres Anda'
                      : 'Centang agenda rombongan yang sudah Anda kerjakan'}
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowRoomActivitiesSheet(false)}
                aria-label="Tutup Checklist Kegiatan"
                className="min-w-[36px] min-h-[36px] rounded-full flex items-center justify-center text-[#4D6659] hover:bg-[#ECFDF5] dark:hover:bg-[#132E24] cursor-pointer shrink-0"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-3.5">
              <RoomActivityChecklistPanel
                mode="room"
                activities={roomActivities}
                progressByMember={roomActivityProgress}
                members={members}
                currentUid={effectiveActivityUid}
                isRoomCreator={isEffectiveLeader}
                roomId={activeRoom?.roomId}
                roomTitle={activeRoom?.roomTitle}
                isCloudSynced={activeRoom?.isSyncedRoom}
                onToggleActivity={onToggleRoomActivity}
                onAddActivity={onAddRoomActivity}
                onRemoveActivity={onRemoveRoomActivity}
                onMoveActivity={onMoveRoomActivity}
                onApplyStarterTemplates={onApplyRoomStarterTemplates}
              />
            </div>
          </div>
        </div>
      )}

      {/* 10-Second Full-Screen Flashing Red Overlay & Large SOS Notification Directly on Navigation Screen */}
      {showSosFlashOverlay && sosState.isActive && (
        <div
          role="alertdialog"
          aria-live="assertive"
          aria-label="Notifikasi Darurat SOS"
          className="absolute inset-0 z-40 pointer-events-none"
        >
          {/* Flashing Red Screen Tint + Thick Perimeter Glow */}
          <div className="absolute inset-0 bg-[#DC2626]/70 ring-[8px] ring-inset ring-[#EF4444] shadow-[inset_0_0_96px_rgba(185,28,28,0.95)] animate-sos-screen-flash" />

          {/* Large Prominent Red SOS Card */}
          <div className="relative z-10 h-full w-full flex items-center justify-center p-4">
            <div className="pointer-events-auto w-full max-w-[380px] rounded-[24px] bg-gradient-to-b from-[#DC2626] to-[#991B1B] text-white border-2 border-[#FCA5A5] shadow-2xl p-4 sm:p-5 space-y-3.5">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-12 h-12 rounded-2xl bg-white/20 flex items-center justify-center shrink-0">
                    <AlertTriangle className="w-7 h-7 text-white" />
                  </div>
                  <div className="min-w-0">
                    <h3 className="text-lg font-extrabold tracking-tight leading-tight">
                      SINYAL DARURAT SOS
                    </h3>
                    <p className="text-xs text-[#FEE2E2] font-mono tabular-nums mt-0.5">
                      Hilang otomatis dalam {sosFlashSecondsLeft} dtk
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setDismissedSosKey(currentSosKey)}
                  aria-label="Tutup Notifikasi SOS"
                  className="min-w-[40px] min-h-[40px] rounded-xl bg-white/15 hover:bg-white/25 flex items-center justify-center cursor-pointer shrink-0 transition-colors"
                >
                  <X className="w-5 h-5 text-white" />
                </button>
              </div>

              <div className="rounded-2xl bg-black/25 border border-white/20 px-3.5 py-3 space-y-1">
                <p className="text-[10px] font-bold tracking-wider uppercase text-[#FECACA]">
                  Pengirim Sinyal SOS
                </p>
                <p className="text-base sm:text-lg font-extrabold text-white leading-snug break-words">
                  {sosState.senderName || 'Jamaah Umroh'}
                </p>
                <p className="text-xs text-[#FEE2E2]">
                  {sosState.senderVehicle || 'Peserta Rombongan'} · Pukul{' '}
                  {sosState.triggeredAtLabel || '-'}
                </p>
                <p className="text-xs font-mono text-white/95 pt-1 break-words">
                  {sosState.locationText || 'Lokasi Live'} (
                  {sosState.latitude.toFixed(4)}, {sosState.longitude.toFixed(4)})
                </p>
              </div>

              <p className="text-[11px] text-[#FEE2E2] text-center">
                Identitas pengirim SOS tetap tercatat di kolom chat rombongan.
              </p>

              <div className="grid grid-cols-2 gap-2.5">
                <button
                  type="button"
                  onClick={() => {
                    if (sosState.senderId) {
                      onSelectMember(sosState.senderId);
                    }
                    setDismissedSosKey(currentSosKey);
                  }}
                  className="min-h-[44px] px-3 rounded-xl bg-white text-[#991B1B] hover:bg-[#FEF2F2] text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors"
                >
                  <LocateFixed className="w-4 h-4 shrink-0" />
                  <span>Lokasi Pengirim</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setDismissedSosKey(currentSosKey);
                    onResolveSos();
                  }}
                  className="min-h-[44px] px-3 rounded-xl bg-[#7F1D1D] hover:bg-[#450A0A] border border-white/30 text-white text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer transition-colors"
                >
                  <Check className="w-4 h-4 shrink-0" />
                  <span>Selesai SOS</span>
                </button>
              </div>

              <div className="h-1.5 w-full rounded-full bg-white/25 overflow-hidden">
                <div
                  className="h-full w-full bg-white origin-left transition-transform duration-1000 ease-linear"
                  style={{
                    transform: `scaleX(${Math.max(0, Math.min(1, sosFlashSecondsLeft / 10))})`,
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Priority Broadcast Composer Modal (Leader / Acting Leader) */}
      {showBroadcastModal && onSendPriorityBroadcast && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Kirim Broadcast Pengumuman Prioritas"
          className="absolute inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-4"
          onClick={() => setShowBroadcastModal(false)}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-[360px] rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3.5 shadow-2xl"
          >
            <div className="flex items-center justify-between gap-2">
              <div className="flex items-center gap-2 min-w-0">
                <div className="w-9 h-9 rounded-xl bg-[#FEF3C7] dark:bg-[#451A03] text-[#B45309] dark:text-[#FBBF24] flex items-center justify-center shrink-0">
                  <Megaphone className="w-4 h-4" />
                </div>
                <div className="min-w-0">
                  <h3 className="text-sm font-bold truncate">
                    Broadcast Prioritas
                  </h3>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                    Tampil serentak di layar seluruh jamaah
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowBroadcastModal(false)}
                aria-label="Tutup Broadcast"
                className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#133025] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="space-y-1.5">
              <p className="text-[10px] font-semibold uppercase tracking-wide text-[#4D6659] dark:text-[#9CC2AE]">
                Template Cepat Umroh / Haji
              </p>
              <div className="grid grid-cols-2 gap-1.5">
                {[
                  {
                    title: 'Kumpul di Pintu Masjidil Haram',
                    message:
                      'Seluruh jamaah harap berkumpul di Pintu King Fahd dalam 15 menit.',
                  },
                  {
                    title: 'Persiapan Miqat & Ihram',
                    message:
                      'Segera mandi sunnah & kenakan kain ihram sebelum rombongan bergerak.',
                  },
                  {
                    title: 'Bus Rombongan Segera Berangkat',
                    message:
                      'Mohon seluruh jamaah naik ke bus masing-masing dalam 10 menit.',
                  },
                  {
                    title: 'Jadwal Tawaf & Sa’i Dimulai',
                    message:
                      'Rombongan bersiap menuju area Mataf bersama Mutawwif.',
                  },
                ].map((tpl) => (
                  <button
                    key={tpl.title}
                    type="button"
                    onClick={() => {
                      setBroadcastTitle(tpl.title);
                      setBroadcastMessage(tpl.message);
                    }}
                    className="px-2.5 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#B45309] text-left text-[10px] font-semibold leading-snug cursor-pointer"
                  >
                    {tpl.title}
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <div>
                <label className="block text-[11px] font-semibold mb-1">
                  Judul Pengumuman
                </label>
                <input
                  type="text"
                  maxLength={120}
                  value={broadcastTitle}
                  onChange={(e) => setBroadcastTitle(e.target.value)}
                  placeholder="Misal: Kumpul di Pintu King Fahd"
                  className="w-full min-h-[40px] px-3 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#B45309]"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold mb-1">
                  Isi Instruksi untuk Jamaah
                </label>
                <textarea
                  rows={3}
                  maxLength={500}
                  value={broadcastMessage}
                  onChange={(e) => setBroadcastMessage(e.target.value)}
                  placeholder="Tulis instruksi penting untuk seluruh rombongan..."
                  className="w-full p-3 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#B45309] resize-none"
                />
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setShowBroadcastModal(false)}
                className="flex-1 min-h-[42px] rounded-xl border border-[#CFE0D5] dark:border-[#1B3B2E] text-xs font-semibold cursor-pointer"
              >
                Batal
              </button>
              <button
                type="button"
                disabled={!broadcastTitle.trim() || !broadcastMessage.trim()}
                onClick={() => {
                  onSendPriorityBroadcast(broadcastTitle, broadcastMessage);
                  setBroadcastTitle('');
                  setBroadcastMessage('');
                  setShowBroadcastModal(false);
                }}
                className="flex-1 min-h-[42px] rounded-xl bg-[#B45309] hover:bg-[#92400E] disabled:opacity-45 text-white text-xs font-bold flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Megaphone className="w-3.5 h-3.5 shrink-0" />
                <span>Kirim Broadcast</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
