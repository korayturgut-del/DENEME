package com.yasli.yardimci.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_dial")
data class QuickDial(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ad: String,
    val telefon: String,
    val renk: String = "green", // green | blue
    val fotoUri: String? = null
)
