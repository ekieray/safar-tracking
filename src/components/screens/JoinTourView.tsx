import { ArrowLeft, ClipboardPaste, Cloud, Link2, LogIn } from 'lucide-react';
import { VehicleProfile } from '../../types/convoy';
import { ConvoyAvatar } from '../ConvoyAvatar';

interface JoinTourViewProps {
  joinInput: string;
  joinError: string | null;
  extractedRoomId: string;
  vehicleProfile: VehicleProfile;
  isAuthenticated?: boolean;
  isJoining?: boolean;
  onGoogleSignIn?: () => void;
  onInputChange: (val: string) => void;
  onJoinSubmit: () => void;
  onJoinDemoOffline?: () => void;
  onBack: () => void;
}

export function JoinTourView({
  joinInput,
  joinError,
  extractedRoomId,
  vehicleProfile,
  isAuthenticated,
  isJoining,
  onGoogleSignIn,
  onInputChange,
  onJoinSubmit,
  onJoinDemoOffline,
  onBack,
}: JoinTourViewProps) {
  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) onInputChange(text);
    } catch {
      onInputChange('https://safartracking.id/join/UMR-8492');
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onJoinSubmit();
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]"
    >
      {/* Top App Bar */}
      <div className="sticky top-0 z-10 flex items-center gap-2 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="button"
          onClick={onBack}
          aria-label="Kembali"
          className="min-w-[44px] min-h-[44px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-base font-semibold truncate">Gabung Rombongan Umroh</h2>
      </div>

      <div className="flex-1 px-5 py-5 flex flex-col justify-between gap-6">
        <div className="space-y-4">
          <div className="rounded-[20px] bg-white/95 dark:bg-[#0C1E17]/95 border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 shadow-xs space-y-4">
            <p className="text-sm text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
              Masukkan Kode Grup Rombongan atau tempel tautan undangan yang dibagikan oleh Mutawwif / Pembimbing.
            </p>

            {!isAuthenticated && onGoogleSignIn && (
              <div className="rounded-2xl bg-[#ECFDF5] dark:bg-[#064E3B]/45 border border-[#A7F3D0] dark:border-[#059669] p-3.5 flex items-center justify-between gap-2.5">
                <div className="min-w-0">
                  <p className="text-xs font-bold text-[#065F46] dark:text-[#A7F3D0] flex items-center gap-1.5">
                    <Cloud className="w-3.5 h-3.5 shrink-0" />
                    <span>Gabung Grup Multi-Perangkat</span>
                  </p>
                  <p className="text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5]">
                    Masuk Google untuk sinkronisasi GPS nyata ke grup Mutawwif.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={onGoogleSignIn}
                  className="px-3 py-1.5 rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-[11px] font-semibold cursor-pointer shrink-0 whitespace-nowrap"
                >
                  Masuk Google
                </button>
              </div>
            )}

            {/* Input Room ID / Link */}
            <div>
              <label
                htmlFor="roomInput"
                className="block text-xs font-semibold text-[#1E3A2F] dark:text-[#D1FAE5] mb-1.5"
              >
                Kode Grup Rombongan atau Link Undangan
              </label>
              <div className="relative">
                <Link2 className="w-4 h-4 text-[#4D6659] absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
                <input
                  id="roomInput"
                  type="text"
                  value={joinInput}
                  onChange={(e) => onInputChange(e.target.value)}
                  placeholder="UMR-8492 atau https://safartracking.id/join/UMR-8492"
                  className={`w-full min-h-[48px] pl-10 pr-12 rounded-[14px] bg-[#F4F8F5] dark:bg-[#091812] border text-sm font-mono outline-none transition-colors ${
                    joinError
                      ? 'border-[#DC2626] focus:border-[#DC2626]'
                      : 'border-[#C6DACD] dark:border-[#1E3F31] focus:border-[#047857]'
                  }`}
                />
                <button
                  type="button"
                  onClick={handlePaste}
                  aria-label="Tempel dari Clipboard"
                  title="Tempel dari Clipboard"
                  className="min-w-[40px] min-h-[40px] rounded-lg flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 absolute right-1.5 top-1/2 -translate-y-1/2 transition-colors cursor-pointer"
                >
                  <ClipboardPaste className="w-4 h-4" />
                </button>
              </div>
              {joinError ? (
                <p className="mt-1.5 text-xs text-[#DC2626]">{joinError}</p>
              ) : extractedRoomId.length >= 4 ? (
                <p className="mt-1.5 text-xs text-[#047857] dark:text-[#34D399] font-medium">
                  Terdeteksi Kode Grup: <span className="font-mono">{extractedRoomId}</span>
                </p>
              ) : null}
            </div>

            {/* Quick Demo Sample */}
            <div className="flex flex-wrap items-center gap-2 text-xs text-[#4D6659] dark:text-[#9CC2AE]">
              <span>Contoh kode:</span>
              <button
                type="button"
                onClick={() => onInputChange('https://safartracking.id/join/UMR-8492')}
                className="px-2.5 py-1.5 rounded-lg bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#A7F3D0] font-mono font-medium transition-colors cursor-pointer"
              >
                UMR-8492
              </button>
              {!isAuthenticated && onJoinDemoOffline && (
                <button
                  type="button"
                  onClick={onJoinDemoOffline}
                  className="px-2.5 py-1.5 rounded-lg border border-[#FDE68A] dark:border-[#B45309] bg-[#FFFBEB] dark:bg-[#451A03]/45 text-[#B45309] dark:text-[#FCD34D] font-semibold transition-colors cursor-pointer"
                >
                  Simulasi Demo Offline
                </button>
              )}
            </div>
          </div>

          {/* Registered Pilgrim Summary */}
          <div className="rounded-2xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 flex items-center gap-3">
            <ConvoyAvatar
              preset={vehicleProfile.avatarPreset}
              photoUrl={vehicleProfile.avatarPhotoUrl}
              name={vehicleProfile.fullName.trim() || 'Jamaah Umroh'}
              sizeClass="w-11 h-11"
              ringClass="ring-2 ring-[#047857] dark:ring-[#34D399]"
            />
            <div className="min-w-0 flex-1 space-y-0.5">
              <p className="text-xs font-medium text-[#047857] dark:text-[#34D399]">
                Profil Jamaah Terdaftar
              </p>
              <p className="text-sm font-semibold truncate">
                {vehicleProfile.fullName.trim() || 'Jamaah Umroh'}
              </p>
              <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] truncate">
                {vehicleProfile.vehicleType === 'MOBIL' ? 'Bus Rombongan' : 'Jalan Kaki'} ·{' '}
                <span className="font-mono">
                  {vehicleProfile.licensePlate.trim() || '-'}
                </span>{' '}
                · Darurat:{' '}
                <span className="font-mono">
                  {vehicleProfile.emergencyContact.trim() || '-'}
                </span>
              </p>
            </div>
          </div>
        </div>

        {/* Submit CTA */}
        <div className="mt-auto pt-2 pb-[calc(1.75rem+env(safe-area-inset-bottom))]">
          <button
            type="submit"
            disabled={isJoining}
            className="w-full min-h-[48px] px-4 py-3 rounded-[14px] bg-[#B45309] hover:bg-[#92400E] disabled:opacity-60 active:scale-[0.98] text-white text-sm font-semibold flex items-center justify-center gap-2 transition-all cursor-pointer"
          >
            <LogIn className="w-4 h-4 shrink-0" />
            <span className="whitespace-nowrap">
              {isJoining ? 'Menghubungkan Grup...' : 'Masuk Rombongan'}
            </span>
          </button>
        </div>
      </div>
    </form>
  );
}
