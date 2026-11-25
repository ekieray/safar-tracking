import { useMemo, useState } from 'react';
import {
  Check,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  ClipboardList,
  Clock,
  MapPin,
  Plus,
  Sparkles,
  Trash2,
  Users,
  X,
} from 'lucide-react';
import {
  createStarterRoomUmrahActivities,
  UMRAH_ACTIVITY_QUICK_TEMPLATES,
} from '../data/umrahActivityTemplates';
import {
  ConvoyMember,
  RoomActivityItem,
  RoomActivityMemberProgress,
} from '../types/convoy';

interface RoomActivityChecklistPanelProps {
  mode: 'draft' | 'room';
  activities: RoomActivityItem[];
  progressByMember?: Record<string, RoomActivityMemberProgress>;
  members?: ConvoyMember[];
  currentUid?: string;
  isRoomCreator?: boolean;
  roomId?: string;
  roomTitle?: string;
  isCloudSynced?: boolean;
  onToggleActivity?: (activityId: string) => void;
  onAddActivity?: (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => void;
  onRemoveActivity?: (activityId: string) => void;
  onMoveActivity?: (activityId: string, direction: 'up' | 'down') => void;
  onApplyStarterTemplates?: (items: RoomActivityItem[]) => void;
}

type ActivityFilter = 'all' | 'pending' | 'completed';

export function RoomActivityChecklistPanel({
  mode,
  activities,
  progressByMember = {},
  members = [],
  currentUid = '',
  isRoomCreator = false,
  roomId = '',
  roomTitle = 'Rombongan Umroh',
  isCloudSynced = false,
  onToggleActivity,
  onAddActivity,
  onRemoveActivity,
  onMoveActivity,
  onApplyStarterTemplates,
}: RoomActivityChecklistPanelProps) {
  const [filterMode, setFilterMode] = useState<ActivityFilter>('all');
  const [showAddForm, setShowAddForm] = useState(false);
  const [showTemplates, setShowTemplates] = useState(false);
  const [titleInput, setTitleInput] = useState('');
  const [timeInput, setTimeInput] = useState('');
  const [locationInput, setLocationInput] = useState('');
  const [expandedDetailId, setExpandedDetailId] = useState<string | null>(null);

  const sortedActivities = useMemo(
    () => [...activities].sort((a, b) => a.orderIndex - b.orderIndex),
    [activities]
  );

  const myProgress = currentUid ? progressByMember[currentUid] : undefined;
  const myCompletedSet = useMemo(
    () => new Set(myProgress?.completedActivityIds ?? []),
    [myProgress]
  );

  const totalCount = sortedActivities.length;
  const myCompletedCount = sortedActivities.filter((a) =>
    myCompletedSet.has(a.id)
  ).length;
  const progressPercent =
    totalCount > 0 ? Math.round((myCompletedCount / totalCount) * 100) : 0;

  const filteredActivities = useMemo(() => {
    if (mode === 'draft') return sortedActivities;
    if (filterMode === 'pending') {
      return sortedActivities.filter((a) => !myCompletedSet.has(a.id));
    }
    if (filterMode === 'completed') {
      return sortedActivities.filter((a) => myCompletedSet.has(a.id));
    }
    return sortedActivities;
  }, [mode, sortedActivities, filterMode, myCompletedSet]);

  const progressEntries = useMemo(
    () => Object.values(progressByMember),
    [progressByMember]
  );

  const participantCount = Math.max(members.length, progressEntries.length, 1);

  const handleSubmitAdd = (e: React.FormEvent) => {
    e.preventDefault();
    const cleanTitle = titleInput.trim();
    if (!cleanTitle || !onAddActivity) return;
    onAddActivity({
      title: cleanTitle.slice(0, 100),
      timeLabel: timeInput.trim().slice(0, 32),
      locationNote: locationInput.trim().slice(0, 120),
    });
    setTitleInput('');
    setTimeInput('');
    setLocationInput('');
    setShowAddForm(false);
  };

  const canManageActivities = mode === 'draft' || isRoomCreator;

  return (
    <div className="space-y-3">
      {/* Header / Progress Card (for active room) */}
      {mode === 'room' ? (
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
          <div className="flex items-start justify-between gap-2">
            <div className="min-w-0">
              <p className="text-[11px] font-medium text-[#4D6659] dark:text-[#9CC2AE] truncate">
                {roomTitle}
                {roomId ? ` · ${roomId}` : ''}
                {' · '}
                {isCloudSynced ? 'Sync Rombongan Aktif' : 'Mode Lokal'}
              </p>
              <h3 className="text-sm font-bold text-[#0B1F16] dark:text-[#ECFDF5] mt-0.5">
                Checklist Kegiatan Umroh Rombongan
              </h3>
              <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] mt-0.5 tabular-nums">
                {totalCount > 0
                  ? `${myCompletedCount} dari ${totalCount} kegiatan selesai · ${progressPercent}%`
                  : 'Belum ada daftar kegiatan yang dibuat pembimbing'}
              </p>
            </div>

            {canManageActivities && onAddActivity && (
              <button
                type="button"
                onClick={() => setShowAddForm((prev) => !prev)}
                className="min-h-[38px] px-3 py-1.5 rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center gap-1.5 cursor-pointer shrink-0 whitespace-nowrap"
              >
                {showAddForm ? (
                  <>
                    <X className="w-3.5 h-3.5 shrink-0" />
                    <span>Tutup</span>
                  </>
                ) : (
                  <>
                    <Plus className="w-3.5 h-3.5 shrink-0" />
                    <span>Tambah</span>
                  </>
                )}
              </button>
            )}
          </div>

          {totalCount > 0 && (
            <>
              <div
                role="progressbar"
                aria-valuenow={progressPercent}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-label="Progres checklist kegiatan rombongan"
                className="h-2.5 w-full rounded-full bg-[#E6EFE9] dark:bg-[#132E24] overflow-hidden"
              >
                <div
                  className="h-full rounded-full bg-gradient-to-r from-[#047857] to-[#D97706] transition-all duration-200"
                  style={{ width: `${progressPercent}%` }}
                />
              </div>

              <div className="flex items-center gap-1.5 flex-wrap">
                {(
                  [
                    { id: 'all', label: `Semua (${totalCount})` },
                    {
                      id: 'pending',
                      label: `Belum (${totalCount - myCompletedCount})`,
                    },
                    { id: 'completed', label: `Selesai (${myCompletedCount})` },
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
            </>
          )}

          {totalCount > 0 && myCompletedCount === totalCount && (
            <div className="pt-2 border-t border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center gap-2 text-xs text-[#047857] dark:text-[#6EE7B7] font-medium">
              <CheckCircle2 className="w-4 h-4 shrink-0" />
              <span>
                Alhamdulillah, Anda telah menyelesaikan seluruh checklist kegiatan rombongan.
              </span>
            </div>
          )}
        </div>
      ) : (
        /* Draft Header (TourLeaderView Step 4) */
        <div className="flex items-center justify-between gap-2">
          <div className="min-w-0">
            <p className="text-xs font-bold text-[#0B1F16] dark:text-[#ECFDF5]">
              Checklist Kegiatan Umroh ({totalCount})
            </p>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
              Kegiatan ini akan tampil dan bisa dicentang oleh seluruh jamaah rombongan.
            </p>
          </div>
          {onAddActivity && (
            <button
              type="button"
              onClick={() => setShowAddForm((prev) => !prev)}
              className="min-h-[38px] px-3 py-1.5 rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center gap-1 cursor-pointer shrink-0 whitespace-nowrap"
            >
              {showAddForm ? (
                <>
                  <X className="w-3.5 h-3.5" />
                  <span>Tutup</span>
                </>
              ) : (
                <>
                  <Plus className="w-3.5 h-3.5" />
                  <span>Kegiatan</span>
                </>
              )}
            </button>
          )}
        </div>
      )}

      {/* Room Creator Add Activity Form */}
      {canManageActivities && showAddForm && onAddActivity && (
        <form
          onSubmit={handleSubmitAdd}
          className="rounded-[18px] bg-white dark:bg-[#0C1E17] border-2 border-[#047857] dark:border-[#34D399] p-3.5 space-y-2.5"
        >
          <div className="flex items-center justify-between gap-2">
            <p className="text-xs font-bold text-[#047857] dark:text-[#6EE7B7]">
              Tambah Kegiatan Umroh Baru
            </p>
            <button
              type="button"
              onClick={() => setShowTemplates((prev) => !prev)}
              className="min-h-[30px] px-2.5 py-1 rounded-lg bg-[#FFFBEB] dark:bg-[#451A03] border border-[#FDE68A] dark:border-[#92400E] text-[#B45309] dark:text-[#FCD34D] text-[11px] font-semibold flex items-center gap-1 cursor-pointer"
            >
              <Sparkles className="w-3 h-3 shrink-0" />
              <span>{showTemplates ? 'Sembunyikan Template' : 'Pilih Template'}</span>
            </button>
          </div>

          {showTemplates && (
            <div className="grid grid-cols-1 gap-1.5 max-h-44 overflow-y-auto p-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E]">
              {UMRAH_ACTIVITY_QUICK_TEMPLATES.map((tpl) => {
                const alreadyExists = sortedActivities.some(
                  (a) => a.title.toLowerCase() === tpl.title.toLowerCase()
                );
                return (
                  <button
                    key={tpl.key}
                    type="button"
                    disabled={alreadyExists || sortedActivities.length >= 40}
                    onClick={() =>
                      onAddActivity({
                        title: tpl.title,
                        timeLabel: tpl.timeLabel,
                        locationNote: tpl.locationNote,
                      })
                    }
                    className="w-full px-2.5 py-2 rounded-lg bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] disabled:opacity-50 text-left flex items-center justify-between gap-2 cursor-pointer disabled:cursor-not-allowed"
                  >
                    <div className="min-w-0">
                      <p className="text-xs font-semibold truncate text-[#0B1F16] dark:text-[#ECFDF5]">
                        {tpl.title}
                      </p>
                      <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                        {tpl.timeLabel} · {tpl.locationNote}
                      </p>
                    </div>
                    <span className="text-[10px] font-bold text-[#047857] dark:text-[#34D399] shrink-0">
                      {alreadyExists ? 'Ada' : '+ Tambah'}
                    </span>
                  </button>
                );
              })}
            </div>
          )}

          <div className="space-y-2">
            <div>
              <label
                htmlFor={`act-title-${mode}`}
                className="block text-[11px] font-semibold text-[#1E3A2F] dark:text-[#D1FAE5] mb-1"
              >
                Nama Kegiatan Umroh
              </label>
              <input
                id={`act-title-${mode}`}
                type="text"
                required
                maxLength={100}
                value={titleInput}
                onChange={(e) => setTitleInput(e.target.value)}
                placeholder="Contoh: Sholat Sunnah di Hijir Ismail / Kumpul Pintu 79"
                className="w-full min-h-[42px] px-3 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-xs outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label
                  htmlFor={`act-time-${mode}`}
                  className="block text-[11px] font-semibold text-[#1E3A2F] dark:text-[#D1FAE5] mb-1"
                >
                  Waktu / Sesi (Opsional)
                </label>
                <input
                  id={`act-time-${mode}`}
                  type="text"
                  maxLength={32}
                  value={timeInput}
                  onChange={(e) => setTimeInput(e.target.value)}
                  placeholder="07:00 WAS"
                  className="w-full min-h-[40px] px-3 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-xs outline-none"
                />
              </div>

              <div>
                <label
                  htmlFor={`act-loc-${mode}`}
                  className="block text-[11px] font-semibold text-[#1E3A2F] dark:text-[#D1FAE5] mb-1"
                >
                  Lokasi / Catatan
                </label>
                <input
                  id={`act-loc-${mode}`}
                  type="text"
                  maxLength={120}
                  value={locationInput}
                  onChange={(e) => setLocationInput(e.target.value)}
                  placeholder="Lobi / Pelataran"
                  className="w-full min-h-[40px] px-3 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857] text-xs outline-none"
                />
              </div>
            </div>
          </div>

          <div className="flex items-center justify-end gap-2 pt-1">
            <button
              type="button"
              onClick={() => setShowAddForm(false)}
              className="min-h-[38px] px-3 rounded-xl border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs font-semibold text-[#4D6659] dark:text-[#9CC2AE] cursor-pointer"
            >
              Batal
            </button>
            <button
              type="submit"
              disabled={!titleInput.trim() || sortedActivities.length >= 40}
              className="min-h-[38px] px-4 rounded-xl bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white text-xs font-semibold cursor-pointer"
            >
              Simpan Kegiatan
            </button>
          </div>
        </form>
      )}

      {/* Activity Checklist Rows */}
      {filteredActivities.length === 0 ? (
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-5 text-center space-y-2.5">
          <ClipboardList className="w-6 h-6 mx-auto text-[#047857] dark:text-[#34D399]" />
          <p className="text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5]">
            {totalCount === 0
              ? mode === 'room' && !isRoomCreator
                ? 'Pembuat room belum menambahkan checklist kegiatan untuk grup ini.'
                : 'Belum ada kegiatan Umroh pada checklist grup.'
              : filterMode === 'pending'
                ? 'Semua kegiatan grup sudah Anda centang selesai.'
                : 'Belum ada kegiatan yang Anda centang selesai.'}
          </p>
          {totalCount > 0 && filterMode !== 'all' && (
            <button
              type="button"
              onClick={() => setFilterMode('all')}
              className="min-h-[38px] px-3.5 py-1.5 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold cursor-pointer"
            >
              Tampilkan Semua
            </button>
          )}
          {totalCount === 0 && canManageActivities && onApplyStarterTemplates && (
            <button
              type="button"
              onClick={() =>
                onApplyStarterTemplates(
                  createStarterRoomUmrahActivities(currentUid || 'leader')
                )
              }
              className="min-h-[40px] px-4 py-2 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold cursor-pointer"
            >
              Muat 4 Kegiatan Umroh Standar
            </button>
          )}
        </div>
      ) : (
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E] overflow-hidden">
          {filteredActivities.map((act, idx) => {
            const isCheckedByMe = myCompletedSet.has(act.id);
            const checkedAtClock = myProgress?.completedTimes?.[act.id];
            const completedMembersList = progressEntries.filter((p) =>
              p.completedActivityIds.includes(act.id)
            );
            const showDetails = expandedDetailId === act.id;

            return (
              <div
                key={act.id}
                className={`p-3 transition-colors ${
                  mode === 'room' && isCheckedByMe
                    ? 'bg-[#F4F8F5]/75 dark:bg-[#091812]/70'
                    : ''
                }`}
              >
                <div className="flex items-start gap-2.5">
                  {/* Member Checkbox (Room Mode) or Order Number (Draft Mode) */}
                  {mode === 'room' && onToggleActivity ? (
                    <button
                      type="button"
                      role="checkbox"
                      aria-checked={isCheckedByMe}
                      aria-label={`Centang kegiatan ${act.title}`}
                      onClick={() => onToggleActivity(act.id)}
                      className="min-w-[44px] min-h-[44px] -ml-1 -mt-1 flex items-center justify-center cursor-pointer shrink-0 group"
                    >
                      <span
                        className={`w-6 h-6 rounded-lg border-2 flex items-center justify-center transition-colors ${
                          isCheckedByMe
                            ? 'bg-[#047857] border-[#047857] text-white'
                            : 'border-[#9CC2AE] dark:border-[#2C5B47] bg-white dark:bg-[#06130E] group-hover:border-[#047857]'
                        }`}
                      >
                        {isCheckedByMe && (
                          <Check className="w-3.5 h-3.5 stroke-[3]" />
                        )}
                      </span>
                    </button>
                  ) : (
                    <span className="mt-0.5 w-6 h-6 rounded-full bg-[#047857] text-white text-[11px] font-bold flex items-center justify-center shrink-0 tabular-nums">
                      {idx + 1}
                    </span>
                  )}

                  {/* Activity Info */}
                  <div className="min-w-0 flex-1 space-y-1">
                    <div className="flex items-center gap-1.5 text-[11px] text-[#4D6659] dark:text-[#9CC2AE] flex-wrap tabular-nums">
                      <span className="font-mono font-semibold text-[#B45309] dark:text-[#FCD34D]">
                        {String(idx + 1).padStart(2, '0')}
                      </span>
                      {act.timeLabel && (
                        <>
                          <span aria-hidden="true">·</span>
                          <span className="inline-flex items-center gap-1">
                            <Clock className="w-3 h-3 shrink-0" />
                            <span>{act.timeLabel}</span>
                          </span>
                        </>
                      )}
                      {mode === 'room' && isCheckedByMe && (
                        <>
                          <span aria-hidden="true">·</span>
                          <span className="font-semibold text-[#047857] dark:text-[#34D399]">
                            Selesai{checkedAtClock ? ` (${checkedAtClock})` : ''}
                          </span>
                        </>
                      )}
                    </div>

                    <p
                      className={`text-sm font-bold leading-snug ${
                        mode === 'room' && isCheckedByMe
                          ? 'line-through text-[#4D6659] dark:text-[#6EE7B7]/75'
                          : 'text-[#0B1F16] dark:text-[#ECFDF5]'
                      }`}
                    >
                      {act.title}
                    </p>

                    {act.locationNote && (
                      <p className="text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5]/90 flex items-center gap-1">
                        <MapPin className="w-3 h-3 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
                        <span className="truncate">{act.locationNote}</span>
                      </p>
                    )}

                    {/* Room completion tally across members */}
                    {mode === 'room' && (
                      <div className="pt-0.5 flex items-center justify-between gap-2">
                        <button
                          type="button"
                          onClick={() =>
                            setExpandedDetailId((prev) =>
                              prev === act.id ? null : act.id
                            )
                          }
                          className="inline-flex items-center gap-1 text-[11px] font-medium text-[#047857] dark:text-[#6EE7B7] hover:underline cursor-pointer tabular-nums"
                        >
                          <Users className="w-3 h-3 shrink-0" />
                          <span>
                            {completedMembersList.length}/{participantCount} jamaah selesai
                          </span>
                          {completedMembersList.length > 0 && (
                            <span className="text-[10px]">
                              {showDetails ? '▲' : '▼'}
                            </span>
                          )}
                        </button>
                      </div>
                    )}

                    {mode === 'room' && showDetails && (
                      <div className="pt-1.5 mt-1 border-t border-[#D8E6DD] dark:border-[#1B3B2E] text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5] space-y-1">
                        {completedMembersList.length === 0 ? (
                          <p className="text-[#4D6659] dark:text-[#9CC2AE]">
                            Belum ada jamaah yang mencentang kegiatan ini.
                          </p>
                        ) : (
                          completedMembersList.map((entry) => (
                            <div
                              key={entry.uid}
                              className="flex items-center justify-between gap-2"
                            >
                              <span className="truncate font-medium">
                                ✓ {entry.memberName}
                                {entry.uid === currentUid ? ' (Anda)' : ''}
                              </span>
                              {entry.completedTimes[act.id] && (
                                <span className="font-mono text-[10px] text-[#4D6659] dark:text-[#9CC2AE] shrink-0">
                                  {entry.completedTimes[act.id]}
                                </span>
                              )}
                            </div>
                          ))
                        )}
                      </div>
                    )}
                  </div>

                  {/* Creator Management Actions */}
                  {canManageActivities && (
                    <div className="flex items-center gap-0.5 shrink-0">
                      {onMoveActivity && (
                        <>
                          <button
                            type="button"
                            disabled={idx === 0}
                            onClick={() => onMoveActivity(act.id, 'up')}
                            aria-label={`Naikkan urutan ${act.title}`}
                            className="min-w-[32px] min-h-[34px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/50 disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                          >
                            <ChevronUp className="w-4 h-4" />
                          </button>
                          <button
                            type="button"
                            disabled={idx === filteredActivities.length - 1}
                            onClick={() => onMoveActivity(act.id, 'down')}
                            aria-label={`Turunkan urutan ${act.title}`}
                            className="min-w-[32px] min-h-[34px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/50 disabled:opacity-30 cursor-pointer disabled:cursor-not-allowed"
                          >
                            <ChevronDown className="w-4 h-4" />
                          </button>
                        </>
                      )}
                      {onRemoveActivity && (
                        <button
                          type="button"
                          onClick={() => onRemoveActivity(act.id)}
                          aria-label={`Hapus kegiatan ${act.title}`}
                          className="min-w-[32px] min-h-[34px] rounded-lg flex items-center justify-center text-[#4D6659] hover:text-[#DC2626] hover:bg-[#FEE2E2] dark:hover:bg-[#450A0A]/40 cursor-pointer"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
