import { useMemo, useState } from 'react';
import {
  BookOpenCheck,
  Check,
  Copy,
  Search,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';
import {
  PRAYER_CATEGORY_META,
  PrayerCategory,
  PrayerLibraryItem,
  VERIFIED_PRAYER_LIBRARY,
} from '../data/prayerLibraryData';

interface PrayerGuidePanelProps {
  onOpenAssistant?: (initialPrompt?: string) => void;
  compact?: boolean;
}

export function PrayerGuidePanel({
  onOpenAssistant,
  compact = false,
}: PrayerGuidePanelProps) {
  const [selectedCategory, setSelectedCategory] = useState<'ALL' | PrayerCategory>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [copiedId, setCopiedId] = useState<string | null>(null);

  const filteredPrayers = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return VERIFIED_PRAYER_LIBRARY.filter((item) => {
      if (selectedCategory !== 'ALL' && item.category !== selectedCategory) {
        return false;
      }
      if (!q) return true;
      return (
        item.title.toLowerCase().includes(q) ||
        item.occasionHint.toLowerCase().includes(q) ||
        item.transliteration.toLowerCase().includes(q) ||
        item.translation.toLowerCase().includes(q) ||
        item.verificationBadge.toLowerCase().includes(q)
      );
    });
  }, [selectedCategory, searchQuery]);

  const handleCopyPrayer = async (item: PrayerLibraryItem) => {
    const fullText = [
      item.title,
      `(${item.verificationBadge})`,
      '',
      item.arabicText,
      item.transliteration,
      `Artinya: "${item.translation}"`,
    ].join('\n');
    try {
      await navigator.clipboard.writeText(fullText);
      setCopiedId(item.id);
      window.setTimeout(() => {
        setCopiedId((prev) => (prev === item.id ? null : prev));
      }, 2200);
    } catch {
      // Clipboard unsupported
    }
  };

  return (
    <div className={compact ? 'space-y-3' : 'space-y-4'}>
      {/* Verified Reference Banner */}
      <div className="rounded-[18px] bg-[#ECFDF5] dark:bg-[#064E3B]/45 border border-[#A7F3D0] dark:border-[#059669] px-3.5 py-3 flex items-start gap-2.5">
        <ShieldCheck className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0 mt-0.5" />
        <div className="min-w-0 space-y-0.5">
          <p className="text-xs font-bold text-[#065F46] dark:text-[#A7F3D0]">
            Kumpulan Doa Travel Umroh &amp; Standar Kemenag RI / MUI
          </p>
          <p className="text-[11px] leading-relaxed text-[#2D4A3D] dark:text-[#D1FAE5]">
            Disusun sesuai Buku Tuntunan Manasik Haji &amp; Umrah Kementerian Agama RI dan kumpulan doa harian mu&apos;tabar MUI ({VERIFIED_PRAYER_LIBRARY.length} doa lengkap Arab, Latin, &amp; arti).
          </p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="relative">
        <Search className="w-4 h-4 text-[#4D6659] dark:text-[#9CC2AE] absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          placeholder="Cari doa (safar, ka'bah, zamzam, makan, tidur)..."
          aria-label="Cari doa"
          className="w-full min-h-[42px] pl-9 pr-3 py-2 rounded-xl bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#6B8477] focus:outline-none focus:ring-2 focus:ring-[#047857]"
        />
      </div>

      {/* Category Filter Chips */}
      <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-0.5">
        <button
          type="button"
          onClick={() => setSelectedCategory('ALL')}
          className={`shrink-0 px-3 py-1.5 rounded-xl text-[11px] font-semibold border transition-colors cursor-pointer ${
            selectedCategory === 'ALL'
              ? 'bg-[#047857] text-white border-[#047857]'
              : 'bg-white dark:bg-[#0C1E17] text-[#2D4A3D] dark:text-[#D1FAE5] border-[#D8E6DD] dark:border-[#1B3B2E]'
          }`}
        >
          Semua ({VERIFIED_PRAYER_LIBRARY.length})
        </button>
        {PRAYER_CATEGORY_META.map((cat) => {
          const count = VERIFIED_PRAYER_LIBRARY.filter(
            (i) => i.category === cat.id
          ).length;
          const active = selectedCategory === cat.id;
          return (
            <button
              key={cat.id}
              type="button"
              onClick={() => setSelectedCategory(cat.id)}
              className={`shrink-0 px-3 py-1.5 rounded-xl text-[11px] font-semibold border transition-colors cursor-pointer ${
                active
                  ? 'bg-[#047857] text-white border-[#047857]'
                  : 'bg-white dark:bg-[#0C1E17] text-[#2D4A3D] dark:text-[#D1FAE5] border-[#D8E6DD] dark:border-[#1B3B2E]'
              }`}
            >
              {cat.shortLabel} ({count})
            </button>
          );
        })}
      </div>

      {/* Prayer Cards */}
      {filteredPrayers.length === 0 ? (
        <div className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-6 text-center space-y-2">
          <BookOpenCheck className="w-7 h-7 mx-auto text-[#047857] dark:text-[#34D399]" />
          <p className="text-xs font-semibold">Doa tidak ditemukan</p>
          <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
            Ubah kata kunci pencarian atau pilih kategori &quot;Semua&quot;.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredPrayers.map((item) => {
            const isCopied = copiedId === item.id;
            return (
              <article
                key={item.id}
                className="rounded-[18px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3.5 space-y-2.5 shadow-xs"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0 space-y-1">
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#6EE7B7] text-[10px] font-semibold">
                      <ShieldCheck className="w-3 h-3 shrink-0" />
                      <span>{item.verificationBadge}</span>
                    </span>
                    <h3 className="text-sm font-bold leading-snug text-[#0B1F16] dark:text-[#ECFDF5]">
                      {item.title}
                    </h3>
                    <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE]">
                      {item.occasionHint}
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleCopyPrayer(item)}
                    className={`shrink-0 px-2.5 py-1.5 rounded-lg border text-[10px] font-semibold flex items-center gap-1 cursor-pointer transition-colors ${
                      isCopied
                        ? 'bg-[#047857] text-white border-[#047857]'
                        : 'bg-[#F4F8F5] dark:bg-[#091812] text-[#2D4A3D] dark:text-[#D1FAE5] border-[#D8E6DD] dark:border-[#1B3B2E]'
                    }`}
                  >
                    {isCopied ? (
                      <>
                        <Check className="w-3 h-3" />
                        <span>Tersalin</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3 h-3" />
                        <span>Salin</span>
                      </>
                    )}
                  </button>
                </div>

                {/* Arabic + Transliteration + Translation */}
                <div className="rounded-xl bg-[#F4F8F5] dark:bg-[#06130E] border border-[#D8E6DD] dark:border-[#1B3B2E] p-3 space-y-2">
                  <p
                    dir="rtl"
                    lang="ar"
                    className="text-lg font-semibold leading-[2] text-right text-[#064E3B] dark:text-[#ECFDF5]"
                  >
                    {item.arabicText}
                  </p>
                  <p className="text-xs font-semibold italic text-[#B45309] dark:text-[#FCD34D] leading-relaxed">
                    {item.transliteration}
                  </p>
                  <p className="text-xs text-[#2D4A3D] dark:text-[#D1FAE5] leading-relaxed">
                    &ldquo;{item.translation}&rdquo;
                  </p>
                </div>

                {item.adabNote && (
                  <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] bg-[#FFFBEB] dark:bg-[#451A03]/40 border border-[#FDE68A] dark:border-[#92400E] rounded-lg px-2.5 py-1.5 leading-relaxed">
                    <span className="font-semibold text-[#92400E] dark:text-[#FCD34D]">
                      Catatan Adab:{' '}
                    </span>
                    {item.adabNote}
                  </p>
                )}

                {onOpenAssistant && (
                  <div className="pt-0.5">
                    <button
                      type="button"
                      onClick={() =>
                        onOpenAssistant(
                          `Jelaskan adab, keutamaan, dan tata cara membaca "${item.title}" sesuai tuntunan manasik Kemenag RI.`
                        )
                      }
                      className="min-h-[34px] px-3 py-1 rounded-lg bg-[#FFFBEB] dark:bg-[#451A03] border border-[#FDE68A] dark:border-[#92400E] text-[#B45309] dark:text-[#FCD34D] text-[11px] font-semibold inline-flex items-center gap-1.5 cursor-pointer"
                    >
                      <Sparkles className="w-3 h-3 shrink-0" />
                      <span>Tanya Penjelasan Mutawwif AI</span>
                    </button>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
