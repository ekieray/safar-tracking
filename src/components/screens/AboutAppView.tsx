import {
  ArrowLeft,
  BadgeCheck,
  Code2,
  Compass,
  MapPinned,
  Radio,
  Route,
  ShieldAlert,
} from 'lucide-react';
import {
  TouringCanvasBackdrop,
  TouringHeroBanner,
} from '../TouringAtmosphereBackdrop';

interface AboutAppViewProps {
  onBack: () => void;
}

const FEATURES = [
  {
    icon: MapPinned,
    title: 'Pelacakan Jamaah Real-Time & ETA',
    description:
      'Pantau posisi seluruh jamaah umroh, mutawwif, bus rombongan, dan estimasi waktu tiba di Masjidil Haram maupun Madinah.',
  },
  {
    icon: ShieldAlert,
    title: 'Deteksi Jamaah Terpisah, Keluar Jalur & SOS',
    description:
      'Peringatan otomatis apabila jamaah tertinggal di belakang pendamping, keluar dari rute ziarah, atau menekan tombol darurat SOS.',
  },
  {
    icon: Radio,
    title: 'Walkie-Talkie & Pesan Grup Rombongan',
    description:
      'Komunikasi suara cepat Push-to-Talk dan pesan singkat antara Mutawwif, pendamping, dan seluruh jamaah dalam satu grup.',
  },
  {
    icon: Route,
    title: 'Riwayat Safar, Ekspor GPX & Kartu Ziarah',
    description:
      'Simpan catatan perjalanan ziarah ke format GPX/JSON dan bagikan kartu rute visual bernuansa Tanah Suci.',
  },
];

export function AboutAppView({ onBack }: AboutAppViewProps) {
  return (
    <div className="relative flex flex-col min-h-full bg-transparent text-[#0B1F16] dark:text-[#ECFDF5]">
      {/* Top App Bar */}
      <div className="sticky top-0 z-20 flex items-center gap-1.5 px-3 py-2.5 bg-white/90 dark:bg-[#0C1E17]/90 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <button
          type="button"
          onClick={onBack}
          aria-label="Kembali"
          className="min-w-[44px] min-h-[44px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div className="min-w-0">
          <h2 className="text-sm font-bold truncate">Tentang Aplikasi</h2>
          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
            Informasi &amp; Kredit Pengembang
          </p>
        </div>
      </div>

      {/* Body */}
      <div className="relative flex-1 px-4 py-4 space-y-4">
        <TouringCanvasBackdrop />

        <div className="relative z-10 space-y-4">
          <TouringHeroBanner
            title="Safar Tracking"
            subtitle="Pendamping perjalanan ibadah Umroh & Haji untuk Mutawwif dan jamaah dengan pelacakan langsung, pos kumpul, walkie-talkie, serta tombol SOS."
          />

          {/* Features Summary Card */}
          <div className="rounded-[20px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
            <h3 className="text-sm font-bold">Fitur Utama Rombongan Umroh</h3>

            <div className="divide-y divide-[#D8E6DD] dark:divide-[#1B3B2E]">
              {FEATURES.map((item) => {
                const IconComp = item.icon;
                return (
                  <div
                    key={item.title}
                    className="py-2.5 first:pt-0 last:pb-0 flex items-start gap-3"
                  >
                    <div className="w-9 h-9 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399] flex items-center justify-center shrink-0">
                      <IconComp className="w-4 h-4" />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="text-xs font-semibold leading-snug">
                        {item.title}
                      </p>
                      <p className="text-[11px] text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed mt-0.5">
                        {item.description}
                      </p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Creator Credits Card */}
          <div className="rounded-[22px] bg-white dark:bg-[#0C1E17] border border-[#047857]/35 dark:border-[#34D399]/40 p-5 text-center space-y-2.5 shadow-xs">
            <div className="w-13 h-13 mx-auto rounded-full bg-[#ECFDF5] dark:bg-[#064E3B]/70 border-2 border-[#047857] dark:border-[#34D399] flex items-center justify-center text-[#047857] dark:text-[#6EE7B7]">
              <BadgeCheck className="w-6 h-6" />
            </div>

            <div className="flex items-center justify-center gap-1.5">
              <Code2 className="w-4 h-4 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
              <p className="text-sm font-bold tracking-tight text-[#0B1F16] dark:text-[#ECFDF5]">
                App Created &amp; Designed by Kang Ekie
              </p>
            </div>

            <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
              Dirancang dengan prinsip keteraturan dan ketenangan untuk mendampingi keselamatan jamaah Umroh di Tanah Suci.
            </p>

            <div className="pt-1 flex items-center justify-center gap-1.5 text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
              <Compass className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FCD34D]" />
              <span>Safar Tracking · Pendamping Perjalanan Umroh</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
