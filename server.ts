import 'dotenv/config';
import express from 'express';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import type { Request, Response } from 'express';
import { GoogleGenAI, ThinkingLevel } from '@google/genai';

import fs from 'node:fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DEV_FALLBACK_URL =
  'https://ais-dev-3puhws3li7lokxfmbtgyz4-588877716211.asia-southeast1.run.app';
const SHARED_FALLBACK_URL =
  'https://ais-pre-3puhws3li7lokxfmbtgyz4-588877716211.asia-southeast1.run.app';

function resolveBaseUrl(req: Request): string {
  const envUrl = process.env.APP_URL?.trim();
  if (envUrl) {
    return envUrl.replace(/\/+$/, '');
  }
  const forwardedProto = req.get('x-forwarded-proto') || req.protocol || 'https';
  const host = req.get('host');
  if (host) {
    return `${forwardedProto}://${host}`;
  }
  return DEV_FALLBACK_URL;
}

function escapeHtmlLiteral(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

async function startServer() {
  const app = express();
  app.set('trust proxy', true);
  app.use(express.json());

  const getGoogleMapsApiKey = (): string =>
    process.env.GOOGLE_MAPS_API_KEY?.trim() ||
    process.env.VITE_GOOGLE_MAPS_API_KEY?.trim() ||
    '';

  app.get('/api/health', (_req: Request, res: Response) => {
    res.setHeader('Cache-Control', 'no-store');
    res.json({ ok: true, service: 'safar-tracking' });
  });

  type AssistantRoleId = 'mutawwif' | 'doa_fast' | 'koordinator';

  const ROLE_CONFIGS: Record<
    AssistantRoleId,
    {
      model: 'gemini-4-hydrogen' | 'gemini-3.1-flash-lite';
      systemInstruction: string;
    }
  > = {
    mutawwif: {
      model: 'gemini-4-hydrogen',
      systemInstruction:
        'Anda adalah Ustadz / Mutawwif Pembimbing Umroh & Haji pada aplikasi Safar Tracking. Jawablah pertanyaan jamaah dalam Bahasa Indonesia yang ramah, santun, terstruktur, dan menenangkan. Jelaskan tata cara manasik (Ihram, Tawaf, Sa’i, Tahallul, Wukuf, Mabit, Jamarat), rukun/wajib, serta adab beribadah di Masjidil Haram & Masjid Nabawi secara ringkas dan akurat.',
    },
    doa_fast: {
      model: 'gemini-3.1-flash-lite',
      systemInstruction:
        'Anda adalah Pemandu Doa & Zikir Cepat Safar Tracking. Berikan bacaan niat, talbiyah, atau doa Umroh & Haji dengan sangat ringkas, cepat dibaca saat beribadah: sertakan tulisan Arab (jika relevan), transliterasi Latin, dan arti singkat dalam Bahasa Indonesia.',
    },
    koordinator: {
      model: 'gemini-4-hydrogen',
      systemInstruction:
        'Anda adalah Koordinator Safar & Keselamatan Rombongan di aplikasi Safar Tracking. Bantu Mutawwif dan jamaah mengenai titik kumpul (Ajyad, Bab Al-Malik, Mas’a, Tan’im, Bir Ali, Arafah, Muzdalifah, Mina), tips mencegah jamaah terpisah, serta langkah antisipasi saat lansia kelelahan atau terpisah dari rombongan.',
    },
  };

  app.options(['/api/gemini/chat', '/api/gemini/chat/'], (_req: Request, res: Response) => {
    res.status(204).end();
  });

  app.post(['/api/gemini/chat', '/api/gemini/chat/'], async (req: Request, res: Response) => {
    const apiKey = process.env.GEMINI_API_KEY?.trim();
    if (!apiKey) {
      res.status(503).json({
        ok: false,
        error:
          'GEMINI_API_KEY belum tersedia di server. Silakan atur kunci API pada panel Settings > Secrets di AI Studio.',
      });
      return;
    }

    const body = req.body as {
      message?: unknown;
      history?: unknown;
      roleId?: unknown;
      context?: {
        guideType?: unknown;
        completedCount?: unknown;
        totalCount?: unknown;
        currentLocationText?: unknown;
      };
    };

    const userMessage =
      typeof body?.message === 'string' ? body.message.trim() : '';
    if (!userMessage || userMessage.length > 2000) {
      res.status(400).json({
        ok: false,
        error: 'Pesan tidak valid atau terlalu panjang (maksimal 2000 karakter).',
      });
      return;
    }

    const roleId: AssistantRoleId =
      body?.roleId === 'doa_fast' || body?.roleId === 'koordinator'
        ? body.roleId
        : 'mutawwif';
    const roleCfg = ROLE_CONFIGS[roleId];

    const formattedHistory: Array<{
      role: 'user' | 'model';
      parts: Array<{ text: string }>;
    }> = [];
    if (Array.isArray(body?.history)) {
      for (const item of body.history.slice(-20)) {
        if (!item || typeof item !== 'object') continue;
        const rawRole = (item as { role?: unknown }).role;
        const rawText = (item as { text?: unknown }).text;
        if (
          (rawRole === 'user' || rawRole === 'model') &&
          typeof rawText === 'string' &&
          rawText.trim().length > 0
        ) {
          const cleanText = rawText.trim().slice(0, 4000);
          if (formattedHistory.length === 0 && rawRole !== 'user') {
            continue;
          }
          const prev = formattedHistory[formattedHistory.length - 1];
          if (prev && prev.role === rawRole) {
            prev.parts.push({ text: cleanText });
          } else {
            formattedHistory.push({
              role: rawRole,
              parts: [{ text: cleanText }],
            });
          }
        }
      }
      if (
        formattedHistory.length > 0 &&
        formattedHistory[formattedHistory.length - 1].role === 'user'
      ) {
        formattedHistory.pop();
      }
    }

    const ctxParts: string[] = [];
    if (body?.context) {
      const gt = body.context.guideType;
      const done = Number(body.context.completedCount);
      const total = Number(body.context.totalCount);
      const loc =
        typeof body.context.currentLocationText === 'string'
          ? body.context.currentLocationText.trim().slice(0, 120)
          : '';
      if (gt === 'umrah' || gt === 'haji') {
        ctxParts.push(
          `Panduan aktif jamaah: ${gt === 'umrah' ? 'Umroh' : 'Haji'}${
            Number.isFinite(done) && Number.isFinite(total) && total > 0
              ? ` (${done}/${total} langkah selesai)`
              : ''
          }.`
        );
      }
      if (loc) {
        ctxParts.push(`Posisi / area jamaah saat ini: ${loc}.`);
      }
    }

    const fullSystemInstruction =
      ctxParts.length > 0
        ? `${roleCfg.systemInstruction}\n\nKonteks jamaah saat ini: ${ctxParts.join(' ')}`
        : roleCfg.systemInstruction;

    try {
      const ai = new GoogleGenAI({
        apiKey,
        httpOptions: {
          headers: {
            'User-Agent': 'aistudio-build',
          },
        },
      });

      const fallbackModel: 'gemini-4-hydrogen' | 'gemini-3.1-flash-lite' =
        roleCfg.model === 'gemini-4-hydrogen' ? 'gemini-3.1-flash-lite' : 'gemini-4-hydrogen';

      const invokeModel = async (
        targetModel: 'gemini-4-hydrogen' | 'gemini-3.1-flash-lite'
      ): Promise<string> => {
        const chat = ai.chats.create({
          model: targetModel,
          config: {
            systemInstruction: fullSystemInstruction,
            ...(targetModel === 'gemini-4-hydrogen'
              ? { thinkingConfig: { thinkingLevel: ThinkingLevel.LOW } }
              : {}),
          },
          history: formattedHistory,
        });
        const geminiTimeoutMs = 45_000;
        const response = await Promise.race([
          chat.sendMessage({ message: userMessage }),
          new Promise<never>((_, reject) => {
            setTimeout(() => {
              reject(new Error('GATEWAY_TIMEOUT: Permintaan ke Gemini API melebihi batas waktu 45 detik.'));
            }, geminiTimeoutMs);
          }),
        ]);
        return (response.text || '').trim();
      };

      const isTransientOverload = (err: unknown): boolean => {
        const msg = (err instanceof Error ? err.message : String(err)).toLowerCase();
        return (
          msg.includes('503') ||
          msg.includes('unavailable') ||
          msg.includes('high demand') ||
          msg.includes('overloaded') ||
          msg.includes('502')
        );
      };

      const attemptOrder: Array<{
        model: 'gemini-4-hydrogen' | 'gemini-3.1-flash-lite';
        delayMs: number;
      }> = [
        { model: roleCfg.model, delayMs: 0 },
        { model: fallbackModel, delayMs: 450 },
        { model: fallbackModel, delayMs: 900 },
      ];

      let replyText = '';
      let usedModel: 'gemini-4-hydrogen' | 'gemini-3.1-flash-lite' = roleCfg.model;
      let lastError: unknown = null;

      for (const attempt of attemptOrder) {
        if (attempt.delayMs > 0) {
          await new Promise((resolve) => setTimeout(resolve, attempt.delayMs));
        }
        try {
          replyText = await invokeModel(attempt.model);
          usedModel = attempt.model;
          lastError = null;
          break;
        } catch (err) {
          lastError = err;
          if (!isTransientOverload(err)) {
            throw err;
          }
        }
      }

      if (lastError) {
        throw lastError;
      }

      if (!replyText) {
        res.status(502).json({
          ok: false,
          error: 'Model tidak mengembalikan teks jawaban. Silakan coba lagi.',
        });
        return;
      }

      res.json({
        ok: true,
        reply: replyText,
        model: usedModel,
        roleId,
      });
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      const lower = msg.toLowerCase();
      if (lower.includes('gateway_timeout') || lower.includes('timed out') || lower.includes('etimedout')) {
        res.status(504).json({
          ok: false,
          error: 'Permintaan ke Gemini API melebihi batas waktu (timeout). Silakan coba pertanyaan yang lebih ringkas atau ulangi sesaat lagi.',
        });
        return;
      }
      if (
        lower.includes('503') ||
        lower.includes('unavailable') ||
        lower.includes('high demand') ||
        lower.includes('overloaded')
      ) {
        res.status(503).json({
          ok: false,
          error:
            'Server AI Gemini sedang mengalami lonjakan trafik sementara (503). Silakan tunggu beberapa detik lalu tekan Kirim Ulang.',
        });
        return;
      }
      if (lower.includes('403') || lower.includes('permission_denied') || lower.includes('api_key_invalid')) {
        res.status(403).json({
          ok: false,
          error:
            'Kunci Gemini API tidak valid atau akses ditolak. Periksa API key Anda pada panel Settings > Secrets.',
        });
        return;
      }
      if (lower.includes('429') || lower.includes('resource_exhausted')) {
        res.status(429).json({
          ok: false,
          error:
            'Batas kuota Gemini API sementara tercapai. Silakan coba sesaat lagi atau cek kuota di Settings > Secrets.',
        });
        return;
      }
      res.status(500).json({
        ok: false,
        error: `Gagal menghubungi asisten Gemini: ${msg}`,
      });
    }
  });

  // Server-side Google Geocoding API proxy for reverse geocoding map clicks
  app.get('/api/maps/reverse', async (req: Request, res: Response) => {
    const lat = Number(req.query.lat);
    const lng = Number(req.query.lng);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
      res.status(400).json({ error: 'Koordinat tidak valid.' });
      return;
    }

    const fallbackName = `Titik Peta (${lat.toFixed(3)}, ${lng.toFixed(3)})`;
    const fallbackAddress = `Koordinat GPS ${lat.toFixed(5)}, ${lng.toFixed(5)}`;
    const apiKey = getGoogleMapsApiKey();
    if (!apiKey) {
      res.json({ name: fallbackName, address: fallbackAddress });
      return;
    }

    const url = `https://maps.googleapis.com/maps/api/geocode/json?latlng=${encodeURIComponent(
      `${lat},${lng}`
    )}&language=id&key=${encodeURIComponent(apiKey)}`;

    try {
      const upstream = await fetch(url);
      if (upstream.status === 429) {
        res.status(429).json({
          quotaExceeded: true,
          name: fallbackName,
          address: fallbackAddress,
        });
        return;
      }

      const data = (await upstream.json()) as {
        status?: string;
        results?: Array<{
          formatted_address?: string;
          address_components?: Array<{
            long_name?: string;
            short_name?: string;
            types?: string[];
          }>;
        }>;
      };

      if (
        data.status === 'OVER_QUERY_LIMIT' ||
        data.status === 'RESOURCE_EXHAUSTED'
      ) {
        res.status(429).json({
          quotaExceeded: true,
          name: fallbackName,
          address: fallbackAddress,
        });
        return;
      }

      const first = data.results?.[0];
      if (!upstream.ok || !first) {
        res.json({ name: fallbackName, address: fallbackAddress });
        return;
      }

      const components = first.address_components ?? [];
      const findComponent = (types: string[]) =>
        components.find((c) =>
          types.some((t) => Array.isArray(c.types) && c.types.includes(t))
        )?.long_name;

      const street = findComponent(['route', 'premise', 'point_of_interest']);
      const area = findComponent([
        'sublocality_level_1',
        'sublocality',
        'locality',
        'administrative_area_level_2',
      ]);
      const name = [street, area].filter(Boolean).join(', ') || fallbackName;
      const address =
        first.formatted_address ||
        `${name} · ${lat.toFixed(4)}, ${lng.toFixed(4)}`;

      res.json({ name, address });
    } catch {
      res.json({ name: fallbackName, address: fallbackAddress });
    }
  });

  app.get('/api/auth/instagram/url', (req: Request, res: Response) => {
    const clientId = process.env.INSTAGRAM_CLIENT_ID?.trim();
    const baseUrl = resolveBaseUrl(req);
    const redirectUri = `${baseUrl}/auth/callback`;
    const callbackUrls = [
      `${DEV_FALLBACK_URL}/auth/callback`,
      `${SHARED_FALLBACK_URL}/auth/callback`,
    ];

    if (!clientId) {
      res.status(200).json({
        configured: false,
        redirectUri,
        callbackUrls,
        message:
          'INSTAGRAM_CLIENT_ID belum diatur di environment server AI Studio.',
      });
      return;
    }

    const params = new URLSearchParams({
      client_id: clientId,
      redirect_uri: redirectUri,
      response_type: 'code',
      scope: process.env.INSTAGRAM_OAUTH_SCOPE?.trim() || 'user_profile',
    });

    const providerAuthUrl =
      process.env.INSTAGRAM_AUTH_URL?.trim() ||
      'https://api.instagram.com/oauth/authorize';
    const authUrl = `${providerAuthUrl}?${params.toString()}`;

    res.json({
      configured: true,
      url: authUrl,
      redirectUri,
      callbackUrls,
    });
  });

  const handleInstagramCallback = async (req: Request, res: Response) => {
    const code = typeof req.query.code === 'string' ? req.query.code : '';
    const oauthError =
      typeof req.query.error_description === 'string'
        ? req.query.error_description
        : typeof req.query.error === 'string'
          ? req.query.error
          : '';

    const clientId = process.env.INSTAGRAM_CLIENT_ID?.trim();
    const clientSecret = process.env.INSTAGRAM_CLIENT_SECRET?.trim();
    const redirectUri = `${resolveBaseUrl(req)}/auth/callback`;

    if (oauthError || !code) {
      const errMsg = oauthError || 'Kode otorisasi Instagram tidak ditemukan.';
      const safeJson = JSON.stringify({
        type: 'OAUTH_AUTH_ERROR',
        provider: 'instagram',
        error: errMsg,
      });
      res.status(400).send(`
        <!doctype html>
        <html lang="id">
          <body style="font-family:sans-serif;padding:24px;text-align:center;">
            <h3>Autentikasi Instagram Gagal</h3>
            <p>${escapeHtmlLiteral(errMsg)}</p>
            <script>
              if (window.opener) {
                window.opener.postMessage(${safeJson}, '*');
                setTimeout(() => window.close(), 1200);
              }
            </script>
          </body>
        </html>
      `);
      return;
    }

    if (!clientId || !clientSecret) {
      const safeJson = JSON.stringify({
        type: 'OAUTH_AUTH_ERROR',
        provider: 'instagram',
        error: 'INSTAGRAM_CLIENT_ID / INSTAGRAM_CLIENT_SECRET belum dikonfigurasi.',
      });
      res.status(500).send(`
        <!doctype html>
        <html lang="id">
          <body style="font-family:sans-serif;padding:24px;text-align:center;">
            <h3>Konfigurasi Server Belum Lengkap</h3>
            <script>
              if (window.opener) {
                window.opener.postMessage(${safeJson}, '*');
                setTimeout(() => window.close(), 1200);
              }
            </script>
          </body>
        </html>
      `);
      return;
    }

    try {
      const formBody = new URLSearchParams({
        client_id: clientId,
        client_secret: clientSecret,
        grant_type: 'authorization_code',
        redirect_uri: redirectUri,
        code,
      });

      const tokenRes = await fetch('https://api.instagram.com/oauth/access_token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: formBody.toString(),
      });

      const tokenData = (await tokenRes.json()) as {
        access_token?: string;
        user_id?: string | number;
        error_message?: string;
      };

      if (!tokenRes.ok || !tokenData.access_token) {
        throw new Error(
          tokenData.error_message || 'Gagal menukar token OAuth Instagram.'
        );
      }

      const profileRes = await fetch(
        `https://graph.instagram.com/me?fields=id,username,name,profile_picture_url&access_token=${encodeURIComponent(tokenData.access_token)}`
      );
      const profileJson = (await profileRes.json()) as {
        id?: string;
        username?: string;
        name?: string;
        profile_picture_url?: string;
      };

      const username = profileJson.username || `ig_jamaah_${String(tokenData.user_id || '').slice(-4)}`;
      const fullName = profileJson.name || `@${username}`;
      const payload = {
        type: 'OAUTH_AUTH_SUCCESS',
        provider: 'instagram',
        profile: {
          id: String(profileJson.id || tokenData.user_id || ''),
          username,
          fullName,
          avatarPhotoUrl: profileJson.profile_picture_url || '',
        },
      };

      res.send(`
        <!doctype html>
        <html lang="id">
          <body style="font-family:sans-serif;padding:24px;text-align:center;">
            <h3>Instagram Terhubung!</h3>
            <p>Menyinkronkan akun @${escapeHtmlLiteral(username)} ke Safar Tracking...</p>
            <script>
              if (window.opener) {
                window.opener.postMessage(${JSON.stringify(payload)}, '*');
                window.close();
              } else {
                window.location.href = '/';
              }
            </script>
          </body>
        </html>
      `);
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : 'Terjadi kesalahan saat memverifikasi akun Instagram.';
      res.status(500).send(`
        <!doctype html>
        <html lang="id">
          <body style="font-family:sans-serif;padding:24px;text-align:center;">
            <h3>Verifikasi Instagram Gagal</h3>
            <p>${escapeHtmlLiteral(message)}</p>
            <script>
              if (window.opener) {
                window.opener.postMessage(${JSON.stringify({
                  type: 'OAUTH_AUTH_ERROR',
                  provider: 'instagram',
                  error: message,
                })}, '*');
                setTimeout(() => window.close(), 1500);
              }
            </script>
          </body>
        </html>
      `);
    }
  };

  app.get(['/auth/callback', '/auth/callback/'], handleInstagramCallback);

  // Proxy Firebase Auth & Hosting helper endpoints if hit on this origin
  let firebaseAuthDomain = 'excellent-victor-7v0ns.firebaseapp.com';
  try {
    const rawCfg = fs.readFileSync(path.join(__dirname, 'firebase-applet-config.json'), 'utf8');
    const parsedCfg = JSON.parse(rawCfg) as { authDomain?: string };
    if (parsedCfg.authDomain && parsedCfg.authDomain.trim()) {
      firebaseAuthDomain = parsedCfg.authDomain.trim();
    }
  } catch {
    // Keep default Safar Tracking authDomain
  }
  app.all(['/__/auth/*', '/__/firebase/*'], async (req: Request, res: Response) => {
    const targetUrl = `https://${firebaseAuthDomain}${req.originalUrl}`;
    try {
      const headers: Record<string, string> = {};
      const accept = req.get('accept');
      const contentType = req.get('content-type');
      if (accept) headers['accept'] = accept;
      if (contentType) headers['content-type'] = contentType;

      const hasBody = req.method !== 'GET' && req.method !== 'HEAD';
      const upstream = await fetch(targetUrl, {
        method: req.method,
        headers,
        body: hasBody && req.body && Object.keys(req.body).length > 0
          ? JSON.stringify(req.body)
          : undefined,
        redirect: 'manual',
      });

      const location = upstream.headers.get('location');
      if (location) {
        res.setHeader('Location', location);
      }
      const upstreamType = upstream.headers.get('content-type');
      if (upstreamType) {
        res.setHeader('Content-Type', upstreamType);
      }
      const buf = Buffer.from(await upstream.arrayBuffer());
      res.status(upstream.status).send(buf);
    } catch {
      res.redirect(302, targetUrl);
    }
  });

  app.all('/api/*', (_req: Request, res: Response) => {
    res.status(404).json({ ok: false, error: 'Endpoint API tidak ditemukan.' });
  });

  const distPath = path.join(__dirname, 'dist');
  const hasBuiltDist = fs.existsSync(path.join(distPath, 'index.html'));
  const useProdStatic =
    process.env.NODE_ENV === 'production' ||
    (process.env.NODE_ENV !== 'development' && hasBuiltDist);

  if (!useProdStatic) {
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(distPath));
    app.get('*', (_req: Request, res: Response) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  const PORT = Number(process.env.PORT) || 3000;
  const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`Safar Tracking server listening on http://0.0.0.0:${PORT}`);
  });
  server.requestTimeout = 90_000;
  server.headersTimeout = 65_000;
}

startServer().catch((err) => {
  console.error('Failed to start Safar Tracking server:', err);
  process.exit(1);
});
