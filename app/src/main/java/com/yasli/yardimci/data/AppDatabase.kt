package com.yasli.yardimci.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.yasli.yardimci.data.dao.MedicineDao
import com.yasli.yardimci.data.dao.MedicineLogDao
import com.yasli.yardimci.data.dao.NotificationLogDao
import com.yasli.yardimci.data.dao.QuickDialDao
import com.yasli.yardimci.data.dao.ReminderDao
import com.yasli.yardimci.data.entity.Medicine
import com.yasli.yardimci.data.entity.MedicineLog
import com.yasli.yardimci.data.entity.NotificationLog
import com.yasli.yardimci.data.entity.QuickDial
import com.yasli.yardimci.data.entity.Reminder

@Database(
    entities = [
        QuickDial::class,
        Reminder::class,
        Medicine::class,
        MedicineLog::class,
        NotificationLog::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun quickDialDao(): QuickDialDao
    abstract fun reminderDao(): ReminderDao
    abstract fun medicineDao(): MedicineDao
    abstract fun medicineLogDao(): MedicineLogDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yasli_yardimci.db"
                ).build().also { INSTANCE = it }
            }
    }
}
