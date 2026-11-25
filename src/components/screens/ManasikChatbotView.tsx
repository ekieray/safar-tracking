import { useEffect, useRef, useState } from 'react';
import {
  AlertTriangle,
  ArrowLeft,
  BookOpenCheck,
  Loader2,
  Send,
  Sparkles,
  Trash2,
} from 'lucide-react';
import {
  AssistantChatMessage,
  AssistantRoleId,
  ManasikProgressState,
} from '../../types/convoy';
import { TouringCanvasBackdrop } from '../TouringAtmosphereBackdrop';

interface ManasikChatbotViewProps {
  manasikProgress: ManasikProgressState;
  currentLocationText?: string;
  initialPrompt?: string | null;
  onConsumeInitialPrompt?: () => void;
  onBack: () => void;
}

const ROLE_OPTIONS: Array<{
  id: AssistantRoleId;
  label: string;
  subtitle: string;
  modelBadge: string;
}> = [
  {
    id: 'mutawwif',
    label: 'Mutawwif',
    subtitle: 'Bimbingan lengkap tata cara Umroh & Haji',
    modelBadge: 'gemini-4-hydrogen',
  },
  {
    id: 'doa_fast',
    label: 'Doa Cepat',
    subtitle: 'Bacaan niat, talbiyah, dan doa ringkas',
    modelBadge: 'gemini-3.1-flash-lite',
  },
  {
    id: 'koordinator',
    label: 'Koordinator',
    subtitle: 'Titik kumpul, lansia & koordinasi rombongan',
    modelBadge: 'gemini-4-hydrogen',
  },
];

const QUICK_PROMPTS: Array<{ label: string; text: string }> = [
  {
    label: 'Niat Ihram Umroh',
    text: 'Tolong tuliskan bacaan niat Ihram Umroh beserta Latin dan artinya, serta larangan utama saat ihram.',
  },
  {
    label: "Urutan Sa'i Safa-Marwah",
    text: "Bagaimana urutan dan hitungan perjalanan Sa'i dari Bukit Safa ke Marwah agar tidak salah hitung?",
  },
  {
    label: 'Jaga Lansia Saat Tawaf',
    text: 'Apa tips menjaga jamaah lansia agar tetap aman dan tidak terpisah saat Tawaf dan Sa’i?',
  },
];

const CHAT_STORAGE_KEY = 'safar_gemini_chat_history_v1';

function clockLabel(): string {
  const d = new Date();
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

export function ManasikChatbotView({
  manasikProgress,
  currentLocationText = '',
  initialPrompt = null,
  onConsumeInitialPrompt,
  onBack,
}: ManasikChatbotViewProps) {
  const [roleId, setRoleId] = useState<AssistantRoleId>('mutawwif');
  const [messages, setMessages] = useState<AssistantChatMessage[]>(() => {
    if (typeof window === 'undefined') return [];
    try {
      const raw = window.localStorage.getItem(CHAT_STORAGE_KEY);
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  });
  const [inputText, setInputText] = useState('');
  const [isSending, setIsSending] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const scrollEndRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      window.localStorage.setItem(
        CHAT_STORAGE_KEY,
        JSON.stringify(messages.slice(-40))
      );
    } catch {
      // Ignore storage error
    }
  }, [messages]);

  useEffect(() => {
    scrollEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages.length, isSending]);

  const sendMessageText = async (rawText: string) => {
    const trimmed = rawText.trim();
    if (!trimmed || isSending) return;

    const userMsg: AssistantChatMessage = {
      id: `usr-${Date.now()}`,
      role: 'user',
      text: trimmed,
      timestampLabel: clockLabel(),
      roleId,
    };

    const historySnapshot = messages.slice(-20).map((m) => ({
      role: m.role,
      text: m.text,
    }));

    setMessages((prev) => [...prev, userMsg]);
    setInputText('');
    setErrorMsg(null);
    setIsSending(true);

    try {
      const response = await fetch('/api/gemini/chat', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: trimmed,
          history: historySnapshot,
          roleId,
          context: {
            guideType: manasikProgress.haji.length > manasikProgress.umrah.length ? 'haji' : 'umrah',
            completedCount:
              manasikProgress.haji.length > manasikProgress.umrah.length
                ? manasikProgress.haji.length
                : manasikProgress.umrah.length,
            totalCount:
              manasikProgress.haji.length > manasikProgress.umrah.length ? 9 : 5,
            currentLocationText,
          },
        }),
      });

      const data = (await response.json()) as {
        ok?: boolean;
        reply?: string;
        model?: string;
        error?: string;
      };

      if (!response.ok || !data.ok || !data.reply) {
        throw new Error(
          data.error || `Gagal menerima jawaban asisten (${response.status}).`
        );
      }

      const modelMsg: AssistantChatMessage = {
        id: `ai-${Date.now()}`,
        role: 'model',
        text: data.reply,
        timestampLabel: clockLabel(),
        roleId,
        modelName: data.model,
      };
      setMessages((prev) => [...prev, modelMsg]);
    } catch (err) {
      setErrorMsg(
        err instanceof Error
          ? err.message
          : 'Terjadi gangguan jaringan saat menghubungi asisten Gemini.'
      );
    } finally {
      setIsSending(false);
    }
  };

  useEffect(() => {
    if (initialPrompt && initialPrompt.trim().length > 0 && !isSending) {
      const promptToSend = initialPrompt;
      onConsumeInitialPrompt?.();
      sendMessageText(promptToSend);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialPrompt]);

  const activeRole =
    ROLE_OPTIONS.find((r) => r.id === roleId) || ROLE_OPTIONS[0];

  return (
    <div className="relative flex flex-col h-[680px] bg-transparent text-[#0B1F16] dark:text-[#ECFDF5] overflow-hidden">
      {/* Top App Bar */}
      <div className="shrink-0 z-20 flex items-center justify-between gap-2 px-3 py-2.5 bg-white/95 dark:bg-[#0C1E17]/95 backdrop-blur-md border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="flex items-center gap-1.5 min-w-0">
          <button
            type="button"
            onClick={onBack}
            aria-label="Kembali"
            className="min-w-[40px] min-h-[40px] rounded-full flex items-center justify-center text-[#047857] dark:text-[#34D399] hover:bg-[#ECFDF5] dark:hover:bg-[#064E3B]/40 transition-colors cursor-pointer shrink-0"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="min-w-0">
            <h2 className="text-sm font-bold truncate">Tanya Mutawwif AI</h2>
            <p className="text-[11px] text-[#4D6659] dark:text-[#9CC2AE] truncate">
              {activeRole.subtitle}
            </p>
          </div>
        </div>

        {messages.length > 0 && (
          <button
            type="button"
            onClick={() => {
              setMessages([]);
              setErrorMsg(null);
            }}
            aria-label="Hapus riwayat percakapan"
            className="min-h-[36px] px-2.5 py-1.5 rounded-lg border border-[#D8E6DD] dark:border-[#1B3B2E] text-[11px] font-semibold text-[#4D6659] dark:text-[#9CC2AE] hover:text-[#B45309] flex items-center gap-1 cursor-pointer shrink-0 whitespace-nowrap"
          >
            <Trash2 className="w-3.5 h-3.5" />
            <span>Baru</span>
          </button>
        )}
      </div>

      {/* Role Selector */}
      <div className="shrink-0 z-10 px-3 py-2 bg-white/90 dark:bg-[#0C1E17]/90 border-b border-[#D8E6DD] dark:border-[#1B3B2E]">
        <div className="grid grid-cols-3 gap-1 p-1 rounded-xl bg-[#E6EFE9] dark:bg-[#091812]">
          {ROLE_OPTIONS.map((roleOpt) => {
            const active = roleOpt.id === roleId;
            return (
              <button
                key={roleOpt.id}
                type="button"
                onClick={() => setRoleId(roleOpt.id)}
                className={`min-h-[36px] px-2 py-1.5 rounded-lg text-[11px] font-semibold transition-colors cursor-pointer whitespace-nowrap ${
                  active
                    ? 'bg-[#047857] text-white shadow-xs'
                    : 'text-[#2D4A3D] dark:text-[#9CC2AE] hover:text-[#0B1F16] dark:hover:text-white'
                }`}
              >
                {roleOpt.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Scrollable Chat Thread */}
      <div className="relative flex-1 overflow-y-auto px-3.5 py-4 space-y-3">
        <TouringCanvasBackdrop />

        <div className="relative z-10 space-y-3">
          {messages.length === 0 && (
            <div className="rounded-[20px] bg-white/95 dark:bg-[#0C1E17]/95 border border-[#D8E6DD] dark:border-[#1B3B2E] p-4 space-y-3">
              <div className="flex items-start gap-2.5">
                <div className="w-10 h-10 rounded-xl bg-[#ECFDF5] dark:bg-[#064E3B] text-[#047857] dark:text-[#34D399] flex items-center justify-center shrink-0">
                  <Sparkles className="w-5 h-5" />
                </div>
                <div className="min-w-0">
                  <h3 className="text-sm font-bold">
                    Assalamu&apos;alaikum, ada yang bisa dibantu?
                  </h3>
                  <p className="text-xs text-[#4D6659] dark:text-[#9CC2AE] leading-relaxed mt-0.5">
                    Tanyakan tata cara manasik Umroh &amp; Haji, bacaan doa, atau tips koordinasi rombongan di Tanah Suci.
                  </p>
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="text-[11px] font-semibold text-[#047857] dark:text-[#6EE7B7]">
                  Contoh pertanyaan cepat:
                </p>
                {QUICK_PROMPTS.map((q) => (
                  <button
                    key={q.label}
                    type="button"
                    onClick={() => sendMessageText(q.text)}
                    className="w-full text-left px-3 py-2.5 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] hover:border-[#047857] text-xs font-medium text-[#0B1F16] dark:text-[#ECFDF5] transition-colors cursor-pointer flex items-center gap-2"
                  >
                    <BookOpenCheck className="w-3.5 h-3.5 text-[#B45309] dark:text-[#FCD34D] shrink-0" />
                    <span className="truncate">{q.label}</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {messages.map((msg) => {
            const isUser = msg.role === 'user';
            return (
              <div
                key={msg.id}
                className={`flex flex-col ${
                  isUser ? 'items-end' : 'items-start'
                }`}
              >
                <div
                  className={`max-w-[88%] rounded-[18px] px-3.5 py-2.5 text-xs leading-relaxed whitespace-pre-wrap break-words ${
                    isUser
                      ? 'bg-[#047857] text-white rounded-br-sm'
                      : 'bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] text-[#0B1F16] dark:text-[#ECFDF5] rounded-bl-sm shadow-xs'
                  }`}
                >
                  {msg.text}
                </div>
                <span className="mt-1 px-1 text-[10px] text-[#4D6659] dark:text-[#9CC2AE] tabular-nums">
                  {isUser ? 'Anda' : 'Mutawwif AI'} · {msg.timestampLabel}
                </span>
              </div>
            );
          })}

          {isSending && (
            <div className="flex items-start">
              <div className="rounded-[18px] rounded-bl-sm bg-white dark:bg-[#0C1E17] border border-[#D8E6DD] dark:border-[#1B3B2E] px-3.5 py-2.5 text-xs text-[#4D6659] dark:text-[#9CC2AE] flex items-center gap-2">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-[#047857] dark:text-[#34D399]" />
                <span>Menyiapkan jawaban...</span>
              </div>
            </div>
          )}

          {errorMsg && (
            <div className="rounded-xl bg-[#FEF2F2] dark:bg-[#450A0A]/80 border border-[#FECACA] dark:border-[#991B1B] px-3.5 py-2.5 flex items-start gap-2 text-xs text-[#991B1B] dark:text-[#FECACA]">
              <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
              <p className="leading-relaxed">{errorMsg}</p>
            </div>
          )}

          <div ref={scrollEndRef} />
        </div>
      </div>

      {/* Message Composer */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          sendMessageText(inputText);
        }}
        className="shrink-0 z-20 p-3 bg-white/95 dark:bg-[#0C1E17]/95 backdrop-blur-md border-t border-[#D8E6DD] dark:border-[#1B3B2E] flex items-center gap-2"
      >
        <input
          type="text"
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Tanya tata cara manasik atau doa..."
          disabled={isSending}
          className="flex-1 min-w-0 min-h-[44px] px-3.5 py-2 rounded-xl bg-[#F4F8F5] dark:bg-[#091812] border border-[#D8E6DD] dark:border-[#1B3B2E] text-xs text-[#0B1F16] dark:text-[#ECFDF5] placeholder:text-[#4D6659]/70 dark:placeholder:text-[#9CC2AE]/60 focus:outline-none focus:border-[#047857]"
        />
        <button
          type="submit"
          disabled={isSending || !inputText.trim()}
          aria-label="Kirim pesan"
          className="min-w-[44px] min-h-[44px] rounded-xl bg-[#047857] hover:bg-[#065F46] disabled:opacity-50 text-white flex items-center justify-center transition-colors cursor-pointer shrink-0"
        >
          <Send className="w-4 h-4" />
        </button>
      </form>
    </div>
  );
}
