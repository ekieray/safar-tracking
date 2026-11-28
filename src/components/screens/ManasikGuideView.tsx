import { useEffect, useState } from 'react';
import { ArrowLeft, BookOpen, BookOpenCheck, ClipboardCheck } from 'lucide-react';
import { GpsPermissionStatus } from '../../hooks/useDeviceGps';
import {
  ManasikCheckInRecord,
  ManasikGuideType,
  ManasikProgressState,
  RoomActivityItem,
  RoomActivityMemberProgress,
  TourRoom,
} from '../../types/convoy';
import { ManasikChecklistPanel } from '../ManasikChecklistPanel';
import { PrayerGuidePanel } from '../PrayerGuidePanel';
import { RoomActivityChecklistPanel } from '../RoomActivityChecklistPanel';
import {
  TouringCanvasBackdrop,
  TouringHeroBanner,
} from '../TouringAtmosphereBackdrop';

export type ManasikViewTab = 'manasik' | 'doa' | 'activities';

interface ManasikGuideViewProps {
  progress: ManasikProgressState;
  isCloudSynced: boolean;
  initialTab?: ManasikViewTab;
  activeRoom?: TourRoom | null;
  roomActivities?: RoomActivityItem[];
  roomActivityProgress?: Record<string, RoomActivityMemberProgress>;
  currentMemberUid?: string;
  isRoomCreator?: boolean;
  onToggleRoomActivity?: (activityId: string) => void;
  onAddRoomActivity?: (input: {
    title: string;
    timeLabel: string;
    locationNote: string;
  }) => void;
  onRemoveRoomActivity?: (activityId: string) => void;
  onMoveRoomActivity?: (activityId: string, direction: 'up' | 'down') => void;
  onApplyRoomStarterTemplates?: (items: RoomActivityItem[]) => void;
  userLocation?: {
    latitude: number;
    longitude: number;
    accuracyMeters?: number;
    sourceLabel?: string;
  } | null;
  gpsStatus?: GpsPermissionStatus;
  onRetryGps?: () => void;
  onToggleStep: (guideType: ManasikGuideType, stepId: string) => void;
  onCheckInStep: (
    guideType: ManasikGuideType,
    checkIn: ManasikCheckInRecord
  ) => void;
  onResetGuide: (guideType: ManasikGuideType) => void;
  onOpenAssistant?: (initialPrompt?: string) => void;
  onBack: () => void;
}

function resolveManasikTab(
  requested: ManasikViewTab,
  hasActiveRoom: boolean
): ManasikViewTab {
  if (requested === 'activities' && !hasActiveRoom) {
    return 'manasik';
  }
  return requested;
}

export function ManasikGuideView({
  progress,
  isCloudSynced,
  initialTab = 'manasik',
  activeRoom = null,
  roomActivities = [],
  roomActivityProgress = {},
  currentMemberUid = '',
  isRoomCreator = false,
  onToggleRoomActivity,
  onAddRoomActivity,
  onRemoveRoomActivity,
  onMoveRoomActivity,
  onApplyRoomStarterTemplates,
  userLocation = null,
  gpsStatus = 'idle',
  onRetryGps,
  onToggleStep,
  onCheckInStep,
  onResetGuide,
  onOpenAssistant,
  onBack,
}: ManasikGuideViewProps) {
  const [activeTab, setActiveTab] = useState<ManasikViewTab>(() =>
    resolveManasikTab(initialTab, Boolean(activeRoom))
  );

  useEffect(() => {
    setActiveTab(resolveManasikTab(initialTab, Boolean(activeRoom)));
  }, [initialTab, activeRoom?.roomId]);

  const selfId =
    currentMemberUid ||
    activeRoom?.members.find((m) => m.isCurrentUser)?.id ||
    '';
  const myCompletedSet = new Set(
    selfId ? (roomActivityProgress[selfId]?.completedActivityIds ?? []) : []
  );
  const doneCount = roomActivities.filter((a) => myCompletedSet.has(a.id)).length;

  const headerTitle =
    activeTab === 'activities' && activeRoom
      ? 'Checklist Kegiatan Umroh'
      : activeTab === 'doa'
      ? 'Kumpulan Doa Travel & Harian'
      : 'Panduan & Check-in Manasik';

  const headerSubtitle =
    activeTab === 'activities' && activeRoom
      ? `${activeRoom.roomTitle} · ${doneCount}/${roomActivities.length} selesai`
      : activeTab === 'doa'
      ? 'Standar Tuntunan Manasik Kemenag RI & MUI'
      : 'Tandai & check-in lokasi GPS setiap tahapan Umroh & Haji';

  return (
    <div className="relative flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Top App Bar */}
      <div className="sticky top-0 z-20 flex items-center gap-1.5 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="button"
          onClick={onBack}
          aria-label="Kembali"
          className="min-w-[44px] min-h-[44px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="min-w-0">
          <h2 className="text-sm font-bold truncate">{headerTitle}</h2>
          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
            {headerSubtitle}
          </p>
        </div>
      </div>

      {/* Body */}
      <div className="relative flex-1 px-4 py-4 space-y-4">
        <TouringCanvasBackdrop />

        <div className="relative z-10 space-y-4">
          <div
            className={`grid gap-1.5 p-1 rounded-[16px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] ${
              activeRoom ? 'grid-cols-3' : 'grid-cols-2'
            }`}
          >
            <button
              type="button"
              onClick={() => setActiveTab('manasik')}
              className={`min-h-[40px] px-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                activeTab === 'manasik'
                  ? 'bg-[#047857] text-white'
                  : 'text-[#4D6659] dark:text-[#9CC2AE] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24]'
              }`}
            >
              <BookOpenCheck className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">Manasik</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('doa')}
              className={`min-h-[40px] px-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer ${
                activeTab === 'doa'
                  ? 'bg-[#047857] text-white'
                  : 'text-[#4D6659] dark:text-[#9CC2AE] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24]'
              }`}
            >
              <BookOpen className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">
                {activeRoom ? 'Doa Kemenag' : 'Doa Kemenag / MUI'}
              </span>
            </button>
            {activeRoom && (
              <button
                type="button"
                onClick={() => setActiveTab('activities')}
                className={`min-h-[40px] px-2 rounded-xl text-xs font-semibold flex items-center justify-center gap-1.5 transition-colors cursor-pointer tabular-nums ${
                  activeTab === 'activities'
                    ? 'bg-[#047857] text-white'
                    : 'text-[#4D6659] dark:text-[#9CC2AE] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24]'
                }`}
              >
                <ClipboardCheck className="w-3.5 h-3.5 shrink-0" />
                <span className="truncate">
                  Kegiatan ({doneCount}/{roomActivities.length})
                </span>
              </button>
            )}
          </div>

          {activeTab === 'activities' && activeRoom ? (
            <RoomActivityChecklistPanel
              mode="room"
              activities={roomActivities}
              progressByMember={roomActivityProgress}
              members={activeRoom.members}
              currentUid={selfId}
              isRoomCreator={isRoomCreator}
              roomId={activeRoom.roomId}
              roomTitle={activeRoom.roomTitle}
              isCloudSynced={activeRoom.isSyncedRoom}
              onToggleActivity={onToggleRoomActivity}
              onAddActivity={onAddRoomActivity}
              onRemoveActivity={onRemoveRoomActivity}
              onMoveActivity={onMoveRoomActivity}
              onApplyStarterTemplates={onApplyRoomStarterTemplates}
            />
          ) : activeTab === 'doa' ? (
            <PrayerGuidePanel onOpenAssistant={onOpenAssistant} />
          ) : (
            <>
              <TouringHeroBanner
                title="Panduan Manasik & Check-in Lokasi"
                subtitle="Ketika koordinat GPS Anda mendekati area landmark (seperti Ka'bah, Mas'a Sa'i, Miqat, atau Arafah), tombol Check-in Lokasi aktif otomatis."
              />

              <ManasikChecklistPanel
                progress={progress}
                isCloudSynced={isCloudSynced}
                userLocation={userLocation}
                gpsStatus={gpsStatus}
                onRetryGps={onRetryGps}
                onToggleStep={onToggleStep}
                onCheckInStep={onCheckInStep}
                onResetGuide={onResetGuide}
                onOpenAssistant={onOpenAssistant}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
