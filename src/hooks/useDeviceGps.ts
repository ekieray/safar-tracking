import { useCallback, useEffect, useRef, useState } from 'react';
import { computeBearingDegrees, haversineMeters } from '../utils/geo';

export type GpsPermissionStatus =
  | 'idle'
  | 'requesting'
  | 'active'
  | 'denied'
  | 'unavailable';

export interface LiveGpsFix {
  latitude: number;
  longitude: number;
  speedKmh: number;
  heading: number;
  accuracyMeters: number;
  timestampMs: number;
}

interface UseDeviceGpsOptions {
  enabled: boolean;
  onFix?: (fix: LiveGpsFix) => void;
}

export function useDeviceGps({ enabled, onFix }: UseDeviceGpsOptions) {
  const [status, setStatus] = useState<GpsPermissionStatus>(
    typeof navigator !== 'undefined' && 'geolocation' in navigator
      ? 'idle'
      : 'unavailable'
  );
  const [lastFix, setLastFix] = useState<LiveGpsFix | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [retryCounter, setRetryCounter] = useState(0);

  const prevFixRef = useRef<LiveGpsFix | null>(null);
  const onFixRef = useRef(onFix);
  onFixRef.current = onFix;

  const requestSingleFix = useCallback(() => {
    setRetryCounter((c) => c + 1);
  }, []);

  useEffect(() => {
    if (!enabled) {
      setStatus('idle');
      return;
    }

    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      setStatus('unavailable');
      setErrorMessage('Perangkat atau browser tidak mendukung sensor GPS.');
      return;
    }

    setStatus('requesting');
    setErrorMessage(null);

    const handleSuccess = (position: GeolocationPosition) => {
      const { latitude, longitude, speed, heading, accuracy } = position.coords;
      const nowMs = position.timestamp || Date.now();
      const prev = prevFixRef.current;

      let speedKmh = 0;
      if (typeof speed === 'number' && !Number.isNaN(speed) && speed >= 0) {
        speedKmh = Math.round(speed * 3.6);
      } else if (prev && nowMs > prev.timestampMs) {
        const distMeters = haversineMeters(
          { lat: prev.latitude, lng: prev.longitude },
          { lat: latitude, lng: longitude }
        );
        const dtSec = (nowMs - prev.timestampMs) / 1000;
        if (dtSec >= 0.8 && distMeters >= 1.5 && distMeters < 1000) {
          speedKmh = Math.min(220, Math.round((distMeters / dtSec) * 3.6));
        }
      }

      let resolvedHeading = 0;
      if (typeof heading === 'number' && !Number.isNaN(heading) && heading >= 0) {
        resolvedHeading = Math.round(heading % 360);
      } else if (prev) {
        const distMeters = haversineMeters(
          { lat: prev.latitude, lng: prev.longitude },
          { lat: latitude, lng: longitude }
        );
        if (distMeters >= 2) {
          resolvedHeading = computeBearingDegrees(
            { lat: prev.latitude, lng: prev.longitude },
            { lat: latitude, lng: longitude }
          );
        } else {
          resolvedHeading = prev.heading;
        }
      }

      const fix: LiveGpsFix = {
        latitude,
        longitude,
        speedKmh,
        heading: resolvedHeading,
        accuracyMeters: Math.round(typeof accuracy === 'number' ? accuracy : 25),
        timestampMs: nowMs,
      };

      prevFixRef.current = fix;
      setLastFix(fix);
      setStatus('active');
      setErrorMessage(null);
      onFixRef.current?.(fix);
    };

    const handleError = (err: GeolocationPositionError) => {
      if (err.code === err.PERMISSION_DENIED) {
        setStatus('denied');
        const msg = (err.message || '').toLowerCase();
        if (msg.includes('permissions policy') || msg.includes('disallowed')) {
          setErrorMessage(
            'Akses GPS diblokir oleh izin iframe/preview. Refresh preview setelah izin aktif atau buka di tab browser penuh.'
          );
        } else {
          setErrorMessage(
            'Izin lokasi ditolak oleh browser/HP. Aktifkan izin Lokasi di ikon gembok/pengaturan browser lalu tekan Coba Lagi.'
          );
        }
      } else if (err.code === err.POSITION_UNAVAILABLE) {
        setStatus('unavailable');
        setErrorMessage('Sinyal satelit GPS sedang tidak tersedia.');
      } else {
        setErrorMessage('Menunggu sinyal GPS yang lebih stabil...');
      }
    };

    const watchId = navigator.geolocation.watchPosition(handleSuccess, handleError, {
      enableHighAccuracy: true,
      maximumAge: 2000,
      timeout: 12000,
    });

    return () => {
      navigator.geolocation.clearWatch(watchId);
    };
  }, [enabled, retryCounter]);

  return {
    status,
    lastFix,
    errorMessage,
    retryGps: requestSingleFix,
  };
}
