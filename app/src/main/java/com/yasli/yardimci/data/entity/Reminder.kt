package com.yasli.yardimci.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val baslik: String,
    val saat: String,           // "HH:mm"
    val tekrar: String = "bugun", // bugun | hergun | haftaici
    val aktif: Boolean = true
)
