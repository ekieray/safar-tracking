import { AvatarPreset, TripCardTheme, TripHistoryRecord } from '../types/convoy';
import { projectTripMapViewport } from './geo';

interface RenderTripCardOptions {
  trip: TripHistoryRecord;
  theme: TripCardTheme;
  customPhotoUrl?: string;
  defaultScenicPhotoUrl: string;
  currentUserName?: string;
  currentUserAvatarPreset?: AvatarPreset;
  currentUserAvatarPhotoUrl?: string;
}

function loadImageElement(src: string): Promise<HTMLImageElement | null> {
  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = () => resolve(null);
    img.src = src;
  });
}

function drawRoundedRectPath(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number
) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + w - r, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + r);
  ctx.lineTo(x + w, y + h - r);
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  ctx.lineTo(x + r, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - r);
  ctx.lineTo(x, y + r);
  ctx.quadraticCurveTo(x, y, x + r, y);
  ctx.closePath();
}

function drawWrappedText(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  startY: number,
  maxWidth: number,
  lineHeight: number,
  maxLines: number
) {
  const words = text.split(' ');
  const lines: string[] = [];
  let current = '';

  for (const word of words) {
    const testLine = current ? `${current} ${word}` : word;
    if (ctx.measureText(testLine).width <= maxWidth) {
      current = testLine;
    } else {
      if (current) lines.push(current);
      current = word;
      if (lines.length === maxLines - 1) break;
    }
  }
  if (current && lines.length < maxLines) {
    lines.push(current);
  }
  if (lines.length === maxLines) {
    let last = lines[maxLines - 1];
    while (last.length > 3 && ctx.measureText(`${last}...`).width > maxWidth) {
      last = last.slice(0, -1);
    }
    if (text.length > lines.join(' ').length) {
      lines[maxLines - 1] = `${last}...`;
    }
  }

  lines.forEach((line, idx) => {
    ctx.fillText(line, x, startY + idx * lineHeight, maxWidth);
  });
}

export async function renderTripCardPngBlob({
  trip,
  theme,
  customPhotoUrl,
  defaultScenicPhotoUrl,
  currentUserName,
  currentUserAvatarPreset,
  currentUserAvatarPhotoUrl,
}: RenderTripCardOptions): Promise<Blob> {
  const width = 1080;
  const height = 1350;
  const canvas = document.createElement('canvas');
  canvas.width = width;
  canvas.height = height;
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('Canvas 2D context unavailable');
  }

  const isLight = theme === 'MINIMAL_LIGHT';
  const primaryColor = isLight ? '#0B1F16' : '#FFFFFF';
  const secondaryColor = isLight ? '#4D6659' : '#D1FAE5';
  const borderColor =
    theme === 'MINIMAL_LIGHT'
      ? '#D8E6DD'
      : theme === 'SATELLITE'
        ? '#059669'
        : '#1B3B2E';

  // 1. Background Layer
  if (theme === 'DARK_CARBON') {
    const grad = ctx.createLinearGradient(0, 0, width, height);
    grad.addColorStop(0, '#041A12');
    grad.addColorStop(0.55, '#07271C');
    grad.addColorStop(1, '#0C3527');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);

    ctx.strokeStyle = 'rgba(52, 211, 153, 0.08)';
    ctx.lineWidth = 2;
    for (let x = 0; x < width; x += 72) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += 72) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    const glow = ctx.createRadialGradient(
      width * 0.85,
      height * 0.16,
      10,
      width * 0.85,
      height * 0.16,
      width * 0.45
    );
    glow.addColorStop(0, 'rgba(217, 119, 6, 0.28)');
    glow.addColorStop(1, 'rgba(217, 119, 6, 0)');
    ctx.fillStyle = glow;
    ctx.fillRect(0, 0, width, height);
  } else if (theme === 'SATELLITE') {
    const grad = ctx.createLinearGradient(0, 0, width, height);
    grad.addColorStop(0, '#05231B');
    grad.addColorStop(0.5, '#0A3327');
    grad.addColorStop(1, '#061D17');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);

    ctx.strokeStyle = 'rgba(52, 211, 153, 0.09)';
    ctx.lineWidth = 2;
    for (let x = 0; x < width; x += 84) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += 84) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    ctx.strokeStyle = 'rgba(251, 191, 36, 0.14)';
    ctx.lineWidth = 3;
    for (let i = 1; i <= 6; i++) {
      ctx.beginPath();
      ctx.arc(width * 0.72, height * 0.42, width * 0.18 * i, 0, Math.PI * 2);
      ctx.stroke();
    }

    const satMapPad = 72;
    const satMapW = width - satMapPad * 2;
    const satMapH = 620;
    ctx.save();
    drawRoundedRectPath(ctx, satMapPad, 350, satMapW, satMapH, 36);
    ctx.fillStyle = 'rgba(4, 22, 16, 0.4)';
    ctx.fill();
    ctx.strokeStyle = 'rgba(167, 243, 208, 0.22)';
    ctx.lineWidth = 3;
    ctx.stroke();
    ctx.restore();
  } else if (theme === 'MINIMAL_LIGHT') {
    ctx.fillStyle = '#F7FAF8';
    ctx.fillRect(0, 0, width, height);
    ctx.strokeStyle = '#DCE9E0';
    ctx.lineWidth = 2;
    for (let x = 0; x < width; x += 84) {
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }
    for (let y = 0; y < height; y += 84) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }
  } else {
    const targetSrc = customPhotoUrl || defaultScenicPhotoUrl;
    const img = await loadImageElement(targetSrc);
    if (img) {
      const scale = Math.max(width / img.width, height / img.height);
      const sw = img.width * scale;
      const sh = img.height * scale;
      const ox = (width - sw) / 2;
      const oy = (height - sh) / 2;
      ctx.drawImage(img, ox, oy, sw, sh);
    } else {
      const grad = ctx.createLinearGradient(0, 0, 0, height);
      grad.addColorStop(0, '#064E3B');
      grad.addColorStop(0.5, '#B45309');
      grad.addColorStop(1, '#041A12');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, width, height);
    }
    const scrim = ctx.createLinearGradient(0, 0, 0, height);
    scrim.addColorStop(0, 'rgba(3, 24, 16, 0.62)');
    scrim.addColorStop(0.45, 'rgba(3, 24, 16, 0.38)');
    scrim.addColorStop(1, 'rgba(3, 24, 16, 0.90)');
    ctx.fillStyle = scrim;
    ctx.fillRect(0, 0, width, height);
  }

  const pad = 72;

  // 2. Top Header: Safar Tracking Mark & Mutawwif/Jamaah Avatar
  drawRoundedRectPath(ctx, pad, pad, 72, 72, 20);
  ctx.fillStyle = '#047857';
  ctx.fill();

  // Crescent icon in badge
  ctx.fillStyle = '#FDE68A';
  ctx.beginPath();
  ctx.arc(pad + 38, pad + 36, 18, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = '#047857';
  ctx.beginPath();
  ctx.arc(pad + 45, pad + 32, 15, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = primaryColor;
  ctx.font = '800 38px "Plus Jakarta Sans", system-ui, sans-serif';
  ctx.fillText('Safar Tracking', pad + 92, pad + 36);

  ctx.fillStyle = secondaryColor;
  ctx.font = '600 24px "JetBrains Mono", monospace';
  ctx.fillText(trip.roomId, pad + 92, pad + 68);

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

  {
    const avRadius = 40;
    const avCx = width - pad - avRadius;
    const avCy = pad + 36;

    const avatarImg = headerAvatarPhotoUrl
      ? await loadImageElement(headerAvatarPhotoUrl)
      : null;

    ctx.save();
    ctx.beginPath();
    ctx.arc(avCx, avCy, avRadius, 0, Math.PI * 2);
    ctx.clip();
    if (avatarImg) {
      ctx.drawImage(
        avatarImg,
        avCx - avRadius,
        avCy - avRadius,
        avRadius * 2,
        avRadius * 2
      );
    } else {
      ctx.fillStyle =
        headerAvatarPreset === 'RIDER_CAPTAIN'
          ? '#B45309'
          : headerAvatarPreset === 'PILOT_CLASSIC'
            ? '#0F766E'
            : '#047857';
      ctx.fillRect(avCx - avRadius, avCy - avRadius, avRadius * 2, avRadius * 2);
      ctx.fillStyle = '#FFFFFF';
      ctx.font = '700 34px "Plus Jakarta Sans", system-ui, sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(
        (headerDisplayName[0] || 'J').toUpperCase(),
        avCx,
        avCy + 2
      );
    }
    ctx.restore();

    ctx.beginPath();
    ctx.arc(avCx, avCy, avRadius, 0, Math.PI * 2);
    ctx.strokeStyle = '#D97706';
    ctx.lineWidth = 5;
    ctx.stroke();

    ctx.textAlign = 'right';
    ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = primaryColor;
    ctx.font = '700 28px "Plus Jakarta Sans", system-ui, sans-serif';
    ctx.fillText(headerDisplayName, avCx - avRadius - 20, pad + 32, width - pad * 2 - 360);

    ctx.fillStyle = isLight ? '#047857' : '#FDE68A';
    ctx.font = '600 22px "Plus Jakarta Sans", system-ui, sans-serif';
    ctx.fillText(headerRoleLabel, avCx - avRadius - 20, pad + 64);
    ctx.textAlign = 'left';
  }

  // 3. Tour Title & Date
  ctx.fillStyle = primaryColor;
  ctx.font = '700 52px "Plus Jakarta Sans", system-ui, sans-serif';
  ctx.fillText(trip.tourTitle, pad, 248, width - pad * 2);

  ctx.fillStyle = secondaryColor;
  ctx.font = '500 28px "Plus Jakarta Sans", system-ui, sans-serif';
  ctx.fillText(
    `${trip.dateLabel} · ${trip.destinationName}`,
    pad,
    296,
    width - pad * 2
  );

  // 4. Polyline Map Area (Web Mercator aspect-preserving projection)
  const mapLeft = pad;
  const mapTop = 350;
  const mapWidth = width - pad * 2;
  const mapHeight = 620;
  const routeProjection = projectTripMapViewport(
    trip.waypoints,
    trip.visitedCheckpoints,
    mapWidth,
    mapHeight,
    0.13
  );

  if (routeProjection.waypoints.length >= 2) {
    ctx.save();
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    ctx.beginPath();
    ctx.moveTo(
      mapLeft + routeProjection.waypoints[0].x,
      mapTop + routeProjection.waypoints[0].y
    );
    for (let i = 1; i < routeProjection.waypoints.length; i++) {
      ctx.lineTo(
        mapLeft + routeProjection.waypoints[i].x,
        mapTop + routeProjection.waypoints[i].y
      );
    }

    ctx.strokeStyle = isLight
      ? 'rgba(16, 185, 129, 0.42)'
      : 'rgba(217, 119, 6, 0.36)';
    ctx.lineWidth = 34;
    ctx.stroke();

    ctx.strokeStyle = isLight ? '#047857' : '#34D399';
    ctx.lineWidth = 14;
    ctx.stroke();
    ctx.restore();
  }

  const startPt = routeProjection.waypoints[0];
  if (startPt) {
    const cx = mapLeft + startPt.x;
    const cy = mapTop + startPt.y;
    ctx.beginPath();
    ctx.arc(cx, cy, 22, 0, Math.PI * 2);
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(cx, cy, 14, 0, Math.PI * 2);
    ctx.fillStyle = '#D97706';
    ctx.fill();
  }

  const endPt = routeProjection.waypoints[routeProjection.waypoints.length - 1];
  if (endPt) {
    const cx = mapLeft + endPt.x;
    const cy = mapTop + endPt.y;
    ctx.beginPath();
    ctx.arc(cx, cy, 22, 0, Math.PI * 2);
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(cx, cy, 14, 0, Math.PI * 2);
    ctx.fillStyle = '#047857';
    ctx.fill();
  }

  routeProjection.checkpoints.forEach((projectedCp) => {
    const cx = mapLeft + projectedCp.x;
    const cy = mapTop + projectedCp.y;
    ctx.beginPath();
    ctx.arc(cx, cy, 18, 0, Math.PI * 2);
    ctx.fillStyle = '#FFFFFF';
    ctx.fill();
    ctx.beginPath();
    ctx.arc(cx, cy, 13, 0, Math.PI * 2);
    ctx.fillStyle = '#0F766E';
    ctx.fill();
  });

  // 5. Achievement Metrics Grid
  const colWidth = (width - pad * 2) / 4;
  const stats = [
    { label: 'Jarak', value: `${trip.distanceKm.toFixed(1)} km` },
    { label: 'Durasi', value: trip.durationLabel },
    { label: 'Rata-rata', value: `${trip.avgSpeedKmh} km/j` },
    { label: 'Pos Kumpul', value: `${trip.visitedCheckpoints.length} titik` },
  ];

  stats.forEach((item, idx) => {
    const x = pad + colWidth * idx;
    ctx.fillStyle = secondaryColor;
    ctx.font = '500 25px "Plus Jakarta Sans", system-ui, sans-serif';
    ctx.fillText(item.label, x, 1062);

    ctx.fillStyle = primaryColor;
    ctx.font = '700 40px "JetBrains Mono", monospace';
    ctx.fillText(item.value, x, 1116);
  });

  // Divider & Footer
  ctx.strokeStyle = borderColor;
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(pad, 1166);
  ctx.lineTo(width - pad, 1166);
  ctx.stroke();

  ctx.fillStyle = secondaryColor;
  ctx.font = '500 25px "Plus Jakarta Sans", system-ui, sans-serif';
  const participantNames = trip.participants
    .map((p) => p.fullName.trim())
    .filter(Boolean);
  const membersText = `${Math.max(1, participantNames.length)} Jamaah Ikut: ${
    participantNames.length > 0 ? participantNames.join(', ') : headerDisplayName
  }`;
  drawWrappedText(ctx, membersText, pad, 1214, width - pad * 2, 34, 3);

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) resolve(blob);
        else reject(new Error('Gagal merender kartu PNG'));
      },
      'image/png',
      1.0
    );
  });
}
