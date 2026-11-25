package com.konvoiku.touring.data

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Lightweight local loopback HTTP server (http://localhost:<port>/auth) so Android users can
 * complete Google Sign-In via Chrome without requiring an externally deployed web bridge.
 * Works because Firebase project `konvoi-touring` authorizes `localhost` OAuth callbacks.
 */
class LocalBrowserGoogleAuthServer {

    private var serverSocket: ServerSocket? = null
    private var serverScope: CoroutineScope? = null
    private var timeoutJob: Job? = null

    @Volatile
    private var delivered = false

    fun start(
        onIdTokenReceived: (String) -> Unit,
        onErrorReceived: (String) -> Unit = {}
    ): Int {
        stop()
        delivered = false

        val socket = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        serverSocket = socket
        val port = socket.localPort
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serverScope = scope

        timeoutJob = scope.launch {
            delay(5 * 60_000L)
            stop()
        }

        scope.launch {
            while (isActive && !socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: break
                launch {
                    handleClient(
                        client = client,
                        port = port,
                        onIdTokenReceived = onIdTokenReceived,
                        onErrorReceived = onErrorReceived
                    )
                }
            }
        }
        return port
    }

    fun stop() {
        timeoutJob?.cancel()
        timeoutJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        serverScope?.cancel()
        serverScope = null
    }

    private fun handleClient(
        client: Socket,
        port: Int,
        onIdTokenReceived: (String) -> Unit,
        onErrorReceived: (String) -> Unit
    ) {
        runCatching {
            client.soTimeout = 10_000
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val requestLine = reader.readLine().orEmpty()
            var line = reader.readLine()
            while (!line.isNullOrEmpty()) {
                line = reader.readLine()
            }

            val rawTarget = requestLine.split(" ").getOrNull(1)?.trim().orEmpty().ifBlank { "/" }
            val uri = Uri.parse("http://localhost:$port$rawTarget")
            val path = uri.path.orEmpty()

            val out = client.getOutputStream()
            when {
                path.startsWith("/callback") -> {
                    val idToken = uri.getQueryParameter("id_token")?.trim().orEmpty()
                    val errorMsg = uri.getQueryParameter("error")?.trim().orEmpty()
                    if (idToken.isNotBlank()) {
                        writeHttpResponse(
                            out = out,
                            status = "200 OK",
                            contentType = "application/json; charset=utf-8",
                            body = """{"ok":true}"""
                        )
                        if (!delivered) {
                            delivered = true
                            onIdTokenReceived(idToken)
                            serverScope?.launch {
                                delay(2_500L)
                                stop()
                            }
                        }
                    } else {
                        writeHttpResponse(
                            out = out,
                            status = "400 Bad Request",
                            contentType = "application/json; charset=utf-8",
                            body = """{"ok":false}"""
                        )
                        if (errorMsg.isNotBlank() && !delivered) {
                            onErrorReceived(errorMsg)
                        }
                    }
                }
                path == "/favicon.ico" -> {
                    writeHttpResponse(
                        out = out,
                        status = "204 No Content",
                        contentType = "text/plain; charset=utf-8",
                        body = ""
                    )
                }
                else -> {
                    writeHttpResponse(
                        out = out,
                        status = "200 OK",
                        contentType = "text/html; charset=utf-8",
                        body = buildAuthPageHtml(port)
                    )
                }
            }
        }
        runCatching { client.close() }
    }

    private fun writeHttpResponse(
        out: OutputStream,
        status: String,
        contentType: String,
        body: String
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = buildString {
            append("HTTP/1.1 $status\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store, max-age=0\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n\r\n")
        }
        out.write(headers.toByteArray(Charsets.US_ASCII))
        if (bytes.isNotEmpty()) {
            out.write(bytes)
        }
        out.flush()
    }

    private fun buildAuthPageHtml(port: Int): String {
        val apiKey = FirestoreConvoyRepository.FIREBASE_WEB_API_KEY
        val authDomain = FirestoreConvoyRepository.FIREBASE_AUTH_DOMAIN
        val projectId = FirestoreConvoyRepository.FIREBASE_PROJECT_ID_PUBLIC
        val appId = FirestoreConvoyRepository.FIREBASE_WEB_APP_ID

        return """
            <!doctype html>
            <html lang="id">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>KonvoiKu — Login Google</title>
              <style>
                body {
                  margin: 0;
                  min-height: 100vh;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  background: #0f172a;
                  color: #f8fafc;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  padding: 20px;
                  box-sizing: border-box;
                }
                .card {
                  width: 100%;
                  max-width: 420px;
                  background: #1e293b;
                  border: 1px solid #334155;
                  border-radius: 20px;
                  padding: 24px;
                  box-shadow: 0 20px 50px rgba(0,0,0,0.35);
                }
                .badge {
                  display: inline-block;
                  font-size: 12px;
                  font-weight: 700;
                  color: #38bdf8;
                  background: rgba(56, 189, 248, 0.14);
                  padding: 5px 10px;
                  border-radius: 999px;
                  margin-bottom: 12px;
                }
                h1 { margin: 0 0 8px; font-size: 22px; }
                p { margin: 0 0 18px; color: #cbd5e1; font-size: 14px; line-height: 1.5; }
                .btn {
                  width: 100%;
                  display: block;
                  text-align: center;
                  text-decoration: none;
                  border: none;
                  border-radius: 14px;
                  padding: 14px 16px;
                  font-size: 15px;
                  font-weight: 700;
                  cursor: pointer;
                  box-sizing: border-box;
                }
                .btn-primary {
                  background: #ea580c;
                  color: #ffffff;
                }
                .btn-secondary {
                  background: #334155;
                  color: #e2e8f0;
                  margin-top: 10px;
                  font-size: 13px;
                  padding: 11px 14px;
                }
                .btn-success {
                  background: #16a34a;
                  color: #ffffff;
                  margin-top: 12px;
                }
                .status {
                  margin-top: 14px;
                  padding: 12px;
                  border-radius: 12px;
                  font-size: 13px;
                  line-height: 1.45;
                  display: none;
                }
                .status.ok { display: block; background: rgba(22, 163, 74, 0.18); border: 1px solid #22c55e; color: #bbf7d0; }
                .status.err { display: block; background: rgba(220, 38, 38, 0.18); border: 1px solid #ef4444; color: #fecaca; }
                .status.info { display: block; background: rgba(37, 99, 235, 0.18); border: 1px solid #60a5fa; color: #dbeafe; }
                .token-box {
                  margin-top: 10px;
                  width: 100%;
                  padding: 10px;
                  border-radius: 10px;
                  background: #0f172a;
                  color: #93c5fd;
                  border: 1px solid #334155;
                  font-family: monospace;
                  font-size: 11px;
                  box-sizing: border-box;
                  word-break: break-all;
                  display: none;
                }
              </style>
            </head>
            <body>
              <div class="card">
                <div class="badge">KonvoiKu · Sinkronisasi Akun Google</div>
                <h1>Masuk dengan Google</h1>
                <p>Ketuk tombol di bawah untuk memilih akun Google Anda. Setelah selesai, aplikasi KonvoiKu akan otomatis menerima sesi Anda.</p>

                <button id="loginPopupBtn" class="btn btn-primary" onclick="startGooglePopup()">
                  Pilih Akun Google
                </button>
                <button id="loginRedirectBtn" class="btn btn-secondary" onclick="startGoogleRedirect()">
                  Alternatif: Mode Redirect Penuh
                </button>

                <div id="statusBox" class="status"></div>
                <a id="openAppLink" class="btn btn-success" style="display:none;" href="#">
                  Buka Aplikasi KonvoiKu
                </a>
                <textarea id="tokenOutput" class="token-box" rows="3" readonly onclick="this.select()"></textarea>
              </div>

              <script src="https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js"></script>
              <script src="https://www.gstatic.com/firebasejs/10.14.1/firebase-auth-compat.js"></script>
              <script>
                const firebaseConfig = {
                  apiKey: "$apiKey",
                  authDomain: "$authDomain",
                  projectId: "$projectId",
                  appId: "$appId"
                };
                if (!firebase.apps.length) {
                  firebase.initializeApp(firebaseConfig);
                }
                const auth = firebase.auth();
                const statusBox = document.getElementById('statusBox');
                const openAppLink = document.getElementById('openAppLink');
                const tokenOutput = document.getElementById('tokenOutput');

                function setStatus(type, msg) {
                  statusBox.className = 'status ' + type;
                  statusBox.textContent = msg;
                }

                async function deliverToken(result) {
                  const cred = firebase.auth.GoogleAuthProvider.credentialFromResult(result);
                  const idToken = (cred && cred.idToken) || (result && result.credential && result.credential.idToken) || '';
                  const email = result && result.user && result.user.email ? result.user.email : '';
                  if (!idToken) {
                    setStatus('err', 'Token Google tidak tersedia dari hasil login. Coba pilih ulang akun.');
                    return;
                  }
                  const deepLink = 'konvoiku://auth?id_token=' + encodeURIComponent(idToken);
                  openAppLink.href = deepLink;
                  openAppLink.style.display = 'block';
                  tokenOutput.value = deepLink;
                  tokenOutput.style.display = 'block';
                  setStatus('ok', 'Berhasil masuk sebagai ' + (email || 'pengguna Google') + '! Menyinkronkan ke aplikasi KonvoiKu...');

                  try {
                    await fetch('http://localhost:$port/callback?id_token=' + encodeURIComponent(idToken) + '&email=' + encodeURIComponent(email));
                  } catch (e) {}

                  setTimeout(() => {
                    window.location.href = deepLink;
                  }, 300);
                }

                async function startGooglePopup() {
                  try {
                    setStatus('info', 'Membuka pilihan akun Google...');
                    const provider = new firebase.auth.GoogleAuthProvider();
                    provider.setCustomParameters({ prompt: 'select_account' });
                    const result = await auth.signInWithPopup(provider);
                    await deliverToken(result);
                  } catch (err) {
                    const code = err && err.code ? err.code : '';
                    if (code === 'auth/popup-blocked' || code === 'auth/operation-not-supported-in-this-environment') {
                      startGoogleRedirect();
                      return;
                    }
                    const msg = err && err.message ? err.message : String(err);
                    setStatus('err', 'Login Google gagal (' + code + '): ' + msg + ' — Coba tombol "Mode Redirect Penuh" di bawah.');
                  }
                }

                async function startGoogleRedirect() {
                  try {
                    setStatus('info', 'Mengalihkan ke halaman login Google...');
                    const provider = new firebase.auth.GoogleAuthProvider();
                    provider.setCustomParameters({ prompt: 'select_account' });
                    await auth.signInWithRedirect(provider);
                  } catch (err) {
                    setStatus('err', 'Gagal memulai redirect: ' + (err && err.message ? err.message : err));
                  }
                }

                auth.getRedirectResult().then((result) => {
                  if (result && result.user) {
                    deliverToken(result);
                  }
                }).catch((err) => {
                  if (err && err.code) {
                    setStatus('err', 'Gagal memproses redirect (' + err.code + '): ' + err.message);
                  }
                });
              </script>
            </body>
            </html>
        """.trimIndent()
    }
}
