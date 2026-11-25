import { useState } from 'react';
import {
  ArrowRight,
  BadgeCheck,
  BookOpenCheck,
  Bus,
  CheckCircle2,
  ClipboardCheck,
  Compass,
  Footprints,
  History,
  Info,
  Link2,
  LogOut,
  Megaphone,
  RotateCcw,
  ShieldCheck,
  Sparkles,
  UserCog,
  Users,
  Wrench,
  X,
} from 'lucide-react';
import { MANASIK_GUIDE_MAP } from '../../data/manasikGuideData';
import { AuthProviderId, GlobalAppConfig, ManasikProgressState, RoomActivityItem, RoomActivityMemberProgress, TourRoom, TripHistoryRecord, UserRole, VehicleProfile } from '../../types/convoy';
import { ConvoyAvatar } from '../ConvoyAvatar';
import {
  TouringCanvasBackdrop,
  TouringHeroBanner,
} from '../TouringAtmosphereBackdrop';

interface RoleSelectionViewProps {
  activeRoom: TourRoom | null;
  lastCompletedTrip?: TripHistoryRecord | null;
  destinationName: string;
  tripHistoryCount: number;
  latestTripTitle?: string;
  authUserEmail?: string | null;
  authUserName?: string | null;
  authProvider?: AuthProviderId;
  userProfile?: VehicleProfile;
  localUserName?: string | null;
  isSigningIn?: boolean;
  globalConfig?: GlobalAppConfig;
  isAdminUser?: boolean;
  onGoogleSignIn: () => void;
  onGoogleSignOut: () => void;
  onOpenCustomizeProfile: () => void;
  onOpenAdminBackend?: () => void;
  onSelectRole: (role: UserRole) => void;
  onResumeCurrentConvoy: () => void;
  onRepeatTour?: (trip: TripHistoryRecord) => void;
  onOpenTripHistory: () => void;
  onOpenManasikGuide?: () => void;
  onOpenRoomActivities?: () => void;
  onOpenAssistant?: () => void;
  manasikProgress?: ManasikProgressState;
  roomActivities?: RoomActivityItem[];
  roomActivityProgress?: Record<string, RoomActivityMemberProgress>;
  currentMemberUid?: string;
  onOpenAboutApp: () => void;
}

const PROVIDER_TAG: Record<AuthProviderId, string> = {
  google: 'Google Sync',
  facebook: 'Facebook Sync',
  instagram: 'Instagram Sync',
  email: 'Email Sync',
  local: 'Email Lokal',
};

export function RoleSelectionView({
  activeRoom,
  lastCompletedTrip = null,
  destinationName,
  tripHistoryCount,
  latestTripTitle,
  authUserEmail,
  authUserName,
  authProvider = 'local',
  userProfile,
  localUserName,
  isSigningIn,
  globalConfig,
  isAdminUser = false,
  onGoogleSignIn,
  onGoogleSignOut,
  onOpenCustomizeProfile,
  onOpenAdminBackend,
  onSelectRole,
  onResumeCurrentConvoy,
  onRepeatTour,
  onOpenTripHistory,
  onOpenManasikGuide,
  onOpenRoomActivities,
  onOpenAssistant,
  manasikProgress,
  roomActivities = [],
  roomActivityProgress = {},
  currentMemberUid = '',
  onOpenAboutApp,
}: RoleSelectionViewProps) {
  const umrahDoneCount = manasikProgress?.umrah.length ?? 0;
  const umrahTotalCount = MANASIK_GUIDE_MAP.umrah.length;
  const hajiDoneCount = manasikProgress?.haji.length ?? 0;
  const hajiTotalCount = MANASIK_GUIDE_MAP.haji.length;
  const selfUid =
    currentMemberUid ||
    activeRoom?.members.find((m) => m.isCurrentUser)?.id ||
    '';
  const myRoomCompletedSet = new Set(
    selfUid ? (roomActivityProgress[selfUid]?.completedActivityIds ?? []) : []
  );
  const roomActivitiesDoneCount = roomActivities.filter((a) =>
    myRoomCompletedSet.has(a.id)
  ).length;
  const [showCompletedModal, setShowCompletedModal] = useState(false);
  const roomCreationPaused = globalConfig?.allowNewRooms === false && !isAdminUser;
  const isConvoyFinished = !activeRoom && Boolean(lastCompletedTrip);
  const activeTourName =
    activeRoom?.roomTitle ||
    lastCompletedTrip?.tourTitle ||
    'Belum ada rombongan aktif';
  const activeMemberCount =
    activeRoom?.members.length ??
    Math.max(1, lastCompletedTrip?.participants.length ?? 0);
  const hasAnySession = Boolean(authUserEmail || localUserName);
  const displayName =
    userProfile?.fullName?.trim() || authUserName || localUserName || 'Jamaah Umroh';
  const displayEmail =
    userProfile?.email?.trim() || authUserEmail || '';
  const metaSubtitle = [
    userProfile?.clubName?.trim(),
    userProfile?.homeCity?.trim(),
  ]
    .filter(Boolean)
    .join(' · ');

  return (
    <div className="relative flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Clean Material 3 Top App Bar */}
      <div className="sticky top-0 z-20 flex items-center justify-between gap-2 px-4 py-3 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-[10px] bg-[#047857] flex items-center justify-center text-white shrink-0">
            <Compass className="w-4 h-4" />
          </div>
          <span className="text-base font-bold tracking-tight">Safar Tracking</span>
        </div>

        <div className="flex items-center gap-1.5">
          {onOpenManasikGuide && (
            <button
              type="button"
              onClick={onOpenManasikGuide}
              className="min-h-[36px] px-2.5 py-1.5 rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
            >
              <BookOpenCheck className="w-3.5 h-3.5 shrink-0" />
              <span>Manasik</span>
            </button>
          )}
          <button
            type="button"
            onClick={onOpenTripHistory}
            className="min-h-[36px] px-2.5 py-1.5 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/70 text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold flex items-center gap-1.5 hover:bg-[#D1FAE5] dark:hover:bg-[#065F46] transition-colors cursor-pointer whitespace-nowrap"
          >
            <History className="w-3.5 h-3.5 shrink-0" />
            <span>Riwayat ({tripHistoryCount})</span>
          </button>
          <button
            type="button"
            onClick={onOpenAboutApp}
            aria-label="Tentang Aplikasi"
            className="min-w-[36px] min-h-[36px] rounded-xl flex items-center justify-center text-[#047857] dark:text-[#6EE7B7] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
          >
            <Info className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="relative flex-1 px-4 py-5 space-y-4">
        <TouringCanvasBackdrop />

        <div className="relative z-10 space-y-4">
          {/* Atmospheric Umrah Header */}
          <TouringHeroBanner
            title="Dashboard Perjalanan Umroh"
            subtitle="Pantau pergerakan jamaah umroh, bus rombongan, dan titik kumpul Masjidil Haram secara real-time dengan GPS & Google Maps."
          />

          {/* Backend Maintenance & Announcement Banners */}
          {globalConfig?.maintenanceMode && (
            <div className="rounded-[18px] bg-[#FFFBEB] dark:bg-[#451A03]/70 border border-[#D97706] px-3.5 py-3 flex items-start gap-2.5 text-xs text-[#92400E] dark:text-[#FCD34D]">
              <Wrench className="w-4 h-4 shrink-0 mt-0.5 text-[#D97706]" />
              <div>
                <p className="font-bold">Mode Pemeliharaan Aktif</p>
                <p className="mt-0.5 leading-relaxed">
                  {globalConfig.maintenanceMessage ||
                    'Sistem sedang dalam pemeliharaan oleh Admin.'}
                </p>
              </div>
            </div>
          )}

          {globalConfig?.announcementActive && globalConfig?.announcementMessage && (
            <div className="rounded-[18px] bg-[#ECFDF5] dark:bg-[#064E3B]/45 border border-[#A7F3D0] dark:border-[#059669] px-3.5 py-3 flex items-start gap-2.5 text-xs text-[#065F46] dark:text-[#A7F3D0]">
              <Megaphone className="w-4 h-4 shrink-0 mt-0.5 text-[#047857] dark:text-[#34D399]" />
              <div>
                <p className="font-bold">{globalConfig.announcementTitle || 'Info Safar Tracking'}</p>
                <p className="mt-0.5 leading-relaxed">{globalConfig.announcementMessage}</p>
              </div>
            </div>
          )}

          {/* Account & Customize Profile Card */}
          <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-3 shadow-xs">
            <div className="flex items-center justify-between gap-2.5">
              <div className="flex items-center gap-2.5 min-w-0">
                <ConvoyAvatar
                  preset={userProfile?.avatarPreset || 'HELMET_TOURING'}
                  photoUrl={userProfile?.avatarPhotoUrl}
                  name={displayName}
                  sizeClass="w-11 h-11"
                  ringClass="ring-2 ring-[#047857]/70 dark:ring-[#34D399]/70"
                />
                <div className="min-w-0">
                  <p className="text-sm font-bold truncate">{displayName}</p>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                    {displayEmail
                      ? `${displayEmail} · ${PROVIDER_TAG[authProvider]}`
                      : 'Belum terhubung email · Atur profil untuk sinkronisasi'}
                  </p>
                  {metaSubtitle && (
                    <p className="text-[11px] font-medium text-[#047857] dark:text-[#6EE7B7] truncate">
                      {metaSubtitle}
                    </p>
                  )}
                </div>
              </div>

              {hasAnySession && (
                <button
                  type="button"
                  onClick={onGoogleSignOut}
                  aria-label="Keluar Aplikasi"
                  className="px-2.5 py-1.5 rounded-lg border border-[#D8E6DD] dark:border-[#1E3F31] text-[11px] font-semibold text-[#2D4A3D] dark:text-[#D1FAE5] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] flex items-center gap-1 cursor-pointer shrink-0 whitespace-nowrap"
                >
                  <LogOut className="w-3 h-3" />
                  <span>Keluar</span>
                </button>
              )}
            </div>

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onOpenCustomizeProfile}
                className="flex-1 min-h-[40px] px-3 py-2 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/60 text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold flex items-center justify-center gap-1.5 hover:bg-[#D1FAE5] dark:hover:bg-[#065F46] transition-colors cursor-pointer whitespace-nowrap"
              >
                <UserCog className="w-3.5 h-3.5 shrink-0" />
                <span>Atur Profil Jamaah</span>
              </button>

              {!authUserEmail && (
                <button
                  type="button"
                  disabled={isSigningIn}
                  onClick={onGoogleSignIn}
                  className="px-3 min-h-[40px] py-2 rounded-xl bg-[#047857] hover:bg-[#065F46] disabled:opacity-60 text-white text-xs font-semibold cursor-pointer whitespace-nowrap shrink-0"
                >
                  {isSigningIn ? 'Memuat...' : 'Sync Google'}
                </button>
              )}
            </div>
          </div>

          {/* Admin Backend Console Quick Access */}
          {isAdminUser && onOpenAdminBackend && (
            <button
              type="button"
              onClick={onOpenAdminBackend}
              className="w-full text-left rounded-[20px] bg-gradient-to-r from-[#064E3B] to-[#041D14] text-white border border-[#10B981]/50 p-4 shadow-sm flex items-center gap-3 hover:opacity-95 active:scale-[0.99] transition-all cursor-pointer"
            >
              <div className="w-11 h-11 rounded-[13px] bg-white/15 flex items-center justify-center shrink-0">
                <ShieldCheck className="w-5 h-5 text-[#FCD34D]" />
              </div>
              <div className="min-w-0 flex-1">
                <p className="text-sm font-bold truncate">Admin Backend Console</p>
                <p className="text-[11px] text-[#A7F3D0] truncate">
                  Pantau jamaah aktif, grup berjalan, &amp; atur konfigurasi global
                </p>
              </div>
              <ArrowRight className="w-4 h-4 text-[#FCD34D] shrink-0" />
            </button>
          )}

          {/* Card Header 1: Buat Rombongan Baru */}
          <div className="rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 shadow-xs space-y-3">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-[14px] bg-[#ECFDF5] dark:bg-[#064E3B] flex items-center justify-center gap-1 shrink-0">
                <Bus className="w-4 h-4 text-[#047857] dark:text-[#34D399]" />
                <Footprints className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D]" />
              </div>
              <div className="min-w-0">
                <h2 className="text-base font-bold leading-tight">
                  Buat Rombongan Baru
                </h2>
                <p className="text-xs font-medium text-[#047857] dark:text-[#34D399]">
                  Mutawwif / Pembimbing · Rute &amp; Grup Baru
                </p>
              </div>
            </div>

            <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
              {roomCreationPaused
                ? 'Pembuatan grup rombongan baru sedang dijeda sementara oleh Admin Backend.'
                : 'Tentukan hotel/miqat awal, tujuan Masjidil Haram atau ziarah, pos kumpul maktab, dan batas jarak jamaah tertinggal.'}
            </p>

            <button
              type="button"
              disabled={roomCreationPaused}
              onClick={() => onSelectRole('TOUR_LEADER')}
              className="w-full min-h-[48px] px-4 py-3 rounded-[14px] bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 disabled:cursor-not-allowed active:scale-[0.98] text-white text-sm font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <span className="whitespace-nowrap">
                {roomCreationPaused ? 'Pembuatan Grup Dijeda Admin' : 'Buat Rombongan Baru'}
              </span>
              <ArrowRight className="w-4 h-4 shrink-0" />
            </button>
          </div>

          {/* Card Header 2: Gabung Rombongan */}
          <div className="rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 shadow-xs space-y-3">
            <div className="flex items-center gap-3">
              <div className="w-12 h-12 rounded-[14px] bg-[#FFFBEB] dark:bg-[#451A03] flex items-center justify-center shrink-0">
                <Link2 className="w-5 h-5 text-[#B45309] dark:text-[#FCD34D]" />
              </div>
              <div className="min-w-0">
                <h2 className="text-base font-bold leading-tight">Gabung Rombongan</h2>
                <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                  Jamaah · Kode Grup atau Link Mutawwif
                </p>
              </div>
            </div>

            <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
              Masukkan Kode Grup Rombongan atau tempel tautan undangan dari Mutawwif untuk langsung terpantau di peta.
            </p>

            <button
              type="button"
              onClick={() => onSelectRole('MEMBER')}
              className="w-full min-h-[48px] px-4 py-3 rounded-[14px] bg-[#B45309] hover:bg-[#92400E] active:scale-[0.98] text-white text-sm font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
            >
              <span className="whitespace-nowrap">Gabung Rombongan</span>
              <ArrowRight className="w-4 h-4 shrink-0" />
            </button>
          </div>

          {/* Bagian "Rombongan Anda Saat Ini" / "Status Rombongan Terakhir" */}
          <div className="pt-1 space-y-2.5">
            <h3 className="text-sm font-bold text-[#0B1F16] dark:text-[#ECFDF5]">
              {activeRoom ? 'Rombongan Anda Saat Ini' : 'Status Rombongan Terakhir'}
            </h3>

            <div
              onClick={() => {
                if (activeRoom) {
                  onResumeCurrentConvoy();
                } else if (lastCompletedTrip) {
                  setShowCompletedModal(true);
                } else {
                  onSelectRole('TOUR_LEADER');
                }
              }}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  if (activeRoom) {
                    onResumeCurrentConvoy();
                  } else if (lastCompletedTrip) {
                    setShowCompletedModal(true);
                  } else {
                    onSelectRole('TOUR_LEADER');
                  }
                }
              }}
              className={`rounded-[18px] bg-white dark:bg-[#0C1E17] border p-3.5 flex items-center gap-3 transition-colors cursor-pointer ${
                activeRoom
                  ? 'border-[#047857] dark:border-[#10B981]/70 hover:border-[#065F46]'
                  : 'border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857]'
              }`}
            >
              <div
                className={`w-11 h-11 rounded-[12px] flex items-center justify-center shrink-0 ${
                  isConvoyFinished
                    ? 'bg-[#F4F8F5] dark:bg-[#091812] text-[#4D6659] dark:text-[#9CC2AE]'
                    : 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399]'
                }`}
              >
                {isConvoyFinished ? (
                  <CheckCircle2 className="w-5 h-5" />
                ) : (
                  <Users className="w-5 h-5" />
                )}
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold truncate">{activeTourName}</p>
                <p
                  className={`text-xs font-medium truncate tabular-nums ${
                    activeRoom
                      ? 'text-[#047857] dark:text-[#34D399]'
                      : 'text-[#4D6659] dark:text-[#9CC2AE]'
                  }`}
                >
                  {activeRoom
                    ? `${activeMemberCount} Jamaah Aktif · Sedang Berjalan`
                    : lastCompletedTrip
                      ? `${activeMemberCount} Jamaah · Status: Selesai (${lastCompletedTrip.timeRangeLabel})`
                      : 'Belum ada rombongan aktif'}
                </p>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                  {activeRoom
                    ? `Tujuan: ${destinationName}`
                    : lastCompletedTrip
                      ? `Tujuan: ${lastCompletedTrip.destinationName} · ${lastCompletedTrip.distanceKm.toFixed(1)} km`
                      : 'Pilih Buat Rombongan Baru atau Gabung Rombongan'}
                </p>
              </div>

              <span
                className={`px-3 py-2 rounded-[10px] text-xs font-semibold shrink-0 ${
                  isConvoyFinished
                    ? 'bg-[#E6EFE9] dark:bg-[#132E24] text-[#1E3A2F] dark:text-[#D1FAE5]'
                    : 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#A7F3D0]'
                }`}
              >
                {activeRoom ? 'Buka' : isConvoyFinished ? 'Selesai' : 'Mulai'}
              </span>
            </div>

            {activeRoom && onOpenRoomActivities && (
              <button
                type="button"
                onClick={onOpenRoomActivities}
                className="w-full text-left rounded-[16px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3.5 py-2.5 flex items-center gap-3 hover:border-[#047857] transition-colors cursor-pointer"
              >
                <div className="w-9 h-9 rounded-[10px] bg-[#FFFBEB] dark:bg-[#451A03] flex items-center justify-center text-[#B45309] dark:text-[#FCD34D] shrink-0">
                  <ClipboardCheck className="w-4 h-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="text-xs font-bold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                    Checklist Kegiatan Rombongan
                  </p>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate tabular-nums">
                    {roomActivities.length > 0
                      ? `${roomActivitiesDoneCount}/${roomActivities.length} agenda selesai dicentang`
                      : 'Kelola atau centang kegiatan umroh grup ini'}
                  </p>
                </div>
                <ArrowRight className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
              </button>
            )}
          </div>

          {/* Modal Pilihan Safar Selesai */}
          {showCompletedModal && lastCompletedTrip && (
            <div
              role="dialog"
              aria-modal="true"
              aria-labelledby="completed-tour-modal-title"
              className="fixed inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-4"
              onClick={() => setShowCompletedModal(false)}
            >
              <div
                className="w-full max-w-[340px] rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] shadow-2xl p-4 space-y-3.5"
                onClick={(e) => e.stopPropagation()}
              >
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <h4
                      id="completed-tour-modal-title"
                      className="text-base font-bold text-[#0B1F16] dark:text-[#ECFDF5]"
                    >
                      Perjalanan Safar Selesai
                    </h4>
                    <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] mt-0.5">
                      Pilih langkah selanjutnya untuk perjalanan ini
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowCompletedModal(false)}
                    aria-label="Tutup dialog"
                    className="p-1.5 rounded-lg text-[#4D6659] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] cursor-pointer"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>

                <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 space-y-1">
                  <p className="text-sm font-bold text-[#0B1F16] dark:text-[#ECFDF5] truncate">
                    {lastCompletedTrip.tourTitle}
                  </p>
                  <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] truncate">
                    Tujuan: {lastCompletedTrip.destinationName}
                  </p>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                    {lastCompletedTrip.distanceKm.toFixed(1)} km · {lastCompletedTrip.durationLabel} · {lastCompletedTrip.timeRangeLabel}
                  </p>
                </div>

                <div className="space-y-2">
                  <button
                    type="button"
                    disabled={roomCreationPaused}
                    onClick={() => {
                      setShowCompletedModal(false);
                      onRepeatTour?.(lastCompletedTrip);
                    }}
                    className="w-full min-h-[44px] px-4 py-2.5 rounded-xl bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
                  >
                    <RotateCcw className="w-4 h-4 shrink-0" />
                    <span>
                      {roomCreationPaused ? 'Pembuatan Grup Dijeda Admin' : 'Ulangi Rute Ziarah'}
                    </span>
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setShowCompletedModal(false);
                      onOpenTripHistory();
                    }}
                    className="w-full min-h-[44px] px-4 py-2.5 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/50 border border-[#A7F3D0] dark:border-[#059669] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold flex items-center justify-center gap-2 hover:bg-[#D1FAE5] transition-colors cursor-pointer"
                  >
                    <History className="w-4 h-4 shrink-0" />
                    <span>Masuk ke Riwayat Safar</span>
                  </button>

                  <button
                    type="button"
                    onClick={() => setShowCompletedModal(false)}
                    className="w-full py-2 text-xs font-medium text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#0B1F16] dark:hover:text-white cursor-pointer"
                  >
                    Tutup
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Bagian "Riwayat Safar & Ekspor Log" */}
          <div className="pt-1 space-y-2.5">
            <h3 className="text-sm font-bold text-[#0B1F16] dark:text-[#ECFDF5]">
              Riwayat Safar &amp; Ekspor Log
            </h3>

            <button
              type="button"
              onClick={onOpenTripHistory}
              className="w-full text-left rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 flex items-center gap-3 hover:border-[#B45309] transition-colors cursor-pointer"
            >
              <div className="w-11 h-11 rounded-[12px] bg-[#FFFBEB] dark:bg-[#451A03] flex items-center justify-center text-[#B45309] dark:text-[#FCD34D] shrink-0">
                <History className="w-5 h-5" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold truncate">
                  Riwayat Perjalanan ({tripHistoryCount})
                </p>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                  {latestTripTitle
                    ? `Terakhir: ${latestTripTitle}`
                    : 'Rekam jejak rute Makkah/Madinah & ekspor GPX'}
                </p>
              </div>

              <ArrowRight className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
            </button>
          </div>

          {/* Bagian "Panduan & Checklist Manasik" */}
          {onOpenManasikGuide && (
            <div className="pt-1 space-y-2.5">
              <h3 className="text-sm font-bold text-[#0B1F16] dark:text-[#ECFDF5]">
                Checklist &amp; Panduan Manasik
              </h3>

              <button
                type="button"
                onClick={onOpenManasikGuide}
                className="w-full text-left rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 flex items-center gap-3 hover:border-[#047857] transition-colors cursor-pointer"
              >
                <div className="w-11 h-11 rounded-[12px] bg-[#ECFDF5] dark:bg-[#064E3B] flex items-center justify-center text-[#047857] dark:text-[#34D399] shrink-0">
                  <BookOpenCheck className="w-5 h-5" />
                </div>

                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold truncate">
                    Checklist Langkah Umroh &amp; Haji
                  </p>
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate tabular-nums">
                    Umroh {umrahDoneCount}/{umrahTotalCount} · Haji {hajiDoneCount}/{hajiTotalCount} · Ihram, Tawaf, Sa&apos;i
                  </p>
                </div>

                <ArrowRight className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
              </button>

              {onOpenAssistant && (
                <button
                  type="button"
                  onClick={onOpenAssistant}
                  className="w-full text-left rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 flex items-center gap-3 hover:border-[#B45309] transition-colors cursor-pointer"
                >
                  <div className="w-11 h-11 rounded-[12px] bg-[#FFFBEB] dark:bg-[#451A03] flex items-center justify-center text-[#B45309] dark:text-[#FCD34D] shrink-0">
                    <Sparkles className="w-5 h-5" />
                  </div>

                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-semibold truncate">
                      Tanya Mutawwif AI (Gemini)
                    </p>
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                      Konsultasi doa, tata cara manasik &amp; koordinasi rombongan
                    </p>
                  </div>

                  <ArrowRight className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
                </button>
              )}
            </div>
          )}

          {/* Bagian "Tentang Aplikasi & Kredit Pembuat" */}
          <div className="pt-1 space-y-2.5">
            <button
              type="button"
              onClick={onOpenAboutApp}
              className="w-full text-left rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 flex items-center gap-3 hover:border-[#047857] transition-colors cursor-pointer"
            >
              <div className="w-11 h-11 rounded-[12px] bg-[#ECFDF5] dark:bg-[#064E3B] flex items-center justify-center text-[#047857] dark:text-[#34D399] shrink-0">
                <BadgeCheck className="w-5 h-5" />
              </div>

              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold truncate">Tentang Aplikasi</p>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                  App Created &amp; Designed by Kang Ekie
                </p>
              </div>

              <ArrowRight className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
            </button>
          </div>

          {/* Quiet Footer Credit */}
          <p className="pt-1 pb-2 text-center text-[11px] font-medium text-[#4D6659] dark:text-[#9CC2AE]">
            Created by Kang Ekie
          </p>
        </div>
      </div>
    </div>
  );
}
