package com.yasli.yardimci.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.yasli.yardimci.data.entity.Medicine
import com.yasli.yardimci.data.entity.MedicineLog
import com.yasli.yardimci.data.entity.NotificationLog
import com.yasli.yardimci.data.entity.QuickDial
import com.yasli.yardimci.data.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickDialDao {
    @Query("SELECT * FROM quick_dial ORDER BY id")
    fun hepsi(): Flow<List<QuickDial>>

    @Insert
    suspend fun ekle(q: QuickDial): Long

    @Update
    suspend fun guncelle(q: QuickDial)

    @Delete
    suspend fun sil(q: QuickDial)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminder WHERE aktif = 1 ORDER BY saat")
    fun aktif(): Flow<List<Reminder>>

    @Insert
    suspend fun ekle(r: Reminder): Long

    @Update
    suspend fun guncelle(r: Reminder)

    @Query("DELETE FROM reminder WHERE id = :id")
    suspend fun sil(id: Long)
}

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicine WHERE aktif = 1 ORDER BY saat")
    fun aktif(): Flow<List<Medicine>>

    @Insert
    suspend fun ekle(m: Medicine): Long

    @Update
    suspend fun guncelle(m: Medicine)

    @Query("DELETE FROM medicine WHERE id = :id")
    suspend fun sil(id: Long)
}

@Dao
interface MedicineLogDao {
    @Query("SELECT * FROM medicine_log ORDER BY tarihSaat DESC")
    fun hepsi(): Flow<List<MedicineLog>>

    @Insert
    suspend fun ekle(l: MedicineLog)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_log WHERE kaynak = :kaynak ORDER BY zaman DESC LIMIT 50")
    fun son(kaynak: String): Flow<List<NotificationLog>>

    @Insert
    suspend fun ekle(l: NotificationLog)
}
