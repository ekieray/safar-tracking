package com.konvoiku.touring.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.konvoiku.touring.data.ManasikGuideData
import com.konvoiku.touring.data.PrayerCategory
import com.konvoiku.touring.data.PrayerLibraryData
import com.konvoiku.touring.data.PrayerLibraryItem
import com.konvoiku.touring.model.GpsTrackingStatus
import com.konvoiku.touring.model.ManasikGuideType
import com.konvoiku.touring.model.ManasikProgressState
import com.konvoiku.touring.model.ManasikStepCategory
import com.konvoiku.touring.model.ManasikStepItem
import com.konvoiku.touring.model.RoomActivityItem
import com.konvoiku.touring.model.RoomActivityMemberProgress
import com.konvoiku.touring.model.TourRoom
import com.konvoiku.touring.ui.components.TouringDashboardCanvasBackdrop
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManasikGuideScreen(
    initialTab: String = "manasik",
    activeRoom: TourRoom?,
    isRoomCreator: Boolean,
    gpsStatus: GpsTrackingStatus,
    userGpsLat: Double?,
    userGpsLng: Double?,
    manasikProgress: ManasikProgressState,
    roomActivities: List<RoomActivityItem>,
    roomActivityProgress: Map<String, RoomActivityMemberProgress>,
    currentUserUid: String,
    onRequestGpsPermission: () -> Unit,
    onToggleManasikStep: (ManasikGuideType, String) -> Unit,
    onCheckInManasikStep: (ManasikGuideType, ManasikStepItem, Double, Double) -> Boolean,
    onResetManasikGuide: (ManasikGuideType) -> Unit,
    onAddRoomActivity: (String, String, String) -> Unit,
    onRemoveRoomActivity: (String) -> Unit,
    onMoveRoomActivity: (String, Int) -> Unit,
    onApplyStarterActivities: () -> Unit,
    onToggleRoomActivity: (String) -> Unit,
    onOpenAssistant: () -> Unit = {},
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val hasActiveRoom = activeRoom != null
    var selectedMainTab by remember(initialTab, hasActiveRoom) {
        mutableStateOf(
            when {
                initialTab == "activities" && hasActiveRoom -> "activities"
                initialTab == "doa" -> "doa"
                else -> "manasik"
            }
        )
    }
    var selectedGuide by remember { mutableStateOf(ManasikGuideType.UMRAH) }
    var manasikFilter by remember { mutableStateOf("all") } // "all" | "pending" | "done"
    var expandedStepId by remember { mutableStateOf<String?>(null) }
    var simLocationId by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    // Doa Kemenag/MUI state
    var selectedPrayerCategory by remember { mutableStateOf<PrayerCategory?>(null) }
    var prayerSearchQuery by remember { mutableStateOf("") }

    // Group activity state
    var activityFilter by remember { mutableStateOf("all") } // "all" | "pending" | "done"
    var showAddActivityForm by remember { mutableStateOf(false) }
    var newActTitle by remember { mutableStateOf("") }
    var newActTime by remember { mutableStateOf("") }
    var newActLocation by remember { mutableStateOf("") }
    var expandedActivityId by remember { mutableStateOf<String?>(null) }

    val simLoc = ManasikGuideData.simulationLocations.find { it.id == simLocationId }
    val effectiveLat = simLoc?.latitude ?: userGpsLat
    val effectiveLng = simLoc?.longitude ?: userGpsLng

    val currentGuideProgress = if (selectedGuide == ManasikGuideType.UMRAH) {
        manasikProgress.umrah
    } else {
        manasikProgress.haji
    }
    val allGuideSteps = ManasikGuideData.stepsForGuide(selectedGuide)
    val completedSet = currentGuideProgress.completedStepIds.toSet()
    val filteredSteps = remember(allGuideSteps, completedSet, manasikFilter) {
        when (manasikFilter) {
            "pending" -> allGuideSteps.filterNot { it.id in completedSet }
            "done" -> allGuideSteps.filter { it.id in completedSet }
            else -> allGuideSteps
        }
    }

    val filteredPrayers = remember(selectedPrayerCategory, prayerSearchQuery) {
        val q = prayerSearchQuery.trim().lowercase()
        PrayerLibraryData.prayers.filter { item ->
            val catOk = selectedPrayerCategory == null || item.category == selectedPrayerCategory
            if (!catOk) return@filter false
            if (q.isEmpty()) return@filter true
            item.title.lowercase().contains(q) ||
                item.occasionHint.lowercase().contains(q) ||
                item.transliteration.lowercase().contains(q) ||
                item.translation.lowercase().contains(q) ||
                item.verificationBadge.lowercase().contains(q)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                selectedMainTab == "activities" && hasActiveRoom -> "Checklist Kegiatan Umroh"
                                selectedMainTab == "doa" -> "Kumpulan Doa Travel & Harian"
                                else -> "Panduan Manasik"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                selectedMainTab == "activities" && hasActiveRoom -> "${activeRoom?.roomId ?: ""} · ${activeRoom?.roomTitle ?: "Rombongan"}"
                                selectedMainTab == "doa" -> "Standar Tuntunan Manasik Kemenag RI & MUI"
                                else -> "Tuntunan Umroh & Haji + Check-in GPS"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                    Surface(
                        onClick = onOpenAssistant,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Tanya Mutawwif AI",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Tanya AI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TouringDashboardCanvasBackdrop()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val tabCount = if (hasActiveRoom) 3 else 2
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = selectedMainTab == "manasik",
                            onClick = { selectedMainTab = "manasik" },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = tabCount),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = if (hasActiveRoom) "Manasik" else "Panduan Manasik",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        SegmentedButton(
                            selected = selectedMainTab == "doa",
                            onClick = { selectedMainTab = "doa" },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = tabCount),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = if (hasActiveRoom) "Doa Kemenag" else "Doa Kemenag / MUI",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (hasActiveRoom) {
                            SegmentedButton(
                                selected = selectedMainTab == "activities",
                                onClick = { selectedMainTab = "activities" },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = tabCount),
                                icon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Checklist,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            ) {
                                Text(
                                    text = "Kegiatan (${roomActivities.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (selectedMainTab == "doa") {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Terverifikasi Buku Tuntunan Manasik Kemenag RI & MUI",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = "${PrayerLibraryData.prayers.size} doa lengkap (Arab, Latin, arti & adab) meliputi doa safar travel umroh, ihram/tawaf/sa'i, ziarah Madinah, serta doa harian standar.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = prayerSearchQuery,
                            onValueChange = { prayerSearchQuery = it },
                            placeholder = { Text("Cari doa (contoh: kendaraan, tawaf, makan, rawdah)", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (prayerSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { prayerSearchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Bersihkan pencarian", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedPrayerCategory == null,
                                    onClick = { selectedPrayerCategory = null },
                                    label = {
                                        Text(
                                            text = "Semua (${PrayerLibraryData.prayers.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                            items(PrayerCategory.entries.toList(), key = { it.id }) { cat ->
                                val count = PrayerLibraryData.prayers.count { it.category == cat }
                                FilterChip(
                                    selected = selectedPrayerCategory == cat,
                                    onClick = { selectedPrayerCategory = cat },
                                    label = {
                                        Text(
                                            text = "${cat.shortLabel} ($count)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (filteredPrayers.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Doa tidak ditemukan untuk pencarian tersebut.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    TextButton(onClick = {
                                        prayerSearchQuery = ""
                                        selectedPrayerCategory = null
                                    }) {
                                        Text("Reset Filter Doa")
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredPrayers, key = { it.id }) { prayer ->
                            PrayerLibraryCard(
                                prayer = prayer,
                                onCopy = {
                                    val copyText = buildString {
                                        appendLine(prayer.title)
                                        appendLine("(${prayer.verificationBadge})")
                                        appendLine()
                                        appendLine(prayer.arabicText)
                                        appendLine(prayer.transliteration)
                                        append("Artinya: \"${prayer.translation}\"")
                                    }
                                    clipboardManager.setText(AnnotatedString(copyText))
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Doa disalin: ${prayer.title}")
                                    }
                                },
                                onAskAi = onOpenAssistant
                            )
                        }
                    }
                } else if (selectedMainTab == "manasik" || !hasActiveRoom) {
                    // Umroh / Haji Switcher
                    item {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = selectedGuide == ManasikGuideType.UMRAH,
                                onClick = {
                                    selectedGuide = ManasikGuideType.UMRAH
                                    expandedStepId = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text(
                                    text = "Umroh (${manasikProgress.umrah.completedStepIds.size}/${ManasikGuideData.umrahSteps.size})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                            SegmentedButton(
                                selected = selectedGuide == ManasikGuideType.HAJI,
                                onClick = {
                                    selectedGuide = ManasikGuideType.HAJI
                                    expandedStepId = null
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text(
                                    text = "Haji (${manasikProgress.haji.completedStepIds.size}/${ManasikGuideData.hajjSteps.size})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Progress Summary Card
                    item {
                        val totalCount = allGuideSteps.size.coerceAtLeast(1)
                        val doneCount = allGuideSteps.count { it.id in completedSet }
                        val checkInCount = allGuideSteps.count { it.id in currentGuideProgress.checkInRecords }
                        val progressFraction = doneCount.toFloat() / totalCount.toFloat()

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedGuide.label,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "$doneCount dari $totalCount tahap selesai · $checkInCount check-in GPS",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = { showResetConfirm = true },
                                        enabled = doneCount > 0 || checkInCount > 0
                                    ) {
                                        Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Reset", fontSize = 12.sp)
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "all" to "Semua ($totalCount)",
                                        "pending" to "Belum (${totalCount - doneCount})",
                                        "done" to "Selesai ($doneCount)"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = manasikFilter == key,
                                            onClick = { manasikFilter = key },
                                            label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // GPS & Landmark Check-in Banner + Simulation Location Switcher
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Rounded.GpsFixed,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = if (simLoc != null) {
                                                    "Simulasi GPS: ${simLoc.label}"
                                                } else if (effectiveLat != null && effectiveLng != null) {
                                                    "GPS Real Aktif"
                                                } else {
                                                    "Check-in Lokasi Manasik"
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = if (effectiveLat != null && effectiveLng != null) {
                                                    String.format(Locale.US, "%.4f, %.4f", effectiveLat, effectiveLng)
                                                } else {
                                                    "Aktifkan GPS HP atau pilih lokasi uji manasik"
                                                },
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (simLoc != null) {
                                        TextButton(onClick = { simLocationId = null }) {
                                            Text("Pakai GPS", fontSize = 11.sp)
                                        }
                                    } else if (gpsStatus != GpsTrackingStatus.ACTIVE) {
                                        OutlinedButton(
                                            onClick = onRequestGpsPermission,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Rounded.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Aktifkan GPS", fontSize = 11.sp)
                                        }
                                    }
                                }

                                Text(
                                    text = "Simulasi titik lokasi Haramain (untuk latihan manasik):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(ManasikGuideData.simulationLocations, key = { it.id }) { loc ->
                                        FilterChip(
                                            selected = simLocationId == loc.id,
                                            onClick = {
                                                simLocationId = if (simLocationId == loc.id) null else loc.id
                                            },
                                            label = { Text(loc.label, fontSize = 11.sp, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Manasik Steps List
                    items(filteredSteps, key = { it.id }) { step ->
                        val isDone = step.id in completedSet
                        val checkInRecord = currentGuideProgress.checkInRecords[step.id]
                        val isExpanded = expandedStepId == step.id
                        val proximity = if (effectiveLat != null && effectiveLng != null) {
                            ManasikGuideData.evaluateLandmarkProximity(step, effectiveLat, effectiveLng)
                        } else null

                        ManasikStepCard(
                            step = step,
                            isDone = isDone,
                            isExpanded = isExpanded,
                            checkInRecord = checkInRecord,
                            proximityDistanceMeters = proximity?.distanceMeters,
                            proximityLandmarkName = proximity?.landmark?.name,
                            isWithinRadius = proximity?.isWithinRadius == true,
                            onToggleExpand = {
                                expandedStepId = if (isExpanded) null else step.id
                            },
                            onToggleDone = {
                                onToggleManasikStep(selectedGuide, step.id)
                            },
                            onCheckIn = {
                                val lat = effectiveLat
                                val lng = effectiveLng
                                if (lat == null || lng == null) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Aktifkan GPS atau pilih lokasi simulasi terlebih dahulu.")
                                    }
                                } else {
                                    val ok = onCheckInManasikStep(selectedGuide, step, lat, lng)
                                    coroutineScope.launch {
                                        if (ok) {
                                            snackbarHostState.showSnackbar("Check-in berhasil: ${step.title}")
                                        } else {
                                            snackbarHostState.showSnackbar("Posisi masih di luar radius ${step.landmark.name}.")
                                        }
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // Tab 2: Checklist Kegiatan Umroh Rombongan
                    val myProgress = roomActivityProgress[currentUserUid]
                    val myCompletedIds = myProgress?.completedActivityIds.orEmpty().toSet()
                    val membersCount = activeRoom?.members?.size?.coerceAtLeast(1) ?: 1

                    item {
                        val doneByMe = roomActivities.count { it.id in myCompletedIds }
                        val totalAct = roomActivities.size
                        val frac = if (totalAct > 0) doneByMe.toFloat() / totalAct.toFloat() else 0f

                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Checklist Kegiatan Rombongan",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "$doneByMe dari $totalAct kegiatan telah Anda centang · $membersCount jamaah di room",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isRoomCreator) {
                                        Button(
                                            onClick = { showAddActivityForm = !showAddActivityForm },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(
                                                imageVector = if (showAddActivityForm) Icons.Rounded.Close else Icons.Rounded.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (showAddActivityForm) "Tutup" else "Tambah", fontSize = 12.sp)
                                        }
                                    }
                                }

                                LinearProgressIndicator(
                                    progress = { frac },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(99.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "all" to "Semua ($totalAct)",
                                        "pending" to "Belum (${totalAct - doneByMe})",
                                        "done" to "Selesai ($doneByMe)"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = activityFilter == key,
                                            onClick = { activityFilter = key },
                                            label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Creator Add Form & Quick Templates
                    if (isRoomCreator) {
                        item {
                            AnimatedVisibility(visible = showAddActivityForm || roomActivities.isEmpty()) {
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Kelola Agenda Rombongan (Mutawwif)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(
                                                onClick = {
                                                    onApplyStarterActivities()
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("4 kegiatan starter Umroh ditambahkan.")
                                                    }
                                                }
                                            ) {
                                                Text("4 Starter Umroh", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        OutlinedTextField(
                                            value = newActTitle,
                                            onValueChange = { newActTitle = it },
                                            label = { Text("Nama Kegiatan") },
                                            placeholder = { Text("Mis. Tawaf Qudum Bersama") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = newActTime,
                                                onValueChange = { newActTime = it },
                                                label = { Text("Waktu / Sesi") },
                                                placeholder = { Text("08:00 / Ba'da Subuh") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = newActLocation,
                                                onValueChange = { newActLocation = it },
                                                label = { Text("Lokasi") },
                                                placeholder = { Text("Lobi / Pintu King Fahd") },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1.2f)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (newActTitle.isNotBlank()) {
                                                    onAddRoomActivity(newActTitle, newActTime, newActLocation)
                                                    newActTitle = ""
                                                    newActTime = ""
                                                    newActLocation = ""
                                                }
                                            },
                                            enabled = newActTitle.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Tambah ke Checklist Room", fontWeight = FontWeight.SemiBold)
                                        }

                                        Text(
                                            text = "Tambah cepat dari template Umroh:",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(ManasikGuideData.umrahActivityQuickTemplates, key = { it.key }) { tpl ->
                                                val alreadyAdded = roomActivities.any {
                                                    it.title.equals(tpl.title, ignoreCase = true)
                                                }
                                                FilterChip(
                                                    selected = alreadyAdded,
                                                    onClick = {
                                                        if (!alreadyAdded) {
                                                            onAddRoomActivity(tpl.title, tpl.timeLabel, tpl.locationNote)
                                                        }
                                                    },
                                                    label = { Text(tpl.title, fontSize = 11.sp, maxLines = 1) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val displayedActivities = when (activityFilter) {
                        "pending" -> roomActivities.filterNot { it.id in myCompletedIds }
                        "done" -> roomActivities.filter { it.id in myCompletedIds }
                        else -> roomActivities
                    }

                    if (displayedActivities.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Checklist,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = if (roomActivities.isEmpty()) {
                                            "Belum ada checklist kegiatan di room ini"
                                        } else {
                                            "Tidak ada kegiatan pada filter ini"
                                        },
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (isRoomCreator) {
                                            "Tekan '4 Starter Umroh' atau tambahkan agenda rombongan di atas."
                                        } else {
                                            "Menunggu Mutawwif / pembuat room menambahkan daftar kegiatan."
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedActivities, key = { it.id }) { act ->
                            val isMineDone = act.id in myCompletedIds
                            val myDoneClock = myProgress?.completedAtByActivity?.get(act.id)
                            val completedMembers = roomActivityProgress.values.filter {
                                act.id in it.completedActivityIds
                            }
                            val isExpanded = expandedActivityId == act.id
                            val fullIndex = roomActivities.indexOfFirst { it.id == act.id }

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMineDone) {
                                        Color(0xFFF0FDF4)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isMineDone) Color(0xFF86EFAC) else MaterialTheme.colorScheme.outline
                                ),
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
                                        Checkbox(
                                            checked = isMineDone,
                                            onCheckedChange = { onToggleRoomActivity(act.id) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onToggleRoomActivity(act.id) }
                                        ) {
                                            Text(
                                                text = act.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                textDecoration = if (isMineDone) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (isMineDone) Color(0xFF166534) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (act.timeLabel.isNotBlank() || act.locationNote.isNotBlank()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (act.timeLabel.isNotBlank()) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                                            Text(act.timeLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                                                        }
                                                    }
                                                    if (act.locationNote.isNotBlank()) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            Text(act.locationNote, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                    }
                                                }
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(6.dp),
                                                    modifier = Modifier.clickable {
                                                        expandedActivityId = if (isExpanded) null else act.id
                                                    }
                                                ) {
                                                    Text(
                                                        text = "${completedMembers.size}/$membersCount jamaah selesai",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (!myDoneClock.isNullOrBlank()) {
                                                    Text(
                                                        text = "Dicentang $myDoneClock",
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF15803D)
                                                    )
                                                }
                                            }
                                        }
                                        if (isRoomCreator) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Row {
                                                    IconButton(
                                                        onClick = { onMoveRoomActivity(act.id, -1) },
                                                        enabled = fullIndex > 0,
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Naikkan", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { onMoveRoomActivity(act.id, 1) },
                                                        enabled = fullIndex in 0 until (roomActivities.size - 1),
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Turunkan", modifier = Modifier.size(16.dp))
                                                    }
                                                    IconButton(
                                                        onClick = { onRemoveRoomActivity(act.id) },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    AnimatedVisibility(visible = isExpanded && completedMembers.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.background,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "Jamaah yang sudah menyelesaikan:",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                completedMembers.forEach { prog ->
                                                    val doneAt = prog.completedAtByActivity[act.id]
                                                    Text(
                                                        text = "• ${prog.memberName}${if (!doneAt.isNullOrBlank()) " ($doneAt)" else ""}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
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

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Progres ${selectedGuide.label}?") },
            text = { Text("Seluruh tanda selesai dan riwayat check-in GPS untuk panduan ${selectedGuide.label} akan dihapus.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetManasikGuide(selectedGuide)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ManasikStepCard(
    step: ManasikStepItem,
    isDone: Boolean,
    isExpanded: Boolean,
    checkInRecord: com.konvoiku.touring.model.ManasikCheckInRecord?,
    proximityDistanceMeters: Int?,
    proximityLandmarkName: String?,
    isWithinRadius: Boolean,
    onToggleExpand: () -> Unit,
    onToggleDone: () -> Unit,
    onCheckIn: () -> Unit
) {
    val categoryColor = when (step.category) {
        ManasikStepCategory.RUKUN -> Color(0xFF047857)
        ManasikStepCategory.WAJIB -> Color(0xFFB45309)
        ManasikStepCategory.SUNNAH -> Color(0xFF0F766E)
    }
    val categoryBg = when (step.category) {
        ManasikStepCategory.RUKUN -> Color(0xFFECFDF5)
        ManasikStepCategory.WAJIB -> Color(0xFFFFFBEB)
        ManasikStepCategory.SUNNAH -> Color(0xFFF0FDFA)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isDone) Color(0xFF6EE7B7) else MaterialTheme.colorScheme.outline
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    onClick = onToggleDone,
                    shape = CircleShape,
                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selesai",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "${step.order}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onToggleExpand),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = categoryBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = step.category.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (checkInRecord != null) {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(Icons.Rounded.Verified, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "Check-in ${checkInRecord.checkedInAtLabel}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = step.locationHint,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = step.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isExpanded) "Tutup Detail" else "Lihat Detail"
                    )
                }
            }

            // Proximity & Check-in Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (proximityDistanceMeters != null) {
                    val distLabel = if (proximityDistanceMeters >= 1000) {
                        String.format(Locale.US, "%.1f km", proximityDistanceMeters / 1000.0)
                    } else {
                        "${proximityDistanceMeters} m"
                    }
                    Text(
                        text = if (isWithinRadius) {
                            "Di radius ${proximityLandmarkName ?: step.landmark.name} ($distLabel)"
                        } else {
                            "Jarak ke ${proximityLandmarkName ?: step.landmark.name}: $distLabel"
                        },
                        fontSize = 11.sp,
                        fontWeight = if (isWithinRadius) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isWithinRadius) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "Radius check-in: ${step.landmark.radiusMeters} m",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onCheckIn,
                    enabled = isWithinRadius,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isWithinRadius) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Icon(
                        imageVector = if (checkInRecord != null) Icons.Rounded.CheckCircle else Icons.Rounded.GpsFixed,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (checkInRecord != null) "Update Check-in" else "Check-in Lokasi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    if (!step.arabicText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                    Text(
                                        text = step.arabicText,
                                        fontSize = 20.sp,
                                        lineHeight = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF064E3B),
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (!step.transliteration.isNullOrBlank()) {
                                    Text(
                                        text = step.transliteration,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF047857)
                                    )
                                }
                                if (!step.translation.isNullOrBlank()) {
                                    Text(
                                        text = "\"${step.translation}\"",
                                        fontSize = 11.sp,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        step.details.forEachIndexed { idx, detail ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${idx + 1}.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = detail,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onToggleDone,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDone) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                contentColor = if (isDone) MaterialTheme.colorScheme.onSurface else Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (isDone) Icons.Rounded.RadioButtonUnchecked else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDone) "Batalkan Tanda Selesai" else "Tandai Sudah Selesai",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerLibraryCard(
    prayer: PrayerLibraryItem,
    onCopy: () -> Unit,
    onAskAi: () -> Unit
) {
    val categoryColor = when (prayer.category) {
        PrayerCategory.SAFAR -> Color(0xFF047857)
        PrayerCategory.UMRAH -> Color(0xFF0F766E)
        PrayerCategory.MADINAH -> Color(0xFFB45309)
        PrayerCategory.HARIAN -> Color(0xFF1D4ED8)
    }
    val categoryBg = when (prayer.category) {
        PrayerCategory.SAFAR -> Color(0xFFECFDF5)
        PrayerCategory.UMRAH -> Color(0xFFF0FDFA)
        PrayerCategory.MADINAH -> Color(0xFFFFFBEB)
        PrayerCategory.HARIAN -> Color(0xFFEFF6FF)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = categoryBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = prayer.category.shortLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = prayer.verificationBadge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text(
                        text = prayer.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = prayer.occasionHint,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Salin Doa",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = prayer.arabicText,
                            fontSize = 20.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF064E3B),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = prayer.transliteration,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF047857)
                    )
                }
            }

            Text(
                text = "Artinya: \"${prayer.translation}\"",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!prayer.adabNote.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Catatan Adab: ${prayer.adabNote}",
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onAskAi,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tanya Mutawwif AI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

