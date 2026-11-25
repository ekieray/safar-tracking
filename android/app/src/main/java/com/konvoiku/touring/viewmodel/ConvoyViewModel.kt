package com.konvoiku.touring.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.auth.FirebaseUser
import com.konvoiku.touring.data.FirestoreConvoyRepository
import com.konvoiku.touring.data.LocalBrowserGoogleAuthServer
import com.konvoiku.touring.location.GpsLocationTracker
import com.konvoiku.touring.model.AuthProviderType
import com.konvoiku.touring.model.AvatarPreset
import com.konvoiku.touring.model.ChatMessageType
import com.konvoiku.touring.model.ConnectionStatus
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.ConvoyRouteConfig
import com.konvoiku.touring.model.ConvoyUiState
import com.konvoiku.touring.model.GlobalAppConfig
import com.konvoiku.touring.model.GpsTrackingStatus
import com.konvoiku.touring.model.MapPosition
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.OffRouteAlertState
import com.konvoiku.touring.model.RestCheckpoint
import com.konvoiku.touring.model.RoomMessage
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePoint
import com.konvoiku.touring.model.RoutePreference
import com.konvoiku.touring.model.SosEmergencyState
import com.konvoiku.touring.model.StragglerAlertState
import com.konvoiku.touring.model.TourRoom
import com.konvoiku.touring.model.TripHistoryRecord
import com.konvoiku.touring.model.TripParticipantSummary
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.model.VehicleProfile
import com.konvoiku.touring.model.VehicleType
import com.konvoiku.touring.model.VisitedCheckpointLog
import com.konvoiku.touring.util.GeoUtils
import com.konvoiku.touring.util.LatLngGeo
import org.json.JSONObject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.random.Random

class ConvoyViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ConvoyUiState())
    val uiState: StateFlow<ConvoyUiState> = _uiState.asStateFlow()

    private val gpsTracker = GpsLocationTracker(application.applicationContext)
    private val syncRepo = FirestoreConvoyRepository()
    private val localBrowserAuthServer = LocalBrowserGoogleAuthServer()
    private val profilePrefs = application.applicationContext
        .getSharedPreferences("konvoiku_profiles", Context.MODE_PRIVATE)

    private var voiceTimerJob: Job? = null
    private var voiceStatusClearJob: Job? = null
    private var voiceRecorder: MediaRecorder? = null
    private var voiceOutputFile: File? = null
    private var voiceRecordingStartMs: Long = 0L
    private var voicePlayer: MediaPlayer? = null
    private var gpsJob: Job? = null
    private var presenceJob: Job? = null
    private var roomUnsubscribe: (() -> Unit)? = null
    private var configUnsubscribe: (() -> Unit)? = null
    private var adminMonitorUnsubscribe: (() -> Unit)? = null
    private var authUnsubscribe: (() -> Unit)? = null
    private var lastTelemetrySentMs = 0L
    private val recordedTrackPoints = mutableListOf<LatLngGeo>()
    private var lastComputedRouteSig = ""
    private var lastSyncedCoLeaderUid = ""
    private var lastFailoverAttemptMs = 0L

    private data class SimTemplate(
        val name: String,
        val vehicleType: VehicleType,
        val vehicleDetail: String,
        val plate: String,
        val phone: String,
        val status: ConnectionStatus,
        val condition: MemberConditionStatus,
        val locationText: String,
        val baseProgress: Float,
        val baseSpeed: Int,
        val avatarPreset: AvatarPreset
    )

    data class DestinationPreset(
        val name: String,
        val endPoint: RoutePoint,
        val totalMeters: Int
    )

    val destinationPresets = listOf(
        DestinationPreset("Puncak Pass Resort", RoutePoint(0.86f, 0.14f, -6.7003, 106.9960), 16500),
        DestinationPreset("Lembang Highland", RoutePoint(0.78f, 0.22f, -6.8120, 107.6180), 15000),
        DestinationPreset("Ciwidey Kawah Putih", RoutePoint(0.24f, 0.18f, -7.1660, 107.4020), 18000),
        DestinationPreset("Rest Area KM 57", RoutePoint(0.88f, 0.44f, -6.3690, 107.3590), 9500)
    )

    private val defaultConvoyTemplates = listOf(
        SimTemplate(
            name = "Budi",
            vehicleType = VehicleType.MOBIL,
            vehicleDetail = "Avanza Hitam",
            plate = "B 1402 KNV",
            phone = "0812-3301-7712",
            status = ConnectionStatus.CONNECTED,
            condition = MemberConditionStatus.AMAN,
            locationText = "Simpang Gadog KM 44",
            baseProgress = 0.68f,
            baseSpeed = 58,
            avatarPreset = AvatarPreset.RIDER_CAPTAIN
        ),
        SimTemplate(
            name = "Siti",
            vehicleType = VehicleType.MOTOR,
            vehicleDetail = "Nmax Putih",
            plate = "D 4821 XZT",
            phone = "0813-9012-4411",
            status = ConnectionStatus.CONNECTED,
            condition = MemberConditionStatus.AMAN,
            locationText = "Jalur Puncak KM 46",
            baseProgress = 0.60f,
            baseSpeed = 54,
            avatarPreset = AvatarPreset.HELMET_TOURING
        ),
        SimTemplate(
            name = "Raka",
            vehicleType = VehicleType.MOTOR,
            vehicleDetail = "Vario 160 Merah",
            plate = "B 6190 TRG",
            phone = "0857-2201-8834",
            status = ConnectionStatus.WEAK_SIGNAL,
            condition = MemberConditionStatus.ISTIRAHAT,
            locationText = "Rest Area Ciawi KM 41",
            baseProgress = 0.54f,
            baseSpeed = 48,
            avatarPreset = AvatarPreset.HELMET_SPORT
        ),
        SimTemplate(
            name = "Nadia",
            vehicleType = VehicleType.MOBIL,
            vehicleDetail = "HR-V Abu-abu",
            plate = "B 2048 KNV",
            phone = "0811-3349-7720",
            status = ConnectionStatus.CONNECTED,
            condition = MemberConditionStatus.AMAN,
            locationText = "Tol Jagorawi KM 39",
            baseProgress = 0.46f,
            baseSpeed = 62,
            avatarPreset = AvatarPreset.PILOT_CLASSIC
        )
    )

    private var extraMemberCounter = 0

    init {
        FirestoreConvoyRepository.init(application.applicationContext)
        _uiState.update { state ->
            val seeded = defaultTripHistory()
            state.copy(
                tripHistory = seeded,
                selectedTripId = seeded.firstOrNull()?.id,
                isCloudSyncConnected = syncRepo.isAvailable(),
                isVerifiedAdmin = syncRepo.isVerifiedAdminSession()
            )
        }
        restoreSessionAndProfile()
        attachAuthStateObserver()
        subscribeToGlobalConfig()
        startPresenceHeartbeatLoop()
        startLiveMapTicker()
        viewModelScope.launch {
            val savedCustomUrl = profilePrefs.getString("custom_web_base_url", null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val activeBase = syncRepo.resolveActiveWebBaseUrl(savedCustomUrl)
            _uiState.update { it.copy(activeWebBaseUrl = activeBase) }
        }
        if (gpsTracker.hasLocationPermission()) {
            startGpsTracking()
        }
    }

    override fun onCleared() {
        super.onCleared()
        localBrowserAuthServer.stop()
        authUnsubscribe?.invoke()
        roomUnsubscribe?.invoke()
        configUnsubscribe?.invoke()
        adminMonitorUnsubscribe?.invoke()
        presenceJob?.cancel()
        gpsJob?.cancel()
        voiceTimerJob?.cancel()
        voiceStatusClearJob?.cancel()
        releaseVoiceRecorder(deleteFile = true)
        stopVoicePlayback()
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            startGpsTracking()
        } else {
            _uiState.update { it.copy(gpsStatus = GpsTrackingStatus.DENIED) }
        }
    }

    fun startGpsTracking() {
        if (!gpsTracker.hasLocationPermission()) {
            _uiState.update { it.copy(gpsStatus = GpsTrackingStatus.DENIED) }
            return
        }
        if (gpsJob?.isActive == true) return

        _uiState.update { it.copy(gpsStatus = GpsTrackingStatus.REQUESTING) }
        gpsJob = viewModelScope.launch {
            gpsTracker.locationUpdates(intervalMs = 2000L).collect { sample ->
                val state = _uiState.value
                val eval = GeoUtils.evaluatePositionAlongRoute(
                    pos = LatLngGeo(sample.latitude, sample.longitude),
                    waypoints = state.routeConfig.waypoints,
                    totalRouteMetersFallback = state.routeConfig.totalRouteMeters
                )
                _uiState.update { cur ->
                    cur.copy(
                        gpsStatus = GpsTrackingStatus.ACTIVE,
                        gpsAccuracyMeters = sample.accuracyMeters,
                        userGpsLat = sample.latitude,
                        userGpsLng = sample.longitude
                    )
                }

                val room = state.activeRoom ?: return@collect
                recordGpsPoint(sample.latitude, sample.longitude, sample.accuracyMeters)
                val updatedMembers = room.members.map { m ->
                    if (m.isCurrentUser) {
                        m.copy(
                            connectionStatus = ConnectionStatus.CONNECTED,
                            lastLocationText = String.format(
                                Locale.US,
                                "GPS (%.4f, %.4f)",
                                sample.latitude,
                                sample.longitude
                            ),
                            lastUpdatedLabel = "GPS Live",
                            mapPosition = m.mapPosition.copy(
                                routeProgress = eval.routeProgress,
                                latitude = sample.latitude,
                                longitude = sample.longitude,
                                speedKmh = sample.speedKmh,
                                heading = sample.headingDegrees,
                                accuracyMeters = sample.accuracyMeters
                            )
                        )
                    } else m
                }
                recomputeAndCommitRoom(updatedMembers)

                if (room.isSyncedRoom) {
                    val uid = syncRepo.currentUid()
                    val now = System.currentTimeMillis()
                    if (uid != null && now - lastTelemetrySentMs >= 2000L) {
                        lastTelemetrySentMs = now
                        syncRepo.publishTelemetry(
                            roomId = room.roomId,
                            uid = uid,
                            latitude = sample.latitude,
                            longitude = sample.longitude,
                            speedKmh = sample.speedKmh,
                            heading = sample.headingDegrees,
                            accuracyMeters = sample.accuracyMeters,
                            routeProgress = eval.routeProgress
                        )
                    }
                }
            }
        }
    }

    private fun startLiveMapTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(2000L)
                val state = _uiState.value
                val room = state.activeRoom
                if (state.isSimulationRunning && room != null && !room.isSyncedRoom) {
                    val orderedByProgress = room.members.sortedByDescending { it.mapPosition.routeProgress }
                    val lastMemberId = orderedByProgress.lastOrNull()?.id
                    val useRealSelfGps = state.gpsStatus == GpsTrackingStatus.ACTIVE && !state.isOffRouteSimActive

                    val movedMembers = room.members.mapIndexed { idx, member ->
                        if (member.isCurrentUser && useRealSelfGps) {
                            return@mapIndexed member
                        }
                        val isStopped = member.conditionStatus == MemberConditionStatus.MOGOK ||
                            member.conditionStatus == MemberConditionStatus.SOS
                        val isTail = member.id == lastMemberId && state.isStragglerSimActive
                        val step = when {
                            isStopped -> 0f
                            isTail -> 0.002f
                            else -> 0.010f + (idx % 2) * 0.002f
                        }
                        val nextProgress = (member.mapPosition.routeProgress + step).let {
                            if (it > 0.90f) 0.16f else it
                        }
                        val offRouteShift = if (member.isCurrentUser && state.isOffRouteSimActive) 0.12f else 0f
                        val nextPos = interpolateAlongPolyline(
                            progress = nextProgress,
                            waypoints = state.routeConfig.waypoints,
                            lateralOffset = offRouteShift,
                            speedKmh = when {
                                isStopped -> 0
                                isTail -> 28
                                else -> (48..65).random()
                            }
                        )
                        member.copy(
                            mapPosition = nextPos,
                            lastLocationText = landmarkForProgress(nextProgress),
                            lastUpdatedLabel = if (member.connectionStatus == ConnectionStatus.WEAK_SIGNAL) {
                                "5 dtk lalu"
                            } else {
                                "Baru saja"
                            }
                        )
                    }
                    recomputeAndCommitRoom(movedMembers)
                } else if (room != null && room.isSyncedRoom && !room.leaderTroubleActive) {
                    val nowMs = System.currentTimeMillis()
                    val leaderMember = room.members.firstOrNull {
                        (room.leaderUid.isNotBlank() && it.id == room.leaderUid) ||
                            it.role == UserRole.TOUR_LEADER
                    }
                    val staleLeader = leaderMember != null &&
                        leaderMember.lastUpdatedEpochMs > 0L &&
                        (nowMs - leaderMember.lastUpdatedEpochMs > 18_000L)
                    val leaderHasTrouble = leaderMember != null && (
                        leaderMember.connectionStatus == ConnectionStatus.OFFLINE ||
                            leaderMember.conditionStatus == MemberConditionStatus.MOGOK ||
                            leaderMember.conditionStatus == MemberConditionStatus.SOS ||
                            staleLeader
                        )
                    val targetCoLeader = room.coLeaderUid.ifBlank {
                        GeoUtils.selectClosestCoLeader(room.members, leaderMember?.id).coLeaderUid.orEmpty()
                    }
                    if (leaderHasTrouble && targetCoLeader.isNotBlank() && nowMs - lastFailoverAttemptMs > 8_000L) {
                        val currentUid = syncRepo.currentUid()
                        if (currentUid != null && currentUid != leaderMember?.id) {
                            lastFailoverAttemptMs = nowMs
                            val reason = when {
                                leaderMember?.conditionStatus == MemberConditionStatus.MOGOK ->
                                    "Kendaraan Leader mengalami mogok"
                                leaderMember?.conditionStatus == MemberConditionStatus.SOS ->
                                    "Leader mengirim sinyal darurat SOS"
                                else ->
                                    "HP Leader kehilangan sinyal / tidak aktif (>18 dtk)"
                            }
                            if (room.coLeaderUid != targetCoLeader) {
                                syncRepo.updateCoLeaderUid(room.roomId, targetCoLeader)
                            }
                            val ok = syncRepo.triggerLeaderFailover(room.roomId, targetCoLeader, reason)
                            if (ok) {
                                val promotedName = room.members.firstOrNull { it.id == targetCoLeader }?.fullName ?: "Co-Leader"
                                val sysMsg = RoomMessage(
                                    id = "failover-${UUID.randomUUID()}",
                                    senderId = "system",
                                    senderName = "Sistem Failover",
                                    senderVehicle = "",
                                    type = ChatMessageType.SYSTEM,
                                    textContent = "Leader gangguan ($reason). Co-Leader $promotedName otomatis mengambil alih sebagai Plt. Leader & TomTom API.",
                                    timestampLabel = currentClockLabel()
                                )
                                syncRepo.sendMessage(room.roomId, currentUid, sysMsg)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.ROOT)

    private fun saveProfileByEmail(profile: VehicleProfile) {
        val emailKey = normalizeEmail(profile.email)
        if (emailKey.isBlank()) return
        val json = JSONObject().apply {
            put("fullName", profile.fullName)
            put("email", emailKey)
            put("authProvider", profile.authProvider.id)
            put("clubName", profile.clubName)
            put("homeCity", profile.homeCity)
            put("bio", profile.bio)
            put("instagramHandle", profile.instagramHandle)
            put("vehicleType", profile.vehicleType.name)
            put("vehicleDetail", profile.vehicleDetail)
            put("licensePlate", profile.licensePlate)
            put("emergencyContact", profile.emergencyContact)
            put("avatarPreset", profile.avatarPreset.name)
            put("avatarPhotoUri", profile.avatarPhotoUri.orEmpty())
        }
        profilePrefs.edit().putString("profile_$emailKey", json.toString()).apply()
    }

    private fun loadProfileByEmail(email: String): VehicleProfile? {
        val emailKey = normalizeEmail(email)
        if (emailKey.isBlank()) return null
        val raw = profilePrefs.getString("profile_$emailKey", null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            VehicleProfile(
                fullName = json.optString("fullName"),
                email = json.optString("email", emailKey),
                authProvider = AuthProviderType.fromId(json.optString("authProvider")),
                clubName = json.optString("clubName"),
                homeCity = json.optString("homeCity"),
                bio = json.optString("bio"),
                instagramHandle = json.optString("instagramHandle"),
                vehicleType = runCatching {
                    VehicleType.valueOf(json.optString("vehicleType", "MOBIL"))
                }.getOrDefault(VehicleType.MOBIL),
                vehicleDetail = json.optString("vehicleDetail"),
                licensePlate = json.optString("licensePlate"),
                emergencyContact = json.optString("emergencyContact"),
                avatarPreset = runCatching {
                    AvatarPreset.valueOf(json.optString("avatarPreset", "HELMET_TOURING"))
                }.getOrDefault(AvatarPreset.HELMET_TOURING),
                avatarPhotoUri = json.optString("avatarPhotoUri").takeIf { it.isNotBlank() }
            )
        }.getOrNull()
    }

    private fun saveActiveSession(
        fullName: String,
        email: String,
        provider: AuthProviderType,
        isGuest: Boolean
    ) {
        val json = JSONObject().apply {
            put("fullName", fullName)
            put("email", normalizeEmail(email))
            put("authProvider", provider.id)
            put("isGuest", isGuest)
        }
        profilePrefs.edit().putString("active_session", json.toString()).apply()
    }

    private fun clearActiveSession() {
        profilePrefs.edit().remove("active_session").apply()
    }

    private fun detectProvider(user: FirebaseUser): AuthProviderType {
        val providerIds = user.providerData.map { it.providerId }
        return when {
            providerIds.any { it.contains("google", ignoreCase = true) } -> AuthProviderType.GOOGLE
            providerIds.any { it.contains("facebook", ignoreCase = true) } -> AuthProviderType.FACEBOOK
            providerIds.any { it.contains("password", ignoreCase = true) } -> AuthProviderType.EMAIL
            else -> _uiState.value.authProvider.takeIf { it != AuthProviderType.LOCAL }
                ?: AuthProviderType.GOOGLE
        }
    }

    private fun attachAuthStateObserver() {
        authUnsubscribe?.invoke()
        authUnsubscribe = syncRepo.observeAuthState { user ->
            val available = syncRepo.isAvailable()
            val verifiedAdmin = syncRepo.isVerifiedAdminSession()
            if (user != null && !user.isAnonymous && !syncRepo.isDeviceCloudGuestUser(user)) {
                val detectedProvider = detectProvider(user)
                viewModelScope.launch {
                    syncFirebaseUserProfile(user, detectedProvider)
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCloudSyncConnected = available,
                        isVerifiedAdmin = verifiedAdmin
                    )
                }
            }
        }
    }

    private suspend fun syncFirebaseUserProfile(
        user: FirebaseUser,
        fallbackProvider: AuthProviderType
    ) {
        val uid = user.uid
        val detectedProvider = detectProvider(user).let { detected ->
            if (detected == AuthProviderType.LOCAL) fallbackProvider else detected
        }
        val cleanEmail = normalizeEmail(user.email.orEmpty())
        val displayName = user.displayName.orEmpty().trim()
        val photoUrl = user.photoUrl?.toString()?.takeIf { it.isNotBlank() }

        val cloudProfile = syncRepo.loadUserPrivateProfile(uid)
        val localProfile = loadProfileByEmail(
            cloudProfile?.email?.ifBlank { cleanEmail } ?: cleanEmail
        )
        val baseProfile = when {
            cloudProfile != null -> mergeProfiles(cloudProfile, localProfile)
            localProfile != null -> localProfile
            else -> _uiState.value.vehicleProfile
        }
        val resolvedName = baseProfile.fullName.ifBlank { displayName }
            .ifBlank { cleanEmail.substringBefore("@") }
            .ifBlank { "Rider KonvoiKu" }
        val resolvedEmail = baseProfile.email.ifBlank { cleanEmail }
        val resolvedProvider = if (
            baseProfile.authProvider != AuthProviderType.LOCAL &&
            baseProfile.authProvider != AuthProviderType.EMAIL
        ) {
            baseProfile.authProvider
        } else {
            detectedProvider
        }
        val merged = baseProfile.copy(
            fullName = resolvedName,
            email = resolvedEmail,
            authProvider = resolvedProvider,
            avatarPhotoUri = baseProfile.avatarPhotoUri ?: photoUrl
        )

        saveProfileByEmail(merged)
        saveActiveSession(resolvedName, resolvedEmail, resolvedProvider, isGuest = false)
        if (cloudProfile == null) {
            syncRepo.saveUserPrivateProfile(uid, merged)
        }

        val isVerifiedAdmin = syncRepo.isVerifiedAdminSession()
        _uiState.update { state ->
            state.copy(
                isCloudSyncConnected = syncRepo.isAvailable(),
                isLoggedIn = true,
                authUserName = resolvedName,
                authUserEmail = resolvedEmail,
                authProvider = resolvedProvider,
                isGuestMode = false,
                isVerifiedAdmin = isVerifiedAdmin,
                authError = null,
                showGoogleBrowserFallback = false,
                showOwnerDevPreviewFallback = false,
                vehicleProfile = merged
            )
        }
        pushPresenceSnapshotNow()
        val cloudTrips = syncRepo.loadUserTrips(uid)
        mergeCloudTrips(cloudTrips)
    }

    private fun restoreSessionAndProfile() {
        val sessionRaw = profilePrefs.getString("active_session", null)
        val firebaseUser = syncRepo.currentUser()
        val firebaseEmail = normalizeEmail(syncRepo.currentUserEmail())
        val firebaseUid = syncRepo.currentUid()
        val isVerifiedAdmin = syncRepo.isVerifiedAdminSession()

        if (firebaseUser != null && !firebaseUser.isAnonymous && !syncRepo.isDeviceCloudGuestUser(firebaseUser) && firebaseEmail.isNotBlank()) {
            val provider = detectProvider(firebaseUser)
            val savedProfile = loadProfileByEmail(firebaseEmail)
            val displayName = firebaseUser.displayName.orEmpty().trim()
            val mergedProfile = (savedProfile ?: _uiState.value.vehicleProfile).copy(
                fullName = savedProfile?.fullName?.ifBlank { displayName } ?: displayName.ifBlank { firebaseEmail.substringBefore("@") },
                email = firebaseEmail,
                authProvider = provider,
                avatarPhotoUri = savedProfile?.avatarPhotoUri ?: firebaseUser.photoUrl?.toString()
            )
            saveActiveSession(mergedProfile.fullName, firebaseEmail, provider, isGuest = false)
            _uiState.update { state ->
                state.copy(
                    isLoggedIn = true,
                    authUserName = mergedProfile.fullName,
                    authUserEmail = firebaseEmail,
                    authProvider = provider,
                    isGuestMode = false,
                    isVerifiedAdmin = isVerifiedAdmin,
                    vehicleProfile = mergedProfile
                )
            }
        } else if (sessionRaw != null) {
            runCatching {
                val json = JSONObject(sessionRaw)
                val email = normalizeEmail(json.optString("email")).ifBlank { firebaseEmail }
                val name = json.optString("fullName").ifBlank { "Pengendara KonvoiKu" }
                val provider = AuthProviderType.fromId(json.optString("authProvider"))
                val isGuest = json.optBoolean("isGuest", false)
                val savedProfile = loadProfileByEmail(email)
                _uiState.update { state ->
                    val mergedProfile = (savedProfile ?: state.vehicleProfile).copy(
                        fullName = (savedProfile?.fullName?.ifBlank { name }) ?: name,
                        email = email,
                        authProvider = provider
                    )
                    state.copy(
                        isLoggedIn = true,
                        authUserName = mergedProfile.fullName.ifBlank { name },
                        authUserEmail = email,
                        authProvider = provider,
                        isGuestMode = isGuest,
                        isVerifiedAdmin = isVerifiedAdmin,
                        vehicleProfile = mergedProfile
                    )
                }
            }
        }

        if (firebaseUid != null && firebaseUser != null && !firebaseUser.isAnonymous && !syncRepo.isDeviceCloudGuestUser(firebaseUser)) {
            viewModelScope.launch {
                syncFirebaseUserProfile(firebaseUser, detectProvider(firebaseUser))
            }
        }
    }

    private fun mergeProfiles(primary: VehicleProfile, fallback: VehicleProfile?): VehicleProfile {
        if (fallback == null) return primary
        return primary.copy(
            fullName = primary.fullName.ifBlank { fallback.fullName },
            email = primary.email.ifBlank { fallback.email },
            authProvider = if (primary.authProvider != AuthProviderType.LOCAL) primary.authProvider else fallback.authProvider,
            clubName = primary.clubName.ifBlank { fallback.clubName },
            homeCity = primary.homeCity.ifBlank { fallback.homeCity },
            bio = primary.bio.ifBlank { fallback.bio },
            instagramHandle = primary.instagramHandle.ifBlank { fallback.instagramHandle },
            vehicleDetail = primary.vehicleDetail.ifBlank { fallback.vehicleDetail },
            licensePlate = primary.licensePlate.ifBlank { fallback.licensePlate },
            emergencyContact = primary.emergencyContact.ifBlank { fallback.emergencyContact },
            avatarPhotoUri = primary.avatarPhotoUri ?: fallback.avatarPhotoUri
        )
    }

    fun clearAuthError() {
        _uiState.update {
            it.copy(
                authError = null,
                authInfoMessage = null,
                showOwnerDevPreviewFallback = false
            )
        }
    }

    fun clearProfileSyncMessage() {
        _uiState.update { it.copy(profileSyncMessage = null) }
    }

    fun signInWithGoogle(activity: Activity?, onSuccess: () -> Unit = {}) {
        if (activity == null) {
            _uiState.update {
                it.copy(
                    authError = "Jendela aplikasi tidak siap. Gunakan tombol Sinkronkan via Browser di bawah.",
                    showGoogleBrowserFallback = true
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthLoading = true,
                    authError = null,
                    authInfoMessage = null,
                    showGoogleBrowserFallback = false
                )
            }
            val result = syncRepo.signInWithGoogleProvider(activity)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                syncFirebaseUserProfile(user, AuthProviderType.GOOGLE)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authInfoMessage = "Akun Google (${user.email.orEmpty()}) berhasil disinkronkan!",
                        showGoogleBrowserFallback = false
                    )
                }
                onSuccess()
            } else {
                val err = result.exceptionOrNull()
                val raw = err?.message.orEmpty().lowercase(Locale.ROOT)
                val cancelled = raw.contains("cancel") || raw.contains("closed")
                val reasonHint = err?.localizedMessage?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        showGoogleBrowserFallback = !cancelled,
                        authError = if (cancelled) {
                            "Login Google dibatalkan. Silakan coba lagi."
                        } else {
                            "Login Google langsung di APK membutuhkan registrasi SHA-1 di Firebase$reasonHint. Gunakan tombol 'Sinkronkan Google via Browser' atau Email/Kata Sandi."
                        }
                    )
                }
            }
        }
    }

    fun completeGoogleIdTokenSignIn(idToken: String, onSuccess: () -> Unit = {}) {
        val cleanToken = idToken.trim()
        if (cleanToken.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthLoading = true,
                    authError = null,
                    authInfoMessage = null,
                    showGoogleBrowserFallback = false
                )
            }
            val result = syncRepo.signInWithGoogleIdToken(cleanToken)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                syncFirebaseUserProfile(user, AuthProviderType.GOOGLE)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authInfoMessage = "Sinkronisasi Google (${user.email.orEmpty()}) berhasil!",
                        showGoogleBrowserFallback = false
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        showGoogleBrowserFallback = true,
                        authError = mapAuthError(result.exceptionOrNull())
                    )
                }
            }
        }
    }

    fun openGoogleBrowserSync(
        context: Context,
        customBaseUrl: String? = null,
        allowDevProxyFallback: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    authError = null,
                    showOwnerDevPreviewFallback = false,
                    authInfoMessage = "Menyiapkan halaman login Google di browser..."
                )
            }
            val savedCustomUrl = profilePrefs.getString("custom_web_base_url", null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
            val explicitCustom = customBaseUrl?.trim()?.takeIf { it.isNotEmpty() }

            var targetUrl: String? = null
            var isDevProxy = false
            var isSharedUnpublished = false
            var isLocalLoopback = false
            var resolvedBase = ""

            if (explicitCustom != null || savedCustomUrl != null || allowDevProxyFallback) {
                val resolution = syncRepo.resolveWebBridgeUrl(
                    customBaseUrl = explicitCustom ?: savedCustomUrl,
                    allowDevProxyFallback = allowDevProxyFallback
                )
                if (resolution.baseUrl.isNotBlank()) {
                    resolvedBase = resolution.baseUrl
                    isDevProxy = resolution.isDevProxy
                    isSharedUnpublished = resolution.isSharedUnpublished
                    targetUrl = "${resolution.baseUrl}/?mobile_auth=google"
                    if (explicitCustom != null) {
                        profilePrefs.edit().putString("custom_web_base_url", resolution.baseUrl).apply()
                    }
                    _uiState.update { it.copy(activeWebBaseUrl = resolution.baseUrl) }
                } else if (explicitCustom != null) {
                    _uiState.update {
                        it.copy(
                            authInfoMessage = null,
                            showGoogleBrowserFallback = true,
                            showOwnerDevPreviewFallback = resolution.isDevProxy && !allowDevProxyFallback,
                            authError = resolution.errorMessage
                                ?: "URL web kustom tidak dapat digunakan untuk login Google."
                        )
                    }
                    return@launch
                }
            }

            if (targetUrl == null) {
                val portResult = withContext(Dispatchers.IO) {
                    runCatching {
                        localBrowserAuthServer.start(
                            onIdTokenReceived = { idToken ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    completeGoogleIdTokenSignIn(idToken)
                                }
                            },
                            onErrorReceived = { errMsg ->
                                viewModelScope.launch(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            isAuthLoading = false,
                                            showGoogleBrowserFallback = true,
                                            authError = errMsg
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
                val port = portResult.getOrNull()
                if (port == null) {
                    _uiState.update {
                        it.copy(
                            authInfoMessage = null,
                            showGoogleBrowserFallback = true,
                            authError = "Gagal menyiapkan server login browser lokal di perangkat."
                        )
                    }
                    return@launch
                }
                isLocalLoopback = true
                resolvedBase = "http://localhost:$port"
                targetUrl = "$resolvedBase/auth"
            }

            val finalUrl = targetUrl
            runCatching {
                withContext(Dispatchers.Main) {
                    val targetUri = Uri.parse(finalUrl)
                    val baseIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        if (context !is Activity) {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    val browserProbe = Intent(Intent.ACTION_VIEW, Uri.parse("https://accounts.google.com")).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                    }
                    @Suppress("DEPRECATION")
                    val externalBrowsers = runCatching {
                        context.packageManager.queryIntentActivities(
                            browserProbe,
                            PackageManager.MATCH_DEFAULT_ONLY
                        ).mapNotNull { info ->
                            info.activityInfo?.packageName?.takeIf { it != context.packageName }
                        }.distinct()
                    }.getOrDefault(emptyList())

                    val preferredPackage = externalBrowsers.firstOrNull { pkg ->
                        pkg.contains("chrome", ignoreCase = true)
                    } ?: externalBrowsers.firstOrNull()

                    if (preferredPackage != null) {
                        baseIntent.setPackage(preferredPackage)
                    }
                    context.startActivity(baseIntent)
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        showGoogleBrowserFallback = true,
                        showOwnerDevPreviewFallback = false,
                        authInfoMessage = when {
                            isLocalLoopback ->
                                "Browser dibuka ke halaman login Google ($resolvedBase). Ketuk 'Pilih Akun Google' di browser, lalu tunggu otomatis kembali ke KonvoiKu."
                            isDevProxy && isSharedUnpublished ->
                                "Dev Preview ($resolvedBase) dibuka khusus pemilik. Pastikan Chrome memakai akun Google pemilik (${FirestoreConvoyRepository.ADMIN_EMAIL}), lalu salin link/tokennya setelah login."
                            isDevProxy ->
                                "Browser dibuka ke Dev Preview ($resolvedBase). Pastikan Chrome memakai akun Google pemilik AI Studio, lalu selesaikan login & ketuk 'Buka Aplikasi KonvoiKu' atau salin link/tokennya."
                            else ->
                                "Browser dibuka ($resolvedBase). Setelah login Google selesai, ketuk 'Buka Aplikasi KonvoiKu' atau salin link/tokennya ke kolom di bawah."
                        }
                    )
                }
            }.onFailure {
                if (isLocalLoopback) {
                    localBrowserAuthServer.stop()
                }
                _uiState.update {
                    it.copy(
                        authInfoMessage = null,
                        authError = "Tidak dapat membuka browser untuk sinkronisasi Google."
                    )
                }
            }
        }
    }

    fun importGoogleSyncInput(rawInput: String, onSuccess: () -> Unit = {}) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            _uiState.update {
                it.copy(authError = "Tempel link konvoiku://auth atau kode id_token dari browser terlebih dahulu.")
            }
            return
        }
        val extractedToken = if (trimmed.startsWith("konvoiku://", ignoreCase = true) || trimmed.contains("id_token=")) {
            val uriString = if (trimmed.contains("://")) trimmed else "konvoiku://auth?$trimmed"
            Uri.parse(uriString).getQueryParameter("id_token")?.trim().orEmpty()
        } else {
            trimmed
        }
        if (extractedToken.isBlank()) {
            _uiState.update {
                it.copy(authError = "Token Google tidak ditemukan pada teks yang ditempel. Pastikan menyalin seluruh link sinkronisasi.")
            }
            return
        }
        completeGoogleIdTokenSignIn(extractedToken, onSuccess)
    }

    fun handleIncomingIntent(intent: Intent?) {
        val dataUri = intent?.data ?: return
        if (dataUri.scheme == "konvoiku" && dataUri.host == "auth") {
            val idToken = dataUri.getQueryParameter("id_token")?.trim().orEmpty()
            val errorMsg = dataUri.getQueryParameter("error")?.trim()
            if (idToken.isNotBlank()) {
                completeGoogleIdTokenSignIn(idToken)
            } else if (!errorMsg.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = errorMsg
                    )
                }
            }
        } else {
            val roomCode = extractRoomId(dataUri.toString())
            val isRoomInvite = roomCode.length >= 4 && (
                (dataUri.scheme == "konvoiku" && dataUri.host == "join") ||
                    (dataUri.scheme == "https" && (
                        dataUri.host == "konvoiku.id" ||
                            dataUri.host?.endsWith(".run.app") == true ||
                            !dataUri.getQueryParameter("room").isNullOrBlank()
                        ))
                )
            if (isRoomInvite) {
                _uiState.update {
                    it.copy(
                        joinInput = roomCode,
                        joinError = null,
                        pendingInviteRoomId = roomCode,
                        selectedRole = UserRole.MEMBER
                    )
                }
            }
        }
    }

    fun consumePendingInviteNavigation(): String? {
        val pending = _uiState.value.pendingInviteRoomId ?: return null
        _uiState.update { it.copy(pendingInviteRoomId = null) }
        return pending
    }

    fun clearRoomSetupError() {
        _uiState.update { it.copy(roomSetupError = null) }
    }

    fun buildWebRoomShareLink(roomId: String): String {
        val base = _uiState.value.activeWebBaseUrl.trimEnd('/')
        return "$base/?room=${Uri.encode(roomId.trim().uppercase(Locale.ROOT))}"
    }

    fun signInWithFacebook(activity: Activity?, onSuccess: () -> Unit = {}) {
        if (activity == null) {
            _uiState.update { it.copy(authError = "Tidak dapat membuka login Facebook.") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAuthLoading = true,
                    authError = null,
                    authInfoMessage = null
                )
            }
            val result = syncRepo.signInWithFacebookProvider(activity)
            if (result.isSuccess) {
                val user = result.getOrThrow()
                syncFirebaseUserProfile(user, AuthProviderType.FACEBOOK)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authInfoMessage = "Akun Facebook berhasil disinkronkan!"
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = mapAuthError(result.exceptionOrNull())
                    )
                }
            }
        }
    }

    fun sendPasswordReset(email: String) {
        val cleanEmail = normalizeEmail(email)
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _uiState.update {
                it.copy(authError = "Masukkan alamat email terlebih dahulu untuk reset kata sandi.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null, authInfoMessage = null) }
            val result = syncRepo.sendPasswordReset(cleanEmail)
            _uiState.update {
                if (result.isSuccess) {
                    it.copy(
                        isAuthLoading = false,
                        authInfoMessage = "Email reset kata sandi telah dikirim ke $cleanEmail."
                    )
                } else {
                    it.copy(
                        isAuthLoading = false,
                        authError = mapAuthError(result.exceptionOrNull())
                    )
                }
            }
        }
    }

    fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanName = fullName.trim()
        val cleanEmail = normalizeEmail(email)
        if (cleanName.length < 2) {
            _uiState.update { it.copy(authError = "Nama lengkap minimal 2 karakter.") }
            return
        }
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _uiState.update { it.copy(authError = "Format email tidak valid.") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(authError = "Kata sandi minimal 6 karakter.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            val result = syncRepo.signUpWithEmail(cleanName, cleanEmail, password)
            if (result.isSuccess) {
                val uid = result.getOrThrow()
                val existing = loadProfileByEmail(cleanEmail)
                val merged = (existing ?: _uiState.value.vehicleProfile).copy(
                    fullName = cleanName,
                    email = cleanEmail,
                    authProvider = AuthProviderType.EMAIL
                )
                saveProfileByEmail(merged)
                saveActiveSession(cleanName, cleanEmail, AuthProviderType.EMAIL, isGuest = false)
                syncRepo.saveUserPrivateProfile(uid, merged)
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = true,
                        authUserName = cleanName,
                        authUserEmail = cleanEmail,
                        authProvider = AuthProviderType.EMAIL,
                        isGuestMode = false,
                        isAuthLoading = false,
                        authError = null,
                        vehicleProfile = merged
                    )
                }
                pushPresenceSnapshotNow()
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = mapAuthError(result.exceptionOrNull())
                    )
                }
            }
        }
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: () -> Unit = {}
    ) {
        val cleanEmail = normalizeEmail(email)
        if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
            _uiState.update { it.copy(authError = "Format email tidak valid.") }
            return
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(authError = "Masukkan kata sandi akun Anda.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            val result = syncRepo.signInWithEmail(cleanEmail, password)
            if (result.isSuccess) {
                val uid = result.getOrThrow()
                val cloudProfile = syncRepo.loadUserPrivateProfile(uid)
                val localProfile = loadProfileByEmail(cleanEmail)
                val baseProfile = when {
                    cloudProfile != null -> mergeProfiles(cloudProfile, localProfile)
                    localProfile != null -> localProfile
                    else -> _uiState.value.vehicleProfile
                }
                val resolvedName = baseProfile.fullName.ifBlank { cleanEmail.substringBefore("@") }
                val syncedProfile = baseProfile.copy(
                    fullName = resolvedName,
                    email = cleanEmail,
                    authProvider = AuthProviderType.EMAIL
                )
                saveProfileByEmail(syncedProfile)
                saveActiveSession(resolvedName, cleanEmail, AuthProviderType.EMAIL, isGuest = false)
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = true,
                        authUserName = resolvedName,
                        authUserEmail = cleanEmail,
                        authProvider = AuthProviderType.EMAIL,
                        isGuestMode = false,
                        isAuthLoading = false,
                        authError = null,
                        vehicleProfile = syncedProfile
                    )
                }
                pushPresenceSnapshotNow()
                val cloudTrips = syncRepo.loadUserTrips(uid)
                mergeCloudTrips(cloudTrips)
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = mapAuthError(result.exceptionOrNull())
                    )
                }
            }
        }
    }

    fun loginWithSocialProvider(
        provider: AuthProviderType,
        fullName: String,
        email: String,
        instagramHandle: String = "",
        onSuccess: () -> Unit = {}
    ) {
        val cleanHandle = instagramHandle.trim().removePrefix("@")
        val resolvedEmail = normalizeEmail(email).ifBlank {
            if (provider == AuthProviderType.INSTAGRAM && cleanHandle.isNotBlank()) {
                "${cleanHandle.lowercase(Locale.ROOT)}@instagram.konvoiku.id"
            } else {
                "pengendara.${provider.id}@konvoiku.id"
            }
        }
        val existing = loadProfileByEmail(resolvedEmail)
        val resolvedName = fullName.trim().ifBlank {
            existing?.fullName?.ifBlank {
                if (cleanHandle.isNotBlank()) "@$cleanHandle" else "Rider ${provider.label}"
            } ?: "Rider ${provider.label}"
        }
        val merged = (existing ?: _uiState.value.vehicleProfile).copy(
            fullName = resolvedName,
            email = resolvedEmail,
            authProvider = provider,
            instagramHandle = cleanHandle.ifBlank { existing?.instagramHandle.orEmpty() }
        )
        saveProfileByEmail(merged)
        saveActiveSession(resolvedName, resolvedEmail, provider, isGuest = false)

        _uiState.update { state ->
            state.copy(
                isLoggedIn = true,
                authUserName = resolvedName,
                authUserEmail = resolvedEmail,
                authProvider = provider,
                isGuestMode = false,
                authError = null,
                vehicleProfile = merged
            )
        }

        if (syncRepo.isAvailable()) {
            viewModelScope.launch {
                val uid = syncRepo.ensureSignedInUid()
                if (uid != null) {
                    syncRepo.saveUserPrivateProfile(uid, merged)
                    pushPresenceSnapshotNow()
                }
            }
        }
        onSuccess()
    }

    fun saveAndSyncCustomizedProfile(profile: VehicleProfile) {
        val cleanEmail = normalizeEmail(profile.email).ifBlank {
            normalizeEmail(_uiState.value.authUserEmail)
        }
        val cleanName = profile.fullName.trim().ifBlank { "Pengendara KonvoiKu" }
        val resolvedProvider = if (profile.authProvider == AuthProviderType.LOCAL) {
            _uiState.value.authProvider
        } else {
            profile.authProvider
        }
        val cleaned = profile.copy(
            fullName = cleanName,
            email = cleanEmail,
            authProvider = resolvedProvider,
            licensePlate = profile.licensePlate.trim().uppercase()
        )
        saveProfileByEmail(cleaned)
        saveActiveSession(cleanName, cleanEmail, resolvedProvider, _uiState.value.isGuestMode)

        _uiState.update { state ->
            val updatedRoom = state.activeRoom?.let { room ->
                room.copy(
                    members = room.members.map { m ->
                        if (m.isCurrentUser) {
                            m.copy(
                                fullName = cleanName,
                                vehicleType = cleaned.vehicleType,
                                vehicleDetail = cleaned.displayVehicleLabel,
                                licensePlate = cleaned.licensePlate.ifBlank { m.licensePlate },
                                emergencyContact = cleaned.emergencyContact.ifBlank { m.emergencyContact },
                                avatarPreset = cleaned.avatarPreset,
                                avatarPhotoUri = cleaned.avatarPhotoUri
                            )
                        } else m
                    }
                )
            }
            state.copy(
                isSavingProfile = syncRepo.isAvailable(),
                authUserName = cleanName,
                authUserEmail = cleanEmail,
                authProvider = resolvedProvider,
                vehicleProfile = cleaned,
                activeRoom = updatedRoom,
                profileSyncMessage = if (!syncRepo.isAvailable()) {
                    "Profil tersimpan & disinkronkan ke $cleanEmail (penyimpanan lokal)."
                } else null
            )
        }

        if (syncRepo.isAvailable()) {
            viewModelScope.launch {
                val uid = syncRepo.ensureSignedInUid()
                val syncedCloud = if (uid != null) {
                    syncRepo.saveUserPrivateProfile(uid, cleaned)
                } else false
                val targetLabel = cleanEmail.ifBlank { "akun Anda" }
                _uiState.update {
                    it.copy(
                        isSavingProfile = false,
                        profileSyncMessage = if (syncedCloud) {
                            "Profil berhasil disimpan & disinkronkan ke cloud ($targetLabel)."
                        } else {
                            "Profil tersimpan untuk $targetLabel (perangkat)."
                        }
                    )
                }
            }
        }
    }

    private fun mapAuthError(err: Throwable?): String {
        val msg = err?.message.orEmpty().lowercase(Locale.ROOT)
        return when {
            syncRepo.isOperationNotAllowedError(err) ->
                "Metode Email/Tamu Cloud belum diaktifkan di Firebase Console (Authentication → Sign-in method). Saat ini hanya Google Sign-In yang aktif."
            msg.contains("already in use") || msg.contains("email_exists") ->
                "Email sudah terdaftar. Silakan pindah ke tab Masuk."
            msg.contains("wrong password") || msg.contains("invalid_login_credentials") || msg.contains("invalid credential") ->
                "Email atau kata sandi tidak sesuai."
            msg.contains("user not found") || msg.contains("no user record") ->
                "Akun email belum terdaftar. Silakan pilih Daftar terlebih dahulu."
            msg.contains("weak-password") || msg.contains("at least 6") ->
                "Kata sandi terlalu lemah (minimal 6 karakter)."
            msg.contains("network") ->
                "Koneksi internet bermasalah. Periksa jaringan Anda."
            else -> err?.message ?: "Autentikasi gagal. Periksa kembali data Anda."
        }
    }

    fun loginUser(fullName: String, email: String, isGuest: Boolean = false) {
        val cleanName = fullName.trim().ifBlank { "Pengendara KonvoiKu" }
        val cleanEmail = normalizeEmail(email)
        val savedProfile = loadProfileByEmail(cleanEmail)
        val merged = (savedProfile ?: _uiState.value.vehicleProfile).copy(
            fullName = savedProfile?.fullName?.ifBlank { cleanName } ?: cleanName,
            email = cleanEmail,
            authProvider = AuthProviderType.LOCAL
        )
        saveProfileByEmail(merged)
        saveActiveSession(merged.fullName, cleanEmail, AuthProviderType.LOCAL, isGuest)
        _uiState.update { state ->
            state.copy(
                isLoggedIn = true,
                authUserName = merged.fullName,
                authUserEmail = cleanEmail,
                authProvider = AuthProviderType.LOCAL,
                isGuestMode = isGuest,
                authError = null,
                vehicleProfile = merged
            )
        }
        if (syncRepo.isAvailable()) {
            viewModelScope.launch {
                val uid = syncRepo.ensureSignedInUid()
                if (uid != null) {
                    syncRepo.saveUserPrivateProfile(uid, merged)
                    pushPresenceSnapshotNow()
                }
            }
        }
    }

    fun logoutUser() {
        roomUnsubscribe?.invoke()
        roomUnsubscribe = null
        recordedTrackPoints.clear()
        lastComputedRouteSig = ""
        voiceTimerJob?.cancel()
        releaseVoiceRecorder(deleteFile = true)
        stopVoicePlayback()
        stopAdminMonitoring()
        syncRepo.signOutAuth()
        clearActiveSession()
        _uiState.update { state ->
            state.copy(
                isLoggedIn = false,
                authUserName = "",
                authUserEmail = "",
                authProvider = AuthProviderType.LOCAL,
                isGuestMode = false,
                isVerifiedAdmin = false,
                authError = null,
                authInfoMessage = null,
                showGoogleBrowserFallback = false,
                profileSyncMessage = null,
                adminPresenceList = emptyList(),
                adminRooms = emptyList(),
                adminUsers = emptyList(),
                isAdminMonitoringLoading = false,
                adminMonitorError = null,
                activeRoom = null,
                selectedRole = null,
                lastCompletedTrip = null,
                sosState = SosEmergencyState(),
                messages = emptyList()
            )
        }
    }

    fun selectRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun updateVehicleProfile(
        fullName: String? = null,
        vehicleType: VehicleType? = null,
        vehicleDetail: String? = null,
        licensePlate: String? = null,
        emergencyContact: String? = null,
        avatarPreset: AvatarPreset? = null,
        avatarPhotoUri: String? = null,
        clearPhoto: Boolean = false
    ) {
        _uiState.update { state ->
            val current = state.vehicleProfile
            state.copy(
                vehicleProfile = current.copy(
                    fullName = fullName ?: current.fullName,
                    vehicleType = vehicleType ?: current.vehicleType,
                    vehicleDetail = vehicleDetail ?: current.vehicleDetail,
                    licensePlate = (licensePlate ?: current.licensePlate).uppercase(),
                    emergencyContact = emergencyContact ?: current.emergencyContact,
                    avatarPreset = avatarPreset ?: current.avatarPreset,
                    avatarPhotoUri = if (clearPhoto) null else (avatarPhotoUri ?: current.avatarPhotoUri)
                )
            )
        }
    }

    fun createNewTourRoom(
        customTitle: String = "Konvoi Touring Akhir Pekan",
        onSuccess: () -> Unit = {}
    ) {
        val state = _uiState.value
        if (!state.globalConfig.allowNewRooms && !syncRepo.isVerifiedAdminSession()) {
            val msg = "Pembuatan room baru sedang dijeda sementara oleh Admin Backend."
            _uiState.update {
                it.copy(authError = msg, roomSetupError = msg)
            }
            return
        }
        val profile = state.vehicleProfile
        val routeConfig = state.routeConfig
        val roomCode = generateUniqueRoomId()
        val mockLink = "https://konvoiku.id/join/$roomCode"
        val firstWp = routeConfig.waypoints.firstOrNull()
        val startLat = if (routeConfig.originLockedByLeader && firstWp != null) {
            firstWp.latitude
        } else {
            state.userGpsLat ?: firstWp?.latitude ?: -6.648
        }
        val startLng = if (routeConfig.originLockedByLeader && firstWp != null) {
            firstWp.longitude
        } else {
            state.userGpsLng ?: firstWp?.longitude ?: 106.845
        }
        lastComputedRouteSig = ""
        seedRecordedTrackFromStart(startLat, startLng)

        if (syncRepo.isAvailable()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isCreatingRoom = true, roomSetupError = null) }
                val authResult = syncRepo.ensureSignedInUidResult()
                val uid = authResult.getOrNull()
                if (uid == null) {
                    val authErr = authResult.exceptionOrNull()
                    val reason = authErr?.localizedMessage
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " ($it)" }
                        .orEmpty()
                    val friendlyError = if (syncRepo.isOperationNotAllowedError(authErr)) {
                        "Mode Tamu Cloud belum diaktifkan di Firebase Console. Masuk Google di halaman Login untuk Room Cloud Live, aktifkan Anonymous/Email Auth di Firebase Console, atau ketuk 'Atau Buat Room Lokal (Offline)' di bawah."
                    } else {
                        "Autentikasi cloud belum terhubung$reason. Pastikan internet aktif atau ketuk 'Atau Buat Room Lokal (Offline)' di bawah."
                    }
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
                            roomSetupError = friendlyError
                        )
                    }
                    return@launch
                }
                syncRepo.saveUserPrivateProfile(uid, profile)
                val createResult = syncRepo.createSyncedRoomResult(
                    roomId = roomCode,
                    roomTitle = customTitle,
                    leaderUid = uid,
                    profile = profile,
                    routeConfig = routeConfig,
                    initialLat = startLat,
                    initialLng = startLng
                )
                if (createResult.isSuccess) {
                    val syncedRoom = TourRoom(
                        roomId = roomCode,
                        roomTitle = customTitle,
                        inviteLink = mockLink,
                        createdAtLabel = "Sinkronisasi Cloud",
                        members = emptyList(),
                        isSyncedRoom = true,
                        leaderUid = uid,
                        actingLeaderUid = uid
                    )
                    _uiState.update {
                        it.copy(
                            activeRoom = syncedRoom,
                            selectedRole = UserRole.TOUR_LEADER,
                            selectedMemberId = uid,
                            lastCompletedTrip = null,
                            isCloudSyncConnected = true,
                            isCreatingRoom = false,
                            roomSetupError = null
                        )
                    }
                    attachRoomSubscription(roomCode, uid)
                    pushPresenceSnapshotNow()
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val reason = createResult.exceptionOrNull()?.localizedMessage
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " ($it)" }
                        .orEmpty()
                    _uiState.update {
                        it.copy(
                            isCreatingRoom = false,
                            roomSetupError = "Gagal membuat room di server cloud$reason. Periksa koneksi internet Anda lalu coba lagi."
                        )
                    }
                }
            }
        } else {
            createLocalDemoRoom(roomCode, customTitle, mockLink, startLat, startLng)
            pushPresenceSnapshotNow()
            onSuccess()
        }
    }

    fun createLocalTourRoom(
        customTitle: String = "Konvoi Touring Akhir Pekan",
        onSuccess: () -> Unit = {}
    ) {
        val state = _uiState.value
        val routeConfig = state.routeConfig
        val roomCode = generateUniqueRoomId()
        val mockLink = "https://konvoiku.id/join/$roomCode"
        val firstWp = routeConfig.waypoints.firstOrNull()
        val startLat = if (routeConfig.originLockedByLeader && firstWp != null) {
            firstWp.latitude
        } else {
            state.userGpsLat ?: firstWp?.latitude ?: -6.648
        }
        val startLng = if (routeConfig.originLockedByLeader && firstWp != null) {
            firstWp.longitude
        } else {
            state.userGpsLng ?: firstWp?.longitude ?: 106.845
        }
        lastComputedRouteSig = ""
        seedRecordedTrackFromStart(startLat, startLng)
        _uiState.update { it.copy(isCreatingRoom = false, roomSetupError = null) }
        createLocalDemoRoom(roomCode, customTitle, mockLink, startLat, startLng)
        onSuccess()
    }

    private fun createLocalDemoRoom(
        roomCode: String,
        customTitle: String,
        mockLink: String,
        startLat: Double,
        startLng: Double
    ) {
        val profile = _uiState.value.vehicleProfile
        val routeConfig = _uiState.value.routeConfig
        val hasGps = _uiState.value.gpsStatus == GpsTrackingStatus.ACTIVE
        val leaderMember = ConvoyMember(
            id = "self-${UUID.randomUUID()}",
            fullName = profile.fullName.trim().ifBlank { "Anda (Leader)" },
            vehicleType = profile.vehicleType,
            vehicleDetail = profile.displayVehicleLabel,
            licensePlate = profile.licensePlate.ifBlank { "B 1001 LDR" },
            emergencyContact = profile.emergencyContact.ifBlank { "0812-0000-0001" },
            role = UserRole.TOUR_LEADER,
            joinedAtLabel = "Baru saja",
            isCurrentUser = true,
            isSweeper = false,
            avatarPreset = profile.avatarPreset,
            avatarPhotoUri = profile.avatarPhotoUri,
            conditionStatus = MemberConditionStatus.AMAN,
            connectionStatus = ConnectionStatus.CONNECTED,
            lastLocationText = if (hasGps) "GPS Live" else "Puncak Pass KM 49",
            lastUpdatedLabel = "Baru saja",
            mapPosition = if (hasGps) {
                MapPosition(0.2f, 0.25f, 0.75f, startLat, startLng, 0)
            } else {
                interpolateAlongPolyline(0.74f, routeConfig.waypoints, 0f, 60)
            }
        )

        val simulatedMembers = defaultConvoyTemplates.take(3).mapIndexed { index, tpl ->
            ConvoyMember(
                id = "sim-$index-${UUID.randomUUID()}",
                fullName = tpl.name,
                vehicleType = tpl.vehicleType,
                vehicleDetail = tpl.vehicleDetail,
                licensePlate = tpl.plate,
                emergencyContact = tpl.phone,
                role = UserRole.MEMBER,
                joinedAtLabel = "Aktif di room",
                isCurrentUser = false,
                isSweeper = index == 1,
                avatarPreset = tpl.avatarPreset,
                conditionStatus = tpl.condition,
                connectionStatus = tpl.status,
                lastLocationText = tpl.locationText,
                lastUpdatedLabel = if (tpl.status == ConnectionStatus.WEAK_SIGNAL) "4 dtk lalu" else "Baru saja",
                mapPosition = interpolateAlongPolyline(tpl.baseProgress, routeConfig.waypoints, 0f, tpl.baseSpeed)
            )
        }

        val allMembers = listOf(leaderMember) + simulatedMembers
        extraMemberCounter = 0
        lastSyncedCoLeaderUid = ""
        _uiState.update {
            it.copy(
                activeRoom = TourRoom(
                    roomId = roomCode,
                    roomTitle = customTitle,
                    inviteLink = mockLink,
                    createdAtLabel = "Aktif sekarang",
                    members = allMembers,
                    isSyncedRoom = false,
                    leaderUid = leaderMember.id,
                    actingLeaderUid = leaderMember.id
                ),
                selectedMemberId = leaderMember.id,
                lastCompletedTrip = null,
                sosState = SosEmergencyState(),
                messages = initialRoomMessages(roomCode),
                isOffRouteSimActive = false,
                isStragglerSimActive = false,
                isLeaderTroubleSimActive = false
            )
        }
        recomputeAndCommitRoom(allMembers)
    }

    private fun attachRoomSubscription(roomId: String, uid: String) {
        roomUnsubscribe?.invoke()
        lastSyncedCoLeaderUid = ""
        roomUnsubscribe = syncRepo.subscribeToRoom(
            roomId = roomId,
            currentUid = uid,
            onRoomHeader = { header ->
                if (header.status == "FINISHED") {
                    if (_uiState.value.activeRoom?.roomId == header.roomId) {
                        finishAndSaveCurrentTrip(autoEndedByLeader = true)
                    }
                    return@subscribeToRoom
                }
                _uiState.update { state ->
                    val mergedConfig = header.routeConfig.copy(checkpoints = state.routeConfig.checkpoints)
                    val curRoom = state.activeRoom
                    state.copy(
                        routeConfig = mergedConfig,
                        sosState = header.sosState,
                        activeRoom = curRoom?.copy(
                            roomTitle = header.roomTitle,
                            leaderUid = header.leaderUid,
                            coLeaderUid = header.coLeaderUid,
                            actingLeaderUid = header.actingLeaderUid,
                            leaderTroubleActive = header.leaderTroubleActive,
                            leaderTroubleReason = header.leaderTroubleReason,
                            isSyncedRoom = true
                        ) ?: TourRoom(
                            roomId = header.roomId,
                            roomTitle = header.roomTitle,
                            inviteLink = "https://konvoiku.id/join/${header.roomId}",
                            createdAtLabel = "Cloud Live",
                            members = emptyList(),
                            isSyncedRoom = true,
                            leaderUid = header.leaderUid,
                            coLeaderUid = header.coLeaderUid,
                            actingLeaderUid = header.actingLeaderUid,
                            leaderTroubleActive = header.leaderTroubleActive,
                            leaderTroubleReason = header.leaderTroubleReason
                        )
                    )
                }
                _uiState.value.activeRoom?.members?.let { existing ->
                    if (existing.isNotEmpty()) recomputeAndCommitRoom(existing)
                }
            },
            onMembers = { members ->
                recomputeAndCommitRoom(members)
            },
            onCheckpoints = { cps ->
                _uiState.update { state ->
                    state.copy(routeConfig = state.routeConfig.copy(checkpoints = cps))
                }
            },
            onMessages = { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        )
    }

    fun simulateMemberJoin() {
        val room = _uiState.value.activeRoom ?: return
        if (room.isSyncedRoom) return
        val waypoints = _uiState.value.routeConfig.waypoints
        val tpl = defaultConvoyTemplates[extraMemberCounter % defaultConvoyTemplates.size]
        extraMemberCounter++

        val newMember = ConvoyMember(
            id = UUID.randomUUID().toString(),
            fullName = "${tpl.name} #${extraMemberCounter + 1}",
            vehicleType = tpl.vehicleType,
            vehicleDetail = tpl.vehicleDetail,
            licensePlate = tpl.plate,
            emergencyContact = tpl.phone,
            role = UserRole.MEMBER,
            joinedAtLabel = "Baru bergabung",
            isCurrentUser = false,
            avatarPreset = tpl.avatarPreset,
            conditionStatus = MemberConditionStatus.AMAN,
            connectionStatus = ConnectionStatus.CONNECTED,
            lastLocationText = tpl.locationText,
            lastUpdatedLabel = "Baru saja",
            mapPosition = interpolateAlongPolyline(0.50f, waypoints, 0f, tpl.baseSpeed)
        )

        recomputeAndCommitRoom(room.members + newMember)
    }

    fun removeMember(memberId: String) {
        val room = _uiState.value.activeRoom ?: return
        recomputeAndCommitRoom(room.members.filterNot { it.id == memberId && !it.isCurrentUser })
    }

    fun updateJoinInput(value: String) {
        _uiState.update { it.copy(joinInput = value, joinError = null) }
    }

    fun extractRoomId(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return ""
        Regex("[?&]room=([A-Za-z0-9_-]{4,32})", RegexOption.IGNORE_CASE)
            .find(trimmed)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it.uppercase(Locale.ROOT) }
        Regex("/join/([A-Za-z0-9_-]{4,32})", RegexOption.IGNORE_CASE)
            .find(trimmed)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it.uppercase(Locale.ROOT) }
        val candidate = trimmed.substringAfterLast("/", missingDelimiterValue = trimmed)
            .substringBefore("?")
            .substringBefore("#")
            .trim()
            .uppercase(Locale.ROOT)
        return if (Regex("^[A-Z0-9_-]{4,32}$").matches(candidate)) candidate else ""
    }

    fun joinConvoyRoom(onSuccess: () -> Unit = {}): Boolean {
        val extractedId = extractRoomId(_uiState.value.joinInput)
        if (extractedId.length < 4) {
            _uiState.update {
                it.copy(joinError = "Masukkan Room ID yang valid (contoh: KNV-8492) atau tempel link undangan.")
            }
            return false
        }

        if (syncRepo.isAvailable()) {
            val state = _uiState.value
            val startLat = state.userGpsLat ?: -6.65
            val startLng = state.userGpsLng ?: 106.85
            viewModelScope.launch {
                _uiState.update { it.copy(isJoiningRoom = true, joinError = null) }
                val authResult = syncRepo.ensureSignedInUidResult()
                val uid = authResult.getOrNull()
                if (uid == null) {
                    val authErr = authResult.exceptionOrNull()
                    val reason = authErr?.localizedMessage
                        ?.takeIf { it.isNotBlank() }
                        ?.let { " ($it)" }
                        .orEmpty()
                    val friendlyError = if (syncRepo.isOperationNotAllowedError(authErr)) {
                        "Mode Tamu Cloud belum aktif di Firebase Console. Masuk Google terlebih dahulu untuk bergabung ke Room Cloud, atau gunakan Demo Offline."
                    } else {
                        "Gagal menghubungkan autentikasi cloud$reason. Periksa koneksi internet Anda."
                    }
                    _uiState.update {
                        it.copy(
                            isJoiningRoom = false,
                            joinError = friendlyError
                        )
                    }
                    return@launch
                }
                syncRepo.saveUserPrivateProfile(uid, state.vehicleProfile)
                val result = syncRepo.joinSyncedRoom(
                    roomId = extractedId,
                    uid = uid,
                    profile = state.vehicleProfile,
                    initialLat = startLat,
                    initialLng = startLng
                )
                if (result.isSuccess) {
                    val role = result.getOrDefault(UserRole.MEMBER)
                    lastComputedRouteSig = ""
                    seedRecordedTrackFromStart(startLat, startLng)
                    _uiState.update {
                        it.copy(
                            selectedRole = role,
                            joinError = null,
                            isJoiningRoom = false,
                            pendingInviteRoomId = null,
                            selectedMemberId = uid,
                            lastCompletedTrip = null,
                            activeRoom = TourRoom(
                                roomId = extractedId,
                                roomTitle = "Room $extractedId",
                                inviteLink = "https://konvoiku.id/join/$extractedId",
                                createdAtLabel = "Terhubung Cloud",
                                members = emptyList(),
                                isSyncedRoom = true
                            )
                        )
                    }
                    attachRoomSubscription(extractedId, uid)
                    pushPresenceSnapshotNow()
                    withContext(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isJoiningRoom = false,
                            joinError = result.exceptionOrNull()?.message ?: "Room $extractedId tidak ditemukan atau sudah berakhir."
                        )
                    }
                }
            }
            return true
        }

        joinLocalDemoRoom(extractedId)
        onSuccess()
        return true
    }

    fun joinOfflineDemoRoom(roomIdRaw: String = _uiState.value.joinInput) {
        val roomId = extractRoomId(roomIdRaw).takeIf { it.length >= 4 } ?: "KNV-8492"
        joinLocalDemoRoom(roomId)
    }

    private fun joinLocalDemoRoom(extractedId: String) {
        val profile = _uiState.value.vehicleProfile
        val waypoints = _uiState.value.routeConfig.waypoints
        lastComputedRouteSig = ""
        seedRecordedTrackFromStart(
            _uiState.value.userGpsLat ?: waypoints.firstOrNull()?.latitude ?: -6.648,
            _uiState.value.userGpsLng ?: waypoints.firstOrNull()?.longitude ?: 106.845
        )
        val leaderTpl = defaultConvoyTemplates[0]
        val peerTpl1 = defaultConvoyTemplates[1]
        val peerTpl2 = defaultConvoyTemplates[2]
        val hasGps = _uiState.value.gpsStatus == GpsTrackingStatus.ACTIVE

        val leaderMember = ConvoyMember(
            id = "leader-host",
            fullName = leaderTpl.name,
            vehicleType = leaderTpl.vehicleType,
            vehicleDetail = leaderTpl.vehicleDetail,
            licensePlate = leaderTpl.plate,
            emergencyContact = leaderTpl.phone,
            role = UserRole.TOUR_LEADER,
            joinedAtLabel = "Host Room",
            isCurrentUser = false,
            avatarPreset = leaderTpl.avatarPreset,
            conditionStatus = MemberConditionStatus.AMAN,
            connectionStatus = ConnectionStatus.CONNECTED,
            lastLocationText = "Puncak Pass KM 49",
            lastUpdatedLabel = "Baru saja",
            mapPosition = interpolateAlongPolyline(0.74f, waypoints, 0f, 58)
        )

        val selfMember = ConvoyMember(
            id = "self-${UUID.randomUUID()}",
            fullName = profile.fullName.trim().ifBlank { "Anda" },
            vehicleType = profile.vehicleType,
            vehicleDetail = profile.displayVehicleLabel,
            licensePlate = profile.licensePlate.ifBlank { "B 2026 AGT" },
            emergencyContact = profile.emergencyContact.ifBlank { "0812-9999-8888" },
            role = UserRole.MEMBER,
            joinedAtLabel = "Baru bergabung",
            isCurrentUser = true,
            avatarPreset = profile.avatarPreset,
            avatarPhotoUri = profile.avatarPhotoUri,
            conditionStatus = MemberConditionStatus.AMAN,
            connectionStatus = ConnectionStatus.CONNECTED,
            lastLocationText = if (hasGps) "GPS Live" else "Jalur Puncak KM 45",
            lastUpdatedLabel = "Baru saja",
            mapPosition = if (hasGps) {
                MapPosition(
                    0.45f,
                    0.45f,
                    0.55f,
                    _uiState.value.userGpsLat ?: -6.66,
                    _uiState.value.userGpsLng ?: 106.89,
                    0
                )
            } else {
                interpolateAlongPolyline(0.58f, waypoints, 0f, 54)
            }
        )

        val peer1 = ConvoyMember(
            id = "peer-1",
            fullName = peerTpl1.name,
            vehicleType = peerTpl1.vehicleType,
            vehicleDetail = peerTpl1.vehicleDetail,
            licensePlate = peerTpl1.plate,
            emergencyContact = peerTpl1.phone,
            role = UserRole.MEMBER,
            joinedAtLabel = "Aktif",
            isCurrentUser = false,
            isSweeper = true,
            avatarPreset = peerTpl1.avatarPreset,
            conditionStatus = peerTpl1.condition,
            connectionStatus = peerTpl1.status,
            lastLocationText = peerTpl1.locationText,
            lastUpdatedLabel = "1 dtk lalu",
            mapPosition = interpolateAlongPolyline(0.66f, waypoints, 0f, 52)
        )

        val peer2 = ConvoyMember(
            id = "peer-2",
            fullName = peerTpl2.name,
            vehicleType = peerTpl2.vehicleType,
            vehicleDetail = peerTpl2.vehicleDetail,
            licensePlate = peerTpl2.plate,
            emergencyContact = peerTpl2.phone,
            role = UserRole.MEMBER,
            joinedAtLabel = "Aktif",
            isCurrentUser = false,
            avatarPreset = peerTpl2.avatarPreset,
            conditionStatus = peerTpl2.condition,
            connectionStatus = peerTpl2.status,
            lastLocationText = peerTpl2.locationText,
            lastUpdatedLabel = "5 dtk lalu",
            mapPosition = interpolateAlongPolyline(0.51f, waypoints, 0f, 46)
        )

        val members = listOf(leaderMember, selfMember, peer1, peer2)
        val joinedRoom = TourRoom(
            roomId = extractedId,
            roomTitle = "Konvoi Bersama $extractedId",
            inviteLink = "https://konvoiku.id/join/$extractedId",
            createdAtLabel = "Terhubung",
            members = members,
            isSyncedRoom = false,
            leaderUid = leaderMember.id,
            actingLeaderUid = leaderMember.id
        )

        _uiState.update {
            it.copy(
                activeRoom = joinedRoom,
                joinError = null,
                selectedMemberId = selfMember.id,
                sosState = SosEmergencyState(),
                messages = initialRoomMessages(extractedId),
                isOffRouteSimActive = false,
                isStragglerSimActive = false,
                isLeaderTroubleSimActive = false
            )
        }
        recomputeAndCommitRoom(members)
    }

    fun isCurrentUserEffectiveLeader(): Boolean {
        val state = _uiState.value
        val room = state.activeRoom ?: return state.selectedRole == UserRole.TOUR_LEADER
        val self = room.members.find { it.isCurrentUser }
        if (self?.isActingLeader == true) return true
        val effectiveUid = room.actingLeaderUid.ifBlank { room.leaderUid }
        if (room.isSyncedRoom) {
            val uid = syncRepo.currentUid()
            if (uid != null && effectiveUid.isNotBlank()) return uid == effectiveUid
            return self?.role == UserRole.TOUR_LEADER && !room.leaderTroubleActive
        }
        if (room.leaderTroubleActive && room.actingLeaderUid.isNotBlank()) {
            return self?.id == room.actingLeaderUid
        }
        return state.selectedRole == UserRole.TOUR_LEADER || self?.role == UserRole.TOUR_LEADER
    }

    fun toggleLeaderTroubleSimulation() {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        if (room.isSyncedRoom) {
            val uid = syncRepo.currentUid() ?: return
            if (room.leaderTroubleActive) {
                reclaimLeaderAuthority()
            } else {
                val targetCoLeader = room.coLeaderUid.ifBlank {
                    GeoUtils.selectClosestCoLeader(room.members, room.leaderUid).coLeaderUid.orEmpty()
                }
                if (targetCoLeader.isBlank()) return
                viewModelScope.launch {
                    val reason = "Simulasi HP Leader kehilangan sinyal / baterai habis"
                    if (room.coLeaderUid != targetCoLeader) {
                        syncRepo.updateCoLeaderUid(room.roomId, targetCoLeader)
                    }
                    if (syncRepo.triggerLeaderFailover(room.roomId, targetCoLeader, reason)) {
                        val promotedName = room.members.firstOrNull { it.id == targetCoLeader }?.fullName ?: "Co-Leader"
                        syncRepo.sendMessage(
                            room.roomId,
                            uid,
                            RoomMessage(
                                id = "failover-sim-${UUID.randomUUID()}",
                                senderId = "system",
                                senderName = "Sistem Failover",
                                senderVehicle = "",
                                type = ChatMessageType.SYSTEM,
                                textContent = "Leader trouble ($reason). $promotedName otomatis menjadi Plt. Leader & mengambil alih ping TomTom API.",
                                timestampLabel = currentClockLabel()
                            )
                        )
                    }
                }
            }
            return
        }

        val nextActive = !state.isLeaderTroubleSimActive
        val leaderId = room.leaderUid.ifBlank {
            room.members.firstOrNull { it.role == UserRole.TOUR_LEADER }?.id.orEmpty()
        }
        if (nextActive) {
            val coResult = GeoUtils.selectClosestCoLeader(room.members, leaderId)
            val coLeaderId = coResult.coLeaderUid ?: room.coLeaderUid.ifBlank {
                room.members.firstOrNull { it.id != leaderId }?.id.orEmpty()
            }
            if (coLeaderId.isBlank()) return
            val promotedName = room.members.firstOrNull { it.id == coLeaderId }?.fullName ?: "Co-Leader"
            val updatedMembers = room.members.map { m ->
                if (m.id == leaderId) {
                    m.copy(
                        connectionStatus = ConnectionStatus.OFFLINE,
                        lastLocationText = "Sinyal HP Hilang / Baterai Habis",
                        mapPosition = m.mapPosition.copy(speedKmh = 0)
                    )
                } else m
            }
            val sysMsg = RoomMessage(
                id = "failover-sim-${UUID.randomUUID()}",
                senderId = "system",
                senderName = "Sistem Failover",
                senderVehicle = "",
                type = ChatMessageType.SYSTEM,
                textContent = "Leader trouble (sinyal hilang/baterai habis). Co-Leader terdekat $promotedName otomatis menggantikan posisi Leader & ping TomTom API.",
                timestampLabel = currentClockLabel()
            )
            _uiState.update {
                it.copy(
                    isLeaderTroubleSimActive = true,
                    activeRoom = room.copy(
                        leaderUid = leaderId,
                        coLeaderUid = coLeaderId,
                        actingLeaderUid = coLeaderId,
                        leaderTroubleActive = true,
                        leaderTroubleReason = "HP Leader kehilangan sinyal / baterai habis"
                    ),
                    messages = it.messages + sysMsg
                )
            }
            recomputeAndCommitRoom(updatedMembers)
        } else {
            reclaimLeaderAuthority()
        }
    }

    fun reclaimLeaderAuthority() {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        val leaderId = room.leaderUid.ifBlank {
            room.members.firstOrNull { it.role == UserRole.TOUR_LEADER }?.id.orEmpty()
        }
        val leaderName = room.members.firstOrNull { it.id == leaderId }?.fullName ?: "Tour Leader"
        val sysMsg = RoomMessage(
            id = "reclaim-${UUID.randomUUID()}",
            senderId = "system",
            senderName = "Sistem Konvoi",
            senderVehicle = "",
            type = ChatMessageType.SYSTEM,
            textContent = "$leaderName kembali terhubung dan mengambil alih posisi Tour Leader & ping TomTom API.",
            timestampLabel = currentClockLabel()
        )

        if (room.isSyncedRoom) {
            val uid = syncRepo.currentUid() ?: return
            viewModelScope.launch {
                if (syncRepo.reclaimOriginalLeader(room.roomId, leaderId.ifBlank { uid })) {
                    syncRepo.sendMessage(room.roomId, uid, sysMsg)
                }
            }
            _uiState.update { it.copy(isLeaderTroubleSimActive = false) }
            return
        }

        val updatedMembers = room.members.map { m ->
            if (m.id == leaderId) {
                m.copy(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    lastUpdatedLabel = "Baru saja"
                )
            } else m
        }
        _uiState.update {
            it.copy(
                isLeaderTroubleSimActive = false,
                activeRoom = room.copy(
                    leaderUid = leaderId,
                    actingLeaderUid = leaderId,
                    leaderTroubleActive = false,
                    leaderTroubleReason = ""
                ),
                messages = it.messages + sysMsg
            )
        }
        recomputeAndCommitRoom(updatedMembers)
    }

    fun selectMapMember(memberId: String) {
        _uiState.update { it.copy(selectedMemberId = memberId) }
    }

    fun toggleSimulation() {
        _uiState.update { it.copy(isSimulationRunning = !it.isSimulationRunning) }
    }

    // --- Checkpoints / Titik Istirahat ---

    fun startPickingCheckpointOnMap(checkpointName: String) {
        _uiState.update {
            it.copy(
                isPickingCheckpointOnMap = true,
                isPickingDestinationOnMap = false,
                pendingCheckpointName = checkpointName.trim()
            )
        }
    }

    fun cancelPickingCheckpoint() {
        _uiState.update {
            it.copy(
                isPickingCheckpointOnMap = false,
                pendingCheckpointName = ""
            )
        }
    }

    fun addRestCheckpointAtLatLng(name: String, latitude: Double, longitude: Double) {
        val cleanName = name.trim().ifBlank { "Titik Istirahat" }
        val checkpoint = RestCheckpoint(
            id = "cp-${UUID.randomUUID()}",
            name = cleanName,
            xFraction = 0.5f,
            yFraction = 0.5f,
            latitude = latitude,
            longitude = longitude
        )

        val broadcastMsg = RoomMessage(
            id = "cp-msg-${UUID.randomUUID()}",
            senderId = "system",
            senderName = "Tour Leader",
            senderVehicle = "",
            type = ChatMessageType.SYSTEM,
            textContent = "Titik istirahat baru dibagikan ke seluruh rombongan: $cleanName",
            timestampLabel = currentClockLabel()
        )

        val room = _uiState.value.activeRoom
        val uid = syncRepo.currentUid()
        if (room != null && room.isSyncedRoom && uid != null) {
            syncRepo.addCheckpoint(room.roomId, uid, checkpoint)
            syncRepo.sendMessage(room.roomId, uid, broadcastMsg)
            _uiState.update {
                it.copy(isPickingCheckpointOnMap = false, pendingCheckpointName = "")
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                routeConfig = state.routeConfig.copy(
                    checkpoints = state.routeConfig.checkpoints + checkpoint
                ),
                isPickingCheckpointOnMap = false,
                pendingCheckpointName = "",
                messages = state.messages + broadcastMsg
            )
        }
    }

    fun addRestCheckpointAtPoint(name: String, xFraction: Float, yFraction: Float) {
        val wps = _uiState.value.routeConfig.waypoints
        val start = wps.firstOrNull()
        val end = wps.lastOrNull()
        val factor = xFraction.coerceIn(0.1f, 0.9f).toDouble()
        val lat = if (start != null && end != null) {
            start.latitude + (end.latitude - start.latitude) * factor
        } else {
            -6.7100 + (1f - yFraction) * 0.08
        }
        val lng = if (start != null && end != null) {
            start.longitude + (end.longitude - start.longitude) * factor
        } else {
            106.8500 + xFraction * 0.16
        }
        addRestCheckpointAtLatLng(name, lat, lng)
    }

    fun removeRestCheckpoint(checkpointId: String) {
        val room = _uiState.value.activeRoom
        if (room != null && room.isSyncedRoom) {
            syncRepo.removeCheckpoint(room.roomId, checkpointId)
        }
        _uiState.update { state ->
            state.copy(
                routeConfig = state.routeConfig.copy(
                    checkpoints = state.routeConfig.checkpoints.filterNot { it.id == checkpointId }
                )
            )
        }
    }

    // --- Stage 4: SOS Emergency, Walkie-Talkie Voice, Chat, Condition & Sweeper ---

    fun triggerSosEmergency() {
        val room = _uiState.value.activeRoom ?: return
        val self = room.members.find { it.isCurrentUser } ?: room.members.firstOrNull() ?: return
        val now = currentClockLabel()
        val lat = _uiState.value.userGpsLat ?: self.mapPosition.latitude
        val lng = _uiState.value.userGpsLng ?: self.mapPosition.longitude

        val sos = SosEmergencyState(
            isActive = true,
            senderId = self.id,
            senderName = self.fullName,
            senderVehicle = self.vehicleDetail,
            latitude = lat,
            longitude = lng,
            locationText = self.lastLocationText,
            triggeredAtLabel = now
        )

        val sosMessage = RoomMessage(
            id = "msg-${UUID.randomUUID()}",
            senderId = self.id,
            senderName = self.fullName,
            senderVehicle = self.vehicleDetail,
            type = ChatMessageType.SOS_ALERT,
            textContent = "SINYAL DARURAT SOS di ${self.lastLocationText} (${String.format(Locale.US, "%.4f, %.4f", lat, lng)}). Butuh bantuan segera!",
            timestampLabel = now,
            isFromCurrentUser = self.isCurrentUser
        )

        val uid = syncRepo.currentUid()
        if (room.isSyncedRoom && uid != null) {
            syncRepo.triggerSos(room.roomId, sos, uid)
            syncRepo.sendMessage(room.roomId, uid, sosMessage)
            return
        }

        val updatedMembers = room.members.map { member ->
            if (member.id == self.id) member.copy(conditionStatus = MemberConditionStatus.SOS) else member
        }
        _uiState.update {
            it.copy(
                sosState = sos,
                selectedMemberId = self.id,
                messages = it.messages + sosMessage
            )
        }
        recomputeAndCommitRoom(updatedMembers)
    }

    fun resolveSosEmergency() {
        val room = _uiState.value.activeRoom ?: return
        val sosSenderId = _uiState.value.sosState.senderId
        val sysMsg = RoomMessage(
            id = "msg-${UUID.randomUUID()}",
            senderId = "system",
            senderName = "Sistem Konvoi",
            senderVehicle = "",
            type = ChatMessageType.SYSTEM,
            textContent = "Sinyal darurat SOS telah diselesaikan.",
            timestampLabel = currentClockLabel()
        )
        val uid = syncRepo.currentUid()
        if (room.isSyncedRoom && uid != null) {
            syncRepo.resolveSos(room.roomId, uid)
            syncRepo.sendMessage(room.roomId, uid, sysMsg)
            return
        }

        val updatedMembers = room.members.map { member ->
            if (member.id == sosSenderId && member.conditionStatus == MemberConditionStatus.SOS) {
                member.copy(conditionStatus = MemberConditionStatus.AMAN)
            } else member
        }
        _uiState.update {
            it.copy(
                sosState = SosEmergencyState(),
                messages = it.messages + sysMsg
            )
        }
        recomputeAndCommitRoom(updatedMembers)
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val room = _uiState.value.activeRoom ?: return
        val self = room.members.find { it.isCurrentUser }
        val profile = _uiState.value.vehicleProfile
        val message = RoomMessage(
            id = "msg-${UUID.randomUUID()}",
            senderId = self?.id ?: "self",
            senderName = self?.fullName ?: "Anda",
            senderVehicle = self?.vehicleDetail ?: "Kendaraan",
            type = ChatMessageType.TEXT,
            textContent = trimmed,
            timestampLabel = currentClockLabel(),
            isFromCurrentUser = true,
            senderAvatarPreset = self?.avatarPreset ?: profile.avatarPreset,
            senderAvatarPhotoUri = self?.avatarPhotoUri ?: profile.avatarPhotoUri
        )
        val uid = syncRepo.currentUid()
        if (room.isSyncedRoom && uid != null) {
            syncRepo.sendMessage(room.roomId, uid, message)
            return
        }
        _uiState.update { it.copy(messages = it.messages + message) }
    }

    private fun showVoiceStatus(message: String) {
        _uiState.update { it.copy(voiceStatusMessage = message) }
        voiceStatusClearJob?.cancel()
        voiceStatusClearJob = viewModelScope.launch {
            delay(4500L)
            _uiState.update { cur ->
                if (cur.voiceStatusMessage == message) cur.copy(voiceStatusMessage = null) else cur
            }
        }
    }

    private fun releaseVoiceRecorder(deleteFile: Boolean) {
        val recorder = voiceRecorder
        voiceRecorder = null
        if (recorder != null) {
            runCatching { recorder.stop() }
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
        }
        if (deleteFile) {
            voiceOutputFile?.let { runCatching { it.delete() } }
            voiceOutputFile = null
        }
    }

    private fun stopVoicePlayback() {
        val player = voicePlayer
        voicePlayer = null
        if (player != null) {
            runCatching {
                if (player.isPlaying) player.stop()
            }
            runCatching { player.release() }
        }
        _uiState.update { it.copy(playingMessageId = null) }
    }

    fun startVoiceRecording() {
        if (_uiState.value.isRecordingVoice) return
        val appContext = getApplication<Application>().applicationContext
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            showVoiceStatus("Izin mikrofon diperlukan untuk mengirim pesan Walkie-Talkie.")
            return
        }

        stopVoicePlayback()
        releaseVoiceRecorder(deleteFile = true)
        _uiState.update { it.copy(voiceStatusMessage = null) }

        val folder = File(appContext.cacheDir, "voice_ptt").apply { mkdirs() }
        val outFile = File(folder, "voice_rec_${UUID.randomUUID()}.m4a")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val started = runCatching {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(16_000)
            recorder.setAudioEncodingBitRate(24_000)
            recorder.setMaxDuration(30_000)
            recorder.setMaxFileSize(220_000L)
            recorder.setOutputFile(outFile.absolutePath)
            recorder.setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                    what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                ) {
                    viewModelScope.launch(Dispatchers.Main) {
                        stopAndSendVoiceRecording()
                    }
                }
            }
            recorder.prepare()
            recorder.start()
        }

        if (started.isFailure) {
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            runCatching { outFile.delete() }
            showVoiceStatus("Gagal mengaktifkan mikrofon perangkat.")
            return
        }

        voiceRecorder = recorder
        voiceOutputFile = outFile
        voiceRecordingStartMs = System.currentTimeMillis()
        _uiState.update { it.copy(isRecordingVoice = true, recordingSeconds = 0) }
        voiceTimerJob?.cancel()
        voiceTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val nextSec = ((System.currentTimeMillis() - voiceRecordingStartMs) / 1000L)
                    .toInt()
                    .coerceAtLeast(1)
                _uiState.update { it.copy(recordingSeconds = nextSec) }
                if (nextSec >= 28) {
                    stopAndSendVoiceRecording()
                    break
                }
            }
        }
    }

    fun stopAndSendVoiceRecording(filePath: String? = null) {
        if (!_uiState.value.isRecordingVoice) return
        voiceTimerJob?.cancel()
        val elapsedSec = ((System.currentTimeMillis() - voiceRecordingStartMs) / 1000.0)
            .roundToInt()
            .coerceAtLeast(_uiState.value.recordingSeconds)
            .coerceAtLeast(1)

        val recorder = voiceRecorder
        voiceRecorder = null
        val stopOk = if (recorder != null) {
            val res = runCatching { recorder.stop() }
            runCatching { recorder.reset() }
            runCatching { recorder.release() }
            res.isSuccess
        } else {
            !filePath.isNullOrBlank()
        }

        val capturedFile = filePath?.let { File(it) } ?: voiceOutputFile
        voiceOutputFile = null

        _uiState.update { it.copy(isRecordingVoice = false, recordingSeconds = 0) }

        if (!stopOk || capturedFile == null || !capturedFile.exists() || capturedFile.length() < 512L) {
            capturedFile?.let { runCatching { it.delete() } }
            showVoiceStatus("Rekaman terlalu singkat. Tahan tombol minimal 1 detik.")
            return
        }

        val room = _uiState.value.activeRoom
        val self = room?.members?.find { it.isCurrentUser }
        val profile = _uiState.value.vehicleProfile
        val messageId = "voice-${UUID.randomUUID()}"

        viewModelScope.launch(Dispatchers.IO) {
            val bytes = runCatching { capturedFile.readBytes() }.getOrNull()
            if (bytes == null || bytes.size < 512) {
                withContext(Dispatchers.Main) {
                    showVoiceStatus("File audio rekaman tidak terbaca.")
                }
                return@launch
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUrl = "data:audio/mp4;base64,$b64"
            if (dataUrl.length > 345_000) {
                runCatching { capturedFile.delete() }
                withContext(Dispatchers.Main) {
                    showVoiceStatus("Rekaman terlalu panjang (maks 28 detik). Coba lebih singkat.")
                }
                return@launch
            }

            val voiceMsg = RoomMessage(
                id = messageId,
                senderId = self?.id ?: "self",
                senderName = self?.fullName ?: profile.fullName.ifBlank { "Anda" },
                senderVehicle = self?.vehicleDetail ?: profile.displayVehicleLabel,
                type = ChatMessageType.VOICE,
                textContent = "Pesan Suara PTT ($elapsedSec detik)",
                timestampLabel = currentClockLabel(),
                voiceDurationSec = elapsedSec,
                voiceFilePath = capturedFile.absolutePath,
                voiceAudioDataUrl = dataUrl,
                isFromCurrentUser = true,
                senderAvatarPreset = self?.avatarPreset ?: profile.avatarPreset,
                senderAvatarPhotoUri = self?.avatarPhotoUri ?: profile.avatarPhotoUri
            )

            withContext(Dispatchers.Main) {
                val currentRoom = _uiState.value.activeRoom
                val uid = syncRepo.currentUid()
                if (currentRoom != null && currentRoom.isSyncedRoom && uid != null) {
                    syncRepo.sendMessage(currentRoom.roomId, uid, voiceMsg)
                } else {
                    _uiState.update { it.copy(messages = it.messages + voiceMsg) }
                }
            }
        }
    }

    fun toggleVoicePlayback(messageId: String) {
        if (_uiState.value.playingMessageId == messageId) {
            stopVoicePlayback()
            return
        }
        val msg = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        stopVoicePlayback()

        viewModelScope.launch(Dispatchers.IO) {
            val appContext = getApplication<Application>().applicationContext
            val localFile = msg.voiceFilePath?.let { File(it) }?.takeIf { it.exists() && it.length() >= 256L }
            val playableFile = localFile ?: run {
                val dataUrl = msg.voiceAudioDataUrl ?: return@run null
                val commaIdx = dataUrl.indexOf(',')
                if (!dataUrl.startsWith("data:audio/") || commaIdx <= 0) return@run null
                val header = dataUrl.substring(0, commaIdx).lowercase(Locale.ROOT)
                val ext = when {
                    header.contains("webm") -> "webm"
                    header.contains("ogg") -> "ogg"
                    header.contains("mpeg") || header.contains("mp3") -> "mp3"
                    else -> "m4a"
                }
                val safeId = msg.id.replace(Regex("[^a-zA-Z0-9_-]"), "").take(48)
                    .ifBlank { "clip" }
                val folder = File(appContext.cacheDir, "voice_ptt").apply { mkdirs() }
                val cached = File(folder, "voice_play_$safeId.$ext")
                if (!cached.exists() || cached.length() < 256L) {
                    val b64 = dataUrl.substring(commaIdx + 1)
                    val decoded = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
                        ?: return@run null
                    if (decoded.size < 256) return@run null
                    runCatching {
                        FileOutputStream(cached).use { it.write(decoded) }
                    }.onFailure {
                        return@run null
                    }
                }
                cached
            }

            if (playableFile == null || !playableFile.exists()) {
                withContext(Dispatchers.Main) {
                    showVoiceStatus("Rekaman audio tidak tersedia untuk pesan suara ini.")
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setOnCompletionListener { mp ->
                        runCatching { mp.release() }
                        if (voicePlayer === mp) voicePlayer = null
                        _uiState.update {
                            if (it.playingMessageId == messageId) it.copy(playingMessageId = null) else it
                        }
                    }
                    setOnErrorListener { mp, _, _ ->
                        runCatching { mp.release() }
                        if (voicePlayer === mp) voicePlayer = null
                        _uiState.update {
                            if (it.playingMessageId == messageId) it.copy(playingMessageId = null) else it
                        }
                        showVoiceStatus("Gagal memutar audio Walkie-Talkie di perangkat.")
                        true
                    }
                }

                val started = runCatching {
                    player.setDataSource(playableFile.absolutePath)
                    player.prepare()
                    player.start()
                }

                if (started.isSuccess) {
                    voicePlayer = player
                    _uiState.update { it.copy(playingMessageId = messageId, voiceStatusMessage = null) }
                } else {
                    runCatching { player.release() }
                    showVoiceStatus("Format audio tidak dapat diputar oleh perangkat.")
                }
            }
        }
    }

    fun updateOwnConditionStatus(newStatus: MemberConditionStatus) {
        val room = _uiState.value.activeRoom ?: return
        val self = room.members.find { it.isCurrentUser } ?: return
        val statusMsg = RoomMessage(
            id = "status-${UUID.randomUUID()}",
            senderId = self.id,
            senderName = self.fullName,
            senderVehicle = self.vehicleDetail,
            type = ChatMessageType.SYSTEM,
            textContent = "${self.fullName} memperbarui status: ${newStatus.label}",
            timestampLabel = currentClockLabel()
        )
        val uid = syncRepo.currentUid()
        if (room.isSyncedRoom && uid != null) {
            syncRepo.updateMemberCondition(room.roomId, uid, newStatus)
            syncRepo.sendMessage(room.roomId, uid, statusMsg)
            return
        }

        val updatedMembers = room.members.map { member ->
            if (member.isCurrentUser) member.copy(conditionStatus = newStatus) else member
        }
        _uiState.update { it.copy(messages = it.messages + statusMsg) }
        recomputeAndCommitRoom(updatedMembers)
    }

    fun assignSweeper(memberId: String) {
        val room = _uiState.value.activeRoom ?: return
        val target = room.members.find { it.id == memberId } ?: return
        val sysMsg = RoomMessage(
            id = "sweeper-${UUID.randomUUID()}",
            senderId = "system",
            senderName = "Tour Leader",
            senderVehicle = "",
            type = ChatMessageType.SYSTEM,
            textContent = "${target.fullName} (${target.vehicleDetail}) ditetapkan sebagai Sweeper penutup barisan.",
            timestampLabel = currentClockLabel()
        )
        val uid = syncRepo.currentUid()
        if (room.isSyncedRoom && uid != null) {
            syncRepo.assignSweeper(room.roomId, memberId, room.members.map { it.id })
            syncRepo.sendMessage(room.roomId, uid, sysMsg)
            return
        }
        val updatedMembers = room.members.map { member ->
            member.copy(
                isSweeper = member.id == memberId,
                isAutoSweeper = false
            )
        }
        _uiState.update { it.copy(messages = it.messages + sysMsg) }
        recomputeAndCommitRoom(updatedMembers)
    }

    // --- Stage 3: Leader Route, Destination, Off-Route & Straggler Config ---

    fun setCustomOriginLatLng(
        latitude: Double,
        longitude: Double,
        placeName: String? = null,
        addressLabel: String? = null
    ) {
        val state = _uiState.value
        val startPoint = RoutePoint(0.14f, 0.86f, latitude, longitude)
        val existing = state.routeConfig.waypoints
        val destPoint = existing.lastOrNull()
            ?: RoutePoint(0.86f, 0.14f, -6.7003, 106.9960)
        val waypoints = if (existing.size >= 3) {
            listOf(startPoint) + existing.drop(1)
        } else {
            buildPolylineToDestination(startPoint, destPoint)
        }
        val estimatedMeters = (GeoUtils.haversineMeters(
            LatLngGeo(latitude, longitude),
            LatLngGeo(destPoint.latitude, destPoint.longitude)
        ) * 1.25).roundToInt().coerceAtLeast(500)

        val resolvedName = placeName?.trim()?.takeIf { it.isNotEmpty() }
            ?: String.format(Locale.US, "Titik Start (%.4f, %.4f)", latitude, longitude)
        val resolvedAddress = addressLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: String.format(Locale.US, "%s · %.4f, %.4f", resolvedName, latitude, longitude)

        lastComputedRouteSig = ""
        val nextConfig = state.routeConfig.copy(
            originName = resolvedName,
            originAddress = resolvedAddress,
            originLockedByLeader = true,
            waypoints = waypoints,
            routeInstructions = emptyList(),
            totalRouteMeters = estimatedMeters
        )
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        syncMembersToUpdatedRoute()
    }

    fun useCurrentGpsAsOrigin(): Boolean {
        val state = _uiState.value
        val lat = state.userGpsLat
        val lng = state.userGpsLng
        if (lat == null || lng == null || !GeoUtils.hasValidGeoCoord(lat, lng)) {
            return false
        }
        setCustomOriginLatLng(
            latitude = lat,
            longitude = lng,
            placeName = "Lokasi GPS Saya Sekarang",
            addressLabel = String.format(Locale.US, "GPS Perangkat · %.5f, %.5f", lat, lng)
        )
        return true
    }

    fun selectRoutePreference(preference: RoutePreference) {
        val state = _uiState.value
        if (state.routeConfig.routePreference == preference) return
        lastComputedRouteSig = ""
        val nextConfig = state.routeConfig.copy(
            routePreference = preference,
            routeInstructions = emptyList()
        )
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
    }

    fun selectDestinationPreset(preset: DestinationPreset) {
        val state = _uiState.value
        val existingStart = state.routeConfig.waypoints.firstOrNull()
        val start = if (state.routeConfig.originLockedByLeader && existingStart != null) {
            existingStart
        } else if (existingStart != null) {
            existingStart
        } else {
            state.userGpsLat?.let { lat ->
                state.userGpsLng?.let { lng -> RoutePoint(0.14f, 0.86f, lat, lng) }
            } ?: RoutePoint(0.14f, 0.86f, -6.648, 106.845)
        }

        val waypoints = buildPolylineToDestination(start, preset.endPoint)
        val estimatedMeters = if (state.routeConfig.originLockedByLeader) {
            (GeoUtils.haversineMeters(
                LatLngGeo(start.latitude, start.longitude),
                LatLngGeo(preset.endPoint.latitude, preset.endPoint.longitude)
            ) * 1.25).roundToInt().coerceAtLeast(500)
        } else {
            preset.totalMeters
        }
        val coordText = String.format(
            Locale.US,
            "%.4f, %.4f",
            preset.endPoint.latitude,
            preset.endPoint.longitude
        )
        lastComputedRouteSig = ""
        val nextConfig = state.routeConfig.copy(
            destinationName = preset.name,
            destinationAddress = "${preset.name} · $coordText",
            waypoints = waypoints,
            routeInstructions = emptyList(),
            totalRouteMeters = estimatedMeters
        )
        _uiState.update {
            it.copy(routeConfig = nextConfig, isPickingDestinationOnMap = false)
        }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        syncMembersToUpdatedRoute()
    }

    fun setCustomDestinationLatLng(
        latitude: Double,
        longitude: Double,
        placeName: String? = null,
        addressLabel: String? = null
    ) {
        val state = _uiState.value
        val existingStart = state.routeConfig.waypoints.firstOrNull()
        val start = if (state.routeConfig.originLockedByLeader && existingStart != null) {
            existingStart
        } else if (existingStart != null) {
            existingStart
        } else {
            state.userGpsLat?.let { lat ->
                state.userGpsLng?.let { lng -> RoutePoint(0.14f, 0.86f, lat, lng) }
            } ?: RoutePoint(0.14f, 0.86f, -6.648, 106.845)
        }

        val endPoint = RoutePoint(0.86f, 0.14f, latitude, longitude)
        val waypoints = buildPolylineToDestination(start, endPoint)
        val estimatedMeters = (GeoUtils.haversineMeters(
            LatLngGeo(start.latitude, start.longitude),
            LatLngGeo(latitude, longitude)
        ) * 1.25).roundToInt().coerceAtLeast(500)

        val resolvedName = placeName?.trim()?.takeIf { it.isNotEmpty() }
            ?: String.format(Locale.US, "Titik Tujuan (%.4f, %.4f)", latitude, longitude)
        val resolvedAddress = addressLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: String.format(Locale.US, "%s · %.4f, %.4f", resolvedName, latitude, longitude)

        lastComputedRouteSig = ""
        val nextConfig = state.routeConfig.copy(
            destinationName = resolvedName,
            destinationAddress = resolvedAddress,
            waypoints = waypoints,
            routeInstructions = emptyList(),
            totalRouteMeters = estimatedMeters
        )
        _uiState.update {
            it.copy(routeConfig = nextConfig, isPickingDestinationOnMap = false)
        }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        syncMembersToUpdatedRoute()
    }

    fun setCustomDestinationPoint(
        xFraction: Float,
        yFraction: Float,
        placeName: String? = null,
        addressLabel: String? = null
    ) {
        val clampedX = xFraction.coerceIn(0.08f, 0.92f)
        val clampedY = yFraction.coerceIn(0.08f, 0.90f)
        val lat = -6.7100 + (1f - clampedY) * 0.08
        val lng = 106.8500 + clampedX * 0.16
        setCustomDestinationLatLng(lat, lng, placeName, addressLabel)
    }

    fun setPickingDestinationMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isPickingDestinationOnMap = enabled,
                isPickingCheckpointOnMap = false
            )
        }
    }

    fun updateCustomWaypoints(waypoints: List<RoutePoint>, destinationName: String? = null) {
        if (waypoints.size < 2) return
        val state = _uiState.value
        lastComputedRouteSig = ""
        val nextConfig = state.routeConfig.copy(
            destinationName = destinationName?.takeIf { it.isNotBlank() }
                ?: state.routeConfig.destinationName,
            waypoints = waypoints,
            routeInstructions = emptyList()
        )
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        syncMembersToUpdatedRoute()
    }

    fun updateComputedTomTomRoute(
        distanceMeters: Int,
        roadWaypoints: List<RoutePoint>,
        instructions: List<RouteInstruction> = emptyList()
    ) {
        if (roadWaypoints.size < 2) return
        val state = _uiState.value
        val first = roadWaypoints.first()
        val last = roadWaypoints.last()
        val sig = String.format(
            Locale.US,
            "%s|%.4f,%.4f->%.4f,%.4f|%d|%d",
            state.routeConfig.routePreference.id,
            first.latitude,
            first.longitude,
            last.latitude,
            last.longitude,
            roadWaypoints.size,
            instructions.size
        )
        if (sig == lastComputedRouteSig) return
        lastComputedRouteSig = sig

        val nextConfig = state.routeConfig.copy(
            waypoints = roadWaypoints,
            routeInstructions = instructions,
            totalRouteMeters = distanceMeters.coerceAtLeast(100)
        )
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true && isCurrentUserEffectiveLeader()) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
    }

    private fun recordGpsPoint(lat: Double, lng: Double, accuracyMeters: Int) {
        if (!GeoUtils.hasValidGeoCoord(lat, lng)) return
        val isFirst = recordedTrackPoints.isEmpty()
        if (!isFirst && accuracyMeters > 80) return
        val nextPt = LatLngGeo(lat, lng)
        val prev = recordedTrackPoints.lastOrNull()
        if (prev == null || GeoUtils.haversineMeters(prev, nextPt) >= 15.0) {
            recordedTrackPoints.add(nextPt)
            if (recordedTrackPoints.size > 1800) {
                val compact = GeoUtils.downsampleTrackPoints(recordedTrackPoints, 15.0, 300)
                recordedTrackPoints.clear()
                recordedTrackPoints.addAll(compact)
            }
        }
    }

    private fun seedRecordedTrackFromStart(lat: Double, lng: Double) {
        recordedTrackPoints.clear()
        if (GeoUtils.hasValidGeoCoord(lat, lng)) {
            recordedTrackPoints.add(LatLngGeo(lat, lng))
        }
    }

    private fun mergeCloudTrips(cloudTrips: List<TripHistoryRecord>) {
        if (cloudTrips.isEmpty()) return
        _uiState.update { state ->
            val existingById = state.tripHistory.associateBy { it.id }
            val mergedCloud = cloudTrips.map { cloud ->
                val existing = existingById[cloud.id]
                if (existing != null) {
                    existing.copy(
                        waypoints = if (cloud.waypoints.size >= 2) cloud.waypoints else existing.waypoints
                    )
                } else {
                    cloud
                }
            }
            val cloudIds = mergedCloud.map { it.id }.toSet()
            val remainingLocal = state.tripHistory.filterNot { it.id in cloudIds }
            val combined = mergedCloud + remainingLocal
            state.copy(
                tripHistory = combined,
                selectedTripId = state.selectedTripId ?: combined.firstOrNull()?.id
            )
        }
    }

    fun resetDefaultWaypoints() {
        val state = _uiState.value
        val first = state.routeConfig.waypoints.firstOrNull()
        val last = state.routeConfig.waypoints.lastOrNull()
        val resetWaypoints = if (first != null && last != null &&
            (state.routeConfig.originLockedByLeader || state.routeConfig.waypoints.size >= 2)
        ) {
            listOf(first, last)
        } else {
            ConvoyRouteConfig.defaultWaypoints()
        }
        lastComputedRouteSig = ""
        val nextConfig = state.routeConfig.copy(
            waypoints = resetWaypoints,
            routeInstructions = emptyList()
        )
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        syncMembersToUpdatedRoute()
    }

    fun updateMaxStragglerDistance(meters: Int) {
        val state = _uiState.value
        val nextConfig = state.routeConfig.copy(maxStragglerDistanceMeters = meters)
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        _uiState.value.activeRoom?.members?.let { recomputeAndCommitRoom(it) }
    }

    fun updateOffRouteTolerance(meters: Int) {
        val state = _uiState.value
        val nextConfig = state.routeConfig.copy(offRouteToleranceMeters = meters)
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
        _uiState.value.activeRoom?.members?.let { recomputeAndCommitRoom(it) }
    }

    fun toggleSoundAlert() {
        val state = _uiState.value
        val nextConfig = state.routeConfig.copy(soundAlertEnabled = !state.routeConfig.soundAlertEnabled)
        _uiState.update { it.copy(routeConfig = nextConfig) }
        if (state.activeRoom?.isSyncedRoom == true) {
            syncRepo.updateRouteConfig(state.activeRoom.roomId, nextConfig)
        }
    }

    fun toggleOffRouteSimulation() {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        if (room.isSyncedRoom) return
        val nextActive = !state.isOffRouteSimActive
        _uiState.update { it.copy(isOffRouteSimActive = nextActive) }
        val waypoints = state.routeConfig.waypoints
        val updated = room.members.map { member ->
            if (member.isCurrentUser) {
                val nextPos = interpolateAlongPolyline(
                    progress = member.mapPosition.routeProgress,
                    waypoints = waypoints,
                    lateralOffset = if (nextActive) 0.12f else 0f,
                    speedKmh = member.mapPosition.speedKmh
                )
                member.copy(mapPosition = nextPos)
            } else member
        }
        recomputeAndCommitRoom(updated)
    }

    fun toggleStragglerSimulation() {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        if (room.isSyncedRoom) return
        val nextActive = !state.isStragglerSimActive
        _uiState.update { it.copy(isStragglerSimActive = nextActive) }
        val waypoints = state.routeConfig.waypoints
        val ordered = room.members.sortedByDescending { it.mapPosition.routeProgress }
        val tailId = ordered.lastOrNull()?.id ?: return

        val updated = room.members.map { member ->
            if (member.id == tailId) {
                val targetProgress = if (nextActive) 0.18f else 0.56f
                val nextPos = interpolateAlongPolyline(
                    progress = targetProgress,
                    waypoints = waypoints,
                    lateralOffset = 0f,
                    speedKmh = if (nextActive) 28 else 52
                )
                member.copy(
                    mapPosition = nextPos,
                    lastLocationText = landmarkForProgress(targetProgress)
                )
            } else member
        }
        recomputeAndCommitRoom(updated)
    }

    private fun syncMembersToUpdatedRoute() {
        val state = _uiState.value
        val room = state.activeRoom ?: return
        if (room.isSyncedRoom) {
            recomputeAndCommitRoom(room.members)
            return
        }
        val synced = room.members.map { member ->
            if (member.isCurrentUser && state.gpsStatus == GpsTrackingStatus.ACTIVE) {
                return@map member
            }
            val offset = if (member.isCurrentUser && state.isOffRouteSimActive) 0.12f else 0f
            member.copy(
                mapPosition = interpolateAlongPolyline(
                    progress = member.mapPosition.routeProgress,
                    waypoints = state.routeConfig.waypoints,
                    lateralOffset = offset,
                    speedKmh = member.mapPosition.speedKmh
                )
            )
        }
        recomputeAndCommitRoom(synced)
    }

    private fun recomputeAndCommitRoom(rawMembers: List<ConvoyMember>) {
        val state = _uiState.value
        val route = state.routeConfig
        val room = state.activeRoom ?: return

        val enriched = rawMembers.map { member ->
            val hasValidGeo = abs(member.mapPosition.latitude) > 0.001 ||
                abs(member.mapPosition.longitude) > 0.001
            if (hasValidGeo && route.waypoints.size >= 2) {
                val eval = GeoUtils.evaluatePositionAlongRoute(
                    pos = LatLngGeo(member.mapPosition.latitude, member.mapPosition.longitude),
                    waypoints = route.waypoints,
                    totalRouteMetersFallback = route.totalRouteMeters
                )
                member.copy(
                    routeDistanceMeters = eval.distanceTraveledMeters,
                    offRouteMeters = eval.offRouteMeters,
                    mapPosition = member.mapPosition.copy(routeProgress = eval.routeProgress)
                )
            } else {
                val routeMeters = (member.mapPosition.routeProgress * route.totalRouteMeters).roundToInt()
                member.copy(routeDistanceMeters = routeMeters, offRouteMeters = 0)
            }
        }

        val leaderMember = enriched.firstOrNull {
            room.leaderUid.isNotBlank() && it.id == room.leaderUid
        } ?: enriched.firstOrNull { it.role == UserRole.TOUR_LEADER }
        val resolvedLeaderId = leaderMember?.id ?: room.leaderUid
        val coLeaderResult = GeoUtils.selectClosestCoLeader(enriched, resolvedLeaderId)

        val effectiveCoLeaderUid = coLeaderResult.coLeaderUid ?: room.coLeaderUid
        val isTroubleActive = room.leaderTroubleActive || (!room.isSyncedRoom && state.isLeaderTroubleSimActive)
        val effectiveActingUid = if (isTroubleActive) {
            room.actingLeaderUid.takeIf { id -> id.isNotBlank() && enriched.any { it.id == id } }
                ?: effectiveCoLeaderUid.ifBlank { resolvedLeaderId }
        } else {
            resolvedLeaderId
        }

        val sortedByRoute = enriched.sortedByDescending { it.routeDistanceMeters }
        val rankMap = sortedByRoute.mapIndexed { idx, m -> m.id to (idx + 1) }.toMap()
        val hasManualSweeper = enriched.any { it.isSweeper && !it.isAutoSweeper }
        val autoSweeperId = if (!hasManualSweeper && sortedByRoute.size >= 2) {
            val nonLeaderRearmost = sortedByRoute.asReversed().firstOrNull {
                it.id != resolvedLeaderId &&
                    it.id != effectiveActingUid &&
                    it.role != UserRole.TOUR_LEADER
            }
            (nonLeaderRearmost ?: sortedByRoute.last()).id
        } else {
            ""
        }

        val rankedMembers = enriched.map {
            val isManualSweeper = it.isSweeper && !it.isAutoSweeper
            val isAutoSweeper = !hasManualSweeper && autoSweeperId.isNotBlank() && it.id == autoSweeperId
            it.copy(
                convoyOrderRank = rankMap[it.id] ?: 1,
                isCoLeader = effectiveCoLeaderUid.isNotBlank() && it.id == effectiveCoLeaderUid,
                isActingLeader = effectiveActingUid.isNotBlank() && it.id == effectiveActingUid,
                isSweeper = isManualSweeper || isAutoSweeper,
                isAutoSweeper = isAutoSweeper,
                distanceToLeaderMeters = coLeaderResult.distanceByMemberId[it.id]
            )
        }
        val sortedRanked = rankedMembers.sortedByDescending { it.routeDistanceMeters }

        if (room.isSyncedRoom &&
            !coLeaderResult.coLeaderUid.isNullOrBlank() &&
            coLeaderResult.coLeaderUid != room.coLeaderUid &&
            coLeaderResult.coLeaderUid != lastSyncedCoLeaderUid
        ) {
            val uid = syncRepo.currentUid()
            if (uid != null && (uid == resolvedLeaderId || uid == effectiveActingUid)) {
                lastSyncedCoLeaderUid = coLeaderResult.coLeaderUid
                syncRepo.updateCoLeaderUid(room.roomId, coLeaderResult.coLeaderUid)
            }
        }

        val self = rankedMembers.find { it.isCurrentUser }
        val selfDeviation = self?.offRouteMeters ?: 0
        val offRouteAlert = OffRouteAlertState(
            isOffRoute = selfDeviation > route.offRouteToleranceMeters,
            deviationMeters = selfDeviation,
            toleranceMeters = route.offRouteToleranceMeters
        )

        val stragglerAlert = if (sortedRanked.size >= 2) {
            val designatedSweeper = sortedRanked.find { it.isSweeper && it.id != sortedRanked.last().id }
            val aheadVehicle = designatedSweeper ?: sortedRanked[sortedRanked.size - 2]
            val trailingVehicle = sortedRanked[sortedRanked.size - 1]
            val gapMeters = (aheadVehicle.routeDistanceMeters - trailingVehicle.routeDistanceMeters)
                .coerceAtLeast(0)
            StragglerAlertState(
                isTriggered = gapMeters > route.maxStragglerDistanceMeters,
                trailingMember = trailingVehicle,
                aheadMember = aheadVehicle,
                gapMeters = gapMeters,
                maxAllowedMeters = route.maxStragglerDistanceMeters
            )
        } else {
            StragglerAlertState(maxAllowedMeters = route.maxStragglerDistanceMeters)
        }

        _uiState.update {
            it.copy(
                activeRoom = room.copy(
                    members = rankedMembers,
                    leaderUid = resolvedLeaderId,
                    coLeaderUid = effectiveCoLeaderUid,
                    actingLeaderUid = effectiveActingUid,
                    leaderTroubleActive = isTroubleActive
                ),
                offRouteAlert = offRouteAlert,
                stragglerAlert = stragglerAlert
            )
        }
    }

    private fun initialRoomMessages(roomCode: String): List<RoomMessage> = listOf(
        RoomMessage(
            id = "init-sys",
            senderId = "system",
            senderName = "Sistem Konvoi",
            senderVehicle = "",
            type = ChatMessageType.SYSTEM,
            textContent = "Room $roomCode aktif. Jaga jarak aman dan pantau titik istirahat di peta.",
            timestampLabel = currentClockLabel()
        ),
        RoomMessage(
            id = "init-1",
            senderId = "leader",
            senderName = "Budi",
            senderVehicle = "Avanza Hitam",
            type = ChatMessageType.TEXT,
            textContent = "Rombongan siap, jalur TomTom Maps telah ditetapkan.",
            timestampLabel = currentClockLabel(),
            senderAvatarPreset = AvatarPreset.RIDER_CAPTAIN
        )
    )

    private fun currentClockLabel(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    private fun buildPolylineToDestination(start: RoutePoint, end: RoutePoint): List<RoutePoint> {
        val mid1 = RoutePoint(
            xFraction = start.xFraction + (end.xFraction - start.xFraction) * 0.33f,
            yFraction = start.yFraction + (end.yFraction - start.yFraction) * 0.33f,
            latitude = start.latitude + (end.latitude - start.latitude) * 0.33,
            longitude = start.longitude + (end.longitude - start.longitude) * 0.33
        )
        val mid2 = RoutePoint(
            xFraction = start.xFraction + (end.xFraction - start.xFraction) * 0.66f,
            yFraction = start.yFraction + (end.yFraction - start.yFraction) * 0.66f,
            latitude = start.latitude + (end.latitude - start.latitude) * 0.66,
            longitude = start.longitude + (end.longitude - start.longitude) * 0.66
        )
        return listOf(start, mid1, mid2, end)
    }

    private fun interpolateAlongPolyline(
        progress: Float,
        waypoints: List<RoutePoint>,
        lateralOffset: Float,
        speedKmh: Int
    ): MapPosition {
        val p = progress.coerceIn(0.05f, 0.95f)
        if (waypoints.size < 2) {
            return MapPosition(p, 0.5f, 0.5f, -6.67, 106.92, speedKmh)
        }
        val segmentCount = waypoints.size - 1
        val scaled = p * segmentCount
        val idx = scaled.toInt().coerceIn(0, segmentCount - 1)
        val localT = scaled - idx
        val a = waypoints[idx]
        val b = waypoints[idx + 1]

        val dx = b.xFraction - a.xFraction
        val dy = b.yFraction - a.yFraction
        val len = hypot(dx, dy).coerceAtLeast(0.001f)
        val perpX = -dy / len
        val perpY = dx / len

        val dLat = b.latitude - a.latitude
        val dLng = b.longitude - a.longitude
        val geoLen = hypot(dLat, dLng).coerceAtLeast(0.0001)
        val latShift = (-dLng / geoLen) * lateralOffset * 0.03
        val lngShift = (dLat / geoLen) * lateralOffset * 0.03

        val x = (a.xFraction + dx * localT + perpX * lateralOffset).coerceIn(0.06f, 0.94f)
        val y = (a.yFraction + dy * localT + perpY * lateralOffset).coerceIn(0.06f, 0.94f)
        val lat = a.latitude + dLat * localT + latShift
        val lng = a.longitude + dLng * localT + lngShift

        return MapPosition(p, x, y, lat, lng, speedKmh)
    }

    private fun landmarkForProgress(progress: Float): String {
        return when {
            progress < 0.30f -> "Tol Jagorawi KM 39"
            progress < 0.50f -> "Rest Area Ciawi KM 42"
            progress < 0.70f -> "Simpang Gadog KM 45"
            else -> "Jalur Puncak KM 49"
        }
    }

    fun selectTripHistory(tripId: String) {
        _uiState.update { it.copy(selectedTripId = tripId) }
    }

    fun consumePendingAutoFinishNavigation(): String? {
        val pending = _uiState.value.pendingAutoFinishTripId ?: return null
        _uiState.update { it.copy(pendingAutoFinishTripId = null) }
        return pending
    }

    fun clearAutoFinishNotice() {
        _uiState.update { it.copy(autoFinishedTripNotice = null) }
    }

    fun finishAndSaveCurrentTrip(autoEndedByLeader: Boolean = false): TripHistoryRecord? {
        roomUnsubscribe?.invoke()
        roomUnsubscribe = null
        voiceTimerJob?.cancel()
        releaseVoiceRecorder(deleteFile = true)
        stopVoicePlayback()
        val state = _uiState.value
        val room = state.activeRoom ?: return null
        val route = state.routeConfig
        val role = state.selectedRole ?: UserRole.MEMBER

        val activeSpeeds = room.members.map { it.mapPosition.speedKmh }.filter { it > 0 }
        val avgSpeed = if (activeSpeeds.isNotEmpty()) activeSpeeds.average().roundToInt() else 52
        val maxSpeed = (activeSpeeds.maxOrNull() ?: 64) + 12

        val snapshotTrack = recordedTrackPoints.toList()
        recordedTrackPoints.clear()
        lastComputedRouteSig = ""
        val compactedTrack = if (snapshotTrack.size >= 2) {
            GeoUtils.downsampleTrackPoints(snapshotTrack, 15.0, 140)
        } else {
            emptyList()
        }
        val recordedLengthMeters = if (compactedTrack.size >= 2) {
            GeoUtils.computePolylineLengthMeters(compactedTrack)
        } else {
            0
        }
        val useRecordedTrack = compactedTrack.size >= 2 && recordedLengthMeters >= 50
        val savedWaypoints = if (useRecordedTrack) {
            GeoUtils.toRoutePoints(compactedTrack)
        } else {
            route.waypoints
        }

        val distanceKm = if (useRecordedTrack) {
            (recordedLengthMeters / 1000.0).coerceAtLeast(0.1)
        } else {
            val polyMeters = GeoUtils.computePolylineLengthMeters(
                route.waypoints.map { LatLngGeo(it.latitude, it.longitude) }
            )
            ((polyMeters.takeIf { it >= 100 } ?: route.totalRouteMeters) / 1000.0).coerceAtLeast(0.5)
        }

        val estMinutes = ((distanceKm / avgSpeed.coerceAtLeast(25)) * 60.0)
            .roundToInt()
            .coerceAtLeast(15)
        val hours = estMinutes / 60
        val mins = estMinutes % 60
        val durationLabel = if (hours > 0) "${hours}j ${mins}m" else "$mins mnt"

        val nowLabel = currentClockLabel()
        val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())

        val checkpointLogs = route.checkpoints.mapIndexed { idx, cp ->
            VisitedCheckpointLog(
                id = cp.id,
                name = cp.name,
                arrivedAtLabel = "Check #${idx + 1}",
                xFraction = cp.xFraction,
                yFraction = cp.yFraction,
                latitude = cp.latitude,
                longitude = cp.longitude
            )
        }

        val participants = room.members.map { m ->
            TripParticipantSummary(
                id = m.id,
                fullName = m.fullName,
                vehicleType = m.vehicleType,
                vehicleDetail = m.vehicleDetail,
                licensePlate = m.licensePlate,
                role = m.role,
                isSweeper = m.isSweeper,
                avatarPreset = m.avatarPreset,
                avatarPhotoUri = m.avatarPhotoUri
            )
        }

        val record = TripHistoryRecord(
            id = "trip-${UUID.randomUUID()}",
            roomId = room.roomId,
            tourTitle = room.roomTitle,
            dateLabel = dateLabel,
            timeRangeLabel = "Selesai $nowLabel",
            durationLabel = durationLabel,
            durationMinutes = estMinutes,
            distanceKm = distanceKm,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            userRole = role,
            destinationName = route.destinationName,
            destinationAddress = route.destinationAddress,
            waypoints = savedWaypoints,
            visitedCheckpoints = checkpointLogs,
            participants = participants
        )

        _uiState.update {
            it.copy(
                tripHistory = listOf(record) + it.tripHistory,
                selectedTripId = record.id,
                lastCompletedTrip = record,
                activeRoom = null,
                sosState = SosEmergencyState(),
                isOffRouteSimActive = false,
                isStragglerSimActive = false,
                isLeaderTroubleSimActive = false,
                pendingAutoFinishTripId = if (autoEndedByLeader) record.id else null,
                autoFinishedTripNotice = if (autoEndedByLeader) {
                    "Tour Leader telah mengakhiri konvoi. Perjalanan Anda otomatis selesai & tersimpan ke riwayat."
                } else {
                    null
                }
            )
        }
        if (syncRepo.isAvailable()) {
            viewModelScope.launch {
                val uid = syncRepo.currentUid()
                if (uid != null) {
                    val effective = room.leaderUid == uid || room.actingLeaderUid == uid
                    if (!autoEndedByLeader && room.isSyncedRoom && effective) {
                        syncRepo.finishRoomIfLeader(room.roomId, uid)
                    }
                    syncRepo.saveTripRecord(uid, record)
                }
            }
        }
        pushPresenceSnapshotNow()
        return record
    }

    fun repeatCompletedTrip(trip: TripHistoryRecord, onReady: () -> Unit = {}) {
        val state = _uiState.value
        if (!state.globalConfig.allowNewRooms && !syncRepo.isVerifiedAdminSession()) {
            _uiState.update {
                it.copy(authError = "Pembuatan room baru sedang dijeda sementara oleh Admin Backend.")
            }
            return
        }
        val restoredWaypoints = if (trip.waypoints.size >= 2) {
            trip.waypoints
        } else {
            state.routeConfig.waypoints
        }
        val restoredCheckpoints = trip.visitedCheckpoints.map { cp ->
            RestCheckpoint(
                id = cp.id.ifBlank { "cp-${UUID.randomUUID()}" },
                name = cp.name,
                xFraction = cp.xFraction,
                yFraction = cp.yFraction,
                latitude = cp.latitude,
                longitude = cp.longitude
            )
        }
        val restoredConfig = state.routeConfig.copy(
            destinationName = trip.destinationName.ifBlank { state.routeConfig.destinationName },
            destinationAddress = trip.destinationAddress.ifBlank { state.routeConfig.destinationAddress },
            waypoints = restoredWaypoints,
            checkpoints = restoredCheckpoints,
            totalRouteMeters = (trip.distanceKm * 1000.0).roundToInt().coerceAtLeast(1000)
        )
        _uiState.update {
            it.copy(
                selectedRole = UserRole.TOUR_LEADER,
                routeConfig = restoredConfig,
                lastCompletedTrip = null,
                sosState = SosEmergencyState(),
                isOffRouteSimActive = false,
                isStragglerSimActive = false
            )
        }
        createNewTourRoom(customTitle = trip.tourTitle.ifBlank { "Konvoi Touring Akhir Pekan" })
        onReady()
    }

    private fun defaultTripHistory(): List<TripHistoryRecord> = listOf(
        TripHistoryRecord(
            id = "trip-seed-1",
            roomId = "KNV-7391",
            tourTitle = "Touring Puncak Sunrise",
            dateLabel = "08 Nov 2026",
            timeRangeLabel = "05:30 - 08:15",
            durationLabel = "2j 45m",
            durationMinutes = 165,
            distanceKm = 68.4,
            avgSpeedKmh = 54,
            maxSpeedKmh = 82,
            userRole = UserRole.TOUR_LEADER,
            destinationName = "Puncak Pass Resort",
            destinationAddress = "Jl. Raya Puncak KM 90, Cisarua · -6.7003, 106.9960",
            waypoints = GeoUtils.toRoutePoints(
                listOf(
                    LatLngGeo(-6.5976, 106.8060),
                    LatLngGeo(-6.6124, 106.8185),
                    LatLngGeo(-6.6285, 106.8322),
                    LatLngGeo(-6.6458, 106.8452),
                    LatLngGeo(-6.6529, 106.8618),
                    LatLngGeo(-6.6562, 106.8794),
                    LatLngGeo(-6.6638, 106.8942),
                    LatLngGeo(-6.6705, 106.9088),
                    LatLngGeo(-6.6746, 106.9245),
                    LatLngGeo(-6.6818, 106.9372),
                    LatLngGeo(-6.6852, 106.9488),
                    LatLngGeo(-6.6914, 106.9605),
                    LatLngGeo(-6.6948, 106.9735),
                    LatLngGeo(-6.6981, 106.9852),
                    LatLngGeo(-6.7003, 106.9960)
                )
            ),
            visitedCheckpoints = listOf(
                VisitedCheckpointLog("cp-s1", "Rest Area KM 57", "06:20", 0.36f, 0.62f, -6.6638, 106.8942),
                VisitedCheckpointLog("cp-s2", "Warung Kopi Cisarua", "07:15", 0.66f, 0.36f, -6.6852, 106.9488)
            ),
            participants = listOf(
                TripParticipantSummary("p1", "Anda (Leader)", VehicleType.MOBIL, "Avanza Hitam", "B 1001 LDR", UserRole.TOUR_LEADER, avatarPreset = AvatarPreset.RIDER_CAPTAIN),
                TripParticipantSummary("p2", "Siti", VehicleType.MOTOR, "Nmax Putih", "D 4821 XZT", UserRole.MEMBER, isSweeper = true, avatarPreset = AvatarPreset.HELMET_TOURING),
                TripParticipantSummary("p3", "Raka", VehicleType.MOTOR, "Vario 160 Merah", "B 6190 TRG", UserRole.MEMBER, avatarPreset = AvatarPreset.HELMET_SPORT),
                TripParticipantSummary("p4", "Nadia", VehicleType.MOBIL, "HR-V Abu-abu", "B 2048 KNV", UserRole.MEMBER, avatarPreset = AvatarPreset.PILOT_CLASSIC)
            )
        ),
        TripHistoryRecord(
            id = "trip-seed-2",
            roomId = "KNV-4120",
            tourTitle = "Lembang Highland Weekend",
            dateLabel = "01 Nov 2026",
            timeRangeLabel = "07:00 - 10:20",
            durationLabel = "3j 20m",
            durationMinutes = 200,
            distanceKm = 94.2,
            avgSpeedKmh = 49,
            maxSpeedKmh = 78,
            userRole = UserRole.MEMBER,
            destinationName = "Lembang Highland",
            destinationAddress = "Jl. Raya Lembang No. 12, Bandung · -6.8120, 107.6180",
            waypoints = GeoUtils.toRoutePoints(
                listOf(
                    LatLngGeo(-6.8425, 107.4965),
                    LatLngGeo(-6.8548, 107.5152),
                    LatLngGeo(-6.8662, 107.5318),
                    LatLngGeo(-6.8745, 107.5515),
                    LatLngGeo(-6.8722, 107.5718),
                    LatLngGeo(-6.8638, 107.5892),
                    LatLngGeo(-6.8524, 107.5956),
                    LatLngGeo(-6.8398, 107.6012),
                    LatLngGeo(-6.8265, 107.6064),
                    LatLngGeo(-6.8178, 107.6115),
                    LatLngGeo(-6.8120, 107.6180)
                )
            ),
            visitedCheckpoints = listOf(
                VisitedCheckpointLog("cp-s3", "SPBU Padalarang KM 120", "08:05", 0.38f, 0.68f, -6.8650, 107.5200),
                VisitedCheckpointLog("cp-s4", "Rumah Makan Sate Pak Budi", "09:10", 0.52f, 0.44f, -6.8420, 107.5650)
            ),
            participants = listOf(
                TripParticipantSummary("p5", "Budi", VehicleType.MOBIL, "Pajero Sport", "B 1402 KNV", UserRole.TOUR_LEADER, avatarPreset = AvatarPreset.RALLY_PRO),
                TripParticipantSummary("p6", "Anda", VehicleType.MOTOR, "PCX 160 Hitam", "B 2026 AGT", UserRole.MEMBER, avatarPreset = AvatarPreset.HELMET_TOURING),
                TripParticipantSummary("p7", "Siti", VehicleType.MOTOR, "Nmax Putih", "D 4821 XZT", UserRole.MEMBER, isSweeper = true, avatarPreset = AvatarPreset.HELMET_SPORT)
            )
        )
    )

    private fun subscribeToGlobalConfig() {
        configUnsubscribe?.invoke()
        configUnsubscribe = syncRepo.subscribeToGlobalAppConfig { cfg ->
            _uiState.update { state ->
                val updatedRoute = if (state.activeRoom == null) {
                    state.routeConfig.copy(
                        offRouteToleranceMeters = cfg.defaultOffRouteToleranceMeters,
                        maxStragglerDistanceMeters = cfg.defaultMaxStragglerDistanceMeters,
                        soundAlertEnabled = cfg.defaultSoundAlertEnabled
                    )
                } else {
                    state.routeConfig
                }
                state.copy(
                    globalConfig = cfg,
                    routeConfig = updatedRoute
                )
            }
        }
    }

    private fun startPresenceHeartbeatLoop() {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            while (isActive) {
                pushPresenceSnapshotNow()
                delay(30_000L)
            }
        }
    }

    private fun pushPresenceSnapshotNow() {
        if (!syncRepo.isAvailable()) return
        val state = _uiState.value
        val uid = syncRepo.currentUid() ?: return
        if (!state.isLoggedIn && state.activeRoom == null) return
        val selfMember = state.activeRoom?.members?.firstOrNull { it.isCurrentUser }
        val speed = selfMember?.mapPosition?.speedKmh ?: 0
        val name = state.vehicleProfile.fullName.ifBlank { state.authUserName }.ifBlank { "Rider KonvoiKu" }
        val email = state.vehicleProfile.email.ifBlank { state.authUserEmail }
        syncRepo.publishUserPresence(
            uid = uid,
            fullName = name,
            email = email,
            isTraveling = state.activeRoom != null,
            activeRoomId = state.activeRoom?.roomId.orEmpty(),
            speedKmh = speed
        )
    }

    fun isAdminEmailSession(): Boolean {
        val state = _uiState.value
        val activeEmail = state.authUserEmail
            .ifBlank { state.vehicleProfile.email }
            .ifBlank { syncRepo.currentUserEmail() }
        return syncRepo.isAdminEmail(activeEmail)
    }

    fun isVerifiedAdminSession(): Boolean = syncRepo.isVerifiedAdminSession()

    fun startAdminMonitoring() {
        if (!syncRepo.isAvailable()) {
            _uiState.update {
                it.copy(
                    isAdminMonitoringLoading = false,
                    adminMonitorError = "Koneksi Firestore belum siap di perangkat. Periksa jaringan internet."
                )
            }
            return
        }
        if (!syncRepo.isVerifiedAdminSession()) {
            _uiState.update {
                it.copy(
                    isAdminMonitoringLoading = false,
                    isVerifiedAdmin = false,
                    adminMonitorError = "Masuk dengan akun Google terverifikasi ekie.ray@gmail.com untuk membaca monitoring realtime."
                )
            }
            return
        }
        adminMonitorUnsubscribe?.invoke()
        _uiState.update {
            it.copy(
                isVerifiedAdmin = true,
                isAdminMonitoringLoading = true,
                adminMonitorError = null
            )
        }
        adminMonitorUnsubscribe = syncRepo.subscribeToAdminMonitoring(
            onPresence = { list ->
                _uiState.update {
                    it.copy(
                        adminPresenceList = list,
                        isAdminMonitoringLoading = false
                    )
                }
            },
            onRooms = { rooms ->
                _uiState.update {
                    it.copy(
                        adminRooms = rooms,
                        isAdminMonitoringLoading = false
                    )
                }
            },
            onUsers = { users ->
                _uiState.update {
                    it.copy(
                        adminUsers = users,
                        isAdminMonitoringLoading = false
                    )
                }
            },
            onError = { msg ->
                _uiState.update {
                    it.copy(
                        isAdminMonitoringLoading = false,
                        adminMonitorError = msg
                    )
                }
            }
        )
    }

    fun stopAdminMonitoring() {
        adminMonitorUnsubscribe?.invoke()
        adminMonitorUnsubscribe = null
        _uiState.update { it.copy(isAdminMonitoringLoading = false) }
    }

    fun clearGlobalConfigFeedback() {
        _uiState.update { it.copy(globalConfigFeedback = null) }
    }

    fun saveGlobalConfig(config: GlobalAppConfig) {
        val uid = syncRepo.currentUid()
        if (uid == null || !syncRepo.isVerifiedAdminSession()) {
            _uiState.update {
                it.copy(globalConfigFeedback = "Akses ditolak: perlu akun admin ekie.ray@gmail.com terverifikasi.")
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGlobalConfig = true, globalConfigFeedback = null) }
            val result = syncRepo.saveGlobalAppConfig(uid, config)
            _uiState.update { state ->
                state.copy(
                    isSavingGlobalConfig = false,
                    globalConfigFeedback = if (result.isSuccess) {
                        "Pengaturan global berhasil disimpan ke backend & langsung diterapkan."
                    } else {
                        "Gagal menyimpan pengaturan: ${result.exceptionOrNull()?.message ?: "Coba lagi"}"
                    }
                )
            }
        }
    }

    fun adminToggleRoomStatus(roomId: String, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "ACTIVE") "FINISHED" else "ACTIVE"
            val result = syncRepo.adminSetRoomStatus(roomId, nextStatus)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(adminMonitorError = "Gagal ubah status room $roomId: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun adminResolveSos(roomId: String) {
        viewModelScope.launch {
            val result = syncRepo.adminResolveRoomSos(roomId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(adminMonitorError = "Gagal reset SOS room $roomId: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun adminRemoveRoomMember(roomId: String, memberUid: String) {
        viewModelScope.launch {
            val result = syncRepo.adminRemoveMember(roomId, memberUid)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(adminMonitorError = "Gagal mengeluarkan anggota: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    fun adminDeleteRoom(roomId: String) {
        viewModelScope.launch {
            val result = syncRepo.adminDeleteRoom(roomId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(adminMonitorError = "Gagal menghapus room $roomId: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }

    private fun generateUniqueRoomId(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "KNV-$suffix"
    }
}
