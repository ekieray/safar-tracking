package com.konvoiku.touring.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konvoiku.touring.model.AvatarPreset
import com.konvoiku.touring.model.ConnectionStatus
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.FamilyGrantInfo
import com.konvoiku.touring.model.FamilyMonitorSnapshot
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.RoomActivityItem
import com.konvoiku.touring.ui.components.ConvoyAvatar
import com.konvoiku.touring.ui.components.TomTomConvoyMap
import com.konvoiku.touring.ui.components.TomTomMapPin
import com.konvoiku.touring.ui.components.TomTomMapStyle
import com.konvoiku.touring.ui.components.TouringDashboardCanvasBackdrop
import com.konvoiku.touring.ui.components.rememberTomTomMapController
import com.konvoiku.touring.ui.theme.RoadAmber
import com.konvoiku.touring.ui.theme.SosRed
import com.konvoiku.touring.ui.theme.TouringBlue
import com.konvoiku.touring.ui.theme.TouringBlueLight
import com.konvoiku.touring.util.DualTimezoneClockBar
import kotlinx.coroutines.delay

private const val FAMILY_MISSING_THRESHOLD_MS = 75_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMonitorScreen(
    familyGrant: FamilyGrantInfo?,
    snapshot: FamilyMonitorSnapshot?,
    errorMessage: String?,
    onLogout: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Peta Live, 1 = Kegiatan
    var mapStyle by remember { mutableStateOf(TomTomMapStyle.ROAD) }
    var isAutoFollow by remember { mutableStateOf(true) }
    val mapController = rememberTomTomMapController()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val targetMember: ConvoyMember? = snapshot?.targetMember
    val displayName = targetMember?.fullName?.ifBlank { null }
        ?: familyGrant?.targetName?.ifBlank { null }
        ?: "Jamaah Terpantau"

    val isTargetMissing = remember(targetMember, nowMs) {
        if (targetMember == null) {
            false
        } else {
            val isOffline = targetMember.connectionStatus == ConnectionStatus.OFFLINE
            val isStale = targetMember.lastUpdatedEpochMs > 0L &&
                (nowMs - targetMember.lastUpdatedEpochMs) >= FAMILY_MISSING_THRESHOLD_MS
            isOffline || isStale
        }
    }

    LaunchedEffect(
        targetMember?.mapPosition?.latitude,
        targetMember?.mapPosition?.longitude,
        isAutoFollow,
        selectedTab
    ) {
        if (selectedTab == 0 && isAutoFollow && targetMember != null) {
            mapController.animateTo(
                latitude = targetMember.mapPosition.latitude,
                longitude = targetMember.mapPosition.longitude,
                zoomLevel = 15.5
            )
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TouringBlue.copy(alpha = 0.14f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.FamilyRestroom,
                                    contentDescription = null,
                                    tint = TouringBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Pantauan Keluarga",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${snapshot?.roomTitle ?: familyGrant?.roomId ?: "-"} · $displayName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    TextButton(
                        onClick = onLogout,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SosRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Keluar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Keluar",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            TouringDashboardCanvasBackdrop(isDarkTheme = isDark)

            when {
                errorMessage != null -> {
                    FamilyStatusMessageView(
                        icon = Icons.Rounded.Warning,
                        iconTint = SosRed,
                        title = "Pantauan Tidak Dapat Dimuat",
                        message = errorMessage,
                        actionLabel = "Keluar & Masuk Ulang",
                        onAction = onLogout
                    )
                }

                snapshot == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            CircularProgressIndicator(color = TouringBlue)
                            Text(
                                text = "Menghubungkan pantauan posisi $displayName...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                !snapshot.linkActive -> {
                    FamilyStatusMessageView(
                        icon = Icons.Rounded.LinkOff,
                        iconTint = SosRed,
                        title = "Link Pantauan Dicabut",
                        message = "Link atau kode pantauan untuk $displayName telah dinonaktifkan oleh jamaah atau ketua rombongan.",
                        actionLabel = "Keluar",
                        onAction = onLogout
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (snapshot.roomStatus.equals("FINISHED", ignoreCase = true)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = RoadAmber.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, RoadAmber.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Flag,
                                        contentDescription = null,
                                        tint = RoadAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Perjalanan rombongan telah selesai. Data posisi & kegiatan terakhir ditampilkan dalam mode hanya lihat.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (isTargetMissing && targetMember != null) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SosRed,
                                shadowElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.WifiOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "JAMAAH TIDAK TERDETEKSI",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${targetMember.fullName} terputus atau sinyal GPS tidak terdeteksi (>75 dtk). Terakhir di ${targetMember.lastLocationText} (${targetMember.lastUpdatedLabel}).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.94f)
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedTab = 0
                                            isAutoFollow = true
                                            mapController.animateTo(
                                                latitude = targetMember.mapPosition.latitude,
                                                longitude = targetMember.mapPosition.longitude,
                                                zoomLevel = 16.5
                                            )
                                        },
                                        border = BorderStroke(1.dp, Color.White),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Fokus Lokasi",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        FamilyTargetHeaderCard(
                            displayName = displayName,
                            targetMember = targetMember,
                            snapshot = snapshot,
                            isTargetMissing = isTargetMissing
                        )

                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Peta Live",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                            SegmentedButton(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.EventAvailable,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                label = {
                                    val doneCount = snapshot.completedActivityIds.size
                                    val totalCount = snapshot.activities.size
                                    Text(
                                        text = if (totalCount > 0) "Kegiatan ($doneCount/$totalCount)" else "Kegiatan",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }

                        if (selectedTab == 0) {
                            FamilyLiveMapSection(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                snapshot = snapshot,
                                targetMember = targetMember,
                                mapController = mapController,
                                mapStyle = mapStyle,
                                isDark = isDark,
                                onToggleMapStyle = {
                                    mapStyle = if (mapStyle == TomTomMapStyle.ROAD) {
                                        TomTomMapStyle.SATELLITE
                                    } else {
                                        TomTomMapStyle.ROAD
                                    }
                                },
                                onUserDrag = { isAutoFollow = false },
                                onFocusTarget = {
                                    if (targetMember != null) {
                                        isAutoFollow = true
                                        mapController.animateTo(
                                            latitude = targetMember.mapPosition.latitude,
                                            longitude = targetMember.mapPosition.longitude,
                                            zoomLevel = 16.0
                                        )
                                    }
                                }
                            )
                        } else {
                            FamilyActivitiesReadOnlySection(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                activities = snapshot.activities,
                                completedIds = snapshot.completedActivityIds.toSet(),
                                completedTimes = snapshot.completedTimes
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyTargetHeaderCard(
    displayName: String,
    targetMember: ConvoyMember?,
    snapshot: FamilyMonitorSnapshot,
    isTargetMissing: Boolean
) {
    val conditionColor = when {
        isTargetMissing -> SosRed
        targetMember?.conditionStatus == MemberConditionStatus.SOS -> SosRed
        targetMember?.conditionStatus == MemberConditionStatus.MOGOK -> RoadAmber
        targetMember?.conditionStatus == MemberConditionStatus.ISTIRAHAT -> Color(0xFF0F766E)
        else -> TouringBlue
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isTargetMissing) SosRed.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConvoyAvatar(
                    preset = targetMember?.avatarPreset ?: AvatarPreset.HELMET_TOURING,
                    photoUri = targetMember?.avatarPhotoUri,
                    displayName = displayName,
                    size = 48.dp,
                    borderColor = conditionColor
                )

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = conditionColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (isTargetMissing) "Terputus" else (targetMember?.conditionStatus?.label ?: "Terpantau"),
                                style = MaterialTheme.typography.labelSmall,
                                color = conditionColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsBus,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = targetMember?.vehicleDetail?.ifBlank { targetMember.vehicleType.label }
                                ?: "Jamaah Rombongan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = TouringBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = targetMember?.lastLocationText?.ifBlank { "Menunggu pembaruan lokasi..." }
                            ?: "Jamaah sedang bersiap di rombongan",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (targetMember != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = TouringBlue,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "${targetMember.mapPosition.speedKmh} km/j",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Sinkron ${snapshot.lastSyncLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            DualTimezoneClockBar(
                latitude = targetMember?.mapPosition?.latitude ?: snapshot.routeConfig?.waypoints?.firstOrNull()?.latitude,
                longitude = targetMember?.mapPosition?.longitude ?: snapshot.routeConfig?.waypoints?.firstOrNull()?.longitude,
                subjectName = displayName,
                compact = false
            )
        }
    }
}

@Composable
private fun FamilyLiveMapSection(
    modifier: Modifier,
    snapshot: FamilyMonitorSnapshot,
    targetMember: ConvoyMember?,
    mapController: com.konvoiku.touring.ui.components.TomTomMapController,
    mapStyle: TomTomMapStyle,
    isDark: Boolean,
    onToggleMapStyle: () -> Unit,
    onUserDrag: () -> Unit,
    onFocusTarget: () -> Unit
) {
    val routePoints = snapshot.routeConfig?.waypoints.orEmpty()
    val destPoint = routePoints.lastOrNull()
    val initialLat = targetMember?.mapPosition?.latitude
        ?: routePoints.firstOrNull()?.latitude
        ?: 21.4225
    val initialLng = targetMember?.mapPosition?.longitude
        ?: routePoints.firstOrNull()?.longitude
        ?: 39.8262

    val pins = remember(targetMember, destPoint, snapshot.destinationName, snapshot.destinationAddress) {
        buildList {
            if (targetMember != null) {
                add(
                    TomTomMapPin.MemberPin(
                        member = targetMember,
                        isSelected = true,
                        isOffRoute = false,
                        isStraggler = false,
                        isSosBeacon = targetMember.conditionStatus == MemberConditionStatus.SOS
                    )
                )
            }
            if (destPoint != null) {
                add(
                    TomTomMapPin.DestinationPin(
                        latitude = destPoint.latitude,
                        longitude = destPoint.longitude,
                        title = snapshot.destinationName.ifBlank { "Tujuan Rombongan" },
                        subtitle = snapshot.destinationAddress.ifBlank { "Titik Akhir Safar" }
                    )
                )
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TomTomConvoyMap(
                modifier = Modifier.fillMaxSize(),
                controller = mapController,
                initialLat = initialLat,
                initialLng = initialLng,
                initialZoom = 15.0,
                mapStyle = mapStyle,
                isDarkMode = isDark,
                showTraffic = false,
                polylinePoints = routePoints,
                pins = pins,
                onUserDrag = onUserDrag
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = onToggleMapStyle,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    contentColor = TouringBlue
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Layers,
                        contentDescription = "Ganti tampilan peta"
                    )
                }

                if (targetMember != null) {
                    FloatingActionButton(
                        onClick = onFocusTarget,
                        containerColor = TouringBlue,
                        contentColor = Color.White,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Fokus Lokasi Jamaah"
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = TouringBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tujuan: ${snapshot.destinationName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (targetMember != null) {
                                "Pantauan hanya lihat · Update: ${targetMember.lastUpdatedLabel}"
                            } else {
                                "Menunggu data GPS jamaah..."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FamilyActivitiesReadOnlySection(
    modifier: Modifier,
    activities: List<RoomActivityItem>,
    completedIds: Set<String>,
    completedTimes: Map<String, String>
) {
    val completedActivities = remember(activities, completedIds) {
        activities.filter { it.id in completedIds }
    }
    val upcomingActivities = remember(activities, completedIds) {
        activities.filter { it.id !in completedIds }
    }
    val progressRatio = if (activities.isEmpty()) 0f else completedActivities.size.toFloat() / activities.size.toFloat()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Progres Kegiatan Jamaah",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TouringBlueLight
                        ) {
                            Text(
                                text = "${completedActivities.size}/${activities.size} selesai",
                                style = MaterialTheme.typography.labelMedium,
                                color = TouringBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressRatio.coerceIn(0f, 1f) },
                        color = TouringBlue,
                        trackColor = TouringBlue.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Text(
                        text = "Mode hanya lihat · Checklist hanya dapat diperbarui oleh jamaah atau ketua rombongan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (activities.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Belum ada jadwal kegiatan rombongan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ketua rombongan belum menambahkan daftar kegiatan untuk perjalanan ini.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Sudah Dilaksanakan (${completedActivities.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TouringBlue,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }

            if (completedActivities.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Belum ada kegiatan yang selesai dilaksanakan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(completedActivities, key = { "done_${it.id}" }) { activity ->
                    FamilyReadOnlyActivityCard(
                        activity = activity,
                        isCompleted = true,
                        completedAt = completedTimes[activity.id]
                    )
                }
            }

            item {
                Text(
                    text = "Akan Dilaksanakan (${upcomingActivities.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = RoadAmber,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp)
                )
            }

            if (upcomingActivities.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = TouringBlue.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, TouringBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Alhamdulillah, seluruh kegiatan rombongan telah selesai dilaksanakan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TouringBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(upcomingActivities, key = { "todo_${it.id}" }) { activity ->
                    FamilyReadOnlyActivityCard(
                        activity = activity,
                        isCompleted = false,
                        completedAt = null
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyReadOnlyActivityCard(
    activity: RoomActivityItem,
    isCompleted: Boolean,
    completedAt: String?
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) {
                TouringBlue.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isCompleted) TouringBlue else RoadAmber,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(22.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activity.timeLabel.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = activity.timeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (activity.locationNote.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = activity.locationNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isCompleted) {
                    Text(
                        text = if (!completedAt.isNullOrBlank()) {
                            "Selesai pada $completedAt"
                        } else {
                            "Sudah dilaksanakan"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = TouringBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "Belum selesai · Terjadwal",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoadAmber,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun FamilyStatusMessageView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            ),
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.12f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = iconTint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
