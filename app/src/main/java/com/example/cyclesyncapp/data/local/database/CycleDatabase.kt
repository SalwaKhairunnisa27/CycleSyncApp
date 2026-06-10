package com.example.cyclesyncapp.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.cyclesyncapp.data.local.dao.CycleDao
import com.example.cyclesyncapp.data.local.dao.DailyLogDao
import com.example.cyclesyncapp.data.local.dao.EducationDao
import com.example.cyclesyncapp.data.local.dao.UserDao
import com.example.cyclesyncapp.data.local.dao.RecommendationDao // Import baru
import com.example.cyclesyncapp.data.local.entity.CycleEntity
import com.example.cyclesyncapp.data.local.entity.DailyLogEntity
import com.example.cyclesyncapp.data.local.entity.EducationEntity
import com.example.cyclesyncapp.data.local.entity.UserEntity
import com.example.cyclesyncapp.data.local.entity.RecommendationEntity // Import baru

@Database(
    entities = [
        CycleEntity::class,
        EducationEntity::class,
        DailyLogEntity::class,
        UserEntity::class,
        RecommendationEntity::class // Tambahkan ini
    ],
    version = 4,
    exportSchema = false
)
abstract class CycleDatabase : RoomDatabase() {

    abstract fun cycleDao(): CycleDao
    abstract fun educationDao(): EducationDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun userDao(): UserDao
    abstract fun recommendationDao(): RecommendationDao // Tambahkan ini

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
                    .createFromAsset("databases/education.db") // Pastikan database awal punya data rekomendasi jika ingin pre-populated
                    .fallbackToDestructiveMigration() // Hati-hati: ini akan menghapus data lama jika versi naik
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}