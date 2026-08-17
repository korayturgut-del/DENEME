package com.yasli.yardimci.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_log")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kaynak: String,  // "whatsapp" | "sms"
    val kimden: String,
    val metin: String,
    val zaman: Long,     // epoch millis
    val okundu: Boolean = false
)
