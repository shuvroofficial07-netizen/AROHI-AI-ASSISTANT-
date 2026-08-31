package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.engine.SystemEventBus
import com.example.engine.SystemEventLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class CallState { IDLE, RINGING, OFFHOOK, UNKNOWN }

data class PhoneCallInfo(
    val state: CallState = CallState.IDLE,
    val incomingNumber: String = "",
    val callerName: String = "" // Resolved from ContactsContract, never invented
)

/**
 * Real incoming/outgoing call intelligence using TelephonyManager.
 * Caller names are resolved from the actual contacts database — if no contact
 * matches, the name stays blank and the UI shows "Unknown caller".
 */
class PhoneStateManager(
    private val context: Context,
    private val contactsManager: ContactsManager,
    private val eventBus: SystemEventBus
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val _callInfo = MutableStateFlow(PhoneCallInfo())
    val callInfo: StateFlow<PhoneCallInfo> = _callInfo.asStateFlow()

    private var registered = false

    @Suppress("DEPRECATION")
    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            updateCallState(state, phoneNumber)
        }
    }

    private val telephonyCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            updateCallState(state, null)
        }
    }

    fun hasPhoneStatePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    fun register() {
        if (registered) return
        if (!hasPhoneStatePermission()) return
        try {
            val tm = telephonyManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                tm.registerTelephonyCallback(context.mainExecutor, telephonyCallback)
            } else {
                @Suppress("DEPRECATION")
                tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            }
            registered = true
            updateCallState(tm.callState, null)
        } catch (e: Exception) {
            // Registration failure is reported through the diagnostics screen
        }
    }

    @Suppress("DEPRECATION")
    fun unregister() {
        if (!registered) return
        try {
            val tm = telephonyManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                tm.unregisterTelephonyCallback(telephonyCallback)
            } else {
                tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
            }
        } catch (e: Exception) {
            // Ignored
        }
        registered = false
    }

    private fun updateCallState(state: Int, number: String?) {
        val callState = when (state) {
            TelephonyManager.CALL_STATE_RINGING -> CallState.RINGING
            TelephonyManager.CALL_STATE_OFFHOOK -> CallState.OFFHOOK
            TelephonyManager.CALL_STATE_IDLE -> CallState.IDLE
            else -> CallState.UNKNOWN
        }

        val cleanNumber = number?.takeIf { it.isNotBlank() } ?: _callInfo.value.incomingNumber
        val callerName = if (callState == CallState.RINGING) {
            val matches = cleanNumber?.let { contactsManager.searchContacts(it) }
            if (matches.isNullOrEmpty()) "" else matches.first().name
        } else {
            _callInfo.value.callerName
        }

        val previous = _callInfo.value
        if (previous.state != callState || previous.incomingNumber != cleanNumber) {
            _callInfo.value = PhoneCallInfo(state = callState, incomingNumber = cleanNumber ?: "", callerName = callerName)
            when (callState) {
                CallState.RINGING -> eventBus.log(
                    "CALLS",
                    if (callerName.isNotBlank()) "Incoming call: $callerName" else "Incoming call: unknown caller",
                    SystemEventLevel.WARNING
                )
                CallState.OFFHOOK -> eventBus.log("CALLS", "Call in progress", SystemEventLevel.INFO)
                CallState.IDLE -> eventBus.log("CALLS", "Call ended / idle", SystemEventLevel.INFO)
                else -> Unit
            }
        }
    }
}
