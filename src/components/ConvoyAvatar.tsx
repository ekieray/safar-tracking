import { AvatarPreset } from '../types/convoy';

export interface AvatarPresetOption {
  id: AvatarPreset;
  label: string;
  gradient: string;
}

export const AVATAR_PRESET_OPTIONS: AvatarPresetOption[] = [
  {
    id: 'HELMET_TOURING',
    label: 'Jamaah Ihram',
    gradient: 'from-[#047857] to-[#10B981]',
  },
  {
    id: 'HELMET_SPORT',
    label: 'Jamaah Madinah',
    gradient: 'from-[#065F46] to-[#14B8A6]',
  },
  {
    id: 'RIDER_CAPTAIN',
    label: 'Mutawwif Utama',
    gradient: 'from-[#B45309] to-[#F59E0B]',
  },
  {
    id: 'PILOT_CLASSIC',
    label: 'Pendamping',
    gradient: 'from-[#0F766E] to-[#34D399]',
  },
  {
    id: 'RALLY_PRO',
    label: 'Ketua Kloter',
    gradient: 'from-[#064E3B] to-[#D97706]',
  },
  {
    id: 'STANDARD_USER',
    label: 'Jamaah Umum',
    gradient: 'from-[#334155] to-[#64748B]',
  },
];

interface ConvoyAvatarProps {
  preset?: AvatarPreset;
  photoUrl?: string;
  name: string;
  sizeClass?: string;
  ringClass?: string;
}

export function ConvoyAvatar({
  preset = 'HELMET_TOURING',
  photoUrl,
  name,
  sizeClass = 'w-9 h-9',
  ringClass = 'ring-2 ring-white dark:ring-[#0C1E17]',
}: ConvoyAvatarProps) {
  const option =
    AVATAR_PRESET_OPTIONS.find((o) => o.id === preset) ??
    AVATAR_PRESET_OPTIONS[0];

  if (photoUrl) {
    return (
      <div
        className={`${sizeClass} rounded-full overflow-hidden shrink-0 ${ringClass} bg-[#D8E6DD] shadow-xs`}
      >
        <img
          src={photoUrl}
          alt={`Avatar ${name}`}
          className="w-full h-full object-cover"
        />
      </div>
    );
  }

  return (
    <div
      className={`${sizeClass} rounded-full shrink-0 ${ringClass} bg-gradient-to-br ${option.gradient} flex items-center justify-center shadow-xs overflow-hidden`}
      title={option.label}
    >
      {preset === 'HELMET_TOURING' && (
        <svg viewBox="0 0 36 36" className="w-[72%] h-[72%]" aria-hidden="true">
          {/* Peci / Ihram portrait */}
          <circle cx="18" cy="14.5" r="5" fill="#FFFFFF" />
          <path d="M12.5 11.5C12.5 9.2 14.8 8 18 8C21.2 8 23.5 9.2 23.5 11.5H12.5Z" fill="#064E3B" />
          <path d="M8.5 28.5C10 22.8 13.5 20.5 18 20.5C22.5 20.5 26 22.8 27.5 28.5Z" fill="#FFFFFF" />
          <path d="M15 21L18 25.5L21 21" fill="none" stroke="#047857" strokeWidth="1.8" strokeLinecap="round" />
        </svg>
      )}

      {preset === 'HELMET_SPORT' && (
        <svg viewBox="0 0 36 36" className="w-[72%] h-[72%]" aria-hidden="true">
          {/* Masjid Nabawi dome + crescent emblem */}
          <path
            d="M9 26V19C9 14.5 13 11 18 9C23 11 27 14.5 27 19V26H9Z"
            fill="#FFFFFF"
          />
          <rect x="7" y="25.5" width="22" height="2.8" rx="1.4" fill="#A7F3D0" />
          <path d="M18 5.2V8.8" stroke="#FDE68A" strokeWidth="1.8" strokeLinecap="round" />
          <circle cx="18" cy="19.5" r="2.6" fill="#065F46" />
        </svg>
      )}

      {preset === 'RIDER_CAPTAIN' && (
        <svg viewBox="0 0 36 36" className="w-[72%] h-[72%]" aria-hidden="true">
          {/* Mutawwif Ka'bah + star emblem */}
          <rect x="10" y="12" width="16" height="15" rx="2.5" fill="#1E293B" />
          <rect x="10" y="15.5" width="16" height="2.6" fill="#FACC15" />
          <path
            d="M18 4.8L19.4 7.5L22.3 7.9L20.1 9.9L20.6 12.8L18 11.4L15.4 12.8L15.9 9.9L13.7 7.9L16.6 7.5Z"
            fill="#FFFFFF"
          />
        </svg>
      )}

      {preset === 'PILOT_CLASSIC' && (
        <svg viewBox="0 0 36 36" className="w-[72%] h-[72%]" aria-hidden="true">
          {/* Pendamping compass/shield */}
          <path
            d="M18 6.5L27 10.5V18C27 23.6 23.2 27.8 18 29.5C12.8 27.8 9 23.6 9 18V10.5L18 6.5Z"
            fill="#FFFFFF"
          />
          <path d="M18 11L20.5 18L18 23.5L15.5 18L18 11Z" fill="#0F766E" />
          <circle cx="18" cy="18" r="1.4" fill="#FBBF24" />
        </svg>
      )}

      {preset === 'RALLY_PRO' && (
        <svg viewBox="0 0 36 36" className="w-[72%] h-[72%]" aria-hidden="true">
          {/* Ketua Kloter crescent & 8-point star */}
          <rect x="10" y="10" width="16" height="16" rx="2" fill="none" stroke="#FDE68A" strokeWidth="1.8" />
          <rect x="10" y="10" width="16" height="16" rx="2" fill="none" stroke="#FFFFFF" strokeWidth="1.8" transform="rotate(45 18 18)" />
          <circle cx="18" cy="18" r="4" fill="#FFFFFF" />
        </svg>
      )}

      {preset === 'STANDARD_USER' && (
        <svg viewBox="0 0 36 36" className="w-[70%] h-[70%]" aria-hidden="true">
          <circle cx="18" cy="13.5" r="5" fill="#FFFFFF" />
          <path
            d="M8.5 28C9.8 22.6 13.4 20.2 18 20.2C22.6 20.2 26.2 22.6 27.5 28"
            fill="#FFFFFF"
          />
        </svg>
      )}
    </div>
  );
}
