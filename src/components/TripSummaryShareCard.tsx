import { useEffect, useRef, useState } from 'react';
import { MapPin, Moon } from 'lucide-react';
import { AvatarPreset, TripCardTheme, TripHistoryRecord } from '../types/convoy';
import { projectTripMapViewport } from '../utils/geo';
import { ConvoyAvatar } from './ConvoyAvatar';

interface TripSummaryShareCardProps {
  trip: TripHistoryRecord;
  theme: TripCardTheme;
  customPhotoUrl?: string;
  defaultScenicPhotoUrl: string;
  currentUserName?: string;
  currentUserAvatarPreset?: AvatarPreset;
  currentUserAvatarPhotoUrl?: string;
}

export function TripSummaryShareCard({
  trip,
  theme,
  customPhotoUrl,
  defaultScenicPhotoUrl,
  currentUserName,
  currentUserAvatarPreset,
  currentUserAvatarPhotoUrl,
}: TripSummaryShareCardProps) {
  const [imgError, setImgError] = useState(false);
  const mapContainerRef = useRef<HTMLDivElement | null>(null);
  const [mapSize, setMapSize] = useState({ width: 300, height: 185 });

  useEffect(() => {
    const el = mapContainerRef.current;
    if (!el || typeof ResizeObserver === 'undefined') return;
    const updateSize = () => {
      const rect = el.getBoundingClientRect();
      if (rect.width > 40 && rect.height > 40) {
        setMapSize({
          width: Math.round(rect.width),
          height: Math.round(rect.height),
        });
      }
    };
    updateSize();
    const ro = new ResizeObserver(updateSize);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const isLight = theme === 'MINIMAL_LIGHT';
  const primaryText = isLight ? 'text-[#0B1F16]' : 'text-white';
  const secondaryText = isLight ? 'text-[#4D6659]' : 'text-[#D1FAE5]';
  const borderClass =
    theme === 'MINIMAL_LIGHT'
      ? 'border-[#D8E6DD]'
      : theme === 'SATELLITE'
        ? 'border-[#059669]'
        : 'border-[#1B3B2E]';

  const leader =
    trip.participants.find((p) => p.role === 'TOUR_LEADER') ??
    trip.participants[0];
  const trimmedLoginName = currentUserName?.trim();
  const matchedSelf = trimmedLoginName
    ? trip.participants.find(
        (p) => p.fullName.trim().toLowerCase() === trimmedLoginName.toLowerCase()
      )
    : undefined;
  const headerDisplayName = trimmedLoginName || leader?.fullName || 'Jamaah Safar';
  const headerAvatarPreset =
    currentUserAvatarPreset ??
    matchedSelf?.avatarPreset ??
    leader?.avatarPreset ??
    'HELMET_TOURING';
  const headerAvatarPhotoUrl =
    currentUserAvatarPhotoUrl ??
    matchedSelf?.avatarPhotoUrl ??
    (trimmedLoginName ? undefined : leader?.avatarPhotoUrl);
  const headerRoleLabel =
    trip.userRole === 'TOUR_LEADER'
      ? 'Mutawwif'
      : matchedSelf?.isSweeper
        ? 'Pendamping'
        : 'Jamaah Umroh';
  const participantNames = trip.participants
    .map((p) => p.fullName.trim())
    .filter(Boolean);
  const participantDisplayList =
    participantNames.length > 0 ? participantNames.join(', ') : headerDisplayName;

  const projection = projectTripMapViewport(
    trip.waypoints,
    trip.visitedCheckpoints,
    mapSize.width,
    mapSize.height,
    0.13
  );
  const polylinePts = projection.waypoints
    .map((pt) => `${pt.x.toFixed(1)},${pt.y.toFixed(1)}`)
    .join(' ');
  const startPt = projection.waypoints[0];
  const endPt = projection.waypoints[projection.waypoints.length - 1];
  const activePhotoSrc = customPhotoUrl || defaultScenicPhotoUrl;

  return (
    <div
      className={`relative w-full aspect-[4/5] rounded-[22px] border overflow-hidden shadow-md select-none ${borderClass}`}
    >
      {/* 1. Background Layer per Theme */}
      {theme === 'DARK_CARBON' && (
        <div
          className="absolute inset-0"
          style={{
            background:
              'linear-gradient(145deg, #041A12 0%, #07271C 55%, #0C3527 100%)',
          }}
        >
          <svg className="w-full h-full opacity-100" viewBox="0 0 320 400">
            <defs>
              <pattern
                id="darkHaramGrid"
                width="24"
                height="24"
                patternUnits="userSpaceOnUse"
              >
                <path
                  d="M 24 0 L 0 0 0 24"
                  fill="none"
                  stroke="rgba(52,211,153,0.08)"
                  strokeWidth="1"
                />
              </pattern>
            </defs>
            <rect width="320" height="400" fill="url(#darkHaramGrid)" />
            <circle
              cx="275"
              cy="62"
              r="110"
              fill="rgba(217,119,6,0.22)"
            />
          </svg>
        </div>
      )}

      {theme === 'SATELLITE' && (
        <div
          className="absolute inset-0 overflow-hidden"
          style={{
            background:
              'linear-gradient(155deg, #05231B 0%, #0A3327 52%, #061D17 100%)',
          }}
        >
          <svg className="w-full h-full" viewBox="0 0 320 400">
            <defs>
              <pattern
                id="topoHaramGrid"
                width="28"
                height="28"
                patternUnits="userSpaceOnUse"
              >
                <path
                  d="M 28 0 L 0 0 0 28"
                  fill="none"
                  stroke="rgba(52,211,153,0.09)"
                  strokeWidth="1"
                />
              </pattern>
            </defs>
            <rect width="320" height="400" fill="url(#topoHaramGrid)" />
            {[1, 2, 3, 4, 5, 6].map((ring) => (
              <circle
                key={ring}
                cx="230"
                cy="168"
                r={ring * 52}
                fill="none"
                stroke="rgba(251,191,36,0.13)"
                strokeWidth="1.2"
              />
            ))}
          </svg>
          <div className="absolute inset-0 bg-gradient-to-b from-[#041A12]/75 via-[#041A12]/25 to-[#041A12]/85" />
        </div>
      )}

      {theme === 'MINIMAL_LIGHT' && (
        <div className="absolute inset-0 bg-gradient-to-b from-white to-[#F4F8F5]">
          <svg className="w-full h-full" viewBox="0 0 320 400">
            <defs>
              <pattern
                id="minimalKiswahGrid"
                width="26"
                height="26"
                patternUnits="userSpaceOnUse"
              >
                <path
                  d="M 26 0 L 0 0 0 26"
                  fill="none"
                  stroke="#DCE9E0"
                  strokeWidth="1"
                />
              </pattern>
            </defs>
            <rect width="320" height="400" fill="url(#minimalKiswahGrid)" />
          </svg>
        </div>
      )}

      {theme === 'CUSTOM_PHOTO' && (
        <>
          {!imgError ? (
            <img
              key={activePhotoSrc}
              src={activePhotoSrc}
              alt="Foto latar belakang perjalanan umroh Makkah"
              referrerPolicy="no-referrer"
              onLoad={() => setImgError(false)}
              onError={() => setImgError(true)}
              className="absolute inset-0 w-full h-full object-cover"
            />
          ) : (
            <div
              className="absolute inset-0"
              style={{
                background:
                  'linear-gradient(180deg, #064E3B 0%, #B45309 48%, #041A12 100%)',
              }}
            />
          )}
          <div className="absolute inset-0 bg-gradient-to-b from-[#031810]/65 via-[#031810]/35 to-[#031810]/90" />
        </>
      )}

      {/* 2. Foreground Content Layer */}
      <div className="relative z-10 h-full p-4 flex flex-col justify-between">
        {/* Brand Header & Mutawwif/Jamaah Avatar */}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 min-w-0">
            <div className="w-7 h-7 rounded-[9px] bg-[#047857] text-[#FDE68A] flex items-center justify-center shrink-0">
              <Moon className="w-4 h-4" />
            </div>
            <div className="min-w-0">
              <p className={`text-xs font-extrabold leading-tight ${primaryText}`}>
                Safar Tracking
              </p>
              <p className={`text-[10px] font-mono leading-tight ${secondaryText}`}>
                {trip.roomId}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 min-w-0">
            <div className="text-right min-w-0">
              <p
                className={`text-[11px] font-semibold truncate max-w-[125px] ${primaryText}`}
              >
                {headerDisplayName}
              </p>
              <p
                className={`text-[10px] font-medium ${
                  isLight ? 'text-[#047857]' : 'text-[#FDE68A]'
                }`}
              >
                {headerRoleLabel}
              </p>
            </div>
            <ConvoyAvatar
              preset={headerAvatarPreset}
              photoUrl={headerAvatarPhotoUrl}
              name={headerDisplayName}
              sizeClass="w-8 h-8"
              ringClass="ring-2 ring-[#D97706]"
            />
          </div>
        </div>

        {/* Title & Date Metadata */}
        <div className="mt-2 space-y-0.5">
          <h3
            className={`text-base font-bold leading-snug line-clamp-2 ${primaryText}`}
          >
            {trip.tourTitle}
          </h3>
          <p className={`text-[11px] truncate ${secondaryText}`}>
            {trip.dateLabel} · {trip.destinationName}
          </p>
        </div>

        {/* Center Real-Shape Polyline Path */}
        <div
          ref={mapContainerRef}
          className={`relative flex-1 my-1.5 min-h-[115px] overflow-hidden ${
            theme === 'SATELLITE'
              ? 'rounded-2xl border border-[#A7F3D0]/20 bg-black/25'
              : ''
          }`}
        >
          <svg
            className="relative z-[1] w-full h-full"
            viewBox={`0 0 ${mapSize.width} ${mapSize.height}`}
          >
            {projection.waypoints.length >= 2 && (
              <>
                <polyline
                  points={polylinePts}
                  fill="none"
                  stroke={
                    isLight
                      ? 'rgba(16,185,129,0.4)'
                      : 'rgba(217,119,6,0.42)'
                  }
                  strokeWidth="12"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
                <polyline
                  points={polylinePts}
                  fill="none"
                  stroke={isLight ? '#047857' : '#34D399'}
                  strokeWidth="4.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </>
            )}

            {startPt && (
              <g>
                <circle cx={startPt.x} cy={startPt.y} r="7" fill="#FFFFFF" />
                <circle cx={startPt.x} cy={startPt.y} r="4.5" fill="#D97706" />
              </g>
            )}

            {endPt && (
              <g>
                <circle cx={endPt.x} cy={endPt.y} r="7" fill="#FFFFFF" />
                <circle cx={endPt.x} cy={endPt.y} r="4.5" fill="#047857" />
              </g>
            )}
          </svg>

          {trip.visitedCheckpoints.map((cp, idx) => {
            const projectedCp = projection.checkpoints[idx];
            if (!projectedCp) return null;
            return (
              <div
                key={cp.id}
                style={{
                  left: `${(projectedCp.x / mapSize.width) * 100}%`,
                  top: `${(projectedCp.y / mapSize.height) * 100}%`,
                }}
                title={cp.name}
                className="absolute z-[2] -translate-x-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-[#0F766E] border-2 border-white flex items-center justify-center text-white"
              >
                <MapPin className="w-2.5 h-2.5" />
              </div>
            );
          })}
        </div>

        {/* Bottom Stats Grid & Participants */}
        <div className="space-y-2.5">
          <div className="grid grid-cols-4 gap-1.5">
            <div className="min-w-0">
              <p className={`text-[9px] ${secondaryText}`}>Jarak</p>
              <p
                className={`text-xs font-bold font-mono tabular-nums truncate ${primaryText}`}
              >
                {trip.distanceKm.toFixed(1)} km
              </p>
            </div>
            <div className="min-w-0">
              <p className={`text-[9px] ${secondaryText}`}>Durasi</p>
              <p
                className={`text-xs font-bold font-mono tabular-nums truncate ${primaryText}`}
              >
                {trip.durationLabel}
              </p>
            </div>
            <div className="min-w-0">
              <p className={`text-[9px] ${secondaryText}`}>Rata-rata</p>
              <p
                className={`text-xs font-bold font-mono tabular-nums truncate ${primaryText}`}
              >
                {trip.avgSpeedKmh} km/j
              </p>
            </div>
            <div className="min-w-0">
              <p className={`text-[9px] ${secondaryText}`}>Pos Kumpul</p>
              <p
                className={`text-xs font-bold font-mono tabular-nums truncate ${primaryText}`}
              >
                {trip.visitedCheckpoints.length} titik
              </p>
            </div>
          </div>

          <div
            className={`pt-2 border-t text-[10px] leading-snug line-clamp-3 ${
              isLight ? 'border-[#D8E6DD]' : 'border-white/20'
            } ${secondaryText}`}
          >
            <span className="font-semibold">
              {Math.max(1, participantNames.length)} Jamaah Ikut:
            </span>{' '}
            {participantDisplayList}
          </div>
        </div>
      </div>
    </div>
  );
}
