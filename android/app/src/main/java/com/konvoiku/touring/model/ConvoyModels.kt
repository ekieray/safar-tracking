package com.konvoiku.touring.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class UserRole(val label: String, val subtitle: String) {
    TOUR_LEADER(
        label = "Tour Leader",
        subtitle = "Buat room baru, bagikan link akses, dan kelola daftar anggota konvoi."
    ),
    MEMBER(
        label = "Anggota Tour",
        subtitle = "Gabung ke room konvoi menggunakan Room ID atau link undangan."
    )
}

enum class VehicleType(val label: String) {
    MOBIL("Mobil"),
    MOTOR("Motor")
}

enum class ConnectionStatus(val label: String) {
    CONNECTED("Terhubung"),
    WEAK_SIGNAL("Sinyal Lemah"),
    OFFLINE("Terputus")
}

enum class MemberConditionStatus(val label: String) {
    AMAN("Aman"),
    ISTIRAHAT("Istirahat / Rest Area"),
    MOGOK("Kendaraan Mogok"),
    SOS("Darurat SOS")
}

enum class ChatMessageType {
    TEXT,
    VOICE,
    SOS_ALERT,
    SYSTEM
}

enum class AvatarPreset(val label: String) {
    HELMET_TOURING("Helm Touring"),
    HELMET_SPORT("Helm Sport"),
    RIDER_CAPTAIN("Kapten Rider"),
    PILOT_CLASSIC("Pilot Klasik"),
    RALLY_PRO("Rally Pro"),
    STANDARD_USER("Foto Standar")
}

enum class AuthProviderType(val id: String, val label: String) {
    GOOGLE("google", "Google"),
    FACEBOOK("facebook", "Facebook"),
    INSTAGRAM("instagram", "Instagram"),
    EMAIL("email", "Email"),
    LOCAL("local", "Lokal");

    companion object {
        fun fromId(raw: String?): AuthProviderType =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: LOCAL
    }
}

data class RoomMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderVehicle: String,
    val type: ChatMessageType,
    val textContent: String,
    val timestampLabel: String,
    val voiceDurationSec: Int = 0,
    val voiceFilePath: String? = null,
    val voiceAudioDataUrl: String? = null,
    val isFromCurrentUser: Boolean = false,
    val senderAvatarPreset: AvatarPreset = AvatarPreset.HELMET_TOURING,
    val senderAvatarPhotoUri: String? = null
)

data class SosEmergencyState(
    val isActive: Boolean = false,
    val senderId: String = "",
    val senderName: String = "",
    val senderVehicle: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationText: String = "",
    val triggeredAtLabel: String = ""
)

enum class RoutePreference(val id: String, val label: String, val subtitle: String) {
    FASTEST("fastest", "Tercepat", "Hindari macet & rute utama"),
    SHORTEST("shortest", "Terpendek", "Jarak tempuh paling singkat"),
    NO_TOLL("no_toll", "Bebas Tol", "Cocok motor & jalur non-tol");

    companion object {
        fun fromId(raw: String?): RoutePreference =
            entries.firstOrNull { it.id.equals(raw?.trim(), ignoreCase = true) } ?: FASTEST
    }
}

data class RouteInstruction(
    val latitude: Double,
    val longitude: Double,
    val routeOffsetInMeters: Int,
    val maneuver: String,
    val message: String,
    val street: String = ""
)

data class NavigationGuidanceUiState(
    val instructionText: String,
    val streetName: String,
    val maneuver: String,
    val distanceToTurnLabel: String,
    val distanceToTurnMeters: Int,
    val headingDegrees: Int,
    val speedKmh: Int,
    val isOffRoute: Boolean
)

data class RoutePoint(
    val xFraction: Float,
    val yFraction: Float,
    val latitude: Double,
    val longitude: Double
)

data class RestCheckpoint(
    val id: String,
    val name: String,
    val xFraction: Float,
    val yFraction: Float,
    val latitude: Double,
    val longitude: Double
)

data class ConvoyRouteConfig(
    val originName: String = "Simpang Gadog / Tol Jagorawi",
    val originAddress: String = "Tol Jagorawi KM 45, Ciawi · -6.7100, 106.8500",
    val originLockedByLeader: Boolean = false,
    val destinationName: String = "Puncak Pass Resort",
    val destinationAddress: String = "Jl. Raya Puncak KM 90, Cisarua · -6.6450, 106.9900",
    val routePreference: RoutePreference = RoutePreference.FASTEST,
    val waypoints: List<RoutePoint> = defaultWaypoints(),
    val routeInstructions: List<RouteInstruction> = emptyList(),
    val checkpoints: List<RestCheckpoint> = defaultCheckpoints(),
    val totalRouteMeters: Int = 12000,
    val offRouteToleranceMeters: Int = 50,
    val maxStragglerDistanceMeters: Int = 1000,
    val soundAlertEnabled: Boolean = true
) {
    companion object {
        fun defaultWaypoints(): List<RoutePoint> = listOf(
            RoutePoint(0.14f, 0.86f, -6.7100, 106.8500),
            RoutePoint(0.32f, 0.65f, -6.6920, 106.8850),
            RoutePoint(0.55f, 0.48f, -6.6740, 106.9250),
            RoutePoint(0.72f, 0.30f, -6.6580, 106.9600),
            RoutePoint(0.86f, 0.14f, -6.6450, 106.9900)
        )

        fun defaultCheckpoints(): List<RestCheckpoint> = emptyList()
    }
}

data class StragglerAlertState(
    val isTriggered: Boolean = false,
    val trailingMember: ConvoyMember? = null,
    val aheadMember: ConvoyMember? = null,
    val gapMeters: Int = 0,
    val maxAllowedMeters: Int = 1000
)

data class OffRouteAlertState(
    val isOffRoute: Boolean = false,
    val deviationMeters: Int = 0,
    val toleranceMeters: Int = 50
)

data class MapPosition(
    val routeProgress: Float,
    val xFraction: Float,
    val yFraction: Float,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Int,
    val heading: Int = 0,
    val accuracyMeters: Int = 20
)

enum class GpsTrackingStatus(val label: String) {
    IDLE("GPS Siaga"),
    REQUESTING("Mencari GPS..."),
    ACTIVE("GPS Aktif"),
    DENIED("Izin GPS Ditolak"),
    UNAVAILABLE("GPS Tidak Tersedia")
}

data class VehicleProfile(
    val fullName: String = "",
    val email: String = "",
    val authProvider: AuthProviderType = AuthProviderType.LOCAL,
    val clubName: String = "",
    val homeCity: String = "",
    val bio: String = "",
    val instagramHandle: String = "",
    val vehicleType: VehicleType = VehicleType.MOBIL,
    val vehicleDetail: String = "",
    val licensePlate: String = "",
    val emergencyContact: String = "",
    val avatarPreset: AvatarPreset = AvatarPreset.HELMET_TOURING,
    val avatarPhotoUri: String? = null
) {
    val isValid: Boolean
        get() = fullName.trim().length >= 2 &&
            licensePlate.trim().length >= 3 &&
            emergencyContact.trim().length >= 8

    val displayVehicleLabel: String
        get() = vehicleDetail.trim().ifBlank { vehicleType.label }
}

data class ConvoyMember(
    val id: String,
    val fullName: String,
    val vehicleType: VehicleType,
    val vehicleDetail: String,
    val licensePlate: String,
    val emergencyContact: String,
    val role: UserRole,
    val joinedAtLabel: String,
    val isCurrentUser: Boolean = false,
    val isSweeper: Boolean = false,
    val isAutoSweeper: Boolean = false,
    val isCoLeader: Boolean = false,
    val isActingLeader: Boolean = false,
    val distanceToLeaderMeters: Int? = null,
    val lastUpdatedEpochMs: Long = 0L,
    val avatarPreset: AvatarPreset = AvatarPreset.HELMET_TOURING,
    val avatarPhotoUri: String? = null,
    val conditionStatus: MemberConditionStatus = MemberConditionStatus.AMAN,
    val connectionStatus: ConnectionStatus = ConnectionStatus.CONNECTED,
    val lastLocationText: String = "Tol Jagorawi KM 39",
    val lastUpdatedLabel: String = "Baru saja",
    val routeDistanceMeters: Int = 5000,
    val offRouteMeters: Int = 0,
    val convoyOrderRank: Int = 1,
    val mapPosition: MapPosition = MapPosition(
        routeProgress = 0.5f,
        xFraction = 0.5f,
        yFraction = 0.5f,
        latitude = -6.6740,
        longitude = 106.9250,
        speedKmh = 54
    )
) {
    val markerTitle: String
        get() = "$fullName - ${vehicleDetail.ifBlank { vehicleType.label }}"
}

data class DestinationEtaInfo(
    val remainingMeters: Int,
    val speedKmh: Int,
    val etaMinutes: Int?,
    val isStopped: Boolean,
    val isArrived: Boolean,
    val durationLabel: String,
    val arrivalClockLabel: String,
    val distanceLabel: String
)

fun calculateDestinationEta(
    member: ConvoyMember?,
    routeConfig: ConvoyRouteConfig,
    nowMillis: Long = System.currentTimeMillis()
): DestinationEtaInfo {
    val progress = (member?.mapPosition?.routeProgress ?: 0f).coerceIn(0f, 1f)
    val traveledMeters = (progress * routeConfig.totalRouteMeters).roundToInt()
    val remainingMeters = (routeConfig.totalRouteMeters - traveledMeters).coerceAtLeast(0)
    val speedKmh = (member?.mapPosition?.speedKmh ?: 0).coerceAtLeast(0)
    val distanceLabel = if (remainingMeters >= 1000) {
        String.format(Locale.US, "%.1f km", remainingMeters / 1000.0)
    } else {
        "$remainingMeters m"
    }
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    if (remainingMeters <= 50) {
        return DestinationEtaInfo(
            remainingMeters = 0,
            speedKmh = speedKmh,
            etaMinutes = 0,
            isStopped = false,
            isArrived = true,
            durationLabel = "Tiba",
            arrivalClockLabel = formatter.format(Date(nowMillis)),
            distanceLabel = "0 m"
        )
    }

    fun formatDuration(mins: Int): String = if (mins >= 60) {
        val hours = mins / 60
        val rem = mins % 60
        if (rem > 0) "${hours}j ${rem}m" else "$hours jam"
    } else {
        "$mins mnt"
    }

    if (speedKmh <= 0) {
        val estMinutes = ((remainingMeters / 1000.0 / 45.0) * 60.0)
            .roundToInt()
            .coerceAtLeast(1)
        val estClock = "~${formatter.format(Date(nowMillis + estMinutes * 60_000L))}"
        return DestinationEtaInfo(
            remainingMeters = remainingMeters,
            speedKmh = 0,
            etaMinutes = estMinutes,
            isStopped = true,
            isArrived = false,
            durationLabel = "Est. ${formatDuration(estMinutes)} · Diam",
            arrivalClockLabel = estClock,
            distanceLabel = distanceLabel
        )
    }

    val etaMinutes = ((remainingMeters / 1000.0 / speedKmh.toDouble()) * 60.0)
        .roundToInt()
        .coerceAtLeast(1)

    val durationLabel = formatDuration(etaMinutes)

    val arrivalClockLabel = formatter.format(Date(nowMillis + etaMinutes * 60_000L))

    return DestinationEtaInfo(
        remainingMeters = remainingMeters,
        speedKmh = speedKmh,
        etaMinutes = etaMinutes,
        isStopped = false,
        isArrived = false,
        durationLabel = durationLabel,
        arrivalClockLabel = arrivalClockLabel,
        distanceLabel = distanceLabel
    )
}

data class TripParticipantSummary(
    val id: String,
    val fullName: String,
    val vehicleType: VehicleType,
    val vehicleDetail: String,
    val licensePlate: String,
    val role: UserRole,
    val isSweeper: Boolean = false,
    val avatarPreset: AvatarPreset = AvatarPreset.HELMET_TOURING,
    val avatarPhotoUri: String? = null
)

data class VisitedCheckpointLog(
    val id: String,
    val name: String,
    val arrivedAtLabel: String,
    val xFraction: Float,
    val yFraction: Float,
    val latitude: Double,
    val longitude: Double
)

data class TripHistoryRecord(
    val id: String,
    val roomId: String,
    val tourTitle: String,
    val dateLabel: String,
    val timeRangeLabel: String,
    val durationLabel: String,
    val durationMinutes: Int,
    val distanceKm: Double,
    val avgSpeedKmh: Int,
    val maxSpeedKmh: Int,
    val userRole: UserRole,
    val destinationName: String,
    val destinationAddress: String,
    val waypoints: List<RoutePoint>,
    val visitedCheckpoints: List<VisitedCheckpointLog>,
    val participants: List<TripParticipantSummary>
)

enum class TripCardTheme(val label: String, val subtitle: String) {
    DARK_CARBON("Mode Gelap", "Karbon kontras tinggi"),
    SATELLITE("Peta Satelit", "Topografi & citra satelit"),
    MINIMAL_LIGHT("Minimalis", "Putih bersih editorial"),
    CUSTOM_PHOTO("Foto Galeri", "Latar foto perjalanan")
}

fun buildTripGpxXml(trip: TripHistoryRecord): String {
    val sb = StringBuilder()
    sb.appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
    sb.appendLine("""<gpx version="1.1" creator="KonvoiKu Touring App" xmlns="http://www.topografix.com/GPX/1/1">""")
    sb.appendLine("  <metadata>")
    sb.appendLine("    <name>${escapeXml(trip.tourTitle)} (${trip.roomId})</name>")
    sb.appendLine("    <desc>Konvoi Touring menuju ${escapeXml(trip.destinationName)} · ${trip.distanceKm} km · Rata-rata ${trip.avgSpeedKmh} km/j</desc>")
    sb.appendLine("  </metadata>")

    trip.waypoints.firstOrNull()?.let { start ->
        sb.appendLine("""  <wpt lat="${String.format(Locale.US, "%.6f", start.latitude)}" lon="${String.format(Locale.US, "%.6f", start.longitude)}">""")
        sb.appendLine("    <name>Titik Mulai Konvoi</name>")
        sb.appendLine("    <type>Start</type>")
        sb.appendLine("  </wpt>")
    }

    trip.visitedCheckpoints.forEach { cp ->
        sb.appendLine("""  <wpt lat="${String.format(Locale.US, "%.6f", cp.latitude)}" lon="${String.format(Locale.US, "%.6f", cp.longitude)}">""")
        sb.appendLine("    <name>${escapeXml(cp.name)}</name>")
        sb.appendLine("    <desc>Titik Istirahat · Tiba ${cp.arrivedAtLabel}</desc>")
        sb.appendLine("    <type>Rest Area</type>")
        sb.appendLine("  </wpt>")
    }

    trip.waypoints.lastOrNull()?.let { end ->
        sb.appendLine("""  <wpt lat="${String.format(Locale.US, "%.6f", end.latitude)}" lon="${String.format(Locale.US, "%.6f", end.longitude)}">""")
        sb.appendLine("    <name>Tujuan: ${escapeXml(trip.destinationName)}</name>")
        sb.appendLine("    <desc>${escapeXml(trip.destinationAddress)}</desc>")
        sb.appendLine("    <type>Destination</type>")
        sb.appendLine("  </wpt>")
    }

    sb.appendLine("  <trk>")
    sb.appendLine("    <name>${escapeXml(trip.tourTitle)}</name>")
    sb.appendLine("    <trkseg>")
    trip.waypoints.forEach { pt ->
        sb.appendLine("""      <trkpt lat="${String.format(Locale.US, "%.6f", pt.latitude)}" lon="${String.format(Locale.US, "%.6f", pt.longitude)}"/>""")
    }
    sb.appendLine("    </trkseg>")
    sb.appendLine("  </trk>")
    sb.appendLine("</gpx>")
    return sb.toString()
}

fun buildTripJsonLog(trip: TripHistoryRecord): String {
    val participantsJson = trip.participants.joinToString(",\n    ") { p ->
        """{"name":"${escapeJson(p.fullName)}","role":"${p.role.label}","vehicle":"${escapeJson(p.vehicleDetail)}","plate":"${escapeJson(p.licensePlate)}","sweeper":${p.isSweeper}}"""
    }
    val checkpointsJson = trip.visitedCheckpoints.joinToString(",\n    ") { cp ->
        """{"name":"${escapeJson(cp.name)}","arrivedAt":"${escapeJson(cp.arrivedAtLabel)}","lat":${String.format(Locale.US, "%.6f", cp.latitude)},"lng":${String.format(Locale.US, "%.6f", cp.longitude)}}"""
    }
    val trackJson = trip.waypoints.joinToString(",\n    ") { pt ->
        """{"lat":${String.format(Locale.US, "%.6f", pt.latitude)},"lng":${String.format(Locale.US, "%.6f", pt.longitude)}}"""
    }
    return """
{
  "app": "KonvoiKu",
  "roomId": "${escapeJson(trip.roomId)}",
  "tourTitle": "${escapeJson(trip.tourTitle)}",
  "date": "${escapeJson(trip.dateLabel)}",
  "timeRange": "${escapeJson(trip.timeRangeLabel)}",
  "duration": "${escapeJson(trip.durationLabel)}",
  "distanceKm": ${String.format(Locale.US, "%.1f", trip.distanceKm)},
  "avgSpeedKmh": ${trip.avgSpeedKmh},
  "maxSpeedKmh": ${trip.maxSpeedKmh},
  "destination": "${escapeJson(trip.destinationName)}",
  "userRole": "${trip.userRole.label}",
  "participants": [
    $participantsJson
  ],
  "checkpoints": [
    $checkpointsJson
  ],
  "trackPoints": [
    $trackJson
  ]
}
""".trimIndent()
}

fun buildTripShareSummaryText(trip: TripHistoryRecord): String {
    val cpList = if (trip.visitedCheckpoints.isEmpty()) {
        "Tidak ada perhentian khusus"
    } else {
        trip.visitedCheckpoints.joinToString(", ") { "${it.name} (${it.arrivedAtLabel})" }
    }
    val memberList = trip.participants.joinToString(", ") { it.fullName }
    return buildString {
        appendLine("🏁 REKAP TOUR KONVOIKU")
        appendLine("Tour: ${trip.tourTitle} (${trip.roomId})")
        appendLine("Tanggal: ${trip.dateLabel} (${trip.timeRangeLabel})")
        appendLine("Tujuan: ${trip.destinationName}")
        appendLine("Jarak Tempuh: ${String.format(Locale.US, "%.1f", trip.distanceKm)} km · Durasi: ${trip.durationLabel}")
        appendLine("Kecepatan Rata-rata: ${trip.avgSpeedKmh} km/j (Maks ${trip.maxSpeedKmh} km/j)")
        appendLine("Titik Istirahat: $cpList")
        appendLine("Peserta (${trip.participants.size}): $memberList")
    }.trim()
}

private fun escapeXml(input: String): String =
    input.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

private fun escapeJson(input: String): String =
    input.replace("\\", "\\\\").replace("\"", "\\\"")

data class GlobalAppConfig(
    val configId: String = "global",
    val defaultOffRouteToleranceMeters: Int = 75,
    val defaultMaxStragglerDistanceMeters: Int = 1000,
    val defaultSoundAlertEnabled: Boolean = true,
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Aplikasi sedang dalam pemeliharaan berkala. Mohon tunggu beberapa saat.",
    val announcementActive: Boolean = false,
    val announcementTitle: String = "Info KonvoiKu",
    val announcementMessage: String = "",
    val allowGuestMode: Boolean = true,
    val allowNewRooms: Boolean = true,
    val updatedAtLabel: String = ""
)

data class UserPresenceInfo(
    val uid: String,
    val fullName: String,
    val email: String,
    val platform: String,
    val isTraveling: Boolean,
    val activeRoomId: String,
    val speedKmh: Int,
    val lastSeenMs: Long,
    val lastSeenLabel: String
)

data class AdminUserSummary(
    val uid: String,
    val fullName: String,
    val email: String,
    val authProvider: AuthProviderType,
    val clubName: String,
    val homeCity: String,
    val vehicleType: VehicleType,
    val vehicleDetail: String,
    val licensePlate: String,
    val avatarPreset: AvatarPreset,
    val avatarPhotoUri: String? = null,
    val updatedAtLabel: String = "-"
)

data class AdminRoomOverview(
    val roomId: String,
    val roomTitle: String,
    val leaderUid: String,
    val destinationName: String,
    val status: String,
    val sosActive: Boolean,
    val sosSenderName: String,
    val offRouteToleranceMeters: Int,
    val maxStragglerDistanceMeters: Int,
    val createdAtLabel: String,
    val updatedAtMs: Long,
    val updatedAtLabel: String,
    val members: List<ConvoyMember> = emptyList()
)

data class TourRoom(
    val roomId: String,
    val roomTitle: String,
    val inviteLink: String,
    val createdAtLabel: String,
    val members: List<ConvoyMember>,
    val isSyncedRoom: Boolean = false,
    val leaderUid: String = "",
    val coLeaderUid: String = "",
    val actingLeaderUid: String = "",
    val leaderTroubleActive: Boolean = false,
    val leaderTroubleReason: String = ""
)

data class ConvoyUiState(
    val selectedRole: UserRole? = null,
    val vehicleProfile: VehicleProfile = VehicleProfile(),
    val activeRoom: TourRoom? = null,
    val joinInput: String = "",
    val joinError: String? = null,
    val selectedMemberId: String? = null,
    val isSimulationRunning: Boolean = true,
    val routeConfig: ConvoyRouteConfig = ConvoyRouteConfig(),
    val stragglerAlert: StragglerAlertState = StragglerAlertState(),
    val offRouteAlert: OffRouteAlertState = OffRouteAlertState(),
    val sosState: SosEmergencyState = SosEmergencyState(),
    val messages: List<RoomMessage> = emptyList(),
    val isRecordingVoice: Boolean = false,
    val recordingSeconds: Int = 0,
    val playingMessageId: String? = null,
    val voiceStatusMessage: String? = null,
    val isPickingDestinationOnMap: Boolean = false,
    val isPickingCheckpointOnMap: Boolean = false,
    val pendingCheckpointName: String = "",
    val isOffRouteSimActive: Boolean = false,
    val isStragglerSimActive: Boolean = false,
    val isLeaderTroubleSimActive: Boolean = false,
    val tripHistory: List<TripHistoryRecord> = emptyList(),
    val selectedTripId: String? = null,
    val lastCompletedTrip: TripHistoryRecord? = null,
    val gpsStatus: GpsTrackingStatus = GpsTrackingStatus.IDLE,
    val gpsAccuracyMeters: Int = 20,
    val userGpsLat: Double? = null,
    val userGpsLng: Double? = null,
    val isCloudSyncConnected: Boolean = false,
    val isLoggedIn: Boolean = false,
    val authUserName: String = "",
    val authUserEmail: String = "",
    val authProvider: AuthProviderType = AuthProviderType.LOCAL,
    val isGuestMode: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val authInfoMessage: String? = null,
    val showGoogleBrowserFallback: Boolean = false,
    val showOwnerDevPreviewFallback: Boolean = false,
    val isVerifiedAdmin: Boolean = false,
    val isSavingProfile: Boolean = false,
    val profileSyncMessage: String? = null,
    val globalConfig: GlobalAppConfig = GlobalAppConfig(),
    val adminPresenceList: List<UserPresenceInfo> = emptyList(),
    val adminRooms: List<AdminRoomOverview> = emptyList(),
    val adminUsers: List<AdminUserSummary> = emptyList(),
    val isAdminMonitoringLoading: Boolean = false,
    val adminMonitorError: String? = null,
    val isSavingGlobalConfig: Boolean = false,
    val globalConfigFeedback: String? = null,
    val pendingInviteRoomId: String? = null,
    val roomSetupError: String? = null,
    val isCreatingRoom: Boolean = false,
    val isJoiningRoom: Boolean = false,
    val pendingAutoFinishTripId: String? = null,
    val autoFinishedTripNotice: String? = null,
    val activeWebBaseUrl: String = "https://ais-pre-mbwxrxf647cphhl5i4ggug-588877716211.asia-southeast1.run.app"
)
