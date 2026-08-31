package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.Locale

data class ContactItem(
    val id: String,
    val name: String,
    val phoneNumber: String
)

class ContactsManager(private val context: Context) {

    fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun searchContacts(query: String): List<ContactItem> {
        if (!hasContactsPermission()) return emptyList()

        val results = mutableListOf<ContactItem>()
        val cleanQuery = query.lowercase(Locale.ROOT).trim()

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getString(idIdx) else ""
                    val name = if (nameIdx >= 0) it.getString(nameIdx) ?: "" else ""
                    val number = if (numberIdx >= 0) it.getString(numberIdx) ?: "" else ""

                    if (cleanQuery.isEmpty() ||
                        name.lowercase(Locale.ROOT).contains(cleanQuery) ||
                        number.replace(Regex("[^0-9+]"), "").contains(cleanQuery)
                    ) {
                        results.add(ContactItem(id = id, name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            // Ignored
        } finally {
            cursor?.close()
        }
        return results.distinctBy { it.phoneNumber }
    }
}
