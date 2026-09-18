package com.example.device

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

/**
 * Reads the device's real last-known coordinates (no play-services dependency).
 * Falls back to null when no permission or no cached fix is available, in which case the
 * caller resolves the city through Open-Meteo geocoding instead.
 */
class LocationHelper(private val context: Context) {

    private val locationManager: LocationManager? =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownCoordinates(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        val manager = locationManager ?: return null
        var best: Location? = null
        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            for (provider in providers) {
                val location = try {
                    manager.getLastKnownLocation(provider)
                } catch (e: Exception) {
                    null
                } ?: continue
                val current = best
                if (current == null || location.time > current.time) {
                    best = location
                }
            }
        } catch (e: Exception) {
            return null
        }
        val location = best ?: return null
        return Pair(location.latitude, location.longitude)
    }
}
