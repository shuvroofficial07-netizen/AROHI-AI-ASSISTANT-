package com.example.device

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import java.util.Locale

data class InstalledApp(
    val label: String,
    val packageName: String,
    val aliases: List<String>
)

class AppDiscoveryManager(private val context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private var cachedApps: List<InstalledApp> = emptyList()

    fun refreshInstalledApps(): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
        val apps = mutableListOf<InstalledApp>()

        for (info in resolveInfos) {
            val label = info.loadLabel(packageManager).toString()
            val pkg = info.activityInfo.packageName
            val aliases = generateAliases(label, pkg)
            apps.add(InstalledApp(label = label, packageName = pkg, aliases = aliases))
        }
        cachedApps = apps
        return apps
    }

    fun getInstalledApps(): List<InstalledApp> {
        if (cachedApps.isEmpty()) {
            return refreshInstalledApps()
        }
        return cachedApps
    }

    fun findApp(query: String): InstalledApp? {
        val apps = getInstalledApps()
        val cleanQuery = query.lowercase(Locale.ROOT).trim()

        // 1. Exact match on label or package
        apps.firstOrNull { it.label.equals(cleanQuery, ignoreCase = true) || it.packageName.equals(cleanQuery, ignoreCase = true) }?.let { return it }

        // 2. Exact match on aliases
        apps.firstOrNull { app -> app.aliases.any { it.equals(cleanQuery, ignoreCase = true) } }?.let { return it }

        // 3. Contains match on label or alias
        apps.firstOrNull { it.label.lowercase(Locale.ROOT).contains(cleanQuery) }?.let { return it }
        apps.firstOrNull { app -> app.aliases.any { it.lowercase(Locale.ROOT).contains(cleanQuery) } }?.let { return it }

        // 4. Query contains app label or alias
        apps.firstOrNull { cleanQuery.contains(it.label.lowercase(Locale.ROOT)) }?.let { return it }
        apps.firstOrNull { app -> app.aliases.any { cleanQuery.contains(it.lowercase(Locale.ROOT)) } }?.let { return it }

        return null
    }

    /** Builds a lowercase alias/label → real app label map for the intent classifier. */
    fun buildAliasMap(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (app in getInstalledApps()) {
            map[app.label.lowercase(Locale.ROOT).trim()] = app.label
            for (alias in app.aliases) {
                map[alias.lowercase(Locale.ROOT).trim()] = app.label
            }
        }
        return map
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun generateAliases(label: String, pkg: String): List<String> {
        val aliases = mutableListOf<String>()
        val lower = label.lowercase(Locale.ROOT)
        aliases.add(lower)

        // Common Bengali & English transliteration aliases
        when {
            lower.contains("facebook") || pkg.contains("facebook") -> {
                aliases.addAll(listOf("fb", "ফেসবুক", "ফেস বুক", "face book"))
            }
            lower.contains("whatsapp") || pkg.contains("whatsapp") -> {
                aliases.addAll(listOf("হোয়াটসঅ্যাপ", "হোয়াটসঅ্যাপ", "whats app", "ওয়াটসঅ্যাপ"))
            }
            lower.contains("youtube") || pkg.contains("youtube") -> {
                aliases.addAll(listOf("ইউটিউব", "ইউ ট্যুব", "you tube", "yt"))
            }
            lower.contains("chrome") || pkg.contains("chrome") -> {
                aliases.addAll(listOf("ক্রোম", "গুগল ক্রোম", "browser", "ব্রাউজার"))
            }
            lower.contains("camera") || pkg.contains("camera") -> {
                aliases.addAll(listOf("ক্যামেরা", "ছবি", "photo"))
            }
            lower.contains("gallery") || lower.contains("photos") || pkg.contains("gallery") || pkg.contains("photos") -> {
                aliases.addAll(listOf("গ্যালারি", "গ্যালারী", "ফটোস", "ছবি"))
            }
            lower.contains("calculator") || pkg.contains("calculator") -> {
                aliases.addAll(listOf("ক্যালকুলেটর", "হিসাব", "calc"))
            }
            lower.contains("clock") || lower.contains("alarm") -> {
                aliases.addAll(listOf("ঘড়ি", "এলার্ম", "অ্যালার্ম"))
            }
            lower.contains("settings") -> {
                aliases.addAll(listOf("সেটিংস", "সেটিং"))
            }
            lower.contains("telegram") -> {
                aliases.addAll(listOf("টেলিগ্রাম", "টিজি"))
            }
            lower.contains("maps") -> {
                aliases.addAll(listOf("ম্যাপ", "গুগল ম্যাপ", "map"))
            }
            lower.contains("contacts") || lower.contains("phone") || lower.contains("dialer") -> {
                aliases.addAll(listOf("ফোন", "কন্টাক্ট", "ডায়ালার", "কল"))
            }
            lower.contains("messages") || lower.contains("messaging") -> {
                aliases.addAll(listOf("মেসেজ", "বার্তা", "sms", "মেসেঞ্জার"))
            }
        }
        return aliases.distinct()
    }
}
