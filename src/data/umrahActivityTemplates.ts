import { RoomActivityItem } from '../types/convoy';

export interface UmrahActivityTemplate {
  key: string;
  title: string;
  timeLabel: string;
  locationNote: string;
}

export const UMRAH_ACTIVITY_QUICK_TEMPLATES: UmrahActivityTemplate[] = [
  {
    key: 'tpl-kumpul-lobi',
    title: 'Kumpul Lobi Hotel & Absen Rombongan',
    timeLabel: 'Persiapan',
    locationNote: 'Lobi Hotel / Titik Kumpul Awal',
  },
  {
    key: 'tpl-miqat-niat',
    title: 'Pakai Kain Ihram & Niat Umroh di Miqat',
    timeLabel: 'Berangkat',
    locationNote: 'Masjid Miqat (Bir Ali / Tan’im / Yalamlam)',
  },
  {
    key: 'tpl-tawaf',
    title: 'Tawaf Umroh 7 Putaran Bersama',
    timeLabel: 'Masjidil Haram',
    locationNote: 'Pelataran Ka’bah · Mulai Hajar Aswad',
  },
  {
    key: 'tpl-maqam-zamzam',
    title: 'Sholat Sunnah Maqam Ibrahim & Minum Zamzam',
    timeLabel: 'Setelah Tawaf',
    locationNote: 'Belakang Maqam Ibrahim',
  },
  {
    key: 'tpl-sai',
    title: 'Sa’i Shafa ke Marwah 7 Kali',
    timeLabel: 'Mas’a',
    locationNote: 'Lintasan Shafa – Marwah',
  },
  {
    key: 'tpl-tahallul',
    title: 'Tahallul & Kumpul Pintu Marwah',
    timeLabel: 'Penutup',
    locationNote: 'Bukit Marwah · Titik Kumpul Rombongan',
  },
  {
    key: 'tpl-ziarah-makkah',
    title: 'Ziarah Jabal Nur, Jabal Tsur & Arafah',
    timeLabel: 'Ziarah',
    locationNote: 'Bus Rombongan Makkah',
  },
  {
    key: 'tpl-raudhah',
    title: 'Kunjungan Raudhah & Salam Makam Rasulullah',
    timeLabel: 'Madinah',
    locationNote: 'Masjid Nabawi · Pintu Tasreh',
  },
];

export function createStarterRoomUmrahActivities(
  createdByUid = 'leader'
): RoomActivityItem[] {
  return UMRAH_ACTIVITY_QUICK_TEMPLATES.slice(0, 4).map((tpl, idx) => ({
    id: `act-init-${idx + 1}`,
    title: tpl.title,
    timeLabel: tpl.timeLabel,
    locationNote: tpl.locationNote,
    orderIndex: idx,
    createdByUid,
  }));
}
