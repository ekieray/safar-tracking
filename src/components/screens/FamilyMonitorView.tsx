import { useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Eye,
  LocateFixed,
  LogOut,
  MapPin,
  Navigation,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import {
  ConvoyRouteConfig,
  DestinationEtaInfo,
  FamilyGrantRecord,
  FamilyMonitorSnapshot,
  RoomActivityItem,
} from '../../types/convoy';
import { ConvoyAvatar } from '../ConvoyAvatar';
import { DualTimezoneClockBar } from '../DualTimezoneClockBar';
import { LiveGoogleConvoyMap } from '../map/LiveGoogleConvoyMap';

interface FamilyMonitorViewProps {
  grant: FamilyGrantRecord;
  snapshot: FamilyMonitorSnapshot | null;
  errorMessage?: string | null;
  darkMode?: boolean;
  nowMs?: number;
  onLogoutFamily: () => void;
}

const FALLBACK_ROUTE: ConvoyRouteConfig = {
  destinationName: 'Masjidil Haram / Tujuan Ziarah',
  destinationAddress: 'Makkah Al-Mukarramah',
  waypoints: [
    {
      xFraction: 0.2,
      yFraction: 0.8,
      latitude: 21.418,
      longitude: 39.8275,
    },
    {
      xFraction: 0.8,
      yFraction: 0.2,
      latitude: 21.4225,
      longitude: 39.8262,
    },
  ],
  checkpoints: [],
  totalRouteMeters: 5000,
  offRouteToleranceMeters: 75,
  maxStragglerDistanceMeters: 1000,
  soundAlertEnabled: false,
};

const EMPTY_STRAGGLER = {
  isTriggered: false,
  trailingMember: null,
  aheadMember: null,
  gapMeters: 0,
  maxAllowedMeters: 1000,
};

const EMPTY_SOS = {
  isActive: false,
  senderId: '',
  senderName: '',
  senderVehicle: '',
  latitude: 0,
  longitude: 0,
  locationText: '',
  triggeredAtLabel: '',
};

export function FamilyMonitorView({
  grant,
  snapshot,
  errorMessage,
  darkMode = false,
  nowMs = Date.now(),
  onLogoutFamily,
}: FamilyMonitorViewProps) {
  const [activeTab, setActiveTab] = useState<'map' | 'activities'>('map');
  const [focusPulse, setFocusPulse] = useState(0);

  const target = snapshot?.targetMember ?? null;
  const routeConfig = snapshot?.routeConfig ?? FALLBACK_ROUTE;
  const activities: RoomActivityItem[] = snapshot?.activities ?? [];
  const completedSet = new Set(snapshot?.completedActivityIds ?? []);
  const completedTimes = snapshot?.completedTimes ?? {};

  const completedActivities = activities.filter((a) => completedSet.has(a.id));
  const upcomingActivities = activities.filter((a) => !completedSet.has(a.id));

  const isTargetMissing = Boolean(
    target &&
      (target.connectionStatus === 'OFFLINE' ||
        (target.lastUpdatedMs && target.lastUpdatedMs > 0 && nowMs - target.lastUpdatedMs >= 75_000))
  );

  const remainingMeters = target
    ? Math.max(
        0,
        Math.round((1 - Math.max(0, Math.min(1, target.mapPosition.routeProgress))) * routeConfig.totalRouteMeters)
      )
    : routeConfig.totalRouteMeters;

  const etaInfo: DestinationEtaInfo = {
    remainingMeters,
    speedKmh: target?.mapPosition.speedKmh ?? 0,
    etaMinutes: null,
    isStopped: (target?.mapPosition.speedKmh ?? 0) <= 0,
    isArrived: remainingMeters <= 50,
    durationLabel: remainingMeters <= 50 ? 'Tiba' : 'Pantau Live',
    arrivalClockLabel: snapshot?.lastSyncLabel || '-',
    distanceLabel:
      remainingMeters >= 1000
        ? `${(remainingMeters / 1000).toFixed(1)} km`
        : `${remainingMeters} m`,
  };

  if (snapshot && !snapshot.linkActive) {
    return (
      <div className="flex flex-col min-h-full bg-[#F4F8F5] dark:bg-[#07140F] text-[#0B1F16] dark:text-[#ECFDF5] p-5 justify-center items-center">
        <div className="w-full max-w-md rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-6 space-y-4 text-center shadow-sm">
          <div className="w-12 h-12 mx-auto rounded-2xl bg-[#FEF2F2] dark:bg-[#450A0A] text-[#B91C1C] dark:text-[#FCA5A5] flex items-center justify-center">
            <ShieldAlert className="w-6 h-6" />
          </div>
          <div className="space-y-1.5">
            <h1 className="text-lg font-bold">Akses Pantauan Keluarga Dihentikan</h1>
            <p className="text-xs leading-relaxed text-[#4D6659] dark:text-[#9CC2AE]">
              Tautan kode <span className="font-mono font-semibold">{grant.inviteCode}</span> telah
              dinonaktifkan atau diganti oleh <span className="font-semibold">{grant.targetName}</span>.
              Minta link terbaru jika Anda perlu memantau kembali.
            </p>
          </div>
          <button
            type="button"
            onClick={onLogoutFamily}
            className="w-full min-h-[44px] px-4 py-2.5 rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
          >
            <LogOut className="w-4 h-4" />
            <span>Kembali ke Halaman Masuk</span>
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full min-h-0 bg-[#F4F8F5] dark:bg-[#07140F] text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Header Bar */}
      <header className="shrink-0 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E] px-4 py-3 flex items-center justify-between gap-3">
        <div className="min-w-0 flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] flex items-center justify-center shrink-0">
            <Eye className="w-4 h-4" />
          </div>
          <div className="min-w-0">
            <p className="text-sm font-bold truncate">
              {snapshot?.roomTitle || 'Pantauan Keluarga'}
            </p>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              Mode Keluarga · Hanya Baca · Kode {grant.inviteCode}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={onLogoutFamily}
          className="min-h-[40px] px-3 py-2 rounded-xl border border-[#C6DACD] dark:border-[#1E3F31] text-xs font-semibold text-[#B91C1C] dark:text-[#FCA5A5] hover:bg-[#FEF2F2] dark:hover:bg-[#450A0A]/50 flex items-center gap-1.5 cursor-pointer shrink-0"
        >
          <LogOut className="w-3.5 h-3.5" />
          <span>Keluar</span>
        </button>
      </header>

      {/* Error Notice */}
      {errorMessage && (
        <div
          role="alert"
          className="shrink-0 mx-3 mt-3 rounded-xl bg-[#FEF2F2] dark:bg-[#450A0A] border border-[#FECACA] dark:border-[#991B1B] px-3.5 py-2.5 text-xs text-[#B91C1C] dark:text-[#FCA5A5]"
        >
          {errorMessage}
        </div>
      )}

      {/* Missing Pilgrim Warning Banner for Family */}
      {isTargetMissing && target && (
        <div
          role="alert"
          className="shrink-0 mx-3 mt-3 rounded-xl bg-[#B91C1C] text-white px-3.5 py-3 flex items-center justify-between gap-3 shadow-sm"
        >
          <div className="flex items-start gap-2.5 min-w-0">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <div className="min-w-0">
              <p className="text-xs font-bold">Sinyal Jamaah Tidak Terdeteksi</p>
              <p className="text-[11px] text-white/90 truncate">
                {target.fullName} · Terakhir terpantau {target.lastUpdatedLabel} ·{' '}
                {target.lastLocationText}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => {
              setActiveTab('map');
              setFocusPulse((n) => n + 1);
            }}
            className="min-h-[36px] px-2.5 py-1.5 rounded-lg bg-white text-[#B91C1C] text-[11px] font-bold shrink-0 cursor-pointer"
          >
            Fokus Lokasi
          </button>
        </div>
      )}

      {/* Single Tracked Jamaah Identity Summary */}
      <div className="shrink-0 px-3 pt-3">
        <div className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-2.5">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3 min-w-0">
              <ConvoyAvatar
                preset={target?.avatarPreset || 'HELMET_TOURING'}
                name={target?.fullName || grant.targetName}
                sizeClass="w-11 h-11"
              />
              <div className="min-w-0">
                <p className="text-sm font-bold truncate">
                  {target?.fullName || grant.targetName}
                </p>
                <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] truncate">
                  {target?.vehicleDetail || 'Jamaah Rombongan'}
                  {' · '}
                  Kondisi: {target?.conditionStatus || 'AMAN'}
                </p>
              </div>
            </div>

            <div className="text-right shrink-0">
              <p className="text-xs font-bold font-mono tabular-nums text-[#047857] dark:text-[#6EE7B7]">
                {target ? `${target.mapPosition.speedKmh} km/j` : '--'}
              </p>
              <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                Update {target?.lastUpdatedLabel || snapshot?.lastSyncLabel || '-'}
              </p>
            </div>
          </div>

          <div className="pt-2 border-t border-[#E6EFE9] dark:border-[#1B3B2E] flex items-center justify-between gap-2 text-xs text-[#2D4A3D] dark:text-[#D1FAE5]">
            <div className="flex items-center gap-1.5 min-w-0">
              <MapPin className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
              <span className="truncate">
                {target?.lastLocationText || 'Menunggu sinyal posisi jamaah...'}
              </span>
            </div>
            <span className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] shrink-0">
              Tujuan: {snapshot?.destinationName || routeConfig.destinationName}
            </span>
          </div>

          <DualTimezoneClockBar
            latitude={
              target?.mapPosition.latitude ?? routeConfig.waypoints[0]?.latitude
            }
            longitude={
              target?.mapPosition.longitude ??
              routeConfig.waypoints[0]?.longitude
            }
            subjectName={target?.fullName || grant.targetName}
          />
        </div>
      </div>

      {/* Mode Switcher: Peta Live vs Progres Kegiatan */}
      <div className="shrink-0 px-3 pt-2.5">
        <div className="grid grid-cols-2 p-1 rounded-xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E]">
          <button
            type="button"
            onClick={() => setActiveTab('map')}
            className={`min-h-[40px] rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
              activeTab === 'map'
                ? 'bg-[#047857] text-white'
                : 'text-[#4D6659] dark:text-[#9CC2AE]'
            }`}
          >
            <Navigation className="w-3.5 h-3.5" />
            <span>Posisi Live</span>
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('activities')}
            className={`min-h-[40px] rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
              activeTab === 'activities'
                ? 'bg-[#047857] text-white'
                : 'text-[#4D6659] dark:text-[#9CC2AE]'
            }`}
          >
            <CheckCircle2 className="w-3.5 h-3.5" />
            <span>
              Kegiatan ({completedActivities.length}/{activities.length})
            </span>
          </button>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 min-h-0 p-3 flex flex-col">
        {activeTab === 'map' ? (
          <div className="relative flex-1 min-h-[280px] rounded-2xl overflow-hidden border border-[#D8E6DD] dark:border-[#1B3B2E] bg-[#EBF3EE] dark:bg-[#091812]">
            {target ? (
              <LiveGoogleConvoyMap
                key={focusPulse}
                members={[target]}
                focusedMember={target}
                routeConfig={routeConfig}
                vehicleType={target.vehicleType}
                stragglerAlert={EMPTY_STRAGGLER}
                sosState={EMPTY_SOS}
                etaInfo={etaInfo}
                darkMode={darkMode}
                isPickingDestination={false}
                isPickingCheckpoint={false}
                isNavigationMode={false}
                cleanNavMode={false}
                canPingRouting={false}
                onSelectMember={() => {}}
                onPickCoordinateOnMap={() => {}}
              />
            ) : (
              <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-2">
                <LocateFixed className="w-7 h-7 text-[#047857] dark:text-[#34D399]" />
                <p className="text-sm font-semibold">Menunggu Data Posisi Jamaah</p>
                <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] max-w-xs">
                  Posisi akan muncul otomatis segera setelah {grant.targetName} membagikan GPS di
                  dalam rombongan aktif.
                </p>
              </div>
            )}

            {/* Quick Read-Only Activity Summary Overlay */}
            <div className="absolute bottom-3 left-3 right-3 z-20 rounded-xl bg-white/95 dark:bg-[#0C1E17]/95 backdrop-blur-md border border-[#D8E6DD] dark:border-[#1B3B2E] px-3.5 py-2.5 flex items-center justify-between gap-3 shadow-sm">
              <div className="min-w-0">
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                  Progres Kegiatan {target?.fullName || grant.targetName}
                </p>
                <p className="text-xs font-bold truncate">
                  {activities.length === 0
                    ? 'Jadwal kegiatan belum ditambahkan pembimbing'
                    : upcomingActivities[0]
                      ? `Berikutnya: ${upcomingActivities[0].title}${
                          upcomingActivities[0].timeLabel
                            ? ` (${upcomingActivities[0].timeLabel})`
                            : ''
                        }`
                      : 'Seluruh kegiatan terjadwal telah selesai'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActiveTab('activities')}
                className="min-h-[36px] px-3 py-1.5 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold shrink-0 cursor-pointer"
              >
                Lihat Daftar
              </button>
            </div>
          </div>
        ) : (
          <div className="flex-1 min-h-0 overflow-y-auto space-y-4 pb-4">
            {/* Read-only Security Notice */}
            <div className="rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B]/40 border border-[#A7F3D0] dark:border-[#059669] px-3.5 py-2.5 flex items-center gap-2 text-xs text-[#065F46] dark:text-[#A7F3D0]">
              <ShieldCheck className="w-4 h-4 shrink-0" />
              <span>
                Tampilan hanya-baca. Hanya {grant.targetName} atau pembimbing yang memperbarui
                checklist ibadah ini.
              </span>
            </div>

            {activities.length === 0 ? (
              <div className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-6 text-center space-y-1.5">
                <p className="text-sm font-bold">Belum Ada Jadwal Kegiatan</p>
                <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                  Ketua Rombongan belum menambahkan agenda kegiatan untuk perjalanan ini.
                </p>
              </div>
            ) : (
              <>
                {/* Sudah Dilaksanakan */}
                <section className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <h2 className="text-sm font-bold text-[#047857] dark:text-[#6EE7B7]">
                      Sudah Dilaksanakan ({completedActivities.length})
                    </h2>
                  </div>

                  {completedActivities.length === 0 ? (
                    <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                      Belum ada kegiatan yang ditandai selesai oleh jamaah.
                    </p>
                  ) : (
                    <div className="divide-y divide-[#E6EFE9] dark:divide-[#1B3B2E]">
                      {completedActivities.map((act) => (
                        <div key={act.id} className="py-2.5 first:pt-0 last:pb-0 flex items-start gap-2.5">
                          <CheckCircle2 className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0 mt-0.5" />
                          <div className="min-w-0 flex-1">
                            <p className="text-xs font-semibold">{act.title}</p>
                            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                              {act.timeLabel || 'Terjadwal'}
                              {act.locationNote ? ` · ${act.locationNote}` : ''}
                              {completedTimes[act.id] ? ` · Selesai ${completedTimes[act.id]}` : ''}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </section>

                {/* Akan Dilaksanakan */}
                <section className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <h2 className="text-sm font-bold text-[#B45309] dark:text-[#FCD34D]">
                      Akan Dilaksanakan ({upcomingActivities.length})
                    </h2>
                  </div>

                  {upcomingActivities.length === 0 ? (
                    <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                      Semua kegiatan terjadwal telah diselesaikan.
                    </p>
                  ) : (
                    <div className="divide-y divide-[#E6EFE9] dark:divide-[#1B3B2E]">
                      {upcomingActivities.map((act) => (
                        <div key={act.id} className="py-2.5 first:pt-0 last:pb-0 flex items-start gap-2.5">
                          <Clock className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D] shrink-0 mt-0.5" />
                          <div className="min-w-0 flex-1">
                            <p className="text-xs font-semibold">{act.title}</p>
                            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                              {act.timeLabel || 'Segera menyusul'}
                              {act.locationNote ? ` · ${act.locationNote}` : ''}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              </>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
