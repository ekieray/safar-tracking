export interface ResolvedLocationZone {
  timeZoneId: string | null;
  fixedOffsetMinutes: number | null;
  zoneCode: string;
  regionLabel: string;
  isFromCoordinates: boolean;
}

export interface DualClockSnapshot {
  localTime: string;
  localDateLabel: string;
  localZoneCode: string;
  localRegionLabel: string;
  wibTime: string;
  wibDateLabel: string;
  diffToWibLabel: string;
  isSameAsWib: boolean;
  isFromCoordinates: boolean;
}

const WIB_OFFSET_MINUTES = 7 * 60;

export function resolveLocationTimeZone(
  latitude?: number | null,
  longitude?: number | null
): ResolvedLocationZone {
  const hasValidCoords =
    typeof latitude === 'number' &&
    typeof longitude === 'number' &&
    Number.isFinite(latitude) &&
    Number.isFinite(longitude) &&
    !(Math.abs(latitude) < 0.001 && Math.abs(longitude) < 0.001);

  if (!hasValidCoords) {
    let deviceZone: string | null = null;
    try {
      deviceZone = Intl.DateTimeFormat().resolvedOptions().timeZone || null;
    } catch {
      deviceZone = null;
    }
    const offsetMins = -new Date().getTimezoneOffset();
    const zoneCode =
      offsetMins === WIB_OFFSET_MINUTES
        ? 'WIB'
        : offsetMins === 8 * 60
          ? 'WITA'
          : offsetMins === 9 * 60
            ? 'WIT'
            : offsetMins === 3 * 60
              ? 'WSA'
              : formatUtcOffsetLabel(offsetMins);

    return {
      timeZoneId: deviceZone,
      fixedOffsetMinutes: deviceZone ? null : offsetMins,
      zoneCode,
      regionLabel: 'Lokasi Perangkat',
      isFromCoordinates: false,
    };
  }

  const lat = latitude as number;
  const lng = longitude as number;

  // 1. Arab Saudi (Makkah, Madinah, Jeddah, Riyadh, dll.)
  if (lat >= 16.0 && lat <= 32.2 && lng >= 34.4 && lng <= 55.7) {
    let city = 'Arab Saudi';
    if (Math.abs(lat - 21.4225) <= 0.45 && Math.abs(lng - 39.8262) <= 0.45) {
      city = 'Makkah';
    } else if (Math.abs(lat - 24.4672) <= 0.45 && Math.abs(lng - 39.6111) <= 0.45) {
      city = 'Madinah';
    } else if (Math.abs(lat - 21.5433) <= 0.45 && Math.abs(lng - 39.1728) <= 0.45) {
      city = 'Jeddah';
    }
    return {
      timeZoneId: 'Asia/Riyadh',
      fixedOffsetMinutes: 3 * 60,
      zoneCode: 'WSA',
      regionLabel: city,
      isFromCoordinates: true,
    };
  }

  // 2. Malaysia Semenanjung & Singapura
  if (lat >= 1.23 && lat <= 7.0 && lng >= 99.5 && lng <= 104.6) {
    const isSingapore = lat <= 1.48 && lng >= 103.55 && lng <= 104.12;
    return {
      timeZoneId: isSingapore ? 'Asia/Singapore' : 'Asia/Kuala_Lumpur',
      fixedOffsetMinutes: 8 * 60,
      zoneCode: isSingapore ? 'SGT' : 'MYT',
      regionLabel: isSingapore ? 'Singapura' : 'Malaysia',
      isFromCoordinates: true,
    };
  }

  // 3. Indonesia (WIB / WITA / WIT)
  if (lat >= -11.2 && lat <= 6.2 && lng >= 94.9 && lng <= 141.1) {
    if (lng >= 125.7 && lat <= 2.5) {
      return {
        timeZoneId: 'Asia/Jayapura',
        fixedOffsetMinutes: 9 * 60,
        zoneCode: 'WIT',
        regionLabel: 'Indonesia Timur',
        isFromCoordinates: true,
      };
    }
    if (lng >= 114.55 || (lat >= -4.2 && lat <= 4.3 && lng >= 114.3)) {
      return {
        timeZoneId: 'Asia/Makassar',
        fixedOffsetMinutes: 8 * 60,
        zoneCode: 'WITA',
        regionLabel: 'Indonesia Tengah',
        isFromCoordinates: true,
      };
    }
    return {
      timeZoneId: 'Asia/Jakarta',
      fixedOffsetMinutes: WIB_OFFSET_MINUTES,
      zoneCode: 'WIB',
      regionLabel: 'Indonesia Barat',
      isFromCoordinates: true,
    };
  }

  // 4. UEA (Dubai / Abu Dhabi)
  if (lat >= 22.6 && lat <= 26.2 && lng >= 51.5 && lng <= 56.5) {
    return {
      timeZoneId: 'Asia/Dubai',
      fixedOffsetMinutes: 4 * 60,
      zoneCode: 'GST',
      regionLabel: 'UEA',
      isFromCoordinates: true,
    };
  }

  // 5. Mesir
  if (lat >= 22.0 && lat <= 31.8 && lng >= 24.7 && lng < 34.4) {
    return {
      timeZoneId: 'Africa/Cairo',
      fixedOffsetMinutes: 2 * 60,
      zoneCode: 'EET',
      regionLabel: 'Mesir',
      isFromCoordinates: true,
    };
  }

  // 6. Yordania / Palestina / Syam
  if (lat >= 29.2 && lat <= 33.4 && lng >= 34.2 && lng < 39.4) {
    return {
      timeZoneId: 'Asia/Amman',
      fixedOffsetMinutes: 3 * 60,
      zoneCode: 'AST',
      regionLabel: 'Yordania / Syam',
      isFromCoordinates: true,
    };
  }

  // 7. Turki
  if (lat >= 35.8 && lat <= 42.2 && lng >= 25.6 && lng <= 44.8) {
    return {
      timeZoneId: 'Europe/Istanbul',
      fixedOffsetMinutes: 3 * 60,
      zoneCode: 'TRT',
      regionLabel: 'Turki',
      isFromCoordinates: true,
    };
  }

  // 8. Global Longitude Fallback
  const estHours = Math.max(-12, Math.min(14, Math.round(lng / 15)));
  const estOffsetMinutes = estHours * 60;
  return {
    timeZoneId: null,
    fixedOffsetMinutes: estOffsetMinutes,
    zoneCode: formatUtcOffsetLabel(estOffsetMinutes),
    regionLabel: 'Zona Lokasi',
    isFromCoordinates: true,
  };
}

function formatUtcOffsetLabel(offsetMinutes: number): string {
  if (offsetMinutes === 0) return 'UTC';
  const sign = offsetMinutes > 0 ? '+' : '-';
  const absMins = Math.abs(offsetMinutes);
  const hours = Math.floor(absMins / 60);
  const mins = absMins % 60;
  return mins === 0
    ? `UTC${sign}${hours}`
    : `UTC${sign}${hours}:${String(mins).padStart(2, '0')}`;
}

function formatWithIanaOrOffset(
  nowMs: number,
  timeZoneId: string | null,
  fallbackOffsetMinutes: number
): { time: string; dateLabel: string; resolvedOffsetMinutes: number } {
  if (timeZoneId) {
    try {
      const timeParts = new Intl.DateTimeFormat('en-GB', {
        timeZone: timeZoneId,
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hourCycle: 'h23',
      }).formatToParts(new Date(nowMs));

      const h = timeParts.find((p) => p.type === 'hour')?.value ?? '00';
      const m = timeParts.find((p) => p.type === 'minute')?.value ?? '00';
      const s = timeParts.find((p) => p.type === 'second')?.value ?? '00';
      const normalizedHour = h === '24' ? '00' : h;

      const dateParts = new Intl.DateTimeFormat('id-ID', {
        timeZone: timeZoneId,
        weekday: 'short',
        day: '2-digit',
        month: 'short',
      }).format(new Date(nowMs));

      // Compute exact offset minutes in this timezone at nowMs
      const utcIsoParts = new Intl.DateTimeFormat('en-CA', {
        timeZone: timeZoneId,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hourCycle: 'h23',
      }).formatToParts(new Date(nowMs));

      const y = Number(utcIsoParts.find((p) => p.type === 'year')?.value ?? 1970);
      const mo = Number(utcIsoParts.find((p) => p.type === 'month')?.value ?? 1);
      const d = Number(utcIsoParts.find((p) => p.type === 'day')?.value ?? 1);
      const hr = Number(utcIsoParts.find((p) => p.type === 'hour')?.value ?? 0) % 24;
      const mn = Number(utcIsoParts.find((p) => p.type === 'minute')?.value ?? 0);
      const sc = Number(utcIsoParts.find((p) => p.type === 'second')?.value ?? 0);
      const zonedAsUtcMs = Date.UTC(y, mo - 1, d, hr, mn, sc);
      const nowSecMs = Math.floor(nowMs / 1000) * 1000;
      const resolvedOffsetMinutes = Math.round((zonedAsUtcMs - nowSecMs) / 60_000);

      return {
        time: `${normalizedHour}:${m}:${s}`,
        dateLabel: dateParts,
        resolvedOffsetMinutes,
      };
    } catch {
      // Fall through to offset math
    }
  }

  const shifted = new Date(nowMs + fallbackOffsetMinutes * 60_000);
  const hh = String(shifted.getUTCHours()).padStart(2, '0');
  const mm = String(shifted.getUTCMinutes()).padStart(2, '0');
  const ss = String(shifted.getUTCSeconds()).padStart(2, '0');
  const days = ['Min', 'Sen', 'Sel', 'Rab', 'Kam', 'Jum', 'Sab'];
  const months = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'Mei',
    'Jun',
    'Jul',
    'Agu',
    'Sep',
    'Okt',
    'Nov',
    'Des',
  ];
  const dateLabel = `${days[shifted.getUTCDay()]}, ${String(shifted.getUTCDate()).padStart(2, '0')} ${months[shifted.getUTCMonth()]}`;
  return {
    time: `${hh}:${mm}:${ss}`,
    dateLabel,
    resolvedOffsetMinutes: fallbackOffsetMinutes,
  };
}

export function buildDualClockSnapshot(
  nowMs: number,
  latitude?: number | null,
  longitude?: number | null
): DualClockSnapshot {
  const resolved = resolveLocationTimeZone(latitude, longitude);
  const localFmt = formatWithIanaOrOffset(
    nowMs,
    resolved.timeZoneId,
    resolved.fixedOffsetMinutes ?? WIB_OFFSET_MINUTES
  );
  const wibFmt = formatWithIanaOrOffset(nowMs, 'Asia/Jakarta', WIB_OFFSET_MINUTES);

  const diffMins = localFmt.resolvedOffsetMinutes - wibFmt.resolvedOffsetMinutes;
  const isSameAsWib = diffMins === 0;
  let diffToWibLabel = '= WIB';
  if (!isSameAsWib) {
    const sign = diffMins > 0 ? '+' : '-';
    const abs = Math.abs(diffMins);
    const hrs = Math.floor(abs / 60);
    const rem = abs % 60;
    diffToWibLabel = rem === 0 ? `${sign}${hrs}j dari WIB` : `${sign}${hrs}j${rem}m`;
  }

  return {
    localTime: localFmt.time,
    localDateLabel: localFmt.dateLabel,
    localZoneCode: resolved.zoneCode,
    localRegionLabel: resolved.regionLabel,
    wibTime: wibFmt.time,
    wibDateLabel: wibFmt.dateLabel,
    diffToWibLabel,
    isSameAsWib,
    isFromCoordinates: resolved.isFromCoordinates,
  };
}
