package com.konvoiku.touring.util

import com.konvoiku.touring.model.ConnectionStatus
import com.konvoiku.touring.model.ConvoyMember
import com.konvoiku.touring.model.MemberConditionStatus
import com.konvoiku.touring.model.NavigationGuidanceUiState
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePoint
import com.konvoiku.touring.model.UserRole
import com.konvoiku.touring.model.VisitedCheckpointLog
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLngGeo(val lat: Double, val lng: Double)

data class RouteEvaluationResult(
    val routeProgress: Float,
    val distanceTraveledMeters: Int,
    val remainingMeters: Int,
    val offRouteMeters: Int
)

data class ProjectedViewportPoint(
    val x: Float,
    val y: Float
)

data class ProjectedMapTile(
    val key: String,
    val z: Int,
    val x: Int,
    val y: Int,
    val left: Float,
    val top: Float,
    val size: Float
)

data class TripMapViewportProjection(
    val waypoints: List<ProjectedViewportPoint>,
    val checkpoints: List<ProjectedViewportPoint>,
    val tiles: List<ProjectedMapTile>,
    val hasGeo: Boolean,
    val tileZoom: Int
)

data class ClosestCoLeaderResult(
    val coLeaderUid: String?,
    val distanceByMemberId: Map<String, Int>
)

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun hasValidGeoCoord(lat: Double, lng: Double): Boolean =
        lat.isFinite() && lng.isFinite() && (abs(lat) > 0.001 || abs(lng) > 0.001)

    fun haversineMeters(a: LatLngGeo, b: LatLngGeo): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)

        val sinDLat = sin(dLat / 2.0)
        val sinDLng = sin(dLng / 2.0)
        val h = sinDLat * sinDLat + cos(lat1) * cos(lat2) * sinDLng * sinDLng
        return 2.0 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(h)))
    }

    fun computePolylineLengthMeters(points: List<LatLngGeo>): Int {
        if (points.size < 2) return 0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += haversineMeters(points[i], points[i + 1])
        }
        return total.roundToInt()
    }

    fun evaluatePositionAlongRoute(
        pos: LatLngGeo,
        waypoints: List<RoutePoint>,
        totalRouteMetersFallback: Int = 12000
    ): RouteEvaluationResult {
        if (waypoints.size < 2) {
            val dest = waypoints.firstOrNull()
            val dist = if (dest != null) {
                haversineMeters(pos, LatLngGeo(dest.latitude, dest.longitude)).roundToInt()
            } else 0
            return RouteEvaluationResult(
                routeProgress = 0f,
                distanceTraveledMeters = 0,
                remainingMeters = dist,
                offRouteMeters = dist
            )
        }

        val refLatRad = Math.toRadians(waypoints.first().latitude)
        val metersPerDegLat = 111_132.92
        val metersPerDegLng = 111_412.84 * cos(refLatRad)

        fun toXY(pt: LatLngGeo): Pair<Double, Double> =
            (pt.lng - waypoints.first().longitude) * metersPerDegLng to
                (pt.lat - waypoints.first().latitude) * metersPerDegLat

        val (px, py) = toXY(pos)
        var cumulative = 0.0
        val segStarts = DoubleArray(waypoints.size - 1)
        val segLengths = DoubleArray(waypoints.size - 1)

        for (i in 0 until waypoints.size - 1) {
            segStarts[i] = cumulative
            val len = haversineMeters(
                LatLngGeo(waypoints[i].latitude, waypoints[i].longitude),
                LatLngGeo(waypoints[i + 1].latitude, waypoints[i + 1].longitude)
            )
            segLengths[i] = len
            cumulative += len
        }

        val totalMeters = cumulative.coerceAtLeast(totalRouteMetersFallback.toDouble()).coerceAtLeast(100.0)
        var minPerpMeters = Double.MAX_VALUE
        var bestTraveled = 0.0

        for (i in 0 until waypoints.size - 1) {
            val (ax, ay) = toXY(LatLngGeo(waypoints[i].latitude, waypoints[i].longitude))
            val (bx, by) = toXY(LatLngGeo(waypoints[i + 1].latitude, waypoints[i + 1].longitude))
            val abx = bx - ax
            val aby = by - ay
            val abLenSq = abx * abx + aby * aby
            val t = if (abLenSq > 0.0) {
                (((px - ax) * abx + (py - ay) * aby) / abLenSq).coerceIn(0.0, 1.0)
            } else 0.0
            val projX = ax + t * abx
            val projY = ay + t * aby
            val perp = hypot(px - projX, py - projY)
            if (perp < minPerpMeters) {
                minPerpMeters = perp
                bestTraveled = segStarts[i] + t * segLengths[i]
            }
        }

        val progress = (bestTraveled / totalMeters).toFloat().coerceIn(0f, 1f)
        val remaining = (totalMeters - bestTraveled).roundToInt().coerceAtLeast(0)
        return RouteEvaluationResult(
            routeProgress = progress,
            distanceTraveledMeters = bestTraveled.roundToInt(),
            remainingMeters = remaining,
            offRouteMeters = if (minPerpMeters == Double.MAX_VALUE) 0 else minPerpMeters.roundToInt()
        )
    }

    fun computeBearingDegrees(a: LatLngGeo, b: LatLngGeo): Int {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLng = Math.toRadians(b.lng - a.lng)
        val y = sin(dLng) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).roundToInt()
    }

    fun offsetPointAlongHeading(
        lat: Double,
        lng: Double,
        headingDeg: Double,
        offsetMeters: Double = 90.0
    ): LatLngGeo {
        val brgRad = Math.toRadians(headingDeg)
        val dLat = (offsetMeters * cos(brgRad)) / 111_132.92
        val metersPerDegLng = (111_412.84 * cos(Math.toRadians(lat))).coerceAtLeast(1000.0)
        val dLng = (offsetMeters * sin(brgRad)) / metersPerDegLng
        return LatLngGeo(lat + dLat, lng + dLng)
    }

    fun compassDirectionShort(headingDeg: Int): String {
        val dirs = listOf("U", "TL", "T", "TG", "S", "BD", "B", "BL")
        val idx = (((headingDeg % 360) + 360) % 360 + 22) / 45 % 8
        return dirs[idx]
    }

    fun formatNavDistanceLabel(meters: Int): String {
        val safe = meters.coerceAtLeast(0)
        return if (safe >= 1000) {
            String.format(Locale.US, "%.1f km", safe / 1000.0)
        } else if (safe >= 100) {
            "${(safe / 10) * 10} m"
        } else {
            "$safe m"
        }
    }

    fun computeNavigationGuidance(
        lat: Double,
        lng: Double,
        speedKmh: Int,
        rawHeadingDeg: Int,
        waypoints: List<RoutePoint>,
        instructions: List<RouteInstruction>,
        offRouteMeters: Int,
        toleranceMeters: Int,
        totalRouteMeters: Int,
        routeProgress: Float
    ): NavigationGuidanceUiState {
        val traveledMeters = (routeProgress.coerceIn(0f, 1f) * totalRouteMeters.coerceAtLeast(100)).roundToInt()
        val remainingTotal = (totalRouteMeters - traveledMeters).coerceAtLeast(0)
        val isOffRoute = offRouteMeters > toleranceMeters

        var routeSegmentBearing = ((rawHeadingDeg % 360) + 360) % 360
        if (waypoints.size >= 2) {
            val segIdx = (routeProgress.coerceIn(0f, 0.999f) * (waypoints.size - 1))
                .toInt()
                .coerceIn(0, waypoints.size - 2)
            routeSegmentBearing = computeBearingDegrees(
                LatLngGeo(waypoints[segIdx].latitude, waypoints[segIdx].longitude),
                LatLngGeo(waypoints[segIdx + 1].latitude, waypoints[segIdx + 1].longitude)
            )
        }
        val effectiveHeading = if (speedKmh > 2 && rawHeadingDeg in 0..359) {
            rawHeadingDeg
        } else {
            routeSegmentBearing
        }

        if (isOffRoute) {
            return NavigationGuidanceUiState(
                instructionText = "Keluar jalur ${offRouteMeters} m — Kembali ke rute biru",
                streetName = "Peringatan Rute Konvoi",
                maneuver = "STRAIGHT",
                distanceToTurnLabel = formatNavDistanceLabel(offRouteMeters),
                distanceToTurnMeters = offRouteMeters,
                headingDegrees = effectiveHeading,
                speedKmh = speedKmh.coerceAtLeast(0),
                isOffRoute = true
            )
        }

        if (remainingTotal <= 60) {
            return NavigationGuidanceUiState(
                instructionText = "Anda tiba di titik tujuan konvoi",
                streetName = "Tujuan Tercapai",
                maneuver = "ARRIVE",
                distanceToTurnLabel = "Tiba",
                distanceToTurnMeters = 0,
                headingDegrees = effectiveHeading,
                speedKmh = speedKmh.coerceAtLeast(0),
                isOffRoute = false
            )
        }

        val upcoming = instructions
            .sortedBy { it.routeOffsetInMeters }
            .firstOrNull { it.routeOffsetInMeters >= traveledMeters - 25 }

        if (upcoming != null) {
            val distToTurn = (upcoming.routeOffsetInMeters - traveledMeters).coerceAtLeast(0)
            return NavigationGuidanceUiState(
                instructionText = upcoming.message.ifBlank { "Ikuti rute utama konvoi" },
                streetName = upcoming.street.ifBlank { "Jalur Konvoi TomTom" },
                maneuver = upcoming.maneuver.ifBlank { "STRAIGHT" },
                distanceToTurnLabel = if (distToTurn <= 25) "Sekarang" else formatNavDistanceLabel(distToTurn),
                distanceToTurnMeters = distToTurn,
                headingDegrees = effectiveHeading,
                speedKmh = speedKmh.coerceAtLeast(0),
                isOffRoute = false
            )
        }

        // Polyline bend-detection fallback when TomTom guidance array is unavailable
        if (waypoints.size >= 3) {
            val curSeg = (routeProgress.coerceIn(0f, 0.99f) * (waypoints.size - 1))
                .toInt()
                .coerceIn(0, waypoints.size - 2)
            for (i in (curSeg + 1) until waypoints.lastIndex) {
                val b1 = computeBearingDegrees(
                    LatLngGeo(waypoints[i - 1].latitude, waypoints[i - 1].longitude),
                    LatLngGeo(waypoints[i].latitude, waypoints[i].longitude)
                )
                val b2 = computeBearingDegrees(
                    LatLngGeo(waypoints[i].latitude, waypoints[i].longitude),
                    LatLngGeo(waypoints[i + 1].latitude, waypoints[i + 1].longitude)
                )
                var diff = b2 - b1
                while (diff > 180) diff -= 360
                while (diff < -180) diff += 360
                if (abs(diff) >= 28) {
                    val distToBend = haversineMeters(
                        LatLngGeo(lat, lng),
                        LatLngGeo(waypoints[i].latitude, waypoints[i].longitude)
                    ).roundToInt()
                    val (maneuver, text) = when {
                        diff >= 120 || diff <= -120 -> "TURN_AROUND" to "Putar balik mengikuti jalur"
                        diff >= 55 -> "TURN_RIGHT" to "Belok kanan mengikuti rute"
                        diff >= 28 -> "BEAR_RIGHT" to "Ambil lajur kanan"
                        diff <= -55 -> "TURN_LEFT" to "Belok kiri mengikuti rute"
                        else -> "BEAR_LEFT" to "Ambil lajur kiri"
                    }
                    return NavigationGuidanceUiState(
                        instructionText = text,
                        streetName = "Rute Resmi Konvoi",
                        maneuver = maneuver,
                        distanceToTurnLabel = formatNavDistanceLabel(distToBend),
                        distanceToTurnMeters = distToBend,
                        headingDegrees = effectiveHeading,
                        speedKmh = speedKmh.coerceAtLeast(0),
                        isOffRoute = false
                    )
                }
            }
        }

        return NavigationGuidanceUiState(
            instructionText = "Lurus terus menuju tujuan konvoi",
            streetName = "Jalur Resmi Konvoi",
            maneuver = "STRAIGHT",
            distanceToTurnLabel = formatNavDistanceLabel(remainingTotal),
            distanceToTurnMeters = remainingTotal,
            headingDegrees = effectiveHeading,
            speedKmh = speedKmh.coerceAtLeast(0),
            isOffRoute = false
        )
    }

    fun encodePolyline(points: List<LatLngGeo>): String {
        var lastLat = 0
        var lastLng = 0
        val out = StringBuilder()

        fun encodeValue(v: Int) {
            var value = if (v < 0) (v shl 1).inv() else v shl 1
            while (value >= 0x20) {
                out.append(((0x20 or (value and 0x1f)) + 63).toChar())
                value = value shr 5
            }
            out.append((value + 63).toChar())
        }

        for (pt in points) {
            val lat = (pt.lat * 1e5).roundToInt()
            val lng = (pt.lng * 1e5).roundToInt()
            encodeValue(lat - lastLat)
            encodeValue(lng - lastLng)
            lastLat = lat
            lastLng = lng
        }
        return out.toString()
    }

    fun decodePolyline(encoded: String): List<LatLngGeo> {
        if (encoded.isBlank()) return emptyList()
        val points = mutableListOf<LatLngGeo>()
        var index = 0
        var lat = 0
        var lng = 0
        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            val dLat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            if (index >= encoded.length) break
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            val dLng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            points.add(LatLngGeo(lat / 1e5, lng / 1e5))
        }
        return points
    }

    fun downsampleTrackPoints(
        points: List<LatLngGeo>,
        minSpacingMeters: Double = 15.0,
        maxPoints: Int = 150
    ): List<LatLngGeo> {
        if (points.isEmpty()) return emptyList()
        val spaced = mutableListOf(points.first())
        for (i in 1 until points.size - 1) {
            val prev = spaced.last()
            if (haversineMeters(prev, points[i]) >= minSpacingMeters) {
                spaced.add(points[i])
            }
        }
        if (points.size > 1) {
            spaced.add(points.last())
        }
        return downsampleLatLng(spaced, maxPoints)
    }

    fun downsampleLatLng(
        points: List<LatLngGeo>,
        maxPoints: Int = 120
    ): List<LatLngGeo> {
        if (points.size <= maxPoints || maxPoints < 2) return points
        val step = (points.size - 1).toDouble() / (maxPoints - 1).toDouble()
        return List(maxPoints) { i ->
            val idx = if (i == maxPoints - 1) points.lastIndex else (i * step).roundToInt().coerceIn(0, points.lastIndex)
            points[idx]
        }
    }

    fun toRoutePoints(points: List<LatLngGeo>): List<RoutePoint> {
        if (points.isEmpty()) return emptyList()
        var minLat = Double.POSITIVE_INFINITY
        var maxLat = Double.NEGATIVE_INFINITY
        var minLng = Double.POSITIVE_INFINITY
        var maxLng = Double.NEGATIVE_INFINITY
        for (pt in points) {
            if (pt.lat < minLat) minLat = pt.lat
            if (pt.lat > maxLat) maxLat = pt.lat
            if (pt.lng < minLng) minLng = pt.lng
            if (pt.lng > maxLng) maxLng = pt.lng
        }
        val avgLatRad = ((minLat + maxLat) / 2.0) * PI / 180.0
        val lngScale = max(0.2, cos(avgLatRad))
        val rawLatSpan = max(0.002, maxLat - minLat)
        val rawLngSpan = max(0.002, (maxLng - minLng) * lngScale)
        val maxSpan = max(rawLatSpan, rawLngSpan)
        val centerLat = (minLat + maxLat) / 2.0
        val centerLng = (minLng + maxLng) / 2.0

        return points.map { pt ->
            RoutePoint(
                xFraction = (0.5 + (((pt.lng - centerLng) * lngScale) / maxSpan) * 0.8)
                    .toFloat().coerceIn(0.08f, 0.92f),
                yFraction = (0.5 - ((pt.lat - centerLat) / maxSpan) * 0.8)
                    .toFloat().coerceIn(0.08f, 0.92f),
                latitude = pt.lat,
                longitude = pt.lng
            )
        }
    }

    private fun mercatorNormX(lng: Double): Double = (lng + 180.0) / 360.0

    private fun mercatorNormY(lat: Double): Double {
        val clamped = lat.coerceIn(-85.05112878, 85.05112878)
        val sinLat = sin(clamped * PI / 180.0)
        return 0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)
    }

    fun projectTripMapViewport(
        waypoints: List<RoutePoint>,
        checkpoints: List<VisitedCheckpointLog>,
        width: Float,
        height: Float,
        paddingFraction: Float = 0.14f
    ): TripMapViewportProjection {
        val validWaypoints = waypoints.filter { hasValidGeoCoord(it.latitude, it.longitude) }
        val validCheckpoints = checkpoints.filter { hasValidGeoCoord(it.latitude, it.longitude) }
        val allGeo = buildList {
            validWaypoints.forEach { add(LatLngGeo(it.latitude, it.longitude)) }
            validCheckpoints.forEach { add(LatLngGeo(it.latitude, it.longitude)) }
        }

        val padX = width * paddingFraction
        val padY = height * paddingFraction
        val usableW = max(20f, width - padX * 2f)
        val usableH = max(20f, height - padY * 2f)

        if (allGeo.isEmpty() || width <= 1f || height <= 1f) {
            return TripMapViewportProjection(
                waypoints = waypoints.map {
                    ProjectedViewportPoint(padX + it.xFraction * usableW, padY + it.yFraction * usableH)
                },
                checkpoints = checkpoints.map {
                    ProjectedViewportPoint(padX + it.xFraction * usableW, padY + it.yFraction * usableH)
                },
                tiles = emptyList(),
                hasGeo = false,
                tileZoom = 12
            )
        }

        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        for (pt in allGeo) {
            val nx = mercatorNormX(pt.lng)
            val ny = mercatorNormY(pt.lat)
            if (nx < minX) minX = nx
            if (nx > maxX) maxX = nx
            if (ny < minY) minY = ny
            if (ny > maxY) maxY = ny
        }

        val minNormSpan = 0.000018
        val spanX = max(minNormSpan, maxX - minX)
        val spanY = max(minNormSpan, maxY - minY)

        val worldNeededX = usableW / (spanX * 256.0)
        val worldNeededY = usableH / (spanY * 256.0)
        val scaleZoom = log2(min(worldNeededX, worldNeededY)).coerceIn(4.0, 16.4)
        val tileZoom = floor(scaleZoom).toInt().coerceIn(4, 16)
        val tileCountPerAxis = 1 shl tileZoom

        val totalWorldPx = 256.0 * 2.0.pow(scaleZoom)
        val centerWorldX = ((minX + maxX) / 2.0) * totalWorldPx
        val centerWorldY = ((minY + maxY) / 2.0) * totalWorldPx

        fun projectLatLng(lat: Double, lng: Double): ProjectedViewportPoint = ProjectedViewportPoint(
            x = (width / 2.0 + (mercatorNormX(lng) * totalWorldPx - centerWorldX)).toFloat(),
            y = (height / 2.0 + (mercatorNormY(lat) * totalWorldPx - centerWorldY)).toFloat()
        )

        val tileSizePx = (256.0 * 2.0.pow(scaleZoom - tileZoom)).toFloat()
        val worldLeftPx = centerWorldX - width / 2.0
        val worldTopPx = centerWorldY - height / 2.0
        val worldRightPx = centerWorldX + width / 2.0
        val worldBottomPx = centerWorldY + height / 2.0

        val txStart = floor(worldLeftPx / tileSizePx).toInt()
        val txEnd = floor(worldRightPx / tileSizePx).toInt()
        val tyStart = max(0, floor(worldTopPx / tileSizePx).toInt())
        val tyEnd = min(tileCountPerAxis - 1, floor(worldBottomPx / tileSizePx).toInt())

        val tiles = buildList {
            for (ty in tyStart..tyEnd) {
                for (tx in txStart..txEnd) {
                    val wrappedX = ((tx % tileCountPerAxis) + tileCountPerAxis) % tileCountPerAxis
                    add(
                        ProjectedMapTile(
                            key = "$tileZoom-$wrappedX-$ty-$tx",
                            z = tileZoom,
                            x = wrappedX,
                            y = ty,
                            left = (width / 2.0 + (tx * tileSizePx - centerWorldX)).toFloat(),
                            top = (height / 2.0 + (ty * tileSizePx - centerWorldY)).toFloat(),
                            size = tileSizePx
                        )
                    )
                }
            }
        }

        return TripMapViewportProjection(
            waypoints = waypoints.map { pt ->
                if (hasValidGeoCoord(pt.latitude, pt.longitude)) {
                    projectLatLng(pt.latitude, pt.longitude)
                } else {
                    ProjectedViewportPoint(padX + pt.xFraction * usableW, padY + pt.yFraction * usableH)
                }
            },
            checkpoints = checkpoints.map { cp ->
                if (hasValidGeoCoord(cp.latitude, cp.longitude)) {
                    projectLatLng(cp.latitude, cp.longitude)
                } else {
                    ProjectedViewportPoint(padX + cp.xFraction * usableW, padY + cp.yFraction * usableH)
                }
            },
            tiles = tiles,
            hasGeo = true,
            tileZoom = tileZoom
        )
    }

    fun compactInstructionsForSync(
        instructions: List<RouteInstruction>,
        maxCount: Int = 50
    ): List<Map<String, Any>> {
        if (instructions.isEmpty()) return emptyList()
        return instructions
            .take(maxCount.coerceIn(1, 60))
            .map { inst ->
                mapOf(
                    "lat" to inst.latitude,
                    "lng" to inst.longitude,
                    "offsetMeters" to inst.routeOffsetInMeters.coerceAtLeast(0),
                    "maneuver" to inst.maneuver.take(40).ifBlank { "STRAIGHT" },
                    "message" to inst.message.take(160),
                    "street" to inst.street.take(100)
                )
            }
    }

    fun selectClosestCoLeader(
        members: List<ConvoyMember>,
        leaderUid: String?
    ): ClosestCoLeaderResult {
        val leaderMember = members.firstOrNull { !leaderUid.isNullOrBlank() && it.id == leaderUid }
            ?: members.firstOrNull { it.role == UserRole.TOUR_LEADER }
            ?: return ClosestCoLeaderResult(null, emptyMap())

        if (!hasValidGeoCoord(leaderMember.mapPosition.latitude, leaderMember.mapPosition.longitude)) {
            return ClosestCoLeaderResult(null, emptyMap())
        }

        val leaderPos = LatLngGeo(leaderMember.mapPosition.latitude, leaderMember.mapPosition.longitude)
        val distanceMap = mutableMapOf<String, Int>()
        var closestUid: String? = null
        var minDist = Int.MAX_VALUE

        for (member in members) {
            if (member.id == leaderMember.id) continue
            if (!hasValidGeoCoord(member.mapPosition.latitude, member.mapPosition.longitude)) continue
            val dist = haversineMeters(
                leaderPos,
                LatLngGeo(member.mapPosition.latitude, member.mapPosition.longitude)
            ).roundToInt().coerceAtLeast(0)
            distanceMap[member.id] = dist

            val isHealthy = member.connectionStatus != ConnectionStatus.OFFLINE &&
                member.conditionStatus != MemberConditionStatus.SOS &&
                member.conditionStatus != MemberConditionStatus.MOGOK

            if (isHealthy && dist < minDist) {
                minDist = dist
                closestUid = member.id
            }
        }

        return ClosestCoLeaderResult(
            coLeaderUid = closestUid,
            distanceByMemberId = distanceMap
        )
    }
}
