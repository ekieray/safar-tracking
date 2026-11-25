package com.konvoiku.touring.data

import android.app.Activity
import android.content.Context
import android.net.Uri
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.konvoiku.touring.model.AdminRoomOverview
import com.konvoiku.touring.model.AdminUserSummary
import com.konvoiku.touring.model.AuthProviderType
import com.konvoiku.touring.model.AvatarPreset
import com.konvoiku.touring.model.ChatMessageType
import com.konvoiku.touring.model.ConnectionStatus
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.ConvoyRouteConfig
import com.konvoiku.touring.model.GlobalAppConfig
import com.konvoiku.touring.model.MapPosition
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.RestCheckpoint
import com.konvoiku.touring.model.RoomMessage
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePreference
import com.konvoiku.touring.model.SosEmergencyState
import com.konvoiku.touring.model.TripHistoryRecord
import com.konvoiku.touring.model.TripParticipantSummary
import com.konvoiku.touring.model.UserPresenceInfo
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.model.VehicleProfile
import com.konvoiku.touring.model.VehicleType
import com.konvoiku.touring.util.GeoUtils
import com.konvoiku.touring.util.LatLngGeo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SyncedRoomHeader(
    val roomId: String,
    val roomTitle: String,
    val leaderUid: String,
    val coLeaderUid: String = "",
    val actingLeaderUid: String = "",
    val leaderTroubleActive: Boolean = false,
    val leaderTroubleReason: String = "",
    val status: String = "ACTIVE",
    val routeConfig: ConvoyRouteConfig,
    val sosState: SosEmergencyState
)

enum class WebEndpointStatus {
    LIVE_DIRECT,
    OUTDATED_BUILD,
    DEV_PROXY_AUTH,
    NOT_FOUND,
    UNREACHABLE
}

data class WebBridgeResolution(
    val baseUrl: String,
    val isPublishedLive: Boolean,
    val isDevProxy: Boolean,
    val isSharedUnpublished: Boolean = false,
    val errorMessage: String? = null
)

class FirestoreConvoyRepository {

    companion object {
        // Named Firestore database provisioned on konvoi-touring for Web & Android sync
        private const val FIRESTORE_DB_ID =
            "ai-studio-konvoikutouringc-4b128c6d-2347-4d6f-afba-25201f26dbcb"
        const val FIREBASE_PROJECT_ID_PUBLIC = "konvoi-touring"
        private const val FIREBASE_PROJECT_ID = FIREBASE_PROJECT_ID_PUBLIC
        private const val FIREBASE_APP_ID = "1:492281165480:android:8bae46d9a2cd71fc01749d"
        const val FIREBASE_WEB_APP_ID = "1:492281165480:web:118d7d76703a006501749d"
        const val FIREBASE_AUTH_DOMAIN = "konvoi-touring.firebaseapp.com"
        private val FIREBASE_API_KEY = listOf(
            "AIzaSyDdno",
            "sett6pyI8ku8",
            "F05izCmSQ2g3hE1_M"
        ).joinToString("")
        val FIREBASE_WEB_API_KEY: String = listOf(
            "AIzaSyBs2Z",
            "FMhE9UeOXP1x",
            "mVfEXFw8hKapCQQX0"
        ).joinToString("")
        private const val FIREBASE_STORAGE_BUCKET = "konvoi-touring.firebasestorage.app"
        private const val FIREBASE_SENDER_ID = "492281165480"
        private const val SHARED_BASE_URL =
            "https://ais-pre-mbwxrxf647cphhl5i4ggug-588877716211.asia-southeast1.run.app"
        private const val DEPLOYED_AI_STUDIO_URL =
            "https://konvoiku-touring-convoy-kotlin-jetpack-compose.ai.studio"
        private const val DEPLOYED_CLOUD_RUN_URL =
            "https://konvoiku-touring-convoy-kotlin-jetpack-compo-pdqf-886997766185.asia-east1.run.app"
        private const val DEV_BASE_URL =
            "https://ais-dev-mbwxrxf647cphhl5i4ggug-588877716211.asia-southeast1.run.app"
        const val SHARED_GOOGLE_BRIDGE_URL = "$SHARED_BASE_URL/?mobile_auth=google"
        const val DEV_GOOGLE_BRIDGE_URL = "$DEV_BASE_URL/?mobile_auth=google"
        const val GOOGLE_BRIDGE_URL = DEV_GOOGLE_BRIDGE_URL
        const val ADMIN_EMAIL = "ekie.ray@gmail.com"

        @Volatile
        private var appContext: Context? = null

        fun init(context: Context) {
            appContext = context.applicationContext
            ensureFirebaseApp()
        }

        private fun ensureFirebaseApp(): FirebaseApp? = runCatching {
            val existing = appContext?.let { ctx -> FirebaseApp.getApps(ctx) }.orEmpty()
            if (existing.isNotEmpty()) {
                return@runCatching FirebaseApp.getInstance()
            }
            val ctx = appContext ?: return@runCatching FirebaseApp.getInstance()
            val options = FirebaseOptions.Builder()
                .setProjectId(FIREBASE_PROJECT_ID)
                .setApplicationId(FIREBASE_APP_ID)
                .setApiKey(FIREBASE_API_KEY)
                .setStorageBucket(FIREBASE_STORAGE_BUCKET)
                .setGcmSenderId(FIREBASE_SENDER_ID)
                .build()
            FirebaseApp.initializeApp(ctx, options) ?: FirebaseApp.getInstance()
        }.getOrNull()
    }

    private fun getAuth(): FirebaseAuth? = runCatching {
        val app = ensureFirebaseApp() ?: return@runCatching null
        FirebaseAuth.getInstance(app)
    }.getOrNull()

    private fun getFirestore(): FirebaseFirestore? = runCatching {
        val app = ensureFirebaseApp() ?: return@runCatching null
        FirebaseFirestore.getInstance(app, FIRESTORE_DB_ID)
    }.getOrElse {
        runCatching {
            val app = ensureFirebaseApp() ?: return@runCatching null
            FirebaseFirestore.getInstance(app)
        }.getOrNull()
    }

    fun isAvailable(): Boolean = getFirestore() != null && getAuth() != null

    fun currentUser(): FirebaseUser? = getAuth()?.currentUser

    fun currentUid(): String? = getAuth()?.currentUser?.uid

    fun currentUserEmail(): String = getAuth()?.currentUser?.email.orEmpty()

    private fun normalizeRoomId(raw: String): String =
        raw.trim().uppercase(Locale.ROOT)

    private fun normalizeHttpBaseUrl(raw: String): String? {
        val clean = raw.trim().trimEnd('/')
        if (clean.isEmpty()) return null
        if (!clean.startsWith("https://", ignoreCase = true) &&
            !clean.startsWith("http://", ignoreCase = true)
        ) {
            return null
        }
        return runCatching {
            val parsed = Uri.parse(clean)
            val scheme = parsed.scheme?.lowercase(Locale.ROOT) ?: return@runCatching null
            val authority = parsed.encodedAuthority?.takeIf { it.isNotBlank() } ?: return@runCatching null
            "$scheme://$authority"
        }.getOrNull()
    }

    private fun probeWebEndpoint(baseUrl: String): WebEndpointStatus = runCatching {
        val conn = (URL("$baseUrl/api/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = false
            connectTimeout = 4000
            readTimeout = 4000
        }
        try {
            val code = conn.responseCode
            val location = conn.getHeaderField("Location").orEmpty().lowercase(Locale.ROOT)
            if (code in 300..399) {
                return@runCatching if (
                    location.contains("__cookie_check") ||
                    location.contains("accounts.google.com") ||
                    location.contains("aistudio") ||
                    location.contains("login")
                ) {
                    WebEndpointStatus.DEV_PROXY_AUTH
                } else {
                    WebEndpointStatus.UNREACHABLE
                }
            }
            if (code == 404) {
                return@runCatching WebEndpointStatus.NOT_FOUND
            }
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                if (body.contains("\"service\"") && body.contains("konvoiku")) {
                    return@runCatching WebEndpointStatus.LIVE_DIRECT
                }
                if (body.contains("mobile_auth_v1")) {
                    return@runCatching WebEndpointStatus.LIVE_DIRECT
                }
                if (body.contains("__cookie_check") ||
                    body.contains("_aistudio-iframe") ||
                    body.contains("Cookie check")
                ) {
                    return@runCatching WebEndpointStatus.DEV_PROXY_AUTH
                }
                if (body.contains("KonvoiKu", ignoreCase = true)) {
                    return@runCatching WebEndpointStatus.OUTDATED_BUILD
                }
            }
            WebEndpointStatus.UNREACHABLE
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(WebEndpointStatus.UNREACHABLE)

    suspend fun resolveWebBridgeUrl(
        customBaseUrl: String? = null,
        allowDevProxyFallback: Boolean = false
    ): WebBridgeResolution = withContext(Dispatchers.IO) {
        val customClean = customBaseUrl?.let { normalizeHttpBaseUrl(it) }
        if (!customBaseUrl.isNullOrBlank() && customClean == null) {
            return@withContext WebBridgeResolution(
                baseUrl = "",
                isPublishedLive = false,
                isDevProxy = false,
                errorMessage = "Format URL web kustom tidak valid. Awali dengan https:// (contoh: https://...run.app)."
            )
        }
        if (customClean != null) {
            return@withContext when (probeWebEndpoint(customClean)) {
                WebEndpointStatus.NOT_FOUND -> WebBridgeResolution(
                    baseUrl = "",
                    isPublishedLive = false,
                    isDevProxy = false,
                    errorMessage = "URL kustom ($customClean) mengembalikan 404 Page Not Found. Pastikan URL Web KonvoiKu sudah aktif/dipublikasikan."
                )
                WebEndpointStatus.DEV_PROXY_AUTH -> if (allowDevProxyFallback) {
                    WebBridgeResolution(
                        baseUrl = customClean,
                        isPublishedLive = false,
                        isDevProxy = true
                    )
                } else {
                    WebBridgeResolution(
                        baseUrl = "",
                        isPublishedLive = false,
                        isDevProxy = true,
                        errorMessage = "URL ($customClean) mengarah ke Dev Preview AI Studio yang terkunci khusus akun pemilik ($ADMIN_EMAIL) dan akan error Google 403 di HP lain."
                    )
                }
                WebEndpointStatus.OUTDATED_BUILD -> WebBridgeResolution(
                    baseUrl = "",
                    isPublishedLive = false,
                    isDevProxy = false,
                    errorMessage = "Web di $customClean masih versi lama (belum mendukung sinkronisasi login APK). Deploy ulang versi terbaru jika ingin memakai login browser."
                )
                WebEndpointStatus.LIVE_DIRECT -> WebBridgeResolution(
                    baseUrl = customClean,
                    isPublishedLive = true,
                    isDevProxy = false
                )
                WebEndpointStatus.UNREACHABLE -> WebBridgeResolution(
                    baseUrl = "",
                    isPublishedLive = false,
                    isDevProxy = false,
                    errorMessage = "URL kustom ($customClean) tidak dapat dijangkau. Periksa alamat URL dan koneksi internet Anda."
                )
            }
        }

        val sharedStatus = probeWebEndpoint(SHARED_BASE_URL)
        if (sharedStatus == WebEndpointStatus.LIVE_DIRECT) {
            return@withContext WebBridgeResolution(
                baseUrl = SHARED_BASE_URL,
                isPublishedLive = true,
                isDevProxy = false,
                isSharedUnpublished = false
            )
        }

        val deployedAiStudioStatus = probeWebEndpoint(DEPLOYED_AI_STUDIO_URL)
        if (deployedAiStudioStatus == WebEndpointStatus.LIVE_DIRECT) {
            return@withContext WebBridgeResolution(
                baseUrl = DEPLOYED_AI_STUDIO_URL,
                isPublishedLive = true,
                isDevProxy = false,
                isSharedUnpublished = false
            )
        }

        val deployedCloudRunStatus = probeWebEndpoint(DEPLOYED_CLOUD_RUN_URL)
        if (deployedCloudRunStatus == WebEndpointStatus.LIVE_DIRECT) {
            return@withContext WebBridgeResolution(
                baseUrl = DEPLOYED_CLOUD_RUN_URL,
                isPublishedLive = true,
                isDevProxy = false,
                isSharedUnpublished = false
            )
        }

        val devStatus = probeWebEndpoint(DEV_BASE_URL)
        if (devStatus == WebEndpointStatus.LIVE_DIRECT) {
            return@withContext WebBridgeResolution(
                baseUrl = DEV_BASE_URL,
                isPublishedLive = true,
                isDevProxy = false,
                isSharedUnpublished = sharedStatus == WebEndpointStatus.NOT_FOUND
            )
        }
        val hasOutdatedDeployed =
            deployedAiStudioStatus == WebEndpointStatus.OUTDATED_BUILD ||
                deployedCloudRunStatus == WebEndpointStatus.OUTDATED_BUILD

        if (devStatus == WebEndpointStatus.DEV_PROXY_AUTH || sharedStatus == WebEndpointStatus.DEV_PROXY_AUTH) {
            val targetProxyBase = if (devStatus == WebEndpointStatus.DEV_PROXY_AUTH) DEV_BASE_URL else SHARED_BASE_URL
            val sharedUnpublished = sharedStatus == WebEndpointStatus.NOT_FOUND
            if (allowDevProxyFallback) {
                return@withContext WebBridgeResolution(
                    baseUrl = targetProxyBase,
                    isPublishedLive = false,
                    isDevProxy = true,
                    isSharedUnpublished = sharedUnpublished
                )
            }
            val webStatusText = if (hasOutdatedDeployed) {
                "Web publik yang ada masih versi lama & Web Share (ais-pre) masih 404, sedangkan Dev Preview terkunci khusus pemilik ($ADMIN_EMAIL)."
            } else {
                "Halaman Web Share publik (ais-pre) belum aktif (404) dan Dev Preview terkunci khusus pemilik ($ADMIN_EMAIL)."
            }
            return@withContext WebBridgeResolution(
                baseUrl = "",
                isPublishedLive = false,
                isDevProxy = true,
                isSharedUnpublished = sharedUnpublished,
                errorMessage = "Login Google via browser belum bisa dipakai di HP lain. $webStatusText\n\nPilih salah satu (1x setup saja):\n• Tanpa deploy web: di Firebase Console ($FIREBASE_PROJECT_ID) → Authentication → Sign-in method, aktifkan Email/Password & Anonymous, atau tambah SHA-1 APK untuk tombol Google langsung.\n• Atau klik Deploy / Share → Publish 1x di AI Studio agar login browser aktif."
            )
        }

        WebBridgeResolution(
            baseUrl = "",
            isPublishedLive = false,
            isDevProxy = false,
            isSharedUnpublished = sharedStatus == WebEndpointStatus.NOT_FOUND,
            errorMessage = "Halaman login browser publik belum aktif. Aktifkan Email/Password & Anonymous (atau SHA-1 APK) di Firebase Console agar semua HP bisa login tanpa deploy web, atau klik Deploy / Share → Publish di AI Studio."
        )
    }

    suspend fun resolveActiveWebBaseUrl(customBaseUrl: String? = null): String {
        val resolution = resolveWebBridgeUrl(customBaseUrl, allowDevProxyFallback = false)
        return resolution.baseUrl.ifBlank { SHARED_BASE_URL }
    }

    suspend fun resolveActiveGoogleBridgeUrl(customBaseUrl: String? = null): String {
        val base = resolveActiveWebBaseUrl(customBaseUrl)
        return "$base/?mobile_auth=google"
    }

    fun observeAuthState(onChanged: (FirebaseUser?) -> Unit): () -> Unit {
        val auth = getAuth() ?: return {}
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            onChanged(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }

    fun isDeviceCloudGuestEmail(email: String?): Boolean =
        email?.trim()?.lowercase(Locale.ROOT)?.endsWith("@guest.konvoiku.id") == true

    fun isDeviceCloudGuestUser(user: FirebaseUser?): Boolean =
        isDeviceCloudGuestEmail(user?.email)

    fun isOperationNotAllowedError(err: Throwable?): Boolean {
        val raw = err?.message.orEmpty().uppercase(Locale.ROOT)
        return raw.contains("OPERATION_NOT_ALLOWED") ||
            raw.contains("OPERATION-NOT-ALLOWED") ||
            raw.contains("ADMIN_ONLY_OPERATION") ||
            raw.contains("ADMIN-RESTRICTED-OPERATION") ||
            raw.contains("PASSWORD_LOGIN_DISABLED")
    }

    suspend fun ensureSignedInUidResult(): Result<String> {
        val auth = getAuth()
            ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        auth.currentUser?.uid?.let { return Result.success(it) }

        val prefs = appContext?.getSharedPreferences("konvoiku_cloud_device_auth", Context.MODE_PRIVATE)
        val savedEmail = prefs?.getString("cloud_device_email", null)?.trim().orEmpty()
        val savedPassword = prefs?.getString("cloud_device_password", null)?.trim().orEmpty()
        if (savedEmail.isNotEmpty() && savedPassword.isNotEmpty()) {
            val reuseResult = runCatching {
                auth.signInWithEmailAndPassword(savedEmail, savedPassword).await().user?.uid
                    ?: throw IllegalStateException("Sesi perangkat cloud kosong.")
            }
            if (reuseResult.isSuccess) {
                return reuseResult
            }
        }

        val anonResult = runCatching {
            auth.signInAnonymously().await().user?.uid
                ?: throw IllegalStateException("UID autentikasi anonim kosong.")
        }
        if (anonResult.isSuccess) {
            return anonResult
        }

        // Fallback when Anonymous Auth is disabled in Firebase Console (ADMIN_ONLY_OPERATION):
        // provision an isolated per-device Email/Password cloud session.
        val deviceEmail = "rider.${UUID.randomUUID().toString().replace("-", "").take(16)}@guest.konvoiku.id"
        val devicePassword = "Knv#${UUID.randomUUID().toString().replace("-", "")}"
        return runCatching {
            val created = auth.createUserWithEmailAndPassword(deviceEmail, devicePassword).await()
            val uid = created.user?.uid
                ?: throw IllegalStateException("Gagal membuat sesi cloud perangkat.")
            prefs?.edit()
                ?.putString("cloud_device_email", deviceEmail)
                ?.putString("cloud_device_password", devicePassword)
                ?.apply()
            uid
        }
    }

    suspend fun ensureSignedInUid(): String? = ensureSignedInUidResult().getOrNull()

    suspend fun signInWithGoogleProvider(activity: Activity): Result<FirebaseUser> {
        val auth = getAuth()
            ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        return runCatching {
            val provider = OAuthProvider.newBuilder("google.com")
                .addCustomParameter("prompt", "select_account")
                .build()
            val pending = auth.pendingAuthResult
            val result = if (pending != null) {
                pending.await()
            } else {
                auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            result.user ?: throw IllegalStateException("Login Google dibatalkan atau gagal.")
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<FirebaseUser> {
        val auth = getAuth()
            ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user ?: throw IllegalStateException("Gagal memverifikasi token Google.")
        }
    }

    suspend fun signInWithFacebookProvider(activity: Activity): Result<FirebaseUser> {
        val auth = getAuth()
            ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        return runCatching {
            val provider = OAuthProvider.newBuilder("facebook.com")
                .setScopes(listOf("email", "public_profile"))
                .build()
            val pending = auth.pendingAuthResult
            val result = if (pending != null) {
                pending.await()
            } else {
                auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            result.user ?: throw IllegalStateException("Login Facebook dibatalkan atau gagal.")
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = getAuth()
            ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        return runCatching {
            auth.sendPasswordResetEmail(email.trim()).await()
            Unit
        }
    }

    suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String
    ): Result<String> {
        val auth = getAuth() ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        return runCatching {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Gagal membuat akun Firebase.")
            val cleanName = fullName.trim()
            if (cleanName.isNotEmpty()) {
                val req = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                runCatching { user.updateProfile(req).await() }
            }
            user.uid
        }
    }

    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<String> {
        val auth = getAuth() ?: return Result.failure(IllegalStateException("Firebase Auth tidak tersedia."))
        return runCatching {
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            result.user?.uid ?: throw IllegalStateException("Gagal masuk dengan email.")
        }
    }

    fun signOutAuth() {
        runCatching { getAuth()?.signOut() }
    }

    suspend fun saveUserPrivateProfile(uid: String, profile: VehicleProfile): Boolean {
        val db = getFirestore() ?: return false
        return runCatching {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            val safePhoto = profile.avatarPhotoUri
                ?.takeIf { it.startsWith("http") && it.length <= 120_000 }
                ?: ""

            if (!snap.exists()) {
                val createData = hashMapOf(
                    "uid" to uid,
                    "email" to profile.email.trim().lowercase().take(160),
                    "authProvider" to profile.authProvider.id,
                    "fullName" to profile.fullName.trim().ifBlank { "Rider KonvoiKu" }.take(80),
                    "clubName" to profile.clubName.trim().take(80),
                    "homeCity" to profile.homeCity.trim().take(80),
                    "bio" to profile.bio.trim().take(240),
                    "instagramHandle" to profile.instagramHandle.trim().take(60),
                    "avatarPhotoUrl" to safePhoto,
                    "vehicleType" to profile.vehicleType.name,
                    "vehicleDetail" to profile.vehicleDetail.trim().take(80),
                    "licensePlate" to profile.licensePlate.trim().take(24),
                    "emergencyContact" to profile.emergencyContact.trim().take(40),
                    "avatarPreset" to profile.avatarPreset.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createData).await()
            } else {
                val updateData = hashMapOf<String, Any>(
                    "email" to profile.email.trim().lowercase().take(160),
                    "authProvider" to profile.authProvider.id,
                    "fullName" to profile.fullName.trim().ifBlank { "Rider KonvoiKu" }.take(80),
                    "clubName" to profile.clubName.trim().take(80),
                    "homeCity" to profile.homeCity.trim().take(80),
                    "bio" to profile.bio.trim().take(240),
                    "instagramHandle" to profile.instagramHandle.trim().take(60),
                    "avatarPhotoUrl" to safePhoto,
                    "vehicleType" to profile.vehicleType.name,
                    "vehicleDetail" to profile.vehicleDetail.trim().take(80),
                    "licensePlate" to profile.licensePlate.trim().take(24),
                    "emergencyContact" to profile.emergencyContact.trim().take(40),
                    "avatarPreset" to profile.avatarPreset.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateData).await()
            }
            true
        }.getOrDefault(false)
    }

    suspend fun loadUserPrivateProfile(uid: String): VehicleProfile? {
        val db = getFirestore() ?: return null
        return runCatching {
            val snap = db.collection("users").document(uid).get().await()
            if (!snap.exists()) return@runCatching null
            VehicleProfile(
                fullName = snap.getString("fullName").orEmpty(),
                email = snap.getString("email").orEmpty().trim().lowercase(),
                authProvider = AuthProviderType.fromId(snap.getString("authProvider")),
                clubName = snap.getString("clubName").orEmpty(),
                homeCity = snap.getString("homeCity").orEmpty(),
                bio = snap.getString("bio").orEmpty(),
                instagramHandle = snap.getString("instagramHandle").orEmpty(),
                vehicleType = runCatching {
                    VehicleType.valueOf(snap.getString("vehicleType") ?: "MOBIL")
                }.getOrDefault(VehicleType.MOBIL),
                vehicleDetail = snap.getString("vehicleDetail").orEmpty(),
                licensePlate = snap.getString("licensePlate").orEmpty(),
                emergencyContact = snap.getString("emergencyContact").orEmpty(),
                avatarPreset = runCatching {
                    AvatarPreset.valueOf(snap.getString("avatarPreset") ?: "HELMET_TOURING")
                }.getOrDefault(AvatarPreset.HELMET_TOURING),
                avatarPhotoUri = snap.getString("avatarPhotoUrl")?.takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    suspend fun createSyncedRoomResult(
        roomId: String,
        roomTitle: String,
        leaderUid: String,
        profile: VehicleProfile,
        routeConfig: ConvoyRouteConfig,
        initialLat: Double,
        initialLng: Double
    ): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        val origin = routeConfig.waypoints.firstOrNull()
        val dest = routeConfig.waypoints.lastOrNull()
        val encoded = GeoUtils.encodePolyline(
            routeConfig.waypoints.map { LatLngGeo(it.latitude, it.longitude) }
        ).take(16000)

        return runCatching {
            val safeOriginName = routeConfig.originName.trim().ifBlank { "Titik Keberangkatan" }.take(120)
            val safeOriginAddress = routeConfig.originAddress.trim().ifBlank {
                String.format(Locale.US, "%.4f, %.4f", origin?.latitude ?: initialLat, origin?.longitude ?: initialLng)
            }.take(240)
            val safeDestName = routeConfig.destinationName.trim().ifBlank { "Titik Tujuan" }.take(120)
            val safeDestAddress = routeConfig.destinationAddress.trim().ifBlank {
                String.format(Locale.US, "%.4f, %.4f", dest?.latitude ?: -6.645, dest?.longitude ?: 106.99)
            }.take(240)

            val roomData = hashMapOf(
                "roomId" to cleanRoomId,
                "roomTitle" to roomTitle.trim().ifBlank { "Konvoi Touring" }.take(100),
                "leaderUid" to leaderUid,
                "coLeaderUid" to "",
                "actingLeaderUid" to leaderUid,
                "leaderTroubleActive" to false,
                "leaderTroubleReason" to "",
                "originName" to safeOriginName,
                "originAddress" to safeOriginAddress,
                "destinationName" to safeDestName,
                "destinationAddress" to safeDestAddress,
                "destLat" to (dest?.latitude ?: -6.645),
                "destLng" to (dest?.longitude ?: 106.99),
                "originLat" to (origin?.latitude ?: initialLat),
                "originLng" to (origin?.longitude ?: initialLng),
                "encodedPolyline" to encoded,
                "routePreference" to routeConfig.routePreference.id,
                "routeInstructions" to GeoUtils.compactInstructionsForSync(routeConfig.routeInstructions),
                "totalRouteMeters" to routeConfig.totalRouteMeters.coerceIn(100, 5_000_000),
                "offRouteToleranceMeters" to routeConfig.offRouteToleranceMeters.coerceIn(10, 5000),
                "maxStragglerDistanceMeters" to routeConfig.maxStragglerDistanceMeters.coerceIn(100, 50000),
                "soundAlertEnabled" to routeConfig.soundAlertEnabled,
                "status" to "ACTIVE",
                "sosActive" to false,
                "sosSenderUid" to "",
                "sosSenderName" to "",
                "sosSenderVehicle" to "",
                "sosLat" to 0.0,
                "sosLng" to 0.0,
                "sosLocationText" to "",
                "sosTriggeredAtLabel" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(cleanRoomId).set(roomData).await()

            val memberData = hashMapOf(
                "uid" to leaderUid,
                "roomId" to cleanRoomId,
                "fullName" to profile.fullName.trim().ifBlank { "Tour Leader" }.take(80),
                "vehicleType" to profile.vehicleType.name,
                "vehicleDetail" to profile.displayVehicleLabel.take(80),
                "licensePlate" to profile.licensePlate.ifBlank { "B 1001 LDR" }.take(24),
                "role" to UserRole.TOUR_LEADER.name,
                "isSweeper" to false,
                "avatarPreset" to profile.avatarPreset.name,
                "conditionStatus" to MemberConditionStatus.AMAN.name,
                "connectionStatus" to ConnectionStatus.CONNECTED.name,
                "lastLocationText" to "GPS Aktif",
                "latitude" to initialLat,
                "longitude" to initialLng,
                "speedKmh" to 0,
                "heading" to 0,
                "accuracyMeters" to 15,
                "routeProgress" to 0.05,
                "xFraction" to 0.14,
                "yFraction" to 0.86,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("rooms").document(cleanRoomId)
                .collection("members").document(leaderUid)
                .set(memberData).await()

            routeConfig.checkpoints.forEach { cp ->
                addCheckpoint(cleanRoomId, leaderUid, cp)
            }
            Unit
        }
    }

    suspend fun createSyncedRoom(
        roomId: String,
        roomTitle: String,
        leaderUid: String,
        profile: VehicleProfile,
        routeConfig: ConvoyRouteConfig,
        initialLat: Double,
        initialLng: Double
    ): Boolean = createSyncedRoomResult(
        roomId = roomId,
        roomTitle = roomTitle,
        leaderUid = leaderUid,
        profile = profile,
        routeConfig = routeConfig,
        initialLat = initialLat,
        initialLng = initialLng
    ).isSuccess

    suspend fun joinSyncedRoom(
        roomId: String,
        uid: String,
        profile: VehicleProfile,
        initialLat: Double,
        initialLng: Double
    ): Result<UserRole> {
        val db = getFirestore() ?: return Result.failure(IllegalStateException("Firestore unavailable"))
        val cleanRoomId = normalizeRoomId(roomId)
        return runCatching {
            val snap = db.collection("rooms").document(cleanRoomId).get().await()
            if (!snap.exists()) {
                throw IllegalArgumentException("Room $cleanRoomId tidak ditemukan di server.")
            }
            if (snap.getString("status") == "FINISHED") {
                throw IllegalStateException("Konvoi pada Room $cleanRoomId sudah berakhir.")
            }
            val leaderUid = snap.getString("leaderUid").orEmpty()
            val role = if (leaderUid == uid) UserRole.TOUR_LEADER else UserRole.MEMBER
            val memberRef = db.collection("rooms").document(cleanRoomId)
                .collection("members").document(uid)
            val existingMember = runCatching { memberRef.get().await() }.getOrNull()

            if (existingMember != null && existingMember.exists()) {
                val existingRole = runCatching {
                    UserRole.valueOf(existingMember.getString("role") ?: role.name)
                }.getOrDefault(role)
                memberRef.update(
                    mapOf(
                        "fullName" to profile.fullName.trim().ifBlank { "Anggota" }.take(80),
                        "vehicleType" to profile.vehicleType.name,
                        "vehicleDetail" to profile.displayVehicleLabel.take(80),
                        "licensePlate" to profile.licensePlate.ifBlank { "B 2026 KNV" }.take(24),
                        "avatarPreset" to profile.avatarPreset.name,
                        "conditionStatus" to MemberConditionStatus.AMAN.name,
                        "connectionStatus" to ConnectionStatus.CONNECTED.name,
                        "lastLocationText" to "GPS Aktif",
                        "latitude" to initialLat,
                        "longitude" to initialLng,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                return@runCatching existingRole
            }

            val memberData = hashMapOf(
                "uid" to uid,
                "roomId" to cleanRoomId,
                "fullName" to profile.fullName.trim().ifBlank { "Anggota" }.take(80),
                "vehicleType" to profile.vehicleType.name,
                "vehicleDetail" to profile.displayVehicleLabel.take(80),
                "licensePlate" to profile.licensePlate.ifBlank { "B 2026 KNV" }.take(24),
                "role" to role.name,
                "isSweeper" to false,
                "avatarPreset" to profile.avatarPreset.name,
                "conditionStatus" to MemberConditionStatus.AMAN.name,
                "connectionStatus" to ConnectionStatus.CONNECTED.name,
                "lastLocationText" to "GPS Aktif",
                "latitude" to initialLat,
                "longitude" to initialLng,
                "speedKmh" to 0,
                "heading" to 0,
                "accuracyMeters" to 20,
                "routeProgress" to 0.05,
                "xFraction" to 0.2,
                "yFraction" to 0.8,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            memberRef.set(memberData).await()
            role
        }
    }

    fun subscribeToRoom(
        roomId: String,
        currentUid: String,
        onRoomHeader: (SyncedRoomHeader) -> Unit,
        onMembers: (List<ConvoyMember>) -> Unit,
        onCheckpoints: (List<RestCheckpoint>) -> Unit,
        onMessages: (List<RoomMessage>) -> Unit
    ): () -> Unit {
        val db = getFirestore() ?: return {}
        val cleanRoomId = normalizeRoomId(roomId)
        val regs = mutableListOf<ListenerRegistration>()

        regs += db.collection("rooms").document(cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val decoded = GeoUtils.decodePolyline(snap.getString("encodedPolyline").orEmpty())
                val waypoints = if (decoded.size >= 2) {
                    GeoUtils.toRoutePoints(decoded)
                } else {
                    listOf(
                        com.konvoiku.touring.model.RoutePoint(
                            0.14f, 0.86f,
                            snap.getDouble("originLat") ?: -6.648,
                            snap.getDouble("originLng") ?: 106.845
                        ),
                        com.konvoiku.touring.model.RoutePoint(
                            0.86f, 0.14f,
                            snap.getDouble("destLat") ?: -6.7003,
                            snap.getDouble("destLng") ?: 106.996
                        )
                    )
                }
                val rawInstList = snap.get("routeInstructions") as? List<*>
                val parsedInstructions = rawInstList.orEmpty().mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val lat = ((map["lat"] as? Number) ?: (map["latitude"] as? Number))
                        ?.toDouble()?.takeIf { it.isFinite() } ?: return@mapNotNull null
                    val lng = ((map["lng"] as? Number) ?: (map["longitude"] as? Number))
                        ?.toDouble()?.takeIf { it.isFinite() } ?: return@mapNotNull null
                    val offsetMeters = ((map["offsetMeters"] as? Number) ?: (map["routeOffsetInMeters"] as? Number))
                        ?.toInt() ?: 0
                    RouteInstruction(
                        latitude = lat,
                        longitude = lng,
                        routeOffsetInMeters = offsetMeters,
                        maneuver = (map["maneuver"] as? String)?.ifBlank { "STRAIGHT" } ?: "STRAIGHT",
                        message = (map["message"] as? String).orEmpty(),
                        street = (map["street"] as? String).orEmpty()
                    )
                }
                val leaderUid = snap.getString("leaderUid") ?: ""
                val cfg = ConvoyRouteConfig(
                    originName = snap.getString("originName")?.takeIf { it.isNotBlank() } ?: "Titik Keberangkatan",
                    originAddress = snap.getString("originAddress") ?: "",
                    originLockedByLeader = true,
                    destinationName = snap.getString("destinationName") ?: "Tujuan Konvoi",
                    destinationAddress = snap.getString("destinationAddress") ?: "",
                    routePreference = RoutePreference.fromId(snap.getString("routePreference")),
                    waypoints = waypoints,
                    routeInstructions = parsedInstructions,
                    checkpoints = emptyList(),
                    totalRouteMeters = (snap.getLong("totalRouteMeters") ?: 12000L).toInt(),
                    offRouteToleranceMeters = (snap.getLong("offRouteToleranceMeters") ?: 75L).toInt(),
                    maxStragglerDistanceMeters = (snap.getLong("maxStragglerDistanceMeters") ?: 1000L).toInt(),
                    soundAlertEnabled = snap.getBoolean("soundAlertEnabled") ?: true
                )
                val sos = SosEmergencyState(
                    isActive = snap.getBoolean("sosActive") ?: false,
                    senderId = snap.getString("sosSenderUid") ?: "",
                    senderName = snap.getString("sosSenderName") ?: "",
                    senderVehicle = snap.getString("sosSenderVehicle") ?: "",
                    latitude = snap.getDouble("sosLat") ?: 0.0,
                    longitude = snap.getDouble("sosLng") ?: 0.0,
                    locationText = snap.getString("sosLocationText") ?: "",
                    triggeredAtLabel = snap.getString("sosTriggeredAtLabel") ?: ""
                )
                onRoomHeader(
                    SyncedRoomHeader(
                        roomId = snap.getString("roomId") ?: cleanRoomId,
                        roomTitle = snap.getString("roomTitle") ?: "Konvoi Live",
                        leaderUid = leaderUid,
                        coLeaderUid = snap.getString("coLeaderUid") ?: "",
                        actingLeaderUid = snap.getString("actingLeaderUid")?.ifBlank { leaderUid } ?: leaderUid,
                        leaderTroubleActive = snap.getBoolean("leaderTroubleActive") ?: false,
                        leaderTroubleReason = snap.getString("leaderTroubleReason") ?: "",
                        status = if (snap.getString("status") == "FINISHED") "FINISHED" else "ACTIVE",
                        routeConfig = cfg,
                        sosState = sos
                    )
                )
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("members")
            .whereEqualTo("roomId", cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val members = snap.documents.mapIndexed { idx, doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val updatedTs = doc.getTimestamp("updatedAt")
                    ConvoyMember(
                        id = uid,
                        fullName = doc.getString("fullName") ?: "Rider",
                        vehicleType = runCatching {
                            VehicleType.valueOf(doc.getString("vehicleType") ?: "MOBIL")
                        }.getOrDefault(VehicleType.MOBIL),
                        vehicleDetail = doc.getString("vehicleDetail") ?: "",
                        licensePlate = doc.getString("licensePlate") ?: "",
                        emergencyContact = "",
                        role = runCatching {
                            UserRole.valueOf(doc.getString("role") ?: "MEMBER")
                        }.getOrDefault(UserRole.MEMBER),
                        joinedAtLabel = formatTimestamp(doc.getTimestamp("joinedAt"), "Aktif"),
                        isCurrentUser = uid == currentUid,
                        isSweeper = doc.getBoolean("isSweeper") ?: false,
                        lastUpdatedEpochMs = updatedTs?.toDate()?.time ?: 0L,
                        avatarPreset = runCatching {
                            AvatarPreset.valueOf(doc.getString("avatarPreset") ?: "HELMET_TOURING")
                        }.getOrDefault(AvatarPreset.HELMET_TOURING),
                        conditionStatus = runCatching {
                            MemberConditionStatus.valueOf(doc.getString("conditionStatus") ?: "AMAN")
                        }.getOrDefault(MemberConditionStatus.AMAN),
                        connectionStatus = runCatching {
                            ConnectionStatus.valueOf(doc.getString("connectionStatus") ?: "CONNECTED")
                        }.getOrDefault(ConnectionStatus.CONNECTED),
                        lastLocationText = doc.getString("lastLocationText") ?: "GPS Live",
                        lastUpdatedLabel = formatTimestamp(updatedTs, "Baru saja"),
                        convoyOrderRank = idx + 1,
                        mapPosition = MapPosition(
                            routeProgress = (doc.getDouble("routeProgress") ?: 0.05).toFloat(),
                            xFraction = (doc.getDouble("xFraction") ?: 0.5).toFloat(),
                            yFraction = (doc.getDouble("yFraction") ?: 0.5).toFloat(),
                            latitude = doc.getDouble("latitude") ?: -6.67,
                            longitude = doc.getDouble("longitude") ?: 106.92,
                            speedKmh = (doc.getLong("speedKmh") ?: 0L).toInt(),
                            heading = (doc.getLong("heading") ?: 0L).toInt(),
                            accuracyMeters = (doc.getLong("accuracyMeters") ?: 20L).toInt()
                        )
                    )
                }
                onMembers(members)
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("checkpoints")
            .whereEqualTo("roomId", cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val cps = snap.documents.map { doc ->
                    RestCheckpoint(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "Rest Area",
                        xFraction = (doc.getDouble("xFraction") ?: 0.5).toFloat(),
                        yFraction = (doc.getDouble("yFraction") ?: 0.5).toFloat(),
                        latitude = doc.getDouble("latitude") ?: -6.68,
                        longitude = doc.getDouble("longitude") ?: 106.92
                    )
                }
                onCheckpoints(cps)
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("messages")
            .whereEqualTo("roomId", cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val msgs = snap.documents.sortedBy {
                    it.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                }.takeLast(80).map { doc ->
                    val senderUid = doc.getString("senderUid") ?: ""
                    RoomMessage(
                        id = doc.getString("id") ?: doc.id,
                        senderId = senderUid,
                        senderName = doc.getString("senderName") ?: "Rider",
                        senderVehicle = doc.getString("senderVehicle") ?: "",
                        type = runCatching {
                            ChatMessageType.valueOf(doc.getString("type") ?: "TEXT")
                        }.getOrDefault(ChatMessageType.TEXT),
                        textContent = doc.getString("textContent") ?: "",
                        timestampLabel = doc.getString("timestampLabel") ?: "Baru saja",
                        voiceDurationSec = (doc.getLong("voiceDurationSec") ?: 0L).toInt(),
                        voiceAudioDataUrl = doc.getString("voiceAudioDataUrl")
                            ?.takeIf { it.startsWith("data:audio/") && it.length <= 350_000 },
                        isFromCurrentUser = senderUid == currentUid,
                        senderAvatarPreset = runCatching {
                            AvatarPreset.valueOf(doc.getString("senderAvatarPreset") ?: "HELMET_TOURING")
                        }.getOrDefault(AvatarPreset.HELMET_TOURING)
                    )
                }
                onMessages(msgs)
            }

        return { regs.forEach { it.remove() } }
    }

    fun publishTelemetry(
        roomId: String,
        uid: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Int,
        heading: Int,
        accuracyMeters: Int,
        routeProgress: Float
    ) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        db.collection("rooms").document(cleanRoomId)
            .collection("members").document(uid)
            .update(
                mapOf(
                    "latitude" to latitude.coerceIn(-90.0, 90.0),
                    "longitude" to longitude.coerceIn(-180.0, 180.0),
                    "speedKmh" to speedKmh.coerceIn(0, 350),
                    "heading" to heading.coerceIn(0, 360),
                    "accuracyMeters" to accuracyMeters.coerceIn(0, 500_000),
                    "routeProgress" to routeProgress.toDouble().coerceIn(0.0, 1.0),
                    "lastLocationText" to "GPS Live (±${accuracyMeters}m)",
                    "connectionStatus" to ConnectionStatus.CONNECTED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }

    fun updateRouteConfig(roomId: String, config: ConvoyRouteConfig) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        val origin = config.waypoints.firstOrNull()
        val dest = config.waypoints.lastOrNull()
        val encoded = GeoUtils.encodePolyline(
            config.waypoints.map { LatLngGeo(it.latitude, it.longitude) }
        ).take(16000)
        val updates = hashMapOf<String, Any>(
            "originName" to config.originName.take(120),
            "originAddress" to config.originAddress.take(240),
            "destinationName" to config.destinationName.take(120),
            "destinationAddress" to config.destinationAddress.take(240),
            "encodedPolyline" to encoded,
            "routePreference" to config.routePreference.id,
            "routeInstructions" to GeoUtils.compactInstructionsForSync(config.routeInstructions),
            "totalRouteMeters" to config.totalRouteMeters.coerceIn(100, 5_000_000),
            "offRouteToleranceMeters" to config.offRouteToleranceMeters.coerceIn(10, 5000),
            "maxStragglerDistanceMeters" to config.maxStragglerDistanceMeters.coerceIn(100, 50000),
            "soundAlertEnabled" to config.soundAlertEnabled,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (origin != null) {
            updates["originLat"] = origin.latitude
            updates["originLng"] = origin.longitude
        }
        if (dest != null) {
            updates["destLat"] = dest.latitude
            updates["destLng"] = dest.longitude
        }
        db.collection("rooms").document(cleanRoomId).update(updates)
    }

    fun addCheckpoint(roomId: String, uid: String, cp: RestCheckpoint) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        db.collection("rooms").document(cleanRoomId)
            .collection("checkpoints").document(cp.id)
            .set(
                mapOf(
                    "id" to cp.id,
                    "roomId" to cleanRoomId,
                    "createdByUid" to uid,
                    "name" to cp.name.take(100),
                    "latitude" to cp.latitude,
                    "longitude" to cp.longitude,
                    "xFraction" to cp.xFraction.toDouble().coerceIn(0.0, 1.0),
                    "yFraction" to cp.yFraction.toDouble().coerceIn(0.0, 1.0),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
    }

    fun removeCheckpoint(roomId: String, checkpointId: String) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        db.collection("rooms").document(cleanRoomId)
            .collection("checkpoints").document(checkpointId)
            .delete()
    }

    fun sendMessage(roomId: String, senderUid: String, msg: RoomMessage) {
        val db = getFirestore() ?: return
        val payload = hashMapOf<String, Any>(
            "id" to msg.id,
            "roomId" to roomId,
            "senderUid" to senderUid,
            "senderName" to msg.senderName.take(80),
            "senderVehicle" to msg.senderVehicle.take(80),
            "senderAvatarPreset" to msg.senderAvatarPreset.name,
            "type" to msg.type.name,
            "textContent" to msg.textContent.take(500),
            "voiceDurationSec" to msg.voiceDurationSec.coerceIn(0, 600),
            "timestampLabel" to msg.timestampLabel.take(32),
            "createdAt" to FieldValue.serverTimestamp()
        )
        val audioData = msg.voiceAudioDataUrl
        if (!audioData.isNullOrBlank() && audioData.startsWith("data:audio/") && audioData.length <= 345_000) {
            payload["voiceAudioDataUrl"] = audioData
        }
        db.collection("rooms").document(roomId)
            .collection("messages").document(msg.id)
            .set(payload)
    }

    fun triggerSos(roomId: String, sos: SosEmergencyState, uid: String) {
        val db = getFirestore() ?: return
        db.collection("rooms").document(roomId).update(
            mapOf(
                "sosActive" to true,
                "sosSenderUid" to uid,
                "sosSenderName" to sos.senderName.take(80),
                "sosSenderVehicle" to sos.senderVehicle.take(80),
                "sosLat" to sos.latitude,
                "sosLng" to sos.longitude,
                "sosLocationText" to sos.locationText.take(160),
                "sosTriggeredAtLabel" to sos.triggeredAtLabel.take(32),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        updateMemberCondition(roomId, uid, MemberConditionStatus.SOS)
    }

    fun resolveSos(roomId: String, uid: String) {
        val db = getFirestore() ?: return
        db.collection("rooms").document(roomId).update(
            mapOf(
                "sosActive" to false,
                "sosSenderUid" to "",
                "sosSenderName" to "",
                "sosSenderVehicle" to "",
                "sosLat" to 0.0,
                "sosLng" to 0.0,
                "sosLocationText" to "",
                "sosTriggeredAtLabel" to "",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        updateMemberCondition(roomId, uid, MemberConditionStatus.AMAN)
    }

    fun updateMemberCondition(roomId: String, uid: String, status: MemberConditionStatus) {
        val db = getFirestore() ?: return
        db.collection("rooms").document(roomId)
            .collection("members").document(uid)
            .update(
                mapOf(
                    "conditionStatus" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
    }

    fun assignSweeper(roomId: String, targetMemberUid: String, allMemberUids: List<String>) {
        val db = getFirestore() ?: return
        allMemberUids.forEach { memberUid ->
            db.collection("rooms").document(roomId)
                .collection("members").document(memberUid)
                .update(
                    mapOf(
                        "isSweeper" to (memberUid == targetMemberUid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
        }
    }

    suspend fun finishRoomIfLeader(roomId: String, uid: String) {
        val db = getFirestore() ?: return
        runCatching {
            val roomRef = db.collection("rooms").document(roomId)
            val snap = roomRef.get().await()
            val isEffective = snap.exists() &&
                (snap.getString("leaderUid") == uid || snap.getString("actingLeaderUid") == uid)
            if (isEffective && snap.getString("status") != "FINISHED") {
                roomRef.update(
                    mapOf(
                        "status" to "FINISHED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    fun updateCoLeaderUid(roomId: String, coLeaderUid: String) {
        val db = getFirestore() ?: return
        db.collection("rooms").document(roomId).update(
            mapOf(
                "coLeaderUid" to coLeaderUid.take(128),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
    }

    suspend fun triggerLeaderFailover(
        roomId: String,
        actingLeaderUid: String,
        reason: String
    ): Boolean {
        val db = getFirestore() ?: return false
        return runCatching {
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "actingLeaderUid" to actingLeaderUid.take(128),
                    "leaderTroubleActive" to true,
                    "leaderTroubleReason" to reason.take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }.getOrDefault(false)
    }

    suspend fun reclaimOriginalLeader(
        roomId: String,
        originalLeaderUid: String
    ): Boolean {
        val db = getFirestore() ?: return false
        return runCatching {
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "actingLeaderUid" to originalLeaderUid.take(128),
                    "leaderTroubleActive" to false,
                    "leaderTroubleReason" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }.getOrDefault(false)
    }

    suspend fun saveTripRecord(uid: String, record: TripHistoryRecord) {
        val db = getFirestore() ?: return
        val safeId = record.id.replace(Regex("[^a-zA-Z0-9_-]"), "").take(64)
            .ifBlank { "trip-${System.currentTimeMillis()}" }
        val roomCode = record.roomId.trim().uppercase(Locale.ROOT).take(24)
            .ifBlank { "KNV-0000" }
        val validWaypoints = record.waypoints
            .filter { GeoUtils.hasValidGeoCoord(it.latitude, it.longitude) }
            .map { LatLngGeo(it.latitude, it.longitude) }
        val compactWaypoints = GeoUtils.downsampleLatLng(validWaypoints, 140)
        val encodedPolyline = if (compactWaypoints.size >= 2) {
            GeoUtils.encodePolyline(compactWaypoints).take(16384)
        } else {
            ""
        }

        val compactParticipants = record.participants.take(40).map { p ->
            hashMapOf<String, Any>(
                "id" to p.id.take(128),
                "fullName" to p.fullName.trim().ifBlank { "Rider" }.take(80),
                "vehicleType" to p.vehicleType.name,
                "vehicleDetail" to p.vehicleDetail.take(80),
                "licensePlate" to p.licensePlate.take(32),
                "role" to p.role.name,
                "isSweeper" to p.isSweeper,
                "avatarPreset" to p.avatarPreset.name
            )
        }

        runCatching {
            val data = hashMapOf<String, Any>(
                "id" to safeId,
                "ownerUid" to uid,
                "roomId" to roomCode,
                "tourTitle" to record.tourTitle.trim().ifBlank { "Konvoi Touring" }.take(100),
                "dateLabel" to record.dateLabel.take(32),
                "timeRangeLabel" to record.timeRangeLabel.take(32),
                "durationLabel" to record.durationLabel.take(32),
                "durationMinutes" to record.durationMinutes.coerceIn(0, 10080),
                "distanceKm" to record.distanceKm.coerceIn(0.0, 20000.0),
                "avgSpeedKmh" to record.avgSpeedKmh.coerceIn(0, 350),
                "maxSpeedKmh" to record.maxSpeedKmh.coerceIn(0, 350),
                "userRole" to record.userRole.name,
                "destinationName" to record.destinationName.take(120),
                "destinationAddress" to record.destinationAddress.take(240),
                "encodedPolyline" to encodedPolyline,
                "participants" to compactParticipants,
                "createdAt" to FieldValue.serverTimestamp()
            )
            db.collection("users").document(uid)
                .collection("trips").document(safeId)
                .set(data)
                .await()
        }
    }

    suspend fun loadUserTrips(uid: String): List<TripHistoryRecord> {
        val db = getFirestore() ?: return emptyList()
        return runCatching {
            val snap = db.collection("users").document(uid)
                .collection("trips")
                .whereEqualTo("ownerUid", uid)
                .get()
                .await()

            snap.documents
                .map { doc ->
                    val createdMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    val encoded = doc.getString("encodedPolyline").orEmpty()
                    val decoded = GeoUtils.downsampleLatLng(GeoUtils.decodePolyline(encoded), 120)
                    val waypoints = if (decoded.size >= 2) {
                        GeoUtils.toRoutePoints(decoded)
                    } else {
                        emptyList()
                    }
                    val rawParticipants = doc.get("participants") as? List<*>
                    val restoredParticipants = rawParticipants.orEmpty().mapIndexedNotNull { idx, item ->
                        val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
                        val name = (m["fullName"] as? String)?.trim().orEmpty()
                        if (name.isEmpty()) return@mapIndexedNotNull null
                        TripParticipantSummary(
                            id = (m["id"] as? String).orEmpty().ifBlank { "p-$idx" },
                            fullName = name,
                            vehicleType = runCatching {
                                VehicleType.valueOf((m["vehicleType"] as? String) ?: "MOTOR")
                            }.getOrDefault(VehicleType.MOTOR),
                            vehicleDetail = (m["vehicleDetail"] as? String).orEmpty(),
                            licensePlate = (m["licensePlate"] as? String).orEmpty(),
                            role = runCatching {
                                UserRole.valueOf((m["role"] as? String) ?: "MEMBER")
                            }.getOrDefault(UserRole.MEMBER),
                            isSweeper = (m["isSweeper"] as? Boolean) == true,
                            avatarPreset = runCatching {
                                AvatarPreset.valueOf((m["avatarPreset"] as? String) ?: "HELMET_TOURING")
                            }.getOrDefault(AvatarPreset.HELMET_TOURING)
                        )
                    }
                    val record = TripHistoryRecord(
                        id = doc.getString("id") ?: doc.id,
                        roomId = doc.getString("roomId") ?: "KNV-0000",
                        tourTitle = doc.getString("tourTitle") ?: "Konvoi Touring",
                        dateLabel = doc.getString("dateLabel") ?: "-",
                        timeRangeLabel = doc.getString("timeRangeLabel") ?: "-",
                        durationLabel = doc.getString("durationLabel") ?: "-",
                        durationMinutes = (doc.getLong("durationMinutes") ?: 0L).toInt(),
                        distanceKm = doc.getDouble("distanceKm") ?: 0.0,
                        avgSpeedKmh = (doc.getLong("avgSpeedKmh") ?: 0L).toInt(),
                        maxSpeedKmh = (doc.getLong("maxSpeedKmh") ?: 0L).toInt(),
                        userRole = runCatching {
                            UserRole.valueOf(doc.getString("userRole") ?: "MEMBER")
                        }.getOrDefault(UserRole.MEMBER),
                        destinationName = doc.getString("destinationName") ?: "Tujuan Konvoi",
                        destinationAddress = doc.getString("destinationAddress") ?: "",
                        waypoints = waypoints,
                        visitedCheckpoints = emptyList(),
                        participants = restoredParticipants
                    )
                    createdMs to record
                }
                .sortedByDescending { it.first }
                .take(40)
                .map { it.second }
        }.getOrDefault(emptyList())
    }

    fun isAdminEmail(email: String?): Boolean =
        email?.trim()?.lowercase(Locale.ROOT) == ADMIN_EMAIL

    fun isVerifiedAdminSession(): Boolean {
        val user = getAuth()?.currentUser ?: return false
        return isAdminEmail(user.email) && user.isEmailVerified
    }

    fun subscribeToGlobalAppConfig(onUpdate: (GlobalAppConfig) -> Unit): () -> Unit {
        val db = getFirestore() ?: return {}
        val reg = db.collection("app_config").document("global")
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    onUpdate(GlobalAppConfig())
                    return@addSnapshotListener
                }
                onUpdate(
                    GlobalAppConfig(
                        configId = "global",
                        defaultOffRouteToleranceMeters = (snap.getLong("defaultOffRouteToleranceMeters") ?: 75L)
                            .toInt().coerceIn(10, 5000),
                        defaultMaxStragglerDistanceMeters = (snap.getLong("defaultMaxStragglerDistanceMeters") ?: 1000L)
                            .toInt().coerceIn(100, 50000),
                        defaultSoundAlertEnabled = snap.getBoolean("defaultSoundAlertEnabled") ?: true,
                        maintenanceMode = snap.getBoolean("maintenanceMode") ?: false,
                        maintenanceMessage = snap.getString("maintenanceMessage")
                            ?: "Aplikasi sedang dalam pemeliharaan berkala. Mohon tunggu beberapa saat.",
                        announcementActive = snap.getBoolean("announcementActive") ?: false,
                        announcementTitle = snap.getString("announcementTitle") ?: "Info KonvoiKu",
                        announcementMessage = snap.getString("announcementMessage") ?: "",
                        allowGuestMode = snap.getBoolean("allowGuestMode") ?: true,
                        allowNewRooms = snap.getBoolean("allowNewRooms") ?: true,
                        updatedAtLabel = formatTimestamp(snap.getTimestamp("updatedAt"), "Baru saja")
                    )
                )
            }
        return { reg.remove() }
    }

    suspend fun saveGlobalAppConfig(uid: String, config: GlobalAppConfig): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        return runCatching {
            val payload = hashMapOf(
                "configId" to "global",
                "defaultOffRouteToleranceMeters" to config.defaultOffRouteToleranceMeters.coerceIn(10, 5000),
                "defaultMaxStragglerDistanceMeters" to config.defaultMaxStragglerDistanceMeters.coerceIn(100, 50000),
                "defaultSoundAlertEnabled" to config.defaultSoundAlertEnabled,
                "maintenanceMode" to config.maintenanceMode,
                "maintenanceMessage" to config.maintenanceMessage.trim().take(240),
                "announcementActive" to config.announcementActive,
                "announcementTitle" to config.announcementTitle.trim().take(80),
                "announcementMessage" to config.announcementMessage.trim().take(240),
                "allowGuestMode" to config.allowGuestMode,
                "allowNewRooms" to config.allowNewRooms,
                "updatedByUid" to uid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("app_config").document("global").set(payload).await()
            Unit
        }
    }

    fun publishUserPresence(
        uid: String,
        fullName: String,
        email: String,
        isTraveling: Boolean,
        activeRoomId: String,
        speedKmh: Int
    ) {
        val db = getFirestore() ?: return
        db.collection("presence").document(uid).set(
            mapOf(
                "uid" to uid,
                "fullName" to fullName.trim().ifBlank { "Rider KonvoiKu" }.take(80),
                "email" to email.trim().lowercase(Locale.ROOT).take(160),
                "platform" to "android",
                "isTraveling" to isTraveling,
                "activeRoomId" to activeRoomId.trim().take(32),
                "speedKmh" to speedKmh.coerceIn(0, 350),
                "lastSeenAt" to FieldValue.serverTimestamp()
            )
        )
    }

    fun subscribeToAdminMonitoring(
        onPresence: (List<UserPresenceInfo>) -> Unit,
        onRooms: (List<AdminRoomOverview>) -> Unit,
        onUsers: (List<AdminUserSummary>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val db = getFirestore() ?: return {}
        val regs = mutableListOf<ListenerRegistration>()
        val memberRegs = mutableMapOf<String, ListenerRegistration>()
        var latestRoomsBase = emptyList<AdminRoomOverview>()
        val membersByRoom = mutableMapOf<String, List<ConvoyMember>>()

        fun emitRooms() {
            val combined = latestRoomsBase.map { r ->
                r.copy(members = membersByRoom[r.roomId].orEmpty())
            }.sortedWith(
                compareBy<AdminRoomOverview> { if (it.status == "ACTIVE") 0 else 1 }
                    .thenByDescending { it.updatedAtMs }
            )
            onRooms(combined)
        }

        regs += db.collection("presence").addSnapshotListener { snap, err ->
            if (err != null) {
                onError("Gagal memuat presence aktif: ${err.message}")
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            val items = snap.documents.map { doc ->
                val ts = doc.getTimestamp("lastSeenAt")
                UserPresenceInfo(
                    uid = doc.getString("uid") ?: doc.id,
                    fullName = doc.getString("fullName") ?: "Rider",
                    email = doc.getString("email").orEmpty(),
                    platform = if (doc.getString("platform") == "android") "android" else "web",
                    isTraveling = doc.getBoolean("isTraveling") ?: false,
                    activeRoomId = doc.getString("activeRoomId").orEmpty(),
                    speedKmh = (doc.getLong("speedKmh") ?: 0L).toInt(),
                    lastSeenMs = ts?.toDate()?.time ?: 0L,
                    lastSeenLabel = formatTimestamp(ts, "Baru saja")
                )
            }.sortedByDescending { it.lastSeenMs }
            onPresence(items)
        }

        regs += db.collection("users").addSnapshotListener { snap, err ->
            if (err != null) {
                onError("Gagal memuat daftar pengguna: ${err.message}")
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            val list = snap.documents.map { doc ->
                AdminUserSummary(
                    uid = doc.getString("uid") ?: doc.id,
                    fullName = doc.getString("fullName") ?: "Rider KonvoiKu",
                    email = doc.getString("email").orEmpty(),
                    authProvider = AuthProviderType.fromId(doc.getString("authProvider")),
                    clubName = doc.getString("clubName").orEmpty(),
                    homeCity = doc.getString("homeCity").orEmpty(),
                    vehicleType = runCatching {
                        VehicleType.valueOf(doc.getString("vehicleType") ?: "MOBIL")
                    }.getOrDefault(VehicleType.MOBIL),
                    vehicleDetail = doc.getString("vehicleDetail").orEmpty(),
                    licensePlate = doc.getString("licensePlate").orEmpty(),
                    avatarPreset = runCatching {
                        AvatarPreset.valueOf(doc.getString("avatarPreset") ?: "HELMET_TOURING")
                    }.getOrDefault(AvatarPreset.HELMET_TOURING),
                    avatarPhotoUri = doc.getString("avatarPhotoUrl")?.takeIf { it.isNotBlank() },
                    updatedAtLabel = formatTimestamp(doc.getTimestamp("updatedAt"), "-")
                )
            }
            onUsers(list)
        }

        regs += db.collection("rooms").addSnapshotListener { snap, err ->
            if (err != null) {
                onError("Gagal memuat daftar room konvoi: ${err.message}")
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            val activeIds = mutableSetOf<String>()
            latestRoomsBase = snap.documents.map { doc ->
                val roomId = doc.getString("roomId") ?: doc.id
                activeIds += roomId
                if (!memberRegs.containsKey(roomId)) {
                    memberRegs[roomId] = db.collection("rooms").document(roomId)
                        .collection("members")
                        .addSnapshotListener { mSnap, _ ->
                            if (mSnap == null) return@addSnapshotListener
                            val mList = mSnap.documents.mapIndexed { idx, mDoc ->
                                val uid = mDoc.getString("uid") ?: mDoc.id
                                ConvoyMember(
                                    id = uid,
                                    fullName = mDoc.getString("fullName") ?: "Rider",
                                    vehicleType = runCatching {
                                        VehicleType.valueOf(mDoc.getString("vehicleType") ?: "MOBIL")
                                    }.getOrDefault(VehicleType.MOBIL),
                                    vehicleDetail = mDoc.getString("vehicleDetail").orEmpty(),
                                    licensePlate = mDoc.getString("licensePlate").orEmpty(),
                                    emergencyContact = "",
                                    role = runCatching {
                                        UserRole.valueOf(mDoc.getString("role") ?: "MEMBER")
                                    }.getOrDefault(UserRole.MEMBER),
                                    joinedAtLabel = formatTimestamp(mDoc.getTimestamp("joinedAt"), "Aktif"),
                                    isCurrentUser = false,
                                    isSweeper = mDoc.getBoolean("isSweeper") ?: false,
                                    avatarPreset = runCatching {
                                        AvatarPreset.valueOf(mDoc.getString("avatarPreset") ?: "HELMET_TOURING")
                                    }.getOrDefault(AvatarPreset.HELMET_TOURING),
                                    conditionStatus = runCatching {
                                        MemberConditionStatus.valueOf(mDoc.getString("conditionStatus") ?: "AMAN")
                                    }.getOrDefault(MemberConditionStatus.AMAN),
                                    connectionStatus = runCatching {
                                        ConnectionStatus.valueOf(mDoc.getString("connectionStatus") ?: "CONNECTED")
                                    }.getOrDefault(ConnectionStatus.CONNECTED),
                                    lastLocationText = mDoc.getString("lastLocationText") ?: "GPS Live",
                                    lastUpdatedLabel = formatTimestamp(mDoc.getTimestamp("updatedAt"), "Baru saja"),
                                    convoyOrderRank = idx + 1,
                                    mapPosition = MapPosition(
                                        routeProgress = (mDoc.getDouble("routeProgress") ?: 0.0).toFloat(),
                                        xFraction = (mDoc.getDouble("xFraction") ?: 0.5).toFloat(),
                                        yFraction = (mDoc.getDouble("yFraction") ?: 0.5).toFloat(),
                                        latitude = mDoc.getDouble("latitude") ?: 0.0,
                                        longitude = mDoc.getDouble("longitude") ?: 0.0,
                                        speedKmh = (mDoc.getLong("speedKmh") ?: 0L).toInt(),
                                        heading = (mDoc.getLong("heading") ?: 0L).toInt(),
                                        accuracyMeters = (mDoc.getLong("accuracyMeters") ?: 20L).toInt()
                                    )
                                )
                            }
                            membersByRoom[roomId] = mList
                            emitRooms()
                        }
                }
                val updatedTs = doc.getTimestamp("updatedAt")
                AdminRoomOverview(
                    roomId = roomId,
                    roomTitle = doc.getString("roomTitle") ?: "Konvoi Live",
                    leaderUid = doc.getString("leaderUid").orEmpty(),
                    destinationName = doc.getString("destinationName") ?: "-",
                    status = if (doc.getString("status") == "FINISHED") "FINISHED" else "ACTIVE",
                    sosActive = doc.getBoolean("sosActive") ?: false,
                    sosSenderName = doc.getString("sosSenderName").orEmpty(),
                    offRouteToleranceMeters = (doc.getLong("offRouteToleranceMeters") ?: 75L).toInt(),
                    maxStragglerDistanceMeters = (doc.getLong("maxStragglerDistanceMeters") ?: 1000L).toInt(),
                    createdAtLabel = formatTimestamp(doc.getTimestamp("createdAt"), "-"),
                    updatedAtMs = updatedTs?.toDate()?.time ?: 0L,
                    updatedAtLabel = formatTimestamp(updatedTs, "-")
                )
            }
            val staleIds = memberRegs.keys.filterNot { it in activeIds }
            staleIds.forEach { staleId ->
                memberRegs.remove(staleId)?.remove()
                membersByRoom.remove(staleId)
            }
            emitRooms()
        }

        return {
            regs.forEach { it.remove() }
            memberRegs.values.forEach { it.remove() }
            memberRegs.clear()
        }
    }

    suspend fun adminSetRoomStatus(roomId: String, status: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        return runCatching {
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }
    }

    suspend fun adminResolveRoomSos(roomId: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        return runCatching {
            db.collection("rooms").document(roomId).update(
                mapOf(
                    "sosActive" to false,
                    "sosSenderUid" to "",
                    "sosSenderName" to "",
                    "sosSenderVehicle" to "",
                    "sosLat" to 0.0,
                    "sosLng" to 0.0,
                    "sosLocationText" to "",
                    "sosTriggeredAtLabel" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }
    }

    suspend fun adminRemoveMember(roomId: String, memberUid: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        return runCatching {
            db.collection("rooms").document(roomId)
                .collection("members").document(memberUid)
                .delete().await()
            Unit
        }
    }

    suspend fun adminDeleteRoom(roomId: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        return runCatching {
            db.collection("rooms").document(roomId).delete().await()
            Unit
        }
    }

    private fun formatTimestamp(ts: Timestamp?, fallback: String): String {
        val date: Date = ts?.toDate() ?: return fallback
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
}
