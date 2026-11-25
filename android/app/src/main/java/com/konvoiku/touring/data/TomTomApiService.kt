package com.konvoiku.touring.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.konvoiku.touring.BuildConfig
import com.konvoiku.touring.model.RouteInstruction
import com.konvoiku.touring.model.RoutePoint
import com.konvoiku.touring.model.RoutePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class TomTomRoutePoint(
    val latitude: Double,
    val longitude: Double
)

data class TomTomRouteResult(
    val distanceMeters: Int,
    val durationMillis: Long,
    val trafficDelaySeconds: Int,
    val path: List<TomTomRoutePoint>,
    val instructions: List<RouteInstruction> = emptyList()
)

data class TomTomPlaceResult(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

data class TomTomReverseResult(
    val name: String,
    val address: String
)

object TomTomApiService {
    private val apiKey: String
        get() = BuildConfig.TOMTOM_API_KEY.trim()

    private val tileCache = LruCache<String, Bitmap>(64)

    suspend fun fetchMapTileBitmap(
        layer: String,
        style: String,
        z: Int,
        x: Int,
        y: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val cacheKey = "$layer/$style/$z/$x/$y"
        tileCache.get(cacheKey)?.let { return@withContext it }

        val ext = if (layer == "sat") "jpg" else "png"
        val url = "https://api.tomtom.com/map/1/tile/$layer/$style/$z/$x/$y.$ext?key=${
            URLEncoder.encode(apiKey, "UTF-8")
        }&tileSize=256"

        runCatching {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            try {
                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.also { bmp ->
                            tileCache.put(cacheKey, bmp)
                        }
                    }
                } else {
                    null
                }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    suspend fun calculateRoute(
        waypoints: List<RoutePoint>,
        travelMode: String = "motorcycle",
        routePreference: RoutePreference = RoutePreference.FASTEST
    ): TomTomRouteResult? = withContext(Dispatchers.IO) {
        if (waypoints.size < 2 || apiKey.isBlank()) return@withContext null

        val sampled = if (waypoints.size <= 8) {
            waypoints
        } else {
            val step = ((waypoints.size - 2) / 6.0).toInt().coerceAtLeast(1)
            buildList {
                add(waypoints.first())
                waypoints.subList(1, waypoints.lastIndex)
                    .filterIndexed { index, _ -> index % step == 0 }
                    .take(6)
                    .forEach { add(it) }
                add(waypoints.last())
            }
        }

        val pointsParam = sampled.joinToString(":") {
            String.format(Locale.US, "%.6f,%.6f", it.latitude, it.longitude)
        }

        val routeTypeParam = if (routePreference == RoutePreference.SHORTEST) "shortest" else "fastest"
        val avoidParam = if (routePreference == RoutePreference.NO_TOLL) "&avoid=tollRoads" else ""
        val url = "https://api.tomtom.com/routing/1/calculateRoute/${
            URLEncoder.encode(pointsParam, "UTF-8")
        }/json?key=${URLEncoder.encode(apiKey, "UTF-8")}&traffic=true&travelMode=$travelMode&routeType=$routeTypeParam&instructionsType=text&language=id-ID$avoidParam"

        runCatching {
            val body = httpGet(url) ?: return@runCatching null
            val json = JSONObject(body)
            val routes = json.optJSONArray("routes") ?: return@runCatching null
            if (routes.length() == 0) return@runCatching null

            val primary = routes.getJSONObject(0)
            val summary = primary.optJSONObject("summary")
            val distanceMeters = summary?.optInt("lengthInMeters", 0) ?: 0
            val travelTimeSeconds = summary?.optLong("travelTimeInSeconds", 0L) ?: 0L
            val trafficDelaySeconds = summary?.optInt("trafficDelayInSeconds", 0) ?: 0

            val legs = primary.optJSONArray("legs")
            val path = mutableListOf<TomTomRoutePoint>()
            if (legs != null) {
                for (i in 0 until legs.length()) {
                    val points = legs.getJSONObject(i).optJSONArray("points") ?: continue
                    for (j in 0 until points.length()) {
                        val pt = points.getJSONObject(j)
                        val lat = pt.optDouble("latitude", Double.NaN)
                        val lng = pt.optDouble("longitude", Double.NaN)
                        if (!lat.isNaN() && !lng.isNaN()) {
                            path.add(TomTomRoutePoint(lat, lng))
                        }
                    }
                }
            }

            val instructionsArray = primary.optJSONObject("guidance")?.optJSONArray("instructions")
            val parsedInstructions = buildList {
                if (instructionsArray != null) {
                    for (k in 0 until instructionsArray.length()) {
                        val ins = instructionsArray.getJSONObject(k)
                        val ptObj = ins.optJSONObject("point")
                        val lat = ptObj?.optDouble("latitude", Double.NaN) ?: Double.NaN
                        val lng = ptObj?.optDouble("longitude", Double.NaN) ?: Double.NaN
                        val offsetMeters = ins.optInt("routeOffsetInMeters", 0)
                        val maneuver = ins.optString("maneuver", "STRAIGHT").trim()
                        val message = ins.optString("message", "").trim()
                        val street = ins.optString("street", "").trim()
                        if (!lat.isNaN() && !lng.isNaN() && message.isNotBlank()) {
                            add(
                                RouteInstruction(
                                    latitude = lat,
                                    longitude = lng,
                                    routeOffsetInMeters = offsetMeters,
                                    maneuver = maneuver,
                                    message = message,
                                    street = street
                                )
                            )
                        }
                    }
                }
            }

            if (path.isEmpty()) null
            else TomTomRouteResult(
                distanceMeters = distanceMeters,
                durationMillis = travelTimeSeconds * 1000L,
                trafficDelaySeconds = trafficDelaySeconds,
                path = path,
                instructions = parsedInstructions
            )
        }.getOrNull()
    }

    suspend fun searchPlaces(query: String): List<TomTomPlaceResult> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2 || apiKey.isBlank()) return@withContext emptyList()

        val encoded = URLEncoder.encode(trimmed, "UTF-8").replace("+", "%20")
        val url = "https://api.tomtom.com/search/2/search/$encoded.json?key=${
            URLEncoder.encode(apiKey, "UTF-8")
        }&countrySet=ID&limit=6&language=id-ID"

        runCatching {
            val body = httpGet(url) ?: return@runCatching emptyList()
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@runCatching emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val pos = item.optJSONObject("position") ?: continue
                    val lat = pos.optDouble("lat", Double.NaN)
                    val lng = pos.optDouble("lon", Double.NaN)
                    if (lat.isNaN() || lng.isNaN()) continue
                    val poiName = item.optJSONObject("poi")?.optString("name", "")?.trim().orEmpty()
                    val addrObj = item.optJSONObject("address")
                    val freeform = addrObj?.optString("freeformAddress", "")?.trim().orEmpty()
                    val municipality = addrObj?.optString("municipality", "")?.trim().orEmpty()
                    val address = freeform.ifBlank {
                        municipality.ifBlank {
                            String.format(Locale.US, "%.4f, %.4f", lat, lng)
                        }
                    }
                    val name = poiName.ifBlank {
                        address.substringBefore(",").trim().ifBlank { "Lokasi Dipilih" }
                    }
                    add(
                        TomTomPlaceResult(
                            id = item.optString("id", "$i-$lat-$lng"),
                            name = name,
                            address = address,
                            latitude = lat,
                            longitude = lng
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): TomTomReverseResult =
        withContext(Dispatchers.IO) {
            val fallback = TomTomReverseResult(
                name = String.format(Locale.US, "Titik Peta (%.3f, %.3f)", lat, lng),
                address = String.format(Locale.US, "Koordinat GPS %.5f, %.5f", lat, lng)
            )
            if (apiKey.isBlank()) return@withContext fallback

            val coord = String.format(Locale.US, "%.6f,%.6f", lat, lng)
            val url = "https://api.tomtom.com/search/2/reverseGeocode/$coord.json?key=${
                URLEncoder.encode(apiKey, "UTF-8")
            }&language=id-ID"

            runCatching {
                val body = httpGet(url) ?: return@runCatching fallback
                val json = JSONObject(body)
                val addresses = json.optJSONArray("addresses") ?: return@runCatching fallback
                if (addresses.length() == 0) return@runCatching fallback
                val addr = addresses.getJSONObject(0).optJSONObject("address")
                    ?: return@runCatching fallback
                val street = addr.optString("streetName", "").ifBlank {
                    addr.optString("street", "")
                }
                val area = addr.optString("municipalitySecondarySubdivision", "").ifBlank {
                    addr.optString("municipality", "")
                }
                val name = listOf(street, area).filter { it.isNotBlank() }.joinToString(", ")
                    .ifBlank { fallback.name }
                val address = addr.optString("freeformAddress", "")
                    .ifBlank { String.format(Locale.US, "%s · %.4f, %.4f", name, lat, lng) }
                TomTomReverseResult(name = name, address = address)
            }.getOrDefault(fallback)
        }

    private fun httpGet(urlString: String): String? {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
        }
        return try {
            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } finally {
            conn.disconnect()
        }
    }
}
