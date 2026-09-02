package com.example.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.util.Locale

data class DeviceTelemetry(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val chargingType: String,
    val freeRamMb: Long,
    val totalRamMb: Long,
    val freeStorageGb: Double,
    val totalStorageGb: Double,
    val networkType: String,
    val isConnected: Boolean,
    val mediaVolumePercent: Int,
    val ringVolumePercent: Int,
    val isFlashlightOn: Boolean,
    val brightnessPercent: Int,
    val deviceName: String,
    val androidVersion: String,
    val apiLevel: Int
)

class DeviceStateManager(private val context: Context) {

    companion object {
        /** Sentinel used when Android genuinely cannot supply a value. Never a made-up number. */
        const val UNAVAILABLE = -1
        const val UNAVAILABLE_DOUBLE = -1.0
        const val UNAVAILABLE_TEXT = "Unavailable"

        /** Fully "Unavailable" telemetry — used only when reading the device state itself failed. */
        fun unavailableTelemetry(): DeviceTelemetry = DeviceTelemetry(
            batteryPercent = UNAVAILABLE,
            isCharging = false,
            chargingType = UNAVAILABLE_TEXT,
            freeRamMb = UNAVAILABLE.toLong(),
            totalRamMb = UNAVAILABLE.toLong(),
            freeStorageGb = UNAVAILABLE_DOUBLE,
            totalStorageGb = UNAVAILABLE_DOUBLE,
            networkType = UNAVAILABLE_TEXT,
            isConnected = false,
            mediaVolumePercent = UNAVAILABLE,
            ringVolumePercent = UNAVAILABLE,
            isFlashlightOn = false,
            brightnessPercent = UNAVAILABLE,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT
        )
    }
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var isTorchActive = false
    private var cameraIdWithFlash: String? = null

    init {
        findFlashCamera()
    }

    private fun findFlashCamera() {
        try {
            cameraManager?.cameraIdList?.forEach { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraIdWithFlash = id
                    return
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun getTelemetry(): DeviceTelemetry = try {
        readTelemetry()
    } catch (e: Exception) {
        unavailableTelemetry()
    }

    private fun readTelemetry(): DeviceTelemetry {
        val (battery, charging, chargeType) = getBatteryInfo()
        val (freeRam, totalRam) = getRamInfo()
        val (freeStorage, totalStorage) = getStorageInfo()
        val (netType, connected) = getNetworkInfo()
        val (mediaVol, ringVol) = getVolumeInfo()
        val brightness = getBrightnessInfo()

        return DeviceTelemetry(
            batteryPercent = battery,
            isCharging = charging,
            chargingType = chargeType,
            freeRamMb = freeRam,
            totalRamMb = totalRam,
            freeStorageGb = freeStorage,
            totalStorageGb = totalStorage,
            networkType = netType,
            isConnected = connected,
            mediaVolumePercent = mediaVol,
            ringVolumePercent = ringVol,
            isFlashlightOn = isTorchActive,
            brightnessPercent = brightness,
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidVersion = "Android ${Build.VERSION.RELEASE}",
            apiLevel = Build.VERSION.SDK_INT
        )
    }

    fun getBatteryInfo(): Triple<Int, Boolean, String> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else UNAVAILABLE

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeType = when {
            batteryStatus == null -> UNAVAILABLE_TEXT
            chargePlug == BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            chargePlug == BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Charging"
            isCharging -> "Charging"
            else -> "On Battery"
        }

        return Triple(percent, isCharging, chargeType)
    }

    fun getRamInfo(): Pair<Long, Long> {
        val manager = activityManager ?: return Pair(UNAVAILABLE.toLong(), UNAVAILABLE.toLong())
        return try {
            val memoryInfo = ActivityManager.MemoryInfo()
            manager.getMemoryInfo(memoryInfo)
            Pair(memoryInfo.availMem / (1024 * 1024), memoryInfo.totalMem / (1024 * 1024))
        } catch (e: Exception) {
            Pair(UNAVAILABLE.toLong(), UNAVAILABLE.toLong())
        }
    }

    fun getStorageInfo(): Pair<Double, Double> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong

            val freeGb = (availableBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
            val totalGb = (totalBlocks * blockSize).toDouble() / (1024 * 1024 * 1024)
            // Locale.US: Bengali/Arabic locales render non-ASCII digits that cannot be re-parsed.
            val formattedFree = String.format(Locale.US, "%.1f", freeGb).toDoubleOrNull() ?: freeGb
            val formattedTotal = String.format(Locale.US, "%.1f", totalGb).toDoubleOrNull() ?: totalGb
            Pair(formattedFree, formattedTotal)
        } catch (e: Exception) {
            Pair(UNAVAILABLE_DOUBLE, UNAVAILABLE_DOUBLE)
        }
    }

    fun getNetworkInfo(): Pair<String, Boolean> {
        if (connectivityManager == null) return Pair("Unavailable", false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivityManager.activeNetwork ?: return Pair("Offline", false)
            val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return Pair("Offline", false)
            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Pair("Wi-Fi Connected", true)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Pair("Mobile Data (4G/LTE)", true)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Pair("Ethernet", true)
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> Pair("Connected", true)
                else -> Pair("Offline", false)
            }
        } else {
            @Suppress("DEPRECATION")
            val netInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            val isConnected = netInfo?.isConnectedOrConnecting == true
            @Suppress("DEPRECATION")
            val typeName = netInfo?.typeName ?: "Offline"
            return Pair(if (isConnected) typeName else "Offline", isConnected)
        }
    }

    fun getVolumeInfo(): Pair<Int, Int> {
        if (audioManager == null) return Pair(UNAVAILABLE, UNAVAILABLE)
        val maxMedia = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val curMedia = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val mediaPercent = (curMedia * 100) / maxMedia

        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val curRing = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val ringPercent = (curRing * 100) / maxRing

        return Pair(mediaPercent, ringPercent)
    }

    fun setMediaVolume(percent: Int): Boolean {
        if (audioManager == null) return false
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = ((percent.coerceIn(0, 100) * max) / 100).coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return true
    }

    fun toggleFlashlight(enabled: Boolean): Boolean {
        return try {
            if (cameraIdWithFlash == null) findFlashCamera()
            val id = cameraIdWithFlash ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cameraManager?.setTorchMode(id, enabled)
                isTorchActive = enabled
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isFlashlightSupported(): Boolean = cameraIdWithFlash != null

    private fun getBrightnessInfo(): Int {
        return try {
            val brightness = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                UNAVAILABLE
            )
            if (brightness < 0) UNAVAILABLE else (brightness * 100) / 255
        } catch (e: Exception) {
            UNAVAILABLE
        }
    }
}
