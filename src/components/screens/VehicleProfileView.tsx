import { useRef, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  BadgeInfo,
  Bus,
  FileText,
  Footprints,
  Hotel,
  ImagePlus,
  Info,
  Phone,
  User,
  X,
} from 'lucide-react';
import { UserRole, VehicleProfile, VehicleType } from '../../types/convoy';
import { compressAvatarImage } from '../../utils/imageCompress';
import { AVATAR_PRESET_OPTIONS, ConvoyAvatar } from '../ConvoyAvatar';

interface VehicleProfileViewProps {
  selectedRole: UserRole;
  profile: VehicleProfile;
  onUpdateProfile: (patch: Partial<VehicleProfile>) => void;
  onBack: () => void;
  onContinue: () => void;
  onOpenAboutApp: () => void;
}

export function VehicleProfileView({
  selectedRole,
  profile,
  onUpdateProfile,
  onBack,
  onContinue,
  onOpenAboutApp,
}: VehicleProfileViewProps) {
  const [showErrors, setShowErrors] = useState(false);
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  const handlePhotoUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    try {
      const compressed = await compressAvatarImage(file);
      onUpdateProfile({ avatarPhotoUrl: compressed });
    } catch {
      // Fallback ignored
    }
  };

  const nameError = showErrors && profile.fullName.trim().length < 2;
  const plateError = showErrors && profile.licensePlate.trim().length < 3;
  const contactError = showErrors && profile.emergencyContact.trim().length < 8;

  const isValid =
    profile.fullName.trim().length >= 2 &&
    profile.licensePlate.trim().length >= 3 &&
    profile.emergencyContact.trim().length >= 8;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (isValid) {
      onContinue();
    } else {
      setShowErrors(true);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col min-h-full bg-[#F4F8F5] dark:bg-[#06130E] text-[#0B1F16] dark:text-[#ECFDF5]"
    >
      {/* Material 3 Top App Bar */}
      <div className="sticky top-0 z-10 flex items-center gap-2 px-3 py-2.5 bg-white dark:bg-[#0C1E17] border-b border-[#BDD6C7] dark:border-[#244E3D] shadow-2xs">
        <button
          type="button"
          onClick={onBack}
          aria-label="Kembali"
          className="min-w-[44px] min-h-[44px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="min-w-0 flex-1">
          <h2 className="text-base font-semibold leading-tight truncate">
            Identitas Jamaah &amp; Rombongan
          </h2>
          <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] truncate">
            Peran: {selectedRole === 'TOUR_LEADER' ? 'Mutawwif / Pembimbing' : 'Jamaah Umroh'}
          </p>
        </div>
        <button
          type="button"
          onClick={onOpenAboutApp}
          aria-label="Tentang Aplikasi"
          className="min-w-[44px] min-h-[44px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
        >
          <Info className="w-5 h-5" />
        </button>
      </div>

      {/* Form Body */}
      <div className="flex-1 px-4 py-4 space-y-4">
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border-[1.5px] border-[#BDD6C7] dark:border-[#244E3D] px-3.5 py-2.5 shadow-sm">
          <p className="text-xs sm:text-sm text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
            Lengkapi foto profil dan identitas jamaah (maktab/hotel, nomor gelang/kloter, &amp; kontak darurat) agar mudah dipantau Mutawwif di peta.
          </p>
        </div>

        {/* Foto Profil & Pilihan Avatar Bawaan */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border-[1.5px] border-[#BDD6C7] dark:border-[#244E3D] p-4 space-y-3.5 shadow-sm">
          <div className="flex items-center gap-3.5">
            <ConvoyAvatar
              preset={profile.avatarPreset}
              photoUrl={profile.avatarPhotoUrl}
              name={profile.fullName || 'Jamaah'}
              sizeClass="w-16 h-16"
              ringClass="ring-2 ring-[#047857] dark:ring-[#34D399]"
            />
            <div className="min-w-0 flex-1 space-y-2">
              <p className="text-xs font-bold text-[#0B1F16] dark:text-[#ECFDF5]">
                Foto Profil / Avatar Jamaah
              </p>
              <div className="flex flex-wrap items-center gap-2">
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handlePhotoUpload}
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  className="px-3 py-1.5 rounded-xl border border-[#047857] dark:border-[#34D399] text-[#047857] dark:text-[#6EE7B7] text-xs font-semibold flex items-center gap-1.5 hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 cursor-pointer"
                >
                  <ImagePlus className="w-3.5 h-3.5" />
                  <span>Pilih Foto Galeri</span>
                </button>
                {profile.avatarPhotoUrl && (
                  <button
                    type="button"
                    onClick={() => onUpdateProfile({ avatarPhotoUrl: undefined })}
                    className="px-2.5 py-1.5 rounded-xl text-xs font-semibold text-[#DC2626] hover:bg-[#FEE2E2] dark:hover:bg-[#450A0A]/40 flex items-center gap-1 cursor-pointer"
                  >
                    <X className="w-3.5 h-3.5" />
                    <span>Hapus</span>
                  </button>
                )}
              </div>
            </div>
          </div>

          <div className="space-y-1.5">
            <p className="text-[11px] font-semibold text-[#4D6659] dark:text-[#9CC2AE]">
              Atau pilih ikon karakter jamaah bawaan:
            </p>
            <div className="grid grid-cols-3 gap-2">
              {AVATAR_PRESET_OPTIONS.map((opt) => {
                const active =
                  !profile.avatarPhotoUrl && profile.avatarPreset === opt.id;
                return (
                  <button
                    key={opt.id}
                    type="button"
                    onClick={() =>
                      onUpdateProfile({
                        avatarPreset: opt.id,
                        avatarPhotoUrl: undefined,
                      })
                    }
                    className={`p-2 rounded-[14px] border flex flex-col items-center gap-1.5 transition-colors cursor-pointer ${
                      active
                        ? 'bg-[#ECFDF5] dark:bg-[#064E3B]/55 border-[#047857] text-[#047857] dark:text-[#A7F3D0]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] border-[#D8E6DD] dark:border-[#1B3B2E]'
                    }`}
                  >
                    <ConvoyAvatar
                      preset={opt.id}
                      name={opt.label}
                      sizeClass="w-9 h-9"
                      ringClass="ring-1 ring-white/80"
                    />
                    <span className="text-[10px] font-semibold truncate max-w-full">
                      {opt.label}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>

        {/* Frame Identitas & Moda Rombongan */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border-[1.5px] border-[#BDD6C7] dark:border-[#244E3D] p-4 space-y-3.5 shadow-sm">
          <p className="text-xs font-bold uppercase tracking-wider text-[#047857] dark:text-[#6EE7B7]">
            Identitas Jamaah &amp; Rombongan
          </p>

          {/* Nama Lengkap */}
          <div>
            <label
              htmlFor="fullName"
              className="block text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5] mb-1.5"
            >
              Nama Lengkap Jamaah
            </label>
            <div className="relative">
              <User className="w-4 h-4 text-[#047857] dark:text-[#6EE7B7] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                id="fullName"
                type="text"
                value={profile.fullName}
                onChange={(e) => onUpdateProfile({ fullName: e.target.value })}
                placeholder="Contoh: Ahmad Fauzi"
                className={`w-full min-h-[46px] pl-10 pr-3.5 rounded-[14px] bg-[#F8FBF9] dark:bg-[#0F261D] border-[1.5px] text-sm text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#6B8A7A] dark:placeholder:text-[#86B09A] shadow-2xs outline-none transition-colors ${
                  nameError
                    ? 'border-[#DC2626] focus:border-[#DC2626]'
                    : 'border-[#9EC5B0] dark:border-[#2B5D49] focus:border-[#047857] focus:ring-2 focus:ring-[#047857]'
                }`}
              />
            </div>
            {nameError && (
              <p className="mt-1 text-xs text-[#DC2626]">
                Nama lengkap minimal 2 karakter
              </p>
            )}
          </div>

          {/* Moda Pergerakan Segmented Control */}
          <div>
            <span className="block text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5] mb-1.5">
              Moda Pergerakan
            </span>
            <div className="grid grid-cols-2 rounded-[14px] border-[1.5px] border-[#9EC5B0] dark:border-[#2B5D49] overflow-hidden bg-[#F8FBF9] dark:bg-[#0F261D]">
              {(['MOBIL', 'MOTOR'] as VehicleType[]).map((type, idx) => {
                const active = profile.vehicleType === type;
                return (
                  <button
                    key={type}
                    type="button"
                    onClick={() => onUpdateProfile({ vehicleType: type })}
                    className={`min-h-[44px] flex items-center justify-center gap-2 text-sm font-medium transition-colors cursor-pointer ${
                      idx === 0
                        ? 'border-r border-[#9EC5B0] dark:border-[#2B5D49]'
                        : ''
                    } ${
                      active
                        ? 'bg-[#047857] text-white'
                        : 'text-[#2D4A3D] dark:text-[#D1FAE5] hover:bg-[#ECFDF5] dark:hover:bg-[#132E24]'
                    }`}
                  >
                    {type === 'MOBIL' ? (
                      <Bus className="w-4 h-4 shrink-0" />
                    ) : (
                      <Footprints className="w-4 h-4 shrink-0" />
                    )}
                    <span>{type === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'}</span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Hotel / Maktab / Bus Rombongan */}
          <div>
            <label
              htmlFor="vehicleDetail"
              className="block text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5] mb-1.5"
            >
              Hotel / Maktab / No. Bus (Opsional)
            </label>
            <div className="relative">
              <Hotel className="w-4 h-4 text-[#047857] dark:text-[#6EE7B7] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                id="vehicleDetail"
                type="text"
                value={profile.vehicleDetail}
                onChange={(e) => onUpdateProfile({ vehicleDetail: e.target.value })}
                placeholder={
                  profile.vehicleType === 'MOBIL'
                    ? 'Contoh: Hotel Ajyad · Bus 01'
                    : 'Contoh: Hotel Kiswah · Jalan Kaki'
                }
                className="w-full min-h-[46px] pl-10 pr-3.5 rounded-[14px] bg-[#F8FBF9] dark:bg-[#0F261D] border-[1.5px] border-[#9EC5B0] dark:border-[#2B5D49] focus:border-[#047857] focus:ring-2 focus:ring-[#047857] text-sm text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#6B8A7A] dark:placeholder:text-[#86B09A] shadow-2xs outline-none transition-colors"
              />
            </div>
            <p className="mt-1 text-xs text-[#4D6659] dark:text-[#9CC2AE]">
              Tampil di label peta jamaah, misal: H. Ahmad - Hotel Ajyad · Bus 01.
            </p>
          </div>

          {/* Nomor Kloter / Kamar / Gelang */}
          <div>
            <label
              htmlFor="licensePlate"
              className="block text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5] mb-1.5"
            >
              No. Kloter / Kamar / Gelang Jamaah
            </label>
            <div className="relative">
              <BadgeInfo className="w-4 h-4 text-[#047857] dark:text-[#6EE7B7] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                id="licensePlate"
                type="text"
                value={profile.licensePlate}
                onChange={(e) =>
                  onUpdateProfile({ licensePlate: e.target.value.toUpperCase() })
                }
                placeholder="Contoh: JKT-01 / KMR 402"
                className={`w-full min-h-[46px] pl-10 pr-3.5 rounded-[14px] bg-[#F8FBF9] dark:bg-[#0F261D] border-[1.5px] text-sm font-mono uppercase text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#6B8A7A] dark:placeholder:text-[#86B09A] shadow-2xs outline-none transition-colors ${
                  plateError
                    ? 'border-[#DC2626] focus:border-[#DC2626]'
                    : 'border-[#9EC5B0] dark:border-[#2B5D49] focus:border-[#047857] focus:ring-2 focus:ring-[#047857]'
                }`}
              />
            </div>
            {plateError && (
              <p className="mt-1 text-xs text-[#DC2626]">
                Masukkan nomor kloter, kamar, atau gelang jamaah yang valid
              </p>
            )}
          </div>
        </div>

        {/* Frame Dokumen Passport & Kontak Darurat */}
        <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border-[1.5px] border-[#BDD6C7] dark:border-[#244E3D] p-4 space-y-3.5 shadow-sm">
          <p className="text-xs font-bold uppercase tracking-wider text-[#047857] dark:text-[#6EE7B7]">
            Dokumen Passport &amp; Kontak Darurat
          </p>

          {/* Nomor Passport */}
          <div>
            <label
              htmlFor="passportNumber"
              className="block text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5] mb-1.5"
            >
              Nomor Passport (Opsional)
            </label>
            <div className="relative">
              <FileText className="w-4 h-4 text-[#047857] dark:text-[#6EE7B7] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                id="passportNumber"
                type="text"
                maxLength={32}
                value={profile.passportNumber || ''}
                onChange={(e) =>
                  onUpdateProfile({
                    passportNumber: e.target.value.toUpperCase().slice(0, 32),
                  })
                }
                placeholder="Contoh: C1234567"
                className="w-full min-h-[46px] pl-10 pr-3.5 rounded-[14px] bg-[#F8FBF9] dark:bg-[#0F261D] border-[1.5px] border-[#9EC5B0] dark:border-[#2B5D49] focus:border-[#047857] focus:ring-2 focus:ring-[#047857] text-sm font-mono uppercase text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#6B8A7A] dark:placeholder:text-[#86B09A] shadow-2xs outline-none transition-colors"
              />
            </div>
            <p className="mt-1 text-xs text-[#4D6659] dark:text-[#9CC2AE]">
              Disimpan privat di akun Anda untuk keperluan administrasi safar umroh.
            </p>
          </div>

          {/* Kontak Darurat */}
          <div>
            <label
              htmlFor="emergencyContact"
              className="block text-xs font-semibold text-[#0B1F16] dark:text-[#ECFDF5] mb-1.5"
            >
              Kontak Darurat Keluarga / Mutawwif (WA / Telp)
            </label>
            <div className="relative">
              <Phone className="w-4 h-4 text-[#047857] dark:text-[#6EE7B7] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                id="emergencyContact"
                type="tel"
                value={profile.emergencyContact}
                onChange={(e) =>
                  onUpdateProfile({ emergencyContact: e.target.value })
                }
                placeholder="Contoh: 0812-3456-7890"
                className={`w-full min-h-[46px] pl-10 pr-3.5 rounded-[14px] bg-[#F8FBF9] dark:bg-[#0F261D] border-[1.5px] text-sm font-mono text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#6B8A7A] dark:placeholder:text-[#86B09A] shadow-2xs outline-none transition-colors ${
                  contactError
                    ? 'border-[#DC2626] focus:border-[#DC2626]'
                    : 'border-[#9EC5B0] dark:border-[#2B5D49] focus:border-[#047857] focus:ring-2 focus:ring-[#047857]'
                }`}
              />
            </div>
            {contactError ? (
              <p className="mt-1 text-xs text-[#DC2626]">
                Masukkan nomor kontak darurat minimal 8 digit
              </p>
            ) : (
              <p className="mt-1 text-xs text-[#4D6659] dark:text-[#9CC2AE]">
                Digunakan jika jamaah terpisah atau membutuhkan bantuan saat umroh.
              </p>
            )}
          </div>
        </div>

        {/* Tentang Aplikasi & Kredit Pembuat */}
        <button
          type="button"
          onClick={onOpenAboutApp}
          className="w-full text-left rounded-[18px] bg-white dark:bg-[#0C1E17] border-[1.5px] border-[#BDD6C7] dark:border-[#244E3D] hover:border-[#047857] p-3.5 flex items-center gap-3 transition-colors cursor-pointer shadow-sm"
        >
          <BadgeCheck className="w-5 h-5 text-[#047857] dark:text-[#34D399] shrink-0" />
          <div className="min-w-0 flex-1">
            <p className="text-xs font-semibold">Tentang Aplikasi Safar Tracking</p>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              App Created &amp; Designed by Kang Ekie
            </p>
          </div>
          <ArrowRight className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
        </button>
      </div>

      {/* Sticky Bottom CTA */}
      <div className="sticky bottom-0 mt-auto px-5 pt-3.5 pb-[calc(2rem+env(safe-area-inset-bottom))] mb-2 bg-white dark:bg-[#0C1E17] border-t border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="submit"
          className="w-full min-h-[48px] px-4 py-3 rounded-[14px] bg-[#047857] hover:bg-[#065F46] active:scale-[0.98] text-white text-sm font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer shadow-sm"
        >
          <span className="whitespace-nowrap">
            {selectedRole === 'TOUR_LEADER'
              ? 'Lanjut Setup Rombongan'
              : 'Lanjut Gabung Rombongan'}
          </span>
          <ArrowRight className="w-4 h-4 shrink-0" />
        </button>
      </div>
    </form>
  );
}
