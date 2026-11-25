package com.konvoiku.touring.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.TwoWheeler
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konvoiku.touring.data.TomTomApiService
import com.konvoiku.touring.data.TomTomPlaceResult
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.ConvoyRouteConfig
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePoint
import com.konvoiku.touring.model.RoutePreference
import com.konvoiku.touring.model.TourRoom
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.model.VehicleType
import com.konvoiku.touring.ui.components.ConvoyAvatar
import com.konvoiku.touring.ui.components.TomTomConvoyMap
import com.konvoiku.touring.ui.components.TomTomMapPin
import com.konvoiku.touring.ui.components.TomTomMapStyle
import com.konvoiku.touring.ui.components.rememberTomTomMapController
import com.konvoiku.touring.util.GeoUtils
import com.konvoiku.touring.util.LatLngGeo
import com.konvoiku.touring.viewmodel.ConvoyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourLeaderScreen(
    activeRoom: TourRoom?,
    routeConfig: ConvoyRouteConfig,
    destinationPresets: List<ConvoyViewModel.DestinationPreset>,
    vehicleType: VehicleType = VehicleType.MOTOR,
    userGpsLat: Double? = null,
    userGpsLng: Double? = null,
    isCreatingRoom: Boolean = false,
    roomSetupError: String? = null,
    activeWebBaseUrl: String = "https://ais-pre-mbwxrxf647cphhl5i4ggug-588877716211.asia-southeast1.run.app",
    onPickOriginLatLng: (latitude: Double, longitude: Double, placeName: String, addressLabel: String) -> Unit = { _, _, _, _ -> },
    onUseGpsAsOrigin: () -> Boolean = { false },
    onSelectPreset: (ConvoyViewModel.DestinationPreset) -> Unit,
    onPickDestinationLatLng: (latitude: Double, longitude: Double, placeName: String, addressLabel: String) -> Unit,
    onSelectRoutePreference: (RoutePreference) -> Unit = {},
    onRouteComputed: (Int, List<RoutePoint>, List<RouteInstruction>) -> Unit = { _, _, _ -> },
    onUpdateWaypoints: (List<RoutePoint>, String?) -> Unit,
    onResetDefaultWaypoints: () -> Unit,
    onAddCheckpointAtPoint: (String, Float, Float) -> Unit,
    onRemoveCheckpoint: (String) -> Unit,
    onUpdateMaxStragglerDistance: (Int) -> Unit,
    onToggleSoundAlert: () -> Unit,
    onCreateRoom: (String) -> Unit,
    onCreateLocalRoom: (String) -> Unit = {},
    onSimulateJoin: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onEnterMapPlaceholder: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(0) }
    var showSetupStepper by remember(activeRoom) { mutableStateOf(activeRoom == null) }
    var mapPickerMode by remember { mutableStateOf<String?>(null) } // "origin" | "destination" | null
    var tourNameDraft by remember { mutableStateOf(activeRoom?.roomTitle ?: "Konvoi Puncak Pass Weekend") }
    var viaPoints by remember { mutableStateOf<List<RoutePoint>>(emptyList()) }
    var isComputingPreviewRoute by remember { mutableStateOf(false) }
    var previewDurationMin by remember { mutableStateOf<Int?>(null) }

    var originSearchQuery by remember { mutableStateOf("") }
    var originSearchResults by remember { mutableStateOf<List<TomTomPlaceResult>>(emptyList()) }
    var isSearchingOrigin by remember { mutableStateOf(false) }

    var destSearchQuery by remember { mutableStateOf("") }
    var destSearchResults by remember { mutableStateOf<List<TomTomPlaceResult>>(emptyList()) }
    var isSearchingDest by remember { mutableStateOf(false) }

    LaunchedEffect(originSearchQuery) {
        val q = originSearchQuery.trim()
        if (q.length < 2) {
            originSearchResults = emptyList()
            isSearchingOrigin = false
            return@LaunchedEffect
        }
        isSearchingOrigin = true
        delay(320)
        originSearchResults = TomTomApiService.searchPlaces(q)
        isSearchingOrigin = false
    }

    LaunchedEffect(destSearchQuery) {
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

    val stepTitles = listOf("Start", "Tujuan", "Rute", "Jarak")

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (showSetupStepper) "Buat Konvoi Baru" else "Ruang Tour Leader",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (showSetupStepper) "Langkah ${currentStep + 1} dari 4 · ${stepTitles.getOrElse(currentStep) { "Jarak" }}" else "Room ID & Peserta Konvoi",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (activeRoom != null) {
                        TextButton(onClick = { showSetupStepper = !showSetupStepper }) {
                            Text(if (showSetupStepper) "Lihat Room" else "Edit Rute")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!showSetupStepper && activeRoom != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp
                ) {
                    Button(
                        onClick = onEnterMapPlaceholder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Map,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buka Peta Konvoi Live",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showSetupStepper) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Stepper Header Bar
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stepTitles.forEachIndexed { idx, label ->
                                val isActive = idx == currentStep
                                val isDone = idx < currentStep
                                Surface(
                                    onClick = { currentStep = idx },
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        isActive -> MaterialTheme.colorScheme.primary
                                        isDone -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (isDone) "✓" else "${idx + 1}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isActive -> Color.White
                                                isDone -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = when {
                                                isActive -> Color.White
                                                isDone -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (currentStep) {
                                // Step 1: Menentukan Titik Keberangkatan (Start)
                                0 -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color(0xFFEA580C))
                                        Text("1. Titik Keberangkatan (Start)", style = MaterialTheme.typography.titleLarge)
                                    }

                                    OutlinedTextField(
                                        value = tourNameDraft,
                                        onValueChange = { tourNameDraft = it },
                                        label = { Text("Nama Perjalanan / Tour") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            val ok = onUseGpsAsOrigin()
                                            coroutineScope.launch {
                                                if (ok) {
                                                    snackbarHostState.showSnackbar("Titik keberangkatan diset ke posisi GPS Anda saat ini.")
                                                } else {
                                                    snackbarHostState.showSnackbar("GPS perangkat belum siap. Pilih hub populer atau cari area di bawah.")
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFEA580C),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Gunakan Lokasi GPS Saya Sekarang", fontWeight = FontWeight.SemiBold)
                                    }

                                    // Inline TomTom Area Search for Start
                                    OutlinedTextField(
                                        value = originSearchQuery,
                                        onValueChange = { originSearchQuery = it },
                                        label = { Text("Cari Area / Titik Start") },
                                        placeholder = { Text("Mis. Senayan, Rest Area KM 57, Bandung...") },
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                        trailingIcon = {
                                            if (originSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    originSearchQuery = ""
                                                    originSearchResults = emptyList()
                                                }) {
                                                    Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (isSearchingOrigin || originSearchResults.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.background,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                if (isSearchingOrigin) {
                                                    Text(
                                                        text = "Mencari area start di TomTom...",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                } else {
                                                    originSearchResults.take(5).forEach { result ->
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    onPickOriginLatLng(
                                                                        result.latitude,
                                                                        result.longitude,
                                                                        result.name,
                                                                        result.address
                                                                    )
                                                                    originSearchQuery = ""
                                                                    originSearchResults = emptyList()
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Titik Start dipilih: ${result.name}")
                                                                    }
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                        ) {
                                                            Text(result.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text(result.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        HorizontalDivider()
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Atau pilih titik kumpul keberangkatan:",
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    REGIONAL_ORIGINS.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { hub ->
                                                FilterChip(
                                                    selected = routeConfig.originName == hub.name,
                                                    onClick = {
                                                        onPickOriginLatLng(
                                                            hub.latitude,
                                                            hub.longitude,
                                                            hub.name,
                                                            String.format(Locale.US, "%s · %.4f, %.4f", hub.address, hub.latitude, hub.longitude)
                                                        )
                                                    },
                                                    label = {
                                                        Text(
                                                            text = hub.name,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            fontSize = 11.sp
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { mapPickerMode = "origin" },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFFEA580C))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddLocationAlt,
                                            contentDescription = null,
                                            tint = Color(0xFFEA580C),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pilih Titik Start di TomTom Maps",
                                            color = Color(0xFFEA580C),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    val startPoint = routeConfig.waypoints.firstOrNull()
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFFF7ED),
                                        border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFEA580C),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.MyLocation,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "START · ${routeConfig.originName}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF9A3412)
                                                )
                                                Text(
                                                    text = routeConfig.originAddress,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF7C2D12),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (startPoint != null) {
                                                    Text(
                                                        text = String.format(
                                                            Locale.US,
                                                            "Koordinat Start: %.4f, %.4f",
                                                            startPoint.latitude,
                                                            startPoint.longitude
                                                        ),
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFFEA580C)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Step 2: Menentukan Tujuan Akhir
                                1 -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFF16A34A))
                                        Text("2. Menentukan Tujuan Akhir", style = MaterialTheme.typography.titleLarge)
                                    }

                                    // Inline TomTom Area Search for Destination
                                    OutlinedTextField(
                                        value = destSearchQuery,
                                        onValueChange = { destSearchQuery = it },
                                        label = { Text("Cari Area / Lokasi Tujuan") },
                                        placeholder = { Text("Mis. Puncak Pass, Lembang, Ciwidey...") },
                                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                        trailingIcon = {
                                            if (destSearchQuery.isNotEmpty()) {
                                                IconButton(onClick = {
                                                    destSearchQuery = ""
                                                    destSearchResults = emptyList()
                                                }) {
                                                    Icon(Icons.Rounded.Close, contentDescription = "Hapus pencarian")
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (isSearchingDest || destSearchResults.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.background,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column {
                                                if (isSearchingDest) {
                                                    Text(
                                                        text = "Mencari area tujuan di TomTom...",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(12.dp)
                                                    )
                                                } else {
                                                    destSearchResults.take(5).forEach { result ->
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    viaPoints = emptyList()
                                                                    onPickDestinationLatLng(
                                                                        result.latitude,
                                                                        result.longitude,
                                                                        result.name,
                                                                        result.address
                                                                    )
                                                                    destSearchQuery = ""
                                                                    destSearchResults = emptyList()
                                                                    coroutineScope.launch {
                                                                        snackbarHostState.showSnackbar("Lokasi tujuan dipilih: ${result.name}")
                                                                    }
                                                                }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                                        ) {
                                                            Text(result.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text(result.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        HorizontalDivider()
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Pilih Destinasi Utama dari ${routeConfig.originName}:",
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    destinationPresets.chunked(2).forEach { pair ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pair.forEach { preset ->
                                                FilterChip(
                                                    selected = routeConfig.destinationName == preset.name,
                                                    onClick = {
                                                        viaPoints = emptyList()
                                                        onSelectPreset(preset)
                                                    },
                                                    label = {
                                                        Text(
                                                            text = preset.name,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { mapPickerMode = "destination" },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, Color(0xFF16A34A))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AddLocationAlt,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Pilih Tujuan di TomTom Maps",
                                            color = Color(0xFF16A34A),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    val destPoint = routeConfig.waypoints.lastOrNull()
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Dari: ${routeConfig.originName}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF16A34A),
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Flag,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = routeConfig.destinationName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                                    )
                                                    Text(
                                                        text = routeConfig.destinationAddress,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (destPoint != null) {
                                                        Text(
                                                            text = String.format(
                                                                Locale.US,
                                                                "Tujuan: %.4f, %.4f · Est. %d km",
                                                                destPoint.latitude,
                                                                destPoint.longitude,
                                                                routeConfig.totalRouteMeters / 1000
                                                            ),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Step 3: Memilih & Menggambar Rute yang Diambil
                                2 -> {
                                    val startPt = routeConfig.waypoints.firstOrNull()
                                    val endPt = routeConfig.waypoints.lastOrNull()
                                    val viaKey = remember(startPt, endPt, viaPoints, routeConfig.routePreference, vehicleType) {
                                        val viaSig = viaPoints.joinToString("|") {
                                            String.format(Locale.US, "%.4f,%.4f", it.latitude, it.longitude)
                                        }
                                        String.format(
                                            Locale.US,
                                            "%s|%s|%.4f,%.4f->%.4f,%.4f|%s",
                                            vehicleType.name,
                                            routeConfig.routePreference.id,
                                            startPt?.latitude ?: 0.0,
                                            startPt?.longitude ?: 0.0,
                                            endPt?.latitude ?: 0.0,
                                            endPt?.longitude ?: 0.0,
                                            viaSig
                                        )
                                    }

                                    LaunchedEffect(viaKey) {
                                        if (startPt != null && endPt != null) {
                                            isComputingPreviewRoute = true
                                            val inputWaypoints = listOf(startPt) + viaPoints + listOf(endPt)
                                            val travelMode = if (vehicleType == VehicleType.MOBIL) "car" else "motorcycle"
                                            val res = TomTomApiService.calculateRoute(
                                                waypoints = inputWaypoints,
                                                travelMode = travelMode,
                                                routePreference = routeConfig.routePreference
                                            )
                                            isComputingPreviewRoute = false
                                            if (res != null && res.path.size >= 2) {
                                                previewDurationMin = (res.durationMillis / 60_000L).toInt().coerceAtLeast(1)
                                                val snapped = GeoUtils.toRoutePoints(
                                                    GeoUtils.downsampleLatLng(
                                                        res.path.map { LatLngGeo(it.latitude, it.longitude) },
                                                        maxPoints = 90
                                                    )
                                                )
                                                onRouteComputed(res.distanceMeters, snapped, res.instructions)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Text("3. Pilih & Gambar Rute", style = MaterialTheme.typography.titleLarge)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (viaPoints.isNotEmpty()) {
                                                TextButton(onClick = { viaPoints = viaPoints.dropLast(1) }) {
                                                    Text("Hapus Titik")
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    viaPoints = emptyList()
                                                    onResetDefaultWaypoints()
                                                }
                                            ) {
                                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Reset")
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Pilih jenis rute lalu ketuk peta jika ingin menambah titik belokan jalur (${routeConfig.originName} → ${routeConfig.destinationName}).",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RoutePreference.entries.forEach { pref ->
                                            FilterChip(
                                                selected = routeConfig.routePreference == pref,
                                                onClick = { onSelectRoutePreference(pref) },
                                                label = {
                                                    Text(
                                                        text = pref.label,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (isComputingPreviewRoute) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Text(
                                                    text = "Menghitung rute jalan TomTom (${routeConfig.routePreference.label})...",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF1E40AF)
                                                )
                                            } else {
                                                val distKmText = String.format(Locale.US, "%.1f km", routeConfig.totalRouteMeters / 1000.0)
                                                val durText = previewDurationMin?.let { " · Est. $it mnt" } ?: ""
                                                val navCount = if (routeConfig.routeInstructions.isNotEmpty()) {
                                                    " · ${routeConfig.routeInstructions.size} panduan belokan"
                                                } else ""
                                                Text(
                                                    text = "$distKmText$durText · ${routeConfig.routePreference.label}$navCount",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF1E3A8A)
                                                )
                                            }
                                        }
                                    }

                                    val previewLat = if (routeConfig.waypoints.isNotEmpty()) {
                                        routeConfig.waypoints.map { it.latitude }.average()
                                    } else -6.674
                                    val previewLng = if (routeConfig.waypoints.isNotEmpty()) {
                                        routeConfig.waypoints.map { it.longitude }.average()
                                    } else 106.925
                                    val routeEditorController = rememberTomTomMapController()
                                    val editorPins = remember(startPt, endPt, viaPoints) {
                                        buildList<TomTomMapPin> {
                                            if (startPt != null) {
                                                add(TomTomMapPin.WaypointBadgePin(startPt.latitude, startPt.longitude, "Start", 0xFFEA580C.toInt()))
                                            }
                                            viaPoints.forEachIndexed { idx, pt ->
                                                add(TomTomMapPin.WaypointBadgePin(pt.latitude, pt.longitude, "Via ${idx + 1}", 0xFF1D4ED8.toInt()))
                                            }
                                            if (endPt != null) {
                                                add(TomTomMapPin.WaypointBadgePin(endPt.latitude, endPt.longitude, "Tujuan", 0xFF16A34A.toInt()))
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(235.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFEEF2F6))
                                    ) {
                                        TomTomConvoyMap(
                                            modifier = Modifier.fillMaxSize(),
                                            controller = routeEditorController,
                                            initialLat = previewLat,
                                            initialLng = previewLng,
                                            initialZoom = 11.5,
                                            polylinePoints = routeConfig.waypoints,
                                            pins = editorPins,
                                            onMapTap = { lat, lng ->
                                                val newPt = RoutePoint(
                                                    xFraction = 0.5f,
                                                    yFraction = 0.5f,
                                                    latitude = lat,
                                                    longitude = lng
                                                )
                                                viaPoints = viaPoints + newPt
                                                if (startPt != null && endPt != null) {
                                                    onUpdateWaypoints(listOf(startPt) + viaPoints + listOf(endPt), null)
                                                }
                                            }
                                        )
                                    }

                                    Text(
                                        text = "${viaPoints.size} titik singgah tambahan · Ketuk peta untuk belok lewat jalur tertentu",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Step 4: Mengatur Batas Jarak & Konfirmasi Konvoi
                                3 -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("4. Batas Jarak & Konfirmasi", style = MaterialTheme.typography.titleLarge)
                                    }

                                    val kmValue = (routeConfig.maxStragglerDistanceMeters / 1000f).coerceIn(0.5f, 5.0f)

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Jarak Maksimal dari Sweeper",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = String.format(Locale.US, "%.1f km", kmValue),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "Peringatan aktif otomatis jika jarak anggota paling belakang melebihi batas ini.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("0.5 km", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("2.5 km", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("5.0 km", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Peringatan Suara Otomatis", fontWeight = FontWeight.SemiBold)
                                            Text("Aktifkan nada peringatan bila anggota tertinggal atau keluar jalur.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = routeConfig.soundAlertEnabled, onCheckedChange = { onToggleSoundAlert() })
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Start: ${routeConfig.originName}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Rounded.Flag, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                                Text(
                                                    text = "Tujuan: ${routeConfig.destinationName}",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "Rute ${routeConfig.routePreference.label} · ${String.format(Locale.US, "%.1f km", routeConfig.totalRouteMeters / 1000.0)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            if (!roomSetupError.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = roomSetupError,
                                        color = Color(0xFFB91C1C),
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (currentStep > 0) {
                                    OutlinedButton(
                                        onClick = { currentStep-- },
                                        enabled = !isCreatingRoom,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Sebelumnya")
                                    }
                                }
                                if (currentStep < 3) {
                                    Button(
                                        onClick = { currentStep++ },
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Langkah Berikutnya")
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            onCreateRoom(tourNameDraft.ifBlank { "Konvoi Touring Akhir Pekan" })
                                        },
                                        enabled = !isCreatingRoom,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        if (isCreatingRoom) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Membuat Room...", fontWeight = FontWeight.SemiBold)
                                        } else {
                                            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buat Room & Mulai", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            if (currentStep == 3) {
                                OutlinedButton(
                                    onClick = {
                                        onCreateLocalRoom(tourNameDraft.ifBlank { "Konvoi Touring Akhir Pekan" })
                                    },
                                    enabled = !isCreatingRoom,
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Atau Buat Room Lokal (Offline)",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(28.dp)) }
            } else if (activeRoom != null) {
                val webShareLink = "${activeWebBaseUrl.trimEnd('/')}/?room=${Uri.encode(activeRoom.roomId)}"
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeRoom.roomTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = activeRoom.roomId,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(activeRoom.roomId))
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Kode Room ${activeRoom.roomId} disalin")
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salin Kode")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            // Link Browser (?room=...)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Link Undangan Web", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = webShareLink,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(webShareLink))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Link undangan web disalin")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin Link Web", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            // Link APK (konvoiku.id/join/...)
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Link Deep-Link APK", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = activeRoom.inviteLink,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(activeRoom.inviteLink))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Link undangan APK disalin")
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Salin Link APK", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val shareBody = "Gabung konvoi \"${activeRoom.roomTitle}\" di KonvoiKu!\nKode Room: ${activeRoom.roomId}\nLink Web: $webShareLink\nLink Aplikasi: ${activeRoom.inviteLink}"
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Undangan Konvoi ${activeRoom.roomId}")
                                        putExtra(Intent.EXTRA_TEXT, shareBody)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Bagikan Link & Kode Room"))
                                },
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bagikan Link & Kode Room", fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onCreateRoom(activeRoom.roomTitle) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Buat ID Baru")
                                }
                                if (!activeRoom.isSyncedRoom) {
                                    OutlinedButton(
                                        onClick = onSimulateJoin,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Simulasi")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Daftar Sementara Anggota (${activeRoom.members.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                items(activeRoom.members, key = { it.id }) { member ->
                    MemberListCard(
                        member = member,
                        onRemove = if (member.role != UserRole.TOUR_LEADER) {
                            { onRemoveMember(member.id) }
                        } else null
                    )
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }

    if (mapPickerMode != null) {
        val isOriginMode = mapPickerMode == "origin"
        val currentPoint = if (isOriginMode) {
            routeConfig.waypoints.firstOrNull() ?: RoutePoint(0.14f, 0.86f, -6.7100, 106.8500)
        } else {
            routeConfig.waypoints.lastOrNull() ?: RoutePoint(0.86f, 0.14f, -6.7003, 106.9960)
        }
        DestinationMapPickerDialog(
            isOriginMode = isOriginMode,
            initialLat = currentPoint.latitude,
            initialLng = currentPoint.longitude,
            initialName = if (isOriginMode) routeConfig.originName else routeConfig.destinationName,
            userGpsLat = userGpsLat,
            userGpsLng = userGpsLng,
            onDismiss = { mapPickerMode = null },
            onConfirm = { lat, lng, placeName, addressLabel ->
                if (isOriginMode) {
                    onPickOriginLatLng(lat, lng, placeName, addressLabel)
                    mapPickerMode = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Titik Start disimpan: $placeName")
                    }
                } else {
                    viaPoints = emptyList()
                    onPickDestinationLatLng(lat, lng, placeName, addressLabel)
                    mapPickerMode = null
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Lokasi tujuan disimpan: $placeName")
                    }
                }
            }
        )
    }
    }
}

@Composable
private fun MemberListCard(
    member: ConvoyMember,
    onRemove: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                ConvoyAvatar(
                    preset = member.avatarPreset,
                    photoUri = member.avatarPhotoUri,
                    displayName = member.fullName,
                    size = 42.dp,
                    borderColor = if (member.role == UserRole.TOUR_LEADER) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    }
                )
                Surface(
                    color = if (member.role == UserRole.TOUR_LEADER) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (member.vehicleType == VehicleType.MOBIL) Icons.Rounded.DirectionsCar else Icons.Rounded.TwoWheeler,
                            contentDescription = member.vehicleType.label,
                            tint = if (member.role == UserRole.TOUR_LEADER) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${member.role.label} · ${member.vehicleType.label} · ${member.licensePlate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Keluarkan Anggota")
                }
            }
        }
    }
}

private data class MapLandmark(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

private val REGIONAL_ORIGINS = listOf(
    MapLandmark("Simpang Gadog", "Jl. Raya Ciawi - Gadog KM 45", -6.6520, 106.8680),
    MapLandmark("Rest Area KM 57", "Tol Jakarta-Cikampek KM 57", -6.3690, 107.3590),
    MapLandmark("Gerbang Tol Pasteur", "Jl. Dr. Djunjunan, Bandung", -6.8902, 107.5815),
    MapLandmark("Cilandak Town Square", "Jl. TB Simatupang Kav 17, Jaksel", -6.2913, 106.7988)
)

private val REGIONAL_LANDMARKS = listOf(
    MapLandmark("Puncak Pass Resort", "Jl. Raya Puncak KM 90, Cisarua", -6.7003, 106.9960),
    MapLandmark("Taman Safari Cisarua", "Jl. Kapten Harun Kabir No. 724", -6.7124, 106.9502),
    MapLandmark("Rest Area KM 57", "Tol Jakarta-Cikampek KM 57", -6.3690, 107.3590),
    MapLandmark("Lembang Highland", "Jl. Raya Lembang No. 12, Bandung", -6.8120, 107.6180),
    MapLandmark("Simpang Gadog", "Jl. Raya Ciawi - Gadog KM 45", -6.6520, 106.8680),
    MapLandmark("Ciwidey Kawah Putih", "Sugihmukti, Pasirjambu, Bandung", -7.1660, 107.4020)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationMapPickerDialog(
    isOriginMode: Boolean = false,
    initialLat: Double,
    initialLng: Double,
    initialName: String,
    userGpsLat: Double?,
    userGpsLng: Double?,
    onDismiss: () -> Unit,
    onConfirm: (latitude: Double, longitude: Double, placeName: String, addressLabel: String) -> Unit
) {
    var pinLatLng by remember { mutableStateOf(LatLngGeo(initialLat, initialLng)) }
    var placeName by remember { mutableStateOf(initialName) }
    var resolvedAddress by remember {
        mutableStateOf(String.format(Locale.US, "Koordinat TomTom · %.4f, %.4f", initialLat, initialLng))
    }
    var mapStyle by remember { mutableStateOf(TomTomMapStyle.ROAD) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TomTomPlaceResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val mapController = rememberTomTomMapController()

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(320)
        searchResults = TomTomApiService.searchPlaces(q)
        isSearching = false
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (isOriginMode) "Pilih Titik Start di TomTom Maps" else "Pilih Tujuan Akhir di TomTom Maps",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Cari tempat atau ketuk lokasi di peta",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Tutup Peta")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                mapStyle = if (mapStyle == TomTomMapStyle.ROAD) TomTomMapStyle.SATELLITE else TomTomMapStyle.ROAD
                            }
                        ) {
                            Text(if (mapStyle == TomTomMapStyle.ROAD) "Satelit" else "Peta")
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
                // TomTom Places Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari lokasi di TomTom (mis. Puncak, Lembang)...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )

                if (isSearching || searchResults.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column {
                            if (isSearching) {
                                Text(
                                    text = "Mencari di TomTom...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                searchResults.take(4).forEach { result ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                pinLatLng = LatLngGeo(result.latitude, result.longitude)
                                                placeName = result.name
                                                resolvedAddress = result.address
                                                searchQuery = ""
                                                searchResults = emptyList()
                                                mapController.animateTo(result.latitude, result.longitude, 14.0)
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

                // Quick Jump Landmark Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(REGIONAL_LANDMARKS) { lm ->
                        FilterChip(
                            selected = placeName == lm.name,
                            onClick = {
                                pinLatLng = LatLngGeo(lm.latitude, lm.longitude)
                                placeName = lm.name
                                resolvedAddress = String.format(Locale.US, "%s · %.4f, %.4f", lm.address, lm.latitude, lm.longitude)
                                mapController.animateTo(lm.latitude, lm.longitude, 13.5)
                            },
                            label = { Text(lm.name, fontSize = 11.sp) }
                        )
                    }
                }

                val pickerPins = remember(pinLatLng, placeName, userGpsLat, userGpsLng, isOriginMode) {
                    buildList<TomTomMapPin> {
                        if (userGpsLat != null && userGpsLng != null) {
                            add(TomTomMapPin.SimpleDotPin(userGpsLat, userGpsLng, 0xFF2563EB.toInt()))
                        }
                        if (isOriginMode) {
                            add(
                                TomTomMapPin.WaypointBadgePin(
                                    latitude = pinLatLng.lat,
                                    longitude = pinLatLng.lng,
                                    label = "Start: ${placeName.ifBlank { "Keberangkatan" }.take(14)}",
                                    colorHex = 0xFFEA580C.toInt()
                                )
                            )
                        } else {
                            add(
                                TomTomMapPin.DestinationPin(
                                    latitude = pinLatLng.lat,
                                    longitude = pinLatLng.lng,
                                    title = placeName.ifBlank { "Titik Tujuan Konvoi" },
                                    subtitle = String.format(Locale.US, "%.4f, %.4f", pinLatLng.lat, pinLatLng.lng)
                                )
                            )
                        }
                    }
                }

                var pendingLookupKey by remember { mutableStateOf<Pair<Double, Double>?>(null) }
                LaunchedEffect(pendingLookupKey) {
                    val coords = pendingLookupKey ?: return@LaunchedEffect
                    val res = TomTomApiService.reverseGeocode(coords.first, coords.second)
                    placeName = res.name
                    resolvedAddress = res.address
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE9EFF5))
                ) {
                    TomTomConvoyMap(
                        modifier = Modifier.fillMaxSize(),
                        controller = mapController,
                        initialLat = initialLat,
                        initialLng = initialLng,
                        initialZoom = 12.5,
                        mapStyle = mapStyle,
                        pins = pickerPins,
                        onMapTap = { lat, lng ->
                            pinLatLng = LatLngGeo(lat, lng)
                            val closest = REGIONAL_LANDMARKS.minByOrNull { lm ->
                                GeoUtils.haversineMeters(
                                    LatLngGeo(lm.latitude, lm.longitude),
                                    LatLngGeo(lat, lng)
                                )
                            }
                            val closestDist = closest?.let {
                                GeoUtils.haversineMeters(LatLngGeo(it.latitude, it.longitude), LatLngGeo(lat, lng))
                            } ?: Double.MAX_VALUE
                            if (closest != null && closestDist < 2500.0) {
                                placeName = closest.name
                                resolvedAddress = String.format(Locale.US, "%s · %.4f, %.4f", closest.address, lat, lng)
                            } else {
                                placeName = String.format(Locale.US, "Titik Tujuan (%.4f, %.4f)", lat, lng)
                                resolvedAddress = String.format(Locale.US, "Mencari nama jalan... · %.4f, %.4f", lat, lng)
                                pendingLookupKey = lat to lng
                            }
                        }
                    )

                    // Zoom & GPS Controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { mapController.zoomIn() },
                            containerColor = MaterialTheme.colorScheme.surface,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Perbesar Peta")
                        }
                        SmallFloatingActionButton(
                            onClick = { mapController.zoomOut() },
                            containerColor = MaterialTheme.colorScheme.surface,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Perkecil Peta")
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                if (userGpsLat != null && userGpsLng != null) {
                                    mapController.animateTo(userGpsLat, userGpsLng, 14.0)
                                } else {
                                    mapController.animateTo(pinLatLng.lat, pinLatLng.lng, 14.0)
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(Icons.Rounded.MyLocation, contentDescription = "Fokus Lokasi")
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xCC0F172A),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Pin: %.4f, %.4f · Ketuk peta",
                                pinLatLng.lat,
                                pinLatLng.lng
                            ),
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Bottom Confirmation Sheet
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = placeName,
                            onValueChange = { placeName = it },
                            label = { Text(if (isOriginMode) "Nama Titik Keberangkatan (Start)" else "Nama Lokasi Tujuan") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = resolvedAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Batal")
                            }
                            Button(
                                onClick = {
                                    onConfirm(
                                        pinLatLng.lat,
                                        pinLatLng.lng,
                                        placeName.trim().ifBlank { if (isOriginMode) "Titik Start TomTom" else "Titik Tujuan TomTom" },
                                        resolvedAddress
                                    )
                                },
                                modifier = Modifier.weight(1.6f).height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOriginMode) Color(0xFFEA580C) else MaterialTheme.colorScheme.secondary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gunakan Lokasi Ini", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

