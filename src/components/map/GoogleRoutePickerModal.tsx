import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AdvancedMarker,
  ColorScheme,
  Map as GoogleMap,
  MapMouseEvent,
  useMap,
  useMapsLibrary,
} from '@vis.gl/react-google-maps';
import {
  Check,
  Layers,
  LocateFixed,
  MapPin,
  Search,
  X,
} from 'lucide-react';
import { RoutePoint } from '../../types/convoy';
import { haversineMeters, LatLngPoint } from '../../utils/geo';
import {
  GMP_ATTRIBUTION_ID,
  GooglePlaceSuggestion,
  reverseGeocodeGoogle,
  searchGoogleAutocompleteSuggestions,
  selectGooglePlacePrediction,
} from '../../services/googleMapsService';

interface GoogleRoutePickerModalProps {
  mode?: 'origin' | 'destination' | 'add_point';
  initialPoint: RoutePoint;
  initialName: string;
  userGpsLocation?: LatLngPoint | null;
  darkMode?: boolean;
  onDismiss: () => void;
  onConfirm: (
    lat: number,
    lng: number,
    placeName: string,
    addressLabel: string
  ) => void;
}

interface LandmarkCoord {
  name: string;
  address: string;
  lat: number;
  lng: number;
}

const MAKKAH_UMRAH_LANDMARKS: LandmarkCoord[] = [
  {
    name: 'Masjidil Haram (Ka\'bah)',
    address: 'Al Haram, Makkah Al-Mukarramah, Arab Saudi',
    lat: 21.4225,
    lng: 39.8262,
  },
  {
    name: 'Hotel Area Ajyad Makkah',
    address: 'Jl. Ajyad, Makkah Al-Mukarramah, Arab Saudi',
    lat: 21.418,
    lng: 39.8275,
  },
  {
    name: 'Terminal Syib Amir Makkah',
    address: 'Area Masjidil Haram Utara, Makkah Al-Mukarramah',
    lat: 21.4258,
    lng: 39.8296,
  },
  {
    name: 'Jabal Rahmah · Arafah',
    address: 'Padang Arafah, Makkah Al-Mukarramah',
    lat: 21.3549,
    lng: 39.9841,
  },
  {
    name: 'Jabal Nur · Gua Hira',
    address: 'Al Nur, Makkah Al-Mukarramah',
    lat: 21.4576,
    lng: 39.8594,
  },
  {
    name: 'Masjid Nabawi · Madinah',
    address: 'Al Haram, Madinah Al-Munawwarah, Arab Saudi',
    lat: 24.4672,
    lng: 39.6111,
  },
];

function PickerCameraSync({
  focusRequest,
}: {
  focusRequest: { lat: number; lng: number; zoom: number; seq: number } | null;
}) {
  const map = useMap();
  useEffect(() => {
    if (!map || !focusRequest) return;
    map.panTo({ lat: focusRequest.lat, lng: focusRequest.lng });
    map.setZoom(focusRequest.zoom);
  }, [map, focusRequest]);
  return null;
}

export function GoogleRoutePickerModal({
  mode = 'destination',
  initialPoint,
  initialName,
  userGpsLocation,
  darkMode = false,
  onDismiss,
  onConfirm,
}: GoogleRoutePickerModalProps) {
  const placesLib = useMapsLibrary('places');
  const sessionTokenRef = useRef<google.maps.places.AutocompleteSessionToken | undefined>(
    undefined
  );

  const isOrigin = mode === 'origin';
  const isAddPoint = mode === 'add_point';
  const [pin, setPin] = useState<LatLngPoint>({
    lat: initialPoint.latitude || (isOrigin ? 21.418 : 21.4225),
    lng: initialPoint.longitude || (isOrigin ? 39.8275 : 39.8262),
  });
  const [placeName, setPlaceName] = useState(
    initialName ||
      (isOrigin
        ? 'Titik Kumpul Berangkat'
        : isAddPoint
          ? 'Titik Tujuan Baru'
          : 'Tujuan Ziarah / Ibadah')
  );
  const [addressLabel, setAddressLabel] = useState(
    `${(initialPoint.latitude || 21.4225).toFixed(4)}, ${(initialPoint.longitude || 39.8262).toFixed(4)}`
  );
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<GooglePlaceSuggestion[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [mapType, setMapType] = useState<'roadmap' | 'hybrid'>('roadmap');
  const [focusRequest, setFocusRequest] = useState<{
    lat: number;
    lng: number;
    zoom: number;
    seq: number;
  } | null>(null);

  const defaultCenter = useMemo(
    () => ({ lat: pin.lat, lng: pin.lng }),
    []
  );

  // Initialize AutocompleteSessionToken once places library is ready
  useEffect(() => {
    if (placesLib && !sessionTokenRef.current) {
      sessionTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
  }, [placesLib]);

  const nearestLandmarkFallback = (lat: number, lng: number) => {
    let best = MAKKAH_UMRAH_LANDMARKS[0];
    let bestMeters = Infinity;
    for (const lm of MAKKAH_UMRAH_LANDMARKS) {
      const dist = haversineMeters({ lat, lng }, { lat: lm.lat, lng: lm.lng });
      if (dist < bestMeters) {
        bestMeters = dist;
        best = lm;
      }
    }
    return bestMeters < 4000
      ? {
          name: best.name,
          address: `${best.address} · ${lat.toFixed(4)}, ${lng.toFixed(4)}`,
        }
      : null;
  };

  const handleSelectCoord = async (lat: number, lng: number) => {
    setPin({ lat, lng });
    const landmark = nearestLandmarkFallback(lat, lng);
    if (landmark) {
      setPlaceName(landmark.name);
      setAddressLabel(landmark.address);
      return;
    }
    setPlaceName('Memuat nama lokasi...');
    setAddressLabel(`${lat.toFixed(5)}, ${lng.toFixed(5)}`);
    const resolved = await reverseGeocodeGoogle(lat, lng);
    setPlaceName(resolved.name);
    setAddressLabel(resolved.address);
  };

  // Debounced Google Places API (New) AutocompleteSuggestion search
  useEffect(() => {
    const trimmed = searchQuery.trim();
    if (!placesLib || trimmed.length < 2) {
      setSearchResults([]);
      setIsSearching(false);
      return;
    }
    let cancelled = false;
    setIsSearching(true);
    if (!sessionTokenRef.current) {
      sessionTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
    const timer = window.setTimeout(() => {
      searchGoogleAutocompleteSuggestions(
        placesLib,
        trimmed,
        sessionTokenRef.current
      )
        .then((res) => {
          if (!cancelled) setSearchResults(res);
        })
        .finally(() => {
          if (!cancelled) setIsSearching(false);
        });
    }, 320);
    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [placesLib, searchQuery]);

  const handlePickSuggestion = async (item: GooglePlaceSuggestion) => {
    setIsSearching(true);
    const resolved = await selectGooglePlacePrediction(item);
    if (placesLib) {
      sessionTokenRef.current = new placesLib.AutocompleteSessionToken();
    }
    setIsSearching(false);
    if (!resolved) return;

    setPin({ lat: resolved.lat, lng: resolved.lng });
    setPlaceName(resolved.name);
    setAddressLabel(resolved.address);
    setSearchQuery('');
    setSearchResults([]);
    setFocusRequest((prev) => ({
      lat: resolved.lat,
      lng: resolved.lng,
      zoom: 14,
      seq: (prev?.seq ?? 0) + 1,
    }));
  };

  const handleMapClick = (ev: MapMouseEvent) => {
    const ll = ev.detail.latLng;
    if (!ll) return;
    handleSelectCoord(ll.lat, ll.lng);
  };

  const pinColor = isOrigin ? '#B45309' : '#047857';
  const pinEmoji = isOrigin ? '⚑' : '🕌';

  return (
    <div className="fixed inset-0 z-50 bg-black/55 backdrop-blur-[2px] flex items-center justify-center p-2 sm:p-4">
      <div className="w-full max-w-[390px] h-[640px] max-h-[92vh] rounded-[24px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] shadow-2xl overflow-hidden flex flex-col">
        <div className="px-3.5 py-2.5 bg-white dark:bg-[#0C1E17] border-b border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center justify-between gap-2">
          <div className="min-w-0">
            <h3 className="text-sm font-bold truncate">
              {isOrigin
                ? 'Pilih Titik Berangkat di Peta'
                : isAddPoint
                  ? 'Tambah Titik Tujuan (Add Point)'
                  : 'Pilih Tujuan Ziarah di Peta'}
            </h3>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              {isOrigin
                ? 'Cari hotel/maktab atau ketuk peta untuk menaruh pin'
                : isAddPoint
                  ? 'Cari lokasi atau ketuk peta untuk menambah titik singgah/tujuan'
                  : 'Cari masjid/tempat ziarah atau ketuk peta untuk pin tujuan'}
            </p>
          </div>
          <button
            type="button"
            onClick={onDismiss}
            aria-label="Tutup Peta"
            className="w-9 h-9 rounded-full flex items-center justify-center hover:bg-[#ECFDF5] dark:hover:bg-[#112A20] cursor-pointer shrink-0"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Google Places (New) Autocomplete Search */}
        <div className="relative px-3 py-2 bg-[#F4F8F5] dark:bg-[#06130E] border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
          <div className="flex items-center gap-2 px-3 min-h-[40px] rounded-xl bg-white dark:bg-[#0C1E17] border border-[#CBD5E1] dark:border-[#1B3B2E]">
            <Search className="w-4 h-4 text-[#4D6659] shrink-0" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder={
                isOrigin
                  ? 'Cari lokasi kumpul (mis. Hotel Ajyad, Syib Amir)...'
                  : 'Cari tujuan (mis. Masjidil Haram, Arafah)...'
              }
              className="w-full bg-transparent text-xs outline-none"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => {
                  setSearchQuery('');
                  setSearchResults([]);
                }}
                aria-label="Hapus pencarian"
                className="text-[#64748B] cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {(isSearching || searchResults.length > 0) && (
            <div className="absolute left-3 right-3 top-[52px] z-30 rounded-xl bg-white dark:bg-[#141C24] border border-[#CBD5E1] dark:border-[#334155] shadow-xl max-h-52 overflow-y-auto divide-y divide-[#E2E8F0] dark:divide-[#334155]">
              {isSearching ? (
                <p className="p-3 text-[11px] text-[#64748B]">
                  Mencari lokasi di Google Maps...
                </p>
              ) : (
                searchResults.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handlePickSuggestion(item)}
                    className="w-full text-left px-3 py-2 hover:bg-[#EFF6FF] dark:hover:bg-[#1E293B] cursor-pointer"
                  >
                    <p className="text-xs font-semibold truncate">{item.name}</p>
                    <p className="text-[10px] text-[#64748B] dark:text-[#94A3B8] truncate">
                      {item.address}
                    </p>
                  </button>
                ))
              )}
            </div>
          )}
        </div>

        {/* Map Canvas */}
        <div className="relative flex-1 min-h-[240px]">
          <GoogleMap
            mapId="DEMO_MAP_ID"
            colorScheme={darkMode ? ColorScheme.DARK : ColorScheme.LIGHT}
            mapTypeId={mapType}
            gestureHandling="greedy"
            disableDefaultUI={true}
            defaultCenter={defaultCenter}
            defaultZoom={12}
            internalUsageAttributionIds={[GMP_ATTRIBUTION_ID]}
            onClick={handleMapClick}
            className="w-full h-full"
          >
            <PickerCameraSync focusRequest={focusRequest} />

            {/* Selected Pin */}
            <AdvancedMarker position={{ lat: pin.lat, lng: pin.lng }} zIndex={200}>
              <div className="flex flex-col items-center pointer-events-none">
                <span className="px-2 py-0.5 rounded-lg bg-[#0F172A] text-white text-[10px] font-semibold shadow-md max-w-[170px] truncate">
                  {placeName}
                </span>
                <span
                  className="mt-[3px] w-[30px] h-[30px] rounded-full border-2 border-white shadow-lg flex items-center justify-center text-white text-[13px]"
                  style={{ backgroundColor: pinColor }}
                >
                  {pinEmoji}
                </span>
              </div>
            </AdvancedMarker>

            {/* User GPS Dot */}
            {userGpsLocation && (
              <AdvancedMarker
                position={{ lat: userGpsLocation.lat, lng: userGpsLocation.lng }}
                zIndex={150}
              >
                <div className="w-3.5 h-3.5 rounded-full bg-[#047857] border-2 border-white shadow-md" />
              </AdvancedMarker>
            )}
          </GoogleMap>

          <button
            type="button"
            onClick={() =>
              setMapType((m) => (m === 'roadmap' ? 'hybrid' : 'roadmap'))
            }
            aria-label="Ubah Tampilan Peta"
            className={`absolute top-3 right-3 z-20 w-9 h-9 rounded-xl border shadow-md flex items-center justify-center cursor-pointer ${
              mapType === 'hybrid'
                ? 'bg-[#047857] text-white border-[#065F46]'
                : 'bg-white dark:bg-[#0C1E17] border-[#CBD5E1] dark:border-[#1B3B2E]'
            }`}
          >
            <Layers className="w-4 h-4" />
          </button>

          {userGpsLocation && (
            <button
              type="button"
              onClick={() => {
                setPin(userGpsLocation);
                setPlaceName('Lokasi GPS Saya Saat Ini');
                setAddressLabel(
                  `${userGpsLocation.lat.toFixed(5)}, ${userGpsLocation.lng.toFixed(5)}`
                );
                setFocusRequest((prev) => ({
                  lat: userGpsLocation.lat,
                  lng: userGpsLocation.lng,
                  zoom: 14,
                  seq: (prev?.seq ?? 0) + 1,
                }));
              }}
              className="absolute bottom-3 right-3 z-20 px-2.5 py-1.5 rounded-xl bg-white dark:bg-[#0C1E17] border border-[#CBD5E1] dark:border-[#1B3B2E] shadow-md text-[11px] font-semibold flex items-center gap-1.5 cursor-pointer"
            >
              <LocateFixed className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FBBF24]" />
              <span>Pin di GPS Saya</span>
            </button>
          )}
        </div>

        {/* Footer Confirm Panel */}
        <div className="px-3.5 pt-3.5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E] space-y-2.5">
          <div className="flex items-start gap-2.5">
            <div
              className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                isOrigin
                  ? 'bg-[#FFFBEB] dark:bg-[#451A03] text-[#B45309] dark:text-[#FBBF24]'
                  : 'bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399]'
              }`}
            >
              <MapPin className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <input
                type="text"
                value={placeName}
                onChange={(e) => setPlaceName(e.target.value)}
                aria-label={isOrigin ? 'Nama Titik Berangkat' : 'Nama Lokasi Tujuan'}
                className="w-full text-xs font-bold bg-transparent border-b border-transparent focus:border-[#047857] outline-none pb-0.5"
              />
              <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
                {addressLabel}
              </p>
              <p className="text-[10px] font-mono text-[#047857] dark:text-[#34D399]">
                {pin.lat.toFixed(5)}, {pin.lng.toFixed(5)}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onDismiss}
              className="flex-1 min-h-[42px] rounded-xl border border-[#CBD5E1] dark:border-[#1B3B2E] text-xs font-semibold cursor-pointer"
            >
              Batal
            </button>
            <button
              type="button"
              onClick={() =>
                onConfirm(
                  pin.lat,
                  pin.lng,
                  placeName.trim() ||
                    (isOrigin ? 'Titik Kumpul Berangkat' : 'Tujuan Ziarah'),
                  addressLabel
                )
              }
              className={`flex-1 min-h-[42px] rounded-xl text-white text-xs font-semibold flex items-center justify-center gap-1.5 cursor-pointer ${
                isOrigin
                  ? 'bg-[#B45309] hover:bg-[#92400E]'
                  : 'bg-[#047857] hover:bg-[#065F46]'
              }`}
            >
              <Check className="w-4 h-4" />
              <span>
                {isOrigin
                  ? 'Tetapkan Titik Start'
                  : isAddPoint
                    ? 'Tambahkan Point Ini'
                    : 'Tetapkan Tujuan'}
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
