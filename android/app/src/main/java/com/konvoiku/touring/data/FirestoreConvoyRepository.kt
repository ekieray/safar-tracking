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
import com.konvoiku.touring.model.AssistantChatMessage
import com.konvoiku.touring.model.AssistantRoleId
import com.konvoiku.touring.model.AuthProviderType
import com.konvoiku.touring.model.AvatarPreset
import com.konvoiku.touring.model.ChatMessageType
import com.konvoiku.touring.model.ConnectionStatus
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.ConvoyRouteConfig
import com.konvoiku.touring.model.DestinationStop
import com.konvoiku.touring.model.FamilyGrantInfo
import com.konvoiku.touring.model.FamilyLinkInfo
import com.konvoiku.touring.model.FamilyMonitorSnapshot
import com.konvoiku.touring.model.GlobalAppConfig
import com.konvoiku.touring.model.ManasikCheckInRecord
import com.konvoiku.touring.model.ManasikGuideProgress
import com.konvoiku.touring.model.ManasikGuideType
import com.konvoiku.touring.model.ManasikProgressState
import com.konvoiku.touring.model.MapPosition
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.PriorityBroadcast
import com.konvoiku.touring.model.RegisteredTourLeader
import com.konvoiku.touring.model.RestCheckpoint
import com.konvoiku.touring.model.RoomActivityItem
import com.konvoiku.touring.model.RoomActivityMemberProgress
import com.konvoiku.touring.model.RoomMessage
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePoint
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
    val sosState: SosEmergencyState,
    val priorityBroadcast: PriorityBroadcast? = null
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
        // Named Firestore database provisioned on excellent-victor-7v0ns for Safar Tracking Web & Android sync
        private const val FIRESTORE_DB_ID =
            "ai-studio-safartracking-e69c0149-e70e-44e4-892e-bdaae64e8754"
        const val FIREBASE_PROJECT_ID_PUBLIC = "excellent-victor-7v0ns"
        private const val FIREBASE_PROJECT_ID = FIREBASE_PROJECT_ID_PUBLIC
        private const val FIREBASE_APP_ID = "1:886997766185:web:749a3335a7991519350c38"
        const val FIREBASE_WEB_APP_ID = "1:886997766185:web:749a3335a7991519350c38"
        const val FIREBASE_AUTH_DOMAIN = "excellent-victor-7v0ns.firebaseapp.com"
        private val FIREBASE_API_KEY = listOf(
            "AIzaSyAjMt",
            "4190SzciFuy",
            "VrkGunLf-mPmHQ9rxk"
        ).joinToString("")
        val FIREBASE_WEB_API_KEY: String = FIREBASE_API_KEY
        private const val FIREBASE_STORAGE_BUCKET = "excellent-victor-7v0ns.firebasestorage.app"
        private const val FIREBASE_SENDER_ID = "886997766185"
        private const val SHARED_BASE_URL =
            "https://ais-pre-3puhws3li7lokxfmbtgyz4-588877716211.asia-southeast1.run.app"
        private const val DEV_BASE_URL =
            "https://ais-dev-3puhws3li7lokxfmbtgyz4-588877716211.asia-southeast1.run.app"
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

    fun normalizeHttpBaseUrl(raw: String): String? {
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
            connectTimeout = 6000
            readTimeout = 8000
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
                if (body.contains("\"service\"") && body.contains("safar-tracking")) {
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
                if (body.contains("Safar Tracking", ignoreCase = true)) {
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
        allowDevProxyFallback: Boolean = false,
        configuredPublicUrl: String? = null
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
                    errorMessage = "URL kustom ($customClean) mengembalikan 404 Page Not Found. Pastikan URL Web Safar Tracking sudah aktif/dipublikasikan."
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
                        errorMessage = "URL ($customClean) masih dilindungi proxy login AI Studio (applet-auth-bridge). Gunakan URL deploy Cloud Run publik (non-ais-pre/ais-dev)."
                    )
                }
                WebEndpointStatus.OUTDATED_BUILD -> WebBridgeResolution(
                    baseUrl = "",
                    isPublishedLive = false,
                    isDevProxy = false,
                    errorMessage = "Web di $customClean masih versi lama (belum mendukung sinkronisasi API/APK). Deploy ulang versi terbaru."
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

        val configuredClean = configuredPublicUrl?.let { normalizeHttpBaseUrl(it) }
        if (configuredClean != null && probeWebEndpoint(configuredClean) == WebEndpointStatus.LIVE_DIRECT) {
            return@withContext WebBridgeResolution(
                baseUrl = configuredClean,
                isPublishedLive = true,
                isDevProxy = false,
                isSharedUnpublished = false
            )
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

        val devStatus = probeWebEndpoint(DEV_BASE_URL)
        if (devStatus == WebEndpointStatus.LIVE_DIRECT) {
            return@withContext WebBridgeResolution(
                baseUrl = DEV_BASE_URL,
                isPublishedLive = true,
                isDevProxy = false,
                isSharedUnpublished = sharedStatus == WebEndpointStatus.NOT_FOUND
            )
        }

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
            val webStatusText =
                "Link Share/Preview AI Studio (ais-pre/ais-dev) dilindungi proxy login Google (applet-auth-bridge)."
            return@withContext WebBridgeResolution(
                baseUrl = "",
                isPublishedLive = false,
                isDevProxy = true,
                isSharedUnpublished = sharedUnpublished,
                errorMessage = "Endpoint web publik belum dapat diakses langsung dari APK. $webStatusText\n\nSolusi:\n• Deploy web ke Cloud Run (izinkan unauthenticated/akses publik), lalu buka web Cloud Run sebagai Admin agar URL tersinkron otomatis ke APK."
            )
        }

        WebBridgeResolution(
            baseUrl = "",
            isPublishedLive = false,
            isDevProxy = false,
            isSharedUnpublished = sharedStatus == WebEndpointStatus.NOT_FOUND,
            errorMessage = "Server web publik belum aktif. Deploy web ke Cloud Run (akses publik) dan buka sekali sebagai Admin agar URL backend tersinkron otomatis."
        )
    }

    suspend fun resolveActiveWebBaseUrl(
        customBaseUrl: String? = null,
        configuredPublicUrl: String? = null
    ): String {
        val resolution = resolveWebBridgeUrl(
            customBaseUrl = customBaseUrl,
            allowDevProxyFallback = false,
            configuredPublicUrl = configuredPublicUrl
        )
        val configuredClean = configuredPublicUrl?.let { normalizeHttpBaseUrl(it) }
        return resolution.baseUrl.ifBlank { configuredClean ?: SHARED_BASE_URL }
    }

    suspend fun resolveActiveGoogleBridgeUrl(
        customBaseUrl: String? = null,
        configuredPublicUrl: String? = null
    ): String {
        val base = resolveActiveWebBaseUrl(customBaseUrl, configuredPublicUrl)
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

    fun isDeviceCloudGuestEmail(email: String?): Boolean {
        val norm = email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return norm.endsWith("@guest.safartracking.id") || norm.endsWith("@guest.konvoiku.id")
    }

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

        val prefs = appContext?.getSharedPreferences("safartracking_cloud_device_auth", Context.MODE_PRIVATE)
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
        val deviceEmail = "jamaah.${UUID.randomUUID().toString().replace("-", "").take(16)}@guest.safartracking.id"
        val devicePassword = "Sfr#${UUID.randomUUID().toString().replace("-", "")}"
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
                    "fullName" to profile.fullName.trim().ifBlank { "Jamaah Safar Tracking" }.take(80),
                    "clubName" to profile.clubName.trim().take(80),
                    "homeCity" to profile.homeCity.trim().take(80),
                    "bio" to profile.bio.trim().take(240),
                    "instagramHandle" to profile.instagramHandle.trim().take(60),
                    "avatarPhotoUrl" to safePhoto,
                    "vehicleType" to profile.vehicleType.name,
                    "vehicleDetail" to profile.vehicleDetail.trim().take(80),
                    "licensePlate" to profile.licensePlate.trim().take(24),
                    "passportNumber" to profile.passportNumber.trim().uppercase().take(32),
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
                    "fullName" to profile.fullName.trim().ifBlank { "Jamaah Safar Tracking" }.take(80),
                    "clubName" to profile.clubName.trim().take(80),
                    "homeCity" to profile.homeCity.trim().take(80),
                    "bio" to profile.bio.trim().take(240),
                    "instagramHandle" to profile.instagramHandle.trim().take(60),
                    "avatarPhotoUrl" to safePhoto,
                    "vehicleType" to profile.vehicleType.name,
                    "vehicleDetail" to profile.vehicleDetail.trim().take(80),
                    "licensePlate" to profile.licensePlate.trim().take(24),
                    "passportNumber" to profile.passportNumber.trim().uppercase().take(32),
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
                passportNumber = snap.getString("passportNumber").orEmpty(),
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
            val safeDestName = routeConfig.destinationName.trim().ifBlank { "Masjidil Haram (Ka'bah)" }.take(120)
            val safeDestAddress = routeConfig.destinationAddress.trim().ifBlank {
                String.format(Locale.US, "%.4f, %.4f", dest?.latitude ?: 21.4225, dest?.longitude ?: 39.8262)
            }.take(240)

            val stopsPayload = routeConfig.destinationStops
                .take(12)
                .mapIndexed { idx, s ->
                    val sName = s.name.trim().ifBlank { "Tujuan ${idx + 1}" }.take(120)
                    val sAddr = s.address.trim().ifBlank {
                        String.format(Locale.US, "%s · %.4f, %.4f", sName, s.latitude, s.longitude)
                    }.take(240)
                    hashMapOf<String, Any>(
                        "id" to s.id.trim().ifBlank { "stop-${idx + 1}" }.take(64),
                        "name" to sName,
                        "address" to sAddr,
                        "latitude" to s.latitude,
                        "longitude" to s.longitude
                    )
                }

            val roomData = hashMapOf<String, Any>(
                "roomId" to cleanRoomId,
                "roomTitle" to roomTitle.trim().ifBlank { "Rombongan Safar" }.take(100),
                "leaderUid" to leaderUid,
                "coLeaderUid" to "",
                "actingLeaderUid" to leaderUid,
                "leaderTroubleActive" to false,
                "leaderTroubleReason" to "",
                "originName" to safeOriginName,
                "originAddress" to safeOriginAddress,
                "destinationName" to safeDestName,
                "destinationAddress" to safeDestAddress,
                "destinationStops" to stopsPayload,
                "destLat" to (dest?.latitude ?: 21.4225),
                "destLng" to (dest?.longitude ?: 39.8262),
                "originLat" to (origin?.latitude ?: initialLat),
                "originLng" to (origin?.longitude ?: initialLng),
                "encodedPolyline" to encoded,
                "routePreference" to routeConfig.routePreference.id,
                "routeInstructions" to GeoUtils.compactInstructionsForSync(routeConfig.routeInstructions),
                "totalRouteMeters" to routeConfig.totalRouteMeters.coerceIn(100, 5_000_000),
                "estimatedDurationMillis" to routeConfig.estimatedDurationMillis.coerceIn(0L, 604_800_000L),
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
                "fullName" to profile.fullName.trim().ifBlank { "Mutawwif / Ketua" }.take(80),
                "vehicleType" to profile.vehicleType.name,
                "vehicleDetail" to profile.displayVehicleLabel.take(80),
                "licensePlate" to profile.licensePlate.ifBlank { "KLOTER-01" }.take(24),
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
                throw IllegalStateException("Perjalanan pada Room $cleanRoomId sudah berakhir.")
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
                        "fullName" to profile.fullName.trim().ifBlank { "Jamaah" }.take(80),
                        "vehicleType" to profile.vehicleType.name,
                        "vehicleDetail" to profile.displayVehicleLabel.take(80),
                        "licensePlate" to profile.licensePlate.ifBlank { "JAMAAH" }.take(24),
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
                "fullName" to profile.fullName.trim().ifBlank { "Jamaah" }.take(80),
                "vehicleType" to profile.vehicleType.name,
                "vehicleDetail" to profile.displayVehicleLabel.take(80),
                "licensePlate" to profile.licensePlate.ifBlank { "JAMAAH" }.take(24),
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
        onMessages: (List<RoomMessage>) -> Unit,
        onActivities: (List<RoomActivityItem>) -> Unit = {},
        onActivityProgress: (Map<String, RoomActivityMemberProgress>) -> Unit = {}
    ): () -> Unit {
        val db = getFirestore() ?: return {}
        val cleanRoomId = normalizeRoomId(roomId)
        val regs = mutableListOf<ListenerRegistration>()

        regs += db.collection("rooms").document(cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val decoded = GeoUtils.decodePolyline(snap.getString("encodedPolyline").orEmpty())
                val originLat = snap.getDouble("originLat") ?: 21.4180
                val originLng = snap.getDouble("originLng") ?: 39.8275
                val destLat = snap.getDouble("destLat") ?: 21.4225
                val destLng = snap.getDouble("destLng") ?: 39.8262
                val waypoints = if (decoded.size >= 2) {
                    GeoUtils.toRoutePoints(decoded)
                } else {
                    listOf(
                        com.konvoiku.touring.model.RoutePoint(0.14f, 0.86f, originLat, originLng),
                        com.konvoiku.touring.model.RoutePoint(0.86f, 0.14f, destLat, destLng)
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
                val parsedDestName = snap.getString("destinationName") ?: "Masjidil Haram (Ka'bah)"
                val parsedDestAddr = snap.getString("destinationAddress") ?: ""
                val rawStops = snap.get("destinationStops") as? List<*>
                val parsedStops = rawStops.orEmpty().mapIndexedNotNull { idx, item ->
                    val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
                    val lat = (m["latitude"] as? Number)?.toDouble()?.takeIf { it.isFinite() }
                        ?: return@mapIndexedNotNull null
                    val lng = (m["longitude"] as? Number)?.toDouble()?.takeIf { it.isFinite() }
                        ?: return@mapIndexedNotNull null
                    val name = (m["name"] as? String)?.trim().orEmpty()
                    if (name.isEmpty()) return@mapIndexedNotNull null
                    DestinationStop(
                        id = (m["id"] as? String)?.trim().orEmpty().ifBlank { "stop-${idx + 1}" },
                        name = name,
                        address = (m["address"] as? String).orEmpty(),
                        latitude = lat,
                        longitude = lng
                    )
                }.ifEmpty {
                    listOf(
                        DestinationStop(
                            id = "stop-default-1",
                            name = parsedDestName,
                            address = parsedDestAddr,
                            latitude = destLat,
                            longitude = destLng
                        )
                    )
                }
                val leaderUid = snap.getString("leaderUid") ?: ""
                val cfg = ConvoyRouteConfig(
                    originName = snap.getString("originName")?.takeIf { it.isNotBlank() } ?: "Hotel Area Ajyad Makkah",
                    originAddress = snap.getString("originAddress") ?: "",
                    originLockedByLeader = true,
                    destinationName = parsedDestName,
                    destinationAddress = parsedDestAddr,
                    destinationStops = parsedStops,
                    estimatedDurationMillis = (snap.getLong("estimatedDurationMillis") ?: 0L).coerceAtLeast(0L),
                    routePreference = RoutePreference.fromId(snap.getString("routePreference")),
                    waypoints = waypoints,
                    routeInstructions = parsedInstructions,
                    checkpoints = emptyList(),
                    totalRouteMeters = (snap.getLong("totalRouteMeters") ?: 1800L).toInt(),
                    offRouteToleranceMeters = (snap.getLong("offRouteToleranceMeters") ?: 50L).toInt(),
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
                val broadcastId = snap.getString("priorityBroadcastId") ?: ""
                val broadcastActive = snap.getBoolean("priorityBroadcastActive") ?: false
                val broadcast = if (broadcastActive || broadcastId.isNotBlank()) {
                    PriorityBroadcast(
                        isActive = broadcastActive,
                        id = broadcastId,
                        title = snap.getString("priorityBroadcastTitle") ?: "",
                        message = snap.getString("priorityBroadcastMessage") ?: "",
                        senderName = snap.getString("priorityBroadcastSenderName") ?: "",
                        atLabel = snap.getString("priorityBroadcastAtLabel") ?: ""
                    )
                } else null
                onRoomHeader(
                    SyncedRoomHeader(
                        roomId = snap.getString("roomId") ?: cleanRoomId,
                        roomTitle = snap.getString("roomTitle") ?: "Rombongan Safar Live",
                        leaderUid = leaderUid,
                        coLeaderUid = snap.getString("coLeaderUid") ?: "",
                        actingLeaderUid = snap.getString("actingLeaderUid")?.ifBlank { leaderUid } ?: leaderUid,
                        leaderTroubleActive = snap.getBoolean("leaderTroubleActive") ?: false,
                        leaderTroubleReason = snap.getString("leaderTroubleReason") ?: "",
                        status = if (snap.getString("status") == "FINISHED") "FINISHED" else "ACTIVE",
                        routeConfig = cfg,
                        sosState = sos,
                        priorityBroadcast = broadcast
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
                        fullName = doc.getString("fullName") ?: "Jamaah",
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
                            latitude = doc.getDouble("latitude") ?: 21.4225,
                            longitude = doc.getDouble("longitude") ?: 39.8262,
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
                        name = doc.getString("name") ?: "Titik Kumpul",
                        xFraction = (doc.getDouble("xFraction") ?: 0.5).toFloat(),
                        yFraction = (doc.getDouble("yFraction") ?: 0.5).toFloat(),
                        latitude = doc.getDouble("latitude") ?: 21.4225,
                        longitude = doc.getDouble("longitude") ?: 39.8262
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
                        senderName = doc.getString("senderName") ?: "Jamaah",
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

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("activities")
            .whereEqualTo("roomId", cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val items = snap.documents.map { doc ->
                    RoomActivityItem(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "Kegiatan Umroh",
                        timeLabel = doc.getString("timeLabel") ?: "",
                        locationNote = doc.getString("locationNote") ?: "",
                        orderIndex = (doc.getLong("orderIndex") ?: 0L).toInt(),
                        createdByUid = doc.getString("createdByUid") ?: "",
                        createdAtMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        updatedAtMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }.sortedWith(compareBy<RoomActivityItem> { it.orderIndex }.thenBy { it.createdAtMs })
                onActivities(items)
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("activityProgress")
            .whereEqualTo("roomId", cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val progressMap = linkedMapOf<String, RoomActivityMemberProgress>()
                snap.documents.forEach { doc ->
                    val uid = doc.getString("uid") ?: doc.id
                    val completedIds = (doc.get("completedActivityIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        ?.filter { it.isNotBlank() }
                        .orEmpty()
                    val recordMap = linkedMapOf<String, String>()
                    val rawRecords = (doc.get("completedRecords") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    rawRecords.forEach { rec ->
                        val parts = rec.split("|", limit = 2)
                        val actId = parts.getOrNull(0)?.trim().orEmpty()
                        val time = parts.getOrNull(1)?.trim().orEmpty()
                        if (actId.isNotEmpty() && time.isNotEmpty()) {
                            recordMap[actId] = time
                        }
                    }
                    progressMap[uid] = RoomActivityMemberProgress(
                        uid = uid,
                        memberName = doc.getString("memberName") ?: "Jamaah",
                        completedActivityIds = completedIds,
                        completedAtByActivity = recordMap,
                        updatedAtMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }
                onActivityProgress(progressMap)
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
        val stopsPayload = config.destinationStops
            .take(12)
            .mapIndexed { idx, s ->
                val sName = s.name.trim().ifBlank { "Tujuan ${idx + 1}" }.take(120)
                val sAddr = s.address.trim().ifBlank {
                    String.format(Locale.US, "%s · %.4f, %.4f", sName, s.latitude, s.longitude)
                }.take(240)
                hashMapOf<String, Any>(
                    "id" to s.id.trim().ifBlank { "stop-${idx + 1}" }.take(64),
                    "name" to sName,
                    "address" to sAddr,
                    "latitude" to s.latitude,
                    "longitude" to s.longitude
                )
            }
        val safeDestName = config.destinationName.trim().ifBlank { "Masjidil Haram (Ka'bah)" }.take(120)
        val safeDestAddr = config.destinationAddress.trim().ifBlank {
            String.format(Locale.US, "%s · %.4f, %.4f", safeDestName, dest?.latitude ?: 21.4225, dest?.longitude ?: 39.8262)
        }.take(240)
        val updates = hashMapOf<String, Any>(
            "originName" to config.originName.take(120),
            "originAddress" to config.originAddress.take(240),
            "destinationName" to safeDestName,
            "destinationAddress" to safeDestAddr,
            "destinationStops" to stopsPayload,
            "encodedPolyline" to encoded,
            "routePreference" to config.routePreference.id,
            "routeInstructions" to GeoUtils.compactInstructionsForSync(config.routeInstructions),
            "totalRouteMeters" to config.totalRouteMeters.coerceIn(100, 5_000_000),
            "estimatedDurationMillis" to config.estimatedDurationMillis.coerceIn(0L, 604_800_000L),
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
            val isCreator = snap.exists() && snap.getString("leaderUid") == uid
            if (isCreator && snap.getString("status") != "FINISHED") {
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
            .ifBlank { "UMR-0000" }
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
                "fullName" to p.fullName.trim().ifBlank { "Jamaah" }.take(80),
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
                "tourTitle" to record.tourTitle.trim().ifBlank { "Safar Rombongan Umroh" }.take(100),
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
                                VehicleType.valueOf((m["vehicleType"] as? String) ?: "MOBIL")
                            }.getOrDefault(VehicleType.MOBIL),
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
                        roomId = doc.getString("roomId") ?: "UMR-0000",
                        tourTitle = doc.getString("tourTitle") ?: "Safar Rombongan",
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
                        destinationName = doc.getString("destinationName") ?: "Masjidil Haram (Ka'bah)",
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

    suspend fun loadManasikProgress(uid: String): ManasikProgressState? {
        val db = getFirestore() ?: return null
        return runCatching {
            val snap = db.collection("users").document(uid)
                .collection("manasik")
                .whereEqualTo("ownerUid", uid)
                .get()
                .await()
            var umrah = ManasikGuideProgress()
            var haji = ManasikGuideProgress()
            snap.documents.forEach { doc ->
                val guideId = doc.getString("guideId") ?: doc.id
                val completed = (doc.get("completedStepIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                val checkIns = linkedMapOf<String, ManasikCheckInRecord>()
                val rawCheckIns = (doc.get("checkInRecords") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                rawCheckIns.forEach { line ->
                    val parts = line.split("|")
                    if (parts.size >= 6) {
                        val stepId = parts[0].trim()
                        if (stepId.isNotEmpty()) {
                            checkIns[stepId] = ManasikCheckInRecord(
                                stepId = stepId,
                                landmarkName = parts[1].trim(),
                                checkedInAtLabel = parts[2].trim(),
                                distanceMeters = parts[3].trim().toIntOrNull() ?: 0,
                                latitude = parts[4].trim().toDoubleOrNull() ?: 21.4225,
                                longitude = parts[5].trim().toDoubleOrNull() ?: 39.8262
                            )
                        }
                    }
                }
                val guideProgress = ManasikGuideProgress(
                    completedStepIds = completed,
                    checkInRecords = checkIns,
                    updatedAtMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                )
                if (guideId == ManasikGuideType.UMRAH.id) {
                    umrah = guideProgress
                } else if (guideId == ManasikGuideType.HAJI.id) {
                    haji = guideProgress
                }
            }
            ManasikProgressState(umrah = umrah, haji = haji)
        }.getOrNull()
    }

    suspend fun saveManasikProgress(
        uid: String,
        guideType: ManasikGuideType,
        progress: ManasikGuideProgress
    ) {
        val db = getFirestore() ?: return
        val guideId = guideType.id
        val docRef = db.collection("users").document(uid)
            .collection("manasik").document(guideId)
        val completedList = progress.completedStepIds
            .map { it.trim().take(64) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(24)
        val checkInLines = progress.checkInRecords.values.take(24).map { r ->
            String.format(
                Locale.US,
                "%s|%s|%s|%d|%.5f|%.5f",
                r.stepId.take(40),
                r.landmarkName.replace("|", " ").take(80),
                r.checkedInAtLabel.take(24),
                r.distanceMeters.coerceAtLeast(0),
                r.latitude,
                r.longitude
            ).take(240)
        }
        runCatching {
            val exists = runCatching { docRef.get().await().exists() }.getOrDefault(false)
            if (exists) {
                docRef.update(
                    mapOf(
                        "completedStepIds" to completedList,
                        "checkInRecords" to checkInLines,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "guideId" to guideId,
                        "ownerUid" to uid,
                        "completedStepIds" to completedList,
                        "checkInRecords" to checkInLines,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    suspend fun addSyncedRoomActivity(
        roomId: String,
        createdByUid: String,
        activity: RoomActivityItem
    ) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        val safeId = activity.id.replace(Regex("[^a-zA-Z0-9_-]"), "").take(64)
            .ifBlank { "act-${System.currentTimeMillis()}" }
        runCatching {
            db.collection("rooms").document(cleanRoomId)
                .collection("activities").document(safeId)
                .set(
                    mapOf(
                        "id" to safeId,
                        "roomId" to cleanRoomId,
                        "createdByUid" to createdByUid.take(128),
                        "title" to activity.title.trim().ifBlank { "Kegiatan Umroh" }.take(100),
                        "timeLabel" to activity.timeLabel.trim().take(32),
                        "locationNote" to activity.locationNote.trim().take(120),
                        "orderIndex" to activity.orderIndex.coerceIn(0, 200),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
    }

    suspend fun updateSyncedRoomActivityOrder(
        roomId: String,
        activityId: String,
        orderIndex: Int
    ) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        val safeId = activityId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(64)
        if (safeId.isBlank()) return
        runCatching {
            db.collection("rooms").document(cleanRoomId)
                .collection("activities").document(safeId)
                .update(
                    mapOf(
                        "orderIndex" to orderIndex.coerceIn(0, 200),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
    }

    suspend fun removeSyncedRoomActivity(roomId: String, activityId: String) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        val safeId = activityId.replace(Regex("[^a-zA-Z0-9_-]"), "").take(64)
        if (safeId.isBlank()) return
        runCatching {
            db.collection("rooms").document(cleanRoomId)
                .collection("activities").document(safeId)
                .delete().await()
        }
    }

    suspend fun saveMemberRoomActivityProgress(
        roomId: String,
        uid: String,
        memberName: String,
        completedIds: List<String>,
        completedAtByActivity: Map<String, String>
    ) {
        val db = getFirestore() ?: return
        val cleanRoomId = normalizeRoomId(roomId)
        val docRef = db.collection("rooms").document(cleanRoomId)
            .collection("activityProgress").document(uid)
        val cleanIds = completedIds
            .map { it.trim().take(64) }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(40)
        val cleanRecords = cleanIds.map { actId ->
            val clock = completedAtByActivity[actId]?.trim().orEmpty().ifBlank { "Selesai" }
            "$actId|${clock.replace("|", " ")}".take(96)
        }
        val cleanName = memberName.trim().ifBlank { "Jamaah" }.take(80)
        runCatching {
            val exists = runCatching { docRef.get().await().exists() }.getOrDefault(false)
            if (exists) {
                docRef.update(
                    mapOf(
                        "memberName" to cleanName,
                        "completedActivityIds" to cleanIds,
                        "completedRecords" to cleanRecords,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "uid" to uid,
                        "roomId" to cleanRoomId,
                        "memberName" to cleanName,
                        "completedActivityIds" to cleanIds,
                        "completedRecords" to cleanRecords,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
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
                        announcementTitle = snap.getString("announcementTitle") ?: "Info Safar Tracking",
                        announcementMessage = snap.getString("announcementMessage") ?: "",
                        allowGuestMode = snap.getBoolean("allowGuestMode") ?: true,
                        allowNewRooms = snap.getBoolean("allowNewRooms") ?: true,
                        publicWebBaseUrl = snap.getString("publicWebBaseUrl")?.trim().orEmpty(),
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
            val cleanPublicUrl = normalizeHttpBaseUrl(config.publicWebBaseUrl)?.take(200) ?: ""
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
                "publicWebBaseUrl" to cleanPublicUrl,
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
                "fullName" to fullName.trim().ifBlank { "Jamaah Safar Tracking" }.take(80),
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
                    fullName = doc.getString("fullName") ?: "Jamaah Safar Tracking",
                    email = doc.getString("email").orEmpty(),
                    authProvider = AuthProviderType.fromId(doc.getString("authProvider")),
                    clubName = doc.getString("clubName").orEmpty(),
                    homeCity = doc.getString("homeCity").orEmpty(),
                    vehicleType = runCatching {
                        VehicleType.valueOf(doc.getString("vehicleType") ?: "MOBIL")
                    }.getOrDefault(VehicleType.MOBIL),
                    vehicleDetail = doc.getString("vehicleDetail").orEmpty(),
                    licensePlate = doc.getString("licensePlate").orEmpty(),
                    passportNumber = doc.getString("passportNumber").orEmpty(),
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
                                    fullName = mDoc.getString("fullName") ?: "Jamaah",
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
                    roomTitle = doc.getString("roomTitle") ?: "Rombongan Safar Live",
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

    fun isValidTourLeaderEmail(rawEmail: String): Boolean {
        val clean = rawEmail.trim().lowercase(Locale.ROOT)
        return clean.length in 3..160 &&
            Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$").matches(clean)
    }

    fun computeEmailSha256Hex(rawEmail: String): String {
        val clean = rawEmail.trim().lowercase(Locale.ROOT)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(clean.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }

    fun subscribeToTourLeaderEligibility(
        rawEmail: String?,
        onUpdate: (Boolean) -> Unit
    ): () -> Unit {
        val clean = rawEmail?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (clean.isBlank()) {
            onUpdate(false)
            return {}
        }
        if (isAdminEmail(clean)) {
            onUpdate(true)
            return {}
        }
        val db = getFirestore() ?: run {
            onUpdate(false)
            return {}
        }
        val hash = computeEmailSha256Hex(clean)
        val reg = db.collection("tour_leader_hashes").document(hash)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onUpdate(false)
                    return@addSnapshotListener
                }
                val eligible = snap != null && snap.exists() && (snap.getBoolean("active") != false)
                onUpdate(eligible)
            }
        return { reg.remove() }
    }

    fun subscribeToRegisteredTourLeaders(
        onUpdate: (List<RegisteredTourLeader>) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val db = getFirestore() ?: return {}
        val reg = db.collection("tour_leaders")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError("Gagal memuat daftar Ketua Rombongan: ${err.message}")
                    return@addSnapshotListener
                }
                if (snap == null) return@addSnapshotListener
                val items = snap.documents.map { doc ->
                    val ts = doc.getTimestamp("createdAt")
                    RegisteredTourLeader(
                        id = doc.getString("id") ?: doc.id,
                        email = (doc.getString("email") ?: "").trim().lowercase(Locale.ROOT),
                        label = doc.getString("label").orEmpty(),
                        addedByUid = doc.getString("addedByUid").orEmpty(),
                        createdAtLabel = formatTimestamp(ts, "Baru saja"),
                        createdAtMs = ts?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.createdAtMs }
                onUpdate(items)
            }
        return { reg.remove() }
    }

    suspend fun addRegisteredTourLeader(
        adminUid: String,
        rawEmail: String,
        rawLabel: String
    ): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanEmail = rawEmail.trim().lowercase(Locale.ROOT)
        if (!isValidTourLeaderEmail(cleanEmail)) {
            return Result.failure(IllegalArgumentException("Format email Ketua Rombongan tidak valid."))
        }
        val cleanLabel = rawLabel.trim().take(80)
        val hash = computeEmailSha256Hex(cleanEmail)
        return runCatching {
            val batch = db.batch()
            val leaderRef = db.collection("tour_leaders").document(hash)
            val hashRef = db.collection("tour_leader_hashes").document(hash)
            batch.set(
                leaderRef,
                mapOf(
                    "id" to hash,
                    "email" to cleanEmail,
                    "label" to cleanLabel,
                    "addedByUid" to adminUid.take(128),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                hashRef,
                mapOf(
                    "emailHash" to hash,
                    "active" to true,
                    "updatedByUid" to adminUid.take(128),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Unit
        }
    }

    suspend fun removeRegisteredTourLeader(leaderId: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanId = leaderId.trim().lowercase(Locale.ROOT)
        if (!Regex("^[a-f0-9]{64}$").matches(cleanId)) {
            return Result.failure(IllegalArgumentException("ID Ketua Rombongan tidak valid."))
        }
        return runCatching {
            val batch = db.batch()
            batch.delete(db.collection("tour_leaders").document(cleanId))
            batch.delete(db.collection("tour_leader_hashes").document(cleanId))
            batch.commit().await()
            Unit
        }
    }

    suspend fun publishTelemetryAwait(
        roomId: String,
        uid: String,
        latitude: Double,
        longitude: Double,
        speedKmh: Int,
        heading: Int,
        accuracyMeters: Int,
        routeProgress: Float
    ): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        return runCatching {
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
                ).await()
            Unit
        }
    }

    suspend fun sendMessageAwait(roomId: String, senderUid: String, msg: RoomMessage): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        val payload = hashMapOf<String, Any>(
            "id" to msg.id,
            "roomId" to cleanRoomId,
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
        return runCatching {
            db.collection("rooms").document(cleanRoomId)
                .collection("messages").document(msg.id)
                .set(payload)
                .await()
            Unit
        }
    }

    suspend fun sendPriorityBroadcast(
        roomId: String,
        senderUid: String,
        senderName: String,
        senderVehicle: String,
        senderAvatarPreset: AvatarPreset,
        broadcastId: String,
        title: String,
        message: String,
        atLabel: String
    ): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        val safeTitle = title.trim().take(120)
        val safeMsg = message.trim().take(500)
        if (safeTitle.isEmpty() || safeMsg.isEmpty()) {
            return Result.failure(IllegalArgumentException("Judul dan isi pengumuman wajib diisi."))
        }
        val safeId = broadcastId.trim().take(64).ifBlank { "brd-${System.currentTimeMillis()}" }
        val safeSender = senderName.trim().ifBlank { "Mutawwif" }.take(80)
        val safeAt = atLabel.trim().ifBlank { "Baru saja" }.take(32)
        val combinedText = "[PENGUMUMAN] $safeTitle: $safeMsg".take(500)

        return runCatching {
            val batch = db.batch()
            val roomRef = db.collection("rooms").document(cleanRoomId)
            batch.update(
                roomRef,
                mapOf(
                    "priorityBroadcastActive" to true,
                    "priorityBroadcastId" to safeId,
                    "priorityBroadcastTitle" to safeTitle,
                    "priorityBroadcastMessage" to safeMsg,
                    "priorityBroadcastSenderName" to safeSender,
                    "priorityBroadcastAtLabel" to safeAt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val msgRef = roomRef.collection("messages").document(safeId)
            batch.set(
                msgRef,
                mapOf(
                    "id" to safeId,
                    "roomId" to cleanRoomId,
                    "senderUid" to senderUid,
                    "senderName" to safeSender,
                    "senderVehicle" to senderVehicle.trim().ifBlank { "Pembimbing Rombongan" }.take(80),
                    "senderAvatarPreset" to senderAvatarPreset.name,
                    "type" to ChatMessageType.BROADCAST.name,
                    "textContent" to combinedText,
                    "voiceDurationSec" to 0,
                    "timestampLabel" to safeAt,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Unit
        }
    }

    suspend fun clearPriorityBroadcast(roomId: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        return runCatching {
            db.collection("rooms").document(cleanRoomId)
                .update(
                    mapOf(
                        "priorityBroadcastActive" to false,
                        "priorityBroadcastTitle" to "",
                        "priorityBroadcastMessage" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Unit
        }
    }

    suspend fun sendManasikChatMessage(
        webBaseUrl: String,
        message: String,
        history: List<AssistantChatMessage>,
        roleId: AssistantRoleId,
        guideType: String,
        completedCount: Int,
        totalCount: Int,
        currentLocationText: String,
        configuredPublicUrl: String = ""
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val configuredClean = normalizeHttpBaseUrl(configuredPublicUrl)
        val base = webBaseUrl.trim().trimEnd('/').ifBlank { configuredClean ?: SHARED_BASE_URL }
        runCatching {
            val historyArray = JSONArray()
            history.takeLast(20).forEach { entry ->
                historyArray.put(
                    JSONObject().apply {
                        put("role", if (entry.role == "model") "model" else "user")
                        put("text", entry.text)
                    }
                )
            }
            val contextObj = JSONObject().apply {
                put("guideType", guideType)
                put("completedCount", completedCount)
                put("totalCount", totalCount)
                if (currentLocationText.isNotBlank()) {
                    put("currentLocationText", currentLocationText)
                }
            }
            val bodyJson = JSONObject().apply {
                put("message", message.trim())
                put("history", historyArray)
                put("roleId", roleId.id)
                put("context", contextObj)
            }.toString()

            val attemptCall: (String) -> Pair<String, String> = { targetBase ->
                val endpoint = "$targetBase/api/gemini/chat"
                val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    instanceFollowRedirects = false
                    connectTimeout = 20000
                    readTimeout = 65000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    try {
                        conn.outputStream.use { os ->
                            os.write(bodyJson.toByteArray(StandardCharsets.UTF_8))
                        }
                    } catch (netErr: java.net.SocketTimeoutException) {
                        throw IllegalStateException(
                            "Koneksi ke server ($targetBase) melebihi batas waktu (timeout). Jika Cloud Run baru aktif (cold start), silakan tekan Kirim / Coba Lagi atau periksa URL Cloud Run di bawah."
                        )
                    } catch (hostErr: java.net.UnknownHostException) {
                        throw IllegalStateException(
                            "Alamat server ($targetBase) tidak ditemukan. Pastikan URL Cloud Run benar dan koneksi internet aktif."
                        )
                    } catch (connErr: java.net.ConnectException) {
                        throw IllegalStateException(
                            "Gagal terhubung ke server ($targetBase). Pastikan server Cloud Run aktif dan dapat diakses publik."
                        )
                    }
                    val code = try {
                        conn.responseCode
                    } catch (readTimeoutErr: java.net.SocketTimeoutException) {
                        throw IllegalStateException(
                            "Server ($targetBase) tidak merespons tepat waktu (timeout). Jika Cloud Run sedang cold start atau AI sedang memproses jawaban panjang, silakan coba kirim ulang."
                        )
                    }
                    val location = conn.getHeaderField("Location").orEmpty()
                    if (code in 300..399) {
                        if (location.contains("__cookie_check", ignoreCase = true) ||
                            location.contains("applet-auth-bridge", ignoreCase = true) ||
                            location.contains("accounts.google.com", ignoreCase = true) ||
                            location.contains("aistudio", ignoreCase = true)
                        ) {
                            throw IllegalStateException(
                                "Server ($targetBase) dilindungi proxy login AI Studio (applet-auth-bridge) sehingga APK Android tidak dapat memanggil /api/gemini/chat langsung. Deploy web ke Cloud Run (akses publik / unauthenticated), lalu buka web Cloud Run sebagai Admin atau masukkan URL Cloud Run (*.run.app) di bawah / di Admin Backend."
                            )
                        }
                        throw IllegalStateException(
                            "Server ($targetBase) mengalihkan request ($code). Pastikan URL menunjuk ke server Cloud Run Safar Tracking yang aktif."
                        )
                    }
                    val rawBody = (if (code in 200..299) conn.inputStream else conn.errorStream)
                        ?.bufferedReader(StandardCharsets.UTF_8)
                        ?.use { it.readText() }
                        .orEmpty()

                    if (code == 401 || code == 403 || rawBody.contains("accounts.google.com", ignoreCase = true)) {
                        throw IllegalStateException(
                            "Server AI ($targetBase) menolak akses tanpa cookie login. Pastikan web di-deploy ke Cloud Run dengan akses publik (Allow unauthenticated invocations)."
                        )
                    }
                    if (code == 404) {
                        val healthStatus = probeWebEndpoint(targetBase)
                        val detailMsg = when (healthStatus) {
                            WebEndpointStatus.NOT_FOUND ->
                                "Server Web ($targetBase) belum aktif (HTTP 404). Deploy web ke Cloud Run atau masukkan URL Cloud Run yang aktif di bawah."
                            WebEndpointStatus.DEV_PROXY_AUTH ->
                                "Server ($targetBase) dilindungi proxy login AI Studio (applet-auth-bridge). Gunakan URL hasil deploy Cloud Run publik (non-ais-pre/ais-dev)."
                            WebEndpointStatus.OUTDATED_BUILD ->
                                "Web di ($targetBase) belum menjalankan server backend Express (/api/gemini/chat). Pastikan aplikasi dijalankan lewat server full-stack (npm start)."
                            WebEndpointStatus.LIVE_DIRECT ->
                                "Endpoint /api/gemini/chat belum ditemukan di server ($targetBase). Deploy ulang versi web terbaru."
                            WebEndpointStatus.UNREACHABLE ->
                                "Endpoint /api/gemini/chat tidak ditemukan di server ($targetBase) (HTTP 404)."
                        }
                        throw IllegalStateException(detailMsg)
                    }
                    val json = runCatching { JSONObject(rawBody) }.getOrNull()
                    if (code !in 200..299 || json == null || !json.optBoolean("ok", false)) {
                        val rawErr = json?.optString("error").orEmpty()
                        val serverErr = when {
                            code == 503 ||
                                rawErr.contains("503") ||
                                rawErr.contains("high demand", ignoreCase = true) ||
                                rawErr.contains("UNAVAILABLE", ignoreCase = true) ->
                                "Server AI Gemini sedang mengalami lonjakan trafik sementara (503). Silakan tunggu beberapa detik lalu tekan Kirim Ulang."
                            rawErr.isNotBlank() -> rawErr
                            else -> "Gagal menghubungi Mutawwif AI (HTTP $code)."
                        }
                        throw IllegalStateException(serverErr)
                    }
                    val reply = json.optString("reply", "").trim()
                    if (reply.isEmpty()) {
                        throw IllegalStateException("Jawaban Mutawwif AI kosong.")
                    }
                    val model = json.optString("model", "gemini-4-hydrogen")
                    reply to model
                } finally {
                    conn.disconnect()
                }
            }

            try {
                attemptCall(base)
            } catch (firstErr: Throwable) {
                val msg = firstErr.message.orEmpty()
                val isUpstreamRateOrOverload = msg.contains("503") ||
                    msg.contains("kuota", ignoreCase = true) ||
                    msg.contains("lonjakan trafik", ignoreCase = true)
                if (isUpstreamRateOrOverload) {
                    throw firstErr
                }
                val fallbackCandidates = listOfNotNull(configuredClean, SHARED_BASE_URL)
                    .distinct()
                    .filter { it != base }
                val liveFallback = fallbackCandidates.firstOrNull {
                    probeWebEndpoint(it) == WebEndpointStatus.LIVE_DIRECT
                }
                if (liveFallback != null) {
                    attemptCall(liveFallback)
                } else {
                    throw firstErr
                }
            }
        }
    }

    // --- Family Monitoring (Personal Invite Links, Grants & Single-Member Monitor) ---

    fun extractFamilyInviteCode(rawInput: String): String? {
        val upper = rawInput.trim().uppercase(Locale.ROOT)
        if (upper.isEmpty()) return null
        Regex("[?&]FAMILY=(FAM-[A-Z0-9]{4,28})")
            .find(upper)?.groupValues?.getOrNull(1)?.let { return it.take(32) }
        Regex("/FAMILY/(FAM-[A-Z0-9]{4,28})")
            .find(upper)?.groupValues?.getOrNull(1)?.let { return it.take(32) }
        return Regex("FAM-[A-Z0-9]{4,28}")
            .find(upper)?.value?.take(32)
    }

    fun generateFamilyInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..6).map { alphabet[(Math.random() * alphabet.length).toInt().coerceIn(0, alphabet.lastIndex)] }.joinToString("")
        return "FAM-$suffix"
    }

    fun buildFamilyInviteLinks(inviteCode: String, webBaseUrl: String = SHARED_BASE_URL): Pair<String, String> {
        val safeCode = extractFamilyInviteCode(inviteCode) ?: inviteCode.trim().uppercase(Locale.ROOT)
        val base = webBaseUrl.trim().trimEnd('/').ifBlank { SHARED_BASE_URL }
        val webLink = "$base/?family=${Uri.encode(safeCode)}"
        val deepLink = "safartracking://family/$safeCode"
        return webLink to deepLink
    }

    private fun mapDocToFamilyLink(
        inviteCode: String,
        getField: (String) -> Any?,
        createdTs: Timestamp?,
        updatedTs: Timestamp?
    ): FamilyLinkInfo {
        val presetName = (getField("targetAvatarPreset") as? String)?.trim().orEmpty()
        val preset = runCatching { AvatarPreset.valueOf(presetName) }.getOrDefault(AvatarPreset.HELMET_TOURING)
        return FamilyLinkInfo(
            inviteCode = ((getField("inviteCode") as? String)?.ifBlank { inviteCode }) ?: inviteCode,
            roomId = (getField("roomId") as? String).orEmpty(),
            roomTitle = ((getField("roomTitle") as? String)?.ifBlank { "Rombongan Safar" }) ?: "Rombongan Safar",
            targetUid = (getField("targetUid") as? String).orEmpty(),
            targetName = ((getField("targetName") as? String)?.ifBlank { "Jamaah" }) ?: "Jamaah",
            targetVehicleDetail = (getField("targetVehicleDetail") as? String).orEmpty(),
            targetAvatarPreset = preset,
            destinationName = (getField("destinationName") as? String).orEmpty(),
            active = getField("active") as? Boolean ?: false,
            claimedByUid = (getField("claimedByUid") as? String).orEmpty(),
            claimedByName = (getField("claimedByName") as? String).orEmpty(),
            createdAtLabel = formatTimestamp(createdTs, "Baru saja"),
            updatedAtLabel = formatTimestamp(updatedTs, "Baru saja")
        )
    }

    fun subscribeToMyFamilyLink(
        roomId: String,
        targetUid: String,
        onUpdate: (FamilyLinkInfo?) -> Unit
    ): () -> Unit {
        val db = getFirestore()
        val cleanRoomId = normalizeRoomId(roomId)
        if (db == null || cleanRoomId.isBlank() || targetUid.isBlank()) {
            onUpdate(null)
            return {}
        }
        val reg = db.collection("family_links")
            .whereEqualTo("targetUid", targetUid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                val matching = snap.documents
                    .map { doc ->
                        val sortMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                        val info = mapDocToFamilyLink(
                            inviteCode = doc.id,
                            getField = { key -> doc.get(key) },
                            createdTs = doc.getTimestamp("createdAt"),
                            updatedTs = doc.getTimestamp("updatedAt")
                        )
                        sortMs to info
                    }
                    .filter { (_, info) -> info.roomId == cleanRoomId && info.active }
                    .sortedByDescending { (ms, _) -> ms }
                onUpdate(matching.firstOrNull()?.second)
            }
        return { reg.remove() }
    }

    suspend fun createOrGetFamilyInviteLink(
        roomId: String,
        roomTitle: String,
        targetUid: String,
        targetName: String,
        targetVehicleDetail: String,
        targetAvatarPreset: AvatarPreset,
        destinationName: String
    ): Result<FamilyLinkInfo> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        if (cleanRoomId.isBlank() || targetUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Data room atau akun jamaah tidak valid."))
        }
        return runCatching {
            val existingSnap = db.collection("family_links")
                .whereEqualTo("targetUid", targetUid)
                .get()
                .await()
            val activeExisting = existingSnap.documents
                .map { doc ->
                    mapDocToFamilyLink(
                        inviteCode = doc.id,
                        getField = { key -> doc.get(key) },
                        createdTs = doc.getTimestamp("createdAt"),
                        updatedTs = doc.getTimestamp("updatedAt")
                    )
                }
                .firstOrNull { it.roomId == cleanRoomId && it.active }
            if (activeExisting != null) {
                return@runCatching activeExisting
            }

            val inviteCode = generateFamilyInviteCode()
            val safeRoomTitle = roomTitle.trim().ifBlank { "Rombongan Safar" }.take(100)
            val safeTargetUid = targetUid.take(128)
            val safeTargetName = targetName.trim().ifBlank { "Jamaah" }.take(80)
            val safeVehicle = targetVehicleDetail.trim().take(80)
            val safePreset = targetAvatarPreset.name.take(40)
            val safeDest = destinationName.trim().take(120)

            val payload = hashMapOf<String, Any>(
                "inviteCode" to inviteCode,
                "roomId" to cleanRoomId.take(32),
                "roomTitle" to safeRoomTitle,
                "targetUid" to safeTargetUid,
                "targetName" to safeTargetName,
                "targetVehicleDetail" to safeVehicle,
                "targetAvatarPreset" to safePreset,
                "destinationName" to safeDest,
                "active" to true,
                "claimedByUid" to "",
                "claimedByName" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("family_links").document(inviteCode).set(payload).await()
            FamilyLinkInfo(
                inviteCode = inviteCode,
                roomId = cleanRoomId.take(32),
                roomTitle = safeRoomTitle,
                targetUid = safeTargetUid,
                targetName = safeTargetName,
                targetVehicleDetail = safeVehicle,
                targetAvatarPreset = targetAvatarPreset,
                destinationName = safeDest,
                active = true,
                claimedByUid = "",
                claimedByName = "",
                createdAtLabel = "Baru saja",
                updatedAtLabel = "Baru saja"
            )
        }
    }

    suspend fun regenerateFamilyInviteLink(
        previousInviteCode: String?,
        roomId: String,
        roomTitle: String,
        targetUid: String,
        targetName: String,
        targetVehicleDetail: String,
        targetAvatarPreset: AvatarPreset,
        destinationName: String
    ): Result<FamilyLinkInfo> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val cleanRoomId = normalizeRoomId(roomId)
        return runCatching {
            val cleanPrev = previousInviteCode?.let { extractFamilyInviteCode(it) }
            if (!cleanPrev.isNullOrBlank()) {
                runCatching {
                    db.collection("family_links").document(cleanPrev)
                        .update(
                            mapOf(
                                "active" to false,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                }
            }
            val inviteCode = generateFamilyInviteCode()
            val safeRoomTitle = roomTitle.trim().ifBlank { "Rombongan Safar" }.take(100)
            val safeTargetUid = targetUid.take(128)
            val safeTargetName = targetName.trim().ifBlank { "Jamaah" }.take(80)
            val safeVehicle = targetVehicleDetail.trim().take(80)
            val safePreset = targetAvatarPreset.name.take(40)
            val safeDest = destinationName.trim().take(120)

            val payload = hashMapOf<String, Any>(
                "inviteCode" to inviteCode,
                "roomId" to cleanRoomId.take(32),
                "roomTitle" to safeRoomTitle,
                "targetUid" to safeTargetUid,
                "targetName" to safeTargetName,
                "targetVehicleDetail" to safeVehicle,
                "targetAvatarPreset" to safePreset,
                "destinationName" to safeDest,
                "active" to true,
                "claimedByUid" to "",
                "claimedByName" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("family_links").document(inviteCode).set(payload).await()
            FamilyLinkInfo(
                inviteCode = inviteCode,
                roomId = cleanRoomId.take(32),
                roomTitle = safeRoomTitle,
                targetUid = safeTargetUid,
                targetName = safeTargetName,
                targetVehicleDetail = safeVehicle,
                targetAvatarPreset = targetAvatarPreset,
                destinationName = safeDest,
                active = true,
                claimedByUid = "",
                claimedByName = "",
                createdAtLabel = "Baru saja",
                updatedAtLabel = "Baru saja"
            )
        }
    }

    suspend fun revokeFamilyInviteLink(inviteCode: String): Result<Unit> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val clean = extractFamilyInviteCode(inviteCode)
            ?: return Result.failure(IllegalArgumentException("Kode undangan keluarga tidak valid."))
        return runCatching {
            db.collection("family_links").document(clean)
                .update(
                    mapOf(
                        "active" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Unit
        }
    }

    suspend fun redeemFamilyInviteLink(
        rawCodeOrUrl: String,
        familyUid: String,
        familyName: String
    ): Result<FamilyGrantInfo> {
        val db = getFirestore()
            ?: return Result.failure(IllegalStateException("Firestore tidak tersedia."))
        val inviteCode = extractFamilyInviteCode(rawCodeOrUrl)
            ?: return Result.failure(
                IllegalArgumentException(
                    "Kode atau link pantauan keluarga tidak valid. Gunakan link/kode berformat FAM-XXXXXX dari anggota rombongan."
                )
            )
        return runCatching {
            val linkRef = db.collection("family_links").document(inviteCode)
            val linkSnap = linkRef.get().await()
            if (!linkSnap.exists()) {
                throw IllegalStateException(
                    "Kode pantauan keluarga \"$inviteCode\" tidak ditemukan. Pastikan link berasal dari anggota rombongan yang aktif."
                )
            }
            val linkData = mapDocToFamilyLink(
                inviteCode = inviteCode,
                getField = { key -> linkSnap.get(key) },
                createdTs = linkSnap.getTimestamp("createdAt"),
                updatedTs = linkSnap.getTimestamp("updatedAt")
            )
            if (!linkData.active) {
                throw IllegalStateException(
                    "Link pantauan keluarga \"$inviteCode\" sudah dinonaktifkan oleh anggota rombongan."
                )
            }
            if (linkData.claimedByUid.isNotBlank() && linkData.claimedByUid != familyUid) {
                val claimedLabel = linkData.claimedByName.ifBlank { "Terdaftar" }
                throw IllegalStateException(
                    "Link ini sudah terhubung ke akun keluarga lain ($claimedLabel). Minta anggota rombongan membuat kode baru jika ingin mengganti perangkat."
                )
            }

            val grantRef = db.collection("family_grants").document(familyUid)
            val grantSnap = grantRef.get().await()
            val cleanFamilyName = familyName.trim().ifBlank { "Keluarga Jamaah" }.take(80)

            val batch = db.batch()
            batch.update(
                linkRef,
                mapOf(
                    "claimedByUid" to familyUid.take(128),
                    "claimedByName" to cleanFamilyName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (grantSnap.exists()) {
                batch.update(
                    grantRef,
                    mapOf(
                        "inviteCode" to inviteCode,
                        "roomId" to linkData.roomId.take(32),
                        "targetUid" to linkData.targetUid.take(128),
                        "targetName" to linkData.targetName.take(80),
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    grantRef,
                    mapOf(
                        "familyUid" to familyUid.take(128),
                        "inviteCode" to inviteCode,
                        "roomId" to linkData.roomId.take(32),
                        "targetUid" to linkData.targetUid.take(128),
                        "targetName" to linkData.targetName.take(80),
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            FamilyGrantInfo(
                familyUid = familyUid,
                inviteCode = inviteCode,
                roomId = linkData.roomId,
                targetUid = linkData.targetUid,
                targetName = linkData.targetName,
                active = true
            )
        }
    }

    suspend fun loadActiveFamilyGrant(familyUid: String): FamilyGrantInfo? {
        val db = getFirestore() ?: return null
        if (familyUid.isBlank()) return null
        return runCatching {
            val grantSnap = db.collection("family_grants").document(familyUid).get().await()
            if (!grantSnap.exists()) return@runCatching null
            if (grantSnap.getBoolean("active") != true) return@runCatching null
            val inviteCode = grantSnap.getString("inviteCode").orEmpty()
            val roomId = grantSnap.getString("roomId").orEmpty()
            val targetUid = grantSnap.getString("targetUid").orEmpty()
            if (inviteCode.isBlank() || roomId.isBlank() || targetUid.isBlank()) return@runCatching null

            val linkSnap = db.collection("family_links").document(inviteCode).get().await()
            if (!linkSnap.exists()) return@runCatching null
            if (linkSnap.getBoolean("active") != true) return@runCatching null
            val claimedByUid = linkSnap.getString("claimedByUid").orEmpty()
            if (claimedByUid.isNotBlank() && claimedByUid != familyUid) return@runCatching null

            FamilyGrantInfo(
                familyUid = familyUid,
                inviteCode = inviteCode,
                roomId = roomId,
                targetUid = targetUid,
                targetName = grantSnap.getString("targetName")
                    ?.ifBlank { linkSnap.getString("targetName").orEmpty() }
                    ?.ifBlank { "Jamaah" } ?: "Jamaah",
                active = true
            )
        }.getOrNull()
    }

    suspend fun clearFamilyGrant(familyUid: String) {
        val db = getFirestore() ?: return
        if (familyUid.isBlank()) return
        runCatching {
            val grantRef = db.collection("family_grants").document(familyUid)
            val snap = grantRef.get().await()
            if (snap.exists() && snap.getBoolean("active") == true) {
                grantRef.update(
                    mapOf(
                        "active" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
    }

    fun subscribeToFamilyMonitor(
        grant: FamilyGrantInfo,
        onSnapshotUpdate: (FamilyMonitorSnapshot) -> Unit,
        onError: (String) -> Unit
    ): () -> Unit {
        val db = getFirestore() ?: return {}
        val cleanRoomId = normalizeRoomId(grant.roomId)
        val lock = Any()
        var snapshotState = FamilyMonitorSnapshot()

        fun emit() {
            val clock = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val copy = synchronized(lock) {
                snapshotState = snapshotState.copy(lastSyncLabel = clock)
                snapshotState
            }
            onSnapshotUpdate(copy)
        }

        val regs = mutableListOf<ListenerRegistration>()

        regs += db.collection("family_links").document(grant.inviteCode)
            .addSnapshotListener { snap, _ ->
                synchronized(lock) {
                    if (snap == null || !snap.exists()) {
                        snapshotState = snapshotState.copy(linkActive = false)
                    } else {
                        val claimed = snap.getString("claimedByUid").orEmpty()
                        val stillValid = (snap.getBoolean("active") == true) &&
                            (claimed.isEmpty() || claimed == grant.familyUid)
                        snapshotState = snapshotState.copy(
                            linkActive = stillValid,
                            roomTitle = snap.getString("roomTitle")?.ifBlank { snapshotState.roomTitle } ?: snapshotState.roomTitle,
                            destinationName = snap.getString("destinationName")?.ifBlank { snapshotState.destinationName } ?: snapshotState.destinationName
                        )
                    }
                }
                emit()
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    onError("Gagal memuat info rombongan: ${err.localizedMessage ?: "akses ditolak"}")
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    if (snap == null || !snap.exists()) {
                        snapshotState = snapshotState.copy(roomStatus = "FINISHED")
                    } else {
                        val decoded = GeoUtils.decodePolyline(snap.getString("encodedPolyline").orEmpty())
                        val originLat = snap.getDouble("originLat") ?: 21.4180
                        val originLng = snap.getDouble("originLng") ?: 39.8275
                        val destLat = snap.getDouble("destLat") ?: 21.4225
                        val destLng = snap.getDouble("destLng") ?: 39.8262
                        val waypoints = if (decoded.size >= 2) {
                            GeoUtils.toRoutePoints(decoded)
                        } else {
                            listOf(
                                RoutePoint(0.14f, 0.86f, originLat, originLng),
                                RoutePoint(0.86f, 0.14f, destLat, destLng)
                            )
                        }
                        val destName = snap.getString("destinationName")?.ifBlank { "Tujuan Ziarah" } ?: "Tujuan Ziarah"
                        val destAddr = snap.getString("destinationAddress").orEmpty()
                        val cfg = ConvoyRouteConfig(
                            originName = snap.getString("originName")?.ifBlank { "Titik Keberangkatan" } ?: "Titik Keberangkatan",
                            originAddress = snap.getString("originAddress").orEmpty(),
                            originLockedByLeader = true,
                            destinationName = destName,
                            destinationAddress = destAddr,
                            estimatedDurationMillis = (snap.getLong("estimatedDurationMillis") ?: 0L).coerceAtLeast(0L),
                            waypoints = waypoints,
                            checkpoints = emptyList(),
                            totalRouteMeters = (snap.getLong("totalRouteMeters") ?: 12000L).toInt(),
                            offRouteToleranceMeters = (snap.getLong("offRouteToleranceMeters") ?: 75L).toInt(),
                            maxStragglerDistanceMeters = (snap.getLong("maxStragglerDistanceMeters") ?: 1000L).toInt(),
                            soundAlertEnabled = false
                        )
                        snapshotState = snapshotState.copy(
                            roomStatus = if (snap.getString("status") == "FINISHED") "FINISHED" else "ACTIVE",
                            roomTitle = snap.getString("roomTitle")?.ifBlank { snapshotState.roomTitle } ?: snapshotState.roomTitle,
                            destinationName = destName,
                            destinationAddress = destAddr,
                            routeConfig = cfg
                        )
                    }
                }
                emit()
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("members").document(grant.targetUid)
            .addSnapshotListener { doc, err ->
                if (err != null) {
                    onError("Akses posisi jamaah dibatasi atau terputus: ${err.localizedMessage ?: "akses ditolak"}")
                    return@addSnapshotListener
                }
                synchronized(lock) {
                    if (doc == null || !doc.exists()) {
                        snapshotState = snapshotState.copy(targetMember = null)
                    } else {
                        val updatedTs = doc.getTimestamp("updatedAt")
                        val member = ConvoyMember(
                            id = doc.getString("uid") ?: doc.id,
                            fullName = doc.getString("fullName") ?: grant.targetName.ifBlank { "Jamaah" },
                            vehicleType = runCatching {
                                VehicleType.valueOf(doc.getString("vehicleType") ?: "MOBIL")
                            }.getOrDefault(VehicleType.MOBIL),
                            vehicleDetail = doc.getString("vehicleDetail").orEmpty(),
                            licensePlate = doc.getString("licensePlate").orEmpty(),
                            emergencyContact = "",
                            role = runCatching {
                                UserRole.valueOf(doc.getString("role") ?: "MEMBER")
                            }.getOrDefault(UserRole.MEMBER),
                            joinedAtLabel = formatTimestamp(doc.getTimestamp("joinedAt"), "Aktif"),
                            isCurrentUser = true,
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
                            lastLocationText = doc.getString("lastLocationText") ?: "GPS Aktif",
                            lastUpdatedLabel = formatTimestamp(updatedTs, "Baru saja"),
                            convoyOrderRank = 1,
                            mapPosition = MapPosition(
                                routeProgress = (doc.getDouble("routeProgress") ?: 0.05).toFloat(),
                                xFraction = (doc.getDouble("xFraction") ?: 0.5).toFloat(),
                                yFraction = (doc.getDouble("yFraction") ?: 0.5).toFloat(),
                                latitude = doc.getDouble("latitude") ?: 21.4225,
                                longitude = doc.getDouble("longitude") ?: 39.8262,
                                speedKmh = (doc.getLong("speedKmh") ?: 0L).toInt(),
                                heading = (doc.getLong("heading") ?: 0L).toInt(),
                                accuracyMeters = (doc.getLong("accuracyMeters") ?: 20L).toInt()
                            )
                        )
                        snapshotState = snapshotState.copy(targetMember = member)
                    }
                }
                emit()
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("activities")
            .whereEqualTo("roomId", cleanRoomId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val list = snap.documents.mapIndexed { idx, doc ->
                    RoomActivityItem(
                        id = doc.getString("id") ?: doc.id,
                        title = doc.getString("title") ?: "Kegiatan Umroh",
                        timeLabel = doc.getString("timeLabel").orEmpty(),
                        locationNote = doc.getString("locationNote").orEmpty(),
                        orderIndex = (doc.getLong("orderIndex") ?: idx.toLong()).toInt(),
                        createdByUid = doc.getString("createdByUid").orEmpty(),
                        createdAtMs = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        updatedAtMs = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                    )
                }.sortedWith(compareBy<RoomActivityItem> { it.orderIndex }.thenBy { it.createdAtMs })
                synchronized(lock) {
                    snapshotState = snapshotState.copy(activities = list)
                }
                emit()
            }

        regs += db.collection("rooms").document(cleanRoomId)
            .collection("activityProgress").document(grant.targetUid)
            .addSnapshotListener { doc, _ ->
                synchronized(lock) {
                    if (doc == null || !doc.exists()) {
                        snapshotState = snapshotState.copy(
                            completedActivityIds = emptyList(),
                            completedTimes = emptyMap()
                        )
                    } else {
                        val completedIds = (doc.get("completedActivityIds") as? List<*>)
                            ?.mapNotNull { it as? String }
                            ?.filter { it.isNotBlank() }
                            .orEmpty()
                        val recordMap = linkedMapOf<String, String>()
                        val rawRecords = (doc.get("completedRecords") as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty()
                        rawRecords.forEach { rec ->
                            val parts = rec.split("|", limit = 2)
                            val actId = parts.getOrNull(0)?.trim().orEmpty()
                            val time = parts.getOrNull(1)?.trim().orEmpty()
                            if (actId.isNotEmpty() && time.isNotEmpty()) {
                                recordMap[actId] = time
                            }
                        }
                        snapshotState = snapshotState.copy(
                            completedActivityIds = completedIds,
                            completedTimes = recordMap
                        )
                    }
                }
                emit()
            }

        return { regs.forEach { it.remove() } }
    }

    private fun formatTimestamp(ts: Timestamp?, fallback: String): String {
        val date: Date = ts?.toDate() ?: return fallback
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    }
}
