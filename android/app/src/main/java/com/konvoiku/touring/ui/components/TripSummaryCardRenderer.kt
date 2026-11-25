package com.konvoiku.touring.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.konvoiku.touring.data.TomTomApiService
import com.konvoiku.touring.model.AvatarPreset
import com.konvoiku.touring.model.TripCardTheme
import com.konvoiku.touring.model.TripHistoryRecord
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.util.GeoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

@Composable
fun StravaStyleTripShareCard(
    trip: TripHistoryRecord,
    theme: TripCardTheme,
    customPhotoUri: String?,
    currentUserName: String? = null,
    currentUserAvatarPreset: AvatarPreset? = null,
    currentUserAvatarPhotoUri: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customBitmap = produceState<ImageBitmap?>(initialValue = null, key1 = customPhotoUri) {
        value = if (!customPhotoUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(customPhotoUri))?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        } else {
            null
        }
    }.value

    val isLight = theme == TripCardTheme.MINIMAL_LIGHT
    val primaryText = if (isLight) Color(0xFF0F172A) else Color.White
    val secondaryText = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
    val cardBorder = when (theme) {
        TripCardTheme.DARK_CARBON -> Color(0xFF334155)
        TripCardTheme.SATELLITE -> Color(0xFF1E4E45)
        TripCardTheme.MINIMAL_LIGHT -> Color(0xFFCBD5E1)
        TripCardTheme.CUSTOM_PHOTO -> Color(0xFF475569)
    }

    val leader = trip.participants.firstOrNull { it.role == UserRole.TOUR_LEADER }
        ?: trip.participants.firstOrNull()
    val trimmedLoginName = currentUserName?.trim().orEmpty()
    val matchedSelf = if (trimmedLoginName.isNotEmpty()) {
        trip.participants.firstOrNull {
            it.fullName.trim().equals(trimmedLoginName, ignoreCase = true)
        }
    } else null
    val headerDisplayName = trimmedLoginName.ifBlank { leader?.fullName ?: "Rider KonvoiKu" }
    val headerAvatarPreset = currentUserAvatarPreset
        ?: matchedSelf?.avatarPreset
        ?: leader?.avatarPreset
        ?: AvatarPreset.HELMET_TOURING
    val headerAvatarPhotoUri = currentUserAvatarPhotoUri
        ?: matchedSelf?.avatarPhotoUri
        ?: if (trimmedLoginName.isNotEmpty()) null else leader?.avatarPhotoUri
    val headerRoleLabel = when {
        trip.userRole == UserRole.TOUR_LEADER -> "Tour Leader"
        matchedSelf?.isSweeper == true -> "Sweeper"
        else -> "Anggota Konvoi"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.82f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Background Layer per Theme
            when (theme) {
                TripCardTheme.DARK_CARBON -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF090E17),
                                        Color(0xFF111827),
                                        Color(0xFF172133)
                                    )
                                )
                            )
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 28.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(Color.White.copy(alpha = 0.05f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                            y += step
                        }
                        drawCircle(
                            color = Color(0xFFEA580C).copy(alpha = 0.14f),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.85f, size.height * 0.15f)
                        )
                    }
                }

                TripCardTheme.SATELLITE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0B1F1D),
                                        Color(0xFF112A2B),
                                        Color(0xFF0B1821)
                                    )
                                )
                            )
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Topographic contour rings & arterial map traces
                        for (i in 1..6) {
                            drawCircle(
                                color = Color(0xFF2DD4BF).copy(alpha = 0.08f),
                                radius = size.width * (0.2f * i),
                                center = Offset(size.width * 0.72f, size.height * 0.42f),
                                style = Stroke(width = 1.4.dp.toPx())
                            )
                        }
                        drawLine(
                            color = Color.White.copy(alpha = 0.07f),
                            start = Offset(0f, size.height * 0.25f),
                            end = Offset(size.width, size.height * 0.75f),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
                        )
                    }
                }

                TripCardTheme.MINIMAL_LIGHT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFF8FAFC)
                                    )
                                )
                            )
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val step = 32.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(Color(0xFFE2E8F0).copy(alpha = 0.7f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                            x += step
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(Color(0xFFE2E8F0).copy(alpha = 0.7f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                            y += step
                        }
                    }
                }

                TripCardTheme.CUSTOM_PHOTO -> {
                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap,
                            contentDescription = "Foto Latar Tour",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Scenic highland golden-hour fallback gradient when no photo picked yet
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF312E81),
                                            Color(0xFF9A3412),
                                            Color(0xFF1E293B),
                                            Color(0xFF090D16)
                                        )
                                    )
                                )
                        )
                    }
                    // Measured high-contrast scrim
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.55f),
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )
                }
            }

            // 2. Foreground Strava-style Content Layer
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header: Brand Logo & Tour Leader Avatar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = Color(0xFFEA580C),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Navigation,
                                    contentDescription = "KonvoiKu",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "KonvoiKu",
                                color = primaryText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = trip.roomId,
                                color = secondaryText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = headerDisplayName,
                                color = primaryText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = headerRoleLabel,
                                color = if (isLight) Color(0xFF1D4ED8) else Color(0xFFFDBA74),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        ConvoyAvatar(
                            preset = headerAvatarPreset,
                            photoUri = headerAvatarPhotoUri,
                            displayName = headerDisplayName,
                            size = 34.dp,
                            borderColor = Color(0xFFEA580C),
                            borderWidth = 2.dp
                        )
                    }
                }

                // Title & Date
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = trip.tourTitle,
                        color = primaryText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${trip.dateLabel} · ${trip.destinationName}",
                        color = secondaryText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center Mini Route Polyline Visual
                val isSatellite = theme == TripCardTheme.SATELLITE
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .then(
                            if (isSatellite) {
                                Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black.copy(alpha = 0.25f))
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val density = LocalDensity.current

                    val projection = remember(
                        trip.id,
                        trip.waypoints,
                        trip.visitedCheckpoints,
                        widthPx,
                        heightPx
                    ) {
                        GeoUtils.projectTripMapViewport(
                            waypoints = trip.waypoints,
                            checkpoints = trip.visitedCheckpoints,
                            width = widthPx,
                            height = heightPx,
                            paddingFraction = 0.13f
                        )
                    }

                    val satTileBitmaps = produceState<Map<String, Pair<ImageBitmap?, ImageBitmap?>>>(
                        initialValue = emptyMap(),
                        key1 = isSatellite,
                        key2 = projection.tiles
                    ) {
                        if (!isSatellite || !projection.hasGeo || projection.tiles.isEmpty()) {
                            value = emptyMap()
                            return@produceState
                        }
                        value = supervisorScope {
                            projection.tiles.map { tile ->
                                async {
                                    val sat = async { TomTomApiService.fetchMapTileBitmap("sat", "main", tile.z, tile.x, tile.y)?.asImageBitmap() }
                                    val lbl = async { TomTomApiService.fetchMapTileBitmap("labels", "main", tile.z, tile.x, tile.y)?.asImageBitmap() }
                                    tile.key to (sat.await() to lbl.await())
                                }
                            }.awaitAll().toMap()
                        }
                    }.value

                    val routeStrokeColor = if (isLight) Color(0xFF1D4ED8) else Color(0xFFFB923C)
                    val routeGlowColor = if (isLight) Color(0xFF93C5FD).copy(alpha = 0.5f) else Color(0xFFEA580C).copy(alpha = 0.35f)

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (isSatellite) {
                            projection.tiles.forEach { tile ->
                                val pair = satTileBitmaps[tile.key]
                                val satBmp = pair?.first
                                val lblBmp = pair?.second
                                val tilePx = ceil(tile.size).toInt() + 1
                                val offset = IntOffset(tile.left.roundToInt(), tile.top.roundToInt())
                                val dstSize = IntSize(tilePx, tilePx)
                                if (satBmp != null) {
                                    drawImage(image = satBmp, dstOffset = offset, dstSize = dstSize)
                                }
                                if (lblBmp != null) {
                                    drawImage(image = lblBmp, dstOffset = offset, dstSize = dstSize, alpha = 0.85f)
                                }
                            }
                            if (satTileBitmaps.isNotEmpty()) {
                                drawRect(Color(0xFF071315).copy(alpha = 0.22f))
                            }
                        }

                        val pts = projection.waypoints
                        if (pts.size >= 2) {
                            val path = androidx.compose.ui.graphics.Path().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = routeGlowColor,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                            drawPath(
                                path = path,
                                color = routeStrokeColor,
                                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }

                        pts.firstOrNull()?.let { start ->
                            val c = Offset(start.x, start.y)
                            drawCircle(Color.White, radius = 8.dp.toPx(), center = c)
                            drawCircle(Color(0xFFEA580C), radius = 5.dp.toPx(), center = c)
                        }
                        pts.lastOrNull()?.let { end ->
                            val c = Offset(end.x, end.y)
                            drawCircle(Color.White, radius = 8.dp.toPx(), center = c)
                            drawCircle(Color(0xFF16A34A), radius = 5.dp.toPx(), center = c)
                        }
                    }

                    trip.visitedCheckpoints.forEachIndexed { idx, cp ->
                        val projectedCp = projection.checkpoints.getOrNull(idx) ?: return@forEachIndexed
                        val px = projectedCp.x.roundToInt()
                        val py = projectedCp.y.roundToInt()
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0F766E),
                            border = BorderStroke(1.5.dp, Color.White),
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = px - with(density) { 10.dp.roundToPx() },
                                        y = py - with(density) { 10.dp.roundToPx() }
                                    )
                                }
                                .size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalCafe,
                                    contentDescription = cp.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Stats Grid + Participant Footer
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CardStatColumn(
                            label = "Jarak",
                            value = String.format(Locale.US, "%.1f km", trip.distanceKm),
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                        CardStatColumn(
                            label = "Durasi",
                            value = trip.durationLabel,
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                        CardStatColumn(
                            label = "Rata-rata",
                            value = "${trip.avgSpeedKmh} km/j",
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                        CardStatColumn(
                            label = "Checkpoint",
                            value = "${trip.visitedCheckpoints.size} titik",
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(cardBorder.copy(alpha = 0.65f))
                    )

                    Text(
                        text = if (trip.participants.isNotEmpty()) {
                            "Anggota Konvoi (${trip.participants.size}): ${trip.participants.joinToString(", ") { it.fullName }}"
                        } else {
                            "Peserta konvoi belum tercatat"
                        },
                        color = secondaryText,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CardStatColumn(
    label: String,
    value: String,
    primaryText: Color,
    secondaryText: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            color = secondaryText,
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = primaryText,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}

suspend fun renderTripCardBitmapToFile(
    context: Context,
    trip: TripHistoryRecord,
    theme: TripCardTheme,
    customPhotoUri: String?,
    currentUserName: String? = null,
    currentUserAvatarPreset: AvatarPreset? = null,
    currentUserAvatarPhotoUri: String? = null
): File = withContext(Dispatchers.IO) {
    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val isLight = theme == TripCardTheme.MINIMAL_LIGHT
    val primaryColor = if (isLight) AndroidColor.parseColor("#0F172A") else AndroidColor.WHITE
    val secondaryColor = if (isLight) AndroidColor.parseColor("#475569") else AndroidColor.parseColor("#CBD5E1")
    val borderColor = when (theme) {
        TripCardTheme.DARK_CARBON -> AndroidColor.parseColor("#334155")
        TripCardTheme.SATELLITE -> AndroidColor.parseColor("#1E4E45")
        TripCardTheme.MINIMAL_LIGHT -> AndroidColor.parseColor("#CBD5E1")
        TripCardTheme.CUSTOM_PHOTO -> AndroidColor.parseColor("#475569")
    }

    // 1. Paint Background
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    when (theme) {
        TripCardTheme.DARK_CARBON -> {
            bgPaint.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    AndroidColor.parseColor("#090E17"),
                    AndroidColor.parseColor("#111827"),
                    AndroidColor.parseColor("#172133")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.argb(14, 255, 255, 255)
                strokeWidth = 2f
            }
            var gx = 0f
            while (gx < width) {
                canvas.drawLine(gx, 0f, gx, height.toFloat(), gridPaint)
                gx += 72f
            }
            var gy = 0f
            while (gy < height) {
                canvas.drawLine(0f, gy, width.toFloat(), gy, gridPaint)
                gy += 72f
            }
        }

        TripCardTheme.SATELLITE -> {
            bgPaint.shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    AndroidColor.parseColor("#0B1F1D"),
                    AndroidColor.parseColor("#112A2B"),
                    AndroidColor.parseColor("#0B1821")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = AndroidColor.argb(28, 45, 212, 191)
            }
            for (i in 1..6) {
                canvas.drawCircle(width * 0.72f, height * 0.42f, width * 0.18f * i, contourPaint)
            }
        }

        TripCardTheme.MINIMAL_LIGHT -> {
            bgPaint.color = AndroidColor.WHITE
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.parseColor("#E2E8F0")
                strokeWidth = 2f
            }
            var gx = 0f
            while (gx < width) {
                canvas.drawLine(gx, 0f, gx, height.toFloat(), gridPaint)
                gx += 84f
            }
            var gy = 0f
            while (gy < height) {
                canvas.drawLine(0f, gy, width.toFloat(), gy, gridPaint)
                gy += 84f
            }
        }

        TripCardTheme.CUSTOM_PHOTO -> {
            val photoBmp = customPhotoUri?.let { uriStr ->
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                        BitmapFactory.decodeStream(it)
                    }
                }.getOrNull()
            }
            if (photoBmp != null) {
                val scale = maxOf(width.toFloat() / photoBmp.width, height.toFloat() / photoBmp.height)
                val sw = photoBmp.width * scale
                val sh = photoBmp.height * scale
                val left = (width - sw) / 2f
                val top = (height - sh) / 2f
                canvas.drawBitmap(photoBmp, null, RectF(left, top, left + sw, top + sh), bgPaint)
            } else {
                bgPaint.shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        AndroidColor.parseColor("#312E81"),
                        AndroidColor.parseColor("#9A3412"),
                        AndroidColor.parseColor("#090D16")
                    ),
                    null,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
            }
            val scrimPaint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, height.toFloat(),
                    intArrayOf(
                        AndroidColor.argb(145, 0, 0, 0),
                        AndroidColor.argb(90, 0, 0, 0),
                        AndroidColor.argb(225, 0, 0, 0)
                    ),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        }
    }

    val pad = 72f

    // 2. Top Brand Lockup
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#EA580C")
    }
    canvas.drawRoundRect(RectF(pad, pad, pad + 72f, pad + 72f), 20f, 20f, badgePaint)
    val navArrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    val arrowPath = Path().apply {
        moveTo(pad + 36f, pad + 16f)
        lineTo(pad + 56f, pad + 54f)
        lineTo(pad + 36f, pad + 44f)
        lineTo(pad + 16f, pad + 54f)
        close()
    }
    canvas.drawPath(arrowPath, navArrowPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textSize = 40f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("KonvoiKu", pad + 92f, pad + 36f, titlePaint)

    val monoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryColor
        textSize = 26f
        typeface = Typeface.MONOSPACE
    }
    canvas.drawText(trip.roomId, pad + 92f, pad + 68f, monoPaint)

    // Logged-in User Avatar & Role (Right Side)
    val leader = trip.participants.firstOrNull { it.role == UserRole.TOUR_LEADER }
        ?: trip.participants.firstOrNull()
    val trimmedLoginName = currentUserName?.trim().orEmpty()
    val matchedSelf = if (trimmedLoginName.isNotEmpty()) {
        trip.participants.firstOrNull {
            it.fullName.trim().equals(trimmedLoginName, ignoreCase = true)
        }
    } else null
    val headerName = trimmedLoginName.ifBlank { leader?.fullName ?: "Rider KonvoiKu" }
    val headerPreset = currentUserAvatarPreset
        ?: matchedSelf?.avatarPreset
        ?: leader?.avatarPreset
        ?: AvatarPreset.HELMET_TOURING
    val headerPhotoUri = currentUserAvatarPhotoUri
        ?: matchedSelf?.avatarPhotoUri
        ?: if (trimmedLoginName.isNotEmpty()) null else leader?.avatarPhotoUri
    val headerRoleLabel = when {
        trip.userRole == UserRole.TOUR_LEADER -> "Tour Leader"
        matchedSelf?.isSweeper == true -> "Sweeper"
        else -> "Anggota Konvoi"
    }

    run {
        val avRadius = 42f
        val avCx = width - pad - avRadius
        val avCy = pad + 36f

        val userBmp = headerPhotoUri?.let { uriStr ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                    BitmapFactory.decodeStream(it)
                }
            }.getOrNull()
        }

        if (userBmp != null) {
            val scaled = Bitmap.createScaledBitmap(userBmp, (avRadius * 2).toInt(), (avRadius * 2).toInt(), true)
            val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            canvas.save()
            canvas.translate(avCx - avRadius, avCy - avRadius)
            canvas.drawCircle(avRadius, avRadius, avRadius, circlePaint)
            canvas.restore()
        } else {
            val avBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = when (headerPreset) {
                    AvatarPreset.HELMET_SPORT -> AndroidColor.parseColor("#1D4ED8")
                    AvatarPreset.PILOT_CLASSIC -> AndroidColor.parseColor("#0F766E")
                    else -> AndroidColor.parseColor("#EA580C")
                }
            }
            canvas.drawCircle(avCx, avCy, avRadius, avBg)
            val initPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = AndroidColor.WHITE
                textSize = 32f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            val initial = headerName.firstOrNull()?.uppercaseChar()?.toString() ?: "R"
            canvas.drawText(initial, avCx, avCy + 11f, initPaint)
        }

        val avBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = AndroidColor.parseColor("#EA580C")
        }
        canvas.drawCircle(avCx, avCy, avRadius, avBorder)

        val userNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = primaryColor
            textSize = 28f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(headerName.take(26), avCx - avRadius - 20f, pad + 32f, userNamePaint)
        val userRolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isLight) AndroidColor.parseColor("#1D4ED8") else AndroidColor.parseColor("#FDBA74")
            textSize = 22f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(headerRoleLabel, avCx - avRadius - 20f, pad + 64f, userRolePaint)
    }

    // 3. Tour Title & Date
    val tourTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textSize = 54f
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText(trip.tourTitle.take(30), pad, 248f, tourTitlePaint)

    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryColor
        textSize = 28f
    }
    canvas.drawText("${trip.dateLabel} · ${trip.destinationName}", pad, 296f, subPaint)

    // 4. Polyline Map Area
    val mapLeft = pad
    val mapTop = 350f
    val mapWidth = width - pad * 2f
    val mapHeight = 620f
    val projection = GeoUtils.projectTripMapViewport(
        waypoints = trip.waypoints,
        checkpoints = trip.visitedCheckpoints,
        width = mapWidth,
        height = mapHeight,
        paddingFraction = 0.13f
    )

    if (theme == TripCardTheme.SATELLITE && projection.hasGeo && projection.tiles.isNotEmpty()) {
        val loadedTiles = supervisorScope {
            projection.tiles.map { tile ->
                async {
                    val sat = async { TomTomApiService.fetchMapTileBitmap("sat", "main", tile.z, tile.x, tile.y) }
                    val lbl = async { TomTomApiService.fetchMapTileBitmap("labels", "main", tile.z, tile.x, tile.y) }
                    Triple(tile, sat.await(), lbl.await())
                }
            }.awaitAll()
        }

        val mapBox = RectF(mapLeft, mapTop, mapLeft + mapWidth, mapTop + mapHeight)
        val clipPath = Path().apply {
            addRoundRect(mapBox, 32f, 32f, Path.Direction.CW)
        }
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.translate(mapLeft, mapTop)
        val tileBmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 215 }
        loadedTiles.forEach { (tile, satBmp, lblBmp) ->
            val dst = RectF(tile.left, tile.top, tile.left + tile.size + 1f, tile.top + tile.size + 1f)
            if (satBmp != null) {
                canvas.drawBitmap(satBmp, null, dst, tileBmpPaint)
            }
            if (lblBmp != null) {
                canvas.drawBitmap(lblBmp, null, dst, labelPaint)
            }
        }
        canvas.restore()

        canvas.drawRoundRect(
            mapBox,
            32f,
            32f,
            Paint().apply { color = AndroidColor.argb(56, 7, 19, 21) }
        )
        canvas.drawRoundRect(
            mapBox,
            32f,
            32f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = AndroidColor.argb(50, 255, 255, 255)
            }
        )
    }

    val pts = projection.waypoints
    if (pts.size >= 2) {
        val routePath = Path().apply {
            moveTo(mapLeft + pts.first().x, mapTop + pts.first().y)
            for (i in 1 until pts.size) {
                lineTo(mapLeft + pts[i].x, mapTop + pts[i].y)
            }
        }
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 36f
            color = if (isLight) AndroidColor.argb(110, 147, 197, 253) else AndroidColor.argb(95, 234, 88, 12)
        }
        canvas.drawPath(routePath, glowPaint)

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 14f
            color = if (isLight) AndroidColor.parseColor("#1D4ED8") else AndroidColor.parseColor("#FB923C")
        }
        canvas.drawPath(routePath, linePaint)
    }

    pts.firstOrNull()?.let { start ->
        val cx = mapLeft + start.x
        val cy = mapTop + start.y
        canvas.drawCircle(cx, cy, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
        canvas.drawCircle(cx, cy, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.parseColor("#EA580C") })
    }
    pts.lastOrNull()?.let { end ->
        val cx = mapLeft + end.x
        val cy = mapTop + end.y
        canvas.drawCircle(cx, cy, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
        canvas.drawCircle(cx, cy, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.parseColor("#16A34A") })
    }

    projection.checkpoints.forEach { cp ->
        val cx = mapLeft + cp.x
        val cy = mapTop + cp.y
        canvas.drawCircle(cx, cy, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
        canvas.drawCircle(cx, cy, 13f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.parseColor("#0F766E") })
    }

    // 5. Bottom Stats Grid
    val statsLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryColor
        textSize = 26f
    }
    val statsValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = primaryColor
        textSize = 42f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    val colWidth = (width - pad * 2f) / 4f
    val stats = listOf(
        "Jarak" to String.format(Locale.US, "%.1f km", trip.distanceKm),
        "Durasi" to trip.durationLabel,
        "Rata-rata" to "${trip.avgSpeedKmh} km/j",
        "Checkpoint" to "${trip.visitedCheckpoints.size} titik"
    )
    stats.forEachIndexed { idx, (label, valStr) ->
        val x = pad + colWidth * idx
        canvas.drawText(label, x, 1062f, statsLabelPaint)
        canvas.drawText(valStr, x, 1116f, statsValuePaint)
    }

    // Divider & Participant Footer
    val divPaint = Paint().apply {
        color = borderColor
        strokeWidth = 2f
    }
    canvas.drawLine(pad, 1166f, width - pad, 1166f, divPaint)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = secondaryColor
        textSize = 24f
    }
    val membersText = if (trip.participants.isNotEmpty()) {
        "Anggota Konvoi (${trip.participants.size}): ${trip.participants.joinToString(", ") { it.fullName }}"
    } else {
        "Peserta konvoi belum tercatat"
    }
    drawWrappedCanvasText(
        canvas = canvas,
        text = membersText,
        x = pad,
        startY = 1212f,
        maxWidth = width - pad * 2f,
        lineHeight = 34f,
        maxLines = 3,
        paint = footerPaint
    )

    val folder = File(context.cacheDir, "shared_cards").apply { mkdirs() }
    val outFile = File(folder, "konvoiku-card-${trip.roomId.lowercase()}.png")
    FileOutputStream(outFile).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }
    outFile
}

private fun drawWrappedCanvasText(
    canvas: Canvas,
    text: String,
    x: Float,
    startY: Float,
    maxWidth: Float,
    lineHeight: Float,
    maxLines: Int,
    paint: Paint
) {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    for (word in words) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (paint.measureText(candidate) <= maxWidth) {
            current = candidate
        } else {
            if (current.isNotEmpty()) lines.add(current)
            current = word
        }
    }
    if (current.isNotEmpty()) lines.add(current)

    val visible = lines.take(maxLines).toMutableList()
    if (lines.size > maxLines && visible.isNotEmpty()) {
        var last = visible.last()
        while (last.length > 3 && paint.measureText("$last...") > maxWidth) {
            last = last.dropLast(1)
        }
        visible[visible.lastIndex] = "$last..."
    }
    visible.forEachIndexed { idx, line ->
        canvas.drawText(line, x, startY + idx * lineHeight, paint)
    }
}

fun shareTripCardImage(
    context: Context,
    imageFile: File,
    trip: TripHistoryRecord
): Boolean {
    return runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        val caption = "🏁 ${trip.tourTitle} (${trip.roomId})\n" +
            "Jarak: ${String.format(Locale.US, "%.1f", trip.distanceKm)} km · Durasi: ${trip.durationLabel} · Kec. Rata-rata: ${trip.avgSpeedKmh} km/j\n" +
            "Direkam dengan KonvoiKu"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            putExtra(Intent.EXTRA_SUBJECT, "Kartu Tour ${trip.tourTitle}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Bagikan Kartu Tour (Share Image)")
        )
        true
    }.getOrDefault(false)
}
