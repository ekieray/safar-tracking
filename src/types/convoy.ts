export type UserRole = 'TOUR_LEADER' | 'MEMBER';

export type VehicleType = 'MOBIL' | 'MOTOR';

export type ConnectionStatus = 'CONNECTED' | 'WEAK_SIGNAL' | 'OFFLINE';

export type MemberConditionStatus = 'AMAN' | 'ISTIRAHAT' | 'MOGOK' | 'SOS';

export type ChatMessageType = 'TEXT' | 'VOICE' | 'SOS_ALERT' | 'SYSTEM' | 'BROADCAST';

export type AuthProviderId = 'google' | 'facebook' | 'instagram' | 'email' | 'local';

export type ScreenRoute =
  | 'login'
  | 'role_selection'
  | 'customize_profile'
  | 'admin_backend'
  | 'vehicle_profile'
  | 'tour_leader'
  | 'join_tour'
  | 'live_map'
  | 'trip_history'
  | 'trip_detail'
  | 'manasik_guide'
  | 'ai_assistant'
  | 'family_monitor'
  | 'about_app';

export type AvatarPreset =
  | 'HELMET_TOURING'
  | 'HELMET_SPORT'
  | 'RIDER_CAPTAIN'
  | 'PILOT_CLASSIC'
  | 'RALLY_PRO'
  | 'STANDARD_USER';

export interface RoomMessage {
  id: string;
  senderId: string;
  senderName: string;
  senderVehicle: string;
  type: ChatMessageType;
  textContent: string;
  timestampLabel: string;
  voiceDurationSec?: number;
  voiceAudioUrl?: string;
  isFromCurrentUser?: boolean;
  senderAvatarPreset?: AvatarPreset;
  senderAvatarPhotoUrl?: string;
}

export interface SosEmergencyState {
  isActive: boolean;
  senderId: string;
  senderName: string;
  senderVehicle: string;
  latitude: number;
  longitude: number;
  locationText: string;
  triggeredAtLabel: string;
}

export interface PriorityBroadcast {
  isActive: boolean;
  id: string;
  title: string;
  message: string;
  senderName: string;
  atLabel: string;
}

export interface RoutePoint {
  xFraction: number;
  yFraction: number;
  latitude: number;
  longitude: number;
}

export interface DestinationPreset {
  name: string;
  endPoint: RoutePoint;
  totalMeters: number;
}

export interface DestinationStop {
  id: string;
  name: string;
  address: string;
  latitude: number;
  longitude: number;
}

export interface RestCheckpoint {
  id: string;
  name: string;
  xFraction: number;
  yFraction: number;
  latitude: number;
  longitude: number;
}

export type RoutePreference = 'fastest' | 'shortest' | 'no_toll';

export interface RouteInstruction {
  latitude: number;
  longitude: number;
  routeOffsetInMeters: number;
  maneuver: string;
  message: string;
  street?: string;
}

export interface NavigationGuidanceState {
  maneuver: string;
  instructionText: string;
  streetName: string;
  distanceToTurnMeters: number;
  distanceToTurnLabel: string;
  routeBearingDeg: number;
  speedKmh: number;
  isOffRoute: boolean;
}

export interface ConvoyRouteConfig {
  originName?: string;
  originAddress?: string;
  originLockedByLeader?: boolean;
  routePreference?: RoutePreference;
  routeInstructions?: RouteInstruction[];
  destinationName: string;
  destinationAddress: string;
  destinationStops?: DestinationStop[];
  waypoints: RoutePoint[];
  checkpoints: RestCheckpoint[];
  totalRouteMeters: number;
  estimatedDurationMillis?: number;
  offRouteToleranceMeters: number;
  maxStragglerDistanceMeters: number;
  soundAlertEnabled: boolean;
}

export interface StragglerAlertState {
  isTriggered: boolean;
  trailingMember: ConvoyMember | null;
  aheadMember: ConvoyMember | null;
  gapMeters: number;
  maxAllowedMeters: number;
}

export interface OffRouteAlertState {
  isOffRoute: boolean;
  deviationMeters: number;
  toleranceMeters: number;
}

export interface MapPosition {
  routeProgress: number;
  xFraction: number;
  yFraction: number;
  latitude: number;
  longitude: number;
  speedKmh: number;
  heading?: number;
  accuracyMeters?: number;
}

export interface VehicleProfile {
  fullName: string;
  email?: string;
  authProvider?: AuthProviderId;
  clubName?: string;
  homeCity?: string;
  bio?: string;
  instagramHandle?: string;
  vehicleType: VehicleType;
  vehicleDetail: string;
  licensePlate: string;
  emergencyContact: string;
  avatarPreset: AvatarPreset;
  avatarPhotoUrl?: string;
}

export interface ConvoyMember {
  id: string;
  fullName: string;
  vehicleType: VehicleType;
  vehicleDetail: string;
  licensePlate: string;
  emergencyContact: string;
  role: UserRole;
  joinedAtLabel: string;
  isCurrentUser: boolean;
  isSweeper: boolean;
  isAutoSweeper?: boolean;
  avatarPreset: AvatarPreset;
  avatarPhotoUrl?: string;
  conditionStatus: MemberConditionStatus;
  connectionStatus: ConnectionStatus;
  lastLocationText: string;
  lastUpdatedLabel: string;
  lastUpdatedMs?: number;
  isCoLeader?: boolean;
  isActingLeader?: boolean;
  distanceToLeaderMeters?: number;
  routeDistanceMeters: number;
  offRouteMeters: number;
  convoyOrderRank: number;
  mapPosition: MapPosition;
}

export interface TourRoom {
  roomId: string;
  roomTitle: string;
  inviteLink: string;
  createdAtLabel: string;
  isSyncedRoom?: boolean;
  leaderUid?: string;
  coLeaderUid?: string;
  actingLeaderUid?: string;
  leaderTroubleActive?: boolean;
  leaderTroubleReason?: string;
  priorityBroadcast?: PriorityBroadcast;
  members: ConvoyMember[];
}

export interface DestinationEtaInfo {
  remainingMeters: number;
  speedKmh: number;
  etaMinutes: number | null;
  isStopped: boolean;
  isArrived: boolean;
  durationLabel: string;
  arrivalClockLabel: string;
  distanceLabel: string;
}

export interface TripParticipantSummary {
  id: string;
  fullName: string;
  vehicleType: VehicleType;
  vehicleDetail: string;
  licensePlate: string;
  role: UserRole;
  isSweeper?: boolean;
  avatarPreset: AvatarPreset;
  avatarPhotoUrl?: string;
}

export interface VisitedCheckpointLog {
  id: string;
  name: string;
  arrivedAtLabel: string;
  xFraction: number;
  yFraction: number;
  latitude: number;
  longitude: number;
}

export interface TripHistoryRecord {
  id: string;
  roomId: string;
  tourTitle: string;
  dateLabel: string;
  timeRangeLabel: string;
  durationLabel: string;
  durationMinutes: number;
  distanceKm: number;
  avgSpeedKmh: number;
  maxSpeedKmh: number;
  userRole: UserRole;
  destinationName: string;
  destinationAddress: string;
  waypoints: RoutePoint[];
  visitedCheckpoints: VisitedCheckpointLog[];
  participants: TripParticipantSummary[];
}

export type TripCardTheme =
  | 'DARK_CARBON'
  | 'SATELLITE'
  | 'MINIMAL_LIGHT'
  | 'CUSTOM_PHOTO';

export interface GlobalAppConfig {
  configId: 'global';
  defaultOffRouteToleranceMeters: number;
  defaultMaxStragglerDistanceMeters: number;
  defaultSoundAlertEnabled: boolean;
  maintenanceMode: boolean;
  maintenanceMessage: string;
  announcementActive: boolean;
  announcementTitle: string;
  announcementMessage: string;
  allowGuestMode: boolean;
  allowNewRooms: boolean;
  updatedByUid?: string;
  updatedAtLabel?: string;
}

export interface RegisteredTourLeader {
  id: string;
  email: string;
  label: string;
  addedByUid: string;
  createdAtLabel: string;
  createdAtMs: number;
}

export interface UserPresenceRecord {
  uid: string;
  fullName: string;
  email: string;
  platform: 'web' | 'android';
  isTraveling: boolean;
  activeRoomId: string;
  speedKmh: number;
  lastSeenMs: number;
  lastSeenLabel: string;
}

export interface AdminUserSummary {
  uid: string;
  fullName: string;
  email: string;
  authProvider: AuthProviderId;
  clubName: string;
  homeCity: string;
  vehicleType: VehicleType;
  vehicleDetail: string;
  licensePlate: string;
  avatarPreset: AvatarPreset;
  avatarPhotoUrl?: string;
  updatedAtLabel: string;
}

export interface AdminRoomOverview {
  roomId: string;
  roomTitle: string;
  leaderUid: string;
  destinationName: string;
  status: 'ACTIVE' | 'FINISHED';
  sosActive: boolean;
  sosSenderName: string;
  offRouteToleranceMeters: number;
  maxStragglerDistanceMeters: number;
  createdAtLabel: string;
  updatedAtMs: number;
  updatedAtLabel: string;
  members: ConvoyMember[];
}

export type ManasikGuideType = 'umrah' | 'haji';

export type ManasikStepCategory = 'RUKUN' | 'WAJIB' | 'SUNNAH';

export interface ManasikLandmark {
  name: string;
  latitude: number;
  longitude: number;
  radiusMeters: number;
}

export interface ManasikCheckInRecord {
  stepId: string;
  landmarkName: string;
  checkedInAtLabel: string;
  distanceMeters: number;
  latitude: number;
  longitude: number;
}

export interface ManasikStepItem {
  id: string;
  guideType: ManasikGuideType;
  order: number;
  title: string;
  category: ManasikStepCategory;
  locationHint: string;
  landmark: ManasikLandmark;
  alternateLandmarks?: ManasikLandmark[];
  summary: string;
  details: string[];
  arabicText?: string;
  transliteration?: string;
  translation?: string;
}

export interface ManasikProgressState {
  umrah: string[];
  haji: string[];
  checkIns?: Record<string, ManasikCheckInRecord>;
}

export type AssistantRoleId = 'mutawwif' | 'doa_fast' | 'koordinator';

export interface AssistantChatMessage {
  id: string;
  role: 'user' | 'model';
  text: string;
  timestampLabel: string;
  roleId?: AssistantRoleId;
  modelName?: string;
}

export interface RoomActivityItem {
  id: string;
  title: string;
  timeLabel: string;
  locationNote: string;
  orderIndex: number;
  createdByUid?: string;
}

export interface RoomActivityMemberProgress {
  uid: string;
  memberName: string;
  completedActivityIds: string[];
  completedTimes: Record<string, string>;
}

export interface MissingPilgrimAlertState {
  isTriggered: boolean;
  affectedMembers: ConvoyMember[];
  thresholdSeconds: number;
  staleSinceLabel: string;
}

export interface FamilyLinkRecord {
  inviteCode: string;
  roomId: string;
  roomTitle: string;
  targetUid: string;
  targetName: string;
  targetVehicleDetail: string;
  targetAvatarPreset: AvatarPreset;
  destinationName: string;
  active: boolean;
  claimedByUid: string;
  claimedByName: string;
  createdAtLabel?: string;
  updatedAtLabel?: string;
}

export interface FamilyGrantRecord {
  familyUid: string;
  inviteCode: string;
  roomId: string;
  targetUid: string;
  targetName: string;
  active: boolean;
}

export interface FamilyMonitorSnapshot {
  linkActive: boolean;
  roomStatus: 'ACTIVE' | 'FINISHED';
  roomTitle: string;
  destinationName: string;
  destinationAddress: string;
  targetMember: ConvoyMember | null;
  routeConfig: ConvoyRouteConfig | null;
  activities: RoomActivityItem[];
  completedActivityIds: string[];
  completedTimes: Record<string, string>;
  lastSyncLabel: string;
}


