import { useEffect, useState } from 'react';
import { Clock, Globe2 } from 'lucide-react';
import { buildDualClockSnapshot } from '../utils/timezoneClock';

interface DualTimezoneClockBarProps {
  latitude?: number | null;
  longitude?: number | null;
  subjectName?: string;
  compact?: boolean;
  className?: string;
}

export function DualTimezoneClockBar({
  latitude,
  longitude,
  subjectName,
  compact = false,
  className = '',
}: DualTimezoneClockBarProps) {
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  const clock = buildDualClockSnapshot(nowMs, latitude, longitude);
  const locationSubtitle = subjectName
    ? `${subjectName} · ${clock.localRegionLabel}`
    : clock.localRegionLabel;

  if (compact) {
    return (
      <div
        aria-label="Jam Lokasi Jamaah dan Jam Fix WIB"
        className={`flex items-center justify-between gap-2 px-3 py-1.5 bg-[#F4F8F5] dark:bg-[#091812] border-b border-[#D8E6DD] dark:border-[#1B3B2E] text-[11px] ${className}`}
      >
        <div className="flex items-center gap-1.5 min-w-0">
          <Globe2 className="w-3.5 h-3.5 text-[#047857] dark:text-[#34D399] shrink-0" />
          <span className="text-[#4D6659] dark:text-[#9CC2AE] truncate">
            {locationSubtitle}:
          </span>
          <span className="font-mono font-bold tabular-nums text-[#0B1F16] dark:text-[#ECFDF5] shrink-0">
            {clock.localTime}
          </span>
          <span className="px-1.5 py-0.5 rounded bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] font-mono font-bold text-[9px] shrink-0">
            {clock.localZoneCode}
          </span>
        </div>

        <div className="flex items-center gap-1.5 shrink-0 pl-2 border-l border-[#D8E6DD] dark:border-[#1B3B2E]">
          <Clock className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FBBF24] shrink-0" />
          <span className="font-mono font-bold tabular-nums text-[#0B1F16] dark:text-[#ECFDF5]">
            {clock.wibTime}
          </span>
          <span className="px-1.5 py-0.5 rounded bg-[#FEF3C7] dark:bg-[#451A03] text-[#92400E] dark:text-[#FCD34D] font-mono font-bold text-[9px]">
            WIB
          </span>
        </div>
      </div>
    );
  }

  return (
    <div
      aria-label="Jam Lokasi Jamaah dan Jam Fix WIB"
      className={`grid grid-cols-2 gap-2 ${className}`}
    >
      {/* Jam Mengikuti Lokasi Jamaah */}
      <div className="rounded-xl bg-[#ECFDF5]/80 dark:bg-[#064E3B]/35 border border-[#A7F3D0] dark:border-[#065F46] px-2.5 py-2 min-w-0">
        <div className="flex items-center justify-between gap-1">
          <span className="flex items-center gap-1 text-[10px] font-semibold text-[#065F46] dark:text-[#A7F3D0] truncate">
            <Globe2 className="w-3 h-3 shrink-0" />
            <span className="truncate">{locationSubtitle}</span>
          </span>
          <span className="px-1.5 py-0.5 rounded bg-[#047857] text-white font-mono text-[9px] font-bold shrink-0">
            {clock.localZoneCode}
          </span>
        </div>
        <div className="flex items-baseline justify-between gap-1 mt-1">
          <span className="font-mono text-sm font-extrabold tabular-nums text-[#0B1F16] dark:text-[#ECFDF5]">
            {clock.localTime}
          </span>
          <span className="text-[9px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
            {clock.localDateLabel}
          </span>
        </div>
      </div>

      {/* Jam Fix Waktu Indonesia Barat (WIB) */}
      <div className="rounded-xl bg-[#FFFBEB]/90 dark:bg-[#451A03]/35 border border-[#FDE68A] dark:border-[#92400E] px-2.5 py-2 min-w-0">
        <div className="flex items-center justify-between gap-1">
          <span className="flex items-center gap-1 text-[10px] font-semibold text-[#92400E] dark:text-[#FCD34D] truncate">
            <Clock className="w-3 h-3 shrink-0" />
            <span className="truncate">Waktu Indonesia Barat</span>
          </span>
          <span className="px-1.5 py-0.5 rounded bg-[#B45309] text-white font-mono text-[9px] font-bold shrink-0">
            WIB
          </span>
        </div>
        <div className="flex items-baseline justify-between gap-1 mt-1">
          <span className="font-mono text-sm font-extrabold tabular-nums text-[#0B1F16] dark:text-[#ECFDF5]">
            {clock.wibTime}
          </span>
          <span className="text-[9px] font-mono text-[#92400E] dark:text-[#FCD34D] truncate">
            {clock.isSameAsWib ? clock.wibDateLabel : clock.diffToWibLabel}
          </span>
        </div>
      </div>
    </div>
  );
}
