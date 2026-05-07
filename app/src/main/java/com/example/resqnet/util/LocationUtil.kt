package com.example.resqnet.util

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class LatLng(val latitude: Double, val longitude: Double)

object LocationUtil {

    /** Great-circle distance between two lat/lng points in kilometres. */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    /** Centre of India — only used when GPS is completely unavailable */
    val DEFAULT_LOCATION = LatLng(20.5937, 78.9629)

    /**
     * Returns the device's best available location:
     *   1. Last-known location (instant, works indoors/offline).
     *   2. Fresh high-accuracy fix with a 5-second timeout (so we never block indefinitely).
     *   3. Centre-of-India fallback if both fail.
     *
     * Caller must have location permission before calling.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): LatLng {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            // Step 1: try last known (instant, no timeout needed)
            val last = withTimeoutOrNull(2_000) { client.lastLocation.await() }
            if (last != null) return LatLng(last.latitude, last.longitude)

            // Step 2: fresh fix, 5-second cap so we never hang
            val cts = CancellationTokenSource()
            val fresh = withTimeoutOrNull(5_000) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            }
            if (fresh != null) LatLng(fresh.latitude, fresh.longitude) else DEFAULT_LOCATION
        } catch (e: Exception) {
            DEFAULT_LOCATION
        }
    }
}
