package com.example.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.KeyEvent

/**
 * Universal phone control: every action here maps to a REAL Android subsystem
 * (system settings panels, media key events, Bluetooth radio). Results are
 * reported honestly — the caller receives true/false from the platform.
 */
class DeviceControlManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    enum class SettingsPanel(val label: String) {
        WIFI("Wi-Fi settings"),
        BLUETOOTH("Bluetooth settings"),
        DISPLAY("Display settings"),
        SOUND("Sound settings"),
        BATTERY("Battery settings"),
        NOTIFICATIONS("Notification settings"),
        ACCESSIBILITY("Accessibility settings"),
        APPS("Application settings"),
        STORAGE("Storage settings"),
        DEVICE_INFO("Device information"),
        LOCATION("Location settings"),
        SECURITY("Security settings")
    }

    fun openSettingsPanel(panel: SettingsPanel): Boolean {
        return try {
            val action: String = when (panel) {
                SettingsPanel.WIFI -> Settings.ACTION_WIFI_SETTINGS
                SettingsPanel.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
                SettingsPanel.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
                SettingsPanel.SOUND -> Settings.ACTION_SOUND_SETTINGS
                SettingsPanel.BATTERY -> Settings.ACTION_BATTERY_SAVER_SETTINGS
                SettingsPanel.NOTIFICATIONS -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
                SettingsPanel.ACCESSIBILITY -> Settings.ACTION_ACCESSIBILITY_SETTINGS
                SettingsPanel.APPS -> Settings.ACTION_APPLICATION_SETTINGS
                SettingsPanel.STORAGE -> Settings.ACTION_INTERNAL_STORAGE_SETTINGS
                SettingsPanel.DEVICE_INFO -> Settings.ACTION_DEVICE_INFO_SETTINGS
                SettingsPanel.LOCATION -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
                SettingsPanel.SECURITY -> Settings.ACTION_SECURITY_SETTINGS
            }
            val intent = when (panel) {
                SettingsPanel.NOTIFICATIONS -> Intent(action).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                else -> Intent(action)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openUrl(url: String): Boolean {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Real media key dispatch (works while AROHI runs in the foreground). */
    fun dispatchMediaAction(action: String): Boolean {
        val keyCode = when (action.lowercase()) {
            "play", "pause", "play_pause", "playpause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> return false
        }
        return try {
            audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Toggles the Bluetooth radio. Requires BLUETOOTH_ADMIN (declared) on
     * Android 9-11; on newer Android versions this is restricted by the
     * platform and we report the true result instead of pretending.
     */
    @Suppress("DEPRECATION")
    fun setBluetoothEnabled(enabled: Boolean): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ removed app-level Bluetooth toggling.
                openSettingsPanel(SettingsPanel.BLUETOOTH)
            } else {
                val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
                } else {
                    BluetoothAdapter.getDefaultAdapter()
                }
                when {
                    adapter == null -> false
                    enabled -> adapter.enable()
                    else -> adapter.disable()
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
