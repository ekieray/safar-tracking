interface TouringHeroBannerProps {
  title: string;
  subtitle: string;
}

/**
 * Full-canvas subtle Islamic geometric & Haramain contour backdrop at very low opacity.
 * Sits strictly behind solid sandstone/emerald surface cards.
 */
export function TouringCanvasBackdrop() {
  return (
    <div
      aria-hidden="true"
      className="pointer-events-none absolute inset-0 overflow-hidden select-none"
    >
      {/* Subtle Light / Dark Translucent Haramain Wash */}
      <div
        className="absolute inset-0 dark:hidden"
        style={{
          background:
            'linear-gradient(180deg, rgba(244,248,245,0.22) 0%, rgba(236,253,245,0.10) 55%, rgba(255,251,235,0.18) 100%)',
        }}
      />
      <div
        className="hidden dark:block absolute inset-0"
        style={{
          background:
            'linear-gradient(180deg, rgba(6,19,14,0.48) 0%, rgba(12,30,23,0.34) 55%, rgba(6,19,14,0.52) 100%)',
        }}
      />

      {/* Low-Opacity Rub el Hizb Geometric Stars & Haramain Horizon Traces */}
      <svg
        className="absolute inset-0 w-full h-full opacity-[0.075] dark:opacity-[0.11]"
        viewBox="0 0 390 760"
        preserveAspectRatio="none"
      >
        {/* Top-right 8-point Rub el Hizb star */}
        <g transform="translate(325, 110)" stroke="currentColor" className="text-[#047857] dark:text-[#34D399]" fill="none" strokeWidth="1.2">
          <rect x="-34" y="-34" width="68" height="68" rx="2" />
          <rect x="-34" y="-34" width="68" height="68" rx="2" transform="rotate(45)" />
          <circle cx="0" cy="0" r="16" />
        </g>

        {/* Bottom-left subtle 8-point geometric medallion */}
        <g transform="translate(55, 620)" stroke="currentColor" className="text-[#B45309] dark:text-[#FBBF24]" fill="none" strokeWidth="1.1">
          <rect x="-28" y="-28" width="56" height="56" rx="2" />
          <rect x="-28" y="-28" width="56" height="56" rx="2" transform="rotate(45)" />
        </g>

        {[0, 1, 2, 3].map((idx) => {
          const yOffset = idx * 46;
          return (
            <path
              key={`contour-${idx}`}
              d={`M -30 ${150 + yOffset} C 110 ${85 + yOffset}, 250 ${230 + yOffset}, 420 ${145 + yOffset}`}
              fill="none"
              stroke="currentColor"
              className="text-[#047857] dark:text-[#34D399]"
              strokeWidth="1.1"
            />
          );
        })}

        {/* Subtle golden path arc in bottom whitespace */}
        <path
          d="M -20 695 C 120 605, 260 735, 420 630"
          fill="none"
          stroke="currentColor"
          className="text-[#B45309] dark:text-[#FBBF24]"
          strokeWidth="2"
          strokeDasharray="9 8"
          strokeLinecap="round"
        />
      </svg>
    </div>
  );
}

/**
 * Haramain Makkah Hero Banner:
 * Deep Haram emerald gradient + Kiswah gold horizon glow + Masjidil Haram silhouette
 */
export function TouringHeroBanner({ title, subtitle }: TouringHeroBannerProps) {
  return (
    <div
      className="relative overflow-hidden rounded-[22px] border border-[#059669]/50 px-4 py-4 shadow-sm"
      style={{
        background:
          'linear-gradient(135deg, #032217 0%, #064E3B 58%, #047857 100%)',
      }}
    >
      {/* Kiswah Gold Horizon Glow + Masjidil Haram Skyline & Rub el Hizb */}
      <svg
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 w-full h-full"
        viewBox="0 0 360 140"
        preserveAspectRatio="none"
      >
        <defs>
          <radialGradient id="kiswahGoldGlow" cx="88%" cy="82%" r="58%">
            <stop offset="0%" stopColor="#F59E0B" stopOpacity="0.32" />
            <stop offset="55%" stopColor="#D97706" stopOpacity="0.12" />
            <stop offset="100%" stopColor="#D97706" stopOpacity="0" />
          </radialGradient>
        </defs>
        <rect width="360" height="140" fill="url(#kiswahGoldGlow)" />

        {/* Subtle 8-point Islamic geometric star top right */}
        <g transform="translate(318, 34)" stroke="rgba(253,230,138,0.18)" fill="none" strokeWidth="1.2">
          <rect x="-18" y="-18" width="36" height="36" />
          <rect x="-18" y="-18" width="36" height="36" transform="rotate(45)" />
        </g>

        {/* Crescent Moon & Star */}
        <path
          d="M 272 22 A 10 10 0 1 0 282 34 A 8 8 0 1 1 272 22 Z"
          fill="rgba(253,230,138,0.35)"
        />

        {/* Subtle Masjidil Haram Minarets & Dome Silhouette on bottom right */}
        <path
          d="M 220 140 L 220 112 L 226 112 L 226 94 L 229 84 L 232 94 L 232 112 L 246 112 C 250 96, 266 96, 270 112 L 284 112 L 284 90 L 287 79 L 290 90 L 290 112 L 304 112 L 304 100 L 328 100 L 328 112 L 340 112 L 340 94 L 343 84 L 346 94 L 346 140 Z"
          fill="rgba(2, 24, 16, 0.36)"
        />
        {/* Ka'bah subtle gold Kiswah band accent */}
        <rect x="307" y="104" width="18" height="2.4" fill="rgba(251,191,36,0.45)" />
      </svg>

      <div className="relative z-10 space-y-1">
        <h1 className="text-xl font-bold tracking-tight leading-snug text-white text-balance">
          {title}
        </h1>
        <p className="text-xs text-[#D1FAE5] leading-relaxed">{subtitle}</p>
      </div>
    </div>
  );
}
