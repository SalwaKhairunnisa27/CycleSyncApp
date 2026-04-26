package com.example.cyclesyncapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cyclesyncapp.data.local.dao.CycleDao
import com.example.cyclesyncapp.data.local.dao.DailyLogDao
import com.example.cyclesyncapp.data.local.dao.EducationDao
import com.example.cyclesyncapp.data.local.dao.UserDao // Import baru
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import com.example.cyclesyncapp.data.local.entity.UserEntity // Import baru

@Database(
    entities = [
        CycleEntity::class,
        EducationEntity::class,
        DailyLogEntity::class,
        UserEntity::class // Daftarkan User (baru)
    ],
    version = 1,
    exportSchema = false
)
abstract class CycleDatabase : RoomDatabase() {

    abstract fun cycleDao(): CycleDao
    abstract fun educationDao(): EducationDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun userDao(): UserDao // Tambahan

    companion object {
        @Volatile
        private var INSTANCE: CycleDatabase? = null

        fun getDatabase(context: Context): CycleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CycleDatabase::class.java,
                    "cycle_sync_db"
                )
                    .createFromAsset("databases/education.db")
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}