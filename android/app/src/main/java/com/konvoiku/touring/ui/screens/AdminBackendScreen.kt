package com.konvoiku.touring.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konvoiku.touring.data.FirestoreConvoyRepository
import com.konvoiku.touring.model.AdminRoomOverview
import com.konvoiku.touring.model.AdminUserSummary
import com.konvoiku.touring.model.GlobalAppConfig
import com.konvoiku.touring.model.UserPresenceInfo
import com.konvoiku.touring.ui.components.ConvoyAvatar

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBackendScreen(
    isVerifiedAdmin: Boolean,
    authUserEmail: String,
    isCloudConnected: Boolean = true,
    isAuthLoading: Boolean = false,
    authError: String? = null,
    authInfoMessage: String? = null,
    showOwnerDevPreviewFallback: Boolean = false,
    isMonitoringLoading: Boolean = false,
    globalConfig: GlobalAppConfig,
    presenceList: List<UserPresenceInfo>,
    rooms: List<AdminRoomOverview>,
    users: List<AdminUserSummary>,
    monitorError: String?,
    isSavingConfig: Boolean,
    configFeedback: String?,
    onGoogleSignIn: (Activity?) -> Unit = {},
    onOpenGoogleBrowserSync: () -> Unit = {},
    onOpenCustomGoogleBrowserSync: (String) -> Unit = {},
    onOpenOwnerDevPreviewSync: () -> Unit = {},
    onSubmitGoogleSyncToken: (String) -> Unit = {},
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onSaveGlobalConfig: (GlobalAppConfig) -> Unit,
    onToggleRoomStatus: (roomId: String, currentStatus: String) -> Unit,
    onResolveRoomSos: (roomId: String) -> Unit,
    onRemoveRoomMember: (roomId: String, memberUid: String) -> Unit,
    onDeleteRoom: (roomId: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showCustomBridgeInput by remember { mutableStateOf(false) }
    var customBridgeUrl by remember { mutableStateOf("") }
    var showManualTokenInput by remember { mutableStateOf(false) }
    var manualSyncToken by remember { mutableStateOf("") }

    DisposableEffect(isVerifiedAdmin) {
        if (isVerifiedAdmin) {
            onStartMonitoring()
        }
        onDispose {
            onStopMonitoring()
        }
    }

    var activeTab by remember { mutableStateOf(0) } // 0 = Pantau Aktif, 1 = Pengaturan Backend
    var expandedRoomId by remember { mutableStateOf<String?>(null) }
    var userQuery by remember { mutableStateOf("") }

    var draftOffRoute by remember(globalConfig) {
        mutableStateOf(globalConfig.defaultOffRouteToleranceMeters.toString())
    }
    var draftStraggler by remember(globalConfig) {
        mutableStateOf(globalConfig.defaultMaxStragglerDistanceMeters.toString())
    }
    var draftSoundAlert by remember(globalConfig) {
        mutableStateOf(globalConfig.defaultSoundAlertEnabled)
    }
    var draftMaintenanceMode by remember(globalConfig) {
        mutableStateOf(globalConfig.maintenanceMode)
    }
    var draftMaintenanceMsg by remember(globalConfig) {
        mutableStateOf(globalConfig.maintenanceMessage)
    }
    var draftAnnouncementActive by remember(globalConfig) {
        mutableStateOf(globalConfig.announcementActive)
    }
    var draftAnnouncementTitle by remember(globalConfig) {
        mutableStateOf(globalConfig.announcementTitle)
    }
    var draftAnnouncementMsg by remember(globalConfig) {
        mutableStateOf(globalConfig.announcementMessage)
    }
    var draftAllowGuest by remember(globalConfig) {
        mutableStateOf(globalConfig.allowGuestMode)
    }
    var draftAllowNewRooms by remember(globalConfig) {
        mutableStateOf(globalConfig.allowNewRooms)
    }

    LaunchedEffect(globalConfig) {
        draftOffRoute = globalConfig.defaultOffRouteToleranceMeters.toString()
        draftStraggler = globalConfig.defaultMaxStragglerDistanceMeters.toString()
        draftSoundAlert = globalConfig.defaultSoundAlertEnabled
        draftMaintenanceMode = globalConfig.maintenanceMode
        draftMaintenanceMsg = globalConfig.maintenanceMessage
        draftAnnouncementActive = globalConfig.announcementActive
        draftAnnouncementTitle = globalConfig.announcementTitle
        draftAnnouncementMsg = globalConfig.announcementMessage
        draftAllowGuest = globalConfig.allowGuestMode
        draftAllowNewRooms = globalConfig.allowNewRooms
    }

    val nowMs = System.currentTimeMillis()
    val onlineUsers = presenceList.filter { nowMs - it.lastSeenMs <= 180_000L }
    val travelingOnline = onlineUsers.filter { it.isTraveling }
    val activeRooms = rooms.filter { it.status == "ACTIVE" }
    val activeSosCount = activeRooms.count { it.sosActive }
    val webCount = onlineUsers.count { it.platform == "web" }
    val androidCount = onlineUsers.count { it.platform == "android" }

    val filteredUsers = users.filter { u ->
        val q = userQuery.trim().lowercase()
        q.isEmpty() ||
            u.fullName.lowercase().contains(q) ||
            u.email.lowercase().contains(q) ||
            u.clubName.lowercase().contains(q) ||
            u.licensePlate.lowercase().contains(q)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Admin Backend Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kontrol KonvoiKu · Realtime Firestore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (isVerifiedAdmin && activeTab == 1) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                val offRoute = draftOffRoute.toIntOrNull()?.coerceIn(10, 5000) ?: 75
                                val straggler = draftStraggler.toIntOrNull()?.coerceIn(100, 50000) ?: 1000
                                onSaveGlobalConfig(
                                    globalConfig.copy(
                                        defaultOffRouteToleranceMeters = offRoute,
                                        defaultMaxStragglerDistanceMeters = straggler,
                                        defaultSoundAlertEnabled = draftSoundAlert,
                                        maintenanceMode = draftMaintenanceMode,
                                        maintenanceMessage = draftMaintenanceMsg,
                                        announcementActive = draftAnnouncementActive,
                                        announcementTitle = draftAnnouncementTitle,
                                        announcementMessage = draftAnnouncementMsg,
                                        allowGuestMode = draftAllowGuest,
                                        allowNewRooms = draftAllowNewRooms
                                    )
                                )
                            },
                            enabled = !isSavingConfig,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isSavingConfig) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyimpan...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Save,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Simpan Pengaturan Backend",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isVerifiedAdmin) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = Color(0xFFD97706)
                            )
                            Text(
                                text = "Verifikasi Admin Google Diperlukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            color = if (isCloudConnected) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isCloudConnected) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCloudConnected) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff,
                                    contentDescription = null,
                                    tint = if (isCloudConnected) Color(0xFF047857) else Color(0xFFB91C1C),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isCloudConnected) {
                                        "Firebase Cloud & Firestore siap di perangkat."
                                    } else {
                                        "Firebase Cloud belum terhubung. Periksa koneksi internet."
                                    },
                                    fontSize = 11.sp,
                                    color = if (isCloudConnected) Color(0xFF065F46) else Color(0xFF991B1B),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Text(
                            text = "Panel Admin Backend membaca telemetri semua pengendara secara real-time dan mengubah pengaturan global. Masuk menggunakan akun Google terverifikasi ${FirestoreConvoyRepository.ADMIN_EMAIL} (sesi saat ini: ${authUserEmail.ifBlank { "belum login Google/Email" }}).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!authInfoMessage.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = authInfoMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        val diagnostic = authError ?: monitorError
                        if (!diagnostic.isNullOrBlank()) {
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = diagnostic,
                                    fontSize = 11.sp,
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (showOwnerDevPreviewFallback) {
                            OutlinedButton(
                                onClick = onOpenOwnerDevPreviewSync,
                                enabled = !isAuthLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 46.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInBrowser,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Buka Dev Preview (Khusus Pemilik)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB45309),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = { onGoogleSignIn(context.findActivity()) },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyinkronkan...", fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(
                                    text = "Masuk Admin dengan Google",
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (customBridgeUrl.isNotBlank()) {
                                    onOpenCustomGoogleBrowserSync(customBridgeUrl)
                                } else {
                                    onOpenGoogleBrowserSync()
                                }
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2563EB))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Verifikasi Google via Browser",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB),
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "Verifikasi via browser berjalan langsung melalui halaman login Google lokal di Chrome tanpa perlu deploy web.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showCustomBridgeInput = !showCustomBridgeInput }
                            ) {
                                Text(
                                    text = if (showCustomBridgeInput) "Tutup URL kustom" else "URL web kustom?",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { showManualTokenInput = !showManualTokenInput }
                            ) {
                                Text(
                                    text = if (showManualTokenInput) "Tutup tempel token" else "Sudah login di browser?",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2563EB)
                                )
                            }
                        }

                        if (showCustomBridgeInput) {
                            OutlinedTextField(
                                value = customBridgeUrl,
                                onValueChange = { customBridgeUrl = it },
                                label = { Text("URL Web KonvoiKu (opsional)") },
                                placeholder = { Text("https://...run.app") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        if (showManualTokenInput) {
                            OutlinedTextField(
                                value = manualSyncToken,
                                onValueChange = { manualSyncToken = it },
                                label = { Text("Tempel link konvoiku://auth atau token") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Button(
                                onClick = { onSubmitGoogleSyncToken(manualSyncToken) },
                                enabled = !isAuthLoading && manualSyncToken.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Terapkan Verifikasi Admin", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Kembali ke Beranda")
                        }
                    }
                }
                return@Column
            }

            // Live Firestore Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Firestore Live Backend",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Surface(
                                color = Color(0xFF14532D),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SYNC REAL-TIME",
                                    color = Color(0xFF86EFAC),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Operator: ${authUserEmail.ifBlank { FirestoreConvoyRepository.ADMIN_EMAIL }}",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onStartMonitoring) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Muat ulang data",
                            tint = Color(0xFF93C5FD)
                        )
                    }
                }
            }

            if (isMonitoringLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Menyinkronkan data presence, room, dan pengguna...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Pantau Aktif", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Pengaturan Backend", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            if (monitorError != null) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = monitorError,
                            fontSize = 12.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                }
            }

            if (activeTab == 0) {
                // 2x2 KPI Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Online Sekarang",
                        value = onlineUsers.size.toString(),
                        subtitle = "Web $webCount · Android $androidCount",
                        accent = MaterialTheme.colorScheme.primary
                    )
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Sedang Jalan",
                        value = travelingOnline.size.toString(),
                        subtitle = if (activeSosCount > 0) "$activeSosCount SOS Aktif!" else "Dalam perjalanan",
                        accent = if (activeSosCount > 0) Color(0xFFDC2626) else Color(0xFF15803D)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Room Aktif",
                        value = activeRooms.size.toString(),
                        subtitle = "Total ${rooms.size} room",
                        accent = MaterialTheme.colorScheme.secondary
                    )
                    AdminKpiCard(
                        modifier = Modifier.weight(1f),
                        title = "Akun Terdaftar",
                        value = users.size.toString(),
                        subtitle = "Tersimpan di Cloud",
                        accent = Color(0xFF475569)
                    )
                }

                // Online Presence Feed
                AdminSectionCard(
                    title = "Pengguna Aktif Realtime (${onlineUsers.size})",
                    icon = Icons.Rounded.Person
                ) {
                    if (onlineUsers.isEmpty()) {
                        Text(
                            text = "Belum ada heartbeat perangkat yang aktif dalam 3 menit terakhir.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        onlineUsers.take(15).forEach { p ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (p.platform == "android") {
                                            Icons.Rounded.Smartphone
                                        } else {
                                            Icons.Rounded.Computer
                                        },
                                        contentDescription = null,
                                        tint = if (p.isTraveling) Color(0xFFEA580C) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.fullName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = p.email.ifBlank { p.uid.take(12) },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (p.isTraveling) {
                                                "Jalan · ${p.speedKmh} km/j"
                                            } else {
                                                "Siaga"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (p.isTraveling) Color(0xFFEA580C) else Color(0xFF15803D)
                                        )
                                        Text(
                                            text = p.lastSeenLabel,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active & Finished Convoy Rooms
                AdminSectionCard(
                    title = "Kontrol Room Konvoi (${rooms.size})",
                    icon = Icons.Rounded.Navigation
                ) {
                    if (rooms.isEmpty()) {
                        Text(
                            text = "Belum ada room konvoi tercatat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        rooms.forEach { room ->
                            val isExpanded = expandedRoomId == room.roomId
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (room.sosActive) Color(0xFFEF4444) else MaterialTheme.colorScheme.outline
                                ),
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = room.roomId,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Surface(
                                                    color = if (room.status == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFE2E8F0),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = room.status,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (room.status == "ACTIVE") Color(0xFF15803D) else Color(0xFF475569),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (room.sosActive) {
                                                    Surface(
                                                        color = Color(0xFFFEE2E2),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "SOS",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFB91C1C),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = room.roomTitle,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Tujuan: ${room.destinationName} · ${room.members.size} anggota",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                expandedRoomId = if (isExpanded) null else room.roomId
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                                contentDescription = "Rincian Room"
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onToggleRoomStatus(room.roomId, room.status) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PowerSettingsNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (room.status == "ACTIVE") "Akhiri" else "Aktifkan",
                                                fontSize = 11.sp
                                            )
                                        }

                                        if (room.sosActive) {
                                            Button(
                                                onClick = { onResolveRoomSos(room.roomId) },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                            ) {
                                                Text("Reset SOS", fontSize = 11.sp)
                                            }
                                        }

                                        IconButton(onClick = { onDeleteRoom(room.roomId) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = "Hapus Room",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    if (isExpanded) {
                                        HorizontalDivider()
                                        if (room.members.isEmpty()) {
                                            Text(
                                                text = "Tidak ada anggota aktif di room ini.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            room.members.forEach { m ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.DirectionsCar,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Column {
                                                            Text(
                                                                text = "${m.fullName} (${m.role.label})",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Medium
                                                            )
                                                            Text(
                                                                text = "${m.vehicleDetail} · ${m.mapPosition.speedKmh} km/j · ${m.conditionStatus.label}",
                                                                fontSize = 10.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = { onRemoveRoomMember(room.roomId, m.id) }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Delete,
                                                            contentDescription = "Keluarkan",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Registered Users Directory
                AdminSectionCard(
                    title = "Direktori Pengguna (${users.size})",
                    icon = Icons.Rounded.Group
                ) {
                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        placeholder = { Text("Cari nama, email, klub, plat...", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    filteredUsers.take(25).forEach { u ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.background,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ConvoyAvatar(
                                    preset = u.avatarPreset,
                                    photoUri = u.avatarPhotoUri,
                                    displayName = u.fullName,
                                    size = 38.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = u.fullName,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${u.email.ifBlank { "-" }} · ${u.authProvider.label}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val meta = listOf(u.vehicleDetail, u.licensePlate, u.clubName)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" · ")
                                    if (meta.isNotBlank()) {
                                        Text(
                                            text = meta,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Tab 1: Global Backend Settings
                if (configFeedback != null) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = configFeedback,
                                fontSize = 12.sp,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }

                AdminSectionCard(
                    title = "Parameter Default Konvoi Global",
                    icon = Icons.Rounded.Settings
                ) {
                    OutlinedTextField(
                        value = draftOffRoute,
                        onValueChange = { draftOffRoute = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Toleransi Keluar Jalur Default (meter)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftStraggler,
                        onValueChange = { draftStraggler = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Batas Jarak Tertinggal Default (meter)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    AdminSwitchRow(
                        label = "Aktifkan Peringatan Suara Default",
                        subtitle = "Berlaku otomatis untuk room baru",
                        checked = draftSoundAlert,
                        onCheckedChange = { draftSoundAlert = it }
                    )
                }

                AdminSectionCard(
                    title = "Pengumuman Global (Banner)",
                    icon = Icons.Rounded.Campaign
                ) {
                    AdminSwitchRow(
                        label = "Tampilkan Pengumuman Global",
                        subtitle = "Muncul di Login & Dashboard Web/Android",
                        checked = draftAnnouncementActive,
                        onCheckedChange = { draftAnnouncementActive = it }
                    )
                    OutlinedTextField(
                        value = draftAnnouncementTitle,
                        onValueChange = { draftAnnouncementTitle = it },
                        label = { Text("Judul Pengumuman") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftAnnouncementMsg,
                        onValueChange = { draftAnnouncementMsg = it },
                        label = { Text("Isi Pesan Pengumuman") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                AdminSectionCard(
                    title = "Kontrol Akses & Pemeliharaan",
                    icon = Icons.Rounded.Build
                ) {
                    AdminSwitchRow(
                        label = "Izinkan Pembuatan Room Baru",
                        subtitle = "Matikan untuk menghentikan pembuatan konvoi baru sementara",
                        checked = draftAllowNewRooms,
                        onCheckedChange = { draftAllowNewRooms = it }
                    )
                    AdminSwitchRow(
                        label = "Izinkan Mode Tamu / Lokal",
                        subtitle = "Kontrol opsi masuk tanpa akun cloud",
                        checked = draftAllowGuest,
                        onCheckedChange = { draftAllowGuest = it }
                    )
                    AdminSwitchRow(
                        label = "Mode Pemeliharaan (Maintenance)",
                        subtitle = "Tampilkan banner pemeliharaan di seluruh aplikasi",
                        checked = draftMaintenanceMode,
                        onCheckedChange = { draftMaintenanceMode = it }
                    )
                    if (draftMaintenanceMode) {
                        OutlinedTextField(
                            value = draftMaintenanceMsg,
                            onValueChange = { draftMaintenanceMsg = it },
                            label = { Text("Pesan Mode Pemeliharaan") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AdminKpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun AdminSwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
