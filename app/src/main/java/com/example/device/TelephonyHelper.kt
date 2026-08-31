package com.example.device

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

class TelephonyHelper(private val context: Context) {

    fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun makeCallOrDial(phoneNumber: String): Boolean {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isEmpty()) return false

        return try {
            if (hasCallPermission()) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                true
            } else {
                openDialer(cleanNumber)
            }
        } catch (e: Exception) {
            openDialer(cleanNumber)
        }
    }

    fun openDialer(phoneNumber: String): Boolean {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendSms(phoneNumber: String, messageText: String): Boolean {
        return try {
            val uri = Uri.parse("smsto:$phoneNumber")
            val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun sendWhatsAppMessage(phoneNumber: String, messageText: String): Boolean {
        return try {
            val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
            val encodedMsg = Uri.encode(messageText)
            val url = if (cleanPhone.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
            } else {
                "https://api.whatsapp.com/send?text=$encodedMsg"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            try {
                // Fallback to browser or any app that handles WhatsApp URL
                val encodedMsg = Uri.encode(messageText)
                val url = "https://wa.me/?text=$encodedMsg"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }
}
