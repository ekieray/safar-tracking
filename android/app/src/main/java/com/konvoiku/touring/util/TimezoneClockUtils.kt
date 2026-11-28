package com.konvoiku.touring.util

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.roundToInt

data class ResolvedLocationZone(
    val timeZoneId: String?,
    val fixedOffsetMinutes: Int?,
    val zoneCode: String,
    val regionLabel: String,
    val isFromCoordinates: Boolean
)

data class DualClockSnapshot(
    val localTime: String,
    val localDateLabel: String,
    val localZoneCode: String,
    val localRegionLabel: String,
    val wibTime: String,
    val wibDateLabel: String,
    val diffToWibLabel: String,
    val isSameAsWib: Boolean,
    val isFromCoordinates: Boolean
)

object TimezoneClockUtils {
    private const val WIB_OFFSET_MINUTES = 7 * 60

    fun resolveLocationTimeZone(
        latitude: Double?,
        longitude: Double?
    ): ResolvedLocationZone {
        val hasValidCoords = latitude != null &&
            longitude != null &&
            latitude.isFinite() &&
            longitude.isFinite() &&
            !(abs(latitude) < 0.001 && abs(longitude) < 0.001)

        if (!hasValidCoords) {
            val deviceTz = TimeZone.getDefault()
            val offsetMins = deviceTz.getOffset(System.currentTimeMillis()) / 60_000
            val code = when (offsetMins) {
                WIB_OFFSET_MINUTES -> "WIB"
                8 * 60 -> "WITA"
                9 * 60 -> "WIT"
                3 * 60 -> "WSA"
                else -> formatUtcOffsetLabel(offsetMins)
            }
            return ResolvedLocationZone(
                timeZoneId = deviceTz.id,
                fixedOffsetMinutes = offsetMins,
                zoneCode = code,
                regionLabel = "Lokasi Perangkat",
                isFromCoordinates = false
            )
        }

        val lat = latitude!!
        val lng = longitude!!

        // 1. Arab Saudi (Makkah, Madinah, Jeddah, Riyadh, dll.)
        if (lat in 16.0..32.2 && lng in 34.4..55.7) {
            val city = when {
                abs(lat - 21.4225) <= 0.45 && abs(lng - 39.8262) <= 0.45 -> "Makkah"
                abs(lat - 24.4672) <= 0.45 && abs(lng - 39.6111) <= 0.45 -> "Madinah"
                abs(lat - 21.5433) <= 0.45 && abs(lng - 39.1728) <= 0.45 -> "Jeddah"
                else -> "Arab Saudi"
            }
            return ResolvedLocationZone(
                timeZoneId = "Asia/Riyadh",
                fixedOffsetMinutes = 3 * 60,
                zoneCode = "WSA",
                regionLabel = city,
                isFromCoordinates = true
            )
        }

        // 2. Malaysia Semenanjung & Singapura
        if (lat in 1.23..7.0 && lng in 99.5..104.6) {
            val isSingapore = lat <= 1.48 && lng in 103.55..104.12
            return ResolvedLocationZone(
                timeZoneId = if (isSingapore) "Asia/Singapore" else "Asia/Kuala_Lumpur",
                fixedOffsetMinutes = 8 * 60,
                zoneCode = if (isSingapore) "SGT" else "MYT",
                regionLabel = if (isSingapore) "Singapura" else "Malaysia",
                isFromCoordinates = true
            )
        }

        // 3. Indonesia (WIB / WITA / WIT)
        if (lat in -11.2..6.2 && lng in 94.9..141.1) {
            return when {
                lng >= 125.7 && lat <= 2.5 -> ResolvedLocationZone(
                    timeZoneId = "Asia/Jayapura",
                    fixedOffsetMinutes = 9 * 60,
                    zoneCode = "WIT",
                    regionLabel = "Indonesia Timur",
                    isFromCoordinates = true
                )
                lng >= 114.55 || (lat in -4.2..4.3 && lng >= 114.3) -> ResolvedLocationZone(
                    timeZoneId = "Asia/Makassar",
                    fixedOffsetMinutes = 8 * 60,
                    zoneCode = "WITA",
                    regionLabel = "Indonesia Tengah",
                    isFromCoordinates = true
                )
                else -> ResolvedLocationZone(
                    timeZoneId = "Asia/Jakarta",
                    fixedOffsetMinutes = WIB_OFFSET_MINUTES,
                    zoneCode = "WIB",
                    regionLabel = "Indonesia Barat",
                    isFromCoordinates = true
                )
            }
        }

        // 4. UEA (Dubai / Abu Dhabi)
        if (lat in 22.6..26.2 && lng in 51.5..56.5) {
            return ResolvedLocationZone(
                timeZoneId = "Asia/Dubai",
                fixedOffsetMinutes = 4 * 60,
                zoneCode = "GST",
                regionLabel = "UEA",
                isFromCoordinates = true
            )
        }

        // 5. Mesir
        if (lat in 22.0..31.8 && lng in 24.7..34.39) {
            return ResolvedLocationZone(
                timeZoneId = "Africa/Cairo",
                fixedOffsetMinutes = 2 * 60,
                zoneCode = "EET",
                regionLabel = "Mesir",
                isFromCoordinates = true
            )
        }

        // 6. Yordania / Palestina / Syam
        if (lat in 29.2..33.4 && lng in 34.2..39.4) {
            return ResolvedLocationZone(
                timeZoneId = "Asia/Amman",
                fixedOffsetMinutes = 3 * 60,
                zoneCode = "AST",
                regionLabel = "Yordania / Syam",
                isFromCoordinates = true
            )
        }

        // 7. Turki
        if (lat in 35.8..42.2 && lng in 25.6..44.8) {
            return ResolvedLocationZone(
                timeZoneId = "Europe/Istanbul",
                fixedOffsetMinutes = 3 * 60,
                zoneCode = "TRT",
                regionLabel = "Turki",
                isFromCoordinates = true
            )
        }

        // 8. Global Longitude Fallback
        val estHours = (lng / 15.0).roundToInt().coerceIn(-12, 14)
        val estOffsetMins = estHours * 60
        return ResolvedLocationZone(
            timeZoneId = null,
            fixedOffsetMinutes = estOffsetMins,
            zoneCode = formatUtcOffsetLabel(estOffsetMins),
            regionLabel = "Zona Lokasi",
            isFromCoordinates = true
        )
    }

    private fun formatUtcOffsetLabel(offsetMinutes: Int): String {
        if (offsetMinutes == 0) return "UTC"
        val sign = if (offsetMinutes > 0) "+" else "-"
        val absMins = abs(offsetMinutes)
        val hours = absMins / 60
        val mins = absMins % 60
        return if (mins == 0) {
            "UTC$sign$hours"
        } else {
            String.format(Locale.US, "UTC%s%d:%02d", sign, hours, mins)
        }
    }

    private fun formatForZone(
        nowMs: Long,
        timeZoneId: String?,
        fallbackOffsetMinutes: Int
    ): Triple<String, String, Int> {
        val tz: TimeZone = if (!timeZoneId.isNullOrBlank()) {
            TimeZone.getTimeZone(timeZoneId)
        } else {
            val sign = if (fallbackOffsetMinutes >= 0) "+" else "-"
            val absMins = abs(fallbackOffsetMinutes)
            val hh = absMins / 60
            val mm = absMins % 60
            TimeZone.getTimeZone(String.format(Locale.US, "GMT%s%02d:%02d", sign, hh, mm))
        }

        val idLocale = Locale("in", "ID")
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US).apply { timeZone = tz }
        val dateFmt = SimpleDateFormat("EEE, dd MMM", idLocale).apply { timeZone = tz }
        val date = Date(nowMs)
        val offsetMins = tz.getOffset(nowMs) / 60_000
        return Triple(timeFmt.format(date), dateFmt.format(date), offsetMins)
    }

    fun buildDualClockSnapshot(
        nowMs: Long,
        latitude: Double?,
        longitude: Double?
    ): DualClockSnapshot {
        val resolved = resolveLocationTimeZone(latitude, longitude)
        val (localTime, localDate, localOffset) = formatForZone(
            nowMs = nowMs,
            timeZoneId = resolved.timeZoneId,
            fallbackOffsetMinutes = resolved.fixedOffsetMinutes ?: WIB_OFFSET_MINUTES
        )
        val (wibTime, wibDate, wibOffset) = formatForZone(
            nowMs = nowMs,
            timeZoneId = "Asia/Jakarta",
            fallbackOffsetMinutes = WIB_OFFSET_MINUTES
        )

        val diffMins = localOffset - wibOffset
        val isSame = diffMins == 0
        val diffLabel = if (isSame) {
            "= WIB"
        } else {
            val sign = if (diffMins > 0) "+" else "-"
            val absMins = abs(diffMins)
            val hrs = absMins / 60
            val rem = absMins % 60
            if (rem == 0) "${sign}${hrs}j dari WIB" else "${sign}${hrs}j${rem}m"
        }

        return DualClockSnapshot(
            localTime = localTime,
            localDateLabel = localDate,
            localZoneCode = resolved.zoneCode,
            localRegionLabel = resolved.regionLabel,
            wibTime = wibTime,
            wibDateLabel = wibDate,
            diffToWibLabel = diffLabel,
            isSameAsWib = isSame,
            isFromCoordinates = resolved.isFromCoordinates
        )
    }
}

@Composable
fun DualTimezoneClockBar(
    latitude: Double?,
    longitude: Double?,
    subjectName: String? = null,
    compact: Boolean = true,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val clock = remember(nowMs, latitude, longitude) {
        TimezoneClockUtils.buildDualClockSnapshot(nowMs, latitude, longitude)
    }
    val locationSubtitle = if (!subjectName.isNullOrBlank()) {
        "$subjectName · ${clock.localRegionLabel}"
    } else {
        clock.localRegionLabel
    }

    if (compact) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "Waktu Lokasi Jamaah",
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = locationSubtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = clock.localTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFECFDF5)
                    ) {
                        Text(
                            text = clock.localZoneCode,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = "Jam Fix WIB",
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = clock.wibTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "WIB",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFECFDF5).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = locationSubtitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF047857)
                        ) {
                            Text(
                                text = clock.localZoneCode,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = clock.localTime,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0B1F16)
                        )
                        Text(
                            text = clock.localDateLabel,
                            fontSize = 9.sp,
                            color = Color(0xFF4D6659)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFFFFBEB).copy(alpha = 0.92f),
                border = BorderStroke(1.dp, Color(0xFFFDE68A))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Waktu Indonesia Barat",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFB45309)
                        ) {
                            Text(
                                text = "WIB",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = clock.wibTime,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0B1F16)
                        )
                        Text(
                            text = if (clock.isSameAsWib) clock.wibDateLabel else clock.diffToWibLabel,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }
        }
    }
}
