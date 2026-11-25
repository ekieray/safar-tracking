import { useMemo, useState } from 'react';
import {
  BookOpen,
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  LocateFixed,
  MapPin,
  MapPinCheck,
  Navigation,
  RotateCcw,
  Sparkles,
} from 'lucide-react';
import {
  evaluateManasikLandmarkProximity,
  MANASIK_GUIDE_MAP,
  MANASIK_SIMULATION_LOCATIONS,
} from '../data/manasikGuideData';
import { GpsPermissionStatus } from '../hooks/useDeviceGps';
import {
  ManasikCheckInRecord,
  ManasikGuideType,
  ManasikProgressState,
  ManasikStepCategory,
} from '../types/convoy';

interface ManasikChecklistPanelProps {
  progress: ManasikProgressState;
  onToggleStep: (guideType: ManasikGuideType, stepId: string) => void;
  onCheckInStep: (
    guideType: ManasikGuideType,
    checkIn: ManasikCheckInRecord
  ) => void;
  onResetGuide: (guideType: ManasikGuideType) => void;
  userLocation?: {
    latitude: number;
    longitude: number;
    accuracyMeters?: number;
    sourceLabel?: string;
  } | null;
  gpsStatus?: GpsPermissionStatus;
  onRetryGps?: () => void;
  onOpenAssistant?: (contextPrompt?: string) => void;
  initialGuide?: ManasikGuideType;
  isCloudSynced?: boolean;
  compact?: boolean;
}

type StepFilterMode = 'all' | 'pending' | 'completed';

const CATEGORY_LABEL: Record<ManasikStepCategory, string> = {
  RUKUN: 'Rukun',
  WAJIB: 'Wajib',
  SUNNAH: 'Sunnah',
};

function formatDistanceText(meters: number): string {
  if (meters >= 1000) {
    return `${(meters / 1000).toFixed(1)} km`;
  }
  return `${meters} m`;
}

function currentClockLabel(): string {
  const now = new Date();
  return `${String(now.getHours()).padStart(2, '0')}:${String(
    now.getMinutes()
  ).padStart(2, '0')}`;
}

export function ManasikChecklistPanel({
  progress,
  onToggleStep,
  onCheckInStep,
  onResetGuide,
  userLocation = null,
  gpsStatus = 'idle',
  onRetryGps,
  onOpenAssistant,
  initialGuide = 'umrah',
  isCloudSynced = false,
  compact = false,
}: ManasikChecklistPanelProps) {
  const [activeGuide, setActiveGuide] = useState<ManasikGuideType>(initialGuide);
  const [filterMode, setFilterMode] = useState<StepFilterMode>('all');
  const [expandedStepId, setExpandedStepId] = useState<string | null>(
    MANASIK_GUIDE_MAP[initialGuide][0]?.id ?? null
  );
  const [confirmingReset, setConfirmingReset] = useState(false);
  const [simLocationId, setSimLocationId] = useState<string>('gps');

  const steps = MANASIK_GUIDE_MAP[activeGuide];
  const completedIds = useMemo(
    () => new Set(progress[activeGuide] || []),
    [progress, activeGuide]
  );
  const checkIns = progress.checkIns || {};

  const simulatedPreset = useMemo(
    () =>
      simLocationId === 'gps'
        ? null
        : MANASIK_SIMULATION_LOCATIONS.find((p) => p.id === simLocationId) ?? null,
    [simLocationId]
  );

  const effectiveCoords = useMemo(() => {
    if (simulatedPreset) {
      return {
        latitude: simulatedPreset.latitude,
        longitude: simulatedPreset.longitude,
        label: `Simulasi: ${simulatedPreset.label}`,
        isSimulated: true,
      };
    }
    if (userLocation) {
      return {
        latitude: userLocation.latitude,
        longitude: userLocation.longitude,
        label: userLocation.sourceLabel || 'GPS Perangkat Aktif',
        isSimulated: false,
      };
    }
    return null;
  }, [simulatedPreset, userLocation]);

  const proximityByStepId = useMemo(() => {
    const map = new Map<
      string,
      ReturnType<typeof evaluateManasikLandmarkProximity>
    >();
    if (!effectiveCoords) return map;
    for (const step of steps) {
      map.set(
        step.id,
        evaluateManasikLandmarkProximity(
          step,
          effectiveCoords.latitude,
          effectiveCoords.longitude
        )
      );
    }
    return map;
  }, [steps, effectiveCoords]);

  const nearbyPendingSteps = useMemo(() => {
    if (!effectiveCoords) return [];
    return steps.filter((step) => {
      if (completedIds.has(step.id)) return false;
      const prox = proximityByStepId.get(step.id);
      return Boolean(prox?.isWithinRadius);
    });
  }, [steps, completedIds, proximityByStepId, effectiveCoords]);

  const totalCount = steps.length;
  const completedCount = steps.filter((s) => completedIds.has(s.id)).length;
  const progressPercent =
    totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;
  const allCompleted = totalCount > 0 && completedCount === totalCount;

  const filteredSteps = useMemo(() => {
    if (filterMode === 'pending') {
      return steps.filter((s) => !completedIds.has(s.id));
    }
    if (filterMode === 'completed') {
      return steps.filter((s) => completedIds.has(s.id));
    }
    return steps;
  }, [steps, filterMode, completedIds]);

  const umrahDone = progress.umrah.length;
  const umrahTotal = MANASIK_GUIDE_MAP.umrah.length;
  const hajiDone = progress.haji.length;
  const hajiTotal = MANASIK_GUIDE_MAP.haji.length;

  const handleSelectGuide = (guide: ManasikGuideType) => {
    setActiveGuide(guide);
    setFilterMode('all');
    setConfirmingReset(false);
    const firstPending = MANASIK_GUIDE_MAP[guide].find(
      (s) => !(progress[guide] || []).includes(s.id)
    );
    setExpandedStepId(
      firstPending?.id ?? MANASIK_GUIDE_MAP[guide][0]?.id ?? null
    );
  };

  const handleTriggerCheckIn = (stepId: string) => {
    if (!effectiveCoords) return;
    const prox = proximityByStepId.get(stepId);
    if (!prox || !prox.isWithinRadius) return;
    onCheckInStep(activeGuide, {
      stepId,
      landmarkName: prox.landmark.name,
      checkedInAtLabel: currentClockLabel(),
      distanceMeters: prox.distanceMeters,
      latitude: effectiveCoords.latitude,
      longitude: effectiveCoords.longitude,
    });
  };

  return (
    <div className={compact ? 'space-y-3' : 'space-y-4'}>
      {/* Guide Mode Switcher (Umroh vs Haji) */}
      <div className="grid grid-cols-2 gap-1.5 p-1 rounded-xl bg-[#DCEAE1] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="button"
          onClick={() => handleSelectGuide('umrah')}
          className={`min-h-[44px] px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer flex items-center justify-center gap-2 whitespace-nowrap ${
            activeGuide === 'umrah'
              ? 'bg-[#047857] text-white shadow-xs'
              : 'text-[#2D4A3D] dark:text-[#9CC2AE] hover:text-[#0B1F16] dark:hover:text-white'
          }`}
        >
          <span>Umroh</span>
          <span
            className={`font-mono tabular-nums text-[11px] ${
              activeGuide === 'umrah'
                ? 'text-[#A7F3D0]'
                : 'text-[#4D6659] dark:text-[#6EE7B7]'
            }`}
          >
            {umrahDone}/{umrahTotal}
          </span>
        </button>

        <button
          type="button"
          onClick={() => handleSelectGuide('haji')}
          className={`min-h-[44px] px-3 py-2 rounded-lg text-xs font-semibold transition-colors cursor-pointer flex items-center justify-center gap-2 whitespace-nowrap ${
            activeGuide === 'haji'
              ? 'bg-[#047857] text-white shadow-xs'
              : 'text-[#2D4A3D] dark:text-[#9CC2AE] hover:text-[#0B1F16] dark:hover:text-white'
          }`}
        >
          <span>Haji</span>
          <span
            className={`font-mono tabular-nums text-[11px] ${
              activeGuide === 'haji'
                ? 'text-[#A7F3D0]'
                : 'text-[#4D6659] dark:text-[#6EE7B7]'
            }`}
          >
            {hajiDone}/{hajiTotal}
          </span>
        </button>
      </div>

      {/* GPS Location & Proximity Check-In Card */}
      <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-2.5">
        <div className="flex items-start justify-between gap-2">
          <div className="flex items-start gap-2.5 min-w-0">
            <div
              className={`w-9 h-9 rounded-xl flex items-center justify-center shrink-0 ${
                effectiveCoords
                  ? 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399]'
                  : 'bg-[#FFFBEB] dark:bg-[#451A03] text-[#B45309] dark:text-[#FCD34D]'
              }`}
            >
              <LocateFixed className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-[#0B1F16] dark:text-[#ECFDF5] truncate">
                Check-in Lokasi GPS Otomatis
              </p>
              <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate font-mono tabular-nums">
                {effectiveCoords
                  ? `${effectiveCoords.label} (${effectiveCoords.latitude.toFixed(4)}, ${effectiveCoords.longitude.toFixed(4)})`
                  : gpsStatus === 'requesting'
                    ? 'Mencari koordinat GPS perangkat...'
                    : 'GPS belum aktif · Aktifkan GPS atau pilih area uji'}
              </p>
            </div>
          </div>

          {!effectiveCoords && onRetryGps && (
            <button
              type="button"
              onClick={onRetryGps}
              className="min-h-[34px] px-2.5 py-1 rounded-lg bg-[#047857] hover:bg-[#065F46] text-white text-[11px] font-semibold shrink-0 cursor-pointer whitespace-nowrap"
            >
              Aktifkan GPS
            </button>
          )}
        </div>

        {/* Simulation Area Selector so users outside Makkah can also test Check-in Lokasi */}
        <div className="flex items-center gap-2">
          <label
            htmlFor={`sim-loc-${compact ? 'sheet' : 'page'}`}
            className="text-[11px] font-medium text-[#4D6659] dark:text-[#9CC2AE] shrink-0"
          >
            Sumber Lokasi:
          </label>
          <select
            id={`sim-loc-${compact ? 'sheet' : 'page'}`}
            value={simLocationId}
            onChange={(e) => setSimLocationId(e.target.value)}
            className="flex-1 min-w-0 min-h-[36px] px-2.5 py-1.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs font-medium text-[#0B1F16] dark:text-[#ECFDF5] focus:outline-none focus:border-[#047857]"
          >
            <option value="gps">
              {userLocation
                ? `GPS Asli (${userLocation.latitude.toFixed(3)}, ${userLocation.longitude.toFixed(3)})`
                : 'GPS Perangkat Asli'}
            </option>
            {MANASIK_SIMULATION_LOCATIONS.map((loc) => (
              <option key={loc.id} value={loc.id}>
                Simulasi: {loc.label}
              </option>
            ))}
          </select>
        </div>

        {/* Instant Banner if Pilgrim is Within a Landmark Radius */}
        {nearbyPendingSteps.length > 0 && (
          <div className="pt-2 border-t border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center justify-between gap-2">
            <div className="min-w-0 flex items-center gap-1.5 text-xs text-[#047857] dark:text-[#34D399] font-semibold">
              <MapPinCheck className="w-4 h-4 shrink-0" />
              <span className="truncate">
                Anda berada di area: {nearbyPendingSteps[0].title}
              </span>
            </div>
            <button
              type="button"
              onClick={() => handleTriggerCheckIn(nearbyPendingSteps[0].id)}
              className="min-h-[36px] px-3 py-1.5 rounded-lg bg-[#047857] hover:bg-[#065F46] text-white text-[11px] font-semibold shrink-0 cursor-pointer whitespace-nowrap"
            >
              Centang Otomatis
            </button>
          </div>
        )}
      </div>

      {/* Progress & Filter Summary */}
      <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h3 className="text-sm font-bold text-[#0B1F16] dark:text-[#ECFDF5]">
              {activeGuide === 'umrah'
                ? 'Checklist Rangkaian Umroh'
                : 'Checklist Rangkaian Haji'}
            </h3>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] mt-0.5 tabular-nums">
              {completedCount} dari {totalCount} langkah selesai · {progressPercent}%
              {' · '}
              {isCloudSynced ? 'Tersinkron ke Akun' : 'Tersimpan di Perangkat'}
            </p>
          </div>

          {completedCount > 0 && !confirmingReset && (
            <button
              type="button"
              onClick={() => setConfirmingReset(true)}
              className="min-h-[36px] px-2.5 py-1.5 rounded-lg border border-[#D8E6DD] dark:border-[#1B3B2E] text-[11px] font-semibold text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#B45309] dark:hover:text-[#FCD34D] hover:border-[#D97706] flex items-center gap-1 transition-colors cursor-pointer shrink-0 whitespace-nowrap"
            >
              <RotateCcw className="w-3.5 h-3.5 shrink-0" />
              <span>Reset</span>
            </button>
          )}
        </div>

        {/* Inline Reset Confirmation */}
        {confirmingReset && (
          <div className="flex items-center justify-between gap-2 pt-1 pb-1 border-y border-[#D8E6DD] dark:border-[#1B3B2E] text-xs">
            <span className="text-[#92400E] dark:text-[#FCD34D] font-medium">
              Ulangi seluruh tanda centang {activeGuide === 'umrah' ? 'Umroh' : 'Haji'}?
            </span>
            <div className="flex items-center gap-1.5 shrink-0">
              <button
                type="button"
                onClick={() => setConfirmingReset(false)}
                className="min-h-[36px] px-2.5 py-1 rounded-lg border border-[#D8E6DD] dark:border-[#1B3B2E] text-[11px] font-semibold text-[#4D6659] dark:text-[#9CC2AE] cursor-pointer"
              >
                Batal
              </button>
              <button
                type="button"
                onClick={() => {
                  onResetGuide(activeGuide);
                  setConfirmingReset(false);
                }}
                className="min-h-[36px] px-2.5 py-1 rounded-lg bg-[#B45309] hover:bg-[#92400E] text-white text-[11px] font-semibold cursor-pointer"
              >
                Ya, Reset
              </button>
            </div>
          </div>
        )}

        {/* Progress Bar */}
        <div
          role="progressbar"
          aria-valuenow={progressPercent}
          aria-valuemin={0}
          aria-valuemax={100}
          aria-label="Progres langkah ibadah"
          className="h-2.5 w-full rounded-full bg-[#E6EFE9] dark:bg-[#132E24] overflow-hidden"
        >
          <div
            className="h-full rounded-full bg-gradient-to-r from-[#047857] to-[#D97706] transition-all duration-200"
            style={{ width: `${progressPercent}%` }}
          />
        </div>

        {/* Filter Controls */}
        <div className="flex items-center justify-between gap-2 pt-0.5 flex-wrap">
          <div className="flex items-center gap-1.5">
            {(
              [
                { id: 'all', label: `Semua (${totalCount})` },
                { id: 'pending', label: `Belum (${totalCount - completedCount})` },
                { id: 'completed', label: `Selesai (${completedCount})` },
              ] as const
            ).map((tab) => {
              const active = filterMode === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setFilterMode(tab.id)}
                  className={`min-h-[36px] px-2.5 py-1 rounded-lg text-[11px] font-semibold transition-colors cursor-pointer whitespace-nowrap ${
                    active
                      ? 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] border border-[#A7F3D0] dark:border-[#059669]'
                      : 'text-[#4D6659] dark:text-[#9CC2AE] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24]'
                  }`}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>

          {onOpenAssistant && (
            <button
              type="button"
              onClick={() =>
                onOpenAssistant(
                  `Saya sedang berada di panduan ${activeGuide === 'umrah' ? 'Umroh' : 'Haji'} (${completedCount}/${totalCount} langkah selesai). Jelaskan ringkas apa yang perlu saya perhatikan pada tahap berikutnya.`
                )
              }
              className="min-h-[36px] px-2.5 py-1 rounded-lg bg-[#FFFBEB] dark:bg-[#451A03] border border-[#FDE68A] dark:border-[#92400E] text-[#B45309] dark:text-[#FCD34D] text-[11px] font-semibold flex items-center gap-1.5 cursor-pointer whitespace-nowrap"
            >
              <Sparkles className="w-3.5 h-3.5 shrink-0" />
              <span>Tanya Mutawwif AI</span>
            </button>
          )}
        </div>

        {/* Completion Message */}
        {allCompleted && (
          <div className="pt-2 border-t border-[#D8E6DD] dark:border-[#1B3B2E] flex items-start gap-2 text-xs text-[#065F46] dark:text-[#6EE7B7]">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5 text-[#047857] dark:text-[#34D399]" />
            <p className="leading-relaxed font-medium">
              Alhamdulillah, seluruh rangkaian {activeGuide === 'umrah' ? 'Umroh' : 'Haji'} telah selesai dicentang. Semoga menjadi ibadah yang maqbul dan mabrur.
            </p>
          </div>
        )}
      </div>

      {/* Checklist Items */}
      {filteredSteps.length === 0 ? (
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-6 text-center space-y-2">
          <BookOpen className="w-6 h-6 mx-auto text-[#047857] dark:text-[#34D399]" />
          <p className="text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5]">
            {filterMode === 'pending'
              ? 'Semua langkah pada panduan ini sudah ditandai selesai.'
              : 'Belum ada langkah yang ditandai selesai.'}
          </p>
          <button
            type="button"
            onClick={() => setFilterMode('all')}
            className="min-h-[40px] px-3.5 py-2 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold cursor-pointer"
          >
            Tampilkan Semua Langkah
          </button>
        </div>
      ) : (
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E] overflow-hidden">
          {filteredSteps.map((step) => {
            const isChecked = completedIds.has(step.id);
            const isExpanded = expandedStepId === step.id;
            const prox = proximityByStepId.get(step.id) ?? null;
            const isNearLandmark = Boolean(prox?.isWithinRadius);
            const checkInRecord = checkIns[step.id];

            return (
              <div
                key={step.id}
                className={`transition-colors ${
                  isChecked
                    ? 'bg-[#F4F8F5]/75 dark:bg-[#091812]/70'
                    : isNearLandmark
                      ? 'bg-[#ECFDF5]/60 dark:bg-[#064E3B]/20'
                      : ''
                }`}
              >
                <div className="flex items-stretch">
                  {/* Left Checkbox Hitbox (min 44x44) */}
                  <button
                    type="button"
                    role="checkbox"
                    aria-checked={isChecked}
                    aria-label={`Tandai ${step.title} sebagai ${isChecked ? 'belum selesai' : 'selesai'}`}
                    onClick={() => onToggleStep(activeGuide, step.id)}
                    className="min-w-[52px] min-h-[56px] pl-3.5 pr-2 py-3 flex items-start justify-center cursor-pointer shrink-0 group"
                  >
                    <span
                      className={`mt-0.5 w-6 h-6 rounded-lg border-2 flex items-center justify-center transition-colors ${
                        isChecked
                          ? 'bg-[#047857] border-[#047857] text-white'
                          : 'border-[#9CC2AE] dark:border-[#2C5B47] bg-white dark:bg-[#06130E] group-hover:border-[#047857]'
                      }`}
                    >
                      {isChecked && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </span>
                  </button>

                  {/* Middle & Right Expandable Guide Trigger */}
                  <button
                    type="button"
                    aria-expanded={isExpanded}
                    onClick={() =>
                      setExpandedStepId((prev) => (prev === step.id ? null : step.id))
                    }
                    className="flex-1 min-w-0 py-3 pr-3.5 text-left flex items-start justify-between gap-2 cursor-pointer"
                  >
                    <div className="min-w-0 flex-1 space-y-1">
                      <div className="flex items-center gap-1.5 text-[11px] text-[#4D6659] dark:text-[#9CC2AE] tabular-nums flex-wrap">
                        <span className="font-mono font-semibold text-[#B45309] dark:text-[#FCD34D]">
                          {String(step.order).padStart(2, '0')}
                        </span>
                        <span aria-hidden="true">·</span>
                        <span className="font-medium">
                          {CATEGORY_LABEL[step.category]}
                        </span>
                        {prox && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span
                              className={
                                isNearLandmark
                                  ? 'font-semibold text-[#047857] dark:text-[#34D399]'
                                  : ''
                              }
                            >
                              {isNearLandmark
                                ? `Di Area (${formatDistanceText(prox.distanceMeters)})`
                                : `${formatDistanceText(prox.distanceMeters)} ke landmark`}
                            </span>
                          </>
                        )}
                        {isChecked && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span className="font-semibold text-[#047857] dark:text-[#34D399]">
                              Selesai
                            </span>
                          </>
                        )}
                      </div>

                      <p
                        className={`text-sm font-bold leading-snug ${
                          isChecked
                            ? 'line-through text-[#4D6659] dark:text-[#6EE7B7]/75'
                            : 'text-[#0B1F16] dark:text-[#ECFDF5]'
                        }`}
                      >
                        {step.title}
                      </p>

                      <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5]/90 leading-relaxed">
                        {step.summary}
                      </p>
                    </div>

                    <span className="min-w-[28px] min-h-[28px] mt-1 flex items-center justify-center text-[#4D6659] dark:text-[#9CC2AE] shrink-0">
                      {isExpanded ? (
                        <ChevronUp className="w-4 h-4" />
                      ) : (
                        <ChevronDown className="w-4 h-4" />
                      )}
                    </span>
                  </button>
                </div>

                {/* Location Check-in Action Row (always reachable on each step) */}
                <div className="pl-[52px] pr-3.5 pb-3 flex items-center justify-between gap-2 flex-wrap">
                  {checkInRecord ? (
                    <div className="flex items-center gap-1.5 text-[11px] font-medium text-[#047857] dark:text-[#6EE7B7]">
                      <MapPinCheck className="w-3.5 h-3.5 shrink-0" />
                      <span>
                        Check-in di {checkInRecord.landmarkName} · {checkInRecord.checkedInAtLabel} ({formatDistanceText(checkInRecord.distanceMeters)})
                      </span>
                    </div>
                  ) : (
                    <button
                      type="button"
                      disabled={!isNearLandmark || isChecked}
                      onClick={() => handleTriggerCheckIn(step.id)}
                      className={`min-h-[38px] px-3 py-1.5 rounded-xl text-[11px] font-semibold flex items-center gap-1.5 transition-colors whitespace-nowrap ${
                        isNearLandmark && !isChecked
                          ? 'bg-[#047857] hover:bg-[#065F46] text-white cursor-pointer shadow-xs'
                          : 'bg-[#F4F8F5] dark:bg-[#091812] text-[#4D6659] dark:text-[#9CC2AE]/70 border border-[#D8E6DD] dark:border-[#1B3B2E] opacity-75 cursor-not-allowed'
                      }`}
                    >
                      <Navigation className="w-3.5 h-3.5 shrink-0" />
                      <span>
                        {isChecked
                          ? 'Langkah Sudah Selesai'
                          : isNearLandmark
                            ? 'Check-in Lokasi · Centang Otomatis'
                            : prox
                              ? `Check-in Lokasi (${formatDistanceText(prox.distanceMeters)} > ${formatDistanceText(prox.landmark.radiusMeters)})`
                              : 'Check-in Lokasi (Menunggu GPS)'}
                      </span>
                    </button>
                  )}
                </div>

                {/* Expanded Guidance Details */}
                {isExpanded && (
                  <div className="pl-[52px] pr-4 pb-4 space-y-2.5">
                    <div className="flex items-start gap-1.5 text-[11px] font-medium text-[#047857] dark:text-[#6EE7B7]">
                      <MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5 text-[#B45309] dark:text-[#FCD34D]" />
                      <span>
                        {step.locationHint} · Radius Check-in {formatDistanceText(step.landmark.radiusMeters)}
                      </span>
                    </div>

                    <ul className="space-y-1.5 text-xs text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
                      {step.details.map((line, idx) => (
                        <li key={idx} className="flex items-start gap-2">
                          <span className="font-mono text-[11px] font-semibold text-[#047857] dark:text-[#34D399] tabular-nums shrink-0 mt-0.5">
                            {idx + 1}.
                          </span>
                          <span>{line}</span>
                        </li>
                      ))}
                    </ul>

                    {step.arabicText && (
                      <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#06130E] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 space-y-1">
                        <p
                          dir="rtl"
                          lang="ar"
                          className="text-base font-semibold leading-loose text-right text-[#064E3B] dark:text-[#ECFDF5]"
                        >
                          {step.arabicText}
                        </p>
                        {step.transliteration && (
                          <p className="text-[11px] font-semibold italic text-[#B45309] dark:text-[#FCD34D]">
                            {step.transliteration}
                          </p>
                        )}
                        {step.translation && (
                          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] leading-relaxed">
                            “{step.translation}”
                          </p>
                        )}
                      </div>
                    )}

                    <div className="pt-1 flex items-center gap-2 flex-wrap">
                      <button
                        type="button"
                        onClick={() => onToggleStep(activeGuide, step.id)}
                        className={`min-h-[42px] px-4 py-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                          isChecked
                            ? 'bg-[#E6EFE9] dark:bg-[#132E24] text-[#2D4A3D] dark:text-[#D1FAE5]'
                            : 'bg-[#047857] hover:bg-[#065F46] text-white'
                        }`}
                      >
                        <Check className="w-3.5 h-3.5 shrink-0" />
                        <span>
                          {isChecked
                            ? 'Tandai Belum Selesai'
                            : 'Tandai Langkah Selesai'}
                        </span>
                      </button>

                      {onOpenAssistant && (
                        <button
                          type="button"
                          onClick={() =>
                            onOpenAssistant(
                              `Jelaskan panduan praktis dan hal yang perlu diperhatikan saat melaksanakan langkah "${step.title}" di ${step.landmark.name}.`
                            )
                          }
                          className="min-h-[42px] px-3.5 py-2 rounded-xl bg-[#FFFBEB] dark:bg-[#451A03] border border-[#FDE68A] dark:border-[#92400E] text-[#B45309] dark:text-[#FCD34D] text-xs font-semibold flex items-center gap-1.5 cursor-pointer"
                        >
                          <Sparkles className="w-3.5 h-3.5 shrink-0" />
                          <span>Tanya AI Langkah Ini</span>
                        </button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
