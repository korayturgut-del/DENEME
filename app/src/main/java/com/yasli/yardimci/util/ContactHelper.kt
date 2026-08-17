package com.yasli.yardimci.util

import android.content.Context
import android.provider.ContactsContract

/**
 * Cihaz rehberini okur (READ_CONTACTS izni gerektirir). Ad -> telefon çiftleri döner.
 */
object ContactHelper {

    fun oku(context: Context): List<Pair<String, String>> {
        val liste = mutableListOf<Pair<String, String>>()
        val cr = context.contentResolver
        cr.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )?.use { c ->
            val adIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val noIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val ad = c.getString(adIdx) ?: continue
                val no = c.getString(noIdx) ?: continue
                liste.add(ad to no)
            }
        }
        return liste
    }
}
