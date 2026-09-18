package com.example.data.repository

import android.content.Context
import com.example.device.LocationHelper
import com.example.device.WeatherClient
import com.example.device.WeatherSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real weather data with a small in-memory cache. Uses the device location when the
 * permission is granted, otherwise the city configured in settings (default Dhaka).
 */
class WeatherRepository(
    context: Context,
    private val settingsRepository: SettingsRepository
) {
    private val client = WeatherClient()
    private val locationHelper = LocationHelper(context)

    private val _snapshot = MutableStateFlow<WeatherSnapshot?>(null)
    val snapshot: StateFlow<WeatherSnapshot?> = _snapshot.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var lastFetchAt = 0L

    fun cached(): WeatherSnapshot? = _snapshot.value

    fun hasLocationPermission(): Boolean = locationHelper.hasLocationPermission()

    suspend fun refresh(force: Boolean = false): WeatherSnapshot? {
        val cachedSnapshot = _snapshot.value
        if (!force && cachedSnapshot != null &&
            System.currentTimeMillis() - lastFetchAt < CACHE_TTL_MS
        ) {
            return cachedSnapshot
        }

        _isRefreshing.value = true
        try {
            val coordinates = locationHelper.getLastKnownCoordinates()
            val snapshot = if (coordinates != null) {
                client.fetchByCoordinates(coordinates.first, coordinates.second, "আপনার লোকেশন")
            } else {
                val configuredCity = settingsRepository.getWeatherCity()
                val geocoded = client.geocode(configuredCity)
                if (geocoded != null) {
                    client.fetchByCoordinates(geocoded.first, geocoded.second, geocoded.third)
                } else {
                    null
                }
            }

            if (snapshot != null) {
                _snapshot.value = snapshot
                lastFetchAt = System.currentTimeMillis()
                _lastError.value = null
            } else {
                _lastError.value = "আবহাওয়ার ডেটা পাওয়া যায়নি (ইন্টারনেট সংযোগ পরীক্ষা করুন)"
            }
            return snapshot
        } finally {
            _isRefreshing.value = false
        }
    }

    fun clearCache() {
        _snapshot.value = null
        lastFetchAt = 0L
    }

    companion object {
        private const val CACHE_TTL_MS = 20L * 60L * 1000L
    }
}
