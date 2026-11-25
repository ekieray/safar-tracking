package com.konvoiku.touring.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.konvoiku.touring.util.GeoUtils
import com.konvoiku.touring.util.LatLngGeo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

data class LiveGpsSample(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Int,
    val headingDegrees: Int,
    val accuracyMeters: Int,
    val timestampMs: Long
)

class GpsLocationTracker(private val context: Context) {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(intervalMs: Long = 2000L): Flow<LiveGpsSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        var prevSample: LiveGpsSample? = null

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(3f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                val now = loc.time.takeIf { it > 0L } ?: System.currentTimeMillis()
                val prev = prevSample

                val speedKmh = if (loc.hasSpeed() && loc.speed >= 0f) {
                    (loc.speed * 3.6f).roundToInt()
                } else if (prev != null && now > prev.timestampMs) {
                    val dist = GeoUtils.haversineMeters(
                        LatLngGeo(prev.latitude, prev.longitude),
                        LatLngGeo(loc.latitude, loc.longitude)
                    )
                    val dtSec = (now - prev.timestampMs) / 1000.0
                    if (dtSec >= 0.8 && dist in 1.5..1000.0) {
                        ((dist / dtSec) * 3.6).roundToInt().coerceIn(0, 220)
                    } else 0
                } else 0

                val heading = if (loc.hasBearing() && loc.bearing >= 0f) {
                    loc.bearing.roundToInt() % 360
                } else if (prev != null) {
                    val dist = GeoUtils.haversineMeters(
                        LatLngGeo(prev.latitude, prev.longitude),
                        LatLngGeo(loc.latitude, loc.longitude)
                    )
                    if (dist >= 2.0) {
                        GeoUtils.computeBearingDegrees(
                            LatLngGeo(prev.latitude, prev.longitude),
                            LatLngGeo(loc.latitude, loc.longitude)
                        )
                    } else prev.headingDegrees
                } else 0

                val sample = LiveGpsSample(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    speedKmh = speedKmh,
                    headingDegrees = heading,
                    accuracyMeters = if (loc.hasAccuracy()) loc.accuracy.roundToInt() else 20,
                    timestampMs = now
                )
                prevSample = sample
                trySend(sample)
            }
        }

        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                val initial = LiveGpsSample(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6f).roundToInt() else 0,
                    headingDegrees = if (loc.hasBearing()) loc.bearing.roundToInt() % 360 else 0,
                    accuracyMeters = if (loc.hasAccuracy()) loc.accuracy.roundToInt() else 25,
                    timestampMs = loc.time.takeIf { it > 0L } ?: System.currentTimeMillis()
                )
                prevSample = initial
                trySend(initial)
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }
}
