import { ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import {
  AdvancedMarker,
  ColorScheme,
  Map as GoogleMap,
  useMap,
  useMapsLibrary,
} from '@vis.gl/react-google-maps';
import {
  ArrowLeft,
  Bus,
  Check,
  Clock,
  Code2,
  Copy,
  Download,
  Flag,
  Footprints,
  Gauge,
  ImagePlus,
  Landmark,
  Route,
  Share2,
  Trash2,
  X,
} from 'lucide-react';
import scenicUmrahPhoto from '../../assets/images/makkah_scenic_card_1795511632738.jpg';
import { GMP_ATTRIBUTION_ID } from '../../services/googleMapsService';
import {
  AvatarPreset,
  TripCardTheme,
  TripHistoryRecord,
} from '../../types/convoy';
import { projectTripMapViewport } from '../../utils/geo';
import { renderTripCardPngBlob } from '../../utils/tripCardRenderer';
import {
  buildTripGpxXml,
  buildTripJsonLog,
  buildTripShareSummaryText,
  triggerFileDownload,
} from '../../utils/tripExport';
import { ConvoyAvatar } from '../ConvoyAvatar';
import { TripSummaryShareCard } from '../TripSummaryShareCard';

interface TripDetailViewProps {
  trip: TripHistoryRecord | null;
  currentUserName?: string;
  currentUserAvatarPreset?: AvatarPreset;
  currentUserAvatarPhotoUrl?: string;
  autoFinishNotice?: string | null;
  onDismissAutoFinishNotice?: () => void;
  onBack: () => void;
}

const CARD_THEME_OPTIONS: Array<{ id: TripCardTheme; label: string }> = [
  { id: 'DARK_CARBON', label: 'Malam Makkah' },
  { id: 'SATELLITE', label: 'Topografi Haram' },
  { id: 'MINIMAL_LIGHT', label: 'Kiswah Putih' },
  { id: 'CUSTOM_PHOTO', label: 'Foto Galeri' },
];

function TripReplayPolylineOverlay({
  waypoints,
}: {
  waypoints: Array<{ latitude: number; longitude: number }>;
}) {
  const map = useMap();
  const mapsLib = useMapsLibrary('maps');

  const path = useMemo(
    () =>
      waypoints
        .filter(
          (w) =>
            Number.isFinite(w.latitude) &&
            Number.isFinite(w.longitude) &&
            (w.latitude !== 0 || w.longitude !== 0)
        )
        .map((w) => ({ lat: w.latitude, lng: w.longitude })),
    [waypoints]
  );

  useEffect(() => {
    if (!map || path.length === 0) return;
    const bounds = new google.maps.LatLngBounds();
    path.forEach((pt) => bounds.extend(pt));
    map.fitBounds(bounds, 24);
  }, [map, path]);

  useEffect(() => {
    if (!map || !mapsLib || path.length < 2) return;
    const casing = new mapsLib.Polyline({
      path,
      strokeColor: '#FFFFFF',
      strokeOpacity: 0.92,
      strokeWeight: 7,
      zIndex: 10,
      clickable: false,
      map,
    });
    const inner = new mapsLib.Polyline({
      path,
      strokeColor: '#047857',
      strokeOpacity: 0.96,
      strokeWeight: 4,
      zIndex: 11,
      clickable: false,
      map,
    });
    return () => {
      casing.setMap(null);
      inner.setMap(null);
    };
  }, [map, mapsLib, path]);

  return null;
}

export function TripDetailView({
  trip,
  currentUserName,
  currentUserAvatarPreset,
  currentUserAvatarPhotoUrl,
  autoFinishNotice,
  onDismissAutoFinishNotice,
  onBack,
}: TripDetailViewProps) {
  const [showGpxPreview, setShowGpxPreview] = useState(false);
  const [feedbackBanner, setFeedbackBanner] = useState<string | null>(null);
  const [sharePreviewText, setSharePreviewText] = useState<string | null>(null);
  const [cardTheme, setCardTheme] = useState<TripCardTheme>('DARK_CARBON');
  const [customPhotoUrl, setCustomPhotoUrl] = useState<string | undefined>(
    undefined
  );
  const [isRenderingCard, setIsRenderingCard] = useState(false);
  const [renderedCardModalUrl, setRenderedCardModalUrl] = useState<
    string | null
  >(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    return () => {
      if (renderedCardModalUrl) {
        URL.revokeObjectURL(renderedCardModalUrl);
      }
    };
  }, [renderedCardModalUrl]);

  if (!trip) {
    return (
      <div className="flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
        <div className="sticky top-0 z-10 flex items-center gap-1.5 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
          <button
            type="button"
            onClick={onBack}
            aria-label="Kembali"
            className="min-w-[42px] min-h-[42px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <h2 className="text-sm font-bold">Rekapitulasi Safar</h2>
        </div>
        <div className="flex-1 flex items-center justify-center p-6 text-xs text-[#4D6659]">
          Data perjalanan tidak ditemukan.
        </div>
      </div>
    );
  }

  const gpxXml = buildTripGpxXml(trip);
  const jsonLog = buildTripJsonLog(trip);
  const shareSummary = buildTripShareSummaryText(trip);
  const mapWidth = 320;
  const mapHeight = 180;
  const projection = projectTripMapViewport(
    trip.waypoints,
    trip.visitedCheckpoints,
    mapWidth,
    mapHeight,
    0.15
  );
  const polylinePts = projection.waypoints
    .map((pt) => `${pt.x.toFixed(2)},${pt.y.toFixed(2)}`)
    .join(' ');
  const startPt = projection.waypoints[0];
  const endPt = projection.waypoints[projection.waypoints.length - 1];
  const isDarkDocument =
    typeof document !== 'undefined' &&
    document.documentElement.classList.contains('dark');
  const geoWaypoints = trip.waypoints.filter(
    (w) =>
      Number.isFinite(w.latitude) &&
      Number.isFinite(w.longitude) &&
      (w.latitude !== 0 || w.longitude !== 0)
  );
  const geoStart = geoWaypoints[0];
  const geoEnd = geoWaypoints[geoWaypoints.length - 1];

  const showToast = (msg: string) => {
    setFeedbackBanner(msg);
    setTimeout(() => {
      setFeedbackBanner((prev) => (prev === msg ? null : prev));
    }, 3000);
  };

  const handleExportGpx = () => {
    triggerFileDownload(
      `safar-tracking-${trip.roomId.toLowerCase()}.gpx`,
      gpxXml,
      'application/gpx+xml'
    );
    showToast(
      `File safar-tracking-${trip.roomId.toLowerCase()}.gpx diunduh (kompatibel Google Earth & Garmin).`
    );
  };

  const handleExportJson = () => {
    triggerFileDownload(
      `safar-tracking-${trip.roomId.toLowerCase()}.json`,
      jsonLog,
      'application/json'
    );
    showToast(`Log safar-tracking-${trip.roomId.toLowerCase()}.json berhasil diunduh.`);
  };

  const handleUploadGalleryPhoto = (ev: ChangeEvent<HTMLInputElement>) => {
    const file = ev.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        setCustomPhotoUrl(reader.result);
        setCardTheme('CUSTOM_PHOTO');
        showToast('Foto perjalanan diterapkan ke kartu.');
      }
    };
    reader.readAsDataURL(file);
    ev.target.value = '';
  };

  const generatePngCardBlob = async () => {
    return renderTripCardPngBlob({
      trip,
      theme: cardTheme,
      customPhotoUrl,
      defaultScenicPhotoUrl: scenicUmrahPhoto,
      currentUserName,
      currentUserAvatarPreset,
      currentUserAvatarPhotoUrl,
    });
  };

  const handleShareCardImage = async () => {
    setIsRenderingCard(true);
    try {
      const blob = await generatePngCardBlob();
      const fileName = `safar-tracking-card-${trip.roomId.toLowerCase()}.png`;
      const pngFile = new File([blob], fileName, { type: 'image/png' });

      const caption = `🕌 ${trip.tourTitle} (${trip.roomId})\nJarak: ${trip.distanceKm.toFixed(1)} km · Durasi: ${trip.durationLabel} · Kec. Rata-rata: ${trip.avgSpeedKmh} km/j`;

      if (
        typeof navigator.canShare === 'function' &&
        navigator.canShare({ files: [pngFile] })
      ) {
        try {
          await navigator.share({
            files: [pngFile],
            title: `Kartu Safar ${trip.tourTitle}`,
            text: caption,
          });
          showToast('Kartu PNG berhasil dibagikan.');
          return;
        } catch {
          // User dismissed or iframe denied Web Share; show in-app Share Sheet Preview
        }
      }

      if (renderedCardModalUrl) {
        URL.revokeObjectURL(renderedCardModalUrl);
      }
      setRenderedCardModalUrl(URL.createObjectURL(blob));
    } catch {
      showToast('Gagal merender kartu gambar PNG.');
    } finally {
      setIsRenderingCard(false);
    }
  };

  const handleDownloadCardPng = async () => {
    setIsRenderingCard(true);
    try {
      const blob = await generatePngCardBlob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `safar-tracking-card-${trip.roomId.toLowerCase()}.png`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      showToast(
        `Kartu safar-tracking-card-${trip.roomId.toLowerCase()}.png (1080×1350) tersimpan.`
      );
    } catch {
      showToast('Gagal menyimpan file PNG.');
    } finally {
      setIsRenderingCard(false);
    }
  };

  const handleShareSummary = async () => {
    try {
      await navigator.clipboard.writeText(shareSummary);
    } catch {
      // Clipboard fallback handled by preview modal
    }
    if (navigator.share) {
      try {
        await navigator.share({
          title: `Rekap Safar ${trip.tourTitle}`,
          text: shareSummary,
        });
        showToast('Ringkasan perjalanan dibagikan.');
        return;
      } catch {
        // User cancelled or share unavailable in iframe
      }
    }
    setSharePreviewText(shareSummary);
    showToast(
      'Ringkasan perjalanan disalin & siap dibagikan ke WhatsApp / Sosial Media.'
    );
  };

  return (
    <div className="flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Top App Bar */}
      <div className="sticky top-0 z-10 flex items-center gap-1.5 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="button"
          onClick={onBack}
          aria-label="Kembali"
          className="min-w-[42px] min-h-[42px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer shrink-0"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="min-w-0">
          <h2 className="text-sm font-bold truncate">{trip.tourTitle}</h2>
          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate font-mono">
            {trip.roomId} · {trip.dateLabel}
          </p>
        </div>
      </div>

      {/* Feedback Banner */}
      {feedbackBanner && (
        <div className="px-4 py-2 bg-[#ECFDF5] dark:bg-[#064E3B] border-b border-[#A7F3D0] dark:border-[#047857] text-[#065F46] dark:text-[#A7F3D0] text-[11px] font-medium flex items-center gap-2">
          <Check className="w-3.5 h-3.5 shrink-0" />
          <span>{feedbackBanner}</span>
        </div>
      )}

      {/* Body */}
      <div className="flex-1 px-4 py-4 space-y-4">
        {autoFinishNotice && (
          <div className="rounded-[16px] bg-[#ECFDF5] dark:bg-[#064E3B]/60 border border-[#A7F3D0] dark:border-[#047857] px-3.5 py-2.5 flex items-center gap-2.5 text-[#065F46] dark:text-[#A7F3D0]">
            <Flag className="w-4 h-4 shrink-0 text-[#047857] dark:text-[#34D399]" />
            <span className="text-[11px] font-medium flex-1 leading-relaxed">
              {autoFinishNotice}
            </span>
            {onDismissAutoFinishNotice && (
              <button
                type="button"
                onClick={onDismissAutoFinishNotice}
                className="text-[11px] font-semibold text-[#047857] dark:text-[#6EE7B7] cursor-pointer shrink-0"
              >
                Tutup
              </button>
            )}
          </div>
        )}

        {/* 0. Umrah Journey Summary Card & Social Share Studio */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3.5">
          <div>
            <h3 className="text-sm font-bold">Kartu Perjalanan Umroh</h3>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] mt-0.5 leading-relaxed">
              Pilih nuansa latar belakang Makkah lalu ekspor sebagai gambar PNG (1080×1350) untuk dibagikan ke jamaah atau keluarga.
            </p>
          </div>

          <TripSummaryShareCard
            trip={trip}
            theme={cardTheme}
            customPhotoUrl={customPhotoUrl}
            defaultScenicPhotoUrl={scenicUmrahPhoto}
            currentUserName={currentUserName}
            currentUserAvatarPreset={currentUserAvatarPreset}
            currentUserAvatarPhotoUrl={currentUserAvatarPhotoUrl}
          />

          {/* Theme Customization Selector */}
          <div className="grid grid-cols-2 gap-1.5">
            {CARD_THEME_OPTIONS.map((opt) => {
              const active = cardTheme === opt.id;
              return (
                <button
                  key={opt.id}
                  type="button"
                  onClick={() => setCardTheme(opt.id)}
                  className={`min-h-[44px] px-3 py-2 rounded-xl text-xs font-semibold border transition-colors cursor-pointer whitespace-nowrap ${
                    active
                      ? 'bg-[#047857] border-[#047857] text-white'
                      : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E] text-[#1E3A2F] dark:text-[#D1FAE5] hover:border-[#047857]'
                  }`}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>

          {/* Custom Gallery Photo Picker */}
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            onChange={handleUploadGalleryPhoto}
            className="hidden"
          />
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => fileInputRef.current?.click()}
              className="flex-1 min-h-[44px] px-3 rounded-xl border border-[#C6DACD] dark:border-[#1E3F31] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer whitespace-nowrap"
            >
              <ImagePlus className="w-4 h-4 shrink-0 text-[#047857] dark:text-[#34D399]" />
              <span>
                {customPhotoUrl ? 'Ganti Foto Galeri' : 'Tambah Foto Galeri'}
              </span>
            </button>

            {customPhotoUrl && (
              <button
                type="button"
                aria-label="Hapus foto kustom"
                onClick={() => {
                  setCustomPhotoUrl(undefined);
                  showToast('Foto kustom dihapus, kembali ke foto Masjidil Haram.');
                }}
                className="min-h-[44px] min-w-[44px] px-3 rounded-xl border border-[#C6DACD] dark:border-[#1E3F31] hover:bg-[#FEE2E2] hover:border-[#FCA5A5] text-[#DC2626] flex items-center justify-center cursor-pointer"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Primary CTA: Bagikan Kartu Safar */}
          <button
            type="button"
            disabled={isRenderingCard}
            onClick={handleShareCardImage}
            className="w-full min-h-[48px] px-4 rounded-[14px] bg-[#B45309] hover:bg-[#92400E] disabled:opacity-60 text-white text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
          >
            <Share2 className="w-4 h-4 shrink-0" />
            <span>
              {isRenderingCard
                ? 'Merender Kartu PNG...'
                : 'Bagikan Kartu Safar (Share Image)'}
            </span>
          </button>

          <button
            type="button"
            disabled={isRenderingCard}
            onClick={handleDownloadCardPng}
            className="w-full min-h-[44px] px-4 rounded-[14px] border border-[#C6DACD] dark:border-[#1E3F31] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] disabled:opacity-60 text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
          >
            <Download className="w-4 h-4 shrink-0" />
            <span>Simpan Kartu ke Galeri (.PNG)</span>
          </button>
        </div>

        {/* 1. Mini Polyline Track Replay Map */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
          <div className="flex items-center justify-between gap-2">
            <div>
              <h3 className="text-sm font-bold">Peta Rekam Jejak Ziarah</h3>
              <p className="text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                {projection.hasGeo
                  ? 'Mengikuti jalur nyata yang ditempuh di atas Google Maps'
                  : 'Rekaman jalur perjalanan'}
              </p>
            </div>
            <span className="px-2 py-0.5 rounded-md bg-[#ECFDF5] dark:bg-[#064E3B]/60 text-[#047857] dark:text-[#6EE7B7] text-[10px] font-mono font-semibold shrink-0">
              {trip.waypoints.length} Titik GPS
            </span>
          </div>

          <div className="relative w-full aspect-[16/9] min-h-[180px] rounded-[16px] bg-[#EBF3EE] dark:bg-[#091812] border border-[#C6DACD] dark:border-[#1E3F31] overflow-hidden">
            {projection.hasGeo && geoStart ? (
              <GoogleMap
                mapId="DEMO_MAP_ID"
                colorScheme={isDarkDocument ? ColorScheme.DARK : ColorScheme.LIGHT}
                gestureHandling="cooperative"
                disableDefaultUI={true}
                defaultCenter={{ lat: geoStart.latitude, lng: geoStart.longitude }}
                defaultZoom={11}
                internalUsageAttributionIds={[GMP_ATTRIBUTION_ID]}
                className="w-full h-full"
              >
                <TripReplayPolylineOverlay waypoints={geoWaypoints} />

                <AdvancedMarker
                  position={{ lat: geoStart.latitude, lng: geoStart.longitude }}
                  zIndex={120}
                  clickable={false}
                >
                  <span className="block w-3.5 h-3.5 rounded-full bg-[#D97706] border-2 border-white shadow" />
                </AdvancedMarker>

                {geoEnd && (
                  <AdvancedMarker
                    position={{ lat: geoEnd.latitude, lng: geoEnd.longitude }}
                    zIndex={130}
                    clickable={false}
                  >
                    <span className="block w-3.5 h-3.5 rounded-full bg-[#047857] border-2 border-white shadow" />
                  </AdvancedMarker>
                )}

                {trip.visitedCheckpoints
                  .filter(
                    (cp) =>
                      typeof cp.latitude === 'number' &&
                      typeof cp.longitude === 'number'
                  )
                  .map((cp) => (
                    <AdvancedMarker
                      key={cp.id}
                      position={{ lat: cp.latitude!, lng: cp.longitude! }}
                      zIndex={110}
                      clickable={false}
                    >
                      <div
                        title={cp.name}
                        className="w-6 h-6 rounded-full bg-[#065F46] border-2 border-white shadow-sm flex items-center justify-center text-[#FCD34D]"
                      >
                        <Landmark className="w-3 h-3" />
                      </div>
                    </AdvancedMarker>
                  ))}
              </GoogleMap>
            ) : (
              <>
                <svg
                  className="w-full h-full"
                  viewBox="0 0 320 180"
                  preserveAspectRatio="xMidYMid meet"
                >
                  <defs>
                    <pattern
                      id="tripReplayGrid"
                      width="26"
                      height="26"
                      patternUnits="userSpaceOnUse"
                    >
                      <path
                        d="M 26 0 L 0 0 0 26"
                        fill="none"
                        stroke="currentColor"
                        className="text-[#D4E3D9] dark:text-[#17362A]"
                        strokeWidth="1"
                      />
                    </pattern>
                  </defs>
                  <rect width="320" height="180" fill="url(#tripReplayGrid)" />
                  {projection.waypoints.length >= 2 && (
                    <>
                      <polyline
                        points={polylinePts}
                        fill="none"
                        stroke="#FFFFFF"
                        strokeWidth="8.5"
                        strokeOpacity="0.92"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                      <polyline
                        points={polylinePts}
                        fill="none"
                        stroke="#047857"
                        strokeWidth="4.2"
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
                        left: `${(projectedCp.x / mapWidth) * 100}%`,
                        top: `${(projectedCp.y / mapHeight) * 100}%`,
                      }}
                      title={cp.name}
                      className="absolute z-[2] -translate-x-1/2 -translate-y-1/2 w-6 h-6 rounded-full bg-[#065F46] border-2 border-white shadow-sm flex items-center justify-center text-[#FCD34D]"
                    >
                      <Landmark className="w-3 h-3" />
                    </div>
                  );
                })}
              </>
            )}

            <div className="absolute z-[2] bottom-2 left-2 px-2 py-0.5 rounded-md bg-[#06130E]/85 text-white text-[9px] font-medium pointer-events-none">
              Emas: Start · Zamrud: Tujuan · Pos Kumpul
            </div>
          </div>

          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] leading-relaxed">
            Tujuan: <span className="font-semibold text-[#0B1F16] dark:text-[#ECFDF5]">{trip.destinationName}</span> · {trip.destinationAddress}
          </p>
        </div>

        {/* 2. Statistics Grid */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
          <h3 className="text-sm font-bold">Statistik Perjalanan Rombongan</h3>
          <div className="grid grid-cols-2 gap-2.5">
            <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3">
              <div className="flex items-center gap-1.5 text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                <Route className="w-3.5 h-3.5 text-[#047857]" />
                <span>Total Jarak</span>
              </div>
              <p className="mt-1 text-base font-bold font-mono tabular-nums">
                {trip.distanceKm.toFixed(1)} km
              </p>
            </div>

            <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3">
              <div className="flex items-center gap-1.5 text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                <Clock className="w-3.5 h-3.5 text-[#047857]" />
                <span>Durasi Total</span>
              </div>
              <p className="mt-1 text-base font-bold font-mono tabular-nums">
                {trip.durationLabel}
              </p>
            </div>

            <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3">
              <div className="flex items-center gap-1.5 text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                <Gauge className="w-3.5 h-3.5 text-[#B45309]" />
                <span>Kec. Rata-rata</span>
              </div>
              <p className="mt-1 text-base font-bold font-mono tabular-nums">
                {trip.avgSpeedKmh} km/j
              </p>
            </div>

            <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3">
              <div className="flex items-center gap-1.5 text-[10px] text-[#4D6659] dark:text-[#9CC2AE]">
                <Flag className="w-3.5 h-3.5 text-[#047857]" />
                <span>Kec. Maksimal</span>
              </div>
              <p className="mt-1 text-base font-bold font-mono tabular-nums">
                {trip.maxSpeedKmh} km/j
              </p>
            </div>
          </div>
        </div>

        {/* 3. Export GPX / JSON & Share Summary */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
          <div>
            <h3 className="text-sm font-bold">Ekspor Data &amp; Bagikan</h3>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] mt-0.5">
              Unduh file GPX/JSON untuk dokumentasi perjalanan, atau bagikan rekap rombongan ke WhatsApp.
            </p>
          </div>

          <button
            type="button"
            onClick={handleExportGpx}
            className="w-full min-h-[46px] px-4 rounded-[14px] bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center justify-center gap-2 transition-colors cursor-pointer"
          >
            <Download className="w-4 h-4 shrink-0" />
            <span>Ekspor Rute (.GPX)</span>
          </button>

          <div className="grid grid-cols-2 gap-2.5">
            <button
              type="button"
              onClick={handleExportJson}
              className="min-h-[44px] px-3 rounded-[14px] border border-[#C6DACD] dark:border-[#1E3F31] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <Code2 className="w-4 h-4 shrink-0" />
              <span>Ekspor JSON</span>
            </button>

            <button
              type="button"
              onClick={handleShareSummary}
              className="min-h-[44px] px-3 rounded-[14px] bg-[#B45309] hover:bg-[#92400E] text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <Share2 className="w-4 h-4 shrink-0" />
              <span>Bagikan Rekap</span>
            </button>
          </div>

          {sharePreviewText && (
            <div className="rounded-xl bg-[#FFFBEB] dark:bg-[#451A03]/50 border border-[#FDE68A] dark:border-[#B45309] p-3 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-[11px] font-bold text-[#92400E] dark:text-[#FCD34D]">
                  Teks Ringkasan Siap Kirim (WhatsApp / Sosmed)
                </span>
                <button
                  type="button"
                  onClick={async () => {
                    try {
                      await navigator.clipboard.writeText(sharePreviewText);
                    } catch {
                      // ignore
                    }
                    showToast('Teks ringkasan disalin.');
                  }}
                  className="text-[10px] font-semibold text-[#B45309] dark:text-[#FCD34D] flex items-center gap-1 cursor-pointer"
                >
                  <Copy className="w-3 h-3" />
                  <span>Salin</span>
                </button>
              </div>
              <pre className="text-[10px] leading-relaxed whitespace-pre-wrap font-sans text-[#451A03] dark:text-[#FDE68A]">
                {sharePreviewText}
              </pre>
            </div>
          )}

          <button
            type="button"
            onClick={() => setShowGpxPreview((prev) => !prev)}
            className="text-[11px] font-semibold text-[#047857] dark:text-[#34D399] hover:underline cursor-pointer"
          >
            {showGpxPreview ? 'Sembunyikan Pratinjau GPX' : 'Lihat Pratinjau GPX 1.1 XML'}
          </button>

          {showGpxPreview && (
            <pre className="rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 text-[10px] font-mono overflow-x-auto leading-relaxed">
              {gpxXml}
            </pre>
          )}
        </div>

        {/* 4. Pos Kumpul / Checkpoints Visited */}
        <div className="space-y-2">
          <h3 className="text-sm font-bold">
            Pos Kumpul / Miqat Dikunjungi ({trip.visitedCheckpoints.length})
          </h3>
          {trip.visitedCheckpoints.length === 0 ? (
            <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
              Tidak ada pos kumpul tercatat pada perjalanan ini.
            </p>
          ) : (
            <div className="space-y-2">
              {trip.visitedCheckpoints.map((cp) => (
                <div
                  key={cp.id}
                  className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 flex items-center gap-3"
                >
                  <div className="w-9 h-9 rounded-full bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399] flex items-center justify-center shrink-0">
                    <Landmark className="w-4 h-4" />
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-semibold truncate">{cp.name}</p>
                    <p className="text-[10px] font-mono text-[#4D6659] dark:text-[#9CC2AE]">
                      {cp.latitude.toFixed(4)}, {cp.longitude.toFixed(4)}
                    </p>
                  </div>
                  <span className="text-[11px] font-mono font-semibold text-[#047857] dark:text-[#34D399] shrink-0">
                    {cp.arrivedAtLabel}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* 5. Participants */}
        <div className="space-y-2">
          <h3 className="text-sm font-bold">
            Jamaah Rombongan ({trip.participants.length})
          </h3>
          {trip.participants.length === 0 ? (
            <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE]">
              Daftar jamaah tidak tercatat pada riwayat ini.
            </p>
          ) : (
            <div className="space-y-2">
              {trip.participants.map((p) => (
                <div
                  key={p.id}
                  className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 flex items-center gap-3"
                >
                  <ConvoyAvatar
                    preset={p.avatarPreset}
                    photoUrl={p.avatarPhotoUrl}
                    name={p.fullName}
                    sizeClass="w-9 h-9"
                    ringClass={
                      p.role === 'TOUR_LEADER'
                        ? 'ring-2 ring-[#047857]'
                        : 'ring-1 ring-[#C6DACD] dark:ring-[#1E3F31]'
                    }
                  />
                  <div className="min-w-0 flex-1">
                    <p className="text-xs font-semibold truncate">{p.fullName}</p>
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                      {p.isSweeper
                        ? 'Pendamping'
                        : p.role === 'TOUR_LEADER'
                          ? 'Mutawwif'
                          : 'Jamaah'}{' '}
                      · {p.vehicleDetail} ·{' '}
                      <span className="font-mono">{p.licensePlate}</span>
                    </p>
                  </div>
                  {p.vehicleType === 'MOBIL' ? (
                    <Bus className="w-4 h-4 text-[#4D6659] shrink-0" />
                  ) : (
                    <Footprints className="w-4 h-4 text-[#4D6659] shrink-0" />
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Rendered PNG Share Sheet Preview Modal */}
      {renderedCardModalUrl && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Bagikan Kartu Safar"
          className="fixed inset-0 z-50 bg-black/70 backdrop-blur-xs flex items-end sm:items-center justify-center p-3"
        >
          <div className="w-full max-w-[360px] rounded-[24px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3 shadow-2xl">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h4 className="text-sm font-bold">Kartu Siap Dibagikan</h4>
                <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                  PNG 1080×1350 · Instagram Story · WhatsApp · Dokumentasi
                </p>
              </div>
              <button
                type="button"
                aria-label="Tutup dialog"
                onClick={() => setRenderedCardModalUrl(null)}
                className="min-w-[40px] min-h-[40px] rounded-full hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] flex items-center justify-center cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="rounded-[18px] overflow-hidden border border-[#D8E6DD] dark:border-[#1B3B2E] bg-[#041A12]">
              <img
                src={renderedCardModalUrl}
                alt="Pratinjau PNG Kartu Rute Safar Tracking"
                className="w-full max-h-[340px] object-contain mx-auto"
              />
            </div>

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => {
                  const a = document.createElement('a');
                  a.href = renderedCardModalUrl;
                  a.download = `safar-tracking-card-${trip.roomId.toLowerCase()}.png`;
                  document.body.appendChild(a);
                  a.click();
                  document.body.removeChild(a);
                  showToast('Gambar PNG diunduh ke perangkat Anda.');
                }}
                className="min-h-[44px] px-3 rounded-xl bg-[#B45309] hover:bg-[#92400E] text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Download className="w-4 h-4 shrink-0" />
                <span>Unduh PNG</span>
              </button>

              <button
                type="button"
                onClick={async () => {
                  try {
                    await navigator.clipboard.writeText(shareSummary);
                  } catch {
                    // Ignore
                  }
                  showToast('Caption safar disalin ke clipboard.');
                }}
                className="min-h-[44px] px-3 rounded-xl border border-[#C6DACD] dark:border-[#1E3F31] hover:bg-[#F4F8F5] dark:hover:bg-[#132E24] text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer"
              >
                <Copy className="w-4 h-4 shrink-0" />
                <span>Salin Caption</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
