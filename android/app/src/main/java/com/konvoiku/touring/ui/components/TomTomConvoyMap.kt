package com.konvoiku.touring.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.konvoiku.touring.BuildConfig
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.RestCheckpoint
import com.konvoiku.touring.model.RoutePoint
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.model.VehicleType
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay

enum class TomTomMapStyle {
    ROAD,
    SATELLITE
}

@Stable
class TomTomMapController {
    internal var mapView: MapView? by mutableStateOf(null)

    fun zoomIn() {
        mapView?.controller?.zoomIn()
    }

    fun zoomOut() {
        mapView?.controller?.zoomOut()
    }

    fun animateTo(latitude: Double, longitude: Double, zoomLevel: Double? = null) {
        val mv = mapView ?: return
        val targetZoom = zoomLevel ?: mv.zoomLevelDouble
        mv.controller.animateTo(GeoPoint(latitude, longitude), targetZoom, 500L)
    }

    fun setCenter(latitude: Double, longitude: Double, zoomLevel: Double = 13.0) {
        val mv = mapView ?: return
        mv.controller.setZoom(zoomLevel)
        mv.controller.setCenter(GeoPoint(latitude, longitude))
    }

    fun fitBounds(points: List<GeoPoint>) {
        val mv = mapView ?: return
        if (points.size < 2) {
            points.firstOrNull()?.let { animateTo(it.latitude, it.longitude, 14.0) }
            return
        }
        runCatching {
            val box = BoundingBox.fromGeoPoints(points)
            mv.zoomToBoundingBox(box, true, 96)
        }
    }

    val currentZoom: Double
        get() = mapView?.zoomLevelDouble ?: 13.5
}

@Composable
fun rememberTomTomMapController(): TomTomMapController {
    return remember { TomTomMapController() }
}

private fun createTomTomTileSource(
    name: String,
    urlTemplate: (z: Int, x: Int, y: Int, key: String) -> String,
    isPng: Boolean = true
): OnlineTileSourceBase {
    val ext = if (isPng) ".png" else ".jpg"
    return object : OnlineTileSourceBase(name, 1, 19, 256, ext, arrayOf("https://api.tomtom.com/")) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            val z = MapTileIndex.getZoom(pMapTileIndex)
            val x = MapTileIndex.getX(pMapTileIndex)
            val y = MapTileIndex.getY(pMapTileIndex)
            return urlTemplate(z, x, y, BuildConfig.TOMTOM_API_KEY)
        }
    }
}

sealed interface TomTomMapPin {
    val latitude: Double
    val longitude: Double

    data class MemberPin(
        val member: ConvoyMember,
        val isSelected: Boolean,
        val isOffRoute: Boolean,
        val isStraggler: Boolean,
        val isSosBeacon: Boolean
    ) : TomTomMapPin {
        override val latitude: Double get() = member.mapPosition.latitude
        override val longitude: Double get() = member.mapPosition.longitude
    }

    data class DestinationPin(
        override val latitude: Double,
        override val longitude: Double,
        val title: String,
        val subtitle: String
    ) : TomTomMapPin

    data class CheckpointPin(
        val checkpoint: RestCheckpoint
    ) : TomTomMapPin {
        override val latitude: Double get() = checkpoint.latitude
        override val longitude: Double get() = checkpoint.longitude
    }

    data class WaypointBadgePin(
        override val latitude: Double,
        override val longitude: Double,
        val label: String,
        val colorHex: Int
    ) : TomTomMapPin

    data class SimpleDotPin(
        override val latitude: Double,
        override val longitude: Double,
        val colorHex: Int
    ) : TomTomMapPin
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun TomTomConvoyMap(
    modifier: Modifier = Modifier,
    controller: TomTomMapController = rememberTomTomMapController(),
    initialLat: Double = -6.674,
    initialLng: Double = 106.925,
    initialZoom: Double = 13.0,
    mapStyle: TomTomMapStyle = TomTomMapStyle.ROAD,
    isDarkMode: Boolean = false,
    showTraffic: Boolean = false,
    isNavigationMode: Boolean = false,
    navigationHeadingDeg: Float = 0f,
    navigatedMemberId: String? = null,
    polylinePoints: List<RoutePoint> = emptyList(),
    pins: List<TomTomMapPin> = emptyList(),
    onMapTap: (Double, Double) -> Unit = { _, _ -> },
    onUserDrag: () -> Unit = {},
    onMemberPinClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentOnUserDrag by rememberUpdatedState(onUserDrag)
    val currentOnMemberClick by rememberUpdatedState(onMemberPinClick)

    val roadDaySource = remember {
        createTomTomTileSource(
            name = "TomTomRoadDay",
            urlTemplate = { z, x, y, key ->
                "https://api.tomtom.com/map/1/tile/basic/main/$z/$x/$y.png?key=$key&tileSize=256"
            }
        )
    }
    val roadNightSource = remember {
        createTomTomTileSource(
            name = "TomTomRoadNight",
            urlTemplate = { z, x, y, key ->
                "https://api.tomtom.com/map/1/tile/basic/night/$z/$x/$y.png?key=$key&tileSize=256"
            }
        )
    }
    val satSource = remember {
        createTomTomTileSource(
            name = "TomTomSat",
            urlTemplate = { z, x, y, key ->
                "https://api.tomtom.com/map/1/tile/sat/main/$z/$x/$y.jpg?key=$key&tileSize=256"
            },
            isPng = false
        )
    }
    val labelsSource = remember {
        createTomTomTileSource(
            name = "TomTomLabels",
            urlTemplate = { z, x, y, key ->
                "https://api.tomtom.com/map/1/tile/labels/main/$z/$x/$y.png?key=$key&tileSize=256"
            }
        )
    }
    val trafficSource = remember {
        createTomTomTileSource(
            name = "TomTomTraffic",
            urlTemplate = { z, x, y, key ->
                "https://api.tomtom.com/traffic/map/4/tile/flow/relative0/$z/$x/$y.png?key=$key&tileSize=256"
            }
        )
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(if (isDarkMode) roadNightSource else roadDaySource)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled = false
            minZoomLevel = 4.0
            maxZoomLevel = 19.0
            this.controller.setZoom(initialZoom)
            this.controller.setCenter(GeoPoint(initialLat, initialLng))

            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                    currentOnUserDrag()
                }
                false
            }
        }
    }

    DisposableEffect(mapView) {
        controller.mapView = mapView
        onDispose {
            if (controller.mapView == mapView) {
                controller.mapView = null
            }
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val labelsOverlay = remember {
        TilesOverlay(MapTileProviderBasic(context, labelsSource), context).apply {
            loadingBackgroundColor = Color.TRANSPARENT
        }
    }
    val trafficOverlay = remember {
        TilesOverlay(MapTileProviderBasic(context, trafficSource), context).apply {
            loadingBackgroundColor = Color.TRANSPARENT
        }
    }
    val eventsOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    currentOnMapTap(p.latitude, p.longitude)
                }
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { mv ->
            val desiredSource = when (mapStyle) {
                TomTomMapStyle.SATELLITE -> satSource
                TomTomMapStyle.ROAD -> if (isDarkMode) roadNightSource else roadDaySource
            }
            if (mv.tileProvider.tileSource.name() != desiredSource.name()) {
                mv.setTileSource(desiredSource)
            }

            // Ensure persistent tap & base tile overlays exist without recreating tile providers every 2s
            if (!mv.overlays.contains(eventsOverlay)) {
                mv.overlays.add(0, eventsOverlay)
            }
            if (mapStyle == TomTomMapStyle.SATELLITE) {
                if (!mv.overlays.contains(labelsOverlay)) {
                    mv.overlays.add(labelsOverlay)
                }
            } else {
                mv.overlays.remove(labelsOverlay)
            }
            if (showTraffic) {
                if (!mv.overlays.contains(trafficOverlay)) {
                    mv.overlays.add(trafficOverlay)
                }
            } else {
                mv.overlays.remove(trafficOverlay)
            }

            // Remove only dynamic vector overlays (Polyline & Marker) before redrawing
            mv.overlays.removeAll { it is Polyline || it is Marker }

            // Route polyline (white casing + blue primary road line)
            if (polylinePoints.size >= 2) {
                val geoPts = polylinePoints.map { GeoPoint(it.latitude, it.longitude) }
                val casing = Polyline(mv).apply {
                    setPoints(geoPts)
                    outlinePaint.color = Color.WHITE
                    outlinePaint.strokeWidth = 15f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                val roadLine = Polyline(mv).apply {
                    setPoints(geoPts)
                    outlinePaint.color = 0xFF1D4ED8.toInt()
                    outlinePaint.strokeWidth = 9f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.strokeJoin = Paint.Join.ROUND
                }
                mv.overlays.add(casing)
                mv.overlays.add(roadLine)
            }

            // Markers
            pins.forEach { pin ->
                val marker = Marker(mv)
                marker.position = GeoPoint(pin.latitude, pin.longitude)
                marker.infoWindow = null
                when (pin) {
                    is TomTomMapPin.SimpleDotPin -> {
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        marker.icon = createDotDrawable(context, pin.colorHex)
                    }
                    is TomTomMapPin.WaypointBadgePin -> {
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.icon = createPillBadgeDrawable(context, pin.label, pin.colorHex)
                    }
                    is TomTomMapPin.CheckpointPin -> {
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.icon = createCheckpointDrawable(context, pin.checkpoint.name)
                    }
                    is TomTomMapPin.DestinationPin -> {
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.icon = createDestinationDrawable(context, pin.title, pin.subtitle)
                    }
                    is TomTomMapPin.MemberPin -> {
                        val isNavPuck = isNavigationMode && pin.member.id == navigatedMemberId
                        if (isNavPuck) {
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            marker.icon = createNavigationPuckDrawable(context, pin.isOffRoute || pin.isSosBeacon)
                        } else {
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.icon = createMemberDrawable(context, pin)
                        }
                        marker.setOnMarkerClickListener { _, _ ->
                            currentOnMemberClick(pin.member.id)
                            true
                        }
                    }
                }
                mv.overlays.add(marker)
            }

            val desiredOrientation = if (isNavigationMode) -navigationHeadingDeg else 0f
            if (kotlin.math.abs(mv.mapOrientation - desiredOrientation) >= 1f) {
                mv.mapOrientation = desiredOrientation
            }

            mv.invalidate()
        }
    )
}

private fun dpToPx(context: Context, dp: Float): Float =
    dp * context.resources.displayMetrics.density

private fun createDotDrawable(context: Context, colorHex: Int): BitmapDrawable {
    val size = dpToPx(context, 18f).toInt().coerceAtLeast(12)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val r = size / 2f
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorHex }
    canvas.drawCircle(r, r, r - 1f, borderPaint)
    canvas.drawCircle(r, r, r - dpToPx(context, 2.5f), fillPaint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createPillBadgeDrawable(
    context: Context,
    label: String,
    bgColor: Int
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * d
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val textWidth = textPaint.measureText(label)
    val w = (textWidth + 18f * d).toInt().coerceAtLeast((36f * d).toInt())
    val h = (26f * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val rect = RectF(1f * d, 1f * d, w - 1f * d, h - 1f * d)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    canvas.drawRoundRect(rect, 12f * d, 12f * d, bgPaint)
    canvas.drawRoundRect(rect, 12f * d, 12f * d, strokePaint)
    canvas.drawText(label, w / 2f, h / 2f + 4f * d, textPaint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createCheckpointDrawable(context: Context, name: String): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val safeName = if (name.length > 18) "${name.take(17)}…" else name
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10f * d
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val pillW = (textPaint.measureText(safeName) + 16f * d).coerceAtLeast(60f * d)
    val circleSize = 24f * d
    val w = pillW.toInt().coerceAtLeast(circleSize.toInt())
    val h = (48f * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F766E.toInt() }
    val whiteStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    val pillRect = RectF(1f * d, 1f * d, w - 1f * d, 20f * d)
    canvas.drawRoundRect(pillRect, 8f * d, 8f * d, bgPaint)
    canvas.drawText(safeName, w / 2f, 14f * d, textPaint)

    val cx = w / 2f
    val cy = h - circleSize / 2f - 1f * d
    canvas.drawCircle(cx, cy, circleSize / 2f - 1f * d, bgPaint)
    canvas.drawCircle(cx, cy, circleSize / 2f - 1f * d, whiteStroke)
    canvas.drawText("R", cx, cy + 3.5f * d, textPaint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createDestinationDrawable(
    context: Context,
    title: String,
    subtitle: String
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val safeTitle = if (title.length > 24) "${title.take(23)}…" else title
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10.5f * d
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF86EFAC.toInt()
        textSize = 9.5f * d
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.CENTER
    }
    val boxW = maxOf(titlePaint.measureText(safeTitle), subPaint.measureText(subtitle)) + 20f * d
    val w = boxW.toInt().coerceAtLeast((90f * d).toInt())
    val h = (70f * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0F172A.toInt() }
    val cardRect = RectF(1f * d, 1f * d, w - 1f * d, 36f * d)
    canvas.drawRoundRect(cardRect, 10f * d, 10f * d, cardPaint)
    canvas.drawText(safeTitle, w / 2f, 15f * d, titlePaint)
    canvas.drawText(subtitle, w / 2f, 29f * d, subPaint)

    val flagRadius = 13f * d
    val cx = w / 2f
    val cy = h - flagRadius - 2f * d
    val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF16A34A.toInt() }
    val whiteStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    canvas.drawCircle(cx, cy, flagRadius, greenPaint)
    canvas.drawCircle(cx, cy, flagRadius, whiteStroke)
    canvas.drawText("★", cx, cy + 4f * d, titlePaint)
    return BitmapDrawable(context.resources, bmp)
}

private fun createMemberDrawable(
    context: Context,
    pin: TomTomMapPin.MemberPin
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val member = pin.member
    val prefix = when {
        pin.isSosBeacon -> "SOS · "
        member.isActingLeader && member.role != UserRole.TOUR_LEADER -> "Plt.Ldr · "
        member.isCoLeader && member.isSweeper -> if (member.isAutoSweeper) "Co/Swp* · " else "Co/Swp · "
        member.isCoLeader -> "Co-Ldr · "
        member.isSweeper -> if (member.isAutoSweeper) "Swp(Auto) · " else "Swp · "
        else -> ""
    }
    val suffix = if (member.isCurrentUser) " (Anda)" else ""
    val rawLabel = "$prefix${member.fullName}$suffix"
    val label = if (rawLabel.length > 22) "${rawLabel.take(21)}…" else rawLabel

    val pillBg = when {
        pin.isSosBeacon -> 0xFF991B1B.toInt()
        pin.isOffRoute -> 0xFFDC2626.toInt()
        pin.isStraggler || member.isSweeper -> 0xFFB45309.toInt()
        member.isActingLeader && member.role != UserRole.TOUR_LEADER -> 0xFF7C3AED.toInt()
        member.isCoLeader -> 0xFF0F766E.toInt()
        pin.isSelected -> 0xFF1D4ED8.toInt()
        else -> Color.WHITE
    }
    val pillTextColor = if (
        pin.isSosBeacon ||
        pin.isOffRoute ||
        pin.isStraggler ||
        member.isSweeper ||
        pin.isSelected ||
        (member.isActingLeader && member.role != UserRole.TOUR_LEADER) ||
        member.isCoLeader
    ) {
        Color.WHITE
    } else {
        0xFF0F172A.toInt()
    }
    val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pillBg }
    val pillBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (pin.isSosBeacon || pin.isOffRoute) 0xFF7F1D1D.toInt() else 0xFFCBD5E1.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * d
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pillTextColor
        textSize = 10f * d
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    val pillW = labelPaint.measureText(label) + 16f * d
    val w = pillW.toInt().coerceAtLeast((52f * d).toInt())
    val h = (56f * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val pillRect = RectF(1f * d, 1f * d, w - 1f * d, 20f * d)
    canvas.drawRoundRect(pillRect, 8f * d, 8f * d, pillPaint)
    canvas.drawRoundRect(pillRect, 8f * d, 8f * d, pillBorder)
    canvas.drawText(label, w / 2f, 14f * d, labelPaint)

    val cx = w / 2f
    val pinR = 14f * d
    val cy = h - pinR - 3f * d

    if (pin.isSosBeacon) {
        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66DC2626 }
        canvas.drawCircle(cx, cy, pinR + 4f * d, haloPaint)
    }

    val pinColor = when {
        pin.isSosBeacon || pin.isOffRoute -> 0xFFDC2626.toInt()
        member.isActingLeader -> 0xFF7C3AED.toInt()
        member.isCoLeader -> 0xFF0D9488.toInt()
        member.isCurrentUser -> 0xFFEA580C.toInt()
        member.role == UserRole.TOUR_LEADER -> 0xFF1D4ED8.toInt()
        else -> 0xFF334155.toInt()
    }
    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pinColor }
    val whiteStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f * d
    }
    canvas.drawCircle(cx, cy, pinR, circlePaint)
    canvas.drawCircle(cx, cy, pinR, whiteStroke)

    val glyph = when {
        pin.isSosBeacon || member.conditionStatus == MemberConditionStatus.SOS -> "!"
        member.vehicleType == VehicleType.MOBIL -> "M"
        else -> "B"
    }
    val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 11f * d
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(glyph, cx, cy + 4f * d, glyphPaint)

    return BitmapDrawable(context.resources, bmp)
}

private fun createNavigationPuckDrawable(
    context: Context,
    isAlert: Boolean
): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (52f * d).toInt().coerceAtLeast(48)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val cx = size / 2f
    val cy = size / 2f

    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAlert) 0x55DC2626 else 0x552563EB
    }
    canvas.drawCircle(cx, cy, 23f * d, haloPaint)

    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }
    canvas.drawCircle(cx, cy, 17f * d, ringPaint)

    val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAlert) 0xFFDC2626.toInt() else 0xFF0F172A.toInt()
    }
    canvas.drawCircle(cx, cy, 14f * d, corePaint)

    val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAlert) Color.WHITE else 0xFF38BDF8.toInt()
        style = Paint.Style.FILL
    }
    val arrowPath = Path().apply {
        moveTo(cx, cy - 10f * d)
        lineTo(cx + 8.5f * d, cy + 7.5f * d)
        lineTo(cx, cy + 3.2f * d)
        lineTo(cx - 8.5f * d, cy + 7.5f * d)
        close()
    }
    canvas.drawPath(arrowPath, arrowPaint)

    return BitmapDrawable(context.resources, bmp)
}
