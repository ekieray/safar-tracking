import { useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Calendar,
  Clock,
  Gauge,
  History,
  Route,
} from 'lucide-react';
import { TripHistoryRecord } from '../../types/convoy';
import { projectTripMapViewport } from '../../utils/geo';
import { ConvoyAvatar } from '../ConvoyAvatar';

interface TripHistoryViewProps {
  trips: TripHistoryRecord[];
  onSelectTrip: (tripId: string) => void;
  onBack: () => void;
}

type RoleFilter = 'ALL' | 'TOUR_LEADER' | 'MEMBER';

export function TripHistoryView({
  trips,
  onSelectTrip,
  onBack,
}: TripHistoryViewProps) {
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL');

  const filteredTrips = trips.filter((trip) => {
    if (roleFilter === 'ALL') return true;
    return trip.userRole === roleFilter;
  });

  const totalDistanceKm = trips.reduce((acc, t) => acc + t.distanceKm, 0);

  return (
    <div className="flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Top App Bar */}
      <div className="sticky top-0 z-10 flex items-center gap-1.5 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="button"
          onClick={onBack}
          aria-label="Kembali"
          className="min-w-[42px] min-h-[42px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="min-w-0">
          <h2 className="text-sm font-bold truncate">Riwayat Safar</h2>
          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate font-mono">
            {trips.length} Perjalanan · {totalDistanceKm.toFixed(1)} km Total
          </p>
        </div>
      </div>

      {/* Body */}
      <div className="flex-1 px-4 py-4 space-y-3.5">
        {/* Filter Chips */}
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
          {(
            [
              { id: 'ALL', label: 'Semua' },
              { id: 'TOUR_LEADER', label: 'Sebagai Mutawwif' },
              { id: 'MEMBER', label: 'Sebagai Jamaah' },
            ] as Array<{ id: RoleFilter; label: string }>
          ).map((tab) => {
            const active = roleFilter === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                onClick={() => setRoleFilter(tab.id)}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold border whitespace-nowrap transition-colors cursor-pointer ${
                  active
                    ? 'bg-[#047857] border-[#047857] text-white'
                    : 'bg-white dark:bg-[#0C1E17] border-[#D8E6DD] dark:border-[#1B3B2E] text-[#2D4A3D] dark:text-[#D1FAE5]'
                }`}
              >
                {tab.label}
              </button>
            );
          })}
        </div>

        {/* History Cards */}
        {filteredTrips.length === 0 ? (
          <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-6 text-center space-y-2">
            <History className="w-8 h-8 text-[#9CC2AE] mx-auto" />
            <p className="text-sm font-bold">Belum Ada Riwayat</p>
            <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
              Selesaikan perjalanan rombongan umroh untuk merekam rute ziarah dan mengekspor log GPX.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {filteredTrips.map((trip) => {
              const isLeader = trip.userRole === 'TOUR_LEADER';
              const miniProj = projectTripMapViewport(
                trip.waypoints,
                [],
                72,
                50,
                0.18
              );
              const miniPts = miniProj.waypoints
                .map((p) => `${p.x.toFixed(1)},${p.y.toFixed(1)}`)
                .join(' ');
              const miniStart = miniProj.waypoints[0];
              const miniEnd = miniProj.waypoints[miniProj.waypoints.length - 1];
              return (
                <button
                  key={trip.id}
                  type="button"
                  onClick={() => onSelectTrip(trip.id)}
                  className="w-full text-left rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] p-4 space-y-3 transition-colors cursor-pointer"
                >
                  <div className="flex items-start justify-between gap-2.5">
                    <div className="min-w-0 flex-1 space-y-1">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        <span className="px-2 py-0.5 rounded-md bg-[#ECFDF5] dark:bg-[#064E3B]/60 text-[#047857] dark:text-[#6EE7B7] text-[10px] font-mono font-bold">
                          {trip.roomId}
                        </span>
                        <span
                          className={`px-2 py-0.5 rounded-md text-[10px] font-semibold ${
                            isLeader
                              ? 'bg-[#FFFBEB] dark:bg-[#451A03]/60 text-[#B45309] dark:text-[#FCD34D]'
                              : 'bg-[#F4F8F5] dark:bg-[#091812] text-[#2D4A3D] dark:text-[#D1FAE5]'
                          }`}
                        >
                          {isLeader ? 'Mutawwif' : 'Jamaah'}
                        </span>
                      </div>

                      <h3 className="text-sm font-bold truncate">
                        {trip.tourTitle}
                      </h3>

                      <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] flex items-center gap-1">
                        <Calendar className="w-3 h-3 shrink-0" />
                        <span>
                          {trip.dateLabel} · {trip.timeRangeLabel}
                        </span>
                      </p>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      {miniProj.waypoints.length >= 2 && (
                        <div className="w-[72px] h-[50px] rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] overflow-hidden">
                          <svg className="w-full h-full" viewBox="0 0 72 50">
                            <polyline
                              points={miniPts}
                              fill="none"
                              stroke="#047857"
                              strokeWidth="2.5"
                              strokeLinecap="round"
                              strokeLinejoin="round"
                            />
                            {miniStart && (
                              <circle cx={miniStart.x} cy={miniStart.y} r="2.5" fill="#D97706" />
                            )}
                            {miniEnd && (
                              <circle cx={miniEnd.x} cy={miniEnd.y} r="2.5" fill="#047857" />
                            )}
                          </svg>
                        </div>
                      )}
                      <ArrowRight className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
                    </div>
                  </div>

                  {/* Metrics Box */}
                  <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3 py-2 grid grid-cols-3 gap-2">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <Route className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
                      <div className="min-w-0">
                        <p className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE]">
                          Jarak
                        </p>
                        <p className="text-xs font-bold font-mono tabular-nums truncate">
                          {trip.distanceKm.toFixed(1)} km
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 min-w-0">
                      <Clock className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
                      <div className="min-w-0">
                        <p className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE]">
                          Durasi
                        </p>
                        <p className="text-xs font-bold font-mono tabular-nums truncate">
                          {trip.durationLabel}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1.5 min-w-0">
                      <Gauge className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
                      <div className="min-w-0">
                        <p className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE]">
                          Rata-rata
                        </p>
                        <p className="text-xs font-bold font-mono tabular-nums truncate">
                          {trip.avgSpeedKmh} km/j
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* Participants Avatar Strip */}
                  <div className="flex items-start gap-2.5">
                    {trip.participants.length > 0 && (
                      <div className="flex items-center -space-x-2 shrink-0 mt-0.5">
                        {trip.participants.slice(0, 4).map((p) => (
                          <ConvoyAvatar
                            key={p.id}
                            preset={p.avatarPreset}
                            photoUrl={p.avatarPhotoUrl}
                            name={p.fullName}
                            sizeClass="w-6 h-6"
                            ringClass="ring-2 ring-white dark:ring-[#0C1E17]"
                          />
                        ))}
                      </div>
                    )}
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] leading-snug line-clamp-3">
                      {trip.participants.length > 0 ? (
                        <>
                          <span className="font-semibold text-[#1E3A2F] dark:text-[#D1FAE5]">
                            {trip.participants.length} Jamaah Ikut:
                          </span>{' '}
                          {trip.participants.map((p) => p.fullName).join(', ')}
                        </>
                      ) : (
                        'Peserta rombongan belum tercatat'
                      )}
                    </p>
                  </div>
                </button>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
