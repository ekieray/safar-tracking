import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Compass,
  Database,
  Globe,
  Mail,
  Megaphone,
  Power,
  RefreshCw,
  Save,
  Search,
  Settings,
  ShieldAlert,
  ShieldCheck,
  Smartphone,
  Trash2,
  UserPlus,
  Users,
  Wrench,
} from 'lucide-react';
import { User } from 'firebase/auth';
import {
  AdminRoomOverview,
  AdminUserSummary,
  GlobalAppConfig,
  RegisteredTourLeader,
  UserPresenceRecord,
} from '../../types/convoy';
import {
  addRegisteredTourLeader,
  ADMIN_EMAIL,
  adminDeleteRoom,
  adminResolveRoomSos,
  adminSetRoomStatus,
  DEFAULT_GLOBAL_APP_CONFIG,
  isValidTourLeaderEmail,
  isVerifiedAdminSession,
  removeRegisteredTourLeader,
  removeSyncedMember,
  saveGlobalAppConfig,
  subscribeToAdminMonitoring,
  subscribeToRegisteredTourLeaders,
} from '../../services/convoySyncService';
import { ConvoyAvatar } from '../ConvoyAvatar';

interface AdminBackendViewProps {
  authUser: User | null;
  globalConfig: GlobalAppConfig;
  isSigningIn: boolean;
  onGoogleSignIn: () => void;
  onInspectRoom?: (roomId: string) => void;
  onBack: () => void;
}

function formatAgeLabel(lastSeenMs: number): { label: string; isOnline: boolean } {
  const diffSec = Math.max(0, Math.floor((Date.now() - lastSeenMs) / 1000));
  if (diffSec <= 90) return { label: 'Online sekarang', isOnline: true };
  if (diffSec <= 180) return { label: `${Math.floor(diffSec / 60)} mnt lalu`, isOnline: true };
  if (diffSec < 3600) return { label: `${Math.floor(diffSec / 60)} mnt lalu`, isOnline: false };
  return { label: `${Math.floor(diffSec / 3600)} jam lalu`, isOnline: false };
}

export function AdminBackendView({
  authUser,
  globalConfig,
  isSigningIn,
  onGoogleSignIn,
  onBack,
}: AdminBackendViewProps) {
  const isVerifiedAdmin = isVerifiedAdminSession(authUser);

  const [activeTab, setActiveTab] = useState<'monitoring' | 'settings'>('monitoring');
  const [presenceList, setPresenceList] = useState<UserPresenceRecord[]>([]);
  const [rooms, setRooms] = useState<AdminRoomOverview[]>([]);
  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [monitorError, setMonitorError] = useState<string | null>(null);
  const [expandedRoomId, setExpandedRoomId] = useState<string | null>(null);
  const [userSearch, setUserSearch] = useState('');
  const [actionBusyKey, setActionBusyKey] = useState<string | null>(null);

  // Global config draft state
  const [configDraft, setConfigDraft] = useState<GlobalAppConfig>(
    globalConfig || DEFAULT_GLOBAL_APP_CONFIG
  );
  const [isSavingConfig, setIsSavingConfig] = useState(false);
  const [configFeedback, setConfigFeedback] = useState<string | null>(null);
  const [tourLeaders, setTourLeaders] = useState<RegisteredTourLeader[]>([]);
  const [leaderEmailInput, setLeaderEmailInput] = useState('');
  const [leaderLabelInput, setLeaderLabelInput] = useState('');
  const [isSavingLeader, setIsSavingLeader] = useState(false);
  const [leaderFeedback, setLeaderFeedback] = useState<string | null>(null);
  const [leaderError, setLeaderError] = useState<string | null>(null);
  const [, setClockTick] = useState(0);

  useEffect(() => {
    setConfigDraft(globalConfig);
  }, [globalConfig]);

  // Refresh relative timestamps every 15 seconds
  useEffect(() => {
    const id = setInterval(() => setClockTick((v) => v + 1), 15000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    if (!isVerifiedAdmin) return;
    setMonitorError(null);
    const unsubMonitor = subscribeToAdminMonitoring({
      onPresence: setPresenceList,
      onRooms: setRooms,
      onUsers: setUsers,
      onError: (msg) => setMonitorError(msg),
    });
    const unsubLeaders = subscribeToRegisteredTourLeaders(
      (list) => setTourLeaders(list),
      (msg) => setLeaderError(msg)
    );
    return () => {
      unsubMonitor();
      unsubLeaders();
    };
  }, [isVerifiedAdmin]);

  const metrics = useMemo(() => {
    const now = Date.now();
    const onlineUsers = presenceList.filter((p) => now - p.lastSeenMs <= 180_000);
    const travelingOnline = onlineUsers.filter((p) => p.isTraveling);
    const activeRooms = rooms.filter((r) => r.status === 'ACTIVE');
    const activeRoomMembers = activeRooms.reduce((acc, r) => acc + r.members.length, 0);
    const activeSosCount = activeRooms.filter((r) => r.sosActive).length;
    const webOnline = onlineUsers.filter((p) => p.platform === 'web').length;
    const androidOnline = onlineUsers.filter((p) => p.platform === 'android').length;

    return {
      onlineCount: onlineUsers.length,
      travelingCount: Math.max(travelingOnline.length, activeRoomMembers),
      activeRoomsCount: activeRooms.length,
      totalRegisteredUsers: users.length,
      activeSosCount,
      webOnline,
      androidOnline,
    };
  }, [presenceList, rooms, users]);

  const filteredUsers = useMemo(() => {
    const q = userSearch.trim().toLowerCase();
    if (!q) return users;
    return users.filter(
      (u) =>
        u.fullName.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.clubName.toLowerCase().includes(q) ||
        u.homeCity.toLowerCase().includes(q) ||
        u.licensePlate.toLowerCase().includes(q)
    );
  }, [users, userSearch]);

  const handleSaveGlobalConfig = async () => {
    if (!authUser || !isVerifiedAdmin) return;
    setIsSavingConfig(true);
    setConfigFeedback(null);
    try {
      await saveGlobalAppConfig(authUser.uid, configDraft);
      setConfigFeedback(
        'Pengaturan backend berhasil disimpan & diterapkan ke seluruh pengguna Safar Tracking.'
      );
    } catch (err) {
      setConfigFeedback(
        err instanceof Error
          ? `Gagal menyimpan pengaturan: ${err.message}`
          : 'Gagal menyimpan pengaturan global.'
      );
    } finally {
      setIsSavingConfig(false);
    }
  };

  const handleRoomStatusToggle = async (room: AdminRoomOverview) => {
    setActionBusyKey(`status-${room.roomId}`);
    try {
      await adminSetRoomStatus(
        room.roomId,
        room.status === 'ACTIVE' ? 'FINISHED' : 'ACTIVE'
      );
    } catch (err) {
      setMonitorError(
        err instanceof Error ? err.message : 'Gagal mengubah status grup rombongan.'
      );
    } finally {
      setActionBusyKey(null);
    }
  };

  const handleResolveSos = async (roomId: string) => {
    setActionBusyKey(`sos-${roomId}`);
    try {
      await adminResolveRoomSos(roomId);
    } catch (err) {
      setMonitorError(
        err instanceof Error ? err.message : 'Gagal menyelesaikan SOS.'
      );
    } finally {
      setActionBusyKey(null);
    }
  };

  const handleRemoveMember = async (roomId: string, memberUid: string) => {
    setActionBusyKey(`kick-${roomId}-${memberUid}`);
    try {
      await removeSyncedMember(roomId, memberUid);
    } catch (err) {
      setMonitorError(
        err instanceof Error ? err.message : 'Gagal mengeluarkan jamaah.'
      );
    } finally {
      setActionBusyKey(null);
    }
  };

  const handleDeleteRoom = async (roomId: string) => {
    setActionBusyKey(`del-${roomId}`);
    try {
      await adminDeleteRoom(roomId);
    } catch (err) {
      setMonitorError(
        err instanceof Error ? err.message : 'Gagal menghapus grup rombongan.'
      );
    } finally {
      setActionBusyKey(null);
    }
  };

  const handleAddTourLeader = async () => {
    if (!authUser || !isVerifiedAdmin) return;
    const cleanEmail = leaderEmailInput.trim().toLowerCase();
    setLeaderFeedback(null);
    setLeaderError(null);
    if (!isValidTourLeaderEmail(cleanEmail)) {
      setLeaderError('Masukkan alamat email Ketua Rombongan yang valid (contoh: mutawwif@travel.com).');
      return;
    }
    setIsSavingLeader(true);
    try {
      await addRegisteredTourLeader(authUser.uid, cleanEmail, leaderLabelInput);
      setLeaderEmailInput('');
      setLeaderLabelInput('');
      setLeaderFeedback(
        `Email ${cleanEmail} terdaftar sebagai Ketua Rombongan & langsung dapat membuat rombongan baru.`
      );
    } catch (err) {
      setLeaderError(
        err instanceof Error
          ? `Gagal menambahkan Ketua Rombongan: ${err.message}`
          : 'Gagal menambahkan email Ketua Rombongan.'
      );
    } finally {
      setIsSavingLeader(false);
    }
  };

  const handleRemoveTourLeader = async (leader: RegisteredTourLeader) => {
    if (!isVerifiedAdmin) return;
    setActionBusyKey(`tl-del-${leader.id}`);
    setLeaderFeedback(null);
    setLeaderError(null);
    try {
      await removeRegisteredTourLeader(leader.id);
      setLeaderFeedback(`Akses Ketua Rombongan untuk ${leader.email} telah dicabut.`);
    } catch (err) {
      setLeaderError(
        err instanceof Error
          ? `Gagal menghapus Ketua Rombongan: ${err.message}`
          : 'Gagal mencabut akses Ketua Rombongan.'
      );
    } finally {
      setActionBusyKey(null);
    }
  };

  return (
    <div className="flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Sticky Top App Bar */}
      <div className="sticky top-0 z-20 flex items-center justify-between gap-2 px-4 py-3 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="flex items-center gap-2 min-w-0">
          <button
            type="button"
            onClick={onBack}
            aria-label="Kembali"
            className="min-w-[40px] min-h-[40px] rounded-xl flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 cursor-pointer shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <h1 className="text-sm font-bold truncate">Admin Safar Tracking</h1>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              Pemantauan Jamaah &amp; Kontrol Konfigurasi Cloud
            </p>
          </div>
        </div>

        <span className="px-2.5 py-1 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B]/70 text-[#047857] dark:text-[#6EE7B7] text-[10px] font-bold shrink-0">
          {isVerifiedAdmin ? 'ADMIN VERIFIED' : 'AUTH CHECK'}
        </span>
      </div>

      {/* Main Content */}
      <div className="flex-1 px-4 py-4 space-y-4">
        {!isVerifiedAdmin ? (
          <div className="rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 space-y-4 shadow-xs">
            <div className="flex items-start gap-3">
              <div className="w-11 h-11 rounded-[14px] bg-[#FFFBEB] dark:bg-[#451A03] flex items-center justify-center text-[#B45309] dark:text-[#FCD34D] shrink-0">
                <ShieldAlert className="w-5 h-5" />
              </div>
              <div className="space-y-1">
                <h2 className="text-sm font-bold">
                  Verifikasi Akun Admin Diperlukan
                </h2>
                <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
                  Untuk menjaga keamanan database Firestore, akses pemantauan seluruh jamaah, grup rombongan aktif, serta perubahan pengaturan global hanya dibuka untuk akun terverifikasi{' '}
                  <span className="font-semibold text-[#047857] dark:text-[#6EE7B7]">
                    {ADMIN_EMAIL}
                  </span>
                  .
                </p>
              </div>
            </div>

            <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 text-[11px] space-y-1 text-[#2D4A3D] dark:text-[#9CC2AE]">
              <p>
                Status saat ini:{' '}
                <span className="font-semibold text-[#0B1F16] dark:text-[#ECFDF5]">
                  {authUser?.email
                    ? `${authUser.email} (${authUser.emailVerified ? 'Terverifikasi' : 'Belum terverifikasi'})`
                    : 'Belum login Firebase (Mode Lokal/Tamu)'}
                </span>
              </p>
            </div>

            <button
              type="button"
              disabled={isSigningIn}
              onClick={onGoogleSignIn}
              className="w-full min-h-[48px] px-4 py-3 rounded-[14px] bg-[#047857] hover:bg-[#065F46] disabled:opacity-60 text-white text-sm font-semibold flex items-center justify-center gap-2 cursor-pointer transition-colors"
            >
              <ShieldCheck className="w-4 h-4 shrink-0" />
              <span>
                {isSigningIn
                  ? 'Menghubungkan Google...'
                  : `Masuk Admin dengan Google (${ADMIN_EMAIL})`}
              </span>
            </button>
          </div>
        ) : (
          <>
            {/* Admin Identity Strip */}
            <div className="rounded-[18px] bg-[#064E3B] text-white p-3.5 flex items-center justify-between gap-3">
              <div className="flex items-center gap-2.5 min-w-0">
                <div className="w-9 h-9 rounded-xl bg-[#047857] border border-[#34D399]/30 flex items-center justify-center shrink-0">
                  <Database className="w-4 h-4 text-[#FCD34D]" />
                </div>
                <div className="min-w-0">
                  <p className="text-xs font-bold truncate">
                    Firestore Live Backend
                  </p>
                  <p className="text-[11px] text-[#A7F3D0] truncate">
                    Operator: {authUser?.email}
                  </p>
                </div>
              </div>
              <div className="text-right shrink-0">
                <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-white/10 text-[#6EE7B7] text-[10px] font-bold">
                  SYNC REAL-TIME
                </span>
              </div>
            </div>

            {/* 4 Live KPI Cards */}
            <div className="grid grid-cols-2 gap-2.5">
              <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-1">
                <div className="flex items-center justify-between text-[#047857] dark:text-[#34D399]">
                  <span className="text-[11px] font-semibold">Online Sekarang</span>
                  <Activity className="w-4 h-4" />
                </div>
                <p className="text-2xl font-extrabold tabular-nums">
                  {metrics.onlineCount}
                </p>
                <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Web: {metrics.webOnline} · Android: {metrics.androidOnline}
                </p>
              </div>

              <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-1">
                <div className="flex items-center justify-between text-[#B45309] dark:text-[#FCD34D]">
                  <span className="text-[11px] font-semibold">Sedang Safar</span>
                  <Compass className="w-4 h-4" />
                </div>
                <p className="text-2xl font-extrabold tabular-nums">
                  {metrics.travelingCount}
                </p>
                <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Jamaah aktif dalam perjalanan
                </p>
              </div>

              <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-1">
                <div className="flex items-center justify-between text-[#047857] dark:text-[#34D399]">
                  <span className="text-[11px] font-semibold">Grup Rombongan</span>
                  <Users className="w-4 h-4" />
                </div>
                <p className="text-2xl font-extrabold tabular-nums">
                  {metrics.activeRoomsCount}
                </p>
                <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                  {metrics.activeSosCount > 0
                    ? `${metrics.activeSosCount} Grup SOS Darurat!`
                    : `Total ${rooms.length} grup tercatat`}
                </p>
              </div>

              <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-1">
                <div className="flex items-center justify-between text-[#2D4A3D] dark:text-[#D1FAE5]">
                  <span className="text-[11px] font-semibold">Akun Terdaftar</span>
                  <ShieldCheck className="w-4 h-4" />
                </div>
                <p className="text-2xl font-extrabold tabular-nums">
                  {metrics.totalRegisteredUsers}
                </p>
                <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Profil jamaah tersinkronisasi cloud
                </p>
              </div>
            </div>

            {monitorError && (
              <div className="rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 px-3 py-2.5 text-xs text-red-700 dark:text-red-300 flex items-start gap-2">
                <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                <span>{monitorError}</span>
              </div>
            )}

            {/* Sub-Navigation Tabs */}
            <div className="grid grid-cols-2 gap-1.5 p-1 rounded-2xl bg-[#E6EFE9] dark:bg-[#091812]">
              <button
                type="button"
                onClick={() => setActiveTab('monitoring')}
                className={`py-2.5 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                  activeTab === 'monitoring'
                    ? 'bg-white dark:bg-[#0C1E17] text-[#047857] dark:text-[#6EE7B7] shadow-xs'
                    : 'text-[#4D6659] dark:text-[#9CC2AE]'
                }`}
              >
                <Activity className="w-3.5 h-3.5" />
                <span>Pantau Aktif</span>
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('settings')}
                className={`py-2.5 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all cursor-pointer ${
                  activeTab === 'settings'
                    ? 'bg-white dark:bg-[#0C1E17] text-[#B45309] dark:text-[#FCD34D] shadow-xs'
                    : 'text-[#4D6659] dark:text-[#9CC2AE]'
                }`}
              >
                <Settings className="w-3.5 h-3.5" />
                <span>Pengaturan Backend</span>
              </button>
            </div>

            {activeTab === 'monitoring' ? (
              <div className="space-y-4 pb-6">
                {/* Quick Shortcut to Tour Leader Email Allowlist */}
                <button
                  type="button"
                  onClick={() => setActiveTab('settings')}
                  className="w-full text-left rounded-[18px] bg-[#ECFDF5] dark:bg-[#064E3B]/45 border border-[#A7F3D0] dark:border-[#059669] px-3.5 py-3 flex items-center justify-between gap-3 hover:bg-[#D1FAE5] dark:hover:bg-[#065F46]/60 transition-colors cursor-pointer"
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div className="w-9 h-9 rounded-xl bg-[#047857] text-white flex items-center justify-center shrink-0">
                      <UserPlus className="w-4 h-4" />
                    </div>
                    <div className="min-w-0">
                      <p className="text-xs font-bold text-[#064E3B] dark:text-[#ECFDF5] truncate">
                        Email Ketua Rombongan ({tourLeaders.length} terdaftar)
                      </p>
                      <p className="text-[11px] text-[#047857] dark:text-[#6EE7B7] truncate">
                        Kelola siapa saja yang boleh membuat rombongan baru
                      </p>
                    </div>
                  </div>
                  <span className="px-2.5 py-1 rounded-lg bg-white dark:bg-[#0C1E17] text-[#047857] dark:text-[#6EE7B7] text-[10px] font-bold shrink-0">
                    Kelola
                  </span>
                </button>

                {/* Section 1: Grup Rombongan Berjalan & Kontrol */}
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between">
                    <h2 className="text-xs font-bold uppercase tracking-wider text-[#4D6659] dark:text-[#9CC2AE]">
                      Grup Rombongan &amp; Kontrol ({rooms.length})
                    </h2>
                  </div>

                  {rooms.length === 0 ? (
                    <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 text-center text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                      Belum ada grup rombongan umroh tersinkronisasi di cloud.
                    </div>
                  ) : (
                    <div className="space-y-2.5">
                      {rooms.map((room) => {
                        const isExpanded = expandedRoomId === room.roomId;
                        const isActive = room.status === 'ACTIVE';
                        return (
                          <div
                            key={room.roomId}
                            className={`rounded-[18px] bg-white dark:bg-[#0C1E17] border p-3.5 space-y-3 ${
                              room.sosActive
                                ? 'border-red-400 dark:border-red-700'
                                : 'border-[#D8E6DD] dark:border-[#1B3B2E]'
                            }`}
                          >
                            <div className="flex items-start justify-between gap-2">
                              <div className="min-w-0 flex-1">
                                <div className="flex items-center gap-1.5 flex-wrap">
                                  <span className="px-2 py-0.5 rounded-md bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-[10px] font-bold">
                                    {room.roomId}
                                  </span>
                                  <span
                                    className={`px-2 py-0.5 rounded-md text-[10px] font-bold ${
                                      isActive
                                        ? 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300'
                                        : 'bg-[#F4F8F5] dark:bg-[#091812] text-[#4D6659] dark:text-[#9CC2AE]'
                                    }`}
                                  >
                                    {isActive ? 'AKTIF' : 'SELESAI'}
                                  </span>
                                  {room.sosActive && (
                                    <span className="px-2 py-0.5 rounded-md bg-red-600 text-white text-[10px] font-bold">
                                      SOS: {room.sosSenderName || 'DARURAT'}
                                    </span>
                                  )}
                                </div>
                                <p className="text-sm font-bold mt-1 truncate">
                                  {room.roomTitle}
                                </p>
                                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                                  Tujuan: {room.destinationName} · {room.members.length}{' '}
                                  jamaah
                                </p>
                              </div>

                              <button
                                type="button"
                                onClick={() =>
                                  setExpandedRoomId(isExpanded ? null : room.roomId)
                                }
                                className="p-2 rounded-xl hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] text-[#4D6659] cursor-pointer shrink-0"
                                aria-label="Detail Grup"
                              >
                                {isExpanded ? (
                                  <ChevronUp className="w-4 h-4" />
                                ) : (
                                  <ChevronDown className="w-4 h-4" />
                                )}
                              </button>
                            </div>

                            {/* Action Buttons */}
                            <div className="flex flex-wrap items-center gap-2">
                              <button
                                type="button"
                                disabled={actionBusyKey === `status-${room.roomId}`}
                                onClick={() => handleRoomStatusToggle(room)}
                                className={`px-3 py-1.5 rounded-xl text-[11px] font-semibold flex items-center gap-1 cursor-pointer transition-colors ${
                                  isActive
                                    ? 'bg-[#FFFBEB] dark:bg-[#451A03]/60 text-[#B45309] dark:text-[#FCD34D] hover:bg-[#FEF3C7]'
                                    : 'bg-[#ECFDF5] dark:bg-[#064E3B]/60 text-[#047857] dark:text-[#6EE7B7]'
                                }`}
                              >
                                <Power className="w-3 h-3" />
                                <span>
                                  {isActive ? 'Akhiri Perjalanan' : 'Aktifkan Lagi'}
                                </span>
                              </button>

                              {room.sosActive && (
                                <button
                                  type="button"
                                  disabled={actionBusyKey === `sos-${room.roomId}`}
                                  onClick={() => handleResolveSos(room.roomId)}
                                  className="px-3 py-1.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-[11px] font-semibold flex items-center gap-1 cursor-pointer"
                                >
                                  <CheckCircle2 className="w-3 h-3" />
                                  <span>Reset SOS</span>
                                </button>
                              )}

                              {!isActive && (
                                <button
                                  type="button"
                                  disabled={actionBusyKey === `del-${room.roomId}`}
                                  onClick={() => handleDeleteRoom(room.roomId)}
                                  className="px-2.5 py-1.5 rounded-xl border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-[11px] font-semibold flex items-center gap-1 cursor-pointer hover:bg-red-50 dark:hover:bg-red-950/40"
                                >
                                  <Trash2 className="w-3 h-3" />
                                  <span>Hapus</span>
                                </button>
                              )}
                            </div>

                            {isExpanded && (
                              <div className="pt-2 border-t border-[#D8E6DD] dark:border-[#1B3B2E] space-y-2">
                                <p className="text-[11px] font-semibold text-[#4D6659] dark:text-[#9CC2AE]">
                                  Daftar Jamaah di Grup ({room.members.length})
                                </p>
                                {room.members.length === 0 ? (
                                  <p className="text-[11px] text-[#4D6659]">
                                    Tidak ada jamaah terhubung.
                                  </p>
                                ) : (
                                  room.members.map((m) => (
                                    <div
                                      key={m.id}
                                      className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] px-2.5 py-2 flex items-center justify-between gap-2"
                                    >
                                      <div className="min-w-0 flex-1">
                                        <p className="text-xs font-bold truncate">
                                          {m.fullName}{' '}
                                          <span className="font-normal text-[10px] text-[#4D6659]">
                                            ({m.role === 'TOUR_LEADER' ? 'Mutawwif' : m.isSweeper ? 'Pendamping' : 'Jamaah'})
                                          </span>
                                        </p>
                                        <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate tabular-nums">
                                          {m.vehicleDetail || (m.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki')} · {m.licensePlate} ·{' '}
                                          <span className="font-semibold text-[#047857] dark:text-[#34D399]">
                                            {m.mapPosition.speedKmh} km/j
                                          </span>{' '}
                                          · {m.conditionStatus}
                                        </p>
                                      </div>

                                      <button
                                        type="button"
                                        disabled={
                                          actionBusyKey === `kick-${room.roomId}-${m.id}`
                                        }
                                        onClick={() =>
                                          handleRemoveMember(room.roomId, m.id)
                                        }
                                        className="px-2 py-1 rounded-lg text-[10px] font-semibold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 cursor-pointer shrink-0"
                                      >
                                        Keluarkan
                                      </button>
                                    </div>
                                  ))
                                )}
                              </div>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* Section 2: Jamaah Online & Aktivitas Real-Time */}
                <div className="space-y-2.5">
                  <h2 className="text-xs font-bold uppercase tracking-wider text-[#4D6659] dark:text-[#9CC2AE]">
                    Pengguna Aktif &amp; Sedang Safar ({presenceList.length})
                  </h2>

                  {presenceList.length === 0 ? (
                    <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 text-center text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                      Belum ada heartbeat pengguna aktif tercatat.
                    </div>
                  ) : (
                    <div className="space-y-2">
                      {presenceList.map((p) => {
                        const age = formatAgeLabel(p.lastSeenMs);
                        return (
                          <div
                            key={p.uid}
                            className="rounded-[16px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3.5 py-2.5 flex items-center justify-between gap-2.5"
                          >
                            <div className="flex items-center gap-2.5 min-w-0">
                              <div
                                className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 ${
                                  p.platform === 'android'
                                    ? 'bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300'
                                    : 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7]'
                                }`}
                              >
                                {p.platform === 'android' ? (
                                  <Smartphone className="w-4 h-4" />
                                ) : (
                                  <Globe className="w-4 h-4" />
                                )}
                              </div>
                              <div className="min-w-0">
                                <div className="flex items-center gap-1.5">
                                  <span
                                    className={`w-2 h-2 rounded-full shrink-0 ${
                                      age.isOnline ? 'bg-emerald-500' : 'bg-slate-400'
                                    }`}
                                  />
                                  <p className="text-xs font-bold truncate">
                                    {p.fullName}
                                  </p>
                                </div>
                                <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                                  {p.email || 'Tanpa email'} · {p.platform.toUpperCase()} ·{' '}
                                  {age.label}
                                </p>
                              </div>
                            </div>

                            <div className="text-right shrink-0">
                              {p.isTraveling ? (
                                <>
                                  <span className="inline-block px-2 py-0.5 rounded-md bg-[#FFFBEB] dark:bg-[#451A03]/60 text-[#B45309] dark:text-[#FCD34D] text-[10px] font-bold">
                                    Safar · {p.speedKmh} km/j
                                  </span>
                                  {p.activeRoomId && (
                                    <p className="text-[10px] text-[#4D6659] mt-0.5">
                                      Grup {p.activeRoomId}
                                    </p>
                                  )}
                                </>
                              ) : (
                                <span className="inline-block px-2 py-0.5 rounded-md bg-[#F4F8F5] dark:bg-[#091812] text-[#4D6659] dark:text-[#9CC2AE] text-[10px] font-semibold">
                                  Di Aplikasi
                                </span>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>

                {/* Section 3: Direktori Jamaah Terdaftar */}
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <h2 className="text-xs font-bold uppercase tracking-wider text-[#4D6659] dark:text-[#9CC2AE]">
                      Direktori Jamaah ({users.length})
                    </h2>
                  </div>

                  <div className="relative">
                    <Search className="w-4 h-4 text-[#4D6659] absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={userSearch}
                      onChange={(e) => setUserSearch(e.target.value)}
                      placeholder="Cari nama, email, travel, embarkasi, atau kloter..."
                      className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857]"
                    />
                  </div>

                  <div className="space-y-2">
                    {filteredUsers.slice(0, 30).map((u) => (
                      <div
                        key={u.uid}
                        className="rounded-[16px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3.5 py-2.5 flex items-center gap-3"
                      >
                        <ConvoyAvatar
                          preset={u.avatarPreset}
                          photoUrl={u.avatarPhotoUrl}
                          name={u.fullName}
                          sizeClass="w-9 h-9"
                        />
                        <div className="min-w-0 flex-1">
                          <div className="flex items-center gap-1.5">
                            <p className="text-xs font-bold truncate">{u.fullName}</p>
                            <span className="px-1.5 py-0.5 rounded bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-[9px] font-bold uppercase">
                              {u.authProvider}
                            </span>
                          </div>
                          <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                            {u.email || 'Belum ada email'}
                            {u.clubName ? ` · ${u.clubName}` : ''}
                            {u.homeCity ? ` (${u.homeCity})` : ''}
                          </p>
                          {(u.vehicleDetail || u.licensePlate) && (
                            <p className="text-[10px] text-[#2D4A3D] dark:text-[#D1FAE5] truncate">
                              {u.vehicleDetail || u.vehicleType}
                              {u.licensePlate ? ` · ${u.licensePlate}` : ''}
                            </p>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            ) : (
              /* TAB 2: PENGATURAN BACKEND GLOBAL */
              <div className="space-y-4">
                {/* Kelola Email Ketua Rombongan Terdaftar */}
                <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3.5">
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <UserPlus className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
                      <div>
                        <h2 className="text-sm font-bold">
                          Email Ketua Rombongan (Mutawwif)
                        </h2>
                        <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                          Menu &quot;Buat Rombongan Baru&quot; hanya tampil bagi email di daftar ini (+ Admin). Pengguna lain hanya dapat Gabung Rombongan.
                        </p>
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded-full bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-[10px] font-bold shrink-0 tabular-nums">
                      {tourLeaders.length}
                    </span>
                  </div>

                  <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3 py-2 text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5] flex items-center gap-2">
                    <ShieldCheck className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
                    <span className="truncate">
                      Admin ({ADMIN_EMAIL}) selalu aktif sebagai Ketua Rombongan.
                    </span>
                  </div>

                  <div className="space-y-2">
                    <div className="relative">
                      <Mail className="w-4 h-4 text-[#4D6659] absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="email"
                        value={leaderEmailInput}
                        onChange={(e) => setLeaderEmailInput(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            e.preventDefault();
                            handleAddTourLeader();
                          }
                        }}
                        placeholder="Email Google Ketua Rombongan (misal: mutawwif@gmail.com)"
                        className="w-full pl-9 pr-3 py-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857]"
                      />
                    </div>
                    <input
                      type="text"
                      maxLength={80}
                      value={leaderLabelInput}
                      onChange={(e) => setLeaderLabelInput(e.target.value)}
                      placeholder="Nama / Kloter / Travel (opsional, misal: Ust. Fauzi · Kloter 01)"
                      className="w-full px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857]"
                    />
                    <button
                      type="button"
                      disabled={isSavingLeader || !leaderEmailInput.trim()}
                      onClick={handleAddTourLeader}
                      className="w-full min-h-[42px] px-3.5 py-2 rounded-xl bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
                    >
                      {isSavingLeader ? (
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <UserPlus className="w-3.5 h-3.5" />
                      )}
                      <span>
                        {isSavingLeader ? 'Mendaftarkan...' : 'Daftarkan Email Ketua Rombongan'}
                      </span>
                    </button>
                  </div>

                  {leaderError && (
                    <div className="rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 px-3 py-2 text-xs text-red-700 dark:text-red-300 flex items-start gap-2">
                      <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
                      <span>{leaderError}</span>
                    </div>
                  )}

                  {leaderFeedback && (
                    <div className="rounded-xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 px-3 py-2 text-xs text-emerald-700 dark:text-emerald-300 flex items-start gap-2">
                      <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
                      <span>{leaderFeedback}</span>
                    </div>
                  )}

                  {tourLeaders.length === 0 ? (
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] text-center py-2">
                      Belum ada email Ketua Rombongan tambahan yang didaftarkan.
                    </p>
                  ) : (
                    <div className="space-y-2 max-h-60 overflow-y-auto pr-0.5">
                      {tourLeaders.map((leader) => (
                        <div
                          key={leader.id}
                          className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3 py-2 flex items-center justify-between gap-2.5"
                        >
                          <div className="min-w-0 flex-1">
                            <p className="text-xs font-semibold truncate">{leader.email}</p>
                            <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                              {leader.label ? `${leader.label} · ` : ''}
                              Terdaftar {leader.createdAtLabel}
                            </p>
                          </div>
                          <button
                            type="button"
                            disabled={actionBusyKey === `tl-del-${leader.id}`}
                            onClick={() => handleRemoveTourLeader(leader)}
                            aria-label={`Hapus ${leader.email}`}
                            className="p-2 rounded-lg text-red-600 hover:bg-red-50 dark:hover:bg-red-950/40 disabled:opacity-50 cursor-pointer shrink-0"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>

                {/* Default Rombongan Tolerances */}
                <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3.5">
                  <div className="flex items-center gap-2">
                    <Wrench className="w-4 h-4 text-[#047857] dark:text-[#34D399]" />
                    <h2 className="text-sm font-bold">
                      Pengaturan Default Rombongan Global
                    </h2>
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-xs font-semibold">
                      Default Toleransi Keluar Jalur (meter)
                    </label>
                    <input
                      type="number"
                      min={10}
                      max={5000}
                      value={configDraft.defaultOffRouteToleranceMeters}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          defaultOffRouteToleranceMeters:
                            Number(e.target.value) || 75,
                        }))
                      }
                      className="w-full px-3 py-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-sm outline-none focus:border-[#047857]"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-xs font-semibold">
                      Default Batas Jarak Jamaah Terpisah (meter)
                    </label>
                    <input
                      type="number"
                      min={100}
                      max={50000}
                      step={100}
                      value={configDraft.defaultMaxStragglerDistanceMeters}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          defaultMaxStragglerDistanceMeters:
                            Number(e.target.value) || 1000,
                        }))
                      }
                      className="w-full px-3 py-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-sm outline-none focus:border-[#047857]"
                    />
                  </div>

                  <label className="flex items-center justify-between gap-3 pt-1 cursor-pointer">
                    <div>
                      <p className="text-xs font-semibold">
                        Alarm Suara Peringatan Default
                      </p>
                      <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                        Aktifkan peringatan audio saat jamaah keluar jalur/terpisah
                      </p>
                    </div>
                    <input
                      type="checkbox"
                      checked={configDraft.defaultSoundAlertEnabled}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          defaultSoundAlertEnabled: e.target.checked,
                        }))
                      }
                      className="w-5 h-5 accent-[#047857]"
                    />
                  </label>
                </div>

                {/* Broadcast Pengumuman */}
                <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2">
                      <Megaphone className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D]" />
                      <h2 className="text-sm font-bold">
                        Banner Pengumuman Global
                      </h2>
                    </div>
                    <input
                      type="checkbox"
                      checked={configDraft.announcementActive}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          announcementActive: e.target.checked,
                        }))
                      }
                      className="w-5 h-5 accent-[#B45309]"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-xs font-semibold">
                      Judul Pengumuman
                    </label>
                    <input
                      type="text"
                      maxLength={80}
                      value={configDraft.announcementTitle}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          announcementTitle: e.target.value,
                        }))
                      }
                      placeholder="Contoh: Info Waktu Berkumpul Pelataran Masjidil Haram"
                      className="w-full px-3 py-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857]"
                    />
                  </div>

                  <div className="space-y-1.5">
                    <label className="block text-xs font-semibold">
                      Isi Pesan Broadcast ke Semua Jamaah
                    </label>
                    <textarea
                      rows={2}
                      maxLength={240}
                      value={configDraft.announcementMessage}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          announcementMessage: e.target.value,
                        }))
                      }
                      placeholder="Tulis pengumuman perjalanan umroh untuk seluruh jamaah & mutawwif..."
                      className="w-full px-3 py-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs outline-none focus:border-[#047857] resize-none"
                    />
                  </div>
                </div>

                {/* Kontrol Akses & Maintenance */}
                <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
                  <div className="flex items-center gap-2">
                    <ShieldCheck className="w-4 h-4 text-[#047857] dark:text-[#34D399]" />
                    <h2 className="text-sm font-bold">Kontrol Akses &amp; Sistem</h2>
                  </div>

                  <label className="flex items-center justify-between gap-3 cursor-pointer">
                    <div>
                      <p className="text-xs font-semibold">
                        Izinkan Pembuatan Grup Baru
                      </p>
                      <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                        Nonaktifkan sementara jika ingin menutup grup baru
                      </p>
                    </div>
                    <input
                      type="checkbox"
                      checked={configDraft.allowNewRooms}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          allowNewRooms: e.target.checked,
                        }))
                      }
                      className="w-5 h-5 accent-[#047857]"
                    />
                  </label>

                  <label className="flex items-center justify-between gap-3 cursor-pointer">
                    <div>
                      <p className="text-xs font-semibold">
                        Izinkan Mode Tamu (Tanpa Akun)
                      </p>
                      <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                        Matikan untuk mewajibkan login Google/Email/Sosmed
                      </p>
                    </div>
                    <input
                      type="checkbox"
                      checked={configDraft.allowGuestMode}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          allowGuestMode: e.target.checked,
                        }))
                      }
                      className="w-5 h-5 accent-[#047857]"
                    />
                  </label>

                  <div className="pt-1 border-t border-[#D8E6DD] dark:border-[#1B3B2E] space-y-2.5">
                    <label className="flex items-center justify-between gap-3 cursor-pointer">
                      <div>
                        <p className="text-xs font-semibold text-red-600 dark:text-red-400">
                          Mode Pemeliharaan (Maintenance)
                        </p>
                        <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                          Tampilkan peringatan pemeliharaan ke seluruh pengguna
                        </p>
                      </div>
                      <input
                        type="checkbox"
                        checked={configDraft.maintenanceMode}
                        onChange={(e) =>
                          setConfigDraft((prev) => ({
                            ...prev,
                            maintenanceMode: e.target.checked,
                          }))
                        }
                        className="w-5 h-5 accent-red-600"
                      />
                    </label>

                    <textarea
                      rows={2}
                      maxLength={240}
                      value={configDraft.maintenanceMessage}
                      onChange={(e) =>
                        setConfigDraft((prev) => ({
                          ...prev,
                          maintenanceMessage: e.target.value,
                        }))
                      }
                      placeholder="Pesan saat mode maintenance aktif..."
                      className="w-full px-3 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs outline-none focus:border-red-500 resize-none"
                    />
                  </div>
                </div>

                {configFeedback && (
                  <div className="rounded-xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 px-3 py-2.5 text-xs text-emerald-700 dark:text-emerald-300 flex items-start gap-2">
                    <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
                    <span>{configFeedback}</span>
                  </div>
                )}

                {/* Sticky Bottom Save CTA */}
                <div className="sticky bottom-0 z-10 -mx-4 px-4 pt-3 pb-[calc(2rem+env(safe-area-inset-bottom))] bg-gradient-to-t from-[#F4F8F5] via-[#F4F8F5]/95 to-transparent dark:from-[#06130E] dark:via-[#06130E]/95">
                  <button
                    type="button"
                    disabled={isSavingConfig}
                    onClick={handleSaveGlobalConfig}
                    className="w-full min-h-[50px] px-4 py-3 rounded-[14px] bg-[#047857] hover:bg-[#065F46] active:scale-[0.98] disabled:opacity-60 text-white text-sm font-semibold flex items-center justify-center gap-2 shadow-sm cursor-pointer transition-all"
                  >
                    {isSavingConfig ? (
                      <RefreshCw className="w-4 h-4 animate-spin" />
                    ) : (
                      <Save className="w-4 h-4" />
                    )}
                    <span>
                      {isSavingConfig
                        ? 'Menyimpan ke Cloud...'
                        : 'Simpan & Terapkan ke Backend'}
                    </span>
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
