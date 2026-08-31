package com.example.device

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings

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
    val apiLevel: Int,
    val bluetoothState: String,
    val isScreenOn: Boolean,
    val uptimeMillis: Long,
    val foregroundAppLabel: String?
)

class DeviceStateManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

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

    fun getTelemetry(): DeviceTelemetry {
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
            apiLevel = Build.VERSION.SDK_INT,
            bluetoothState = getBluetoothState(),
            isScreenOn = powerManager?.isInteractive == true,
            uptimeMillis = SystemClock.elapsedRealtime(),
            foregroundAppLabel = getForegroundAppLabel()
        )
    }

    /**
     * Real Bluetooth radio state. Returns "Unavailable" when the device has
     * no Bluetooth hardware or the state cannot be read.
     */
    fun getBluetoothState(): String {
        return try {
            val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                btManager?.adapter
            } else {
                @Suppress("DEPRECATION")
                BluetoothAdapter.getDefaultAdapter()
            }
            when (adapter?.state) {
                BluetoothAdapter.STATE_ON -> "On"
                BluetoothAdapter.STATE_OFF -> "Off"
                BluetoothAdapter.STATE_TURNING_ON -> "Turning on"
                BluetoothAdapter.STATE_TURNING_OFF -> "Turning off"
                else -> "Unavailable"
            }
        } catch (e: Exception) {
            "Unavailable"
        }
    }

    /**
     * Foreground application label via UsageStatsManager. Requires the
     * special "Usage access" permission; returns null when not granted so the
     * UI can honestly show "Unavailable".
     */
    fun getForegroundAppLabel(): String? {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) return null
            if (!hasUsageAccessPermission()) return null
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 60_000L
            val stats = usageStatsManager
                ?.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
                ?.filter { it.lastTimeUsed > 0 }
                ?.maxByOrNull { it.lastTimeUsed }
                ?: return null
            val appInfo = try {
                context.packageManager.getApplicationInfo(stats.packageName, 0)
            } catch (e: Exception) {
                null
            }
            appInfo?.let { context.packageManager.getApplicationLabel(it).toString() }
        } catch (e: Exception) {
            null
        }
    }

    fun hasUsageAccessPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun getBatteryInfo(): Triple<Int, Boolean, String> {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargeType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Fast Charging"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        return Triple(percent, isCharging, chargeType)
    }

    fun getRamInfo(): Pair<Long, Long> {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val freeMb = memoryInfo.availMem / (1024 * 1024)
        val totalMb = memoryInfo.totalMem / (1024 * 1024)
        return Pair(freeMb, totalMb)
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
            val formattedFree = String.format("%.1f", freeGb).toDoubleOrNull() ?: freeGb
            val formattedTotal = String.format("%.1f", totalGb).toDoubleOrNull() ?: totalGb
            Pair(formattedFree, formattedTotal)
        } catch (e: Exception) {
            Pair(0.0, 0.0)
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
        if (audioManager == null) return Pair(0, 0)
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
                -1
            )
            if (brightness < 0) -1 else (brightness * 100) / 255
        } catch (e: Exception) {
            -1 // UI must show "Unavailable" instead of an invented value
        }
    }
}
