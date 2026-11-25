import { useEffect, useState } from 'react';
import { Check, Copy, Download, FileCode2 } from 'lucide-react';
import { KOTLIN_SOURCE_FILES, KotlinSourceFile } from '../data/kotlinFiles';
import { ScreenRoute } from '../types/convoy';

interface KotlinSourceExplorerProps {
  currentRoute: ScreenRoute;
  syncWithPreview: boolean;
  onToggleSync: () => void;
}

const CATEGORIES: Array<KotlinSourceFile['category']> = [
  'Layar Compose',
  'Navigasi & State',
  'Tema & Konfigurasi',
];

export function KotlinSourceExplorer({
  currentRoute,
  syncWithPreview,
  onToggleSync,
}: KotlinSourceExplorerProps) {
  const [selectedFileId, setSelectedFileId] = useState<string>('role_selection');
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!syncWithPreview) return;
    const matched = KOTLIN_SOURCE_FILES.find(
      (file) => file.linkedRoute === currentRoute
    );
    if (matched) {
      setSelectedFileId(matched.id);
    }
  }, [currentRoute, syncWithPreview]);

  const activeFile =
    KOTLIN_SOURCE_FILES.find((f) => f.id === selectedFileId) ||
    KOTLIN_SOURCE_FILES[0];

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(activeFile.code);
      setCopied(true);
      setTimeout(() => setCopied(false), 1800);
    } catch {
      // Ignore clipboard failure
    }
  };

  const handleDownloadFile = () => {
    const blob = new Blob([activeFile.code], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = activeFile.name.includes('/')
      ? activeFile.name.split('/').pop() || 'Source.kt'
      : activeFile.name;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const lines = activeFile.code.split('\n');

  return (
    <div className="flex flex-col rounded-2xl bg-white dark:bg-[#181F26] border border-[#E2E8F0] dark:border-[#334155] overflow-hidden h-full">
      {/* Header Bar */}
      <div className="px-5 py-4 border-b border-[#E2E8F0] dark:border-[#334155] flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <div className="flex items-center gap-2">
            <FileCode2 className="w-4 h-4 text-[#047857] dark:text-[#34D399] shrink-0" />
            <h2 className="text-sm font-semibold text-[#0B1F16] dark:text-[#ECFDF5] truncate">
              Sumber Kode Android Native (Kotlin & Jetpack Compose)
            </h2>
          </div>
          <p className="mt-0.5 text-xs font-mono text-[#4D6659] dark:text-[#9CC2AE] truncate">
            android/{activeFile.path}
          </p>
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={onToggleSync}
            className={`min-h-[36px] px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors cursor-pointer whitespace-nowrap ${
              syncWithPreview
                ? 'bg-[#ECFDF5] dark:bg-[#064E3B] border-[#A7F3D0] dark:border-[#059669] text-[#065F46] dark:text-[#D1FAE5]'
                : 'bg-transparent border-[#CBD5E1] dark:border-[#1B3B2E] text-[#4D6659] dark:text-[#9CC2AE] hover:bg-[#ECFDF5] dark:hover:bg-[#112A20]'
            }`}
          >
            {syncWithPreview ? 'Sinkron Layar: Aktif' : 'Sinkron Layar: Manual'}
          </button>

          <button
            type="button"
            onClick={handleCopyCode}
            className="min-h-[36px] px-3 py-1.5 rounded-lg border border-[#CBD5E1] dark:border-[#334155] hover:bg-[#F1F5F9] dark:hover:bg-[#222B35] text-xs font-medium text-[#111827] dark:text-[#F1F5F9] flex items-center gap-1.5 transition-colors cursor-pointer whitespace-nowrap"
          >
            {copied ? (
              <Check className="w-3.5 h-3.5 text-[#16A34A]" />
            ) : (
              <Copy className="w-3.5 h-3.5" />
            )}
            <span>{copied ? 'Tersalin' : 'Salin'}</span>
          </button>

          <button
            type="button"
            onClick={handleDownloadFile}
            className="min-h-[36px] px-3 py-1.5 rounded-lg bg-[#111827] dark:bg-[#F1F5F9] text-white dark:text-[#111827] hover:opacity-90 text-xs font-medium flex items-center gap-1.5 transition-opacity cursor-pointer whitespace-nowrap"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Unduh</span>
          </button>
        </div>
      </div>

      {/* File Selector Tabs grouped cleanly */}
      <div className="px-5 py-3 bg-[#F8FAFC] dark:bg-[#0F1419] border-b border-[#E2E8F0] dark:border-[#334155] space-y-2.5">
        {CATEGORIES.map((category) => {
          const files = KOTLIN_SOURCE_FILES.filter(
            (f) => f.category === category
          );
          return (
            <div
              key={category}
              className="flex flex-wrap items-center gap-1.5"
            >
              <span className="text-[11px] font-semibold text-[#64748B] dark:text-[#94A3B8] mr-1.5 min-w-[118px]">
                {category}
              </span>
              {files.map((file) => {
                const active = file.id === activeFile.id;
                return (
                  <button
                    key={file.id}
                    type="button"
                    onClick={() => setSelectedFileId(file.id)}
                    className={`px-2.5 py-1 rounded-md text-xs font-mono transition-colors cursor-pointer whitespace-nowrap shrink-0 ${
                      active
                        ? 'bg-[#047857] text-white font-medium'
                        : 'bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] text-[#334155] dark:text-[#D1FAE5] hover:border-[#047857]'
                    }`}
                  >
                    {file.name}
                  </button>
                );
              })}
            </div>
          );
        })}
      </div>

      {/* Code Viewport */}
      <div className="flex-1 overflow-auto bg-[#0F1419] text-[#E2E8F0] p-4 max-h-[640px]">
        <table className="w-full border-collapse text-xs font-mono leading-relaxed">
          <tbody>
            {lines.map((line, idx) => (
              <tr key={idx} className="hover:bg-white/5">
                <td className="select-none pr-4 text-right text-[#64748B] w-10 align-top tabular-nums">
                  {idx + 1}
                </td>
                <td className="whitespace-pre text-[#F1F5F9]">{line}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
