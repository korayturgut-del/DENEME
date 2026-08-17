package com.yasli.yardimci.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val doz: String = "1 tablet",
    val saat: String = "08:00",   // "HH:mm"
    val gunler: String = "hergun", // hergun | haftaici
    val aktif: Boolean = true
)
