import React from 'react';
import { AlertTriangle, RotateCcw } from 'lucide-react';

interface State {
  hasError: boolean;
  errorMessage: string;
}

export class FirestoreErrorBoundary extends React.Component<
  { children: React.ReactNode },
  State
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, errorMessage: '' };
  }

  static getDerivedStateFromError(error: Error): State {
    return {
      hasError: true,
      errorMessage: error.message || 'Terjadi gangguan sinkronisasi data.',
    };
  }

  componentDidCatch(error: Error) {
    console.error('Safar Tracking ErrorBoundary caught:', error);
  }

  render() {
    if (this.state.hasError) {
      let parsedSummary = this.state.errorMessage;
      try {
        const obj = JSON.parse(this.state.errorMessage);
        if (obj?.error) {
          parsedSummary = `Akses data ditolak atau gagal (${obj.operationType || 'sync'}): ${obj.error}`;
        }
      } catch {
        // Non-JSON error message
      }

      return (
        <div className="min-h-screen bg-[#F4F8F5] dark:bg-[#06130E] flex items-center justify-center p-6 text-[#0F241B] dark:text-[#EDF7F2]">
          <div className="max-w-md w-full rounded-[24px] bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] p-6 shadow-xl space-y-4">
            <div className="w-12 h-12 rounded-2xl bg-[#FEE2E2] dark:bg-[#450A0A] text-[#DC2626] dark:text-[#F87171] flex items-center justify-center">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div className="space-y-1.5">
              <h2 className="text-base font-bold">Sinkronisasi Terhenti</h2>
              <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] leading-relaxed break-words">
                {parsedSummary}
              </p>
            </div>
            <button
              type="button"
              onClick={() => window.location.reload()}
              className="w-full min-h-[44px] rounded-xl bg-[#047857] hover:bg-[#065F46] text-white text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer"
            >
              <RotateCcw className="w-4 h-4" />
              <span>Muat Ulang Aplikasi</span>
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
