package com.yasli.yardimci.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_log")
data class MedicineLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ilacId: Long,
    val tarihSaat: Long, // epoch millis
    val alindi: Boolean = true
)
