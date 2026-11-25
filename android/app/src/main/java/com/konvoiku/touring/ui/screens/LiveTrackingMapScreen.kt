package com.konvoiku.touring.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.konvoiku.touring.data.TomTomApiService
import com.konvoiku.touring.data.TomTomPlaceResult
import com.konvoiku.touring.model.ChatMessageType
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.ConvoyRouteConfig
import com.konvoiku.touring.model.DestinationEtaInfo
import com.konvoiku.touring.model.GpsTrackingStatus
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.OffRouteAlertState
import com.konvoiku.touring.model.RoomMessage
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePoint
import com.konvoiku.touring.model.SosEmergencyState
import com.konvoiku.touring.model.StragglerAlertState
import com.konvoiku.touring.model.TourRoom
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.model.VehicleType
import com.konvoiku.touring.model.calculateDestinationEta
import com.konvoiku.touring.ui.components.ConvoyAvatar
import com.konvoiku.touring.ui.components.TomTomConvoyMap
import com.konvoiku.touring.ui.components.TomTomMapPin
import com.konvoiku.touring.ui.components.TomTomMapStyle
import com.konvoiku.touring.ui.components.rememberTomTomMapController
import com.konvoiku.touring.util.GeoUtils
import com.konvoiku.touring.util.LatLngGeo
import com.konvoiku.touring.viewmodel.ConvoyViewModel
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingMapScreen(
    activeRoom: TourRoom?,
    role: UserRole,
    selectedMemberId: String?,
    isSimulationRunning: Boolean,
    routeConfig: ConvoyRouteConfig,
    stragglerAlert: StragglerAlertState,
    offRouteAlert: OffRouteAlertState,
    sosState: SosEmergencyState,
    messages: List<RoomMessage>,
    isRecordingVoice: Boolean,
    recordingSeconds: Int,
    playingMessageId: String?,
    voiceStatusMessage: String? = null,
    isPickingDestinationOnMap: Boolean,
    isPickingCheckpointOnMap: Boolean,
    pendingCheckpointName: String,
    isOffRouteSimActive: Boolean,
    isStragglerSimActive: Boolean,
    canPingTomTomRouting: Boolean = role == UserRole.TOUR_LEADER,
    isEffectiveLeader: Boolean = role == UserRole.TOUR_LEADER,
    isLeaderTroubleSimActive: Boolean = false,
    gpsStatus: GpsTrackingStatus = GpsTrackingStatus.IDLE,
    gpsAccuracyMeters: Int = 20,
    activeWebBaseUrl: String = "https://ais-pre-mbwxrxf647cphhl5i4ggug-588877716211.asia-southeast1.run.app",
    destinationPresets: List<ConvoyViewModel.DestinationPreset>,
    onLocationPermissionResult: (Boolean) -> Unit = {},
    onRetryGps: () -> Unit = {},
    onSelectMember: (String) -> Unit,
    onToggleSimulation: () -> Unit,
    onSelectPreset: (ConvoyViewModel.DestinationPreset) -> Unit,
    onPickCustomDestinationLatLng: (Double, Double, String?, String?) -> Unit,
    onTogglePickMode: (Boolean) -> Unit,
    onStartPickCheckpoint: (String) -> Unit,
    onCancelPickCheckpoint: () -> Unit,
    onConfirmAddCheckpointLatLng: (String, Double, Double) -> Unit,
    onRemoveCheckpoint: (String) -> Unit,
    onUpdateMaxStragglerDistance: (Int) -> Unit,
    onUpdateOffRouteTolerance: (Int) -> Unit,
    onToggleSoundAlert: () -> Unit,
    onToggleOffRouteSim: () -> Unit,
    onToggleStragglerSim: () -> Unit,
    onToggleLeaderTroubleSim: () -> Unit = {},
    onReclaimLeader: () -> Unit = {},
    onTriggerSos: () -> Unit,
    onResolveSos: () -> Unit,
    onSendTextMessage: (String) -> Unit,
    onStartVoiceRecord: () -> Unit,
    onStopVoiceRecord: () -> Unit,
    onToggleVoicePlay: (String) -> Unit,
    onUpdateOwnCondition: (MemberConditionStatus) -> Unit,
    onAssignSweeper: (String) -> Unit,
    onFinishAndSaveTrip: () -> Unit,
    onExitRoom: () -> Unit,
    onRouteComputed: (Int, List<RoutePoint>, List<RouteInstruction>) -> Unit = { _, _, _ -> }
) {
    var mapStyle by remember { mutableStateOf(TomTomMapStyle.ROAD) }
    var showTraffic by remember { mutableStateOf(true) }
    var isNavigationMode by remember(activeRoom?.roomId) { mutableStateOf(true) }
    var isNavInfoHidden by remember(activeRoom?.roomId) { mutableStateOf(true) }
    var showSideInfoDrawer by remember(activeRoom?.roomId) { mutableStateOf(false) }
    var sideInfoTouchTick by remember { mutableStateOf(0) }
    var sideInfoSecondsLeft by remember { mutableStateOf(7) }
    var isParticipantsExpanded by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showLeaderSettingsSheet by remember { mutableStateOf(false) }
    var showShareRoomDialog by remember { mutableStateOf(false) }
    var copiedShareHint by remember { mutableStateOf<String?>(null) }
    var destSearchQuery by remember { mutableStateOf("") }
    var destSearchResults by remember { mutableStateOf<List<TomTomPlaceResult>>(emptyList()) }
    var isSearchingDest by remember { mutableStateOf(false) }
    var showSosConfirmDialog by remember { mutableStateOf(false) }
    var showFinishTripDialog by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    var dismissedSosKey by remember(activeRoom?.roomId) { mutableStateOf("") }
    var sosFlashSecondsLeft by remember(activeRoom?.roomId) { mutableStateOf(10) }

    val currentSosKey = remember(sosState.isActive, sosState.senderId, sosState.triggeredAtLabel) {
        if (sosState.isActive) "${sosState.senderId}|${sosState.triggeredAtLabel}" else ""
    }
    val showSosFlashOverlay = currentSosKey.isNotBlank() && currentSosKey != dismissedSosKey

    LaunchedEffect(currentSosKey, dismissedSosKey) {
        if (currentSosKey.isBlank()) {
            dismissedSosKey = ""
            sosFlashSecondsLeft = 10
            return@LaunchedEffect
        }
        if (currentSosKey == dismissedSosKey) return@LaunchedEffect
        sosFlashSecondsLeft = 10
        for (sec in 10 downTo 1) {
            sosFlashSecondsLeft = sec
            delay(1000L)
        }
        dismissedSosKey = currentSosKey
        sosFlashSecondsLeft = 0
    }

    val touchSideDrawer: () -> Unit = { sideInfoTouchTick += 1 }
    val openSideDrawer: () -> Unit = {
        showSideInfoDrawer = true
        sideInfoTouchTick += 1
    }

    LaunchedEffect(showLeaderSettingsSheet, destSearchQuery) {
        if (!showLeaderSettingsSheet) return@LaunchedEffect
        val q = destSearchQuery.trim()
        if (q.length < 2) {
            destSearchResults = emptyList()
            isSearchingDest = false
            return@LaunchedEffect
        }
        isSearchingDest = true
        delay(320)
        destSearchResults = TomTomApiService.searchPlaces(q)
        isSearchingDest = false
    }

    LaunchedEffect(showSideInfoDrawer, sideInfoTouchTick) {
        if (!showSideInfoDrawer) return@LaunchedEffect
        sideInfoSecondsLeft = 7
        for (sec in 7 downTo 1) {
            sideInfoSecondsLeft = sec
            delay(1000L)
        }
        showSideInfoDrawer = false
    }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasRequestedPermissionOnce by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!granted) {
            val activity = context as? Activity
            val canAskAgain = activity != null && (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            isPermanentlyDenied = hasRequestedPermissionOnce && !canAskAgain
        } else {
            isPermanentlyDenied = false
        }
        hasRequestedPermissionOnce = true
        onLocationPermissionResult(granted)
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onStartVoiceRecord()
    }

    val startVoiceWithPermission: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            onStartVoiceRecord()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(lifecycleOwner, gpsStatus) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && gpsStatus == GpsTrackingStatus.DENIED) {
                onRetryGps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        if (gpsStatus == GpsTrackingStatus.IDLE || gpsStatus == GpsTrackingStatus.DENIED) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val members = activeRoom?.members.orEmpty().sortedBy { it.convoyOrderRank }
    val focusedMember = members.find { it.id == selectedMemberId } ?: members.firstOrNull()
    val currentUser = members.find { it.isCurrentUser }
    val trackedMember = focusedMember ?: currentUser
    val etaInfo = remember(
        trackedMember?.mapPosition?.routeProgress,
        trackedMember?.mapPosition?.speedKmh,
        routeConfig.totalRouteMeters,
        routeConfig.destinationName
    ) {
        calculateDestinationEta(trackedMember, routeConfig)
    }

    val activeSpeeds = members
        .filter { it.conditionStatus != MemberConditionStatus.MOGOK && it.conditionStatus != MemberConditionStatus.SOS }
        .map { it.mapPosition.speedKmh.coerceAtLeast(0) }
    val convoyAvgSpeedKmh = if (activeSpeeds.isNotEmpty()) {
        activeSpeeds.average().roundToInt()
    } else 0

    var speedHistory by remember(activeRoom?.roomId) {
        val base = convoyAvgSpeedKmh.coerceAtLeast(0)
        val offsets = listOf(-4, -2, 1, -1, 3, 2, -2, 1, 2, 0)
        mutableStateOf(
            offsets.map { delta ->
                if (base == 0) 0 else (base + delta).coerceIn(12, 130)
            }
        )
    }
    LaunchedEffect(convoyAvgSpeedKmh) {
        speedHistory = (speedHistory.drop(1) + convoyAvgSpeedKmh).takeLast(10)
    }

    val initialLat = trackedMember?.mapPosition?.latitude ?: -6.674
    val initialLng = trackedMember?.mapPosition?.longitude ?: 106.925
    val mapController = rememberTomTomMapController()
    var isAutoFollowing by remember { mutableStateOf(true) }
    val activeNavMode = isNavigationMode && !isPickingDestinationOnMap
    val isCleanNavHud = activeNavMode && isNavInfoHidden

    val navSubject = currentUser ?: trackedMember
    val navGuidance = remember(
        navSubject?.mapPosition?.latitude,
        navSubject?.mapPosition?.longitude,
        navSubject?.mapPosition?.speedKmh,
        navSubject?.mapPosition?.heading,
        navSubject?.mapPosition?.routeProgress,
        navSubject?.offRouteMeters,
        routeConfig.waypoints,
        routeConfig.routeInstructions,
        routeConfig.offRouteToleranceMeters,
        routeConfig.totalRouteMeters
    ) {
        GeoUtils.computeNavigationGuidance(
            lat = navSubject?.mapPosition?.latitude ?: initialLat,
            lng = navSubject?.mapPosition?.longitude ?: initialLng,
            speedKmh = navSubject?.mapPosition?.speedKmh ?: 0,
            rawHeadingDeg = navSubject?.mapPosition?.heading ?: 0,
            waypoints = routeConfig.waypoints,
            instructions = routeConfig.routeInstructions,
            offRouteMeters = navSubject?.offRouteMeters ?: 0,
            toleranceMeters = routeConfig.offRouteToleranceMeters,
            totalRouteMeters = routeConfig.totalRouteMeters,
            routeProgress = navSubject?.mapPosition?.routeProgress ?: 0f
        )
    }

    var tomTomRoadPath by remember(routeConfig.waypoints) {
        mutableStateOf(routeConfig.waypoints)
    }

    val routeEndpointsKey = remember(
        routeConfig.destinationName,
        routeConfig.routePreference,
        routeConfig.waypoints,
        currentUser?.vehicleType
    ) {
        val first = routeConfig.waypoints.firstOrNull()
        val last = routeConfig.waypoints.lastOrNull()
        String.format(
            Locale.US,
            "%s|%s|%s|%.5f,%.5f->%.5f,%.5f",
            routeConfig.destinationName,
            routeConfig.routePreference.id,
            currentUser?.vehicleType?.name ?: "MOTOR",
            first?.latitude ?: 0.0,
            first?.longitude ?: 0.0,
            last?.latitude ?: 0.0,
            last?.longitude ?: 0.0
        )
    }

    LaunchedEffect(routeEndpointsKey, canPingTomTomRouting) {
        if (canPingTomTomRouting && routeConfig.waypoints.size >= 2) {
            val travelMode = if (currentUser?.vehicleType == VehicleType.MOBIL) "car" else "motorcycle"
            val res = TomTomApiService.calculateRoute(
                waypoints = routeConfig.waypoints,
                travelMode = travelMode,
                routePreference = routeConfig.routePreference
            )
            if (res != null && res.path.size >= 2) {
                val snapped = GeoUtils.toRoutePoints(
                    GeoUtils.downsampleLatLng(
                        res.path.map { LatLngGeo(it.latitude, it.longitude) },
                        maxPoints = 90
                    )
                )
                tomTomRoadPath = snapped
                onRouteComputed(res.distanceMeters, snapped, res.instructions)
            }
        }
    }

    LaunchedEffect(
        trackedMember?.id,
        trackedMember?.mapPosition?.latitude,
        trackedMember?.mapPosition?.longitude,
        activeNavMode,
        navGuidance.headingDegrees,
        isAutoFollowing
    ) {
        if (!isAutoFollowing) return@LaunchedEffect
        val m = (if (activeNavMode) navSubject else trackedMember) ?: return@LaunchedEffect
        val target = if (activeNavMode) {
            GeoUtils.offsetPointAlongHeading(
                lat = m.mapPosition.latitude,
                lng = m.mapPosition.longitude,
                headingDeg = navGuidance.headingDegrees.toDouble(),
                offsetMeters = 85.0
            )
        } else {
            LatLngGeo(m.mapPosition.latitude, m.mapPosition.longitude)
        }
        val targetZoom = if (activeNavMode) {
            mapController.currentZoom.coerceAtLeast(16.8)
        } else {
            mapController.currentZoom.coerceAtLeast(14.5)
        }
        mapController.animateTo(target.lat, target.lng, targetZoom)
    }

    LaunchedEffect(gpsStatus) {
        if (gpsStatus == GpsTrackingStatus.ACTIVE) {
            isAutoFollowing = true
        }
    }

    LaunchedEffect(stragglerAlert.isTriggered, offRouteAlert.isOffRoute, sosState.isActive, currentSosKey, routeConfig.soundAlertEnabled) {
        if (routeConfig.soundAlertEnabled && (stragglerAlert.isTriggered || offRouteAlert.isOffRoute || sosState.isActive)) {
            runCatching {
                val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${activeRoom?.roomId ?: "KNV-0000"} · Live",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = routeConfig.destinationName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onExitRoom) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Keluar Room"
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = {
                            val next = !isNavigationMode
                            isNavigationMode = next
                            if (next) {
                                isNavInfoHidden = true
                                isAutoFollowing = true
                                val m = navSubject
                                if (m != null) {
                                    val ahead = GeoUtils.offsetPointAlongHeading(
                                        m.mapPosition.latitude,
                                        m.mapPosition.longitude,
                                        navGuidance.headingDegrees.toDouble(),
                                        85.0
                                    )
                                    mapController.animateTo(ahead.lat, ahead.lng, 16.8)
                                }
                            } else {
                                mapController.currentZoom
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNavigationMode) Color(0xFFEA580C) else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (isNavigationMode) Color.White else MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            NavCompassArrowCanvasIcon(
                                tint = if (isNavigationMode) Color.White else Color(0xFFEA580C),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isNavigationMode) "Peta" else "Navigasi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (activeNavMode) {
                        IconButton(
                            onClick = { isNavInfoHidden = !isNavInfoHidden },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isNavInfoHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (isNavInfoHidden) "Tampilkan Info Lengkap" else "Sembunyikan Info Navigasi",
                                tint = if (isNavInfoHidden) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            copiedShareHint = null
                            showShareRoomDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Bagikan Link & Kode Room",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    TextButton(
                        onClick = { showFinishTripDialog = true },
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Text("Selesai", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    if (isEffectiveLeader) {
                        IconButton(
                            onClick = { showLeaderSettingsSheet = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Pengaturan Rute Leader",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Collapsible Participant Strip + Expanded Panel (Hidden when Clean Navigation HUD is active)
            if (!isCleanNavHud) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isParticipantsExpanded = !isParticipantsExpanded }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sweeperMember = members.firstOrNull { it.isSweeper }
                            val sweeperText = sweeperMember?.let {
                                " · Sweeper: ${it.fullName}${if (it.isAutoSweeper) " (Auto)" else ""}"
                            }.orEmpty()
                            Text(
                                text = "Peserta (${members.size})$sweeperText",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isParticipantsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = "Buka atau Tutup Daftar Peserta",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (!isParticipantsExpanded) {
                            LazyRow(
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(members, key = { it.id }) { member ->
                                    TopParticipantChip(
                                        member = member,
                                        isSelected = member.id == focusedMember?.id,
                                        onClick = {
                                            onSelectMember(member.id)
                                            isAutoFollowing = true
                                            mapController.animateTo(
                                                member.mapPosition.latitude,
                                                member.mapPosition.longitude,
                                                15.0
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isParticipantsExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        MemberConditionStatus.AMAN to "Aman",
                                        MemberConditionStatus.ISTIRAHAT to "Istirahat",
                                        MemberConditionStatus.MOGOK to "Mogok"
                                    ).forEach { (status, shortLabel) ->
                                        FilterChip(
                                            selected = currentUser?.conditionStatus == status,
                                            onClick = { onUpdateOwnCondition(status) },
                                            label = { Text(shortLabel, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.heightIn(max = 180.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(members, key = { it.id }) { member ->
                                        val memberEta = calculateDestinationEta(member, routeConfig)
                                        ParticipantExpandedRow(
                                            member = member,
                                            etaInfo = memberEta,
                                            viewerIsLeader = isEffectiveLeader,
                                            isSelected = member.id == focusedMember?.id,
                                            onAssignSweeper = { onAssignSweeper(member.id) },
                                            onClick = {
                                                onSelectMember(member.id)
                                                isAutoFollowing = true
                                                isParticipantsExpanded = false
                                                mapController.animateTo(
                                                    member.mapPosition.latitude,
                                                    member.mapPosition.longitude,
                                                    15.0
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Safety Alert Banners
            if (offRouteAlert.isOffRoute) {
                Surface(
                    color = Color(0xFFDC2626),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = "Keluar Jalur", modifier = Modifier.size(18.dp))
                        Text(
                            text = "KELUAR JALUR · Menyimpang ${offRouteAlert.deviationMeters} m dari rute resmi (batas ${offRouteAlert.toleranceMeters} m)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            if (stragglerAlert.isTriggered && stragglerAlert.trailingMember != null && stragglerAlert.aheadMember != null) {
                Surface(
                    color = Color(0xFFB45309),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            text = "TERTINGGAL ${formatDistanceLabel(stragglerAlert.gapMeters)} · ${stragglerAlert.trailingMember.fullName} di belakang ${stragglerAlert.aheadMember.fullName}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.weight(1f)
                        )
                        if (routeConfig.soundAlertEnabled) {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            if (activeRoom?.leaderTroubleActive == true) {
                val actingMember = members.firstOrNull {
                    it.isActingLeader || (activeRoom.actingLeaderUid.isNotBlank() && it.id == activeRoom.actingLeaderUid)
                }
                Surface(
                    color = Color(0xFF6D28D9),
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = "Failover Leader", modifier = Modifier.size(18.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "TAKEOVER OTOMATIS · Plt. Leader: ${actingMember?.fullName ?: "Co-Leader"}",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${activeRoom.leaderTroubleReason.ifBlank { "Leader gangguan sinyal" }} · Ping TomTom API dialihkan ke Plt. Leader.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        if (role == UserRole.TOUR_LEADER || !activeRoom.isSyncedRoom || currentUser?.id == activeRoom.leaderUid) {
                            TextButton(onClick = onReclaimLeader) {
                                Text("Pulihkan", color = Color(0xFFDDD6FE), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Real Live TomTom Maps Viewport (osmdroid + TomTom Tiles & Routing)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val waypoints = routeConfig.waypoints
                val destPoint = waypoints.lastOrNull()
                val mapPins = remember(
                    members,
                    focusedMember?.id,
                    routeConfig.checkpoints,
                    routeConfig.destinationName,
                    routeConfig.offRouteToleranceMeters,
                    etaInfo.arrivalClockLabel,
                    etaInfo.durationLabel,
                    etaInfo.distanceLabel,
                    sosState.isActive,
                    sosState.senderId,
                    stragglerAlert.isTriggered,
                    stragglerAlert.trailingMember?.id
                ) {
                    buildList<TomTomMapPin> {
                        if (destPoint != null) {
                            add(
                                TomTomMapPin.DestinationPin(
                                    latitude = destPoint.latitude,
                                    longitude = destPoint.longitude,
                                    title = "Tujuan: ${routeConfig.destinationName}",
                                    subtitle = "ETA ${etaInfo.arrivalClockLabel} · ${etaInfo.durationLabel} · ${etaInfo.distanceLabel}"
                                )
                            )
                        }
                        members.forEach { member ->
                            add(
                                TomTomMapPin.MemberPin(
                                    member = member,
                                    isSelected = member.id == focusedMember?.id,
                                    isOffRoute = member.offRouteMeters > routeConfig.offRouteToleranceMeters,
                                    isStraggler = stragglerAlert.isTriggered && stragglerAlert.trailingMember?.id == member.id,
                                    isSosBeacon = member.conditionStatus == MemberConditionStatus.SOS || (sosState.isActive && sosState.senderId == member.id)
                                )
                            )
                        }
                    }
                }

                TomTomConvoyMap(
                    modifier = Modifier.fillMaxSize(),
                    controller = mapController,
                    initialLat = initialLat,
                    initialLng = initialLng,
                    initialZoom = if (activeNavMode) 16.8 else 14.5,
                    mapStyle = mapStyle,
                    showTraffic = showTraffic,
                    isNavigationMode = activeNavMode,
                    navigationHeadingDeg = navGuidance.headingDegrees.toFloat(),
                    navigatedMemberId = navSubject?.id,
                    polylinePoints = tomTomRoadPath,
                    pins = mapPins,
                    onUserDrag = { isAutoFollowing = false },
                    onMemberPinClick = { memberId ->
                        onSelectMember(memberId)
                        isAutoFollowing = true
                    },
                    onMapTap = { lat, lng ->
                        if (isPickingDestinationOnMap) {
                            onPickCustomDestinationLatLng(lat, lng, null, null)
                        }
                    }
                )

                // Top Turn-by-Turn GPS Navigation HUD Banner
                if (activeNavMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 12.dp, end = 68.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (navGuidance.isOffRoute) Color(0xF0DC2626) else Color(0xF0065F46),
                        contentColor = Color.White,
                        border = BorderStroke(
                            1.dp,
                            if (navGuidance.isOffRoute) Color(0xFFFCA5A5) else Color(0xFF34D399).copy(alpha = 0.45f)
                        ),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.16f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    TurnManeuverCanvasIcon(
                                        maneuver = navGuidance.maneuver,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = navGuidance.distanceToTurnLabel,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = if (navGuidance.isOffRoute) "Keluar Jalur" else navGuidance.streetName,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = navGuidance.instructionText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Map Tap Picking Hint
                if (isPickingDestinationOnMap) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 92.dp, end = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Ketuk Peta TomTom untuk menetapkan Titik Tujuan baru",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = { onTogglePickMode(false) }
                            ) {
                                Text("Batal", color = Color(0xFFFFEDD5), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Top-Left GPS & Cloud Sync Status + Offline Sim Triggers (Hidden in Clean Nav HUD)
                if (!isCleanNavHud) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(
                                start = 12.dp,
                                top = if (activeNavMode) 88.dp else 12.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                onClick = {
                                    if (gpsStatus == GpsTrackingStatus.DENIED) {
                                        if (isPermanentlyDenied) {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null)
                                            )
                                            context.startActivity(intent)
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                )
                                            )
                                        }
                                    } else {
                                        onRetryGps()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = when (gpsStatus) {
                                    GpsTrackingStatus.ACTIVE -> Color(0xE6059669)
                                    GpsTrackingStatus.DENIED -> Color(0xE6DC2626)
                                    else -> Color(0xCC0F172A)
                                },
                                contentColor = Color.White,
                                shadowElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = if (gpsStatus == GpsTrackingStatus.ACTIVE) {
                                            "GPS ±${gpsAccuracyMeters}m"
                                        } else if (gpsStatus == GpsTrackingStatus.DENIED && isPermanentlyDenied) {
                                            "Buka Pengaturan Lokasi"
                                        } else {
                                            gpsStatus.label
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (activeRoom?.isSyncedRoom == true) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xE61D4ED8),
                                    contentColor = Color.White,
                                    shadowElevation = 3.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Rounded.CloudDone, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Text("Room Sync", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        // Economical TomTom API Key Ping Indicator Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (canPingTomTomRouting) Color(0xEB4338CA) else Color(0xEB0F766E),
                            contentColor = Color.White,
                            shadowElevation = 3.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(Icons.Rounded.Route, contentDescription = null, modifier = Modifier.size(12.dp))
                                Text(
                                    text = if (canPingTomTomRouting) {
                                        if (activeRoom?.leaderTroubleActive == true) "Ping TomTom: Plt. Leader" else "Ping TomTom: HP Leader"
                                    } else {
                                        "Salinan Rute Leader (Hemat API)"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (gpsStatus == GpsTrackingStatus.DENIED) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xEBFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = if (isPermanentlyDenied) {
                                        "Izin lokasi diblokir permanen di Android. Ketuk tombol merah untuk buka Pengaturan > Izin > Lokasi."
                                    } else {
                                        "Izin GPS belum aktif. Pastikan Lokasi HP nyala & ketuk tombol merah untuk mengizinkan."
                                    },
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier
                                        .widthIn(max = 240.dp)
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (activeRoom?.isSyncedRoom != true) {
                            FilterChip(
                                selected = isOffRouteSimActive,
                                onClick = onToggleOffRouteSim,
                                label = { Text("Uji Keluar Jalur", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = isStragglerSimActive,
                                onClick = onToggleStragglerSim,
                                label = { Text("Uji Tertinggal", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = isLeaderTroubleSimActive,
                                onClick = onToggleLeaderTroubleSim,
                                label = { Text("Uji Trouble Leader", fontSize = 11.sp) }
                            )
                        }
                    }
                } else if (gpsStatus == GpsTrackingStatus.DENIED) {
                    Surface(
                        onClick = {
                            if (isPermanentlyDenied) {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 12.dp, top = 88.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xE6DC2626),
                        contentColor = Color.White,
                        shadowElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.GpsFixed, contentDescription = null, modifier = Modifier.size(13.dp))
                            Text("Izin GPS Ditolak · Ketuk", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Bottom-Left Digital Speedometer, Compass & Compact ETA in GPS Navigation Mode
                if (activeNavMode) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 12.dp, bottom = 18.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xEB0F172A),
                        contentColor = Color.White,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${navGuidance.speedKmh}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 24.sp
                                )
                                Text(
                                    text = "km/j",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.78f),
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                if (isCleanNavHud) {
                                    Text(
                                        text = "· ${etaInfo.arrivalClockLabel} · ${etaInfo.distanceLabel}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF86EFAC),
                                        modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                NavCompassArrowCanvasIcon(
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "${navGuidance.headingDegrees}° ${GeoUtils.compassDirectionShort(navGuidance.headingDegrees)} · GPS",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                    }
                }

                // Top-Right Layer, Zoom & Center Controls (Streamlined during Clean Nav HUD)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            val next = !isNavigationMode
                            isNavigationMode = next
                            if (next) {
                                isNavInfoHidden = true
                                isAutoFollowing = true
                                val m = navSubject
                                if (m != null) {
                                    val ahead = GeoUtils.offsetPointAlongHeading(
                                        m.mapPosition.latitude,
                                        m.mapPosition.longitude,
                                        navGuidance.headingDegrees.toDouble(),
                                        85.0
                                    )
                                    mapController.animateTo(ahead.lat, ahead.lng, 16.8)
                                }
                            }
                        },
                        containerColor = if (isNavigationMode) Color(0xFFEA580C) else MaterialTheme.colorScheme.surface,
                        contentColor = if (isNavigationMode) Color.White else Color(0xFFEA580C)
                    ) {
                        NavCompassArrowCanvasIcon(
                            tint = if (isNavigationMode) Color.White else Color(0xFFEA580C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            mapStyle = if (mapStyle == TomTomMapStyle.ROAD) TomTomMapStyle.SATELLITE else TomTomMapStyle.ROAD
                        },
                        containerColor = if (mapStyle == TomTomMapStyle.SATELLITE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (mapStyle == TomTomMapStyle.SATELLITE) Color.White else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Rounded.Layers, contentDescription = "Ubah Lapisan Peta")
                    }
                    if (!isCleanNavHud) {
                        SmallFloatingActionButton(
                            onClick = { showTraffic = !showTraffic },
                            containerColor = if (showTraffic) Color(0xFF0F766E) else MaterialTheme.colorScheme.surface,
                            contentColor = if (showTraffic) Color.White else MaterialTheme.colorScheme.primary
                        ) {
                            Icon(Icons.Rounded.Route, contentDescription = "Lalu Lintas Live")
                        }
                        if (activeRoom?.isSyncedRoom != true) {
                            SmallFloatingActionButton(
                                onClick = onToggleSimulation,
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Icon(if (isSimulationRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null)
                            }
                        }
                        SmallFloatingActionButton(
                            onClick = { mapController.zoomIn() },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Perbesar")
                        }
                        SmallFloatingActionButton(
                            onClick = { mapController.zoomOut() },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Perkecil")
                        }
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            isAutoFollowing = true
                            val target = (if (activeNavMode) navSubject else trackedMember) ?: currentUser
                            if (target != null) {
                                val camPt = if (activeNavMode) {
                                    GeoUtils.offsetPointAlongHeading(
                                        target.mapPosition.latitude,
                                        target.mapPosition.longitude,
                                        navGuidance.headingDegrees.toDouble(),
                                        85.0
                                    )
                                } else {
                                    LatLngGeo(target.mapPosition.latitude, target.mapPosition.longitude)
                                }
                                mapController.animateTo(
                                    camPt.lat,
                                    camPt.lng,
                                    if (activeNavMode) 16.8 else 15.0
                                )
                            }
                        },
                        containerColor = if (isAutoFollowing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (isAutoFollowing) Color.White else MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Pusatkan & Ikuti Live")
                    }
                }

                // Left-Edge Trigger Handle for Auto-Hiding Side Info Drawer
                if (!showSideInfoDrawer && !isPickingDestinationOnMap) {
                    Surface(
                        onClick = openSideDrawer,
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 6.dp,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 8.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Buka Panel Info Perjalanan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (sosState.isActive || offRouteAlert.isOffRoute || stragglerAlert.isTriggered || activeRoom?.leaderTroubleActive == true) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .align(Alignment.TopEnd)
                                            .background(Color(0xFFEA580C), CircleShape)
                                    )
                                }
                            }
                            Text("Info", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                // Bottom-Right Floating Action Controls: Chat placed cleanly above Mic + SOS row
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { showChatSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = "Buka Chat",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Chat (${messages.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }

                    if (!voiceStatusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            shadowElevation = 4.dp,
                            modifier = Modifier.widthIn(max = 240.dp)
                        ) {
                            Text(
                                text = voiceStatusMessage,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = {
                                if (isRecordingVoice) {
                                    onStopVoiceRecord()
                                    showChatSheet = true
                                } else {
                                    startVoiceWithPermission()
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isRecordingVoice) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isRecordingVoice) Icons.Rounded.Stop else Icons.Rounded.Mic,
                                    contentDescription = "Rekam Suara",
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Prominent Rounded Elevated Red SOS Button
                        Surface(
                            onClick = { showSosConfirmDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFDC2626),
                            contentColor = Color.White,
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Tombol Darurat SOS",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("SOS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Backdrop scrim + Auto-Hiding Side Info Drawer (7s inactivity auto-collapse)
                if (showSideInfoDrawer) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.25f))
                            .clickable { showSideInfoDrawer = false }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showSideInfoDrawer,
                    enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    val actingLeaderMember = members.firstOrNull {
                        it.isActingLeader || (activeRoom?.actingLeaderUid?.isNotBlank() == true && it.id == activeRoom.actingLeaderUid)
                    }
                    val coLeaderMember = members.firstOrNull {
                        it.isCoLeader || (activeRoom?.coLeaderUid?.isNotBlank() == true && it.id == activeRoom.coLeaderUid)
                    }
                    val avgTenMin = if (speedHistory.isNotEmpty()) speedHistory.average().roundToInt() else etaInfo.speedKmh

                    Surface(
                        shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 14.dp,
                        modifier = Modifier
                            .width(296.dp)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        awaitPointerEvent(PointerEventPass.Initial)
                                        touchSideDrawer()
                                    }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header with countdown
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Info & Data Perjalanan",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${activeRoom?.roomId ?: "KNV-0000"} · Auto-hide ${sideInfoSecondsLeft}d",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { showSideInfoDrawer = false }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Tutup Panel Info")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Toggle Clean Navigation HUD
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Layar Navigasi Bersih",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Sembunyikan strip & grafik saat navigasi",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isNavInfoHidden,
                                        onCheckedChange = {
                                            touchSideDrawer()
                                            isNavInfoHidden = it
                                        }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                // Trip Telemetry & 10-Min Speed Sparkline
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "TUJUAN & ETA",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = routeConfig.destinationName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = if (trackedMember == null || trackedMember.id == currentUser?.id) "Fokus: Posisi Anda" else "Fokus: ${trackedMember.fullName}",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (currentUser != null && trackedMember != null && trackedMember.id != currentUser.id) {
                                                    Text(
                                                        text = "· Kembali",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.clickable {
                                                            touchSideDrawer()
                                                            onSelectMember(currentUser.id)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "ETA ${etaInfo.arrivalClockLabel}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = etaInfo.durationLabel,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("Sisa Jarak", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(etaInfo.distanceLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("Kecepatan", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("${etaInfo.speedKmh} km/j", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                }
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                                    Text("Rata² 10m", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("$avgTenMin km/j", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                                }
                                            }
                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(26.dp)
                                            ) {
                                                if (speedHistory.size >= 2) {
                                                    val minVal = (speedHistory.minOrNull() ?: 0).toFloat()
                                                    val maxVal = ((speedHistory.maxOrNull() ?: 60).toFloat()).coerceAtLeast(minVal + 10f)
                                                    val stepX = size.width / (speedHistory.size - 1)
                                                    val linePath = Path()
                                                    speedHistory.forEachIndexed { idx, value ->
                                                        val x = idx * stepX
                                                        val norm = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                                                        val y = size.height - norm * (size.height - 6f) - 3f
                                                        if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                                                    }
                                                    drawPath(
                                                        path = linePath,
                                                        color = Color(0xFF1D4ED8),
                                                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                // System GPS, Cloud Sync & TomTom API Status
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = "SISTEM & RUTE API",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sinyal GPS", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (gpsStatus == GpsTrackingStatus.ACTIVE) "Aktif ±${gpsAccuracyMeters}m" else gpsStatus.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (gpsStatus == GpsTrackingStatus.ACTIVE) Color(0xFF16A34A) else if (gpsStatus == GpsTrackingStatus.DENIED) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.clickable {
                                                touchSideDrawer()
                                                if (gpsStatus == GpsTrackingStatus.DENIED && isPermanentlyDenied) {
                                                    val intent = Intent(
                                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                        Uri.fromParts("package", context.packageName, null)
                                                    )
                                                    context.startActivity(intent)
                                                } else {
                                                    onRetryGps()
                                                }
                                            }
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Sinkronisasi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (activeRoom?.isSyncedRoom == true) "Room Sync Aktif" else "Simulasi Lokal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ping TomTom", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (canPingTomTomRouting) {
                                                if (activeRoom?.leaderTroubleActive == true) "Plt. Leader" else "HP Leader"
                                            } else {
                                                "Salinan Leader"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    val sideSweeperMember = members.firstOrNull { it.isSweeper }
                                    if (actingLeaderMember != null && activeRoom?.leaderTroubleActive == true) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Plt. Leader", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(actingLeaderMember.fullName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    } else if (coLeaderMember != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Co-Leader", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(coLeaderMember.fullName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (sideSweeperMember != null) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Sweeper", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = "${sideSweeperMember.fullName}${if (sideSweeperMember.isAutoSweeper) " (Auto)" else ""}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                    if (activeRoom != null) {
                                        OutlinedButton(
                                            onClick = {
                                                copiedShareHint = null
                                                showSideInfoDrawer = false
                                                showShareRoomDialog = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Bagikan Link & Kode (${activeRoom.roomId})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                // Convoy Participants & Quick Self Condition
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "PESERTA KONVOI (${members.size})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            MemberConditionStatus.AMAN to "Aman",
                                            MemberConditionStatus.ISTIRAHAT to "Istirahat",
                                            MemberConditionStatus.MOGOK to "Mogok"
                                        ).forEach { (status, shortLabel) ->
                                            FilterChip(
                                                selected = currentUser?.conditionStatus == status,
                                                onClick = {
                                                    touchSideDrawer()
                                                    onUpdateOwnCondition(status)
                                                },
                                                label = { Text(shortLabel, fontSize = 10.sp, maxLines = 1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    members.forEach { member ->
                                        val isSel = member.id == focusedMember?.id
                                        val roleLabel = when {
                                            member.isActingLeader && member.role != UserRole.TOUR_LEADER -> "Plt. Leader"
                                            member.isCoLeader && member.isSweeper -> if (member.isAutoSweeper) "Co-Leader & Sweeper (Auto)" else "Co-Leader & Sweeper"
                                            member.isCoLeader -> "Co-Leader"
                                            member.isSweeper -> if (member.isAutoSweeper) "Sweeper (Auto · Belakang)" else "Sweeper"
                                            member.role == UserRole.TOUR_LEADER -> "Leader"
                                            else -> "Anggota"
                                        }
                                        val distText = member.distanceToLeaderMeters?.let { " · ${formatDistanceLabel(it)}" }.orEmpty()
                                        Surface(
                                            onClick = {
                                                touchSideDrawer()
                                                onSelectMember(member.id)
                                                isAutoFollowing = true
                                                mapController.animateTo(
                                                    member.mapPosition.latitude,
                                                    member.mapPosition.longitude,
                                                    if (activeNavMode) 16.8 else 15.0
                                                )
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.background,
                                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ConvoyAvatar(
                                                    preset = member.avatarPreset,
                                                    photoUri = member.avatarPhotoUri,
                                                    displayName = member.fullName,
                                                    size = 28.dp,
                                                    borderColor = if (member.isCurrentUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = if (member.isCurrentUser) "${member.fullName} (Anda)" else member.fullName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "$roleLabel$distText · ${member.conditionStatus.label}",
                                                        fontSize = 10.sp,
                                                        color = if (member.isSweeper) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isEffectiveLeader && member.role != UserRole.TOUR_LEADER && !member.isActingLeader) {
                                                        Text(
                                                            text = when {
                                                                member.isSweeper && !member.isAutoSweeper -> "Sweeper Aktif"
                                                                member.isAutoSweeper -> "Tetapkan Sweeper"
                                                                else -> "Jadikan Sweeper"
                                                            },
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier
                                                                .padding(top = 2.dp)
                                                                .clickable {
                                                                    touchSideDrawer()
                                                                    onAssignSweeper(member.id)
                                                                }
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${member.mapPosition.speedKmh} km/j",
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                                // Traffic & Offline Simulation Controls inside Side Drawer
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "KONTROL PETA & SIMULASI",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilterChip(
                                            selected = showTraffic,
                                            onClick = {
                                                touchSideDrawer()
                                                showTraffic = !showTraffic
                                            },
                                            label = { Text("Traffic Live", fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (activeRoom?.isSyncedRoom != true) {
                                            FilterChip(
                                                selected = isSimulationRunning,
                                                onClick = {
                                                    touchSideDrawer()
                                                    onToggleSimulation()
                                                },
                                                label = { Text(if (isSimulationRunning) "Jeda Sim" else "Lanjut Sim", fontSize = 10.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    if (activeRoom?.isSyncedRoom != true) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = isOffRouteSimActive,
                                                onClick = {
                                                    touchSideDrawer()
                                                    onToggleOffRouteSim()
                                                },
                                                label = { Text("Keluar Jalur", fontSize = 10.sp, maxLines = 1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                            FilterChip(
                                                selected = isStragglerSimActive,
                                                onClick = {
                                                    touchSideDrawer()
                                                    onToggleStragglerSim()
                                                },
                                                label = { Text("Tertinggal", fontSize = 10.sp, maxLines = 1) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        FilterChip(
                                            selected = isLeaderTroubleSimActive,
                                            onClick = {
                                                touchSideDrawer()
                                                onToggleLeaderTroubleSim()
                                            },
                                            label = { Text("Uji Trouble HP Leader", fontSize = 10.sp) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Docked Destination ETA & 10-Min Avg Speed Telemetry Bar (Hidden in Clean Nav HUD)
            if (!isCleanNavHud) {
                DestinationEtaHudBar(
                    destinationName = routeConfig.destinationName,
                    trackedMember = trackedMember,
                    currentUserId = currentUser?.id,
                    etaInfo = etaInfo,
                    speedHistory = speedHistory,
                    onFocusSelf = if (currentUser != null && trackedMember?.id != currentUser.id) {
                        { onSelectMember(currentUser.id) }
                    } else null
                )
            }
        }
        }

        // 10-Second Full-Screen Flashing Red Screen & Large SOS Alert Directly on Navigation Screen
        if (showSosFlashOverlay && sosState.isActive) {
            val flashTransition = rememberInfiniteTransition(label = "sosScreenFlash")
            val flashAlpha by flashTransition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.46f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 360),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sosFlashAlpha"
            )
            val borderAlpha = (flashAlpha * 2f).coerceIn(0.45f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFDC2626).copy(alpha = flashAlpha))
                    .border(8.dp, Color(0xFFEF4444).copy(alpha = borderAlpha))
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFB91C1C),
                    contentColor = Color.White,
                    border = BorderStroke(2.dp, Color(0xFFFCA5A5)),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .widthIn(max = 400.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = "Darurat SOS",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SINYAL DARURAT SOS",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hilang otomatis dalam $sosFlashSecondsLeft dtk",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFFEE2E2)
                                )
                            }
                            IconButton(
                                onClick = { dismissedSosKey = currentSosKey },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Tutup Notifikasi SOS",
                                    tint = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Black.copy(alpha = 0.28f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PENGIRIM SINYAL SOS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFECACA)
                                )
                                Text(
                                    text = sosState.senderName.ifBlank { "Anggota Konvoi" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${sosState.senderVehicle.ifBlank { "Kendaraan Konvoi" }} · Pukul ${sosState.triggeredAtLabel.ifBlank { "-" }}",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFEE2E2)
                                )
                                Text(
                                    text = "${sosState.locationText.ifBlank { "Lokasi Live" }} (${String.format(Locale.US, "%.4f, %.4f", sosState.latitude, sosState.longitude)})",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Identitas pengirim SOS tetap tercatat di kolom chat konvoi.",
                                fontSize = 11.sp,
                                color = Color(0xFFFEE2E2)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (sosState.senderId.isNotBlank()) {
                                        onSelectMember(sosState.senderId)
                                        isAutoFollowing = true
                                    }
                                    dismissedSosKey = currentSosKey
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF991B1B)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lokasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    dismissedSosKey = currentSosKey
                                    onResolveSos()
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF7F1D1D),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Selesai SOS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((sosFlashSecondsLeft / 10f).coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .background(Color.White, RoundedCornerShape(99.dp))
                            )
                        }
                    }
                }
            }
        }
    }

    // Modern Messenger Chat & Large Waveform PTT Bottom Sheet
    if (showChatSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp, max = 560.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Komunikasi Konvoi", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${activeRoom?.roomId ?: "KNV-8492"} · ${members.size} Anggota Terhubung",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showChatSheet = false }) {
                        Text("Tutup")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        val matchedMember = members.find { it.id == msg.senderId }
                        MessengerChatBubble(
                            message = msg,
                            avatarPreset = matchedMember?.avatarPreset ?: msg.senderAvatarPreset,
                            avatarPhotoUri = matchedMember?.avatarPhotoUri ?: msg.senderAvatarPhotoUri,
                            isPlaying = playingMessageId == msg.id,
                            onTogglePlay = { onToggleVoicePlay(msg.id) },
                            sosActive = sosState.isActive,
                            onResolveSos = onResolveSos
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Large Interactive Audio Waveform Push-to-Talk Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!voiceStatusMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = voiceStatusMessage,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = Color(0xFFB91C1C),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    InteractiveWaveformPttButton(
                        isRecording = isRecordingVoice,
                        recordingSeconds = recordingSeconds,
                        onToggleRecord = {
                            if (isRecordingVoice) onStopVoiceRecord() else startVoiceWithPermission()
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = chatInputText,
                            onValueChange = { chatInputText = it },
                            placeholder = { Text("Tulis pesan ke rombongan...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f)
                        )

                        FloatingActionButton(
                            onClick = {
                                if (chatInputText.isNotBlank()) {
                                    onSendTextMessage(chatInputText)
                                    chatInputText = ""
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Kirim Pesan")
                        }
                    }
                }
            }
        }
    }

    // Finish & Save Trip to History Dialog
    if (showFinishTripDialog) {
        AlertDialog(
            onDismissRequest = { showFinishTripDialog = false },
            icon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(30.dp)) },
            title = { Text("Akhiri & Simpan Perjalanan?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isEffectiveLeader && activeRoom?.isSyncedRoom == true) {
                        "Sesi konvoi akan langsung berakhir untuk Anda dan seluruh anggota room. Rekam jejak rute menuju ${routeConfig.destinationName} dan daftar peserta akan disimpan ke Riwayat Tour untuk ekspor GPX."
                    } else {
                        "Rekam jejak rute menuju ${routeConfig.destinationName} dan daftar peserta akan disimpan ke Riwayat Tour untuk ekspor GPX."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishTripDialog = false
                        onFinishAndSaveTrip()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Simpan ke Riwayat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishTripDialog = false }) {
                    Text("Lanjut Tour")
                }
            }
        )
    }

    // SOS Confirmation Dialog
    if (showSosConfirmDialog) {
        val latStr = String.format(Locale.US, "%.4f", currentUser?.mapPosition?.latitude ?: -6.6740)
        val lngStr = String.format(Locale.US, "%.4f", currentUser?.mapPosition?.longitude ?: 106.9250)
        AlertDialog(
            onDismissRequest = { showSosConfirmDialog = false },
            icon = { Icon(Icons.Rounded.Sos, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) },
            title = { Text("Kirim Sinyal Darurat SOS?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Sinyal bahaya dan koordinat lokasi Anda ($latStr, $lngStr) akan dikirimkan ke seluruh anggota room konvoi.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosConfirmDialog = false
                        onTriggerSos()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Kirim SOS Sekarang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSosConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showShareRoomDialog && activeRoom != null) {
        val webShareLink = "${activeWebBaseUrl.trimEnd('/')}/?room=${Uri.encode(activeRoom.roomId)}"
        AlertDialog(
            onDismissRequest = { showShareRoomDialog = false },
            icon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Bagikan Undangan Konvoi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("KODE ROOM", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = activeRoom.roomId,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeRoom.roomId))
                                    copiedShareHint = "Kode Room ${activeRoom.roomId} disalin"
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salin")
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Link Browser", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(webShareLink, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(webShareLink))
                                    copiedShareHint = "Link Browser disalin"
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin Link Web", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Link APK KonvoiKu", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(activeRoom.inviteLink, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(activeRoom.inviteLink))
                                    copiedShareHint = "Link APK disalin"
                                }
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin Link APK", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (!copiedShareHint.isNullOrBlank()) {
                        Text(
                            text = copiedShareHint.orEmpty(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shareBody = "Gabung konvoi \"${activeRoom.roomTitle}\" di KonvoiKu!\nKode Room: ${activeRoom.roomId}\nLink Web: $webShareLink\nLink Aplikasi: ${activeRoom.inviteLink}"
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Undangan Konvoi ${activeRoom.roomId}")
                            putExtra(Intent.EXTRA_TEXT, shareBody)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Bagikan Undangan Konvoi"))
                    }
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showShareRoomDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Leader Route, Checkpoints & Straggler Settings Bottom Sheet
    if (showLeaderSettingsSheet && isEffectiveLeader) {
        ModalBottomSheet(
            onDismissRequest = { showLeaderSettingsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Pengaturan Rute & Peringatan Leader", style = MaterialTheme.typography.titleLarge)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Titik Tujuan Akhir & Garis Rute Resmi", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = destSearchQuery,
                        onValueChange = { destSearchQuery = it },
                        label = { Text("Cari Area Tujuan Baru") },
                        placeholder = { Text("Mis. Puncak, Ciwidey, Rest Area...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (destSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    destSearchQuery = ""
                                    destSearchResults = emptyList()
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Hapus")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isSearchingDest || destSearchResults.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                if (isSearchingDest) {
                                    Text(
                                        text = "Mencari lokasi tujuan di TomTom...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                } else {
                                    destSearchResults.take(4).forEach { result ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onPickCustomDestinationLatLng(
                                                        result.latitude,
                                                        result.longitude,
                                                        result.name,
                                                        result.address
                                                    )
                                                    destSearchQuery = ""
                                                    destSearchResults = emptyList()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp)
                                        ) {
                                            Text(result.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(result.address, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        HorizontalDivider()
                                    }
                                }
                            }
                        }
                    }
                    destinationPresets.chunked(2).forEach { rowPresets ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowPresets.forEach { preset ->
                                FilterChip(
                                    selected = routeConfig.destinationName == preset.name,
                                    onClick = { onSelectPreset(preset) },
                                    label = { Text(preset.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            onTogglePickMode(true)
                            showLeaderSettingsSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.EditLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ketuk Peta untuk Tujuan Kustom")
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                // Interactive Straggler Distance Slider
                val kmValue = (routeConfig.maxStragglerDistanceMeters / 1000f).coerceIn(0.5f, 5.0f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Batas Jarak Maksimal Tertinggal: ${String.format(Locale.US, "%.1f km", kmValue)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = kmValue,
                        onValueChange = { nextKm ->
                            val snappedMeters = ((nextKm * 2f).roundToInt() * 500).coerceIn(500, 5000)
                            onUpdateMaxStragglerDistance(snappedMeters)
                        },
                        valueRange = 0.5f..5.0f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Peringatan Suara (Audio Alert)", style = MaterialTheme.typography.titleMedium)
                        Text("Bunyikan nada peringatan saat SOS, keluar jalur, atau tertinggal.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = routeConfig.soundAlertEnabled, onCheckedChange = { onToggleSoundAlert() })
                }

                Button(
                    onClick = { showLeaderSettingsSheet = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Simpan & Terapkan ke Room")
                }
            }
        }
    }
}

@Composable
private fun TopParticipantChip(
    member: ConvoyMember,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            member.isSweeper -> Color(0xFFFFFBEB)
            else -> MaterialTheme.colorScheme.background
        },
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isSelected -> MaterialTheme.colorScheme.primary
                member.isSweeper -> Color(0xFFD97706)
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConvoyAvatar(
                preset = member.avatarPreset,
                photoUri = member.avatarPhotoUri,
                displayName = member.fullName,
                size = 30.dp,
                borderColor = when {
                    member.isSweeper -> Color(0xFFD97706)
                    member.isCurrentUser -> MaterialTheme.colorScheme.secondary
                    else -> Color.White
                }
            )
            Column {
                val roleBadge = when {
                    member.isActingLeader && member.role != UserRole.TOUR_LEADER -> "Plt.Ldr"
                    member.isCoLeader && member.isSweeper -> if (member.isAutoSweeper) "Co/Swp*" else "Co/Swp"
                    member.isCoLeader -> "Co-Ldr"
                    member.isSweeper -> if (member.isAutoSweeper) "Swp·Auto" else "Swp"
                    member.role == UserRole.TOUR_LEADER -> "Leader"
                    else -> null
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = member.fullName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (roleBadge != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                member.isActingLeader && member.role != UserRole.TOUR_LEADER -> Color(0xFFEDE9FE)
                                member.isSweeper -> Color(0xFFFEF3C7)
                                member.isCoLeader -> Color(0xFFCCFBF1)
                                else -> Color(0xFFDBEAFE)
                            }
                        ) {
                            Text(
                                text = roleBadge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    member.isActingLeader && member.role != UserRole.TOUR_LEADER -> Color(0xFF6D28D9)
                                    member.isSweeper -> Color(0xFFB45309)
                                    member.isCoLeader -> Color(0xFF0F766E)
                                    else -> Color(0xFF1D4ED8)
                                },
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = if (member.vehicleType == VehicleType.MOBIL) Icons.Rounded.DirectionsCar else Icons.Rounded.TwoWheeler,
                        contentDescription = member.vehicleType.label,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    val distNote = member.distanceToLeaderMeters?.let { " · ${formatDistanceLabel(it)}" }.orEmpty()
                    Text(
                        text = "${member.vehicleType.label}$distNote",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationEtaHudBar(
    destinationName: String,
    trackedMember: ConvoyMember?,
    currentUserId: String?,
    etaInfo: DestinationEtaInfo,
    speedHistory: List<Int>,
    onFocusSelf: (() -> Unit)?
) {
    val isTrackingSelf = trackedMember == null || trackedMember.id == currentUserId
    val avgTenMin = if (speedHistory.isNotEmpty()) speedHistory.average().roundToInt() else etaInfo.speedKmh
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Destination & Tracked Vehicle
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (etaInfo.isArrived) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Estimasi Tiba",
                                tint = if (etaInfo.isArrived) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = destinationName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isTrackingSelf) "Posisi Anda" else trackedMember?.fullName ?: "Konvoi",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (onFocusSelf != null) {
                                Text(
                                    text = "• Kembali ke Anda",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable(onClick = onFocusSelf)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ETA, Distance, and Current Speed Metrics
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ETA ${etaInfo.arrivalClockLabel}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                etaInfo.isArrived -> Color(0xFF16A34A)
                                etaInfo.isStopped -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                        Text(
                            text = etaInfo.durationLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (etaInfo.isStopped) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = etaInfo.distanceLabel,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "${etaInfo.speedKmh} km/j",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Mini-graph Rata-Rata Kecepatan Konvoi 10 Menit Terakhir
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column {
                        Text(
                            text = "RATA² KONVOI · 10 MNT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$avgTenMin km/j",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                    ) {
                        if (speedHistory.size >= 2) {
                            val minVal = (speedHistory.minOrNull() ?: 0).toFloat()
                            val maxVal = ((speedHistory.maxOrNull() ?: 60).toFloat()).coerceAtLeast(minVal + 10f)
                            val stepX = size.width / (speedHistory.size - 1)
                            val linePath = Path()
                            speedHistory.forEachIndexed { idx, value ->
                                val x = idx * stepX
                                val norm = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
                                val y = size.height - norm * (size.height - 6f) - 3f
                                if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                            }
                            drawPath(
                                path = linePath,
                                color = Color(0xFF1D4ED8),
                                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantExpandedRow(
    member: ConvoyMember,
    etaInfo: DestinationEtaInfo,
    viewerIsLeader: Boolean,
    isSelected: Boolean,
    onAssignSweeper: () -> Unit,
    onClick: () -> Unit
) {
    val conditionColor = when (member.conditionStatus) {
        MemberConditionStatus.AMAN -> Color(0xFF16A34A)
        MemberConditionStatus.ISTIRAHAT -> Color(0xFFD97706)
        MemberConditionStatus.MOGOK, MemberConditionStatus.SOS -> Color(0xFFDC2626)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.background
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ConvoyAvatar(
                preset = member.avatarPreset,
                photoUri = member.avatarPhotoUri,
                displayName = member.fullName,
                size = 38.dp,
                borderColor = if (member.isCurrentUser) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (member.isCurrentUser) "${member.markerTitle} (Anda)" else member.markerTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val roleLabel = when {
                    member.isActingLeader && member.role != UserRole.TOUR_LEADER -> "Plt. Leader (Co-Leader)"
                    member.isCoLeader && member.isSweeper -> if (member.isAutoSweeper) "Co-Leader & Sweeper (Auto)" else "Co-Leader & Sweeper"
                    member.isCoLeader -> "Co-Leader Terdekat"
                    member.isSweeper -> if (member.isAutoSweeper) "Sweeper (Auto · Paling Belakang)" else "Sweeper"
                    else -> member.role.label
                }
                val distLabel = member.distanceToLeaderMeters?.let { " · ${formatDistanceLabel(it)} dr Leader" }.orEmpty()
                Text(
                    text = "$roleLabel$distLabel · ${member.lastLocationText}",
                    fontSize = 11.sp,
                    color = if (member.isSweeper) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (viewerIsLeader && member.role != UserRole.TOUR_LEADER && !member.isActingLeader) {
                    TextButton(
                        onClick = onAssignSweeper,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                member.isSweeper && !member.isAutoSweeper -> "Sweeper Aktif"
                                member.isAutoSweeper -> "Tetapkan Sweeper"
                                else -> "Jadikan Sweeper"
                            },
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = when (member.conditionStatus) {
                            MemberConditionStatus.AMAN -> Icons.Rounded.CheckCircle
                            MemberConditionStatus.ISTIRAHAT -> Icons.Rounded.Coffee
                            MemberConditionStatus.MOGOK -> Icons.Rounded.Build
                            MemberConditionStatus.SOS -> Icons.Rounded.Sos
                        },
                        contentDescription = member.conditionStatus.label,
                        tint = conditionColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(member.conditionStatus.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = conditionColor)
                }
                Text(
                    text = "${member.mapPosition.speedKmh} km/j · ETA ${etaInfo.durationLabel}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sisa ${etaInfo.distanceLabel}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InteractiveWaveformPttButton(
    isRecording: Boolean,
    recordingSeconds: Int,
    onToggleRecord: () -> Unit
) {
    val heights = listOf(10, 20, 14, 28, 18, 32, 22, 16, 26, 14, 20, 12)
    Surface(
        onClick = onToggleRecord,
        shape = RoundedCornerShape(18.dp),
        color = if (isRecording) Color(0xFFDC2626) else MaterialTheme.colorScheme.secondary,
        contentColor = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = "Rekam Suara",
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = if (isRecording) "Sedang Merekam · Ketuk Kirim" else "Tekan untuk Bicara (Walkie-Talkie)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRecording) "0:${recordingSeconds.toString().padStart(2, '0')} · Audio konvoi" else "Pesan suara instan ke semua anggota",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                heights.forEachIndexed { idx, h ->
                    val barHeight = if (isRecording) ((h + (recordingSeconds * 5 + idx * 3) % 10).coerceAtMost(34)) else h / 2 + 4
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(barHeight.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun MessengerChatBubble(
    message: RoomMessage,
    avatarPreset: com.konvoiku.touring.model.AvatarPreset,
    avatarPhotoUri: String?,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    sosActive: Boolean = false,
    onResolveSos: () -> Unit = {}
) {
    when (message.type) {
        ChatMessageType.SYSTEM -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "${message.timestampLabel} · ${message.textContent}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
        ChatMessageType.SOS_ALERT -> {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ConvoyAvatar(
                        preset = avatarPreset,
                        photoUri = avatarPhotoUri,
                        displayName = message.senderName,
                        size = 34.dp,
                        borderColor = Color(0xFFDC2626)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val vehicleLabel = if (message.senderVehicle.isNotBlank()) " (${message.senderVehicle})" else ""
                        Text(
                            text = "DARURAT SOS · ${message.senderName}$vehicleLabel · ${message.timestampLabel}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = message.textContent,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF7F1D1D)
                        )
                        if (sosActive) {
                            Button(
                                onClick = onResolveSos,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Tandai SOS Selesai", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        else -> {
            val isMine = message.isFromCurrentUser
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.Bottom
            ) {
                if (!isMine) {
                    ConvoyAvatar(
                        preset = avatarPreset,
                        photoUri = avatarPhotoUri,
                        displayName = message.senderName,
                        size = 30.dp,
                        borderColor = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Surface(
                    shape = if (isMine) {
                        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                    } else {
                        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
                    },
                    color = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isMine) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.widthIn(max = 250.dp)
                ) {
                    if (message.type == ChatMessageType.VOICE) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = onTogglePlay,
                                shape = CircleShape,
                                color = if (isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Putar Suara",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "${message.senderName} · ${message.senderVehicle}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMine) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Voice 0:${message.voiceDurationSec.toString().padStart(2, '0')} · ${message.timestampLabel}",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            if (!isMine) {
                                Text(
                                    text = "${message.senderName} · ${message.senderVehicle}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            Text(
                                text = message.textContent,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = message.timestampLabel,
                                fontSize = 10.sp,
                                color = if (isMine) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isMine) {
                    Spacer(modifier = Modifier.width(8.dp))
                    ConvoyAvatar(
                        preset = avatarPreset,
                        photoUri = avatarPhotoUri,
                        displayName = message.senderName,
                        size = 30.dp,
                        borderColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ConvoyMapMarker(
    member: ConvoyMember,
    isSelected: Boolean,
    isOffRoute: Boolean,
    isStraggler: Boolean,
    isSosBeacon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSosBeacon) 1.65f else 1f,
        animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
        label = "pulseScale"
    )

    val labelBg = when {
        isSosBeacon -> Color(0xFF991B1B)
        isOffRoute -> Color(0xFFDC2626)
        isStraggler -> Color(0xFFB45309)
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val labelText = if (isSosBeacon || isOffRoute || isStraggler || isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    val pinBackground = when {
        isSosBeacon || isOffRoute -> Color(0xFFDC2626)
        member.isCurrentUser -> MaterialTheme.colorScheme.secondary
        member.role == UserRole.TOUR_LEADER -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surface
    }
    val pinIconTint = if (isSosBeacon || isOffRoute || member.isCurrentUser || member.role == UserRole.TOUR_LEADER) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .width(192.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = labelBg,
            border = BorderStroke(1.dp, if (isSosBeacon || isSelected || isOffRoute || isStraggler) labelBg else MaterialTheme.colorScheme.outline),
            shadowElevation = 4.dp
        ) {
            val prefix = if (isSosBeacon) "SOS · " else if (member.isSweeper) "Sweeper · " else ""
            Text(
                text = prefix + if (member.isCurrentUser) "${member.markerTitle} (Anda)" else member.markerTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = labelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Circular Avatar perched right above the Vehicle Marker Icon
        ConvoyAvatar(
            preset = member.avatarPreset,
            photoUri = member.avatarPhotoUri,
            displayName = member.fullName,
            size = 28.dp,
            borderColor = if (isSosBeacon || isOffRoute) Color(0xFFDC2626) else Color.White,
            borderWidth = 2.dp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Box(contentAlignment = Alignment.Center) {
            if (isSosBeacon) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(pulseScale)
                        .background(Color(0xFFDC2626).copy(alpha = 0.35f), CircleShape)
                )
            }
            Surface(
                shape = CircleShape,
                color = pinBackground,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 6.dp,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSosBeacon) Icons.Rounded.Sos else if (member.vehicleType == VehicleType.MOBIL) Icons.Rounded.DirectionsCar else Icons.Rounded.TwoWheeler,
                        contentDescription = member.markerTitle,
                        tint = pinIconTint,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

private fun formatDistanceLabel(meters: Int): String {
    return if (meters >= 1000) {
        val km = meters / 1000f
        if (km % 1f == 0f) "${km.toInt()} km" else String.format(Locale.US, "%.1f km", km)
    } else {
        "$meters m"
    }
}

@Composable
private fun NavCompassArrowCanvasIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.88f)
            lineTo(w * 0.50f, h * 0.68f)
            lineTo(w * 0.14f, h * 0.88f)
            close()
        }
        drawPath(path = path, color = tint)
    }
}

@Composable
private fun TurnManeuverCanvasIcon(
    maneuver: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val upper = maneuver.uppercase(Locale.US)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(
            width = (w * 0.14f).coerceAtLeast(2.5f),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when {
            upper.contains("ARRIVE") || upper.contains("GOAL") -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.24f,
                    center = Offset(w * 0.5f, h * 0.42f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.5f, h * 0.66f),
                    end = Offset(w * 0.5f, h * 0.88f),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round
                )
            }
            upper.contains("UTURN") -> {
                val p = Path().apply {
                    moveTo(w * 0.30f, h * 0.84f)
                    lineTo(w * 0.30f, h * 0.34f)
                    lineTo(w * 0.50f, h * 0.18f)
                    lineTo(w * 0.70f, h * 0.34f)
                    lineTo(w * 0.70f, h * 0.72f)
                }
                drawPath(path = p, color = tint, style = stroke)
                val head = Path().apply {
                    moveTo(w * 0.56f, h * 0.58f)
                    lineTo(w * 0.70f, h * 0.76f)
                    lineTo(w * 0.84f, h * 0.58f)
                }
                drawPath(path = head, color = tint, style = stroke)
            }
            upper.contains("BEAR_LEFT") || upper.contains("KEEP_LEFT") || upper.contains("SLIGHT_LEFT") -> {
                val p = Path().apply {
                    moveTo(w * 0.62f, h * 0.86f)
                    lineTo(w * 0.62f, h * 0.50f)
                    lineTo(w * 0.26f, h * 0.20f)
                }
                drawPath(path = p, color = tint, style = stroke)
                val head = Path().apply {
                    moveTo(w * 0.46f, h * 0.18f)
                    lineTo(w * 0.24f, h * 0.18f)
                    lineTo(w * 0.24f, h * 0.40f)
                }
                drawPath(path = head, color = tint, style = stroke)
            }
            upper.contains("BEAR_RIGHT") || upper.contains("KEEP_RIGHT") || upper.contains("SLIGHT_RIGHT") -> {
                val p = Path().apply {
                    moveTo(w * 0.38f, h * 0.86f)
                    lineTo(w * 0.38f, h * 0.50f)
                    lineTo(w * 0.74f, h * 0.20f)
                }
                drawPath(path = p, color = tint, style = stroke)
                val head = Path().apply {
                    moveTo(w * 0.54f, h * 0.18f)
                    lineTo(w * 0.76f, h * 0.18f)
                    lineTo(w * 0.76f, h * 0.40f)
                }
                drawPath(path = head, color = tint, style = stroke)
            }
            upper.contains("LEFT") -> {
                val p = Path().apply {
                    moveTo(w * 0.66f, h * 0.86f)
                    lineTo(w * 0.66f, h * 0.36f)
                    lineTo(w * 0.20f, h * 0.36f)
                }
                drawPath(path = p, color = tint, style = stroke)
                val head = Path().apply {
                    moveTo(w * 0.34f, h * 0.20f)
                    lineTo(w * 0.18f, h * 0.36f)
                    lineTo(w * 0.34f, h * 0.52f)
                }
                drawPath(path = head, color = tint, style = stroke)
            }
            upper.contains("RIGHT") -> {
                val p = Path().apply {
                    moveTo(w * 0.34f, h * 0.86f)
                    lineTo(w * 0.34f, h * 0.36f)
                    lineTo(w * 0.80f, h * 0.36f)
                }
                drawPath(path = p, color = tint, style = stroke)
                val head = Path().apply {
                    moveTo(w * 0.66f, h * 0.20f)
                    lineTo(w * 0.82f, h * 0.36f)
                    lineTo(w * 0.66f, h * 0.52f)
                }
                drawPath(path = head, color = tint, style = stroke)
            }
            else -> {
                val p = Path().apply {
                    moveTo(w * 0.50f, h * 0.86f)
                    lineTo(w * 0.50f, h * 0.18f)
                }
                drawPath(path = p, color = tint, style = stroke)
                val head = Path().apply {
                    moveTo(w * 0.30f, h * 0.36f)
                    lineTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.70f, h * 0.36f)
                }
                drawPath(path = head, color = tint, style = stroke)
            }
        }
    }
}
